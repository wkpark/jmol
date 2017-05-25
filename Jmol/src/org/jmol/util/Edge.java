/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-26 16:57:51 -0500 (Thu, 26 Apr 2007) $
 * $Revision: 7502 $
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.util;


public abstract class Edge {

  
  /**
   * Extended Bond Definition Types
   * 
   * Originally these were short 16-bit values
   *
   */
  
  // 1111 1111 1100 0000 0000
  // 9876 5432 1098 7654 3210
  // || CIP stereochemistry mask (unk)  3 << 18  0xC0000
  // |  CIP stereochemistry Z           2 << 18  0x80000
  //  | CIP stereochemistry E           1 << 18  0x40000
  //   | new connection                 1 << 17  0x20000
  //    | render as single              1 << 16  0x10000
  //    |   PyMOL render single         2 << 15  0x18000 + covalent order 
  //    | | PyMOL render multiple       3 << 15  0x18000 + covalent order 
  //      | strut                       1 << 15  0x08000
  //    |  nnm m            | atropisomer        0x10001 + (nnmm << 11)
  //       ||| | Hydrogen bond          F << 11  0x03800
  //            |Stereo                 1 << 10  0x00400   
  //             |Aromatic              1 << 9   0x00200
  //              |Sulfur-Sulfur        1 << 8   0x00100
  //                ||| Partial n       7 << 5   0x00E00
  //                   | |||| Partial m          0x0001F
  //                      ||| Covalent order     0x00007
  //            ||| |||| |||| Covalent           0x003FF
  // 0000 0000 0000 0001 0001 UNSPECIFIED
  // 0000 1111 1111 1111 1111 ANY
  // 0001 1111 1111 1111 1111 NULL

  public final static int BOND_RENDER_SINGLE   = 0x10000;

  public final static int TYPE_ATROPISOMER     = 0x10001;
  public final static int TYPE_ATROPISOMER_REV = 0x10002; // only used by SMILES, for ^^nm-
  private final static int ATROPISOMER_SHIFT   = 11;

  // NOT IMPLEMENTED
//  public final static int BOND_CIP_STEREO_MASK  = 0xC0000; // 3 << 18
//  public final static int BOND_CIP_STEREO_UNK   = 0xC0000; // 3 << 18 same as mask
//  public final static int BOND_CIP_STEREO_E     = 0x80000; // 2 << 18
//  public final static int BOND_CIP_STEREO_Z     = 0x40000; // 1 << 18
//  public final static int BOND_CIP_STEREO_SHIFT = 18;


  public final static int BOND_STEREO_MASK   = 0x400; // 1 << 10
  public final static int BOND_STEREO_NEAR   = 0x401; // for JME reader and SMILES
  public final static int BOND_STEREO_FAR    = 0x411; // for JME reader and SMILES
  public final static int BOND_AROMATIC_MASK   = 0x200; // 1 << 9
  public final static int BOND_AROMATIC_SINGLE = 0x201; // same as single
  public final static int BOND_AROMATIC_DOUBLE = 0x202; // same as double
  public final static int BOND_AROMATIC        = 0x203; // same as partial 2.1
  public final static int BOND_SULFUR_MASK   = 0x100; // 1 << 8; will be incremented
  public final static int BOND_PARTIAL_MASK  = 0xE0;  // 7 << 5;
  public final static int BOND_PARTIAL01     = 0x21;
  public final static int BOND_PARTIAL12     = 0x42;
  public final static int BOND_PARTIAL23     = 0x61;
  public final static int BOND_PARTIAL32     = 0x64;
  public final static int BOND_COVALENT_MASK = 0x3FF; // MUST be numerically correct in 0x7 if not partial
  public final static int BOND_COVALENT_SINGLE = 1;   // and in 0xE0 if partial
  public final static int BOND_COVALENT_DOUBLE = 2;
  public final static int BOND_COVALENT_TRIPLE = 3;
  public final static int BOND_COVALENT_QUADRUPLE = 4;
  public final static int BOND_COVALENT_QUINTUPLE = 5;
  public final static int BOND_COVALENT_sextuple  = 6;
  public final static int BOND_ORDER_UNSPECIFIED = 0x11;
  public final static int BOND_ORDER_ANY     = 0x0FFFF;
  public final static int BOND_ORDER_NULL    = 0x1FFFF;
  public static final int BOND_STRUT         = 0x08000;
  public final static int BOND_PYMOL_NOMULT  = 0x10000;
  public static final int BOND_PYMOL_MULT    = 0x18000;
  public final static int BOND_NEW           = 0x20000;
  public final static int BOND_HBOND_SHIFT   = 11;
  public final static int BOND_HYDROGEN_MASK = 0xF << 11;
  public final static int BOND_H_REGULAR     = 1 << 11;
  public final static int BOND_H_CALC_MASK   = 0xE << 11; // excludes regular
  public final static int BOND_H_CALC        = 2 << 11;
  public final static int BOND_H_PLUS_2      = 3 << 11;
  public final static int BOND_H_PLUS_3      = 4 << 11;
  public final static int BOND_H_PLUS_4      = 5 << 11;
  public final static int BOND_H_PLUS_5      = 6 << 11;
  public final static int BOND_H_MINUS_3     = 7 << 11;
  public final static int BOND_H_MINUS_4     = 8 << 11;
  public final static int BOND_H_NUCLEOTIDE  = 9 << 11;
    
