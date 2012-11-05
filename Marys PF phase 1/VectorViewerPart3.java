import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.FieldsEnum;
import org.apache.lucene.index.FilterAtomicReader;
import org.apache.lucene.index.FilterAtomicReader.FilterTermsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.DocIdBitSet;

public class VectorViewerPart3 {
	String fileName = null;

	int count = 0; // counter of number of documents in the index

	/*
	 * forward index to create term frequency vectors for all documents.
	 */
	public HashMap<Integer, HashMap<BytesRef, Double>> newforwardIndex;
	
	/*
	 * Create a forwardIndex to supplant lucene 4.0.0 termVectors doc[] -->
	 * <term,freq>....<term,freq>
	 */

	private void doit(String field, BytesRef text, IndexReader reader,
			TermsEnum termsEnum2) throws IOException {
		DocsEnum docsEnum = null;

		int docSize = reader.numDocs();

		Bits docBits = SlowCompositeReaderWrapper.wrap(reader).getLiveDocs();
		DocsEnum xyz = termsEnum2.docs(docBits, docsEnum, DocsEnum.FLAG_FREQS);

		// Add following here to retrieve the <docNo,Freq> pair for each term
		FilterAtomicReader.FilterDocsEnum Docs = null;
		Docs = new FilterAtomicReader.FilterDocsEnum(Docs);
		BitSet bitSet = new BitSet();
		DocIdBitSet dbs = new DocIdBitSet(bitSet);

		DocsEnum termdocs = SlowCompositeReaderWrapper.wrap(reader)
				.termDocsEnum(dbs, field, text);

		// add termval's number of occurrence per document into array of
		// document's running total of term instances
		int docID;
		Integer freqcount = 0;
		termdocs = xyz;
		while ((docID = termdocs.nextDoc()) != DocsEnum.NO_MORE_DOCS) {

			freqcount = termdocs.freq();

			Double temp = (double) termdocs.freq();
			BytesRef termWord = new BytesRef();
			termWord.copyBytes(text);

			if (newforwardIndex.containsKey(docID)) {

				HashMap<BytesRef, Double> x = newforwardIndex.get(docID);
				x.put(termWord, temp);
				newforwardIndex.put(docID, x);

			} else {
				Term tempTerm = new Term("abstract", termWord);

				HashMap<BytesRef, Double> termfreqVector = new HashMap<BytesRef, Double>();
				termfreqVector.put(termWord, temp);

				newforwardIndex.put(docID, termfreqVector);

			}
		} // end termdocs for each term

	}

	/*
	 * Create the Forward Index by calling doit for each new term in Inverted
	 * index
	 */

	public void makeForwardIndex(IndexReader reader, IndexSearcher searcher) {

		try {

			// use the TermEnum object to iterate through all the terms in the
			// index

			int docSize = reader.numDocs();
			newforwardIndex = new HashMap<Integer, HashMap<BytesRef, Double>>(
					docSize);

			TermsEnum termsEnums = null;
			DocsEnum docsEnum = null;
			// termsEnums = new FilterTermsEnum(termsEnums);

			Fields what = SlowCompositeReaderWrapper.wrap(reader).fields();
			FieldsEnum fieldsEnum = (new FilterAtomicReader.FilterFields(what))
					.iterator(); // /.terms("field");
			String field;
			while ((field = fieldsEnum.next()) != null) {
				TermsEnum termsEnum = fieldsEnum.terms().iterator(termsEnums);
				BytesRef text;
				while ((text = termsEnum.next()) != null) {

					if (field.equals("abstract")) {
						doit(field, text, reader, termsEnum);
					}
				}
			}

		} catch (IOException e) {
			System.out.println("IO Error has occured: " + e);
			return;
		}

	} // end MakeForwardIndex


