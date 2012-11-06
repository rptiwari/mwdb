package edu.mwdb.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task3b {

	public static void main(String[] args) throws Exception {
		DblpData dblpData = new DblpData();
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		Task2 task2 = new Task2();
		ArrayList<Map.Entry<String, Double>>[] authorGroups = Task3a.getGroupPartitions(task2.getTop3LatSemBySVD_AuthorAuthor());
		ArrayList<Map.Entry<String, Double>>[] coauthorGroups = Task3a.getGroupPartitions(task2.getTop3LatSemBySVD_CoAuthorCoAuthor());
		
		// Author-Author
		System.out.println("Author-Author");
		HashMap<String, Double> authorAssociatedVector = getAssociationKeywVectorToLatSem(authorTermFreq, allTerms, authorGroups);
		for (Map.Entry<String, Double> entry : authorAssociatedVector.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}
		
		Thread.sleep(5000);
		
		// Coauthor-Coauthor
		System.out.println("\n\n\n\n\n\nCoauthor-Coauthor");
		HashMap<String, Double> coauthorAssociatedVector = getAssociationKeywVectorToLatSem(authorTermFreq, allTerms, coauthorGroups);
		for (Map.Entry<String, Double> entry : coauthorAssociatedVector.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}
	}
	
	/**
	 * Get the weights from the 3 partitioned groups and multiplies those authors' weights to their respective 
	 * tf's in the authorTermFreq vector. This modified vector is then averaged in terms of keyword values into
	 * a single vector and returned.
	 * @param authorTermFreq - author term frequency vector from Lucene
	 * @param allTerms - a list of all terms available in all documents
	 * @param groups - the partitions obtained from task3a
	 * @return
	 */
	public static HashMap<String, Double> getAssociationKeywVectorToLatSem(Map<String, TermFreqVector> authorTermFreq, List<String> allTerms, ArrayList<Map.Entry<String, Double>>[] groups) {
		HashMap<String, HashMap<String,Double>> processedAuthorKeywVector = new HashMap<String, HashMap<String,Double>>(); 
		// For each author's keyword vector's tf, multiply it by the correspoinding weight from group and store it in 'processedAuthorKeywVector'
		for (int i=0; i<groups.length; i++) {
			for (int j=0; j<groups[i].size(); j++) {
				String authorId = groups[i].get(j).getKey();
				double weight = groups[i].get(j).getValue();
				
				// Store all author's keywords' frequencies modified by corresponding weight from group into new hashmap 'termFreqForAnAuthor'
				HashMap<String,Double> termFreqForAnAuthor = new HashMap<String,Double>();
				TermFreqVector tfVector = authorTermFreq.get(authorId);
				String[] authorTerms = tfVector.getTerms();
				int[] authorTF = tfVector.getTermFrequencies();				
				for (int k=0; k<authorTerms.length; k++) {
					termFreqForAnAuthor.put(authorTerms[k], authorTF[k]*weight);
				}
				
				processedAuthorKeywVector.put(authorId, termFreqForAnAuthor);
			}
		}
		
		// Average all the authors' tf vectors into a single one for each term
		HashMap<String, Double> retVal = new HashMap<String, Double>();
		int numAuthors = authorTermFreq.size();
		for (String term : allTerms) {
			double sum = 0;
			for (HashMap<String,Double> tfIdx : processedAuthorKeywVector.values()) {
				if (tfIdx.containsKey(term)) {
					sum += tfIdx.get(term);
				}
			}
			retVal.put(term, sum/numAuthors);
		}
		
		return retVal;
	}

}
