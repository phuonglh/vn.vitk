package vn.vitk.dp;

import java.io.Serializable;
import java.util.List;


/**
 * @author Phuong Le-Hong <phuonglh@gmail.com>
 * <p>Jan 30, 2016, 10:36:01 AM
 * <p>
 * A dependency graph.
 */
public class DependencyGraph implements Serializable {
	
	private static final long serialVersionUID = -3230714950937126020L;
	
	private Sentence sentence;
	private Integer[] heads;
	private String[] labels;
	
	/**
	 * Creates a dependency graph.
	 * @param sentence
	 * @param heads
	 * @param labels
	 */
	public DependencyGraph(Sentence sentence, Integer[] heads, String[] labels) {
		this.sentence = sentence;
		this.heads = heads;
		this.labels = labels;
		if (sentence.length() != heads.length || heads.length != labels.length) {
			throw new IllegalArgumentException("Lengths do not match!");
		}
	}
	
	/**
	 * Creates a dependency graph.
	 * @param sentence
	 * @param arcs
	 */
	public DependencyGraph(Sentence sentence, List<Dependency> arcs) {
		this.sentence = sentence;
		int n = sentence.length();
		this.heads = new Integer[n];
		this.labels = new String[n];
		for (int i = 0; i < n; i++) {
			heads[i] = -1;
			labels[i] = "root";
		}
		// fill in 'heads' and 'labels'
		for (Dependency arc : arcs) {
			int u = arc.getHead();
			int v = arc.getDependent();
			heads[v] = u;
			labels[v] = arc.getType().substring(3); // "??-label" --> "label"
		}
	}
	/**
	 * Checks if the graph has arc (u, v) with any label.
	 * @param u
	 * @param v
	 * @return <code>true</code> or <code>false</code>
	 */
	public boolean hasArc(int u, int v) {
		if (u < 0 || u >= sentence.length() || v < 0 || v >= sentence.length()) {
			throw new IllegalArgumentException("Invalid index!");
		}
		return (heads[v] == u);
	}
	
	/**
	 * Checks the projectivity of this graph.
	 * @return <code>true</code> or <code>false</code>
	 */
	public boolean isProjective() {
		// TODO:
		return true;
	}
	
	/**
	 * Checks the connectivity of this graph.
	 * @return <code>true</code> or <code>false</code>
	 */
	public boolean isConnected() {
		// TODO:
		return true;
	}
	

	public Sentence getSentence() {
		return sentence;
	}
	
	public Integer[] getHeads() {
		return heads;
	}
	
	public String[] getLabels() {
		return labels;
	}
	
	/**
	 * Gets the root position of the graph.
	 * @return the head position.
	 */
	public int getRoot() {
		for (int j = 0; j < heads.length; j++)
			if (heads[j] == 0)
				return j;
		return -1;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(512);
		int n = sentence.length();
		for (int j = 0; j < n; j++) {
			sb.append(j);
			sb.append('\t');
			sb.append(sentence.getToken(j));
			sb.append('\t');
			sb.append(sentence.getTag(j));
			sb.append('\t');
			sb.append(heads[j]);
			sb.append('\t');
			sb.append(labels[j]);
			sb.append('\n');
		}
		return sb.toString();
	}
}
