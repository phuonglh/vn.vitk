package vn.vitk.tok;

import java.io.Serializable;


public class WhitespaceContext implements Serializable {
	private static final long serialVersionUID = 62495434328214528L;
	private int id;
	private String context;
	private double label;
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public void setContext(String context) {
		this.context = context;
	}
	
	public String getContext() {
		return context;
	}
	
	public void setLabel(double label) {
		this.label = label;
	}
	
	public double getLabel() {
		return label;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(context);
		sb.append(',');
		sb.append(label);
		sb.append(']');
		return sb.toString();
	}
}