	public void makeForwardIndex(IndexReader reader) {

		try {

			// use the TermEnum object to iterate through all the terms in the
			// index

			int docSize = reader.numDocs();
			newforwardIndex = new HashMap<Integer, HashMap<BytesRef, Double>>(
					docSize);

			TermsEnum termsEnums = null;
			DocsEnum docsEnum = null;
			// termsEnums = new FilterTermsEnum(termsEnums);

			Fields what = SlowCompositeReaderWrapper.wrap(reader).fields();
			FieldsEnum fieldsEnum = (new FilterAtomicReader.FilterFields(what))
					.iterator(); // /.terms("field");
			String field;
			while ((field = fieldsEnum.next()) != null) {
				TermsEnum termsEnum = fieldsEnum.terms().iterator(termsEnums);
				BytesRef text;
				while ((text = termsEnum.next()) != null) {

					if (field.equals("abstract")) {
						doit(field, text, reader, termsEnum);
					}
				}
			}

		} catch (IOException e) {
			System.out.println("IO Error has occured: " + e);
			return;
		}

	} // end MakeForwardIndex

	/*
	 * String personID author id String rankMethod tf or tfidf2 vector model
	 * 
	 * Entry point method that creates forward index, retrieves and collates
	 * author's key words, ranks the keyword vector and then outputs vector
	 */
	public void showAuthorVector(String personID, String rankMethod) {
		// lists the vector
		try {
			fileName = AuthorDifferentiation.getIndexDirectory();
			IndexReader reader = DirectoryReader.open(FSDirectory
					.open(new File(fileName)));

			IndexSearcher searcher = new IndexSearcher(reader);

			IndexReader otherIndex = DirectoryReader.open(FSDirectory
					.open(new File(MakeIndex.getIndexDirectory())));

			makeForwardIndex(reader, searcher);
			TermsEnum termsEnums = null;
			DocsEnum docsEnum = null;
			termsEnums = new FilterTermsEnum(termsEnums);

			Fields what = SlowCompositeReaderWrapper.wrap(reader).fields();
			FieldsEnum fieldsEnum = (new FilterAtomicReader.FilterFields(what))
					.iterator();
			String field;
			while ((field = fieldsEnum.next()) != null) {
				TermsEnum termsEnum = fieldsEnum.terms().iterator(termsEnums);
				BytesRef text;

				while ((text = termsEnum.next()) != null) {

				}
			}

			ArrayList<TermFrequency> termVector = makeAuthorIndexSearch(
					personID, searcher, reader, rankMethod);

			if (termVector.size() > 0) {

				TermFrequency[] rankingArray = (TermFrequency[]) termVector
						.toArray(new TermFrequency[termVector.size()]);
				Arrays.sort(rankingArray, Collections.reverseOrder());

				makeDisplay(rankingArray, personID, rankMethod);
			} else {
				System.out.println(personID + "  " + rankMethod);
				System.out.println("no relevant terms found");
			}
			reader.close();
		} catch (IOException e) {
			System.out.println("IO Error has occured: " + e);
			return;
		}

	}

	/*
	 * ArrayList method queries using Lucene IndexSearch to retrieve all
	 * documents created by the author.
	 * 
	 * Returns an ArrayList of a new class of Lucene Term and Frequency to
	 * represent unsorted keyword vector
	 * 
	 * Parameters:
	 * 
	 * String personID IndexSearcher searcher IndexReader reader String
	 * rankingMethod
	 * 
	 * Assumes no more than 1000 documents are written by a single author
	 */

