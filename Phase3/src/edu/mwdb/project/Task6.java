package edu.mwdb.project;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.extensions.MatlabNumericArray;
import matlabcontrol.extensions.MatlabTypeConverter;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

import edu.mwdb.project.DblpData;
import edu.mwdb.project.MatLab;
import edu.mwdb.project.Utility;


public class Task6 extends Task4 {

	public Task6() throws Exception {
		super();
	}
		
	/**
	 * Compute Papers's Similarity Ranking using Page Rank algorithm
	 * @param K is number of desired ranked pages
	 * @param Graph is weighted graph of paper similarities along with index mapping of graph/matrix
	 * @param seed is paper id that indicates where random jump from current page should land
	 * @param index directory to reference all pages of index and their terms
	 *  
	 * cFactor is Beta weighting that gives preference to known edges or random papers
	 * @return a map in which the keys represent the paperIds and their ranking score. 
	 * 
	 * Uses power iteration method to find the steady state value of the page ranking vector of probabilities
	 * @throws IOException 
	 * @throws CorruptIndexException 
	 * @throws MatlabConnectionException 
	 * @throws MatlabInvocationException 
	 * */
	public ArrayList<Map.Entry<String, Double>> computePersonalizedPageRank(Integer K, Graph simGraph, String seed, Directory luceneIndexDir) throws CorruptIndexException, IOException, MatlabInvocationException, MatlabConnectionException {
		double	resetFactor =  0.0;
		pageRankVector = new double[simGraph.getNumNodes()];
		double[][] simMatrixMStar = new double[pageRankVector.length][pageRankVector.length]; 
		
		simMatrixMStar = getMatrixMStar(resetFactor, simGraph.getAdjacencyMatrix());
		double[][] personalizedMStar = personalizeMatrix(simMatrixMStar, simGraph.getNodeIndexLabelMap() ,seed);
		double[] probRVector = computeSteadyStateVector(personalizedMStar);
		pageRankVector = Arrays.copyOf(probRVector, probRVector.length);
	
//		Arrays.sort(probRVector);
//		maxPageRankValue = probRVector[probRVector.length-1];	
		ArrayList<Map.Entry<String, Double>> retVal = new ArrayList<Map.Entry<String, Double>>();
		for (int ppages = 0; ppages < pageRankVector.length; ppages++ ){
		//	pageRankVector[ppages] /= maxPageRankValue;  // scale value from 0 to 1.0 
			retVal.add(new AbstractMap.SimpleEntry<String,Double>(simGraph.getNodeIndexLabelMap().get(ppages), pageRankVector[ppages]));
		}
		return retVal;
	}		// end compute Paper Page Rank	

	/**
	 * Modify transition matrix to reflect personalization 
	 * which in this case is a structural preference for the seed
	 * @param matrix is current transition matrix
	 * @param map is index to node label mapping of graph/matrix
	 * @param seed  indicates where random jump from current page should land
	 * @returns updated transition matrix
	 *  */
	private double[][] personalizeMatrix(double[][] matrix, Map<Integer, String> matrixMap, String seed) {
		double[][] modMatrix =  matrix;
		Integer seedIndex = getIndexfromValue(matrixMap, seed);
		for (int i= 0; i< modMatrix[0].length; i++){
			modMatrix[seedIndex][i] += 1.0000*(1-cFactor);
		}
		return modMatrix;
	}
	
	public Map<String,Integer> getFieldIndexMap(IndexReader reader, String fieldName) throws CorruptIndexException, IOException{
		Map<String,Integer> docIndexMap = new HashMap<String,Integer>();
		for (int i = 0; i < reader.maxDoc(); i++) {
			String fieldValue = reader.document(i).get(fieldName);
				docIndexMap.put(fieldValue , i);
			}
		return docIndexMap;
	}
	
	public Integer getIndexfromValue(Map<Integer, String> indexMap, String seed){
		
		int index = -1;
		for (int i = 0; i < indexMap.size(); i++) {
			String fieldValue = indexMap.get(i);
			if (fieldValue.equalsIgnoreCase(seed)){
				index = i;
				break;
			}
			//docIndexMap.put(fieldValue , i);
			}
		return index;
	}
	/*
	 * Testing helper method that produces graph for verifying paper page ranking
	 */
	/*
	private Graph getPaperSimilarityGraph(Directory luceneIndexDir) throws Exception{
		Directory indexDir = dblpData.createAllDocumentIndex();
		Task1cCompDocs Task1c = new Task1cCompDocs();
		Map<Integer, TermFreqVector> docTermFreq = Task1c.getDocTermFrequencies(indexDir);
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
		Map<Integer,String>	paperMap = getIndexPaperMap(luceneIndexDir);
		
		Map<String, Integer> allKeywordsPosMap = new HashMap<String, Integer>();
		int termIdx = 0;
		for(String kw:allTerms){
			allKeywordsPosMap.put(kw, termIdx++);
		}
		
		double[][] simMatrix  = new double[docTermFreq.size()][docTermFreq.size()];
		for(int i=0; i<docTermFreq.size(); i++){
			double[] a1 = Utility.getAlignedTermFreqVector(docTermFreq.get(i), allKeywordsPosMap);
			for(int j=0; j<docTermFreq.size(); j++){
				double[] a2 = Utility.getAlignedTermFreqVector(docTermFreq.get(j), allKeywordsPosMap);
				double cosineSim = utils.cosineSimilarity(a1, a2);
				simMatrix[i][j] = cosineSim;
			}
		}
		return new Graph(simMatrix, paperMap);
	}
	
	*/
	
