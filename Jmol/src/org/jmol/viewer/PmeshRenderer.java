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

import org.jmol.g3d.Graphics3D;

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
    Point3i[] screens = viewer.allocTempScreens(vertexCount);
    short colix = mesh.meshColix;
    g3d.setColix(colix);
    for(int i = vertexCount; --i >= 0; )
      viewer.transformPoint(vertices[i], screens[i]);
    for (int i = mesh.polygonCount; --i >= 0; ) {
      int[] vertexIndexes = polygonIndexes[i];
      if (vertexIndexes.length == 3)
        g3d.fillTriangle(colix, mesh.transparent,
                         screens[vertexIndexes[0]],
                         screens[vertexIndexes[1]],
                         screens[vertexIndexes[2]]);
      else if (vertexIndexes.length == 4)
        g3d.fillQuadrilateral(colix, mesh.transparent,
                              screens[vertexIndexes[0]],
                              screens[vertexIndexes[1]],
                              screens[vertexIndexes[2]],
                              screens[vertexIndexes[3]]);
      else
        System.out.println("PmeshRenderer: polygon with > 4 sides");
    }
    viewer.freeTempScreens(screens);
  }
}
