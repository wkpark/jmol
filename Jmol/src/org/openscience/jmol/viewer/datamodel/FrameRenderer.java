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
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

public class FrameRenderer {

  JmolViewer viewer;
  AtomRenderer atomRenderer;
  BondRenderer bondRenderer;
  LabelRenderer labelRenderer;
  DotsRenderer dotsRenderer;
  ArcTest arctest;
  Gtest gtest;
  JmolFrame frame;

  public FrameRenderer(JmolViewer viewer) {
    this.viewer = viewer;
    atomRenderer = new AtomRenderer(viewer);
    bondRenderer = new BondRenderer(viewer);
    labelRenderer = new LabelRenderer(viewer);
    dotsRenderer = new DotsRenderer(viewer);
    arctest = new ArcTest(viewer);
    gtest = new Gtest(viewer);
  }
  
  private void setGraphicsContext(Graphics3D g3d, Rectangle rectClip) {
    frame = viewer.getJmolFrame();
    atomRenderer.setGraphicsContext(g3d, rectClip);
    bondRenderer.setGraphicsContext(g3d, rectClip);
    labelRenderer.setGraphicsContext(g3d, rectClip);
    dotsRenderer.setGraphicsContext(g3d, rectClip, frame);
    arctest.setGraphicsContext(g3d, rectClip, frame);
    gtest.setGraphicsContext(g3d, rectClip, frame);
  }

  public void render(Graphics3D g3d, Rectangle rectClip) {
    setGraphicsContext(g3d, rectClip);

    if (frame.atomShapeCount <= 0)
      return;

    viewer.calcTransformMatrices();

    dotsRenderer.transform();
    //    arctest.transform();
    //    arctest.render();
    gtest.transform();
    gtest.render();


    AtomShape[] atomShapes = frame.atomShapes;
    for (int i = frame.atomShapeCount; --i >= 0; ) {
      AtomShape atomShape = atomShapes[i];
      atomShape.transform(viewer);
      atomRenderer.render(atomShape);
      if (atomShape.strLabel != null)
        labelRenderer.render(atomShape);
    }

    dotsRenderer.render();

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

    for (int i = frame.measurementShapeCount; --i >= 0; ) {
      MeasurementShape measurementShape = frame.measurementShapes[i];
      measurementShape.transform(viewer);
      measurementShape.render(g3d, viewer);
    }

    if (viewer.getModeAxes() != JmolViewer.AXES_NONE) {
      Shape[] axisShapes = viewer.getAxes().getShapes();
      for (int i = axisShapes.length; --i >= 0; ) {
        Shape axisShape = axisShapes[i];
        axisShape.transform(viewer);
        axisShape.render(g3d, viewer);
      }
    }

    if (viewer.getShowBoundingBox()) {
      Shape[] bboxShapes = viewer.getBoundingBox().getBboxShapes();
      for (int i = bboxShapes.length; --i >= 0; ) {
        Shape bboxShape = bboxShapes[i];
        bboxShape.transform(viewer);
        bboxShape.render(g3d, viewer);
      }
    }
  }

  public void renderStringOffset(String str, short colix, int points,
                                 int x, int y, int z,
                                 int xOffset, int yOffset) {
    labelRenderer.renderStringOffset(str, colix, points,
                                     x, y, z, xOffset, yOffset);
  }
  
  public void renderStringOutside(String str, short colix, int pointsFontsize,
                                  int x, int y, int z) {
    labelRenderer.renderStringOutside(str, colix, pointsFontsize, x, y, z);
  }
}
