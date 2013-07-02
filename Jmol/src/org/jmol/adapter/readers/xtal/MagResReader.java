package org.jmol.adapter.readers.xtal;

/**
 * MagRes reader for magnetic resonance files produced by CASTEP
 * 
 * @author Bob Hanson hansonr@stolaf.edu, Simone Sturniolo  6/27/2013
 * 
 */

import java.util.Hashtable;
import java.util.Map;

import jspecview.util.Logger;

import org.jmol.util.JmolList;
import org.jmol.util.SB;
import org.jmol.util.Tensor;
import org.jmol.util.TextFormat;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;

public class MagResReader extends AtomSetCollectionReader {

  private final static int BLOCK_NEW = -2;
  private final static int BLOCK_NONE = -1;
  private final static int BLOCK_CALC = 0;
  private final static int BLOCK_ATOMS = 1;
  private final static int BLOCK_MAGRES = 2;

  private int currentBlock = BLOCK_NONE;

  private float[] cellParams;
  private String tensorTypes = "";
  //private static float maxIso = 10000; // the old code was checking for this.
  
  private Map<String, String> magresUnits = new Hashtable<String, String>();
  private JmolList<Tensor> interactionTensors = new JmolList<Tensor>();
  private SB header = new SB();
  
  /**
   * not sure how to work with symmetry here...
   * 
   */
  @Override
  protected void initializeReader() {
    setFractionalCoordinates(false);
    doApplySymmetry = false;
    ignoreFileSpaceGroupName = true;
  }

  /**
   * Set final auxiliary info and symmetry, including 
   * "fileHeader", "magresUnits", and "interactionTensors";
   * note that print getProperty("auxiliaryInfo.models[1].magresUnits") 
   * should return a catalog of tensor types.
   * 
   */
  @Override
  protected void finalizeReader() throws Exception {
    doApplySymmetry = true;
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("fileHeader",
        header.toString());
    finalizeReaderASCR();
    if (interactionTensors.size() > 0)
      atomSetCollection.setAtomSetAuxiliaryInfo("interactionTensors", interactionTensors);
  }

  /**
   * Valid blocks include [calculation] [atoms] [magres];
   * all magres entries must be prefaced with a corresponding unit;
   * Use of &lt; &gt; instead of [ ] is allowed.
   * 
   * @return true to read another line (some readers return false because they have over-read a line)
   */
  @Override
  protected boolean checkLine() throws Exception {
    if (!trimLine())
      return true;
    switch (checkBlock()) {
    case BLOCK_CALC:
      header.append(line).append("\n");
      appendLoadNote(line);
      break;
    case BLOCK_ATOMS:
      if (cellParams == null && line.startsWith("lattice"))
        return readCellParams();
      if (line.startsWith("symmetry"))
        return readSymmetry();
      if (line.startsWith("units"))
        return setUnits();
      if (line.startsWith("atom"))
        return readAtom();
      break;
    case BLOCK_MAGRES:
      if (line.startsWith("units"))
        return setUnits();
      return readTensor();
    }
    return true;
  }

  /**
   * All characters after hash ignored; lines are trimmed.
   * 
   * @return true if line has content
   *
   */
  private boolean trimLine() {
    int pt = line.indexOf("#");
    if (pt >= 0)
      line = line.substring(0, pt);
    line = line.trim();
    return (line.length() > 0);
  }

  /**
   * looking for tags here.
   * 
   * @return currentBlock if not a tag or BLOCK_NEW otherwise
   */
  private int checkBlock() {
    // please don't use RegEx here, because of issues with JavaScript version
    if (!(line.startsWith("<") && line.endsWith(">"))
       && !(line.startsWith("[") && line.endsWith("]")))      
      return currentBlock;
    line = TextFormat.simpleReplace(line, "<", "[");
    line = TextFormat.simpleReplace(line, ">", "]");
    switch (("..............." +
    		     "[calculation].." +
    		     "[/calculation]." +
    		     "[atoms]........" +
    		     "[/atoms]......." +
    		     "[magres]......." +
    		     "[/magres]......").indexOf(line + ".") / 15) {
    case 0:
      Logger.info("block indicator ignored: " + line);
      break;
    case 1:
      if (currentBlock == BLOCK_NONE)
        currentBlock = BLOCK_CALC;
      break;
    case 2:
      if (currentBlock == BLOCK_CALC)
        currentBlock = BLOCK_NONE;
      break;
    case 3:
      if (currentBlock == BLOCK_NONE) {
        currentBlock = BLOCK_ATOMS;
        atomSetCollection.newAtomSet();
        magresUnits = new Hashtable<String, String>();
      }
      break;
    case 4:
      if (currentBlock == BLOCK_ATOMS)
        currentBlock = BLOCK_NONE;
      break;
    case 5:
      if (currentBlock == BLOCK_NONE) {
        currentBlock = BLOCK_MAGRES;
        magresUnits = new Hashtable<String, String>();
        atomSetCollection.setAtomSetAuxiliaryInfo("magresUnits",
            magresUnits);
      }
      break;
    case 6:
      if (currentBlock == BLOCK_MAGRES)
        currentBlock = BLOCK_NONE;
      break;
    }
   return BLOCK_NEW;    
  }

