package edu.mwdb.project;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Task3a {

	public static void main(String[] args) {
		try {
			Task2 task2 = new Task2();
			DblpData dblp = new DblpData();
			HashMap<String,String> authIdToName = dblp.getAuthNamePersonIdList();
			ArrayList<Map.Entry<String, Double>>[] authorGroups = getGroupPartitions(task2.getTop3LatSemBySVD_AuthorAuthor());
			
			for (int i=0; i<authorGroups.length; i++) {
				System.out.println("AUTHOR GROUP" + (i+1));
				for (int j=0; j<authorGroups[i].size(); j++) {
					System.out.println(authIdToName.get(authorGroups[i].get(j).getKey().toString()) + " : " + authorGroups[i].get(j).getValue());
				}
				System.out.println();
			}
			
			ArrayList<Map.Entry<String, Double>>[] coauthorGroups = getGroupPartitions(task2.getTop3LatSemBySVD_CoAuthorCoAuthor());
			
			for (int i=0; i<coauthorGroups.length; i++) {
				System.out.println("COAUTHOR GROUP" + (i+1));
				for (int j=0; j<coauthorGroups[i].size(); j++) {
					System.out.println(authIdToName.get(coauthorGroups[i].get(j).getKey().toString()) + " : " + coauthorGroups[i].get(j).getValue());
				}
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Partitions the latent semantics into groups depending on where the weight is the highest in the semantics
	 * @param latentSemanticsAuthorOrCoauthor - the ones obtained from task2 (author/coauthor)
	 * @return
	 */
	public static ArrayList<Map.Entry<String, Double>>[] getGroupPartitions(Map.Entry<String, Double>[][] latentSemanticsAuthorOrCoauthor) {
		ArrayList<Map.Entry<String, Double>>[] retVal = new ArrayList[latentSemanticsAuthorOrCoauthor.length];
		for (int i=0; i<latentSemanticsAuthorOrCoauthor[0].length; i++) {
			double max = Double.NEGATIVE_INFINITY;
			int maxIndex = 0;

			// Find the max value for that column/key/authorId
			for (int j=0; j<latentSemanticsAuthorOrCoauthor.length; j++) {
				if (latentSemanticsAuthorOrCoauthor[j][i].getValue() > max) {
					max = latentSemanticsAuthorOrCoauthor[j][i].getValue();
					maxIndex = j;
				}
			}
			
			// Store the max
			if (retVal[maxIndex] == null)
				retVal[maxIndex] = new ArrayList<Map.Entry<String, Double>>();
			retVal[maxIndex].add(new AbstractMap.SimpleEntry<String,Double>(latentSemanticsAuthorOrCoauthor[0][i].getKey(), max));
		}
		
		// Sort them in descending order
		for (int i=0; i<retVal.length; i++)
			Collections.sort(retVal[i], new MapEntryComparable());
		
		return retVal;
	}
}

class MapEntryComparable implements Comparator<Map.Entry<String, Double>> {
	@Override
	public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
		if (arg0.getValue() > arg1.getValue())
			return -1;
		if (arg0.getValue() > arg1.getValue())
			return 0;
		return 1;
	}
}