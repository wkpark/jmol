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
import org.openscience.jmol.render.AtomShapeIterator;
import org.openscience.jmol.render.BondShape;
import org.openscience.jmol.render.BondShapeIterator;

public class Distributor {

  DisplayControl control;

  public Distributor(DisplayControl control) {
    this.control = control;
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
       ? control.getColixAtom(mode, atomShape.clientAtom) : colix;
      atomShape.setColixAtom(colixT);
    }
  }

  public void setStyleLabel(byte styleLabel, AtomShapeIterator iter) {
    while (iter.hasNext()) {
      AtomShape atomShape = iter.next();
      atomShape.setLabel(control.getLabelAtom(styleLabel,
                                              atomShape.clientAtom));
    }
  }

  public void setLabel(String strLabel, AtomShapeIterator iter) {
    while (iter.hasNext()) {
      AtomShape atomShape = iter.next();
      atomShape.setLabel(control.getLabelAtom(strLabel,
                                              atomShape.clientAtom));
    }
  }

  public void setColixMarDots(short colixDots, short marDots,
                              AtomShapeIterator iter) {
    short colixT = colixDots;
    while (iter.hasNext()) {
      AtomShape atomShape = iter.next();
      if (colixDots == 0)
        colixT = control.getColixAtom(atomShape.clientAtom);
      atomShape.setColixMarDots(colixT, marDots);
    }
  }
}

