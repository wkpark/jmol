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

public class Backbone {

  JmolViewer viewer;
  Frame frame;
  PdbFile pdbFile;

  Bmodel[] bmodels;

  Backbone(JmolViewer viewer, Frame frame) {
    this.viewer = viewer;
    this.frame = frame;
    pdbFile = frame.pdbFile;
  }

  public void setMad(short mad, BitSet bsSelected) {
    initialize();
    for (int m = bmodels.length; --m >= 0; )
      bmodels[m].setMad(mad, bsSelected);
  }

  public void setColix(byte palette, short colix, BitSet bsSelected) {
    initialize();
    for (int m = bmodels.length; --m >= 0; )
      bmodels[m].setColix(palette, colix, bsSelected);
  }

  void initialize() {
    if (bmodels == null) {
      int tmodelCount = pdbFile == null ? 0 : pdbFile.getModelCount();
      bmodels = new Bmodel[tmodelCount];
      for (int i = tmodelCount; --i >= 0; )
        bmodels[i] = new Bmodel(pdbFile.getModel(i));
    }
  }

  int getBmodelCount() {
    return bmodels.length;
  }

  Bmodel getBmodel(int i) {
    return bmodels[i];
  }

  class Bmodel {
    PdbModel model;
    Bchain[] bchains;
    
    Bmodel(PdbModel model) {
      this.model = model;
      bchains = new Bchain[model.getChainCount()];
      for (int i = bchains.length; --i >= 0; )
        bchains[i] = new Bchain(model.getChain(i));
    }
    
    public void setMad(short mad, BitSet bsSelected) {
      for (int i = bchains.length; --i >= 0; )
        bchains[i].setMad(mad, bsSelected);
    }

    public void setColix(byte palette, short colix, BitSet bsSelected) {
      for (int i = bchains.length; --i >= 0; )
        bchains[i].setColix(palette, colix, bsSelected);
    }

    int getBchainCount() {
      return bchains.length;
    }

    Bchain getBchain(int i) {
      return bchains[i];
    }
  }

  class Bchain {
    PdbChain chain;
    int mainchainLength;
    int[] atomIndices;
    short[] colixes;
    short[] mads;

    Bchain(PdbChain chain) {
      this.chain = chain;
      PdbGroup[] mainchain = chain.getMainchain();
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
      Frame frame = chain.model.file.frame;
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

