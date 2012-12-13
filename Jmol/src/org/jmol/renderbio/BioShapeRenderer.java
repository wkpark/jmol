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

package org.jmol.renderbio;

import org.jmol.constant.EnumStructure;
import org.jmol.modelset.Atom; //import org.jmol.modelsetbio.AlphaMonomer;
import org.jmol.modelsetbio.CarbohydratePolymer;
import org.jmol.modelsetbio.Monomer;
import org.jmol.modelsetbio.NucleicPolymer; //import org.jmol.modelsetbio.ProteinStructure;
import org.jmol.render.MeshRenderer;
import org.jmol.script.Token;
import org.jmol.shape.Mesh;
import org.jmol.shapebio.BioShape;
import org.jmol.shapebio.BioShapeCollection;
import org.jmol.util.AxisAngle4f;
import org.jmol.util.BitSet;
import org.jmol.util.Colix;
import org.jmol.util.GData;
import org.jmol.util.Hermite;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Normix;
import org.jmol.util.Point3f;
import org.jmol.util.Point3i;
import org.jmol.util.Vector3f;

abstract class BioShapeRenderer extends MeshRenderer {

  //ultimately this renderer calls MeshRenderer.render1(mesh)

  private boolean invalidateMesh;
  private boolean invalidateSheets;
  private boolean isHighRes;
  private boolean isTraceAlpha;
  private boolean ribbonBorder = false;
  private boolean haveControlPointScreens;
  private float aspectRatio;
  private int hermiteLevel;
  private float sheetSmoothing;
  protected boolean meshElliptical;


  private Mesh[] meshes;
  private boolean[] meshReady;
  private BitSet bsRenderMesh;


  protected int monomerCount;
  protected Monomer[] monomers;

  protected boolean isNucleic;
  protected boolean isCarbohydrate;
  protected BitSet bsVisible = new BitSet();
  protected Point3i[] ribbonTopScreens;
  protected Point3i[] ribbonBottomScreens;
  protected Point3f[] controlPoints;
  protected Point3i[] controlPointScreens;

  protected int[] leadAtomIndices;
  protected Vector3f[] wingVectors;
  protected short[] mads;
  protected short[] colixes;
  protected short[] colixesBack;
  protected EnumStructure[] structureTypes;
  
  protected boolean isPass2;

  protected abstract void renderBioShape(BioShape bioShape);

  @Override
  protected boolean render() {
    if (shape == null)
      return false;
    isPass2 = g3d.isPass2();
    invalidateMesh = false;
    needTranslucent = false;
    boolean TF = isExport || viewer.getHighResolution();
    if (TF != isHighRes)
      invalidateMesh = true;
    isHighRes = TF;

    boolean v = viewer.getCartoonFlag(Token.cartoonfancy);
    if (meshElliptical != v) {
      invalidateMesh = true;
      meshElliptical = v;
    }
    int val1 = viewer.getHermiteLevel();
    val1 = (val1 <= 0 ? -val1 : viewer.getInMotion() ? 0 : val1);
    if (meshElliptical)
      val1 = Math.max(val1, 3); // at least HermiteLevel 3 for "cartoonFancy"
    else if (val1 == 0 && exportType == GData.EXPORT_CARTESIAN)
      val1 = 5; // forces hermite for 3D exporters
    if (val1 != hermiteLevel && val1 != 0)
      invalidateMesh = true;
    hermiteLevel = Math.min(val1, 8);

    int val = viewer.getRibbonAspectRatio();
    val = Math.min(Math.max(0, val), 20);
    if (meshElliptical && val >= 16)
      val = 4; // at most 4 for elliptical cartoonFancy
    if (hermiteLevel == 0)
      val = 0;

    if (val != aspectRatio && val != 0 && val1 != 0)
      invalidateMesh = true;
    aspectRatio = val;


    TF = (viewer.getTraceAlpha());
    if (TF != isTraceAlpha)
      invalidateMesh = true;
    isTraceAlpha = TF;

    invalidateSheets = false;
    float fval = viewer.getSheetSmoothing();
    if (fval != sheetSmoothing && isTraceAlpha) {
      sheetSmoothing = fval;
      invalidateMesh = true;
      invalidateSheets = true;
    }

    BioShapeCollection mps = (BioShapeCollection) shape;
    for (int c = mps.bioShapes.length; --c >= 0;) {
      BioShape bioShape = mps.getBioShape(c);
      if ((bioShape.modelVisibilityFlags & myVisibilityFlag) == 0)
        continue;
      if (bioShape.monomerCount >= 2 && initializePolymer(bioShape)) {
        bsRenderMesh.clearAll();    
        renderBioShape(bioShape);
        renderMeshes();
        freeTempArrays();
      }
    }
    return needTranslucent;
  }

