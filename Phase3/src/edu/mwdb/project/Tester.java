package edu.mwdb.project;

import java.util.List;

public class Tester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//printAuthorPapers("1639041");
		//printAuthorName("1639041");
		printPaperAbstract("504505");
	}
	
	private static void printPaperAbstract(String paperId){
		DblpData d = new DblpData();
		System.out.println(d.getPaperAbstract(paperId));
	}
	
	
	private static void printAuthorName(String authorId){
		DblpData d = new DblpData();
		System.out.println(d.getAuthName(authorId));
	}
	
	private static void printAuthorPapers(String authorId){
		DblpData d = new DblpData();
		List<String> abstracts = d.getAbstractsByAuthor(authorId);
		for(String a :abstracts){
			System.out.println(a);
		}
	}

}
