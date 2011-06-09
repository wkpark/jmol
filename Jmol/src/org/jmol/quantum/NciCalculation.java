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

import org.jmol.api.QuantumPlaneCalculationInterface;
import org.jmol.api.VolumeDataInterface;
import org.jmol.util.Eigen;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

import javax.vecmath.Point3f;

import java.util.List;
import java.util.BitSet;

/*
 * promolecular NCIPLOT implemented in Jmol 12.1.49
 *
 *   -- NCIPLOT promolecule calculation for reduced density.
 *      Plots reduced density mapped with ABS(rho)*SIGN(lambda2)
 *      where lambda2 is the middle eigenvalue of the Hessian matrix of
 *      promolecular electron density. Innovates a discrete SCF option not
 *      available in NCIPLOT itself.
 *
 *      default is "promolecular" approximation
 *
 *        isosurface NCI
 *
 *      DISCRETE SCF -- starting with standard CUBEGEN file
 *
 *        isosurface NCI "dens.cube"  
 *
 *      DISCRETE SCF -- starting with NCIPLOT's xx-nci-dens.cube file        
 *
 *        isosurface parameters [0 0 0 0 0.01] NCI "dens.cube"
 * 
 *      Extended to all volume file formats, including
 *            ABPS, CUBE, Jaguar, MRC, OMAP, CCP4, XPLOR  
 *
 *      Isosurface parameters [cutoff, p1, p2, p3, p4] NCI ...
 *
 *         -- p1 = 0(all, default), 1(intramolecular), 2(intermolecular)
 *         -- p2 = rhoPlot  (cutoff used to remove covalent density, default 0.07 for promolecular, 0.05 for SCF)
 *         -- p3 = rhoParam (fraction of total rho that defines intramolecular, default 0.95)
 *         -- p4 = dataScaling (default 1, but set to 0.01 to read back in NCIPLOT -dens.cube file)
 *
 *  references: 
 *
 *   "Revealing Noncovalent Interactions", 
 *   Erin R. Johnson, Shahar Keinan, Paula Mori-Sanchez, Julia Contreras-Garcia, Aron J. Cohen, and Weitao Yang, 
 *   J. Am. Chem. Soc., 2010, 132, 6498-6506. email to julia.contreras@duke.edu
 * 
 *   "NCIPLOT: A Program for Plotting Noncovalent Interaction Regions"
 *   Julia Contreras-García, Erin R. Johnson, Shahar Keinan, Robin Chaudret, Jean-Philip Piquemal, David N. Beratan, and Weitao Yang,
 *   J. of Chemical Theory and Computation, 2011, 7, 625-632
 *
 * Bob Hanson hansonr@stolaf.edu 6/8/2011
 * 
 */

/*
 * NOTE -- THIS CLASS IS INSTANTIATED USING Interface.getOptionInterface
 * NOT DIRECTLY -- FOR MODULARIZATION. NEVER USE THE CONSTRUCTOR DIRECTLY!
 * 
 */

