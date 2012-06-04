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
        continue;
      }
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = sym;
      atom.partialCharge = parseFloat(line.substring(76, 84));
      setAtomCoord(atom, x, y, z);
    }
    if (nTv > 0) {
      for (int i = nTv; i < 3; i++)
        tvs[i] = new Point3f(0, 0, 0);
      Atom[] atoms = atomSetCollection.getAtoms();
      int atomCount = atomSetCollection.getAtomCount();
      float[] xyz = new float[3];
      for (int i = 0; i < 3; i++) { 
        xyz[0] = tvs[i].x;
        xyz[1] = tvs[i].y;
        xyz[2] = tvs[i].z;
        addPrimitiveLatticeVector(i, xyz, 0);
      }
      for (int i = atomCount; --i >= 0;)
        setAtomCoord(atoms[i]);
      Point3f ptMax = new Point3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
      Point3f ptMin = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
      if (doCentralize) {
        for (int i = atomCount; --i >= 0;) {
          ptMax.x = Math.max(ptMax.x, atoms[i].x);
          ptMax.y = Math.max(ptMax.y, atoms[i].y);
          ptMax.z = Math.max(ptMax.z, atoms[i].z);
          ptMin.x = Math.min(ptMin.x, atoms[i].x);
          ptMin.y = Math.min(ptMin.y, atoms[i].y);
          ptMin.z = Math.min(ptMin.z, atoms[i].z);
        }
        Point3f ptCenter = new Point3f();
        switch (nTv) {
        case 3:
          ptCenter.x = 0.5f;
        case 2:
          ptCenter.y = 0.5f;
        case 1:
          ptCenter.z = 0.5f;
        }
        ptCenter.scaleAdd(-0.5f, ptMin, ptCenter);
        ptCenter.scaleAdd(-0.5f, ptMax, ptCenter);
        for (int i = atomCount; --i >= 0;)
          atoms[i].add(ptCenter);
      }
      doCentralize = false;
    }

    return true;
  }
}
