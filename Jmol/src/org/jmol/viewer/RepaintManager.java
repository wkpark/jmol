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
  int currentTrajectory = -1;
  
  boolean isTrajectory;

  void setTrajectory(int iTraj) {
    isTrajectory = (iTraj >= 0); 
    currentTrajectory = iTraj;
  }


  void setCurrentModelIndex(int modelIndex) {
    setCurrentModelIndex(modelIndex, true);  
  }
  
  void setCurrentModelIndex(int modelIndex, boolean clearBackgroundModel) {
      ModelSet modelSet = viewer.getModelSet();
    if (modelIndex != 0 && isTrajectory)
      viewer.setTrajectory(-1);
    if (modelSet != null && currentModelIndex != modelIndex) {
      boolean fromDataFrame = viewer.isJmolDataFrame(currentModelIndex);
      boolean toDataFrame = viewer.isJmolDataFrame(modelIndex);
      viewer.saveOrientation(viewer.getJmolFrameType(currentModelIndex));
      //System.out.println("saving orientation for " + currentModelIndex + " " + viewer.getJmolDataFrameType(currentModelIndex));
      if (fromDataFrame || toDataFrame) {
        viewer.restoreOrientation(viewer.getJmolFrameType(modelIndex), -1);
        //System.out.println("restoring orientation for " + modelIndex + " " + viewer.getJmolDataFrameType(modelIndex));
      }
    }
    if (modelSet == null || modelIndex < 0
        || modelIndex >= modelSet.getModelCount())
      currentModelIndex = -1;
    else
      currentModelIndex = modelIndex;
    if (currentModelIndex == -1 && clearBackgroundModel)
      setBackgroundModelIndex(-1);
    if (modelSet != null && currentModelIndex >= 0) {
      // entering data frame
      if (viewer.isJmolDataFrame(currentModelIndex)) {
        viewer.restoreOrientation("frameIndex" + currentModelIndex, 0);
      }
    }
    viewer.setTainted(true);
    setFrameRangeVisible();
    if (modelSet != null)
      setStatusFrameChanged();
  }

  
  private void setStatusFrameChanged() {
    int i = (isTrajectory ? currentTrajectory : currentModelIndex);
    viewer.setStatusFrameChanged(animationOn ? -2 - i : i);
  }
  
  int backgroundModelIndex = -1;
  void setBackgroundModelIndex(int modelIndex) {
    ModelSet modelSet = viewer.getModelSet();
    if (modelSet == null || modelIndex < 0 || modelIndex >= modelSet.getModelCount())
      modelIndex = -1;
    backgroundModelIndex = modelIndex;
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
    if (frameStep == 0 || isTrajectory)
      return;
    for (int i = firstModelIndex; i != lastModelIndex; i += frameStep)
      if (!viewer.isJmolDataFrame(i))
        bsVisibleFrames.set(i);
    bsVisibleFrames.set(lastModelIndex);    
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
  }

  void popHoldRepaint() {
    --holdRepaint;
    if (holdRepaint <= 0) {
      holdRepaint = 0;
      repaintPending = true;
      viewer.repaint();
    }
  }

  boolean refresh() {
    if (repaintPending)
      return false;
    repaintPending = true;
    if (holdRepaint == 0) {
      viewer.repaint();
    }
    return true;
  }


  synchronized void requestRepaintAndWait() {
    viewer.repaint();
    try {
      wait();
    } catch (InterruptedException e) {
    }
  }

  synchronized void repaintDone() {
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

  void render(Graphics3D g3d, ModelSet modelSet) {//, Rectangle rectClip
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
    isTrajectory = ((lastModelIndex = viewer.getTrajectoryCount()) > 1);
    int modelCount = viewer.getModelCount();
    lastModelIndex = (frameStep == 0 ? 0 
        : isTrajectory ? lastModelIndex 
        : modelCount) - 1;
    this.frameStep = frameStep;
  }

  void clear() {
    clearAnimation();
    frameRenderer.clear();
  }
  
  void clearAnimation() {
    setAnimationOn(false);
    setCurrentModelIndex(0);
    setTrajectory(-1);
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
    info.put("currentTrajectory", new Integer(currentTrajectory));
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
    commands.append("# frame state;\n# modelCount ")
        .append(modelCount).append(";\n# first ").append(
            viewer.getModelNumberDotted(0)).append(";\n# last ").append(
            viewer.getModelNumberDotted(modelCount - 1)).append(";\n");
    if (backgroundModelIndex >= 0)
      StateManager.appendCmd(commands, "set backgroundModel " + 
          viewer.getModelNumberDotted(backgroundModelIndex));
    StateManager.appendCmd(commands, 
        "frame RANGE " + viewer.getModelNumberDotted(firstModelIndex) + " "
            + viewer.getModelNumberDotted(lastModelIndex));
    StateManager.appendCmd(commands, 
        "animation DIRECTION " + (animationDirection == 1 ? "+1" : "-1"));
    StateManager.appendCmd(commands, "animation FPS " + animationFps);
    StateManager.appendCmd(commands, "animation MODE " + getAnimationModeName()
        + " " + firstFrameDelay + " " + lastFrameDelay);
    StateManager.appendCmd(commands, "frame " + viewer.getModelNumberDotted(currentModelIndex));
    if (currentTrajectory > -1)
      StateManager.appendCmd(commands, "trajectory " + currentTrajectory);
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

  void setAnimationRange(int framePointer, int framePointer2, boolean isTrajectory) {
    int modelCount = (isTrajectory ? viewer.getTrajectoryCount() : viewer.getModelCount());
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
    viewer.refresh(0, "Viewer:setAnimationOn");
    setAnimationRange(-1, -1, isTrajectory);
    resumeAnimation();
  }

  void setAnimationOff(boolean isPaused) {
    if (animationThread != null) {
      animationThread.interrupt();
      animationThread = null;
    }
    animationPaused = isPaused;
    viewer.refresh(0, "Viewer:setAnimationOff");
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
      setAnimationRange(firstModelIndex, lastModelIndex, isTrajectory);
    int nModels = (isTrajectory ? viewer.getTrajectoryCount() : viewer.getModelCount());
    if (nModels <= 1) {
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
    int i = animationDirection > 0 ? lastModelIndex : firstModelIndex;
    if (isTrajectory)
      viewer.setTrajectory(i);//will call this.setTrajectory()
    else 
      setCurrentModelIndex(i);
  }

  void rewindAnimation() {
    int i =  animationDirection > 0 ? firstModelIndex : lastModelIndex;
    if (isTrajectory)
      viewer.setTrajectory(i);//will call this.setTrajectory()
    else 
      setCurrentModelIndex(i);
    currentDirection = 1;
  }
  
  boolean setAnimationPrevious() {
    return setAnimationRelative(-animationDirection);
  }

  boolean setAnimationRelative(int direction) {
    int frameStep = this.frameStep * direction * currentDirection;
    int modelIndexNext = (isTrajectory ? currentTrajectory : currentModelIndex)
        + frameStep;
    boolean isDone = (modelIndexNext > firstModelIndex
        && modelIndexNext > lastModelIndex || modelIndexNext < firstModelIndex
        && modelIndexNext < lastModelIndex);
    
/*
     System.out.println("setAnimationRelative: " +
     " firstModelIndex=" + firstModelIndex +
     " displayModelIndex=" + currentModelIndex +
     " trajectory=" + currentTrajectory +
     " lastModelIndex=" + lastModelIndex +
     " currentDirection=" + currentDirection +
     " animationDirection=" + animationDirection +
     " direction=" + direction +
     " isDone="+isDone +
     " modelIndexNext=" + modelIndexNext +
     " modelCount=" + viewer.getModelCount() +
     " animationReplayMode=" + animationReplayMode +
     " animationDirection=" + animationDirection);     
*/
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
    int nModels = (isTrajectory ? viewer.getTrajectoryCount() : viewer.getModelCount());
    if (modelIndexNext < 0 || modelIndexNext >= nModels)
      return false;
    if (isTrajectory) {
      viewer.setTrajectory(modelIndexNext);//will call this.setTrajectory()
      viewer.setTainted(true);

    }    else
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
      intThread = intAnimThread;
    }

    public void run() {
      long timeBegin = System.currentTimeMillis();
      int targetTime = 0;
      int sleepTime;
      //int holdTime = 0;
      Logger.debug("animation thread " + intThread + " running");
      
      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
        Logger.debug("animation thread " + intThread + " running");
      }
      requestRepaintAndWait();
      
      try {
        sleepTime = targetTime - (int) (System.currentTimeMillis() - timeBegin);
        if (sleepTime > 0)
          Thread.sleep(sleepTime);
        boolean isFirst = true;
        while (!isInterrupted() && animationOn) {
          int i = (isTrajectory ? currentTrajectory : currentModelIndex);
          if (i == framePointer) {
            targetTime += firstFrameDelayMs;
            sleepTime = targetTime
                - (int) (System.currentTimeMillis() - timeBegin);
            if (sleepTime > 0)
              Thread.sleep(sleepTime);
          }
          if (i == framePointer2) {
            targetTime += lastFrameDelayMs;
            sleepTime = targetTime
                - (int) (System.currentTimeMillis() - timeBegin);
            if (sleepTime > 0)
              Thread.sleep(sleepTime);
          }
          //System.out.println(repaintPending + " frame " + currentModelIndex);
          if (!isFirst && !repaintPending && !setAnimationNext()) {
            Logger.debug("animation thread " + intThread + " exiting");
            setAnimationOff(false);
            return;
          }
          isFirst = false;
          targetTime += (1000 / animationFps);
          sleepTime = targetTime
              - (int) (System.currentTimeMillis() - timeBegin);
          //boolean autoFps = viewer.getAutoFps();
          //if (autoFps) {
            //System.out.println("requesting repaint for " + currentModelIndex);
            //requestRepaintAndWait();
          //} else
          refresh();
          sleepTime = targetTime
              - (int) (System.currentTimeMillis() - timeBegin);
          if (sleepTime > 0)  
            Thread.sleep(sleepTime);
          /*if (false && autoFps) {
            if (holdTime <= 0)
              holdTime = 10;
            int nHold = 0;
            //optimally we want 2 hold cycles
            while (repaintPending) {
              Thread.sleep(holdTime);
              nHold++;
            }
            holdTime *=(nHold - 1);
            if (nHold == 1)
              holdTime = holdTime / 2;
         } */
        }
      } catch (InterruptedException ie) {
        Logger.debug("animation thread interrupted!");
        setAnimationOn(false);
      }
    }
  }
 
}
