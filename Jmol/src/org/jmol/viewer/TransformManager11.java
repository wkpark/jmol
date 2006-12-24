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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import org.jmol.util.Logger;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import org.jmol.g3d.Graphics3D;

class TransformManager11 extends TransformManager {

  private float navigationZOffset;
  private float zoomFactor = Float.MAX_VALUE;

  TransformManager11(Viewer viewer) {
    super(viewer);
  }

  TransformManager11(Viewer viewer, int width, int height) {
    super(viewer, width, height);
  }

  private float prevZoomSetting;
  
  protected void calcCameraFactors() {
    //(m) model coordinates
    //(s) screen coordinates = (m) * screenPixelsPerAngstrom
    //(p) plane coordinates = (s) / screenPixelCount

    // factor to apply as part of the transform (not used here)
    cameraScaleFactor = 1; //unitless

    // distance from the front plane of the model at zoom=100, where p=0 
    cameraDistance = cameraDepth * screenPixelCount; //(s)

    // factor to apply based on screen Z
    perspectiveScale = cameraDistance + screenPixelCount / 2f; //(s)

    // conversion factor Angstroms --> pixels
    scalePixelsPerAngstrom = scaleDefaultPixelsPerAngstrom;

    // vertical screen plane of the observer where objects will be clipped
    // based on the ratio:
    // observerOffset / (visualRange * scalePixelsPerAngstrom)
    //   = perspectiveScale / screenPixelCount 
    observerOffset = (visualRange * scalePixelsPerAngstrom) * perspectiveScale
        / screenPixelCount; //(s)

    float f = zoomPercent / 100;
    if (isNavigationMode) {
      if (zoomFactor == Float.MAX_VALUE) {
        //entry point
        if (zoomPercent > MAXIMUM_ZOOM_PERSPECTIVE_DEPTH)
          zoomPercent = MAXIMUM_ZOOM_PERSPECTIVE_DEPTH;
        navigationZOffset = perspectiveScale * (100 / zoomPercent - 1);
      } else if (prevZoomSetting != zoomPercentSetting) {
        //continuing; zoom changed by user
        navigationZOffset += (1 - prevZoomSetting / zoomPercentSetting) * observerOffset;
      }
      prevZoomSetting = zoomPercentSetting;
      // screen offset to fixed rotation center
      modelCenterOffset = perspectiveScale + navigationZOffset; //(s)
      zoomFactor = modelCenterOffset / perspectiveScale;
      // infinite or negative value means there is no corresponding non-navigating zoom setting
      zoomPercent = (zoomFactor == 0 ? MAXIMUM_ZOOM_PERSPECTIVE_DEPTH
          : 100 / zoomFactor);
    } else {
      modelCenterOffset = perspectiveScale;
      zoomFactor = Float.MAX_VALUE;
      navigationZOffset = 0;
      scalePixelsPerAngstrom *= f; //(s/m)
    }
  }

  protected void calcSlabAndDepthValues() {
    slabValue = 0;
    depthValue = Integer.MAX_VALUE;
    if (slabEnabled) {
      float radius = rotationRadius * scalePixelsPerAngstrom;
      if (perspectiveDepth && visualRange > 0 && slabPercentSetting == 0) {
        slabValue = (int) observerOffset;
        depthValue = Integer.MAX_VALUE;
        if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
          Logger.debug("\n" + "\nperspectiveScale: " + perspectiveScale
              + " screenPixelCount: " + screenPixelCount
              + "\nmodelTrailingEdge: " + (modelCenterOffset + radius)
              + "\nmodelCenterOffset: " + modelCenterOffset + " radius: "
              + radius + "\nmodelLeadingEdge: " + (modelCenterOffset - radius)
              + "\nzoom: " + zoomPercent + " observerOffset/navDepth: "
              + observerOffset + "/"
              + (((int) (100 * getNavigationDepthPercent())) / 100f)
              + " visualRange: " + visualRange + "\nnavX/Y/Z: "
              + navigationOffset.x + "/" + navigationOffset.y + "/"
              + navigationOffset.z + "/" + navigationZOffset + " navCenter:"
              + navigationCenter);
        return;
      }
      // a slab percentage of 100 should map to zero
      // a slab percentage of 0 should map to -diameter
      slabValue = (int) (((50 - slabPercentSetting) * radius / 50) + modelCenterOffset);
      depthValue = (int) (((50 - depthPercentSetting) * radius / 50) + modelCenterOffset);
    }
  }

