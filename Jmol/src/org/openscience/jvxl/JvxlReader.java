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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


/*
 
 * The JVXL file format
 * --------------------
 * 
 * as of 3/29/07 this code is COMPLETELY untested. It was hacked out of the
 * Jmol code, so there is probably more here than is needed.
 * 
 * 
 * 
 * see http://www.stolaf.edu/academics/chemapps/jmol/docs/misc/JVXL-format.pdf
 *
 * The JVXL (Jmol VoXeL format) is a file format specifically designed
 * to encode an isosurface or planar slice through a set of 3D scalar values
 * in lieu of a that set. A JVXL file can contain coordinates, and in fact
 * it must contain at least one coordinate, but additional coordinates are
 * optional. The file can contain any finite number of encoded surfaces. 
 * However, the compression of 300-500:1 is based on the reduction of the 
 * data to a SINGLE surface. 
 * 
 * 
 * The original Marching Cubes code was written by Miguel Howard in 2005.
 * The classes Parser, ArrayUtil, and TextFormat are condensed versions
 * of the classes found in org.jmol.util
 * 
 * All code relating to JVXL format is copyrighted 2006/2007 and invented by 
 * Robert M. Hanson, 
 * Professor of Chemistry, 
 * St. Olaf College, 
 * 1520 St. Olaf Ave.
 * Northfield, MN. 55057.
 * 
 * Implementations of the JVXL format should reference 
 * "Robert M. Hanson, St. Olaf College" and the opensource Jmol project.
 * 
 * 
 * implementing marching squares; see 
 * http://www.secam.ex.ac.uk/teaching/ug/studyres/COM3404/COM3404-2006-Lecture15.pdf
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
 * 
 * 
 * THIS READER
 * -----------
 * 
 * This is a first attempt at a generic JVXL file reader and writer class.
 * It is an extraction of Jmol org.jmol.viewer.Isosurface.Java and related pieces.
 * 
 * The goal of the reader is to be able to read CUBE-like data and 
 * convert that data to JVXL file data.
 * 
 * 
 */

package org.openscience.jvxl;

import java.io.BufferedReader;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

class JvxlReader {

  JvxlReader() {
    initializeIsosurface();
  }
  
  int state;
  MeshData meshData;
  JvxlData jvxlData;
  VolumeData volumeData;
  
  int fileIndex; //one-based

  final static int STATE_INITIALIZED = 1;
  final static int STATE_DATA_READ = 2;
  final static int STATE_DATA_COLORED = 3;

  final static float ANGSTROMS_PER_BOHR = 0.5291772f;

  final static int defaultEdgeFractionBase = 35; //#$%.......
  final static int defaultEdgeFractionRange = 90;
  final static int defaultColorFractionBase = 35;
  final static int defaultColorFractionRange = 90;
  final static float defaultMappedDataMin = 0f;
  final static float defaultMappedDataMax = 1.0f;
  final static float defaultCutoff = 0.02f;
  final static int nContourMax = 100;
  final static int defaultContourCount = 11; //odd is better

  int dataType;
  boolean blockCubeData;
  int nSurfaces;
  boolean isAngstroms;
  boolean haveVoxelData;
    
  int edgeFractionBase;
  int edgeFractionRange;
  int colorFractionBase;
  int colorFractionRange;
  float mappedDataMin;
  float mappedDataMax;

  final float[] volumetricVectorLengths = new float[3];
  final Matrix3f volumetricMatrix = new Matrix3f();

  boolean insideOut;
  float cutoff = Float.MAX_VALUE;
  int nContours;
  int thisContour;
  boolean rangeDefined;
  float valueMappedToRed, valueMappedToBlue;

  boolean isColorReversed;

  Point4f thePlane;
  boolean isContoured;
  boolean isBicolorMap;
  boolean isCutoffAbsolute;
  boolean isPositiveOnly;
  
  boolean colorBySign;
  boolean colorByPhase;
  int colorNeg;
  int colorPos;
  int colorPtr;
  int colorPhase;
  float resolution;

  private BufferedReader br;
  private String[] title;
    
  Point3f volumetricOrigin;
  Vector3f[] volumetricVectors;
  int[] voxelCounts;
  float[][][] voxelData;

  final Vector3f[] unitVolumetricVectors = new Vector3f[3];
  {
    unitVolumetricVectors[0] = new Vector3f();
    unitVolumetricVectors[1] = new Vector3f();
    unitVolumetricVectors[2] = new Vector3f();
  }
  
  void setProperty(String propertyName, Object value) {

    
    if ("init" == propertyName) {
      initializeIsosurface();
      return;
    }
    
    if ("fileIndex" == propertyName) {
      fileIndex = ((Integer) value).intValue();
      if (fileIndex < 1)
        fileIndex = 1;
      return;
    }

    if ("title" == propertyName) {
      if (value == null) {
        title = null;
        return;
      } else if (value instanceof String[]) {
        title = (String[]) value;
      }
      return;
    }

    if ("cutoff" == propertyName) {
      cutoff = ((Float) value).floatValue();
      isPositiveOnly = false;
      return;
    }

    if ("cutoffPositive" == propertyName) {
      cutoff = ((Float) value).floatValue();
      isPositiveOnly = true;
      return;
    }

    /// color options 

    if ("insideOut" == propertyName) {
      insideOut = true;
      return;
    }

    if ("sign" == propertyName) {
      isCutoffAbsolute = true;
      colorBySign = true;
      colorPtr = 0;
      return;
    }

    if ("red" == propertyName) {
      valueMappedToRed = ((Float) value).floatValue();
      return;
    }

    if ("blue" == propertyName) {
      valueMappedToBlue = ((Float) value).floatValue();
      rangeDefined = true;
      return;
    }

    if ("reverseColor" == propertyName) {
      isColorReversed = true;
      return;
    }

    if ("plane" == propertyName) {
      thePlane = (Point4f) value;
      isContoured = true;
      ++state;
      return;
    }

    if ("readData" == propertyName) {
      if (++state != STATE_DATA_READ)
        return;
      meshData = new MeshData();
      jvxlData = new JvxlData();
      if (!setData(value)) {
        Logger.error("Could not set the data");
        return;
      }
      if (!createIsosurface()) {
        Logger.error("Could not create isosurface");
        return;
      }
      jvxlFileMessage = (jvxlDataIsColorMapped ? "mapped" : "");
      if (isContoured && thePlane == null) {
        planarVectors[0].set(volumetricVectors[0]);
        planarVectors[1].set(volumetricVectors[1]);
        pixelCounts[0] = voxelCounts[0];
        pixelCounts[1] = voxelCounts[1];
      }
      jvxlData.nBytes = nBytes;
      jvxlUpdateInfo();
      discardTempData(false);
      mappedDataMin = Float.MAX_VALUE;
      return;
    }

    if ("mapColor" == propertyName) {
      if (++state != STATE_DATA_COLORED)
        return;
      if (!setData(value)) {
        Logger.error("Could not set the mapping data");
        return;
      }
      if (thePlane != null) {
        createIsosurface(); //for the plane
        readVolumetricData(true); //for the data
        colorIsosurface();
      } else {
        readData(true);
        if (jvxlDataIsColorMapped) {
          jvxlReadColorData();
        } else {
          colorIsosurface();
        }
      }
      jvxlData.nBytes = nBytes;
      jvxlUpdateInfo();
      discardTempData(true);
      return;
    }
  }

  Object getProperty(String property, int index) {
    if (property == "plane")
      return (jvxlData.jvxlPlane);
    if (property == "jvxlFileData")
      return jvxlGetFile("", true, index);
    if (property == "jvxlFileInfo")
      return jvxlData.jvxlInfoLine;
    if (property == "jvxlSurfaceData")
      return jvxlGetFile("", false, 1);
    if (property == "volumeData")
      return volumeData;
    if (property == "meshData")
      return meshData;
    return null;
  }

  boolean setData(Object value) {
    if (value instanceof VolumeData) {
      setVolumeData((VolumeData)value);
      haveVoxelData = true;
      //from loadData;
    } else if (value instanceof BufferedReader) {
      setVolumeData(new VolumeData());
      br = (BufferedReader) value;
    } else if (value instanceof String) {
      setVolumeData(new VolumeData());
      Object t = (new FileReader())
          .getBufferedReaderOrErrorMessageFromName((String) value);
      if (t instanceof String) {
        Logger.error((String) t);
        return false;
      }
      br = (BufferedReader) t;
    } 
    return true;
  }
  
  void setVolumeData(VolumeData v) {
    volumetricOrigin = v.volumetricOrigin;
    volumetricVectors = v.volumetricVectors;
    voxelCounts = v.voxelCounts;
    voxelData = v.voxelData;
    volumeData = v;
  }
  
  void initializeIsosurface() {
    blockCubeData = false; // Gaussian standard, but we allow for multiple surfaces one per data block
    isColorReversed = false;
    haveVoxelData = false;
    colorBySign = colorByPhase = false;
    resolution = Float.MAX_VALUE;
    //anisotropy[0] = anisotropy[1] = anisotropy[2] = 1f;
    cutoff = Float.MAX_VALUE;
    thePlane = null;
    //surface_data = null;
    nBytes = 0;
    nContours = 0;
    colorPtr = 0;
    thisContour = -1;
    isContoured = false;
    rangeDefined = false;
    mappedDataMin = Float.MAX_VALUE;
    isBicolorMap = isCutoffAbsolute = isPositiveOnly = false;
    jvxlInitFlags();
    initState();
  }

  void initState() {
    state = STATE_INITIALIZED;
  }

  boolean createIsosurface() {
    resetIsosurface();
    try {
      readData(false);
      calcVoxelVertexVectors();
      generateSurfaceData();
    } catch (Exception e) {
      return false;
    }
    jvxlData.jvxlFileHeader = "" + jvxlFileHeader;
    jvxlData.cutoff = (isJvxl ? jvxlCutoff : cutoff);
    jvxlData.jvxlColorData = "";
    jvxlData.jvxlEdgeData = "" + fractionData;
    jvxlData.isBicolorMap = isBicolorMap;
    jvxlData.isContoured = isContoured;
    jvxlData.nContours = nContours;
    if (jvxlDataIsColorMapped)
      jvxlReadColorData();
    jvxlData.jvxlExtraLine = jvxlExtraLine(1);
    //if (thePlane != null && iAddGridPoints)
      //addGridPointCube();
    return true;
  }

  void resetIsosurface() {
    meshData.clear("isosurface");
    contourVertexCount = 0;
    if (cutoff == Float.MAX_VALUE)
      cutoff = defaultCutoff;
    jvxlData.jvxlSurfaceData = "";
    jvxlData.jvxlEdgeData = "";
    jvxlData.jvxlColorData = "";
    edgeCount = 0;
  }

  boolean isJvxl, isApbsDx;
  boolean endOfData;

  void readData(boolean isMapData) {
    isJvxl = false;
    isApbsDx = false;
    endOfData = false;
    mappedDataMin = Float.MAX_VALUE;
    nSurfaces = readVolumetricHeader();
    if (nSurfaces < fileIndex) {
      Logger.warn("not enough surfaces in file -- resetting fileIndex to "
          + nSurfaces);
      fileIndex = nSurfaces;
    }
    if (isJvxl && isMapData)
      try {
        int nPoints = nPointsX * nPointsY * nPointsZ;
        gotoData(fileIndex - 1, nPoints);
        jvxlSkipData(nPoints, false);
      } catch (Exception e) {
        Logger.error(e.toString());
        throw new NullPointerException();
      }
    else
      readVolumetricData(isMapData);
  }

  void discardTempData(boolean discardAll) {
    voxelData = null;
    try {
      br.close();
    } catch (Exception e) {
    }
    if (!discardAll)
      return;
    pixelData = null;
    planarSquares = null;
    contourVertexes = null;
    contourVertexCount = 0;
  } ////////////////////////////////////////////////////////////////

  // default color stuff
  ////////////////////////////////////////////////////////////////

  int indexColorPositive;
  int indexColorNegative;

  ////////////////////////////////////////////////////////////////
  // CUBE/JVXL file reading stuff
  ////////////////////////////////////////////////////////////////

  int readVolumetricHeader() {
    try {
        readTitleLines();
        readAtomCountAndOrigin();
        for (int i = 0; i < 3; ++i) {
          readVoxelVector(i);
        }
      setupMatrix(volumetricMatrix, volumetricVectors);
      readAtoms();
      return readExtraLine();
    } catch (Exception e) {
      Logger.error(e.toString());
      throw new NullPointerException();
    }
  }

  void readVolumetricData(boolean isMapData) {
    try {
      readVoxelData(isMapData);
      if (isJvxl && jvxlEdgeDataCount > 0)
        jvxlEdgeDataRead = jvxlReadData("edge", jvxlEdgeDataCount);
      if (isJvxl && jvxlColorDataCount > 0)
        jvxlColorDataRead = jvxlReadData("color", jvxlColorDataCount);

    } catch (Exception e) {
      Logger.error(e.toString());
      throw new NullPointerException();
    }
  }

  StringBuffer jvxlFileHeader = new StringBuffer();
  String jvxlFileMessage;
  String jvxlEdgeDataRead;
  String jvxlColorDataRead;

  void readTitleLines() throws Exception {
    jvxlFileHeader = new StringBuffer();
    jvxlFileHeader.append(br.readLine());
    jvxlFileHeader.append('\n');
    skipComments(true);
    isApbsDx = (line.indexOf("object 1 class gridpositions counts") == 0);
    if (isApbsDx) {
      line = "APBS OpenDx DATA: " + line + " see http://apbs.sourceforge.net";
      isAngstroms = true;
    }
    jvxlFileHeader.append(line);
    jvxlFileHeader.append('\n');
  }

  int atomCount;
  boolean negativeAtomCount;

  void readAtomCountAndOrigin() throws Exception {
    String atomLine;
    skipComments(true);
    atomCount = parseInt(line);
    if (atomCount == Integer.MIN_VALUE) { //unreadable
      next[0] = line.indexOf(" ");
      atomCount = 0;
    }
    atomLine = line.substring(next[0]);
    if (isAngstroms)
      atomLine += " ANGSTROMS";
    else if (atomLine.indexOf("ANGSTROMS") >= 0)
      isAngstroms = true;

    negativeAtomCount = (atomCount < 0);
    if (negativeAtomCount)
      atomCount = -atomCount;

    int jvxlAtoms = (atomCount == 0? -2 : -atomCount);
    volumetricOrigin.set(parseFloat(), parseFloat(), parseFloat());
    if (!isAngstroms)
      volumetricOrigin.scale(ANGSTROMS_PER_BOHR);
    jvxlFileHeader.append(jvxlAtoms + atomLine + '\n');
  }

  void readVoxelVector(int voxelVectorIndex) throws Exception {
    
    line = br.readLine();
    if (isApbsDx) {
      line = "%dx" + voxelVectorIndex + line;      
      /* see http://apbs.sourceforge.net/doc/user-guide/index.html#opendx-format
       * 
          delta hx 0.0 0.0
          delta 0.0 hy 0.0 
          delta 0.0 0.0 hz
       */
    }
    jvxlFileHeader.append(line);
    jvxlFileHeader.append('\n');
    Vector3f voxelVector = volumetricVectors[voxelVectorIndex];
    if ((voxelCounts[voxelVectorIndex] = parseInt(line)) == Integer.MIN_VALUE) //unreadable
      next[0] = line.indexOf(" ");
    voxelVector.set(parseFloat(), parseFloat(), parseFloat());
    if (!isAngstroms)
      voxelVector.scale(ANGSTROMS_PER_BOHR);
    volumetricVectorLengths[voxelVectorIndex] = voxelVector.length();

    unitVolumetricVectors[voxelVectorIndex].normalize(voxelVector);
    for (int i = 0; i < voxelVectorIndex; i++) {
      float orthoTest = Math.abs(unitVolumetricVectors[i]
          .dot(unitVolumetricVectors[voxelVectorIndex]));
      if (orthoTest > 1.001 || orthoTest < 0.999 && orthoTest > 0.001)
        Logger.warn("Warning: voxel coordinate vectors are not orthogonal.");
    }
    if (voxelVectorIndex == 2 && isApbsDx) {
      line = br.readLine();
      String[] tokens = getTokens();
      /* see http://apbs.sourceforge.net/doc/user-guide/index.html#opendx-format
       object 2 class gridconnections counts nx ny nz
       object 3 class array type double rank 0 times n data follows
       * 
       */
      String s = jvxlFileHeader.toString();
      s = TextFormat.simpleReplace(s, "%dx0delta", "" + (voxelCounts[0] = parseInt(tokens[5])));
      s = TextFormat.simpleReplace(s, "%dx1delta", "" + (voxelCounts[1] = parseInt(tokens[6])));
      s = TextFormat.simpleReplace(s, "%dx2delta", "" + (voxelCounts[2] = parseInt(tokens[7])));
      jvxlFileHeader = new StringBuffer(s);
      br.readLine();
    }
  }

