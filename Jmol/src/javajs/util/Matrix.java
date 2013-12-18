package javajs.util;

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

public class Matrix implements Cloneable {

  /* ------------------------
     Class variables
   * ------------------------ */

  /**
   * Array for internal storage of elements.
   * 
   */
  protected double[][] a;

  /**
   * Row and column dimensions.
   * 
   */
  protected int m, n;

  /* ------------------------
     Constructors
   * ------------------------ */

  /**
   * Construct a matrix quickly without checking arguments.
   * 
   * @param a
   *        Two-dimensional array of doubles.
   * @param m
   *        Number of rows.
   * @param n
   *        Number of colums.
   */

  public Matrix(double[][] a, int m, int n) {
    this.a = (a == null ? new double[m][n] : a);
    this.m = m;
    this.n = n;
  }

  /**
   * Make a deep copy of a matrix
   * 
   * @return copy
   */

  public Matrix copy() {
    Matrix x = new Matrix(null, m, n);
    double[][] c = x.a;
    for (int i = m; --i >= 0;)
      for (int j = n; --j >= 0;)
        c[i][j] = a[i][j];
    return x;
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
    return a;
  }

  /**
   * Copy the internal two-dimensional array.
   * 
   * @return Two-dimensional array copy of matrix elements.
   */

