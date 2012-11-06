package edu.mwdb.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Phase2 {

	/**
	 * @param args Command Line Argument to run the application
	 */
	public static void main(String[] args) {
		/*if(args.length < 1){
			System.err.println("Incorrect Usage... Please specify the task to run (task1a, task1b etc).");
			System.exit(1);
		}*/

		//String taskName = args[0];
		Phase2 phase2 = new Phase2();
		String taskName = "task1b";
		try{
			if(taskName.equalsIgnoreCase("task1a")){
				/*if(args.length != 3){
					System.err.println("Please Provide TaskName, Model and userid");
					System.exit(1);
				}
				else*/
					//phase2.doTask1a(args[1],args[2]);
				phase2.doTask1a("","");
			}else if(taskName.equalsIgnoreCase("task1b")){
				phase2.doTask1b();
			}else if(taskName.equalsIgnoreCase("task1c")){
				phase2.doTask1c();
			}else if(taskName.equalsIgnoreCase("task2a")){
				phase2.doTask2a();
			}else if(taskName.equalsIgnoreCase("task2b")){
				phase2.doTask2b();
			}else if(taskName.equalsIgnoreCase("task3a")){
				phase2.doTask3a();
			}else if(taskName.equalsIgnoreCase("task3b")){
				phase2.doTask3b();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void doTask1a(String model, String personId) throws Exception{
		personId = "1632672";
//		Top5Semantics1a latentSemantics = new Top5Semantics1a();
//		latentSemantics.getSemantics(personId, "PCA");
//		System.out.println("\n\n");
//		latentSemantics.getSemantics(personId, "SVD");
//		System.out.println("\n\n");
		task1aLDA t1Lda = new task1aLDA();
		t1Lda.doTask(personId);
	}
	
	private void doTask1b() throws Exception{
		int authorId = 1632672;
		UserCompKeywordVector1b t1bKV = new UserCompKeywordVector1b();
		t1bKV.runTask(authorId);
		System.out.println("\n\n");
		UserCompDiffVector1b t1bTFIDF2 = new UserCompDiffVector1b();
		t1bTFIDF2.doTask(authorId);
		System.out.println("\n\n");
//		UserCompPCASVD1b comp = new UserCompPCASVD1b();
//		comp.computePCA_SVD(authorId, "PCA");
//		System.out.println("\n\n");
//		comp.computePCA_SVD(authorId, "SVD");
//		System.out.println("\n\n");
//		UserCompLDA1 t1b = new UserCompLDA1();
//		t1b.doLatentSemantics(Integer.toString(authorId));
	}
	
	private void doTask1c() throws Exception{
		Task1cCompDocs t1c = new Task1cCompDocs();
		String personNum = "1632672";
		t1c.findKWSimilarDocs(personNum);
	}
	
	private void doTask2a() throws Exception{
		Task2 task = new Task2();
		System.out.println("Top 3 Latent Semantics in Author-Author similarity matrix");

		Map.Entry<String, Double>[][] top3_author = task.getTop3LatSemBySVD_AuthorAuthor();
		for(int i=0; i<top3_author.length; i++) {
			for (int j=0; j<top3_author[i].length; j++) {
				System.out.print(top3_author[i][j].getKey() + ":" + top3_author[i][j].getValue() + "\t");
			}
			System.out.println();
		}
	}
	
	private void doTask2b() throws Exception{
		System.out.println("Top 3 Latent Semantics in Coauthor-Coauthor similarity matrix");
		Task2 task = new Task2();
		
		Map.Entry<String, Double>[][] top3_coauthors = task.getTop3LatSemBySVD_CoAuthorCoAuthor();
		for(int i=0; i<top3_coauthors.length; i++) {
			for (int j=0; j<top3_coauthors[i].length; j++) {
				System.out.print(top3_coauthors[i][j].getKey() + ":" + top3_coauthors[i][j].getValue() + "\t");
			}
			System.out.println();
		}
	}
	
	private void doTask3a() throws Exception{
		Task2 task2 = new Task2();
		Task3a task = new Task3a();
		ArrayList<Map.Entry<String, Double>>[] authorGroups = task.getGroupPartitions(task2.getTop3LatSemBySVD_AuthorAuthor());
		
		for (int i=0; i<authorGroups.length; i++) {
			System.out.println("AUTHOR GROUP" + (i+1));
			for (int j=0; j<authorGroups[i].size(); j++) {
				System.out.println(authorGroups[i].get(j).getKey() + " : " + authorGroups[i].get(j).getValue());
			}
			System.out.println();
		}
		
		ArrayList<Map.Entry<String, Double>>[] coauthorGroups = task.getGroupPartitions(task2.getTop3LatSemBySVD_CoAuthorCoAuthor());
		
		for (int i=0; i<coauthorGroups.length; i++) {
			System.out.println("COAUTHOR GROUP" + (i+1));
			for (int j=0; j<coauthorGroups[i].size(); j++) {
				System.out.println(coauthorGroups[i].get(j).getKey() + " : " + coauthorGroups[i].get(j).getValue());
			}
			System.out.println();
		}
	}
	
	private void doTask3b() throws Exception{
		Task3b task = new Task3b();
		DblpData dblpData = new DblpData();
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
		Map<String, TermFreqVector> authorTermFreq = dblpData.getAuthorTermFrequencies(indexDir);
		Task2 task2 = new Task2();
		ArrayList<Map.Entry<String, Double>>[] authorGroups = Task3a.getGroupPartitions(task2.getTop3LatSemBySVD_AuthorAuthor());
		ArrayList<Map.Entry<String, Double>>[] coauthorGroups = Task3a.getGroupPartitions(task2.getTop3LatSemBySVD_CoAuthorCoAuthor());
		
		// Author-Author
		System.out.println("Author-Author");
		HashMap<String, Double> authorAssociatedVector = task.getAssociationKeywVectorToLatSem(authorTermFreq, allTerms, authorGroups);
		for (Map.Entry<String, Double> entry : authorAssociatedVector.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}
		
		// Coauthor-Coauthor
		System.out.println("\n\n\n\n\n\nCoauthor-Coauthor");
		HashMap<String, Double> coauthorAssociatedVector = task.getAssociationKeywVectorToLatSem(authorTermFreq, allTerms, coauthorGroups);
		for (Map.Entry<String, Double> entry : coauthorAssociatedVector.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}
	}
}
