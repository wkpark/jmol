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
 * Iterator used for finding all points within a sphere or a hemisphere
 *<p>
 * Obtain a SphereIterator by calling Bspt.allocateSphereIterator().
 *<p>
 * call initialize(...) or initializeHemizphere(...)
 *<p>
 * re-initialize in order to reuse the same SphereIterator
 *
 * @author Miguel, miguel@jmol.org
 */
public class SphereIterator {
  Bspt bspt;

  Node[] stack;
  int sp;
  int leafIndex;
  Leaf leaf;

  Tuple center;
  float radius;

  float[] centerValues;
  float radius2;
  float foundDistance2; // the dist squared of a found Element;

  // when set, only the hemisphere sphere .GE. the point
  // (on the first dim) is returned
  boolean tHemisphere;

  SphereIterator(Bspt bspt) {
    this.bspt = bspt;
    centerValues = new float[bspt.dimMax];
    stack = new Node[bspt.treeDepth];
  }

  /**
   * initialize to return all points within the sphere defined
   * by <code>center</code> and <code>radius</code>.
   */
  public void initialize(Tuple center, float radius) {
    this.center = center;
    this.radius = radius;
    this.radius2 = radius*radius;
    this.tHemisphere = false;
    for (int dim = bspt.dimMax; --dim >= 0; )
      centerValues[dim] = center.getDimensionValue(dim);
    sp = 0;
    Element ele = bspt.eleRoot;
    while (ele instanceof Node) {
      Node node = (Node) ele;
      if (centerValues[node.dim] - radius <= node.splitValue) {
        if (sp == bspt.treeDepth)
          System.out.println("Bspt.SphereIterator tree stack overflow");
        stack[sp++] = node;
        ele = node.eleLE;
      } else {
        ele = node.eleGE;
      }
    }
    leaf = (Leaf)ele;
    leafIndex = 0;
  }

  /**
   * initialize to return all points within the hemisphere defined
   * by <code>center</code> and <code>radius</code>.
   *<p>
   * the points returned are those that have a coordinate value >=
   * to <code>center</code> along the first (x) dimension
   *<p>
   * Note that if you are iterating through all points, and two
   * points are within <code>radius</code> and have the same
   * x coordinate, then each will return the other
   */
  public void initializeHemisphere(Tuple center, float radius) {
    initialize(center, radius);
    tHemisphere = true;
  }

  /**
   * nulls internal references
   */
  public void release() {
    for (int i = bspt.treeDepth; --i >= 0; )
      stack[i] = null;
  }

  private boolean isWithin(Tuple t) {
    float dist2;
    float distT;
    distT = t.getDimensionValue(0) - centerValues[0];
    if  (tHemisphere && distT < 0)
      return false;
    dist2 = distT * distT;
    if (dist2 > radius2)
      return false;
    int dim = bspt.dimMax - 1;
    do {
      distT = t.getDimensionValue(dim) - centerValues[dim];
      dist2 += distT*distT;
      if (dist2 > radius2)
        return false;
    } while (--dim > 0);
    this.foundDistance2 = dist2;
    return true;
  }
    
  /**
   * normal iterator predicate
   */
  public boolean hasMoreElements() {
    while (true) {
      for ( ; leafIndex < leaf.count; ++leafIndex)
        if (isWithin(leaf.tuples[leafIndex]))
          return true;
      if (sp == 0)
        return false;
      Element ele = stack[--sp];
      while (ele instanceof Node) {
        Node node = (Node) ele;
        if (centerValues[node.dim] + radius < node.splitValue) {
          if (sp == 0)
            return false;
          ele = stack[--sp];
        } else {
          ele = node.eleGE;
          while (ele instanceof Node) {
            Node nodeLeft = (Node) ele;
            stack[sp++] = nodeLeft;
            ele = nodeLeft.eleLE;
          }
        }
      }
      leaf = (Leaf)ele;
      leafIndex = 0;
    }
  }

  /**
   * normal iterator method
   */
  public Tuple nextElement() {
    return leaf.tuples[leafIndex++];
  }

  /**
   * After calling nextElement(), allows one to find out
   * the value of the distance squared. To get the distance
   * just take the sqrt.
   */
  public float foundDistance2() {
    return foundDistance2;
  }
}

