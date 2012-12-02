package edu.mwdb.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {

	private double[][] adjacencyMatrix;
	private Map<Integer, String> nodeIndexLabelMap;
	private Map<String, Integer> reversedNodeIndexLabelMap;
	
	public String getNodeLabel(int index)
	{
		return nodeIndexLabelMap.get(index);
	}
	
	public int getNumNodes()
	{
		return adjacencyMatrix[0].length;
	}
	
	public List<Integer> getNeighbours(int nodeIndex)
	{
		List<Integer> neighbours = new ArrayList<Integer>();
		for(int i=0; i<adjacencyMatrix[nodeIndex].length; i++)
		{
			if(i != nodeIndex && adjacencyMatrix[nodeIndex][i] > 0)
			{
				neighbours.add(i);
			}
		}
		return neighbours;
	}
	
	public Graph(double[][] adjacencyMatrix, Map<Integer, String> nodeIndexLabelMap){
		this.setAdjacencyMatrix(adjacencyMatrix);
		this.setNodeIndexLabelMap(nodeIndexLabelMap);
		buildReversedNodeIndexLabelMap();
	}

	public void setAdjacencyMatrix(double[][] adjacencyMatrix) {
		this.adjacencyMatrix = adjacencyMatrix;
	}

	public double[][] getAdjacencyMatrix() {
		return adjacencyMatrix;
	}

	public void setNodeIndexLabelMap(Map<Integer, String> nodeIndexLabelMap) {
		this.nodeIndexLabelMap = nodeIndexLabelMap;
		buildReversedNodeIndexLabelMap();
	}

	public Map<Integer, String> getNodeIndexLabelMap() {
		return nodeIndexLabelMap;
	}
	
	/**
	 * Gets a hashmap with the item id as a key and the index location as the value
	 * @return a hashmap
	 */
	public Map<String, Integer> getReversedNodeIndexLabelMap() {
		return reversedNodeIndexLabelMap;
	}
	
	private void buildReversedNodeIndexLabelMap() {
		reversedNodeIndexLabelMap = new HashMap<String, Integer>();
		for (Map.Entry<Integer,String> e : nodeIndexLabelMap.entrySet()) {
			reversedNodeIndexLabelMap.put(e.getValue(), e.getKey());
		}
	}
}
