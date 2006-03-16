/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import java.util.Hashtable;
import java.util.Properties;
import java.util.BitSet;
import java.util.Vector;

// Mmset == Molecular Model set

final class Mmset {
  Frame frame;

  Properties modelSetProperties;
  Hashtable modelSetAuxiliaryInfo;

  private int modelCount = 0;
  private String[] modelNames = new String[1];
  private int[] modelNumbers = new int[1];
  private Properties[] modelProperties = new Properties[1];
  private Hashtable[] modelAuxiliaryInfo = new Hashtable[1];
  private Model[] models = new Model[1];

  private int structureCount = 0;
  private Structure[] structures = new Structure[10];

  Mmset(Frame frame) {
    this.frame = frame;
  }
  
  void defineStructure(String structureType,
                       char startChainID,
                       int startSequenceNumber, char startInsertionCode,
                       char endChainID,
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
      new Structure(structureType,
                    startChainID, Group.getSeqcode(startSequenceNumber,
                                                   startInsertionCode),
                    endChainID, Group.getSeqcode(endSequenceNumber,
                                                 endInsertionCode));
  }

  void calculateStructures() {
    //    System.out.println("Mmset.calculateStructures()");
    for (int i = modelCount; --i >= 0; )
      models[i].calculateStructures();
  }

  void freeze() {
    //    System.out.println("Mmset.freeze() modelCount=" + modelCount);
    for (int i = modelCount; --i >= 0; ) {
      //      System.out.println(" model " + i);
      models[i].freeze();
    }
    propogateSecondaryStructure();

  }

  void setModelSetProperties(Properties modelSetProperties) {
    this.modelSetProperties = modelSetProperties;
  }

  void setModelSetAuxiliaryInfo(Hashtable modelSetAuxiliaryInfo) {
    this.modelSetAuxiliaryInfo = modelSetAuxiliaryInfo;
  }

  Properties getModelSetProperties() {
    return modelSetProperties;
  }

  Hashtable getModelSetAuxiliaryInfo() {
    return modelSetAuxiliaryInfo;
  }

  String getModelSetProperty(String propertyName) {
    return (modelSetProperties == null
            ? null : modelSetProperties.getProperty(propertyName));
  }

  Object getModelSetAuxiliaryInfo(String keyName) {
    return (modelSetAuxiliaryInfo == null
            ? null : modelSetAuxiliaryInfo.get(keyName));
  }

  void setModelCount(int modelCount) {
    //    System.out.println("setModelCount(" + modelCount + ")");
    if (this.modelCount != 0)
      throw new NullPointerException();
    this.modelCount = modelCount;
    models = (Model[])Util.setLength(models, modelCount);
    modelNames = Util.setLength(modelNames, modelCount);
    modelNumbers = Util.setLength(modelNumbers, modelCount);
    modelProperties = (Properties[])Util.setLength(modelProperties,
        modelCount);
    modelAuxiliaryInfo = (Hashtable[])Util.setLength(modelAuxiliaryInfo,
        modelCount);
  }

  String getModelName(int modelIndex) {
    return modelNames[modelIndex];
  }

  int getModelNumber(int modelIndex) {
    return modelNumbers[modelIndex];
  }

  Properties getModelProperties(int modelIndex) {
    return modelProperties[modelIndex];
  }

  String getModelProperty(int modelIndex, String property) {
    Properties props = modelProperties[modelIndex];
    return props == null ? null : props.getProperty(property);
  }

  Hashtable getModelAuxiliaryInfo(int modelIndex) {
    return modelAuxiliaryInfo[modelIndex];
  }

  Object getModelAuxiliaryInfo(int modelIndex, String key) {
    Hashtable info = modelAuxiliaryInfo[modelIndex];
    return info == null ? null : info.get(key);
  }

  Model getModel(int modelIndex) {
    return models[modelIndex];
  }

