/* $RCSfile$
 * $Author$
 * $Date$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.viewer.datamodel;

/*
  A Binary Space Partitioning Forest
  trees + an iterator
*/

public final class Bspf {

  int dimMax;
  Bspt bspts[];
  Bspt.SphereIterator[] sphereIterators;
  
  Bspf(int dimMax) {
    this.dimMax = dimMax;
    bspts = new Bspt[0];
    sphereIterators = new Bspt.SphereIterator[0];
  }

  public int getBsptCount() {
    return bspts.length;
  }
  
  public void addTuple(int bsptIndex, Bspt.Tuple tuple) {
    if (bsptIndex >= bspts.length) {
      Bspt[] t = new Bspt[bsptIndex + 1];
      System.arraycopy(bspts, 0, t, 0, bspts.length);
      bspts = t;
    }
    Bspt bspt = bspts[bsptIndex];
    if (bspt == null)
      bspt = bspts[bsptIndex] = new Bspt(dimMax);
    bspt.addTuple(tuple);
  }

  public Bspt.SphereIterator getSphereIterator(int bsptIndex) {
    if (bsptIndex >= sphereIterators.length) {
      Bspt.SphereIterator[] t = new Bspt.SphereIterator[bsptIndex + 1];
      System.arraycopy(sphereIterators, 0, t, 0, sphereIterators.length);
      sphereIterators = t;
    }
    if (sphereIterators[bsptIndex] == null &&
        bspts[bsptIndex] != null)
      sphereIterators[bsptIndex] = bspts[bsptIndex].allocateSphereIterator();
    return sphereIterators[bsptIndex];
  }
}
