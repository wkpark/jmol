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
  mth 2003 05
  BSP-Tree stands for Binary Space Partitioning Tree
  The tree partitions n-dimensional space (in our case 3) into little
  boxes, facilitating searches for things which are *nearby*.
  For some useful background info, search the web for "bsp tree faq".
  Our application is somewhat simpler because we are storing points instead
  of polygons.
  We are working with three dimensions. For the purposes of the Bspt code
  these dimensions are stored as 0, 1, or 2. Each node of the tree splits
  along the next dimension, wrapping around to 0.
    mySplitDimension = (parentSplitDimension + 1) % 3;
  A split value is stored in the node. Values which are <= splitValue are
  stored down the left branch. Values which are >= splitValue are stored
  down the right branch. If searchValue == splitValue then the search must
  proceed down both branches.
  Planar and crystaline substructures can generate values which are == along
  one dimension.
  To get a good picture in your head, first think about it in one dimension,
  points on a number line. The tree just partitions the points.
  Now think about 2 dimensions. The first node of the tree splits the plane
  into two rectangles along the x dimension. The second level of the tree
  splits the subplanes (independently) along the y dimension into smaller
  rectangles. The third level splits along the x dimension.
  In three dimensions, we are doing the same thing, only working with
  3-d boxes.

  Three iterators are provided
    enumNear(Bspt.Tuple center, float distance)
      returns all the points contained in of all the boxes which are within
      distance from the center.
    enumSphere(Bspt.Tuple center, float distance)
      returns all the points which are contained within the sphere (inclusive)
      defined by center + distance
    enumHemiSphere(Bspt.Tuple center, float distance)
      same as sphere, but only the points which are greater along the
      x dimension
*/

public final class Bspt {

  private final static int leafCountMax = 4;
  // this corresponds to the max height of the tree
  private final static int stackDepth = 64;
  int dimMax;
  Element eleRoot;

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

  Bspt(int dimMax) {
    this.dimMax = dimMax;
    this.eleRoot = new Leaf();
  }

  void addTuple(Tuple tuple) {
    if (! eleRoot.addTuple(tuple)) {
      eleRoot = new Node(0, dimMax, (Leaf) eleRoot);
      if (! eleRoot.addTuple(tuple))
        System.out.println("Bspt.addTuple() failed");
    }
  }

