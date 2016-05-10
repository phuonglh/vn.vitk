package vn.vitk.dp;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Transformer;
import org.apache.spark.ml.feature.CountVectorizer;
import org.apache.spark.ml.feature.CountVectorizerModel;
import org.apache.spark.ml.feature.IndexToString;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;

import vn.vitk.util.SparkContextFactory;

/**
 * @author phuonglh, phuonglh@gmail.com
 * <p>
 * Transition classifier for deterministic dependency parsing. 
 * 
 */
public abstract class TransitionClassifier implements Serializable {

	private static final long serialVersionUID = -8638888022254374959L;

	protected SQLContext sqlContext;

	protected JavaSparkContext jsc;
	
	protected DataFrame trainingData;
	
	/**
	 * Number of distinct transition labels
	 */
	protected int K = 0;
	
	/**
	 * Number of distinct features
	 */
	protected int D = 0;
	
	/**
	 * The file name that the trained classifier is saved to. 
	 */
	protected String classifierFileName;
	
	protected int numFeatures = 0;

	
	/**
	 * Creates a transition classifier from a list of dependency graphs and 
	 * a feature frame.
	 * @param master
	 * @param graphs
	 * @param featureFrame
	 * @param classifierFileName
	 * @param transitionIndexFileName
	 * @param featureIndexFileName
	 * @param numFeatures
	 */
	public TransitionClassifier(String master, List<DependencyGraph> graphs,
			FeatureFrame featureFrame, String classifierFileName,
			String transitionIndexFileName, String featureIndexFileName, int numFeatures) {
		jsc = SparkContextFactory.create(master);
		// create a SQLContext
		//
		sqlContext = new SQLContext(jsc);
		// store the classifier file name
		//
		this.classifierFileName = classifierFileName;
		// extract a data frame from these graphs
		DataFrame dataset = toDataFrame(graphs, featureFrame);
		// segment the text to get tokens (features)
		//
		Tokenizer tokenizer = new Tokenizer().setInputCol("text").setOutputCol("tokens");
		DataFrame df0 = tokenizer.transform(dataset);
		// count tokens appears in at least c documents (that is, c parsing contexts) 
		// 
		CountVectorizer countVectorizer = new CountVectorizer()
			.setInputCol("tokens")
			.setOutputCol("features")
			.setMinDF(3)
			.setVocabSize(numFeatures); // try a fixed domain dimension for English (90K), for Vietnamese (30K) 
		CountVectorizerModel cvm = countVectorizer.fit(df0);
		DataFrame df1 = cvm.transform(df0);
		D = cvm.vocabulary().length; 
		// index the transition and get label (double type)
		// 
		StringIndexer si = new StringIndexer().setInputCol("transition").setOutputCol("label");
		StringIndexerModel sim = si.fit(df1);
		
		// create the training data frame
		trainingData = sim.transform(df1);
		trainingData.show();
		
		// compute the number of different labels, which is the maximum element 
		// in the 'label' column.
		trainingData.registerTempTable("dfTable");
		Row row = sqlContext.sql("SELECT MAX(label) as maxValue from dfTable").first();
		K = (int)row.getDouble(0);
		K++;
		this.numFeatures = numFeatures;
		// convert indexed labels (double) back to original labels (strings)
		IndexToString its = new IndexToString().setInputCol("label").setOutputCol("transitionName").setLabels(sim.labels());
		// save the transition index and the feature index
		try {
			its.write().overwrite().save(transitionIndexFileName + "-d" + numFeatures);
			cvm.write().overwrite().save(featureIndexFileName + "-d" + numFeatures);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Trains a transition classifier on the data frame.
	 * @param nHiddenUnits
	 * @return a transition classifier.
	 */
	public abstract Transformer train(int nHiddenUnits);
	
	/**
	 * Converts a dependency graph to a data frame.
	 * @param graph
	 * @param featureFrame
	 * @return a data frame
	 */
	public DataFrame toDataFrame(DependencyGraph graph, FeatureFrame featureFrame) {
		List<ParsingContext> list = TransitionDecoder.decode(graph, featureFrame);
		JavaRDD<ParsingContext> javaRDD = jsc.parallelize(list);
		return sqlContext.createDataFrame(javaRDD, ParsingContext.class);
	}

	/**
	 * Converts a list of dependency graphs to a data frame. 
	 * @param graphs
	 * @param featureFrame
	 * @return a data frame
	 */
	protected DataFrame toDataFrame(List<DependencyGraph> graphs, FeatureFrame featureFrame) {
		List<ParsingContext> list = new ArrayList<ParsingContext>();
		for (DependencyGraph graph : graphs) {
			List<ParsingContext> xy = TransitionDecoder.decode(graph, featureFrame);
			list.addAll(xy);
		}
		JavaRDD<ParsingContext> javaRDD = jsc.parallelize(list);
		return sqlContext.createDataFrame(javaRDD, ParsingContext.class);
	}
	
	public void close() {
		jsc.close();
	}
}
