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

import org.jmol.g3d.Graphics3D;
import java.util.Hashtable;

class Bond {

  Atom atom1;
  Atom atom2;
  short order;
  short mad;
  short colix;

  Bond(Atom atom1, Atom atom2, short order,
              short mad, short colix) {
    if (atom1 == null)
      throw new NullPointerException();
    if (atom2 == null)
      throw new NullPointerException();
    this.atom1 = atom1;
    this.atom2 = atom2;
    if (atom1.elementNumber == 16 && atom2.elementNumber == 16)
      order |= JmolConstants.BOND_SULFUR_MASK;
    if (order == JmolConstants.BOND_AROMATIC_MASK)
      order = JmolConstants.BOND_AROMATIC;
    this.order = order;
    this.mad = mad;
    this.colix = colix;
  }

  Bond(Atom atom1, Atom atom2, short order, Frame frame) {
    this(atom1, atom2, order,
         ((order & JmolConstants.BOND_HYDROGEN_MASK) != 0
          ? 0 : frame.viewer.getMadBond()), (short)0);
  }

  boolean isCovalent() {
    return (order & JmolConstants.BOND_COVALENT_MASK) != 0;
  }

  boolean isStereo() {
    return (order & JmolConstants.BOND_STEREO_MASK) != 0;
  }

  boolean isAromatic() {
    return (order & JmolConstants.BOND_AROMATIC_MASK) != 0;
  }

  void deleteAtomReferences() {
    if (atom1 != null)
      atom1.deleteBond(this);
    if (atom2 != null)
      atom2.deleteBond(this);
    atom1 = atom2 = null;
  }

  void setMad(short mad) {
    this.mad = mad;
  }

  void setColix(short colix) {
    this.colix = colix;
  }

  void setTranslucent(boolean isTranslucent) {
    colix = Graphics3D.setTranslucent(colix, isTranslucent);
  }

  void setOrder(short order) {
    this.order = order;
  }

  Atom getAtom1() {
    return atom1;
  }

  Atom getAtom2() {
    return atom2;
  }

  float getRadius() {
    return mad / 2000f;
  }

  short getOrder() {
    return order;
  }

  String getOrderName() {
    switch (order) {
    case 1:
      return "single";
    case 2:
      return "double";
    case 3:
      return "triple";
    case 4:
      return "aromatic";
    }
    if ((order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
      return "hbond";
    return "unknown";
  }

  short getColix1() {
    return Graphics3D.inheritColix(colix, atom1.colixAtom);
  }

  int getArgb1() {
    return atom1.group.chain.frame.viewer.getColixArgb(getColix1());
  }

  short getColix2() {
    return Graphics3D.inheritColix(colix, atom2.colixAtom);
  }

  int getArgb2() {
    return atom1.group.chain.frame.viewer.getColixArgb(getColix2());
  }

  ////////////////////////////////////////////////////////////////
  
  Hashtable getPublicProperties() {
    Hashtable ht = new Hashtable();
    ht.put("atomIndexA", new Integer(atom1.atomIndex));
    ht.put("atomIndexB", new Integer(atom2.atomIndex));
    ht.put("argbA", new Integer(getArgb1()));
    ht.put("argbB", new Integer(getArgb2()));
    ht.put("order", getOrderName());
    ht.put("radius", new Double(getRadius()));
    ht.put("modelIndex", new Integer(atom1.modelIndex));
    ht.put("xA", new Double(atom1.point3f.x));
    ht.put("yA", new Double(atom1.point3f.y));
    ht.put("zA", new Double(atom1.point3f.z));
    ht.put("xB", new Double(atom2.point3f.x));
    ht.put("yB", new Double(atom2.point3f.y));
    ht.put("zB", new Double(atom2.point3f.z));
    return ht;
  }
}

