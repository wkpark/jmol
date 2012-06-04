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

import javax.vecmath.Point3f;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;

/**
 * 
 * Mopac Archive reader -- presumes "zMatrix" is really Cartesians
 * 
 * use FILTER "CENTER" to center atoms in unit cell
 * use CENTROID FILTER "CENTER" for complete molecules with centroids within unit cell
 * use PACKED CENTROID FILTER "CENTER" for complete molecules with any atoms within unit cell 
 * 
 * @author Bob Hanson hansonr@stolaf.edu
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
    Point3f[] tvs = new Point3f[3]; 
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
        tvs[nTv++] = new Point3f(x, y, z);
        if (nTv == 3) {
          Atom[] atoms = atomSetCollection.getAtoms();
          int atomCount = atomSetCollection.getAtomCount();
          Point3f ptCenter = new Point3f();
          if (doCentralize) {
            for (int i = atomCount; --i >= 0;) {
              ptCenter.x += atoms[i].x;
              ptCenter.y += atoms[i].y;
              ptCenter.z += atoms[i].z;
            }
            ptCenter.scale(-1f / atomCount);
            for (int i = 0; i < 3; i++)
              ptCenter.scaleAdd(0.5f, tvs[i], ptCenter);
          }
          float[] xyz = new float[3];
          fileScaling = new Point3f(1, 1, 1);
          if (fileOffset == null)
            fileOffset = new Point3f();
          fileOffset.add(ptCenter);
          for (int i = 0; i < 3; i++) { 
            xyz[0] = tvs[i].x;
            xyz[1] = tvs[i].y;
            xyz[2] = tvs[i].z;
            addPrimitiveLatticeVector(i, xyz, 0);
          }
          for (int i = atomCount; --i >= 0;) {
            setAtomCoord(atoms[i]);           
          }
          doCentralize = false;
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
