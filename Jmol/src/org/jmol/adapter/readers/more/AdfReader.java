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
  int nXX = 0;

  /**
   * Read the ADF output.
   *
   * @param reader  input stream
   */
  public void readAtomSetCollection(BufferedReader reader) {
    atomSetCollection = new AtomSetCollection("adf", this);
    this.reader = reader;
    boolean iHaveAtoms = false;
    modelNumber = 0;
    try {
      while (readLine() != null) {
        if (line.indexOf("Coordinates (Cartesian)") >= 0
            || line.indexOf("G E O M E T R Y  ***  3D  Molecule  ***") >= 0) {
          if (!doGetModel(++modelNumber)) {
            if (isLastModel(modelNumber) && iHaveAtoms)
              break;
            iHaveAtoms = false;
            continue;
          }
          iHaveAtoms = true;
          readCoordinates();          
        } else if (iHaveAtoms && line.indexOf("Energy:") >= 0) {
          String[] tokens = getTokens();
          energy = tokens[1];
        } else if (iHaveAtoms && line.indexOf("Vibrations") >= 0) {
          readFrequencies();
        }
      }
    } catch (Exception e) {
      setError(e);
    }

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
    nXX = 0;
    while (readLine() != null && !line.startsWith(" -----")) {
      String[] tokens = getTokens();
      if (tokens.length < 5)
        break;
      String symbol = tokens[1];
      if (JmolAdapter.getElementNumber(symbol) < 1) {
        nXX++;
        continue;
      }
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
    readLine();
    while (readLine() != null) {
      while (readLine() != null && line.indexOf(".") < 0
          && line.indexOf("====") < 0) {
      }
      if (line == null || line.indexOf(".") < 0)
        return;
      String[] frequencies = getTokens();
      readLine(); // -------- -------- --------
      int iAtom0 = atomSetCollection.getAtomCount();
      int atomCount = atomSetCollection.getLastAtomSetAtomCount();
      int frequencyCount = frequencies.length;
      boolean[] ignore = new boolean[frequencyCount];
      for (int i = 0; i < frequencyCount; ++i) {
        ignore[i] = !doGetVibration(++vibrationNumber);
        atomSetCollection.cloneLastAtomSet();
        if (ignore[i])
          continue;
        atomSetCollection.setAtomSetName(frequencies[i] + " cm^-1");
        atomSetCollection.setAtomSetProperty("Frequency", frequencies[i]
            + " cm^-1");
        atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
            "Frequencies");
      }
      discardLines(nXX);
      fillFrequencyData(iAtom0, atomCount, ignore, true, 0, 0);
    }
  }
}
