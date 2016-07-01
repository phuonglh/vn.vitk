package vn.vitk.tag;

import java.io.Serializable;

/**
 * @author Phuong LE-HONG, <phuonglh@gmail.com>
 *         <p>
 *         May 13, 2016, 11:29:16 AM
 *         <p>
 *         A Viterbi decoder on a score lattice. The lattice is supposed to
 *         store log probabilities of events and therefore, this decoder find a
 *         path on the lattice whose sum of scores along the path is maximized.
 */
public class ViterbiDecoder implements Serializable {

	private static final long serialVersionUID = -3028685262078557081L;
	/**
	 * A score lattice of size <code>sxn</code> where <code>s</code> is
	 * the number of states, and <code>n</code> is the sequence length.
	 */
	private double[][] score;
	
	/**
	 * Creates a Viterbi decoder on a lattice of scores.
	 * @param score
	 */
	public ViterbiDecoder(double[][] score) {
		this.score = score;
	}
	
	/**
	 * The Viterbi decoding algorithm.
	 * @return the best label sequence.
	 */
	public int[] bestPath() {
		if (score == null || score.length == 0)
			return null;
		int numLabels = score.length;
		int n = score[0].length;
		double[][] tabular = new double[numLabels][n];
		for (int i = 0; i < numLabels; i++)
			tabular[i][0] = score[i][0];
		
		int[] path = new int[n];
		
		for (int j = 1; j < n; j++) {
			for (int i = 0; i < numLabels; i++) {
				double max = Double.NEGATIVE_INFINITY;
				for (int k = 0; k < numLabels; k++) {
					double s = tabular[k][j-1] + score[i][j]; 
					if (s > max) {
						max = s;
					}
				}
				tabular[i][j] = max;
			}
		}
		
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < numLabels; i++) {
			if (tabular[i][n-1] > max) {
				max = tabular[i][n-1];
				path[n-1] = i;
			}
		}
		for (int j = n-2; j >= 0; j--) {
			int u = path[j+1];
			for (int k = 0; k < numLabels; k++) {
				if (new Double(tabular[u][j+1]).equals(new Double(tabular[k][j] + score[u][j+1]))) {
					path[j] = k;
					break;
				}
			}
		}
		return path;
	}
	
	
	void print(double[][] tabular) {
		int m = tabular.length;
		int n = tabular[0].length;
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++)
				System.out.printf("%10.5f", tabular[i][j]);
			System.out.println();
		}
		
	}
	
	/**
	 * For internal test only.
	 * @param args
	 */
	public static void main(String[] args) {
		// test 1
		double[][] score = {
				{0.3, 0.1, 0.0},
				{0.2, 0.4, 0.1},
				{0.1, 0.4, 0.2}};
		ViterbiDecoder vd = new ViterbiDecoder(score);
		int[] path = vd.bestPath();
		System.out.println("Best path: ");
		for (int i : path)
			System.out.println(i);
		
		// test 2
		double[][] score2 = { 
				{ 29, 4, 20, 46, 30 }, 
				{ 13, 95, 52, 33, 56 },
				{ 87, 25, 19, 50, 23 }, 
				{ 92, 28, 28, 45, 54 },
				{ 30, 64, 25, 29, 80 }};
		vd = new ViterbiDecoder(score2);
		path = vd.bestPath();
		System.out.println("Best path: ");
		for (int i : path)
			System.out.println(i);
		
		// test 3
		double[][] score3 = {
				{ 0.321223, 0.726969, 0.802805, 0.747823, 0.848001 },
				{ 0.509316, 0.330791, 0.186159, 0.800983, 0.320709 },
				{ 0.078514, 0.110131, 0.688176, 0.080399, 0.823965 },
				{ 0.602709, 0.017961, 0.162477, 0.962956, 0.897437 },
				{ 0.208598, 0.232417, 0.286169, 0.865968, 0.181086 }};
		vd = new ViterbiDecoder(score3);
		path = vd.bestPath();
		System.out.println("Best path: ");
		for (int i : path)
			System.out.println(i);

		// test 4
		double[][] score4 = {
				{2, 0, 1, 0, 8, 0},
				{2, 3, 0, 2, 8, 5},
				{2, 2, 0, 7, 1, 2},
				{0, 9, 0, 9, 1, 5}
		};
		vd = new ViterbiDecoder(score4);
		path = vd.bestPath();
		System.out.println("Best path: ");
		for (int i : path)
			System.out.println(i);
	}
}
