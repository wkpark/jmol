
/*
 * Copyright 2002 The Jmol Development Team
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

import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;

/**
 * Stores user and application preferences and configuration data.
 * The interface is modeled after the new Preferences API in Java 1.4
 * so that eventual migration will be easier.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class Preferences {

  /**
   * The singleton instance of preferences.
   */
  private static Preferences instance = null;
  
  /**
   * Listeners for preference changes.
   */
  Set listeners = new HashSet();
  
  /**
   * The storage for preference values.
   */
  private Properties properties = new Properties();
  
  /**
   * Creates preferences. Private to ensure only a singleton is created.
   */
  private Preferences() {
    properties = System.getProperties();
    try {
      FileInputStream input = new FileInputStream(Jmol.UserPropsFile);
      properties.load(new BufferedInputStream(input));
      input.close();
    } catch (Exception ex) {
    }
  }

  /**
   * Returns the Preferences for this application.
   */
  public static Preferences getInstance() {
    if (instance == null) {
      instance = new Preferences();
    }
    return instance;
  }
  
  /**
   * Adds the given listener for preference changes.
   *
   * @param listener the listener to be added.
   */
  public void addPreferenceChangeListener(PreferenceChangeListener listener) {
    listeners.add(listener);
  }

  /**
   * Removes the given listener for preference changes.
   *
   * @param listener the listener to be removed.
   */
  public void removePreferenceChangeListener(PreferenceChangeListener listener) {
    listeners.remove(listener);
  }

  /**
   * Sends a preference change event to all listeners.
   */
  private void firePreferenceChange(String key, String newValue) {
    PreferenceChangeEvent event = new PreferenceChangeEvent(this, key, newValue);
    Iterator listenerIter = listeners.iterator();
    while (listenerIter.hasNext()) {
      PreferenceChangeListener listener =
        (PreferenceChangeListener) listenerIter.next();
      listener.preferenceChange(event);
    }
  }
  
  /**
   * Returns the value associated with the specified key.
   *
   * @param key the key whose value is returned.
   * @param def value returned if the key is not found.
   */
  public String get(String key, String def) {
    if (properties.containsKey(key)) {
      return properties.getProperty(key);
    }
    return def;
  }
  
  /**
   * Returns the boolean value associated with the specified key.
   *
   * @param key the key whose value is returned.
   * @param def value returned if the key is not found.
   */
  public boolean getBoolean(String key, boolean def) {
    if (properties.containsKey(key)) {
      String value = properties.getProperty(key);
      if (value.equalsIgnoreCase("true")) {
        return true;
      } else if (value.equalsIgnoreCase("false")) {
        return false;
      }
    }
    return def;
  }
  
  /**
   * Returns the double value associated with the specified key.
   *
   * @param key the key whose value is returned.
   * @param def value returned if the key is not found.
   */
  public double getDouble(String key, double def) {
    if (properties.containsKey(key)) {
      try {
        String value = properties.getProperty(key);
        return Double.parseDouble(value);
      } catch (NumberFormatException ex) {
        // Ignore and return default value.
      }
    }
    return def;
  }
  
  /**
   * Returns the int value associated with the specified key.
   *
   * @param key the key whose value is returned.
   * @param def value returned if the key is not found.
   */
  public int getInt(String key, int def) {
    if (properties.containsKey(key)) {
      try {
        String value = properties.getProperty(key);
        return Integer.parseInt(value);
      } catch (NumberFormatException ex) {
        // Ignore and return default value.
      }
    }
    return def;
  }
  
  /**
   * Returns the long value associated with the specified key.
   *
   * @param key the key whose value is returned.
   * @param def value returned if the key is not found.
   */
  public long getLong(String key, long def) {
    if (properties.containsKey(key)) {
      try {
        String value = properties.getProperty(key);
        return Long.parseLong(value);
      } catch (NumberFormatException ex) {
        // Ignore and return default value.
      }
    }
    return def;
  }

  /**
   * Stores the value associated with the specified key.
   *
   * @param key the key whose value is stored.
   * @param value the value to be assigned to the given key.
   */
  public void put(String key, String value) {
    storeValue(key, value);
  }
  
  /**
   * Stores the boolean value associated with the specified key.
   *
   * @param key the key whose value is stored.
   * @param value the value to be assigned to the given key.
   */
  public void putBoolean(String key, boolean value) {
    storeValue(key, new Boolean(value).toString());
  }
  
  /**
   * Stores the double value associated with the specified key.
   *
   * @param key the key whose value is stored.
   * @param value the value to be assigned to the given key.
   */
  public void putDouble(String key, double value) {
    storeValue(key, Double.toString(value));
  }
  
  /**
   * Stores the int value associated with the specified key.
   *
   * @param key the key whose value is stored.
   * @param value the value to be assigned to the given key.
   */
  public void putInt(String key, int value) {
    storeValue(key, Integer.toString(value));
  }
  
  /**
   * Stores the long value associated with the specified key.
   *
   * @param key the key whose value is stored.
   * @param value the value to be assigned to the given key.
   */
  public void putLong(String key, long value) {
    storeValue(key, Long.toString(value));
  }
  
  /**
   * Saves the preferences to the persistant storage (a file).
   */
  public void sync() {

    try {
      FileOutputStream output = new FileOutputStream(Jmol.UserPropsFile);
      properties.store(output, "Jmol");
      output.close();
    } catch (Exception ex) {
      System.out.println("Error saving preferences" + ex);
    }
  }

  /**
   * Stores the given value for the given key.
   *
   * @param key the key whose value is stored.
   * @param value the new value.
   */
  private void storeValue(String key, String value) {
    properties.put(key, value);
    firePreferenceChange(key, value);
  }

}
