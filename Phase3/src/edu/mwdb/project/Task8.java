package edu.mwdb.project;

import edu.mwdb.project.DblpData;
import edu.mwdb.project.Graph;
import edu.mwdb.project.Task1;
import edu.mwdb.project.Task2;
import edu.mwdb.project.Task3;
import edu.mwdb.project.Task4;
import edu.mwdb.project.Task5;
import edu.mwdb.project.Task6;
import edu.mwdb.project.Task7;
import edu.mwdb.project.TaskResults;
import edu.mwdb.project.Utility;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.lucene.store.Directory;

public class Task8 {

    public static void main(String args[]) {
        if (args.length < 2) {
            System.err.print("**** Incorrect Usage **** \n");
            System.exit(1);
        }

        String taskName = args[0];
        try {
            if (taskName.equalsIgnoreCase("Task1")) {
                Graph g = createGraphFromParam(args[1]);
                Utility.printSortedEdges(g);
            } 
            else if (taskName.equalsIgnoreCase("Task2")) {
                Graph g = createGraphFromParam(args[1]);
                Task2.printGraph(g);
            } 
            else if (taskName.equalsIgnoreCase("Task3")) {
                Task3.runTask3(Integer.parseInt(args[1]));
            } 
            else if (taskName.equalsIgnoreCase("Task4")) {
                Task4.runTask4(Integer.parseInt(args[1]), args[2], createGraphFromParam(args[3]));
            } 
            else if (taskName.equalsIgnoreCase("Task5")) {
                Task5.runTask5(Integer.parseInt(args[1]), args[2], args[3], createGraphFromParam(args[4]));
            } 
            else if (taskName.equalsIgnoreCase("Task6")) {
                Task6.runTask6(Integer.parseInt(args[1]), args[2], args[3], createGraphFromParam(args[4]));
            } 
            else if (taskName.equalsIgnoreCase("Task8")) {
                Task5 t5 = new Task5();
                DblpData dblp = new DblpData();
                
                Map.Entry<String, Double>[] result = t5.GraphSearchContent(createGraphFromParam(args[4]), args[3], Integer.parseInt(args[1]));
                
                if(args[2].equalsIgnoreCase("Author")){
                    for (Map.Entry<String, Double> r : result) {
                    System.out.println(r.getKey()+"  : "+dblp.getAuthName(r.getKey()) + " : " + r.getValue());
                }
                }else if(args[2].equalsIgnoreCase("Paper")){
                    for (Map.Entry<String, Double> r : result) {
                    System.out.println(r.getKey()+"  : "+dblp.getPaperTitle(r.getKey()) + " : " + r.getValue());
                }
                }else{
                    System.out.println("Incorrect Input");
                    System.exit(1);
                }
                 
                
                System.out.println("\nPlease provide comma sepearated IDs for relevence feedback..");
                Scanner sc = new Scanner(System.in);
                String input = sc.nextLine();
                String[] res = input.split(",");
                
                List<String> nodeList = Arrays.asList(res);
                Task7 task = new Task7();
                TaskResults outputTask5 = task.doTask7(nodeList, Integer.parseInt(args[1]), createGraphFromParam(args[4]).getNodeIndexLabelMap(),args[3]);
                
                
                 if(args[2].equalsIgnoreCase("Author")){
                    task.displayAuthors(outputTask5.getSimilarities(),Integer.parseInt(args[1]), args[2]);
                 }
                 
                 if(args[2].equalsIgnoreCase("Paper")){
                    Directory index = dblp.createAllDocumentIndex();
                    task.displayPapers(outputTask5.getSimilarities(),Integer.parseInt(args[1]), args[3], index);
                 }
                 
                 task.displayAdjustedQuery(outputTask5.getNewTermFreqVector(),outputTask5.getOldQuery());
            } 
            else {
                System.err.print("Incorrect Usage");
                System.exit(1);
            }
            
            System.out.println("\n\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    
    public static Graph createGraphFromParam(String param){
        Graph g = null;
        try{
            if(param.equalsIgnoreCase("KV") || param.equalsIgnoreCase("PCA") ||
                param.equalsIgnoreCase("LDA") || param.equalsIgnoreCase("SVD")){
                Task1 t1 = new Task1();
                g = t1.runTask1(param);
            }else if(param.equalsIgnoreCase("TF") || param.equalsIgnoreCase("TF-IDF")){
                Task2 t2 = new Task2();
                g = t2.runTask2(param);
            }
        
            return g;
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }
}
