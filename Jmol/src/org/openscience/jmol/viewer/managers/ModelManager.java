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
import java.awt.Rectangle;
import java.awt.Color;

public class ModelManager {

  JmolViewer viewer;
  JmolModelAdapter modelAdapter;
  boolean suppliesAtomicNumber;
  boolean suppliesAtomicSymbol;
  boolean suppliesAtomTypeName;
  boolean suppliesVanderwaalsRadius;
  boolean suppliesCovalentRadius;
  final Frame nullFrame;

  public ModelManager(JmolViewer viewer, JmolModelAdapter modelAdapter) {
    this.viewer = viewer;
    this.modelAdapter = modelAdapter;
    this.nullFrame = new Frame(viewer);

    suppliesAtomicNumber = modelAdapter.suppliesAtomicNumber();
    suppliesAtomicSymbol = modelAdapter.suppliesAtomicSymbol();
    suppliesAtomTypeName = modelAdapter.suppliesAtomTypeName();
    suppliesVanderwaalsRadius = modelAdapter.suppliesVanderwaalsRadius();
    suppliesCovalentRadius = modelAdapter.suppliesCovalentRadius();

    if (JmolConstants.vanderwaalsMars.length != JmolConstants.atomicNumberMax)
      System.out.println("WARNING! vanderwaalsRadii.length not consistent");
    if (JmolConstants.covalentMars.length != JmolConstants.atomicNumberMax)
      System.out.println("WARNING! covalentRadii.length not consistent");
  }

  public Object clientFile;
  public String fullPathName;
  public String fileName;
  public String modelName;
  public int frameCount = 0;
  public int atomCount = 0;
  public boolean haveFile = false;
  public int currentFrameNumber;
  public Frame frame;
  public Frame[] frames;

  public void setClientFile(String fullPathName, String fileName,
                            Object clientFile) {
    Object clientFilePrevious = this.clientFile;
    this.clientFile = clientFile;
    if (clientFile == null) {
      fullPathName = fileName = modelName = null;
      frameCount = 0;
      currentFrameNumber = -1;
      frame = null;
      atomCount = 0;
      haveFile = false;
      frames = null;
    } else {
      this.fullPathName = fullPathName;
      this.fileName = fileName;
      modelName = getModelName(clientFile);
      if (modelName != null) {
        modelName = modelName.trim();
        if (modelName.length() == 0)
          modelName = null;
      }
      frameCount = viewer.getFrameCount(clientFile);
      frames = new Frame[frameCount];
      haveFile = true;
    }
    viewer.notifyFileLoaded(fullPathName, fileName, modelName, clientFile);
  }

  public Object getClientFile() {
    return clientFile;
  }

  public Frame getFrame() {
    return (frame == null) ? nullFrame : frame;
  }

  public String getModelName() {
    return modelName;
  }

  public float getRotationRadius() {
    return frame.getRotationRadius();
  }

  public Point3f getRotationCenter() {
    return frame.getRotationCenter();
  }

  public Point3f getBoundingBoxCenter() {
    return frame.getBoundingBoxCenter();
  }

  public Point3f getBoundingBoxCorner() {
    return frame.getBoundingBoxCorner();
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
      if (frame == null)
        frame = frames[frameNumber] =
          new FrameBuilder(viewer, clientFile, frameNumber)
          .buildFrame();
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
    Object clientAtom = frame.deleteAtom(atomIndex);
    modelAdapter.notifyAtomDeleted(clientAtom);
  }

  public int findNearestAtomIndex(int x, int y) {
    return frame.findNearestAtomIndex(x, y);
  }

  public BitSet findAtomsInRectangle(Rectangle rectRubber) {
    return frame.findAtomsInRectangle(rectRubber);
  }

  /****************************************************************
   * JmolModelAdapter routines
   ****************************************************************/

  public JmolModelAdapter getJmolModelAdapter() {
    return modelAdapter;
  }

  public int getFrameCount(Object clientFile) {
    return modelAdapter.getFrameCount(clientFile);
  }

  public String getModelName(Object clientFile) {
    return modelAdapter.getModelName(clientFile);
  }

