/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.util.*;
import org.jmol.jvxl.data.*;
import org.jmol.jvxl.calc.*;
import org.jmol.jvxl.api.*;

public abstract class VoxelReader implements VertexDataServer {
 
  /*
   * JVXL VoxelReader Class
   * ----------------------
   * Bob Hanson, hansonr@stolaf.edu, 20 Apr 2007
   * 
   * VoxelReader performs four functions:
   * 
   * 1) reading/creating volume scalar data ("voxels")
   * 2) generating a surface (vertices and triangles) from this set
   *      based on a specific cutoff value
   * 3) color-mapping this surface with other data
   * 4) creating JVXL format file data for this surface
   * 
   * VoxelReader is an ABSTRACT class, instantiated as one of the 
   * following to perform specific functions:
   * 
   *     VoxelReader (abstract MarchingReader)
   *          |
   *          |_______VolumeDataReader (uses provided predefined data)
   *          |          |
   *          |          |_____IsoFxyReader (creates data as needed)
   *          |          |_____IsoMepReader (creates predefined data)
   *          |          |_____IsoMOReader (creates predefined data)
   *          |          |_____IsoShapeReader (creates data as needed)
   *          |          |_____IsoSolventReader (creates predefined data)
   *          |                    |___IsoPlaneReader (predefines data)
   *          |          
   *          |
   *          |_______VolumeFileReader (abstract)
   *                      |
   *                      |______ApbsReader
   *                      |______CubeReader
   *                      |______JvxlReader
   *                                  |______JvxlPReader (progressive order -- X low to high)
   * 
   * The first step is to create a VolumeData structure:
   * 
   *   public final Point3f volumetricOrigin = new Point3f();
   *   public final Vector3f[] volumetricVectors = new Vector3f[3];
   *   public final int[] voxelCounts = new int[3];
   *   public float[][][] voxelData;
   * 
   * such as exists in a CUBE file.
   * 
   * The second step is to use the Marching Cubes algorithm to 
   * create a surface set of vertices and triangles. The data structure
   * involved for that is MeshData, containing:
   * 
   *   public int vertexCount;
   *   public Point3f[] vertices;
   *   public float[] vertexValues;
   *   public int polygonCount;
   *   public int[][] polygonIndexes;
   *   
   * The third (optional) step is to color those vertices using
   * a set of color index values provided by a color encoder. This
   * data is also stored in MeshData:  
   *   
   *   public short[] vertexColixes; 
   * 
   * Finally -- actually, throughout the process -- VoxelReader
   * creates a JvxlData structure containing the critical information
   * that is necessary for creating Jvxl surface data files. For that,
   * we have the JvxlData structure. 
   * 
   * Two interfaces are defined, and more should be. These include 
   * VertexDataServer and MeshDataServer.
   * 
   * VertexDataServer
   * ----------------
   * 
   * contains three methods, getSurfacePointIndex, addVertexCopy, and addTriangleCheck.
   * 
   * These deliver MarchingCubes and MarchingSquares vertex data in 
   * return for a vertex index number that can later be used for defining
   * a set of triangles.
   * 
   * VoxelReader implements this interface.
   * 
   * 
   * MeshDataServer extends VertexDataServer
   * ---------------------------------------
   * 
   * contains additional methods that allow for later processing 
   * of the vertex/triangle data:
   * 
   *   public abstract void invalidateTriangles();
   *   public abstract void fillMeshData(MeshData meshData, int mode);
   *   public abstract void notifySurfaceGenerationCompleted();
   *   public abstract void notifySurfaceMappingCompleted();
   * 
   * Note that, in addition to these interfaces, some of the readers,
   * namely IsoFxyReader, IsoMepReader,IsoMOReader, and IsoSolvenReader
   * and (due to subclassing) IsoPlaneReader all currently require
   * direct connections to Jmol Viewer and Atom classes.   
   * 
   * 
   * The rough outline of Jvxl files is 
   * given below:
   * 

#comments (optional)
info line1
info line2
-na originx originy originz   [ANGSTROMS/BOHR] optional; BOHR assumed
n1 x y z
n2 x y z
n3 x y z
a1 a1.0 x y z
a2 a2.0 x y z
a3 a3.0 x y z
a4 a4.0 x y z 
etc. -- na atoms
-ns 35 90 35 90 Jmol voxel format version 1.0
# more comments
cutoff +/-nEdges +/-nVertices [more here]
integer inside/outside edge data
ascii-encoded fractional edge data
ascii-encoded fractional color data
# optional comments

   * 
   * 
   * 
   * 
   */

