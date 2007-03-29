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

import org.jmol.g3d.Graphics3D;

class IsosurfaceRenderer extends MeshRenderer {

  boolean iShowTriangles;
  boolean iShowNormals;
  boolean iHideBackground;
  boolean isContoured;
  boolean isPlane;
  boolean isBicolorMap;
  short backgroundColix;
  
  
  void render() {
    iShowTriangles = viewer.getTestFlag3();
    iShowNormals = viewer.getTestFlag4();
    Isosurface isosurface = (Isosurface) shape;
    for (int i = isosurface.meshCount; --i >= 0;)
      render1((IsosurfaceMesh) isosurface.meshes[i]);
  }
  
  boolean render1(IsosurfaceMesh mesh) {
    iHideBackground = (isPlane && mesh.hideBackground);
    if (iHideBackground)
      backgroundColix = Graphics3D.getColix(viewer.getBackgroundArgb());
   isPlane = (mesh.jvxlPlane != null);
   isContoured = mesh.isContoured;
   isBicolorMap = mesh.isBicolorMap;    
    return renderMesh(mesh);
  }  
  
  void renderTriangles(Mesh mesh, boolean fill) {
    int[][] polygonIndexes = mesh.polygonIndexes;
    short[] normixes = mesh.normixes;
    short colix = mesh.colix;
    short[] vertexColixes = mesh.vertexColixes;
    short hideColix = 0;
    try {
      hideColix = vertexColixes[mesh.polygonIndexes[0][0]];
    } catch (Exception e) {
    }
    g3d.setColix(mesh.colix);
    for (int i = mesh.polygonCount; --i >= 0;) {
      //if (!mesh.isPolygonDisplayable(i))
      // continue;
      int[] vertexIndexes = polygonIndexes[i];
      if (vertexIndexes == null)
        continue;
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      short colixA, colixB, colixC;
      if (vertexColixes != null) {
        colixA = vertexColixes[iA];
        colixB = vertexColixes[iB];
        colixC = vertexColixes[iC];
        if (isBicolorMap && (colixA != colixB || colixB != colixC))
          continue;
        //System.out.println("meshrender " + colixA + " " + colixB + " " + colixC);
      } else {
        colixA = colixB = colixC = colix;
      }
      if (iHideBackground) {
        if (colixA == hideColix && colixB == hideColix && colixC == hideColix)
          continue;
        if (colixA == hideColix)
          colixA = backgroundColix;
        if (colixB == hideColix)
          colixB = backgroundColix;
        if (colixC == hideColix)
          colixC = backgroundColix;
      }
      if (fill) {
        if (iShowTriangles) {
          g3d.fillTriangle(screens[iA], colixA, normixes[iA], screens[iB],
              colixB, normixes[iB], screens[iC], colixC, normixes[iC], 0.1f);
        } else {
          try {
            g3d.fillTriangle(screens[iA], colixA, normixes[iA], screens[iB],
                colixB, normixes[iB], screens[iC], colixC, normixes[iC]);
          } catch (Exception e) {
            //TODO  I can't track this one down -- happened once, not second time, with script running to create isosurface plane for slabbing
            System.out.println("MeshRenderer bug?" + e);
          }
        }
        if (iShowNormals)
          renderNormals(mesh);
      } else {
        // FIX ME ... need a drawTriangle routine with multiple colors
        g3d.drawTriangle(screens[iA], screens[iB], screens[iC]);
      }
    }
  }

  final Point3f ptTemp = new Point3f();
  final Point3i ptTempi = new Point3i();

  void renderPoints(Mesh mesh) {
    super.renderPoints(mesh);
    int iFirst = mesh.firstViewableVertex;
    if (!mesh.hasGridPoints)
      return;
    if (iFirst > 0 || !isContoured) {
      g3d.setColix(isTranslucent ? Graphics3D.getColixTranslucent(
          Graphics3D.GRAY, true, 0.5f) : Graphics3D.GRAY);
      if (iFirst > 0)
        for (int i = 0; i < iFirst; i++)
          g3d.fillSphereCentered(2, screens[i]);
      if (!isContoured)
        for (int i = 1; i < vertexCount; i += 3)
          g3d.fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, 1, screens[i],
              screens[i + 1]);
    }
    g3d.setColix(isTranslucent ? Graphics3D.getColixTranslucent(
        Graphics3D.YELLOW, true, 0.5f) : Graphics3D.YELLOW);
    for (int i = 1; i < vertexCount; i += 3)
      g3d.fillSphereCentered(4, screens[i]);

    g3d.setColix(isTranslucent ? Graphics3D.getColixTranslucent(
        Graphics3D.BLUE, true, 0.5f) : Graphics3D.BLUE);
    for (int i = 2; i < vertexCount; i += 3)
      g3d.fillSphereCentered(4, screens[i]);
  }

  void renderNormals(Mesh mesh) {
    //Logger.debug("mesh renderPoints: " + vertexCount);
    if (!g3d.setColix(Graphics3D.WHITE))
      return;
    for (int i = vertexCount; --i >= 0;)
      if (true || vertexValues != null && !Float.isNaN(vertexValues[i]))
        if ((i % 3) == 0) { //investigate vertex normixes
          ptTemp.set(mesh.vertices[i]);
          short n = mesh.normixes[i];
          // -n is an intensity2sided and does not correspond to a true normal index
          if (n > 0) {
            ptTemp.add(g3d.getNormixVector(n));
            viewer.transformPoint(ptTemp, ptTempi);
            g3d.fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, 1,
                screens[i], ptTempi);
          }
        }
  }

}
