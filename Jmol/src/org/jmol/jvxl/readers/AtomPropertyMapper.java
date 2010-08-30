/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import javax.vecmath.Point3f;

import org.jmol.util.Logger;

import org.jmol.api.AtomIndexIterator;

class AtomPropertyMapper extends AtomDataReader {

  AtomPropertyMapper(SurfaceGenerator sg) {
    super(sg);
  }
  //// maps property data ////
  
  private boolean doSmoothProperty;
  private AtomIndexIterator iter;

  
  @Override
  protected void setup() {
    super.setup();
    // MAP only
    volumeData.sr = this;
    volumeData.doIterate = false;
    point = params.point;
    doSmoothProperty = params.propertySmoothing;
    doUseIterator = true;
    maxDistance = 4;
    getAtoms(Float.NaN, false, doSmoothProperty);    
    setHeader("property", params.calculationType);
    // for plane mapping
    setRangesAndAddAtoms(params.solvent_ptsPerAngstrom, params.solvent_gridMax, 0); 
    params.cutoff = 0;
  }

  @Override
  protected void initializeMapping() {
    if (Logger.debugging)
      Logger.startTimer();
    bsMySelected.or(bsNearby);
    iter = atomDataServer.getSelectedAtomIterator(bsMySelected, false, true);
  }
  
  @Override
  protected void finalizeMapping() {
    iter.release();
    iter = null;
    if (Logger.debugging)
      Logger.checkTimer("property mapping time");
  }
  
  //////////// meshData extensions ////////////

  /////////////// calculation methods //////////////
    
  @Override
  protected void generateCube() {
    // not applicable
  }

  @Override
  public float getValueAtPoint(Point3f pt) {
    float value = (doSmoothProperty ? 0 : Float.NaN);
    float dmin = Float.MAX_VALUE;
    float dminNearby = Float.MAX_VALUE;
    float vdiv = 0;
    atomDataServer.setIteratorForPoint(iter, modelIndex, pt, maxDistance);
    while (iter.hasNext()) {
      int iAtom = myIndex[iter.next()];
      boolean isNearby = (iAtom >= firstNearbyAtom);
      Point3f ptA = atomXyz[iAtom];
      float p = atomProp[iAtom];
      if (Float.isNaN(p))
        continue;
      float d = pt.distance(ptA);
      if (isNearby) {
        if (d < dminNearby)
          dminNearby = d;
      } else if (d < dmin) {
        dmin = d;
        if (!doSmoothProperty)
          value = p;
      }
      if (doSmoothProperty) { // fourth-power smoothing
        d = 1 / d;
        d *= d;
        d *= d;
        vdiv += d;
        value += d * p;
      }
    }
    return (doSmoothProperty ? (vdiv == 0  || dminNearby < dmin ? Float.NaN : value / vdiv) : value);
  }

}
