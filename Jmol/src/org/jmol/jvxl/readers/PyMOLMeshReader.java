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


import java.io.BufferedReader;
import java.util.Hashtable;

import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.SB;


class PyMOLMeshReader extends MapFileReader {

  private Hashtable<String, JmolList<Object>> map;
  private JmolList<Object> data;
  private JmolList<Object> surfaceList;
  private JmolList<Object> voxelList;
  private String surfaceName;
  private int pymolType;
  private boolean isMesh;
  private float cutoff = Float.NaN;

  /*
   * PyMOL surface/mesh reader. 
   * 
   */

  
  PyMOLMeshReader(){}
  
  @SuppressWarnings("unchecked")
  @Override
  void init2(SurfaceGenerator sg, BufferedReader brNull) {
    super.init2(sg, null);
    map = (Hashtable<String, JmolList<Object>>) sg.getReaderData();    
    nSurfaces = 1;
    data = map.get(params.calculationType);
    if (data == null)
      return;
    pymolType = (int) getFloat(getList(data, 0), 0);
    isMesh = (pymolType == 3);
    if (isMesh) {
      surfaceName = (String) data.get(data.size() - 1);
      surfaceList = getList(getList(map.get(surfaceName), 2), 0);
      if (params.thePlane == null && params.cutoffAutomatic) {
        params.cutoff = getFloat(getList(getList(data, 2), 0), 8);
        // within range = getFloat(getList(getList(data, 2), 0), 11);
        // within list = getList(getList(data, 2), 0), 12);
        params.cutoffAutomatic = false;
      }
    } else {
      surfaceList = data;
      surfaceName = (String) data.get(data.size() - 1);
    }
    voxelList = getList(getList(getList(surfaceList, 14), 2), 6);
    System.out.println("Number of grid points = " + voxelList.size());
  }

  @SuppressWarnings("unchecked")
  private static JmolList<Object> getList(JmolList<Object> list, int i) {
    return (JmolList<Object>) list.get(i);
  }

  @Override
  protected void readParameters() throws Exception {
    
    JmolList<Object> t;

    jvxlFileHeaderBuffer = new SB();
    jvxlFileHeaderBuffer.append("PyMOL surface reader\n");
    jvxlFileHeaderBuffer.append(surfaceName + " (" + params.calculationType + ")\n");

    // cell parameters
    t = getList(getList(surfaceList, 1), 0);
    a = getFloat(t, 0);
    b = getFloat(t, 1);
    c = getFloat(t, 2);
    t = getList(getList(surfaceList, 1), 1);
    alpha = getFloat(t, 0);
    beta = getFloat(t, 1);
    gamma = getFloat(t, 2);
    
    // origin
    t = getList(surfaceList, 7);    
    origin.set(getFloat(t, 0), getFloat(t, 1), getFloat(t, 2));
    
    // unit cell vectors in grid counts
    t = getList(surfaceList, 10);
    na = (int) getFloat(t, 0);
    nb = (int) getFloat(t, 1);
    nc = (int) getFloat(t, 2);
    
    // data block start and extents in grid units
    t = getList(surfaceList, 11);
    nxyzStart[0] = (int) getFloat(t, 0);
    nxyzStart[1] = (int) getFloat(t, 1);
    nxyzStart[2] = (int) getFloat(t, 2);
    
    // number of grid points
    // will end up with xyz, but we use zyx because of the storage
    t = getList(surfaceList, 13);
    nz = (int) getFloat(t, 0);
    ny = (int) getFloat(t, 1);
    nx = (int) getFloat(t, 2);
    
    mapc = 3; // fastest
    mapr = 2;
    maps = 1; // slowest
    
    getVectorsAndOrigin();
    setCutoffAutomatic();

  }
   
  private int pt;
  @Override
  protected float nextVoxel() throws Exception {
    return getFloat(voxelList, pt++);
  }

  private float getFloat(JmolList<Object> list, int i) {
    return ((Number) list.get(i)).floatValue();
  }

  @Override
  protected void skipData(int nPoints) throws Exception {
  }
  
  @Override
  protected void setCutoffAutomatic() {
    if (params.thePlane == null && params.cutoffAutomatic) {
      params.cutoff = (boundingBox == null ? 3.0f : 1.6f);
      if (dmin != Float.MAX_VALUE) {
        if (params.cutoff > dmax)
          params.cutoff = dmax / 4; // just a guess
      }
      Logger.info("MapReader: setting cutoff to default value of "
          + params.cutoff
          + (boundingBox == null ? " (no BOUNDBOX parameter)\n" : "\n"));
    }
  }

}
