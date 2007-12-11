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

import org.jmol.api.MOCalculationInterface;
import org.jmol.jvxl.data.VolumeData;
import org.jmol.jvxl.readers.Parameters;
import org.jmol.util.Logger;
import javax.vecmath.Point3f;
import java.util.Vector;
import java.util.Hashtable;
import java.util.BitSet;

/*
 * See J. Computational Chemistry, vol 7, p 359, 1986.
 * thanks go to Won Kyu Park, wkpark@chem.skku.ac.kr, 
 * jmol-developers list communication "JMOL AND CALCULATED ORBITALS !!!!"
 * and his http://chem.skku.ac.kr/~wkpark/chem/mocube.f
 * based on PSI88 http://www.ccl.net/cca/software/SOURCES/FORTRAN/psi88/index.shtml
 * http://www.ccl.net/cca/software/SOURCES/FORTRAN/psi88/src/psi1.f
 * 
 * While we are not exactly copying this code, I include here the information from that
 * FORTRAN as acknowledgment of the source of the algorithmic idea to use single 
 * row arrays to reduce the number of calculations.
 *  
 * Slater functions provided by JR Schmidt and Will Polik. Many thanks!
 * 
 * Spherical functions by Matthew Zwier <mczwier@gmail.com>
 * 
 * A neat trick here is using Java Point3f. null atoms allow selective removal of
 * their contribution to the MO. Maybe a first time this has ever been done?
 * 
 * Bob Hanson hansonr@stolaf.edu 7/3/06
 * 
 C
 C      DANIEL L. SEVERANCE
 C      WILLIAM L. JORGENSEN
 C      DEPARTMENT OF CHEMISTRY
 C      YALE UNIVERSITY
 C      NEW HAVEN, CT 06511
 C
 C      THIS CODE DERIVED FROM THE PSI1 PORTION OF THE ORIGINAL PSI77
 C      PROGRAM WRITTEN BY WILLIAM L. JORGENSEN, PURDUE.
 C      IT HAS BEEN REWRITTEN TO ADD SPEED AND BASIS FUNCTIONS. DLS
 C
 C      THE CONTOURING CODE HAS BEEN MOVED TO A SEPARATE PROGRAM TO ALLOW
 C      MULTIPLE CONTOURS TO BE PLOTTED WITHOUT RECOMPUTING THE
 C      ORBITAL VALUE MATRIX.
 C
 C Redistribution and use in source and binary forms are permitted
 C provided that the above paragraphs and this one are duplicated in 
 C all such forms and that any documentation, advertising materials,
 C and other materials related to such distribution and use acknowledge 
 C that the software was developed by Daniel Severance at Purdue University
 C The name of the University or Daniel Severance may not be used to endorse 
 C or promote products derived from this software without specific prior 
 C written permission.  The authors are now at Yale University.
 C THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR
 C IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
 C WARRANTIES OF MERCHANTIBILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 */

public class MOCalculation extends QuantumCalculation implements MOCalculationInterface {

  private static int MAX_GRID = Parameters.MO_MAX_GRID;
  // slater coefficients in Bohr
  float[] CX = new float[MAX_GRID];
  float[] CY = new float[MAX_GRID];
  float[] CZ = new float[MAX_GRID];

  // d-orbital partial coefficients in Bohr
  float[] DXY = new float[MAX_GRID];
  float[] DXZ = new float[MAX_GRID];
  float[] DYZ = new float[MAX_GRID];

  // exp(-alpha x^2...)
  float[] EX = new float[MAX_GRID];
  float[] EY = new float[MAX_GRID];
  float[] EZ = new float[MAX_GRID];

  String calculationType;
  Vector shells;
  float[][] gaussians;
  //Hashtable aoOrdersDF;
  int[][] slaterInfo;
  float[][] slaterData;
  float[] moCoefficients;
  int moCoeff;
  int gaussianPtr;
  int firstAtomOffset;
  
  private final static String[][] shellOrder = { 
    {"S"},
    {"X", "Y", "Z"},
    {"S", "X", "Y", "Z"},
    {"XX", "YY", "ZZ", "XY", "XZ", "YZ"},
    {"d0", "d1+", "d1-", "d2+", "d2-"},
    {"XXX", "YYY", "ZZZ", "XYY", "XXY", "XXZ", "XZZ", "YZZ", "YYZ", "XYZ"},
    {"f0", "f1+", "f1-", "f2+", "f2-", "f3+", "f3-"}
  };

