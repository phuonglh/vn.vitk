package vn.vitk.tok;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.spark.Accumulator;
import org.apache.spark.AccumulatorParam;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;

import scala.Tuple2;
import vn.vitk.util.SparkContextFactory;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;

/**
 * @author Phuong LE-HONG
 * <p>
 * Mar 11, 2016, 10:20:06 AM
 * <p>
 * Fast and simple reimplementation of <code>vnTokenizer</code> tool.
 */
public class Tokenizer implements Serializable {
	
	private static final long serialVersionUID = 216079852616487675L;
	
	private transient JavaSparkContext jsc;
	private Lexicon lexicon = new Lexicon();
	private Map<String, Pattern> patterns = new TreeMap<String, Pattern>();
	private TextNormalizationFunction normalizationFunction = new TextNormalizationFunction();
	private PhraseGraph graph = new PhraseGraph();
	private Bigrams bigram = null;
	private WhitespaceClassifier classifier = null;
	private Accumulator<List<WhitespaceContext>> contexts;
	private PipelineModel model = null;
	private Broadcast<Row[]> prediction = null;
	private int counter = 0;
	
	private boolean verbose = false;
	
	/**
	 * Creates a Vietnamese tokenizer.
	 * @param master
	 * @param lexiconFileName
	 * @param regexpFileName
	 */
	public Tokenizer(String master, String lexiconFileName, String regexpFileName) {
		jsc = SparkContextFactory.create(master);
		lexicon = new Lexicon().load(lexiconFileName);
		if (verbose) 
			System.out.println("#(nodes of the lexicon) = " + lexicon.numNodes());
		
		List<String> lines = jsc.textFile(regexpFileName).collect();
		for (String line : lines) {
			line = line.trim();
			if (!line.startsWith("#")) { // ignore comment lines
				String[] s = line.split("\\s+");
				if (s.length == 2) {
					patterns.put(s[0], Pattern.compile(s[1]));
				}
			}
		}
	}
	
	/**
	 * Creates a Vietnamese tokenizer.
	 * @param master
	 * @param lexiconFileName
	 * @param regexpFileName
	 * @param bigramFileName
	 */
	public Tokenizer(String master, String lexiconFileName, String regexpFileName, String bigramFileName) {
		this(master, lexiconFileName, regexpFileName);
		bigram = new Bigrams(bigramFileName);
	}

	public Tokenizer(String master, String lexiconFileName, String regexpFileName, String whitespaceModelFileName, boolean lr) {
		this(master, lexiconFileName, regexpFileName);
		classifier = new WhitespaceClassifier(lexicon, patterns);
		model = classifier.load(whitespaceModelFileName);
		contexts = jsc.accumulator(new LinkedList<WhitespaceContext>(), new WhitespaceContextAccumulatorParam());
	}
	
	/**
	 * Reads the content of a text file to get lines.
	 * @param fileName
	 * @return a RDD of text lines.
	 */
	public JavaRDD<String> readTextFile(String fileName) {
		JavaRDD<String> input = jsc.textFile(fileName);
		return input.map(normalizationFunction);
	}
	
