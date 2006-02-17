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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import org.jmol.g3d.*;

import java.awt.Image;
import java.awt.Rectangle;

class RepaintManager {

  Viewer viewer;
  FrameRenderer frameRenderer;

  RepaintManager(Viewer viewer) {
    this.viewer = viewer;
    frameRenderer = new FrameRenderer(viewer);
  }

  int displayModelIndex = 0;
  boolean setDisplayModelIndex(int modelIndex) {
    Frame frame = viewer.getFrame();
    if (frame == null ||
        modelIndex < 0 ||
        modelIndex >= frame.getModelCount())
      displayModelIndex = -1;
    else
      displayModelIndex = modelIndex;
    this.displayModelIndex = modelIndex;
    viewer.setStatusFrameChanged(modelIndex);
    return true;
  }

  AnimationThread animationThread;

  boolean wireframeRotating = false;
  void setWireframeRotating(boolean wireframeRotating) {
    this.wireframeRotating = wireframeRotating;
  }

  boolean inMotion = false;
  void setInMotion(boolean inMotion) {
    if (this.inMotion != inMotion && viewer.getWireframeRotation()) {
      setWireframeRotating(inMotion);
      if (!inMotion)
        refresh();
    }
    this.inMotion = inMotion;
  }

  Image takeSnapshot() {
    return null;
    //return awtComponent.takeSnapshot();
  }

  int holdRepaint = 0;
  boolean repaintPending;
  void pushHoldRepaint() {
    ++holdRepaint;
    //System.out.println("pushHoldRepaint:" + holdRepaint);
  }

  void popHoldRepaint() {
    --holdRepaint;
    //System.out.println("popHoldRepaint:" + holdRepaint);
    if (holdRepaint <= 0) {
      holdRepaint = 0;
      repaintPending = true;
      //System.out.println("popHoldRepaint called awtComponent.repaint()");
      viewer.awtComponent.repaint();
    }
  }

  /*
  void forceRefresh() {
    System.out.println("repaintManager.forceRefresh");
    repaintPending = true;
    viewer.awtComponent.repaint();
  }
  */

  void refresh() {
    //System.out.println("repaintManager.refresh");
    if (repaintPending)
      return;
    repaintPending = true;
    if (holdRepaint == 0) {
      viewer.awtComponent.repaint();
    }
  }

  synchronized void requestRepaintAndWait() {
    viewer.awtComponent.repaint();
    try {
      wait();
    } catch (InterruptedException e) {
    }
  }

  synchronized void repaintView() {
    repaintPending = false;
    //System.out.println("RepaintManager.repaintView");
    notify();
  }

  final Rectangle rectOversample = new Rectangle();
  boolean tOversample;

  void setOversample(boolean tOversample) {
    this.tOversample = tOversample;
  }