  protected Point3i adjustedTemporaryScreenPoint() {

    //fixedRotation point is at the origin initially

    float z = point3fScreenTemp.z;

    //this could easily go negative -- behind the screen --
    //but we don't care. In fact, that just makes it easier, 
    //because it means we won't render it.
    //we should probably assign z = 0 as "unrenderable"

    if (Float.isNaN(z)) {
      if (!haveNotifiedNaN)
        Logger.debug("NaN seen in TransformPoint");
      haveNotifiedNaN = true;
      z = 1;
    } else if (z <= 0) {
      //just don't let z go past 1  BH 11/15/06
      z = 1;
    }
    point3fScreenTemp.z = z;

    // x and y are moved inward (generally) relative to 0, which
    // is either the fixed rotation center or the navigation center

    // at this point coordinates are centered on rotation center

    if (perspectiveDepth) {
      if (isNavigationMode) {
        // move nav center to 0; refOffset = Nav - Rot
        point3fScreenTemp.x -= referenceOffset.x;
        point3fScreenTemp.y -= referenceOffset.y;
      }
      // apply perspective factor
      float factor = getPerspectiveFactor(z);
      point3fScreenTemp.x *= factor;
      point3fScreenTemp.y *= factor;
    }

    //now move the center point to where it needs to be
    if (isNavigationMode) {
      point3fScreenTemp.x += navigationOffset.x;
      point3fScreenTemp.y += navigationOffset.y;
    } else {
      point3fScreenTemp.x += fixedRotationOffset.x;
      point3fScreenTemp.y += fixedRotationOffset.y;
    }

    if (Float.isNaN(point3fScreenTemp.x) && !haveNotifiedNaN) {
      Logger.debug("NaN found in transformPoint ");
      haveNotifiedNaN = true;
    }

    point3iScreenTemp.x = (int) point3fScreenTemp.x;
    point3iScreenTemp.y = (int) point3fScreenTemp.y;
    point3iScreenTemp.z = (int) point3fScreenTemp.z;

    return point3iScreenTemp;
  }

  /* ***************************************************************
   * Navigation support
   ****************************************************************/

  boolean canNavigate() {
    return true;
  }

  int nHits;
  int multiplier = 1;

  final static int NAV_MODE_NONE = 0;
  final static int NAV_MODE_RESET = 1;
  final static int NAV_MODE_NEWXY = 2;
  final static int NAV_MODE_NEWXYZ = 3;
  final static int NAV_MODE_NEWZ = 4;

  int navMode = NAV_MODE_RESET;

  protected void resetNavigationPoint() {
    if (isNavigationMode) {
      navMode = NAV_MODE_RESET;
      slabPercentSetting = 0;
    } else {
      slabPercentSetting = 100;
      if (zoomPercent < 0)
        zoomPercent = MAXIMUM_ZOOM_PERSPECTIVE_DEPTH;
    }
    slabEnabled = isNavigationMode;
    zoomFactor = Float.MAX_VALUE;
    zoomPercentSetting = zoomPercent;
  }

  synchronized void navigate(int keyCode, int modifiers) {
    if (!isNavigationMode)
      return;
    if (keyCode == 0) {
      nHits = 0;
      multiplier = 1;
      if (!navigating)
        return;
      navigating = false;
      return;
    }
    nHits++;
    if (nHits % 10 == 0)
      multiplier *= (multiplier == 4 ? 1 : 2);
    boolean isOffsetShifted = ((modifiers & InputEvent.SHIFT_MASK) > 0);
    boolean isAltKey = ((modifiers & InputEvent.ALT_MASK) > 0);
    switch (keyCode) {
    case KeyEvent.VK_UP:
      if (isOffsetShifted) {
        navigationOffset.y -= 2 * multiplier;
        navMode = NAV_MODE_NEWXY;
        break;
      }
      if (isAltKey) {
        rotateXRadians(radiansPerDegree * -.2f * multiplier);
        navMode = NAV_MODE_NEWXYZ;
        break;
      }
      navigationZOffset -= 5 * multiplier;
      navMode = NAV_MODE_NEWZ;
      break;
    case KeyEvent.VK_DOWN:
      if (isOffsetShifted) {
        navigationOffset.y += 2 * multiplier;
        navMode = NAV_MODE_NEWXY;
        break;
      }
      if (isAltKey) {
        rotateXRadians(radiansPerDegree * .2f * multiplier);
        navMode = NAV_MODE_NEWXYZ;
        break;
      }
      navigationZOffset += 5 * multiplier;
      navMode = NAV_MODE_NEWZ;
      break;
    case KeyEvent.VK_LEFT:
      if (isOffsetShifted) {
        navigationOffset.x -= 2 * multiplier;
        navMode = NAV_MODE_NEWXY;
        break;
      }
      rotateYRadians(radiansPerDegree * 3 * -.2f * multiplier);
      navMode = NAV_MODE_NEWXYZ;
      break;
    case KeyEvent.VK_RIGHT:
      if (isOffsetShifted) {
        navigationOffset.x += 2 * multiplier;
        navMode = NAV_MODE_NEWXY;
        break;
      }
      rotateYRadians(radiansPerDegree * 3 * .2f * multiplier);
      navMode = NAV_MODE_NEWXYZ;
      break;
    default:
      navigating = false;
      navMode = NAV_MODE_NONE;
      return;
    }
    navigating = true;
    finalizeTransformParameters();
  }

