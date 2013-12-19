package org.jmol.adapter.readers.cif;

import javajs.util.List;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.V3;

import org.jmol.api.SymmetryInterface;
import org.jmol.util.Logger;


class Subsystem {

  private String code;
  SymmetryInterface symmetry;
  private MSReader msReader;
  private Matrix sigma_nu;
  
  Matrix w;
  private int d;

  Subsystem(MSReader msReader, String code, Matrix w) {
    this.msReader = msReader;
    this.code = code;
    this.w = w;
    d = w.getArray().length - 3;
  }

  public SymmetryInterface getSymmetry() {
    if (symmetry == null)
      setSymmetry();
    return symmetry;
  }

  public Matrix getSigma() {
    if (sigma_nu == null)
      setSymmetry();
    return sigma_nu;
  }

  private void setSymmetry() {
    double[][] a = w.getArray();
    double[] t;

    // Part 1: Get sigma_nu
    // van Smaalen, p. 92.

    Matrix sigma = msReader.getSigma();
    Matrix w33 = w.getSubmatrix(0, 0, 3, 3);
    Matrix w3d = w.getSubmatrix(0, 3, 3, d);
    Matrix wd3 = w.getSubmatrix(3, 0, d, 3);
    Matrix wdd = w.getSubmatrix(3, 3, d, d);
    sigma_nu = wdd.mul(sigma).add(wd3)
        .mul(w3d.mul(sigma).add(w33).inverse());

    // Part 2: Get the new unit cell and symmetry operators (not!)

    a = w.getArray();

    SymmetryInterface s0 = msReader.cr.atomSetCollection.symmetry;
    V3[] vu43 = s0.getUnitCellVectors();
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
    double[][] sa = msReader.getSigma().getArray();
    for (int j = 3; j < 3 + d; j++) {
      t = sa[j - 3]; //  t is a wave vector
      for (int i = 0; i < 3; i++) {
        vt.setT(vr43[i + 1]); // vt is a reciprocal vector
        vt.scale((float)t[i]);
        vnewu[i + 1].scaleAdd2((float) a[i][j], vt, vnewu[i + 1]);
      }
    }
    vnewu = reciprocalsOf(vnewu);
    symmetry = msReader.cr.symmetry.getUnitCell(vnewu);

    // Part 3: Transform the operators 
    // 

    Matrix winv = w.inverse();
    symmetry.createSpaceGroup(-1, "[subsystem " + code + "]", new List<M4>());
    int nOps = s0.getSpaceGroupOperationCount();
    for (int iop = 0; iop < nOps; iop++) {
      Matrix rv = s0.getOperationRsVs(iop);
      String jf = symmetry.addOp(w.mul(rv.getRotation()).mul(winv), w.mul(rv.getTranslation()));
      Logger.info(jf);
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
