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

import org.openscience.jmol.MeasurementInterface;
import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.g3d.Graphics3D;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

//import freeware.PrintfFormat;
import java.awt.Font;
import java.awt.FontMetrics;

public class MeasurementShape extends LineShape
  implements MeasurementInterface {

  public int[] atomIndices;
  private String strMeasurement;

  public MeasurementShape(JmolViewer viewer, int count, int[] atomIndices) {
    Point3d point1 = viewer.getPoint3d(atomIndices[0]);
    Point3d point2 = viewer.getPoint3d(atomIndices[1]);
    Point3d point3 = null;
    Point3d point4 = null;
    switch (count) {
    case 2:
      strMeasurement = formatDistance(point1.distance(point2));;

      pointOrigin = point1;
      pointEnd = point2;
      break;
    case 3:
      point3 = viewer.getPoint3d(atomIndices[2]);
      Vector3d vector12 = new Vector3d(point1);
      vector12.sub(point2);
      Vector3d vector32 = new Vector3d(point3);
      vector32.sub(point2);
      double angle = toDegrees(vector12.angle(vector32));
      strMeasurement = formatAngle(angle);

      pointOrigin = new Point3d(point1);
      pointOrigin.scaleAdd(3, point2);
      pointOrigin.scale(.25);
      pointEnd = new Point3d(point3);
      pointEnd.scaleAdd(3, point2);
      pointEnd.scale(.25);
      break;
    case 4:
      point3 = viewer.getPoint3d(atomIndices[2]);
      point4 = viewer.getPoint3d(atomIndices[3]);
      double dihedral = computeDihedral(point1, point2, point3, point4);
      strMeasurement = formatAngle(dihedral);

      pointOrigin = new Point3d(point1);
      pointOrigin.add(point2);
      pointOrigin.scale(0.5);
      pointEnd = new Point3d(point3);
      pointEnd.add(point4);
      pointEnd.scale(0.5);
      break;
    default:
      System.out.println("Invalid count to measurement shape:" + count);
      throw new IndexOutOfBoundsException();
    }
    this.atomIndices = new int[count];
    System.arraycopy(atomIndices, 0, this.atomIndices, 0, count);
  }

  public void render(Graphics3D g3d, JmolViewer viewer) {
    g3d.drawDottedLine(viewer.getColixDistance(), x, y, z, xEnd, yEnd, zEnd);
    if (viewer.getShowMeasurementLabels())
      paintMeasurementString(g3d, viewer);
  }


  /**
   * The format used for displaying distance values.
   */
  /*
  private static PrintfFormat distanceFormat =
    new PrintfFormat("%0.3f \u00c5");
  private static PrintfFormat angleFormat = new PrintfFormat("%0.1f\u00b0");
  private static PrintfFormat dihedralFormat = new PrintfFormat("%0.1f\u00b0");
  */
  
  String formatDistance(double dist) {
    dist = (int)(dist * 1000 + .5);
    dist /= 1000;
    return "" + dist + '\u00C5';
  }

  String formatAngle(double angle) {
    angle = (int)(angle * 10 + (angle >= 0 ? 0.5 : -0.5));
    angle /= 10;
    return "" + angle + '\u00B0';
  }

  void paintMeasurementString(Graphics3D g3d, JmolViewer viewer) {
    Font font = viewer.getMeasureFont(10);
    g3d.setFont(font);
    FontMetrics fontMetrics = g3d.getFontMetrics(font);
    int j = fontMetrics.stringWidth(strMeasurement);
    int xT = (x + xEnd) / 2;
    int yT = (y + yEnd) / 2;
    int zT = (z + zEnd) / 2;
    g3d.drawString(strMeasurement, viewer.getColixDistance(), xT, yT, zT);
  }

  public int[] getAtomList() {
    return atomIndices;
  }

  public boolean sameAs(int count, int[] atomIndices) {
    if (count != this.atomIndices.length)
      return false;
    if (count == 2)
      return ((atomIndices[0] == this.atomIndices[0] &&
               atomIndices[1] == this.atomIndices[1]) ||
              (atomIndices[0] == this.atomIndices[1] &&
               atomIndices[1] == this.atomIndices[0]));
    if (count == 3)
      return (atomIndices[1] == this.atomIndices[1] &&
              ((atomIndices[0] == this.atomIndices[0] &&
                atomIndices[2] == this.atomIndices[2]) ||
               (atomIndices[0] == this.atomIndices[2] &&
                atomIndices[2] == this.atomIndices[0])));
    return ((atomIndices[0] == this.atomIndices[0] &&
             atomIndices[1] == this.atomIndices[1] &&
             atomIndices[2] == this.atomIndices[2] &&
             atomIndices[3] == this.atomIndices[3]) ||
            (atomIndices[0] == this.atomIndices[3] &&
             atomIndices[1] == this.atomIndices[2] &&
             atomIndices[2] == this.atomIndices[1] &&
             atomIndices[3] == this.atomIndices[0]));
  }

  public double computeDihedral(Point3d p1, Point3d p2,
                                Point3d p3, Point3d p4) {

    double ijx = p1.x - p2.x;
    double ijy = p1.y - p2.y;
    double ijz = p1.z - p2.z;

    double kjx = p3.x - p2.x;
    double kjy = p3.y - p2.y;
    double kjz = p3.z - p2.z;

    double klx = p3.x - p4.x;
    double kly = p3.y - p4.y;
    double klz = p3.z - p4.z;

    double ax = ijy * kjz - ijz * kjy;
    double ay = ijz * kjx - ijx * kjz;
    double az = ijx * kjy - ijy * kjx;
    double cx = kjy * klz - kjz * kly;
    double cy = kjz * klx - kjx * klz;
    double cz = kjx * kly - kjy * klx;

    double ai2 = 1.0 / (ax * ax + ay * ay + az * az);
    double ci2 = 1.0 / (cx * cx + cy * cy + cz * cz);

    double ai = Math.sqrt(ai2);
    double ci = Math.sqrt(ci2);
    double denom = ai * ci;
    double cross = ax * cx + ay * cy + az * cz;
    double cosang = cross * denom;
    if (cosang > 1.0) {
      cosang = 1.0;
    }
    if (cosang < -1.0) {
      cosang = -1.0;
    }

    double dihedral = toDegrees(Math.acos(cosang));
    double dot  =  ijx*cx + ijy*cy + ijz*cz;
    double absDot =  Math.abs(dot);
    dihedral = (dot/absDot > 0) ? dihedral : -dihedral;
    return dihedral;
  }

  public static double toDegrees(double angrad) {
    return angrad * 180.0 / Math.PI;
  }

  public String toString() {
    String str = "[";
    int i;
    for (i = 0; i < atomIndices.length - 1; ++i)
      str += atomIndices[i] + ",";
    str += atomIndices[i] + " = " + strMeasurement + "]";
    return str;
  }
}


