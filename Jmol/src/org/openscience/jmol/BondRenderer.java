
/*
 * Copyright 2002 The Jmol Development Team
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

import java.awt.Graphics;

/**
 * Drawing methods for bonds.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public interface BondRenderer {

  /**
   * Draws a bond on a particular graphics context.
   *
   * @param gc the Graphics context
   * @param atom1 the atom from which the bond is to be drawn
   * @param atom2 the atom to which the bond is to be drawn
   * @param settings the display settings
   */
  public void paint(Graphics gc, Atom atom1, Atom atom2,
      DisplaySettings settings);

}
