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

import org.jmol.util.Escape;
import org.jmol.util.Logger;

import org.jmol.g3d.*;
import org.jmol.modelset.ModelSet;

import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.BitSet;

class RepaintManager {

  Viewer viewer;
  FrameRenderer frameRenderer;
  
  RepaintManager(Viewer viewer) {
    this.viewer = viewer;
    frameRenderer = new FrameRenderer(viewer);
  }

  int currentModelIndex = 0;

  void setCurrentModelIndex(int modelIndex) {
    setCurrentModelIndex(modelIndex, true);  
  }
  
  void setCurrentModelIndex(int modelIndex, boolean clearBackgroundModel) {
    int formerModelIndex = currentModelIndex;
    ModelSet modelSet = viewer.getModelSet();
    int modelCount = (modelSet == null ? 0 : modelSet.getModelCount());
    if (modelCount == 1)
      currentModelIndex = modelIndex = 0;
    else if (modelIndex < 0 || modelIndex >= modelCount)
      modelIndex = -1;
    String ids = null;
    boolean isSameSource = false;
    if (currentModelIndex != modelIndex) {
      if (modelCount > 0) {
        boolean toDataFrame = viewer.isJmolDataFrame(modelIndex);
        boolean fromDataFrame = viewer.isJmolDataFrame(currentModelIndex);
        if (fromDataFrame)
          viewer.setJmolDataFrame(null, -1, currentModelIndex);
        if (currentModelIndex != -1)
          viewer.saveModelOrientation();
        if (fromDataFrame || toDataFrame) {
          ids = viewer.getJmolFrameType(modelIndex) + viewer.getJmolFrameType(currentModelIndex);
          isSameSource = (viewer.getJmolDataSourceFrame(modelIndex) == viewer
              .getJmolDataSourceFrame(currentModelIndex));
        }
      }
      currentModelIndex = modelIndex;
      if (ids != null) {
        if (modelIndex >= 0)
          viewer.restoreModelOrientation(modelIndex);
        if (isSameSource && ids.indexOf("quaternion") >= 0 
            && ids.indexOf("ramachandran") < 0)
          viewer.restoreModelRotation(formerModelIndex);
      }
    }
    //System.out.println("set trajectory " + currentModelIndex);
    viewer.setTrajectory(currentModelIndex);
    viewer.setFrameOffset(currentModelIndex);
    if (currentModelIndex == -1 && clearBackgroundModel)
      setBackgroundModelIndex(-1);  
    viewer.setTainted(true);
    setFrameRangeVisible();
    if (modelSet != null) {
      setStatusFrameChanged();
      if (!viewer.getSelectAllModels())
        viewer.setSelectionSubset(viewer.getModelAtomBitSet(currentModelIndex, false));
    }
  
  }

  
  private void setStatusFrameChanged() {
    viewer.setStatusFrameChanged(animationOn ? -2 - currentModelIndex
        : currentModelIndex);
  }
  
  int backgroundModelIndex = -1;
  void setBackgroundModelIndex(int modelIndex) {
    ModelSet modelSet = viewer.getModelSet();
    if (modelSet == null || modelIndex < 0 || modelIndex >= modelSet.getModelCount())
      modelIndex = -1;
    backgroundModelIndex = modelIndex;
    if (modelIndex >= 0)
      viewer.setTrajectory(modelIndex);
    viewer.setTainted(true);
    setFrameRangeVisible(); 
  }
  
  private BitSet bsVisibleFrames = new BitSet();
  BitSet getVisibleFramesBitSet() {
    return bsVisibleFrames;
  }
  
  private void setFrameRangeVisible() {
    bsVisibleFrames.clear();
    if (backgroundModelIndex >= 0)
      bsVisibleFrames.set(backgroundModelIndex);
    if (currentModelIndex >= 0) {
      bsVisibleFrames.set(currentModelIndex);
      return;
    }
    if (frameStep == 0)
      return;
    int nDisplayed = 0;
    int frameDisplayed = 0;
    for (int i = firstModelIndex; i != lastModelIndex; i += frameStep)
      if (!viewer.isJmolDataFrame(i)) {
        bsVisibleFrames.set(i);
        nDisplayed++;
        frameDisplayed = i;
      }
    if (firstModelIndex == lastModelIndex || !viewer.isJmolDataFrame(lastModelIndex)
        || nDisplayed == 0) {
      bsVisibleFrames.set(lastModelIndex);
      if (nDisplayed == 0)
        firstModelIndex = lastModelIndex;
      nDisplayed = 0;
    }
    if (nDisplayed == 1 && currentModelIndex < 0)
      setCurrentModelIndex(frameDisplayed);
  }

  AnimationThread animationThread;

