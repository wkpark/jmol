/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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

package org.jmol.viewer.datamodel;
import org.jmol.viewer.*;

public class Bond {

  Atom atom1;
  Atom atom2;
  short order;
  short mad;
  short colix;

  Bond(Atom atom1, Atom atom2, int order,
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
    this.order = (short)order;
    this.mad = mad;
    this.colix = colix;
  }

  Bond(Atom atom1, Atom atom2, int order, Viewer viewer) {
    this(atom1, atom2, order,
         (order & JmolConstants.BOND_HYDROGEN_MASK) != 0 ? 0 : viewer.getMadBond(),
         viewer.getColixBond(order));
  }

  boolean isCovalent() {
    return (order & JmolConstants.BOND_COVALENT) != 0;
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

  void setOrder(short order) {
    this.order = order;
  }

  public Atom getAtom1() {
    return atom1;
  }

  public Atom getAtom2() {
    return atom2;
  }

  public float getRadius() {
    return mad / 2000f;
  }

  public short getOrder() {
    return order;
  }

  public short getColix1() {
    return (colix != 0 ? colix : atom1.colixAtom);
  }

  public short getColix2() {
    return (colix != 0 ? colix : atom2.colixAtom);
  }
}

