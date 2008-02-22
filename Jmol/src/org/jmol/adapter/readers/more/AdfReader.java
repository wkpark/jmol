/* $RCSfile: ADFReader.java,v $
 * $Author: egonw $
 * $Date: 2004/02/23 08:52:55 $
 * $Revision: 1.3.2.4 $
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;
import org.jmol.api.JmolAdapter;

import java.io.BufferedReader;

/**
 * A reader for ADF output.
 * Amsterdam Density Functional (ADF) is a quantum chemistry program
 * by Scientific Computing & Modelling NV (SCM)
 * (http://www.scm.com/).
 *
 * <p> Molecular coordinates, energies, and normal coordinates of
 * vibrations are read. Each set of coordinates is added to the
 * ChemFile in the order they are found. Energies and vibrations
 * are associated with the previously read set of coordinates.
 *
 * <p> This reader was developed from a small set of
 * example output files, and therefore, is not guaranteed to
 * properly read all ADF output. If you have problems,
 * please contact the author of this code, not the developers
 * of ADF.
 *
 * @author Bradley A. Smith (yeldar@home.com)
 * @version 1.0
 */
public class AdfReader extends AtomSetCollectionReader {

  

  String energy = null;

  /**
   * Read the ADF output.
   *
   * @param reader  input stream
   * @return a ChemFile with the coordinates, energies, and vibrations.
   */
  public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    atomSetCollection = new AtomSetCollection("adf");
    this.reader = reader;
    boolean iHaveAtoms = false;
    modelNumber = 0;
    try {
      while (readLine() != null) {
        if (line.indexOf("Coordinates (Cartesian)") >= 0
            || line.indexOf("G E O M E T R Y  ***  3D  Molecule  ***") >= 0) {
          if (++modelNumber != desiredModelNumber && desiredModelNumber > 0) {
            if (iHaveAtoms)
              break;
            continue;
          }
          iHaveAtoms = true;
          readCoordinates();          
        } else if (line.indexOf("Energy:") >= 0) {
          String[] tokens = getTokens();
          energy = tokens[1];
        } else if (line.indexOf("Vibrations") >= 0) {
          readFrequencies();
        }
      }
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }

  /**
   * Reads a set of coordinates
   *
   * @exception Exception  if an I/O error occurs
   */
  private void readCoordinates() throws Exception {

    /*
     * 
 Coordinates (Cartesian)
 =======================

   Atom                      bohr                                 angstrom                 Geometric Variables
                   X           Y           Z              X           Y           Z       (0:frozen, *:LT par.)
 --------------------------------------------------------------------------------------------------------------
   1 XX         .000000     .000000     .000000        .000000     .000000     .000000      0       0       0


OR


 ATOMS
 =====                            X Y Z                    CHARGE
                                (Angstrom)             Nucl     +Core       At.Mass
                       --------------------------    ----------------       -------
    1  Ni              0.0000    0.0000    0.0000     28.00     28.00       57.9353

     * 
     */
    atomSetCollection.newAtomSet();
    atomSetCollection.setAtomSetName("" + energy); // start with an empty name
    discardLinesUntilContains("----");
    while (readLine() != null && !line.startsWith(" -----")) {
      String[] tokens = getTokens();
      if (tokens.length < 5)
        break;
      String symbol = tokens[1];
      if (JmolAdapter.getElementNumber(symbol) < 1)
        continue;
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = symbol;
      atom.set(parseFloat(tokens[2]), parseFloat(tokens[3]), parseFloat(tokens[4]));
      if (tokens.length > 8)
        atom.scale(ANGSTROMS_PER_BOHR);
    }
  }

  /*
   Vibrations and Normal Modes  ***  (cartesian coordinates, NOT mass-weighted)  ***
   ===========================
   
   The headers on the normal mode eigenvectors below give the Frequency in cm-1
   (a negative value means an imaginary frequency, no output for (almost-)zero frequencies)


   940.906                      1571.351                      1571.351
   ------------------------      ------------------------      ------------------------
   1.XX          .000    .000    .000          .000    .000    .000          .000    .000    .000
   2.N           .000    .000    .115          .008    .067    .000         -.067    .008    .000
   3.H           .104    .180   -.534          .323   -.037   -.231          .580   -.398    .098
   4.H          -.208    .000   -.534          .017   -.757    .030         -.140   -.092   -.249
   5.H           .104   -.180   -.534         -.453   -.131    .201          .485    .378    .151


   ====================================
   */
  /**
   * Reads a set of vibrations.
   *
   * @exception Exception  if an I/O error occurs
   */
  private void readFrequencies() throws Exception {
    String[] tokens;
    String[] frequencies;
    readLine();
    int atomCount = atomSetCollection.getLastAtomSetAtomCount();
    while (readLine() != null) {
      while (readLine() != null && line.indexOf(".") < 0
          && line.indexOf("====") < 0) {
      }
      if (line == null || line.indexOf(".") < 0)
        return;
      frequencies = getTokens();
      readLine(); // -------- -------- --------
      int frequencyCount = frequencies.length;
      int firstModelAtom = atomSetCollection.getAtomCount();
      for (int i = 0; i < frequencyCount; ++i) {
        atomSetCollection.cloneLastAtomSet();
        atomSetCollection.setAtomSetName(frequencies[i] + " cm**-1");
        atomSetCollection.setAtomSetProperty("Frequency", frequencies[i]
            + " cm**-1");
        atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
            "Frequencies");
      }
      int atomPt = 0;
      while (readLine() != null && line.indexOf(".") >= 0) {
        tokens = getTokens();
        String symbol = tokens[0].substring(tokens[0].indexOf(".") + 1);
        if (JmolAdapter.getElementNumber(symbol) < 1)
          continue;
        float x, y, z;
        int offset = 1;
        for (int j = 0; j < frequencyCount; ++j) {
          int atomOffset = firstModelAtom + j * atomCount + atomPt;
          Atom atom = atomSetCollection.getAtom(atomOffset);
          x = parseFloat(tokens[offset++]);
          y = parseFloat(tokens[offset++]);
          z = parseFloat(tokens[offset++]);
          atom.addVibrationVector(x, y, z);
        }
        atomPt++;
      }
    }
  }
}
