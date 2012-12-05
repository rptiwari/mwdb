package edu.mwdb.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.TermFreqVector;

public class TaskResults {
protected	ArrayList<Map.Entry<String, Double>> similarities;

protected	HashMap<String,Double> newQueryTermFreqVector;
protected 	TermFreqVector 			oldQuery;

public TermFreqVector getOldQuery() {
	return oldQuery;
}

public void setOldQuery(TermFreqVector oldQuery) {
	this.oldQuery = oldQuery;
}

public HashMap<String, Double> getNewTermFreqVector() {
	return newQueryTermFreqVector;
}

public void setNewTermFreqVector(HashMap<String, Double> newTermFreqVector) {
	this.newQueryTermFreqVector = newTermFreqVector;
}

public ArrayList<Map.Entry<String, Double>> getSimilarities() {
	return similarities;
}

public void setSimilarities(ArrayList<Map.Entry<String, Double>> similarities) {
	this.similarities = similarities;
}

public TaskResults(ArrayList<Map.Entry<String, Double>> similarities, HashMap<String,Double> newTermFreqVector, TermFreqVector oldQuery ){
	this.similarities = similarities;
	this.newQueryTermFreqVector = newTermFreqVector;
	this.oldQuery = oldQuery;

}

}
