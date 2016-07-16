package vn.vitk.lang;

import java.util.List;

import vn.vitk.dep.DependencyGraph;
import vn.vitk.dep.DependencyGraphReader;

/**
 * @author Phuong LE-HONG, <phuonglh@gmail.com>
 *         <p>
 *         Mar 15, 2016, 2:44:37 PM
 *         <p>
 *         Some string constants of corpus and data resources corresponding to a
 *         language.
 */
public class CorpusPack {
	private Language language;
	
	/**
	 * Creates a corpus pack for a specified language.
	 * @param languge
	 */
	public CorpusPack(Language language) {
		this.language = language;
	}
	
	/**
	 * Gets training dependency treebank.
	 * @return a training dependency treebank
	 */
	public List<DependencyGraph> dependencyTreebankTraining() {
		switch (language) {
		case ENGLISH:
			return DependencyGraphReader.read("/export/dat/udt/en/en-ud-train.conllu", 'u');
		case VIETNAMESE:
			return DependencyGraphReader.read("/export/dat/udt/vi/01-training.conll", 'x');
		}
		return null;
	}

	/**
	 * Gets development dependency treebank.
	 * @return a development dependency treebank
	 */
	public List<DependencyGraph> dependencyTreebankDevelopment() {
		switch (language) {
		case ENGLISH:
			return DependencyGraphReader.read("/export/dat/udt/en/en-ud-dev.conllu", 'u');
		case VIETNAMESE:
			return DependencyGraphReader.read("/export/dat/udt/vi/01-test.conll", 'x');
		}
		return null;
	}
	
	/**
	 * Gets test dependency treebank.
	 * @return a test dependency treebank
	 */
	public List<DependencyGraph> dependencyTreebankTest() {
		switch (language) {
		case ENGLISH:
			return DependencyGraphReader.read("/export/dat/udt/en/en-ud-test.conllu", 'u');
		case VIETNAMESE:
			return DependencyGraphReader.read("/export/dat/udt/vi/01-test.conll", 'x');
		}
		return null;
	}
	
	public String dependencyTransitionIndexFileName() {
		switch (language) {
		case ENGLISH:
			return "/export/dat/udt/en/model-train-transitionIndex";
		case VIETNAMESE:
			return "/export/dat/udt/vi/model-train-01-transitionIndex";
		}
		return null;
	}

	public String dependencyFeatureIndexFileName() {
		switch (language) {
		case ENGLISH:
			return "/export/dat/udt/en/model-train-featureIndex";
		case VIETNAMESE:
			return "/export/dat/udt/vi/model-train-01-featureIndex";
		}
		return null;
	}

	public String dependencyClassifierFileName() {
		switch (language) {
		case ENGLISH:
			return "/export/dat/udt/en/model-train-mlp-2layers";
//			return "/export/dat/udt/en/model-train-mlp-3layers";
		case VIETNAMESE:
			return "/export/dat/udt/vi/model-train-01-mlp-2layers";
		}
		return null;
	}

	/**
	 * The tagger model filename 
	 * @return
	 */
	public String taggerModelFileName() {
		switch (language) {
		case ENGLISH:
			return "/export/dat/tag/en/cmm";
		case VIETNAMESE:
			return "/export/dat/tag/vi/cmm";
		}
		return null;
	}
	
	/**
	 * The filename containing all tagged corpus.
	 * @return
	 */
	public String taggerCorpusFileName() {
		switch (language) {
		case ENGLISH:
			return "/export/dat/tag/en/brown.txt";
		case VIETNAMESE:
			return "/export/dat/tag/vi/vtb-tagged.txt";
		}
		return null;
	}
	
}