  void setupMatrix(Matrix3f mat, Vector3f[] cols) {
    for (int i = 0; i < 3; i++)
      mat.setColumn(i, cols[i]);
  }

  void readAtoms() throws Exception {
    for (int i = 0; i < atomCount; ++i)
      jvxlFileHeader.append(br.readLine() + "\n");
    if (atomCount == 0) {
      Point3f pt = new Point3f(volumetricOrigin);
      jvxlFileHeader.append("1 1.0 " + pt.x + " " + pt.y + " " + pt.z
          + " //BOGUS H ATOM ADDED FOR JVXL FORMAT\n");
      for (int i = 0; i < 3; i++)
        pt.scaleAdd(voxelCounts[i] - 1, volumetricVectors[i], pt);
      jvxlFileHeader.append("2 2.0 " + pt.x + " " + pt.y + " " + pt.z
          + " //BOGUS He ATOM ADDED FOR JVXL FORMAT\n");
    }
  }

  int readExtraLine() throws Exception {
    edgeFractionBase = defaultEdgeFractionBase;
    edgeFractionRange = defaultEdgeFractionRange;
    colorFractionBase = defaultColorFractionBase;
    colorFractionRange = defaultColorFractionRange;
    if (!negativeAtomCount)
      return 1;
    line = br.readLine();
    Logger.info("Reading extra orbital/JVXL information line: " + line);
    int nSurfaces = parseInt(line);
    if (!(isJvxl = (nSurfaces < 0)))
      return nSurfaces;
    nSurfaces = -nSurfaces;
    Logger.info("jvxl file surfaces: " + nSurfaces);
    int ich;
    if ((ich = parseInt()) == Integer.MIN_VALUE) {
      Logger.info("using default edge fraction base and range");
    } else {
      edgeFractionBase = ich;
      edgeFractionRange = parseInt();
    }
    if ((ich = parseInt()) == Integer.MIN_VALUE) {
      Logger.info("using default color fraction base and range");
    } else {
      colorFractionBase = ich;
      colorFractionRange = parseInt();
    }
    return nSurfaces;
  }

  int nBytes;
  int nDataPoints;
  String surfaceData;
  int nPointsX, nPointsY, nPointsZ;

  void readVoxelData(boolean isMapData) throws Exception {
    /*
     * possibilities:
     * 
     * cube file data only -- monochrome surface (single pass)
     * cube file with plane (color, two pass)
     * cube file data + cube file color data (two pass)
     * jvxl file no color data (single pass)
     * jvxl file with color data (single pass)
     * jvxl file with plane (single pass)
     * 
     * cube file with multiple MO data will be interspersed 
     * 
     * 
     */
    /* 
     * This routine is used twice in the case of color mapping. 
     * First (isMapData = false) to read the surface values, which
     * might be a plane, then (isMapData = true) to color them based 
     * on a second data set.
     * 
     * Planes are only compatible with data sets that return actual 
     * numbers at all grid points -- cube files, orbitals, functionXY -- 
     * not solvent, spheres, elipsoids, or lobes.
     * 
     * It is possible to map a QM orbital onto a plane. In the first pass we defined
     * the plane; in the second pass we just calculate the new voxel values and return.
     * 
     */
    boolean inside = false;
    int dataCount = 0;
    next[0] = 0;
    nThisValue = 0;
    surfaceData = "";
    nPointsX = voxelCounts[0];
    nPointsY = voxelCounts[1];
    nPointsZ = voxelCounts[2];
    int nPoints = nPointsX * nPointsY * nPointsZ;
    if (nPointsX <= 0 || nPointsY <= 0 || nPointsZ <= 0)
      return;
    // skip to the correct dataset and read the JVXL definition line if present
    // several JVXL variables and the plane will be defined here
    gotoData(fileIndex - 1, nPoints);

    thisInside = (!isJvxl || !isContoured);
    if (insideOut)
      thisInside = !thisInside;

    if (thePlane != null) {
      setPlaneParameters(thePlane);
      cutoff = 0f;
    } else if (isJvxl) {
      cutoff = (isBicolorMap || colorBySign ? 0.01f : 0.5f);
    }
    boolean justDefiningPlane = (!isMapData && thePlane != null);
    boolean isPrecalculation = (haveVoxelData && !justDefiningPlane);
    
    if (isPrecalculation) {
      voxelData = volumeData.voxelData;
      if (isMapData || thePlane != null)
        return;
    } else {
      voxelData = new float[nPointsX][][];
    }
    nDataPoints = 0;
    line = "";
    for (int x = 0; x < nPointsX; ++x) {
      float[][] plane;
      if (isPrecalculation) {
        plane = voxelData[x];
      } else {
        plane = new float[nPointsY][];
        voxelData[x] = plane;
      }
      for (int y = 0; y < nPointsY; ++y) {
        float[] strip;
        if (isPrecalculation) {
          strip = plane[y];
        } else {
          strip = new float[nPointsZ];
          plane[y] = strip;
        }
        for (int z = 0; z < nPointsZ; ++z) {
          float voxelValue;
          if (justDefiningPlane) {
            voxelValue = calcVoxelPlaneDistance(x, y, z);
          } else {
            if (isPrecalculation)
              voxelValue = strip[z]; //precalculated
            else
              voxelValue = getNextVoxelValue();
          }
          strip[z] = voxelValue;
          ++nDataPoints;
          if (isJvxl && thePlane == null || isMapData)
            continue;

          // update surfaceData

          if (inside == isInside(voxelValue, cutoff)) {
            dataCount++;
          } else {
            if (dataCount != 0)
              surfaceData += " " + dataCount;
            dataCount = 1;
            inside = !inside;
          }
        }
      }
    }
    if (!isJvxl)
      surfaceData += " " + dataCount + "\n";
    if (!isMapData) {
      jvxlData.jvxlSurfaceData = (thePlane == null ? surfaceData : "");
      jvxlData.jvxlPlane = thePlane;
    }
    volumeData.setVoxelData(voxelData);
  }

  final Vector3f thePlaneNormal = new Vector3f();
  float thePlaneNormalMag;

  void setPlaneParameters(Point4f plane) {
    if (plane.x == 0 && plane.y == 0 && plane.z == 0)
      plane.z = 1; //{0 0 0 w} becomes {0 0 1 w}
    thePlaneNormal.set(plane.x, plane.y, plane.z);
    thePlaneNormalMag = thePlaneNormal.length();
  }

  final Point3f ptXyzTemp = new Point3f();

  float calcVoxelPlaneDistance(int x, int y, int z) {
    voxelPtToXYZ(x, y, z, ptXyzTemp);
    return distancePointToPlane(ptXyzTemp, thePlane);
  }

  float distancePointToPlane(Point3f pt, Point4f plane) {
    return (plane.x * pt.x + plane.y * pt.y + plane.z * pt.z + plane.w)
        / thePlaneNormalMag;
  }

  int jvxlSurfaceDataCount;
  int jvxlEdgeDataCount;
  int jvxlColorDataCount;
  boolean jvxlDataIsColorMapped;
  //boolean jvxlDataisBicolorMap;
  boolean jvxlDataIsPrecisionColor;
  boolean jvxlWritePrecisionColor;
  boolean jvxlDataIs2dContour;
  float jvxlCutoff;

  void jvxlInitFlags() {
    jvxlEdgeDataRead = "";
    jvxlColorDataRead = "";
    jvxlDataIs2dContour = false;
    jvxlDataIsColorMapped = false;
    jvxlDataIsPrecisionColor = false;
    //jvxlDataisBicolorMap = false;
    jvxlWritePrecisionColor = false;
  }

  void skipComments(boolean addToHeader) throws Exception {
    while ((line = br.readLine()) != null && (line.length() == 0
        || line.charAt(0) == '#')) {
      if (addToHeader) {
        jvxlFileHeader.append(line);
        jvxlFileHeader.append('\n');
      }
    }
  }
  void jvxlReadDefinitionLine(boolean showMsg) throws Exception {
    skipComments(false);
    if (showMsg)
      Logger.info("reading jvxl data set: " + line);

    jvxlCutoff = parseFloat(line);
    Logger.info("JVXL read: cutoff " + jvxlCutoff);

    // cutoff        param1              param2         param3
    //                 |                   |              |
    //   when          |                   |        >  0 ==> 1-byte jvxlDataIsColorMapped
    //   when          |                   |       == -1 ==> not color mapped
    //   when          |                   |        < -1 ==> 2-byte jvxlDataIsPrecisionColor    
    //   when        == -1     &&   == -1 ==> noncontoured plane
    //   when        == -1     &&   == -2 ==> contourable plane
    //   when        < -1      &&    >  0 ==> contourable functionXY
    //   when        > 0       &&    <  0 ==> jvxlDataisBicolorMap

    // early on I wasn't contouring planes, so it's possible that a plane would
    // not be contoured (-1 -1), but that is NOT a possibility anymore with Jmol.
    // instead, we just set "contour 1" to indicate just one contour to demo that.
    // In addition, now we consider contouring functionXY, so in that case we would
    // have surface data, edge data, and color data

    int param1 = parseInt();
    int param2 = parseInt();
    int param3 = parseInt();
    if (param3 == Integer.MIN_VALUE || param3 == -1)
      param3 = 0;

    if (param1 == -1) {
      // a plane is defined
      try {
        thePlane = new Point4f(parseFloat(), parseFloat(), parseFloat(),
            parseFloat());
      } catch (Exception e) {
        Logger
            .error("Error reading 4 floats for PLANE definition -- setting to 0 0 1 0  (z=0)");
        thePlane = new Point4f(0, 0, 1, 0);
      }
      Logger.info("JVXL read: {" + thePlane.x + " " + thePlane.y + " "
          + thePlane.z + " " + thePlane.w + "}");
    } else {
      thePlane = null;
    }
    if (param1 < 0 && param2 != -1) {
      isContoured = (param3 != 0);
      // contours are defined (possibly overridden -- this is just a display option
      // could be plane or functionXY
      int nContoursRead = parseInt();
      if (nContours == 0 && nContoursRead != Integer.MIN_VALUE
          && nContoursRead != 0 && nContoursRead <= nContourMax) {
        nContours = nContoursRead;
        Logger.info("JVXL read: contours " + nContours);
      }
    } else {
      isContoured = false;
    }

    jvxlDataIsPrecisionColor = (param1 == -1 && param2 == -2 || param3 < 0);
    isBicolorMap = (param1 > 0 && param2 < 0);
    jvxlDataIsColorMapped = (param3 != 0);
    jvxlDataIs2dContour = (jvxlDataIsColorMapped && isContoured);

    if (isBicolorMap || colorBySign)
      jvxlCutoff = 0;
    jvxlSurfaceDataCount = (param1 < -1 ? -param1 : param1 > 0 ? param1 : 0);
    if (param1 == -1)
      jvxlEdgeDataCount = 0; //plane
    else
      jvxlEdgeDataCount = (param2 < -1 ? -param2 : param2 > 0 ? param2 : 0);
    jvxlColorDataCount = (isBicolorMap ? -param2 : param3 < -1 ? -param3 : param3 > 0 ? param3 : 0);
    if (jvxlDataIsColorMapped) {
      float dataMin = parseFloat();
      float dataMax = parseFloat();
      float red = parseFloat();
      float blue = parseFloat();
      if (!Float.isNaN(dataMin) && !Float.isNaN(dataMax)) {
        if (dataMax == 0 && dataMin == 0) {
          //set standard -1/1; bit of a hack
          dataMin = -1;
          dataMax = 1;
        }
        mappedDataMin = dataMin;
        mappedDataMax = dataMax;
        Logger.info("JVXL read: data min/max: " + mappedDataMin + "/"
            + mappedDataMax);
      }
      if (!rangeDefined)
        if (!Float.isNaN(red) && !Float.isNaN(blue)) {
          if (red == 0 && blue == 0) {
            //set standard -1/1; bit of a hack
            red = -1;
            blue = 1;
          }
          valueMappedToRed = red;
          valueMappedToBlue = blue;
          rangeDefined = true;
        } else {
          valueMappedToRed = 0f;
          valueMappedToBlue = 1f;
          rangeDefined = true;
        }
      Logger.info("JVXL read: color red/blue: " + valueMappedToRed + " "
          + valueMappedToBlue);
    }
  }

  int nThisValue;
  boolean thisInside;

  float getNextVoxelValue() throws Exception {
    if (isJvxl) {
      if (jvxlSurfaceDataCount <= 0)
        return 0f; //unnecessary -- probably a plane
      if (nThisValue == 0) {
        nThisValue = parseInt();
        if (nThisValue == Integer.MIN_VALUE) {
          line = br.readLine();
          if (line == null
              || (nThisValue = parseInt(line)) == Integer.MIN_VALUE) {
            if (!endOfData)
              Logger.error("end of file in JvxlReader?" + " line=" + line);
            endOfData = true;
            nThisValue = 10000;
            //throw new NullPointerException();
          } else {
            surfaceData += line + "\n";
          }
        }
        thisInside = !thisInside;
      }
      --nThisValue;
      return (thisInside ? 1f : 0f);
    }
    float voxelValue = 0;
    if (nSurfaces > 1 && !blockCubeData) {
      for (int i = 1; i < fileIndex; i++)
        nextVoxel();
      voxelValue = nextVoxel();
      for (int i = fileIndex; i < nSurfaces; i++)
        nextVoxel();
    } else {
      voxelValue = nextVoxel();
    }
    return voxelValue;
  }

  float nextVoxel() throws Exception {
    float voxelValue = parseFloat();
    if (Float.isNaN(voxelValue)) {
      while ((line = br.readLine()) != null
          && Float.isNaN(voxelValue = parseFloat(line))) {
      }
      if (line == null) {
        if (!endOfData)
          Logger.warn("end of file reading cube voxel data? nBytes=" + nBytes
              + " nDataPoints=" + nDataPoints + " (line):" + line);
        endOfData = true;
        line = "0 0 0 0 0 0 0 0 0 0";
      }
      nBytes += line.length() + 1;
    }
    return voxelValue;
  }

  void gotoData(int n, int nPoints) throws Exception {
    if (n > 0)
      Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
    for (int i = 0; i < n; i++)
      if (isJvxl) {
        jvxlReadDefinitionLine(true);
        Logger.info("JVXL skipping: jvxlSurfaceDataCount="
            + jvxlSurfaceDataCount + " jvxlEdgeDataCount=" + jvxlEdgeDataCount
            + " jvxlDataIsColorMapped=" + jvxlDataIsColorMapped);
        jvxlSkipData(nPoints, true);
      } else if (blockCubeData) {
        skipData(nPoints, true);
      }
    if (isJvxl)
      jvxlReadDefinitionLine(true);
  }

  void jvxlSkipData(int nPoints, boolean doSkipColorData) throws Exception {
    if (jvxlSurfaceDataCount > 0)
      skipData(nPoints, true);
    if (jvxlEdgeDataCount > 0)
      skipData(jvxlEdgeDataCount, false);
    if (jvxlDataIsColorMapped && doSkipColorData)
      skipData(jvxlColorDataCount, false);
  }

  void skipData(int nPoints, boolean isInt) throws Exception {
    int iV = 0;
    while (iV < nPoints) {
      line = br.readLine();
      iV += (isInt ? countData(line) : jvxlUncompressString(line).length());
    }
  }

  String jvxlReadData(String type, int nPoints) {
    String str = "";
    try {
      while (str.length() < nPoints) {
        line = br.readLine();
        str += jvxlUncompressString(line);
      }
    } catch (Exception e) {
      Logger.error("Error reading " + type + " data " + e);
      throw new NullPointerException();
    }
    return str;
  }

  int countData(String str) {
    int count = 0;
    if (isJvxl) {
      int n = parseInt(str);
      while (n != Integer.MIN_VALUE) {
        count += n;
        n = parseIntNext(str);
      }
      return count;
    }
    int ich = 0;
    int ichMax = str.length();
    char ch;
    while (ich < ichMax) {
      while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
        ++ich;
      if (ich < ichMax)
        ++count;
      while (ich < ichMax && ((ch = str.charAt(ich)) != ' ' && ch != '\t'))
        ++ich;
    }
    return count;
  }

