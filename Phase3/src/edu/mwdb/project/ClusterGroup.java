package edu.mwdb.project;

import java.util.ArrayList;

/**
 * Provides a grouping for the clusters that contain the item ids and also their similarity values with respect to the centroid 
 */
public class ClusterGroup {
	private ArrayList<Double>[] clustersSimValues;
	private ArrayList<String>[] clusters;
	
	public ClusterGroup(ArrayList<String>[] clust, ArrayList<Double>[] clustSimValues) {
		clustersSimValues = clustSimValues;
		clusters = clust;
	}
	
	public ArrayList<String>[] getClusters() {
		return clusters;
	}
	
	public ArrayList<Double>[] getClustersSimilarities() {
		return clustersSimValues;
	}
}
