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
package org.openscience.jmol.viewer.datamodel;
import org.openscience.jmol.viewer.datamodel.Frame;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.*;

import javax.vecmath.Point3f;
import java.util.Hashtable;
import java.util.Vector;

final class Model {

  Mmset mmset;
  int modelNumber;
  int modelIndex;

  private int chainCount = 0;
  private Chain[] chains = new Chain[8];

  private int polymerCount = 0;
  private Polymer[] polymers = new Polymer[8];


  Model(Mmset mmset, int modelIndex, int modelNumber) {
    this.mmset = mmset;
    this.modelIndex = modelIndex;
    this.modelNumber = modelNumber;
  }

  void freeze() {
    //    System.out.println("Mmset.freeze() chainCount=" + chainCount);
    chains = (Chain[])Util.setLength(chains, chainCount);
    for (int i = chainCount; --i >= 0; )
      chains[i].freeze();
    // keep these loops separate so that some day we can search
    // for polymers that involve multiple chains
    for (int i = chainCount; --i >= 0; )
      addPolymer(Polymer.allocatePolymer(this, chains[i]));
    polymers = (Polymer[])Util.setLength(polymers, polymerCount);
  }

  void addSecondaryStructure(byte type, char chainID,
                             int startSeqcode, int endSeqcode) {
    for (int i = polymerCount; --i >= 0; ) {
      Polymer polymer = polymers[i];
      if (polymer.getChainID() == chainID)
        polymer.addSecondaryStructure(type, startSeqcode, endSeqcode);
    }
  }

  int getChainCount() {
    return chainCount;
  }

  int getPolymerCount() {
    return polymerCount;
  }

  int getGroupCount() {
    int groupCount = 0;
    for (int i = chainCount; --i >= 0; )
      groupCount += chains[i].getGroupCount();
    return groupCount;
  }

  Chain getChain(char chainID) {
    for (int i = chainCount; --i >= 0; ) {
      Chain chain = chains[i];
      if (chain.chainID == chainID)
        return chain;
    }
    return null;
  }

  /*
    Chain getChain(int chainIndex) {
    return chains[chainIndex];
    }
  */

  Chain getOrAllocateChain(char chainID) {
    //    System.out.println("chainID=" + chainID + " -> " + (chainID + 0));
    Chain chain = getChain(chainID);
    if (chain != null)
      return chain;
    if (chainCount == chains.length)
      chains = (Chain[])Util.doubleLength(chains);
    return chains[chainCount++] = new Chain(mmset.frame, this, chainID);
  }

  void addPolymer(Polymer polymer) {
    if (polymer == null)
      return;
    if (polymerCount == polymers.length)
      polymers = (Polymer[])Util.doubleLength(polymers);
    polymers[polymerCount++] = polymer;
  }

  Polymer getPolymer(int polymerIndex) {
    return polymers[polymerIndex];
  }

  void calcHydrogenBonds() {
    for (int i = polymerCount; --i >= 0; )
      polymers[i].calcHydrogenBonds();
  }
}
