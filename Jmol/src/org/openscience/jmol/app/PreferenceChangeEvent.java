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

import java.util.EventObject;

/**
 * Represents a change to a preference.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class PreferenceChangeEvent extends EventObject {

  /**
   * The key for whom the value has changed.
   */
  private String key;
  
  /**
   * The new value for the preference.
   */
  private String newValue;

  /**
   * Creates a preference change event.
   *
   * @param node the source of the preferences.
   * @param key the key for whom the value has changed.
   * @param newValue the new value for the preference.
   */
  public PreferenceChangeEvent(Preferences node, String key, String newValue) {
    super(node);
    this.key = key;
    this.newValue = newValue;
  }
  
  /**
   * Returns the key for whom the value has changed.
   */
  public String getKey() {
    return key;
  }
  
  /**
   * Returns the new value for the preference.
   */
  public String getNewValue() {
    return newValue;
  }
  
}