  private final static int[] argbsHbondType = {
    0xFFFF69B4, // 0  unused - pink
    0xFFFFFF00, // 1  regular yellow
    0xFFFFFF00, // 2  calc -- unspecified; yellow
    0xFFFFFFFF, // 3  +2 white
    0xFFFF00FF, // 4  +3 magenta
    0xFFFF0000, // 5  +4 red
    0xFFFFA500, // 6  +5 orange
    0xFF00FFFF, // 7  -3 cyan
    0xFF00FF00, // 8  -4 green
    0xFFFF8080, // 9  nucleotide
  };

  public int index = -1;
  public int order;

  abstract public int getAtomIndex1();

  abstract public int getAtomIndex2();

  abstract public int getCovalentOrder();

  abstract public Node getOtherAtomNode(Node atom);

  abstract public boolean isCovalent();

  abstract public boolean isHydrogen();

  public static int getArgbHbondType(int order) {
    int argbIndex = ((order & BOND_HYDROGEN_MASK) >> BOND_HBOND_SHIFT);
    return argbsHbondType[argbIndex];
  }

  /**
   * used for formatting labels and in the connect PARTIAL command
   *  
   * @param order
   * @return a string representation to preserve float n.m
   */
  public final static String getBondOrderNumberFromOrder(int order) {
    order &= ~BOND_NEW;
    if (order == BOND_ORDER_NULL || order == BOND_ORDER_ANY)
      return "0"; // I don't think this is possible
    if (isOrderH(order) || isAtropism(order) || (order & BOND_SULFUR_MASK) != 0)
      return EnumBondOrder.SINGLE.number;
    if ((order & BOND_PARTIAL_MASK) != 0)
      return (order >> 5) + "." + (order & 0x1F);
    return EnumBondOrder.getNumberFromCode(order);
  }

  public final static String getCmlBondOrder(int order) {
    String sname = getBondOrderNameFromOrder(order);
    switch (sname.charAt(0)) {
    case 's':
    case 'd':
    case 't':
      return "" + sname.toUpperCase().charAt(0);
    case 'a':
      if (sname.indexOf("Double") >= 0)
        return "D";
      else if (sname.indexOf("Single") >= 0)
        return "S";
      return "aromatic";
    case 'p':
      if (sname.indexOf(" ") >= 0)
        return sname.substring(sname.indexOf(" ") + 1);
      return "partial12";
    }
    return null;
  }

  public final static String getBondOrderNameFromOrder(int order) {
    order &= ~BOND_NEW;
    switch (order) {
    case BOND_ORDER_ANY:
    case BOND_ORDER_NULL:
      return "";
    case BOND_STRUT:
      return EnumBondOrder.STRUT.name;
    case BOND_COVALENT_SINGLE:
      return EnumBondOrder.SINGLE.name;
    case BOND_COVALENT_DOUBLE:
      return EnumBondOrder.DOUBLE.name;
    }
    if ((order & BOND_PARTIAL_MASK) != 0)
      return "partial " + getBondOrderNumberFromOrder(order);
    if (isOrderH(order))
      return EnumBondOrder.H_REGULAR.name;
    if ((order & TYPE_ATROPISOMER) == TYPE_ATROPISOMER) {
      int code = getAtropismCode(order);
      return "atropisomer_" + (code / 4) + (code % 4);
    }
      
    if ((order & BOND_SULFUR_MASK) != 0)
      return EnumBondOrder.SINGLE.name;
    return EnumBondOrder.getNameFromCode(order);
  }
  
  public static int getAtropismOrder(int nn, int mm) {
    return getAtropismOrder12(((nn + 1) << 2) + mm + 1);
  }

  public static int getAtropismOrder12(int nnmm) {
    return ((nnmm << ATROPISOMER_SHIFT) | TYPE_ATROPISOMER);
  }

  private static int getAtropismCode(int order) {
      return  (order >> (ATROPISOMER_SHIFT)) & 0xF;
  }

