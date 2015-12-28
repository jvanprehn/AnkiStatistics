package utils;

import java.io.PrintStream;

public class ArrayUtils {
	public static int[][] createIntMatrix(int m, int n) {
	    int[][] matrix = new int[m][]; 
	    for(int i=0; i<m; i++) {
	    	matrix[i] = new int[n];
	    	for(int j=0; j<n; j++)
	    		matrix[i][j] = 0;
	    }
	    
	    return matrix;
	}
	
	public static void formatMatrix(int[][] matrix, PrintStream s, String format) {
		for(int i=0; i<matrix.length; i++) {
	    	for(int j=0; j<matrix[i].length; j++)
	    		s.format(format, matrix[i][j]);
	    	s.println();
	    }
	}
}
