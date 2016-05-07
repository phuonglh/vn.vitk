package vn.vitk.tok;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import scala.Tuple2;

/**
 * @author Phuong LE-HONG
 * <p>
 * Loads an n-gram language model. The n-gram language model
 * is pretrained with SRILM and save to an  ARPA file.  
 * <p>
 * P_backoff(b|a) = P(b|a) if count(a,b) > 0, or = alpha(a) * P_backoff(b) if 
 * count(a,b) = 0; where alpha(a) is the backoff weight associated with a. 
 * <p>
 */
public class Bigrams implements Serializable {
	
	private static final long serialVersionUID = 7615320413734822131L;
	
	/**
	 * (token, (probability, backoff weight))
	 */
	private Map<String, Tuple2<Double, Double>> ugrams;
	/**
	 * ((token1, token2)), probability)
	 */
	private Map<Tuple2<String, String>, Double> bgrams;
	
	
	/**
	 * Constructs a bigram language model from an ARPA file. 
	 * @param bigramARPAFileName
	 */
	public Bigrams(String bigramARPAFileName) {
		ugrams = new HashMap<String, Tuple2<Double, Double>>();
		bgrams = new HashMap<Tuple2<String, String>, Double>();
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(bigramARPAFileName), "UTF-8"));
			String line = null;
			boolean u = false;
			boolean b = false;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.contains("1-grams:")) {
					u = true;
				}
				if (!line.isEmpty() && u) {
					String[] parts = line.split("\\s+");
					if (parts.length == 3) {
						ugrams.put(parts[1], new Tuple2<Double, Double>(Double.parseDouble(parts[0]), Double.parseDouble(parts[2])));
					} else if (parts.length == 2) {
						// there is no backoff weight for this token
						ugrams.put(parts[1], new Tuple2<Double, Double>(Double.parseDouble(parts[0]), 0d));
					}
				}
				if (line.contains("2-grams:")) {
					u = false;
					b = true;
				}
				if (!line.isEmpty() && b) {
					String[] parts = line.split("\\s+");
					if (parts.length == 3) {
						bgrams.put(new Tuple2<String, String>(parts[1], parts[2]), Double.parseDouble(parts[0]));
					} 
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Computes log(P(a)) in base 10.
	 * @param a
	 * @return log(P(a)) in base 10.
	 */
	public double logProb(String a) {
		Tuple2<Double, Double> t = ugrams.get(a);
		if (t == null)
			return ugrams.get("<unk>")._1().doubleValue();
		else return t._1().doubleValue();
		
	}
	
	/**
	 * Computes log(P(b | a)) in base 10.
	 * @param a
	 * @param b
	 * @return log(P(b | a)) in base 10.
	 */
	public double logConditionalProb(String a, String b) {
		Tuple2<String, String> tuple = new Tuple2<String, String>(a, b);
		Double p = bgrams.get(tuple); 
		if (p != null) {
			return p.doubleValue();
		} else {
			double x = logProb(b);
			Tuple2<Double, Double> y = ugrams.get(a);
			if (y != null) {
				x += y._2().doubleValue(); // add the backoff weight (log_{10})
			}
			return x;
		}
	}

	/**
	 * Computes log(P(a,b)) in base 10.
	 * @param a
	 * @param b
	 * @return log(P(a,b)).
	 */
	public double logJointProb(String a, String b) {
		return logConditionalProb(a, b) + logProb(a);
	}
	
	public void statistics() {
		System.out.println("#(1-grams) = " + ugrams.size());
		System.out.println("#(2-grams) = " + bgrams.size());
	}
	
	public static void main(String[] args) {
		Bigrams ngram = new Bigrams("dat/tok/syllables2M.arpa");
		ngram.statistics();
		System.out.println(ngram.logProb("an_ninh"));
		System.out.println(ngram.logConditionalProb("an_ninh", "đểu"));
		System.out.println(ngram.logJointProb("an_ninh", "đểu"));
	}
}