  protected SurfaceGenerator sg;
  protected MeshDataServer meshDataServer;
  
  protected ColorEncoder colorEncoder;
  
  protected Parameters params;
  protected MeshData meshData;
  protected JvxlData jvxlData;
  protected VolumeData volumeData;
  
  protected boolean isProgressive = false;
  protected boolean isXLowToHigh = false; //can be overridden in some readers by --progressive
  private float assocCutoff = 0.3f; 
  
  VoxelReader(SurfaceGenerator sg) {
    this.sg = sg;
    this.colorEncoder = sg.getColorEncoder();
    this.params = sg.getParams();
    
    assocCutoff = params.assocCutoff;
    isXLowToHigh = params.isXLowToHigh;
    this.meshData = sg.getMeshData();
    this.jvxlData = sg.getJvxlData();
    setVolumeData(sg.getVolumeData());
    this.meshDataServer = sg.getMeshDataServer();
    cJvxlEdgeNaN = (char) (defaultEdgeFractionBase + defaultEdgeFractionRange);
  }
    
  final static float ANGSTROMS_PER_BOHR = 0.5291772f;
  final static int defaultEdgeFractionBase = 35; //#$%.......
  final static int defaultEdgeFractionRange = 90;
  final static int defaultColorFractionBase = 35;
  final static int defaultColorFractionRange = 90;
  final static float defaultMappedDataMin = 0f;
  final static float defaultMappedDataMax = 1.0f;
  final static float defaultCutoff = 0.02f;

  private int edgeCount;
  
  protected Point3f volumetricOrigin;
  protected Vector3f[] volumetricVectors;
  protected int[] voxelCounts;
  protected float[][][] voxelData;

  void setVolumeData(VolumeData v) {
    nBytes = 0;
    volumetricOrigin = v.volumetricOrigin;
    volumetricVectors = v.volumetricVectors;
    voxelCounts = v.voxelCounts;
    voxelData = v.voxelData;
    volumeData = v;
  }
  
  abstract void readVolumeParameters();
  abstract void readVolumeData(boolean isMapData);
  
  ////////////////////////////////////////////////////////////////
  // CUBE/APBS/JVXL file reading stuff
  ////////////////////////////////////////////////////////////////

  protected int nBytes;
  protected int nDataPoints;
  protected int nPointsX, nPointsY, nPointsZ;

  protected boolean isJvxl, isApbsDx;

  protected int edgeFractionBase;
  protected int edgeFractionRange;
  protected int colorFractionBase;
  protected int colorFractionRange;

  protected StringBuffer jvxlFileHeaderBuffer;
  protected StringBuffer fractionData;
  protected String  jvxlEdgeDataRead = "";
  protected String  jvxlColorDataRead = "";
  protected boolean jvxlDataIsColorMapped;
  protected boolean jvxlDataIsPrecisionColor;
  protected boolean jvxlDataIs2dContour;
  protected float   jvxlCutoff;
  protected int     jvxlNSurfaceInts;
  protected char    cJvxlEdgeNaN;

  
  protected int contourVertexCount;
  
  void jvxlUpdateInfo() {
    JvxlReader.jvxlUpdateInfo(jvxlData, params.title, nBytes);
  }

  boolean createIsosurface(boolean justForPlane) {
    resetIsosurface();
    readVolumeParameters();
    if (justForPlane) {
      volumeData.setDataDistanceToPlane(params.thePlane);
      if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES);
      params.setMapRanges(this);
    } else {
      readVolumeData(false);
    }
    generateSurfaceData();
    jvxlData.jvxlFileHeader = jvxlFileHeaderBuffer.toString();
    jvxlData.cutoff = (isJvxl ? jvxlCutoff : params.cutoff);
    jvxlData.jvxlColorData = "";
    jvxlData.jvxlPlane = params.thePlane;
    jvxlData.jvxlEdgeData = fractionData.toString();
    jvxlData.isBicolorMap = params.isBicolorMap;
    jvxlData.isContoured = params.isContoured;
    jvxlData.nContours = params.nContours;
    jvxlData.nEdges = edgeCount;
    jvxlData.edgeFractionBase = edgeFractionBase;
    jvxlData.edgeFractionRange = edgeFractionRange;
    jvxlData.colorFractionBase = colorFractionBase;
    jvxlData.colorFractionRange = colorFractionRange;
    jvxlData.jvxlDataIs2dContour = jvxlDataIs2dContour;
    jvxlData.jvxlDataIsColorMapped = jvxlDataIsColorMapped;
    jvxlData.isXLowToHigh = isXLowToHigh;
    jvxlData.nPointsX = nPointsX;
    jvxlData.nPointsY = nPointsY;
    jvxlData.nPointsZ = nPointsZ;

