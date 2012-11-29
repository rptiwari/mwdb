package edu.mwdb.project;

import java.util.Map;

import org.apache.lucene.index.TermFreqVector;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;

/**
 * Task 5: Graph search task (content): Given a “co-author” or “co-authored papers” graph and an author 
 * or paperid, identify and list K most content-wise similar nodes (for a user supplied K).
 */
public class Task5 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Given a graph, return the topK most content-wise similar entries from the graph to the searchId
	 * @param graph “co-author” or “co-authored papers” matrix
	 * @param searchId author or paper Id to find similarities for
	 * @param topK number of results to return
	 * @param isAuthorId specifies if searchId is an author. True=authorId, False=paperId. 
	 * @return the most content-wise similar entries
	 * @throws MatlabInvocationException
	 * @throws MatlabConnectionException
	 * @throws Exception
	 */
	public static Object[] GraphSearchContent(double[][] graph, String searchId, int topK, boolean isAuthorId, Map<String, TermFreqVector> allAuthorsTermFreq) throws MatlabInvocationException, MatlabConnectionException, Exception {
		
		// Retrieve keyword vector for author or paper id "searchId"
		double[] keywordVector = null;
		if (isAuthorId) {
			TermFreqVector tfVector = allAuthorsTermFreq.get(searchId);
			String[] authorTerms = tfVector.getTerms();
			int[] authorTF = tfVector.getTermFrequencies();	
			
			// Convert from int[] to double[]
			keywordVector = new double[authorTF.length];
			for (int i=0; i<authorTF.length; i++) {
				keywordVector[i] = authorTF[i];
			}
		} else {
			throw new Exception("NOt Implemented yet");
		}
		
		// Use knn search to find 
		MatLab ml = new MatLab();
		return ml.knnSearch(graph, keywordVector, topK);
	}

}
