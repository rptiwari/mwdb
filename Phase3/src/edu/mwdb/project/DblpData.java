package edu.mwdb.project;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
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
	
	public List<String> getAllTermsInIndex(Directory luceneIndexDir, String termName){
		List<String> allTerms = new ArrayList<String>();
		IndexReader reader;
		try {
			reader = IndexReader.open(luceneIndexDir);
			TermEnum te = reader.terms();
			while(te.next()){
				if(te.term().field().equals(termName)){
					allTerms.add(te.term().text());
				}
			}
			reader.close();
			 
		} catch (CorruptIndexException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return allTerms;		
	}
	
	
	public Map<String, TermFreqVector> getAuthorTermFrequencies(Directory luceneIndexDir){
		IndexReader reader;
		Map<String, TermFreqVector> authorTermFrequencies = new LinkedHashMap<String, TermFreqVector>();
		try {
			reader = IndexReader.open(luceneIndexDir);
			for (int i = 0; i < reader.numDocs(); i++) {
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
			
				PreparedStatement ps = con.prepareStatement("Select paperid,title,abstract from papers  WHERE abstract != \"\"");
				
				ResultSet rs = ps.executeQuery();
			
				while(rs.next()){
					Document document = new Document();
					document.add(new Field("paperid", rs.getString("paperid"), Field.Store.YES, Field.Index.NOT_ANALYZED));
					document.add(new Field("title", rs.getString("title"), Field.Store.YES, Field.Index.NOT_ANALYZED));
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
			
				String statement = "select paperid, abstract " +
					"from dblp.papers " +
					"where paperid NOT IN " +
					"(select paperid " +
					"from dblp.writtenby " +
					"where personid=" + authorId + ")";
				PreparedStatement ps = con.prepareStatement(statement);
				
				ResultSet rs = ps.executeQuery();
			
				while(rs.next()){
					Document document = new Document();
					String abs = rs.getString("abstract");
					if(abs != null && !abs.equals("")){
						document.add(new Field("paperid", rs.getString("paperid"), Field.Store.YES, Field.Index.NOT_ANALYZED));
						document.add(new Field("doc", abs, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
						indexWriter.addDocument(document);
					}
					
				}
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
	
	public Map<String,Set<String>> getCoauthors(){
		Utility util = new Utility();
		Connection con = util.getDBConnection();
		
		try {
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM coauthors");
			ResultSet rs = stmt.executeQuery();
			Map<String, Set<String>> result = new HashMap<String,Set<String>>();
			
			while(rs.next()){
				Set<String> s = result.get(rs.getString(1));
				if(s == null)
					s = new HashSet<String>();
				
				s.add(rs.getString(2));
				result.put(rs.getString(1), s);
			}
			
			return result;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public Map<String,Set<String>> getAuthorPapers(){
		Utility util = new Utility();
		Connection con = util.getDBConnection();
		
		try {
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM writtenby");
			ResultSet rs = stmt.executeQuery();
			Map<String, Set<String>> result = new HashMap<String,Set<String>>();
			
			while(rs.next()){
				Set<String> s = result.get(rs.getString(2));
				if(s == null)
					s = new HashSet<String>();
				
				s.add(rs.getString(1));
				result.put(rs.getString(2), s);
			}
			
			return result;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Get all the paper Ids written by the author and his coauthors. (OR)
	 * @param authorId - Author's ID in which to get his paper Ids, as well as his coauthors' paper Ids.
	 * @return a map in which the keys represent the paperIds. The values are boolean but are meaningless.
	 * @throws Exception
	 */
	public Set<Integer> getPaperIdsFromCoauthorAndSelf(String authorId) throws Exception {
		Utility util = new Utility();
		Connection con = util.getDBConnection();
		Statement stmt = con.createStatement();
		
		ResultSet coauthor_and_self_paperIds_rs = stmt.executeQuery("select paperid " +
				"from writtenby inner join " + 
				"(select personid2 " + 
				"from coauthorswpaper " +
				"where personid1=" + authorId + " " +
				"group by personid2 " +
				"UNION " +
				"select personid2 " +
				"from coauthorswpaper " +
				"where personid2=" + authorId + " " +
				"group by personid2) as x " +
				"on writtenby.personid=x.personid2 " +
				"group by paperid");	
		Set<Integer> coauthor_and_self_paperIds = new HashSet<Integer>();
		while (coauthor_and_self_paperIds_rs.next()) {
			coauthor_and_self_paperIds.add(coauthor_and_self_paperIds_rs.getInt("paperid"));
	  	}
		return coauthor_and_self_paperIds;
	}
	
	/**
	 * Get all the paper Ids written by the author's coauthors excluding the ones written by the author himself
	 * @param authorId
	 * @return
	 * @throws Exception
	 */
	public HashSet<Integer> getPaperIdsFromCoauthorExcludingSelf(String authorId) throws Exception {
		Utility util = new Utility();
		Connection con = util.getDBConnection();
		Statement stmt = con.createStatement();
		
	  	stmt = con.createStatement();
		ResultSet coauthor_paperIds_rs = stmt.executeQuery("select paperid " +
				"from writtenby inner join " + 
				"(select personid2 " +  
				"from coauthorswpaper " + 
				"where personid1=" + authorId + " " +
				"group by personid2) as x " + 
				"on writtenby.personid=x.personid2 " +
				"where paperid NOT IN " +
				"(select paperid " +
				"from writtenby " +
				"where personid=" + authorId + ") " +
				"group by paperid");
		HashSet<Integer> coauthor_paperIds = new HashSet<Integer>();
	  	while (coauthor_paperIds_rs.next()) {
	  		coauthor_paperIds.add(coauthor_paperIds_rs.getInt("paperid"));
	  	}
	  	return coauthor_paperIds;
	}
	
	/**
	 * Get all the paperIds that are written from authorId
	 * @param authorId
	 * @return a list of Integers representing the paperIds
	 * @throws Exception
	 */
	public List<Integer> getPaperIdsFromAuthor(String authorId) throws Exception {
		Utility util = new Utility();
		Connection con = util.getDBConnection();
		Statement stmt = con.createStatement();
		
	  	stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("select p.paperid " +
				"from writtenby w, papers p " +  
				"where w.personid=" + authorId + " and w.paperid = p.paperid and p.abstract <> ''");
		List<Integer> retVal = new ArrayList<Integer>();
	  	while (rs.next()) {
	  		retVal.add(rs.getInt("paperid"));
	  	}
	  	return retVal;
	}
	
	/**
	 * Builds a forward and an inverse index based on the papers and their keywords.
	 * The forward index is a HashMap<Integer,HashMap> where Integer represents the paperId, 
	 * and the inner Hashmap is <String,Double> where String is the keyword and Double is the TF
	 * The inverse index is a HashMap<String,HashMap> where String represents the keyword,
	 * and the inner HashMap is <Integer,Boolean> where Integer is the paperId and boolean is just a dummy value.
	 * @return a hashmap array containing the forward index in slot 0 and backward index in slot 1.
	 * @throws Exception
	 */
	public HashMap[] getForwardAndInversePaperKeywIndex() throws Exception {
		HashMap<Integer,HashMap> paperIdIndex = new HashMap();
		HashMap<String,HashMap> invertedIndex = new HashMap();
		Directory lucenedir = createAllDocumentIndex();
		IndexReader reader;
		try {
			reader = IndexReader.open(lucenedir);
			for (int i = 0; i < reader.numDocs(); i++) {
				TermFreqVector tfv = reader.getTermFreqVector(i, "doc");
				
				int paperId = Integer.parseInt(reader.document(i).get("paperid"));
				String[] terms = tfv.getTerms();
				int[] termFreqs = tfv.getTermFrequencies();
				
				HashMap<String,Double> abstractIndex = new HashMap<String, Double>();
				for(int j=0; j<terms.length; j++){
					abstractIndex.put(terms[j], (double)termFreqs[j]);
				}

				// Creating the inverted index
				for (String key : abstractIndex.keySet()) { 
					HashMap papersHavingWord;
					if (invertedIndex.containsKey(key)) {
						papersHavingWord = invertedIndex.get(key);
						papersHavingWord.put(paperId, true);
					} else {
						papersHavingWord = new HashMap();
						papersHavingWord.put(paperId, true);
					}
					invertedIndex.put(key, papersHavingWord);
				}
				paperIdIndex.put(paperId, abstractIndex);
			}
			reader.close();

		} catch (CorruptIndexException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return new HashMap[]{paperIdIndex, invertedIndex};
	}
	
	public Map<String, Double> getTFIDF2Vector(String authorId) throws Exception{
		Map<String, Double> tfidf2Vector = new LinkedHashMap<String, Double>();	
		Directory coAuthAndSelfIndex = createCoAuthorSelfIndex(authorId);
		IndexReader coAuthAndSelfIndexReader = IndexReader.open(coAuthAndSelfIndex);
		Directory authorIndex = createAuthorDocumentIndex();
		TermFreqVector authorKwVector = getAuthorTermFrequencies(authorIndex).get(authorId);
		String[] terms = authorKwVector.getTerms();
		int[] termFreqs = authorKwVector.getTermFrequencies();
		for(int i=0;i<terms.length; i++){
			double idf = Utility.getIDF(coAuthAndSelfIndexReader, terms[i]);
			tfidf2Vector.put(terms[i], termFreqs[i]*idf);
		}
		return tfidf2Vector;
	}

	private Directory createCoAuthorSelfIndex(String authorId)
			throws IOException, CorruptIndexException,
			LockObtainFailedException, SQLException {
		String query_coauthor_and_self = 
			"SELECT distinct abstract from papers join " +
					"(select distinct(writby.paperid) from writtenby writby join coauthorswpaper coauth on writby.personid = coauth.personid2 " +
					"where coauth.personid1 = " + authorId + " )T " +
					" ON papers.paperid = T.paperid where abstract!=\"\"";
		Utility util = new Utility();
		util.getDBConnection();
		Directory coAuthAndSelfIndex = new RAMDirectory();
		StopAnalyzer sa = new StopAnalyzer(Version.LUCENE_36, Utility.getStopWordsFile());
		IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_36, sa);
		IndexWriter coAuthorSelfIdxWriter = new IndexWriter(coAuthAndSelfIndex, indexConfig);
		ResultSet rs = util.getDBConnection().prepareStatement(query_coauthor_and_self).executeQuery();
		while (rs.next())
		{
			Document document = new Document();
			String doc = rs.getString("abstract");
			document.add(new Field("doc", doc, Field.Store.YES,Field.Index.ANALYZED));
			coAuthorSelfIdxWriter.addDocument(document);
		}
		coAuthorSelfIdxWriter.close();
		return coAuthAndSelfIndex;
	}
	

	public HashMap<String, String> getAuthNamePersonIdList(){
		Utility util = new Utility();
		Connection con = util.getDBConnection();

		HashMap<String,String> authNamePersonIDList = new HashMap<String, String>();
		try{
			Statement statement = con.createStatement();
			ResultSet resultSet = statement.executeQuery("select distinct(a.personid),a.name from writtenby w, papers p,authors a where a.personid = w.personid" +
					" and w.paperid=p.paperid and p.abstract != \"\"");
			while (resultSet.next()) 
			{
				authNamePersonIDList.put(resultSet.getString("personid"), resultSet.getString("name"));
			}
			return authNamePersonIDList;
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Get single author name from unique author id  Assumes Author is represented onlyif written a paper with an abstract.
	 * @param personid
	 * @return authName (String)
	 * @throws Exception
	 */
	public String getAuthName(String personid){
		Utility util = new Utility();
		Connection con = util.getDBConnection();

		String authName = new String();
		try{
			Statement statement = con.createStatement();
			ResultSet resultSet = statement.executeQuery("select a.name from authors a where a.personid = " +personid );
			while (resultSet.next()) 
			{
				authName = resultSet.getString("name");
			}
			return authName;
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}
	
	/**
	 * Get all the authorIds that have non-empty abstracts 
	 * quickly from Lucene index
	 * @param lucene directory that has authorid field indexed
	 * @return a list of Strings representing the authorIds
	 * @throws Exception
	 */
	public List<String> getAllActiveAuthors(Directory luceneIndexDir){
 
 		List<String> authors = new ArrayList<String>();
		IndexReader reader;

		try {
			reader = IndexReader.open(luceneIndexDir);
			for (int i = 0; i < reader.numDocs(); i++) {
				authors.add(reader.document(i).get("authorid"));
			}
			reader.close();
			 
		} catch (CorruptIndexException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return authors;
	}
	
	/**
	 * Get all the TermFreqVectors for every Document 
	 * quickly from Lucene index
	 * @param lucene directory that has doc field indexed
	 * @return a Map of TermFreqVector for every document entry in the index
	 * 
	 */
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
	
}
