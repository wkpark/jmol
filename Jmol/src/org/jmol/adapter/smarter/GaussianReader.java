/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.adapter.smarter;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Reader for Gaussian 94/98/03 output files.
 *
 **/
class GaussianReader extends AtomSetCollectionReader {
  
  /**
   * Word index of atomic number in line with atom coordinates in an
   * orientation block.
   */
  private final static int STD_ORIENTATION_ATOMIC_NUMBER_OFFSET = 1;
  /**
   * Word index of the first X vector of the first frequency in the
   * frequency output.
   */
  private final static int FREQ_FIRST_VECTOR_OFFSET = 2;
  
  /**
   * The default offset for the coordinate output is that for G98 or G03.
   * If it turns out to be a G94 file, this will be reset.
   */
  private int firstCoordinateOffset = 3;
  
  /** Calculated energy with units (if possible). */
  private String energyString = "";
  /**
   * Type of energy calculated, e.g., E(RB+HF-PW91).
   */
  private String energyKey = "";
  
  /** The number of the calculation being interpreted. */
  private int calculationNumber = 1;
  
  /**  The scan point, where -1 denotes no scan information. */
  private int scanPoint = -1;
  
  /**
   * The number of equivalent atom sets.
   * <p>Needed to associate identical properties to multiple atomsets
   */
  private int equivalentAtomSets = 0;
  
