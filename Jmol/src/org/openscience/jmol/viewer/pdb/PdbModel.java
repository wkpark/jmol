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
import org.openscience.jmol.viewer.datamodel.Frame;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.JmolConstants;

import javax.vecmath.Point3f;
import java.util.Hashtable;
import java.util.Vector;

final public class PdbModel {

  PdbFile pdbfile;
  int modelNumber;

  private int chainCount = 0;
  private PdbChain[] chains = new PdbChain[8];


  public PdbModel(PdbFile pdbfile, int modelNumber) {
    this.pdbfile = pdbfile;
    this.modelNumber = modelNumber;
  }

  public void freeze() {
    if (chainCount != chains.length) {
      PdbChain[] t = new PdbChain[chainCount];
      System.arraycopy(chains, 0, t, 0, chainCount);
      chains = t;
    }
    for (int i = chainCount; --i >= 0; )
      chains[i].freeze();
  }

  public void addSecondaryStructure(char chainID, byte type,
                                    int startSeqcode, int endSeqcode) {
    PdbChain chain = getChain(chainID);
    if (chain != null)
      chain.addSecondaryStructure(type, startSeqcode, endSeqcode);
  }

  public int getChainCount() {
    return chainCount;
  }

  public PdbChain getChain(char chainID) {
    for (int i = chainCount; --i >= 0; ) {
      PdbChain chain = chains[i];
      if (chain.chainID == chainID)
        return chain;
    }
    return null;
  }

  public PdbChain getChain(int chainIndex) {
    return chains[chainIndex];
  }

  PdbChain getOrAllocateChain(char chainID) {
    PdbChain chain = getChain(chainID);
    if (chain != null)
      return chain;
    if (chainCount == chains.length) {
      PdbChain[] t = new PdbChain[chainCount * 2];
      System.arraycopy(chains, 0, t, 0, chainCount);
      chains = t;
    }
    return chains[chainCount++] = new PdbChain(this, chainID);
  }
  
  public PdbGroup[] getMainchain(int chainIndex) {
    return chains[chainIndex].getMainchain();
  }

  void calcHydrogenBonds() {
    for (int i = chainCount; --i >= 0; )
      chains[i].calcHydrogenBonds();
  }
}
