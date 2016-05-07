package vn.vitk.dp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Transformer;
import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel;
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.sql.DataFrame;

/**
 * @author phuonglh, phuonglh@gmail.com
 * <p>
 * A MLP classifier. 
 */
public class TransitionClassifierMLP extends TransitionClassifier {
	
	private static final long serialVersionUID = -5601913295074675947L;
	private Logger logger = Logger.getLogger(TransitionClassifierMLP.class.getName());

	/**
	 * Creates a MLP transition classifier from a training dataset containing a list of
	 * dependency graphs. The classifier is then saved to a file.
	 * @param master 
	 * @param graphs
	 * @param featureFrame
	 * @param classifierFileName
	 * @param transitionIndexFileName
	 * @param featureIndexFileName
	 * @param numFeatures
	 */
	public TransitionClassifierMLP(String master, List<DependencyGraph> graphs,
			FeatureFrame featureFrame, String classifierFileName,
			String transitionIndexFileName, String featureIndexFileName, int numFeatures) {
		super(master, graphs, featureFrame, classifierFileName, transitionIndexFileName, featureIndexFileName, numFeatures);
		
		// initialize logger
		//
		FileHandler handler;
		try {
			handler = new FileHandler("transitionClassifier.log");
			handler.setFormatter(new SimpleFormatter());
			logger.addHandler(handler);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public Transformer train(int nHiddenUnits) {
		// specify parameters for the neural network
		// 
		System.out.println("N = " + trainingData.count());
		System.out.println("D = " + D);
		System.out.println("K = " + K);
		int[] layers = {D, K};
		if (nHiddenUnits > 0) {
			layers = new int[3];
			layers[0] = D;
			layers[1] = nHiddenUnits;
			layers[2] = K;
		}
		MultilayerPerceptronClassifier classifier = new MultilayerPerceptronClassifier()
			.setLayers(layers)
			.setBlockSize(128)
			.setSeed(1234L)
			.setMaxIter(100);
		MultilayerPerceptronClassificationModel model = classifier.fit(trainingData);
		
		// save the model
		//
		String suffix = "-d" + numFeatures;
		if (nHiddenUnits > 0)
			suffix = suffix + "-h" + nHiddenUnits;
		jsc.parallelize(Arrays.asList(model), 1).saveAsObjectFile(classifierFileName + suffix);
		// compute precision on the training data
		//
		DataFrame result = model.transform(trainingData);
		DataFrame predictionAndLabel = result.select("prediction", "label");
		MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator().setMetricName("precision");
		logger.log(Level.INFO, "training precision = " + evaluator.evaluate(predictionAndLabel));
		
		return model;
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
