
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

import java.util.Vector;

/**
 * A collection of unique atoms.
 *
 * @author  Bradley A. Smith (bradley@baysmith.com)
 */
public class AtomSet {

  /**
   * The atoms in this set.
   */
  private Vector atoms = new Vector();
  
  /**
   * Adds the specified atom to this set if it is not already present.
   *
   * @param atom the atom to add.
   * @return true if this atom was added.
   */
  public boolean add(Atom atom) {
    if (atoms.contains(atom)) {
      return false;
    }
    atoms.addElement(atom);
    return true;
  }
  
  /**
   * Removes all of the atoms from this set.
   */
  public void clear() {
    atoms.removeAllElements();
  }
  
  /**
   * Returns true if this set contains the specified atom.
   *
   * @param atom the atom for which to look.
   */
  public boolean contains(Atom atom) {
    return atoms.contains(atom);
  }
  
  /**
   * Compares the specified object with this set for equality.
   */
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof AtomSet)) {
      return false;
    }
    AtomSet otherSet = (AtomSet) obj;
    return atoms.equals(otherSet.atoms);
  }
  
  /**
   * Returns the hash code value for this set.
   */
  public int hashCode() {
    return atoms.hashCode();
  }
  
  /**
   * Returns true if this set contains no elements.
   */
  public boolean isEmpty() {
    return atoms.isEmpty();
  }
  
  /**
   * Removes the specified atom from this set if it is present.
   *
   * @param atom the atom to remove.
   * @return true if the atom was removed; otherwise false.
   */
  public boolean remove(Atom atom) {
    return atoms.removeElement(atom);
  }
  
  /**
   * Returns the number of elements in this set.
   */
  public int size() {
    return atoms.size();
  }

}

