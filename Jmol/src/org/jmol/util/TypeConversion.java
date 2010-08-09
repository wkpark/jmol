/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * A simple helper class for converting between types.
 */
public final class TypeConversion {

  /**
   * Helper method for creating a ArrayList<T>[] without warnings.
   * 
   * @param <T> Type of objects in the list.
   * @param size Array size.
   * @return Array of ArrayList<T>
   */
  @SuppressWarnings("unchecked")
  public static <T> List<T>[] createArrayOfArrayList(int size) {
    return new ArrayList[size];
  }

  /**
   * Helper method for creating a Hashtable<K, V>[] without warnings.
   * 
   * @param <K> Type of object for the keys in the map.
   * @param <V> Type of object for the values in the map.
   * @param size Array size.
   * @return Array of Hashtable<K, V>
   */
  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V>[] createArrayOfHashtable(int size) {
    return new Hashtable[size];
  }

  /**
   * Private constructor : utility class.
   */
  private TypeConversion() {
    //
  }
}
