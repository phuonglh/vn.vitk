package vn.vitk.util;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.ml.feature.NGram;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import scala.Tuple2;

/**
 * @author Phuong LE-HONG
 * <p>
 * Apr 12, 2016, 5:15:19 PM
 * <p>
 *  Constructs bigrams from a Vietnamese corpus.
 */
public class NGramBuilder implements Serializable {
	
	private static final long serialVersionUID = -7781645249679542493L;
	private transient JavaSparkContext jsc = SparkContextFactory.create();
	private Converter converter;
	
	public NGramBuilder(String regexpFileName, String inputFileName) {
		JavaRDD<String> lines = jsc.textFile(inputFileName).filter(new InvalidLineFilter());
		System.out.println("#(lines) = " + lines.count());
		// write out a plain text file name which will be used by SRILM to train 
		// a language model
		BufferedWriter bw = null;
		try {
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(inputFileName + ".txt"), "UTF-8");
			bw = new BufferedWriter(osw);
			List<String> validLines = lines.collect();
			for (String line : validLines) {
				// lowercase the line
				line = line.toLowerCase();
				osw.write(line);
				osw.write('\n');
			}
			bw.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public NGramBuilder(String regexpFileName, String inputFileName, String unigramFileName, String bigramFileName) {
		JavaRDD<String> lines = jsc.textFile(inputFileName).filter(new InvalidLineFilter());
		System.out.println("#(lines) = " + lines.count());
		// create unigrams and save them
		//
		converter = new Converter(regexpFileName);
		Map<String, Long> unigrams = lines.flatMap(new UnigramFunction()).countByValue();
		List<Tuple2<String, Long>> tuples = new ArrayList<Tuple2<String, Long>>(unigrams.size());
		for (String word : unigrams.keySet()) {
			Long f = unigrams.get(word);
			if (f >= 2)
				tuples.add(new Tuple2<String, Long>(word, f));
		}
		
		JavaPairRDD<String, Long> jprdd = jsc.parallelizePairs(tuples);
		jprdd.saveAsTextFile(unigramFileName, GzipCodec.class);
		
		// create bigrams and save them
		Map<Tuple2<String, String>, Long> bigrams = lines.flatMap(new BigramFunction()).countByValue();
		tuples = new ArrayList<Tuple2<String, Long>>(bigrams.size());
		for (Tuple2<String, String> pair : bigrams.keySet()) {
			Long f = bigrams.get(pair);
			if (f >= 2)
				tuples.add(new Tuple2<String, Long>(pair._1() + ',' + pair._2(), f));
		}
		jprdd = jsc.parallelizePairs(tuples);
		jprdd.saveAsTextFile(bigramFileName, GzipCodec.class);		
	}
	
	
	/**
	 * Creates a n-gram data frame from text lines.
	 * @param lines
	 * @return a n-gram data frame.
	 */
	DataFrame createNGramDataFrame(JavaRDD<String> lines) {
		JavaRDD<Row> rows = lines.map(new Function<String, Row>(){
			private static final long serialVersionUID = -4332903997027358601L;
			
			@Override
			public Row call(String line) throws Exception {
				return RowFactory.create(Arrays.asList(line.split("\\s+")));
			}
		});
		StructType schema = new StructType(new StructField[] {
				new StructField("words",
						DataTypes.createArrayType(DataTypes.StringType), false,
						Metadata.empty()) });
		DataFrame wordDF = new SQLContext(jsc).createDataFrame(rows, schema);
		// build a bigram language model
		NGram transformer = new NGram().setInputCol("words")
				.setOutputCol("ngrams").setN(2);
		DataFrame ngramDF = transformer.transform(wordDF);
		ngramDF.show(10, false);
		return ngramDF;
	}
	
	
	class InvalidLineFilter implements Function<String, Boolean> {
		private static final long serialVersionUID = -5443181660691899302L;
		Pattern tag = Pattern.compile("^<\\/?\\w+>$");
		Pattern txt = Pattern.compile("\\w+"); 
		Pattern author = Pattern.compile("^[\\p{Lu}_\\-\\s\\.]+$");
		Pattern dateTime = Pattern.compile("^[\\d\\:\\s/]+$");
		
		@Override
		public Boolean call(String s) throws Exception {
			s = s.trim();
			if (s.length() == 0)	// s is empty
				return false;
			if (s.split("\\s+").length < 3) // less than 3 words
				return false;
			
			Matcher matcher = tag.matcher(s);
			if (matcher.matches())	// s is an XML tag
				return false;
			
			matcher = txt.matcher(s);
			if (!matcher.find())	// s does not contain any word character
				return false;
			
			matcher = author.matcher(s);
			if (matcher.matches())	// s is author line 
				return false;
			
			matcher = dateTime.matcher(s);
			if (matcher.matches())	// s is date time line 
				return false;
			return true;
		}
	}
	
	class UnigramFunction implements FlatMapFunction<String, String> {

		private static final long serialVersionUID = 7642582945770292178L;

		@Override
		public Iterable<String> call(String s) throws Exception {
			String[] tokens = s.split("\\s+");
			for (int i = 0; i < tokens.length; i++) {
				tokens[i] = converter.convert(tokens[i]);
			}
			return Arrays.asList(tokens);
		}
		
	}
	
	class BigramFunction implements FlatMapFunction<String, Tuple2<String, String>> {
		private static final long serialVersionUID = 2749290102631243248L;

		@Override
		public Iterable<Tuple2<String, String>> call(String s) throws Exception {
			List<Tuple2<String, String>> bigrams = new ArrayList<Tuple2<String, String>>();
			String[] tokens = s.split("\\s+");
			for (int i = 0; i < tokens.length; i++) {
				tokens[i] = converter.convert(tokens[i]);
			}
			String previous = "BOS";
			for (String token : tokens) {
				bigrams.add(new Tuple2<String, String>(previous, token));
				previous = token;
			}
			bigrams.add(new Tuple2<String, String>(previous, "EOS"));
			return bigrams;
		}
	}
	
	/**
	 * Normalizes tokens of a string s. This function converts all 
	 * dates to "DATE", numbers to "NUMBER", etc.
	 * @param s
	 * @return
	 */
	String normalize(String s) {
		
		return null;
	}
	
	public static void main(String[] args) {
//		new NGramBuilder("dat/tok/regexp.txt", "dat//syllables2M.seg", "dat/1grams", "dat/2grams");
		new NGramBuilder("dat/tok/regexp.txt", "dat//syllables2M.seg");
	}
}
