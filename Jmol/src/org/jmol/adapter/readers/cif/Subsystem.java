package org.jmol.adapter.readers.cif;

import javajs.util.M3;
import javajs.util.M4;
import javajs.util.V3;

import org.jmol.api.SymmetryInterface;
import org.jmol.util.Logger;

import com.jama.Matrix;

class Subsystem {

  private String code;
  SymmetryInterface symmetry;
  private MSReader msReader;
  private M3 q123;
  
  Matrix w;
  private int d;

  Subsystem(MSReader msReader, String code, int[][] iw) {
    this.msReader = msReader;
    this.code = code;
    int n = iw.length;
    d = n - 3;
    double[][] a = new double[n][n];
    for (int i = n; --i >= 0;)
      for (int j = n; --j >= 0;)
        a[i][j] = iw[i][j];
    w = new Matrix(a, n, n);
    if (w.trace() == n)
      w = null;
  }

  public SymmetryInterface getSymmetry() {
    if (symmetry == null)
      setSymmetry();
    return symmetry;
  }

  public M3 getQ123() {
    if (q123 == null)
      setSymmetry();
    return q123;
  }

  private void setSymmetry() {
    double[][] a = w.getArray();
    float[] t = new float[3];

    // Part 1: Get sigma_nu
    // van Smaalen, p. 92.

    a = new double[d][3];
    for (int i = d; --i >= 0;) {
      msReader.q123.getRow(i, t);
      a[i] = new double[] { t[0], t[1], t[2] };
    }
    Matrix sd3 = new Matrix(a, d, 3);
    Matrix w33 = w.getMatrix4(0, 0, 3, 3);
    Matrix w3d = w.getMatrix4(0, 3, 3, d);
    Matrix wd3 = w.getMatrix4(3, 0, d, 3);
    Matrix wdd = w.getMatrix4(3, 3, d, d);
    Matrix sd3new = wdd.times(sd3).add(wd3)
        .times(w3d.times(sd3).add(w33).inverse());
    q123 = new M3();
    a = sd3new.getArray();
    for (int i = d; --i >= 0;)
      q123.setRow(i, (float) a[i][0], (float) a[i][1], (float) a[i][2]);

    // Part 2: Get the new unit cell and symmetry operators (not!)

    a = w.getArray();

    V3[] vu43 = msReader.cr.symmetry.getUnitCellVectors();
    V3[] vr43 = reciprocalsOf(vu43);

    // for unit cell we need an origin and three vectors
    V3[] vnewq = new V3[3];
    V3[] vnewu = new V3[4];
    vnewu[0] = vr43[0];
    for (int i = 0; i < 3; i++) {
      vnewq[i] = new V3();
      vnewu[i + 1] = new V3();
    }

    // add in vectors from unit cell using W33 block
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
        vnewu[i + 1].scaleAdd2((float) a[i][j], vr43[j + 1], vnewu[i + 1]);

    // add in vectors from W3d block (assumed d <= 3 here)
    V3 vt = new V3();
    for (int j = 3; j < 3 + d; j++) {
      msReader.q123.getRow(j - 3, t); //  t is a wave vector
      for (int i = 0; i < 3; i++) {
        vt.setT(vr43[i + 1]); // vt is a reciprocal vector
        vt.scale(t[i]);
        vnewu[i + 1].scaleAdd2((float) a[i][j], vt, vnewu[i + 1]);
      }
    }
    vnewu = reciprocalsOf(vnewu);
    symmetry = msReader.cr.symmetry.getUnitCell(vnewu);

    // Part 3: Transform the operators 
    // 

    Matrix winv = w.inverse();
    symmetry.createSpaceGroup(-1, "P1", symmetry.getNotionalUnitCell());
    SymmetryInterface s0 = msReader.cr.atomSetCollection.symmetry;
    int nOps = s0.getSpaceGroupOperationCount();
    for (int iop = 0; iop < nOps; iop++) {
      M3 gammaE = new M3();
      M4 op = s0.getSpaceGroupOperation(iop);
      op.getRotationScale(gammaE);
      M4 gammaIS =s0.getOperationGammaIS(iop);
//      System.out.println(gammaIS);
//      System.out.println(gammaE);
//      System.out.println(op);
      a = new double[3 + d][3 + d];
      double[][] v = new double[3 + d][1];
      loadArray(a, op, 0, 3, d);
      loadArray(a, gammaIS, 3, d, 3);
      Matrix r = new Matrix(a, 3 + d, 3 + d);
      for (int i = 0; i < 3; i++)
        v[i][0] = op.getElement(i, 3);
      for (int i = 0; i < d; i++)
        v[i + 3][0] = gammaIS.getElement(i, 3);
      
      r = w.times(r).times(winv);
      Matrix m2 = w.times(new Matrix(v, 3 + d, 1));
      String jf = symmetry.addOp(r.getArray(), m2.getArray());
      Logger.info(jf);
    }
  }

  private void loadArray(double[][] a, M4 op, int ij0, int max, int off) {
    float[] t = new float[16];
    op.toA(t);
    int pt = 0;
    for (int i = 0; i < max; i++) {
      for (int j = 0; j < max; j++)
        a[i + ij0][j + ij0] = t[pt++];
      pt += off;
    }
  }

  private V3[] reciprocalsOf(V3[] abc) {
    V3[] rabc = new V3[4];
    rabc[0] = abc[0];
    for (int i = 0; i < 3; i++) {
      rabc[i + 1] = new V3();
      rabc[i + 1].cross(abc[((i + 1) % 3) + 1], abc[((i + 2) % 3) + 1]);
      rabc[i + 1].scale(1/abc[i + 1].dot(rabc[i + 1]));
    }
    return rabc;
  }
  
  @Override
  public String toString() {
    return "Subsystem " + code + "\n" + w;
  }
}
