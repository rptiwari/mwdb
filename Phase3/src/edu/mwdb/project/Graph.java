package edu.mwdb.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Graph {

	private double[][] adjacencyMatrix;
	private Map<Integer, String> nodeIndexLabelMap;
	
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
	}

	public void setAdjacencyMatrix(double[][] adjacencyMatrix) {
		this.adjacencyMatrix = adjacencyMatrix;
	}

	public double[][] getAdjacencyMatrix() {
		return adjacencyMatrix;
	}

	public void setNodeIndexLabelMap(Map<Integer, String> nodeIndexLabelMap) {
		this.nodeIndexLabelMap = nodeIndexLabelMap;
	}

	public Map<Integer, String> getNodeIndexLabelMap() {
		return nodeIndexLabelMap;
	}
	
}
