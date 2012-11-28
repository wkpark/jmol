/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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

package org.jmol.io2;

import org.jmol.api.Event;
import org.jmol.api.JmolNavigatorInterface;
import org.jmol.util.Escape;
import org.jmol.util.Hermite;
import org.jmol.util.Matrix3f;
import org.jmol.util.Point3f;
import org.jmol.util.Vector3f;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.TransformManager11;
import org.jmol.viewer.Viewer;


/**
 * Navigator is a user input mechanism that utilizes the keypad
 * to drive through the model. 
 * 
 * 
 */
public final class Navigator implements JmolNavigatorInterface {

  public Navigator() {
    // for Reflection from TransformManage11.java
  }
  
  private TransformManager11 tm;
  private Viewer viewer;

  private int nHits;
  private int multiplier = 1;


  public void navigateTo(float floatSecondsTotal, Vector3f axis, float degrees,
                         Point3f center, float depthPercent, float xTrans,
                         float yTrans) {
    /*
     * Orientation o = viewer.getOrientation(); if (!Float.isNaN(degrees) &&
     * degrees != 0) navigate(0, axis, degrees); if (center != null) {
     * navigate(0, center); } if (!Float.isNaN(xTrans) || !Float.isNaN(yTrans))
     * navTranslatePercent(-1, xTrans, yTrans); if (!Float.isNaN(depthPercent))
     * setNavigationDepthPercent(depthPercent); Orientation o1 =
     * viewer.getOrientation(); o.restore(0, true);
     * o1.restore(floatSecondsTotal, true);
     */
    if (!viewer.haveDisplay)
      floatSecondsTotal = 0;
    Point3f ptMoveToCenter = (center == null ? tm.navigationCenter : center);
    int fps = 30;
    int totalSteps = (int) (floatSecondsTotal * fps);
    if (floatSecondsTotal > 0)
      viewer.setInMotion(true);
    if (degrees == 0)
      degrees = Float.NaN;
    if (totalSteps > 1) {
      int frameTimeMillis = 1000 / fps;
      long targetTime = System.currentTimeMillis();
      float depthStart = tm.getNavigationDepthPercent();
      float depthDelta = depthPercent - depthStart;
      float xTransStart = tm.navigationOffset.x;
      float xTransDelta = xTrans - xTransStart;
      float yTransStart = tm.navigationOffset.y;
      float yTransDelta = yTrans - yTransStart;
      float degreeStep = degrees / totalSteps;
      Vector3f aaStepCenter = new Vector3f();
      aaStepCenter.setT(ptMoveToCenter);
      aaStepCenter.sub(tm.navigationCenter);
      aaStepCenter.scale(1f / totalSteps);
      Point3f centerStart = Point3f.newP(tm.navigationCenter);
      for (int iStep = 1; iStep < totalSteps; ++iStep) {

        tm.navigating = true;
        float fStep = iStep / (totalSteps - 1f);
        if (!Float.isNaN(degrees))
          tm.navigateAxis(0, axis, degreeStep);
        if (center != null) {
          centerStart.add(aaStepCenter);
          tm.navigatePt(0, centerStart);
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
          setNavigationDepthPercent(depthStart + depthDelta * fStep);
        }
        tm.navigating = false;
        targetTime += frameTimeMillis;
        if (System.currentTimeMillis() < targetTime) {
          viewer.requestRepaintAndWait();
          if (!viewer.isScriptExecuting())
            return;
          int sleepTime = (int) (targetTime - System.currentTimeMillis());
          if (sleepTime > 0) {
            try {
              Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
              return;
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
    // if (center != null)
    // navigate(0, center);
    if (!Float.isNaN(xTrans) || !Float.isNaN(yTrans))
      navTranslatePercent(-1, xTrans, yTrans);
    if (!Float.isNaN(depthPercent))
      setNavigationDepthPercent(depthPercent);
    viewer.setInMotion(false);
  }

  public void set(TransformManager11 tm, Viewer viewer) {
    this.tm = tm;
    this.viewer = viewer;
  }

  public void navigate(float seconds, Point3f[][] pathGuide, Point3f[] path,
                       float[] theta, int indexStart, int indexEnd) {
    if (seconds <= 0) // PER station
      seconds = 2;
    if (!viewer.haveDisplay)
      seconds = 0;
    boolean isPathGuide = (pathGuide != null);
    int nSegments = Math.min(
        (isPathGuide ? pathGuide.length : path.length) - 1, indexEnd);
    if (!isPathGuide)
      while (nSegments > 0 && path[nSegments] == null)
        nSegments--;
    nSegments -= indexStart;
    if (nSegments < 1)
      return;
    int nPer = (int) Math.floor(10 * seconds); // ?
    int nSteps = nSegments * nPer + 1;
    Point3f[] points = new Point3f[nSteps + 2];
    Point3f[] pointGuides = new Point3f[isPathGuide ? nSteps + 2 : 0];
    int iPrev, iNext, iNext2, iNext3, pt;
    for (int i = 0; i < nSegments; i++) {
      iPrev = Math.max(i - 1, 0) + indexStart;
      pt = i + indexStart;
      iNext = Math.min(i + 1, nSegments) + indexStart;
      iNext2 = Math.min(i + 2, nSegments) + indexStart;
      iNext3 = Math.min(i + 3, nSegments) + indexStart;
      if (isPathGuide) {
        Hermite.getHermiteList(7, pathGuide[iPrev][0], pathGuide[pt][0],
            pathGuide[iNext][0], pathGuide[iNext2][0], pathGuide[iNext3][0],
            points, i * nPer, nPer + 1, true);
        Hermite.getHermiteList(7, pathGuide[iPrev][1], pathGuide[pt][1],
            pathGuide[iNext][1], pathGuide[iNext2][1], pathGuide[iNext3][1],
            pointGuides, i * nPer, nPer + 1, true);
      } else {
        Hermite.getHermiteList(7, path[iPrev], path[pt], path[iNext],
            path[iNext2], path[iNext3], points, i * nPer, nPer + 1, true);
      }
    }
    int totalSteps = nSteps;
    viewer.setInMotion(true);
    int frameTimeMillis = (int) (1000 / tm.navFps);
    long targetTime = System.currentTimeMillis();
    for (int iStep = 0; iStep < totalSteps; ++iStep) {
      tm.navigatePt(0, points[iStep]);
      if (isPathGuide) {
        alignZX(points[iStep], points[iStep + 1], pointGuides[iStep]);
      }
      targetTime += frameTimeMillis;
      if (System.currentTimeMillis() < targetTime) {
        viewer.requestRepaintAndWait();
        if (!viewer.isScriptExecuting())
          return;
        int sleepTime = (int) (targetTime - System.currentTimeMillis());
        if (sleepTime > 0) {
          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {
            return;
          }
        }
      }
    }
  }
  
  /**
   * brings pt0-pt1 vector to [0 0 -1], then rotates about [0 0 1] until
   * ptVectorWing is in xz plane
   * 
   * @param pt0
   * @param pt1
   * @param ptVectorWing
   */
  private void alignZX(Point3f pt0, Point3f pt1, Point3f ptVectorWing) {
    Point3f pt0s = new Point3f();
    Point3f pt1s = new Point3f();
    Matrix3f m = tm.getMatrixRotate();
    m.transform2(pt0, pt0s);
    m.transform2(pt1, pt1s);
    Vector3f vPath = Vector3f.newV(pt0s);
    vPath.sub(pt1s);
    Vector3f v = Vector3f.new3(0, 0, 1);
    float angle = vPath.angle(v);
    v.cross(vPath, v);
    if (angle != 0)
      tm.navigateAxis(0, v, (float) (angle * TransformManager.degreesPerRadian));
    m.transform2(pt0, pt0s);
    Point3f pt2 = Point3f.newP(ptVectorWing);
    pt2.add(pt0);
    Point3f pt2s = new Point3f();
    m.transform2(pt2, pt2s);
    vPath.setT(pt2s);
    vPath.sub(pt0s);
    vPath.z = 0; // just use projection
    v.set(-1, 0, 0); // puts alpha helix sidechain above
    angle = vPath.angle(v);
    if (vPath.y < 0)
      angle = -angle;
    v.set(0, 0, 1);
    if (angle != 0)
      tm.navigateAxis(0, v, (float) (angle * TransformManager.degreesPerRadian));
    if (viewer.getNavigateSurface()) {
      // set downward viewpoint 20 degrees to horizon
      v.set(1, 0, 0);
      tm.navigateAxis(0, v, 20);
    }
    m.transform2(pt0, pt0s);
    m.transform2(pt1, pt1s);
    m.transform2(ptVectorWing, pt2s);
  }

  public void zoomByFactor(float factor, int x, int y) {
    float navZ = tm.navZ;
    if (navZ > 0) {
      navZ /= factor;
      if (navZ < 5)
        navZ = -5;
      else if (navZ > 200)
        navZ = 200;
    } else if (navZ == 0) {
      navZ = (factor < 1 ? 5 : -5);
    } else {
      navZ *= factor;
      if (navZ > -5)
        navZ = 5;
      else if (navZ < -200)
        navZ = -200;
    }
    tm.navZ = navZ;
      
/*    float range = visualRange / factor;
    System.out.println(navZ);
    
    if (viewer.getNavigationPeriodic())
      range = Math.min(range, 0.8f * modelRadius);      
    visualRange = range;  
*/    
  }

  public void calcNavigationPoint() {
    // called by finalize
    calcNavigationDepthPercent();
    if (!tm.navigating && tm.navMode != TransformManager11.NAV_MODE_RESET) {
      // rotations are different from zoom changes
      if (tm.navigationDepth < 100 && tm.navigationDepth > 0
          && !Float.isNaN(tm.previousX) && tm.previousX == tm.fixedTranslation.x
          && tm.previousY == tm.fixedTranslation.y && tm.navMode != TransformManager11.NAV_MODE_ZOOMED)
        tm.navMode = TransformManager11.NAV_MODE_NEWXYZ;
      else
        tm.navMode = TransformManager11.NAV_MODE_NONE;
    }
    switch (tm.navMode) {
    case TransformManager11.NAV_MODE_RESET:
      // simply place the navigation center front and center and recalculate
      // modelCenterOffset
      tm.navigationOffset.set(tm.width / 2f, tm.getNavPtHeight(), tm.referencePlaneOffset);
      tm.zoomFactor = Float.MAX_VALUE;
      tm.calcCameraFactors();
      tm.calcTransformMatrix();
      newNavigationCenter();
      break;
    case TransformManager11.NAV_MODE_NONE:
    case TransformManager11.NAV_MODE_ZOOMED:
      // update fixed rotation offset and find the new 3D navigation center
      tm.fixedRotationOffset.setT(tm.fixedTranslation);
      newNavigationCenter();
      break;
    case TransformManager11.NAV_MODE_NEWXY:
      // redefine the navigation center based on its old screen position
      newNavigationCenter();
      break;
    case TransformManager11.NAV_MODE_IGNORE:
    case TransformManager11.NAV_MODE_NEWXYZ:
      // must just be (not so!) simple navigation
      // navigation center will initially move
      // but we center it by moving the rotation center instead
      Point3f pt1 = new Point3f();
      tm.matrixTransform.transform2(tm.navigationCenter, pt1);
      float z = pt1.z;
      tm.matrixTransform.transform2(tm.fixedRotationCenter, pt1);
      tm.modelCenterOffset = tm.referencePlaneOffset + (pt1.z - z);
      tm.calcCameraFactors();
      tm.calcTransformMatrix();
      break;
    case TransformManager11.NAV_MODE_NEWZ:
      // just untransform the offset to get the new 3D navigation center
      tm.navigationOffset.z = tm.referencePlaneOffset;
      // System.out.println("nav_mode_newz " + navigationOffset);
      tm.unTransformPoint(tm.navigationOffset, tm.navigationCenter);
      break;
    }
    tm.matrixTransform.transform2(tm.navigationCenter, tm.navigationShiftXY);
    if (viewer.getNavigationPeriodic()) {
      // TODO
      // but if periodic, then the navigationCenter may have to be moved back a
      // notch
      Point3f pt = Point3f.newP(tm.navigationCenter);
      viewer.toUnitCell(tm.navigationCenter, null);
      // presuming here that pointT is still a molecular point??
      if (pt.distance(tm.navigationCenter) > 0.01) {
        tm.matrixTransform.transform2(tm.navigationCenter, pt);
        float dz = tm.navigationShiftXY.z - pt.z;
        // the new navigation center determines the navigationZOffset
        tm.modelCenterOffset += dz;
        tm.calcCameraFactors();
        tm.calcTransformMatrix();
        tm.matrixTransform.transform2(tm.navigationCenter, tm.navigationShiftXY);
      }
    }
    tm.transformPoint2(tm.fixedRotationCenter, tm.fixedTranslation);
    tm.fixedRotationOffset.setT(tm.fixedTranslation);
    tm.previousX = tm.fixedTranslation.x;
    tm.previousY = tm.fixedTranslation.y;
    tm.transformPoint2(tm.navigationCenter, tm.navigationOffset);
    tm.navigationOffset.z = tm.referencePlaneOffset;
    tm.navMode = TransformManager11.NAV_MODE_NONE;
    calcNavSlabAndDepthValues();
  }

  private void calcNavSlabAndDepthValues() {
    tm.calcSlabAndDepthValues();
    if (tm.slabEnabled) {
      tm.slabValue = (tm.mode == TransformManager.MODE_NAVIGATION ? -100 : 0)
          + (int) (tm.referencePlaneOffset - tm.navigationSlabOffset);
      if (tm.zSlabPercentSetting == tm.zDepthPercentSetting)
        tm.zSlabValue = tm.slabValue;
    }

//    if (Logger.debugging)
//      Logger.debug("\n" + "\nperspectiveScale: " + referencePlaneOffset
//          + " screenPixelCount: " + screenPixelCount + "\nmodelTrailingEdge: "
//          + (modelCenterOffset + modelRadiusPixels) + " depthValue: "
//          + depthValue + "\nmodelCenterOffset: " + modelCenterOffset
//          + " modelRadiusPixels: " + modelRadiusPixels + "\nmodelLeadingEdge: "
//          + (modelCenterOffset - modelRadiusPixels) + " slabValue: "
//          + slabValue + "\nzoom: " + zoomPercent + " navDepth: "
//          + ((int) (100 * getNavigationDepthPercent()) / 100f)
//          + " visualRange: " + visualRange + "\nnavX/Y/Z/modelCenterOffset: "
//          + navigationOffset.x + "/" + navigationOffset.y + "/"
//          + navigationOffset.z + "/" + modelCenterOffset + " navCenter:"
//          + navigationCenter);
  }

  /**
   * We do not want the fixed navigation offset to change, but we need a new
   * model-based equivalent position. The fixed rotation center is at a fixed
   * offset as well. This means that the navigationCenter must be recalculated
   * based on its former offset in the new context. We have two points,
   * N(navigation) and R(rotation). We know where they ARE:
   * fixedNavigationOffset and fixedRotationOffset. From these we must derive
   * navigationCenter.
   */
  private void newNavigationCenter() {

    // Point3f fixedRotationCenter, Point3f navigationOffset,

    // Point3f navigationCenter) {

    // fixedRotationCenter, navigationOffset, navigationCenter
    tm.mode = tm.defaultMode;
    // get the rotation center's Z offset and move X and Y to 0,0
    Point3f pt = new Point3f();
    tm.transformPoint2(tm.fixedRotationCenter, pt);
    pt.x -= tm.navigationOffset.x;
    pt.y -= tm.navigationOffset.y;
    // unapply the perspective as if IT were the navigation center
    float f = -tm.getPerspectiveFactor(pt.z);
    pt.x /= f;
    pt.y /= f;
    pt.z = tm.referencePlaneOffset;
    // now untransform that point to give the center that would
    // deliver this fixedModel position
    tm.matrixTransformInv.transform2(pt, tm.navigationCenter);
    tm.mode = TransformManager.MODE_NAVIGATION;
  }

  public void setNavigationOffsetRelative(boolean navigatingSurface) {
    if (navigatingSurface) {
      navigateSurface(Integer.MAX_VALUE);
      return;
    }
    if (tm.navigationDepth < 0 && tm.navZ > 0 || tm.navigationDepth > 100 && tm.navZ < 0) {
      tm.navZ = 0;
    }
    tm.rotateXRadians(JmolConstants.radiansPerDegree * -.02f * tm.navY, null);
    tm.rotateYRadians(JmolConstants.radiansPerDegree * .02f * tm.navX, null);
    Point3f pt = tm.getNavigationCenter();
    Point3f pts = new Point3f();
    tm.transformPoint2(pt, pts);
    pts.z += tm.navZ;
    tm.unTransformPoint(pts, pt);
    tm.navigatePt(0, pt);
  }

  public void navigate(int keyCode, int modifiers) {
    // 0 0 here means "key released"
    String key = null;
    float value = 0;
    if (tm.mode != TransformManager.MODE_NAVIGATION)
      return;
    if (keyCode == 0) {
      nHits = 0;
      multiplier = 1;
      if (!tm.navigating)
        return;
      tm.navigating = false;
      return;
    }
    nHits++;
    if (nHits % 10 == 0)
      multiplier *= (multiplier == 4 ? 1 : 2);
    boolean navigateSurface = viewer.getNavigateSurface();
    boolean isShiftKey = ((modifiers & Event.SHIFT_MASK) > 0);
    boolean isAltKey = ((modifiers & Event.ALT_MASK) > 0);
    boolean isCtrlKey = ((modifiers & Event.CTRL_MASK) > 0);
    float speed = viewer.getNavigationSpeed() * (isCtrlKey ? 10 : 1);
    // race condition viewer.cancelRendering();
    switch (keyCode) {
    case Event.VK_PERIOD:
      tm.navX = tm.navY = tm.navZ = 0;
      tm.homePosition(true);
      return;
    case Event.VK_SPACE:
      if (!tm.navOn)
        return;
      tm.navX = tm.navY = tm.navZ = 0;
      return;
    case Event.VK_UP:
      if (tm.navOn) {
        if (isAltKey) {
          tm.navY += multiplier;
          value = tm.navY;
          key = "navY";
        } else {
          tm.navZ += multiplier;
          value = tm.navZ;
          key = "navZ";
        }
        break;
      }
      if (navigateSurface) {
        navigateSurface(Integer.MAX_VALUE);
        break;
      }
      if (isShiftKey) {
        tm.navigationOffset.y -= 2 * multiplier;
        tm.navMode = TransformManager11.NAV_MODE_NEWXY;
        break;
      }
      if (isAltKey) {
        tm.rotateXRadians(JmolConstants.radiansPerDegree * -.2f * multiplier, null);
        tm.navMode = TransformManager11.NAV_MODE_NEWXYZ;
        break;
      }
      tm.modelCenterOffset -= speed
          * (viewer.getNavigationPeriodic() ? 1 : multiplier);
      tm.navMode = TransformManager11.NAV_MODE_NEWZ;
      break;
    case Event.VK_DOWN:
      if (tm.navOn) {
        if (isAltKey) {
          tm.navY -= multiplier;
          value = tm.navY;
          key = "navY";
        } else {
          tm.navZ -= multiplier;
          value = tm.navZ;
          key = "navZ";
        }
        break;
      }
      if (navigateSurface) {
        navigateSurface(-2 * multiplier);
        break;
      }
      if (isShiftKey) {
        tm.navigationOffset.y += 2 * multiplier;
        tm.navMode = TransformManager11.NAV_MODE_NEWXY;
        break;
      }
      if (isAltKey) {
        tm.rotateXRadians(JmolConstants.radiansPerDegree * .2f * multiplier, null);
        tm.navMode = TransformManager11.NAV_MODE_NEWXYZ;
        break;
      }
      tm.modelCenterOffset += speed
          * (viewer.getNavigationPeriodic() ? 1 : multiplier);
      tm.navMode = TransformManager11.NAV_MODE_NEWZ;
      break;
    case Event.VK_LEFT:
      if (tm.navOn) {
        tm.navX -= multiplier;
        value = tm.navX;
        key = "navX";
        break;
      }
      if (navigateSurface) {
        break;
      }
      if (isShiftKey) {
        tm.navigationOffset.x -= 2 * multiplier;
        tm.navMode = TransformManager11.NAV_MODE_NEWXY;
        break;
      }
      tm.rotateYRadians(JmolConstants.radiansPerDegree * 3 * -.2f * multiplier,
          null);
      tm.navMode = TransformManager11.NAV_MODE_NEWXYZ;
      break;
    case Event.VK_RIGHT:
      if (tm.navOn) {
        tm.navX += multiplier;
        value = tm.navX;
        key = "navX";
        break;
      }
      if (navigateSurface) {
        break;
      }
      if (isShiftKey) {
        tm.navigationOffset.x += 2 * multiplier;
        tm.navMode = TransformManager11.NAV_MODE_NEWXY;
        break;
      }
      tm.rotateYRadians(JmolConstants.radiansPerDegree * 3 * .2f * multiplier,
          null);
      tm.navMode = TransformManager11.NAV_MODE_NEWXYZ;
      break;
    default:
      tm.navigating = false;
      tm.navMode = TransformManager11.NAV_MODE_NONE;
      return;
    }
    if (key != null)
      viewer.getGlobalSettings().setParamF(key, value);
    tm.navigating = true;
    tm.finalizeTransformParameters();
  }

  private void navigateSurface(int dz) {
    if (viewer.isRepaintPending())
      return;
    viewer.setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "navigate",
        Integer.valueOf(dz == Integer.MAX_VALUE ? 2 * multiplier : dz));
    viewer.requestRepaintAndWait();
  }

  private void setNavigationDepthPercent(float percent) {
    // navigation depth 0 # place user at rear plane of the model
    // navigation depth 100 # place user at front plane of the model

    viewer.getGlobalSettings().setParamF("navigationDepth", percent);
    tm.calcCameraFactors(); // current
    tm.modelCenterOffset = tm.referencePlaneOffset - (1 - percent / 50)
        * tm.modelRadiusPixels;
    tm.calcCameraFactors(); // updated
    tm.navMode = TransformManager11.NAV_MODE_ZOOMED;
  }

  private void calcNavigationDepthPercent() {
    tm.calcCameraFactors(); // current
    tm.navigationDepth = (tm.modelRadiusPixels == 0 ? 50
        : 50 * (1 + (tm.modelCenterOffset - tm.referencePlaneOffset)
            / tm.modelRadiusPixels));
  }

  public void setNavigationDepthPercent(float timeSec, float percent) {
    // from MoveToThread
    if (timeSec > 0) {
      navigateTo(timeSec, null, Float.NaN, null, percent, Float.NaN, Float.NaN);
      return;
    }
    setNavigationDepthPercent(percent);
  }

  public void navTranslatePercent(float seconds, float x, float y) {
    // from MoveToThread and Viewer
    // if either is Float.NaN, then the other is RELATIVE to current
    tm.transformPoint2(tm.navigationCenter, tm.navigationOffset);
    if (seconds >= 0) {
      if (!Float.isNaN(x))
        x = tm.width * x / 100f
            + (Float.isNaN(y) ? tm.navigationOffset.x : (tm.width / 2f));
      if (!Float.isNaN(y))
        y = tm.height * y / 100f
            + (Float.isNaN(x) ? tm.navigationOffset.y : tm.getNavPtHeight());
    }
    if (seconds > 0) {
      navigateTo(seconds, null, Float.NaN, null, Float.NaN, x, y);
      return;
    }
    if (!Float.isNaN(x))
      tm.navigationOffset.x = x;
    if (!Float.isNaN(y))
      tm.navigationOffset.y = y;
    tm.navMode = TransformManager11.NAV_MODE_NEWXY;
    tm.navigating = true;
    tm.finalizeTransformParameters();
    tm.navigating = false;
  }

  public String getNavigationState() {
    return "# navigation state;\nnavigate 0 center "
    + Escape.escapePt(tm.getNavigationCenter()) + ";\nnavigate 0 translate "
    + tm.getNavigationOffsetPercent('X') + " "
    + tm.getNavigationOffsetPercent('Y') + ";\nset navigationDepth "
    + tm.getNavigationDepthPercent() + ";\nset navigationSlab "
    + getNavigationSlabOffsetPercent() + ";\n\n";  }


  private float getNavigationSlabOffsetPercent() {
    tm.calcCameraFactors(); // current
    return 50 * tm.navigationSlabOffset / tm.modelRadiusPixels;
  }

  
}
