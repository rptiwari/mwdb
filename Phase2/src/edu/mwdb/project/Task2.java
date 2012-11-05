package edu.mwdb.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;

import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task2 {

	public static void main(String[] args) throws Exception {
		DblpData dblpData = new DblpData();
		MatlabProxyFactory factory = new MatlabProxyFactory();
		MatlabProxy proxy = factory.getProxy();
		MatLab matlab = new MatLab(proxy);
		Utility utils = new Utility();
		
		// 2a
		
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
		SimilarityAnalyzer sa = new SimilarityAnalyzer();
		double[][] simMatrix = null;
		try {
			simMatrix = sa.getAuthorSimilarityMatrix(authorTermFreq, allTerms);
			for(int i=0; i < simMatrix[0].length; i++){
				for(int j=0; j < simMatrix[0].length; j++){
					System.out.print(simMatrix[i][j]);
					System.out.print(" ");
				}
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("\n\n\n---- SVD -----");
		double[][] svdMatrix = matlab.svd(simMatrix, 3);
		
		String[] authorNames = authorTermFreq.keySet().toArray(new String[0]);
		for(int i=0; i<svdMatrix.length; i++) {
			for (int j=0; j<svdMatrix[i].length; j++) {
				System.out.print(authorNames[j] + ":" + svdMatrix[i][j] + "\t");
			}
			System.out.println();
		}
	}
}
