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


class PolyhedraRenderer extends ShapeRenderer {

  void render() {
    Polyhedra polyhedra = (Polyhedra)shape;
    Polyhedra.Polyhedron[] polyhedrons = polyhedra.polyhedrons;
    for (int i = polyhedra.polyhedronCount; --i >= 0; )
      render1(polyhedrons[i]);
  }

  void render1(Polyhedra.Polyhedron p) {
    short colix = p.colix;
    if (colix == 0)
      colix = p.centralAtom.colixAtom;
    boolean transparent = p.transparent;
    Atom[] vertices = p.vertices;
    byte[] faces;
    switch(vertices.length) {
    case 6:
      faces = octahedronFaces;
      break;
    case 4:
      faces = tetrahedronFaces;
      break;
    default:
      System.out.println("?Que? vertices.length=" + vertices.length);
      return;
    }
    for (int i = 0; i < faces.length; )
      renderFace(colix, transparent, vertices[faces[i++]],
                 vertices[faces[i++]], vertices[faces[i++]]);
  }

  final static byte[] tetrahedronFaces =
  { 0,1,2, 0,1,3, 0,2,3, 1,2,3 };
  final static byte[] octahedronFaces =
  { 0,1,2, 0,2,3, 0,3,4, 0,4,1, 5,2,1, 5,3,2, 5,4,3, 5,1,4 };

  void renderFace(short colix, boolean transparent,
                  Atom atomA, Atom atomB, Atom atomC) {
    g3d.fillTriangle(colix, transparent, atomA.xyzd, atomB.xyzd, atomC.xyzd);
  }
}
