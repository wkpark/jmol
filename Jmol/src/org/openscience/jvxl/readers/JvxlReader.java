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

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point4f;
import java.io.BufferedReader;

import org.jmol.util.Logger;
import org.openscience.jvxl.util.*;
import org.openscience.jvxl.data.JvxlData;
import org.openscience.jvxl.data.VolumeData;

class JvxlReader extends VolumeFileReader {

  JvxlReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
    isJvxl = true;
    isXLowToHigh = false;
  }

  protected static void jvxlUpdateInfo(JvxlData jvxlData, String[] title, int nBytes) {
    jvxlData.title = title;
    jvxlData.nBytes = nBytes;
    jvxlData.jvxlDefinitionLine = jvxlGetDefinitionLine(jvxlData, false);
    jvxlData.jvxlInfoLine = jvxlGetDefinitionLine(jvxlData, true);
  }

  //// methods used for reading any file format, but creating a JVXL file

  /////////////reading the format///////////

  private int surfaceDataCount;
  private int edgeDataCount;
  private int colorDataCount;

  protected void readData(boolean isMapData) {
    super.readData(isMapData);
    if (isMapData)
      try {
        int nPoints = nPointsX * nPointsY * nPointsZ;
        gotoData(params.fileIndex - 1, nPoints);
        jvxlSkipData(nPoints, false);
      } catch (Exception e) {
        Logger.error(e.toString());
        throw new NullPointerException();
      }
    else
      readVolumetricData(isMapData);
    Logger.info("Read " + nPointsX + " x " + nPointsY + " x " + nPointsZ
        + " data points");
    strFractionTemp = jvxlEdgeDataRead;
    fractionPtr = 0;
  }


  protected void readVolumetricData(boolean isMapData) {
    initializeVolumetricData();
    if (nPointsX <= 0 || nPointsY <= 0 || nPointsZ <= 0)
      return;
    try {
      gotoData(params.fileIndex - 1, nPointsX * nPointsY * nPointsZ);
      readVoxelData(isMapData);
      if (edgeDataCount > 0)
        jvxlEdgeDataRead = jvxlReadData("edge", edgeDataCount);
      if (colorDataCount > 0)
        jvxlColorDataRead = jvxlReadData("color", colorDataCount);
    } catch (Exception e) {
      Logger.error(e.toString());
      throw new NullPointerException();
    }
  }
  
  private int nThisValue;
  private boolean thisInside;
  
  protected void initializeVoxelData() {
    thisInside = !params.isContoured;
    if (params.insideOut)
      thisInside = !thisInside;
    nThisValue = 0;
  }
  
  protected void readVoxelData(boolean isMapDataIgnored) throws Exception {
    initializeVoxelData();
    //calls VolumeFileReader.readVoxelData; no mapping allowed
    super.readVoxelData(false);
  }

  // #comments (optional)
  // info line1
  // info line2
  // -na originx originy originz   [ANGSTROMS/BOHR] optional; BOHR assumed
  // n1 x y z
  // n2 x y z
  // n3 x y z
  // a1 a1.0 x y z
  // a2 a2.0 x y z
  // a3 a3.0 x y z
  // a4 a4.0 x y z 
  // etc. -- na atoms
  // -ns 35 90 35 90 Jmol voxel format version 1.0
  // # more comments
  // cutoff +/-nEdges +/-nVertices [more here]
  // integer inside/outside edge data
  // ascii-encoded fractional edge data
  // ascii-encoded fractional color data
  // # optional comments

  protected void readTitleLines() throws Exception {
    jvxlFileHeaderBuffer = new StringBuffer();
    skipComments(true);
    int nLines = 1;
    while (nLines <= 2) {
      if (line == null || line.length() == 0)
        line = "Line " + nLines;
      jvxlFileHeaderBuffer.append(line).append('\n');
      if (nLines++ == 1)
        line = br.readLine();
    }
  }

  
  /**
   * checks an atom line for "ANGSTROMS", possibly overriding the data's 
   * natural units, BOHR (similar to Gaussian CUBE files).
   * 
   * @param isXLowToHigh
   * @param isAngstroms
   * @param strAtomCount
   * @param atomLine
   * @param bs
   * @return  isAngstroms
   */
  protected static boolean jvxlCheckAtomLine(boolean isXLowToHigh, boolean isAngstroms,
                                   String strAtomCount, String atomLine,
                                   StringBuffer bs) {
    if (strAtomCount != null) {
      int atomCount = Parser.parseInt(strAtomCount);
      if (atomCount == Integer.MIN_VALUE) {
        atomCount = 0;
        atomLine = " " + atomLine.substring(atomLine.indexOf(" ") + 1);
      } else {
        String s = "" + atomCount;
        atomLine = atomLine.substring(atomLine.indexOf(s) + s.length());
      }
      bs.append((isXLowToHigh ? "+" : "-") + Math.abs(atomCount));
    }
    int i = atomLine.indexOf("ANGSTROM");
    if (isAngstroms && i < 0)
      atomLine += " ANGSTROMS";
    else if (atomLine.indexOf("ANGSTROMS") >= 0)
      isAngstroms = true;
    i = atomLine.indexOf("BOHR");
    if (!isAngstroms && i < 0)
      atomLine += " BOHR";
    bs.append(atomLine).append('\n');
    return isAngstroms;
  }
  
  protected void readAtomCountAndOrigin() throws Exception {
      skipComments(true);
      String atomLine = line;
      String[] tokens = Parser.getTokens(atomLine, 0);
      isXLowToHigh = false;
      negativeAtomCount = true;
      atomCount = 0;
      if (tokens[0] == "-0") {
      } else if (tokens[0].charAt(0) == '+'){
        isXLowToHigh = true;
        atomCount = parseInt(tokens[0].substring(1));
      } else {
        atomCount = -parseInt(tokens[0]);
      }
      volumetricOrigin.set(parseFloat(tokens[1]), parseFloat(tokens[2]), parseFloat(tokens[3]));
      isAngstroms = jvxlCheckAtomLine(isXLowToHigh, isAngstroms, null, atomLine, jvxlFileHeaderBuffer);
      if (!isAngstroms)
        volumetricOrigin.scale(ANGSTROMS_PER_BOHR);
  }

  protected static void jvxlReadAtoms(BufferedReader br, StringBuffer bs, int atomCount,
                            VolumeData v) throws Exception {
    //mostly ignored
    for (int i = 0; i < atomCount; ++i)
      bs.append(br.readLine() + "\n");
    //if (atomCount == 0)
      //jvxlAddDummyAtomList(v, bs);
  }

  protected int readExtraLine() throws Exception {
    line = br.readLine();
    Logger.info("Reading extra JVXL information line: " + line);
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

  private void jvxlReadDefinitionLine(boolean showMsg) throws Exception {
    skipComments(false);
    if (showMsg)
      Logger.info("reading jvxl data set: " + line);

    jvxlCutoff = parseFloat(line);
    Logger.info("JVXL read: cutoff " + jvxlCutoff);

    //  cutoff       nInts     (+/-)bytesEdgeData (+/-)bytesColorData
    //               param1              param2         param3    
    //                 |                   |              |
    //   when          |                   |        >  0 ==> jvxlDataIsColorMapped
    //   when          |                   |       == -1 ==> not color mapped
    //   when          |                   |        < -1 ==> jvxlDataIsPrecisionColor    
    //   when        == -1     &&   == -1 ==> noncontoured plane
    //   when        == -1     &&   == -2 ==> contourable plane
    //   when        < -1*     &&    >  0 ==> contourable functionXY
    //   when        > 0       &&    <  0 ==> jvxlDataisBicolorMap

    // * nInts saved as -1 - nInts
    
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
        params.thePlane = new Point4f(parseFloat(), parseFloat(), parseFloat(),
            parseFloat());
      } catch (Exception e) {
        Logger
            .error("Error reading 4 floats for PLANE definition -- setting to 0 0 1 0  (z=0)");
        params.thePlane = new Point4f(0, 0, 1, 0);
      }
      Logger.info("JVXL read: {" + params.thePlane.x + " " + params.thePlane.y
          + " " + params.thePlane.z + " " + params.thePlane.w + "}");
    } else {
      params.thePlane = null;
    }
    if (param1 < 0 && param2 != -1) {
      // contours are defined (possibly overridden -- this is just a display option
      // could be plane or functionXY
      params.isContoured = (param3 != 0);
      int nContoursRead = parseInt();
      if (params.nContours == 0 && nContoursRead != Integer.MIN_VALUE
          && nContoursRead != 0) {
        params.nContours = nContoursRead;
        Logger.info("JVXL read: contours " + params.nContours);
      }
    } else {
      params.isContoured = false;
    }

    jvxlDataIsPrecisionColor = (param1 == -1 && param2 == -2 || param3 < 0);
    params.isBicolorMap = (param1 > 0 && param2 < 0);
    jvxlDataIsColorMapped = (param3 != 0);
    jvxlDataIs2dContour = (jvxlDataIsColorMapped && params.isContoured);

    if (params.isBicolorMap || params.colorBySign)
      jvxlCutoff = 0;
    surfaceDataCount = (param1 < -1 ? -1 - param1 : param1 > 0 ? param1 : 0);
    //prior to JVXL 1.1 (4/2007), this number counts the bytes of integer data.
    //after that, the number of integers, for the progressive reader
    
    if (param1 == -1)
      edgeDataCount = 0; //plane
    else
      edgeDataCount = (param2 < -1 ? -param2 : param2 > 0 ? param2 : 0);
    colorDataCount = (params.isBicolorMap ? -param2 : param3 < -1 ? -param3
        : param3 > 0 ? param3 : 0);
    if (params.colorBySign)
      params.isBicolorMap = true;
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
        params.mappedDataMin = dataMin;
        params.mappedDataMax = dataMax;
        Logger.info("JVXL read: data min/max: " + params.mappedDataMin + "/"
            + params.mappedDataMax);
      }
      if (!params.rangeDefined)
        if (!Float.isNaN(red) && !Float.isNaN(blue)) {
          if (red == 0 && blue == 0) {
            //set standard -1/1; bit of a hack
            red = -1;
            blue = 1;
          }
          params.valueMappedToRed = red;
          params.valueMappedToBlue = blue;
          params.rangeDefined = true;
        } else {
          params.valueMappedToRed = 0f;
          params.valueMappedToBlue = 1f;
          params.rangeDefined = true;
        }
      Logger.info("JVXL read: color red/blue: " + params.valueMappedToRed + "/"
          + params.valueMappedToBlue);
    }
    jvxlData.valueMappedToRed = params.valueMappedToRed;
    jvxlData.valueMappedToBlue = params.valueMappedToBlue;
    jvxlData.mappedDataMin = params.mappedDataMin;
    jvxlData.mappedDataMax = params.mappedDataMax;
  }

  private String jvxlReadData(String type, int nPoints) {
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

  private static String jvxlUncompressString(String data) {
    if (data.indexOf("~") < 0)
      return data;
    String dataOut = "";
    char chLast = '\0';
    int[] next = new int[1];
    for (int i = 0; i < data.length(); i++) {
      char ch = data.charAt(i);
      if (ch == '~') {
        next[0] = ++i;
        int nChar = Parser.parseInt(data, next);
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

  protected float getNextVoxelValue(StringBuffer sb) throws Exception {

    //called by VolumeFileReader.readVoxelData

    if (surfaceDataCount <= 0)
      return 0f; //unnecessary -- probably a plane
    if (nThisValue == 0) {
      nThisValue = parseInt();
      if (nThisValue == Integer.MIN_VALUE) {
        line = br.readLine();
        if (line == null || (nThisValue = parseInt(line)) == Integer.MIN_VALUE) {
          if (!endOfData)
            Logger.error("end of file in JvxlReader?" + " line=" + line);
          endOfData = true;
          nThisValue = 10000;
          //throw new NullPointerException();
        } else if (sb != null){
          sb.append(line).append('\n');
        }
      }
      thisInside = !thisInside;
      ++jvxlNSurfaceInts;
    }
    --nThisValue;
    return (thisInside ? 1f : 0f);
  }

  protected static void setSurfaceInfo(JvxlData jvxlData, Point4f thePlane, int nSurfaceInts, StringBuffer surfaceData) {
    jvxlData.jvxlSurfaceData = surfaceData.toString();
    jvxlData.jvxlPlane = thePlane;
    jvxlData.nSurfaceInts = nSurfaceInts;
  }
  
  protected float readSurfacePoint(float cutoff, boolean isCutoffAbsolute, float valueA,
                         float valueB, Point3f pointA, Vector3f edgeVector) {
    float fraction;
    if (edgeDataCount <= 0)
      return super.readSurfacePoint(cutoff, isCutoffAbsolute, valueA, valueB,
          pointA, edgeVector);
    fraction = jvxlGetNextFraction(edgeFractionBase, edgeFractionRange, 0.5f);
    surfacePoint.scaleAdd(fraction, edgeVector, pointA);
    return fraction;
  }

  private int fractionPtr;
  private String strFractionTemp = "";

  private float jvxlGetNextFraction(int base, int range, float fracOffset) {
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

  protected String readColorData() {
    // overloads VoxelReader
    // standard jvxl file read for color 

    fractionPtr = 0;
    int vertexCount = meshData.vertexCount;
    int[] colors = meshData.vertexColors;
    fractionData = new StringBuffer();
    strFractionTemp = (isJvxl ? jvxlColorDataRead : "");
    if (isJvxl && strFractionTemp.length() == 0) {
      Logger
          .error("You cannot use JVXL data to map onto OTHER data, because it only containts the data for one surface. Use ISOSURFACE \"file.jvxl\" not ISOSURFACE .... MAP \"file.jvxl\".");
      return "";
    }
    fractionPtr = 0;
    Logger.info("JVXL reading color data base/range: " + params.mappedDataMin
        + "/" + params.mappedDataMax + " for " + vertexCount + " vertices."
        + " using encoding keys " + colorFractionBase + " "
        + colorFractionRange);
    Logger.info("mapping red-->blue for " + params.valueMappedToRed + " to "
        + params.valueMappedToBlue + " colorPrecision:"
        + jvxlDataIsPrecisionColor);

    float min = (params.mappedDataMin == Float.MAX_VALUE ? defaultMappedDataMin
        : params.mappedDataMin);
    float range = (params.mappedDataMin == Float.MAX_VALUE ? defaultMappedDataMax
        : params.mappedDataMax)
        - min;
    float colorRange = params.valueMappedToBlue - params.valueMappedToRed;
    float contourPlaneMinimumValue = Float.MAX_VALUE;
    float contourPlaneMaximumValue = -Float.MAX_VALUE;
    if (colors == null || colors.length < vertexCount)
      meshData.vertexColors = colors = new int[vertexCount];
    int n = (params.isContoured ? contourVertexCount : vertexCount);
    String data = jvxlColorDataRead;
    int cpt = 0;
    for (int i = 0; i < n; i++) {
      float fraction, value;
      if (jvxlDataIsPrecisionColor) {
        // this COULD be an option for mapped surfaces; 
        // necessary for planes.
        // precision is used for FULL-data range encoding, allowing full
        // treatment of JVXL files as though they were CUBE files.
        // the two parts of the "double-character-precision" value
        // are in separate lines, separated by n characters.
        fraction = jvxlFractionFromCharacter2(data.charAt(cpt), data.charAt(cpt
            + n), colorFractionBase, colorFractionRange);
        value = min + fraction * range;
      } else {
        // my original encoding scheme
        // low precision only allows for mapping relative to the defined color range
        fraction = jvxlFractionFromCharacter(data.charAt(cpt),
            colorFractionBase, colorFractionRange, 0.5f);
        value = params.valueMappedToRed + fraction * colorRange;
      }
      ++cpt;
      if (value < contourPlaneMinimumValue)
        contourPlaneMinimumValue = value;
      if (value > contourPlaneMaximumValue)
        contourPlaneMaximumValue = value;

      if (params.isContoured) {
        marchingSquares.setContourData(i, value);
      } else if (params.colorBySign) {
        colors[i] = ((params.isColorReversed ? value > 0 : value <= 0) ? params.colorNeg
            : params.colorPos);
      } else {
        colors[i] = getColorFromPalette(value);
      }
    }
    if (params.mappedDataMin == Float.MAX_VALUE) {
      params.mappedDataMin = contourPlaneMinimumValue;
      params.mappedDataMax = contourPlaneMaximumValue;
    }
    return data + "\n";
  }

  protected void gotoData(int n, int nPoints) throws Exception {

    //called by VolumeFileReader.readVoxelData

    if (n > 0)
      Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
    for (int i = 0; i < n; i++) {
      jvxlReadDefinitionLine(true);
      Logger.info("JVXL skipping: jvxlSurfaceDataCount=" + surfaceDataCount
          + " jvxlEdgeDataCount=" + edgeDataCount
          + " jvxlDataIsColorMapped=" + jvxlDataIsColorMapped);
      jvxlSkipData(nPoints, true);
    }
    jvxlReadDefinitionLine(true);
  }

  private void jvxlSkipData(int nPoints, boolean doSkipColorData)
      throws Exception {
    // surfaceDataCount is quantitatively unreliable in pre-4/2007 versions (Jvxl 1.0)
    // so we just add them all up -- they must sum to nX * nY * nZ points 
    if (surfaceDataCount > 0) // unreliable in pre-4/2007 versions (Jvxl 1.0)
      jvxlSkipDataBlock(nPoints, true);
    if (edgeDataCount > 0)
      jvxlSkipDataBlock(edgeDataCount, false);
    if (jvxlDataIsColorMapped && doSkipColorData)
      jvxlSkipDataBlock(colorDataCount, false);
  }

  private void jvxlSkipDataBlock(int nPoints, boolean isInt) throws Exception {
    int iV = 0;
    while (iV < nPoints) {
      line = br.readLine();
      iV += (isInt ? countData(line) : jvxlUncompressString(line).length());
    }
  }

  private int countData(String str) {
    int count = 0;
    int n = parseInt(str);
    while (n != Integer.MIN_VALUE) {
      count += n;
      n = parseIntNext(str);
    }
    return count;
  }

  //// methods for creating the JVXL code  

  protected static void jvxlCreateHeader(String line1, String line2, VolumeData v,
                               StringBuffer bs) {
    bs.append(line1).append('\n');
    bs.append(line2).append('\n');
    bs.append("-2 " + v.volumetricOrigin.x + " " + v.volumetricOrigin.y + " "
        + v.volumetricOrigin.z + " ANGSTROMS\n");
    for (int i = 0; i < 3; i++)
      bs.append(v.voxelCounts[i] + " " + v.volumetricVectors[i].x + " "
          + v.volumetricVectors[i].y + " " + v.volumetricVectors[i].z + '\n');
    jvxlAddDummyAtomList(v, bs);
  }
  
  private static void jvxlAddDummyAtomList(VolumeData v, StringBuffer bs) {
    Point3f pt = new Point3f(v.volumetricOrigin);
    bs.append("1 1.0 " + pt.x + " " + pt.y + " " + pt.z
        + " //BOGUS H ATOM ADDED FOR JVXL FORMAT\n");
    for (int i = 0; i < 3; i++)
      pt.scaleAdd(v.voxelCounts[i] - 1, v.volumetricVectors[i], pt);
    bs.append("2 2.0 " + pt.x + " " + pt.y + " " + pt.z
        + " //BOGUS He ATOM ADDED FOR JVXL FORMAT\n");
  }

  private static String jvxlGetDefinitionLine(JvxlData jvxlData, boolean isInfo) {
    String definitionLine = jvxlData.cutoff + " ";

    //  cutoff       nInts     (+/-)bytesEdgeData (+/-)bytesColorData
    //               param1              param2         param3    
    //                 |                   |              |
    //   when          |                   |        >  0 ==> jvxlDataIsColorMapped
    //   when          |                   |       == -1 ==> not color mapped
    //   when          |                   |        < -1 ==> jvxlDataIsPrecisionColor    
    //   when        == -1     &&   == -1 ==> noncontoured plane
    //   when        == -1     &&   == -2 ==> contourable plane
    //   when        < -1*     &&    >  0 ==> contourable functionXY
    //   when        > 0       &&    <  0 ==> jvxlDataisBicolorMap

    // * nInts saved as -1 - nInts

    if (jvxlData.jvxlSurfaceData == null)
      return "";
    int nSurfaceInts = jvxlData.nSurfaceInts;//jvxlData.jvxlSurfaceData.length();
    int bytesUncompressedEdgeData = (jvxlData.jvxlEdgeData.length() - 1);
    int nColorData = (jvxlData.jvxlColorData.length() - 1);
    String info = "# cutoff = " + jvxlData.cutoff + "; nSurfaceInts = "
        + nSurfaceInts
        + "; nBytesData = "
        + (jvxlData.jvxlSurfaceData.length() + bytesUncompressedEdgeData + (jvxlData.jvxlColorData
            .length()));
    if (jvxlData.jvxlPlane == null) {
      if (jvxlData.isContoured) {
        definitionLine += (-1 - nSurfaceInts) + " " + bytesUncompressedEdgeData;
        info += "; contoured";
      } else if (jvxlData.isBicolorMap) {
        definitionLine += (nSurfaceInts) + " " + (-bytesUncompressedEdgeData);
        info += "; bicolor map";
      } else {
        definitionLine += nSurfaceInts + " " + bytesUncompressedEdgeData;
        if (nColorData > 0)
          info += "; colormapped";
      }
      definitionLine += " "
          + (jvxlData.isJvxlPrecisionColor && nColorData != -1 ? -nColorData
              : nColorData);
      if (jvxlData.isJvxlPrecisionColor && nColorData != -1)
        info += "; precision colored";
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
    definitionLine += " " + jvxlData.mappedDataMin + " "
        + jvxlData.mappedDataMax + " " + jvxlData.valueMappedToRed + " "
        + jvxlData.valueMappedToBlue;

    if (jvxlData.jvxlColorData.length() > 0 && !jvxlData.isBicolorMap)
      info += "\n# data minimum = " + jvxlData.mappedDataMin
        + "; data maximum = " + jvxlData.mappedDataMax + " "
        + "\n# value mapped to red = " + jvxlData.valueMappedToRed
        + "; value mapped to blue = " + jvxlData.valueMappedToBlue;
    if (jvxlData.jvxlCompressionRatio > 0)
      info += "; approximate compressionRatio=" + jvxlData.jvxlCompressionRatio
          + ":1";
    if (jvxlData.isXLowToHigh)
      info += "\n# progressive JVXL+ -- X values read from low(0) to high(" + (jvxlData.nPointsX - 1) + ")";
    info += "\n# created using Jvxl.java";
    return (isInfo ? info : definitionLine);
  }

  protected static String jvxlExtraLine(JvxlData jvxlData, int n) {
    return (-n) + " " + jvxlData.edgeFractionBase + " "
        + jvxlData.edgeFractionRange + " " + jvxlData.colorFractionBase + " "
        + jvxlData.colorFractionRange + " Jmol voxel format version 1.1\n";
    //0.9e adds color contours for planes and min/max range, contour settings
  }

  static String jvxlGetFile(JvxlData jvxlData, String[] title, String msg,
                            boolean includeHeader, int nSurfaces, String state, String comment) {
    
    StringBuffer data = new StringBuffer();
    if (includeHeader) {
      String s = jvxlData.jvxlFileHeader
          + (nSurfaces > 0 ? (-nSurfaces) + jvxlData.jvxlExtraLine.substring(2)
              : jvxlData.jvxlExtraLine);
      if (s.indexOf("#JVXL") != 0)
        data.append("#JVXL").append(jvxlData.isXLowToHigh ? "+\n" : "\n");
      data.append(s);
    }
    data.append("# ").append(msg).append('\n');
    if (title != null)
      for (int i = 0; i < title.length; i++)
        data.append("# ").append(title[i]).append('\n');
    data.append(jvxlData.jvxlDefinitionLine + " rendering:" + state).append('\n');
    
    String compressedData = (jvxlData.jvxlPlane == null ? jvxlData.jvxlSurfaceData : "");
    if (jvxlData.jvxlPlane == null) {
      //no real point in compressing this unless it's a sign-based coloring 
      compressedData += jvxlCompressString(jvxlData.jvxlEdgeData
          + jvxlData.jvxlColorData);
    } else {
      compressedData += jvxlCompressString(jvxlData.jvxlColorData);
    }
    int r = 0;
    if (jvxlData.wasCubic && jvxlData.nBytes > 0 && compressedData.length() > 0)
      jvxlData.jvxlCompressionRatio = r = (int) (((float) jvxlData.nBytes) / compressedData.length());
    data.append(compressedData);
    if (msg != null)
      data.append("#-------end of jvxl file data-------\n");
    data.append(jvxlData.jvxlInfoLine).append('\n');
    if (comment != null)
      data.append("# ").append(comment).append('\n');
    if (state != null)
      data.append("# ").append(state).append('\n');
    if (r > 0) {
      String s = "bytes read: " + jvxlData.nBytes + "; approximate voxel-only input/output byte ratio: " + r + ":1\n";
      data.append("# ").append(s);
      Logger.info("\n" + s);
    }

    return data.toString();
  }

  private static String jvxlCompressString(String data) {
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

  //  to/from ascii-encoded data

  protected static float jvxlFractionFromCharacter(int ich, int base, int range,
                                         float fracOffset) {
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

  /* unused here
  float jvxlValueFromCharacter(int ich, float min, float max, int base,
  int range, float fracOffset) {
  float fraction = jvxlFractionFromCharacter(ich, base, range, fracOffset);
  return (max == min ? fraction : min + fraction * (max - min));
  }
  */

  protected static float jvxlValueFromCharacter2(int ich, int ich2, float min, float max,
                                       int base, int range) {
    float fraction = jvxlFractionFromCharacter2(ich, ich2, base, range);
    return (max == min ? fraction : min + fraction * (max - min));
  }

  protected static float jvxlFractionFromCharacter2(int ich1, int ich2, int base,
                                          int range) {
    float fraction = jvxlFractionFromCharacter(ich1, base, range, 0);
    float remains = jvxlFractionFromCharacter(ich2, base, range, 0.5f);
    return fraction + remains / range;
  }

  protected static char jvxlValueAsCharacter(float value, float min, float max, int base,
                                   int range) {
    float fraction = (min == max ? value : (value - min) / (max - min));
    return jvxlFractionAsCharacter(fraction, base, range);
  }

  protected static char jvxlFractionAsCharacter(float fraction, int base, int range) {
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

  protected static char jvxlValueAsCharacter2(float value, float min, float max,
                                    int base, int range, char[] remainder) {
    float fraction = (min == max ? value : (value - min) / (max - min));
    char ch1 = jvxlFractionAsCharacter(fraction, base, range);
    fraction -= jvxlFractionFromCharacter(ch1, base, range, 0);
    remainder[0] = jvxlFractionAsCharacter(fraction * range, base, range);
    return ch1;
  }

}
