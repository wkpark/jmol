/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.*;
import org.openscience.jmol.viewer.pdb.*;
import java.util.BitSet;

public class Backbone extends Mcg {

  Backbone(JmolViewer viewer, Frame frame) {
    super(viewer, frame);
  }

  Mcg.Chain allocateMcgChain(PdbChain pdbChain) {
    return new Chain(pdbChain);
  }

  class Chain extends Mcg.Chain {
    int mainchainLength;
    PdbGroup[] mainchain;
    Frame frame;
    short[] colixes;
    short[] mads;
    int[] atomIndices;

    Chain(PdbChain pdbChain) {
      super(pdbChain);
      PdbGroup[] mainchain = pdbChain.getMainchain();
      mainchainLength = mainchain.length;
      if (mainchainLength < 2) {
        // this is somewhat important ... 
        // don't accept a chain of length 1
        mainchainLength = 0;
        return;
      }
      atomIndices = new int[mainchainLength];
      for (int i = mainchainLength; --i >= 0; )
        atomIndices[i] = mainchain[i].getAlphaCarbonIndex();
      colixes = new short[mainchainLength];
      mads = new short[mainchainLength - 1];
    }

    public void setMad(short mad, BitSet bsSelected) {
      boolean bondSelectionModeOr = viewer.getBondSelectionModeOr();
      for (int i = mainchainLength - 1; --i >= 0; ) {
        if ((bsSelected.get(atomIndices[i]) &&
             bsSelected.get(atomIndices[i + 1]))
            ||
            (bondSelectionModeOr &&
             (bsSelected.get(atomIndices[i]) ||
              bsSelected.get(atomIndices[i + 1]))))
          mads[i] = mad;
      }
    }

    public void setColix(byte palette, short colix, BitSet bsSelected) {
      Frame frame = pdbChain.model.file.frame;
      boolean bondSelectionModeOr = viewer.getBondSelectionModeOr();
      for (int i = mainchainLength; --i >= 0; ) {
        int atomIndex = atomIndices[i];
        if (bsSelected.get(atomIndex))
          colixes[i] =
            palette > JmolConstants.PALETTE_CPK
            ? viewer.getColixAtomPalette(frame.getAtomAt(atomIndex), palette)
            : colix;
      }
    }
  }
}
