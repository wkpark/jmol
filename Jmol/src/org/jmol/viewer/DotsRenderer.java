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
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;


class DotsRenderer extends ShapeRenderer {

  boolean perspectiveDepth;
  int scalePixelsPerAngstrom;
  boolean bondSelectionModeOr;
  boolean isInMotion;
  boolean iShowSolid;
  int nPoints;

  int level = 3;
  
  Vector3f[] verticesTransformed;
  int screenCoordinateCount;
  int[] screenCoordinates;
  
  void initRenderer() {
    int nVertices = Geodesic3D.getVertexCount(level);
    verticesTransformed = new Vector3f[nVertices];
    for (int i = nVertices; --i >= 0; )
      verticesTransformed[i] = new Vector3f();
    screenCoordinates = new int[3 * nVertices];
  }

  void render() {
    perspectiveDepth = viewer.getPerspectiveDepth();
    scalePixelsPerAngstrom = (int) viewer.getScalePixelsPerAngstrom();
    bondSelectionModeOr = viewer.getBondSelectionModeOr();
    nPoints = 0;
    Dots dots = (Dots) shape;
    if (dots == null || dots.isCalcOnly)
      return;
    transform();
    isInMotion = (viewer.getInMotion() && dots.dotsConvexMax > 100);
    iShowSolid = dots.isSurface && !isInMotion;
    if (iShowSolid && !g3d.setColix(dots.surfaceColix) ||
        !iShowSolid && !g3d.setColix(Graphics3D.BLACK))
      return;
    dots.timeBeginExecution = System.currentTimeMillis();

    Atom[] atoms = frame.atoms;
    int[][] dotsConvexMaps = dots.dotsConvexMaps;
    short[] colixes = dots.colixes;
    for (int i = dots.dotsConvexMax; --i >= 0;) {
      Atom atom = atoms[i];
      if (!atom.isShapeVisible(myVisibilityFlag) || frame.bsHidden.get(i)
          ||!g3d.isInDisplayRange(atom.screenX, atom.screenY))
        continue;
      renderConvex(dots, atom, colixes[i], dotsConvexMaps[i]);
    }
    //System.out.println("dotsrenderer n=" + nPoints);
    dots.timeEndExecution = System.currentTimeMillis();
    //Logger.debug("dots rendering time = "+ dots.getExecutionWalltime());
  }

  void transform() {
    for (int i = Geodesic3D.getVertexCount(level); --i >= 0;)
      viewer.transformVector(Geodesic3D.vertexVectors[i], verticesTransformed[i]);
  }

  int[] mapAtoms = null; 
  
  void renderConvex(Dots dots, Atom atom, short colix, int[] visibilityMap) {
    if (mapAtoms == null)
      mapAtoms = new int[Geodesic3D.vertexVectors.length];
    //System.out.println("dotsrend atom " + atom.atomIndex + " " + geodesic.screenDotCount);
    calcScreenPoints(visibilityMap, dots.getAppropriateRadius(atom),
        atom.screenX, atom.screenY, atom.screenZ);
    if (screenCoordinateCount == 0)
      return;
    // dots are never translucent; geoSurface may be translucent
    if (!iShowSolid)
      g3d.setColix(Graphics3D.getColixTranslucent(Graphics3D.getColixInherited(colix,
        atom.colixAtom), false, 0));
    if (iShowSolid)
      renderGeodesicFragment(visibilityMap, mapAtoms, screenDotCount);
    else {
      g3d.drawPoints(screenCoordinateCount, screenCoordinates);
      nPoints += screenCoordinateCount;
    }
    
  }

  private int screenLevel;
  private int screenDotCount;

  void calcScreenPoints(int[] visibilityMap, float radius, int x, int y, int z) {
    float scaledRadius;
    
    int dotCount;
    if (iShowSolid) {
      dotCount = 642;
      screenLevel = 3;
    } else if (scalePixelsPerAngstrom > 5) {
      dotCount = 42;
      screenLevel = 1;
      if (scalePixelsPerAngstrom > 10) {
        dotCount = 162;
        screenLevel = 2;
        if (scalePixelsPerAngstrom > 20) {
          dotCount = 642;
          screenLevel = 3;
          //      if (scalePixelsPerAngstrom > 32) {
          //          screenLevel = 4; //untested
          //          dotCount = 2562;
          //      }
        }
      }
    } else {
      dotCount = 12;
      screenLevel = 0;
    }
    screenDotCount = dotCount;
    scaledRadius = viewer.scaleToPerspective(z, radius);
    int icoordinates = 0;
    int iDot = visibilityMap.length << 5;
    screenCoordinateCount = 0;
    if (iDot > dotCount)
      iDot = dotCount;
    while (--iDot >= 0) {
      if (!Dots.getBit(visibilityMap, iDot))
        continue;
      Vector3f vertex = verticesTransformed[iDot];
      if (mapAtoms != null)
        mapAtoms[iDot] = icoordinates;
      screenCoordinates[icoordinates++] = x
          + (int) ((scaledRadius * vertex.x) + (vertex.x < 0 ? -0.5 : 0.5));
      screenCoordinates[icoordinates++] = y
          + (int) ((scaledRadius * vertex.y) + (vertex.y < 0 ? -0.5 : 0.5));
      screenCoordinates[icoordinates++] = z
          + (int) ((scaledRadius * vertex.z) + (vertex.z < 0 ? -0.5 : 0.5));
      ++screenCoordinateCount;
    }
  }

  Point3i facePt1 = new Point3i();
  Point3i facePt2 = new Point3i();
  Point3i facePt3 = new Point3i();
  
  void renderGeodesicFragment(int[] points, int[] map, int dotCount) {
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

