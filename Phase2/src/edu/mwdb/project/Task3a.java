package edu.mwdb.project;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;

public class Task3a {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Task2 task2;
		try {
			task2 = new Task2();
			Map.Entry<String, Double>[][] top3latentAuthor = task2.getTop3LatSemBySVD_AuthorAuthor();
			ArrayList<Map.Entry<String, Double>>[] groups = partitionIntoGroups(top3latentAuthor);
			
			for (int i=0; i<groups.length; i++) {
				System.out.println("GROUP" + (i+1));
				for (int j=0; j<groups[i].size(); j++) {
					System.out.println(groups[i].get(j).getKey() + " : " + groups[i].get(j).getValue());
				}
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Partitions the latent semantics into groups depending on where the weight is the highest in the semantics
	 * @param latentSemantics - the ones obtained from task2
	 * @return
	 */
	public static ArrayList<Map.Entry<String, Double>>[] partitionIntoGroups(Map.Entry<String, Double>[][] latentSemantics) {
		ArrayList<Map.Entry<String, Double>>[] retVal = new ArrayList[latentSemantics.length];
		for (int i=0; i<latentSemantics[0].length; i++) {
			double max = Double.MIN_VALUE;
			int maxIndex = 0;

			// Find the max value for that column/key/authorId
			for (int j=0; j<latentSemantics.length; j++) {
				if (latentSemantics[j][i].getValue() > max) {
					max = latentSemantics[j][i].getValue();
					maxIndex = j;
				}
			}
			
			// Store the max
			if (retVal[maxIndex] == null)
				retVal[maxIndex] = new ArrayList<Map.Entry<String, Double>>();
			retVal[maxIndex].add(new AbstractMap.SimpleEntry<String,Double>(latentSemantics[0][i].getKey(), max));
		}
		
		return retVal;
	}
}
