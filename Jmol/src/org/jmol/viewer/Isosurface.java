/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005 Miguel, Jmol Development
 *
 * Contact: miguel@jmol.org,jmol-developers@lists.sourceforge.net
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

/*
 * miguel 2005 07 17
 *
 *  System and method for the display of surface structures
 *  contained within the interior region of a solid body
 * United States Patent Number 4,710,876
 * Granted: Dec 1, 1987
 * Inventors:  Cline; Harvey E. (Schenectady, NY);
 *             Lorensen; William E. (Ballston Lake, NY)
 * Assignee: General Electric Company (Schenectady, NY)
 * Appl. No.: 741390
 * Filed: June 5, 1985
 *
 *
 * Patents issuing prior to June 8, 1995 can last up to 17
 * years from the date of issuance.
 *
 * Dec 1 1987 + 17 yrs = Dec 1 2004
 */

package org.jmol.viewer;
import org.jmol.g3d.*;

import java.util.BitSet;
import java.io.BufferedReader;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;

class Isosurface extends MeshCollection {

  final float ANGSTROMS_PER_BOHR = JmolConstants.ANGSTROMS_PER_BOHR;

  final Point3f volumetricOrigin = new Point3f();
  final Vector3f[] volumetricVectors = new Vector3f[3];
  {
    for (int i = 3; --i >= 0; )
      volumetricVectors[i] = new Vector3f();
  }
  final Vector3f[] unitVolumetricVectors = new Vector3f[3];
  {
    for (int i = 3; --i >= 0; )
      unitVolumetricVectors[i] = new Vector3f();
  }
  final float[] volumetricVectorLengths = new float[3];

  final int[] voxelCounts = new int[3];
  final Matrix3f volumetricMatrix = new Matrix3f();
  float[][][] voxelData;

  int edgePointCount = 0;
  Point3f[] edgePoints;

