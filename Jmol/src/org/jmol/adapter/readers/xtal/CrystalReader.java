/* $RCSfile$
 * $Author: hansonr $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 * Copyright (C) 2009  Piero Canepa, University of Kent , UK
 *
 * Contact: pc229@kent.ac.uk or pieremanuele.canepa@gmail.com
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
 *
 */

package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.*;
import java.io.BufferedReader;

/**
 *
 * http://www.crystal.unito.it/
 *
 * @author Pieremanuele Canepa, Room 104, FM Group
 * School of Physical Sciences, Ingram Building,
 * University of Kent, Canterbury, Kent,
 * CT2 7NH
 * United Kingdom
 *
 * @version 1.0
 *
 * This version works and has been well tested on several structures!
 *
 */

public class CrystalReader extends AtomSetCollectionReader {

 public void readAtomSetCollection(BufferedReader reader) {

   boolean isthisPeriodic=false ;

   atomSetCollection = new AtomSetCollection("Crystal", this);
   try {
     this.reader = reader;
     atomSetCollection.setCollectionName(readLine());
     while (readLine() != null) {
       if (line.startsWith("MOLECULE")) {
         isthisPeriodic=false;
         readAtomCoords(false,isthisPeriodic);

         break;
       }
       if (line.startsWith("CRYSTAL") || line.startsWith("SLAB")
           || line.startsWith("POLYMER") || line.startsWith("EXTERNAL")) {
         atomSetCollection.setAtomSetAuxiliaryInfo("periodicity", line);
         isthisPeriodic=true;
         readCellParams();
         readAtomCoords(true, isthisPeriodic);

         break;
       }
     }

     applySymmetryAndSetTrajectory();

   } catch (Exception e) {
     setError(e);
   }
 }

 private void readCellParams() throws Exception {

   discardLinesUntilStartsWith(" PRIMITIVE CELL");
   readLine();
   readLine();
   float a = parseFloat(line.substring(2,17)) ;
   float b = parseFloat(line.substring(16,33)) ;
   float c = parseFloat(line.substring(33,42));

   if (b == 500.00000){
     b = 1;
   }

   if (c == 500.0000){
     c = 1;
   }
   float alpha =  parseFloat(line.substring(48,58)) ;
   float beta =  parseFloat(line.substring(59,69)) ;
   float gamma =  parseFloat(line.substring(70,80)) ;

   setUnitCell(a, b, c, alpha, beta, gamma);
 }

 private void readAtomCoords(boolean isFractional, boolean isthisPeriodic)
      throws Exception {
    setFractionalCoordinates(isFractional);
    discardLinesUntilContains(" ATOMS IN THE ASYMMETRIC");
    int atomCount = parseInt(line.substring(61, 65));
    readLine();
    readLine();
    for (int i = 0; i < atomCount; i++) {
      readLine();
      String atomName = line.substring(8, 11);
      int atomicnumber = parseInt(atomName.substring(0, 2).trim());
      float x = parseFloat(line.substring(15, 35));
      float y = parseFloat(line.substring(36, 55));
      float z = parseFloat(line.substring(56, 75));

      if (x < 0 && isthisPeriodic) {
        x = 1 + x;
      }

      if (y < 0 && isthisPeriodic) {
        y = 1 + y;
      }

      if (z < 0 && isthisPeriodic) {
        z = 1 + z;
      }

      Atom atom = atomSetCollection.addNewAtom();
      setAtomCoord(atom, x, y, z);
      atom.elementSymbol = getElementSymbol(atomicnumber);
      atom.atomName = atomName + "_" + i;
    }
  }

}
