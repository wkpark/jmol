/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-24 20:49:07 -0500 (Tue, 24 Apr 2007) $
 * $Revision: 7483 $
 *
 * Copyright (C) 2005  Miguel, The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net, jmol-developers@lists.sf.net
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

package org.jmol.shape;


import java.util.Hashtable;

import java.util.Map;

import org.jmol.java.BS;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.SB;

import org.jmol.util.MeshSurface;
import org.jmol.util.Normix;
import org.jmol.viewer.Viewer;

import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;

import javajs.util.T3;
import javajs.util.V3;
import org.jmol.api.SymmetryInterface;

//import javax.vecmath.Matrix3f;

public class Mesh extends MeshSurface {
  
  public final static String PREVIOUS_MESH_ID = "+PREVIOUS_MESH+";

  public String[] title;
  
  public short meshColix;
  public short[] normixes;
  public Lst<P3[]> lineData;
  public String thisID;
  public boolean isValid = true;
  public String scriptCommand;
  public String colorCommand;
  public P3 lattice;
  public M4[] symops;
  public short[][] symopNormixes;
  public boolean visible = true;
  public int lighting = T.frontlit;
  public int colorType; // could be T.symop

  public boolean haveXyPoints;
  public int diameter;
  public float width;
  public P3 ptCenter;
  public Mesh linkedMesh; //for lcaoOrbitals
  public Map<String, BS> vertexColorMap;
  
  public V3 vAB, vTemp;

  public int color;
  public boolean useColix = true;
  public SymmetryInterface unitCell;
  
  public float scale3d = 0;

  public int index;
  public int atomIndex = -1;
  public int modelIndex = -1;  // for Isosurface and Draw
  public int visibilityFlags;
  public boolean insideOut;
  public int checkByteCount;

  public void setVisibilityFlags(int n) {
    visibilityFlags = n;//set to 1 in mps
  }

  public boolean showContourLines = false;
  public boolean showPoints = false;
  public boolean drawTriangles = false;
  public boolean fillTriangles = true;
  public boolean showTriangles = false; //as distinct entitities
  public boolean frontOnly = false;
  public boolean isTwoSided = true;
  public boolean havePlanarContours = false;

  /**
   * always use Mesh().mesh1(thisID, colix, index)
   * 
   * @j2sIgnore
   */
  public Mesh() {
  }
  
  public Mesh mesh1(Viewer vwr, String thisID, short colix, int index) {
    // prevents JavaScript 
    if (PREVIOUS_MESH_ID.equals(thisID))
      thisID = null;
    this.vwr = vwr;
    this.thisID = thisID;
    this.colix = colix;
    this.index = index;
    ptCenter = new P3();
    vAB = new V3();
    vTemp = new V3();

    //System.out.println("Mesh " + this + " constructed");
    return this;
  }

  //public void finalize() {
  //  System.out.println("Mesh " + this + " finalized");
  //}
  
  public void clear(String meshType) {
    clearMesh(meshType);
  }

  public void clearMesh(String meshType) {
    altVertices = null;
    bsDisplay = null;
    bsSlabDisplay = null;
    bsSlabGhost = null;
    symops = null;
    symopColixes = null;
    //bsTransPolygons = null;
    cappingObject = null;
    colix = C.GOLD;
    colorDensity = false;
    connections = null;
    diameter = 0;
    drawTriangles = false;
    fillTriangles = true;
    frontOnly = false;
    havePlanarContours = false;
    haveXyPoints = false;
    isTriangleSet = false;
    isTwoSided = false;
    lattice = null;
    mat4 = null;
    normixes = null;
    pis = null;
    //polygonTranslucencies = null;
    scale3d = 0;
    showContourLines = false;
    showPoints = false;
    showTriangles = false; //as distinct entities
    slabbingObject = null;
    slabOptions = null;
    spanningVectors = null;    
    symopNormixes = null;
    title = null;
    unitCell = null;
    useColix = true;
    vertexCount0 = polygonCount0 = vc = pc = 0;
    vs = null;
    vertexSource = null;
    volumeRenderPointSize = 0.15f;
    this.meshType = meshType;
  }

  protected BS bsTemp;
  
