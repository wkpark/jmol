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
import org.openscience.jmol.viewer.*;

import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.g3d.Colix;

import java.awt.Rectangle;

public class Bond {

  /*
  public final static byte COVALENT    = 3;
  public final static byte STEREO      = (1 << 2);
  public final static byte STEREO_NEAR = (1 << 2) | 1;
  public final static byte STEREO_FAR  = (1 << 2) | 2;
  public final static byte AROMATIC    = (1 << 3) | 1;
  public final static byte SULFUR      = 1 << 4;
  public final static byte HYDROGEN    = 1 << 5;

  public final static byte ALL         = (byte)0xFF;
  */

  public Atom atom1;
  public Atom atom2;
  byte order;
  byte style;
  short mar;
  short colix;

  public Bond(Atom atom1, Atom atom2,
              int order, byte style, short mar, short colix) {
    if (atom1 == null)
      throw new NullPointerException();
    if (atom2 == null)
      throw new NullPointerException();
    this.atom1 = atom1;
    this.atom2 = atom2;
    if (atom1.atomicNumber == 16 && atom2.atomicNumber == 16)
      order |= JmolConstants.BOND_SULFUR;
    this.order = (byte)order;
    this.style = style;
    this.mar = mar;
    this.colix = colix;
  }

  public Bond(Atom atom1, Atom atom2, int order, JmolViewer viewer) {
    this(atom1, atom2, order, viewer.getStyleBond(),
         viewer.getMarBond(), viewer.getColixBond());
  }

  public boolean isCovalent() {
    return (order & JmolConstants.BOND_COVALENT) != 0;
  }

  public boolean isStereo() {
    return (order & JmolConstants.BOND_STEREO) != 0;
  }

  public void deleteAtomReferences() {
    if (atom1 != null)
      atom1.deleteBond(this);
    if (atom2 != null)
      atom2.deleteBond(this);
    atom1 = atom2 = null;
  }

  public void setStyle(byte style) {
    this.style = style;
  }

  public void setMar(short mar) {
    this.mar = mar;
  }

  public void setStyleMar(byte style, short mar) {
    this.style = style;
    this.mar = mar;
  }

  public void setColix(short colix) {
    this.colix = colix;
  }

  public float getRadius() {
    if (style == JmolConstants.STYLE_NONE) return 0;
    float radius = mar/1000f;
    if (style == JmolConstants.STYLE_WIREFRAME)
      return -radius;
    return radius;
  }

  public byte getOrder() {
    return order;
  }

  public short getColix1() {
    return (colix != 0 ? colix : atom1.colixAtom);
  }

  public short getColix2() {
    return (colix != 0 ? colix : atom2.colixAtom);
  }
}

