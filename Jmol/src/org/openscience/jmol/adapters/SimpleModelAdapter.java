/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.openscience.jmol.adapters;

// these are standard and should be needed by all adapters
import org.openscience.jmol.viewer.*;

import java.awt.Color;
import java.io.BufferedReader;
import java.util.StringTokenizer;

// client-specific imports

public class SimpleModelAdapter extends JmolModelAdapter {

  /****************************************************************
   * the file related methods
   ****************************************************************/

  final static int UNKNOWN = -1;
  final static int XYZ = 0;
  final static int MOL = 1;
  final static int JME = 2;
  final static int PDB = 3;

  public void finish(Object clientFile) {
    ((Model)clientFile).finish();
  }

  public Object openBufferedReader(JmolViewer viewer,
                                   String name, BufferedReader bufferedReader) {
    System.out.println("SimpleModelAdapter reading:" + name);
    try {
      Model model;
      switch (determineModel(bufferedReader)) {
      case XYZ:
        model = new XyzModel(bufferedReader);
        break;
      case MOL:
        model = new MolModel(bufferedReader);
        break;
      case JME:
        model = new JmeModel(bufferedReader);
        break;
      case PDB:
        model = new PdbModel(bufferedReader);
        break;
      default:
        return "unrecognized file format";
      }
      if (model.errorMessage != null)
        return model.errorMessage;
      return model;
    } catch (Exception e) {
      e.printStackTrace();
      return "" + e;
    }
  }

  private int determineModel(BufferedReader bufferedReader)
    throws Exception {
    bufferedReader.mark(512);
    String line1 = bufferedReader.readLine();
    String line2 = bufferedReader.readLine();
    String line3 = bufferedReader.readLine();
    String line4 = bufferedReader.readLine();
    bufferedReader.reset();
    try {
      int atomCount = Integer.parseInt(line1.trim());
      return XYZ;
    } catch (NumberFormatException e) {
    }
    if (line4 != null && line4.length() >= 6) {
      String line4trimmed = line4.trim();
      if (line4trimmed.endsWith("V2000") ||
          line4trimmed.endsWith("v2000") ||
          line4trimmed.endsWith("V3000"))
        return MOL;
      try {
        Integer.parseInt(line4.substring(0, 3).trim());
        Integer.parseInt(line4.substring(3, 6).trim());
        return MOL;
      } catch (NumberFormatException nfe) {
      }
    }
    for (int i = pdbRecords.length; --i >= 0; )
      if (line1.startsWith(pdbRecords[i]))
        return PDB;
    if (line2 == null || line2.trim().length() == 0)
      return JME;
    return UNKNOWN;
  }

  private final static String[] pdbRecords = {
    "HEADER", "OBSLTE", "TITLE ", "CAVEAT", "COMPND", "SOURCE", "KEYWDS",
    "EXPDTA", "AUTHOR", "REVDAT", "SPRSDE", "JRNL  ", "REMARK",

    "DBREF ", "SEQADV", "SEQRES", "MODRES", 

    "HELIX ", "SHEET ", "TURN  ",

    "CRYST1", "ORIGX1", "ORIGX2", "ORIGX3", "SCALE1", "SCALE2", "SCALE3",

    "ATOM  ", "HETATM", "MODEL ",
  };

  public int getModelType(Object clientFile) {
    if (clientFile instanceof PdbModel)
      return JmolConstants.MODEL_TYPE_PDB;
    if (clientFile instanceof XyzModel)
      return JmolConstants.MODEL_TYPE_XYZ;
    return JmolConstants.MODEL_TYPE_OTHER;
  }

  public String getModelName(Object clientFile) {
    return ((Model)clientFile).modelName;
  }

  public String getModelHeader(Object clientFile) {
    return ((Model)clientFile).fileHeader;
  }

  /****************************************************************
   * The frame related methods
   ****************************************************************/

  public int getAtomCount(Object clientFile) {
    return ((Model)clientFile).atomCount;
  }

