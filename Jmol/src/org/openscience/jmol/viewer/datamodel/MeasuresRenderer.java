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

class MeasuresRenderer extends ShapeRenderer {

  short colixDistance;
  boolean showMeasurementLabels;
  short measurementMad;
  int fontsize;

  Measurement measurement;

  void render() {

    Measures measures = (Measures)shape;

    colixDistance = measures.colix;
    measurementMad = (short)-1;
    fontsize = measures.fontsize;
    showMeasurementLabels = viewer.getShowMeasurementLabels();
    
    for (int i = measures.measurementCount; --i >= 0; )
      renderMeasurement(measures.measurements[i], colixDistance);

    renderPendingMeasurement(measures.pendingMeasurement);
  }

  void renderMeasurement(Measurement measurement, short colix) {
    renderMeasurement(measurement.count, measurement, colix, true); 
  }

  void renderMeasurement(int count, Measurement measurement,
                         short colix, boolean renderArcs) {
    this.measurement = measurement;
    switch(count) {
    case 2:
      renderDistance(colix);
      break;
    case 3:
      renderAngle(colix, renderArcs);
      break;
    case 4:
      renderTorsion(colix, renderArcs);
      break;
    default:
      throw new NullPointerException();
    }
  }

  int drawSegment(int x1, int y1, int z1, int x2, int y2, int z2,
                  short colix) {
    if (measurementMad < 0) {
      g3d.drawDashedLine(colix, 4, 2, x1, y1, z1, x2, y2, z2);
      return 1;
    } else {
      System.out.println("drawSegment cylinder");
      int widthPixels = measurementMad;
      if (measurementMad >= 20)
        widthPixels = viewer.scaleToScreen((z1 + z2) / 2, measurementMad);
      g3d.fillCylinder(colix, Graphics3D.ENDCAPS_FLAT,
                       widthPixels, x1, y1, z1, x2, y2, z2);

      return (widthPixels + 1) / 2;
    }
  }

  void renderDistance(short colix) {
    renderDistance(frame.getAtomAt(measurement.atomIndices[0]),
                   frame.getAtomAt(measurement.atomIndices[1]), colix);
  }
                           

  void renderDistance(Atom atomA, Atom atomB, short colix) {
    int zA = atomA.z - atomA.diameter - 10;
    int zB = atomB.z - atomB.diameter - 10;
    int radius = drawSegment(atomA.x, atomA.y, zA, atomB.x, atomB.y, zB,
                             colix);
    paintMeasurementString((atomA.x + atomB.x) / 2,
                           (atomA.y + atomB.y) / 2,
                           ((zA + zB) / 2), radius, colix);
  }
                           

  AxisAngle4f aaT = new AxisAngle4f();
  Matrix3f matrixT = new Matrix3f();
  Point3f pointT = new Point3f();

  void renderAngle(short colix, boolean renderArcs) {
    renderAngle(frame.getAtomAt(measurement.atomIndices[0]),
                frame.getAtomAt(measurement.atomIndices[1]),
                frame.getAtomAt(measurement.atomIndices[2]),
                colix, renderArcs);
  }

  void renderAngle(Atom atomA, Atom atomB, Atom atomC,
                   short colix, boolean renderArcs) {
    g3d.setColix(colix);
    int zA = atomA.z - atomA.diameter - 10;
    int zB = atomB.z - atomB.diameter - 10;
    int zC = atomC.z - atomC.diameter - 10;
    int zOffset = (zA + zB + zC) / 3;
    int radius = drawSegment(atomA.x, atomA.y, zA, atomB.x, atomB.y, zB,
                             colix);
    radius += drawSegment(atomB.x, atomB.y, zB, atomC.x, atomC.y, zC, colix);
    radius = (radius + 1) / 2;

    if (! renderArcs)
      return;

    // FIXME mth -- this really should be a function of pixelsPerAngstrom
    // in addition, the location of the arc is not correct
    // should probably be some percentage of the smaller distance
    AxisAngle4f aa = measurement.aa;
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
        paintMeasurementString(screenLabel.x, screenLabel.y, zLabel,
                               radius, colix);
      }
    }
  }

  void renderTorsion(short colix, boolean renderArcs) {
    renderTorsion(frame.getAtomAt(measurement.atomIndices[0]),
                  frame.getAtomAt(measurement.atomIndices[1]),
                  frame.getAtomAt(measurement.atomIndices[2]),
                  frame.getAtomAt(measurement.atomIndices[3]),
                  colix, renderArcs);
  }

  void renderTorsion(Atom atomA, Atom atomB, Atom atomC, Atom atomD,
                     short colix, boolean renderArcs) {
    int zA = atomA.z - atomA.diameter - 10;
    int zB = atomB.z - atomB.diameter - 10;
    int zC = atomC.z - atomC.diameter - 10;
    int zD = atomD.z - atomD.diameter - 10;
    int radius = drawSegment(atomA.x, atomA.y, zA, atomB.x, atomB.y, zB,
                             colix);
    radius += drawSegment(atomB.x, atomB.y, zB, atomC.x, atomC.y, zC, colix);
    radius += drawSegment(atomC.x, atomC.y, zC, atomD.x, atomD.y, zD, colix);
    radius /= 3;
    paintMeasurementString((atomA.x + atomB.x + atomC.x + atomD.x) / 4,
                           (atomA.y + atomB.y + atomC.y + atomD.y) / 4,
                           (zA + zB + zC + zD) / 4, radius, colix);
  }

  void paintMeasurementString(int x, int y, int z, int radius, short colix) {
    if (! showMeasurementLabels)
      return;
    String strMeasurement = measurement.strMeasurement;
    // I *think* that the string "" is always interned
    if (strMeasurement == null || strMeasurement == "")
      return;
    g3d.setFontOfSize(fontsize);
    FontMetrics fontMetrics = g3d.getFontMetrics();
    int j = fontMetrics.stringWidth(strMeasurement);
    g3d.drawString(strMeasurement, colix,
                   x+radius/2+2, y-radius/2, z - radius - 2);
  }

  void renderPendingMeasurement(PendingMeasurement pendingMeasurement) {
    int count = pendingMeasurement.count;
    int[] indices = pendingMeasurement.atomIndices;
    if (! pendingMeasurement.isActive || count < 2)
      return;
    if (indices[count - 1] == -1)
      renderPendingWithCursor(pendingMeasurement);
    else
      renderMeasurement(pendingMeasurement, Graphics3D.PINK);
  }
  
  void renderPendingWithCursor(PendingMeasurement pendingMeasurement) {
    int count = pendingMeasurement.count;
    if (count < 2)
      return;
    if (count > 2)
      renderMeasurement(count - 1, pendingMeasurement, Graphics3D.PINK, false);
    Atom atomLast = frame.getAtomAt(pendingMeasurement.atomIndices[count - 2]);
    int lastZ = atomLast.z - atomLast.diameter - 10;
    drawSegment(atomLast.x, atomLast.y, lastZ,
                viewer.getCursorX(), viewer.getCursorY(), 0, Graphics3D.PINK);
  }
}
