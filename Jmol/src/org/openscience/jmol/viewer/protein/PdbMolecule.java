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

  public void finalize(int atomCount, Atom[] atoms) {
    propogateAtomStructure(atomCount, atoms);
  }

  private void propogateAtomStructure(int atomCount, Atom[] atoms) {
    if (structureRecords == null)
      return;

    Hashtable ht = new Hashtable();
    for (int i = atomCount; --i >= 0; ) {
      PdbAtom pdbatom = atoms[i].pdbAtom;
      if (pdbatom == null)
        continue;
      int residueNum = pdbatom.getResidueNumber();
      Integer boxed = new Integer(residueNum);
      Vector v = (Vector)ht.get(boxed);
      if (v == null)
        ht.put(boxed, v = new Vector());
      v.addElement(pdbatom);
    }

    for (int i = structureRecords.length; --i >= 0; ) {
      String structure = structureRecords[i];
      byte type = PdbAtom.STRUCTURE_NONE;
      int startIndex = 0;
      int endIndex = 0;
      if (structure.startsWith("HELIX ")) {
        type = PdbAtom.STRUCTURE_HELIX;
        startIndex = 21;
        endIndex = 33;
      } else if (structure.startsWith("SHEET ")) {
        type = PdbAtom.STRUCTURE_SHEET;
        startIndex = 22;
        endIndex = 33;
      } else if (structure.startsWith("TURN  ")) {
        type = PdbAtom.STRUCTURE_TURN;
        startIndex = 20;
        endIndex = 31;
      } else
        continue;

      int start = 0;
      int end = -1;
      try {
        start = Integer.parseInt(structure.substring(startIndex, startIndex + 4).trim());
        end = Integer.parseInt(structure.substring(endIndex, endIndex + 4).trim());
      } catch (NumberFormatException e) {
        System.out.println("number format exception");
        continue;
      }
        
      for (int j = start; j <= end; ++j) {
        Vector v = (Vector)ht.get(new Integer(j));
        if (v == null)
          continue;
        for (int k = v.size(); --k >= 0; ) {
          PdbAtom pdbatom = (PdbAtom)v.elementAt(k);
          pdbatom.setStructureType(type);
        }
      }
    }
  }

  char chainCurrent;
  int alphaCountCurrent;
  Atom[] alphasCurrent;

  int chainCount = 0;
  char[] chainIDs;
  Atom[][] chains;

  public Atom[][] getAlphaChains() {
    if (chains == null)
      buildAlphaChains();
    return chains;
  }

  void buildAlphaChains() {
    initializeAlphaChains();
    int atomCount = frame.getAtomCount();
    for (int i = 0; i < atomCount; ++i) {
      Atom atom = frame.getAtomAt(i);
      PdbAtom pdbAtom = atom.pdbAtom;
      if (pdbAtom == null || pdbAtom.getAtomID() != 1) // FIXME!! needs a symbol
        continue;
      addAlpha(atom);
    }
    finalizeAlphaChains();
  }

  void initializeAlphaChains() {
    chainCount = 0;
    chainIDs = new char[8];
    chains = new Atom[8][];

    alphaCountCurrent = 0;
    alphasCurrent = new Atom[64];
    chainCurrent = '\u0000';
  }

  void addAlpha(Atom atom) {
    char chain = atom.pdbAtom.getChainID();
    if (chain != chainCurrent) {
      if (alphaCountCurrent > 0)
        addCurrentChain();
      alphaCountCurrent = 0;
      chainCurrent = chain;
    }
    if (alphaCountCurrent == alphasCurrent.length) {
      Atom[] t = new Atom[alphaCountCurrent * 2];
      System.arraycopy(alphasCurrent, 0, t, 0, alphaCountCurrent);
      alphasCurrent = t;
    }
    alphasCurrent[alphaCountCurrent++] = atom;
  }

  void addCurrentChain() {
    if (chainCount == chains.length) {
      Atom[][] t = new Atom[chainCount * 2][];
      System.arraycopy(chains, 0, t, 0, chainCount);
      chains = t;
    }
    Atom[] newChain = new Atom[alphaCountCurrent];
    System.arraycopy(alphasCurrent, 0, newChain, 0, alphaCountCurrent);
    chains[chainCount] = newChain;
    chainIDs[chainCount++] = chainCurrent;
  }

  void finalizeAlphaChains() {
    if (alphaCountCurrent != 0)
      addCurrentChain();
    if (chainCount != chains.length) {
      Atom[][] t = new Atom[chainCount][];
      System.arraycopy(chains, 0, t, 0, chainCount);
      chains = t;
    }
    alphasCurrent = null;
  }
}
