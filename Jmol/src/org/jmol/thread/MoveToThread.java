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



import javajs.vec.AxisAngle4f;
import javajs.vec.Matrix3f;
import javajs.vec.P3;
import javajs.vec.V3;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public class MoveToThread extends JmolThread {
  /**
   * 
   */
  private TransformManager transformManager;

  public MoveToThread() {}
  
  private final V3 aaStepCenter = new V3();
  private final V3 aaStepNavCenter = new V3();
  private final AxisAngle4f aaStep = new AxisAngle4f();
  private final AxisAngle4f aaTotal = new AxisAngle4f();
  private final Matrix3f matrixStart = new Matrix3f();
  private final Matrix3f matrixStartInv = new Matrix3f();
  private Matrix3f matrixStep = new Matrix3f();
  private final Matrix3f matrixEnd = new Matrix3f();

  private P3 center;
  private P3 navCenter;
  private P3 ptMoveToCenter;
  
  private Slider zoom; 
  private Slider xTrans;
  private Slider yTrans;
  private Slider xNav;
  private Slider yNav;
  private Slider navDepth;
  private Slider cameraDepth;
  private Slider cameraX;
  private Slider cameraY;
  private Slider rotationRadius;
  private Slider pixelScale;
  
  private int totalSteps;
  private int fps;
  private long frameTimeMillis;
  private int iStep;  
  private boolean doEndMove;
  private float floatSecondsTotal;
  private float fStep;

  @Override
  public int setManager(Object manager, Viewer viewer, Object params) {
    Object[] options = (Object[]) params;
    //  { center, matrixEnd, navCenter },
    //  { 
    //  0 floatSecondsTotal
    //  1 zoom,
    //  2 xTrans,
    //  3 yTrans,
    //  4 newRotationRadius, 
    //  5 pixelScale, 
    //  6 navDepth,
    //  7 xNav,
    //  8 yNav,
    //  9 cameraDepth,
    //  10 cameraX,
    //  11 cameraY });
    setViewer(viewer, "MoveToThread");
    transformManager = (TransformManager) manager;
    center = (P3) options[0];
    matrixEnd.setM((Matrix3f) options[1]);
    float[] f = (float[]) options[3];
    ptMoveToCenter = (center == null ? transformManager.fixedRotationCenter
        : center);
    floatSecondsTotal = f[0];
    zoom = newSlider(transformManager.zoomPercent, f[1]);
    xTrans = newSlider(transformManager.getTranslationXPercent(), f[2]);
    yTrans = newSlider(transformManager.getTranslationYPercent(), f[3]);
    rotationRadius = newSlider(transformManager.modelRadius, (center == null
        || Float.isNaN(f[4]) ? transformManager.modelRadius
        : f[4] <= 0 ? viewer.calcRotationRadius(center) : f[4]));
    pixelScale = newSlider(transformManager.scaleDefaultPixelsPerAngstrom, f[5]);
    if (f[6] != 0) {
      navCenter = (P3) options[2];
      navDepth = newSlider(transformManager.getNavigationDepthPercent(),
          f[6]);
      xNav = newSlider(transformManager.getNavigationOffsetPercent('X'),
         f[7]);
      yNav = newSlider(transformManager.getNavigationOffsetPercent('Y'),
          f[8]);
    }
    cameraDepth = newSlider(transformManager.getCameraDepth(), f[9]);
    cameraX = newSlider(transformManager.camera.x, f[10]);
    cameraY = newSlider(transformManager.camera.y, f[11]);
    transformManager.getRotation(matrixStart);
    matrixStartInv.invertM(matrixStart);
    matrixStep.mul2(matrixEnd, matrixStartInv);
    aaTotal.setM(matrixStep);
    fps = 30;
    totalSteps = (int) (floatSecondsTotal * fps);
    frameTimeMillis = 1000 / fps;
    targetTime = System.currentTimeMillis();
    aaStepCenter.setT(ptMoveToCenter);
    aaStepCenter.sub(transformManager.fixedRotationCenter);
    aaStepCenter.scale(1f / totalSteps);
    if (navCenter != null
        && transformManager.mode == TransformManager.MODE_NAVIGATION) {
      aaStepNavCenter.setT(navCenter);
      aaStepNavCenter.sub(transformManager.navigationCenter);
      aaStepNavCenter.scale(1f / totalSteps);
    }
    return totalSteps;
  }
         
  private Slider newSlider(float start, float value) {
    return (Float.isNaN(value) || value == Float.MAX_VALUE ? null : new Slider(start, value));
  }

  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        if (totalSteps > 0)
          viewer.setInMotion(true);
        mode = MAIN;
        break;
      case MAIN:
        if (stopped || ++iStep >= totalSteps) {
          mode = FINISH;
          break;
        }
        doStepTransform();
        doEndMove = true;
        targetTime += frameTimeMillis;
        currentTime = System.currentTimeMillis();
        boolean doRender = (currentTime < targetTime);
        if (!doRender && isJS) {
          // JavaScript will be slow anyway -- make sure we render
          targetTime = currentTime;
          doRender = true;
        }
        if (doRender)
          viewer.requestRepaintAndWait("movetoThread");
        if (transformManager.motion == null || !isJS && eval != null
            && !viewer.isScriptExecuting()) {
          stopped = true;
          break;
        }
        currentTime = System.currentTimeMillis();
        int sleepTime = (int) (targetTime - currentTime);
        if (!runSleep(sleepTime, MAIN))
          return;
        mode = MAIN;
        break;
      case FINISH:
        if (totalSteps <= 0 || doEndMove && !stopped)
          doFinalTransform();
        if (totalSteps > 0)
          viewer.setInMotion(false);
        viewer.moveUpdate(floatSecondsTotal);
        if (transformManager.motion != null && !stopped) {
          transformManager.motion = null;
          viewer.finalizeTransformParameters();
        }
        resumeEval();
        return;
      }
  }

  private void doStepTransform() {
    if (!Float.isNaN(matrixEnd.m00)) {
      transformManager.getRotation(matrixStart);
      matrixStartInv.invertM(matrixStart);
      matrixStep.mul2(matrixEnd, matrixStartInv);
      aaTotal.setM(matrixStep);
      aaStep.setAA(aaTotal);
      aaStep.angle /= (totalSteps - iStep);
      if (aaStep.angle == 0)
        matrixStep.setIdentity();
      else
        matrixStep.setAA(aaStep);
      matrixStep.mul(matrixStart);
    }
    fStep = iStep / (totalSteps - 1f);
    if (center != null)
      transformManager.fixedRotationCenter.add(aaStepCenter);
    if (navCenter != null
        && transformManager.mode == TransformManager.MODE_NAVIGATION) {
      P3 pt = P3.newP(transformManager.navigationCenter);
      pt.add(aaStepNavCenter);
      transformManager.setNavigatePt(pt);
    }
    setValues(matrixStep, null, null);
  }

  private void doFinalTransform() {
    fStep = -1;
    setValues(matrixEnd, center, navCenter);
  }

  private void setValues(Matrix3f m, P3 center, P3 navCenter) {
    transformManager.setAll(center, m, navCenter, getVal(zoom), getVal(xTrans), getVal(yTrans), 
        getVal(rotationRadius), getVal(pixelScale), getVal(navDepth), 
        getVal(xNav), getVal(yNav),
        getVal(cameraDepth), getVal(cameraX), getVal(cameraY) );
  }

  private float getVal(Slider s) {
    return (s == null ? Float.NaN : s.getVal(fStep));
  }

  @Override
  public void interrupt() {
    doEndMove = false;
    super.interrupt();
  }
 
  class Slider{
    float start;
    float delta;
    float value;
    
    Slider(float start, float value) {
      this.start = start;
      this.value = value;
      this.delta = value - start;
    }
    
    float getVal(float fStep) {
      return (fStep < 0 ? value : start + fStep * delta);
    }
    
  }

}