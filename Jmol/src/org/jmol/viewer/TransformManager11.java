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

import org.jmol.api.Interface;
import org.jmol.api.JmolNavigatorInterface;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Point3f;
import org.jmol.util.Vector3f;



public class TransformManager11 extends TransformManager {

  final public static int NAV_MODE_IGNORE = -2;
  final public static int NAV_MODE_ZOOMED = -1;
  final public static int NAV_MODE_NONE = 0;
  final public static int NAV_MODE_RESET = 1;
  final public static int NAV_MODE_NEWXY = 2;
  final public static int NAV_MODE_NEWXYZ = 3;
  final public static int NAV_MODE_NEWZ = 4;


  public int navMode = NAV_MODE_RESET;
  public float zoomFactor = Float.MAX_VALUE;

  public float navigationSlabOffset;

  private JmolNavigatorInterface nav;
  
  private JmolNavigatorInterface getNav() {
    if (nav != null)
      return nav;
    nav = (JmolNavigatorInterface) Interface.getOptionInterface("io2.Navigator");
    nav.set(this, viewer);
    return nav;
  }
  
  TransformManager11(Viewer viewer, int width, int height) {
    super(viewer, width, height);
    setNavFps(10);
  }

  @Override
  protected void setNavFps(int navFps) {
    this.navFps = navFps;
  }

  @Override
  public void calcCameraFactors() {
    // (m) model coordinates
    // (s) screen coordinates = (m) * screenPixelsPerAngstrom
    // (p) plane coordinates = (s) / screenPixelCount

    if (Float.isNaN(cameraDepth)) {
      cameraDepth = cameraDepthSetting;
      zoomFactor = Float.MAX_VALUE;
    }

    // reference point where p=0
    cameraDistance = cameraDepth * screenPixelCount; // (s)

    // distance from camera to midPlane of model (p=0.5)
    // the factor to apply based on screen Z
    referencePlaneOffset = cameraDistance + screenPixelCount / 2f; // (s)

    // conversion factor Angstroms --> pixels
    // so that "full window" is visualRange
    scalePixelsPerAngstrom = (scale3D && !perspectiveDepth
        && mode != MODE_NAVIGATION ? 72 / scale3DAngstromsPerInch
        * (antialias ? 2 : 1) : screenPixelCount / visualRange); // (s/m)

    // model radius in pixels
    modelRadiusPixels = modelRadius * scalePixelsPerAngstrom; // (s)

    // model center offset for zoom 100
    float offset100 = (2 * modelRadius) / visualRange * referencePlaneOffset; // (s)

    // System.out.println("sppA " + scalePixelsPerAngstrom + " pD " +
    // perspectiveDepth
    // + " spC " + screenPixelCount + " vR " + visualRange
    // + " sDPPA " + scaleDefaultPixelsPerAngstrom);

    if (mode == MODE_NAVIGATION) {
      calcNavCameraFactors(offset100);
      return;
    }
    // nonNavigation mode -- to match Jmol 10.2 at midplane (caffeine.xyz)
    // flag that we have left navigation mode
    zoomFactor = Float.MAX_VALUE;
    // we place the model at the referencePlaneOffset offset and then change
    // the scale
    modelCenterOffset = referencePlaneOffset;
    // now factor the scale by distance from camera and zoom
    if (!scale3D || perspectiveDepth)
      scalePixelsPerAngstrom *= (modelCenterOffset / offset100) * zoomPercent
          / 100; // (s/m)

    // System.out.println("sppA revised:" + scalePixelsPerAngstrom);
    // so that's sppa = (spc / vR) * rPO * (vR / 2) / mR * rPO = spc/2/mR

    modelRadiusPixels = modelRadius * scalePixelsPerAngstrom; // (s)
    // System.out.println("transformman scalppa modelrad " +
    // scalePixelsPerAngstrom + " " + modelRadiusPixels + " " + visualRange);
  }

  private void calcNavCameraFactors(float offset100) {
    if (zoomFactor == Float.MAX_VALUE) {
      // entry point
      if (zoomPercent > MAXIMUM_ZOOM_PERSPECTIVE_DEPTH)
        zoomPercent = MAXIMUM_ZOOM_PERSPECTIVE_DEPTH;
      // screen offset to fixed rotation center
      modelCenterOffset = offset100 * 100 / zoomPercent;
    } else if (prevZoomSetting != zoomPercentSetting) {
      if (zoomRatio == 0) // scripted change zoom xxx
        modelCenterOffset = offset100 * 100 / zoomPercentSetting;
      else
        // fractional change by script or mouse
        modelCenterOffset += (1 - zoomRatio) * referencePlaneOffset;
      navMode = NAV_MODE_ZOOMED;
    }
    prevZoomSetting = zoomPercentSetting;
    zoomFactor = modelCenterOffset / referencePlaneOffset;
    // infinite or negative value means there is no corresponding non-navigating
    // zoom setting
    zoomPercent = (zoomFactor == 0 ? MAXIMUM_ZOOM_PERSPECTIVE_DEPTH : offset100
        / modelCenterOffset * 100);
    
  }

