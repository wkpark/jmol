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

import org.jmol.g3d.Graphics3D;

class PolyhedraRenderer extends ShapeRenderer {

  int drawEdges;

  void render() {
    Polyhedra polyhedra = (Polyhedra) shape;
    Polyhedra.Polyhedron[] polyhedrons = polyhedra.polyhedrons;
    drawEdges = polyhedra.drawEdges;
    for (int i = polyhedra.polyhedronCount; --i >= 0;)
      render1(polyhedrons[i]);
  }

  void render1(Polyhedra.Polyhedron p) {
    if (! p.visible)
      return;
    short colix = Graphics3D.inheritColix(p.polyhedronColix,
                                          p.centralAtom.colixAtom);
    Atom[] vertices = p.vertices;
    byte[] planes;

    planes = p.planes;
    for (int i = vertices.length; --i >= 0;) {
      if (vertices[i].isSimple)
        vertices[i].transform(viewer);
    }

    for (int i = 0, j = 0; j < planes.length; ) {
      drawFace(colix, p.normixes[i++],
               vertices[planes[j++]],
               vertices[planes[j++]],
               vertices[planes[j++]]);
    }
    
    for (int i = 0, j = 0; j < planes.length; )
      fillFace(colix, p.normixes[i++],
               vertices[planes[j++]],
               vertices[planes[j++]],
               vertices[planes[j++]]);
  }

  void drawFace(short colix, short normix,
                Atom atomA, Atom atomB, Atom atomC) {
    if (drawEdges == Polyhedra.EDGES_ALL ||
        (drawEdges == Polyhedra.EDGES_FRONT &&
         g3d.isDirectedTowardsCamera(normix))) {
      g3d.drawCylinderTriangle(Graphics3D.getOpaqueColix(colix),
                               atomA.screenX, atomA.screenY, atomA.screenZ,
                               atomB.screenX, atomB.screenY, atomB.screenZ,
                               atomC.screenX, atomC.screenY, atomC.screenZ,
                               3);
    }
  }

  void fillFace(short colix, short normix,
                  Atom atomA, Atom atomB, Atom atomC) {
    g3d.fillTriangle(colix, normix,
                     atomA.screenX, atomA.screenY, atomA.screenZ,
                     atomB.screenX, atomB.screenY, atomB.screenZ,
                     atomC.screenX, atomC.screenY, atomC.screenZ);
  }
}
