/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 08:19:45 -0500 (Fri, 18 May 2007) $
 * $Revision: 7742 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.modelset;



import org.jmol.api.AtomIndexIterator;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.bspt.Bspf;
import org.jmol.bspt.CubeIterator;
import org.jmol.util.BS;
import org.jmol.util.P3;
import org.jmol.viewer.Viewer;

public class AtomIteratorWithinModel implements AtomIndexIterator {

  protected CubeIterator cubeIterator;
  protected Bspf bspf;
  private boolean threadSafe;
  private boolean hemisphereOnly;
  private boolean isZeroBased;

  protected int modelIndex = Integer.MAX_VALUE;
  private int atomIndex = -1;
  private int zeroBase;
  private float distanceSquared;

  private BS bsSelected;
  private boolean isGreaterOnly;
  private boolean checkGreater;

  AtomIteratorWithinModel() {
    //for one model only
  }
  
  
  /**
   * 
   * ############## ITERATOR SHOULD BE RELEASED #################
   * 
   * @param bspf
   * @param bsSelected 
   * @param isGreaterOnly 
   * @param isZeroBased
   * @param hemisphereOnly TODO
   * @param threadSafe 
   * 
   */

  void initialize(Bspf bspf, BS bsSelected, boolean isGreaterOnly, 
                  boolean isZeroBased, boolean hemisphereOnly, boolean threadSafe) {
    this.bspf = bspf;
    this.bsSelected = bsSelected;
    this.isGreaterOnly = isGreaterOnly;
    this.isZeroBased = isZeroBased;
    this.hemisphereOnly = hemisphereOnly;
    this.threadSafe = threadSafe;
    cubeIterator = null;
  }

  private RadiusData radiusData;
  private float vdw1;
  private boolean isVdw;
  private Atom[] atoms;
  private Viewer viewer;
  
  
  public void setModel(ModelCollection modelSet, int modelIndex, int firstModelAtom, int atomIndex, P3 center, float distance, RadiusData rd) {
    if (threadSafe)
      modelIndex = -1 - modelIndex; // no caching
    if (modelIndex != this.modelIndex || cubeIterator == null) {
      cubeIterator = bspf.getCubeIterator(modelIndex);
      this.modelIndex = modelIndex;
      //bspf.dump();
    }
    zeroBase = (isZeroBased ? firstModelAtom : 0);
    if (distance == Integer.MIN_VALUE) // distance and center will be added later
      return;
    this.atomIndex = (distance < 0 ? -1 : atomIndex);
    isVdw = (rd != null);
    if (isVdw) {
      radiusData = rd;
      atoms = modelSet.atoms;
      viewer = modelSet.viewer;
      distance = (rd.factorType == EnumType.OFFSET ? 5f + rd.value : 5f * rd.value);
      vdw1 = atoms[atomIndex].getVanderwaalsRadiusFloat(viewer, rd.vdwType);
    }
    checkGreater = (isGreaterOnly && atomIndex != Integer.MAX_VALUE);
    setCenter(center, distance);
  }

  public void setCenter(P3 center, float distance) {
    if (cubeIterator == null)
      return;
    cubeIterator.initialize(center, distance, hemisphereOnly);
    distanceSquared = distance * distance;
  }
  
  private int iNext;
  public boolean hasNext() {
    if (atomIndex >= 0)
      while (cubeIterator.hasMoreElements()) {
        Atom a = (Atom) cubeIterator.nextElement();
        if ((iNext = a.index) != atomIndex
            && (!checkGreater || iNext > atomIndex)
            && (bsSelected == null || bsSelected.get(iNext))) {
          return true;
        }
      }
    else if (cubeIterator.hasMoreElements()) {
      Atom a = (Atom) cubeIterator.nextElement();
      iNext = a.index;
      return true;
    }
    iNext = -1;
    return false;
  }
  
  public int next() {
    return iNext - zeroBase;
  }
  
  public float foundDistance2() {
    return (cubeIterator == null ? -1 : cubeIterator.foundDistance2());
  }
  
  /**
   * turns this into a SPHERICAL iterator
   * for "within Distance" measures
   * 
   * @param bsResult 
   * 
   */
  @SuppressWarnings("incomplete-switch")
  public void addAtoms(BS bsResult) {
    int iAtom;
    while (hasNext())
      if ((iAtom = next()) >= 0) {
        float d;
        if (isVdw) {
          d = atoms[iAtom].getVanderwaalsRadiusFloat(viewer, radiusData.vdwType) + vdw1;
          switch (radiusData.factorType) {
          case OFFSET:
            d += radiusData.value * 2;
            break;
          case FACTOR:
            d *= radiusData.value;
            break;  
          }
          d *= d;
        } else {
          d = distanceSquared;
        }
        if (foundDistance2() <= d)
          bsResult.set(iAtom);    
      }
  }

  public void release() {
    if (cubeIterator != null) {
      cubeIterator.release();
      cubeIterator = null;
    }
  }

}

