/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.viewer.protein;

import org.openscience.jmol.viewer.*;

import java.util.Hashtable;

public class PdbAtom {

  public final static byte STRUCTURE_NONE = 0;
  public final static byte STRUCTURE_HELIX = 1;
  public final static byte STRUCTURE_SHEET = 2;
  public final static byte STRUCTURE_TURN = 3;

  // FIXME mth -- a very quick/dirty/ugly implementation
  // just to get some complex queries running
  public String recordPdb;
  short resid;
  short atomid;
  int atomNumber;
  public byte structureType = STRUCTURE_NONE;

  public PdbAtom(String recordPdb) {
    this.recordPdb = recordPdb;

    resid = lookupResid(recordPdb.substring(17, 20));
    atomid = lookupAtomid(recordPdb.substring(12, 16));
    atomNumber = -999999;
    try {
      atomNumber = Integer.parseInt(recordPdb.substring(6, 11).trim());
    } catch (NumberFormatException e)
      {}
  }

  public boolean isHetero() {
    return recordPdb.startsWith("HETATM");
  }

  public boolean isResidue(String residue) {
    return recordPdb.regionMatches(true, 17, residue, 0, 3);
  }

  public String getName() {
    return recordPdb.substring(12, 16).trim();
  }

  public String getResidue() {
    return residueNames3[resid];
  }

  public int getResidueNumber() {
    int num = -1;
    try {
      num = Integer.parseInt(recordPdb.substring(22, 26).trim());
    } catch (NumberFormatException e)
      {}
    return num;
  }

  public short getResID() {
    return resid;
  }

  public short getAtomID() {
    return atomid;
  }

  public String getAtomName() {
    return atomNames[atomid];
  }

  public String getAtomPrettyName() {
    return atomPrettyNames[atomid];
  }

  public boolean isResidueNameMatch(String strWildcard) {
    if (strWildcard.length() != 3) {
      System.err.println("residue wildcard length != 3");
      return false;
    }
    String strResidue = getResidue();
    for (int i = 0; i < 3; ++i) {
      char charWild = strWildcard.charAt(i);
      if (charWild == '?')
        continue;
      if (charWild != strResidue.charAt(i))
        return false;
    }
    return true;
  }

  public boolean isAtomNameMatch(String strPattern) {
    int cchPattern = strPattern.length();
    if (cchPattern > 4) {
      System.err.println("atom wildcard length != 4");
      return false;
    }
    String strAtomName = getAtomName();
    int cchAtomName = strAtomName.length();
    for (int i = 0; i < cchPattern; ++i) {
      char charWild = strPattern.charAt(i);
      if (charWild == '?')
        continue;
      if (i >= cchAtomName || charWild != strAtomName.charAt(i))
        return false;
    }
    return true;
  }

  public int getResno() {
    int chain = 0;
    try {
      chain = Integer.parseInt(recordPdb.substring(22, 26).trim());
    } catch (NumberFormatException e) {
      System.out.println("Resno is not an integer:" + recordPdb);
    }
    return chain;
  }

  public int getTemperature() {
    float temp = 0;
    if (recordPdb.length() >= 66) {
      try {
        temp = Float.valueOf(recordPdb.substring(60, 66).trim()).floatValue();
      } catch (NumberFormatException e) {
        System.out.println("temp is not a decimal:" + recordPdb);
      }
    }
    return (int)(temp * 100);
  }

  public char getChainID() {
    return recordPdb.charAt(21);
  }

  public void setStructureType(byte structureType) {
    this.structureType = structureType;
  }

  public int getAtomNumber() {
    return atomNumber;
  }

  /*
    "N",   "N",  // 0
    "CA",  "C\u03B1",
    "C",   "C",
    "O",   "O", // 3
    "C'",  "C'", // 4
    "OT",  "OT", 
    "S",   "S",
    "P",   "P", // 7
    "O1P", "O1P",
    "O2P", "O2P",
    "O5*", "O5'",
    "C5*", "C5'",
    "C4*", "C4'",
    "O4*", "O4'",
    "C3*", "C3'",
    "O3*", "O3'",
    "C2*", "C2'",
    "O2*", "O2'",
    "C1*", "C1'", // 18
    "CA2", "C\u03B12",
    "SG",  "S\u03B3",
    "N1",  "N1",
    "N2",  "N2",
    "N3",  "N3",
    "N4",  "N4",
    "N6",  "N6",
    "O2",  "O2",
    "O4",  "O4",
    "O6",  "O6", // 28
    // kludge for now -- need to come up with a better scheme
    "CB",  "C\u03B2", // 29
    "CG2", "C\u03B32",
    "OG1", "O\u03B31",
  */

  public final static short atomidMainchainMax = 3;

  private static Hashtable htAtom = new Hashtable();
  static String[] atomNames = new String[128];
  static String[] atomPrettyNames = new String[128];
  static short atomidMax = 0;

  static {
    for (int i = 0; i < JmolConstants.predefinedAtomNames4.length; ++i)
      addAtomName(JmolConstants.predefinedAtomNames4[i]);
  }

  static String calcPrettyName(String name) {
    char chBranch = name.charAt(3);
    char chRemote = name.charAt(2);
    switch (chRemote) {
    case 'A':
      chRemote = '\u03B1';
      break;
    case 'B':
      chRemote = '\u03B2';
      break;
    case 'C':
    case 'G':
      chRemote = '\u03B3';
      break;
    case 'D':
      chRemote = '\u03B4';
      break;
    case 'E':
      chRemote = '\u03B5';
      break;
    case 'Z':
      chRemote = '\u03B6';
      break;
    case 'H':
      chRemote = '\u03B7';
    }
    String pretty = name.substring(0, 2).trim();
    if (chBranch != ' ')
      pretty += "" + chRemote + chBranch;
    else
      pretty += chRemote;
    return pretty;
  }

  synchronized static short addAtomName(String name) {
    String prettyName = calcPrettyName(name);
    if (atomidMax == atomNames.length) {
      String[] t;
      t = new String[atomidMax * 2];
      System.arraycopy(atomNames, 0, t, 0, atomidMax);
      atomNames = t;
      t = new String[atomidMax * 2];
      System.arraycopy(atomPrettyNames, 0, t, 0, atomidMax);
      atomPrettyNames = t;
    }
    short atomid = atomidMax++;
    atomNames[atomid] = name;
    atomPrettyNames[atomid] = prettyName;
    htAtom.put(name, new Short(atomid));
    return atomid;
  }

  short lookupAtomid(String strAtom) {
    Short boxedAtomid = (Short)htAtom.get(strAtom);
    if (boxedAtomid != null)
      return boxedAtomid.shortValue();
    return addAtomName(strAtom);
  }

  private static Hashtable htResidue = new Hashtable();
  static String[] residueNames3 = new String[128];
  static short residMax = 0;

  static {
    for (int i = 0; i < JmolConstants.predefinedResidueNames3.length; ++i)
      addResidueName(JmolConstants.predefinedResidueNames3[i]);
  }

  synchronized static short addResidueName(String name) {
    if (residMax == residueNames3.length) {
      String[] t;
      t = new String[residMax * 2];
      System.arraycopy(residueNames3, 0, t, 0, residMax);
      residueNames3 = t;
    }
    short resid = residMax++;
    residueNames3[resid] = name;
    htResidue.put(name, new Short(resid));
    return resid;
  }

  short lookupResid(String strRes3) {
    Short boxedResid = (Short)htResidue.get(strRes3);
    if (boxedResid != null)
      return boxedResid.shortValue();
    return addResidueName(strRes3);
  }

}