  /**
   * Reads a Collection of AtomSets from a BufferedReader.
   *
   * <p>New AtomSets are generated when an <code>Input</code>,
   * <code>Standard</code> or <code>Z-Matrix</code> orientation is read.
   * The occurence of these orientations seems to depend on (in pseudo-code):
   * <code>
   *  <br>&nbsp;if (opt=z-matrix) Z-Matrix; else Input;
   *  <br>&nbsp;if (!NoSymmetry) Standard;
   * </code>
   * <br>Which means that if <code>NoSymmetry</code> is used with a z-matrix
   * optimization, no other orientation besides <code>Z-Matrix</code> will be
   * present.
   * This is important because <code>Z-Matrix</code> may have dummy atoms while
   * the analysis of the calculation results will not, i.e., the
   * <code>Center Numbers</code> in the z-matrix orientation may be different
   * from those in the population analysis!
   *
   * <p>Single point or frequency calculations always have an
   * <code>Input</code> orientation. If symmetry is used a
   * <code>Standard</code> will be present too.
   *
   * @param reader BufferedReader associated with the Gaussian output text.
   * @return The AtomSetCollection representing the interpreted Gaussian text.
   * @throws Exception If an error occurs
   **/
  AtomSetCollection readAtomSetCollection(BufferedReader reader)
  throws Exception {
    
    atomSetCollection = new AtomSetCollection("gaussian");
    
    try {
      String line;
      int lineNum = 0;

      while ((line = reader.readLine()) != null) {
        if (line.startsWith(" Step number")) {
          equivalentAtomSets = 0;
          // check for scan point information
          int scanPointIndex= line.indexOf("scan point");
          if (scanPointIndex>0) {
            scanPoint = parseInt(line,scanPointIndex+10);
          } else {
            scanPoint = -1; // no scan point information
          }
        } else if (line.indexOf("-- Stationary point found")>0) {
          // stationary point, if have scanPoint: need to increment now...
          // to get the initial geometry for the next scan point in the proper
          // place
          if (scanPoint>=0) scanPoint++;
        } else if (line.indexOf("Input orientation:") >= 0 ||
            line.indexOf("Z-Matrix orientation:") >= 0) {
          ++equivalentAtomSets;
          readAtoms(reader, line);
        } else if (line.indexOf("Standard orientation:") >= 0) {
          ++equivalentAtomSets;
          readAtoms(reader, line);
        } else if (line.startsWith(" Energy=")){
          setEnergy(line);
        }
        else if (line.startsWith(" SCF Done:")) {
          readSCFDone(reader,line);
        } else if (line.startsWith(" Harmonic frequencies")) {
          readFrequencies(reader);
        } else if (line.startsWith(" Total atomic charges:") ||
            line.startsWith(" Mulliken atomic charges:")) {
          // NB this only works for the Standard or Input orientation of
          // the molecule since it does not list the values for the
          // dummy atoms in the z-matrix
          readPartialCharges(reader);
        } else if (line.startsWith(" Normal termination of Gaussian")) {
          ++calculationNumber;
        } else if (lineNum < 20) {
          if (line.indexOf("This is part of the Gaussian 94(TM) system") >= 0)
            firstCoordinateOffset = 2;
        }
        lineNum++;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      atomSetCollection.errorMessage = "Could not read file:" + ex;
      return atomSetCollection;
    }
    if (atomSetCollection.atomCount == 0) {
      atomSetCollection.errorMessage = "No atoms in file";
    }
    
    return atomSetCollection;
  }
  
  /**
   * Interprets the SCF Done: section.
   *
   * <p>The energyKey and energyString will be set for further AtomSets that have
   * the same molecular geometry (e.g., frequencies).
   * The energy, convergence, -V/T and S**2 values will be set as properties
   * for the atomSet.
   *
   * @param reader BufferedReader associated with the Gaussian output text.
   * @param line The input line containing SCF Done:.
   * @throws Exception If an error occurs
   **/
  private void readSCFDone(BufferedReader reader, String line) throws Exception {
    String tokens[] = getTokens(line,11);
    energyKey = tokens[0];
    energyString = tokens[2]+" "+tokens[3];
    // now set the names for the last equivalentAtomSets
    atomSetCollection.setAtomSetNames(energyKey+" = " + energyString, equivalentAtomSets);
    // also set the properties for them
    atomSetCollection.setAtomSetProperties(energyKey, energyString, equivalentAtomSets);
    tokens = getTokens(reader.readLine());
    atomSetCollection.setAtomSetProperties(tokens[0], tokens[2], equivalentAtomSets);
    atomSetCollection.setAtomSetProperties(tokens[3], tokens[5], equivalentAtomSets);
    tokens = getTokens(reader.readLine());
    atomSetCollection.setAtomSetProperties(tokens[0], tokens[2], equivalentAtomSets);
  }
  
  /**
   * Interpret the Energy= line for non SCF type energy output
   *
   * @param line The input line containing Energy=
   */
  private void setEnergy(String line) {
    String tokens[] = getTokens(line);
    energyKey = "Energy";
    energyString = tokens[1];
    atomSetCollection.setAtomSetNames("Energy = "+tokens[1], equivalentAtomSets);
  }
  
  /* GAUSSIAN STRUCTURAL INFORMATION THAT IS EXPECTED
   NB I currently use the firstCoordinateOffset value to determine where
   X starts, I could use the number of tokens - 3, and read the last 3...
   */
  
  // GAUSSIAN 04 format
  /*                 Standard orientation:
   ----------------------------------------------------------
   Center     Atomic              Coordinates (Angstroms)
   Number     Number             X           Y           Z
   ----------------------------------------------------------
   1          6           0.000000    0.000000    1.043880
   ##SNIP##    
   ---------------------------------------------------------------------
   */
  
  // GAUSSIAN 98 and 03 format
  /*                    Standard orientation:                         
   ---------------------------------------------------------------------
   Center     Atomic     Atomic              Coordinates (Angstroms)
   Number     Number      Type              X           Y           Z
   ---------------------------------------------------------------------
   1          6             0        0.852764   -0.020119    0.050711
   ##SNIP##
   ---------------------------------------------------------------------
   */
  
  private void readAtoms(BufferedReader reader, String line) throws Exception {
    atomSetCollection.newAtomSet();
    atomSetCollection.setAtomSetName(""); // start with an empty name
    String path = getTokens(line)[0]; // path = type of orientation
    discardLines(reader, 4);
    String tokens[];
    while ((line = reader.readLine()) != null &&
        !line.startsWith(" --")) {
      tokens = getTokens(line); // get the tokens in the line
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementNumber =
        (byte)parseInt(tokens[STD_ORIENTATION_ATOMIC_NUMBER_OFFSET]);
      if (atom.elementNumber < 0)
        atom.elementNumber = 0; // dummy atoms have -1 -> 0
      int offset = firstCoordinateOffset;
      atom.x = parseFloat(tokens[offset]);
      atom.y = parseFloat(tokens[++offset]);
      atom.z = parseFloat(tokens[++offset]);
    }
    atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
        "Calculation "+calculationNumber+
        (scanPoint>=0?(SmarterJmolAdapter.PATH_SEPARATOR+"Scan Point "+scanPoint):"")+
        SmarterJmolAdapter.PATH_SEPARATOR+path);
    // always make sure that I have a name for the atomset
    // mostly needed if no "SCF Done" line follows the structure (e.g., last one
    // in a scan...)
    atomSetCollection.setAtomSetName("Last read atomset.");
  }
  
