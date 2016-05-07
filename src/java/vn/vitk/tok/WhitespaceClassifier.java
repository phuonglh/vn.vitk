package vn.vitk.tok;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.LogisticRegression;
import org.apache.spark.ml.classification.LogisticRegressionModel;
import org.apache.spark.ml.classification.LogisticRegressionTrainingSummary;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;

import vn.vitk.util.SparkContextFactory;

/**
 * @author Phuong LE-HONG, <phuonglh@gmail.com>
 * <p>
 * Apr 25, 2016, 4:51:27 PM
 * <p>
 * Whitespace classifier with logistic regression.
 */
public class WhitespaceClassifier implements Serializable {
	private static final long serialVersionUID = 3100732272743758977L;
	private transient JavaSparkContext jsc = SparkContextFactory.create();
	private SQLContext sqlContext;
	private Lexicon lexicon;
	private PipelineModel model;
	private Map<String, Pattern> patterns = new TreeMap<String, Pattern>();
	
	public WhitespaceClassifier(String lexiconFileName, String regexpFileName) {
		sqlContext = new SQLContext(jsc);
		lexicon = new Lexicon().load(lexiconFileName);
		List<String> lines = jsc.textFile(regexpFileName).collect();
		for (String line : lines) {
			line = line.trim();
			if (!line.startsWith("#")) { // ignore comment lines
				String[] s = line.split("\\s+");
				if (s.length == 2) {
					patterns.put(s[0], Pattern.compile(s[1]));
				}
			}
		}
	}
	
	public WhitespaceClassifier(Lexicon lexicon, Map<String, Pattern> patterns) {
		sqlContext = new SQLContext(jsc);
		this.lexicon = lexicon;
		this.patterns = patterns;
	}
	
