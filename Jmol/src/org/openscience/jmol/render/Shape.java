
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
public interface Shape {

  public void render(Graphics g, Rectangle rectClip, DisplayControl control);

  public void transform(DisplayControl control);

  public int getZ();

}
