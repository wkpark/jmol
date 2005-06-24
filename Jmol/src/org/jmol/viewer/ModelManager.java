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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.viewer;

import org.jmol.api.JmolAdapter;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Properties;
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

  int getModelCount() {
    return (frame == null) ? 0 : frame.getModelCount();
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

  Point3f getBoundingBoxCenter() {
    return (frame == null) ? null : frame.getBoundingBoxCenter();
  }

  Vector3f getBoundingBoxCornerVector() {
    return (frame == null) ? null : frame.getBoundingBoxCornerVector();
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

  void setRotationCenter(boolean relative, float x, float y, float z) {
    if (frame == null)
      return;
    pointT.set(x, y, z);
    if (relative)
      pointT.add(frame.getRotationCenterDefault());
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
}
