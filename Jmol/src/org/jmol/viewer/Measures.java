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
import java.util.Hashtable;

class Measures extends Shape {

  final static int measurementGrowthIncrement = 16;
  int measurementCount = 0;
  Measurement[] measurements = new Measurement[measurementGrowthIncrement];
  PendingMeasurement pendingMeasurement;

  short mad = (short)-1;
  short colix; // default to none in order to contrast with background
  boolean showMeasurementNumbers = true;
  Font3D font3d;
  float[] rangeMinMax = {Float.MAX_VALUE, Float.MAX_VALUE};
  
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
    float value = frame.getMeasurement(atomCountPlusIndices);
    //System.out.println("measures define value,maxmin "+value+" "+rangeMinMax[0]+" "+rangeMinMax[1]);
    
    if (rangeMinMax[0] != Float.MAX_VALUE) {
      if (value < rangeMinMax[0] || value > rangeMinMax[1])
        return;
    }
    //System.out.println("define " + rangeMinMax[0] + "-" + rangeMinMax[1] + ";" + value);
    Measurement measureNew = new Measurement(frame, atomCountPlusIndices, value);
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
  
  void setRange(float[] rangeMinMax) {
    //System.out.println("setRange"+rangeMinMax[0]+rangeMinMax[1]);
    this.rangeMinMax[0] = rangeMinMax[0];
    this.rangeMinMax[1] = rangeMinMax[1];
  }
  
  void toggle(int[] atomCountPlusIndices) {
    if (isDefined(atomCountPlusIndices))
      delete(atomCountPlusIndices);
    else {
      rangeMinMax[0] = Float.MAX_VALUE;
      define(atomCountPlusIndices);
    }
  }

  void define(Vector monitorExpressions) {
  /*
   * sets up measures based on an array of atom selection expressions -RMH 3/06
   * 
   *(1) run through first expression, choosing model
   *(2) for each item of next bs, iterate over next bitset, etc.
   *(3) for each last bitset, trigger toggle(int[])
   *
   *simple!
   *
   */
  
    int nPoints = monitorExpressions.size();
    if (nPoints < 2)
      return;
    int[] atomCountPlusIndices = new int[5];
    atomCountPlusIndices[0] = nPoints;
    nextMeasure(0, nPoints, monitorExpressions, atomCountPlusIndices, 0);
  }

  void nextMeasure(int thispt, int nPoints, Vector monitorExpressions, 
                   int[] atomCountPlusIndices, int thisModel) {
    BitSet bs = (BitSet)monitorExpressions.get(thispt);
    //System.out.println("nextMeasure"+thispt+" acpi:"+atomCountPlusIndices);
    //System.out.println("bs "+ bs);
    for (int i = bs.size(); --i >= 0;) {
      if (bs.get(i)) {
        if (thispt > 0 && i == atomCountPlusIndices[thispt])
          continue;
        int modelIndex = frame.atoms[i].getModelIndex();
        //System.out.println("nextMeasure i"+i+" modelIndex:"+modelIndex);
        if (thispt == 0) {
          thisModel = modelIndex;
        } else if (thisModel != modelIndex) {
          continue;
        }
        atomCountPlusIndices[thispt + 1] = i;
        if (thispt == nPoints - 1) {
          if (! isDefined(atomCountPlusIndices))
            define(atomCountPlusIndices);
        } else {
          nextMeasure(thispt+1, nPoints, monitorExpressions, 
              atomCountPlusIndices, thisModel);
        }
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
  }

  void setProperty(String propertyName, Object value,
                          BitSet bsSelected){
    
    if ("color".equals(propertyName))
      {
        //System.out.println("Measures.color set to:" + value);
        colix = value == null ? 0 : Graphics3D.getColix(value); return; }
    else if ("font".equals(propertyName))
      { font3d = (Font3D)value; return; }
    else if ("define".equals(propertyName))
      { define((int[])value); }
    else if ("delete".equals(propertyName))
      { delete(value); }
    else if ("toggle".equals(propertyName))
    { toggle((int[])value); }
    else if ("defineVector".equals(propertyName))
    { define((Vector)value); }
    else if ("setRange".equals(propertyName))
    { setRange((float[])value); }
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
    if ("info".equals(property)) {
      return getAllInfo();
    }
    return null;
  }

  Vector getAllInfo() {
    Vector info = new Vector();
    for (int i = 0; i< measurementCount; i++) {
      info.add(getInfo(i));
    }
    return info;
  }
  
  Hashtable getInfo(int index) {
    Hashtable info = new Hashtable();
    info.put("index",new Integer(index));
    info.put("strMeasurement",measurements[index].strMeasurement);
    int count = measurements[index].count;
    info.put("count",new Integer(count));
    info.put("value",new Float(measurements[index].value));
    Vector atomsInfo = new Vector();
    for (int i = 0; i < count; i++) {
      Hashtable atomInfo = new Hashtable();
      Atom atom = frame.atoms[measurements[index].countPlusIndices[i + 1]];
      atomInfo.put("_ipt", new Integer(atom.atomIndex));
      atomInfo.put("atomno", new Integer(atom.getAtomNumber()));
      atomInfo.put("info", atom.getInfo());
      atomsInfo.add(atomInfo);
    }
    info.put("atoms", atomsInfo);
    return info;  
  }
  
  void reformatDistances() {
    for (int i = measurementCount; --i >= 0; )
      measurements[i].reformatDistanceIfSelected();
  }
}
