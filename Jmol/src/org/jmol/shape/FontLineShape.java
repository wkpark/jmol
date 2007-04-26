/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shape;

import org.jmol.g3d.Font3D;

import java.util.BitSet;

public abstract class FontLineShape extends Shape {
  
  // Axes, Bbcage, Frank, Uccage

  Font3D font3d;
  String myType;

 public void initShape() {
    translucentAllowed = false;
  }
  
 public void setProperty(String propertyName, Object value, BitSet bs) {
    if ("font" == propertyName) {
      font3d = (Font3D)value;
      return;
    }
  }
  
 public String getShapeState() {
    return viewer.getObjectState(myType) + Shape.getFontCommand(myType, font3d)
        + ";\n";
  }
}
