package org.openscience.jmol.render;

import org.openscience.jmol.*;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.vecmath.Point3d;

public class AtomVectorShape implements Shape {

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
    StringBuffer buffer = new StringBuffer();
    buffer.append("Atom vector shape for ");
    buffer.append(atom);
    buffer.append(": z = ");
    buffer.append(getZ());
    return buffer.toString();
  }

  public void render(Graphics g, Rectangle rectClip, DisplayControl control) {
    if (control.getShowVectors()) {
      if (atom.getVector() != null) {
        double magnitude = atom.getVector().distance(zeroPoint);
        double scaling = (magnitude - minMagnitude) / magnitudeRange  + 0.5f;
        ArrowLine al =
          new ArrowLine(g, control, atom.getScreenX(), atom.getScreenY(),
                        atom.getScreenVector().x, atom.getScreenVector().y,
                        false, true, scaling);
      }
    }
  }
  
  public int getZ() {
    return atom.getScreenZ() + 1;
  }
  
  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3d zeroPoint = new Point3d();
}
