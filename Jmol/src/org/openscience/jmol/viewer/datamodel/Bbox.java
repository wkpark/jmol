/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

import java.awt.Font;
import java.awt.FontMetrics;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import java.util.BitSet;

class Bbox implements Graphic {

  JmolViewer viewer;

  final Point3f[] bboxPoints = new Point3f[8];

  final static Point3f[] unitBboxPoints = {
    new Point3f( 1, 1, 1),
    new Point3f( 1, 1,-1),
    new Point3f( 1,-1, 1),
    new Point3f( 1,-1,-1),
    new Point3f(-1, 1, 1),
    new Point3f(-1, 1,-1),
    new Point3f(-1,-1, 1),
    new Point3f(-1,-1,-1),
  };

  // the points I am connected with are at the indices obtained
  // by XORing each of the three bits of my index
  final static byte edges[] =
  {0, 1, 0, 2, 0, 4, 1, 3, 1, 5, 2, 3, 2, 6, 3, 7, 4, 5, 4, 6, 5, 7, 6, 7};

  Bbox(JmolViewer viewer, Frame frame) {
    this.viewer = viewer;
    for (int i = 8; --i >= 0; )
      bboxPoints[i] = new Point3f();
  }

  boolean show;

  public void setShow(boolean show) {
    this.show = show;
    if (! show)
      return;
    Point3f pointOrigin = viewer.getBoundingBoxCenter();
    Point3f pointCorner = viewer.getBoundingBoxCorner();
    for (int i = 0; i < 8; ++i) {
      Point3f bboxPoint = bboxPoints[i];
      bboxPoint.set(unitBboxPoints[i]);
      bboxPoint.x *= pointCorner.x;
      bboxPoint.y *= pointCorner.y;
      bboxPoint.z *= pointCorner.z;
      bboxPoint.add(pointOrigin);
    }
  }

  public void setMad(short mad, BitSet bsSelected) {
  }
  
  public void setColix(byte palette, short colix, BitSet bsSelected) {
  }
}
