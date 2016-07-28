package vn.vitk.tag;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.ml.Model;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.feature.CountVectorizerModel;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.ml.util.DefaultParamsReader;
import org.apache.spark.ml.util.DefaultParamsWriter;
import org.apache.spark.ml.util.MLReader;
import org.apache.spark.ml.util.MLWritable;
import org.apache.spark.ml.util.MLWriter;
import org.apache.spark.ml.util.SchemaUtils;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.util.MLUtils;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.mutable.WrappedArray;
import vn.vitk.util.Constants;

/**
 * @author Phuong LE-HONG
 *         <p>
 *         May 9, 2016, 5:52:38 PM
 *         <p>
 *         A conditional Markov model (or Maximum-Entropy Markov model -- MEMM)
 *         for sequence labeling which is fitted by a CMM graphical model.
 * 
 */
public class CMMModel extends Model<CMMModel> implements MLWritable {

	private static final long serialVersionUID = -4855076361905836432L;
	
	private PipelineModel pipelineModel;
	private ContextExtractor contextExtractor;
	private final Vector weights;
	private final String[] tags;
	private final Map<String, Integer> featureMap;
	private final Map<String, Set<Integer>> tagDictionary; 
	
	/**
	 * Creates a conditional Markov model.
	 * @param pipelineModel
	 * @param weights
	 * @param markovOrder
	 */
	public CMMModel(PipelineModel pipelineModel, Vector weights, MarkovOrder markovOrder, Map<String, Set<Integer>> tagDictionary) {
		this.pipelineModel = pipelineModel;
		this.contextExtractor = new ContextExtractor(markovOrder, Constants.REGEXP_FILE);
		this.weights = weights;
		this.tags = ((StringIndexerModel)(pipelineModel.stages()[2])).labels();
		String[] features = ((CountVectorizerModel)(pipelineModel.stages()[1])).vocabulary();
		featureMap = new HashMap<String, Integer>();
		for (int j = 0; j < features.length; j++) {
			featureMap.put(features[j], j);
		}
		this.tagDictionary = tagDictionary;
	}
	
	@Override
	public CMMModel copy(ParamMap extra) {
		return defaultCopy(extra);
	}

	/**
	 * An immutable unique ID for the object and its derivatives.
	 * @return an immutable unique ID for the object and its derivatives.
	 */
	@Override
	public String uid() {
		String ruid = UUID.randomUUID().toString();
		int n = ruid.length();
		return "cmmModel" + "_" + ruid.substring(n-12, n);
	}

	/* (non-Javadoc)
	 * @see org.apache.spark.ml.Transformer#transform(org.apache.spark.sql.DataFrame)
	 */
	@Override
	public DataFrame transform(DataFrame dataset) {
		JavaRDD<Row> output = dataset.javaRDD().map(new DecodeFunction());
		StructType schema = new StructType(new StructField[]{
			new StructField("sentence", DataTypes.StringType, false, Metadata.empty()),
			new StructField("prediction", DataTypes.StringType, false, Metadata.empty())
		});
		return dataset.sqlContext().createDataFrame(output, schema);
	}

	class DecodeFunction implements Function<Row, Row> {
		private static final long serialVersionUID = 5026042203808959533L;

		@Override
		public Row call(Row row) throws Exception {
			List<String> words = Arrays.asList(row.getString(0).split("\\s+"));
			int n = words.size();
			List<Tuple2<String, String>> sequence = new ArrayList<Tuple2<String, String>>(n);
			for (int i = 0; i < n; i++) {
				sequence.add(new Tuple2<String, String>(words.get(i), "UNK"));
			}
			List<String> partsOfSpeech = decode(sequence);
			StringBuilder sb = new StringBuilder();
			for (String pos : partsOfSpeech) {
				sb.append(pos);
				sb.append(' ');
			}
			return RowFactory.create(row.getString(0), sb.toString().trim());
		}
		
