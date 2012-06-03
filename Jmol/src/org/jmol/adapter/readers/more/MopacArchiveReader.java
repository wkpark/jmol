/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-26 01:48:23 -0500 (Tue, 26 Sep 2006) $
 * $Revision: 5729 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;
import org.jmol.util.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3f;

/**
 * Amber Coordinate File Reader
 * 
 * not a stand-alone reader -- must be after COORD keyword in LOAD command
 * 
 */

public class MopacArchiveReader extends AtomSetCollectionReader {


  @Override
  protected boolean checkLine() throws Exception {
    if (line.indexOf("FINAL GEOMETRY OBTAINED") >= 0)
      return readCoordinates();
    return true;
  }

  /*
          FINAL GEOMETRY OBTAINED                                    CHARGE
MERS=(1,2,2)   GNORM=4
 Lansfordite (MgCO3 5(H2O))

0         1         2         3         4         5         6         7         8
012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
 Mg     0.00407813 +1  -0.10980012 +1  -0.07460042 +1                         0.0000
   */
  
  private boolean readCoordinates() throws Exception {
    readLine();
    atomSetCollection.newAtomSet();
    atomSetCollection.setAtomSetName(readLine().trim());
    readLine();
    int nTv = 0;
    float[] xyz = new float[3];
    while (readLine() != null && line.length() > 0) {
      float x = parseFloat(line.substring(5, 18));
      float y = parseFloat(line.substring(21, 34));
      float z = parseFloat(line.substring(37, 50));
      String sym = line.substring(1,3).trim();
      if (sym.equals("Tv")) {
        if (nTv == 0) {
          setFractionalCoordinates(false);
          setSpaceGroupName("P1");
        }
        xyz[0] = x;
        xyz[1] = y;
        xyz[2] = z;
        addPrimitiveLatticeVector(nTv++, xyz, 0);
        if (nTv == 3) {
          Atom[] atoms = atomSetCollection.getAtoms();
          for (int i = atomSetCollection.getAtomCount(); --i >= 0;)
            setAtomCoord(atoms[i]);            
        }
        continue;
      }
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = sym;
      atom.partialCharge = parseFloat(line.substring(76, 84));
      setAtomCoord(atom, x, y, z);
    }
    return true;
  }
}
