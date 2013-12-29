package org.jmol.adapter.readers.cif;

import javajs.util.List;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.V3;

import org.jmol.api.SymmetryInterface;
import org.jmol.util.Logger;


class Subsystem {

  private MSReader msReader;
  private String code;
  private int d;
  private Matrix w;

  private SymmetryInterface symmetry;
  private Matrix[] modMatrices;

  Subsystem(MSReader msReader, String code, Matrix w) {
    this.msReader = msReader;
    this.code = code;
    this.w = w;
    d = w.getArray().length - 3;
  }

  public SymmetryInterface getSymmetry() {
    if (modMatrices == null)
      setSymmetry();
    return symmetry;
  }

  public Matrix[] getModMatrices() {
    if (modMatrices == null)
      setSymmetry();
    return modMatrices;
  }

  private void setSymmetry() {
    double[][] a;

    // Part 1: Get sigma_nu
    // van Smaalen, p. 92.

    Matrix w33 = w.getSubmatrix(0, 0, 3, 3);
    Matrix wd3 = w.getSubmatrix(3, 0, d, 3);
    Matrix w3d = w.getSubmatrix(0, 3, 3, d);
    Matrix wdd = w.getSubmatrix(3, 3, d, d);
    Matrix sigma = msReader.getSigma();
    Matrix sigma_nu = wdd.mul(sigma).add(wd3).mul(w3d.mul(sigma).add(w33).inverse());
    Matrix tFactor = wdd.sub(sigma_nu.mul(w3d)); 
    modMatrices = new Matrix[] { sigma_nu, tFactor };
    
    // Part 2: Get the new unit cell and symmetry operators

    SymmetryInterface s0 = msReader.cr.atomSetCollection.symmetry;
    V3[] vu43 = s0.getUnitCellVectors();
    V3[] vr43 = reciprocalsOf(vu43);

    // using full matrix math here:
    //
    // mar3  = just 3x3 matrix of ai* (a*, b*, c*)
    // mard3 = full (3+d)x3 matrix of ai* (a*, b*, c*, x4*, x5*,....)
    //       = [ mard3 | sigma * mard3 ]
    // 
    // We need top half of W*mard3 here
    
    Matrix mard3 = new Matrix(null, 3 + d, 3); 
    Matrix mar3 = new Matrix(null, 3, 3); 
    double[][] mard3a = mard3.getArray();
    double[][] mar3a = mar3.getArray();
    for (int i = 0; i < 3; i++)
      mard3a[i] = mar3a[i] = new double[] { vr43[i + 1].x, vr43[i + 1].y, vr43[i + 1].z };
    
    Matrix sx = sigma.mul(mar3);
    a = sx.getArray();
    for (int i = 0; i < d; i++)
      mard3a[i + 3] = a[i];
    a = w.mul(mard3).getArray();
    
    // back to vector notation and direct lattice
    
    V3[] uc_nu = new V3[4];
    uc_nu[0] = vu43[0]; // origin
    for (int i = 0; i < 3; i++)
      uc_nu[i + 1] = V3.new3((float) a[i][0], (float) a[i][1], (float) a[i][2]);    
    uc_nu = reciprocalsOf(uc_nu);
    symmetry = msReader.cr.symmetry.getUnitCell(uc_nu);

    // Part 3: Transform the operators 
    // 

    Matrix winv = w.inverse();
    Logger.info("[subsystem " + code + "]");
    symmetry.createSpaceGroup(-1, "[subsystem " + code + "]", new List<M4>());
    int nOps = s0.getSpaceGroupOperationCount();
    for (int iop = 0; iop < nOps; iop++) {
      Matrix rv = s0.getOperationRsVs(iop);
      Matrix r = w.mul(rv.getRotation()).mul(winv);
      Matrix v = w.mul(rv.getTranslation());
      String jf = symmetry.addOp(r, v);
      Logger.info(jf);
    }
  }

  private V3[] reciprocalsOf(V3[] abc) {
    V3[] rabc = new V3[4];
    rabc[0] = abc[0]; // origin
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
