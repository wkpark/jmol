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

import org.jmol.util.Parser;

class ApbsReader extends VolumeFileReader {

  ApbsReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
    jvxlData.wasCubic = isApbsDx = true;
  }
  
  protected void readTitleLines() throws Exception {
    jvxlFileHeaderBuffer = new StringBuffer();
    skipComments(true);
    while (line != null && line.length() == 0)
      br.readLine();
    jvxlFileHeaderBuffer.append("APBS OpenDx DATA ").append(line).append("\n");
    jvxlFileHeaderBuffer.append("see http://apbs.sourceforge.net\n");
    isAngstroms = true;
  }
  
  protected void readAtomCountAndOrigin() throws Exception {
    String atomLine = br.readLine();
    String[] tokens = Parser.getTokens(atomLine, 0);
    negativeAtomCount = false;
    atomCount = 0;
    if (tokens.length >= 4)
      volumetricOrigin.set(parseFloat(tokens[1]), parseFloat(tokens[2]),
          parseFloat(tokens[3]));
    JvxlReader.jvxlCheckAtomLine(isXLowToHigh, isAngstroms, tokens[0],
        atomLine, jvxlFileHeaderBuffer);
  }

  protected void readVoxelVector(int voxelVectorIndex) throws Exception {
    super.readVoxelVector(voxelVectorIndex);
    if (voxelVectorIndex == 2) {
      line = br.readLine();
      String[] tokens = getTokens();
      /* see http://apbs.sourceforge.net/doc/user-guide/index.html#opendx-format
       object 2 class gridconnections counts nx ny nz
       object 3 class array type double rank 0 times n data follows
       * 
       */
      for (int i = 0; i < 3; i++)
        voxelCounts[i] = parseInt(tokens[i + 5]);
      br.readLine();
    }
  }
}
