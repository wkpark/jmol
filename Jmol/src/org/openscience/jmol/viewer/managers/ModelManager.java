/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol.viewer.managers;

import org.openscience.jmol.viewer.*;
import org.jmol.api.ModelAdapter;
import org.openscience.jmol.viewer.datamodel.Frame;
import org.openscience.jmol.viewer.datamodel.FrameBuilder;
import org.openscience.jmol.viewer.datamodel.Atom;

import java.util.BitSet;
import java.util.Hashtable;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.awt.Rectangle;
import java.awt.Color;

public class ModelManager {

  final JmolViewer viewer;
  final ModelAdapter modelAdapter;

  final Frame nullFrame;
  final FrameBuilder frameBuilder;

  public ModelManager(JmolViewer viewer, ModelAdapter modelAdapter) {
    this.viewer = viewer;
    this.modelAdapter = modelAdapter;
    nullFrame = new Frame(viewer, "null", 0);
    frameBuilder = new FrameBuilder(viewer, modelAdapter);
  }

  public String fullPathName;
  public String fileName;
  public String modelSetName;
  public String modelFileHeader;
  //  public int frameCount = 0;
  public boolean haveFile = false;
  //  public int currentFrameNumber;
  public Frame frame;
  //  public Frame[] frames;

  public void setClientFile(String fullPathName, String fileName,
                            Object clientFile) {
    if (clientFile == null) {
      fullPathName = fileName = modelSetName = modelFileHeader = null;
      frame = null;
      haveFile = false;
    } else {
      this.fullPathName = fullPathName;
      this.fileName = fileName;
      modelSetName = modelAdapter.getModelSetName(clientFile);
      if (modelSetName != null) {
        modelSetName = modelSetName.trim();
        if (modelSetName.length() == 0)
          modelSetName = null;
      }
      modelFileHeader = modelAdapter.getModelFileHeader(clientFile);
      frame = frameBuilder.buildFrame(clientFile);

      haveFile = true;
    }
  }

  public String getClientAtomStringProperty(Object clientAtom,
                                            String propertyName) {
    return modelAdapter.getClientAtomStringProperty(clientAtom, propertyName);
  }

  public Frame getFrame() {
    return (frame == null) ? nullFrame : frame;
  }

  public ModelAdapter getExportModelAdapter() {
    return (frame == null) ? null : frame.getExportModelAdapter();
  }

  public String getModelSetName() {
    return modelSetName;
  }

  public String getModelFileHeader() {
    return modelFileHeader;
  }

  public float getRotationRadius() {
    return (frame == null) ? 1 : frame.getRotationRadius();
  }

  public void increaseRotationRadius(float increaseInAngstroms) {
    if (frame != null)
      frame.increaseRotationRadius(increaseInAngstroms);
  }

  public Point3f getRotationCenter() {
    return (frame == null) ? null : frame.getRotationCenter();
  }

  public Point3f getBoundingBoxCenter() {
    return (frame == null) ? null : frame.getBoundingBoxCenter();
  }

  public Vector3f getBoundingBoxCornerVector() {
    return (frame == null) ? null : frame.getBoundingBoxCornerVector();
  }

  public int getModelCount() {
    return (frame == null) ? 0 : frame.getModelCount();
  }

  public int getChainCount() {
    return (frame == null) ? 0 : frame.getChainCount();
  }

  public int getGroupCount() {
    return (frame == null) ? 0 : frame.getGroupCount();
  }

  public int getAtomCount() {
    return (frame == null) ? 0 : frame.getAtomCount();
  }

  public int getBondCount() {
    return (frame == null) ? 0 : frame.getBondCount();
  }

  public void setCenterAsSelected() {
    if (frame == null)
      return;
    int countSelected = 0;
    Point3f  center = new Point3f(); // defaults to 0,00,
    BitSet bsSelection = viewer.getSelectionSet();
    for (int i = getAtomCount(); --i >= 0; ) {
      if (!bsSelection.get(i))
        continue;
      ++countSelected;
      center.add(frame.getAtomPoint3f(i));
    }
    if (countSelected > 0) {
      center.scale(1.0f / countSelected); // just divide by the quantity
    } else {
      center = null;
    }
    frame.setRotationCenter(center);
  }

  public void setRotationCenter(Point3f center) {
    if (frame != null)
      frame.setRotationCenter(center);
  }

  public boolean autoBond = true;

  public void rebond() {
    if (frame != null)
      frame.rebond();
  }

