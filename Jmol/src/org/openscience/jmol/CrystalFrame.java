/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol;

import java.util.Vector;
import java.util.Enumeration;
import javax.vecmath.Point3d;

public class CrystalFrame extends ChemFrame {

  //the 3 primitives vectors
  private double[][] rprimd;

  //The box edges needed to draw the unit cell box
  //boxEdges.elementAt(0) = *origin* of the *first* edge of the box
  //boxEdges.elementAt(1) = *end* of the *first* edge of the box
  //boxEdges.elementAt(2) = *origin* of the *second* edge of the box
  //etc.
  private Vector boxEdges; //Vector of Point3d
  
  public CrystalFrame() {
    
  }

  public CrystalFrame(int na) {
    super(na);
  }

  public void setRprimd(double[][] rprimd) {
    this.rprimd = rprimd;
  }

  public double[][] getRprimd() {
    return this.rprimd;
  }

  public void setBoxEdges(Vector boxEdges) {
    this.boxEdges = boxEdges;
  }


  public Vector getBoxEdges() {
    return this.boxEdges;
  }

  void calcBoundingBox() {
    Point3d position = (Point3d) boxEdges.elementAt(0);
    double minX = position.x, maxX = minX;
    double minY = position.y, maxY = minY;
    double minZ = position.z, maxZ = minZ;

    for (int i = 1, size = boxEdges.size(); i < size; ++i) {
      position = (Point3d) boxEdges.elementAt(i);
      double x = position.x;
      if (x < minX) { minX = x; }
      if (x > maxX) { maxX = x; }
      double y = position.y;
      if (y < minY) { minY = y; }
      if (y > maxY) { maxY = y; }
      double z = position.z;
      if (z < minZ) { minZ = z; }
      if (z > maxZ) { maxZ = z; }
    }
    centerBoundingBox = new Point3d((minX + maxX) / 2,
                                    (minY + maxY) / 2,
                                    (minZ + maxZ) / 2);
    cornerBoundingBox = new Point3d(maxX, maxY, maxZ);
    cornerBoundingBox.sub(centerBoundingBox);
  }

  // arrowhead size isn't included because it is currently in screen
  // coordinates .. oh well. 
  public double calcRadius(Point3d center) {
    // initialize to be the radius of the atoms ...
    // just in case there are atoms outside the box
    double radius = super.calcRadius(center);
    for (int i = 0, size = boxEdges.size(); i < size; ++i) {
      Point3d position = (Point3d) boxEdges.elementAt(i);
      double dist = center.distance(position);
      if (dist > radius)
        radius = dist;
    }
    return radius;
  }
}
