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
import javax.vecmath.Point4f;
import java.io.BufferedReader;

import org.openscience.jvxl.util.*;

class JvxlReader extends VolumeFileReader {

  JvxlReader(BufferedReader br, SurfaceReader.Parameters params,
      VolumeData volumeData, MeshData meshData, JvxlData jvxlData) {
    super(br, params, volumeData, meshData, jvxlData);
    isJvxl = true;
  }

  static void jvxlUpdateInfo(JvxlData jvxlData, String[] title, int nBytes) {
    jvxlData.title = title;
    jvxlData.nBytes = nBytes;
    jvxlData.jvxlDefinitionLine = jvxlGetDefinitionLine(jvxlData, false);
    jvxlData.jvxlInfoLine = jvxlGetDefinitionLine(jvxlData, true);
  }

  //// methods used for reading any file format, but creating a JVXL file

  /////////////reading the format///////////

  // #comments (optional)
  // info line1
  // info line2
  // -na originx originy originz   [ANGSTROMS] optional
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

  void readTitleLines() throws Exception {
    skipComments(true);
    int nLines = 1;
    jvxlFileHeaderBuffer = new StringBuffer();
    while (nLines <= 2) {
      if (line == null || line.length() == 0)
        line = "Line " + nLines;
      jvxlFileHeaderBuffer.append(line).append('\n');
      if (nLines++ == 1)
        br.readLine();
    }
  }

  void readAtomCountAndOrigin() throws Exception {
    super.readAtomCountAndOrigin(); // same as for cube file
  }

  static void jvxlReadAtoms(BufferedReader br, StringBuffer bs, int atomCount,
                            VolumeData v) throws Exception {
    //mostly ignored
    for (int i = 0; i < atomCount; ++i)
      bs.append(br.readLine() + "\n");
    if (atomCount == 0) {
      Point3f pt = new Point3f(v.volumetricOrigin);
      bs.append("1 1.0 " + pt.x + " " + pt.y + " " + pt.z
          + " //BOGUS H ATOM ADDED FOR JVXL FORMAT\n");
      for (int i = 0; i < 3; i++)
        pt.scaleAdd(v.voxelCounts[i] - 1, v.volumetricVectors[i], pt);
      bs.append("2 2.0 " + pt.x + " " + pt.y + " " + pt.z
          + " //BOGUS He ATOM ADDED FOR JVXL FORMAT\n");
    }
  }

  int readExtraLine() throws Exception {
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
      params.isContoured = (param3 != 0);
      // contours are defined (possibly overridden -- this is just a display option
      // could be plane or functionXY
      int nContoursRead = parseInt();
      if (nContours == 0 && nContoursRead != Integer.MIN_VALUE
          && nContoursRead != 0 && nContoursRead <= nContourMax) {
        nContours = nContoursRead;
        Logger.info("JVXL read: contours " + nContours);
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
    jvxlSurfaceDataCount = (param1 < -1 ? -param1 : param1 > 0 ? param1 : 0);
    if (param1 == -1)
      jvxlEdgeDataCount = 0; //plane
    else
      jvxlEdgeDataCount = (param2 < -1 ? -param2 : param2 > 0 ? param2 : 0);
    jvxlColorDataCount = (params.isBicolorMap ? -param2 : param3 < -1 ? -param3
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
      Logger.info("JVXL read: color red/blue: " + params.valueMappedToRed + " "
          + params.valueMappedToBlue);
    }
  }

  void readData(boolean isMapData) {
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

  void readVolumetricData(boolean isMapData) {
    initializeVolumetricData();
    try {
      readVoxelData(isMapData);
      if (jvxlEdgeDataCount > 0)
        jvxlEdgeDataRead = jvxlReadData("edge", jvxlEdgeDataCount);
      if (jvxlColorDataCount > 0)
        jvxlColorDataRead = jvxlReadData("color", jvxlColorDataCount);
    } catch (Exception e) {
      Logger.error(e.toString());
      throw new NullPointerException();
    }
  }

  void readVoxelData(boolean isMapData) throws Exception {

    //calls VolumeFileReader.readVoxelData

    super.readVoxelData(isMapData);
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

  float getNextVoxelValue() throws Exception {

    //called by VolumeFileReader.readVoxelData

    if (jvxlSurfaceDataCount <= 0)
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
        } else {
          surfaceData += line + "\n";
        }
      }
      thisInside = !thisInside;
    }
    --nThisValue;
    return (thisInside ? 1f : 0f);
  }

  float readSurfacePoint(float cutoff, boolean isCutoffAbsolute, float valueA,
                         float valueB, Point3f surfacePoint) {
    float fraction;
    if (jvxlEdgeDataCount <= 0)
      return super.readSurfacePoint(cutoff, isCutoffAbsolute, valueA, valueB,
          surfacePoint);
    fraction = jvxlGetNextFraction(edgeFractionBase, edgeFractionRange, 0.5f);
    edgeVector.sub(pointB, pointA);
    surfacePoint.scaleAdd(fraction, edgeVector, pointA);
    return fraction;
  }

  int fractionPtr;
  String strFractionTemp = "";

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

  void readColorData() {
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
      return;
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
    contourPlaneMinimumValue = Float.MAX_VALUE;
    contourPlaneMaximumValue = -Float.MAX_VALUE;
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
        contourVertexes[i].setValue(value);
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
    jvxlData.jvxlColorData = data + "\n";
  }

