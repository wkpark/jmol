package org.openscience.jmol;

import java.awt.Graphics;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix4d;

class LineShape implements Shape {

  DisplaySettings settings;
  Matrix4d mat;
  Point3f origPoint;
  Point3f endPoint;

  LineShape(DisplaySettings settings, Matrix4d mat, Point3f origPoint,
      Point3f endPoint) {

    this.settings = settings;
    this.mat = mat;
    this.origPoint = origPoint;
    this.endPoint = endPoint;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Primitive line shape");
    return buffer.toString();
  }

  public void render(Graphics g) {

    mat.transform(origPoint, screenPositionOrig);
    mat.transform(endPoint, screenPositionEnd);


    PlainLine al = new PlainLine(g, screenPositionOrig.x,
                     screenPositionOrig.y, screenPositionEnd.x,
                     screenPositionEnd.y);

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

