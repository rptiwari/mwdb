package edu.mwdb.project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.*;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class DifferentiateAuthor extends PrintKeywordVector
{

	/**
	 * @param args
	 */

	public static void main(String[] args)
	{
		Utility utilityObj = new Utility();
		try 
		{
			Connection con = utilityObj.getDBConnection();
			// List to store the author abstracts
			List<String> rowData = new ArrayList<String>();
			// List to store co-author abstracts
			List<String> rowData1 = new ArrayList<String>();
			// List to store the co-authors and self abstracts.
			List<String> rowData2 = new ArrayList<String>();

			Statement stmt = con.createStatement();
			String personId = args[0];

			//String personId = "1632672";

			// Creation of a Index Directory.
			StandardAnalyzer docAnalyzer = new StandardAnalyzer(Version.LUCENE_36);
			IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_36,docAnalyzer);
			Directory indexDirectory = new RAMDirectory();
			IndexWriter indexWr = new IndexWriter(indexDirectory, indexConfig);

			String doc="";

			String query_authorid = 
					"select p.abstract from papers p join " +  
							"(select distinct w.paperid from " + 
							"authors a join writtenby w where a.personid = w.personid and a.personid = " + personId  + 
							" order by paperid) T1 on p.paperid = T1.paperid where p.abstract != \"\"";

			ResultSet rs = stmt.executeQuery(query_authorid);
			while (rs.next())
			{
				rowData.add(rs.getString("abstract"));
			}

			String query_coauthor_and_self = 
					"SELECT distinct abstract from papers join " +
							"(select distinct(writby.paperid) from writtenby writby join coauthorswpaper coauth on writby.personid = coauth.personid2 " +
							"where coauth.personid1 = " + personId + " )T " +
							" ON papers.paperid = T.paperid where abstract!=\"\"";

			ResultSet rs1 = stmt.executeQuery(query_coauthor_and_self);
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

			CharArraySet stopWordsCharArrSet;
			TokenStream docStream;
			TokenStream keywords;
			
			Map<String,Float> termFreq = new HashMap<String, Float>();
			Map<String,Float> termCoauthorFreq = new HashMap<String, Float>();
			Map<String,Float> termCoauthorSelfFreq = new HashMap<String, Float>();
			
			KeywordConfig config;
			KeywordConfig coauthor;
			KeywordConfig coSelfAuthor;
			
			List<KeywordConfig> configList = new ArrayList<KeywordConfig>();
			List<KeywordConfig> coauthorsList = new ArrayList<KeywordConfig>();
			List<KeywordConfig> coauthorsSelfList = new ArrayList<KeywordConfig>();
			int noOfWords = 0;

			for (int i=0;i<rowData.size();i++) 
			{

				String[] rowDataArr = rowData.get(i).split("[ ]+");
				noOfWords += rowDataArr.length;

				//Creating the Character Array Set from the list of stop words
				stopWordsCharArrSet = new CharArraySet(Version.LUCENE_36, utilityObj.createStopWordsSet(), true);
				//Creating a token stream from the abstract got from the DB for the given paperId
				docStream = new StandardTokenizer(Version.LUCENE_36, new StringReader(rowData.get(i)));
				//Creating the Keywords of a given abstract
				keywords = new StopFilter(Version.LUCENE_36, docStream ,stopWordsCharArrSet);

				termFreq = utilityObj.createauthorTF(keywords, rowData.get(i));

				for(Map.Entry<String, Float> keys : termFreq.entrySet())
				{
					config = new KeywordConfig();
					config.setKeyword(keys.getKey());
					config.setWeightedFreq(keys.getValue());
					configList.add(config);
				}
			}

			Map<String,Float> termFinalFreq = new HashMap<String, Float>();
			for (KeywordConfig itr: configList){
				Float val = termFinalFreq.get(itr.getKeyword());
				termFinalFreq.put(itr.getKeyword(), (val == null) ? itr.getWeightedFreq() : (val + itr.getWeightedFreq()));
			}		
			for(Map.Entry<String, Float> k: termFinalFreq.entrySet())
			{
				termFinalFreq.put(k.getKey(), k.getValue()/noOfWords);
			}
			
			//Calling the method createTFIDF to create the TF and the TF-IDF vector output
			utilityObj.createTFIDF(rowData1.size(),indexDirectory, termFinalFreq,args[1]);



			// For calculation of PF model
			if(args[1].equalsIgnoreCase("PF")){
				String query_coauthor = 
						"SELECT abstract " + 
								"from papers join " +
								"(select distinct(writb.paperid) " + 
								"from writtenby writb join coauthorswpaper coauth " +
								"on writb.personid = coauth.personid2 " +
								"where coauth.personid1 =" + personId + " AND writb.paperid NOT IN (SELECT paperid from writtenby where personid = " + personId + ")) T " + 
								"ON papers.paperid = T.paperid where abstract!=\"\"";


				ResultSet rs2 = stmt.executeQuery(query_coauthor);
				while (rs2.next())
				{
					rowData2.add(rs2.getString("abstract"));
				}

				float Ri=0,Ni = 0;
				float rij,nij=0;
				Ri = rowData2.size();
				Ni = rowData1.size();

				// For calculating the values of co-author vector values - rij
				for (int i=0;i<rowData.size();i++)
				{
					//Creating the Character Array Set from the list of stop words
					stopWordsCharArrSet = new CharArraySet(Version.LUCENE_36, utilityObj.createStopWordsSet(), true);
					//Creating a token stream from the abstract got from the DB for the given paperId
					docStream = new StandardTokenizer(Version.LUCENE_36, new StringReader(rowData.get(i)));
					//Creating the Keywords of a given abstract
					keywords = new StopFilter(Version.LUCENE_36, docStream ,stopWordsCharArrSet);

					List<String> keywordsList = new ArrayList<String>(); 
					while(keywords.incrementToken())
					{
						keywordsList.add(keywords.getAttribute(CharTermAttribute.class).toString());
					}
					String keyword;

					for(int j=0;j<keywordsList.size();j++)
					{
						keyword = keywordsList.get(j);
						termCoauthorFreq = utilityObj.countijvalue(keyword, rowData2);
						for(Map.Entry<String, Float> keys : termCoauthorFreq.entrySet())
						{
							coauthor = new KeywordConfig();
							coauthor.setKeyword(keys.getKey());
							coauthor.setWeightedFreq(keys.getValue());
							coauthorsList.add(coauthor);
						}
					}
				}

				Map<String,Float> coauthorMapforCount = new HashMap<String, Float>();
				for (KeywordConfig itr: coauthorsList){
					//Float val = coauthorMapforCount.get(itr.getKeyword());
					coauthorMapforCount.put(itr.getKeyword(), rowData2.size() - itr.getWeightedFreq());
				}

				// For calculating the values of co-author and self vector values - nij
				for (int i=0;i<rowData.size();i++)
				{
					//Creating the Character Array Set from the list of stop words
					stopWordsCharArrSet = new CharArraySet(Version.LUCENE_36, utilityObj.createStopWordsSet(), true);
					//Creating a token stream from the abstract got from the DB for the given paperId
					docStream = new StandardTokenizer(Version.LUCENE_36, new StringReader(rowData.get(i)));
					//Creating the Keywords of a given abstract
					keywords = new StopFilter(Version.LUCENE_36, docStream ,stopWordsCharArrSet);

					List<String> keywordsList = new ArrayList<String>(); 
					while(keywords.incrementToken())
					{
						keywordsList.add(keywords.getAttribute(CharTermAttribute.class).toString());
					}
					String keyword;

					for(int j=0;j<keywordsList.size();j++)
					{
						keyword = keywordsList.get(j);
						//System.out.println(keyword);
						termCoauthorSelfFreq = utilityObj.countijvalue(keyword, rowData1);
						for(Map.Entry<String, Float> keys : termCoauthorSelfFreq.entrySet())
						{
							coSelfAuthor = new KeywordConfig();
							coSelfAuthor.setKeyword(keys.getKey());
							coSelfAuthor.setWeightedFreq(keys.getValue());
							coauthorsSelfList.add(coSelfAuthor);
						}
					}
				}
				Map<String,Float> coauthorSelfMapforCount = new HashMap<String, Float>();
				for (KeywordConfig itr: coauthorsSelfList){
					//Float val = coauthorSelfMapforCount.get(itr.getKeyword());
					coauthorSelfMapforCount.put(itr.getKeyword(), rowData1.size() - itr.getWeightedFreq());
				}

				Map<String, Float> uijMap = new HashMap<String, Float>();
				float temp = 0;

				for(Map.Entry<String, Float> k: termFinalFreq.entrySet())
				{
					rij = (coauthorMapforCount.get(k.getKey())==null)?0:coauthorMapforCount.get(k.getKey());
					nij = (coauthorSelfMapforCount.get(k.getKey())==null)?0:coauthorSelfMapforCount.get(k.getKey());
					//System.out.println(rij + "\t" + Ri + "\t" + nij + "\t" + Ni);
					temp = (float)Math.log(Math.abs((rij/(Ri - rij))/((nij-rij)/(Ni-nij-Ri+rij)))) * Math.abs((rij/Ri)-((nij-rij)/(Ni-Ri)));
					uijMap.put(k.getKey(), temp);
				}
				
				uijMap = utilityObj.sortByComparator(uijMap);

				for(Map.Entry<String, Float> k: uijMap.entrySet()){
					System.out.println("PF: " + "{" + k.getKey() + "," + k.getValue() + "}");
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
