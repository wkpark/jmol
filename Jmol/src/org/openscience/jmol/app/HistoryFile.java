/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      String value = properties.getProperty(key);
      this.properties.setProperty(key, value);
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

