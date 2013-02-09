/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 10:56:39 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11127 $
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

import org.jmol.thread.AnimationThread;
import org.jmol.util.BitSet;

import org.jmol.constant.EnumAnimationMode;
import org.jmol.modelset.ModelSet;

public class AnimationManager {

  protected Viewer viewer;
  
  AnimationManager(Viewer viewer) {
    this.viewer = viewer;
  }

  EnumAnimationMode animationReplayMode = EnumAnimationMode.ONCE;

  public boolean animationOn;
  boolean animationPaused;
  boolean inMotion;
  
  public int animationFps;  // set in stateManager
  int animationDirection = 1;
  int currentDirection = 1;
  public int currentModelIndex;
  int firstModelIndex;
  int frameStep;
  int lastModelIndex;
   
  public int firstFrameDelayMs;
  public int lastFrameDelayMs;
  public int lastModelPainted;
  
  private AnimationThread animationThread;
  int backgroundModelIndex = -1;
  private final BitSet bsVisibleFrames = new BitSet();
  
  BitSet getVisibleFramesBitSet() {
    return bsVisibleFrames;
  }
  
  float firstFrameDelay;
  private int intAnimThread;
  float lastFrameDelay = 1;

  void setCurrentModelIndex(int modelIndex, boolean clearBackgroundModel) {
    if (modelIndex < 0)
      setAnimationOff(false);
    int formerModelIndex = currentModelIndex;
    ModelSet modelSet = viewer.getModelSet();
    int modelCount = (modelSet == null ? 0 : modelSet.modelCount);
    if (modelCount == 1)
      currentModelIndex = modelIndex = 0;
    else if (modelIndex < 0 || modelIndex >= modelCount)
      modelIndex = -1;
    String ids = null;
    boolean isSameSource = false;
    if (currentModelIndex != modelIndex) {
      if (modelCount > 0) {
        boolean toDataFrame = viewer.isJmolDataFrameForModel(modelIndex);
        boolean fromDataFrame = viewer.isJmolDataFrameForModel(currentModelIndex);
        if (fromDataFrame)
          viewer.setJmolDataFrame(null, -1, currentModelIndex);
        if (currentModelIndex != -1)
          viewer.saveModelOrientation();
        if (fromDataFrame || toDataFrame) {
          ids = viewer.getJmolFrameType(modelIndex) 
          + " "  + modelIndex + " <-- " 
          + " " + currentModelIndex + " " 
          + viewer.getJmolFrameType(currentModelIndex);
          
          isSameSource = (viewer.getJmolDataSourceFrame(modelIndex) == viewer
              .getJmolDataSourceFrame(currentModelIndex));
        }
      }
      currentModelIndex = modelIndex;
      if (ids != null) {
        if (modelIndex >= 0)
          viewer.restoreModelOrientation(modelIndex);
        if (isSameSource && ids.indexOf("quaternion") >= 0 
            && ids.indexOf("plot") < 0
            && ids.indexOf("ramachandran") < 0
            && ids.indexOf(" property ") < 0) {
          viewer.restoreModelRotation(formerModelIndex);
        }
      }
    }
    viewer.setTrajectory(currentModelIndex);
    viewer.setFrameOffset(currentModelIndex);
    if (currentModelIndex == -1 && clearBackgroundModel)
      setBackgroundModelIndex(-1);  
    viewer.setTainted(true);
    setFrameRangeVisible();
    setStatusFrameChanged();
    if (modelSet != null) {
      if (!viewer.getSelectAllModels())
        viewer.setSelectionSubset(viewer.getModelUndeletedAtomsBitSet(currentModelIndex));
    }
  
  }

  void setBackgroundModelIndex(int modelIndex) {
    ModelSet modelSet = viewer.getModelSet();
    if (modelSet == null || modelIndex < 0 || modelIndex >= modelSet.modelCount)
      modelIndex = -1;
    backgroundModelIndex = modelIndex;
    if (modelIndex >= 0)
      viewer.setTrajectory(modelIndex);
    viewer.setTainted(true);
    setFrameRangeVisible(); 
  }
  
  private void setStatusFrameChanged() {
    if (viewer.getModelSet() != null)
      viewer.setStatusFrameChanged(animationOn ? -2 - currentModelIndex
          : currentModelIndex);
  }
  
