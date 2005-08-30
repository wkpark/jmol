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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f;

class Measurement {

  Frame frame;
  int count;
  int[] countPlusIndices;
  String strMeasurement;

  AxisAngle4f aa;
  Point3f pointArc;
  
  Measurement(Frame frame, int[] atomCountPlusIndices) {
    this.frame = frame;
    if (atomCountPlusIndices == null)
      count = 0;
    else {
      count = atomCountPlusIndices[0];
      this.countPlusIndices = new int[count + 1];
      System.arraycopy(atomCountPlusIndices, 0, countPlusIndices, 0, count+1);
    }
    formatMeasurement();
  }

  void formatMeasurement() {
    for (int i = count; --i >= 0; )
      if (countPlusIndices[i+1] < 0) {
        strMeasurement = null;
        return;
      }
    if (count < 2)
      return;
    switch (count) {
    case 2:
      float distance = frame.getDistance(countPlusIndices[1],
                                         countPlusIndices[2]);
      strMeasurement = formatDistance(distance);
      break;
    case 3:
      float degrees = frame.getAngle(countPlusIndices[1],
                                     countPlusIndices[2],
                                     countPlusIndices[3]);
      strMeasurement = formatAngle(degrees);

      if (degrees == 180) {
        aa = null;
        pointArc = null;
      } else {
        Point3f pointA = getAtomPoint3f(1);
        Point3f pointB = getAtomPoint3f(2);
        Point3f pointC = getAtomPoint3f(3);
        
        Vector3f vectorBA = new Vector3f();
        Vector3f vectorBC = new Vector3f();
        vectorBA.sub(pointA, pointB);
        vectorBC.sub(pointC, pointB);
        float radians = vectorBA.angle(vectorBC);
        
        Vector3f vectorAxis = new Vector3f();
        vectorAxis.cross(vectorBA, vectorBC);
        aa = new AxisAngle4f(vectorAxis.x, vectorAxis.y, vectorAxis.z, radians);

        vectorBA.normalize();
        vectorBA.scale(0.5f);
        pointArc = new Point3f(vectorBA);
      }

      break;
    case 4:
      float torsion = frame.getTorsion(countPlusIndices[1],
                                       countPlusIndices[2],
                                       countPlusIndices[3],
                                       countPlusIndices[4]);

      strMeasurement = formatAngle(torsion);
      break;
    default:
      System.out.println("Invalid count to measurement shape:" + count);
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
    String units = frame.viewer.getMeasureDistanceUnits();
    if (units == "nanometers")
      return "" + (nDist / 1000.0) + " nm";
    if (units == "picometers")
      return "" + nDist + " pm";
    return "" + (nDist / 100.0) + '\u00C5'; // angstroms
  }

  String formatAngle(float angle) {
    angle = (int)(angle * 10 + (angle >= 0 ? 0.5f : -0.5f));
    angle /= 10;
    return "" + angle + '\u00B0';
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

  static float computeTorsion(Point3f p1, Point3f p2,
                              Point3f p3, Point3f p4) {

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

    float ai = (float)Math.sqrt(ai2);
    float ci = (float)Math.sqrt(ci2);
    float denom = ai * ci;
    float cross = ax * cx + ay * cy + az * cz;
    float cosang = cross * denom;
    if (cosang > 1) {
      cosang = 1;
    }
    if (cosang < -1) {
      cosang = -1;
    }

    float torsion = toDegrees((float)Math.acos(cosang));
    float dot  =  ijx*cx + ijy*cy + ijz*cz;
    float absDot =  Math.abs(dot);
    torsion = (dot/absDot > 0) ? torsion : -torsion;
    return torsion;
  }

  static float toDegrees(float angrad) {
    return angrad * 180 / (float)Math.PI;
  }
}


