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
package org.openscience.jmol;

import org.openscience.jmol.render.JmolFrame;
import org.openscience.jmol.render.JmolFrameBuilder;

import java.util.BitSet;
import javax.vecmath.Point3d;
import java.awt.Rectangle;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ModelManager {

  DisplayControl control;
  final JmolFrame nullJmolFrame;


  public ModelManager(DisplayControl control) {
    this.control = control;
    this.nullJmolFrame = new JmolFrame(control);
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
      frameCount = control.getFrameCount(clientFile);
      jmolFrames = new JmolFrame[frameCount];
      haveFile = true;
      setFrame(0);
    }
    pcs.firePropertyChange(DisplayControl.PROP_CHEM_FILE,
                           clientFilePrevious, clientFile);
  }

  public Object getClientFile() {
    return clientFile;
  }

  public JmolFrame getJmolFrame() {
    return (jmolFrame == null) ? nullJmolFrame : jmolFrame;
  }

  public String getModelName() {
    String name = control.getModelName(clientFile);
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
          (true
           ? control.getJmolFrame(clientFile, frameNumber)
           : (new JmolFrameBuilder(control, clientFile, frameNumber)
              .buildJmolFrame()));
      atomCount = jmolFrame.getAtomCount();
    }
  }

  public int getAtomCount() {
    return atomCount;
  }

  public void setCenterAsSelected() {
    int atomCount = getAtomCount();
    int countSelected = 0;
    Point3d  center = new Point3d(); // defaults to 0,00,
    BitSet bsSelection = control.getSelectionSet();
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

  // FIXME NEEDSWORK -- bond binding stuff
  public double bondFudge = 1.12f;
  public boolean autoBond = true;

  public void rebond() {
    jmolFrame.rebond();
  }

  public void setBondFudge(double bf) {
    bondFudge = bf;
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
}
