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


import org.jmol.util.AxisAngle4f;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.P3;
import org.jmol.util.V3;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public class MoveToThread extends JmolThread {
  /**
   * 
   */
  private final TransformManager transformManager;

  /**
   * @param transformManager
   * @param viewer 
   */
  public MoveToThread(TransformManager transformManager, Viewer viewer) {
    super();
    setViewer(viewer, "MoveToThread");
    this.transformManager = transformManager;
  }

  private final V3 aaStepCenter = new V3();
  private final V3 aaStepNavCenter = new V3();
  private final AxisAngle4f aaStep = new AxisAngle4f();
  private final AxisAngle4f aaTotal = new AxisAngle4f();
  private final Matrix3f matrixStart = new Matrix3f();
  private final Matrix3f matrixStartInv = new Matrix3f();
  private final Matrix3f matrixStep = new Matrix3f();
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
  
  public int set(float floatSecondsTotal, P3 center, Matrix3f end,
                 float zoom, float xTrans, float yTrans,
                 float newRotationRadius, P3 navCenter, float xNav,
                 float yNav, float navDepth, 
                 float cameraDepth, float cameraX, float cameraY) {
    this.center = center;
    this.navCenter = navCenter;
    ptMoveToCenter = (center == null ? transformManager.fixedRotationCenter
        : center);
    this.rotationRadius = newSlider(transformManager.modelRadius, (center == null || Float.isNaN(newRotationRadius) ? transformManager.modelRadius
        : newRotationRadius <= 0 ? viewer.calcRotationRadius(center)
            : newRotationRadius));
    this.pixelScale = newSlider(transformManager.scaleDefaultPixelsPerAngstrom, (center == null ? transformManager.scaleDefaultPixelsPerAngstrom : transformManager
        .defaultScaleToScreen(this.rotationRadius.value)));
    this.zoom = newSlider(transformManager.zoomPercent, zoom);
    this.xTrans = newSlider(transformManager.getTranslationXPercent(), xTrans);
    this.yTrans = newSlider(transformManager.getTranslationYPercent(), yTrans);
    this.xNav = newSlider(transformManager.getNavigationOffsetPercent('X'), xNav);
    this.yNav = newSlider(transformManager.getNavigationOffsetPercent('Y'), yNav);
    this.navDepth = newSlider(transformManager.getNavigationDepthPercent(), navDepth);
    this.cameraDepth = newSlider(transformManager.getCameraDepth(), cameraDepth);
    this.cameraX = newSlider(transformManager.camera.x, cameraX);
    this.cameraY = newSlider(transformManager.camera.y, cameraY);    
    matrixEnd.setM(end);
    transformManager.getRotation(matrixStart);
    matrixStartInv.invertM(matrixStart);
    matrixStep.mul2(matrixEnd, matrixStartInv);
    aaTotal.setM(matrixStep);
    
    fps = 30;
    this.floatSecondsTotal = floatSecondsTotal;
    totalSteps = (int) (floatSecondsTotal * fps);
    if (totalSteps == 0)
      return 0;
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
    return (Float.isNaN(value) ? null : new Slider(start, value));
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
          viewer.requestRepaintAndWait();
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
    transformManager.setRotation(matrixStep);
    float fStep = iStep / (totalSteps - 1f);
    if (center != null)
      transformManager.fixedRotationCenter.add(aaStepCenter);
    if (navCenter != null
        && transformManager.mode == TransformManager.MODE_NAVIGATION) {
      P3 pt = P3.newP(transformManager.navigationCenter);
      pt.add(aaStepNavCenter);
      transformManager.setNavigatePt(pt);
    }
    setValues(fStep);
  }

  private void doFinalTransform() {
    transformManager.setRotation(matrixEnd);
    if (center != null)
      transformManager.moveRotationCenter(center,
          !transformManager.windowCentered);
    if (navCenter != null
        && transformManager.mode == TransformManager.MODE_NAVIGATION)
      transformManager.navigationCenter.setT(navCenter);
    setValues(-1);
  }

  private void setValues(float fStep) {
    transformManager.modelRadius = rotationRadius.getVal(fStep);
    transformManager.scaleDefaultPixelsPerAngstrom = pixelScale.getVal(fStep);
    if (zoom != null)
      transformManager.zoomToPercent(zoom.getVal(fStep));
    if (xTrans != null && yTrans != null) {
      transformManager.translateToPercent('x', xTrans.getVal(fStep));
      transformManager.translateToPercent('y', yTrans.getVal(fStep));
    }
    if (xNav != null && yNav != null)
      transformManager.navTranslatePercentOrTo(0, xNav.getVal(fStep), yNav
          .getVal(fStep));
    if (navDepth != null)
      transformManager.setNavigationDepthPercent(navDepth.getVal(fStep));
    if (cameraDepth != null)
      transformManager.setCameraDepthPercent(cameraDepth.getVal(fStep), false);
    if (cameraX != null && cameraY != null)
      transformManager.setCamera(cameraX.getVal(fStep), cameraY.getVal(fStep));
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