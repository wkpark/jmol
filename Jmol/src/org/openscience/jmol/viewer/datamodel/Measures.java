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
import org.openscience.jmol.viewer.g3d.Font3D;

import java.util.BitSet;

public class Measures extends Shape {

  final static int measurementGrowthIncrement = 16;
  int measurementCount = 0;
  Measurement[] measurements = new Measurement[measurementGrowthIncrement];
  PendingMeasurement pendingMeasurement;

  short colix = Graphics3D.WHITE;
  Font3D font3d;

  void initShape() {
    pendingMeasurement = new PendingMeasurement(frame);
    font3d = g3d.getFont3D(JmolConstants.MEASURE_DEFAULT_FONTSIZE);
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
  
  boolean delete(Object value) {
    if (value instanceof int[])
      return delete((int[])value);
    if (value instanceof Integer)
      return delete(((Integer)value).intValue());
    return false;
  }

  boolean delete(int[] atomCountPlusIndices) {
    for (int i = measurementCount; --i >= 0; ) {
      if (measurements[i].sameAs(atomCountPlusIndices))
        return delete(i);
    }
    return false;
  }

  boolean delete(int i) {
    if (i < measurementCount) {
      System.arraycopy(measurements, i+1,
                       measurements, i,
                       measurementCount - i - 1);
      --measurementCount;
      measurements[measurementCount] = null;
      return true;
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
      {
        System.out.println("Measures.color set to:" + value);
        colix = g3d.getColix(value); return; }
    else if ("font".equals(propertyName))
      { font3d = (Font3D)value; return; }
    else if ("define".equals(propertyName))
      { define((int[])value); }
    else if ("delete".equals(propertyName))
      { delete(value); }
    else if ("toggle".equals(propertyName))
      { toggle((int[])value); }
    else if ("pending".equals(propertyName))
      { pending((int[])value); }
    else if ("clear".equals(propertyName))
      { clear(); }
    else
      return;
    viewer.notifyMeasurementsChanged();
  }

  public Object getProperty(String property, int index) {
    //    System.out.println("Measures.getProperty(" +property + "," + index +")");
    String propertyString = (String)property;
    if ("count".equals(property))
      { return new Integer(measurementCount); }
    if ("countPlusIndices".equals(property)) {
      return index < measurementCount
        ? measurements[index].countPlusIndices : null;
    }
    if ("stringValue".equals(property)) {
      return index < measurementCount
        ? measurements[index].strMeasurement : null;
    }
    return null;
  }
}
