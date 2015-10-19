/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-16 18:06:32 -0500 (Mon, 16 Apr 2007) $
 * $Revision: 7418 $
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

import java.util.Hashtable;
import java.util.Map;

import org.jmol.shape.Mesh;

import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.T3;
import javajs.util.V3;

public class Pmesh extends Isosurface {
  @Override
  public void initShape() {
    super.initShape();
    myType = "pmesh";
  }
  
  @Override
  public Object getProperty(String property, int index) {
    if (property == "face") {
      Mesh m = currentMesh;
      if (index >= 0
          && (index >= meshCount || (m = meshes[index]) == null))
        return null;
      return  m == null ? null : getFace(m);
    }
    return getPropI(property, index);
  }
  
  /**
   * return a cycle of points generating this face
   * used after slabbing
   * 
   * @param m
   * @return set of points constituting this face
   */
  private P3[] getFace(Mesh m) {
    if (m.haveQuads)
      return null;
    T3[] vs = m.vs;
    Map<String, int[]> htEdges = new Hashtable<String, int[]>();
    int v1 = 0;
    int n = 0;
    for (int i = m.pc; --i >= 0;) {
      if (m.bsSlabDisplay != null && !m.bsSlabDisplay.get(i))
        continue;
      int[] face = m.pis[i];
      int mask = face[3];
      for (int j = 0; j < 3; j++)
        if ((mask & (1 << j)) != 0) {
          v1 = face[j];
          int v2 = face[(j + 1) % 3];
          String key = v2 + "_" + v1;
          if (htEdges.containsKey(key)) {
            htEdges.remove(key);
            n--;
          } else {
            n++;
            htEdges.put(v1 + "_" + v2, new int[] { v1, v2 });
            htEdges.put("" + v1, new int[] { v1, v2 });
          }
        }
    }
    if (n == 0)
      return null;
    int[][] a = new int[n][2];
    for (String e : htEdges.keySet()) {
      a[0] = htEdges.get(e);
      break;
    }
    V3 vectorBA = new V3();
    V3 vectorBC = new V3();
    int v0 = a[0][0];
    int v01 = v0;
    v1 = a[0][1];
    int pt = 0;
    while (v1 != v0) {
      int[] edge = htEdges.get("" + v1);
      if (edge == null)
        break;
      float angle = Measure.computeAngle(vs[v01], vs[v1], vs[edge[1]],
          vectorBA, vectorBC, true);
      v1 = edge[1];
      if (angle < 179) {
        a[++pt] = edge;
        v01 = edge[0];
      } else {
        a[pt][1] = v1;
      }
    }
    if (Measure.computeAngle(vs[v01], vs[v1], vs[a[0][1]],
          vectorBA, vectorBC, true) >= 179) {
      a[0][0] = a[pt--][0];
    }
    n = ++pt;
    P3[] pts = new P3[n];
    for (int i = 0; i < n; i++)
      pts[i] = P3.newP(vs[a[i][0]]);
    return pts;
  }


}
