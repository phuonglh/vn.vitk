package vn.vitk.tok;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import vn.vitk.util.UTF8FileIO;

/**
 * @author Le Hong Phuong, phuonglh@gmail.com
 * <p>
 * The evaluator of a tokenizer.
 */
public class Evaluator {

	/**
	 * The default syllable separator of a word  
	 */
	private static final String PADDING_STRING_1 = "$$";
	private static final String PADDING_STRING_2 = "€€";
	
	private static final boolean isLogged = true;
	private static List<UnmatchedToken> logs = new ArrayList<UnmatchedToken>();
	
	/**
	 * The users can only use static methods.
	 */
	private Evaluator() {	}
	
	
	
	
	/**
	 * Pads two strings before comparing them.
	 * @param s1 an array of words (an automatically tokenized sentence) 
	 * @param s2 an array of words (a manually tokenized sentence)
	 * @return
	 */
	private static String[][] padStrings(String[] s1, String[] s2) {
		String[][] result = new String[2][];
		
		LinkedList<String> l1 = new LinkedList<String>(Arrays.asList(s1));
		LinkedList<String> l2 = new LinkedList<String>(Arrays.asList(s2));
		int i1 = 0;
		int i2 = 0;
		while (i1 < l1.size() && i2 < l2.size()) {
			String e1 = l1.get(i1);
			String[] a1 = e1.split("_");
			
			String e2 = l2.get(i2);
			String[] a2 = e2.split("_");
			
			if (a1.length >= a2.length) {
				for (int j = 0; j < a1.length - a2.length; j++) {
					l1.add(i1+1, PADDING_STRING_1);
				}
			} else {
				for (int j = 0; j < a2.length - a1.length; j++) {
					l2.add(i2+1, PADDING_STRING_2);
				}
			}
			i1++;
			i2++;
		}
		
		if (l1.size() != l2.size()) {
			System.err.println("Error: After padding, two lists must be equal in size!");
			System.err.println(l1);
			System.err.println(l2);
		}
		result[0] = l1.toArray(new String[l1.size()]); 
		result[1] = l2.toArray(new String[l2.size()]);	
			
		return result;
	}
	/**
	 * Evaluates two arrays of words. These arrays are supposed to have positive length 
	 * and "well-formed", e.g., they have the same length measured in syllables.
	 * 
	 * @param s1 an array of words (a sentence). This is an output sentence of a tokenizer.
	 * @param s2 an array of words (a sentence). This is the correct sentence (manually tokenized).
	 * @param id the id (order) of the two strings 
	 * @return number of matched words of the two sentences.
	 */
	private static int evaluate(String[] s1, String[] s2, int id) {

		String[][] paddedStrings = padStrings(s1, s2);
		int n = 0;
		
		// count the number of matches
		// 
		for (int i = 0; i < paddedStrings[0].length; i++) {
			String w1 = paddedStrings[0][i];
			String w2 = paddedStrings[1][i];
			if (w1.equals(w2)) {
				n++;
			} else {
				if (isLogged) {
					if (w1.equals(PADDING_STRING_1) && (!w2.equals(PADDING_STRING_2))) {
						logs.add(new UnmatchedToken(id, i, w2));
					} 
					if (!w1.equals(PADDING_STRING_1) && (w2.equals(PADDING_STRING_2))) {
						logs.add(new UnmatchedToken(id, i, w1));
					}
					if (!w1.equals(PADDING_STRING_1) && (!w2.equals(PADDING_STRING_2))) {
						logs.add(new UnmatchedToken(id, i, w1 + "#" + w2));
					}
				}
			}
		}
		return n;
	}
	
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
	 * Evaluates two lists of tokenized sentences, calculates the precision and recall ratios. 
	 *  
	 * @param automaticSentences a list of automatically tokenized sentences
	 * @param correctSentences a list of manually tokenized sentences
	 * @return a two element float array, the first number if the precision ratio, the second number is the recall ratio. 
	 */
	public static float[] evaluate(List<String> automaticSentences, List<String> correctSentences) {
		logs.clear();
		
		float[] pr = new float[2];
		pr[0] = 0;
		pr[1] = 0;
		
		int n = Math.min(automaticSentences.size(), correctSentences.size());
		int totalMatches = 0;
		int totalLen1 = 0;
		int totalLen2 = 0;
		
		for (int i = 0; i < n; i ++) {
			String[] s1 = automaticSentences.get(i).split("\\s+");
			String[] s2 = correctSentences.get(i).split("\\s+");
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
		
		System.out.println("      #(matches) = " + totalMatches + " (words)");
		System.out.println("Automatic length = " + totalLen1 + " (words)");
		System.out.println("  Correct length = " + totalLen2 + " (words)");
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
	
	
	/**
	 * Gets the log
	 * @return the log
	 */
	public static List<UnmatchedToken> getLogs() {
		return logs;
	}
	
	
	/**
	 * Gets the log string representation
	 * @return the string representing the log
	 */
	private static String getLogString() {
		StringBuffer buffer = new StringBuffer();
		for (UnmatchedToken log : logs) {
			buffer.append(log.toString());
			buffer.append("\n");
		}
		return buffer.toString();
	}
	/**
	 * Print out the log for checking
	 */
	public static void printLogs() {
		System.out.println(getLogString());
	}
	
	/**
	 * Writes unmatched tokens to a log file.
	 * @param logFile a log file.
	 */
	public static void saveLogs(String logFile) {
		UTF8FileIO.writeln(logFile, new String[]{getLogString()});
	}

	static class UnmatchedToken {
		private int line;
		private int position;
		private String token;
		
		/**
		 * Default constructor.
		 */
		public UnmatchedToken() {
			line = -1;
			position = -1;
			token = null;
		}
		
		/**
		 * Constructs an unmatched token.
		 * @param line line position of the unmatched token.
		 * @param position the column position of the unmatched token
		 * @param token the token
		 */
		public UnmatchedToken(int line, int position, String token) {
			this.line = line;
			this.position = position;
			this.token = token;
		}
		
		public UnmatchedToken(String token) {
			this(-1,-1,token);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Line " + line + ":\t" + token + "\t[" + position + "]";
		}
	}
	
	static void test1() {
		Tokenizer tokenizer = new Tokenizer("dat/tok/lexicon.xml", "dat/tok/regexp.txt",  "dat/tok/syllables2M.arpa");
		
		String correctFileName = "dat/syllables2M.seg.tok";
		List<String> correctSentences = tokenizer.readTextFile(correctFileName).collect();
		
		List<String> automaticSentences = new ArrayList<String>(correctSentences.size());
		for (int i = 0; i < correctSentences.size(); i++) {
			String s = correctSentences.get(i).replace('_', ' ');
			String line = tokenizer.tokenizeOneLine(s); 
			automaticSentences.add(line.trim());
			if (i % 1000 == 0) {
				System.out.printf("%9d\n", i);
			}
		}
		Evaluator.evaluate(automaticSentences, correctSentences);
		Evaluator.saveLogs("dat/syllables2M.log");
		UTF8FileIO.writeln("dat/syllables2M.seg.aut", automaticSentences);
	}

	static void test2() {
		String autoFileName = "dat/syllables2M-logreg.tok";
		List<String> automaticSentences = Arrays.asList(UTF8FileIO.readLines(autoFileName));
		String correctFileName = "dat/syllables2M.txt";
		List<String> correctSentences = Arrays.asList(UTF8FileIO.readLines(correctFileName));
		Evaluator.evaluate(automaticSentences, correctSentences);
		Evaluator.saveLogs("dat/syllables2M-logreg.log");
	}
	
	public static void main(String[] args) {
		test2();
	}
}
