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
package org.openscience.jmol.app;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.ImageIcon;

/**
 * Provides access to resources (for example, strings and images). This class is
 * a singleton which is retrieved by the getInstance method.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
class JmolResourceHandler {

  private static JmolResourceHandler instance;
  private ResourceBundle stringsResourceBundle;
  private ResourceBundle generalResourceBundle;

  private JmolResourceHandler() {
    String language = "en";
    String country = "";
    String localeString = System.getProperty("user.language");
    if (localeString != null) {
      StringTokenizer st = new StringTokenizer(localeString, "_");
      if (st.hasMoreTokens()) {
        language = st.nextToken();
      }
      if (st.hasMoreTokens()) {
        country = st.nextToken();
      }
    }
    Locale locale = new Locale(language, country);
    stringsResourceBundle =
      ResourceBundle.getBundle("org.openscience.jmol.Properties.Jmol", locale);

    try {
      generalResourceBundle = new PropertyResourceBundle(getClass()
        .getClassLoader()
          .getResourceAsStream("org/openscience/jmol/Properties/Jmol-resources.properties"));
    } catch (IOException ex) {
      throw new RuntimeException(ex.toString());
    }
  }

  public static JmolResourceHandler getInstance() {
    if (instance == null) {
      instance = new JmolResourceHandler();
    }
    return instance;
  }

  public synchronized ImageIcon getIcon(String key) {

    String resourceName = null;
    try {
      resourceName = getString(key);
    } catch (MissingResourceException e) {
    }

    if (resourceName != null) {
      String imageName = "org/openscience/jmol/images/" + resourceName;
      URL imageUrl = this.getClass().getClassLoader().getResource(imageName);
      if (imageUrl != null) {
        return new ImageIcon(imageUrl);
      } else {
        System.err.println("Warning: unable to load " + resourceName
            + " for icon " + key + ".");
      }
    }
    return null;
  }

  public synchronized String getString(String key) {

    String result = null;
    try {
      result = stringsResourceBundle.getString(key);
    } catch (MissingResourceException e) {
    }
    if (result == null) {
      try {
        result = generalResourceBundle.getString(key);
      } catch (MissingResourceException e) {
      }
    }
    return result;
  }

  /**
   * A wrapper for easy detection which strins in the
   * source code are localized.
   */
  public synchronized String translate(String text) {
    StringTokenizer st = new StringTokenizer(text, " ");
    StringBuffer key = new StringBuffer();
    while (st.hasMoreTokens()) {
      key.append(st.nextToken());
      if (st.hasMoreTokens()) {
        key.append("_");
      }
    }
    String translatedText = getString(key.toString());
    return (translatedText != null) ? translatedText : text;
  }

}

