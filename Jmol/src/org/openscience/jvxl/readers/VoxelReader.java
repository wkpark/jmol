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
package org.openscience.jvxl.readers;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.util.Logger;
import org.openscience.jvxl.util.*;
import org.openscience.jvxl.data.JvxlData;
import org.openscience.jvxl.data.VolumeData;
import org.openscience.jvxl.data.MeshData;
import org.openscience.jvxl.calc.MarchingCubes;
import org.openscience.jvxl.calc.MarchingSquares;

public class VoxelReader {
  
  //main class for reading any of the file formats
  
  /*
   * Jvxl.java
   *           creates a surfaceReader, which handles all user interface
   *           and then instantiates a voxel reader 
   * 
   * 
   *     VoxelReader
   *          |
   *          |_______VolumeDataReader  (still needs work)
   *          |
   *          |_______VolumeFileReader
   *                      |
   *                      |______ApbsReader
   *                      |______CubeReader
   *                      |______JvxlReader
   * 
   */

  ColorEncoder colorEncoder;
  Parameters params;
  MeshData meshData;
  JvxlData jvxlData;
  VolumeData volumeData; 
  boolean isProgressive = false;
  boolean isXLowToHigh = false; //can be overridden in some readers by --progressive

  VoxelReader(SurfaceGenerator sg) {
    this.colorEncoder = sg.colorEncoder;
    this.params = sg.params;
    isXLowToHigh = params.isProgressive;
    this.meshData = sg.meshData;
    this.jvxlData = sg.jvxlData;
    setVolumeData(sg.volumeData);
  }
    
  final static float ANGSTROMS_PER_BOHR = 0.5291772f;
  final static int defaultEdgeFractionBase = 35; //#$%.......
  final static int defaultEdgeFractionRange = 90;
  final static int defaultColorFractionBase = 35;
  final static int defaultColorFractionRange = 90;
  final static float defaultMappedDataMin = 0f;
  final static float defaultMappedDataMax = 1.0f;
  final static float defaultCutoff = 0.02f;

  boolean isAngstroms;
  int edgeFractionBase;
  int edgeFractionRange;
  int colorFractionBase;
  int colorFractionRange;

  StringBuffer jvxlFileHeaderBuffer;
  StringBuffer fractionData;
  String  jvxlEdgeDataRead = "";
  String  jvxlColorDataRead = "";
  boolean jvxlDataIsColorMapped;
  boolean jvxlDataIsPrecisionColor;
  boolean jvxlDataIs2dContour;
  float   jvxlCutoff;
  int     jvxlNSurfaceInts;
  
  void setVolumeData(VolumeData v) {
    nBytes = 0;
    volumetricOrigin = v.volumetricOrigin;
    volumetricVectors = v.volumetricVectors;
    voxelCounts = v.voxelCounts;
    voxelData = v.voxelData;
    volumeData = v;
  }
  
  Point3f volumetricOrigin;
  Vector3f[] volumetricVectors;
  int[] voxelCounts;
  float[][][] voxelData;

  void readData(boolean isMapData) {
    params.mappedDataMin = Float.MAX_VALUE;
  }
  
  ////////////////////////////////////////////////////////////////
  // CUBE/APBS/JVXL file reading stuff
  ////////////////////////////////////////////////////////////////

  int nBytes;
  int nDataPoints;
  int nPointsX, nPointsY, nPointsZ;

  private int edgeCount;
  
  boolean isJvxl;
  boolean isApbsDx;

  void jvxlUpdateInfo() {
    JvxlReader.jvxlUpdateInfo(jvxlData, params.title, nBytes);
  }

  boolean createIsosurface() {
    resetIsosurface();
    try {
      readData(false);
      generateSurfaceData();
    } catch (Exception e) {
      return false;
    }

    jvxlData.jvxlFileHeader = jvxlFileHeaderBuffer.toString();
    jvxlData.cutoff = (isJvxl ? jvxlCutoff : params.cutoff);
    jvxlData.jvxlColorData = "";
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
    
    if (jvxlDataIsColorMapped)
      jvxlData.jvxlColorData = readColorData();
    jvxlData.jvxlExtraLine = JvxlReader.jvxlExtraLine(jvxlData, 1);

    //if (thePlane != null && iAddGridPoints)
      //addGridPointCube();
    return true;
  }

  int contourVertexCount;
  
  void resetIsosurface() {
    meshData.clear("isosurface");
    contourVertexCount = 0;
    if (params.cutoff == Float.MAX_VALUE)
      params.cutoff = defaultCutoff;
    jvxlData.jvxlSurfaceData = "";
    jvxlData.jvxlEdgeData = "";
    jvxlData.jvxlColorData = "";
    edgeCount = 0;
  }

  void discardTempData(boolean discardAll) {
    voxelData = null;
    if (!discardAll)
      return;
    marchingSquares = null;
    marchingCubes = null;
  }
 