	/**
	 * Compute coAuthor's Similarity Ranking using a Personalized Page Rank algorithm
	 * @param K is number of desired ranked pages
	 * @param Graph is weighted graph of paper similarities along with index mapping of graph/matrix
	 * @param seed is paper id that indicates where random jump from current page should land (personlization)
	 * @return a map in which the keys represent the authorIds and their ranking score. 
	 * Uses power iteration method to find the steady state value of the page ranking vector of probabilities
	 * */
	
	public ArrayList<Map.Entry<String, Double>> computePersonalizedAuthorPageRank(Integer K, Graph simGraph, String seed) {
		double	resetFactor =  0.0;
		pageRankVector = new double[simGraph.getNumNodes()];
		
		double[][] simMatrixMStar = getAuthorMatrixMStar(resetFactor, simGraph);
		double[][] personalizedMStar = personalizeMatrix(simMatrixMStar, simGraph.getNodeIndexLabelMap() ,seed);
		double[] probRVector = computePowerIterationVector(personalizedMStar);
		pageRankVector = Arrays.copyOf(probRVector, probRVector.length);
			
		//	Arrays.sort(cumulativeRank);		// sort in order to scale from 0 to 1
		//	maxPageRankValue = cumulativeRank[cumulativeRank.length-1];	
		ArrayList<Map.Entry<String, Double>> retVal = new ArrayList<Map.Entry<String, Double>>();
		for (int ppages = 0; ppages < pageRankVector.length; ppages++ ){
			//	pageRankVector[ppages] /= maxPageRankValue; 
			retVal.add(new AbstractMap.SimpleEntry<String,Double>(simGraph.getNodeLabel(ppages), pageRankVector[ppages]));
		}
		return retVal;
	}		// end compute Author Rank
	
