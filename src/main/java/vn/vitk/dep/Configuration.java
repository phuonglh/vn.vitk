package vn.vitk.dep;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

/**
 * @author Phuong Le-Hong <phuonglh@gmail.com>
 * <p>Jan 29, 2016, 8:10:19 PM
 * <p>
 * A configuration in the transition-based dependency parsing technique.
 */
public class Configuration {
	private Sentence sentence;
	private Stack<Integer> stack;
	private Queue<Integer> queue;
	private List<Dependency> arcs;
	
	/**
	 * Constructs a configuration: (sentence, stack, buffer, arcs)
	 * @param sentence
	 * @param stack
	 * @param queue
	 * @param arcs
	 */
	public Configuration(Sentence sentence, Stack<Integer> stack, Queue<Integer> queue, List<Dependency> arcs) {
		this.sentence = sentence;
		this.stack = stack;
		this.queue = queue;
		this.arcs = arcs;
	}

	/**
	 * Constructs a configuration: (sentence, stack, buffer, arcs)
	 * @param sentence
	 * @param stack
	 * @param queue
	 */
	public Configuration(Sentence sentence, Stack<Integer> stack, Queue<Integer> queue) {
		this.sentence = sentence;
		this.stack = stack;
		this.queue = queue;
		this.arcs = new ArrayList<Dependency>();
	}
	
	/**
	 * Constructs a configuration.
	 * @param sentence
	 */
	public Configuration(Sentence sentence) {
		this(sentence, new Stack<Integer>(), new LinkedList<Integer>(), new ArrayList<Dependency>());
	}
	
	/**
	 * Gets the sentence.
	 * @return the sentence.
	 */
	public Sentence getSentence() {
		return sentence;
	}
	
	/**
	 * Gets the underlying stack.
	 * @return the stack
	 */
	public Stack<Integer> getStack() {
		return stack;
	}
	
	/**
	 * Gets the underlying queue.
	 * @return queue
	 */
	public Queue<Integer> getQueue() {
		return queue;
	}

	/**
	 * Gets the dependency list.
	 * @return list of dependencies.
	 */
	public List<Dependency> getArcs() {
		return arcs;
	}
	
	/**
	 * Computes the next configuration given a transition.
	 * @param transition
	 * @return a configuration
	 */
	public Configuration next(String transition) {
		if (transition.equals("SH")) {
			stack.push(queue.remove());
		} else if (transition.equals("RE")) {
			stack.pop();
		} else if (transition.startsWith("LA")) {
			Integer u = stack.pop();
			Integer v = queue.peek();
			String label = transition.substring(3);
			addDependency(new Dependency(v, u, label));
		} else if (transition.startsWith("RA")) {
			Integer u = stack.peek();
			Integer v = queue.remove();
			stack.push(v);
			String label = transition.substring(3);
			addDependency(new Dependency(u, v, label));
		}
		return new Configuration(sentence, stack, queue, arcs);
	}
	
	/**
	 * Adds a dependency to this configuration.
	 * @param dependency
	 */
	public void addDependency(Dependency dependency) {
		this.arcs.add(dependency);
	}
	
	public boolean isReducible() {
		Integer u = stack.peek();
		for (Dependency dependency : arcs) {
			if (dependency.getDependent() == u)
				return true;
		}
		return false;
	}
	
	/**
	 * Tests the final property of this configuration.
	 * @return <code>true</code> or <code>false</code>
	 */
	public boolean isFinal() {
		return queue.isEmpty();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("s=[");
		for (int i = 0; i < stack.size(); i++) {
			sb.append(stack.elementAt(i));
			sb.append(':');
			sb.append(sentence.getToken(stack.elementAt(i)));
			if (i < stack.size()-1) 
				sb.append(' ');
		}
		sb.append("]\n");
		sb.append("q=[");
		Integer[] q = queue.toArray(new Integer[queue.size()]);
		for (int i = 0; i < queue.size(); i++) {
			sb.append(q[i]);
			sb.append(':');
			sb.append(sentence.getToken(q[i]));
			if (i < queue.size()-1) 
				sb.append(' ');
		}
		sb.append("]\n");
		sb.append("a=[");
		for (int i = 0; i < arcs.size(); i++) {
			Dependency dependency = arcs.get(i);
			sb.append(dependency.toString());
			if (i < arcs.size()-1)
				sb.append(',');
		}
		sb.append(']');
		return sb.toString();
	}
	
}
