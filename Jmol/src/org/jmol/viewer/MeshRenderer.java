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
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;
import org.jmol.g3d.Graphics3D;

abstract class MeshRenderer extends ShapeRenderer {

  Point3f[] vertices;
  Point3i[] screens;
  float[] vertexValues;
  Point3i pt0 = new Point3i();
  Point3i pt3 = new Point3i();
  Vector3f[] transformedVectors;
  boolean doMesh;
  boolean isTranslucent;
  int vertexCount;
  
  boolean render1(Mesh mesh) {
    return renderMesh(mesh);
  }
  
  boolean renderMesh(Mesh mesh) {
    if (mesh == null || mesh.visibilityFlags == 0 || !g3d.setColix(mesh.colix))
      return false;
    isTranslucent = Graphics3D.isColixTranslucent(mesh.colix);
    vertexCount = mesh.vertexCount;
    if (vertexCount == 0)
      return false;
    vertices = mesh.vertices;
    screens = viewer.allocTempScreens(vertexCount);
    vertexValues = mesh.vertexValues;
    transformedVectors = g3d.getTransformedVertexVectors();
    for (int i = vertexCount; --i >= 0;)
      if (vertexValues == null || !Float.isNaN(vertexValues[i])
          || mesh.hasGridPoints) {
        viewer.transformPoint(vertices[i], screens[i]);
        //System.out.println(i + " meshRender " + vertices[i] + screens[i]);
      }
    boolean isDrawPickMode = (mesh.meshType == "draw" && viewer
        .getPickingMode() == JmolConstants.PICKING_DRAW);
    int drawType = mesh.drawType;
    if ((drawType == Mesh.DRAW_CURVE || drawType == Mesh.DRAW_ARROW)
        && vertexCount >= 2) {
      int diameter = (mesh.diameter > 0 ? mesh.diameter : 3);
      for (int i = 0, i0 = 0; i < vertexCount - 1; i++) {
        g3d.fillHermite(5, diameter, diameter, diameter, screens[i0],
            screens[i], screens[i + 1], screens[i
                + (i + 2 == vertexCount ? 1 : 2)]);
        i0 = i;
      }
    }
    switch (drawType) {
    case Mesh.DRAW_ARROW:
      Point3i pt1 = screens[vertexCount - 2];
      Point3i pt2 = screens[vertexCount - 1];
      Vector3f tip = new Vector3f(pt2.x - pt1.x, pt2.y - pt1.y, pt2.z - pt1.z);
      int diameter = (mesh.diameter > 0 ? mesh.diameter : 3);
      float d = tip.length();
      if (d > 0) {
        tip.scale(5 / d);
        pt0.x = pt2.x - (int) Math.floor(4 * tip.x);
        pt0.y = pt2.y - (int) Math.floor(4 * tip.y);
        pt0.z = pt2.z - (int) Math.floor(4 * tip.z);
        pt3.x = pt2.x + (int) Math.floor(tip.x);
        pt3.y = pt2.y + (int) Math.floor(tip.y);
        pt3.z = pt2.z + (int) Math.floor(tip.z);
        g3d.fillCone(Graphics3D.ENDCAPS_FLAT, diameter * 5, pt0, pt3);
      }
      break;
    case Mesh.DRAW_CIRCLE:
      //unimplemented
      break;
    case Mesh.DRAW_CURVE:
      //unnecessary
      break;
    default:
      if (mesh.drawTriangles)
        renderTriangles(mesh, false, false, mesh.frontOnly && !mesh.isTwoSided);
      if (mesh.fillTriangles)
        renderTriangles(mesh, true, mesh.showTriangles, mesh.frontOnly && !mesh.isTwoSided);
      if (mesh.showPoints)
        renderPoints(mesh);
    }
    if (isDrawPickMode) {
      renderHandles(mesh);
    }
    viewer.freeTempScreens(screens);
    return true;
  }

