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
package org.jmol.jvxl.calc;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.jvxl.api.VertexDataServer;
import org.jmol.jvxl.data.VolumeData;

public class MarchingCubes {

  /*
   * An adaptation of Marching Cubes to include data slicing and the option
   * for progressive reading of the data. Associated VoxelReader and VoxelData
   * structures are required to store the sequential values in the case of a plane
   * and to deliver the sequential vertex numbers in any case.
   * 
   * Author: Bob Hanson, hansonr@stolaf.edu
   *  
   */

  private VertexDataServer voxelReader;
  private VolumeData volumeData;
  private int contourType;
  private boolean isContoured;
  private float cutoff;
  private boolean isCutoffAbsolute;

  private int cubeCountX, cubeCountY, cubeCountZ;

  public MarchingCubes(VertexDataServer voxelReader, VolumeData volumeData,
      boolean isContoured, int contourType, float cutoff,
      boolean isCutoffAbsolute) {
    this.voxelReader = voxelReader;
    this.volumeData = volumeData;
    this.isContoured = isContoured;
    this.cutoff = cutoff;
    this.isCutoffAbsolute = isCutoffAbsolute;
    this.contourType = contourType;

    cubeCountX = volumeData.voxelData.length - 1;
    cubeCountY = volumeData.voxelData[0].length - 1;
    cubeCountZ = volumeData.voxelData[0][0].length - 1;
    calcVoxelVertexVectors();
  }

  public final Vector3f[] voxelVertexVectors = new Vector3f[8];
  private final float[] vertexValues = new float[8];
  private final Point3i[] vertexPoints = new Point3i[8];
  private final Vector3f[] edgeVectors = new Vector3f[12];
  {
    for (int i = 12; --i >= 0;)
      edgeVectors[i] = new Vector3f();
    for (int i = 8; --i >= 0;)
      vertexPoints[i] = new Point3i();
  }
  private final int[] linearOffsets = new int[8];
  int xyCount;

  boolean isXLowToHigh;
  int edgeCount;


  private void calcVoxelVertexVectors() {
    xyCount = (cubeCountX + 1) * (cubeCountY + 1);
    setLinearOffsets(); //only used for associating normals 
    volumeData.setMatrix();
    for (int i = 8; --i >= 0;)
      volumeData.transform(cubeVertexVectors[i],
          voxelVertexVectors[i] = new Vector3f());
    for (int i = 12; --i >= 0;)
      edgeVectors[i].sub(voxelVertexVectors[edgeVertexes[i + i + 1]],
          voxelVertexVectors[edgeVertexes[i + i]]);
  }

  public int generateSurfaceData(boolean isXLowToHigh) {

    this.isXLowToHigh = isXLowToHigh;

    int[][] isoPointIndexes = new int[cubeCountY * cubeCountZ][12];

    int insideCount = 0, outsideCount = 0, surfaceCount = 0;
    edgeCount = 0;

    int x0, x1, xStep;
    if (isXLowToHigh) {
      x0 = 0;
      x1 = cubeCountX;
      xStep = 1;
    } else {
      x0 = cubeCountX - 1;
      x1 = -1;
      xStep = -1;
    }
    for (int x = x0; x != x1; x += xStep) {
      for (int y = cubeCountY; --y >= 0;) {
        for (int z = cubeCountZ; --z >= 0;) {
          int[] voxelPointIndexes = propagateNeighborPointIndexes(x, y, z,
              isoPointIndexes);
          int insideMask = 0;
          for (int i = 8; --i >= 0;) {
            Point3i offset = cubeVertexOffsets[i];
            if (isInside(
                (vertexValues[i] = volumeData.voxelData[x + offset.x][y
                    + offset.y][z + offset.z]), cutoff, isCutoffAbsolute))
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
          if (!processOneCubical(insideMask, voxelPointIndexes, x, y, z)
              || isContoured)
            continue;

          byte[] triangles = triangleTable2[insideMask];
          for (int i = triangles.length; (i -= 4) >= 0;)
            voxelReader.addTriangleCheck(voxelPointIndexes[triangles[i]],
                voxelPointIndexes[triangles[i + 1]],
                voxelPointIndexes[triangles[i + 2]], triangles[i + 3], isCutoffAbsolute);
        }
      }
    }
    return edgeCount;
  }
  
  public static boolean isInside(float voxelValue, float max, boolean isAbsolute) {
    return ((max > 0 && (isAbsolute ? Math.abs(voxelValue) : voxelValue) >= max) || (max <= 0 && voxelValue <= max));
  }

  /* set the linear offsets for unique cell ID. Add offset to 0: z * (nX * nY) + y * nX + x */
  void setLinearOffsets() {
    linearOffsets[0] = 0;
    linearOffsets[1] = 1;
    linearOffsets[5] = 1 + (linearOffsets[4] = (cubeCountX + 1));
    linearOffsets[6] = linearOffsets[5] + xyCount;
    linearOffsets[7] = linearOffsets[4] + xyCount;
    linearOffsets[3] = linearOffsets[0] + xyCount;
    linearOffsets[2] = linearOffsets[1] + xyCount;
  }
  
  public int getLinearOffset(int x, int y, int z, int offset) {
    return z * (xyCount) + y * cubeCountX + y + x + linearOffsets[offset];
  }
  
  private final int[] nullNeighbor = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1 };

