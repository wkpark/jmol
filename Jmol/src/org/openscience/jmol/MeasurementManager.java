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
import org.openscience.jmol.render.MeasurementShape;

import java.util.BitSet;
import java.util.Vector;
import javax.vecmath.Point3d;
import java.awt.Rectangle;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class MeasurementManager {

  DisplayControl control;

  public MeasurementManager(DisplayControl control) {
    this.control = control;
  }

  public Vector distanceMeasurements = new Vector();
  public Vector angleMeasurements = new Vector();
  public Vector dihedralMeasurements = new Vector();

  public void clearMeasurements() {
    distanceMeasurements.removeAllElements();
    angleMeasurements.removeAllElements();
    dihedralMeasurements.removeAllElements();
  }

  public void defineMeasure(int[] atoms) {
    switch (atoms.length) {
    case 2:
      defineMeasure(atoms[0], atoms[1]);
      break;
    case 3:
      defineMeasure(atoms[0], atoms[1], atoms[2]);
      break;
    case 4:
      defineMeasure(atoms[0], atoms[1], atoms[2], atoms[3]);
      break;
    default:
      control.logError("unrecognized number of args to defineMeasure");
    }
  }

  public void defineMeasure(int iatom1, int iatom2) {
    JmolFrame frame = control.getJmolFrame();
    MeasurementShape meas =
      new MeasurementShape(iatom1, frame.getAtomAt(iatom1).getPoint3d(),
                           iatom2, frame.getAtomAt(iatom2).getPoint3d());
    frame.addMeasurementShape(meas);
    distanceMeasurements.addElement(meas);
  }

  public void defineMeasure(int iatom1, int iatom2, int iatom3) {
    JmolFrame frame = control.getJmolFrame();
    MeasurementShape meas =
      new MeasurementShape(iatom1, frame.getAtomAt(iatom1).getPoint3d(),
                           iatom2, frame.getAtomAt(iatom2).getPoint3d(),
                           iatom3, frame.getAtomAt(iatom3).getPoint3d());
    frame.addMeasurementShape(meas);
    angleMeasurements.addElement(meas);
  }

  public void defineMeasure(int iatom1, int iatom2, int iatom3, int iatom4) {
    JmolFrame frame = control.getJmolFrame();
    MeasurementShape meas =
      new MeasurementShape(iatom1, frame.getAtomAt(iatom1).getPoint3d(),
                           iatom2, frame.getAtomAt(iatom2).getPoint3d(),
                           iatom3, frame.getAtomAt(iatom3).getPoint3d(),
                           iatom4, frame.getAtomAt(iatom4).getPoint3d());
    frame.addMeasurementShape(meas);
    dihedralMeasurements.addElement(meas);
  }

  public boolean deleteMeasurement(MeasurementShape measurement) {
    return true;
  }

  public boolean deleteMatchingMeasurement(int atom1, int atom2) {
    return true;
  }

  public boolean deleteMatchingMeasurement(int atom1, int atom2, int atom3) {
    return true;
  }

  public boolean deleteMatchingMeasurement(int atom1, int atom2,
                                           int atom3, int atom4) {
    return true;
  }

}
