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

package org.jmol.adapter.smarter;

import org.jmol.api.ModelAdapter;

import java.io.BufferedReader;

public class SmarterModelAdapter extends ModelAdapter {

  public SmarterModelAdapter(Logger logger) {
    super("SmarterModelAdapter", logger);
  }

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

  public Object openBufferedReader(String name,
                                   BufferedReader bufferedReader) {
    try {
      Object modelOrErrorMessage =
        ModelResolver.resolveModel(name, bufferedReader, logger);
      if (modelOrErrorMessage instanceof String)
        return modelOrErrorMessage;
      if (modelOrErrorMessage instanceof Model) {
        Model model = (Model)modelOrErrorMessage;
        if (model.errorMessage != null)
          return model.errorMessage;
        return model;
      }
      return "unknown reader error";
    } catch (Exception e) {
      e.printStackTrace();
      return "" + e;
    }
  }

  public String getFileTypeName(Object clientFile) {
    return ((Model)clientFile).modelTypeName;
  }

  public String getModelSetName(Object clientFile) {
    return ((Model)clientFile).modelName;
  }

  public String getModelFileHeader(Object clientFile) {
    return ((Model)clientFile).fileHeader;
  }

  /****************************************************************
   * The frame related methods
   ****************************************************************/

  public int getAtomCount(Object clientFile) {
    return ((Model)clientFile).atomCount;
  }

  public boolean coordinatesAreFractional(Object clientFile) {
    return ((Model)clientFile).coordinatesAreFractional;
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

  public ModelAdapter.AtomIterator
    getAtomIterator(Object clientFile) {
    return new AtomIterator((Model)clientFile);
  }

  public ModelAdapter.BondIterator
    getBondIterator(Object clientFile) {
    return new BondIterator((Model)clientFile);
  }

  public ModelAdapter.StructureIterator
    getStructureIterator(Object clientFile) {
    Model model = (Model)clientFile;
    return model.structureCount == 0 ? null : new StructureIterator(model);
  }

  /****************************************************************
   * the frame iterators
   ****************************************************************/
  class AtomIterator extends ModelAdapter.AtomIterator {
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
    public String getElementSymbol() {
      if (atom.elementSymbol != null)
        return atom.elementSymbol;
      return atom.getElementSymbol();
    }
    public int getElementNumber() { return atom.elementNumber; }
    public String getAtomName() { return atom.atomName; }
    public int getFormalCharge() { return atom.formalCharge; }
    public float getPartialCharge() { return atom.partialCharge; }
    public float getX() { return atom.x; }
    public float getY() { return atom.y; }
    public float getZ() { return atom.z; }
    public float getVectorX() { return atom.vectorX; }
    public float getVectorY() { return atom.vectorY; }
    public float getVectorZ() { return atom.vectorZ; }
    public float getBfactor() { return atom.bfactor; }
    public int getOccupancy() { return atom.occupancy; }
    public boolean getIsHetero() { return atom.isHetero; }
    public int getAtomSerial() { return atom.atomSerial; }
    public char getChainID() { return atom.chainID; }
    public String getGroup3() { return atom.group3; }
    public int getSequenceNumber() { return atom.sequenceNumber; }
    public char getInsertionCode() { return atom.insertionCode; }
    public String getPdbAtomRecord() { return atom.pdbAtomRecord; }
  }

  class BondIterator extends ModelAdapter.BondIterator {
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

  public class StructureIterator extends ModelAdapter.StructureIterator {
    int structureCount;
    Structure[] structures;
    Structure structure;
    int istructure;
    
    StructureIterator(Model model) {
      structureCount = model.structureCount;
      structures = model.structures;
      istructure = 0;
    }

    public boolean hasNext() {
      if (istructure == structureCount)
        return false;
      structure = structures[istructure++];
      return true;
    }

    public String getStructureType() {
      return structure.structureType;
    }

    public char getChainID() {
      return structure.chainID;
    }
    
    public int getStartSequenceNumber() {
      return structure.startSequenceNumber;
    }
    
    public char getStartInsertionCode() {
      return structure.startInsertionCode;
    }
    
    public int getEndSequenceNumber() {
      return structure.endSequenceNumber;
    }
      
    public char getEndInsertionCode() {
      return structure.endInsertionCode;
    }
  }
}
