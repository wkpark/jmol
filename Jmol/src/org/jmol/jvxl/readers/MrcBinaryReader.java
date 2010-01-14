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

import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.util.BinaryDocument;
import org.jmol.util.Logger;

class MrcBinaryReader extends VolumeFileReader {

  MrcHeader mrcHeader;
  MrcBinaryReader(SurfaceGenerator sg, String fileName, boolean isBigEndian) {
    super(sg, null);
    binarydoc = new BinaryDocument();
    binarydoc.setStream(sg.getAtomDataServer().getBufferedInputStream(fileName), isBigEndian);
    mrcHeader = new MrcHeader();
    // data are HIGH on the inside and LOW on the outside
    params.insideOut = !params.insideOut;
  }
  
  private class MrcHeader {
    /* see http://ami.scripps.edu/software/mrctools/mrc_specification.php
     * 
    1 NX       number of columns (fastest changing in map)
    2 NY       number of rows   
    3 NZ       number of sections (slowest changing in map)
    4 MODE     data type :
         0        image : signed 8-bit bytes range -128 to 127
         1        image : 16-bit halfwords
         2        image : 32-bit reals
         3        transform : complex 16-bit integers
         4        transform : complex 32-bit reals
         6        image : unsigned 16-bit range 0 to 65535
    5 NXSTART number of first column in map (Default = 0)
    6 NYSTART number of first row in map
    7 NZSTART number of first section in map
    8 MX       number of intervals along X
    9 MY       number of intervals along Y
    10  MZ       number of intervals along Z
    11-13 CELLA    cell dimensions in angstroms
    14-16 CELLB    cell angles in degrees
    17  MAPC     axis corresp to cols (1,2,3 for X,Y,Z)
    18  MAPR     axis corresp to rows (1,2,3 for X,Y,Z)
    19  MAPS     axis corresp to sections (1,2,3 for X,Y,Z)
    20  DMIN     minimum density value
    21  DMAX     maximum density value
    22  DMEAN    mean density value
    23  ISPG     space group number 0 or 1 (default=0)
    24  NSYMBT   number of bytes used for symmetry data (0 or 80)
    25-49 EXTRA    extra space used for anything   - 0 by default
    50-52 ORIGIN   origin in X,Y,Z used for transforms
    53  MAP      character string 'MAP ' to identify file type
    54  MACHST   machine stamp
    55  RMS      rms deviation of map from mean density
    56  NLABL    number of labels being used
    57-256  LABEL(20,10) 10 80-character text labels
    */
    
    int nx, ny, nz, mode, nxStart, nyStart, nzStart, mx, my, mz;
    float a, b, c, alpha, beta, gamma;
    int mapc, mapr, maps;
    float hdmin, hdmax, hdmean;
    int ispg;
    int nsymbt;
    byte[] extra = new byte[100];
    float originX, originY, originZ;
    byte[] map = new byte[4];
    byte[] machst = new byte[4];
    float rms;
    int nlabel;
    String[] labels = new String[10];
    SymmetryInterface unitCell;
    
    MrcHeader() {
      try {
        nx = binarydoc.readInt();
        ny = binarydoc.readInt();
        nz = binarydoc.readInt();
        
        Logger.info("MRC header: nx,ny,nz: " + nx + "," + ny + "," + nz);

        mode = binarydoc.readInt();
        Logger.info("MRC header: mode: " +mode);

        nxStart = binarydoc.readInt();
        nyStart = binarydoc.readInt();
        nzStart = binarydoc.readInt();
        
        Logger.info("MRC header: nxStart,nyStart,nzStart: " + nxStart + "," + nyStart + "," + nzStart);

        mx = binarydoc.readInt();
        my = binarydoc.readInt();
        mz = binarydoc.readInt();

        Logger.info("MRC header: mx,my,mz: " + mx + "," + my + "," + mz);

        a = binarydoc.readFloat();
        b = binarydoc.readFloat();
        c = binarydoc.readFloat();
        alpha = binarydoc.readFloat();
        beta = binarydoc.readFloat();
        gamma = binarydoc.readFloat();
        
        Logger.info("MRC header: a,b,c,alpha,beta,gamma: " + a + "," + b + "," + c + "," + alpha + "," + beta + "," + gamma);

        unitCell = (SymmetryInterface) Interface.getOptionInterface("symmetry.Symmetry");
        unitCell.setUnitCell(new float[] {a, b, c, alpha, beta, gamma} );

        mapc = binarydoc.readInt();
        mapr = binarydoc.readInt();
        maps = binarydoc.readInt();

        Logger.info("MRC header: mapc,mapr,maps: " + mapc + "," + mapr + "," + maps);
        
        hdmin = binarydoc.readFloat();
        hdmax = binarydoc.readFloat();
        hdmean = binarydoc.readFloat();
        
        Logger.info("MRC header: dmin,dmax,dmean: " + hdmin + "," + hdmax + "," + hdmean);
        
        ispg = binarydoc.readInt();
        nsymbt = binarydoc.readInt();
        
        Logger.info("MRC header: ispg,nsymbt: " + ispg + "," +  nsymbt);

        binarydoc.readByteArray(extra);

        originX = binarydoc.readFloat();
        originY = binarydoc.readFloat();
        originZ = binarydoc.readFloat();
        
        Logger.info("MRC header: originX,Y,Z: " + originX + "," + originY + "," + originZ);

        binarydoc.readByteArray(map);
        binarydoc.readByteArray(machst);
        
        rms = binarydoc.readFloat();
        
        Logger.info("MRC header: rms: " + rms);
        
        nlabel = binarydoc.readInt();
        byte[] temp = new byte[80];
        for (int i = 0; i < 10; i++) {
          binarydoc.readByteArray(temp);
          StringBuffer s = new StringBuffer();
          for (int j = 0; j < 80; j++)
            s.append((char)temp[j]);
          labels[i] = s.toString().trim();
        }
        
        Logger.info("MRC header: bytes read: " + binarydoc.getPosition());
        
        if (params.cutoffAutomatic) {
          params.cutoff = rms * 2 + hdmean;
          Logger.info("MRC header: cutoff set to (dmean + 2*rms) = " + params.cutoff);
        }
        
      } catch (Exception e) {
        Logger.error("Error reading " + sg.getParams().fileName + " " + e.getMessage());
      }
    }
    
  }
  
