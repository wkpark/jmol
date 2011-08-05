/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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

package org.jmol.constant;



public enum EnumProteinStructure {
  NOT(-1,"",0xFF808080),
  NONE(0,"none",0xFFFFFFFF),
  TURN(1,"turn",0xFF6080FF),
  SHEET(2,"sheet",0xFFFFC800),
  HELIX(3,"helix",0xFFFF0080),
  DNA(4,"dna",0xFFAE00FE),
  RNA(5,"rna",0xFFFD0162),
  CARBOHYDRATE(6,"carbohydrate",0xFFA6A6FA),
  HELIX_310(7,"helix310",0xFFA00080),
  HELIX_ALPHA(8,"helixalpha",0xFFFF0080),
  HELIX_PI(9,"helixpi",0xFF600080)
  ;
  private String name;
  public byte id;
  private int color;
  
  public final static byte PROTEIN_STRUCTURE_NOT = -1;
  public final static byte PROTEIN_STRUCTURE_NONE = 0;
  public final static byte PROTEIN_STRUCTURE_TURN = 1;
  public final static byte PROTEIN_STRUCTURE_SHEET = 2;
  public final static byte PROTEIN_STRUCTURE_HELIX = 3;
  public final static byte PROTEIN_STRUCTURE_DNA = 4;
  public final static byte PROTEIN_STRUCTURE_RNA = 5;
  public final static byte PROTEIN_STRUCTURE_CARBOHYDRATE = 6;
  public final static byte PROTEIN_STRUCTURE_HELIX_310 = 7;
  public final static byte PROTEIN_STRUCTURE_HELIX_ALPHA = 8;
  public final static byte PROTEIN_STRUCTURE_HELIX_PI = 9;

  private EnumProteinStructure(int id, String name, int color) {
    this.id = (byte) id;
    this.name = name;
    this.color = color;
  }
  
  public final static int[] argbsStructure = new int[11];
  public final static String[] proteinStructureNames = new String[10];
  
  static {
    for (EnumProteinStructure item : values()) {
      if (item.id >= 0)
        proteinStructureNames[item.id] = item.name;
      argbsStructure[item.id + 1] = item.color;
    }
  }

  /****************************************************************
   * In DRuMS, RasMol, and Chime, quoting from
   * http://www.umass.edu/microbio/rasmol/rascolor.htm
   *
   *The RasMol structure color scheme colors the molecule by
   *protein secondary structure.
   *
   *Structure                   Decimal RGB    Hex RGB
   *Alpha helices  red-magenta  [255,0,128]    FF 00 80  *
   *Beta strands   yellow       [255,200,0]    FF C8 00  *
   *
   *Turns          pale blue    [96,128,255]   60 80 FF
   *Other          white        [255,255,255]  FF FF FF
   *
   **Values given in the 1994 RasMol 2.5 Quick Reference Card ([240,0,128]
   *and [255,255,0]) are not correct for RasMol 2.6-beta-2a.
   *This correction was made above on Dec 5, 1998.
   * @param type 
   * @return     0-3 or 7-9, but not dna, rna, carbohydrate
   ****************************************************************/
  public final static byte getProteinStructureType(String type) {
    for (EnumProteinStructure item : values())
      if (type.equalsIgnoreCase(item.name))
        return (item.id < 4 || item.id > 6 ? item.id : NOT.id);
    return NOT.id;
  }

  public final static String getProteinStructureName(int itype, boolean isGeneric) {
    return (itype < 0 || itype > EnumProteinStructure.proteinStructureNames.length ? "" 
        : isGeneric && (itype < 4 || itype > 6) ? "protein" : EnumProteinStructure.proteinStructureNames[itype]);
  }
 
}
