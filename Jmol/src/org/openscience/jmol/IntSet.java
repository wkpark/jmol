
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

import java.util.Enumeration;
import java.util.Vector;

/**
 * A collection of unique integer values.
 *
 * @author  Bradley A. Smith (bradley@baysmith.com)
 */
public class IntSet {

  /**
   * The integers in this set.
   */
  private Vector integers = new Vector();
  
  /**
   * Adds the specified integer to this set if it is not already present.
   *
   * @param intValue the integer to add.
   * @return true if this integer was added.
   */
  public boolean add(int intValue) {
    Integer integer = new Integer(intValue);
    if (integers.contains(integer)) {
      return false;
    }
    integers.addElement(integer);
    return true;
  }
  
  /**
   * Removes all of the integers from this set.
   */
  public void clear() {
    integers.removeAllElements();
  }
  
  /**
   * Returns true if this set contains the specified integer.
   *
   * @param intValue the integer for which to look.
   */
  public boolean contains(int intValue) {
    return integers.contains(new Integer(intValue));
  }
  
  /**
   * Compares the specified object with this set for equality.
   */
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof IntSet)) {
      return false;
    }
    IntSet otherSet = (IntSet) obj;
    return integers.equals(otherSet.integers);
  }
  
  /**
   * Returns the hash code value for this set.
   */
  public int hashCode() {
    return integers.hashCode();
  }
  
  /**
   * Returns true if this set contains no elements.
   */
  public boolean isEmpty() {
    return integers.isEmpty();
  }
  
  /**
   * Removes the specified integer from this set if it is present.
   *
   * @param intValue the integer to remove.
   * @return true if the integer was removed; otherwise false.
   */
  public boolean remove(int intValue) {
    return integers.removeElement(new Integer(intValue));
  }
  
  /**
   * Returns the number of elements in this set.
   */
  public int size() {
    return integers.size();
  }

  /**
   * Returns the integers in this set.
   */
  public int[] elements() {
    int[] result = new int[integers.size()];
    for (int i = 0; i < result.length; ++i) {
      result[i] = ((Integer)integers.elementAt(i)).intValue();
    }
    return result;
  }
}