  private int[] propagateNeighborPointIndexes(int x, int y, int z,
                                              int[][] isoPointIndexes) {
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
     * We are running through the grid points in yz planes from high x --> low x
     * and within those planes along strips from high y to low y
     * and within those strips, from high z to low z. The "leading vertex" is 0, 
     * and the "leading edges" are {0,3,8}. 
     * 
     * For each such cube, edges are traversed from high to low (11-->0)
     * 
     * Each edge has the potential to be "critical" and cross the surface.
     * Setting -1 in voxelPointIndexes indicates that this edge needs checking.
     * Otherwise, the crossing point for this edge is taken from the value
     * already determined, because it has already been determined to be critical. 
     *
     * The above model, because it starts at HIGH x, requires that all x,y,z points 
     * be in memory from the beginning. We could have instead used a progressive 
     * streaming model, where we only pull in the slice of data that we need. In 
     * that case, each edge corresponds to a specific pair of indices in our slice.
     * 
     * Say we have a 51 x 11 x 21 block of data. This represents a 50 x 10 x 20 set
     * of cubes. If, instead of reading all the data, we pull in just the first two
     * "slices" x=0(10x20), x=1(10x20), that is just 400 points. Once a slice of
     * data is used, we can flush it -- it is never used again. 
     * 
     * When color mapping, we can do the same thing; we just have to put the verticies
     * into bins based on which pair of slices will be relevant, and then make sure we
     * process the verticies based on these bins. 
     * 
     * The JVXL format depends on a specific order of reading of the edge data. The
     * progressive model completely messes this up. The vertices will be read in the 
     * same order around the cube, but the "leading edges" will be {0,1,9}, not {0,3,8}. 
     * We do know which edge is which, so we could construct a progressive model from
     * a nonprogressive one, if necessary. 
     * 
     * All we are really talking about is the JVXL reader, because we can certainly
     * switch to progressive mode in all the other readers.  
     *  
     *  
     */
    int cellIndex = y * cubeCountZ + z;
    int[] voxelPointIndexes = isoPointIndexes[cellIndex];

    boolean noXNeighbor = (x == cubeCountX - 1);
    if (isXLowToHigh) {
      // reading x from low to high
      if (noXNeighbor) {
        voxelPointIndexes[3] = -1;
        voxelPointIndexes[8] = -1;
        voxelPointIndexes[7] = -1;
        voxelPointIndexes[11] = -1;
      } else {
        voxelPointIndexes[3] = voxelPointIndexes[1];
        voxelPointIndexes[7] = voxelPointIndexes[5];
        voxelPointIndexes[8] = voxelPointIndexes[9];
        voxelPointIndexes[11] = voxelPointIndexes[10];
      }
    } else {
      // reading x from high to low
      if (noXNeighbor) {
        // the x neighbor is myself from my last pass through here
        voxelPointIndexes[1] = -1;
        voxelPointIndexes[9] = -1;
        voxelPointIndexes[5] = -1;
        voxelPointIndexes[10] = -1;
      } else {
        voxelPointIndexes[1] = voxelPointIndexes[3];
        voxelPointIndexes[5] = voxelPointIndexes[7];
        voxelPointIndexes[9] = voxelPointIndexes[8];
        voxelPointIndexes[10] = voxelPointIndexes[11];
      }
    }
    //from the y neighbor pick up the top
    boolean noYNeighbor = (y == cubeCountY - 1);
    int[] yNeighbor = noYNeighbor ? nullNeighbor : isoPointIndexes[cellIndex
        + cubeCountZ];

    voxelPointIndexes[4] = yNeighbor[0];
    voxelPointIndexes[6] = yNeighbor[2];

    if (isXLowToHigh) {
      voxelPointIndexes[5] = yNeighbor[1];
      if (noXNeighbor)
        voxelPointIndexes[7] = yNeighbor[3];
    } else {
      voxelPointIndexes[7] = yNeighbor[3];
      if (noXNeighbor)
        voxelPointIndexes[5] = yNeighbor[1];
    }
    // from my z neighbor
    boolean noZNeighbor = (z == cubeCountZ - 1);
    int[] zNeighbor = noZNeighbor ? nullNeighbor
        : isoPointIndexes[cellIndex + 1];

    voxelPointIndexes[2] = zNeighbor[0];
    if (noYNeighbor)
      voxelPointIndexes[6] = zNeighbor[4];
    if (isXLowToHigh) {
      if (noXNeighbor)
        voxelPointIndexes[11] = zNeighbor[8];
      voxelPointIndexes[10] = zNeighbor[9];
    } else {
      if (noXNeighbor)
        voxelPointIndexes[10] = zNeighbor[9];
      voxelPointIndexes[11] = zNeighbor[8];
    }
    // these must always be calculated
    voxelPointIndexes[0] = -1;
    if (isXLowToHigh) {
      voxelPointIndexes[1] = -1;
      voxelPointIndexes[9] = -1;
    } else {
      voxelPointIndexes[3] = -1;
      voxelPointIndexes[8] = -1;
    }
    return voxelPointIndexes;
  }

  private final Point3f pt0 = new Point3f();
  private final Point3f pointA = new Point3f();
  