  private void setFrameRangeVisible() {
    bsVisibleFrames.clearAll();
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
      if (!viewer.isJmolDataFrameForModel(i)) {
        bsVisibleFrames.set(i);
        nDisplayed++;
        frameDisplayed = i;
      }
    if (firstModelIndex == lastModelIndex || !viewer.isJmolDataFrameForModel(lastModelIndex)
        || nDisplayed == 0) {
      bsVisibleFrames.set(lastModelIndex);
      if (nDisplayed == 0)
        firstModelIndex = lastModelIndex;
      nDisplayed = 0;
    }
    if (nDisplayed == 1 && currentModelIndex < 0)
      setCurrentModelIndex(frameDisplayed, true);
  }

  void initializePointers(int frameStep) {
    firstModelIndex = 0;
    int modelCount = viewer.getModelCount();
    lastModelIndex = (frameStep == 0 ? 0 
        : modelCount) - 1;
    this.frameStep = frameStep;
    viewer.setFrameVariables();
  }

  void clear() {
    setAnimationOn(false);
    setCurrentModelIndex(0, true);
    currentDirection = 1;
    setAnimationDirection(1);
    setAnimationFps(10);
    setAnimationReplayMode(EnumAnimationMode.ONCE, 0, 0);
    initializePointers(0);
  }
  
  void setAnimationDirection(int animationDirection) {
    this.animationDirection = animationDirection;
    //if (animationReplayMode != ANIMATION_LOOP)
      //currentDirection = 1;
  }

  void setAnimationFps(int animationFps) {
    this.animationFps = animationFps;
  }

  // 0 = once
  // 1 = loop
  // 2 = palindrome
  
  void setAnimationReplayMode(EnumAnimationMode animationReplayMode,
                                     float firstFrameDelay,
                                     float lastFrameDelay) {
    this.firstFrameDelay = firstFrameDelay > 0 ? firstFrameDelay : 0;
    firstFrameDelayMs = (int)(this.firstFrameDelay * 1000);
    this.lastFrameDelay = lastFrameDelay > 0 ? lastFrameDelay : 0;
    lastFrameDelayMs = (int)(this.lastFrameDelay * 1000);
    this.animationReplayMode = animationReplayMode;
    viewer.setFrameVariables();
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

  private void animationOn(boolean TF) {
    animationOn = TF; 
    viewer.setBooleanProperty("_animating", TF);
  }
  
  public void setAnimationOn(boolean animationOn) {
    if (!animationOn || !viewer.haveModelSet() || viewer.isHeadless()) {
      setAnimationOff(false);
      return;
    }
    if (!viewer.getSpinOn())
      viewer.refresh(3, "Viewer:setAnimationOn");
    setAnimationRange(-1, -1);
    resumeAnimation();
  }

  public void setAnimationOff(boolean isPaused) {
    if (animationThread != null) {
      animationThread.interrupt();
      animationThread = null;
    }
    animationPaused = isPaused;
    if (!viewer.getSpinOn())
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
  
  void repaintDone() {
    lastModelPainted = currentModelIndex;
  }
  
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
      animationThread = new AnimationThread(this, viewer, firstModelIndex, lastModelIndex, intAnimThread);
      animationThread.start();
    }
  }
  
  public boolean setAnimationNext() {
    return setAnimationRelative(animationDirection);
  }

  void setAnimationLast() {
    setCurrentModelIndex(animationDirection > 0 ? lastModelIndex : firstModelIndex, true);
  }

  void rewindAnimation() {
    setCurrentModelIndex(animationDirection > 0 ? firstModelIndex : lastModelIndex, true);
    currentDirection = 1;
    viewer.setFrameVariables();
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
      case ONCE:
        return false;
      case LOOP:
        modelIndexNext = (animationDirection == currentDirection ? firstModelIndex
            : lastModelIndex);
        break;
      case PALINDROME:
        currentDirection = -currentDirection;
        modelIndexNext -= 2 * frameStep;
      }
    }
    //Logger.debug("next="+modelIndexNext+" dir="+currentDirection+" isDone="+isDone);
    int nModels = viewer.getModelCount();
    if (modelIndexNext < 0 || modelIndexNext >= nModels)
      return false;
    setCurrentModelIndex(modelIndexNext, true);
    return true;
  }

  float getAnimRunTimeSeconds() {
    if (firstModelIndex == lastModelIndex
        || lastModelIndex < 0 || firstModelIndex < 0
        || lastModelIndex >= viewer.getModelCount()
        || firstModelIndex >= viewer.getModelCount())
      return  0;
    int i0 = Math.min(firstModelIndex, lastModelIndex);
    int i1 = Math.max(firstModelIndex, lastModelIndex);
    float nsec = 1f * (i1 - i0) / animationFps + firstFrameDelay
        + lastFrameDelay;
    for (int i = i0; i <= i1; i++)
      nsec += viewer.getFrameDelayMs(i) / 1000f;
    return nsec;
  }
 
}
