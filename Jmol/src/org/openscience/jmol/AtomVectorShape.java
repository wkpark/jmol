package org.openscience.jmol;

import java.awt.Graphics;
import javax.vecmath.Point3f;

class AtomVectorShape implements Shape {

  Atom atom;
  DisplaySettings settings;
  double minMagnitude;
  double magnitudeRange;
  
  AtomVectorShape(Atom atom, DisplaySettings settings, double minMagnitude,
      double magnitudeRange) {
    
    this.atom = atom;
    this.settings = settings;
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

  public void render(Graphics g) {
    if (settings.getShowVectors()) {
      if (atom.getVector() != null) {
        double magnitude = atom.getVector().distance(zeroPoint);
        double scaling = (magnitude - minMagnitude) / magnitudeRange
                           + 0.5;
        ArrowLine al = new ArrowLine(g, atom.screenX, atom.screenY,
                         atom.getScreenVector().x,
                         atom.getScreenVector().y, false, true, scaling);
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

