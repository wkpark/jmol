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

import org.jmol.api.JmolAdapter;

import java.io.BufferedReader;

public class SmarterJmolAdapter extends JmolAdapter {

  public SmarterJmolAdapter(Logger logger) {
    super("SmarterJmolAdapter", logger);
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
    ((AtomSetCollection)clientFile).finish();
  }

  public Object openBufferedReader(String name,
                                   BufferedReader bufferedReader) {
    try {
      Object atomSetCollectionOrErrorMessage =
        Resolver.resolve(name, bufferedReader, logger);
      if (atomSetCollectionOrErrorMessage instanceof String)
        return atomSetCollectionOrErrorMessage;
      if (atomSetCollectionOrErrorMessage instanceof AtomSetCollection) {
        AtomSetCollection atomSetCollection =
          (AtomSetCollection)atomSetCollectionOrErrorMessage;
        if (atomSetCollection.errorMessage != null)
          return atomSetCollection.errorMessage;
        return atomSetCollection;
      }
      return "unknown reader error";
    } catch (Exception e) {
      e.printStackTrace();
      return "" + e;
    }
  }

  public String getFileTypeName(Object clientFile) {
    return ((AtomSetCollection)clientFile).fileTypeName;
  }

  public String getAtomSetCollectionName(Object clientFile) {
    return ((AtomSetCollection)clientFile).collectionName;
  }

  public String getFileHeader(Object clientFile) {
    return ((AtomSetCollection)clientFile).fileHeader;
  }
  
  public int getAtomSetCount(Object clientFile) {
    // we need to implement this
    // the current AtomSetCollection does not have support for this
    // but now we need it
    return ((AtomSetCollection)clientFile).getAtomSetCount();
  }

  public int getAtomSetNumber(Object clientFile, int atomSetIndex) {
    return ((AtomSetCollection)clientFile).getAtomSetNumber(atomSetIndex);
  }

  public String getAtomSetName(Object clientFile, int atomSetIndex) {
    return ((AtomSetCollection)clientFile).getAtomSetName(atomSetIndex);
  }

  /****************************************************************
   * The frame related methods
   ****************************************************************/

  public int getEstimatedAtomCount(Object clientFile) {
    return ((AtomSetCollection)clientFile).atomCount;
  }

  public boolean coordinatesAreFractional(Object clientFile) {
    return ((AtomSetCollection)clientFile).coordinatesAreFractional;
  }

  public float[] getNotionalUnitcell(Object clientFile) {
    return ((AtomSetCollection)clientFile).notionalUnitcell;
  }

  public float[] getPdbScaleMatrix(Object clientFile) {
    return ((AtomSetCollection)clientFile).pdbScaleMatrix;
  }

  public float[] getPdbScaleTranslate(Object clientFile) {
    return ((AtomSetCollection)clientFile).pdbScaleTranslate;
  }

  public JmolAdapter.AtomIterator
    getAtomIterator(Object clientFile) {
    return new AtomIterator((AtomSetCollection)clientFile);
  }

  public JmolAdapter.BondIterator
    getBondIterator(Object clientFile) {
    return new BondIterator((AtomSetCollection)clientFile);
  }

  public JmolAdapter.StructureIterator
    getStructureIterator(Object clientFile) {
    AtomSetCollection atomSetCollection = (AtomSetCollection)clientFile;
    return atomSetCollection.structureCount == 0 ? null : new StructureIterator(atomSetCollection);
  }

  /****************************************************************
   * the frame iterators
   ****************************************************************/
  class AtomIterator extends JmolAdapter.AtomIterator {
    AtomSetCollection atomSetCollection;
    int iatom;
    Atom atom;

    AtomIterator(AtomSetCollection atomSetCollection) {
      this.atomSetCollection = atomSetCollection;
      iatom = 0;
    }
    public boolean hasNext() {
      if (iatom == atomSetCollection.atomCount)
        return false;
      atom = atomSetCollection.atoms[iatom++];
      return true;
    }
    public int getAtomSetNumber() { return atom.atomSetNumber; }
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
    public char getChainID() { return canonizeChainID(atom.chainID); }
    public String getGroup3() { return atom.group3; }
    public int getSequenceNumber() { return atom.sequenceNumber; }
    public char getInsertionCode()
    { return canonizeInsertionCode(atom.insertionCode); }
    public String getPdbAtomRecord() { return atom.pdbAtomRecord; }
  }

  class BondIterator extends JmolAdapter.BondIterator {
    AtomSetCollection atomSetCollection;
    Atom[] atoms;
    Bond[] bonds;
    int ibond;
    Bond bond;

    BondIterator(AtomSetCollection atomSetCollection) {
      this.atomSetCollection = atomSetCollection;
      atoms = atomSetCollection.atoms;
      bonds = atomSetCollection.bonds;
      ibond = 0;
    }
    public boolean hasNext() {
      if (ibond == atomSetCollection.bondCount)
        return false;
      bond = bonds[ibond++];
      return true;
    }
    public Object getAtomUniqueID1() {
      return atoms[bond.atomIndex1];
    }
    public Object getAtomUniqueID2() {
      return atoms[bond.atomIndex2];
    }
    public int getEncodedOrder() {
      return bond.order;
    }
  }

  public class StructureIterator extends JmolAdapter.StructureIterator {
    int structureCount;
    Structure[] structures;
    Structure structure;
    int istructure;
    
    StructureIterator(AtomSetCollection atomSetCollection) {
      structureCount = atomSetCollection.structureCount;
      structures = atomSetCollection.structures;
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

    public char getStartChainID() {
      return canonizeChainID(structure.startChainID);
    }
    
    public int getStartSequenceNumber() {
      return structure.startSequenceNumber;
    }
    
    public char getStartInsertionCode() {
      return canonizeInsertionCode(structure.startInsertionCode);
    }
    
    public char getEndChainID() {
      return canonizeChainID(structure.endChainID);
    }
    
    public int getEndSequenceNumber() {
      return structure.endSequenceNumber;
    }
      
    public char getEndInsertionCode() {
      return structure.endInsertionCode;
    }
  }
}
