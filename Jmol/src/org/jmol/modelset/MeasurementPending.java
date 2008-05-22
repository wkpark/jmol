/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.modelset;

import javax.vecmath.Point3f;

import org.jmol.vecmath.Point3fi;

public class MeasurementPending extends Measurement {

  private boolean isActive = false;
  
  public boolean getIsActive() {
    return isActive;
  }
  public void setIsActive(boolean TF) {
    isActive = TF;
  }
  
  public MeasurementPending(ModelSet modelSet) {
    super(modelSet, null, Float.NaN, (short) 0, null, 0);
    countPlusIndices = new int[5];
  }

  public void setPoint(int i, Point3f pt) {
    i--;
    if (points[i] == null)
      points[i] = new Point3fi();
    points[i].set(pt);
  }
  
  public void setCountPlusIndices(int[] countPlusIndices, Point3f ptClicked) {
    
    if (countPlusIndices == null) {
      count = 0;
      isActive = false;
    } else {
      count = countPlusIndices[0];
      if (ptClicked != null) {
        setPoint(count, ptClicked);
        countPlusIndices[count] = -1 - count; 
      }
      this.countPlusIndices = new int[count + 1];
      System.arraycopy(countPlusIndices, 0, this.countPlusIndices, 0,
                       count + 1);
      isActive = true;
    }
    if (this.countPlusIndices != null) 
      value = getMeasurement();
    formatMeasurement();
  }
  public boolean checkPoint(int[] countPlusIndices, Point3f ptClicked) {
    for (int i = count; --i >= 0;)
      if (countPlusIndices[i + 1] == -2 - i 
          && points[i].distance(ptClicked) < 0.01)
        return false;
    return true;
  }
}


