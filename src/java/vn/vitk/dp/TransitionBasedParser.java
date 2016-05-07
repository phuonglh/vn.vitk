package vn.vitk.dp;

import java.util.ArrayList;
import java.util.List;

import vn.vitk.dp.DependencyGraph;
import vn.vitk.dp.DependencyGraphReader;
import vn.vitk.dp.DependencyParser;
import vn.vitk.dp.Evaluator;
import vn.vitk.dp.FeatureFrame;
import vn.vitk.dp.TransitionBasedParserMLP;
import vn.vitk.dp.Sentence;
import vn.vitk.lang.CorpusPack;
import vn.vitk.lang.Language;

/**
 * @author Phuong LE-HONG
 * <p>
 * Feb 5, 2016, 1:35:11 PM
 * <p>
 * Test the MLP transition-based dependency parser.
 * 
 */
public class TransitionBasedParser {

	static CorpusPack pack = new CorpusPack(Language.VIETNAMESE);
	static DependencyParser parser = new TransitionBasedParserMLP(
			"local[*]", 
			pack.dependencyClassifierFileName(), 
			pack.dependencyTransitionIndexFileName(),
			pack.dependencyFeatureIndexFileName(),
			FeatureFrame.TAG_TOKEN_TYPE);
	
	static void test1() {
		String[] tokens = {"ROOT", "Xã", "không", "còn", "hộ", "nghèo", "."};
		String[] tags = {"ROOT", "N", "R", "V", "N", "A", "."};
		Sentence sentence = new Sentence(tokens, tags);
		DependencyGraph graph = parser.parse(sentence);
		System.out.println(graph);
	}
	
	static void test2() {
		List<DependencyGraph> graphs = DependencyGraphReader.read("dat/00.conll");
		for (DependencyGraph graph : graphs) {
			DependencyGraph proposedGraph = parser.parse(graph.getSentence());
			System.out.println(proposedGraph);
		}
	}
	
	static void test3() {
//		List<DependencyGraph> graphs = DependencyGraphReader.read("dat/01-test.conll");
		List<DependencyGraph> graphs = DependencyGraphReader.read("dat/01-training.conll");
		List<Sentence> sentences = new ArrayList<Sentence>();
		for (DependencyGraph graph : graphs) {
			sentences.add(graph.getSentence());
		}
		List<DependencyGraph> proposedGraphs = parser.parse(sentences);
		Evaluator evaluator = new Evaluator();
		evaluator.evaluate(graphs, proposedGraphs);
		System.out.println("UAS(token) = " + evaluator.getUASToken());
		System.out.println("UAS(sentence) = " + evaluator.getUASSentence());
		System.out.println("LAS(token) = " + evaluator.getLASToken());
		System.out.println("LAS(sentence) = " + evaluator.getLASSentence());
	}
	
	static void test() {
		List<DependencyGraph> graphs = pack.dependencyTreebankTest();
		List<Sentence> sentences = new ArrayList<Sentence>();
		for (DependencyGraph graph : graphs) {
			sentences.add(graph.getSentence());
		}
		List<DependencyGraph> proposedGraphs = parser.parse(sentences);
		Evaluator evaluator = new Evaluator();
		evaluator.evaluate(graphs, proposedGraphs);
		System.out.println("UAS(token) = " + evaluator.getUASToken());
		System.out.println("UAS(sentence) = " + evaluator.getUASSentence());
		System.out.println("LAS(token) = " + evaluator.getLASToken());
		System.out.println("LAS(sentence) = " + evaluator.getLASSentence());
	}
	
	public static void main(String[] args) {
//		test1();
//		test2();
//		test3();
		test();
	}

}