	/**
	 * Tokenizes a RDD of text lines and return a RDD of result.
	 * @param input
	 * @return a RDD of tokenized text lines.
	 */
	public JavaRDD<String> tokenize(JavaRDD<String> input) {
		if (verbose) {
			// print some basic statistic about the input, including 
			// max line length, min line length, average line length in syllables
			JavaRDD<Integer> wordCount = input.map(new Function<String, Integer>() {
				private static final long serialVersionUID = 7214093453452927565L;
				@Override
				public Integer call(String line) throws Exception {
					return line.split("\\s+").length;
				}
				
			});
			Comparator<Integer> comp = new IntegerComparator();
			System.out.println("Max line length (in syllables) = " + wordCount.max(comp));
			System.out.println("Min line length (in syllables) = " + wordCount.min(comp));
			float totalCount = wordCount.reduce(new Function2<Integer, Integer, Integer>() {
				private static final long serialVersionUID = 1L;
				@Override
				public Integer call(Integer v1, Integer v2) throws Exception {
					return v1 + v2;
				}
			});
			System.out.println("Avg line length (in syllables) = " + (totalCount) / input.count());
		}
		
		JavaRDD<String> output = null;
		if (classifier == null) {
			// use phrase graph approach (shortest paths and bigram model)
			// to segment phrases
			output = input.map(new SegmentationFunction());
		} else {
			// use logistic regression approach to segment phrases
			JavaRDD<String> s = input.map(new SegmentationFunction());
			// make sure that the preceding lazy computation has been evaluated
			// so that whitespace contexts have been properly accumulated
			System.out.println("Number of text lines = " + s.count());
			System.out.println("Number of contexts = " + contexts.value().size());
			// use whitespace classification approach (logistic regresion model)
			JavaRDD<WhitespaceContext> jrdd = jsc.parallelize(contexts.value());
			DataFrame df0 = (new SQLContext(jsc)).createDataFrame(jrdd, WhitespaceContext.class);
			DataFrame df1 = model.transform(df0);
			prediction = jsc.broadcast(df1.select("prediction").collect());
			if (df1.count() > 0) {
				output = s.map(new WhitespaceClassificationFunction());
			}
			else { 
				System.err.println("Empty data frame!");
			}
		}
		if (verbose) {
			// print number of non-space characters of the input and output dataset
			System.out.println("#(non-space characters of input) = " + numCharacters(input));
			if (output != null) {
				System.out.println("#(non-space characters of output) = " + numCharacters(output));
			}
		}
		return output;
	}
	
	
	/**
	 * Tokenizes a line. 
	 * @param line a line of text
	 * @return a result text string
	 */
	public String tokenizeOneLine(String line) {
		List<String> list = new ArrayList<String>();
		list.add(line);
		JavaRDD<String> input = jsc.parallelize(list);
		JavaRDD<String> output = tokenize(input);
		return output.first();
	}
	
	/**
	 * Tokenizes a text file and returns a list of tokens.
	 * @param fileName
	 * @return a list of tokens.
	 */
	public List<String> tokenize(String fileName) {
		JavaRDD<String> input = readTextFile(fileName);
		JavaRDD<String> output = tokenize(input);
		return output.collect();
	}
	
	/**
	 * Tokenizes a text file and saves the result to an output directory by using 
	 * Spark save as text file utility.
	 * @param inputFileName
	 * @param outputDirectory
	 */
	public void tokenize(String inputFileName, String outputDirectory) {
		JavaRDD<String> input = readTextFile(inputFileName);
		JavaRDD<String> output = tokenize(input);
		output.saveAsTextFile(outputDirectory);
	}
	
	
	/**
	 * Tokenizes a text file and writes the result to a writer. 
	 * @param inputFileName
	 * @param writer
	 * @return tokenization result.
	 */
	public List<String> tokenize(String inputFileName, PrintWriter writer) {
		JavaRDD<String> input = readTextFile(inputFileName);
		JavaRDD<String> output = tokenize(input);
		List<String> lines = output.collect();
		for (String line : lines) {
			writer.write(line);
			writer.write('\n');
		}
		writer.flush();
		return lines;
	}
	