  int getModelNumberIndex(int modelNumber) {
    int i;
    for (i = modelCount; --i >= 0 && modelNumbers[i] != modelNumber; )
      {}
    return i;
  }


  void setModelNameNumberProperties(int modelIndex, String modelName,
                                    int modelNumber,
                                    Properties modelProperties, 
                                    Hashtable modelAuxiliaryInfo) {
    modelNames[modelIndex] = modelName;
    modelNumbers[modelIndex] = modelNumber;
    this.modelProperties[modelIndex] = modelProperties;
    this.modelAuxiliaryInfo[modelIndex] = modelAuxiliaryInfo;
    models[modelIndex] = new Model(this, modelIndex, modelName);
  }

  private void propogateSecondaryStructure() {

    for (int i = structureCount; --i >= 0; ) {
      Structure structure = structures[i];
      for (int j = modelCount; --j >= 0; )
        models[j].addSecondaryStructure(structure.type,
                                        structure.startChainID, structure.startSeqcode,
                                        structure.endChainID, structure.endSeqcode);
    }
  }
  
  int getModelCount() {
    return modelCount;
  }

  Model[] getModels() {
    return models;
  }

  int getChainCount() {
    int chainCount = 0;
    for (int i = modelCount; --i >= 0; )
      chainCount += models[i].getChainCount();
    return chainCount;
  }

  int getPolymerCount() {
    int polymerCount = 0;
    for (int i = modelCount; --i >= 0; )
      polymerCount += models[i].getPolymerCount();
    return polymerCount;
  }

  int getPolymerCountInModel(int modelIndex) {
    return models[modelIndex].getPolymerCount();
  }
  
  int getChainCountInModel(int modelIndex) {
    return models[modelIndex].getChainCount();
  }
  
  Polymer getPolymerAt(int modelIndex, int polymerIndex) {
    return models[modelIndex].getPolymer(polymerIndex);
  }
  
  int getGroupCount() {
    int groupCount = 0;
    for (int i = modelCount; --i >= 0; )
      groupCount += models[i].getGroupCount();
    return groupCount;
  }

  int getGroupCountInModel(int modelIndex) {
    return models[modelIndex].getGroupCount();
  }

  void calcSelectedGroupsCount(BitSet bsSelected) {
    for (int i = modelCount; --i >= 0; )
      models[i].calcSelectedGroupsCount(bsSelected);
  }

  void calcSelectedMonomersCount(BitSet bsSelected) {
    for (int i = modelCount; --i >= 0; )
      models[i].calcSelectedMonomersCount(bsSelected);
  }

  void calcHydrogenBonds() {
    for (int i = modelCount; --i >= 0; )
      models[i].calcHydrogenBonds();
  }

  void selectSeqcodeRange(int seqcodeA, int seqcodeB, BitSet bs) {
    for (int i = modelCount; --i >= 0; )
      models[i].selectSeqcodeRange(seqcodeA, seqcodeB, bs);
  }

  static class Structure {
    String typeName;
    byte type;
    char startChainID;
    int startSeqcode;
    char endChainID;
    int endSeqcode;
    
    Structure(String typeName, char startChainID, int startSeqcode,
              char endChainID, int endSeqcode) {
      this.typeName = typeName;
      this.startChainID = startChainID;
      this.startSeqcode = startSeqcode;
      this.endChainID = endChainID;
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

    Hashtable toHashtable() {
      Hashtable info = new Hashtable();
      info.put("type",typeName);
      info.put("startChainID",startChainID+"");
      info.put("startSeqcode",new Integer(startSeqcode));
      info.put("endChainID",endChainID+"");
      info.put("endSeqcode",new Integer(endSeqcode));
      return info;
    }

  }
  
  Vector getStructureInfo() {
    Vector info = new Vector();
    for(int i = 0; i < structureCount; i++) 
      info.add(structures[i].toHashtable());
    return info;
  }
  
}
