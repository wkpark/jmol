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
import javax.vecmath.Point3i;
import javax.vecmath.Point3f;
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

import java.awt.Font;
import java.awt.FontMetrics;

public class FrameRenderer {

  JmolViewer viewer;
  AtomRenderer atomRenderer;
  BondRenderer bondRenderer;
  MeasurementRenderer measurementRenderer;
  DotsRenderer dotsRenderer;
  RibbonsRenderer ribbonsRenderer;
  TraceRenderer traceRenderer;
  BackboneRenderer backboneRenderer;
  CartoonRenderer cartoonRenderer;
  StrandsRenderer strandsRenderer;
  AxesRenderer axesRenderer;
  BboxRenderer bboxRenderer;
  LineRenderer lineRenderer;
  CellLineRenderer cellLineRenderer;

  public FrameRenderer(JmolViewer viewer) {
    this.viewer = viewer;
    atomRenderer = new AtomRenderer(viewer, this);
    bondRenderer = new BondRenderer(viewer, this);
  }
  
  public void render(Graphics3D g3d, Rectangle rectClip, Frame frame) {

    if (frame.atomCount <= 0)
      return;

    viewer.calcTransformMatrices();

    atomRenderer.render(g3d, rectClip, frame, null);
    bondRenderer.render(g3d, rectClip, frame, null);
    if (frame.measurementCount > 0) {
      if (measurementRenderer == null)
        measurementRenderer = new MeasurementRenderer(viewer, this);
      measurementRenderer.render(g3d, rectClip, frame, null);
    }
    if (frame.dots != null) {
      if (dotsRenderer == null)
        dotsRenderer = new DotsRenderer(viewer, this);
      dotsRenderer.render(g3d, rectClip, frame, null);
    }
    if (frame.ribbons != null) {
      if (ribbonsRenderer == null)
        ribbonsRenderer = new RibbonsRenderer(viewer, this);
      ribbonsRenderer.render(g3d, rectClip, frame, frame.ribbons);
    }
    if (frame.trace != null) {
      if (traceRenderer == null)
        traceRenderer = new TraceRenderer(viewer, this);
      traceRenderer.render(g3d, rectClip, frame, frame.trace);
    }
    if (frame.backbone != null) {
      if (backboneRenderer == null)
        backboneRenderer = new BackboneRenderer(viewer, this);
      backboneRenderer.render(g3d, rectClip, frame, frame.backbone);
    }
    if (frame.cartoon != null) {
      if (cartoonRenderer == null)
        cartoonRenderer = new CartoonRenderer(viewer, this);
      cartoonRenderer.render(g3d, rectClip, frame, frame.cartoon);
    }
    if (frame.strands != null) {
      if (strandsRenderer == null)
        strandsRenderer = new StrandsRenderer(viewer, this);
      strandsRenderer.render(g3d, rectClip, frame, frame.strands);
    }
    if (frame.axes != null) {
      if (axesRenderer == null)
        axesRenderer = new AxesRenderer(viewer, this);
      axesRenderer.render(g3d, rectClip, frame, frame.axes);
    }
    if (frame.bbox != null) {
      if (bboxRenderer == null)
        bboxRenderer = new BboxRenderer(viewer, this);
      bboxRenderer.render(g3d, rectClip, frame, frame.bbox);
    }

    if (frame.lineCount > 0) {
      if (lineRenderer == null)
        lineRenderer = new LineRenderer(viewer, this);
      lineRenderer.render(g3d, rectClip, frame, null);
    }

    if (frame.cellLineCount > 0) {
      if (cellLineRenderer == null)
        cellLineRenderer = new CellLineRenderer(viewer, this);
      cellLineRenderer.render(g3d, rectClip, frame, null);
    }
  }