  void navigate(float seconds, Point3f pt) {
    if (seconds > 0) {
      navigateTo(seconds, null, Float.NaN, pt, Float.NaN, Float.NaN, Float.NaN);
      return;
    }
    navigationCenter.set(pt);
    navMode = NAV_MODE_NEWXYZ;
    navigating = true;
    finalizeTransformParameters();
    navigating = false;
  }

  void navigate(float seconds, Vector3f rotAxis, float degrees) {
    if (seconds > 0) {
      navigateTo(seconds, rotAxis, degrees, null, Float.NaN, Float.NaN,
          Float.NaN);
      return;
    }
    if (rotAxis.x != 0)
      rotateXRadians((float) Math.PI / 180 * degrees);
    if (rotAxis.y != 0)
      rotateYRadians((float) Math.PI / 180 * degrees);
    if (rotAxis.z != 0)
      rotateZRadians((float) Math.PI / 180 * degrees);
    navMode = NAV_MODE_NEWXYZ;
    navigating = true;
    finalizeTransformParameters();
    navigating = false;
  }

  void setNavigationDepthPercent(float timeSec, float percent) {
    // navigation depth 0 # place user at front plane of the model
    // navigation depth 100 # place user at rear plane of the model

    // scalePixelsPerAngstrom takes into account any zoom

    // perspectiveScale +  navigationZOffset = observerOffset + dz

    if (timeSec > 0) {
      navigateTo(timeSec, null, Float.NaN, null, percent, Float.NaN, Float.NaN);
      return;
    }

    calcCameraFactors(); //current
    float radius = rotationRadius * scalePixelsPerAngstrom;
    float dz = ((50 - percent) * radius / 50);
    navigationZOffset = observerOffset - dz - perspectiveScale;
    calcCameraFactors(); //updated
  }

  void navTranslate(float seconds, Point3f pt) {
    transformPoint(pt, pointT);
    if (seconds > 0) {
      navigateTo(seconds, null, Float.NaN, null, Float.NaN, pointT.x, pointT.y);
      return;
    }
    navTranslatePercent(-1, pointT.x, pointT.y);
  }

  void navTranslatePercent(float seconds, float x, float y) {
    // if either is Float.NaN, then the other is RELATIVE to current
    transformPoint(navigationCenter, navigationOffset);
    if (seconds >= 0) {
      if (!Float.isNaN(x))
        x = width * x / 100
            + (Float.isNaN(y) ? navigationOffset.x : (width / 2));
      if (!Float.isNaN(y))
        y = height * y / 100
            + (Float.isNaN(x) ? navigationOffset.y : (height / 2));
    }
    if (seconds > 0) {
      navigateTo(seconds, null, Float.NaN, null, Float.NaN, x, y);
      return;
    }
    if (!Float.isNaN(x))
      navigationOffset.x = x;
    if (!Float.isNaN(y))
      navigationOffset.y = y;
    navMode = NAV_MODE_NEWXY;
    navigating = true;
    finalizeTransformParameters();
    navigating = false;
  }

