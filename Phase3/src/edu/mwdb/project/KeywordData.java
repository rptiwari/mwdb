package edu.mwdb.project;

public class KeywordData {
	
	private String keyword;
	private float weightedFreq;
	private float idf;
	
	public float getWeightedFreq() {
		return weightedFreq;
	}
	public void setWeightedFreq(float weightedFreq) {
		this.weightedFreq = weightedFreq;
	}
	
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	public float getIdf() {
		return idf;
	}
	public void setIdf(float idf) {
		this.idf = idf;
	}

}