	public ArrayList<TermFrequency> makeAuthorIndexSearch0(String personID,
			IndexSearcher searcher, IndexReader reader, String rankingMethod)
			throws IOException {

		ArrayList<TermFrequency> resultVector = new ArrayList<TermFrequency>();

		ArrayList<String> coauthors = new ArrayList<String>();
		HashMap<BytesRef, Double> combinedResultVector = new HashMap<BytesRef, Double>(); // master
																							// keyword
																							// vector
																							// for
																							// author.

		DocsEnum docsEnum = null;
		TermsEnum termsEnum = null;
		Double termWeight = 0.00;
		Double termWeightYearFactor = 1.0;
		int totalNumWordsinDocs = 0;
		Double maxFreqOfWordinDocs = 0.00;
		boolean PFswitch = false;
		String field = "abstract";

		// establish values used for every term frequency calculation
		int sizeOfCorpus = reader.numDocs();

		if (rankingMethod.equalsIgnoreCase("PF")) {
			PFswitch = true;
		}
		AuthorDifferentiation p = new AuthorDifferentiation();

		try {
			coauthors = (ArrayList<String>) p.makeSQLAuthorsQuery(p.ReadDB(),
					personID);
		} catch (IOException e) {
			System.out.println("failed");
			e.printStackTrace();
		}
		/*
		 * use lucene query to return doc ids for all documents written by
		 * personid This method assumes index is built upon authorid for each
		 * paper stored in index
		 */
		Query query = new TermQuery(new Term("personid", personID));

		ScoreDoc[] hitDocsNum = searcher.search(query, 1000).scoreDocs;
		if (hitDocsNum.length > 0) {
			ScoreDoc[] dolt = hitDocsNum;
			for (int i = 0; i < hitDocsNum.length; i++) {
				// for (ScoreDoc docsHits : hitDocsNum){
				int docNum = hitDocsNum[i].doc;

				/*
				 * find the document that corresponds to personID value in field
				 * personid of index
				 */
				Document doc = searcher.doc(docNum);

				Fields fields = MultiFields.getFields(reader);
				if (fields != null) {

					termWeightYearFactor = findYearWeight(fields, docsEnum,
							termsEnum, reader, docNum);

					if (newforwardIndex.containsKey(docNum)) {
						Iterator termsIt = newforwardIndex.get(docNum)
								.entrySet().iterator();

						while (termsIt.hasNext()) {

							Map.Entry entry = (Map.Entry) termsIt.next();

							BytesRef tempTerm = (BytesRef) entry.getKey();
							//
							// System.out.println(entry.getValue());

							Double tt = (Double) entry.getValue()
									* termWeightYearFactor;
							Double freqValue = new Double(tt);
							// System.out.println(entry.getValue());
							if (combinedResultVector.containsKey(tempTerm)) {

								Double cumulative = combinedResultVector
										.get(tempTerm) + freqValue;

								combinedResultVector.put(tempTerm, cumulative);
							} else {
								BytesRef anotherTerm = BytesRef
										.deepCopyOf(tempTerm);
								combinedResultVector
										.put(anotherTerm, freqValue);
							}
							totalNumWordsinDocs += freqValue;
							maxFreqOfWordinDocs = Math.max(maxFreqOfWordinDocs,
									freqValue);
							System.out.println(maxFreqOfWordinDocs + " "
									+ freqValue);
						} // end while iterating over terms in forwardindex
					} // end if any terms extracted from this document's
						// abstract and put into forwardindex
				} // end for non-null fields
			} /* end for every document with author id */

			/*
			 * do normalization of word frequency over all of author's documents
			 * after master keyword vector has been created
			 */

			Iterator vectorTermsIt1 = combinedResultVector.entrySet()
					.iterator();

			while (vectorTermsIt1.hasNext()) {

				Map.Entry entry = (Map.Entry) vectorTermsIt1.next();

				BytesRef tempTerm = (BytesRef) entry.getKey();
				Double freqValue = (Double) entry.getValue();
				// System.out.println("FREQvalue "+ freqValue+
				// " "+tempTerm.utf8ToString());
			}

			Iterator vectorTermsIt = combinedResultVector.entrySet().iterator();

			while (vectorTermsIt.hasNext()) {

				Map.Entry entry = (Map.Entry) vectorTermsIt.next();

				BytesRef tempTerm = (BytesRef) entry.getKey();
				Double freqValue = (Double) entry.getValue();
				// System.out.println("FREQvalue "+ freqValue);
				// Double temp = (freqValue / totalNumWordsinDocs);
				Double temp = (freqValue / maxFreqOfWordinDocs);
				BytesRef termText2 = tempTerm;

				if (PFswitch) {
					// Calculate idf for this term i: log(N/Ni)
					double idfFactor = Math.log((sizeOfCorpus / (double) reader
							.docFreq(field, termText2)));

					Double it = freqValue * idfFactor;
					combinedResultVector.put(tempTerm, it);
				} else {
					combinedResultVector.put(tempTerm, temp);
				}
				// System.out.println(combinedResultVector.get(tempTerm));
			} /* if termvector from abstract field contains any terms */

			/*
			 * convert hashmap to arraylist of TermFrequency for standard
			 * interface
			 */

			Iterator vectorIt = combinedResultVector.entrySet().iterator();

			while (vectorIt.hasNext()) {

				Map.Entry entry = (Map.Entry) vectorIt.next();

				TermFrequency tf = new TermFrequency();

				tf.term = new Term("abstract", (BytesRef) entry.getKey());
				tf.frequency = (Double) entry.getValue();
			
				resultVector.add(tf);
			}

		} /*
		 * end if can find document matching the input personID string ie
		 * hits.length > 0
		 */

		return resultVector;

	} /* end makeAuthorSearch */

