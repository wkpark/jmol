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
package org.jmol.adapter.readers.more;

/*
 * Sincere thanks to Jimmy Stewart, MrMopac@att.net for these constants
 * 
 */

public class MopacData {

  ///////////// MOPAC CALCULATION SLATER CONSTANTS //////////////

  private final static boolean isNoble(int atomicNumber) {
    switch (atomicNumber) {
    case 2:
    case 10:
    case 18:
    case 36:
    case 54:
    case 86:
      return true;
    default:
      return false;
    }
  }

  //H                                                             He
  //Li Be                                          B  C  N  O  F  Ne
  //Na Mg                                          Al Si P  S  Cl Ar
  //K  Ca Sc          Ti V  Cr Mn Fe Co Ni Cu Zn   Ga Ge As Se Br Kr
  //Rb Sr Y           Zr Nb Mo Tc Ru Rh Pd Ag Cd   In Sn Sb Te I  Xe
  //Cs Ba La Ce-Lu    Hf Ta W  Re Os Ir Pt Au Hg   Tl Pb Bi Po At Rn
  //Fr Ra Ac Th-Lr    ?? ?? ?? ??

  private final static int[] principalQuantumNumber = new int[] { 0, 1, 1, //  2
      2, 2, 2, 2, 2, 2, 2, 2, // 10
      3, 3, 3, 3, 3, 3, 3, 3, // 18
      4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, // 36
      5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, // 54
      6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
      6, 6, 6, 6, 6, 6, 6, 6, // 86
  };

  private final static int getNPQ(int atomicNumber) {
    return (atomicNumber < principalQuantumNumber.length ? principalQuantumNumber[atomicNumber]
        : 0);
  }

  public final static int getNPQs(int atomicNumber) {
    return getNPQ(atomicNumber)
        + (atomicNumber > 2 && isNoble(atomicNumber) ? 1 : 0);
  }

  public final static int getNPQp(int atomicNumber) {
    return getNPQ(atomicNumber) + (atomicNumber == 2 ? 1 : 0);
  }

  private final static int[] pnqD = new int[] { 0, //1-10
      0, 0, //  2
      0, 0, 0, 0, 0, 0, 0, 0, // 10
      3, 3, 3, 3, 3, 3, 3, 4, // 18
      3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, // 36
      4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 6, // 54
      5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
      5, 6, 6, 6, 6, 6, 6, 7, // 86
  };

  public final static int getNPQd(int atomicNumber) {
    return (atomicNumber < pnqD.length ? pnqD[atomicNumber] : 0);
  }

  private final static float[] fact = new float[20];
  static {
    fact[0] = 1;
    for (int n = 1; n < fact.length; n++)
      fact[n] = fact[n - 1] * n;
  }

  private final static float fourPi = (float) (4 * Math.PI);

  public final static float getMopacConstS(int atomicNumber, float zeta) {
    int n = getNPQs(atomicNumber);
    return (float) (Math.pow(2 * zeta, n + 0.5) * Math.sqrt(1 / fourPi
        / fact[2 * n]));
  }

  public final static float getMopacConstP(int atomicNumber, float zeta) {
    int n = getNPQp(atomicNumber);
    return (float) (Math.pow(2 * zeta, n + 0.5) * Math.sqrt(3 / fourPi
        / fact[2 * n]));
  }

  private final static float[] factorDs = new float[] { 0.5f, 1f,
      (float) (0.5 / Math.sqrt(3)), 1f, 1f };

  //  x2-y2 xz        2r2 - x2 - y2        yz  xy 

  public static float getFactorD(int n) {
    return factorDs[n];
  }

  public final static float getMopacConstD(int atomicNumber, float zeta) {
    int n = getNPQd(atomicNumber);
    return (float) (Math.pow(2 * zeta, n + 0.5) * Math.sqrt(15 / fourPi
        / fact[2 * n]));
  }
}
