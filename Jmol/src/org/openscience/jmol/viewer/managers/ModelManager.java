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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

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
  public String clientFileName;
  public int frameCount = 0;
  public int atomCount = 0;
  public boolean haveFile = false;
  public int currentFrameNumber;
  public JmolFrame jmolFrame;
  public JmolFrame[] jmolFrames;

  public PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void setClientFile(String name, Object clientFile) {
    System.out.println("setClientFile:" + name);
    Object clientFilePrevious = this.clientFile;
    this.clientFile = clientFile;
    if (clientFile == null) {
      clientFileName = "<null>";
      frameCount = 0;
      currentFrameNumber = -1;
      jmolFrame = null;
      atomCount = 0;
      haveFile = false;
      jmolFrames = null;
    } else {
      this.clientFileName = name;
      frameCount = viewer.getFrameCount(clientFile);
      jmolFrames = new JmolFrame[frameCount];
      haveFile = true;
      setFrame(0);
    }
    pcs.firePropertyChange(JmolViewer.PROP_CHEM_FILE,
                           clientFilePrevious, clientFile);
  }

  public Object getClientFile() {
    return clientFile;
  }

  public JmolFrame getJmolFrame() {
    return (jmolFrame == null) ? nullJmolFrame : jmolFrame;
  }

  public String getModelName() {
    String name = viewer.getModelName(clientFile);
    if (name == null) {
      name = clientFileName;
      if (name == null)
        name = "Jmol";
    }
    return name;
  }

  public double getRotationRadius() {
    return jmolFrame.getRotationRadius();
  }

  public Point3d getRotationCenter() {
    return jmolFrame.getRotationCenter();
  }

  public Point3d getBoundingBoxCenter() {
    return jmolFrame.getBoundingBoxCenter();
  }

  public Point3d getBoundingBoxCorner() {
    return jmolFrame.getBoundingBoxCorner();
  }
  
  public int getFrameCount() {
    return frameCount;
  }

  public int getCurrentFrameNumber() {
    return currentFrameNumber;
  }

  public void setFrame(int frameNumber) {
    if (haveFile && frameNumber >= 0 && frameNumber < frameCount) {
      jmolFrame = jmolFrames[frameNumber];
      if (jmolFrame == null)
        jmolFrame = jmolFrames[frameNumber] =
          new JmolFrameBuilder(viewer, clientFile, frameNumber)
          .buildJmolFrame();
      atomCount = jmolFrame.getAtomCount();
      hasPdbRecords = jmolModelAdapter.hasPdbRecords(clientFile, frameNumber);
    }
  }

  public int getAtomCount() {
    return atomCount;
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
      center.add(jmolFrame.getAtomAt(i).getPoint3d());
    }
    if (countSelected > 0) {
      center.scale(1.0f / countSelected); // just divide by the quantity
    } else {
      center = null;
    }
    jmolFrame.setRotationCenter(center);
  }

  public void setRotationCenter(Point3d center) {
    jmolFrame.setRotationCenter(center);
  }

  public boolean autoBond = true;

  public void rebond() {
    jmolFrame.rebond();
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
    //    jmolFrame.deleteAtom(atomIndex);
  }

  public int findNearestAtomIndex(int x, int y) {
    return jmolFrame.findNearestAtomIndex(x, y);
  }

  public BitSet findAtomsInRectangle(Rectangle rectRubber) {
    return jmolFrame.findAtomsInRectangle(rectRubber);
  }

  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    pcs.addPropertyChangeListener(pcl);
  }

  public void addPropertyChangeListener(String prop,
                                        PropertyChangeListener pcl) {
    pcs.addPropertyChangeListener(prop, pcl);
  }

  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    pcs.removePropertyChangeListener(pcl);
  }

  public void removePropertyChangeListener(String prop,
                                           PropertyChangeListener pcl) {
    pcs.removePropertyChangeListener(prop, pcl);
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

}
