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

import org.jmol.jvxl.data.VolumeData;
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

public class MOCalculation extends QuantumCalculation {

  public static int MAX_GRID = 80;

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

  public MOCalculation(String calculationType, Point3f[] atomCoordAngstroms,
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
  }

  public void createSlaterCube(VolumeData volumeData, BitSet bsSelected) {    
    voxelData = volumeData.voxelData;
    countsXYZ = volumeData.voxelCounts;
    if ((atomSet = bsSelected) == null)
      atomSet = new BitSet();
    setupCoordinates(volumeData.origin, volumeData.volumetricVectorLengths);
    atomIndex = firstAtomOffset -1;
    moCoeff = 0;
    doDebug = (Logger.isActiveLevel(Logger.LEVEL_DEBUG));
    // each STO shell is the combination of one or more gaussians
    int nSlaters = slaterInfo.length;
    for (int i = 0; i < nSlaters; i++) {
      processSlater(i);
    }
  }

  public void createGaussianCube(VolumeData volumeData, BitSet bsSelected) {    
    if (!checkCalculationType())
      return;
    voxelData = volumeData.voxelData;
    countsXYZ = volumeData.voxelCounts;
    if ((atomSet = bsSelected) == null)
      atomSet = new BitSet();
    setupCoordinates(volumeData.origin, volumeData.volumetricVectorLengths);
    atomIndex = firstAtomOffset - 1;
    int nShells = shells.size();
    doDebug = (Logger.isActiveLevel(Logger.LEVEL_DEBUG));
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
    if (calculationType.indexOf("5D") >= 0) {
      Logger
          .error("QuantumCalculation.checkCalculationType: can't read 5D basis sets yet: "
              + calculationType + " -- exit");
      return false;
    }
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
    Hashtable shell = (Hashtable) shells.get(iShell);
    gaussianPtr = ((Integer) shell.get("gaussianPtr")).intValue();
    int nGaussians = ((Integer) shell.get("nGaussians")).intValue();
    atomIndex = ((Integer) shell.get("atomIndex")).intValue() + firstAtomOffset;
    String basisType = (String) shell.get("basisType");
    if (doDebug)
      Logger.debug("processShell: " + iShell + " " + basisType + " nGaussians="
          + nGaussians + " atom=" + atomIndex);
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
    if (basisType.equals("S"))
      addDataS(nGaussians);
    else if (basisType.equals("SP") || basisType.equals("L"))
      addDataSP(nGaussians);
    else if (basisType.equals("P"))
      addDataP(nGaussians);
    else if (basisType.equals("D"))
      addDataD(nGaussians);
    else
      Logger.warn(" Unsupported basis type: " + basisType);
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

  private void addDataD(int nGaussians) {
    //for now just assumes 6 orbitals in the order XX YY ZZ XY XZ YZ
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
      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
        Logger.debug("Gaussian " + (ig + 1) + " alpha=" + alpha + " c=" + c1);
      }
    }
    int n = info.length() / 2;
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      for (int i = 0; i < n; i++)
        Logger.debug(
            "MO coeff " + info.substring(2 * i, 2 * i + 2) + " " +
            (moCoeff + i + 1) + " " + moCoefficients[moCoeff + i]);
    }
    return;
  }
}

