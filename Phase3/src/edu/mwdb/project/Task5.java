package edu.mwdb.project;

import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.index.TermFreqVector;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;

/**
 * Task 5: Graph search task (content): Given a �co-author� or �co-authored papers� graph and an author 
 * or paperid, identify and list K most content-wise similar nodes (for a user supplied K).
 */
public class Task5 {

	public static void main(String[] args) throws MatlabInvocationException, MatlabConnectionException, Exception {
		DblpData dblp = new DblpData();
		
		// Task1
		Task1 t1 = new Task1();
		
		Graph g = t1.getCoauthorSimilarityGraph_KeywordVector();
		System.out.println("\nTop 10 coauthors most content-wise similar nodes with: 1636579 (" + dblp.getAuthName("1636579") + ")");
		Map.Entry<String, Double>[] results = GraphSearchContent(g, "1636579", 10);
		for (Map.Entry<String, Double> r : results) {
			System.out.println(dblp.getAuthName(r.getKey()) + " : " + r.getValue());
		}
		
		// Task2
		Task2 t2 = new Task2();
		
		g = t2.getCoauthorPapersSimilarityGraph_KeywordVector("TF-IDF");
		System.out.println("\nTop 10 coauthored papers most content-wise similar nodes with: 674403 (" + dblp.getPaperTitle("674403") + ")");
		results = GraphSearchContent(g, "674403", 10);
		for (Map.Entry<String, Double> r : results) {
			System.out.println(dblp.getPaperTitle(r.getKey()) + " :\t" + r.getValue());
		}
	}
        
        public static void runTask5(int k, String searchID, Graph g) throws MatlabInvocationException, MatlabConnectionException, Exception {
            DblpData dblp = new DblpData();

            System.out.println("\nPrinting Results for "+searchID+"\n");
            DecimalFormat f = new DecimalFormat("#.####");
            
            Map.Entry<String, Double>[] results = GraphSearchContent(g, searchID, k);
            for (Map.Entry<String, Double> r : results) {
                System.out.format("%-30s%10s\n", dblp.getAuthName(r.getKey()), 
                    f.format(r.getValue()));
                
            }    
        }
        
        
	
	/**
	 * Given a graph, return the topK most content-wise similar entries from the graph to the searchId
	 * @param graph �co-author� or �co-authored papers� matrix
	 * @param searchId author or paper Id to find similarities for
	 * @param topK number of results to return 
	 * @return the most content-wise similar entries
	 * @throws MatlabInvocationException
	 * @throws MatlabConnectionException
	 * @throws Exception
	 */
	public static Map.Entry<String, Double>[] GraphSearchContent(Graph graph, String searchId, int topK) throws MatlabInvocationException, MatlabConnectionException, Exception {
		double[][] matrix = graph.getAdjacencyMatrix();
		
		// Locate the index row that the entry searchId is
		Map<String,Integer> revIndex = graph.getReversedNodeIndexLabelMap();
		int index = revIndex.get(searchId);

		// Create a list of all the nonzero entries from the searchId's row with the labels attached
		ArrayList<Map.Entry<String, Double>> nonZeroFields = new ArrayList<Map.Entry<String, Double>>();
		for (int i=0; i<matrix[index].length; i++) {
			Map.Entry<String,Double> nonZero = new AbstractMap.SimpleEntry<String,Double>(graph.getNodeLabel(i), matrix[index][i]);
			nonZeroFields.add(nonZero);
		}
		
		// Sort them in descending order
		Collections.sort(nonZeroFields, new MapEntryComparable());
		
		// Return only topK
		int realTopK = Math.min(topK, nonZeroFields.size());
		Map.Entry<String, Double>[] retVal = new Map.Entry[realTopK];
		for (int i=0; i<realTopK; i++) {
			
			// If the searchId is in the topK, remove it
			if (nonZeroFields.get(i).getKey().equalsIgnoreCase(searchId))
				nonZeroFields.remove(i);
			
			retVal[i] = nonZeroFields.get(i);
		}
		
		return retVal;
	}

}

class MapEntryComparable implements Comparator<Map.Entry<String, Double>> {
	@Override
	public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
		return -1*Double.compare(arg0.getValue(), arg1.getValue());
	}
}