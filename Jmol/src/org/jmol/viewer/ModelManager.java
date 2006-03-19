/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

import org.jmol.api.JmolAdapter;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.awt.Rectangle;

class ModelManager {

  final Viewer viewer;
  final JmolAdapter adapter;

  ModelManager(Viewer viewer, JmolAdapter adapter) {
    this.viewer = viewer;
    this.adapter = adapter;
  }

  String fullPathName;
  String fileName;
  String modelSetName;
  //  int frameCount = 0;
  boolean haveFile = false;
  //  int currentFrameNumber;
  Frame frame;
  //  Frame[] frames;
  
  void setClientFile(String fullPathName, String fileName,
                            Object clientFile) {
    if (clientFile == null) {
      fullPathName = fileName = modelSetName = null;
      frame = null;
      haveFile = false;
    } else {
      this.fullPathName = fullPathName;
      this.fileName = fileName;
      modelSetName = adapter.getAtomSetCollectionName(clientFile);
      if (modelSetName != null) {
        modelSetName = modelSetName.trim();
        if (modelSetName.length() == 0)
          modelSetName = null;
      }
      if (modelSetName == null)
        modelSetName = reduceFilename(fileName);
      frame = new Frame(viewer, adapter, clientFile);
      haveFile = true;
    }
  }

  String reduceFilename(String fileName) {
    if (fileName == null)
      return null;
    int ichDot = fileName.indexOf('.');
    if (ichDot > 0)
      fileName = fileName.substring(0, ichDot);
    if (fileName.length() > 24)
      fileName = fileName.substring(0, 20) + " ...";
    return fileName;
  }

  String getClientAtomStringProperty(Object clientAtom,
                                            String propertyName) {
    return adapter.getClientAtomStringProperty(clientAtom, propertyName);
  }

  Frame getFrame() {
    return frame;
  }

  JmolAdapter getExportJmolAdapter() {
    return (frame == null) ? null : frame.getExportJmolAdapter();
  }

  String getModelSetName() {
    return modelSetName;
  }

  String getModelSetFileName() {
    return fileName;
  }

  String getModelSetPathName() {
    return fullPathName;
  }

  Properties getModelSetProperties() {
    return frame == null ? null : frame.getModelSetProperties();
  }

  String getModelSetProperty(String propertyName) {
    return frame == null ? null : frame.getModelSetProperty(propertyName);
  }

  Hashtable getModelSetAuxiliaryInfo() {
    return frame == null ? null : frame.getModelSetAuxiliaryInfo();
  }

  Object getModelSetAuxiliaryInfo(String keyName) {
    return frame == null ? null : frame.getModelSetAuxiliaryInfo(keyName);
  }

  boolean modelSetHasVibrationVectors() {
    return frame == null ? false : frame.modelSetHasVibrationVectors();
  }

  boolean modelHasVibrationVectors(int modelIndex) {
    return frame == null ? false : frame.modelHasVibrationVectors(modelIndex);
  }

  String getModelSetTypeName() {
    return frame == null ? null : frame.getModelSetTypeName();
  }

  boolean isPDB() {
    return frame == null ? false : frame.isPDB;
  }

  int getModelCount() {
    return (frame == null) ? 0 : frame.getModelCount();
  }

  String getModelInfoAsString() {
    int modelCount = getModelCount();
    String str =  "model count = " + modelCount +
                 "\nmodelSetHasVibrationVectors:" +
                 modelSetHasVibrationVectors();
    Properties props = getModelSetProperties();
    str = str.concat(listProperties(props));
    for (int i = 0; i < modelCount; ++i) {
      str = str.concat("\n" + i + ":" + getModelNumber(i) +
                 ":" + getModelName(i) +
                 "\nmodelHasVibrationVectors:" +
                 modelHasVibrationVectors(i));
      //str = str.concat(listProperties(getModelProperties(i)));
    }
    return str;
  }
  
  String getModelName(int modelIndex) {
    return (frame == null) ? null : frame.getModelName(modelIndex);
  }

  int getModelNumber(int modelIndex) {
    return (frame == null) ? -1 : frame.getModelNumber(modelIndex);
  }

  Properties getModelProperties(int modelIndex) {
    return frame == null ? null : frame.getModelProperties(modelIndex);
  }

  String getModelProperty(int modelIndex, String propertyName) {
    return frame == null ? null : frame.getModelProperty(modelIndex,
                                                         propertyName);
  }

  Hashtable getModelAuxiliaryInfo(int modelIndex) {
    return frame == null ? null : frame.getModelAuxiliaryInfo(modelIndex);
  }

  Object getModelAuxiliaryInfo(int modelIndex, String keyName) {
    return frame == null ? null : frame.getModelAuxiliaryInfo(modelIndex,
        keyName);
  }

  int getModelNumberIndex(int modelNumber) {
    return (frame == null) ? -1 : frame.getModelNumberIndex(modelNumber);
  }

  boolean hasVibrationVectors() {
    return frame.hasVibrationVectors();
  }