  static void setupMatrix(Matrix3f mat, Vector3f[] cols) {
    for (int i = 0; i < 3; i++)
      mat.setColumn(i, cols[i]);
  }


  void initializeVolumetricData() {
    nPointsX = voxelCounts[0];
    nPointsY = voxelCounts[1];
    nPointsZ = voxelCounts[2];
  }
  
  void readVoxelData(boolean isMapData) throws Exception {
    //overridden in every implementation.
    Logger.error("VoxelReader.readVoxelData has not been overridden!");
  }

  void gotoData(int n, int nPoints) throws Exception {
    //only for file reader
  }
  
  void readVolumetricData(boolean isMapData) {
    initializeVolumetricData();
    if (nPointsX <= 0 || nPointsY <= 0 || nPointsZ <= 0)
      return;
    try {
      gotoData(params.fileIndex - 1, nPointsX * nPointsY * nPointsZ);
      if (!isMapData && params.thePlane != null) {
        readPlaneData();
        return;
      }
      readVoxelData(isMapData);
    } catch (Exception e) {
      Logger.error(e.toString());
      throw new NullPointerException();
    }
  }

  /*
   * called by VolumeDataReader or VolumeFileReader
   * when there is a plane involved
   * 
   */
  void readPlaneData() throws Exception {
    boolean inside = false;
    int dataCount = 0; 
    volumeData.setPlaneParameters(params.thePlane);
    params.cutoff = 0f;
    voxelData = new float[nPointsX][][];
    nDataPoints = 0;
    float cutoff = params.cutoff;
    boolean isCutoffAbsolute = params.isCutoffAbsolute;
    for (int x = 0; x < nPointsX; ++x) {
      float[][] plane = new float[nPointsY][];
      voxelData[x] = plane;
      for (int y = 0; y < nPointsY; ++y) {
        float[] strip = new float[nPointsZ];
        plane[y] = strip;
        for (int z = 0; z < nPointsZ; ++z) {
          float voxelValue;
          voxelValue = volumeData.calcVoxelPlaneDistance(x, y, z);
          //if (nDataPoints < 100)
            //System.out.println("planedata "+nDataPoints + " " + params.thePlane+" "+voxelValue);
          strip[z] = voxelValue;
          ++nDataPoints;
          if (inside == isInside(voxelValue, cutoff, isCutoffAbsolute)) {
            dataCount++;
          } else {
            //if (dataCount != 0)
              //surfaceData += " " + dataCount;
            dataCount = 1;
            inside = !inside;
          }
        }
      }
    }
    jvxlData.jvxlSurfaceData = "";
    jvxlData.jvxlPlane = params.thePlane;
    volumeData.setVoxelData(voxelData);  
  }
  
  String readColorData() {
    //jvxl only -- overloaded
    return "";
  }
  
  ////////////////////////////////////////////////////////////////
  // marching cube stuff
  ////////////////////////////////////////////////////////////////

  MarchingSquares marchingSquares;
  MarchingCubes marchingCubes;

  private void generateSurfaceData() {
    fractionData = new StringBuffer();
    contourVertexCount = 0;
    int contourType = -1;
    MarchingSquares marchingSquares = null;
    if (params.thePlane != null || params.isContoured) {
      marchingSquares = new MarchingSquares(volumeData, params.thePlane,
          params.nContours, params.thisContour);
      contourType = marchingSquares.getContourType();
    }

    marchingCubes = new MarchingCubes(this, volumeData, params.isContoured, contourType,
        params.cutoff, params.isCutoffAbsolute);
    
    edgeCount = marchingCubes.generateSurfaceData(isXLowToHigh);
    
    if (isJvxl)
      fractionData = new StringBuffer(jvxlEdgeDataRead);
    fractionData.append('\n');
  }

  static boolean isInside(float voxelValue, float max, boolean isAbsolute) {
    return MarchingCubes.isInside(voxelValue, max, isAbsolute);
  }

  public void addTriangleCheck(int iA, int iB, int iC, int check, boolean isAbsolute) {
    if (!isAbsolute || checkCutoff(iA, iB, iC))
      meshData.addTriangleCheck(iA, iB, iC, check);
  }
  
  private boolean checkCutoff(int iA, int iB, int iC) {
    // never cross a +/- junction with a triangle in the case of orbitals, 
    // where we are using |psi| instead of psi for the surface generation.
    // note that for bicolor maps, where the values are all positive, we 
    // check this later in the meshRenderer
    if (iA < 0 || iB < 0 || iC < 0)
      return false;

    float val1 = meshData.vertexValues[iA];
    float val2 = meshData.vertexValues[iB];
    float val3 = meshData.vertexValues[iC];

    return (val1 >= 0 && val2 >= 0 && val3 >= 0 
        || val1 <= 0 && val2 <= 0 && val3 <= 0);
  }

