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

import java.io.OutputStream;
import java.util.BitSet;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.util.*;
import org.jmol.g3d.Graphics3D;
import org.jmol.jvxl.data.*;
import org.jmol.jvxl.api.MeshDataServer;
import org.jmol.jvxl.api.VertexDataServer;
import org.jmol.jvxl.calc.*;

public abstract class SurfaceReader implements VertexDataServer {

  /*
   * JVXL SurfaceReader Class
   * ----------------------
   * Bob Hanson, hansonr@stolaf.edu, 20 Apr 2007
   * 
   * SurfaceReader performs four functions:
   * 
   * 1) reading/creating volume scalar data ("voxels")
   * 2) generating a surface (vertices and triangles) from this set
   *      based on a specific cutoff value
   * 3) color-mapping this surface with other data
   * 4) creating JVXL format file data for this surface
   * 
   * In the case that the surface type does not include voxel data (EfvetReader), 
   * only steps 2 and 3 are involved, and no cutoff value is used.
   * 
   * SurfaceReader is an ABSTRACT class, instantiated as one of the 
   * following to perform specific functions:
   * 
   *     SurfaceReader (abstract MarchingReader)
   *          |
   *          |_______VolumeDataReader (uses provided predefined data)
   *          |          |
   *          |          |_____IsoFxyReader (creates data as needed)
   *          |          |_____IsoFxyzReader (creates data as needed)
   *          |          |_____IsoMepReader (creates predefined data)
   *          |          |_____IsoMOReader (creates predefined data)
   *          |          |_____IsoShapeReader (creates data as needed)
   *          |          |_____IsoSolventReader (creates predefined data)
   *          |                    |___IsoPlaneReader (predefines data)
   *          |          
   *          |_______SurfaceFileReader (abstract)
   *                    |
   *                    |_______VolumeFileReader (abstract)
   *                    |           |
   *                    |           |______ApbsReader
   *                    |           |______CubeReader
   *                    |           |______JaguarReader
   *                    |           |______JvxlXmlReader
   *                    |           |           |______JvxlReader
   *                    |           |
   *                    |           |______ElectronDensityFileReader (abstract)
   *                    |                       |______MrcBinaryReader
   *                    |                       |______XplorReader (version 3.1)
   *                    |
   *                    |_______PolygonFileReader (abstract)
   *                                |
   *                                |______EfvetReader
   *                                |______PmeshReader
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
   * Finally -- actually, throughout the process -- SurfaceReader
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
   * SurfaceReader implements this interface.
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
  private String edgeData;

  protected boolean isProgressive = false;
  protected boolean isXLowToHigh = false; //can be overridden in some readers by --progressive
  private float assocCutoff = 0.3f;
  protected Point4f mappingPlane;
  
  boolean vertexDataOnly;
  boolean hasColorData;

  protected Point3f xyzMin, xyzMax;

  protected Point3f center;
  protected float[] anisotropy;
  protected boolean isAnisotropic;
  protected Matrix3f eccentricityMatrix;
  protected Matrix3f eccentricityMatrixInverse;
  protected boolean isEccentric;
  protected float eccentricityScale;
  protected float eccentricityRatio;

  SurfaceReader(SurfaceGenerator sg) {
    this.sg = sg;
    colorEncoder = sg.getColorEncoder();
    params = sg.getParams();
    marchingSquares = sg.getMarchingSquares();
    assocCutoff = params.assocCutoff;
    isXLowToHigh = params.isXLowToHigh;
    center = params.center;
    anisotropy = params.anisotropy;
    isAnisotropic = params.isAnisotropic;
    eccentricityMatrix = params.eccentricityMatrix;
    eccentricityMatrixInverse = params.eccentricityMatrixInverse;
    isEccentric = params.isEccentric;
    eccentricityScale = params.eccentricityScale;
    eccentricityRatio = params.eccentricityRatio;
    meshData = sg.getMeshData();
    jvxlData = sg.getJvxlData();
    setVolumeData(sg.getVolumeData());
    meshDataServer = sg.getMeshDataServer();
    cJvxlEdgeNaN = (char) (JvxlCoder.defaultEdgeFractionBase + JvxlCoder.defaultEdgeFractionRange);
  }

  final static float ANGSTROMS_PER_BOHR = 0.5291772f;
  final static float defaultMappedDataMin = 0f;
  final static float defaultMappedDataMax = 1.0f;
  final static float defaultCutoff = 0.02f;

  private int edgeCount;

  protected Point3f volumetricOrigin;
  protected Vector3f[] volumetricVectors;
  protected int[] voxelCounts;
  protected float[][][] voxelData;
  
  abstract protected void closeReader();
  
//  boolean mustCalcPoint = true; // for now

  protected void setOutputStream(OutputStream os) {
    // only for file readers
  }
 
  void setVolumeData(VolumeData v) {
    nBytes = 0;
    volumetricOrigin = v.volumetricOrigin;
    volumetricVectors = v.volumetricVectors;
    voxelCounts = v.voxelCounts;
    voxelData = v.voxelData;
    volumeData = v;
    
/*    if (mustCalcPoint)
      v.setDataSource(this);
*/  }

  protected abstract boolean readVolumeParameters();

  protected abstract boolean readVolumeData(boolean isMapData);

  boolean readTheVolumeData(boolean isMapData, Point4f plane) {
    mappingPlane = plane;
    return readVolumeData(isMapData);  
  }
  
  ////////////////////////////////////////////////////////////////
  // CUBE/APBS/JVXL file reading stuff
  ////////////////////////////////////////////////////////////////

  protected long nBytes;
  protected int nDataPoints;
  protected int nPointsX, nPointsY, nPointsZ;

  protected boolean isJvxl;

  protected int edgeFractionBase;
  protected int edgeFractionRange;
  protected int colorFractionBase;
  protected int colorFractionRange;

  protected StringBuffer jvxlFileHeaderBuffer;
  protected StringBuffer fractionData;
  protected String jvxlEdgeDataRead = "";
  protected String jvxlColorDataRead = "";
  protected BitSet jvxlVoxelBitSet;
  protected boolean jvxlDataIsColorMapped;
  protected boolean jvxlDataIsPrecisionColor;
  protected boolean jvxlDataIs2dContour;
  protected float jvxlCutoff;
  protected int jvxlNSurfaceInts;
  protected char cJvxlEdgeNaN;

  protected int contourVertexCount;

  void jvxlUpdateInfo() {
    jvxlData.jvxlUpdateInfo(params.title, nBytes);
  }

  boolean readAndSetVolumeParameters() {
    return (readVolumeParameters() && (vertexDataOnly || volumeData.setUnitVectors()));
  }

  boolean createIsosurface(boolean justForPlane) {
    resetIsosurface();
    if (!readAndSetVolumeParameters())
      return false;
    nPointsX = voxelCounts[0];
    nPointsY = voxelCounts[1];
    nPointsZ = voxelCounts[2];
    jvxlData.insideOut = params.insideOut;
    jvxlData.nPointsX = nPointsX;
    jvxlData.nPointsY = nPointsY;
    jvxlData.nPointsZ = nPointsZ;
    jvxlData.jvxlVolumeDataXml = volumeData.xmlData;
    if (justForPlane) {
      float[][][] voxelDataTemp =  volumeData.voxelData;
      volumeData.setDataDistanceToPlane(params.thePlane);
      if (meshDataServer != null)
        meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
      params.setMapRanges(this);
      generateSurfaceData();
      volumeData.voxelData = voxelDataTemp;
    } else {
      if (!readTheVolumeData(false, null))
        return false;
      generateSurfaceData();
    }
    String s = jvxlFileHeaderBuffer.toString();
    int i = s.indexOf('\n', s.indexOf('\n',s.indexOf('\n') + 1) + 1) + 1;
    jvxlData.jvxlFileTitle = s.substring(0, i);
    jvxlData.jvxlFileHeader = s;
    if (xyzMin == null)
      setBoundingBox();
    Logger.info("boundbox corners " + Escape.escape(xyzMin) + " " + Escape.escape(xyzMax));
    jvxlData.boundingBox = new Point3f[] {xyzMin, xyzMax};
    jvxlData.cutoff = (isJvxl ? jvxlCutoff : params.cutoff);
    jvxlData.isCutoffAbsolute = params.isCutoffAbsolute;
    jvxlData.pointsPerAngstrom = 1f/volumeData.volumetricVectorLengths[0];
    jvxlData.jvxlColorData = "";
    jvxlData.jvxlPlane = params.thePlane;
    jvxlData.jvxlEdgeData = edgeData;
    jvxlData.isBicolorMap = params.isBicolorMap;
    jvxlData.isContoured = params.isContoured;
    jvxlData.colorDensity = params.colorDensity;
    if (jvxlData.vContours != null)
      params.nContours = jvxlData.vContours.length;
    jvxlData.nContours = (params.contourFromZero 
        ? params.nContours : -1 - params.nContours);
    jvxlData.nEdges = edgeCount;
    jvxlData.edgeFractionBase = edgeFractionBase;
    jvxlData.edgeFractionRange = edgeFractionRange;
    jvxlData.colorFractionBase = colorFractionBase;
    jvxlData.colorFractionRange = colorFractionRange;
    jvxlData.jvxlDataIs2dContour = jvxlDataIs2dContour;
    jvxlData.jvxlDataIsColorMapped = jvxlDataIsColorMapped;
    jvxlData.isXLowToHigh = isXLowToHigh;
    jvxlData.vertexDataOnly = vertexDataOnly;

    if (jvxlDataIsColorMapped) {
      if (meshDataServer != null) {
        meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
        meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_COLOR_INDEXES, null);
      }
      jvxlData.jvxlColorData = readColorData();
      updateSurfaceData();
      if (meshDataServer != null)
        meshDataServer.notifySurfaceMappingCompleted();
    }
    return true;
  }

  void resetIsosurface() {
    meshData = new MeshData();
    if (meshDataServer != null)
      meshDataServer.fillMeshData(null, 0, null);
    contourVertexCount = 0;
    if (params.cutoff == Float.MAX_VALUE)
      params.cutoff = defaultCutoff;
    jvxlData.jvxlSurfaceData = "";
    jvxlData.jvxlEdgeData = "";
    jvxlData.jvxlColorData = "";
    //TODO: more resets of jvxlData?
    edgeCount = 0;
    edgeFractionBase = JvxlCoder.defaultEdgeFractionBase;
    edgeFractionRange = JvxlCoder.defaultEdgeFractionRange;
    colorFractionBase = JvxlCoder.defaultColorFractionBase;
    colorFractionRange = JvxlCoder.defaultColorFractionRange;
    params.mappedDataMin = Float.MAX_VALUE;
  }

  void discardTempData(boolean discardAll) {
    if (!discardAll)
      return;
    voxelData = null;
    sg.setMarchingSquares(marchingSquares = null);
    marchingCubes = null;
  }

  protected void initializeVolumetricData() {
    nPointsX = voxelCounts[0];
    nPointsY = voxelCounts[1];
    nPointsZ = voxelCounts[2];
    setVolumeData(volumeData);
  }

  // this needs to be specific for each reader
  abstract protected void readSurfaceData(boolean isMapData) throws Exception;

  protected boolean gotoAndReadVoxelData(boolean isMapData) {
    //overloaded in jvxlReader
    initializeVolumetricData();
    if (nPointsX > 0 && nPointsY > 0 && nPointsZ > 0)
      try {
        gotoData(params.fileIndex - 1, nPointsX * nPointsY * nPointsZ);
        readSurfaceData(isMapData);
      } catch (Exception e) {
        Logger.error(e.toString());
        return false;
      }
    return true;
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

  public float getValue(int x, int y, int z, int ptyz) {
    return volumeData.voxelData[x][y][z];
  }

  public void getPlane(int x) {
    // implementation specific -- for progressive readers
    // MrcBinaryReader does this
  }
  private void generateSurfaceData() {
    edgeData = "";
    if (vertexDataOnly) {
      try {
        readSurfaceData(false);
      } catch (Exception e) {
        e.printStackTrace();
        Logger.error("Exception in SurfaceReader::readSurfaceData: "
            + e.getMessage());
      }
      return;
    }
    contourVertexCount = 0;
    int contourType = -1;
    marchingSquares = null;

    if (params.thePlane != null || params.isContoured) {
      marchingSquares = new MarchingSquares(this, volumeData, params.thePlane,
          params.contoursDiscrete, params.nContours, params.thisContour,
          params.contourFromZero);
      contourType = marchingSquares.getContourType();
      marchingSquares.setMinMax(params.valueMappedToRed,
          params.valueMappedToBlue);
    }
    params.contourType = contourType;
    params.isXLowToHigh = isXLowToHigh;
    marchingCubes = new MarchingCubes(this, volumeData, params, jvxlVoxelBitSet);
    String data = marchingCubes.getEdgeData();
    if (params.thePlane == null)
      edgeData = data;
    jvxlData.setSurfaceInfoFromBitSet(marchingCubes.getBsVoxels(),
        params.thePlane);
    jvxlData.jvxlExcluded = params.bsExcluded;
    if (isJvxl)
      edgeData = jvxlEdgeDataRead;
  }

  /////////////////  MarchingReader Interface Methods ///////////////////

  protected final Point3f ptTemp = new Point3f();

  public int getSurfacePointIndexAndFraction(float cutoff, boolean isCutoffAbsolute,
                                  int x, int y, int z, Point3i offset, int vA,
                                  int vB, float valueA, float valueB,
                                  Point3f pointA, Vector3f edgeVector,
                                  boolean isContourType, float[] fReturn) {
    float thisValue = getSurfacePointAndFraction(cutoff, isCutoffAbsolute, valueA,
        valueB, pointA, edgeVector, fReturn, ptTemp);
    /* 
     * from MarchingCubes
     * 
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
        : fReturn[0] > 1 - assocCutoff ? vB : MarchingSquares.CONTOUR_POINT)
        : MarchingSquares.CONTOUR_POINT);
    if (assocVertex >= 0)
      assocVertex = marchingCubes.getLinearOffset(x, y, z, assocVertex);
    int n = addVertexCopy(ptTemp, thisValue, assocVertex);
    if (params.iAddGridPoints) {
      marchingCubes.calcVertexPoint(x, y, z, vB, ptTemp);
      addVertexCopy(valueA < valueB ? pointA : ptTemp, Float.NaN,
          MarchingSquares.EDGE_POINT);
      addVertexCopy(valueA < valueB ? ptTemp : pointA, Float.NaN,
          MarchingSquares.EDGE_POINT);
    }
    return n;
  }

  protected float getSurfacePointAndFraction(float cutoff, boolean isCutoffAbsolute,
                                   float valueA, float valueB, Point3f pointA,
                                   Vector3f edgeVector, float[] fReturn,
                                   Point3f ptReturn) {

    //JvxlReader may or may not call this

    float diff = valueB - valueA;
    float fraction = (cutoff - valueA) / diff;
    if (isCutoffAbsolute && (fraction < 0 || fraction > 1))
      fraction = (-cutoff - valueA) / diff;

    if (fraction < 0 || fraction > 1) {
      //Logger.error("problem with unusual fraction=" + fraction + " cutoff="
      //  + cutoff + " A:" + valueA + " B:" + valueB);
      fraction = Float.NaN;
    }
    fReturn[0] = fraction;
    ptReturn.scaleAdd(fraction, edgeVector, pointA);
    return valueA + fraction * diff;
  }

  public int addVertexCopy(Point3f vertexXYZ, float value, int assocVertex) {
    if (meshDataServer == null)
      return meshData.addVertexCopy(vertexXYZ, value, assocVertex);
    return meshDataServer.addVertexCopy(vertexXYZ, value, assocVertex);
  }

  public int addTriangleCheck(int iA, int iB, int iC, int check, int check2,
                              boolean isAbsolute, int color) {
    return (meshDataServer != null ? meshDataServer.addTriangleCheck(iA, iB,
        iC, check, check2, isAbsolute, color) : isAbsolute
        && !MeshData.checkCutoff(iA, iB, iC, meshData.vertexValues) ? -1
        : meshData.addTriangleCheck(iA, iB, iC, check, check2, color));
  }

  ////////////////////////////////////////////////////////////////
  // color mapping methods
  ////////////////////////////////////////////////////////////////

  void colorIsosurface() {
    if (params.isSquared)
      volumeData.filterData(true, Float.NaN);
/*    if (params.isContoured && marchingSquares == null) {
      //    if (params.isContoured && !(jvxlDataIs2dContour || params.thePlane != null)) {
      Logger.error("Isosurface error: Cannot contour this type of data.");
      return;
    }
*/
    if (meshDataServer != null) {
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    }

    if (params.isContoured && marchingSquares != null) {
      params.setMapRanges(this);
      marchingSquares.setMinMax(params.valueMappedToRed,
          params.valueMappedToBlue);
      contourVertexCount = marchingSquares
          .generateContourData(jvxlDataIs2dContour);
      jvxlData.contourValuesUsed = marchingSquares.getContourValues();
      if (meshDataServer != null)
        meshDataServer.notifySurfaceGenerationCompleted();
    }

    applyColorScale();
    jvxlData.nContours = (params.contourFromZero 
        ? params.nContours : -1 - params.nContours);
    jvxlData.jvxlFileMessage = "mapped: min = " + params.valueMappedToRed
        + "; max = " + params.valueMappedToBlue;
  }

  void applyColorScale() {
    colorFractionBase = jvxlData.colorFractionBase = JvxlCoder.defaultColorFractionBase;
    colorFractionRange = jvxlData.colorFractionRange = JvxlCoder.defaultColorFractionRange;
    if (params.colorPhase == 0)
      params.colorPhase = 1;
    if (meshDataServer == null) {
      meshData.vertexColixes = new short[meshData.vertexCount];
    } else {
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_COLOR_INDEXES, null);
    }
    params.setMapRanges(this);
    //colorBySign is true when colorByPhase is true, but not vice-versa
    //old: boolean saveColorData = !(params.colorByPhase && !params.isBicolorMap && !params.colorBySign); //sorry!
    boolean saveColorData = (params.colorDensity || params.isBicolorMap || params.colorBySign || !params.colorByPhase);
    // colors mappable always now
    jvxlData.isJvxlPrecisionColor = true;//(jvxlDataIsPrecisionColor || params.isContoured || params.remappable);
    jvxlData.valueMappedToRed = params.valueMappedToRed;
    jvxlData.valueMappedToBlue = params.valueMappedToBlue;
    jvxlData.mappedDataMin = params.mappedDataMin;
    jvxlData.mappedDataMax = params.mappedDataMax;
    jvxlData.vertexCount = (contourVertexCount > 0 ? contourVertexCount
        : meshData.vertexCount);
    jvxlData.minColorIndex = -1;
    jvxlData.maxColorIndex = 0;
    jvxlData.contourValues = params.contoursDiscrete;
    jvxlData.isColorReversed = params.isColorReversed;
    if (!params.colorDensity)
    if (params.isBicolorMap && !params.isContoured || params.colorBySign) {
      jvxlData.minColorIndex = ColorEncoder
          .getColorIndex(params.isColorReversed ? params.colorPos
              : params.colorNeg);
      jvxlData.maxColorIndex = ColorEncoder
          .getColorIndex(params.isColorReversed ? params.colorNeg
              : params.colorPos);
    }
    jvxlData.isTruncated = (jvxlData.minColorIndex >= 0 && !params.isContoured);
    boolean useMeshDataValues =
    //      !jvxlDataIs2dContour && (params.isContoured && jvxlData.jvxlPlane != null || 
    vertexDataOnly || params.colorDensity || params.isBicolorMap && !params.isContoured;
    float value;
    if (!useMeshDataValues)
      for (int i = meshData.vertexCount; --i >= 0;) {
        /* right, so what we are doing here is setting a range within the 
         * data for which we want red-->blue, but returning the actual
         * number so it can be encoded more precisely. This turned out to be
         * the key to making the JVXL contours work.
         *  
         */
        if (params.colorBySets)
          value = meshData.vertexSets[i];
        else if (params.colorByPhase)
          value = getPhase(meshData.vertices[i]);
        else if (jvxlDataIs2dContour)
          value = marchingSquares
              .getInterpolatedPixelValue(meshData.vertices[i]);
        else
          value = volumeData.lookupInterpolatedVoxelValue(meshData.vertices[i]);
        meshData.vertexValues[i] = value;
      }
    colorData();

    JvxlCoder.jvxlCreateColorData(jvxlData,
        (saveColorData ? meshData.vertexValues : null));

    if (meshDataServer != null && params.colorBySets)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_SETS, null);
  }

  private void colorData() {

    float[] vertexValues = meshData.vertexValues;
    short[] vertexColixes = meshData.vertexColixes;
    meshData.polygonColixes = null;
    float valueBlue = jvxlData.valueMappedToBlue;
    float valueRed = jvxlData.valueMappedToRed;
    short minColorIndex = jvxlData.minColorIndex;
    short maxColorIndex = jvxlData.maxColorIndex;

    for (int i = meshData.vertexCount; --i >= 0;) {
      float value = vertexValues[i];
      if (minColorIndex >= 0) {
        if (value <= 0)
          vertexColixes[i] = minColorIndex;
        else if (value > 0)
          vertexColixes[i] = maxColorIndex;
      } else {
        if (value < valueRed)
          value = valueRed;
        if (value >= valueBlue)
          value = valueBlue;
          
        vertexColixes[i] = getColorIndexFromPalette(value);
      }
    }

    if ((params.nContours > 0 || jvxlData.contourValues != null) && jvxlData.contourColixes == null) {
      int n = (jvxlData.contourValues == null ? params.nContours : jvxlData.contourValues.length);
      short[] colors = jvxlData.contourColixes = new short[n];
      float dv = (valueBlue - valueRed) / (n + 1);
      // n + 1 because we want n lines between n + 1 slices
      for (int i = 0; i < n; i++) {
        float v = (jvxlData.contourValues == null ? valueRed + (i + 1) * dv : jvxlData.contourValues[i]);
        colors[i] = Graphics3D.getColixTranslucent(getArgbFromPalette(v));
      }
      //TODO -- this strips translucency
      jvxlData.contourColors = Graphics3D.getHexCodes(colors);
    }
  }
  
  private final static String[] colorPhases = { "_orb", "x", "y", "z", "xy",
      "yz", "xz", "x2-y2", "z2" };

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
    int vertexCount = (contourVertexCount > 0 ? contourVertexCount
        : meshData.vertexCount);
    Point3f[] vertexes = meshData.vertices;
    float min = Float.MAX_VALUE;
    boolean useVertexValue = (vertexDataOnly || params.colorDensity);
    for (int i = 0; i < vertexCount; i++) {
      float challenger;
      if (useVertexValue)
        challenger = meshData.vertexValues[i];
      else if (jvxlDataIs2dContour)
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
    int vertexCount = (contourVertexCount > 0 ? contourVertexCount
        : meshData.vertexCount);
    Point3f[] vertexes = meshData.vertices;
    float max = -Float.MAX_VALUE;
    boolean useVertexValue = (vertexDataOnly || params.colorDensity);
    for (int i = 0; i < vertexCount; i++) {
      float challenger;
      if (useVertexValue)
        challenger = meshData.vertexValues[i];
      else if (jvxlDataIs2dContour)
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
      return colorEncoder.getColorIndexFromPalette(-value,
          -params.valueMappedToBlue, -params.valueMappedToRed);
    return colorEncoder.getColorIndexFromPalette(value,
        params.valueMappedToRed, params.valueMappedToBlue);
  }

  protected int getArgbFromPalette(float value) {
    if (params.isColorReversed)
      return colorEncoder.getArgbFromPalette(-value,
          -params.valueMappedToBlue, -params.valueMappedToRed);
    return colorEncoder.getArgbFromPalette(value,
        params.valueMappedToRed, params.valueMappedToBlue);
  }

  void updateTriangles() {
    if (meshDataServer == null) {
      meshData.invalidatePolygons();
    } else {
      meshDataServer.invalidateTriangles();
    }
  }

  void updateSurfaceData() {
    updateTriangles();
    jvxlData.updateSurfaceData(meshData.vertexValues,
        meshData.vertexCount, meshData.vertexIncrement, cJvxlEdgeNaN);
  }

  public void selectPocket(boolean doExclude) {
    // solvent reader implements this
  }

  void excludeMinimumSet() {
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    meshData.getSurfaceSet();
    BitSet bs;
    for (int i = meshData.nSets; --i >= 0;)
      if ((bs = meshData.surfaceSet[i]) != null) {
        int n = 0;
        for (int j = bs.size(); --j >= 0;)
          // cardinality
          if (bs.get(j))
            n++;
        if (n < params.minSet)
          meshData.invalidateSurfaceSet(i);
      }
    updateSurfaceData();
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_SETS, null);
  }

  void excludeMaximumSet() {
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    meshData.getSurfaceSet();
    BitSet bs;
    for (int i = meshData.nSets; --i >= 0;)
      if ((bs = meshData.surfaceSet[i]) != null) {
        int n = 0;
        for (int j = bs.size(); --j >= 0;)
          // cardinality
          if (bs.get(j))
            n++;
        if (n > params.maxSet)
          meshData.invalidateSurfaceSet(i);
      }
    updateSurfaceData();
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_PUT_SETS, null);
  }
  
  protected void setVertexAnisotropy(Point3f pt) {
    pt.x *= anisotropy[0];
    pt.y *= anisotropy[1];
    pt.z *= anisotropy[2];
    pt.add(center);
  }

  protected void setVectorAnisotropy(Vector3f v) {
    haveSetAnisotropy = true;
    v.x *= anisotropy[0];
    v.y *= anisotropy[1];
    v.z *= anisotropy[2];
  }

  private boolean haveSetAnisotropy = false;
  
  protected void setVolumetricAnisotropy() {
    if (haveSetAnisotropy)
      return;
    setVolumetricOriginAnisotropy();
    setVectorAnisotropy(volumetricVectors[0]);
    setVectorAnisotropy(volumetricVectors[1]);
    setVectorAnisotropy(volumetricVectors[2]);
    
  }
  
  protected void setVolumetricOriginAnisotropy() {
    volumetricOrigin.set(center);
  }

  protected void setBoundingBox(Point3f pt, float margin) {
    if (xyzMin == null) {
      xyzMin = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
      xyzMax = new Point3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
    }
    if (pt.x - margin < xyzMin.x)
      xyzMin.x = pt.x - margin;
    if (pt.x + margin > xyzMax.x)
      xyzMax.x = pt.x + margin;
    if (pt.y - margin < xyzMin.y)
      xyzMin.y = pt.y - margin;
    if (pt.y + margin > xyzMax.y)
      xyzMax.y = pt.y + margin;
    if (pt.z - margin < xyzMin.z)
      xyzMin.z = pt.z - margin;
    if (pt.z + margin > xyzMax.z)
      xyzMax.z = pt.z + margin;
  }

  private void setBoundingBox() {
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    for (int i = 0; i < meshData.vertexCount; i++) {
      Point3f p = meshData.vertices[i];
      if (!Float.isNaN(p.x))
        setBoundingBox(p, 0);
    }
  }

  public void setMappingPlane(Point4f thePlane) {
    mappingPlane = thePlane;
  }
  

}