	/**
	 * Tokenizes a text specified in an URL and writes the result to 
	 * a writer.
	 * @param url
	 * @param writer
	 */
	public List<String> tokenize(URL url, PrintWriter writer) {
		try {
			System.out.println("Extracting the text content of the URL...");
			String text = ArticleExtractor.INSTANCE.getText(new InputStreamReader(url.openStream(), "UTF-8"));
			if (verbose) {
				System.out.println("URL text content:");
				System.out.println(text);
			}
			System.out.println("Tokenizing the content...");
			JavaRDD<String> input = jsc.parallelize(Arrays.asList(text.split("\\n+")));
			JavaRDD<String> output = tokenize(input.map(normalizationFunction));
			List<String> lines = output.collect();
			for (String line : lines) {
				writer.write(line);
				writer.write('\n');
			}
			writer.flush();
			return lines;
		} catch (BoilerpipeProcessingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * Counts the number of non-space characters in this data set. This utility method 
	 * is used to check the tokenization result.
	 * @param lines
	 * @return number of characters
	 */
	int numCharacters(JavaRDD<String> lines) {
		JavaRDD<Integer> lengths = lines.map(new Function<String, Integer>() {
			private static final long serialVersionUID = -2189399343462982586L;
			@Override
			public Integer call(String line) throws Exception {
				line = line.replaceAll("[\\s_]+", "");
				return line.length();
			}
		});
		return lengths.reduce(new Function2<Integer, Integer, Integer>() {
			private static final long serialVersionUID = -8438072946884289401L;

			@Override
			public Integer call(Integer e0, Integer e1) throws Exception {
				return e0 + e1;
			}
		});
	}
	
	/**
	 * Adds more words to the lexicon.
	 * @param words a list of words.
	 */
	public void addWords(List<String> words) {
		addWords(words.toArray(new String[words.size()]));
	}
	
	/**
	 * Adds more words to the lexicon.
	 * @param words a string of words.
	 */
	public void addWords(String[] words) {
		for (String word : words)
			lexicon.addWord(word);
	}
	
	class SegmentationFunction implements Function<String, String> {
		private static final long serialVersionUID = 7206190528458789338L;

		@Override
		public String call(String sentence) throws Exception {
			Iterable<Tuple2<String, String>> tokens = segment(sentence);
			StringBuilder sb = new StringBuilder(64);
			for (Tuple2<String, String> token : tokens) {
				String content = token._2();
				if (!token._1().equals("phrase")) {
					content = content.replace(' ', '_');
				}
				sb.append(content);
				sb.append(' ');
			}
			return sb.toString().trim();
		}
		
		/**
		 * Segments a sentence, return an iterable of (token, type).
		 * @param sentence
		 * @return an iterable of (token, type) pairs.
		 */
		private Iterable<Tuple2<String, String>> segment(String sentence) {
			List<Tuple2<String, String>> tokens = new ArrayList<Tuple2<String, String>>();
			sentence = sentence.trim();
			if (sentence.length() == 0) {
				return tokens;
			}
			String s = sentence;
			while (true) {
				int maxLen = 0;
				String nextToken = "";
				String type = "";
				// greedy search for the longest pattern
				// from the beginning of 's'
				for (String patternName : patterns.keySet()) {
					Pattern pattern = patterns.get(patternName);
					Matcher matcher = pattern.matcher(s);
					if (matcher.lookingAt()) {
						int len = matcher.end() - matcher.start();
						if (maxLen < len) {
							maxLen = len;
							nextToken = matcher.group();
							type = patternName;
						}
					}
				}
				nextToken = nextToken.trim();
				if (nextToken.length() > 0) {
					s = s.substring(maxLen).trim();
					if (type.contains("name") && s.length() > 0) {
						Tuple2<String, String> tup = processName(nextToken, s);
						if (tup._1().length() != nextToken.length()) {
							nextToken = tup._1();
							s = tup._2();
							type = "word";
						}
						tokens.add(new Tuple2<String, String>(type, nextToken));
					} else if (type.contains("unit") && s.length() > 0) {
						Tuple2<String, String> tup = processUnit(nextToken, s);
						if (tup._1().length() > nextToken.length()) {
							nextToken = tup._1();
							s = tup._2();
							type = "unit";
						}
						tokens.add(new Tuple2<String, String>(type, nextToken));
					} else if (type.contains("phrase")) {
						if (nextToken.indexOf(' ') > 0) { // multi-syllabic phrase
							if (classifier != null) {
								contexts.add(classifier.makeContexts(nextToken));
								tokens.add(new Tuple2<String, String>("phrase", "[" + nextToken + "]"));
							} else {
								// segment the phrase using a phrase graph  
								List<String> words = tokenizePhrase(nextToken);
								if (words != null) {
									for (int i = 0; i < words.size(); i++) {
										tokens.add(new Tuple2<String, String>("word", words.get(i)));
									}
								} else {
									System.out.println("Error when tokenizing phrase: " + nextToken);
								}
							}
						} else { // mono-syllabic phrase, don't need to tokenize the phrase
							tokens.add(new Tuple2<String, String>("word", nextToken));
						}
					}  else { // next token is not a name, an unit or a phrase
						tokens.add(new Tuple2<String, String>(type, nextToken));
					}
				} else {
					if (s.trim().length() > 0) {
						System.out.println("Unprocessed substring: " + s);
					}
					break;
				}
				if (s.length() == 0) {
					break;
				}
			}
			return tokens;
		}
		
		private Tuple2<String, String> processName(String currentToken, String s) {
			// If this is a name pattern, we process it further to capture 2 cases: 
			//
			// 1. It should be merged with the next syllable, like in "Thủ tướng" or "Bộ Giáo dục." where 
			// the name pattern captures only the first part "Thủ" or "Bộ Giáo".
			// We try to combine the last syllable of the current token with the first token of s 
			// to see whether they may form a word or not, note that the first token of s may contain 
			// delimiters (like "dục." in the example above); we therefore need to remove them 
			// beforehand if necessary.
			int j = s.indexOf(' ');
			String nextSyllable = (j > 0) ? s.substring(0, j) : s;
			// s can either be "dục" or "dục.", find the last alphabetic character of s
			// so as to leave the non-alphabetic characters out.
			int u = nextSyllable.length();
			while (u > 0 && !Character.isAlphabetic(nextSyllable.charAt(--u)));
			nextSyllable = nextSyllable.substring(0, u+1);
			
			int k = currentToken.lastIndexOf(' ');
			String lastSyllable = (k > 0) ? currentToken.substring(k+1) : currentToken;
			String nextTokenPrefix = (k > 0) ? currentToken.substring(0, k+1) : "";  
			String w = lastSyllable.toLowerCase() + ' ' + nextSyllable;
			if (lexicon.hasWord(w)) {
				currentToken = nextTokenPrefix + lastSyllable + ' ' + nextSyllable;
				s = s.substring(nextSyllable.length()).trim();
			}
			// 2. It should be divided into two parts if the first syllable of the name 
			// is a name prefix like "Ông", "Bà", "Anh", "Em", etc.
			j = currentToken.indexOf(' ');
			if (j > 0) {
				String firstSyllable = currentToken.substring(0, j);
				Matcher matcher = patterns.get("prefix").matcher(firstSyllable);
				if (matcher.matches()) {
					StringBuilder sb = new StringBuilder(currentToken.substring(j+1));
					sb.append(' ');
					sb.append(s);
					s = sb.toString();
					currentToken = firstSyllable;
				}
			}
			return new Tuple2<String, String>(currentToken, s);
		}
		
		private Tuple2<String, String> processUnit(String currentToken, String s) {
			// "[đồng/đô] [la Mỹ...]" => [đồng/đô la] [Mỹ...] 
			// "[đồng/cổ phiếu] [...]"
			String lastSyllable = currentToken.substring(currentToken.indexOf('/') + 1);
			int j = s.indexOf(' ');
			String nextSyllable = (j > 0) ? s.substring(0, j) : s;
			// s can either be "phiếu" or "phiếu.", find the last alphabetic character of s
			// so as to leave the non-alphabetic characters out.
			int u = nextSyllable.length();
			while (u > 0 && !Character.isAlphabetic(nextSyllable.charAt(--u)));
			nextSyllable = nextSyllable.substring(0, u+1);
			
			if (lexicon.hasWord(lastSyllable + ' ' + nextSyllable)) {
				currentToken = currentToken + ' ' + nextSyllable;
				s = s.substring(nextSyllable.length()).trim();
			}
			return new Tuple2<String, String>(currentToken, s);
		}
		
		/**
		 * Tokenizes a phrase.
		 * @param phrase
		 * @return a list of tokens.
		 */
		private List<String> tokenizePhrase(String phrase) {
			graph.makeGraph(phrase);
			List<LinkedList<Integer>> paths = graph.shortestPaths();
			if (paths.size() > 0) {
//				// print out overlap groups
//				List<Tuple2<Integer, Integer>> ambiguities = graph.overlaps();
//				for (Tuple2<Integer, Integer> tup : ambiguities) {
//					System.out.println(graph.words(tup));
//				}
				LinkedList<Integer> selectedPath = paths.get(paths.size()-1);
				if (bigram != null) {
					int best = graph.select(paths);
					selectedPath = paths.get(best);
				}
				return graph.words(selectedPath);
			}
			if (verbose) {
				System.out.println("Cannot tokenize the following phrase: [" + phrase +"]");
			}
			return null;
		}
		
	}
	
	class PhraseGraph implements Serializable {
		private static final long serialVersionUID = 6761055345566557524L;
		private String[] syllables;
		private int n;
		
		/**
		 * For each vertex v, we store a list of vertices u where (u, v) is an edge
		 * of the graph. This is used to recursively search for all paths on the graph.  
		 */
		private Map<Integer, LinkedList<Integer>> edges = new HashMap<Integer, LinkedList<Integer>>();

		void makeGraph(String phrase) {
			edges.clear();
			syllables = phrase.split("\\s+");
			n = syllables.length;
			if (n > 128) {
				System.out.println("WARNING: Phrase too long (>= 128 syllables), tokenization may be slow...");
				System.out.println(phrase);
			}
			for (int j = 0; j <= n; j++) {
				edges.put(j, new LinkedList<Integer>());
			}
			for (int i = 0; i < n; i++) {
				String token = syllables[i];
				int j = i;
				while (j < n) {
					if (lexicon.hasWord(token)) {
						edges.get(j+1).add(i);
					}
					j++;
					if (j < n) {
						token = token + ' ' + syllables[j];
					}
				}
			}
			// make sure that the graph is connected by adding adjacent 
			// edges if necessary
			for (int i = n; i > 0; i--) {
				if (edges.get(i).size() == 0) { // i cannot reach by any previous node
					edges.get(i).add(i-1);
				}
			}
		}

		/**
		 * Finds all shortest paths from the first node to the last node
		 * of this graph. 
		 * @return a list of paths, each path is a linked list of vertices.
		 */
		public List<LinkedList<Integer>> shortestPaths() {
			Dijkstra dijkstra = new Dijkstra(edges);
			List<LinkedList<Integer>> allPaths = dijkstra.shortestPaths();
			if (verbose) {
				if (allPaths.size() > 16) {
					StringBuilder phrase = new StringBuilder();
					for (String syllable : syllables) {
						phrase.append(syllable);
						phrase.append(' ');
					}
					System.out.printf("This phrase is too ambiguous, giving %d shortest paths!\n\t%s\n", 
							allPaths.size(), phrase.toString().trim());
				}
			}
			return allPaths;
		}
		
		/**
		 * Gets a list of words specified by a given path.
		 * @param path
		 * @return a list of words.
		 */
		public List<String> words(LinkedList<Integer> path) {
			int m = path.size();
			if (m <= 1) 
				return null;
			Integer[] a = path.toArray(new Integer[m]);
			StringBuilder[] tok = new StringBuilder[m-1];
			int i;
			for (int j = 0; j < m-1; j++) {
				// get the token from a[j] to a[j+1] (exclusive)
				tok[j] = new StringBuilder();
				i = a[j];
				tok[j].append(syllables[i]);
				for (int k = a[j]+1; k < a[j+1]; k++) {
					tok[j].append(' ');
					tok[j].append(syllables[k]);
				}
			}
			List<String> result = new LinkedList<String>();
			for (StringBuilder sb : tok) {
				result.add(sb.toString());
			}
			return result;
		}
		
		/**
		 * Gets a sub-sequence of syllables from a segment marking 
		 * the beginning and the end positions.
		 * @param segment
		 * @return
		 */
		public String words(Tuple2<Integer, Integer> segment) {
			StringBuilder sb = new StringBuilder();
			for (int i = segment._1(); i < segment._2(); i++) {
				sb.append(syllables[i]);
				sb.append(' ');
			}
			return sb.toString().trim();
		}
		
		/**
		 * Finds all overlap groups of syllables of this graph. A syllable group is overlap if
		 * it defines multiple shortest paths, e.g. 4 nodes and two paths: [0, 1, 3] or [0, 2, 3]. 
		 * @return a list of syllable groups, each is a tuple of begin and end position.
		 */
		public List<Tuple2<Integer, Integer>> overlaps() {
			List<Tuple2<Integer, Integer>> result = new ArrayList<Tuple2<Integer, Integer>>();
			if (n >= 4) {
				int i = n-1;
				while (i >= 3) {
					if (edges.get(i).contains(i-1) && edges.get(i).contains(i-2)
							&& edges.get(i-1).contains(i-3) && edges.get(i-2).contains(i-3)) {
						result.add(new Tuple2<Integer, Integer>(i-3, i));
						i = i - 4;
					} else {
						i = i - 1;
					}
				}
			}
			return result;
		}
		
		@Override
		public String toString() {
			return edges.toString();
		}
		
		/**
		 * Selects the most likely segmentation from a list 
		 * of different segmentations.
		 * @param paths
		 * @return
		 */
		public int select(List<LinkedList<Integer>> paths) {
			if (bigram != null) {
				int maxIdx = 0;
				double maxVal = Double.MIN_VALUE;
				// find the maximum of log probabilities of segmentations
				for (int j = 0; j < paths.size(); j++) {
					LinkedList<Integer> path = paths.get(j);
					List<String> words = words(path);
					words.add(0, "<s>");
					words.add("</s>");
					double p = 0d;
					for (int w = 1; w < words.size(); w++)
						p += bigram.logConditionalProb(words.get(w-1), words.get(w));
					if (p > maxVal) {
						maxVal = p;
						maxIdx = j;
					}
				}
				return maxIdx;
			}
			return 0;
		}
	}

	class TextNormalizationFunction implements Function<String, String> {
		private static final long serialVersionUID = 5727433453096616457L;
		private Map<String, String> vowels = new HashMap<String, String>();
		private Pattern pattern = Pattern.compile("[hklmst][yỳýỷỹỵ]");  
		private Map<Character, Character> ymap = new HashMap<Character, Character>();

		public TextNormalizationFunction() {
			// initialize the vowel map
			vowels.put("òa", "oà");
			vowels.put("óa", "oá");
			vowels.put("ỏa", "oả");
			vowels.put("õa", "oã");
			vowels.put("ọa", "oạ");
			vowels.put("òe", "oè");
			vowels.put("óe", "oé");
			vowels.put("ỏe", "oẻ");
			vowels.put("õe", "oẽ");
			vowels.put("ọe", "oẹ");
			vowels.put("ùy", "uỳ");
			vowels.put("úy", "uý");
			vowels.put("ủy", "uỷ");
			vowels.put("ũy", "uỹ");
			vowels.put("ụy", "uỵ");
			// initialize the y map
			ymap.put('y', 'y');
			ymap.put('ỳ', 'ì');
			ymap.put('ý', 'í');
			ymap.put('ỷ', 'ỉ');
			ymap.put('ỹ', 'ĩ');
			ymap.put('ỵ', 'ị');
		}
		
		@Override
		public String call(String phrase) throws Exception {
			// normalize all the vowels of the phrase
			for (String u : vowels.keySet()) {
				String v = vowels.get(u);
				phrase = phrase.replace(u, v);
			}
			// normalize the i/y by converting 'y' to 'i'  
			// if 'y' goes after consonants "h, k, l, m, s, t".
			StringBuilder sb = new StringBuilder(phrase);
			Matcher matcher = pattern.matcher(phrase);
			while (matcher.find()) {
				int idx = matcher.start() + 1;
				sb = sb.replace(idx, matcher.end(), String.valueOf(ymap.get(sb.charAt(idx))));
			}
			return sb.toString();
		}
	}
	
	
	class WhitespaceContextAccumulatorParam implements AccumulatorParam<List<WhitespaceContext>> {

		private static final long serialVersionUID = -8140972589714419311L;
		
		@Override
		public List<WhitespaceContext> addInPlace(List<WhitespaceContext> l1,
				List<WhitespaceContext> l2) {
			l1.addAll(l2);
			return l1;
		}

		@Override
		public List<WhitespaceContext> zero(List<WhitespaceContext> list) {
			return new LinkedList<WhitespaceContext>();
		}

		@Override
		public List<WhitespaceContext> addAccumulator(
				List<WhitespaceContext> l1, List<WhitespaceContext> l2) {
			l1.addAll(l2);
			return l1;
		}

	}
	
	class WhitespaceClassificationFunction implements Function<String, String> {
		private static final long serialVersionUID = -8841083728315408933L;
		private Pattern phrase = Pattern.compile("\\[[\\p{Ll}\\s]+\\]");
		
		@Override
		public String call(String sentence) throws Exception {
			StringBuilder r = new StringBuilder(sentence);
			Matcher matcher = phrase.matcher(sentence);
			while (matcher.find()) {
				int u = matcher.start();
				int v = matcher.end();
				String s = sentence.substring(u+1, v-1);
				int i = 0;
				for (int j = 0; j < s.length(); j++)
					if (s.charAt(j) == ' ') i++;
				String[] syllables = s.split("\\s+");
				StringBuilder sb = new StringBuilder(s.length());
				for (int j = 0; j < i; j++) {
					sb.append(syllables[j]);
					if (prediction.value()[counter + j].getDouble(0) == 0) {
						sb.append(' ');
					} else {
						sb.append('_');
					}
				}
				sb.append(syllables[syllables.length-1]);
				counter += i;
				r.replace(u+1, v-1, sb.toString());
			}
			return r.toString();
		}
		
	}
	
	class IntegerComparator implements Comparator<Integer>, Serializable {
		
		private static final long serialVersionUID = 3285846060042662009L;

		@Override
		public int compare(Integer i, Integer j) {
			return i - j;
		}
		
	}
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	/**
	 * For internal test only.
	 * @param args
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) throws MalformedURLException {
		String master = "local[*]";
		String inputFileName = "";
		String outputFileName = "";
		String url = "";
		
		Options options = new Options();
		options.addOption("m", true, "master");
		options.addOption("i", true, "input file name)");
		options.addOption("o", true, "output file name");
		options.addOption("u", true, "input URL");
		options.addOption("v", false, "verbose");
		options.addOption("s", false, "whitespace classification");
		CommandLineParser parser = new PosixParser();
		CommandLine cm;
		try {
			cm = parser.parse(options, args);
			if (cm.hasOption("m")) {
				master = cm.getOptionValue("m");
			}
			String dataFolder = "/export/dat/tok";
			Tokenizer tokenizer = null;
			if (cm.hasOption("s")) {
				// use whitespace classification
				tokenizer = new Tokenizer(master, dataFolder + "/lexicon.xml", 
						dataFolder + "/regexp.txt", dataFolder + "/whitespace.model", true);
			} else {
				// use a bigram model
				tokenizer = new Tokenizer(master, dataFolder + "/lexicon.xml", 
						dataFolder + "/regexp.txt", dataFolder + "/syllables2M.arpa");
			}
			if (cm.hasOption("v")) {
				tokenizer.setVerbose(true);
			}
			if (cm.hasOption("i")) {
				inputFileName = cm.getOptionValue("i");
			}
			if (cm.hasOption("u")) {
				url = cm.getOptionValue("u");
			}
			if (inputFileName.length() == 0 && url.length() == 0) {
				System.err.println("Either an input file or an URL must be provided!");
				System.exit(1);
			} else if (inputFileName.length() > 0) {
				if (cm.hasOption("o")) {
					outputFileName = cm.getOptionValue("o");
					tokenizer.tokenize(inputFileName, outputFileName);
				} else {
					try {
						PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"));
						tokenizer.tokenize(inputFileName, writer);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			} else {
				try {
					PrintWriter writer = null;
					if (cm.hasOption("o")) {
						outputFileName = cm.getOptionValue("o");
						writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFileName), "UTF-8"));
						tokenizer.tokenize(new URL(url), writer);
						writer.close();
					} else {
						writer = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"));
						tokenizer.tokenize(new URL(url), writer);
					}
				} catch (UnsupportedEncodingException | FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
	}
}
