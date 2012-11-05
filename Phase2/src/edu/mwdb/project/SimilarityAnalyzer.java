package edu.mwdb.project;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.TermFreqVector;

public class SimilarityAnalyzer {

	public double[][] getAuthorSimilarityMatrix(Map<String, TermFreqVector> authorKeywordVector, List<String> completeKeywordList) throws Exception{
		Utility util = new Utility();
		Map<String, Integer> authorIndexMap = new HashMap<String, Integer>();
		List<String> authorList = new ArrayList<String>();
		int authIdx = 0;
		for(String authorId:authorKeywordVector.keySet()){
			authorList.add(authorId);
			authorIndexMap.put(authorId, authIdx++);
		}
		
		int termIdx = 0;
		Map<String, Integer> allKeywordsPosMap = new HashMap<String, Integer>();
		for(String kw:completeKeywordList){
			allKeywordsPosMap.put(kw, termIdx++);
		}

		int numAuthors = authorKeywordVector.keySet().size();
		double[][] similarityMatrix = new double[numAuthors][numAuthors];
		
		for(int i=0; i<authorList.size(); i++){
			for(int j=0; j<authorList.size(); j++){
				String authorId1 = authorList.get(i);
				String authorId2 = authorList.get(j);
				
				double[] a1 = alignVectors(authorKeywordVector.get(authorId1), allKeywordsPosMap);
				double[] a2 = alignVectors(authorKeywordVector.get(authorId2), allKeywordsPosMap);
				double cosineSim = util.cosineSimilarity(a1, a2);
				similarityMatrix[authorIndexMap.get(authorId1)][authorIndexMap.get(authorId2)] = cosineSim;
			}
		}
		return similarityMatrix;
	}

	private double[] alignVectors(TermFreqVector authorTermFreqVector, Map<String, Integer> allKeywordsPosMap){
		double[] alignedVector = new double[allKeywordsPosMap.keySet().size()];
		String termTexts[] = authorTermFreqVector.getTerms();
		int termFreqs[] = authorTermFreqVector.getTermFrequencies();
		for(int i=0; i<termTexts.length; i++){
			if(!allKeywordsPosMap.containsKey(termTexts[i])){
				System.out.println(termTexts[i]);
			}
			int j = allKeywordsPosMap.get(termTexts[i]);
			if(j != -1){
				alignedVector[j] = termFreqs[i];
			}
		}
		return alignedVector;
	}
	
}