  final public static int SHELL_S = 0;
  final public static int SHELL_P = 1;
  final public static int SHELL_SP = 2;
  final public static int SHELL_L = 2;
  
  // these next in cartesian/spherical pairs:
  
  final public static int SHELL_D_CARTESIAN = 3;
  final public static int SHELL_D_SPHERICAL = 4;
  final public static int SHELL_F_CARTESIAN = 5;
  final public static int SHELL_F_SPHERICAL = 6;

  final private static String[] quantumShellTags = {"S", "P", "SP", "L", 
    "D", "5D", "F", "7F"};
  
  final private static int[] quantumShellIDs = {
    SHELL_S, SHELL_P, SHELL_SP, SHELL_L, 
    SHELL_D_CARTESIAN, SHELL_D_SPHERICAL,
    SHELL_F_CARTESIAN, SHELL_F_SPHERICAL
  };
  
  final public static int getQuantumShellTagID(String tag) {
    for (int i = quantumShellTags.length; --i >= 0;)
      if (tag.equals(quantumShellTags[i]))
        return quantumShellIDs[i];
    return -1;
  }

  final public static int getQuantumShellTagIDSpherical(String tag) {
    final int tagID = getQuantumShellTagID(tag);
    return tagID + (tagID < SHELL_D_CARTESIAN ? 0 : tagID % 2);
  }

  final public static String getQuantumShellTag(int shell) {
    for (int i = quantumShellTags.length; --i >= 0;)
      if (shell == quantumShellIDs[i])
        return quantumShellTags[i];
    return "" + shell;
  }

  public MOCalculation() {
  }
    
  public void calculate(VolumeData volumeData, BitSet bsSelected, String calculationType, Point3f[] atomCoordAngstroms,
      int firstAtomOffset, Vector shells, float[][] gaussians, Hashtable aoOrdersDF,
      int[][] slaterInfo, float[][] slaterData, float[] moCoefficients) {
    this.calculationType = calculationType;
    this.atomCoordAngstroms = atomCoordAngstroms;
    this.firstAtomOffset = firstAtomOffset;
    this.shells = shells;
    this.gaussians = gaussians;
    //this.aoOrdersDF = aoOrdersDF;
    this.slaterInfo = slaterInfo;
    this.slaterData = slaterData;
    this.moCoefficients = moCoefficients;
    initialize(MAX_GRID);
    setVolume(volumeData, bsSelected);
    atomIndex = firstAtomOffset -1;
    doDebug = (Logger.debugging);
    if (slaterInfo != null)
      createSlaterCube();
    else
      createGaussianCube();
  }

  private void createSlaterCube() {    
    moCoeff = 0;
    // each STO shell is the combination of one or more gaussians
    int nSlaters = slaterInfo.length;
    for (int i = 0; i < nSlaters; i++) {
      processSlater(i);
    }
  }

  private void createGaussianCube() {    
    if (!checkCalculationType())
      return;
    int nShells = shells.size();
    // each STO shell is the combination of one or more gaussians
    moCoeff = 0;
    for (int i = 0; i < nShells; i++) {
      processShell(i);
      if (doDebug)
        Logger.debug("createGaussianCube shell=" + i + " moCoeff=" + moCoeff
            + "/" + moCoefficients.length);
    }
  }

  private boolean checkCalculationType() {
    if (calculationType == null) {
      Logger
      .warn("calculation type not identified -- continuing");
      return true;
    }
    /*if (calculationType.indexOf("5D") >= 0) {
     Logger
          .error("QuantumCalculation.checkCalculationType: can't read 5D basis sets yet: "
              + calculationType + " -- exit");
      return false;
    }*/
    if (calculationType.indexOf("+") >= 0 || calculationType.indexOf("*") >= 0) {
      Logger
          .warn("polarization/diffuse wavefunctions have not been tested fully: "
              + calculationType + " -- continuing");
    }
    if (calculationType.indexOf("?") >= 0) {
      Logger
          .warn("unknown calculation type may not render correctly -- continuing");
    } else {
      Logger.info("calculation type: " + calculationType + " OK.");
    }
    return true;
  }

