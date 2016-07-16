package vn.vitk.dep;

import java.io.Serializable;


/**
 * @author Phuong Le-Hong <phuonglh@gmail.com>
 *         <p>
 *         Jan 30, 2016, 9:08:00 PM
 *         <p>
 *         A parsing context contains an id and space-delimited feature strings.
 *         This is designed as a JavaBean object in order to be usable within
 *         the Spark ML library.
 */
public class ParsingContext implements Serializable {
	private static final long serialVersionUID = -1305468109406288094L;
	private int id;
	private String text;
	private String transition;

	/**
	 * Gets the id of this parsing context.
	 * @return the id of this parsing context.
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Set the id.
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * Gets the text of this context.
	 * @return the text.
	 */
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	/**
	 * Gets the transition, or the label of this context.
	 * @return the transition associated with this context.
	 */
	public String getTransition() {
		return transition;
	}
	
	/**
	 * Set the transition of this context.
	 * @param transition
	 */
	public void setTransition(String transition) {
		this.transition = transition;
	}
}
