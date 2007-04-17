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
package org.openscience.jvxl.calc;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.util.Logger;
import org.openscience.jvxl.util.*;
import org.openscience.jvxl.data.VolumeData;
import org.openscience.jvxl.data.MeshData;


public class MarchingSquares {

  /*
   * An adaptation of Marching Cubes to a two-dimensional slice.
   * 
   * Author: Bob Hanson, hansonr@stolaf.edu
   *  
   */
  
  private VolumeData volumeData;

  private final static int nContourMax = 100;
  private final static int defaultContourCount = 11; //odd is better
  private int nContours;
  private int contourType;//0, 1, or 2
  private Point4f thePlane;
  private boolean is3DContour;
  private int thisContour = 0;
  private float valueMin, valueMax;

  private final Vector3f pointVector = new Vector3f();
  private final Point3f pointA = new Point3f();
  private final Point3f pointB = new Point3f();
  private final Vector3f edgeVector = new Vector3f();

  private final Point3f planarOrigin = new Point3f();
  private final Vector3f[] planarVectors = new Vector3f[3];
  private final Vector3f[] unitPlanarVectors = new Vector3f[3];
  private final float[] planarVectorLengths = new float[2];
  private final Matrix3f matXyzToPlane = new Matrix3f();
  {
    planarVectors[0] = new Vector3f();
    planarVectors[1] = new Vector3f();
    planarVectors[2] = new Vector3f();
    unitPlanarVectors[0] = new Vector3f();
    unitPlanarVectors[1] = new Vector3f();
    unitPlanarVectors[2] = new Vector3f();
  }

  public MarchingSquares(VolumeData volumeData, Point4f thePlane, 
      int nContours, int thisContour) {
    this.volumeData = volumeData;
    this.thePlane = thePlane;
    this.nContours = (nContours == 0 ? defaultContourCount
        : nContours > nContourMax ? nContourMax : nContours);
    this.thisContour = thisContour;
    is3DContour = (thePlane == null);
    setContourType();
  }

  public int getContourType() {
    return contourType;
  }
  
  public void setMinMax(float valueMin, float valueMax) {
    this.valueMin = valueMin;
    this.valueMax = valueMax;
  }
    
  ////////////////////////////////////////////////////////////////
  // contour plane implementation 
  ////////////////////////////////////////////////////////////////

  private void setContourType() {
    if (is3DContour) {
      planarVectors[0].set(volumeData.volumetricVectors[0]);
      planarVectors[1].set(volumeData.volumetricVectors[1]);
      pixelCounts[0] = volumeData.voxelCounts[0];
      pixelCounts[1] = volumeData.voxelCounts[1];
      contourType = 2;
      return;
    }
    contourType = getContourType(thePlane, volumeData.volumetricVectors);
  }
  
