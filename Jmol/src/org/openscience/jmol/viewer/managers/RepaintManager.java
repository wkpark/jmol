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
package org.openscience.jmol.viewer.managers;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.datamodel.FrameRenderer;
import org.openscience.jmol.viewer.datamodel.Frame;

import java.awt.Image;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Rectangle;

public class RepaintManager {

  JmolViewer viewer;
  public FrameRenderer frameRenderer;

  public RepaintManager(JmolViewer viewer) {
    this.viewer = viewer;
    frameRenderer = new FrameRenderer(viewer);
  }

  public boolean wireframeRotating = false;
  public void setWireframeRotating(boolean wireframeRotating) {
    this.wireframeRotating = wireframeRotating;
  }

  public boolean inMotion = false;

  public void setInMotion(boolean inMotion) {
    if (this.inMotion != inMotion && viewer.getWireframeRotation()) {
      setWireframeRotating(inMotion);
      if (!inMotion)
        refresh();
    }
    this.inMotion = inMotion;
  }

  public Image takeSnapshot() {
    return null;
    //return awtComponent.takeSnapshot();
  }

  public int holdRepaint = 0;
  public boolean repaintPending;

  public void pushHoldRepaint() {
    ++holdRepaint;
  }

  public void popHoldRepaint() {
    if (--holdRepaint <= 0) {
      holdRepaint = 0;
      repaintPending = true;
      viewer.awtComponent.repaint();
    }
  }

  public void refresh() {
    if (repaintPending)
      return;
    repaintPending = true;
    if (holdRepaint == 0)
      viewer.awtComponent.repaint();
  }

  public synchronized void requestRepaintAndWait() {
    viewer.awtComponent.repaint();
    try {
      wait();
    } catch (InterruptedException e) {
    }
  }

  public synchronized void notifyRepainted() {
    repaintPending = false;
    notify();
  }

  final Rectangle rectOversample = new Rectangle();
  boolean tOversample;

  public void setOversample(boolean tOversample) {
    this.tOversample = tOversample;
  }

  public void render(Graphics3D g3d, Rectangle rectClip, Frame frame) {
    g3d.beginRendering(tOversample);
    if (tOversample) {
      rectOversample.x = rectClip.x << 1;
      rectOversample.y = rectClip.y << 1;
      rectOversample.width = rectClip.width << 1;
      rectOversample.height = rectClip.height << 1;
      rectClip = rectOversample;
    }
    g3d.clearScreenBuffer(viewer.getColorBackground(),
                           rectClip.x, rectClip.y,
                           rectClip.width, rectClip.height);
    frameRenderer.render(g3d, rectClip, frame);
    Rectangle band = viewer.getRubberBandSelection();
    if (band != null)
      g3d.drawRect(viewer.getColixRubberband(),
                    band.x, band.y, band.width, band.height);
    g3d.endRendering();
    notifyRepainted();
  }
}