  /* SAMPLE FREQUENCY OUTPUT */
  /*
   Harmonic frequencies (cm**-1), IR intensities (KM/Mole), Raman scattering
   activities (A**4/AMU), depolarization ratios for plane and unpolarized
   incident light, reduced masses (AMU), force constants (mDyne/A),
   and normal coordinates:
                       1                      2                      3
                      A1                     B2                     B1
   Frequencies --    64.6809                64.9485               203.8241
   Red. masses --     8.0904                 2.2567                 1.0164
   Frc consts  --     0.0199                 0.0056                 0.0249
   IR Inten    --     1.4343                 1.4384                15.8823
   Atom AN      X      Y      Z        X      Y      Z        X      Y      Z
   1   6     0.00   0.00   0.48     0.00  -0.05   0.23     0.01   0.00   0.00
   2   6     0.00   0.00   0.48     0.00  -0.05  -0.23     0.01   0.00   0.00
   3   1     0.00   0.00   0.49     0.00  -0.05   0.63     0.03   0.00   0.00
   4   1     0.00   0.00   0.49     0.00  -0.05  -0.63     0.03   0.00   0.00
   5   1     0.00   0.00  -0.16     0.00  -0.31   0.00    -1.00   0.00   0.00
   6  35     0.00   0.00  -0.16     0.00   0.02   0.00     0.01   0.00   0.00
   ##SNIP##
                      10                     11                     12
                      A1                     B2                     A1
   Frequencies --  2521.0940              3410.1755              3512.0957
   Red. masses --     1.0211                 1.0848                 1.2333
   Frc consts  --     3.8238                 7.4328                 8.9632
   IR Inten    --   264.5877               109.0525                 0.0637
   Atom AN      X      Y      Z        X      Y      Z        X      Y      Z
   1   6     0.00   0.00   0.00     0.00   0.06   0.00     0.00  -0.10   0.00
   2   6     0.00   0.00   0.00     0.00   0.06   0.00     0.00   0.10   0.00
   3   1     0.00   0.01   0.00     0.00  -0.70   0.01     0.00   0.70  -0.01
   4   1     0.00  -0.01   0.00     0.00  -0.70  -0.01     0.00  -0.70  -0.01
   5   1     0.00   0.00   1.00     0.00   0.00   0.00     0.00   0.00   0.00
   6  35     0.00   0.00  -0.01     0.00   0.00   0.00     0.00   0.00   0.00
   
   -------------------
   - Thermochemistry -
   -------------------
   */
  
