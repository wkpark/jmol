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

import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.JmolViewer;

import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

public class ArrowLineShape extends LineShape {

  float headWidthAngstroms;
  int[] ax = new int[4];
  int[] ay = new int[4];
  int[] az = new int[4];

  final static int widthDivisor = 8;
  final static int shaftDivisor = 5;
  final static int finDivisor = 6;

  public ArrowLineShape(Point3f pointOrigin, Point3f pointVector) {
    super(pointOrigin, pointVector);
    headWidthAngstroms =
	(float)pointOrigin.distance(this.pointEnd) / widthDivisor;
  }

  public void render(Graphics3D g3d, JmolViewer viewer) {
    short colixVector = viewer.getColixVector();
    g3d.drawLine(colixVector, x, y, z, xEnd, yEnd, zEnd);

    int dx = xEnd - x, xHead = xEnd - (dx / shaftDivisor);
    int dy = yEnd - y, yHead = yEnd - (dy / shaftDivisor);
    int mag2d = (int)(Math.sqrt(dx*dx + dy*dy) + 0.5);
    int dz = zEnd - z, zHead = zEnd - (dz / shaftDivisor);
    int headWidthPixels =
      (int)(viewer.scaleToScreen(zHead, headWidthAngstroms) + 0.5f);

    ax[0] = xEnd; ax[2] = xEnd - dx/finDivisor;
    ay[0] = yEnd; ay[2] = yEnd - dy/finDivisor;
    az[0] = zEnd; az[2] = zEnd - dz/finDivisor;
    int dxHead, dyHead;
    if (mag2d == 0) {
      dxHead = 0;
      dyHead = headWidthPixels;
    } else {
      dxHead = headWidthPixels * -dy / mag2d;
      dyHead = headWidthPixels * dx / mag2d;
    }

    ax[1] = xHead - dxHead/2; ax[3] = ax[1] + dxHead;
    ay[1] = yHead - dyHead/2; ay[3] = ay[1] + dyHead;
    az[1] = zHead;            az[3] = zHead;
    g3d.fillPolygon4(colixVector, ax, ay, az);
  }
}


