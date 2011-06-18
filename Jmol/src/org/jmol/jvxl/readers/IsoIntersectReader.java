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

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jmol.util.ArrayUtil;
import org.jmol.util.Logger;

import org.jmol.jvxl.data.MeshData;

class IsoIntersectReader extends AtomDataReader {

  IsoIntersectReader(SurfaceGenerator sg) {
    super(sg);
  }

  ///// VDW intersection reader -- not mappable //////

  final private Point3f ptXyzTemp = new Point3f();
  private int[] voxelSource;

  @Override
  protected boolean readVolumeParameters(boolean isMapData) {
    setup();
    if (isMapData)
      return false;
    initializeVolumetricData();
    return true;
  }

  private BitSet bsA, bsB;
  private float margin;
  
  @Override
  protected void setup() {
    super.setup();
    point = params.point;
    bsA = params.intersection[0];
    bsB = params.intersection[1];
    BitSet bsSelected = new BitSet();
    bsSelected.or(bsA);
    bsSelected.or(bsB);
    doUseIterator = true; // just means we want a map
    getAtoms(Float.NaN, false, false, bsSelected);
    setHeader("VDW intersection surface", params.calculationType);
    setRangesAndAddAtoms(params.solvent_ptsPerAngstrom, params.solvent_gridMax,
        params.thePlane != null ? Integer.MAX_VALUE : Math.min(firstNearbyAtom,
            100));
    margin = 5f;//volumeData.maxGrid * 1.8f;
  }

  //////////// meshData extensions ////////////

  @Override
  protected void generateCube() {
    // This is the starting point for the calculation.
    Logger.startTimer();
    volumeData.getYzCount();
    volumeData.voxelSource = voxelSource = new int[volumeData.nPoints];
    generateSolventCube();
    unsetVoxelData();
    Logger.checkTimer("solvent surface time");
  }

  private int iAtomSurface;

  /* 
   * @param cutoff
   * @param isCutoffAbsolute
   * @param valueA
   * @param valueB
   * @param pointA
   * @param edgeVector
   * @param fReturn
   * @param ptReturn
   * @return fractional distance from A to B
   */

  @Override
  public int addVertexCopy(Point3f vertexXYZ, float value, int assocVertex) {
    int i = super.addVertexCopy(vertexXYZ, value, assocVertex);
    if (i < 0)
      return i;
    if (params.vertexSource != null)
      params.vertexSource[i] = iAtomSurface;
    return i;
  }

  @Override
  protected void postProcessVertices() {
    if (meshDataServer != null)
      meshDataServer.fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    if (params.vertexSource != null) {
      params.vertexSource = ArrayUtil.setLength(params.vertexSource,
          meshData.vertexCount);
      for (int i = 0; i < meshData.vertexCount; i++)
        params.vertexSource[i] = Math.abs(params.vertexSource[i]) - 1;
    }
  }

  /////////////// calculation methods //////////////

  private void generateSolventCube() {
    params.vertexSource = new int[volumeData.nPoints]; // overkill?
    volumeData.voxelData = voxelData = new float[nPointsX][nPointsY][nPointsZ];
    resetVoxelData(Float.MAX_VALUE);
    markSphereVoxels(0, params.distance, bsA);
    voxelData = new float[nPointsX][nPointsY][nPointsZ];
    resetVoxelData(Float.MAX_VALUE);
    markSphereVoxels(0, params.distance, bsB);
    if (!setVoxels()) { 
      volumeData.voxelData = new float[nPointsX][nPointsY][nPointsZ];      
    }
  }
  
  protected boolean setVoxels() {
    for (int i = 0; i < nPointsX; i++)
      for (int j = 0; j < nPointsY; j++)
        for (int k = 0; k < nPointsX; k++) {
          float va = volumeData.voxelData[i][j][k];
          float vb = voxelData[i][j][k];
          float v = getValue(va, vb);
          if (Float.isNaN(v))
            return false;
          volumeData.voxelData[i][j][k] = v;
        }
    return true;
  }

  private float[] values = new float[2];
  
