/* $RCSfile$
 * $Author: nicove $
 * $Date: 2006-08-30 13:20:20 -0500 (Wed, 30 Aug 2006) $
 * $Revision: 5447 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
import org.jmol.api.JmolAdapter;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * A reader for Q-Chem 2.1
 * Q-Chem  is a quantum chemistry program developed
 * by Q-Chem, Inc. (http://www.q-chem.com/)
 *
 * <p> Molecular coordinates and normal coordinates of
 * vibrations are read. 
 *
 * <p> This reader was developed from a single
 * output file, and therefore, is not guaranteed to
 * properly read all Q-chem output. If you have problems,
 * please contact the author of this code, not the developers
 * of Q-chem.
 *
 * <p> This is a hacked version of Miguel's GaussianReader
 *
 * @author Steven E. Wheeler (swheele2@ccqc.uga.edu)
 * @version 1.0
 * 
 * added modifications to deal with qchem 3.2 output
 * also will keep the structures of optimization calculations
 * @author RenŽ P.F Kanters (rkanters@richmond.edu)
 * @version 1.1
 */

public class QchemReader extends AtomSetCollectionReader {
 
/** The number of the calculation being interpreted. */
  private int calculationNumber = 1;

  
 public AtomSetCollection readAtomSetCollection(BufferedReader reader)  {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("qchem");
    try {
      int lineNum = 0;
      while (readLine() != null) {
        if (line.indexOf("Standard Nuclear Orientation") >= 0) {
          readAtoms();
        } else if (line.indexOf("VIBRATIONAL FREQUENCIES") >= 0) {
          readFrequencies();
 //         break;
        } else if (line.indexOf("Mulliken Net Atomic Charges") >= 0){
          readPartialCharges();
        } else if (line.indexOf("Job ") >= 0) {
          calculationNumber++;
        }
        ++lineNum;
      }
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }

/* Q-chem 2.1 format:
       Standard Nuclear Orientation (Angstroms)
    I     Atom         X            Y            Z
 ----------------------------------------------------
    1      H       0.000000     0.000000     4.756791
*/

//  int atomCount;

  void readAtoms() throws Exception {
    atomSetCollection.newAtomSet();
    
    discardLines(2);
    String[] tokens;
    while (readLine() != null && !line.startsWith(" --")) {
      tokens = getTokens();
      if (tokens.length < 5)
        continue;
      String symbol = tokens[1];
      if (JmolAdapter.getElementNumber(symbol) < 1)
        continue;
      //q-chem specific offsets
      float x = parseFloat(tokens[2]);
      float y = parseFloat(tokens[3]);
      float z = parseFloat(tokens[4]);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        continue;
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = symbol;
      atom.set(x, y, z);
      atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
          "Calculation "+calculationNumber);
   }
  }
  
  /**
   * Interprets the Harmonic frequencies section.
   *
   * <p>The vectors are added to a clone of the last read AtomSet.
   * Only the Frequencies, reduced masses, force constants and IR intensities
   * are set as properties for each of the frequency type AtomSet generated.
   *
   * @throws Exception If no frequences were encountered
   * @throws IOException If an I/O error occurs
   **/
  private void readFrequencies() throws Exception, IOException {
    String[] tokens; String[] frequencies;
    
    // first get the the proper line with the Frequencies:
    frequencies = getTokens(discardLinesUntilStartsWith(" Frequency:"));
   
    // G98 ends the frequencies with a line with a space (03 an empty line)
    // so I decided to read till the line is too short
    while (true)
    {
      int frequencyCount = frequencies.length;
      
      for (int i = 1; i < frequencyCount; ++i) {
        atomSetCollection.cloneLastAtomSet();
        atomSetCollection.setAtomSetName(frequencies[i]+" cm**-1");
        // set the properties
        atomSetCollection.setAtomSetProperty("Frequency",
            frequencies[i]+" cm**-1");
        atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
            "Calculation " + calculationNumber+
            SmarterJmolAdapter.PATH_SEPARATOR+"Frequencies");
      }
      
      int atomCount = atomSetCollection.getLastAtomSetAtomCount();
      int firstModelAtom =
        atomSetCollection.getAtomCount() - frequencyCount * atomCount;
      
      // position to start reading the displacement vectors
      discardLinesUntilStartsWith("               X");
      
      // read the displacement vectors for every atom and frequency
      float x, y, z;
      Atom[] atoms = atomSetCollection.getAtoms();
      for (int i = 0; i < atomCount; ++i) {
        tokens = getTokens(readLine());
        for (int j = 1, offset=1; j < frequencyCount; ++j) {
          int atomOffset = firstModelAtom+j*atomCount + i ;
          Atom atom = atoms[atomOffset];
          x = parseFloat(tokens[offset++]);
          y = parseFloat(tokens[offset++]);
          z = parseFloat(tokens[offset++]);
          atom.addVibrationVector(x, y, z);
        }
      }
      // Position the reader to have the next frequencies already tokenized
      while ((line= readLine()) != null && line.length() > 0) { }
      // I am now either at the next Frequency line or Mode line or STANDARD
      line=readLine();
      if (line.indexOf("STANDARD")>=0) {
        break;  // we are done with the frequencies
      } else if (line.indexOf(" Frequency:") == -1) {
        frequencies = getTokens(discardLinesUntilStartsWith(" Frequency:"));
      } else {
        frequencies = getTokens(line);
      }
    }
  }

  void readPartialCharges() throws Exception {
    discardLines(3);
    Atom[] atoms = atomSetCollection.getAtoms();
    int atomCount = atomSetCollection.getLastAtomSetAtomCount();
    for (int i = 0; i < atomCount && readLine() != null; ++i)
      atoms[i].partialCharge = parseFloat(getTokens()[2]);
  }
}
