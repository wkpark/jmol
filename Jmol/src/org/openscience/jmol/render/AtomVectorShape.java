package org.openscience.jmol.render;

import org.openscience.jmol.*;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.vecmath.Point3d;

public class AtomVectorShape extends Shape {

  Atom atom;
  DisplayControl control;
  double minMagnitude;
  double magnitudeRange;
  
  AtomVectorShape(Atom atom, DisplayControl control,
                  double minMagnitude, double magnitudeRange) {
    this.atom = atom;
    this.control = control;
    this.minMagnitude = minMagnitude;
    this.magnitudeRange = magnitudeRange;
  }

  public String toString() {
    return "Atom vector shape for " + atom + ": z = " + z;
  }

  public void render(Graphics g, Rectangle rectClip, DisplayControl control) {
    // FIXME I think that much/all of this could be moved to instance creation
    double magnitude = atom.getVector().distance(zeroPoint);
    double scaling = 1;
    if (magnitudeRange>0) {
      scaling = (magnitude - minMagnitude) / magnitudeRange  + 0.5f;
    }
    ArrowLine al =
      new ArrowLine(g, control, atom.getScreenX(), atom.getScreenY(),
                    x, y,
                    false, true, scaling);
  }

  public void transform(DisplayControl control) {
    Point3d screen = control.transformPoint(atom.getPosition());
    int zAtom = (int)screen.z;
    screen = control.transformPoint(atom.getScaledVector());
    x = (int)screen.x;
    y = (int)screen.y;
    z = (zAtom + (int)screen.z) / 2;
  }
  
  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3d zeroPoint = new Point3d();
}
