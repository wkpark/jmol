
/*
 * Copyright 2001 The Jmol Development Team
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
package org.openscience.jmol;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Vector;

public class Atom {
  
  /**
   * Creates an atom with the given type.
   *
   * @param the type of this atom.
   */
  public Atom(BaseAtomType atomType) {
    this.atomType = new AtomType(atomType);
  }
  
  /**
   * Returns this atom's base type.
   *
   * @return the base type of this atom.
   */
  public BaseAtomType getType() {
    return atomType.getBaseAtomType();
  }
  
  /**
   * Returns this atom's color.
   *
   * @return the color of this atom.
   */
  public Color getColor() {
    return atomType.getColor();
  }
  
  /**
   * Sets this atom's color.
   *
   * @param the color to set the atom.
   */
  public void setColor(Color color) {
    atomType.setColor(color);
  }
  
  /**
   * Draws the atom on a particular graphics context
   *
   * @param gc the Graphics context
   * @param x x position of the atom in screen space
   * @param y y position of the atom in screen space
   * @param z z position (helps in perspective and depth cueing)
   * @param n atom location in the input file
   * @param props Vector containing the physical properties associated with this atom
   * @param picked whether or not the atom has been selected and gets a "halo"
   *
   */
  public void paint(Graphics gc, DisplaySettings settings, int x, int y,
          int z, int n, Vector props, boolean picked) {
    atomType.paint(gc, settings, x, y, z, n, props, picked);
  }
  
  private AtomType atomType;
  
}

