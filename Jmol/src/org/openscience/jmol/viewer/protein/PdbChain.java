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

public class PdbChain {

  public char chainID;
  int firstResidueNumber;
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
    int residueNumber = residue.resNumber;
    if (residueCount == 0)
      firstResidueNumber = residueNumber;
    int residueIndex = residueNumber - firstResidueNumber;
    if (residueIndex < 0) {
      System.out.println("residue out of sequence?");
      return;
    }
    if (residueIndex >= residues.length) {
      PdbResidue[] t = new PdbResidue[residueIndex * 2];
      System.arraycopy(residues, 0, t, 0, residueCount);
      residues = t;
    }
    residues[residueIndex] = residue;
    if (residueIndex >= residueCount)
      residueCount = residueIndex + 1;
  }

  PdbResidue getResidue(int residueNumber) {
    int residueIndex = residueNumber - firstResidueNumber;
    if (residueIndex < 0 || residueIndex >= residueCount)
      return null;
    return residues[residueIndex];
  }

  Point3f getResiduePoint(int residueNumber) {
    return getResidue(residueNumber).getAlphaCarbonAtom().point3f;
  }

  Point3f getResiduePoint(int residueNumber, int mainchainIndex) {
    return getResidue(residueNumber).getMainchainAtom(mainchainIndex).point3f;
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

  void addSecondaryStructure(byte type,
                             int startResidueNumber, int endResidueNumber) {
    for (int i = startResidueNumber; i <= endResidueNumber; ++i)
      if (getResidue(i) == null) {
        System.out.println("structure definition error");
        return;
      }
    PdbStructure structure;
    switch(type) {
    case JmolConstants.SECONDARY_STRUCTURE_HELIX:
      structure = new Helix(this, startResidueNumber, endResidueNumber);
      break;
    case JmolConstants.SECONDARY_STRUCTURE_SHEET:
      structure = new Sheet(this, startResidueNumber, endResidueNumber);
      break;
    case JmolConstants.SECONDARY_STRUCTURE_TURN:
      structure = new Turn(this, startResidueNumber, endResidueNumber);
      break;
    default:
      System.out.println("unrecognized secondary structure type");
      return;
    }
    for (int i = residueCount; --i >= 0; ) {
      PdbResidue residue = residues[i];
      int resNumber = residue.resNumber;
      if (resNumber >= startResidueNumber && resNumber <= endResidueNumber)
        residue.setStructure(structure);
    }
    
  }

  void getAlphaCarbonMidPoint(int residueNumber, Point3f midPoint) {
    int residueIndex = residueNumber - firstResidueNumber;
    if (residueIndex >= residueCount) {
      residueIndex = residueCount - 1;
    } else if (residueIndex > 0) {
      midPoint.set(residues[residueIndex].getAlphaCarbonPoint());
      midPoint.add(residues[residueIndex-1].getAlphaCarbonPoint());
      midPoint.scale(0.5f);
      return;
    }
    midPoint.set(residues[residueIndex].getAlphaCarbonPoint());
  }
}
