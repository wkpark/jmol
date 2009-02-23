/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-25 11:08:02 -0500 (Wed, 25 Apr 2007) $
 * $Revision: 7492 $
 *
 * Copyright (C) 2005 Miguel, Jmol Development
 *
 * Contact: miguel@jmol.org,jmol-developers@lists.sourceforge.net
 * Contact: hansonr@stolaf.edu
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

/*
 * miguel 2005 07 17
 *
 *  System and method for the display of surface structures
 *  contained within the interior region of a solid body
 * United States Patent Number 4,710,876
 * Granted: Dec 1, 1987
 * Inventors:  Cline; Harvey E. (Schenectady, NY);
 *             Lorensen; William E. (Ballston Lake, NY)
 * Assignee: General Electric Company (Schenectady, NY)
 * Appl. No.: 741390
 * Filed: June 5, 1985
 *
 *
 * Patents issuing prior to June 8, 1995 can last up to 17
 * years from the date of issuance.
 *
 * Dec 1 1987 + 17 yrs = Dec 1 2004
 */

/*
 * Bob Hanson May 22, 2006
 * 
 * implementing marching squares; see 
 * http://www.secam.ex.ac.uk/teaching/ug/studyres/COM3404/COM3404-2006-Lecture15.pdf
 *  
 * inventing "Jmol Voxel File" format, *.jvxl
 * 
 * see http://www.stolaf.edu/academics/chemapps/jmol/docs/misc/JVXL-format.pdf
 * 
 * lines through coordinates are identical to CUBE files
 * after that, we have a line that starts with a negative number to indicate this
 * is a JVXL file:
 * 
 * line1:  (int)-nSurfaces  (int)edgeFractionBase (int)edgeFractionRange  
 * (nSurface lines): (float)cutoff (int)nBytesData (int)nBytesFractions
 * 
 * definition1
 * edgedata1
 * fractions1
 * colordata1
 * ....
 * definition2
 * edgedata2
 * fractions2
 * colordata2
 * ....
 * 
 * definitions: a line with detail about what sort of compression follows
 * 
 * edgedata: a list of the count of vertices ouside and inside the cutoff, whatever
 * that may be, ordered by nested for loops for(x){for(y){for(z)}}}.
 * 
 * nOutside nInside nOutside nInside...
 * 
 * fractions: an ascii list of characters represting the fraction of distance each
 * encountered surface point is along each voxel cube edge found to straddle the 
 * surface. The order written is dictated by the reader algorithm and is not trivial
 * to describe. Each ascii character is constructed by taking a base character and 
 * adding onto it the fraction times a range. This gives a character that can be
 * quoted EXCEPT for backslash, which MAY be substituted for by '!'. Jmol uses the 
 * range # - | (35 - 124), reserving ! and } for special meanings.
 * 
 * colordata: same deal here, but with possibility of "double precision" using two bytes.
 * 
 */

package org.jmol.shapesurface;

import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.ColorEncoder;
import org.jmol.util.ArrayUtil;
import org.jmol.util.Parser;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Token;
import org.jmol.viewer.Viewer;
import org.jmol.jvxl.readers.JvxlReader;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.jvxl.api.MeshDataServer;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.MeshData;
import org.jmol.jvxl.readers.SurfaceGenerator;

public class Isosurface extends MeshCollection implements MeshDataServer {

  private IsosurfaceMesh[] isomeshes = new IsosurfaceMesh[4];
  private IsosurfaceMesh thisMesh;

  public void allocMesh(String thisID) {
    meshes = isomeshes = (IsosurfaceMesh[]) ArrayUtil.ensureLength(isomeshes,
        meshCount + 1);
    currentMesh = thisMesh = isomeshes[meshCount++] = new IsosurfaceMesh(
        thisID, g3d, colix);
      sg.setJvxlData(jvxlData = thisMesh.jvxlData);
    //System.out.println("Isosurface allocMesh thisMesh:" + thisMesh.thisID + " " + thisMesh);
  }

  public void initShape() {
    super.initShape();
    myType = "isosurface";
    newSg();
  }

  private void newSg() {
    sg = new SurfaceGenerator(viewer, this, colorEncoder, null, jvxlData = new JvxlData());
    sg.setVersion("Jmol " + Viewer.getJmolVersion());
  }
  
