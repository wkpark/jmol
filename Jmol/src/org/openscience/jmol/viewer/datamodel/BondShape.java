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

  AtomShape atomShape1;
  AtomShape atomShape2;
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

  public AtomShape getOtherAtomShape(AtomShape atomShape) {
    return (atomShape == atomShape1) ? atomShape2 : atomShape1;
  }
  
  public void render(JmolViewer viewer) {
    int z = (atomShape1.z + atomShape2.z) / 2;
    int diameter = viewer.scaleToScreen(z, mar * 2);
    viewer.bondRenderer.render(atomShape1, atomShape2, order,
                                style, mar, colix, diameter);
  }

  // FIXME ... do something about this rectTemp, probably stick it in the BondRenderer
  Rectangle rectTemp = new Rectangle();

  private boolean isClipVisible(Rectangle clip,
                                int x1, int y1, int x2, int y2) {
    // this is not actually correct, but quick & dirty
    int xMin, width, yMin, height;
    if (x1 < x2) {
      xMin = x1;
      width = x2 - x1;
    } else if (x2 < x1) {
      xMin = x2;
      width = x1 - x2;
    } else {
      xMin = x1;
      width = 1;
    }
    if (y1 < y2) {
      yMin = y1;
      height = y2 - y1;
    } else if (y2 < y1) {
      yMin = y2;
      height = y1 - y2;
    } else {
      yMin = y1;
      height = 1;
    }
    // there are some problems with this quick&dirty implementation
    // so I am going to throw in some slop
    xMin -= 5;
    yMin -= 5;
    width += 10;
    height += 10;
    rectTemp.x = xMin;
    rectTemp.y = yMin;
    rectTemp.width = width;
    rectTemp.height = height;
    boolean visible = clip.intersects(rectTemp);
    /*
    System.out.println("bond " + x + "," + y + "->" + x2 + "," + y2 +
                       " & " + clip.x + "," + clip.y +
                       " W " + clip.width + " H " + clip.height +
                       "->" + visible);
    visible = true;
    */
    return visible;
  }
}

