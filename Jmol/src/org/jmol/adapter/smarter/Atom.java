/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

package org.jmol.adapter.smarter;

class Atom {
  int modelNumber = 1;
  String elementSymbol;
  String atomName;
  int charge;
  int scatterFactor = Integer.MIN_VALUE;
  float x, y, z;
  float vectorX = Float.NaN, vectorY = Float.NaN, vectorZ = Float.NaN;
  float bfactor = Float.NaN;
  int occupancy = 100;
  String pdbAtomRecord;

  Atom() {
  }

  String getElementSymbol() {
    if (elementSymbol == null)
      if (atomName != null) {
        int len = atomName.length();
        int ichFirst = 0;
        char chFirst = 0;
        while (ichFirst < len &&
               !isValidFirstSymbolChar(chFirst = atomName.charAt(ichFirst)))
          ++ichFirst;
        switch(len - ichFirst) {
        case 0:
          break;
        case 1:
          elementSymbol = atomName.substring(ichFirst, len);
          break;
        default:
          char chSecond = atomName.charAt(ichFirst + 1);
          boolean secondValid = isValidSecondSymbolChar(chFirst, chSecond);
          elementSymbol =
            atomName.substring(ichFirst, ichFirst + (secondValid ? 2 : 1));
        }
      }
    return elementSymbol;
  }

  static boolean isValidFirstSymbolChar(char ch) {
    // make this a little more restricive later, if you want
    return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
  }
  
  static boolean isValidSecondSymbolChar(char chFirst, char ch) {
    // is the second char valid, given the first
    return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
  }
}
