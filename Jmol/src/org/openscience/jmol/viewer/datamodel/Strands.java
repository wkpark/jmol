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
import org.openscience.jmol.viewer.protein.*;
import javax.vecmath.Point3f;
import java.util.BitSet;

public class Strands {

  JmolViewer viewer;
  Frame frame;
  boolean hasPdbRecords;
  PdbMolecule pdbMolecule;

  boolean initialized;
  int chainCount;
  short[][] madsChains;
  short[][] colixesChains;

  Strands(JmolViewer viewer, Frame frame) {
    this.viewer = viewer;
    this.frame = frame;
    hasPdbRecords = frame.hasPdbRecords;
    pdbMolecule = frame.pdbMolecule;
  }

  public void setMad(short mad, BitSet bsSelected) {
    if (! hasPdbRecords)
      return;
    initialize();
    for (int i = pdbMolecule.getChainCount(); --i >= 0; ) {
      short[] mads = madsChains[i];
      PdbResidue[] mainchain = pdbMolecule.getMainchain(i);
      for (int j = mainchain.length; --j >= 0; ) {
        if (bsSelected.get(mainchain[j].getAlphaCarbonIndex()))
          if (mad < 0) {
            mads[j] = (short)(mainchain[j].isHelixOrSheet() ? 1500 : 500);
          } else {
            mads[j] = mad;
          }
      }
    }
  }

  public void setColor(byte palette, short colix, BitSet bsSelected) {
    if (! hasPdbRecords)
      return;
    initialize();
    for (int i = pdbMolecule.getChainCount(); --i >= 0; ) {
      short[] colixes = colixesChains[i];
      PdbResidue[] mainchain = pdbMolecule.getMainchain(i);
      for (int j = mainchain.length; --j >= 0; ) {
        int atomIndex = mainchain[j].getAlphaCarbonIndex();
        if (bsSelected.get(atomIndex))
          colixes[j] =
            (colix == 0 ? viewer.getColixAtomPalette(frame.getAtomAt(atomIndex), palette) : colix);
      }
    }
  }

  void initialize() {
    if (! initialized) {
      chainCount = pdbMolecule.getChainCount();
      madsChains = new short[chainCount][];
      colixesChains = new short[chainCount][];
      for (int i = chainCount; --i >= 0; ) {
        int chainLength = pdbMolecule.getMainchain(i).length;
        madsChains[i] = new short[chainLength];
        colixesChains[i] = new short[chainLength];
      }
      initialized = true;
    }
  }
}
