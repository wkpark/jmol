/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.viewer;
import org.jmol.util.Logger;
import org.jmol.util.ColorEncoder;
import org.jmol.util.ArrayUtil;
import org.jmol.jvxl.readers.JvxlReader;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.MeshData;
import org.jmol.jvxl.api.MeshDataServer;
import org.jmol.jvxl.readers.SurfaceGenerator;




class Isosurface extends MeshFileCollection implements MeshDataServer {

  IsosurfaceMesh[] isomeshes = new IsosurfaceMesh[4];
  IsosurfaceMesh thisMesh;
  boolean logMessages;
  
  void allocMesh(String thisID) {
    meshes = isomeshes = (IsosurfaceMesh[])ArrayUtil.ensureLength(isomeshes, meshCount + 1);
    currentMesh = thisMesh = isomeshes[meshCount++] = new IsosurfaceMesh(thisID, g3d, colix);
    sg.setJvxlData(jvxlData = thisMesh.jvxlData);
    System.out.println("Isosurface allocMesh thisMesh:" + thisMesh.vertexColixes);

  }

  void initShape() {
    super.initShape();
    myType = "isosurface";
    jvxlData = new JvxlData();
    sg = new SurfaceGenerator(viewer, this, colorEncoder, null, jvxlData);
  }

  int lighting;
  private BitSet bsSelected;
  private BitSet bsIgnore;
  private boolean iHaveBitSets;
  private int modelIndex;
  private int atomIndex;
  private int moNumber;
  private short defaultColix;
  private Point3f center;
 
  protected SurfaceGenerator sg;
  private JvxlData jvxlData;

  private ColorEncoder colorEncoder = new ColorEncoder();
  
  void setProperty(String propertyName, Object value, BitSet bs) {

    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("Isosurface state=" + sg.getState() + " setProperty: "
          + propertyName + " = " + value);
    }

    ////isosurface-only (no calculation required; no calculation parameters to set)

    if ("delete" == propertyName) {
      setPropertySuper(propertyName, value, bs);
      if (!explicitID)
        nLCAO = nUnnamed = 0;
      return;
    }

    if ("thisID" == propertyName) {
      setPropertySuper("thisID", value, null);
      return;
    }

    if ("map" == propertyName) {
      return;
    }

    if ("color" == propertyName) {
      if (thisMesh != null) {
        thisMesh.vertexColixes = null;
        thisMesh.isColorSolid = true;
      } else {
        for (int i = meshCount; --i >= 0;) {
          isomeshes[i].vertexColixes = null;
          isomeshes[i].isColorSolid = true;
        }
      }
      setPropertySuper(propertyName, value, bs);
      return;
    }

    if ("fixed" == propertyName) {
      isFixed = ((Boolean) value).booleanValue();
      setModelIndex();
      return;
    }

    if ("modelIndex" == propertyName) {
      modelIndex = ((Integer) value).intValue();
      return;
    }

    if ("lcaoCartoon" == propertyName) {
      Vector3f[] info = (Vector3f[]) value;
      if (!explicitID)
        setPropertySuper("thisID", null, null);
      if (sg.setParameter("lcaoCartoon", value))
        return;
      drawLcaoCartoon(info[0], info[1]);
      return;
    }

    // isosurface FIRST, but also need to set some parameters

    if ("debug" == propertyName) {
      logMessages = ((Boolean) value).booleanValue();
    }
    
    if ("title" == propertyName) {
      setPropertySuper(propertyName, value, bs);
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
      lighting = (pocket.booleanValue() ? Mesh.FULLYLIT : Mesh.FRONTLIT);
    }
    
    if ("colorRGB" == propertyName) {
      int rgb = ((Integer) value).intValue();
      defaultColix = Graphics3D.getColix(rgb);
    }

    if ("molecularOrbital" == propertyName) {
      moNumber = ((Integer) value).intValue();
    }

    if ("center" == propertyName) {
      center.set((Point3f) value);
    }
    
    //surface generator only (return TRUE) or shared (return FALSE)

    if (sg.setParameter(propertyName, value, bs))
      return;

    /////////////// isosurface LAST, shared

    if ("init" == propertyName) {
      setPropertySuper("thisID", Mesh.PREVIOUS_MESH_ID, null);
      if (!(iHaveBitSets = getScriptBitSets())) {
        sg.setParameter("select", bs);
        bsSelected = bs; //THIS MAY BE NULL
      } else {
        sg.setParameter("select", bsSelected);
        sg.setParameter("ignore", bsIgnore);
      }
      initializeIsosurface();
      sg.setModelIndex(modelIndex);
      setModelIndex();
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

    // processed by meshCollection

    setPropertySuper(propertyName, value, bs);
  }

