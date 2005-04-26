/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;


class MeshRibbonRenderer extends MpsRenderer { // not current for Mcp class

  MeshRibbon strands;

  Point3i[] calcScreens(Point3f[] centers, Vector3f[] vectors,
                        short[] mads, float offsetFraction) {
    
    Point3f pointT = new Point3f();  //DC: made local
    Point3i[] screens = viewer.allocTempScreens(centers.length);
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
  float strandSeparation;
  float baseOffset;

  boolean isNucleicPolymer;

  void renderMpspolymer( Mps.Mpspolymer mpspolymer) {
    MeshRibbon.Schain strandsChain = (MeshRibbon.Schain)mpspolymer;

    strandCount = viewer.getStrandsCount();
    strandSeparation = (strandCount <= 1 ) ? 0 : 1f / (strandCount - 1);
    baseOffset =
      ((strandCount & 1) == 0) ? strandSeparation / 2 : strandSeparation;

    if (strandsChain.wingVectors != null) {
      isNucleicPolymer = strandsChain.polymer instanceof NucleicPolymer;
      render1Chain(strandsChain.monomerCount,
                   strandsChain.monomers,
                   strandsChain.leadMidpoints,
                   strandsChain.wingVectors,
                   strandsChain.mads,
                   strandsChain.colixes);
    }
  }


  void render1Chain(int monomerCount,
                    Monomer[] monomers, Point3f[] centers,
                    Vector3f[] vectors, short[] mads, short[] colixes) {
    Point3i[] ribbonTopScreens;
    Point3i[] ribbonBottomScreens;

    int j = strandCount >> 1;
    float offset = (j * strandSeparation) + baseOffset;
    ribbonTopScreens = calcScreens(centers, vectors, mads, offset);
    ribbonBottomScreens = calcScreens(centers, vectors, mads, -offset);
    render2Strand(monomerCount, monomers, mads, colixes,
                  ribbonTopScreens, ribbonBottomScreens);
    viewer.freeTempScreens(ribbonTopScreens);
    viewer.freeTempScreens(ribbonBottomScreens);
    
    Point3i[] screens;
    for (int i = strandCount >> 1; --i >= 0; ) {
      float f = (i * strandSeparation) + baseOffset;
      screens = calcScreens(centers, vectors, mads, f);
      render1Strand(monomerCount, monomers, mads, colixes, screens);
      viewer.freeTempScreens(screens);
      screens = calcScreens(centers, vectors, mads, -f);
      render1Strand(monomerCount, monomers, mads, colixes, screens);
      viewer.freeTempScreens(screens);
    }
    if ((strandCount & 1) != 0) {
      screens = calcScreens(centers, vectors, mads, 0f);
      render1Strand(monomerCount, monomers, mads, colixes, screens);
      viewer.freeTempScreens(screens);
    }
    
  }


  void render1Strand(int monomerCount, Monomer[] monomers, short[] mads,
                     short[] colixes, Point3i[] screens) {
    for (int i = monomerCount; --i >= 0; )
      if (mads[i] > 0)
        render1StrandSegment(monomerCount,
                             monomers[i], colixes[i], mads, screens, i);
  }
  
  
  void render1StrandSegment(int monomerCount, Monomer monomer, short colix,
                            short[] mads, Point3i[] screens, int i) {
    int iLast = monomerCount;
    int iPrev = i - 1; if (iPrev < 0) iPrev = 0;
    int iNext = i + 1; if (iNext > iLast) iNext = iLast;
    int iNext2 = i + 2; if (iNext2 > iLast) iNext2 = iLast;
    if (colix == 0)
      colix = monomer.getLeadAtom().colixAtom;
    g3d.drawHermite(colix, 7, screens[iPrev], screens[i],
                    screens[iNext], screens[iNext2]);
  }

  void render2Strand(int monomerCount, Monomer[] monomers, short[] mads,
                     short[] colixes, Point3i[] ribbonTopScreens,
                     Point3i[] ribbonBottomScreens) {
    for (int i = monomerCount; --i >= 0; )
      if (mads[i] > 0)
        render2StrandSegment(monomerCount,
                             monomers[i], colixes[i], mads,
                             ribbonTopScreens, ribbonBottomScreens, i);
  }
  
  void render2StrandSegment(int monomerCount, Monomer monomer, short colix,
                            short[] mads, Point3i[] ribbonTopScreens,
                            Point3i[] ribbonBottomScreens, int i) {
    int iLast = monomerCount;
    int iPrev = i - 1; if (iPrev < 0) iPrev = 0;
    int iNext = i + 1; if (iNext > iLast) iNext = iLast;
    int iNext2 = i + 2; if (iNext2 > iLast) iNext2 = iLast;
    if (colix == 0)
      colix = monomer.getLeadAtom().colixAtom;
    
    //change false -> true to fill in mesh
      
    g3d.drawHermite(false, true, colix, isNucleicPolymer ? 4 : 7,
                    ribbonTopScreens[iPrev], ribbonTopScreens[i],
                    ribbonTopScreens[iNext], ribbonTopScreens[iNext2],
                    ribbonBottomScreens[iPrev], ribbonBottomScreens[i],
                    ribbonBottomScreens[iNext], ribbonBottomScreens[iNext2]
                    );
  }
}
