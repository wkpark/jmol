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

package org.jmol.render;

import org.jmol.modelset.Measurement;
import org.jmol.modelset.MeasurementPending;
import org.jmol.script.T;
import org.jmol.shape.Measures;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.Point3fi;

import javajs.util.A4;
import javajs.util.M3;
import javajs.util.P3;
import javajs.util.P3i;



public class MeasuresRenderer extends LabelsRenderer {

  private Measurement m;
  private boolean doJustify;
  private short mad0;
  protected Point3fi[] p = new Point3fi[4];
  private boolean modulating;
  private int count;
  
  @Override
  protected boolean render() {
    if (!g3d.checkTranslucent(false))
      return false;
    if (atomPt == null)
      atomPt = new Point3fi();
    Measures measures = (Measures) shape;
    doJustify = viewer.getBoolean(T.justifymeasurements);
    modulating = viewer.isVibrationOn() && modelSet.bsModulated != null; 
    // note that this COULD be screen pixels if <= 20. 
    imageFontScaling = viewer.getImageFontScaling();
    mad0 = measures.mad;
    font3d = g3d.getFont3DScaled(measures.font3d, imageFontScaling);
    m = measures.measurementPending;
    if (!isExport && m != null && (count = m.getCount())!= 0)
      renderPendingMeasurement();
    if (!viewer.getBoolean(T.showmeasurements))
      return false;
    boolean showMeasurementLabels = viewer.getBoolean(T.measurementlabels);
    measures.setVisibilityInfo();
    for (int i = measures.measurementCount; --i >= 0;) {
      m = measures.measurements.get(i);
      if (!m.isVisible || !m.isValid || (count = m.getCount()) == 1 && m.traceX == Integer.MIN_VALUE)
        continue;
      getPoints();
      colix = m.colix;
      if (colix == 0)
        colix = measures.colix;
      if (colix == 0)
        colix = viewer.getColixBackgroundContrast();
      labelColix = m.labelColix;
      if (labelColix == 0)
        labelColix = viewer.getColixBackgroundContrast();
      else if (labelColix == -1)
        labelColix = colix;
      g3d.setColix(colix);
      colixA = colixB = colix;
      renderMeasurement(showMeasurementLabels);
    }
    return false;
  }

  private void getPoints() {
    for (int j = count; --j >= 0;)
      p[j] = getAtom(j);
    m.refresh(p);
  }

  private Point3fi[] mpts = new Point3fi[4];
  
  private Point3fi getAtom(int n) {
    int i = m.getAtomIndex(n + 1);
    Point3fi pt = (i < 0 || !modulating ? m.getAtom(n + 1)
        : (mpts[n] = modelSet.getDynamicAtom(i, mpts[n])));
    if (pt.sD < 0) {
      viewer.transformPtScr(pt, pt0i);
      pt.sX = pt0i.x;
      pt.sY = pt0i.y;
      pt.sZ = pt0i.z;
    }
    return pt;
  }

  private void renderMeasurement(boolean renderLabel) {
    String s = (renderLabel ? m.getString() : null);
    if (s != null) {
      if (s.length() == 0) {
        s = null;
      } else if (m.text != null) {
        m.text.setText(s);
        m.text.setColix(labelColix);
      }
    }
    if (m.mad == 0) {
      dotsOrDashes = false;
      mad = mad0;
    } else {
      mad = (short) m.mad;
      //dashDots = hDashes;
      dotsOrDashes = true;
      dashDots = (mad < 0 ? null : ndots);
    }
    switch (count) {
    case 1:
      drawLine(p[0].sX, p[0].sY, p[0].sZ, m.traceX, m.traceY,
          p[0].sZ, mad);
      break;
    case 2:
      renderDistance(s, p[0], p[1]);
      break;
    case 3:
      renderAngle(s, p[0], p[1], p[2]);
      break;
    case 4:
      renderTorsion(s, p[0], p[1], p[2], p[3]);
      break;
    }
    p[0] = p[1] = p[2] = p[3] = null;
  }

  void renderDistance(String s, Point3fi a, Point3fi b) {
   if ((tickInfo = m.tickInfo) != null) {
      drawLine(a.sX, a.sY, a.sZ, b.sX,
          b.sY, b.sZ, mad);
      drawTicks(a, b, mad, s != null);
      return;
    }
    int zA = a.sZ - a.sD - 10;
    int zB = b.sZ - b.sD - 10;
    int radius = drawLine(a.sX, a.sY, zA, b.sX,
        b.sY, zB, mad);
    if (s == null)
      return;
    if (mad > 0)
      radius <<= 1;
    int z = (zA + zB) / 2;
    if (z < 1)
      z = 1;
    int x = (a.sX + b.sX) / 2;
    int y = (a.sY + b.sY) / 2;
    if (m.text == null) {
      g3d.setColix(labelColix);
      drawString(x, y, z, radius, doJustify
          && (x - a.sX) * (y - a.sY) > 0, false, false,
          (doJustify ? 0 : Integer.MAX_VALUE), s);
    } else {
      atomPt.add2(a, b);
      atomPt.scale(0.5f);
      atomPt.sX = (a.sX + b.sX) / 2;
      atomPt.sY = (a.sY + b.sY) / 2;
      renderLabelOrMeasure(m.text, s);
    }
  }
                          
