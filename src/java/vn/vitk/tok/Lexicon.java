package vn.vitk.tok;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import vn.vitk.tok.jaxb.N;
import vn.vitk.tok.jaxb.ObjectFactory;
import vn.vitk.util.UTF8FileIO;

/**
 * @author Phuong LE-HONG, <phuonglh@gmail.com>
 *         <p>
 *         A lexicon (dictionary) implementation using prefix tree data
 *         structure.
 *         <p>
 */
public class Lexicon implements Serializable {
    private static final long serialVersionUID = 4179824459160178446L;
	private Node root;
    private int numNodes = 0;
	private ObjectFactory factory = new ObjectFactory();
    
    /**
     * Creates an empty lexicon. 
     */
    public Lexicon() {
    	root = new Node('_');
    }

    /**
     * Creates a lexicon from a file containing a list of words.
     * @param fileName a file containing a list of words, each on a line. 
     */
    public Lexicon(String fileName) {
    	this();
    	String[] words = UTF8FileIO.readLines(fileName);
    	for (String word : words) {
    		addWord(word);
    	}
    }
    
    /**
     * Checks the existence of a word in the lexicon.
     * @param word
     * @return true/false
     */
    public boolean hasWord(String word) {
    	return root.hasWord(word, 0);
    }
    
    /**
     * Adds a word to the lexicon.
     * @param word
     * @return <code>true</code> if the word is added, <code>false</code> otherwise.
     */
    public boolean addWord(String word) {
    	if (word.length() == 0)
    		return false;
    	if (hasWord(word))
    		return false;
    	Node n = root;
    	int pos = 0;
    	while (pos < word.length() && n != null) {
    		// search a child of numNodes which has the same character
    		char k = word.charAt(pos);
    		boolean found = false;
    		for (Node fils : n.children) {
    			if (fils.c == k) {
    				found = true;
    				n = fils;
    				pos++;
    			}
    		}
    		if (!found) {
    			break;
    		}
    	}
    	// create new nodes for the rest of the string
    	for (int j = pos; j < word.length(); j++) {
    		Node u = new Node(word.charAt(j));
    		n.addChild(u);
    		n = u;
    	}
    	// the last node contains special character '*' 
    	n.addChild(new Node('*'));
    	numNodes += (word.length() - pos + 1); 
    	return true;
    }
    
    /**
     * Verifies whether a string is the prefix of a word in the lexicon.  
     * @param s
     * @return true/false
     */
    public boolean isPrefix(String s) {
    	if (s.length() == 0)
    		return false;
    	Node n = root;
    	int pos = 0;
    	while (pos < s.length() && n != null) {
    		// search for a child which has the same character
    		char k = s.charAt(pos);
    		boolean found = false;
    		for (Node fils : n.children) {
    			if (fils.c == k) {
    				found = true;
    				n = fils;
    				pos++;
    			}
    		}
    		if (!found) {
    			return false;
    		}
    	}
    	return true;
    }
    
    /**
     * Lists all the words of the lexicon in alphabetic order.
     */
    public void listAlphabeticWords() {
    	for (Node n : root.children)
    		n.listAlphabeticWords(new LinkedList<Character>());
    }
    
    /**
     * Gets the number of nodes in this lexicon tree.
     * @return
     */
    public int numNodes() {
    	return numNodes;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
    	return root.toString();
    }

