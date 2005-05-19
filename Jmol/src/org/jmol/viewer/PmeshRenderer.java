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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;


class PmeshRenderer extends ShapeRenderer {

  void render() {
    Pmesh pmesh = (Pmesh)shape;
    for (int i = pmesh.meshCount; --i >= 0; )
      render1(pmesh.meshes[i]);
  }

  void render1(Pmesh.Mesh mesh) {
    if (! mesh.visible)
      return;
    int vertexCount = mesh.vertexCount;
    int[][] polygonIndexes = mesh.polygonIndexes;
    Point3f[] vertices = mesh.vertices;
    short[] normixes = mesh.normixes;
    Point3i[] screens = viewer.allocTempScreens(vertexCount);
    short colix = mesh.meshColix;
    g3d.setColix(colix);
    for(int i = vertexCount; --i >= 0; )
      viewer.transformPoint(vertices[i], screens[i]);
    for (int i = mesh.polygonCount; --i >= 0; ) {
      int[] vertexIndexes = polygonIndexes[i];
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      if (vertexIndexes.length == 3) {
        g3d.fillTriangle(colix,
                         screens[iA], normixes[iA],
                         screens[iB], normixes[iB],
                         screens[iC], normixes[iC]);
      } else if (vertexIndexes.length == 4) {
        int iD = vertexIndexes[3];
        g3d.fillQuadrilateral(colix,
                              screens[iA], normixes[iA],
                              screens[iB], normixes[iB],
                              screens[iC], normixes[iC],
                              screens[iD], normixes[iD]);
      } else {
        System.out.println("PmeshRenderer: polygon with > 4 sides");
      }
    }
    viewer.freeTempScreens(screens);
  }
}
