package org.jmol.adapter.readers.cif;

import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.V3;

import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.util.Logger;


class Subsystem {

  private MSRdr msRdr;
  private String code;
  private int d;
  private Matrix w;

  private SymmetryInterface symmetry;
  private Matrix[] modMatrices;
  private boolean isFinalized;

  Subsystem(MSRdr msRdr, String code, Matrix w) {
    this.msRdr = msRdr;
    this.code = code;
    this.w = w;
    d = w.getArray().length - 3;
  }

  public SymmetryInterface getSymmetry() {
    if (!isFinalized)
      setSymmetry(true);
    return symmetry;
  }

  public Matrix[] getModMatrices() {
    if (!isFinalized)
      setSymmetry(true);
    return modMatrices;
  }

  private void setSymmetry(boolean setOperators) {
    double[][] a;

    // Part 1: Get sigma_nu
    // van Smaalen, p. 92.

    Logger.info("[subsystem " + code + "]");

    Matrix winv = w.inverse();
    Logger.info("w=" + w);
    Logger.info("w_inv=" + winv);

    Matrix w33 = w.getSubmatrix(0, 0, 3, 3);
    Matrix wd3 = w.getSubmatrix(3, 0, d, 3);
    Matrix w3d = w.getSubmatrix(0, 3, 3, d);
    Matrix wdd = w.getSubmatrix(3, 3, d, d);
    Matrix sigma = msRdr.getSigma();
    Matrix sigma_nu = wdd.mul(sigma).add(wd3).mul(w3d.mul(sigma).add(w33).inverse());
    Matrix tFactor = wdd.sub(sigma_nu.mul(w3d)); 
    
    Logger.info("sigma_nu = " + sigma_nu);
    
    // Part 2: Get the new unit cell and symmetry operators

    SymmetryInterface s0 = msRdr.cr.asc.getSymmetry();
    V3[] vu43 = s0.getUnitCellVectors();
    V3[] vr43 = reciprocalsOf(vu43);

    // using full matrix math here:
    //
    // mar3  = just 3x3 matrix of ai* (a*, b*, c*)
    // mard3 = full (3+d)x3 matrix of ai* (a*, b*, c*, x4*, x5*,....)
    //       = [ mard3 | sigma * mard3 ]
    // 
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
    symmetry = Interface.getSymmetry().getUnitCell(uc_nu, false);
    modMatrices = new Matrix[] { sigma_nu, tFactor };
    if (!setOperators)
      return;
    isFinalized = true;

    // Part 3: Transform the operators 
    // 

    Logger.info("unit cell parameters: " + symmetry.getUnitCellInfo());
    symmetry.createSpaceGroup(-1, "[subsystem " + code + "]", new Lst<M4>());
    int nOps = s0.getSpaceGroupOperationCount();
    for (int iop = 0; iop < nOps; iop++) {
      Matrix rv = s0.getOperationRsVs(iop);
      Matrix r0 = rv.getRotation();
      Matrix v0 = rv.getTranslation();
      Matrix r = w.mul(r0).mul(winv);
      Matrix v = w.mul(v0);
      String code = this.code;
      if(isMixed(r)) {
        // This operator mixes x4,x5... into x1,x2,x3. 
        // It is not a pure operation. Instead, it correlates one
        // subsystem with another. Our job is to find the other
        // subsystem "j" that will satisfy the following condition:
        //
        // (Wj R Wi_inv).submatrix(0,3,3,d) == all_zeros
        //
        for (Entry<String, Subsystem> e: msRdr.htSubsystems.entrySet()){
          Subsystem ss = e.getValue();
          if (ss == this)
            continue;
          Matrix rj = ss.w.mul(r0).mul(winv);
          if (!isMixed(rj)) {
            // We have found the corresponding subsystem.
            // The result of this operation will be in other system,
            // and the operation itself will be used to rotate the modulation.
            // ss.code will be used to mark any atom created by this operation as
            // part of the other system.
            r = rj;
            v = ss.w.mul(v0);
            code = ss.code;
            break;
          }
        }
      }
      String jf = symmetry.addOp(code, r, v, sigma_nu);      
      Logger.info(this.code + "." + (iop + 1) + (this.code.equals(code) ? "   " : ">" + code + " ") + jf);
    }
  }

  private boolean isMixed(Matrix r) {
    double[][] a = r.getArray();
    for (int i = 3; --i >= 0;)
      for (int j = 3 + d; --j >= 3;)
        if (a[i][j] != 0)
          return true;
    return false;
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
