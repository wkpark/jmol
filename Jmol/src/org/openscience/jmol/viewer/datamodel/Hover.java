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

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import org.jmol.g3d.*;

import java.awt.Color;
import java.awt.FontMetrics;
import java.util.BitSet;

public class Hover extends Shape {

  private final static int LEFT = 0;
  private final static int CENTER = 1;
  private final static int RIGHT = 2;

  private final static int TOP = 0;
  private final static int BOTTOM = 1;
  private final static int MIDDLE = 2;

  private final static String FONTFACE = "Serif";
  private final static String FONTSTYLE = "Plain";
  private final static int FONTSIZE = 16;

  int atomIndex;
  Font3D font3d;
  String labelFormat = "%U";
  short colixBackground;
  short colixForeground;

  void initShape() {
    font3d = g3d.getFont3D(FONTFACE, FONTSTYLE, FONTSIZE);
    colixBackground = g3d.getColix("lightskyblue");
    colixForeground = Graphics3D.BLACK;
  }

  public void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    if ("target" == propertyName) {
      if (value == null)
        atomIndex = -1;
      else
        atomIndex = ((Integer)value).intValue();
      return;
    }
    
    if ("color" == propertyName) {
      colixForeground = g3d.getColix(value);
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

