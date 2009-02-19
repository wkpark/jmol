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

abstract class PolygonFileReader extends SurfaceFileReader {

  protected int nVertices;
  protected int nTriangles;

  PolygonFileReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
    vertexDataOnly = true;
  }

  void discardTempData(boolean discardAll) {
    try {
      if (br != null)
        br.close();
    } catch (Exception e) {
    }
    super.discardTempData(discardAll);
  }
     
  boolean readVolumeParameters() {
    // required by SurfaceReader
    return true;
  }
  
  boolean readVolumeData(boolean isMapData) {
    // required by SurfaceReader
    return true;
  }

  protected void readSurfaceData(boolean isMapData) throws Exception {
    getSurfaceData();
    // required by SurfaceReader
  }

  abstract void getSurfaceData() throws Exception;
}
