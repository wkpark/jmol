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
package org.jmol.modelframe;

import org.jmol.shape.Dipoles;
import org.jmol.shape.Shape;
import org.jmol.shape.MeshCollection;
import org.jmol.symmetry.SpaceGroup;
import org.jmol.symmetry.UnitCell;
import org.jmol.util.Logger;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;

import org.jmol.api.JmolAdapter;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.AtomIndexIterator;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;
import java.awt.Rectangle;
import org.jmol.modelframe.Frame.CellInfo;

public class ModelManager {

  private final Viewer viewer;
  private Frame frame;

  private String fullPathName;
  private String fileName;
  private String modelSetName;
  //boolean haveFile;


  public ModelManager(Viewer viewer) {
    this.viewer = viewer;
  }

  public void clear() {
    fullPathName = fileName = modelSetName = null;
    //haveFile = false;
    clearFrame();
  }

  void clearFrame() {
    //just a bit cleaner -- never two frames in memory,
    //even if one would always be "empty"
    frame = null;
    //System.gc();
  }
  public void zap() {
    clear();
    fullPathName = fileName = modelSetName = "zapped";
    frame = new Frame(viewer, "empty");
  }
  
  public void merge(JmolAdapter adapter, Object clientFile ) {
    frame = new Frame(viewer, adapter, clientFile, frame);
    //haveFile = true;
    if (frame.atomCount == 0)
      zap();
  }
  
