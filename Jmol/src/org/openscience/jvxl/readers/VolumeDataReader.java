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

class VolumeDataReader extends VoxelReader {

  VolumeDataReader(SurfaceGenerator sg) {
    super(sg);
    setJvxlInfo();
  }

  void setJvxlInfo() {
    jvxlFileHeaderBuffer = new StringBuffer();
    jvxlFileHeaderBuffer.append("JVXL Volume Data\n================\n");
    jvxlFileHeaderBuffer.append("-2 ").append('\n');
  }
  
  // reads a pre-calculated volume data set
  
  void readVoxelData(boolean isMapData) throws Exception {
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

    voxelData = volumeData.voxelData;
    if (isMapData || params.thePlane != null)
      return;
    nDataPoints = 0;
    float cutoff = params.cutoff;
    boolean isCutoffAbsolute = params.isCutoffAbsolute;
    for (int x = 0; x < nPointsX; ++x) {
      float[][] plane;
      plane = voxelData[x];
      for (int y = 0; y < nPointsY; ++y) {
        float[] strip;
        strip = plane[y];
        for (int z = 0; z < nPointsZ; ++z) {
          float voxelValue;
          voxelValue = strip[z]; //precalculated
          ++nDataPoints;
          if (inside == isInside(voxelValue, cutoff, isCutoffAbsolute)) {
            dataCount++;
          } else {
            if (dataCount != 0)
              surfaceData += " " + dataCount;
            dataCount = 1;
            inside = !inside;
          }
        }
      }
    }
    surfaceData += " " + dataCount + "\n";
    jvxlData.jvxlSurfaceData = surfaceData;
    jvxlData.jvxlPlane = params.thePlane;
    volumeData.setVoxelData(voxelData);
  }
  
  float getVoxelValue(int x, int y, int z) {
    return volumeData.voxelData[x][y][z];
  }

  
}