public class NciCalculation extends QuantumCalculation implements
    QuantumPlaneCalculationInterface {
  
  private boolean havePoints;
  private boolean isReducedDensity;
  private double DEFAULT_RHOPLOT_SCF = 0.05;
  private double DEFAULT_RHOPLOT_PRO = 0.07;
  private double DEFAULT_RHOPARAM = 0.95;
  private double rhoPlot;  // only rho <= this number plotted
  private double rhoParam; // fractional rho cutoff defining intramolecular 
  private final static int TYPE_ALL = 0;
  private final static int TYPE_INTRA = 1;
  private final static int TYPE_INTER = 2;
  private final static int TYPE_LIGAND = 3;
  
  private final static double NO_VALUE = 100;
  
  private float dataScaling = 1; // set to 0.01 to read NCIPLOT-generated density files
  
  public float getNoValue() {
    return (float) NO_VALUE;
  }
  
  public NciCalculation() {
  }

  private Eigen eigen;
  private BitSet[] bsMolecules;
  private double[] rhoMolecules;
  private int type;
  private int nMolecules;
  
  public boolean setupCalculation(VolumeDataInterface volumeData,
                                  BitSet bsSelected, String calculationType,
                                  Point3f[] atomCoordAngstroms,
                                  int firstAtomOffset, List<int[]> shells,
                                  float[][] gaussians, int[][] dfCoefMaps,
                                  Object slaters, float[] moCoefficients,
                                  float[] linearCombination, float[][] coefs,
                                  float[] nuclearCharges, boolean isDensityOnly,
                                  Point3f[] points, float[] parameters) {
    boolean isPromolecular = (atomCoordAngstroms != null);    
    havePoints = (points != null);
    isReducedDensity = isDensityOnly;
    if (parameters != null)
      Logger.info("NCI calculation parameters = " + Escape.escape(parameters));
    // parameters[0] is the cutoff.
    type = (int) getParameter(parameters, 1, 0, "type");
    rhoPlot = getParameter(parameters, 2, (isPromolecular ? DEFAULT_RHOPLOT_PRO : DEFAULT_RHOPLOT_SCF), "rhoPlot");
    rhoParam = getParameter(parameters, 3, DEFAULT_RHOPARAM, "rhoParam");
    dataScaling = (float) getParameter(parameters, 4, 1, "dataScaling");
    String stype;
    switch (type) {
    default:
      type = 0;
      stype = "all";
      break;
    case TYPE_INTRA:
      stype = "intramolecular";
      break;
    case TYPE_INTER:
      stype = "intermolecular";
      break;
    case TYPE_LIGAND:
      stype = "ligand";
      break;
    }
    
    Logger.info("NCI calculation type = " + (isPromolecular ? "promolecular " : "SCF(CUBE) ") + stype);
    
    this.firstAtomOffset = firstAtomOffset;
    voxelData = volumeData.getVoxelData();
    countsXYZ = volumeData.getVoxelCounts();
    initialize(countsXYZ[0], countsXYZ[1], countsXYZ[2], points);
    if (havePoints) {
      xMin = yMin = zMin = 0;
      xMax = yMax = zMax = points.length;
    }

    setupCoordinates(volumeData.getOriginFloat(), volumeData
        .getVolumetricVectorLengths(), bsSelected, atomCoordAngstroms, points,
        true);
    
    if (isPromolecular) {
      nMolecules = 0;
      int firstMolecule = Integer.MAX_VALUE;
      for (int i = qmAtoms.length; --i >= 0;) {
        // must ignore heavier elements
        if (qmAtoms[i].znuc < 1) {
          qmAtoms[i] = null;
        } else if (qmAtoms[i].znuc > 18) {
          qmAtoms[i].znuc = 18; // just max it out at argon?
          Logger.info("NCI calculation just setting nuclear charge for "
              + qmAtoms[i].atom + " to 18 (argon)");
        }
        if (type != TYPE_ALL) {
          int iMolecule = qmAtoms[i].iMolecule = qmAtoms[i].atom
              .getMoleculeNumber();
          nMolecules = Math.max(nMolecules, iMolecule);
          firstMolecule = Math.min(firstMolecule, iMolecule);
        }
      }
      if (nMolecules == 0)
        nMolecules = 1;
      else
        nMolecules = nMolecules - firstMolecule + 1;
      if (type != TYPE_ALL) {
        bsMolecules = new BitSet[nMolecules];
        for (int i = qmAtoms.length; --i >= 0;) {
          int j = qmAtoms[i].iMolecule = qmAtoms[i].iMolecule - firstMolecule;
          if (bsMolecules[j] == null)
            bsMolecules[j] = new BitSet();
          bsMolecules[j].set(i);
        }
        for (int i = 0; i < nMolecules; i++)
          if (bsMolecules[i] != null)
            Logger.info("Molecule " + (i + 1) + ": " + bsMolecules[i]);
        rhoMolecules = new double[nMolecules];
      }
    }
    if (!isReducedDensity)
      initializeEigen();
    doDebug = (Logger.debugging);
    return true;
  }  
  
  private static double getParameter(float[] parameters, int i, double def, String name) {
    double param = (parameters == null || parameters.length < i + 1 ? 0 : parameters[i]);
    if (param == 0)
      param = def;
    Logger.info("NCI calculation parameters[" + i + "] (" + name + ") = " + param);
    return param;
  }

  public void createCube() {
    setXYZBohr(points);
    process();
  }  

  @Override
  protected void initializeOnePoint() {
    // called by surface mapper because
    // we have set "hasColorData" in reader
    if (eigen == null)
      initializeEigen();
    isReducedDensity = false;
    super.initializeOnePoint();
  }

  private void initializeEigen() {
    eigen = new Eigen(3);
    hess = new double[3][3];
  }

  private static double c = (1 / (2 * Math.pow(3 * Math.PI * Math.PI, 1/3d)));
  private static double rpower = -4/3d;
  private double[][] hess;
  private double grad, gxTemp, gyTemp, gzTemp, gxxTemp, gyyTemp, gzzTemp, gxyTemp, gyzTemp, gxzTemp;
  
  public void getPlane(int ix, float[] yzPlane) {
    isReducedDensity = true;
    initialize(countsXYZ[0], countsXYZ[1], countsXYZ[2], null);
    setXYZBohr(null);
    for (int iy = 0, i = 0; iy < countsXYZ[1]; iy++) {
      for (int iz = 0; iz < countsXYZ[2]; i++, iz++)
        yzPlane[i] = getValue(process(ix, iy, iz), isReducedDensity);
    }
  }

  @Override
  protected void process() {
    for (int ix = xMax; --ix >= xMin;) {
      for (int iy = yMin; iy < yMax; iy++) {
        vd = voxelData[ix][(havePoints ? 0 : iy)];
        for (int iz = zMin; iz < zMax; iz++)
          vd[(havePoints ? 0 : iz)] = getValue(process(ix, iy, iz), isReducedDensity);
      }
    }
/*    for (int ix = xMax; --ix >= xMin;) {
      for (int iy = yMax; --iy >= yMin;) {
        vd = voxelData[ix][(havePoints ? 0 : iy)];
        for (int iz = zMax; --iz >= zMin;)
          vd[(havePoints ? 0 : iz)] = getValue(process(ix, iy, iz), isReducedDensity);
      }
    }
*/  }
  
  private float getValue(double rho, boolean isReducedDensity) {
    double s;
    if (rho == NO_VALUE) {
      s = NO_VALUE;
    } else if (isReducedDensity) {
      s = c * grad * Math.pow(rho, rpower);
    } else {
      hess[0][0] = gxxTemp;
      hess[1][0] = hess[0][1] = gxyTemp;
      hess[2][0] = hess[0][2] = gxzTemp;
      hess[1][1] = gyyTemp;
      hess[1][2] = hess[2][1] = gyzTemp;
      hess[2][2] = gzzTemp;
      eigen.calc(hess);
      double lambda2 = eigen.getEigenvalues()[1];
      s = (lambda2 < 0 ? -rho : rho);
    }
    return (float) s;
  }

  private double process(int ix, int iy, int iz) {
    double rho = 0;
    if (isReducedDensity) {
      gxTemp = gyTemp = gzTemp = 0;
      if (type != TYPE_ALL)
        for (int i = nMolecules; --i >= 0;)
          rhoMolecules[i] = 0;
    } else {
      gxxTemp = gyyTemp = gzzTemp = gxyTemp = gyzTemp = gxzTemp = 0;
    }
    for (int i = qmAtoms.length; --i >= 0;) {
      int znuc = qmAtoms[i].znuc;
      double z1 = zeta1[znuc];
      double z2 = zeta2[znuc];
      double z3 = zeta3[znuc];
      double x = xBohr[ix] - qmAtoms[i].x;
      double y = yBohr[iy] - qmAtoms[i].y;
      double z = zBohr[iz] - qmAtoms[i].z;
      double r = Math.sqrt(x * x + y * y + z * z);
      double ce1 = coef1[znuc] * Math.exp(-r / z1);
      double ce2 = coef2[znuc] * Math.exp(-r / z2);
      double ce3 = coef3[znuc] * Math.exp(-r / z3);
      double rhoAtom = ce1 + ce2 + ce3;
      rho += rhoAtom;
      // Don't continue to more atoms if the density is already high. 
      // We couldn't do this if we were intending to write the density cube, 
      // but we aren't doing that.
      if (rho > rhoPlot)
        return NO_VALUE; 
      // Some efficiencies introduced here vs. NCIPLOT's FORTRAN code
      // just to minimize number of exponentials and multiplications, mostly.
      double fac1r = (ce1 / z1 + ce2 / z2 + ce3 / z3) / r;
      if (isReducedDensity) {
        if (type != TYPE_ALL)
          rhoMolecules[qmAtoms[i].iMolecule] += rhoAtom;
        gxTemp -= fac1r * x;
        gyTemp -= fac1r * y;
        gzTemp -= fac1r * z;
      } else {
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
    if (isReducedDensity) {
      // Check to see if this is intermolecular or intramolecular.
      // Note that we can do intra (type=1) or inter (type=2) here.
      switch (type) {
      case TYPE_INTRA: // 
      case TYPE_INTER:
        boolean isIntra = false;
        double rhocut2 = rhoParam * rho;
        for (int i = 0; i < nMolecules; i++)
          if (rhoMolecules[i] >= rhocut2) {
            isIntra = true;
            break;
          }
        if ((type == TYPE_INTRA) != isIntra)
          return NO_VALUE;
        break;
      case TYPE_LIGAND:
        // ?? 
        break;
      default:
        break;
      }
      grad = Math.sqrt(gxTemp * gxTemp + gyTemp * gyTemp + gzTemp * gzTemp);

      //if (ix == 4 && iy < 10 && iz < 10)
        //System.out.println(ix + " " + iy + " " + iz + " rho " + rho + " grad " + grad);
    }
    return rho;
  }

  double test1;
  
  ///////////////////////// DISCRETE SCF METHODS /////////////////////

  private float[][] yzPlanesRaw;
  private int yzCount;

  /**
   * Raw file data planes are passed to us here from VolumeFileReader
   * 
   * @param planes 
   */
  public void setPlanes(float[][] planes) {
    yzPlanesRaw = planes;
    yzCount = nY * nZ;
  }
  
  private float[][] yzPlanesRho = new float[2][];

  /**
   * For reduced density only; coloring is done point by point.
   * 
   * @param plane
   *        an OUTPUT plane, to be filled here and used by MarchingCubes
   * 
   */
  public void calcPlane(float[] plane) {

    // (1) shift planes:

    yzPlanesRho[0] = yzPlanesRho[1];
    yzPlanesRho[1] = plane;

    // (2) assign input planes. We either process planes 0, 1, and 2, 
    //     (first time around) or 1, 2, and 3 (after that).

    int i0 = (yzPlanesRho[0] == null ? 0 : 1);
    float[] p0 = yzPlanesRaw[i0++];
    float[] p1 = yzPlanesRaw[i0++];
    float[] p2 = yzPlanesRaw[i0++];

    // (3) Make sure rho data is scaled appropriately and nonnegative.

    for (int i = (i0 == 4 ? 3 : 0); i < i0; i++)
      for (int j = 0; j < yzCount; j++)
        yzPlanesRaw[i][j] = Math.abs(yzPlanesRaw[i][j] * dataScaling);

    // (4) Calculate discrete reduced gradients. All edges are just assigned "no value"

    for (int y = 0, i = 0; y < nY; y++)
      for (int z = 0; z < nZ; z++, i++) {
        double rho = p1[i];
        if (rho == 0) {
          plane[i] = 0;
        } else if (rho > rhoPlot || y == 0 || y == nY - 1 || z == 0 || z == nZ - 1) {
          plane[i] = (float) NO_VALUE;
        } else {
          gxTemp = (p2[i] - p0[i]) / (2 * stepBohr[0]);
          gyTemp = (p1[i + nZ] - p1[i - nZ]) / (2 * stepBohr[1]);
          gzTemp = (p1[i + 1] - p1[i - 1]) / (2 * stepBohr[2]);
          grad = Math.sqrt(gxTemp * gxTemp + gyTemp * gyTemp + gzTemp * gzTemp);
          plane[i] = getValue(rho, true);
        }
      }
  }

  /**
   * Passing the grid points of the two ends of an edge and a fraction
   * to this method returns the value at a triangle point. This way we
   * do not need to calculate this for EVERY point on the grid, only the
   * ones that are part of the surface.  
   * 
   * @param vA 
   * @param vB 
   * @param f 
   * @return value at point f-way between vA and vB
   * 
   */
  public float process(int vA, int vB, float f) {
    // first we need to know what planes we are working with.
    double valueA = getPlaneValue(vA);
    double valueB = getPlaneValue(vB);
    return (float) (valueA + f * (valueB - valueA));
  }

  /**
   * We always have four raw planes in hand; we just need to know which
   * three to use in any particular case. 
   * 
   * @param vA
   * @return    value of sign(lambda2)*rho at this grid point
   */
  private double getPlaneValue(int vA) {
    int i = (vA % yzCount);
    int x = vA / yzCount;
    int y = i / nZ;
    int z = i % nZ;
    if (x == 0 || x == nX - 1
        || y == 0 || y == nY - 1
        || z == 0 || z == nZ - 1)
      return Double.NaN;
    int iPlane = x % 2;
    float[] p0 = yzPlanesRaw[iPlane++];
    float[] p1 = yzPlanesRaw[iPlane++];
    float[] p2 = yzPlanesRaw[iPlane++];
    double rho = p1[i];
    // Shouldn't be possible to be too large, because the MarchingCubes algorithm
    // can't generate a value -- but if it WERE possible, getValue() would set it
    // to NO_VALUE anyway.
    
    if (rho > rhoPlot)  
      return NO_VALUE; 
    float dx = stepBohr[0];
    float dy = stepBohr[1];
    float dz = stepBohr[2];

    // Using explicit discrete second partial derivatives here. 
    // Worked these out myself; just seemed right! Note that
    // d^2rho/dxdy does = d^2rho/dydx. The factors of 1/4 are there
    // because in those nondiagonal cases we are spanning two edges
    // in each direction. Really [(p2.. - p2..)/2dy - (p0.. - p0..)/2dy]/2dx
    // That is, the differences along x of the differences along y.
    
    gxxTemp = (p2[i] - 2 * rho + p0[i]) / (dx * dx);
    gyyTemp = (p1[i + nZ] - 2 * rho + p1[i - nZ]) / (dy * dy);
    gzzTemp = (p1[i + 1] - 2 * rho + p1[i - 1]) / (dz * dz);
    gxyTemp = ((p2[i + nZ] - p2[i - nZ]) - (p0[i + nZ] - p0[i - nZ])) / (4 * dx * dy);
    gxzTemp = ((p2[i + 1] - p2[i - 1]) - (p0[i + 1] - p0[i - 1])) / (4 * dx * dz);
    gyzTemp = ((p1[i + nZ + 1] - p1[i - nZ + 1]) - (p1[i + nZ - 1] - p1[i - nZ - 1])) / (4 * dy * dz);
    return getValue(rho, false);
  }

  public void calculateElectronDensity(float[] nuclearCharges) {
    
    // Required for QuantumCalculationInterface
    
    // I guess we could do this, but really all you need is the original raw data in this case.
  }
  
  /////////////////////////// promolecular data ///////////////////////////
  //// Source: NCIPLOT 1.0
  ///
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
