package vn.vitk.dep;

import java.util.LinkedList;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

/**
 * @author Phuong Le-Hong <phuonglh@gmail.com>
 *         <p>
 *         Jan 29, 2016 7:46:41 PM
 *         <p>
 *         Abstract dependency parser.
 * 
 */
public abstract class DependencyParser {
	
	public enum OutputFormat {
		TEXT,
		JSON,
	}
	
	/**
	 * Parses a sentence. 
	 * @param sentence
	 * @return a dependency graph
	 */
	public abstract DependencyGraph parse(Sentence sentence);
	
	
	/**
	 * Parse a list of sentences in a parallel fashion.
	 * @param jsc
	 * @param sentences
	 * @return a list of dependency graphs.
	 */
	public List<DependencyGraph> parse(JavaSparkContext jsc, List<Sentence> sentences) {
		JavaRDD<Sentence> jrdd = jsc.parallelize(sentences);
		return jrdd.map(new ParsingFunction()).collect();
	}
	
	/**
	 * Parses a list of PoS-tagged sentences, each on a line and writes the result to an output 
	 * file in a specified output format.
	 * @param jsc
	 * @param sentences
	 * @param outputFileName
	 * @param outuptFormat
	 */
	public void parse(JavaSparkContext jsc, List<String> sentences, String outputFileName, OutputFormat outputFormat) {
		JavaRDD<String> input = jsc.parallelize(sentences);
		JavaRDD<Sentence> sents = input.map(new TaggedLineToSentenceFunction());
		JavaRDD<DependencyGraph> graphs = sents.map(new ParsingFunction());
		JavaRDD<Row> rows = graphs.map(new Function<DependencyGraph, Row>() {
			private static final long serialVersionUID = -812004521983071103L;
			public Row call(DependencyGraph graph) {
				return RowFactory.create(graph.getSentence().toString(), graph.dependencies());
			}
		});
		StructType schema = new StructType(new StructField[]{
			new StructField("sentence", DataTypes.StringType, false, Metadata.empty()),	
			new StructField("dependency", DataTypes.StringType, false, Metadata.empty())
		});
		SQLContext sqlContext = new SQLContext(jsc);
		DataFrame df = sqlContext.createDataFrame(rows, schema);
		
		if (outputFormat == OutputFormat.TEXT)  
			df.select("dependency").write().text(outputFileName);
		else 
			df.repartition(1).write().json(outputFileName);
	}
	
	private final class TaggedLineToSentenceFunction implements Function<String, Sentence> {
		private static final long serialVersionUID = -5656057350076339025L;
		@Override
		public Sentence call(String line) throws Exception {
			String[] taggedTokens = line.trim().split("\\s+");
			int n = taggedTokens.length + 1;
			String[] tokens = new String[n];
			String[] tags = new String[n];
			tokens[0] = "ROOT";
			tags[0] = "ROOT";
			for (int j = 1; j < n; j++) {
				String[] parts = taggedTokens[j-1].split("/");
				if (parts.length == 2) {
					tokens[j] = parts[0];
					tags[j] = parts[1];
				} else {
					if (parts.length == 3) {
						tokens[j] = parts[0];
						tags[j] = parts[2];
					} else {
						System.err.println("Line error: " + line);
					}
				}
			}
			return new Sentence(tokens, tags);
		}
	}
	
	/**
	 * Parses all sentences in an input file, each on a line and writes the result to an output 
	 * file in a specified output format.
	 * @param jsc
	 * @param inputFileName
	 * @param outputFileName
	 * @param outputFormat
	 */
	public void parse(JavaSparkContext jsc, String inputFileName, String outputFileName, OutputFormat outputFormat) {
		List<String> sentences = jsc.textFile(inputFileName).collect();
		parse(jsc, sentences, outputFileName, outputFormat);
	}
	
	
	/**
	 * Parses all sentences in an input file, each on a line and writes the result to 
	 * the console window containing flattened dependency tuples.
	 * @param jsc
	 * @param inputFileName
	 */
	public void parse(JavaSparkContext jsc, String inputFileName) {
		List<String> sentences = jsc.textFile(inputFileName).collect();
		JavaRDD<String> input = jsc.parallelize(sentences);
		JavaRDD<Sentence> sents = input.map(new TaggedLineToSentenceFunction());
		JavaRDD<DependencyGraph> graphs = sents.map(new ParsingFunction());
		JavaRDD<String> rows = graphs.map(new Function<DependencyGraph, String>() {
			private static final long serialVersionUID = -6021310762521034121L;

			public String call(DependencyGraph graph) {
				return graph.dependencies();
			}
		});
		for (String s : rows.collect()) {
			System.out.println(s);
		}
	}
	
	/**
	 * Evaluates a parser on a manually parsed corpus.
	 * @param jsc
	 * @param graphs
	 * @return a list of dependency graphs.
	 */
	public void evaluate(JavaSparkContext jsc, List<DependencyGraph> graphs) {
		List<Sentence> sentences = new LinkedList<Sentence>();
		for (DependencyGraph graph : graphs) {
			sentences.add(graph.getSentence());
		}
		JavaRDD<Sentence> jrdd = jsc.parallelize(sentences);
		List<DependencyGraph> proposedGraphs = jrdd.map(new ParsingFunction()).collect();
		Evaluator evaluator = new Evaluator();
		evaluator.evaluate(graphs, proposedGraphs);
		System.out.println("UAS(token) = " + evaluator.getUASToken());
		System.out.println("UAS(sentence) = " + evaluator.getUASSentence());
		System.out.println("LAS(token) = " + evaluator.getLASToken());
		System.out.println("LAS(sentence) = " + evaluator.getLASSentence());
	}
	
	private final class ParsingFunction implements Function<Sentence, DependencyGraph> {
		private static final long serialVersionUID = -427735844309327738L;
		@Override
		public DependencyGraph call(Sentence sentence) throws Exception {
			return parse(sentence);
		}
	}
	
}
