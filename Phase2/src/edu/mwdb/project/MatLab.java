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
	
	/**
	 * Computes the SVD
	 * @param matrix
	 * @param rowsReturned - the top k rows of the matrix to be returned
	 * @return the v matrix (u,s,v) transposed from computing the svd on the input matrix containing only the top k rows (k=rowsReturned) 
	 * @throws Exception
	 */
	public double[][] svd(double[][] matrix, int rowsReturned) throws Exception {
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
	 * @return the matrix is already transposed
	 * @throws Exception
	 */
	public double[][] pca(double[][] matrix, int rowsReturned) throws Exception {
		if (matrix.length == 0)
			throw new Exception("matrix cannot be empty");

		// Connecting to the Matlab
		MatlabProxyFactory factory = new MatlabProxyFactory();
		MatlabProxy proxy = factory.getProxy();

		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
		processor.setNumericArray("matrix", new MatlabNumericArray(matrix, null));

		// For PCA:
		proxy.eval("[pc,score]=princomp(matrix);");
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
}
