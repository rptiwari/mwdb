package edu.mwdb.project;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.extensions.MatlabNumericArray;
import matlabcontrol.extensions.MatlabTypeConverter;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.index.CorruptIndexException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
 

public class Task1cCompDocs {


	public void findKWSimilarDocs(String personNum) {
		String fileName = null;
		String rankingMethod = "tfidf";
		double[][] documentMatrix = null;
		Hashtable<Integer, VectorRanking> Similarities = new Hashtable<Integer, VectorRanking>();

		try {
			DblpData db = new DblpData();
			Directory theIndex = db.createAllDocumentIndex();
			
			IndexReader papersReader = IndexReader.open(theIndex);

				Directory theIndextoAuthors = db.createAuthorDocumentIndex();
				
				TermFreqVector keywordVector = getAuthorKeywordVector(personNum, theIndextoAuthors);
				
				List<String> allterms =	db.getAllTermsInIndex(theIndex, "doc");
				LinkedHashMap<String, Integer> wordPosMap = (LinkedHashMap<String, Integer>) findMap(allterms);
				
				double[] authorKeyword = Utility.getAlignedTFIDFVector(keywordVector,wordPosMap,papersReader);
				
				Directory nonAuthorIndex = db.createNonAuthoredDocsIndex(personNum);
			
				Similarities =  getSimilarity(theIndex,nonAuthorIndex, authorKeyword, wordPosMap);
				
				
				if (!Similarities.isEmpty()){
					// Create an array containing the elements from hashtable Similarities
					VectorRanking[] rankingArray =	doRanking(Similarities);	

					displayRanking(rankingArray, papersReader);

					Similarities.clear();


				}						// end if similarities is non-empty
				else { System.out.println("0 total matching documents");}
				papersReader.close();
				
			
			} catch (Exception e) {
				System.out.println(" caught a " + e.getClass()
						+ "\n with message: " + e.getMessage());
			}
		}					// end main of task1c

	
	
	
	public void findDifferentiationSimilarDocs(String personNum, String divVectorType) throws Exception {
		String rankingMethod = divVectorType;
		double[][] documentMatrix = null;
		Hashtable<Integer, VectorRanking> Similarities = new Hashtable<Integer, VectorRanking>();

		
			DblpData db = new DblpData();
			Directory theIndex = db.createAllDocumentIndex();
			IndexReader papersReader = IndexReader.open(theIndex);

			Directory theIndex2 = db.createAuthorDocumentIndex();


			List<String> allterms =	db.getAllTermsInIndex(theIndex, "doc");
			LinkedHashMap<String, Integer> wordPosMap = (LinkedHashMap<String, Integer>) findMap(allterms);

			//		double[] authorKeyword = Utility.getAlignedTFIDFVector(keywordVector,wordPosMap,papersReader);

			Directory nonAuthorIndex = db.createNonAuthoredDocsIndex(personNum);
			
			double[] authorDiscriminationVector = null;
			if (divVectorType.equalsIgnoreCase("PF")){
				authorDiscriminationVector = getAlignedAuthorKeywordPFVector(personNum, theIndex2, wordPosMap);
			}
			else {
				authorDiscriminationVector =  getAlignedAuthorTfidf2KeywordVector(personNum, wordPosMap);
			}


			Similarities =  getSimilarity(theIndex,nonAuthorIndex, authorDiscriminationVector, wordPosMap);


			if (!Similarities.isEmpty()){
				// Create an array containing the elements from hashtable Similarities
				VectorRanking[] rankingArray =	doRanking(Similarities);	

				displayRanking(rankingArray, papersReader);

				Similarities.clear();


			}						// end if similarities is non-empty
			else { System.out.println("0 total matching documents");}

			papersReader.close();


	}					// end main of task1c

	
	public void findLatentSemantics(String personNum, String divVectorType) throws Exception {
		String fileName = null;
		String rankingMethod = divVectorType;
		double[][] documentMatrix = null;
		
				
			Utility utility = new Utility();
			DblpData db = new DblpData();
			

			//ArrayList<TermFrequency> termVector = getAuthorKeywordVector1(personNum,searcher, papersReader, rankingMethod);
			Directory theIndextoAuthors = db.createAuthorDocumentIndex();
			IndexReader authorsReader = IndexReader.open(theIndextoAuthors);
			
			
			Directory nonAuthorIndex = db.createNonAuthoredDocsIndex(personNum);
			

			Directory theIndex = db.createAllDocumentIndex();
			IndexReader papersReader = IndexReader.open(theIndex);
			List<String> allterms =	db.getAllTermsInIndex(theIndex, "doc");
			Map<String, Integer> wordMap =  findMap(allterms);
			LinkedHashMap<String, Integer> wordPosMap = (LinkedHashMap<String, Integer>)(wordMap);
		
			
			String[] staticVocabulary = allterms.toArray(new String[allterms.size()]);
						
			//		double[] authorKeyword = Utility.getAlignedTFIDFVector(keywordVector,wordPosMap,papersReader);
			
			Map<Integer, String> docIndexMap  = new HashMap<Integer, String>();
			double[][] documentsMatrix =  getDocumentMatrix(nonAuthorIndex, allterms, docIndexMap);
			
			TermFreqVector keywordVector = getAuthorKeywordVector(personNum, theIndextoAuthors);
			double[] authorKeyword = null;
			List<Integer> paperIdsFromAuthor = db.getPaperIdsFromAuthor(personNum);
			
			if (divVectorType.equalsIgnoreCase("PCA")){
				authorKeyword = Utility.getAlignedTFIDFVector(keywordVector,wordPosMap,authorsReader);
				double[][] authorsDocumentMatrix = utility.getAuthor_DocTermMatrix(personNum,  allterms, paperIdsFromAuthor, true);
				MatLab matlab = new MatLab(); 
				double[][] hiddenSemantics = matlab.pca(authorsDocumentMatrix, 5);
				projectPCA( hiddenSemantics, documentsMatrix, authorKeyword, docIndexMap, papersReader);
						}
			else if (divVectorType.equalsIgnoreCase("SVD"))
			{
				authorKeyword  = Utility.getAlignedTFIDFVector(keywordVector, wordPosMap,authorsReader);
				double[][] authorsDocumentMatrix = utility.getAuthor_DocTermMatrix(personNum,  allterms, paperIdsFromAuthor, true);
				
				MatLab matlab = new MatLab(); 
				double[][] hiddenSemantics = matlab.svd(authorsDocumentMatrix, 5);
				projectSVD( hiddenSemantics, documentsMatrix, authorKeyword, docIndexMap, papersReader,5);
			}
			else 
				{
				authorKeyword = Utility.getAlignedTermFreqVector(keywordVector, wordPosMap);
				/* do LDA */
				double[][] authorsDocumentMatrix = utility.getAuthor_DocTermMatrix(personNum,  makeVocabularyList(keywordVector), paperIdsFromAuthor, false);
				UserCompLDA1 lda = new UserCompLDA1(); 
				List<HashMap<String,Float>> topicsList =  lda.doLDA(authorsDocumentMatrix,keywordVector.getTerms());//staticVocabulary
				
				double[][] completeTopicsMatrix = lda.buildTopicsMatrix(topicsList, allterms);
				
				/* Project LDA onto other data */
				Object[] objLSA = lda.doLatentCompare(completeTopicsMatrix, documentsMatrix, authorsDocumentMatrix, authorKeyword);	
				System.out.println("\nLatent Semantics for " + db.getAuthName(personNum)+ "\n");
		        double[] indexLSA = (double[]) objLSA[0];
				double[] distLSA = (double[]) objLSA[1];
				display(indexLSA, distLSA, docIndexMap, papersReader);
	
				}

			papersReader.close(); authorsReader.close();
	}					

	
	public List<String> makeVocabularyList(TermFreqVector tfVector){
		String[] smallVocab = tfVector.getTerms();
		List<String> vMap = new ArrayList<String>(smallVocab.length);
		for(String word : smallVocab){
			vMap.add(word);
		}
		return vMap;
	}
	