  private void processShell(int iShell) {
    int lastAtom = atomIndex;
    int[] shell = (int[]) shells.get(iShell);
    atomIndex = shell[0] + firstAtomOffset;
    int basisType = shell[1];
    gaussianPtr = shell[2];
    int nGaussians = shell[3];
    
    if (doDebug)
      Logger.debug(  "processShell: " + iShell 
                   + " type=" + getQuantumShellTag(basisType) 
                   + " nGaussians=" + nGaussians 
                   + " atom=" + atomIndex
                   );
    if (atomIndex != lastAtom && atomCoordBohr[atomIndex] != null) {
      //Logger.("processSTO center " + atomCoordBohr[atomIndex]);
      float x = atomCoordBohr[atomIndex].x;
      float y = atomCoordBohr[atomIndex].y;
      float z = atomCoordBohr[atomIndex].z;
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
    }
    switch(basisType) {
    case SHELL_S:
      addDataS(nGaussians);
      break;
    case SHELL_P:
      addDataP(nGaussians);
      break;
    case SHELL_SP:
      addDataSP(nGaussians);
      break;
    case SHELL_D_CARTESIAN:
      addData6D(nGaussians);
      break;
    case SHELL_D_SPHERICAL:
      addData5D(nGaussians);
      break;
    case SHELL_F_CARTESIAN:
      addData10F(nGaussians);
      break;
    case SHELL_F_SPHERICAL:
      addData7F(nGaussians);
      break;
    default:
      Logger.warn(" Unsupported basis type for atomno=" + (atomIndex + 1) + " -- use \"set loglevel 5\" to debug.");
      return;
    }
  }

  private void addDataS(int nGaussians) {
    if (!atomSet.get(atomIndex)) {
      moCoeff++;
      return;
    }
    if (doDebug)
      dumpInfo(nGaussians, "S ");

    int moCoeff0 = moCoeff;
    // all gaussians of a set use the same MO coefficient
    // so we just reset each time, then move on
    setMinMax();
    for (int ig = 0; ig < nGaussians; ig++) {
      moCoeff = moCoeff0;
      float alpha = gaussians[gaussianPtr + ig][0];
      float c1 = gaussians[gaussianPtr + ig][1];
      // (2 alpha^3/pi^3)^0.25 exp(-alpha r^2)
      float a = c1 * (float) Math.pow(alpha, 0.75) * 0.712705470f;
      a *= moCoefficients[moCoeff++];
      // the coefficients are all included with the X factor here

      for (int i = xMax; --i >= xMin;) {
        EX[i] = a * (float) Math.exp(-X2[i] * alpha);
      }
      for (int i = yMax; --i >= yMin;) {
        EY[i] = (float) Math.exp(-Y2[i] * alpha);
      }
      for (int i = zMax; --i >= zMin;) {
        EZ[i] = (float) Math.exp(-Z2[i] * alpha);
      }

      for (int ix = xMax; --ix >= xMin;)
        for (int iy = yMax; --iy >= yMin;)
          for (int iz = zMax; --iz >= zMin;)
            voxelData[ix][iy][iz] += EX[ix] * EY[iy] * EZ[iz];
    }
  }

  private void addDataP(int nGaussians) {
    if (!atomSet.get(atomIndex)) {
      moCoeff += 3;
      return;
    }
    if (doDebug)
      dumpInfo(nGaussians, "X Y Z ");
    setMinMax();
    int moCoeff0 = moCoeff;
    for (int ig = 0; ig < nGaussians; ig++) {
      moCoeff = moCoeff0;
      float alpha = gaussians[gaussianPtr + ig][0];
      float c1 = gaussians[gaussianPtr + ig][1];
      // (128 alpha^5/pi^3)^0.25 [x|y|z]exp(-alpha r^2)
      float a = c1 * (float) Math.pow(alpha, 1.25) * 1.42541094f;
      float ax = a * moCoefficients[moCoeff++];
      float ay = a * moCoefficients[moCoeff++];
      float az = a * moCoefficients[moCoeff++];
      calcSP(alpha, 0, ax, ay, az);
    }
  }

