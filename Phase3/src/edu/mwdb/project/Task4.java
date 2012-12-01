package edu.mwdb.project;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

public class Task4 {

	DblpData dblpData;
	Utility utils;
	MatLab matlab;
	protected int MAX_ITERATIONS = 150;
	protected double[] pageRankVector;
	
	protected double  maxPageRankValue = 1.0;
	protected double  cFactor = 0.85;


	public Task4() throws Exception {
		dblpData = new DblpData();
		utils = new Utility();
		matlab = new MatLab();
	}
	public double[] getPageRankVector(){
		return pageRankVector;
	}
	public double getMaxPageRank(){
		return maxPageRankValue;
	}
	
	/*  Multiplies Matrix by a Vector 
	 *  input   double[][]  aMatrix
	 *  input   double[]    vector
	 *  
	 *  returns double[] vector = A * v 
	 */
	protected double[] vectorMultiplication(double[][] aMatrix, double[] vector) {
		double[] resultVector = new double[vector.length];

		for (int i = 0; i < aMatrix.length; i++) {
			//	resultVector[i] = 0;
			for (int k = 0; k < vector.length; k++){
				resultVector[i] += (aMatrix[i][k] * vector[k]);}
		}
		return resultVector;
	}

	// helper method. to normalize column vector using L1
	protected double[] vectorL1Norming( double[] vector) {
		double tally = 0.00000;
		double[] resultVector = new double[vector.length];		
		for (double entry:vector){
			tally += entry;
		}
		double holdDbl = 1.00/tally;
		for (int kk = 0; kk < vector.length; kk++){
			resultVector[kk] = (double)(vector[kk] * holdDbl);
		}
		return resultVector;
	}
	
	// helper method. to scale or normalize column vector to range of 0-1
	public double vectorScalingRange( double[] vector) {
		Arrays.sort(vector);  
		//range is max value - min value
		double range = vector[vector.length-1] - vector[0];
		return range;
	}  
	
