package vn.vitk.dp;


import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel;
import org.apache.spark.ml.feature.CountVectorizerModel;
import org.apache.spark.ml.feature.IndexToString;

import vn.vitk.lang.CorpusPack;
import vn.vitk.lang.Language;
import vn.vitk.util.SparkContextFactory;

/**
 * @author phuonglh, phuonglh@gmail.com
 * <p>
 * Trains a MLP transition classifier on a dependency corpus.
 */
public class TransitionClassifierMLPFactory {
	
	static void load(String master, Language language) {
		JavaSparkContext jsc = SparkContextFactory.create(master);
		CorpusPack pack = new CorpusPack(language);

		MultilayerPerceptronClassificationModel mlpc = TransitionClassifierMLP.load(jsc, pack.dependencyClassifierFileName());
		System.out.println("Tranformer loaded.");
		System.out.println(mlpc.getLabelCol());
		System.out.println(mlpc.getFeaturesCol());
		System.out.println(mlpc.numFeatures());
		int[] layers = mlpc.layers();
		for (int n : layers) {
			System.out.print(n + "; ");
		}
		
		IndexToString sts = IndexToString.load(pack.dependencyTransitionIndexFileName());
		String[] transitionName = sts.getLabels();
		System.out.println("Number of transition = " + transitionName.length);
		for (String t : transitionName)
			System.out.println(t);
		
		CountVectorizerModel cvm = CountVectorizerModel.load(pack.dependencyFeatureIndexFileName());
		System.out.println("Feature vocabulary size = " + cvm.vocabulary().length);
		jsc.close();
	}
	
	static void train(String master, Language language, int numFeatures, int numHiddenUnits) {
		CorpusPack pack = new CorpusPack(language);
		List<DependencyGraph> graphs = pack.dependencyTreebankTraining();
		TransitionClassifier classifier = new TransitionClassifierMLP(master, graphs, 
				FeatureFrame.TAG_TOKEN_TYPE, 
				pack.dependencyClassifierFileName(), 
				pack.dependencyTransitionIndexFileName(), 
				pack.dependencyFeatureIndexFileName(),
				numFeatures);
		classifier.train(numHiddenUnits);
		System.out.println("Done training.");
		classifier.close();
	}
	
	public static void main(String[] args) {
		String language = "ENGLISH";
		String master = "local[*]";
		int numFeatures = 10000; 
		int numHiddenUnits = 0;
		
		Options options = new Options();
		options.addOption("master", true, "master");
		options.addOption("language", true, "language");
		options.addOption("f", true, "number of features (vocabSize)");
		options.addOption("h", true, "number of hidden units");
		CommandLineParser parser = new PosixParser();
		CommandLine cm;
		try {
			cm = parser.parse(options, args);
			if (cm.hasOption("master")) {
				master = cm.getOptionValue("master");
			}
			if (cm.hasOption("language")) {
				language = cm.getOptionValue("language");
			}
			if (cm.hasOption("f")) {
				numFeatures = Integer.parseInt(cm.getOptionValue("f"));
			}
			if (cm.hasOption("h")) {
				numHiddenUnits = Integer.parseInt(cm.getOptionValue("h"));
			}
			train(master, Language.valueOf(language.toUpperCase()), numFeatures, numHiddenUnits);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
	}
}
