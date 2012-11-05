package edu.mwdb.project;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;


public class DblpData {

	public List<String> getAllAuthors(){
		Utility util = new Utility();
		Connection con = util.getDBConnection();
		List<String> authors = new ArrayList<String>();
		try {
			PreparedStatement ps = con.prepareStatement("Select personid from authors");
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				authors.add(rs.getString(1));
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return authors;
	}
	
	public List<String> getAbstractsByAuthor(String authorId){
		Utility util = new Utility();
		Connection con = util.getDBConnection();
		List<String> abstracts = new ArrayList<String>();
		try {
			PreparedStatement ps = con.prepareStatement("Select abstract from papers p JOIN writtenby w ON w.personid = ? AND p.paperid = w.paperid AND p.abstract != \"\"");
			ps.setString(1, authorId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				abstracts.add(rs.getString(1));
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return abstracts;
	}
	
	public String combineAbstracts(List<String> abstracts){
		StringBuffer sb = new StringBuffer();
		for(String a:abstracts){
			sb.append(a);
			sb.append(" ");
		}
		return sb.toString();
	}
	
	
	public Map<String, TermFreqVector> getAuthorTermFrequencies(Directory luceneIndexDir){
		IndexReader reader;
		Map<String, TermFreqVector> authorTermFrequencies = new HashMap<String, TermFreqVector>();
		try {
			reader = IndexReader.open(luceneIndexDir);
			for (int i = 0; i < reader.maxDoc(); i++) {
				TermFreqVector tfv = reader.getTermFreqVector(i, "doc");
				authorTermFrequencies.put(reader.document(i).get("authorid"), tfv);
			}
			reader.close();
			 
		} catch (CorruptIndexException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return authorTermFrequencies;
	}
	
	
	public Directory createAuthorDocumentIndex(){
		Directory indexDirectory = new RAMDirectory();
		StopAnalyzer sa;
		try {
			sa = new StopAnalyzer(Version.LUCENE_36, Utility.getStopWordsFile());
			IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_36, sa);
			IndexWriter indexWriter = new IndexWriter(indexDirectory, indexConfig);
			
			for(String authorId:getAllAuthors()){
				String combinedAbstract = combineAbstracts(getAbstractsByAuthor(authorId));	
				if(combinedAbstract != null && !combinedAbstract.equals("")){
					Document document = new Document();
					document.add(new Field("authorid", authorId, Field.Store.YES, Field.Index.NOT_ANALYZED));
					document.add(new Field("doc", combinedAbstract, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
					indexWriter.addDocument(document);
				}
			}
			indexWriter.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return indexDirectory;
	}
	
	public Directory createAllDocumentIndex() {
		Directory indexDirectory = new RAMDirectory();
		StopAnalyzer sa;
		try {
			
			Utility util = new Utility();
			Connection con = util.getDBConnection();
			

			sa = new StopAnalyzer(Version.LUCENE_36, Utility.getStopWordsFile());
			IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_36, sa);
			IndexWriter indexWriter = new IndexWriter(indexDirectory, indexConfig);
			
				PreparedStatement ps = con.prepareStatement("Select paperid,abstract from papers  WHERE abstract != \"\"");
				
				ResultSet rs = ps.executeQuery();
			
				while(rs.next()){
					Document document = new Document();
					document.add(new Field("paperid", rs.getString("paperid"), Field.Store.YES, Field.Index.NOT_ANALYZED));
					document.add(new Field("doc", rs.getString("abstract"), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
					indexWriter.addDocument(document);
				}
			indexWriter.commit();
			indexWriter.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return indexDirectory;
	}
	
	public Directory createNonAuthoredDocsIndex(String authorId) {
		Directory indexDirectory = new RAMDirectory();
		StopAnalyzer sa;
		try {
			
			Utility util = new Utility();
			Connection con = util.getDBConnection();
			

			sa = new StopAnalyzer(Version.LUCENE_36, Utility.getStopWordsFile());
			IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_36, sa);
			IndexWriter indexWriter = new IndexWriter(indexDirectory, indexConfig);
			
				String statement = "select paperid " +
				"from dblp.papers " +
						"where paperid NOT IN " +
				"(select paperid " +
				"from dblp.writtenby " +
				"where personid=" + authorId + ")";
				PreparedStatement ps = con.prepareStatement(statement);
				
				ResultSet rs = ps.executeQuery();
			
				while(rs.next()){
					Document document = new Document();
					document.add(new Field("paperid", rs.getString("paperid"), Field.Store.YES, Field.Index.NOT_ANALYZED));
					document.add(new Field("doc", rs.getString("abstract"), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
					indexWriter.addDocument(document);
				}
			indexWriter.commit();
			indexWriter.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return indexDirectory;
	}
}
