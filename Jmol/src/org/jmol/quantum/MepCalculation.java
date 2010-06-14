/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-13 19:17:06 -0500 (Sat, 13 May 2006) $
 * $Revision: 5114 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.quantum;

import java.util.BitSet;

import javax.vecmath.Point3f;

import org.jmol.api.MepCalculationInterface;
import org.jmol.api.VolumeDataInterface;
import org.jmol.modelset.Atom;

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

  protected final static int ONE_OVER_D = 0;
  protected final static int E_MINUS_D = 1;
  protected final static int ONE_OVER_ONE_PLUS_D = 2;
  protected final static int ONE_OVER_MINUS_D_31 = 3;
  
  protected int distanceMode = ONE_OVER_D;
  
  public MepCalculation() {
    rangeBohr = 15; //bohr; about 7 Angstroms
    distanceMode = ONE_OVER_D;
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
      //System.out.println("process map for atom " + atomIndex + " nX,nY,nZ=" + nX + "," + nY + "," + nZ + " charge=" + charge);
      thisAtom.setXYZ(true);
      for (int ix = xMax; --ix >= xMin;) {
        float dX = X2[ix];
        for (int iy = yMax; --iy >= yMin;) {
          float dXY = dX + Y2[iy];
          for (int iz = zMax; --iz >= zMin;) {
            float d2 = dXY + Z2[iz];
            float x = charge;
            if (d2 == 0) {
              x *= Float.POSITIVE_INFINITY;
              continue;
            }
            switch (distanceMode) {
            case ONE_OVER_D:
              x /= (float) Math.sqrt(d2);
              break;
            case E_MINUS_D:
              x *= (float) Math.exp(-Math.sqrt(d2));
            }
            voxelData[ix][iy][iz] += x;
          }
        }
      }
    }
    
  }

  public void fillPotentials(Atom[] atoms, float[] potentials, BitSet bsAromatic, BitSet bsCarbonyl) {
  }


}
