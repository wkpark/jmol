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

public class Trace {

  JmolViewer viewer;
  Frame frame;
  boolean hasPdbRecords;
  PdbMolecule pdbMolecule;

  boolean initialized;
  int chainCount;
  Atom[][] chains;
  short[][] madsChains;
  short[][] colixesChains;
  Point3f[][] midPointsChains;

  Trace(JmolViewer viewer, Frame frame) {
    this.viewer = viewer;
    this.frame = frame;
    hasPdbRecords = frame.hasPdbRecords;
    pdbMolecule = frame.pdbMolecule;
  }

  public void setMad(short mad, BitSet bsSelected) {
    if (! hasPdbRecords)
      return;
    if (! initialized)
      initialize();
    for (int i = chainCount; --i >= 0; ) {
      Atom[] alphas = chains[i];
      short[] mads = madsChains[i];
      for (int j = alphas.length; --j >= 0; ) {
        Atom alpha = alphas[j];
        if (bsSelected.get(alpha.getAtomIndex()))
          mads[j] = mad;
      }
    }
  }

  public void setColor(byte palette, short colix, BitSet bsSelected) {
    if (! hasPdbRecords)
      return;
    if (! initialized)
      initialize();
    boolean usePalette = (colix == 0);
    for (int i = chainCount; --i >= 0; ) {
      Atom[] alphas = chains[i];
      short[] colixes = colixesChains[i];
      for (int j = alphas.length; --j >= 0; ) {
        Atom alpha = alphas[j];
        if (bsSelected.get(alpha.getAtomIndex()))
          colixes[j] = 
            usePalette ? viewer.getColixAtomPalette(alpha, palette) : colix;
      }
    }
  }
  
  void initialize() {
    chains = pdbMolecule.getAlphaChains();
    chainCount = chains.length;
    madsChains = new short[chainCount][];
    colixesChains = new short[chainCount][];
    midPointsChains = new Point3f[chainCount][];
    for (int i = chainCount; --i >= 0; ) {
      int chainLength = chains[i].length;
      madsChains[i] = new short[chainLength];
      colixesChains[i] = new short[chainLength];
      calcMidPoints(chains[i],
                    midPointsChains[i] = new Point3f[chainLength + 1]);
    }
    initialized = true;
  }

  void calcMidPoints(Atom[] alphas, Point3f[] midPoints) {
    int chainLength = alphas.length;
    Point3f atomPrevious = alphas[0].point3f;
    midPoints[0] = atomPrevious;
    for (int i = 1; i < chainLength; ++i) {
      Point3f mid = midPoints[i] = new Point3f(atomPrevious);
      atomPrevious = alphas[i].point3f;
      mid.add(atomPrevious);
      mid.scale(0.5f);
    }
    midPoints[chainLength] = atomPrevious;
  }
}
