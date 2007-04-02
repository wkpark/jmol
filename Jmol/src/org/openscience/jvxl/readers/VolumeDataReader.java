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
    jvxlFileHeaderBuffer = new StringBuffer();
    JvxlReader.jvxlCreateHeader("JVXL Volume Data","================", volumeData, jvxlFileHeaderBuffer);
  }

  // UNTESTED -- reads a pre-calculated volume data set -- UNTESTED

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
    voxelData = volumeData.voxelData;
    if (isMapData)
      return;

    StringBuffer sb = new StringBuffer();
    boolean inside = false;
    int dataCount = 0;
    nDataPoints = 0;
    int nSurfaceInts = 0;
    float cutoff = params.cutoff;
    boolean isCutoffAbsolute = params.isCutoffAbsolute;
    for (int x = 0; x < nPointsX; ++x)
      for (int y = 0; y < nPointsY; ++y)
        for (int z = 0; z < nPointsZ; ++z) {
          ++nDataPoints;
          if (inside == isInside(voxelData[x][y][z], cutoff, isCutoffAbsolute)) {
            dataCount++;
          } else {
            if (dataCount != 0) {
              sb.append(' ').append(dataCount);
              ++nSurfaceInts;
            }
            dataCount = 1;
            inside = !inside;
          }
        }
    sb.append(' ').append(dataCount).append('\n');
    ++nSurfaceInts;
    JvxlReader.setSurfaceInfo(jvxlData, params.thePlane, nSurfaceInts, sb);
    volumeData.setVoxelData(voxelData);
  }  
}
