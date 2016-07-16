package vn.vitk.dep;

import java.util.ArrayList;
import java.util.List;

import vn.vitk.util.TokenNormalizer;

/**
 * @author Phuong Le-Hong <phuonglh@gmail.com>
 * <p>Jan 30, 2016, 8:40:39 AM
 * <p>
 * Extracts all interested features from a configuration.
 */
public class FeatureExtractor {

	private List<Extractor> extractors = new ArrayList<Extractor>();
	
	/**
	 * Creates a {@link FeatureExtractor} object given a feature frame specifying 
	 * feature type to be used in the parser.
	 * @param featureFrame
	 */
	public FeatureExtractor(FeatureFrame featureFrame) {
		switch (featureFrame) {
		case TAG:
			// tag features
			extractors.add(new Stack0Tag());
			extractors.add(new Stack1Tag());
			extractors.add(new Stack01Tags());
			extractors.add(new Queue0Tag());
			extractors.add(new Queue1Tag());
			extractors.add(new Queue01Tags());
			break;
		case TAG_TOKEN:
			// tag features
			extractors.add(new Stack0Tag());
			extractors.add(new Stack1Tag());
			extractors.add(new Stack01Tags());
			extractors.add(new Queue0Tag());
			extractors.add(new Queue1Tag());
			extractors.add(new Queue01Tags());
			// token features
			extractors.add(new Stack0Token());
			extractors.add(new Stack1Token());
			extractors.add(new Stack01Tokens());
			extractors.add(new Queue0Token());
			extractors.add(new Queue1Token());
			extractors.add(new Queue01Tokens());
			break;
		case TAG_TOKEN_TYPE:
			// tag features
			extractors.add(new Stack0Tag());
			extractors.add(new Stack1Tag());
			extractors.add(new Stack01Tags());
			extractors.add(new Queue0Tag());
			extractors.add(new Queue1Tag());
			extractors.add(new Queue01Tags());
			// token features
			extractors.add(new Stack0Token());
			extractors.add(new Stack1Token());
			extractors.add(new Stack01Tokens());
			extractors.add(new Queue0Token());
			extractors.add(new Queue1Token());
			extractors.add(new Queue01Tokens());
			// type features
			extractors.add(new Stack0Type());
			extractors.add(new Stack0TypeLeft());
			extractors.add(new Stack0TypeRight());
			break;
		}
	}
	