  ////////////////////////////////////////////////////////////////
  // associated vertex normalization
  ////////////////////////////////////////////////////////////////

  int addVertexCopy(Point3f vertex, float value) {
    int vPt = meshData.addVertexCopy(vertex, value);
    return vPt;
  }

  ////////////////////////////////////////////////////////////////
  // color mapping methods
  ////////////////////////////////////////////////////////////////

  //String remainderString;

  void colorIsosurface() {
    if (isContoured &&
        !(jvxlDataIs2dContour 
            || thePlane != null)) {
      Logger.error("Isosurface error: Cannot contour this type of data.");
      return;
    }
    setMapRanges();
    if (isContoured) {
      generateContourData(jvxlDataIs2dContour);
    }
    applyColorScale();
    jvxlData.jvxlExtraLine = jvxlExtraLine(1);
    jvxlFileMessage = "mapped: min = " + valueMappedToRed + "; max = "
        + valueMappedToBlue;
  }

  void applyColorScale() {
    if (colorPhase == 0)
      colorPhase = 1;
    int vertexCount = meshData.vertexCount;
    int[] colors = meshData.vertexColors;
    colorFractionBase = defaultColorFractionBase;
    colorFractionRange = defaultColorFractionRange;
    setMapRanges();
    float min = mappedDataMin;
    float max = mappedDataMax;
    if (colors == null)
      meshData.vertexColors = colors = new int[vertexCount];
    String list = "";
    String list1 = "";
    int incr = 1;
    if (jvxlDataIsPrecisionColor || isContoured)
      jvxlWritePrecisionColor = true;
    for (int i = 0; i < vertexCount; i += incr) {
      float value = getVertexColorValue(i);
        char ch;
        if (jvxlWritePrecisionColor) {
          ch = jvxlValueAsCharacter2(value, min, max, colorFractionBase,
              colorFractionRange);
          list1 += remainder;
        } else {
          //isColorReversed
          ch = jvxlValueAsCharacter(value, valueMappedToRed, valueMappedToBlue,
              colorFractionBase, colorFractionRange);
        }
        list += ch;
    }
    jvxlData.isJvxlPrecisionColor = jvxlWritePrecisionColor;
    jvxlData.jvxlColorData = (colorByPhase && !isBicolorMap && !colorBySign ? ""
        : list + list1 + "\n");
  }

  void setMapRanges() {
    // ["mapColor" | "getSurface/jvxl-2dContour] --> colorIsosurface
    // ["phase" | colorIsoSurface] --> applyColorScale
    // colorIsosurface --> generateContourData --> createContours
    if (colorByPhase || colorBySign || isBicolorMap && !isContoured) {
      mappedDataMin = -1;
      mappedDataMax = 1;
    }
    if (mappedDataMin == Float.MAX_VALUE || mappedDataMin == mappedDataMax) {
      mappedDataMin = getMinMappedValue();
      mappedDataMax = getMaxMappedValue();
    }
    if (mappedDataMin == 0 && mappedDataMax == 0) {
      //just set default -1/1 if there is no obvious data
      mappedDataMin = -1;
      mappedDataMax = 1;
    }

    if (!rangeDefined) {
      valueMappedToRed = mappedDataMin;
      valueMappedToBlue = mappedDataMax;
    }
    jvxlData.valueMappedToRed = valueMappedToRed;
    jvxlData.valueMappedToBlue = valueMappedToBlue;
    jvxlData.mappedDataMin = mappedDataMin;
    jvxlData.mappedDataMax = mappedDataMax;
  }

  float getVertexColorValue(int vertexIndex) {
    float value, datum;
    /* but RETURNS the actual value, not the truncated one
     * right, so what we are doing here is setting a range within the 
     * data for which we want red-->blue, but returning the actual
     * number so it can be encoded more precisely. This turned out to be
     * the key to making the JVXL contours work.
     *  
     */
    if (isBicolorMap && !isContoured) // will be current mesh only
      datum = value = meshData.vertexValues[vertexIndex];
    else if (jvxlDataIs2dContour)
      datum = value = getInterpolatedPixelValue(meshData.vertices[vertexIndex]);
    else
      datum = value = lookupInterpolatedVoxelValue(meshData.vertices[vertexIndex]);
    if (isBicolorMap && !isContoured || colorBySign) {
      if (value <= 0)
        meshData.vertexColors[vertexIndex] = (isColorReversed ? colorPos : colorNeg);
      if (value > 0)
        meshData.vertexColors[vertexIndex] =(isColorReversed ? colorNeg : colorPos);
      if (!isContoured)
        datum = (value > 0 ? 0.999f : -0.999f);
    } else {
      if (value < valueMappedToRed)
        value = valueMappedToRed;
      if (value >= valueMappedToBlue)
        value = valueMappedToBlue;
      meshData.vertexColors[vertexIndex] = getColorFromPalette(value);
    }
    return datum;
  }

  float getMinMappedValue() {
    int vertexCount = meshData.vertexCount;
    Point3f[] vertexes = meshData.vertices;
    float min = Float.MAX_VALUE;
    int incr = 1;
    for (int i = 0; i < vertexCount; i += incr) {
      float challenger;
      if (jvxlDataIs2dContour)
        challenger = getInterpolatedPixelValue(vertexes[i]);
      else
        challenger = lookupInterpolatedVoxelValue(vertexes[i]);
      if (challenger < min)
        min = challenger;
    }
    return min;
  }

  float getMaxMappedValue() {
    int vertexCount = meshData.vertexCount;
    Point3f[] vertexes = meshData.vertices;
    float max = -Float.MAX_VALUE;
    int incr = 1;
    for (int i = 0; i < vertexCount; i += incr) {
      float challenger;
      if (jvxlDataIs2dContour)
        challenger = getInterpolatedPixelValue(vertexes[i]);
      else
        challenger = lookupInterpolatedVoxelValue(vertexes[i]);
      if (challenger == Float.MAX_VALUE)
        challenger = 0; //for now TESTING ONLY
      if (challenger > max && challenger != Float.MAX_VALUE)
        max = challenger;
    }
    return max;
  }

  float lookupInterpolatedVoxelValue(Point3f point) {
    //ARGH!!! ONLY FOR ORTHOGONAL AXES!!!!!
    //the dot product presumes axes are PERPENDICULAR.
    Point3f pt = new Point3f();
    xyzToVoxelPt(point, pt);
    return getInterpolatedVoxelValue(pt);
  }

  float getInterpolatedVoxelValue(Point3f pt) {
    int iMax;
    int xDown = indexDown(pt.x, iMax = voxelCounts[0] - 1);
    int xUp = xDown + (pt.x < 0 || xDown == iMax ? 0 : 1);
    int yDown = indexDown(pt.y, iMax = voxelCounts[1] - 1);
    int yUp = yDown + (pt.y < 0 || yDown == iMax ? 0 : 1);
    int zDown = indexDown(pt.z, iMax = voxelCounts[2] - 1);
    int zUp = zDown
        + (pt.z < 0 || zDown == iMax || jvxlDataIs2dContour ? 0 : 1);
    float v1 = getFractional2DValue(pt.x - xDown, pt.y - yDown,
        voxelData[xDown][yDown][zDown], voxelData[xUp][yDown][zDown],
        voxelData[xDown][yUp][zDown], voxelData[xUp][yUp][zDown]);
    float v2 = getFractional2DValue(pt.x - xDown, pt.y - yDown,
        voxelData[xDown][yDown][zUp], voxelData[xUp][yDown][zUp],
        voxelData[xDown][yUp][zUp], voxelData[xUp][yUp][zUp]);
    return v1 + (pt.z - zDown) * (v2 - v1);
  }

  final Vector3f pointVector = new Vector3f();

  float getInterpolatedPixelValue(Point3f ptXYZ) {
    pointVector.set(ptXYZ);
    xyzToPixelVector(pointVector);
    float x = pointVector.x;
    float y = pointVector.y;
    int xDown = (x >= pixelCounts[0] ? pixelCounts[0] - 1 : x < 0 ? 0 : (int) x);
    int yDown = (y >= pixelCounts[1] ? pixelCounts[1] - 1 : y < 0 ? 0 : (int) y);
    int xUp = xDown + (xDown == pixelCounts[0] - 1 ? 0 : 1);
    int yUp = yDown + (yDown == pixelCounts[1] - 1 ? 0 : 1);
    float value = getFractional2DValue(x - xDown, y - yDown,
        pixelData[xDown][yDown], pixelData[xUp][yDown], pixelData[xDown][yUp],
        pixelData[xUp][yUp]);
    return value;
  }

  int indexDown(float value, int iMax) {
    if (value < 0)
      return 0;
    int floor = (int) value;
    return (floor > iMax ? iMax : floor);
  }

  float getFractional2DValue(float fx, float fy, float x11, float x12,
                             float x21, float x22) {
    float v1 = x11 + fx * (x12 - x11);
    float v2 = x21 + fx * (x22 - x21);
    return v1 + fy * (v2 - v1);
  }

  float contourPlaneMinimumValue;
  float contourPlaneMaximumValue;

  void jvxlReadColorData() {

    // standard jvxl file read for color 

    fractionPtr = 0;
    int vertexCount = meshData.vertexCount;
    int[] colors = meshData.vertexColors;
    fractionData = new StringBuffer();
    strFractionTemp = (isJvxl ? jvxlColorDataRead : "");
    if (isJvxl && strFractionTemp.length() == 0) {
      Logger.error("You cannot use JVXL data to map onto OTHER data, because it only containts the data for one surface. Use ISOSURFACE \"file.jvxl\" not ISOSURFACE .... MAP \"file.jvxl\".");
      return;
    }
    fractionPtr = 0;
    Logger.info("JVXL reading color data base/range: " + mappedDataMin + "/"
        + mappedDataMax + " for " + vertexCount + " vertices."
        + " using encoding keys " + colorFractionBase + " "
        + colorFractionRange);
    Logger.info("mapping red-->blue for " + valueMappedToRed + " to "
        + valueMappedToBlue + " colorPrecision:" + jvxlDataIsPrecisionColor);

    float min = (mappedDataMin == Float.MAX_VALUE ? defaultMappedDataMin
        : mappedDataMin);
    float range = (mappedDataMin == Float.MAX_VALUE ? defaultMappedDataMax
        : mappedDataMax)
        - min;
    float colorRange = valueMappedToBlue - valueMappedToRed;
    contourPlaneMinimumValue = Float.MAX_VALUE;
    contourPlaneMaximumValue = -Float.MAX_VALUE;
    if (colors == null || colors.length < vertexCount)
      meshData.vertexColors = colors = new int[vertexCount];
    int n = (isContoured ? contourVertexCount : vertexCount);
    String data = jvxlColorDataRead;
    int cpt = 0;
    for (int i = 0; i < n; i++) {
      float fraction, value;
      if (jvxlDataIsPrecisionColor) {
        // this COULD be an option for mapped surfaces; 
        // necessary for planes.
        // precision is used for FULL-data range encoding, allowing full
        // treatment of JVXL files as though they were CUBE files.
        fraction = jvxlFractionFromCharacter2(data.charAt(cpt), data.charAt(cpt
            + n), colorFractionBase, colorFractionRange);
        value = min + fraction * range;
      } else {
        // my original encoding scheme
        // low precision only allows for mapping relative to the defined color range
        fraction = jvxlFractionFromCharacter(data.charAt(cpt),
            colorFractionBase, colorFractionRange, 0.5f);
        value = valueMappedToRed + fraction * colorRange;
      }
      ++cpt;
      if (value < contourPlaneMinimumValue)
        contourPlaneMinimumValue = value;
      if (value > contourPlaneMaximumValue)
        contourPlaneMaximumValue = value;

      if (isContoured) {
        contourVertexes[i].setValue(value);
      } else if (colorBySign) {
        colors[i] = ((isColorReversed ? value > 0
            : value <= 0) ? colorNeg : colorPos);
      } else {
        colors[i] = getColorFromPalette(value);
      }
    }
    if (mappedDataMin == Float.MAX_VALUE) {
      mappedDataMin = contourPlaneMinimumValue;
      mappedDataMax = contourPlaneMaximumValue;
    }
    jvxlData.jvxlColorData = data + "\n";
  }

  int colorScheme;
  int getColorFromPalette(float value) {
    if (isColorReversed)
      return getColorFromPalette(-value, -valueMappedToBlue,
          -valueMappedToRed, colorScheme);
    return getColorFromPalette(value, valueMappedToRed,
        valueMappedToBlue, colorScheme);
  }
  
  int getColorFromPalette(float value, float min, float max, int colorScheme) {
    return 0;  // user-supplied
  }
  
  ////////////////////////////////////////////////////////////////
  ////////// JVXL FILE READING/WRITING ////////////
  ////////////////////////////////////////////////////////////////

  int fractionPtr;
  String strFractionTemp = "";
  StringBuffer fractionData = new StringBuffer();

  float jvxlGetNextFraction(int base, int range, float fracOffset) {
    if (fractionPtr >= strFractionTemp.length()) {
      if (!endOfData)
        Logger.error("end of file reading compressed fraction data at point "
            + fractionData.length());
      endOfData = true;
      strFractionTemp = "" + (char) base;
      fractionData.append(strFractionTemp);
      fractionData.append('\n');
      fractionPtr = 0;
    }
    return jvxlFractionFromCharacter(strFractionTemp.charAt(fractionPtr++),
        base, range, fracOffset);
  }
  
  /* unused here
  float jvxlValueFromCharacter(int ich, float min, float max, int base,
                               int range, float fracOffset) {
    float fraction = jvxlFractionFromCharacter(ich, base, range, fracOffset);
    return (max == min ? fraction : min + fraction * (max - min));
  }
 */
  
  float jvxlFractionFromCharacter(int ich, int base, int range, float fracOffset) {
    if (ich == base + range)
      return Float.NaN;
    if (ich < base)
      ich = 92; // ! --> \
    float fraction = (ich - base + fracOffset) / range;
    if (fraction < 0f)
      return 0f;
    if (fraction > 1f)
      return 0.999999f;
    //if (logCompression)
      //Logger.info("ffc: " + fraction + " <-- " + ich + " " + (char) ich);
    return fraction;
  }

  char jvxlValueAsCharacter(float value, float min, float max, int base,
                            int range) {
    float fraction = (min == max ? value : (value - min) / (max - min));
    return jvxlFractionAsCharacter(fraction, base, range);
  }

  char jvxlFractionAsCharacter(float fraction, int base, int range) {
    if (fraction > 0.9999f)
      fraction = 0.9999f;
    else if (Float.isNaN(fraction))
      fraction = 1.0001f;
    int ich = (int) (fraction * range + base);
    if (ich < base)
      return (char) base;
    if (ich == 92)
      return 33; // \ --> !
    //if (logCompression)
      //Logger.info("fac: " + fraction + " --> " + ich + " " + (char) ich);
    return (char) ich;
  }

  float jvxlValueFromCharacter2(int ich, int ich2, float min, float max,
                                int base, int range) {
    float fraction = jvxlFractionFromCharacter2(ich, ich2, base, range);
    return (max == min ? fraction : min + fraction * (max - min));
  }

  float jvxlFractionFromCharacter2(int ich1, int ich2, int base, int range) {
    float fraction = jvxlFractionFromCharacter(ich1, base, range, 0);
    float remains = jvxlFractionFromCharacter(ich2, base, range, 0.5f);
    return fraction + remains / range;
  }

  char remainder;

  char jvxlValueAsCharacter2(float value, float min, float max, int base,
                             int range) {
    float fraction = (min == max ? value : (value - min) / (max - min));
    char ch1 = jvxlFractionAsCharacter(fraction, base, range);
    fraction -= jvxlFractionFromCharacter(ch1, base, range, 0);
    remainder = jvxlFractionAsCharacter(fraction * range, base, range);
    return ch1;
  }

  String jvxlExtraLine(int n) {
    return (-n) + " " + edgeFractionBase + " " + edgeFractionRange + " "
        + colorFractionBase + " " + colorFractionRange
        + " Jmol voxel format version 1.0\n";
    //0.9e adds color contours for planes and min/max range, contour settings
  }

  void jvxlUpdateInfo() {
      jvxlData.title = title;
      jvxlData.jvxlDefinitionLine = jvxlGetDefinitionLine(false);
      jvxlData.jvxlInfoLine = jvxlGetDefinitionLine(true);
  }
  
