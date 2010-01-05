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
import org.jmol.util.Escape;


import java.io.BufferedReader;
import java.util.Arrays;
import java.util.BitSet;
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
  
  public abstract void readAtomSetCollection(BufferedReader reader);
  
  protected final static int [] dValues = new int[] { 
    // MOPAC2007 graphf output data order
    0, -2, 0, //dx2-y2
    1,  0, 1, //dxz
   -2,  0, 0, //dz2
    0,  1, 1, //dyz
    1,  1, 0, //dxy
  };

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
  
  protected BitSet bsBases;
  protected int nBases;
  
  protected final void setSlaters() {
    int ndata = intinfo.size();
    if (bsBases == null)
      nBases = ndata;
    int[][] iarray = new int[nBases][];
    for (int i = 0, pt = 0; i < ndata; i++)
      if (bsBases == null || bsBases.get(i)) {
        //System.out.println("MopacDataReader mapping basis " + i + " to basis " + pt + Escape.escape(intinfo.get(i)));
        iarray[pt++] = (int[]) intinfo.get(i);
      }
    float[][] farray = new float[nBases][];
    for (int i = 0, pt = 0; i < ndata; i++)
      if (bsBases == null || bsBases.get(i))
        farray[pt++] = (float[]) floatinfo.get(i);
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
