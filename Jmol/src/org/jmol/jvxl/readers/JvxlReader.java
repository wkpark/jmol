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
import java.util.BitSet;
import java.util.Vector;

import org.jmol.shapesurface.IsosurfaceMesh;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.Escape;
import org.jmol.util.ColorEncoder;
import org.jmol.g3d.Graphics3D;
import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.VolumeData;

public class JvxlReader extends VolumeFileReader {

  protected final static String JVXL_VERSION = "2.1";
  
  // 1.4 adds -nContours to indicate contourFromZero for MEP data mapped onto planes
  // 2.0 adds vertex/triangle compression when no grid is present 
  // Jmol 11.7.25 -- recoded so that we do not create voxelData[nx][ny][nz] and instead
  //                 simply create a BitSet of length nx * ny * nz. This saves memory hugely.
  // 2.1 adds JvxlXmlReader
  
  JvxlReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
    jvxlData.wasJvxl = isJvxl = true;
    isXLowToHigh = false;
  }

  //// methods used for reading any file format, but creating a JVXL file

  /////////////reading the format///////////

  protected int surfaceDataCount;
  protected int edgeDataCount;
  protected int colorDataCount;
  protected boolean haveContourData;

  protected boolean readVolumeData(boolean isMapData) {
    if (!super.readVolumeData(isMapData))
      return false;
    strFractionTemp = jvxlEdgeDataRead;
    fractionPtr = 0;
    return true;
  }

  protected boolean gotoAndReadVoxelData(boolean isMapData) {
    initializeVolumetricData();
    if (nPointsX < 0 || nPointsY < 0 || nPointsZ < 0) 
      return true;
    try {
      gotoData(params.fileIndex - 1, nPointsX * nPointsY * nPointsZ);
      if (vertexDataOnly)
        return true;
      readSurfaceData(isMapData);
      if (edgeDataCount > 0)
        jvxlEdgeDataRead = jvxlReadData("edge", edgeDataCount);
      if (colorDataCount > 0)
        jvxlColorDataRead = jvxlReadData("color", colorDataCount);
      if (haveContourData)
        jvxlDecodeContourData(jvxlData, getXmlData("jvxlContourData", null, false));
    } catch (Exception e) {
      Logger.error(e.toString());
      try {
      br.close();
      } catch (Exception e2) {
        // ignore
      }
      return false;
    }
    return true;
  }
  
  protected boolean thisInside;
  
  protected void initializeVoxelData() {
    thisInside = !params.isContoured;
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
    jvxlData.setSurfaceInfo(params.thePlane, 0, "");
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
    jvxlFileHeaderBuffer = new StringBuffer(skipComments(false));
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
    if (atomLine.indexOf("ANGSTROMS") >= 0)
      isAngstroms = true;
    int atomCount = (strAtomCount == null ? Integer.MAX_VALUE : Parser.parseInt(strAtomCount));
    switch(atomCount) {
    case Integer.MIN_VALUE:
        atomCount = 0;
        atomLine = " " + atomLine.substring(atomLine.indexOf(" ") + 1);
      break;
    case Integer.MAX_VALUE:
      atomCount = Integer.MIN_VALUE;
      break;
    default:
        String s = "" + atomCount;
        atomLine = atomLine.substring(atomLine.indexOf(s) + s.length());
      }
    atomLine = JvxlCoder.fixAtomLineVersion1(atomCount, atomLine, isXLowToHigh, isAngstroms);
    bs.append(atomLine);
    return isAngstroms;
  }
  
  protected void readAtomCountAndOrigin() throws Exception {
      jvxlFileHeaderBuffer.append(skipComments(false));
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
    v.setVolumetricXml();
    //mostly ignored
    for (int i = 0; i < atomCount; ++i)
      bs.append(br.readLine() + "\n");
    //if (atomCount == 0)
      //jvxlAddDummyAtomList(v, bs);
  }

  protected int readExtraLine() throws Exception {
    skipComments(true);
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

  protected void jvxlReadDefinitionLine(boolean showMsg) throws Exception {
    // params values come from user adding options to the isosurface command
    // jvxlData values are from this file
    String comment = skipComments(true);
    if (showMsg)
      Logger.info("reading jvxl data set: " + comment + line);
    haveContourData = (comment.indexOf("+contourlines") >= 0);
    jvxlCutoff = parseFloat(line);
    Logger.info("JVXL read: cutoff " + jvxlCutoff);

    //  optional comment line for compatibility with earlier Jmol versions:
    //  #+contourlines
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
      Logger.info("JVXL read: plane " + params.thePlane);
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
      if (nContoursRead == Integer.MIN_VALUE) {
        if (line.charAt(next[0]) == '[') {
           jvxlData.contourValues = params.contoursDiscrete = parseFloatArray();
           Logger.info("JVXL read: contourValues " + Escape.escapeArray(jvxlData.contourValues));            
           jvxlData.contourColixes = params.contourColixes = Graphics3D.getColixArray(getNextQuotedString());
           jvxlData.contourColors = Graphics3D.getHexCodes(jvxlData.contourColixes);
           Logger.info("JVXL read: contourColixes " + jvxlData.contourColors); 
           params.nContours = jvxlData.contourValues.length;
                 }
      } else {
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

    jvxlData.isJvxlPrecisionColor = (param1 == -1 && param2 == -2 
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
        Logger.info("JVXL read: data_min/max " + params.mappedDataMin + "/"
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

  protected String jvxlReadData(String type, int nPoints) {
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

  public static String jvxlCompressString(String data) {
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
      if (ch == '\n' || ch == '\r')
        continue;
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

  protected static String jvxlUncompressString(String data) {
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

  protected BitSet bsVoxelBitSet;

  protected BitSet getVoxelBitSet(int nPoints) throws Exception {
    if (bsVoxelBitSet != null)
      return bsVoxelBitSet;
    BitSet bs = new BitSet();
    int bsVoxelPtr = 0;
    if (surfaceDataCount <= 0)
      return bs; //unnecessary -- probably a plane
    int nThisValue = 0;
    while (bsVoxelPtr < nPoints) {
      nThisValue = parseInt();
      if (nThisValue == Integer.MIN_VALUE) {
        line = br.readLine();
        // note -- does not allow for empty lines;
        // must be a continuous block of numbers.
        if (line == null || (nThisValue = parseInt(line)) == Integer.MIN_VALUE) {
          if (!endOfData)
            Logger.error("end of file in JvxlReader?" + " line=" + line);
          endOfData = true;
          nThisValue = 10000;
          //throw new NullPointerException();
        }
      } 
      thisInside = !thisInside;
      ++jvxlNSurfaceInts;
      if (thisInside)
        bs.set(bsVoxelPtr, bsVoxelPtr + nThisValue);
      bsVoxelPtr += nThisValue;
    }
    return bs;
  }
  
  protected float getSurfacePointAndFraction(float cutoff, boolean isCutoffAbsolute, float valueA,
                         float valueB, Point3f pointA, Vector3f edgeVector, 
                         float[] fReturn, Point3f ptReturn) {
    if (edgeDataCount <= 0)
      return super.getSurfacePointAndFraction(cutoff, isCutoffAbsolute, valueA, valueB,
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
        Logger.error("end of file reading compressed fraction data");
      endOfData = true;
      strFractionTemp = "" + (char) base;
      fractionPtr = 0;
    }
    return JvxlCoder.jvxlFractionFromCharacter(strFractionTemp.charAt(fractionPtr++),
        base, range, fracOffset);
  }

  protected String readColorData() {
    // overloads SurfaceReader
    // standard jvxl file read for color 

    fractionPtr = 0;
    int vertexCount = jvxlData.vertexCount = meshData.vertexCount;
    short[] colixes = meshData.vertexColixes;
    float[] vertexValues = meshData.vertexValues;
    strFractionTemp = (isJvxl ? jvxlColorDataRead : "");
    if (isJvxl && strFractionTemp.length() == 0) {
      Logger
          .error("You cannot use JVXL data to map onto OTHER data, because it only contains the data for one surface. Use ISOSURFACE \"file.jvxl\" not ISOSURFACE .... MAP \"file.jvxl\".");
      return "";
    }
    fractionPtr = 0;
    Logger.info("JVXL reading color data mapped min/max: " + params.mappedDataMin
        + "/" + params.mappedDataMax + " for " + vertexCount + " vertices."
        + " using encoding keys " + colorFractionBase + " "
        + colorFractionRange);
    Logger.info("mapping red-->blue for " + params.valueMappedToRed + " to "
        + params.valueMappedToBlue + " colorPrecision:"
        + jvxlData.isJvxlPrecisionColor);

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
    String data = jvxlColorDataRead;
    //hasColorData = true;
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
      if (jvxlData.isJvxlPrecisionColor) {
        // this COULD be an option for mapped surfaces; 
        // necessary for planes; used for vertex/triangle 2.0 style
        // precision is used for FULL-data range encoding, allowing full
        // treatment of JVXL files as though they were CUBE files.
        // the two parts of the "double-character-precision" value
        // are in separate lines, separated by n characters.
        fraction = JvxlCoder.jvxlFractionFromCharacter2(data.charAt(cpt), data.charAt(cpt
            + vertexCount), colorFractionBase, colorFractionRange);
        value = min + fraction * range;
      } else {
        // my original encoding scheme
        // low precision only allows for mapping relative to the defined color range
        fraction = JvxlCoder.jvxlFractionFromCharacter(data.charAt(cpt),
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
      if (marchingSquares != null && params.isContoured) {
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

  protected void jvxlSkipData(int nPoints, boolean doSkipColorData)
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
    int n = 0;
    while (n < nPoints) {
      line = br.readLine();
      n += (isInt ? countData(line) : jvxlUncompressString(line).length());
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

  /**
   * retrieve Jvxl 2.0 format vertex/triangle/color data found
   * within <jvxlSurfaceData> element 
   * 
   * @throws Exception
   */
  private void getEncodedVertexData() throws Exception {
    String data = getXmlData("jvxlSurfaceData", null, true);
    jvxlDecodeVertexData(getXmlData("jvxlVertexData", data, true), false);
    String polygonColorData = getXmlData("jvxlPolygonColorData", data, false);
    jvxlDecodeTriangleData(getXmlData("jvxlTriangleData", data, true), polygonColorData);
    Logger.info("Checking for vertex values");
    data = jvxlUncompressString(getXmlData("jvxlColorData", data, true));
    jvxlData.isJvxlPrecisionColor = getXmlAttrib(data, "precision").equals("true");
    jvxlColorDataRead = getXmlAttrib(data, "data");
    if (jvxlColorDataRead.length() == 0)
      jvxlColorDataRead = getXmlData("jvxlColorData", data, false);
    jvxlDataIsColorMapped = (jvxlColorDataRead.length() > 0);
    if (haveContourData)
      jvxlDecodeContourData(jvxlData, getXmlData("jvxlContourData", null, false));
    Logger.info("Done");
  }

  public void jvxlDecodeContourData(JvxlData jvxlData, String data) throws Exception {
    Vector vs = new Vector();
    int pt = -1;
    jvxlData.vContours = null;
    if (data == null)
      return;
    while ((pt = data.indexOf("<jvxlContour", pt + 1)) >= 0) {
      Vector v = new Vector();
      String s = getXmlData("jvxlContour", data.substring(pt), true);
      int n = parseInt(getXmlAttrib(s, "npolygons"));
      float value = parseFloat(getXmlAttrib(s, "value"));
      short colix = Graphics3D.getColix(Graphics3D.getArgbFromString(getXmlAttrib(s,
          "color")));
      int color = Graphics3D.getArgb(colix);
      String fData = getXmlAttrib(s, "data");
      BitSet bs = JvxlCoder.jvxlDecodeBitSet(getXmlData("jvxlContour", s, false));
      IsosurfaceMesh.setContourVector(v, n, bs, value, colix, color, new StringBuffer(
          fData));
      vs.add(v);
    }
    int n = vs.size();
    if (n > 0)
      jvxlData.vContours = new Vector[n];
    // 3D contour values and colors
    jvxlData.contourColixes = params.contourColixes = new short[n];
    jvxlData.contourValues = params.contoursDiscrete = new float[n];
    for (int i = 0; i < n; i++) {
      jvxlData.vContours[i] = (Vector) vs.get(i);
      jvxlData.contourValues[i] = ((Float) jvxlData.vContours[i].get(2)).floatValue();
      jvxlData.contourColixes[i] = ((short[]) jvxlData.vContours[i].get(3))[0];
    }
    jvxlData.contourColors = Graphics3D.getHexCodes(jvxlData.contourColixes);
  }

  /**
   * a relatively simple XML reader for this specific application.
   * 
   * @param name
   * @param data
   * @param withTag
   * @return            trimmed contents or tag + contents, never closing tag 
   * @throws Exception
   */
  protected String getXmlData(String name, String data, boolean withTag)
      throws Exception {
    //crude
    String closer = "</" + name + ">";
    String tag = "<" + name;
    if (data == null) {
      StringBuffer sb = new StringBuffer();
      try {
        while (line.indexOf(tag) < 0) {
          line = br.readLine();
        }
      } catch (Exception e) {
        return null;
      }
      sb.append(line);
      while (line.indexOf(closer) < 0)
        sb.append(line = br.readLine());
      data = sb.toString();
    }
    int pt1 = data.indexOf(tag);
    if (pt1 < 0)
      return "";
    int pt2 = data.indexOf(closer, pt1);
    if (pt2 < 0)
      return "";
    if (withTag) {
      pt2 += closer.length();
    } else {
      boolean quoted = false;
      for (;pt1 < pt2; pt1++) {
        char ch;
        if ((ch = data.charAt(pt1)) == '"')
          quoted = !quoted;
        else if (quoted && ch == '\\')
          pt1++;
        else if (!quoted && ch == '>')
          break;
      }
      if (pt1 >= pt2)
        return "";
      while (Character.isWhitespace(data.charAt(++pt1))) {        
      }
    }
    return data.substring(pt1, pt2);
  }

  /**
   * decode vertex data found within <jvxlVertexData> element
   * as created by jvxlEncodeVertexData (see above)
   * 
   * @param data      tag and contents 
   * @param asArray   or just addVertexCopy    
   * @return          Point3f[] if desired 
   * @throws Exception 
   *    
   */
  public Point3f[] jvxlDecodeVertexData(String data, boolean asArray) throws Exception {
    int vertexCount = parseInt(getXmlAttrib(data, "count"));
    if (!asArray)
      Logger.info("Reading " + vertexCount + " vertices");
    Point3f min = getXmlPoint(data, "min");
    Point3f range = getXmlPoint(data, "max");
    range.sub(min);
    int colorFractionBase = jvxlData.colorFractionBase;
    int colorFractionRange = jvxlData.colorFractionRange;
    int ptCount = vertexCount * 3;
    Point3f[] vertices = (asArray ? new Point3f[vertexCount] : null);
    Point3f p = (asArray ? null : new Point3f());
    float fraction;
    String s = getXmlAttrib(data, "data");
    if (s.length() == 0)
      s = getXmlData("jvxlVertexData", data, false); 
    for (int i = 0, pt = -1; i < vertexCount; i++) {
      if (asArray)
        p = vertices[i] = new Point3f();
      fraction = JvxlCoder.jvxlFractionFromCharacter2(s.charAt(++pt), s.charAt(pt
          + ptCount), colorFractionBase, colorFractionRange);
      p.x = min.x + fraction * range.x;
      fraction = JvxlCoder.jvxlFractionFromCharacter2(s.charAt(++pt), s.charAt(pt
          + ptCount), colorFractionBase, colorFractionRange);
      p.y = min.y + fraction * range.y;
      fraction = JvxlCoder.jvxlFractionFromCharacter2(s.charAt(++pt), s.charAt(pt
          + ptCount), colorFractionBase, colorFractionRange);
      p.z = min.z + fraction * range.z;
      if (!asArray)
        addVertexCopy(p, 0, i);
    }
    return vertices;
  }

  /**
   * decode triangle data found within <jvxlTriangleData> element as created
   * with jvxlEncodeTriangleData (see above)
   * 
   * @param data
   *          tag and contents
   * @param colorData
   * @param asArray
   *          or just addTriangleCheck
   * @return int[][] if desired
   * @throws Exception
   */
  int[][] jvxlDecodeTriangleData(String data, String colorData)
      throws Exception {
    int nColors = (colorData == null ? -1 : 0);
    int color = 0;
    int nData = parseInt(getXmlAttrib(data, "count"));
    Logger.info("Reading " + nData + " triangles");
    int[][] triangles = null;
    int[] triangle = new int[3];
    String s = getXmlAttrib(data, "data");
    if (s.length() == 0)
      s = getXmlData("jvxlTriangleData", data, false);
    int[] nextp = new int[1];
    int[] nextc = new int[1];
    int ilast = 0;
    int p = 0;
    int b0 = (int) '\\';
    for (int i = 0, pt = -1; i < nData;) {
      char ch = s.charAt(++pt);
      int idiff;
      switch (ch) {
      case '!':
        idiff = 0;
        break;
      case '+':
      case '.':
      case ' ':
      case '\n':
      case '\r':
      case '\t':
      case ',':
        continue;
      case '-':
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        nextp[0] = pt;
        idiff = Parser.parseInt(s, nextp);
        pt = nextp[0] - 1;
        break;
      default:
        idiff = (int) ch - b0;
      }
      ilast += idiff;
      triangle[p] = ilast;
      if (++p % 3 == 0) {
        i++;
        p = 0;
        if (nColors >= 0) {
          if (nColors == 0) {
            nColors = Parser.parseInt(colorData, nextc);
            color = Parser.parseInt(colorData, nextc);
            if (color == Integer.MIN_VALUE)
              color = nColors = 0;
          }
          nColors--;
        }
        addTriangleCheck(triangle[0], triangle[1], triangle[2], 7, 0, false,
            color);
      }
    }
    return triangles;
  }

  protected Point3f getXmlPoint(String data, String key) {
    String spt = getXmlAttrib(data, key).replace('(', '{').replace(')', '}');
    Object value = Escape.unescapePoint(spt);
    if (value instanceof Point3f)
      return (Point3f) value;
    return new Point3f();
  }

  protected static String getXmlAttrib(String data, String what) {
    // presumes what="xxxx" exactly like that, no whitespace around =
    // no escaped "; no check for spurious "what"
    int[] nexta = new int[1];
    int pt = setNext(data, what, nexta, 2);
    if (pt < 2)
      return "";
    int pt1 = setNext(data, "\"", nexta, -1);
    return (pt1 <= 0 ? "" : data.substring(pt, pt1));
  }
  /**
   * shift pointer to a new tag or field contents
   * 
   * @param data   string of data
   * @param what   tag or field name
   * @param next   current pointer into data
   * @param offset offset past end of "what" for pointer
   * @return pointer to data
   */
  private static int setNext(String data, String what, int[] next, int offset) {
    int ipt = next[0];
    if (ipt < 0 || (ipt = data.indexOf(what, next[0])) < 0)
      return -1;
    return next[0] = ipt + what.length() + offset;
  }
}