  protected void clearSg() {
    sg = null; // not Molecular Orbitals
  }
  //private boolean logMessages;
  private int lighting;
  private boolean iHaveBitSets;
  private boolean explicitContours;
  private int atomIndex;
  private int moNumber;
  private short defaultColix;
  private Point3f center;
  private boolean isPhaseColored;

  protected SurfaceGenerator sg;
  private JvxlData jvxlData;

  private ColorEncoder colorEncoder = new ColorEncoder();

  public void setProperty(String propertyName, Object value, BitSet bs) {

    if (Logger.debugging) {
      Logger.debug("Isosurface setProperty: "
          + propertyName + " = " + value);
    }

    ////isosurface-only (no calculation required; no calculation parameters to set)

    if ("delete" == propertyName) {
      setPropertySuper(propertyName, value, bs);
      if (!explicitID)
        nLCAO = nUnnamed = 0;
      return;
    }

    if ("remapcolor" == propertyName) {
      if (thisMesh != null)
        remapColors();
      return;
    }

    if ("thisID" == propertyName) {
      setPropertySuper("thisID", value, null);
      return;
    }

    if ("map" == propertyName) {
      setProperty("squareData", Boolean.FALSE, null);
      return;
    }

    if ("color" == propertyName) {
      if (thisMesh != null) {
        //thisMesh.vertexColixes = null;
        thisMesh.isColorSolid = true;
      } else if (!TextFormat.isWild(previousMeshID)){
        for (int i = meshCount; --i >= 0;) {
          //isomeshes[i].vertexColixes = null;
          isomeshes[i].isColorSolid = true;
        }
      }
      setPropertySuper(propertyName, value, bs);
      return;
    }

    if ("contour" == propertyName) {
      explicitContours = true;  
    }
    
    if ("fixed" == propertyName) {
      isFixed = ((Boolean) value).booleanValue();
      setModelIndex();
      return;
    }

    if ("modelIndex" == propertyName) {
      if (!iHaveModelIndex) {
        modelIndex = ((Integer) value).intValue();
        sg.setModelIndex(modelIndex);
        isFixed = (modelIndex < 0);
      }
      isFixed = (modelIndex < 0);
      return;
    }

    if ("lcaoCartoon" == propertyName) {
      // z x center rotationAxis (only one of x, y, or z is nonzero; in radians) 
      Vector3f[] info = (Vector3f[]) value;
      if (!explicitID) {
        setPropertySuper("thisID", null, null);
      }
      //center (info[2]) is set in SurfaceGenerator
      if (sg.setParameter("lcaoCartoonCenter", info[2]))
        return;
      drawLcaoCartoon(info[0], info[1], info[3]);
      return;
    }

    // isosurface FIRST, but also need to set some parameters

    if ("title" == propertyName) {
      if (value instanceof String && "-".equals((String) value))
        value = null;
      setPropertySuper(propertyName, value, bs);
      sg.setParameter("title", title, bs);
      return;
    }

    if ("select" == propertyName) {
      if (iHaveBitSets)
        return;
    }

    if ("ignore" == propertyName) {
      if (iHaveBitSets)
        return;
    }

    if ("atomIndex" == propertyName) {
      atomIndex = ((Integer) value).intValue();
    }

    if ("pocket" == propertyName) {
      Boolean pocket = (Boolean) value;
      lighting = (pocket.booleanValue() ? JmolConstants.FULLYLIT
          : JmolConstants.FRONTLIT);
    }

    if ("colorRGB" == propertyName) {
      int rgb = ((Integer) value).intValue();
      defaultColix = Graphics3D.getColix(rgb);
    }

    if ("molecularOrbital" == propertyName) {
      moNumber = ((Integer) value).intValue();
    }

    if (propertyName == "functionXY") {
      if (sg.isStateDataRead())
        setScriptInfo(); // for script DATA1
    }

    if ("center" == propertyName) {
      center.set((Point3f) value);
    }

    if ("phase" == propertyName) {
      isPhaseColored = true;
    }

    if ("plane" == propertyName) {
      allowContourLines = false;
    }

    if ("functionXY" == propertyName) {
      allowContourLines = false;
    }

    if ("finalize" == propertyName) {
      setScriptInfo();
      setJvxlInfo();
      clearSg();
      return;
    }
    
    if ("init" == propertyName) {
      newSg();
    }
    
    //surface generator only (return TRUE) or shared (return FALSE)

    if (sg != null && sg.setParameter(propertyName, value, bs))
      return;

    /////////////// isosurface LAST, shared

    if ("init" == propertyName) {
      setPropertySuper("thisID", JmolConstants.PREVIOUS_MESH_ID, null);
      if (!(iHaveBitSets = getScriptBitSets((String) value, null))) {
        sg.setParameter("select", bs);
      }
      initializeIsosurface();
      sg.setModelIndex(modelIndex);
      //setModelIndex();
      return;
    }

    if ("clear" == propertyName) {
      discardTempData(true);
      return;
    }

    /*
     if ("background" == propertyName) {
     boolean doHide = !((Boolean) value).booleanValue();
     if (thisMesh != null)
     thisMesh.hideBackground = doHide;
     else {
     for (int i = meshCount; --i >= 0;)
     meshes[i].hideBackground = doHide;
     }
     return;
     }

     */

    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      BitSet bsModels = new BitSet();
      bsModels.set(modelIndex);
      int firstAtomDeleted = ((int[])((Object[])value)[2])[1];
      int nAtomsDeleted = ((int[])((Object[])value)[2])[2];
      for (int i = meshCount; --i >= 0;) {
        Mesh m = meshes[i];
        if (m == null)
          continue;
        if (m.modelIndex == modelIndex) {
           meshCount--;
            if (m == currentMesh) 
              currentMesh = thisMesh = null;
            meshes = isomeshes = (IsosurfaceMesh[]) ArrayUtil.deleteElements(
                meshes, i, 1);
        } else if (m.modelIndex > modelIndex) {
          m.modelIndex--;
          if (m.atomIndex >= firstAtomDeleted)
            m.atomIndex -= nAtomsDeleted;
          if (m.bitsets != null) {
            BitSetUtil.deleteBits(m.bitsets[0], bs);
            BitSetUtil.deleteBits(m.bitsets[1], bs);
            BitSetUtil.deleteBits(m.bitsets[2], bsModels);
          }
        }
      }
      return;
    }
    
