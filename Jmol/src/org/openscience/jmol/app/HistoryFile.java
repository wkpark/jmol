/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * The history file contains data from previous uses of Jmol.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
class HistoryFile {

  /**
   * The data stored in the history file.
   */
  private Properties properties = new Properties();

  /**
   * The location of the history file.
   */
  File file;

  /**
   * The information written to the header of the history file.
   */
  String header;

  /**
   * Creates a history file.
   *
   * @param file the location of the file.
   * @param header information written to the header of the file.
   */
  HistoryFile(File file, String header) {
    this.file = file;
    this.header = header;
    load();
  }

  /**
   * Adds the given properties to the history. If a property existed previously,
   * it will be replaced.
   *
   * @param properties the properties to add.
   */
  void addProperties(Properties properties) {

    Enumeration keys = properties.keys();
    boolean modified = false;
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      String value = properties.getProperty(key);
      modified |= addProperty(key, value);
    }
    save();
  }

  /**
   * @return The properties stored in the history file.
   */
  Properties getProperties() {
    return new Properties(properties);
  }

  /**
   * Get the value of a property
   * 
   * @param key Key of the property to find
   * @param defaultValue Default value to use if the property is not found
   * @return The value of the property
   */
  String getProperty(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  /**
   * Adds the given property to the history. If it existed previously,
   * it will be replaced.
   * 
   * @param key Key of the property to add
   * @param value Value of the property
   * @return true if the property is modified
   */
  private boolean addProperty(String key, String value) {
    boolean modified = false;
    Object oldValue = properties.setProperty(key, value);
    if (!value.equals(oldValue)) {
        modified = true;
    }
    return modified;
  }

  /**
   * @param name Window name
   * @return Position of the window stored in the history file
   */
  Point getWindowPosition(String name) {
    Point result = null;
    if (name != null) {
      try {
        String x = getProperty("Jmol.window." + name + ".x", null);
        String y = getProperty("Jmol.window." + name + ".y", null);
        if ((x != null) && (y != null)) {
          int posX = Integer.parseInt(x);
          int posY = Integer.parseInt(y);
          result = new Point(posX, posY);
        }
      } catch (Exception e) {
        //Just return a null result
      }
    }
    return result;
  }

  /**
   * @param name Window name
   * @return Size of the window stored in the history file
   */
  Dimension getWindowSize(String name) {
    Dimension result = null;
    if (name != null) {
      try {
        String w = getProperty("Jmol.window." + name + ".w", null);
        String h = getProperty("Jmol.window." + name + ".h", null);
        if ((w != null) && (h != null)) {
          int dimW = Integer.parseInt(w);
          int dimH = Integer.parseInt(h);
          result = new Dimension(dimW, dimH);
        }
      } catch (Exception e) {
        //Just return a null result
      }
    }
    return result;
  }

  /**
   * @param name Window name
   * @return Visibility of the window stored in the history file
   */
  Boolean getWindowVisibility(String name) {
    Boolean result = null;
    if (name != null) {
      try {
        String v = getProperty("Jmol.window." + name + ".visible", null);
        if (v != null) {
          result = Boolean.valueOf(v);
        }
      } catch (Exception e) {
        //Just return a null result
      }
    }
    return result;
  }

  /**
   * Adds the window positon to the history.
   * If it existed previously, it will be replaced.
   * 
   * @param name Window name
   * @param position Window position
   * @return Tells if the properties are modified
   */
  private boolean addWindowPosition(String name, Point position) {
    boolean modified = false;
    if (name != null) {
      if (position != null) {
        modified |= addProperty("Jmol.window." + name + ".x", "" + position.x);
        modified |= addProperty("Jmol.window." + name + ".y", "" + position.y);
      }
    }
    return modified;
  }

  /**
   * Adds the window size to the history.
   * If it existed previously, it will be replaced.
   * 
   * @param name Window name
   * @param size Window size
   * @return Tells if the properties are modified
   */
  private boolean addWindowSize(String name, Dimension size) {
    boolean modified = false;
    if (name != null) {
      if (size != null) {
        modified |= addProperty("Jmol.window." + name + ".w", "" + size.width);
        modified |= addProperty("Jmol.window." + name + ".h", "" + size.height);
      }
    }
    return modified;
  }

  /**
   * Adds the window visibility to the history.
   * If it existed previously, it will be replaced.
   * 
   * @param name Window name
   * @param visible Window visibilite
   * @return Tells if the properties are modified
   */
  private boolean addWindowVisibility(String name, boolean visible) {
    boolean modified = false;
    if (name != null) {
      modified |= addProperty("Jmol.window." + name + ".visible", "" + visible);
    }
    return modified;
  }

  /**
   * Adds the window informations to the history.
   * If it existed previously, it will be replaced.
   * 
   * @param name Window name
   * @param window Window
   */
  void addWindowInfo(String name, Component window) {
    if (window != null) {
      boolean modified = false;
      modified |= addWindowPosition(name, window.getLocation());
      modified |= addWindowSize(name, window.getSize());
      modified |= addWindowVisibility(name, window.isVisible());
      if (modified) {
        save();
      }
    }
  }

  /**
   * Uses the informations in the history to reposition the window.
   * 
   * @param name Window name
   * @param window Window
   */
  void repositionWindow(String name, Component window) {
    if (window != null) {
      Point position = getWindowPosition(name);
      Dimension size = getWindowSize(name);
      Boolean visible = getWindowVisibility(name);
      if (position != null) {
        window.setLocation(position);
      }
      if (size != null) {
        window.setSize(size);
      }
      if ((visible != null) && (visible.equals(Boolean.TRUE))) {
        window.show();
      }
    }
  }

  /**
   * Loads properties from the history file.
   */
  private void load() {

    try {
      FileInputStream input = new FileInputStream(file);
      properties.load(input);
      input.close();
    } catch (IOException ex) {
      // System.err.println("Error loading history: " + ex);
    }
  }

  /**
   * Saves properties to the history file.
   */
  private void save() {

    try {
      FileOutputStream output = new FileOutputStream(file);
      properties.store(output, header);
      output.close();
    } catch (IOException ex) {
      System.err.println("Error saving history: " + ex);
    }
  }

}
