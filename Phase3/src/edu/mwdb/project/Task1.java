package edu.mwdb.project;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task1 {

	DblpData dblpData;
	Utility utils;
	
	public Task1() throws Exception {
		dblpData = new DblpData();
		utils = new Utility();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Task1 t1;
		try {
			t1 = new Task1();
			Graph g = t1.getCoauthorSimilarityGraph_KeywordVector();
			printGraph(g);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void printGraph(Graph g) {
		DecimalFormat f = new DecimalFormat("#.##");
		for(int i=0; i<g.getNumNodes(); i++)
		{
			System.out.print("\t" + g.getNodeLabel(i));
		}
		System.out.println();
		for(int i=0; i<g.getNumNodes(); i++)
		{
			System.out.print(g.getNodeLabel(i) + "\t");
			for(int j=0; j<g.getNumNodes(); j++)
			{
				System.out.print(f.format(g.getAdjacencyMatrix()[i][j]) + "\t");
			}
			System.out.print("\n");
		}
	}
	
	public Graph getCoauthorSimilarityGraph_KeywordVector() throws Exception{
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");

		int authIdx = 0;
		Map<Integer, String> authorIndexMap = new HashMap<Integer, String>();
		for(String authorId:authorTermFreq.keySet()){
			authorIndexMap.put(authIdx++, authorId);
		}

		int termIdx = 0;
		Map<String, Integer> allKeywordsPosMap = new HashMap<String, Integer>();
		for(String kw:allTerms){
			allKeywordsPosMap.put(kw, termIdx++);
		}

		int numAuthors = authorTermFreq.keySet().size();
		double[][] similarityMatrix = new double[numAuthors][numAuthors];

		Map<String, Set<String>> coauthorsMap = dblpData.getCoauthors();

		for(int i=0; i<numAuthors; i++){
			for(int j=0; j<numAuthors; j++){
				String authorId1 = authorIndexMap.get(i);
				String authorId2 = authorIndexMap.get(j);
				double cosineSim = 0;	
				if(coauthorsMap.get(authorId1).contains(authorId2)){
					double[] a1 = Utility.getAlignedTermFreqVector(authorTermFreq.get(authorId1), allKeywordsPosMap);
					double[] a2 = Utility.getAlignedTermFreqVector(authorTermFreq.get(authorId2), allKeywordsPosMap);
					cosineSim = utils.cosineSimilarity(a1, a2);
				}
				similarityMatrix[i][j] = cosineSim;
			}
		}
		
		Graph g = new Graph(similarityMatrix, authorIndexMap);
		return g;
	}

}
