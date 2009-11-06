/* $RCSfile$
 * $Author: hansonr $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

/* 
 * Copyright (C) 2009  Joerg Meyer, FHI Berlin
 *
 * Contact: meyer@fhi-berlin.mpg.de
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
import org.jmol.util.Logger;

import javax.vecmath.Vector3f;

import java.io.BufferedReader;

/**
 * FHI-AIMS (http://www.fhi-berlin.mpg.de/aims) geometry.in file format
 *
 * samples of relevant lines in geometry.in file are included as comments below
 *
 * @author Joerg Meyer, FHI Berlin 2009 (meyer@fhi-berlin.mpg.de)
 * @version 1.1
 * 
 */

public class AimsReader extends AtomSetCollectionReader {

  public void readAtomSetCollection(BufferedReader br) {
    reader = br;
    atomSetCollection = new AtomSetCollection("aims", this);
    //boolean iHaveAtoms = false;  // unused

    String tokens[];
    float x,y,z;
    int nLatticeVectors = 0;
    Vector3f[] latticeVectors = new Vector3f[3];
    try {
      while (readLine() != null) {

        tokens = getTokens();

        if (tokens.length == 0) continue;

        /*
atom       2.750645380     2.750645380    25.000000000   Pd 
        */
        if (tokens[0].equals("atom")) {
           if (tokens.length != 5) {
              Logger.warn("cannot read line with AIMS atom data: " + line);
           } else {
             Atom atom = atomSetCollection.addNewAtom();
             x = parseFloat(tokens[1]);
             y = parseFloat(tokens[2]);
             z = parseFloat(tokens[3]);
             atom.set(x,y,z);
             atom.elementSymbol = tokens[4];
          }
        }

        /*
lattice_vector      16.503872273     0.000000000     0.000000000 
        */
        if (tokens[0].equals("lattice_vector")) { 
           if (nLatticeVectors > 2) {
              Logger.warn("more than 3 AIMS lattice vectors found with line: " + line);
           } else {
             x = parseFloat(tokens[1]);
             y = parseFloat(tokens[2]);
             z = parseFloat(tokens[3]);
             latticeVectors[nLatticeVectors] = new Vector3f(x,y,z);
           }  
           nLatticeVectors++;
        }
      }

      /* translational symmetry in less than three directions 
         is currently neither supported by Jmol nor FHI-AIMS */
      if (nLatticeVectors == 3) {

         /* enforce application of (translational!) symmetry in Jmol
            (in case of crystal lattice being present in the AIMS input file) */

        doApplySymmetry = true;

         /* coordinates in AIMS input file are not fractional */
         
         setFractionalCoordinates(false);

         /* transformation of lattice represenation (i.e. some linear algebra) 
            is done in separate (recyclable) routine below 
          */
         setUnitCellFromLatticeVectors(latticeVectors);

         /* better do not hardcode as other interpreted to be given in file
            setSpaceGroupName("P1");
         */

         /* this takes care of properly converting cartesian coordinates to fractionals
          * when necessary (i.e. use of symmetry is turned on) - 
          * and is crucial for unit cell being internally 'accepted' as such 
          * (as indicated in AtomSetCollectionReader.java)
          * doing this above (when adding atoms) is not sufficient, as a unit cell
          * has (deliberately!) not yet been added then 
          */
         int nAtoms = atomSetCollection.getAtomCount();
         for (int n=0; n<nAtoms; n++) {
             Atom atom = atomSetCollection.getAtom(n);
             setAtomCoord(atom);
         }
         /* now update the symmetry information of the Jmol model */
         applySymmetryAndSetTrajectory();
      }

    } catch (Exception e) {
      setError(e);
    }
  }

  void setUnitCellFromLatticeVectors(Vector3f[] lv) {
    float a = lv[0].length();
    float b = lv[1].length();
    float c = lv[2].length();
    float alpha =  (float) Math.toDegrees(lv[1].angle(lv[2]));
    float beta = (float) Math.toDegrees(lv[2].angle(lv[0]));
    float gamma = (float) Math.toDegrees(lv[0].angle(lv[1]));
    setUnitCell(a,b,c,alpha,beta,gamma);
  } 

}