  float getRotationRadius() {
    return (frame == null) ? 1 : frame.getRotationRadius();
  }

  Point3f getSpinCenter(String axisID, int modelIndex) {
    Draw draw = (Draw) frame.shapes[JmolConstants.SHAPE_DRAW];
    if (draw == null) 
      return null;
    int meshIndex = draw.getMeshIndex(axisID);
    if (meshIndex < 0) {
      return null;
    }
    return draw.meshes[meshIndex].getSpinCenter(modelIndex);
   }
   
  Vector3f getSpinAxis(String axisID, int modelIndex) {
    Draw draw = (Draw) frame.shapes[JmolConstants.SHAPE_DRAW];
    if (draw == null) 
      return null;
    int meshIndex = draw.getMeshIndex(axisID);
    if (meshIndex < 0) {
      return null;
    }
    return draw.meshes[meshIndex].getSpinAxis(modelIndex);
   }
   
  void increaseRotationRadius(float increaseInAngstroms) {
    if (frame != null)
      frame.increaseRotationRadius(increaseInAngstroms);
  }

  Point3f getBoundBoxCenter() {
    return (frame == null) ? null : frame.getBoundBoxCenter();
  }

  Vector3f getBoundBoxCornerVector() {
    return (frame == null) ? null : frame.getBoundBoxCornerVector();
  }

  Point3f getAtomSetCenter(BitSet bs) {
    return (frame == null) ? null : frame.getAtomSetCenter(bs);
  }

  int getChainCount() {
    return (frame == null) ? 0 : frame.getChainCount();
  }

  int getChainCountInModel(int modelIndex) {
    return (frame == null) ? 0 : frame.getChainCountInModel(modelIndex);
  }
  
  int getGroupCount() {
    return (frame == null) ? 0 : frame.getGroupCount();
  }

  int getGroupCountInModel(int modelIndex) {
    return (frame == null) ? 0 : frame.getGroupCountInModel(modelIndex);
  }
  
  int getPolymerCount() {
    return (frame == null) ? 0 : frame.getPolymerCount();
  }

  int getPolymerCountInModel(int modelIndex) {
    return (frame == null) ? 0 : frame.getPolymerCountInModel(modelIndex);
  }
  
  int getAtomCount() {
    return (frame == null) ? 0 : frame.getAtomCount();
  }

  int getBondCount() {
    return (frame == null) ? 0 : frame.getBondCount();
  }

  int getAtomCountInModel(int modelIndex) {
    return (frame == null) ? 0 : frame.getAtomCountInModel(modelIndex);
  }

  int getBondCountInModel(int modelIndex) {
    return (frame == null) ? 0 : frame.getBondCountInModel(modelIndex);
  }

  Point3f getRotationCenter() {
    return (frame == null ? null : frame.getRotationCenter());
  }

  private final Point3f pointT = new Point3f();
  Point3f setCenterBitSet(BitSet bsCenter, boolean doScale) {
    if (frame == null)
      return new Point3f(0, 0, 0);
    Point3f center = null;
    if (bsCenter != null) {
      int countSelected = 0;
      center = pointT;
      center.set(0,0,0);
      for (int i = getAtomCount(); --i >= 0; ) {
        if (! bsCenter.get(i))
          continue;
        ++countSelected;
        center.add(frame.getAtomPoint3f(i));
      }
      if (countSelected > 0)
        center.scale(1.0f / countSelected); // just divide by the quantity
      else
        center = null;
    }
    
    if (center == null)
      center = frame.getRotationCenterDefault();
    if (viewer.isWindowCentered()) {
      viewer.translateCenterTo(0, 0);
      frame.setRotationCenterAndRadiusXYZ(center, true);
      if (doScale)
        viewer.scaleFitToScreen();
    } else {
      viewer.moveRotationCenter(center);
    }
    return center;
  }

  Point3f setRotationCenterAndRadiusXYZ(Point3f center, boolean andRadius) {
    if (frame == null)
      return null;
    return frame.setRotationCenterAndRadiusXYZ(center, andRadius);
  }

  Point3f setRotationCenterAndRadiusXYZ(String relativeTo, float x, float y, float z) {
    if (frame == null)
      return new Point3f(0, 0, 0);
    pointT.set(x, y, z);
    if (relativeTo == "average")
      pointT.add(frame.getAverageAtomPoint());
    else if (relativeTo == "boundbox")
      pointT.add(frame.getBoundBoxCenter());
    else if (relativeTo != "absolute")
      pointT.set(frame.getRotationCenterDefault());
    frame.setRotationCenterAndRadiusXYZ(pointT, true);
    return pointT;
  }

  boolean autoBond = true;

  void rebond() {
    if (frame != null)
      frame.rebond();
  }

  void setAutoBond(boolean ab) {
    autoBond = ab;
  }

  // angstroms of slop ... from OpenBabel ... mth 2003 05 26
  float bondTolerance = 0.45f;
  void setBondTolerance(float bondTolerance) {
    this.bondTolerance = bondTolerance;
  }