  public boolean hasPdbRecords(Object clientFile) {
    return clientFile instanceof PdbModel;
  }

  public String[] getPdbStructureRecords(Object clientFile) {
    Model model = (Model)clientFile;
    if (model.pdbStructureRecordCount == 0)
      return null;
    String[] t = new String[model.pdbStructureRecordCount];
    System.arraycopy(model.pdbStructureRecords, 0, t, 0,
                     model.pdbStructureRecordCount);
    return t;
  }

  public float[] getNotionalUnitcell(Object clientFile) {
    return ((Model)clientFile).notionalUnitcell;
  }

  public float[] getPdbScaleMatrix(Object clientFile) {
    return ((Model)clientFile).pdbScaleMatrix;
  }

  public float[] getPdbScaleTranslate(Object clientFile) {
    return ((Model)clientFile).pdbScaleTranslate;
  }

  public JmolModelAdapter.AtomIterator
    getAtomIterator(Object clientFile) {
    return new AtomIterator((Model)clientFile);
  }

  public JmolModelAdapter.BondIterator
    getBondIterator(Object clientFile) {
    return new BondIterator((Model)clientFile);
  }

  /****************************************************************
   * the frame iterators
   ****************************************************************/
  class AtomIterator extends JmolModelAdapter.AtomIterator {
    Model model;
    int iatom;
    Atom atom;

    AtomIterator(Model model) {
      this.model = model;
      iatom = 0;
    }
    public boolean hasNext() {
      if (iatom == model.atomCount)
        return false;
      atom = model.atoms[iatom++];
      return true;
    }
    public int getModelNumber() { return atom.modelNumber; }
    public Object getUniqueID() { return atom; }
    public String getAtomicSymbol() { return atom.elementSymbol; }
    public int getAtomicCharge() { return atom.atomicCharge; }
    public float getX() { return atom.x; }
    public float getY() { return atom.y; }
    public float getZ() { return atom.z; }
    public boolean hasVector() { return atom.vectorX != Float.NaN; };
    public float getVectorX() { return atom.vectorX; }
    public float getVectorY() { return atom.vectorY; }
    public float getVectorZ() { return atom.vectorZ; }
    public String getPdbAtomRecord() { return atom.pdbAtomRecord; }
  }

  class BondIterator extends JmolModelAdapter.BondIterator {
    Model model;
    Atom[] atoms;
    Bond[] bonds;
    int ibond;
    Bond bond;

    BondIterator(Model model) {
      this.model = model;
      atoms = model.atoms;
      bonds = model.bonds;
      ibond = 0;
    }
    public boolean hasNext() {
      if (ibond == model.bondCount)
        return false;
      bond = bonds[ibond++];
      return true;
    }
    public Object getAtomUid1() {
      return atoms[bond.atomIndex1];
    }
    public Object getAtomUid2() {
      return atoms[bond.atomIndex2];
    }
    public int getOrder() {
      return bond.order;
    }
  }
}

class Atom {
  int modelNumber;
  String elementSymbol;
  int atomicCharge;
  float x, y, z;
  float vectorX = Float.NaN, vectorY = Float.NaN, vectorZ = Float.NaN;
  String pdbAtomRecord;

  Atom(int modelNumber, String symbol, int charge,
       float x, float y, float z,
       float vectorX, float vectorY, float vectorZ) {
    this.modelNumber = modelNumber;
    this.elementSymbol = symbol;
    this.atomicCharge = charge;
    this.x = x;
    this.y = y;
    this.z = z;
    this.vectorX = vectorX;
    this.vectorY = vectorY;
    this.vectorZ = vectorZ;
  }

  Atom(String symbol, int charge, float x, float y, float z) {
    this.elementSymbol = symbol;
    this.atomicCharge = charge;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  Atom(int modelNumber, String symbol, int charge,
       float x, float y, float z, String pdb) {
    this.elementSymbol = symbol;
    this.atomicCharge = charge;
    this.x = x;
    this.y = y;
    this.z = z;
    this.modelNumber = modelNumber;
    this.pdbAtomRecord = pdb;
  }
}

class Bond {
  int atomIndex1;
  int atomIndex2;
  int order;