  /**
   * catalog units
   * 
   * @return true
   */
  private boolean setUnits() {
    String[] tokens = getTokens();
    magresUnits.put(tokens[1], tokens[2]);
    return true;
  }

  //  symmetry x,y,z
  //  symmetry x+1/2,-y+1/2,-z
  //  symmetry -x,y+1/2,-z+1/2
  //  symmetry -x+1/2,-y,z+1/2

  /**
   * not doing anything with this -- P1 assumed
   * @return true
   */
  private boolean readSymmetry() {
    // not used
    setSymmetryOperator(getTokens()[1]);
    return true;
  }

  //  lattice    6.0000000000000009E+00   0.0000000000000000E+00   0.0000000000000000E+00   0.0000000000000000E+00   6.0000000000000009E+00   0.0000000000000000E+00   0.0000000000000000E+00   0.0000000000000000E+00   6.0000000000000009E+00

  /**
   * 
   * @return true;
   * @throws Exception
   */
  private boolean readCellParams() throws Exception {
    String[] tokens = getTokens();   
    cellParams = new float[9];
    for (int i = 0; i < 9; i++)
      cellParams[i] = parseFloatStr(tokens[i + 1]);
    addPrimitiveLatticeVector(0, cellParams, 0);
    addPrimitiveLatticeVector(1, cellParams, 3);
    addPrimitiveLatticeVector(2, cellParams, 6);
    setSpaceGroupName("P1");
    return true;
  }

  /*
    C    1 Coordinates      2.054    0.000    0.000   A
   */

  /**
   * 
   * Allowing for BOHR units here; probably unnecessary.
   * 
   * @return true
   */
  private boolean readAtom() {
    String units = magresUnits.get("atom");
    if (units == null)
      return true;
    float f = (units.startsWith("A") ? 1 : ANGSTROMS_PER_BOHR);
    String[] tokens = getTokens();
    Atom atom = new Atom();
    int pt = 1;
    atom.elementSymbol = tokens[pt++];
    atom.atomName = getAtomName(tokens[pt++], tokens[pt++]);
    atomSetCollection.addAtomWithMappedName(atom);
    float x = parseFloatStr(tokens[pt++]) * f;
    float y = parseFloatStr(tokens[pt++]) * f;
    float z = parseFloatStr(tokens[pt++]) * f;
    atom.set(x, y, z);
    setAtomCoord(atom);
    return true;
  }

  /**
   * combine name and index
   * @param name
   * @param index
   * @return name_index
   */
  private static String getAtomName(String name, String index) {
    return name + "_" + index;
  }

  // ms H                  1          1.9115355485265077E+01         -6.8441521786256319E+00          1.9869475943756368E-01         -7.4231606832789883E+00          3.5078237789073569E+01          1.6453141184608533E+00         -8.4492087560280138E-01          1.4000600350356041E+00          1.7999188282948701E+01
  // efg H                  1         -9.7305664267778647E-02         -1.3880930041098827E-01          8.3161631703720738E-03         -1.3880930041098827E-01          2.5187188360357782E-01         -4.4856574290225361E-02          8.3161631703720738E-03         -4.4856574290225361E-02         -1.5456621933580317E-01
  // isc_fc C                   2 H                   3         -1.0414024145274923E+00          5.9457737246691622E-02          1.3323917584132525E-01          5.9457737246692129E-02         -8.0480723469752380E-01          5.4194562595693906E-02          1.3323917584132525E-01          5.4194562595693989E-02         -8.1674287041188620E-01

  /**
   * Read a tensor line. Note that a corresponding unit line must have appeared first.
   * @return true;
   * @throws Exception 
   */
  private boolean readTensor() throws Exception {
    String[] tokens = getTokens();
    String id = tokens[0];
    String units = magresUnits.get(id);
    if (units == null)
      return true;
    String atomName1 = getAtomName(tokens[1], tokens[2]);
    int pt = 3;
    String atomName2 = (id.startsWith("isc") ? getAtomName(tokens[pt++], tokens[pt++]) : null);
    // TODO: maxIso for isc?
    double[][] a = new double[3][3];
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
        a[i][j] = Double.valueOf(tokens[pt++]).doubleValue();
    int index1 = atomSetCollection.getAtomIndexFromName(atomName1);
    int index2;
    Tensor t = Tensor.getTensorFromAsymmetricTensor(a, id);
    if (atomName2 == null) {
      index2 = -1;
      atomSetCollection.getAtoms()[index1].addTensor(t, null);
    } else {
      index2 = atomSetCollection.getAtomIndexFromName(atomName2);
      interactionTensors.addLast(t);
    }
    t.setAtomIndexes(index1, index2);
    String key = ";" + id +";";
    if (tensorTypes.indexOf(key) < 0) {
      tensorTypes += key;
      appendLoadNote("Ellipsoids set \"" + id + "\": "
          + (id.startsWith("ms") ? "Magnetic Shielding" : 
            id.startsWith("efg") ? "Electric Field Gradient" : id.startsWith("isc") ? "J-Coupling" : "?"));
    }
    return true;
  }
}
