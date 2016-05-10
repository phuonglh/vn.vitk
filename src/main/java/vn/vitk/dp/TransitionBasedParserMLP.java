package vn.vitk.dp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel;
import org.apache.spark.ml.feature.CountVectorizerModel;
import org.apache.spark.ml.feature.IndexToString;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;

import vn.vitk.util.SparkContextFactory;

/**
 * @author phuonglh, phuonglh@gmail.com
 * <p>
 * A transition parser which uses a multi-layer perceptron classifier 
 * to predict the best transition from a given parsing context. 
 */
public class TransitionBasedParserMLP extends DependencyParser {

	private MultilayerPerceptronClassificationModel classifier;
	private CountVectorizerModel countVectorizerModel;
	private FeatureFrame featureFrame;
	private SQLContext sqlContext;
	private String[] transitionName;
	
	/**
	 * Creates a multi-layer perceptron transition-based parser.
	 * @param master
	 * @param classifierFileName
	 * @param transitionIndexFileName
	 * @param featureIndexFileName
	 * @param featureFrame
	 */
	public TransitionBasedParserMLP(String master, String classifierFileName, String transitionIndexFileName, String featureIndexFileName, FeatureFrame featureFrame) {
		this.featureFrame = featureFrame;
		JavaSparkContext jsc = SparkContextFactory.create(master);
		sqlContext = new SQLContext(jsc);
		// load the transition classifier
		//
		classifier = TransitionClassifierMLP.load(jsc, classifierFileName);
		// load the transition index
		//
		transitionName = IndexToString.load(transitionIndexFileName).getLabels();
		// load the feature index
		countVectorizerModel = CountVectorizerModel.load(featureIndexFileName);
	}

	/* (non-Javadoc)
	 * @see vn.vitk.dp.DependencyParser#parse(vn.vitk.dp.Sentence)
	 */
	@Override
	public DependencyGraph parse(Sentence sentence) {
		Stack<Integer> stack = new Stack<Integer>();
		Queue<Integer> queue = new LinkedList<Integer>();
		for (int i = 0; i < sentence.length(); i++) 
			queue.add(i);
		
		List<Dependency> arcs = new ArrayList<Dependency>();
		FeatureExtractor featureBuilder = new FeatureExtractor(featureFrame);
		// the first transition is always a SHIFT
		Configuration config = new Configuration(sentence, stack, queue);
		config = config.next("SH");
		
		while (!config.isFinal()) {
			// extract feature strings
			List<String> features = featureBuilder.extract(config);
			StringBuilder text = new StringBuilder();
			for (String f : features) {
				text.append(f);
				text.append(' ');
			}
			List<ParsingContext> list = new ArrayList<ParsingContext>();
			ParsingContext pc = new ParsingContext();
			pc.setId(-1);
			pc.setText(text.toString().trim());
			pc.setTransition("NULL");
			list.add(pc);
			DataFrame input = sqlContext.createDataFrame(list, ParsingContext.class);
			// segment the text to get tokens (features)
			//
			Tokenizer tokenizer = new Tokenizer().setInputCol("text").setOutputCol("tokens");
			DataFrame df0 = tokenizer.transform(input);
			// apply the count vectorizer model which is the same as that used in the training stage
			DataFrame df1 = countVectorizerModel.transform(df0);
			// classify the data frame
			DataFrame output = classifier.transform(df1);
			
			// get the prediction result
			double label = output.select("prediction").collect()[0].getDouble(0);
			String transition = transitionName[(int)label];
			// add the arc if it is a LA or RA transition
			Integer u = stack.peek();
			Integer v = queue.peek();
			if (transition.startsWith("LA")) { 
				arcs.add(new Dependency(v, u, transition));
			} else if (transition.startsWith("RA")) {
				arcs.add(new Dependency(u, v, transition));
			} else if (transition.equals("RE") && !config.isReducible()) {
				System.err.println("Predicted transition is REDUCE while the current configuration is not reducible");
			}
			// proceed to the next configuration
			config = config.next(transition);
		}
		return new DependencyGraph(sentence, arcs);
	}

	
}
