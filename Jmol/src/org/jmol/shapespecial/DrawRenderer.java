/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-02-25 11:44:18 -0600 (Sat, 25 Feb 2006) $
 * $Revision: 4528 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development
 *
 * Contact: miguel@jmol.org, jmol-developers@lists.sourceforge.net
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
package org.jmol.shapespecial;


import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.shape.MeshRenderer;
import org.jmol.viewer.JmolConstants;

public class DrawRenderer extends MeshRenderer {

  private int drawType;
  private DrawMesh dmesh;

  protected void render() {
    /*
     * Each drawn object, draw.meshes[i], may consist of several polygons, one
     * for each MODEL FRAME. Or, it may be "fixed" and only contain one single
     * polygon.
     * 
     */
    imageFontScaling = viewer.getImageFontScaling();
    Draw draw = (Draw) shape;
    for (int i = draw.meshCount; --i >= 0;)
      if (render1(dmesh = (DrawMesh) draw.meshes[i]))
        renderInfo();
  }
  
  protected boolean isPolygonDisplayable(int i) {
      return Draw.isPolygonDisplayable(dmesh, i) 
          && (dmesh.modelFlags == null || dmesh.modelFlags.get(i)); 
  }
  
  private Point3f[] controlHermites;
  
  protected void render2() {
    boolean isDrawPickMode = (viewer.getPickingMode() == JmolConstants.PICKING_DRAW);
    drawType = dmesh.drawType;
    diameter = dmesh.diameter;
    width = dmesh.width;
    boolean isCurved = ((drawType == JmolConstants.DRAW_CURVE || drawType == JmolConstants.DRAW_ARROW) && vertexCount >= 2);
    if (width > 0 && isCurved) {
      pt1f.set(0, 0, 0);
      for (int i = 0; i < vertexCount; i++)
        pt1f.add(vertices[i]);
      pt1f.scale(1f / vertexCount);
      viewer.transformPoint(pt1f, pt1i);
      diameter = viewer.scaleToScreen(pt1i.z, (int) (width * 1000));
      if (diameter == 0)
        diameter = 1;
    }
    int tension = 5;
    switch (drawType) {
    default:
      super.render2();
      break;
    case JmolConstants.DRAW_CIRCLE:
    case JmolConstants.DRAW_CIRCULARPLANE:
      viewer.transformPoint(vertices[0], pt1i);
      if (diameter == 0 && width == 0)
        width = 1.0f;
      if (width > 0)
        diameter = viewer.scaleToScreen(pt1i.z, (int) (width * 1000));
      if (diameter > 0 && (mesh.drawTriangles || mesh.fillTriangles))
        g3d.drawCircleCentered(colix, diameter, pt1i.x, pt1i.y, pt1i.z,
            mesh.fillTriangles);
      break;
    case JmolConstants.DRAW_CURVE:
      //unnecessary
      break;
    case JmolConstants.DRAW_ARROW:
      Vector3f tip = new Vector3f();
      float d;
      float fScale = dmesh.drawArrowScale;
      if (fScale == 0)
        fScale = viewer.getDefaultDrawArrowScale();
      if (fScale <= 0)
        fScale = 0.5f;
      int nHermites = 5;
      if (controlHermites == null || controlHermites.length < nHermites + 1) {
        controlHermites = new Point3f[nHermites + 1];
      }
      if (vertexCount == 2) {
        if (controlHermites[nHermites - 1] == null) {
          controlHermites[nHermites - 2] = new Point3f(vertices[0]);
          controlHermites[nHermites - 1] = new Point3f(vertices[1]);
        } else {
          controlHermites[nHermites - 2].set(vertices[0]);
          controlHermites[nHermites - 1].set(vertices[1]);
        }
      } else {
        Graphics3D.getHermiteList(tension, vertices[vertexCount - 3],
            vertices[vertexCount - 2], vertices[vertexCount - 1],
            vertices[vertexCount - 1], vertices[vertexCount - 1],
            controlHermites, 0, nHermites);
      }
      pt1f.set(controlHermites[nHermites - 2]);
      pt2f.set(controlHermites[nHermites - 1]);
      tip.set(pt2f);
      tip.sub(pt1f);
      d = tip.length();
      if (d == 0)
        break;
      tip.scale(fScale / d / 5);
      pt2f.add(tip);
      tip.scale(5);
      pt1f.set(pt2f);
      pt1f.sub(tip);
      viewer.transformPoint(pt2f, pt2i);
      viewer.transformPoint(pt1f, pt1i);
      tip.set(pt2i.x - pt1i.x, pt2i.y - pt1i.y, pt2i.z - pt1i.z);
      if (pt2i.z == 1 || pt1i.z == 1) //slabbed
        break;
      int headDiameter;
      if (diameter > 0) {
        headDiameter = diameter * 5;
      } else {
        headDiameter = (int) (tip.length() * .5);
        diameter = headDiameter / 5;
      }
      if (diameter < 1)
        diameter = 1;
      if (headDiameter > 2)
        g3d.fillCone(Graphics3D.ENDCAPS_FLAT, headDiameter, pt1i, pt2i);
      break;
    }
    if (diameter == 0)
      diameter = 3;
    if (isCurved) {
      for (int i = 0, i0 = 0; i < vertexCount - 1; i++) {
        g3d.fillHermite(tension, diameter, diameter, diameter, screens[i0],
            screens[i], screens[i + 1], screens[i
                + (i + 2 == vertexCount ? 1 : 2)]);
        i0 = i;
      }
    }
    if (isDrawPickMode && !isGenerator) {
      renderHandles();
    }
  }
  
  private void renderHandles() {
    int diameter = (int) (10 * imageFontScaling);
    switch (drawType) {
    case JmolConstants.DRAW_NONE:
    case JmolConstants.DRAW_TRIANGLE:
      return;
    default:
      for (int i = dmesh.polygonCount; --i >= 0;) {
        if (!isPolygonDisplayable(i))
          continue;
        int[] vertexIndexes = dmesh.polygonIndexes[i];
        if (vertexIndexes == null)
          continue;
        for (int j = vertexIndexes.length; --j >= 0;) {
          int k = vertexIndexes[j];
          g3d.fillScreenedCircleCentered(Graphics3D.GOLD, diameter, screens[k].x,
              screens[k].y, screens[k].z);
        }
        break;
      }
    }
  }
  
  private void renderInfo() {
    if (mesh.title == null || viewer.getDrawHover()
        || !g3d.setColix(viewer.getColixBackgroundContrast()))
      return;
    for (int i = dmesh.polygonCount; --i >= 0;)
      if (isPolygonDisplayable(i)) {
        //just the first line of the title -- nothing fancy here.
        byte fid = g3d.getFontFid(14 * imageFontScaling);
        g3d.setFont(fid);
        String s = mesh.title[i < mesh.title.length ? i : mesh.title.length - 1];
        int pt = 0;
        if (s.length() > 1 && s.charAt(0) == '>') {
          pt = dmesh.polygonIndexes[i].length - 1;
          s = s.substring(1);
        }
        viewer.transformPoint(vertices[dmesh.polygonIndexes[i][pt]], pt1i);
        int offset = (int) (5 * imageFontScaling);
        g3d.drawString(s, null, pt1i.x + offset, pt1i.y - offset,
            pt1i.z, pt1i.z);
        break;
      }
  }
  
}
