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

import org.jmol.g3d.Graphics3D;

class PolyhedraRenderer extends ShapeRenderer {

  int drawEdges;

  void render() {
    Polyhedra polyhedra = (Polyhedra)shape;
    Polyhedra.Polyhedron[] polyhedrons = polyhedra.polyhedrons;
    drawEdges = polyhedra.drawEdges;
    for (int i = polyhedra.polyhedronCount; --i >= 0; )
      render1(polyhedrons[i]);
  }

  void render1(Polyhedra.Polyhedron p) {
    if (! p.visible)
      return;
    short colix = Graphics3D.inheritColix(p.polyhedronColix,
                                          p.centralAtom.colixAtom);
    Atom[] vertices = p.vertices;
    byte[] faces;
    switch(vertices.length) {
    case 6:
      faces = Polyhedra.octahedronFaces;
      break;
    case 4:
      faces = Polyhedra.tetrahedronFaces;
      break;
    default:
      System.out.println("?Que? vertices.length=" + vertices.length);
      return;
    }

    for (int i = 0, j = 0; j < faces.length; )
      drawFace(colix, p.normixes[i++],
               vertices[faces[j++]],
               vertices[faces[j++]],
               vertices[faces[j++]]);
    for (int i = 0, j = 0; j < faces.length; )
      fillFace(colix, p.normixes[i++],
               vertices[faces[j++]],
               vertices[faces[j++]],
               vertices[faces[j++]]);
  }

  void drawFace(short colix, short normix,
                Atom atomA, Atom atomB, Atom atomC) {
    if (drawEdges == Polyhedra.EDGES_ALL ||
        (drawEdges == Polyhedra.EDGES_FRONT &&
         g3d.isDirectedTowardsCamera(normix))) {
      g3d.drawTriangle(Graphics3D.getOpaqueColix(colix),
                       atomA.xyzd, atomB.xyzd, atomC.xyzd);
    }
  }

  void fillFace(short colix, short normix,
                  Atom atomA, Atom atomB, Atom atomC) {
    g3d.fillTriangle(colix, normix, atomA.xyzd, atomB.xyzd, atomC.xyzd);
  }
}
