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
package org.openscience.jmol.viewer.pdb;
import org.openscience.jmol.viewer.datamodel.Frame;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.JmolConstants;

import javax.vecmath.Point3f;
import java.util.Hashtable;
import java.util.Vector;

final public class PdbFile {
  Frame frame;

  private String[] structureRecords;
  private int pdbmodelCount = 0;
  private PdbModel[] pdbmodels = new PdbModel[1];
  private short[] pdbmodelIDs = new short[1];

  public PdbFile(Frame frame) {
    this.frame = frame;
  }

  public void setStructureRecords(String[] structureRecords) {
    this.structureRecords = structureRecords;
  }

  public void freeze() {
    for (int i = pdbmodelCount; --i >= 0; )
      pdbmodels[i].freeze();
    propogateSecondaryStructure();
  }


  PdbModel getOrAllocateModel(short pdbmodelID) {
    for (int i = pdbmodelCount; --i >= 0; )
      if (pdbmodelIDs[i] == pdbmodelID)
        return pdbmodels[i];
    if (pdbmodelCount == pdbmodels.length) {
      short[] tNumbers = new short[pdbmodelCount * 2];
      System.arraycopy(pdbmodelIDs, 0, tNumbers, 0, pdbmodelCount);
      pdbmodelIDs = tNumbers;

      PdbModel[] t = new PdbModel[pdbmodelCount * 2];
      System.arraycopy(pdbmodels, 0, t, 0, pdbmodelCount);
      pdbmodels = t;
    }
    pdbmodelIDs[pdbmodelCount] = pdbmodelID;
    return pdbmodels[pdbmodelCount++] = new PdbModel(this, pdbmodelID);
  }

  private void propogateSecondaryStructure() {
    if (structureRecords == null)
      return;
    for (int i = structureRecords.length; --i >= 0; ) {
      String structureRecord = structureRecords[i];
      byte type = JmolConstants.SECONDARY_STRUCTURE_NONE;
      int chainIDIndex = 19;
      int startIndex = 0;
      int endIndex = 0;
      if (structureRecord.startsWith("HELIX ")) {
        type = JmolConstants.SECONDARY_STRUCTURE_HELIX;
        startIndex = 21;
        endIndex = 33;
      } else if (structureRecord.startsWith("SHEET ")) {
        type = JmolConstants.SECONDARY_STRUCTURE_SHEET;
        chainIDIndex = 21;
        startIndex = 22;
        endIndex = 33;
      } else if (structureRecord.startsWith("TURN  ")) {
        type = JmolConstants.SECONDARY_STRUCTURE_TURN;
        startIndex = 20;
        endIndex = 31;
      } else
        continue;

      int startSeqcode = 0;
      int endSeqcode = 0;
      try {
        int startSequenceNumber = 
          Integer.parseInt(structureRecord.substring(startIndex,
                                                     startIndex + 4).trim());
        char startInsertionCode = structureRecord.charAt(startIndex + 4);
        int endSequenceNumber =
          Integer.parseInt(structureRecord.substring(endIndex,
                                                     endIndex + 4).trim());
        char endInsertionCode = structureRecord.charAt(endIndex + 4);

        startSeqcode = PdbGroup.getSeqcode(startSequenceNumber,
                                             startInsertionCode);
        endSeqcode = PdbGroup.getSeqcode(endSequenceNumber,
                                           endInsertionCode);
      } catch (NumberFormatException e) {
        System.out.println("secondary structure record error");
        continue;
      }

      char chainID = structureRecord.charAt(chainIDIndex);

      for (int j = pdbmodelCount; --j >= 0; )
        pdbmodels[j].addSecondaryStructure(chainID, type,
                                        startSeqcode, endSeqcode);
    }
  }

  int pdbmodelIDCurrent = -1;
  char chainIDCurrent = '\uFFFF';
  int seqcodeCurrent;
  PdbGroup groupCurrent;

  void setCurrentResidue(short pdbmodelID, char chainID,
                         int seqcode, String group3) {
    pdbmodelIDCurrent = pdbmodelID;
    chainIDCurrent = chainID;
    seqcodeCurrent = seqcode;
    PdbModel model = getOrAllocateModel(pdbmodelID);
    PdbChain chain = model.getOrAllocateChain(chainID);
    groupCurrent = chain.allocateGroup(seqcode, group3);
  }
  
  public PdbAtom allocatePdbAtom(int atomIndex, short pdbmodelID,
                                 String pdbRecord) {
    char chainID = pdbRecord.charAt(21);
    int seqcode = Integer.MIN_VALUE;
    try {
      int sequenceNum = Integer.parseInt(pdbRecord.substring(22, 26).trim());
      char insertionCode = pdbRecord.charAt(26);
      seqcode = PdbGroup.getSeqcode(sequenceNum, insertionCode);
    } catch (NumberFormatException e) {
      System.out.println("bad residue number in: " + pdbRecord);
    }
    if (pdbmodelID != pdbmodelIDCurrent ||
        chainID != chainIDCurrent ||
        seqcode != seqcodeCurrent)
      setCurrentResidue(pdbmodelID, chainID,
                        seqcode, pdbRecord.substring(17, 20).trim());
    return groupCurrent.allocatePdbAtom(atomIndex, pdbRecord);
  }

  public int getModelCount() {

    return pdbmodelCount;
  }

  public PdbModel getModel(int i) {
    return pdbmodels[i];
  }

  /*
    temporary hack for backward compatibility with drawing code
  */

  public PdbGroup[] getMainchain(int i) {
    return pdbmodels[0].getMainchain(i);
  }

  public PdbChain getChain(int i) {
    return pdbmodels[0].getChain(i);
  }

  public int getChainCount() {
    return pdbmodels[0].getChainCount();
  }

  public void calcHydrogenBonds() {
    for (int i = pdbmodelCount; --i >= 0; )
      pdbmodels[i].calcHydrogenBonds();
  }
}
