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

  public void setStyleBond(byte styleBond, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      AtomShape atomShape = iter.nextAtom().getAtomShape();
      if (iter.allBonds())
        atomShape.setStyleAllBonds(styleBond);
      else
        atomShape.setStyleBond(styleBond, iter.indexBond());
    }
  }

  public void setMarBond(short marBond, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      AtomShape atomShape = iter.nextAtom().getAtomShape();
      if (iter.allBonds())
        atomShape.setMarAllBonds(marBond);
      else
        atomShape.setMarBond(marBond, iter.indexBond());
    }
  }

  public void setStyleMarBond(byte style, short mar, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      AtomShape atomShape = iter.nextAtom().getAtomShape();
      if (iter.allBonds())
        atomShape.setStyleMarAllBonds(style, mar);
      else
        atomShape.setStyleMarBond(style, mar,iter.indexBond());
    }
  }

  public void setStyleBackbone(byte styleBackbone, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      AtomShape atomShape = iter.nextAtom().getAtomShape();
      if (iter.allBonds())
        atomShape.setStyleAllBackbones(styleBackbone);
      else
        atomShape.setStyleBackbone(styleBackbone, iter.indexBond());
    }
  }

  public void setStyleMarBackbone(byte style,short mar,JmolAtomIterator iter) {
    while (iter.hasNext()) {
      AtomShape atomShape = iter.nextAtom().getAtomShape();
      if (iter.allBonds())
        atomShape.setStyleMarAllBackbones(style, mar);
      else
        atomShape.setStyleMarBackbone(style, mar,iter.indexBond());
    }
  }

  public void setColixBond(short colixBond, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      AtomShape atomShape = iter.nextAtom().getAtomShape();
      if (iter.allBonds())
        atomShape.setColixAllBonds(colixBond);
      else
        atomShape.setColixBond(colixBond, iter.indexBond());
    }
  }

  public void setColixBackbone(short colixBackbone, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      AtomShape atomShape = iter.nextAtom().getAtomShape();
      if (iter.allBonds())
        atomShape.setColixAllBackbones(colixBackbone);
      else
        atomShape.setColixBackbone(colixBackbone, iter.indexBond());
    }
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

}
