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

import java.util.BitSet;

import javax.vecmath.Point3f;

import org.jmol.api.AtomIndexIterator;
import org.jmol.bspt.Bspf;
import org.jmol.bspt.CubeIterator;

public class AtomIteratorWithinModel implements AtomIndexIterator {

  private CubeIterator bsptIter;
  private Bspf bspf;
  private boolean threadSafe;
  private boolean isZeroBased;

  private int modelIndex = Integer.MAX_VALUE;
  private int atomIndex = -1;
  private int zeroBase;
  private float distanceSquared;

  private BitSet bsSelected;
  private boolean isGreaterOnly;

  
  /**
   * 
   * ############## ITERATOR SHOULD BE RELEASED #################
   * 
   * @param bspf
   * @param bsSelected 
   * @param isGreaterOnly 
   * @param isZeroBased
   * @param threadSafe 
   * 
   */

  void initialize(Bspf bspf, BitSet bsSelected, boolean isGreaterOnly, boolean isZeroBased, boolean threadSafe) {
    this.bspf = bspf;
    this.bsSelected = bsSelected;
    this.isGreaterOnly = isGreaterOnly;
    this.isZeroBased = isZeroBased;
    this.threadSafe = threadSafe;
  }

  public void set(int modelIndex, int firstModelAtom, int atomIndex, Point3f center, float distance) {
    if (threadSafe)
      modelIndex = -1 - modelIndex; // no caching
    if (modelIndex != this.modelIndex || bsptIter == null) {
      bsptIter = bspf.getCubeIterator(modelIndex);
      this.modelIndex = modelIndex;
    }
    zeroBase = (isZeroBased ? firstModelAtom : 0);
    if (distance < 0) // distance and center will be added later
      return;
    this.atomIndex = atomIndex;
    set(center, distance);
  }

  public void set(Point3f center, float distance) {
    if (bsptIter == null)
      return;
    bsptIter.initialize(center, distance);
    distanceSquared = distance * distance;
  }
/*  
  private int iNext;
  public boolean hasNext() {
    iNext = (bsptIter == null || !bsptIter.hasMoreElements() ? -1
        : ((Atom) bsptIter.nextElement()).index);
    if (atomIndex >= 0) {
      while (iNext >= 0) {
        if (iNext != atomIndex && iNext > (isGreaterOnly ? atomIndex : -1)
            && (bsSelected == null || bsSelected.get(iNext)))
          return true;
        if (!bsptIter.hasMoreElements()) {
          iNext = -1;
          break;
        }
        iNext = ((Atom) bsptIter.nextElement()).index;
      }
    }
    return (iNext >= 0);
  }
*/
  
  private int iNext;
  public boolean hasNext() {
    if (atomIndex >= 0)
      while (bsptIter.hasMoreElements()) {
        if ((iNext = ((Atom) bsptIter.nextElement()).index) != atomIndex
            && iNext > (isGreaterOnly ? atomIndex : -1)
            && (bsSelected == null || bsSelected.get(iNext)))
          return true;
      }
    else if (bsptIter.hasMoreElements()) {
      iNext = ((Atom) bsptIter.nextElement()).index;
      return true;
    }
    iNext = -1;
    return false;
  }
  
  public int next() {
    return iNext - zeroBase;
  }
  
  public float foundDistance2() {
    return (bsptIter == null ? -1 : bsptIter.foundDistance2());
  }
  
  public void addAtoms(BitSet bsResult) {
    int iAtom;
    while (hasNext())
      if ((iAtom = next()) >= 0
          && foundDistance2() <= distanceSquared)
        bsResult.set(iAtom);    
  }

  public void release() {
    if (bsptIter != null) {
      bsptIter.release();
      bsptIter = null;
    }
  }

}

