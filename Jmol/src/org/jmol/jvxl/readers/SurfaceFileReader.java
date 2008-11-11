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
import org.jmol.util.Parser;
import org.jmol.util.Logger;

abstract class SurfaceFileReader extends SurfaceReader {

  protected BufferedReader br;
  protected int dataIndex;
 
  SurfaceFileReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg);
    this.br = br;
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
     
  void readVolumeParameters() {
    // required by SurfaceReader
    return;
  }
  
  void readVolumeData(boolean isMapData) {
    // required by SurfaceReader
  }

  protected void readSurfaceData(boolean isMapData) throws Exception {
    getSurfaceData();
    // required by SurfaceReader
  }

  protected void gotoData(int n, int nPoints) throws Exception {
    dataIndex = n;
  }
  
  abstract void getSurfaceData() throws Exception;

  ///////////file reading //////////
  
  String line;
  int[] next = new int[1];

  String[] getTokens() {
    return Parser.getTokens(line, 0);
  }

  float parseFloat() {
    return Parser.parseFloat(line, next);
  }

  int parseInt() {
    return Parser.parseInt(line, next);
  }
  
  /*  
  float parseFloat(String s) {
    next[0] = 0;
    return Parser.parseFloat(s, next);
  }

  float parseFloatNext(String s) {
    return Parser.parseFloat(s, next);
  }

  int parseInt(String s) {
    next[0] = 0;
    return Parser.parseInt(s, next);
  }
  
  int parseIntNext(String s) {
    return Parser.parseInt(s, next);
  }
  
  int parseInt(String s, int iStart) {
    next[0] = iStart;
    return Parser.parseInt(s, next);
  }
  
  */
}
