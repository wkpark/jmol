/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-23 16:52:20 -0500 (Mon, 23 Apr 2007) $
 * $Revision: 7474 $
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
package org.jmol.modelframe;

import java.util.Hashtable;
import java.util.Properties;
import java.util.BitSet;
import java.util.Vector;
import java.util.Enumeration;

import org.jmol.util.ArrayUtil;
import org.jmol.viewer.JmolConstants;

// Mmset == Molecular Model set

public final class Mmset {
  Frame frame;

  Properties modelSetProperties;
  Hashtable modelSetAuxiliaryInfo;

  private int modelCount = 0;
  private Properties[] modelProperties = new Properties[1];
  private Hashtable[] modelAuxiliaryInfo = new Hashtable[1];
  private Model[] models = new Model[1];

  private int structureCount = 0;
  private Structure[] structures = new Structure[10];

  Mmset(Frame frame) {
    this.frame = frame;
  }

  void merge(Mmset mmset) {
    for (int i = 0; i < mmset.modelCount; i++) {
      models[i] = mmset.getModel(i);
      modelProperties[i] = mmset.getModelProperties(i);
      modelAuxiliaryInfo[i] = mmset.getModelAuxiliaryInfo(i);
    }    
  }
  
  void defineStructure(int modelIndex, String structureType, char startChainID,
                       int startSequenceNumber, char startInsertionCode,
                       char endChainID, int endSequenceNumber,
                       char endInsertionCode) {
    if (structureCount == structures.length)
      structures = (Structure[]) ArrayUtil
          .setLength(structures, structureCount + 10);
    structures[structureCount++] = new Structure(modelIndex, structureType, startChainID,
        Group.getSeqcode(startSequenceNumber, startInsertionCode), endChainID,
        Group.getSeqcode(endSequenceNumber, endInsertionCode));
  }

  void clearStructures() {
    for (int i = modelCount; --i >= 0;)
      models[i].clearStructures();
  }

  void calculateStructures() {
    for (int i = modelCount; --i >= 0;)
      models[i].calculateStructures();
  }

  void setConformation(int modelIndex, BitSet bsConformation) {
    for (int i = modelCount; --i >= 0;)
      if (i == modelIndex || modelIndex < 0)
      models[i].setConformation(bsConformation);
  }

  Hashtable getHeteroList(int modelIndex) {
    Hashtable htFull = new Hashtable();
    boolean ok = false;
    for (int i = modelCount; --i >= 0;)
      if (modelIndex < 0 || i == modelIndex) {
        Hashtable ht = (Hashtable) getModelAuxiliaryInfo(i, "hetNames");
        if (ht == null)
          continue;
        ok = true;
        Enumeration e = ht.keys();
        while (e.hasMoreElements()) {
          String key = (String) e.nextElement();
          htFull.put(key, ht.get(key));
        }
      }
    return (ok ? htFull : (Hashtable) getModelSetAuxiliaryInfo("hetNames"));
  }

