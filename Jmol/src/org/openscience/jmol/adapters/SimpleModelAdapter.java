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

public class SimpleModelAdapter implements JmolModelAdapter {

  /****************************************************************
   * the capabilities
   ****************************************************************/
  public boolean suppliesAtomicNumber() { return false; }
  public boolean suppliesAtomicSymbol() { return true; }
  public boolean suppliesAtomTypeName() { return false; }
  public boolean suppliesVanderwaalsRadius() { return false; }
  public boolean suppliesCovalentRadius() { return false; }
  public boolean suppliesAtomArgb() { return false; }
  
  /****************************************************************
   * the file related methods
   ****************************************************************/

  final static int UNKNOWN = -1;
  final static int XYZ = 0;
  final static int MOL = 1;
  final static int JME = 2;
  final static int PDB = 3;

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
      return JmolModelAdapter.MODEL_TYPE_PDB;
    if (clientFile instanceof XyzModel)
      return JmolModelAdapter.MODEL_TYPE_XYZ;
    return JmolModelAdapter.MODEL_TYPE_OTHER;
  }

  public String getModelName(Object clientFile) {
    return ((Model)clientFile).modelName;
  }

  public int getFrameCount(Object clientFile) {
    return 1;
  }


  /****************************************************************
   * The frame related methods
   ****************************************************************/

  public int getAtomCount(Object clientFile, int frameNumber) {
    return ((Model)clientFile).atomCount;
  }

  public boolean hasPdbRecords(Object clientFile, int frameNumber) {
    return clientFile instanceof PdbModel;
  }

  public JmolModelAdapter.AtomIterator
    getAtomIterator(Object clientFile, int frameNumber) {
    return new AtomIterator((Model)clientFile);
  }

  public JmolModelAdapter.BondIterator
    getCovalentBondIterator(Object clientFile, int frameNumber) {
    return new BondIterator((Model)clientFile);
  }

  public JmolModelAdapter.BondIterator
    getAssociationBondIterator(Object clientFile, int frameNumber) {
    return null;
  }

  public JmolModelAdapter.LineIterator
    getVectorIterator(Object clientFile, int frameNumber) {
    return null;
  }

  public JmolModelAdapter.LineIterator
    getCrystalCellIterator(Object clientFile, int frameNumber) {
    return null;
  }

  /****************************************************************
   * the frame iterators
   ****************************************************************/
  class AtomIterator extends JmolModelAdapter.AtomIterator {
    Model model;
    int iatom;

    AtomIterator(Model model) {
      this.model = model;
      iatom = 0;
    }
    public boolean hasNext() {
      return iatom < model.atomCount;
    }
    public Object next() {
      return model.atoms[iatom++];
    }
  }

  class BondIterator extends JmolModelAdapter.BondIterator {
    Model model;
    Atom[] atoms;
    Bond[] bonds;
    int ibond;

    BondIterator(Model model) {
      this.model = model;
      atoms = model.atoms;
      bonds = model.bonds;
      ibond = -1;
    }
    public boolean hasNext() {
      return ibond + 1 < model.bondCount;
    }
    public void moveNext() {
      ++ibond;
    }
    public Object getAtom1() {
      return atoms[bonds[ibond].atomIndex1];
    }
    public Object getAtom2() {
      return atoms[bonds[ibond].atomIndex2];
    }
    public int getOrder() {
      return bonds[ibond].order;
    }
  }

  /****************************************************************
   * The atom related methods
   ****************************************************************/

  public int getAtomicNumber(Object clientAtom) {
    return -1;
  }
  
  public String getAtomicSymbol(Object clientAtom) {
    return ((Atom)clientAtom).atomicSymbol;
  }

  public String getAtomTypeName(Object clientAtom) {
    return null;
  }

  public float getVanderwaalsRadius(Object clientAtom) {
    return 0;
  }

  public float getCovalentRadius(Object clientAtom) {
    return 0;
  }

  public float getAtomX(Object clientAtom) {
    return ((Atom)clientAtom).x;
  }

  public float getAtomY(Object clientAtom) {
    return ((Atom)clientAtom).y;
  }

  public float getAtomZ(Object clientAtom) {
    return ((Atom)clientAtom).z;
  }


  public String getPdbAtomRecord(Object clientAtom){
    return ((Atom)clientAtom).pdbAtomRecord;
  }

  public String[] getPdbStructureRecords(Object clientFile, int frameNumber) {
    Model model = (Model)clientFile;
    if (model.pdbStructureRecordCount == 0)
      return null;
    String[] t = new String[model.pdbStructureRecordCount];
    System.arraycopy(model.pdbStructureRecords, 0, t, 0, model.pdbStructureRecordCount);
    return t;
  }

  public int getAtomArgb(Object clientAtom, int colorScheme) {
    return 0;
  }

  ////////////////////////////////////////////////////////////////
  // notifications
  ////////////////////////////////////////////////////////////////
  public void notifyAtomDeleted(Object clientAtom) {
    // insert CDK code here
  }
}

