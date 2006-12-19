/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-12-18 10:29:29 -0600 (Mon, 18 Dec 2006) $
 * $Revision: 6502 $
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
import javax.vecmath.Point3i;

class TransformManager10 extends TransformManager {

  TransformManager10(Viewer viewer) {
    super(viewer);
  }

  // older Jmol 10 method 
  // -- applies cameraScaleFactor to scalePixelsPerAngstrom
  // -- no navigation 
  
  protected void calcCameraFactors() {
    cameraDistance = cameraDepth * screenPixelCount;
    cameraScaleFactor = 1.02f + 0.5f / cameraDepth;
    scalePixelsPerAngstrom = scaleDefaultPixelsPerAngstrom * zoomPercent / 100 * cameraScaleFactor;
    screenCenterOffset = cameraDistance + rotationRadius * scalePixelsPerAngstrom;
    perspectiveScale = cameraDistance;
    //screenCenterOffset same as perspectiveScale, by the way
  }
  
  protected void calcSlabAndDepthValues() {
    slabValue = 0;
    depthValue = Integer.MAX_VALUE;
    if (slabEnabled) {
      // a slab percentage of 100 should map to zero
      // a slab percentage of 0 should map to -diameter
      int radius = (int) (rotationRadius * scalePixelsPerAngstrom);
      slabValue = (int) (((100 - slabPercentSetting) * 2 * radius / 100) + cameraDistance);
      depthValue = (int) (((100 - depthPercentSetting) * 2 * radius / 100) + cameraDistance);
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
      point3fScreenTemp.x *= factor;
      point3fScreenTemp.y *= factor;
    }

    //higher resolution here for spin control. 

    //now move the center point to where it needs to be

    point3fScreenTemp.x += fixedRotationOffset.x;
    point3fScreenTemp.y += fixedRotationOffset.y;

    if (Float.isNaN(point3fScreenTemp.x) && !haveNotifiedNaN) {
      Logger.debug("NaN found in transformPoint ");
      haveNotifiedNaN = true;
    }

    point3iScreenTemp.x = (int) point3fScreenTemp.x;
    point3iScreenTemp.y = (int) point3fScreenTemp.y;
    point3iScreenTemp.z = (int) point3fScreenTemp.z;

    return point3iScreenTemp;
  }
}
