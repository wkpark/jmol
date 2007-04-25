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
import javax.vecmath.Matrix3f;

import org.jmol.util.Logger;

class VolumeDataReader extends VoxelReader {

  /*        (requires AtomDataServer)
   *                |-- IsoSolventReader
   *                |-- IsoMOReader, IsoMepReader
   *                |-- IsoPlaneReader
   *                |
   *            AtomDataReader (abstract)
   *                |
   *                |         |-- IsoFxyReader (not precalculated)
   *                |         |-- IsoShapeReader (not precalculated)  
   *                |         |         
   *            VolumeDataReader (precalculated data)       
   *                   |
   *                VoxelReader
   * 
   * 
   */
  
  protected int dataType;
  protected boolean precalculateVoxelData;
  protected boolean allowMapData;
  protected Point3f center, point;
  protected float[] anisotropy;
  protected boolean isAnisotropic;
  protected Matrix3f eccentricityMatrix;
  protected Matrix3f eccentricityMatrixInverse;
  protected boolean isEccentric;
  protected float eccentricityScale;
  protected float eccentricityRatio;


  VolumeDataReader(SurfaceGenerator sg) {
    super(sg);
    dataType = params.dataType;
    precalculateVoxelData = true;
    allowMapData = true;    
    center = params.center;
    anisotropy = params.anisotropy;
    isAnisotropic = params.isAnisotropic;
    
    eccentricityMatrix = params.eccentricityMatrix;
    eccentricityMatrixInverse = params.eccentricityMatrixInverse;
    isEccentric = params.isEccentric;
    eccentricityScale = params.eccentricityScale;
    eccentricityRatio = params.eccentricityRatio;
  }
  
  void setup() {
    //as is, just the volumeData as we have it.
    //but subclasses can modify this behavior.
    jvxlFileHeaderBuffer = new StringBuffer("volume data read from file\n\n");
    JvxlReader.jvxlCreateHeaderWithoutTitleOrAtoms(volumeData, jvxlFileHeaderBuffer);
  }
  
  void readVolumeParameters() {
    setup();
    initializeVolumetricData();
  }

  void readVolumeData(boolean isMapData) {
    try {
      readVoxelData(isMapData);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
  }

  protected void readVoxelDataIndividually(boolean isMapData) throws Exception {
    if (isMapData && !allowMapData)
      return; //not applicable
    boolean inside = false;
    int dataCount = 0;
    voxelData = new float[nPointsX][nPointsY][nPointsZ];
    nDataPoints = 0;
    int nSurfaceInts = 0;
    StringBuffer sb = new StringBuffer();
    float cutoff = params.cutoff;
    boolean isCutoffAbsolute = params.isCutoffAbsolute;
    for (int x = 0; x < nPointsX; ++x) {
      float[][] plane = new float[nPointsY][];
      voxelData[x] = plane;
      for (int y = 0; y < nPointsY; ++y) {
        float[] strip = plane[y] = new float[nPointsZ];
        for (int z = 0; z < nPointsZ; ++z) {
          float voxelValue = strip[z] = getValue(x, y, z);
          ++nDataPoints;
          if (inside == isInside(voxelValue, cutoff, isCutoffAbsolute)) {
            dataCount++;
          } else {
            if (!isMapData)
              sb.append(' ').append(dataCount);
            ++nSurfaceInts;
            dataCount = 1;
            inside = !inside;
          }
        }
      }
    }
    //Jvxl getNextVoxelValue records the data read on its own.
    if (!isMapData) {
      sb.append(' ').append(dataCount).append('\n');
      JvxlReader.setSurfaceInfo(jvxlData, params.thePlane, nSurfaceInts, sb);
    }
    volumeData.setVoxelData(voxelData);
  }
  
  protected float getValue(int x, int y, int z) {
    return 0;
  }

  protected int setVoxelRange(int index, float min, float max, float ptsPerAngstrom,
                    int gridMax) {
    if (min >= max) {
      min = -10;
      max = 10;
    }
    float range = max - min;
    int nGrid;
    float resolution = params.resolution;
    if (resolution != Float.MAX_VALUE) {
      ptsPerAngstrom = resolution;
      nGrid = (int) (range * ptsPerAngstrom);
    } else {
      nGrid = (int) (range * ptsPerAngstrom);
    }
    if (nGrid > gridMax) {
      if ((dataType & Parameters.HAS_MAXGRID) > 0) {
        if (resolution != Float.MAX_VALUE)
          Logger.info("Maximum number of voxels for index=" + index);
        nGrid = gridMax;
      } else if (resolution == Float.MAX_VALUE) {
        nGrid = gridMax;
      }
    }
    ptsPerAngstrom = nGrid / range;
    float d = volumeData.volumetricVectorLengths[index] = 1f / ptsPerAngstrom;
    voxelCounts[index] = nGrid + ((dataType & Parameters.IS_SOLVENTTYPE) != 0 ? 3 : 0);

    switch (index) {
    case 0:
      volumetricVectors[0].set(d, 0, 0);
      volumetricOrigin.x = min;
      break;
    case 1:
      volumetricVectors[1].set(0, d, 0);
      volumetricOrigin.y = min;
      break;
    case 2:
      volumetricVectors[2].set(0, 0, d);
      volumetricOrigin.z = min;
      if (isEccentric)
        eccentricityMatrix.transform(volumetricOrigin);
      if (center.x != Float.MAX_VALUE)
        volumetricOrigin.add(center);
    }
    if (isEccentric)
      eccentricityMatrix.transform(volumetricVectors[index]);
    return voxelCounts[index];
  }

  protected void readVoxelData(boolean isMapData) throws Exception {
    //precalculated -- just creating the JVXL equivalent
    if (!precalculateVoxelData) {
      readVoxelDataIndividually(isMapData);
      return;
    }
    if (dataType != Parameters.SURFACE_INFO) {
      volumeData.voxelData = voxelData = new float[nPointsX][nPointsY][nPointsZ];
      generateCube();
    }
    if (isMapData)
      return;
    nDataPoints = JvxlReader.jvxlCreateSurfaceData(jvxlData, volumeData.voxelData, params.cutoff, params.isCutoffAbsolute, nPointsX, nPointsY, nPointsZ);
  }
  
  protected void generateCube() {
    //generic VolumeData reader does not require this; others do.
  }  
 }