	/*
	 * Revision 1.0 Now use sql to retrieve unique values rather than searching
	 * thru index ArrayList method queries using Lucene IndexSearch to retrieve
	 * all documents created by the author.
	 * 
	 * Returns an ArrayList of a new class of Lucene Term and Frequency to
	 * represent unsorted keyword vector
	 * 
	 * Parameters:
	 * 
	 * String personID IndexSearcher searcher IndexReader reader String
	 * rankingMethod
	 * 
	 * Assumes no more than 1000 documents are written by a single author
	 */

	public ArrayList<TermFrequency> makeAuthorIndexSearch(String personID,
			IndexSearcher searcher, IndexReader reader, String rankingMethod)
			throws IOException {

		ArrayList<TermFrequency> resultVector = new ArrayList<TermFrequency>();

		ArrayList<String> papersByAuthor = new ArrayList<String>();
		ArrayList<String> papersByCoauthors = new ArrayList<String>();
		ArrayList<String> coauthors = new ArrayList<String>();
		HashMap<BytesRef, Double> combinedResultVector = new HashMap<BytesRef, Double>(); // master
																							// keyword
																							// vector
																							// for
																							// author.

		DocsEnum docsEnum = null;
		TermsEnum termsEnum = null;
		Double termWeight = 0.00;
		Double termWeightYearFactor = 1.0;
		int totalNumWordsinDocs = 0;
		Double maxFreqOfWordinDocs = 0.00;
		boolean PFswitch = false;
		String field = "abstract";
		Integer setCoauthorDocs = 0;
		Integer setCoauthorAndSelfDocs = 0;
		Connection conn;

		// establish default values used for every term frequency calculation
		Double NsizeOfCorpus = (double) reader.numDocs();
		Double RsizeOfCorpus = (double) reader.numDocs();

		if (rankingMethod.equalsIgnoreCase("PF")) {
			PFswitch = true;
		}

		AuthorDifferentiation p = new AuthorDifferentiation();

		try {
			conn = p.ReadDB();
			coauthors = (ArrayList<String>) p.makeSQLAuthorsQuery(conn,
					personID); // find coauthors

			// int howManyDocs = computeNumDocsByAuthor(p, coauthors, searcher
			// );
			papersByCoauthors = computeNumDocsByAuthor(p, coauthors, conn);

			System.out.println(papersByCoauthors.size());

			ArrayList thisAuthor = new ArrayList<String>();
			thisAuthor.add(personID);
			papersByAuthor = computeNumDocsByAuthor(p, thisAuthor, conn);
			// System.out.println("AUTHEORS PAPERS " + papersByAuthor.size());

			ArrayList<String> uniquePapers = new ArrayList<String>(
					new HashSet<String>(papersByCoauthors)); // eliminate
																// duplicates
																// amongst
																// coauthors

			Boolean george = uniquePapers.removeAll(new HashSet<String>(
					papersByAuthor)); // eliminate all authors docs from
										// coauther set
			// System.out.println("size of unique coathors without author" +
			// uniquePapers.size());

			papersByAuthor.addAll(papersByCoauthors);

			ArrayList<String> uniqueNpapers = new ArrayList<String>(
					new HashSet<String>(papersByAuthor));
			// System.out.println("size of unique authors & coathors " +
			// uniqueNpapers.size());
			NsizeOfCorpus = (double) uniqueNpapers.size();
			RsizeOfCorpus = (double) uniquePapers.size();

			Fields fields = MultiFields.getFields(reader);
			/*
			 * use lucene query to return doc ids for all documents written by
			 * personid This method assumes index is built upon authorid for
			 * each paper stored in index
			 */
			Query query = new TermQuery(new Term("personid", personID));

			ScoreDoc[] hitDocsNum = searcher.search(query, 1000).scoreDocs;
			if (hitDocsNum.length > 0) {
				ScoreDoc[] dolt = hitDocsNum;
				for (int i = 0; i < hitDocsNum.length; i++) {
					// for (ScoreDoc docsHits : hitDocsNum){
					int docNum = hitDocsNum[i].doc;

					/*
					 * find the document that corresponds to personID value in
					 * field personid of index
					 */
					Document doc = searcher.doc(docNum);

					if (fields != null) {
						/*
						 * Remove weighting based upon date as unclear reqs
						 * 
						 * termWeightYearFactor = findYearWeight(fields,
						 * docsEnum, termsEnum, reader, docNum);
						 */

						termWeightYearFactor = 1.0;

						if (newforwardIndex.containsKey(docNum)) {
							Iterator termsIt = newforwardIndex.get(docNum)
									.entrySet().iterator();

							while (termsIt.hasNext()) {

								Map.Entry entry = (Map.Entry) termsIt.next();

								BytesRef tempTerm = (BytesRef) entry.getKey();
								//
								// System.out.println(entry.getValue());

								Double tt = (Double) entry.getValue()
										* termWeightYearFactor;

								Double freqValue = new Double(tt);
								if (combinedResultVector.containsKey(tempTerm)) {

									Double cumulative = combinedResultVector
											.get(tempTerm) + freqValue;

									combinedResultVector.put(tempTerm,
											cumulative);
								} else {
									BytesRef anotherTerm = BytesRef
											.deepCopyOf(tempTerm);
									combinedResultVector.put(anotherTerm,
											freqValue);
								}
								totalNumWordsinDocs += freqValue;

								maxFreqOfWordinDocs = Math.max(
										maxFreqOfWordinDocs, freqValue);

							} // end while iterating over terms in forwardindex
						} // end if any terms extracted from this document's
							// abstract and put into forwardindex
					} // end for non-null fields
				} /* end for every document with author id */

				/*
				 * do normalization of word frequency over all of author's
				 * documents after master keyword vector has been created
				 */

				Iterator vectorTermsIt = combinedResultVector.entrySet()
						.iterator();

				while (vectorTermsIt.hasNext()) {

					Map.Entry entry = (Map.Entry) vectorTermsIt.next();

					BytesRef tempTerm = (BytesRef) entry.getKey();
					Double freqValue = (Double) entry.getValue();

					Double temp = (freqValue / totalNumWordsinDocs);
					// Double temp = (freqValue / maxFreqOfWordinDocs);
					BytesRef termText2 = tempTerm;

					if (!PFswitch) {
						// Calculate idf for this term i: log(N/Ni)
						double idfFactor = Math
								.log((NsizeOfCorpus / (double) reader
										.docFreq(field, termText2)));

						Double it = temp * idfFactor;

						combinedResultVector.put(tempTerm, it);
					} else {

						ArrayList<Integer> indexDocs = findDocNums(fields,
								docsEnum, termsEnum, reader, uniquePapers);
						ArrayList<Integer> indexDocswithAuthor = findDocNums(
								fields, docsEnum, termsEnum, reader,
								uniqueNpapers);

						Double pfFactoredWeight = (doPFModelWeight(tempTerm,
								NsizeOfCorpus, RsizeOfCorpus, indexDocs,
								indexDocswithAuthor));
System.out.println(" pf " +pfFactoredWeight);
						Double trouble = temp * pfFactoredWeight;
						combinedResultVector.put(tempTerm, trouble);
					}
				} /* if termvector from abstract field contains any terms */

				/*
				 * convert hashmap to arraylist of TermFrequency for standard
				 * interface
				 */

				Iterator vectorIt = combinedResultVector.entrySet().iterator();

				while (vectorIt.hasNext()) {

					Map.Entry entry = (Map.Entry) vectorIt.next();

					TermFrequency tf = new TermFrequency();

					tf.term = new Term("abstract", (BytesRef) entry.getKey());
					tf.frequency = (Double) entry.getValue();
					resultVector.add(tf);
				}

			} /*
			 * end if can find document matching the input personID string ie
			 * hits.length > 0
			 */
		} catch (IOException e) {
			System.out.println("failed");
			e.printStackTrace();
		}

		return resultVector;

	} /* end makeAuthorSearch */

