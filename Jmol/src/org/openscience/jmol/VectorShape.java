package org.openscience.jmol;

import java.awt.Graphics;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix4d;

class VectorShape implements Shape {

  DisplaySettings settings;
  Matrix4d mat;
  Point3f origPoint;
  Point3f endPoint;
  boolean arrowStart;
  boolean arrowEnd;


  VectorShape(DisplaySettings settings, Matrix4d mat, Point3f origPoint,
      Point3f endPoint, boolean arrowStart, boolean arrowEnd) {

    this.settings = settings;
    this.mat = mat;
    this.origPoint = origPoint;
    this.endPoint = endPoint;
    this.arrowStart = arrowStart;
    this.arrowEnd = arrowEnd;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Primitive vector shape");
    return buffer.toString();
  }

  public void render(Graphics g) {

    double scaling = 1.0;

    mat.transform(origPoint, screenPositionOrig);
    mat.transform(endPoint, screenPositionEnd);


    ArrowLine al = new ArrowLine(g, screenPositionOrig.x,
                     screenPositionOrig.y, screenPositionEnd.x,
                     screenPositionEnd.y, arrowStart, arrowEnd, scaling);

  }

  public double getZ() {
    return screenPositionEnd.z + 4.0;
  }

  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3f zeroPoint = new Point3f();
  private Point3f screenPositionEnd = new Point3f();
  private Point3f screenPositionOrig = new Point3f();
}

