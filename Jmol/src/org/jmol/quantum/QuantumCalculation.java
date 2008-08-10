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

import javax.vecmath.Point3f;

import org.jmol.api.VolumeDataInterface;
import org.jmol.util.Logger;

import java.util.BitSet;

abstract class QuantumCalculation {

  boolean doDebug = false;

  protected final static float bohr_per_angstrom = 1 / 0.52918f;

  protected void setVolume(VolumeDataInterface volumeData, BitSet bsSelected) {
    voxelData = volumeData.getVoxelData();
    countsXYZ = volumeData.getVoxelCounts();
    if ((atomSet = bsSelected) == null)
      atomSet = new BitSet();
    setupCoordinates(volumeData.getOriginFloat(), 
        volumeData.getVolumetricVectorLengths());
  }
  // absolute grid coordinates in Bohr 
  protected float[][] xyzBohr;

  // grid coordinates relative to orbital center in Bohr 
  protected float[] X, Y, Z;

  // grid coordinate squares relative to orbital center in Bohr
  protected float[] X2, Y2, Z2;

  protected Point3f[] atomCoordBohr;
  protected Point3f[] atomCoordAngstroms;
  protected BitSet atomSet;
  protected float[][][] voxelData;
  protected float[] originBohr = new float[3];
  protected float[] stepBohr = new float[3];
  protected int[] countsXYZ;
  protected int atomIndex;
  
  protected int xMin;
  protected int xMax;
  protected int yMin;
  protected int yMax;
  protected int zMin;
  protected int zMax;

  protected void setMinMax() {
    // optimization will come later
    xMin = 0;
    yMin = 0;
    zMin = 0;
    xMax = countsXYZ[0];
    yMax = countsXYZ[1];
    zMax = countsXYZ[2];
  }

  protected void initialize(int n) {
    // absolute grid coordinates in Bohr 
    xyzBohr = new float[n][3];

    // grid coordinates relative to orbital center in Bohr 
     X = new float[n];
     Y = new float[n];
     Z = new float[n];

    // grid coordinate squares relative to orbital center in Bohr
     X2 = new float[n];
     Y2 = new float[n];
     Z2 = new float[n];
  }
  
  protected void setupCoordinates(float[] originXYZ, float[] stepsXYZ) {

    // all coordinates come in as angstroms, not bohr, and are converted here into bohr

    for (int i = 3; --i >= 0;) {
      originBohr[i] = originXYZ[i] * bohr_per_angstrom;
      stepBohr[i] = stepsXYZ[i] * bohr_per_angstrom;
    }
    for (int i = 3; --i >= 0;) {
      xyzBohr[0][i] = originBohr[i];
      int n = countsXYZ[i];
      float inc = stepBohr[i];
      for (int j = 0; ++j < n;)
        xyzBohr[j][i] = xyzBohr[j - 1][i] + inc;
    }
    /* 
     * allowing null atoms allows for selectively removing
     * atoms from the rendering. Maybe a first time this has ever been done?
     * 
     */
    atomCoordBohr = new Point3f[atomCoordAngstroms.length];
    for (int i = 0; i < atomCoordAngstroms.length; i++) {
      if (!atomSet.get(i))
        continue;
      atomCoordBohr[i] = new Point3f(atomCoordAngstroms[i]);
      atomCoordBohr[i].scale(bohr_per_angstrom);
    }

    if (doDebug)
      Logger.debug("QuantumCalculation:\n origin(Bohr)= " + originBohr[0] + " "
          + originBohr[1] + " " + originBohr[2] + "\n steps(Bohr)= "
          + stepBohr[0] + " " + stepBohr[1] + " " + stepBohr[2] + "\n counts= "
          + countsXYZ[0] + " " + countsXYZ[1] + " " + countsXYZ[2]);
  }
}