  void render(Graphics3D g3d, Rectangle rectClip,
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
  
  int animationDirection = 1;
  int currentDirection = 1;
  void setAnimationDirection(int animationDirection) {
    if (animationDirection == 1 || animationDirection == -1) {
      this.animationDirection = currentDirection = animationDirection;
    }
    else
      System.out.println("invalid animationDirection:" + animationDirection);
  }

  int animationFps = 10;
  void setAnimationFps(int animationFps) {
    if (animationFps >= 1 && animationFps <= 50)
      this.animationFps = animationFps;
    else
      System.out.println("invalid animationFps:" + animationFps);
  }

  // 0 = once
  // 1 = loop
  // 2 = palindrome
  int animationReplayMode = 0;
  float firstFrameDelay, lastFrameDelay;
  int firstFrameDelayMs, lastFrameDelayMs;
  void setAnimationReplayMode(int animationReplayMode,
                                     float firstFrameDelay,
                                     float lastFrameDelay) {
    //System.out.println("animationReplayMode=" + animationReplayMode);
    this.firstFrameDelay = firstFrameDelay > 0 ? firstFrameDelay : 0;
    firstFrameDelayMs = (int)(this.firstFrameDelay * 1000);
    this.lastFrameDelay = lastFrameDelay > 0 ? lastFrameDelay : 0;
    lastFrameDelayMs = (int)(this.lastFrameDelay * 1000);
    if (animationReplayMode >= 0 && animationReplayMode <= 2)
      this.animationReplayMode = animationReplayMode;
    else
      System.out.println("invalid animationReplayMode:" + animationReplayMode);
  }

  void setAnimationRange(int framePointer, int framePointer2) {
    modelCount = viewer.getModelCount();
    if (framePointer < 0) framePointer = 0;
    if (framePointer2 < 0) framePointer2 = modelCount;
    if (framePointer >= modelCount) framePointer = modelCount - 1;
    if (framePointer2 >= modelCount) framePointer2 = modelCount - 1;
    frameStep = (framePointer2 < framePointer ? -1 : 1);
    firstModelIndex = framePointer;
    lastModelIndex = framePointer2;
    currentDirection = 1;
    //System.out.println("setAnimationRange first=" + firstModelIndex + " last=" + lastModelIndex +" currentDirection="+currentDirection);
  }

  boolean animationOn = false;
  boolean animationPaused = false;
  void setAnimationOn(boolean animationOn) {
    if (! animationOn || ! viewer.haveFrame()) {
      setAnimationOff(false);
      return;
    }
    setAnimationRange(-1, -1);

    //System.out.println("setAnimationOn first=" + firstModelIndex + " last=" + lastModelIndex +" currentDirection="+currentDirection);
    
    setDisplayModelIndex(animationDirection > 0 ? firstModelIndex : lastModelIndex);
    resumeAnimation();
  }

  void setAnimationOff(boolean isPaused) {
    if (animationThread != null) {
      animationThread.interrupt();
      animationThread = null;
    }
    animationPaused = isPaused;
    animationOn = false;
  }

  void pauseAnimation() {
    setAnimationOff(true);
  }
  
  
  int modelCount;
  void resumeAnimation() {
    if(frameStep == 0) {
      firstModelIndex = 0;
      lastModelIndex = modelCount = viewer.getModelCount();
      frameStep = 1;      
    }
    if (modelCount <= 1) {
      animationOn = false;
      return;
    }
    //System.out.println("resumeAnimation " + firstModelIndex+" "+lastModelIndex+" "+frameStep+" "+displayModelIndex);
    if (animationThread == null) {
      animationThread = new AnimationThread(firstModelIndex, lastModelIndex);
      animationThread.start();
    }
    animationOn = true;
    animationPaused = false;
  }
  
  boolean setAnimationNext() {
    return setAnimationRelative(animationDirection);
  }

  boolean setAnimationPrevious() {
    return setAnimationRelative(-animationDirection);
  }

  boolean setAnimationRelative(int direction) {
    boolean isDone = (displayModelIndex == (currentDirection == 1? lastModelIndex : firstModelIndex));
    int frameStep = this.frameStep * direction * currentDirection;
    int modelIndexNext = displayModelIndex + frameStep;

    /*
     System.out.println("setAnimationRelative: " +
                       " firstModelIndex=" + firstModelIndex +
                       " displayModelIndex=" + displayModelIndex +
                       " lastModelIndex=" + lastModelIndex +
                       " currentDirection=" + currentDirection +
                       " direction=" + direction +
                       " isDone="+isDone +
                       " modelIndexNext=" + modelIndexNext +
                       " modelCount=" + modelCount +
                       " animationReplayMode=" + animationReplayMode +
                       " animationDirection=" + animationDirection);
   */
    if (isDone) {
      switch (animationReplayMode) {
      case 0:  //once through
        return false;
      case 1:  //repeat
        modelIndexNext = firstModelIndex;
        break;
      case 2:  //palindrome
        currentDirection = -currentDirection;
        modelIndexNext -= 2 * frameStep;
      }
    }
    //System.out.println("next="+modelIndexNext+" dir="+currentDirection+" isDone="+isDone);
    if (modelIndexNext < 0 || modelIndexNext >= modelCount)
      return false; 
    setDisplayModelIndex(modelIndexNext);
    return true;
  }

  int firstModelIndex;
  int lastModelIndex;
  int frameStep;
  
  void clearAnimation() {
    setAnimationOn(false);
    setDisplayModelIndex(0);
    setAnimationDirection(1);
    setAnimationFps(10);
    setAnimationReplayMode(0, 0, 0);
    firstModelIndex = -1;
    lastModelIndex = -1;
    frameStep = 0;
  }

  class AnimationThread extends Thread implements Runnable {
    final int framePointer;
    final int framePointer2;
    
    AnimationThread(int framePointer, int framePointer2) {
      this.framePointer = framePointer;
      this.framePointer2 = framePointer2;
    }

    public void run() {
      long timeBegin = System.currentTimeMillis();
      int targetTime = 0;
      int sleepTime;
      requestRepaintAndWait();
      try {
        sleepTime = targetTime - (int)(System.currentTimeMillis() - timeBegin);
        if (sleepTime > 0)
          Thread.sleep(sleepTime);
        while (! isInterrupted()) {
          if (displayModelIndex == framePointer) {
            targetTime += firstFrameDelayMs;
            sleepTime =
              targetTime - (int)(System.currentTimeMillis() - timeBegin);
            if (sleepTime > 0)
              Thread.sleep(sleepTime);
          }
          if (displayModelIndex == framePointer2) {
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
