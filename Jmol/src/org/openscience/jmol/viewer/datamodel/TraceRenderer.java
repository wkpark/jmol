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
import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

class TraceRenderer extends Renderer {

  TraceRenderer(JmolViewer viewer, FrameRenderer frameRenderer) {
    this.viewer = viewer;
    this.frameRenderer = frameRenderer;
  }

  Trace trace;

  void render() {
    this.trace = frame.trace;

    if (trace == null || !trace.initialized)
      return;
    PdbMolecule pdbMolecule = trace.pdbMolecule;
    short[][] madsChains = trace.madsChains;
    short[][] colixesChains = trace.colixesChains;
    for (int i = trace.chainCount; --i >= 0; ) {
      render1Chain(pdbMolecule.getMainchain(i), madsChains[i],
                   colixesChains[i]);
    }
    screens = null;
    alphas = null;
  }
  
  int mainchainLength;
  Point3i[] screens;
  Atom[] alphas;

  void render1Chain(PdbGroup[] mainchain, short[] mads, short[] colixes) {
    calcMidPoints(mainchain);
    mainchainLength = mainchain.length;
    for (int i = mainchainLength; --i >= 0; ) {
      short colix = colixes[i];
      if (colix == 0)
        colix = alphas[i].colixAtom;
      render1Segment(colix, mads, i);
    }
  }

  void calcMidPoints(PdbGroup[] mainchain) {
    int chainLength = mainchain.length;
    screens = frameRenderer.getTempScreens(chainLength + 1);
    alphas = frameRenderer.getTempAtoms(chainLength);
    Atom atomPrev = alphas[0] = mainchain[0].getAlphaCarbonAtom();
    setScreen(atomPrev, screens[0]);
    for (int i = 1; i < chainLength; ++i) {
      Atom atomThis = alphas[i] = mainchain[i].getAlphaCarbonAtom();
      calcAverageScreen(atomPrev, atomThis, screens[i]);
      atomPrev = atomThis;
    }
    setScreen(atomPrev, screens[chainLength]);
  }

  void setScreen(Atom atom, Point3i dest) {
    dest.x = atom.x;
    dest.y = atom.y;
    dest.z = atom.z;
  }

  void calcAverageScreen(Atom atomA, Atom atomB, Point3i dest) {
    dest.x = (atomA.x + atomB.x) / 2;
    dest.y = (atomA.y + atomB.y) / 2;
    dest.z = (atomA.z + atomB.z) / 2;
  }

  void render1Segment(short colix, short[] mads, int i) {
    int iPrev1 = i - 1; if (iPrev1 < 0) iPrev1 = 0;
    int iNext1 = i + 1; if (iNext1 > mainchainLength) iNext1 = mainchainLength;
    int iNext2 = i + 2; if (iNext2 > mainchainLength) iNext2 = mainchainLength;
    
    int madThis = mads[i];
    int madBeg = (mads[iPrev1] + madThis) / 2;
    int diameterBeg = viewer.scaleToScreen(screens[i].z, madBeg);
    int madEnd = (mads[iNext1] + madThis) / 2;
    int diameterEnd = viewer.scaleToScreen(screens[iNext1].z, madEnd);
    int diameterMid = viewer.scaleToScreen(alphas[i].z, madThis);
    g3d.fillHermite(colix, 7, diameterBeg, diameterMid, diameterEnd,
                    screens[iPrev1], screens[i],
                    screens[iNext1], screens[iNext2]);
  }
}

