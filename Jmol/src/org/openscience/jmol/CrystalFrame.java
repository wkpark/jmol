package org.openscience.jmol;

import java.util.Vector;
import java.util.Enumeration;
import javax.vecmath.Point3f;

public class CrystalFrame extends ChemFrame {

  //the 3 primitives vectors
  private float[][] rprimd;

  //The box edges needed to draw the unit cell box
  //boxEdges.elementAt(0) = *origin* of the *first* edge of the box
  //boxEdges.elementAt(1) = *end* of the *first* edge of the box
  //boxEdges.elementAt(2) = *origin* of the *second* edge of the box
  //etc.
  private Vector boxEdges; //Vector of Point3f
  
  public CrystalFrame() {
    
  }

  public CrystalFrame(int na) {
    super(na);
  }

  public void setRprimd(float[][] rprimd) {
    this.rprimd = rprimd;
  }

  public float[][] getRprimd() {
    return this.rprimd;
  }

  public void setBoxEdges(Vector boxEdges) {
    this.boxEdges = boxEdges;
  }


  public Vector getBoxEdges() {
    return this.boxEdges;
  }

  public Point3f calculateGeometricCenter() {
    Point3f position = (Point3f) boxEdges.elementAt(0);
    float minX = position.x, maxX = minX;
    float minY = position.y, maxY = minY;
    float minZ = position.z, maxZ = minZ;

    for (int i = 1, size = boxEdges.size(); i < size; ++i) {
      position = (Point3f) boxEdges.elementAt(i);
      float x = position.x;
      if (x < minX) { minX = x; }
      if (x > maxX) { maxX = x; }
      float y = position.y;
      if (y < minY) { minY = y; }
      if (y > maxY) { maxY = y; }
      float z = position.z;
      if (z < minZ) { minZ = z; }
      if (z > maxZ) { maxZ = z; }
    }
    return new Point3f((minX + maxX) / 2,
                       (minY + maxY) / 2,
                       (minZ + maxZ) / 2);
  }

  // arrowhead size isn't included because it is currently in screen
  // coordinates .. oh well. 
  public float calculateRadius(Point3f center) {
    // initialize to be the radius of the atoms ...
    // just in case there are atoms outside the box
    float radius = super.calculateRadius(center);
    for (int i = 0, size = boxEdges.size(); i < size; ++i) {
      Point3f position = (Point3f) boxEdges.elementAt(i);
      float dist = center.distance(position);
      if (dist > radius)
        radius = dist;
    }
    return radius;
  }
}
