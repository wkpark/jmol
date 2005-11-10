/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

abstract class MeshRenderer extends ShapeRenderer {

  void render1(Mesh mesh) {
    if (! mesh.visible)
      return;
    int vertexCount = mesh.vertexCount;
    if (vertexCount == 0)
      return;
    Point3f[] vertices = mesh.vertices;
    Point3i[] screens = viewer.allocTempScreens(vertexCount);
    for(int i = vertexCount; --i >= 0; )
      viewer.transformPoint(vertices[i], screens[i]);

    if (mesh.showPoints)
      renderPoints(mesh, screens, vertexCount);
    if (mesh.drawTriangles)
      renderTriangles(mesh, screens, false);
    if (mesh.fillTriangles)
      renderTriangles(mesh, screens, true);

    viewer.freeTempScreens(screens);
  }

  void renderPoints(Mesh mesh, Point3i[] screens, int vertexCount) {
    short colix = mesh.colix;
    short[] vertexColixes = mesh.vertexColixes;
    for (int i = vertexCount; --i >= 0; )
      g3d.fillSphereCentered(vertexColixes != null ? vertexColixes[i] : colix,
                             4, screens[i]);
  }

  void renderTriangles(Mesh mesh, Point3i[] screens, boolean fill) {
    int[][] polygonIndexes = mesh.polygonIndexes;
    short[] normixes = mesh.normixes;
    short colix = mesh.colix;
    short[] vertexColixes = mesh.vertexColixes;
    for (int i = mesh.polygonCount; --i >= 0; ) {
      int[] vertexIndexes = polygonIndexes[i];
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      short colixA, colixB, colixC;
      if (vertexColixes != null) {
        colixA = vertexColixes[iA];
        colixB = vertexColixes[iB];
        colixC = vertexColixes[iC];
      } else {
        colixA = colixB = colixC = colix;
      }
      if (vertexIndexes.length == 3) {
        if (fill)
          g3d.fillTriangle(screens[iA], colixA, normixes[iA],
                           screens[iB], colixB, normixes[iB],
                           screens[iC], colixC, normixes[iC]);
        else // FIX ME ... need a drawTriangle routine with multiple colors
          g3d.drawTriangle(colixA, screens[iA], screens[iB], screens[iC]);
          
      } else if (vertexIndexes.length == 4) {
        int iD = vertexIndexes[3];
        short colixD = vertexColixes != null ? vertexColixes[iD] : colix;
        if (fill)
          g3d.fillQuadrilateral(screens[iA], colixA, normixes[iA],
                                screens[iB], colixB, normixes[iB],
                                screens[iC], colixC, normixes[iC],
                                screens[iD], colixD, normixes[iD]);
        else
          g3d.drawQuadrilateral(colixA, screens[iA],
                                screens[iB], screens[iC], screens[iD]);

      } else {
        System.out.println("PmeshRenderer: polygon with > 4 sides");
      }
    }
  }
}
