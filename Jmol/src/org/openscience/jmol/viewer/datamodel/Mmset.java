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

// Mmset == Molecular Model set

final public class Mmset {
  Frame frame;

  private int modelCount = 0;
  private Model[] models = new Model[1];
  private int[] modelIDs = new int[1];

  private int structureCount = 0;
  private Structure[] structures = new Structure[10];

  public Mmset(Frame frame) {
    this.frame = frame;
  }

  public void defineStructure(String structureType, char chainID,
                              int startSequenceNumber, char startInsertionCode,
                              int endSequenceNumber, char endInsertionCode) {
    /*
    System.out.println("Mmset.defineStructure(" + structureType + "," +
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
    //    System.out.println("Mmset.freeze() modelCount=" + modelCount);
    for (int i = modelCount; --i >= 0; ) {
      //      System.out.println(" model " + i);
      models[i].freeze();
    }
    propogateSecondaryStructure();
  }


  Model getOrAllocateModel(int modelID) {
    for (int i = modelCount; --i >= 0; )
      if (modelIDs[i] == modelID)
        return models[i];
    if (modelCount == models.length) {
      models = (Model[])Util.doubleLength(models);
      modelIDs = Util.doubleLength(modelIDs);
    }
    modelIDs[modelCount] = modelID;
    Model model = new Model(this, modelCount, modelID);
    models[modelCount++] = model;
    return model;
  }

  int getModelIndex(int modelID) {
    int i;
    for (i = modelCount; --i >= 0 && modelIDs[i] != modelID; )
      ;
    return i;
  }

  private void propogateSecondaryStructure() {

    for (int i = structureCount; --i >= 0; ) {
      Structure structure = structures[i];
      for (int j = modelCount; --j >= 0; )
        models[j].addSecondaryStructure(structure.type,
                                        structure.chainID,
                                        structure.startSeqcode,
                                        structure.endSeqcode);
    }
  }

  public int getModelCount() {
    return modelCount;
  }

  public Model[] getModels() {
    return models;
  }

  /*
    temporary hack for backward compatibility with drawing code
  */

  /*
  public Group[] getMainchain(int i) {
    return models[0].getMainchain(i);
  }
  */

  /*
  public Chain getChain(int i) {
    return models[0].getChain(i);
  }
  */

  public int getChainCount() {
    int chainCount = 0;
    for (int i = modelCount; --i >= 0; )
      chainCount += models[i].getChainCount();
    return chainCount;
  }

  public int getGroupCount() {
    int groupCount = 0;
    for (int i = modelCount; --i >= 0; )
      groupCount += models[i].getGroupCount();
    return groupCount;
  }

  public void calcHydrogenBonds() {
    for (int i = modelCount; --i >= 0; )
      models[i].calcHydrogenBonds();
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
        type = JmolConstants.PROTEIN_STRUCTURE_HELIX;
      else if ("sheet".equals(typeName))
        type = JmolConstants.PROTEIN_STRUCTURE_SHEET;
      else if ("turn".equals(typeName))
        type = JmolConstants.PROTEIN_STRUCTURE_TURN;
      else
        type = JmolConstants.PROTEIN_STRUCTURE_NONE;
    }
  }
}
