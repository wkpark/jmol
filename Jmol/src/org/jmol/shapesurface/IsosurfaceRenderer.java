/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-25 09:53:35 -0500 (Wed, 25 Apr 2007) $
 * $Revision: 7491 $
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
package org.jmol.shapesurface;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jmol.g3d.Graphics3D;
import org.jmol.shape.MeshRenderer;
import org.jmol.util.Logger;

public class IsosurfaceRenderer extends MeshRenderer {

  private boolean iShowNormals;
  protected boolean iHideBackground;
  protected boolean isBicolorMap;
  protected short backgroundColix;
  protected int nError = 0;
  protected float[] vertexValues;

  protected IsosurfaceMesh imesh;

  protected void render() {
    iShowNormals = viewer.getTestFlag4();
    Isosurface isosurface = (Isosurface) shape;
    for (int i = isosurface.meshCount; --i >= 0;)
      render1(imesh = (IsosurfaceMesh) isosurface.meshes[i]);
  }

  protected void transform() {
    vertexValues = imesh.vertexValues;
    for (int i = vertexCount; --i >= 0;) {
      if (vertexValues == null || !Float.isNaN(vertexValues[i])
          || imesh.hasGridPoints) {
        viewer.transformPoint(vertices[i], screens[i]);
      }
    }
  }
  
  protected void render2() {
    isBicolorMap = imesh.jvxlData.isBicolorMap;
    super.render2();
  }
  
  private final Point3f ptTemp = new Point3f();
  private final Point3i ptTempi = new Point3i();

  protected void renderPoints() {
    int incr = imesh.vertexIncrement;
    int diam = 4;
    for (int i = (!imesh.hasGridPoints || imesh.firstRealVertex < 0 ? 0 : imesh.firstRealVertex); i < vertexCount; i += incr) {
      if (vertexValues != null && Float.isNaN(vertexValues[i]) || frontOnly
          && transformedVectors[normixes[i]].z < 0)
        continue;
      if (imesh.vertexColixes != null)
        g3d.setColix(imesh.vertexColixes[i]);
      g3d.fillSphereCentered(diam, screens[i]);
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

  protected void renderTriangles(boolean fill, boolean iShowTriangles) {
    int[][] polygonIndexes = imesh.polygonIndexes;
    colix = imesh.colix;
    short[] vertexColixes = imesh.vertexColixes;
    g3d.setColix(imesh.colix);
    boolean generateSet = (isGenerator && fill);
    if (generateSet) {
      frontOnly = false;
      bsFaces.clear();
    }
    //System.out.println("Isosurface renderTriangle polygoncount = "
    //  + mesh.polygonCount + " screens: " + screens.length + " normixes: "
    //+ normixes.length);
    // two-sided means like a plane, with no front/back distinction
    for (int i = imesh.polygonCount; --i >= 0;) {
//      if (i > 500)
  //      continue;
      int[] vertexIndexes = polygonIndexes[i];
      if (vertexIndexes == null)
        continue;
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      short nA = normixes[iA];
      short nB = normixes[iB];
      short nC = normixes[iC];
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
      } else {
        colixA = colixB = colixC = colix;
      }
      if (fill) {
        if (generateSet) {
          bsFaces.set(i);
          continue;
        }
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
    if (generateSet)
     renderExport();
  }

  private void renderNormals() {
    //Logger.debug("mesh renderPoints: " + vertexCount);
    if (!g3d.setColix(Graphics3D.WHITE))
      return;
    for (int i = vertexCount; --i >= 0;) {
      if (vertexValues != null && !Float.isNaN(vertexValues[i]))
        //if ((i % 3) == 0) { //investigate vertex normixes
          ptTemp.set(mesh.vertices[i]);
          short n = mesh.normixes[i];
          // -n is an intensity2sided and does not correspond to a true normal index
          if (n >= 0) {
            ptTemp.add(g3d.getNormixVector(n));
            viewer.transformPoint(ptTemp, ptTempi);
            g3d.fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, 1,
                screens[i], ptTempi);
          }
        //}
    }
  }

}
