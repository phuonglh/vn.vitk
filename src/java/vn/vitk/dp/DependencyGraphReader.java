package vn.vitk.dp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * @author phuonglh, phuonglh@gmail.com
 * <p>
 * Reads depdendency graphs in some format (for example, CoNLL-X, CoNLL-U) from a file.
 * 
 */
public class DependencyGraphReader {
	

	/**
	 * Reads a list of dependency graphs from a text file.
	 * @param fileName
	 * @param dependencyFormat
	 * @return a list of dependency graphs.
	 */
	public static List<DependencyGraph> read(String fileName, DependencyFormat dependencyFormat) {
		// TODO:
		return null;
	}
	
	/**
	 * Reads a list of dependency graphs from a text file.
	 * @param fileName
	 * @return a list of dependency graphs.
	 */
	public static List<DependencyGraph> read(String fileName) {
		List<DependencyGraph> graphs = new ArrayList<DependencyGraph>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
			String line = null;
			List<String> tokens = new ArrayList<String>();
			tokens.add("ROOT");
			List<String> tags = new ArrayList<String>();
			tags.add("ROOT");
			List<Integer> heads = new ArrayList<Integer>();
			heads.add(-1);
			List<String> labels = new ArrayList<String>();
			labels.add("");
			
			while ((line = br.readLine()) != null) {
				if (line.trim().length() == 0) {
					int n = tokens.size();
					Sentence sentence = new Sentence(tokens.toArray(new String[n]), tags.toArray(new String[n]));
					DependencyGraph graph = new DependencyGraph(sentence, heads.toArray(new Integer[n]), labels.toArray(new String[n]));
					graphs.add(graph);
					tokens = new ArrayList<String>();
					tokens.add("ROOT");
					tags = new ArrayList<String>();
					tags.add("ROOT");
					heads = new ArrayList<Integer>();
					heads.add(-1);
					labels = new ArrayList<String>();
					labels.add("");
				} else {
					String[] parts = line.split("\\s+");
					if (parts.length != 10) {
						throw new IllegalArgumentException("Bad file format! " + line);
					}
					tokens.add(parts[1]);
					tags.add(parts[4]);
					heads.add(new Integer(parts[6]));
					labels.add(parts[7]);
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return graphs;
	}
	
	/**
	 * Reads a list of dependency graphs from a text file.
	 * @param fileName CoNLL-U format.
	 * @return a list of dependency graphs.
	 */
	public static List<DependencyGraph> readCoNLLU(String fileName) {
		List<DependencyGraph> graphs = new ArrayList<DependencyGraph>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
			String line = null;
			List<String> tokens = new ArrayList<String>();
			tokens.add("ROOT");
			List<String> tags = new ArrayList<String>();
			tags.add("ROOT");
			List<Integer> heads = new ArrayList<Integer>();
			heads.add(-1);
			List<String> labels = new ArrayList<String>();
			labels.add("");
			
			while ((line = br.readLine()) != null) {
				if (line.trim().length() == 0) {
					// a new graph
					int n = tokens.size();
					Sentence sentence = new Sentence(tokens.toArray(new String[n]), tags.toArray(new String[n]));
					DependencyGraph graph = new DependencyGraph(sentence, heads.toArray(new Integer[n]), labels.toArray(new String[n]));
					graphs.add(graph);
					tokens = new ArrayList<String>();
					tokens.add("ROOT");
					tags = new ArrayList<String>();
					tags.add("ROOT");
					heads = new ArrayList<Integer>();
					heads.add(-1);
					labels = new ArrayList<String>();
					labels.add("");
				} else {
					// construct a graph
					String[] parts = line.split("\\s+");
					if (parts.length != 10) {
						throw new IllegalArgumentException("Bad file format! " + line);
					}
					tokens.add(parts[1]);// word form or punctuation symbol
					tags.add(parts[3]); // universal PoS tag  
					heads.add(new Integer(parts[6])); // head of the current token
					labels.add(parts[7]); // universal dependency relation to the head (or root iff head=0)
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return graphs;
	}
	
}
