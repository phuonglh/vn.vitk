package vn.vitk.dep;

import java.util.List;

/**
 * @author phuonglh, phuonglh@gmail.com
 *         <p>
 *         Evaluator of a dependency parser. It computes the parsing accuracy at
 *         token level or sentence level. Two types of scoring are evaluated:
 *         unlabeled attachment score (UAS) and labeled attachment score (LAS).
 */
public class Evaluator {
	private double lasToken = 0;
	private double lasSentence = 0;
	private double uasToken = 0;
	private double uasSentence = 0;

	public void evaluate(List<DependencyGraph> correctGraphs, List<DependencyGraph> proposedGraphs) {
		lasToken = 0;
		lasSentence = 0;
		uasToken = 0;
		uasSentence = 0;
		if (correctGraphs.size() != proposedGraphs.size())
			throw new IllegalArgumentException("Incompatible lengths!");
		double nTokens = 0;
		for (int i = 0; i < correctGraphs.size(); i++) {
			DependencyGraph cg = correctGraphs.get(i);
			DependencyGraph pg = proposedGraphs.get(i);
			Integer[] ch = cg.getHeads();
			Integer[] ph = pg.getHeads();
			double uMatch = 0;
			double lMatch = 0;
			for (int j = 1; j < ch.length; j++) { // not count the ROOT token
				if (ch[j].intValue() == ph[j].intValue()) {
					uMatch++;
					if (cg.getLabels()[j].equals(pg.getLabels()[j])) {
						lMatch++;
					}
				}
			}
			nTokens = nTokens + (ch.length - 1);
			uasToken = uasToken + uMatch;
			uasSentence = uasSentence + uMatch / (ch.length - 1);
			lasToken = lasToken + lMatch;
			lasSentence = lasSentence + lMatch / (ch.length - 1);
		}
		System.out.println("nTokens = " + nTokens);
		uasToken = uasToken / nTokens;
		System.out.println("nGraphs = " + correctGraphs.size());
		uasSentence = uasSentence / correctGraphs.size();
		lasToken = lasToken / nTokens;
		lasSentence = lasSentence / correctGraphs.size();
	}
	
	public double getLASToken() {
		return lasToken;
	}
	
	public double getLASSentence() {
		return lasSentence;
	}
	
	public double getUASToken() {
		return uasToken;
	}
	
	public double getUASSentence() {
		return uasSentence;
	}
}
