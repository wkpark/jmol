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

package org.openscience.jmol.viewer.datamodel;

import org.jmol.api.ModelAdapter;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.pdb.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

final public class FrameExportModelAdapter extends ModelAdapter {

  public JmolViewer viewer;
  public Frame frame;

  public FrameExportModelAdapter(JmolViewer viewer, Frame frame) {
    super("FrameExportModelAdapter", null);
    this.viewer = viewer;
    this.frame = frame;
  }

  public String getModelName(Object clientFile) {
    return viewer.getModelName();
  }

  public int getAtomCount(Object clientFile) {
    return frame.atomCount;
  }

  public float[] getNotionalUnitcell(Object clientFile) {
    return frame.notionalUnitcell;
  }

  public ModelAdapter.AtomIterator
    getAtomIterator(Object clientFile) {
    return new AtomIterator();
  }

  public ModelAdapter.BondIterator
    getBondIterator(Object clientFile) {
    return new BondIterator();
  }

  class AtomIterator extends ModelAdapter.AtomIterator {
    int iatom;
    Atom atom;

    public boolean hasNext() {
      if (iatom == frame.atomCount)
        return false;
      atom = frame.atoms[iatom++];
      return true;
    }
    public Object getUniqueID() { return new Integer(iatom); }
    public int getElementNumber() { return atom.elementNumber; }
    public String getElementSymbol() { return atom.getElementSymbol(); }
    public int getAtomicCharge() { return atom.getAtomicCharge(); }
    public float getX() { return atom.getAtomX(); }
    public float getY() { return atom.getAtomY(); }
    public float getZ() { return atom.getAtomZ(); }
  }

  class BondIterator extends ModelAdapter.BondIterator {
    int ibond;
    Bond bond;

    public boolean hasNext() {
      if (ibond >= frame.bondCount)
        return false;
      bond = frame.bonds[ibond++];
      return true;
    }
    public Object getAtomUid1() { return new Integer(bond.atom1.atomIndex); }
    public Object getAtomUid2() { return new Integer(bond.atom2.atomIndex); }
    public int getOrder() { return bond.order; }
  }
}