	/**
	 * Extracts all feature from this configuration.
	 * @param config a parser configuration
	 * @return a list of features.
	 */
	public List<String> extract(Configuration config) {
		List<String> features = new ArrayList<String>();
		for (Extractor fe : extractors) {
			features.add(fe.extract(config));
		}
		return features;
	}
	
	
	interface Extractor {
		/**
		 * Extracts a feature string from a configuration.
		 * @param configuration
		 * @return a feature string
		 */
		public abstract String extract(Configuration configuration);
	}

	
	class Stack0Tag implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			Integer s0 = configuration.getStack().peek();
			return "ts0:" + configuration.getSentence().getTag(s0);
		}
	}

	class Stack1Tag implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			int n = configuration.getStack().size();
			if (n < 2) {
				return "ts1:NULL";
			}
			Integer s1 = configuration.getStack().get(n-2);
			return "ts1:" + configuration.getSentence().getTag(s1);
		}
	}

	class Stack01Tags implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			Integer s0 = configuration.getStack().peek();
			String ts0 = configuration.getSentence().getTag(s0);
			int n = configuration.getStack().size();
			if (n < 2) {
				return "ts0+ts1:" + ts0 + "+NULL";
			}
			Integer s1 = configuration.getStack().get(n-2);
			String ts1 = configuration.getSentence().getTag(s1);
			return "ts0+ts1:" + ts0 + '+' + ts1;
		}
	}
	
	class Queue0Tag implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			Integer q0 = configuration.getQueue().peek();
			return "tq0:" + configuration.getSentence().getTag(q0);
		}
	}

	class Queue1Tag implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			int n = configuration.getQueue().size();
			if (n < 2) {
				return "tq1:NULL";
			}
			Integer q1 = configuration.getQueue().toArray(new Integer[n])[1];
			return "tq1:" + configuration.getSentence().getTag(q1);
		}
	}
	
	class Queue01Tags implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			Integer q0 = configuration.getQueue().peek();
			String tq0 = configuration.getSentence().getTag(q0);
			int n = configuration.getQueue().size();
			if (n < 2) {
				return "tq0+tq1:" + tq0 + "+NULL";
			}
			Integer q1 = configuration.getQueue().toArray(new Integer[n])[1];
			String tq1 = configuration.getSentence().getTag(q1);
			return "tq0+tq1:" + tq0 + '+' + tq1;
		}
	}
	
	class Stack0Token implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			Integer s0 = configuration.getStack().peek();
			String token = configuration.getSentence().getToken(s0); 
			return "ws0:" + TokenNormalizer.normalize(token);
		}
	}

	class Stack1Token implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			int n = configuration.getStack().size();
			if (n < 2) {
				return "ws1:NULL";
			}
			Integer s1 = configuration.getStack().get(n-2);
			String token = configuration.getSentence().getToken(s1);
			return "ws1:" + TokenNormalizer.normalize(token);
		}
	}

	class Stack01Tokens implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			Integer s0 = configuration.getStack().peek();
			String ws0 = configuration.getSentence().getToken(s0);
			int n = configuration.getStack().size();
			if (n < 2) {
				return "ws0+ws1:" + TokenNormalizer.normalize(ws0) + "+NULL";
			}
			Integer s1 = configuration.getStack().get(n-2);
			String ws1 = configuration.getSentence().getToken(s1);
			return "ws0+ws1:" + TokenNormalizer.normalize(ws0) + '+' + TokenNormalizer.normalize(ws1);
		}
	}
	
	class Queue0Token implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			Integer q0 = configuration.getQueue().peek();
			String token = configuration.getSentence().getToken(q0);
			return "wq0:" + TokenNormalizer.normalize(token);
		}
	}

	class Queue1Token implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			int n = configuration.getQueue().size();
			if (n < 2) {
				return "wq1:NULL";
			}
			Integer q1 = configuration.getQueue().toArray(new Integer[n])[1];
			String token = configuration.getSentence().getToken(q1);
			return "wq1:" + TokenNormalizer.normalize(token);
		}
	}
	
	class Queue01Tokens implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			Integer q0 = configuration.getQueue().peek();
			String wq0 = configuration.getSentence().getToken(q0);
			int n = configuration.getQueue().size();
			if (n < 2) {
				return "wq0+wq1:" + TokenNormalizer.normalize(wq0) + "+NULL";
			}
			Integer q1 = configuration.getQueue().toArray(new Integer[n])[1];
			String wq1 = configuration.getSentence().getToken(q1);
			return "wq0+wq1:" + TokenNormalizer.normalize(wq0) + '+' + TokenNormalizer.normalize(wq1);
		}
	}
	
	/**
	 * @author phuonglh, phuonglh@gmail.com
	 * <p>
	 * Extract the type of the top element on the stack, which is 
	 * the label of the arc coming to the element.  
	 */
	class Stack0Type implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			Integer s0 = configuration.getStack().peek();
			List<Dependency> arcs = configuration.getArcs();
			for (Dependency a: arcs) {
				if (a.getDependent() == s0) {
					return "rs0:" + a.getType();
				}
			}
			return "rs0:NULL";
		}
	}
	
	/**
	 * @author phuonglh, phuonglh@gmail.com
	 * <p>
	 * Extract the type of the left-most arc going from the top element 
	 * on the stack.
	 */
	class Stack0TypeLeft implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			Integer s0 = configuration.getStack().peek();
			String type = "NULL";
			int maxPosition = -1;
			List<Dependency> arcs = configuration.getArcs();
			for (Dependency a : arcs) {
				if (a.getHead() == s0) {
					Integer dependent = a.getDependent();
					if (dependent < s0 && dependent > maxPosition) {
						type = a.getType();
						maxPosition = dependent;
					}
				}
			}
			return "rs0l:" + type;
		}
	}
	
	/**
	 * @author phuonglh, phuonglh@gmail.com
	 * <p>
	 * Extract the type of the left-most arc going from the top element 
	 * on the stack.
	 */
	class Stack0TypeRight implements Extractor {
		@Override
		public String extract(Configuration configuration) {
			Integer s0 = configuration.getStack().peek();
			String type = "NULL";
			int minPosition = Integer.MAX_VALUE;
			List<Dependency> arcs = configuration.getArcs();
			for (Dependency a : arcs) {
				if (a.getHead() == s0) {
					Integer dependent = a.getDependent();
					if (s0 < dependent && dependent < minPosition) {
						type = a.getType();
						minPosition = dependent;
					}
				}
			}
			return "rs0r:" + type;
		}
	}
}
