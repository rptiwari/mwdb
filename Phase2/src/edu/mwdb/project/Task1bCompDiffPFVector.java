package edu.mwdb.project;
/*
 *  Class that executes PF similarity comparison between all users
 *  and a single author.
 *  
 *  Separated due to the length of computation.
 *  Presently uses Lucene indexes for searching
 *  		  uses sql query to find the PF factors quickly
 */
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.extensions.MatlabNumericArray;
import matlabcontrol.extensions.MatlabTypeConverter;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task1bCompDiffPFVector {


	public void findPFDifferentiationSimilarUsers(String personNum) throws Exception {
		List<String> allAuthors = null;
		double[][] userMatrix = null;
		double[] 	theAuthorPFVector = null;
		
			DblpData db = new DblpData();
			Directory theIndex = db.createAllDocumentIndex();
			
			Directory authorIndex2 = db.createAuthorDocumentIndex();


			List<String> allAuthorterms =	db.getAllTermsInIndex(authorIndex2, "doc");
			LinkedHashMap<String, Integer> wordPosMap = (LinkedHashMap<String, Integer>) findMap(allAuthorterms);

			//		double[] authorKeyword = Utility.getAlignedTFIDFVector(keywordVector,wordPosMap,papersReader);

			allAuthors = db.getAllActiveAuthors(authorIndex2);
			userMatrix = new double[allAuthors.size()][wordPosMap.keySet().size()];
			int index = 0;
			for (String thisAuthor : allAuthors){
				double[] authorDiscriminationVector = new double[wordPosMap.keySet().size()];
				authorDiscriminationVector = getAlignedAuthorKeywordPFVector(thisAuthor, authorIndex2, wordPosMap);
				userMatrix[index] = authorDiscriminationVector;
				index++;
			}
	
			theAuthorPFVector = getAlignedAuthorKeywordPFVector(personNum, authorIndex2, wordPosMap);
			
			Object[]  objPF =  doUserCompare(userMatrix, theAuthorPFVector, 12);


			double[] indexPF = (double[]) objPF[0]; 
			double[] distPF = (double[]) objPF[1];
			display(indexPF, distPF, personNum, allAuthors);
	}		

	
	public Object[] doUserCompare( double[][] allUsersKeyWordMatrix,  double[] authKeywordMatrix, int kNumber) throws MatlabInvocationException, MatlabConnectionException, Exception{
		//LDA - Start
		DblpData db = new DblpData();
		
			
			double[][] usersPFMatrix = allUsersKeyWordMatrix;
			
			double[][] givenauthPFMatrix = new double[1][authKeywordMatrix.length];
			
			givenauthPFMatrix[0] = authKeywordMatrix;
			
		
			//Create a proxy, which we will use to control MATLAB
			MatlabProxy proxy = MatLab.getProxy();
			MatlabTypeConverter processor = new MatlabTypeConverter(proxy);

			String currentPath = Utility.getCurrentFolder();
			proxy.eval("cd "+currentPath);
			
			processor.setNumericArray("givenAuthPFArray", new MatlabNumericArray(givenauthPFMatrix, null));
			
			processor.setNumericArray("usersMatrix", new MatlabNumericArray(usersPFMatrix, null));
			proxy.setVariable("kRange",kNumber);  
			
			Object[] objLDA = null;
			objLDA = proxy.returningEval("knnsearch( usersMatrix, givenAuthPFArray,'k', kRange,'Distance','cosine')",2);
			
			return  objLDA;
	}
	

	public void display(double[] indexLDA, double[] distLDA, String authorid,List<String> authorIds) {

		DblpData db = new DblpData();
		
		System.out.println("Top 10 Similar Users - Comparing Users Differentiation Vectors -> PF");
		System.out.println("*************************************************************************");

		HashMap<String, String> authNamePersonIdList = db.getAuthNamePersonIdList();
		
		String[] authors = authorIds.toArray(new String[authorIds.size()]);
		System.out.println("For Author: " + authNamePersonIdList.get(authorid));
		if (authNamePersonIdList.get(authors[(int) (indexLDA[0] - 1)]) == null) {
			for (int i = 0; i < 10; i++) {
				System.out.println("Author Name: "
						+ authors[(int) (indexLDA[0] - 1)] + " "
						+ "Distance from given author: " + distLDA[i]);
			}
		} else {
			for (int j = 0; j < indexLDA.length; j++) {
				if (!authorid.equals(authors[(int) (indexLDA[j] - 1)])) {
					System.out.printf("Author Name: %-25s Distance from given author: %10.9f",
									authNamePersonIdList.get(authors[(int) (indexLDA[j] - 1)]),	distLDA[j]);
					System.out.println();
				}
			}
		}
	}
	
	public double[] getAlignedAuthorKeywordPFVector(String personNum, Directory  luceneIndex, LinkedHashMap<String, Integer> wordPosMap) throws Exception{
			

		HashMap<String,Double> allPFs = Utility.getPF(personNum);
		
		/* Get Author's Differentiation Vector */			
			TermFreqVector authorTermFreqVector = getAuthorKeywordVector(personNum, luceneIndex);
				double[] alignedVector = new double[wordPosMap.keySet().size()];
				
				String termTexts[] = authorTermFreqVector.getTerms();
				int termFreqs[] = authorTermFreqVector.getTermFrequencies();
				
				for(int i=0; i<termTexts.length; i++){
					if(!wordPosMap.containsKey(termTexts[i])){
						System.out.println(termTexts[i]);
					}
					int j = wordPosMap.get(termTexts[i]);
					if(j != -1){
						Double pfFactor = allPFs.get(termTexts[i]);
						alignedVector[j] = termFreqs[i]*pfFactor;
					}
				}
				return alignedVector;
		}

	public Map<String, Integer> findMap(List<String> completeKeywordList){
		int termIdx = 0;
		Map<String, Integer> allKeywordsPosMap = new LinkedHashMap<String, Integer>();
		for(String kw:completeKeywordList){
			allKeywordsPosMap.put(kw, termIdx++);
		}
		return allKeywordsPosMap;

	}
	public TermFreqVector getAuthorKeywordVector(String authorId,Directory luceneIndexDir) throws CorruptIndexException, IOException
	{	DblpData db = new DblpData();
	 Map<String, TermFreqVector>  allAuthors = db.getAuthorTermFrequencies(luceneIndexDir);
	 TermFreqVector tfv = allAuthors.get(authorId);
	 return tfv;
	}
	
	
	public void findFasterPFSimilarUsers(String personNum) throws Exception {
		List<String> allAuthors = null;
		double[][] userMatrix = null;
		double[] 	theAuthorPFVector = null;
		
			DblpData db = new DblpData();
			Directory theIndex = db.createAllDocumentIndex();
			
			Directory authorIndex2 = db.createAuthorDocumentIndex();


			List<String> allAuthorterms =	db.getAllTermsInIndex(authorIndex2, "doc");
			LinkedHashMap<String, Integer> wordPosMap = (LinkedHashMap<String, Integer>) findMap(allAuthorterms);

			//		double[] authorKeyword = Utility.getAlignedTFIDFVector(keywordVector,wordPosMap,papersReader);

			allAuthors = db.getAllActiveAuthors(authorIndex2);
			userMatrix = new double[allAuthors.size()][wordPosMap.keySet().size()];
			
			Map<String, TermFreqVector> allAuthorTFs =  db.getAuthorTermFrequencies(authorIndex2);
			List<String> allWords = db.getAllTermsInIndex(theIndex, "doc");
			HashMap<Integer,HashMap> forwardIndex = db.getForwardAndInversePaperKeywIndex()[0];

			int index = 0;
			for (String thisAuthor : allAuthors){
				double[] alignedPFVector= new double[wordPosMap.keySet().size()];
				TermFreqVector  authorDiscriminationVector = allAuthorTFs.get(thisAuthor);
				alignedPFVector = applyPFFactor(thisAuthor, authorDiscriminationVector,forwardIndex,allWords, wordPosMap);
				userMatrix[index] = alignedPFVector;
				index++;
			}
	
			theAuthorPFVector = getAlignedAuthorKeywordPFVector(personNum, authorIndex2, wordPosMap);
			
			Object[]  objPF =  doUserCompare(userMatrix, theAuthorPFVector, 12);


			double[] indexPF = (double[]) objPF[0]; 
			double[] distPF = (double[]) objPF[1];
			display(indexPF, distPF, personNum, allAuthors);
	}		

	private double[] applyPFFactor(String personNum, TermFreqVector authorTermFreqVector,HashMap<Integer,HashMap> forwardIndex,List<String> allWords,LinkedHashMap<String, Integer> wordPosMap ) throws Exception{
		double[] alignedVector = new double[wordPosMap.keySet().size()];
		
		HashMap<String,Double> allPFs = getPFAll(personNum,forwardIndex, allWords);

		
		String termTexts[] = authorTermFreqVector.getTerms();
		int termFreqs[] = authorTermFreqVector.getTermFrequencies();

		for(int i=0; i<termTexts.length; i++){
			if(!wordPosMap.containsKey(termTexts[i])){
				System.out.println(termTexts[i]);
			}
			int j = wordPosMap.get(termTexts[i]);
			if(j != -1){
				Double pfFactor = allPFs.get(termTexts[i]);
				alignedVector[j] = termFreqs[i]*pfFactor;
			}
		}
		return alignedVector;
	}
	
	/**
	 * Given an authorId, computes the PF for all the keywords
	 * @param authorId
	 * @return a HashMap that contains the terms as the key and the pf as the value.
	 * @throws Exception
	 */
	public static HashMap<String,Double> getPFAll(String authorId,HashMap<Integer,HashMap> forwardIndex, List<String> allWords) throws Exception {
		DblpData dblp = new DblpData();
		
		HashSet<Integer> paperIdsByCoauthors = dblp.getPaperIdsFromCoauthorExcludingSelf(authorId);
		Set<Integer> paperIdsByCoauthorsAndSelf = dblp.getPaperIdsFromCoauthorAndSelf(authorId);
		Utility ut = new Utility();
		
		double R = paperIdsByCoauthors.size();
		double N = paperIdsByCoauthorsAndSelf.size();
		
		HashMap<String,Double> retVal = new HashMap<String,Double>(allWords.size());
		for (String word : allWords) {
  			// Calculate the number of coauthor papers not containing the keyword
  			double r_ij = 0;
  			for (int paperId : paperIdsByCoauthors) {
  				if (!forwardIndex.get(paperId).containsKey(word))
  					r_ij++;
  			}
  			
  			// Calculate number of papers in coauthor_and_self(ai) not containing the keyword
  			double n_ij = 0;
  			for (int paperId : paperIdsByCoauthorsAndSelf) {
  				if (!forwardIndex.get(paperId).containsKey(word))
  					n_ij++;
  			}
  			
  			double result = ut.doFormulaPF(R, N, r_ij, n_ij);
  			retVal.put(word, result);
		}
		return retVal;
	}
	
}

