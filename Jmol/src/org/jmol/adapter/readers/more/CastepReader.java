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
 * CASTEP (http://www.castep.org) .cell file format
 *
 * relevant section of .cell file are included as comments below
 *
 * @author Joerg Meyer, FHI Berlin 2009 (meyer@fhi-berlin.mpg.de)
 * @version 1.1
 */

public class CastepReader extends AtomSetCollectionReader {

  private String[] tokens;

  private float a = 0.0f;
  private float b = 0.0f;
  private float c = 0.0f;
  private float alpha = 0.0f;
  private float beta = 0.0f;
  private float gamma = 0.0f;

  public void readAtomSetCollection(BufferedReader br) {

    reader = br;
    atomSetCollection = new AtomSetCollection("castep", this);

    boolean iHaveFractionalCoordinates = false;

    try {

      while (tokenizeCastepCell() > 0) {


        if ((tokens.length >= 2) && (tokens[0].equalsIgnoreCase("%BLOCK"))) {

          /*
           * unit cell can only be set later
           * to get symmetry properly initialized -
           * see below!
           */

          /*
%BLOCK LATTICE_ABC
ang
  16.66566792 8.33283396  16.82438907
  90.0    90.0    90.0
%ENDBLOCK LATTICE_ABC
          */
          if (tokens[1].equalsIgnoreCase("LATTICE_ABC")) {
            readLatticeAbc();
          }
          /*
%BLOCK LATTICE_CART
ang
  16.66566792 0.0   0.0
  0.0   8.33283396  0.0
  0.0   0.0   16.82438907
%ENDBLOCK LATTICE_CART
          */
          if (tokens[1].equalsIgnoreCase("LATTICE_CART")) {
            readLatticeCart();
          }

          /* coordinates are set immediately */
          /*
%BLOCK POSITIONS_FRAC
   Pd         0.0 0.0 0.0
%ENDBLOCK POSITIONS_FRAC
          */
          if (tokens[1].equalsIgnoreCase("POSITIONS_FRAC")) {
            readPositionsFrac();
            iHaveFractionalCoordinates = true;
          }
          /*
%BLOCK POSITIONS_ABS
ang
   Pd         0.00000000         0.00000000       0.00000000 
%ENDBLOCK POSITIONS_ABS
          */
          if (tokens[1].equalsIgnoreCase("POSITIONS_ABS")) {
            readPositionsAbs();
            iHaveFractionalCoordinates = false;
          }
        }
      }

      doApplySymmetry = true;
      setFractionalCoordinates(iHaveFractionalCoordinates);
      setUnitCell(a, b, c, alpha, beta, gamma);
      int nAtoms = atomSetCollection.getAtomCount();
      /*
       * this needs to be run either way (i.e. even if coordinates are already
       * fractional) - to satisfy the logic in AtomSetCollectionReader()
       */
      for (int n = 0; n < nAtoms; n++) {
        Atom atom = atomSetCollection.getAtom(n);
        setAtomCoord(atom);
      }
      applySymmetryAndSetTrajectory();

    } catch (Exception e) {
      setError(e);
    }
  }

  private void readLatticeAbc() throws Exception {

    float factor = 1.0f;

    if (tokenizeCastepCell() == 0)
      return;
    if (tokens[0].equalsIgnoreCase("bohr"))
      factor = ANGSTROMS_PER_BOHR;
    if (tokens.length < 3)
      tokenizeCastepCell();
    if (tokens.length >= 3) {
      a = parseFloat(tokens[0]) * factor;
      b = parseFloat(tokens[1]) * factor;
      c = parseFloat(tokens[2]) * factor;
    } else {
      Logger
          .warn("error reading a,b,c in %BLOCK LATTICE_ABC in CASTEP .cell file");
      return;
    }

    if (tokenizeCastepCell() == 0)
      return;
    if (tokens.length >= 3) {
      alpha = parseFloat(tokens[0]);
      beta = parseFloat(tokens[1]);
      gamma = parseFloat(tokens[2]);
    } else {
      Logger
          .warn("error reading alpha,beta,gamma in %BLOCK LATTICE_ABC in CASTEP .cell file");
    }
  }

  private void readLatticeCart() throws Exception {

    float factor = 1.0f;
    float x, y, z;
    Vector3f[] lv = new Vector3f[3];

    if (tokenizeCastepCell() == 0)
      return;
    if (tokens[0].equalsIgnoreCase("bohr"))
      factor = ANGSTROMS_PER_BOHR;
    if (tokens.length < 3)
      tokenizeCastepCell();
    for (int n = 0; n < 3; n++) {
      if (tokens.length >= 3) {
        x = parseFloat(tokens[0]) * factor;
        y = parseFloat(tokens[1]) * factor;
        z = parseFloat(tokens[2]) * factor;
        lv[n] = new Vector3f(x, y, z);
      } else {
        Logger.warn("error reading coordinates of lattice vector "
            + Integer.toString(n + 1)
            + " in %BLOCK LATTICE_CART in CASTEP .cell file");
        return;
      }
      if (tokenizeCastepCell() == 0)
        return;
    }

    a = lv[0].length();
    b = lv[1].length();
    c = lv[2].length();
    alpha = (float) Math.toDegrees(lv[1].angle(lv[2]));
    beta = (float) Math.toDegrees(lv[2].angle(lv[0]));
    gamma = (float) Math.toDegrees(lv[0].angle(lv[1]));
  }

  private void readPositionsFrac() throws Exception {

    if (tokenizeCastepCell() == 0)
      return;
    readAtomData(1.0f);
  }

  private void readPositionsAbs() throws Exception {

    if (tokenizeCastepCell() == 0)
      return;
    if (tokens[0].equalsIgnoreCase("bohr")) {
      tokenizeCastepCell();
      readAtomData(ANGSTROMS_PER_BOHR);
    } else if (tokens[0].equalsIgnoreCase("ang")){
      tokenizeCastepCell();
      readAtomData(1.0f);
    } else {
      readAtomData(1.0f);
    }
  }

  private void readAtomData(float factor) throws Exception {

    float x, y, z;

    do {

      if (tokens[0].equalsIgnoreCase("%ENDBLOCK"))
        break;

      if (tokens.length >= 4) {
        Atom atom = atomSetCollection.addNewAtom();
        x = parseFloat(tokens[1]) * factor;
        y = parseFloat(tokens[2]) * factor;
        z = parseFloat(tokens[3]) * factor;
        atom.set(x, y, z);
        atom.elementSymbol = tokens[0];
      } else {
        Logger.warn("cannot read line with CASTEP atom data: " + line);
      }
    } while (tokenizeCastepCell() > 0);
  }

  private int tokenizeCastepCell() throws Exception {

    while (true) {
      if (readLine() == null)
        return 0;
      if (line.trim().length() == 0)
        continue;
      tokens = getTokens();
      if (line.startsWith("#") || line.startsWith("!") || tokens[0].equals("#")
          || tokens[0].equals("!"))
        continue;
      break;
    }
    return tokens.length;
  }

}
