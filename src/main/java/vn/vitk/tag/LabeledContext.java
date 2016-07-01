package vn.vitk.tag;

import java.io.Serializable;

/**
 * @author Phuong LE-HONG
 *         <p>
 *         May 11, 2016, 5:15:17 PM
 *         <p>
 *         A labeled context in a CMM. This is designed as a JavaBean
 *         to facilitate data conversion.
 */

public class LabeledContext implements Serializable {

	private static final long serialVersionUID = 4407904622770252221L;
	
	/**
	 * The current word of this context.
	 */
	private String word;
	
	/**
	 * The tag associated with this context.
	 */
	private String tag;
	
	/**
	 * Features of the context which are separated by whitespace characters.
	 */
	private String featureStrings;

	public LabeledContext() {}

	public void setWord(String word) {
		this.word = word;
	}
	
	public String getWord() {
		return word;
	}
	
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public String getTag() {
		return tag;
	}

	public String getFeatureStrings() {
		return featureStrings;
	}
	
	public void setFeatureStrings(String featureStrings) {
		this.featureStrings = featureStrings;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		sb.append(word);
		sb.append(',');
		sb.append(tag);
		sb.append(',');
		sb.append('{');
		sb.append(featureStrings);
		sb.append('}');
		sb.append(')');
		return sb.toString();
	}
}
