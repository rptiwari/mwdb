


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer; 
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase.*; 
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntDocValuesField;
import org.apache.lucene.document.TextField;


import org.apache.lucene.document.Document.*;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/*
import com.lucene.analysis.Analyzer;
import com.lucene.analysis.StopAnalyzer;
import com.lucene.document.Document;
import com.lucene.index.*;
import com.lucene.search.Searcher;

import com.lucene.search.Query;
import com.lucene.search.Hits;
import com.lucene.search.IndexSearcher.*;
import com.lucene.queryParser.QueryParser;
*/

public class AuthorDifferentiation { 
	private static IndexWriter writer;		  // new index being built
	private static String	   stopFileName	= "englishstop.txt";	// location of stop words for analyzer
	private static String	   indexDirectory	= "index3Location";
	public static String getIndexDirectory() {
		return indexDirectory;
	}

	public static void setIndexDirectory(String indexDirectory) {
		AuthorDifferentiation.indexDirectory = indexDirectory;
	}
	
	public Connection ReadDB() throws IOException{
	
	try {
        // The newInstance() call is a work around for some
        // broken Java implementations

        Class.forName("com.mysql.jdbc.Driver").newInstance();
    } catch (Exception ex) { 
    	System.out.println("FAIL to get class name");
    }

	Connection conn = null;
    
 
	
/* hard code db permissions for development */
	String user = "dbadmin";
	String pw 	= "password";
    try {
   //     conn = DriverManager.getConnection("jdbc:mysql://localhost/dblp?user=" + args[0] + "&password=" + args[1]);
    	 conn = DriverManager.getConnection("jdbc:mysql://localhost/dblp?user=" + user + "&password=" + pw );
    } catch (SQLException excep) {
        // handle any errors
        System.out.println("SQLException: " + excep.getMessage());
    }
    
  return conn;
	}
	
	
	public void makeSQLforIndex(Connection conn){
		
		Statement stmt = null;
	    ResultSet rs = null;
	    IndexWriter iwriter = null;
	// create NEW index
    
//    Directory indexDir = new RAMDirectory();

  	Directory directory;
	try {
		directory = FSDirectory.open(new File(indexDirectory));
		
   	 
  	 Analyzer analyzer3 = new StandardAnalyzer(Version.LUCENE_40,makeStopWordSet(stopFileName));
  	
  	  
  	
 
    System.out.println("*****************");
    System.out.println( ((StopwordAnalyzerBase) analyzer3).getStopwordSet().toString()); 
    
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer3);

    iwriter = new IndexWriter(directory, config);
	

	/*
	 *  retrieve from db the paper id and associated abstract 
	 *  then parse terms from abstract
	 */
	/*
	 *  retrieve from db the paper id and associated abstract 
	 *  then parse terms from abstract
	 */

	String sql = "select authors.name,writtenby.personid,papers.paperid,papers.year,papers.abstract from authors " +
			" JOIN writtenby ON authors.personid= writtenby.personid" +
			" JOIN papers ON writtenby.paperid=papers.paperid";
			
	Statement sqlstmt = null;
	
		if (conn!= null){
		sqlstmt = conn.createStatement();}
		else System.out.println("NULL");
	
	rs = sqlstmt.executeQuery(sql);
	
	/* ... repeat for each row in result set */
	if (rs != null){
		int k = 0;
	while (rs.next()) {
		System.out.println("DB ACCESS" + k);
	    Document doc = new Document();
	 

	    doc.add(new Field("name",  rs.getString("name"), TextField.TYPE_STORED));
	    
	    String personText = Integer.toString(rs.getInt("personid"));
	    doc.add(new Field("personid", personText, TextField.TYPE_STORED));
	    
	    doc.add(new Field("paperid", Integer.toString(rs.getInt("paperid")), TextField.TYPE_STORED));


	    doc.add(new Field("year", rs.getString("year"), TextField.TYPE_STORED));  
	    
	    String text = rs.getString("abstract");
	    doc.add(new Field("abstract", text, TextField.TYPE_STORED));  
	  
	    Map<String,String> attributes = new HashMap<String,String>();
	    FieldInfo finfo = new FieldInfo("abstract", true, 0, true, false, true, FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS,
	    		DocValues.Type.BYTES_VAR_STRAIGHT, DocValues.Type.BYTES_VAR_STRAIGHT, attributes);
	    iwriter.addDocument(doc);
	  k++; 
	}		// end while row in resultset
	}
	iwriter.commit();
	iwriter.close();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	
	 catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	

}	
	