  String jvxlGetFile(String msg, boolean includeHeader, int nSurfaces) {
    String data = "";
    if (includeHeader) {
      data = jvxlData.jvxlFileHeader
          + (nSurfaces > 0 ? (-nSurfaces) + jvxlData.jvxlExtraLine.substring(2)
              : jvxlData.jvxlExtraLine);
      if (data.indexOf("JVXL") != 0)
        data = "JVXL " + data;
    }
    data += "# " + msg + "\n";
    if (title != null)
      for (int i = 0; i < title.length; i++)
        data += "# " + title[i] + "\n";
    data += jvxlData.jvxlDefinitionLine + "\n";
    String compressedData = (jvxlData.jvxlPlane == null ? jvxlData.jvxlSurfaceData : "");
    if (jvxlData.jvxlPlane == null) {
      //no real point in compressing this unless it's a sign-based coloring 
      compressedData += jvxlCompressString(jvxlData.jvxlEdgeData
          + jvxlData.jvxlColorData);
    } else {
      compressedData += jvxlCompressString(jvxlData.jvxlColorData);
    }
    if (!isJvxl &&jvxlData.nBytes > 0)
      jvxlData.jvxlCompressionRatio = (int) (((float)jvxlData.nBytes + jvxlData.jvxlFileHeader
          .length()) / (data.length() + compressedData.length()));
    data += compressedData;
    if (msg != null)
      data += "#-------end of jvxl file data-------\n";
    data += jvxlData.jvxlInfoLine + "\n";
    return data;
  }

  String jvxlGetDefinitionLine(boolean isInfo) {
    String definitionLine = jvxlData.cutoff + " ";

    // cutoff        param1              param2         param3
    //                 |                   |              |
    //   when          |                   |        >  0 ==> jvxlDataIsColorMapped
    //   when          |                   |       == -1 ==> not color mapped
    //   when          |                   |        < -1 ==> jvxlDataIsPrecisionColor    
    //   when        == -1     &&   == -1 ==> noncontoured plane
    //   when        == -1     &&   == -2 ==> contourable plane
    //   when        < -1      &&    >  0 ==> contourable functionXY
    //   when        > 0       &&    <  0 ==> jvxlDataisBicolorMap

    if (jvxlData.jvxlSurfaceData == null)
      return "";
    int nSurfaceData = jvxlData.jvxlSurfaceData.length();
    int nEdgeData = (jvxlData.jvxlEdgeData.length() - 1);
    int nColorData = (jvxlData.jvxlColorData.length() - 1);
    String info = "# nSurfaceData = " + nSurfaceData + "; nEdgeData = "
        + nEdgeData;
    if (jvxlData.jvxlPlane == null) {
      if (jvxlData.isContoured) {
        definitionLine += (-nSurfaceData) + " " + nEdgeData;
        info += "; contoured";
      } else if (jvxlData.isBicolorMap) {
        definitionLine += (nSurfaceData) + " " + (-nEdgeData);
        info += "; bicolor map";
      } else {
        definitionLine += nSurfaceData + " " + nEdgeData;
        info += (jvxlData.isJvxlPrecisionColor && nColorData != -1 ? "; precision colored"
            : nColorData > 0 ? "; colormapped" : "");
      }
      definitionLine += " "
          + (jvxlData.isJvxlPrecisionColor && nColorData != -1 ? -nColorData
              : nColorData);
    } else {
      String s = " " + jvxlData.jvxlPlane.x + " " + jvxlData.jvxlPlane.y + " "
          + jvxlData.jvxlPlane.z + " " + jvxlData.jvxlPlane.w;
      definitionLine += "-1 -2 " + (-nColorData) + s;
      info += "; " + (nColorData > 0 ? "color mapped " : "") + "plane: {" + s
          + " }";
    }
    if (jvxlData.isContoured) {
      definitionLine += " " + jvxlData.nContours;
      info += "; " + jvxlData.nContours + " contours";
    }
    // ...  mappedDataMin  mappedDataMax  valueMappedToRed  valueMappedToBlue ... 
    definitionLine += " " + jvxlData.mappedDataMin + " " + jvxlData.mappedDataMax + " "
        + jvxlData.valueMappedToRed + " " + jvxlData.valueMappedToBlue;
    
    info += "\n# data mimimum = " + jvxlData.mappedDataMin + "; data maximum = "
        + jvxlData.mappedDataMax + " " + "\n# value mapped to red = "
        + jvxlData.valueMappedToRed + "; value mapped to blue = "
        + jvxlData.valueMappedToBlue;
    if (jvxlData.jvxlCompressionRatio > 0)
      info += "; approximate compressionRatio=" + jvxlData.jvxlCompressionRatio
          + ":1";
    info += "\n# created using Jvxl";
    return (isInfo ? info : definitionLine);
  }

  String jvxlCompressString(String data) {
    /* just a simple compression, but allows 2000-6000:1 CUBE:JVXL for planes!
     * 
     *   "X~nnn " means "nnn copies of character X" 
     *   
     *   ########## becomes "#~10 " 
     *   ~ becomes "~~" 
     *
     */
    String dataOut = "";
    String dataBuffer = "";
    char chLast = '\0';
    data += '\0';
    int nLast = 0;
    for (int i = 0; i < data.length(); i++) {
      char ch = data.charAt(i);
      if (ch == chLast) {
        ++nLast;
        dataBuffer += ch;
        if (ch != '~')
          ch = '\0';
      } else if (nLast > 0) {
        dataOut += (nLast < 4 || chLast == '~' || chLast == ' '
            || chLast == '\t' ? dataBuffer : "~" + nLast + " ");
        dataBuffer = "";
        nLast = 0;
      }
      if (ch != '\0') {
        dataOut += ch;
        chLast = ch;
      }
    }
    return dataOut;
  }

  String jvxlUncompressString(String data) {
    if (data.indexOf("~") < 0)
      return data;
    String dataOut = "";
    char chLast = '\0';
    for (int i = 0; i < data.length(); i++) {
      char ch = data.charAt(i);
      if (ch == '~') {
        int nChar = parseInt(data, ++i);
        if (nChar == Integer.MIN_VALUE) {
          if (chLast == '~') {
            dataOut += '~';
            while ((ch = data.charAt(++i)) == '~')
              dataOut += '~';
          } else {
            Logger.error("Error uncompressing string " + data.substring(0, i)
                + "?");
          }
        } else {
          for (int c = 0; c < nChar; c++)
            dataOut += chLast;
          i = next[0];
        }
      } else {
        dataOut += ch;
        chLast = ch;
      }
    }
    return dataOut;
  }

  ////////////////////////////////////////////////////////////////
  // marching cube stuff
  ////////////////////////////////////////////////////////////////

  final float[] vertexValues = new float[8];
  final Point3i[] vertexPoints = new Point3i[8];
  final Point3f[] surfacePoints = new Point3f[12];
  {
    for (int i = 12; --i >= 0;)
      surfacePoints[i] = new Point3f();
    for (int i = 8; --i >= 0;)
      vertexPoints[i] = new Point3i();
  }
  int cubeCountX, cubeCountY, cubeCountZ;
  int contourType; // 0, 1, or 2

  int getContourType(Point4f plane) {
    Vector3f norm = new Vector3f(plane.x, plane.y, plane.z);
    float dotX = norm.dot(volumetricVectors[0]);
    float dotY = norm.dot(volumetricVectors[1]);
    float dotZ = norm.dot(volumetricVectors[2]);
    dotX *= dotX;
    dotY *= dotY;
    dotZ *= dotZ;
    float max = Math.max(dotX, dotY);
    int iType = (max < dotZ ? 2 : max == dotY ? 1 : 0);
    return iType;
  }

  void generateSurfaceData() {
    cubeCountX = voxelData.length - 1;
    cubeCountY = voxelData[0].length - 1;
    cubeCountZ = voxelData[0][0].length - 1;
    fractionData = new StringBuffer();
    strFractionTemp = (isJvxl ? jvxlEdgeDataRead : "");
    fractionPtr = 0;
    if (thePlane != null) {
      contourVertexCount = 0;
      contourType = getContourType(thePlane);
    } else if (isContoured) {
      contourVertexCount = 0;
      contourType = 2;
    }
    int[][] isoPointIndexes = new int[cubeCountY * cubeCountZ][12];
    //for (int i = cubeCountY * cubeCountZ; --i >= 0;)
      //isoPointIndexes[i] = new int[12];
    int insideCount = 0, outsideCount = 0, surfaceCount = 0;
    for (int x = cubeCountX; --x >= 0;) {
      for (int y = cubeCountY; --y >= 0;) {
        for (int z = cubeCountZ; --z >= 0;) {
          int[] voxelPointIndexes = propagateNeighborPointIndexes(x, y, z,
              isoPointIndexes);
          //System.out.println("generateSurfaceData " 
            //  + " xyz " + x + " " + y + " " + z);
          int insideMask = 0;
          for (int i = 8; --i >= 0;) {
            Point3i offset = cubeVertexOffsets[i];
            if (isInside(
                (vertexValues[i] = voxelData[x + offset.x][y + offset.y][z
                    + offset.z]), cutoff))
              insideMask |= 1 << i;
          }

          if (insideMask == 0) {
            ++outsideCount;
            continue;
          }
          if (insideMask == 0xFF) {
            ++insideCount;
            continue;
          }
          ++surfaceCount;
          if (!processOneCubical(insideMask, cutoff, voxelPointIndexes, x, y, z)
              || isContoured)
            continue;

          byte[] triangles = triangleTable2[insideMask];
          for (int i = triangles.length; (i -= 4) >= 0;) {
            if (!isCutoffAbsolute
                || checkCutoff(voxelPointIndexes[triangles[i]],
                    voxelPointIndexes[triangles[i + 1]],
                    voxelPointIndexes[triangles[i + 2]]))
              meshData.addTriangleCheck(voxelPointIndexes[triangles[i]],
                  voxelPointIndexes[triangles[i + 1]],
                  voxelPointIndexes[triangles[i + 2]], triangles[i + 3]);
          }
        }
      }
    }
    if (isJvxl) {
      fractionData = new StringBuffer();
      fractionData.append(jvxlEdgeDataRead);
    }
    fractionData.append('\n'); //from generateSurfaceData
  }

  boolean checkCutoff(int v1, int v2, int v3) {
    // never cross a +/- junction with a triangle in the case of orbitals, 
    // where we are using |psi| instead of psi for the surface generation.
    // note that for bicolor maps, where the values are all positive, we 
    // check this later in the meshRenderer
    if (v1 < 0 || v2 < 0 || v3 < 0)
      return false;
    
    float val1 = meshData.vertexValues[v1];
    float val2 = meshData.vertexValues[v2];
    float val3 = meshData.vertexValues[v3];

    return (val1 * val2 >= 0 && val2 * val3 >= 0);
  }

  boolean isInside(float voxelValue, float max) {
    return ((max > 0 && (isCutoffAbsolute ? Math.abs(voxelValue) : voxelValue) >= max) || (max <= 0 && voxelValue <= max));
  }

  final int[] nullNeighbor = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

  int[] propagateNeighborPointIndexes(int x, int y, int z, int[][] isoPointIndexes) {
    /*                     Y 
     *                      4 --------4--------- 5  
     *                     /|                   /|
     *                    / |                  / |
     *                   /  |                 /  |
     *                  7   8                5   |
     *                 /    |               /    9
     *                /     |              /     |
     *               7 --------6--------- 6      |
     *               |      |             |      |
     *               |      0 ---------0--|----- 1    X
     *               |     /              |     /
     *              11    /               10   /
     *               |   3                |   1
     *               |  /                 |  /
     *               | /                  | /
     *               3 ---------2-------- 2
     *              Z 
     * 
     * We are running through the grid points in yz planes from high x --> low x
     * and within those planes along strips from high y to low y
     * and within those strips, from high z to low z. 
     * 
     * For each such cube, edges are traversed from high to low (11-->0)
     * 
     * Each edge has the potential to be "critical" and cross the surface.
     * Setting -1 in voxelPointIndexes indicates that this edge needs checking.
     * Otherwise, the crossing point for this edge is taken from the value
     * already determined, because it has already been determined to be critical. 
     * 
     */
    int cellIndex = y * cubeCountZ + z;
    int[] voxelPointIndexes = isoPointIndexes[cellIndex];

    boolean noXNeighbor = (x == cubeCountX - 1);
    if (noXNeighbor) {
      // the x neighbor is myself from my last pass through here
      voxelPointIndexes[1] = -1;
      voxelPointIndexes[9] = -1;
      voxelPointIndexes[5] = -1;
      voxelPointIndexes[10] = -1;
    } else {
      voxelPointIndexes[1] = voxelPointIndexes[3];
      voxelPointIndexes[9] = voxelPointIndexes[8];
      voxelPointIndexes[5] = voxelPointIndexes[7];
      voxelPointIndexes[10] = voxelPointIndexes[11];
    }

    //from the y neighbor pick up the top
    boolean noYNeighbor = (y == cubeCountY - 1);
    int[] yNeighbor = noYNeighbor ? nullNeighbor : isoPointIndexes[cellIndex
        + cubeCountZ];

    voxelPointIndexes[6] = yNeighbor[2];
    voxelPointIndexes[7] = yNeighbor[3];
    voxelPointIndexes[4] = yNeighbor[0];
    if (noXNeighbor)
      voxelPointIndexes[5] = yNeighbor[1];

    // from my z neighbor
    boolean noZNeighbor = (z == cubeCountZ - 1);
    int[] zNeighbor = noZNeighbor ? nullNeighbor
        : isoPointIndexes[cellIndex + 1];

    voxelPointIndexes[2] = zNeighbor[0];
    voxelPointIndexes[11] = zNeighbor[8];
    if (noYNeighbor)
      voxelPointIndexes[6] = zNeighbor[4];
    if (noXNeighbor)
      voxelPointIndexes[10] = zNeighbor[9];

    // these must always be calculated
    voxelPointIndexes[0] = -1;
    voxelPointIndexes[3] = -1;
    voxelPointIndexes[8] = -1;

    return voxelPointIndexes;
  }

  //int firstCriticalVertex;
  //int lastCriticalVertex;
  int edgeCount;
  final static float assocCutoff = 0.3f;

  boolean processOneCubical(int insideMask, float cutoff,
                            int[] voxelPointIndexes, int x, int y, int z) {
    
    
    int edgeMask = insideMaskTable[insideMask];
    boolean isNaN = false;
    for (int iEdge = 12; --iEdge >= 0;) {
      if ((edgeMask & (1 << iEdge)) == 0)
        continue;
      if (voxelPointIndexes[iEdge] >= 0)
        continue; // propagated from neighbor
      ++edgeCount;
      int vertexA = edgeVertexes[2 * iEdge];
      int vertexB = edgeVertexes[2 * iEdge + 1];
      float valueA = vertexValues[vertexA];
      float valueB = vertexValues[vertexB];
      if (Float.isNaN(valueA) || Float.isNaN(valueB))
        isNaN = true;
      calcVertexPoints(x, y, z, vertexA, vertexB);
      //System.out.println("critical edge " + edgeCount + " index " + iEdge 
        //  + " xyz " + x + " " + y + " " + z 
          //+ " vertexA/B "+vertexA + " " + vertexB 
          //+ " pointA/B "+ pointA + " " + pointB 
          //+ " valueA/B " + valueA + " " + valueB);
      float thisValue = calcSurfacePoint(cutoff, valueA, valueB, surfacePoints[iEdge]);
      if (isContoured) {
        // Logger.info(" processVoxel " + x + "," + y + "," + z + " "
        //   + iEdge + " " + binaryString(edgeMask) + " " + contourType
        // + "  fraction " + fraction + " valueA " + valueA + " valueB "
        //+ valueB + " cutoff " + cutoff);
        /* 
         * we are collecting just the desired type of intersection for the 2D marching
         * square contouring -- x, y, or z. In the case of a contoured f(x,y) surface, 
         * we take every point.
         * 
         */
        int vPt = Integer.MAX_VALUE;
        if (edgeTypeTable[iEdge] == contourType)
          vPt = addContourData(x, y, z, cubeVertexOffsets[vertexA],
              surfacePoints[iEdge], cutoff);
        voxelPointIndexes[iEdge] = vPt;
        continue;
      }
      voxelPointIndexes[iEdge] = addVertexCopy(surfacePoints[iEdge], thisValue);
    }
    return !isNaN;
  }