  /*
  public void renderStringOffset(String str, short colix, int points,
                                 int x, int y, int z,
                                 int xOffset, int yOffset) {
    Font font = viewer.getFontOfSize(points);
    g3d.setFont(font);
    FontMetrics fontMetrics = g3d.getFontMetrics(font);
    int strHeight = fontMetrics.getAscent();
    strHeight -= 2; // this should not be necessary, but looks like it is;
    if (yOffset > 0)
      y += yOffset + strHeight;
    else if (yOffset == 0)
      y += strHeight / 2;
    else
      y += yOffset;
    if (xOffset > 0)
      x += xOffset;
    else if (xOffset == 0)
      x -= fontMetrics.stringWidth(str) / 2;
    else
      x += xOffset - fontMetrics.stringWidth(str);
    g3d.drawString(str, colix, x, y, z);
  }
  */
  
  public void renderStringOutside(String str, short colix, int pointsFontsize,
                                  Point3i screen, Graphics3D g3d) {
    renderStringOutside(str, colix, pointsFontsize,
                        screen.x, screen.y, screen.z, g3d);
  }

  public void renderStringOutside(String str, short colix, int pointsFontsize,
                                  int x, int y, int z, Graphics3D g3d) {
    g3d.setColix(colix);
    Font font = viewer.getFontOfSize(pointsFontsize);
    g3d.setFont(font);
    FontMetrics fontMetrics = g3d.getFontMetrics(font);
    int strAscent = fontMetrics.getAscent();
    int strWidth = fontMetrics.stringWidth(str);
    int xStrCenter, yStrCenter;
    int xCenter = viewer.getBoundingBoxCenterX();
    int yCenter = viewer.getBoundingBoxCenterY();
    int dx = x - xCenter;
    int dy = y - yCenter;
    if (dx == 0 && dy == 0) {
      xStrCenter = x;
      yStrCenter = y;
    } else {
      int dist = (int) Math.sqrt(dx*dx + dy*dy);
      xStrCenter = xCenter + ((dist + 2 + (strWidth + 1) / 2) * dx / dist);
      yStrCenter = yCenter + ((dist + 3 + (strAscent + 1)/ 2) * dy / dist);
    }
    int xStrBaseline = xStrCenter - strWidth / 2;
    int yStrBaseline = yStrCenter + strAscent / 2;
    g3d.drawString(str, colix, xStrBaseline, yStrBaseline, z);
  }

  public DotsRenderer getDotsRenderer() {
    if (dotsRenderer == null)
      dotsRenderer = new DotsRenderer(viewer, this);
    return dotsRenderer;
  }

  private Point3i[] tempScreens = new Point3i[32];
  {
    for (int i = tempScreens.length; --i >= 0; )
      tempScreens[i] = new Point3i();
  }
  public Point3i[] getTempScreens(int n) {
    if (tempScreens.length < n) {
      int tSize = n + (n >> 1);
      Point3i[] t = new Point3i[tSize];
      System.arraycopy(tempScreens, 0, t, 0, tempScreens.length);
      for (int i = tempScreens.length; i < tSize; ++i)
        t[i] = new Point3i();
      tempScreens = t;
    }
    return tempScreens;
  }

  private Point3f[] tempPoints = new Point3f[32];
  {
    for (int i = tempPoints.length; --i >= 0; )
      tempPoints[i] = new Point3f();
  }
  public Point3f[] getTempPoints(int n) {
    if (tempPoints.length < n) {
      int tSize = n + (n >> 1);
      Point3f[] t = new Point3f[tSize];
      System.arraycopy(tempPoints, 0, t, 0, tempPoints.length);
      for (int i = tempPoints.length; i < tSize; ++i)
        t[i] = new Point3f();
      tempPoints = t;
    }
    return tempPoints;
  }

  private Atom[] tempAtoms = new Atom[32];
  public Atom[] getTempAtoms(int n) {
    if (tempAtoms.length < n)
      tempAtoms = new Atom[n + (n >> 1)];
    return tempAtoms;
  }

  private boolean[] tempBooleans = new boolean[32];
  public boolean[] getTempBooleans(int n) {
    if (tempBooleans.length < n)
      tempBooleans = new boolean[n + (n >> 1)];
    return tempBooleans;
  }

}
