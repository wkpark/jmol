/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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
package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;


class PruebaRenderer extends ShapeRenderer {

  private final static int level = 1;

  void render() {
    Prueba prueba = (Prueba)shape;
    short colix = prueba.colix;

    int vertexCount = g3d.getGeodesicVertexCount(level);
    Vector3f[] vectors = g3d.getGeodesicVertexVectors();
    Vector3f[] tvs = g3d.getTransformedVertexVectors();
    Point3i[] screens = viewer.allocTempScreens(vertexCount);
    short[] geodesicFaceVertexes = g3d.getGeodesicFaceVertexes(level);
    int geodesicFaceCount = g3d.getGeodesicFaceCount(level);
      
    calcScreens(vertexCount, tvs, screens);

    for (int i = geodesicFaceCount, j = 0; --i >= 0; ) {
      short vA = geodesicFaceVertexes[j++];
      short vB = geodesicFaceVertexes[j++];
      short vC = geodesicFaceVertexes[j++];
      g3d.fillTriangle(colix, false,
                       screens[vA], vA,
                       screens[vB], vB,
                       screens[vC], vC);
    }
    viewer.freeTempScreens(screens);
  }

  void calcScreens(int count, Vector3f[] tvs, Point3i[] screens) {
    float scaledRadius = viewer.scaleToScreen(1000, 1f);
    for (int i = count; --i >= 0; ) {
      Vector3f tv = tvs[i];
      Point3i screen = screens[i];
      screen.x = 150 + (int)(scaledRadius * tv.x);
      screen.y = 150 - (int)(scaledRadius * tv.y); // y inverted on screen!
      screen.z = 1000 - (int)(scaledRadius * tv.z); // smaller z comes to me
    }
  }
}
