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

import java.util.BitSet;
import java.util.Vector;
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

  public boolean haveFile = false;
  public ChemFile chemfile;
  public ChemFrame chemframe;
  public int nframes = 0;
  public int currentFrameNumber;
  public PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void setChemFile(ChemFile chemfile) {
    ChemFile chemfilePrevious = this.chemfile;
    this.chemfile = chemfile;
    if (chemfile != null) {
      nframes = chemfile.getNumberOfFrames();
      this.chemframe = chemfile.getFrame(0);
      haveFile = true;
    } else {
      nframes = 0;
      this.chemframe = null;
      haveFile = false;
    }
    pcs.firePropertyChange(DisplayControl.PROP_CHEM_FILE,
                           chemfilePrevious, chemfile);
  }

  public boolean hasFrame() {
    return (chemframe != null);
  }

  public JmolFrame getJmolFrame() {
    return (chemframe == null) ? nullJmolFrame : chemframe.getJmolFrame();
  }

  public ChemFile getChemFile() {
    return chemfile;
  }

  public boolean haveFile() {
    return haveFile;
  }

  public String getModelName() {
    // FIXME mth -- model name goes here
    return "jmol";
  }

  public double getRotationRadius() {
    return chemframe.getJmolFrame().getRotationRadius();
  }

  public Point3d getRotationCenter() {
    return chemframe.getJmolFrame().getRotationCenter();
  }

  public Point3d getBoundingBoxCenter() {
    return haveFile ? chemframe.getJmolFrame().getBoundingBoxCenter() : null;
  }

  public Point3d getBoundingBoxCorner() {
    return haveFile ? chemframe.getJmolFrame().getBoundingBoxCorner() : null;
  }
  

  public int getNumberOfFrames() {
    return nframes;
  }

  public int getCurrentFrameNumber() {
    return currentFrameNumber;
  }

  public void setFrame(int fr) {
    if (haveFile && fr >= 0 && fr < nframes) {
      currentFrameNumber = fr;
      setFrame(chemfile.getFrame(fr));
    }
  }

  public void setFrame(ChemFrame chemframe) {
    ChemFrame chemframePrevious = this.chemframe;
    this.chemframe = chemframe;
    pcs.firePropertyChange(DisplayControl.PROP_CHEM_FRAME,
                           chemframePrevious, chemframe);
  }

  public int numberOfAtoms() {
    return (chemframe == null) ? 0 : chemframe.getAtomCount();
  }

  public int getAtomCount() {
    return getJmolFrame().getAtomCount();
  }

  /*
  public Atom[] getCurrentFrameAtoms() {
    return chemframe.getJmolAtoms();
  }
  */

  /*
  public ChemFrame[] getFrames() {
    return chemfile.getFrames();
  }
  */

  public void setCenterAsSelected() {
    int numberOfAtoms = numberOfAtoms();
    int countSelected = 0;
    Point3d  center = new Point3d(); // defaults to 0,00,
    BitSet bsSelection = control.getSelectionSet();
    for (int i = 0; i < numberOfAtoms; ++i) {
      if (!bsSelection.get(i))
        continue;
      ++countSelected;
      center.add(((Atom)chemframe.getAtomAt(i)).getPoint3D());
    }
    if (countSelected > 0) {
      center.scale(1.0f / countSelected); // just divide by the quantity
    } else {
      center = null;
    }
    chemframe.getJmolFrame().setRotationCenter(center);
  }

  public void setRotationCenter(Point3d center) {
    chemframe.getJmolFrame().setRotationCenter(center);
  }

  // FIXME NEEDSWORK -- bond binding stuff
  public double bondFudge = 1.12f;
  public boolean autoBond = true;

  public void rebond() {
    chemframe.getJmolFrame().rebond();
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
    chemframe.deleteAtom(atomIndex);
  }

  public int findNearestAtomIndex(int x, int y) {
    return chemframe.getJmolFrame().findNearestAtomIndex(x, y);
  }

  public BitSet findAtomsInRectangle(Rectangle rectRubber) {
    return chemframe.getJmolFrame().findAtomsInRectangle(rectRubber);
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
