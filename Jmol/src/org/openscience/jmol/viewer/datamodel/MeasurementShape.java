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

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.g3d.Graphics3D;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;

import java.awt.Font;
import java.awt.FontMetrics;

public class MeasurementShape extends LineShape {

  public int[] atomIndices;
  public String strMeasurement;

    public Point3f center, pointT;
    Vector3f vector21, vector23, vectorAxis;
    public int count;
    AxisAngle4f aa, aaT;
    Matrix3f matrixT;

  public MeasurementShape(JmolViewer viewer, int count, int[] atomIndices) {
    Point3f point1 = viewer.getPoint3f(atomIndices[0]);
    Point3f point2 = viewer.getPoint3f(atomIndices[1]);
    Point3f point3 = null;
    Point3f point4 = null;
    this.count = count;
    switch (count) {
    case 2:
      strMeasurement = formatDistance(point1.distance(point2));;

      pointOrigin = point1;
      pointEnd = point2;
      break;
    case 3:
      point3 = viewer.getPoint3f(atomIndices[2]);
      vector21 = new Vector3f(point1);
      vector21.sub(point2);
      vector23 = new Vector3f(point3);
      vector23.sub(point2);
      float angle = vector21.angle(vector23);
      float degrees = toDegrees(angle);
      strMeasurement = formatAngle(degrees);

      pointOrigin = new Point3f(point1);
      pointOrigin.scaleAdd(3, point2);
      pointOrigin.scale(0.25f);
      pointEnd = new Point3f(point3);
      pointEnd.scaleAdd(3, point2);
      pointEnd.scale(0.25f);

      center = point2;

      vectorAxis = new Vector3f();
      vectorAxis.cross(vector21, vector23);
      aa = new AxisAngle4f(vectorAxis.x, vectorAxis.y, vectorAxis.z, angle);

      pointT = new Point3f();
      aaT = new AxisAngle4f();
      matrixT = new Matrix3f();
      break;
    case 4:
      point3 = viewer.getPoint3f(atomIndices[2]);
      point4 = viewer.getPoint3f(atomIndices[3]);
      float dihedral = computeDihedral(point1, point2, point3, point4);
      strMeasurement = formatAngle(dihedral);

      pointOrigin = new Point3f(point1);
      pointOrigin.add(point2);
      pointOrigin.scale(0.5f);
      pointEnd = new Point3f(point3);
      pointEnd.add(point4);
      pointEnd.scale(0.5f);
      break;
    default:
      System.out.println("Invalid count to measurement shape:" + count);
      throw new IndexOutOfBoundsException();
    }
    this.atomIndices = new int[count];
    System.arraycopy(atomIndices, 0, this.atomIndices, 0, count);
  }

  public void render(Graphics3D g3d, JmolViewer viewer) {
      if (count == 3) {
	  renderArc(g3d, viewer);
      } else {
	  g3d.drawDottedLine(viewer.getColixDistance(),
			     x, y, z, xEnd, yEnd, zEnd);
	  if (viewer.getShowMeasurementLabels())
	      paintMeasurementString(g3d, viewer);
      }
  }

  public void renderArc(Graphics3D g3d, JmolViewer viewer) {
      g3d.setColix(viewer.getColixDistance());
      int dotCount = 32;
      float stepAngle = aa.angle / dotCount;
      aaT.set(aa);
      for (int i = dotCount; --i >= 0; ) {
	  aaT.angle = i * stepAngle;
	  matrixT.set(aaT);
	  pointT.set(vector21);
	  matrixT.transform(pointT);
	  pointT.add(center);
	  g3d.plotPoint(viewer.transformPoint(pointT));
      }
      int xC, yC, zC;
      Point3i screen = viewer.transformPoint(center);
      xC = screen.x; yC = screen.y; zC = screen.z;
      pointT.set(vector21);
      pointT.scale(1.5f);
      pointT.add(center);
      screen = viewer.transformPoint(pointT);
      g3d.drawDottedLine(viewer.getColixDistance(),
			 xC, yC, zC, screen.x, screen.y, screen.z);
      pointT.set(vector23);
      pointT.scale(1.5f);
      pointT.add(center);
      screen = viewer.transformPoint(pointT);
      g3d.drawDottedLine(viewer.getColixDistance(),
			 xC, yC, zC, screen.x, screen.y, screen.z);
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
  
  String formatDistance(float dist) {
    dist = (int)(dist * 1000 + 0.5f);
    dist /= 1000;
    return "" + dist + '\u00C5';
  }

  String formatAngle(float angle) {
    angle = (int)(angle * 10 + (angle >= 0 ? 0.5f : -0.5f));
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

  public float computeDihedral(Point3f p1, Point3f p2,
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

    float dihedral = toDegrees((float)Math.acos(cosang));
    float dot  =  ijx*cx + ijy*cy + ijz*cz;
    float absDot =  (float)Math.abs(dot);
    dihedral = (dot/absDot > 0) ? dihedral : -dihedral;
    return dihedral;
  }

  public static float toDegrees(float angrad) {
    return angrad * 180 / (float)Math.PI;
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


