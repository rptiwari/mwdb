package edu.mwdb.project;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task3b {

	/**
	 * Get the weights from the 3 partitioned groups and multiplies those authors' weights to their respective 
	 * tf's in the authorTermFreq vector. This modified vector is then averaged in terms of keyword values into
	 * a single vector and returned.
	 * @param authorTermFreq - author term frequency vector from Lucene
	 * @param allTerms - a list of all terms available in all documents
	 * @param groups - the partitions obtained from task3a
	 * @return
	 */
	public static SortedSet<Map.Entry<String, Double>> getAssociationKeywVectorToLatSem(Map<String, TermFreqVector> authorTermFreq, List<String> allTerms, ArrayList<Map.Entry<String, Double>>[] groups) {
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

		return entriesSortedByValues(retVal);
	}
	
	static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
            new Comparator<Map.Entry<K,V>>() {
                @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                    int res = e2.getValue().compareTo(e1.getValue());
                    return res != 0 ? res : 1; // Special fix to preserve items with equal values
                }
            }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
}