  /**
   * All the magic happens here.
   *
   */
  protected void calcNavigationPoint() {
    if (!navigating && navMode != NAV_MODE_RESET)
      navMode = NAV_MODE_NONE;
    switch (navMode) {
    case NAV_MODE_RESET:
      //simply place the navigation center front and center
      navigationOffset.set(width / 2, height / 2, observerOffset);//navigationOffset.z = observerOffset;
      findCenterAt(fixedRotationCenter, navigationOffset, navigationCenter);
      break;
    case NAV_MODE_NONE:
      //update fixed rotation offset and find the new 3D navigation center
      fixedRotationOffset.set(fixedTranslation);
      //fall through
    case NAV_MODE_NEWXY:
      // redefine the navigation center based on its old screen position
      findCenterAt(fixedRotationCenter, navigationOffset, navigationCenter);
      break;
    case NAV_MODE_NEWXYZ:
      // must just be (not so!) simple navigation
      // navigation center will initially move
      // but we center it by moving the rotation center instead
      matrixTransform(navigationCenter, pointT);
      float z = pointT.z;
      matrixTransform(fixedRotationCenter, pointT);
      navigationZOffset = observerOffset + (pointT.z - z) - perspectiveScale;
      calcCameraFactors();
      calcTransformMatrix();
      break;
    case NAV_MODE_NEWZ:
      //just untransform the offset to get the new 3D navigation center
      navigationOffset.z = observerOffset;
      unTransformPoint(navigationOffset, navigationCenter);
      break;
    }
    matrixTransform(navigationCenter, referenceOffset);
    transformPoint(fixedRotationCenter, fixedTranslation);
    fixedRotationOffset.set(fixedTranslation);
    transformPoint(navigationCenter, navigationOffset);
    navigationOffset.z = observerOffset;
    navMode = NAV_MODE_NONE;
  }

  /**
   * We do not want the fixed navigation offset to change,
   * but we need a new model-based equivalent position.
   * The fixed rotation center is at a fixed offset as well.
   * This means that the navigationCenter must be recalculated
   * based on its former offset in the new context. We have two points, 
   * N(navigation) and R(rotation). We know where they ARE: 
   * fixedNavigationOffset and fixedRotationOffset.
   * From these we must derive navigationCenter.

   * @param fixedScreenXYZ 
   * @param fixedModelXYZ
   * @param center
   */
  private void findCenterAt(Point3f fixedModelXYZ, Point3f fixedScreenXYZ,
                            Point3f center) {
    isNavigationMode = false;
    //get the rotation center's Z offset and move X and Y to 0,0
    transformPoint(fixedModelXYZ, pointT);
    pointT.x -= fixedScreenXYZ.x;
    pointT.y -= fixedScreenXYZ.y;
    //unapply the perspective as if IT were the navigation center
    float f = -getPerspectiveFactor(pointT.z);
    pointT.x /= f;
    pointT.y /= f;
    pointT.z = fixedScreenXYZ.z;
    //now untransform that point to give the center that would
    //deliver this fixedModel position
    matrixUnTransform(pointT, center);
    isNavigationMode = true;
  }

  private void navigateTo(float floatSecondsTotal, Vector3f axis,
                          float degrees, Point3f center, float depthPercent,
                          float xTrans, float yTrans) {

    ptMoveToCenter = (center == null ? navigationCenter : center);
    int fps = 30;
    int totalSteps = (int) (floatSecondsTotal * fps);
    viewer.setInMotion(true);
    if (degrees == 0)
      degrees = Float.NaN;
    if (totalSteps > 1) {
      int frameTimeMillis = 1000 / fps;
      long targetTime = System.currentTimeMillis();
      float depthStart = getNavigationDepthPercent();
      float depthDelta = depthPercent - depthStart;
      float xTransStart = navigationOffset.x;
      float xTransDelta = xTrans - xTransStart;
      float yTransStart = navigationOffset.y;
      float yTransDelta = yTrans - yTransStart;
      float degreeStep = degrees / totalSteps;
      aaStepCenter.set(ptMoveToCenter);
      aaStepCenter.sub(navigationCenter);
      aaStepCenter.scale(1f / totalSteps);
      Point3f centerStart = new Point3f(navigationCenter);
      for (int iStep = 1; iStep < totalSteps; ++iStep) {

        navigating = true;
        float fStep = iStep / (totalSteps - 1f);
        if (!Float.isNaN(degrees))
          navigate(0, axis, degreeStep);
        if (center != null) {
          centerStart.add(aaStepCenter);
          navigate(0, centerStart);
        }
        if (!Float.isNaN(xTrans) || !Float.isNaN(yTrans)) {
          float x = Float.NaN;
          float y = Float.NaN;
          if (!Float.isNaN(xTrans))
            x = xTransStart + xTransDelta * fStep;
          if (!Float.isNaN(yTrans))
            y = yTransStart + yTransDelta * fStep;
          navTranslatePercent(-1, x, y);
        }

        if (!Float.isNaN(depthPercent)) {
          setNavigationDepthPercent(0, depthStart + depthDelta * fStep);
        }
        navigating = false;
        targetTime += frameTimeMillis;
        if (System.currentTimeMillis() < targetTime) {
          viewer.requestRepaintAndWait();
          if (!viewer.isScriptExecuting())
            break;
          int sleepTime = (int) (targetTime - System.currentTimeMillis());
          if (sleepTime > 0) {
            try {
              Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
            }
          }
        }
      }
    } else {
      int sleepTime = (int) (floatSecondsTotal * 1000) - 30;
      if (sleepTime > 0) {
        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {
        }
      }
    }
    //if (center != null)
    //navigate(0, center);
    if (!Float.isNaN(xTrans) || !Float.isNaN(yTrans))
      navTranslatePercent(-1, xTrans, yTrans);
    if (!Float.isNaN(depthPercent))
      setNavigationDepthPercent(0, depthPercent);
    viewer.setInMotion(false);
  }