  boolean inMotion = false;
  void setInMotion(boolean inMotion) {
    this.inMotion = inMotion;
  }

  int holdRepaint = 0;
  boolean repaintPending;
  void pushHoldRepaint() {
    ++holdRepaint;
    //System.out.println("Repaintmanager pushHold  " + holdRepaint + " " + repaintPending + " " + Thread.currentThread());
  }

  void popHoldRepaint() {
    //System.out.println("Repaintmanager popHold  " + holdRepaint + " " + repaintPending + " " + Thread.currentThread());
    --holdRepaint;
    if (holdRepaint <= 0) {
      holdRepaint = 0;
      repaintPending = true;
      viewer.repaint();
    }
  }

  boolean refresh() {
    //System.out.println("Repaintmanager refresh  " + holdRepaint + " " + repaintPending + " " + Thread.currentThread());
    if (repaintPending)
      return false;
    repaintPending = true;
    if (holdRepaint == 0) {
      viewer.repaint();
    }
    return true;
  }


  synchronized void requestRepaintAndWait() {
    //System.out.println("Repaintmanager requestRepaintAndWait  " + holdRepaint + " " + repaintPending + " " + Thread.currentThread());
    viewer.repaint();
    try {
      wait();
    } catch (InterruptedException e) {
    }
  }

  synchronized void repaintDone() {
    //System.out.println("Repaintmanager repaintDone  " + holdRepaint + " " + repaintPending + " " + Thread.currentThread());
    repaintPending = false;
    notify(); // to cancel any wait in requestRepaintAndWait()
  }

  /*
  final Rectangle rectOversample = new Rectangle();
  boolean tOversample;

  void setOversample(boolean tOversample) {
    //not implemented --
    this.tOversample = tOversample;
  }
  */

  void releaseRenderer(int shapeID) {
    frameRenderer.clear(shapeID);
  }
  
  void render(Graphics3D g3d, ModelSet modelSet) {//, Rectangle rectClip
    if (!viewer.getRefreshing())
      return;
    System.out.println("repaint manager render " + modelSet);
    frameRenderer.render(g3d, modelSet); //, rectClip
    Rectangle band = viewer.getRubberBandSelection();
    if (band != null && g3d.setColix(viewer.getColixRubberband()))
      g3d.drawRect(band.x, band.y, 0, 0, band.width, band.height);
  }

  String generateOutput(String type, Graphics3D g3d, ModelSet modelSet, 
                        String fileName) {
    return frameRenderer.generateOutput(type, g3d, modelSet, fileName);
  }

  /****************************************************************
   * Animation support
   ****************************************************************/
  
  int firstModelIndex;
  int lastModelIndex;
  int frameStep;

  void initializePointers(int frameStep) {
    firstModelIndex = 0;
    int modelCount = viewer.getModelCount();
    lastModelIndex = (frameStep == 0 ? 0 
        : modelCount) - 1;
    this.frameStep = frameStep;
    viewer.setFrameVariables(firstModelIndex, lastModelIndex);
  }

  void clear() {
    clearAnimation();
    frameRenderer.clear(-1);
  }
  
  void clearAnimation() {
    setAnimationOn(false);
    setCurrentModelIndex(0);
    setAnimationDirection(1);
    setAnimationFps(10);
    setAnimationReplayMode(0, 0, 0);
    initializePointers(0);
  }

  Hashtable getAnimationInfo(){
    Hashtable info = new Hashtable();
    info.put("firstModelIndex", new Integer(firstModelIndex));
    info.put("lastModelIndex", new Integer(lastModelIndex));
    info.put("animationDirection", new Integer(animationDirection));
    info.put("currentDirection", new Integer(currentDirection));
    info.put("displayModelIndex", new Integer(currentModelIndex));
    info.put("displayModelNumber", viewer.getModelNumberDotted(currentModelIndex));
    info.put("displayModelName", (currentModelIndex >=0 ? viewer.getModelName(currentModelIndex) : ""));
    info.put("animationFps", new Integer(animationFps));
    info.put("animationReplayMode", getAnimationModeName());
    info.put("firstFrameDelay", new Float(firstFrameDelay));
    info.put("lastFrameDelay", new Float(lastFrameDelay));
    info.put("animationOn", Boolean.valueOf(animationOn));
    info.put("animationPaused", Boolean.valueOf(animationPaused));
    return info;
  }
 