  @Override
  public float getPerspectiveFactor(float z) {
    // System.out.println (z + " getPerspectiveFactor " + referencePlaneOffset +
    // " " + (z <= 0 ? referencePlaneOffset : referencePlaneOffset / z));
    return (z <= 0 ? referencePlaneOffset : referencePlaneOffset / z);
  }

  @Override
  protected void adjustTemporaryScreenPoint() {

    // fixedRotation point is at the origin initially

    float z = point3fScreenTemp.z;

    // this could easily go negative -- behind the screen --
    // but we don't care. In fact, that just makes it easier,
    // because it means we won't render it.
    // we should probably assign z = 0 as "unrenderable"

    if (Float.isNaN(z)) {
      if (!haveNotifiedNaN)
        Logger.debug("NaN seen in TransformPoint");
      haveNotifiedNaN = true;
      z = 1;
    } else if (z <= 0) {
      // just don't let z go past 1 BH 11/15/06
      z = 1;
    }
    point3fScreenTemp.z = z;

    // x and y are moved inward (generally) relative to 0, which
    // is either the fixed rotation center or the navigation center

    // at this point coordinates are centered on rotation center

    switch (mode) {
    case MODE_NAVIGATION:
      // move nav center to 0; refOffset = Nav - Rot
      point3fScreenTemp.x -= navigationShiftXY.x;
      point3fScreenTemp.y -= navigationShiftXY.y;
      break;
    case MODE_PERSPECTIVE_CENTER:
      point3fScreenTemp.x -= perspectiveShiftXY.x;
      point3fScreenTemp.y -= perspectiveShiftXY.y;
      break;
    }
    if (perspectiveDepth) {
      // apply perspective factor
      float factor = getPerspectiveFactor(z);
      point3fScreenTemp.x *= factor;
      point3fScreenTemp.y *= factor;
    }
    switch (mode) {
    case MODE_NAVIGATION:
      point3fScreenTemp.x += navigationOffset.x;
      point3fScreenTemp.y += navigationOffset.y;
      break;
    case MODE_PERSPECTIVE_CENTER:
      point3fScreenTemp.x += perspectiveOffset.x;
      point3fScreenTemp.y += perspectiveOffset.y;
      break;
    case MODE_STANDARD:
      point3fScreenTemp.x += fixedRotationOffset.x;
      point3fScreenTemp.y += fixedRotationOffset.y;
      break;
    }

    if (Float.isNaN(point3fScreenTemp.x) && !haveNotifiedNaN) {
      Logger.debug("NaN found in transformPoint ");
      haveNotifiedNaN = true;
    }

    point3iScreenTemp.set((int) point3fScreenTemp.x, (int) point3fScreenTemp.y,
        (int) point3fScreenTemp.z);
  }

  @Override
  boolean canNavigate() {
    return true;
  }


  @Override
  protected void resetNavigationPoint(boolean doResetSlab) {
    if (zoomPercent < 5 && mode != MODE_NAVIGATION) {
      perspectiveDepth = true;
      mode = MODE_NAVIGATION;
      return;
    }
    if (mode == MODE_NAVIGATION) {
      navMode = NAV_MODE_RESET;
      slabPercentSetting = 0;
      perspectiveDepth = true;
    } else if (doResetSlab) {
      slabPercentSetting = 100;
    }
    viewer.setFloatProperty("slabRange", 0);
    if (doResetSlab) {
      slabEnabled = (mode == MODE_NAVIGATION);
    }
    zoomFactor = Float.MAX_VALUE;
    zoomPercentSetting = zoomPercent;
  }

  @Override
  public void navigatePt(float seconds, Point3f pt) {
    // from MoveToThread
    if (seconds > 0) {
      navigateTo(seconds, null, Float.NaN, pt, Float.NaN, Float.NaN, Float.NaN);
      return;
    }
    navigationCenter.setT(pt);
    navMode = NAV_MODE_NEWXYZ;
    navigating = true;
    finalizeTransformParameters();
    navigating = false;
  }

  @Override
  public
  void navigateAxis(float seconds, Vector3f rotAxis, float degrees) {
    if (degrees == 0)
      return;
    if (seconds > 0) {
      navigateTo(seconds, rotAxis, degrees, null, Float.NaN, Float.NaN,
          Float.NaN);
      return;
    }
    rotateAxisAngle(rotAxis, (float) (degrees / degreesPerRadian));
    navMode = NAV_MODE_NEWXYZ;
    navigating = true;
    finalizeTransformParameters();
    navigating = false;
  }

