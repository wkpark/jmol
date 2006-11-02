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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;

import java.util.BitSet;

abstract class MpsRenderer extends ShapeRenderer {

  boolean isTraceAlpha; 
  int myVisibilityFlag;
  boolean isNucleic;
  boolean isCarbohydrate;
  boolean ribbonBorder = false;
  BitSet bsVisible = new BitSet();
  Point3i[] ribbonTopScreens;
  Point3i[] ribbonBottomScreens;

  int monomerCount;
  Monomer[] monomers;

  Point3f[] controlPoints;
  Point3i[] controlPointScreens;
  boolean haveControlPointScreens;
  Vector3f[] wingVectors;
  short[] mads;
  short[] colixes;
  boolean[] isSpecials;
  boolean[] isHelixes;
  int[] leadAtomIndices;
  
  void render() {
    if (shape == null)
      return;
    Mps mcps = (Mps)shape;
    this.myVisibilityFlag = shape.myVisibilityFlag;
    for (int m = mcps.getMpsmodelCount(); --m >= 0; ) {
      Mps.Mpsmodel mcpsmodel = mcps.getMpsmodel(m);
      if ((mcpsmodel.modelVisibilityFlags & myVisibilityFlag) == 0)
        continue;
      for (int c = mcpsmodel.getMpspolymerCount(); --c >= 0; ) {
        Mps.Mpspolymer mpspolymer = mcpsmodel.getMpspolymer(c);
        if (mpspolymer.monomerCount >= 2) {
          initializePolymer(mpspolymer);
          renderMpspolymer(mpspolymer);
          freeTempScreens();
        }
      }
    }
    freeTempScreens();
  }

  void freeTempScreens() {
    if (haveControlPointScreens)
      viewer.freeTempScreens(controlPointScreens);
    viewer.freeTempBooleans(isSpecials);
    viewer.freeTempBooleans(isHelixes);
  }
  abstract void renderMpspolymer(Mps.Mpspolymer mpspolymer);

  ////////////////////////////////////////////////////////////////
  // some utilities
  void initializePolymer(Mps.Mpspolymer schain) {
    ribbonBorder = viewer.getRibbonBorder();
    isTraceAlpha = viewer.getTraceAlpha();
    // note that we are not treating a PhosphorusPolymer
    // as nucleic because we are not calculating the wing
    // vector correctly.
    // if/when we do that then this test will become
    // isNucleic = schain.polymer.isNucleic();    
    isNucleic = schain.polymer instanceof NucleicPolymer;
    isCarbohydrate = schain.polymer instanceof CarbohydratePolymer;
    controlPoints = (isTraceAlpha ? schain.leadPoints : schain.leadMidpoints);
    haveControlPointScreens = false;
    wingVectors = schain.wingVectors;
    monomerCount = schain.monomerCount;
    monomers = schain.monomers;
    mads = schain.mads;
    colixes = schain.colixes;
    leadAtomIndices = schain.polymer.getLeadAtomIndices();
    setStructureBooleans();
    bsVisible.clear();
    for (int i = monomerCount; --i >= 0;)
      if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) != 0
          && !frame.bsHidden.get(leadAtomIndices[i]))
        bsVisible.set(i);
  }
  
  void setStructureBooleans() {
    isSpecials = viewer.allocTempBooleans(monomerCount + 1);
    for (int i = monomerCount; --i >= 0; ) {
      isSpecials[i] = monomers[i].isHelixOrSheet();
    }
    isSpecials[monomerCount] = isSpecials[monomerCount - 1];
    isHelixes = viewer.allocTempBooleans(monomerCount + 1);
    for (int i = monomerCount; --i >= 0; ) {
      isHelixes[i] = monomers[i].isHelix();
    }
    isHelixes[monomerCount] = isHelixes[monomerCount - 1];
    //if more added, don't forget to free them
  }

  void calcScreenControlPoints() {
    int count = monomerCount + 1;
    controlPointScreens = viewer.allocTempScreens(count);
    for (int i = count; --i >= 0; ) {
      viewer.transformPoint(controlPoints[i], controlPointScreens[i]);
    }
    haveControlPointScreens = true;
  }

  final Point3f pointT = new Point3f();
  /**
   * calculate screen points based on control points and wing positions
   * @param offsetFraction
   * @return Point3i array THAT MUST BE LATER FREED
   */
  Point3i[] calcScreens(float offsetFraction) {
    int count = controlPoints.length;
    Point3i[] screens = viewer.allocTempScreens(count);
    if (offsetFraction == 0) {
      for (int i = count; --i >= 0; )
        viewer.transformPoint(controlPoints[i], screens[i]);
    } else {
      offsetFraction /= 1000f;
      for (int i = count; --i >= 0; ) {
        pointT.set(wingVectors[i]);
        short mad = mads[i];
        float scale = mad * offsetFraction;
        pointT.scaleAdd(scale, controlPoints[i]);
        viewer.transformPoint(pointT, screens[i]);
      }
    }
    return screens;
  }

  void calc1Screen(Point3f center, Vector3f vector,
                   short mad, float offsetFraction, Point3i screen) {
    pointT.set(vector);
    float scale = mad * offsetFraction / 1000f;
    pointT.scaleAdd(scale, center);
    viewer.transformPoint(pointT, screen);
  }

  short getLeadColix(int i) {
    return Graphics3D.inheritColix(colixes[i], monomers[i].getLeadAtom().colixAtom);
  }
  
  final void renderRopeSegment(short colix, int i, boolean isSpecial) {
    renderRopeSegment2(colix, i, i, isSpecial);    
  }

  final void renderRopeSegment2(short colix, int i, int imad, boolean isSpecial) {
    int iPrev1 = i - 1; if (iPrev1 < 0) iPrev1 = 0;
    int iNext1 = i + 1; if (iNext1 > monomerCount) iNext1 = monomerCount;
    int iNext2 = i + 2; if (iNext2 > monomerCount) iNext2 = monomerCount;
    
    int madThis, madBeg, madEnd;
    madThis = madBeg = madEnd = mads[imad];
    if (isSpecial) {
      if (! isSpecials[iPrev1])
        madBeg = (mads[iPrev1] + madThis) / 2;
      if (! isSpecials[iNext1])
        madEnd = (mads[iNext1] + madThis) / 2;
    }
    int diameterBeg = 0;
    try{
      diameterBeg =
      viewer.scaleToScreen(controlPointScreens[i].z, madBeg);
    }catch (Exception e) {
      System.out.println(i);
    }
    int diameterEnd =
      viewer.scaleToScreen(controlPointScreens[iNext1].z, madEnd);
    int diameterMid =
      viewer.scaleToScreen(monomers[i].getLeadAtom().getScreenZ(),
                           madThis);
    g3d.fillHermite(colix, monomers[i].isNucleic() ? 4 : 7,
                    diameterBeg, diameterMid, diameterEnd,
                    controlPointScreens[iPrev1], controlPointScreens[i],
                    controlPointScreens[iNext1], controlPointScreens[iNext2]);
  }
}
