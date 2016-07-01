package vn.vitk.tok;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * @author Phuong LE-HONG
 *         <p>
 *         May 10, 2016, 4:08:28 PM
 *         <p>
 *         Implementation of the Dijkstra algorithm using a priority queue. This
 *         utility helps find all shortest paths in a phrase graph in a
 *         segmentation.
 * 
 */
public class Dijkstra implements Serializable {
	
	private static final long serialVersionUID = 2342428661855874411L;
	/**
	 * Number of vertices in the graph.
	 */
	private int n;
	/**
	 * For each vertex v, we store a list of vertices u where (u, v) is an edge
	 * of the graph.  
	 */
	private Map<Integer, LinkedList<Integer>> edges = new HashMap<Integer, LinkedList<Integer>>();

	private float[] distance;
	private int[] previous;
	
	/**
	 * Constructs a Dijkstra object.
	 * @param edges
	 */
	public Dijkstra(Map<Integer, LinkedList<Integer>> edges) {
		this.edges = edges;
		n = edges.size();
		distance = new float[n];
		previous = new int[n];
		
		distance[n-1] = 0;
		previous[n-1] = 0;
		
		for (int i = 0; i < n-1; i++) {
			distance[i] = Float.MAX_VALUE;
			previous[i] = -1;
		}
		
		PriorityQueue<Vertex> queue = new PriorityQueue<Vertex>(n);
		queue.add(new Vertex(n-1, distance[n-1]));
		while (!queue.isEmpty()) {
			Vertex v = queue.poll();
			LinkedList<Integer> nodes = this.edges.get(v.vertex);
			for (Integer u : nodes) {
				float d = length(u, v.vertex) + distance[v.vertex];
				if (d < distance[u]) {
					distance[u] = d;
					previous[u] = v.vertex;
					queue.add(new Vertex(u, distance[u]));
				}
			}
		}
	}
	
	/**
	 * The length between two nodes u and v. In a phrase graph, this 
	 * length is equal to 1.0/(v-u).
	 * @param u
	 * @param v
	 * @return the length between u and v.
	 */
	public float length(int u, int v) {
		if (u == v)
			return 0;
		else 
			return 1f/(v - u);
	}

	/**
	 * Finds all shortest paths on this graph, between vertex <code>0</code>
	 * and vertex <code>n-1</code>.
	 * @return all shortest paths.
	 */
	public List<LinkedList<Integer>> shortestPaths() {
		return shortestPaths(n-1);
	}
	
	/**
	 * Finds all shortest paths on this graph, between vertex <code>0</code>
	 * and vertex <code>v</code>.
	 * @param v a vertex
	 * @return all shortest paths
	 */
	private List<LinkedList<Integer>> shortestPaths(int v) {
		LinkedList<Integer> nodes = edges.get(v);
		if (nodes.isEmpty()) {
			LinkedList<Integer> stop = new LinkedList<Integer>();
			stop.add(v);
			List<LinkedList<Integer>> list = new ArrayList<LinkedList<Integer>>();
			list.add(stop);
			return list;
		} else {
			List<LinkedList<Integer>> vList = new ArrayList<LinkedList<Integer>>();
			for (Integer u : nodes) {
				float d = length(u, v) + distance[v];
				if (new Float(distance[u]).compareTo(new Float(d)) == 0) {
					// recursively compute the paths from u
					List<LinkedList<Integer>> uList = shortestPaths(u);
					for (LinkedList<Integer> list : uList) {
						list.add(v);
						vList.add(list);
					}
				}
			}
			return vList;
		}
	}
	
	void print() {
		System.out.printf("v\tdist\tprev\n");
		for (int i = 0; i < n; i++) {
			System.out.printf("%d\t%6.4f\t%d\n", i, distance[i], previous[i]);
		}
	}
	
	class Vertex implements Comparable<Vertex>, Serializable {

		private static final long serialVersionUID = 6980341654036165566L;
		private int vertex = -1;
		private double priority = Double.MAX_VALUE;
		
		public Vertex(int v, double p) {
			vertex = v;
			priority = p;
		}
		
		public int getVertex() {
			return vertex;
		}
		
		public double getPriority() {
			return priority;
		}

		@Override
		public int compareTo(Vertex v) {
			if (priority > v.priority)
				return 1;
			else if (priority < v.priority)
				return -1;
			else return 0;
		}
	}
	
	/**
	 * For internal test only.
	 * @param args
	 */
	public static void main(String[] args) {
		Map<Integer, LinkedList<Integer>> edges = new TreeMap<Integer, LinkedList<Integer>>();
		// test 1
		for (int v = 0; v < 6; v++)
			edges.put(v, new LinkedList<Integer>());
		edges.get(5).add(4);
//		edges.get(5).add(3);
		edges.get(4).add(3);
		edges.get(4).add(2);
		edges.get(3).add(2);
		edges.get(3).add(1);
		edges.get(2).add(1);
		edges.get(1).add(0);
		Dijkstra dijkstra = new Dijkstra(edges);
		dijkstra.print();
		System.out.println("Shortest paths: ");
		List<LinkedList<Integer>> paths = dijkstra.shortestPaths();
		for (LinkedList<Integer> path : paths) {
			System.out.println(path);
		}
		// test 2
		edges.clear();
		for (int v = 0; v < 3; v++)
			edges.put(v, new LinkedList<Integer>());
		edges.get(2).add(1);
		edges.get(2).add(0);
		edges.get(1).add(0);
		dijkstra = new Dijkstra(edges);
		dijkstra.print();
		System.out.println("Shortest paths: ");
		paths = dijkstra.shortestPaths();
		for (LinkedList<Integer> path : paths) {
			System.out.println(path);
		}
	}
}