  private static int getContourType(Point4f plane, Vector3f[] volumetricVectors) {
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

  private final static Point3i[] squareVertexOffsets = { new Point3i(0, 0, 0),
    new Point3i(1, 0, 0), new Point3i(1, 1, 0), new Point3i(0, 1, 0) };

  private final static Vector3f[] squareVertexVectors = { new Vector3f(0, 0, 0),
    new Vector3f(1, 0, 0), new Vector3f(1, 1, 0), new Vector3f(0, 1, 0) };

  private final static byte edgeVertexes2d[] = { 0, 1, 1, 2, 2, 3, 3, 0 };

  private final static byte insideMaskTable2d[] = { 0, 9, 3, 10, 6, 15, 5, 12, 12, 5,
    15, 6, 10, 3, 9, 0 };

  // position in the table corresponds to the binary equivalent of which corners are inside
  // for example, 0th is completely outside; 15th is completely inside;
  // the 4th entry (0b0100; 2**3), corresponding to only the third corner inside, is 6 (0b1100). 
  // Bits 2 and 3 are set, so edges 2 and 3 intersect the contour.

  private MeshData meshData;

  public void generateContourData(boolean haveData, MeshData meshData) {

    this.meshData = meshData;

    /*
     * (1) define the plane
     * (2) calculate the grid "pixel" points
     * (3) generate the contours using marching squares
     * (4) 
     * The idea is to first just catalog the vertices, then see what we need to do about them.
     * 
     */

    Logger.info("generateContours:" + nContours);

    if (!is3DContour)
      getPlanarVectors();
    setPlanarTransform();
    getPlanarOrigin();
    setupMatrix(planarMatrix, planarVectors);

    calcPixelVertexVectors();
    if (!is3DContour)
      getPixelCounts();
    createPlanarSquares();
    loadPixelData(haveData);
    createContours(valueMin, valueMax);
    triangulateContours();
  }

  // (1) define the plane

  private void getPlanarVectors() {
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

    Vector3f thePlaneNormal = new Vector3f(thePlane.x, thePlane.y, thePlane.z);

    Vector3f[] volumetricVectors = volumeData.volumetricVectors;
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

  private void setPlanarTransform() {
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

  private void getPlanarOrigin() {
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

    void setValue(float value, VolumeData volumeData) {
      this.value = volumeData.voxelData[voxelLocation.x][voxelLocation.y][voxelLocation.z] = value;
    }

    void setPixelLocation(Point3i pt) {
      pixelLocation[0] = pt.x;
      pixelLocation[1] = pt.y;
    }
  }

  public int addContourData(int x, int y, int z, Point3i offsets, Point3f vertexXYZ,
                     float value) {
    if (contourVertexes == null)
      contourVertexes = new ContourVertex[256];
    if (contourVertexCount == contourVertexes.length)
      contourVertexes = (ContourVertex[]) ArrayUtil
          .doubleLength(contourVertexes);
    x += offsets.x;
    y += offsets.y;
    z += offsets.z;
    int vPt = meshData.addVertexCopy(vertexXYZ, value);
    contourVertexes[contourVertexCount++] = new ContourVertex(x, y, z,
        vertexXYZ, vPt);
    return vPt;
  }

  public void setContourData(int i, float value) {
    contourVertexes[i].setValue(value, volumeData);  
  }
  
  private void loadPixelData(boolean haveData) {
    pixelData = new float[pixelCounts[0]][pixelCounts[1]];
    int x, y;
    contourPlaneMinimumValue = Float.MAX_VALUE;
    contourPlaneMaximumValue = -Float.MAX_VALUE;
    for (int i = 0; i < contourVertexCount; i++) {
      ContourVertex c = contourVertexes[i];
      Point3i pt = locatePixel(c.vertexXYZ);
      c.setPixelLocation(pt);
      float value;
      if (haveData) {
        value = c.value;
      } else {
        value = volumeData.lookupInterpolatedVoxelValue(c.vertexXYZ);
        c.setValue(value, volumeData);
      }
      if (value < contourPlaneMinimumValue)
        contourPlaneMinimumValue = value;
      if (value > contourPlaneMaximumValue)
        contourPlaneMaximumValue = value;
      //if (i < 10)
        //Logger.info("loadPixelDatv " + c.vertexXYZ + value + pt);
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

  public float getInterpolatedPixelValue(Point3f ptXYZ) {
    pointVector.set(ptXYZ);
    xyzToPixelVector(pointVector);
    float x = pointVector.x;
    float y = pointVector.y;
    int xDown = (x >= pixelCounts[0] ? pixelCounts[0] - 1 : x < 0 ? 0 : (int) x);
    int yDown = (y >= pixelCounts[1] ? pixelCounts[1] - 1 : y < 0 ? 0 : (int) y);
    int xUp = xDown + (xDown == pixelCounts[0] - 1 ? 0 : 1);
    int yUp = yDown + (yDown == pixelCounts[1] - 1 ? 0 : 1);
    float value = VolumeData.getFractional2DValue(x - xDown, y - yDown,
        pixelData[xDown][yDown], pixelData[xUp][yDown], pixelData[xDown][yUp],
        pixelData[xUp][yUp]);
    return value;
  }

  // (3) generate the contours using marching squares

  private final int[] pixelCounts = new int[2];
  private final Matrix3f planarMatrix = new Matrix3f();
  private float[][] pixelData;

  private final float[] vertexValues2d = new float[4];
  private final Point3f[] contourPoints = new Point3f[4];
  {
    for (int i = 4; --i >= 0;)
      contourPoints[i] = new Point3f();
  }
  private int squareCountX, squareCountY;

  private PlanarSquare[] planarSquares;
  int nSquares;

  private class PlanarSquare {
    int[] edgeMask12; //one per contour
    int edgeMask12All;
    int nInside;
    int nOutside;
    int nThrough;
    int contourBits;
    //int x, y;
    //Point3f origin;
    final int[] vertexes = new int[4];
    float[][] fractions;
    int[][] intersectionPoints;

    PlanarSquare(int nContours) {
      edgeMask12 = new int[nContours];
      intersectionPoints = new int[nContours][4];
      fractions = new float[nContours][4];
      edgeMask12All = 0;
      contourBits = 0;
      //this.origin = origin;
      //this.x = x;
      //this.y = y;
    }

    void setIntersectionPoints(int contourIndex, int[] pts, float[] f) {
      for (int i = 0; i < 4; i++) {
        intersectionPoints[contourIndex][i] = pts[i];
        fractions[contourIndex][i] = f[i];
      }
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
       * the duplication is so that this can be used efficiently
       * in triangulateContourSquare().  
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

  private void getPixelCounts() {
    int max = 1;
    for (int i = 0; i < 3; i++) {
      if (i != contourType)
        max = Math.max(max, volumeData.voxelCounts[i]);
    }
    pixelCounts[0] = pixelCounts[1] = max;
    // just use the maximum value -- this isn't too critical,
    // but we want to have enough, and there were
    // problems with hkl = 110

    //    if (logMessages)
    //Logger.info("getPixelCounts " + pixelCounts[0] + "," + pixelCounts[1]);
  }

  private void createPlanarSquares() {
    squareCountX = pixelCounts[0] - 1;
    squareCountY = pixelCounts[1] - 1;

    planarSquares = new PlanarSquare[squareCountX * squareCountY];
    nSquares = 0;
    for (int x = 0; x < squareCountX; x++)
      for (int y = 0; y < squareCountY; y++)
        planarSquares[nSquares++] = new PlanarSquare(nContours);
    Logger.info("nSquares = " + nSquares);
  }

  private float contourPlaneMinimumValue;
  private float contourPlaneMaximumValue;

  private int contourIndex;

  public void createContours(float min, float max) {
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

  private void generateContourData(float contourCutoff) {

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
    float[][] squareFractions2d = new float[squareCountY][4];
    
    for (int i = squareCountY; --i >= 0;)
      isoPointIndexes2d[i][0] = isoPointIndexes2d[i][1] = isoPointIndexes2d[i][2] = isoPointIndexes2d[i][3] = -1;

    if (Math.abs(contourCutoff) < 0.0001)
      contourCutoff = (contourCutoff <= 0 ? -0.0001f : 0.0001f);
    int insideCount = 0, outsideCount = 0, contourCount = 0;
    for (int x = squareCountX; --x >= 0;) {
      for (int y = squareCountY; --y >= 0;) {
        int[] pixelPointIndexes = propagateNeighborPointIndexes2d(x, y,
            isoPointIndexes2d, squareFractions2d);
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

        //System.out.println(contourIndex + "/" + (x * squareCountY + y) + " " + x + " " + y 
          //  + " " + (pixelPointIndexes[0]  >= 0 ? squareFractions[0] : -1)
           // + " " + (pixelPointIndexes[1]  >= 0 ? squareFractions[1] : -1)
           // + " " + (pixelPointIndexes[2]  >= 0 ? squareFractions[2] : -1)
           // + " " + (pixelPointIndexes[3]  >= 0 ? squareFractions[3] : -1));
      }
    }

//    if (logMessages)
      Logger.info("contourCutoff=" + contourCutoff + " pixel squares="
          + squareCountX + "," + squareCountY + "," + " total="
          + (squareCountX * squareCountY) + "\n" + " insideCount="
          + insideCount + " outsideCount=" + outsideCount + " contourCount="
          + contourCount + " total="
          + (insideCount + outsideCount + contourCount));
  }

  private boolean isInside2d(float voxelValue, float max) {
    return (max > 0 && voxelValue >= max) || (max <= 0 && voxelValue <= max);
  }

  private float[] squareFractions;
  
  private int[] propagateNeighborPointIndexes2d(int x, int y, int[][] isoPointIndexes2d, float[][]squareFractions2d) {

    // propagates only the intersection point -- one in the case of a square

    int[] pixelPointIndexes = isoPointIndexes2d[y];
    squareFractions = squareFractions2d[y];

    boolean noXNeighbor = (x == squareCountX - 1);
    // the x neighbor is myself from my last pass through here
    if (noXNeighbor) {
      pixelPointIndexes[0] = -1;
      pixelPointIndexes[1] = -1;
      pixelPointIndexes[2] = -1;
      pixelPointIndexes[3] = -1;
    } else {
      pixelPointIndexes[1] = pixelPointIndexes[3];
      squareFractions[1] = 1f - squareFractions[3];
    }

    // from my y neighbor
    boolean noYNeighbor = (y == squareCountY - 1);
    if (noYNeighbor) {
      pixelPointIndexes[2] = -1;    
    } else {
      pixelPointIndexes[2] = isoPointIndexes2d[y + 1][0];
      squareFractions[2] = 1f - squareFractions2d[y + 1][0];
    }

    // these must always be calculated
    pixelPointIndexes[0] = -1;
    pixelPointIndexes[3] = -1;
    return pixelPointIndexes;
  }

  private void processOneQuadrilateral(int insideMask, float cutoff,
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
      if (is3DContour) //contouring f(x,y)
        calcVertexPoints3d(x, y, vertexA, vertexB);
      else
        calcVertexPoints2d(x, y, vertexA, vertexB);
      squareFractions[iEdge] = calcContourPoint(cutoff, valueA, valueB, contourPoints[iEdge]);
      //System.out.println("x y iEdge cutoff A B f " + x + " " + y + " " + iEdge + " :: " + cutoff + " " + valueA + " " + valueB + " " + squareFractions[iEdge]);
      pixelPointIndexes[iEdge] = meshData.addVertexCopy(contourPoints[iEdge], cutoff);
    }
    //this must be a square that is involved in this particular contour
    planarSquares[x * squareCountY + y].setIntersectionPoints(contourIndex,
        pixelPointIndexes, squareFractions);
  }

  private final Point3f pixelOrigin = new Point3f();

  private void calcVertexPoints2d(int x, int y, int vertexA, int vertexB) {
    pixelOrigin.scaleAdd(x, planarVectors[0], planarOrigin);
    pixelOrigin.scaleAdd(y, planarVectors[1], pixelOrigin);
    pointA.add(pixelOrigin, pixelVertexVectors[vertexA]);
    pointB.add(pixelOrigin, pixelVertexVectors[vertexB]);
  }

  private void calcVertexPoints3d(int x, int y, int vertexA, int vertexB) {
    contourLocateXYZ(x + squareVertexOffsets[vertexA].x, y
        + squareVertexOffsets[vertexA].y, pointA);
    contourLocateXYZ(x + squareVertexOffsets[vertexB].x, y
        + squareVertexOffsets[vertexB].y, pointB);
  }

  private void contourLocateXYZ(int ix, int iy, Point3f pt) {
    int i = findContourVertex(ix, iy);
    if (i < 0) {
      pt.x = Float.NaN;
      return;
    }
    ContourVertex c = contourVertexes[i];
    pt.set(c.vertexXYZ);
  }

  private int findContourVertex(int ix, int iy) {
    for (int i = 0; i < contourVertexCount; i++) {
      if (contourVertexes[i].pixelLocation[0] == ix
          && contourVertexes[i].pixelLocation[1] == iy)
        return i;
    }
    return -1;
  }

  private float calcContourPoint(float cutoff, float valueA, float valueB,
                         Point3f contourPoint) {

    float diff = valueB - valueA;
    float fraction = (cutoff - valueA) / diff;
    edgeVector.sub(pointB, pointA);
    contourPoint.scaleAdd(fraction, edgeVector, pointA);
    return fraction;
  }

  private Vector3f[] pixelVertexVectors = new Vector3f[4];

  private void calcPixelVertexVectors() {
    for (int i = 4; --i >= 0;)
      pixelVertexVectors[i] = calcPixelVertexVector(squareVertexVectors[i]);
  }

  private Vector3f calcPixelVertexVector(Vector3f squareVector) {
    Vector3f v = new Vector3f();
    planarMatrix.transform(squareVector, v);
    return v;
  }

  private void triangulateContours() {

    /*
     * Y
     *  3 ---c---- 2
     *  |          |           binar edgeMask is dcba 3210
     *  |          |           dcba: 1 is intersection
     *  d          b           3210: 1 is Inside
     *  |          |                 
     *  0 ----a--- 1  X
     *  
     *  for example:
     *  
     *         \
     *  3 ------c- 2
     *  |        \ |           edgeMask is 0110 0100
     *  |         \|           0110: intesection on b and c
     *  d          b           0100: only vertex 2 is inside
     *  |          |\_contour
     *  0 ----a--- 1  
     *  
     *  
     *  we need to go around the loop: 0 a 1 b 2 c 3 d 0
     *  to construct the polyhedron that will then be 
     *  turned into a set of triangles.
     *  
     *   But what if the square crosses TWO contours?
     *  
     *  for example:
     *  
     *         \
     *  3 ------c- 2
     *  |        \ |           mask(n)   is 1001 1001 1110
     * \|         \|           mask(n-1) is 0110 0110 0100
     *  d          b           
     *  |\         |\_contour n-1
     *  0 a------- 1  
     *     \_contour n
     *  
     *  In that case, what we do is load bits 8-11 with
     *  information about the other contour and XOR the two masks:
     *  
     *  mask(n)   & 0FF 0000 1001 1110
     *  mask(n-1) & F0F 0110 0000 0100
     *                  --------------
     *  XOR :           0110 1001 1010
     *  
     *  This says (1010) that vertices 0 and 2 are outside our contour
     *  and (1001) that our contour line intersects at a and d
     *  and (0110) that our inner contour line intersects at b and c
     *  
     * The problem is, we don't know the order of the two contours
     * if they both intersect the same edge.
     * 
     *         contour n-1
     *        \ \
     *  3 -----c-c 2
     *  |       \ \|               mask(n)   is 0110 0110 0100
     *  |        \ b(n-1)          mask(n-1) is 0110 0110 0100
     *  |         \|\          
     *  |          b(n)
     *  0 -------- 1\  
     *               contour n 
     *  
     * Half the time the algorithm will mess up if it has no
     * additional information, because it will flip the order 
     * of the edges on BOTH sides of the quadrilateral and so
     * not properly generate the listing of triangles. 
     * 
     * So, how to distinguish the above from the following?
     * 
     *         contour n
     *        \ \
     *  3 -----c-c 2
     *  |       \ \|               mask(n)   is 0110 0110 0100
     *  |        \ b(n)            mask(n-1) is 0110 0110 0100
     *  |         \|\          
     *  |          b(n-1)
     *  0 -------- 1\  
     *               contour n-1 
     *  
     *  The simple solution is "Don't!" Just draw the contour part
     *  twice, once each way. This is a reasonable alternative. 
     *  
     *  Or, we could check the fractional distance for b(n) and
     *  b(n-1). If f(n) < f(n-1), then we should start with n.
     *  
     */

    for (int contourIndex = 0; contourIndex < nContours; contourIndex++) {
      if (thisContour <= 0 || thisContour == contourIndex + 1)
        for (int squareIndex = 0; squareIndex < nSquares; squareIndex++) {

          /*
           * binary dcba dcba 3210 where dcba is edge intersection mask and
           * 3210 is the inside/outside mask for the vertices (0-15)
           */

          PlanarSquare square = planarSquares[squareIndex];
          int edgeMask0 = square.edgeMask12[contourIndex] & 0xFF;
          if (edgeMask0 == 0) // all outside
            continue;
          // unnecessary inside square drawn by different contour?
          // full square and also inside next inner contour
          if (edgeMask0 == 0xF && contourIndex > 0
              && square.edgeMask12[contourIndex - 1] == 0xF)
            continue;

          //still working here.... not efficient; stubbornly trying to avoid just
          // writing the damn triangle table.

      //    if (squareIndex != 281 && squareIndex != 303)
        //    continue;
          boolean isOK = true;
          int edgeMask = edgeMask0;
          if (contourIndex > 0) {
            edgeMask0 = square.edgeMask12[contourIndex - 1];
            if (edgeMask0 != 0) {
              int andMask = (edgeMask & edgeMask0 & 0xF0) >> 4;
              int orMask = ((edgeMask | edgeMask0) & 0xF0) >> 4;
//              System.out.println(squareIndex + " " + Integer.toBinaryString(edgeMask) + " " + Integer.toBinaryString(edgeMask0));
              if (andMask != 0) {
                //isOK = false;
                //at least one edge is common
                for (int i = 0; i < 4; i++)
                  if ((andMask & (1 << i)) != 0) {
                    //isOK = false;
  //                  System.out.println (square.fractions[contourIndex][i] +" " + square.fractions[contourIndex - 1][i]);
                    if (square.fractions[contourIndex][i] > square.fractions[contourIndex - 1][i]) {
                      isOK = false;
                    }
                    break;
                  } else if ((orMask & (1 << i)) != 0) {
                    break;
                  }
              }
              edgeMask ^= edgeMask0 & 0x0F0F;
            }
          }
          if (contourIndex > 0 && edgeMask == 0)
            continue;

    //      System.out.println("trianglate " + squareIndex + " " + Integer.toBinaryString(edgeMask0) + " " + Integer.toBinaryString(edgeMask));

          fillSquare(square, contourIndex, edgeMask, !isOK);
 //         if (!isOK) // a lazy hack instead of really figuring out the order
   //         fillSquare(square, contourIndex, edgeMask, true);
        }
    }
  }

  private final int[] triangleVertexList = new int[20];

  private void fillSquare(PlanarSquare square, int contourIndex, int edgeMask,
                  boolean reverseWinding) {
    int vPt = 0;
    boolean lowerFirst = reverseWinding;
    int mesh1 = -1, mesh2 = -1;
    for (int i = 0; i < 4; i++) {
      boolean newVertex = ((edgeMask & (1 << i)) != 0);
      boolean thisIntersect = ((edgeMask & (1 << (4 + i))) != 0);
      boolean lowerIntersect = ((edgeMask & (1 << (8 + i))) != 0);
      boolean lowerLast = false;
      
      //this vertex inside?
      
      if (newVertex) {
        triangleVertexList[vPt++] = square.vertexes[i];
      }

      //intersection of next lower contour on this edge?
      if (lowerFirst && lowerIntersect) {
        lowerLast = true;
        triangleVertexList[vPt++] = square.intersectionPoints[contourIndex - 1][i];
      }
      
      //intersection point of this contour on this edge?
      
      if (thisIntersect) {
        lowerLast = false;
        int pt = triangleVertexList[vPt++] = square.intersectionPoints[contourIndex][i];
        if (mesh1 < 0)
          mesh1 = pt;
        else
          mesh2 = pt;
      }
      
      //intersection of next lower contour on this edge?

      if (!lowerFirst && lowerIntersect) {
        lowerLast = true;
        triangleVertexList[vPt++] = square.intersectionPoints[contourIndex - 1][i];
      }
      if (lowerLast && newVertex)
        lowerFirst = true;
      if (thisIntersect && newVertex)
        lowerFirst = false;
      if (thisIntersect && !lowerLast)
        lowerFirst = false;
      if (thisIntersect && lowerLast)
        lowerFirst = true;
    }
    /*
     Logger.debug("\nfillSquare (" + square.x + " " + square.y + ") "
     + contourIndex + " " + binaryString(edgeMask) + "\n");
     Logger.debug("square vertexes:" + dumpIntArray(square.vertexes, 4));
     Logger.debug("square inters. pts:"
     + dumpIntArray(square.intersectionPoints[contourIndex], 4));
     Logger.debug(dumpIntArray(triangleVertexList, vPt));
     */
    createTriangleSet(vPt, mesh1, mesh2);
  }

  private void createTriangleSet(int nVertex, int mesh1, int mesh2) {
    int k = triangleVertexList[1];
    for (int i = 2; i < nVertex; i++) {
      int iA = triangleVertexList[0];
      int iB = k;
      int iC = triangleVertexList[i];
      int check = (iA == mesh1 && iB == mesh2 || iB == mesh1 && iA == mesh2 ? 1
          : iB == mesh1 && iC == mesh2 || iC == mesh1 && iB == mesh2 ? 2
              : iA == mesh1 && iC == mesh2 || iC == mesh1 && iA == mesh2 ? 4
                  : 0);
      meshData.addTriangleCheck(iA, iB, iC, check);
      k = triangleVertexList[i];
    }
  }
  
  private static void setupMatrix(Matrix3f mat, Vector3f[] cols) {
    for (int i = 0; i < 3; i++)
      mat.setColumn(i, cols[i]);
  }
  
  private void xyzToPixelVector(Vector3f vector) {
    //  factored for nonorthogonality; assumes vector is IN the plane already
    vector.sub(vector, planarOrigin);
    matXyzToPlane.transform(vector);
    vector.x /= planarVectorLengths[0];
    vector.y /= planarVectorLengths[1];
  }

  private Point3f pixelPtToXYZ(int x, int y) {
    Point3f ptXyz = new Point3f();
    ptXyz.scaleAdd(x, planarVectors[0], planarOrigin);
    ptXyz.scaleAdd(y, planarVectors[1], ptXyz);
    return ptXyz;
  }
  
  private final Point3i ptiTemp = new Point3i();

  private Point3i locatePixel(Point3f ptXyz) {
    pointVector.set(ptXyz);
    xyzToPixelVector(pointVector);
    ptiTemp.x = (int) (pointVector.x + 0.5f);
    //NOTE: fails if negative -- (int) (-0.9 + 0.5) = (int) (-0.4) = 0
    ptiTemp.y = (int) (pointVector.y + 0.5f);
    return ptiTemp;
  }
}