  Bond(int atomIndex1, int atomIndex2, int order) {
    this.atomIndex1 = atomIndex1;
    this.atomIndex2 = atomIndex2;
    this.order = order;
  }
}

abstract class Model {
  int atomCount;
  int bondCount;
  String modelName;
  Atom[] atoms = new Atom[512];
  Bond[] bonds = new Bond[1024];
  String errorMessage;
  String fileHeader;
  float[] notionalUnitcell;
  float[] pdbScaleMatrix;
  float[] pdbScaleTranslate;

  int pdbStructureRecordCount;
  String[] pdbStructureRecords;

  protected void finalize() {
    System.out.println("SimpleModelAdapter.Model.finalize() called");
  }

  void finish() {
    atoms = null;
    bonds = null;
    notionalUnitcell = pdbScaleMatrix = pdbScaleTranslate = null;
    pdbStructureRecords = null;
  }

  void addAtom(Atom atom) {
    if (atomCount == atoms.length) {
      Atom[] t = new Atom[atomCount + 512];
      System.arraycopy(atoms, 0, t, 0, atomCount);
      atoms = t;
    }
    atoms[atomCount++] = atom;
  }

  void addBond(Bond bond) {
    if (bondCount == bonds.length) {
      Bond[] t = new Bond[bondCount + 1024];
      System.arraycopy(bonds, 0, t, 0, bondCount);
      bonds = t;
    }
    bonds[bondCount++] = bond;
  }

  void setModelName(String modelName) {
    if (modelName != null) {
      modelName.trim();
      if (modelName.length() > 0)
        this.modelName = modelName;
    }
  }
}

class XyzModel extends Model {
    
  XyzModel(BufferedReader reader) {
    try {
      int modelNumber = 1;
      int modelCount;
      while ((modelCount = readAtomCount(reader)) > 0) {
        if (modelNumber == 1)
          setModelName(reader.readLine());
        else
          reader.readLine();
        readAtoms(reader, modelNumber, modelCount);
        ++modelNumber;
      }
    } catch (Exception ex) {
      errorMessage = "Could not read file:" + ex;
      System.out.println(errorMessage);
      ex.printStackTrace();
    }
    if (atomCount == 0) {
      errorMessage = "No atoms in file";
      System.out.println(errorMessage);
    }
  }
    
  int readAtomCount(BufferedReader reader) throws Exception {
    String line = reader.readLine();
    if (line == null)
      return 0;
    StringTokenizer tokenizer = new StringTokenizer(line, "\t ");
    if (! tokenizer.hasMoreTokens())
      return 0;
    return Integer.parseInt(tokenizer.nextToken());
  }

  void readAtoms(BufferedReader reader, int modelNumber, int modelCount)
    throws Exception {
    float[] chargeAndOrVector = new float[4];
    for (int i = 0; i < modelCount; ++i) {
      StringTokenizer tokenizer =
        new StringTokenizer(reader.readLine(), "\t ");
      String elementSymbol = tokenizer.nextToken();
      float x = Float.valueOf(tokenizer.nextToken()).floatValue();
      float y = Float.valueOf(tokenizer.nextToken()).floatValue();
      float z = Float.valueOf(tokenizer.nextToken()).floatValue();
      int j;
      for (j = 0; j < 4 && tokenizer.hasMoreTokens(); ++j)
        chargeAndOrVector[j] =
          Float.valueOf(tokenizer.nextToken()).floatValue();
      int charge = (j == 1 || j == 4) ? (int)chargeAndOrVector[0] : 0;
      float vectorX, vectorY, vectorZ;
      vectorX = vectorY = vectorZ = Float.NaN;
      if (j >= 3) {
        vectorX = chargeAndOrVector[j - 3];
        vectorY = chargeAndOrVector[j - 2];
        vectorZ = chargeAndOrVector[j - 1];
      }
      addAtom(new Atom(modelNumber, elementSymbol, charge,
                       x, y, z, vectorX, vectorY, vectorZ));
    }
  }
}

class JmeModel extends Model {
  String line;
  StringTokenizer tokenizer;