  String getState(StringBuffer sfunc) {
    int modelCount = viewer.getModelCount();
    if (modelCount < 2)
      return "";
    StringBuffer commands = new StringBuffer();
    if (sfunc != null) {
      sfunc.append("  _setFrameState;\n");
      commands.append("function _setFrameState();\n");
    }
    commands.append("# frame state;\n");
    
    commands.append("# modelCount ").append(modelCount)
        .append(";\n# first ").append(
             viewer.getModelNumberDotted(0)).append(";\n# last ").append(
             viewer.getModelNumberDotted(modelCount - 1)).append(";\n");
    if (backgroundModelIndex >= 0)
      StateManager.appendCmd(commands, "set backgroundModel " + 
          viewer.getModelNumberDotted(backgroundModelIndex));
    BitSet bs = viewer.getFrameOffsets();
    if (bs != null)
      StateManager.appendCmd(commands, "frame align " + Escape.escape(bs));
    StateManager.appendCmd(commands, 
        "frame RANGE " + viewer.getModelNumberDotted(firstModelIndex) + " "
            + viewer.getModelNumberDotted(lastModelIndex));
    StateManager.appendCmd(commands, 
        "animation DIRECTION " + (animationDirection == 1 ? "+1" : "-1"));
    StateManager.appendCmd(commands, "animation FPS " + animationFps);
    StateManager.appendCmd(commands, "animation MODE " + getAnimationModeName()
        + " " + firstFrameDelay + " " + lastFrameDelay);
    StateManager.appendCmd(commands, "frame " + viewer.getModelNumberDotted(currentModelIndex));
    StateManager.appendCmd(commands, "animation "
            + (!animationOn ? "OFF" : currentDirection == 1 ? "PLAY"
                : "PLAYREV"));
    if (animationOn && animationPaused)
      StateManager.appendCmd(commands, "animation PAUSE");
    if (sfunc != null)
      commands.append("end function;\n\n");
    return commands.toString();
  }
  
  int animationDirection = 1;
  int currentDirection = 1;
  void setAnimationDirection(int animationDirection) {
    this.animationDirection = animationDirection;
    //if (animationReplayMode != ANIMATION_LOOP)
      //currentDirection = 1;
  }

  int animationFps;  // set in stateManager
  
  void setAnimationFps(int animationFps) {
    this.animationFps = animationFps;
  }

  // 0 = once
  // 1 = loop
  // 2 = palindrome
  
  final static int ANIMATION_ONCE = 0;
  final static int ANIMATION_LOOP = 1;
  final static int ANIMATION_PALINDROME = 2;
  
  int animationReplayMode = 0;
  float firstFrameDelay, lastFrameDelay;
  int firstFrameDelayMs, lastFrameDelayMs;
  void setAnimationReplayMode(int animationReplayMode,
                                     float firstFrameDelay,
                                     float lastFrameDelay) {
    this.firstFrameDelay = firstFrameDelay > 0 ? firstFrameDelay : 0;
    firstFrameDelayMs = (int)(this.firstFrameDelay * 1000);
    this.lastFrameDelay = lastFrameDelay > 0 ? lastFrameDelay : 0;
    lastFrameDelayMs = (int)(this.lastFrameDelay * 1000);
    if (animationReplayMode >= ANIMATION_ONCE && animationReplayMode <= ANIMATION_PALINDROME)
      this.animationReplayMode = animationReplayMode;
    else
      Logger.error("invalid animationReplayMode:" + animationReplayMode);
  }

  void setAnimationRange(int framePointer, int framePointer2) {
    int modelCount = viewer.getModelCount();
    if (framePointer < 0) framePointer = 0;
    if (framePointer2 < 0) framePointer2 = modelCount;
    if (framePointer >= modelCount) framePointer = modelCount - 1;
    if (framePointer2 >= modelCount) framePointer2 = modelCount - 1;
    firstModelIndex = framePointer;
    lastModelIndex = framePointer2;
    frameStep = (framePointer2 < framePointer ? -1 : 1);
    rewindAnimation();
  }

  boolean animationOn = false;
  private void animationOn(boolean TF) {
    animationOn = TF; 
    viewer.setBooleanProperty("_animating", TF);
  }
  
  boolean animationPaused = false;
  void setAnimationOn(boolean animationOn) {
    if (! animationOn || ! viewer.haveModelSet()) {
      setAnimationOff(false);
      return;
    }
    viewer.refresh(3, "Viewer:setAnimationOn");
    setAnimationRange(-1, -1);
    resumeAnimation();
  }

  void setAnimationOff(boolean isPaused) {
    if (animationThread != null) {
      animationThread.interrupt();
      animationThread = null;
    }
    animationPaused = isPaused;
    viewer.refresh(3, "Viewer:setAnimationOff");
    animationOn(false);
    setStatusFrameChanged();
  }

  void pauseAnimation() {
    setAnimationOff(true);
  }
  
  void reverseAnimation() {
    currentDirection = -currentDirection;
    if (!animationOn)
      resumeAnimation();
  }
  
