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
import org.openscience.jmol.viewer.protein.*;
import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

class StrandsRenderer extends Renderer {

  StrandsRenderer(JmolViewer viewer) {
    this.viewer = viewer;
  }

  Point3i s0 = new Point3i();
  Point3i s1 = new Point3i();
  Point3i s2 = new Point3i();
  Point3i s3 = new Point3i();
  int diameterBeg, diameterMid, diameterEnd;

  Strands strands;

  void render(Graphics3D g3d, Rectangle rectClip, Frame frame) {
    this.frame = frame;
    this.g3d = g3d;
    this.rectClip = rectClip;
    this.strands = frame.strands;

    if (strands == null || !strands.initialized)
      return;
    PdbMolecule pdbMolecule = strands.pdbMolecule;
    short[][] madsChains = strands.madsChains;
    short[][] colixesChains = strands.colixesChains;
    for (int i = strands.chainCount; --i >= 0; ) {
      render1Chain(pdbMolecule.getMainchain(i), madsChains[i], colixesChains[i]);
    }
  }
  
  int mainchainLast;

  void render1Chain(PdbResidue[] mainchain, short[] mads, short[] colixes) {
    mainchainLast = mainchain.length - 1;
    for (int i = mainchain.length; --i >= 0; ) {
      Atom alpha = mainchain[i].getAlphaCarbonAtom();
      int x = alpha.x, y = alpha.y, z = alpha.z;
      short colix = colixes[i];
      if (colix == 0)
        colix = alpha.colixAtom;
      calcSegmentPoints(mainchain, i, mads);
      render1Segment(colix);
    }
  }

  void calcSegmentPoints(PdbResidue[] mainchain, int i, short[] mads) {
    int iPrev = i - 1, iNext = i + 1, iNext2 = i + 2;
    if (iPrev < 0)
      iPrev = 0;
    if (iNext > mainchainLast)
      iNext = mainchainLast;
    if (iNext2 > mainchainLast)
      iNext2 = mainchainLast;
    calc(mainchain, iPrev, s0);
    calc(mainchain, i, s1);
    calc(mainchain, iNext, s2);
    calc(mainchain, iNext2, s3);
  }

  void calc(PdbResidue[] mainchain, int i, Point3i dest) {
    Atom atom = mainchain[i].getAlphaCarbonAtom();
    dest.x = atom.x;
    dest.y = atom.y;
    dest.z = atom.z;
  }

  void calcAverage(PdbResidue[] mainchain, int iA, int iB, Point3i dest) {
    Atom atomA = mainchain[iA].getAlphaCarbonAtom();
    Atom atomB = mainchain[iB].getAlphaCarbonAtom();
    dest.x = (atomA.x + atomB.x) / 2;
    dest.y = (atomA.y + atomB.y) / 2;
    dest.z = (atomA.z + atomB.z) / 2;
  }

  void render1Segment(short colix) {
    g3d.drawHermite(colix,
                    s0.x, s0.y, s0.z, s1.x, s1.y, s1.z,
                    s2.x, s2.y, s2.z, s3.x, s3.y, s3.z);
  }
}

