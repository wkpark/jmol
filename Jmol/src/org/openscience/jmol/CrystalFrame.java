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

  public Point3d calculateGeometricCenter() {
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
    return new Point3d((minX + maxX) / 2,
                       (minY + maxY) / 2,
                       (minZ + maxZ) / 2);
  }

  // arrowhead size isn't included because it is currently in screen
  // coordinates .. oh well. 
  public double calculateRadius(Point3d center) {
    // initialize to be the radius of the atoms ...
    // just in case there are atoms outside the box
    double radius = super.calculateRadius(center);
    for (int i = 0, size = boxEdges.size(); i < size; ++i) {
      Point3d position = (Point3d) boxEdges.elementAt(i);
      double dist = center.distance(position);
      if (dist > radius)
        radius = dist;
    }
    return radius;
  }
}