  int intAnimThread = 0;
  void resumeAnimation() {
    if(currentModelIndex < 0)
      setAnimationRange(firstModelIndex, lastModelIndex);
    if (viewer.getModelCount() <= 1) {
      animationOn(false);
      return;
    }
    animationOn(true);
    animationPaused = false;
    if (animationThread == null) {
      intAnimThread++;
      animationThread = new AnimationThread(firstModelIndex, lastModelIndex, intAnimThread);
      animationThread.start();
    }
  }
  
  boolean setAnimationNext() {
    return setAnimationRelative(animationDirection);
  }

  void setAnimationLast() {
    setCurrentModelIndex(animationDirection > 0 ? lastModelIndex : firstModelIndex);
  }

  void rewindAnimation() {
    setCurrentModelIndex(animationDirection > 0 ? firstModelIndex : lastModelIndex);
    currentDirection = 1;
    viewer.setFrameVariables(firstModelIndex, lastModelIndex);
  }
  
  boolean setAnimationPrevious() {
    return setAnimationRelative(-animationDirection);
  }

  boolean setAnimationRelative(int direction) {
    int frameStep = this.frameStep * direction * currentDirection;
    int modelIndexNext = currentModelIndex + frameStep;
    boolean isDone = (modelIndexNext > firstModelIndex
        && modelIndexNext > lastModelIndex || modelIndexNext < firstModelIndex
        && modelIndexNext < lastModelIndex);    
    if (isDone) {
      switch (animationReplayMode) {
      case ANIMATION_ONCE:
        return false;
      case ANIMATION_LOOP:
        modelIndexNext = (animationDirection == currentDirection ? firstModelIndex
            : lastModelIndex);
        break;
      case ANIMATION_PALINDROME:
        currentDirection = -currentDirection;
        modelIndexNext -= 2 * frameStep;
      }
    }
    //Logger.debug("next="+modelIndexNext+" dir="+currentDirection+" isDone="+isDone);
    int nModels = viewer.getModelCount();
    if (modelIndexNext < 0 || modelIndexNext >= nModels)
      return false;
    setCurrentModelIndex(modelIndexNext);
    return true;
  }
  
  String getAnimationModeName() {
    switch (animationReplayMode) {
    case ANIMATION_LOOP:
      return "LOOP";
    case ANIMATION_PALINDROME:
      return "PALINDROME";
    default:
      return "ONCE";
    }
  }

  class AnimationThread extends Thread implements Runnable {
    final int framePointer;
    final int framePointer2;
    int intThread;

    AnimationThread(int framePointer, int framePointer2, int intAnimThread) {
      this.framePointer = framePointer;
      this.framePointer2 = framePointer2;
      this.setName("AnimationThread");
      intThread = intAnimThread;
    }

    public void run() {
      long timeBegin = System.currentTimeMillis();
      int targetTime = 0;
      int sleepTime;
      //int holdTime = 0;
      if (Logger.debugging)
        Logger.debug("animation thread " + intThread + " running");
      requestRepaintAndWait();

      try {
        sleepTime = targetTime - (int) (System.currentTimeMillis() - timeBegin);
        if (sleepTime > 0)
          Thread.sleep(sleepTime);
        boolean isFirst = true;
        while (!isInterrupted() && animationOn) {
          //System.out.println(" anim thread " + currentModelIndex + " " + framePointer);
          if (currentModelIndex == framePointer) {
            targetTime += firstFrameDelayMs;
            sleepTime = targetTime
                - (int) (System.currentTimeMillis() - timeBegin);
            if (sleepTime > 0)
              Thread.sleep(sleepTime);
          }
          if (currentModelIndex == framePointer2) {
            targetTime += lastFrameDelayMs;
            sleepTime = targetTime
                - (int) (System.currentTimeMillis() - timeBegin);
            if (sleepTime > 0)
              Thread.sleep(sleepTime);
          }
          if (!isFirst && !repaintPending && !setAnimationNext()) {
            Logger.debug("animation thread " + intThread + " exiting");
            setAnimationOff(false);
            return;
          }
          isFirst = false;
          targetTime += (1000 / animationFps);
          sleepTime = targetTime
              - (int) (System.currentTimeMillis() - timeBegin);

          while(!isInterrupted() && animationOn && !viewer.getRefreshing()) {
            Thread.sleep(10); 
          }
          viewer.refresh(1, "animationThread");
          sleepTime = targetTime
              - (int) (System.currentTimeMillis() - timeBegin);
          if (sleepTime > 0)
            Thread.sleep(sleepTime);
        }
      } catch (InterruptedException ie) {
        Logger.debug("animation thread interrupted!");
        try {
          setAnimationOn(false);
        } catch (Exception e) {
          // null pointer -- don't care;
        }
      }
    }
  }
 
}
