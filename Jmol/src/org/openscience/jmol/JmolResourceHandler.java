
/*
 * Copyright 2001 The Jmol Development Team
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
package org.openscience.jmol;

import java.io.PrintStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.text.*;
import java.util.*;
import java.net.*;
import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.ImageIcon;

class JmolResourceHandler {

  private static ResourceBundle rb;
  private String baseKey;

  public static void initialize(String resources) {
    JmolResourceHandler.rb = ResourceBundle.getBundle(resources);
  }

  JmolResourceHandler(String string) {
    baseKey = string;
  }

  synchronized ImageIcon getIcon(String key) {

    String iname = null;    // Image name
    String resourceName = null;
    try {
      resourceName = rb.getString(getQualifiedKey(key));
      iname = "org/openscience/jmol/images/" + resourceName;
    } catch (MissingResourceException e) {

      // Ignore
    }
    if (iname != null) {
      URL imageUrl = ClassLoader.getSystemResource(iname);
      if (imageUrl != null) {
        return new ImageIcon(imageUrl);
      } else {
        System.err.println("Warning: unable to load " + resourceName
                + " for icon " + key + ".");
      }
    }
    return null;
  }

  synchronized String getString(String string) {

    String ret = null;
    try {
      ret = rb.getString(getQualifiedKey(string));
    } catch (MissingResourceException e) {
    }
    if (ret != null) {
      return ret;
    }
    return null;
  }

  synchronized Object getObject(String string) {

    Object o = null;
    try {
      o = rb.getObject(getQualifiedKey(string));
    } catch (MissingResourceException e) {
    }
    if (o != null) {
      return o;
    }
    return null;
  }

  synchronized String getQualifiedKey(String string) {
    return baseKey + "." + string;
  }

}