  void navigate(float seconds, Point3f[] path, float[] theta, int indexStart,
                int indexEnd) {
    if (seconds <= 0) // PER station
      seconds = 2;
    int nSegments = Math.min(path.length - 1, indexEnd);
    while (nSegments > 0 && path[nSegments] == null)
      nSegments--;
    nSegments -= indexStart;
    if (nSegments < 1)
      return;
    int nPer = (int) (10 * seconds); //?
    int nSteps = nSegments * nPer + 1;
    Point3f[] points = new Point3f[nSteps + 2];
    int iPrev, iNext, iNext2, iNext3, pt;
    for (int i = 0; i < nSegments; i++) {
      iPrev = Math.max(i - 1, 0) + indexStart;
      pt = i + indexStart;
      iNext = Math.min(i + 1, nSegments) + indexStart;
      iNext2 = Math.min(i + 2, nSegments) + indexStart;
      iNext3 = Math.min(i + 3, nSegments) + indexStart;
      Graphics3D.getHermiteList(7, path[iPrev], path[pt], path[iNext],
          path[iNext2], path[iNext3], points, i * nPer, nPer + 1);
    }
    int fps = 10;
    int totalSteps = nSteps;
    viewer.setInMotion(true);
    int frameTimeMillis = 1000 / fps;
    long targetTime = System.currentTimeMillis();
    for (int iStep = 0; iStep < totalSteps; ++iStep) {
      navigating = true;
      navigate(0, points[iStep]);
      navigating = false;
      targetTime += frameTimeMillis;
      if (System.currentTimeMillis() < targetTime) {
        viewer.requestRepaintAndWait();
        if (!viewer.isScriptExecuting())
          break;
        int sleepTime = (int) (targetTime - System.currentTimeMillis());
        if (sleepTime > 0) {
          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {
          }
        }
      }
    }
  }

  void setNavigationCenter(Point3f center) {
    navigate(0, center);
  }

  Point3f getNavigationCenter() {
    return navigationCenter;
  }

  Point3f getNavigationOffset() {
    transformPoint(navigationCenter, navigationOffset);
    return navigationOffset;
  }

  float getNavigationDepthPercent() {
    calcCameraFactors(); //current
    float radius = rotationRadius * scalePixelsPerAngstrom;
    float dz = navigationZOffset - observerOffset + perspectiveScale;
    return 50 + dz * 50 / radius;
  }

  float getNavigationOffsetPercent(char XorY) {
    transformPoint(navigationCenter, navigationOffset);
    return (XorY == 'X' ? (navigationOffset.x - width / 2) * 100 / width
        : (navigationOffset.y - height / 2) * 100 / height);
  }

  protected String getNavigationText() {
    transformPoint(navigationCenter, navigationOffset);
    return " /* navigation center, translation, depth */ "
        + StateManager.escape(navigationCenter) + " "
        + getNavigationOffsetPercent('X') + " "
        + getNavigationOffsetPercent('Y') + " " + getNavigationDepthPercent();
  }

  protected String getNavigationState() {
    if (!isNavigationMode)
      return "";
    return "# navigation state;\nnavigate 0 center "
        + StateManager.escape(getNavigationCenter())
        + ";\nnavigate 0 translate " + getNavigationOffsetPercent('X') + " "
        + getNavigationOffsetPercent('Y') + ";\nnavigate 0 depth "
        + getNavigationDepthPercent() + ";\n\n";
  }

}