  public int getAtomicNumber(Object clientAtom) {
    if (suppliesAtomicNumber) {
      int atomicNumber = modelAdapter.getAtomicNumber(clientAtom);
      if (atomicNumber < -1 ||
          atomicNumber >= JmolConstants.atomicNumberMax) {
        System.out.println("JmolModelAdapter.getAtomicNumber() returned " +
                           atomicNumber);
        return 0;
      }
      if (atomicNumber >= 0)
        return atomicNumber;
    }
    return mapAtomicSymbolToAtomicNumber(clientAtom);
  }

  public String getAtomicSymbol(Atom atom) {
    if (suppliesAtomicSymbol) {
      String atomicSymbol = modelAdapter.getAtomicSymbol(atom.clientAtom);
      if (atomicSymbol != null)
        return atomicSymbol;
      System.out.println("JmolModelAdapter.getAtomicSymbol returned null");
    }
    return JmolConstants.atomicSymbols[atom.atomicNumber];
  }

  public String getAtomTypeName(Atom atom) {
    if (suppliesAtomTypeName) {
      String atomTypeName = modelAdapter.getAtomTypeName(atom.clientAtom);
      if (atomTypeName != null)
        return atomTypeName;
    }
    if (atom.pdbAtom != null) {
      return atom.pdbAtom.getAtomPrettyName();
    }
    return getAtomicSymbol(atom);
  }

  public short getVanderwaalsMar(Atom atom) {
    if (suppliesVanderwaalsRadius) {
      float vanderwaalsRadius =
        modelAdapter.getVanderwaalsRadius(atom.clientAtom);
      if (vanderwaalsRadius > 0)
        return (short)(vanderwaalsRadius * 1000);
      System.out.println("JmolClientAdapter.getVanderwaalsRadius() returned " +
                         vanderwaalsRadius);
    }
    return JmolConstants.vanderwaalsMars[atom.atomicNumber];
  }

  public short getCovalentMar(Atom atom) {
    if (suppliesCovalentRadius) {
      float covalentRadius = modelAdapter.getCovalentRadius(atom.clientAtom);
      if (covalentRadius > 0)
        return (short)(covalentRadius * 1000);
      System.out.println("JmolClientAdapter.getCovalentRadius() returned " +
                         covalentRadius);
    }
    return JmolConstants.covalentMars[atom.atomicNumber];
  }

  public String getPdbAtomRecord(Object clientAtom) {
    return modelAdapter.getPdbAtomRecord(clientAtom);
  }

  public int getPdbModelNumber(Object clientAtom) {
    return modelAdapter.getPdbModelNumber(clientAtom);
  }

  public float solventProbeRadius = 0;
  public void setSolventProbeRadius(float radius) {
    this.solventProbeRadius = radius;
  }

  /****************************************************************
   * default values if not supplied by client
   * note that atomicSymbols are stored in the JmolClientAdapter
   ****************************************************************/

  private static Hashtable htAtomicMap;
  private int mapAtomicSymbolToAtomicNumber(Object clientAtom) {
    if (htAtomicMap == null) {
      Hashtable map = new Hashtable();
      for (int atomicNumber = JmolConstants.atomicNumberMax;
           --atomicNumber >= 0; ) {
        String symbol = JmolConstants.atomicSymbols[atomicNumber];
        Integer boxed = new Integer(atomicNumber);
        map.put(symbol, boxed);
        if (symbol.length() == 2) {
          symbol =
            "" + symbol.charAt(0) + Character.toUpperCase(symbol.charAt(1));
          map.put(symbol, boxed);
        }
      }
      htAtomicMap = map;
    }
    String atomicSymbol = modelAdapter.getAtomicSymbol(clientAtom);
    if (atomicSymbol == null) {
      System.out.println("JmolModelAdapter.getAtomicSymbol() returned null");
      return 0;
    }
    Integer boxedAtomicNumber = (Integer)htAtomicMap.get(atomicSymbol);
    if (boxedAtomicNumber != null)
	return boxedAtomicNumber.intValue();
    System.out.println("" + atomicSymbol + "' is not a recognized symbol");
    return 0;
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
