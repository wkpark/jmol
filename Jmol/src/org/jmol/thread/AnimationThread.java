/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.thread;

import org.jmol.util.Logger;
import org.jmol.viewer.AnimationManager;
import org.jmol.viewer.Viewer;

public class AnimationThread extends JmolThread {
  /**
   * 
   */
  private final AnimationManager animationManager;
  private final Viewer viewer;
  private final int framePointer;
  private final int framePointer2;
  private int intThread;
  

  public AnimationThread(AnimationManager animationManager, Viewer viewer, int framePointer, int framePointer2, int intAnimThread) {
    this.animationManager = animationManager;
    this.viewer = viewer;
    this.framePointer = framePointer;
    this.framePointer2 = framePointer2;
    setMyName("AnimationThread");
    intThread = intAnimThread;
  }

  @Override
  public void run() {
    long timeBegin = System.currentTimeMillis();
    int targetTime = 0;
    int sleepTime;
    //int holdTime = 0;
    if (Logger.debugging)
      Logger.debug("animation thread " + intThread + " running");
    viewer.requestRepaintAndWait();

    try {
      sleepTime = targetTime - (int) (System.currentTimeMillis() - timeBegin);
      if (sleepTime > 0)
        Thread.sleep(sleepTime);
      boolean isFirst = true;
      while (!isInterrupted() && animationManager.animationOn) {
        if (animationManager.currentModelIndex == framePointer) {
          targetTime += animationManager.firstFrameDelayMs;
          sleepTime = targetTime
              - (int) (System.currentTimeMillis() - timeBegin);
          if (sleepTime > 0)
            Thread.sleep(sleepTime);
        }
        if (animationManager.currentModelIndex == framePointer2) {
          targetTime += animationManager.lastFrameDelayMs;
          sleepTime = targetTime
              - (int) (System.currentTimeMillis() - timeBegin);
          if (sleepTime > 0)
            Thread.sleep(sleepTime);
        }
        if (!isFirst && animationManager.lastModelPainted == animationManager.currentModelIndex && !animationManager.setAnimationNext()) {
          Logger.debug("animation thread " + intThread + " exiting");
          animationManager.setAnimationOff(false);
          return;
        }
        isFirst = false;
        targetTime += (int)((1000f / animationManager.animationFps) + viewer.getFrameDelayMs(animationManager.currentModelIndex));
        sleepTime = targetTime
            - (int) (System.currentTimeMillis() - timeBegin);

        while(!isInterrupted() && animationManager.animationOn && !viewer.getRefreshing()) {
          Thread.sleep(10); 
        }
        if (!viewer.getSpinOn())
          viewer.refresh(1, "animationThread");
        sleepTime = targetTime
            - (int) (System.currentTimeMillis() - timeBegin);
        if (sleepTime > 0)
          Thread.sleep(sleepTime);
      }
    } catch (InterruptedException ie) {
      Logger.debug("animation thread interrupted!");
      try {
        animationManager.setAnimationOn(false);
      } catch (Exception e) {
        // null pointer -- don't care;
      }
    }
  }
}