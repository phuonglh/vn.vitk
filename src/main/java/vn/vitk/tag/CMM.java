package vn.vitk.tag;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.ml.Estimator;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.feature.CountVectorizer;
import org.apache.spark.ml.feature.CountVectorizerModel;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.optimization.LBFGS;
import org.apache.spark.mllib.optimization.LogisticGradient;
import org.apache.spark.mllib.optimization.SquaredL2Updater;
import org.apache.spark.mllib.util.MLUtils;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.StructType;

import scala.Tuple2;
import vn.vitk.util.Constants;

/**
 * @author Phuong LE-HONG
 *         <p>
 *         May 9, 2016, 6:09:08 PM
 *         <p>
 *         Conditional Markov model for sequence labeling. This models is also
 *         called Maximum-Entropy Markov model (MEMM).
 *         <p>
 *         The input of this estimator is a data frame containing two columns: "featureStrings"
 *         and "label".
 * 
 */
public class CMM extends Estimator<CMMModel> {

	private static final long serialVersionUID = -6366026106299743238L;
	private String uid = null;
	private int numLabels = 0;
	private CMMParams params;
	
	private boolean verbose = false;
	
	/**
	 * Constructs a Conditional Markov Model for sequence labeling with default 
	 * parameters for use in training.
	 */
	public CMM() {
		params = new CMMParams();
	}
	
