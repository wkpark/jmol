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
  final Point3d screenVector = new Point3d();
  
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
    // FIXME I think that much/all of this could be moved to instance creation
    double magnitude = atom.getVector().distance(zeroPoint);
    double scaling = 1;
    if (magnitudeRange>0) {
      scaling = (magnitude - minMagnitude) / magnitudeRange  + 0.5f;
    }
    ArrowLine al =
      new ArrowLine(g, control, atom.getScreenX(), atom.getScreenY(),
                    (int)screenVector.x, (int)screenVector.y,
                    false, true, scaling);
  }

  public void transform(DisplayControl control) {
    control.transformPoint(atom.getScaledVector(), screenVector);
  }
  
  public int getZ() {
    return (atom.getScreenZ() + (int)screenVector.z) / 2;
  }
  
  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3d zeroPoint = new Point3d();
}
