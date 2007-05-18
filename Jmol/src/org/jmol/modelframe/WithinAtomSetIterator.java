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

package org.jmol.modelframe;

import org.jmol.atomdata.AtomIndexIterator;
import org.jmol.bspt.Bspf;
import org.jmol.bspt.SphereIterator;
import java.util.BitSet;

class WithinAtomSetIterator implements AtomIndexIterator {
  
  //does NOT return i == atomIndex

  Frame frame;
  SphereIterator bsptIter;
  BitSet bsSelected;
  boolean isGreaterOnly;
  int atomIndex;
  int zerobase;

  void initialize(Frame frame, Bspf bspf, int bsptIndex, int atomIndex, float distance, BitSet bsSelected, boolean isGreaterOnly) {
    bsptIter = bspf.getSphereIterator(bsptIndex);
    bsptIter.initialize(frame.atoms[atomIndex], distance);
    this.atomIndex = atomIndex;
    this.bsSelected = bsSelected;
    this.isGreaterOnly = isGreaterOnly;
    zerobase = frame.getFirstAtomIndexInModel(bsptIndex);
  }

  int iNext;
  public boolean hasNext() {
    while (bsptIter.hasMoreElements()) {
      Atom atom = (Atom) bsptIter.nextElement();
      if ((iNext = atom.atomIndex) != atomIndex 
          && iNext > (isGreaterOnly ? atomIndex : -1)
          && (bsSelected == null || bsSelected.get(iNext)))
        return true;
    }
    iNext = -1;
    return false;
  }

  public int next() {
    return iNext - zerobase;
  }

  public void release() {
    bsptIter.release();
    bsptIter = null;
  }
}

