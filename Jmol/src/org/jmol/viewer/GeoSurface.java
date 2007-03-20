/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-19 12:18:17 -0500 (Mon, 19 Mar 2007) $
 * $Revision: 7171 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import java.util.BitSet;

import org.jmol.g3d.Geodesic3D;
import org.jmol.util.Logger;

class GeoSurface extends Dots {
  
  void initShape() {
    isSurface = true;
    super.initShape();
  }

  void initialize(int mode) {
    bsIgnore = null;
    bsSelected = null;
    argb = 0;
    isActive = false;
  }
  
  void setProperty(String propertyName, Object value, BitSet bs) {

    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("GeoSurface.setProperty: " + propertyName + " " + value);
    }

    if ("translucency" == propertyName) {
      //skip dots and go straight to AtomShape
      super.setSuperProperty(propertyName, value, bs);
      return;
    }    
    super.setProperty(propertyName, value, bs);
  }


  void calcConvexMap() {
    calcConvexBits();
    int[] map = mapNull;
    int count = getMapStorageCount(geodesicMap);
    if (count > 0) {
      bsSurface.set(indexI);
      addIncompleteFaces(geodesicMap);
      addIncompleteFaces(geodesicMap);
      count = getMapStorageCount(geodesicMap);
      map = new int[count];
      System.arraycopy(geodesicMap, 0, map, 0, count);
    }
    dotsConvexMaps[indexI] = map;
  }
  
  void addIncompleteFaces(int[] points) {
    clearBitmap(mapT);
    short[] faces = Geodesic3D.faceVertexesArrays[MAX_LEVEL];
    int len = faces.length;
    int maxPt = -1;
    for (int f = 0; f < len;) {
      short p1 = faces[f++];
      short p2 = faces[f++];
      short p3 = faces[f++];
      boolean ok1 = getBit(points, p1); 
      boolean ok2 = getBit(points, p2); 
      boolean ok3 = getBit(points, p3);
      if (! (ok1 || ok2 || ok3) || ok1 && ok2 && ok3)
        continue;
      // trick: DO show faces if ANY ONE vertex is missing
      if (!ok1) {
        setBit(mapT, p1);
        if (maxPt < p1)
          maxPt = p1;
      }
      if (!ok2) {
        setBit(mapT, p2);
        if (maxPt < p2)
          maxPt = p2;
      }
      if (!ok3) {
        setBit(mapT, p3);
        if (maxPt < p3)
          maxPt = p3;
      }
    }
    for (int i=0; i <= maxPt; i++) {
      if (getBit(mapT, i))
        setBit(points, i);
    }
  }
}
