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

  public int displayModel = 0;
  public void setDisplayModel(int model) {
    System.out.println("display model=" + model);
    this.displayModel = model;
  }

  public int animationDirection = 1;
  public void setAnimationDirection(int animationDirection) {
    if (animationDirection == 1 || animationDirection == -1)
      this.animationDirection = animationDirection;
    else
      System.out.println("invalid animationDirection:" + animationDirection);
  }

  public int animationFps = 10;
  public void setAnimationFps(int animationFps) {
    if (animationFps >= 1 && animationFps <= 30)
      this.animationFps = animationFps;
    else
      System.out.println("invalid animationFps:" + animationFps);
  }

  // 0 = once
  // 1 = loop
  // 2 = palindrome
  public int animationReplayMode = 0;
  public void setAnimationReplayMode(int animationReplayMode) {
    if (animationReplayMode >= 0 && animationReplayMode <= 2)
      this.animationReplayMode = animationReplayMode;
    else
      System.out.println("invalid animationReplayMode:" + animationReplayMode);
  }

  public int animationReplayDelay = 1000;
  public void setAnimationReplayDelay(int ms) {
    if (animationReplayDelay < 0)
      animationReplayDelay = 0;
    this.animationReplayDelay = ms;
  }

  public boolean isAnimating = false;
  public void setAnimate(boolean animate) {
    this.isAnimating = animate;
  }

  private int getIndex(int id, int idCount, short[] ids) {
    int index = idCount;
    while ((--index >= 0) && (ids[index] != id))
      ;
    return index;
  }

  public boolean setAnimationRelative(int direction) {
    if (displayModel == 0)
      return false;
    Frame frame = viewer.getFrame();
    short[] modelIDs = frame.modelIDs;
    int modelCount = frame.modelCount;
    int modelIndex = modelCount;
    modelIndex = getIndex(displayModel, modelCount, modelIDs);
    if (modelIndex < 0) {
      displayModel = 0;
      return false;
    }
    int modelIndexNext = modelIndex + direction;
    /*
    System.out.println("setAnimationRelative: displayModel=" + displayModel +
                       " modelIndex=" + modelIndex +
                       " direction=" + direction +
                       " modelIndexNext=" + modelIndexNext +
                       " modelCount=" + modelCount +
                       " animationReplayMode=" + animationReplayMode +
                       " animationDirection=" + animationDirection);
    */
    if (modelIndexNext == modelCount) {
      switch (animationReplayMode) {
      case 1:
        displayModel = modelIDs[0];
        return true;
      case 2:
        displayModel = modelIDs[modelCount - 2];
        animationDirection = -1;
        return true;
      }
    } else if (modelIndexNext < 0) {
      switch (animationReplayMode) {
      case 1:
        displayModel = modelIDs[modelCount - 1];
        return true;
      case 2:
        displayModel = modelIDs[1];
        animationDirection = 1;
        return true;
      }
    } else {
      displayModel = modelIDs[modelIndexNext];
      return true;
    }
    return false;
  }

  public boolean setAnimationNext() {
    return setAnimationRelative(animationDirection);
  }

  public boolean setAnimationPrevious() {
    return setAnimationRelative(-animationDirection);
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

  public void render(Graphics3D g3d, Rectangle rectClip,
                     Frame frame, int displayModel) {
    g3d.validateRectClip(rectClip);
    g3d.beginRendering(tOversample);
    if (tOversample) {
      rectOversample.x = rectClip.x << 1;
      rectOversample.y = rectClip.y << 1;
      rectOversample.width = rectClip.width << 1;
      rectOversample.height = rectClip.height << 1;
      rectClip = rectOversample;
    }
    g3d.clearScreenBuffer(viewer.getColorBackground().getRGB(), rectClip);
    frameRenderer.render(g3d, rectClip, frame, displayModel);
    viewer.checkCameraDistance();
    Rectangle band = viewer.getRubberBandSelection();
    if (band != null)
      g3d.drawRect(viewer.getColixRubberband(),
                    band.x, band.y, band.width, band.height);
    g3d.endRendering();
    notifyRepainted();
  }
}
