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

import java.util.List;
import java.util.Map;

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

  private Map<String, Object> movie;

  private int currentFrameIndex;

  void clear() {
    setMovie(null);
    setAnimationOn(false);
    setCurrentModelIndex(0, true);
    currentDirection = 1;
    setAnimationDirection(1);
    setAnimationFps(10);
    setAnimationReplayMode(EnumAnimationMode.ONCE, 0, 0);
    initializePointers(0);
  }
  
  @SuppressWarnings("unchecked")
  private void setFrame(int frameIndex) {
    if (movie == null) {
      setCurrentModelIndex(frameIndex, true);
      return;
    }
    currentModelIndex = -1;
    if (frameIndex == -1)
      frameIndex = ((Integer) movie.get("currentFrame")).intValue();
    currentFrameIndex = frameIndex;
    List<BitSet> bs = (List<BitSet>) movie.get("states");
    List<Object> frames = (List<Object>) movie.get("frames");
    if (bs == null || frames == null || frameIndex >= frames.size())
      return;
    int iState = ((Integer) frames.get(frameIndex)).intValue();
    viewer.displayAtoms(bs.get(iState), true, false, null, true);
    setViewer(true);
 }

  void setCurrentModelIndex(int modelIndex, boolean clearBackgroundModel) {
    if (movie != null) {
      setFrame(modelIndex);
      return;
    }
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
        boolean toDataFrame = isJmolDataFrameForModel(modelIndex);
        boolean fromDataFrame = isJmolDataFrameForModel(currentModelIndex);
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
    setViewer(clearBackgroundModel);
  }

  private void setViewer(boolean clearBackgroundModel) {
    viewer.setTrajectory(currentModelIndex);
    viewer.setFrameOffset(currentModelIndex);
    if (currentModelIndex == -1 && clearBackgroundModel)
      setBackgroundModelIndex(-1);  
    viewer.setTainted(true);
    setFrameRangeVisible();
    setStatusFrameChanged();
    if (viewer.modelSet != null && !viewer.getSelectAllModels())
        viewer.setSelectionSubset(viewer.getModelUndeletedAtomsBitSet(currentModelIndex));
  }

  private boolean isJmolDataFrameForModel(int i) {
    return movie == null && viewer.isJmolDataFrameForModel(i);
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
    if (movie != null) {
      bsVisibleFrames.setBits(0, viewer.getModelCount());
      return;
    }
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
      if (!isJmolDataFrameForModel(i)) {
        bsVisibleFrames.set(i);
        nDisplayed++;
        frameDisplayed = i;
      }
    if (firstModelIndex == lastModelIndex || !isJmolDataFrameForModel(lastModelIndex)
        || nDisplayed == 0) {
      bsVisibleFrames.set(lastModelIndex);
      if (nDisplayed == 0)
        firstModelIndex = lastModelIndex;
      nDisplayed = 0;
    }
    if (nDisplayed == 1 && currentModelIndex < 0)
      setFrame(frameDisplayed);
    //System.out.println(bsVisibleFrames + "  " + frameDisplayed + " " + currentModelIndex);
   
  }

  void initializePointers(int frameStep) {
    firstModelIndex = 0;
    int modelCount = getFrameCount();
    lastModelIndex = (frameStep == 0 ? 0 
        : modelCount) - 1;
    this.frameStep = frameStep;
    viewer.setFrameVariables();
  }

  private int getFrameCount() {
    return (movie == null ? viewer.getModelCount() : ((Integer) movie.get("frameCount")).intValue());
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
    int modelCount = getFrameCount();
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
    if (getFrameCount() <= 1) {
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
    setFrame(animationDirection > 0 ? lastModelIndex : firstModelIndex);
  }

  void rewindAnimation() {
    setFrame(animationDirection > 0 ? firstModelIndex : lastModelIndex);
    currentDirection = 1;
    viewer.setFrameVariables();
  }
  
  boolean setAnimationPrevious() {
    return setAnimationRelative(-animationDirection);
  }

  boolean setAnimationRelative(int direction) {
    int frameStep = this.frameStep * direction * currentDirection;
    int frameNext = getCurrentFrame() + frameStep;
    boolean isDone = (frameNext > firstModelIndex
        && frameNext > lastModelIndex || frameNext < firstModelIndex
        && frameNext < lastModelIndex);    
    if (isDone) {
      switch (animationReplayMode) {
      case ONCE:
        return false;
      case LOOP:
        frameNext = (animationDirection == currentDirection ? firstModelIndex
            : lastModelIndex);
        break;
      case PALINDROME:
        currentDirection = -currentDirection;
        frameNext -= 2 * frameStep;
      }
    }
    //Logger.debug("next="+modelIndexNext+" dir="+currentDirection+" isDone="+isDone);
    if (frameNext < 0 || frameNext >= getFrameCount())
      return false;
    setFrame(frameNext);
    return true;
  }

  float getAnimRunTimeSeconds() {
    int modelCount = getFrameCount();
    if (firstModelIndex == lastModelIndex
        || lastModelIndex < 0 || firstModelIndex < 0
        || lastModelIndex >= modelCount
        || firstModelIndex >= modelCount)
      return  0;
    int i0 = Math.min(firstModelIndex, lastModelIndex);
    int i1 = Math.max(firstModelIndex, lastModelIndex);
    float nsec = 1f * (i1 - i0) / animationFps + firstFrameDelay
        + lastFrameDelay;
    for (int i = i0; i <= i1; i++)
      nsec += viewer.getFrameDelayMs(i) / 1000f;
    return nsec;
  }

  public void setMovie(Map<String, Object> info) {
    
    movie = info;
    
  }

  public int getCurrentFrame() {
    return (movie == null ? currentModelIndex : currentFrameIndex);
  }
 
}
