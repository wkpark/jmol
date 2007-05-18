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

import org.jmol.bspt.Bspf;
import org.jmol.bspt.SphereIterator;
import org.jmol.bspt.Tuple;

public class WithinModelIterator implements AtomIterator {

  SphereIterator bsptIter;

  void initialize(Bspf bspf, int bsptIndex, Tuple center, float radius) {
    bsptIter = bspf.getSphereIterator(bsptIndex);
    bsptIter.initialize(center, radius);
  }

  public boolean hasNext() {
    return bsptIter.hasMoreElements();
  }

  public Atom next() {
    return (Atom) bsptIter.nextElement();
  }

  public void release() {
    bsptIter.release();
    bsptIter = null;
  }
}

/*
 * 
 *   not used:
 *   
class WithinAnyModelIterator implements AtomIterator {

  int bsptIndex;
  Tuple center;
  float radius;
  SphereIterator bsptIter;

  void initialize(Tuple center, float radius) {
    initializeBspf();
    bsptIndex = bspf.getBsptCount();
    bsptIter = null;
    this.center = center;
    this.radius = radius;
  }

  public boolean hasNext() {
    while (bsptIter == null || !bsptIter.hasMoreElements()) {
      if (--bsptIndex < 0) {
        bsptIter = null;
        return false;
      }
      bsptIter = bspf.getSphereIterator(bsptIndex);
      bsptIter.initialize(center, radius);
    }
    return true;
  }

  public Atom next() {
    return (Atom) bsptIter.nextElement();
  }

  public void release() {
    bsptIter.release();
    bsptIter = null;
  }
}

*/
