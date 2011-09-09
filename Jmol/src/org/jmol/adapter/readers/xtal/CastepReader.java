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

package org.jmol.adapter.readers.xtal;

import java.util.ArrayList;
import java.util.List;

import org.jmol.adapter.smarter.*;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

import javax.vecmath.Vector3f;

/**
 * CASTEP (http://www.castep.org) .cell file format
 * relevant section of .cell file are included as comments below
 * 
 * preliminary .phonon frequency reader -- hansonr@stolaf.edu 9/2011
 *   atom's mass is encoded as bfactor
 *
 * @author Joerg Meyer, FHI Berlin 2009 (meyer@fhi-berlin.mpg.de)
 * @version 1.2
 */

public class CastepReader extends AtomSetCollectionReader {

  private String[] tokens;

  private float a, b, c, alpha, beta, gamma;
  private Vector3f[] abc = new Vector3f[3];
  private boolean iHaveFractionalCoordinates;
  private boolean isPhonon;

  private int atomCount;

  @Override
  public void initializeReader() throws Exception {
    
    while (tokenizeCastepCell() > 0) {
      if (isPhonon)
        return; // use checkLine
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
          continue;
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
          continue;
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
          continue;
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
          continue;
        }
      }
    }
    continuing = false;
  }
  
  @Override
  protected boolean checkLine() throws Exception {
    // only for .phonon file
    if (line.indexOf("Unit cell vectors") == 1) {
      readPhononUnitCell();
      return true;
    }
    if (line.indexOf("Fractional Co-ordinates") >= 0) {
      readPhononFractionalCoord();
      return true;
    }
    if (line.indexOf("q-pt") >= 0) {
      readPhononFrequencies();
      return true;
    }
    return true;
  }
  
  @Override
  protected void finalizeReader() throws Exception {
    if (isPhonon) {
      super.finalizeReader();
      return;
    }
      
    doApplySymmetry = true;
    setFractionalCoordinates(iHaveFractionalCoordinates);
    // relay length of and angles between cell vectors to Jmol
    setUnitCell(a, b, c, alpha, beta, gamma);
    /*
     * IMPORTANT: 
     * also hand over (matrix of) unit cell vectors to trigger
     * the calculation of the correct transformation matrices
     * from cartesian to fractional coordinates (which are used
     * internally by Jmol)
     */
    setLatticeVectors();
    int nAtoms = atomSetCollection.getAtomCount();
    /*
     * this needs to be run either way (i.e. even if coordinates are already
     * fractional) - to satisfy the logic in AtomSetCollectionReader()
     */
    for (int n = 0; n < nAtoms; n++) {
      Atom atom = atomSetCollection.getAtom(n);
      setAtomCoord(atom);
    }
    super.finalizeReader();
  }

  private void setLatticeVectors() {
    float[] lv = new float[3];
    for (int n = 0; n < 3; n++) {
      abc[n].get(lv);
      addPrimitiveLatticeVector(n, lv, 0);
    }
  }

  private void readLatticeAbc() throws Exception {
    if (tokenizeCastepCell() == 0)
      return;
    float factor = readLengthUnit();
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

    // initialize lattice vectors to NaN - since not present in .cell file
    for (int n = 0; n < 3; n++) {
      abc[n] = new Vector3f(Float.NaN, Float.NaN, Float.NaN);
    }
  }

  private void readLatticeCart() throws Exception {
    if (tokenizeCastepCell() == 0)
      return;
    float factor = readLengthUnit();
    float x, y, z;
    for (int n = 0; n < 3; n++) {
      if (tokens.length >= 3) {
        x = parseFloat(tokens[0]) * factor;
        y = parseFloat(tokens[1]) * factor;
        z = parseFloat(tokens[2]) * factor;
        abc[n] = new Vector3f(x, y, z);
      } else {
        Logger.warn("error reading coordinates of lattice vector "
            + Integer.toString(n + 1)
            + " in %BLOCK LATTICE_CART in CASTEP .cell file");
        return;
      }
      if (tokenizeCastepCell() == 0)
        return;
    }
    a = abc[0].length();
    b = abc[1].length();
    c = abc[2].length();
    alpha = (float) Math.toDegrees(abc[1].angle(abc[2]));
    beta = (float) Math.toDegrees(abc[2].angle(abc[0]));
    gamma = (float) Math.toDegrees(abc[0].angle(abc[1]));
  }

  private void readPositionsFrac() throws Exception {
    if (tokenizeCastepCell() == 0)
      return;
    readAtomData(1.0f);
  }

  private void readPositionsAbs() throws Exception {
    if (tokenizeCastepCell() == 0)
      return;
    float factor = readLengthUnit();
    readAtomData(factor);
  }

  /*
     to be kept in sync with Utilities/io.F90
  */
  private final static String[] lengthUnitIds = {
    "bohr", "m", "cm", "nm", "ang", "a0" };

  private final static float[] lengthUnitFactors = {
    ANGSTROMS_PER_BOHR, 1E10f, 1E8f, 1E1f, 1.0f, ANGSTROMS_PER_BOHR };

  private final static int lengthUnits = lengthUnitIds.length;

  private float readLengthUnit() throws Exception {
    float factor = 1.0f;
    for (int i=0; i<lengthUnits; i++) {
      if (tokens[0].equalsIgnoreCase(lengthUnitIds[i])) {
        factor = lengthUnitFactors[i];
        tokenizeCastepCell();
      }
    }
    return factor;
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
      if (line.startsWith(" BEGIN header")) {
        isPhonon = true;
        return 1;
      }
      tokens = getTokens();
      if (line.startsWith("#") || line.startsWith("!") || tokens[0].equals("#")
          || tokens[0].equals("!"))
        continue;
      break;
    }
    return tokens.length;
  }
  
  //////////// phonon code ////////////
  
  /*
  Unit cell vectors (A)
     0.000000    1.819623    1.819623
     1.819623    0.000000    1.819623
     1.819623    1.819623    0.000000
  Fractional Co-ordinates
      1     0.000000    0.000000    0.000000   B        10.811000
      2     0.250000    0.250000    0.250000   N        14.006740
    */
  private void readPhononUnitCell() throws Exception {
    abc = readDirectLatticeVectors(line.indexOf("bohr") >= 0);
    setSpaceGroupName("P1");
    setLatticeVectors();
  }

  private void readPhononFractionalCoord() throws Exception {
    setFractionalCoordinates(true);
    while (readLine() != null && line.indexOf("END") < 0) {
      String[] tokens = getTokens();
      Atom atom = atomSetCollection.addNewAtom();
      setAtomCoord(atom, parseFloat(tokens[1]), parseFloat(tokens[2]), parseFloat(tokens[3]));
      atom.bfactor = parseFloat(tokens[5]); // mass, actually
      atom.elementSymbol = tokens[4];
    }
    atomCount = atomSetCollection.getAtomCount();
  }
  

  /*
     q-pt=    1    0.000000  0.000000  0.000000      1.000000    1.000000  0.000000  0.000000
       1      58.268188              0.0000000                                  
       2      58.268188              0.0000000                                  
       3      58.292484              0.0000000                                  
       4    1026.286406             13.9270643                                  
       5    1026.286406             13.9270643                                  
       6    1262.072445             13.9271267                                  
                        Phonon Eigenvectors
  Mode Ion                X                                   Y                                   Z
   1   1 -0.188759409143  0.000000000000      0.344150676582  0.000000000000     -0.532910085817  0.000000000000
   1   2 -0.213788416373  0.000000000000      0.389784162147  0.000000000000     -0.603572578624  0.000000000000
   2   1 -0.506371267280  0.000000000000     -0.416656077168  0.000000000000     -0.089715190073  0.000000000000
   2   2 -0.573514781701  0.000000000000     -0.471903590472  0.000000000000     -0.101611191184  0.000000000000
   3   1  0.381712598768  0.000000000000     -0.381712598812  0.000000000000     -0.381712598730  0.000000000000
   3   2  0.433161430960  0.000000000000     -0.433161431010  0.000000000000     -0.433161430917  0.000000000000
   4   1  0.431092607594  0.000000000000     -0.160735361462  0.000000000000      0.591827969056  0.000000000000
   4   2 -0.380622988260  0.000000000000      0.141917473232  0.000000000000     -0.522540461492  0.000000000000
   5   1  0.434492641457  0.000000000000      0.590583470288  0.000000000000     -0.156090828832  0.000000000000
   5   2 -0.383624967478  0.000000000000     -0.521441660837  0.000000000000      0.137816693359  0.000000000000
   6   1  0.433161430963  0.000000000000     -0.433161430963  0.000000000000     -0.433161430963  0.000000000000
   6   2 -0.381712598770  0.000000000000      0.381712598770  0.000000000000      0.381712598770  0.000000000000
   */

  private void readPhononFrequencies() throws Exception {
    String[] tokens = getTokens();
    Vector3f qvec = new Vector3f(parseFloat(tokens[2]),parseFloat(tokens[3]),parseFloat(tokens[4]));
    if (supercell == null && qvec.length() != 0)
      return;      
    List<Float> freqs = new ArrayList<Float>();
    while (readLine() != null && line.indexOf("Phonon") < 0) {
      tokens = getTokens();
      freqs.add(Float.valueOf(parseFloat(tokens[1])));
    }
    readLine();
    int frequencyCount = freqs.size();
    for (int i = 0; i < frequencyCount; i++) {
      if (!doGetVibration(++vibrationNumber)) {
        for (int j = 0; j < atomCount; j++)
          readLine();
        continue;
      }
      if (desiredVibrationNumber <= 0)
        cloneLastAtomSet(atomCount);
      int iAtom = atomSetCollection.getLastAtomSetAtomIndex();
      float freq = freqs.get(i).floatValue();
      atomSetCollection.setAtomSetFrequency(null, null, "" + freq, null);
      atomSetCollection.setAtomSetName(TextFormat.formatDecimal(freq, 2)
          + " cm-1");
      Atom[] atoms = atomSetCollection.getAtoms();
      for (int j = 0; j < atomCount; j++) {
        tokens = getTokens(readLine());
        float factor = (float) Math.sqrt(1/atoms[iAtom].bfactor);
        atomSetCollection.addVibrationVector(iAtom++, parseFloat(tokens[2]) * factor,
            parseFloat(tokens[4]) * factor, parseFloat(tokens[6]) * factor, true);
      }
    }
  }

  

  
}
