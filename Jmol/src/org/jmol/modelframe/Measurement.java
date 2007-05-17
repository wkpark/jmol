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
package org.jmol.modelframe;

import org.jmol.util.Logger;
import org.jmol.util.TextFormat;
import org.jmol.util.Measure;

import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f;

import java.util.Vector;

public class Measurement {

  Frame frame;
  Viewer viewer;
  
  protected int count;
  
  public int getCount() {
    return count;
  }
  
  protected int[] countPlusIndices;
  
  public int[] getCountPlusIndices() {
    return countPlusIndices;
  }
  
  public int getIndex(int n) {
    return (n > 0 && n <= count ? countPlusIndices[n] : -1);
  }
  
  public int getLastIndex() {
    return (count > 0 ? countPlusIndices[count] : -1);
  }
  
  public int getPreviousIndex() {
    return (count > 0 ? countPlusIndices[count - 1] : -1);
  }
  
  private String strMeasurement;
  
  public String getString() {
    return strMeasurement;
  }
  
  String strFormat;
  
  public String getStrFormat() {
    return strFormat;
  }
  
  float value;
  
  public float getValue() {
    return value;
  }
  
  private boolean isVisible = true;
  private boolean isHidden = false;
  private boolean isDynamic = false;
  
  public boolean isVisible() {
    return isVisible;
  }
  public boolean isHidden() {
    return isHidden;
  }
  public boolean isDynamic() {
    return isDynamic;
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
  
  public Measurement(Frame frame, int[] atomCountPlusIndices, float value,
      short colix, String strFormat, int index) {
    //value Float.isNaN ==> pending
    this.frame = frame;
    this.viewer = frame.viewer;
    this.colix = colix;
    this.strFormat = strFormat;
    setInfo(frame, atomCountPlusIndices, value, index);
  }   

  public void refresh() {
    value = frame.getMeasurement(countPlusIndices);
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
  
  void setInfo(Frame frame, int[] atomCountPlusIndices, float value, int index) {
    if (atomCountPlusIndices == null)
      count = 0;
    else {
      count = atomCountPlusIndices[0];
      this.countPlusIndices = new int[count + 1];
      System.arraycopy(atomCountPlusIndices, 0, countPlusIndices, 0, count+1);
    }
    if (countPlusIndices != null && Float.isNaN(value)) 
      value = frame.getMeasurement(countPlusIndices);
    
    this.value = value;
    this.index = index;
    formatMeasurement();
  }

  void setFormat(String strFormat) {
    this.strFormat = strFormat; 
  }

  public void formatMeasurement(String strFormat, boolean useDefault) {
    if (strFormat != null && strFormat.length() == 0)
      strFormat = null;
    if (!useDefault && strFormat != null && strFormat.indexOf(countPlusIndices[0]+":")!=0)
      return;
    setFormat(strFormat);
    formatMeasurement();
  }

  void formatMeasurement() {
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
        float radians = Measure.computeAngle(getAtomPoint3f(1), getAtomPoint3f(2), getAtomPoint3f(3), vectorBA, vectorBC, false);
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

  Point3f getAtomPoint3f(int i) {
    return frame.getAtomAt(countPlusIndices[i]);
  }

  String formatDistance(float dist) {
    int nDist = (int)(dist * 100 + 0.5f);
    float value = nDist;
    String units = viewer.getMeasureDistanceUnits();
    if (units == "nanometers") {
      units = "nm";
      value = nDist / 1000f;
    } else if (units == "picometers") {
      units = "pm";
      value = nDist;
    } else if (units == "au") {
      value = (int) (dist / JmolConstants.ANGSTROMS_PER_BOHR * 1000 + 0.5f) / 1000f;
    } else {
      units = "\u00C5"; // angstroms
      value = nDist / 100f;
    }
    return formatString(value, units);
  }

  String formatAngle(float angle) {
    angle = (int)(angle * 10 + (angle >= 0 ? 0.5f : -0.5f));
    angle /= 10;
    return formatString(angle, "\u00B0");
  }

  String formatString(float value, String units) {
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
      label = frame.atoms[countPlusIndices[i]].formatLabel(label, (char)('0' + i), null);
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
}


