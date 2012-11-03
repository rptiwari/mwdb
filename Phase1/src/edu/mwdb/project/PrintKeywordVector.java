package edu.mwdb.project;

import java.sql.*;
import java.util.Map;
import java.io.*;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class PrintKeywordVector
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
			String rowData = "";
			int noOfDocs = 0;
			//237222

			// To get the required abstract given the paperId
			Statement stmt = con.createStatement();
			String query_paperid = "SELECT * FROM papers WHERE paperid=" + args[0];
			ResultSet rs = stmt.executeQuery(query_paperid);
			while (rs.next()) 
			{
				rowData = rs.getString(5);
			}

			// To know the count of abstracts in the DB and store it in noOfDocs.
			Statement stmt1 = con.createStatement();
			String query_alldocs_count = "SELECT count(*) AS count FROM papers WHERE abstract != \"\"";
			ResultSet rs1 = stmt1.executeQuery(query_alldocs_count);
			while (rs1.next()) 
			{
				noOfDocs = rs1.getInt("count");
			}
			
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
			
			//Creating the Character Array Set from the list of stop words
			CharArraySet stopWordsCharArrSet = new CharArraySet(Version.LUCENE_36, utilityObj.createStopWordsSet(), true);
			//Creating a token stream from the abstract got from the DB for the given paperId
			TokenStream docStream = new StandardTokenizer(Version.LUCENE_36, new StringReader(rowData));
			//Creating the Keywords of a given abstract
			TokenStream keywords = new StopFilter(Version.LUCENE_36, docStream ,stopWordsCharArrSet);

			//Storing the details of Keyword and its corresponding TF in the Map termFreq
			Map<String,Float> termFreq = utilityObj.createTF(keywords, rowData);
			//Calling the method createTFIDF to create the TF and the TF-IDF vector output
			utilityObj.createTFIDF(noOfDocs,indexDirectory, termFreq,args[1]);

			con.close();

		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
}
