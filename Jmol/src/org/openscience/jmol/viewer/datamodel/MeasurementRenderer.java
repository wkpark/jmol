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
import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;

import java.awt.Font;
import java.awt.FontMetrics;

public class MeasurementRenderer {

  JmolViewer viewer;
  public MeasurementRenderer(JmolViewer viewer) {
    this.viewer = viewer;
  }

  Graphics3D g3d;
  Rectangle clip;
  JmolFrame frame;

  short colixDistance;
  boolean showMeasurementLabels;

  MeasurementShape measurement;

  public void setGraphicsContext(Graphics3D g3d, Rectangle clip, JmolFrame frame) {
    this.g3d = g3d;
    this.clip = clip;
    this.frame = frame;

    colixDistance = viewer.getColixDistance();
    showMeasurementLabels = viewer.getShowMeasurementLabels();
  }

  public void render(MeasurementShape measurement) {
    this.measurement = measurement;
    switch(measurement.count) {
    case 2:
      renderDistance();
      break;
    case 3:
      renderAngle();
      break;
    case 4:
      renderDihedral();
      break;
    default:
      throw new NullPointerException();
    }
    if (showMeasurementLabels)
      paintMeasurementString();
  }

  void renderDistance() {
    g3d.drawDottedLine(colixDistance,
                       measurement.x, measurement.y, measurement.z,
                       measurement.xEnd, measurement.yEnd, measurement.zEnd);
  }

  void renderDihedral() {
    g3d.drawDottedLine(colixDistance,
                       measurement.x, measurement.y, measurement.z,
                       measurement.xEnd, measurement.yEnd, measurement.zEnd);
  }

  AxisAngle4f aaT = new AxisAngle4f();
  Matrix3f matrixT = new Matrix3f();
  Point3f pointT = new Point3f();

  void renderAngle() {
    g3d.setColix(colixDistance);
    AxisAngle4f aa = measurement.aa;
    Vector3f vector21 = measurement.vector21;
    // FIXME mth -- this really should be a function of pixelsPerAngstrom
    int dotCount = (int)((aa.angle / (2 * Math.PI)) * 64);
    float stepAngle = aa.angle / dotCount;
    aaT.set(aa);
    for (int i = dotCount; --i >= 0; ) {
      aaT.angle = i * stepAngle;
      matrixT.set(aaT);
      pointT.set(vector21);
      pointT.scale(0.75f);
      matrixT.transform(pointT);
      pointT.add(measurement.center);
      g3d.plotPoint(viewer.transformPoint(pointT));
    }
    int xC, yC, zC;
    Point3i pointC = viewer.transformPoint(measurement.center);
    g3d.drawDottedLine(colixDistance,
                       pointC.x, pointC.y, pointC.z, measurement.x, measurement.y, measurement.z);
    g3d.drawDottedLine(colixDistance,
                       pointC.x, pointC.y, pointC.z, measurement.xEnd, measurement.yEnd, measurement.zEnd);
  }

  void paintMeasurementString() {
    String strMeasurement = measurement.strMeasurement;
    Font font = viewer.getMeasureFont(10);
    g3d.setFont(font);
    FontMetrics fontMetrics = g3d.getFontMetrics(font);
    int j = fontMetrics.stringWidth(strMeasurement);
    int xT = (measurement.x + measurement.xEnd) / 2;
    int yT = (measurement.y + measurement.yEnd) / 2;
    int zT = (measurement.z + measurement.zEnd) / 2;
    g3d.drawString(strMeasurement, colixDistance, xT, yT, zT);
  }
}
