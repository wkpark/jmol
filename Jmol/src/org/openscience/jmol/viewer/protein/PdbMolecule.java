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
import org.openscience.jmol.viewer.datamodel.Frame;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.JmolConstants;

import javax.vecmath.Point3f;
import java.util.Hashtable;
import java.util.Vector;

public class PdbMolecule {
  Frame frame;
  String[] structureRecords;

  public PdbMolecule(Frame frame) {
    this.frame = frame;
  }

  public void setStructureRecords(String[] structureRecords) {
    this.structureRecords = structureRecords;
  }

  public void freeze() {
    if (chainCount != chains.length) {
      PdbChain[] t = new PdbChain[chainCount];
      System.arraycopy(chains, 0, t, 0, chainCount);
      chains = t;
    }
    for (int i = chainCount; --i >= 0; )
      chains[i].freeze();
    propogateSecondaryStructure();
  }

  private void propogateSecondaryStructure() {
    if (structureRecords == null)
      return;
    for (int i = structureRecords.length; --i >= 0; ) {
      String structureRecord = structureRecords[i];
      byte type = JmolConstants.SECONDARY_STRUCTURE_NONE;
      int chainIDIndex = 19;
      int startIndex = 0;
      int endIndex = 0;
      if (structureRecord.startsWith("HELIX ")) {
        type = JmolConstants.SECONDARY_STRUCTURE_HELIX;
        startIndex = 21;
        endIndex = 33;
      } else if (structureRecord.startsWith("SHEET ")) {
        type = JmolConstants.SECONDARY_STRUCTURE_SHEET;
        chainIDIndex = 21;
        startIndex = 22;
        endIndex = 33;
      } else if (structureRecord.startsWith("TURN  ")) {
        type = JmolConstants.SECONDARY_STRUCTURE_TURN;
        startIndex = 20;
        endIndex = 31;
      } else
        continue;

      PdbChain chain = getPdbChain(structureRecord.charAt(chainIDIndex));
      if (chain == null) {
        System.out.println("secondary structure record error");
        continue;
      }

      int start = 0;
      int end = -1;
      try {
        start = Integer.parseInt(structureRecord.substring(startIndex,
                                                     startIndex + 4).trim());
        end = Integer.parseInt(structureRecord.substring(endIndex,
                                                   endIndex + 4).trim());
      } catch (NumberFormatException e) {
        System.out.println("secondary structure record error");
        continue;
      }
      chain.addSecondaryStructure(type, start, end);
    }
  }

  int chainCount = 0;
  PdbChain[] chains = new PdbChain[8];

  public int getChainCount() {
    return chainCount;
  }

  public PdbChain getChain(int chainIndex) {
    return chains[chainIndex];
  }

  public PdbResidue[] getMainchain(int chainIndex) {
    return chains[chainIndex].getMainchain();
  }

  /*
  Point3f[][] midpointsChains;

  public Point3f[][] getMidpointsChains() {
    calcMidpoints();
    return midpointsChains;
  }

  public Point3f[] getMidpointsChain(int chainIndex) {
    calcMidpoints();
    return midpointsChains[chainIndex];
  }
  */

  /*
  void calcMidpoints() {
    //    buildAlphaChains();
    midpointsChains = new Point3f[chainIDCount][];
    for (int i = chainIDCount; --i >= 0; ) {
      Atom[] alphaChain = alphaChains[i];
      int chainLength = alphaChain.length;
      calcMidpoints(alphaChain, midpointsChains[i] = new Point3f[chainLength + 1]);
    }
  }

  void calcMidpoints(Atom[] alphas, Point3f[] midpoints) {
    int chainLength = alphas.length;
    Point3f atomPrevious = alphas[0].point3f;
    midpoints[0] = atomPrevious;
    for (int i = 1; i < chainLength; ++i) {
      Point3f mid = midpoints[i] = new Point3f(atomPrevious);
      atomPrevious = alphas[i].point3f;
      mid.add(atomPrevious);
      mid.scale(0.5f);
    }
    midpoints[chainLength] = atomPrevious;
  }
  */
  
  PdbChain getPdbChain(char chainID) {
    for (int i = chainCount; --i >= 0; ) {
      PdbChain chain = chains[i];
      if (chain.chainID == chainID)
        return chain;
    }
    return null;
  }

  public PdbChain getPdbChain(int chainIndex) {
    return chains[chainIndex];
  }

  PdbChain getOrAllocPdbChain(char chainID) {
    PdbChain chain = getPdbChain(chainID);
    if (chain != null)
      return chain;
    if (chainCount == chains.length) {
      PdbChain[] t = new PdbChain[chainCount * 2];
      System.arraycopy(chains, 0, t, 0, chainCount);
      chains = t;
    }
    return chains[chainCount++] = new PdbChain(chainID);
  }


  char chainIDCurrent = '\uFFFF';
  short resNumberCurrent = -1;
  PdbResidue pdbResidueCurrent;
  short residCurrent;

  void setCurrentResidue(char chainID, short resNumber, String residue3) {
    resNumberCurrent = resNumber;
    chainIDCurrent = chainID;
    PdbChain chain = getOrAllocPdbChain(chainID);
    residCurrent = PdbResidue.lookupResid(residue3);
    pdbResidueCurrent = null;
    if (residCurrent < JmolConstants.RESID_AMINO_MAX) {
      pdbResidueCurrent =
        new PdbResidue(this, chainID, resNumber, residCurrent);
      chain.addResidue(pdbResidueCurrent);
    }
  }

  public PdbAtom getPdbAtom(int atomIndex, String pdbRecord) {
    try {
      char chainID = pdbRecord.charAt(21);
      short resNumber = Short.parseShort(pdbRecord.substring(22, 26).trim());
      if (chainID != chainIDCurrent || resNumber != resNumberCurrent)
        setCurrentResidue(chainID, resNumber, pdbRecord.substring(17, 20));
      return new PdbAtom(atomIndex, pdbRecord,
                         pdbResidueCurrent, residCurrent, resNumber);
    } catch (NumberFormatException e) {
      System.out.println("bad residue number in: " + pdbRecord);
    }
    return null;
  }
}
