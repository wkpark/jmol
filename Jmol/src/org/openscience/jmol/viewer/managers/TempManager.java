/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.viewer.managers;

import org.openscience.jmol.viewer.*;

import javax.vecmath.*;

public class TempManager {

  JmolViewer viewer;

  public TempManager(JmolViewer viewer) {
    this.viewer = viewer;
  }

  static int findBestFit(int size, short[] lengths) {
    int iFit = -1;
    int fitLength = Integer.MAX_VALUE;
    
    for (int i = lengths.length; --i >= 0; ) {
      int freeLength = (short)lengths[i];
      if (freeLength >= size &&
          freeLength < fitLength) {
        fitLength = freeLength;
        iFit = i;
      }
    }
    if (iFit >= 0)
      lengths[iFit] = 0;
    return iFit;
  }

  static int findShorter(int size, short[] lengths) {
    for (int i = lengths.length; --i >= 0; )
      if (lengths[i] == 0) {
        lengths[i] = (short)size;
        return i;
      }
    int iShortest = 0;
    int shortest = lengths[0];
    for (int i = lengths.length; --i > 0; )
      if (lengths[i] < shortest) {
        shortest = lengths[i];
        iShortest = i;
      }
    if (shortest < size) {
      lengths[iShortest] = (short)size;
      return iShortest;
    }
    return -1;
  }

  ////////////////////////////////////////////////////////////////
  // temp Points
  ////////////////////////////////////////////////////////////////
  final static int freePointsSize = 8;
  final short[] lengthsFreePoints = new short[freePointsSize];
  final Point3f[][] freePoints = new Point3f[freePointsSize][];

  public Point3f[] allocTempPoints(int size) {
    Point3f[] tempPoints;
    int iFit = findBestFit(size, lengthsFreePoints);
    if (iFit > 0) {
      tempPoints = freePoints[iFit];
    } else {
      tempPoints = new Point3f[size];
      for (int i = size; --i >= 0; )
        tempPoints[i] = new Point3f();
    }
    return tempPoints;
  }

  public void freeTempPoints(Point3f[] tempPoints) {
    int iFree = findShorter(tempPoints.length, lengthsFreePoints);
    if (iFree >= 0)
      freePoints[iFree] = tempPoints;
  }

  ////////////////////////////////////////////////////////////////
  // temp Screens
  ////////////////////////////////////////////////////////////////
  final static int freeScreensSize = 8;
  final short[] lengthsFreeScreens = new short[freeScreensSize];
  final Point3i[][] freeScreens = new Point3i[freeScreensSize][];

  public Point3i[] allocTempScreens(int size) {
    Point3i[] tempScreens;
    int iFit = findBestFit(size, lengthsFreeScreens);
    if (iFit > 0) {
      tempScreens = freeScreens[iFit];
    } else {
      tempScreens = new Point3i[size];
      for (int i = size; --i >= 0; )
        tempScreens[i] = new Point3i();
    }
    return tempScreens;
  }

  public void freeTempScreens(Point3i[] tempScreens) {
    int iFree = findShorter(tempScreens.length, lengthsFreeScreens);
    if (iFree >= 0)
      freeScreens[iFree] = tempScreens;
  }
}
