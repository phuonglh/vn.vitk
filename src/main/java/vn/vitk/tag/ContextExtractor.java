package vn.vitk.tag;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Row;

import scala.Tuple2;
import vn.vitk.util.Converter;

/**
 * @author Phuong LE-HONG
 *         <p>
 *         May 11, 2016, 5:57:52 PM
 *         <p>
 *         A context extractor extracts all labeled contexts from a training dataset
 *         containing labeled sequences.
 *         
 */
public class ContextExtractor implements Serializable {

	private static final long serialVersionUID = 6481557600701316137L;
	
	private MarkovOrder markovOrder = MarkovOrder.FIRST;
	
	private Converter tokenConverter;
	
	enum Unknown {
		BOS,	// begin of sequence
		EOS		// end of sequence
	}
	
	/**
	 * Creates a context extractor.
	 * @param markovOrder
	 * @param regexpFileName
	 */
	public ContextExtractor(MarkovOrder markovOrder, String regexpFileName) {
		this.markovOrder = markovOrder;
		this.tokenConverter = new Converter(regexpFileName);
	}
	
	
	List<String> extractBasicFeatures(List<Tuple2<String, String>> labeledSequence, int position) {
		List<String> fs = new LinkedList<String>();
		// current word
		String f = labeledSequence.get(position)._1();
		fs.add("w(0):" + f);
		// previous words
		f = (position < 1) ? Unknown.BOS.name() : labeledSequence.get(position-1)._1();
		fs.add("w(-1):" + f);
		f = (position < 2) ? Unknown.BOS.name() : labeledSequence.get(position-2)._1();
		fs.add("w(-2):" + f);
		// next words
		f = (position >= labeledSequence.size()-1) ? Unknown.EOS.name() : labeledSequence.get(position+1)._1();
		fs.add("w(+1):" + f);
		f = (position >= labeledSequence.size()-2) ? Unknown.EOS.name() : labeledSequence.get(position+2)._1();
		fs.add("w(+2):" + f);
		// previous tag
		f = (position < 1) ? Unknown.BOS.name() : labeledSequence.get(position-1)._2();
		fs.add("t(-1):" + f);
		if (markovOrder == MarkovOrder.SECOND) {
			f = (position < 2) ? Unknown.BOS.name() : labeledSequence.get(position-2)._2();
			fs.add("t(-2):" + f);
		} else if (markovOrder == MarkovOrder.THIRD) {
			f = (position < 2) ? Unknown.BOS.name() : labeledSequence.get(position-2)._2();
			fs.add("t(-2):" + f);
			f = (position < 3) ? Unknown.BOS.name() : labeledSequence.get(position-3)._2();
			fs.add("t(-3):" + f);
		}
		return fs;
	}
	
	List<String> extractWordFormFeatures(List<Tuple2<String, String>> labeledSequence, int position) {
		List<String> fs = new LinkedList<String>();
		String word = labeledSequence.get(position)._1();
		// the converted form of the current word
		fs.add("wf:" + tokenConverter.convert(word));
		// the number of syllables of the current word
		fs.add("ns:" + word.split("_").length);
		return fs;
	}
	
	List<String> extractJointFeatures(List<Tuple2<String, String>> labeledSequence, int position) {
		List<String> fs = new LinkedList<String>();
		// current word and previous tag
		String u = labeledSequence.get(position)._1();
		String v = (position < 1) ? Unknown.BOS.name() : labeledSequence.get(position-1)._2();
		fs.add("w(0)+t(-1):" + (u + '+' + v));
		// current word and previous word
		v = (position < 1) ? Unknown.BOS.name() : labeledSequence.get(position-1)._1();
		fs.add("w(0)+w(-1):" + (u + '+' + v));
		// current word and the word preceding the previous word
		v = (position < 2) ? Unknown.BOS.name() : labeledSequence.get(position-2)._1();
		fs.add("w(0)+w(-2):" + (u + '+' + v));
		// current word and next word
		v = (position >= labeledSequence.size()-1) ? Unknown.EOS.name() : labeledSequence.get(position+1)._1();
		fs.add("w(0)+w(+1):" + (u + '+' + v));
		// current word and the word following the next word
		v = (position >= labeledSequence.size()-2) ? Unknown.EOS.name() : labeledSequence.get(position+2)._1();
		fs.add("w(0)+w(+2):" + (u + '+' + v));
		// two previous tags
		if (markovOrder == MarkovOrder.SECOND) {
			u = (position < 2) ? Unknown.BOS.name() : labeledSequence.get(position-2)._2();
			v = (position < 1) ? Unknown.BOS.name() : labeledSequence.get(position-1)._2();
			fs.add("t(-2)+t(-1):" + (u + '+' + v));
		}
		return fs;
		
	}
	
	/**
	 * Extracts a labeled context at a position in a tagged sequence.
	 * @param labeledSequence
	 * @param position
	 * @return a labeled context
	 */
	public LabeledContext extract(List<Tuple2<String, String>> labeledSequence, int position) {
		Set<String> fs = new HashSet<String>();
		fs.addAll(extractBasicFeatures(labeledSequence, position));
		fs.addAll(extractWordFormFeatures(labeledSequence, position));
		fs.addAll(extractJointFeatures(labeledSequence, position));
		StringBuilder features = new StringBuilder(64);
		for (String s : fs) {
			features.append(s);
			features.append(' ');
		}
		LabeledContext context = new LabeledContext();
		context.setWord(labeledSequence.get(position)._1());
		context.setTag(labeledSequence.get(position)._2());
		context.setFeatureStrings(features.toString().trim());
		return context;
	}
	
	public MarkovOrder getMarkovOrder() {
		return markovOrder;
	}

	/**
	 * Extracts a RDD of labeled contexts from a RDD of rows where each row 
	 * has two string cells containing a word sequence and a tag sequence. 
	 * @param dataset
	 * @return a RDD of labeled contexts
	 */
	public JavaRDD<LabeledContext> extract(JavaRDD<Row> dataset) {
		return dataset.flatMap(new RowToContextFunction()); 
	}
	
	class RowToContextFunction implements FlatMapFunction<Row, LabeledContext> {
		private static final long serialVersionUID = 2126656465710981973L;
		
		@Override
		public Iterable<LabeledContext> call(Row row) throws Exception {
			String[] words = row.getString(0).split("\\s+");
			String[] tags = row.getString(1).split("\\s+");
			List<Tuple2<String, String>> labeledSequence = new LinkedList<Tuple2<String, String>>();
			for (int i = 0; i < words.length; i++)
				labeledSequence.add(new Tuple2<String, String>(words[i], tags[i]));
			List<LabeledContext> contexts = new LinkedList<LabeledContext>();
			for (int i = 0; i < labeledSequence.size(); i++) {
				LabeledContext context = extract(labeledSequence, i);
				contexts.add(context);
			}
			return contexts;
		}
	}
}