  // minimum acceptable bonding distance ... from OpenBabel ... mth 2003 05 26
  float minBondDistance = 0.4f;
  void setMinBondDistance(float minBondDistance) {
    this.minBondDistance = minBondDistance;
  }

  /*
  void deleteAtom(int atomIndex) {
    frame.deleteAtom(atomIndex);
  }
  */

  boolean frankClicked(int x, int y) {
    return (getShapeSize(JmolConstants.SHAPE_FRANK) != 0 &&
            frame.frankClicked(x, y));
  }

  int findNearestAtomIndex(int x, int y) {
    return (frame == null) ? -1 : frame.findNearestAtomIndex(x, y);
  }

  BitSet findAtomsInRectangle(Rectangle rectRubber) {
    return frame.findAtomsInRectangle(rectRubber);
  }

  // FIXME mth 2004 02 23 -- this does *not* belong here
  float solventProbeRadius = 1.2f;
  void setSolventProbeRadius(float radius) {
    this.solventProbeRadius = radius;
  }

  boolean solventOn = false;
  void setSolventOn(boolean solventOn) {
    this.solventOn = solventOn;
  }

  /****************************************************************
   * shape support
   ****************************************************************/

  int[] shapeSizes = new int[JmolConstants.SHAPE_MAX];
  Hashtable[] shapeProperties = new Hashtable[JmolConstants.SHAPE_MAX];

  void loadShape(int shapeID) {
    if (frame != null)
      frame.loadShape(shapeID);
  }
  
  void setShapeSize(int shapeType, int size, BitSet bsSelected) {
    shapeSizes[shapeType] = size;
    if (frame != null)
      frame.setShapeSize(shapeType, size, bsSelected);
  }
  
  int getShapeSize(int shapeType) {
    return shapeSizes[shapeType];
  }
  
  private static final Object NULL_SURROGATE = new Object();

  void setShapeProperty(int shapeType, String propertyName,
                               Object value, BitSet bsSelected) {
    Hashtable props = shapeProperties[shapeType];
    if (props == null)
      props = shapeProperties[shapeType] = new Hashtable();

    // be sure to intern all propertyNames!
    propertyName = propertyName.intern();
    /*
    System.out.println("propertyName=" + propertyName + "\n" +
                       "value=" + value);
    */

    // Hashtables cannot store null values :-(
    props.put(propertyName, value != null ? value : NULL_SURROGATE);
    if (frame != null)
      frame.setShapeProperty(shapeType, propertyName, value, bsSelected);
  }

  Object getShapeProperty(int shapeType, String propertyName,
                                 int index) {
    Object value = null;
    if (frame != null)
      value = frame.getShapeProperty(shapeType, propertyName, index);
    if (value == null) {
      Hashtable props = shapeProperties[shapeType];
      if (props != null) {
        value = props.get(propertyName);
        if (value == NULL_SURROGATE)
          return value = null;
      }
    }
    return value;
  }

  int getShapeIdFromObjectName(String objectName) {
    for (int i = JmolConstants.SHAPE_MIN_MESH_COLLECTION; 
        i < JmolConstants.SHAPE_MAX; ++i) {
      MeshCollection shape = (MeshCollection) frame.shapes[i];
      if (shape != null && shape.getMeshIndex(objectName) >= 0)
        return i;
    }
    return -1;
  }

  int getAtomIndexFromAtomNumber(int atomNumber) {
    return (frame == null) ? -1 : frame.getAtomIndexFromAtomNumber(atomNumber);
  }

  BitSet getElementsPresentBitSet() {
    return (frame == null) ? null : frame.getElementsPresentBitSet();
  }

  BitSet getGroupsPresentBitSet() {
    return (frame == null) ? null : frame.getGroupsPresentBitSet();
  }

  BitSet getVisibleSet() {
    return (frame == null) ? null : frame.getVisibleSet();
  }

  BitSet getModelAtomBitSet(int modelIndex) {
    BitSet bs = new BitSet();
    int atomCount = getAtomCount();
    Atom[] atoms = frame.atoms;
    for (int i = 0; i < atomCount; i++)
      if (atoms[i].modelIndex == modelIndex)
        bs.set(i);
    return bs;
  }
  
  void calcSelectedGroupsCount(BitSet bsSelected) {
    if (frame != null)
      frame.calcSelectedGroupsCount(bsSelected);
  }

  void calcSelectedMonomersCount(BitSet bsSelected) {
    if (frame != null)
      frame.calcSelectedMonomersCount(bsSelected);
  }

  ////////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  ////////////////////////////////////////////////////////////////

