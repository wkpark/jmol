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


import javajs.util.V3;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public class MoveThread extends JmolThread {

  private TransformManager transformManager;
  private float floatSecondsTotal;
  private int iStep;
  private int timePerStep;
  private int totalSteps;
  private float radiansXStep;
  private float radiansYStep;
  private float radiansZStep;
  private V3 dRot;
  private V3 dTrans;
  private float dZoom;
  private float dSlab;
  private float zoomPercent0;
  private int slab;
  private float transX;
  private float transY;
  private float transZ;

  public MoveThread() {}
  
  @Override
  public int setManager(Object manager, Viewer vwr, Object params) {
    Object[] options = (Object[]) params;
    setViewer(vwr, "MoveThread");
    transformManager = (TransformManager) manager;
    dRot = (V3) options[0];
    dTrans = (V3) options[1];
    float[] f = (float[]) options[2];
    dZoom = f[0];
    dSlab = f[1];
    floatSecondsTotal = f[2];
    int fps = (int) f[3];

    slab = transformManager.getSlabPercentSetting();
    transX = transformManager.getTranslationXPercent();
    transY = transformManager.getTranslationYPercent();
    transZ = transformManager.getTranslationZPercent();

    timePerStep = 1000 / fps;
    totalSteps = (int) (fps * floatSecondsTotal);
    if (totalSteps <= 0)
      totalSteps = 1; // to catch a zero secondsTotal parameter
    float radiansPerDegreePerStep = (float) (1 / TransformManager.degreesPerRadian / totalSteps);
    radiansXStep = radiansPerDegreePerStep * dRot.x;
    radiansYStep = radiansPerDegreePerStep * dRot.y;
    radiansZStep = radiansPerDegreePerStep * dRot.z;
    zoomPercent0 = transformManager.zmPct;
    iStep = 0;
    return totalSteps;
  }
  

  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        if (floatSecondsTotal > 0)
          vwr.setInMotion(true);
        mode = MAIN;
        break;
      case MAIN:
        if (stopped || ++iStep >= totalSteps) {
          mode = FINISH;
          break;
        }
        if (dRot.x != 0)
          transformManager.rotateXRadians(radiansXStep, null);
        if (dRot.y != 0)
          transformManager.rotateYRadians(radiansYStep, null);
        if (dRot.z != 0)
          transformManager.rotateZRadians(radiansZStep);
        if (dZoom != 0)
          transformManager.zoomToPercent(zoomPercent0 + dZoom * iStep / totalSteps);
        if (dTrans.x != 0)
          transformManager.translateToPercent('x', transX + dTrans.x * iStep / totalSteps);
        if (dTrans.y != 0)
          transformManager.translateToPercent('y', transY + dTrans.y * iStep / totalSteps);
        if (dTrans.z != 0)
          transformManager.translateToPercent('z', transZ + dTrans.z * iStep / totalSteps);
        if (dSlab != 0)
          transformManager.slabToPercent((int) Math.floor(slab + dSlab * iStep / totalSteps));
        int timeSpent = (int) (System.currentTimeMillis() - startTime);
        int timeAllowed = iStep * timePerStep;
        if (timeSpent < timeAllowed) {
          vwr.requestRepaintAndWait("moveThread");
          if (!isJS && !vwr.isScriptExecuting()) {
            mode = FINISH;
            break;
          }
          timeSpent = (int) (System.currentTimeMillis() - startTime);
          sleepTime = timeAllowed - timeSpent;
          if (!runSleep(sleepTime, MAIN))
            return;
        }
        break;
      case FINISH:
        if (floatSecondsTotal > 0)
          vwr.setInMotion(false);
        resumeEval();
        return;
      }
  }

}