class Atom {
  String atomicSymbol;
  float x, y, z;
  String pdbAtomRecord;
  Atom(String atomicSymbol, float x, float y, float z) {
    this.atomicSymbol = atomicSymbol;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  Atom(String atomicSymbol, float x, float y, float z, String pdb) {
    this.atomicSymbol = atomicSymbol;
    this.x = x;
    this.y = y;
    this.z = z;
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
  Atom[] atoms;
  Bond[] bonds;
  String errorMessage;

  int pdbStructureRecordCount;
  String[] pdbStructureRecords;

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
      readAtomCount(reader);
      setModelName(reader.readLine());
      readAtoms(reader);
    } catch (Exception ex) {
      errorMessage = "Could not read file:" + ex;
      System.out.println(errorMessage);
    }
  }
    
  void readAtomCount(BufferedReader reader) throws Exception {
    String line = reader.readLine();
    StringTokenizer tokenizer = new StringTokenizer(line, "\t ");
    atomCount = Integer.parseInt(tokenizer.nextToken());
    System.out.println("atomCount=" + atomCount);
  }

  void readAtoms(BufferedReader reader) throws Exception {
    atoms = new Atom[atomCount];
    for (int i = 0; i < atomCount; ++i) {
      StringTokenizer tokenizer = new StringTokenizer(reader.readLine(), "\t ");
      String atomicSymbol = tokenizer.nextToken();
      float x = Float.valueOf(tokenizer.nextToken()).floatValue();
      float y = Float.valueOf(tokenizer.nextToken()).floatValue();
      float z = Float.valueOf(tokenizer.nextToken()).floatValue();
      atoms[i] = new Atom(atomicSymbol, x, y, z);
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
      atomCount = Integer.parseInt(tokenizer.nextToken());
      System.out.println("atomCount=" + atomCount);
      bondCount = Integer.parseInt(tokenizer.nextToken());
      setModelName("JME");
      readAtoms();
      readBonds();
    } catch (Exception ex) {
      errorMessage = "Could not read file:" + ex;
      System.out.println(errorMessage);
    }
  }
    
  void readAtoms() throws Exception {
    atoms = new Atom[atomCount];
    for (int i = 0; i < atomCount; ++i) {
      String atom = tokenizer.nextToken();
      System.out.println("atom=" + atom);
      int indexColon = atom.indexOf(':');
      String atomicSymbol = (indexColon > 0
                             ? atom.substring(0, indexColon)
                             : atom);
      float x = Float.valueOf(tokenizer.nextToken()).floatValue();
      float y = Float.valueOf(tokenizer.nextToken()).floatValue();
      float z = 0;
      atoms[i] = new Atom(atomicSymbol, x, y, z);
    }
  }

