/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;

import java.awt.Color;
import java.util.BitSet;

public class Measures extends Shape {

  final static int measurementGrowthIncrement = 16;
  int measurementCount = 0;
  Measurement[] measurements = new Measurement[measurementGrowthIncrement];
  PendingMeasurement pendingMeasurement;

  short colix = Graphics3D.WHITE;
  int fontsize = JmolConstants.MEASURE_DEFAULT_FONTSIZE;

  void initShape() {
    pendingMeasurement = new PendingMeasurement(frame);
  }

  void clear() {
    int countT = measurementCount;
    measurementCount = 0;
    for (int i = countT; --i >= 0; )
      measurements[i] = null;
  }

  boolean isDefined(int[] atomCountPlusIndices) {
    for (int i = measurementCount; --i >= 0; ) {
      if (measurements[i].sameAs(atomCountPlusIndices))
        return true;
    }
    return false;
  }

  void define(int[] atomCountPlusIndices) {
    if (isDefined(atomCountPlusIndices))
      return;
    Measurement measureNew = new Measurement(frame, atomCountPlusIndices);
    if (measurementCount == measurements.length) {
      Measurement[] t = new Measurement[measurementCount + measurementGrowthIncrement];
      System.arraycopy(measurements, 0, t, 0, measurementCount);
      measurements = t;
    }
    measurements[measurementCount++] = measureNew;
  }
  
  boolean delete(int[] atomCountPlusIndices) {
    for (int i = measurementCount; --i >= 0; ) {
      if (measurements[i].sameAs(atomCountPlusIndices)) {
        System.arraycopy(measurements, i+1,
                         measurements, i,
                         measurementCount - i - 1);
        --measurementCount;
        measurements[measurementCount] = null;
        return true;
      }
    }
    return false;
  }
  
  void toggle(int[] atomCountPlusIndices) {
    if (isDefined(atomCountPlusIndices))
      delete(atomCountPlusIndices);
    else
      define(atomCountPlusIndices);
  }

  void pending(int[] countPlusIndices) {
    pendingMeasurement.setCountPlusIndices(countPlusIndices);
  }

  public void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    if ("color".equals(propertyName))
      { colix = viewer.getColix((Color)value); return; }
    if ("fontsize".equals(propertyName))
      { fontsize = ((Integer)value).intValue(); return; }
    if ("define".equals(propertyName))
      { define((int[])value); return; }
    if ("delete".equals(propertyName))
      { delete((int[])value); return; }
    if ("toggle".equals(propertyName))
      { toggle((int[])value); return; }
    if ("pending".equals(propertyName))
      { pending((int[])value); return; }
    if ("clear".equals(propertyName))
      { clear(); return; }
  }
}
