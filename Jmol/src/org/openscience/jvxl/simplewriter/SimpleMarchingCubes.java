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
package org.openscience.jvxl.simplewriter;

import java.util.BitSet;
import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.VolumeData;

//import org.jmol.util.Logger;

public class SimpleMarchingCubes {

  /*
   * An adaptation of Marching Cubes to include data slicing and the option
   * for progressive reading of the data. Associated SurfaceReader and VoxelData
   * structures are required to store the sequential values in the case of a plane
   * and to deliver the sequential vertex numbers in any case.
   * 
   * Author: Bob Hanson, hansonr@stolaf.edu
   * 
   * The "Simple" version does not create triangle data, 
   * just the JVXL fractionData string
   *  
   */

  private VolumeData volumeData;
  private float cutoff;
  private boolean isCutoffAbsolute;
  private boolean isXLowToHigh;
  private boolean doCalcArea;
  private boolean doSaveSurfacePoints;
  private float calculatedArea = Float.NaN;
  private float calculatedVolume = Float.NaN;
  private Vector surfacePoints;
    
  private StringBuffer fractionData = new StringBuffer();

  private int cubeCountX, cubeCountY, cubeCountZ;
  private int nY, nZ;
  private int yzCount;

  private BitSet bsVoxels = new BitSet();

  private int mode;
  private final static int MODE_CUBE = 1;
  private final static int MODE_BITSET = 2;
  private final static int MODE_GETXYZ = 3;

  private VoxelDataCreator vdc;
  private BitSet bsExcludedVertices;
//  private BitSet bsExcludedTriangles; // not used in this application
  
  public SimpleMarchingCubes(VoxelDataCreator vdc, VolumeData volumeData,
      JvxlData jvxlData, Vector surfacePointsReturn, float[] areaVolumeReturn) {

    // when just creating a JVXL file all you really need are:
    //
    // volumeData.voxelData[x][y][z]
    // cutoff
    //
    // also includes the option to return a Vector of surfacePoints
    // and/or calculate the area of the surface.
    //

    /* these next two bitsets encode vertices excluded because they are NaN
     * (which will exclude the entire cell)
     * and triangles because, perhaps, they are out of range.
     * 
     */
    jvxlData.jvxlExcluded = new BitSet[2];
    jvxlData.jvxlExcluded[0] = bsExcludedVertices = new BitSet();
   // jvxlData.jvxlExcluded[1] = bsExcludedTriangles = new BitSet();
    
    this.vdc = vdc;
    this.volumeData = volumeData;
    cutoff = jvxlData.cutoff;
    isCutoffAbsolute = jvxlData.isCutoffAbsolute;
    isXLowToHigh = jvxlData.isXLowToHigh;
    doCalcArea = (areaVolumeReturn != null);
    surfacePoints = surfacePointsReturn;
    if (surfacePoints == null && doCalcArea)
      surfacePoints = new Vector();
    doSaveSurfacePoints = (surfacePoints != null);

    if (vdc == null) {
      mode = MODE_CUBE;
    } else {
      mode = MODE_GETXYZ;
    }

    cubeCountX = volumeData.voxelCounts[0] - 1;
    cubeCountY = (nY = volumeData.voxelCounts[1]) - 1;
    cubeCountZ = (nZ = volumeData.voxelCounts[2]) - 1;
    yzCount = nY * nZ;
    edgeVertexPointers = (isXLowToHigh ? edgeVertexPointersLowToHigh
        : edgeVertexPointersHighToLow);
    edgeVertexPlanes = (isXLowToHigh ? edgeVertexPlanesLowToHigh
        : edgeVertexPlanesHighToLow);
    isoPointIndexPlanes = new int[2][yzCount][3];
    xyPlanes = (mode == MODE_GETXYZ ? new float[2][yzCount] : null);
    setLinearOffsets();
    calcVoxelVertexVectors();
    jvxlData.jvxlEdgeData = getEdgeData();
    jvxlData.nPointsX = volumeData.voxelCounts[0];
    jvxlData.nPointsY = volumeData.voxelCounts[1];
    jvxlData.nPointsZ = volumeData.voxelCounts[2];
    jvxlData.setSurfaceInfoFromBitSet(bsVoxels, null);
    if (doCalcArea) {
      areaVolumeReturn[0] = calculatedArea;
      areaVolumeReturn[1] = calculatedVolume;
    }    
  }

