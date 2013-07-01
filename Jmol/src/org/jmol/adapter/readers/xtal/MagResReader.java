package org.jmol.adapter.readers.xtal;

/**
 * MagRes reader for magnetic resonance files produced by CASTEP
 * 
 * @author Bob Hanson hansonr@stolaf.edu 6/27/2013
 * 
 */

import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.JmolList;
import org.jmol.util.Tensor;
import org.jmol.util.TextFormat;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;

public class MagResReader extends AtomSetCollectionReader {

  private float[] cellParams;
  private static float maxIso = 10000;
  private String tensorTypes = "";
  private boolean isNew;
  private Map<String, String> mapUnits = new Hashtable<String, String>();
  private JmolList<Tensor> interactionTensors = new JmolList<Tensor>();
  @Override
  protected void initializeReader() {
    setFractionalCoordinates(false);
    doApplySymmetry = false;
    atomSetCollection.newAtomSet();
    try {
      readLine();
      isNew = line.startsWith("#$magres");      
      if (isNew) {
        ignoreFileSpaceGroupName = true;
        //setSpaceGroupName("P1");
      }
    } catch (Exception e) {
    }
  }

  @Override
  protected void finalizeReader() throws Exception {
    doApplySymmetry = true;
    finalizeReaderASCR();
    if (interactionTensors.size() > 0)
      atomSetCollection.setAtomSetAuxiliaryInfo("interactionTensors", interactionTensors);
  }

  @Override
  protected boolean checkLine() throws Exception {
    line = line.trim();
    if (cellParams == null && line.startsWith("lattice")) {
      readCellParams();
      return true;
    }
    if (isNew) {
      if (line.startsWith("units")) {
        setUnitsNew();
      } else if (line.startsWith("atom")) {
        readAtom(true);
      } else if (line.startsWith("symmetry")) {
        readSymmetryNew();
      } else if (line.startsWith("ms") || line.startsWith("efg") || line.startsWith("isc")) {
        readTensorNew();
      } else if (line.startsWith("<magres_old>")) {
        continuing = false;
      }
      return true;
    }
    if (line.contains("Coordinates")) {
      readAtom(false);
    } else if (line.contains("J-coupling Total")
        || line.contains("TOTAL tensor")|| line.contains("TOTAL Shielding Tensor")) {
      readTensorOld();
    }
    return true;
  }

  // ms H                  1          1.9115355485265077E+01         -6.8441521786256319E+00          1.9869475943756368E-01         -7.4231606832789883E+00          3.5078237789073569E+01          1.6453141184608533E+00         -8.4492087560280138E-01          1.4000600350356041E+00          1.7999188282948701E+01
  // efg H                  1         -9.7305664267778647E-02         -1.3880930041098827E-01          8.3161631703720738E-03         -1.3880930041098827E-01          2.5187188360357782E-01         -4.4856574290225361E-02          8.3161631703720738E-03         -4.4856574290225361E-02         -1.5456621933580317E-01
  // isc_fc C                   2 H                   3         -1.0414024145274923E+00          5.9457737246691622E-02          1.3323917584132525E-01          5.9457737246692129E-02         -8.0480723469752380E-01          5.4194562595693906E-02          1.3323917584132525E-01          5.4194562595693989E-02         -8.1674287041188620E-01

  private void readTensorNew() throws Exception {
    String[] tokens = getTokens();
    String id = tokens[0];
    int pt = id.indexOf("_");
    String type = (pt < 0 ? id : id.substring(0, pt));
    String atomName1 = getAtomName(tokens[1], tokens[2]);
    pt = 3;
    String atomName2 = (type.equals("isc") ? getAtomName(tokens[pt++], tokens[pt++]) : null);
    // TODO: maxIso for isc?
    double[][] a = new double[3][3];
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
        a[i][j] = Double.valueOf(tokens[pt++]).doubleValue();
    int index1 = atomSetCollection.getAtomIndexFromName(atomName1);
    int index2;
    Tensor t = Tensor.getTensorFromAsymmetricTensor(a, type);
    if (atomName2 == null) {
      index2 = -1;
      atomSetCollection.getAtoms()[index1].addTensor(t, null);
      interactionTensors.addLast(t);
    } else {
      index2 = atomSetCollection.getAtomIndexFromName(atomName2);
    }
    t.setAtomIndexes(index1, index2);  
    if (tensorTypes.indexOf(type) < 0) {
      tensorTypes += type;
      appendLoadNote("Ellipsoids set \"" + type + "\": "
          + (type.equals("ms") ? "Magnetic Shielding" : 
            type.equals("efg") ? "Electric Field Gradient" : type.equals("isc") ? "Coupling" : "?"));
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
      cellParams[i] = parseFloatStr(tokens[i + 1]);
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
    String[] tokens = getTokens();
    atom = new Atom();
    int pt = (isNew ? 1 : 0);
    atom.elementSymbol = tokens[isNew ? pt++ : pt];
    atom.atomName = getAtomName(tokens[pt++], tokens[pt++]);
    atomSetCollection.addAtomWithMappedName(atom);
    if (!isNew)
      pt++;
    float x = parseFloatStr(tokens[pt++]) * f;
    float y = parseFloatStr(tokens[pt++]) * f;
    float z = parseFloatStr(tokens[pt++]) * f;
    atom.set(x, y, z);
    setAtomCoord(atom);
  }

  private String getAtomName(String name, String index) {
    return name + (name.indexOf("_") >= 0 ? "_" : "") + index;
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
    String type = (line.indexOf("J-") >= 0 ? "isc" : line.indexOf("Shielding") >= 0 ? "ms" : "efg");
    float[] data = new float[9];
    readLine();
    String s = TextFormat.simpleReplace(readLine() + readLine() + readLine(),
        "-", " -");
    fillFloatArray(s, 0, data);
    //float f = 3;
    if (type.equals("isc")) {
      discardLinesUntilContains("Isotropic");
      float iso = parseFloatStr(getTokens()[3]);
      if (Math.abs(iso) > maxIso)
        return;
      //f = 0.04f;
    }
    double[][] a = new double[3][3];
    for (int i = 0, pt = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
        a[i][j] = data[pt++];
    atom.addTensor(Tensor.getTensorFromAsymmetricTensor(a, type), null);
  }
}
