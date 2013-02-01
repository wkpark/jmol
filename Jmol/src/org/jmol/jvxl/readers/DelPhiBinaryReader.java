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

import org.jmol.util.Logger;
import org.jmol.util.StringXBuilder;
import org.jmol.util.Vector3f;

class DelPhiBinaryReader extends VolumeFileReader {

  /*
   * also referred to as CCP4 format
   * 
   * examples include the emd_1xxx..map electron microscopy files
   * and xxxx.ccp4 files
   * 
   * Jmol 12.1.33: adjusted to allow for nonstandard "MRC" files
   * that are little-endian, have no labels, have no "MAP" file type marker
   * and are not inside-out.
   *
   */

  
  DelPhiBinaryReader() {}
  
  /**
   * @param sg 
   */
  @Override
  void init2(SurfaceGenerator sg, BufferedReader brNull) {
    String fileName = (String) sg.getReaderData();
    super.init2(sg, null);
    binarydoc = newBinaryDocument();
    binarydoc.setStream(sg.getAtomDataServer().getBufferedInputStream(fileName), false);
    // data are HIGH on the inside and LOW on the outside
    nSurfaces = 1; 
    if (params.thePlane == null)
      params.insideOut = !params.insideOut;
    allowSigma = false;
  }
  

    /* presumed only:
     * 
     * <14><00><00><00> [14 bytes of data]] <14><00><00><00><00>
     * <len> [title] <len>
     * 
    */
    
  private float[] data;
  
  @Override
  protected void readParameters() throws Exception {
    System.out.println(readString());
    String title = readString();
    data = readFloatArray();
    System.out.println("DelPhi data length: " + data.length);
    System.out.println(readString());
    binarydoc.readInt();
    float dx = 1/binarydoc.readFloat();
    volumetricVectors[0] = Vector3f.new3(0, 0, dx);
    volumetricVectors[1] = Vector3f.new3(0, dx, 0);
    volumetricVectors[2] = Vector3f.new3(dx, 0, 0);
    Logger.info("DelPhi resolution: " + dx);
    
    float x = binarydoc.readFloat();
    float y = binarydoc.readFloat();
    float z = binarydoc.readFloat();
    //  volumetricOrigin.set(-x/2, -y/2, -z/2);
    Logger.info("DelPhi origin: " + volumetricOrigin);
    
    int nx = binarydoc.readInt();
    voxelCounts[0] = voxelCounts[1] = voxelCounts[2] = nx;
    Logger.info("DelPhi voxel counts: " + nx);
    
    jvxlFileHeaderBuffer = new StringXBuilder();
    jvxlFileHeaderBuffer.append("DelPhi DATA ").append(title.replace('\n', ' ').trim()).append("\n\n");
  }
  
  private float[] readFloatArray() throws Exception {
    int n = binarydoc.readInt()/4;
    float[] a = new float[n];
    for (int i = 0; i < n; i++)
      a[i] = binarydoc.readFloat();
    binarydoc.readInt();
    return a;
  }

  private String readString() throws Exception {
    int n = binarydoc.readInt();
    byte[] buf = new byte[n];
    binarydoc.readByteArray(buf, 0, n);
    binarydoc.readInt();
    return new String(buf);
  }

  int pt;
  
  @Override
  protected float nextVoxel() throws Exception {
    nBytes += 4;
    return data[pt++];
  }

  @Override
  protected void skipData(int nPoints) throws Exception {
    pt += nPoints;
  }
}
