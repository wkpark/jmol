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

class TraceRenderer extends Renderer {

  TraceRenderer(JmolViewer viewer) {
    this.viewer = viewer;
  }

  Point3i s0 = new Point3i();
  Point3i s1 = new Point3i();
  Point3i s2 = new Point3i();
  Point3i s3 = new Point3i();
  int diameterBeg, diameterMid, diameterEnd;

  Trace trace;

  void render(Graphics3D g3d, Rectangle rectClip, Frame frame) {
    this.frame = frame;
    this.g3d = g3d;
    this.rectClip = rectClip;
    this.trace = frame.trace;

    if (trace == null || !trace.initialized)
      return;
    PdbMolecule pdbMolecule = trace.pdbMolecule;
    short[][] madsChains = trace.madsChains;
    short[][] colixesChains = trace.colixesChains;
    for (int i = trace.chainCount; --i >= 0; ) {
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
    int iPrev1 = i - 1, iPrev2 = i - 2, iNext1 = i + 1, iNext2 = i + 2;
    if (iPrev1 < 0)
      iPrev1 = 0;
    if (iPrev2 < 0)
      iPrev2 = 0;
    if (iNext1 > mainchainLast)
      iNext1 = mainchainLast;
    if (iNext2 > mainchainLast)
      iNext2 = mainchainLast;
    calcAverage(mainchain, iPrev2, iPrev1, s0);
    calcAverage(mainchain, iPrev1, i, s1);
    calcAverage(mainchain, i, iNext1, s2);
    calcAverage(mainchain, iNext1, iNext2, s3);
    int madBeg = (mads[iPrev1] + mads[i]) / 2;
    int madEnd = (mads[iNext1] + mads[i]) / 2;
    diameterBeg = viewer.scaleToScreen(s1.z, madBeg);
    diameterMid = viewer.scaleToScreen(mainchain[i].getAlphaCarbonAtom().z, mads[i]);
    diameterEnd = viewer.scaleToScreen(s2.z, madEnd);
  }

  void calcAverage(PdbResidue[] mainchain, int iA, int iB, Point3i dest) {
    Atom atomA = mainchain[iA].getAlphaCarbonAtom();
    Atom atomB = mainchain[iB].getAlphaCarbonAtom();
    dest.x = (atomA.x + atomB.x) / 2;
    dest.y = (atomA.y + atomB.y) / 2;
    dest.z = (atomA.z + atomB.z) / 2;
  }

  void render1Segment(short colix) {
    g3d.fillHermite(colix, diameterBeg, diameterMid, diameterEnd,
                    s0, s1, s2, s3);
  }
}