	/**
	 * Compute coAuthor's Similarity Ranking  Personalized Page Rank algorithm
	 * @param cFactor is Beta weighting that gives preference to known edges or random authors
	 * @return a map in which the keys represent the authorIds and their ranking score. 
	 * 
	 * Uses Matlab eig function as method to find the steady state value of the page ranking vector of probabilities
	 * */
	public ArrayList<Map.Entry<String, Double>> computePersonalizedAuthorRank(Integer K, Graph simGraph, String seed) throws MatlabInvocationException, MatlabConnectionException {
		int			length = simGraph.getNumNodes(); 
		double[]	cumulativeRank = new double[length];
		double[][] 	simGraphMStar = new double[length][length];
		pageRankVector = new double[length];
		double	resetFactor =  0.0;
		
		simGraphMStar = getAuthorMatrixMStar(resetFactor, simGraph);
		double[][] matrixMStar = personalizeMatrix(simGraphMStar,simGraph.getNodeIndexLabelMap(),seed);
		double[] probRVector = computeSteadyStateVector(matrixMStar);
		pageRankVector = Arrays.copyOf(probRVector, probRVector.length);

		ArrayList<Map.Entry<String, Double>> retVal = new ArrayList<Map.Entry<String, Double>>();
		for (int ppages = 0; ppages < pageRankVector.length; ppages++ ){
			//pageRankVector[ppages] /= maxPageRankValue; 
			retVal.add(new AbstractMap.SimpleEntry<String,Double>(simGraph.getNodeLabel(ppages), probRVector[ppages]));
		}
		return retVal;
	}		// end compute Page Rank	

	
	public void doTask6a(Integer K, String seed, Graph simGraph){

		ArrayList<Map.Entry<String, Double>> authorRank = computePersonalizedAuthorPageRank(K, simGraph, seed);

		// Sort them in descending order
		Collections.sort(authorRank, new MapEntryComparable());

		HashMap<String, String> authNamePersonIdList = dblpData.getAuthNamePersonIdList();
		int maxDisplay = Math.min(K,authorRank.size());
		System.out.println("PERSONALIZED PAGE RANK");
		System.out.println();
		System.out.println("Dominant "+ maxDisplay + " Authors  With Preference for "+ authNamePersonIdList.get(seed));
		System.out.println();
		System.out.println("Author                Author ID     Ranking Score" );
		System.out.println("---------------------------------------------------");
		for (int j = 0; j < maxDisplay; j++) {
			System.out.printf("%-20s  %-12s  " ,authNamePersonIdList.get(authorRank.get(j).getKey()),authorRank.get(j).getKey());
			System.out.printf("%10.10f", authorRank.get(j).getValue()); 
			System.out.println();
		}

		try {
			ArrayList<Map.Entry<String, Double>> authorRank2 = computePersonalizedAuthorRank(K, simGraph, seed);

			// Sort them in descending order
			Collections.sort(authorRank2,  new MapEntryComparable());

			int maxDisplay2 = Math.min(K,authorRank2.size());
			System.out.println();
			System.out.println("Dominant "+ maxDisplay2 + " Authors With Preference for "+ authNamePersonIdList.get(seed));
			System.out.println();
			System.out.println("Author		      AuthorID      Ranking Value" );
			System.out.println("----------------------------------------------------");
			for (int j = 0; j < maxDisplay; j++) {
				String authorID = authorRank2.get(j).getKey();
				System.out.printf("%-20s  %-12s  %-10.10f",authNamePersonIdList.get(authorID), authorID, authorRank2.get(j).getValue());
				System.out.println();
			}
		} catch (MatlabInvocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MatlabConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void doTask6b(Integer K, String seed, Graph simGraph,Directory luceneIndexDir) throws CorruptIndexException, IOException, MatlabInvocationException, MatlabConnectionException{
		
		ArrayList<Map.Entry<String, Double>> paperRank = computePersonalizedPageRank(K, simGraph, seed,luceneIndexDir);

		// Sort them in descending order
		Collections.sort(paperRank, new MapEntryComparable());
		
//		Directory luceneIndexDir = dblpData.createAllDocumentIndex();
		IndexReader papersReader =  IndexReader.open(luceneIndexDir);
		Map<String,Integer>  paperIndexMap = getPaperMap(papersReader);		
		int maxDisplay = Math.min(K,paperRank.size());
		System.out.println("PERSONALIZED PAGE RANK");
		System.out.println();
		System.out.println("Dominant "+ maxDisplay + " Papers Giving Preference to  Paper:" + seed + " " +papersReader.document(paperIndexMap.get(seed)).get("title") );
		System.out.println();
		System.out.println(" Paper       Ranking Score     Title" );
		System.out.println("---------------------------------------------------------");
		for (int j = 0; j < maxDisplay; j++) {
			System.out.printf(" %-12s%-11.11f",paperRank.get(j).getKey(),paperRank.get(j).getValue());
			System.out.println("    "+ papersReader.document(paperIndexMap.get(paperRank.get(j).getKey())).get("title"));
		}
	}
	

	public static void main(String[] args)  {
		
		System.out.println("\nEnter Command (task args1, args2....)   ");
		Scanner sc = new Scanner(System.in);
		String command = sc.nextLine();
		String[] tokens = command.split(" ");

		String taskName = tokens[0];
		int k = Integer.parseInt(tokens[1]);
		String seed = tokens[2];
		try {
			Task6 task  = new Task6();
			task.setcFactor(0.8500);
			if (taskName.equalsIgnoreCase("task6a")){
				try{
					Task1 task1 = new Task1();
					Graph simGraph = task1.getCoauthorSimilarityGraph_KeywordVector();
					task.doTask6a(k,seed, simGraph);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (taskName.equalsIgnoreCase("task6b")){
				Directory luceneIndexDir = task.dblpData.createAllDocumentIndex();
				Task2 task2 = new Task2();
				Graph simGraph = task2.getCoauthorPapersSimilarityGraph_KeywordVector("TF");
				task.doTask6b(k,seed,simGraph, luceneIndexDir);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
        
        public static void runTask6(int k, String task, String seed, Graph g) throws Exception{
            Task6 t6 = new Task6();
            
            if(task.equalsIgnoreCase("Author")){
                t6.doTask6a(k,seed, g);
            }else if(task.equalsIgnoreCase("Paper")){
                Directory luceneIndexDir = t6.dblpData.createAllDocumentIndex();
                t6.doTask6b(k,seed, g, luceneIndexDir);
            }else{
                System.out.println("Incorrect Usage");
                System.exit(1);
            }
        }
        
        
	public double getcFactor() {
		return cFactor;
	}
	public void setcFactor(double cFactor) {
		this.cFactor = cFactor;
	}

}