  void freeze() {
    for (int i = modelCount; --i >= 0;) {
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
    return (modelSetProperties == null ? null : modelSetProperties
        .getProperty(propertyName));
  }

  Object getModelSetAuxiliaryInfo(String keyName) {
    return (modelSetAuxiliaryInfo == null ? null : modelSetAuxiliaryInfo
        .get(keyName));
  }

  boolean getModelSetAuxiliaryInfoBoolean(String keyName) {
    return (modelSetAuxiliaryInfo != null
        && modelSetAuxiliaryInfo.containsKey(keyName) && ((Boolean) modelSetAuxiliaryInfo
        .get(keyName)).booleanValue());
  }

  int getModelSetAuxiliaryInfoInt(String keyName) {
    if (modelSetAuxiliaryInfo != null
        && modelSetAuxiliaryInfo.containsKey(keyName)) {
      return ((Integer) modelSetAuxiliaryInfo.get(keyName)).intValue();
    }
    return Integer.MIN_VALUE;
  }

  void setModelCount(int modelCount) {
    if (this.modelCount != 0)
      throw new NullPointerException();
    this.modelCount = modelCount;
    models = (Model[]) ArrayUtil.setLength(models, modelCount);
    modelProperties = (Properties[]) ArrayUtil
        .setLength(modelProperties, modelCount);
    modelAuxiliaryInfo = (Hashtable[]) ArrayUtil.setLength(modelAuxiliaryInfo,
        modelCount);
  }

  String getModelTitle(int modelIndex) {
    return models[modelIndex].modelTitle;
  }
  
  String getModelFile(int modelIndex) {
    return models[modelIndex].modelFile;
  }
  
  int getFirstAtomIndex(int modelIndex) {
    return models[modelIndex].firstAtomIndex;  
  }
  
  void setFirstAtomIndex(int modelIndex, int atomIndex) {
    models[modelIndex].firstAtomIndex = atomIndex;  
  }
  
  int setSymmetryAtomInfo(int modelIndex, int atomIndex, int atomCount) {
    models[modelIndex].preSymmetryAtomIndex = atomIndex;
    return models[modelIndex].preSymmetryAtomCount = atomCount;
  }

  int getPreSymmetryAtomIndex(int modelIndex) {
    return models[modelIndex].preSymmetryAtomIndex;
  }

  int getPreSymmetryAtomCount(int modelIndex) {
    return models[modelIndex].preSymmetryAtomCount;
  }

  String getModelName(int modelIndex) {
    if (modelIndex < 0)
      return getModelNumberDotted(-1 - modelIndex);
    return models[modelIndex].modelTag;
  }

  String getModelNumberDotted(int modelIndex) {
    if (modelCount < 1)
      return "";
    return models[modelIndex].modelNumberDotted;
  }
  
  int getModelNumberIndex(int modelNumber, boolean useModelNumber) {
    if (useModelNumber) {
      for (int i = 0; i < modelCount; i++)
        if (models[i].modelNumber == modelNumber)
          return i;
      return -1;
    }
    //new decimal format:   frame 1.2 1.3 1.4
    for (int i = 0; i < modelCount; i++)
      if (models[i].modelFileNumber == modelNumber)
        return i;
    return -1;
  }

  int getModelNumber(int modelIndex) {
    return models[modelIndex].modelNumber;
  }

  int getModelFileNumber(int modelIndex) {
    return models[modelIndex].modelFileNumber;
  }

  Properties getModelProperties(int modelIndex) {
    return modelProperties[modelIndex];
  }

  String getModelProperty(int modelIndex, String property) {
    Properties props = modelProperties[modelIndex];
    return props == null ? null : props.getProperty(property);
  }

  Hashtable getModelAuxiliaryInfo(int modelIndex) {
    return (modelIndex < 0 ? null : modelAuxiliaryInfo[modelIndex]);
  }

  void setModelAuxiliaryInfo(int modelIndex, Object key, Object value) {
    modelAuxiliaryInfo[modelIndex].put(key, value);
  }

  Object getModelAuxiliaryInfo(int modelIndex, String key) {
    if (modelIndex < 0)
      return null;
    Hashtable info = modelAuxiliaryInfo[modelIndex];
    return info == null ? null : info.get(key);
  }

  boolean getModelAuxiliaryInfoBoolean(int modelIndex, String keyName) {
    Hashtable info = modelAuxiliaryInfo[modelIndex];
    return (info != null && info.containsKey(keyName) && ((Boolean) info
        .get(keyName)).booleanValue());
  }

  int getModelAuxiliaryInfoInt(int modelIndex, String keyName) {
    Hashtable info = modelAuxiliaryInfo[modelIndex];
    if (info != null && info.containsKey(keyName)) {
      return ((Integer) info.get(keyName)).intValue();
    }
    return Integer.MIN_VALUE;
  }

  Model getModel(int modelIndex) {
    return models[modelIndex];
  }

  int getNAltLocs(int modelIndex) {
    return models[modelIndex].nAltLocs;
  }
    
  int getNInsertions(int modelIndex) {
    return models[modelIndex].nInsertions;
  }
    
  boolean setModelNameNumberProperties(int modelIndex, String modelName,
                                       int modelNumber,
                                       Properties modelProperties,
                                       Hashtable modelAuxiliaryInfo,
                                       boolean isPDB) {

    this.modelProperties[modelIndex] = modelProperties;
    this.modelAuxiliaryInfo[modelIndex] = modelAuxiliaryInfo;

    String modelTitle = (String) getModelAuxiliaryInfo( modelIndex, "title");
    String modelFile = (String) getModelAuxiliaryInfo( modelIndex, "fileName");
    if (modelNumber != Integer.MAX_VALUE)
      models[modelIndex] = new Model(this, modelIndex, modelNumber, modelName,
          modelTitle, modelFile);
    String codes = (String) getModelAuxiliaryInfo(modelIndex, "altLocs");
    models[modelIndex].setNAltLocs(codes == null ? 0 : codes.length());
    codes = (String) getModelAuxiliaryInfo(modelIndex, "insertionCodes");
    models[modelIndex].setNInsertions(codes == null ? 0 : codes.length());
    return models[modelIndex].isPDB = getModelAuxiliaryInfoBoolean(modelIndex, "isPDB");
  }
  
  /**
   * Model numbers are considerably more complicated in Jmol 11.
   * 
   * int modelNumber
   *  
   *   The adapter gives us a modelNumber, but that is not necessarily
   *   what the user accesses. If a single files is loaded this is:
   *   
   *   a) single file context:
   *   
   *     1) the sequential number of the model in the file , or
   *     2) if a PDB file and "MODEL" record is present, that model number
   *     
   *   b) multifile context:
   *   
   *     always 1000000 * (fileIndex + 1) + (modelIndexInFile + 1)
   *   
   *   
   * int fileIndex
   * 
   *   The 0-based reference to the file containing this model. Used
   *   when doing   "select model=3.2" in a multifile context
   *   
   * int modelFileNumber
   * 
   *   An integer coding both the file and the model:
   *   
   *     file * 1000000 + modelInFile (1-based)
   *     
   *   Used all over the place. Note that if there is only one file,
   *   then modelFileNumber < 1000000.
   * 
   * String modelNumberDotted
   *   
   *   A number the user can use "1.3"
   *   
   * @param baseModelCount
   *    
   */
  void finalizeModelNumbers(int baseModelCount) {
    if (modelCount == baseModelCount)
      return;
    String sNum;
    int modelnumber = 0;
    
    int lastfilenumber = -1;
    if (baseModelCount > 0) {
      if (models[0].modelNumber < 1000000) {
        for (int i = 0; i < baseModelCount; i++) {
          models[i].modelNumber = 1000000 + i + 1;
          models[i].modelNumberDotted = "1." + (i + 1);
          models[i].modelTag = "" + models[i].modelNumber;
        }
      }
      modelnumber = models[baseModelCount - 1].modelNumber;
      modelnumber -= modelnumber % 1000000;
      if (models[baseModelCount].modelNumber < 1000000)
        modelnumber += 1000000;
      for (int i = baseModelCount; i < modelCount; i++) {
        models[i].modelNumber += modelnumber;
        models[i].modelNumberDotted = (modelnumber / 1000000) + "." + (modelnumber % 1000000);
        models[i].modelTag = "" + models[i].modelNumber;
      }
    }
    for (int i = baseModelCount; i < modelCount; ++i) {
      int filenumber = models[i].modelNumber / 1000000;
      if (filenumber != lastfilenumber) {
        modelnumber = 0;
        lastfilenumber = filenumber;
      }
      modelnumber++;
      if (filenumber == 0) {
        // only one file -- take the PDB number or sequential number as given by adapter
        sNum = "" + getModelNumber(i);
        filenumber = 1;
      } else {
        //if only one file, just return the integer file number
        if (modelnumber == 1
            && (i + 1 == modelCount || models[i + 1].modelNumber / 1000000 != filenumber))
          sNum = filenumber + "";
        else
          sNum = filenumber + "." + modelnumber;
      }
      models[i].modelNumberDotted = sNum;
      models[i].fileIndex = filenumber - 1;
      models[i].modelInFileIndex = modelnumber - 1;
      models[i].modelFileNumber = filenumber * 1000000 + modelnumber;
    }
  }

  public static int modelFileNumberFromFloat(float fDotM) {
    //only used in the case of select model = someVariable
    //2.1 and 2.10 will be ambiguous and reduce to 2.1  
    
    int file = (int)(fDotM);
    int model = (int) ((fDotM - file +0.00001) * 10000);
    while (model % 10 == 0)
      model /= 10;
    return file * 1000000 + model;
  }
  
  int getAltLocCountInModel(int modelIndex) {
    return models[modelIndex].nAltLocs;
  }
  private void propogateSecondaryStructure() {
    // issue arises with multiple file loading and multi-_data  mmCIF files
    // that structural information may be model-specific
    
    for (int i = structureCount; --i >= 0;) {
      Structure structure = structures[i];
      for (int j = modelCount; --j >= 0;)
        if (structure.modelIndex == j || structure.modelIndex == -1)
          models[j].addSecondaryStructure(structure.type,
              structure.startChainID, structure.startSeqcode,
              structure.endChainID, structure.endSeqcode);
    }
  }

  public int getModelCount() {
    return modelCount;
  }

  public Model[] getModels() {
    return models;
  }

  int getChainCount() {
    int chainCount = 0;
    for (int i = modelCount; --i >= 0;)
      chainCount += models[i].getChainCount();
    return chainCount;
  }

  int getBioPolymerCount() {
    int polymerCount = 0;
    for (int i = modelCount; --i >= 0;)
      polymerCount += models[i].getBioPolymerCount();
    return polymerCount;
  }

  int getBioPolymerCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return getBioPolymerCount();
    return models[modelIndex].getBioPolymerCount();
  }

