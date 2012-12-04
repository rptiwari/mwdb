package edu.mwdb.project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task1 {

	DblpData dblpData;
	Utility utils;
	MatLab matlab;
	
	public Task1() throws Exception {
		dblpData = new DblpData();
		utils = new Utility();
		matlab = new MatLab();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Task1 t1;
		try {
			t1 = new Task1();
			//Graph g = t1.getCoauthorSimilarityGraph_KeywordVector();
			//Graph g = t1.getCoauthorSimilarityGraph_PCA();
			Graph g = t1.getCoauthorSimilarityGraph_SVD();
			Utility.printGraph(g);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private double getTopkSemanticSimilarity(double[][] author1topksemantics, double[][] author2topksemantics, int k) throws Exception{
		double sim = 0;
		for(int i=0; i<k; i++){
			for(int j=0; j<k;j++){
				sim += utils.cosineSimilarity(author1topksemantics[i], author2topksemantics[j]);
			}
		}
		return sim;
	}
	
	
	public Graph getCoauthorSimilarityGraph_SVD() throws Exception{
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
	
		Map<Integer, String> authorIndexMap = new HashMap<Integer, String>();
		
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		int authIdx = 0;
		for(String authorId:authorTermFreq.keySet()){
			authorIndexMap.put(authIdx++, authorId);
		}
		
		Map<String, Set<String>> coauthorsMap = dblpData.getCoauthors();
		Map<String, double[][]> topksemantics = new HashMap<String, double[][]>();
		int numAuthors = authorTermFreq.keySet().size();
		double[][] similarityMatrix = new double[numAuthors][numAuthors];
		for(int i=0; i<numAuthors; i++){
			for(int j=0; j<numAuthors; j++){
				String authorId1 = authorIndexMap.get(i);
				String authorId2 = authorIndexMap.get(j);
				double sim = 0;	
				if(coauthorsMap.get(authorId1).contains(authorId2)){
					
					double[][] author1top3Semantics = getTop3Semantics_SVD(
							allTerms, topksemantics, authorId1);
					
					double[][] author2top3semantics = getTop3Semantics_SVD(
							allTerms, topksemantics, authorId2);

					if(author1top3Semantics != null && author2top3semantics != null){
						sim = getTopkSemanticSimilarity(author1top3Semantics, author2top3semantics, 3);
					}
				}
				similarityMatrix[i][j] = sim;
			}
		}
		Graph g = new Graph(similarityMatrix, authorIndexMap);
		return g;
	}
	
	
	public Graph getCoauthorSimilarityGraph_PCA() throws Exception{
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
	
		Map<Integer, String> authorIndexMap = new HashMap<Integer, String>();
		
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		int authIdx = 0;
		for(String authorId:authorTermFreq.keySet()){
			authorIndexMap.put(authIdx++, authorId);
		}
		
		Map<String, Set<String>> coauthorsMap = dblpData.getCoauthors();
		Map<String, double[][]> topksemantics = new HashMap<String, double[][]>();
		int numAuthors = authorTermFreq.keySet().size();
		double[][] similarityMatrix = new double[numAuthors][numAuthors];
		for(int i=0; i<numAuthors; i++){
			for(int j=0; j<numAuthors; j++){
				String authorId1 = authorIndexMap.get(i);
				String authorId2 = authorIndexMap.get(j);
				double sim = 0;	
				if(coauthorsMap.get(authorId1).contains(authorId2)){
					
					double[][] author1top3Semantics = getTop3Semantics_PCA(
							allTerms, topksemantics, authorId1);
					
					double[][] author2top3semantics = getTop3Semantics_PCA(
							allTerms, topksemantics, authorId2);

					if(author1top3Semantics != null && author2top3semantics != null){
						sim = getTopkSemanticSimilarity(author1top3Semantics, author2top3semantics, 3);
					}
				}
				similarityMatrix[i][j] = sim;
			}
		}
		Graph g = new Graph(similarityMatrix, authorIndexMap);
		return g;
	}

	private double[][] getTop3Semantics_SVD(List<String> allTerms,
			Map<String, double[][]> topksemantics, String authorId)
			throws Exception {
		double[][] authortop3Semantics = null;
		if(topksemantics.containsKey(authorId)){
			authortop3Semantics = topksemantics.get(authorId);
		}
		else{
			double[][] author1docMatrix = utils.getAuthor_DocTermMatrix(authorId, allTerms, dblpData.getPaperIdsFromAuthor(authorId), false, null);
			if(author1docMatrix.length > 0){
				authortop3Semantics = matlab.svd(author1docMatrix, 3);
				topksemantics.put(authorId, authortop3Semantics);
			}
		}
		return authortop3Semantics;
	}
	
	
	
	private double[][] getTop3Semantics_PCA(List<String> allTerms,
			Map<String, double[][]> topksemantics, String authorId)
			throws Exception {
		double[][] authortop3Semantics = null;
		if(topksemantics.containsKey(authorId)){
			authortop3Semantics = topksemantics.get(authorId);
		}
		else{
			double[][] author1docMatrix = utils.getAuthor_DocTermMatrix(authorId, allTerms, dblpData.getPaperIdsFromAuthor(authorId), false, null);
			if(author1docMatrix.length > 0){
				authortop3Semantics = matlab.pca(author1docMatrix, 3);
				topksemantics.put(authorId, authortop3Semantics);
			}
		}
		return authortop3Semantics;
	}
	
	
	public Graph getCoauthorSimilarityGraph_KeywordVector() throws Exception{
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		

		int authIdx = 0;
		Map<Integer, String> authorIndexMap = new HashMap<Integer, String>();
		for(String authorId:authorTermFreq.keySet()){
			authorIndexMap.put(authIdx++, authorId);
		}

		Map<String, Integer> allKeywordsPosMap = getAllTermsPosMap(indexDir);

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

	private Map<String, Integer> getAllTermsPosMap(Directory indexDir) {
		int termIdx = 0;
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
		Map<String, Integer> allKeywordsPosMap = new HashMap<String, Integer>();
		for(String kw:allTerms){
			allKeywordsPosMap.put(kw, termIdx++);
		}
		return allKeywordsPosMap;
	}

}
