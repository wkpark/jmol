package org.openscience.jmol.render;

import org.openscience.jmol.*;

import java.awt.Graphics;
import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix4f;

public class VectorShape implements Shape, Transformable {

  Point3f origPoint;
  Point3f endPoint;
  boolean arrowStart;
  boolean arrowEnd;


  VectorShape(Point3f origPoint, Point3f endPoint,
              boolean arrowStart, boolean arrowEnd) {
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

  public void transform(DisplayControl control) {
    control.transformPoint(origPoint, screenPositionOrig);
    control.transformPoint(endPoint, screenPositionEnd);
  }

  public void render(Graphics g, Rectangle rectClip, DisplayControl control) {

    double scaling = 1.0;


    ArrowLine al = new ArrowLine(g, control,
                                 screenPositionOrig.x, screenPositionOrig.y,
                                 screenPositionEnd.x, screenPositionEnd.y,
                                 arrowStart, arrowEnd, scaling);

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

