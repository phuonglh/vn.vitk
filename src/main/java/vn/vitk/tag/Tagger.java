package vn.vitk.tag;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
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

import vn.vitk.lang.CorpusPack;
import vn.vitk.lang.Language;
import vn.vitk.util.SparkContextFactory;

/**
 * @author Phuong LE-HONG
 * <p>
 * May 16, 2016, 9:40:34 AM
 * <p>
 * A part-of-speech tagger which uses the conditional Markov model. 
 *   
 */
public class Tagger implements Serializable {
	
	private static final long serialVersionUID = 8061440373376898771L;
	private transient JavaSparkContext jsc;
	private CMMModel cmmModel;
	
	private boolean verbose = false;
	
	public enum OutputFormat {
		TEXT,
		JSON,
		PARQUET
	}
	
	enum TaggerMode {
		TRAIN,
		TAG
	}
	
	
	/**
	 * Creates a tagger which runs on a stand-alone machine.
	 */
	public Tagger(JavaSparkContext jsc) {
		this.jsc = jsc;
	}
	
	/**
	 * Trains a tagger. The <code>inputFileName</code> contains tagged sentences
	 * in a simple format: "He/P kicks/V a/D ball/N ./.". After training, the
	 * tagger model is saved to a file <code>modelFileName</code>. The
	 * parameters used in training is given in an argument.
	 * 
	 * @param inputFileName
	 * @param modelFileName
	 * @param params
	 * @return a {@link CMMModel}
	 */
	public CMMModel train(String inputFileName, String modelFileName, CMMParams params) {
		JavaRDD<String> lines = jsc.textFile(inputFileName);
		cmmModel = train(lines.collect(), modelFileName, params);
		return cmmModel;
	}
	
