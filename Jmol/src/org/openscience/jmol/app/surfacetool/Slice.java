/* $RCSfile$
 * $J. Gutow$
 * $July 2011$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.openscience.jmol.app.surfacetool;

import javax.vecmath.*;

import org.jmol.util.Measure;

public class Slice {

  //	Point3f[] vertices = new Point3f[8]; // 8 vertices (ordered to match
  // boundbox)
  Point4f leftPlane = new Point4f(); // definition of the left plane, using Jmol format
  Point4f middle = new Point4f();//plane representing center of slice.
  Point4f rightPlane = new Point4f(); // definition of the left plane
  float angleXY; // 0<=anglexy< PI/2 radians
  float anglefromZ;// 0<=anglefromZ < PI/2 radians
  float position; // distance of slice middle from origin
  float thickness; // thickness of slice
  Point3f boundBoxNegCorner;
  Point3f boundBoxPosCorner;
  Point3f boundBoxCenter;
  float diagonal;

  /*
  	class Plane extends Vector3f {
  		Vector3f[] basis;

  		Plane(float angleXY, float anglefromZ, float length) {
  			basis = new Vector3f[2];
  			basis[0] = new Vector3f();
  			basis[1] = new Vector3f();
  			this.z = (float) (Math.cos(anglefromZ) * length);
  			float projXY = (float) (Math.sin(anglefromZ) * length);
  			this.y = (float) (projXY * Math.sin(angleXY));
  			this.x = (float) (projXY * Math.cos(angleXY));
  			basis[0].x = (float) (-Math.sin(angleXY));
  			basis[0].y = (float) (Math.cos(angleXY));
  			basis[0].z = 0;
  			basis[1].x = (float) (Math.cos(anglefromZ) * Math.cos(angleXY));
  			basis[1].y = (float) (Math.cos(anglefromZ) * Math.sin(angleXY));
  			basis[1].z = (float) (-Math.sin(anglefromZ));
  		}
    }*/
  /**
   * @param length
   *        (float) length of vector from origin
   * @param angleXY
   *        (float) angle of vector projection in XY plane (radians)
   * @param anglefromZ
   *        (float) angle of vector from Z axis (radians)
   * @return (Point4f) meeting the Jmol definition of a plane.
   */
  Point4f makePlane(float length, float angleXY, float anglefromZ) {
    Point4f result = new Point4f();
    result.x = (float) (Math.cos(angleXY) * Math.sin(anglefromZ));
    result.y = (float) (Math.sin(angleXY) * Math.sin(anglefromZ));
    result.z = (float) (Math.cos(anglefromZ));
    result.w = -1 * length;
    return (result);
  }

  /**
   * Sets the right plane and left plane bounding a slice.
   * 
   * @param angleXY
   *        (float)angle in radians from X-axis to projection in XY plane
   * @param anglefromZ
   *        (float)angle in radians from z-axis to vector
   * @param position
   *        (float) position from origin of slice center along vector in
   *        molecular units
   * @param thickness
   *        (float) thickness of slice in molecular units.
   * @param boundBoxCenter
   *        (Point3f) center of the boundbox in molecular coordinates
   * @param boundBoxVec
   *        (Vector3f) vector from the boundbox center to the most positive
   *        corner.
   * @param useMolecular
   *        (boolean) if true angles and positions are relative to the origin of
   *        the molecular coordinate system. If false angles and position are
   *        relative to the center of the boundbox, which is usually more
   *        intuitive for the viewer as this is typically close to the center of
   *        the viewed object.
   */
  void setSlice(float angleXY, float anglefromZ, float position,
                float thickness, Point3f boundBoxCenter, Vector3f boundBoxVec,
                boolean useMolecular) {
    this.boundBoxNegCorner = vectoPoint(vecAdd(pointtoVec(boundBoxCenter),
        vecScale(-1, boundBoxVec)));
    this.boundBoxPosCorner = vectoPoint(vecAdd(pointtoVec(boundBoxCenter),
        boundBoxVec));
    this.boundBoxCenter = boundBoxCenter;
    this.diagonal = boundBoxPosCorner.distance(boundBoxNegCorner);
    if (angleXY >= 0 && angleXY < Math.PI) {
      this.angleXY = angleXY;
    } else {
      float fix = (float) (Math.floor(angleXY / Math.PI));
      this.angleXY = (float) (angleXY - fix * Math.PI);
    }
    if (anglefromZ >= 0 && anglefromZ < Math.PI) {
      this.anglefromZ = anglefromZ;
    } else {
      double fix = Math.floor(anglefromZ / Math.PI);
      this.anglefromZ = (float) (anglefromZ - fix * Math.PI);
    }
    this.position = position;
    this.middle = makePlane(position, angleXY, anglefromZ);
    if (!useMolecular) {
      //correct for the offset between the boundbox center and the origin
      Point3f pt = new Point3f();
      pt.x = middle.x;
      pt.y = middle.y;
      pt.z = middle.z;
      pt.scale((-1 * middle.w));
      pt.add(boundBoxCenter);
      Measure.getPlaneThroughPoint(pt, new Vector3f(middle.x, middle.y,
          middle.z), middle);
    }
    this.thickness = thickness;
    this.leftPlane = new Point4f(this.middle);
    this.leftPlane.w = middle.w + thickness / 2;
    this.rightPlane = new Point4f(this.middle);
    this.rightPlane.w = this.middle.w - thickness / 2;
  }

  /**
   * @param plane
   *        (Plane) the plane
   * @param start
   *        (Point3f) start of line segment
   * @param end
   *        (Point3f) end of line segement
   * @return a Point3f if line segment intersects plane
   */
  /*  private Point3f intersectionSegmentPlane(Plane plane, Point3f start,
                                             Point3f end) {
      Point3f intersection = new Point3f();
      Vector3f planeVec = new Vector3f(plane);
      Vector3f startVec = new Vector3f(start);
      Vector3f endVec = new Vector3f(end);
      float d = (planeVec.lengthSquared() - planeVec.dot(startVec))
          / (planeVec.dot(endVec) - planeVec.dot(startVec));
      if (d > 0 && d < 1) {
        intersection.x = start.x + d * (end.x - start.x);
        intersection.y = start.y + d * (end.y - start.y);
        intersection.z = start.z + d * (end.z - start.z);
      } else {
        intersection = null; // no intersection so don't return a value.
      }
      return (intersection);
    }*/

  /**
   * 
   * @return returns this Slice
   */
  public Slice getSlice() {
    return (this);
  }

  public Point4f getMiddle() {
    return middle;
  }

  public static Vector3f vecAdd(Vector3f v1, Vector3f v2) {
    Vector3f result = new Vector3f();
    result.x = v1.x + v2.x;
    result.y = v1.y + v2.y;
    result.z = v1.z + v2.z;
    return (result);
  }

  public static Vector3f vecScale(float scale, Vector3f v) {
    Vector3f result = new Vector3f();
    result.x = scale * v.x;
    result.y = scale * v.y;
    result.z = scale * v.z;
    return (result);
  }

  public static Point3f vectoPoint(Vector3f v) {
    Point3f result = new Point3f();
    result.x = v.x;
    result.y = v.y;
    result.z = v.z;
    return (result);
  }

  public static Vector3f pointtoVec(Point3f p) {
    Vector3f result = new Vector3f(p);
    return (result);
  }

  /*	private Point3f[] calcPlaneVert(Plane plane) {
  		Point3f[] result = new Point3f[4];
  		float scale = (float) (0.5 * diagonal);
  		Vector3f tempVec = new Vector3f();
  		tempVec = vecScale(scale, vecAdd(plane.basis[0], plane.basis[1]));
  		result[0] = vectoPoint(vecAdd(tempVec, plane));
  		result[2] = vectoPoint(vecAdd(plane, vecScale(-1, tempVec)));
  		tempVec = vecScale(scale,
  				vecAdd(plane.basis[1], vecScale(-1, plane.basis[0])));
  		result[1] = vectoPoint(vecAdd(plane, tempVec));
  		result[3] = vectoPoint(vecAdd(plane, vecScale(-1, tempVec)));
  		return (result);
  	}*/
}
