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

import java.awt.Color;

public class Distributor {

  DisplayControl control;

  public Distributor(DisplayControl control) {
    this.control = control;
  }

  public void initializeAtomShapes() {
    byte styleAtom = control.getStyleAtom();
    int madAtom = -control.getPercentVdwAtom();
    byte styleBond = control.getStyleBond();
    int madBond = control.getPercentAngstromBond() * 10;
    Color colorBond = control.getColorBond();
    JmolAtomIterator iter = control.getChemFileIterator();
    while (iter.hasNext()) {
      Atom atom = iter.nextAtom();
      atom.setAtomShape(new AtomShape(atom,
                                      styleAtom, madAtom,
                                      control.getColorAtom(atom),
                                      styleBond, madBond,
                                      colorBond,
                                      control.getLabelAtom(atom)));
    }
  }

  public void setStyleAtom(byte styleAtom, JmolAtomIterator iter) {
    while (iter.hasNext())
      iter.nextAtom().atomShape.setStyleAtom(styleAtom);
  }

  public void setMadAtom(int madAtom, JmolAtomIterator iter) {
    while (iter.hasNext())
      iter.nextAtom().atomShape.setMadAtom(madAtom);
  }

  public void setStyleBond(byte styleBond, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      if (iter.allBonds())
        iter.nextAtom().atomShape.setStyleAllBonds(styleBond);
      else
        iter.nextAtom().atomShape.setStyleBond(styleBond, iter.indexBond());
    }
  }

  public void setMadBond(int madBond, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      if (iter.allBonds())
        iter.nextAtom().atomShape.setMadAllBonds(madBond);
      else
        iter.nextAtom().atomShape.setMadBond(madBond, iter.indexBond());
    }
  }

  public void setColorBond(Color colorBond, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      if (iter.allBonds())
        iter.nextAtom().atomShape.setColorAllBonds(colorBond);
      else
        iter.nextAtom().atomShape.setColorBond(colorBond, iter.indexBond());
    }
  }

  public void setColorAtom(Color colorAtom, JmolAtomIterator iter) {
    boolean useColorProfile = colorAtom == null;
    while (iter.hasNext()) {
      Atom atom = iter.nextAtom();
      Color color = useColorProfile ? control.getColorAtom(atom) : colorAtom;
      atom.atomShape.setColorAtom(color);
    }
  }

  public void setStyleLabel(byte styleLabel, JmolAtomIterator iter) {
    while (iter.hasNext()) {
      Atom atom = iter.nextAtom();
      atom.atomShape.setLabel(control.getLabelAtom(styleLabel, atom));
    }
  }
}