  String getAtomInfo(int i) {
    return frame.getAtomAt(i).getInfo();
  }

/*
String getAtomInfoChime(int i) {
    Atom atom = frame.atoms[i];
    PdbAtom pdbAtom = atom.pdbAtom;
    if (pdbAtom == null)
      return "Atom: " + atom.getAtomicSymbol() + " " + atom.getAtomno();
    return "Atom: " + pdbAtom.getAtomName() + " " + pdbAtom.getAtomSerial() +
      " " + pdbAtom.getGroup3() + " " + pdbAtom.getSeqcodeString() +
      " Chain:" + pdbAtom.getChainID();
  }
*/

  String getElementSymbol(int i) {
    return frame.getAtomAt(i).getElementSymbol();
  }

  int getElementNumber(int i) {
    return frame.getAtomAt(i).getElementNumber();
  }

  String getAtomName(int i) {
    return frame.getAtomAt(i).getAtomName();
  }

  boolean getAtomVisibility(int i) {
    return frame.getAtomAt(i).isVisible();
  }
  int getAtomNumber(int i) {
    return frame.getAtomAt(i).getAtomNumber();
  }

  float getAtomX(int i) {
    return frame.getAtomAt(i).getAtomX();
  }

  float getAtomY(int i) {
    return frame.getAtomAt(i).getAtomY();
  }

  float getAtomZ(int i) {
    return frame.getAtomAt(i).getAtomZ();
  }

  Point3f getAtomPoint3f(int i) {
    return frame.getAtomAt(i).getPoint3f();
  }

  float getAtomRadius(int i) {
    return frame.getAtomAt(i).getRadius();
  }

  short getAtomColix(int i) {
    return frame.getAtomAt(i).getColix();
  }

  String getAtomChain(int i) {
    return "" + frame.getAtomAt(i).getChainID();
  }

  String getAtomSequenceCode(int i) {
    return frame.getAtomAt(i).getSeqcodeString();
  }

  int getAtomModelIndex(int i) {
    return frame.getAtomAt(i).getModelIndex();
  }
  
  Point3f getBondPoint3f1(int i) {
    return frame.getBondAt(i).getAtom1().getPoint3f();
  }

  Point3f getBondPoint3f2(int i) {
    return frame.getBondAt(i).getAtom2().getPoint3f();
  }

  float getBondRadius(int i) {
    return frame.getBondAt(i).getRadius();
  }

  short getBondOrder(int i) {
    return frame.getBondAt(i).getOrder();
  }

  float getBondLength(int i) {
    return getBondAtom1(i).point3f.distance(getBondAtom2(i).point3f);
  }
  
  Atom getBondAtom1(int i) {
    return frame.getBondAt(i).getAtom1();
  }

  Atom getBondAtom2(int i) {
    return frame.getBondAt(i).getAtom2();
  }
  
  short getBondColix1(int i) {
    return frame.getBondAt(i).getColix1();
  }

  short getBondColix2(int i) {
    return frame.getBondAt(i).getColix2();
  }
  
  int getBondModelIndex(int i) {
    Atom atom = frame.getBondAt(i).getAtom1();
    if (atom != null) {
      return atom.getModelIndex();
    }
    atom = frame.getBondAt(i).getAtom2();
    if (atom != null) {
      return atom.getModelIndex();
    }
    return 0;
  }

  public Point3f[] getPolymerLeadMidPoints(int modelIndex, int polymerIndex) {
    Polymer polymer = frame.getPolymerAt(modelIndex, polymerIndex);
    return polymer.getLeadMidpoints();
  }

  String getModelExtract(BitSet bs) {
    String str = "";
    int atomCount = getAtomCount();
    int bondCount = getBondCount();
    int nAtoms = 0;
    int nBonds = 0;
    int[] atomMap = new int[atomCount];
    
    for (int i = 0; i < atomCount; i++) {
      if (bs.get(i)) {
        atomMap[i] = ++nAtoms;
        str = str + getAtomRecordMOL(i);
      }
    }
    for (int i = 0; i < bondCount; i++) {
      if (bs.get(frame.getBondAt(i).getAtom1().atomIndex) 
          && bs.get(frame.getBondAt(i).getAtom2().atomIndex)) {
        int order = getBondOrder(i);
        if (order >= 1 && order < 3) {
          str = str + getBondRecordMOL(i,atomMap);
          nBonds++;
        }
      }
    }
    if(nAtoms > 999 || nBonds > 999) {
      System.out.println("ModelManager.java::getModel: ERROR atom/bond overflow");
      return "";
    }
    // 21 21  0  0  0
    return rFill("   ",""+nAtoms) + rFill("   ",""+nBonds) + "  0  0  0\n" + str;
  }
  
  String getAtomRecordMOL(int i){
    // -2.2240   -1.4442   -0.4577 C 
    return rFill("          " ,(getAtomX(i)+"         ").substring(0,9))
      + rFill("          " ,(getAtomY(i)+"         ").substring(0,9))
      + rFill("          " ,(getAtomZ(i)+"         ").substring(0,9))
      + " " + (getElementSymbol(i) + "  ").substring(0,2) + "\n";
  }

