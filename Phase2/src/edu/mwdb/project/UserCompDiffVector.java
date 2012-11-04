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

public class UserCompDiffVector {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Utility utilityObj = new Utility();
		try
		{
			Connection con = utilityObj.getDBConnection();
			int personId = 1636579;
			
			// Get the Id's of all the authors in the DB.
			Statement stmtUserdIds = con.createStatement();
			String query_authorList = "select distinct a.personid from authors a join writtenby w join papers p ON a.personid = w.personid and w.paperid = p.paperid and p.abstract !=\"\"";
			ResultSet authorsIdsFromDB = stmtUserdIds.executeQuery(query_authorList);
			List<Integer> userIdList = new ArrayList<Integer>();
			while (authorsIdsFromDB.next())
			{
				userIdList.add(authorsIdsFromDB.getInt("personid"));
			}
			
			int userId = 0;
			// Creation of a Index Directory.
			StandardAnalyzer docAnalyzer = new StandardAnalyzer(Version.LUCENE_36);
			IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_36,docAnalyzer);
			Directory indexDirectory = new RAMDirectory();
			IndexWriter indexWr = new IndexWriter(indexDirectory, indexConfig);
			String doc="";
			Statement stmt1 = con.createStatement();
			Statement stmt2 = con.createStatement();
			
			// List that consists of all the documents for a given author.
			String rowData = "";
			
			int noOfWords = 0;
			
			// List to store co-author abstracts
			List<String> rowData1 = new ArrayList<String>();
			
			Map<String,Float> termFreq = new HashMap<String, Float>();
			Map<String,Float> termFinalFreq = new HashMap<String, Float>();
			Map<String,Float> idfGivenAuthMap = new HashMap<String, Float>();

			List<Map<String, Float>> listFreqMaps = new ArrayList<Map<String, Float>>();
			List<Map<String, Float>> listTFIDFMaps = new ArrayList<Map<String, Float>>();

			// List of Lists - where each list stores the keywords of the respective documents.
			List<List<String>> docKeywords = new ArrayList<List<String>>();
			
			Iterator<Integer> itr = userIdList.iterator();
			CharArraySet stopWordsCharArrSet = new CharArraySet(Version.LUCENE_36, utilityObj.createStopWordsSet(), true);

			while (itr.hasNext()) 
			{
				userId = itr.next();
				System.out.println("UserId: *********************************** : " + userId);
				
				String query_authorid = 
						"select p.abstract from papers p join " +  
								"(select distinct w.paperid from " + 
								"authors a join writtenby w where a.personid = w.personid and a.personid = " + personId  + 
								" order by paperid) T1 on p.paperid = T1.paperid where p.abstract != \"\"";

				ResultSet rs = stmt1.executeQuery(query_authorid);
				while (rs.next())
				{
					rowData += rs.getString("abstract");
				}

				String query_coauthor_and_self = 
						"SELECT distinct abstract from papers join " +
								"(select distinct(writby.paperid) from writtenby writby join coauthorswpaper coauth on writby.personid = coauth.personid2 " +
								"where coauth.personid1 = " + personId + " )T " +
								" ON papers.paperid = T.paperid where abstract!=\"\"";

				ResultSet rs1 = stmt2.executeQuery(query_coauthor_and_self);
				
				while (rs1.next())
				{
					rowData1.add(rs1.getString("abstract"));
					// Adding a field 'doc' from the abstract to create an indexed document.
					doc = rs1.getString("abstract");
					Document document = new Document();
					document.add(new Field("doc", doc, Field.Store.YES,Field.Index.ANALYZED));
					indexWr.addDocument(document);
					indexWr.commit();
				}
				TokenStream docStream;
				TokenStream keywords;
				
				String[] rowDataArr = rowData.split("[ ]+");
				noOfWords += rowDataArr.length;

				//Creating the Character Array Set from the list of stop words
				//Creating a token stream from the abstract got from the DB for the given paperId
				docStream = new StandardTokenizer(Version.LUCENE_36, new StringReader(rowData));
				//Creating the Keywords of a given abstract
				keywords = new StopFilter(Version.LUCENE_36, docStream ,stopWordsCharArrSet);
				
				termFreq = utilityObj.createNewTF(keywords, rowData);
				listFreqMaps.add(termFreq);

				List<String> keywordsList = new ArrayList<String>();
				for(Map.Entry<String, Float> k : termFreq.entrySet())
				{
					keywordsList.add(k.getKey());
				}
				docKeywords.add(keywordsList);

				//Calling the method createTFIDF to create TF-IDF vector output
				Map<String,Float> idfMap = utilityObj.createTFIDF(rowData1.size(),indexDirectory, termFreq);
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

			double[][] docDVCorpusMatrix = new double[rowSize][columnSize];
			double[][] givenAuthMatrix = new double[1][columnSize];

			for(int j=0;j<columnSize;j++)
			{
				for(Map.Entry<String, Float> k: termFinalFreq.entrySet())
				{
					givenAuthMatrix[0][j] = idfGivenAuthMap.get(k.getKey());
					j++;
				}
			}

			// Print the givenauthKeyword i/p matrix.
			/*for(int row=0;row<1;row++)
			{
				for(int column=0;column<columnSize;column++)
				{
					System.out.print(givenauthKeywordTfMatrix[row][column] + "\t");
				}
				System.out.println();
			}*/
			
			// Build the input Corpus matrix.
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
								docDVCorpusMatrix[row][column] = listTFIDFMaps.get(row).get(k.getKey());
								break;
							}
							docDVCorpusMatrix[row][column] = 0;
						}
						if (column<columnSize-1)
							column++;
					}
				}
			}
			
			// Print the i/p Corpus matrix.
			/*for(int row=0;row<rowSize;row++)
			{
				for(int column=0;column<columnSize;column++)
				{
					out.write(docKeywordCorpusMatrix[row][column] + "\t");
				}
				out.write("\n");
			}
			out.close();*/
			
			System.out.println("**************************************************************");
			System.out.println("Top 10 Similar Users - Comparing Users Differentiation Vectors");
			System.out.println("**************************************************************");

			HashMap<Integer, String> authNamePersonIdList = new HashMap<Integer, String>();

			Statement statement = con.createStatement();
			ResultSet resultSet = statement.executeQuery("select distinct(a.personid),a.name from dblp.writtenby w,dblp.papers b ,dblp.authors a "
													   + "where a.personid = w.personid and w.paperid=b.paperid and b.abstract != \"\"");
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
			processor.setNumericArray("inputCorpusMatrix", new MatlabNumericArray(docDVCorpusMatrix, null));
			processor.setNumericArray("userMatrix", new MatlabNumericArray(givenAuthMatrix, null));
			Object[] obj = new Object[2];
			obj = proxy.returningEval("knnsearch( inputCorpusMatrix, userMatrix,'k', 11,'Distance','cosine')",2);
			proxy.disconnect();

			double[] iDistX = (double[]) obj[0];
			double[] distance = (double[]) obj[1];
			// obj[1] = proxy.getVariable("D");
			if (authNamePersonIdList.get(userIdList.get((int) iDistX[0] - 1)) == null) 
			{
				for (int j = 0; j < 10; j++) 
				{
					System.out.println(userIdList.get((int) iDistX[j] - 1) + "\t"+ distance[j]);
				}
			} 
			else 
			{
				for (int j = 1; j < 11; j++) 
				{
					System.out.println(authNamePersonIdList.get(userIdList.get((int) iDistX[j] - 1)) + "\t" + distance[j]);
				}
			}

			con.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

}