  void renderHandles(Mesh mesh) {
    switch (mesh.drawType) {
    case Mesh.DRAW_POINT:
    case Mesh.DRAW_ARROW:
    case Mesh.DRAW_CURVE:
    case Mesh.DRAW_LINE:
    case Mesh.DRAW_PLANE:
    case Mesh.DRAW_CIRCLE:
    case Mesh.DRAW_MULTIPLE:
      for (int i = mesh.polygonCount; --i >= 0;) {
        if (!mesh.isPolygonDisplayable(i))
          continue;
        int[] vertexIndexes = mesh.polygonIndexes[i];
        if (vertexIndexes == null)
          continue;
        for (int j = vertexIndexes.length; --j >= 0;) {
          int k = vertexIndexes[j];
          g3d.fillScreenedCircleCentered(Graphics3D.GOLD, 10, screens[k].x,
              screens[k].y, screens[k].z);
        }
        break;
      }
    }
  }

  void renderPoints(Mesh mesh) {
    short[] vertexColixes = mesh.vertexColixes;
    int iCount = (mesh.lastViewableVertex > 0 ? mesh.lastViewableVertex + 1
        : vertexCount);
    int iFirst = mesh.firstViewableVertex;
    for (int i = iCount; --i >= iFirst;)
      if (vertexValues != null && !Float.isNaN(vertexValues[i])) {
        if (vertexColixes != null)
          g3d.setColix(vertexColixes[i]);
        g3d.fillSphereCentered(4, screens[i]);
      }
  }

  /**
   * NOTE-- IF YOU CHANGE THIS SET OF PARAMETERS, 
   * YOU MUST CHANGE THEM IN IsosurfaceRenderer.java
   * 
   * @param mesh
   * @param fill
   * @param iShowTriangles
   * @param frontOnly
   */
  void renderTriangles(Mesh mesh, boolean fill, boolean iShowTriangles, boolean frontOnly) {
    int[][] polygonIndexes = mesh.polygonIndexes;
    short[] normixes = mesh.normixes;
    short colix = mesh.colix;
    short[] vertexColixes = mesh.vertexColixes;
    g3d.setColix(mesh.colix);
    for (int i = mesh.polygonCount; --i >= 0;) {
      if (!mesh.isPolygonDisplayable(i))
        continue;
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
      } else {
        colixA = colixB = colixC = colix;
      }
      if (iB == iC) {
        int diameter = (mesh.diameter > 0 ? mesh.diameter : iA == iB ? 6 : 3);
        g3d.setColix(colixA);
        g3d.fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, diameter, screens[iA],
            screens[iB]);
      } else if (vertexIndexes.length == 3) {
        if (frontOnly && transformedVectors[normixes[iA]].z < 0
            && transformedVectors[normixes[iB]].z < 0
            && transformedVectors[normixes[iC]].z < 0)
          continue;
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
        } else if (vertexColixes == null) {
          g3d.drawTriangle(screens[iA], screens[iB], screens[iC], 7);
        } else {
          g3d.drawTriangle(screens[iA], colixA, screens[iB], colixB, screens[iC], colixC, 7);
        }
      } else if (vertexIndexes.length == 4) {
        int iD = vertexIndexes[3];
        short colixD = vertexColixes != null ? vertexColixes[iD] : colix;
        if (frontOnly && transformedVectors[normixes[iA]].z < 0
          && transformedVectors[normixes[iB]].z < 0
          && transformedVectors[normixes[iC]].z < 0
          && transformedVectors[normixes[iD]].z < 0)
        continue;
        if (fill) {
          g3d.fillQuadrilateral(screens[iA], colixA, normixes[iA], screens[iB],
              colixB, normixes[iB], screens[iC], colixC, normixes[iC],
              screens[iD], colixD, normixes[iD]);
        } else
          g3d.drawQuadrilateral(colixA, screens[iA], screens[iB], screens[iC],
              screens[iD]);

        //      } else {
        //      Logger.debug("MeshRenderer: polygon with > 4 sides");
      }
    }
  }

}
