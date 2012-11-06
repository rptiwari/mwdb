package edu.mwdb.project;

public class Run1C {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{// TODO Auto-generated method stub
		Task1cCompDocs t1c = new Task1cCompDocs();
		String personNum = "1632506";
//		t1c.findDifferentiationSimilarDocs(personNum, "TFIDF2");
		t1c.findLatentSemantics(personNum, "SVD");
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}

}
