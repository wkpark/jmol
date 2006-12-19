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

  TransformManager11(Viewer viewer) {
    super(viewer);
  }

  TransformManager11(Viewer viewer, int width, int height) {
    super(viewer);
    setScreenDimension(width, height);
    scaleFitToScreen();
  }

  protected void calcCameraFactors() {
    cameraDistance = cameraDepth * screenPixelCount;
    screenCenterOffset = cameraDistance + screenPixelCount /2f;
    perspectiveScale = screenCenterOffset;
    cameraScaleFactor = 1;
    scalePixelsPerAngstrom = scaleDefaultPixelsPerAngstrom * zoomPercent / 100;
  }
  
  protected void calcSlabAndDepthValues() {
    slabValue = 0;
    depthValue = Integer.MAX_VALUE;
    System.out.println("calcSlab " + slabEnabled + " " + slabPercentSetting + " " + visualRange);
    if (slabEnabled) {
      float radius = rotationRadius * scalePixelsPerAngstrom;
      float center = cameraDistance + screenPixelCount / 2f;
      if (perspectiveDepth && visualRange > 0 && slabPercentSetting == 0) {
        slabValue = (int) fixedNavigationOffset.z - 1;
        depthValue = Integer.MAX_VALUE;
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
      float factor = getPerspectiveFactor(z);
      if (isNavigationMode) {
        // move nav center to 0; refOffset = ptNav - ptFixedRot 
        point3fScreenTemp.x -= referenceOffset.x;
        point3fScreenTemp.y -= referenceOffset.y;
      }
      point3fScreenTemp.x *= factor;
      point3fScreenTemp.y *= factor;
    }

    //higher resolution here for spin control. 

    //now move the center point to where it needs to be

    if (isNavigationMode) {
      point3fScreenTemp.x += fixedNavigationOffset.x;
      point3fScreenTemp.y += fixedNavigationOffset.y;
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

  boolean canNavigate () {
    return true;
  }

  private Point3f ptNav = new Point3f();
  private final Point3f newNavigationOffset = new Point3f();

  synchronized void navigate(int keyWhere, int modifiers) {
    if (!isNavigationMode)
      return;
    if (keyWhere == 0) {
      if (!navigating)
        return;
      navigating = false;
      return;
    }
    if (!navigating) {
      if (Float.isNaN(ptNav.x)) {
        transformPoint(fixedRotationCenter, fixedNavigationOffset);
      }
    }
    boolean isOffsetShifted = ((modifiers & InputEvent.SHIFT_MASK) > 0);
    newNavigationOffset.set(fixedNavigationOffset);
    switch (keyWhere) {
    case KeyEvent.VK_UP:
      if (isOffsetShifted)
        newNavigationOffset.y -= 2;
      else if ((modifiers & InputEvent.ALT_MASK) > 0)
        rotateXRadians(radiansPerDegree * -.2f);
      else {
        ptNav.z = Float.NaN;
        zoomBy(1);
        newNavigationOffset.z = -1;
      }
      break;
    case KeyEvent.VK_DOWN:
      if (isOffsetShifted)
        newNavigationOffset.y += 2;
      else if ((modifiers & InputEvent.ALT_MASK) > 0)
        rotateXRadians(radiansPerDegree * .2f);
      else {
        ptNav.z = Float.NaN;
        zoomBy(-1);
        newNavigationOffset.z = 1;
      }
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

  void navigate(float seconds, Point3f[] path) {  
     // TODO
  }

  protected void calcNavigationPoint() {
    matrixTransform(navigationCenter, pointT);
    if (isNavigationMode)
      recenterNavigationPoint();
    else
      fixedNavigationOffset.z = findZFromVisualRange();
    unTransformPoint(fixedNavigationOffset, navigationCenter);
  }
  
  protected void unsetNavigationPoint(boolean getNew) {
    ptNav.x = Float.NaN;
  }

  private void recenterNavigationPoint() {
    //find rotationRadius in screen coordinates at z of fixedRotationCenter
    boolean isReset = Float.isNaN(ptNav.x);
    boolean isNewXY = Float.isNaN(ptNav.y);
    boolean isNewZ = Float.isNaN(ptNav.z);
    ptNav.set(0, 0, 0);
    if (isReset) {
      isNavigationMode = false;
      navigationCenter.set(fixedRotationCenter);
      transformPoint(navigationCenter, fixedNavigationOffset);
      fixedNavigationOffset.z = findZFromVisualRange();
      findCenterAt(fixedNavigationOffset, fixedRotationCenter, navigationCenter);
    } else if (isNewXY || !navigating) {
      if (navigating)
        fixedNavigationOffset.set(newNavigationOffset);
      findCenterAt(fixedNavigationOffset, fixedRotationCenter, navigationCenter);
    }
    matrixTransform(navigationCenter, referenceOffset);
    transformPoint(fixedRotationCenter, fixedTranslation);
    if (isNewZ)
      fixedNavigationOffset.z = findZFromVisualRange();
    else
      transformPoint(navigationCenter, fixedNavigationOffset);
  }

  /**
   * determines the vertical plane screen Z value that corresponds to a
   * specific screen range in Angstroms
   * 
   * @return vertical plane Z value
   */
  private float findZFromVisualRange() {
    if (visualRange <= 0)
      return 1;
    
    // perspective Scale f = (c+0.5)/(c+p) = screenAngstroms / visualRange

    float screenAngstroms = screenPixelCount / scalePixelsPerAngstrom;
    float f = screenAngstroms / visualRange;
    float p = (cameraDepth + 0.5f) / f - cameraDepth;
    float z = cameraDistance + screenPixelCount * p;
    return z;
  }
  
  /**
   * 
   * @param screenXYZ 
   * @param modelXYZ
   * @param center
   */
  private void findCenterAt(Point3f screenXYZ, Point3f modelXYZ, Point3f center) {
    //we do not want the fixed navigation offset to change
    //the fixed rotation center is at fixedTranslation
    //this means that the navigationCenter must be recalculated
    //based on its former offset in the new context.
    //alas, we have two points, N(navigation) and R(rotation).
    //we know where they ARE: fixedNavigationOffset and fixedTranslation
    //from these we must derive navigationCenter
    //1) get the Z value of the fixed rotation center and 
    //   transfer it to a copy of the fixedNavigationOffset
    isNavigationMode = false;
    transformPoint(modelXYZ, pointT);
    pointT.x -= screenXYZ.x;
    pointT.y -= screenXYZ.y;
    float f = -getPerspectiveFactor(pointT.z);
    pointT.x /= f;
    pointT.y /= f;
    pointT.z = screenXYZ.z;
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

}