  private final float[] vertexValues = new float[8];
  private final Point3i[] vertexPoints = new Point3i[8];
  {
    for (int i = 8; --i >= 0;)
      vertexPoints[i] = new Point3i();
  }

  int edgeCount;

  ////// the following methods are only necessary if working with triangles:

  private final Vector3f[] voxelVertexVectors = new Vector3f[8];
  private final Vector3f[] edgeVectors = new Vector3f[12];
  {
    for (int i = 12; --i >= 0;)
      edgeVectors[i] = new Vector3f();
    for (int i = 8; --i >= 0;)
      vertexPoints[i] = new Point3i();
  }

  private void calcVoxelVertexVectors() {
    // only necessary if working with the surface points
    volumeData.setMatrix();
    for (int i = 8; --i >= 0;)
      volumeData.transform(cubeVertexVectors[i],
          voxelVertexVectors[i] = new Vector3f());
    for (int i = 12; --i >= 0;)
      edgeVectors[i].sub(voxelVertexVectors[edgeVertexes[i + i + 1]],
          voxelVertexVectors[edgeVertexes[i + i]]);
  }

  private final static Vector3f[] cubeVertexVectors = { 
    new Vector3f(0, 0, 0),
    new Vector3f(1, 0, 0), 
    new Vector3f(1, 0, 1), 
    new Vector3f(0, 0, 1),
    new Vector3f(0, 1, 0), 
    new Vector3f(1, 1, 0), 
    new Vector3f(1, 1, 1),
    new Vector3f(0, 1, 1) };
  
  ////////
  
  
  private static int[] xyPlanePts = new int[] { 
      0, 1, 1, 0, 
      0, 1, 1, 0 
  };
  
  private final int[] edgePointIndexes = new int[12];
  private int[][][] isoPointIndexPlanes;
  private float[][] xyPlanes;

  private int[][] resetIndexPlane(int[][] plane) {
    for (int i = 0; i < yzCount; i++)
      for (int j = 0; j < 3; j++)
        plane[i][j] = -1;
    return plane;
  }
  
