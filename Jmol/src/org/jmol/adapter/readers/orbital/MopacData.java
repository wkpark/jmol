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
package org.jmol.adapter.readers.orbital;

public class MopacData extends SlaterData {

  final static void scaleSlaterSpherical(int atomicNumber, int[] idata, float[] fdata) {
    int x = idata[1]; // may be -2 for spherical Dz2
    int y = idata[2]; // may be -2 for spherical Dx2-y2
    int z = idata[3];
    int el = x + y + z;
    float zeta = Math.abs(fdata[0]);
    switch (el) {
    case 0: //S
      fdata[1] *= getSlaterConstS(getNPQs(atomicNumber), zeta);
      break;
    case 1: //P
      fdata[1] *= getSlaterConstP(getNPQp(atomicNumber), zeta);
      break;
    case 2: //D
      fdata[1] *= getSlaterConstDSpherical(getNPQd(atomicNumber), zeta, x, y, z);
      break;
    case 3: //F
      fdata[1] = 0; // not set up for spherical f
      break;
    }
  }

  /*
   * Sincere thanks to Jimmy Stewart, MrMopac@att.net for these constants
   * 
   */

  ///////////// MOPAC CALCULATION SLATER CONSTANTS //////////////

  // see http://openmopac.net/Downloads/Mopac_7.1source.zip|src_modules/parameters_C.f90

  //H                                                             He
  //Li Be                                          B  C  N  O  F  Ne
  //Na Mg                                          Al Si P  S  Cl Ar
  //K  Ca Sc          Ti V  Cr Mn Fe Co Ni Cu Zn   Ga Ge As Se Br Kr
  //Rb Sr Y           Zr Nb Mo Tc Ru Rh Pd Ag Cd   In Sn Sb Te I  Xe
  //Cs Ba La Ce-Lu    Hf Ta W  Re Os Ir Pt Au Hg   Tl Pb Bi Po At Rn
  //Fr Ra Ac Th-Lr    ?? ?? ?? ??

  private final static int[] principalQuantumNumber = new int[] { 0, 
      1, 1, //  2
      2, 2, 2, 2, 2, 2, 2, 2, // 10
      3, 3, 3, 3, 3, 3, 3, 3, // 18
      4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, // 36
      5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, // 54
      6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
      6, 6, 6, 6, 6, 6, 6, 6, // 86
  };

  private final static int getNPQ(int atomicNumber) {
    return (atomicNumber < principalQuantumNumber.length ? 
        principalQuantumNumber[atomicNumber] : 0);
  }

  final static int getNPQs(int atomicNumber) {
    int n = getNPQ(atomicNumber);
    switch (atomicNumber) {
    case 10:
    case 18:
    case 36:
    case 54:
    case 86:
      return n + 1;
    default:
      return n;        
    }
  }

  final static int getNPQp(int atomicNumber) {
    int n = getNPQ(atomicNumber);
    switch (atomicNumber) {
    case 2:
      return n + 1;
    default:
      return n;        
    }
  }

  final static int getNPQd(int atomicNumber) {
    return (atomicNumber < npqd.length ? npqd[atomicNumber] : 0);
  }

  //H                                                             He
  //Li Be                                          B  C  N  O  F  Ne
  //Na Mg                                          Al Si P  S  Cl Ar
  //K  Ca Sc          Ti V  Cr Mn Fe Co Ni Cu Zn   Ga Ge As Se Br Kr
  //Rb Sr Y           Zr Nb Mo Tc Ru Rh Pd Ag Cd   In Sn Sb Te I  Xe
  //Cs Ba La Ce-Lu    Hf Ta W  Re Os Ir Pt Au Hg   Tl Pb Bi Po At Rn
  //Fr Ra Ac Th-Lr    ?? ?? ?? ??

  private final static int[] npqd = new int[] { 0,
      0, 3, // 2
      0, 0, 0, 0, 0, 0, 0, 3, // 10
      3, 3, 3, 3, 3, 3, 3, 4, // 18
      3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, // 36
      4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 6, // 54
      5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
      5, 6, 6, 6, 6, 6, 6, 7, // 86
  };

  private final static double[] factorDSpherical = new double[] { 
    0.5,      1,     0.5 / Math.sqrt(3)};
  //x2-y2     xz     2r2 - x2 - y2  

  private final static float getSlaterConstDSpherical(int n, float zeta, 
                                                      int x, int y, int z) {
    int dPt = (y < 0 ? 0 : x < 0 ? 2 : 1);
    return (float) (fact(15, zeta, n) * factorDSpherical[dPt]);
  }

}
