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
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

class TransformManager11 extends TransformManager {

  private float navigationZOffset = 0;
  private final Point3f navigationCenter = new Point3f();
  private final Point3f ptNav = new Point3f(Float.NaN, Float.NaN, Float.NaN);
  private final Point3f newNavigationOffset = new Point3f();

  TransformManager11(Viewer viewer) {
    super(viewer);
  }

  TransformManager11(Viewer viewer, int width, int height) {
    super(viewer, width, height);
  }

  protected void calcCameraFactors() {
    //(m) model coordinates
    //(s) screen coordinates = (m) * screenPixelsPerAngstrom
    //(p) plane coordinates = (s) / screenPixelCount

    // conversion factor Angstroms --> pixels
    scalePixelsPerAngstrom = scaleDefaultPixelsPerAngstrom * zoomPercent / 100; //(s/m)

    // distance from the front plane of the model at zoom=100, where p=0 
    cameraDistance = cameraDepth * screenPixelCount; //(s)

    // screen offset to fixed rotation center
    modelCenterOffset = cameraDistance + screenPixelCount / 2f
        + navigationZOffset; //(s)

    // factor to apply based on screen Z
    perspectiveScale = cameraDistance + screenPixelCount / 2f; //(s)

    // factor to apply as part of the transform (not used here)
    cameraScaleFactor = 1; //unitless

    // vertical screen plane of the observer where objects will be clipped
    observerOffset = visualRange / (2 * rotationRadius) * perspectiveScale; //(s)
  }

