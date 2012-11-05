package edu.mwdb.project;

import java.util.List;
import java.util.Map;

import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task2 {

	public static void main(String[] args) {

		DblpData dblpData = new DblpData();
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
		SimilarityAnalyzer sa = new SimilarityAnalyzer();
		try {
			double[][] simMatrix = sa.getAuthorSimilarityMatrix(authorTermFreq, allTerms);
			for(int i=0; i < simMatrix[0].length; i++){
				for(int j=0; j < simMatrix[0].length; j++){
					System.out.print(simMatrix[i][j]);
					System.out.print(" ");
				}
				System.out.println();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

}
