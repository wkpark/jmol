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
import javax.vecmath.Point3d;

public class ModelManager {

  DisplayControl control;

  public ModelManager(DisplayControl control) {
    this.control = control;
  }

  public boolean haveFile = false;
  public ChemFile chemfile;
  public ChemFrame chemframe;
  public int nframes = 0;
  public MeasurementList mlist = null;

  public void setChemFile(ChemFile chemfile) {
    // FIXME -- I think I need to disable repaints during this process
    this.chemfile = chemfile;
    haveFile = true;
    nframes = chemfile.getNumberOfFrames();
    this.chemframe = chemfile.getFrame(0);
    Measurement.setChemFrame(chemframe);
    if (mlist != null) {
      mlistChanged(new MeasurementListEvent(mlist));
    }
  }

  public boolean haveFile() {
    return haveFile;
  }

  public double getRotationRadius() {
    return chemframe.getRotationRadius();
  }

  public Point3d getRotationCenter() {
    return chemframe.getRotationCenter();
  }

  public void setFrame(int fr) {
    if (haveFile && fr >= 0 && fr < nframes) {
        setFrame(chemfile.getFrame(fr));
    }
  }

  public void setFrame(ChemFrame frame) {
    chemframe = frame;
    Measurement.setChemFrame(frame);
    if (mlist != null) {
      mlistChanged(new MeasurementListEvent(mlist));
    }
  }

  public int numberOfAtoms() {
    return (chemframe == null) ? 0 : chemframe.getNumberOfAtoms();
  }

  public void mlistChanged(MeasurementListEvent mle) {
    MeasurementList source = (MeasurementList) mle.getSource();
    mlist = source;
    chemframe.updateMlists(mlist.getDistanceList(),
                           mlist.getAngleList(),
                           mlist.getDihedralList());
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
      center.add(((Atom)chemframe.getAtomAt(i)).getPosition());
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

  public void defineMeasure(int atom1, int atom2) {
    mlist.addDistance(atom1, atom2);
  }

  public void defineMeasure(int atom1, int atom2, int atom3) {
    mlist.addAngle(atom1, atom2, atom3);
  }

  public void defineMeasure(int atom1, int atom2, int atom3, int atom4) {
    mlist.addDihedral(atom1, atom2, atom3, atom4);
  }

  public void deleteAtom(Atom atom) {
    chemframe.deleteAtom(atom);
  }
}
