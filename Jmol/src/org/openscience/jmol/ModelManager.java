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

import org.openscience.jmol.Atom;

import java.util.BitSet;
import java.util.Vector;
import javax.vecmath.Point3d;
import java.awt.Rectangle;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ModelManager {

  DisplayControl control;

  public ModelManager(DisplayControl control) {
    this.control = control;
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
    return chemframe.getRotationRadius();
  }

  public Point3d getRotationCenter() {
    return chemframe.getRotationCenter();
  }

  public Point3d getBoundingBoxCenter() {
    return haveFile ? chemframe.getBoundingBoxCenter() : null;
  }

  public Point3d getBoundingBoxCorner() {
    return haveFile ? chemframe.getBoundingBoxCorner() : null;
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

  public Atom[] getCurrentFrameAtoms() {
    return chemframe.getJmolAtoms();
  }

  public ChemFrame[] getFrames() {
    return chemfile.getFrames();
  }

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
    chemframe.setRotationCenter(center);
  }

  public void setRotationCenter(Point3d center) {
    chemframe.setRotationCenter(center);
  }

  // FIXME NEEDSWORK -- bond binding stuff
  public double bondFudge = 1.12f;
  public boolean autoBond = true;

  public void rebond() {
    if (chemframe != null) {
      try {
        chemframe.rebond();
      } catch (Exception e){
      }
    }
  }

  public void setBondFudge(double bf) {
    bondFudge = bf;
  }

  public void setAutoBond(boolean ab) {
    autoBond = ab;
  }

  public void deleteAtom(int atomIndex) {
    chemframe.deleteAtom(atomIndex);
  }

  public JmolAtomIterator getChemFileIterator() {
    return chemfile.getJmolAtomIterator();
  }

  public JmolAtomIterator getChemFrameIterator(BitSet set) {
    return chemframe.getJmolAtomIterator(set);
  }

  public JmolAtomIterator getChemFrameIterator(BitSet set,boolean bondmodeOr) {
    return chemframe.getJmolBondIterator(set, bondmodeOr);
  }

  public int findNearestAtomIndex(int x, int y) {
    return chemframe.findNearestAtomIndex(x, y);
  }

  public BitSet findAtomsInRectangle(Rectangle rectRubber) {
    return chemframe.findAtomsInRectangle(rectRubber);
    /*
    return chemframe.findAtomsInRegion(rectRubber.x,
                                       rectRubber.y,
                                       rectRubber.x + rectRubber.width,
                                       rectRubber.y + rectRubber.height);
    */
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
