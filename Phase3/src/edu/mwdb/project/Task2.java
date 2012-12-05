package edu.mwdb.project;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;

public class Task2 {

	DblpData dblpData;
	Utility utils;

	public Task2() throws Exception {
		dblpData = new DblpData();
		utils = new Utility();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Task2 t2;
		try {
			t2 = new Task2();
			String t = "TF";
			if(t.equals("TF")){
				Graph g_TF = t2.getCoauthorPapersSimilarityGraph_KeywordVector("TF");
				printGraph(g_TF);
			}
			else if(t.equals("TF-IDF")){
				Graph g_TFIDF = t2.getCoauthorPapersSimilarityGraph_KeywordVector("TF-IDF");
				printGraph(g_TFIDF);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

        public static Graph runTask2(String param) throws Exception{
            Task2 t2 = new Task2();
            Graph g = null;
            
            if(param.equalsIgnoreCase("TF")){
                g = t2.getCoauthorPapersSimilarityGraph_KeywordVector("TF");               
            }else if(param.equalsIgnoreCase("TF-IDF")){
                g = t2.getCoauthorPapersSimilarityGraph_KeywordVector("TF-IDF");                
            }else{
                System.out.println("Incorrect Usage");
                System.exit(1);
            }
            
            return g;
        }
        
	public Graph getCoauthorPapersSimilarityGraph_KeywordVector(String type) throws Exception{
		Directory indexDir = dblpData.createAllDocumentIndex();
		IndexReader reader = IndexReader.open(indexDir);
		Map<String, TermFreqVector> paperTermFreq = dblpData.getDocTermFrequencies(indexDir);
		List<String> allTerms = dblpData.getAllTermsInIndex(indexDir, "doc");

		int docIdx = 0;
		Map<Integer, String> paperIndexMap = new HashMap<Integer, String>();
		for(String paperId:paperTermFreq.keySet()){
			paperIndexMap.put(docIdx++, paperId);
		}

		int termIdx = 0;
		Map<String, Integer> allKeywordsPosMap = new HashMap<String, Integer>();
		for(String kw:allTerms){
			allKeywordsPosMap.put(kw, termIdx++);
		}

		int numPapers = paperTermFreq.keySet().size();
		double[][] similarityMatrix = new double[numPapers][numPapers];

		Map<String, Set<String>> papersMap = dblpData.getAuthorPapers();

		for(int i=0; i<numPapers; i++){
			for(int j=0; j<numPapers; j++){
				String paperId1 = paperIndexMap.get(i);
				String paperId2 = paperIndexMap.get(j);

				double cosineSim = 0;
				Set<String> intersection = new HashSet<String>(papersMap.get(paperId1));
				intersection.retainAll(papersMap.get(paperId2));
				if(!(intersection.isEmpty())){
					if(type.equals("TF")){
						double[] a1 = Utility.getAlignedTermFreqVector(paperTermFreq.get(paperId1), allKeywordsPosMap);
						double[] a2 = Utility.getAlignedTermFreqVector(paperTermFreq.get(paperId2), allKeywordsPosMap);
						cosineSim = utils.cosineSimilarity(a1, a2);
					}
					else if (type.equals("TF-IDF")){
						double[] a1 = Utility.getAlignedTFIDFVector(paperTermFreq.get(paperId1), allKeywordsPosMap,reader);
						double[] a2 = Utility.getAlignedTFIDFVector(paperTermFreq.get(paperId2), allKeywordsPosMap,reader);
						cosineSim = utils.cosineSimilarity(a1, a2);
					}
				}
				similarityMatrix[i][j] = cosineSim;
			}
		}

		Graph g = new Graph(similarityMatrix, paperIndexMap);
		return g;
	}

	public static void printGraph(Graph g) {
		DecimalFormat f = new DecimalFormat("#.##");
		for(int i=0; i<g.getNumNodes(); i++)
		{
			System.out.print("\t" + g.getNodeLabel(i));
		}
		System.out.println();
		for(int i=0; i<g.getNumNodes(); i++)
		{
			System.out.print(g.getNodeLabel(i) + "\t");
			for(int j=0; j<g.getNumNodes(); j++)
			{
				System.out.print(f.format(g.getAdjacencyMatrix()[i][j]) + "\t");
			}
			System.out.print("\n");
		}
	}
}