  String calcDataKey(int x, int y, int z, int vertexPt) {
    Point3i offset = cubeVertexOffsets[vertexPt];
    return (x + offset.x) + "_" + (y + offset.y) + "_" + (z + offset.z);
  }

  final Point3f voxelOrigin = new Point3f();
  final Point3f voxelT = new Point3f();
  final Point3f pointA = new Point3f();
  final Point3f pointB = new Point3f();
  final Vector3f edgeVector = new Vector3f();

  float calcSurfacePoint(float cutoff, float valueA, float valueB,
                         Point3f surfacePoint) {
    float fraction, thisValue;
    if (isJvxl && jvxlEdgeDataCount > 0) {
      fraction = jvxlGetNextFraction(edgeFractionBase, edgeFractionRange, 0.5f);
      thisValue = fraction;
    } else {
      float diff = valueB - valueA;
      fraction = (cutoff - valueA) / diff;
      if (isCutoffAbsolute && (fraction < 0 || fraction > 1))
        fraction = (-cutoff - valueA) / diff;

      if (fraction < 0 || fraction > 1) {
        //Logger.error("problem with unusual fraction=" + fraction + " cutoff="
          //  + cutoff + " A:" + valueA + " B:" + valueB);
        fraction = Float.NaN;
      }
      thisValue = valueA + fraction * diff;
      if (!isJvxl)
        fractionData.append(jvxlFractionAsCharacter(fraction, edgeFractionBase,
            edgeFractionRange));
    }
    edgeVector.sub(pointB, pointA);
    surfacePoint.scaleAdd(fraction, edgeVector, pointA);
    return thisValue;
  }

  void calcVertexPoints(int x, int y, int z, int vertexA, int vertexB) {
    voxelPtToXYZ(x, y, z, voxelOrigin);
    pointA.add(voxelOrigin, voxelVertexVectors[vertexA]);
    pointB.add(voxelOrigin, voxelVertexVectors[vertexB]);
  }

  final static Point3i[] cubeVertexOffsets = { new Point3i(0, 0, 0),
      new Point3i(1, 0, 0), new Point3i(1, 0, 1), new Point3i(0, 0, 1),
      new Point3i(0, 1, 0), new Point3i(1, 1, 0), new Point3i(1, 1, 1),
      new Point3i(0, 1, 1) };

  final static Vector3f[] cubeVertexVectors = { new Vector3f(0, 0, 0),
      new Vector3f(1, 0, 0), new Vector3f(1, 0, 1), new Vector3f(0, 0, 1),
      new Vector3f(0, 1, 0), new Vector3f(1, 1, 0), new Vector3f(1, 1, 1),
      new Vector3f(0, 1, 1) };

  Vector3f[] voxelVertexVectors = new Vector3f[8];

  void calcVoxelVertexVectors() {
    for (int i = 8; --i >= 0;) {
      voxelVertexVectors[i] = new Vector3f();
      volumetricMatrix.transform(cubeVertexVectors[i], voxelVertexVectors[i]);
    }
  }

  /*                     Y 
   *                      4 --------4--------- 5  
   *                     /|                   /|
   *                    / |                  / |
   *                   /  |                 /  |
   *                  7   8                5   |
   *                 /    |               /    9
   *                /     |              /     |
   *               7 --------6--------- 6      |
   *               |      |             |      |
   *               |      0 ---------0--|----- 1    X
   *               |     /              |     /
   *              11    /               10   /
   *               |   3                |   1
   *               |  /                 |  /
   *               | /                  | /
   *               3 ---------2-------- 2
   *              Z 
   * 
   * 
   * type 0: x-edges: 0 2 4 6
   * type 1: y-edges: 8 9 10 11
   * type 2: z-edges: 1 3 5 7
   * 
   * 
   * 
   */
  
  final static int edgeTypeTable[] = { 0, 2, 0, 2, 0, 2, 0, 2, 1, 1, 1, 1 };

  final static byte edgeVertexes[] = { 0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6,
      7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7 };

  final static short insideMaskTable[] = { 0x0000, 0x0109, 0x0203, 0x030A,
      0x0406, 0x050F, 0x0605, 0x070C, 0x080C, 0x0905, 0x0A0F, 0x0B06, 0x0C0A,
      0x0D03, 0x0E09, 0x0F00, 0x0190, 0x0099, 0x0393, 0x029A, 0x0596, 0x049F,
      0x0795, 0x069C, 0x099C, 0x0895, 0x0B9F, 0x0A96, 0x0D9A, 0x0C93, 0x0F99,
      0x0E90, 0x0230, 0x0339, 0x0033, 0x013A, 0x0636, 0x073F, 0x0435, 0x053C,
      0x0A3C, 0x0B35, 0x083F, 0x0936, 0x0E3A, 0x0F33, 0x0C39, 0x0D30, 0x03A0,
      0x02A9, 0x01A3, 0x00AA, 0x07A6, 0x06AF, 0x05A5, 0x04AC, 0x0BAC, 0x0AA5,
      0x09AF, 0x08A6, 0x0FAA, 0x0EA3, 0x0DA9, 0x0CA0, 0x0460, 0x0569, 0x0663,
      0x076A, 0x0066, 0x016F, 0x0265, 0x036C, 0x0C6C, 0x0D65, 0x0E6F, 0x0F66,
      0x086A, 0x0963, 0x0A69, 0x0B60, 0x05F0, 0x04F9, 0x07F3, 0x06FA, 0x01F6,
      0x00FF, 0x03F5, 0x02FC, 0x0DFC, 0x0CF5, 0x0FFF, 0x0EF6, 0x09FA, 0x08F3,
      0x0BF9, 0x0AF0, 0x0650, 0x0759, 0x0453, 0x055A, 0x0256, 0x035F, 0x0055,
      0x015C, 0x0E5C, 0x0F55, 0x0C5F, 0x0D56, 0x0A5A, 0x0B53, 0x0859, 0x0950,
      0x07C0, 0x06C9, 0x05C3, 0x04CA, 0x03C6, 0x02CF, 0x01C5, 0x00CC, 0x0FCC,
      0x0EC5, 0x0DCF, 0x0CC6, 0x0BCA, 0x0AC3, 0x09C9, 0x08C0, 0x08C0, 0x09C9,
      0x0AC3, 0x0BCA, 0x0CC6, 0x0DCF, 0x0EC5, 0x0FCC, 0x00CC, 0x01C5, 0x02CF,
      0x03C6, 0x04CA, 0x05C3, 0x06C9, 0x07C0, 0x0950, 0x0859, 0x0B53, 0x0A5A,
      0x0D56, 0x0C5F, 0x0F55, 0x0E5C, 0x015C, 0x0055, 0x035F, 0x0256, 0x055A,
      0x0453, 0x0759, 0x0650, 0x0AF0, 0x0BF9, 0x08F3, 0x09FA, 0x0EF6, 0x0FFF,
      0x0CF5, 0x0DFC, 0x02FC, 0x03F5, 0x00FF, 0x01F6, 0x06FA, 0x07F3, 0x04F9,
      0x05F0, 0x0B60, 0x0A69, 0x0963, 0x086A, 0x0F66, 0x0E6F, 0x0D65, 0x0C6C,
      0x036C, 0x0265, 0x016F, 0x0066, 0x076A, 0x0663, 0x0569, 0x0460, 0x0CA0,
      0x0DA9, 0x0EA3, 0x0FAA, 0x08A6, 0x09AF, 0x0AA5, 0x0BAC, 0x04AC, 0x05A5,
      0x06AF, 0x07A6, 0x00AA, 0x01A3, 0x02A9, 0x03A0, 0x0D30, 0x0C39, 0x0F33,
      0x0E3A, 0x0936, 0x083F, 0x0B35, 0x0A3C, 0x053C, 0x0435, 0x073F, 0x0636,
      0x013A, 0x0033, 0x0339, 0x0230, 0x0E90, 0x0F99, 0x0C93, 0x0D9A, 0x0A96,
      0x0B9F, 0x0895, 0x099C, 0x069C, 0x0795, 0x049F, 0x0596, 0x029A, 0x0393,
      0x0099, 0x0190, 0x0F00, 0x0E09, 0x0D03, 0x0C0A, 0x0B06, 0x0A0F, 0x0905,
      0x080C, 0x070C, 0x0605, 0x050F, 0x0406, 0x030A, 0x0203, 0x0109, 0x0000 };

  
  /* the new triangle table. Fourth number in each ABC set is b3b2b1, where
   * b1 = 1 for AB, b2 = 1 for BC, b3 = 1 for CA lines to be drawn for mesh
   * 
   * So, for example: 
  
   1, 8, 3, 6
   
   * 6 is 110 in binary, so b3 = 1, b2 = 1, b1 = 0.
   * b1 refers to the 18 edge, b2 refers to the 83 edge, 
   * and b3 refers to the 31 edge. The 31 and 83, but not 18 edges 
   * should be drawn for a mesh. On the cube above, you can see
   * that the 18 edges is in the interior of the cube. That's why we
   * don't render it with a mesh.
   
   
   Bob Hanson, 3/29/2007
  
  */
  
