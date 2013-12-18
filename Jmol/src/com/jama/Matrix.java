package com.jama;

/**
 * 
 * abbreviated for Jmol by Bob Hanson
 * 
 * Jama = Java Matrix class.
 * <P>
 * The Java Matrix Class provides the fundamental operations of numerical linear
 * algebra. Various constructors create Matrices from two dimensional arrays of
 * double precision floating point numbers. Various "gets" and "sets" provide
 * access to submatrices and matrix elements. Several methods implement basic
 * matrix arithmetic, including matrix addition and multiplication, matrix
 * norms, and element-by-element array operations. Methods for reading and
 * printing matrices are also included. All the operations in this version of
 * the Matrix Class involve real matrices. Complex matrices may be handled in a
 * future version.
 * <P>
 * Five fundamental matrix decompositions, which consist of pairs or triples of
 * matrices, permutation vectors, and the like, produce results in five
 * decomposition classes. These decompositions are accessed by the Matrix class
 * to compute solutions of simultaneous linear equations, determinants, inverses
 * and other matrix functions. The five decompositions are:
 * <P>
 * <UL>
 * <LI>Cholesky Decomposition of symmetric, positive definite matrices.
 * <LI>LU Decomposition of rectangular matrices.
 * <LI>QR Decomposition of rectangular matrices.
 * <LI>Singular Value Decomposition of rectangular matrices.
 * <LI>Eigenvalue Decomposition of both symmetric and nonsymmetric square
 * matrices.
 * </UL>
 * <DL>
 * <DT><B>Example of use:</B></DT>
 * <P>
 * <DD>Solve a linear system A x = b and compute the residual norm, ||b - A x||.
 * <P>
 * 
 * <PRE>
 * double[][] vals = { { 1., 2., 3 }, { 4., 5., 6. }, { 7., 8., 10. } };
 * Matrix A = new Matrix(vals);
 * Matrix b = Matrix.random(3, 1);
 * Matrix x = A.solve(b);
 * Matrix r = A.times(x).minus(b);
 * double rnorm = r.normInf();
 * </PRE>
 * 
 * </DD>
 * </DL>
 * 
 * @author The MathWorks, Inc. and the National Institute of Standards and
 *         Technology.
 * @version 5 August 1998
 */

public class Matrix implements Cloneable, java.io.Serializable {

  /* ------------------------
     Class variables
   * ------------------------ */

  /**
   * Array for internal storage of elements.
   * 
   * @serial internal array storage.
   */
  private double[][] A;

  /**
   * Row and column dimensions.
   * 
   * @serial row dimension.
   * @serial column dimension.
   */
  private int m, n;

  /* ------------------------
     Constructors
   * ------------------------ */

  /**
   * Construct a matrix quickly without checking arguments.
   * 
   * @param A
   *        Two-dimensional array of doubles.
   * @param m
   *        Number of rows.
   * @param n
   *        Number of colums.
   */

  public Matrix(double[][] A, int m, int n) {
    this.A = (A == null ? new double[m][n] : A);
    this.m = m;
    this.n = n;
  }

  /**
   * Make a deep copy of a matrix
   * @return copy
   */