  /**
   * Interprets the Harmonic frequencies section.
   *
   * <p>The vectors are added to a clone of the last read AtomSet.
   * Only the Frequencies, reduced masses, force constants and IR intensities
   * are set as properties for each of the frequency type AtomSet generated.
   *
   * @param reader BufferedReader associated with the Gaussian output text.
   * @throws Exception If no frequences were encountered
   * @throws IOException If an I/O error occurs
   **/
  private void readFrequencies(BufferedReader reader) throws Exception, IOException {
    String line;
    String[] tokens; String[] symmetries; String[] frequencies;
    String[] red_masses; String[] frc_consts; String[] intensities;
    
    while ((line = reader.readLine()) != null &&
        line.indexOf(":")<0) {
    }
    if (line == null)
      throw (new Exception("No frequencies encountered"));
    
    // G98 ends the frequencies with a line with a space (03 an empty line)
    // so I decided to read till the line is too short
    while ((line= reader.readLine()) != null &&
        line.length() > 15)
    {
      // we now have the line with the vibration numbers in them, but don't need it
      symmetries = getTokens(reader.readLine()); // read symmetry labels
      // TODO I should really read all the properties of the vibrations listed
      // and not limit myself to only IR type ones..
      frequencies = getTokens(discardLinesUntilStartsWith(reader," Frequencies"), 15);
      red_masses = getTokens(discardLinesUntilStartsWith(reader," Red. masses"), 15);
      frc_consts = getTokens(discardLinesUntilStartsWith(reader," Frc consts"), 15);
      intensities = getTokens(discardLinesUntilStartsWith(reader," IR Inten"), 15);
      
      int frequencyCount = frequencies.length;
      
      for (int i = 0; i < frequencyCount; ++i) {
        atomSetCollection.cloneLastAtomSet();
        atomSetCollection.setAtomSetName(
            symmetries[i] + " "
            + frequencies[i]+" cm**-1"
//            + ", Inten = " + intensities[i] + " KM/Mole "
//            + energyKey + " = " + energyString
        );
        // set the properties
        atomSetCollection.setAtomSetProperty(energyKey, energyString);
        atomSetCollection.setAtomSetProperty("Frequency",
            frequencies[i]+" cm**-1");
        atomSetCollection.setAtomSetProperty("Reduced Mass",
            red_masses[i]+" AMU");
        atomSetCollection.setAtomSetProperty("Force Constant",
            frc_consts[i]+" mDyne/A");
        atomSetCollection.setAtomSetProperty("IR Intensity",
            intensities[i]+" KM/Mole");
        atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
            "Calculation " + calculationNumber+
            SmarterJmolAdapter.PATH_SEPARATOR+"Frequencies");
      }
      
      int atomCount = atomSetCollection.getLastAtomSetAtomCount();
      int firstModelAtom =
        atomSetCollection.atomCount - frequencyCount * atomCount;
      
      // position to start reading the displacement vectors
      discardLinesUntilStartsWith(reader, " Atom AN");
      
      // read the displacement vectors for every atom and frequency
      for (int i = 0; i < atomCount; ++i) {
        tokens = getTokens(reader.readLine());
        int atomCenterNumber = parseInt(tokens[0]);
        for (int j = 0, offset=FREQ_FIRST_VECTOR_OFFSET;
        j < frequencyCount; ++j) {
          int atomOffset = firstModelAtom+j*atomCount + atomCenterNumber - 1 ;
          Atom atom = atomSetCollection.atoms[atomOffset];
          atom.vectorX = parseFloat(tokens[offset++]);
          atom.vectorY = parseFloat(tokens[offset++]);
          atom.vectorZ = parseFloat(tokens[offset++]);
        }
      }
    }
  }
  
  /* SAMPLE Mulliken Charges OUTPUT from G98 */
  /*
   Mulliken atomic charges:
   1
   1  C   -0.238024
   2  C   -0.238024
   ###SNIP###
   6  Br  -0.080946
   Sum of Mulliken charges=   0.00000
   */
  
  /**
   * Reads partial charges and assigns them only to the last atom set. 
   * @param reader The reader from which to read the charges
   * @throws Exception When an I/O error or discardlines error occurs
   */
  // TODO this really should set the charges for the last nOrientations read
  // being careful about the dummy atoms...
  void readPartialCharges(BufferedReader reader) throws Exception {
    discardLines(reader, 1);
    for (int i = atomSetCollection.getLastAtomSetAtomIndex();
    i < atomSetCollection.atomCount;
    ++i) {
      // first skip over the dummy atoms
      while (atomSetCollection.atoms[i].elementNumber == 0)
        ++i;
      // assign the partial charge
      atomSetCollection.atoms[i].partialCharge =
        parseFloat(getTokens(reader.readLine())[2]);
    }
  }
}
