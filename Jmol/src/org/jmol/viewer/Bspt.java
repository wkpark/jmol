/* $RCSfile$
 * $Author$
 * $Date$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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
package org.jmol.viewer;

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

final class Bspt {

  private final static int leafCountMax = 8;
  // this corresponds to the max height of the tree
  private final static int MAX_TREE_DEPTH = 100;
  int treeDepth = 0;
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
    eleRoot = eleRoot.addTuple(0, tuple);
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
      stack = new Node[treeDepth];
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

    void initializeHemisphere(Tuple center, float radius) {
      initialize(center, radius);
      tHemisphere = true;
    }

    void release() {
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

  abstract class Element {
    int count;
    abstract Element addTuple(int level, Tuple tuple);
  }

  class Node extends Element {
    Element eleLE;
    int dim;
    float splitValue;
    Element eleGE;
    
    Node(int level, Leaf leafLE) {
      if (level >= treeDepth) {
        if (level >= MAX_TREE_DEPTH) {
          System.out.println("BSPT tree depth too great");
          throw new NullPointerException();
        }
        treeDepth = level;
      }
      eleLE = leafLE;
      dim = level % dimMax;
      leafLE.sort(dim);
      splitValue = leafLE.tuples[leafCountMax/2 - 1].getDimensionValue(dim);
      eleGE = new Leaf(leafLE, leafCountMax/2);
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
  }

  class Leaf extends Element {
    Tuple[] tuples;
    
    Leaf() {
      count = 0;
      tuples = new Tuple[leafCountMax];
    }
    
    Leaf(Leaf leaf, int countToKeep) {
      this();
      for (int i = countToKeep; i < leafCountMax; ++i) {
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

    /*
    String toString() {
      return "leaf:" + count + "\n";
    }
    */

    Element addTuple(int level, Tuple tuple) {
      if (count < leafCountMax) {
        tuples[count++] = tuple;
        return this;
      }
      Node node = new Node(level, this);
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
    */
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
