package edu.mwdb.project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class SimilarityAnalyzer {

	public boolean areCoauthors(String authorId1, String authorId2){
		Utility util = new Utility();
		Connection con = util.getDBConnection();
		
		try {
			PreparedStatement stmt = con.prepareStatement("SELECT * FROM coauthors WHERE personid1=? AND personid2=?");
			stmt.setInt(1, Integer.parseInt(authorId1));
			stmt.setInt(2, Integer.parseInt(authorId2));
			
			ResultSet rs = stmt.executeQuery();
			
			if(rs.first())
				return true;
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
//	public double[][] getAuthorSimilarityMatrix(){
//		
//	}
	
}
