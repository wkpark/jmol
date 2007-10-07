/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-08-05 16:26:58 -0500 (Sun, 05 Aug 2007) $
 * $Revision: 8032 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
package org.jmol.shape;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;

abstract class FontLineShapeRenderer extends ShapeRenderer {

  protected short colix;
  
  protected void render(short mad, Point3f[] vertices, Point3i[] screens,
                        Point3f[] axisPoints, int firstLine) {
    //used by Bbcage and Uccage
    if (!isGenerator)
      g3d.setColix(colix);
    int zSum = 0;
    for (int i = 8; --i >= 0;) {
      viewer.transformPoint(vertices[i], screens[i]);
      zSum += screens[i].z;
    }
    if (mad > 0 && mad < 2)
      mad = 2;
    int widthPixels = mad;
    if (mad >= 20) {
      widthPixels = viewer.scaleToScreen(zSum / 8, mad);
    }
    int axisPt = 2;
    for (int i = firstLine * 2; i < 24; i += 2) {
      int edge0 = Bbcage.edges[i];
      if (axisPoints != null && edge0 == 0)
        viewer.transformPoint(axisPoints[axisPt--], screens[0]);
      if (mad < 0)
        drawDottedLine(screens[edge0], screens[Bbcage.edges[i + 1]]);
      else
        fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, widthPixels, screens[edge0],
            screens[Bbcage.edges[i + 1]]);
    }
  }  

  protected void drawString(String str, Font3D font3d,
                         int xBaseline, int yBaseline, int z, int zSlab) {
    g3d.drawString(str, font3d, xBaseline, yBaseline, z, zSlab);
  }
  
  protected void fillCylinder(byte endcaps, int diameter,
                              Point3i screenA, Point3i screenB) {
    g3d.fillCylinder(endcaps, diameter, screenA, screenB);
  }
  
  protected void drawDottedLine(Point3i pointA, Point3i pointB) {
    g3d.drawDottedLine(pointA, pointB);
  }
  
}

