package vn.vitk.dp;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Phuong Le-Hong <phuonglh@gmail.com>
 * <p>Jan 29, 2016 7:46:41 PM
 * <p>
 * Abstract dependency parser.
 * 
 */
public abstract class DependencyParser {
	/**
	 * Parses a sentence
	 * @param sentence
	 * @return a dependency graph
	 */
	public abstract DependencyGraph parse(Sentence sentence);
	
	/**
	 * Parses a list of sentences.
	 * @param sentences
	 * @return a list of parsing results, each result is a list of dependency.
	 */
	public List<DependencyGraph> parse(List<Sentence> sentences) {
		List<DependencyGraph> results = new ArrayList<DependencyGraph>();
		for (Sentence s : sentences) {
			results.add(parse(s));
		}
		return results;
	}
	
}