  final static byte[][] triangleTable2 = {
    null,
    { 0, 8, 3, 7},
    { 0, 1, 9, 7},
    { 1, 8, 3, 6,  9, 8, 1, 5},
    { 1, 2, 10, 7},
    { 0, 8, 3, 7,  1, 2, 10, 7},
    { 9, 2, 10, 6,  0, 2, 9, 5},
    { 2, 8, 3, 6,  2, 10, 8, 1,  10, 9, 8, 3},
    { 3, 11, 2, 7},
    { 0, 11, 2, 6,  8, 11, 0, 5},
    { 1, 9, 0, 7,  2, 3, 11, 7},
    { 1, 11, 2, 6,  1, 9, 11, 1,  9, 8, 11, 3},
    { 3, 10, 1, 6,  11, 10, 3, 5},
    { 0, 10, 1, 6,  0, 8, 10, 1,  8, 11, 10, 3},
    { 3, 9, 0, 6,  3, 11, 9, 1,  11, 10, 9, 3},
    { 9, 8, 10, 5,  10, 8, 11, 6},
    { 4, 7, 8, 7},
    { 4, 3, 0, 6,  7, 3, 4, 5},
    { 0, 1, 9, 7,  8, 4, 7, 7},
    { 4, 1, 9, 6,  4, 7, 1, 1,  7, 3, 1, 3},
    { 1, 2, 10, 7,  8, 4, 7, 7},
    { 3, 4, 7, 6,  3, 0, 4, 3,  1, 2, 10, 7},
    { 9, 2, 10, 6,  9, 0, 2, 3,  8, 4, 7, 7},
    { 2, 10, 9, 3,  2, 9, 7, 0,  2, 7, 3, 6,  7, 9, 4, 6},
    { 8, 4, 7, 7,  3, 11, 2, 7},
    { 11, 4, 7, 6,  11, 2, 4, 1,  2, 0, 4, 3},
    { 9, 0, 1, 7,  8, 4, 7, 7,  2, 3, 11, 7},
    { 4, 7, 11, 3,  9, 4, 11, 1,  9, 11, 2, 2,  9, 2, 1, 6},
    { 3, 10, 1, 6,  3, 11, 10, 3,  7, 8, 4, 7},
    { 1, 11, 10, 6,  1, 4, 11, 0,  1, 0, 4, 3,  7, 11, 4, 5},
    { 4, 7, 8, 7,  9, 0, 11, 1,  9, 11, 10, 6,  11, 0, 3, 6},
    { 4, 7, 11, 3,  4, 11, 9, 4,  9, 11, 10, 6},
    { 9, 5, 4, 7},
    { 9, 5, 4, 7,  0, 8, 3, 7},
    { 0, 5, 4, 6,  1, 5, 0, 5},
    { 8, 5, 4, 6,  8, 3, 5, 1,  3, 1, 5, 3},
    { 1, 2, 10, 7,  9, 5, 4, 7},
    { 3, 0, 8, 7,  1, 2, 10, 7,  4, 9, 5, 7},
    { 5, 2, 10, 6,  5, 4, 2, 1,  4, 0, 2, 3},
    { 2, 10, 5, 3,  3, 2, 5, 1,  3, 5, 4, 2,  3, 4, 8, 6},
    { 9, 5, 4, 7,  2, 3, 11, 7},
    { 0, 11, 2, 6,  0, 8, 11, 3,  4, 9, 5, 7},
    { 0, 5, 4, 6,  0, 1, 5, 3,  2, 3, 11, 7},
    { 2, 1, 5, 3,  2, 5, 8, 0,  2, 8, 11, 6,  4, 8, 5, 5},
    { 10, 3, 11, 6,  10, 1, 3, 3,  9, 5, 4, 7},
    { 4, 9, 5, 7,  0, 8, 1, 5,  8, 10, 1, 2,  8, 11, 10, 3},
    { 5, 4, 0, 3,  5, 0, 11, 0,  5, 11, 10, 6,  11, 0, 3, 6},
    { 5, 4, 8, 3,  5, 8, 10, 4,  10, 8, 11, 6},
    { 9, 7, 8, 6,  5, 7, 9, 5},
    { 9, 3, 0, 6,  9, 5, 3, 1,  5, 7, 3, 3},
    { 0, 7, 8, 6,  0, 1, 7, 1,  1, 5, 7, 3},
    { 1, 5, 3, 5,  3, 5, 7, 6},
    { 9, 7, 8, 6,  9, 5, 7, 3,  10, 1, 2, 7},
    { 10, 1, 2, 7,  9, 5, 0, 5,  5, 3, 0, 2,  5, 7, 3, 3},
    { 8, 0, 2, 3,  8, 2, 5, 0,  8, 5, 7, 6,  10, 5, 2, 5},
    { 2, 10, 5, 3,  2, 5, 3, 4,  3, 5, 7, 6},
    { 7, 9, 5, 6,  7, 8, 9, 3,  3, 11, 2, 7},
    { 9, 5, 7, 3,  9, 7, 2, 0,  9, 2, 0, 6,  2, 7, 11, 6},
    { 2, 3, 11, 7,  0, 1, 8, 5,  1, 7, 8, 2,  1, 5, 7, 3},
    { 11, 2, 1, 3,  11, 1, 7, 4,  7, 1, 5, 6},
    { 9, 5, 8, 5,  8, 5, 7, 6,  10, 1, 3, 3,  10, 3, 11, 6},
    { 5, 7, 0, 1,  5, 0, 9, 6,  7, 11, 0, 1,  1, 0, 10, 5,  11, 10, 0, 1},
    { 11, 10, 0, 1,  11, 0, 3, 6,  10, 5, 0, 1,  8, 0, 7, 5,  5, 7, 0, 1},
    { 11, 10, 5, 3,  7, 11, 5, 5},
    { 10, 6, 5, 7},
    { 0, 8, 3, 7,  5, 10, 6, 7},
    { 9, 0, 1, 7,  5, 10, 6, 7},
    { 1, 8, 3, 6,  1, 9, 8, 3,  5, 10, 6, 7},
    { 1, 6, 5, 6,  2, 6, 1, 5},
    { 1, 6, 5, 6,  1, 2, 6, 3,  3, 0, 8, 7},
    { 9, 6, 5, 6,  9, 0, 6, 1,  0, 2, 6, 3},
    { 5, 9, 8, 3,  5, 8, 2, 0,  5, 2, 6, 6,  3, 2, 8, 5},
    { 2, 3, 11, 7,  10, 6, 5, 7},
    { 11, 0, 8, 6,  11, 2, 0, 3,  10, 6, 5, 7},
    { 0, 1, 9, 7,  2, 3, 11, 7,  5, 10, 6, 7},
    { 5, 10, 6, 7,  1, 9, 2, 5,  9, 11, 2, 2,  9, 8, 11, 3},
    { 6, 3, 11, 6,  6, 5, 3, 1,  5, 1, 3, 3},
    { 0, 8, 11, 3,  0, 11, 5, 0,  0, 5, 1, 6,  5, 11, 6, 6},
    { 3, 11, 6, 3,  0, 3, 6, 1,  0, 6, 5, 2,  0, 5, 9, 6},
    { 6, 5, 9, 3,  6, 9, 11, 4,  11, 9, 8, 6},
    { 5, 10, 6, 7,  4, 7, 8, 7},
    { 4, 3, 0, 6,  4, 7, 3, 3,  6, 5, 10, 7},
    { 1, 9, 0, 7,  5, 10, 6, 7,  8, 4, 7, 7},
    { 10, 6, 5, 7,  1, 9, 7, 1,  1, 7, 3, 6,  7, 9, 4, 6},
    { 6, 1, 2, 6,  6, 5, 1, 3,  4, 7, 8, 7},
    { 1, 2, 5, 5,  5, 2, 6, 6,  3, 0, 4, 3,  3, 4, 7, 6},
    { 8, 4, 7, 7,  9, 0, 5, 5,  0, 6, 5, 2,  0, 2, 6, 3},
    { 7, 3, 9, 1,  7, 9, 4, 6,  3, 2, 9, 1,  5, 9, 6, 5,  2, 6, 9, 1},
    { 3, 11, 2, 7,  7, 8, 4, 7,  10, 6, 5, 7},
    { 5, 10, 6, 7,  4, 7, 2, 1,  4, 2, 0, 6,  2, 7, 11, 6},
    { 0, 1, 9, 7,  4, 7, 8, 7,  2, 3, 11, 7,  5, 10, 6, 7},
    { 9, 2, 1, 6,  9, 11, 2, 2,  9, 4, 11, 1,  7, 11, 4, 5,  5, 10, 6, 7},
    { 8, 4, 7, 7,  3, 11, 5, 1,  3, 5, 1, 6,  5, 11, 6, 6},
    { 5, 1, 11, 1,  5, 11, 6, 6,  1, 0, 11, 1,  7, 11, 4, 5,  0, 4, 11, 1},
    { 0, 5, 9, 6,  0, 6, 5, 2,  0, 3, 6, 1,  11, 6, 3, 5,  8, 4, 7, 7},
    { 6, 5, 9, 3,  6, 9, 11, 4,  4, 7, 9, 5,  7, 11, 9, 1},
    { 10, 4, 9, 6,  6, 4, 10, 5},
    { 4, 10, 6, 6,  4, 9, 10, 3,  0, 8, 3, 7},
    { 10, 0, 1, 6,  10, 6, 0, 1,  6, 4, 0, 3},
    { 8, 3, 1, 3,  8, 1, 6, 0,  8, 6, 4, 6,  6, 1, 10, 6},
    { 1, 4, 9, 6,  1, 2, 4, 1,  2, 6, 4, 3},
    { 3, 0, 8, 7,  1, 2, 9, 5,  2, 4, 9, 2,  2, 6, 4, 3},
    { 0, 2, 4, 5,  4, 2, 6, 6},
    { 8, 3, 2, 3,  8, 2, 4, 4,  4, 2, 6, 6},
    { 10, 4, 9, 6,  10, 6, 4, 3,  11, 2, 3, 7},
    { 0, 8, 2, 5,  2, 8, 11, 6,  4, 9, 10, 3,  4, 10, 6, 6},
    { 3, 11, 2, 7,  0, 1, 6, 1,  0, 6, 4, 6,  6, 1, 10, 6},
    { 6, 4, 1, 1,  6, 1, 10, 6,  4, 8, 1, 1,  2, 1, 11, 5,  8, 11, 1, 1},
    { 9, 6, 4, 6,  9, 3, 6, 0,  9, 1, 3, 3,  11, 6, 3, 5},
    { 8, 11, 1, 1,  8, 1, 0, 6,  11, 6, 1, 1,  9, 1, 4, 5,  6, 4, 1, 1},
    { 3, 11, 6, 3,  3, 6, 0, 4,  0, 6, 4, 6},
    { 6, 4, 8, 3,  11, 6, 8, 5},
    { 7, 10, 6, 6,  7, 8, 10, 1,  8, 9, 10, 3},
    { 0, 7, 3, 6,  0, 10, 7, 0,  0, 9, 10, 3,  6, 7, 10, 5},
    { 10, 6, 7, 3,  1, 10, 7, 1,  1, 7, 8, 2,  1, 8, 0, 6},
    { 10, 6, 7, 3,  10, 7, 1, 4,  1, 7, 3, 6},
    { 1, 2, 6, 3,  1, 6, 8, 0,  1, 8, 9, 6,  8, 6, 7, 6},
    { 2, 6, 9, 1,  2, 9, 1, 6,  6, 7, 9, 1,  0, 9, 3, 5,  7, 3, 9, 1},
    { 7, 8, 0, 3,  7, 0, 6, 4,  6, 0, 2, 6},
    { 7, 3, 2, 3,  6, 7, 2, 5},
    { 2, 3, 11, 7,  10, 6, 8, 1,  10, 8, 9, 6,  8, 6, 7, 6},
    { 2, 0, 7, 1,  2, 7, 11, 6,  0, 9, 7, 1,  6, 7, 10, 5,  9, 10, 7, 1},
    { 1, 8, 0, 6,  1, 7, 8, 2,  1, 10, 7, 1,  6, 7, 10, 5,  2, 3, 11, 7},
    { 11, 2, 1, 3,  11, 1, 7, 4,  10, 6, 1, 5,  6, 7, 1, 1},
    { 8, 9, 6, 1,  8, 6, 7, 6,  9, 1, 6, 1,  11, 6, 3, 5,  1, 3, 6, 1},
    { 0, 9, 1, 7,  11, 6, 7, 7},
    { 7, 8, 0, 3,  7, 0, 6, 4,  3, 11, 0, 5,  11, 6, 0, 1},
    { 7, 11, 6, 7},
    { 7, 6, 11, 7},
    { 3, 0, 8, 7,  11, 7, 6, 7},
    { 0, 1, 9, 7,  11, 7, 6, 7},
    { 8, 1, 9, 6,  8, 3, 1, 3,  11, 7, 6, 7},
    { 10, 1, 2, 7,  6, 11, 7, 7},
    { 1, 2, 10, 7,  3, 0, 8, 7,  6, 11, 7, 7},
    { 2, 9, 0, 6,  2, 10, 9, 3,  6, 11, 7, 7},
    { 6, 11, 7, 7,  2, 10, 3, 5,  10, 8, 3, 2,  10, 9, 8, 3},
    { 7, 2, 3, 6,  6, 2, 7, 5},
    { 7, 0, 8, 6,  7, 6, 0, 1,  6, 2, 0, 3},
    { 2, 7, 6, 6,  2, 3, 7, 3,  0, 1, 9, 7},
    { 1, 6, 2, 6,  1, 8, 6, 0,  1, 9, 8, 3,  8, 7, 6, 3},
    { 10, 7, 6, 6,  10, 1, 7, 1,  1, 3, 7, 3},
    { 10, 7, 6, 6,  1, 7, 10, 4,  1, 8, 7, 2,  1, 0, 8, 3},
    { 0, 3, 7, 3,  0, 7, 10, 0,  0, 10, 9, 6,  6, 10, 7, 5},
    { 7, 6, 10, 3,  7, 10, 8, 4,  8, 10, 9, 6},
    { 6, 8, 4, 6,  11, 8, 6, 5},
    { 3, 6, 11, 6,  3, 0, 6, 1,  0, 4, 6, 3},
    { 8, 6, 11, 6,  8, 4, 6, 3,  9, 0, 1, 7},
    { 9, 4, 6, 3,  9, 6, 3, 0,  9, 3, 1, 6,  11, 3, 6, 5},
    { 6, 8, 4, 6,  6, 11, 8, 3,  2, 10, 1, 7},
    { 1, 2, 10, 7,  3, 0, 11, 5,  0, 6, 11, 2,  0, 4, 6, 3},
    { 4, 11, 8, 6,  4, 6, 11, 3,  0, 2, 9, 5,  2, 10, 9, 3},
    { 10, 9, 3, 1,  10, 3, 2, 6,  9, 4, 3, 1,  11, 3, 6, 5,  4, 6, 3, 1},
    { 8, 2, 3, 6,  8, 4, 2, 1,  4, 6, 2, 3},
    { 0, 4, 2, 5,  4, 6, 2, 3},
    { 1, 9, 0, 7,  2, 3, 4, 1,  2, 4, 6, 6,  4, 3, 8, 6},
    { 1, 9, 4, 3,  1, 4, 2, 4,  2, 4, 6, 6},
    { 8, 1, 3, 6,  8, 6, 1, 0,  8, 4, 6, 3,  6, 10, 1, 3},
    { 10, 1, 0, 3,  10, 0, 6, 4,  6, 0, 4, 6},
    { 4, 6, 3, 1,  4, 3, 8, 6,  6, 10, 3, 1,  0, 3, 9, 5,  10, 9, 3, 1},
    { 10, 9, 4, 3,  6, 10, 4, 5},
    { 4, 9, 5, 7,  7, 6, 11, 7},
    { 0, 8, 3, 7,  4, 9, 5, 7,  11, 7, 6, 7},
    { 5, 0, 1, 6,  5, 4, 0, 3,  7, 6, 11, 7},
    { 11, 7, 6, 7,  8, 3, 4, 5,  3, 5, 4, 2,  3, 1, 5, 3},
    { 9, 5, 4, 7,  10, 1, 2, 7,  7, 6, 11, 7},
    { 6, 11, 7, 7,  1, 2, 10, 7,  0, 8, 3, 7,  4, 9, 5, 7},
    { 7, 6, 11, 7,  5, 4, 10, 5,  4, 2, 10, 2,  4, 0, 2, 3},
    { 3, 4, 8, 6,  3, 5, 4, 2,  3, 2, 5, 1,  10, 5, 2, 5,  11, 7, 6, 7},
    { 7, 2, 3, 6,  7, 6, 2, 3,  5, 4, 9, 7},
    { 9, 5, 4, 7,  0, 8, 6, 1,  0, 6, 2, 6,  6, 8, 7, 6},
    { 3, 6, 2, 6,  3, 7, 6, 3,  1, 5, 0, 5,  5, 4, 0, 3},
    { 6, 2, 8, 1,  6, 8, 7, 6,  2, 1, 8, 1,  4, 8, 5, 5,  1, 5, 8, 1},
    { 9, 5, 4, 7,  10, 1, 6, 5,  1, 7, 6, 2,  1, 3, 7, 3},
    { 1, 6, 10, 6,  1, 7, 6, 2,  1, 0, 7, 1,  8, 7, 0, 5,  9, 5, 4, 7},
    { 4, 0, 10, 1,  4, 10, 5, 6,  0, 3, 10, 1,  6, 10, 7, 5,  3, 7, 10, 1},
    { 7, 6, 10, 3,  7, 10, 8, 4,  5, 4, 10, 5,  4, 8, 10, 1},
    { 6, 9, 5, 6,  6, 11, 9, 1,  11, 8, 9, 3},
    { 3, 6, 11, 6,  0, 6, 3, 4,  0, 5, 6, 2,  0, 9, 5, 3},
    { 0, 11, 8, 6,  0, 5, 11, 0,  0, 1, 5, 3,  5, 6, 11, 3},
    { 6, 11, 3, 3,  6, 3, 5, 4,  5, 3, 1, 6},
    { 1, 2, 10, 7,  9, 5, 11, 1,  9, 11, 8, 6,  11, 5, 6, 6},
    { 0, 11, 3, 6,  0, 6, 11, 2,  0, 9, 6, 1,  5, 6, 9, 5,  1, 2, 10, 7},
    { 11, 8, 5, 1,  11, 5, 6, 6,  8, 0, 5, 1,  10, 5, 2, 5,  0, 2, 5, 1},
    { 6, 11, 3, 3,  6, 3, 5, 4,  2, 10, 3, 5,  10, 5, 3, 1},
    { 5, 8, 9, 6,  5, 2, 8, 0,  5, 6, 2, 3,  3, 8, 2, 5},
    { 9, 5, 6, 3,  9, 6, 0, 4,  0, 6, 2, 6},
    { 1, 5, 8, 1,  1, 8, 0, 6,  5, 6, 8, 1,  3, 8, 2, 5,  6, 2, 8, 1},
    { 1, 5, 6, 3,  2, 1, 6, 5},
    { 1, 3, 6, 1,  1, 6, 10, 6,  3, 8, 6, 1,  5, 6, 9, 5,  8, 9, 6, 1},
    { 10, 1, 0, 3,  10, 0, 6, 4,  9, 5, 0, 5,  5, 6, 0, 1},
    { 0, 3, 8, 7,  5, 6, 10, 7},
    { 10, 5, 6, 7},
    { 11, 5, 10, 6,  7, 5, 11, 5},
    { 11, 5, 10, 6,  11, 7, 5, 3,  8, 3, 0, 7},
    { 5, 11, 7, 6,  5, 10, 11, 3,  1, 9, 0, 7},
    { 10, 7, 5, 6,  10, 11, 7, 3,  9, 8, 1, 5,  8, 3, 1, 3},
    { 11, 1, 2, 6,  11, 7, 1, 1,  7, 5, 1, 3},
    { 0, 8, 3, 7,  1, 2, 7, 1,  1, 7, 5, 6,  7, 2, 11, 6},
    { 9, 7, 5, 6,  9, 2, 7, 0,  9, 0, 2, 3,  2, 11, 7, 3},
    { 7, 5, 2, 1,  7, 2, 11, 6,  5, 9, 2, 1,  3, 2, 8, 5,  9, 8, 2, 1},
    { 2, 5, 10, 6,  2, 3, 5, 1,  3, 7, 5, 3},
    { 8, 2, 0, 6,  8, 5, 2, 0,  8, 7, 5, 3,  10, 2, 5, 5},
    { 9, 0, 1, 7,  5, 10, 3, 1,  5, 3, 7, 6,  3, 10, 2, 6},
    { 9, 8, 2, 1,  9, 2, 1, 6,  8, 7, 2, 1,  10, 2, 5, 5,  7, 5, 2, 1},
    { 1, 3, 5, 5,  3, 7, 5, 3},
    { 0, 8, 7, 3,  0, 7, 1, 4,  1, 7, 5, 6},
    { 9, 0, 3, 3,  9, 3, 5, 4,  5, 3, 7, 6},
    { 9, 8, 7, 3,  5, 9, 7, 5},
    { 5, 8, 4, 6,  5, 10, 8, 1,  10, 11, 8, 3},
    { 5, 0, 4, 6,  5, 11, 0, 0,  5, 10, 11, 3,  11, 3, 0, 3},
    { 0, 1, 9, 7,  8, 4, 10, 1,  8, 10, 11, 6,  10, 4, 5, 6},
    { 10, 11, 4, 1,  10, 4, 5, 6,  11, 3, 4, 1,  9, 4, 1, 5,  3, 1, 4, 1},
    { 2, 5, 1, 6,  2, 8, 5, 0,  2, 11, 8, 3,  4, 5, 8, 5},
    { 0, 4, 11, 1,  0, 11, 3, 6,  4, 5, 11, 1,  2, 11, 1, 5,  5, 1, 11, 1},
    { 0, 2, 5, 1,  0, 5, 9, 6,  2, 11, 5, 1,  4, 5, 8, 5,  11, 8, 5, 1},
    { 9, 4, 5, 7,  2, 11, 3, 7},
    { 2, 5, 10, 6,  3, 5, 2, 4,  3, 4, 5, 2,  3, 8, 4, 3},
    { 5, 10, 2, 3,  5, 2, 4, 4,  4, 2, 0, 6},
    { 3, 10, 2, 6,  3, 5, 10, 2,  3, 8, 5, 1,  4, 5, 8, 5,  0, 1, 9, 7},
    { 5, 10, 2, 3,  5, 2, 4, 4,  1, 9, 2, 5,  9, 4, 2, 1},
    { 8, 4, 5, 3,  8, 5, 3, 4,  3, 5, 1, 6},
    { 0, 4, 5, 3,  1, 0, 5, 5},
    { 8, 4, 5, 3,  8, 5, 3, 4,  9, 0, 5, 5,  0, 3, 5, 1},
    { 9, 4, 5, 7},
    { 4, 11, 7, 6,  4, 9, 11, 1,  9, 10, 11, 3},
    { 0, 8, 3, 7,  4, 9, 7, 5,  9, 11, 7, 2,  9, 10, 11, 3},
    { 1, 10, 11, 3,  1, 11, 4, 0,  1, 4, 0, 6,  7, 4, 11, 5},
    { 3, 1, 4, 1,  3, 4, 8, 6,  1, 10, 4, 1,  7, 4, 11, 5,  10, 11, 4, 1},
    { 4, 11, 7, 6,  9, 11, 4, 4,  9, 2, 11, 2,  9, 1, 2, 3},
    { 9, 7, 4, 6,  9, 11, 7, 2,  9, 1, 11, 1,  2, 11, 1, 5,  0, 8, 3, 7},
    { 11, 7, 4, 3,  11, 4, 2, 4,  2, 4, 0, 6},
    { 11, 7, 4, 3,  11, 4, 2, 4,  8, 3, 4, 5,  3, 2, 4, 1},
    { 2, 9, 10, 6,  2, 7, 9, 0,  2, 3, 7, 3,  7, 4, 9, 3},
    { 9, 10, 7, 1,  9, 7, 4, 6,  10, 2, 7, 1,  8, 7, 0, 5,  2, 0, 7, 1},
    { 3, 7, 10, 1,  3, 10, 2, 6,  7, 4, 10, 1,  1, 10, 0, 5,  4, 0, 10, 1},
    { 1, 10, 2, 7,  8, 7, 4, 7},
    { 4, 9, 1, 3,  4, 1, 7, 4,  7, 1, 3, 6},
    { 4, 9, 1, 3,  4, 1, 7, 4,  0, 8, 1, 5,  8, 7, 1, 1},
    { 4, 0, 3, 3,  7, 4, 3, 5},
    { 4, 8, 7, 7},
    { 9, 10, 8, 5,  10, 11, 8, 3},
    { 3, 0, 9, 3,  3, 9, 11, 4,  11, 9, 10, 6},
    { 0, 1, 10, 3,  0, 10, 8, 4,  8, 10, 11, 6},
    { 3, 1, 10, 3,  11, 3, 10, 5},
    { 1, 2, 11, 3,  1, 11, 9, 4,  9, 11, 8, 6},
    { 3, 0, 9, 3,  3, 9, 11, 4,  1, 2, 9, 5,  2, 11, 9, 1},
    { 0, 2, 11, 3,  8, 0, 11, 5},
    { 3, 2, 11, 7},
    { 2, 3, 8, 3,  2, 8, 10, 4,  10, 8, 9, 6},
    { 9, 10, 2, 3,  0, 9, 2, 5},
    { 2, 3, 8, 3,  2, 8, 10, 4,  0, 1, 8, 5,  1, 10, 8, 1},
    { 1, 10, 2, 7},
    { 1, 3, 8, 3,  9, 1, 8, 5},
    { 0, 9, 1, 7},
    { 0, 3, 8, 7},
    null
  };

