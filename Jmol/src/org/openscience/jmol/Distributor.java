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

import org.openscience.jmol.render.AtomShape;
import org.openscience.jmol.render.BondShape;
import org.openscience.cdk.renderer.color.AtomColorer;

public class Distributor {

  DisplayControl control;

  public Distributor(DisplayControl control) {
    this.control = control;
  }

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

  public void setStyle(byte styleBond, byte bondType) {
    BondShapeIterator iter = new BondShapeIterator(bondType);
    while (iter.hasNext())
      iter.nextBondShape().setStyle(styleBond);
  }

  public void setMar(short marBond, byte bondType) {
    BondShapeIterator iter = new BondShapeIterator(bondType);
    while (iter.hasNext())
      iter.nextBondShape().setMar(marBond);
  }

  public void setStyleMar(byte style, short mar, byte bondType) {
    BondShapeIterator iter = new BondShapeIterator(bondType);
    while (iter.hasNext()) {
      BondShape bond = iter.nextBondShape();
      bond.setStyle(style);
      bond.setMar(mar);
    }
  }

  public void setColix(short colixBond, byte bondType) {
    BondShapeIterator iter = new BondShapeIterator(bondType);
    while (iter.hasNext())
      iter.nextBondShape().setColix(colixBond);
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

  class BondShapeIterator {

    JmolAtomIterator iterAtomSelected;
    boolean bondSelectionModeOr;
    AtomShape atomShapeCurrent;
    BondShape[] bondsCurrent;
    BondShape bondCurrent;
    int ibondCurrent;
    int bondType;

    BondShapeIterator(byte bondType) {
      this.bondType = bondType;
      iterAtomSelected = control.iterAtomSelected();
      bondSelectionModeOr = control.getBondSelectionModeOr();
    }

    public boolean hasNext() {
      while (true) {
        if (atomShapeCurrent != null) {
          while (bondsCurrent != null && ibondCurrent < bondsCurrent.length) {
            bondCurrent = bondsCurrent[ibondCurrent++];
            if ((bondCurrent.order & bondType) != 0) {
              if (bondSelectionModeOr)
                return true;
              AtomShape atomShapeOther =
                (bondCurrent.atomShape1 != atomShapeCurrent) ?
                bondCurrent.atomShape1 : bondCurrent.atomShape2;
              if (atomShapeOther.isSelected())
                return true;
            }
          }
          bondCurrent = null;
          atomShapeCurrent = null;
        }
        if (! iterAtomSelected.hasNext())
          return false;
        atomShapeCurrent = iterAtomSelected.nextAtom().getAtomShape();
        bondsCurrent = atomShapeCurrent.getBonds();
        bondCurrent = null;
        ibondCurrent = 0;
      }
    }
    
    public BondShape nextBondShape() {
      return bondCurrent;
    }
  }
}