  public double[][] getArrayCopy() {
    double[][] x = new double[m][n];
    for (int i = m; --i >= 0;)
      for (int j = n; --j >= 0;)
        x[i][j] = a[i][j];
    return x;
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
    Matrix x = new Matrix(null, r, c);
    double[][] xa = x.a;
    for (int i = r; --i >= 0;)
      for (int j = c; --j >= 0;)
        xa[i][j] = a[i0 + i][j0 + j];
    return x;
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
   */

  public Matrix getMatrix(int[] r, int j0, int j1) {
    Matrix x = new Matrix(null, r.length, ++j1 - j0);
    double[][] xa = x.a;
    //      for (int i = 0; i < r.length; i++) {
    //        for (int j = j0; j <= j1; j++) {
    for (int i = r.length; --i >= 0;) {
      double[] b = a[r[i]];
      for (int j = j1; --j >= j0;)
        xa[i][j - j0] = b[j];
    }
    return x;
  }

  /**
   * Matrix transpose.
   * 
   * @return A'
   */

  public Matrix transpose() {
    Matrix x = new Matrix(null, n, m);
    double[][] c = x.a;
    for (int i = 0; i < m; i++)
      for (int j = 0; j < n; j++)
        c[j][i] = a[i][j];
    return x;
  }

  public Matrix add(Matrix b) {
    Matrix x = new Matrix(null, m, n);
    double[][] xa = x.a;
    double[][] ba = b.a;
    for (int i = m; --i >= 0;)
      for (int j = n; --j >= 0;)
        xa[i][j] = ba[i][j] + a[i][j];
    return x;
  }

  /**
   * Linear algebraic matrix multiplication, A * B
   * 
   * @param b
   *        another matrix
   * @return Matrix product, A * B
   * @exception IllegalArgumentException
   *            Matrix inner dimensions must agree.
   */

  public Matrix times(Matrix b) {
    if (b.m != n)
      return null;
    Matrix x = new Matrix(null, m, b.n);
    double[][] xa = x.a;
    double[][] ba = b.a;
    for (int j = b.n; --j >= 0;)
      for (int i = m; --i >= 0;) {
        double[] arowi = a[i];
        double s = 0;
        for (int k = n; --k >= 0;)
          s += arowi[k] * ba[k][j];
        xa[i][j] = s;
      }
    return x;
  }

  /**
   * Matrix inverse or pseudoinverse
   * 
   * @return inverse(A) if A is square, pseudoinverse otherwise.
   */

  public Matrix inverse() {
    try {
      return new LUDecomp().solve(identity(m, m));
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Matrix trace.
   * 
   * @return sum of the diagonal elements.
   */

  public double trace() {
    double t = 0;
    for (int i = Math.min(m, n); --i >= 0;)
      t += a[i][i];
    return t;
  }

  /**
   * Generate identity matrix
   * 
   * @param m
   *        Number of rows.
   * @param n
   *        Number of columns.
   * @return An m-by-n matrix with ones on the diagonal and zeros elsewhere.
   */

  public static Matrix identity(int m, int n) {
    Matrix x = new Matrix(null, m, n);
    double[][] xa = x.a;
    for (int i = m; --i >= 0;)
      for (int j = n; --j >= 0;)
        xa[i][j] = (i == j ? 1.0 : 0.0);
    return x;
  }

  @Override
  public String toString() {
    String s = "[\n";
    for (int i = 0; i < m; i++) {
      s += "  [";
      for (int j = 0; j < n; j++)
        s += " " + a[i][j];
      s += "]\n";
    }
    s += "]";
    return s;
  }

  /**
   * 
   * Edited down by Bob Hanson for minimum needed by Jmol -- just constructor
   * and solve
   * 
   * LU Decomposition.
   * <P>
   * For an m-by-n matrix A with m >= n, the LU decomposition is an m-by-n unit
   * lower triangular matrix L, an n-by-n upper triangular matrix U, and a
   * permutation vector piv of length m so that A(piv,:) = L*U. If m < n, then L
   * is m-by-m and U is m-by-n.
   * <P>
   * The LU decompostion with pivoting always exists, even if the matrix is
   * singular, so the constructor will never fail. The primary use of the LU
   * decomposition is in the solution of square systems of simultaneous linear
   * equations. This will fail if isNonsingular() returns false.
   */

  private class LUDecomp {

    /* ------------------------
       Class variables
     * ------------------------ */

    /**
     * Array for internal storage of decomposition.
     * 
     */
    private double[][] LU;

    /**
     * Internal storage of pivot vector.
     * 
     */
    private int[] piv;

    private int pivsign;

    /* ------------------------
       Constructor
     * ------------------------ */

    /**
     * LU Decomposition Structure to access L, U and piv.
     * 
     */

    LUDecomp() {

      // Use a "left-looking", dot-product, Crout/Doolittle algorithm.

      LU = getArrayCopy();
      piv = new int[m];
      for (int i = m; --i >= 0;)
        piv[i] = i;
      pivsign = 1;
      double[] LUrowi;
      double[] LUcolj = new double[m];

      // Outer loop.

      for (int j = 0; j < n; j++) {

        // Make a copy of the j-th column to localize references.

        for (int i = m; --i >= 0;)
          LUcolj[i] = LU[i][j];

        // Apply previous transformations.

        for (int i = m; --i >= 0;) {
          LUrowi = LU[i];

          // Most of the time is spent in the following dot product.

          int kmax = Math.min(i, j);
          double s = 0.0;
          for (int k = kmax; --k >= 0;)
            s += LUrowi[k] * LUcolj[k];

          LUrowi[j] = LUcolj[i] -= s;
        }

        // Find pivot and exchange if necessary.

        int p = j;
        for (int i = m; --i > j;)
          if (Math.abs(LUcolj[i]) > Math.abs(LUcolj[p]))
            p = i;
        if (p != j) {
          for (int k = n; --k >= 0;) {
            double t = LU[p][k];
            LU[p][k] = LU[j][k];
            LU[j][k] = t;
          }
          int k = piv[p];
          piv[p] = piv[j];
          piv[j] = k;
          pivsign = -pivsign;
        }

        // Compute multipliers.

        if (j < m & LU[j][j] != 0.0)
          for (int i = m; --i > j;)
            LU[i][j] /= LU[j][j];
      }
    }

    /* ------------------------
       default Methods
     * ------------------------ */

    /**
     * Solve A*X = B
     * 
     * @param b
     *        A Matrix with as many rows as A and any number of columns.
     * @return X so that L*U*X = B(piv,:) or null for exception
     * @throws Exception
     *         if singular
     */

    Matrix solve(Matrix b) throws Exception {
      if (b.m != m)
        return null;
      for (int j = 0; j < n; j++)
        if (LU[j][j] == 0)
          throw new Exception("Matrix is singular.");

      // Copy right hand side with pivoting
      int nx = b.getColumnDimension();
      Matrix x = b.getMatrix(piv, 0, nx - 1);
      double[][] a = x.a;

      // Solve L*Y = B(piv,:)
      for (int k = 0; k < n; k++) {
        for (int i = k + 1; i < n; i++) {
          for (int j = 0; j < nx; j++) {
            a[i][j] -= a[k][j] * LU[i][k];
          }
        }
      }
      // Solve U*X = Y;
      for (int k = n - 1; k >= 0; k--) {
        for (int j = 0; j < nx; j++) {
          a[k][j] /= LU[k][k];
        }
        for (int i = 0; i < k; i++) {
          for (int j = 0; j < nx; j++) {
            a[i][j] -= a[k][j] * LU[i][k];
          }
        }
      }
      return x;
    }
  }

}