  void gotoData(int n, int nPoints) throws Exception {

    //called by VolumeFileReader.readVoxelData

    if (n > 0)
      Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
    for (int i = 0; i < n; i++) {
      jvxlReadDefinitionLine(true);
      Logger.info("JVXL skipping: jvxlSurfaceDataCount=" + jvxlSurfaceDataCount
          + " jvxlEdgeDataCount=" + jvxlEdgeDataCount
          + " jvxlDataIsColorMapped=" + jvxlDataIsColorMapped);
      jvxlSkipData(nPoints, true);
    }
    jvxlReadDefinitionLine(true);
  }

  private void jvxlSkipData(int nPoints, boolean doSkipColorData)
      throws Exception {
    if (jvxlSurfaceDataCount > 0)
      jvxlSkipDataBlock(nPoints, true);
    if (jvxlEdgeDataCount > 0)
      jvxlSkipDataBlock(jvxlEdgeDataCount, false);
    if (jvxlDataIsColorMapped && doSkipColorData)
      jvxlSkipDataBlock(jvxlColorDataCount, false);
  }

  private void jvxlSkipDataBlock(int nPoints, boolean isInt) throws Exception {
    int iV = 0;
    while (iV < nPoints) {
      line = br.readLine();
      iV += (isInt ? countData(line) : jvxlUncompressString(line).length());
    }
  }

  int countData(String str) {
    int count = 0;
    int n = parseInt(str);
    while (n != Integer.MIN_VALUE) {
      count += n;
      n = parseIntNext(str);
    }
    return count;
  }

  //// methods for creating the JVXL code  

  static String jvxlGetDefinitionLine(JvxlData jvxlData, boolean isInfo) {
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
    definitionLine += " " + jvxlData.mappedDataMin + " "
        + jvxlData.mappedDataMax + " " + jvxlData.valueMappedToRed + " "
        + jvxlData.valueMappedToBlue;

    info += "\n# data mimimum = " + jvxlData.mappedDataMin
        + "; data maximum = " + jvxlData.mappedDataMax + " "
        + "\n# value mapped to red = " + jvxlData.valueMappedToRed
        + "; value mapped to blue = " + jvxlData.valueMappedToBlue;
    if (jvxlData.jvxlCompressionRatio > 0)
      info += "; approximate compressionRatio=" + jvxlData.jvxlCompressionRatio
          + ":1";
    info += "\n# created using Jvxl.java";
    return (isInfo ? info : definitionLine);
  }

  static String jvxlExtraLine(JvxlData jvxlData, int n) {
    return (-n) + " " + jvxlData.edgeFractionBase + " "
        + jvxlData.edgeFractionRange + " " + jvxlData.colorFractionBase + " "
        + jvxlData.colorFractionRange + " JVXL Jmol voxel format version 1.0\n";
    //0.9e adds color contours for planes and min/max range, contour settings
  }

  static String jvxlGetFile(String[] title, JvxlData jvxlData, String msg,
                            boolean includeHeader, int nSurfaces) {
    String data = "";
    if (includeHeader) {
      data = jvxlData.jvxlFileHeader
          + (nSurfaces > 0 ? (-nSurfaces) + jvxlData.jvxlExtraLine.substring(2)
              : jvxlData.jvxlExtraLine);
      if (data.indexOf("JVXL") != 0)
        data = (data.indexOf("#") == 0 ? "#" : "") + "JVXL " + data;
    }
    data += "# " + msg + "\n";
    if (title != null)
      for (int i = 0; i < title.length; i++)
        data += "# " + title[i] + "\n";
    data += jvxlData.jvxlDefinitionLine + "\n";
    String compressedData = (jvxlData.jvxlPlane == null ? jvxlData.jvxlSurfaceData
        : "");
    if (jvxlData.jvxlPlane == null) {
      //no real point in compressing this unless it's a sign-based coloring 
      compressedData += jvxlCompressString(jvxlData.jvxlEdgeData
          + jvxlData.jvxlColorData);
    } else {
      compressedData += jvxlCompressString(jvxlData.jvxlColorData);
    }
    //if (!isJvxl &&jvxlData.nBytes > 0)
    //jvxlData.jvxlCompressionRatio = (int) (((float)jvxlData.nBytes + jvxlData.jvxlFileHeader
    //  .length()) / (data.length() + compressedData.length()));
    data += compressedData;
    if (msg != null)
      data += "#-------end of jvxl file data-------\n";
    data += jvxlData.jvxlInfoLine + "\n";
    return data;
  }

  static String jvxlCompressString(String data) {
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

  static float jvxlFractionFromCharacter(int ich, int base, int range,
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

  static float jvxlValueFromCharacter2(int ich, int ich2, float min, float max,
                                       int base, int range) {
    float fraction = jvxlFractionFromCharacter2(ich, ich2, base, range);
    return (max == min ? fraction : min + fraction * (max - min));
  }

  static float jvxlFractionFromCharacter2(int ich1, int ich2, int base,
                                          int range) {
    float fraction = jvxlFractionFromCharacter(ich1, base, range, 0);
    float remains = jvxlFractionFromCharacter(ich2, base, range, 0.5f);
    return fraction + remains / range;
  }

  static char jvxlValueAsCharacter(float value, float min, float max, int base,
                                   int range) {
    float fraction = (min == max ? value : (value - min) / (max - min));
    return jvxlFractionAsCharacter(fraction, base, range);
  }

  static char jvxlFractionAsCharacter(float fraction, int base, int range) {
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

  static char jvxlValueAsCharacter2(float value, float min, float max,
                                    int base, int range, char[] remainder) {
    float fraction = (min == max ? value : (value - min) / (max - min));
    char ch1 = jvxlFractionAsCharacter(fraction, base, range);
    fraction -= jvxlFractionFromCharacter(ch1, base, range, 0);
    remainder[0] = jvxlFractionAsCharacter(fraction * range, base, range);
    return ch1;
  }

}
