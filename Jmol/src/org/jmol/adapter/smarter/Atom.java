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

class Atom implements Cloneable {
  int modelNumber = 1;
  String elementSymbol;
  byte elementNumber = -1;
  String atomName;
  int charge;
  int scatterFactor = Integer.MIN_VALUE;
  float x, y, z;
  float vectorX = Float.NaN, vectorY = Float.NaN, vectorZ = Float.NaN;
  float bfactor = Float.NaN;
  int occupancy = 100;
  boolean isHetero;
  int atomSerial = Integer.MIN_VALUE;
  char chainID = (char)0;
  String group3;
  int sequenceNumber = Integer.MIN_VALUE;
  char insertionCode = (char)0;
  String pdbAtomRecord;

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
          if (isValidElementSymbol(chFirst, chSecond)) {
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
    //   Db Dy
    1 << ('d' - 'a') |
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
    //   Ta Tb Tc Te Th Ti Tl Tm
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
    if (ch >= 'a' && ch <= 'z')
      ch -= 'a' - 'A';
    else if (ch < 'A' || ch > 'Z')
      return false;
    return elementCharMasks[ch - 'A'] < 0;
  }

  static boolean isValidElementSymbol(char chFirst, char chSecond) {
    if (chFirst >= 'a' && chFirst <= 'z')
      chFirst -= 'a' - 'A';
    else if (chFirst < 'A' || chFirst > 'Z')
      return false;
    if (chSecond >= 'A' && chSecond <= 'Z')
      chSecond += 'a' - 'A';
    else if (chSecond < 'a' || chSecond > 'z')
      return false;
    return ((elementCharMasks[chFirst - 'A'] >> (chSecond - 'a')) & 1) != 0;
  }

  static boolean isValidFirstSymbolChar(char ch) {
    if (ch >= 'a' && ch <= 'z')
      ch -= 'a' - 'A';
    else if (ch < 'A' || ch > 'Z')
      return false;
    return elementCharMasks[ch - 'A'] != 0;
  }
}