  private void addDataSP(int nGaussians) {
    if (!atomSet.get(atomIndex)) {
      moCoeff += 4;
      return;
    }
    if (doDebug)
      dumpInfo(nGaussians, "S X Y Z ");
    setMinMax();
    int moCoeff0 = moCoeff;
    for (int ig = 0; ig < nGaussians; ig++) {
      moCoeff = moCoeff0;
      float alpha = gaussians[gaussianPtr + ig][0];
      float c1 = gaussians[gaussianPtr + ig][1];
      float c2 = gaussians[gaussianPtr + ig][2];
      float a1 = c1 * (float) Math.pow(alpha, 0.75) * 0.712705470f;
      float a2 = c2 * (float) Math.pow(alpha, 1.25) * 1.42541094f;
      // spartan uses format "1" for BOTH SP and P, which is fine, but then
      // when c1 = 0, there is no mo coefficient, of course. 
      float as = (c1 == 0 ? 0 : a1 * moCoefficients[moCoeff++]);
      float ax = a2 * moCoefficients[moCoeff++];
      float ay = a2 * moCoefficients[moCoeff++];
      float az = a2 * moCoefficients[moCoeff++];
      calcSP(alpha, as, ax, ay, az);
    }
  }

  private void setCE(float alpha, float as, float ax, float ay, float az) {
    for (int i = xMax; --i >= xMin;) {
      CX[i] = as + ax * X[i];
      EX[i] = (float) Math.exp(-X2[i] * alpha);
    }
    for (int i = yMax; --i >= yMin;) {
      CY[i] = ay * Y[i];
      EY[i] = (float) Math.exp(-Y2[i] * alpha);
    }
    for (int i = zMax; --i >= zMin;) {
      CZ[i] = az * Z[i];
      EZ[i] = (float) Math.exp(-Z2[i] * alpha);
    }
  }

  private void calcSP(float alpha, float as, float ax, float ay, float az) {
    setCE(alpha, as, ax, ay, az);
    for (int ix = xMax; --ix >= xMin;)
      for (int iy = yMax; --iy >= yMin;)
        for (int iz = zMax; --iz >= zMin;)
          voxelData[ix][iy][iz] += (CX[ix] + CY[iy] + CZ[iz]) * EX[ix] * EY[iy]
              * EZ[iz];
  }

  private final static float ROOT3 = 1.73205080756887729f;

  private void addData6D(int nGaussians) {
    //expects 6 orbitals in the order XX YY ZZ XY XZ YZ
    if (!atomSet.get(atomIndex)) {
      moCoeff += 6;
      return;
    }
    if (doDebug)
      dumpInfo(nGaussians, "XXYYZZXYXZYZ");
    setMinMax();
    int moCoeff0 = moCoeff;
    for (int ig = 0; ig < nGaussians; ig++) {
      moCoeff = moCoeff0;
      float alpha = gaussians[gaussianPtr + ig][0];
      float c1 = gaussians[gaussianPtr + ig][1];
      // xx|yy|zz: (2048 alpha^7/9pi^3)^0.25 [xx|yy|zz]exp(-alpha r^2)
      // xy|xz|yz: (2048 alpha^7/pi^3)^0.25 [xy|xz|yz]exp(-alpha r^2)
      float a = c1 * (float) Math.pow(alpha, 1.75) * 2.8508219178923f;
      float axx = a / ROOT3 * moCoefficients[moCoeff++];
      float ayy = a / ROOT3 * moCoefficients[moCoeff++];
      float azz = a / ROOT3 * moCoefficients[moCoeff++];
      float axy = a * moCoefficients[moCoeff++];
      float axz = a * moCoefficients[moCoeff++];
      float ayz = a * moCoefficients[moCoeff++];
      setCE(alpha, 0, axx, ayy, azz);

      for (int i = xMax; --i >= xMin;) {
        DXY[i] = axy * X[i];
        DXZ[i] = axz * X[i];
      }
      for (int i = yMax; --i >= yMin;) {
        DYZ[i] = ayz * Y[i];
      }
      for (int ix = xMax; --ix >= xMin;) {
        float axx_x2 = CX[ix] * X[ix];
        float axy_x = DXY[ix];
        float axz_x = DXZ[ix];
        for (int iy = yMax; --iy >= yMin;) {
          float axx_x2__ayy_y2__axy_xy = axx_x2 + (CY[iy] + axy_x) * Y[iy];
          float axz_x__ayz_y = axz_x + DYZ[iy];
          for (int iz = zMax; --iz >= zMin;)
            voxelData[ix][iy][iz] += (axx_x2__ayy_y2__axy_xy + (CZ[iz] + axz_x__ayz_y)
                * Z[iz])
                * EX[ix] * EY[iy] * EZ[iz];
          // giving (axx_x2 + ayy_y2 + azz_z2 + axy_xy + axz_xz + ayz_yz)e^-br2; 
        }
      }
    }
  }
  
