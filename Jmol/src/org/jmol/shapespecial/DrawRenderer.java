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


import javax.vecmath.Point3i;
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
    Draw draw = (Draw) shape;
    for (int i = draw.meshCount; --i >= 0;) {
      render1(dmesh = (DrawMesh) draw.meshes[i]);
      renderInfo();
    }
  }
  
  protected boolean isPolygonDisplayable(int i) {
      return (dmesh.polygonIndexes[i].length > 0 
          && (dmesh.modelFlags == null || dmesh.modelFlags[i] != 0)); 
  }
  
  private final Point3i pt0 = new Point3i();
  private final Point3i pt3 = new Point3i();

  protected void render2() {
    boolean isDrawPickMode = (viewer.getPickingMode() == JmolConstants.PICKING_DRAW);
    drawType = dmesh.drawType;
    if ((drawType == DrawMesh.DRAW_CURVE || drawType == DrawMesh.DRAW_ARROW)
        && vertexCount >= 2) {
      int diameter = (dmesh.diameter > 0 ? dmesh.diameter : 3);
      for (int i = 0, i0 = 0; i < vertexCount - 1; i++) {
        g3d.fillHermite(5, diameter, diameter, diameter, screens[i0],
            screens[i], screens[i + 1], screens[i
                + (i + 2 == vertexCount ? 1 : 2)]);
        i0 = i;
      }
    }
    switch (drawType) {
    case DrawMesh.DRAW_ARROW:
      Point3i pt1 = screens[vertexCount - 2];
      Point3i pt2 = screens[vertexCount - 1];
      Vector3f tip = new Vector3f(pt2.x - pt1.x, pt2.y - pt1.y, pt2.z - pt1.z);
      int diameter = (dmesh.diameter > 0 ? dmesh.diameter : 3);
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
    case DrawMesh.DRAW_CIRCLE:
      //unimplemented
      break;
    case DrawMesh.DRAW_CURVE:
      //unnecessary
      break;
    default:
      super.render2();
    }
    if (isDrawPickMode) {
      renderHandles();
    }
  }
  
  private void renderHandles() {
    switch (drawType) {
    case DrawMesh.DRAW_POINT:
    case DrawMesh.DRAW_ARROW:
    case DrawMesh.DRAW_CURVE:
    case DrawMesh.DRAW_LINE:
    case DrawMesh.DRAW_PLANE:
    case DrawMesh.DRAW_CIRCLE:
    case DrawMesh.DRAW_MULTIPLE:
      for (int i = dmesh.polygonCount; --i >= 0;) {
        if (!isPolygonDisplayable(i))
          continue;
        int[] vertexIndexes = dmesh.polygonIndexes[i];
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

  private final Point3i xyz = new Point3i();
  
  private void renderInfo() {
    if (dmesh == null || dmesh.title == null || dmesh.visibilityFlags == 0 
        || !g3d.setColix(viewer.getColixBackgroundContrast()))
      return;
    byte fid = g3d.getFontFid("SansSerif", 14);
    g3d.setFont(fid);
    for (int i = 0; i < dmesh.title.length; i++)
      if (dmesh.title[i].length() > 0) {
        viewer.transformPoint(vertices[i], xyz);
        g3d.drawString(dmesh.title[i], null, xyz.x + 5, xyz.y - 5, xyz.z, xyz.z);
      }
  }
}