  public String getEdgeData() {

    //Logger.startTimer();
    
    /* The (new, Jmol 11.7.26) Marching Cubes code creates 
     * the isoPointIndexes[2][nY * nZ][3] array that holds two slices 
     * of edge data. Each edge is assigned a specific vertex, 
     * such that each vertex may have up to 3 associated edges. 
     * 
     *  Feb 10, 2009 -- Bob Hanson
     */
    
    edgeCount = 0;
    calculatedArea = 0;
    calculatedVolume = 0;
    if (doSaveSurfacePoints)
      surfacePoints.clear();

    int x0, x1, xStep, ptStep, pt, ptX;
    if (isXLowToHigh) {
      x0 = 0;
      x1 = cubeCountX;
      xStep = 1;
      ptStep = yzCount;
      pt = ptX = (yzCount - 1) - nZ - 1;
      // we are starting at the top corner, in the next to last
      // cell on the next to last row of the first plane
    } else {
      x0 = cubeCountX - 1;
      x1 = -1;
      xStep = -1;
      ptStep = -yzCount;
      pt = ptX = (cubeCountX * yzCount - 1) - nZ - 1;
      // we are starting at the top corner, in the next to last
      // cell on the next to last row of the next to last plane(!)
    }
    int cellIndex0 = cubeCountY * cubeCountZ - 1;
    int cellIndex = cellIndex0;
    resetIndexPlane(isoPointIndexPlanes[1]);
    for (int x = x0; x != x1; x += xStep, ptX += ptStep, pt = ptX, cellIndex = cellIndex0) {
      
      // we swap planes of grid data when 
      // obtaining the grid data point by point
      
      if (mode == MODE_GETXYZ) {
        float[] plane = xyPlanes[0];
        xyPlanes[0] = xyPlanes[1];
        xyPlanes[1] = plane;
      }
      
      // we swap the edge vertex index planes
      
      int[][] indexPlane = isoPointIndexPlanes[0];
      isoPointIndexPlanes[0] = isoPointIndexPlanes[1];
      isoPointIndexPlanes[1] = resetIndexPlane(indexPlane);
      
      // now scan the plane of cubicals

      // now scan the plane of cubicals
      
      for (int y = cubeCountY; --y >= 0; pt--) {
        for (int z = cubeCountZ; --z >= 0; pt--, cellIndex--) {
          
          // create the bitset mask indicating which vertices are inside.
          // 0xFF here means "all inside"; 0x00 means "all outside"
          
          int insideMask = 0;
          for (int i = 8; --i >= 0;) {
            
            // cubeVertexOffsets just gets us the specific grid point relative
            // to our base x,y,z cube position
            
            boolean isInside;
            float v;
            Point3i offset = cubeVertexOffsets[i];
            int pti = pt + linearOffsets[i];
            switch (mode) {
            case MODE_GETXYZ:
              v = vertexValues[i] = getValue(i, x + offset.x, y + offset.y, z
                  + offset.z, pti, xyPlanes[xyPlanePts[i]]);
              isInside = bsVoxels.get(pti);
              break;
            case MODE_BITSET:
              isInside = bsVoxels.get(pti);
              v = vertexValues[i] = (bsExcludedVertices.get(pti) ? Float.NaN : isInside ? 1 : 0);
              break;
            default:
            case MODE_CUBE:
              v = vertexValues[i] = volumeData.voxelData[x + offset.x][y + offset.y][z
                  + offset.z];
              isInside = isInside(vertexValues[i], cutoff, isCutoffAbsolute);
              if (isInside)
                bsVoxels.set(pti);
            }
            if (isInside) {
              insideMask |= Pwr2[i];
            }
            
            if (Float.isNaN(v))
              bsExcludedVertices.set(pti);
          }

          if (insideMask == 0) {
            continue;
          }
          if (insideMask == 0xFF) {
            continue;
          }
          // This cube is straddling the cutoff. We must check all edges 
          
          if (!processOneCubical(insideMask, x, y, z, pt))
            continue;

          // the inside mask serves to define the triangles necessary 
          // if just creating JVXL files, this step is unnecessary
          
          if (!doCalcArea)
            continue;
          byte[] triangles = triangleTable2[insideMask];
          for (int i = triangles.length; (i -= 4) >= 0;)
            addTriangle(triangles[i], triangles[i + 1],
                triangles[i + 2],triangles[i + 3]);
          
        }
      }
    }
    //Logger.checkTimer("getEdgeData");
    return fractionData.toString();
  }
  
  Vector3f vTemp = new Vector3f();
  Vector3f vAC = new Vector3f();
  Vector3f vAB = new Vector3f();

  private void addTriangle(int ia, int ib, int ic, int edgeType) {
    
    // If you were doing something with the triangle vertics
    // Say, for example, summing the area, then here you would 
    // need to retrieve the saved coordinates from some other array
    // for each of the three points ia, ib, and ic,
    // and then process them.
    
    // or you could excluede a triangle and record that in bsExcludedTriangles
    
    // in this example we are just computing the area and volume
   
    Point3f pta = (Point3f) surfacePoints.get(edgePointIndexes[ia]);
    Point3f ptb = (Point3f) surfacePoints.get(edgePointIndexes[ib]);
    Point3f ptc = (Point3f) surfacePoints.get(edgePointIndexes[ic]);
    
    vAB.sub(ptb, pta);
    vAC.sub(ptc, pta);
    vTemp.cross(vAB, vAC);
    float area = vTemp.length() / 2;
    calculatedArea += area;
    
    vAB.set(ptb);
    vAC.set(ptc);
    vTemp.cross(vAB, vAC);
    vAC.set(pta);
    calculatedVolume += vAC.dot(vTemp) / 6;
  }


  public static boolean isInside(float voxelValue, float max, boolean isAbsolute) {
    return ((max > 0 && (isAbsolute ? Math.abs(voxelValue) : voxelValue) >= max) || (max <= 0 && voxelValue <= max));
  }

