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

import org.jmol.api.MOCalculationInterface;
import org.jmol.api.VolumeDataInterface;
import org.jmol.util.Logger;

import javax.vecmath.Point3f;

import java.util.List;
import java.util.BitSet;

/*
 * promolecular NCIPLOT 
 * (no mapping yet)
 * 
 * ref: 
 * 
 * "Revealing Noncovalent Interactions", 
 * Erin R. Johnson, Shahar Keinan, Paula Mori-Sanchez, Julia Contreras-Garcia, Aron J. Cohen, and Weitao Yang, 
 * J. Am. Chem. Soc., 2010, 132, 6498-6506. email to julia.contreras@duke.edu
 * 
 * "NCIPLOT: A Program for Plotting Noncovalent Interaction Regions"
 * Julia Contreras-García, Erin R. Johnson, Shahar Keinan, Robin Chaudret, Jean-Philip Piquemal, David N. Beratan, and Weitao Yang,
 * J. of Chemical Theory and Computation, 2011, 7, 625-632
 * 
 * 
 * Bob Hanson hansonr@stolaf.edu 6/1/2011
 * 
 */

/*
 * NOTE -- THIS CLASS IS INSTANTIATED USING Interface.getOptionInterface
 * NOT DIRECTLY -- FOR MODULARIZATION. NEVER USE THE CONSTRUCTOR DIRECTLY!
 * 
 */