  JmeModel(BufferedReader reader) {
    try {
      line = reader.readLine();
      tokenizer = new StringTokenizer(line, "\t ");
      int atomCount = Integer.parseInt(tokenizer.nextToken());
      System.out.println("atomCount=" + atomCount);
      int bondCount = Integer.parseInt(tokenizer.nextToken());
      setModelName("JME");
      readAtoms(atomCount);
      readBonds(bondCount);
    } catch (Exception ex) {
      errorMessage = "Could not read file:" + ex;
      System.out.println(errorMessage);
    }
  }
    
  void readAtoms(int atomCount) throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      String atom = tokenizer.nextToken();
      //      System.out.println("atom=" + atom);
      int indexColon = atom.indexOf(':');
      String elementSymbol = (indexColon > 0
                             ? atom.substring(0, indexColon)
                             : atom);
      float x = Float.valueOf(tokenizer.nextToken()).floatValue();
      float y = Float.valueOf(tokenizer.nextToken()).floatValue();
      float z = 0;
      addAtom(new Atom(elementSymbol, 0, x, y, z));
    }
  }

  void readBonds(int bondCount) throws Exception {
    for (int i = 0; i < bondCount; ++i) {
      int atomIndex1 = Integer.parseInt(tokenizer.nextToken());
      int atomIndex2 = Integer.parseInt(tokenizer.nextToken());
      int order = Integer.parseInt(tokenizer.nextToken());
      //      System.out.println("bond "+atomIndex1+"->"+atomIndex2+" "+order);
      if (order < 1) {
        //        System.out.println("Stereo found:" + order);
        order = ((order == -1)
                 ? JmolConstants.BOND_STEREO_NEAR
                 : JmolConstants.BOND_STEREO_FAR);
      }
      addBond(new Bond(atomIndex1-1, atomIndex2-1, order));
    }
  }
}

class MolModel extends Model {
    
  MolModel(BufferedReader reader) throws Exception {
    setModelName(reader.readLine());
    reader.readLine();
    reader.readLine();
    String countLine = reader.readLine();
    int atomCount = Integer.parseInt(countLine.substring(0, 3).trim());
    int bondCount = Integer.parseInt(countLine.substring(3, 6).trim());
    readAtoms(reader, atomCount);
    readBonds(reader, bondCount);
  }
  
  // www.mdli.com/downloads/public/ctfile/ctfile.jsp
  void readAtoms(BufferedReader reader, int atomCount) throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      String line = reader.readLine();
      String elementSymbol = line.substring(31,34).trim();
      float x = Float.valueOf(line.substring( 0,10).trim()).floatValue();
      float y = Float.valueOf(line.substring(10,20).trim()).floatValue();
      float z = Float.valueOf(line.substring(20,30).trim()).floatValue();
      int charge = 0;
      if (line.length() >= 39) {
        int chargeCode = Integer.parseInt(line.substring(36, 39).trim());
        if (chargeCode != 0)
          charge = 4 - chargeCode;
      }

      addAtom(new Atom(elementSymbol, charge, x, y, z));
    }
  }

  void readBonds(BufferedReader reader, int bondCount) throws Exception {
    for (int i = 0; i < bondCount; ++i) {
      String line = reader.readLine();
      int atomIndex1 = Integer.parseInt(line.substring(0, 3).trim());
      int atomIndex2 = Integer.parseInt(line.substring(3, 6).trim());
      int order = Integer.parseInt(line.substring(6, 9).trim());
      if (order == 4)
        order = JmolConstants.BOND_AROMATIC;
      addBond(new Bond(atomIndex1-1, atomIndex2-1, order));
    }
  }
}