  public void setClientFile(String fullPathName, String fileName, JmolAdapter adapter, Object clientFile) {
    if (clientFile == null) {
      clear();
      return;
    }
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
    clearFrame();
    frame = new Frame(viewer, adapter, clientFile, null);
    //haveFile = true;
    if (frame.atomCount == 0)
      zap();
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

  public Frame getFrame() {
    return frame;
  }

  public String getModelSetName() {
    return modelSetName;
  }

  public String getModelSetFileName() {
    return fileName;
  }

  public String getModelSetPathName() {
    return fullPathName;
  }

  public Properties getModelSetProperties() {
    return frame.getModelSetProperties();
  }

  public String getModelSetProperty(String propertyName) {
    return frame.getModelSetProperty(propertyName);
  }

  public Hashtable getModelSetAuxiliaryInfo() {
    return frame.getModelSetAuxiliaryInfo();
  }

  public Object getModelSetAuxiliaryInfo(String keyName) {
    return frame.getModelSetAuxiliaryInfo(keyName);
  }

  public boolean modelSetHasVibrationVectors() {
    return frame.modelSetHasVibrationVectors();
  }

  public boolean modelHasVibrationVectors(int modelIndex) {
    return frame.modelHasVibrationVectors(modelIndex);
  }

  public String getModelSetTypeName() {
    return frame.getModelSetTypeName();
  }

  boolean isPDB() {
    return frame.isPDB;
  }

  boolean isPDB(int modelIndex) {
    return frame.mmset.getModel(modelIndex).isPDB;
  }

  public int getModelCount() {
    return frame.getModelCount();
  }

  public String getModelInfoAsString() {
    int modelCount = getModelCount();
    String str =  "model count = " + modelCount +
                 "\nmodelSetHasVibrationVectors:" +
                 modelSetHasVibrationVectors();
    Properties props = getModelSetProperties();
    str = str.concat(listProperties(props));
    for (int i = 0; i < modelCount; ++i) {
      str = str.concat("\n" + i + ":" + getModelName(-1 -i) +
                 ":" + getModelTitle(i) +
                 "\nmodelHasVibrationVectors:" +
                 modelHasVibrationVectors(i));
      //str = str.concat(listProperties(getModelProperties(i)));
    }
    return str;
  }
  
  public String getSymmetryInfoAsString() {
    int modelCount = getModelCount();
    String str = "Symmetry Information:";
    for (int i = 0; i < modelCount; ++i) {
      str += "\nmodel #" + getModelName(-1 - i) + "; name=" + getModelName(i) + "\n"
          + frame.getSymmetryInfoAsString(i);
    }
    return str;
  }
  
  public String getModelName(int modelIndex) {
    //necessary for status manager frame change?
    return frame == null ? null : frame.getModelName(modelIndex);
  }

  public String getModelTitle(int modelIndex) {
    //necessary for status manager frame change?
    return frame == null ? null : frame.getModelTitle(modelIndex);
  }

  public String getModelFile(int modelIndex) {
    //necessary for status manager frame change?
    return frame == null ? null : frame.getModelFile(modelIndex);
  }

  public int getModelNumber(int modelIndex) {
    return frame.getModelNumber(modelIndex);
  }

  public int getModelFileNumber(int modelIndex) {
    return frame.getModelFileNumber(modelIndex);
  }

  public Properties getModelProperties(int modelIndex) {
    return frame.getModelProperties(modelIndex);
  }

  public String getModelProperty(int modelIndex, String propertyName) {
    return frame.getModelProperty(modelIndex, propertyName);
  }

  public Hashtable getModelAuxiliaryInfo(int modelIndex) {
    return frame.getModelAuxiliaryInfo(modelIndex);
  }

  public Object getModelAuxiliaryInfo(int modelIndex, String keyName) {
    return frame.getModelAuxiliaryInfo(modelIndex,
        keyName);
  }

  public int getModelNumberIndex(int modelNumber, boolean useModelNumber) {
    return frame.getModelNumberIndex(modelNumber, useModelNumber);
  }

  public float calcRotationRadius(Point3f center) {
    return frame.calcRotationRadius(center);
  }
  
  public float calcRotationRadius(BitSet bs) {
    return frame.calcRotationRadius(bs);
  }

  public Point3f getBoundBoxCenter() {
    return frame.getBoundBoxCenter();
  }

  public Point3f getAverageAtomPoint() {
    return frame.getAverageAtomPoint();
  }

  public Vector3f getBoundBoxCornerVector() {
    return frame.getBoundBoxCornerVector();
  }

  public Point3f getAtomSetCenter(BitSet bs) {
    return frame.getAtomSetCenter(bs);
  }

  public BitSet getAtomBits(String setType) {
    return frame.getAtomBits(setType);
  }

  public BitSet getAtomBits(String setType, String specInfo) {
    return frame.getAtomBits(setType, specInfo);
  }

  public BitSet getAtomBits(String setType, int specInfo) {
    return frame.getAtomBits(setType, specInfo);
  }

  public BitSet getAtomBits(String setType, int[] specInfo) {
    return frame.getAtomBits(setType, specInfo);
  }

  public int getAtomCount() {
    return frame.getAtomCount();
  }

  public int getAtomCountInModel(int modelIndex) {
    return modelIndex < 0 ? getAtomCount() : frame
        .getAtomCountInModel(modelIndex);
  }

  public int getBondCount() {
    return frame.getBondCount();
  }

  public int getBondCountInModel(int modelIndex) {
    return frame.getBondCountInModel(modelIndex);
  }

  public BitSet getBondsForSelectedAtoms(BitSet bsAtoms) {
    return frame.getBondsForSelectedAtoms(bsAtoms);
  }

  public int getGroupCount() {
    return frame.getGroupCount();
  }

  public int getGroupCountInModel(int modelIndex) {
    return modelIndex < 0 ? getGroupCount() : frame
        .getGroupCountInModel(modelIndex);
  }
  
  public int getChainCount() {
    return frame.getChainCount();
  }

  public int getChainCountInModel(int modelIndex) {
    return modelIndex < 0 ? getChainCount() : frame
        .getChainCountInModel(modelIndex);
  }
  
  public int getBioPolymerCount() {
    return frame.getBioPolymerCount();
  }

  public int getBioPolymerCountInModel(int modelIndex) {
    return modelIndex < 0 ? getBioPolymerCount() : frame
        .getBioPolymerCountInModel(modelIndex);
  }
  
  int getMoleuleCount() {
    return (frame.getMoleculeCount());
  }

  public int makeConnections(float minDistance, float maxDistance, short order,
                      int connectOperation, BitSet bsA, BitSet bsB, BitSet bsBonds, boolean isBonds) {
    return (frame.makeConnections(minDistance, maxDistance,
        order, connectOperation, bsA, bsB, bsBonds, isBonds));
  }

  public void rebond() {
    frame.rebond();
  }

  public boolean frankClicked(int x, int y) {
    return (getShapeSize(JmolConstants.SHAPE_FRANK) != 0 &&
            frame.frankClicked(x, y));
  }

  public int findNearestAtomIndex(int x, int y) {
    if (frame == null)
      return -1;
    return frame.findNearestAtomIndex(x, y);
  }

  public BitSet findAtomsInRectangle(Rectangle rectRubber) {
    return frame.findAtomsInRectangle(rectRubber);
  }

  /****************************************************************
   * shape support
   ****************************************************************/

  int[] shapeSizes = new int[JmolConstants.SHAPE_MAX];
 // Hashtable[] shapeProperties = new Hashtable[JmolConstants.SHAPE_MAX];

  public void loadShape(int shapeID) {
    frame.loadShape(shapeID);
  }
  
  public void setShapeSize(int shapeType, int size, BitSet bsSelected) {
    shapeSizes[shapeType] = size;
    frame.setShapeSize(shapeType, size, bsSelected);
  }
  
  public int getShapeSize(int shapeType) {
    return shapeSizes[shapeType];
  }
  
  public void setShapeProperty(int shapeType, String propertyName,
                               Object value, BitSet bsSelected) {
    // this one is necessary, because we do try to load some shapes
    // while the frame itself is loading.
   // if (frame != null)
      frame.setShapeProperty(shapeType, propertyName.intern(), value, bsSelected);
  }

  public Object getShapeProperty(int shapeType, String propertyName,
                                 int index) {
    return frame.getShapeProperty(shapeType, propertyName, index);
  }

  public int getShapeIdFromObjectName(String objectName) {
    int i;
    for (i = JmolConstants.SHAPE_MIN_MESH_COLLECTION; 
        i < JmolConstants.SHAPE_FRANK; ++i) {
      MeshCollection shape = (MeshCollection) frame.shapes[i];
      if (shape != null && shape.getIndexFromName(objectName) >= 0)
        return i;
    }
    i = JmolConstants.SHAPE_DIPOLES;
    Dipoles dipoles = (Dipoles) frame.shapes[i];
    if (dipoles != null && dipoles.getIndexFromName(objectName) >= 0)
      return i;    
    return -1;
  }

  public int getAtomIndexFromAtomNumber(int atomNumber) {
    return frame.getAtomIndexFromAtomNumber(atomNumber);
  }

  public BitSet getElementsPresentBitSet(int modelIndex) {
    return frame.getElementsPresentBitSet(modelIndex);
  }

  public Hashtable getHeteroList(int modelIndex) {
    return frame.mmset.getHeteroList(modelIndex);
  }

  public BitSet getVisibleSet() {
    return frame.getVisibleSet();
  }

  public BitSet getClickableSet() {
    return frame.getClickableSet();
  }

  public BitSet getModelAtomBitSet(int modelIndex) {
    return frame.getModelAtomBitSet(modelIndex);
  }

  public BitSet getModelBitSet(BitSet atomList) {
    return frame.getModelBitSet(atomList);
  }

  BitSet getMoleculeBitSet(int modelIndex) {
    return frame.getMoleculeBitSet(modelIndex);
  }

  public void calcSelectedGroupsCount(BitSet bsSelected) {
    frame.calcSelectedGroupsCount(bsSelected);
  }

  public void calcSelectedMonomersCount(BitSet bsSelected) {
    frame.calcSelectedMonomersCount(bsSelected);
  }

  public void calcSelectedMoleculesCount(BitSet bsSelected) {
    frame.calcSelectedGroupsCount(bsSelected);
  }

  ////////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  ////////////////////////////////////////////////////////////////

  public String getAtomInfo(int i) {
    return frame.getAtomAt(i).getInfo();
  }

  public String getAtomInfoXYZ(int i, boolean withScreens) {
    return frame.getAtomAt(i).getInfoXYZ(withScreens);
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

  public String getElementSymbol(int i) {
    return frame.getAtomAt(i).getElementSymbol();
  }

  public int getElementNumber(int i) {
    return frame.getAtomAt(i).getElementNumber();
  }

  String getElementName(int i) {
      return JmolConstants.elementNameFromNumber(frame.getAtomAt(i)
          .getAtomicAndIsotopeNumber());
  }

  public String getAtomName(int i) {
    return frame.getAtomAt(i).getAtomName();
  }

  boolean getAtomVisibility(int i) {
    return frame.getAtomAt(i).isVisible();
  }
  public int getAtomNumber(int i) {
    return frame.getAtomAt(i).getAtomNumber();
  }

  public float getAtomX(int i) {
    return frame.getAtomAt(i).x;
  }

  public float getAtomY(int i) {
    return frame.getAtomAt(i).y;
  }

  public float getAtomZ(int i) {
    return frame.getAtomAt(i).z;
  }

  public Point3f getAtomPoint3f(int i) {
    return frame.getAtomAt(i);
  }

  public float getAtomRadius(int i) {
    return frame.getAtomAt(i).getRadius();
  }

  public float getAtomVdwRadius(int i) {
    return frame.getAtomAt(i).getVanderwaalsRadiusFloat();
  }

  public short getAtomColix(int i) {
    return frame.getAtomAt(i).getColix();
  }

  public String getAtomChain(int i) {
    return "" + frame.getAtomAt(i).getChainID();
  }

  public String getAtomSequenceCode(int i) {
    return frame.getAtomAt(i).getSeqcodeString();
  }

  public int getAtomModelIndex(int i) {
    return frame.getAtomAt(i).getModelIndex();
  }
  
  public Point3f getBondPoint3f1(int i) {
    return frame.getBondAt(i).getAtom1();
  }

  public Point3f getBondPoint3f2(int i) {
    return frame.getBondAt(i).getAtom2();
  }

  public float getBondRadius(int i) {
    return frame.getBondAt(i).getRadius();
  }

  public short getBondOrder(int i) {
    return frame.getBondAt(i).getOrder();
  }

  float getBondLength(int i) {
    return getBondAtom1(i).distance(getBondAtom2(i));
  }
  
  Atom getBondAtom1(int i) {
    return frame.getBondAt(i).getAtom1();
  }

  Atom getBondAtom2(int i) {
    return frame.getBondAt(i).getAtom2();
  }
  
  public short getBondColix1(int i) {
    return frame.getBondAt(i).getColix1();
  }

  public short getBondColix2(int i) {
    return frame.getBondAt(i).getColix2();
  }
  
  public int getBondModelIndex(int i) {
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

  public BitSet getAtomsWithin(String withinWhat, String specInfo, BitSet bs) {
    if (withinWhat.equals("sequence"))
      return withinSequence(specInfo, bs);
    return null;
  }
  
  public BitSet getAtomsWithin(String withinWhat, BitSet bs) {
    if (withinWhat.equals("group"))
      return withinGroup(bs);
    if (withinWhat.equals("chain"))
      return withinChain(bs);
    if (withinWhat.equals("molecule"))
      return withinMolecule(bs);
    if (withinWhat.equals("model"))
      return withinModel(bs);
    if (withinWhat.equals("element"))
      return withinElement(bs);
    if (withinWhat.equals("site"))
      return withinSite(bs);
    return null;
  }

  BitSet withinGroup(BitSet bs) {
    //Logger.debug("withinGroup");
    Group groupLast = null;
    BitSet bsResult = new BitSet();
    for (int i = getAtomCount(); --i >= 0;) {
      if (!bs.get(i))
        continue;
      Atom atom = frame.getAtomAt(i);
      Group group = atom.getGroup();
      if (group != groupLast) {
        group.selectAtoms(bsResult);
        groupLast = group;
      }
    }
    return bsResult;
  }

  BitSet withinChain(BitSet bs) {
    Chain chainLast = null;
    BitSet bsResult = new BitSet();
    int atomCount = getAtomCount();
    Atom[] atoms = frame.atoms;
    for (int i = atomCount; --i >= 0;) {
      if (!bs.get(i))
        continue;
      Chain chain = atoms[i].getChain();
      if (chain != chainLast) {
        for (int j = atomCount; --j >= 0;)
          if (atoms[j].getChain() == chain)
            bs.set(j);
        chainLast = chain;
      }
    }
    return bsResult;
  }

  BitSet withinMolecule(BitSet bs) {
    return frame.getMoleculeBitSet(bs);
  }

  BitSet withinModel(BitSet bs) {
    BitSet bsResult = new BitSet();
    BitSet bsThis = new BitSet();
    for (int i = getAtomCount(); --i >= 0;)
      if (bs.get(i))
        bsThis.set(frame.getAtomAt(i).modelIndex);
    for (int i = getAtomCount(); --i >= 0;)
      if (bsThis.get(frame.getAtomAt(i).modelIndex))
        bsResult.set(i);
    return bsResult;
  }

  BitSet withinSite(BitSet bs) {
    //Logger.debug("withinGroup");
    BitSet bsResult = new BitSet();
    BitSet bsThis = new BitSet();
    for (int i = getAtomCount(); --i >= 0;)
      if (bs.get(i))
        bsThis.set(frame.getAtomAt(i).atomSite);
    for (int i = getAtomCount(); --i >= 0;)
      if (bsThis.get(frame.getAtomAt(i).atomSite))
        bsResult.set(i);
    return bsResult;
  }

  BitSet withinElement(BitSet bs) {
    //Logger.debug("withinGroup");
    BitSet bsResult = new BitSet();
    BitSet bsThis = new BitSet();
    for (int i = getAtomCount(); --i >= 0;)
      if (bs.get(i))
        bsThis.set(getElementNumber(i));
    for (int i = getAtomCount(); --i >= 0;)
      if (bsThis.get(getElementNumber(i)))
        bsResult.set(i);
    return bsResult;
  }

  BitSet withinSequence(String specInfo, BitSet bs) {
    //Logger.debug("withinSequence");
    String sequence = "";
    int lenInfo = specInfo.length();
    BitSet bsResult = new BitSet();
    if (lenInfo == 0)
      return bsResult;
    int modelCount = viewer.getModelCount();
    for (int i = 0; i < modelCount; ++i) {
      int polymerCount = getBioPolymerCountInModel(i);
      for (int ip = 0; ip < polymerCount; ip++) {
        sequence = getPolymerSequence(i, ip);
        int j = -1;
        while ((j = sequence.indexOf(specInfo, ++j)) >=0)
          getPolymerSequenceAtoms(i, ip, j, lenInfo, bs, bsResult);
      }
    }
    return bsResult;
  }

  void selectModelIndexAtoms(int modelIndex, BitSet bsResult) {
    Frame frame = viewer.getFrame();
    for (int i = viewer.getAtomCount(); --i >= 0;)
      if (frame.getAtomAt(i).getModelIndex() == modelIndex)
        bsResult.set(i);
  }

  public BitSet getAtomsWithin(float distance, BitSet bs) {
    BitSet bsResult = new BitSet();
    for (int i = frame.getAtomCount(); --i >= 0;) {
      if (bs.get(i)) {
        Atom atom = frame.getAtomAt(i);
        AtomIterator iterWithin = frame.getWithinModelIterator(atom,
            distance);
        while (iterWithin.hasNext())
          bsResult.set(iterWithin.next().getAtomIndex());
      }
    }
    return bsResult;
  }

  public BitSet getAtomsWithin(float distance, Point3f coord) {
    BitSet bsResult = new BitSet();
    for (int i = frame.getAtomCount(); --i >= 0;) {
      Atom atom = frame.getAtomAt(i);
      if (atom.distance(coord) <= distance)
        bsResult.set(atom.atomIndex);
    }
    return bsResult;
  }

  public BitSet getAtomsWithin(float distance, Point4f plane) {
    return frame.getAtomsWithin(distance, plane);
  }

  public void loadCoordinates(String coordinateData) {
    frame.loadCoordinates(coordinateData);
  }
 
  public BitSet getAtomsConnected(float min, float max, int intType, BitSet bs) {
    BitSet bsResult = new BitSet();
    int atomCount = getAtomCount();
    int[] nBonded = new int[atomCount];
    int bondCount = getBondCount();
    int i;
    for (int ibond = 0; ibond < bondCount; ibond++) {
      Bond bond = frame.bonds[ibond];
      if (intType == JmolConstants.BOND_ORDER_ANY || bond.order == intType) {
        if (bs.get(bond.atom1.atomIndex)) {
          nBonded[i = bond.atom2.atomIndex]++;
          bsResult.set(i);
        }
        if (bs.get(bond.atom2.atomIndex)) {
          nBonded[i = bond.atom1.atomIndex]++;
          bsResult.set(i);
        }
      }
    }
    boolean nonbonded = (min == 0 && max == 0);
    for (i = atomCount; --i >= 0;) {
      int n = nBonded[i];
      if (n < min || n > max)
        bsResult.clear(i);
      else if (nonbonded && n == 0)
        bsResult.set(i);
    }
    return bsResult;
  }

  public String getModelExtract(BitSet bs) {
    int atomCount = getAtomCount();
    int bondCount = getBondCount();
    int nAtoms = 0;
    int nBonds = 0;
    int[] atomMap = new int[atomCount];
    StringBuffer mol = new StringBuffer();
    StringBuffer s = new StringBuffer();
    
    for (int i = 0; i < atomCount; i++) {
      if (bs.get(i)) {
        atomMap[i] = ++nAtoms;
        getAtomRecordMOL(s, i);
      }
    }
    for (int i = 0; i < bondCount; i++) {
      if (bs.get(frame.getBondAt(i).getAtom1().atomIndex) 
          && bs.get(frame.getBondAt(i).getAtom2().atomIndex)) {
        int order = getBondOrder(i);
        if (order >= 1 && order < 3) {
          getBondRecordMOL(s, i,atomMap);
          nBonds++;
        }
      }
    }
    if(nAtoms > 999 || nBonds > 999) {
      Logger.error("ModelManager.java::getModel: ERROR atom/bond overflow");
      return "";
    }
    // 21 21  0  0  0
    rFill(mol, "   ",""+nAtoms);
    rFill(mol, "   ",""+nBonds);
    mol.append("  0  0  0\n");
    mol.append(s);
    return mol.toString();
  }
  
  void getAtomRecordMOL(StringBuffer s, int i){
    //   -0.9920    3.2030    9.1570 Cl  0  0  0  0  0
    //    3.4920    4.0920    5.8700 Cl  0  0  0  0  0
    //012345678901234567890123456789012
    rFill(s, "          " ,safeTruncate(getAtomX(i),9));
    rFill(s, "          " ,safeTruncate(getAtomY(i),9));
    rFill(s, "          " ,safeTruncate(getAtomZ(i),9));
    s.append(" ").append((getElementSymbol(i) + "  ").substring(0,2)).append("\n");
  }

  void getBondRecordMOL(StringBuffer s, int i,int[] atomMap){
  //  1  2  1
    Bond b = frame.getBondAt(i);
    rFill(s, "   ","" + atomMap[b.getAtom1().atomIndex]);
    rFill(s, "   ","" + atomMap[b.getAtom2().atomIndex]);
    s.append("  ").append(getBondOrder(i)).append("\n"); 
  }
  
  private void rFill(StringBuffer s, String s1, String s2) {
    s.append(s1.substring(0, s1.length() - s2.length()));
    s.append(s2);
  }
  
  private String safeTruncate(float f, int n) {
    if (f > -0.001 && f < 0.001)
      f = 0;
    return (f + "         ").substring(0,n);
  }

/*
  final static String[] pdbRecords = { "ATOM  ", "HELIX ", "SHEET ", "TURN  ",
    "MODEL ", "SCALE",  "HETATM", "SEQRES",
    "DBREF ", };
*/

  final static String[] pdbRecords = { "ATOM  ", "MODEL ", "HETATM" };

  public String getFileHeader() {
    if (frame.isPDB) 
      return getFullPDBHeader();
    String info = getModelSetProperty("fileHeader");
    if (info == null)
      info = getModelSetName();
    if (info != null) return info;
    return "no header information found";
  }

  public String getPDBHeader() {
    return (frame.isPDB ? getFullPDBHeader() : getFileHeader());
  }
  
  String getFullPDBHeader() {
    String info = viewer.getCurrentFileAsString();
    int ichMin = info.length();
    for (int i = pdbRecords.length; --i >= 0;) {
      int ichFound;
      String strRecord = pdbRecords[i];
      switch (ichFound = (info.startsWith(strRecord) ? 0 : 
        info.indexOf("\n" + strRecord))) {
      case -1:
        continue;
      case 0:
        return "";
      default:
        if (ichFound < ichMin)
          ichMin = ++ichFound;
      }
    }
    return info.substring(0, ichMin);
  }

  public Hashtable getModelInfo() {
    Hashtable info = new Hashtable();
    int modelCount = viewer.getModelCount();
    info.put("modelSetName",getModelSetName());
    info.put("modelCount",new Integer(modelCount));
    info.put("modelSetHasVibrationVectors", 
        Boolean.valueOf(modelSetHasVibrationVectors()));
    Properties props = viewer.getModelSetProperties();
    if(props != null)
      info.put("modelSetProperties",props);
    Vector models = new Vector();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable model = new Hashtable();
      model.put("_ipt",new Integer(i));
      model.put("num",new Integer(viewer.getModelNumber(i)));
      //model.put("name",viewer.getModelName(i));
      model.put("name", getModelName(i));
      String s = getModelTitle(i);
      if (s != null)
        model.put("title", s);
      model.put("file", getModelFile(i));
      model.put("vibrationVectors", Boolean.valueOf(modelHasVibrationVectors(i)));
      model.put("atomCount",new Integer(getAtomCountInModel(i)));
      model.put("bondCount",new Integer(getBondCountInModel(i)));
      model.put("groupCount",new Integer(getGroupCountInModel(i)));
      model.put("polymerCount",new Integer(getBioPolymerCountInModel(i)));
      model.put("chainCount",new Integer(getChainCountInModel(i)));      
      props = viewer.getModelProperties(i);
      if (props != null)
        model.put("modelProperties", props);
      models.addElement(model);
    }
    info.put("models",models);
    return info;
  }

  public Hashtable getAuxiliaryInfo() {
    Hashtable info = getModelSetAuxiliaryInfo();
    if (info == null)
      return info;
    Vector models = new Vector();
    int modelCount = viewer.getModelCount();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable modelinfo = getModelAuxiliaryInfo(i);
      models.addElement(modelinfo);
    }
    info.put("models",models);
    return info;
  }

  public Vector getAllAtomInfo(BitSet bs) {
    Vector V = new Vector();
    int atomCount = viewer.getAtomCount();
    for (int i = 0; i < atomCount; i++) 
      if (bs.get(i))
        V.addElement(getAtomInfoLong(i));
    return V;
  }

  public Vector getMoleculeInfo(BitSet bsAtoms) {
    return frame.getMoleculeInfo(bsAtoms);
  }
  
  public void getAtomIdentityInfo(int i, Hashtable info) {
    info.put("_ipt", new Integer(i));
    info.put("atomno", new Integer(getAtomNumber(i)));
    info.put("info", getAtomInfo(i));
    info.put("sym", getElementSymbol(i));
  }
  
  Hashtable getAtomInfoLong(int i) {
    Atom atom = frame.getAtomAt(i);
    Hashtable info = new Hashtable();
    getAtomIdentityInfo(i, info);
    info.put("element", getElementName(i));
    info.put("elemno", new Integer(getElementNumber(i)));
    info.put("x", new Float(getAtomX(i)));
    info.put("y", new Float(getAtomY(i)));
    info.put("z", new Float(getAtomZ(i)));
    if (frame.vibrationVectors != null && frame.vibrationVectors[i] != null) {
      info.put("vibVector", new Vector3f(frame.vibrationVectors[i]));
    }
    info.put("bondCount", new Integer(atom.getCovalentBondCount()));
    info.put("radius", new Float((atom.getRasMolRadius() / 120.0)));
    info.put("model", new Integer(atom.getModelNumberDotted()));
    info.put("visible", Boolean.valueOf(getAtomVisibility(i)));
    info.put("clickabilityFlags", new Integer(atom.clickabilityFlags));
    info.put("visibilityFlags", new Integer(atom.shapeVisibilityFlags));
    info.put("spacefill", new Float(atom.getRadius()));
    String strColor = viewer.getHexColorFromIndex(atom.colixAtom);
    if (strColor != null)
      info.put("color", strColor);
    info.put("colix", new Integer(atom.colixAtom));
    boolean isTranslucent = atom.isTranslucent();
    if (isTranslucent)
      info.put("translucent", Boolean.valueOf(isTranslucent));
    info.put("formalCharge", new Integer(atom.getFormalCharge()));
    info.put("partialCharge", new Float(atom.getPartialCharge()));
    float d = atom.getSurfaceDistance100() / 100f;
    if (d >= 0)
      info.put("surfaceDistance", new Float(d));
    if (isPDB(atom.modelIndex)) {
      info.put("resname", atom.getGroup3());
      int seqNum = atom.getSeqNumber();
      char insCode = atom.getInsertionCode();
      if (seqNum > 0)
        info.put("resno", new Integer(seqNum));
      if (insCode != 0)
        info.put("insertionCode", "" + insCode);
      char chainID = atom.getChainID();
      info.put("name", getAtomName(i));
      info.put("chain", (chainID == '\0' ? "" : "" + chainID));
      info.put("atomID", new Integer(atom.getSpecialAtomID()));
      info.put("groupID", new Integer(atom.getGroupID()));
      if (atom.alternateLocationID != '\0')
        info.put("altLocation", "" + atom.alternateLocationID);
      info.put("structure", new Integer(atom.getProteinStructureType()));
      info.put("polymerLength", new Integer(atom.getPolymerLength()));
      info.put("occupancy", new Integer(atom.getOccupancy()));
      int temp = atom.getBfactor100();
      info.put("temp", new Integer((temp < 0 ? 0 : temp / 100)));
    }
    return info;
  }  

  public Vector getAllBondInfo(BitSet bs) {
    Vector V = new Vector();
    int bondCount = getBondCount();
    for (int i = 0; i < bondCount; i++)
      if (bs.get(frame.getBondAt(i).getAtom1().atomIndex) 
          && bs.get(frame.getBondAt(i).getAtom2().atomIndex)) 
        V.addElement(getBondInfo(i));
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
    info.put("visible", Boolean.valueOf(bond.shapeVisibilityFlags != 0));
    String strColor = viewer.getHexColorFromIndex(bond.colix);
    if (strColor != null) 
      info.put("color", strColor);
    info.put("colix", new Integer(bond.colix));
    boolean isTranslucent = bond.isTranslucent();
    if (isTranslucent)
      info.put("translucent", Boolean.valueOf(isTranslucent));
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

  public Hashtable getAllChainInfo(BitSet bs) {
    Hashtable finalInfo = new Hashtable();
    Vector modelVector = new Vector();
    int modelCount = getModelCount();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable modelInfo = new Hashtable();
      Vector info = getChainInfo(i, bs);
      if (info.size() > 0) {
        modelInfo.put("modelIndex",new Integer(i));
        modelInfo.put("chains",info);
        modelVector.addElement(modelInfo);
      }
    }
    finalInfo.put("models",modelVector);
    return finalInfo;
  }

  public void getPolymerPointsAndVectors(BitSet bs, Vector vList) {
    int modelCount = viewer.getModelCount();
    boolean isTraceAlpha = viewer.getTraceAlpha();
    float sheetSmoothing = viewer.getSheetSmoothing();
    int last = Integer.MAX_VALUE - 1;
    for (int i = 0; i < modelCount; ++i) {
      int polymerCount = getBioPolymerCountInModel(i);
      for (int ip = 0; ip < polymerCount; ip++)
        last = frame.mmset.getModel(i).getBioPolymer(ip)
            .getPolymerPointsAndVectors(last, bs, vList, isTraceAlpha, sheetSmoothing);
    }
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
        infoChain.addElement(infoGroup);
      }
      if (! infoChain.isEmpty()) { 
        arrayName.put("residues",infoChain);
        infoChains.addElement(arrayName);
      }
    }
    return infoChains;
  }  
  
  public Hashtable getAllPolymerInfo(BitSet bs) {
    Hashtable finalInfo = new Hashtable();
    Vector modelVector = new Vector();
    int modelCount = getModelCount();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable modelInfo = new Hashtable();
      Vector info = new Vector();
      int polymerCount = getBioPolymerCountInModel(i);
      for (int ip = 0; ip < polymerCount; ip++) {
        Hashtable polyInfo = getPolymerInfo(i, ip, bs); 
        if (! polyInfo.isEmpty())
          info.addElement(polyInfo);
      }
      if (info.size() > 0) {
        modelInfo.put("modelIndex",new Integer(i));
        modelInfo.put("polymers",info);
        modelVector.addElement(modelInfo);
      }
    }
    finalInfo.put("models",modelVector);
    return finalInfo;
  }

  
  public Point3f[] getPolymerLeadMidPoints(int iModel, int iPolymer) {
    return frame.mmset.getModel(iModel).getBioPolymer(iPolymer).getLeadMidpoints();
  }


  String getPolymerSequence(int iModel, int iPolymer) {
    return frame.mmset.getModel(iModel).getBioPolymer(iPolymer).getSequence();
  }

  void getPolymerSequenceAtoms(int iModel, int iPolymer, int group1,
                               int nGroups, BitSet bsInclude, BitSet bsResult) {
    frame.mmset.getModel(iModel).getBioPolymer(iPolymer).getPolymerSequenceAtoms(
        iModel, iPolymer, group1, nGroups, bsInclude, bsResult);
  }

  Hashtable getPolymerInfo(int iModel, int iPolymer, BitSet bs) {
    return frame.mmset.getModel(iModel).getBioPolymer(iPolymer)
        .getPolymerInfo(bs);
  }
  
  public void addStateScript(String script) {
    frame.addStateScript(script);  
  }
 
  public String getState() {
    return frame.getState();
  }
  
  public Hashtable getBoundBoxInfo() {
    Hashtable info = new Hashtable();
    info.put("center", getBoundBoxCenter());
    info.put("edge", getBoundBoxCornerVector());
    return info;
  }
  
  public void setModelVisibility() {

    if (frame == null) //necessary for file chooser
      return;

    //named objects must be set individually
    //in the future, we might include here a BITSET of models rather than just a modelIndex

    // all these isTranslucent = f() || isTranslucent are that way because
    // in general f() does MORE than just check translucency. 
    // so isTranslucent = isTranslucent || f() would NOT work.
    
    BitSet bs = viewer.getVisibleFramesBitSet();
    //NOT balls (yet)
    for (int i = 1; i < JmolConstants.SHAPE_MAX; i++)
      if (frame.shapes[i] != null)
        frame.shapes[i].setVisibilityFlags(bs);
    //s(bs);
    //
    // BALLS sets the JmolConstants.ATOM_IN_MODEL flag.
    frame.shapes[JmolConstants.SHAPE_BALLS]
        .setVisibilityFlags(bs);

    //set clickability -- this enables measures and such
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = frame.shapes[i];
      if (shape != null)
        shape.setModelClickability();
    }
  }
 
  public boolean checkObjectClicked(int x, int y, int modifiers) {
    Shape shape = frame.shapes[JmolConstants.SHAPE_ECHO];
    if (shape != null && shape.checkObjectClicked(x, y, modifiers))
      return true;
    return ((shape = frame.shapes[JmolConstants.SHAPE_DRAW]) != null
        && shape.checkObjectClicked(x, y, modifiers));
  }
 
  public boolean checkObjectHovered(int x, int y) {
    if (frame == null)
      return false;
    Shape shape = frame.shapes[JmolConstants.SHAPE_ECHO];
    if (shape != null && shape.checkObjectHovered(x, y))
      return true;
    shape = frame.shapes[JmolConstants.SHAPE_DRAW];
    if (shape == null || !viewer.getDrawHover())
      return false;
    return shape.checkObjectHovered(x, y);
  }
 
  public void checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
                          int modifiers) {
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = frame.shapes[i];
      if (shape != null
          && shape.checkObjectDragged(prevX, prevY, deltaX, deltaY, modifiers))
        break;
    }
  }

  public Hashtable getShapeInfo() {
    Hashtable info = new Hashtable();
    StringBuffer commands = new StringBuffer();
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = frame.shapes[i];
      if (shape != null) {
        String shapeType = JmolConstants.shapeClassBases[i];
        if ("Draw,Dipoles,Isosurface,LcaoOrbital,MolecularOrbital".indexOf(shapeType) >= 0) {
          Hashtable shapeinfo = new Hashtable();
          shapeinfo.put("obj", shape.getShapeDetail());
          info.put(shapeType, shapeinfo);
        }
      }
    }
    if (commands.length() > 0)
      info.put("shapeCommands", commands.toString());
    return info;
  }
  
  Point3f getAveragePosition(int atomIndex1, int atomIndex2) {
    return frame.getAveragePosition(atomIndex1, atomIndex2);
  }

  Vector3f getAtomVector(int atomIndex1, int atomIndex2) {
    return frame.getAtomVector(atomIndex1, atomIndex2);
  }

  public Vector3f getModelDipole() {
    return frame.getModelDipole();
  }

  public void getBondDipoles() {
    frame.getBondDipoles();
  }

  public boolean useXtalDefaults() {
    return (frame.someModelsHaveSymmetry);
  }

  public BitSet setConformation(int modelIndex, BitSet bsConformation) {
    frame.setConformation(modelIndex, bsConformation);
    return bsConformation;
  }
  
  public BitSet setConformation(int modelIndex, int conformationIndex) {
    return frame.setConformation(modelIndex, conformationIndex);
  }

  public String getAltLocListInModel(int modelIndex) {
    if (modelIndex < 0)
      return "";
    return frame.getAltLocListInModel(modelIndex);
  }
  
  public int autoHbond(BitSet bsFrom, BitSet bsTo, BitSet bsBonds) {
    return frame.autoHbond(bsFrom, bsTo, bsBonds);
  }

  public boolean hbondsAreVisible(int modelIndex) {
    int bondCount = getBondCount();
    Bond[] bonds = frame.bonds;
    for (int i = bondCount; --i >= 0;)
      if (modelIndex < 0 || modelIndex == bonds[i].atom1.modelIndex)
        if (bonds[i].isHydrogen() && bonds[i].mad > 0)
          return true;
    return false;
  }

  public void toCartesian(int modelIndex, Point3f pt) {
    frame.toCartesian(modelIndex, pt);
    return;
  }

  public void toUnitCell(int modelIndex, Point3f pt, Point3f offset) {
    frame.toUnitCell(modelIndex, pt, offset);
    return;
  }
  
  public void clearBfactorRange(){
    frame.clearBfactorRange();
  }
  
  public void setZeroBased() {
    frame.setZeroBased();
  }

  public void setTrajectory(int iTraj) {
    frame.setTrajectory(iTraj);
  }

  public int getTrajectoryCount() {
    return frame.getTrajectoryCount();
  }
  
  public void setAtomCoord(int atomIndex, float x, float y, float z) {
    frame.setAtomCoord(atomIndex,x,y,z); 
  }

  public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    frame.setAtomCoordRelative(atomIndex,x,y,z);
  }

  public void setAtomCoordRelative(Point3f offset, BitSet bs) {
    frame.setAtomCoordRelative(bs, offset.x, offset.y, offset.z);
  }

  public void invertSelected(Point3f pt, Point4f plane, BitSet bs) {
    frame.invertSelected(pt, plane, bs);
  }
  
  public void rotateSelected(Matrix3f mNew, Matrix3f matrixRotate, BitSet bs, boolean fullMolecule) {
    frame.rotateSelected(mNew, matrixRotate, bs, fullMolecule);
  }

  public boolean getPrincipalAxes(int atomIndex, Vector3f z, Vector3f x,
                           String lcaoType, boolean hybridizationCompatible) {
    return frame.getPrincipalAxes(atomIndex, z, x, lcaoType,
        hybridizationCompatible);
  }
  
  public String getUnitCellInfoText() {
    int modelIndex = viewer.getCurrentModelIndex();
    if (modelIndex < 0)
      return "no single current model";
    if (frame.cellInfos == null)
      return "not applicable";
    return frame.cellInfos[modelIndex].getUnitCellInfo();
  }

  public String getSpaceGroupInfoText(String spaceGroup) {
    SpaceGroup sg;
    String strOperations = "";
    int modelIndex = viewer.getCurrentModelIndex();
    if (spaceGroup == null) {
      if (modelIndex < 0)
        return "no single current model";
      if (frame.cellInfos == null)
        return "not applicable";
      CellInfo cellInfo = frame.cellInfos[modelIndex];
      spaceGroup = cellInfo.spaceGroup;
      if (spaceGroup.indexOf("[") >= 0)
        spaceGroup = spaceGroup.substring(0, spaceGroup.indexOf("[")).trim();
      if (spaceGroup == "spacegroup unspecified")
        return "no space group identified in file";
      sg = SpaceGroup.determineSpaceGroup(spaceGroup, cellInfo
          .getNotionalUnitCell());
      strOperations = "\nSymmetry operations employed:"
          + frame.getModelSymmetryList(modelIndex);
    } else if (spaceGroup.equalsIgnoreCase("ALL")) {
      return SpaceGroup.dumpAll();
    } else {
      sg = SpaceGroup.determineSpaceGroup(spaceGroup);
      if (sg == null)
        sg = SpaceGroup.createSpaceGroup(spaceGroup, false);
    }
    if (sg == null)
      return "could not identify space group from name: " + spaceGroup;
    return sg.dumpInfo() + strOperations;
  }

  public int getSpaceGroupIndexFromName(String spaceGroup) {
    return SpaceGroup.determineSpaceGroupIndex(spaceGroup);
  }

  public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    frame.setSelectionHaloEnabled(selectionHaloEnabled);
  }

  public boolean getSelectionHaloEnabled() {
    return frame.getSelectionHaloEnabled();
  }

  public void calculateStructures() {
    frame.calculateStructures(true);
  }
  
  public boolean getEchoStateActive() {
    return frame.getEchoStateActive();
  }
  public void setEchoStateActive(boolean TF) {
    frame.setEchoStateActive(TF);
  }

  public boolean havePartialCharges() {
    return frame.partialCharges != null;
  }

  public void setFormalCharges(BitSet bs, int formalCharge) {    
    frame.setFormalCharges(bs, formalCharge);
  }

  public UnitCell getUnitCell(int modelIndex) {
    if (modelIndex < 0)
      return null;
    return (frame.cellInfos == null ? null : frame.cellInfos[modelIndex].unitCell);
  }

  public Point3f getUnitCellOffset(int modelIndex) {
    // from "unitcell {i j k}" via uccage
    UnitCell unitCell = getUnitCell(modelIndex);
    if (unitCell == null)
      return null;
    return unitCell.getCartesianOffset();
  }

  public boolean setUnitCellOffset(int modelIndex, Point3f pt) {
    // from "unitcell {i j k}" via uccage
    UnitCell unitCell = getUnitCell(modelIndex);
    if (unitCell == null)
      return false;
    unitCell.setOffset(pt);
    return true;
  }

  public boolean setUnitCellOffset(int modelIndex, int nnn) {
    UnitCell unitCell = getUnitCell(modelIndex);
    if (unitCell == null)
      return false;
    unitCell.setOffset(nnn);
    return true;
  }
  
  public BitSet getTaintedAtoms() {
    return frame.tainted;
  }
  
  public void setTaintedAtoms(BitSet bs) {
    frame.setTaintedAtoms(bs);
  }
  
  public Point3f[] calculateSurface(BitSet bsSelected, BitSet bsIgnore,
                             float envelopeRadius) {
    return frame.calculateSurface(bsSelected, bsIgnore, envelopeRadius);
  }
  
  public AtomIterator getWithinModelIterator(Atom atom, float distance) {
    return frame.getWithinModelIterator(atom, distance);
  }
  
  public AtomIndexIterator getWithinAtomSetIterator(int atomIndex, float distance, BitSet bsSelected, boolean isGreaterOnly) {
    return frame.getWithinAtomSetIterator(atomIndex, distance, bsSelected, isGreaterOnly);
  }
  
  public void fillAtomData(AtomData atomData, int mode) {
    frame.fillAtomData(atomData, mode);
  }

  public float[] getPartialCharges() {
    return frame.getPartialCharges();
  }
  
}
