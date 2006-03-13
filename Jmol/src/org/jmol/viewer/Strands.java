/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

package org.jmol.viewer;
import java.util.BitSet;

class Strands extends Mps {

  /*==============================================================*
   * M. Carson and C.E. Bugg (1986)
   * Algorithm for Ribbon Models of Proteins. J.Mol.Graphics 4:121-122.
   * http://sgce.cbse.uab.edu/carson/papers/ribbons86/ribbons86.html
   *==============================================================*/

  int strandCount = 5;

  Mps.Mpspolymer allocateMpspolymer(Polymer polymer) {
    return new Schain(polymer);
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    initialize();
    if ("strandCount" == propertyName) {
      if (value instanceof Integer) {
        int count = ((Integer)value).intValue();
        if (count < 0)
          count = 0;
        else if (count > 20)
          count = 20;
        strandCount = count;
        return;
      }
    }
    super.setProperty(propertyName, value, bs);
  }

  class Schain extends Mps.Mpspolymer {

    Schain(Polymer polymer) {
      super(polymer, -2, 3000, 800, 5000);
    }
  }
}
