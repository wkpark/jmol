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

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;

import java.util.BitSet;

abstract class MpsRenderer extends MeshRenderer {

  
  Mps.Mpspolymer thisChain;
  
  int aspectRatio;
  int hermiteLevel;
  
  boolean isHighRes;
  boolean isTraceAlpha; 
  boolean isNucleic;
  boolean isCarbohydrate;
  boolean ribbonBorder = false;
  BitSet bsVisible = new BitSet();
  Point3i[] ribbonTopScreens;
  Point3i[] ribbonBottomScreens;

  Mesh[] meshes;
  boolean[] meshReady;

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
    boolean invalidate = false;

    boolean TF = viewer.getHighResolution();
    if (isHighRes != TF)
      invalidate = true;
    isHighRes = TF;

    TF = viewer.getTraceAlpha();
    if (isTraceAlpha != TF)
      invalidate = true;
    isTraceAlpha = TF;

    int val = viewer.getRibbonAspectRatio();
    if (val != aspectRatio && aspectRatio != 0)
      invalidate = true;
    aspectRatio = Math.min(Math.max(0, val), 20);
    
    val = viewer.getHermiteLevel();
    if (val != hermiteLevel)
      invalidate = true;
    hermiteLevel = Math.min(Math.max(0, val), 8);
    if (hermiteLevel == 0)
      aspectRatio = 0;

