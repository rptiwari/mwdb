package edu.mwdb.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Phase2 {

	/**
	 * @param args
	 *            Command Line Argument to run the application
	 */
	public static void main(String[] args) {
		while (true) {
			System.out.println("\nEnter Command (task args1, args2....)   ");
			Scanner sc = new Scanner(System.in);
			String command = sc.nextLine();
			String[] tokens = command.split(" ");

			Phase2 phase2 = new Phase2();
			String taskName = tokens[0];
			
			try {
				if (taskName.equalsIgnoreCase("task1a")) {
					phase2.doTask1a(tokens[1], tokens[2]);
				} else if (taskName.equalsIgnoreCase("task1b")) {
					phase2.doTask1b(tokens[1], tokens[2], tokens[3]);
				} else if (taskName.equalsIgnoreCase("task1c")) {
					phase2.doTask1c(tokens[1], tokens[2], tokens[3]);
				} else if (taskName.equalsIgnoreCase("task2a")) {
					phase2.doTask2a();
				} else if (taskName.equalsIgnoreCase("task2b")) {
					phase2.doTask2b();
				} else if (taskName.equalsIgnoreCase("task3a")) {
					phase2.doTask3a();
				} else if (taskName.equalsIgnoreCase("task3b")) {
					phase2.doTask3b();
				}
				else if (taskName.equalsIgnoreCase("quit")) {
					MatLab.closeConnection();
					System.exit(0);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			System.out.println("\n");
		}
	}

	private void doTask1a(String personId, String model) throws Exception {
		if (model.equalsIgnoreCase("PCA")) {
			Top5Semantics1a latentSemantics = new Top5Semantics1a();

			System.out.println("\n********   Top 5 Latent Symantics using "
					+ model + "   *******\n");
			latentSemantics.getSemantics(personId, "PCA");
			System.out.println("\n\n");
		} else if (model.equalsIgnoreCase("SVD")) {
			Top5Semantics1a latentSemantics = new Top5Semantics1a();

			System.out.println("\n********   Top 5 Latent Symantics using "
					+ model + "   *******");
			latentSemantics.getSemantics(personId, "SVD");
			System.out.println("\n\n");
		} else if (model.equalsIgnoreCase("LDA")) {
			System.out.println("\n\n");
			System.out.println("********   Top 5 Latent Symantics using "
					+ model + "   *******");

			task1aLDA t1Lda = new task1aLDA();
			t1Lda.doTask(personId);
			System.out.println("\n\n");
		} else {
			System.err.println("Incorrect Model");
			System.exit(1);
		}

	}

	private void doTask1b(String personNum, String diffVector, String latentSem)
			throws Exception {
		int authorId = Integer.parseInt(personNum);
		System.out.println("\n\n");
		System.out.println("*******   Keyword Vector  *******");
		UserCompKeywordVector1b t1bKV = new UserCompKeywordVector1b();
		t1bKV.runTask(authorId);

		System.out.println("\n\n");
		System.out.println("*******   Differentiation Vector with "
				+ diffVector + "   *******");
		UserCompDiffVector1b t1bTFIDF2 = new UserCompDiffVector1b();
		t1bTFIDF2.doTask(authorId);

		System.out.println("\n\n");
		System.out.println("*******   Top 5 Latent Symantics. Model - "
				+ latentSem + "   *******");

		if (latentSem.equalsIgnoreCase("PCA")) {
			UserCompPCASVD1b comp = new UserCompPCASVD1b();
			comp.computePCA_SVD(authorId, "PCA");
		} else if (latentSem.equalsIgnoreCase("SVD")) {
			UserCompPCASVD1b comp = new UserCompPCASVD1b();
			comp.computePCA_SVD(authorId, "SVD");
		} else if (latentSem.equalsIgnoreCase("LDA")) {
			UserCompLDA1 t1b = new UserCompLDA1();
			t1b.doLatentSemantics(Integer.toString(authorId));
		} else {
			System.err.println("Incorrect Model");
			System.exit(1);
		}

	}

	private void doTask1c(String personNum, String diffVector, String latentSem)
			throws Exception {
		Task1cCompDocs t1c = new Task1cCompDocs();
		System.out.println("\n\n");
		System.out.println("*******   Keyword Vector  *******");
		t1c.findKWSimilarDocs(personNum);

		System.out.println("\n\n");
		System.out.println("*******   Differentiation Vector  with "
				+ diffVector + "   *******");
		t1c.findDifferentiationSimilarDocs(personNum, diffVector);

		System.out.println("\n\n");
		System.out.println("*******   Top 5 Latent Symantics. Model - "
				+ latentSem + "   *******");
		t1c.findLatentSemantics(personNum, latentSem);

	}

	private void doTask2a() throws Exception {
		Task2 task = new Task2();
		System.out
				.println("Top 3 Latent Semantics in Author-Author similarity matrix");

		Map.Entry<String, Double>[][] top3_author = task
				.getTop3LatSemBySVD_AuthorAuthor();
		for (int i = 0; i < top3_author.length; i++) {
			for (int j = 0; j < top3_author[i].length; j++) {
				System.out.print(top3_author[i][j].getKey() + ":"
						+ top3_author[i][j].getValue() + "\t");
			}
			System.out.println();
		}
	}

	private void doTask2b() throws Exception {
		System.out
				.println("Top 3 Latent Semantics in Coauthor-Coauthor similarity matrix");
		Task2 task = new Task2();

		Map.Entry<String, Double>[][] top3_coauthors = task
				.getTop3LatSemBySVD_CoAuthorCoAuthor();
		for (int i = 0; i < top3_coauthors.length; i++) {
			for (int j = 0; j < top3_coauthors[i].length; j++) {
				System.out.print(top3_coauthors[i][j].getKey() + ":"
						+ top3_coauthors[i][j].getValue() + "\t");
			}
			System.out.println();
		}
	}

	private void doTask3a() throws Exception {
		Task2 task2 = new Task2();
		Task3a task = new Task3a();
		ArrayList<Map.Entry<String, Double>>[] authorGroups = task
				.getGroupPartitions(task2.getTop3LatSemBySVD_AuthorAuthor());

		for (int i = 0; i < authorGroups.length; i++) {
			System.out.println("AUTHOR GROUP" + (i + 1));
			for (int j = 0; j < authorGroups[i].size(); j++) {
				System.out.println(authorGroups[i].get(j).getKey() + " : "
						+ authorGroups[i].get(j).getValue());
			}
			System.out.println();
		}

		ArrayList<Map.Entry<String, Double>>[] coauthorGroups = task
				.getGroupPartitions(task2.getTop3LatSemBySVD_CoAuthorCoAuthor());

		for (int i = 0; i < coauthorGroups.length; i++) {
			System.out.println("COAUTHOR GROUP" + (i + 1));
			for (int j = 0; j < coauthorGroups[i].size(); j++) {
				System.out.println(coauthorGroups[i].get(j).getKey() + " : "
						+ coauthorGroups[i].get(j).getValue());
			}
			System.out.println();
		}
	}

	private void doTask3b() throws Exception {
		Task3b task = new Task3b();
		DblpData dblpData = new DblpData();
		Directory indexDir = dblpData.createAuthorDocumentIndex();
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");
		Map<String, TermFreqVector> authorTermFreq = dblpData
				.getAuthorTermFrequencies(indexDir);
		Task2 task2 = new Task2();
		ArrayList<Map.Entry<String, Double>>[] authorGroups = Task3a
				.getGroupPartitions(task2.getTop3LatSemBySVD_AuthorAuthor());
		ArrayList<Map.Entry<String, Double>>[] coauthorGroups = Task3a
				.getGroupPartitions(task2.getTop3LatSemBySVD_CoAuthorCoAuthor());

		// Author-Author
		System.out.println("Author-Author");
		SortedSet<Map.Entry<String, Double>> authorAssociatedVector = task
				.getAssociationKeywVectorToLatSem(authorTermFreq, allTerms,
						authorGroups);
		for (Map.Entry<String, Double> entry : authorAssociatedVector) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}

		// Coauthor-Coauthor
		System.out.println("\n\n\n\n\n\nCoauthor-Coauthor");
		SortedSet<Map.Entry<String, Double>> coauthorAssociatedVector = task
				.getAssociationKeywVectorToLatSem(authorTermFreq, allTerms,
						coauthorGroups);
		for (Map.Entry<String, Double> entry : coauthorAssociatedVector) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}
	}
}
