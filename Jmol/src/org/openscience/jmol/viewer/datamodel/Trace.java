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
import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.g3d.Colix;
import org.openscience.jmol.viewer.pdb.*;
import javax.vecmath.Point3f;
import java.util.BitSet;

public class Trace {

  JmolViewer viewer;
  Frame frame;
  PdbFile pdbFile;

  Tmodel[] tmodels;

  int chainCount;
  short[][] madsChains;
  short[][] colixesChains;

  Trace(JmolViewer viewer, Frame frame) {
    this.viewer = viewer;
    this.frame = frame;
    pdbFile = frame.pdbFile;
  }

  public void setMad(short mad, BitSet bsSelected) {
    initialize();
    for (int m = tmodels.length; --m >= 0; )
      tmodels[m].setMad(mad, bsSelected);
  }

  public void setColix(byte palette, short colix, BitSet bsSelected) {
    initialize();
    for (int m = tmodels.length; --m >= 0; )
      tmodels[m].setColix(palette, colix, bsSelected);
  }

  void initialize() {
    if (tmodels == null) {
      int tmodelCount = pdbFile == null ? 0 : pdbFile.getModelCount();
      tmodels = new Tmodel[tmodelCount];
      for (int i = tmodelCount; --i >= 0; )
        tmodels[i] = new Tmodel(pdbFile.getModel(i));
    }
  }

  int getTmodelCount() {
    return tmodels.length;
  }

  Tmodel getTmodel(int i) {
    return tmodels[i];
  }

  class Tmodel {
    PdbModel model;
    Tchain[] tchains;
    
    Tmodel(PdbModel model) {
      this.model = model;
      tchains = new Tchain[model.getChainCount()];
      for (int i = tchains.length; --i >= 0; )
        tchains[i] = new Tchain(model.getChain(i));
    }
    
    public void setMad(short mad, BitSet bsSelected) {
      for (int i = tchains.length; --i >= 0; )
        tchains[i].setMad(mad, bsSelected);
    }

    public void setColix(byte palette, short colix, BitSet bsSelected) {
      for (int i = tchains.length; --i >= 0; )
        tchains[i].setColix(palette, colix, bsSelected);
    }

    int getTchainCount() {
      return tchains.length;
    }

    Tchain getTchain(int i) {
      return tchains[i];
    }
  }

  class Tchain {
    PdbChain chain;
    int mainchainLength;
    PdbGroup[] mainchain;
    short[] colixes;
    short[] mads;

    Tchain(PdbChain chain) {
      this.chain = chain;
      mainchain = chain.getMainchain();
      mainchainLength = mainchain.length;
      if (mainchainLength > 0) {
        colixes = new short[mainchainLength];
        mads = new short[mainchainLength + 1];
      }
    }

    public void setMad(short mad, BitSet bsSelected) {
      for (int i = mainchainLength; --i >= 0; ) {
        if (bsSelected.get(mainchain[i].getAlphaCarbonIndex()))
          if (mad < 0) {
            // -2.0 angstrom diameter -> -4000 milliangstroms diameter
            if (mad == -4000)
              mads[i] = 1000; // trace temperature goes here
            else
              mads[i] = (short)(mainchain[i].isHelixOrSheet() ? 1500 : 500);
          } else {
            mads[i] = mad;
          }
      }
      if (mainchainLength > 0)
        mads[mainchainLength] = mads[mainchainLength - 1];
    }

    public void setColix(byte palette, short colix, BitSet bsSelected) {
      Frame frame = chain.model.file.frame;
      for (int i = mainchainLength; --i >= 0; ) {
        int atomIndex = mainchain[i].getAlphaCarbonIndex();
        if (bsSelected.get(atomIndex))
          colixes[i] =
            palette > JmolConstants.PALETTE_CPK
            ? viewer.getColixAtomPalette(frame.getAtomAt(atomIndex), palette)
            : colix;
      }
    }

  /*
    void initialize() {
      if (! initialized) {
        chainCount = pdbFile.getChainCount();
        madsChains = new short[chainCount][];
        colixesChains = new short[chainCount][];
        for (int i = chainCount; --i >= 0; ) {
          int chainLength = pdbFile.getMainchain(i).length;
          colixesChains[i] = new short[chainLength];
          // mads are one larger and the last two values are always ==
          // makes interval caluclations easier (maybe?)
          madsChains[i] = new short[chainLength + 1];
        }
        initialized = true;
      }
    }
  */
  }
}