  protected void calcSlabAndDepthValues() {
    slabValue = 0;
    depthValue = Integer.MAX_VALUE;
    //System.out.println("calcSlab " + slabEnabled + " " + slabPercentSetting
    //  + " " + visualRange);
    if (slabEnabled) {
      float radius = rotationRadius * scalePixelsPerAngstrom;
      float center = cameraDistance + screenPixelCount / 2f;
      if (perspectiveDepth && visualRange > 0 && slabPercentSetting == 0) {
        slabValue = (int) observerOffset;
        depthValue = Integer.MAX_VALUE;
        System.out.println("fixedNavigationOffset=" + navigationOffset
            + " navigationZOffset=" + navigationZOffset);
        return;
      }
      // a slab percentage of 100 should map to zero
      // a slab percentage of 0 should map to -diameter
      slabValue = (int) (((50 - slabPercentSetting) * radius / 50) + center);
      depthValue = (int) (((50 - depthPercentSetting) * radius / 50) + center);
      //System.out.println("sv=" + slabValue + ","+slabPercentSetting+" dv=" + depthValue +","+depthPercentSetting+ " cent=" + center + " cdist=" + cameraDistance + " ps="+ perspectiveScale+" cdepth=" + cameraDepth + " radius="+radius );
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

  // navigation

  boolean canNavigate() {
    return true;
  }

  /**
   * entry point for keyboard-based navigation
   * 
   * @param keyCode  0 indicates key released    
   * @param modifiers shift,alt,ctrl
   */
  synchronized void navigate(int keyCode, int modifiers) {
    if (!isNavigationMode)
      return;
    if (keyCode == 0) {
      if (!navigating)
        return;
      navigating = false;
      return;
    }
    boolean isOffsetShifted = ((modifiers & InputEvent.SHIFT_MASK) > 0);
    boolean isAltKey = ((modifiers & InputEvent.ALT_MASK) > 0);
    newNavigationOffset.set(navigationOffset);
    ptNav.set(0, 0, 0);
    switch (keyCode) {
    case KeyEvent.VK_UP:
      if (isOffsetShifted)
        newNavigationOffset.y -= 2;
      else if (isAltKey)
        rotateXRadians(radiansPerDegree * -.2f);
      else if (isNavigationDistant())
        zoomBy(1);
      else
        navigationZOffset -= 5;
      ptNav.z = Float.NaN;
      break;
    case KeyEvent.VK_DOWN:
      if (isOffsetShifted)
        newNavigationOffset.y += 2;
      else if (isAltKey)
        rotateXRadians(radiansPerDegree * .2f);
      else if (isNavigationDistant())
        zoomBy(-1);
      else
        navigationZOffset += 5;
      ptNav.z = Float.NaN;
      break;
    case KeyEvent.VK_LEFT:
      if (isOffsetShifted)
        newNavigationOffset.x -= 2;
      else
        rotateYRadians(radiansPerDegree * 3 * -.2f);
      break;
    case KeyEvent.VK_RIGHT:
      if (isOffsetShifted)
        newNavigationOffset.x += 2;
      else
        rotateYRadians(radiansPerDegree * 3 * .2f);
      break;
    default:
      navigating = false;
      return;
    }
    if (isOffsetShifted)
      ptNav.y = Float.NaN;
    navigating = true;
  }

  /**
   * determines whether the visualRange plane is outside the nominal
   * model radius, in which case we should just zoom and not move the center
   * any closer
   * 
   * @return whether it is appropriate to zoom
   */
  private boolean isNavigationDistant() {
    return (fixedRotationOffset.z - rotationRadius * scalePixelsPerAngstrom > observerOffset);
  }

  /** 
   * scripted navigation
   * 
   * @param seconds  number of seconds to allow for total movement, like moveTo
   * @param path     sequence of points to turn into a hermetian path
   * @param theta    orientation angle along path (0 aligns with window Y axis) 
   *                 [or Z axis if path is vertical] 
   *                 
   *                 not implemented yet
   */
  void navigate(float seconds, Point3f[] path, float[] theta) {
    // TODO
  }

  protected void unsetNavigationPoint() {
    if (ptNav == null)
      return;
    ptNav.x = Float.NaN;
  }

  /**
   * All the magic happens here.
   *
   */
  protected void calcNavigationPoint() {
    boolean isReset = Float.isNaN(ptNav.x);
    boolean isNewXY = Float.isNaN(ptNav.y);
    boolean isNewZ = Float.isNaN(ptNav.z);
    ptNav.set(0, 0, 0);
    if (isReset) {
      //simply place the navigation center in front of the fixed rotation center
      navigationZOffset = 0;
      calcCameraFactors();
      calcTransformMatrix();
      isNavigationMode = false;
      transformPoint(fixedRotationCenter, navigationOffset);
      navigationOffset.z = observerOffset;
      findCenterAt(fixedRotationCenter, navigationOffset, navigationCenter);
    } else if (isNewXY || !navigating) {
      // redefine the navigation center based on its new or old screen position
      if (navigating)
        navigationOffset.set(newNavigationOffset);
      findCenterAt(fixedRotationCenter, navigationOffset, navigationCenter);
    } else if (isNewZ) {
      // nothing special to do -- navigationZOffset has changed.
    } else {
      // must just be (not so!) simple navigation
      // navigation center will initially move
      // but we center it by moving the rotation center instead
      matrixTransform(navigationCenter, ptNav);
      matrixTransform(fixedRotationCenter, pointT);
      navigationZOffset = observerOffset + (pointT.z - ptNav.z)
          - perspectiveScale;
      calcCameraFactors();
      calcTransformMatrix();
    }
    matrixTransform(navigationCenter, referenceOffset);
    transformPoint(fixedRotationCenter, fixedTranslation);
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

  class NavigationThread extends Thread implements Runnable {

    public void run() {
      long startTime = System.currentTimeMillis();
      long lastRepaintTime = startTime;
      try {
        do {
          long currentTime = System.currentTimeMillis();
          int elapsed = (int) (currentTime - lastRepaintTime);
          int sleepTime = 33 - elapsed;
          if (sleepTime > 0)
            Thread.sleep(sleepTime);
          //
          lastRepaintTime = currentTime = System.currentTimeMillis();
          elapsed = (int) (currentTime - startTime);
          //what here?
          viewer.refresh(1, "TransformationManager:NavigationThread:run()");
        } while (!isInterrupted());
      } catch (InterruptedException ie) {
      }
    }
  }

  protected String getNavigationState() {
    StringBuffer commands = new StringBuffer("");

    return commands.toString();
  }

}
