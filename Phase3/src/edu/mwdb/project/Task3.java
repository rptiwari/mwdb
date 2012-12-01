package edu.mwdb.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task3 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

	}

	public ClusterGroup GetClustersByKMeans(Graph graph, int k, boolean isGraphPaperIds) throws Exception {
		ArrayList<HashMap<String,Double>> centroids = new ArrayList<HashMap<String,Double>>();
		ArrayList<String>[] clusters = new ArrayList[k];
		ArrayList<String>[] oldClusters = new ArrayList[k];
		ArrayList<Double>[] clustersSimValues = new ArrayList[k];
		double[][] matrix = graph.getAdjacencyMatrix();
		
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
				if (clusters[maxSimilarityIdx] == null)
					clusters[maxSimilarityIdx] = new ArrayList<String>();
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
		Map<String, TermFreqVector> authorTermFreq = dblp.getAuthorTermFrequencies(indexDir);
		List<String> allTerms = dblp.getAllTermsInIndex(indexDir, "doc");
		HashMap<Integer,HashMap> forwIdx;
		
		while(true) {
		
			// Compute new centroids
			if (isGraphPaperIds) {
				forwIdx = dblp.getForwardAndInversePaperKeywIndex()[0];
				
				for (int i=0; i<clusters.length; i++) {
					centroids.add(getAverageInCluster(clusters[i], allTerms, forwIdx));
				}
			} else {
				throw new Exception("Not Implemented Yet");
			}
			
			oldClusters = clusters;
			
			// Assign the clusters
			clusters = new ArrayList[k];
			clustersSimValues = new ArrayList[k];
			Collection<String> items = graph.getNodeIndexLabelMap().values();
			Utility util = new Utility();
			for (String itm : items) {
				int maxSimilarityIdx = 0;
				double maxSimilarity = 0;
				
				// Find the centroid with max similarity to item
				for (int centIdx=0; centIdx<centroids.size(); centIdx++) {
					double similarity = util.cosineSimilarity(centroids.get(centIdx), forwIdx.get(itm), allTerms);
					if (maxSimilarity < similarity) {
						maxSimilarity = similarity;
						maxSimilarityIdx = centIdx;
					}
				}
				
				if (clusters[maxSimilarityIdx] == null) {
					clusters[maxSimilarityIdx] = new ArrayList<String>();
					clustersSimValues[maxSimilarityIdx] = new ArrayList<Double>();
				}
				clusters[maxSimilarityIdx].add(itm);
				clustersSimValues[maxSimilarityIdx].add(maxSimilarity);
			}
			
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
		
		//TODO instead of clustergroup, change it to map.entry since it may need to be sorted
		
		return new ClusterGroup(clusters, clustersSimValues);
	}
	
	/**
	 * Generates k unique random numbers
	 * @param k number of random numbers wanted
	 * @param maxRandomNumber the maximum value of a random number, [0,maxRandomNumber), it's exclusive
	 * @return an arraylist of size k with random numbers
	 */
	private ArrayList<Integer> getRandomNumbers(int k, int maxRandomNumber) {
		Random randomGen = new Random();
		ArrayList<Integer> randomNumbers = new ArrayList<Integer>();
		
		for (int i=0; i<k; i++) {
			int randTemp = randomGen.nextInt(maxRandomNumber);
			if (!randomNumbers.contains(randTemp))
				randomNumbers.add(randTemp);
			else
				i--;
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
	private HashMap<String,Double> getAverageInCluster(ArrayList<String> cluster, List<String> allTerms, HashMap<Integer,HashMap> forwIdx) {
		HashMap<String,Double> retVal = new HashMap<String,Double>();
		
		// Compute average for each term and store it in hashmap of retVal
		for (int termsIdx=0; termsIdx < allTerms.size(); termsIdx++) {
			double termAverage = 0;
			String currentTerm = allTerms.get(termsIdx);
			
			for (String label : cluster) {
				termAverage += (Double) (forwIdx.get(Integer.parseInt(label)).get(currentTerm));
			}
			termAverage /= cluster.size();
			
			retVal.put(currentTerm, termAverage);
		}
		
		return retVal;
}
}
