
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
package org.openscience.miniJmol;

import java.util.Hashtable;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;

/**
 * Collection of AtomTypes. No duplicates are allowed. This class is
 * implemented with Hashtable to allow compatability with Java 1.1.
 */
public class AtomTypeSet extends Hashtable {

  /**
   * Adds the specified AtomType to this set if it is not already
   * present.
   *
   * @param at  AtomType to be added to the set.
   * @returns true if the set did not already contain the AtomType.
   */
  boolean add(BaseAtomType at) {

    if (contains(at)) {
      return false;
    } else {
      put(at.getName(), at);
      return true;
    }
  }

  /**
   * Loads AtomTypes from a Reader.
   */
  public void load(InputStream input) throws IOException {

    BufferedReader br1 = new BufferedReader(new InputStreamReader(input),
                           1024);
    clear();
    String line = br1.readLine();
    while (line != null) {
      if (!line.startsWith("#")) {
        add(BaseAtomType.parse(line));
      }
      line = br1.readLine();
    }

  }
}
