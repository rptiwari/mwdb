package edu.mwdb.project;
public class VectorRanking implements Comparable<VectorRanking>{
	public Integer documentID;
	public double  howSimilar;
	//	public termFreqVectors vectorTermFreq = new termFreqVectors();

	//hash table with keys docid and values termFreqVectors;

	@Override
	public int compareTo(VectorRanking anyDocVector) {
		return Double.compare(howSimilar, anyDocVector.howSimilar);
	}





}