  public void initialize(int lighting, T3[] vertices, P4 plane) {
    if (vertices == null)
      vertices = this.vs;
    V3[] normals = getNormals(vertices, plane);
    setNormixes(normals);
    this.lighting = T.frontlit;
    if (insideOut)
      invertNormixes();
    setLighting(lighting);
  }

  public short[] setNormixes(V3[] normals) {
    if (normals == null)
      return (normixes = null);
    normixes = new short[normixCount];
    if (bsTemp == null)
      bsTemp = Normix.newVertexBitSet();
    if (haveXyPoints)
      for (int i = normixCount; --i >= 0;)
        normixes[i] = Normix.NORMIX_NULL;
    else
      for (int i = normixCount; --i >= 0;)
        normixes[i] = Normix.getNormixV(normals[i], bsTemp);
    return normixes;
  }

  public V3[] getNormals(T3[] vertices, P4 plane) {
    normixCount = (isTriangleSet ? pc : vc);
    if (normixCount < 0)
      return null;
    V3[] normals = new V3[normixCount];
    for (int i = normixCount; --i >= 0;)
      normals[i] = new V3();
    if (plane == null) {
      sumVertexNormals(vertices, normals);
    }else {
      V3 normal = V3.new3(plane.x, plane.y, plane.z); 
      for (int i = normixCount; --i >= 0;)
        normals[i] = normal;
    }
    if (!isTriangleSet)
      for (int i = normixCount; --i >= 0;) {
        normals[i].normalize();
      }
    return normals;
  }
  
  public void setLighting(int lighting) {
    isTwoSided = (lighting == T.fullylit);
    if (lighting == this.lighting)
      return;
    flipLighting(this.lighting);
    flipLighting(this.lighting = lighting);
  }
  
  private void flipLighting(int lighting) {
    if (lighting == T.fullylit) // this will not be a WebGL option
      for (int i = normixCount; --i >= 0;)
        normixes[i] = (short)~normixes[i];
    else if ((lighting == T.frontlit) == insideOut)
      invertNormixes();
  }

  private void invertNormixes() {
    Normix.setInverseNormixes();
    for (int i = normixCount; --i >= 0;)
      normixes[i] = Normix.getInverseNormix(normixes[i]);
  }

  public void setTranslucent(boolean isTranslucent, float iLevel) {
    colix = C.getColixTranslucent3(colix, isTranslucent, iLevel);
  }

  //public Vector data1;
  //public Vector data2;
  //public List<Object> xmlProperties;
  public boolean colorDensity;
  public Object cappingObject;
  public Object slabbingObject;
  public float volumeRenderPointSize = 0.15f;

  public int[] connections;

  public boolean recalcAltVertices;

  public short[] symopColixes;

  protected void sumVertexNormals(T3[] vertices, V3[] normals) {
    // subclassed in IsosurfaceMesh
    sumVertexNormals2(this, vertices, normals);
  }

  protected static void sumVertexNormals2(Mesh m, T3[] vertices, V3[] normals) {
    int adjustment = m.checkByteCount;
    float min = m.getMinDistance2ForVertexGrouping();
    for (int i = m.pc; --i >= 0;) {
      try {
        int[] face = m.setABC(i);
        if (face == null)
          continue;
        T3 vA = vertices[face[0]];
        T3 vB = vertices[face[1]];
        T3 vC = vertices[face[2]];
        // no skinny triangles
        if (vA.distanceSquared(vB) < min || vB.distanceSquared(vC) < min
            || vA.distanceSquared(vC) < min)
          continue;
        Measure.calcNormalizedNormal(vA, vB, vC, m.vTemp, m.vAB);
        if (m.isTriangleSet) {
          normals[i].setT(m.vTemp);
        } else {
          float l = m.vTemp.length();
          if (l > 0.9 && l < 1.1) // test for not infinity or -infinity or isNaN
            for (int j = face.length - adjustment; --j >= 0;) {
              int k = face[j];
              normals[k].add(m.vTemp);
            }
        }
      } catch (Exception e) {
        System.out.println(e);
      }
    }
  }

  protected float getMinDistance2ForVertexGrouping() {
    return 1e-8f; // different for an isosurface
  }

