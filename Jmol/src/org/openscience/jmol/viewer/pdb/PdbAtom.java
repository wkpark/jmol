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
package org.openscience.jmol.viewer.pdb;

import org.openscience.jmol.viewer.*;

import java.util.Hashtable;

public class PdbAtom {

  // FIXME mth -- a very quick/dirty/ugly implementation
  // just to get some complex queries running
  public PdbGroup group;
  String name;
  short atomID;
  int atomSerial;
  int temperature;
  boolean isHetero;

  public PdbAtom(int atomIndex, String recordPdb, PdbGroup group) {
    this.group = group;
    isHetero = recordPdb.startsWith("HETATM");

    String t = recordPdb.substring(12, 16);
    name = t.trim();
    atomID = lookupAtomID(t);
    atomSerial = -999999;
    try {
      atomSerial = Integer.parseInt(recordPdb.substring(6, 11).trim());
    } catch (NumberFormatException e) {
    }
    if (atomID < JmolConstants.ATOMID_MAINCHAIN_MAX) {
      if (! group.registerMainchainAtomIndex(atomID, atomIndex))
        atomID += JmolConstants.ATOMID_MAINCHAIN_IMPOSTERS;
    }
    if (recordPdb.length() >= 66) {
      try {
        t = recordPdb.substring(60, 66).trim();
        temperature = (int)(Float.valueOf(t).floatValue() * 100);
      } catch (NumberFormatException e) {
        System.out.println("temp is not a decimal:" + recordPdb);
      }
    }
  }
  
  public boolean isHetero() {
    return isHetero;
  }
  
  public boolean isGroup3(String group3) {
    return group.isGroup3(group3);
  }
  
  public String getName() {
    return name;
  }

  public String getGroup3() {
    return group.getGroup3();
  }
  
  public short getGroupSequence() {
    return group.groupSequence;
  }

  public short getGroupID() {
    return group.groupID;
  }
  
  public short getAtomID() {
    return atomID;
  }

  public String getAtomName() {
    return atomNames[atomID];
  }

  public String getAtomPrettyName() {
    return atomPrettyNames[atomID];
  }

  public boolean isGroup3Match(String strWildcard) {
    return group.isGroup3Match(strWildcard);
  }

  public boolean isAtomNameMatch(String strPattern) {
    int cchPattern = strPattern.length();
    if (cchPattern > 4) {
      System.err.println("atom wildcard length > 4 : " + strPattern);
      return false;
    }
    String strAtomName = atomNames[atomID];
    int cchAtomName = strAtomName.length();
    int ichAtomName = 0;
    while (strAtomName.charAt(ichAtomName) == ' ')
      ++ichAtomName;
    for (int i = 0; i < cchPattern; ++i, ++ichAtomName) {
      char charWild = strPattern.charAt(i);
      if (charWild == '?')
        continue;
      if (ichAtomName >= cchAtomName ||
          charWild != Character.toUpperCase(strAtomName.charAt(ichAtomName)))
        return false;
    }
    while (ichAtomName < cchAtomName)
      if (strAtomName.charAt(ichAtomName++) != ' ')
        return false;
    return true;
  }

  public int getTemperature() {
    return temperature;
  }

  public char getChainID() {
    return group.chainID;
  }

  public int getAtomSerial() {
    return atomSerial;
  }

  public int getSecondaryStructureType() {
    return group.getStructureType();
  }
  
  public final static short atomIDMainchainMax = 3;
  
  private static Hashtable htAtom = new Hashtable();
  static String[] atomNames = new String[128];
  static String[] atomPrettyNames = new String[128];
  static short atomIDMax = 0;

  static {
    // this loop *must* run in forward direction;
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
    if (atomIDMax == atomNames.length) {
      String[] t;
      t = new String[atomIDMax * 2];
      System.arraycopy(atomNames, 0, t, 0, atomIDMax);
      atomNames = t;
      t = new String[atomIDMax * 2];
      System.arraycopy(atomPrettyNames, 0, t, 0, atomIDMax);
      atomPrettyNames = t;
    }
    short atomID = atomIDMax++;
    atomNames[atomID] = name;
    atomPrettyNames[atomID] = prettyName;
    // if already exists then this is an imposter entry
    if (htAtom.get(name) == null)
      htAtom.put(name, new Short(atomID));
    return atomID;
  }

  short lookupAtomID(String strAtom) {
    Short boxedAtomID = (Short)htAtom.get(strAtom);
    if (boxedAtomID != null)
      return boxedAtomID.shortValue();
    return addAtomName(strAtom);
  }
}
