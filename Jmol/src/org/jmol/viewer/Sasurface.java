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

package org.jmol.viewer;

import org.jmol.g3d.Graphics3D;
import java.util.BitSet;

class Sasurface extends Shape {

  static int MAX_GEODESIC_RENDERING_LEVEL = 1;

  // note that when there is a full torus
  // the 0 point will be repeated as 2*PI
  static int MAX_FULL_TORUS_STEP_COUNT = 15;

  // note that the outer torus is at most 180 degrees
  // so this step count is over 180 degrees, not 360
  static int OUTER_TORUS_STEP_COUNT = 9;

  final static int MAX_TORUS_POINTS =
    MAX_FULL_TORUS_STEP_COUNT * OUTER_TORUS_STEP_COUNT;


  int surfaceCount;
  Sasurface1[] surfaces = new Sasurface1[4];

  Sasurface1 currentSurface;

  void initShape() {
  }

  void setSize(int size, BitSet bsSelected) {
    dumpState("setSize:" + size);
    if (currentSurface != null)
      currentSurface.setSize(size, bsSelected);
    else {
      for (int i = surfaceCount; --i >= 0; )
        surfaces[i].setSize(size, bsSelected);
    }
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    System.out.println("Sasurface.setProperty(" + propertyName + "," + value +
                       ")");
    dumpState("setProperty:" + propertyName + ":" + value);
    if ("surfaceID" == propertyName) {
      String surfaceID = (String)value;
      System.out.println("surfaceID=" + surfaceID);
      if (surfaceID == null) {
        currentSurface = null;
        System.out.println("----> null surfaceID");
        return;
      }
      for (int i = surfaceCount; --i >= 0; ) {
        currentSurface = surfaces[i];
        if (surfaceID.equals(currentSurface.surfaceID)) {
          System.out.println("surfaceID is set to" + surfaceID);
          return;
        }
      }
      allocSurface(surfaceID, bs);
      dumpState("done");
      return;
    }

    if ("delete" == propertyName) {
      System.out.println("delete && surfaceCount=" + surfaceCount);
      if (currentSurface != null) {
        System.out.println("deleting currentSurface:" +
                           currentSurface.surfaceID);
        int iCurrent;
        for (iCurrent = surfaceCount; surfaces[--iCurrent] != currentSurface; )
          {}
        for (int j = iCurrent + 1; j < surfaceCount; ++j)
          surfaces[j - 1] = surfaces[j];
        surfaces[--surfaceCount] = null;
        currentSurface = null;
      } else {
        System.out.println("deleting all surfaces");
        for (int i = surfaceCount; --i >= 0; )
          surfaces[i] = null;
        surfaceCount = 0;
      }
      return;
    }

    if (currentSurface != null)
      currentSurface.setProperty(propertyName, value, bs);
    else
      for (int i = surfaceCount; --i >= 0; )
        surfaces[i].setProperty(propertyName, value, bs);
  }

  void allocSurface(String surfaceID, BitSet bs) {
    System.out.println("allocSurface(" + surfaceID + ")");
    surfaces = (Sasurface1[])Util.ensureLength(surfaces, surfaceCount + 1);
    currentSurface = surfaces[surfaceCount++] =
      new Sasurface1(surfaceID, viewer, g3d, Graphics3D.YELLOW, bs);
  }

  void dumpState(String msg) {
    System.out.println(">>>>>>>>>>>>>>>>>>>>> " + msg);
    System.out.println("surfaceCount=" + surfaceCount);
    System.out.println("currentSurface=" +
                       (currentSurface == null ? "NULL" :
                        currentSurface.surfaceID));
  }
}
