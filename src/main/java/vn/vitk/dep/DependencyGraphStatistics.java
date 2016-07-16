package vn.vitk.dep;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import scala.Tuple2;
import vn.vitk.util.MapUtil;

/**
 * @author Phuong LE-HONG
 *         <p>
 *         Mar 7, 2016, 4:58:47 PM
 *         <p>
 *         Computes some statistics on dependency graphs.
 * 
 */
public class DependencyGraphStatistics {
	
	/**
	 * Counts the (headTag, dependentTag) frequency table of a dependency corpus. 
	 * @param graphs a list of dependency graphs
	 * @return a map of (tuple2, freq).
	 */
	public static Map<Tuple2<String, String>, Integer> headDependentMap(List<DependencyGraph> graphs) {
		Map<Tuple2<String, String>, Integer> map = new HashMap<Tuple2<String,String>, Integer>();
		for (DependencyGraph graph : graphs) {
			Integer[] heads = graph.getHeads();
			Sentence sentence = graph.getSentence();
			for (int j = 0; j < heads.length; j++) {
				String dependentTag = sentence.getTag(j);
				if (heads[j] >= 0) {
					String headTag = sentence.getTag(heads[j]);
					Tuple2<String, String> tuple = new Tuple2<String, String>(headTag, dependentTag);
					if (map.get(tuple) == null) {
						map.put(tuple, 1);
					} else {
						map.put(tuple, map.get(tuple) + 1);
					}
				}
			}
		}
		return map;
	}
	
	public static Map<String, Map<String, Integer>> headMap(List<DependencyGraph> graphs) {
		Map<String, Map<String, Integer>> map = new TreeMap<String, Map<String,Integer>>();
		for (DependencyGraph graph : graphs) {
			Integer[] heads = graph.getHeads();
			Sentence sentence = graph.getSentence();
			for (int j = 0; j < heads.length; j++) {
				String dependentTag = sentence.getTag(j);
				if (heads[j] >= 0) {
					String headTag = sentence.getTag(heads[j]);
					if (map.get(headTag) == null) {
						map.put(headTag, new TreeMap<String, Integer>());
					}
					Map<String, Integer> dependent = map.get(headTag);
					if (dependent.get(dependentTag) == null) {
						dependent.put(dependentTag, 1);
					} else {
						dependent.put(dependentTag, dependent.get(dependentTag) + 1);
					}
				}
			}
		}
		return map;
	}
	
	public static void main(String[] args) {
//		String corpusFileName = "/export/dat/udt/en/en-ud-train.conllu";
//		String corpusFileName = "/export/dat/udt/en/en-ud-test.conllu";
//		List<DependencyGraph> graphs = DependencyGraphReader.readCoNLLU(corpusFileName);
		
		String corpusFileName = "/export/dat/udt/vi/dataAll.conll";
		List<DependencyGraph> graphs = DependencyGraphReader.read(corpusFileName, 'x');
		
		Map<Tuple2<String, String>, Integer> map = headDependentMap(graphs);
		System.out.println("#(tuples) = " + map.size());
		for (Tuple2<String, String> tuple : map.keySet()) {
			System.out.println(tuple + ":" + map.get(tuple));
		}
		System.out.println();
		
		Map<String, Map<String, Integer>> heads = headMap(graphs);
		System.out.println("#(head) = " + heads.size());
		for (String head : heads.keySet()) {
			System.out.println(head);
			Map<String, Integer> dependents = heads.get(head);
			System.out.println("\t" + dependents.size() + " dependents.");
			System.out.println("\t" + heads.get(head));
		}
		
		// get possible root PoS tags, ordered by frequency
		// 
		System.out.println();
		System.out.println("root PoS: ");
		Map<String, Integer> dependentsOfRoot = heads.get("ROOT");
		Map<String, Integer> sortedDoR = MapUtil.sortByValue(dependentsOfRoot);
		System.out.println(sortedDoR);
	}
}
