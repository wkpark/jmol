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

public class PdbChain {

  public char chainID;
  int residueCount;
  PdbResidue[] residues = new PdbResidue[16];
  PdbResidue[] mainchain;

  public PdbChain(char chainID) {
    this.chainID = chainID;
  }

  void freeze() {
    if (residueCount != residues.length) {
      PdbResidue[] t = new PdbResidue[residueCount];
      System.arraycopy(residues, 0, t, 0, residueCount);
      residues = t;
    }
  }

  void addResidue(PdbResidue residue) {
    if (residueCount == residues.length) {
      PdbResidue[] t = new PdbResidue[residueCount * 2];
      System.arraycopy(residues, 0, t, 0, residueCount);
      residues = t;
    }
    residues[residueCount++] = residue;
  }

  int mainchainHelper(boolean addResidues) {
    int mainchainCount = 0;
    outer:
    for (int i = residueCount; --i >= 0; ) {
      PdbResidue residue = residues[i];
      int[] mainchainIndices = residue.mainchainIndices;
      if (mainchainIndices == null)
        continue;
      for (int j = 4; --j >=0; )
        if (mainchainIndices[j] == -1)
          continue outer;
      if (addResidues)
        mainchain[mainchainCount] = residue;
      ++mainchainCount;
    }
    return mainchainCount;
  }

  PdbResidue[] getMainchain() {
    if (mainchain == null) {
      int mainchainCount = mainchainHelper(false);
      if (mainchainCount == residueCount) {
        mainchain = residues;
      } else {
        mainchain = new PdbResidue[mainchainCount];
        if (mainchainCount > 0)
          mainchainHelper(true);
      }
    }
    return mainchain;
  }

  void propogateSecondaryStructure(byte type, int startResidueNumber, int endResidueNumber) {
    for (int i = residueCount; --i >= 0; ) {
      PdbResidue residue = residues[i];
      int resNumber = residue.resNumber;
      if (resNumber >= startResidueNumber && resNumber <= endResidueNumber)
        residue.setStructureType(type);
    }
  }
}
