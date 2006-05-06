/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import org.jmol.g3d.*;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;

class MeasuresRenderer extends ShapeRenderer {

  short colix;
  boolean showMeasurementNumbers;
  short measurementMad;
  Font3D font3d;

  Measurement measurement;

  void render() {

    Measures measures = (Measures)shape;

    colix = measures.colix;
    if (colix == 0)
      colix = viewer.getColixBackgroundContrast();
    measurementMad = measures.mad;
    font3d = measures.font3d;
    showMeasurementNumbers = measures.showMeasurementNumbers;
    
    for (int i = measures.measurementCount; --i >= 0; )
      renderMeasurement(measures.measurements[i], colix);

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
    }
    int widthPixels = measurementMad;
    if (measurementMad >= 20)
      widthPixels = viewer.scaleToScreen((z1 + z2) / 2, measurementMad);
    g3d.fillCylinder(colix, Graphics3D.ENDCAPS_FLAT,
                     widthPixels, x1, y1, z1, x2, y2, z2);

    return (widthPixels + 1) / 2;
  }

  void renderDistance(short colix) {
    renderDistance(frame.getAtomAt(measurement.countPlusIndices[1]),
                   frame.getAtomAt(measurement.countPlusIndices[2]), colix);
  }

  void renderDistance(Atom atomA, Atom atomB, short colix) {
    /*
      Miguel commented this out on 31 Aug 2005
      and sent an email to the list asking why it would have been here
    if (! (atomA.isVisible() && atomB.isVisible()))
      return;

      1 Sep 2005 ... the previous hack was put in to control the
      display of measurements when there are multiple models.
      The code below should do a better job ... Miguel
    */
    if (displayModelIndex >= 0 &&
        (displayModelIndex != atomA.modelIndex ||
         displayModelIndex != atomB.modelIndex))
      return;
    int zA = atomA.getScreenZ() - atomA.getScreenD() - 10;
    int zB = atomB.getScreenZ() - atomB.getScreenD() - 10;
    int radius = drawSegment(atomA.getScreenX(), atomA.getScreenY(), zA, atomB.getScreenX(), atomB.getScreenY(), zB,
                             colix);
    paintMeasurementString((atomA.getScreenX() + atomB.getScreenX()) / 2,
                           (atomA.getScreenY() + atomB.getScreenY()) / 2,
                           ((zA + zB) / 2), radius, colix);
  }
                           

  AxisAngle4f aaT = new AxisAngle4f();
  Matrix3f matrixT = new Matrix3f();
  Point3f pointT = new Point3f();

  void renderAngle(short colix, boolean renderArcs) {
    renderAngle(frame.getAtomAt(measurement.countPlusIndices[1]),
                frame.getAtomAt(measurement.countPlusIndices[2]),
                frame.getAtomAt(measurement.countPlusIndices[3]),
                colix, renderArcs);
  }

  void renderAngle(Atom atomA, Atom atomB, Atom atomC,
                   short colix, boolean renderArcs) {
    /*
    if (! (atomA.isVisible() && atomB.isVisible() && atomC.isVisible()))
      return;
    */
    if (displayModelIndex >= 0 &&
        (displayModelIndex != atomA.modelIndex ||
         displayModelIndex != atomB.modelIndex ||
         displayModelIndex != atomC.modelIndex))
      return;
    g3d.setColix(colix);
    int zA = atomA.getScreenZ() - atomA.getScreenD() - 10;
    int zB = atomB.getScreenZ() - atomB.getScreenD() - 10;
    int zC = atomC.getScreenZ() - atomC.getScreenD() - 10;
    int zOffset = (zA + zB + zC) / 3;
    int radius = drawSegment(atomA.getScreenX(), atomA.getScreenY(), zA,
                             atomB.getScreenX(), atomB.getScreenY(), zB,
                             colix);
    radius += drawSegment(atomB.getScreenX(), atomB.getScreenY(), zB,
                          atomC.getScreenX(), atomC.getScreenY(), zC, colix);
    radius = (radius + 1) / 2;

    if (! renderArcs)
      return;


    // FIXME mth -- this really should be a function of pixelsPerAngstrom
    // in addition, the location of the arc is not correct
    // should probably be some percentage of the smaller distance
    AxisAngle4f aa = measurement.aa;
    if (aa == null) { // 180 degrees
      paintMeasurementString(atomB.getScreenX() + 5, atomB.getScreenY() - 5,
                             zB, radius, colix);
      return;
    }
    int dotCount = (int)((aa.angle / (2 * Math.PI)) * 64);
    float stepAngle = aa.angle / dotCount;
    aaT.set(aa);
    int iMid = dotCount / 2;
    for (int i = dotCount; --i >= 0; ) {
      aaT.angle = i * stepAngle;
      matrixT.set(aaT);
      pointT.set(measurement.pointArc);
      matrixT.transform(pointT);
      pointT.add(atomB);
      Point3i screenArc = viewer.transformPoint(pointT);
      int zArc = screenArc.z - zOffset;
      if (zArc < 0) zArc = 0;
      g3d.drawPixel(screenArc.x, screenArc.y, zArc);
      if (i == iMid) {
        pointT.set(measurement.pointArc);
        pointT.scale(1.1f);
        matrixT.transform(pointT);
        pointT.add(atomB);
        Point3i screenLabel = viewer.transformPoint(pointT);
        int zLabel = screenLabel.z - zOffset;
        paintMeasurementString(screenLabel.x, screenLabel.y, zLabel,
                               radius, colix);
      }
    }
  }

  void renderTorsion(short colix, boolean renderArcs) {
    int[] countPlusIndices = measurement.countPlusIndices;
    renderTorsion(frame.getAtomAt(countPlusIndices[1]),
                  frame.getAtomAt(countPlusIndices[2]),
                  frame.getAtomAt(countPlusIndices[3]),
                  frame.getAtomAt(countPlusIndices[4]),
                  colix, renderArcs);
  }

  void renderTorsion(Atom atomA, Atom atomB, Atom atomC, Atom atomD,
                     short colix, boolean renderArcs) {
    /*
    if (! (atomA.isVisible() && atomB.isVisible() &&
           atomC.isVisible() && atomD.isVisible()))
      return;
    */
    if (displayModelIndex >= 0 &&
        (displayModelIndex != atomA.modelIndex ||
         displayModelIndex != atomB.modelIndex ||
         displayModelIndex != atomC.modelIndex ||
         displayModelIndex != atomD.modelIndex))
      return;
    int zA = atomA.getScreenZ() - atomA.getScreenD() - 10;
    int zB = atomB.getScreenZ() - atomB.getScreenD() - 10;
    int zC = atomC.getScreenZ() - atomC.getScreenD() - 10;
    int zD = atomD.getScreenZ() - atomD.getScreenD() - 10;
    int radius = drawSegment(atomA.getScreenX(), atomA.getScreenY(), zA, atomB.getScreenX(), atomB.getScreenY(), zB,
                             colix);
    radius += drawSegment(atomB.getScreenX(), atomB.getScreenY(), zB, atomC.getScreenX(), atomC.getScreenY(), zC, colix);
    radius += drawSegment(atomC.getScreenX(), atomC.getScreenY(), zC, atomD.getScreenX(), atomD.getScreenY(), zD, colix);
    radius /= 3;
    paintMeasurementString((atomA.getScreenX() + atomB.getScreenX() + atomC.getScreenX() + atomD.getScreenX()) / 4,
                           (atomA.getScreenY() + atomB.getScreenY() + atomC.getScreenY() + atomD.getScreenY()) / 4,
                           (zA + zB + zC + zD) / 4, radius, colix);
  }

  void paintMeasurementString(int x, int y, int z, int radius, short colix) {
    if (! showMeasurementNumbers)
      return;
    String strMeasurement = measurement.strMeasurement;
    if (strMeasurement == null)
      return;
    int xT = x + radius/2 + 2;
    int yT = y - radius/2;
    int zT = z - radius - 2;
    if (zT < 1)
      zT = 1;
    g3d.drawString(strMeasurement, font3d, colix, xT, yT, zT);
  }

  void renderPendingMeasurement(PendingMeasurement pendingMeasurement) {
    int count = pendingMeasurement.count;
    int[] countPlusIndices = pendingMeasurement.countPlusIndices;
    if (! pendingMeasurement.isActive || count < 2)
      return;
    short colixRubberband = viewer.getColixRubberband();
    if (countPlusIndices[count] == -1)
      renderPendingWithCursor(pendingMeasurement, colixRubberband);
    else
      renderMeasurement(pendingMeasurement, colixRubberband);
  }
  
  void renderPendingWithCursor(PendingMeasurement pendingMeasurement,
                               short colixRubberband) {
    int count = pendingMeasurement.count;
    if (count < 2)
      return;
    if (count > 2)
      renderMeasurement(count - 1, pendingMeasurement, colixRubberband, false);
    Atom atomLast = frame.getAtomAt(pendingMeasurement.
                                    countPlusIndices[count - 1]);
    int lastZ = atomLast.getScreenZ() - atomLast.getScreenD() - 10;
    drawSegment(atomLast.getScreenX(), atomLast.getScreenY(), lastZ,
                viewer.getCursorX(), viewer.getCursorY(), 0, colixRubberband);
  }
}