	public void display(double[] indexLSA, double[] distLSA, Map<Integer, String> docIndexMap, IndexReader allPapersReader) throws CorruptIndexException, IOException{
		
		DblpData db = new DblpData();	
		
		for (int i = 0; i < indexLSA.length; i++) {
			{System.out.println();
			System.out.printf("Paper ID: %-12s" ,docIndexMap.get((int)indexLSA[i]) ); 
 			System.out.printf("  Distance from given author: %-10.7f   %-50s" , distLSA[i], allPapersReader.document((int)indexLSA[i]).get("title"));
 			System.out.println();
			}
		} 
	}	
	
	/*
	public void projectSVD(double[][] hiddenSemantics, double[][] docsKeywordsArray, double[] authorKeyword, Map<Integer, String> docIndexMap) throws MatlabInvocationException, MatlabConnectionException{
		
		
		double[][] givenAuthKWarray = new double[1][authorKeyword.length];
		givenAuthKWarray[0] = authorKeyword;


		MatlabProxy proxy = MatLab.getProxy();
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
		
			processor.setNumericArray("VTranspose", new MatlabNumericArray(hiddenSemantics, null));
			processor.setNumericArray("givenAuthKWArray", new MatlabNumericArray(givenAuthKWarray, null));
			processor.setNumericArray("docsKeywordsArray", new MatlabNumericArray(docsKeywordsArray, null));
			
			double[][] sVDSemUserMatrix = new double[1][5];

			proxy.eval("[SVDMatrix] =  docsKeywordsArray * transpose(VTranspose)  ");
			proxy.eval("[SVDUserMatrix] = VTranspose * transpose(givenAuthKWArray) ");

			
			
			Object[] svdObj = proxy.returningEval("SVDUserMatrix(1,:)", 1);
			sVDSemUserMatrix[0] = (double[]) svdObj[0];
			System.out.println("Top 20 Similar Papers - Comparing Papers to the User's Semantics SVD");
			System.out.println("****************************************************");
		//	processor.setNumericArray("inputCorpusMatrixSVD", new MatlabNumericArray(sVDKeywordTop5Matrix, null));
		//	processor.setNumericArray("userMatrixSVD", new MatlabNumericArray(sVDSemUserMatrix, null));
			Object[] objSVD = new Object[2];
			objSVD = proxy.returningEval("knnsearch( SVDMatrix, transpose(SVDUserMatrix),'k', 21,'Distance','cosine')",2);
			double[] indexSVD = (double[]) objSVD[0];
			double[] distSVD = (double[]) objSVD[1];
			
			display(indexSVD, distSVD,  docIndexMap);
				
				

		
	}

*/