  void setPropertySuper(String propertyName, Object value, BitSet bs) {
    currentMesh = thisMesh;
    super.setProperty(propertyName, value, bs);
    thisMesh = (IsosurfaceMesh)currentMesh;
    jvxlData = (thisMesh == null ? null : thisMesh.jvxlData);
  }
  
  Object getProperty(String property, int index) {
    if (property == "list")
      return super.getProperty(property, index);
    if (property == "moNumber")
      return new Integer(moNumber);
    if (thisMesh == null)
      return "no current isosurface";
    if (property == "plane")
      return jvxlData.jvxlPlane;
    if (property == "jvxlFileData")
      return JvxlReader.jvxlGetFile(jvxlData, title, "", true, index, thisMesh.getState(myType), shortScript());
    if (property == "jvxlSurfaceData")
      return JvxlReader.jvxlGetFile(jvxlData, title, "", false, 1, thisMesh.getState(myType), shortScript());
    return super.getProperty(property, index);
  }

  String shortScript() {
    return (thisMesh.scriptCommand == null ? "" : thisMesh.scriptCommand.substring(0, (thisMesh.scriptCommand+";").indexOf(";")));
  }
  
  boolean getScriptBitSets() {
    if (script == null)
      return false;
    int i = script.indexOf("# ({");
    if (i < 0)
      return false;
    int j = script.indexOf("})", i);
    bsSelected = StateManager.unescapeBitset(script.substring(i + 3, j + 1));
    if ((i = script.indexOf("({", j)) < 0)
      return false;
    j = script.indexOf("})", i);
    bsIgnore = StateManager.unescapeBitset(script.substring(i + 1, j + 1));
    return true;
  }

  String fixScript() {
    if (script == null)
      return null;
    if (script.indexOf("# ({") >= 0)
      return script;
    if (script.charAt(0) == ' ')
      return myType + " " + thisMesh.thisID + script;
    if (!sg.getIUseBitSets())
      return script;
    return script + "# "
        + (bsSelected == null ? "({null})" : StateManager.escape(bsSelected))
        + " " + (bsIgnore == null ? "({null})" : StateManager.escape(bsIgnore));
  }

  void initializeIsosurface() {
    lighting = Mesh.FRONTLIT;
    modelIndex = viewer.getCurrentModelIndex();
    isFixed = (modelIndex < 0);
    if (modelIndex < 0)
      modelIndex = 0;
    title = null;
    atomIndex = -1;
    defaultColix = 0;
    bsIgnore = null;
    center = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    initState();
  }

  void initState() {
    associateNormals = true;
    sg.initState();
//TODO   need to pass assocCutoff to sg
  }

  void checkFlags() {
    //if (viewer.getTestFlag1()) // turn off new solvent method
      //newSolventMethod = false;
    if (viewer.getTestFlag2())
      associateNormals = false;
    
    if (logMessages) {
      Logger.debug("Isosurface using testflag2: no associative grouping = "
          + !associateNormals);
      Logger.debug("IsosurfaceRenderer using testflag3: separated triangles = "
          + viewer.getTestFlag3());
      Logger.debug("IsosurfaceRenderer using testflag4: show vertex normals = "
          + viewer.getTestFlag4());
      Logger
          .debug("For grid points, use: isosurface delete myiso gridpoints \"\"");
    }
  }

  /**
   * simple processing of mesh rendering information:
   * type fill|nofill|mesh|nomesh|dots|nodots|backlit|frontlit|fullylit
   * @param data
   */
  void setRendering(String data) {
    line = data.toLowerCase();
    String[] tokens = getTokens();
    for (int i = 1; i < tokens.length; i++) { //skip first
      super.setProperty(tokens[i].intern(),Boolean.TRUE, null);
    }
  }
  void discardTempData(boolean discardAll) {
    if (!discardAll)
      return;
    title = null;
    if (thisMesh == null)
      return;
    thisMesh.surfaceSet = null;
  }
  
  ////////////////////////////////////////////////////////////////
  // default color stuff
  ////////////////////////////////////////////////////////////////

  int indexColorPositive;
  int indexColorNegative;

