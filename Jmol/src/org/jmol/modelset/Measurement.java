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
package org.jmol.modelset;

import org.jmol.util.Logger;
import org.jmol.util.TextFormat;
import org.jmol.util.Measure;

import org.jmol.vecmath.Point3fi;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f;

import java.util.Vector;

public class Measurement {

  private Viewer viewer;

  public ModelSet modelSet;

  protected int count;
  protected int[] countPlusIndices;
  protected Point3fi[] points = new Point3fi[4];
  
  public int getCount() {
    return count;
  }
  
  public int[] getCountPlusIndices() {
    return countPlusIndices;
  }
  
  public int getIndex(int n) {
    return (n > 0 && n <= count ? countPlusIndices[n] : -1);
  }
  
  public Point3fi getAtom(int i) {
    int pt = countPlusIndices[i];
    return (pt < -1 ? points[-2 - pt] : modelSet.getAtomAt(pt));
  }

  public int getLastIndex() {
    return (count > 0 ? countPlusIndices[count] : -1);
  }
  
  private String strMeasurement;
  
  public String getString() {
    return strMeasurement;
  }
  
  private String strFormat;
  
  public String getStrFormat() {
    return strFormat;
  }
  
  protected float value;
  
  public float getValue() {
    return value;
  }
  
  private boolean isVisible = true;
  private boolean isHidden = false;
  private boolean isDynamic = false;
  private boolean isTrajectory = false;
  
  public boolean isVisible() {
    return isVisible;
  }
  public boolean isHidden() {
    return isHidden;
  }
  public boolean isDynamic() {
    return isDynamic;
  }
  
  public boolean isTrajectory() {
    return isTrajectory;
  }
  
  public void setVisible(boolean TF) {
    this.isVisible = TF;
  }
  public void setHidden(boolean TF) {
    this.isHidden = TF;
  }
  public void setDynamic(boolean TF) {
    this.isDynamic = TF;
  }
  
  private short colix;
  
  public short getColix() {
    return colix;
  }
  
  public void setColix(short colix) {
    this.colix = colix;
  }
  
  private int index;
  
  public void setIndex(int index) {
    this.index = index;
  }
  
  public int getIndex() {
    return index;
  }
  
  private AxisAngle4f aa;
  
  public AxisAngle4f getAxisAngle() {
    return aa;
  }
  
  private Point3f pointArc;
  
  public Point3f getPointArc() {
    return pointArc;
  }
  
  public Measurement(ModelSet modelSet, int[] atomCountPlusIndices, float value,
      short colix, String strFormat, int index) {
    //value Float.isNaN ==> pending
    this.modelSet = modelSet;
    this.viewer = modelSet.viewer;
    this.colix = colix;
    this.strFormat = strFormat;
    setInfo(modelSet, atomCountPlusIndices, value, index);
  }   

  public void refresh() {
    value = getMeasurement();
    isTrajectory = modelSet.isTrajectory(countPlusIndices);
    formatMeasurement();
  }
  
  /**
   * Used by MouseManager and Picking Manager to build the script
   * @param countPlusIndexes
   * @return measure (atomIndex=1) (atomIndex=2)....
   */
  public static String getMeasurementScript(int[] countPlusIndexes) {
    String str = "measure";
    int nAtoms = countPlusIndexes[0];
    for (int i = 0; i < nAtoms; i++) {
      str += " (atomIndex=" + countPlusIndexes[i + 1] + ")"; 
    }
    return str;  
  }
  
  private void setInfo(ModelSet modelSet, int[] atomCountPlusIndices, float value, int index) {
    if (atomCountPlusIndices == null)
      count = 0;
    else {
      count = atomCountPlusIndices[0];
      countPlusIndices = new int[count + 1];
      System.arraycopy(atomCountPlusIndices, 0, countPlusIndices, 0, count+1);
    }
    isTrajectory = modelSet.isTrajectory(countPlusIndices);    
    if (countPlusIndices != null && (Float.isNaN(value) || isTrajectory)) {
      value = getMeasurement();
    }
    this.value = value;
    this.index = index;
    formatMeasurement();
  }

  public void formatMeasurement(String strFormat, boolean useDefault) {
    if (strFormat != null && strFormat.length() == 0)
      strFormat = null;
    if (!useDefault && strFormat != null && strFormat.indexOf(countPlusIndices[0]+":")!=0)
      return;
    this.strFormat = strFormat; 
    formatMeasurement();
  }

  protected void formatMeasurement() {
    strMeasurement = null;
    if (Float.isNaN(value) || count == 0) {
      strMeasurement = null;
      return;
    }
    switch (count) {
    case 2:
      strMeasurement = formatDistance(value);
      break;
    case 3:
      if (value == 180) {
        aa = null;
        pointArc = null;
      } else {
        Vector3f vectorBA = new Vector3f();
        Vector3f vectorBC = new Vector3f();        
        float radians = Measure.computeAngle(getAtom(1), getAtom(2), getAtom(3), vectorBA, vectorBC, false);
        Vector3f vectorAxis = new Vector3f();
        vectorAxis.cross(vectorBA, vectorBC);
        aa = new AxisAngle4f(vectorAxis.x, vectorAxis.y, vectorAxis.z, radians);

        vectorBA.normalize();
        vectorBA.scale(0.5f);
        pointArc = new Point3f(vectorBA);
      }
    case 4:
      strMeasurement = formatAngle(value);
      break;
    default:
      Logger.error("Invalid count to measurement shape:" + count);
      throw new IndexOutOfBoundsException();
    }
  }
  
