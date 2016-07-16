package vn.vitk.dep;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import org.apache.spark.sql.DataFrame;

/**
 * @author Phuong Le-Hong <phuonglh@gmail.com>
 *         <p>
 *         Jan 30, 2016, 9:02:58 PM
 *         <p>
 *         First, decodes manually-created dependency graphs to extract the
 *         parsing contexts and their corresponding transitions, then converts
 *         the data to a {@link DataFrame} for use in the SparkML library.
 * 
 */
public class TransitionDecoder implements Serializable {

	private static final long serialVersionUID = 5296031009769814684L;
	/**
	 * Parsing context identifier which is automatically incremented.
	 */
	private static int id = 0;
	
	/**
	 * Derives a transition sequence from this dependency graph. This 
	 * is used to reconstruct the parsing process
	 * @param graph a dependency graph
	 * @param featureFrame
	 * @return a list of labeled parsing context.
	 */
	public static List<ParsingContext> decode(DependencyGraph graph, FeatureFrame featureFrame) {
		List<ParsingContext> data = new ArrayList<ParsingContext>();
		Stack<Integer> stack = new Stack<Integer>();
		Queue<Integer> queue = new LinkedList<Integer>();
		for (int i = 0; i < graph.getSentence().length(); i++) 
			queue.add(i);
		
		List<Dependency> currentArcs = new ArrayList<Dependency>();
		FeatureExtractor featureBuilder = new FeatureExtractor(featureFrame);
		// the first transition is always a SHIFT
		Configuration config = new Configuration(graph.getSentence(), stack, queue);
		config = config.next("SH");
		while (!config.isFinal()) {
			// extract feature strings
			List<String> features = featureBuilder.extract(config);
			StringBuilder text = new StringBuilder();
			for (String f : features) {
				text.append(f);
				text.append(' ');
			}
			// determine the transition for this configuration
			Integer u = stack.peek();
			Integer v = queue.peek();
			String transition = "";
			if (graph.hasArc(v, u)) { // LA
				transition = "LA-" + graph.getLabels()[u];
				currentArcs.add(new Dependency(v, u, transition));
			} else if (graph.hasArc(u, v)) { // RA
				transition = "RA-" + graph.getLabels()[v];
				currentArcs.add(new Dependency(u, v, transition));
			} else if (config.isReducible()) {
				transition = "RE";
			} else {
				transition = "SH";
			}
			
			// create a data point (a JavaBean)
			ParsingContext pc = new ParsingContext();
			pc.setId(id);
			pc.setText(text.toString().trim());
			pc.setTransition(transition);
			data.add(pc);
			id++;
			// proceed to the next configuration
			config = config.next(transition);
		}
		return data;
	}
}
