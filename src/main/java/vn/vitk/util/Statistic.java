package vn.vitk.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.spark.api.java.JavaSparkContext;


/**
 * @author Phuong LE-HONG
 * <p>
 * May 31, 2016, 4:35:32 PM
 * <p>
 * 
 */
public class Statistic {

	static String taggedCorpusFileName = "/export/dat/tag/vtb-tagged.txt";
	static transient JavaSparkContext jsc = SparkContextFactory.create();
	
	/**
	 * Print some statistics about Vietnamese PoS tagging.
	 */
	static void posTagging() {
		int nTokens = 0;
		// word -> tag
		Map<String, Set<String>> map = new HashMap<String, Set<String>>(); 
		List<String> taggedSequences = jsc.textFile(taggedCorpusFileName).collect();
		Set<String> easyWords = new HashSet<String>();
		for (String sequence : taggedSequences) {
			String[] tokens = sequence.split("\\s+");
			nTokens += tokens.length;
			for (String token : tokens) {
				String[] parts = token.split("/");
				if (parts.length == 2) {
					String word = parts[0];
					if (map.get(word) == null)
						map.put(word, new HashSet<String>());
					map.get(word).add(parts[1]);
					if (parts[0].equals(parts[1])) {
						easyWords.add(parts[0]);
					}
				}
				
			}
		}
		System.out.println("#(tokens): " + nTokens);
		System.out.println("#(unique words): " + map.size());
		int max = 0; // max number of tags for a word
		int n1 = 0; // number of words having an unique tag
		double sum = 0d; 
		for (String word : map.keySet()) {
			int nTags = map.get(word).size();
			if (nTags > max) {
				max = nTags; 
			}
			if (nTags == 1)
				n1++;
			sum += nTags;
		}
		System.out.println("Max number of tags per word: " + max);
		System.out.println("Most ambiguous words are: ");
		for (String word : map.keySet()) {
			if (map.get(word).size() == max) 
				System.out.println(word);
		}
		System.out.println("Number of unambiguous words: " + n1);
		System.out.println("Average number of tags per word: " + sum / map.size());
		System.out.println("Easy words: ");
		for (String ew: easyWords) {
			System.out.println(ew);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		posTagging();
	}

}
