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
import org.jmol.util.Logger;

class IsosurfaceRenderer extends MeshRenderer {

  private boolean iShowNormals;
  private boolean iHideBackground;
  private boolean isPlane;
  private boolean isBicolorMap;
  private short backgroundColix;
  private boolean isTranslucent;
  private int nError = 0;
  private float[] vertexValues;

  IsosurfaceMesh imesh;

  void render() {
    iShowNormals = viewer.getTestFlag4();
    Isosurface isosurface = (Isosurface) shape;
    for (int i = isosurface.meshCount; --i >= 0;)
      render1(imesh = (IsosurfaceMesh) isosurface.meshes[i]);
  }

  void transform() {
    vertexValues = imesh.vertexValues;
    for (int i = vertexCount; --i >= 0;) {
      if (vertexValues == null || !Float.isNaN(vertexValues[i])
          || imesh.hasGridPoints) {
        viewer.transformPoint(vertices[i], screens[i]);
        //System.out.println(i + " meshRender " + vertices[i] + screens[i]);
      }
    }
  }
  
  void render2() {
    isTranslucent = Graphics3D.isColixTranslucent(imesh.colix);
    iHideBackground = (isPlane && imesh.hideBackground);
    if (iHideBackground)
      backgroundColix = Graphics3D.getColix(viewer.getBackgroundArgb());
    isPlane = (imesh.jvxlData.jvxlPlane != null);
    isBicolorMap = imesh.jvxlData.isBicolorMap;
    super.render2();
  }
  
  private final Point3f ptTemp = new Point3f();
  private final Point3i ptTempi = new Point3i();

  void renderPoints() {
    int incr = imesh.vertexIncrement;
    for (int i = (!imesh.hasGridPoints || imesh.firstRealVertex < 0 ? 0 : imesh.firstRealVertex); i < vertexCount; i += incr) {
      if (vertexValues != null && Float.isNaN(vertexValues[i]) || frontOnly
          && transformedVectors[normixes[i]].z < 0)
        continue;
      if (imesh.vertexColixes != null)
        g3d.setColix(imesh.vertexColixes[i]);
      g3d.fillSphereCentered(4, screens[i]);
    }
    if (incr != 3)
      return;
    g3d.setColix(isTranslucent ? Graphics3D.getColixTranslucent(
        Graphics3D.GRAY, true, 0.5f) : Graphics3D.GRAY);
    for (int i = 1; i < vertexCount; i += 3)
      g3d.fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, 1, screens[i],
          screens[i + 1]);
    g3d.setColix(isTranslucent ? Graphics3D.getColixTranslucent(
        Graphics3D.YELLOW, true, 0.5f) : Graphics3D.YELLOW);
    for (int i = 1; i < vertexCount; i += 3)
      g3d.fillSphereCentered(4, screens[i]);
    g3d.setColix(isTranslucent ? Graphics3D.getColixTranslucent(
        Graphics3D.BLUE, true, 0.5f) : Graphics3D.BLUE);
    for (int i = 2; i < vertexCount; i += 3)
      g3d.fillSphereCentered(4, screens[i]);
  }

  void renderTriangles(boolean fill, boolean iShowTriangles) {
    int[][] polygonIndexes = imesh.polygonIndexes;
    short colix = imesh.colix;
    short[] vertexColixes = imesh.vertexColixes;
    short hideColix = 0;
    try {
      hideColix = vertexColixes[imesh.polygonIndexes[0][0]];
    } catch (Exception e) {
    }
    g3d.setColix(imesh.colix);
    //System.out.println("Isosurface renderTriangle polygoncount = "
    //  + mesh.polygonCount + " screens: " + screens.length + " normixes: "
    //+ normixes.length);
    // two-sided means like a plane, with no front/back distinction
    for (int i = imesh.polygonCount; --i >= 0;) {
      //if (!mesh.isPolygonDisplayable(i))
      // continue;
      //if (i !=20)
        //continue;
      int[] vertexIndexes = polygonIndexes[i];
      if (vertexIndexes == null)
        continue;
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      
      short nA = normixes[iA];
      short nB = normixes[iB];
      short nC = normixes[iC];
/*
      if ((vertexIndexes[3] & 1) == 1)
        System.out.println(iA + " " + vertices[iA] + ", "+iB + " "+ vertices[iB]);
      if ((vertexIndexes[3] & 2) == 2)
        System.out.println(iB + " " + vertices[iB] + ", "+iC + " "+ vertices[iC]);
      if ((vertexIndexes[3] & 4) == 4)
        System.out.println(iC + " " + vertices[iC] + ", "+iA + " "+ vertices[iA]);
*/
           
      //if (frontOnly && (nA < 0 || nB < 0 || nC < 0))
      //frontOnly = false;
    //if (frontOnly)
      
      if (frontOnly && transformedVectors[nA].z < 0
          && transformedVectors[nB].z < 0 && transformedVectors[nC].z < 0)
        continue;
      short colixA, colixB, colixC;
      if (vertexColixes != null) {
        colixA = vertexColixes[iA];
        colixB = vertexColixes[iB];
        colixC = vertexColixes[iC];
        if (isBicolorMap && (colixA != colixB || colixB != colixC))
          continue;
        //System.out.println("meshrender " + iA + " " + iB + " " + iC + " " + colixA + " " + colixB + " " + colixC);
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
          g3d.fillTriangle(screens[iA], colixA, nA, screens[iB], colixB, nB,
              screens[iC], colixC, nC, 0.1f);
        } else {
          try {
            g3d.fillTriangle(screens[iA], colixA, nA, screens[iB], colixB, nB,
                screens[iC], colixC, nC);
          } catch (Exception e) {
            if (nError++ < 1) {
              Logger.warn("IsosurfaceRenderer -- competing thread bug?\n", e);
            }
          }
        }
        if (iShowNormals)
          renderNormals();
      } else {
        int check = vertexIndexes[3];
        if (check == 0)
          continue;
        if (vertexColixes == null)
          g3d.drawTriangle(screens[iA], screens[iB], screens[iC], check);
        else
          g3d.drawTriangle(screens[iA], colixA, screens[iB], colixB,
              screens[iC], colixC, check);
      }
    }
  }

  private void renderNormals() {
    //Logger.debug("mesh renderPoints: " + vertexCount);
    if (!g3d.setColix(Graphics3D.WHITE))
      return;
    for (int i = vertexCount; --i >= 0;) {
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

}
