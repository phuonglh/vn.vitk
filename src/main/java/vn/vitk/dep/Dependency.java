package vn.vitk.dep;

/**
 * @author Phuong Le-Hong <phuonglh@gmail.com>
 * <p>Jan 29, 2016, 7:52:08 PM
 * <p>
 *	A dependency relation 
 */
public class Dependency {
	private int head = -1;
	private int dependent = -1;
	private String type;
	
	public Dependency(int head, int dependent, String type) {
		this.head = head;
		this.dependent = dependent;
		this.type = type;
	}
	
	public Dependency(int head, int dependent) {
		this(head, dependent, "");
	}
	
	public int getHead() {
		return head;
	}
	
	public int getDependent() {
		return dependent;
	}
	
	public String getType() {
		return type;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(32);
		sb.append(type);
		sb.append('(');
		sb.append(head);
		sb.append(',');
		sb.append(dependent);
		sb.append(')');
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof Dependency))
			return false;
		Dependency o = (Dependency)obj;
		return ((head == o.head) && (dependent == o.dependent) && (type.equals(o.type)));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int h = head;
		h = h * 31 + dependent;
		h = h * 31 + type.hashCode(); 
		return h;
	}
}
