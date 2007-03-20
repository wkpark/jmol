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

  boolean perspectiveDepth;
  int scalePixelsPerAngstrom;
  boolean bondSelectionModeOr;
  int level = 3;
  boolean iShowSolid;
  
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
    Dots dots = (Dots) shape;
    if (dots == null || dots.isCalcOnly)
      return;
    if (!g3d.setColix(Graphics3D.BLACK))
      return;
    render1(dots);
  }

  void render1(Dots dots) {
    //dots.timeBeginExecution = System.currentTimeMillis();

    perspectiveDepth = viewer.getPerspectiveDepth();
    scalePixelsPerAngstrom = (int) viewer.getScalePixelsPerAngstrom();
    bondSelectionModeOr = viewer.getBondSelectionModeOr();

    transform();

    Atom[] atoms = frame.atoms;
    int[][] dotsConvexMaps = dots.dotsConvexMaps;
    short[] colixes = dots.colixes;
    for (int i = dots.dotsConvexMax; --i >= 0;) {
      Atom atom = atoms[i];
      if (!atom.isShapeVisible(myVisibilityFlag) || frame.bsHidden.get(i)
          ||!g3d.isInDisplayRange(atom.screenX, atom.screenY))
        continue;
      if (mapAtoms == null)
        mapAtoms = new int[Geodesic3D.vertexVectors.length];
      calcScreenPoints(dotsConvexMaps[i], dots.getAppropriateRadius(atom),
          atom.screenX, atom.screenY, atom.screenZ);
      if (screenCoordinateCount == 0)
        continue;
      renderConvex(atom, colixes[i], dotsConvexMaps[i]);
    }
    //dots.timeEndExecution = System.currentTimeMillis();
    //Logger.debug("dots rendering time = "+ gs.getExecutionWalltime());

  }
  
  void transform() {
    for (int i = Geodesic3D.getVertexCount(level); --i >= 0;)
      viewer.transformVector(Geodesic3D.vertexVectors[i], verticesTransformed[i]);
  }

  int[] mapAtoms = null; 
  
  void renderConvex(Atom atom, short colix, int[] map) {
      g3d.setColix(Graphics3D.getColixTranslucent(Graphics3D.getColixInherited(colix,
        atom.colixAtom), false, 0));
      g3d.drawPoints(screenCoordinateCount, screenCoordinates);
  }

  int screenLevel;
  int screenDotCount;

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
}