	/*
	 * helper method Calculates a weight based upon age of paper older papers
	 * term frequencies are reduced while new papers term frequencies are
	 * slightly smaller Fields fields DocsEnum docsEnum enum constructs passed
	 * in for possible reuse TermsEnum termsEnum INdexReader reader
	 */

	private Double findYearWeight(Fields fields, DocsEnum docsEnum,
			TermsEnum termsEnum, IndexReader reader, int docNum)
			throws IOException {

		Double termWeight = 1.0; // default to weight that has no impact
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int docYear = year - 1;
		int oldest = year;

		Terms terms = fields.terms("year");
		if (terms != null) {

			TermsEnum termsIterator = terms.iterator(termsEnum);
			BytesRef text2 = null;
			while ((text2 = termsIterator.next()) != null) {
				oldest = Math.min(oldest,
						Integer.parseInt(text2.utf8ToString()));

				Bits docBits = SlowCompositeReaderWrapper.wrap(reader)
						.getLiveDocs();
				DocsEnum xyz = termsIterator.docs(docBits, docsEnum,
						DocsEnum.FLAG_FREQS);

				// xyz.nextDoc(); // no longer use this in lucene 4.0.0 despite
				// migration folder instructions

				if ((xyz.advance(docNum)) != DocsEnum.NO_MORE_DOCS) {

					if (xyz.docID() == docNum) {
						docYear = Integer.parseInt(text2.utf8ToString());

						break;
					}

				}
			}

			termWeight = (double) ((docYear - oldest) + 1) / (year - oldest);

		} // end if not null terms
		return termWeight;
	}

