/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.viewer.datamodel;
import org.openscience.jmol.viewer.datamodel.Frame;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.*;

import javax.vecmath.Point3f;
import java.util.Hashtable;
import java.util.Vector;

final public class PdbFile {
  Frame frame;

  private int modelCount = 0;
  private PdbModel[] pdbmodels = new PdbModel[1];
  private int[] modelNumbers = new int[1];

  private int structureCount = 0;
  private Structure[] structures = new Structure[10];

  public PdbFile(Frame frame) {
    this.frame = frame;
  }

  public void defineStructure(String structureType, char chainID,
                              int startSequenceNumber, char startInsertionCode,
                              int endSequenceNumber, char endInsertionCode) {
    /*
    System.out.println("PdbFile.defineStructure(" + structureType + "," +
                       chainID + "," +
                       startSequenceNumber + "," + startInsertionCode + "," +
                       endSequenceNumber + "," + endInsertionCode + ")" );
    */
    if (structureCount == structures.length)
      structures =
        (Structure[])Util.setLength(structures, structureCount + 10);
    structures[structureCount++] =
      new Structure(structureType, chainID,
                    Group.getSeqcode(startSequenceNumber,
                                        startInsertionCode),
                    Group.getSeqcode(endSequenceNumber,
                                        endInsertionCode));
  }

  public void freeze() {
    for (int i = modelCount; --i >= 0; )
      pdbmodels[i].freeze();
    propogateSecondaryStructure();
  }


  PdbModel getOrAllocateModel(int modelNumber) {
    for (int i = modelCount; --i >= 0; )
      if (modelNumbers[i] == modelNumber)
        return pdbmodels[i];
    if (modelCount == pdbmodels.length) {
      pdbmodels = (PdbModel[])Util.doubleLength(pdbmodels);
      modelNumbers = Util.doubleLength(modelNumbers);
    }
    modelNumbers[modelCount] = modelNumber;
    return pdbmodels[modelCount++] = new PdbModel(this, modelNumber);
  }

  private void propogateSecondaryStructure() {

    for (int i = structureCount; --i >= 0; ) {
      Structure structure = structures[i];
      for (int j = modelCount; --j >= 0; )
        pdbmodels[j].addSecondaryStructure(structure.type,
                                           structure.chainID,
                                           structure.startSeqcode,
                                           structure.endSeqcode);
    }
  }

  int modelNumberCurrent = Integer.MIN_VALUE;
  char chainIDCurrent;
  int sequenceNumberCurrent;
  char insertionCodeCurrent;
  Group groupCurrent;

  void setCurrentResidue(int modelNumber, char chainID,
                         int sequenceNumber, char insertionCode,
                         String group3) {
    modelNumberCurrent = modelNumber;
    chainIDCurrent = chainID;
    sequenceNumberCurrent = sequenceNumber;
    insertionCodeCurrent = insertionCode;
    PdbModel model = getOrAllocateModel(modelNumber);
    Chain chain = model.getOrAllocateChain(chainID);
    groupCurrent = chain.allocateGroup(sequenceNumber, insertionCode, group3);
  }

  /*
  public Group assignGroup(Atom atom, String pdbRecord) {
    char chainID = pdbRecord.charAt(21);
    int seqcode = Integer.MIN_VALUE;
    try {
      int sequenceNum = Integer.parseInt(pdbRecord.substring(22, 26).trim());
      char insertionCode = pdbRecord.charAt(26);
      seqcode = Group.getSeqcode(sequenceNum, insertionCode);
    } catch (NumberFormatException e) {
      System.out.println("bad residue number in: " + pdbRecord);
    }
    int modelNumber = atom.getModelNumber();
    if (modelNumber != modelNumberCurrent ||
        chainID != chainIDCurrent ||
        seqcode != seqcodeCurrent)
      setCurrentResidue(modelNumber, chainID,
                        seqcode, pdbRecord.substring(17, 20).trim());
    groupCurrent.assignAtom(atom, pdbRecord);
    return groupCurrent;
  }
  */

  public Group registerAtom(Atom atom, int modelNumber, char chainID,
                               int sequenceNumber, char insertionCode,
                               String group3) {
    /*
    System.out.println("PdbFile.registerAtom(...," + modelNumber + "," +
                       chainID + "," + sequenceNumber + "," + insertionCode +
                       "," + group3 + ")");
    */
                       
    if (sequenceNumber != sequenceNumberCurrent ||
        insertionCode != insertionCodeCurrent ||
        chainID != chainIDCurrent ||
        modelNumber != modelNumberCurrent)
      setCurrentResidue(modelNumber, chainID,
                        sequenceNumber, insertionCode, group3);
    groupCurrent.registerAtom(atom);
    return groupCurrent;
  }
  
  public int getModelCount() {
    return modelCount;
  }

  public PdbModel getModel(int i) {
    return pdbmodels[i];
  }

  /*
    temporary hack for backward compatibility with drawing code
  */

  public Group[] getMainchain(int i) {
    return pdbmodels[0].getMainchain(i);
  }

  public Chain getChain(int i) {
    return pdbmodels[0].getChain(i);
  }

  public int getChainCount() {
    int chainCount = 0;
    for (int i = modelCount; --i >= 0; )
      chainCount += pdbmodels[i].getChainCount();
    return chainCount;
  }

  public int getGroupCount() {
    int groupCount = 0;
    for (int i = modelCount; --i >= 0; )
      groupCount += pdbmodels[i].getGroupCount();
    return groupCount;
  }

  public void calcHydrogenBonds() {
    for (int i = modelCount; --i >= 0; )
      pdbmodels[i].calcHydrogenBonds();
  }

  class Structure {
    String typeName;
    byte type;
    char chainID;
    int startSeqcode;
    int endSeqcode;
    
    Structure(String typeName, char chainID,
              int startSeqcode, int endSeqcode) {
      this.typeName = typeName;
      this.chainID = chainID;
      this.startSeqcode = startSeqcode;
      this.endSeqcode = endSeqcode;
      if ("helix".equals(typeName))
        type = JmolConstants.SECONDARY_STRUCTURE_HELIX;
      else if ("sheet".equals(typeName))
        type = JmolConstants.SECONDARY_STRUCTURE_SHEET;
      else if ("turn".equals(typeName))
        type = JmolConstants.SECONDARY_STRUCTURE_TURN;
      else
        type = JmolConstants.SECONDARY_STRUCTURE_NONE;
    }
  }
}
