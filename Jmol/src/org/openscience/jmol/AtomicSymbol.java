
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

import java.util.Hashtable;

/**
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
public class AtomicSymbol {

  static Hashtable atomicSymbolMap = new Hashtable();

  static {
    atomicSymbolMap.put("XX", new Integer(0));
    atomicSymbolMap.put("X", new Integer(0));
    atomicSymbolMap.put("H", new Integer(1));
    atomicSymbolMap.put("He", new Integer(2));
    atomicSymbolMap.put("Li", new Integer(3));
    atomicSymbolMap.put("Be", new Integer(4));
    atomicSymbolMap.put("B", new Integer(5));
    atomicSymbolMap.put("C", new Integer(6));
    atomicSymbolMap.put("N", new Integer(7));
    atomicSymbolMap.put("O", new Integer(8));
    atomicSymbolMap.put("F", new Integer(9));
    atomicSymbolMap.put("Ne", new Integer(10));
    atomicSymbolMap.put("Na", new Integer(11));
    atomicSymbolMap.put("Mg", new Integer(12));
    atomicSymbolMap.put("Al", new Integer(13));
    atomicSymbolMap.put("Si", new Integer(14));
    atomicSymbolMap.put("P", new Integer(15));
    atomicSymbolMap.put("S", new Integer(16));
    atomicSymbolMap.put("Cl", new Integer(17));
    atomicSymbolMap.put("Ar", new Integer(18));
    atomicSymbolMap.put("K", new Integer(19));
    atomicSymbolMap.put("Ca", new Integer(20));
    atomicSymbolMap.put("Sc", new Integer(21));
    atomicSymbolMap.put("Ti", new Integer(22));
    atomicSymbolMap.put("V", new Integer(23));
    atomicSymbolMap.put("Cr", new Integer(24));
    atomicSymbolMap.put("Mn", new Integer(25));
    atomicSymbolMap.put("Fe", new Integer(26));
    atomicSymbolMap.put("Co", new Integer(27));
    atomicSymbolMap.put("Ni", new Integer(28));
    atomicSymbolMap.put("Cu", new Integer(29));
    atomicSymbolMap.put("Zn", new Integer(30));
    atomicSymbolMap.put("Ga", new Integer(31));
    atomicSymbolMap.put("Ge", new Integer(32));
    atomicSymbolMap.put("As", new Integer(33));
    atomicSymbolMap.put("Se", new Integer(34));
    atomicSymbolMap.put("Br", new Integer(35));
    atomicSymbolMap.put("Kr", new Integer(36));
    atomicSymbolMap.put("Rb", new Integer(37));
    atomicSymbolMap.put("Sr", new Integer(38));
    atomicSymbolMap.put("Y", new Integer(39));
    atomicSymbolMap.put("Zr", new Integer(40));
    atomicSymbolMap.put("Nb", new Integer(41));
    atomicSymbolMap.put("Mo", new Integer(42));
    atomicSymbolMap.put("Tc", new Integer(43));
    atomicSymbolMap.put("Ru", new Integer(44));
    atomicSymbolMap.put("Rh", new Integer(45));
    atomicSymbolMap.put("Pd", new Integer(46));
    atomicSymbolMap.put("Ag", new Integer(47));
    atomicSymbolMap.put("Cd", new Integer(48));
    atomicSymbolMap.put("In", new Integer(49));
    atomicSymbolMap.put("Sn", new Integer(50));
    atomicSymbolMap.put("Sb", new Integer(51));
    atomicSymbolMap.put("Te", new Integer(52));
    atomicSymbolMap.put("I", new Integer(53));
    atomicSymbolMap.put("Xe", new Integer(54));
    atomicSymbolMap.put("Cs", new Integer(55));
    atomicSymbolMap.put("Ba", new Integer(56));
    atomicSymbolMap.put("La", new Integer(57));
    atomicSymbolMap.put("Ce", new Integer(58));
    atomicSymbolMap.put("Pr", new Integer(59));
    atomicSymbolMap.put("Nd", new Integer(60));
    atomicSymbolMap.put("Pm", new Integer(61));
    atomicSymbolMap.put("Sm", new Integer(62));
    atomicSymbolMap.put("Eu", new Integer(63));
    atomicSymbolMap.put("Gd", new Integer(64));
    atomicSymbolMap.put("Tb", new Integer(65));
    atomicSymbolMap.put("Dy", new Integer(66));
    atomicSymbolMap.put("Ho", new Integer(67));
    atomicSymbolMap.put("Er", new Integer(68));
    atomicSymbolMap.put("Tm", new Integer(69));
    atomicSymbolMap.put("Yb", new Integer(70));
    atomicSymbolMap.put("Lu", new Integer(71));
    atomicSymbolMap.put("Hf", new Integer(72));
    atomicSymbolMap.put("Ta", new Integer(73));
    atomicSymbolMap.put("W", new Integer(74));
    atomicSymbolMap.put("Re", new Integer(75));
    atomicSymbolMap.put("Os", new Integer(76));
    atomicSymbolMap.put("Ir", new Integer(77));
    atomicSymbolMap.put("Pt", new Integer(78));
    atomicSymbolMap.put("Au", new Integer(79));
    atomicSymbolMap.put("Hg", new Integer(80));
    atomicSymbolMap.put("Tl", new Integer(81));
    atomicSymbolMap.put("Pd", new Integer(82));
    atomicSymbolMap.put("Bi", new Integer(83));
    atomicSymbolMap.put("Po", new Integer(84));
    atomicSymbolMap.put("At", new Integer(85));
    atomicSymbolMap.put("Rn", new Integer(86));
    atomicSymbolMap.put("Fr", new Integer(87));
    atomicSymbolMap.put("Ra", new Integer(88));
    atomicSymbolMap.put("Ac", new Integer(89));
    atomicSymbolMap.put("Th", new Integer(90));
    atomicSymbolMap.put("Pa", new Integer(91));
    atomicSymbolMap.put("U", new Integer(92));
    atomicSymbolMap.put("Np", new Integer(93));
    atomicSymbolMap.put("Pu", new Integer(94));
    atomicSymbolMap.put("Am", new Integer(95));
    atomicSymbolMap.put("Cm", new Integer(96));
    atomicSymbolMap.put("Bk", new Integer(97));
    atomicSymbolMap.put("Cf", new Integer(98));
    atomicSymbolMap.put("Es", new Integer(99));
    atomicSymbolMap.put("Fm", new Integer(100));
    atomicSymbolMap.put("Md", new Integer(101));
    atomicSymbolMap.put("No", new Integer(102));
    atomicSymbolMap.put("Lr", new Integer(103));
    atomicSymbolMap.put("Rf", new Integer(104));
    atomicSymbolMap.put("Db", new Integer(105));
    atomicSymbolMap.put("Sg", new Integer(106));
    atomicSymbolMap.put("Bh", new Integer(107));
    atomicSymbolMap.put("Hs", new Integer(108));
    atomicSymbolMap.put("Mt", new Integer(109));
    atomicSymbolMap.put("Uun", new Integer(110));
    atomicSymbolMap.put("Uuu", new Integer(111));
    atomicSymbolMap.put("Uub", new Integer(112));
    atomicSymbolMap.put("Uut", new Integer(113));
    atomicSymbolMap.put("Uuq", new Integer(114));
    atomicSymbolMap.put("Uup", new Integer(115));
    atomicSymbolMap.put("Uuh", new Integer(116));
    atomicSymbolMap.put("Uus", new Integer(117));
    atomicSymbolMap.put("Uuo", new Integer(118));
  }

  static int elementToAtomicNumber(String label) {

    int number = 0;

    if (atomicSymbolMap.containsKey(label)) {
      number = ((Integer) atomicSymbolMap.get(label)).intValue();
    }
    return number;
  }

  static String atomicNumberToSymbol(int atomicNumber) {

    String result = null;
    if ((atomicNumber >= 0) && (atomicNumber < atomicSymbols.length)) {
      result = atomicSymbols[atomicNumber];
    } else {
      result = atomicSymbols[0];
    }
    return result;
  }

  private static String[] atomicSymbols = new String[] {
    "X", "H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg",
    "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca", "Sc", "Ti", "V", "Cr", "Mn",
    "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb",
    "Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In",
    "Sn", "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Ce", "Pr", "Nd", "Pm",
    "Sm", "Eu", "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Hf", "Ta",
    "W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pd", "Bi", "Po", "At",
    "Rn", "Fr", "Ra", "Ac", "Th", "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk",
    "Cf", "Es", "Fm", "Md", "No", "Lr", "Rf", "Db", "Sg", "Bh", "Hs", "Mt",
    "Uun", "Uuu", "Uub", "Uut", "Uuq", "Uup", "Uuh", "Uus", "Uuo"
  };

}

