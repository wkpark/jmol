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

import org.openscience.jmol.viewer.script.Token;

import java.util.Hashtable;

public class PdbAtom {

  public final static byte STRUCTURE_NONE = 0;
  public final static byte STRUCTURE_HELIX = 1;
  public final static byte STRUCTURE_SHEET = 2;
  public final static byte STRUCTURE_TURN = 3;

  // FIXME mth -- a very quick/dirty/ugly implementation
  // just to get some complex queries running
  public String recordPdb;
  byte resid;
  byte atomid;
  public byte structureType = STRUCTURE_NONE;

  public PdbAtom(String recordPdb) {
    this.recordPdb = recordPdb;

    resid = Token.getResid(recordPdb.substring(17, 20));
    atomid = Token.getPdbAtomid(getName());
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
    return Token.getResidue3(resid);
  }

  public int getResidueNumber() {
    int num = -1;
    try {
      num = Integer.parseInt(recordPdb.substring(22, 26).trim());
    } catch (NumberFormatException e)
      {}
    return num;
  }

  public byte getResID() {
    return resid;
  }

  public byte getAtomID() {
    return atomid;
  }

  public String getAtomName() {
    if (atomid >= 0)
      return Token.getPdbAtomName(atomid);
    return getName();
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
    try {
      temp = Float.valueOf(recordPdb.substring(60, 66).trim()).floatValue();
    } catch (NumberFormatException e) {
      System.out.println("temp is not a decimal:" + recordPdb);
    }
    return (int)(temp * 100);
  }

  public char getChain() {
    return recordPdb.charAt(21);
  }

  public void setStructureType(byte structureType) {
    this.structureType = structureType;
  }

  public int getAtomNumber() {
    int num = -999999;
    try {
      num = Integer.parseInt(recordPdb.substring(6, 11).trim());
    } catch (NumberFormatException e)
      {}
    return num;
  }
}