	public List<String> makeSQLAuthorsQuery(Connection conn, String author){
		
		Statement stmt = null;
	    ResultSet rs = null;
	  
	    List<String> coauthors = new ArrayList<String>();
    


	/*
	 *  retrieve from db the coauthors id and associated abstract 
	 *  then parse terms from abstract
	 */
	    /* use preparedstatement to safely pass in parameter */
	String sql = "select personid2 from coauthors where personid1 = ? " ;
	PreparedStatement sqlstmt = null;
	try {
		if (conn!= null){
		sqlstmt = conn.prepareStatement(sql);
		sqlstmt.setString(1,author);
		}
		else System.out.println("NULL");
	
	rs = sqlstmt.executeQuery();
	
	/* ... repeat for each row in result set */
	/* do not add to index but return list of authors */
	if (rs != null){
		
		while (rs.next()) {
	
		
	    String text = rs.getString("personid2");
	    coauthors.add(text);
		}		// end while row in resultset
	}
	
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
ArrayList<String> uniqueAuthors = new ArrayList<String>(new HashSet<String>(coauthors));	
return uniqueAuthors;
		
}		
	/*
	 *  Return paperids set for all coauthors
	 */

	public List<String> makeSQLCoAuthorsQuery(Connection conn, String author){
		
		Statement stmt = null;
	    ResultSet rs = null;
	    List<String> coauthorsPapers = new ArrayList<String>();
    


	/*
	 *  retrieve from db the paper id and associated abstract 
	 *  then parse terms from abstract
	 */
	    /* use preparedstatement to safely pass in parameter */
	String sql = "select paperid from writtenby where personid=?" ;
	PreparedStatement sqlstmt = null;
	try {
		if (conn!= null){
		sqlstmt = conn.prepareStatement(sql);
		sqlstmt.setString(1,author);
		}
		else System.out.println("NULL");
	
	rs = sqlstmt.executeQuery();
	
	/* ... repeat for each row in result set */
	/* do not add to index but return list of papers */
	if (rs != null){
		
		while (rs.next()) {
	
		
	    String text = rs.getString("paperid");
	    coauthorsPapers.add(text);
		}		// end while row in resultset
	}
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
	/* reduce any occurrences attributed to multiple papers written by same 2 authors */
ArrayList<String> uniquePapers = new ArrayList<String>(new HashSet<String>(coauthorsPapers));	
return uniquePapers;
		
}		
	/*
	 * Helper method to read stopwordfile and create a CharArraySet  
	 */
	public CharArraySet makeStopWordSet(String stopfile){
		Set<String> wordList = new HashSet<String>();
		
		try {
			 System.out.println(System.getProperty("user.dir"));
		    BufferedReader in = new BufferedReader(new FileReader(stopfile));
		    
		    String str;
		    while ((str = in.readLine()) != null) {
		        wordList.add(str);
		    }
		    in.close();
		} catch (IOException e) {
			System.out.println("stopword error");
		}
		CharArraySet cas = new CharArraySet(Version.LUCENE_40, wordList, true);
		return cas;
	}
	
	/*
	 * Helper method to read string and print term
	 */
	public void parseString(String dbResult, Analyzer analyzer){

		try {
		TokenStream tokenStream = analyzer.tokenStream("abstract",new StringReader(dbResult)); 
		
		OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);


	      tokenStream.reset(); // Resets this stream to the beginning. (Required)
			while (tokenStream.incrementToken()) {
			    int startOffset = offsetAttribute.startOffset();
			    int endOffset = offsetAttribute.endOffset();
			    String term = charTermAttribute.toString();
			    System.out.println("token term: "+term); 
			    // Use AttributeSource.reflectAsString(boolean)
		        // for token stream debugging.
		        System.out.println("token: " + tokenStream.reflectAsString(true));
			}
			 tokenStream.end();

		      tokenStream.close(); // Release resources associated with this stream.
		} catch (IOException e) {
			System.out.println("tokenizer failure");
			e.printStackTrace();
		     }  
		

	}
	
}		
