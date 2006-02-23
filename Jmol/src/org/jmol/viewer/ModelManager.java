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
import javax.vecmath.Point3i;

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

  String getModelInfo() {
    int modelCount = getModelCount();
    String str =  "model count = " + modelCount +
                 "\nmodelSetHasVibrationVectors:" +
                 modelSetHasVibrationVectors();
    //Properties props = getModelSetProperties();
    //str = str.concat(listProperties(props));
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

  int getModelNumberIndex(int modelNumber) {
    return (frame == null) ? -1 : frame.getModelNumberIndex(modelNumber);
  }

  boolean hasVibrationVectors() {
    return frame.hasVibrationVectors();
  }

  float getRotationRadius() {
    return (frame == null) ? 1 : frame.getRotationRadius();
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

  Hashtable getBoundBoxInfo() {
    Hashtable info = new Hashtable();
    info.put("center", getBoundBoxCenter());
    info.put("edge", getBoundBoxCornerVector());
    return info;
  }

  int getChainCount() {
    return (frame == null) ? 0 : frame.getChainCount();
  }

  int getGroupCount() {
    return (frame == null) ? 0 : frame.getGroupCount();
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

  private final Point3f pointT = new Point3f();
  void setCenterBitSet(BitSet bsCenter) {
    if (frame == null)
      return;
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
    if (viewer.getFriedaSwitch()) {
      if (center == null)
        center = frame.getRotationCenterDefault();
      Point3i newCenterScreen = viewer.transformPoint(center);
      viewer.translateCenterTo(newCenterScreen.x, newCenterScreen.y);
    }
    frame.setRotationCenter(center);
  }

  void setRotationCenter(Point3f center) {
    if (frame != null)
      frame.setRotationCenter(center);
  }

  Point3f getRotationCenter() {
    return (frame == null ? null : frame.getRotationCenter());
  }

  void setRotationCenter(String relativeTo, float x, float y, float z) {
    if (frame == null)
      return;
    pointT.set(x, y, z);
    if (relativeTo == "average")
      pointT.add(frame.getAverageAtomPoint());
    else if (relativeTo == "boundbox")
      pointT.add(frame.getBoundBoxCenter());
    else if (relativeTo != "absolute")
      pointT.set(frame.getRotationCenterDefault());
    frame.setRotationCenter(pointT);
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

  String getModelExtractFromBitSet(BitSet bs) {
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
      if (bs.get(frame.getBondAt(i).getAtom1().atomIndex) && bs.get(frame.getBondAt(i).getAtom2().atomIndex)) {
        int order = getBondOrder(i);
        if (order >= 1 && order < 3) {
          str = str + getBondRecordMOL(i,atomMap);
          nBonds++;
        }
      }
    }
    if(nAtoms > 999 || nBonds > 999) {
      System.out.println("ModelManager.java::getModelExtractFromBitSet: ERROR atom/bond overflow");
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
  
  Hashtable getModelInfoObject() {
    Hashtable info = new Hashtable();
    boolean isPDB = frame.isPDB;
    int modelCount = viewer.getModelCount();
    info.put("modelCount",new Integer(modelCount));
    info.put("modelSetHasVibrationVectors", 
        new Boolean(viewer.modelSetHasVibrationVectors()));
    //Properties props = viewer.getModelSetProperties();
    //str = str.concat(listPropertiesJSON(props));
    Vector models = new Vector();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable model = new Hashtable();
      model.put("_ipt",new Integer(i));
      model.put("num",new Integer(viewer.getModelNumber(i)));
      model.put("name",viewer.getModelName(i));
      model.put("vibrationVectors", new Boolean(viewer.modelHasVibrationVectors(i)));
      if(isPDB)
        addChainInfo(i, model);
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

  Hashtable getAtomInfo(int i, boolean isPDB) {
    Atom atom = frame.getAtomAt(i);
    Hashtable info = new Hashtable();
    info.put("_ipt", new Integer(i));
    info.put("atomno", new Integer(atom.getAtomNumber()));
    info.put("sym", getElementSymbol(i));
    info.put("elemno", new Integer(atom.getElementNumber()));
    info.put("x", new Float(getAtomX(i)));
    info.put("y", new Float(getAtomY(i)));
    info.put("z", new Float(getAtomZ(i)));
    if (frame.vibrationVectors != null && frame.vibrationVectors[i] != null) {
      info.put("vibVector", new Vector3f(frame.vibrationVectors[i]));
    }
    info.put("model", new Integer(atom.getModelTagNumber()));
    info.put("bondCount", new Integer(atom.getCovalentBondCount()));
    info.put("radius", new Float((atom.getRasMolRadius()/120.0)));
    info.put("info", getAtomInfo(i));
    info.put("visible", new Boolean(getAtomVisibility(i)));
    info.put("spacefill", new Integer(atom.madAtom >> 3));
    info.put("color", viewer.getHexColorFromIndex(atom.colixAtom));
    info.put("colix", new Integer(atom.colixAtom));
    info.put("translucent", new Boolean(atom.isTranslucent()));
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

  Vector getBondInfoFromBitSet(BitSet bs) {
    Vector V = new Vector();
    int bondCount = viewer.getBondCount();
    for (int i = 0; i < bondCount; i++)
      if (bs.get(frame.getBondAt(i).getAtom1().atomIndex) && bs.get(frame.getBondAt(i).getAtom2().atomIndex)) 
        V.add(getBondInfo(i));
    return V;
  }

  Hashtable getBondInfo(int i) {
    Hashtable info = new Hashtable();
    info.put("_bpt", new Integer(i));
    info.put("_apt1", new Integer(getBondAtom1(i).atomIndex));
    info.put("_apt2", new Integer(getBondAtom2(i).atomIndex));
    info.put("atomno1", new Integer(getBondAtom1(i).getAtomNumber()));
    info.put("atomno2", new Integer(getBondAtom2(i).getAtomNumber()));
    info.put("order", new Integer(getBondOrder(i)));
    info.put("radius", new Float(frame.getBondAt(i).mad/2000.));
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
    String info = "no header information found";
    if (frame.isPDB) 
      return getPDBHeader();
    if ("xyz" == getModelSetTypeName() && getModelCount() == 1) 
      return getModelName(0);
    // options here for other file formats?
   return info;
  }

  void addChainInfo(int modelIndex, Hashtable modelInfo) {
    Model model = frame.mmset.getModel(modelIndex);
    int nChains = model.getChainCount();
    Vector infoChains = new Vector();    
    for(int i = 0; i < nChains; i++) {
      Chain chain = model.getChain(i);
      Vector infoChain = new Vector();
      int nGroups = chain.getGroupCount();
      for (int igroup = 0; igroup < nGroups; igroup++) {
        Group group = chain.getGroup(igroup);
        Hashtable infoGroup = new Hashtable();
        infoGroup.put("groupID", new Short(group.getGroupID()));
        infoGroup.put("seqCode", group.getSeqcodeString());
        infoGroup.put("_apt1", new Integer(group.firstAtomIndex));
        infoGroup.put("_apt2", new Integer(group.lastAtomIndex));
        infoGroup.put("atomInfo1", getAtomInfo(group.firstAtomIndex));
        infoGroup.put("atomInfo2", getAtomInfo(group.lastAtomIndex));
        infoChain.add(infoGroup);
      }
      infoChains.add(infoChain);
    }
    modelInfo.put("chains",infoChains);
  }  
  

}

