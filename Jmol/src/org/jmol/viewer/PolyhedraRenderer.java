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
import javax.vecmath.Point3i;


class PolyhedraRenderer extends ShapeRenderer {

  void render() {
    Polyhedra polyhedra = (Polyhedra) shape;
    Polyhedra.Polyhedron[] polyhedrons = polyhedra.polyhedrons;
    for (int i = polyhedra.polyhedronCount; --i >= 0;)
      render1(polyhedrons[i]);
  }

  void render1(Polyhedra.Polyhedron p) {
    int displayModelIndex = this.displayModelIndex;
    if (displayModelIndex >= 0 && p.centralAtom.modelIndex != displayModelIndex)
      return;
    if (! p.visible)
      return;
    short colix = Graphics3D.inheritColix(p.polyhedronColix,
                                          p.centralAtom.colixAtom);
    if (p.collapsed)
      renderCollapsed(p, colix);
    else
      renderFlat(p, colix);
  }

  void renderFlat(Polyhedra.Polyhedron p, short colix) {
    Atom[] vertexAtoms = p.vertexAtoms;
    byte[] planes = p.planes;
    short[] normixes = p.normixes;
    int drawEdges = p.edges;

    for (int i = 0, j = 0; j < planes.length; )
      drawFace(colix, drawEdges, normixes[i++], 
               vertexAtoms[planes[j++]],
               vertexAtoms[planes[j++]],
               vertexAtoms[planes[j++]]);
    for (int i = 0, j = 0; j < planes.length; )
      fillFace(colix, normixes[i++],
               vertexAtoms[planes[j++]],
               vertexAtoms[planes[j++]],
               vertexAtoms[planes[j++]]);
  }

  void drawFace(short colix, int drawEdges, short normix,
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

  void renderCollapsed(Polyhedra.Polyhedron p, short colix) {
    Point3i[] screens = viewer.allocTempScreens(p.faceCount);
    viewer.transformPoints(p.collapsedCenters, screens);

    Atom[] vertexAtoms = p.vertexAtoms;
    byte[] planes = p.planes;
    short[] normixes = p.collapsedNormixes;
    int drawEdges = p.edges;

    for (int i = p.faceCount; --i >= 0; )
      renderCollapsedFace(colix, drawEdges, screens[i],
                          vertexAtoms[planes[3*i + 0]],
                          vertexAtoms[planes[3*i + 1]],
                          vertexAtoms[planes[3*i + 2]],
                          normixes[3*i + 0],
                          normixes[3*i + 1],
                          normixes[3*i + 2]);
    viewer.freeTempScreens(screens);
  }

  void renderCollapsedFace(short colix, int drawEdges, Point3i center,
                           Atom atomA, Atom atomB, Atom atomC,
                           short normixA, short normixB, short normixC) {
    drawCollapsed(colix, drawEdges, normixA, center, atomB, atomC);
    drawCollapsed(colix, drawEdges, normixB, center, atomA, atomC);
    drawCollapsed(colix, drawEdges, normixC, center, atomA, atomB);
    fillCollapsed(colix, normixA, center, atomB, atomC);
    fillCollapsed(colix, normixB, center, atomA, atomC);
    fillCollapsed(colix, normixC, center, atomA, atomB);
  }

  void drawCollapsed(short colix, int drawEdges, short normix,
                     Point3i collapsed, Atom atomB, Atom atomC) {
    if (drawEdges == Polyhedra.EDGES_ALL ||
        (drawEdges == Polyhedra.EDGES_FRONT &&
         g3d.isDirectedTowardsCamera(normix))) {
      g3d.drawCylinderTriangle(Graphics3D.getOpaqueColix(colix),
                               collapsed.x, collapsed.y, collapsed.z,
                               atomB.screenX, atomB.screenY, atomB.screenZ,
                               atomC.screenX, atomC.screenY, atomC.screenZ,
                               3);
    }
  }

  void fillCollapsed(short colix, short normix,
                     Point3i collapsed, Atom atomB, Atom atomC) {
    g3d.fillTriangle(colix, normix,
                     collapsed.x, collapsed.y, collapsed.z,
                     atomB.screenX, atomB.screenY, atomB.screenZ,
                     atomC.screenX, atomC.screenY, atomC.screenZ);
  }

}
