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
import org.jmol.util.Eigen;
import org.jmol.util.Logger;

import javax.vecmath.Point3f;

import java.util.List;
import java.util.BitSet;

/*
 * promolecular NCIPLOT implemented in Jmol 12.1.49 as
 * 
 *   isosurface NCI
 * 
 * and 
 * 
 *   isosurface NCI map NCI
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
  
  private boolean havePoints;
  private boolean isReducedDensity;
  
  public NciCalculation() {
  }

  private Eigen eigen;
  public boolean setupCalculation(VolumeDataInterface volumeData, BitSet bsSelected,
                        String calculationType, Point3f[] atomCoordAngstroms,
                        int firstAtomOffset, List<int[]> shells,
                        float[][] gaussians, int[][] dfCoefMaps,
                        Object slaters,
                        float[] moCoefficients, float[] linearCombination, float[][] coefs,
                        float[] nuclearCharges, boolean isDensity, Point3f[] points) {
    havePoints = (points != null);
    isReducedDensity = isDensity;
    this.firstAtomOffset = firstAtomOffset;
    int[] countsXYZ = volumeData.getVoxelCounts();
    initialize(countsXYZ[0], countsXYZ[1], countsXYZ[2], points);
    voxelData = volumeData.getVoxelData();
    if (havePoints) {
      xMin = yMin = zMin = 0;
      xMax = yMax = zMax = points.length;
    }
    if (!isReducedDensity) {
      eigen = new Eigen(3);
      hess = new double[3][3];
    }
    setupCoordinates(volumeData.getOriginFloat(), 
        volumeData.getVolumetricVectorLengths(), 
        bsSelected, atomCoordAngstroms, points);
    
    doDebug = (Logger.debugging);
    return true;
  }  
  
  public void createCube() {
    setXYZBohr();
    process();
  }  
  
  private static double c = (1 / (2 * Math.pow(3 * Math.PI * Math.PI, 1/3d)));
  private static double rpower = -4/3d;
  private double[][] hess;
  private double gxTemp, gyTemp, gzTemp, gxxTemp, gyyTemp, gzzTemp, gxyTemp, gyzTemp, gxzTemp;
  
  @Override
  protected void process() {
    for (int i = qmAtoms.length; --i >= firstAtomOffset;) {
      if (qmAtoms[i] == null)
        continue;
      // must ignore heavier elements
      if (qmAtoms[i].znuc < 1)
        qmAtoms[i] = null;
      else if (qmAtoms[i].znuc > 18)
        qmAtoms[i].znuc = 18; // just max it out at argon?
    }
   for (int ix = xMax; --ix >= xMin;) {
      for (int iy = yMax; --iy >= yMin;) {
        vd = voxelData[ix][(havePoints ? 0 : iy)];
        for (int iz = zMax; --iz >= zMin;) {
          double rho = process(ix, iy, iz);
          double s;
          if (isReducedDensity) {
            double grad = Math.sqrt(gxTemp * gxTemp + gyTemp * gyTemp + gzTemp * gzTemp);
            s = c * grad * Math.pow(rho, rpower);
          } else{
            // do Hessian only for specified points
            //GET LAMBDA
            hess[0][0] = gxxTemp;
            hess[1][0] = hess[0][1] = gxyTemp;
            hess[2][0] = hess[0][2] = gxzTemp;
            hess[1][1] = gyyTemp;
            hess[1][2] = hess[2][1] = gyzTemp;
            hess[2][2] = gzzTemp;
            eigen.calc(hess);            
            double lambda2 = eigen.getEigenvalues()[1];
            s = Math.signum(lambda2) * Math.abs(rho);
          }
          vd[(havePoints ? 0 : iz)] = (float) s;
        }
      }
    }
  }
  
  private double process(int ix, int iy, int iz) {
    double rho = 0;
    if (isReducedDensity) {
      gxTemp = gyTemp = gzTemp = 0;
    } else {
      gxxTemp = gyyTemp = gzzTemp = gxyTemp = gyzTemp = gxzTemp = 0;      
    }
    for (int i = qmAtoms.length; --i >= firstAtomOffset;) {
      if (qmAtoms[i] == null)
        continue;
      int znuc = qmAtoms[i].znuc;
      double z1 = zeta1[znuc];
      double z2 = zeta2[znuc];
      double z3 = zeta3[znuc];
      //System.out.println(i + " " + znuc + " " + z1 + " " + z2 + " " + z3 + " " + coef1[znuc] + " " + coef2[znuc] +" " + coef3[znuc]);
      double x = xBohr[ix] - qmAtoms[i].x;
      double y = yBohr[iy] - qmAtoms[i].y;
      double z = zBohr[iz] - qmAtoms[i].z;
      double r = Math.sqrt(x * x + y * y + z * z);
      double ce1 = coef1[znuc] * Math.exp(-r / z1);
      double ce2 = coef2[znuc] * Math.exp(-r / z2);
      double ce3 = coef3[znuc] * Math.exp(-r / z3);
      /*
      R=SQRT((SVEC(1)-X3(I))**2+(SVEC(2)-Y3(I))**2+(SVEC(3)-Z3(I))**2)
      EXP1=exp(-R/ZETA1(J))
      EXP2=exp(-R/ZETA2(J))
      EXP3=exp(-R/ZETA3(J))
      RHO=RHO + C1(J)*EXP1 + C2(J)*EXP2 + C3(J)*EXP3
      */
      rho += ce1 + ce2 + ce3;
      double fac1r = (ce1 / z1 + ce2 / z2 + ce3 / z3) / r;
      if (isReducedDensity) {
        gxTemp -= fac1r * x;
        gyTemp -= fac1r * y;
        gzTemp -= fac1r * z;
        /*
        FAC=(C1(J)/ZETA1(J))*EXP1 + (C2(J)/ZETA2(J))*EXP2 +
       +    (C3(J)/ZETA3(J))*EXP3
        GRADX=GRADX - FAC*(SVEC(1)-X3(I))/R
        GRADY=GRADY - FAC*(SVEC(2)-Y3(I))/R
        GRADZ=GRADZ - FAC*(SVEC(3)-Z3(I))/R
        */
      } else {
        /*
        FAC1=(C1(J)/ZETA1(J))*EXP1 + (C2(J)/ZETA2(J))*EXP2 +
       +     (C3(J)/ZETA3(J))*EXP3
        FAC2=(C1(J)/ZETA1(J)**2)*EXP1 + (C2(J)/ZETA2(J)**2)*EXP2 +
       +     (C3(J)/ZETA3(J)**2)*EXP3
       
        but here we see what is going to happen with them, and condense;
        
        GRADXX=GRADXX + FAC2*((SVEC(1)-X3(I))/R)**2 
              + FAC1*( (SVEC(1)-X3(I))**2/R**3 - 1.D0/R )
       
         == FAC2 * x^2/r^2 + FAC1 * (x^2/r^3 - 1/r)
         == (FAC2 + FAC1/r) * x/r * x/r - FAC1/r
         
        and

        GRADXY=GRADXY + FAC2*(SVEC(1)-X3(I))*(SVEC(2)-Y3(I))/R**2 
              + FAC1*(SVEC(1)-X3(I))*(SVEC(2)-Y3(I))/R**3
              
         == (FAC2 + FAC1/r) * x/r * y/r 
        GRADXZ=GRADXZ + FAC2*(SVEC(1)-X3(I))*(SVEC(3)-Z3(I))/R**2
       +       + FAC1*(SVEC(1)-X3(I))*(SVEC(3)-Z3(I))/R**3
        GRADYZ=GRADYZ + FAC2*(SVEC(2)-Y3(I))*(SVEC(3)-Z3(I))/R**2
       +       + FAC1*(SVEC(2)-Y3(I))*(SVEC(3)-Z3(I))/R**3
       */
         x /= r;
         y /= r;
         z /= r;
        double fr2 = fac1r + (ce1 / z1 / z1 + ce2 / z2 / z2 + ce3 / z3 / z3);
        gxxTemp += fr2 * x * x - fac1r;
        gyyTemp += fr2 * y * y - fac1r;
        gzzTemp += fr2 * z * z - fac1r;
        gxyTemp += fr2 * x * y;
        gxzTemp += fr2 * x * z;
        gyzTemp += fr2 * y * z;
      }
    }
    return rho;
  }

  public void calculateElectronDensity(float[] nuclearCharges) {
    // n/a
  }
  
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
    0,  0.,                                                    0., 
        0., 0.,                            0., 0., 0., 0., 0., 0.,
        0.06358, 0.3331,  0.8878, 0.7888, 1.465, 2.170, 3.369, 5.211  
  };
  
  private static double[] zeta1 = new double[] {
    0, 0.5288,                                                    0.3379, 
       0.1912, 0.1390,    0.1059, 0.0884, 0.0767, 0.0669, 0.0608, 0.0549, 
       0.0496, 0.0449,    0.0411, 0.0382, 0.0358, 0.0335, 0.0315, 0.0296 
  };
  
  private static double[] zeta2 = new double[] {
    0,  1.,                                                       1.,     
        0.9992, 0.6945,   0.5300, 0.5480, 0.4532, 0.3974, 0.3994, 0.3447, 
        0.2511, 0.2150,   0.1874, 0.1654, 0.1509, 0.1369, 0.1259, 0.1168     
  };
  
  private static double[] zeta3 = new double[] {
    0, 1.,                                                        1., 
       1., 1.,                                1., 1., 1., 1., 1., 1., 
       1.0236, 0.7753,    0.5962, 0.6995, 0.5851, 0.5149, 0.4974, 0.4412     
  };
  

}