  void readBonds() throws Exception {
    bonds = new Bond[bondCount];
    for (int i = 0; i < bondCount; ++i) {
      int atomIndex1 = Integer.parseInt(tokenizer.nextToken());
      int atomIndex2 = Integer.parseInt(tokenizer.nextToken());
      int order = Integer.parseInt(tokenizer.nextToken());
      System.out.println("bond "+atomIndex1+"->"+atomIndex2+" "+order);
      if (order < 1) {
        System.out.println("Stereo found:" + order);
        order = ((order == -1)
                 ? JmolModelAdapter.STEREO_NEAR : JmolModelAdapter.STEREO_FAR);
      }
      bonds[i] = new Bond(atomIndex1-1, atomIndex2-1, order);
    }
  }
}

class MolModel extends Model {
    
  MolModel(BufferedReader reader) throws Exception {
    setModelName(reader.readLine());
    reader.readLine();
    reader.readLine();
    String countLine = reader.readLine();
    atomCount = Integer.parseInt(countLine.substring(0, 3).trim());
    bondCount = Integer.parseInt(countLine.substring(3, 6).trim());
    readAtoms(reader);
    readBonds(reader);
  }

  void readAtoms(BufferedReader reader) throws Exception {
    atoms = new Atom[atomCount];
    for (int i = 0; i < atomCount; ++i) {
      String line = reader.readLine();
      String atomicSymbol = line.substring(31,34).trim();
      float x = Float.valueOf(line.substring( 0,10).trim()).floatValue();
      float y = Float.valueOf(line.substring(10,20).trim()).floatValue();
      float z = Float.valueOf(line.substring(20,30).trim()).floatValue();
      atoms[i] = new Atom(atomicSymbol, x, y, z);
    }
  }

  void readBonds(BufferedReader reader) throws Exception {
    bonds = new Bond[bondCount];
    for (int i = 0; i < bondCount; ++i) {
      String line = reader.readLine();
      int atomIndex1 = Integer.parseInt(line.substring(0, 3).trim());
      int atomIndex2 = Integer.parseInt(line.substring(3, 6).trim());
      int order = Integer.parseInt(line.substring(6, 9).trim());
      bonds[i] = new Bond(atomIndex1-1, atomIndex2-1, order);
    }
  }
}

class PdbModel extends Model {
    
  PdbModel(BufferedReader reader) throws Exception {
    String line;
    atoms = new Atom[512];
    pdbStructureRecords = new String[32];
    while ((line = reader.readLine()) != null) {
      if (line.startsWith("ATOM  ") ||
          line.startsWith("HETATM")) {
        int len = line.length();
        String atomicSymbol = (len >= 78 ? line.substring(76, 78).trim() : "");
        if (atomicSymbol.length() == 0 ||
            Character.isDigit(atomicSymbol.charAt(0))) {
          atomicSymbol = line.substring(12, 14).trim();
        }
        float x =
          Float.valueOf(line.substring(30, 38).trim()).floatValue();
        float y =
          Float.valueOf(line.substring(38, 46).trim()).floatValue();
        float z =
          Float.valueOf(line.substring(46, 54).trim()).floatValue();
        if (atomCount == atoms.length) {
          Atom[] t = new Atom[atomCount + 512];
          System.arraycopy(atoms, 0, t, 0, atomCount);
          atoms = t;
        }
        atoms[atomCount++] = new Atom(atomicSymbol, x, y, z, line);
        continue;
      }
      if (line.startsWith("HELIX ") ||
          line.startsWith("SHEET ") ||
          line.startsWith("TURN  ")) {
        if (pdbStructureRecordCount == pdbStructureRecords.length) {
          String[] t = new String[2 * pdbStructureRecordCount];
          System.arraycopy(pdbStructureRecords, 0, t, 0, pdbStructureRecordCount);
          pdbStructureRecords = t;
        }
        pdbStructureRecords[pdbStructureRecordCount++] = line;
        continue;
      }
      if (line.startsWith("HEADER")) {
        setModelName(line.substring(62, 66));
        continue;
      }
    }
  }
}
