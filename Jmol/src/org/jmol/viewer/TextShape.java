/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-13 00:06:10 -0500 (Wed, 13 Sep 2006) $
 * $Revision: 5516 $
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
import java.util.Hashtable;

class TextShape extends Shape {

  Hashtable texts = new Hashtable();
  Text currentText;
  
  void initShape() {
  }

  void setProperty(String propertyName, Object value, BitSet bsSelected) {

    Logger.debug("TextShape.setProperty(" + propertyName + "," + value + ")");

    if ("allOff" == propertyName) {
      currentText = null;
      texts = new Hashtable();
      return;
    }

    if ("off" == propertyName) {
      if (currentText == null)
        return;
      texts.remove(currentText.target);
      currentText = null;
      return;
    }

    if ("align" == propertyName) {
      if (currentText == null)
        return;
      String align = (String) value;
      if (!currentText.setAlignment(align))
        Logger.error("unrecognized align:" + align);
      return;
    }

    if ("bgcolor" == propertyName) {
      if (currentText != null)
        currentText.setBgColix(value);
      return;
    }

    if ("color" == propertyName) {
      if (currentText != null)
        currentText.setColix(value);
      return;
    }

    if ("echo" == propertyName) {
      if (currentText != null)
        currentText.setText((String) value);
      return;
    }

    if ("font" == propertyName) {
      if (currentText != null)
        currentText.setFont((Font3D) value);
      return;
    }

    if ("target" == propertyName) {
      //handled by individual types -- echo or hover
      return;
    }

    if ("translucency" == propertyName) {
      Logger.warn("translucent TextShape not implemented");
      return;
    }

    if ("xpos" == propertyName) {
      if (currentText != null)
        currentText.setMovableX(((Integer) value).intValue());
      return;
    }

    if ("ypos" == propertyName) {
      if (currentText != null)
        currentText.setMovableY(currentText.windowHeight - ((Integer) value).intValue());
      return;
    }

    if ("%xpos" == propertyName) {
      if (currentText != null)
        currentText.setMovableX(((Integer) value).intValue() * currentText.windowWidth/100);
      return;
    }

    if ("%ypos" == propertyName) {
      if (currentText != null)
        currentText.setMovableY(currentText.windowHeight * (100 - ((Integer) value).intValue())/100);
      return;
    }
  }
}

