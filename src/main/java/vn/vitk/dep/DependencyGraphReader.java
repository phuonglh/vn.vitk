package vn.vitk.dep;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Phuong LE-HONG
 *         <p>
 *         Reads dependency graphs in some format (for example, CoNLL-X,
 *         CoNLL-U) from a file.
 * 
 */
public class DependencyGraphReader {


	/**
	 * Reads a list of dependency graphs from a text file.
	 * @param fileName
	 * @param format either 'x' or 'u' (universal) 
	 * @return a list of dependency graphs.
	 */
	public static List<DependencyGraph> read(String fileName, char format) {
		if (format == 'x')
			return readCoNLLX(fileName);
		else if (format == 'u') 
			return readCoNLLU(fileName);
		return null;
	}
	
	/**
	 * Reads a list of dependency graphs from a text file.
	 * @param fileName
	 * @return a list of dependency graphs.
	 */
	private static List<DependencyGraph> readCoNLLX(String fileName) {
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
			labels.add("NULL");
			
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
					labels.add("NULL");
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
	private static List<DependencyGraph> readCoNLLU(String fileName) {
		List<DependencyGraph> graphs = new ArrayList<DependencyGraph>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
			String line = null;
			List<String> tokens = new LinkedList<String>();
			tokens.add("ROOT");
			List<String> lemmas = new LinkedList<String>();
			lemmas.add("ROOT");
			List<String> tags = new LinkedList<String>();
			tags.add("ROOT");
			List<Integer> heads = new LinkedList<Integer>();
			heads.add(-1);
			List<String> labels = new LinkedList<String>();
			labels.add("NULL");
			
			while ((line = br.readLine()) != null) {
				if (line.trim().length() == 0) {
					// a new graph
					int n = tokens.size();
					Sentence sentence = new Sentence(
							tokens.toArray(new String[n]),
							lemmas.toArray(new String[n]),
							tags.toArray(new String[n]));
					DependencyGraph graph = new DependencyGraph(sentence, heads.toArray(new Integer[n]), labels.toArray(new String[n]));
					graphs.add(graph);
					tokens = new LinkedList<String>();
					tokens.add("ROOT");
					lemmas = new LinkedList<String>();
					lemmas.add("ROOT");
					tags = new LinkedList<String>();
					tags.add("ROOT");
					heads = new LinkedList<Integer>();
					heads.add(-1);
					labels = new LinkedList<String>();
					labels.add("NULL");
				} else {
					// construct a graph
					String[] parts = line.split("\\s+");
					if (parts.length != 10) {
						throw new IllegalArgumentException("Bad file format! " + line);
					}
					tokens.add(parts[1]);// word form or punctuation symbol
					lemmas.add(parts[2]); // lemma
					tags.add(parts[3]); // universal PoS tag  
					heads.add(new Integer(parts[6])); // head of the current token
					labels.add(parts[7]); // universal dependency relation to the head (or root iff head==0)
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
