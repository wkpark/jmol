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
import org.openscience.jmol.viewer.datamodel.Frame;
import org.openscience.jmol.viewer.datamodel.FrameBuilder;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.pdb.PdbAtom;

import java.util.BitSet;
import java.util.Hashtable;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.awt.Rectangle;
import java.awt.Color;

public class ModelManager {

  JmolViewer viewer;
  JmolModelAdapter modelAdapter;
  final Frame nullFrame;

  public ModelManager(JmolViewer viewer, JmolModelAdapter modelAdapter) {
    this.viewer = viewer;
    this.modelAdapter = modelAdapter;
    this.nullFrame = new Frame(viewer);
  }

  public String fullPathName;
  public String fileName;
  public String modelName;
  public String modelHeader;
  public int frameCount = 0;
  public int atomCount = 0;
  public boolean haveFile = false;
  public int currentFrameNumber;
  public Frame frame;
  public Frame[] frames;

  public void setClientFile(String fullPathName, String fileName,
                            Object clientFile) {
    if (clientFile == null) {
      fullPathName = fileName = modelName = modelHeader = null;
      frameCount = 0;
      currentFrameNumber = -1;
      frame = null;
      atomCount = 0;
      haveFile = false;
      frames = null;
    } else {
      this.fullPathName = fullPathName;
      this.fileName = fileName;
      modelName = modelAdapter.getModelName(clientFile);
      if (modelName != null) {
        modelName = modelName.trim();
        if (modelName.length() == 0)
          modelName = null;
      }
      modelName = modelAdapter.getModelName(clientFile);
      modelHeader = modelAdapter.getModelHeader(clientFile);
      frameCount = modelAdapter.getFrameCount(clientFile);
      frames = new Frame[frameCount];
      for (int i = 0; i < frameCount; ++i) {
        // FIXME mth 2004 02 23 - allocate one FrameBuilder and reuse it
        frames[i] =
          new FrameBuilder(viewer, modelAdapter, clientFile, i).buildFrame();
      }

      haveFile = true;
    }
    viewer.notifyFileLoaded(fullPathName, fileName, modelName, clientFile);
  }

  public Frame getFrame() {
    return (frame == null) ? nullFrame : frame;
  }

  public JmolModelAdapter getExportModelAdapter() {
    return (frame == null) ? null : frame.getExportModelAdapter();
  }

  public String getModelName() {
    return modelName;
  }

  public String getModelHeader() {
    return modelHeader;
  }

  public float getRotationRadius() {
    return frame.getRotationRadius();
  }

  public void increaseRotationRadius(float increaseInAngstroms) {
    frame.increaseRotationRadius(increaseInAngstroms);
  }

  public Point3f getRotationCenter() {
    return frame.getRotationCenter();
  }

  public Point3f getBoundingBoxCenter() {
    return frame.getBoundingBoxCenter();
  }

  public Vector3f getBoundingBoxCornerVector() {
    return frame.getBoundingBoxCornerVector();
  }

  public int getFrameCount() {
    return frameCount;
  }

  public int getCurrentFrameNumber() {
    return currentFrameNumber;
  }

  public void setFrame(int frameNumber) {
    if (haveFile && frameNumber >= 0 && frameNumber < frameCount) {
      currentFrameNumber = frameNumber;
      frame = frames[frameNumber];
      atomCount = frame.getAtomCount();
    }
  }

  public int getAtomCount() {
    return atomCount;
  }

  public int getBondCount() {
    return frame.getBondCount();
  }

  public Point3f getPoint3f(int atomIndex) {
    return frame.getAtomAt(atomIndex).getPoint3f();
  }

  public void setCenterAsSelected() {
    int atomCount = getAtomCount();
    int countSelected = 0;
    Point3f  center = new Point3f(); // defaults to 0,00,
    BitSet bsSelection = viewer.getSelectionSet();
    for (int i = 0; i < atomCount; ++i) {
      if (!bsSelection.get(i))
        continue;
      ++countSelected;
      center.add(getPoint3f(i));
    }
    if (countSelected > 0) {
      center.scale(1.0f / countSelected); // just divide by the quantity
    } else {
      center = null;
    }
    frame.setRotationCenter(center);
  }

  public void setRotationCenter(Point3f center) {
    frame.setRotationCenter(center);
  }

  public boolean autoBond = true;

  public void rebond() {
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

  public int findNearestAtomIndex(int x, int y) {
    return frame.findNearestAtomIndex(x, y);
  }

  public BitSet findAtomsInRectangle(Rectangle rectRubber) {
    return frame.findAtomsInRectangle(rectRubber);
  }

  // FIXME mth 2004 02 23 -- this does *not* belong here
  public float solventProbeRadius = 0;
  public void setSolventProbeRadius(float radius) {
    this.solventProbeRadius = radius;
  }

  ////////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  ////////////////////////////////////////////////////////////////

  public String getAtomInfo(int i) {
    Atom atom = frame.atoms[i];
    PdbAtom pdbAtom = atom.pdbAtom;
    if (pdbAtom == null)
      return "Atom: " + atom.getAtomicSymbol() + " " + atom.getAtomno();
    return "Atom: " + pdbAtom.getAtomName() + " " + pdbAtom.getAtomSerial() +
      " " + pdbAtom.getGroup3() + " " + pdbAtom.getSeqcodeString() +
      " Chain:" + pdbAtom.getChainID();
  }

  public String getAtomicSymbol(int i) {
    return frame.atoms[i].getAtomicSymbol();
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