  public String getState(String type) {
    //String sxml = null; // problem here is that it can be WAY to large. Shape.getXmlPropertyString(xmlProperties, type);
    SB s = new SB();
    if (isValid) {
      //if (sxml != null)
      //s.append("/** XML ** ").append(sxml).append(" ** XML **/\n");
      s.append(type);
      if (!type.equals("mo") && !type.equals("nbo"))
        s.append(" ID ").append(PT.esc(thisID));
      if (lattice != null)
        s.append(" lattice ").append(Escape.eP(lattice));
      if (meshColix != 0)
        s.append(" color mesh ").append(C.getHexCode(meshColix));
      s.append(getRendering());
      if (!visible)
        s.append(" hidden");
      if (bsDisplay != null) {
        s.append(";\n  ").append(type);
        if (!type.equals("mo") && !type.equals("nbo"))
          s.append(" ID ").append(PT.esc(thisID));
        s.append(" display " + Escape.eBS(bsDisplay));
      }
    }
    return s.toString();
  }

  protected String getRendering() {
    SB s = new SB();
    s.append(fillTriangles ? " fill" : " noFill");
    s.append(drawTriangles ? " mesh" : " noMesh");
    s.append(showPoints ? " dots" : " noDots");
    s.append(frontOnly ? " frontOnly" : " notFrontOnly");
    if (showContourLines)
      s.append(" contourlines");
    if (showTriangles)
      s.append(" triangles");
    s.append(" ").append(T.nameOf(lighting));
    return s.toString();
  }

  public P3[] getOffsetVertices(P4 thePlane) {
    if (altVertices != null && !recalcAltVertices)
      return (P3[]) altVertices;
    altVertices = new P3[vc];
    for (int i = 0; i < vc; i++)
      altVertices[i] = P3.newP(vs[i]);
    V3 normal = null;
    float val = 0;
    if (scale3d != 0 && vvs != null && thePlane != null) {
        normal = V3.new3(thePlane.x, thePlane.y, thePlane.z);
        normal.normalize();
        normal.scale(scale3d);
        if (mat4 != null) {
          M3 m3 = new M3();
          mat4.getRotationScale(m3); 
          m3.rotate(normal);
        }
    }
    for (int i = 0; i < vc; i++) {
      if (vvs != null && Float.isNaN(val = vvs[i]))
        continue;
      if (mat4 != null)
        mat4.rotTrans(altVertices[i]);
      P3 pt = (P3) altVertices[i];
      if (normal != null && val != 0)
        pt.scaleAdd2(val, normal, pt);
    }
    
    initialize(lighting, altVertices, null);
    recalcAltVertices = false;
    return (P3[]) altVertices;
  }

  /**
   * 
   * @param showWithinPoints
   * @param showWithinDistance2
   * @param isWithinNot
   */
  public void setShowWithin(Lst<P3> showWithinPoints,
                            float showWithinDistance2, boolean isWithinNot) {
    if (showWithinPoints.size() == 0) {
      bsDisplay = (isWithinNot ? BSUtil.newBitSet2(0, vc) : null);
      return;
    }
    bsDisplay = new BS();
    for (int i = 0; i < vc; i++)
      if (checkWithin(vs[i], showWithinPoints, showWithinDistance2, isWithinNot))
        bsDisplay.set(i);
  }

  public static boolean checkWithin(T3 pti, Lst<P3> withinPoints,
                                    float withinDistance2, boolean isWithinNot) {
    if (withinPoints.size() != 0)
      for (int i = withinPoints.size(); --i >= 0;)
        if (pti.distanceSquared(withinPoints.get(i)) <= withinDistance2)
          return !isWithinNot;
    return isWithinNot;
  }

  public int getVertexIndexFromNumber(int vertexIndex) {
    if (--vertexIndex < 0)
      vertexIndex = vc + vertexIndex;
    return (vc <= vertexIndex ? vc - 1
        : vertexIndex < 0 ? 0 : vertexIndex);
  }

  public BS getVisibleVertexBitSet() {
    return getVisibleVBS();
  }

