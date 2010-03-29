/* $RCSfile$
 * $Author: egonw $
 * $Date: 2006-03-18 15:59:33 -0600 (Sat, 18 Mar 2006) $
 * $Revision: 4652 $
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
package org.jmol.adapter.readers.quantum;

import org.jmol.adapter.smarter.*;
import org.jmol.util.Logger;

import java.util.Hashtable;

import java.util.Vector;

/**
 * 
 * @author hansonr <hansonr@stolaf.edu>
 */
abstract class BasisFunctionReader extends AtomSetCollectionReader {

  protected Hashtable moData = new Hashtable();
  protected Vector orbitals = new Vector();
  protected int nOrbitals = 0;

  private int[][] dfCoefMaps;
  

  // DS: org.jmol.quantum.MOCalculation expects 
  //   d2z^2-x2-y2, dxz, dyz, dx2-y2, dxy
  
  // DC: org.jmol.quantum.MOCalculation expects 
  //      Dxx Dyy Dzz Dxy Dxz Dyz

  // FS: org.jmol.quantum.MOCalculation expects
  //        as 2z3-3x2z-3y2z
  //               4xz2-x3-xy2
  //                   4yz2-x2y-y3
  //                           x2z-y2z
  //                               xyz
  //                                  x3-3xy2
  //                                     3x2y-y3

  // FC: org.jmol.quantum.MOCalculation expects
  //           xxx yyy zzz xyy xxy xxz xzz yzz yyz xyz

  
  /**
   * 
   * finds the position in the Jmol-required list of function types. This list is
   * reader-dependent. 
   * 
   * @param fileList 
   * @param shellType 
   * @param jmolList 
   * @param minLength 
   * @return            true if successful
   * 
   */
  protected boolean getDFMap(String fileList, int shellType, String jmolList, int minLength) {
   if (fileList.equals(jmolList))
      return true;

    
    // say we had line = "251   252   253   254   255"  i  points here
    // Jmol expects list "255   252   253   254   251"  pt points here
    // we need an array that reads
    //                    [4     0     0     0    -4]
    // meaning add that number to the pointer for this coef.
    if (dfCoefMaps == null) {
      dfCoefMaps = new int[][] {null, null, null, // first unused; P and SP are assumed standard X Y Z
          new int[5], new int[6], new int[7], new int[10]};
      moData.put("dfCoefMaps", dfCoefMaps);
    }
    String[] tokens = getTokens(fileList);
    boolean isOK = true;
    for (int i = 0; i < tokens.length && isOK; i++) {
      String key = tokens[i];
      if (key.length() >= minLength) {
        int pt = jmolList.indexOf(key);
        if (pt >= 0) {
          pt /= 6;
          dfCoefMaps[shellType][pt] = i - pt;
          continue;
        }
      }
      isOK = false;
    }
    if (!isOK) {
      Logger.error("Disabling orbitals of type " + shellType + " -- Cannot read orbital order for: " + fileList + "\n expecting: " + jmolList);
      dfCoefMaps[shellType][0] = Integer.MIN_VALUE;
    }
    return isOK;
  }



}
