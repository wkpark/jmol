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

import javax.vecmath.Point3f;

import org.jmol.bspt.Bspf;
import org.jmol.bspt.CubeIterator;

public class AtomIteratorWithinModel implements AtomIndexIterator {

  CubeIterator bsptIter;

  /**
   * just the basic iterator for finding atoms 
   * within a cube centered on some point in space
   * 
   * Used for select within(distance, atom)
   * 
   * @param bspf
   * @param bsptIndex
   * @param center
   * @param radius
   * 
   */

  void initialize(Bspf bspf, int bsptIndex, Point3f center, float radius) {
    bsptIter = bspf.getCubeIterator(bsptIndex);
    bsptIter.initialize(center, radius);
  }

  public boolean hasNext() {
    if (bsptIter.hasMoreElements())
      return true;
    release();
    return false;
  }

  public int next() {
    return ((Atom) bsptIter.nextElement()).index;
  }

  public float foundDistance2() {
    return bsptIter.foundDistance2();
  }
  
  private void release() {
    bsptIter.release();
    bsptIter = null;
  }
}

