/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-14 23:28:16 -0500 (Sat, 14 Apr 2007) $
 * $Revision: 7408 $
 *
 * Copyright (C) 2005  Miguel, The Jmol Development Team
 *
 * Contact: miguel@jmol.org, jmol-developers@lists.sf.net
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

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.shape.Mesh;
import org.jmol.util.ArrayUtil;
import org.jmol.viewer.JmolConstants;
import org.jmol.g3d.Graphics3D;

public class DrawMesh extends Mesh {
  
  DrawMesh(String thisID, Graphics3D g3d, short colix) {
    super(thisID, g3d, colix);
  }

  BitSet modelFlags;
  
  int drawType = JmolConstants.DRAW_TRIANGLE;
  int[] drawTypes;
  Point3f ptCenters[];
  Vector3f axis = new Vector3f(1,0,0);
  Vector3f axes[];
  int drawVertexCount;
  int[] drawVertexCounts;
  boolean isFixed;
  float drawArrowScale;

  final void setCenter(int iModel) {
    Point3f center = new Point3f(0, 0, 0);
    int iptlast = -1;
    int ipt = 0;
    int n = 0;
    for (int i = polygonCount; --i >= 0;) {
      if (iModel >=0 && i != iModel || polygonIndexes[i] == null)
        continue;
      iptlast = -1;
      for (int iV = polygonIndexes[i].length; --iV >= 0;) {
        ipt = polygonIndexes[i][iV];
        if (ipt == iptlast)
          continue;
        iptlast = ipt;
        center.add(vertices[ipt]);
        n++;
      }
      if (n > 0 && (i == iModel || i == 0)) {
        center.scale(1.0f / n);
        break;
      }
    }
    if (iModel < 0){
      ptCenter = center;
    } else {
      ptCenters[iModel] = center;
    }
  }

  void offset(Vector3f offset) {
    for (int i = vertexCount; --i >= 0;)
      vertices[i].add(offset);
  }

  public void deleteAtoms(int modelIndex) {
    if (modelIndex >= polygonCount)
      return;
    polygonCount--;
    polygonIndexes = (int[][]) ArrayUtil.deleteElements(polygonIndexes, modelIndex, 1);
    drawTypes = (int[]) ArrayUtil.deleteElements(drawTypes, modelIndex, 1);
    drawVertexCounts = (int[]) ArrayUtil.deleteElements(drawVertexCounts, modelIndex, 1);
    ptCenters = (Point3f[]) ArrayUtil.deleteElements(ptCenters, modelIndex, 1);
    axes = (Vector3f[]) ArrayUtil.deleteElements(axes, modelIndex, 1);
    title = (String[]) ArrayUtil.deleteElements(title, modelIndex, 1);
  }
}
