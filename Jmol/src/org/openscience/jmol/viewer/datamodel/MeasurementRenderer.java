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

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;

import java.awt.Font;
import java.awt.FontMetrics;

class MeasurementRenderer extends Renderer {

  MeasurementRenderer(JmolViewer viewer, FrameRenderer frameRenderer) {
    this.viewer = viewer;
    this.frameRenderer = frameRenderer;
  }

  short colixDistance;
  boolean showMeasurementLabels;

  Measurement measurement;

  void render() {

    colixDistance = viewer.getColixDistance();
    showMeasurementLabels = viewer.getShowMeasurementLabels();

    Measurement[] measurements = frame.measurements;
    for (int i = frame.measurementCount; --i >= 0; )
      render(measurements[i]);
  }

  void render(Measurement measurement) {
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
  }

  void renderDistance() {
    Atom atomA = frame.getAtomAt(measurement.atomIndices[0]);
    Atom atomB = frame.getAtomAt(measurement.atomIndices[1]);
    int diamMax = atomA.diameter;
    if (atomB.diameter > diamMax)
      diamMax = atomB.diameter;
    int zOffset = diamMax;
    int zA = atomA.z - zOffset;
    if (zA < 0) zA = 0;
    int zB = atomB.z - zOffset;
    if (zB < 0) zB = 0;
    g3d.drawDottedLine(colixDistance,
                       atomA.x, atomA.y, zA, atomB.x, atomB.y, zB);
    paintMeasurementString((atomA.x + atomB.x) / 2,
                           (atomA.y + atomB.y) / 2,
                           (zA + zB) / 2);
  }
                           

  void renderDihedral() {
    Atom atomA = frame.getAtomAt(measurement.atomIndices[0]);
    Atom atomB = frame.getAtomAt(measurement.atomIndices[1]);
    Atom atomC = frame.getAtomAt(measurement.atomIndices[2]);
    Atom atomD = frame.getAtomAt(measurement.atomIndices[3]);
    int diamMax = atomA.diameter;
    if (atomB.diameter > diamMax)
      diamMax = atomB.diameter;
    if (atomC.diameter > diamMax)
      diamMax = atomC.diameter;
    if (atomD.diameter > diamMax)
      diamMax = atomD.diameter;
    int zOffset = diamMax;
    int zA = atomA.z - zOffset;
    if (zA < 0) zA = 0;
    int zB = atomB.z - zOffset;
    if (zB < 0) zB = 0;
    int zC = atomC.z - zOffset;
    if (zC < 0) zC = 0;
    int zD = atomD.z - zOffset;
    if (zD < 0) zD = 0;
    g3d.drawDottedLine(colixDistance,
                       atomA.x, atomA.y, zA, atomB.x, atomB.y, zB);
    g3d.drawDottedLine(colixDistance,
                       atomB.x, atomB.y, zB, atomC.x, atomC.y, zC);
    g3d.drawDottedLine(colixDistance,
                       atomC.x, atomC.y, zC, atomD.x, atomD.y, zD);
    paintMeasurementString((atomA.x + atomB.x + atomC.x + atomD.x) / 4,
                           (atomA.y + atomB.y + atomC.y + atomD.y) / 4,
                           (zA + zB + zC + zD) / 4);
  }

  AxisAngle4f aaT = new AxisAngle4f();
  Matrix3f matrixT = new Matrix3f();
  Point3f pointT = new Point3f();

  void renderAngle() {
    g3d.setColix(colixDistance);
    Atom atomA = frame.getAtomAt(measurement.atomIndices[0]);
    Atom atomB = frame.getAtomAt(measurement.atomIndices[1]);
    Atom atomC = frame.getAtomAt(measurement.atomIndices[2]);
    int diamMax = atomA.diameter;
    if (atomB.diameter > diamMax)
      diamMax = atomB.diameter;
    if (atomC.diameter > diamMax)
      diamMax = atomC.diameter;
    int zOffset = diamMax;
    int zA = atomA.z - zOffset;
    if (zA < 0) zA = 0;
    int zB = atomB.z - zOffset;
    if (zB < 0) zB = 0;
    int zC = atomC.z - zOffset;
    if (zC < 0) zC = 0;
    g3d.drawDottedLine(colixDistance,
                       atomA.x, atomA.y, zA, atomB.x, atomB.y, zB);
    g3d.drawDottedLine(colixDistance,
                       atomB.x, atomB.y, zB, atomC.x, atomC.y, zC);

    AxisAngle4f aa = measurement.aa;
    // FIXME mth -- this really should be a function of pixelsPerAngstrom
    int dotCount = (int)((aa.angle / (2 * Math.PI)) * 64);
    float stepAngle = aa.angle / dotCount;
    aaT.set(aa);
    int iMid = dotCount / 2;
    for (int i = dotCount; --i >= 0; ) {
      aaT.angle = i * stepAngle;
      matrixT.set(aaT);
      pointT.set(measurement.pointArc);
      matrixT.transform(pointT);
      pointT.add(atomB.point3f);
      Point3i screenArc = viewer.transformPoint(pointT);
      int zArc = screenArc.z - zOffset;
      if (zArc < 0) zArc = 0;
      g3d.drawPixel(screenArc.x, screenArc.y, zArc);
      if (i == iMid) {
        pointT.set(measurement.pointArc);
        pointT.scale(1.1f);
        matrixT.transform(pointT);
        pointT.add(atomB.point3f);
        Point3i screenLabel = viewer.transformPoint(pointT);
        int zLabel = screenLabel.z - zOffset;
        if (zLabel < 0) zLabel = 0;
        paintMeasurementString(screenLabel.x, screenLabel.y, zLabel);
      }
    }
  }

  void paintMeasurementString(int x, int y, int z) {
    if (! showMeasurementLabels)
      return;
    String strMeasurement = measurement.strMeasurement;
    Font font = viewer.getMeasureFont(10);
    g3d.setFont(font);
    FontMetrics fontMetrics = g3d.getFontMetrics(font);
    int j = fontMetrics.stringWidth(strMeasurement);
    g3d.drawString(strMeasurement, colixDistance, x+2, y, z);
  }
}
