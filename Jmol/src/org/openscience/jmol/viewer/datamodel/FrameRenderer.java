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
  StrandsRenderer strandsRenderer;
  AxesRenderer axesRenderer;
  BboxRenderer bboxRenderer;
  LineRenderer lineRenderer;
  CellLineRenderer cellLineRenderer;

  public FrameRenderer(JmolViewer viewer) {
    this.viewer = viewer;
    atomRenderer = new AtomRenderer(viewer);
    bondRenderer = new BondRenderer(viewer);
  }
  
  public void render(Graphics3D g3d, Rectangle rectClip, Frame frame) {

    if (frame.atomCount <= 0)
      return;

    viewer.calcTransformMatrices();

    atomRenderer.render(g3d, rectClip, frame);
    bondRenderer.render(g3d, rectClip, frame);
    if (frame.measurementCount > 0) {
      if (measurementRenderer == null)
        measurementRenderer = new MeasurementRenderer(viewer);
      measurementRenderer.render(g3d, rectClip, frame);
    }
    if (frame.dots != null) {
      if (dotsRenderer == null)
        dotsRenderer = new DotsRenderer(viewer);
      dotsRenderer.render(g3d, rectClip, frame);
    }
    if (frame.ribbons != null) {
      if (ribbonsRenderer == null)
        ribbonsRenderer = new RibbonsRenderer(viewer);
      ribbonsRenderer.render(g3d, rectClip, frame);
    }
    if (frame.trace != null) {
      if (traceRenderer == null)
        traceRenderer = new TraceRenderer(viewer);
      traceRenderer.render(g3d, rectClip, frame);
    }
    if (frame.strands != null) {
      if (strandsRenderer == null)
        strandsRenderer = new StrandsRenderer(viewer);
      strandsRenderer.render(g3d, rectClip, frame);
    }
    if (frame.axes != null) {
      if (axesRenderer == null)
        axesRenderer = new AxesRenderer(viewer);
      axesRenderer.render(g3d, rectClip, frame);
    }
    if (frame.bbox != null) {
      if (bboxRenderer == null)
        bboxRenderer = new BboxRenderer(viewer);
      bboxRenderer.render(g3d, rectClip, frame);
    }

    if (frame.lineCount > 0) {
      if (lineRenderer == null)
        lineRenderer = new LineRenderer(viewer);
      lineRenderer.render(g3d, rectClip, frame);
    }

    if (frame.cellLineCount > 0) {
      if (cellLineRenderer == null)
        cellLineRenderer = new CellLineRenderer(viewer);
      cellLineRenderer.render(g3d, rectClip, frame);
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
      dotsRenderer = new DotsRenderer(viewer);
    return dotsRenderer;
  }
}
