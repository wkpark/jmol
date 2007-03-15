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
package org.jmol.viewer;


import javax.vecmath.Point3i;

class DrawRenderer extends MeshRenderer {

  void render() {
    /*
     * Each drawn object, draw.meshes[i], may consist of several polygons, one
     * for each MODEL FRAME. Or, it may be "fixed" and only contain one single
     * polygon.
     * 
     */
    Draw draw = (Draw) shape;
    for (int i = draw.meshCount; --i >= 0;) {
      render1(draw.meshes[i]);
      renderInfo(draw.meshes[i]);
    }
  }
  
  Point3i xyz = new Point3i();
  void renderInfo(Mesh mesh) {
    if (mesh == null || mesh.title == null
        || !g3d.setColix(viewer.getColixBackgroundContrast()))
      return;
    byte fid = g3d.getFontFid("SansSerif", 14);
    g3d.setFont(fid);
    for (int i = 0; i < mesh.title.length; i++)
      if (mesh.title[i].length() > 0) {
        viewer.transformPoint(vertices[i], xyz);
        g3d.drawString(mesh.title[i], null, xyz.x + 5, xyz.y - 5, xyz.z, xyz.z);
      }

  }
}