		/**
		 * Finds the best label sequence for an observation sequence.
		 * @param sequence
		 * @return a label sequence.
		 */
		private List<String> decode(List<Tuple2<String, String>> sequence) {
			int n = sequence.size();
			double[][] score = new double[tags.length][n];
					
			for (int j = 0; j < n; j++) {
				LabeledContext context = contextExtractor.extract(sequence, j);
				Tuple2<double[], String> tuple = probability(context);
				double[] prob = tuple._1();
				for (int i = 0; i < prob.length; i++) {
					score[i][j] = prob[i];
				}
				// update the tag at position j for the next incremental extraction
				// 
				sequence.set(j, new Tuple2<String, String>(sequence.get(j)._1(), tuple._2()));
			}
			
			ViterbiDecoder decoder = new ViterbiDecoder(score);
			int[] path = decoder.bestPath();
			List<String> partsOfSpeech = new LinkedList<String>();
			for (int k : path) {
				partsOfSpeech.add(tags[k]);
			}
			return partsOfSpeech;
		}
		
		/**
		 * Computes the probability mass function (pmf) of an unlabeled context. 
		 * @param context
		 * @return a tuple of the pmf over the tagset and the best tag.
		 */
		private Tuple2<double[], String> probability(LabeledContext context) {
			String[] fs = context.getFeatureStrings().toLowerCase().split("\\s+");
			Set<String> fsset = new HashSet<String>();
			for (String f : fs) {
				fsset.add(f);
			}
			List<Tuple2<Integer, Double>> x = new LinkedList<Tuple2<Integer, Double>>();
			for (String f : fsset) {
				Integer i = featureMap.get(f);
				if (i != null) {
					x.add(new Tuple2<Integer, Double>(i, 1.0));
				}
			}
			Vector features = MLUtils.appendBias(Vectors.sparse(featureMap.size(), x));
			int numLabels = tags.length;
			double[] score = new double[numLabels];
			Arrays.fill(score, 0d);
			int maxLabel = 0;
			double maxScore = 0d;
			String word = context.getWord();
			Set<Integer> labels = tagDictionary.get(word);
			if (labels == null) { // this is a rare/unknown word, try all possible labels
				labels = new HashSet<Integer>();
				for (int k = 1; k < numLabels; k++) // k goes from 1 since we do not need to compute score[0], it is always 0.0.
					labels.add(k);
			}
			
			int d = features.size();
			for (int k : labels) {
				if (k > 0) {
					for (int j : features.toSparse().indices())
						score[k] += weights.apply((k-1) * d + j);
					if (score[k] > maxScore) {
						maxScore = score[k];
						maxLabel = k;
					}
				}
			}
			
			// prevent possible numerical overflow error in the case maxScore > 0
			if (maxScore > 0) {
				for (int k = 0; k < numLabels; k++)
					score[k] -= maxScore;
			}
			// normalize the score to get probability
			double z = 0d;
			for (int k = 0; k < numLabels; k++) {
				score[k] = Math.exp(score[k]); 
				z += score[k];
			}
			for (int k = 0; k < numLabels; k++) {
				score[k] /= z;
			}
			return new Tuple2<double[], String>(score, tags[maxLabel]);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.apache.spark.ml.PipelineStage#transformSchema(org.apache.spark.sql.types.StructType)
	 */
	@Override
	public StructType transformSchema(StructType schema) {
		return SchemaUtils.appendColumn(schema, new StructField("prediction", DataTypes.StringType, false, Metadata.empty()));
	}

	/* (non-Javadoc)
	 * @see org.apache.spark.ml.util.MLWritable#save(java.lang.String)
	 */
	@Override
	public void save(String path) throws IOException {
		write().overwrite().save(path);
	}

	/* (non-Javadoc)
	 * @see org.apache.spark.ml.util.MLWritable#write()
	 */
	@Override
	public MLWriter write() {
		return new CMMModelWriter(this);
	}
	
	private class CMMModelWriter extends MLWriter {
		CMMModel instance;
		
		public CMMModelWriter(CMMModel instance) {
			this.instance = instance;
		}
		
		@Override
		public void saveImpl(String path) {
			// save metadata and params
			DefaultParamsWriter.saveMetadata(instance, path, sc(), 
					DefaultParamsWriter.saveMetadata$default$4(),
					DefaultParamsWriter.saveMetadata$default$5());

			// save model data: markovOrder, numLabels, weights
			Data data = new Data();
			data.setMarkovOrder(contextExtractor.getMarkovOrder().ordinal()+1);
			data.setWeights(weights);
			data.setTagDictionary(tagDictionary);
			List<Data> list = new LinkedList<Data>();
			list.add(data);
			String dataPath = new Path(path, "data").toString();
			sqlContext().createDataFrame(list, Data.class).write().parquet(dataPath);
			// save pipeline model
			try {
				String pipelinePath = new Path(path, "pipelineModel").toString(); 
				pipelineModel.write().overwrite().save(pipelinePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class Data implements Serializable {

		private static final long serialVersionUID = 1L;
		private int markovOrder;
		private Vector weights;
		private Map<String, Set<Integer>> tagDictionary;
		
		public void setMarkovOrder(int markovOrder) {
			this.markovOrder = markovOrder;
		}
		
		public int getMarkovOrder() {
			return markovOrder;
		}
		
		public void setWeights(Vector weights) {
			this.weights = weights;
		}
		
		public Vector getWeights() {
			return weights;
		}
		
		public Map<String, Set<Integer>> getTagDictionary() {
			return tagDictionary;
		}
		
		public void setTagDictionary(Map<String, Set<Integer>> tagDictionary) {
			this.tagDictionary = tagDictionary;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.apache.spark.ml.PipelineStage#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[markovOrder=");
		sb.append(contextExtractor.getMarkovOrder());
		sb.append(", ");
		sb.append("numLabels = ");
		sb.append(tags.length);
		sb.append(", ");
		sb.append(", weights=");
		sb.append(weights.toString());
		sb.append(']');
		return sb.toString();
	}

	/**
	 * Loads a {@link CMMModel} from an external file.
	 * @param path
	 * @return a CMM model.
	 */
	public static CMMModel load(String path) {
		return read().load(path);
	}

	/**
	 * This functions is used in the reflection framework of Spark ML.
	 * @return a {@link MLReader}.
	 */
	public static MLReader<CMMModel> read() {
		return new CMMModelReader();
	}
	
	private static class CMMModelReader extends MLReader<CMMModel> {
		@Override
		public CMMModel load(String path) {
			org.apache.spark.ml.util.DefaultParamsReader.Metadata metadata = DefaultParamsReader.loadMetadata(path, sc(), CMMModel.class.getName());
			String pipelinePath = new Path(path, "pipelineModel").toString();
			PipelineModel pipelineModel = PipelineModel.load(pipelinePath);
			String dataPath = new Path(path, "data").toString();
			DataFrame df = sqlContext().read().format("parquet").load(dataPath);
			Row row = df.select("markovOrder", "weights", "tagDictionary").head();
			// load the Markov order
			MarkovOrder order = MarkovOrder.values()[row.getInt(0)-1];
			// load the weight vector
			Vector w = row.getAs(1);
			// load the tag dictionary
			@SuppressWarnings("unchecked")
			scala.collection.immutable.HashMap<String, WrappedArray<Integer>> td = (scala.collection.immutable.HashMap<String, WrappedArray<Integer>>)row.get(2);
			Map<String, Set<Integer>> tagDict = new HashMap<String, Set<Integer>>();
			Iterator<Tuple2<String, WrappedArray<Integer>>> iterator = td.iterator();
			while (iterator.hasNext()) {
				Tuple2<String, WrappedArray<Integer>> tuple = iterator.next();
				Set<Integer> labels = new HashSet<Integer>();
				scala.collection.immutable.List<Integer> list = tuple._2().toList();
				for (int i = 0; i < list.size(); i++)
					labels.add(list.apply(i));
				tagDict.put(tuple._1(), labels);
			}
			// build a CMM model
			CMMModel model = new CMMModel(pipelineModel, w, order, tagDict);
			DefaultParamsReader.getAndSetParams(model, metadata);
			return model;
		}
	}

}
