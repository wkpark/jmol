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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.bspt;

import javax.vecmath.Point3f;

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

  Element[] stack;
  int sp;
  int leafIndex;
  Leaf leaf;

  //Point3f center;
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
    stack = new Element[bspt.treeDepth];
  }

  /**
   * initialize to return all points within the sphere defined
   * by center and radius
   *
   * @param center
   * @param radius
   */
  public void initialize(Point3f center, float radius) {
    //this.center = center;
    this.radius = radius;
    this.radius2 = radius*radius;
    this.tHemisphere = false;
    centerValues[0] = center.x;
    centerValues[1] = center.y;
    centerValues[2] = center.z;
    leaf = null;
    stack[0] = bspt.eleRoot;
    sp = 1;
    findLeftLeaf();
  }

  /**
   * initialize to return all points within the hemisphere defined
   * by center and radius.
   *<p>
   * the points returned are those that have a coordinate value >=
   * to center along the first (x) dimension
   *<p>
   * Note that if you are iterating through all points, and two
   * points are within radius and have the same
   * x coordinate, then each will return the other.
   *
   * @param center
   * @param radius
   */
  public void initializeHemisphere(Point3f center, float radius) {
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

  /**
   * normal iterator predicate
   *
   * @return boolean
   */
  public boolean hasMoreElements() {
    while (leaf != null) {
      for ( ; leafIndex < leaf.count; ++leafIndex)
        if (isWithinRadius(leaf.tuples[leafIndex]))
          return true;
      findLeftLeaf();
    }
    return false;
  }

  /**
   * normal iterator method
   *
   * @return Tuple
   */
  public Point3f nextElement() {
    return leaf.tuples[leafIndex++];
  }

  /**
   * After calling nextElement(), allows one to find out
   * the value of the distance squared. To get the distance
   * just take the sqrt.
   *
   * @return float
   */
  public float foundDistance2() {
    return foundDistance2;
  }

  /**
   * does the work
   */
  private void findLeftLeaf() {
    leaf = null;
    if (sp == 0)
      return;
    Element ele = stack[--sp];
    while (ele instanceof Node) {
      Node node = (Node)ele;
      float centerValue = centerValues[node.dim];
      float maxValue = centerValue + radius;
      float minValue = centerValue;
      if (! tHemisphere || node.dim != 0)
        minValue -= radius;
      if (minValue <= node.maxLeft && maxValue >= node.minLeft) {
        if (maxValue >= node.minRight && minValue <= node.maxRight)
          stack[sp++] = node.eleRight;
        ele = node.eleLeft;
      } else if (maxValue >= node.minRight && minValue <= node.maxRight) {
        ele = node.eleRight;
      } else {
        if (sp == 0)
          return;
        ele = stack[--sp];
      }
    }
    leaf = (Leaf)ele;
    leafIndex = 0;
  }

  /**
   * checks one Point3f for distance
   * @param t
   * @return boolean
   */
  private boolean isWithinRadius(Point3f t) {
    float dist2;
    float distT;
    distT = t.x - centerValues[0];
    if  (tHemisphere && distT < 0)
      return false;
    dist2 = distT * distT;
    if (dist2 > radius2)
      return false;

    distT = t.y - centerValues[1];
    dist2 += distT*distT;
    if (dist2 > radius2)
      return false;

    distT = t.z - centerValues[2];
    dist2 += distT*distT;
    if (dist2 > radius2)
      return false;
    
    this.foundDistance2 = dist2;
    return true;
  }
    
}