  public int getSurfacePointIndex(float cutoff, boolean isCutoffAbsolute,
                                  int x, int y, int z, Point3i offset,
                                  float valueA, float valueB, Point3f pointA,
                                  Vector3f edgeVector, Point3f surfacePoint,
                                  boolean isContourType) {

    float thisValue = readSurfacePoint(cutoff, isCutoffAbsolute, valueA,
        valueB, pointA, edgeVector, surfacePoint);
    /* 
     * In the case of a Marching Squares calculation,
     * we are collecting just the desired type of intersection for the 2D marching
     * square contouring -- x, y, or z. In the case of a contoured f(x,y) surface, 
     * we take every point.
     * 
     */
    return (marchingSquares == null ? meshData.addVertexCopy(surfacePoint,
        thisValue) : isContourType ? marchingSquares.addContourData(x, y, z,
        offset, surfacePoint, cutoff) : Integer.MAX_VALUE);
  }
  
  float readSurfacePoint(float cutoff, boolean isCutoffAbsolute, float valueA,
                         float valueB, Point3f pointA, Vector3f edgeVector,
                         Point3f surfacePoint) {

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
    if (!isJvxl)
      fractionData.append(JvxlReader.jvxlFractionAsCharacter(fraction,
          edgeFractionBase, edgeFractionRange));

    thisValue = valueA + fraction * diff;
    surfacePoint.scaleAdd(fraction, edgeVector, pointA);
    return thisValue;
  }
  
  ////////// debug utility methods /////////
/*
  String dumpArray(String msg, float[][] A, int x1, int x2, int y1, int y2) {
    String s = "dumpArray: " + msg + "\n";
    for (int x = x1; x <= x2; x++)
      s += "\t*" + x + "*";
    for (int y = y2; y >= y1; y--) {
      s += "\n*" + y + "*";
      for (int x = x1; x <= x2; x++)
        s += "\t" + (x < A.length && y < A[x].length ? A[x][y] : Float.NaN);
    }
    return s;
  }

  String dumpIntArray(int[] A, int n) {
    String str = "";
    for (int i = 0; i < n; i++)
      str += " " + A[i];
    return str;
  }
*/
  
  
  ////////////////////////////////////////////////////////////////
  // color mapping methods
  ////////////////////////////////////////////////////////////////

  void colorIsosurface() {
    if (params.isContoured && !(jvxlDataIs2dContour || params.thePlane != null)) {
      Logger.error("Isosurface error: Cannot contour this type of data.");
      return;
    }
    if (params.isContoured)
      marchingSquares.generateContourData(jvxlDataIs2dContour, meshData);
    applyColorScale();
    jvxlData.nContours = params.nContours;
    jvxlData.jvxlExtraLine = JvxlReader.jvxlExtraLine(jvxlData,1);
    
    jvxlData.jvxlFileMessage = "mapped: min = " + params.valueMappedToRed + "; max = "
        + params.valueMappedToBlue;
  }

  void applyColorScale() {
    if (params.colorPhase == 0)
      params.colorPhase = 1;
    int vertexCount = meshData.vertexCount;
    int[] colors = meshData.vertexColors;
    colorFractionBase = defaultColorFractionBase;
    colorFractionRange = defaultColorFractionRange;
    setMapRanges();
    float min = params.mappedDataMin;
    float max = params.mappedDataMax;
    if (colors == null)
      meshData.vertexColors = colors = new int[vertexCount];
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
    boolean writePrecisionColor = (jvxlDataIsPrecisionColor || params.isContoured);
    int lastVertex = (contourVertexCount > 0 ? contourVertexCount : vertexCount);
    for (int i = 0; i < vertexCount; i += incr) {
      float value = getVertexColorValue(i);
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
  }

  private void setMapRanges() {
    params.setMapRanges(this);
  }

  private float getVertexColorValue(int vertexIndex) {
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
    if (params.isBicolorMap && !params.isContoured || params.colorBySign) {
      if (value <= 0)
        meshData.vertexColors[vertexIndex] = params.minColor;
      else if (value > 0)
        meshData.vertexColors[vertexIndex] = params.maxColor;
      if (!params.isContoured)
        datum = (value > 0 ? 0.999f : -0.999f);
    } else {
      if (value < params.valueMappedToRed)
        value = params.valueMappedToRed;
      if (value >= params.valueMappedToBlue)
        value = params.valueMappedToBlue;
      meshData.vertexColors[vertexIndex] = getColorFromPalette(value);
    }
    return datum;
  }

  final static String[] colorPhases = { "_orb", "x", "y", "z", "xy", "yz",
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
  
  float getPhase(Point3f pt) {
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

  final Vector3f pointVector = new Vector3f();

  int getColorFromPalette(float value) {
    if (params.isColorReversed)
      return colorEncoder.getColorFromPalette(-value, -params.valueMappedToBlue,
          -params.valueMappedToRed);
    return colorEncoder.getColorFromPalette(value, params.valueMappedToRed,
        params.valueMappedToBlue);
  }
  

}


