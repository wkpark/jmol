/* $RCSfile$
 * $Author$
 * $Date$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.bspt;

/**
 * A leaf of Tuple objects in the bsp tree
 *
 * @author Miguel, miguel@jmol.org
 */
class Leaf extends Element {
  Tuple[] tuples;
    
  Leaf(Bspt bspt) {
    this.bspt = bspt;
    count = 0;
    tuples = new Tuple[bspt.leafCountMax];
  }
    
  Leaf(Bspt bspt, Leaf leaf, int countToKeep) {
    this(bspt);
    for (int i = countToKeep; i < bspt.leafCountMax; ++i) {
      tuples[count++] = leaf.tuples[i];
      leaf.tuples[i] = null;
    }
    leaf.count = countToKeep;
  }

  void sort(int dim) {
    for (int i = count; --i > 0; ) { // this is > not >=
      Tuple champion = tuples[i];
      float championValue = champion.getDimensionValue(dim);
      for (int j = i; --j >= 0; ) {
        Tuple challenger = tuples[j];
        float challengerValue = challenger.getDimensionValue(dim);
        if (challengerValue > championValue) {
          tuples[i] = challenger;
          tuples[j] = champion;
          champion = challenger;
          championValue = challengerValue;
        }
      }
    }
  }

  Element addTuple(int level, Tuple tuple) {
    if (count < bspt.leafCountMax) {
      tuples[count++] = tuple;
      return this;
    }
    Node node = new Node(bspt, level, this);
    return node.addTuple(level, tuple);
  }
    
  /*
    void dump(int level) {
    for (int i = 0; i < count; ++i) {
    Tuple t = tuples[i];
    for (int j = 0; j < level; ++j)
    System.out.print(".");
    for (int dim = 0; dim < dimMax-1; ++dim)
    System.out.print("" + t.getDimensionValue(dim) + ",");
    System.out.println("" + t.getDimensionValue(dimMax - 1));
    }
    }

    public String toString() {
    return "leaf:" + count + "\n";
    }
  */

}