  public static Node getAtropismNode(int order, Node a1, boolean isFirst) {
    int i1 = (order >> (ATROPISOMER_SHIFT + (isFirst ? 2 : 0))) & 3;
    return a1.getEdges()[i1 - 1].getOtherAtomNode(a1);
  }

  public static boolean isAtropism(int order) {
    return (order & TYPE_ATROPISOMER) == TYPE_ATROPISOMER;
  }
  
  public static boolean isOrderH(int order) {
    return (order & BOND_HYDROGEN_MASK) != 0 && (order & TYPE_ATROPISOMER) == 0;
  }

  public final static int getPartialBondDotted(int order) {
    return (order & 0x1F);
  }

  public final static int getPartialBondOrder(int order) {
    return ((order & ~BOND_NEW) >> 5);
  }

  protected final static int getCovalentBondOrder(int order) {
    if ((order & BOND_COVALENT_MASK) == 0)
      return 0;
    order &= ~BOND_NEW; 
    if ((order & BOND_PARTIAL_MASK) != 0)
      return getPartialBondOrder(order);
    if ((order & BOND_SULFUR_MASK) != 0)
      order &= ~BOND_SULFUR_MASK;
    if ((order & 0xF8) != 0) // "ANY"
      order = 1;
    return order & 7;
  }

  public final static int getBondOrderFromFloat(float fOrder) {
    switch ((int) (fOrder * 10)) {
    case 10:
      return BOND_COVALENT_SINGLE;
    case 5:
    case -10:
      return BOND_PARTIAL01;
    case 15:
      return BOND_AROMATIC;
    case -15:
      return BOND_PARTIAL12;
    case 20:
      return BOND_COVALENT_DOUBLE;
    case 25:
      return BOND_PARTIAL23;
    case -25:
      return BOND_PARTIAL32;
    case 30:
      return BOND_COVALENT_TRIPLE;
    case 40:
      return BOND_COVALENT_QUADRUPLE;
    }
    return BOND_ORDER_NULL;
  }

  public static int getBondOrderFromString(String name) {
    int order = EnumBondOrder.getCodeFromName(name);
    try {
      if (order == BOND_ORDER_NULL && name.length() == 14
          && name.toLowerCase().startsWith("atropisomer_"))
        order = getAtropismOrder(Integer.parseInt(name.substring(12, 13)), Integer.parseInt(name.substring(13, 14)));
    } catch (NumberFormatException e) {
      // BOND_ORDER_NULL
    }
    return order;
  }

  private enum EnumBondOrder {

    SINGLE(BOND_COVALENT_SINGLE,"1","single"),
    DOUBLE(BOND_COVALENT_DOUBLE,"2","double"),
    TRIPLE(BOND_COVALENT_TRIPLE,"3","triple"),
    QUADRUPLE(BOND_COVALENT_QUADRUPLE,"4","quadruple"),
    QUINTUPLE(BOND_COVALENT_QUINTUPLE,"5","quintuple"),
    sextuple(BOND_COVALENT_sextuple,"6","sextuple"),
    AROMATIC(BOND_AROMATIC,"1.5","aromatic"),
    STRUT(BOND_STRUT,"1","struts"),
    H_REGULAR(BOND_H_REGULAR,"1","hbond"),
    PARTIAL01(BOND_PARTIAL01,"0.5","partial"),
    PARTIAL12(BOND_PARTIAL12,"1.5","partialDouble"),
    PARTIAL23(BOND_PARTIAL23,"2.5","partialTriple"),
    PARTIAL32(BOND_PARTIAL32,"2.5","partialTriple2"),
    AROMATIC_SINGLE(BOND_AROMATIC_SINGLE,"1","aromaticSingle"),
    AROMATIC_DOUBLE(BOND_AROMATIC_DOUBLE,"2","aromaticDouble"),
    ATROPISOMER(TYPE_ATROPISOMER, "1", "atropisomer"),
    UNSPECIFIED(BOND_ORDER_UNSPECIFIED,"1","unspecified");

    private int code;
    protected String number;
    protected String name;

    private EnumBondOrder(int code, String number, String name) {
      this.code = code;
      this.number = number;
      this.name = name;
    }

    protected static int getCodeFromName(String name) {
      for (EnumBondOrder item : values())
        if (item.name.equalsIgnoreCase(name))
          return item.code;
      return BOND_ORDER_NULL;
    }

    protected static String getNameFromCode(int code) {
      for (EnumBondOrder item: values())
        if (item.code == code)
          return item.name;
      return "?";
    }

    protected static String getNumberFromCode(int code) {
      for (EnumBondOrder item: values())
        if (item.code == code)
          return item.number;
      return "?";
    }

  }

  public void setCIPChirality(int c) {
    // default is no action
  }

  public String getCIPChirality(boolean doCalculate) {
    return "";
  }

}
