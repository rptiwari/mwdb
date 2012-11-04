package edu.mwdb.project;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task2 {

	public static void main(String[] args) {

		DblpData dblpData = new DblpData();
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		for(String author:authorTermFreq.keySet()){
			System.out.println(author);
			System.out.println(authorTermFreq.get(author));
		}
		
	}
	

}
