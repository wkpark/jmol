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

import java.io.BufferedReader;

import javax.vecmath.Point4f;

import org.jmol.g3d.Graphics3D;
import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

public class JvxlXmlReader extends JvxlReader {

  JvxlXmlReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
  }

  protected void readTitleLines() throws Exception {
    line = br.readLine();
    while (line != null && line.indexOf("<jvxl") < 0)
      line = br.readLine();
    jvxlFileHeaderBuffer = new StringBuffer();
    line = "Line 1";
    jvxlFileHeaderBuffer.append(line).append('\n');
    line = "Line 2";
    jvxlFileHeaderBuffer.append(line).append('\n');
  }

  String tempDataXml; 
  protected void readAtomCountAndOrigin() throws Exception {
    skipTo("<jvxlVolumeData");
    String data = tempDataXml = getXmlData("jvxlVolumeData", null, true);
    volumetricOrigin.set(getXmlPoint(data, "origin"));
    isAngstroms = true;
  }

  protected void readVoxelVector(int voxelVectorIndex) throws Exception {
    String data = getXmlData("jvxlVolumeVector", tempDataXml, true);
    tempDataXml = tempDataXml.substring(tempDataXml.indexOf(data) + data.length());
    voxelCounts[voxelVectorIndex] = parseInt(getXmlAttrib(data, "count"));
    volumetricVectors[voxelVectorIndex].set(getXmlPoint(data, "vector"));
  }

  protected int readExtraLine() throws Exception {
    skipTo("<jvxlSurfaceSet");
    int nSurfaces = parseInt(getXmlAttrib(line, "count"));
    Logger.info("jvxl file surfaces: " + nSurfaces);
    Logger.info("using default edge fraction base and range");
    Logger.info("using default color fraction base and range");
    cJvxlEdgeNaN = (char) (edgeFractionBase + edgeFractionRange);
    return nSurfaces;
  }

  protected void gotoData(int n, int nPoints) throws Exception {
    if (n > 0)
      Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
    vertexDataOnly = jvxlData.vertexDataOnly = (nPoints == 0);
    for (int i = 0; i < n; i++) {
      jvxlSkipData(nPoints, true);
    }
    skipTo("<jvxlSurface");
    jvxlReadSurfaceInfo();
  }

  private void skipTo(String key) throws Exception {
    if (line == null)
      line = br.readLine();
    while (line != null && line.indexOf(key) < 0)
      line = br.readLine();
  }

  protected void jvxlSkipData(int nPoints, boolean doSkipColorData)
      throws Exception {
    skipTo("</jvxlSurface>");
  }

  protected void jvxlReadSurfaceInfo() throws Exception {
    String s;
    String data = getXmlData("jvxlSurfaceInfo", null, true);
    isXLowToHigh = getXmlAttrib(data, "isXLowToHigh").equals("true");
    jvxlCutoff = parseFloat(getXmlAttrib(data, "cutoff"));
    Logger.info("JVXL read: cutoff " + jvxlCutoff);
    int nContourData = parseInt(getXmlAttrib(data, "nContourData"));
    haveContourData = (nContourData > 0);
    params.isContoured = getXmlAttrib(data, "contoured").equals("true");
    if (params.isContoured) {
      int nContoursRead = parseInt(getXmlAttrib(data, "nContours"));
      if (nContoursRead <= 0) {
        nContoursRead = 0;
      } else {
        s = getXmlAttrib(data, "contourValues");
        if (s.length() > 0) {
          jvxlData.contourValues = params.contoursDiscrete = parseFloatArray(s);
          Logger.info("JVXL read: contourValues " + Escape.escapeArray(jvxlData.contourValues));            
        }
        s = getXmlAttrib(data, "contourColors");
        if (s.length() > 0) {
          jvxlData.contourColixes = params.contourColixes = Graphics3D.getColixArray(s);
          jvxlData.contourColors = Graphics3D.getHexCodes(jvxlData.contourColixes);
          Logger.info("JVXL read: contourColixes " +
              Graphics3D.getHexCodes(jvxlData.contourColixes));        }
        params.contourFromZero = getXmlAttrib(data, "contourFromZero").equals("true");
      }
      params.nContours = (haveContourData ? nContourData : nContoursRead);
      //TODO ? params.contourFromZero = false; // MEP data to complete the plane
    }
    params.isBicolorMap = getXmlAttrib(data, "bicolorMap").equals("true");
    if (params.isBicolorMap || params.colorBySign)
      jvxlCutoff = 0;
    jvxlDataIsColorMapped = getXmlAttrib(data, "colorMapped").equals("true");
    jvxlData.isJvxlPrecisionColor = getXmlAttrib(data, "precisionColor").equals("true");
    s = getXmlAttrib(data, "plane");
    if (s.indexOf("{") >= 0) {
      try {
        params.thePlane = (Point4f) Escape.unescapePoint(s);
        Logger.info("JVXL read: plane " + params.thePlane);
      } catch (Exception e) {
        Logger
            .error("Error reading 4 floats for PLANE definition -- setting to 0 0 1 0  (z=0)");
        params.thePlane = new Point4f(0, 0, 1, 0);
      }
      surfaceDataCount = 0;
      edgeDataCount = 0;
    } else {
      params.thePlane = null;
      surfaceDataCount = parseInt(getXmlAttrib(data, "nSurfaceInts"));
      edgeDataCount = parseInt(getXmlAttrib(data, "nBytesUncompressedEdgeData"));
    }
    colorDataCount = Math.max(0, parseInt(getXmlAttrib(data, "nBytesUncompressedColorData")));
    jvxlDataIs2dContour = (params.thePlane != null && jvxlDataIsColorMapped);
    if (jvxlDataIs2dContour)
      params.isContoured = true;
    
    if (params.colorBySign)
      params.isBicolorMap = true;
    if (jvxlDataIsColorMapped) {
      float dataMin = parseFloat(getXmlAttrib(data, "dataMinimum"));
      float dataMax = parseFloat(getXmlAttrib(data, "dataMaximum"));
      float red = parseFloat(getXmlAttrib(data, "valueMappedToRed"));
      float blue = parseFloat(getXmlAttrib(data, "valueMappedToBlue"));
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
    jvxlData.insideOut = getXmlAttrib(data, "insideOut").equals("true");
    if (params.insideOut)
      jvxlData.insideOut = !jvxlData.insideOut;
    params.insideOut = jvxlData.insideOut;
    jvxlData.valueMappedToRed = params.valueMappedToRed;
    jvxlData.valueMappedToBlue = params.valueMappedToBlue;
    jvxlData.mappedDataMin = params.mappedDataMin;
    jvxlData.mappedDataMax = params.mappedDataMax;
  }

  protected void readSurfaceData(boolean isMapDataIgnored) throws Exception {
    if (!vertexDataOnly && params.thePlane == null) {
      initializeVoxelData();
      tempDataXml = getXmlData("jvxlEdgeData", null, true);
      bsVoxelBitSet = JvxlCoder.jvxlDecodeBitSet(getXmlData("jvxlEdgeData", tempDataXml, false));
      //if (thisInside)
        //bsVoxelBitSet = BitSetUtil.copyInvert(bsVoxelBitSet, bsVoxelBitSet.size());
    }
    super.readSurfaceData(isMapDataIgnored);
  }

  protected String jvxlReadData(String type, int nPoints) {
    String str;
    try {
      if (type.equals("edge")) {
        str = getXmlAttrib(tempDataXml, "data");
      } else {
        str = getXmlAttrib(getXmlData("jvxlColorData", null, true), "data"); 
      }
    } catch (Exception e) {
      Logger.error("Error reading " + type + " data " + e);
      throw new NullPointerException();
    }
    return JvxlCoder.jvxlUncompressString(str);
  }
}
