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

import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f;
import java.util.Vector;

class Measurement {

  final static float radiansPerDegree = (float) (2 * Math.PI / 360);

  Frame frame;
  Viewer viewer;
  int count;
  int[] countPlusIndices;
  String strMeasurement;
  String strFormat;
  float value;
  boolean isVisible = true;
  boolean isHidden = false;
  boolean isDynamic = false;
  short colix;
  int index;
  
  AxisAngle4f aa;
  Point3f pointArc;
  
  Measurement(Frame frame, int[] atomCountPlusIndices, float value,
      short colix, String strFormat, int index) {
    //value Float.isNaN ==> pending
    this.frame = frame;
    this.viewer = frame.viewer;
    this.colix = colix;
    this.strFormat = strFormat;
    setInfo(frame, atomCountPlusIndices, value, index);
  }   

  void refresh() {
    value = frame.getMeasurement(countPlusIndices);
    formatMeasurement();
  }
  
  /**
   * Used by MouseManager and Picking Manager to build the script
   * @param countPlusIndexes
   * @return measure (atomIndex=1) (atomIndex=2)....
   */
  static String getMeasurementScript(int[] countPlusIndexes) {
    String str = "measure";
    int nAtoms = countPlusIndexes[0];
    for (int i = 0; i < nAtoms; i++) {
      str += " (atomIndex=" + countPlusIndexes[i + 1] + ")"; 
    }
    return str;  
  }
  
  void setIndex(int index) {
    this.index = index;
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

  static float computeAngle(Point3f pointA, Point3f pointB, Point3f pointC, boolean asDegrees) {
    Vector3f vectorBA = new Vector3f();
    Vector3f vectorBC = new Vector3f();        
    return computeAngle(pointA, pointB, pointC, vectorBA, vectorBC, asDegrees);
  }

  static float computeAngle(Point3f pointA, Point3f pointB, Point3f pointC, Vector3f vectorBA, Vector3f vectorBC, boolean asDegrees) {
    vectorBA.sub(pointA, pointB);
    vectorBC.sub(pointC, pointB);
    float angle = vectorBA.angle(vectorBC);
    return (asDegrees ? angle / radiansPerDegree : angle);
  }

  static float computeTorsion(Point3f p1, Point3f p2, Point3f p3, Point3f p4, boolean asDegrees) {

    float ijx = p1.x - p2.x;
    float ijy = p1.y - p2.y;
    float ijz = p1.z - p2.z;

    float kjx = p3.x - p2.x;
    float kjy = p3.y - p2.y;
    float kjz = p3.z - p2.z;

    float klx = p3.x - p4.x;
    float kly = p3.y - p4.y;
    float klz = p3.z - p4.z;

    float ax = ijy * kjz - ijz * kjy;
    float ay = ijz * kjx - ijx * kjz;
    float az = ijx * kjy - ijy * kjx;
    float cx = kjy * klz - kjz * kly;
    float cy = kjz * klx - kjx * klz;
    float cz = kjx * kly - kjy * klx;

    float ai2 = 1f / (ax * ax + ay * ay + az * az);
    float ci2 = 1f / (cx * cx + cy * cy + cz * cz);

    float ai = (float) Math.sqrt(ai2);
    float ci = (float) Math.sqrt(ci2);
    float denom = ai * ci;
    float cross = ax * cx + ay * cy + az * cz;
    float cosang = cross * denom;
    if (cosang > 1) {
      cosang = 1;
    }
    if (cosang < -1) {
      cosang = -1;
    }

    float torsion = (float) Math.acos(cosang);
    float dot = ijx * cx + ijy * cy + ijz * cz;
    float absDot = Math.abs(dot);
    torsion = (dot / absDot > 0) ? torsion : -torsion;
    return (asDegrees ? torsion / radiansPerDegree : torsion);
  }

  void setFormat(String strFormat) {
   this.strFormat = strFormat; 
//   System.out.println("setFOrmat" + strFormat);
  }

  void formatMeasurement(String strFormat, boolean useDefault) {
    if (strFormat != null && strFormat.length() == 0)
      strFormat = null;
    if (!useDefault && strFormat != null && strFormat.indexOf(countPlusIndices[0]+":")!=0)
      return;
    setFormat(strFormat);
    formatMeasurement();
  }

  void formatMeasurement() {
    strMeasurement = null;
 //   System.out.println("formatMeaurement" + index + " " + strFormat);
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
        float radians = computeAngle(getAtomPoint3f(1), getAtomPoint3f(2), getAtomPoint3f(3), vectorBA, vectorBC, false);
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
  
  void reformatDistanceIfSelected() {
    if (count != 2)
      return;
    Viewer viewer = frame.viewer;
    if (viewer.isSelected(countPlusIndices[1]) &&
        viewer.isSelected(countPlusIndices[2]))
      formatMeasurement();
  }

  Point3f getAtomPoint3f(int i) {
    return frame.getAtomPoint3f(countPlusIndices[i]);
  }

  String formatDistance(float dist) {
    int nDist = (int)(dist * 100 + 0.5f);
    float value = nDist;
    String units = frame.viewer.getMeasureDistanceUnits();
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
    String s = countPlusIndices[0]+":";
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

  boolean sameAs(int[] atomCountPlusIndices) {
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

  Vector toVector() {
    Vector V = new Vector();
    for (int i = 0; i < count + 1; i++ ) V.add(new Integer(countPlusIndices[i]));
    V.add(strMeasurement);
    return V;  
  }
}


