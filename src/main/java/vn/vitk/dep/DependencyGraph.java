package vn.vitk.dep;

import java.io.Serializable;
import java.util.List;


/**
 * @author Phuong Le-Hong <phuonglh@gmail.com>
 * <p>Jan 30, 2016, 10:36:01 AM
 * <p>
 * A dependency graph.
 */
public final class DependencyGraph implements Serializable {
	
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
	 * Updates the dependency label at a position.
	 * @param position
	 * @param label
	 */
	public void setLabel(int position, String label) {
		labels[position] = label;
	}
	
	/**
	 * Updates the head at a position
	 * @param position
	 * @param head
	 */
	public void setHead(int position, int head) {
		heads[position] = head;
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
			sb.append(sentence.getLemma(j));
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
	
	/**
	 * Generates a flattened list of dependency relations containing label(src-idx, tar-idx) tuples, separated by the whitespace character.
	 * @return a string
	 */
	public String dependencies() {
		StringBuilder sb = new StringBuilder();
		int n = sentence.length();
		for (int j = 0; j < n; j++) {
			if (heads[j] >= 0) {
				sb.append(labels[j]);
				sb.append('(');
				sb.append(sentence.getToken(heads[j]));
				sb.append('-');
				sb.append(heads[j]);
				sb.append(',');
				sb.append(sentence.getToken(j));
				sb.append('-');
				sb.append(j);
				sb.append(')');
				sb.append(' ');
			}
		}		
		return sb.toString().trim();
	}
}