    /**
     * Unmarshals dictionary from an XML file. 
     * @param fileName an XML file encoding a dictionary which is 
     * previously saved by {@link #save(String)} method.
     */
    public Lexicon load(String fileName) {
		try {
			JAXBContext context = JAXBContext.newInstance("vn.vitk.tok.jaxb");
			Unmarshaller unmarshaller = context.createUnmarshaller();
			Object object = unmarshaller.unmarshal(new FileInputStream(fileName));
			if (object instanceof N) {
				N n = (N) object;
				root = loadNode(n);
				return this;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return null;
    }
    
    private Node loadNode(N n) {
    	Node node = new Node(n.getC().charAt(0));
    	for (N k : n.getN()) {
    		Node c = loadNode(k);
    		node.addChild(c);
    	}
    	numNodes += n.getN().size();
    	return node;
    }
    
    /**
     * Marshals the lexicon to an XML file.
     * @param fileName an XML file.
     */
    public void save(String fileName) {
		try {
			// create a marshaller
			JAXBContext context = JAXBContext.newInstance("vn.vitk.tok.jaxb");
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
//			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			// marshal the lexicon
			try {
				OutputStream os = new FileOutputStream(fileName);
				marshaller.marshal(createN(root), os);
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		}
    }
    
    private N createN(Node node) {
		N n = factory.createN();
		n.setC(String.valueOf(node.c));
		for (Node child : node.children) {
			N c = createN(child); 
			n.getN().add(c);
		}
    	return n;
    }
    
    class Node implements Serializable {
    	private static final long serialVersionUID = 7858640386692889724L;
    	char c;
    	LinkedList<Node> children = new LinkedList<Node>();
    	
        Node(char c) {
        	this.c = c;
        }
        
    	public void addChild(Node a) {
    		// add a child into its correct position
    		int index = 0;
    		while (index < children.size() && children.get(index).c < a.c) {
    			index++;
    		}
    		children.add(index, a);
        }
        
        public boolean hasWord(String s, int pos) {
        	if (pos == s.length()) {
        		for (int j = 0; j < children.size(); j++) {
        			if (children.get(j).c == '*')
        				return true;
        		}
        		return false;
        	}
        	for (int j = 0; j < children.size(); j++) {
    			Node n = children.get(j);
    			if (n.c == s.charAt(pos)) {
    				return n.hasWord(s, pos + 1);
    			}
    		}
    		return false;
        }
        
    	public void listAlphabeticWords(LinkedList<Character> prefix) {
    		prefix.add(c);
    		for (Node n : children) {
    			if (n.c == '*') {
    				System.out.println(prefixToString(prefix));
    			} else {
    				n.listAlphabeticWords(prefix);
    			}
    		}
    		// backtracking
    		prefix.removeLast();
    	}
        
        private String prefixToString(LinkedList<Character> prefix) {
        	StringBuilder sb = new StringBuilder();
        	for (Character c : prefix) {
        		sb.append(c);
        	}
        	return sb.toString();
        }
        
        @Override
        public String toString() {
        	if (children.size() == 0) {
            	return "*"; 
        	}
        	StringBuilder sb = new StringBuilder();
        	sb.append(c);
        	sb.append('(');
        	if (children.size() == 1) {
        		Node node = children.getFirst();
        		sb.append(node);
        	} else {
    	    	for (int j = 0; j < children.size() - 1; j++) {
    	    		Node noeud = children.get(j);
    	    		sb.append(noeud);
    	    		sb.append(", ");
    	    	}
        		sb.append(children.getLast());
        	}
        	sb.append(')');
        	return sb.toString();
        }
        
    	/**
    	 * Finds a child containing a character or <code>null</code> if 
    	 * it does not exist. 
    	 * 
    	 * @param c
    	 * @return a node or <code>null</code>.
    	 */
    	public Node findChild(char c) {
    		for (Node n : children) {
    			if (n.c == c)
    				return n;
    		}
    		return null;
    	}

        /**
         * Tests whether this node marks the end of a word or not.
         * @return <code>true</code> or <code>false</code>
         */
        public boolean isWord() {
        	return findChild('*') != null;
        }
    }    
    
    public static void main(String[] args) {
//    	Lexicon lexicon = new Lexicon("dat/tok/lexiconv5.txt");
//    	lexicon.save("dat/tok/lexicon.xml");
    	Lexicon lexicon = new Lexicon();
    	lexicon.load("dat/tok/lexicon.xml");
    	System.out.println("#(nodes) = " + lexicon.numNodes());
    	System.out.println("Done.");
	}
}