  float cutoff = 0.02f;

  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("bufferedreader" == propertyName) {
      BufferedReader br = (BufferedReader)value;
      if (currentMesh == null)
        allocMesh(null);
      currentMesh.clear();
      readVolumetricHeader(br);
      calcVolumetricMatrix();
      readVolumetricData(br);
      calcVoxelVertexVectors();
      constructTessellatedSurface();
      currentMesh.colix = getDefaultColix();
      currentMesh.initialize();
      currentMesh.checkForDuplicatePoints(.001f);
      currentMesh.visible = true;
      discardTempData();
      return;
    }
    if ("cutoff" == propertyName) {
      cutoff = ((Float)value).floatValue();
      return;
    }
    if ("colorreader" == propertyName) {
      BufferedReader br = (BufferedReader)value;
      System.out.println("colorreader seen!");
      readVolumetricHeader(br);
      calcVolumetricMatrix();
      readVolumetricData(br);
      float minMappedValue = getMinMappedValue();
      float maxMappedValue = getMaxMappedValue();
      System.out.println(" minMappedValue=" + minMappedValue +
                         " maxMappedValue=" + maxMappedValue);
      applyColorScale(minMappedValue, maxMappedValue, "roygb");
      discardTempData();
      return;
    }
    super.setProperty(propertyName, value, bs);
  }
  
  void calcVolumetricMatrix() {
    for (int i = 3; --i >= 0; )
      volumetricMatrix.setColumn(i, volumetricVectors[i]);
  }

  void discardTempData() {
    edgePointCount = 0;
    edgePoints = null;
    voxelData = null;
  }

  /*
    System.out.println("setProperty(" + propertyName + "," + value + ")");
    if ("load" == propertyName) {
      volumetricOrigin = new Point3f((float[])((Object[])value)[0]);
      float[][] vvectors = (float[][])((Object[])value)[1];
      for (int i = 3; --i >= 0; ) {
        volumetricVectors[i] = new Vector3f(vvectors[i]);
      }
      volumetricData = (float[][][])((Object[])value)[2];

      calcVoxelVertexVectors();
      constructTessellatedSurface();
      mesh.initialize();
      return;
    }
  */

  ////////////////////////////////////////////////////////////////
  // default color stuff
  int indexColorPositive;
  int indexColorNegative;
  ////////////////////////////////////////////////////////////////

  short getDefaultColix() {
    int argb;
    if (cutoff >= 0) {
      indexColorPositive =
        (indexColorPositive %
         JmolConstants.argbsIsosurfacePositive.length);
      argb = JmolConstants.argbsIsosurfacePositive[indexColorPositive++];
    } else {
      indexColorNegative =
        (indexColorNegative %
         JmolConstants.argbsIsosurfaceNegative.length);
      argb = JmolConstants.argbsIsosurfaceNegative[indexColorNegative++];
    }
    return g3d.getColix(argb);
  }

  ////////////////////////////////////////////////////////////////
  // file reading stuff
  ////////////////////////////////////////////////////////////////

  void readVolumetricHeader(BufferedReader br) {
    try {
      readTitleLines(br);
      readAtomCountAndOrigin(br);
      readVoxelVectors(br);
      readAtoms(br);
      readExtraLine(br);
    } catch (Exception e) {
      e.printStackTrace();
      throw new NullPointerException();
    }
  }

  void readVolumetricData(BufferedReader br) {
    try {
      readVoxelData(br);
    } catch (Exception e) {
      e.printStackTrace();
      throw new NullPointerException();
    }
  }

  void readTitleLines(BufferedReader br) throws Exception {
    String title;
    title = br.readLine().trim() + " - ";
    title += br.readLine().trim();
  }

  int atomCount;
  boolean negativeAtomCount;

  void readAtomCountAndOrigin(BufferedReader br) throws Exception {
    String line = br.readLine();
    atomCount = parseInt(line);
    volumetricOrigin.x = parseFloat(line, ichNextParse);
    volumetricOrigin.y = parseFloat(line, ichNextParse);
    volumetricOrigin.z = parseFloat(line, ichNextParse);
    volumetricOrigin.scale(ANGSTROMS_PER_BOHR);
    if (atomCount < 0) {
      atomCount = -atomCount;
      negativeAtomCount = true;
    }
  }

  void readVoxelVectors(BufferedReader br) throws Exception {
    for (int i = 0; i < 3; ++i)
      readVoxelVector(br, i);
  }

  void readVoxelVector(BufferedReader br, int voxelVectorIndex)
    throws Exception {
    String line = br.readLine();
    Vector3f voxelVector = volumetricVectors[voxelVectorIndex];
    voxelCounts[voxelVectorIndex] = parseInt(line);
    voxelVector.x = parseFloat(line, ichNextParse);
    voxelVector.y = parseFloat(line, ichNextParse);
    voxelVector.z = parseFloat(line, ichNextParse);
    voxelVector.scale(ANGSTROMS_PER_BOHR);
    volumetricVectorLengths[voxelVectorIndex] = voxelVector.length();
    unitVolumetricVectors[voxelVectorIndex].normalize(voxelVector);
  }

  void readAtoms(BufferedReader br) throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      String line = br.readLine();
      /*
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementNumber = (byte)parseInt(line);
      atom.partialCharge = parseFloat(line, ichNextParse);
      atom.x = parseFloat(line, ichNextParse) * ANGSTROMS_PER_BOHR;
      atom.y = parseFloat(line, ichNextParse) * ANGSTROMS_PER_BOHR;
      atom.z = parseFloat(line, ichNextParse) * ANGSTROMS_PER_BOHR;
      */
    }
  }

  void readExtraLine(BufferedReader br) throws Exception {
    if (negativeAtomCount)
      br.readLine();
  }

  void readVoxelData(BufferedReader br) throws Exception {
    System.out.println("entering readVoxelData");
    String line = "";
    ichNextParse = 0;
    int voxelCountX = voxelCounts[0];
    int voxelCountY = voxelCounts[1];
    int voxelCountZ = voxelCounts[2];
    voxelData = new float[voxelCountX][][];
    for (int x = 0; x < voxelCountX; ++x) {
      float[][] plane = new float[voxelCountY][];
      voxelData[x] = plane;
      for (int y = 0; y < voxelCountY; ++y) {
        float[] strip = new float[voxelCountZ];
        plane[y] = strip;
        for (int z = 0; z < voxelCountZ; ++z) {
          float voxelValue = parseFloat(line, ichNextParse);
          if (Float.isNaN(voxelValue)) {
            line = br.readLine();
            if (line == null || Float.isNaN(voxelValue = parseFloat(line))) {
              System.out.println("end of file in CubeReader?");
              throw new NullPointerException();
            }
          }
          strip[z] = voxelValue;
        }
      }
    }
    System.out.println("Successfully read " + voxelCountX +
                       " x " + voxelCountY +
                       " x " + voxelCountZ + " voxels");
  }

  ////////////////////////////////////////////////////////////////
  // marching cube stuff
  ////////////////////////////////////////////////////////////////

  final float[] vertexValues = new float[8];
  final Point3f[] surfacePoints = new Point3f[12];
  {
    for (int i = 12; --i >= 0; )
      surfacePoints[i] = new Point3f();
  }
  final int[] surfacePointIndexes = new int[12];

  int voxelCountX, voxelCountY, voxelCountZ;

  void constructTessellatedSurface() {
    voxelCountX = voxelData.length - 1;
    voxelCountY = voxelData[0].length - 1;
    voxelCountZ = voxelData[0][0].length - 1;

    int[][] isoPointIndexes = new int[voxelCountY * voxelCountZ][12];
    for (int i = voxelCountY * voxelCountZ; --i >= 0; )
      isoPointIndexes[i] = new int[12];

          /*
    for (int x = 0; x < voxelCountX; ++x)
      for (int y = 0; y < voxelCountY; ++y)
        for (int z = 0; z < voxelCountZ; ++z)
          System.out.println("" + x + "," + y + "," + z + " = " +
                             voxelData[x][y][z]);
          */

    int insideCount = 0, outsideCount = 0, surfaceCount = 0;
    for (int x = voxelCountX; --x >= 0; ) {
      for (int y = voxelCountY; --y >= 0; ) {
        for (int z = voxelCountZ; --z >= 0; ) {
          int insideMask = 0;
          for (int i = 8; --i >= 0; ) {
            Point3i offset = cubeVertexOffsets[i];
            float vertexValue = 
              voxelData[x + offset.x][y + offset.y][z + offset.z];
            vertexValues[i] = vertexValue;
            if ((cutoff > 0 && vertexValue >= cutoff) ||
                (cutoff < 0 && vertexValue <= cutoff))
              insideMask |= 1 << i;
          }

          /*
          for (int i = 0; i < 8; ++i )
            System.out.println("vertexValues[" + i + "]=" +
                                vertexValues[i]);
          System.out.println("insideMask=" + Integer.toHexString(insideMask));
          */

          if (insideMask == 0) {
            ++outsideCount;
            continue;
          }
          if (insideMask == 0xFF) {
            ++insideCount;
            continue;
          }
          ++surfaceCount;
          calcVoxelOrigin(x, y, z);
          int[] voxelPointIndexes =
            propogateNeighborPointIndexes(x, y, z, isoPointIndexes);
          processOneVoxel(insideMask, cutoff, voxelPointIndexes);
        }
      }
    }
    System.out.println("volumetric=" +
                       voxelCountX + "," +
                       voxelCountY + "," +
                       voxelCountZ + "," +
                       " total=" +
                       (voxelCountX*voxelCountY*voxelCountZ) +
                       "\n" + 
                       " insideCount=" + insideCount +
                       " outsideCount=" + outsideCount +
                       " surfaceCount=" + surfaceCount +
                       " total=" +
                       (insideCount+
                        outsideCount+surfaceCount));
  }

  final int[] nullNeighbor = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

  int[] propogateNeighborPointIndexes(int x, int y, int z,
                                      int[][] isoPointIndexes) {
    int cellIndex = y * voxelCountZ + z;
    int[] voxelPointIndexes = isoPointIndexes[cellIndex];

    boolean noXNeighbor = (x == voxelCountX - 1);
    // the x neighbor is myself from my last pass through here
    if (noXNeighbor) {
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
    boolean noYNeighbor = (y == voxelCountY - 1);
    int[] yNeighbor =
      noYNeighbor ? nullNeighbor : isoPointIndexes[cellIndex + voxelCountZ];

    voxelPointIndexes[6] = yNeighbor[2];
    voxelPointIndexes[7] = yNeighbor[3];
    voxelPointIndexes[4] = yNeighbor[0];
    if (noXNeighbor)
      voxelPointIndexes[5] = yNeighbor[1];

    // from my z neighbor
    boolean noZNeighbor = (z == voxelCountZ - 1);
    int[] zNeighbor =
      noZNeighbor ? nullNeighbor : isoPointIndexes[cellIndex + 1];

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

  void dump(int[] pointIndexes) {
    for (int i = 0; i < 12; ++i)
      System.out.println(" " + i + ":" + pointIndexes[i]);
  }

  void processOneVoxel(int insideMask, float cutoff,
                       int[] voxelPointIndexes) {
    int edgeMask = edgeMaskTable[insideMask];
    for (int iEdge = 12; --iEdge >= 0; ) {
      if ((edgeMask & (1 << iEdge)) == 0)
        continue;
      if (voxelPointIndexes[iEdge] >= 0)
        continue; // propogated from neighbor
      int vertexA = edgeVertexes[2*iEdge];
      int vertexB = edgeVertexes[2*iEdge + 1];
      float valueA = vertexValues[vertexA];
      float valueB = vertexValues[vertexB];
      calcVertexPoints(vertexA, vertexB);
      addEdgePoint(pointA);
      addEdgePoint(pointB);
      calcSurfacePoint(cutoff, valueA, valueB, surfacePoints[iEdge]);
      voxelPointIndexes[iEdge] =
        currentMesh.addVertexCopy(surfacePoints[iEdge]);
    }
    
    byte[] triangles = triangleTable[insideMask];
    for (int i = triangles.length; (i -= 3) >= 0; )
      currentMesh.addTriangle(voxelPointIndexes[triangles[i    ]],
                              voxelPointIndexes[triangles[i + 1]],
                              voxelPointIndexes[triangles[i + 2]]);
  }
    
  void calcSurfacePoint(float cutoff, float valueA, float valueB,
                             Point3f surfacePoint) {
    float diff = valueB - valueA;
    float fraction = (cutoff - valueA) / diff;
    if (Float.isNaN(fraction) || fraction < 0 || fraction > 1) {
      System.out.println("fraction=" + fraction +
                         " cutoff=" + cutoff +
                         " A:" + valueA +
                         " B:" + valueB);
      throw new IndexOutOfBoundsException();
    }

    edgeVector.sub(pointB, pointA);
    surfacePoint.scaleAdd(fraction, edgeVector, pointA);
  }

  final Point3f voxelOrigin = new Point3f();
  final Point3f voxelT = new Point3f();
  final Point3f pointA = new Point3f();
  final Point3f pointB = new Point3f();
  // edgeVector should be a table lookup based upon edge number
  // vectors should be derived from the volumetric vectors in the file
  final Vector3f edgeVector = new Vector3f();

  void calcVertexPoints(int vertexA, int vertexB) {
    pointA.add(voxelOrigin, voxelVertexVectors[vertexA]);
    pointB.add(voxelOrigin, voxelVertexVectors[vertexB]);
    /*
    System.out.println("calcVertexPoints(" + vertexA + "," + vertexB + ")\n" +
                       " pointA=" + pointA +
                       " pointB=" + pointB);
    */
  }

  void calcVoxelOrigin(int x, int y, int z) {
    voxelOrigin.scaleAdd(x, volumetricVectors[0], volumetricOrigin);
    voxelOrigin.scaleAdd(y, volumetricVectors[1], voxelOrigin);
    voxelOrigin.scaleAdd(z, volumetricVectors[2], voxelOrigin);
    /*
    System.out.println("voxelOrigin=" + voxelOrigin);
    */
  }

  void addEdgePoint(Point3f point) {
    if (edgePoints == null)
      edgePoints = new Point3f[256];
    else if (edgePointCount == edgePoints.length)
      edgePoints = (Point3f[])Util.doubleLength(edgePoints);
    edgePoints[edgePointCount++] = new Point3f(point);
  }

  final static Point3i[] cubeVertexOffsets = {
    new Point3i(0,0,0),
    new Point3i(1,0,0),
    new Point3i(1,0,1),
    new Point3i(0,0,1),
    new Point3i(0,1,0),
    new Point3i(1,1,0),
    new Point3i(1,1,1),
    new Point3i(0,1,1)
  };

  final static Vector3f[] cubeVertexVectors = {
    new Vector3f(0,0,0),
    new Vector3f(1,0,0),
    new Vector3f(1,0,1),
    new Vector3f(0,0,1),
    new Vector3f(0,1,0),
    new Vector3f(1,1,0),
    new Vector3f(1,1,1),
    new Vector3f(0,1,1)
  };

  static Vector3f[] voxelVertexVectors = new Vector3f[8];

  void calcVoxelVertexVectors() {
    for (int i = 8; --i >= 0; )
      voxelVertexVectors[i] =
        calcVoxelVertexVector(cubeVertexVectors[i]);
    for (int i = 0; i < 8; ++i) {
      System.out.println("voxelVertexVectors[" + i + "]=" +
                         voxelVertexVectors[i]);
    }
  }

  Vector3f calcVoxelVertexVector(Vector3f cubeVectors) {
    Vector3f v = new Vector3f();
    volumetricMatrix.transform(cubeVectors, v);
    return v;
  }

  final static byte edgeVertexes[] = {
    0, 1,
    1, 2,
    2, 3,
    3, 0,
    4, 5,
    5, 6,
    6, 7,
    7, 4,
    0, 4,
    1, 5,
    2, 6,
    3, 7
  };

  final static short edgeMaskTable[] = {
    0x0000, 0x0109, 0x0203, 0x030A, 0x0406, 0x050F, 0x0605, 0x070C,
    0x080C, 0x0905, 0x0A0F, 0x0B06, 0x0C0A, 0x0D03, 0x0E09, 0x0F00,
    0x0190, 0x0099, 0x0393, 0x029A, 0x0596, 0x049F, 0x0795, 0x069C,
    0x099C, 0x0895, 0x0B9F, 0x0A96, 0x0D9A, 0x0C93, 0x0F99, 0x0E90,
    0x0230, 0x0339, 0x0033, 0x013A, 0x0636, 0x073F, 0x0435, 0x053C,
    0x0A3C, 0x0B35, 0x083F, 0x0936, 0x0E3A, 0x0F33, 0x0C39, 0x0D30,
    0x03A0, 0x02A9, 0x01A3, 0x00AA, 0x07A6, 0x06AF, 0x05A5, 0x04AC,
    0x0BAC, 0x0AA5, 0x09AF, 0x08A6, 0x0FAA, 0x0EA3, 0x0DA9, 0x0CA0,
    0x0460, 0x0569, 0x0663, 0x076A, 0x0066, 0x016F, 0x0265, 0x036C,
    0x0C6C, 0x0D65, 0x0E6F, 0x0F66, 0x086A, 0x0963, 0x0A69, 0x0B60,
    0x05F0, 0x04F9, 0x07F3, 0x06FA, 0x01F6, 0x00FF, 0x03F5, 0x02FC,
    0x0DFC, 0x0CF5, 0x0FFF, 0x0EF6, 0x09FA, 0x08F3, 0x0BF9, 0x0AF0,
    0x0650, 0x0759, 0x0453, 0x055A, 0x0256, 0x035F, 0x0055, 0x015C,
    0x0E5C, 0x0F55, 0x0C5F, 0x0D56, 0x0A5A, 0x0B53, 0x0859, 0x0950,
    0x07C0, 0x06C9, 0x05C3, 0x04CA, 0x03C6, 0x02CF, 0x01C5, 0x00CC,
    0x0FCC, 0x0EC5, 0x0DCF, 0x0CC6, 0x0BCA, 0x0AC3, 0x09C9, 0x08C0,
    0x08C0, 0x09C9, 0x0AC3, 0x0BCA, 0x0CC6, 0x0DCF, 0x0EC5, 0x0FCC,
    0x00CC, 0x01C5, 0x02CF, 0x03C6, 0x04CA, 0x05C3, 0x06C9, 0x07C0,
    0x0950, 0x0859, 0x0B53, 0x0A5A, 0x0D56, 0x0C5F, 0x0F55, 0x0E5C,
    0x015C, 0x0055, 0x035F, 0x0256, 0x055A, 0x0453, 0x0759, 0x0650,
    0x0AF0, 0x0BF9, 0x08F3, 0x09FA, 0x0EF6, 0x0FFF, 0x0CF5, 0x0DFC,
    0x02FC, 0x03F5, 0x00FF, 0x01F6, 0x06FA, 0x07F3, 0x04F9, 0x05F0,
    0x0B60, 0x0A69, 0x0963, 0x086A, 0x0F66, 0x0E6F, 0x0D65, 0x0C6C,
    0x036C, 0x0265, 0x016F, 0x0066, 0x076A, 0x0663, 0x0569, 0x0460,
    0x0CA0, 0x0DA9, 0x0EA3, 0x0FAA, 0x08A6, 0x09AF, 0x0AA5, 0x0BAC,
    0x04AC, 0x05A5, 0x06AF, 0x07A6, 0x00AA, 0x01A3, 0x02A9, 0x03A0,
    0x0D30, 0x0C39, 0x0F33, 0x0E3A, 0x0936, 0x083F, 0x0B35, 0x0A3C,
    0x053C, 0x0435, 0x073F, 0x0636, 0x013A, 0x0033, 0x0339, 0x0230,
    0x0E90, 0x0F99, 0x0C93, 0x0D9A, 0x0A96, 0x0B9F, 0x0895, 0x099C,
    0x069C, 0x0795, 0x049F, 0x0596, 0x029A, 0x0393, 0x0099, 0x0190,
    0x0F00, 0x0E09, 0x0D03, 0x0C0A, 0x0B06, 0x0A0F, 0x0905, 0x080C,
    0x070C, 0x0605, 0x050F, 0x0406, 0x030A, 0x0203, 0x0109, 0x0000
  };

  final static byte[][] triangleTable = {
    null,
    {0, 8, 3},
    {0, 1, 9},
    {1, 8, 3, 9, 8, 1},
    {1, 2, 10},
    {0, 8, 3, 1, 2, 10},
    {9, 2, 10, 0, 2, 9},
    {2, 8, 3, 2, 10, 8, 10, 9, 8},
    {3, 11, 2},
    {0, 11, 2, 8, 11, 0},
    {1, 9, 0, 2, 3, 11},
    {1, 11, 2, 1, 9, 11, 9, 8, 11},
    {3, 10, 1, 11, 10, 3},
    {0, 10, 1, 0, 8, 10, 8, 11, 10},
    {3, 9, 0, 3, 11, 9, 11, 10, 9},
    {9, 8, 10, 10, 8, 11},
    {4, 7, 8},
    {4, 3, 0, 7, 3, 4},
    {0, 1, 9, 8, 4, 7},
    {4, 1, 9, 4, 7, 1, 7, 3, 1},
    {1, 2, 10, 8, 4, 7},
    {3, 4, 7, 3, 0, 4, 1, 2, 10},
    {9, 2, 10, 9, 0, 2, 8, 4, 7},
    {2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4},
    {8, 4, 7, 3, 11, 2},
    {11, 4, 7, 11, 2, 4, 2, 0, 4},
    {9, 0, 1, 8, 4, 7, 2, 3, 11},
    {4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1},
    {3, 10, 1, 3, 11, 10, 7, 8, 4},
    {1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4},
    {4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3},
    {4, 7, 11, 4, 11, 9, 9, 11, 10},
    {9, 5, 4},
    {9, 5, 4, 0, 8, 3},
    {0, 5, 4, 1, 5, 0},
    {8, 5, 4, 8, 3, 5, 3, 1, 5},
    {1, 2, 10, 9, 5, 4},
    {3, 0, 8, 1, 2, 10, 4, 9, 5},
    {5, 2, 10, 5, 4, 2, 4, 0, 2},
    {2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8},
    {9, 5, 4, 2, 3, 11},
    {0, 11, 2, 0, 8, 11, 4, 9, 5},
    {0, 5, 4, 0, 1, 5, 2, 3, 11},
    {2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5},
    {10, 3, 11, 10, 1, 3, 9, 5, 4},
    {4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10},
    {5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3},
    {5, 4, 8, 5, 8, 10, 10, 8, 11},
    {9, 7, 8, 5, 7, 9},
    {9, 3, 0, 9, 5, 3, 5, 7, 3},
    {0, 7, 8, 0, 1, 7, 1, 5, 7},
    {1, 5, 3, 3, 5, 7},
    {9, 7, 8, 9, 5, 7, 10, 1, 2},
    {10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3},
    {8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2},
    {2, 10, 5, 2, 5, 3, 3, 5, 7},
    {7, 9, 5, 7, 8, 9, 3, 11, 2},
    {9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11},
    {2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7},
    {11, 2, 1, 11, 1, 7, 7, 1, 5},
    {9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11},
    {5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0},
    {11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0},
    {11, 10, 5, 7, 11, 5},
    {10, 6, 5},
    {0, 8, 3, 5, 10, 6},
    {9, 0, 1, 5, 10, 6},
    {1, 8, 3, 1, 9, 8, 5, 10, 6},
    {1, 6, 5, 2, 6, 1},
    {1, 6, 5, 1, 2, 6, 3, 0, 8},
    {9, 6, 5, 9, 0, 6, 0, 2, 6},
    {5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8},
    {2, 3, 11, 10, 6, 5},
    {11, 0, 8, 11, 2, 0, 10, 6, 5},
    {0, 1, 9, 2, 3, 11, 5, 10, 6},
    {5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11},
    {6, 3, 11, 6, 5, 3, 5, 1, 3},
    {0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6},
    {3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9},
    {6, 5, 9, 6, 9, 11, 11, 9, 8},
    {5, 10, 6, 4, 7, 8},
    {4, 3, 0, 4, 7, 3, 6, 5, 10},
    {1, 9, 0, 5, 10, 6, 8, 4, 7},
    {10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4},
    {6, 1, 2, 6, 5, 1, 4, 7, 8},
    {1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7},
    {8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6},
    {7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9},
    {3, 11, 2, 7, 8, 4, 10, 6, 5},
    {5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11},
    {0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6},
    {9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6},
    {8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6},
    {5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11},
    {0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7},
    {6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9},
    {10, 4, 9, 6, 4, 10},
    {4, 10, 6, 4, 9, 10, 0, 8, 3},
    {10, 0, 1, 10, 6, 0, 6, 4, 0},
    {8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10},
    {1, 4, 9, 1, 2, 4, 2, 6, 4},
    {3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4},
    {0, 2, 4, 4, 2, 6},
    {8, 3, 2, 8, 2, 4, 4, 2, 6},
    {10, 4, 9, 10, 6, 4, 11, 2, 3},
    {0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6},
    {3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10},
    {6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1},
    {9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3},
    {8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1},
    {3, 11, 6, 3, 6, 0, 0, 6, 4},
    {6, 4, 8, 11, 6, 8},
    {7, 10, 6, 7, 8, 10, 8, 9, 10},
    {0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10},
    {10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0},
    {10, 6, 7, 10, 7, 1, 1, 7, 3},
    {1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7},
    {2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9},
    {7, 8, 0, 7, 0, 6, 6, 0, 2},
    {7, 3, 2, 6, 7, 2},
    {2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7},
    {2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7},
    {1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11},
    {11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1},
    {8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6},
    {0, 9, 1, 11, 6, 7},
    {7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0},
    {7, 11, 6},
    {7, 6, 11},
    {3, 0, 8, 11, 7, 6},
    {0, 1, 9, 11, 7, 6},
    {8, 1, 9, 8, 3, 1, 11, 7, 6},
    {10, 1, 2, 6, 11, 7},
    {1, 2, 10, 3, 0, 8, 6, 11, 7},
    {2, 9, 0, 2, 10, 9, 6, 11, 7},
    {6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8},
    {7, 2, 3, 6, 2, 7},
    {7, 0, 8, 7, 6, 0, 6, 2, 0},
    {2, 7, 6, 2, 3, 7, 0, 1, 9},
    {1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6},
    {10, 7, 6, 10, 1, 7, 1, 3, 7},
    {10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8},
    {0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7},
    {7, 6, 10, 7, 10, 8, 8, 10, 9},
    {6, 8, 4, 11, 8, 6},
    {3, 6, 11, 3, 0, 6, 0, 4, 6},
    {8, 6, 11, 8, 4, 6, 9, 0, 1},
    {9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6},
    {6, 8, 4, 6, 11, 8, 2, 10, 1},
    {1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6},
    {4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9},
    {10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3},
    {8, 2, 3, 8, 4, 2, 4, 6, 2},
    {0, 4, 2, 4, 6, 2},
    {1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8},
    {1, 9, 4, 1, 4, 2, 2, 4, 6},
    {8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1},
    {10, 1, 0, 10, 0, 6, 6, 0, 4},
    {4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3},
    {10, 9, 4, 6, 10, 4},
    {4, 9, 5, 7, 6, 11},
    {0, 8, 3, 4, 9, 5, 11, 7, 6},
    {5, 0, 1, 5, 4, 0, 7, 6, 11},
    {11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5},
    {9, 5, 4, 10, 1, 2, 7, 6, 11},
    {6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5},
    {7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2},
    {3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6},
    {7, 2, 3, 7, 6, 2, 5, 4, 9},
    {9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7},
    {3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0},
    {6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8},
    {9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7},
    {1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4},
    {4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10},
    {7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10},
    {6, 9, 5, 6, 11, 9, 11, 8, 9},
    {3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5},
    {0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11},
    {6, 11, 3, 6, 3, 5, 5, 3, 1},
    {1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6},
    {0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10},
    {11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5},
    {6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3},
    {5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2},
    {9, 5, 6, 9, 6, 0, 0, 6, 2},
    {1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8},
    {1, 5, 6, 2, 1, 6},
    {1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6},
    {10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0},
    {0, 3, 8, 5, 6, 10},
    {10, 5, 6},
    {11, 5, 10, 7, 5, 11},
    {11, 5, 10, 11, 7, 5, 8, 3, 0},
    {5, 11, 7, 5, 10, 11, 1, 9, 0},
    {10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1},
    {11, 1, 2, 11, 7, 1, 7, 5, 1},
    {0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11},
    {9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7},
    {7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2},
    {2, 5, 10, 2, 3, 5, 3, 7, 5},
    {8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5},
    {9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2},
    {9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2},
    {1, 3, 5, 3, 7, 5},
    {0, 8, 7, 0, 7, 1, 1, 7, 5},
    {9, 0, 3, 9, 3, 5, 5, 3, 7},
    {9, 8, 7, 5, 9, 7},
    {5, 8, 4, 5, 10, 8, 10, 11, 8},
    {5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0},
    {0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5},
    {10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4},
    {2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8},
    {0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11},
    {0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5},
    {9, 4, 5, 2, 11, 3},
    {2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4},
    {5, 10, 2, 5, 2, 4, 4, 2, 0},
    {3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9},
    {5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2},
    {8, 4, 5, 8, 5, 3, 3, 5, 1},
    {0, 4, 5, 1, 0, 5},
    {8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5},
    {9, 4, 5},
    {4, 11, 7, 4, 9, 11, 9, 10, 11},
    {0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11},
    {1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11},
    {3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4},
    {4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2},
    {9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3},
    {11, 7, 4, 11, 4, 2, 2, 4, 0},
    {11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4},
    {2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9},
    {9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7},
    {3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10},
    {1, 10, 2, 8, 7, 4},
    {4, 9, 1, 4, 1, 7, 7, 1, 3},
    {4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1},
    {4, 0, 3, 7, 4, 3},
    {4, 8, 7},
    {9, 10, 8, 10, 11, 8},
    {3, 0, 9, 3, 9, 11, 11, 9, 10},
    {0, 1, 10, 0, 10, 8, 8, 10, 11},
    {3, 1, 10, 11, 3, 10},
    {1, 2, 11, 1, 11, 9, 9, 11, 8},
    {3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9},
    {0, 2, 11, 8, 0, 11},
    {3, 2, 11},
    {2, 3, 8, 2, 8, 10, 10, 8, 9},
    {9, 10, 2, 0, 9, 2},
    {2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8},
    {1, 10, 2},
    {1, 3, 8, 9, 1, 8},
    {0, 9, 1},
    {0, 3, 8},
    null
  };

  ////////////////////////////////////////////////////////////////
  // color scale stuff
  ////////////////////////////////////////////////////////////////

  void applyColorScale(float min, float max, String scaleName) {
    if (currentMesh != null)
      applyColorScale(currentMesh, min, max, scaleName);
    else {
      for (int i = meshCount; --i >= 0; )
        applyColorScale(meshes[i], min, max, scaleName);
    }
  }

  float getMinMappedValue() {
    if (currentMesh != null)
      return getMinMappedValue(currentMesh);
    float min = Float.MAX_VALUE;
    for (int i = meshCount; --i >= 0; ) {
      float challenger = getMinMappedValue(meshes[i]);
      if (challenger < min)
        min = challenger;
    }
    return min;
  }

  float getMaxMappedValue() {
    if (currentMesh != null)
      return getMaxMappedValue(currentMesh);
    float max = Float.MIN_VALUE;
    for (int i = meshCount; --i >= 0; ) {
      float challenger = getMaxMappedValue(meshes[i]);
      if (challenger > max)
        max = challenger;
    }
    return max;
  }

  void applyColorScale(Mesh mesh, float min, float max, String scaleName) {
    int vertexCount = mesh.vertexCount;
    Point3f[] vertexes = mesh.vertices;
    short[] colixes = mesh.vertexColixes;
    if (colixes == null)
      mesh.vertexColixes = colixes = new short[vertexCount];
    for (int i = vertexCount; --i >= 0; ) {
      float value = lookupInterpolatedVoxelValue(vertexes[i]);
      colixes[i] = viewer.getColixFromPalette(value, min, max, scaleName);
    }
  }

  float getMinMappedValue(Mesh mesh) {
    int vertexCount = mesh.vertexCount;
    Point3f[] vertexes = mesh.vertices;
    float min = Float.MAX_VALUE;
    for (int i = vertexCount; --i >= 0; ) {
      float challenger = lookupInterpolatedVoxelValue(vertexes[i]);
      if (challenger < min)
        min = challenger;
    }
    return min;
  }

  float getMaxMappedValue(Mesh mesh) {
    int vertexCount = mesh.vertexCount;
    Point3f[] vertexes = mesh.vertices;
    float max = Float.MIN_VALUE;
    for (int i = vertexCount; --i >= 0; ) {
      float challenger = lookupInterpolatedVoxelValue(vertexes[i]);
      if (challenger > max)
        max = challenger;
    }
    return max;
  }

  int i;
  final Vector3f pointVector = new Vector3f();
  final Point3f cubeLocation = new Point3f();
  float lookupInterpolatedVoxelValue(Point3f point) {
    pointVector.sub(point, volumetricOrigin);
    float x = scaleByVoxelVector(pointVector, 0);
    float y = scaleByVoxelVector(pointVector, 1);
    float z = scaleByVoxelVector(pointVector, 2);
    return getInterpolatedValue(x, y, z);
  }

  float scaleByVoxelVector(Vector3f vector, int voxelVectorIndex) {
    return (vector.dot(unitVolumetricVectors[voxelVectorIndex]) /
            volumetricVectorLengths[voxelVectorIndex]);
  }

  int indexDown(float value, int voxelVectorIndex) {
    if (value < 0)
      return 0;
    int floor = (int) value;
    float delta = value - floor;
    if (delta > 0.9f)
      ++floor;
    int lastValue = voxelCounts[voxelVectorIndex] - 1;
    if (floor > lastValue)
      floor = lastValue;
    return floor;
  }

  int indexUp(float value, int voxelVectorIndex) {
    if (value < 0)
      return 0;
    int ceil = ((int) value) + 1;
    float delta = ceil - value;
    if (delta > 0.9f)
      --ceil;
    return ceil;
  }

  float getInterpolatedValue(float x, float y, float z) {
    int xDown = indexDown(x, 0);
    int xUp = indexUp(x, 0);
    int yDown = indexDown(x, 1);
    int yUp = indexUp(x, 1);
    int zDown = indexDown(x, 2);
    int zUp = indexUp(x, 2);

    float valueDown = voxelData[xDown][yDown][zDown];
    float valueUp = voxelData[xUp][yUp][zUp];
    float valueDelta = valueUp - valueDown;
    float delta;
    int differentMask;
    differentMask = ((((xUp == xDown) ? 0 : 1) << 0) |
                     (((yUp == yDown) ? 0 : 1) << 1) |
                     (((zUp == zDown) ? 0 : 1) << 2));
    switch (differentMask) {
    case 0:
      return valueDown;
    case 1:
      delta = x - xDown;
      break;
    case 2:
      delta = y - yDown;
      break;
    case 4:
      delta = z - zDown;
      break;
    default:
      // I don't feel like dealing with all the cases
      // just stick it in the middle
      delta = 0.5f;
    }
    return valueDown + delta * valueDelta;
  }
}