    // processed by meshCollection

    setPropertySuper(propertyName, value, bs);
  }  

  private void setPropertySuper(String propertyName, Object value, BitSet bs) {
    //System.out.println(propertyName + " " + value);
    //System.out.println(thisMesh + (thisMesh!= null ? thisMesh.thisID : ""));
    currentMesh = thisMesh;
    super.setProperty(propertyName, value, bs);
    thisMesh = (IsosurfaceMesh) currentMesh;
    jvxlData = (thisMesh == null ? null : thisMesh.jvxlData);
    if (sg != null)
      sg.setJvxlData(jvxlData);
  }

  public Object getProperty(String property, int index) {
    Object ret = super.getProperty(property, index);
    if (ret != null)
      return ret;
    if (property == "dataRange")
      return (thisMesh == null ? null : new float[] {
          jvxlData.mappedDataMin, jvxlData.mappedDataMax,
          jvxlData.valueMappedToRed,
          jvxlData.valueMappedToBlue });
    if (property == "moNumber")
      return new Integer(moNumber);
    if (property == "area")
      return new Float((thisMesh == null ? Float.NaN : thisMesh.calculateArea()));
    if (property == "volume")
      return new Float((thisMesh == null ? Float.NaN : thisMesh.calculateVolume()));
    if (thisMesh == null)
      return "no current isosurface";
    if (property == "cutoff")
      return new Float(jvxlData.cutoff);
    if (property == "plane")
      return jvxlData.jvxlPlane;
    if (property == "jvxlFileData") {
      MeshData meshData = null;
      if (jvxlData.vertexDataOnly) {
        meshData = new MeshData();
        fillMeshData(meshData, MeshData.MODE_GET_VERTICES);
      }
      return JvxlReader.jvxlGetFile(jvxlData, meshData, title, "", true, index, thisMesh
              .getState(myType), (thisMesh.scriptCommand == null ? "" : thisMesh.scriptCommand));
    }
    if (property == "jvxlSurfaceData") // MO only
      return JvxlReader.jvxlGetFile(jvxlData, null, title, "", false, 1, thisMesh
              .getState(myType), (thisMesh.scriptCommand == null ? "" : thisMesh.scriptCommand));
    if (property == "jvxlFileInfo")
      return jvxlData.jvxlInfoLine;
    return super.getProperty(property, index);
  }

  private boolean getScriptBitSets(String script, BitSet[] bsCmd) {
    this.script = script;
    getModelIndex(script);
    if (script == null)
      return false;
    int i = script.indexOf("# ({");
    if (i < 0)
      return false;
    int j = script.indexOf("})", i);
    if (j < 0)
      return false;
    BitSet bs = Escape.unescapeBitset(script.substring(i + 3, j + 1));
    if (bsCmd == null)
      sg.setParameter("select", bs);
    else
      bsCmd[0] = bs;
    if ((i = script.indexOf("({", j)) < 0)
      return true;
    j = script.indexOf("})", i);
    if (j < 0) 
      return false;
      bs = Escape.unescapeBitset(script.substring(i + 1, j + 1));
      if (bsCmd == null)
        sg.setParameter("ignore", bs);
      else
        bsCmd[1] = bs;
    if ((i = script.indexOf("/({", j)) == j + 2) {
      if ((j = script.indexOf("})", i)) < 0)
        return false;
      bs = Escape.unescapeBitset(script.substring(i + 3, j + 1));
      if (bsCmd == null)
        viewer.setTrajectory(bs);
      else
        bsCmd[2] = bs;
    }
    return true;
  }

  private void initializeIsosurface() {
    lighting = JmolConstants.FRONTLIT;
    if (!iHaveModelIndex)
      modelIndex = viewer.getCurrentModelIndex();
    isFixed = (modelIndex < 0);
    if (modelIndex < 0)
      modelIndex = 0;
    title = null;
    explicitContours = false;
    atomIndex = -1;
    colix = Graphics3D.ORANGE;
    defaultColix = 0;
    isPhaseColored = false;
    allowContourLines = true; //but not for f(x,y) or plane, which use mesh
    center = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    linkedMesh = null;
    initState();
  }

  private void initState() {
    associateNormals = true;
    sg.initState();
    //TODO   need to pass assocCutoff to sg
  }

  /*
   void checkFlags() {
   if (viewer.getTestFlag2())
   associateNormals = false;
   if (!logMessages)
   return;
   Logger.info("Isosurface using testflag2: no associative grouping = "
   + !associateNormals);
   Logger.info("IsosurfaceRenderer using testflag4: show vertex normals = "
   + viewer.getTestFlag4());
   Logger
   .info("For grid points, use: isosurface delete myiso gridpoints \"\"");
   }
   */

  private void discardTempData(boolean discardAll) {
    if (!discardAll)
      return;
    title = null;
    if (thisMesh == null)
      return;
    thisMesh.surfaceSet = null;
  }

  ////////////////////////////////////////////////////////////////
  // default color stuff (deprecated in 11.2)
  ////////////////////////////////////////////////////////////////

  private int indexColorPositive;
  private int indexColorNegative;

  private short getDefaultColix() {
    if (defaultColix != 0)
      return defaultColix;
    if (!sg.isCubeData())
      return colix; // orange
    int argb;
    if (sg.getCutoff() >= 0) {
      indexColorPositive = (indexColorPositive % JmolConstants.argbsIsosurfacePositive.length);
      argb = JmolConstants.argbsIsosurfacePositive[indexColorPositive++];
    } else {
      indexColorNegative = (indexColorNegative % JmolConstants.argbsIsosurfaceNegative.length);
      argb = JmolConstants.argbsIsosurfaceNegative[indexColorNegative++];
    }
    return Graphics3D.getColix(argb);
  }

  ///////////////////////////////////////////////////
  ////  LCAO Cartoons  are sets of lobes ////

  private int nLCAO = 0;

  private void drawLcaoCartoon(Vector3f z, Vector3f x, Vector3f rotAxis) {
    String lcaoCartoon = sg.setLcao();
    //really rotRadians is just one of these -- x, y, or z -- not all
    float rotRadians = rotAxis.x + rotAxis.y + rotAxis.z;
    defaultColix = Graphics3D.getColix(sg.getColor(1));
    int colorNeg = sg.getColor(-1);
    Vector3f y = new Vector3f();
    boolean isReverse = (lcaoCartoon.length() > 0 && lcaoCartoon.charAt(0) == '-');
    if (isReverse)
      lcaoCartoon = lcaoCartoon.substring(1);
    int sense = (isReverse ? -1 : 1);
    y.cross(z, x);
    if (rotRadians != 0) {
      AxisAngle4f a = new AxisAngle4f();
      if (rotAxis.x != 0)
        a.set(x, rotRadians);
      else if (rotAxis.y != 0)
        a.set(y, rotRadians);
      else
        a.set(z, rotRadians);
      Matrix3f m = new Matrix3f();
      m.set(a);
      m.transform(x);
      m.transform(y);
      m.transform(z);
    }
    if (thisMesh == null && nLCAO == 0)
      nLCAO = meshCount;
    String id = (thisMesh == null ? "lcao" + (++nLCAO) + "_" + lcaoCartoon
        : thisMesh.thisID);
    if (thisMesh == null)
      allocMesh(id);

    if (lcaoCartoon.equals("px")) {
      thisMesh.thisID += "a";
      Mesh meshA = thisMesh;
      createLcaoLobe(x, sense);
      setProperty("thisID", id + "b", null);
      createLcaoLobe(x, -sense);
      thisMesh.colix = Graphics3D.getColix(colorNeg);
      linkedMesh = thisMesh.linkedMesh = meshA;
      return;
    }
    if (lcaoCartoon.equals("py")) {
      thisMesh.thisID += "a";
      Mesh meshA = thisMesh;
      createLcaoLobe(y, sense);
      setProperty("thisID", id + "b", null);
      createLcaoLobe(y, -sense);
      thisMesh.colix = Graphics3D.getColix(colorNeg);
      linkedMesh = thisMesh.linkedMesh = meshA;
      return;
    }
    if (lcaoCartoon.equals("pz")) {
      thisMesh.thisID += "a";
      Mesh meshA = thisMesh;
      createLcaoLobe(z, sense);
      setProperty("thisID", id + "b", null);
      createLcaoLobe(z, -sense);
      thisMesh.colix = Graphics3D.getColix(colorNeg);
      linkedMesh = thisMesh.linkedMesh = meshA;
      return;
    }
    if (lcaoCartoon.equals("pxa")) {
      createLcaoLobe(x, sense);
      return;
    }
    if (lcaoCartoon.equals("pxb")) {
      createLcaoLobe(x, -sense);
      return;
    }
    if (lcaoCartoon.equals("pya")) {
      createLcaoLobe(y, sense);
      return;
    }
    if (lcaoCartoon.equals("pyb")) {
      createLcaoLobe(y, -sense);
      return;
    }
    if (lcaoCartoon.equals("pza")) {
      createLcaoLobe(z, sense);
      return;
    }
    if (lcaoCartoon.equals("pzb")) {
      createLcaoLobe(z, -sense);
      return;
    }
    if (lcaoCartoon.indexOf("sp") == 0 || lcaoCartoon.indexOf("lp") == 0) {
      createLcaoLobe(z, sense);
      return;
    }

    // assume s
    createLcaoLobe(null, 1);
    return;
  }

  private Point4f lcaoDir = new Point4f();

  private void createLcaoLobe(Vector3f lobeAxis, float factor) {
    initState();
    if (Logger.debugging) {
      Logger.debug("creating isosurface ID " + thisMesh.thisID);
    }
    thisMesh.colix = defaultColix;
    if (lobeAxis == null) {
      setProperty("sphere", new Float(factor / 2f), null);
    } else {
      lcaoDir.x = lobeAxis.x * factor;
      lcaoDir.y = lobeAxis.y * factor;
      lcaoDir.z = lobeAxis.z * factor;
      lcaoDir.w = 0.7f;
      setProperty("lobe", lcaoDir, null);
    }
    setScriptInfo();
  }

  /////////////// meshDataServer interface /////////////////

  public void invalidateTriangles() {
    thisMesh.invalidateTriangles();
  }

  public void fillMeshData(MeshData meshData, int mode) {
    if (meshData == null) {
      if (thisMesh == null)
        allocMesh(null);
      thisMesh.clear("isosurface", sg.getIAddGridPoints());
      thisMesh.colix = getDefaultColix();
      if (isPhaseColored || thisMesh.jvxlData.isBicolorMap)
        thisMesh.isColorSolid = false;
      return;
    }
    switch (mode) {

    case MeshData.MODE_GET_VERTICES:
      meshData.vertices = thisMesh.vertices;
      meshData.vertexValues = thisMesh.vertexValues;
      meshData.vertexCount = thisMesh.vertexCount;
      meshData.vertexIncrement = thisMesh.vertexIncrement;
      meshData.polygonCount = thisMesh.polygonCount;
      meshData.polygonIndexes = thisMesh.polygonIndexes;
      return;
    case MeshData.MODE_GET_COLOR_INDEXES:
      if (thisMesh.vertexColixes == null
          || thisMesh.vertexCount > thisMesh.vertexColixes.length)
        thisMesh.vertexColixes = new short[thisMesh.vertexCount];
      meshData.vertexColixes = thisMesh.vertexColixes;
      return;
    case MeshData.MODE_PUT_SETS:
      thisMesh.surfaceSet = meshData.surfaceSet;
      thisMesh.vertexSets = meshData.vertexSets;
      thisMesh.nSets = meshData.nSets;
      //thisMesh.setColorSchemeSets();
      return;
    case MeshData.MODE_PUT_VERTICES:
      thisMesh.vertices = meshData.vertices;
      thisMesh.vertexValues = meshData.vertexValues;
      thisMesh.vertexCount = meshData.vertexCount;
      thisMesh.vertexIncrement = meshData.vertexIncrement;
      thisMesh.polygonCount = meshData.polygonCount;
      thisMesh.polygonIndexes = meshData.polygonIndexes;
      return;
    }
  }

  public void notifySurfaceGenerationCompleted() {
    setModelIndex();
    thisMesh.insideOut = sg.isInsideOut();
    thisMesh.calculatedArea = thisMesh.calculatedVolume = Float.NaN;
    thisMesh.initialize(sg.getPlane() != null ? JmolConstants.FULLYLIT
        : lighting);
    if (thisMesh.jvxlData.jvxlPlane != null)
      allowContourLines = false;
  }

  public void notifySurfaceMappingCompleted() {
    setModelIndex();
    String schemeName = colorEncoder.getColorSchemeName();
    viewer.setPropertyColorScheme(schemeName, false);
    viewer.setCurrentColorRange(jvxlData.valueMappedToRed,
        jvxlData.valueMappedToBlue);
    thisMesh.isColorSolid = false;
    thisMesh.getContours();
    if (thisMesh.jvxlData.jvxlPlane != null)
      allowContourLines = false;
    if (thisMesh.jvxlData.nContours != 0 && thisMesh.jvxlData.nContours != -1)
      explicitContours = true;
    setPropertySuper("token", new Integer(explicitContours ? Token.nofill : Token.fill), null);
    setPropertySuper("token", new Integer(explicitContours ? Token.contourlines : Token.nocontourlines), null);
    thisMesh.colorCommand = "color $" + thisMesh.thisID + " "
        + getUserColorScheme(schemeName) + " range "
        + (jvxlData.isColorReversed ? jvxlData.valueMappedToBlue + " "
            + jvxlData.valueMappedToRed : jvxlData.valueMappedToRed + " "
            + jvxlData.valueMappedToBlue);
    /*
     viewer.setCurrentColorRange(jvxlData.mappedDataMin, jvxlData.mappedDataMax);
     thisMesh.isColorSolid = false;
     thisMesh.colorCommand = "color $" + thisMesh.thisID + " " + getUserColorScheme(schemeName) + " range " 
     + (jvxlData.isColorReversed ? jvxlData.mappedDataMax + " " + jvxlData.mappedDataMin : 
     jvxlData.mappedDataMin + " " + jvxlData.mappedDataMax);

     */
  }

  public Point3f[] calculateGeodesicSurface(BitSet bsSelected,
                                            float envelopeRadius) {
    return viewer.calculateSurface(bsSelected, envelopeRadius);
  }

  /////////////  VertexDataServer interface methods ////////////////

  public int getSurfacePointIndexAndFraction(float cutoff, boolean isCutoffAbsolute,
                                  int x, int y, int z, Point3i offset, int vA,
                                  int vB, float valueA, float valueB,
                                  Point3f pointA, Vector3f edgeVector,
                                  boolean isContourType, float[] fReturn) {
    return 0;
  }

  private boolean associateNormals;

  public int addVertexCopy(Point3f vertexXYZ, float value, int assocVertex) {
    return thisMesh.addVertexCopy(vertexXYZ, value, assocVertex,
        associateNormals);
  }

  public void addTriangleCheck(int iA, int iB, int iC, int check,
                               boolean isAbsolute) {
    if (isAbsolute && !MeshData.checkCutoff(iA, iB, iC, thisMesh.vertexValues))
      return;
    thisMesh.addTriangleCheck(iA, iB, iC, check);
  }

  ////////////////////////////////////////////////////////////////////

  private void setModelIndex() {
    setModelIndex(atomIndex, modelIndex);
    thisMesh.ptCenter.set(center);
  }

  protected void setScriptInfo() {
    thisMesh.title = sg.getTitle();
    String script = sg.getScript();
    thisMesh.bitsets = null;
    if (script != null) {
      if (script.charAt(0) == ' ') { // lobe only
        script = myType + " ID " + Escape.escape(thisMesh.thisID) + script;
      } else if (sg.getIUseBitSets()) {
        thisMesh.bitsets = new BitSet[3];
        thisMesh.bitsets[0] = sg.getBsSelected();
        thisMesh.bitsets[1] = sg.getBsIgnore();
        thisMesh.bitsets[2] = viewer.getBitSetTrajectories();
      }
    }
    int pt;
    if (!explicitID && script != null && (pt = script.indexOf("# ID=")) >= 0)
      thisMesh.thisID = Parser.getNextQuotedString(script, pt);
    thisMesh.scriptCommand = script;
    Vector v = (Vector) sg.getFunctionXYinfo();
    if (thisMesh.data1 == null)
      thisMesh.data1 = v;
    else
      thisMesh.data2 = v;
  }

  private void setJvxlInfo() {
    if (sg.getJvxlData() != jvxlData || sg.getJvxlData() != thisMesh.jvxlData)
      jvxlData = thisMesh.jvxlData = sg.getJvxlData();
    jvxlData.jvxlDefinitionLine = JvxlReader.jvxlGetDefinitionLine(jvxlData,
        false);
    jvxlData.jvxlInfoLine = JvxlReader.jvxlGetDefinitionLine(jvxlData, true);
  }

  public Vector getShapeDetail() {
    Vector V = new Vector();
    for (int i = 0; i < meshCount; i++) {
      Hashtable info = new Hashtable();
      IsosurfaceMesh mesh = isomeshes[i];
      if (mesh == null)
        continue;
      info.put("ID", (mesh.thisID == null ? "<noid>" : mesh.thisID));
      info.put("vertexCount", new Integer(mesh.vertexCount));
      if (mesh.ptCenter.x != Float.MAX_VALUE)
        info.put("center", mesh.ptCenter);
      if (mesh.jvxlData.jvxlDefinitionLine != null)
        info.put("jvxlDefinitionLine", mesh.jvxlData.jvxlDefinitionLine);
      info.put("modelIndex", new Integer(mesh.modelIndex));
      if (mesh.title != null)
        info.put("title", mesh.title);
      V.addElement(info);
    }
    return V;
  }

  protected void remapColors() {
    JvxlData jvxlData = thisMesh.jvxlData;
    float[] vertexValues = thisMesh.vertexValues;
    short[] vertexColixes = thisMesh.vertexColixes;
    if (vertexValues == null || jvxlData.isBicolorMap
        || jvxlData.vertexCount == 0)
      return;
    if (vertexColixes == null)
      vertexColixes = thisMesh.vertexColixes = new short[thisMesh.vertexCount];
    boolean isTranslucent = Graphics3D.isColixTranslucent(thisMesh.colix);
    for (int i = thisMesh.vertexCount; --i >= 0;) {
      vertexColixes[i] = viewer.getColixForPropertyValue(vertexValues[i]);
      if (isTranslucent)
        vertexColixes[i] = Graphics3D.getColixTranslucent(vertexColixes[i], true, translucentLevel);
    }
    Vector[] contours = thisMesh.getContours();
    if (contours != null) {
      for (int i = contours.length; --i >= 0; ) {
        float value = ((Float)contours[i].get(IsosurfaceMesh.CONTOUR_VALUE)).floatValue();
        int[] color = ((int[])contours[i].get(IsosurfaceMesh.CONTOUR_COLOR));
        color[0] = viewer.getColixArgb(viewer.getColixForPropertyValue(value));
      }
    }
    float[] range = viewer.getCurrentColorRange();
    jvxlData.valueMappedToRed = Math.min(range[0], range[1]);
    jvxlData.valueMappedToBlue = Math.max(range[0], range[1]);
    jvxlData.isJvxlPrecisionColor = true;
    JvxlReader.jvxlCreateColorData(jvxlData, vertexValues);
    String schemeName = viewer.getPropertyColorScheme();
    thisMesh.colorCommand = "color $" + thisMesh.thisID + " "
        + getUserColorScheme(schemeName) + " range " + range[0] + " "
        + range[1];
    thisMesh.isColorSolid = false;
  }

  private String getUserColorScheme(String schemeName) {
    String colors = viewer.getColorSchemeList(schemeName, false);
    return "\"" + (colors.length() == 0 ? schemeName : colors) + "\"";
  }
  
  public float getValue(int x, int y, int z) {
    return 0;
  }
  
  public boolean checkObjectHovered(int x, int y, BitSet bsVisible) {
    String s = findValue(x, y, false, bsVisible);
    if (s == null)
      return false;
    if (g3d.isDisplayAntialiased()) {
      //because hover rendering is done in FIRST pass only
      x <<= 1;
      y <<= 1;
    }      
    viewer.hoverOn(x, y, s);
    return true;
  }

  private final static int MAX_OBJECT_CLICK_DISTANCE_SQUARED = 10 * 10;

  private String findValue(int x, int y, boolean isPicking,
                                   BitSet bsVisible) {
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
      dmin2 <<= 1;
    }
    Vector pickedContour = null;
    for (int i = 0; i < meshCount; i++) {
      IsosurfaceMesh m = isomeshes[i];
      if (m.visibilityFlags == 0 || m.modelIndex >= 0
          && !bsVisible.get(m.modelIndex))
        continue;
      Vector[] vs = m.jvxlData.vContours;
      if (vs != null) {
      for (int j = 0; j < vs.length; j++) {
        Vector vc = vs[j];
        int n = vc.size() - 1;
        for (int k = IsosurfaceMesh.CONTOUR_POINTS; k < n; k++) {
          Point3f v = (Point3f) vc.get(k);
          int d2 = coordinateInRange(x, y, v, dmin2);
          if (d2 >= 0) {
            dmin2 = d2;
            pickedContour = vc;
          }
        }
      }
      if (pickedContour != null)
        return pickedContour.get(IsosurfaceMesh.CONTOUR_VALUE).toString();
      } else if (m.jvxlData.jvxlPlane != null && m.vertexValues != null) {
        int pickedVertex = -1;
        for (int k = m.vertexCount; --k >= m.firstRealVertex; ) {
          Point3f v = m.vertices[k];
          int d2 = coordinateInRange(x, y, v, dmin2);
          if (d2 >= 0) {
            dmin2 = d2;
            pickedVertex = k;
          }
        }
        if (pickedVertex != -1)
          return "v" + pickedVertex + ": " + m.vertexValues[pickedVertex];
      }
    }
    return null;
  }
  private final Point3i ptXY = new Point3i();

  private int coordinateInRange(int x, int y, Point3f vertex, int dmin2) {
    int d2 = dmin2;
    viewer.transformPoint(vertex, ptXY);
    d2 = (x - ptXY.x) * (x - ptXY.x) + (y - ptXY.y) * (y - ptXY.y);
    return (d2 < dmin2 ? d2 : -1);
  }
  


}
