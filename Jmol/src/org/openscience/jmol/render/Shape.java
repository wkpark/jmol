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

import org.openscience.jmol.DisplayControl;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Provides an interface for graphical components so that
 * proper depth rendering occurs using z-buffering. Improper
 * depth rendering will still occur for spatially intersecting
 * shapes. This is an unavoidable limitation of the pseudo-3D
 * rendering implemented here.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public abstract class Shape {

  // screen coordinates after transformation
  public int x, y, z;
  // note that this z is used for the z-order sort process
  // remember that for perspective depth calculations all values
  // of z are <= 0 ... 0 is at the surface of the screen and
  // more negative is further back away from the screen

  abstract public void render(Graphics g, DisplayControl control);

  abstract public void transform(DisplayControl control);
}
