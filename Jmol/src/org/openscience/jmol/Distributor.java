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
import org.openscience.jmol.render.AtomShape;
import org.openscience.jmol.render.AtomShapeIterator;
import org.openscience.jmol.render.BondShape;
import org.openscience.jmol.render.BondShapeIterator;
import org.openscience.cdk.renderer.color.AtomColorer;

public class Distributor {

  DisplayControl control;

  public Distributor(DisplayControl control) {
    this.control = control;
  }

  /****************************************************************
   * the ChemFrame guys
   ****************************************************************/

  public void setStyleAtom(byte styleAtom, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      Atom atom = iter.nextAtom();
      AtomShape atomShape = atom.getAtomShape();
      if (atomShape == null)
        System.out.println("how in the hell did you get here?");
      atomShape.setStyleAtom(styleAtom);
    }
    //      iter.nextAtom().atomShape.setStyleAtom(styleAtom);
  }

  public void setMarAtom(short marAtom, JmolAtomIterator iter) {
    while (iter.hasNext())
      iter.nextAtom().getAtomShape().setMarAtom(marAtom);
  }

  public void setStyleMarAtom(byte style, short mar, JmolAtomIterator iter) {
    while (iter.hasNext())
      iter.nextAtom().getAtomShape().setStyleMarAtom(style, mar);
  }

  public void setColixAtom(byte mode, short colix, JmolAtomIterator iter) {
    boolean useColorProfile = colix == 0;
    while (iter.hasNext()) {
      Atom atom = iter.nextAtom();
      short colixT = useColorProfile
        ? control.getColixAtom(mode, atom) : colix;
      atom.getAtomShape().setColixAtom(colixT);
    }
  }

  public void setStyleLabel(byte styleLabel, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      Atom atom = iter.nextAtom();
      atom.getAtomShape().setLabel(control.getLabelAtom(styleLabel, atom));
    }
  }

  public void setLabel(String strLabel, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      Atom atom = iter.nextAtom();
      atom.getAtomShape().setLabel(control.getLabelAtom(strLabel, atom));
    }
  }

  public void setColixMarDots(short colixDots, short marDots,
                              JmolAtomIterator iter) {
    short colixT = colixDots;
    while (iter.hasNext()) {
      Atom atom = iter.nextAtom();
      if (colixDots == 0)
        colixT = control.getColixAtom(atom);
      atom.getAtomShape().setColixMarDots(colixT, marDots);
    }
  }

  /****************************************************************
   * the JmolFrame guys
   ****************************************************************/

  public void setStyleAtom(byte styleAtom, AtomShapeIterator iter) {
    while (iter.hasNext())
      iter.next().setStyleAtom(styleAtom);
  }

  public void setMarAtom(short marAtom, AtomShapeIterator iter) {
    while (iter.hasNext())
      iter.next().setMarAtom(marAtom);
  }

  public void setStyleMarAtom(byte style, short mar, AtomShapeIterator iter) {
    while (iter.hasNext())
      iter.next().setStyleMarAtom(style, mar);
  }

  public void setStyle(byte styleBond, BondShapeIterator iter) {
    while (iter.hasNext())
      iter.next().setStyle(styleBond);
  }

  public void setMar(short marBond, BondShapeIterator iter) {
    while (iter.hasNext())
      iter.next().setMar(marBond);
  }

  public void setStyleMar(byte style, short mar, BondShapeIterator iter) {
    while (iter.hasNext())
      iter.next().setStyleMar(style, mar);
  }

  public void setColix(short colixBond, BondShapeIterator iter) {
    while (iter.hasNext())
      iter.next().setColix(colixBond);
  }

  public void setColixAtom(byte mode, short colix, AtomShapeIterator iter) {
    boolean useColorProfile = colix == 0;
    while (iter.hasNext()) {
      AtomShape atomShape = iter.next();
      short colixT = useColorProfile
        ? control.getColixAtom(mode, atomShape.atom) : colix;
      atomShape.setColixAtom(colixT);
    }
  }

  public void setStyleLabel(byte styleLabel, AtomShapeIterator iter) {
    while (iter.hasNext()) {
      AtomShape atomShape = iter.next();
      atomShape.setLabel(control.getLabelAtom(styleLabel, atomShape.atom));
    }
  }

  public void setLabel(String strLabel, AtomShapeIterator iter) {
    while (iter.hasNext()) {
      AtomShape atomShape = iter.next();
      atomShape.setLabel(control.getLabelAtom(strLabel, atomShape.atom));
    }
  }

  public void setColixMarDots(short colixDots, short marDots,
                              AtomShapeIterator iter) {
    short colixT = colixDots;
    while (iter.hasNext()) {
      AtomShape atomShape = iter.next();
      if (colixDots == 0)
        colixT = control.getColixAtom(atomShape.atom);
      atomShape.setColixMarDots(colixT, marDots);
    }
  }
}

