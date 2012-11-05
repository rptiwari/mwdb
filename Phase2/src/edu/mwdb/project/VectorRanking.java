package edu.mwdb.project;
	public class VectorRanking implements Comparable<VectorRanking>{
		

	 
		public Integer documentID;
		public double  howSimilar;
	//	public termFreqVectors vectorTermFreq = new termFreqVectors();
		
						//hash table with keys docid and values termFreqVectors;
		
		
		public void documentVector() {
		
		}

						@Override
						public int compareTo(VectorRanking anyDocVector) {
							
							    double difference  =  this.howSimilar - ( anyDocVector.howSimilar); 
							    if (difference > 0){ difference = 1;}
							    else if (difference < 0) {difference = -1;}
							    return (int) difference; 
							    
							
						}

		



	}