  int getChainCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return getChainCount();
    return models[modelIndex].getChainCount();
  }

  int getGroupCount() {
    int groupCount = 0;
    for (int i = modelCount; --i >= 0;)
      groupCount += models[i].getGroupCount();
    return groupCount;
  }

  int getGroupCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return getGroupCount();
    return models[modelIndex].getGroupCount();
  }

  void calcSelectedGroupsCount(BitSet bsSelected) {
    for (int i = modelCount; --i >= 0;)
      models[i].calcSelectedGroupsCount(bsSelected);
  }

  void calcSelectedMonomersCount(BitSet bsSelected) {
    for (int i = modelCount; --i >= 0;)
      models[i].calcSelectedMonomersCount(bsSelected);
  }

  void calcHydrogenBonds(BitSet bsA, BitSet bsB) {
    for (int i = modelCount; --i >= 0; )
      models[i].calcHydrogenBonds(bsA, bsB);
  }

  void selectSeqcodeRange(int seqcodeA, int seqcodeB, BitSet bs) {
    for (int i = modelCount; --i >= 0;)
      models[i].selectSeqcodeRange(seqcodeA, seqcodeB, bs);
  }

  static class Structure {
    String typeName;
    byte type;
    char startChainID;
    int startSeqcode;
    char endChainID;
    int endSeqcode;
    int modelIndex;

    Structure(int modelIndex, String typeName, char startChainID, int startSeqcode,
        char endChainID, int endSeqcode) {
      this.modelIndex = modelIndex;
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
      info.put("type", typeName);
      info.put("startChainID", startChainID + "");
      info.put("startSeqcode", new Integer(startSeqcode));
      info.put("endChainID", endChainID + "");
      info.put("endSeqcode", new Integer(endSeqcode));
      return info;
    }

  }

  Vector getStructureInfo() {
    Vector info = new Vector();
    for (int i = 0; i < structureCount; i++)
      info.addElement(structures[i].toHashtable());
    return info;
  }

}
