package org.openscience.jmol.render;

import org.openscience.jmol.*;

import java.awt.Graphics;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix4f;

class LineShape implements Shape, Transformable {

  Point3f origPoint;
  Point3f endPoint;

  LineShape(Point3f origPoint, Point3f endPoint) {

    this.origPoint = origPoint;
    this.endPoint = endPoint;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Primitive line shape");
    return buffer.toString();
  }

  public void transform(Matrix4f matrix, DisplayControl control) {
    matrix.transform(origPoint, screenPositionOrig);
    matrix.transform(endPoint, screenPositionEnd);
  }
  
  public void render(Graphics g) {

    PlainLine al = new PlainLine(g, screenPositionOrig.x,
                     screenPositionOrig.y, screenPositionEnd.x,
                     screenPositionEnd.y);

  }

  public int getZ() {
    return (int)screenPositionEnd.z + 4;
  }

  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3f zeroPoint = new Point3f();
  private Point3f screenPositionEnd = new Point3f();
  private Point3f screenPositionOrig = new Point3f();
}

