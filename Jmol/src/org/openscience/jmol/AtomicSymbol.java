
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

import java.util.Hashtable;

/**
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
public class AtomicSymbol {

  static Hashtable atomicSymbols = new Hashtable();

  static {
    atomicSymbols.put(new String("XX"), new Integer(0));
    atomicSymbols.put("H", new Integer(1));
    atomicSymbols.put("He", new Integer(2));
    atomicSymbols.put("Li", new Integer(3));
    atomicSymbols.put("Be", new Integer(4));
    atomicSymbols.put("B", new Integer(5));
    atomicSymbols.put("C", new Integer(6));
    atomicSymbols.put("N", new Integer(7));
    atomicSymbols.put("O", new Integer(8));
    atomicSymbols.put("F", new Integer(9));
    atomicSymbols.put("Ne", new Integer(10));
    atomicSymbols.put("Na", new Integer(11));
    atomicSymbols.put("Mg", new Integer(12));
    atomicSymbols.put("Al", new Integer(13));
    atomicSymbols.put("Si", new Integer(14));
    atomicSymbols.put("P", new Integer(15));
    atomicSymbols.put("S", new Integer(16));
    atomicSymbols.put("Cl", new Integer(17));
    atomicSymbols.put("Ar", new Integer(18));
    atomicSymbols.put("K", new Integer(19));
    atomicSymbols.put("Ca", new Integer(20));
    atomicSymbols.put("Sc", new Integer(21));
    atomicSymbols.put("Ti", new Integer(22));
    atomicSymbols.put("V", new Integer(23));
    atomicSymbols.put("Cr", new Integer(24));
    atomicSymbols.put("Mn", new Integer(25));
    atomicSymbols.put("Fe", new Integer(26));
    atomicSymbols.put("Co", new Integer(27));
    atomicSymbols.put("Ni", new Integer(28));
    atomicSymbols.put("Cu", new Integer(29));
    atomicSymbols.put("Zn", new Integer(30));
    atomicSymbols.put("Ga", new Integer(31));
    atomicSymbols.put("Ge", new Integer(32));
    atomicSymbols.put("As", new Integer(33));
    atomicSymbols.put("Se", new Integer(34));
    atomicSymbols.put("Br", new Integer(35));
    atomicSymbols.put("Kr", new Integer(36));
  }

  static int elementToAtomicNumber(String label) {

    int number = 0;

    if (atomicSymbols.containsKey(label)) {
      number = ((Integer) atomicSymbols.get(label)).intValue();
    }
    return number;
  }
}