	public void projectSVD(double[][] hiddenSemantics, double[][] docsKeywordsArray, double[] authorKeyword, Map<Integer, String> docIndexMap, IndexReader allPapersReader,int numOfLatents) throws MatlabInvocationException, MatlabConnectionException, CorruptIndexException, IOException{

		double[][] givenAuthKWarray = new double[1][authorKeyword.length];
		givenAuthKWarray[0] = authorKeyword;
		double[][] svdoutput = new double[hiddenSemantics.length][hiddenSemantics[0].length];
		double[][] svdoutput2 = new double[hiddenSemantics[0].length][hiddenSemantics.length];

		MatlabProxy proxy = MatLab.getProxy();
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);

		/* need V so transpose V' back to V*/
		for (int j = 0; j < hiddenSemantics[0].length; j++) 
		{
			for (int k = 0; k < hiddenSemantics.length; k++) 
			{
				svdoutput2[j][k] = hiddenSemantics[k][j];
			}
		}
		processor.setNumericArray("svdoutput2", new MatlabNumericArray(svdoutput2, null));
		processor.setNumericArray("givenAuthKWArray", new MatlabNumericArray(givenAuthKWarray, null));
		processor.setNumericArray("docsKeywordsArray", new MatlabNumericArray(docsKeywordsArray, null));


		proxy.eval("[SVDMatrix2] =  docsKeywordsArray * svdoutput2(:,1:"+ numOfLatents + ") ");
		proxy.eval("[SVDUserMatrix2] = givenAuthKWArray *svdoutput2(:,1:"+ numOfLatents + ") ");

		System.out.println("Top 20 Similar Papers - Comparing Papers to the User's Semantics as determined by SVD");
		System.out.println("*************************************************************************************");


		Object[] objSVD1 = proxy.returningEval("knnsearch( SVDMatrix2, SVDUserMatrix2,'k', 21,'Distance','cosine')",2);

		double[] indexSVD2 = (double[]) objSVD1[0];
		double[] distSVD2 = (double[]) objSVD1[1];

