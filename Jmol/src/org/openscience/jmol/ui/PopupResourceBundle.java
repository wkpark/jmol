/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2004  The Jmol Development Team
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
  /*
   * I tried this ... but for some reason did not work on NS 4.78
   * try again later
   *
package org.openscience.jmol.ui;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.lang.ClassLoader;

class PopupResourceBundle {

  Properties propsStructure;
  Properties propsWords;

  private final static String structuresPath =
    "org/openscience/jmol/ui/JmolPopupStructure.properties";
  private final static String wordsPath =
    "org/openscience/jmol/ui/JmolPopupWords.properties";

  PopupResourceBundle() {
    propsStructure = new Properties();
    propsWords = new Properties();
    
    ClassLoader cl = getClass().getClassLoader();

    try {
      InputStream is;

      System.out.println("foo:" + cl.getResource(structuresPath));

      is = cl.getResourceAsStream(structuresPath);
      if (is == null) {
        System.out.println("unable to open:" + structuresPath);
        return;
      }

      propsStructure.load(is);

      is = cl.getResourceAsStream(wordsPath);
      if (is == null) {
        System.out.println("unable to open:" + wordsPath);
        return;
      }
      propsWords.load(is);
      
    } catch (IOException ioe) {
      System.out.println("PopupResourceBundle:" + ioe);
    }
  }

  String getStructure(String key) {
    return propsStructure.getProperty(key);
  }

  String getWord(String key) {
    return propsWords.getProperty(key);
  }
  */
}