  protected BS getVisibleVBS() {
    BS bs = new BS();
    if (pc == 0 && bsSlabDisplay != null)
      BSUtil.copy2(bsSlabDisplay, bs);
    else
      for (int i = pc; --i >= 0;)
        if (bsSlabDisplay == null || bsSlabDisplay.get(i)) {
          int[] vertexIndexes = pis[i];
          if (vertexIndexes == null)
            continue;
          bs.set(vertexIndexes[0]);
          bs.set(vertexIndexes[1]);
          bs.set(vertexIndexes[2]);
        }
    return bs;
  }
//
//  BS getVisibleGhostBitSet() {
//    BS bs = new BS();
//    if (polygonCount == 0 && bsSlabGhost != null)
//      BSUtil.copy2(bsSlabGhost, bs);
//    else
//      for (int i = polygonCount; --i >= 0;)
//        if (bsSlabGhost == null || bsSlabGhost.get(i)) {
//          int[] vertexIndexes = polygonIndexes[i];
//          if (vertexIndexes == null)
//            continue;
//          bs.set(vertexIndexes[0]);
//          bs.set(vertexIndexes[1]);
//          bs.set(vertexIndexes[2]);
//        }
//    return bs;
//  }

  public void setTokenProperty(int tokProp, boolean bProp) {
    switch (tokProp) {
    case T.notfrontonly:
    case T.frontonly:
      frontOnly = (tokProp == T.frontonly ? bProp : !bProp);
      return;
    case T.frontlit:
    case T.backlit:
    case T.fullylit:
      setLighting(tokProp);
      return;
    case T.nodots:
    case T.dots:
      showPoints =  (tokProp == T.dots ? bProp : !bProp);
      return;
    case T.nomesh:
    case T.mesh:
      drawTriangles =  (tokProp == T.mesh ? bProp : !bProp);
      return;
    case T.nofill:
    case T.fill:
      fillTriangles =  (tokProp == T.fill ? bProp : !bProp);
      return;
    case T.notriangles:
    case T.triangles:
      showTriangles =  (tokProp == T.triangles ? bProp : !bProp);
      return;
    case T.nocontourlines:
    case T.contourlines:
      showContourLines =  (tokProp == T.contourlines ? bProp : !bProp);
      return;
    }
  }
  
  Object getInfo(boolean isAll) {
    Hashtable<String, Object> info = new Hashtable<String, Object>();
    info.put("id", thisID);
    info.put("vertexCount", Integer.valueOf(vc));
    info.put("polygonCount", Integer.valueOf(pc));
    info.put("haveQuads", Boolean.valueOf(haveQuads));
    info.put("haveValues", Boolean.valueOf(vvs != null));
    if (isAll) {
      if (vc > 0) {
        info.put("vertices", AU.arrayCopyPt(vs, vc));
        if (bsSlabDisplay != null)
          info.put("bsVertices", getVisibleVBS());
      }
      if (vvs != null)
        info.put("vertexValues", AU.arrayCopyF(vvs, vc));
      if (pc > 0) {
        info.put("polygons", AU.arrayCopyII(pis, pc));
        if (bsSlabDisplay != null)
          info.put("bsPolygons", bsSlabDisplay);
      }
    }
    return info;
  }

  public P3[] getBoundingBox() {
    return null;
  }

  /**
   * @return unitcell
   */
  public SymmetryInterface getUnitCell() {
    // isosurfaceMesh only
    return null;
  }

  public void rotateTranslate(Quat q, T3 offset, boolean isAbsolute) {
    if (q == null && offset == null) {
      mat4 = null;
      return;
    }
    M3 m3 = new M3();
    V3 v = new V3();
    if (mat4 == null)
      mat4 = M4.newM4(null);
    mat4.getRotationScale(m3);
    mat4.getTranslation(v);
    if (q == null) {
      if (isAbsolute)
        v.setT(offset);
      else
        v.add(offset);
    } else {
      m3.mul(q.getMatrix());
    }
    mat4 = M4.newMV(m3, v);
    recalcAltVertices = true;
  }

  public V3[] getNormalsTemp() {
    return (normalsTemp == null ? (normalsTemp = getNormals(vs, null))
        : normalsTemp);
  }

}
