/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
import org.openscience.jmol.viewer.g3d.Colix;
import org.openscience.jmol.viewer.g3d.Shade3D;
import java.awt.Rectangle;

import java.util.Hashtable;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Quat4f;

public class ArcTest {

  JmolViewer viewer;
  Graphics3D g3d;
  Vector3f[] vertices;
  Vector3f[] verticesTransformed;
  final static int numVertices = 32;
  Matrix3f matrix1, matrix2, matrixT;
  AxisAngle4f aa1, aa2, aaT, aa;
  Quat4f quat;
  Point3f center, pointT;
  Vector3f vector1, vectorT;
  Vector3f vector2;
  Vector3f vectorAxis;

  public ArcTest(JmolViewer viewer) {
    this.viewer = viewer;
    vertices = new Vector3f[numVertices];
    verticesTransformed = new Vector3f[numVertices];
    for (int i = numVertices; --i >= 0; ) {
      double radians = 2 * Math.PI * i / numVertices;
      vertices[i] = new Vector3f((float)Math.sin(radians),
                                 (float)Math.cos(radians),
                                 0);
      verticesTransformed[i] = new Vector3f();
    }
    matrix1 = new Matrix3f();
    matrix2 = new Matrix3f();
    matrixT = new Matrix3f();
    pointT = new Point3f();
    vectorT = new Vector3f();
    center = new Point3f(1, 2, 3);
    vector1 = new Vector3f(1, 0, 0);
    vector2 = new Vector3f(0, 1, 0);
    vectorAxis = new Vector3f();
    vectorAxis.cross(vector1, vector2);
    aa = new AxisAngle4f(vectorAxis.x, vectorAxis.y, vectorAxis.z,
			 (float)Math.acos(vector1.dot(vector2)));
    aaT = new AxisAngle4f();
  }

  public void setGraphicsContext(Graphics3D g3d, Rectangle rectClip,
                                 JmolFrame frame) {
    this.g3d = g3d;
  }

  void transform() {
    /*
      for (int i = numVertices; --i >= 0; ) {
      Vector3f t = verticesTransformed[i];
      matrix.transform(vertices[i], t);
      viewer.transformVector(t, t);
      }
    */
  }

  public void render() {
    g3d.setColix(Colix.BLUE);
    /*
      for (int i = numVertices; --i >= 0; ) {
      Vector3f vertex = verticesTransformed[i];
      g3d.plotPixelClipped((int)(vertex.x + 40),
      (int)(vertex.y + 40),
      (int)(vertex.z + 40));
      }
    */
    int dotCount = 32;
    float stepAngle = aa.angle / dotCount;
    aaT.set(aa);
    for (int i = dotCount; --i >= 0; ) {
      aaT.angle = i * stepAngle;
      matrixT.set(aaT);
      pointT.set(center);
      pointT.add(vector1);
      matrixT.transform(pointT);
      g3d.drawPixel(viewer.transformPoint(pointT));
    }
  }
}
