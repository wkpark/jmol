package org.openscience.jmol.render;

import org.openscience.jmol.*;

import java.awt.Graphics;
import javax.vecmath.Point3d;

public class VectorShape extends LineShape {

  boolean arrowStart;
  boolean arrowEnd;

  VectorShape(Point3d pointOrigin, Point3d pointEnd,
              boolean arrowStart, boolean arrowEnd) {
    super(pointOrigin, pointEnd);
    this.arrowStart = arrowStart;
    this.arrowEnd = arrowEnd;
  }

  public String toString() {
    return "Primitive vector shape";
  }

  public void render(Graphics g, DisplayControl control) {
    double scaling = 1.0;
    ArrowLine al = new ArrowLine(g, control, x, y, xEnd, yEnd,
                                 arrowStart, arrowEnd, scaling);

  }
}