    thisChain = schain;
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
    meshReady = schain.meshReady;
    meshes = schain.meshes;
    mads = schain.mads;
    colixes = schain.colixes;
    leadAtomIndices = schain.polymer.getLeadAtomIndices();
    setStructureBooleans();
    bsVisible.clear();
    for (int i = monomerCount; --i >= 0;) {
      if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) == 0
          || frame.bsHidden.get(leadAtomIndices[i]))
        continue;
      Atom lead = frame.atoms[leadAtomIndices[i]];
      if (!g3d.isInDisplayRange(lead.screenX, lead.screenY))
        continue;
      bsVisible.set(i);
      if (invalidate)
        schain.falsifyMesh(i, false);
    }
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
    calcScreenControlPoints(controlPoints);  
  }
  
  void calcScreenControlPoints(Point3f[] points) {
    int count = monomerCount + 1;
    controlPointScreens = viewer.allocTempScreens(count);
    for (int i = count; --i >= 0; ) {
      viewer.transformPoint(points[i], controlPointScreens[i]);
    }
    haveControlPointScreens = true;
  }

  final Point3f pointT = new Point3f();
  /**
   * calculate screen points based on control points and wing positions
   * (cartoon, strand, meshRibbon, and ribbon)
   * 
   * @param offsetFraction
   * @return Point3i array THAT MUST BE LATER FREED
   */
  Point3i[] calcScreens(float offsetFraction) {
    int count = controlPoints.length;
    Point3i[] screens = viewer.allocTempScreens(count);
    if (offsetFraction == 0) {
      for (int i = count; --i >= 0;)
        viewer.transformPoint(controlPoints[i], screens[i]);
    } else {
      float offset_1000 = offsetFraction / 1000f;
      for (int i = count; --i >= 0;)
        calc1Screen(controlPoints[i], wingVectors[i], mads[i], offset_1000,
            screens[i]);
    }
    return screens;
  }
  
  private void calc1Screen(Point3f center, Vector3f vector,
                   short mad, float offset_1000, Point3i screen) {
    pointT.set(vector);
    float scale = mad * offset_1000;
    pointT.scaleAdd(scale, center);
    viewer.transformPoint(pointT, screen);
  }

  short getLeadColix(int i) {
    return Graphics3D.inheritColix(colixes[i], monomers[i].getLeadAtom().colixAtom);
  }
  
  //// cardinal hermite constant cylinder (meshRibbon, strands)
  
  final void renderHermiteCylinder(Point3i[] screens, int i) {
    int iPrev = Math.max(i - 1, 0);
    int iNext = Math.min(i + 1, monomerCount);
    int iNext2 = Math.min(i + 2, monomerCount);
    g3d.drawHermite(getLeadColix(i), isNucleic ? 4 : 7, screens[iPrev],
        screens[i], screens[iNext], screens[iNext2]);
  }

  //// cardinal hermite variable conic (cartoons, rockets, trace)

  final void renderHermiteConic(int i, boolean isSpecial) {
    int iPrev = Math.max(i - 1, 0);
    int iNext = Math.min(i + 1, monomerCount);
    int iNext2 = Math.min(i + 2, monomerCount);
    int madMid, madBeg, madEnd;
    madMid = madBeg = madEnd = mads[i];
    if (isSpecial) {
      if (isTraceAlpha) {
        if (!isSpecials[iNext]) {
          madEnd = mads[iNext];
          madMid = (madBeg + madEnd) / 2;
        }
      } else {
        if (!isSpecials[iPrev])
          madBeg = (mads[iPrev] + madMid) / 2;
        if (!isSpecials[iNext])
          madEnd = (mads[iNext] + madMid) / 2;
      }
    }
    int diameterMid = viewer.scaleToScreen(monomers[i].getLeadAtom()
        .getScreenZ(), madMid);

    if (aspectRatio > 0 && (isHighRes & diameterMid > ABSOLUTE_MIN_MESH_SIZE || diameterMid >= MIN_MESH_RENDER_SIZE)) {
      try {
        if (meshes[i] == null || !meshReady[i])
          createMeshCylinder(i, madBeg, madMid, madEnd, 1);
        meshes[i].colix = getLeadColix(i);
        render1(meshes[i]);
        return;
      } catch (Exception e) {
        System.out.println("render mesh error");
      }
    }    
    int diameterBeg = viewer.scaleToScreen(controlPointScreens[i].z, madBeg);
    int diameterEnd = viewer
        .scaleToScreen(controlPointScreens[iNext].z, madEnd);
    g3d.fillHermite(getLeadColix(i), isNucleic ? 4 : 7, diameterBeg,
        diameterMid, diameterEnd, controlPointScreens[iPrev],
        controlPointScreens[i], controlPointScreens[iNext],
        controlPointScreens[iNext2]);
  }
  
  //// cardinal hermite box or flat ribbon or twin strand (cartoons, meshRibbon, ribbon)

  final void renderHermiteRibbon(boolean doFill, int i) {
    int iPrev = Math.max(i - 1, 0);
    int iNext = Math.min(i + 1, monomerCount);
    int iNext2 = Math.min(i + 2, monomerCount);
    
    if (doFill && aspectRatio != 0) {
      int madMid, madBeg, madEnd;
      madMid = madBeg = madEnd = mads[i];
      if (isTraceAlpha) {
          if (!isSpecials[iNext]) {
            madEnd = mads[iNext];
            madMid = (madBeg + madEnd) / 2;
          }
        } else {
          if (!isSpecials[iPrev])
            madBeg = (mads[iPrev] + madMid) / 2;
          if (!isSpecials[iNext])
            madEnd = (mads[iNext] + madMid) / 2;
        }
      int diameterMid = viewer.scaleToScreen(monomers[i].getLeadAtom()
          .getScreenZ(), madMid);
      if (isHighRes & diameterMid > ABSOLUTE_MIN_MESH_SIZE || diameterMid >= MIN_MESH_RENDER_SIZE) {
        try {
          if (meshes[i] == null || !meshReady[i])
            createMeshCylinder(i, madBeg, madMid, madEnd, aspectRatio);
          meshes[i].colix = getLeadColix(i);
          render1(meshes[i]);
          return;
        } catch (Exception e) {
          System.out.println("render mesh error:" + e.toString());
        }
      }    
   
    }
    g3d.drawHermite(doFill, ribbonBorder, getLeadColix(i), isNucleic ? 4 : 7,
        ribbonTopScreens[iPrev], ribbonTopScreens[i],
        ribbonTopScreens[iNext], ribbonTopScreens[iNext2],
        ribbonBottomScreens[iPrev], ribbonBottomScreens[i],
        ribbonBottomScreens[iNext], ribbonBottomScreens[iNext2],
        aspectRatio);
  }

  //// cardinal hermite box or flat arrow head (cartoon)

  final Point3i screenArrowTop = new Point3i();
  final Point3i screenArrowTopPrev = new Point3i();
  final Point3i screenArrowBot = new Point3i();
  final Point3i screenArrowBotPrev = new Point3i();

  final void renderHermiteArrowHead(int i) {
    short colix = getLeadColix(i);
    int iPrev = Math.max(i - 1, 0);
    int iNext = Math.min(i + 1, monomerCount);
    int iNext2 = Math.min(i + 2, monomerCount);
    calc1Screen(controlPoints[i], wingVectors[i], mads[i], .7f / 1000,
        screenArrowTop);
    calc1Screen(controlPoints[i], wingVectors[i], mads[i], -.7f / 1000,
        screenArrowBot);
    calc1Screen(controlPoints[iPrev], wingVectors[iPrev], mads[iPrev],
        1.0f / 1000, screenArrowTopPrev);
    calc1Screen(controlPoints[iPrev], wingVectors[iPrev], mads[iPrev],
        -1.0f / 1000, screenArrowBotPrev);
    if (ribbonBorder && aspectRatio == 0)
      g3d.fillCylinder(colix, colix, Graphics3D.ENDCAPS_SPHERICAL, 3,
          screenArrowTop.x, screenArrowTop.y, screenArrowTop.z,
          screenArrowBot.x, screenArrowBot.y, screenArrowBot.z);
    g3d.drawHermite(true, ribbonBorder, colix, isNucleic ? 4 : 7,
        screenArrowTopPrev, screenArrowTop, controlPointScreens[iNext],
        controlPointScreens[iNext2], screenArrowBotPrev, screenArrowBot,
        controlPointScreens[iNext], controlPointScreens[iNext2], aspectRatio);
  }
  
  //////////////////////////// mesh 
  
  // Bob Hanson 11/03/2006 - first attempt at mesh rendering of 
  // secondary structure.
  // mesh creation occurs at rendering time, because we don't
  // know what all the options are, and they aren't important,
  // until it gets rendered, if ever

  Point3f[] controlHermites;
  Vector3f[] wingHermites;
  
  final Vector3f Z = new Vector3f(0.1345f,0.5426f,0.3675f); //random reference
  final Vector3f wing = new Vector3f();
  final Vector3f wing0 = new Vector3f();
  final Vector3f wing1 = new Vector3f();
  final Vector3f wingT = new Vector3f();
  final AxisAngle4f aa = new AxisAngle4f();
  final Point3f pt = new Point3f();
  final Point3f pt1 = new Point3f();
  final Matrix3f mat = new Matrix3f();

  void createMeshCylinder(int i, int madBeg, int madMid, int madEnd,
                          int aspectRatio) {
    boolean isEccentric = (aspectRatio != 1 && wingVectors != null);
    Vector3f norm = new Vector3f();
    int nHermites = (hermiteLevel + 1) * 2; // 4 for hermiteLevel = 1
    int nPer = nHermites * 2 - 2; // 6 for hermiteLevel 1
    int iPrev = Math.max(i - 1, 0);
    int iNext = Math.min(i + 1, monomerCount);
    int iNext2 = Math.min(i + 2, monomerCount);
    int iNext3 = Math.min(i + 3, monomerCount);
    norm.sub(controlPoints[i], controlPoints[iNext]);
    if (norm.length() == 0)
      return;
    Mesh mesh = meshes[i] = new Mesh(viewer, "mesh_" + shapeID + "_" + i, g3d,
        getLeadColix(i));
    float radius1 = madBeg / 2000f;
    float radius2 = madMid / 2000f;
    float radius3 = madEnd / 2000f;
    float dr = (radius2 - radius1) / nHermites * 2;
    float dr2 = (radius3 - radius2) / nHermites * 2;
    controlHermites = new Point3f[nHermites + 1];
    Graphics3D.getHermiteList(isNucleic ? 4 : 7, controlPoints[iPrev],
        controlPoints[i], controlPoints[iNext], controlPoints[iNext2],
        controlPoints[iNext3], controlHermites);
    //System.out.println("create mesh " + thisChain + " mesh_" + shapeID + "_"+i+controlPoints[i] + controlPoints[iNext]);
    if (isEccentric) {
      wingHermites = new Vector3f[nHermites + 1];
      Graphics3D.getHermiteList(isNucleic ? 4 : 7, wingVectors[iPrev],
          wingVectors[i], wingVectors[iNext], wingVectors[iNext2],
          wingVectors[iNext3], wingHermites);
    }
    int nPoints = 0;
    norm.sub(controlHermites[1], controlHermites[0]);

    if (!isEccentric) {
      wing0.cross(norm, Z);
      wing0.cross(norm, wing0);
    }
    int iMid = nHermites / 2;
    for (int p = 0; p < nHermites; p++) {
      norm.sub(controlHermites[p + 1], controlHermites[p]);
      if (isEccentric) {
        wing1.set(wingHermites[p]);
        wing.set(wingHermites[p]);
        //dumpVector(controlHermites[p],wing)
      } else {
        wing.cross(norm, wing0);
      }
      wing.normalize();
      if (isEccentric)
        wing.scale(2f / aspectRatio);
      if (p < iMid)
        wing.scale(radius1 + dr * p);
      else
        wing.scale(radius2 + dr2 * (p - iMid));
      aa.set(norm, (float) (2 * Math.PI / nPer));
      mat.set(aa);
      pt1.set(controlHermites[p]);
      for (int k = 0; k < nPer; k++) {
        mat.transform(wing);
        wingT.set(wing);
        if (isEccentric) {
          if (k == (nPer + 2) / 4 || k == (3 * nPer + 2) / 4)
            wing1.scale(-1);
          wingT.add(wing1);
        }
        //shaping would be done here
        pt.add(pt1, wingT);
        if (isEccentric) {
          //dumpVector(wingHermites[p], pt);
        }
        mesh.addVertexCopy(pt);
      }
      if (p > 0) {
        for (int k = 0; k < nPer; k++) {
          mesh.addQuad(nPoints - nPer + k, nPoints - nPer + ((k + 1) % nPer),
              nPoints + ((k + 1) % nPer), nPoints + k);
        }
      }
      nPoints += nPer;
    }
    mesh.initialize();
    meshReady[i] = true;
    mesh.visibilityFlags = 1;
  }

  //all mesh creation is does as needed, during rendering
  //so screenZ has been defined by then
  
  final static int ABSOLUTE_MIN_MESH_SIZE = 3;
  final static int MIN_MESH_RENDER_SIZE = 8;
  boolean renderAsMesh(int monomerIndex, int d) {
    Atom atom = monomers[monomerIndex].getLeadAtom();
    return (viewer.scaleToScreen(atom.screenZ, d) >= MIN_MESH_RENDER_SIZE);
  }

  void dumpVector(Point3f pt, Vector3f v) {
    Point3f p1 = new Point3f();
    p1.add(pt, v);
    Point3i pt1 = viewer.transformPoint(pt);
    Point3i pt2 = viewer.transformPoint(p1);
    System.out.print("draw pt"+(""+Math.random()).substring(3,10) + " {"+pt.x+" "+pt.y+" "+pt.z+"} {"+p1.x+" "+p1.y+" "+p1.z+"}"+";"+" ");
    g3d.fillCylinder(Graphics3D.GOLD, Graphics3D.ENDCAPS_FLAT, 5, pt1.x, pt1.y, pt1.z, pt2.x, pt2.y, pt2.z);
    g3d.fillSphereCentered(Graphics3D.GOLD, 5, pt1);
  }
}