public class NciCalculation extends QuantumCalculation implements
    MOCalculationInterface {

  private final static double CUT = -50;
  
  //   H                                                      He
  //   Li      Be          B      C      N      O      F      Ne
  //   Na      Mg          Al     Si     P      S      Cl     Ar
  
  private static double[] coef1 = new double[] {
    0, 0.2815,                                                2.437, 
        11.84, 31.34,      67.82, 120.2, 190.9, 289.5, 406.3, 561.3, 
        760.8, 1016.,      1319., 1658., 2042., 2501., 3024., 3625.
  };
  
  private static double[] coef2 = new double[] {
    0, 0.,                                                     0., 
       0.06332, 0.3694,    0.8527, 1.172, 2.247, 2.879, 3.049, 6.984, 
       22.42,   37.17,     57.95,  87.16, 115.7, 158.0, 205.5, 260.0     
  };
  
  private static double[] coef3 = new double[] {
    0,  0.,                                                       0., 
        0., 0.,                               0., 0., 0., 0., 0., 0.,
        0.06358, 0.3331,     0.8878, 0.7888, 1.465, 2.170, 3.369, 5.211  
  };
  
  private static double[] zeta1 = new double[] {
    0, 0.5288,                                                          0.3379, 
       0.1912, 0.1390,          0.1059, 0.0884, 0.0767, 0.0669, 0.0608, 0.0549, 
       0.0496, 0.0449,          0.0411, 0.0382, 0.0358, 0.0335, 0.0315, 0.0296 
  };
  
  private static double[] zeta2 = new double[] {
    0,  1.,                                                             1.,     
        0.9992, 0.6945,         0.5300, 0.5480, 0.4532, 0.3974, 0.3994, 0.3447, 
        0.2511, 0.2150,         0.1874, 0.1654, 0.1509, 0.1369, 0.1259, 0.1168     
  };
  
  private static double[] zeta3 = new double[] {
    0, 1.,                                                              1., 
       1., 1.,                                      1., 1., 1., 1., 1., 1., 
       1.0236, 0.7753,          0.5962, 0.6995, 0.5851, 0.5149, 0.4974, 0.4412     
  };
  
  protected float[][][] rhoTemp;
  protected float[][][] gxTemp;
  protected float[][][] gyTemp;
  protected float[][][] gzTemp;

  private boolean havePoints;

  private boolean densityOnly;
  
  public NciCalculation() {
  }

  public void calculate(VolumeDataInterface volumeData, BitSet bsSelected,
                        String calculationType, Point3f[] atomCoordAngstroms,
                        int firstAtomOffset, List<int[]> shells,
                        float[][] gaussians, int[][] dfCoefMaps,
                        Object slaters,
                        float[] moCoefficients, float[] linearCombination, float[][] coefs,
                        float[] nuclearCharges, boolean densityOnly, Point3f[] points) {
    havePoints = (points != null);
    this.densityOnly = densityOnly;
    int[] countsXYZ = volumeData.getVoxelCounts();
    initialize(countsXYZ[0], countsXYZ[1], countsXYZ[2], points);
    voxelData = volumeData.getVoxelData();
    rhoTemp = new float[nX][nY][nZ];
    gxTemp = new float[nX][nY][nZ];
    gyTemp = new float[nX][nY][nZ];
    gzTemp = new float[nX][nY][nZ];
    setupCoordinates(volumeData.getOriginFloat(), 
        volumeData.getVolumetricVectorLengths(), 
        bsSelected, atomCoordAngstroms, points);
    atomIndex = firstAtomOffset;
    doDebug = (Logger.debugging);
    createCube();
  }  
  
  private void createCube() {
    process();
  }  

  private static double c = (1 / (2 * Math.pow(3 * Math.PI * Math.PI, 1/3d)));
  private static double rpower = -4/3d;
  
  private void process() {
    for (int i = qmAtoms.length; --i >= atomIndex;) {
      if (qmAtoms[i] == null)
        continue;
      // must ignore heavier elements
      if (qmAtoms[i].znuc < 1)
        qmAtoms[i] = null;
      else if (qmAtoms[i].znuc > 18)
        qmAtoms[i].znuc = 18; // just max it out at argon?
    }    
    for (int iX = 0; iX < nX; iX++) {
      for (int iY = 0; iY < nY; iY++) {
        for (int iZ = 0; iZ < nZ; iZ++) {
          process(iX, iY, iZ);
        }
      }      
    }
    for (int iX = 0; iX < nX; iX++) {
      for (int iY = 0; iY < nY; iY++) {
        for (int iZ = 0; iZ < nZ; iZ++) {
          double sumgx = gxTemp[iX][iY][iZ];
          double sumgy = gyTemp[iX][iY][iZ];
          double sumgz = gzTemp[iX][iY][iZ];
          double grad = Math.sqrt(sumgx * sumgx + sumgy * sumgy + sumgz * sumgz);
          double s = (densityOnly ? rhoTemp[iX][iY][iZ] : c * grad * Math.pow(rhoTemp[iX][iY][iZ], rpower));
          voxelData[iX][iY][iZ] = (float) s;
        }
      }      
    }
  }
  
  private void process(int iX, int iY, int iZ) {
    float sx = xBohr[iX];
    float sy = yBohr[iY];
    float sz = zBohr[iZ];
    for (int i = qmAtoms.length; --i >= atomIndex;) {
      if (qmAtoms[i] == null)
        continue;
      int znuc = qmAtoms[i].znuc;
      double z1 = zeta1[znuc];
      double z2 = zeta2[znuc];
      double z3 = zeta3[znuc];
      //System.out.println(i + " " + znuc + " " + z1 + " " + z2 + " " + z3 + " " + coef1[znuc] + " " + coef2[znuc] +" " + coef3[znuc]);
      float x = sx - qmAtoms[i].x;
      float y = sy - qmAtoms[i].y;
      float z = sz - qmAtoms[i].z;
      double r = Math.sqrt(x * x + y * y + z * z);
      double ce1 = coef1[znuc] * Math.exp(-r / z1);
      double ce2 = coef2[znuc] * Math.exp(-r / z2);
      double ce3 = coef3[znuc] * Math.exp(-r / z3);
      float fr = (float) ((ce1 / z1 + ce2 / z2 + ce3 / z3) / r);
      rhoTemp[iX][iY][iZ] += ce1 + ce2 + ce3;
      gxTemp[iX][iY][iZ] -= fr * x;
      gyTemp[iX][iY][iZ] -= fr * y;
      gzTemp[iX][iY][iZ] -= fr * z;
    }
  }

  public void calculateElectronDensity(float[] nuclearCharges) {
    // n/a
  }
}
