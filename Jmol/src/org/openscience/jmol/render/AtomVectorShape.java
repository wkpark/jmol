/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.render;

import org.openscience.jmol.*;
import org.openscience.jmol.g25d.Graphics25D;

//import java.awt.Graphics;
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

  public void render(Graphics25D g25d, DisplayControl control) {
    // FIXME I think that much/all of this could be moved to instance creation
    double magnitude = atom.getVector().distance(zeroPoint);
    double scaling = 1;
    if (magnitudeRange>0) {
      scaling = (magnitude - minMagnitude) / magnitudeRange  + 0.5f;
    }
    ArrowLine al =
      new ArrowLine(g25d, control,
                    atom.getScreenX(), atom.getScreenY(), atom.getScreenZ(),
                    x, y, z,
                    true, false, true, scaling);
  }

  public void transform(DisplayControl control) {
    Point3d screen = control.transformPoint(atom.getPoint3D());
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