	/**
	 * Trains a whitespace classifier model and save the resulting pipeline model
	 * to an external file. 
	 * @param sentences a list of tokenized sentences.
	 * @param pipelineModelFileName
	 * @param numFeatures
	 */
	public void train(List<String> sentences, String pipelineModelFileName, int numFeatures) {
		List<WhitespaceContext> contexts = new ArrayList<WhitespaceContext>(sentences.size());
		int id = 0;
		for (String sentence : sentences) {
			sentence = sentence.trim();
			for (int j = 0; j < sentence.length(); j++) {
				char c = sentence.charAt(j);
				if (c == ' ' || c == '_') {
					WhitespaceContext context = new WhitespaceContext();
					context.setId(id++);
					context.setContext(extractContext(sentence, j));
					context.setLabel(c == ' ' ? 0d : 1d);
					contexts.add(context);
				}
			}
		}
		JavaRDD<WhitespaceContext> jrdd = jsc.parallelize(contexts);
		DataFrame df = sqlContext.createDataFrame(jrdd, WhitespaceContext.class);
		df.show(false);
		System.out.println("N = " + df.count());
		df.groupBy("label").count().show();
		
		org.apache.spark.ml.feature.Tokenizer tokenizer = new Tokenizer()
				.setInputCol("context").setOutputCol("words");
		HashingTF hashingTF = new HashingTF().setNumFeatures(numFeatures)
				.setInputCol(tokenizer.getOutputCol()).setOutputCol("features");
		LogisticRegression lr = new LogisticRegression().setMaxIter(100)
				.setRegParam(0.01);
		Pipeline pipeline = new Pipeline().setStages(new PipelineStage[] {
				tokenizer, hashingTF, lr });
		model = pipeline.fit(df);
		
		try {
			model.write().overwrite().save(pipelineModelFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		DataFrame predictions = model.transform(df);
		predictions.show();
		MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator().setMetricName("precision");
		double accuracy = evaluator.evaluate(predictions);
		System.out.println("training accuracy = " + accuracy);
		
		LogisticRegressionModel lrModel = (LogisticRegressionModel) model.stages()[2];
		LogisticRegressionTrainingSummary trainingSummary = lrModel.summary();
		double[] objectiveHistory = trainingSummary.objectiveHistory();
		System.out.println("#(iterations) = " + objectiveHistory.length);
		for (double lossPerIteration : objectiveHistory) {
		  System.out.println(lossPerIteration);
		}
		
	}
	
	/**
	 * Trains a whitespace classifier model and save the resulting pipeline model
	 * to an external file. 
	 * @param corpusFileName
	 * @param pipelineModelFileName
	 * @param numFeatures
	 */
	public void train(String corpusFileName, String pipelineModelFileName, int numFeatures) {
		JavaRDD<String> jrdd = jsc.textFile(corpusFileName);
		train(jrdd.collect(), pipelineModelFileName, numFeatures);
	}
	
	public void printModel() {
		LogisticRegressionModel lrModel = (LogisticRegressionModel) model.stages()[2];
		System.out.println("intercept = " + lrModel.intercept());
		System.out.println("number of features = " + lrModel.numFeatures());
		System.out.println("regularization parameter = " + lrModel.getRegParam());
		System.out.println(lrModel.explainParams());
	}
	
	/**
	 * Extracts a context string for each whitespace position. 
	 * @param s a phrase
	 * @param position
	 * @return a context string where features are separated by whitespace
	 */
	private String extractContext(String s, int position) {
		StringBuilder sb = new StringBuilder();
		// f1: the previous syllable
		//
		int j = position-1;
		char c = s.charAt(j);
		while (j > 0 && c != ' ' && c != '_') {
			c = s.charAt(--j);
		}
		String ps = j > 0 ? s.substring(j+1, position) : s.substring(0, position);
		ps = convert(ps);
		sb.append("ps:");
		sb.append(ps);
		// f2: is the previous syllable a word?
		// 
		sb.append(" pw:");
		sb.append(lexicon.hasWord(ps) ? 1 : 0);
		// f3: the next syllable
		//
		j = position + 1;
		c = s.charAt(j);
		while (j < s.length() && c != ' ' && c != '_') {
			c = s.charAt(j++);
		}
		String ns = j < s.length() ? s.substring(position+1, j-1) : s.substring(position+1);
		ns = convert(ns);
		sb.append(" ns:");
		sb.append(ns);
		// f4: is the next syllable a word?
		// 
		sb.append(" nw:");
		sb.append(lexicon.hasWord(ns) ? 1 : 0);
		// f5: joint feature
		// 
		sb.append(" ps+ns:");
		sb.append(ps);
		sb.append('+');
		sb.append(ns);
		
		return sb.toString();
	}
	
	private String convert(String token) {
		for (String type : patterns.keySet()) {
			Pattern pattern = patterns.get(type);
			String t = token.replace('_', ' ');
			if (pattern.matcher(t).matches()) {
				if (type.contains("entity"))
					return "ENTITY";
				else if (type.contains("name"))
					return "NAME";
				else if (type.contains("email"))
					return "EMAIL";
				else if (type.contains("date"))
					return "DATE";
				else if (type.contains("hour"))
					return "HOUR";
				else if (type.contains("number"))
					return "NUMBER";
				else if (type.contains("fraction"))
					return "FRACTION";
				else if (type.contains("punctuation"))
					return "PUNCT";
			}
		}
		return token.toLowerCase();
	}

	
	public List<WhitespaceContext> makeContexts(String phrase) {
		List<WhitespaceContext> contexts = new LinkedList<WhitespaceContext>();
		for (int j = 0; j < phrase.length(); j++) {
			if (phrase.charAt(j) == ' ') {
				WhitespaceContext context = new WhitespaceContext();
				context.setId(-1);
				context.setContext(extractContext(phrase, j));
				context.setLabel(-1d);
				contexts.add(context);
			}
		}
		return contexts;
	}
	
	/**
	 * Loads a pipeline model from an external file.
	 * @param pipelineModelFileName
	 * @return a pipeline model.
	 */
	public PipelineModel load(String pipelineModelFileName) {
		model = PipelineModel.load(pipelineModelFileName);
		return model;
	}
	
	public static void main(String[] args) {
		WhitespaceClassifier classifier = new WhitespaceClassifier("dat/tok/lexicon.xml", "dat/tok/regexp.txt");
		String modelFileName = "dat/tok/whitespace.model"; 
		// test 1: train a whitespace classifier
		//
//		String corpusFileName = "dat/syllables2M.seg.txt";
//		classifier.train(corpusFileName, "dat/tok/whitespace.model", 250000);
		// test 2: load a whitespace classifier
		//
		classifier.load(modelFileName);
		classifier.printModel();
	}
}
