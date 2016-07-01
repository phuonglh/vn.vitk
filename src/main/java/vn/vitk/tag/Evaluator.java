package vn.vitk.tag;

import java.util.Arrays;
import java.util.List;

import vn.vitk.util.UTF8FileIO;

/**
 * @author Phuong LE-HONG
 * <p>
 * May 19, 2016, 4:07:00 PM
 * <p>
 * Evaluator of a tagger.
 * 
 */
public class Evaluator {

	private Evaluator() {}
	
	/**
	 * Checks the length of two sentence measured in syllables.
	 * @param s1 a sentence
	 * @param s2 a sentence
	 * @return <code>true</code> if two sentences have the same number of syllables, <code>false</code> otherwise.
	 */
	private static boolean checkSentence(String[] s1, String[] s2) {
		int n1 = 0, n2 = 0;
		for (String word : s1) {
			n1 += word.split("_").length;
		}
		
		for (String word : s2) {
			n2 += word.split("_").length;
		}
		return n1 == n2;
	}
	
	/**
	 * Evaluates two arrays of tags. These arrays are supposed to have positive length 
	 * and "well-formed", e.g., they have the same length measured in tags.
	 * 
	 * @param s1 an array of tags. This is an output tag sequence of a tagger.
	 * @param s2 an array of tags. This is the correct tag sequence (manually tagged).
	 * @param id the id (order) of the two strings 
	 * @return number of matched tags of the two tag sequences.
	 */
	private static int evaluate(String[] s1, String[] s2, int id) {
		int n = 0;
		for (int i = 0; i < s1.length; i++) {
			if (s1[i].equals(s2[i]))
				n++;
		}
		return n;
	}
	
	/**
	 * Evaluates two lists of tagged sentences, calculates the precision and
	 * recall ratios.
	 * 
	 * @param automaticSequences
	 *            a list of proposed tag sequences.
	 * @param correctSequences
	 *            a list of manually tag sentences
	 * @return a two element float array, the first number if the precision
	 *         ratio, the second number is the recall ratio.
	 */
	public static float[] evaluate(List<String> automaticSequences, List<String> correctSequences) {
		float[] pr = new float[2];
		pr[0] = 0;
		pr[1] = 0;
		
		int n = Math.min(automaticSequences.size(), correctSequences.size());
		int totalMatches = 0;
		int totalLen1 = 0;
		int totalLen2 = 0;
		
		for (int i = 0; i < n; i ++) {
			String[] s1 = automaticSequences.get(i).split("\\s+");
			String[] s2 = correctSequences.get(i).split("\\s+");
			if (checkSentence(s1, s2)) {
				int c = evaluate(s1,s2,i);
				totalMatches += c;
				totalLen1 += s1.length;
				totalLen2 += s2.length;
			} 
		}
		
		if (totalLen1 > 0) {
			pr[0] = (float)totalMatches/totalLen1;
		}
		
		if (totalLen2 > 0) {
			pr[1] = (float)totalMatches/totalLen2;
		}
		
		System.out.println("      #(matches) = " + totalMatches + " (tags)");
		System.out.println("Automatic length = " + totalLen1 + " (tags)");
		System.out.println("  Correct length = " + totalLen2 + " (tags)");
		System.out.println("       Precision = " + pr[0]);
		System.out.println("          Recall = " + pr[1]);
		
		return pr;
	}

	/**
	 * Evaluates two files, calculates the precision and recall ratios. The first file 
	 * contains result of the automatic tokenizer, each line of the file contains a tokenized sentence 
	 *  in which words are separated by spaces and syllables of a word are connected by underscore character. 
	 *  The second file contains manually tokenized sentences with the same order and format of the first file. 
	 * @param automaticFileName data produced by the tokenizer
	 * @param correctFileName data prepared by human (gold standard)
	 * @return a two element float array, the first number if the precision ratio, the second 
	 * is the recall ratio.
	 * @see #evaluate(String[][], String[][])
	 */
	public static float[] evaluate(String automaticFileName, String correctFileName) {
		List<String> s1= Arrays.asList(UTF8FileIO.readLines(automaticFileName));
		List<String> s2= Arrays.asList(UTF8FileIO.readLines(correctFileName));
		return evaluate(s1, s2);
	}

}
