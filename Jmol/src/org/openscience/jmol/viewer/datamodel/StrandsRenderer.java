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
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;

class StrandsRenderer extends McgRenderer {

  StrandsRenderer(JmolViewer viewer, FrameRenderer frameRenderer) {
    super(viewer, frameRenderer);
  }

  Strands strands;
  Point3f pointT = new Point3f();

  Point3i[] screensT = new Point3i[0];
  Point3i[] getTempScreens(int minLen) {
    if (screensT.length < minLen) {
      Point3i[] t = new Point3i[minLen];
      System.arraycopy(screensT, 0, t, 0, screensT.length);
      for (int i = screensT.length; i < t.length; ++i)
        t[i] = new Point3i();
      screensT = t;
    }
    return screensT;
  }

  Point3i[] calcScreens(Point3f[] centers, Vector3f[] vectors,
                   short[] mads, float offsetFraction) {
    Point3i[] screens = getTempScreens(centers.length);
    if (offsetFraction == 0) {
      for (int i = centers.length; --i >= 0; )
        viewer.transformPoint(centers[i], screens[i]);
    } else {
      offsetFraction /= 1000;
      for (int i = centers.length; --i >= 0; ) {
        pointT.set(vectors[i]);
        float scale = mads[i] * offsetFraction;
        pointT.scaleAdd(scale, centers[i]);
        viewer.transformPoint(pointT, screens[i]);
      }
    }
    return screens;
  }

  int strandCount;
  float halfStrandCount;
  float strandSeparation;
  float baseOffset;

  void renderMcgChain(Mcg.Chain mcgChain) {
    Strands.Chain strandsChain = (Strands.Chain)mcgChain;
    if (strandsChain.mainchainLength < 2)
      return;

    strandCount = viewer.getStrandsCount();
    strandSeparation = (strandCount <= 1 ) ? 0 : 1f / (strandCount - 1);
    baseOffset =
      ((strandCount & 1) == 0) ? strandSeparation / 2 : strandSeparation;
    
    render1Chain(strandsChain.mainchain,
                 strandsChain.centers,
                 strandsChain.vectors,
                 strandsChain.mads,
                 strandsChain.colixes);
  }

  /*
  void render() {
    this.strands = frame.strands;

    if (strands == null || !strands.initialized)
      return;
    PdbFile pdbFile = strands.pdbFile;
    short[][] madsChains = strands.madsChains;
    short[][] colixesChains = strands.colixesChains;
    Point3f[][] centersChains = strands.centersChains;
    Vector3f[][] vectorsChains = strands.vectorsChains;
    for (int i = strands.chainCount; --i >= 0; ) {
      Point3f[] centers = centersChains[i];
      if (centers != null)
        render1Chain(pdbFile.getMainchain(i), centers,
                     vectorsChains[i], madsChains[i], colixesChains[i]);
    }
  }
  */

  void render1Chain(PdbGroup[] mainchain, Point3f[] centers,
                    Vector3f[] vectors, short[] mads, short[] colixes) {
    Point3i[] screens;
    for (int i = strandCount >> 1; --i >= 0; ) {
      float f = (i * strandSeparation) + baseOffset;
      screens = calcScreens(centers, vectors, mads, f);
      render1Strand(mainchain, mads, colixes, screens);
      screens = calcScreens(centers, vectors, mads, -f);
      render1Strand(mainchain, mads, colixes, screens);
    }
    if ((strandCount & 1) != 0) {
      screens = calcScreens(centers, vectors, mads, 0f);
      render1Strand(mainchain, mads, colixes, screens);
    }
  }

  void render1Strand(PdbGroup[] mainchain, short[] mads,
                     short[] colixes, Point3i[] screens) {
    for (int i = colixes.length; --i >= 0; )
      if (mads[i] > 0)
        render1StrandSegment(mainchain[i], colixes[i], mads, screens, i);
  }

  void render1StrandSegment(PdbGroup group, short colix,
                            short[] mads, Point3i[] screens, int i) {
    int iLast = mads.length - 1;
    int iPrev = i - 1; if (iPrev < 0) iPrev = 0;
    int iNext = i + 1; if (iNext > iLast) iNext = iLast;
    int iNext2 = i + 2; if (iNext2 > iLast) iNext2 = iLast;
    if (colix == 0)
      colix = group.getAlphaCarbonAtom().colixAtom;
    g3d.drawHermite(colix, 7, screens[iPrev], screens[i],
                    screens[iNext], screens[iNext2]);
  }
}