	/*
	 * helper method for output of resultant vector presently one pair displayed
	 * per line
	 */
	public void makeDisplay(TermFrequency[] resultVector, String header,
			String rankMethod) {

		System.out.println("  ID: " + header + "        " + rankMethod);
		// System.out.println(resultVector[0].term.text() + " " +
		// resultVector[0].frequency);
		for (TermFrequency i : resultVector) {

			System.out.println("< " + i.toString() + " >");
		}
	}

	/*
	 * Calculate the PF Model feedback weight on an individual term basis,
	 * regardless of other terms like, IDF this is applied on a global corpus
	 * level with the given formula: ui,j = log [ri,j/(Ri - ri,j)) / ((ni,j -
	 * ri,j )/(Ni -ni,j-Ri + ri,j))] *(ri,j/Ri - (ni,j - ri,j)/Ni - Ri)
	 * 
	 * where N is size of corpus that includes coauthors and author self papers
	 * NSizeofCorpus R is size of corpus that includes only coauthors papers
	 * RSizeofCorpus
	 */

	public Double doPFModelWeight(BytesRef tempTerm, Double NSizeofCorpus,
			Double RSizeOfCorpus, ArrayList<Integer> coauthorPapers,
			ArrayList<Integer> coauthorAndSelfPapers) {
		Integer docCount = 0;
		Double r = 0.00;
		Double n = 0.00;

		for (Integer docID : coauthorPapers) {
			if (newforwardIndex.containsKey(docID)) {

				if (newforwardIndex.get(docID).containsKey(tempTerm)) {
					docCount++;
				}
			}
		}
		r = (Double) (RSizeOfCorpus - docCount);
		docCount = 0;
		for (Integer docID : coauthorAndSelfPapers) {
			if (newforwardIndex.containsKey(docID)) {
				if (newforwardIndex.get(docID).containsKey(tempTerm)) {
					docCount++;
				}
			}
		}
		n = (Double) (NSizeofCorpus - docCount); /*
												 * all papers subtractingthose
												 * with term is same as those
												 * papers w/o keyword
												 */
System.out.print(tempTerm.utf8ToString());
		return doFormulaPF(RSizeOfCorpus, NSizeofCorpus, r, n); /*
																 * the
																 * calculatedWeight
																 */
	}

