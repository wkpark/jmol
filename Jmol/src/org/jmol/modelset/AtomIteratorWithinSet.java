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

import org.jmol.bspt.Bspf;

import java.util.BitSet;

public class AtomIteratorWithinSet extends AtomIteratorWithinModel implements AtomIndexIterator {
  
  /*
   * a more powerful iterator than AtomIteratorWithinModel
   * allowing excluding a specific atom, finding only those
   * atoms with an atom index greater than that specified
   * or atoms within a specific selected subset of atoms.
   * 
   * Returns atoms within a cube centered on the point
   * Does NOT return i == atomIndex
   *
   */
  
  private BitSet bsSelected;
  private boolean isGreaterOnly;

  /**
   * 
   * @param bspf
   * @param bsSelected
   * @param isGreaterOnly
   * @param isZeroBased
   * @param threadSafe
   */
  public void initialize(Bspf bspf, boolean isZeroBased, boolean threadSafe, 
                         BitSet bsSelected, boolean isGreaterOnly) {
    super.initialize(bspf, isZeroBased, threadSafe);
    this.bsSelected = bsSelected;
    this.isGreaterOnly = isGreaterOnly;
  }

  private int iNext;
  public boolean hasNext() {
    if (atomIndex >= 0)
      while (bsptIter.hasMoreElements()) {
        Atom atom = (Atom) bsptIter.nextElement();
        if ((iNext = atom.index) != atomIndex
            && iNext > (isGreaterOnly ? atomIndex : -1)
            && (bsSelected == null || bsSelected.get(iNext)))
          return true;
      }
    iNext = -1;
    return false;
  }

  public int next() {
    return iNext - zeroBase;
  }
}

