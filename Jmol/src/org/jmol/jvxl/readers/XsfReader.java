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

import javax.vecmath.Vector3f;

import org.jmol.util.Logger;
import org.jmol.util.Parser;

class XsfReader extends VolumeFileReader {

  XsfReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
  }
  
  @Override
  protected void readParameters() throws Exception {
    if (Float.isNaN(params.cutoff))
      params.cutoff = 0.05f;
    isAngstroms = false;
    jvxlFileHeaderBuffer = new StringBuffer();
    while (readLine() != null && line.indexOf("BEGIN_DATAGRID") < 0)
      continue;
    jvxlFileHeaderBuffer.append("XsfReader file\n");
    readLine();
    voxelCounts[0] = parseInt(line);
    voxelCounts[1] = parseInt();
    voxelCounts[2] = parseInt();
    volumetricOrigin.set(parseFloat(readLine()), parseFloat(), parseFloat());
    volumetricOrigin.scale(ANGSTROMS_PER_BOHR);
    for (int i = 0; i < 3; ++i)
      volumetricVectors[i].set(parseFloat(readLine()), parseFloat(), parseFloat());
    nSurfaces = 1;
  }
}


