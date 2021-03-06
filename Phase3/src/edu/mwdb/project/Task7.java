package edu.mwdb.project;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;


import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

import edu.mwdb.project.DblpData;
import edu.mwdb.project.MatLab;
import edu.mwdb.project.Utility;
import java.text.DecimalFormat;

public class Task7 {

	DblpData db;
	Utility utils;
	MatLab matlab;
	double 	THRESHOLD = 4.0;  // as using raw tf the word counts are higher than if using tfidf
	

	public Task7() throws Exception {
		db = new DblpData();
		utils = new Utility();
		matlab = new MatLab();
	}

	public TaskResults doTask7a( List<String> relevantAuthors, Integer k, Map<Integer,String> graphNodes,String sourceAuthor){
		Map<String, TermFreqVector>  nodeKWVectors	= new HashMap<String, TermFreqVector>();
		TaskResults output = null;
		try {
			Directory authorIndex2 = db.createAuthorDocumentIndex();
			Map<String, TermFreqVector>  allAuthors = db.getAuthorTermFrequencies(authorIndex2);
			List<String> allGraphAuthors = new ArrayList(graphNodes.values());
			for (String author : allGraphAuthors){
				nodeKWVectors.put(author, allAuthors.get(author));
			}

			HashMap<String,Double> all2PFs = getALLPF(k, nodeKWVectors, relevantAuthors, allGraphAuthors);
			ArrayList<Map.Entry<String, Double>> similarities = computeFeedbackSim(all2PFs, relevantAuthors,  k,  allGraphAuthors,nodeKWVectors, sourceAuthor);

//			displayAuthors(similarities,k, sourceAuthor);
			HashMap<String,Double> newQuery = computeAdjustedQuery(allAuthors.get(sourceAuthor), all2PFs);
//			HashMap<String,Double> newQueryforDisplay = displayAdjustedQuery(allAuthors.get(sourceAuthor), all2PFs);
//			displayAdjustedQueryMap(newQuery);		
			output = new TaskResults(similarities,newQuery,allAuthors.get(sourceAuthor) );
			return output;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}
	
	public TaskResults doTask7b( List<String> relevantPapers, int k, Map<Integer,String> graphNodes, String sourceNode){
		Map<String, TermFreqVector>  nodeKWVectors	= new HashMap<String, TermFreqVector>();
		TaskResults output = null;
		try{
		Directory allIndex = db.createAllDocumentIndex();
		Map<String, TermFreqVector>  allPapers =  getDocTermFrequencies(allIndex);
		List<String> allGraphPapers = new ArrayList(graphNodes.values());
		for (String paper : allGraphPapers){
			nodeKWVectors.put(paper, allPapers.get(paper));
		}
		HashMap<String,Double> all2PFs = getALLPF(k, nodeKWVectors, relevantPapers, allGraphPapers);
		ArrayList<Map.Entry<String, Double>> similarities = computeFeedbackSim(all2PFs, relevantPapers,  k,  allGraphPapers,nodeKWVectors, sourceNode);
		
		HashMap<String,Double> newQuery = computeAdjustedQuery(allPapers.get(sourceNode), all2PFs);
		output = new TaskResults(similarities,newQuery,allPapers.get(sourceNode) );
		return output;
		} catch (Exception e) {
				e.printStackTrace();
		}
		return output;
	}
	
	public ArrayList<Map.Entry<String, Double>> computeFeedbackSim(HashMap<String,Double> all2PFs,List<String> relevantNodes,Integer k,List<String> allNodes, Map<String, TermFreqVector>  nodeKWVectors, String sourceAuthor) throws Exception{
	
		
		ArrayList<Map.Entry<String, Double>> retVal = new ArrayList<Map.Entry<String, Double>>();
		for (String node : relevantNodes){
			TermFreqVector tfv = nodeKWVectors.get(node);
			double simDQ = totalAuthorKeywordAdjPFVector(tfv,all2PFs);
			retVal.add(new AbstractMap.SimpleEntry<String,Double>(node, simDQ));
		}
		allNodes.removeAll(relevantNodes);
		/* ???????????????? */
		/* compute new similarity for non-relevant nodes */
		for (String node : allNodes){
			TermFreqVector tfv = nodeKWVectors.get(node);
			double simDQ = totalAuthorKeywordAdjPFVector(tfv,all2PFs);
			retVal.add(new AbstractMap.SimpleEntry<String,Double>(node, simDQ));
		}
		return retVal;
	}
	/*
	 *  computes relevance feedback formula sim(D,Q) = summation d_i * PF_i
	 */
	public double totalAuthorKeywordAdjPFVector(TermFreqVector authorTermFreqVector, HashMap<String,Double> allPFs) throws Exception{
		double dqSim = 0.0;
		double weight = 1.0;

		String termTexts[] = authorTermFreqVector.getTerms();
		int termFreqs[] = authorTermFreqVector.getTermFrequencies();
				
		for(int i=0; i<termTexts.length; i++){
			Double pfFactor = allPFs.get(termTexts[i]);
			weight = termFreqs[i];
		//	if (termFreqs[i] > THRESHOLD)
				dqSim += weight*pfFactor;
		}
		return dqSim;
		}
	/* compute modified query vector */
	public double[] computeModifiedKWVector(TermFreqVector authorTermFreqVector, HashMap<String,Double> allPFs) throws Exception{
		double[] movedKWVector = new double[authorTermFreqVector.size()];
		double weight = 1.0;
		
		String termTexts[] = authorTermFreqVector.getTerms();
		int termFreqs[] = authorTermFreqVector.getTermFrequencies();
		for(int i=0; i<termTexts.length; i++){
			Double pfFactor = allPFs.get(termTexts[i]);
			if (termFreqs[i] > 0){
				movedKWVector[i] = weight*pfFactor;
			//	movedKWVector[i]*= getIDF(docIndexReader, term);	/**** code for tfidf processing *****/
			//	movedKWVector[i] *= pfFactor;
			}
			else
				movedKWVector[i] = 0;
		}
		return movedKWVector;
	}
	
	public void displayAuthors(ArrayList<Map.Entry<String, Double>> similarities,int k, String sourceAuthor){

		// Sort them in descending order
		Collections.sort(similarities,  new MapEntryComparable());

		HashMap<String, String> authNamePersonIdList = db.getAuthNamePersonIdList();	
		System.out.println("Relevance Feed Back Query: " + authNamePersonIdList.get(sourceAuthor));
		System.out.println();
		int maxDisplay = Math.min(k,similarities.size());
		System.out.println();
		System.out.println("Most Relevant "+ maxDisplay + " Authors");
		System.out.println();
		System.out.println("Author		      AuthorID      Similarity Value" );
		System.out.println("----------------------------------------------------");
		for (int j = 0; j < maxDisplay; j++) {
			String authorID = similarities.get(j).getKey();
			System.out.printf("%-20s  %-12s  %-10.10f",authNamePersonIdList.get(authorID), authorID, similarities.get(j).getValue());
			System.out.println();
		}
	}
	
	public void displayPapers(ArrayList<Map.Entry<String, Double>> similarities,int k, String sourceNode, Directory luceneIndexDir) throws CorruptIndexException, IOException{

		// Sort them in descending order
		Collections.sort(similarities,  new MapEntryComparable());
		
		IndexReader papersReader =  IndexReader.open(luceneIndexDir);
		Map<String,Integer>  paperIndexMap = getPaperMap(papersReader);
		System.out.println();
		System.out.println("Relevance Feed Back Query: " + papersReader.document(paperIndexMap.get(sourceNode)).get("title"));
		System.out.println();
		int maxDisplay = Math.min(k,similarities.size());
		System.out.println();
		System.out.println("Most Relevant "+ maxDisplay + " Papers");
		System.out.println();
		System.out.println("Paper ID    Similarity       Title" );
		System.out.println("----------------------------------------------------");
		for (int j = 0; j < maxDisplay; j++) {
			String paperID = similarities.get(j).getKey();
			System.out.printf("%-12s  %-10.3f  %-20s", paperID, similarities.get(j).getValue(),papersReader.document(paperIndexMap.get(paperID)).get("title"));
			System.out.println();
		
		}
	}
	
	
	public HashMap<String,Double>computeAdjustedQuery(TermFreqVector queryKeyWord, HashMap<String,Double> allPFs) throws Exception {
		double[] adjKeywords = computeModifiedKWVector(queryKeyWord, allPFs);
		String[] currentKeywords = queryKeyWord.getTerms();
		int[] currentValues	= queryKeyWord.getTermFrequencies();
		HashMap<String,Double> tempVector = new HashMap<String,Double>();
		for (int i=0; i<adjKeywords.length; i++) {
			tempVector.put(currentKeywords[i], (double)adjKeywords[i]);
		}
		return tempVector;
	}
	

	public void displayAdjustedQueryMap(HashMap<String,Double> queryKeyWord) {
		int index = 0;
		System.out.println("Query");
		System.out.println("KeyWord Values:   new         new            new          new");
		for (Map.Entry<String,Double> keyword : queryKeyWord.entrySet()){
			System.out.printf("%-15s ",keyword.getKey());
			System.out.printf("%-3f  ",keyword.getValue());
			index++;
			if ((index % 10) == 0) System.out.println();
		}
	}
	
	public void displayAdjustedQuery(HashMap<String,Double> newQuery, TermFreqVector queryKeyWord) {
		
		String[] currentKeywords = queryKeyWord.getTerms();
		int[] currentValues	= queryKeyWord.getTermFrequencies();
		int index = 0;
		boolean newline = false;
                
                
		System.out.println("\n\n Query");
                System.out.println("");
                System.out.format("%-20s%-10s%-10s","KeyWord","Old","New");
               // System.out.format("%-20s%-10s%-10s","KeyWord","Old","New");
                System.out.println("");
		
		DecimalFormat f = new DecimalFormat("#.##");
		
                for (Map.Entry<String,Double> keyword : newQuery.entrySet()){
                    if (!((keyword.getValue() == 0) && (keyword.getValue() == 0))){
                            System.out.format("%-20s%-10s%-10s\n", keyword.getKey(), f.format(currentValues[index]), f.format(keyword.getValue()));                            
                    }
                    
                    index++;
			
                        
		}
	}
	
	/**
	 * Given a termFreqVectors from graph, computes the PF for all the keywords in the entire corpus
	 * Could be improved if speed performance an issue
	 * @param relevantKWVectors
	 * @param nonRelevantKWVectors
	 * 
	 * @return a HashMap that contains the terms as the key and the pf as the value.
	 * @throws Exception
	 */
	public  HashMap<String,Double> getALLPF(Integer k, Map<String,TermFreqVector> allAuthorsKeywVectors, List<String> relevantAuthors, List<String> allNodeAuthors){
		
		HashMap<String,HashMap<String,Double>> forwardIndex =  getForwardAllNodesKeywIndex(allAuthorsKeywVectors);
		
		List<String> allWords = db.getAllTermsInIndex(db.createAllDocumentIndex(), "doc");
	
		Set<String> nonrelevantAuthors = new HashSet<String>(allNodeAuthors);
		nonrelevantAuthors.removeAll(relevantAuthors); 
		double R = relevantAuthors.size();
		double N = allNodeAuthors.size();
		
		HashMap<String,Double> retVal = new HashMap<String,Double>(allWords.size());
		for (String word : allWords) {
  			// Calculate the number of relevant nodes containing the keyword
  			double r_ij = 0;
  			for (String nodeId : relevantAuthors) {
  				if (forwardIndex.get(nodeId).containsKey(word))
  					r_ij++;
  			}
  			
  			// Calculate number of  non-relevant nodes containing the keyword
  			double n_ij = 0;
  			for (String nodeId : nonrelevantAuthors) {
  				if (forwardIndex.get(nodeId).containsKey(word))
  					n_ij++;
  			}
  			n_ij += r_ij;		// need all nodes that contain keyword
  		//	if ((r_ij++)>0  && (n_ij++ > 0)){
  				double result = doFormulaAdjPF(R, N, r_ij, n_ij);
  				retVal.put(word, result);
  		//	}
		}
		return retVal;
	}

	public Map<String,Integer> getPaperMap(IndexReader reader) throws CorruptIndexException, IOException{
		Map<String,Integer> docIndexMap = new HashMap<String,Integer>();
		for (int i = 0; i < reader.maxDoc(); i++) {
			String temp = reader.document(i).get("paperid");
				docIndexMap.put(temp, i);
			}
		return docIndexMap;
	}
	
	/**
	 * Given an termFreqVectors, computes the PF for all the keywords
	 * @param relevantKWVectors
	 * @param nonRelevantKWVectors
	 * 
	 * @return a HashMap that contains the terms as the key and the pf as the value.
	 * @throws Exception
	 */
	public  HashMap<String,Double> getPF(Integer k,Map<String, TermFreqVector>  relevantKWVectors, Map<String, TermFreqVector>  nonRelevantKWVectors,List<String> relevantAuthors) throws Exception {
		DblpData dblp = new DblpData();
		Directory luceneIndex = dblp.createAllDocumentIndex();
		IndexReader  reader = IndexReader.open(luceneIndex);
		
		double R = (double)relevantKWVectors.size(); // numRelevant
		double N = nonRelevantKWVectors.size() + R;  //number of Graph Nodes;	// OR k if we only considered returned documents
		int size = (int) reader.getUniqueTermCount();
		HashMap<String,Double> rValues = new HashMap<String,Double>(size);
		HashMap<String,Double> nLessrValues = new HashMap<String,Double>(size);
		HashMap<String,Double> retVal = new HashMap<String,Double>(size);
		
		/* compute r for every term in relevant keyword vectors */
		/* r i is the number of authors containing keyword i */
		for (Map.Entry<String, TermFreqVector> entry : relevantKWVectors.entrySet()){
			String index = entry.getKey();
			TermFreqVector tf = entry.getValue();
			String termTexts[] = tf.getTerms();
			int termFreqs[] = tf.getTermFrequencies();
					
			for(int i=0; i<termTexts.length; i++){		
  			
  			double r_ij = 1.0;
  			String word = termTexts[i];
  			if (rValues.containsKey(word)){
  					r_ij += rValues.get(word);
  	 			}
  			rValues.put(word, r_ij);
			}
		}  			
		/* compute n-r for every term in non-relevant keyword vectors */
		/* r i is the number of authors containing keyword i */	
  		for (Map.Entry<String, TermFreqVector> entry : nonRelevantKWVectors.entrySet()){
  				String index = entry.getKey();
  				TermFreqVector tf = entry.getValue();
  				String termTexts[] = tf.getTerms();
  				int termFreqs[] = tf.getTermFrequencies();
  						
  			for(int i=0; i<termTexts.length; i++){		
  	  			double n_ij = 1.0;
  	  			String word = termTexts[i];
  	  			if (nLessrValues.containsKey(word)){
  	  					n_ij += nLessrValues.get(word);
  	  	 			}
  	  			nLessrValues.put(word, n_ij);
  			}
  		}
  		/* calculate pf for terms from relevant documents */
  		for (Map.Entry<String, Double> entry : rValues.entrySet()){
				String index = entry.getKey();
				double rCount = entry.getValue();
				double nCount = 0.0;
				if (nLessrValues.containsKey(index)){
					nCount = nLessrValues.get(index) + rCount;
				}
				else nCount = rCount;
				double result = doFormulaAdjPF(R, N, rCount, nCount);
  			retVal.put(index, result);
		}
  		
  		/* calculate pf for terms from non-relevant documents */
  		for (Map.Entry<String, Double> entry : nLessrValues.entrySet()){
				String index = entry.getKey();
				if(!retVal.containsKey(index)){
					/* r is zero ie term not found in relevant documents */
					double nCount = entry.getValue();
					double rCount = 0.0;
					double result = doFormulaAdjPF(R, N, rCount, nCount);
					retVal.put(index, result);
				}
		}
		return retVal;
	}
	
		
	/**
	 * Calculates the PF value
	 * @param R - ||relevant nodes||
	 * @param N - ||total nodes||
	 * @param r - number of relevant nodes containing keyword i
	 * @param n - number of non-relevant nodes containing keyword
	 * @return
	 */
	public static double doFormulaAdjPF(Double R, Double N, Double r, Double n) {
		/* compute the formula for PF Model for this term tempTerm */
		Double leftPart = Math.log(((r + 0.5) / ((R - r) + 1)) / (((n - r) + 0.5) / ((N - n - R + r) + 1)));
		return (leftPart);
	}
	
	/**
	 * Creates a forward index for node keyword vectors
	 * @param allAuthorsKeywVectors obtained 
	 * @return a first level hashmap with node id as a String key. The second level hashmap has the keyword as a string
	 * 			key and a double value for the tf
	 */
	public HashMap<String,HashMap<String,Double>> getForwardAllNodesKeywIndex(Map<String,TermFreqVector> allNodesKeywVectors) {
		HashMap<String,HashMap<String,Double>> forwardIndex = new HashMap<String,HashMap<String,Double>>();
		
		Set<String> keys = allNodesKeywVectors.keySet();
		for (String key : keys) {
			TermFreqVector termFreqVector = allNodesKeywVectors.get(key);
			String termTexts[] = termFreqVector.getTerms();
			int termFreqs[] = termFreqVector.getTermFrequencies();
			
			HashMap<String,Double> temp = new HashMap<String,Double>();
			for (int i=0; i<termTexts.length; i++) {
				temp.put(termTexts[i], (double)termFreqs[i]);
			}
			
			forwardIndex.put(key, temp);
		}
		return forwardIndex;
	}
	
	public TaskResults doTask7(List<String> Labels, int k,Map<Integer,String> graphNodes, String sourceNode ){
		
		HashMap<String, String> authNamePersonIdList = db.getAuthNamePersonIdList();
		TaskResults output = null;
		if (authNamePersonIdList.containsKey(Labels.get(0)))
			output = doTask7a(Labels,k, graphNodes, sourceNode);
		else 
			output = doTask7b(Labels,k, graphNodes,sourceNode);	
		return output;
	}
	
	

	public Map<String, TermFreqVector> getDocTermFrequencies(Directory luceneIndexDir){
		IndexReader reader;
		Map<String, TermFreqVector> docTermFrequencies = new HashMap<String, TermFreqVector>();
		try {
			reader = IndexReader.open(luceneIndexDir);
			for (int i = 0; i < reader.maxDoc(); i++) {
				String index = reader.document(i).get("paperid");
				TermFreqVector tfv = reader.getTermFreqVector(i, "doc");
				docTermFrequencies.put(index, tfv);
			}
			reader.close();
			 
		} catch (CorruptIndexException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return docTermFrequencies;
	}
	

	public static void main(String[] args)  {
		try {
			System.out.println("\nEnter Command (task args1, args2....)   ");
			Scanner sc = new Scanner(System.in);
			String command = sc.nextLine();
			String[] tokens = command.split(" ");

			String taskName = tokens[0];
			int k = Integer.parseInt(tokens[1]);
			
			//String testNode = tokens[2];
			Task2 task2 = new Task2();
			Task1 task1 = new Task1();
			Task7 task = new Task7();
			
			Directory docIndex = task.db.createAllDocumentIndex();
			Graph authorGraph = task1.getCoauthorSimilarityGraph_KeywordVector();
			
			String testAuthor = "1636579";
		
			
			Map.Entry<String, Double>[] task5result = Task5.GraphSearchContent(authorGraph, testAuthor, k);
			List<String> nodeList = new ArrayList<String>();
			for(int i = 0; i < k; i++){
				nodeList.add(task5result[i].getKey());
				System.out.print(task5result[i].getKey());
				System.out.print(" ");
				System.out.println(task5result[i].getValue());
				}

			
			TaskResults outputTask5 = task.doTask7(nodeList, k, authorGraph.getNodeIndexLabelMap(),testAuthor);
			task.displayAuthors(outputTask5.getSimilarities(),k, testAuthor);
			task.displayAdjustedQuery(outputTask5.getNewTermFreqVector(),outputTask5.getOldQuery());
			
			testAuthor = "1792339";
		
			
			Map.Entry<String, Double>[] task5bresult = Task5.GraphSearchContent(authorGraph, testAuthor, k);
			List<String> nodeListb = new ArrayList<String>();
			for(int i = 0; i < k; i++){
				nodeListb.add(task5bresult[i].getKey());
				System.out.print(task5bresult[i].getKey());
				System.out.print(" ");
				System.out.println(task5bresult[i].getValue());
				}

			
			TaskResults outputTask5b = task.doTask7(nodeListb, k, authorGraph.getNodeIndexLabelMap(),testAuthor);
			task.displayAuthors(outputTask5b.getSimilarities(),k, testAuthor);
			task.displayAdjustedQuery(outputTask5b.getNewTermFreqVector(),outputTask5b.getOldQuery());
			
			

			Graph paperGraph = task2.getCoauthorPapersSimilarityGraph_KeywordVector("TF");
			String testPaper = "159366";
			task5result = Task5.GraphSearchContent(paperGraph, testPaper, k);
			nodeList = new ArrayList<String>();
			System.out.println();
			System.out.println("Paper 		 Similarity");
			for(int i = 0; i < k; i++){
				nodeList.add(task5result[i].getKey());
				System.out.print(task5result[i].getKey());
				System.out.print(" ");
				System.out.println(task5result[i].getValue());
			}
			
			TaskResults outputPapers = task.doTask7(nodeList, k, paperGraph.getNodeIndexLabelMap(),testPaper);
			task.displayPapers(outputPapers.getSimilarities(),k, testPaper,docIndex);
			task.displayAdjustedQuery(outputPapers.getNewTermFreqVector(),outputPapers.getOldQuery());
			
	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/***********/
	
	/*************/
	public Map<Integer, TermFreqVector> getDocIndexTermFreq(Directory luceneIndexDir){
		IndexReader reader;
		Map<Integer, TermFreqVector> docTermFrequencies = new HashMap<Integer, TermFreqVector>();
		try {
			reader = IndexReader.open(luceneIndexDir);
			for (int i = 0; i < reader.maxDoc(); i++) {
				TermFreqVector tfv = reader.getTermFreqVector(i, "doc");
				docTermFrequencies.put(i, tfv);
			}
			reader.close();
			 
		} catch (CorruptIndexException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return docTermFrequencies;
	}
	
	class MapEntryComparable implements Comparator<Map.Entry<String, Double>> {
		@Override
		public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
			return Double.compare(arg1.getValue(), arg0.getValue());
		}
	}
}

