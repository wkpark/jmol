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
import org.openscience.jmol.viewer.datamodel.Atom;
import java.util.Hashtable;
import javax.vecmath.Point3f;

public class PdbResidue {

  public PdbStructure structure;
  public PdbMolecule pdbmolecule;
  public char chainID;
  public short resNumber;
  public short resid;
  int[] mainchainIndices;

  public PdbResidue(PdbMolecule pdbmolecule, char chainID,
                    short resNumber, short resid) {
    this.pdbmolecule = pdbmolecule;
    this.chainID = chainID;
    this.resNumber = resNumber;
    this.resid = resid;
  }

  public void setStructure(PdbStructure structure) {
    this.structure = structure;
  }

  public byte getStructureType() {
    return structure == null ? 0 : structure.type;
  }

  public static boolean isResidue3(short resid, String residue3) {
    return residueNames3[resid].equalsIgnoreCase(residue3);
  }

  public static String getResidue3(short resid) {
    return residueNames3[resid];
  }

  public short getResidueNumber() {
    return resNumber;
  }

  public short getResID() {
    return resid;
  }

  public static boolean isResidueNameMatch(short resid, String strWildcard) {
    if (strWildcard.length() != 3) {
      System.err.println("residue wildcard length != 3");
      return false;
    }
    String strResidue = residueNames3[resid];
    for (int i = 0; i < 3; ++i) {
      char charWild = strWildcard.charAt(i);
      if (charWild == '?')
        continue;
      if (charWild != strResidue.charAt(i))
        return false;
    }
    return true;
  }

  public char getChainID() {
    return chainID;
  }

  public boolean isHelix() {
    return structure != null &&
      structure.type == JmolConstants.SECONDARY_STRUCTURE_HELIX;
  }

  public boolean isHelixOrSheet() {
    return structure != null &&
      structure.type >= JmolConstants.SECONDARY_STRUCTURE_SHEET;
  }

  /****************************************************************
   * static stuff for residue ids
   ****************************************************************/

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

  static short lookupResid(String strRes3) {
    Short boxedResid = (Short)htResidue.get(strRes3);
    if (boxedResid != null)
      return boxedResid.shortValue();
    return addResidueName(strRes3);
  }

  boolean registerMainchainAtomIndex(short atomid, int atomIndex) {
    if (mainchainIndices == null) {
      mainchainIndices = new int[4];
      for (int i = 4; --i >= 0; )
        mainchainIndices[i] = -1;
    }
    if (mainchainIndices[atomid] != -1) {
      // my residue already has a mainchain atom with this atomid
      // I must be an imposter
      return false;
    }
    mainchainIndices[atomid] = atomIndex;
    return true;
  }

  public int getAlphaCarbonIndex() {
    if (mainchainIndices == null)
      return -1;
    return mainchainIndices[1];
  }

  public Atom getMainchainAtom(int i) {
    int j;
    if (mainchainIndices == null || (j = mainchainIndices[i]) == -1)
      return null;
    return pdbmolecule.frame.getAtomAt(j);
  }

  public Atom getNitrogenAtom() {
    return getMainchainAtom(0);
  }

  public Atom getAlphaCarbonAtom() {
    return getMainchainAtom(1);
  }

  public Atom getCarbonylCarbonAtom() {
    return getMainchainAtom(2);
  }

  public Atom getCarbonylOxygenAtom() {
    return getMainchainAtom(3);
  }

  public Point3f getAlphaCarbonPoint() {
    return getMainchainAtom(1).point3f;
  }
}