  String getBondRecordMOL(int i,int[] atomMap){
  //  1  2  1
    Bond b = frame.getBondAt(i);
    return rFill("   ","" + atomMap[b.getAtom1().atomIndex])
      + rFill("   ","" + atomMap[b.getAtom2().atomIndex])
      + "  " + getBondOrder(i) + "\n"; 
  }
  
  private String rFill(String s1, String s2) {
    return s1.substring(0, s1.length() - s2.length()) + s2;
  }
  
  
  final static String[] pdbRecords = { "ATOM  ", "HELIX ", "SHEET ", "TURN  ",
    "MODEL ", "SCALE",  "HETATM", "SEQRES",
    "DBREF ", };

  String getPDBHeader() {
    if (! frame.isPDB) {
      return "!Not a pdb file!\n" + getFileHeader();
    }
    String modelFile = viewer.getCurrentFileAsString();
    int ichMin = modelFile.length();
    for (int i = pdbRecords.length; --i >= 0; ) {
      int ichFound = -1;
      String strRecord = pdbRecords[i];
      if (modelFile.startsWith(strRecord))
        ichFound = 0;
      else {
        String strSearch = "\n" + strRecord;
        ichFound = modelFile.indexOf(strSearch);
        if (ichFound >= 0)
          ++ichFound;
      }
      if (ichFound >= 0 && ichFound < ichMin)
        ichMin = ichFound;
    }
    return modelFile.substring(0, ichMin);
  }

  String getFileHeader() {
    String info = getModelSetProperty("fileHeader");
    if (info == null)
      info = getModelSetName();
    if (info != null) return info;
    if (frame.isPDB) 
      return getPDBHeader();
    return "no header information found";
  }

