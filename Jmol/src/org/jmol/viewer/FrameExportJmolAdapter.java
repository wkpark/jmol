/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import org.jmol.api.JmolAdapter;
import org.jmol.modelframe.Atom;
import org.jmol.modelframe.Bond;
import org.jmol.modelframe.Frame;


final public class FrameExportJmolAdapter extends JmolAdapter {

  Viewer viewer;
  Frame frame;

  FrameExportJmolAdapter(Viewer viewer, Frame frame) {
    super("FrameExportJmolAdapter");
    this.viewer = viewer;
    this.frame = frame;
  }

  public String getAtomSetCollectionName(Object clientFile) {
    return viewer.getModelSetName();
  }

  public int getEstimatedAtomCount(Object clientFile) {
    return frame.getAtomCount();
  }

  public float[] getNotionalUnitcell(Object clientFile) {
    return frame.getNotionalUnitcell();
  }

  public JmolAdapter.AtomIterator
    getAtomIterator(Object clientFile) {
    return new AtomIterator();
  }

  public JmolAdapter.BondIterator
    getBondIterator(Object clientFile) {
    return new BondIterator();
  }

  class AtomIterator extends JmolAdapter.AtomIterator {
    int iatom;
    Atom atom;

    public boolean hasNext() {
      if (iatom == frame.getAtomCount())
        return false;
      atom = frame.atoms[iatom++];
      return true;
    }
    public Object getUniqueID() { return new Integer(iatom); }
    public int getElementNumber() { return atom.getElementNumber(); }//no isotope here
    public String getElementSymbol() { return atom.getElementSymbol(); }
    public int getFormalCharge() { return atom.getFormalCharge(); }
    public float getPartialCharge() { return atom.getPartialCharge(); }
    public float getX() { return atom.x; }
    public float getY() { return atom.y; }
    public float getZ() { return atom.z; }
  }

  class BondIterator extends JmolAdapter.BondIterator {
    int ibond;
    Bond bond;

    public boolean hasNext() {
      if (ibond >= frame.getBondCount())
        return false;
      bond = frame.getBonds()[ibond++];
      return true;
    }
    public Object getAtomUniqueID1(){
      return new Integer(bond.getAtomIndex1());
    }
    public Object getAtomUniqueID2() {
      return new Integer(bond.getAtomIndex2());
    }
    public int getEncodedOrder() { return bond.getOrder(); }
  }
}
