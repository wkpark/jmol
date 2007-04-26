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

package org.jmol.shape;

import org.jmol.g3d.*;
import org.jmol.modelframe.Atom;
import org.jmol.viewer.Measurement;
import org.jmol.viewer.PendingMeasurement;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;

public class MeasuresRenderer extends ShapeRenderer {

  boolean showMeasurementNumbers;
  short measurementMad;
  Font3D font3d;
  Measurement measurement;
  boolean doJustify;
  short colix;
  protected void render() {
    if (!viewer.getShowMeasurements() || !g3d.checkTranslucent(false))
      return;

    Measures measures = (Measures) shape;
    doJustify = viewer.getJustifyMeasurements();
    measurementMad = measures.mad;
    font3d = measures.font3d;
    showMeasurementNumbers = measures.showMeasurementNumbers;
    measures.setVisibilityInfo();
    boolean dynamicMeasurements = viewer.getDynamicMeasurements();
    for (int i = measures.measurementCount; --i >= 0;) {
      Measurement m = measures.measurements[i];
      if (dynamicMeasurements || m.isDynamic())
        m.refresh();
      if (!m.isVisible())
        continue;
      colix = m.getColix();
      if (colix == 0)
        colix = measures.colix;
      if (colix == 0)
        colix = viewer.getColixBackgroundContrast();
      g3d.setColix(colix);
      renderMeasurement(m);
    }
    renderPendingMeasurement(measures.pendingMeasurement);
  }

  void renderMeasurement(Measurement measurement) {
    renderMeasurement(measurement.getCount(), measurement, true); 
  }

  void renderMeasurement(int count, Measurement measurement, boolean renderArcs) {
    this.measurement = measurement;
    switch(count) {
    case 2:
      renderDistance();
      break;
    case 3:
      renderAngle(renderArcs);
      break;
    case 4:
      renderTorsion(renderArcs);
      break;
    default:
      throw new NullPointerException();
    }
  }

  int drawSegment(int x1, int y1, int z1, int x2, int y2, int z2) {
    if (measurementMad < 0) {
      g3d.drawDashedLine(4, 2, x1, y1, z1, x2, y2, z2);
      return 1;
    }
    int widthPixels = measurementMad;
    if (measurementMad >= 20)
      widthPixels = viewer.scaleToScreen((z1 + z2) / 2, measurementMad);
    g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT,
                     widthPixels, x1, y1, z1, x2, y2, z2);