  @Override
  void navTranslate(float seconds, Point3f pt) {
    Point3f pt1 = new Point3f();
    transformPoint2(pt, pt1);
    if (seconds > 0) {
      navigateTo(seconds, null, Float.NaN, null, Float.NaN, pt1.x, pt1.y);
      return;
    }
    navTranslatePercent(-1, pt1.x, pt1.y);
  }

  @Override
  void navigateGuide(float seconds, Point3f[][] pathGuide) {
    navigate(seconds, pathGuide, null, null, 0, Integer.MAX_VALUE);
  }

  @Override
  void navigatePath(float seconds, Point3f[] path, float[] theta, int indexStart,
                int indexEnd) {
    navigate(seconds, null, path, theta, indexStart, indexEnd);
  }

  @Override
  void navigateSurface(float timeSeconds, String name) {
  }

  @Override
  public
  Point3f getNavigationCenter() {
    return navigationCenter;
  }

  @Override
  public float getNavigationDepthPercent() {
    return navigationDepth;
  }

  @Override
  void setNavigationSlabOffsetPercent(float percent) {
    viewer.getGlobalSettings().setParamF("navigationSlab", percent);
    calcCameraFactors(); // current
    navigationSlabOffset = percent / 50 * modelRadiusPixels;
  }

  @Override
  Point3f getNavigationOffset() {
    transformPoint2(navigationCenter, navigationOffset);
    return navigationOffset;
  }

  @Override
  public float getNavigationOffsetPercent(char XorY) {
    getNavigationOffset();
    if (width == 0 || height == 0)
      return 0;
    return (XorY == 'X' ? (navigationOffset.x - width / 2f) * 100f / width
        : (navigationOffset.y - getNavPtHeight()) * 100f / height);
  }

  @Override
  protected String getNavigationText(boolean addComments) {
    getNavigationOffset();
    return (addComments ? " /* navigation center, translation, depth */ " : " ")
        + Escape.escapePt(navigationCenter)
        + " "
        + getNavigationOffsetPercent('X')
        + " "
        + getNavigationOffsetPercent('Y') + " " + getNavigationDepthPercent();
  }

  @Override
  void setScreenParameters(int screenWidth, int screenHeight,
                           boolean useZoomLarge, boolean antialias,
                           boolean resetSlab, boolean resetZoom) {
    Point3f pt = (mode == MODE_NAVIGATION ? Point3f.newP(navigationCenter)
        : null);
    Point3f ptoff = Point3f.newP(navigationOffset);
    ptoff.x = ptoff.x / width;
    ptoff.y = ptoff.y / height;
    super.setScreenParameters(screenWidth, screenHeight, useZoomLarge,
        antialias, resetSlab, resetZoom);
    if (pt != null) {
      navigationCenter.setT(pt);
      navTranslatePercent(-1, ptoff.x * width, ptoff.y * height);
      navigatePt(0, pt);
    }
  }

  /*
   * *************************************************************** Navigation
   * support**************************************************************
   */


  public float navigationDepth;

  private void navigateTo(float floatSecondsTotal, Vector3f axis,
                          float degrees, Point3f center, float depthPercent,
                          float xTrans, float yTrans) {
    getNav().navigateTo(floatSecondsTotal, axis, degrees, center, depthPercent, xTrans, yTrans);
  }

  /**
   * @param seconds 
   * @param pathGuide 
   * @param path 
   * @param theta  
   * @param indexStart 
   * @param indexEnd 
   */
  private void navigate(float seconds, Point3f[][] pathGuide, Point3f[] path,
                        float[] theta, int indexStart, int indexEnd) {
    getNav().navigate(seconds, pathGuide, path, theta, indexStart, indexEnd);
  }

  @Override
  void zoomByFactor(float factor, int x, int y) {
    if (mode != MODE_NAVIGATION || !zoomEnabled || factor <= 0)
      super.zoomByFactor(factor, x, y);
    else
      getNav().zoomByFactor(factor, x, y);
  }

  @Override
  public void setNavigationOffsetRelative(boolean navigatingSurface) {
    getNav().setNavigationOffsetRelative(navigatingSurface);
  }

  @Override
  synchronized void navigateKey(int keyCode, int modifiers) {
    getNav().navigate(keyCode, modifiers);
  }

  @Override
  public void setNavigationDepthPercent(float timeSec, float percent) {
    getNav().setNavigationDepthPercent(timeSec, percent);
  }

  @Override
  public void navTranslatePercent(float seconds, float x, float y) {
    getNav().navTranslatePercent(seconds, x, y);
  }

  /**
   * All the magic happens here.
   * 
   */
  @Override
  protected void calcNavigationPoint() {
    getNav().calcNavigationPoint();
  }

  public float getNavPtHeight() {
    boolean navigateSurface = viewer.getNavigateSurface();
    return height / (navigateSurface ? 1f : 2f);
  }

  @Override
  protected String getNavigationState() {
    return (mode == MODE_NAVIGATION ? getNav().getNavigationState() : "");
  }

}
