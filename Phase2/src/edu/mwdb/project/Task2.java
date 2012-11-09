package edu.mwdb.project;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task2 {

	DblpData dblpData;
	Utility utils;
	MatLab matlab;
	
	public Task2() throws Exception {
		dblpData = new DblpData();
		utils = new Utility();
		matlab = new MatLab();
	}
	
	
	/**
	 * It creates an author-author similarity matrix using keyword vectors, performs SVD on this matrix, and gets the top-3 latent semantics
	 * @return a 2d array containing key-value pairs. Key is the authorId, and Value is the weight.
	 */
	public Map.Entry<String, Double>[][] getTop3LatSemBySVD_AuthorAuthor() {
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
		SimilarityAnalyzer sa = new SimilarityAnalyzer();
		
		double[][] svdMatrix = null;
		try {
			double[][] simMatrix = sa.getAuthorSimilarityMatrix(authorTermFreq, allTerms);
			svdMatrix = matlab.svd(simMatrix, 3);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Map.Entry[][] retVal = new Map.Entry[svdMatrix.length][svdMatrix[0].length];
		String[] authorIds = authorTermFreq.keySet().toArray(new String[0]);
		for(int i=0; i<svdMatrix.length; i++) {
			for (int j=0; j<svdMatrix[i].length; j++) {
				retVal[i][j] = new AbstractMap.SimpleEntry<String, Double>(authorIds[j], svdMatrix[i][j]);
			}
		}
		
		return retVal;
	}
	
	/**
	 * It creates an coauthor-coauthor similarity matrix using keyword vectors, performs SVD on this matrix, and gets the top-3 latent semantics
	 * @return a 2d array containing key-value pairs. Key is the authorId, and Value is the weight.
	 */
	public Map.Entry<String, Double>[][] getTop3LatSemBySVD_CoAuthorCoAuthor() {
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
		SimilarityAnalyzer sa = new SimilarityAnalyzer();
		
		double[][] svdMatrix = null;
		try {
			double[][] simMatrix = sa.getCoAuthorSimilarityMatrix(authorTermFreq, allTerms);
			svdMatrix = matlab.svd(simMatrix, 3);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Map.Entry[][] retVal = new Map.Entry[svdMatrix.length][svdMatrix[0].length];
		String[] authorIds = authorTermFreq.keySet().toArray(new String[0]);
		for(int i=0; i<svdMatrix.length; i++) {
			for (int j=0; j<svdMatrix[i].length; j++) {
				retVal[i][j] = new AbstractMap.SimpleEntry<String, Double>(authorIds[j], svdMatrix[i][j]);
			}
		}
		
		return retVal;
	}
}