class PdbModel extends Model {
  String line;
  // index into atoms array + 1
  // so that 0 can be used for the null value
  int currentModelNumber;
  int[] serialMap = new int[512];

  PdbModel(BufferedReader reader) throws Exception {
    atoms = new Atom[512];
    bonds = new Bond[32];
    pdbStructureRecords = new String[32];
    fileHeader = "";
    boolean accumulatingHeader = true;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith("ATOM  ") ||
          line.startsWith("HETATM")) {
        atom();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("CONECT")) {
        conect();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("HELIX ") ||
          line.startsWith("SHEET ") ||
          line.startsWith("TURN  ")) {
        structure();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("MODEL ")) {
        model();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("CRYST1")) {
        cryst1();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("SCALE1")) {
        scale1();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("SCALE2")) {
        scale2();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("SCALE3")) {
        scale3();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("HEADER") && line.length() >= 66) {
        setModelName(line.substring(62, 66));
        continue;
      }
      if (accumulatingHeader) {
        fileHeader += line + '\n';
      }
    }
    serialMap = null;
  }

  boolean isValidAtomicSymbolChar(char ch) {
    return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
  }

  void atom() {
    try {
      // for now, we are only taking alternate location 'A'
      char charAlternateLocation = line.charAt(16);
      if (charAlternateLocation != ' ' && charAlternateLocation != 'A')
        return;
      int len = line.length();
      String elementSymbol = (len >= 78 ? line.substring(76, 78).trim() : "");
      int cchAtomicSymbol = elementSymbol.length();
      if (cchAtomicSymbol == 0 ||
          Character.isDigit(elementSymbol.charAt(0)) ||
          (cchAtomicSymbol == 2 && Character.isDigit(elementSymbol.charAt(1)))) {
        char ch13 = line.charAt(13);
        boolean isValid13 = isValidAtomicSymbolChar(ch13);
        char ch12 = line.charAt(12);
        if (isValidAtomicSymbolChar(ch12))
          elementSymbol = isValid13 ? "" + ch12 + ch13 : "" + ch12;
        else
          elementSymbol = isValid13 ? "" + ch13 : "Xx";
      }
      /****************************************************************
       * calculate the charge from cols 79 & 80 (1-based)
       * 2+, 3-, etc
       ****************************************************************/
      int charge = 0;
      if (len >= 80) {
        char chMag = line.charAt(78);
        char chSign = line.charAt(79);
        if (chMag >= '0' && chMag <= '7' &&
            (chSign == '+' || chSign == '-' || chSign == ' ')) {
          charge = chMag - '0';
          if (chSign == '-')
            charge = -charge;
        }
      }
      /****************************************************************/
      int serial = Integer.parseInt(line.substring(6, 11).trim());
      /****************************************************************
       * coordinates
       ****************************************************************/
      float x =
        Float.valueOf(line.substring(30, 38).trim()).floatValue();
      float y =
        Float.valueOf(line.substring(38, 46).trim()).floatValue();
      float z =
        Float.valueOf(line.substring(46, 54).trim()).floatValue();
      /****************************************************************/
      if (serial >= serialMap.length) {
        int[] t = new int[serial + 500];
        System.arraycopy(serialMap, 0, t, 0, serialMap.length);
        serialMap = t;
      }
      addAtom(new Atom(currentModelNumber, elementSymbol,
                       charge, x, y, z, line));
      // note that values are +1 in this serial map
      serialMap[serial] = atomCount;
    } catch (NumberFormatException e) {
      System.out.println("bad record:" + line);
    }
  }

  void conect() {
    int sourceSerial = -1;
    int sourceIndex = -1;
    try {
      sourceSerial = Integer.parseInt(line.substring(6, 11).trim());
      sourceIndex = serialMap[sourceSerial] - 1;
      if (sourceIndex < 0)
        return;
      // use this for HBONDS
      for (int i = 0; i < 9; i += (i == 5 ? 2 : 1)) {
      //      for (int i = 0; i < 4; i += (i == 5 ? 2 : 1)) {
        int targetSerial = getTargetSerial(i);
        if (targetSerial == -1)
          continue;
        int targetIndex = serialMap[targetSerial] - 1;
        if (targetIndex < 0)
          continue;
        if (bondCount > 0) {
          Bond bond = bonds[bondCount - 1];
          if (i < 4 &&
              bond.atomIndex1 == sourceIndex &&
              bond.atomIndex2 == targetIndex) {
            ++bond.order;
            continue;
          }
        }
        if (i >= 4)
          System.out.println("hbond:" + sourceIndex + "->" + targetIndex);
        addBond(new Bond(sourceIndex, targetIndex,
                         i < 4
                         ? 1 : JmolModelAdapter.ORDER_HBOND));
      }
    } catch (Exception e) {
    }
  }

  int getTargetSerial(int i) {
    int offset = i * 5 + 11;
    int offsetEnd = offset + 5;
    if (offsetEnd <= line.length()) {
      String str = line.substring(offset, offsetEnd).trim();
      if (str.length() > 0) {
        try {
          int target = Integer.parseInt(str);
          return target;
        } catch (NumberFormatException e) {
        }
      }
    }
    return -1;
  }

  void structure() {
    if (pdbStructureRecordCount == pdbStructureRecords.length) {
      String[] t = new String[2 * pdbStructureRecordCount];
      System.arraycopy(pdbStructureRecords, 0, t, 0, pdbStructureRecordCount);
      pdbStructureRecords = t;
    }
    pdbStructureRecords[pdbStructureRecordCount++] = line;
  }

  void model() {
    try {
      int modelNumber = Integer.parseInt(line.substring(10, 14).trim());
      if (modelNumber != currentModelNumber + 1)
        System.out.println("Model number sequence seems confused");
      currentModelNumber = modelNumber;
    } catch (NumberFormatException e) {
    }
  }

  void cryst1() {
    try {
      float a = getFloat( 6, 9);
      float b = getFloat(15, 9);
      float c = getFloat(24, 9);
      float alpha = getFloat(33, 7);
      float beta  = getFloat(40, 7);
      float gamma = getFloat(47, 7);
      notionalUnitcell = new float[6];
      notionalUnitcell[0] = a;
      notionalUnitcell[1] = b;
      notionalUnitcell[2] = c;
      notionalUnitcell[3] = alpha;
      notionalUnitcell[4] = beta;
      notionalUnitcell[5] = gamma;
    } catch (Exception e) {
    }
  }

  float getFloat(int ich, int cch) throws Exception {
    String strFloat = line.substring(ich, ich+cch).trim();
    float value = Float.valueOf(strFloat).floatValue();
    return value;
  }

  void scale(int n) throws Exception {
    pdbScaleMatrix[n*3 + 0] = getFloat(10, 10);
    pdbScaleMatrix[n*3 + 1] = getFloat(20, 10);
    pdbScaleMatrix[n*3 + 2] = getFloat(30, 10);
    float translation = getFloat(45, 10);
    if (translation != 0) {
      if (pdbScaleTranslate == null)
        pdbScaleTranslate = new float[3];
      pdbScaleTranslate[n] = translation;
    }
  }

  void scale1() {
    try {
      pdbScaleMatrix = new float[9];
      scale(0);
    } catch (Exception e) {
      pdbScaleMatrix = null;
      System.out.println("scale1 died:" + 3);
    }
  }

  void scale2() {
    try {
      scale(1);
    } catch (Exception e) {
      pdbScaleMatrix = null;
      System.out.println("scale2 died");
    }
  }

  void scale3() {
    try {
      scale(2);
    } catch (Exception e) {
      pdbScaleMatrix = null;
      System.out.println("scale3 died");
    }
  }
}
