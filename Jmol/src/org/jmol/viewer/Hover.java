/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

import org.jmol.g3d.*;

import java.util.BitSet;

class Hover extends Shape {

  private final static String FONTFACE = "SansSerif";
  private final static String FONTSTYLE = "Plain";
  private final static int FONTSIZE = 12;

  int atomIndex = -1;
  Font3D font3d;
  String labelFormat = "%U";
  short colixBackground;
  short colixForeground;

  void initShape() {
    font3d = g3d.getFont3D(FONTFACE, FONTSTYLE, FONTSIZE);
    colixBackground = g3d.getColix("#FFFFC3"); // 255, 255, 195
    colixForeground = Graphics3D.BLACK;
  }

  void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    if ("target" == propertyName) {
      if (value == null)
        atomIndex = -1;
      else
        atomIndex = ((Integer)value).intValue();
      return;
    }
    
    if ("color" == propertyName) {
      //      System.out.println("hover color changed");
      colixForeground = g3d.getColix(value);
      return;
    }

    if ("bgcolor" == propertyName) {
      //      System.out.println("hover bgcolor changed");
      colixBackground = g3d.getColix(value);
      return;
    }
    
    if ("font" == propertyName) {
      font3d = (Font3D)value;
      return;
    }

    if ("label" == propertyName) {
      labelFormat = (String)value;
      if (labelFormat != null && labelFormat.length() == 0)
        labelFormat = null;
      return;
    }
  }
}

