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
package org.jmol.adapter.readers.orbital;

import org.jmol.adapter.smarter.*;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

import java.util.Vector;

/**
 * 
 * @author hansonr <hansonr@stolaf.edu>
 */
abstract class MopacDataReader extends AtomSetCollectionReader {

  protected final static float MIN_COEF = 0.0001f;  // sufficient?
  
  protected final Hashtable moData = new Hashtable();
  protected int nOrbitals = 0;
  protected final Vector intinfo = new Vector();
  protected final Vector floatinfo = new Vector();
  protected final Vector orbitals = new Vector();
  protected int[] atomicNumbers;
  
  public abstract void readAtomSetCollection(BufferedReader reader);
  
  protected final void addSlater(int iatom, int a, int b, int c, int d, 
                        float zeta, float coef) {
    /*
     * We build two data structures for each slater: 
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
     */
    //System.out.println ("MopacDataReader slater " + intinfo.size() + ": " + iatom + " " + a + " " + b +  " " + c + " " + d + " " + zeta + " " + coef);
    intinfo.addElement(new int[] {iatom, a, b, c, d});
    floatinfo.addElement(new float[] {zeta, coef});
  }
  
  final private static int [] sphericalDValues = new int[] { 
    // MOPAC2007 graphf output data order
    0, -2, 0, //dx2-y2
    1,  0, 1, //dxz
   -2,  0, 0, //dz2
    0,  1, 1, //dyz
    1,  1, 0, //dxy
  };

  protected void createSphericalSlaterByType(int iAtom, int atomicNumber,
                                             String type, float zeta, float coef) {
    int pt = "S Px Py Pz Dx2-y2 Dxz Dz2 Dyz Dxy".indexOf(type);
    // 0 2 5 8 11 18 22 26 30
    switch (pt) {
    case 0: // s
      addSlater(iAtom, 0, 0, 0, MopacData.getNPQs(atomicNumber) - 1, zeta, coef);
      return;
    case 2: // Px
    case 5: // Py
    case 8: // Pz
      addSlater(iAtom, pt == 2 ? 1 : 0, pt == 5 ? 1 : 0, pt == 8 ? 1 : 0,
          MopacData.getNPQp(atomicNumber) - 2, zeta, coef);
      return;
    case 11: // Dx2-y2
    case 18: // Dxz
    case 22: // Dz2
    case 26: // Dyz
    case 30: // Dxy
      int dPt = (pt == 11 ? 0 : pt == 18 ? 1 : pt == 22 ? 2 : pt == 26 ? 3 : 4);
      int dPt3 = dPt * 3;
      addSlater(iAtom, sphericalDValues[dPt3++], sphericalDValues[dPt3++],
          sphericalDValues[dPt3++], MopacData.getNPQd(atomicNumber) - 3, zeta, coef);
      return;
    }
  }  

  protected final void setSlaters(boolean doScale, boolean isMopac) {
    int ndata = intinfo.size();
    int[][] iarray = new int[ndata][];
    float[][] farray = new float[ndata][];
    for (int i = 0; i < ndata; i++) {
      // System.out.println("MopacDataReader mapping basis " + i + " to basis "
      // + pt + Escape.escape(intinfo.get(i)));
      iarray[i] = (int[]) intinfo.get(i);
    }
    for (int i = 0; i < ndata; i++) {
      farray[i] = (float[]) floatinfo.get(i);
      if (doScale) {
        if (isMopac)
          MopacData.scaleSlaterSpherical(atomicNumbers[iarray[i][0]
              % atomicNumbers.length], iarray[i], farray[i]);
        else
          SlaterData.scaleSlater(iarray[i], farray[i]);
      }
    }
    moData.put("slaterInfo", iarray);
    moData.put("slaterData", farray);
    atomSetCollection.setAtomSetAuxiliaryInfo("moData", moData);
  }
  
  protected final void setMOs(String units) {
    moData.put("mos", orbitals);
    moData.put("energyUnits", units);
    setMOData(moData);
  }
  
  protected void sortOrbitals() {
    Object[] array = orbitals.toArray();
    Arrays.sort(array, new OrbitalSorter());
    orbitals.clear();
    for (int i = 0; i < array.length; i++)
      orbitals.add(array[i]);    
  }
  
  class OrbitalSorter implements Comparator {

    public int compare(Object a, Object b) {
      Hashtable mo1 = (Hashtable) a;
      Hashtable mo2 = (Hashtable) b;
      float e1 = ((Float) mo1.get("energy")).floatValue();
      float e2 = ((Float) mo2.get("energy")).floatValue();
      return ( e1 < e2 ? -1 : e2 < e1 ? 1 : 0);
    }    
  }

  protected void sortOrbitalCoefficients(int[] pointers) {
    // now sort the coefficients as well
    for (int i = orbitals.size(); --i >= 0; ) {
      Hashtable mo = (Hashtable) orbitals.get(i);
      float[] coefs = (float[]) mo.get("coefficients");
      float[] sorted = new float[pointers.length];
      for (int j = 0; j < pointers.length; j++) {
        int k = pointers[j];
        if (k < coefs.length)
          sorted[j] = coefs[k];
      }
      mo.put("coefficients", sorted);
    }
  }
}