  ////////////////////////////////////////////////////////////////
  // contour plane implementation 
  ////////////////////////////////////////////////////////////////

  void generateContourData(boolean iHaveContourVertexesAlready) {

    /*
     * (1) define the plane
     * (2) calculate the grid "pixel" points
     * (3) generate the contours using marching squares
     * (4) 
     * The idea is to first just catalog the vertices, then see what we need to do about them.
     * 
     */

    if (nContours == 0 || nContours > nContourMax)
      nContours = defaultContourCount;
    Logger.info("generateContours:" + nContours);
    getPlanarVectors();
    setPlanarTransform();
    getPlanarOrigin();
    setupMatrix(planarMatrix, planarVectors);

    calcPixelVertexVectors();
    getPixelCounts();
    createPlanarSquares();

    loadPixelData(iHaveContourVertexesAlready);
    createContours();
    triangulateContours();
  }

  // (1) define the plane

  final Point3f planarOrigin = new Point3f();
  final Vector3f[] planarVectors = new Vector3f[3];
  final Vector3f[] unitPlanarVectors = new Vector3f[3];
  final float[] planarVectorLengths = new float[2];
  final Matrix3f matXyzToPlane = new Matrix3f();
  {
    planarVectors[0] = new Vector3f();
    planarVectors[1] = new Vector3f();
    planarVectors[2] = new Vector3f();
    unitPlanarVectors[0] = new Vector3f();
    unitPlanarVectors[1] = new Vector3f();
    unitPlanarVectors[2] = new Vector3f();
  }

  void getPlanarVectors() {
    /*
     * Imagine a parallelpiped defined by our original Vx, Vy, Vz.
     * We pick ONE of these to be our "contour type" defining vector.
     * I call that particular vector Vz here.
     * It is the vector best aligned with the normal to the plane we
     * are interested in visualizing, for which the normal is N.
     * (N is just {a b c} in ax + by + cz + d = 0.)
     *  
     * We want to know what the new Vx' and Vy' are going to be for the
     * planar parallelogram defining our marching "squares".
     * 
     * Vx' = Vx - Vz * (Vx dot N) / (Vz dot N)
     * Vy' = Vy - Vz * (Vy dot N) / (Vz dot N)
     * 
     * Thus, if we start with a rectangular grid and Vz IS N, 
     * then Vx dot N is zero, so Vx' = Vx; if were to poorly choose Vz
     * such that it was perpendicular to N, then Vz dot N would be 0, and
     * our grid would have an infinitely long side. 
     * 
     * For clues, see http://mathworld.wolfram.com/Point-PlaneDistance.html
     * 
     */

    planarVectors[2].set(0, 0, 0);

    if (thePlane == null)
      return; //done already

    Vector3f vZ = volumetricVectors[contourType];
    float vZdotNorm = vZ.dot(thePlaneNormal);
    switch (contourType) {
    case 0: //x
      planarVectors[0].scaleAdd(-volumetricVectors[1].dot(thePlaneNormal)
          / vZdotNorm, vZ, volumetricVectors[1]);
      planarVectors[1].scaleAdd(-volumetricVectors[2].dot(thePlaneNormal)
          / vZdotNorm, vZ, volumetricVectors[2]);
      break;
    case 1: //y
      planarVectors[0].scaleAdd(-volumetricVectors[2].dot(thePlaneNormal)
          / vZdotNorm, vZ, volumetricVectors[2]);
      planarVectors[1].scaleAdd(-volumetricVectors[0].dot(thePlaneNormal)
          / vZdotNorm, vZ, volumetricVectors[0]);
      break;
    case 2: //z
      planarVectors[0].scaleAdd(-volumetricVectors[0].dot(thePlaneNormal)
          / vZdotNorm, vZ, volumetricVectors[0]);
      planarVectors[1].scaleAdd(-volumetricVectors[1].dot(thePlaneNormal)
          / vZdotNorm, vZ, volumetricVectors[1]);
    }
  }

  void setPlanarTransform() {
    planarVectorLengths[0] = planarVectors[0].length();
    planarVectorLengths[1] = planarVectors[1].length();
    unitPlanarVectors[0].normalize(planarVectors[0]);
    unitPlanarVectors[1].normalize(planarVectors[1]);
    unitPlanarVectors[2].cross(unitPlanarVectors[0], unitPlanarVectors[1]);

    setupMatrix(matXyzToPlane, unitPlanarVectors);
    matXyzToPlane.invert();

    float alpha = planarVectors[0].angle(planarVectors[1]);
    Logger.info("planar axes type " + contourType + " axis angle = "
        + (alpha / Math.PI * 180) + " normal=" + unitPlanarVectors[2]);
    for (int i = 0; i < 2; i++)
      Logger.info("planar vectors / lengths:" + planarVectors[i] + " / "
          + planarVectorLengths[i]);
    for (int i = 0; i < 3; i++)
      Logger.info("unit orthogonal plane vectors:" + unitPlanarVectors[i]);
  }

  void getPlanarOrigin() {
    /*
     * just find the minimum value such that all coordinates are positive.
     * note that this may be out of the actual range of data
     * 
     */
    planarOrigin.set(0, 0, 0);
    if (contourVertexCount == 0)
      return;

    float minX = Float.MAX_VALUE;
    float minY = Float.MAX_VALUE;
    planarOrigin.set(contourVertexes[0].vertexXYZ);
    for (int i = 0; i < contourVertexCount; i++) {
      pointVector.set(contourVertexes[i].vertexXYZ);
      xyzToPixelVector(pointVector);
      if (pointVector.x < minX)
        minX = pointVector.x;
      if (pointVector.y < minY)
        minY = pointVector.y;
    }
    planarOrigin.set(pixelPtToXYZ((int) (minX * 1.0001f),
        (int) (minY * 1.0001f)));
    //Logger.info("generatePixelData planarOrigin = " + planarOrigin + ":"
      //  + locatePixel(planarOrigin));
  }

  // (2) calculate the grid points

  int contourVertexCount;
  ContourVertex[] contourVertexes;

  class ContourVertex {
    Point3f vertexXYZ = new Point3f();
    Point3i voxelLocation;
    int[] pixelLocation = new int[2];
    float value;
    int vertexIndex;

    ContourVertex(int x, int y, int z, Point3f vertexXYZ, int vPt) {
      this.vertexXYZ.set(vertexXYZ);
      voxelLocation = new Point3i(x, y, z);
      vertexIndex = vPt;
    }

    void setValue(float value) {
      this.value = value;
      voxelData[voxelLocation.x][voxelLocation.y][voxelLocation.z] = value;
    }

    void setPixelLocation(Point3i pt) {
      pixelLocation[0] = pt.x;
      pixelLocation[1] = pt.y;
    }
  }

  int addContourData(int x, int y, int z, Point3i offsets, Point3f vertexXYZ,
                     float value) {
    if (contourVertexes == null)
      contourVertexes = new ContourVertex[256];
    if (contourVertexCount == contourVertexes.length)
      contourVertexes = (ContourVertex[]) ArrayUtil
          .doubleLength(contourVertexes);
    x += offsets.x;
    y += offsets.y;
    z += offsets.z;
    int vPt = addVertexCopy(vertexXYZ, value);
    contourVertexes[contourVertexCount++] = new ContourVertex(x, y, z,
        vertexXYZ, vPt);
    return vPt;
  } // (3) generate the contours using marching squares

  final int[] pixelCounts = new int[2];
  final Matrix3f planarMatrix = new Matrix3f();
  float[][] pixelData;

  final float[] vertexValues2d = new float[4];
  final Point3f[] contourPoints = new Point3f[4];
  {
    for (int i = 4; --i >= 0;)
      contourPoints[i] = new Point3f();
  }
  final int[] contourPointIndexes = new int[4];
  int squareCountX, squareCountY;

  PlanarSquare[] planarSquares;
  int nSquares;

  class PlanarSquare {
    int[] edgeMask12; //one per contour
    int edgeMask12All;
    int nInside;
    int nOutside;
    int nThrough;
    int contourBits;
    //int x, y;
    //Point3f origin;
    int[] vertexes;
    int[][] intersectionPoints;

    PlanarSquare() {
      edgeMask12 = new int[nContours];
      intersectionPoints = new int[nContours][4];
      vertexes = new int[4];
      edgeMask12All = 0;
      contourBits = 0;
      //this.origin = origin;
      //this.x = x;
      //this.y = y;
    }

    void setIntersectionPoints(int contourIndex, int[] pts) {
      for (int i = 0; i < 4; i++)
        intersectionPoints[contourIndex][i] = pts[i];
    }

    void setVertex(int iV, int pt) {
      if (vertexes[iV] != 0 && vertexes[iV] != pt)
        Logger
            .error("IV IS NOT 0 or pt:" + iV + " " + vertexes[iV] + "!=" + pt);
      vertexes[iV] = pt;
    }

    void addEdgeMask(int contourIndex, int edgeMask4, int insideMask) {
      /*
       * binary abcd abcd vvvv  where abcd is edge intersection mask and
       * vvvv is the inside/outside mask (0-15)
       * the duplication is so that this can be used efficiently either as  
       */
      if (insideMask != 0)
        contourBits |= (1 << contourIndex);
      edgeMask12[contourIndex] = (((edgeMask4 << 4) + edgeMask4) << 4)
          + insideMask;
      edgeMask12All |= edgeMask12[contourIndex];
      if (insideMask == 0)
        ++nOutside;
      else if (insideMask == 0xF)
        ++nInside;
      else
        ++nThrough;
    }
  }

  void getPixelCounts() {
    //skipping the dimension designated by the contourType
    if (thePlane == null)
      return;
    int max = 1;
    for (int i = 0; i < 3; i++) {
      if (i != contourType)
        max = Math.max(max, voxelCounts[i]);
    }
    pixelCounts[0] = pixelCounts[1] = max;
    // just use the maximum value -- this isn't too critical,
    // but we want to have enough, and there were
    // problems with hkl = 110

    //    if (logMessages)
    //Logger.info("getPixelCounts " + pixelCounts[0] + "," + pixelCounts[1]);
  }

  void createPlanarSquares() {
    squareCountX = pixelCounts[0] - 1;
    squareCountY = pixelCounts[1] - 1;

    planarSquares = new PlanarSquare[squareCountX * squareCountY];
    nSquares = 0;
    for (int x = 0; x < squareCountX; x++)
      for (int y = 0; y < squareCountY; y++)
        planarSquares[nSquares++] = new PlanarSquare();
    Logger.info("nSquares = " + nSquares);
  }

  void loadPixelData(boolean iHaveContourVertexesAlready) {
    pixelData = new float[pixelCounts[0]][pixelCounts[1]];
    int x, y;
    Logger.info("loadPixelData haveContourVertices? "
        + iHaveContourVertexesAlready);
    contourPlaneMinimumValue = Float.MAX_VALUE;
    contourPlaneMaximumValue = -Float.MAX_VALUE;
    for (int i = 0; i < contourVertexCount; i++) {
      ContourVertex c = contourVertexes[i];
      Point3i pt = locatePixel(c.vertexXYZ);
      c.setPixelLocation(pt);
      float value;
      if (iHaveContourVertexesAlready) {
        value = c.value;
      } else {
        value = lookupInterpolatedVoxelValue(c.vertexXYZ);
        c.setValue(value);
      }
      if (value < contourPlaneMinimumValue)
        contourPlaneMinimumValue = value;
      if (value > contourPlaneMaximumValue)
        contourPlaneMaximumValue = value;
      if ((x = pt.x) >= 0 && x < pixelCounts[0] && (y = pt.y) >= 0
          && y < pixelCounts[1]) {
        pixelData[x][y] = value;
        if (x != squareCountX && y != squareCountY)
          planarSquares[x * squareCountY + y].setVertex(0, c.vertexIndex);
        if (x != 0 && y != squareCountY)
          planarSquares[(x - 1) * squareCountY + y].setVertex(1, c.vertexIndex);
        if (y != 0 && x != squareCountX)
          planarSquares[x * squareCountY + y - 1].setVertex(3, c.vertexIndex);
        if (y != 0 && x != 0)
          planarSquares[(x - 1) * squareCountY + y - 1].setVertex(2,
              c.vertexIndex);
      } else {
        Logger.error("loadPixelData out of bounds: " + pt.x + " " + pt.y + "?");
      }
    }
  }

  int contourIndex;

  void createContours() {
    colorFractionBase = defaultColorFractionBase;
    colorFractionRange = defaultColorFractionRange;
    setMapRanges();
    float min = valueMappedToRed;
    float max = valueMappedToBlue;
    float diff = max - min;
    Logger.info("generateContourData min=" + min + " max=" + max
        + " nContours=" + nContours);
    for (int i = 0; i < nContours; i++) {
      contourIndex = i;
      float cutoff = min + (i * 1f / nContours) * diff;
      /*
       * cutoffs right near zero cause problems, so we adjust just a tad
       * 
       */
      generateContourData(cutoff);
    }
  }

  void generateContourData(float contourCutoff) {

    /*
     * Y
     *  3 ---2---- 2
     *  |          |           
     *  |          |
     *  3          1
     *  |          |
     *  0 ---0---- 1  X
     */

    int[][] isoPointIndexes2d = new int[squareCountY][4];
    for (int i = squareCountY; --i >= 0;)
      isoPointIndexes2d[i][0] = isoPointIndexes2d[i][1] = isoPointIndexes2d[i][2] = isoPointIndexes2d[i][3] = -1; //new int[4];

    if (Math.abs(contourCutoff) < 0.0001)
      contourCutoff = (contourCutoff <= 0 ? -0.0001f : 0.0001f);
    int insideCount = 0, outsideCount = 0, contourCount = 0;
    for (int x = squareCountX; --x >= 0;) {
      for (int y = squareCountY; --y >= 0;) {
        int[] pixelPointIndexes = propagateNeighborPointIndexes2d(x, y,
            isoPointIndexes2d);
        int insideMask = 0;
        for (int i = 4; --i >= 0;) {
          Point3i offset = squareVertexOffsets[i];
          float vertexValue = pixelData[x + offset.x][y + offset.y];
          vertexValues2d[i] = vertexValue;
          if (isInside2d(vertexValue, contourCutoff))
            insideMask |= 1 << i;
        }
        if (insideMask == 0) {
          ++outsideCount;
          continue;
        }
        if (insideMask == 0x0F) {
          ++insideCount;
          planarSquares[x * squareCountY + y]
              .addEdgeMask(contourIndex, 0, 0x0F);
          continue;
        }
        ++contourCount;
        processOneQuadrilateral(insideMask, contourCutoff, pixelPointIndexes,
            x, y);
      }
    }

/*    if (logMessages)
      Logger.info("contourCutoff=" + contourCutoff + " pixel squares="
          + squareCountX + "," + squareCountY + "," + " total="
          + (squareCountX * squareCountY) + "\n" + " insideCount="
          + insideCount + " outsideCount=" + outsideCount + " contourCount="
          + contourCount + " total="
          + (insideCount + outsideCount + contourCount));
          */
    
  }

