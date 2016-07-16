package vn.vitk.dep;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.Transformer;
import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel;
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.CountVectorizer;
import org.apache.spark.ml.feature.CountVectorizerModel;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;

/**
 * @author Phuong LE-HONG
 *         <p>
 *         Transition classifier for deterministic dependency parsing.
 */
public class TransitionClassifier implements Serializable {

	private static final long serialVersionUID = -8638888022254374959L;

	protected TransitionClassifierParams params;
	protected SQLContext sqlContext;
	protected boolean verbose = false;

	public TransitionClassifier() {
		this.params = new TransitionClassifierParams();
	}
	
	public TransitionClassifier(TransitionClassifierParams params) {
		this.params = params;
	}

	
	/**
	 * Trains a transition classifier on the data frame.
	 * @param jsc
	 * @param graphs
	 * @param featureFrame
	 * @param classifierFileName
	 * @param numHiddenUnits
	 * @return a transition classifier.
	 */
	public Transformer trainMLP(JavaSparkContext jsc,
			List<DependencyGraph> graphs, FeatureFrame featureFrame,
			String classifierFileName, int numHiddenUnits) {
		// create a SQLContext
		this.sqlContext = new SQLContext(jsc);
		// extract a data frame from these graphs
		DataFrame dataset = toDataFrame(jsc, graphs, featureFrame);
		
		// create a processing pipeline and fit it to the data frame
		Pipeline pipeline = createPipeline();
		PipelineModel pipelineModel = pipeline.fit(dataset);
		DataFrame trainingData = pipelineModel.transform(dataset);
		
		// cache the training data for better performance
		trainingData.cache();
		
		if (verbose) {
			trainingData.show(false);
		}
		
		// compute the number of different labels, which is the maximum element 
		// in the 'label' column.
		trainingData.registerTempTable("dfTable");
		Row row = sqlContext.sql("SELECT MAX(label) as maxValue from dfTable").first();
		int numLabels = (int)row.getDouble(0);
		numLabels++;
		
		int vocabSize = ((CountVectorizerModel)(pipelineModel.stages()[1])).getVocabSize();
		
		// default is a two-layer MLP
		int[] layers = {vocabSize, numLabels};
		// if user specify a hidden layer, use a 3-layer MLP:
		if (numHiddenUnits > 0) {
			layers = new int[3];
			layers[0] = vocabSize;
			layers[1] = numHiddenUnits;
			layers[2] = numLabels;
		}
		MultilayerPerceptronClassifier classifier = new MultilayerPerceptronClassifier()
			.setLayers(layers)
			.setBlockSize(128)
			.setSeed(1234L)
			.setTol((Double)params.getOrDefault(params.getTolerance()))
			.setMaxIter((Integer)params.getOrDefault(params.getMaxIter()));
		MultilayerPerceptronClassificationModel model = classifier.fit(trainingData);
		
		// compute precision on the training data
		//
		DataFrame result = model.transform(trainingData);
		DataFrame predictionAndLabel = result.select("prediction", "label");
		MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator().setMetricName("precision");
		if (verbose) {
			System.out.println("N = " + trainingData.count());
			System.out.println("D = " + vocabSize);
			System.out.println("K = " + numLabels);
			System.out.println("H = " + numHiddenUnits);
			System.out.println("training precision = " + evaluator.evaluate(predictionAndLabel));
		}
		
		// save the trained MLP to a file
		//
		String classifierPath = new Path(classifierFileName, "data").toString();
		jsc.parallelize(Arrays.asList(model), 1).saveAsObjectFile(classifierPath);
		// save the pipeline model to sub-directory "pipelineModel"
		// 
		try {
			String pipelinePath = new Path(classifierFileName, "pipelineModel").toString(); 
			pipelineModel.write().overwrite().save(pipelinePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return model;
	}
	
	/**
	 * Converts a dependency graph to a data frame.
	 * @param jsc
	 * @param graph
	 * @param featureFrame
	 * @return a data frame
	 */
	public DataFrame toDataFrame(JavaSparkContext jsc, DependencyGraph graph, FeatureFrame featureFrame) {
		List<ParsingContext> list = TransitionDecoder.decode(graph, featureFrame);
		JavaRDD<ParsingContext> javaRDD = jsc.parallelize(list);
		return sqlContext.createDataFrame(javaRDD, ParsingContext.class);
	}

	/**
	 * Converts a list of dependency graphs to a data frame.
	 * @param jsc 
	 * @param graphs
	 * @param featureFrame
	 * @return a data frame
	 */
	private DataFrame toDataFrame(JavaSparkContext jsc, List<DependencyGraph> graphs, FeatureFrame featureFrame) {
		List<ParsingContext> list = new ArrayList<ParsingContext>();
		for (DependencyGraph graph : graphs) {
			List<ParsingContext> xy = TransitionDecoder.decode(graph, featureFrame);
			list.addAll(xy);
		}
		JavaRDD<ParsingContext> javaRDD = jsc.parallelize(list);
		return sqlContext.createDataFrame(javaRDD, ParsingContext.class);
	}
	
	/**
	 * Set the verbose mode.
	 * @param verbose
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	/**
	 * Creates a processing pipeline.
	 * @return a pipeline
	 */
	protected Pipeline createPipeline() {
		Tokenizer tokenizer = new Tokenizer()
			.setInputCol("text")
			.setOutputCol("tokens");
		CountVectorizer countVectorizer = new CountVectorizer()
			.setInputCol("tokens")
			.setOutputCol("features")
			.setMinDF((Double)params.getOrDefault(params.getMinFF()))
			.setVocabSize((Integer)params.getOrDefault(params.getNumFeatures()));  
		StringIndexer transitionIndexer = new StringIndexer()
			.setInputCol("transition")
			.setOutputCol("label");
		
		Pipeline pipeline = new Pipeline().setStages(new PipelineStage[]{tokenizer, countVectorizer, transitionIndexer});
		return pipeline;
	}

	/**
	 * Loads a MLP transition classifier from a file, which is pre-trained.
	 * @param jsc
	 * @param classifierFileName
	 */
	public static MultilayerPerceptronClassificationModel load(JavaSparkContext jsc, String classifierFileName) {
		Object object = jsc.objectFile(classifierFileName).first();
		return ((MultilayerPerceptronClassificationModel)object);
	}
	
}