  short getDefaultColix() {
    if (defaultColix != 0)
      return defaultColix;
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

  int nLCAO = 0;

  void drawLcaoCartoon(Vector3f z, Vector3f x) {
    String lcaoCartoon = sg.setLcao();
    defaultColix = Graphics3D.getColix(sg.getColor(1));
    int colorNeg = sg.getColor(-1);
    Vector3f y = new Vector3f();
    boolean isReverse = (lcaoCartoon.length() > 0 && lcaoCartoon.charAt(0) == '-');
    if (isReverse)
      lcaoCartoon = lcaoCartoon.substring(1);
    int sense = (isReverse ? -1 : 1);
    y.cross(z, x);
    String id = (thisMesh == null ? "lcao" + (++nLCAO) + "_" + lcaoCartoon
        : thisMesh.thisID);
    if (thisMesh == null)
      allocMesh(id);

    if (lcaoCartoon.equals("px")) {
      thisMesh.thisID += "a";
      createLcaoLobe(x, sense);
      setProperty("thisID", id + "b", null);
      createLcaoLobe(x, -sense);
      thisMesh.colix = Graphics3D.getColix(colorNeg);
      return;
    }
    if (lcaoCartoon.equals("py")) {
      thisMesh.thisID += "a";
      createLcaoLobe(y, sense);
      setProperty("thisID", id + "b", null);
      createLcaoLobe(y, -sense);
      thisMesh.colix = Graphics3D.getColix(colorNeg);
      return;
    }
    if (lcaoCartoon.equals("pz")) {
      thisMesh.thisID += "a";
      createLcaoLobe(z, sense);
      setProperty("thisID", id + "b", null);
      createLcaoLobe(z, -sense);
      thisMesh.colix = Graphics3D.getColix(colorNeg);
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

  Point4f lcaoDir = new Point4f();

  void createLcaoLobe(Vector3f lobeAxis, float factor) {
    initState();
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("creating isosurface " + thisMesh.thisID);
    }
    if (lobeAxis == null) {
      setProperty("sphere", new Float(factor / 2f), null);
      return;
    }
    lcaoDir.x = lobeAxis.x * factor;
    lcaoDir.y = lobeAxis.y * factor;
    lcaoDir.z = lobeAxis.z * factor;
    lcaoDir.w = 0.7f;
    setProperty("lobe", lcaoDir, null);
  }

  /////////////// meshDataServer interface /////////////////
  
  public void invalidateTriangles() {
    thisMesh.invalidateTriangles();
  }
  
  public void fillMeshData(MeshData meshData, int mode) {
    if (meshData == null) {
      if (thisMesh == null)
        allocMesh(null);
      thisMesh.clear("isosurface", sg.getIAddGridPoints(), thisMesh.showTriangles);
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
      if (thisMesh.vertexColixes == null || thisMesh.vertexCount > thisMesh.vertexColixes.length)
        thisMesh.vertexColixes = new short[thisMesh.vertexCount];
      meshData.vertexColixes = thisMesh.vertexColixes;
      return;
    case MeshData.MODE_PUT_SETS:
      thisMesh.surfaceSet = meshData.surfaceSet;
      thisMesh.vertexSets = meshData.vertexSets;
      thisMesh.nSets = meshData.nSets;
      //thisMesh.setColorSchemeSets();
      return;
    }
  }
  
  public void notifySurfaceGenerationCompleted() {
    setModelIndex();
    thisMesh.initialize(sg.getPlane() != null ? Mesh.FULLYLIT : lighting);
  }

  public void notifySurfaceMappingCompleted() {
    setModelIndex();
  }

  public Point3f[] calculateGeodesicSurface(BitSet bsSelected, BitSet bsIgnored,
                                    float envelopeRadius) {
    return viewer.calculateSurface(bsSelected, bsIgnored, envelopeRadius);
  }

  
  /////////////  VertexDataServer interface methods ////////////////
  
  public int getSurfacePointIndex(float cutoff, boolean isCutoffAbsolute,
                                  int x, int y, int z, Point3i offset, int vA,
                                  int vB, float valueA, float valueB,
                                  Point3f pointA, Vector3f edgeVector,
                                  boolean isContourType) {return 0;} 

  
  private boolean associateNormals;

  public int addVertexCopy(Point3f vertexXYZ, float value, int assocVertex) {
    return thisMesh.addVertexCopy(vertexXYZ, value, assocVertex, associateNormals);
  }
  
  public void addTriangleCheck(int iA, int iB, int iC, int check, boolean isAbsolute) {
    if (isAbsolute && !MeshData.checkCutoff(iA, iB, iC, thisMesh.vertexValues))
      return;
      thisMesh.addTriangleCheck(iA, iB, iC, check);
  }
  
  ////////////////////////////////////////////////////////////////////
  

  void setModelIndex() {
    setModelIndex(atomIndex, modelIndex);
    thisMesh.ptCenter.set(center);
    thisMesh.title = title;
    script = sg.getScript();
    thisMesh.scriptCommand = fixScript();
    jvxlData.jvxlDefinitionLine = JvxlReader.jvxlGetDefinitionLine(jvxlData, false);
    jvxlData.jvxlInfoLine = JvxlReader.jvxlGetDefinitionLine(jvxlData, true);
  }

  Vector getShapeDetail() {
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
 
}
