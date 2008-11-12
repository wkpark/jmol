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

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point4f;
import java.io.BufferedReader;

import org.jmol.util.*;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.MeshData;
import org.jmol.jvxl.data.VolumeData;

public class JvxlReader extends VolumeFileReader {

  private final static String JVXL_VERSION = "2.0";
  
  // 1.4 adds -nContours to indicate contourFromZero for MEP data mapped onto planes
  // 2.0 adds vertex/triangle compression when no grid is present 

  // NEVER change the numbers for these next defaults
  
  final public static int defaultEdgeFractionBase = 35; //#$%.......
  final public static int defaultEdgeFractionRange = 90;
  final public static int defaultColorFractionBase = 35;
  final public static int defaultColorFractionRange = 90;

  JvxlReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
    jvxlData.wasJvxl = isJvxl = true;
    isXLowToHigh = false;
  }

  protected static void jvxlUpdateInfo(JvxlData jvxlData, String[] title, int nBytes) {
    jvxlData.title = title;
    jvxlData.nBytes = nBytes;
    jvxlUpdateInfoLines(jvxlData);
  }

  public static void jvxlUpdateInfoLines(JvxlData jvxlData) {
    jvxlData.jvxlDefinitionLine = jvxlGetDefinitionLine(jvxlData, false);
    jvxlData.jvxlInfoLine = jvxlGetDefinitionLine(jvxlData, true);
  }
  //// methods used for reading any file format, but creating a JVXL file

  /////////////reading the format///////////

  private int surfaceDataCount;
  private int edgeDataCount;
  private int colorDataCount;

  protected void readVolumeData(boolean isMapData) {
    super.readVolumeData(isMapData);
    strFractionTemp = jvxlEdgeDataRead;
    fractionPtr = 0;
  }

  protected void gotoAndReadVoxelData(boolean isMapData) {
    initializeVolumetricData();
    if (nPointsX < 0 || nPointsY < 0 || nPointsZ < 0) 
      return;
    try {
      gotoData(params.fileIndex - 1, nPointsX * nPointsY * nPointsZ);
      if (vertexDataOnly)
        return;
      readSurfaceData(isMapData);
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
    nThisValue = 0;
  }
  
  protected void readSurfaceData(boolean isMapDataIgnored) throws Exception {
    initializeVoxelData();
    //calls VolumeFileReader.readVoxelData; no mapping allowed
    if (vertexDataOnly) {
      getEncodedVertexData();
      return;
    }
    if (params.thePlane == null) {
      super.readSurfaceData(false);
      return;
    }
    volumeData.setDataDistanceToPlane(params.thePlane);
    setVolumeData(volumeData);
    params.cutoff = 0f;
    setSurfaceInfo(jvxlData, params.thePlane, 0, new StringBuffer());
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
    if (line == null || line.length() == 0)
      line = "Line 1";
    jvxlFileHeaderBuffer.append(line).append('\n');
    if ((line = br.readLine()) == null || line.length() == 0)
      line = "Line 2";
    jvxlFileHeaderBuffer.append(line).append('\n');
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
      if (atomCount == Integer.MIN_VALUE)
        return;
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
    skipComments(false);
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
    cJvxlEdgeNaN = (char)(edgeFractionBase + edgeFractionRange);
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
    
    // it's possible that a plane will not be contoured (-1 -1) when it is a solid color.
    // why you would want to save this as JVXL is another question.
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
      if (param2 == -1 && param3 < 0)
        param3 = -param3;
      //error in some versions of Jmol. (fixed in 11.3.54)
    } else {
      params.thePlane = null;
    }
    if (param1 < 0 && param2 != -1) {
      // contours are defined (possibly overridden -- this is just a display option
      // could be plane or functionXY
      params.isContoured = (param3 != 0);
      int nContoursRead = parseInt();
      if (nContoursRead != Integer.MIN_VALUE) {
        if (nContoursRead < 0) {
          nContoursRead = -1 - nContoursRead;
          params.contourFromZero = false; //MEP data to complete the plane
        }
        if (nContoursRead != 0 && params.nContours == 0) {
          params.nContours = nContoursRead;
          Logger.info("JVXL read: contours " + params.nContours);
        }
      }
    } else {
      params.isContoured = false;
    }

    jvxlDataIsPrecisionColor = (param1 == -1 && param2 == -2 
        || param3 < 0);
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
    jvxlData.insideOut = (line.indexOf("insideOut") >= 0);
    if (params.insideOut)
      jvxlData.insideOut = !jvxlData.insideOut;
    params.insideOut = jvxlData.insideOut;
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

  static String jvxlCompressString(String data) {
    /* just a simple compression, but allows 2000-6000:1 CUBE:JVXL for planes!
     * 
     *   "X~nnn " means "nnn copies of character X" 
     *   
     *   ########## becomes "#~10 " 
     *   ~ becomes "~~" 
     *
     */
    StringBuffer dataOut = new StringBuffer();
    char chLast = '\0';
    data += '\0';
    int nLast = 0;
    for (int i = 0; i < data.length(); i++) {
      char ch = data.charAt(i);
      if (ch == chLast) {
        ++nLast;
        if (ch != '~')
          ch = '\0';
      } else if (nLast > 0) {
        if (nLast < 4 || chLast == '~' || chLast == ' '
            || chLast == '\t')
          while (--nLast >= 0)
            dataOut.append(chLast);
        else 
          dataOut.append("~" + nLast + " ");
        nLast = 0;
      }
      if (ch != '\0') {
        dataOut.append(ch);
        chLast = ch;
      }
    }
    return dataOut.toString();
  }

  private static String jvxlUncompressString(String data) {
    if (data.indexOf("~") < 0)
      return data;
    StringBuffer dataOut = new StringBuffer();
    char chLast = '\0';
    int[] next = new int[1];
    for (int i = 0; i < data.length(); i++) {
      char ch = data.charAt(i);
      if (ch == '~') {
        next[0] = ++i;
        int nChar = Parser.parseInt(data, next);
        if (nChar == Integer.MIN_VALUE) {
          if (chLast == '~') {
            dataOut.append('~');
            while ((ch = data.charAt(++i)) == '~')
              dataOut.append('~');
          } else {
            Logger.error("Error uncompressing string " + data.substring(0, i)
                + "?");
          }
        } else {
          for (int c = 0; c < nChar; c++)
            dataOut.append(chLast);
          i = next[0];
        }
      } else {
        dataOut.append(ch);
        chLast = ch;
      }
    }
    return dataOut.toString();
  }

  protected float getNextVoxelValue(StringBuffer sb) throws Exception {

    //called by VolumeFileReader.readVoxelData

    if (surfaceDataCount <= 0)
      return 0f; //unnecessary -- probably a plane
    while (nThisValue == 0) {
      nThisValue = parseInt();
      if (nThisValue == Integer.MIN_VALUE) {
        line = br.readLine();
        if (line == null || (nThisValue = parseInt(line)) == Integer.MIN_VALUE) {
          if (!endOfData)
            Logger.error("end of file in JvxlReader?" + " line=" + line);
          endOfData = true;
          nThisValue = 10000;
          //throw new NullPointerException();
        } else if (sb != null) {
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
    if (jvxlData.jvxlSurfaceData.indexOf("--") == 0)
      jvxlData.jvxlSurfaceData = jvxlData.jvxlSurfaceData.substring(2);
    jvxlData.jvxlPlane = thePlane;
    jvxlData.nSurfaceInts = nSurfaceInts;
  }
  
  protected float readSurfacePoint(float cutoff, boolean isCutoffAbsolute, float valueA,
                         float valueB, Point3f pointA, Vector3f edgeVector, 
                         float[] fReturn, Point3f ptReturn) {
    if (edgeDataCount <= 0)
      return super.readSurfacePoint(cutoff, isCutoffAbsolute, valueA, valueB,
          pointA, edgeVector, fReturn, ptReturn);
    ptReturn.scaleAdd(fReturn[0] = jvxlGetNextFraction(edgeFractionBase, edgeFractionRange, 0.5f), 
        edgeVector, pointA);
    return fReturn[0];
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
    // overloads SurfaceReader
    // standard jvxl file read for color 

    fractionPtr = 0;
    int vertexCount = meshData.vertexCount;
    short[] colixes = meshData.vertexColixes;
    float[] vertexValues = meshData.vertexValues;
    fractionData = new StringBuffer();
    strFractionTemp = (isJvxl ? jvxlColorDataRead : "");
    if (isJvxl && strFractionTemp.length() == 0) {
      Logger
          .error("You cannot use JVXL data to map onto OTHER data, because it only containts the data for one surface. Use ISOSURFACE \"file.jvxl\" not ISOSURFACE .... MAP \"file.jvxl\".");
      return "";
    }
    fractionPtr = 0;
    Logger.info("JVXL reading color data mapped min/max: " + params.mappedDataMin
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
    if (colixes == null || colixes.length < vertexCount)
      meshData.vertexColixes = colixes = new short[vertexCount];
    jvxlData.vertexCount = vertexCount;
    String data = jvxlColorDataRead;
    hasColorData = true;
    int cpt = 0;
    short colixNeg = 0, colixPos = 0;
    if (params.colorBySign) {
      colixPos = ColorEncoder
          .getColorIndex(params.isColorReversed ? params.colorNeg
              : params.colorPos);
      colixNeg = ColorEncoder
          .getColorIndex(params.isColorReversed ? params.colorPos
              : params.colorNeg);
    }
    int vertexIncrement = meshData.vertexIncrement;
    
    for (int i = 0; i < vertexCount; i+= vertexIncrement) {
      float fraction, value;
      if (jvxlDataIsPrecisionColor) {
        // this COULD be an option for mapped surfaces; 
        // necessary for planes.
        // precision is used for FULL-data range encoding, allowing full
        // treatment of JVXL files as though they were CUBE files.
        // the two parts of the "double-character-precision" value
        // are in separate lines, separated by n characters.
        fraction = jvxlFractionFromCharacter2(data.charAt(cpt), data.charAt(cpt
            + vertexCount), colorFractionBase, colorFractionRange);
        value = min + fraction * range;
      } else {
        // my original encoding scheme
        // low precision only allows for mapping relative to the defined color range
        fraction = jvxlFractionFromCharacter(data.charAt(cpt),
            colorFractionBase, colorFractionRange, 0.5f);
        value = params.valueMappedToRed + fraction * colorRange;
      }
      vertexValues[i] = value;
      ++cpt;
      if (value < contourPlaneMinimumValue)
        contourPlaneMinimumValue = value;
      if (value > contourPlaneMaximumValue)
        contourPlaneMaximumValue = value;
      
      //note: these are just default colorings
      //orbital color had a bug through 11.2.6/11.3.6
      if (params.isContoured) {
        marchingSquares.setContourData(i, value);
      } else if (params.colorBySign) {
        colixes[i] = ((params.isColorReversed ? value > 0 : value <= 0) ? colixNeg
            : colixPos);
      } else {
        colixes[i] = getColorIndexFromPalette(value);
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
    vertexDataOnly = jvxlData.vertexDataOnly = (nPoints == 0);
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

  protected static void jvxlCreateHeaderWithoutTitleOrAtoms(VolumeData v, StringBuffer bs) {
    jvxlCreateHeader(v, Integer.MAX_VALUE, null, null, bs);
  }

  protected static void jvxlCreateHeader(VolumeData v, int nAtoms, 
                                         Point3f[] atomXyz, int[] atomNo,
                                         StringBuffer sb) {
    // if the StringBuffer comes in non-empty, it should have two lines
    // that do not start with # already present.
    if (sb.length() == 0)
      sb.append("Line 1\nLine 2\n");
    sb.append(nAtoms == Integer.MAX_VALUE ? -2 : -nAtoms).append(' ')
      .append(v.volumetricOrigin.x).append(' ')
      .append(v.volumetricOrigin.y).append(' ')
      .append(v.volumetricOrigin.z).append(" ANGSTROMS\n");
    for (int i = 0; i < 3; i++)
      sb.append(v.voxelCounts[i]).append(' ')
        .append(v.volumetricVectors[i].x).append(' ')
        .append(v.volumetricVectors[i].y).append(' ')
        .append(v.volumetricVectors[i].z).append('\n');
    if (nAtoms == Integer.MAX_VALUE) {
      jvxlAddDummyAtomList(v, sb);
      return;
    }
    nAtoms = Math.abs(nAtoms);
      for (int i = 0, n = 0; i < nAtoms; i++)
        sb.append((n = Math.abs(atomNo[i])) + " " + n + ".0 "
            + atomXyz[i].x + " " + atomXyz[i].y + " " + atomXyz[i].z + "\n");
  }
  
  private static void jvxlAddDummyAtomList(VolumeData v, StringBuffer bs) {
    Point3f pt = new Point3f(v.volumetricOrigin);
    bs.append("1 1.0 ").append(pt.x).append(' ').append(pt.y).append(' ')
        .append(pt.z).append(" //BOGUS H ATOM ADDED FOR JVXL FORMAT\n");
    for (int i = 0; i < 3; i++)
      pt.scaleAdd(v.voxelCounts[i] - 1, v.volumetricVectors[i], pt);
    bs.append("2 2.0 ").append(pt.x).append(' ').append(pt.y).append(' ')
        .append(pt.z).append(" //BOGUS He ATOM ADDED FOR JVXL FORMAT\n");
  }

  public static int jvxlCreateSurfaceData(JvxlData jvxlData, float[][][] voxelData, float cutoff, boolean isCutoffAbsolute, int nX, int nY, int nZ) {
    StringBuffer sb = new StringBuffer();
    boolean inside = false;
    int dataCount = 0;
    int nDataPoints = 0;
    int nSurfaceInts = 0;
    for (int x = 0; x < nX; ++x)
      for (int y = 0; y < nY; ++y)
        for (int z = 0; z < nZ; ++z) {
          ++nDataPoints;
          if (inside == isInside(voxelData[x][y][z], cutoff, isCutoffAbsolute)) {
            dataCount++;
          } else {
            if (dataCount != 0) {
              sb.append(' ').append(dataCount);
              ++nSurfaceInts;
            }
            dataCount = 1;
            inside = !inside;
          }
        }
    sb.append(' ').append(dataCount).append('\n');
    ++nSurfaceInts;
    setSurfaceInfo(jvxlData,null, nSurfaceInts, sb);
    return nDataPoints;
  }
  
  public static String jvxlGetDefinitionLine(JvxlData jvxlData, boolean isInfo) {
    String definitionLine = jvxlData.cutoff + " ";

    //  cutoff       nInts     (+/-)bytesEdgeData (+/-)bytesColorData
    //               param1              param2         param3    
    //                 |                   |              |
    //   when          |                   |        >  0 ==> jvxlDataIsColorMapped
    //   when          |                   |       == -1 ==> not color mapped
    //   when          |                   |        < -1 ==> jvxlDataIsPrecisionColor    
    //   when        == -1     &&   == -1 ==> noncontoured plane
    //   when        == -1     &kg&   == -2 ==> contourable plane
    //   when        < -1*     &&    >  0 ==> contourable functionXY
    //   when        > 0       &&    <  0 ==> jvxlDataisBicolorMap

    // * nInts saved as -1 - nInts

    if (jvxlData.jvxlSurfaceData == null)
      return "";
    int nSurfaceInts = jvxlData.nSurfaceInts;//jvxlData.jvxlSurfaceData.length();
    int bytesUncompressedEdgeData = (jvxlData.vertexDataOnly ? 0 : jvxlData.jvxlEdgeData.length() - 1);
    int nColorData = (jvxlData.jvxlColorData.length() - 1);
    String info = "# cutoff = " + jvxlData.cutoff 
        + "; pointsPerAngstrom = " + jvxlData.pointsPerAngstrom
        + "; nSurfaceInts = " + nSurfaceInts
        + "; nBytesData = " + (jvxlData.jvxlSurfaceData.length() 
            + bytesUncompressedEdgeData + (jvxlData.jvxlColorData.length()));
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
      definitionLine += (jvxlData.isContoured ? "-1 -2 " + (-nColorData): "-1 -1 " + nColorData) 
      + s;
      info += "; " + (nColorData > 0 ? "color mapped " : "") + "plane: {" + s
          + " }";
    }
    if (jvxlData.isContoured) {
      definitionLine += " " + jvxlData.nContours;
      info += "; " + Math.abs(jvxlData.nContours) + " contours";
    }
    // ...  mappedDataMin  mappedDataMax  valueMappedToRed  valueMappedToBlue ...
    float min = (jvxlData.mappedDataMin == Float.MAX_VALUE ? 0f : jvxlData.mappedDataMin);
    definitionLine += " " 
        + min + " "
        + jvxlData.mappedDataMax + " " + jvxlData.valueMappedToRed + " "
        + jvxlData.valueMappedToBlue;

    if (jvxlData.jvxlColorData.length() > 0 && !jvxlData.isBicolorMap)
      info += "\n# data minimum = " + min
        + "; data maximum = " + jvxlData.mappedDataMax + " "
        + "\n# value mapped to red = " + jvxlData.valueMappedToRed
        + "; value mapped to blue = " + jvxlData.valueMappedToBlue;
    if (jvxlData.jvxlCompressionRatio > 0)
      info += "; approximate compressionRatio=" + jvxlData.jvxlCompressionRatio
          + ":1";
    if (jvxlData.isXLowToHigh)
      info += "\n# progressive JVXL+ -- X values read from low(0) to high(" + (jvxlData.nPointsX - 1) + ")";
    info += "\n# created using Jvxl.java\n#";
    if (jvxlData.insideOut) {
      info += " insideOut";
      definitionLine += " insideOut";
    }
    info += " precision: " + jvxlData.isJvxlPrecisionColor + " nColorData " + nColorData; 
    return (isInfo ? info : definitionLine);
  }

  protected static String jvxlExtraLine(JvxlData jvxlData, int n) {
    return (-n) + " " + jvxlData.edgeFractionBase + " "
        + jvxlData.edgeFractionRange + " " + jvxlData.colorFractionBase + " "
        + jvxlData.colorFractionRange + " Jmol voxel format version " +  JVXL_VERSION + "\n";
    //0.9e adds color contours for planes and min/max range, contour settings
  }

  public static String jvxlGetFile(JvxlData jvxlData, MeshData meshData,
                                   String[] title, String msg,
                                   boolean includeHeader, int nSurfaces,
                                   String state, String comment) {
    StringBuffer data = new StringBuffer();
    if (includeHeader) {
      String s = jvxlData.jvxlFileHeader
          + (nSurfaces > 0 ? (-nSurfaces) + jvxlData.jvxlExtraLine.substring(2)
              : jvxlData.jvxlExtraLine);
      if (s.indexOf("#JVXL") != 0)
        data.append("#JVXL").append(jvxlData.isXLowToHigh ? "+" : "")
            .append(" VERSION ").append(JVXL_VERSION).append("\n");
      data.append(s);
    }
    data.append("# ").append(msg).append('\n');
    if (title != null)
      for (int i = 0; i < title.length; i++)
        data.append("# ").append(title[i]).append('\n');
    data.append(jvxlData.jvxlDefinitionLine + " rendering:" + state).append(
        '\n');

    StringBuffer sb = new StringBuffer();
    if (jvxlData.vertexDataOnly && meshData != null) {
      int[] vertexMap = new int[meshData.vertexCount];
      sb.append("<jvxlSurfaceData>\n");
      sb.append(JvxlReader.jvxlEncodeTriangleData(meshData.polygonIndexes,
          meshData.polygonCount, vertexMap));
      sb.append(JvxlReader.jvxlEncodeVertexData(jvxlData, vertexMap,
          meshData.vertices, meshData.vertexValues, jvxlData.jvxlColorData
              .length() > 0));
      sb.append("</jvxlSurfaceData>\n");
      //JvxlReader.jvxlDecodeVertexData(jvxlData, vdata, true);
      //JvxlReader.jvxlDecodeTriangleData(tdata, true);
    } else if (jvxlData.jvxlPlane == null) {
      //no real point in compressing this unless it's a sign-based coloring
      sb.append(jvxlData.jvxlSurfaceData);
      sb.append(jvxlCompressString(jvxlData.jvxlEdgeData
          + jvxlData.jvxlColorData));
    } else {
      sb.append(jvxlCompressString(jvxlData.jvxlColorData));
    }
    String compressedData = sb.toString();
    int r = 0;
    if (compressedData.length() > 0) {
      if (jvxlData.wasCubic && jvxlData.nBytes > 0)
        jvxlData.jvxlCompressionRatio = r = (int) (((float) jvxlData.nBytes) / compressedData
            .length());
      else
        jvxlData.jvxlCompressionRatio = r = (int) (((float) (jvxlData.nPointsX
            * jvxlData.nPointsY * jvxlData.nPointsZ * 13)) / compressedData
            .length());
    }

    data.append(compressedData);
    if (msg != null)
      data.append("#-------end of jvxl file data-------\n");
    data.append(jvxlData.jvxlInfoLine).append('\n');
    if (comment != null)
      data.append("# ").append(comment).append('\n');
    if (state != null)
      data.append("# ").append(state).append('\n');
    if (r > 0) {
      String s = "bytes read: " + jvxlData.nBytes
          + "; approximate voxel-only input/output byte ratio: " + r + ":1\n";
      data.append("# ").append(s);
      Logger.info("\n" + s);
    }

    return data.toString();
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

  public static void jvxlUpdateSurfaceData(JvxlData jvxlData, float[] vertexValues, int vertexCount, int vertexIncrement, char isNaN) { 
    char[] chars = jvxlData.jvxlEdgeData.toCharArray();
    for (int i = 0, ipt = 0; i < vertexCount; i+= vertexIncrement, ipt++)
      if (Float.isNaN(vertexValues[i]))
          chars[ipt] = isNaN;
    jvxlData.jvxlEdgeData = String.copyValueOf(chars);
  }
  
  public static void jvxlCreateColorData(JvxlData jvxlData, float[] vertexValues) {
    if (vertexValues == null) {
      jvxlData.jvxlColorData = "";
      return;
    }
    boolean writePrecisionColor = jvxlData.isJvxlPrecisionColor;
    boolean doTruncate = jvxlData.isTruncated;
    int colorFractionBase = jvxlData.colorFractionBase;
    int colorFractionRange = jvxlData.colorFractionRange;
    float valueBlue = jvxlData.valueMappedToBlue;
    float valueRed = jvxlData.valueMappedToRed;
    int vertexCount = jvxlData.vertexCount;
    float min = jvxlData.mappedDataMin;
    float max = jvxlData.mappedDataMax;
    StringBuffer list = null, list1 = null;
    list = new StringBuffer();
    list1 = new StringBuffer();
    char[] remainder = new char[1];
    for (int i = 0; i < vertexCount; i++) {
      float value = vertexValues[i];
      if (doTruncate)
        value = (value > 0 ? 0.999f : -0.999f);
      char ch;
      if (writePrecisionColor) {
        ch = jvxlValueAsCharacter2(value, min, max, colorFractionBase,
            colorFractionRange, remainder);
        list1.append(remainder[0]);
      } else {
        //isColorReversed
        ch = jvxlValueAsCharacter(value, valueRed, valueBlue,
            colorFractionBase, colorFractionRange);
      }
      list.append(ch);
    }
    jvxlData.jvxlColorData = list.append(list1).append('\n').toString();
    jvxlUpdateInfoLines(jvxlData);
  }

  public static String jvxlEncodeTriangleData(int[][] triangles, int nData, int[] vertexMap) {
    StringBuffer list = new StringBuffer();
    StringBuffer list1 = new StringBuffer();
    int ilast = 1;
    int p = 0;
    int inew = 0;
    for (int i = 0; i < nData;) {
      int idata = triangles[i][p];
      if (vertexMap[idata] > 0) {
        idata = vertexMap[idata];
      } else {
        idata = vertexMap[idata] = ++inew;
      }
      
      //if (i < 10 || i + 10 > nData) System.out.println(triangles[i][p]);
      if (++p % 3 == 0) {
        i++;
        p = 0;
      }
      int diff = idata - ilast;
      ilast = idata;
      if (diff == 0) {
        list1.append('!');
      } else if (diff > 32 || diff < -32) {
        list1.append('.').append(diff).append(',');
      } else {
        list1.append((char)('\\' + diff));
      }
    }
    return list.append("  <jvxlTriangleData len=\"" + list1.length() + "\" count=\"" + nData + "\">\n    ")
       .append(list1).append("\n  </jvxlTriangleData>\n").toString();
  }

  int[][] jvxlDecodeTriangleData(String data, boolean asArray) {
    int[] next = new int[1];
    setNext(data, "count", next, 2);
    int nData = Parser.parseInt(data, next);
    if (!asArray)
      Logger.info("Reading " + nData + " triangles");
    int[][] triangles = (asArray ? new int[nData][3] : null);
    int[] triangle = (asArray ? triangles[0] : new int[3]);
    int ilast = 0;
    int p = 0;
    int b0 = (int)'\\';
    setNext(data, ">", next, 1);
    int pt = next[0];
    while (Character.isWhitespace(data.charAt(pt)))
      pt++;
    pt--;
    for (int i = 0; i < nData;) {
      char ch = data.charAt(++pt);
      int idiff;
      switch(ch) {
      case '!':
        idiff = 0;
        break;
      case '.':
        next[0] = ++pt;
        idiff = Parser.parseInt(data, next);
        pt = next[0];
        break;
      default:
        idiff = (int)ch - b0; 
      }
      ilast += idiff;
      if (asArray)
        triangles[i][p] = ilast;
      else
        triangle[p] = ilast;
      //if (i < 10 || i + 10 > nData) System.out.print(triangle[p] + " ");
      if (++p % 3 == 0) {
        //if (i < 10 || i + 10 > nData) System.out.println();
        i++;
        //if (asArray)triangle = triangles[i];
        p = 0;
        if (!asArray)
          addTriangleCheck(triangle[0], triangle[1], triangle[2], 7, false);
      }
    }
    return triangles;
  }

  public static String jvxlEncodeVertexData(JvxlData jvxlData, int[] vertexMap,
                                            Point3f[] vertices, float[] vertexValues, 
                                            boolean addColorData) {
    Point3f min = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    Point3f max = new Point3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
    int vertexCount = jvxlData.vertexCount;
    int colorFractionBase = jvxlData.colorFractionBase;
    int colorFractionRange = jvxlData.colorFractionRange;
    Point3f p;
    for (int i = 0; i < vertexCount; i++) {
      p = vertices[i];
      if (p.x < min.x)
        min.x = p.x;
      if (p.y < min.y)
        min.y = p.y;
      if (p.z < min.z)
        min.z = p.z;
      if (p.x > max.x)
        max.x = p.x;
      if (p.y > max.y)
        max.y = p.y;
      if (p.z > max.z)
        max.z = p.z;
    }
    StringBuffer list = new StringBuffer();
    StringBuffer list1 = new StringBuffer();
    StringBuffer list2 = new StringBuffer();
    char[] remainder = new char[1];
    char ch;
    int[] vertexMapRev = new int[vertexCount];
    for (int i = 0; i < vertexCount; i++)
      vertexMapRev[vertexMap[i] - 1] = i;
    for (int i = 0; i < vertexCount; i++) {
      p = vertices[vertexMapRev[i]];
      //if (i < 10 || i + 10 > vertexCount)System.out.println(vertexMapRev[i] + " " + p);
      ch = jvxlValueAsCharacter2(p.x, min.x, max.x, colorFractionBase,
          colorFractionRange, remainder);
      list2.append(remainder[0]);
      list1.append(ch);
      ch = jvxlValueAsCharacter2(p.y, min.y, max.y, colorFractionBase,
          colorFractionRange, remainder);
      list2.append(remainder[0]);
      list1.append(ch);
      ch = jvxlValueAsCharacter2(p.z, min.z, max.z, colorFractionBase,
          colorFractionRange, remainder);
      list2.append(remainder[0]);
      list1.append(ch);
    }
    list1.append(list2);
    list.append("  <jvxlVertexData len=\"" + list1.length() + "\" count=\"" + vertexCount + "\" min=\"" + min + "\" max=\"" + max + "\">\n    ");
    list.append(list1).append("\n  </jvxlVertexData>\n");
    if (!addColorData)
      return list.toString();
    list1 = new StringBuffer();
    list2 = new StringBuffer();
    boolean writePrecisionColor = jvxlData.isJvxlPrecisionColor;
    for (int i = 0; i < vertexCount; i++) {
      float value = vertexValues[vertexMapRev[i]]; 
      if (writePrecisionColor) {
        ch = jvxlValueAsCharacter2(value, jvxlData.mappedDataMin, jvxlData.mappedDataMax, colorFractionBase,
            colorFractionRange, remainder);
        list1.append(remainder[0]);
      } else {
        //isColorReversed
        ch = jvxlValueAsCharacter(value, jvxlData.mappedDataMin, jvxlData.mappedDataMax,
            colorFractionBase, colorFractionRange);
      }      
      list2.append(ch);
    }
    String s = jvxlCompressString(list2.append(list1).toString());
    return list.append("  <jvxlColorData len=\"" + s.length() + "\" count=\"" + vertexCount + "\" compressed=\"1\">\n    ")
        .append(s).append("\n  </jvxlColorData>\n").toString();
  }

  private static void setNext(String data, String what, int[] next, int offset) {
    next[0] = data.indexOf(what, next[0]) + what.length() + offset;
  }
  
  public Point3f[] jvxlDecodeVertexData(String data, boolean asArray) {
    next[0] = 0;
    setNext(data, "count", next, 2);
    int vertexCount = Parser.parseInt(data, next);
    if (!asArray)
      Logger.info("Reading " + vertexCount + " vertices");
    next[0]++;
    Point3f min = new Point3f();
    Point3f range = new Point3f();
    setNext(data, "min", next, 3);
    min.x = Parser.parseFloat(data, next);
    next[0]++;
    min.y = Parser.parseFloat(data, next);
    next[0]++;
    min.z = Parser.parseFloat(data, next);
    setNext(data, "max", next, 3);
    range.x = Parser.parseFloat(data, next) - min.x;
    next[0]++;
    range.y = Parser.parseFloat(data, next) - min.y;
    next[0]++;
    range.z = Parser.parseFloat(data, next) - min.z;
    int colorFractionBase = jvxlData.colorFractionBase;
    int colorFractionRange = jvxlData.colorFractionRange;
    int ptCount = vertexCount * 3;
    Point3f[] vertices = (asArray ? new Point3f[vertexCount] : null);
    Point3f p = (asArray ? null : new Point3f());
    float fraction;
    setNext(data, ">", next, 1);
    int pt = next[0];
    while (Character.isWhitespace(data.charAt(pt)))
      pt++;
    pt--;
    for (int i = 0; i < vertexCount; i++) {
      if (asArray)
        p = vertices[i] = new Point3f();
      fraction = jvxlFractionFromCharacter2(data.charAt(++pt), data.charAt(pt
          + ptCount), colorFractionBase, colorFractionRange);
      p.x = min.x + fraction * range.x;
      fraction = jvxlFractionFromCharacter2(data.charAt(++pt), data.charAt(pt
          + ptCount), colorFractionBase, colorFractionRange);
      p.y = min.y + fraction * range.y;
      fraction = jvxlFractionFromCharacter2(data.charAt(++pt), data.charAt(pt
          + ptCount), colorFractionBase, colorFractionRange);
      p.z = min.z + fraction * range.z;
      if (!asArray)
        addVertexCopy(p, 0, i);
      //if (i < 10 || i + 10 > vertexCount)System.out.println(p);
    }
    return vertices;
  }
  
  private void getEncodedVertexData() throws Exception {
    String data = getXmlData("jvxlSurfaceData", null, true);
    jvxlDecodeVertexData(getXmlData("jvxlVertexData", data, true), false);
    jvxlDecodeTriangleData(getXmlData("jvxlTriangleData", data, true), false);
    Logger.info("Checking for vertex values");
    jvxlColorDataRead = jvxlUncompressString(getXmlData("jvxlColorData", data, false));
    jvxlDataIsColorMapped = (jvxlColorDataRead.length() > 0);
    Logger.info("Done");
  }

  private String getXmlData(String name, String data, boolean withTag) throws Exception {
    //crude
    String closer = "</" + name + ">";
    String tag = "<" + name;
    if (data == null) {
      StringBuffer sb = new StringBuffer();
      while (line.indexOf(tag) < 0)
        line = br.readLine();
      sb.append(line);
      while (line.indexOf(closer) < 0)
        sb.append(line = br.readLine());
      data = sb.toString();
    }
    int pt1 = data.indexOf(tag);
    int pt2 = data.indexOf(closer);
    if (pt1 >= 0 && !withTag) {
      pt1 = data.indexOf(">", pt1) + 1;
      while (Character.isWhitespace(data.charAt(pt1)))
        pt1++;
    }
    if (pt1 < 0 || pt1 > pt2)
      return "";
    return data.substring(pt1, pt2);
  }

}
