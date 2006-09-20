/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.adapter.smarter;

import javax.vecmath.Point3f;
import java.util.BitSet;

class Atom extends Point3f implements Cloneable {
  int atomSetIndex;
  BitSet bsSymmetry;
  int atomSite;
  String elementSymbol;
  short elementNumber = -1;
  String atomName;
  int formalCharge = Integer.MIN_VALUE;
  {set(Float.NaN, Float.NaN, Float.NaN);}
  float partialCharge = Float.NaN;
  float vectorX = Float.NaN, vectorY = Float.NaN, vectorZ = Float.NaN;
  float bfactor = Float.NaN;
  int occupancy = 100;
  boolean isHetero;
  int atomSerial = Integer.MIN_VALUE;
  char chainID = '\0';
  char alternateLocationID = '\0';
  String group3;
  int sequenceNumber = Integer.MIN_VALUE;
  char insertionCode = '\0';

  Atom() {
  }

  Atom cloneAtom() {
    try {
      return (Atom)super.clone();
    } catch (CloneNotSupportedException cnse) {
      return null;
    }
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
        default:
          char chSecond = atomName.charAt(ichFirst + 1);
          if (isValidElementSymbolNoCaseSecondChar(chFirst, chSecond)) {
            elementSymbol = "" + chFirst + chSecond;
            break;
          }
          // fall into
        case 1:
          if (isValidElementSymbol(chFirst))
            elementSymbol = "" + chFirst;
          break;
        }
      }
    return elementSymbol;
  }

  void addVibrationVector(float vectorX, float vectorY, float vectorZ) {
    this.vectorX = vectorX;
    this.vectorY = vectorY;
    this.vectorZ = vectorZ;
  }
  
 
  /**
   * Bits which indicate whether or not an element symbol is valid.
   *<p>
   * If the high bit is set, then it is valid as a standalone char.
   * otherwise, bits 0-25 say whether or not is valid when followed
   * by the letters a-z.
   */
  final static int[] elementCharMasks = {
    //   Ac Ag Al Am Ar As At Au
    1 << ('c' - 'a') |
    1 << ('g' - 'a') |
    1 << ('l' - 'a') |
    1 << ('m' - 'a') |
    1 << ('r' - 'a') |
    1 << ('s' - 'a') |
    1 << ('t' - 'a') |
    1 << ('u' - 'a'),
    // B Ba Be Bh Bi Bk Br
    1 << 31 |
    1 << ('a' - 'a') |
    1 << ('e' - 'a') |
    1 << ('h' - 'a') |
    1 << ('i' - 'a') |
    1 << ('k' - 'a') |
    1 << ('r' - 'a'),
    // C Ca Cd Ce Cf Cl Cm Co Cr Cs Cu
    1 << 31 |
    1 << ('a' - 'a') |
    1 << ('d' - 'a') |
    1 << ('e' - 'a') |
    1 << ('f' - 'a') |
    1 << ('l' - 'a') |
    1 << ('m' - 'a') |
    1 << ('o' - 'a') |
    1 << ('r' - 'a') |
    1 << ('s' - 'a') |
    1 << ('u' - 'a'),
    //  D Db Dy
    1 << 31 |
    1 << ('b' - 'a') |
    1 << ('y' - 'a'),
    //   Er Es Eu
    1 << ('r' - 'a') |
    1 << ('s' - 'a') |
    1 << ('u' - 'a'),
    // F Fe Fm Fr
    1 << 31 |
    1 << ('e' - 'a') |
    1 << ('m' - 'a') |
    1 << ('r' - 'a'),
    //   Ga Gd Ge
    1 << ('a' - 'a') |
    1 << ('d' - 'a') |
    1 << ('e' - 'a'),
    // H He Hf Hg Ho Hs
    1 << 31 |
    1 << ('e' - 'a') |
    1 << ('f' - 'a') |
    1 << ('g' - 'a') |
    1 << ('o' - 'a') |
    1 << ('s' - 'a'),
    // I In Ir
    1 << 31 |
    1 << ('n' - 'a') |
    1 << ('r' - 'a'),
    //j
    0,
    // K Kr
    1 << 31 |
    1 << ('r' - 'a'),
    //   La Li Lr Lu
    1 << ('a' - 'a') |
    1 << ('i' - 'a') |
    1 << ('r' - 'a') |
    1 << ('u' - 'a'),
    //   Md Mg Mn Mo Mt
    1 << ('d' - 'a') |
    1 << ('g' - 'a') |
    1 << ('n' - 'a') |
    1 << ('o' - 'a') |
    1 << ('t' - 'a'),
    // N Na Nb Nd Ne Ni No Np
    1 << 31 |
    1 << ('a' - 'a') |
    1 << ('b' - 'a') |
    1 << ('d' - 'a') |
    1 << ('e' - 'a') |
    1 << ('i' - 'a') |
    1 << ('o' - 'a') |
    1 << ('p' - 'a'),
    // O Os
    1 << 31 |
    1 << ('s' - 'a'),
    // P Pa Pb Pd Pm Po Pr Pt Pu
    1 << 31 |
    1 << ('a' - 'a') |
    1 << ('b' - 'a') |
    1 << ('d' - 'a') |
    1 << ('m' - 'a') |
    1 << ('o' - 'a') |
    1 << ('r' - 'a') |
    1 << ('t' - 'a') |
    1 << ('u' - 'a'),
    //q
    0,
    //   Ra Rb Re Rf Rh Rn Ru
    1 << ('a' - 'a') |
    1 << ('b' - 'a') |
    1 << ('e' - 'a') |
    1 << ('f' - 'a') |
    1 << ('h' - 'a') |
    1 << ('n' - 'a') |
    1 << ('u' - 'a'),
    // S Sb Sc Se Sg Si Sm Sn Sr
    1 << 31 |
    1 << ('b' - 'a') |
    1 << ('c' - 'a') |
    1 << ('e' - 'a') |
    1 << ('g' - 'a') |
    1 << ('i' - 'a') |
    1 << ('m' - 'a') |
    1 << ('n' - 'a') |
    1 << ('r' - 'a'),
    //  T Ta Tb Tc Te Th Ti Tl Tm
    1 << 31 |
    1 << ('a' - 'a') |
    1 << ('b' - 'a') |
    1 << ('c' - 'a') |
    1 << ('e' - 'a') |
    1 << ('h' - 'a') |
    1 << ('i' - 'a') |
    1 << ('l' - 'a') |
    1 << ('m' - 'a'),
    // U
    1 << 31,
    // V
    1 << 31,
    // W
    1 << 31,
    //   Xe Xx
    1 << ('e' - 'a') |
    1 << ('x' - 'a'), // don't know if I should have Xx here or not?
    // Y Yb
    1 << 31 |
    1 << ('b' - 'a'),
    //   Zn Zr
    1 << ('n' - 'a') |
    1 << ('r' - 'a')
  };

  static boolean isValidElementSymbol(char ch) {
    return ch >= 'A' && ch <= 'Z' && elementCharMasks[ch - 'A'] < 0;
  }

  static boolean isValidElementSymbol(char chFirst, char chSecond) {
    if (chFirst < 'A' || chFirst > 'Z' || chSecond < 'a' || chSecond > 'z')
      return false;
    return ((elementCharMasks[chFirst - 'A'] >> (chSecond - 'a')) & 1) != 0;
  }

  static boolean isValidElementSymbolNoCaseSecondChar(char chFirst,
                                                      char chSecond) {
    if (chSecond >= 'A' && chSecond <= 'Z')
      chSecond += 'a' - 'A';
    if (chFirst < 'A' || chFirst > 'Z' || chSecond < 'a' || chSecond > 'z')
      return false;
    return ((elementCharMasks[chFirst - 'A'] >> (chSecond - 'a')) & 1) != 0;
  }

  static boolean isValidFirstSymbolChar(char ch) {
    return ch >= 'A' && ch <= 'Z' && elementCharMasks[ch - 'A'] != 0;
  }

  static boolean isValidElementSymbolNoCaseSecondChar(String str) {
    if (str == null)
      return false;
    int length = str.length();
    if (length == 0)
      return false;
    char chFirst = str.charAt(0);
    if (length == 1)
      return isValidElementSymbol(chFirst);
    if (length > 2)
      return false;
    char chSecond = str.charAt(1);
    return isValidElementSymbolNoCaseSecondChar(chFirst, chSecond);
  }
}
