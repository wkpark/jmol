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
package org.openscience.jmol.viewer.managers;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.datamodel.AtomIterator;
import org.openscience.jmol.viewer.datamodel.Bond;
import org.openscience.jmol.viewer.datamodel.BondIterator;

public class DistributionManager {

  JmolViewer viewer;

  public DistributionManager(JmolViewer viewer) {
    this.viewer = viewer;
  }

  /****************************************************************
   * the JmolFrame guys
   ****************************************************************/

  /*
  public void setMarAtom(short marAtom, AtomIterator iter) {
    while (iter.hasNext())
      iter.next().setMarAtom(marAtom);
  }
  */

  public void setMarBond(short marBond, BondIterator iter) {
    while (iter.hasNext())
      iter.next().setMar(marBond);
  }

  public void setColix(short colixBond, BondIterator iter) {
    while (iter.hasNext())
      iter.next().setColix(colixBond);
  }

  /*
  public void setColixAtom(byte palette, short colix, AtomIterator iter) {
    boolean useColorProfile = colix == 0;
    while (iter.hasNext()) {
      Atom atom = iter.next();
      short colixT = (useColorProfile
                      ? viewer.getColixAtomPalette(atom, palette) : colix);
      atom.setColixAtom(colixT);
    }
  }
  */

  /*
  public void setStyleLabel(byte styleLabel, AtomIterator iter) {
    while (iter.hasNext()) {
      Atom atom = iter.next();
      atom.setLabel(viewer.getLabelAtom(styleLabel,
                                              atom,
                                              atom.atomIndex));
    }
  }
  */

  /*
  public void setLabel(String strLabel, AtomIterator iter) {
    while (iter.hasNext()) {
      Atom atom = iter.next();
      atom.setLabel(viewer.getLabelAtom(strLabel, atom,
                                              atom.atomIndex));
    }
  }
  */
}