  protected boolean setBioColix(short colix) {
    if (g3d.setColix(colix))
      return  true;
    needTranslucent = true;
    return false;
  }

  private void freeTempArrays() {
    if (haveControlPointScreens)
      viewer.freeTempScreens(controlPointScreens);
    viewer.freeTempEnum(structureTypes);
  }

  private boolean initializePolymer(BioShape bioShape) {
    if (viewer.isJmolDataFrameForModel(bioShape.modelIndex)) {
      controlPoints = bioShape.bioPolymer.getControlPoints(true, 0, false);
    } else {
      controlPoints = bioShape.bioPolymer.getControlPoints(isTraceAlpha,
          sheetSmoothing, invalidateSheets);
    }
    monomerCount = bioShape.monomerCount;
    bsRenderMesh = BitSet.newN(monomerCount);
    monomers = bioShape.monomers;
    leadAtomIndices = bioShape.bioPolymer.getLeadAtomIndices();

    bsVisible.clearAll();
    boolean haveVisible = false;
    if (invalidateMesh)
      bioShape.falsifyMesh();
    for (int i = monomerCount; --i >= 0;) {
      if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) == 0
          || modelSet.isAtomHidden(leadAtomIndices[i]))
        continue;
      Atom lead = modelSet.atoms[leadAtomIndices[i]];
      if (!g3d.isInDisplayRange(lead.screenX, lead.screenY))
        continue;
      bsVisible.set(i);
      haveVisible = true;
    }
    if (!haveVisible)
      return false;
    ribbonBorder = viewer.getRibbonBorder();

    // note that we are not treating a PhosphorusPolymer
    // as nucleic because we are not calculating the wing
    // vector correctly.
    // if/when we do that then this test will become
    // isNucleic = bioShape.bioPolymer.isNucleic();

    isNucleic = bioShape.bioPolymer instanceof NucleicPolymer;
    isCarbohydrate = bioShape.bioPolymer instanceof CarbohydratePolymer;
    haveControlPointScreens = false;
    wingVectors = bioShape.wingVectors;
    meshReady = bioShape.meshReady;
    meshes = bioShape.meshes;
    mads = bioShape.mads;
    colixes = bioShape.colixes;
    colixesBack = bioShape.colixesBack;
    setStructureTypes();
    return true;
  }

  private void setStructureTypes() {
    structureTypes = viewer.allocTempEnum(monomerCount + 1);
    for (int i = monomerCount; --i >= 0;) {
      structureTypes[i] = monomers[i].getProteinStructureType();
      if (structureTypes[i] == EnumStructure.TURN)
        structureTypes[i] = EnumStructure.NONE;
    }
    structureTypes[monomerCount] = structureTypes[monomerCount - 1];
  }

  protected boolean isHelix(int i) {
    return structureTypes[i] == EnumStructure.HELIX;
  }

  protected void getScreenControlPoints() {
    calcScreenControlPoints(controlPoints);
  }

  protected void calcScreenControlPoints(Point3f[] points) {
    int count = monomerCount + 1;
    controlPointScreens = viewer.allocTempScreens(count);
    for (int i = count; --i >= 0;) {
      viewer.transformPtScr(points[i], controlPointScreens[i]);
    }
    haveControlPointScreens = true;
  }

  /**
   * calculate screen points based on control points and wing positions
   * (cartoon, strand, meshRibbon, and ribbon)
   * 
   * @param offsetFraction
   * @return Point3i array THAT MUST BE LATER FREED
   */
  protected Point3i[] calcScreens(float offsetFraction) {
    int count = controlPoints.length;
    Point3i[] screens = viewer.allocTempScreens(count);
    if (offsetFraction == 0) {
      for (int i = count; --i >= 0;)
        viewer.transformPtScr(controlPoints[i], screens[i]);
    } else {
      float offset_1000 = offsetFraction / 1000f;
      for (int i = count; --i >= 0;)
        calc1Screen(controlPoints[i], wingVectors[i],
            (mads[i] == 0 && i > 0 ? mads[i - 1] : mads[i]), offset_1000,
            screens[i]);
    }
    return screens;
  }

  private final Point3f pointT = new Point3f();

  private void calc1Screen(Point3f center, Vector3f vector, short mad,
                           float offset_1000, Point3i screen) {
    pointT.setT(vector);
    float scale = mad * offset_1000;
    pointT.scaleAdd(scale, center);
    viewer.transformPtScr(pointT, screen);
  }

  protected short getLeadColix(int i) {
    return Colix.getColixInherited(colixes[i], monomers[i].getLeadAtom()
        .getColix());
  }

  protected short getLeadColixBack(int i) {
    return (colixesBack == null || colixesBack.length <= i ? 0 : colixesBack[i]);
  }

  //// cardinal hermite constant cylinder (meshRibbon, strands)

  private int iPrev, iNext, iNext2, iNext3;
  private int diameterBeg, diameterMid, diameterEnd;
  private boolean doCap0, doCap1;
  protected short colixBack;

  private void setNeighbors(int i) {
    iPrev = Math.max(i - 1, 0);
    iNext = Math.min(i + 1, monomerCount);
    iNext2 = Math.min(i + 2, monomerCount);
    iNext3 = Math.min(i + 3, monomerCount);
  }

  /**
   * set diameters for a bioshape
   * 
   * @param i
   * @param thisTypeOnly true for Cartoon but not MeshRibbon
   * @return true if a mesh is needed
   */
  private boolean setMads(int i, boolean thisTypeOnly) {
    madMid = madBeg = madEnd = mads[i];
    if (isTraceAlpha) {
      if (!thisTypeOnly || structureTypes[i] == structureTypes[iNext]) {
        madEnd = mads[iNext];
        if (madEnd == 0) {
          if (this instanceof TraceRenderer) {
            madEnd = madBeg;
          } else {
            madEnd = madBeg;
          }
        }
        madMid = (short) ((madBeg + madEnd) >> 1);
      }
    } else {
      if (!thisTypeOnly || structureTypes[i] == structureTypes[iPrev])
        madBeg = (short) (((mads[iPrev] == 0 ? madMid : mads[iPrev]) + madMid) >> 1);
      if (!thisTypeOnly || structureTypes[i] == structureTypes[iNext])
        madEnd = (short) (((mads[iNext] == 0 ? madMid : mads[iNext]) + madMid) >> 1);
    }
    diameterBeg = viewer.scaleToScreen(controlPointScreens[i].z, madBeg);
    diameterMid = viewer.scaleToScreen(monomers[i].getLeadAtom().screenZ,
        madMid);
    diameterEnd = viewer.scaleToScreen(controlPointScreens[iNext].z, madEnd);
    doCap0 = (i == iPrev || thisTypeOnly
        && structureTypes[i] != structureTypes[iPrev]);
    doCap1 = (iNext == iNext2 || thisTypeOnly
        && structureTypes[i] != structureTypes[iNext]);
    return ((aspectRatio > 0 && (exportType == GData.EXPORT_CARTESIAN 
        || checkDiameter(diameterBeg)
        || checkDiameter(diameterMid) 
        || checkDiameter(diameterEnd))));
  }

  private boolean checkDiameter(int d) {
    return (isHighRes & d > ABSOLUTE_MIN_MESH_SIZE || d >= MIN_MESH_RENDER_SIZE);
  }

  protected void renderHermiteCylinder(Point3i[] screens, int i) {
    //strands
    colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    setNeighbors(i);
    g3d.drawHermite4(isNucleic ? 4 : 7, screens[iPrev], screens[i],
        screens[iNext], screens[iNext2]);
  }

  protected void renderHermiteConic(int i, boolean thisTypeOnly) {
    //cartoons, rockets, trace
    setNeighbors(i);
    colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    if (setMads(i, thisTypeOnly)) {
      try {
        if ((meshes[i] == null || !meshReady[i])
            && !createMesh(i, madBeg, madMid, madEnd, 1))
          return;
        meshes[i].setColix(colix);
        bsRenderMesh.set(i);
        return;
      } catch (Exception e) {
        Logger.error("render mesh error hermiteConic: " + e.toString());
        //System.out.println(e.getMessage());
      }
    }
    g3d.fillHermite(isNucleic ? 4 : 7, diameterBeg, diameterMid, diameterEnd,
        controlPointScreens[iPrev], controlPointScreens[i],
        controlPointScreens[iNext], controlPointScreens[iNext2]);
  }

  /**
   * 
   * @param doFill
   * @param i
   * @param thisTypeOnly true for Cartoon but not MeshRibbon
   */
  protected void renderHermiteRibbon(boolean doFill, int i, boolean thisTypeOnly) {
    // cartoons and meshRibbon
    setNeighbors(i);
    colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    colixBack = getLeadColixBack(i);
    if (doFill && aspectRatio != 0) {
      if (setMads(i, thisTypeOnly)) {
        try {
          if ((meshes[i] == null || !meshReady[i])
              && !createMesh(i, madBeg, madMid, madEnd, aspectRatio))
            return;
          meshes[i].setColix(colix);
          meshes[i].setColixBack(colixBack);
          bsRenderMesh.set(i);
          return;
        } catch (Exception e) {
          Logger.error("render mesh error hermiteRibbon: " + e.toString());
          //System.out.println(e.getMessage());
        }
      }
    }
    g3d.drawHermite7(doFill, ribbonBorder, isNucleic ? 4 : 7,
        ribbonTopScreens[iPrev], ribbonTopScreens[i], ribbonTopScreens[iNext],
        ribbonTopScreens[iNext2], ribbonBottomScreens[iPrev],
        ribbonBottomScreens[i], ribbonBottomScreens[iNext],
        ribbonBottomScreens[iNext2], (int) aspectRatio, colixBack);
  }

  //// cardinal hermite (box or flat) arrow head (cartoon)

  private final Point3i screenArrowTop = new Point3i();
  private final Point3i screenArrowTopPrev = new Point3i();
  private final Point3i screenArrowBot = new Point3i();
  private final Point3i screenArrowBotPrev = new Point3i();

  //cartoons
  protected void renderHermiteArrowHead(int i) {
    // cartoons only
    colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    colixBack = getLeadColixBack(i);
    setNeighbors(i);
    if (setMads(i, false)) {
      try {
        doCap0 = true;
        doCap1 = false;
        if ((meshes[i] == null || !meshReady[i])
            && !createMesh(i, (int) Math.floor(madBeg * 1.2), (int) Math.floor(madBeg * 0.6), 0,
                (aspectRatio == 1 ? aspectRatio : aspectRatio / 2)))
          return;
        meshes[i].setColix(colix);
        bsRenderMesh.set(i);
        return;
      } catch (Exception e) {
        Logger.error("render mesh error hermiteArrowHead: " + e.toString());
        //System.out.println(e.getMessage());
      }
    }

    calc1Screen(controlPoints[i], wingVectors[i], madBeg, .0007f,
        screenArrowTop);
    calc1Screen(controlPoints[i], wingVectors[i], madBeg, -.0007f,
        screenArrowBot);
    calc1Screen(controlPoints[i], wingVectors[i], madBeg, 0.001f,
        screenArrowTopPrev);
    calc1Screen(controlPoints[i], wingVectors[i], madBeg, -0.001f,
        screenArrowBotPrev);
    g3d.drawHermite7(true, ribbonBorder, isNucleic ? 4 : 7, screenArrowTopPrev,
        screenArrowTop, controlPointScreens[iNext],
        controlPointScreens[iNext2], screenArrowBotPrev, screenArrowBot,
        controlPointScreens[iNext], controlPointScreens[iNext2], (int) aspectRatio, colixBack);
    if (ribbonBorder && aspectRatio == 0) {
      g3d.fillCylinderXYZ(colix, colix,
          GData.ENDCAPS_SPHERICAL,
          (exportType == GData.EXPORT_CARTESIAN ? 50 : 3), //may not be right 0.05 
          screenArrowTop.x, screenArrowTop.y, screenArrowTop.z,
          screenArrowBot.x, screenArrowBot.y, screenArrowBot.z);
    }
  }

  //  rockets --not satisfactory yet
  /**
   * @param i
   *        IGNORED
   * @param pointBegin
   *        IGNORED
   * @param pointEnd
   *        IGNORED
   * @param screenPtBegin
   * @param screenPtEnd
   * 
   */
  protected void renderCone(int i, Point3f pointBegin, Point3f pointEnd,
                            Point3f screenPtBegin, Point3f screenPtEnd) {
    int coneDiameter = mad + (mad >> 2);
    coneDiameter = viewer.scaleToScreen((int) Math.floor(screenPtBegin.z),
        coneDiameter);
    g3d.fillConeSceen3f(GData.ENDCAPS_FLAT, coneDiameter, screenPtBegin,
        screenPtEnd);
  }

  //////////////////////////// mesh 

  // Bob Hanson 11/04/2006 - mesh rendering of secondary structure.
  // mesh creation occurs at rendering time, because we don't
  // know what all the options are, and they aren't important,
  // until it gets rendered, if ever

  private final static int ABSOLUTE_MIN_MESH_SIZE = 3;
  private final static int MIN_MESH_RENDER_SIZE = 8;

  private Point3f[] controlHermites;
  private Vector3f[] wingHermites;
  private Point3f[] radiusHermites;

  private Vector3f norm = new Vector3f();
  private final Vector3f wing = new Vector3f();
  private final Vector3f wing1 = new Vector3f();
  private final Vector3f wingT = new Vector3f();
  private final AxisAngle4f aa = new AxisAngle4f();
  private final Point3f pt = new Point3f();
  private final Point3f pt1 = new Point3f();
  private final Point3f ptPrev = new Point3f();
  private final Point3f ptNext = new Point3f();
  private final Matrix3f mat = new Matrix3f();

  /**
   * 
   * @param i
   * @param madBeg
   * @param madMid
   * @param madEnd
   * @param aspectRatio
   * @return true if deferred rendering is required due to normals averaging
   */
  private boolean createMesh(int i, int madBeg, int madMid, int madEnd,
                             float aspectRatio) {
    setNeighbors(i);
    if (controlPoints[i].distance(controlPoints[iNext]) == 0)
      return false;
    boolean isEccentric = (aspectRatio != 1 && wingVectors != null);
    int nHermites = (hermiteLevel + 1) * 2 + 1; // 4 for hermiteLevel = 1; 13 for hermitelevel 5
    int nPer = (hermiteLevel + 1) * 4 - 2; // 6 for hermiteLevel 1; 22 for hermiteLevel 5
    Mesh mesh = meshes[i] = new Mesh("mesh_" + shapeID + "_" + i, (short) 0, i);
    boolean variableRadius = (madBeg != madMid || madMid != madEnd);
    if (controlHermites == null || controlHermites.length < nHermites + 1) {
      controlHermites = new Point3f[nHermites + 1];
    }
    Hermite.getHermiteList(isNucleic ? 4 : 7, controlPoints[iPrev],
        controlPoints[i], controlPoints[iNext], controlPoints[iNext2],
        controlPoints[iNext3], controlHermites, 0, nHermites, true);
    //    if (isEccentric) {
    // wing hermites determine the orientation of the cartoon
    if (wingHermites == null || wingHermites.length < nHermites + 1) {
      wingHermites = new Vector3f[nHermites + 1];
    }
    wing.setT(wingVectors[iPrev]);
    if (madEnd == 0)
      wing.scale(2.0f); //adds a flair to an arrow
    Hermite.getHermiteList(isNucleic ? 4 : 7, wing, wingVectors[i],
        wingVectors[iNext], wingVectors[iNext2], wingVectors[iNext3],
        wingHermites, 0, nHermites, false);
    //    }
    // radius hermites determine the thickness of the cartoon
    float radius1 = madBeg / 2000f;
    float radius2 = madMid / 2000f;
    float radius3 = madEnd / 2000f;
    if (variableRadius) {
      if (radiusHermites == null
          || radiusHermites.length < ((nHermites + 1) >> 1) + 1) {
        radiusHermites = new Point3f[((nHermites + 1) >> 1) + 1];
      }
      ptPrev.set(radius1, radius1, 0);
      pt.set(radius1, radius2, 0);
      pt1.set(radius2, radius3, 0);
      ptNext.set(radius3, radius3, 0);
      // two for the price of one!
      Hermite.getHermiteList(4, ptPrev, pt, pt1, ptNext, ptNext,
          radiusHermites, 0, (nHermites + 1) >> 1, true);
    }

    // now create the cartoon polygon

    int nPoints = 0;
    int iMid = nHermites >> 1;
    boolean isElliptical = (meshElliptical || hermiteLevel >= 6);
    for (int p = 0; p < nHermites; p++) {
      norm.sub2(controlHermites[p + 1], controlHermites[p]);
      float scale = (!variableRadius ? radius1 : p < iMid ? radiusHermites[p].x
          : radiusHermites[p - iMid].y);
      if (isEccentric) {
        wing.setT(wingHermites[p]);
        wing1.setT(wing);
        if (isElliptical) {
          wing1.cross(norm, wing);
          wing1.normalize();
          wing1.scale(wing.length() / aspectRatio);
        } else {
          wing.scale(2 / aspectRatio);
          wing1.sub(wing);
        }
      } else {
        wing.setT(wingHermites[p]);
        wing.scale(2f);
        wing.cross(norm, wing);
        wing.cross(norm, wing);
        wing.cross(norm, wing);
        wing.normalize();
      }
      wing.scale(scale);
      wing1.scale(scale);
      float angle = (float) (2 * Math.PI / nPer);
      aa.setVA(norm, angle);
      mat.setAA(aa);
      pt1.setT(controlHermites[p]);
      float theta = angle;
      for (int k = 0; k < nPer; k++, theta += angle) {
        if (!isElliptical || !isEccentric)
          mat.transform(wing);
        if (isEccentric) {
          if (isElliptical) {
            float cos = (float) Math.cos(theta);
            float sin = (float) Math.sin(theta);
            wingT.setT(wing1);
            wingT.scale(sin);
            wingT.scaleAdd2(cos, wing, wingT);
          } else {
            wingT.setT(wing);
            if (k == (nPer + 2) / 4 || k == (3 * nPer + 2) / 4)
              wing1.scale(-1);
            wingT.add(wing1);
          }
          pt.add2(pt1, wingT);
        } else {
          pt.add2(pt1, wing);
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
    if (doCap0)
      for (int k = hermiteLevel * 2; --k >= 0;)
        mesh.addQuad(k + 2, k + 1, (nPer - k) % nPer, nPer - k - 1);
    if (doCap1)
      for (int k = hermiteLevel * 2; --k >= 0;)
        mesh.addQuad(nPoints - k - 1, nPoints - nPer + (nPer - k) % nPer,
            nPoints - nPer + k + 1, nPoints - nPer + k + 2);
    meshReady[i] = true;
    adjustCartoonSeamNormals(i, nPer);
    mesh.setVisibilityFlags(1);
    return true;
  }

  private BitSet bsTemp;
  private final Vector3f norml = new Vector3f();

  /**
   * Matches normals for adjacent mesh sections to create a seamless overall
   * mesh. We use temporary normals here. We will convert normals to normixes
   * later.
   * 
   * @author Alexander Rose
   * @author Bob Hanson
   * 
   * @param i
   * @param nPer
   */
  void adjustCartoonSeamNormals(int i, int nPer) {
    if (bsTemp == null)
      bsTemp = Normix.newVertexBitSet();
    if (i == iNext - 1 && iNext < monomerCount
        && monomers[i].getStrucNo() == monomers[iNext].getStrucNo()
        && meshReady[i] && meshReady[iNext]) {
      try {
        Vector3f[] normals2 = meshes[iNext].getNormalsTemp();
        Vector3f[] normals = meshes[i].getNormalsTemp();
        int normixCount = normals.length;
        for (int j = 1; j <= nPer; ++j) {
          norml.add2(normals[normixCount - j], normals2[nPer - j]);
          norml.normalize();
          meshes[i].normalsTemp[normixCount - j].setT(norml);
          meshes[iNext].normalsTemp[nPer - j].setT(norml);
        }
      } catch (Exception e) {
      }
    }
  }

  private void renderMeshes() {
    for (int i = bsRenderMesh.nextSetBit(0); i >= 0; i = bsRenderMesh
        .nextSetBit(i + 1)) {
      if (meshes[i].normalsTemp != null) {
        meshes[i].setNormixes(meshes[i].normalsTemp);
        meshes[i].normalsTemp = null;
      } else if (meshes[i].normixes == null) {
        meshes[i].initialize(Token.frontlit, null, null);
      }
      renderMesh(meshes[i]);
    }
  }

  /*
  private void dumpPoint(Point3f pt, short color) {
    Point3i pt1 = viewer.transformPoint(pt);
    g3d.fillSphereCentered(color, 20, pt1);
  }

  private void dumpVector(Point3f pt, Vector3f v, short color) {
    Point3f p1 = new Point3f();
    Point3i pt1 = new Point3i();
    Point3i pt2 = new Point3i();
    p1.add(pt, v);
    pt1.set(viewer.transformPoint(pt));
    pt2.set(viewer.transformPoint(p1));
    System.out.print("draw pt" + ("" + Math.random()).substring(3, 10) + " {"
        + pt.x + " " + pt.y + " " + pt.z + "} {" + p1.x + " " + p1.y + " "
        + p1.z + "}" + ";" + " ");
    g3d.fillCylinder(color, GData.ENDCAPS_FLAT, 2, pt1.x, pt1.y, pt1.z,
        pt2.x, pt2.y, pt2.z);
    g3d.fillSphereCentered(color, 5, pt2);
  }
  */

}