  public void setAutoBond(boolean ab) {
    autoBond = ab;
  }

  // angstroms of slop ... from OpenBabel ... mth 2003 05 26
  public float bondTolerance = 0.45f;
  public void setBondTolerance(float bondTolerance) {
    this.bondTolerance = bondTolerance;
  }

  // minimum acceptable bonding distance ... from OpenBabel ... mth 2003 05 26
  public float minBondDistance = 0.4f;
  public void setMinBondDistance(float minBondDistance) {
    this.minBondDistance = minBondDistance;
  }

  public void deleteAtom(int atomIndex) {
    frame.deleteAtom(atomIndex);
  }

  public boolean frankClicked(int x, int y) {
    return (getShapeSize(JmolConstants.SHAPE_FRANK) != 0 &&
            frame.frankClicked(x, y));
  }

  public int findNearestAtomIndex(int x, int y) {
    return (frame == null) ? -1 : frame.findNearestAtomIndex(x, y);
  }

  public BitSet findAtomsInRectangle(Rectangle rectRubber) {
    return frame.findAtomsInRectangle(rectRubber);
  }

  // FIXME mth 2004 02 23 -- this does *not* belong here
  public float solventProbeRadius = 0;
  public void setSolventProbeRadius(float radius) {
    this.solventProbeRadius = radius;
  }

  /****************************************************************
   * shape support
   ****************************************************************/

  int[] shapeSizes = new int[JmolConstants.SHAPE_MAX];
  Hashtable[] shapeProperties = new Hashtable[JmolConstants.SHAPE_MAX];

  public void setShapeSize(int shapeType, int size, BitSet bsSelected) {
    shapeSizes[shapeType] = size;
    if (frame != null)
      frame.setShapeSize(shapeType, size, bsSelected);
  }
  
  public int getShapeSize(int shapeType) {
    return shapeSizes[shapeType];
  }
  
  private static final Object NULL_SURROGATE = new Object();

  public void setShapeProperty(int shapeType, String propertyName,
                               Object value, BitSet bsSelected) {
    Hashtable props = shapeProperties[shapeType];
    if (props == null)
      props = shapeProperties[shapeType] = new Hashtable();

    // be sure to intern all propertyNames!
    propertyName = propertyName.intern();
    System.out.println("propertyName=" + propertyName + "\n" +
                       "value=" + value);

    // Hashtables cannot store null values :-(
    props.put(propertyName, value != null ? value : NULL_SURROGATE);
    if (frame != null)
      frame.setShapeProperty(shapeType, propertyName, value, bsSelected);
  }

  public Object getShapeProperty(int shapeType, String propertyName,
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

  public int getAtomIndexFromAtomNumber(int atomNumber) {
    return (frame == null) ? -1 : frame.getAtomIndexFromAtomNumber(atomNumber);
  }

  ////////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  ////////////////////////////////////////////////////////////////

  public String getAtomInfo(int i) {
    return frame.atoms[i].getInfo();
  }

/*
public String getAtomInfoChime(int i) {
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
    return frame.atoms[i].getElementSymbol();
  }

  public int getElementNumber(int i) {
    return frame.atoms[i].getElementNumber();
  }

  public String getAtomName(int i) {
    return frame.atoms[i].getAtomName();
  }

  public int getAtomNumber(int i) {
    return frame.atoms[i].getAtomNumber();
  }

  public float getAtomX(int i) {
    return frame.atoms[i].getAtomX();
  }

  public float getAtomY(int i) {
    return frame.atoms[i].getAtomY();
  }

  public float getAtomZ(int i) {
    return frame.atoms[i].getAtomZ();
  }

  public Point3f getAtomPoint3f(int i) {
    return frame.atoms[i].getPoint3f();
  }

  public float getAtomRadius(int i) {
    return frame.atoms[i].getRadius();
  }

  public short getAtomColix(int i) {
    return frame.atoms[i].getColix();
  }

  public Point3f getBondPoint3f1(int i) {
    return frame.bonds[i].atom1.getPoint3f();
  }

  public Point3f getBondPoint3f2(int i) {
    return frame.bonds[i].atom2.getPoint3f();
  }

  public float getBondRadius(int i) {
    return frame.bonds[i].getRadius();
  }

  public byte getBondOrder(int i) {
    return frame.bonds[i].getOrder();
  }

  public short getBondColix1(int i) {
    return frame.bonds[i].getColix1();
  }

  public short getBondColix2(int i) {
    return frame.bonds[i].getColix2();
  }
}