  BitSet bsValues = new BitSet();

  private float getValue(int i, int x, int y, int z, int pt, float[] tempValues) {
    if (bsValues.get(pt))
      return tempValues[pt % yzCount];
    bsValues.set(pt);
    float value = vdc.getValue(x, y, z);
    tempValues[pt % yzCount] = value;
    //System.out.println("xyz " + x + " " + y + " " + z + " v=" + value);
    if (isInside(value, cutoff, isCutoffAbsolute))
      bsVoxels.set(pt);
    return value;
  }

  private final Point3f pt0 = new Point3f();
  private final Point3f pointA = new Point3f();

  private static final int[] Pwr2 = new int[] { 1, 2, 4, 8, 16, 32, 64, 128,
      256, 512, 1024, 2048 };

  private final static int[] edgeVertexPointersLowToHigh = new int[] {
      1, 1, 2, 0, 
      5, 5, 6, 4,
      0, 1, 2, 3
  };
  
  private final static int[] edgeVertexPointersHighToLow = new int[] {
      0, 1, 3, 0, 
      4, 5, 7, 4,
      0, 1, 2, 3
  };

  private int[] edgeVertexPointers;

  private final static int[] edgeVertexPlanesLowToHigh = new int[] {
      1, 1, 1, 0, 
      1, 1, 1, 0, 
      0, 1, 1, 0
  };  // from high to low, only edges 3, 7, 8, and 11 are from plane 0

  private final static int[] edgeVertexPlanesHighToLow = new int[] {
      1, 0, 1, 1,
      1, 0, 1, 1,
      1, 0, 0, 1
  }; //from high to low, only edges 1, 5, 9, and 10 are from plane 0
  
  private int[] edgeVertexPlanes;
  
  private boolean processOneCubical(int insideMask, int x, int y, int z, int pt) {

    
    /*
     * The key to the algorithm is that we have a catalog that
     * maps the inside-vertex mask to an edge mask, and then
     * each edge is associated with a specific vertex. 
     * 
     * Each cube vertex may be associated with from 0 to 3 edges,
     * depending upon where it lies in the overall cube of data.
     * 
     * When scanning X from low to high, the "leading vertex" is
     * vertex 1 and edgeVertexPlanes[1]. Edges 0, 1, and 9 are 
     * associated with vertex 1, and others are associated similarly.
     * 
     * When scanning X from high to low, the "leading vertex" is
     * vertex 0 and edgeVertexPlanes[1]. Edges 0, 3, and 8 are 
     * associated with vertex 0, and others are associated similarly.
     * 
     * edgePointIndexes[iEdge] tracks the vertex index for this
     * specific cubical so that triangles can be created properly.
     *  
     * 
     *                      Y 
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
     *              /                    /              
     *  edgeVertexPlanes[0]            [1] (scanning x low to high)
     *  edgeVertexPlanes[1]            [0] (scanning x high to low)
     *  
     */

    int edgeMask = insideMaskTable[insideMask];
    //for (int i =0; i < 8; i++) System.out.print("\nvpi for cell  " + pt + ": vertex " + i + ": " + voxelPointIndexes[i] + " " + Integer.toBinaryString(edgeMask));
    boolean isNaN = false;
    for (int iEdge = 12; --iEdge >= 0;) {
      
      // bit set to one means it's a relevant edge
      
      int xEdge = Pwr2[iEdge];
      if ((edgeMask & xEdge) == 0)
        continue;
      
      
      // if we have a point already, we don't need to check this edge.
      // for triangles, this will be an index into an array;
      // for just creating JVXL files, this can just be 0
      
      int iPlane = edgeVertexPlanes[iEdge];
      int iPt = (pt + linearOffsets[edgeVertexPointers[iEdge]]) % yzCount;
      int iType = edgeTypeTable[iEdge];
      int index = edgePointIndexes[iEdge] = isoPointIndexPlanes[iPlane][iPt][iType];
      //System.out.println(x + " " + y + " " + z + " " + pt + " iEdge=" + iEdge + " p=" + iPlane + " t=" + iType + " e=" + ePt + " i=" + iPt + " index=" + edgePointIndexes[iEdge]);
      if (index >= 0)
        continue; // propagated from neighbor
      
      // here's an edge that has to be checked.
      
      // get the vertex numbers 0 - 7
      
      int vertexA = edgeVertexes[iEdge << 1];
      int vertexB = edgeVertexes[(iEdge << 1) + 1];
      
      // pick up the actual value at each vertex
      // this array of 8 values is updated as we go.
      
      float valueA = vertexValues[vertexA];
      float valueB = vertexValues[vertexB];
      
      // we allow for NaN values -- missing triangles
      
      if (Float.isNaN(valueA) || Float.isNaN(valueB))
        isNaN = true;
      
      // the exact point position -- not important for just
      // creating the JVXL file. In that case, all you 
      // need are the two values valueA and valueB and the cutoff.
      // from those you can define the fractional offset
      
      // here is where we get the value and assign the point for that edge
      // it is where the JVXL surface data line is appended
      
      if (doSaveSurfacePoints) {
        volumeData.voxelPtToXYZ(x, y, z, pt0);
        pointA.add(pt0, voxelVertexVectors[vertexA]);
      }
      float f = (cutoff - valueA) / (valueB - valueA);
      edgePointIndexes[iEdge] = isoPointIndexPlanes[iPlane][iPt][iType] = 
        newVertex(pointA, edgeVectors[iEdge], f);
      //System.out.println(" pt=" + pt + " edge" + iEdge + " xyz " + x + " " + y + " " + z + " vertexAB=" + vertexA + " " + vertexB + " valueAB=" + valueA + " " + valueB + " f= " + (cutoff - valueA) / (valueB - valueA));
      //System.out.println(f);
      fractionData.append(JvxlCoder.jvxlFractionAsCharacter(f));
      
    }
    return !isNaN;
  }

