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
 * Nodes of the bspt. It is a binary tree so nodes contain two children.
 * A splitValue tells which child should be followed. Values <= splitValue
 * are stored down eleLE. Values >= splitValue are stored down eleGE.
 */
class Node extends Element {
  Element eleLE;
  int dim;
  float splitValue;
  Element eleGE;
  
  Node(Bspt bspt, int level, Leaf leafLE) {
    this.bspt = bspt;
    if (level == bspt.treeDepth) {
      bspt.treeDepth = level + 1;
      if (bspt.treeDepth >= bspt.MAX_TREE_DEPTH)
        System.out.println("BSPT tree depth too great:" + bspt.treeDepth);
    }
    if (leafLE.count != bspt.leafCountMax)
      throw new NullPointerException();
    eleLE = leafLE;
    dim = level % bspt.dimMax;
    leafLE.sort(dim);
    splitValue = leafLE.tuples[bspt.leafCountMax/2 - 1].getDimensionValue(dim);
    eleGE = new Leaf(bspt, leafLE, bspt.leafCountMax/2);
    count = bspt.leafCountMax;
  }
  
  Element addTuple(int level, Tuple tuple) {
    float dimValue = tuple.getDimensionValue(dim);
    if (dimValue < splitValue ||
        (dimValue == splitValue && eleLE.count <= eleGE.count))
      eleLE = eleLE.addTuple(level + 1, tuple);
    else
      eleGE = eleGE.addTuple(level + 1, tuple);
    ++count;
    return this;
  }
  
  /*
    void dump(int level) {
    System.out.println("");
    eleLE.dump(level + 1);
    for (int i = 0; i < level; ++i)
    System.out.print("-");
    System.out.println(">" + splitValue);
    eleGE.dump(level + 1);
    }
    
    public String toString() {
    return eleLE.toString() + dim + ":" +
    splitValue + "\n" + eleGE.toString();
    }
  */
}
