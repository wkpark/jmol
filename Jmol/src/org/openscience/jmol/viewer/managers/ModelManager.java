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

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.JmolModelAdapter;
import org.openscience.jmol.viewer.datamodel.JmolFrame;
import org.openscience.jmol.viewer.datamodel.JmolFrameBuilder;

import java.util.BitSet;
import java.util.Hashtable;
import javax.vecmath.Point3d;
import java.awt.Rectangle;
import java.awt.Color;

public class ModelManager {

  JmolViewer viewer;
  JmolModelAdapter jmolModelAdapter;
  boolean suppliesAtomicNumber;
  boolean suppliesAtomicSymbol;
  boolean suppliesAtomTypeName;
  boolean suppliesVanderwaalsRadius;
  boolean suppliesCovalentRadius;
  boolean suppliesAtomColor;
  boolean hasPdbRecords;
  final JmolFrame nullJmolFrame;


  public ModelManager(JmolViewer viewer, JmolModelAdapter jmolModelAdapter) {
    this.viewer = viewer;
    this.jmolModelAdapter = jmolModelAdapter;
    this.nullJmolFrame = new JmolFrame(viewer);

    suppliesAtomicNumber = jmolModelAdapter.suppliesAtomicNumber();
    suppliesAtomicSymbol = jmolModelAdapter.suppliesAtomicSymbol();
    suppliesAtomTypeName = jmolModelAdapter.suppliesAtomTypeName();
    suppliesVanderwaalsRadius = jmolModelAdapter.suppliesVanderwaalsRadius();
    suppliesCovalentRadius = jmolModelAdapter.suppliesCovalentRadius();
    suppliesAtomColor = jmolModelAdapter.suppliesAtomColor();

    if (JmolModelAdapter.vanderwaalsRadii.length != JmolModelAdapter.atomicNumberMax)
      System.out.println("WARNING! vanderwaalsRadii.length not consistent");
    if (JmolModelAdapter.covalentRadii.length != JmolModelAdapter.atomicNumberMax)
      System.out.println("WARNING! covalentRadii.length not consistent");
    if (JmolModelAdapter.atomColors.length != JmolModelAdapter.atomicNumberMax)
      System.out.println("WARNING! atomColors.length not consistent");
  }

  public Object clientFile;
  public String fullPathName;
  public String fileName;
  public String modelName;
  public int frameCount = 0;
  public int atomCount = 0;
  public boolean haveFile = false;
  public int currentFrameNumber;
  public JmolFrame frame;
  public JmolFrame[] frames;

