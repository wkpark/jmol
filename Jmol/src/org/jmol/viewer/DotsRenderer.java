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
import org.jmol.g3d.Geodesic3D;
import javax.vecmath.Vector3f;


class DotsRenderer extends ShapeRenderer {

  boolean iShowSolid;
  
  Vector3f[] verticesTransformed;
  int screenLevel;
  int screenDotCount;
  int[] screenCoordinates;
  int[] faceMap = null; // used only by GeoSurface, but set here
  
  void initRenderer() {
    screenLevel = Dots.MAX_LEVEL;
    screenDotCount = Geodesic3D.getVertexCount(Dots.MAX_LEVEL);
    verticesTransformed = new Vector3f[screenDotCount];
    for (int i = screenDotCount; --i >= 0; )
      verticesTransformed[i] = new Vector3f();
    screenCoordinates = new int[3 * screenDotCount];
  }

  void render() {
    Dots dots = (Dots) shape;
    if (dots.isCalcOnly) //because we were using that shape as a calculator as well.
      return;
    render1(dots);
  }

  protected void render1(Dots dots) {
    //dots.timeBeginExecution = System.currentTimeMillis();
    if (!iShowSolid && !g3d.setColix(Graphics3D.BLACK)) // no translucent for dots
      return;
    int sppa = (int) viewer.getScalePixelsPerAngstrom();
    screenLevel = (iShowSolid || sppa > 20 ? 3 : sppa > 10 ? 2 : sppa > 5 ? 1
        : 0);
    screenDotCount = Geodesic3D.getVertexCount(screenLevel);
    for (int i = screenDotCount; --i >= 0;)
      viewer.transformVector(Geodesic3D.vertexVectors[i],
          verticesTransformed[i]);
    for (int i = dots.dotsConvexMax; --i >= 0;) {
      Atom atom = frame.atoms[i];
      if (!atom.isShapeVisible(myVisibilityFlag) || frame.bsHidden.get(i)
          || !g3d.isInDisplayRange(atom.screenX, atom.screenY))
        continue;
      int nPoints = calcScreenPoints(dots.dotsConvexMaps[i], dots
          .getAppropriateRadius(atom), atom.screenX, atom.screenY, atom.screenZ);
      if (nPoints != 0)
        renderConvex(Graphics3D.getColixInherited(dots.colixes[i],
            atom.colixAtom), dots.dotsConvexMaps[i], nPoints);
    }
    //dots.timeEndExecution = System.currentTimeMillis();
    //Logger.debug("dots rendering time = "+ gs.getExecutionWalltime());
  }
  
  /**
   * calculates the screen xy coordinates for the dots or faces
   * 
   * @param visibilityMap
   * @param radius
   * @param x
   * @param y
   * @param z
   * @return number of points
   */
  private int calcScreenPoints(int[] visibilityMap, float radius, int x, int y, int z) {
    int nPoints = 0;
    int i = 0;
    float scaledRadius = viewer.scaleToPerspective(z, radius);
    int iDot = Math.min(visibilityMap.length << 5, screenDotCount);
    while (--iDot >= 0) {
      if (!Dots.getBit(visibilityMap, iDot))
        continue;
      Vector3f vertex = verticesTransformed[iDot];
      if (faceMap != null)
        faceMap[iDot] = i;
      screenCoordinates[i++] = x
          + (int) ((scaledRadius * vertex.x) + (vertex.x < 0 ? -0.5 : 0.5));
      screenCoordinates[i++] = y
          + (int) ((scaledRadius * vertex.y) + (vertex.y < 0 ? -0.5 : 0.5));
      screenCoordinates[i++] = z
          + (int) ((scaledRadius * vertex.z) + (vertex.z < 0 ? -0.5 : 0.5));
      ++nPoints;
    }
    return nPoints;
  }

  /**
   * generic renderer -- dots and geosurface
   * 
   * @param colix
   * @param map
   * @param nPoints
   */
  protected void renderConvex(short colix, int[] map, int nPoints) {
    renderDots(colix, nPoints);
  }

  /**
   * also called by GeoSurface when in motion
   * 
   * @param colix
   * @param nPoints
   */
  protected void renderDots(short colix, int nPoints) {
    g3d.setColix(Graphics3D.getColixTranslucent(colix, false, 0));
    g3d.drawPoints(nPoints, screenCoordinates);
  }
}