  private A4 aaT = new A4();
  private M3 matrixT = new M3();

  private void renderAngle(String s, Point3fi a, Point3fi b, Point3fi c) {
    int zOffset = b.sD + 10;
    int zA = a.sZ - a.sD - 10;
    int zB = b.sZ - zOffset;
    int zC = c.sZ - c.sD - 10;
    int radius = drawLine(a.sX, a.sY, zA, b.sX,
        b.sY, zB, mad);
    radius += drawLine(b.sX, b.sY, zB, c.sX,
        c.sY, zC, mad);
    if (s == null)
      return;
    radius = (radius + 1) / 2;

    A4 aa = m.getAxisAngle();
    if (aa == null) { // 180 degrees
      if (m.text == null) {
        int offset = (int) Math.floor(5 * imageFontScaling);
        g3d.setColix(labelColix);
        drawString(b.sX + offset, b.sY - offset, zB, radius,
            false, false, false, (doJustify ? 0 : Integer.MAX_VALUE), s);
      } else {
        atomPt.setT(b);
        renderLabelOrMeasure(m.text, s);
      }
      return;
    }
    int dotCount = (int) Math.floor((aa.angle / (2 * Math.PI)) * 64);
    float stepAngle = aa.angle / dotCount;
    aaT.setAA(aa);
    int iMid = dotCount / 2;
    P3 ptArc = m.getPointArc();
    for (int i = dotCount; --i >= 0;) {
      aaT.angle = i * stepAngle;
      matrixT.setAA(aaT);
      pointT.setT(ptArc);
      matrixT.transform(pointT);
      pointT.add(b);
      // NOTE! Point3i screen is just a pointer 
      //  to viewer.transformManager.point3iScreenTemp
      P3i p3i = viewer.transformPt(pointT);
      int zArc = p3i.z - zOffset;
      if (zArc < 0)
        zArc = 0;
      g3d.drawPixel(p3i.x, p3i.y, zArc);
      if (i != iMid)
        continue;
      pointT.setT(ptArc);
      pointT.scale(1.1f);
      // next line modifies Point3i point3iScreenTemp
      matrixT.transform(pointT);
      pointT.add(b);
      viewer.transformPt(pointT);
      int zLabel = p3i.z - zOffset;
      if (m.text == null) {
        g3d.setColix(labelColix);
        drawString(p3i.x, p3i.y, zLabel, radius, p3i.x < b.sX, false,
            false, (doJustify ? b.sY : Integer.MAX_VALUE), s);
      } else {
        atomPt.setT(pointT);
        renderLabelOrMeasure(m.text, s);
      }
    }
  }

  private void renderTorsion(String s, Point3fi a, Point3fi b, Point3fi c, Point3fi d) {
    int zA = a.sZ - a.sD - 10;
    int zB = b.sZ - b.sD - 10;
    int zC = c.sZ - c.sD - 10;
    int zD = d.sZ - d.sD - 10;
    int radius = drawLine(a.sX, a.sY, zA, b.sX,
        b.sY, zB, mad);
    radius += drawLine(b.sX, b.sY, zB, c.sX,
        c.sY, zC, mad);
    radius += drawLine(c.sX, c.sY, zC, d.sX,
        d.sY, zD, mad);
    if (s == null)
      return;
    radius /= 3;
    if (m.text == null) {
      g3d.setColix(labelColix);
      drawString((a.sX + b.sX + c.sX + d.sX) / 4,
          (a.sY + b.sY + c.sY + d.sY) / 4,
          (zA + zB + zC + zD) / 4, radius, false, false, false,
          (doJustify ? 0 : Integer.MAX_VALUE), s);
    } else {
      atomPt.add2(a, b);
      atomPt.add(c);
      atomPt.add(d);
      atomPt.scale(0.25f);
      renderLabelOrMeasure(m.text, s);
    }
  }

  private void renderPendingMeasurement() {
    getPoints();
    boolean renderLabel = (m.traceX == Integer.MIN_VALUE);
    g3d.setColix(labelColix = (renderLabel ? viewer.getColixRubberband()
        : count == 2 ? C.MAGENTA : C.GOLD));
    if (((MeasurementPending) m).haveTarget) {
      renderMeasurement(renderLabel);
      return;
    }    
    Point3fi atomLast = p[count - 1];
    if (count > 1)
      renderMeasurement(false);
    int lastZ = atomLast.sZ - atomLast.sD - 10;
    int x = viewer.getCursorX();
    int y = viewer.getCursorY();
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
    }
    drawLine(atomLast.sX, atomLast.sY, lastZ, x, y, 0, mad);
  }  
 
  //TODO: I think the 20 here is the cutoff for pixels -- check this
  @Override
  protected int drawLine(int x1, int y1, int z1, int x2, int y2, int z2,
                         int mad) {
    // small numbers refer to pixels already? 
    int diameter = (int) (mad >= 20 && exportType != GData.EXPORT_CARTESIAN ?
      viewer.scaleToScreen((z1 + z2) / 2, mad) : mad);
    if (dotsOrDashes && (dashDots == null || dashDots == ndots))
      width = diameter;
    return drawLine2(x1, y1, z1, x2, y2, z2, diameter);
  }
}