  public void setClientFile(String fullPathName, String fileName,
                            Object clientFile) {
    System.out.println("setClientFile:" + fileName);
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
      modelName = viewer.getModelName(clientFile);
      if (modelName != null) {
        modelName = modelName.trim();
        if (modelName.length() == 0)
          modelName = null;
      }
      frameCount = viewer.getFrameCount(clientFile);
      frames = new JmolFrame[frameCount];
      haveFile = true;
    }
    viewer.notifyFileLoaded(fullPathName, fileName, modelName, clientFile);
  }

  public Object getClientFile() {
    return clientFile;
  }

  public JmolFrame getJmolFrame() {
    return (frame == null) ? nullJmolFrame : frame;
  }

  public String getModelName() {
    return modelName;
  }

  public double getRotationRadius() {
    return frame.getRotationRadius();
  }

  public Point3d getRotationCenter() {
    return frame.getRotationCenter();
  }

  public Point3d getBoundingBoxCenter() {
    return frame.getBoundingBoxCenter();
  }

  public Point3d getBoundingBoxCorner() {
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
      frame = frames[frameNumber];
      if (frame == null)
        frame = frames[frameNumber] =
          new JmolFrameBuilder(viewer, clientFile, frameNumber)
          .buildJmolFrame();
      atomCount = frame.getAtomCount();
      hasPdbRecords = jmolModelAdapter.hasPdbRecords(clientFile, frameNumber);
    }
  }

  public int getAtomCount() {
    return atomCount;
  }

  public int getBondCount() {
    return frame.getBondCount();
  }

  public Point3d getPoint3d(int atomIndex) {
    return frame.getAtomAt(atomIndex).getPoint3d();
  }

  public void setCenterAsSelected() {
    int atomCount = getAtomCount();
    int countSelected = 0;
    Point3d  center = new Point3d(); // defaults to 0,00,
    BitSet bsSelection = viewer.getSelectionSet();
    for (int i = 0; i < atomCount; ++i) {
      if (!bsSelection.get(i))
        continue;
      ++countSelected;
      center.add(getPoint3d(i));
    }
    if (countSelected > 0) {
      center.scale(1.0f / countSelected); // just divide by the quantity
    } else {
      center = null;
    }
    frame.setRotationCenter(center);
  }

  public void setRotationCenter(Point3d center) {
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
  public double bondTolerance = 0.45;
  public void setBondTolerance(double bondTolerance) {
    this.bondTolerance = bondTolerance;
  }

  // minimum acceptable bonding distance ... from OpenBabel ... mth 2003 05 26
  public double minBondDistance = 0.4;
  public void setMinBondDistance(double minBondDistance) {
    this.minBondDistance =  minBondDistance;
  }

  public void deleteAtom(int atomIndex) {
    throw new NullPointerException();
    // not implemented
    //    frame.deleteAtom(atomIndex);
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
    return jmolModelAdapter;
  }

  public int getFrameCount(Object clientFile) {
    return jmolModelAdapter.getFrameCount(clientFile);
  }

  public String getModelName(Object clientFile) {
    return jmolModelAdapter.getModelName(clientFile);
  }

  public int getAtomicNumber(Object clientAtom) {
    if (suppliesAtomicNumber) {
      int atomicNumber = jmolModelAdapter.getAtomicNumber(clientAtom);
      if (atomicNumber < -1 || atomicNumber >= JmolModelAdapter.atomicNumberMax) {
        System.out.println("JmolModelAdapter.getAtomicNumber() returned " +
                           atomicNumber);
        return 0;
      }
      if (atomicNumber >= 0)
        return atomicNumber;
    }
    return mapAtomicSymbolToAtomicNumber(clientAtom);
  }

  public String getAtomicSymbol(int atomicNumber, Object clientAtom) {
    if (suppliesAtomicSymbol) {
      String atomicSymbol = jmolModelAdapter.getAtomicSymbol(clientAtom);
      if (atomicSymbol != null)
        return atomicSymbol;
      System.out.println("JmolModelAdapter.getAtomicSymbol returned null");
    }
    return JmolModelAdapter.atomicSymbols[atomicNumber];
  }

  public String getAtomTypeName(int atomicNumber, Object clientAtom) {
    if (suppliesAtomTypeName) {
      String atomTypeName = jmolModelAdapter.getAtomTypeName(clientAtom);
      if (atomTypeName != null)
        return atomTypeName;
    }
    return getAtomicSymbol(atomicNumber, clientAtom);
  }

  public double getVanderwaalsRadius(int atomicNumber, Object clientAtom) {
    if (suppliesVanderwaalsRadius) {
      double vanderwaalsRadius = jmolModelAdapter.getVanderwaalsRadius(clientAtom);
      if (vanderwaalsRadius > 0)
        return vanderwaalsRadius;
      System.out.println("JmolClientAdapter.getVanderwaalsRadius() returned " +
                         vanderwaalsRadius);
    }
    return JmolModelAdapter.vanderwaalsRadii[atomicNumber];
  }

  public double getCovalentRadius(int atomicNumber, Object clientAtom) {
    if (suppliesCovalentRadius) {
      double covalentRadius = jmolModelAdapter.getCovalentRadius(clientAtom);
      if (covalentRadius > 0)
        return covalentRadius;
      System.out.println("JmolClientAdapter.getCovalentRadius() returned " +
                         covalentRadius);
    }
    return JmolModelAdapter.covalentRadii[atomicNumber];
  }

  public Point3d getPoint3d(Object clientAtom) {
    return jmolModelAdapter.getPoint3d(clientAtom);
  }

  public String getPdbAtomRecord(Object clientAtom) {
    if (! hasPdbRecords)
      return null;
    return jmolModelAdapter.getPdbAtomRecord(clientAtom);
  }

  public Color getColorAtom(int atomicNumber, Object clientAtom, byte scheme) {
    if (suppliesAtomColor) {
      Color color = jmolModelAdapter.getAtomColor(clientAtom, scheme);
      if (color != null)
        return color;
      System.out.println("JmolModelAdapter.getColorAtom returned null");
    }
    return JmolModelAdapter.atomColors[atomicNumber];
  }

  /****************************************************************
   * default values if not supplied by client
   * note that atomicSymbols are stored in the JmolClientAdapter
   ****************************************************************/

  private static Hashtable htAtomicMap;
  private int mapAtomicSymbolToAtomicNumber(Object clientAtom) {
    if (htAtomicMap == null) {
      Hashtable map = new Hashtable();
      for (int atomicNumber = JmolModelAdapter.atomicNumberMax;
           --atomicNumber >= 0; )
        map.put(JmolModelAdapter.atomicSymbols[atomicNumber],
                new Integer(atomicNumber));
      htAtomicMap = map;
    }
    String atomicSymbol = jmolModelAdapter.getAtomicSymbol(clientAtom);
    if (atomicSymbol == null) {
      System.out.println("JmolModelAdapter does not supply getAtomicNumber() and " +
                         "JmolModelAdapter.getAtomicSymbol() returned null");
      return 0;
    }
    Integer boxedAtomicNumber = (Integer)htAtomicMap.get(atomicSymbol);
    if (boxedAtomicNumber == null) {
      System.out.println("JmolModelAdapter does not supply getAtomicNumber() and '" +
                         atomicSymbol + "' is not a recognized symbol");
      return 0;
    }
    return boxedAtomicNumber.intValue();
  }

  ////////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  ////////////////////////////////////////////////////////////////

  public String getAtomicSymbol(int i) {
    return frame.atomShapes[i].getAtomicSymbol();
  }

  public double getAtomX(int i) {
    return frame.atomShapes[i].getAtomX();
  }

  public double getAtomY(int i) {
    return frame.atomShapes[i].getAtomY();
  }

  public double getAtomZ(int i) {
    return frame.atomShapes[i].getAtomZ();
  }

  public Point3d getAtomPoint3d(int i) {
    return frame.atomShapes[i].getPoint3d();
  }

  public double getAtomRadius(int i) {
    return frame.atomShapes[i].getRadius();
  }

  public short getAtomColix(int i) {
    return frame.atomShapes[i].getColix();
  }

  public Point3d getBondPoint3d1(int i) {
    return frame.bondShapes[i].atomShape1.getPoint3d();
  }

  public Point3d getBondPoint3d2(int i) {
    return frame.bondShapes[i].atomShape2.getPoint3d();
  }

  public double getBondRadius(int i) {
    return frame.bondShapes[i].getRadius();
  }

  public short getBondColix1(int i) {
    return frame.bondShapes[i].getColix1();
  }

  public short getBondColix2(int i) {
    return frame.bondShapes[i].getColix2();
  }
}