	/**
	 * Creates a Conditioanl Markov model for sequence labeling with given 
	 * parameters for use in training.
	 * @param params
	 */
	public CMM(CMMParams params) {
		this.params = params;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.spark.ml.util.Identifiable#uid()
	 */
	@Override
	public String uid() {
		if (uid == null) {
			String ruid = UUID.randomUUID().toString();
			int n = ruid.length();
			uid = "cmm" + "_" + ruid.substring(n-12, n);
		}
		return uid;
	}

	/* (non-Javadoc)
	 * @see org.apache.spark.ml.Estimator#copy(org.apache.spark.ml.param.ParamMap)
	 */
	@Override
	public Estimator<CMMModel> copy(ParamMap extra) {
		return defaultCopy(extra);
	}

	/* (non-Javadoc)
	 * @see org.apache.spark.ml.Estimator#fit(org.apache.spark.sql.DataFrame)
	 */
	@Override
	public CMMModel fit(DataFrame dataset) {
		MarkovOrder order = MarkovOrder.values()[(Integer)params.getOrDefault(params.getMarkovOrder())-1];
		ContextExtractor contextExtractor = new ContextExtractor(order, Constants.REGEXP_FILE);
		JavaRDD<LabeledContext> contexts = contextExtractor.extract(dataset.javaRDD());
		DataFrame dataFrame = dataset.sqlContext().createDataFrame(contexts, LabeledContext.class);
		
		// create a processing pipeline and fit it to the data frame
		Pipeline pipeline = createPipeline();
		
		PipelineModel pipelineModel = pipeline.fit(dataFrame);
		DataFrame df = pipelineModel.transform(dataFrame);
		
		// compute the tag dictionary from this dataset, each word has an associate set of tag
		JavaRDD<Row> wt = df.select("word", "label").javaRDD();
		JavaPairRDD<String, Set<Integer>> tagDictionary = wt.mapToPair(new PairFunction<Row, String, Set<Integer>>(){
			private static final long serialVersionUID = 5865372074294028547L;
			@Override
			public Tuple2<String, Set<Integer>> call(Row row) throws Exception {
				Set<Integer> labels = new TreeSet<Integer>();
				labels.add((int)row.getDouble(1));
				return new Tuple2<String, Set<Integer>>(row.getString(0), labels);
			}
		}).reduceByKey(new Function2<Set<Integer>, Set<Integer>, Set<Integer>>(){
			private static final long serialVersionUID = 650464866325369877L;
			@Override
			public Set<Integer> call(Set<Integer> s0, Set<Integer> s1)
					throws Exception {
				s0.addAll(s1);
				return s0;
			}
		});
		
		// compute the number of different labels, which is the maximum value in the 'label' column.
		df.registerTempTable("dft");
		Row row = df.sqlContext().sql("SELECT MAX(label) as maxValue FROM dft").first();
		this.numLabels = (int)row.getDouble(0) + 1;
		
		JavaRDD<Row> rows = df.sqlContext().sql("SELECT label, features FROM dft").toJavaRDD();
		// map the rows to a RDD which is compatible to the L-BFGS training algorithm
		JavaRDD<Tuple2<Object, Vector>> trainingData = rows.map(new Function<Row, Tuple2<Object, Vector>>(){
			private static final long serialVersionUID = -8579021851841129697L;
			@Override
			public Tuple2<Object, Vector> call(Row row) throws Exception {
				double label = row.getDouble(0);
				Vector features = MLUtils.appendBias((Vector) row.get(1));
				return new Tuple2<Object, Vector>(label, features);
			}
		});
		
		// cache the training data for better performance
		trainingData.cache();
		// initial weight values (with intercept)
		int numFeatures = (Integer)params.getOrDefault(params.getNumFeatures());
		// if numFeatures is greater than the actual feature vocabulary length, 
		// we need to use the vocabulary length, otherwise it will raise an error 
		// when computing the gradient in the BFGS optimization algorithm.
		CountVectorizerModel cvm = (CountVectorizerModel)pipelineModel.stages()[1];
		int vocabSize = cvm.vocabulary().length;
		numFeatures = Math.min(numFeatures, vocabSize);
		
		Vector initialWeights = Vectors.dense(new double[(numLabels-1)*(numFeatures+1)]);
		// run L-BFGS algorithm to build the model
		double tolerance = (Double)params.getOrDefault(params.getTolerance());
		int maxIter = (Integer)params.getOrDefault(params.getMaxIter());
		double regParam = (Double)params.getOrDefault(params.getRegParam());
		
		long beginTime = System.currentTimeMillis();
		
		Tuple2<Vector, double[]> result = LBFGS.runLBFGS(trainingData.rdd(),
				new LogisticGradient(numLabels), new SquaredL2Updater(), 10, tolerance,
				maxIter, regParam, initialWeights);
		
		long endTime = System.currentTimeMillis();
		
		Vector weights = result._1();
		
		Map<String, Set<Integer>> td = tagDictionary.collectAsMap();
		
		if (verbose) {
			System.out.println("numFeatures = " + numFeatures);
			System.out.println("vocabSize = " + vocabSize);
			System.out.println("numLabels = " + numLabels);
			System.out.println("numUniqueWords = " + td.size());
			System.out.println("Number of iterations = " + result._2().length);
			System.out.println("Initial loss = " + result._2()[0]);
			System.out.println("Final loss = " + result._2()[result._2().length-1]);
			System.out.println("Training time = " + (endTime - beginTime) / 1000 + " seconds.");
		}
		return new CMMModel(pipelineModel, weights, order, td);
	}

	
	/**
	 * Creates a processing pipeline.
	 * @return a pipeline
	 */
	private Pipeline createPipeline() {
		Tokenizer tokenizer = new Tokenizer()
			.setInputCol("featureStrings")
			.setOutputCol("tokens");
		CountVectorizer countVectorizer = new CountVectorizer()
			.setInputCol("tokens")
			.setOutputCol("features")
			.setMinDF((Double)params.getOrDefault(params.getMinFF()))
			.setVocabSize((Integer)params.getOrDefault(params.getNumFeatures()));  
		StringIndexer tagIndexer = new StringIndexer()
			.setInputCol("tag")
			.setOutputCol("label");
		
		Pipeline pipeline = new Pipeline().setStages(new PipelineStage[]{tokenizer, countVectorizer, tagIndexer});
		return pipeline;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.spark.ml.PipelineStage#transformSchema(org.apache.spark.sql.types.StructType)
	 */
	@Override
	public StructType transformSchema(StructType schema) {
		return schema;
	}
	
	public CMM setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}
}
