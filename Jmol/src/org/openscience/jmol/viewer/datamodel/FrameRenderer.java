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
import javax.vecmath.Point3i;
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

public class FrameRenderer {

  JmolViewer viewer;
  private AtomRenderer atomRenderer;
  private BondRenderer bondRenderer;
  private LabelRenderer labelRenderer;
  private MeasurementRenderer measurementRenderer;
  private DotsRenderer dotsRenderer;
  private RibbonsRenderer ribbonsRenderer;
  private TraceRenderer traceRenderer;
  ArcTest arctest;
  Gtest gtest;
  JmolFrame frame;

  public FrameRenderer(JmolViewer viewer) {
    this.viewer = viewer;
    atomRenderer = new AtomRenderer(viewer);
    bondRenderer = new BondRenderer(viewer);
    //    labelRenderer = new LabelRenderer(viewer);
    //    measurementRenderer = new MeasurementRenderer(viewer);
    //    dotsRenderer = new DotsRenderer(viewer);
    //    ribbonsRenderer = new RibbonsRenderer(viewer);
    //    traceRenderer = new TraceRenderer(viewer);
    arctest = new ArcTest(viewer);
    gtest = new Gtest(viewer);
  }
  
  private void setGraphicsContext(Graphics3D g3d, Rectangle rectClip) {
    frame = viewer.getJmolFrame();
    atomRenderer.setGraphicsContext(g3d, rectClip);
    bondRenderer.setGraphicsContext(g3d, rectClip);
    if (labelRenderer != null)
      labelRenderer.setGraphicsContext(g3d, rectClip);
    if (measurementRenderer != null)
      measurementRenderer.setGraphicsContext(g3d, rectClip, frame);
    if (dotsRenderer != null)
      dotsRenderer.setGraphicsContext(g3d, rectClip, frame);
    if (ribbonsRenderer != null)
      ribbonsRenderer.setGraphicsContext(g3d, rectClip, frame);
    if (traceRenderer != null)
      traceRenderer.setGraphicsContext(g3d, rectClip, frame);
    arctest.setGraphicsContext(g3d, rectClip, frame);
    gtest.setGraphicsContext(g3d, rectClip, frame);
  }

  public void render(Graphics3D g3d, Rectangle rectClip) {
    setGraphicsContext(g3d, rectClip);

    if (frame.atomShapeCount <= 0)
      return;

    viewer.calcTransformMatrices();

    //    arctest.transform();
    //    arctest.render();
    gtest.transform();
    gtest.render();


    AtomShape[] atomShapes = frame.atomShapes;
    for (int i = frame.atomShapeCount; --i >= 0; ) {
      AtomShape atomShape = atomShapes[i];
      atomShape.transform(viewer);
      atomRenderer.render(atomShape);
      if (atomShape.strLabel != null) {
        if (labelRenderer == null)
          labelRenderer = new LabelRenderer(viewer);
        labelRenderer.render(atomShape);
      }
    }

    if (frame.dots != null) {
      if (dotsRenderer == null)
        dotsRenderer = new DotsRenderer(viewer);
      dotsRenderer.transform(frame.dots);
      dotsRenderer.render(frame.dots);
    }
    if (frame.ribbons != null) {
      if (ribbonsRenderer == null)
        ribbonsRenderer = new RibbonsRenderer(viewer);
      ribbonsRenderer.transform(frame.ribbons);
      ribbonsRenderer.render(frame.ribbons);
    }
    if (frame.trace != null) {
      if (traceRenderer == null)
        traceRenderer = new TraceRenderer(viewer);
      traceRenderer.transform(frame.trace);
      traceRenderer.render(frame.trace);
    }

    BondShape[] bondShapes = frame.bondShapes;
    for (int i = frame.bondShapeCount; --i >= 0; )
      bondRenderer.render(bondShapes[i]);

    for (int i = frame.lineShapeCount; --i >= 0; ) {
      LineShape lineShape = frame.lineShapes[i];
      lineShape.transform(viewer);
      lineShape.render(g3d, viewer);
    }

    for (int i = frame.crystalCellLineCount; --i >= 0; ) {
      LineShape cellLine = frame.crystalCellLines[i];
      cellLine.transform(viewer);
      cellLine.render(g3d, viewer);
    }

    if (frame.measurementShapeCount > 0) {
      if (measurementRenderer == null)
        measurementRenderer = new MeasurementRenderer(viewer);
      for (int i = frame.measurementShapeCount; --i >= 0; ) {
        MeasurementShape measurementShape = frame.measurementShapes[i];
        //      measurementShape.transform(viewer);
        measurementRenderer.render(measurementShape);
      }
    }

    if (viewer.getModeAxes() != JmolViewer.AXES_NONE) {
      Axes axes = viewer.getAxes();
      axes.transform();
      axes.render(g3d);
    }

    if (viewer.getShowBoundingBox()) {
      BoundingBox bbox = viewer.getBoundingBox();
      bbox.transform();
      bbox.render(g3d);
    }
  }

  public void renderStringOffset(String str, short colix, int points,
                                 int x, int y, int z,
                                 int xOffset, int yOffset) {
    labelRenderer.renderStringOffset(str, colix, points,
                                     x, y, z, xOffset, yOffset);
  }
  
  public void renderStringOutside(String str, short colix, int pointsFontsize,
                                  Point3i screen) {
    labelRenderer.renderStringOutside(str, colix, pointsFontsize,
                                      screen.x, screen.y, screen.z);
  }

  public void renderStringOutside(String str, short colix, int pointsFontsize,
                                  int x, int y, int z) {
    labelRenderer.renderStringOutside(str, colix, pointsFontsize, x, y, z);
  }

  public DotsRenderer getDotsRenderer() {
    if (dotsRenderer == null)
      dotsRenderer = new DotsRenderer(viewer);
    return dotsRenderer;
  }
  public RibbonsRenderer getRibbonsRenderer() {
    if (ribbonsRenderer == null)
      ribbonsRenderer = new RibbonsRenderer(viewer);
    return ribbonsRenderer;
  }
  public TraceRenderer getTraceRenderer() {
    if (traceRenderer == null)
      traceRenderer = new TraceRenderer(viewer);
    return traceRenderer;
  }
}