  boolean isInside2d(float voxelValue, float max) {
    return (max > 0 && voxelValue >= max) || (max <= 0 && voxelValue <= max);
  }

  final int[] nullNeighbor2d = { -1, -1, -1, -1 };

  int[] propagateNeighborPointIndexes2d(int x, int y, int[][] isoPointIndexes2d) {

    // propagates only the intersection point -- one in the case of a square

    int[] pixelPointIndexes = isoPointIndexes2d[y];

    boolean noXNeighbor = (x == squareCountX - 1);
    // the x neighbor is myself from my last pass through here
    if (noXNeighbor) {
      pixelPointIndexes[0] = -1;
      pixelPointIndexes[1] = -1;
      pixelPointIndexes[2] = -1;
      pixelPointIndexes[3] = -1;
    } else {
      pixelPointIndexes[1] = pixelPointIndexes[3];
    }

    // from my y neighbor
    boolean noYNeighbor = (y == squareCountY - 1);
    pixelPointIndexes[2] = (noYNeighbor ? -1 : isoPointIndexes2d[y + 1][0]);

    // these must always be calculated
    pixelPointIndexes[0] = -1;
    pixelPointIndexes[3] = -1;
    return pixelPointIndexes;
  }

  void processOneQuadrilateral(int insideMask, float cutoff,
                               int[] pixelPointIndexes, int x, int y) {
    int edgeMask = insideMaskTable2d[insideMask];
    planarSquares[x * squareCountY + y].addEdgeMask(contourIndex, edgeMask,
        insideMask);
    for (int iEdge = 4; --iEdge >= 0;) {
      if ((edgeMask & (1 << iEdge)) == 0) {
        continue;
      }
      if (pixelPointIndexes[iEdge] >= 0)
        continue; // propagated from neighbor
      int vertexA = edgeVertexes2d[2 * iEdge];
      int vertexB = edgeVertexes2d[2 * iEdge + 1];
      float valueA = vertexValues2d[vertexA];
      float valueB = vertexValues2d[vertexB];
      if (thePlane == null) //contouring f(x,y)
        calcVertexPoints3d(x, y, vertexA, vertexB);
      else
        calcVertexPoints2d(x, y, vertexA, vertexB);
      calcContourPoint(cutoff, valueA, valueB, contourPoints[iEdge]);
      pixelPointIndexes[iEdge] = addVertexCopy(contourPoints[iEdge], cutoff);
    }
    //this must be a square that is involved in this particular contour
    planarSquares[x * squareCountY + y].setIntersectionPoints(contourIndex,
        pixelPointIndexes);
  }

  final Point3f pixelOrigin = new Point3f();
  final Point3f pixelT = new Point3f();

  void calcVertexPoints2d(int x, int y, int vertexA, int vertexB) {
    pixelOrigin.scaleAdd(x, planarVectors[0], planarOrigin);
    pixelOrigin.scaleAdd(y, planarVectors[1], pixelOrigin);
    pointA.add(pixelOrigin, pixelVertexVectors[vertexA]);
    pointB.add(pixelOrigin, pixelVertexVectors[vertexB]);
  }

  void calcVertexPoints3d(int x, int y, int vertexA, int vertexB) {
    contourLocateXYZ(x + squareVertexOffsets[vertexA].x, y
        + squareVertexOffsets[vertexA].y, pointA);
    contourLocateXYZ(x + squareVertexOffsets[vertexB].x, y
        + squareVertexOffsets[vertexB].y, pointB);
  }

  void contourLocateXYZ(int ix, int iy, Point3f pt) {
    int i = findContourVertex(ix, iy);
    if (i < 0) {
      pt.x = Float.NaN;
      return;
    }
    ContourVertex c = contourVertexes[i];
    pt.set(c.vertexXYZ);
  }

  int findContourVertex(int ix, int iy) {
    for (int i = 0; i < contourVertexCount; i++) {
      if (contourVertexes[i].pixelLocation[0] == ix
          && contourVertexes[i].pixelLocation[1] == iy)
        return i;
    }
    return -1;
  }

  float calcContourPoint(float cutoff, float valueA, float valueB,
                         Point3f contourPoint) {

    float diff = valueB - valueA;
    float fraction = (cutoff - valueA) / diff;
    edgeVector.sub(pointB, pointA);
    contourPoint.scaleAdd(fraction, edgeVector, pointA);
    return fraction;
  }

  Vector3f[] pixelVertexVectors = new Vector3f[4];

  void calcPixelVertexVectors() {
    for (int i = 4; --i >= 0;)
      pixelVertexVectors[i] = calcPixelVertexVector(squareVertexVectors[i]);
  }

  Vector3f calcPixelVertexVector(Vector3f squareVector) {
    Vector3f v = new Vector3f();
    planarMatrix.transform(squareVector, v);
    return v;
  }

  void triangulateContours() {
    meshData.vertexColors = new int[meshData.vertexCount];

    for (int i = 0; i < nContours; i++) {
      if (thisContour <= 0 || thisContour == i + 1)
        createContourTriangles(i);
    }
  }

  void createContourTriangles(int contourIndex) {
    for (int i = 0; i < nSquares; i++) {
      triangulateContourSquare(i, contourIndex);
    }
  }

  void triangulateContourSquare(int squareIndex, int contourIndex) {
    PlanarSquare square = planarSquares[squareIndex];
    int edgeMask0 = square.edgeMask12[contourIndex] & 0x00FF;
    if (edgeMask0 == 0) // all outside
      return;

    // unnecessary inside square?
    // full square and next contour is also a full square there
    if (edgeMask0 == 15 && contourIndex + 1 < nContours
        && square.edgeMask12[contourIndex + 1] == 15)
      return;

    //still working here.... not efficient; stubbornly trying to avoid just
    // writing the damn triangle table.
    boolean isOK = true;
    int edgeMask = edgeMask0;
    if (contourIndex < nContours - 1) {
      edgeMask0 = square.edgeMask12[contourIndex + 1];
      if (edgeMask0 != 0x0F) {
        isOK = false;
        if (((edgeMask ^ edgeMask0) & 0xF0) == 0) {
          isOK = false;
        }
        edgeMask &= 0x0FF;
        edgeMask ^= edgeMask0 & 0x0F0F;
      }
    }
    if (contourIndex > 0 && edgeMask == 0)
      return;
    fillSquare(square, contourIndex, edgeMask, false);
    if (!isOK) // a lazy hack instead of really figuring out the order
      fillSquare(square, contourIndex, edgeMask, true);
  }

  int[] triangleVertexList = new int[20];

  void fillSquare(PlanarSquare square, int contourIndex, int edgeMask,
                  boolean reverseWinding) {
    int vPt = 0;
    boolean flip = reverseWinding;
    int nIntersect = 0;
    boolean newIntersect;
    for (int i = 0; i < 4; i++) {
      newIntersect = false;
      if ((edgeMask & (1 << i)) != 0) {
        triangleVertexList[vPt++] = square.vertexes[i];
      }
      //order here needs to be considered for when Edges(A)==Edges(B)
      //for proper winding -- isn't up to snuff

      if (flip && (edgeMask & (1 << (8 + i))) != 0) {
        nIntersect++;
        newIntersect = true;
        triangleVertexList[vPt++] = square.intersectionPoints[contourIndex + 1][i];
      }
      if ((edgeMask & (1 << (4 + i))) != 0) {
        nIntersect++;
        newIntersect = true;
        triangleVertexList[vPt++] = square.intersectionPoints[contourIndex][i];
      }
      if (!flip && (edgeMask & (1 << (8 + i))) != 0) {
        nIntersect++;
        newIntersect = true;
        triangleVertexList[vPt++] = square.intersectionPoints[contourIndex + 1][i];
      }
      if (nIntersect == 2 && newIntersect)
        flip = !flip;
    }
    /*
     Logger.debug("\nfillSquare (" + square.x + " " + square.y + ") "
     + contourIndex + " " + binaryString(edgeMask) + "\n");
     Logger.debug("square vertexes:" + dumpIntArray(square.vertexes, 4));
     Logger.debug("square inters. pts:"
     + dumpIntArray(square.intersectionPoints[contourIndex], 4));
     Logger.debug(dumpIntArray(triangleVertexList, vPt));
     */
    createTriangleSet(vPt);
  }

  void createTriangleSet(int nVertex) {
    int k = triangleVertexList[1];
    for (int i = 2; i < nVertex; i++) {
      meshData.addTriangleCheck(triangleVertexList[0], k, triangleVertexList[i], 7);
      k = triangleVertexList[i];
    }
  }

  final static Point3i[] squareVertexOffsets = { new Point3i(0, 0, 0),
      new Point3i(1, 0, 0), new Point3i(1, 1, 0), new Point3i(0, 1, 0) };

  final static Vector3f[] squareVertexVectors = { new Vector3f(0, 0, 0),
      new Vector3f(1, 0, 0), new Vector3f(1, 1, 0), new Vector3f(0, 1, 0) };

  final static byte edgeVertexes2d[] = { 0, 1, 1, 2, 2, 3, 3, 0 };

  final static byte insideMaskTable2d[] = { 0, 9, 3, 10, 6, 15, 5, 12, 12, 5,
      15, 6, 10, 3, 9, 0 };

  // position in the table corresponds to the binary equivalent of which corners are inside
  // for example, 0th is completely outside; 15th is completely inside;
  // the 4th entry (0b0100; 2**3), corresponding to only the third corner inside, is 6 (0b1100). 
  // Bits 2 and 3 are set, so edges 2 and 3 intersect the contour.

  ////////// debug utility methods /////////

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

  void voxelPtToXYZ(int x, int y, int z, Point3f pt) {
    pt.scaleAdd(x, volumetricVectors[0], volumetricOrigin);
    pt.scaleAdd(y, volumetricVectors[1], pt);
    pt.scaleAdd(z, volumetricVectors[2], pt);
    return;
  }

  float scaleByVoxelVector(Vector3f vector, int voxelVectorIndex) {
    // ORTHOGONAL ONLY!!! -- required for creating planes
    return (vector.dot(unitVolumetricVectors[voxelVectorIndex]) / volumetricVectorLengths[voxelVectorIndex]);
  }

  void xyzToVoxelPt(Point3f point, Point3f pt2) {
    pointVector.set(point);
    pointVector.sub(volumetricOrigin);
    pt2.x = scaleByVoxelVector(pointVector, 0);
    pt2.y = scaleByVoxelVector(pointVector, 1);
    pt2.z = scaleByVoxelVector(pointVector, 2);
  }

  void xyzToVoxelPt(float x, float y, float z, Point3i pt2) {
    pointVector.set(x, y, z);
    pointVector.sub(volumetricOrigin);
    ptXyzTemp.x = scaleByVoxelVector(pointVector, 0);
    ptXyzTemp.y = scaleByVoxelVector(pointVector, 1);
    ptXyzTemp.z = scaleByVoxelVector(pointVector, 2);
    pt2.set((int) ptXyzTemp.x, (int) ptXyzTemp.y, (int) ptXyzTemp.z);
  }

  Point3f pixelPtToXYZ(int x, int y) {
    Point3f ptXyz = new Point3f();
    ptXyz.scaleAdd(x, planarVectors[0], planarOrigin);
    ptXyz.scaleAdd(y, planarVectors[1], ptXyz);
    return ptXyz;
  }

  final Point3i ptiTemp = new Point3i();

  Point3i locatePixel(Point3f ptXyz) {
    pointVector.set(ptXyz);
    xyzToPixelVector(pointVector);
    ptiTemp.x = (int) (pointVector.x + 0.5f);
    //NOTE: fails if negative -- (int) (-0.9 + 0.5) = (int) (-0.4) = 0
    ptiTemp.y = (int) (pointVector.y + 0.5f);
    return ptiTemp;
  }

  void xyzToPixelVector(Vector3f vector) {
    //  factored for nonorthogonality; assumes vector is IN the plane already
    vector.sub(vector, planarOrigin);
    matXyzToPlane.transform(vector);
    vector.x /= planarVectorLengths[0];
    vector.y /= planarVectorLengths[1];
  }

  ///////////file reading //////////
  
  

  String line;
  int[] next = new int[1];
  
  String[] getTokens() {
    return Parser.getTokens(line, 0);
  }

  float parseFloat() {
    return Parser.parseFloat(line, next);
  }

  float parseFloat(String s) {
    next[0] = 0;
    return Parser.parseFloat(s, next);
  }

  float parseFloatNext(String s) {
    return Parser.parseFloat(s, next);
  }

  int parseInt() {
    return Parser.parseInt(line, next);
  }
  
  int parseInt(String s) {
    next[0] = 0;
    return Parser.parseInt(s, next);
  }
  
  int parseIntNext(String s) {
    return Parser.parseInt(s, next);
  }
  
  int parseInt(String s, int iStart) {
    next[0] = iStart;
    return Parser.parseInt(s, next);
  }
}

class VolumeData {
 
  final Point3f volumetricOrigin = new Point3f();
  final Vector3f[] volumetricVectors = new Vector3f[3];
  final int[] voxelCounts = new int[3];
  float[][][] voxelData;

  VolumeData() {   
    volumetricVectors[0] = new Vector3f();
    volumetricVectors[1] = new Vector3f();
    volumetricVectors[2] = new Vector3f();
  }
  
  void setVoxelData(float[][][] voxelData) {
    this.voxelData = voxelData;
  }
  
}

class JvxlData {
  String jvxlFileHeader;
  String jvxlExtraLine;
  String jvxlDefinitionLine;
  String jvxlSurfaceData;
  String jvxlEdgeData;
  String jvxlColorData;
  String jvxlInfoLine;

  Point4f jvxlPlane;

  int jvxlCompressionRatio;
  boolean isJvxlPrecisionColor;

  boolean isContoured;
  boolean isBicolorMap;
  float mappedDataMin;
  float mappedDataMax;
  float valueMappedToRed;
  float valueMappedToBlue;
  float cutoff;
  int nBytes;
  int nContours;  

  String[] title;
  
  JvxlData() {
    
  }
  
}

class MeshData {
  static int SEED_COUNT = 25;
  
  int polygonCount;
  Point3f[] vertices;
  int[] vertexColors;
  int vertexCount;
  float[] vertexValues;
  int[][] polygonIndexes;
  
  void clear(String meshType) {
    vertexCount = polygonCount = 0;
    vertices = null;
    polygonIndexes = null;
  }
  
  int addVertexCopy(Point3f vertex, float value) {
    if (vertexCount == 0)
      vertexValues = new float[SEED_COUNT];
    else if (vertexCount >= vertexValues.length)
      vertexValues = (float[]) ArrayUtil.doubleLength(vertexValues);
    vertexValues[vertexCount] = value;
    return addVertexCopy(vertex);
  }

  int addVertexCopy(Point3f vertex) {
    if (vertexCount == 0)
      vertices = new Point3f[SEED_COUNT];
    else if (vertexCount == vertices.length)
      vertices = (Point3f[]) ArrayUtil.doubleLength(vertices);
    vertices[vertexCount] = new Point3f(vertex);
    //Logger.debug("mesh.addVertexCopy " + vertexCount + vertex +vertices[vertexCount]);
    return vertexCount++;
  }

  void addTriangleCheck(int vertexA, int vertexB, int vertexC, int check) {
  if (vertexValues != null && (Float.isNaN(vertexValues[vertexA])||Float.isNaN(vertexValues[vertexB])||Float.isNaN(vertexValues[vertexC])))
    return;
  if (Float.isNaN(vertices[vertexA].x)||Float.isNaN(vertices[vertexB].x)||Float.isNaN(vertices[vertexC].x))
    return;
  if (polygonCount == 0)
    polygonIndexes = new int[SEED_COUNT][];
  else if (polygonCount == polygonIndexes.length)
    polygonIndexes = (int[][]) ArrayUtil.doubleLength(polygonIndexes);
  polygonIndexes[polygonCount++] = new int[] {vertexA, vertexB, vertexC, check};
 }
}