  private boolean processOneCubical(int insideMask, int[] voxelPointIndexes,
                                    int x, int y, int z) {
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
      volumeData.voxelPtToXYZ(x, y, z, pt0);
      pointA.add(pt0, voxelVertexVectors[vertexA]);
      voxelPointIndexes[iEdge] = voxelReader.getSurfacePointIndex(
          cutoff, isCutoffAbsolute, x, y, z, cubeVertexOffsets[vertexA], 
          vertexA, vertexB, valueA, valueB, pointA, edgeVectors[iEdge], 
          edgeTypeTable[iEdge] == contourType);
    }
    return !isNaN;
  }

  public void calcVertexPoint(int x, int y, int z, int vertex, Point3f pt) {
    volumeData.voxelPtToXYZ(x, y, z, pt0);
    pt.add(pt0, voxelVertexVectors[vertex]);
  }

  final static Point3i[] cubeVertexOffsets = { new Point3i(0, 0, 0),
      new Point3i(1, 0, 0), new Point3i(1, 0, 1), new Point3i(0, 0, 1),
      new Point3i(0, 1, 0), new Point3i(1, 1, 0), new Point3i(1, 1, 1),
      new Point3i(0, 1, 1) };

  static Vector3f[] cubeVertexVectors = { new Vector3f(0, 0, 0),
      new Vector3f(1, 0, 0), new Vector3f(1, 0, 1), new Vector3f(0, 0, 1),
      new Vector3f(0, 1, 0), new Vector3f(1, 1, 0), new Vector3f(1, 1, 1),
      new Vector3f(0, 1, 1) };
  
  

  /*                     Y 
   *                      4 --------4--------- 5                     +z --------4--------- +yz+z                  
   *                     /|                   /|                     /|                   /|
   *                    / |                  / |                    / |                  / |
   *                   /  |                 /  |                   /  |                 /  |
   *                  7   8                5   |                  7   8                5   |
   *                 /    |               /    9                 /    |               /    9
   *                /     |              /     |                /     |              /     |
   *               7 --------6--------- 6      |            +z+1 --------6--------- +yz+z+1|
   *               |      |             |      |               |      |             |      |
   *               |      0 ---------0--|----- 1    X          |      0 ---------0--|----- +yz    X(outer)    
   *               |     /              |     /                |     /              |     /
   *              11    /               10   /                11    /               10   /
   *               |   3                |   1                  |   3                |   1
   *               |  /                 |  /                   |  /                 |  /
   *               | /                  | /                    | /                  | /
   *               3 ---------2-------- 2                     +1 ---------2-------- +yz+1
   *              Z                                           Z (inner)
   * 
   *                                                              streaming data offsets
   * type 0: x-edges: 0 2 4 6
   * type 1: y-edges: 8 9 10 11
   * type 2: z-edges: 1 3 5 7
   * 
   * Data stream offsets for vertices, relative to point 0, based on reading 
   * loops {for x {for y {for z}}} 0-->n-1
   * y and z are numbers of grid points in those directions:
   * 
   *            0    1      2      3      4      5      6        7
   *            0   +yz   +yz+1   +1     +z    +yz+z  +yz+z+1  +z+1     
   * 
   * These are just looked up in a table. After the first set of cubes, 
   * we are only adding points 1, 2, 5 or 6. This means that initially
   * we need two data slices, but after that only one (slice 1):
   * 
   *            base
   *           offset 0    1      2      3      4      5      6     7
   *  slice[0]        0                 +1     +z                 +z+1     
   *  slice[1]  +yz        0     +1                   +z    +z+1      
   * 
   *  slice:          0    1      1      0      0      1      1     0
   *  
   *  We can request reading of two slices (2*nY*nZ data points) first, then
   *  from then on, just nY*nZ points. "Reading" is really just being handed a 
   *  pointer into an array. Perhaps that array is already filled completely;
   *  perhaps it is being read incrementally. 
   *  
   *  As it is now, the JVXL data are just read into an [nX][nY][nZ] array anyway, 
   *  so we can continue to do that with NON progressive files. 
   */

  private final static int edgeTypeTable[] = { 0, 2, 0, 2, 0, 2, 0, 2, 1, 1, 1,
      1 };

  private final static byte edgeVertexes[] = { 0, 1, 1, 2, 2, 3, 3, 0, 4, 5,
  /*   0       1       2       3       4  */
  5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7 };
  /*  5       6       7       8       9       10      11     */

  private final static short insideMaskTable[] = { 0x0000, 0x0109, 0x0203,
      0x030A, 0x0406, 0x050F, 0x0605, 0x070C, 0x080C, 0x0905, 0x0A0F, 0x0B06,
      0x0C0A, 0x0D03, 0x0E09, 0x0F00, 0x0190, 0x0099, 0x0393, 0x029A, 0x0596,
      0x049F, 0x0795, 0x069C, 0x099C, 0x0895, 0x0B9F, 0x0A96, 0x0D9A, 0x0C93,
      0x0F99, 0x0E90, 0x0230, 0x0339, 0x0033, 0x013A, 0x0636, 0x073F, 0x0435,
      0x053C, 0x0A3C, 0x0B35, 0x083F, 0x0936, 0x0E3A, 0x0F33, 0x0C39, 0x0D30,
      0x03A0, 0x02A9, 0x01A3, 0x00AA, 0x07A6, 0x06AF, 0x05A5, 0x04AC, 0x0BAC,
      0x0AA5, 0x09AF, 0x08A6, 0x0FAA, 0x0EA3, 0x0DA9, 0x0CA0, 0x0460, 0x0569,
      0x0663, 0x076A, 0x0066, 0x016F, 0x0265, 0x036C, 0x0C6C, 0x0D65, 0x0E6F,
      0x0F66, 0x086A, 0x0963, 0x0A69, 0x0B60, 0x05F0, 0x04F9, 0x07F3, 0x06FA,
      0x01F6, 0x00FF, 0x03F5, 0x02FC, 0x0DFC, 0x0CF5, 0x0FFF, 0x0EF6, 0x09FA,
      0x08F3, 0x0BF9, 0x0AF0, 0x0650, 0x0759, 0x0453, 0x055A, 0x0256, 0x035F,
      0x0055, 0x015C, 0x0E5C, 0x0F55, 0x0C5F, 0x0D56, 0x0A5A, 0x0B53, 0x0859,
      0x0950, 0x07C0, 0x06C9, 0x05C3, 0x04CA, 0x03C6, 0x02CF, 0x01C5, 0x00CC,
      0x0FCC, 0x0EC5, 0x0DCF, 0x0CC6, 0x0BCA, 0x0AC3, 0x09C9, 0x08C0, 0x08C0,
      0x09C9, 0x0AC3, 0x0BCA, 0x0CC6, 0x0DCF, 0x0EC5, 0x0FCC, 0x00CC, 0x01C5,
      0x02CF, 0x03C6, 0x04CA, 0x05C3, 0x06C9, 0x07C0, 0x0950, 0x0859, 0x0B53,
      0x0A5A, 0x0D56, 0x0C5F, 0x0F55, 0x0E5C, 0x015C, 0x0055, 0x035F, 0x0256,
      0x055A, 0x0453, 0x0759, 0x0650, 0x0AF0, 0x0BF9, 0x08F3, 0x09FA, 0x0EF6,
      0x0FFF, 0x0CF5, 0x0DFC, 0x02FC, 0x03F5, 0x00FF, 0x01F6, 0x06FA, 0x07F3,
      0x04F9, 0x05F0, 0x0B60, 0x0A69, 0x0963, 0x086A, 0x0F66, 0x0E6F, 0x0D65,
      0x0C6C, 0x036C, 0x0265, 0x016F, 0x0066, 0x076A, 0x0663, 0x0569, 0x0460,
      0x0CA0, 0x0DA9, 0x0EA3, 0x0FAA, 0x08A6, 0x09AF, 0x0AA5, 0x0BAC, 0x04AC,
      0x05A5, 0x06AF, 0x07A6, 0x00AA, 0x01A3, 0x02A9, 0x03A0, 0x0D30, 0x0C39,
      0x0F33, 0x0E3A, 0x0936, 0x083F, 0x0B35, 0x0A3C, 0x053C, 0x0435, 0x073F,
      0x0636, 0x013A, 0x0033, 0x0339, 0x0230, 0x0E90, 0x0F99, 0x0C93, 0x0D9A,
      0x0A96, 0x0B9F, 0x0895, 0x099C, 0x069C, 0x0795, 0x049F, 0x0596, 0x029A,
      0x0393, 0x0099, 0x0190, 0x0F00, 0x0E09, 0x0D03, 0x0C0A, 0x0B06, 0x0A0F,
      0x0905, 0x080C, 0x070C, 0x0605, 0x050F, 0x0406, 0x030A, 0x0203, 0x0109,
      0x0000 };

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

  private final static byte[][] triangleTable2 = { null, { 0, 8, 3, 7 },
      { 0, 1, 9, 7 }, { 1, 8, 3, 6, 9, 8, 1, 5 }, { 1, 2, 10, 7 },
      { 0, 8, 3, 7, 1, 2, 10, 7 }, { 9, 2, 10, 6, 0, 2, 9, 5 },
      { 2, 8, 3, 6, 2, 10, 8, 1, 10, 9, 8, 3 }, { 3, 11, 2, 7 },
      { 0, 11, 2, 6, 8, 11, 0, 5 }, { 1, 9, 0, 7, 2, 3, 11, 7 },
      { 1, 11, 2, 6, 1, 9, 11, 1, 9, 8, 11, 3 }, { 3, 10, 1, 6, 11, 10, 3, 5 },
      { 0, 10, 1, 6, 0, 8, 10, 1, 8, 11, 10, 3 },
      { 3, 9, 0, 6, 3, 11, 9, 1, 11, 10, 9, 3 }, { 9, 8, 10, 5, 10, 8, 11, 6 },
      { 4, 7, 8, 7 }, { 4, 3, 0, 6, 7, 3, 4, 5 }, { 0, 1, 9, 7, 8, 4, 7, 7 },
      { 4, 1, 9, 6, 4, 7, 1, 1, 7, 3, 1, 3 }, { 1, 2, 10, 7, 8, 4, 7, 7 },
      { 3, 4, 7, 6, 3, 0, 4, 3, 1, 2, 10, 7 },
      { 9, 2, 10, 6, 9, 0, 2, 3, 8, 4, 7, 7 },
      { 2, 10, 9, 3, 2, 9, 7, 0, 2, 7, 3, 6, 7, 9, 4, 6 },
      { 8, 4, 7, 7, 3, 11, 2, 7 }, { 11, 4, 7, 6, 11, 2, 4, 1, 2, 0, 4, 3 },
      { 9, 0, 1, 7, 8, 4, 7, 7, 2, 3, 11, 7 },
      { 4, 7, 11, 3, 9, 4, 11, 1, 9, 11, 2, 2, 9, 2, 1, 6 },
      { 3, 10, 1, 6, 3, 11, 10, 3, 7, 8, 4, 7 },
      { 1, 11, 10, 6, 1, 4, 11, 0, 1, 0, 4, 3, 7, 11, 4, 5 },
      { 4, 7, 8, 7, 9, 0, 11, 1, 9, 11, 10, 6, 11, 0, 3, 6 },
      { 4, 7, 11, 3, 4, 11, 9, 4, 9, 11, 10, 6 }, { 9, 5, 4, 7 },
      { 9, 5, 4, 7, 0, 8, 3, 7 }, { 0, 5, 4, 6, 1, 5, 0, 5 },
      { 8, 5, 4, 6, 8, 3, 5, 1, 3, 1, 5, 3 }, { 1, 2, 10, 7, 9, 5, 4, 7 },
      { 3, 0, 8, 7, 1, 2, 10, 7, 4, 9, 5, 7 },
      { 5, 2, 10, 6, 5, 4, 2, 1, 4, 0, 2, 3 },
      { 2, 10, 5, 3, 3, 2, 5, 1, 3, 5, 4, 2, 3, 4, 8, 6 },
      { 9, 5, 4, 7, 2, 3, 11, 7 }, { 0, 11, 2, 6, 0, 8, 11, 3, 4, 9, 5, 7 },
      { 0, 5, 4, 6, 0, 1, 5, 3, 2, 3, 11, 7 },
      { 2, 1, 5, 3, 2, 5, 8, 0, 2, 8, 11, 6, 4, 8, 5, 5 },
      { 10, 3, 11, 6, 10, 1, 3, 3, 9, 5, 4, 7 },
      { 4, 9, 5, 7, 0, 8, 1, 5, 8, 10, 1, 2, 8, 11, 10, 3 },
      { 5, 4, 0, 3, 5, 0, 11, 0, 5, 11, 10, 6, 11, 0, 3, 6 },
      { 5, 4, 8, 3, 5, 8, 10, 4, 10, 8, 11, 6 }, { 9, 7, 8, 6, 5, 7, 9, 5 },
      { 9, 3, 0, 6, 9, 5, 3, 1, 5, 7, 3, 3 },
      { 0, 7, 8, 6, 0, 1, 7, 1, 1, 5, 7, 3 }, { 1, 5, 3, 5, 3, 5, 7, 6 },
      { 9, 7, 8, 6, 9, 5, 7, 3, 10, 1, 2, 7 },
      { 10, 1, 2, 7, 9, 5, 0, 5, 5, 3, 0, 2, 5, 7, 3, 3 },
      { 8, 0, 2, 3, 8, 2, 5, 0, 8, 5, 7, 6, 10, 5, 2, 5 },
      { 2, 10, 5, 3, 2, 5, 3, 4, 3, 5, 7, 6 },
      { 7, 9, 5, 6, 7, 8, 9, 3, 3, 11, 2, 7 },
      { 9, 5, 7, 3, 9, 7, 2, 0, 9, 2, 0, 6, 2, 7, 11, 6 },
      { 2, 3, 11, 7, 0, 1, 8, 5, 1, 7, 8, 2, 1, 5, 7, 3 },
      { 11, 2, 1, 3, 11, 1, 7, 4, 7, 1, 5, 6 },
      { 9, 5, 8, 5, 8, 5, 7, 6, 10, 1, 3, 3, 10, 3, 11, 6 },
      { 5, 7, 0, 1, 5, 0, 9, 6, 7, 11, 0, 1, 1, 0, 10, 5, 11, 10, 0, 1 },
      { 11, 10, 0, 1, 11, 0, 3, 6, 10, 5, 0, 1, 8, 0, 7, 5, 5, 7, 0, 1 },
      { 11, 10, 5, 3, 7, 11, 5, 5 }, { 10, 6, 5, 7 },
      { 0, 8, 3, 7, 5, 10, 6, 7 }, { 9, 0, 1, 7, 5, 10, 6, 7 },
      { 1, 8, 3, 6, 1, 9, 8, 3, 5, 10, 6, 7 }, { 1, 6, 5, 6, 2, 6, 1, 5 },
      { 1, 6, 5, 6, 1, 2, 6, 3, 3, 0, 8, 7 },
      { 9, 6, 5, 6, 9, 0, 6, 1, 0, 2, 6, 3 },
      { 5, 9, 8, 3, 5, 8, 2, 0, 5, 2, 6, 6, 3, 2, 8, 5 },
      { 2, 3, 11, 7, 10, 6, 5, 7 }, { 11, 0, 8, 6, 11, 2, 0, 3, 10, 6, 5, 7 },
      { 0, 1, 9, 7, 2, 3, 11, 7, 5, 10, 6, 7 },
      { 5, 10, 6, 7, 1, 9, 2, 5, 9, 11, 2, 2, 9, 8, 11, 3 },
      { 6, 3, 11, 6, 6, 5, 3, 1, 5, 1, 3, 3 },
      { 0, 8, 11, 3, 0, 11, 5, 0, 0, 5, 1, 6, 5, 11, 6, 6 },
      { 3, 11, 6, 3, 0, 3, 6, 1, 0, 6, 5, 2, 0, 5, 9, 6 },
      { 6, 5, 9, 3, 6, 9, 11, 4, 11, 9, 8, 6 }, { 5, 10, 6, 7, 4, 7, 8, 7 },
      { 4, 3, 0, 6, 4, 7, 3, 3, 6, 5, 10, 7 },
      { 1, 9, 0, 7, 5, 10, 6, 7, 8, 4, 7, 7 },
      { 10, 6, 5, 7, 1, 9, 7, 1, 1, 7, 3, 6, 7, 9, 4, 6 },
      { 6, 1, 2, 6, 6, 5, 1, 3, 4, 7, 8, 7 },
      { 1, 2, 5, 5, 5, 2, 6, 6, 3, 0, 4, 3, 3, 4, 7, 6 },
      { 8, 4, 7, 7, 9, 0, 5, 5, 0, 6, 5, 2, 0, 2, 6, 3 },
      { 7, 3, 9, 1, 7, 9, 4, 6, 3, 2, 9, 1, 5, 9, 6, 5, 2, 6, 9, 1 },
      { 3, 11, 2, 7, 7, 8, 4, 7, 10, 6, 5, 7 },
      { 5, 10, 6, 7, 4, 7, 2, 1, 4, 2, 0, 6, 2, 7, 11, 6 },
      { 0, 1, 9, 7, 4, 7, 8, 7, 2, 3, 11, 7, 5, 10, 6, 7 },
      { 9, 2, 1, 6, 9, 11, 2, 2, 9, 4, 11, 1, 7, 11, 4, 5, 5, 10, 6, 7 },
      { 8, 4, 7, 7, 3, 11, 5, 1, 3, 5, 1, 6, 5, 11, 6, 6 },
      { 5, 1, 11, 1, 5, 11, 6, 6, 1, 0, 11, 1, 7, 11, 4, 5, 0, 4, 11, 1 },
      { 0, 5, 9, 6, 0, 6, 5, 2, 0, 3, 6, 1, 11, 6, 3, 5, 8, 4, 7, 7 },
      { 6, 5, 9, 3, 6, 9, 11, 4, 4, 7, 9, 5, 7, 11, 9, 1 },
      { 10, 4, 9, 6, 6, 4, 10, 5 }, { 4, 10, 6, 6, 4, 9, 10, 3, 0, 8, 3, 7 },
      { 10, 0, 1, 6, 10, 6, 0, 1, 6, 4, 0, 3 },
      { 8, 3, 1, 3, 8, 1, 6, 0, 8, 6, 4, 6, 6, 1, 10, 6 },
      { 1, 4, 9, 6, 1, 2, 4, 1, 2, 6, 4, 3 },
      { 3, 0, 8, 7, 1, 2, 9, 5, 2, 4, 9, 2, 2, 6, 4, 3 },
      { 0, 2, 4, 5, 4, 2, 6, 6 }, { 8, 3, 2, 3, 8, 2, 4, 4, 4, 2, 6, 6 },
      { 10, 4, 9, 6, 10, 6, 4, 3, 11, 2, 3, 7 },
      { 0, 8, 2, 5, 2, 8, 11, 6, 4, 9, 10, 3, 4, 10, 6, 6 },
      { 3, 11, 2, 7, 0, 1, 6, 1, 0, 6, 4, 6, 6, 1, 10, 6 },
      { 6, 4, 1, 1, 6, 1, 10, 6, 4, 8, 1, 1, 2, 1, 11, 5, 8, 11, 1, 1 },
      { 9, 6, 4, 6, 9, 3, 6, 0, 9, 1, 3, 3, 11, 6, 3, 5 },
      { 8, 11, 1, 1, 8, 1, 0, 6, 11, 6, 1, 1, 9, 1, 4, 5, 6, 4, 1, 1 },
      { 3, 11, 6, 3, 3, 6, 0, 4, 0, 6, 4, 6 }, { 6, 4, 8, 3, 11, 6, 8, 5 },
      { 7, 10, 6, 6, 7, 8, 10, 1, 8, 9, 10, 3 },
      { 0, 7, 3, 6, 0, 10, 7, 0, 0, 9, 10, 3, 6, 7, 10, 5 },
      { 10, 6, 7, 3, 1, 10, 7, 1, 1, 7, 8, 2, 1, 8, 0, 6 },
      { 10, 6, 7, 3, 10, 7, 1, 4, 1, 7, 3, 6 },
      { 1, 2, 6, 3, 1, 6, 8, 0, 1, 8, 9, 6, 8, 6, 7, 6 },
      { 2, 6, 9, 1, 2, 9, 1, 6, 6, 7, 9, 1, 0, 9, 3, 5, 7, 3, 9, 1 },
      { 7, 8, 0, 3, 7, 0, 6, 4, 6, 0, 2, 6 }, { 7, 3, 2, 3, 6, 7, 2, 5 },
      { 2, 3, 11, 7, 10, 6, 8, 1, 10, 8, 9, 6, 8, 6, 7, 6 },
      { 2, 0, 7, 1, 2, 7, 11, 6, 0, 9, 7, 1, 6, 7, 10, 5, 9, 10, 7, 1 },
      { 1, 8, 0, 6, 1, 7, 8, 2, 1, 10, 7, 1, 6, 7, 10, 5, 2, 3, 11, 7 },
      { 11, 2, 1, 3, 11, 1, 7, 4, 10, 6, 1, 5, 6, 7, 1, 1 },
      { 8, 9, 6, 1, 8, 6, 7, 6, 9, 1, 6, 1, 11, 6, 3, 5, 1, 3, 6, 1 },
      { 0, 9, 1, 7, 11, 6, 7, 7 },
      { 7, 8, 0, 3, 7, 0, 6, 4, 3, 11, 0, 5, 11, 6, 0, 1 }, { 7, 11, 6, 7 },
      { 7, 6, 11, 7 }, { 3, 0, 8, 7, 11, 7, 6, 7 },
      { 0, 1, 9, 7, 11, 7, 6, 7 }, { 8, 1, 9, 6, 8, 3, 1, 3, 11, 7, 6, 7 },
      { 10, 1, 2, 7, 6, 11, 7, 7 }, { 1, 2, 10, 7, 3, 0, 8, 7, 6, 11, 7, 7 },
      { 2, 9, 0, 6, 2, 10, 9, 3, 6, 11, 7, 7 },
      { 6, 11, 7, 7, 2, 10, 3, 5, 10, 8, 3, 2, 10, 9, 8, 3 },
      { 7, 2, 3, 6, 6, 2, 7, 5 }, { 7, 0, 8, 6, 7, 6, 0, 1, 6, 2, 0, 3 },
      { 2, 7, 6, 6, 2, 3, 7, 3, 0, 1, 9, 7 },
      { 1, 6, 2, 6, 1, 8, 6, 0, 1, 9, 8, 3, 8, 7, 6, 3 },
      { 10, 7, 6, 6, 10, 1, 7, 1, 1, 3, 7, 3 },
      { 10, 7, 6, 6, 1, 7, 10, 4, 1, 8, 7, 2, 1, 0, 8, 3 },
      { 0, 3, 7, 3, 0, 7, 10, 0, 0, 10, 9, 6, 6, 10, 7, 5 },
      { 7, 6, 10, 3, 7, 10, 8, 4, 8, 10, 9, 6 }, { 6, 8, 4, 6, 11, 8, 6, 5 },
      { 3, 6, 11, 6, 3, 0, 6, 1, 0, 4, 6, 3 },
      { 8, 6, 11, 6, 8, 4, 6, 3, 9, 0, 1, 7 },
      { 9, 4, 6, 3, 9, 6, 3, 0, 9, 3, 1, 6, 11, 3, 6, 5 },
      { 6, 8, 4, 6, 6, 11, 8, 3, 2, 10, 1, 7 },
      { 1, 2, 10, 7, 3, 0, 11, 5, 0, 6, 11, 2, 0, 4, 6, 3 },
      { 4, 11, 8, 6, 4, 6, 11, 3, 0, 2, 9, 5, 2, 10, 9, 3 },
      { 10, 9, 3, 1, 10, 3, 2, 6, 9, 4, 3, 1, 11, 3, 6, 5, 4, 6, 3, 1 },
      { 8, 2, 3, 6, 8, 4, 2, 1, 4, 6, 2, 3 }, { 0, 4, 2, 5, 4, 6, 2, 3 },
      { 1, 9, 0, 7, 2, 3, 4, 1, 2, 4, 6, 6, 4, 3, 8, 6 },
      { 1, 9, 4, 3, 1, 4, 2, 4, 2, 4, 6, 6 },
      { 8, 1, 3, 6, 8, 6, 1, 0, 8, 4, 6, 3, 6, 10, 1, 3 },
      { 10, 1, 0, 3, 10, 0, 6, 4, 6, 0, 4, 6 },
      { 4, 6, 3, 1, 4, 3, 8, 6, 6, 10, 3, 1, 0, 3, 9, 5, 10, 9, 3, 1 },
      { 10, 9, 4, 3, 6, 10, 4, 5 }, { 4, 9, 5, 7, 7, 6, 11, 7 },
      { 0, 8, 3, 7, 4, 9, 5, 7, 11, 7, 6, 7 },
      { 5, 0, 1, 6, 5, 4, 0, 3, 7, 6, 11, 7 },
      { 11, 7, 6, 7, 8, 3, 4, 5, 3, 5, 4, 2, 3, 1, 5, 3 },
      { 9, 5, 4, 7, 10, 1, 2, 7, 7, 6, 11, 7 },
      { 6, 11, 7, 7, 1, 2, 10, 7, 0, 8, 3, 7, 4, 9, 5, 7 },
      { 7, 6, 11, 7, 5, 4, 10, 5, 4, 2, 10, 2, 4, 0, 2, 3 },
      { 3, 4, 8, 6, 3, 5, 4, 2, 3, 2, 5, 1, 10, 5, 2, 5, 11, 7, 6, 7 },
      { 7, 2, 3, 6, 7, 6, 2, 3, 5, 4, 9, 7 },
      { 9, 5, 4, 7, 0, 8, 6, 1, 0, 6, 2, 6, 6, 8, 7, 6 },
      { 3, 6, 2, 6, 3, 7, 6, 3, 1, 5, 0, 5, 5, 4, 0, 3 },
      { 6, 2, 8, 1, 6, 8, 7, 6, 2, 1, 8, 1, 4, 8, 5, 5, 1, 5, 8, 1 },
      { 9, 5, 4, 7, 10, 1, 6, 5, 1, 7, 6, 2, 1, 3, 7, 3 },
      { 1, 6, 10, 6, 1, 7, 6, 2, 1, 0, 7, 1, 8, 7, 0, 5, 9, 5, 4, 7 },
      { 4, 0, 10, 1, 4, 10, 5, 6, 0, 3, 10, 1, 6, 10, 7, 5, 3, 7, 10, 1 },
      { 7, 6, 10, 3, 7, 10, 8, 4, 5, 4, 10, 5, 4, 8, 10, 1 },
      { 6, 9, 5, 6, 6, 11, 9, 1, 11, 8, 9, 3 },
      { 3, 6, 11, 6, 0, 6, 3, 4, 0, 5, 6, 2, 0, 9, 5, 3 },
      { 0, 11, 8, 6, 0, 5, 11, 0, 0, 1, 5, 3, 5, 6, 11, 3 },
      { 6, 11, 3, 3, 6, 3, 5, 4, 5, 3, 1, 6 },
      { 1, 2, 10, 7, 9, 5, 11, 1, 9, 11, 8, 6, 11, 5, 6, 6 },
      { 0, 11, 3, 6, 0, 6, 11, 2, 0, 9, 6, 1, 5, 6, 9, 5, 1, 2, 10, 7 },
      { 11, 8, 5, 1, 11, 5, 6, 6, 8, 0, 5, 1, 10, 5, 2, 5, 0, 2, 5, 1 },
      { 6, 11, 3, 3, 6, 3, 5, 4, 2, 10, 3, 5, 10, 5, 3, 1 },
      { 5, 8, 9, 6, 5, 2, 8, 0, 5, 6, 2, 3, 3, 8, 2, 5 },
      { 9, 5, 6, 3, 9, 6, 0, 4, 0, 6, 2, 6 },
      { 1, 5, 8, 1, 1, 8, 0, 6, 5, 6, 8, 1, 3, 8, 2, 5, 6, 2, 8, 1 },
      { 1, 5, 6, 3, 2, 1, 6, 5 },
      { 1, 3, 6, 1, 1, 6, 10, 6, 3, 8, 6, 1, 5, 6, 9, 5, 8, 9, 6, 1 },
      { 10, 1, 0, 3, 10, 0, 6, 4, 9, 5, 0, 5, 5, 6, 0, 1 },
      { 0, 3, 8, 7, 5, 6, 10, 7 }, { 10, 5, 6, 7 },
      { 11, 5, 10, 6, 7, 5, 11, 5 }, { 11, 5, 10, 6, 11, 7, 5, 3, 8, 3, 0, 7 },
      { 5, 11, 7, 6, 5, 10, 11, 3, 1, 9, 0, 7 },
      { 10, 7, 5, 6, 10, 11, 7, 3, 9, 8, 1, 5, 8, 3, 1, 3 },
      { 11, 1, 2, 6, 11, 7, 1, 1, 7, 5, 1, 3 },
      { 0, 8, 3, 7, 1, 2, 7, 1, 1, 7, 5, 6, 7, 2, 11, 6 },
      { 9, 7, 5, 6, 9, 2, 7, 0, 9, 0, 2, 3, 2, 11, 7, 3 },
      { 7, 5, 2, 1, 7, 2, 11, 6, 5, 9, 2, 1, 3, 2, 8, 5, 9, 8, 2, 1 },
      { 2, 5, 10, 6, 2, 3, 5, 1, 3, 7, 5, 3 },
      { 8, 2, 0, 6, 8, 5, 2, 0, 8, 7, 5, 3, 10, 2, 5, 5 },
      { 9, 0, 1, 7, 5, 10, 3, 1, 5, 3, 7, 6, 3, 10, 2, 6 },
      { 9, 8, 2, 1, 9, 2, 1, 6, 8, 7, 2, 1, 10, 2, 5, 5, 7, 5, 2, 1 },
      { 1, 3, 5, 5, 3, 7, 5, 3 }, { 0, 8, 7, 3, 0, 7, 1, 4, 1, 7, 5, 6 },
      { 9, 0, 3, 3, 9, 3, 5, 4, 5, 3, 7, 6 }, { 9, 8, 7, 3, 5, 9, 7, 5 },
      { 5, 8, 4, 6, 5, 10, 8, 1, 10, 11, 8, 3 },
      { 5, 0, 4, 6, 5, 11, 0, 0, 5, 10, 11, 3, 11, 3, 0, 3 },
      { 0, 1, 9, 7, 8, 4, 10, 1, 8, 10, 11, 6, 10, 4, 5, 6 },
      { 10, 11, 4, 1, 10, 4, 5, 6, 11, 3, 4, 1, 9, 4, 1, 5, 3, 1, 4, 1 },
      { 2, 5, 1, 6, 2, 8, 5, 0, 2, 11, 8, 3, 4, 5, 8, 5 },
      { 0, 4, 11, 1, 0, 11, 3, 6, 4, 5, 11, 1, 2, 11, 1, 5, 5, 1, 11, 1 },
      { 0, 2, 5, 1, 0, 5, 9, 6, 2, 11, 5, 1, 4, 5, 8, 5, 11, 8, 5, 1 },
      { 9, 4, 5, 7, 2, 11, 3, 7 },
      { 2, 5, 10, 6, 3, 5, 2, 4, 3, 4, 5, 2, 3, 8, 4, 3 },
      { 5, 10, 2, 3, 5, 2, 4, 4, 4, 2, 0, 6 },
      { 3, 10, 2, 6, 3, 5, 10, 2, 3, 8, 5, 1, 4, 5, 8, 5, 0, 1, 9, 7 },
      { 5, 10, 2, 3, 5, 2, 4, 4, 1, 9, 2, 5, 9, 4, 2, 1 },
      { 8, 4, 5, 3, 8, 5, 3, 4, 3, 5, 1, 6 }, { 0, 4, 5, 3, 1, 0, 5, 5 },
      { 8, 4, 5, 3, 8, 5, 3, 4, 9, 0, 5, 5, 0, 3, 5, 1 }, { 9, 4, 5, 7 },
      { 4, 11, 7, 6, 4, 9, 11, 1, 9, 10, 11, 3 },
      { 0, 8, 3, 7, 4, 9, 7, 5, 9, 11, 7, 2, 9, 10, 11, 3 },
      { 1, 10, 11, 3, 1, 11, 4, 0, 1, 4, 0, 6, 7, 4, 11, 5 },
      { 3, 1, 4, 1, 3, 4, 8, 6, 1, 10, 4, 1, 7, 4, 11, 5, 10, 11, 4, 1 },
      { 4, 11, 7, 6, 9, 11, 4, 4, 9, 2, 11, 2, 9, 1, 2, 3 },
      { 9, 7, 4, 6, 9, 11, 7, 2, 9, 1, 11, 1, 2, 11, 1, 5, 0, 8, 3, 7 },
      { 11, 7, 4, 3, 11, 4, 2, 4, 2, 4, 0, 6 },
      { 11, 7, 4, 3, 11, 4, 2, 4, 8, 3, 4, 5, 3, 2, 4, 1 },
      { 2, 9, 10, 6, 2, 7, 9, 0, 2, 3, 7, 3, 7, 4, 9, 3 },
      { 9, 10, 7, 1, 9, 7, 4, 6, 10, 2, 7, 1, 8, 7, 0, 5, 2, 0, 7, 1 },
      { 3, 7, 10, 1, 3, 10, 2, 6, 7, 4, 10, 1, 1, 10, 0, 5, 4, 0, 10, 1 },
      { 1, 10, 2, 7, 8, 7, 4, 7 }, { 4, 9, 1, 3, 4, 1, 7, 4, 7, 1, 3, 6 },
      { 4, 9, 1, 3, 4, 1, 7, 4, 0, 8, 1, 5, 8, 7, 1, 1 },
      { 4, 0, 3, 3, 7, 4, 3, 5 }, { 4, 8, 7, 7 },
      { 9, 10, 8, 5, 10, 11, 8, 3 }, { 3, 0, 9, 3, 3, 9, 11, 4, 11, 9, 10, 6 },
      { 0, 1, 10, 3, 0, 10, 8, 4, 8, 10, 11, 6 },
      { 3, 1, 10, 3, 11, 3, 10, 5 }, { 1, 2, 11, 3, 1, 11, 9, 4, 9, 11, 8, 6 },
      { 3, 0, 9, 3, 3, 9, 11, 4, 1, 2, 9, 5, 2, 11, 9, 1 },
      { 0, 2, 11, 3, 8, 0, 11, 5 }, { 3, 2, 11, 7 },
      { 2, 3, 8, 3, 2, 8, 10, 4, 10, 8, 9, 6 }, { 9, 10, 2, 3, 0, 9, 2, 5 },
      { 2, 3, 8, 3, 2, 8, 10, 4, 0, 1, 8, 5, 1, 10, 8, 1 }, { 1, 10, 2, 7 },
      { 1, 3, 8, 3, 9, 1, 8, 5 }, { 0, 9, 1, 7 }, { 0, 3, 8, 7 }, null };

}