  private void addData10F(int nGaussians) {
    // expects 10 orbitals in the order XXX, YYY, ZZZ, XYY, XXY, 
    //                                  XXZ, XZZ, YZZ, YYZ, XYZ
    if (!atomSet.get(atomIndex)) {
      moCoeff += 10;
      return;
    }
    if (doDebug)
      dumpInfo(nGaussians, SHELL_F_CARTESIAN);
    setMinMax();
    
    float alpha;
    float c1;
    float a;
    float x, y, z, xx, yy, zz;
    float axxx, ayyy, azzz, axyy, axxy, axxz, axzz, ayzz, ayyz, axyz;
    float cxxx, cyyy, czzz, cxyy, cxxy, cxxz, cxzz, cyzz, cyyz, cxyz;
    float Ex, Ey, Ez;
    
    /*
    Cartesian forms for f (l = 3) basis functions:
    Type         Normalization
    xxx          [(32768 * alpha^9) / (225 * pi^3))]^(1/4)
    xxy          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
    xxz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
    xyy          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
    xyz          [(32768 * alpha^9) / (1 * pi^3))]^(1/4)
    xzz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
    yyy          [(32768 * alpha^9) / (225 * pi^3))]^(1/4)
    yyz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
    yzz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
    zzz          [(32768 * alpha^9) / (225 * pi^3))]^(1/4)
    */
    
    final float norm1 = (float) Math.pow(32768.0 / (Math.PI*Math.PI* Math.PI), 
                                         0.25);
    final float norm3 = norm1 / (float) Math.sqrt(15);
    final float norm2 = norm1 / (float) Math.sqrt(3);
    
    for (int ig = nGaussians; --ig >= 0;) {  
      alpha = gaussians[gaussianPtr + ig][0];
      c1 = gaussians[gaussianPtr + ig][1];
      setCE(alpha, 0, 0, 0, 0);

      // common factor of contraction coefficient and alpha normalization 
      // factor; only call pow once per primitive
      a = c1 * (float) Math.pow(alpha, 2.25);
      
      axxx = a * norm3 * moCoefficients[moCoeff];
      ayyy = a * norm3 * moCoefficients[moCoeff+1];
      azzz = a * norm3 * moCoefficients[moCoeff+2];
      axyy = a * norm2 * moCoefficients[moCoeff+3];
      axxy = a * norm2 * moCoefficients[moCoeff+4];
      axxz = a * norm2 * moCoefficients[moCoeff+5];
      axzz = a * norm2 * moCoefficients[moCoeff+6];
      ayzz = a * norm2 * moCoefficients[moCoeff+7];
      ayyz = a * norm2 * moCoefficients[moCoeff+8];
      axyz = a * norm1 * moCoefficients[moCoeff+9];
    
      for (int ix = xMax; --ix >= xMin;) {
        x = X[ix];
        xx = x*x;
        
        Ex = EX[ix];
        cxxx = axxx * xx*x;
        
        for (int iy = yMax; --iy >= yMin;) {
          y = Y[iy];
          yy = y*y;
          Ey = EY[iy];
          cyyy = ayyy * yy*y;
          cxxy = axxy * xx*y;
          cxyy = axyy * x*yy;
          
          for (int iz = zMax; --iz >= zMin;) {
            z = Z[iz];
            zz = z*z;
            Ez = EZ[iz];
            
            czzz = azzz * zz*z;
            cxxz = axxz * xx*z;
            cxzz = axzz * x*zz;
            cyyz = ayyz * yy*z;
            cyzz = ayzz * y*zz;
            cxyz = axyz * x*y*z;

            voxelData[ix][iy][iz] += Ex * Ey * Ez
                                     * ( cxxx + cyyy + czzz
                                       + cxyy + cxxy + cxxz
                                       + cxzz + cyzz + cyyz + cxyz);
            }
          }
        }
    }
    moCoeff += 10;
  }
  
