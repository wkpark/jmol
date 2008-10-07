/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-26 01:48:23 -0500 (Tue, 26 Sep 2006) $
 * $Revision: 5729 $
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;
import org.jmol.util.Logger;


import java.io.BufferedReader;
import java.util.Vector;

import javax.vecmath.Point3f;

/**
 * Amber Coordinate File Reader
 * 
 * not a stand-alone reader -- must be after COORD keyword in LOAD command
 *  
 */

public class MdCrdReader extends AtomSetCollectionReader {
    
 private Vector trajectorySteps;
 public AtomSetCollection readAtomSetCollection(BufferedReader br) {
    reader = br;
    atomSetCollection = new AtomSetCollection("mdcrd");
    // add a dummy atom, just so not "no atoms found"
    atomSetCollection.addAtom(new Atom()); 
    trajectorySteps = (Vector) htParams.get("trajectorySteps");
    if (trajectorySteps == null)
      htParams.put("trajectorySteps", trajectorySteps = new Vector());
    try {
      readLine(); //title
      readCoordinates();
    } catch (Exception e) {
      return setError(e);
    }
    Logger.info("Total number of trajectory steps=" + trajectorySteps.size());
    return atomSetCollection;
  }

  void readCoordinates() throws Exception {
    int atomCount = (bsFilter == null ? templateAtomCount : ((Integer) htParams
        .get("filteredAtomCount")).intValue());
    boolean isPeriodic = htParams.containsKey("isPeriodic");
    int first = firstLastStep[0];
    int last = firstLastStep[1];
    int step = firstLastStep[2];
    int floatCount = templateAtomCount * 3 + (isPeriodic ? 3 : 0);
    if (last < 0)
      last = Integer.MAX_VALUE;
    int ptStep = 0;
    while (ptStep < first) {
      if (!skipFloats(floatCount))
        return;
      ptStep++;
    }
    line = null;
    for (int i = first; i <= last; i++) {
      Point3f[] trajectoryStep = new Point3f[atomCount];
      if (!getTrajectoryStep(trajectoryStep, isPeriodic))
        return;
      trajectorySteps.add(trajectoryStep);
      for (int j = 1; j < step; j++)
        if (!skipFloats(floatCount))
          return;
    }
  }

  private int ptFloat = 0;
  private int lenLine = 0;
  private float getFloat() throws Exception {
    while (line == null || ptFloat >= lenLine) {
      if (readLine() == null)
        return Float.NaN;
      ptFloat = 0;
      lenLine = line.length();
    }
    ptFloat += 8;
    return parseFloat(line.substring(ptFloat - 8, ptFloat));
  }
  
  private Point3f getPoint() throws Exception {
    float x = getFloat();
    float y = getFloat();
    float z = getFloat();
    return (Float.isNaN(z) ? null : new Point3f(x, y, z));
  }

  private boolean getTrajectoryStep(Point3f[] trajectoryStep, boolean isPeriodic)
      throws Exception {
    int atomCount = trajectoryStep.length;
    int n = -1;
    for (int i = 0; i < templateAtomCount; i++) {
      Point3f pt = getPoint();
      if (pt == null)
        return false;
      if (bsFilter == null || bsFilter.get(i)) {
        if (++n == atomCount)
          return false;
        trajectoryStep[n] = pt;
      }
    }
    if (isPeriodic)
      getPoint(); //why? not in specs?
    return (line != null);
  }

  private boolean skipFloats(int n) throws Exception {
    int i = 0;
    // presumes float sets are separated by new line
    while (i < n && readLine() != null)
      i += getTokens().length;
    return (line != null);
  }
}
