/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.viewer;

import org.jmol.g3d.*;

import java.util.BitSet;
import java.util.Vector;

class Measures extends Shape {

  final static int measurementGrowthIncrement = 16;
  int measurementCount = 0;
  Measurement[] measurements = new Measurement[measurementGrowthIncrement];
  PendingMeasurement pendingMeasurement;

  short mad = (short)-1;
  short colix; // default to none in order to contrast with background
  boolean showMeasurementNumbers = true;
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
      measurements =(Measurement[])Util.setLength(measurements,
                                                  measurementCount +
                                                  measurementGrowthIncrement);
    }
    viewer.setStatusNewDefaultModeMeasurement("measureCompleted" , measurementCount, measureNew.toVector().toString());
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

  void toggle(Vector monitorExpressions) {
  /*
   * 
   *(1) run through first expression, choosing model
   *(2) for each item of next bs, iterate over next bitset, etc.
   *(3) for each last bitset, trigger toggle(int[])
   *
   *simple!
   *
   */
  
    int nPoints = monitorExpressions.size();
    if (nPoints == 0)
      return;
    int[] atomCountPlusIndices = new int[5];
    atomCountPlusIndices[0] = nPoints;
    nextMeasure(0, nPoints, monitorExpressions, atomCountPlusIndices, 0);
  }

  void nextMeasure(int thispt, int nPoints, Vector monitorExpressions, 
                   int[] atomCountPlusIndices, int thisModel) {
    BitSet bs = (BitSet)monitorExpressions.get(thispt);
    int i = -1;
    //System.out.println("nextMeasure"+thispt+" acpi:"+atomCountPlusIndices);
    //System.out.println("bs "+ bs);
    for (int iBit = bs.cardinality(); --iBit >= 0;) {
      i = bs.nextSetBit(i + 1);
      int modelIndex = frame.atoms[i].getModelIndex();
      //System.out.println("nextMeasure i"+i+" modelIndex:"+modelIndex);
      if (thispt == 0) {
        thisModel = modelIndex;
      } else if (thisModel != modelIndex) {
        continue;
      }
      atomCountPlusIndices[thispt + 1] = i;
      if (thispt == nPoints - 1) {
        //System.out.println("toggling");
        toggle(atomCountPlusIndices);
      } else {
        nextMeasure(thispt+1, nPoints, monitorExpressions, 
            atomCountPlusIndices, thisModel);
      }
    }
  }
  
  void pending(int[] countPlusIndices) {
    pendingMeasurement.setCountPlusIndices(countPlusIndices);
    if (pendingMeasurement.count > 1)
      viewer.setStatusNewDefaultModeMeasurement("measurePending" , pendingMeasurement.count, pendingMeasurement.strMeasurement);
  }

  void setSize(int size, BitSet bsSelected) {
    mad = (short)size;
    System.out.println("Measures.setSize(" + size + ")");
    //    throw new NullPointerException();
  }

  void setProperty(String propertyName, Object value,
                          BitSet bsSelected){
    
    if ("color".equals(propertyName))
      {
        System.out.println("Measures.color set to:" + value);
        colix = value == null ? 0 : Graphics3D.getColix(value); return; }
    else if ("font".equals(propertyName))
      { font3d = (Font3D)value; return; }
    else if ("define".equals(propertyName))
      { define((int[])value); }
    else if ("delete".equals(propertyName))
      { delete(value); }
    else if ("toggle".equals(propertyName))
    { toggle((int[])value); }
    else if ("toggleVector".equals(propertyName))
    { toggle((Vector)value); }
    else if ("pending".equals(propertyName))
      { pending((int[])value); }
    else if ("clear".equals(propertyName))
      { clear(); }
    else if ("showMeasurementNumbers".equals(propertyName))
      { showMeasurementNumbers = ((Boolean)value).booleanValue(); }
    else if ("reformatDistances".equals(propertyName))
      { reformatDistances(); }
    else
      return;
  }

  Object getProperty(String property, int index) {
    //    System.out.println("Measures.getProperty(" +property + "," + index +")");
    //String propertyString = (String)property;
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

  void reformatDistances() {
    for (int i = measurementCount; --i >= 0; )
      measurements[i].reformatDistanceIfSelected();
  }
}