  private float getValue(float va, float vb) {
    if (va == Float.MAX_VALUE || vb == Float.MAX_VALUE
        || Float.isNaN(va) || Float.isNaN(vb))
      return Float.MAX_VALUE;
    // looks crappy and serated because lens-like
    // surface slimmer than grid:
    //return (va < 0 && vb < 0 ? Math.max(va, vb) 
    //  : Math.min(Math.abs(va), Math.abs(vb)));
    if (params.func == null || atomDataServer == null)
      return (va - vb);
    values[0] = va;
    values[1] = vb;
    float v = atomDataServer.evalFunctionFloat(params.func[0], params.func[1], values);
    return v;
  }
  
  private final Point3f ptY0 = new Point3f();
  private final Point3f ptZ0 = new Point3f();
  private final Point3i pt0 = new Point3i();
  private final Point3i pt1 = new Point3i();

  private void setVoxel(int i, int j, int k, float value) {
    voxelData[i][j][k] = value;
  }

  private void markSphereVoxels(float r0, float distance, BitSet bs) {
    boolean isWithin = (distance != Float.MAX_VALUE && point != null);
    for (int ia = bs.nextSetBit(0); ia >= 0; ia = bs.nextSetBit(ia + 1)) {
      int iAtom = myIndex[ia];
      Point3f ptA = atomXyz[iAtom];
      float rA = atomRadius[iAtom];
      if (isWithin && ptA.distance(point) > distance + rA + 0.5)
        continue;
      float rA0 = rA + r0;
      setGridLimitsForAtom(ptA, rA0, pt0, pt1);
      volumeData.voxelPtToXYZ(pt0.x, pt0.y, pt0.z, ptXyzTemp);
      for (int i = pt0.x; i < pt1.x; i++, ptXyzTemp.scaleAdd(1,
          volumetricVectors[0], ptY0)) {
        ptY0.set(ptXyzTemp);
        for (int j = pt0.y; j < pt1.y; j++, ptXyzTemp.scaleAdd(1,
            volumetricVectors[1], ptZ0)) {
          ptZ0.set(ptXyzTemp);
          for (int k = pt0.z; k < pt1.z; k++, ptXyzTemp
              .add(volumetricVectors[2])) {
            float value = ptXyzTemp.distance(ptA) - rA;
            if ((r0 == 0 || value <= rA0) && value < voxelData[i][j][k]) {
              setVoxel(i, j, k, value);
              if (!Float.isNaN(value)) {
                int ipt = volumeData.getPointIndex(i, j, k);
                voxelSource[ipt] = ia + 1;
              }
            }
          }
        }
      }
    }
  }

  void resetVoxelData(float value) {
    for (int x = 0; x < nPointsX; ++x)
      for (int y = 0; y < nPointsY; ++y)
        for (int z = 0; z < nPointsZ; ++z)
          voxelData[x][y][z] = value;
  }

  void unsetVoxelData() {
    for (int x = 0; x < nPointsX; ++x)
      for (int y = 0; y < nPointsY; ++y)
        for (int z = 0; z < nPointsZ; ++z)
          if (voxelData[x][y][z] == Float.MAX_VALUE)
            voxelData[x][y][z] = Float.NaN;
  }

  private void setGridLimitsForAtom(Point3f ptA, float rA, Point3i pt0,
                                    Point3i pt1) {
    rA += margin; // to span corner-to-corner possibility
    volumeData.xyzToVoxelPt(ptA.x - rA, ptA.y - rA, ptA.z - rA, pt0);
    pt0.x = Math.max(pt0.x - 1, 0);
    pt0.y = Math.max(pt0.y - 1, 0);
    pt0.z = Math.max(pt0.z - 1, 0);
    volumeData.xyzToVoxelPt(ptA.x + rA, ptA.y + rA, ptA.z + rA, pt1);
    pt1.x = Math.min(pt1.x + 1, nPointsX);
    pt1.y = Math.min(pt1.y + 1, nPointsY);
    pt1.z = Math.min(pt1.z + 1, nPointsZ);
  }

  ///////////////// debugging ////////////////
  
}