  /*
  String toString() {
    return eleRoot.toString();
  }

  void dump() {
    eleRoot.dump(0);
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

  SphereIterator allocateSphereIterator() {
    return new SphereIterator();
  }

  class SphereIterator {
    Node[] stack;
    int sp;
    int leafIndex;
    Leaf leaf;

    Tuple center;
    float radius;

    float centerValues[];
    float radius2;
    float foundDistance2; // the dist squared of a found Element;

    // when set, only the hemisphere sphere .GE. the point
    // (on the first dim) is returned
    boolean tHemisphere;

    SphereIterator() {
      centerValues = new float[dimMax];
      stack = new Node[stackDepth];
    }

    void initialize(Tuple center, float radius) {
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
          if (sp == stackDepth)
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

    void initializeHemisphere(Tuple center, float radius) {
      initialize(center, radius);
      tHemisphere = true;
    }

    void release() {
      for (int i = stackDepth; --i >= 0; )
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
    
    boolean hasMoreElements() {
      while (true) {
        for ( ; leafIndex < leaf.count; ++leafIndex)
          if (isWithin(leaf.tuples[leafIndex]))
            return true;
        if (sp == 0)
          return false;
        Element ele = stack[--sp];
        while (ele instanceof Node) {
          Node node = (Node) ele;
          if (centerValues[node.dim]+radius < node.splitValue) {
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

    Object nextElement() {
      return leaf.tuples[leafIndex++];
    }

    float foundDistance2() {
      return foundDistance2;
    }
  }

  interface Tuple {
    float getDimensionValue(int dim);
  }

  interface Element {
    boolean addTuple(Tuple tuple);
    //    void dump(int level);
    boolean isLeafWithSpace();
  }

  static class Node implements Element {
    Element eleLE;
    int dim;
    int dimMax;
    float splitValue;
    Element eleGE;

    Node(int dim, int dimMax, Leaf leafLE) {
      this.eleLE = leafLE;
      this.dim = dim;
      this.dimMax = dimMax;
      this.splitValue = leafLE.getSplitValue(dim);
      this.eleGE = new Leaf(leafLE, dim, splitValue);
    }

    public boolean addTuple(Tuple tuple) {
      if (tuple.getDimensionValue(dim) < splitValue) {
        if (eleLE.addTuple(tuple))
          return true;
        eleLE = new Node((dim + 1) % dimMax, dimMax, (Leaf)eleLE);
        return eleLE.addTuple(tuple);
      }
      if (tuple.getDimensionValue(dim) > splitValue) {
        if (eleGE.addTuple(tuple))
          return true;
        eleGE = new Node((dim + 1) % dimMax, dimMax, (Leaf)eleGE);
        return eleGE.addTuple(tuple);
      }
      if (eleLE.isLeafWithSpace())
        eleLE.addTuple(tuple);
      else if (eleGE.isLeafWithSpace())
        eleGE.addTuple(tuple);
      else if (eleLE instanceof Node)
        eleLE.addTuple(tuple);
      else if (eleGE instanceof Node)
        eleGE.addTuple(tuple);
      else {
        eleLE = new Node((dim + 1) % dimMax, dimMax, (Leaf)eleLE);
        return eleLE.addTuple(tuple);
      }
      return true;
    }

    /*
    String toString() {
      return eleLE.toString() + dim + ":" + splitValue + "\n" + eleGE.toString();
    }

    void dump(int level) {
      System.out.println("");
      eleLE.dump(level + 1);
      for (int i = 0; i < level; ++i)
        System.out.print("-");
      System.out.println(">" + splitValue);
      eleGE.dump(level + 1);
    }
    */

    public boolean isLeafWithSpace() {
      return false;
    }
  }

  static class Leaf implements Element {
    int count;
    Tuple[] tuples;

    Leaf() {
      count = 0;
      tuples = new Tuple[leafCountMax];
    }

    Leaf(Leaf leaf, int dim, float splitValue) {
      this();
      // first, move over all that are greater
      for (int i = leafCountMax; --i >= 0; ) {
        Tuple tuple = leaf.tuples[i];
        float value = tuple.getDimensionValue(dim);
        if (value > splitValue) {
          leaf.tuples[i] = null;
          tuples[count++] = tuple;
        }
      }
      // now, move the ones that are ==, keeping a balance
      for (int i = leafCountMax; --i >= 0; ) {
        Tuple tuple = leaf.tuples[i];
        if (tuple != null &&
            tuple.getDimensionValue(dim) == splitValue &&
            count < (leafCountMax / 2)) {
          leaf.tuples[i] = null;
          tuples[count++] = tuple;
        }
      }
      // slide down the null values
      int dest = 0;
      for (int src = 0; src < leafCountMax; ++src)
        if (leaf.tuples[src] != null)
          leaf.tuples[dest++] = leaf.tuples[src];
      leaf.count = dest;
      if (count == 0 || leaf.count == 0)
        throw new NullPointerException("Bspt leaf splitting error");
    }

    float getSplitValue(int dim) {
      if (count != leafCountMax)
        throw new NullPointerException("Bspt leaf splitting too soon");
      return (tuples[0].getDimensionValue(dim) +
              tuples[1].getDimensionValue(dim)) / 2;
    }

    /*
    String toString() {
      return "leaf:" + count + "\n";
    }
    */

    public boolean addTuple(Tuple tuple) {
      if (count == leafCountMax)
        return false;
      tuples[count++] = tuple;
      return true;
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
    */

    public boolean isLeafWithSpace() {
      return count < leafCountMax;
    }
  }
}

/*
class Point implements Bspt.Tuple {
  float x;
  float y;
  float z;

  Point(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  float getDimensionValue(int dim) {
    if (dim == 0)
      return x;
    if (dim == 1)
      return y;
    return z;
  }

  String toString() {
    return "<" + x + "," + y + "," + z + ">";
  }
}
*/
