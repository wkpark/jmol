/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

package org.jmol.adapter.smarter;

import java.io.BufferedReader;

import org.jmol.util.Logger;

/**
 * Gaussian cube file format
 * 
 * http://www.cup.uni-muenchen.de/oc/zipse/lv18099/orb_MOLDEN.html
 * this is good because it is source code
 * http://ftp.ccl.net/cca/software/SOURCES/C/scarecrow/gcube2plt.c
 *
 * http://www.nersc.gov/nusers/resources/software/apps/chemistry/gaussian/g98/00000430.htm
 * this contains some erroneous info
 * http://astronomy.swin.edu.au/~pbourke/geomformats/cube/
 * Miguel 2005 07 04
 * BUT, the files that I have do not comply with this format
 * because they have a negative atom count and an extra line
 * We will assume that there was a file format change, denoted by
 * the negative atom count.
 *
 * seems that distances are in Bohrs
 *
 * Miguel 2005 07 17
 * first two URLs above explain that a negative atom count means
 * that it is molecular orbital (MO) data
 * with MO data, the extra line contains the number
 * of orbitals and the orbital number
 * we only support # of orbitals == 1
 * if # of orbitals were > 1 then there would be multiple data
 * points in each cell
 */

class CubeReader extends AtomSetCollectionReader {
    
  boolean negativeAtomCount;
  int atomCount;
  
  final int[] voxelCounts = new int[3];
  final float[] origin = new float[3];
  final float[][] voxelVectors = new float[3][];
  float[][][] voxelData;
  
  AtomSetCollection readAtomSetCollection(BufferedReader br)
    throws Exception {
    
    reader = br;
    atomSetCollection = new AtomSetCollection("cube");
    try {
      atomSetCollection.newAtomSet();
      readTitleLines();
      readAtomCountAndOrigin();
      readVoxelVectors();
      readAtoms();
      /*
        volumetric data is no longer read here
      readExtraLine();
      readVoxelData();
      atomSetCollection.volumetricOrigin = origin;
      atomSetCollection.volumetricSurfaceVectors = voxelVectors;
      atomSetCollection.volumetricSurfaceData = voxelData;
      */
    } catch (Exception ex) {
      atomSetCollection.errorMessage = "Could not read Cube file:" + ex + "\n line:\n" + line;
      Logger.error("Could not read Cube file line " + line, ex);
    }
    return atomSetCollection;
  }

  void readTitleLines() throws Exception {
    line = reader.readLine().trim() + " - ";
    line += reader.readLine().trim();
    atomSetCollection.setAtomSetName("" + line);
  }

  void readAtomCountAndOrigin() throws Exception {
    line = reader.readLine();
    atomCount = parseInt(line);
    origin[0] = parseFloat(line, ichNextParse);
    origin[1] = parseFloat(line, ichNextParse);
    origin[2] = parseFloat(line, ichNextParse);
    if (atomCount < 0) {
      atomCount = -atomCount;
      negativeAtomCount = true;
    }
  }

  void readVoxelVectors() throws Exception {
    readVoxelVector(0);
    readVoxelVector(1);
    readVoxelVector(2);
  }

  void readVoxelVector(int voxelVectorIndex) throws Exception {
    line = reader.readLine();
    float[] voxelVector = new float[3];
    voxelVectors[voxelVectorIndex] = voxelVector;
    voxelCounts[voxelVectorIndex] = parseInt(line);
    voxelVector[0] = parseFloat(line, ichNextParse);
    voxelVector[1] = parseFloat(line, ichNextParse);
    voxelVector[2] = parseFloat(line, ichNextParse);
  }

  void readAtoms() throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      line = reader.readLine();
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementNumber = (byte)parseInt(line);
      atom.partialCharge = parseFloat(line, ichNextParse);
      atom.x = parseFloat(line, ichNextParse) * ANGSTROMS_PER_BOHR;
      atom.y = parseFloat(line, ichNextParse) * ANGSTROMS_PER_BOHR;
      atom.z = parseFloat(line, ichNextParse) * ANGSTROMS_PER_BOHR;
    }
  }

  /*

  void readExtraLine() throws Exception {
    if (negativeAtomCount)
      line = reader.readLine();
  }

  void readVoxelData() throws Exception {
    String line = "";
    ichNextParse = 0;
    int voxelCountX = voxelCounts[0];
    int voxelCountY = voxelCounts[1];
    int voxelCountZ = voxelCounts[2];
    voxelData = new float[voxelCountX][][];
    for (int x = 0; x < voxelCountX; ++x) {
      float[][] plane = new float[voxelCountY][];
      voxelData[x] = plane;
      for (int y = 0; y < voxelCountY; ++y) {
        float[] strip = new float[voxelCountZ];
        plane[y] = strip;
        for (int z = 0; z < voxelCountZ; ++z) {
          float voxelValue = parseFloat(line, ichNextParse);
          if (Float.isNaN(voxelValue)) {
            line = reader.readLine();
            if (line == null || Float.isNaN(voxelValue = parseFloat(line))) {
              throw new NullPointerException();
            }
          }
          strip[z] = voxelValue;
        }
      }
    }
  }
  */
}
