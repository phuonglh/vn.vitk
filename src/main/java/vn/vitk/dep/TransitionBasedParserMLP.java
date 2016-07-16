package vn.vitk.dep;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.classification.MultilayerPerceptronClassificationModel;
import org.apache.spark.ml.feature.CountVectorizerModel;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;

import scala.Tuple2;

/**
 * @author phuonglh, phuonglh@gmail.com
 *         <p>
 *         A transition parser which uses a multi-layer perceptron (MLP)
 *         classifier to predict the best transition given a parsing
 *         context.
 */
public class TransitionBasedParserMLP extends DependencyParser implements Serializable {

	private static final long serialVersionUID = -2300120949325582286L;
	
	private MultilayerPerceptronClassificationModel classifier;
	private PipelineModel pipelineModel;
	private FeatureFrame featureFrame;
	private String[] transitionName;
	private final Map<String, Integer> featureMap;
	
	/**
	 * Creates a transition-based parser using a MLP transition classifier.
	 * @param jsc
	 * @param classifierFileName
	 * @param featureFrame
	 */
	public TransitionBasedParserMLP(JavaSparkContext jsc, String classifierFileName, FeatureFrame featureFrame) {
		this.featureFrame = featureFrame;
		this.classifier = TransitionClassifier.load(jsc, new Path(classifierFileName, "data").toString());
		this.pipelineModel = PipelineModel.load(new Path(classifierFileName, "pipelineModel").toString());
		this.transitionName = ((StringIndexerModel)pipelineModel.stages()[2]).labels();
		String[] features = ((CountVectorizerModel)(pipelineModel.stages()[1])).vocabulary();
		this.featureMap = new HashMap<String, Integer>();
		for (int j = 0; j < features.length; j++) {
			this.featureMap.put(features[j], j);
		}
		
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
			List<String> featureList = featureBuilder.extract(config);
			String[] fs = new String[featureList.size()];
			for (int j = 0; j < featureList.size(); j++) {
				fs[j] = featureList.get(j).toLowerCase();
			}
			List<Tuple2<Integer, Double>> x = new LinkedList<Tuple2<Integer, Double>>();
			for (String f : fs) {
				Integer i = featureMap.get(f);
				if (i != null) {
					x.add(new Tuple2<Integer, Double>(i, 1.0));
				}
			}
			Vector features = Vectors.sparse(featureMap.size(), x);
			double label = classifier.predict(features);
			
			String transition = transitionName[(int)label];
			// add the arc if it is a LA or RA transition
			// 
			Integer u = stack.peek();
			Integer v = queue.peek();
			if (transition.startsWith("LA")) { 
				arcs.add(new Dependency(v, u, transition));
			} else if (transition.startsWith("RA")) {
				arcs.add(new Dependency(u, v, transition));
			} else if (transition.equals("RE") && !config.isReducible()) {
				System.err.println("Predicted transition is REDUCE while the current configuration is not reducible");
			}
			config = config.next(transition);
		}
		return new DependencyGraph(sentence, arcs);
	}
	
}
