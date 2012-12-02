package edu.mwdb.project;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task3 {

	public static void main(String[] args) throws Exception {

		DblpData dblp = new DblpData();
		HashMap<Integer,HashMap<String,Double>> forwIdx;
		Task1 t1 = new Task1();
		Task3 t3 = new Task3();
		
		// Coauthors
		
		Directory indexDir = dblp.createAuthorDocumentIndex();
		Map<String,TermFreqVector> authKeywVectors = dblp.getAuthorTermFrequencies(indexDir);
		Utility util = new Utility();
		forwIdx = util.getForwardAllAuthorsKeywIndex(authKeywVectors);
		
		ClusterGroup[] results = t3.GetClustersByKMeans(t1.getCoauthorSimilarityGraph_KeywordVector(), 5, forwIdx);
		
		for (ClusterGroup cg : results) {
			ArrayList<Map.Entry<String, Double>> cl = cg.getCluster();
			SortedSet<Map.Entry<String, Double>> clKeyw = cg.getClusterKeywords();
			
			System.out.println("##CLUSTER##");
			int five = 0;
			for (Map.Entry<String,Double> e : clKeyw) {
				System.out.println(e.getKey() + "\t" + e.getValue());
				if (five == 5) 
					break;
				five++;
			}
			
			for (int i=0; i<cl.size(); i++) {
				Map.Entry<String,Double> e = cl.get(i);
				System.out.println(dblp.getAuthName(e.getKey()) + "\t" + e.getValue());
			}
		}
		
		// Papers
		forwIdx = dblp.getForwardAndInversePaperKeywIndex()[0];
	}

	public ClusterGroup[] GetClustersByKMeans(Graph graph, int k, HashMap<Integer,HashMap<String,Double>> forwIdx) throws Exception {
		ArrayList<HashMap<String,Double>> centroids = new ArrayList<HashMap<String,Double>>(k);
		ArrayList<String>[] clusters = new ArrayList[k];
		ArrayList<String>[] oldClusters = new ArrayList[k];
		ArrayList<Double>[] clustersSimValues = new ArrayList[k];
		double[][] matrix = graph.getAdjacencyMatrix();
		Utility util = new Utility();
		
		// Obtain random centroids
		ArrayList<Integer> randomNumbers = getRandomNumbers(k, matrix.length);
		
		// Use the adjacency matrix to check for similarity for the initial time
		for (int i=0; i<matrix.length; i++) {
			int maxSimilarityIdx = 0;
			double maxSimilarity = 0;
			String label = graph.getNodeLabel(i);
			
			// If encountered a centroid, add it to its own cluster
			int randNumIdx = randomNumbers.indexOf(i);
			if (randNumIdx != -1) {
				if (clusters[randNumIdx] == null)
					clusters[randNumIdx] = new ArrayList<String>();
				clusters[randNumIdx].add(label);
				continue;
			}
			
			// Find the centroid with max similarity to row
			for (int randomIdx=0; randomIdx<k; randomIdx++) {
				if (maxSimilarity < matrix[i][randomIdx]) {
					maxSimilarity = matrix[i][randomIdx];
					maxSimilarityIdx = randomIdx;
				}
			}
			
			if (clusters[maxSimilarityIdx] == null)
				clusters[maxSimilarityIdx] = new ArrayList<String>();
			clusters[maxSimilarityIdx].add(label);
		}

		// Get all terms to iterate through
		DblpData dblp = new DblpData();
		Directory indexDir = dblp.createAuthorDocumentIndex();
		List<String> allTerms = dblp.getAllTermsInIndex(indexDir, "doc");
		
		// Loop until convergence
		while(true) {
			
			// Compute new centroids
			centroids = new ArrayList<HashMap<String,Double>>(k);
			for (int i=0; i<clusters.length; i++) {
				centroids.add(getAverageInCluster(clusters[i], allTerms, forwIdx));
			}
			
			oldClusters = clusters;
			
			// Assign the clusters
			Task3ClusterKeyValue clusterResults = getClustersByCentroids(k, graph.getNodeIndexLabelMap().values(), centroids, forwIdx, allTerms, graph, null);
			clusters = clusterResults.getClusters();
			clustersSimValues = clusterResults.getClustersSimValues();
			
			// Check for convergence
			boolean converged = true;
			outerloop:
			for (int i=0; i<clusters.length; i++) {
				for (String item : clusters[i]) {
					if (!oldClusters[i].contains(item)) {
						converged = false;
						break outerloop;
					}
				}
			}
			
			if (converged)
				break;
		}
		
		// Use the adjacency matrix to relocate entries if they don't have any edges(adj.matrix) with any of the cluster's entries
		Map<String,Integer> revNodeMap = graph.getReversedNodeIndexLabelMap();
		Collection<String> removed = new ArrayList<String>();
	    for (ArrayList<String> cl : clusters) {
	    	for (Iterator<String> iter = cl.iterator(); iter.hasNext();) {
	    		String s = iter.next();
	    		int index = revNodeMap.get(s);
	    		List<Integer> neighbors = graph.getNeighbours(index);
	    		ArrayList<String> remappedNeighbors = remap(neighbors, graph.getNodeIndexLabelMap());
	    		if (!isAnEntryMatching(cl, remappedNeighbors)) {
	    			removed.add(s);
	    			System.out.println("removed:" + s);
	    			iter.remove();
	    		}
	    	}
	    }
	    
	    // Find out where they fit better and relocate the removed entries
	    Task3ClusterKeyValue removedEntries = getClustersByCentroids(k, removed, centroids, forwIdx, allTerms, graph, clusters);
		ArrayList<String>[] removedClusters = removedEntries.getClusters();
		ArrayList<Double>[] removedClustersSimValues = removedEntries.getClustersSimValues();
		for (int rmClustIdx=0; rmClustIdx<removedClusters.length; rmClustIdx++) {
			if (removedClusters[rmClustIdx] != null) {
				clusters[rmClustIdx].addAll(removedClusters[rmClustIdx]);
				clustersSimValues[rmClustIdx].addAll(removedClustersSimValues[rmClustIdx]);
			}
		}
	    
		// Convert clusters to Map.Entry
		ArrayList<Map.Entry<String,Double>>[] clusterEntries = new ArrayList[k];
		for (int clIdx=0; clIdx<clusters.length; clIdx++) {
			ArrayList<String> cluster = clusters[clIdx];
			ArrayList<Double> clusterSim = clustersSimValues[clIdx];
			
			clusterEntries[clIdx] = new ArrayList<Map.Entry<String,Double>>(cluster.size());
			for (int i=0; i<cluster.size(); i++) {
				Map.Entry<String,Double> entry = new AbstractMap.SimpleEntry<String,Double>(cluster.get(i), clusterSim.get(i));
				clusterEntries[clIdx].add(entry);
			}
		}
		
		// Find the keyword that's most related to each centroid, by sorting the centroids
		ArrayList<SortedSet<Map.Entry<String,Double>>> sortedCentroids = new ArrayList(centroids.size());
		for (HashMap<String,Double> ct : centroids) {
			SortedSet<Map.Entry<String,Double>> sortedCentroid = entriesSortedByValues(ct);
			sortedCentroids.add(sortedCentroid);
		}
		
		// Sort them in descending order of similarity to the centroid
		ClusterGroup[] retVal = new ClusterGroup[k];
		for (int i=0; i<clusterEntries.length; i++) {
			Collections.sort(clusterEntries[i], new Task3MapEntryComparable());
			retVal[i] = new ClusterGroup(clusterEntries[i], sortedCentroids.get(i));
			retVal[i].setAddedLater(removedClusters[i]);
		}
		
		return retVal;
	}
	
	/**
	 * Generates k unique random numbers
	 * @param k number of random numbers wanted
	 * @param maxRandomNumber the maximum value of a random number, [0,maxRandomNumber), it's exclusive
	 * @return an arraylist of size k with random numbers
	 */
	private ArrayList<Integer> getRandomNumbers(int k, int maxRandomNumber) {
		Random randomGen = new Random();
		ArrayList<Integer> randomNumbers = new ArrayList<Integer>(k);
		
		for (int i=0; i<k; i++) {
			int randTemp = randomGen.nextInt(maxRandomNumber);
			while (randomNumbers.contains(randTemp)) //unique number
				randTemp = randomGen.nextInt(maxRandomNumber);
			randomNumbers.add(randTemp);
		}
		System.out.println("Random Numbers:");
		for (int i=0; i<randomNumbers.size(); i++)
			System.out.println(randomNumbers.get(i));
		
		return randomNumbers;
	}
	
	/**
	 * Get average keyword vector in a cluster
	 * @param cluster contains a list of all itemIds
	 * @param allTerms reference list to iterate through all keywords
	 * @param forwIdx from a keyword you get its tf
	 * @return HashMap<String,Double>, String=Keyword, Double=Value
	 */
	private HashMap<String,Double> getAverageInCluster(ArrayList<String> cluster, List<String> allTerms, HashMap<Integer,HashMap<String,Double>> forwIdx) {
		HashMap<String,Double> retVal = new HashMap<String,Double>();
		
		// Compute average for each term and store it in hashmap of retVal
		for (int termsIdx=0; termsIdx < allTerms.size(); termsIdx++) {
			double termAverage = 0;
			String currentTerm = allTerms.get(termsIdx);
			
			for (String label : cluster) {
				HashMap<String,Double> keywToFreq = forwIdx.get(Integer.parseInt(label));
				if (keywToFreq.containsKey(currentTerm))
					termAverage += (Double) (keywToFreq.get(currentTerm));
			}
			termAverage /= cluster.size();
			
			retVal.put(currentTerm, termAverage);
		}
		
		return retVal;
	}

	private Task3ClusterKeyValue getClustersByCentroids (int k, Collection<String> items, ArrayList<HashMap<String,Double>> centroids, HashMap<Integer,HashMap<String,Double>> forwIdx, List<String> allTerms, Graph graph, ArrayList<String>[] optionalCurrentClusters) throws NumberFormatException, Exception {
		ArrayList<String>[] clusters = new ArrayList[k];
		ArrayList<Double>[] clustersSimValues = new ArrayList[k];
		Utility util = new Utility();
		Map<String,Integer> revNodeMap = graph.getReversedNodeIndexLabelMap();
		
		for (String itm : items) {
			
			// Calculate the similarities with all centroids
			ArrayList<Map.Entry<Integer, Double>> similarities = new ArrayList<Map.Entry<Integer, Double>>();
			for (int centIdx=0; centIdx<centroids.size(); centIdx++) {
				double similarity = util.cosineSimilarity(centroids.get(centIdx), forwIdx.get(Integer.parseInt(itm)), allTerms);
				similarities.add(new AbstractMap.SimpleEntry(centIdx, similarity));
			}
			
			// Sort the similarities in descending order
			Collections.sort(similarities, new Task3MapEntryComparable_Integer());
			
			int maxSimilarityIdx = 0;
			double maxSimilarity = 0;
			if (optionalCurrentClusters == null) {
				// Get the highest similarity value & index
				maxSimilarityIdx = similarities.get(0).getKey();
				maxSimilarity = similarities.get(0).getValue();
			} else {
				// Find which cluster the item fits best based on similarity and neighbors
				int adjMatrixIndex = revNodeMap.get(itm);
				List<Integer> neighbors = graph.getNeighbours(adjMatrixIndex);
				ArrayList<String> remappedNeighbors = remap(neighbors, graph.getNodeIndexLabelMap());
	    		for (int i=0; i<similarities.size(); i++) {
					if (isAnEntryMatching(optionalCurrentClusters[similarities.get(i).getKey()], remappedNeighbors)) {
						maxSimilarityIdx = similarities.get(i).getKey();
						maxSimilarity = similarities.get(i).getValue();
					}
	    		}
			}
			
			if (clusters[maxSimilarityIdx] == null) {
				clusters[maxSimilarityIdx] = new ArrayList<String>();
				clustersSimValues[maxSimilarityIdx] = new ArrayList<Double>();
			}
			clusters[maxSimilarityIdx].add(itm);
			clustersSimValues[maxSimilarityIdx].add(maxSimilarity);
		}
		
		return new Task3ClusterKeyValue(clusters, clustersSimValues);
	}
	
	/**
	 * Checks if there is any entry that exists in both lists
	 * @param cluster
	 * @param neighbors
	 * @return ture if there is a match in both lists
	 */
	private boolean isAnEntryMatching (ArrayList<String> cluster, ArrayList<String> neighbors) {
		for (String cl : cluster) {
			if (neighbors.contains(cl))
				return true;
		}
		return false;
	}
	
	/**
	 * Remaps values from a list to new values using a map
	 * @param items items to be remapped
	 * @param map reference to be used for the remapping
	 * @return
	 */
	private ArrayList<String> remap (List<Integer> items, Map<Integer,String> map) {
		ArrayList<String> retVal = new ArrayList<String>(items.size());
		for (int item : items) {
			retVal.add(map.get(item));
		}
		return retVal;
	}
	
	/**
	 * Sort a hashmap values in descending order
	 * @param map
	 * @return
	 */
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

class Task3MapEntryComparable implements Comparator<Map.Entry<String, Double>> {
	@Override
	public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
		return -1*Double.compare(arg0.getValue(), arg1.getValue());
	}
}

class Task3MapEntryComparable_Integer implements Comparator<Map.Entry<Integer, Double>> {
	@Override
	public int compare(Entry<Integer, Double> arg0, Entry<Integer, Double> arg1) {
		return -1*Double.compare(arg0.getValue(), arg1.getValue());
	}
}

/**
 * Class used to return data from getClustersByCentroids method
 */
class Task3ClusterKeyValue {
	private ArrayList<String>[] clusters;
	private ArrayList<Double>[] clustersSimValues;
	
	public Task3ClusterKeyValue(ArrayList<String>[] clusters, ArrayList<Double>[] clustersSimValues) {
		this.clusters = clusters;
		this.clustersSimValues = clustersSimValues;
	}
	
	public ArrayList<String>[] getClusters() {
		return clusters;
	}

	public ArrayList<Double>[] getClustersSimValues() {
		return clustersSimValues;
	}
}