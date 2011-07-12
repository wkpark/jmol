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

import org.jmol.util.Logger;

class IsoIntersectReader extends AtomDataReader {

  private static final int TYPE_FUNCTION = 0;
  private static final int TYPE_SUM = 1;
  private static final int TYPE_DIFF = 2;
  private static final int TYPE_MAX = 3;

  IsoIntersectReader(SurfaceGenerator sg) {
    super(sg);
  }

  ///// VDW intersection reader -- not mappable //////

  final private Point3f ptXyzTemp = new Point3f();
  private int[] voxelSource;

  @Override
  protected boolean readVolumeParameters(boolean isMapData) {
    setup(isMapData);
    if (isMapData)
      return false;
    initializeVolumetricData();
    return true;
  }

  private BitSet bsA, bsB;
  private Object[] func;
  private int funcType = TYPE_FUNCTION;
  
  @Override
  protected void setup(boolean isMapData) {
    super.setup(isMapData);
    params.fullyLit = true;
    point = params.point;
    bsA = params.intersection[0];
    bsB = params.intersection[1];
    //System.out.println("select " + Escape.escape(bsA));
    //System.out.println("select " + Escape.escape(bsB));
    BitSet bsSelected = new BitSet();
    bsSelected.or(bsA);
    bsSelected.or(bsB);
    doUseIterator = true; // just means we want a map
    getAtoms(bsSelected, doAddHydrogens, true, true, false, false, Float.NaN);
    setHeader("VDW intersection surface", params.calculationType);
    setRanges(params.solvent_ptsPerAngstrom, params.solvent_gridMax);
    margin = 5f;
    if (params.func instanceof String) {
      funcType = (params.func.equals("a-b") ? TYPE_DIFF : params.func.equals("a+b") ? TYPE_SUM : TYPE_MAX);  
    } else if (func == null || atomDataServer == null) {
      funcType = TYPE_DIFF;
    } else {
      func = (Object[]) params.func;
    }
      
  }

  //////////// meshData extensions ////////////

  @Override
  protected void generateCube() {
    // This is the starting point for the calculation.
    Logger.startTimer();
    volumeData.getYzCount();
    voxelSource = new int[volumeData.nPoints];
    params.vertexSource = new int[volumeData.nPoints]; // overkill?
    volumeData.voxelData = voxelData = new float[nPointsX][nPointsY][nPointsZ];
    resetVoxelData(Float.MAX_VALUE);
    markSphereVoxels(0, params.distance, bsA);
    voxelData = new float[nPointsX][nPointsY][nPointsZ];
    resetVoxelData(Float.MAX_VALUE);
    markSphereVoxels(0, params.distance, bsB);
    if (!setVoxels())
      volumeData.voxelData = new float[nPointsX][nPointsY][nPointsZ];
    voxelData = volumeData.voxelData;
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
    setVertexSource();
  }

  /////////////// calculation methods //////////////

  protected boolean setVoxels() {
    for (int i = 0; i < nPointsX; i++)
      for (int j = 0; j < nPointsY; j++)
        for (int k = 0; k < nPointsZ; k++) {
          float va = volumeData.voxelData[i][j][k];
          float vb = voxelData[i][j][k];
          float v = getValue(va, vb);
          if (Float.isNaN(v))
            return false;
          volumeData.voxelData[i][j][k] = v;
        }
    return true;
  }

  private final float[] values = new float[2];
  
  private float getValue(float va, float vb) {
    if (va == Float.MAX_VALUE || vb == Float.MAX_VALUE
        || Float.isNaN(va) || Float.isNaN(vb))
      return Float.MAX_VALUE;
    // looks crappy and serated because lens-like
    // surface slimmer than grid:
    //return (va < 0 && vb < 0 ? Math.max(va, vb) 
    //  : Math.min(Math.abs(va), Math.abs(vb)));
    switch (funcType) {
    case TYPE_SUM:
      return (va + vb);
    case TYPE_DIFF:
      return (va - vb);
    case TYPE_MAX:
      return (va>vb?va:vb);
    default:
      values[0] = va;
      values[1] = vb;
      return atomDataServer.evalFunctionFloat(func[0], func[1], values);
    }
  }
  
  @Override
  public float getValueAtPoint(Point3f pt) {
    // mapping sasurface/vdw 
    float va = getValueAtPoint(pt, bsA);
    float vb = getValueAtPoint(pt, bsB);
    return getValue(va, vb);
  }
  
  private float getValueAtPoint(Point3f pt, BitSet bs) {
    float value = Float.MAX_VALUE;
    for (int ia = bs.nextSetBit(0); ia >= 0; ia = bs.nextSetBit(ia + 1)) {
      int iAtom = myIndex[ia];
      float r = pt.distance(atomXyz[iAtom]) - atomRadius[iAtom];
      if (r < value)
        value = r;
    }
    return (value == Float.MAX_VALUE ? Float.NaN : value);
  }

  private final Point3f ptY0 = new Point3f();
  private final Point3f ptZ0 = new Point3f();
  private final Point3i pt0 = new Point3i();
  private final Point3i pt1 = new Point3i();

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
              voxelData[i][j][k] = value;
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


}
