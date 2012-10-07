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

import java.util.Date;
import java.util.List;


import org.jmol.script.Token;
import org.jmol.util.BitSet;
import org.jmol.util.Logger;
import org.jmol.util.Point3f;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public class SpinThread extends JmolThread {
  /**
   * 
   */
  private final TransformManager transformManager;
  private final Viewer viewer;
  private float endDegrees;
  private List<Point3f> endPositions;
  private float nDegrees;
  private BitSet bsAtoms;
  private boolean isNav;
  private boolean isGesture;
  private boolean isReset;
  
  public boolean isGesture() {
    return isGesture;
  }
  
  public SpinThread(TransformManager transformManager, Viewer viewer, float endDegrees, List<Point3f> endPositions, BitSet bsAtoms, boolean isNav, boolean isGesture) {
    this.transformManager = transformManager;
    this.viewer = viewer;
    this.endDegrees = Math.abs(endDegrees);
    this.endPositions = endPositions;
    this.bsAtoms = bsAtoms;
    this.isNav = isNav;
    this.isGesture = isGesture;
    setMyName("SpinThread" + new Date());
  }

  @Override
  public void run() {
    float myFps = (isNav ? transformManager.navFps : transformManager.spinFps);
    viewer.getGlobalSettings().setParamB(isNav ? "_navigating" : "_spinning", true);
    int i = 0;
    long timeBegin = System.currentTimeMillis();
    float angle = 0;
    boolean haveNotified = false;
    while (!isInterrupted()) {
      if (isNav && myFps != transformManager.navFps) {
        myFps = transformManager.navFps;
        i = 0;
        timeBegin = System.currentTimeMillis();
      } else if (!isNav && myFps != transformManager.spinFps && bsAtoms == null) {
        myFps = transformManager.spinFps;
        i = 0;
        timeBegin = System.currentTimeMillis();
      }
      if (myFps == 0 || !(isNav ? transformManager.navOn : transformManager.spinOn)) {
        transformManager.setSpinOn(false);
        transformManager.setNavOn(false);
        break;
      }
      boolean navigatingSurface = viewer.getNavigateSurface();
      boolean refreshNeeded = (isNav ?  (navigatingSurface || (transformManager.navX != 0 || transformManager.navY != 0)) || transformManager.navZ != 0
          : transformManager.isSpinInternal && transformManager.internalRotationAxis.angle != 0 
          || transformManager.isSpinFixed && transformManager.fixedRotationAxis.angle != 0 
          || !transformManager.isSpinFixed && !transformManager.isSpinInternal && (transformManager.spinX != 0 || transformManager.spinY != 0 || transformManager.spinZ != 0));
      ++i;
      int targetTime = (int) (i * 1000 / myFps);
      int currentTime = (int) (System.currentTimeMillis() - timeBegin);
      int sleepTime = (targetTime - currentTime);
      //System.out.println(targetTime + " " + currentTime + " " + sleepTime);
      if (sleepTime <= 0) {
        if (!haveNotified)
          Logger.info("spinFPS is set too fast (" + myFps + ") -- can't keep up!");
        haveNotified = true;
      } else {
        boolean isInMotion = (bsAtoms == null && viewer.getInMotion());
        if (isInMotion) {
          if (isGesture)
            break;
          sleepTime += 1000;
        }
        try {
          if (refreshNeeded && (transformManager.spinOn || transformManager.navOn) && !isInMotion) {
            if (isNav) {
              transformManager.setNavigationOffsetRelative(navigatingSurface);
            } else if (transformManager.isSpinInternal || transformManager.isSpinFixed) {
              angle = (transformManager.isSpinInternal ? transformManager.internalRotationAxis
                  : transformManager.fixedRotationAxis).angle / myFps;
              if (transformManager.isSpinInternal) {
                transformManager.rotateAxisAngleRadiansInternal(angle, bsAtoms);
              } else {
                transformManager.rotateAxisAngleRadiansFixed(angle, bsAtoms);
              }
              nDegrees += Math.abs(angle * TransformManager.degreesPerRadian);
              //System.out.println(i + " " + angle + " " + nDegrees);
            } else { // old way: Rx * Ry * Rz
              if (transformManager.spinX != 0) {
                transformManager.rotateXRadians(transformManager.spinX * JmolConstants.radiansPerDegree / myFps, null);
              }
              if (transformManager.spinY != 0) {
                transformManager.rotateYRadians(transformManager.spinY * JmolConstants.radiansPerDegree / myFps, null);
              }
              if (transformManager.spinZ != 0) {
                transformManager.rotateZRadians(transformManager.spinZ * JmolConstants.radiansPerDegree / myFps);
              }
            }
            while (!isInterrupted() && !viewer.getRefreshing()) {
              Thread.sleep(10);
            }
            if (bsAtoms == null)
              viewer.refresh(1, "SpinThread:run()");
            else
              viewer.requestRepaintAndWait();
            //System.out.println(angle * degreesPerRadian + " " + count + " " + nDegrees + " " + endDegrees);
            if (!isNav && nDegrees >= endDegrees - 0.001)
              transformManager.setSpinOn(false);
          }
          Thread.sleep(sleepTime);
          if (isReset)
            break;
        } catch (InterruptedException e) {
          break;
        }
      }
    }
    if (bsAtoms != null && endPositions != null) {
      // when the standard deviations of the end points was
      // exact, we know that we want EXACTLY those final positions
      viewer.setAtomCoord(bsAtoms, Token.xyz, endPositions);
      bsAtoms = null;
      endPositions = null;
    }
    if (!isReset)
      transformManager.setSpinOn(false);
  }

  public void reset() {
    isReset = true;
    interrupt();
  }

}