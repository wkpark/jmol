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

import org.jmol.bspt.Bspf;
import org.jmol.bspt.CubeIterator;

public class AtomIteratorWithinModel implements AtomIndexIterator {

  protected CubeIterator bsptIter;
  protected Bspf bspf;
  protected boolean threadSafe;
  private boolean isZeroBased;

  protected int modelIndex = Integer.MAX_VALUE;
  protected int atomIndex = -1;
  protected int zeroBase;
  private float distanceSquared;

  
  /**
   * just the basic iterator for finding atoms 
   * within a cube centered on some point in space
   * 
   * Used for select within(distance, atom)
   * 
   * @param bspf
   * @param isZeroBased
   * @param threadSafe 
   * 
   */

  void initialize(Bspf bspf, boolean isZeroBased, boolean threadSafe) {
    this.bspf = bspf;
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
    initialize(center, distance);
  }

  public void initialize(Point3f center, float distance) {
    bsptIter.initialize(center, distance);
    distanceSquared = distance * distance;
  }
  
  public boolean hasNext() {
    return (bsptIter != null && bsptIter.hasMoreElements());
  }
  
  public int next() {
    return ((Atom) bsptIter.nextElement()).index - zeroBase;
  }

  public float foundDistance2() {
    return bsptIter.foundDistance2();
  }
  
  public void addAtoms(BitSet bsResult) {
    int iAtom;
    while (hasNext())
      if ((iAtom = next()) >= 0
          && foundDistance2() <= distanceSquared)
        bsResult.set(iAtom);    
  }

  public void release() {
    bsptIter.release();
    bsptIter = null;
  }

}