    if (jvxlDataIsColorMapped) {
      if (meshDataServer != null)
        meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES);
      jvxlData.jvxlColorData = readColorData();
    }
    jvxlData.jvxlExtraLine = JvxlReader.jvxlExtraLine(jvxlData, 1);
    return true;
  }

  void resetIsosurface() {
    meshData.clear("isosurface");
    if (meshDataServer != null)
      meshDataServer.fillMeshData(null, 0);
    contourVertexCount = 0;
    if (params.cutoff == Float.MAX_VALUE)
      params.cutoff = defaultCutoff;
    jvxlData.jvxlSurfaceData = "";
    jvxlData.jvxlEdgeData = "";
    jvxlData.jvxlColorData = "";
    edgeCount = 0;
    edgeFractionBase = defaultEdgeFractionBase;
    edgeFractionRange = defaultEdgeFractionRange;
    colorFractionBase = defaultColorFractionBase;
    colorFractionRange = defaultColorFractionRange;
    params.mappedDataMin = Float.MAX_VALUE;
  }

  void discardTempData(boolean discardAll) {
    if (!discardAll)
      return;
    voxelData = null;
    marchingSquares = null;
    marchingCubes = null;
  }
 
  protected void initializeVolumetricData() {
    nPointsX = voxelCounts[0];
    nPointsY = voxelCounts[1];
    nPointsZ = voxelCounts[2];
    volumeData.setUnitVectors();
    setVolumeData(volumeData);
  }
  
  // this needs to be specific for each reader
  abstract protected void readVoxelData(boolean isMapData) throws Exception ;

  protected void gotoAndReadVoxelData(boolean isMapData) {
    initializeVolumetricData();
    if (nPointsX <= 0 || nPointsY <= 0 || nPointsZ <= 0)
      return;
    try {
      gotoData(params.fileIndex - 1, nPointsX * nPointsY * nPointsZ);
      readVoxelData(isMapData);
    } catch (Exception e) {
      Logger.error(e.toString());
      throw new NullPointerException();
    }
  }

  protected void gotoData(int n, int nPoints) throws Exception {
    //only for file reader
  }
  
  protected String readColorData() {
    //jvxl only -- overloaded
    return "";
  }
  
  ////////////////////////////////////////////////////////////////
  // marching cube stuff
  ////////////////////////////////////////////////////////////////

  protected MarchingSquares marchingSquares;
  private MarchingCubes marchingCubes;

  private void generateSurfaceData() {
    fractionData = new StringBuffer();
    contourVertexCount = 0;
    int contourType = -1;
    marchingSquares = null;
    if (params.thePlane != null || params.isContoured) {
      marchingSquares = new MarchingSquares(this, volumeData, params.thePlane,
          params.nContours, params.thisContour);
      contourType = marchingSquares.getContourType();
      marchingSquares.setMinMax(params.valueMappedToRed, params.valueMappedToBlue);
    }

    marchingCubes = new MarchingCubes(this, volumeData, 
        params.isContoured, contourType,
        params.cutoff, params.isCutoffAbsolute);

    edgeCount = marchingCubes.generateSurfaceData(isXLowToHigh);
    
    if (isJvxl)
      fractionData = new StringBuffer(jvxlEdgeDataRead);
    fractionData.append('\n');
  }

  protected static boolean isInside(float voxelValue, float max, boolean isAbsolute) {
    return MarchingCubes.isInside(voxelValue, max, isAbsolute);
  }

  /////////////////  MarchingReader Interface Methods ///////////////////
  
  protected final Point3f ptTemp = new Point3f();

  final float[] fReturn = new float[1];
  
  public int getSurfacePointIndex(float cutoff, boolean isCutoffAbsolute,
                                  int x, int y, int z, Point3i offset, int vA,
                                  int vB, float valueA, float valueB,
                                  Point3f pointA, Vector3f edgeVector,
                                  boolean isContourType) {
    float thisValue = readSurfacePoint(cutoff, isCutoffAbsolute, valueA,
        valueB, pointA, edgeVector, fReturn);
    /* 
     * In the case of a setup for a Marching Squares calculation,
     * we are collecting just the desired type of intersection for the 2D marching
     * square contouring -- x, y, or z. In the case of a contoured f(x,y) surface, 
     * we take every point.
     * 
     */

    if (marchingSquares != null && params.isContoured)
      return isContourType ? marchingSquares.addContourVertex(x, y, z, offset,
          ptTemp, cutoff) : Integer.MAX_VALUE;
    int assocVertex = (assocCutoff > 0 ? (fReturn[0] < assocCutoff ? vA
        : fReturn[0] > 1 - assocCutoff ? vB : -1) : -1);
    if (assocVertex >= 0)
      assocVertex = marchingCubes.getLinearOffset(x, y, z, assocVertex);
    int iV = addVertexCopy(ptTemp, thisValue, assocVertex);
    if (params.iAddGridPoints) {
      marchingCubes.calcVertexPoint(x, y, z, vB, ptTemp);
      addVertexCopy(valueA < valueB ? pointA : ptTemp, Float.NaN, -3);
      addVertexCopy(valueA < valueB ? ptTemp : pointA, Float.NaN, -3);
    }
    return iV;
  }

  protected float readSurfacePoint(float cutoff, boolean isCutoffAbsolute, float valueA,
                                   float valueB, Point3f pointA, Vector3f edgeVector, float[] fReturn) {

              //JvxlReader may or may not call this

              float fraction, thisValue;
              float diff = valueB - valueA;
              fraction = (cutoff - valueA) / diff;
              if (isCutoffAbsolute && (fraction < 0 || fraction > 1))
                fraction = (-cutoff - valueA) / diff;

              if (fraction < 0 || fraction > 1) {
                //Logger.error("problem with unusual fraction=" + fraction + " cutoff="
                //  + cutoff + " A:" + valueA + " B:" + valueB);
                fraction = Float.NaN;
              }
              fReturn[0] = fraction;
              if (!isJvxl)
                fractionData.append(JvxlReader.jvxlFractionAsCharacter(fraction,
                    edgeFractionBase, edgeFractionRange));

              thisValue = valueA + fraction * diff;
              ptTemp.scaleAdd(fraction, edgeVector, pointA);
              return thisValue;
            }
            
  public int addVertexCopy(Point3f vertexXYZ, float value, int assocVertex) {
    if (meshDataServer == null)
      return meshData.addVertexCopy(vertexXYZ, value, assocVertex);
    return meshDataServer.addVertexCopy(vertexXYZ, value, assocVertex);
  }
  
  public void addTriangleCheck(int iA, int iB, int iC, int check, boolean isAbsolute) {
    if (meshDataServer == null) {
      if (isAbsolute && !MeshData.checkCutoff(iA, iB, iC, meshData.vertexValues))
        return;
      meshData.addTriangleCheck(iA, iB, iC, check);
    } else {
      meshDataServer.addTriangleCheck(iA, iB, iC, check, isAbsolute);
    }
  }
  
  ////////////////////////////////////////////////////////////////////
  
  
   ////////////////////////////////////////////////////////////////
  // color mapping methods
  ////////////////////////////////////////////////////////////////

  void colorIsosurface() {
    if (params.isContoured && !(jvxlDataIs2dContour || params.thePlane != null)) {
      Logger.error("Isosurface error: Cannot contour this type of data.");
      return;
    }
    if (meshDataServer != null) {
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES);
    }
    if (params.isContoured) {
      params.setMapRanges(this);
      marchingSquares.setMinMax(params.valueMappedToRed, params.valueMappedToBlue);
      contourVertexCount = marchingSquares.generateContourData(jvxlDataIs2dContour);
      if (meshDataServer != null)
        meshDataServer.notifySurfaceGenerationCompleted();
    }
      
    applyColorScale();
    jvxlData.nContours = params.nContours;
    jvxlData.jvxlExtraLine = JvxlReader.jvxlExtraLine(jvxlData,1);
    
    jvxlData.jvxlFileMessage = "mapped: min = " + params.valueMappedToRed + "; max = "
        + params.valueMappedToBlue;
  }

  
  void applyColorScale() {
    if (params.colorPhase == 0)
      params.colorPhase = 1;
    if (meshDataServer == null) {
      meshData.vertexColixes = new short[meshData.vertexCount];  
    } else {
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES);
    }
    int vertexCount = meshData.vertexCount;
    colorFractionBase = defaultColorFractionBase;
    colorFractionRange = defaultColorFractionRange;
    params.setMapRanges(this);
    float min = params.mappedDataMin;
    float max = params.mappedDataMax;
    StringBuffer list = null, list1 = null;
    //colorBySign is true when colorByPhase is true, but not vice-versa
    //old: boolean saveColorData = !(params.colorByPhase && !params.isBicolorMap && !params.colorBySign); //sorry!
    boolean saveColorData = params.isBicolorMap || params.colorBySign || !params.colorByPhase;
    if (saveColorData) {
      list = new StringBuffer();
      list1 = new StringBuffer();
    }
    int incr = 1;
    char[] remainder = new char[1];
    boolean writePrecisionColor = (jvxlDataIsPrecisionColor || params.isContoured || params.remappable);
    int lastVertex = (contourVertexCount > 0 ? contourVertexCount : vertexCount);
    short minColorIndex = -1;
    short maxColorIndex = 0;
    if (params.isBicolorMap && !params.isContoured || params.colorBySign) {
      minColorIndex = ColorEncoder.getColorIndex(params.isColorReversed ? params.colorPos : params.colorNeg);
      maxColorIndex = ColorEncoder.getColorIndex(params.isColorReversed ? params.colorNeg : params.colorPos);
    }
    for (int i = 0; i < vertexCount; i += incr) {
      float value = getVertexColorValue(i, minColorIndex, maxColorIndex);
      if (i < lastVertex) {
        char ch;
        if (writePrecisionColor) {
          ch = JvxlReader.jvxlValueAsCharacter2(value, min, max,
              colorFractionBase, colorFractionRange, remainder);
          if (saveColorData)
            list1.append(remainder[0]);
        } else {
          //isColorReversed
          ch = JvxlReader.jvxlValueAsCharacter(value, params.valueMappedToRed,
              params.valueMappedToBlue, colorFractionBase, colorFractionRange);
        }
        if (saveColorData)
          list.append(ch);
      }
    }
    jvxlData.isJvxlPrecisionColor = writePrecisionColor;
    jvxlData.jvxlColorData = (saveColorData ? list.append(list1).append('\n')
        .toString() : "");
    jvxlData.valueMappedToRed = params.valueMappedToRed;
    jvxlData.valueMappedToBlue = params.valueMappedToBlue;
    jvxlData.mappedDataMin = params.mappedDataMin;
    jvxlData.mappedDataMax = params.mappedDataMax;
    if (meshDataServer != null && params.colorBySets)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_SETS);
  }

  private float getVertexColorValue(int vertexIndex, short minColorIndex, short maxColorIndex) {
    float value, datum;
    /* but RETURNS the actual value, not the truncated one
     * right, so what we are doing here is setting a range within the 
     * data for which we want red-->blue, but returning the actual
     * number so it can be encoded more precisely. This turned out to be
     * the key to making the JVXL contours work.
     *  
     */
    if (params.colorBySets)
      datum = value = meshData.vertexSets[vertexIndex];
    else if (params.colorByPhase)
      datum = value = getPhase(meshData.vertices[vertexIndex]);
    else if (params.isBicolorMap && !params.isContoured) // will be current mesh only
      datum = value = meshData.vertexValues[vertexIndex];
    else if (jvxlDataIs2dContour)
      datum = value = marchingSquares.getInterpolatedPixelValue(meshData.vertices[vertexIndex]);
    else
      datum = value = volumeData.lookupInterpolatedVoxelValue(meshData.vertices[vertexIndex]);
    if (minColorIndex >= 0) {
      if (value <= 0)
        meshData.vertexColixes[vertexIndex] = minColorIndex;
      else if (value > 0)
        meshData.vertexColixes[vertexIndex] = maxColorIndex;
      if (!params.isContoured)
        datum = (value > 0 ? 0.999f : -0.999f);
    } else {
      if (value < params.valueMappedToRed)
        value = params.valueMappedToRed;
      if (value >= params.valueMappedToBlue)
        value = params.valueMappedToBlue;
      meshData.vertexColixes[vertexIndex] = getColorIndexFromPalette(value);
    }
    return datum;
  }

  private final static String[] colorPhases = { "_orb", "x", "y", "z", "xy", "yz",
    "xz", "x2-y2", "z2" };

  static int getColorPhaseIndex(String color) {
    int colorPhase = -1;
    for (int i = colorPhases.length; --i >= 0;)
      if (color.equalsIgnoreCase(colorPhases[i])) {
        colorPhase = i;
        break;
      }
    return colorPhase;
  }
  
  private float getPhase(Point3f pt) {
    switch (params.colorPhase) {
    case 0:
    case -1:
    case 1:
      return (pt.x > 0 ? 1 : -1);
    case 2:
      return (pt.y > 0 ? 1 : -1);
    case 3:
      return (pt.z > 0 ? 1 : -1);
    case 4:
      return (pt.x * pt.y > 0 ? 1 : -1);
    case 5:
      return (pt.y * pt.z > 0 ? 1 : -1);
    case 6:
      return (pt.x * pt.z > 0 ? 1 : -1);
    case 7:
      return (pt.x * pt.x - pt.y * pt.y > 0 ? 1 : -1);
    case 8:
      return (pt.z * pt.z * 2f - pt.x * pt.x - pt.y * pt.y > 0 ? 1 : -1);
    }
    return 1;
  }
  
  float getMinMappedValue() {
    if (params.colorBySets)
      return 0;
    int vertexCount = (contourVertexCount > 0 ? contourVertexCount : meshData.vertexCount);
    Point3f[] vertexes = meshData.vertices;
    float min = Float.MAX_VALUE;
    int incr = 1;
    for (int i = 0; i < vertexCount; i += incr) {
      float challenger;
      if (jvxlDataIs2dContour)
        challenger = marchingSquares.getInterpolatedPixelValue(vertexes[i]);
      else
        challenger = volumeData.lookupInterpolatedVoxelValue(vertexes[i]);
      if (challenger < min)
        min = challenger;
    }
    return min;
  }

  float getMaxMappedValue() {
    if (params.colorBySets)
      return Math.max(meshData.nSets - 1, 0);
    int vertexCount = (contourVertexCount > 0 ? contourVertexCount : meshData.vertexCount);
    Point3f[] vertexes = meshData.vertices;
    float max = -Float.MAX_VALUE;
    int incr = 1;
    for (int i = 0; i < vertexCount; i += incr) {
      float challenger;
      if (jvxlDataIs2dContour)
        challenger = marchingSquares.getInterpolatedPixelValue(vertexes[i]);
      else
        challenger = volumeData.lookupInterpolatedVoxelValue(vertexes[i]);
      if (challenger == Float.MAX_VALUE)
        challenger = 0; //for now TESTING ONLY
      if (challenger > max && challenger != Float.MAX_VALUE)
        max = challenger;
    }
    return max;
  }

  protected short getColorIndexFromPalette(float value) {
    if (params.isColorReversed)
      return colorEncoder.getColorIndexFromPalette(-value, -params.valueMappedToBlue,
          -params.valueMappedToRed);
    return colorEncoder.getColorIndexFromPalette(value, params.valueMappedToRed,
        params.valueMappedToBlue);
  }
  
  public void updateSurfaceData() {
    if (meshDataServer == null) {
      meshData.invalidateTriangles();
    } else {
      meshDataServer.invalidateTriangles();
    }
    JvxlReader.jvxlUpdateSurfaceData(jvxlData, meshData.vertexValues,
        meshData.vertexCount, meshData.vertexIncrement, cJvxlEdgeNaN);
  }
  
  boolean selectPocket() {
    return false;
    // solvent reader implements this
  }
  
  void excludeMinimumSet() {
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES);
    meshData.getSurfaceSet();
    BitSet bs;
    for (int i = meshData.nSets; --i >= 0;) 
      //System.out.println(" set " + i + " " + Viewer.cardinalityOf(surfaceSet[i]));
      if ((bs = meshData.surfaceSet[i]) != null) {
        int n = 0;
        for (int j = bs.size(); --j >= 0; )   // cardinality
          if (bs.get(j))
            n++;
        if (n < params.minSet)
          meshData.invalidateSurfaceSet(i);
    }
    updateSurfaceData();
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_SETS);
  }
}