	public Double doFormulaPF(Double R, Double N, Double r, Double n) {
		/* compute the formula for PF Model for this term tempTerm */
		Double leftPart = Math.log((r / (R - r)) / ((n - r) / (N - n - R + r)));
		Double rightPart = Math.abs((r / R) - ((n - r) / (N - R)));				// can be negative so need abs
		System.out.print("N: "+ N + "R " + R + "n " + n + "r " + r);
		return (leftPart * rightPart);

	}

	/*
	 * Tabulate the number of papers written by each author. 1.0 revise counting
	 * process to compensate for duplicates
	 */

	private int computeNumDocsByAuthor(AuthorDifferentiation p,
			List<String> coauthors, IndexSearcher searcher) throws IOException {
		int hitCounter = 0;
		List<String> papers = new ArrayList<String>();

		for (String anyAuthor : coauthors) {

			Query query = new TermQuery(new Term("personid", anyAuthor));

			ScoreDoc[] hitDocsNum = searcher.search(query, 1000).scoreDocs;
			hitCounter = +hitDocsNum.length;

		}
		return hitCounter;

	}

	private ArrayList<String> computeNumDocsByAuthor(AuthorDifferentiation p,
			List<String> coauthors, Connection conn) throws IOException {
		int hitCounter = 0;
		List<String> papers = new ArrayList<String>();

		for (String anyAuthor : coauthors) {

			papers.addAll(p.makeSQLCoAuthorsQuery(conn, anyAuthor));

		}

		ArrayList<String> uniqueAuthorsDocs = new ArrayList<String>(
				new HashSet<String>(papers));
		return uniqueAuthorsDocs;

	}

	/*
	 * Helper method that correlates paperID strings with doc numbers found in
	 * forward index.
	 */
	private ArrayList<Integer> findDocNums(Fields fields, DocsEnum docsEnum,
			TermsEnum termsEnum, IndexReader reader, ArrayList<String> paperIDs)
			throws IOException {

		ArrayList<Integer> docPaperCorrelation = new ArrayList<Integer>();
		/* sort paperids in order to seek through terms in index */
		// Collections.sort(paperIDs);
		Terms terms = fields.terms("paperid");
		if (terms != null) {

			TermsEnum termsIterator = terms.iterator(termsEnum);
			BytesRef text2 = null;
			while ((text2 = termsIterator.next()) != null) {
				String paperTerm = text2.utf8ToString();
				if (paperIDs.contains(paperTerm)) {
					// System.out.println(termsIterator.docFreq());
					Bits docBits = SlowCompositeReaderWrapper.wrap(reader)
							.getLiveDocs();
					DocsEnum xyz = termsIterator.docs(docBits, docsEnum,
							DocsEnum.FLAG_FREQS);

					while ((xyz.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
						docPaperCorrelation.add(xyz.docID());
						// System.out.println("doc " + xyz.docID() + paperTerm
						// );
						break;
					}
				} // end if
			}

		} // end if not null terms
		return docPaperCorrelation;
	}

}