  protected void readTitleLines() throws Exception {
    jvxlFileHeaderBuffer = new StringBuffer();
    jvxlFileHeaderBuffer.append("MRC DATA ").append(mrcHeader.labels[0]).append("\n");
    jvxlFileHeaderBuffer.append("see http://ami.scripps.edu/software/mrctools/mrc_specification.php\n");
    isAngstroms = true;
  }
  
  protected void readAtomCountAndOrigin() {
    VolumeFileReader.checkAtomLine(isXLowToHigh, isAngstroms, "0",
        "0 " + (mrcHeader.originX) + " " + (mrcHeader.originY) + " " +  (mrcHeader.originZ), 
        jvxlFileHeaderBuffer);
    volumetricOrigin.set(mrcHeader.originX, mrcHeader.originY, mrcHeader.originZ);
    if (isAnisotropic)
      setVolumetricOriginAnisotropy();
  }

  protected void readVoxelVector(int voxelVectorIndex) {
    //assuming standard orthogonality here
    // needs fixing...
    int i = 0;
    switch (voxelVectorIndex) {
    case 0:
      i = mrcHeader.maps - 1;
      voxelCounts[i] = mrcHeader.nx;
      volumetricVectors[i].set(mrcHeader.a / mrcHeader.mx, 0, 0);
      break;
    case 1:
      i = mrcHeader.mapr - 1;
      voxelCounts[i] = mrcHeader.ny;
      volumetricVectors[i].set(0, mrcHeader.b / mrcHeader.my, 0);
      break;
    case 2:
      i = mrcHeader.mapc - 1;
      voxelCounts[i] = mrcHeader.nz;
      volumetricVectors[i].set(0, 0, mrcHeader.c / mrcHeader.mz);
      break;
    }
    if (isAnisotropic)
      setVectorAnisotropy(volumetricVectors[i]);
  }  
  
  protected float nextVoxel() throws Exception {
    float voxelValue;
    /*
     *     4 MODE     data type :
         0        image : signed 8-bit bytes range -128 to 127
         1        image : 16-bit halfwords
         2        image : 32-bit reals
         3        transform : complex 16-bit integers
         4        transform : complex 32-bit reals
         6        image : unsigned 16-bit range 0 to 65535

     */
    switch(mrcHeader.mode) {
    case 0:
      voxelValue = binarydoc.readByte();
      break;
    case 1:
      voxelValue = binarydoc.readShort();
      break;
    case 3:
      //read first component only
      voxelValue = binarydoc.readShort();
      binarydoc.readShort();
      break;
    case 4:
      //read first component only
      voxelValue = binarydoc.readFloat();
      binarydoc.readFloat();
      break;
    case 6:
      voxelValue = binarydoc.readUnsignedShort();
      break;
    default:
      voxelValue = binarydoc.readFloat();
    }
    nBytes = binarydoc.getPosition();
    return voxelValue;
  }

  byte[] b2 = new byte[2];
  byte[] b4 = new byte[4];
  protected void skipData(int nPoints) throws Exception {
    for (int i = 0; i < nPoints; i++)
      switch(mrcHeader.mode) {
      case 0:
        binarydoc.readByte();
        break;
      case 1:
      case 6:
        binarydoc.readByteArray(b2);
        break;
      case 4:
        binarydoc.readByteArray(b4);
        binarydoc.readByteArray(b4);
        break;
      default:
        binarydoc.readByteArray(b4);
      }
  }
}
