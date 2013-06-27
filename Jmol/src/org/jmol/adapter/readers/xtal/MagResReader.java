package org.jmol.adapter.readers.xtal;

/**
 * Piero Canepa
 * 
 * Quantum Espresso
 * http://www.quantum-espresso.org and http://qe-forge.org/frs/?group_id=10
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
 * 
 * @version 1.0
 */

import java.util.Hashtable;
import java.util.Map;

//Random comment

import org.jmol.util.Tensor;
import org.jmol.util.TextFormat;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.util.Eigen;

public class MagResReader extends AtomSetCollectionReader {

  private float[] cellParams;
  private float maxIso = 10000;
  private String tensorTypes = "";
  private boolean isNew;
  private Map<String, String> mapUnits = new Hashtable<String, String>();

  @Override
  protected void initializeReader() {
    setFractionalCoordinates(false);
    doApplySymmetry = false;
    atomSetCollection.newAtomSet();
  }

  @Override
  protected void finalizeReader() throws Exception {
    doApplySymmetry = true;
    finalizeReaderASCR();
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (!isNew && line.indexOf("<calculation>") >= 0) {
      isNew = true;
      ignoreFileSpaceGroupName = true;
      //setSpaceGroupName("P1");
    }
    if (cellParams == null && line.startsWith("lattice")) {
      readCellParams();
      return true;
    }
    if (isNew) {
      if (line.startsWith("units")) {
        setUnitsNew();
      } else if (line.startsWith("atom")) {
        readAtom(true);
        atom.tensors = new Tensor[2];
      } else if (line.startsWith("symmetry")) {
        readSymmetryNew();
      } else if (line.startsWith("ms")) {
        readTensorNew(0);
      } else if (line.startsWith("efg")) {
        readTensorNew(1);
      } else if (line.startsWith("<magres_old>")) {
        continuing = false;
      }
      return true;
    }
    if (line.contains("Coordinates")) {
      readAtom(false);
    } else if (line.contains("J-coupling Total")
        || line.contains("TOTAL tensor")) {
      readTensorOld();
    }
    return true;
  }

  // 0 ms H                  1          1.9115355485265077E+01         -6.8441521786256319E+00          1.9869475943756368E-01         -7.4231606832789883E+00          3.5078237789073569E+01          1.6453141184608533E+00         -8.4492087560280138E-01          1.4000600350356041E+00          1.7999188282948701E+01
  // 1 efg H                  1         -9.7305664267778647E-02         -1.3880930041098827E-01          8.3161631703720738E-03         -1.3880930041098827E-01          2.5187188360357782E-01         -4.4856574290225361E-02          8.3161631703720738E-03         -4.4856574290225361E-02         -1.5456621933580317E-01

  private void readTensorNew(int iType) throws Exception {
    float[] data = new float[9];
    String[] tokens = getTokens();
    String atomName = (tokens[1] + tokens[2]);
    fillFloatArray(line.substring(30), 0, data);
    float f = (iType == 0 ? 0.01f : 1f);
    //    if (isJ) {
    //      discardLinesUntilContains("Isotropic");
    //      float iso = parseFloatStr(getTokens()[3]);
    //      if (Math.abs(iso) > maxIso)
    //        return;
    //      f = 0.04f;
    //    }
    double[][] a = new double[3][3];
    for (int i = 0, pt = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
        a[i][j] = data[pt++];
    atom = atomSetCollection.getAtoms()[atomSetCollection.getAtomIndexFromName(atomName)];
    atom.tensors[iType] = Eigen.getEllipsoidDD(a);
    atom.tensors[iType].setTypeFactor(f);
    if (tensorTypes.indexOf("" + iType) < 0) {
      tensorTypes += "" + iType;
      appendLoadNote("Ellipsoids set " + (iType + 1) + ": "
          + (iType == 0 ? "Magnetic Shielding" : "Electric Field Gradient"));
    }
  }

  //  symmetry x,y,z
  //  symmetry x+1/2,-y+1/2,-z
  //  symmetry -x,y+1/2,-z+1/2
  //  symmetry -x+1/2,-y,z+1/2

  private void readSymmetryNew() {
    setSymmetryOperator(getTokens()[1]);
  }

  private void setUnitsNew() {
    String[] tokens = getTokens();
    mapUnits.put(tokens[1], tokens[2]);
  }

  private void readCellParams() throws Exception {
    String[] tokens = getTokens();
    cellParams = new float[9];
    for (int i = 0; i < 9; i++)
      cellParams[i] = parseFloatStr(tokens[i + 1]) * ANGSTROMS_PER_BOHR;
    addPrimitiveLatticeVector(0, cellParams, 0);
    addPrimitiveLatticeVector(1, cellParams, 3);
    addPrimitiveLatticeVector(2, cellParams, 6);
    setSpaceGroupName("P1");
  }

  /*
    C    1 Coordinates      2.054    0.000    0.000   A
   */

  private Atom atom;

  private void readAtom(boolean isNew) {
    float f = ((isNew ? mapUnits.get("atom").startsWith("A") : line.trim()
        .endsWith("A")) ? 1 : ANGSTROMS_PER_BOHR);
    int pt = (isNew ? 2 : 0);
    String[] tokens = getTokens();
    atom = new Atom();
    atom.elementSymbol = tokens[pt];
    atom.atomName = tokens[pt++] + tokens[pt++];
    atomSetCollection.addAtomWithMappedName(atom);
    if (!isNew)
      pt++;
    float x = parseFloatStr(tokens[pt++]) * f;
    float y = parseFloatStr(tokens[pt++]) * f;
    float z = parseFloatStr(tokens[pt++]) * f;
    atom.set(x, y, z);
    setAtomCoord(atom);
  }

  /*
         J-coupling Total

  W    1 Eigenvalue  sigma_xx -412163.5628
  W    1 Eigenvector sigma_xx       0.1467     -0.9892      0.0000
  W    1 Eigenvalue  sigma_yy -412163.6752
  W    1 Eigenvector sigma_yy       0.9892      0.1467      0.0000
  W    1 Eigenvalue  sigma_zz -432981.4974
  W    1 Eigenvector sigma_zz       0.0000      0.0000      1.0000
  
  TOTAL tensor

              -0.0216     -0.1561     -0.0137
              -0.1561     -0.1236     -0.0359
              -0.0137     -0.0359      0.1452

   */
  private void readTensorOld() throws Exception {
    line = line.trim();
    if (tensorTypes.indexOf(line) < 0) {
      tensorTypes += line;
      appendLoadNote("Ellipsoids: " + line);
    }
    atomSetCollection.setAtomSetName(line);
    boolean isJ = (line.indexOf("J-") >= 0);
    float[] data = new float[9];
    readLine();
    String s = TextFormat.simpleReplace(readLine() + readLine() + readLine(),
        "-", " -");
    fillFloatArray(s, 0, data);
    float f = 3;
    if (isJ) {
      discardLinesUntilContains("Isotropic");
      float iso = parseFloatStr(getTokens()[3]);
      if (Math.abs(iso) > maxIso)
        return;
      f = 0.04f;
    }
    double[][] a = new double[3][3];
    for (int i = 0, pt = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
        a[i][j] = data[pt++];
    atom.setEllipsoid(Eigen.getEllipsoidDD(a));
    atom.tensors[0].setTypeFactor(f);
  }
}