  Hashtable getModelInfo() {
    Hashtable info = new Hashtable();
    int modelCount = viewer.getModelCount();
    info.put("modelSetName",getModelSetName());
    info.put("modelCount",new Integer(modelCount));
    info.put("modelSetHasVibrationVectors", 
        new Boolean(viewer.modelSetHasVibrationVectors()));
    Properties props = viewer.getModelSetProperties();
    if(props != null)
      info.put("modelSetProperties",props);
    Hashtable auxInfo = viewer.getModelSetAuxiliaryInfo();
    if (auxInfo != null)
      info.put("modelSetAuxiliaryInfo",auxInfo);
    Vector models = new Vector();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable model = new Hashtable();
      model.put("_ipt",new Integer(i));
      model.put("num",new Integer(viewer.getModelNumber(i)));
      model.put("name",viewer.getModelName(i));
      model.put("vibrationVectors", new Boolean(viewer.modelHasVibrationVectors(i)));
      model.put("atomCount",new Integer(getAtomCountInModel(i)));
      model.put("bondCount",new Integer(getBondCountInModel(i)));
      model.put("groupCount",new Integer(getGroupCountInModel(i)));
      model.put("polymerCount",new Integer(getPolymerCountInModel(i)));
      model.put("chainCount",new Integer(getChainCountInModel(i)));      
      props = viewer.getModelProperties(i);
      if (props != null)
        model.put("modelProperties", props);
      auxInfo = viewer.getModelAuxiliaryInfo(i);
      if (auxInfo != null)
         model.put("modelAuxiliaryInfo",auxInfo);
      models.add(model);
    }
    info.put("models",models);
    return info;
  }

  Vector getAllAtomInfo(BitSet bs) {
    boolean isPDB = frame.isPDB;
    Vector V = new Vector();
    int atomCount = viewer.getAtomCount();
    for (int i = 0; i < atomCount; i++) 
      if (bs.get(i)) 
        V.add(getAtomInfo(i, isPDB));
    return V;
  }

  void getAtomIdentityInfo(int i, Hashtable info) {
    info.put("_ipt", new Integer(i));
    info.put("atomno", new Integer(getAtomNumber(i)));
    info.put("info", getAtomInfo(i));
    info.put("sym", getElementSymbol(i));
  }
  
  Hashtable getAtomInfo(int i, boolean isPDB) {
    Atom atom = frame.getAtomAt(i);
    Hashtable info = new Hashtable();
    getAtomIdentityInfo(i, info);
    info.put("elemno", new Integer(atom.getElementNumber()));
    info.put("x", new Float(getAtomX(i)));
    info.put("y", new Float(getAtomY(i)));
    info.put("z", new Float(getAtomZ(i)));
    if (frame.vibrationVectors != null && frame.vibrationVectors[i] != null) {
      info.put("vibVector", new Vector3f(frame.vibrationVectors[i]));
    }
    info.put("bondCount", new Integer(atom.getCovalentBondCount()));
    info.put("radius", new Float((atom.getRasMolRadius()/120.0)));
    info.put("model", new Integer(atom.getModelTagNumber()));
    info.put("visible", new Boolean(getAtomVisibility(i)));
    info.put("clickabilityFlags", new Integer(atom.clickabilityFlags));
    info.put("visibilityFlags", new Integer(atom.shapeVisibilityFlags));
    info.put("spacefill", new Integer(atom.madAtom >> 3));
    String strColor = viewer.getHexColorFromIndex(atom.colixAtom);
    if(strColor != null) 
      info.put("color", strColor);
    info.put("colix", new Integer(atom.colixAtom));
    boolean isTranslucent = atom.isTranslucent();
    if (isTranslucent)
      info.put("translucent", new Boolean(isTranslucent));
    info.put("formalCharge", new Integer(atom.getFormalCharge()));
    info.put("partialCharge", new Float(atom.getPartialCharge()));
    
    if (isPDB) {
      info.put("resname", atom.getGroup3());
      info.put("resno", atom.getSeqcodeString());
      char chainID = atom.getChainID();
      info.put("name", getAtomName(i));
      info.put("chain", (chainID == '\0' ? "" : "" + chainID ));
      info.put("atomID", new Integer(atom.getSpecialAtomID()));
      info.put("groupID", new Integer(atom.getGroupID()));
      info.put("altLocation", new String(""+atom.alternateLocationID));
      
      info.put("structure", new Integer(atom.getProteinStructureType()));
      info.put("polymerLength", new Integer(atom.getPolymerLength()));
      info.put("occupancy", new Integer(atom.getOccupancy()));
      int temp = atom.getBfactor100();
      info.put("temp", new Integer((temp<0 ? 0 : temp/100)));
    }
    return info;
  }  

  Vector getAllBondInfo(BitSet bs) {
    Vector V = new Vector();
    int bondCount = viewer.getBondCount();
    for (int i = 0; i < bondCount; i++)
      if (bs.get(frame.getBondAt(i).getAtom1().atomIndex) 
          && bs.get(frame.getBondAt(i).getAtom2().atomIndex)) 
        V.add(getBondInfo(i));
    return V;
  }

  Hashtable getBondInfo(int i) {
    Bond bond = frame.getBondAt(i);
    Atom atom1 = getBondAtom1(i);
    Atom atom2 = getBondAtom2(i);
    Hashtable info = new Hashtable();
    info.put("_bpt", new Integer(i));
    Hashtable infoA = new Hashtable();
    getAtomIdentityInfo(atom1.atomIndex, infoA);
    Hashtable infoB = new Hashtable();
    getAtomIdentityInfo(atom2.atomIndex, infoB);
    info.put("atom1",infoA);
    info.put("atom2",infoB);
    info.put("order", new Integer(getBondOrder(i)));
    info.put("radius", new Float(bond.mad/2000.));
    info.put("length_Ang",new Float(getBondLength(i)));
    info.put("visible", new Boolean(bond.shapeVisibilityFlags != 0));
    String strColor = viewer.getHexColorFromIndex(bond.colix);
    if (strColor != null) 
      info.put("color", strColor);
    info.put("colix", new Integer(bond.colix));
    boolean isTranslucent = bond.isTranslucent();
    if (isTranslucent)
      info.put("translucent", new Boolean(isTranslucent));
   return info;
  }  
  
  String listProperties(Properties props) {
    String str = "";
    if (props == null) {
      str = str.concat("\nProperties: null");
    } else {
      Enumeration e = props.propertyNames();
      str = str.concat("\nProperties:");
      while (e.hasMoreElements()) {
        String propertyName = (String)e.nextElement();
        str = str.concat("\n " + propertyName + "=" +
                   props.getProperty(propertyName));
      }
    }
    return str;
  }

  Hashtable listPropertiesAsObject(Properties props) {
    Hashtable info = new Hashtable();
    if (props == null)
      return info;
    Enumeration e = props.propertyNames();
    while (e.hasMoreElements()) {
      String propertyName = (String)e.nextElement();
      info.put(propertyName,props.getProperty(propertyName));
    }
    return info;
  }

  Hashtable getAllChainInfo(BitSet bs) {
    Hashtable finalInfo = new Hashtable();
    Vector modelVector = new Vector();
    int modelCount = getModelCount();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable modelInfo = new Hashtable();
      Vector info = getChainInfo(i, bs);
      if (info.size() > 0) {
        modelInfo.put("modelIndex",new Integer(i));
        modelInfo.put("chains",info);
        modelVector.add(modelInfo);
      }
    }
    finalInfo.put("models",modelVector);
    return finalInfo;
  }

  Vector getChainInfo(int modelIndex, BitSet bs) {
    Model model = frame.mmset.getModel(modelIndex);
    int nChains = model.getChainCount();
    Vector infoChains = new Vector();    
    for(int i = 0; i < nChains; i++) {
      Chain chain = model.getChain(i);
      Vector infoChain = new Vector();
      int nGroups = chain.getGroupCount();
      Hashtable arrayName = new Hashtable();
      for (int igroup = 0; igroup < nGroups; igroup++) {
        Group group = chain.getGroup(igroup);
        if (! bs.get(group.firstAtomIndex)) 
          continue;
        Hashtable infoGroup = new Hashtable();
        infoGroup.put("groupIndex", new Integer(igroup));
        infoGroup.put("groupID", new Short(group.getGroupID()));
        infoGroup.put("seqCode", group.getSeqcodeString());
        infoGroup.put("_apt1", new Integer(group.firstAtomIndex));
        infoGroup.put("_apt2", new Integer(group.lastAtomIndex));
        infoGroup.put("atomInfo1", getAtomInfo(group.firstAtomIndex));
        infoGroup.put("atomInfo2", getAtomInfo(group.lastAtomIndex));
        infoGroup.put("visibilityFlags", new Integer(group.shapeVisibilityFlags));
        infoChain.add(infoGroup);
      }
      if (! infoChain.isEmpty()) { 
        arrayName.put("residues",infoChain);
        infoChains.add(arrayName);
      }
    }
    return infoChains;
  }  
  
  Hashtable getAllPolymerInfo(BitSet bs) {
    Hashtable finalInfo = new Hashtable();
    Vector modelVector = new Vector();
    int modelCount = getModelCount();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable modelInfo = new Hashtable();
      Vector info = new Vector();
      int polymerCount = getPolymerCountInModel(i);
      for (int ip = 0; ip < polymerCount; ip++) {
        Hashtable polyInfo = getPolymerInfo(i, ip, bs); 
        if (! polyInfo.isEmpty())
          info.add(polyInfo);
      }
      if (info.size() > 0) {
        modelInfo.put("modelIndex",new Integer(i));
        modelInfo.put("polymers",info);
        modelVector.add(modelInfo);
      }
    }
    finalInfo.put("models",modelVector);
    return finalInfo;
  }

  Hashtable getPolymerInfo(int iModel, int iPolymer, BitSet bs) {
    Hashtable returnInfo = new Hashtable();
    Vector info = new Vector();
    Polymer polymer = frame.mmset.getModel(iModel).getPolymer(iPolymer) ;
    int monomerCount = polymer.monomerCount;
    for(int i = 0; i < monomerCount; i++) {
      if (bs.get(polymer.monomers[i].getLeadAtomIndex())) {
        Hashtable monomerInfo = polymer.monomers[i].getMyInfo();
        monomerInfo.put("monomerIndex",new Integer(i));
        info.add(monomerInfo);
      }
    }
    if (info.size() > 0)
      returnInfo.put("monomers",info);
    return returnInfo;
  }
  
  Hashtable getAllStateInfo(BitSet bs) {
    
    /*
     * The idea here is to create a running list that only shows
     * differences between atoms and bonds, taken sequentially.
     * This isn't perfect -- but it might be OK.
     * 
     * The visibility flags give detailed information 
     * 
     */
    Hashtable stateInfo = new Hashtable();
    Vector V = new Vector();
    int atomCount = viewer.getAtomCount();
    short colix = -1;
    short lastColix = -1;
    int shapeVisibilityFlags = 0;
    int lastVisibilityFlags = -1;
    int mad = 0;
    int lastMad = -1;
    int defaultMadCode = viewer.getMadAtom();
    boolean isFirst = true;
    Hashtable elementInfo = new Hashtable();
    for (int i = 0; i < atomCount; i++) {
      if (! bs.get(i)) 
        continue;
      boolean isChanged = false;
      Hashtable info = new Hashtable();
      Atom atom = frame.getAtomAt(i);
      String element = getElementSymbol(i); 
      if (!elementInfo.containsKey(element)) {
        Hashtable Htable = new Hashtable();
        Htable.put("mad","" + atom.convertEncodedMad(defaultMadCode));  
        elementInfo.put(element, Htable);
      }  
      Hashtable thisElementInfo = (Hashtable)elementInfo.get(element);
      String value;
      String str;
      
      str = "" + atom.madAtom;
      value = (String)thisElementInfo.get("mad");
      if (! str.equals(value)) {
        thisElementInfo.put("mad",str);
        info.put("mad", new Integer(atom.madAtom));
        isChanged = true;
      }
      
      if (atom.colixAtom >= 0) {
        str = "" + atom.colixAtom;
        value = (String)thisElementInfo.get("colix");
        if (! str.equals(value)) {
          thisElementInfo.put("colix",str);
          info.put("colix", new Integer(atom.colixAtom));
          String strColor = viewer.getHexColorFromIndex(atom.colixAtom);
          if(strColor != null) 
            info.put("color", strColor);
          isChanged = true;
        }
      }

      shapeVisibilityFlags = atom.shapeVisibilityFlags;
      if (isFirst || shapeVisibilityFlags != lastVisibilityFlags)
        info.put("visibilityFlags", new Integer(shapeVisibilityFlags));

      if (!info.isEmpty()) {
        info.put("element", element);
        info.put("_ipt", new Integer(i));
        V.add(info);
      }

      lastVisibilityFlags = shapeVisibilityFlags;
      isFirst = false;
      if (isChanged) {
        elementInfo.put(element, thisElementInfo);
      }
    }
    stateInfo.put("atomState",V);
    
    isFirst = true;
    V = new Vector();
    int bondCount = viewer.getBondCount();
    for (int i = 0; i < bondCount; i++) {
      if (! bs.get(frame.getBondAt(i).getAtom1().atomIndex) 
          || bs.get(frame.getBondAt(i).getAtom2().atomIndex))
        continue;
      Bond bond = frame.getBondAt(i);
      Hashtable info = new Hashtable();
      mad = bond.mad;
      if (isFirst || mad != lastMad)
        info.put("mad", new Integer(mad));
      colix = bond.colix;
      if (isFirst || colix >= 0 && colix != lastColix) {
        info.put("colix", new Integer(bond.colix));
        String strColor = viewer.getHexColorFromIndex(bond.colix);
        if(strColor != null) 
          info.put("color", strColor);
      }
      if (!info.isEmpty()) {
        info.put("_ipt", new Integer(i));
        V.add(info);
      }

      //not taking into account possible connection changes here
      
      lastMad = mad;
      lastColix = colix;
      isFirst = false;
    }
    stateInfo.put("bondState", V);
    stateInfo.put("shapeInfo", getShapeInfo());
    stateInfo.put("modelInfo", getModelInfo());
    stateInfo.put("animationInfo", viewer.getAnimationInfo());
    stateInfo.put("polymerInfo",getAllPolymerInfo(bs));
    return stateInfo;
  }
  
  Hashtable getBoundBoxInfo() {
    Hashtable info = new Hashtable();
    info.put("center", getBoundBoxCenter());
    info.put("edge", getBoundBoxCornerVector());
    return info;
  }
  
  void setModelVisibility() {
    if (frame == null)
      return;
    Atom[] atoms = frame.atoms;
    int displayModelIndex = viewer.getDisplayModelIndex();
    boolean isOneFrame = (displayModelIndex >= 0); 
    boolean showHydrogens = viewer.getShowHydrogens();
    int ballVisibilityFlag = viewer.getShapeVisibilityFlag(JmolConstants.SHAPE_BALLS);
    int haloVisibilityFlag = viewer.getShapeVisibilityFlag(JmolConstants.SHAPE_HALO);
    BitSet bs = viewer.getVisibleFramesBitSet();
    Draw draw = (Draw) frame.shapes[JmolConstants.SHAPE_DRAW];
    if (draw != null)
      draw.setVisibilityFlags(bs);
    for (int i = frame.atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      atom.shapeVisibilityFlags &= (
          ~JmolConstants.ATOM_IN_MODEL
          & ~ballVisibilityFlag
          & ~haloVisibilityFlag);
      if (atom.madAtom == JmolConstants.MAR_DELETED
          || ! showHydrogens && atom.elementNumber == 1)
        continue;
      if (! isOneFrame || atom.modelIndex == displayModelIndex) { 
        atom.shapeVisibilityFlags |= JmolConstants.ATOM_IN_MODEL;
        if (atom.madAtom != 0)
          atom.shapeVisibilityFlags |= ballVisibilityFlag;
        if(viewer.hasSelectionHalo(atom.atomIndex))
          atom.shapeVisibilityFlags |= haloVisibilityFlag;
      }
    }
  }
  
  void setModelClickability() {
    if (frame == null)
      return;
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = frame.shapes[i];
      if (shape != null) {
        shape.setModelClickability();
      }
    }
  }
 
  void checkObjectClicked(int x, int y, boolean isShiftDown) {
    if (frame == null)
      return;
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = frame.shapes[i];
      if (shape != null) {
        shape.checkObjectClicked(x, y, isShiftDown);
      }
    }
  }
 
  Hashtable getShapeInfo() {
    Hashtable info = new Hashtable();
    if (frame == null)
      return info;
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = frame.shapes[i];
      if (shape != null) {
        String shapeType = JmolConstants.shapeClassBases[i];
        Hashtable shapeinfo = new Hashtable();
        shapeinfo.put("index",new Integer(i));
        shapeinfo.put("myVisibilityFlag",new Integer(shape.myVisibilityFlag));
        if(shapeType == "Draw")
          shapeinfo.put("obj",shape.getShapeDetail());
        info.put(shapeType,shapeinfo);
      }
    }
    if (viewer.selectionHaloEnabled) {
      Hashtable shapeinfo = new Hashtable();
      shapeinfo.put("index",new Integer(JmolConstants.SHAPE_HALO));
      shapeinfo.put("myVisibilityFlag",new Integer(viewer.getShapeVisibilityFlag(JmolConstants.SHAPE_HALO)));
      info.put("halo",shapeinfo);
    }
    return info;
  }
  
  Point3f getAveragePosition(int atomIndex1, int atomIndex2) {
    return frame.getAveragePosition(atomIndex1, atomIndex2);
  }

  Vector3f getAtomVector(int atomIndex1, int atomIndex2) {
    return frame.getAtomVector(atomIndex1, atomIndex2);
  }
  
}