	/**
	 * Trains a tagger with data specified in a data frame. The data frame has 
	 * two columns, one column "sentence" contains a word sequence, and the other column "partOfSpeech" 
	 * contains the corresponding tag sequence. Each row of the data frame specifies a tagged sequence
	 * in the training set.
	 * @param dataset
	 * @param modelFileName
	 * @param params
	 * @return a {@link CMMModel}
	 */
	public CMMModel train(DataFrame dataset, String modelFileName, CMMParams params) {
		CMM cmm = new CMM(params).setVerbose(verbose);
		cmmModel = cmm.fit(dataset);
		try {
			cmmModel.write().overwrite().save(modelFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cmmModel;
	}
	
	/**
	 * Trains a tagger. Training data are tagged sequences stored in an input
	 * file of a simple format, each sequence in a line. After training, the
	 * tagger is saved to a file. The parameters used in training is given in 
	 * an argument.
	 * 
	 * @param taggedSentences
	 * @param modelFileName
	 * @param params
	 * @return a {@link CMMModel}
	 */
	public CMMModel train(List<String> taggedSentences, String modelFileName, CMMParams params) {
		DataFrame dataset = createDataFrame(taggedSentences);
		return train(dataset, modelFileName, params);
	}

	/**
	 * Creates a data frame from a list of tagged sentences.
	 * @param taggedSentences
	 * @return a data frame of two columns: "sentence" and "partOfSpeech".
	 */
	public DataFrame createDataFrame(List<String> taggedSentences) {
		List<String> wordSequences = new LinkedList<String>();
		List<String> tagSequences = new LinkedList<String>();
		for (String taggedSentence : taggedSentences) {
			StringBuilder wordBuf = new StringBuilder();
			StringBuilder tagBuf = new StringBuilder();
			String[] tokens = taggedSentence.split("\\s+");
			for (String token : tokens) {
				String[] parts = token.split("/");
				if (parts.length == 2) {
					wordBuf.append(parts[0]);
					wordBuf.append(' ');
					tagBuf.append(parts[1]);
					tagBuf.append(' ');
				} else { // this token is "///"  
					wordBuf.append('/');
					wordBuf.append(' ');
					tagBuf.append('/');
					tagBuf.append(' ');
				}
			}
			wordSequences.add(wordBuf.toString().trim());
			tagSequences.add(tagBuf.toString().trim());
		}
		if (verbose) {
			System.out.println("Number of sentences = " + wordSequences.size());
		}
		List<Row> rows = new LinkedList<Row>();
		for (int i = 0; i < wordSequences.size(); i++) {
			rows.add(RowFactory.create(wordSequences.get(i), tagSequences.get(i)));
		}
		JavaRDD<Row> jrdd = jsc.parallelize(rows);
		StructType schema = new StructType(new StructField[]{
				new StructField("sentence", DataTypes.StringType, false, Metadata.empty()),
				new StructField("partOfSpeech", DataTypes.StringType, false, Metadata.empty())
			});
			
		return new SQLContext(jsc).createDataFrame(jrdd, schema);
	}
	
	/**
	 * Loads a {@link CMMModel} from a model file.
	 * @param modelFileName
	 * @return this object.
	 */
	public Tagger load(String modelFileName) {
		this.cmmModel = CMMModel.load(modelFileName);
		return this;
	}
	
	
	/**
	 * Tags a list of sequences and returns a list of tag sequences.
	 * @param sentences
	 * @return a list of tagged sequences.
	 */
	public List<String> tag(List<String> sentences) {
		List<Row> rows = new LinkedList<Row>();
		for (String sentence : sentences) {
			rows.add(RowFactory.create(sentence));
		}
		StructType schema = new StructType(new StructField[]{
			new StructField("sentence", DataTypes.StringType, false, Metadata.empty())	
		});
		SQLContext sqlContext = new SQLContext(jsc);
		DataFrame input = sqlContext.createDataFrame(rows, schema);
		if (cmmModel != null) {
			DataFrame output = cmmModel.transform(input).repartition(1);
			return output.javaRDD().map(new RowToStringFunction(1)).collect();
		} else {
			System.err.println("Tagging model is null. You need to create or load a model first.");
			return null;
		}
	}
	
	/**
	 * Tags a data frame containing a column named 'sentence'.
	 * @param input
	 * @param outputFileName
	 * @param outputFormat
	 */
	public void tag(DataFrame input, String outputFileName, OutputFormat outputFormat) {
		long tic = System.currentTimeMillis();
		long duration = 0;
		if (cmmModel != null) {
			DataFrame output = cmmModel.transform(input).repartition(1);
			duration = System.currentTimeMillis() - tic;
			switch (outputFormat) {
			case JSON:
				output.write().json(outputFileName);
				break;
			case PARQUET:
				output.write().parquet(outputFileName);
				break;
			case TEXT:
				toTaggedSentence(output).repartition(1).saveAsTextFile(outputFileName);
//				output.select("prediction").write().text(outputFileName);
				break;
			}
		} else {
			System.err.println("Tagging model is null. You need to create or load a model first.");
		}
		if (verbose) {
			long n = input.count();
			System.out.println(" Number of sentences = " + n);
			System.out.println("  Total tagging time = " + duration + " milliseconds.");
			System.out.println("Average tagging time = " + ((float)duration) / n + " milliseconds.");
		}
	}
	
	/**
	 * Tags a list of sequences and writes the result to an output file with a
	 * desired output format.
	 * 
	 * @param sentences
	 * @param outputFileName
	 * @param outputFormat
	 */
	public void tag(List<String> sentences, String outputFileName, OutputFormat outputFormat) {
		List<Row> rows = new LinkedList<Row>();
		for (String sentence : sentences) {
			rows.add(RowFactory.create(sentence));
		}
		StructType schema = new StructType(new StructField[]{
			new StructField("sentence", DataTypes.StringType, false, Metadata.empty())	
		});
		SQLContext sqlContext = new SQLContext(jsc);
		DataFrame input = sqlContext.createDataFrame(rows, schema);
		tag(input, outputFileName, outputFormat);
	}
	
	/**
	 * Tags a distributed list of sentences and writes the result to an output file with 
	 * a desired output format.
	 * @param sentences
	 * @param outputFileName
	 * @param outputFormat
	 */
	public void tag(JavaRDD<Row> sentences, String outputFileName, OutputFormat outputFormat) {
		StructType schema = new StructType(new StructField[]{
			new StructField("sentence", DataTypes.StringType, false, Metadata.empty())	
		});
		SQLContext sqlContext = new SQLContext(jsc);
		DataFrame input = sqlContext.createDataFrame(sentences, schema);
		tag(input, outputFileName, outputFormat);
	}
	
	/**
	 * Tags a text file, each sentence in a line and writes the result to an output file 
	 * with a desired output format.
	 * @param inputFileName
	 * @param outputFileName
	 * @param outputFormat
	 */
	public void tag(String inputFileName, String outputFileName, OutputFormat outputFormat) {
		List<String> sentences = jsc.textFile(inputFileName).collect();
		tag(sentences, outputFileName, outputFormat);
	}
	
	/**
	 * Tags a text file, each sentence in a line and writes the result to the console.
	 * @param inputFileName
	 */
	public void tag(String inputFileName) {
		List<String> sentences = jsc.textFile(inputFileName).collect();
		List<String> output = tag(sentences);
		for (int i = 0; i < sentences.size(); i++) {
			StringBuilder sb = new StringBuilder(64);
			String words[] = sentences.get(i).split("\\s+");
			String tags[] = output.get(i).split("\\s+");
			for (int j = 0; j < words.length; j++) {
				sb.append(words[j]);
				sb.append('/');
				sb.append(tags[j]);
				sb.append(' ');
			}
			System.out.println(sb.toString().trim());
		}
	}
	
	private JavaRDD<String> toTaggedSentence(DataFrame output) {
		return output.javaRDD().map(new Function<Row, String>() {
			private static final long serialVersionUID = 4208643510231783579L;
			@Override
			public String call(Row row) throws Exception {
				String[] tokens = row.getString(0).trim().split("\\s+");
				String[] tags = row.getString(1).trim().split("\\s+");
				if (tokens.length != tags.length) {
					System.err.println("Incompatible lengths!");
					return null;
				}
				StringBuilder sb = new StringBuilder(64);
				for (int j = 0; j < tokens.length; j++) {
					sb.append(tokens[j]);
					sb.append('/');
					sb.append(tags[j]);
					sb.append(' ');
				}
				return sb.toString().trim();
			}
		});
	}
	
	/**
	 * Evaluates the accuracy of a CMM model on a data frame on tagged sentences.
	 * @param dataset
	 * @return evaluation measures.
	 */
	public float[] evaluate(DataFrame dataset) {
		List<String> correctSequences = dataset.javaRDD().map(new RowToStringFunction(1)).collect();
		long beginTime = System.currentTimeMillis();
		DataFrame output = cmmModel.transform(dataset);
		long endTime = System.currentTimeMillis();
		if (verbose) {
			System.out.println(" Number of sentences = " + correctSequences.size());
			long duration = (endTime - beginTime);
			System.out.println("  Total tagging time = " + duration + " ms.");
			System.out.println("Average tagging time = " + ((float)duration) / correctSequences.size() + " ms.");
		}
		List<String> automaticSequences = output.javaRDD().map(new RowToStringFunction(1)).collect();
		return Evaluator.evaluate(automaticSequences, correctSequences);
	}
	
	/**
	 * Evaluates the tagger on a manually tagged data file.
	 * @param inputFileName
	 * @return scores
	 */
	public float[] evaluate(String inputFileName) {
		JavaRDD<String> lines = jsc.textFile(inputFileName);
		DataFrame dataset = createDataFrame(lines.collect());
		return evaluate(dataset);
	}
	
	/**
	 * Evaluates the accuracy of a CMM model on a list of tagged sentences. 
	 * @param taggedSentences
	 * @return evaluation measures.
	 */
	public float[] evaluate(List<String> taggedSentences) {
		DataFrame dataset = createDataFrame(taggedSentences);
		return evaluate(dataset);
	}
	
	private class RowToStringFunction implements Function<Row, String> {
		private static final long serialVersionUID = -2245906041132281238L;
		private int columnIndex = 0;
		
		RowToStringFunction(int columnIndex) {
			this.columnIndex = columnIndex;
		}
		@Override
		public String call(Row row) throws Exception {
			return row.getString(columnIndex);
		}
	}
	
	/**
	 * Set the verbose mode.
	 * @param verbose
	 * @return this object
	 */
	public Tagger setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	void test(String modelFileName, TaggerMode mode) {
		String[] taggedSentences = {
				"tôi/P ăn/V quả/Nc chuối/N to/A ./.",
				"tôi/Np đá/V quả/Nc bóng/N ./.",
				"tôi/Np ăn/V quả/Nc bóng/N to/A ./.",
				"vải/N hoa/N bụng/N cóc/N",
				"tôi/P cóc/R sợ/A" 
		};
		if (mode == TaggerMode.TRAIN) {
			train(Arrays.asList(taggedSentences), modelFileName, 
					new CMMParams().setNumFeatures(30));
		} else if (mode == TaggerMode.TAG){
			load(modelFileName);
			String[] sentences = {
				"tôi ăn quả chuối to .",
				"tôi đá quả bóng .",
				"tôi ăn quả bóng to .", 
				"vải hoa bụng cóc",
				"tôi cóc sợ" 
			};
			tag(Arrays.asList(sentences), "dat/tag/out", OutputFormat.JSON);
			evaluate(Arrays.asList(taggedSentences));
		}
	}
	
	void test(String inputFileName, int numFeatures, String modelFileName, TaggerMode mode) {
		if (mode == TaggerMode.TRAIN) {
			CMMParams params = new CMMParams()
				.setMaxIter(600)
				.setNumFeatures(numFeatures);
			train(inputFileName, modelFileName, params);
		}
		else if (mode == TaggerMode.TAG) {
			load(modelFileName);
			List<String> taggedSentences = jsc.textFile(inputFileName).collect();
			evaluate(taggedSentences);
		}
	}
	
	void testRandomSplit(String inputFileName, int numFeatures, String modelFileName) {
		CMMParams params = new CMMParams()
			.setMaxIter(600)
			.setRegParam(1E-6)
			.setMarkovOrder(2)
			.setNumFeatures(numFeatures);
		
		JavaRDD<String> lines = jsc.textFile(inputFileName);
		DataFrame dataset = createDataFrame(lines.collect());
		DataFrame[] splits = dataset.randomSplit(new double[]{0.9, 0.1}); 
		DataFrame trainingData = splits[0];
		System.out.println("Number of training sequences = " + trainingData.count());
		DataFrame testData = splits[1];
		System.out.println("Number of test sequences = " + testData.count());
		// train and save a model on the training data
		cmmModel = train(trainingData, modelFileName, params);
		// test the model on the test data
		System.out.println("Test accuracy:");
		evaluate(testData); 
		// test the model on the training data
		System.out.println("Training accuracy:");
		evaluate(trainingData);
	}
	
	/**
	 * For internal test only.
	 * @param args
	 */
	public static void main(String[] args) {
		CorpusPack cp = new CorpusPack(Language.VIETNAMESE);
		String modelFileName = cp.taggerModelFileName();
		
		JavaSparkContext jsc = SparkContextFactory.create();
		Tagger tagger = new Tagger(jsc).setVerbose(true);
		// 1. Toy dataset
		tagger.test(modelFileName, TaggerMode.TRAIN);
		tagger.test(modelFileName, TaggerMode.TAG);
		
		// 2. VTB, randomSplit (90%, 10%)
//		String corpusFileName = cp.taggerCorpusFileName();
//		tagger.testRandomSplit(corpusFileName, 160000, modelFileName);
	}
}