    return (widthPixels + 1) / 2;
  }

  void renderDistance() {
    
    renderDistance(frame.getAtomAt(measurement.getIndex(1)),
                   frame.getAtomAt(measurement.getIndex(2)));
  }

  void renderDistance(Atom atomA, Atom atomB) {
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - atomB.screenDiameter - 10;
    int radius = drawSegment(atomA.screenX, atomA.screenY, zA, atomB
        .screenX, atomB.screenY, zB);
    int z = (zA + zB) / 2;
    if (z < 1)
      z = 1;
    int x = (atomA.screenX + atomB.screenX) / 2;
    int y = (atomA.screenY + atomB.screenY) / 2;
    paintMeasurementString(x, y, z, radius, (x - atomA.screenX)*(y-atomA.screenY) > 0, 0);
  }
                           

  AxisAngle4f aaT = new AxisAngle4f();
  Matrix3f matrixT = new Matrix3f();
  Point3f pointT = new Point3f();

  void renderAngle(boolean renderArcs) {
    renderAngle(frame.getAtomAt(measurement.getIndex(1)),
                frame.getAtomAt(measurement.getIndex(2)),
                frame.getAtomAt(measurement.getIndex(3)),
                renderArcs);
  }

  void renderAngle(Atom atomA, Atom atomB, Atom atomC,
                   boolean renderArcs) {
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - atomB.screenDiameter - 10;
    int zC = atomC.screenZ - atomC.screenDiameter - 10;
    int zOffset = (zA + zB + zC) / 3;
    int radius = drawSegment(atomA.screenX, atomA.screenY, zA,
                             atomB.screenX, atomB.screenY, zB);
    radius += drawSegment(atomB.screenX, atomB.screenY, zB,
                          atomC.screenX, atomC.screenY, zC);
    radius = (radius + 1) / 2;

    if (! renderArcs)
      return;


    // FIXME mth -- this really should be a function of pixelsPerAngstrom
    // in addition, the location of the arc is not correct
    // should probably be some percentage of the smaller distance
    AxisAngle4f aa = measurement.getAxisAngle();
    if (aa == null) { // 180 degrees
      paintMeasurementString(atomB.screenX + 5, atomB.screenY - 5,
                             zB, radius, false, 0);
      return;
    }
    int dotCount = (int)((aa.angle / (2 * Math.PI)) * 64);
    float stepAngle = aa.angle / dotCount;
    aaT.set(aa);
    int iMid = dotCount / 2;
    Point3f ptArc = measurement.getPointArc();
    for (int i = dotCount; --i >= 0; ) {
      aaT.angle = i * stepAngle;
      matrixT.set(aaT);
      pointT.set(ptArc);
      matrixT.transform(pointT);
      pointT.add(atomB);
      //CAUTION! screenArc and screenLabel are the SAME OBJECT, TransformManager.point3iScreenTemp
      Point3i screenArc = viewer.transformPoint(pointT);
      int zArc = screenArc.z - zOffset;
      if (zArc < 0) zArc = 0;
      g3d.drawPixel(screenArc.x, screenArc.y, zArc);
      if (i == iMid) {
        pointT.set(ptArc);
        pointT.scale(1.1f);
        matrixT.transform(pointT);
        pointT.add(atomB);
        Point3i screenLabel = viewer.transformPoint(pointT);
        int zLabel = screenLabel.z - zOffset;
        paintMeasurementString(screenLabel.x, screenLabel.y, zLabel,
                               radius, screenLabel.x < atomB.screenX, atomB.screenY);
      }
    }
  }

  void renderTorsion(boolean renderArcs) {
    renderTorsion(frame.getAtomAt(measurement.getIndex(1)),
                  frame.getAtomAt(measurement.getIndex(2)),
                  frame.getAtomAt(measurement.getIndex(3)),
                  frame.getAtomAt(measurement.getIndex(4)),
                  renderArcs);
  }

  void renderTorsion(Atom atomA, Atom atomB, Atom atomC, Atom atomD,
                     boolean renderArcs) {
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - atomB.screenDiameter - 10;
    int zC = atomC.screenZ - atomC.screenDiameter - 10;
    int zD = atomD.screenZ - atomD.screenDiameter - 10;
    int radius = drawSegment(atomA.screenX, atomA.screenY, zA, atomB.screenX, atomB.screenY, zB);
    radius += drawSegment(atomB.screenX, atomB.screenY, zB, atomC.screenX, atomC.screenY, zC);
    radius += drawSegment(atomC.screenX, atomC.screenY, zC, atomD.screenX, atomD.screenY, zD);
    radius /= 3;
    paintMeasurementString((atomA.screenX + atomB.screenX + atomC.screenX + atomD.screenX) / 4,
                           (atomA.screenY + atomB.screenY + atomC.screenY + atomD.screenY) / 4,
                           (zA + zB + zC + zD) / 4, radius, false, 0);
  }

  void paintMeasurementString(int x, int y, int z, int radius,
                              boolean rightJustify, int yRef) {
    if (!showMeasurementNumbers)
      return;
    if (!doJustify) {
      rightJustify = false;
      yRef = y;
    }
    String strMeasurement = measurement.getString();
    if (strMeasurement == null)
      return;
    int xT = x;
    if (rightJustify)
      xT -= radius / 2 + 2 + font3d.fontMetrics.stringWidth(strMeasurement);
    else
      xT += radius / 2 + 2;
    int yT = y
        + (yRef == 0 || yRef < y ? font3d.fontMetrics.getAscent() : -radius / 2);
    int zT = z - radius - 2;
    if (zT < 1)
      zT = 1;
    g3d.drawString(strMeasurement, font3d, xT, yT, zT, zT);
  }

  void renderPendingMeasurement(PendingMeasurement pendingMeasurement) {
    int count = pendingMeasurement.getCount();
    if (! pendingMeasurement.getIsActive() || count < 2)
      return;
    g3d.setColix(viewer.getColixRubberband());
    if (pendingMeasurement.getLastIndex() == -1)
      renderPendingWithCursor(pendingMeasurement);
    else
      renderMeasurement(pendingMeasurement);
  }
  
  public void renderPendingWithCursor(PendingMeasurement pendingMeasurement) {
    int count = pendingMeasurement.getCount();
    if (count < 2)
      return;
    if (count > 2)
      renderMeasurement(count - 1, pendingMeasurement, false);
    Atom atomLast = frame.getAtomAt(pendingMeasurement.getPreviousIndex());
    int lastZ = atomLast.screenZ - atomLast.screenDiameter - 10;
    drawSegment(atomLast.screenX, atomLast.screenY, lastZ,
                viewer.getCursorX(), viewer.getCursorY(), 0);
  }
}
