package org.openscience.jmol.render;

import org.openscience.jmol.*;

import java.awt.Graphics;
import java.awt.Rectangle;
import javax.vecmath.Point3d;
import javax.vecmath.Matrix4d;

class LineShape implements Shape, Transformable {

  Point3d origPoint;
  Point3d endPoint;

  LineShape(Point3d origPoint, Point3d endPoint) {

    this.origPoint = origPoint;
    this.endPoint = endPoint;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("Primitive line shape");
    return buffer.toString();
  }

  public void transform(DisplayControl control) {
    control.transformPoint(origPoint, screenPositionOrig);
    control.transformPoint(endPoint, screenPositionEnd);
  }
  
  public void render(Graphics g, Rectangle rectClip, DisplayControl control) {

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
  private static final Point3d zeroPoint = new Point3d();
  private Point3d screenPositionEnd = new Point3d();
  private Point3d screenPositionOrig = new Point3d();
}

