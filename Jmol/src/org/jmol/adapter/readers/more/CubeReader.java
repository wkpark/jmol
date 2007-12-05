/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-26 01:48:23 -0500 (Tue, 26 Sep 2006) $
 * $Revision: 5729 $
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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;


import java.io.BufferedReader;

/**
 * Gaussian cube file format
 * 
 * http://www.cup.uni-muenchen.de/oc/zipse/lv18099/orb_MOLDEN.html
 * this is good because it is source code
 * http://ftp.ccl.net/cca/software/SOURCES/C/scarecrow/gcube2plt.c
 *
 * http://www.nersc.gov/nusers/resources/software/apps/chemistry/gaussian/g98/00000430.htm
 *
 * distances are in Bohrs because we are reading Gaussian cube OUTPUT files
 * not Gaussian cube INPUT files. 
 *
 * Miguel 2005 07 17
 * a negative atom count means
 * that it is molecular orbital (MO) data
 * with MO data, the extra line contains the number
 * of orbitals and the orbital number
 * 
 * these orbitals are interspersed -- all orbital values are
 * given together for each coordinate point.
 * 
 * also used for JVXL and JVXL+ file format
 * 
 */

public class CubeReader extends AtomSetCollectionReader {
    
  boolean negativeAtomCount;
  int atomCount;
  boolean isAngstroms = false;
  
  final int[] voxelCounts = new int[3];
  final float[] origin = new float[3];
  final float[][] voxelVectors = new float[3][];
  
 public AtomSetCollection readAtomSetCollection(BufferedReader br) {
    reader = br;
    atomSetCollection = new AtomSetCollection("cube");
    try {
      atomSetCollection.newAtomSet();
      readTitleLines();
      readAtomCountAndOrigin();
      readVoxelVectors();
      readAtoms();
      readExtraLine();
      /*
        volumetric data is no longer read here
      readVoxelData();
      atomSetCollection.volumetricOrigin = origin;
      atomSetCollection.volumetricSurfaceVectors = voxelVectors;
      atomSetCollection.volumetricSurfaceData = voxelData;
      */
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }

  void readTitleLines() throws Exception {
    if (readLine().indexOf("#JVXL") == 0)
      while (readLine().indexOf("#") == 0) {
      }
    atomSetCollection.setAtomSetName(line.trim() + " - " + readLineTrimmed());
  }

  void readAtomCountAndOrigin() throws Exception {
    readLine();
    isAngstroms = (line.indexOf("ANGSTROMS") >= 0); //JVXL flag for Angstroms
    String[] tokens = getTokens();
    if (tokens[0].charAt(0) == '+') //Jvxl progressive reader -- ignore and consider negative
      tokens[0] = '-' + tokens[0].substring(1);
    atomCount = parseInt(tokens[0]);
    origin[0] = parseFloat(tokens[1]);
    origin[1] = parseFloat(tokens[2]);
    origin[2] = parseFloat(tokens[3]);
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
    readLine();
    float[] voxelVector = new float[3];
    voxelVectors[voxelVectorIndex] = voxelVector;
    voxelCounts[voxelVectorIndex] = parseInt(line);
    voxelVector[0] = parseFloat();
    voxelVector[1] = parseFloat();
    voxelVector[2] = parseFloat();
  }

  void readAtoms() throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      readLine();
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementNumber = (short)parseInt(line); //allowing atomicAndIsotope for JVXL format
      atom.partialCharge = parseFloat();
      atom.x = parseFloat();
      atom.y = parseFloat();
      atom.z = parseFloat();
      if (!isAngstroms)
        atom.scale(ANGSTROMS_PER_BOHR);
    }
  }

  void readExtraLine() throws Exception {
    if (negativeAtomCount)
      readLine();
    int nSurfaces = parseInt(line);
    if (nSurfaces != Integer.MIN_VALUE && nSurfaces < 0)
      atomSetCollection.setFileTypeName("jvxl");
  }
}
