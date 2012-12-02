package edu.mwdb.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;

/**
 * Provides a grouping for the clusters that contain the item ids and also their similarity values with respect to the centroid 
 */
public class ClusterGroup {
	
	ArrayList<Map.Entry<String,Double>> cluster;
	SortedSet<Map.Entry<String,Double>> centroidsKeywords;
	ArrayList<String> addedLater;
	
	public ClusterGroup(ArrayList<Map.Entry<String,Double>> cluster, SortedSet<Map.Entry<String,Double>> centroidsKeywords) {
		this.cluster = cluster;
		this.centroidsKeywords = centroidsKeywords;
	}
	
	public ArrayList<Map.Entry<String,Double>> getCluster() {
		return cluster;
	}
	
	public SortedSet<Map.Entry<String,Double>> getClusterKeywords() {
		return centroidsKeywords;
	}
	
	public ArrayList<String> getaddedLater() {
		return addedLater;
	}
	
	public void setAddedLater(ArrayList<String> addedLater) {
		this.addedLater = addedLater; 
	}
}
