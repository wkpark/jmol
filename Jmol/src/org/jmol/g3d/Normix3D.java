/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.g3d;

import javax.vecmath.Vector3f;
import java.util.Hashtable;

/**
 * Provides quantization of normalized vectors so that shading for
 * lighting calculations can be handled by a simple index lookup
 *<p>
 * A 'normix' is a normal index, represented as a short
 *
 * @author Miguel, miguel@jmol.org
 */
class Normix3D {

  Graphics3D g3d;
  Geodesic3D geodesic;

  private final static boolean GEODESIC_DUMP = false;

  Normix3D(Graphics3D g3d) {
    // 12, 42, 162, 642, 2562
    this.g3d = g3d;
    geodesic = g3d.geodesic3d;
    if (GEODESIC_DUMP)
      geodesicDump();
  }
  
  void geodesicDump() {
    for (int level = 0; level <= g3d.HIGHEST_GEODESIC_LEVEL; ++level) {
      int vertexCount = geodesic.getVertexCount(level);
      System.out.println("level=" + level +
                         " vertexCount=" + vertexCount +
                         " faceCount=" + geodesic.getFaceCount(level));
      short[] neighborVertexes = geodesic.getNeighborVertexes(level);
      short[] faceVertexes = geodesic.getFaceVertexes(level);
      System.out.println("neighborVertexes.length=" +
                         neighborVertexes.length +
                         " faceVertexes.length=" +
                         faceVertexes.length);
      for (short i = 0; i < vertexCount; ++i) {
        System.out.print("level:" + level + " vertex:" + i + " ->");
        for (int j = 0; j < 6; ++j)
          System.out.print(" " + neighborVertexes[i * 6 + j]);
        System.out.println("");
      }
      /*
      for (short i = 0; i < vertexCount; ++i) {
        System.out.print("level:" + level + " vertex:" + i + " ->");
        for (int j = 6; --j >= 0; )
          System.out.print(" " + geodesic.getNeighborVertex(0, i, j));
        System.out.println("");
      }
      */
      System.out.println("-----------------");
    }
  }
}
