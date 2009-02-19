/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-13 19:17:06 -0500 (Sat, 13 May 2006) $
 * $Revision: 5114 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
package org.jmol.quantum;

import java.util.BitSet;

import javax.vecmath.Point3f;

import org.jmol.api.MepCalculationInterface;
import org.jmol.api.VolumeDataInterface;

/*
 * a simple molecular electrostatic potential cube generator
 * just using q/r here
 * 
 * http://teacher.pas.rochester.edu/phy122/Lecture_Notes/Chapter25/Chapter25.html
 * 
 * applying some of the tricks in QuantumCalculation for speed
 * 
 * NOTE -- THIS CLASS IS INSTANTIATED USING Interface.getOptionInterface
 * NOT DIRECTLY -- FOR MODULARIZATION. NEVER USE THE CONSTRUCTOR DIRECTLY!
 */
public class MepCalculation extends QuantumCalculation implements MepCalculationInterface {

  public MepCalculation() {
  }
  
  public void calculate(VolumeDataInterface volumeData, BitSet bsSelected, Point3f[] atomCoordAngstroms, float[] charges) {
    voxelData = volumeData.getVoxelData();
    int[] countsXYZ = volumeData.getVoxelCounts();
    initialize(countsXYZ[0], countsXYZ[1], countsXYZ[2]);
    setupCoordinates(volumeData.getOriginFloat(), 
        volumeData.getVolumetricVectorLengths(), 
        bsSelected, atomCoordAngstroms);
    processMep(charges);
  }
  
  private void processMep(float[] charges) {
    for (int atomIndex = qmAtoms.length; --atomIndex >= 0;) {
      if ((thisAtom = qmAtoms[atomIndex]) == null)
        continue;
      float charge = charges[atomIndex];
      thisAtom.setXYZ(true);
      for (int ix = xMax; --ix >= xMin;) {
        float dX = X2[ix];
        for (int iy = yMax; --iy >= yMin;) {
          float dXY = dX + Y2[iy];
          for (int iz = zMax; --iz >= zMin;) {
            float d2 = dXY + Z2[iz];
            voxelData[ix][iy][iz] += (d2 == 0 ? charge
                * Float.POSITIVE_INFINITY : charge / (float) Math.sqrt(d2));
          }
        }
      }
    }
  }


}
