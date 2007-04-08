/* $RCSfile$
 * $Author: egonw $
 * $Date: 2006-03-18 15:59:33 -0600 (Sat, 18 Mar 2006) $
 * $Revision: 4652 $
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
package org.jmol.adapter.smarter;


import java.io.BufferedReader;
import java.util.Hashtable;

import java.util.Vector;

/**
 * CSF file reader based on CIF idea -- fluid property fields.
 *
 * note that, like CIF, the order of fields is totally unpredictable
 * in addition, ID numbers are not sequential, requiring atomNames
 * 
 * first crack at this 2006/04/13
 * 
 * @author hansonr <hansonr@stolaf.edu>
 */
abstract class MopacDataReader extends AtomSetCollectionReader {

  final static float MIN_COEF = 0.0001f;  // sufficient?
  
  Hashtable moData = new Hashtable();
  int nOrbitals = 0;
  Vector intinfo = new Vector();
  Vector floatinfo = new Vector();
  Vector orbitals = new Vector();
  
  abstract AtomSetCollection readAtomSetCollection(BufferedReader reader);
  
  static int [] dValues = new int[] { 
    // MOPAC2007 graphf output data order
    0, -2, 0, //dx2-y2
    1,  0, 1, //dxz
   -2,  0, 0, //dz2
    0,  1, 1, //dyz
    1,  1, 0, //dxy
  };

  void addSlater(int i0, int i1, int i2, int i3, int i4, 
                        float zeta, float coef) {
    //System.out.println (i0 + " " + i1 + " " + i2 +  " " + i3 + " " + i4 + " " + zeta + " " + coef);
    intinfo.add(new int[] {i0, i1, i2, i3, i4});
    floatinfo.add(new float[] {zeta, coef});
  }
  
  void setSlaters() {
    int ndata = intinfo.size();
    int[][] iarray = new int[ndata][];
    for (int i = 0; i < ndata; i++)
      iarray[i] = (int[]) intinfo.get(i);
    float[][] farray = new float[ndata][];
    for (int i = 0; i < ndata; i++)
      farray[i] = (float[]) floatinfo.get(i);
    moData.put("slaterInfo", iarray);
    moData.put("slaterData", farray);
    atomSetCollection.setAtomSetAuxiliaryInfo("moData", moData);
  }
  
  void setMOs(String units) {
    moData.put("mos", orbitals);
    moData.put("energyUnits", units);
    atomSetCollection.setAtomSetAuxiliaryInfo("moData", moData);
  }
}
