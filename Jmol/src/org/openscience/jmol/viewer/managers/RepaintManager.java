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

import org.jmol.g3d.*;
import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.datamodel.FrameRenderer;
import org.openscience.jmol.viewer.datamodel.Frame;

import java.awt.Image;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Rectangle;

public class RepaintManager {

  JmolViewer viewer;
  public FrameRenderer frameRenderer;

  public RepaintManager(JmolViewer viewer) {
    this.viewer = viewer;
    frameRenderer = new FrameRenderer(viewer);
  }

  public int displayModelIndex = 0;

  public boolean setDisplayModelIndex(int modelIndex) {
    if (modelIndex > 0) {
      Frame frame = viewer.getFrame();
      if (modelIndex >= frame.getModelCount()) {
        System.out.println("bad model index");
        return false;
      }
    }
    this.displayModelIndex = modelIndex;
    viewer.notifyFrameChanged(modelIndex);
    return true;
  }

  public boolean setDisplayModelID(int modelID) {
    int i = -1;
    if (modelID != 0) {
      i = viewer.getModelIndex(modelID);
      if (i < 0)
        return false;
    }
    return setDisplayModelIndex(i);
  }

  public int animationDirection = 1;
  int currentDirection = 1;
  public void setAnimationDirection(int animationDirection) {
    if (animationDirection == 1 || animationDirection == -1) {
      this.animationDirection = currentDirection = animationDirection;
    }
    else
      System.out.println("invalid animationDirection:" + animationDirection);
  }

  public int animationFps = 10;
  public void setAnimationFps(int animationFps) {
    if (animationFps >= 1 && animationFps <= 50)
      this.animationFps = animationFps;
    else
      System.out.println("invalid animationFps:" + animationFps);
  }

  // 0 = once
  // 1 = loop
  // 2 = palindrome
  public int animationReplayMode = 0;
  public float firstFrameDelay, lastFrameDelay;
  int firstFrameDelayMs, lastFrameDelayMs;

  public void setAnimationReplayMode(int animationReplayMode,
                                     float firstFrameDelay,
                                     float lastFrameDelay) {
    System.out.println("animationReplayMode=" + animationReplayMode);
    this.firstFrameDelay = firstFrameDelay > 0 ? firstFrameDelay : 0;
    firstFrameDelayMs = (int)(this.firstFrameDelay * 1000);
    this.lastFrameDelay = lastFrameDelay > 0 ? lastFrameDelay : 0;
    lastFrameDelayMs = (int)(this.lastFrameDelay * 1000);
    if (animationReplayMode >= 0 && animationReplayMode <= 2)
      this.animationReplayMode = animationReplayMode;
    else
      System.out.println("invalid animationReplayMode:" + animationReplayMode);
  }

  public boolean setAnimationRelative(int direction) {
    if (displayModelIndex < 0)
      return false;
    int modelIndexNext = displayModelIndex + (direction * currentDirection);
    int modelCount = viewer.getModelCount();

    /*
    System.out.println("setAnimationRelative: displayModelID=" +
                       displayModelID +
                       " displayModelIndex=" + displayModelIndex +
                       " currentDirection=" + currentDirection +
                       " direction=" + direction +
                       " modelIndexNext=" + modelIndexNext +
                       " modelCount=" + modelCount +
                       " animationReplayMode=" + animationReplayMode +
                       " animationDirection=" + animationDirection);
    */

    if (modelIndexNext == modelCount) {
      switch (animationReplayMode) {
      case 0:
        return false;
      case 1:
        modelIndexNext = 0;
        break;
      case 2:
        currentDirection = -1;
        modelIndexNext = modelCount - 2;
      }
    } else if (modelIndexNext < 0) {
      switch (animationReplayMode) {
      case 0:
        return false;
      case 1:
        modelIndexNext = modelCount -1;
        break;
      case 2:
        currentDirection = 1;
        modelIndexNext = 1;
      }
    }
    setDisplayModelIndex(modelIndexNext);
    return true;
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
    //    System.out.println("pushHoldRepaint:" + holdRepaint);
  }

