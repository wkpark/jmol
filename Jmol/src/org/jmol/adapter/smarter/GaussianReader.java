/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 * modified by Rene Kanters to use the split functions as opposed to 
 *  index determined parsing...
 *
 * Copyright (C) 2004  The Jmol Development Team
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

package org.jmol.adapter.smarter;

import java.io.BufferedReader;

class GaussianReader extends ModelReader {
  // offset of atomic number in coordinate line
  private final static int STD_ORIENTATION_ATOMIC_NUMBER_OFFSET = 1;
  // the offset of the first X vector of the first frequency in the frequency output
  private final static int FREQ_FIRST_VECTOR_OFFSET = 2;

  // the default offset for the coordinate output is that for G98 or G03
  // if it turns out to be a G94 file, this will be reset.
  private int firstCoordinateOffset = 3;
  
  private int atomCount  = 0;  // the number of atoms in the last read model
  private int modelCount = 0;  // the number of models I have
  // it looks like model numbers really need to start with 1 and not 0 otherwise
  // a single frequency calculation can not go to the first frequency
  
  
  Model readModel(BufferedReader reader) throws Exception {

    model = new Model("gaussian");

    try {
      String line;
      int lineNum=0;
      while ((line = reader.readLine()) != null) {
        if (line.indexOf("Standard orientation:") >= 0) {
          modelCount++;     // reading the next model
          readAtoms(reader);
          System.out.println("Read model number " + modelCount);
        } else if (line.startsWith(" Harmonic frequencies")) {
          readFrequencies(reader);
        } else if (line.startsWith(" Total atomic charges:") ||
                   line.startsWith(" Mulliken atomic charges:")) {
          readPartialCharges(reader);
        } else if (lineNum < 20) {
          if (line.indexOf("This is part of the Gaussian 94(TM) system") >= 0)
            firstCoordinateOffset = 2;
        }
        lineNum++;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      model.errorMessage = "Could not read file:" + ex;
      return model;
    }
    if (model.atomCount == 0) {
      model.errorMessage = "No atoms in file";
    }
    return model;
  }

  /* GAUSSIAN STRUCTURAL INFORMATION THAT IS EXPECTED
     NB I currently use the firstCoordinateOffset value to determine where X starts,
     I could use the number of tokens - 3, and read the last 3...
  */

  // GAUSSIAN 04 format
  /* Standard orientation:
     ----------------------------------------------------------
     Center     Atomic              Coordinates (Angstroms)
     Number     Number             X           Y           Z
     ----------------------------------------------------------
     1          6           0.000000    0.000000    1.043880
     ##SNIP##    
     ---------------------------------------------------------------------
  */

  // GAUSSIAN 98 and 03 format
  /* Standard orientation:                         
     ---------------------------------------------------------------------
     Center     Atomic     Atomic              Coordinates (Angstroms)
     Number     Number      Type              X           Y           Z
     ---------------------------------------------------------------------
     1          6             0        0.852764   -0.020119    0.050711
     ##SNIP##
     ---------------------------------------------------------------------
  */

  private void readAtoms(BufferedReader reader) throws Exception {
    discardLines(reader, 4);
    String line;
    String tokens[];
    atomCount = 0; // we have no atoms for this model yet
    while ((line = reader.readLine()) != null &&
           !line.startsWith(" --")) {
      tokens = getTokens(line); // get the tokens in the line
      Atom atom = model.addNewAtom();
      atom.modelNumber = modelCount;  // associate that current model number
      atom.elementNumber = (byte)parseInt(tokens[STD_ORIENTATION_ATOMIC_NUMBER_OFFSET]);
      int offset = firstCoordinateOffset;
      atom.x = parseFloat(tokens[offset]);
      atom.y = parseFloat(tokens[++offset]);
      atom.z = parseFloat(tokens[++offset]);
      ++atomCount;
    }
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

  // NB RPFK now we can also have multiple geometries read before we encounter
  // the frequencies, so we can't set the modelNumber to 1 

  // If I were to put the frequency on the last model read I need to be
  // smarter about when I need to duplicate the last model (now I really do it always)
  // Maybe also should read the symmetry, frequencies and intensities of each....

  private void readFrequencies(BufferedReader reader) throws Exception {
    String line, tokens[];
    int nNewModels = 0;           // number of new models to make
    boolean firstTime = true;    // flag for first time through
    int modelNumber = modelCount; // tracks the first model the frequencies are for

    if (modelNumber < 1)
      throw (new Exception("Not structure read before frequencies encountered"));
    
    // get to the first set of frequencies
    while ((line = reader.readLine()) != null &&
           ! line.startsWith(" Frequencies --"))
      ;
    if (line == null)
      throw (new Exception("No frequencies encountered"));
      
    do {
      // FIXME deal with frequency line here
      tokens = getTokens(line);
      
      // determine the number of frequencies to interpret in this set of lines
      int nFreq = tokens.length - 2; // Frequencies and -- come in as two tokens
      System.out.println("Detected " + nFreq + " frequencies in line\n  "+line);
      
      // determine and duplicate the number of models needed to put the vectors on
      if (firstTime) {
        modelNumber--;        // use the first model to put the vectors on
        nNewModels = nFreq-1; // so I need to create 1 model less than the # of freqs
        firstTime = false;    // really do this only the first time
      } else {
        nNewModels = nFreq;   // I need to create new models for every frequency
      }

      for (int i = nNewModels; --i >= 0; )
        duplicateLastModel();
      
      // position to start reading the displacement vectors
      discardLinesUntilStartsWith(reader, " Atom AN");
      
      // read the displacement vectors for every atom and frequency
      for (int i = 0; i < atomCount; ++i) {
        tokens = getTokens(reader.readLine());
        int atomCenterNumber = parseInt(tokens[0]);
        for (int j = 0, offset = FREQ_FIRST_VECTOR_OFFSET; j < nFreq; ++j) {
          Atom atom = model.atoms[(modelNumber+j)*atomCount + atomCenterNumber-1];
          atom.vectorX = parseFloat(tokens[offset++]);
          atom.vectorY = parseFloat(tokens[offset++]);
          atom.vectorZ = parseFloat(tokens[offset++]);
        }
      }
      modelNumber += nFreq;
      discardLines(reader, 2);
    } while ((line = reader.readLine()) != null &&
             (line.startsWith(" Frequencies --"))); // more to be read
    // RPFK: why check for the " Frequencies --"? empty line denotes end of frequencies..
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
  void readPartialCharges(BufferedReader reader) throws Exception {
    discardLines(reader, 1);
    String charge;
    for (int i = 0; i < atomCount ; ++i)
      model.atoms[(modelCount-1)*atomCount+i].partialCharge =
        parseFloat(getTokens(reader.readLine())[2]);
  }

  // duplicate the last model 
  private void duplicateLastModel() {
    Atom[] atoms = model.atoms;
    int offset = atomCount*(modelCount-1);
    modelCount++;  // new count of models is increased
    for (int i = 0; i < atomCount; ++i) {
      Atom atomNew = model.newCloneAtom(atoms[offset+i]);
      atomNew.modelNumber = modelCount;  // associate the new model number with the atoms
    }
  }
}
