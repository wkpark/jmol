/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:09:49 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7221 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.jvxl.simplewriter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ASimpleJvxlWriter {

  // example for how to create simple JVXL files from cube data
  // no color mapping, no planes, just simple surfaces.
  
  public static void main(String[] args) {


    // parameters that need setting:
    
    String outputFile = "c:/temp/simple.jvxl";
    float cutoff = 0.01f;
    boolean isCutoffAbsolute = false;
    int nX = 30;
    int nY = 30;
    int nZ = 30;
    String title = "created by SimpleJvxlWriter "
      + new SimpleDateFormat("yyyy-MM-dd', 'HH:mm").format(new Date()) 
      + "\naddional comment line\n";

    VolumeData volumeData;
    VoxelDataCreator vdc;
    JvxlData jvxlData;

    volumeData = new VolumeData();
    volumeData.setVolumetricOrigin(0, 0, 0);
    volumeData.setVolumetricVector(0, 1f, 0f, 0f);
    volumeData.setVolumetricVector(1, 0f, 1f, 0f);
    volumeData.setVolumetricVector(2, 0f, 0f, 1f);
    volumeData.setUnitVectors();


    volumeData.setVoxelCounts(nX, nY, nZ);
    volumeData.setVoxelData(new float[nX][nY][nZ]);
    vdc = new VoxelDataCreator(volumeData);
    vdc.createVoxelData();
    jvxlData = new JvxlData();
    jvxlData.cutoff = cutoff;
    jvxlData.isCutoffAbsolute = isCutoffAbsolute;
    jvxlData.isXLowToHigh = true;
    writeFile(outputFile + jvxlData.isXLowToHigh  , JvxlWrite.jvxlGetData(null, jvxlData, volumeData, title));
    
/*
    volumeData = new VolumeData();
    volumeData.setVolumetricOrigin(2, 2, 2);
    volumeData.setVolumetricVector(0, 1f, 0f, 0f);
    volumeData.setVolumetricVector(1, 0f, 1f, 0f);
    volumeData.setVolumetricVector(2, 0f, 0f, 1f);
    volumeData.setUnitVectors();


    volumeData.setVoxelCounts(nX, nY, nZ);
    volumeData.setVoxelData(new float[nX][nY][nZ]);
    vdc = new VoxelDataCreator(volumeData);
    vdc.createVoxelData();
    jvxlData = new JvxlData();
    jvxlData.cutoff = cutoff;
    jvxlData.isCutoffAbsolute = isCutoffAbsolute;
    jvxlData.isXLowToHigh = true;
    writeFile(outputFile+ "B", JvxlWrite.jvxlGetData(vdc, jvxlData, volumeData, title));
    
    */
    System.out.flush();
    System.exit(0);
  }

  static void writeFile(String fileName, String text) {
    try {
      FileOutputStream os = new FileOutputStream(fileName);
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os), 8192);
      bw.write(text);
      bw.close();
      os = null;
    } catch (IOException e) {
      System.out.println("IO Exception: " + e.toString());
    }
  }
  
}