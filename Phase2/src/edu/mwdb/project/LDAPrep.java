package edu.mwdb.project;
import java.awt.List;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


/*
 *  1.0 Altered index for word lookup for matlab to be +1 to match matlab's indices
 *  Class that does pre-processing  and post-processing for LDA
 *  pre-processing on data matrix to create
 *  the vectors required for the latent semantics lda toolbox
 *  used in Matlab
 *  
 *  WS vector has an entry for each occurrence of a term in a document
 *  DS vector has an entry for each occurrence of a term and identifies in which document this term is occurring
 *  The 2 vectors are parallel arrays with the indices corresponding   
 */

public class LDAPrep {
	String[] WO = null;
	double[] WS = null;
	double[] DS = null;


	public void doLDAPrepFullMatrix(double[][] dataMatrix){


		/* create parallel arrays based upon input matrix */	

		ArrayList<Double> WSarray = new ArrayList<Double>();
		ArrayList<Double> DSarray = new ArrayList<Double>();

		int docIndex = 0;
		int row=0;
		int column = 0;
		int columnsize = dataMatrix[0].length;
		int rowsize = dataMatrix.length;
		for (int i = 0; i < rowsize; i++){
			for (int j = 0; j < columnsize; j++){
				if (dataMatrix[i][j] > 0){

					for (int termcount = 0; termcount < dataMatrix[i][j]; termcount++){

						double wordindex = (j+1);
						WSarray.add((double)wordindex);			//add the word index into the vocabulary index
						DSarray.add((double)(i+1));			// document Index to show WO{j} is found in DS(i+1) document
					}
				}
			}
		}
		WS = new double[WSarray.size()];
		DS = new double[WSarray.size()];

		/* create vectors to be used as inputs to MatLab lda */
		int index =0;
		for (Double wordfreq : WSarray){
			WS[index] = wordfreq;
			index++;
		}

		index =0;
		for (Double wordfreq : DSarray){
			DS[index] = wordfreq;
			index++;
		}
		/* Alternative method to create double[][] from Double ArrayList 
		Double[] DS2 = new Double[WSarray.size()]; 
		Double[] WS2 = new Double[WSarray.size()]; 

		WS2 = (Double[])WSarray.toArray(new Double[WSarray.size()]);
		DS2 = (Double[])DSarray.toArray(new Double[DSarray.size()]);

		DS = new double[WSarray.size()];
		/* unbox Double to double for use in Matlab */
		/*
		for (int i = 0; i < WSarray.size(); i++){
			DS[i] = DS2[i];
			WS[i] = WS2[i];
	    }
		 */
	}			//end do prep

	public void makeDictionaryFile(String[] vocab) throws IOException {

		String filename = "c:\\USERS\\Katherine\\Documents\\MATLAB\\words.mat"; 

		File f = new File(filename);
		if (!f.exists()) {
			f.createNewFile();
		}  
		FileWriter fw = new FileWriter(f.getAbsoluteFile());
		BufferedWriter writer = new BufferedWriter(fw);
		String line;

		for(int i = 0; i < vocab.length; i++){
			line = vocab[i].toLowerCase();
			vocab[i] = line;
			writer.write(line);

			writer.newLine();
		}

		writer.close();

	}

	/*
	 * helper method that retrieves data from writetopics matlab function output
	 * It assumes numImportantTopics  is not less than number of topics
	 */
/*
 *  Prints topics to screen while retrieving from data file
 */

	public ArrayList[] readPrintTopics(String filename, int numImportantTopics, int numRelevantWords){
		ArrayList<KeywordConfig> wordsProbabilities = null;
		ArrayList[] topics = new ArrayList[numImportantTopics];
		try {
			Scanner sc = new Scanner(new File(filename));
			Scanner scLine = null;
			int i = 0;
			int wordCount = 0;
			int topicCount = 0;
			
			while (sc.hasNextLine()){
				i = topicCount;
				String line = sc.nextLine();
				scLine = new Scanner(line);
				while (scLine.hasNext()){
					if (i < numImportantTopics){
					
					KeywordConfig wordProb = new KeywordConfig();
					String word = scLine.next();

					wordProb.setKeyword(word.toLowerCase());
					Float prob = scLine.nextFloat();
					wordProb.setWeightedFreq(prob);

					if (wordCount > 0){
					System.out.printf("%-33.20s    %-10.5f ", word, prob);
					if (topics[i]== null){
						wordsProbabilities = new ArrayList<KeywordConfig>();
					}
					else
						wordsProbabilities = topics[i];

					wordsProbabilities.add(wordProb);
					topics[i] = wordsProbabilities;  // necessary?
					i++;
					}
					else{
						System.out.printf("%-30.20s    ", word);}
					
					
					}
					else break; // skip rest of topics
					
				}
				System.out.println();
				wordCount++;
				if (wordCount > numRelevantWords){
					wordCount =0;
					int k = i;
					topicCount = i;
					i= k+i;
					System.out.println();
				}
			}
			sc.close();
			scLine.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		return topics;

	}



	public ArrayList[] readPrintTopics2(String filename, int numImportantTopics, int numRelevantWords){
		ArrayList<KeywordConfig> wordsProbabilities = null;
		ArrayList[] topics = new ArrayList[numImportantTopics];
		try {
			Scanner sc = new Scanner(new File(filename));
			Scanner scLine = null;
			int i = 0;
			int wordCount = 0;
			int topicCount = 0;

			while (sc.hasNextLine()){
				i = topicCount;
				String line = sc.nextLine();
				scLine = new Scanner(line);
				while (scLine.hasNext()){
					if (i < numImportantTopics){

						KeywordConfig wordProb = new KeywordConfig();
						String word = scLine.next();

						wordProb.setKeyword(word.toLowerCase());
						Float prob = scLine.nextFloat();
						wordProb.setWeightedFreq(prob);

						if (wordCount > 0){
//							System.out.printf("%-33.20s    %-10.5f ", word, prob);
							if (topics[i]== null){
								wordsProbabilities = new ArrayList<KeywordConfig>();
							}
							else
								wordsProbabilities = topics[i];

							wordsProbabilities.add(wordProb);
							topics[i] = wordsProbabilities;  // necessary?
							i++;
						}
						else{
//							System.out.printf("%-30.20s    ", word);
							}


					}
					else break; // skip rest of topics

				}
				System.out.println();
				wordCount++;
				if (wordCount > numRelevantWords){
					wordCount =0;
					int k = i;
					topicCount = i;
					i= k+i;
					System.out.println();
				}
			}
			sc.close();
			scLine.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		return topics;

	}

}
