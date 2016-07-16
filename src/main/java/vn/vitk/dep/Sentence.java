package vn.vitk.dep;

import java.io.Serializable;


/**
 * @author Phuong Le-Hong <phuonglh@gmail.com>
 *         <p>
 *         Jan 29, 2016, 7:53:43 PM
 *         <p>
 *         A Sentence to use in the parser.
 */
public final class Sentence implements Serializable {
	private static final long serialVersionUID = 3840032269496124130L;
	private String[] tokens;
	private String[] lemmas;
	private String[] tags;
	private int[] position;
	
	/**
	 * Constructs a Sentence object from a plain space-delimited sentence, e.g., 
	 * "Tom likes Jerry ." 
	 * @param sentence
	 * @param tags
	 */
	public Sentence(String sentence, String tag) {
		tokens = sentence.split("\\s+");
		lemmas = tokens;
		tags = tag.split("\\s+");
		if (tokens.length != tags.length) {
			throw new IllegalArgumentException("Lengths do not match!");
		}
		position = new int[tokens.length];
		for (int i = 0; i < tokens.length; i++) {
			position[i] = i;
		}
	}
	
	/**
	 * Constructs a Sentence object from an array of tokens, e.g., "{"Tom", "likes", "Jerry", "."}, 
	 * and an array of part-of-speech tags.
	 * @param tokens
	 * @param tags
	 */
	public Sentence(String[] tokens, String[] tags) {
		this.tokens = tokens;
		this.lemmas = tokens;
		this.tags = tags;
		position = new int[tokens.length];
		for (int i = 0; i < tokens.length; i++) {
			position[i] = i;
		}
	}

	/**
	 * Constructs a Sentence object from three arrays.
	 * @param tokens
	 * @param lemmas
	 * @param tags
	 */
	public Sentence(String[] tokens, String[] lemmas, String[] tags) {
		this(tokens, tags);
		this.lemmas = lemmas;
	}
	
	/**
	 * Gets the length of the sentence.
	 * @return the number of tokens of the sentence.
	 */
	public int length() {
		if (tokens != null)
			return tokens.length;
		else return 0;
	}
	
	/**
	 * Gets the token at a position.
	 * @param position
	 * @return a token in the sentence.
	 */
	public String getToken(int position) {
		if (position < 0 || position >= length())
			throw new IllegalArgumentException("Invalid position!");
		return tokens[position];
	}

	/**
	 * Gets the lemma at a position.
	 * @param position
	 * @return a lemma in the sentence
	 */
	public String getLemma(int position) {
		if (position < 0 || position >= length()) 
			throw new IllegalArgumentException("Invalid position!");
		return lemmas[position];
	}
	
	/**
	 * Gets the tag at a position.
	 * @param position
	 * @return a token in the sentence.
	 */
	public String getTag(int position) {
		if (position < 0 || position >= length())
			throw new IllegalArgumentException("Invalid position!");
		return tags[position];
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(256);
		for (int j = 0; j < tokens.length; j++) {
			sb.append(tokens[j]);
			sb.append('/');
			sb.append(tags[j]);
			sb.append(' ');
		}
		return sb.toString().trim();
	}
}