  public Matrix copy() {
    Matrix X = new Matrix(null, m, n);
    double[][] C = X.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = A[i][j];
      }
    }
    return X;
  }

  /**
   * Clone the Matrix object.
   */

  @Override
  public Object clone() {
    return this.copy();
  }

  /**
   * Access the internal two-dimensional array.
   * 
   * @return Pointer to the two-dimensional array of matrix elements.
   */

  public double[][] getArray() {
    return A;
  }

  /**
   * Copy the internal two-dimensional array.
   * 
   * @return Two-dimensional array copy of matrix elements.
   */

  public double[][] getArrayCopy() {
    double[][] C = new double[m][n];
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = A[i][j];
      }
    }
    return C;
  }

  /**
   * Get row dimension.
   * 
   * @return m, the number of rows.
   */

  public int getRowDimension() {
    return m;
  }

  /**
   * Get column dimension.
   * 
   * @return n, the number of columns.
   */

  public int getColumnDimension() {
    return n;
  }

  /**
   * Get a submatrix.
   * 
   * @param i0
   *        Initial row index
   * @param j0
   *        Initial column index
   * @param r
   *        Number of rows
   * @param c
   *        Number of columns
   * @return A(i0:i1,j0:j1)
   * 
   */

  public Matrix getMatrix4(int i0, int j0, int r, int c) {
    Matrix a = new Matrix(null, r, c);
    double[][] B = a.getArray();
    for (int i = r; --i >= 0;)
      for (int j = c; --j >= 0;)
        B[i][j] = A[i0 + i][j0 + j];
    return a;
  }


  /**
   * Get a submatrix.
   * 
   * @param r
   *        Array of row indices.
   * @param j0
   *        Initial column index
   * @param j1
   *        Final column index
   * @return A(r(:),j0:j1)
   * @exception ArrayIndexOutOfBoundsException
   *            Submatrix indices
   */

  public Matrix getMatrix(int[] r, int j0, int j1) {
    Matrix X = new Matrix(null, r.length, j1 - j0 + 1);
    double[][] B = X.getArray();
    try {
      for (int i = 0; i < r.length; i++) {
        for (int j = j0; j <= j1; j++) {
          B[i][j - j0] = A[r[i]][j];
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new ArrayIndexOutOfBoundsException("Submatrix indices");
    }
    return X;
  }



//  /**
//   * Matrix transpose.
//   * 
//   * @return A'
//   */
//
//  public Matrix transpose() {
//    Matrix X = new Matrix(null, n, m);
//    double[][] C = X.getArray();
//    for (int i = 0; i < m; i++) {
//      for (int j = 0; j < n; j++) {
//        C[j][i] = A[i][j];
//      }
//    }
//    return X;
//  }

  public Matrix add(Matrix b) {
    Matrix a = new Matrix(null, m, n);
    for (int i = 0; i < m; i++)
      for (int j = 0; j < n; j++)
        a.A[i][j] = b.A[i][j] + A[i][j];
    return a;
  }

  /**
   * Linear algebraic matrix multiplication, A * B
   * 
   * @param B
   *        another matrix
   * @return Matrix product, A * B
   * @exception IllegalArgumentException
   *            Matrix inner dimensions must agree.
   */

  public Matrix times(Matrix B) {
    if (B.m != n) {
      throw new IllegalArgumentException("Matrix inner dimensions must agree.");
    }
    Matrix X = new Matrix(null, m, B.n);
    double[][] C = X.getArray();
    double[] Bcolj = new double[n];
    for (int j = 0; j < B.n; j++) {
      for (int k = 0; k < n; k++) {
        Bcolj[k] = B.A[k][j];
      }
      for (int i = 0; i < m; i++) {
        double[] Arowi = A[i];
        double s = 0;
        for (int k = 0; k < n; k++) {
          s += Arowi[k] * Bcolj[k];
        }
        C[i][j] = s;
      }
    }
    return X;
  }

  /**
   * Matrix inverse or pseudoinverse
   * 
   * @return inverse(A) if A is square, pseudoinverse otherwise.
   */

  public Matrix inverse() {
    return new LUDecomposition(this).solve(identity(m, m));
  }

  /**
   * Matrix trace.
   * 
   * @return sum of the diagonal elements.
   */

  public double trace() {
    double t = 0;
    for (int i = 0; i < Math.min(m, n); i++) {
      t += A[i][i];
    }
    return t;
  }

  /**
   * Generate identity matrix
   * 
   * @param m
   *        Number of rows.
   * @param n
   *        Number of colums.
   * @return An m-by-n matrix with ones on the diagonal and zeros elsewhere.
   */

  public static Matrix identity(int m, int n) {
    Matrix A = new Matrix(null, m, n);
    double[][] X = A.getArray();
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        X[i][j] = (i == j ? 1.0 : 0.0);
      }
    }
    return A;
  }

  @Override
  public String toString() {
    String s = "[\n";
    for (int i = 0; i < m; i++) {
      s += "  [";
      for (int j = 0; j < n; j++)
        s += " " + A[i][j];
      s += "]\n";
    }
    s += "]";
    return s;
  }
}