  private void addData5D(int nGaussians) {
    // expects 5 real orbitals in the order d0, d+1, d-1, d+2, d-2
    // (i.e. dz^2, dxz, dyz, dx^2-y^2, dxy)
    // To avoid actually having to use spherical harmonics, we use 
    // linear combinations of Cartesian harmonics.  

    // For conversions between spherical and Cartesian gaussians, see
    // "Trasnformation Between Cartesian and Pure Spherical Harmonic Gaussians",
    // Schelgel and Frisch, Int. J. Quant. Chem 54, 83-87, 1995
    
    if (!atomSet.get(atomIndex)) {
      moCoeff += 5;
      return;
    }
    if (doDebug)
      dumpInfo(nGaussians, SHELL_D_SPHERICAL);
    
    setMinMax();
    
    float alpha, c1, a;
    float x, y, z;
    float cxx, cyy, czz, cxy, cxz, cyz;
    float ad0, ad1p, ad1n, ad2p, ad2n;    
    float Ex, Ey, Ez;

    /*
    Cartesian forms for d (l = 2) basis functions:
    Type         Normalization
    xx           [(2048 * alpha^7) / (9 * pi^3))]^(1/4)
    xy           [(2048 * alpha^7) / (1 * pi^3))]^(1/4)
    xz           [(2048 * alpha^7) / (1 * pi^3))]^(1/4)
    yy           [(2048 * alpha^7) / (9 * pi^3))]^(1/4)
    yz           [(2048 * alpha^7) / (1 * pi^3))]^(1/4)
    zz           [(2048 * alpha^7) / (9 * pi^3))]^(1/4)
     */
    
    final float norm1 = (float)Math.pow(2048.0/(Math.PI*Math.PI*Math.PI),0.25);
    final float norm2 = norm1 / (float) Math.sqrt(3);
    
    // Normalization constant that shows up for dx^2-y^2
    final float root34 = (float) Math.sqrt(3/4);   
    
    for (int ig = nGaussians; --ig >= 0;) {  
      alpha = gaussians[gaussianPtr + ig][0];
      c1 = gaussians[gaussianPtr + ig][1]; 
      a = c1 * (float) Math.pow(alpha, 1.75);
      
      ad0  = a * moCoefficients[moCoeff  ];
      ad1p = a * moCoefficients[moCoeff+1];
      ad1n = a * moCoefficients[moCoeff+2];
      ad2p = a * moCoefficients[moCoeff+3];
      ad2n = a * moCoefficients[moCoeff+4];
      
      setCE(alpha, 0, 0, 0, 0);
    
      for (int ix = xMax; --ix >= xMin;) {
        x = X[ix];
        Ex = EX[ix];
        cxx = norm2 * x*x;
        
        for (int iy = yMax; --iy >= yMin;) {
          y = Y[iy];
          Ey = EY[iy];
          
          cyy = norm2 * y*y;
          cxy = norm1 * x*y;
          
          for (int iz = zMax; --iz >= zMin;) {
            z = Z[iz];
            Ez = EZ[iz];

            czz = norm2 * z*z;
            cxz = norm1 * x*z;
            cyz = norm1 * y*z;            
            
            voxelData[ix][iy][iz] += Ex*Ey*Ez
                                    *( ad0  * (czz - 0.5*(cxx + cyy))
                                     + ad1p * cxz
                                     + ad1n * cyz
                                     + ad2p * root34 * (cxx - cyy)
                                     + ad2n * cxy);
          }
        }
      }
    } 
    moCoeff += 5;
  }