  public void popHoldRepaint() {
    --holdRepaint;
    //    System.out.println("popHoldRepaint:" + holdRepaint);
    if (holdRepaint <= 0) {
      holdRepaint = 0;
      repaintPending = true;
      // System.out.println("popHoldRepaint called awtComponent.repaint()");
      viewer.awtComponent.repaint();
    }
  }

  public void forceRefresh() {
    repaintPending = true;
    viewer.awtComponent.repaint();
  }

  public void refresh() {
    if (repaintPending)
      return;
    repaintPending = true;
    if (holdRepaint == 0) {
      viewer.awtComponent.repaint();
    }
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
                     Frame frame, int displayModelID) {
    frameRenderer.render(g3d, rectClip, frame, displayModelID);
    viewer.checkCameraDistance();
    Rectangle band = viewer.getRubberBandSelection();
    if (band != null)
      g3d.drawRect(viewer.getColixRubberband(),
                   band.x, band.y, 0, band.width, band.height);
  }

  /****************************************************************
   * Animation support
   ****************************************************************/
  
  public void clearAnimation() {
    setAnimationOn(false);
    setDisplayModelID(1);
    setAnimationDirection(1);
    setAnimationFps(10);
    setAnimationReplayMode(0, 0, 0);
  }

  public boolean animationOn = false;
  AnimationThread animationThread;
  public void setAnimationOn(boolean animationOn) {
    if (! animationOn || ! viewer.haveFrame()) {
      if (animationThread != null) {
        animationThread.interrupt();
        animationThread = null;
      }
      this.animationOn = false;
      return;
    }
    int modelCount = viewer.getModelCount();
    if (modelCount <= 1) {
      this.animationOn = false;
      return;
    }
    currentDirection = animationDirection;
    setDisplayModelIndex(animationDirection == 1 ? 0 : modelCount - 1);
    if (animationThread == null) {
      animationThread = new AnimationThread(modelCount);
      animationThread.start();
    }
    this.animationOn = true;
  }

  class AnimationThread extends Thread implements Runnable {
    final int modelCount;
    final int lastModelIndex;
    AnimationThread(int modelCount) {
      this.modelCount = modelCount;
      lastModelIndex = modelCount - 1;
    }

    public void run() {
      long timeBegin = System.currentTimeMillis();
      int targetTime = 0;
      int currentTime, sleepTime;
      requestRepaintAndWait();
      try {
        sleepTime = targetTime - (int)(System.currentTimeMillis() - timeBegin);
        if (sleepTime > 0)
          Thread.sleep(sleepTime);
        while (! isInterrupted()) {
          if (displayModelIndex == 0) {
            targetTime += firstFrameDelayMs;
            sleepTime =
              targetTime - (int)(System.currentTimeMillis() - timeBegin);
            if (sleepTime > 0)
              Thread.sleep(sleepTime);
          }
          if (displayModelIndex == lastModelIndex) {
            targetTime += lastFrameDelayMs;
            sleepTime =
              targetTime - (int)(System.currentTimeMillis() - timeBegin);
            if (sleepTime > 0)
              Thread.sleep(sleepTime);
          }
          if (! setAnimationNext()) {
            setAnimationOn(false);
            return;
          }
          targetTime += (1000 / animationFps);
          sleepTime =
            targetTime - (int)(System.currentTimeMillis() - timeBegin);
          if (sleepTime < 0)
            continue;
          refresh();
          sleepTime =
            targetTime - (int)(System.currentTimeMillis() - timeBegin);
          if (sleepTime > 0)
            Thread.sleep(sleepTime);
        }
      } catch (InterruptedException ie) {
        System.out.println("animation interrupted!");
      }
    }
  }
}
