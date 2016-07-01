package vn.vitk;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.spark.api.java.JavaSparkContext;

import vn.vitk.tag.CMMParams;
import vn.vitk.tag.Tagger;
import vn.vitk.tok.Tokenizer;
import vn.vitk.util.SparkContextFactory;

/**
 * @author Phuong LE-HONG
 * <p>
 * Jun 3, 2016, 3:54:08 PM
 * <p>
 * The main class of the toolkit which selects the tool 
 * to run.
 * 
 */
public class Vitk {

	public static void main(String[] args) {
		String master = "local[*]";
		String inputFileName = "";
		String outputFileName = "";
		String url = "";
		String tool = "tok";
		
		Options options = new Options();
		options.addOption("m", true, "master");
		options.addOption("t", true, "tool");
		options.addOption("v", false, "verbose mode");
		// 1. word segmentation options
		options.addOption("i", true, "input filename)");
		options.addOption("o", true, "output filename");
		options.addOption("u", true, "input URL");
		options.addOption("s", false, "whitespace classification");
		// 2. additional options for part-of-speech tagging
		options.addOption("a", true, "action, either 'tag', 'eval' or 'train'");
		options.addOption("cmm", true, "conditional Markov model");
		options.addOption("dim", true, "domain dimension, or the number of features");
		options.addOption("reg", true, "L2-regularization parameter");
		// 3. additional options for dependency parsing
		
		CommandLineParser parser = new PosixParser();
		CommandLine cm;
		try {
			cm = parser.parse(options, args);
			if (cm.hasOption("m")) {
				master = cm.getOptionValue("m");
			}
			if (cm.hasOption("t")) {
				tool = cm.getOptionValue("t");
			}
			// create a Spark context
			JavaSparkContext jsc = SparkContextFactory.create(master);
			// select a tool
			if (tool.equalsIgnoreCase("tok")) {
				// 1. word segmentation
				// 
				String dataFolder = "/export/dat/tok";
				Tokenizer tokenizer = null;
				if (cm.hasOption("s")) {
					// use whitespace classification
					tokenizer = new Tokenizer(master, dataFolder + "/lexicon.xml", 
							dataFolder + "/regexp.txt", dataFolder + "/whitespace.model", true);
				} else {
					// use a bigram language model
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
					} catch (UnsupportedEncodingException | FileNotFoundException | MalformedURLException e) {
						e.printStackTrace();
					}
				}
			} else if (tool.equalsIgnoreCase("tag")) {
				// 2. part-of-speech tagging
				// 
				String dataFolder = "/export/dat/tag";
				String action = "tag";
				String cmm = dataFolder + "/vi/cmm";
				
				Tagger tagger = new Tagger(jsc);
				if (cm.hasOption("v")) {
					tagger.setVerbose(true);
				}
				if (cm.hasOption("cmm")) {
					cmm = cm.getOptionValue("cmm");
				} 
				if (cm.hasOption("i")) {
					inputFileName = cm.getOptionValue("i");
				} else {
					System.err.println("You need to provide an input filename!");
					System.exit(1);
				}
				
				if (cm.hasOption("a")) {
					action = cm.getOptionValue("a");
				}
				if (action.equalsIgnoreCase("train")) {
					int numFeatures = -1;
					double regParam = 0d;
					if (cm.hasOption("dim")) {
						numFeatures = Integer.parseInt(cm.getOptionValue("dim"));
					}
					if (cm.hasOption("reg")) {
						regParam = Double.parseDouble(cm.getOptionValue("reg"));
					}
					if (numFeatures > 0) {
						CMMParams params = new CMMParams()
							.setMarkovOrder(2)
							.setMaxIter(600).setNumFeatures(numFeatures)
							.setRegParam(regParam);
						tagger.train(inputFileName, cmm, params);
					} else {
						System.err.println("Number of features must be positive!");
					}
				} else if (action.equalsIgnoreCase("tag")) {
					tagger.load(cmm);
					if (cm.hasOption("o")) {
						outputFileName = cm.getOptionValue("o");
						tagger.tag(inputFileName, outputFileName, Tagger.OutputFormat.JSON);
					} else {
						tagger.tag(inputFileName);
					}
					
				} else if (action.equalsIgnoreCase("eval")) {
					tagger.load(cmm);
					tagger.evaluate(inputFileName);
				} else {
					System.err.println("You need to provide an action: 'tag', 'eval' or 'train'!");
				}
				
			} else if (tool.equalsIgnoreCase("dep")) {
				// 3. dependency parsing
				// TODO: 
			}
		
		} catch (ParseException e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
		
	}

}
