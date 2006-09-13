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

  int myVisibilityFlag;    

  void initRenderer() {
    myVisibilityFlag = Viewer.getShapeVisibilityFlag(JmolConstants.SHAPE_POLYHEDRA);
  }
  
  void render() {
    Polyhedra polyhedra = (Polyhedra) shape;
    Polyhedra.Polyhedron[] polyhedrons = polyhedra.polyhedrons;
    drawEdges = polyhedra.drawEdges;
    for (int i = polyhedra.polyhedronCount; --i >= 0;)
      render1(polyhedrons[i]);
  }

  void render1(Polyhedra.Polyhedron p) {
    if ((p.visibilityFlags & myVisibilityFlag) == 0)
      return;
    /*Logger.debug("\npolynomialrenderer" + p.planeCount+" "+p.centralAtom.getIdentity());
     for (int i = 0; i < p.vertices.length; i++) {
       Logger.debug("atom "+p.vertices[i].getIdentity());
     }
     */
    short colix = Graphics3D.inheritColix(p.polyhedronColix,
        p.centralAtom.colixAtom);
    Atom[] vertices = p.vertices;
    byte[] planes;

    planes = p.planes;
    for (int i = vertices.length; --i >= 0;) {
      if (vertices[i].isSimple)
        vertices[i].transform(viewer);
    }

    boolean isAll = (drawEdges == Polyhedra.EDGES_ALL);
    boolean isFrontOnly = (drawEdges == Polyhedra.EDGES_FRONT);

    // no edges to new points when not collapsed 
    for (int i = 0, j = 0; j < planes.length;)
      if (true || p.collapsed || planes[j] < p.ptCenter && planes[j + 1] < p.ptCenter
          && planes[j + 2] < p.ptCenter) {
        drawFace(colix, p.normixes[i++], vertices[planes[j++]],
            vertices[planes[j++]], vertices[planes[j++]], isAll, isFrontOnly);
      } else {
        i++;
        j += 3;
      }

    for (int i = 0, j = 0; j < planes.length;)
      fillFace(colix, p.normixes[i++], vertices[planes[j++]],
          vertices[planes[j++]], vertices[planes[j++]]);
  }

  void drawFace(short colix, short normix,
                Atom atomA, Atom atomB, Atom atomC, boolean isAll, boolean isFrontOnly) {
    if (isAll || isFrontOnly && g3d.isDirectedTowardsCamera(normix)) {
      g3d.drawCylinderTriangle(Graphics3D.getOpaqueColix(colix),
                               atomA.screenX, atomA.screenY, atomA.screenZ,
                               atomB.screenX, atomB.screenY, atomB.screenZ,
                               atomC.screenX, atomC.screenY, atomC.screenZ,
                               3);
    }
  }

  void fillFace(short colix, short normix,
                  Atom atomA, Atom atomB, Atom atomC) {
    /*
     * Logger.debug("fillFace "+atomA.screenX+" "+ atomA.screenY+" "+ atomA.screenZ+" "+
        atomB.screenX+" "+ atomB.screenY+" "+ atomB.screenZ+" "+
        atomC.screenX+" "+ atomC.screenY+" "+ atomC.screenZ);
    */
    g3d.fillTriangle(colix, normix,
                     atomA.screenX, atomA.screenY, atomA.screenZ,
                     atomB.screenX, atomB.screenY, atomB.screenZ,
                     atomC.screenX, atomC.screenY, atomC.screenZ);
  }
}
