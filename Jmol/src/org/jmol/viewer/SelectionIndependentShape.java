/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004  The Jmol Development Team
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

package org.jmol.viewer;

import org.jmol.g3d.Font3D;

import java.util.BitSet;

abstract class SelectionIndependentShape extends Shape {

  short mad;
  short colix;
  short bgcolix;
  Font3D font3d;

  void setSize(int size, BitSet bsSelected) {
    this.mad = (short)size;
  }
  
  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("color" == propertyName) {
      colix = g3d.getColix(value);
      return;
    }

    if ("font" == propertyName) {
      font3d = (Font3D)value;
      return;
    }

    if ("bgcolor" == propertyName) {
      bgcolix = g3d.getColix(value);
      return;
    }
  }
}
