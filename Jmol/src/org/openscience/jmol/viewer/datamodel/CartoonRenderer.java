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

class CartoonRenderer extends Renderer {

  CartoonRenderer(JmolViewer viewer) {
    this.viewer = viewer;
  }

  Point3i s0 = new Point3i();
  Point3i s1 = new Point3i();
  Point3i s2 = new Point3i();
  Point3i s3 = new Point3i();
  int diameterBeg, diameterMid, diameterEnd;

  Cartoon cartoon;

  void render(Graphics3D g3d, Rectangle rectClip, Frame frame) {
    this.frame = frame;
    this.g3d = g3d;
    this.rectClip = rectClip;
    this.cartoon = frame.cartoon;

    if (cartoon == null || !cartoon.initialized)
      return;
    PdbMolecule pdbMolecule = cartoon.pdbMolecule;
    short[][] madsChains = cartoon.madsChains;
    short[][] colixesChains = cartoon.colixesChains;
    for (int i = cartoon.chainCount; --i >= 0; ) {
      render1Chain(pdbMolecule.getMainchain(i), madsChains[i], colixesChains[i]);
    }
  }
  
  int mainchainLast;

  void render1Chain(PdbResidue[] mainchain, short[] mads, short[] colixes) {
    mainchainLast = mainchain.length - 1;
    for (int i = 0; i < mainchain.length; ++i) {
      PdbResidue residue = mainchain[i];
      short colix = colixes[i];
      if (colix == 0)
        colix = residue.getAlphaCarbonAtom().colixAtom;
      if (residue.isHelixOrSheet()) {
        renderSpecialSegment(residue, colix, mads[i]);
      } else {
        calcSegmentPoints(mainchain, i, mads);
        render1Segment(colix);
      }
    }
    renderPending();
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
    int madBeg, madMid, madEnd;
    madBeg = madMid = madEnd = mads[i];
    if (! mainchain[iPrev1].isHelixOrSheet())
      madBeg = (mads[iPrev1] + madBeg) / 2;
    if (! mainchain[iNext1].isHelixOrSheet())
      madEnd = (mads[iNext1] + madEnd) / 2;
    diameterBeg = viewer.scaleToScreen(s1.z, madBeg);
    diameterMid = viewer.scaleToScreen(mainchain[i].getAlphaCarbonAtom().z,
                                       madMid);
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

  final Point3i screenA = new Point3i();
  Point3i screenB = new Point3i();
  Point3i screenC = new Point3i();

  void renderSpecialSegment(PdbResidue residue, short colix, short mad) {
    int resNumber = residue.resNumber;
    PdbStructure structure = residue.structure;
    if (tPending) {
      if (structure == structurePending &&
          resNumber == endPending + 1 &&
          mad == madPending &&
          colix == colixPending) {
        ++endPending;
        return;
      }
      renderPending();
    }
    structurePending = structure;
    beginPending = endPending = resNumber;
    colixPending = colix;
    madPending = mad;
    tPending = true;
  }

  boolean tPending;
  PdbStructure structurePending;
  int beginPending;
  int endPending;
  short madPending;
  short colixPending;

  void renderPending() {
    if (tPending) {
      Point3f[] segments = structurePending.getSegments();
      int residueNumberStart = structurePending.getResidueNumberStart();
      int iStart = beginPending - residueNumberStart;
      viewer.transformPoint(segments[iStart], screenA);
      int iEnd = endPending - residueNumberStart + 1;
      viewer.transformPoint(segments[iEnd], screenB);
      boolean tRocketCap = (iEnd == structurePending.getResidueCount());
      if (tRocketCap) {
        viewer.transformPoint(segments[iEnd - 1], screenC);
        int capDiameter =
          viewer.scaleToScreen(screenC.z, madPending + (madPending >> 2));
        g3d.fillCone(colixPending, Graphics3D.ENDCAPS_FLAT, capDiameter,
                     screenC, screenB);
        if (beginPending == endPending)
          return;
        Point3i t = screenB;
        screenB = screenC;
        screenC = t;
      }
      int zMid = (screenA.z + screenB.z) / 2;
      int diameter = viewer.scaleToScreen(zMid, madPending);
      g3d.fillCylinder(colixPending, Graphics3D.ENDCAPS_FLAT, diameter,
                       screenA, screenB);
      tPending = false;
    }
  }
}

