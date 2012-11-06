package edu.mwdb.project;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.extensions.MatlabNumericArray;
import matlabcontrol.extensions.MatlabTypeConverter;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class UserCompPCASVD {

	/**
	 * @param args
	 */
	public void computePCA_SVD(int personId, String model) {
		// TODO Auto-generated method stub
		Utility utilityObj = new Utility();
		try
		{
			Connection con = utilityObj.getDBConnection();
			
			// Get the Id's of all the authors in the DB.
			System.out.println("Getting the list of all authors from the corpus.");
			Statement stmtUserdIds = con.createStatement();
			String query_authorid = "select distinct a.personid from authors a join writtenby w join papers p ON a.personid = w.personid and w.paperid = p.paperid and p.abstract !=\"\"";
			ResultSet authorsIdsFromDB = stmtUserdIds.executeQuery(query_authorid);
			List<Integer> userIdList = new ArrayList<Integer>();
			while (authorsIdsFromDB.next())
			{
				userIdList.add(authorsIdsFromDB.getInt("personid"));
			}
			System.out.println("All authors list completed. Number of Authors: " + userIdList.size());

			// To know the count of abstracts in the DB and store it in noOfDocs.
			Statement stmt1 = con.createStatement();
			int noOfDocs = 0;
			String query_alldocs_count = "SELECT count(*) AS count FROM papers WHERE abstract != \"\"";
			ResultSet rs1 = stmt1.executeQuery(query_alldocs_count);
			while (rs1.next()) 
			{
				noOfDocs = rs1.getInt("count");
			}


			// **********************************************************************************

			//Extract the abstracts from the DB
			Statement stmt2 = con.createStatement();
			String query_alldocs = "SELECT abstract FROM papers WHERE abstract != \"\"";
			ResultSet rs2 = stmt2.executeQuery(query_alldocs);

			// Creation of a Index Directory.
			StandardAnalyzer docAnalyzer = new StandardAnalyzer(Version.LUCENE_36);
			IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_36,docAnalyzer);
			Directory indexDirectory = new RAMDirectory();
			IndexWriter indexWr = new IndexWriter(indexDirectory, indexConfig);

			String doc="";

			// Adding a field 'doc' from the abstract to create an indexed document.
			while (rs2.next()) 
			{
				doc = rs2.getString("abstract");
				Document document = new Document();
				document.add(new Field("doc", doc, Field.Store.YES,Field.Index.ANALYZED));
				indexWr.addDocument(document);
				indexWr.commit();
			}


			// **********************************************************************************

			int userId = 0;

			// List that consists of all the documents for a given author.
			String rowData = "";

			CharArraySet stopWordsCharArrSet;
			TokenStream docStream;
			TokenStream keywords;

			Map<String,Float> termFreq = new HashMap<String, Float>();
			Map<String,Float> termFinalFreq = new HashMap<String, Float>();
			Map<String,Float> idfGivenAuthMap = new HashMap<String, Float>();

			List<Map<String, Float>> listFreqMaps = new ArrayList<Map<String, Float>>();
			List<Map<String, Float>> listTFIDFMaps = new ArrayList<Map<String, Float>>();

			// List of Lists - where each list stores the keywords of the respective documents.
			List<List<String>> docKeywords = new ArrayList<List<String>>();

			Statement stmtauthorPapers = con.createStatement();
			Iterator<Integer> itr = userIdList.iterator();
			while (itr.hasNext()) 
			{
				userId = itr.next();
				// To get the list of papers written by the given author.
				String query_authoridPapers = 
						"select p.abstract from papers p join " +  
								"(select distinct w.paperid from " + 
								"authors a join writtenby w where a.personid = w.personid and a.personid = " + userId  + 
								" order by paperid) T1 on p.paperid = T1.paperid where p.abstract != \"\"";

				ResultSet rs = stmtauthorPapers.executeQuery(query_authoridPapers);
				while (rs.next())
				{
					rowData += rs.getString("abstract");
				}

				//Creating the Character Array Set from the list of stop words
				stopWordsCharArrSet = new CharArraySet(Version.LUCENE_36, utilityObj.createStopWordsSet(), true);

				//Creating a token stream from the abstract got from the DB for the given paperId
				docStream = new StandardTokenizer(Version.LUCENE_36, new StringReader(rowData));

				//Creating the Keywords of a given abstract
				keywords = new StopFilter(Version.LUCENE_36, docStream ,stopWordsCharArrSet);

				termFreq = utilityObj.createTF(keywords, rowData);
				listFreqMaps.add(termFreq);

				List<String> keywordsList = new ArrayList<String>();
				for(Map.Entry<String, Float> k : termFreq.entrySet())
				{
					keywordsList.add(k.getKey());
				}
				docKeywords.add(keywordsList);

				//Calling the method createTFIDF to create TF-IDF vector output
				Map<String,Float> idfMap = utilityObj.createTFIDF(noOfDocs,indexDirectory, termFreq,"TF-IDF");
				listTFIDFMaps.add(idfMap);

				if(userId == personId)
				{
					for(Map.Entry<String, Float> keys : termFreq.entrySet())
					{
						termFinalFreq.put(keys.getKey(),keys.getValue());
					}
					idfGivenAuthMap = idfMap;
				}
				rowData = "";
			}

			int rowSize = userIdList.size();
			int columnSize = termFinalFreq.size();

			// Input Matrices
			double docKeywordCorpusMatrix[][] = new double[rowSize][columnSize];
			double[][] givenauthKeywordTfIdfMatrix = new double[1][columnSize];

			System.out.println("Building givenauthKeywordTfIdfMatrix");
			// Build the givenauthKeywordTfIdfMatrix i/p matrix
			for(int j=0;j<columnSize;j++)
			{
				for(Map.Entry<String, Float> k: termFinalFreq.entrySet())
				{
					givenauthKeywordTfIdfMatrix[0][j] = idfGivenAuthMap.get(k.getKey());
					j++;
				}
			}

			System.out.println("BUilding docKeywordCorpusMatrix");
			// Build the input Corpus matrix - docKeywordCorpusMatrix
			for(int row=0;row<rowSize;row++)
			{
				List<String> tempList = docKeywords.get(row);
				for(int column=0;column<columnSize;column++)
				{
					for(Map.Entry<String, Float> k: termFinalFreq.entrySet())
					{
						for(int i=0;i<tempList.size();i++)
						{
							if(k.getKey().equals(tempList.get(i)))
							{
								docKeywordCorpusMatrix[row][column] = listTFIDFMaps.get(row).get(k.getKey());
								break;
							}
							docKeywordCorpusMatrix[row][column] = 0;
						}
						if (column<columnSize-1)
							column++;
					}
				}
			}

			System.out.println("Building docGivenAuthKeywordCorpusMatrix");
			// Build the docGivenAuthKeywordCorpusMatrix
			// List to store the author abstracts
			List<String> givenAuthorPapersList = new ArrayList<String>();
			// To get the list of papers written by the given author.
			String query_papersByGivenAuthor = 
					"select p.abstract from papers p join " +  
							"(select distinct w.paperid from " + 
							"authors a join writtenby w where a.personid = w.personid and a.personid = " + personId  + 
							" order by paperid) T1 on p.paperid = T1.paperid where p.abstract != \"\"";

			Statement stmt_givenAuthorPapers = con.createStatement();
			ResultSet givenAuthorPapers = stmt_givenAuthorPapers.executeQuery(query_papersByGivenAuthor);
			while (givenAuthorPapers.next())
			{
				givenAuthorPapersList.add(givenAuthorPapers.getString("abstract"));
			}
			// List of Lists - where each list stores the keywords of the respective documents.
			List<List<String>> givenAuthDocKeywords = new ArrayList<List<String>>();
			// List of Maps - where each list stores the IDF map of the respective documents.
			List<Map<String,Float>> docIdfMapList = new ArrayList<Map<String,Float>>();

			for (int i=0;i<givenAuthorPapersList.size();i++)
			{
				//Creating the Character Array Set from the list of stop words
				stopWordsCharArrSet = new CharArraySet(Version.LUCENE_36, utilityObj.createStopWordsSet(), true);

				//Creating a token stream from the abstract got from the DB for the given paperId
				docStream = new StandardTokenizer(Version.LUCENE_36, new StringReader(givenAuthorPapersList.get(i)));

				//Creating the Keywords of a given abstract
				keywords = new StopFilter(Version.LUCENE_36, docStream ,stopWordsCharArrSet);

				termFreq = utilityObj.createNewTF(keywords, givenAuthorPapersList.get(i));

				List<String> keywordsList = new ArrayList<String>();
				for(Map.Entry<String, Float> k : termFreq.entrySet())
				{
					keywordsList.add(k.getKey());
				}
				givenAuthDocKeywords.add(keywordsList);

				Map<String,Float> idfMap = utilityObj.createTFIDF(noOfDocs,indexDirectory, termFreq,"TF-IDF");
				docIdfMapList.add(idfMap);
			}

			int givenAuthRowSize = givenAuthorPapersList.size();
			int givenAuthColumnSize = termFinalFreq.size();

			double docGivenAuthKeywordCorpusMatrix[][] = new double[givenAuthRowSize][givenAuthColumnSize];

			// Build the input Corpus matrix.
			for(int row=0;row<givenAuthRowSize;row++)
			{
				List<String> tempList = givenAuthDocKeywords.get(row);
				for(int column=0;column<givenAuthColumnSize;column++)
				{
					for(Map.Entry<String, Float> k: termFinalFreq.entrySet())
					{
						for(int i=0;i<tempList.size();i++)
						{
							if(k.getKey().equals(tempList.get(i)))
							{
								docGivenAuthKeywordCorpusMatrix[row][column] = docIdfMapList.get(row).get(k.getKey());
								break;
							}
							docGivenAuthKeywordCorpusMatrix[row][column] = 0;
						}
						if (column<columnSize-1)
							column++;
					}
				}
			}

			System.out.println("All Matrices completeted..!!");
			HashMap<Integer, String> authNamePersonIdList = new HashMap<Integer, String>();

			Statement statement = con.createStatement();
			ResultSet resultSet = statement.executeQuery("select distinct(a.personid),a.name from writtenby w, papers p,authors a where a.personid = w.personid" +
					" and w.paperid=p.paperid and p.abstract != \"\"");
			if (resultSet.next()) 
			{
				int personid = resultSet.getInt("personid");
				String name = resultSet.getString("name");
				authNamePersonIdList.put(personid, name);
				while (resultSet.next()) 
				{
					personid = resultSet.getInt("personid");
					name = resultSet.getString("name");
					authNamePersonIdList.put(personid, name);
				}
			}

			MatlabProxyFactory factory = new MatlabProxyFactory();
			MatlabProxy proxy = factory.getProxy();
			MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
			
			processor.setNumericArray("docKeywordArray", new MatlabNumericArray(docKeywordCorpusMatrix, null));
			processor.setNumericArray("givenAuthKWArray", new MatlabNumericArray(givenauthKeywordTfIdfMatrix, null));
			processor.setNumericArray("docGivenKWarray", new MatlabNumericArray(docGivenAuthKeywordCorpusMatrix, null));

			// PCA - Start
			if(model.equalsIgnoreCase("PCA"))
			{
				System.out.println("Starting to compute similar users using top 5 semantics PCA");
				proxy.eval("[princicomp,score]=princomp(docGivenKWarray);");
				double[][] pCAKeywordTop5Matrix = new double[userIdList.size()][5];
				double[][] pCASemUserMatrix = new double[1][5];
				double[][] matrixTemp = new double[userIdList.size()][userIdList.size()];
				proxy.eval("[PCAMatrix] = docKeywordArray * princicomp(:,1:5)");
				proxy.eval("[PCAUserMatrix] = givenAuthKWArray * princicomp(:,1:5)");

				for (int i = 0; i < 5; i++) 
				{
					Object[] pCAObject = proxy.returningEval("PCAMatrix(:,"+ (i + 1) + ")", 1);
					matrixTemp[i] = (double[]) pCAObject[0];
				}
				for (int j = 0; j < userIdList.size(); j++) 
				{
					for (int k = 0; k < 5; k++)
					{
						pCAKeywordTop5Matrix[j][k] = matrixTemp[k][j];
					}
				}

				Object[] pCAObject = proxy.returningEval("PCAUserMatrix(1,:)", 1);
				pCASemUserMatrix[0] = (double[]) pCAObject[0];
				System.out.println("Top 10 Similar Users - Comparing Users Semantics PCA");
				System.out.println("*****************************************************");
				processor.setNumericArray("inputCorpusMatrixPCA", new MatlabNumericArray(pCAKeywordTop5Matrix, null));
				processor.setNumericArray("userMatrixPCA", new MatlabNumericArray(pCASemUserMatrix, null));
				Object[] objPCA = new Object[2];
				objPCA = proxy.returningEval("knnsearch( inputCorpusMatrixPCA, userMatrixPCA,'k', 11,'Distance','cosine')",2);	
				double[] indexPCA = (double[]) objPCA[0];
				double[] distPCA = (double[]) objPCA[1];
				if (authNamePersonIdList.get(userIdList.get((int) indexPCA[0] - 1)) == null)
				{
					for (int i = 0; i < 10; i++) 
					{
						System.out.println("Author Name: " + userIdList.get((int) indexPCA[i] - 1) + "\t\t" + "Distance from given author: " + distPCA[i]);
					}
				} 
				else 
				{
					for (int i = 1; i < 11; i++) 
					{
						System.out.println("Author Name: " + authNamePersonIdList.get(userIdList.get((int) indexPCA[i] - 1)) + "\t\t" + "Distance from given author: " + distPCA[i]);
					}
				}
			}
			// PCA - End

			//SVD - Start
			if(model.equalsIgnoreCase("SVD"))
			{
				System.out.println("Starting to compute similar users using top 5 semantics SVD");
				proxy.eval("[U,S,V]=svd(docGivenKWarray);");
				double[][] sVDKeywordTop5Matrix = new double[userIdList.size()][5];
				double[][] sVDSemUserMatrix = new double[1][5];
				double[][] matrixTemp = new double[userIdList.size()][userIdList.size()];
				proxy.eval("[SVDMatrix] = docKeywordArray * transpose(V(1:5,:))");
				proxy.eval("[SVDUserMatrix] = givenAuthKWArray * transpose(V(1:5,:))");

				for (int i = 0; i < 5; i++) 
				{
					Object[] sVDObject = proxy.returningEval("SVDMatrix(:,"+ (i + 1) + ")", 1);
					matrixTemp[i] = (double[]) sVDObject[0];
				}
				for (int j = 0; j < userIdList.size(); j++) 
				{
					for (int k = 0; k < 5; k++) 
					{
						sVDKeywordTop5Matrix[j][k] = matrixTemp[k][j];
					}
				}

				Object[] svdObj = proxy.returningEval("SVDUserMatrix(1,:)", 1);
				sVDSemUserMatrix[0] = (double[]) svdObj[0];
				System.out.println("Top 10 Similar Users - Comparing Users Semantics SVD");
				System.out.println("****************************************************");
				processor.setNumericArray("inputCorpusMatrixSVD", new MatlabNumericArray(sVDKeywordTop5Matrix, null));
				processor.setNumericArray("userMatrixSVD", new MatlabNumericArray(sVDSemUserMatrix, null));
				Object[] objSVD = new Object[2];
				objSVD = proxy.returningEval("knnsearch( inputCorpusMatrixSVD, userMatrixSVD,'k', 11,'Distance','cosine')",2);
				double[] indexSVD = (double[]) objSVD[0];
				double[] distSVD = (double[]) objSVD[1];
				
				if (authNamePersonIdList.get(userIdList.get((int) indexSVD[0] - 1)) == null) 
				{
					for (int i = 0; i < 10; i++) 
					{
						System.out.println("Author Name: " + userIdList.get((int) indexSVD[i] - 1) + "\t\t" + "Distance from given author: " + distSVD[i]);
					}
				} 
				else 
				{
					for (int j = 1; j < 11; j++) 
					{
						System.out.println("Author Name: " + authNamePersonIdList.get(userIdList.get((int) indexSVD[j] - 1)) + "\t\t" + "Distance from given author: " + distSVD[j]);
					}
				}
			}
			//SVD - End

			proxy.disconnect();
			con.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
