/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2003  The Jmol Development Team
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
package org.openscience.jmol.ui;

import java.util.ResourceBundle;
import java.util.MissingResourceException;

class PopupResourceBundle {

  ResourceBundle rbStructure;
  ResourceBundle rbWords;

  PopupResourceBundle() {
    rbStructure = ResourceBundle.getBundle("org.openscience.jmol.ui." +
                                           "JmolPopupStructure");
    rbWords = ResourceBundle.getBundle("org.openscience.jmol.ui." +
                                       "JmolPopupWords");
  }

  String getStructure(String key) {
    try {
      return rbStructure.getString(key);
    } catch (MissingResourceException e) {
      return null;
    }
  }

  String getWord(String key) {
    String str = key;
    try {
      str = rbWords.getString(key);
    } catch (MissingResourceException e) {
    }
    return str;
  }
}
