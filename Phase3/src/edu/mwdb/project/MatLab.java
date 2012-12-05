package edu.mwdb.project;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;
import matlabcontrol.extensions.MatlabNumericArray;
import matlabcontrol.extensions.MatlabTypeConverter;

public class MatLab {
	
	private static MatlabProxy proxy;
	public MatLab() {

	}
	
	public static MatlabProxy getProxy() throws MatlabConnectionException{
		if(proxy == null){
			MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder().setUsePreviouslyControlledSession(true).build();
			MatlabProxyFactory factory = new MatlabProxyFactory(options);
			proxy = factory.getProxy();
		}
		return proxy;
	}
	
	public static void closeConnection() throws MatlabInvocationException{
		if(proxy != null){
			//proxy.disconnect();
			proxy.exit();
		}
	}
	
	
	
	public double[][] svd(double[][] matrix, int rowsReturned) throws Exception {
		if (matrix.length == 0)
			throw new Exception("matrix cannot be empty");
		int columnSize = matrix[0].length;
		MatlabTypeConverter processor = new MatlabTypeConverter(getProxy());
		processor.setNumericArray("matrix", new MatlabNumericArray(matrix, null));
		
		getProxy().eval("[U,S,V]=svd(matrix);");
		double[][] resultSematicMatrixSVD = new double[rowsReturned][columnSize];
		for(int k=0;k<rowsReturned;k++)
		{
			Object[] obj=getProxy().returningEval("V(:,"+ (k+1) +")" ,1);
			resultSematicMatrixSVD[k]=(double[])obj[0];
		}	
		return resultSematicMatrixSVD;
	}
	
	
	/**
	 * Computes the SVD
	 * @param matrix
	 * @param rowsReturned - the top k rows of the matrix to be returned
	 * @return the v matrix (u,s,v) transposed from computing the svd on the input matrix containing only the top k rows (k=rowsReturned) 
	 * @throws Exception
	 */
	public double[][] svd_withtranspose(double[][] matrix, int rowsReturned) throws Exception {
		if (matrix.length == 0)
			throw new Exception("matrix cannot be empty");
		int columnSize = matrix[0].length;
		
		double[][] tempMatrix = new double[columnSize][columnSize];
		
		MatlabTypeConverter processor = new MatlabTypeConverter(getProxy());
		processor.setNumericArray("matrix", new MatlabNumericArray(matrix, null));
		
		getProxy().eval("[U,S,V]=svd(matrix);");
		for(int k=0;k<columnSize;k++)
		{
			Object[] obj=getProxy().returningEval("V(:,"+ (k+1) +")" ,1);
			tempMatrix[k]=(double[])obj[0];
		}
		
		if (tempMatrix[0].length < rowsReturned)
			throw new Exception("rowsReturned is greater than the number of rows available");
		
		// Transpose v matrix
		double[][] resultSematicMatrixSVD = new double[rowsReturned][columnSize];
		for(int a=0;a<rowsReturned;a++)
		{
			for(int b=0;b<columnSize;b++)
			{
				resultSematicMatrixSVD[a][b]= tempMatrix[b][a];
			}
		}
		
		return resultSematicMatrixSVD;
	}

	
	
	/**
	 * Computes the PCA of a matrix
	 * @param matrix
	 * @param rowsReturned - the top k rows of the matrix to be returned

	 */
	public double[][] pca(double[][] matrix, int rowsReturned) throws Exception {
		if (matrix.length == 0)
			throw new Exception("matrix cannot be empty");

		MatlabTypeConverter processor = new MatlabTypeConverter(getProxy());
		processor.setNumericArray("matrix", new MatlabNumericArray(matrix, null));

		// For PCA:
		getProxy().eval("[pc,score]=princomp(matrix);");
		int columnSize = matrix[0].length;
		double[][] resultSematicMatrixPCA = new double[rowsReturned][columnSize];

		for(int k=0;k<rowsReturned;k++)
		{
			Object[] obj=proxy.returningEval("pc(:,"+ (k+1) +")" ,1);
			resultSematicMatrixPCA[k]=(double[]) obj[0];
		}
		return resultSematicMatrixPCA;
	}
	
	
	
	
	/**
	 * Computes the PCA of a matrix
	 * @param matrix
	 * @param rowsReturned - the top k rows of the matrix to be returned
	 * @return the matrix is already transposed
	 * @throws Exception
	 */
	public double[][] pca_withtranspose(double[][] matrix, int rowsReturned) throws Exception {
		if (matrix.length == 0)
			throw new Exception("matrix cannot be empty");

		MatlabTypeConverter processor = new MatlabTypeConverter(getProxy());
		processor.setNumericArray("matrix", new MatlabNumericArray(matrix, null));

		// For PCA:
		getProxy().eval("[pc,score]=princomp(matrix);");
		int columnSize = matrix[0].length;
		double[][] tempMatrix = new double[columnSize][columnSize];

		for(int k=0;k<columnSize;k++)
		{
			Object[] obj=proxy.returningEval("pc(:,"+ (k+1) +")" ,1);
			tempMatrix[k]=(double[]) obj[0];
		}

		// Transpose
		double[][] resultSematicMatrixPCA = new double[columnSize][rowsReturned];
		for(int a=0;a<columnSize;a++)
		{
			for(int b=0;b<rowsReturned;b++)
			{
				resultSematicMatrixPCA[a][b]= tempMatrix[b][a];
			}
		}
		
		return resultSematicMatrixPCA;
	}
	
	/**
	 * Finds the top kNumber matches of the authKeywordMatrix in allUsersKeyWordMatrix
	 * @param allUsersKeyWordMatrix It's the matrix where all the information to be compared is to find the best matches
	 * @param authKeywordMatrix It's the input query matrix that will be compared to the allUsersKeyWordMatrix 
	 * @param kNumber How many results to be returned
	 * @return the top kNumber best matches to the authKeywordMatrix from allUsersKeyWordMatrix
	 * @throws MatlabInvocationException
	 * @throws MatlabConnectionException
	 * @throws Exception
	 */
	public Object[] knnSearch( double[][] allUsersKeyWordMatrix,  double[] authKeywordMatrix, int kNumber) throws MatlabInvocationException, MatlabConnectionException, Exception{
		DblpData db = new DblpData();

		double[][] usersPFMatrix = allUsersKeyWordMatrix;
		double[][] givenauthPFMatrix = new double[1][authKeywordMatrix.length];
		givenauthPFMatrix[0] = authKeywordMatrix;
		
		MatlabProxy proxy = MatLab.getProxy();
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);

		String currentPath = Utility.getCurrentFolder();
		proxy.eval("cd "+currentPath);
		
		processor.setNumericArray("givenAuthPFArray", new MatlabNumericArray(givenauthPFMatrix, null));
		processor.setNumericArray("usersMatrix", new MatlabNumericArray(usersPFMatrix, null));
		proxy.setVariable("kRange",kNumber);  
		
		Object[] objLDA = null;
		objLDA = proxy.returningEval("knnsearch( usersMatrix, givenAuthPFArray,'k', kRange,'Distance','cosine')",2);
		
		return  objLDA;
	}
}