		display(indexSVD2, distSVD2,  docIndexMap,  allPapersReader);

	}

	public void projectPCA(double[][] hiddenSemantics, double[][] docsKeywordsArray, double[] authorKeyword, Map<Integer, String> docIndexMap,  IndexReader allPapersReader) throws MatlabInvocationException, MatlabConnectionException, CorruptIndexException, IOException{

		double[][] allSemArray = new double[hiddenSemantics.length][hiddenSemantics[0].length];
		double[][] givenAuthKWarray = new double[1][authorKeyword.length];
		givenAuthKWarray[0] = authorKeyword;
		double[][] pcaoutput = new double[hiddenSemantics[0].length][hiddenSemantics.length];


		for (int j = 0; j < hiddenSemantics[0].length; j++) {
			for (int k = 0; k < hiddenSemantics.length; k++) {
				pcaoutput[j][k] = hiddenSemantics[k][j];
			}
		}

		MatlabProxy proxy = MatLab.getProxy();
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);

		processor.setNumericArray("P", new MatlabNumericArray(pcaoutput, null));
		processor.setNumericArray("givenAuthKWArray", new MatlabNumericArray(givenAuthKWarray, null));
		processor.setNumericArray("docsKeywordsArray", new MatlabNumericArray(docsKeywordsArray, null));

		double[][] pcaSemUserMatrix = new double[1][hiddenSemantics.length];
		processor.setNumericArray("PMatrix", new MatlabNumericArray(allSemArray, null));
		proxy.eval("[PMatrix]= transpose(P)");
		proxy.eval("[PCAMatrix] =  docsKeywordsArray * PMatrix(:,1:5) ");
		proxy.eval("[PCAUserMatrix] = givenAuthKWArray * PMatrix(:,1:5)");


		Object[] svdObj = proxy.returningEval("PCAUserMatrix(1,:)", 1);
		pcaSemUserMatrix[0] = (double[]) svdObj[0];

		System.out.println("Top 20 Similar Papers - Comparing Papers to the User's Semantics (PCA)");
		System.out.println("**********************************************************************");
		Object[] objSVD = new Object[2];
		objSVD = proxy.returningEval("knnsearch( PCAMatrix, PCAUserMatrix,'k', 21,'Distance','cosine')",2);
		double[] indexSVD = (double[]) objSVD[0];
		double[] distSVD = (double[]) objSVD[1];

		display(indexSVD, distSVD,  docIndexMap , allPapersReader);
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

	public double[] getAlignedAuthorTfidf2KeywordVector(String personNum,  LinkedHashMap<String, Integer> wordPosMap) throws Exception{
		
		double[] alignedVector = new double[wordPosMap.keySet().size()];
		DblpData db = new DblpData();
		
		 Map<String, Double> authorTFIDF2 = db.getTFIDF2Vector(personNum);
		 
		 for (Map.Entry<String, Double> tfidf2Entry : authorTFIDF2.entrySet()){
			 String term = tfidf2Entry.getKey();
			 int termIndex = wordPosMap.get(term);
			 alignedVector[termIndex] = tfidf2Entry.getValue();
			  }
		 return alignedVector; 
		 
	}
	
	public TermFreqVector getAuthorKeywordVector(String authorId,Directory luceneIndexDir) throws CorruptIndexException, IOException
	{	DblpData db = new DblpData();
	 Map<String, TermFreqVector>  allAuthors = db.getAuthorTermFrequencies(luceneIndexDir);
	 TermFreqVector tfv = allAuthors.get(authorId);
	 return tfv;
	}
