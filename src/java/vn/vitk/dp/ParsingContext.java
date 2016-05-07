package vn.vitk.dp;

import java.io.Serializable;


/**
 * @author Phuong Le-Hong <phuonglh@gmail.com>
 * <p>Jan 30, 2016, 9:08:00 PM
 * <p>
 * 	A parsing context contains an id and space-delimited feature strings.
 * This is designed as a JavaBean object in order to be usable within the 
 * Spark ML library.
 */
public class ParsingContext implements Serializable {
	private static final long serialVersionUID = -1305468109406288094L;
	private int id;
	private String text;
	private String transition;

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public String getTransition() {
		return transition;
	}
	
	public void setTransition(String transition) {
		this.transition = transition;
	}
}
