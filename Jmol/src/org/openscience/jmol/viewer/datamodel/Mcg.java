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

/****************************************************************
 * Mcg stands for Model-Chain-Graphic
 ****************************************************************/
abstract public class Mcg implements Graphic {

  JmolViewer viewer;
  Frame frame;
  PdbFile pdbFile;

  Model[] models;

  Mcg(JmolViewer viewer, Frame frame) {
    this.viewer = viewer;
    this.frame = frame;
    pdbFile = frame.pdbFile;
  }
  
  public void setMad(short mad, BitSet bsSelected) {
    initialize();
    for (int m = models.length; --m >= 0; )
      models[m].setMad(mad, bsSelected);
  }
  
  public void setColix(byte palette, short colix, BitSet bsSelected) {
    initialize();
    for (int m = models.length; --m >= 0; )
      models[m].setColix(palette, colix, bsSelected);
  }

  abstract Chain allocateMcgChain(PdbChain pdbChain);

  void initialize() {
    if (models == null) {
      int modelCount = pdbFile == null ? 0 : pdbFile.getModelCount();
      models = new Model[modelCount];
      for (int i = modelCount; --i >= 0; )
        models[i] = new Model(pdbFile.getModel(i));
    }
  }

  int getModelCount() {
    return models.length;
  }

  Model getMcgModel(int i) {
    return models[i];
  }

  class Model {
    Chain[] chains;
    
    Model(PdbModel model) {
      chains = new Chain[model.getChainCount()];
      for (int i = chains.length; --i >= 0; )
        chains[i] = allocateMcgChain(model.getChain(i));
    }
    
    public void setMad(short mad, BitSet bsSelected) {
      for (int i = chains.length; --i >= 0; )
        chains[i].setMad(mad, bsSelected);
    }

    public void setColix(byte palette, short colix, BitSet bsSelected) {
      for (int i = chains.length; --i >= 0; )
        chains[i].setColix(palette, colix, bsSelected);
    }

    int getChainCount() {
      return chains.length;
    }

    Chain getMcgChain(int i) {
      return chains[i];
    }
  }

  abstract class Chain {
    PdbChain pdbChain;
    short[] colixes;
    short[] mads;
    
    Chain(PdbChain pdbChain) {
      this.pdbChain = pdbChain;
    }
    
    abstract public void setMad(short mad, BitSet bsSelected);
    
    abstract public void setColix(byte palette, short colix,
                                  BitSet bsSelected);
  }

}

