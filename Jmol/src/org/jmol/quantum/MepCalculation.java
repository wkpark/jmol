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
import org.jmol.jvxl.readers.Parameters;

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

  float[] charges;
  
  public MepCalculation() {
  }
  
  public void calculate(VolumeDataInterface volumeData, BitSet bsSelected, Point3f[] atomCoordAngstroms, float[] charges) {
    this.atomCoordAngstroms = atomCoordAngstroms;
    this.charges = charges;
    initialize(Parameters.MEP_MAX_GRID);
    setVolume(volumeData, bsSelected);
    processMep();
  }

  private void processMep() {
    setMinMax();
    int firstAtom = 0;
    int lastAtom = atomCoordBohr.length;
    for (int i = 0; i < lastAtom; i++)
      if (atomSet.get(i)) {
        firstAtom = i;
        break;
      }
    for (int i = lastAtom; --i >= firstAtom;)
      if (atomSet.get(i)) {
        lastAtom = i + 1;
        break;
    }

    for (int atomIndex = firstAtom; atomIndex < lastAtom; atomIndex++) {
      if (!atomSet.get(atomIndex))
        continue;
      float x = atomCoordBohr[atomIndex].x;
      float y = atomCoordBohr[atomIndex].y;
      float z = atomCoordBohr[atomIndex].z;
      float charge = charges[atomIndex];
      for (int i = countsXYZ[0]; --i >= 0;) {
        X2[i] = X[i] = xyzBohr[i][0] - x;
        X2[i] *= X[i];
      }
      for (int i = countsXYZ[1]; --i >= 0;) {
        Y2[i] = Y[i] = xyzBohr[i][1] - y;
        Y2[i] *= Y[i];
      }
      for (int i = countsXYZ[2]; --i >= 0;) {
        Z2[i] = Z[i] = xyzBohr[i][2] - z;
        Z2[i] *= Z[i];
      }
      for (int ix = xMax; --ix >= xMin;) {
        for (int iy = yMax; --iy >= yMin;)
          for (int iz = zMax; --iz >= zMin;) {
            float d2 = X2[ix] + Y2[iy] + Z2[iz];
            voxelData[ix][iy][iz] += (d2 == 0 ? charge
                * Float.POSITIVE_INFINITY : charge / (float) Math.sqrt(d2));
          }
      }
    }
  }

}
