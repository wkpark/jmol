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

public class PdbFile {
  public Frame frame;
  String[] structureRecords;

  int modelCount = 0;
  PdbModel[] models = new PdbModel[1];
  short[] modelIDs = new short[1];

  public PdbFile(Frame frame) {
    this.frame = frame;
  }

  public void setStructureRecords(String[] structureRecords) {
    this.structureRecords = structureRecords;
  }

  public void freeze() {
    for (int i = modelCount; --i >= 0; )
      models[i].freeze();
    propogateSecondaryStructure();
  }


  PdbModel getOrAllocateModel(short modelID) {
    for (int i = modelCount; --i >= 0; )
      if (modelIDs[i] == modelID)
        return models[i];
    if (modelCount == models.length) {
      short[] tNumbers = new short[modelCount * 2];
      System.arraycopy(modelIDs, 0, tNumbers, 0, modelCount);
      modelIDs = tNumbers;

      PdbModel[] t = new PdbModel[modelCount * 2];
      System.arraycopy(models, 0, t, 0, modelCount);
      models = t;
    }
    modelIDs[modelCount] = modelID;
    return models[modelCount++] = new PdbModel(this, modelID);
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

      short startSequence = 0;
      short endSequence = 0;
      try {
        int startSequenceNumber = 
          Integer.parseInt(structureRecord.substring(startIndex,
                                                     startIndex + 4).trim());
        char startInsertionCode = structureRecord.charAt(startIndex + 4);
        int endSequenceNumber =
          Integer.parseInt(structureRecord.substring(endIndex,
                                                     endIndex + 4).trim());
        char endInsertionCode = structureRecord.charAt(endIndex + 4);
        /*
        startSequence = PdbGroup.getSequence(startSequenceNumber,
                                             startInsertionCode);
        endSequence = PdbGroup.getSequence(endSequenceNumber,
                                           endInsertionCode);
        */
        startSequence = (short)startSequenceNumber;
        endSequence = (short)endSequenceNumber;
        
      } catch (NumberFormatException e) {
        System.out.println("secondary structure record error");
        continue;
      }

      char chainID = structureRecord.charAt(chainIDIndex);

      for (int j = modelCount; --j >= 0; )
        models[j].addSecondaryStructure(chainID, type,
                                        startSequence, endSequence);
    }
  }

  int modelIDCurrent = -1;
  char chainIDCurrent = '\uFFFF';
  short groupSequenceCurrent;
  PdbGroup groupCurrent;

  void setCurrentResidue(short modelID, char chainID,
                         short groupSequence, String group3) {
    modelIDCurrent = modelID;
    chainIDCurrent = chainID;
    groupSequenceCurrent = groupSequence;
    PdbModel model = getOrAllocateModel(modelID);
    PdbChain chain = model.getOrAllocateChain(chainID);
    groupCurrent = chain.allocateGroup(groupSequence, group3);
  }
  
  public PdbAtom allocatePdbAtom(int atomIndex, short modelID,
                                 String pdbRecord) {
    char chainID = pdbRecord.charAt(21);
    short groupSequence = Short.MIN_VALUE;
    try {
      groupSequence = Short.parseShort(pdbRecord.substring(22, 26).trim());
    } catch (NumberFormatException e) {
      System.out.println("bad residue number in: " + pdbRecord);
    }
    if (modelID != modelIDCurrent ||
        chainID != chainIDCurrent ||
        groupSequence != groupSequenceCurrent)
      setCurrentResidue(modelID, chainID,
                        groupSequence, pdbRecord.substring(17, 20));
    return groupCurrent.allocatePdbAtom(atomIndex, pdbRecord);
  }

  public int getModelCount() {
    return modelCount;
  }

  public PdbModel getModel(int i) {
    return models[i];
  }

  /*
    temporary hack for backward compatibility with drawing code
  */

  public PdbGroup[] getMainchain(int i) {
    return models[0].getMainchain(i);
  }

  public PdbChain getChain(int i) {
    return models[0].getChain(i);
  }

  public int getChainCount() {
    return models[0].getChainCount();
  }
}
