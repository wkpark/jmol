package org.openscience.jmol.render;

import org.openscience.jmol.*;

import java.awt.Graphics;
import java.awt.Rectangle;
import javax.vecmath.Point3d;
import javax.vecmath.Matrix4d;

class LineShape extends Shape {

  Point3d pointOrigin;
  Point3d pointEnd;
  int xEnd, yEnd, zEnd;

  LineShape(Point3d pointOrigin, Point3d pointEnd) {
    this.pointOrigin = pointOrigin;
    this.pointEnd = pointEnd;
  }

  public String toString() {
    return "Primitive line shape";
  }

  public void transform(DisplayControl control) {
    Point3d screen = control.transformPoint(pointOrigin);
    x = (int)screen.x;
    y = (int)screen.y;
    z = (int)screen.z;
    screen = control.transformPoint(pointEnd);
    xEnd = (int)screen.x;
    yEnd = (int)screen.y;
    zEnd = (int)screen.z;
    if (zEnd > z)
      z = zEnd;
  }
  
  public void render(Graphics g, Rectangle rectClip, DisplayControl control) {
    g.setColor(control.colorVector);
    g.drawLine(x, y, xEnd, yEnd);
  }
}

