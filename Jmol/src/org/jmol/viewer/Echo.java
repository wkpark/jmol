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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import org.jmol.util.Logger;

import org.jmol.g3d.*;

import java.util.BitSet;

class Echo extends TextShape {

  
  /*
   * set echo Text.TOP    [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo MIDDLE [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo BOTTOM [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo name   [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo name  x-position y-position
   * 
   */
  

  private final static String FONTFACE = "Serif";
  private final static int FONTSIZE = 20;
  private final static short COLOR = Graphics3D.RED;

  void initShape() {
    setProperty("target", "top", null);
  }

  void setProperty(String propertyName, Object value, BitSet bsSelected) {

    Logger.debug("Echo.setProperty(" + propertyName + "," + value + ")");

    if ("target" == propertyName) {
      String target = ((String) value).intern().toLowerCase();
      Text text = (Text) texts.get(target);
      if ("none" == target) {
        currentText = null;
        return;
      }
      if (text == null) {
        int valign = Text.XY;
        int halign = Text.LEFT;
        if ("top" == target) {
          valign = Text.TOP;
          halign = Text.CENTER;
        } else if ("middle" == target) {
          valign = Text.MIDDLE;
          halign = Text.CENTER;
        } else if ("bottom" == target) {
          valign = Text.BOTTOM;
        }
        text = new Text(g3d, g3d.getFont3D(FONTFACE, FONTSIZE), target, COLOR, valign,
            halign);
        text.setAdjustForWindow(true); // when a box is around it
        texts.put(target, text);
      }
      currentText = text;
      return;
    }
    super.setProperty(propertyName, value, null);
  }
}

