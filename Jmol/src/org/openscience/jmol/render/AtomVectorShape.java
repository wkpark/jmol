package org.openscience.jmol.render;

import org.openscience.jmol.*;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.vecmath.Point3f;

public class AtomVectorShape implements Shape {

  Atom atom;
  DisplayControl control;
  float minMagnitude;
  float magnitudeRange;
  
  AtomVectorShape(Atom atom, DisplayControl control,
                  float minMagnitude, float magnitudeRange) {
    
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
        float magnitude = atom.getVector().distance(zeroPoint);
        float scaling = (magnitude - minMagnitude) / magnitudeRange  + 0.5f;
        ArrowLine al =
          new ArrowLine(g, control, atom.screenX, atom.screenY,
                        atom.getScreenVector().x, atom.getScreenVector().y,
                        false, true, scaling);
      }
    }
  }
  
  public int getZ() {
    return atom.screenZ + 1;
  }
  
  /**
   * Point for calculating lengths of vectors.
   */
  private static final Point3f zeroPoint = new Point3f();
}