  public void reformatDistanceIfSelected() {
    if (count != 2)
      return;
    if (viewer.isSelected(countPlusIndices[1]) &&
        viewer.isSelected(countPlusIndices[2]))
      formatMeasurement();
  }

  private String formatDistance(float dist) {
    int nDist = (int)(dist * 100 + 0.5f);
    float value;
    String units = viewer.getMeasureDistanceUnits();
    if (units == "nanometers") {
      units = "nm";
      value = nDist / 1000f;
    } else if (units == "picometers") {
      units = "pm";
      value = (int)((dist * 1000 + 0.5)) / 10f;
    } else if (units == "au") {
      value = (int) (dist / JmolConstants.ANGSTROMS_PER_BOHR * 1000 + 0.5f) / 1000f;
    } else {
      units = "\u00C5"; // angstroms
      value = nDist / 100f;
    }
    return formatString(value, units);
  }

  private String formatAngle(float angle) {
    angle = (int)(angle * 10 + (angle >= 0 ? 0.5f : -0.5f));
    angle /= 10;
    return formatString(angle, "\u00B0");
  }

  private String formatString(float value, String units) {
    String s = countPlusIndices[0]+":" + "";
    String label = (strFormat != null && strFormat.indexOf(s)==0? strFormat : viewer
        .getDefaultMeasurementLabel(countPlusIndices[0]));
    if (label.indexOf(s)==0)
      label = label.substring(2);
    label = TextFormat.formatString(label, "#", index + 1);
    label = TextFormat.formatString(label, "UNITS", units);
    label = TextFormat.formatString(label, "VALUE", value);
    for (int i = countPlusIndices[0]; i >= 1;--i) {
      if (label.indexOf("%") < 0)
        break;
      label = modelSet.atoms[countPlusIndices[i]].formatLabel(label, (char)('0' + i), null);
    }
    if (label == null)
      return "";
    return label;
  }

  public boolean sameAs(int[] atomCountPlusIndices) {
    if (count != atomCountPlusIndices[0])
      return false;
    if (count == 2)
      return ((atomCountPlusIndices[1] == this.countPlusIndices[1] &&
               atomCountPlusIndices[2] == this.countPlusIndices[2]) ||
              (atomCountPlusIndices[1] == this.countPlusIndices[2] &&
               atomCountPlusIndices[2] == this.countPlusIndices[1]));
    if (count == 3)
      return (atomCountPlusIndices[2] == this.countPlusIndices[2] &&
              ((atomCountPlusIndices[1] == this.countPlusIndices[1] &&
                atomCountPlusIndices[3] == this.countPlusIndices[3]) ||
               (atomCountPlusIndices[1] == this.countPlusIndices[3] &&
                atomCountPlusIndices[3] == this.countPlusIndices[1])));    
    return ((atomCountPlusIndices[1] == this.countPlusIndices[1] &&
             atomCountPlusIndices[2] == this.countPlusIndices[2] &&
             atomCountPlusIndices[3] == this.countPlusIndices[3] &&
             atomCountPlusIndices[4] == this.countPlusIndices[4]) ||
            (atomCountPlusIndices[1] == this.countPlusIndices[4] &&
             atomCountPlusIndices[2] == this.countPlusIndices[3] &&
             atomCountPlusIndices[3] == this.countPlusIndices[2] &&
             atomCountPlusIndices[4] == this.countPlusIndices[1]));
  }

  public Vector toVector() {
    Vector V = new Vector();
    for (int i = 0; i < count + 1; i++ ) V.addElement(new Integer(countPlusIndices[i]));
    V.addElement(strMeasurement);
    return V;  
  }
  
  protected float getMeasurement() {
    float value = Float.NaN;
    if (countPlusIndices == null)
      return value;
    int count = countPlusIndices[0];
    if (count < 2)
      return value;
    for (int i = count; --i >= 0;)
      if (countPlusIndices[i + 1] == -1) {
        return value;
      }
    Point3fi ptA = getAtom(1);
    Point3fi ptB = getAtom(2);
    Point3fi ptC, ptD;
    switch (count) {
    case 2:
      value = ptA.distance(ptB);
      break;
    case 3:
      ptC = getAtom(3);
      value = Measure.computeAngle(ptA, ptB, ptC, true);
      break;
    case 4:
      ptC = getAtom(3);
      ptD = getAtom(4);
      value = Measure.computeTorsion(ptA, ptB, ptC, ptD, true);
      break;
    default:
      Logger.error("Invalid count in measurement calculation:" + count);
      throw new IndexOutOfBoundsException();
    }
    return value;
  }
}


