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
import org.openscience.jmol.render.Distance;
import org.openscience.jmol.render.Angle;
import org.openscience.jmol.render.Dihedral;

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
    ChemFrame cf = control.getFrame();
    Distance dist = new Distance(iatom1, cf.getJmolAtomAt(iatom1),
                                 iatom2, cf.getJmolAtomAt(iatom2));
    distanceMeasurements.addElement(dist);
  }

  public void defineMeasure(int iatom1, int iatom2, int iatom3) {
    ChemFrame cf = control.getFrame();
    Angle angle = new Angle(iatom1, cf.getJmolAtomAt(iatom1),
                            iatom2, cf.getJmolAtomAt(iatom2),
                            iatom3, cf.getJmolAtomAt(iatom3));
    angleMeasurements.addElement(angle);
  }

  public void defineMeasure(int iatom1, int iatom2, int iatom3, int iatom4) {
    ChemFrame cf = control.getFrame();
    Dihedral dihedral = new Dihedral(iatom1, cf.getJmolAtomAt(iatom1),
                                     iatom2, cf.getJmolAtomAt(iatom2),
                                     iatom3, cf.getJmolAtomAt(iatom3),
                                     iatom4, cf.getJmolAtomAt(iatom4));
    dihedralMeasurements.addElement(dihedral);
  }

}
