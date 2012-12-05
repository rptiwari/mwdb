

package edu.mwdb.project;

class Edge implements Comparable {

        int i, j;
        double weight;
        
        public Edge(int a, int b, double c){
            i = a;
            j = b;
            weight = c;            
        }
        
        @Override
        public int compareTo(Object t) {
            return Double.compare(this.weight, ((Edge)t).weight);
        }
}