  private int newVertex(Point3f pointA, Vector3f edgeVector, float f) {
    // you could do something with this point if you wanted to
    // for example,

    if (doSaveSurfacePoints) {
      Point3f pt = new Point3f();
      pt.scaleAdd(f, edgeVector, pointA);
      surfacePoints.addElement(pt);
    }
    return edgeCount++;
  }
  
  private final int[] linearOffsets = new int[8]; 

  /* 
   * set the linear offsets for generating a unique cell ID,
   * for pointing into the inside/outside BitSet,
   * and for finding the associated vertex for an edge.
   * 
   */
  
  private void setLinearOffsets() {
    linearOffsets[0] = 0;
    linearOffsets[1] = yzCount;
    linearOffsets[2] = yzCount + 1;
    linearOffsets[3] = 1;
    linearOffsets[4] = nZ;
    linearOffsets[5] = yzCount + nZ;
    linearOffsets[6] = yzCount + nZ + 1;
    linearOffsets[7] = nZ + 1;
  }

  public int getLinearOffset(int x, int y, int z, int offset) {
    return x * yzCount + y * nZ + z + linearOffsets[offset];
  }

  private final static Point3i[] cubeVertexOffsets = { 
    new Point3i(0, 0, 0), //0 pt
    new Point3i(1, 0, 0), //1 pt + yz
    new Point3i(1, 0, 1), //2 pt + yz + 1
    new Point3i(0, 0, 1), //3 pt + 1
    new Point3i(0, 1, 0), //4 pt + z
    new Point3i(1, 1, 0), //5 pt + yz + z
    new Point3i(1, 1, 1), //6 pt + yz + z + 1
    new Point3i(0, 1, 1)  //7 pt + z + 1 
  };


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

   private final static int edgeTypeTable[] = { 
     0, 2, 0, 2, 
     0, 2, 0, 2, 
     1, 1, 1, 1 };
  // 0=along X, 1=along Y, 2=along Z

  private final static byte edgeVertexes[] = { 
    0, 1, 1, 2, 2, 3, 3, 0, 4, 5,
  /*0     1     2     3     4  */
    5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7 };
  /*5     6     7     8     9     10    11 */

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

  /* -- not needed just for JVXL writer
   * -- included here for reference or for users who
   * -- want to produce triangles using this code.
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
