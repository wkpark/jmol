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
 *<p>
 *  a Binary Space Partitioning Tree
 *</p>
 *<p>
 *  The tree partitions n-dimensional space (in our case 3) into little
 *  boxes, facilitating searches for things which are *nearby*.
 *</p>
 *<p>
 *  For some useful background info, search the web for "bsp tree faq".
 *  Our application is somewhat simpler because we are storing points instead
 *  of polygons.
 *</p>
 *<p>
 *  We are working with three dimensions. For the purposes of the Bspt code
 *  these dimensions are stored as 0, 1, or 2. Each node of the tree splits
 *  along the next dimension, wrapping around to 0.
 *  <pre>
 *    mySplitDimension = (parentSplitDimension + 1) % 3;
 *  </pre>
 *  A split value is stored in the node. Values which are <= splitValue are
 *  stored down the left branch. Values which are >= splitValue are stored
 *  down the right branch. If searchValue == splitValue then the search must
 *  proceed down both branches.
 *</p>
 *<p>
 *  Planar and crystaline substructures can generate values which are == along
 *  one dimension.
 *</p>
 *<p>
 *  To get a good picture in your head, first think about it in one dimension,
 *  points on a number line. The tree just partitions the points.
 *  Now think about 2 dimensions. The first node of the tree splits the plane
 *  into two rectangles along the x dimension. The second level of the tree
 *  splits the subplanes (independently) along the y dimension into smaller
 *  rectangles. The third level splits along the x dimension.
 *  In three dimensions, we are doing the same thing, only working with
 *  3-d boxes.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */

public final class Bspt {

  final static int leafCountMax = 16;
  // this corresponds to the max height of the tree
  final static int MAX_TREE_DEPTH = 100;
  int treeDepth;
  int dimMax;
  private Element eleRoot;

  /*
    static float distance(int dim, Tuple t1, Tuple t2) {
    return Math.sqrt(distance2(dim, t1, t2));
    }

    static float distance2(int dim, Tuple t1, Tuple t2) {
    float distance2 = 0.0;
    while (--dim >= 0) {
    float distT = t1.getDimensionValue(dim) - t2.getDimensionValue(dim);
    distance2 += distT*distT;
    }
    return distance2;
    }
  */

  /**
   * Create a bspt with the specified number of dimensions. For a 3-dimensional
   * tree (x,y,z) call new Bspt(3).
   */
  public Bspt(int dimMax) {
    this.dimMax = dimMax;
    this.eleRoot = new Leaf(this);
    treeDepth = 1;
  }

  /**
   * Iterate through all of your data points, calling addTuple
   */
  public void addTuple(Tuple tuple) {
    eleRoot = eleRoot.addTuple(0, tuple);
  }

  /**
   * prints some simple stats to stdout
   */
  public void stats() {
    System.out.println("bspt treeDepth=" + treeDepth +
                       " count=" + eleRoot.count);
  }

  /*
    public void dump() {
    eleRoot.dump(0);
    }

    public String toString() {
    return eleRoot.toString();
    }
  */

  /*
    Enumeration enum() {
    return new EnumerateAll();
    }

    class EnumerateAll implements Enumeration {
    Node[] stack;
    int sp;
    int i;
    Leaf leaf;

    EnumerateAll() {
    stack = new Node[stackDepth];
    sp = 0;
    Element ele = eleRoot;
    while (ele instanceof Node) {
    Node node = (Node) ele;
    if (sp == stackDepth)
    System.out.println("Bspt.EnumerateAll tree stack overflow");
    stack[sp++] = node;
    ele = node.eleLE;
    }
    leaf = (Leaf)ele;
    i = 0;
    }

    boolean hasMoreElements() {
    return (i < leaf.count) || (sp > 0);
    }

    Object nextElement() {
    if (i == leaf.count) {
    //        System.out.println("-->" + stack[sp-1].splitValue);
    Element ele = stack[--sp].eleGE;
    while (ele instanceof Node) {
    Node node = (Node) ele;
    stack[sp++] = node;
    ele = node.eleLE;
    }
    leaf = (Leaf)ele;
    i = 0;
    }
    return leaf.tuples[i++];
    }
    }

    Enumeration enumNear(Tuple center, float distance) {
    return new EnumerateNear(center, distance);
    }

    class EnumerateNear implements Enumeration {
    Node[] stack;
    int sp;
    int i;
    Leaf leaf;
    float distance;
    Tuple center;

    EnumerateNear(Tuple center, float distance) {
    this.distance = distance;
    this.center = center;

    stack = new Node[stackDepth];
    sp = 0;
    Element ele = eleRoot;
    while (ele instanceof Node) {
    Node node = (Node) ele;
    if (center.getDimensionValue(node.dim) - distance <= node.splitValue) {
    if (sp == stackDepth)
    System.out.println("Bspt.EnumerateNear tree stack overflow");
    stack[sp++] = node;
    ele = node.eleLE;
    } else {
    ele = node.eleGE;
    }
    }
    leaf = (Leaf)ele;
    i = 0;
    }

    boolean hasMoreElements() {
    if (i < leaf.count)
    return true;
    if (sp == 0)
    return false;
    Element ele = stack[--sp];
    while (ele instanceof Node) {
    Node node = (Node) ele;
    if (center.getDimensionValue(node.dim) + distance < node.splitValue) {
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
    i = 0;
    return true;
    }

    Object nextElement() {
    return leaf.tuples[i++];
    }
    }
  */

  public SphereIterator allocateSphereIterator() {
    return new SphereIterator();
  }

  /**
   * Iterator used for finding all points within a sphere or a hemisphere
   *<p>
   * Obtain a SphereIterator by calling Bspt.allocateSphereIterator().
   *<p>
   * call initialize(...) or initializeHemizphere(...)
   *<p>
   * re-initialize in order to reuse the same SphereIterator
   *
   * @see Bspt.allocateSphereIterator
   */
  public class SphereIterator {
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

    SphereIterator() {
      centerValues = new float[dimMax];
      stack = new Node[treeDepth];
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
      for (int dim = dimMax; --dim >= 0; )
        centerValues[dim] = center.getDimensionValue(dim);
      sp = 0;
      Element ele = eleRoot;
      while (ele instanceof Node) {
        Node node = (Node) ele;
        if (centerValues[node.dim] - radius <= node.splitValue) {
          if (sp == treeDepth)
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
      for (int i = treeDepth; --i >= 0; )
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
      int dim = dimMax - 1;
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

}