  private void addData7F(int nGaussians) {
    // expects 7 real orbitals in the order f0, f+1, f-1, f+2, f-2, f+3, f-3
    
    if (!atomSet.get(atomIndex)) {
      moCoeff += 7;
      return;
    }
    
    if (doDebug)
      dumpInfo(nGaussians, SHELL_F_SPHERICAL);
    
    setMinMax();
    
    float alpha, c1, a;
    float x, y, z, xx, yy, zz;
    float cxxx, cyyy, czzz, cxyy, cxxy, cxxz, cxzz, cyzz, cyyz, cxyz;
    float af0, af1p, af1n, af2p, af2n, af3p, af3n;
    float f0, f1p, f1n, f2p, f2n, f3p, f3n;
    float Ex, Ey, Ez;
    
    /*
      Cartesian forms for f (l = 3) basis functions:
      Type         Normalization
      xxx          [(32768 * alpha^9) / (225 * pi^3))]^(1/4)
      xxy          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
      xxz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
      xyy          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
      xyz          [(32768 * alpha^9) / (1 * pi^3))]^(1/4)
      xzz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
      yyy          [(32768 * alpha^9) / (225 * pi^3))]^(1/4)
      yyz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
      yzz          [(32768 * alpha^9) / (9 * pi^3))]^(1/4)
      zzz          [(32768 * alpha^9) / (225 * pi^3))]^(1/4)
    */
    
    
    final float norm1 = (float) Math.pow(32768.0 / (Math.PI*Math.PI*Math.PI),
                                         0.25);
    final float norm2 = norm1 / (float) Math.sqrt(3);
    final float norm3 = norm1 / (float) Math.sqrt(15);
    
    // Linear combination coefficients for the various Cartesian gaussians
    final float c0_xxz_yyz = (float) (3.0 / (2.0 * Math.sqrt(5)));
    
    final float c1p_xzz = (float) Math.sqrt(6.0/5.0);
    final float c1p_xxx = (float) Math.sqrt(3.0/8.0);
    final float c1p_xyy = (float) Math.sqrt(3.0/40.0);
    final float c1n_yzz = c1p_xzz;
    final float c1n_yyy = c1p_xxx;
    final float c1n_xxy = c1p_xyy;
    
    final float c2p_xxz_yyz = (float) Math.sqrt(3.0/4.0);
    
    final float c3p_xxx = (float) Math.sqrt(5.0/8.0);
    final float c3p_xyy = 0.75f * (float) Math.sqrt(2);
    final float c3n_yyy = c3p_xxx;
    final float c3n_xxy = c3p_xyy;
    
    for (int ig = nGaussians; --ig >=0;) {
      alpha = gaussians[gaussianPtr + ig][0];
      c1 = gaussians[gaussianPtr + ig][1];
      a = c1 * (float) Math.pow(alpha, 2.25);
      
      af0  = a * moCoefficients[moCoeff];
      af1p = a * moCoefficients[moCoeff+1];
      af1n = a * moCoefficients[moCoeff+2];
      af2p = a * moCoefficients[moCoeff+3];
      af2n = a * moCoefficients[moCoeff+4];
      af3p = a * moCoefficients[moCoeff+5];
      af3n = a * moCoefficients[moCoeff+6];
      
      setCE(alpha, 0, 0, 0, 0);
      
      for (int ix = xMax; --ix >= xMin;) {
        x = X[ix];
        xx = x*x;
        Ex = EX[ix];
        
        cxxx = norm3 * x*xx;        
        
        for (int iy = yMax; --iy >= yMin;) {
          y = Y[iy];
          yy = y*y;
          Ey = EY[iy];
          
          cyyy = norm3 * y*yy;
          cxyy = norm2 * x*yy;
          cxxy = norm2 * xx*y;
          
          for (int iz = zMax; --iz >= zMin;) {
            z = Z[iz];
            zz = z*z;
            Ez = EZ[iz];
                        
            czzz = norm3 * z*zz;
            cxxz = norm2 * xx*z;
            cxzz = norm2 * x*zz;
            cyyz = norm2 * yy*z;
            cyzz = norm2 * y*zz;
            cxyz = norm1 * x*y*z;

            f0  = af0  * (czzz - c0_xxz_yyz * (cxxz + cyyz));
            f1p = af1p * (c1p_xzz * cxzz - c1p_xxx * cxxx - c1p_xyy * cxyy);
            f1n = af1n * (c1n_yzz * cyzz - c1n_yyy * cyyy - c1n_xxy * cxxy);
            f2p = af2p * (c2p_xxz_yyz * (cxxz - cyyz));
            f2n = af2n * cxyz;
            f3p = af3p * (c3p_xxx * cxxx - c3p_xyy * cxyy);
            f3n = af3n * (-c3n_yyy * cyyy + c3n_xxy * cxxy);
            
            voxelData[ix][iy][iz] += Ex*Ey*Ez
                                    *( f0 + f1p + f1n + f2p +f2n + f3p + f3n); 

          }
        }
      }
    }
    moCoeff += 7;
  }

  
  private void processSlater(int slaterIndex) {
    /*
     * We have two data structures for each slater, using the WebMO format: 
     * 
     * int[] slaterInfo[] = {iatom, a, b, c, d}
     * float[] slaterData[] = {zeta, coef}
     * 
     * where
     * 
     *  psi = (coef)(x^a)(y^b)(z^c)(r^d)exp(-zeta*r)
     * 
     * except: a == -2 ==> z^2 ==> (coef)(2z^2-x^2-y^2)(r^d)exp(-zeta*r)
     *    and: b == -2 ==> (coef)(x^2-y^2)(r^d)exp(-zeta*r)
     *    
     *    NOTE: A negative zeta means this is contracted!
     */

    atomIndex = slaterInfo[slaterIndex][0];
    float minuszeta = -slaterData[slaterIndex][0];
    if (!atomSet.get(atomIndex)) {
      if (minuszeta <= 0)
        moCoeff++;
      return;
    }
    int a = slaterInfo[slaterIndex][1];
    int b = slaterInfo[slaterIndex][2];
    int c = slaterInfo[slaterIndex][3];
    int d = slaterInfo[slaterIndex][4];
    if (minuszeta > 0) { //this is contracted; use previous moCoeff
      minuszeta = -minuszeta;
      moCoeff--;
    }
    float coef = slaterData[slaterIndex][1] * moCoefficients[moCoeff++];
    if (coef == 0)
      return;
    setMinMax();
    for (int i = xMax; --i >= xMin;)
      X[i] = xyzBohr[i][0] - atomCoordBohr[atomIndex].x;
    for (int i = yMax; --i >= yMin;)
      Y[i] = xyzBohr[i][1] - atomCoordBohr[atomIndex].y;
    for (int i = zMax; --i >= zMin;)
      Z[i] = xyzBohr[i][2] - atomCoordBohr[atomIndex].z;

    if (a == -2 || b == -2) /* if dz2 *//* if dx2-dy2 */
      for (int ix = xMax; --ix >= xMin;) {
        float dx2 = X[ix] * X[ix];
        for (int iy = yMax; --iy >= yMin;) {
          float dy2 = Y[iy] * Y[iy];
          for (int iz = zMax; --iz >= zMin;) {
            float dz2 = Z[iz] * Z[iz];
            float r = (float) Math.sqrt(dx2 + dy2 + dz2);
            float value = coef * (float) Math.exp(minuszeta * r)
                * ((a == -2 ? 2 * dz2 - dx2 : dx2) - dy2);
            for (int i = d; --i >= 0;)
              value *= r;
            voxelData[ix][iy][iz] += value;
          }
        }
      }
    else
      /* everything else */
      for (int ix = xMax; --ix >= xMin;) {
        float dx = X[ix];
        for (int iy = yMax; --iy >= yMin;) {
          float dy = Y[iy];
          for (int iz = zMax; --iz >= zMin;) {
            float dz = Z[iz];
            float r = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float value = coef * (float) Math.exp(minuszeta * r);
            for (int i = a; --i >= 0;)
              value *= dx;
            for (int i = b; --i >= 0;)
              value *= dy;
            for (int i = c; --i >= 0;)
              value *= dz;
            for (int i = d; --i >= 0;)
              value *= r;
            voxelData[ix][iy][iz] += value;
          }
        }
      }
  }

