/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-19 12:18:17 -0500 (Mon, 19 Mar 2007) $
 * $Revision: 7171 $
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
import org.jmol.g3d.Geodesic3D;
import javax.vecmath.Point3i;

class GeoSurfaceRenderer extends DotsRenderer {

  void render() {
    GeoSurface gs = (GeoSurface) shape;
    if (gs == null || gs.isCalcOnly)
      return;
    iShowSolid = !(viewer.getInMotion() && gs.dotsConvexMax > 100);
    if (iShowSolid && !g3d.setColix(gs.surfaceColix) ||
        !iShowSolid && !g3d.setColix(Graphics3D.BLACK))
      return;
    render1(gs);
  }
  
  void renderConvex(Atom atom, short colix, int[] visibilityMap) {
    if (iShowSolid) {
      renderGeodesicFragment(visibilityMap, mapAtoms, screenDotCount);
    } else {
      super.renderConvex(atom, colix, visibilityMap);
    }
  }
  
  Point3i facePt1 = new Point3i();
  Point3i facePt2 = new Point3i();
  Point3i facePt3 = new Point3i();
  
  void renderGeodesicFragment(int[] points, int[] map, int dotCount) {
    if (map == null)
      return;
    short[] faces = Geodesic3D.faceVertexesArrays[screenLevel];
    int[] coords = screenCoordinates;
    short p1, p2, p3;
    int mapMax = (points.length << 5);
    //Logger.debug("geod frag "+mapMax+" "+dotCount);
    if (dotCount < mapMax)
      mapMax = dotCount;
    for (int f = 0; f < faces.length;) {
      p1 = faces[f++];
      p2 = faces[f++];
      p3 = faces[f++];
      if (p1 >= mapMax || p2 >= mapMax || p3 >= mapMax)
        continue;
      //Logger.debug("geod frag "+p1+" "+p2+" "+p3+" "+dotCount);
      if (!Dots.getBit(points, p1) || !Dots.getBit(points, p2)
          || !Dots.getBit(points, p3))
        continue;
      facePt1.set(coords[map[p1]], coords[map[p1] + 1], coords[map[p1] + 2]);
      facePt2.set(coords[map[p2]], coords[map[p2] + 1], coords[map[p2] + 2]);
      facePt3.set(coords[map[p3]], coords[map[p3] + 1], coords[map[p3] + 2]);
      g3d.calcSurfaceShade(facePt1, facePt2, facePt3);
      g3d.fillTriangle(facePt1, facePt2, facePt3);
    }
  }  
}

