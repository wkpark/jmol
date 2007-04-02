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

import java.io.BufferedReader;

import javax.vecmath.Vector3f;

import org.openscience.jvxl.util.*;

class VolumeFileReader extends VoxelReader {

  BufferedReader br;
  boolean endOfData;

  private int atomCount;
  private boolean negativeAtomCount;
  private int nSurfaces;
  
  
  VolumeFileReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg);
    this.br = br;
  }

  static String determineFileType(BufferedReader bufferedReader) {
    // JVXL should be on the FIRST line of the file, but it may be 
    // after comments or missing. 
    LimitedLineReader br = new LimitedLineReader(bufferedReader, 10);
    String line;
    if ((line = br.info()).indexOf("#JVXL+") == 0)
      return "Jvxl+";
    if (line.indexOf("#JVXL") == 0)
      return "Jvxl";
    //must investigate further
    br = new LimitedLineReader(bufferedReader, 16000);
    line = br.readNonCommentLine();
    if (line.indexOf("object 1 class gridpositions counts") == 0)
      return "Apbs";
    if (br.info().indexOf("Jmol voxel format") >= 0) // early files (pre 4/2007) only had this)
      return "Jvxl";
    line = br.readNonCommentLine(); // second line
    line = br.readNonCommentLine(); // third line
    if (br.iLine() > 3)
      return "Jvxl"; //Can't be a cube file -- has more than two lines of comments
    //next line should be the atom line
    int nAtoms = Parser.parseInt(line);
    if (nAtoms == Integer.MIN_VALUE)
      return "UNKNOWN";
    if (nAtoms >= 0)
      return "Cube"; //Can't be a Jvxl file
    nAtoms = -nAtoms;
    for (int i = 4 + nAtoms; --i >=0;)
      if ((line = br.readNonCommentLine()) == null)
        return "UNKNOWN";
    int nSurfaces = Parser.parseInt(line);
    if (nSurfaces == Integer.MIN_VALUE)
      return "UNKNOWN";
    return (nSurfaces < 0 ?  "Jvxl" : "Cube"); //Final test looks at surface definition line
  }
  
  void discardTempData(boolean discardAll) {
    try {
      if (br != null)
        br.close();
    } catch (Exception e) {
    }
    super.discardTempData(discardAll);
  }
      
  void readData(boolean isMapData) {
    //augmented in JvxlReader
    super.readData(isMapData);
    endOfData = false;
    nSurfaces = readVolumetricHeader();
    if (nSurfaces < params.fileIndex) {
      Logger.warn("not enough surfaces in file -- resetting params.fileIndex to "
          + nSurfaces);
      params.fileIndex = nSurfaces;
    }
    if (isJvxl)
      return;
    readVolumetricData(isMapData);
    Logger.info("Read " + nPointsX + " x " + nPointsY + " x " + nPointsZ
        + " data points");
  }

  int readVolumetricHeader() {
    edgeFractionBase = defaultEdgeFractionBase;
    edgeFractionRange = defaultEdgeFractionRange;
    colorFractionBase = defaultColorFractionBase;
    colorFractionRange = defaultColorFractionRange;
    try {
        readTitleLines();
        Logger.info(jvxlFileHeaderBuffer.toString());
        readAtomCountAndOrigin();
        Logger.info("voxel grid origin:" + volumetricOrigin);
        for (int i = 0; i < 3; ++i) {
          readVoxelVector(i);
          Logger.info("voxel grid vector:" + volumetricVectors[i]);
        }
      setupMatrix(volumetricMatrix, volumetricVectors);
      JvxlReader.jvxlReadAtoms(br, jvxlFileHeaderBuffer, atomCount, volumeData);
      return readExtraLine();
    } catch (Exception e) {
      Logger.error(e.toString());
      throw new NullPointerException();
    }
  }
  
  void readTitleLines() throws Exception {
    //implemented in CubeReader, ApbsReader, and JvxlReader  
  }
  
  int skipComments(boolean addToHeader) throws Exception {
    int n = 1;
    while ((line = br.readLine()) != null && 
        (!addToHeader && line.length() == 0 || line.indexOf("#") == 0)) {
      if (addToHeader)
        jvxlFileHeaderBuffer.append(line).append('\n');
      n++;
    }
    return n;
  }
  
  void readAtomCountAndOrigin() throws Exception {
    skipComments(true);
    String atomLine = line;
    String[] tokens = Parser.getTokens(atomLine, 0);
    atomCount = parseInt(tokens[0]);
    if (atomCount == Integer.MIN_VALUE) //unreadable
      atomCount = 0;
    negativeAtomCount = (atomCount < 0);
    if (negativeAtomCount)
      atomCount = -atomCount;
    if (tokens.length >= 4)
      volumetricOrigin.set(parseFloat(tokens[1]), parseFloat(tokens[2]), parseFloat(tokens[3]));
    isAngstroms = JvxlReader.jvxlCheckAtomLine(isAngstroms, atomLine, jvxlFileHeaderBuffer);
    if (!isAngstroms)
      volumetricOrigin.scale(ANGSTROMS_PER_BOHR);
  }

  void adjustVoxelVectorLine(int voxelVectorIndex) {
    //for APBS reader
  }

  void readVoxelVector(int voxelVectorIndex) throws Exception {    
    line = br.readLine();
    adjustVoxelVectorLine(voxelVectorIndex);
    jvxlFileHeaderBuffer.append(line).append('\n');
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
  }

  int readExtraLine() throws Exception {
    if (!negativeAtomCount)
      return 1;
    line = br.readLine();
    Logger.info("Reading extra CUBE information line: " + line);
    return parseInt(line);
    //trouble is, now we have to switch readers!
  }

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
     * Planes are compatible with data sets that return actual 
     * numbers at all grid points -- cube files, orbitals, functionXY,
     * and solvent/molecular surface calculations.
     *  
     * It is possible to map a QM orbital onto a plane. In the first pass we defined
     * the plane; in the second pass we just calculate the new voxel values and return.
     * 
     */

    if (nPointsX <= 0 || nPointsY <= 0 || nPointsZ <= 0)
      return;

    if (!isMapData && params.thePlane != null) {
      readPlaneData();
      return;
    }

    boolean inside = false;
    int dataCount = 0;
    next[0] = 0;
    int nPoints = nPointsX * nPointsY * nPointsZ;
    gotoData(params.fileIndex - 1, nPoints);

    if (params.thePlane != null) {
      setPlaneParameters(params.thePlane);
      params.cutoff = 0f;
    } else if (isJvxl) {
      params.cutoff = (params.isBicolorMap || params.colorBySign ? 0.01f : 0.5f);
    }
    voxelData = new float[nPointsX][][];
    nDataPoints = 0;
    line = "";
    StringBuffer sb = new StringBuffer();
    jvxlNSurfaceInts = 0;
    boolean collectData = (!isJvxl && params.thePlane == null);
    if (isMapData || isJvxl && params.thePlane == null) {
      for (int x = 0; x < nPointsX; ++x) {
        float[][] plane = new float[nPointsY][];
        voxelData[x] = plane;
        for (int y = 0; y < nPointsY; ++y) {
          float[] strip = new float[nPointsZ];
          plane[y] = strip;
          for (int z = 0; z < nPointsZ; ++z) {
            strip[z] = getNextVoxelValue(sb);
            ++nDataPoints;
          }
        }
      }
    } else {
      float cutoff = params.cutoff;
      boolean isCutoffAbsolute = params.isCutoffAbsolute;
      for (int x = 0; x < nPointsX; ++x) {
        float[][] plane;
        plane = new float[nPointsY][];
        voxelData[x] = plane;
        for (int y = 0; y < nPointsY; ++y) {
          float[] strip = new float[nPointsZ];
          plane[y] = strip;
          for (int z = 0; z < nPointsZ; ++z) {
            float voxelValue = getNextVoxelValue(sb);
            strip[z] = voxelValue;
            ++nDataPoints;
            if (inside == isInside(voxelValue, cutoff, isCutoffAbsolute)) {
              dataCount++;
            } else {
              if (collectData && dataCount != 0) {
                sb.append(' ').append(dataCount);
                ++jvxlNSurfaceInts;
              }
              dataCount = 1;
              inside = !inside;
            }
          }
        }
      }
    }
    //Jvxl getNextVoxelValue records the data read on its own.
    if (collectData) {
      sb.append(' ').append(dataCount).append('\n');
      ++jvxlNSurfaceInts;
    }
    if (!isMapData)
      JvxlReader.setSurfaceInfo(jvxlData, params.thePlane, jvxlNSurfaceInts, sb);
    volumeData.setVoxelData(voxelData);
  }

  float getNextVoxelValue(StringBuffer sb) throws Exception {
    //overloaded in JvxlReader, where sb is appended to
    float voxelValue = 0;
    if (nSurfaces > 1 && !params.blockCubeData) {
      for (int i = 1; i < params.fileIndex; i++)
        nextVoxel();
      voxelValue = nextVoxel();
      for (int i = params.fileIndex; i < nSurfaces; i++)
        nextVoxel();
    } else {
      voxelValue = nextVoxel();
    }
    return voxelValue;
  }

  private float nextVoxel() throws Exception {
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
    if (!params.blockCubeData)
      return;
    if (n > 0)
      Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
    for (int i = 0; i < n; i++)
      skipData(nPoints);
  }

  void skipData(int nPoints) throws Exception {
    int iV = 0;
    while (iV < nPoints) {
      line = br.readLine();
      iV += countData(line);
    }
  }

  private int countData(String str) {
    int count = 0;
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

class LimitedLineReader {
  //from Resolver
  private char[] buf;
  private int cchBuf;
  private int ichCurrent;
  private int iLine;

  LimitedLineReader(BufferedReader bufferedReader, int readLimit) {
    buf = new char[readLimit];
    try {
      bufferedReader.mark(readLimit);
      cchBuf = bufferedReader.read(buf);
      ichCurrent = 0;
      bufferedReader.reset();
    } catch (Exception e) {      
    }
  }

  String info() {
    return new String(buf);  
  }
  
  int iLine() {
    return iLine;
  }
  
  String readNonCommentLine() {
    while (ichCurrent < cchBuf) {
      int ichBeginningOfLine = ichCurrent;
      char ch = 0;
      while (ichCurrent < cchBuf &&
             (ch = buf[ichCurrent++]) != '\r' && ch != '\n') {
      }
      int cchLine = ichCurrent - ichBeginningOfLine;
      if (ch == '\r' && ichCurrent < cchBuf && buf[ichCurrent] == '\n')
        ++ichCurrent;
      iLine++;
      if (buf[ichBeginningOfLine] == '#') // flush comment lines;
        continue;
      StringBuffer sb = new StringBuffer(cchLine);
      sb.append(buf, ichBeginningOfLine, cchLine);
      return sb.toString();
    }
    return "";
  }
}
