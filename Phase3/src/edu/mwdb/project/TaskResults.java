package edu.mwdb.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TaskResults {
protected	ArrayList<Map.Entry<String, Double>> similarities;

protected	HashMap<String,Double> newTermFreqVector;

public HashMap<String, Double> getNewTermFreqVector() {
	return newTermFreqVector;
}

public void setNewTermFreqVector(HashMap<String, Double> newTermFreqVector) {
	this.newTermFreqVector = newTermFreqVector;
}

public ArrayList<Map.Entry<String, Double>> getSimilarities() {
	return similarities;
}

public void setSimilarities(ArrayList<Map.Entry<String, Double>> similarities) {
	this.similarities = similarities;
}

public TaskResults(ArrayList<Map.Entry<String, Double>> similarities, HashMap<String,Double> newTermFreqVector ){
	this.similarities = similarities;
	this.newTermFreqVector = newTermFreqVector;

}

}
