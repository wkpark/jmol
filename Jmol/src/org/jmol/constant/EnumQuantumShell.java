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


/**
 * Enum for quantum shells.
 */
public enum EnumQuantumShell {

  S("S",0,0),
  P("P",1,1),
  SP("SP",2,2),
  D_SPHERICAL("5D",3,3),
  D_CARTESIAN("D",4,3),
  F_SPHERICAL("7F",5,5),
  F_CARTESIAN("F",6,5),
  G_SPHERICAL("9G",7,7),
  G_CARTESIAN("G",8,7),
  H_SPHERICAL("10H",9,9),
  H_CARTESIAN("H",10,9);

  final public static String SUPPORTED_BASIS_FUNCTIONS = "SPLDF";
  
  public final String tag;
  public final int id;
  private final int idSpherical;
  
  private EnumQuantumShell(String tag, int id, int idSpherical) {
    this.tag = tag;
    this.id = id;
    this.idSpherical = idSpherical;
  }
  
  public static int[][] getNewDfCoefMap() {
    return new int[][] { 
        new int[1], //S 
        new int[3], //P
        new int[4], //SP
        new int[5], //D5
        new int[6], //D6
        new int[7], //F7 
        new int[10] //F10
    };
  }

  final public static int getQuantumShellTagID(String tag) {
    if (tag.equals("L"))
      return SP.id;
    EnumQuantumShell item = getQuantumShell(tag);
    return (item == null ? -1 : item.id);
  }

  private static EnumQuantumShell getQuantumShell(String tag) {
    for (EnumQuantumShell item : values())
      if (item.tag.equals(tag))
        return item;
    return null;
  }

  final public static int getQuantumShellTagIDSpherical(String tag) {
    if (tag.equals("L"))
      return SP.idSpherical;
    EnumQuantumShell item = getQuantumShell(tag);
    return (item == null ? -1 : item.idSpherical);
  }

  public static EnumQuantumShell getItem(int id) {
    for (EnumQuantumShell item : values())
      if (item.id == id)
        return item;
    return null;
  }

  final public static String getQuantumShellTag(int id) {
    for (EnumQuantumShell item : values())
      if (item.id == id)
        return item.tag;
    return "" + id;
  }

  //   Don't change the order here unless all supporting arrays are 
  //   also modified. 
  

  final public static String getMOString(float[] lc) {
    StringBuffer sb = new StringBuffer();
    if (lc.length == 2)
      return "" + (int)(lc[0] < 0 ? -lc[1] : lc[1]);
    sb.append('[');
    for (int i = 0; i < lc.length; i += 2) {
      if (i > 0)
        sb.append(", ");
      sb.append(lc[i]).append(" ").append((int) lc[i + 1]);
    }
    sb.append(']');
    return sb.toString();
  }


}
