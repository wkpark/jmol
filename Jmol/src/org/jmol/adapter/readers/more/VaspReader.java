/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-22 14:12:46 -0500 (Sun, 22 Oct 2006) $
 * $Revision: 5999 $
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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;

import java.io.BufferedReader;

import org.jmol.util.Logger;

/**
 * 
 * Vasp OUTCAR reader
 * 
 */

public class VaspReader extends AtomSetCollectionReader {
  private String[] atomNames;
  
  public void readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("vasp", this);
    setAtomNames(parameterData);
    try {
      int modelAtomCount = 0;
      while (readLine() != null) {
        if (line.indexOf("what?") == 0) {
          readTitle();
          continue;
        }
        if (line
            .indexOf(" position of ions in fractional coordinates (direct lattice) ") == 0) {
          modelAtomCount = readAtomCount();
          continue;
        }
        if (line
            .indexOf(" position of ions in cartesian coordinates  (Angst):") == 0
            || line
                .indexOf(" POSITION                                       TOTAL-FORCE (eV/Angst)") == 0) {
          if (doGetModel(++modelNumber)) {
            readAtoms(modelAtomCount);
            applySymmetryAndSetTrajectory();
            if (isLastModel(modelNumber))
              break;
          }
        }
      }
    } catch (Exception e) {
      setError(e);
    }
  }

  private void setAtomNames(String data) {
    // TODO
    
  }

  private int readAtomCount() throws Exception {
    int atomCount = 0;
    while (readLine() != null && line.length() > 10)
      atomCount++;
    return atomCount;
  }

  private void readAtoms(int modelAtomCount) throws Exception {
    atomSetCollection.newAtomSet();
    for (int i = 0; i < modelAtomCount; ++i) {
      readLine();
      if (i == 0 && line.indexOf("-----") >= 0)
        readLine();
      String[] tokens = getTokens();
      Atom atom = atomSetCollection.addNewAtom();
      atom.x = parseFloat(tokens[0]);
      atom.y = parseFloat(tokens[1]);
      atom.z = parseFloat(tokens[2]);
      if (Float.isNaN(atom.x) || Float.isNaN(atom.y) || Float.isNaN(atom.z)) {
        Logger.warn("line cannot be read for XYZ atom data: " + line);
        atom.set(0, 0, 0);
      }
      setAtomCoord(atom);
      if (atomNames != null && i < atomNames.length)
        atom.elementSymbol = atomNames[i];
      switch (tokens.length) {
      case 3:
        continue;
      case 6:
        float vx = parseFloat(tokens[3]);
        float vy = parseFloat(tokens[4]);
        float vz = parseFloat(tokens[5]);
        if (Float.isNaN(vx) || Float.isNaN(vy) || Float.isNaN(vz))
          continue;
        atomSetCollection.addVibrationVector(atom.atomIndex, vx, vy, vz);
      }
    }
  }

  private void readTitle() {
    // TODO
    
  }

}
