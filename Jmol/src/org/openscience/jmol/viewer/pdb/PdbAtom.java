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
import org.openscience.jmol.viewer.datamodel.Atom;

import java.util.Hashtable;

public class PdbAtom {

  public PdbGroup group;
  private Atom atom;
  public short atomID;

  public PdbAtom(PdbGroup group, Atom atom) {
    this.group = group;
    this.atom = atom;
    String t = atom.getPdbAtomName4();
    atomID = atom.getAtomID();
  }
  
  public boolean isGroup3(String group3) {
    return group.isGroup3(group3);
  }
  
  public String getGroup3() {
    return group.getGroup3();
  }
  
  public int getSeqcode() {
    return group.seqcode;
  }

  public String getSeqcodeString() {
    return group.getSeqcodeString();
  }

  public short getGroupID() {
    return group.groupID;
  }
  
  public short getAtomID() {
    return atomID;
  }

  public String getAtomName() {
    return atom.getAtomName();
  }

  public String getAtomPrettyName() {
    return atom.getAtomPrettyName();
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
    String strAtomName = Atom.atomNames[atomID];
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

  public char getChainID() {
    return group.chain.chainID;
  }

  public int getModelID() {
    return group.chain.pdbmodel.modelNumber;
  }

  public PdbModel getPdbModel() {
    return group.chain.pdbmodel;
  }

  public PdbChain getPdbChain() {
    return group.chain;
  }

  public PdbGroup getPdbGroup() {
    return group;
  }

  public PdbPolymer getPdbPolymer() {
    return group.polymer;
  }

  public byte getSecondaryStructureType() {
    return group.getStructureType();
  }
  
}