	/**
	 * Compute Papers's Similarity Ranking using Page Rank algorithm
	 * @param cFactor is Beta weighting that gives preference to known edges or random papers
	 * @return a map in which the keys represent the authorIds and their ranking score. 
	 * 
	 * Uses power iteration method to find the steady state value of the page ranking vector of probabilities
	 * @throws IOException 
	 * @throws CorruptIndexException 
	 * @throws MatlabConnectionException 
	 * @throws MatlabInvocationException 
	 * */
	public ArrayList<Map.Entry<String, Double>> computePageRank(Integer K, Graph simGraph, Directory luceneIndexDir) throws CorruptIndexException, IOException, MatlabInvocationException, MatlabConnectionException {
		pageRankVector = new double[simGraph.getNumNodes()];
		double[][] simMatrixMStar = new double[pageRankVector.length][pageRankVector.length]; 
		
		double resetFactor = (1.00000-cFactor)/pageRankVector.length;
		simMatrixMStar = getMatrixMStar(resetFactor, simGraph.getAdjacencyMatrix());
		double[] probRVector = computeSteadyStateVector(simMatrixMStar);
//		double[] probRVector = computePowerIterationVector(simMatrixMStar);
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

		
	
/*	
	private Graph getsimGraph(){
		Map<Integer,String> authorMap = new HashMap<Integer,String>();
		SimilarityAnalyzer sa = new SimilarityAnalyzer();
		double[][] simMatrix  = null;
		
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
				
		try {
			simMatrix = sa.getCoAuthorSimilarityMatrix(authorTermFreq, allTerms);
		} catch (Exception e) {
			e.printStackTrace();
		}
		List<String> allAuthors = dblpData.getAllActiveAuthors(indexDir);
		int mapIndex = 0;
		for(String author : allAuthors){
			authorMap.put(mapIndex++,author);
		}
		return new Graph(simMatrix, authorMap);
	}
*/	
	public Map<String,Integer> getPaperMap(IndexReader reader) throws CorruptIndexException, IOException{
		Map<String,Integer> docIndexMap = new HashMap<String,Integer>();
		for (int i = 0; i < reader.maxDoc(); i++) {
			String temp = reader.document(i).get("paperid");
				docIndexMap.put(temp, i);
			}
		return docIndexMap;
	}
	
	public Map<Integer, String> getIndexPaperMap(Directory luceneIndexDir) throws CorruptIndexException, IOException{
		Map<Integer,String> docIndexMap = new HashMap<Integer,String>();
		
		IndexReader reader = IndexReader.open(luceneIndexDir);
		for (int i = 0; i < reader.maxDoc(); i++) {
			docIndexMap.put(i, reader.document(i).get("paperid"));
		}
		return docIndexMap;
	}
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
	 * Compute Page Rank algorithm's Transition Matrix M
	 * @param cFactor is Beta weighting that gives balances preference to known edges vs random nodes
	 * @param simGraph
	 * @return double[][] M* matrix 
	 * 
	 * MStar = Beta*(M) + (1-Beta)*RandomJumpResetMatrix  where M is column normed A transpose
	 * Adjacency Matrix -> Adjacency Transpose -> Column Normalized Adjacency Transpose -> MStar Matrix
	 * Uses power iteration method to find the steady state value of the page ranking vector of probabilities
	 * */
	protected double[][] getMatrixMStar(double resetFactor, double[][] simMatrix){
		double[][] simMatrixMStar = new double[simMatrix.length][simMatrix.length]; 
		
		for (int pageNum = 0; pageNum < simMatrix.length; pageNum++){
			// get weighted forward links from row of matrix A
			double[] simRowLinks = simMatrix[pageNum];
			double[] rowLinks = vectorL1Norming(simRowLinks);
			double tally = 0.00;
			for (double entry:rowLinks){
				tally += entry;
			}
			// put weighted normed row links in column to create a column of M
			// Normed columns morph to M
			// add Z if necessary & multiply by c to make M*
			// All Zero Column requires adding a column from Z matrix
			if(tally == 0.00){
				for (int j = 0; j < rowLinks.length; j++) {
					simMatrixMStar[j][pageNum] += cFactor*(1.00000/simMatrix.length) + resetFactor; 
				}	// avoid sink nodes by adding weak link 1/Number of pages
			}
			else {
				for (int j = 0; j < rowLinks.length; j++) {
					simMatrixMStar[j][pageNum] += cFactor*(rowLinks[j]) + resetFactor; 
				}
			}
		}	//end iterating through all rows of Similarity Matrix to produce new M* matrix
		return simMatrixMStar;
	}
	
	protected double[][] getAuthorMatrixMStar(double resetFactor, Graph simGraph){
		double[][] simMatrix = simGraph.getAdjacencyMatrix();
		double[][] simMatrixMStar = new double[simMatrix.length][simMatrix.length]; 
		Map<String,Integer> authorIndexMap = new HashMap<String,Integer>(); 
		Map<Integer,String> indexAuthorMap = simGraph.getNodeIndexLabelMap();

		int numAuthors = indexAuthorMap.size();

		Map<String, Set<String>> coauthorsMap = dblpData.getCoauthors();
		Set<String> noCoauthors = new HashSet<String>();
		Set<Integer> coauthorsIndex = new HashSet<Integer>();
		for(int i=0; i<numAuthors; i++){
			String author1 = indexAuthorMap.get(i);
			authorIndexMap.put(author1,i);
			int tempI  = 0;
			tempI = i;
			coauthorsIndex.add(tempI);
			if(!coauthorsMap.containsKey(author1)){
				noCoauthors.add(author1);
			}
		}
		for (int pageNum = 0; pageNum < simMatrix.length; pageNum++){
			// get forward links that should be put in row of A
			double[] simRowLinks = simMatrix[pageNum];
			double[] rowLinks = vectorL1Norming(simRowLinks);
			double tally = 0.0;
			for (double entry:rowLinks){
				tally += entry;
			}
		
			// put links in column to create a column of A Transpose
			// Norm columns to morph to M
			// add Z if necessary & multiply by c to make M*
			// All Zero Column requires adding a column from Z matrix
			
			String author1 = indexAuthorMap.get(pageNum);
			if(noCoauthors.contains(author1)){
				for (int j = 0; j < rowLinks.length; j++) {
					simMatrixMStar[j][pageNum] += cFactor*(1.00000/simMatrix.length) + resetFactor; 
				}	// avoid sink nodes by adding weak link 1/Number of pages
			}
			else {
				for (int j = 0; j < rowLinks.length; j++) {
					simMatrixMStar[j][pageNum] += cFactor*(rowLinks[j]) + resetFactor; 
				}
			}


		}	//end iterating through all rows of Similarity Matrix to produce new M* matrix

		return simMatrixMStar;
	}
	
	/**
	 * Compute coAuthor's Similarity Ranking using Page Rank algorithm
	 * @param cFactor is Beta weighting that gives preference to known edges or random authors
	 * @return a map in which the keys represent the authorIds and their ranking score. 
	 * 
	 * Uses power iteration method to find the steady state value of the page ranking vector of probabilities
	 * @throws IOException 
	 * @throws CorruptIndexException 
	 * */
	
	public ArrayList<Map.Entry<String, Double>> computeAuthorPageRank(Integer K, Graph simGraph) {
		
		pageRankVector = new double[simGraph.getNumNodes()];
		double resetFactor = (1.00000-cFactor)/pageRankVector.length;
		
		double[][] simMatrixMStar = getAuthorMatrixMStar(resetFactor, simGraph);
		
		double[] probRVector = computePowerIterationVector(simMatrixMStar);
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
	 * Compute coAuthor's Similarity Ranking using Page Rank algorithm
	 * @param cFactor is Beta weighting that gives preference to known edges or random authors
	 * @return a map in which the keys represent the authorIds and their ranking score. 
	 * 
	 * Uses Matlab eig function as method to find the steady state value of the page ranking vector of probabilites
	 * */
	public ArrayList<Map.Entry<String, Double>> computeAuthorRank(Integer K, Graph simGraph) throws MatlabInvocationException, MatlabConnectionException {
		int			length = simGraph.getNumNodes(); 
		double[]	cumulativeRank = new double[length];
		double[][] 	simGraphMStar = new double[length][length];
		pageRankVector = new double[length];
		
		double resetFactor = (1.00000-cFactor)/length;
		simGraphMStar = getAuthorMatrixMStar(resetFactor, simGraph);
		double[] probRVector = computeSteadyStateVector(simGraphMStar);
		pageRankVector = Arrays.copyOf(probRVector, probRVector.length);

		ArrayList<Map.Entry<String, Double>> retVal = new ArrayList<Map.Entry<String, Double>>();
		for (int ppages = 0; ppages < pageRankVector.length; ppages++ ){
			//pageRankVector[ppages] /= maxPageRankValue; 
			retVal.add(new AbstractMap.SimpleEntry<String,Double>(simGraph.getNodeLabel(ppages), probRVector[ppages]));
		}
		return retVal;
	}		// end compute Page Rank	

	/* 
	 * Compute the steady state vector R associated with eigen value 1.0 where R = M*R
	 * @param simMatrix is a symmetric matrix M
	 * @return R column vector to represent  a normalized steady state which is vector of probabilities 
	 */
	public double[] computeSteadyStateVector(double[][] simMatrix) throws MatlabInvocationException, MatlabConnectionException {
		int			length = simMatrix.length; 
		double[]	cumulativeRank = new double[length];
		double[][] 	simMatrixMStar = simMatrix;
		pageRankVector = new double[length];
				
		MatlabProxy proxy = MatLab.getProxy();
		String currentPath = Utility.getCurrentFolder();
		proxy.eval("cd "+currentPath);
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
		processor.setNumericArray("MS", new MatlabNumericArray(simMatrixMStar, null));
		proxy.eval("[S,D] = eig(MS)");
		proxy.eval("[W]=real(S)");
		double[][] eigenVectors2 = processor.getNumericArray("W").getRealArray2D();
		proxy.eval("[T]=real(D)");
		double[][] eigenValues2 = processor.getNumericArray("T").getRealArray2D();

		int eigenIndex = -1;
		for (int i = 0; i < eigenValues2.length; i++){
			if (Math.abs(eigenValues2[i][i] - 1.000) < 0.0001) {
				eigenIndex = i;
				break;
			}
		}
		double[] primaryEigenVector= new double[eigenVectors2.length];
		if (eigenIndex > -1){
			for (int j = 0; j < eigenVectors2.length; j++){
				primaryEigenVector[j] = eigenVectors2[j][eigenIndex];
			}
		}
		double[] probRVector = vectorL1Norming(primaryEigenVector);
		pageRankVector = Arrays.copyOf(probRVector, probRVector.length);
		return probRVector;
	}		// end compute Page Rank	
	
	/* 
	 * Compute the steady state vector R associated with eigen value 1.0 where R = M*R
	 * @param simMatrix is a symmetric matrix M
	 * @return R column vector to represent  a normalized steady state which is vector of probabilities
	 * Note, eigen vector from Matlab needs to be normalized to show probabilities  
	 */
	public double[] computePowerIterationVector(double[][] matrix){
		pageRankVector = new double[matrix.length];
		double[] cumulativeRank = new double[pageRankVector.length];
		
		// intialize page ranks to  1/N 
		Arrays.fill(pageRankVector, (1.0000000/matrix.length));

		// Recompute M*R until find eigen vector R aka pageRankVector	
		for (int iterator = 0; iterator < MAX_ITERATIONS; iterator++) {	
			// multiply M* x R i
			cumulativeRank =  vectorMultiplication(matrix, pageRankVector);

			// check for convergence of eigen vector for M*
			double totalEpsilon = 0;
			for (short d = 0; d < pageRankVector.length; d++) {
				double epsilon = (pageRankVector[d] - cumulativeRank[d]);		//cumulativeRank is destination vector
				totalEpsilon += epsilon * epsilon;
			} 
			// Wait for pageRank and new cumulativeRank to converge before stopping iterations
			totalEpsilon = Math.sqrt(totalEpsilon);  // optional, does not impact ranking significantly
		
			if (totalEpsilon < .00000001){
				break; // convergence on both vectors
			}
			// update page rank vector with most recent iteration of M*R
			else pageRankVector = Arrays.copyOf(cumulativeRank, cumulativeRank.length);
			// zero out new page rank that accumulates in cumulative Rank every iteration.  ie Dest = 0
			Arrays.fill(cumulativeRank,0.0);
		}	// end iteration of M* * R to converge onto R
		double[] tempVector =  Arrays.copyOf(pageRankVector, pageRankVector.length);
		return tempVector;
	}
	
	public void doTask4a(Integer K, Graph simGraph){

		ArrayList<Map.Entry<String, Double>> authorRank = computeAuthorPageRank( K,simGraph);

		// Sort them in descending order
		Collections.sort(authorRank, new MapEntryComparable());

		HashMap<String, String> authNamePersonIdList = dblpData.getAuthNamePersonIdList();
		int maxDisplay = Math.min(K,authorRank.size());
		System.out.println("Dominant "+ maxDisplay + " Authors");
		System.out.println();
		System.out.println("Author               Author ID    Ranking Score" );
		for (int j = 0; j < maxDisplay; j++) {
			System.out.printf("%-20s %-12s " ,authNamePersonIdList.get(authorRank.get(j).getKey()),authorRank.get(j).getKey());
			System.out.printf("%10.10f", authorRank.get(j).getValue()); 
			System.out.println();
		}

		try {
			ArrayList<Map.Entry<String, Double>> authorRank2 = computeAuthorRank( K, simGraph);

			// Sort them in descending order
			Collections.sort(authorRank2,  new MapEntryComparable());

			int maxDisplay2 = Math.min(K,authorRank2.size());
			System.out.println();
			System.out.println();
			System.out.println("Dominant "+ maxDisplay2 + " Authors");
			System.out.println();
			System.out.println("Author		      AuthorID      Ranking Value" );
			System.out.println("---------------------------------------------------------------------------------");
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

 void doTask4b(Integer K, Graph simGraph,Directory luceneIndexDir) throws CorruptIndexException, IOException, MatlabInvocationException, MatlabConnectionException{
		
		ArrayList<Map.Entry<String, Double>> paperRank = computePageRank( K, simGraph,luceneIndexDir);
		// Sort them in descending order
		Collections.sort(paperRank, new MapEntryComparable());
		
//		Directory luceneIndexDir = dblpData.createAllDocumentIndex();
		IndexReader papersReader =  IndexReader.open(luceneIndexDir);
		Map<String,Integer>  paperIndexMap = getPaperMap(papersReader);		
		int maxDisplay = Math.min(K,paperRank.size());
		System.out.println("Dominant "+ maxDisplay + " Papers");
		System.out.println();
		System.out.println(" Paper       Ranking Score     Title" );
		System.out.println("-----------------------------------------------------------------------------");
		for (int j = 0; j < maxDisplay; j++) {
			System.out.printf(" %-12s%-11.11f",paperRank.get(j).getKey(),paperRank.get(j).getValue());
			System.out.println("    "+ papersReader.document(paperIndexMap.get(paperRank.get(j).getKey())).get("title"));
		}
	}
	
	class MapEntryComparable implements Comparator<Map.Entry<String, Double>> {
		@Override
		public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
			return Double.compare(arg1.getValue(), arg0.getValue());
		}
	}


	public static void main(String[] args) {
		System.out.println("\nEnter Command (task args1, args2....)   ");
		Scanner sc = new Scanner(System.in);
		String command = sc.nextLine();
		String[] tokens = command.split(" ");

		String taskName = tokens[0];
		int k = Integer.parseInt(tokens[1]);
		Task4 task;
		try {
			task = new Task4();
			task.setCFactor(0.85);
			if (taskName.equalsIgnoreCase("task4a")){
				try{
					Task1 task1 = new Task1();
				//	Graph simGraphTest  = task.getsimGraph();
				//	task.doTask4a(k,simGraphTest);
					Graph simGraph = task1.getCoauthorSimilarityGraph_KeywordVector();
					task.doTask4a(k,simGraph);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (taskName.equalsIgnoreCase("task4b")){
				Directory luceneIndexDir = task.dblpData.createAllDocumentIndex();
//				Graph simGraph = task.getPaperSimilarityGraph( luceneIndexDir);
//				task.doTask4b(k,simGraph, luceneIndexDir);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public double getCFactor() {
		return cFactor;
	}
	public void setCFactor(double cFactor) {
		this.cFactor = cFactor;
	}

}


