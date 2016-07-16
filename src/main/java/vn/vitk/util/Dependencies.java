package vn.vitk.util;

import java.util.LinkedList;
import java.util.List;

import vn.vitk.dep.DependencyGraph;
import vn.vitk.dep.DependencyGraphReader;
import vn.vitk.dep.Sentence;

/**
 * @author Phuong LE-HONG
 * <p>
 * Jun 9, 2016, 10:37:13 AM
 * <p>
 * Some utilities on dependency graphs.
 * 
 */
public class Dependencies {

	/**
	 * Converts a list of graphs to a list of labeled sentences.
	 * @param graphs
	 * @return a list of labeled sentences.
	 */
	public static List<String> toLabeledSentences(List<DependencyGraph> graphs) {
		List<String> list = new LinkedList<String>();
		for (DependencyGraph graph : graphs) {
			StringBuilder sb = new StringBuilder();
			Sentence sentence = graph.getSentence();
			String[] labels = graph.getLabels();
			for (int i = 1; i < sentence.length(); i++) {
				sb.append(sentence.getToken(i));
				sb.append(';');
				sb.append(sentence.getTag(i));
				sb.append('/');
				sb.append(labels[i]);
				sb.append(' ');
			}
			list.add(sb.toString().trim());
		}
		return list;
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		List<DependencyGraph> graphs = DependencyGraphReader.read("dat/udt/vi/01-test.conll", 'x');
		List<DependencyGraph> graphs = DependencyGraphReader.read("dat/udt/en/en-ud-test.conllu", 'u');
		List<String> list = toLabeledSentences(graphs);
		System.out.println("Number of graphs = " + list.size());
//		UTF8FileIO.writeln("dat/udt/vi/01-test.labeled", list);
		UTF8FileIO.writeln("dat/udt/en/en-ud-test.labeled", list);
	}

}