  private void dumpInfo(int nGaussians, String info) {
    for (int ig = 0; ig < nGaussians; ig++) {
      float alpha = gaussians[gaussianPtr + ig][0];
      float c1 = gaussians[gaussianPtr + ig][1];
      if (Logger.debugging) {
        Logger.debug("Gaussian " + (ig + 1) + " alpha=" + alpha + " c=" + c1);
      }
    }
    int n = info.length() / 2;
    if (Logger.debugging) {
      for (int i = 0; i < n; i++)
        Logger.debug(
            "MO coeff " + info.substring(2 * i, 2 * i + 2) + " " +
            (moCoeff + i + 1) + " " + moCoefficients[moCoeff + i]);
    }
    return;
  }
  
  private void dumpInfo(int nGaussians, int shell) {
    for (int ig = 0; ig < nGaussians; ig++) {
      float alpha = gaussians[gaussianPtr + ig][0];
      float c1 = gaussians[gaussianPtr + ig][1];
      if (Logger.debugging) {
        Logger.debug("Gaussian " + (ig + 1) + " alpha=" + alpha + " c=" + c1);
      }
    }
    if (shell >= 0 && Logger.debugging){
      for (int i = 0; i < shellOrder[shell].length; i++) {
        Logger.debug(
            "MO coeff " + shellOrder[shell][i] + " " +
            (moCoeff + i + 1) + " " + moCoefficients[moCoeff + i]);
      }
    }
    
  }
}