/*
 *  replacement method that creates a single author key word vector by accessing db directly
 */

	public Map<String, Float> getAuthorKeywordVector2(String authorId,Directory indexDirectory,String rankMethod) throws CorruptIndexException, IOException{
		CharArraySet stopWordsCharArrSet;
		TokenStream docStream;
		TokenStream keywords;
		IndexReader reader3 = IndexReader.open(indexDirectory);

		int noOfDocs = reader3.numDocs();
		Map<String,Float> termFreq = new HashMap<String, Float>();
		Map<String,Float> termFinalFreq = new HashMap<String, Float>();
		Map<String,Float> idfGivenAuthMap = new HashMap<String, Float>();

		List<Map<String, Float>> listFreqMaps = new ArrayList<Map<String, Float>>();
		List<Map<String, Float>> listTFIDFMaps = new ArrayList<Map<String, Float>>();
		
		DblpData db = new DblpData();
		Utility utilityObj = new Utility();
		List<String> abstracts =  db.getAbstractsByAuthor(authorId);
		String allRowData = db.combineAbstracts(abstracts);
		
		//Creating the Character Array Set from the list of stop words
		stopWordsCharArrSet = new CharArraySet(Version.LUCENE_36, utilityObj.createStopWordsSet(), true);
		
		//Creating a token stream from the abstract got from the DB for the given paperId
			docStream = new StandardTokenizer(Version.LUCENE_36, new StringReader(allRowData));

		//Creating the Keywords of a given abstract
			keywords = new StopFilter(Version.LUCENE_36, docStream ,stopWordsCharArrSet);

			termFreq = utilityObj.createTF(keywords, allRowData);
		
		
		//Calling the method createTFIDF to create TF-IDF vector output
		Map<String,Float> idfMap = utilityObj.createTFIDF(noOfDocs, indexDirectory, termFreq,"TF-IDF");
		
		return idfMap;
		
			 
			}
		
	// placeholder	
	
	
	
	

		
		private VectorRanking[] doRanking(Hashtable<Integer,VectorRanking> Similarities){
			// Create an array containing the elements from hashtable Similarities

			Collection<VectorRanking> ranking = Similarities.values();
			VectorRanking[] rankingArray = (VectorRanking[])ranking.toArray(new VectorRanking[Similarities.size()]);
			Arrays.sort(rankingArray, Collections.reverseOrder());
			return rankingArray;
		}	
	
		

		/*
		 * Helper method displays ranking of observations/documents
		 */
		private  void displayRanking(VectorRanking[] rankingArray, IndexReader papersReader) throws IOException{
		System.out.println("Similar documents");
		System.out.println();
		System.out.printf(" \t "  + "PaperID"  + "\t" + " CosineSimilarity"); 
		System.out.println();

			final int HITS_PER_PAGE = 20;
			; 
			int end = Math.min(rankingArray.length, HITS_PER_PAGE);
			for (int i = 0; i < end; i++) {
				int docNum = rankingArray[i].documentID;

				System.out.printf(" RANK " + "\t" +(i + 1) + " " + papersReader.document(docNum).get("paperid")+ "\t" + " SCORE %10.7f",
						rankingArray[i].howSimilar);
				System.out.println(" "+ papersReader.document(docNum).get("title"));

			}
		}
		
		

		public Map<Integer, TermFreqVector> getDocTermFrequencies(Directory luceneIndexDir){
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
	
	public Map<String, Integer> findMap(List<String> completeKeywordList){
		int termIdx = 0;
		Map<String, Integer> allKeywordsPosMap = new LinkedHashMap<String, Integer>();
		for(String kw:completeKeywordList){
			allKeywordsPosMap.put(kw, termIdx++);
		}
		return allKeywordsPosMap;
	}
	/*	
	public double[] getAlignedTFIDF2Vector (TermFreqVector atfv, Map<String, Integer> wordPosMap, Directory LuceneIndexDir, Directory notAllAuthorsIndex, String authorID){
		IndexReader readerNotAuthor  = IndexReader.open(notAllAuthorsIndex);
		IndexReader readerAllAuthors = IndexReader.open(LuceneIndexDir);
		Integer numDocs = readerAllAuthors.numDocs() - readerNotAuthor.numDocs();
		
	}
	*/
	
	public double[][] getDocumentMatrix(Directory luceneIndexDir, List<String> completeKeywordList, Map<Integer, String> docIndexMap) throws Exception{
		Utility util = new Utility();
		DblpData db = new DblpData();
		SimilarityAnalyzer sv = new SimilarityAnalyzer();
		

		IndexReader reader = IndexReader.open(luceneIndexDir);
		double[][] documentMatrix = new double[reader.maxDoc()][completeKeywordList.size()];
		
		int termIdx = 0;
		Map<String, Integer> allKeywordsPosMap = new LinkedHashMap<String, Integer>();
		for(String kw:completeKeywordList){
			allKeywordsPosMap.put(kw, termIdx++);
		}

			try {
					
			for (int i = 0; i < reader.maxDoc(); i++) {
				TermFreqVector tfv = reader.getTermFreqVector(i, "doc");
				double[] doc1 = util.getAlignedTFIDFVector(tfv,allKeywordsPosMap,reader );
				docIndexMap.put(i, reader.document(i).get("paperid"));
				documentMatrix[i] = doc1;
			}
			reader.close();
			 
		} catch (CorruptIndexException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return documentMatrix;
	}
			
	public Hashtable<Integer, VectorRanking> getSimilarity(	Directory luceneIndexDir, Directory nonAuthorIndexDir,double[] authorskeywords,
															Map<String, Integer> allKeywordsPosMap)throws Exception {
		Utility util = new Utility();
		DblpData db = new DblpData();
		IndexReader nonAuthorReader = IndexReader.open(nonAuthorIndexDir);
		IndexReader allReader = IndexReader.open(luceneIndexDir);
		Hashtable<Integer, VectorRanking> Similarities = new Hashtable<Integer, VectorRanking>();
		
		try {

			for (int i = 0; i < nonAuthorReader.numDocs(); i++) {
				TermFreqVector tfv = nonAuthorReader.getTermFreqVector(i, "doc");
				// double[] doc1 = util.getAlignedTermFreqVector(tfv,allKeywordsPosMap);
				double[] doc1 = util.getAlignedTFIDFVector(tfv,	allKeywordsPosMap, allReader);
				double cosineSim = util.cosineSimilarity(authorskeywords, doc1);
				
				VectorRanking vr = new VectorRanking();
				vr.howSimilar = cosineSim;
				vr.documentID = i;
				Similarities.put(i, vr);
			}
			allReader.close();

		} catch (CorruptIndexException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return Similarities;
	}

			public List<HashMap<String,Float>> doLDAAuthor(TermFreqVector authorKW, double[][] docKeywordCorpusMatrix,String[] staticVocabulary) throws Exception{

				MatlabProxy proxy = MatLab.getProxy();
				LDAPrep ldaInputs = new LDAPrep();

				ldaInputs.doLDAPrepFullMatrix(docKeywordCorpusMatrix);
				//ldaInputs.makeDictionaryFile(staticVocabulary);

				double[][] WS = new double[1][ldaInputs.WS.length];
				WS[0] = ldaInputs.WS;
				double[][] DS = new double[1][ldaInputs.DS.length];
				DS[0] = ldaInputs.DS;

				double[] WS3 = ldaInputs.WS;
				/* lose precision this way */    
				//	proxy.setVariable("WS3",WS3);
				// proxy.setVariable("DS",DS);
				String currentPath = task1aLDA.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		                currentPath = currentPath.replaceFirst("Phase2.jar", "");
		                currentPath = currentPath.replaceFirst("/", "");
				proxy.eval("cd "+currentPath);


				MatlabTypeConverter processor = new MatlabTypeConverter(proxy);

				processor.setNumericArray("WS", new MatlabNumericArray(WS, null));
				processor.setNumericArray("DS", new MatlabNumericArray(DS, null));

				/* set the number of latent semantics to retrieve */
				double T = 5.0;
				proxy.setVariable("T", 5);
				proxy.setVariable("WO", staticVocabulary);
				proxy.eval("[WPALL,DPALL,ZALL] = LDA1(WS,DS,T,WO)");

				double[][] WP = processor.getNumericArray("WPALL").getRealArray2D();
				double[][] DP = processor.getNumericArray("DPALL").getRealArray2D();
				double[][] Z = processor.getNumericArray("ZALL").getRealArray2D();

				//Do processing of Topics probability Matrix generated by matlab in text file to display top k topics

				/* numImportantTopics = T,  numRelevantWords = 7 + 1 for header */
				String filename = currentPath + "/" + "topics.txt";
				//				 List<KeywordConfig>[] topicsconfigList = ldaInputs.readLDATopics( filename,  5, 8);
				List<KeywordConfig>[] topicsconfigList = ldaInputs.readPrintTopics(filename, 5,10);
				
				/* for consistency convert to list of HashMap */
				List<HashMap<String,Float>> topicsList = new ArrayList<HashMap<String,Float>>();
				for(List<KeywordConfig> aTopic : topicsconfigList){
					HashMap<String,Float> topicFreq = new HashMap<String,Float>(aTopic.size());
						for(KeywordConfig term : aTopic){
							String item = term.getKeyword();
							Float  freq = term.getWeightedFreq();
							if (authorKW.indexOf(item)!= -1)
								topicFreq.put(item,freq);
					}
					topicsList.add(topicFreq);
				}

				return topicsList;

			}

			
		
}




