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
import org.openscience.jmol.viewer.JmolViewer;

import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.g3d.Colix;

import java.awt.Rectangle;

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

public class BondShape {

  public final static byte COVALENT = 3;
  public final static byte BACKBONE = 4;
  public final static byte ALL = COVALENT | BACKBONE;

  public AtomShape atomShape1;
  public AtomShape atomShape2;
  byte order;
  byte style;
  short mar;
  short colix;

  public BondShape(AtomShape atomShape1, AtomShape atomShape2,
                   int order, byte style, short mar, short colix) {
    if (atomShape1 == null)
      throw new NullPointerException();
    if (atomShape2 == null)
      throw new NullPointerException();
    this.atomShape1 = atomShape1;
    this.atomShape2 = atomShape2;
    this.order = (byte)order;
    this.style = style;
    this.mar = mar;
    this.colix = colix;
  }

  public BondShape(AtomShape atomShape1, AtomShape atomShape2, int order,
                   JmolViewer viewer) {
    if (atomShape1 == null)
      throw new NullPointerException();
    if (atomShape2 == null)
      throw new NullPointerException();
    this.atomShape1 = atomShape1;
    this.atomShape2 = atomShape2;
    this.order = (byte)order;
    this.style = (order == BACKBONE
                  ? JmolViewer.NONE
                  : viewer.getStyleBond());
    this.mar = viewer.getMarBond();
    this.colix = viewer.getColixBond();
  }

  public boolean isCovalent() {
    return (order & COVALENT) != 0;
  }

  public boolean isBackbone() {
    return (order & BACKBONE) != 0;
  }

  public void delete() {
    atomShape1.deleteBond(this);
    atomShape2.deleteBond(this);
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

  public double getRadius() {
    switch (style) {
    case JmolViewer.NONE:
      return 0;
    case JmolViewer.WIREFRAME:
      return -1;
    default:
      return mar/1000.0;
    }
  }

  public short getColix1() {
    return (colix != 0 ? colix : atomShape1.colixAtom);
  }

  public short getColix2() {
    return (colix != 0 ? colix : atomShape2.colixAtom);
  }
}

