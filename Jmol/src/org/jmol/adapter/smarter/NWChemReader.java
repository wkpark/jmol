/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
import java.util.StringTokenizer;

class NWChemReader extends ModelReader {

  // multiplication factor to go from AU to Angstrom
  private final static float AU2ANGSTROM = (float) (1.0/1.889725989);  
  
  private int atomCount  = 0;  // the number of atoms in the last read model
  private int modelCount = 0;  // the number of models I have  
  // it looks like model numbers really need to start with 1 and not 0 otherwise
  // a single frequency calculation can not go to the first frequency
  
  Model readModel(BufferedReader reader) throws Exception {

    model = new Model("nwchem");

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.indexOf("Output coordinates in angstroms") >= 0) {
//          System.out.println("Interpreting output coordinates in angstroms");
          readAtoms(reader);
        } else if (line.indexOf("ENERGY GRADIENTS") >=0 ) {
//          System.out.println("Interpreting ENERGY GRADIENTS");
          readGradients(reader);
        } else if (line.indexOf("NWChem Nuclear Hessian and Frequency Analysis") >=0 ) {
//          System.out.println("Interpreting Hessian and Frequency Analysis");
          readFrequencies(reader);
        }
        /* else if (line.startsWith(" Total atomic charges:") ||
                    line.startsWith(" Mulliken atomic charges:")) {
          readPartialCharges(reader);
        }
        */
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

// NWChem Output coordinates
/*
  Output coordinates in angstroms (scale by  1.889725989 to convert to a.u.)

  No.       Tag          Charge          X              Y              Z
 ---- ---------------- ---------- -------------- -------------- --------------
    1 O                    8.0000     0.00000000     0.00000000     0.14142136
    2 H                    1.0000     0.70710678     0.00000000    -0.56568542
    3 H                    1.0000    -0.70710678     0.00000000    -0.56568542

      Atomic Mass 
*/

  private void readAtoms(BufferedReader reader) throws Exception {
    discardLines(reader, 3); // skip blank line, titles and dashes
    String line;
    String tokens[];
    modelCount++;     // reading the next model
    atomCount = 0; // we have no atoms for this model yet
    while ( (line = reader.readLine()).length() > 0) {
      tokens = getTokens(line); // get the tokens in the line
			Atom atom = model.addNewAtom();
			atom.modelNumber = modelCount;  // associate that current model number
			atom.atomName = fixTag(tokens[1]);
			atom.x = parseFloat(tokens[3]);
			atom.y = parseFloat(tokens[4]);
			atom.z = parseFloat(tokens[5]);
			++atomCount;
    }
  }
  
// NWChem Gradients output
// The 'atom' is really a Tag (as above)
/*
                         UHF ENERGY GRADIENTS

    atom               coordinates                        gradient
                 x          y          z           x          y          z
   1 O       0.000000   0.000000   0.267248    0.000000   0.000000  -0.005967
   2 H       1.336238   0.000000  -1.068990   -0.064647   0.000000   0.002984
   3 H      -1.336238   0.000000  -1.068990    0.064647   0.000000   0.002984

*/
// NB one could consider removing the previous read structure since that
// must have been the input structure for the optimizition?
  private void readGradients(BufferedReader reader) throws Exception {
    discardLines(reader, 3); // skip blank line, titles and dashes
    String line;
    String tokens[];
    modelCount++;
    atomCount = 0; // we have no atoms for this model yet
    while ( (line = reader.readLine()).length() > 0) {
      tokens = getTokens(line); // get the tokens in the line
			Atom atom = model.addNewAtom();
			atom.modelNumber = modelCount;  // associate that current model number
			atom.atomName = fixTag(tokens[1]);
			atom.x = parseFloat(tokens[2])*AU2ANGSTROM;
			atom.y = parseFloat(tokens[3])*AU2ANGSTROM;
			atom.z = parseFloat(tokens[4])*AU2ANGSTROM;
			// Keep gradients in a.u. (larger value that way)
			// need to multiply with -1 so the direction is in the direction the
			// atom needs to move to lower the energy
			atom.vectorX = -parseFloat(tokens[5]);
			atom.vectorY = -parseFloat(tokens[6]);
			atom.vectorZ = -parseFloat(tokens[7]);
			++atomCount;
    }
  }


// SAMPLE FREQUENCY OUTPUT
// First the structure. The atom column has real element names (not the tags)
// units of X Y and Z in a.u.
/*
 ---------------------------- Atom information ----------------------------
     atom    #        X              Y              Z            mass
 --------------------------------------------------------------------------
    O        1  0.0000000E+00  0.0000000E+00  9.5835421E-02  1.5994910E+01
    H        2  1.5498088E+00  0.0000000E+00 -9.8328435E-01  1.0078250E+00
    H        3 -1.5498088E+00  0.0000000E+00 -9.8328435E-01  1.0078250E+00
 --------------------------------------------------------------------------


*/
// NB another header but with subhead (Frequencies expressed in cm-1)
// is in the output before this....
/*
          -------------------------------------------------
          NORMAL MODE EIGENVECTORS IN CARTESIAN COORDINATES
          -------------------------------------------------
             (Projected Frequencies expressed in cm-1)

                    1           2           3           4           5           6
 
 P.Frequency        0.00        0.00        0.00        0.00        0.00        0.00
 
           1     0.00000     0.00000     0.00000     0.23565    -0.01637     0.04279
           2     0.00000     0.25004     0.00000     0.00000     0.00000     0.00000
           3     0.00000     0.00000     0.00000     0.00000     0.22007     0.08421
           4     0.00000     0.00000     0.00000     0.23552     0.13016    -0.34017
           5     0.99611     0.00000     0.00000     0.00000     0.00000     0.00000
           6     0.00000     0.00000     0.00000    -0.00018     0.43052    -0.46580
           7     0.00000     0.00000     0.00000     0.23552     0.13016    -0.34017
           8     0.00000     0.00000     0.99611     0.00000     0.00000     0.00000
           9     0.00000     0.00000     0.00000     0.00018     0.00963     0.63422

                    7           8           9
 
 P.Frequency     1484.77     3460.15     3551.50
 
           1     0.00000     0.00000    -0.06994
           2     0.00000     0.00000     0.00000
           3    -0.06910    -0.04713     0.00000
           4     0.39688    -0.58190     0.55497
           5     0.00000     0.00000     0.00000
           6     0.54837     0.37401    -0.38642
           7    -0.39688     0.58190     0.55497
           8     0.00000     0.00000     0.00000
           9     0.54837     0.37401     0.38642



 ----------------------------------------------------------------------------
 Normal Eigenvalue ||    Projected Derivative Dipole Moments (debye/angs)
  Mode   [cm**-1]  ||      [d/dqX]             [d/dqY]           [d/dqZ]
 ------ ---------- || ------------------ ------------------ -----------------
    1        0.000 ||       0.000               2.480             0.000
    2        0.000 ||       0.000              -0.044             0.000
    3        0.000 ||       0.000               2.480             0.000
    4        0.000 ||       1.131               0.000             0.000
    5        0.000 ||       0.651               0.000             1.057
    6        0.000 ||      -1.701               0.000             0.404
    7     1484.765 ||       0.000               0.000             2.112
    8     3460.153 ||       0.000               0.000             1.877
    9     3551.497 ||       3.435               0.000             0.000
 ----------------------------------------------------------------------------
*/

  private void readFrequencies(BufferedReader reader) throws Exception {
    String line, tokens[];
    
    // position myself to read the atom information, i.e., structure
    discardLinesUntilContains(reader,"Atom information");
    discardLines(reader, 2);
    line = reader.readLine();
    modelCount++;            // new model
    atomCount = 0;           // start with 0 atoms...
    do {
      tokens = getTokens(line);
			Atom atom = model.addNewAtom();
			atom.modelNumber = modelCount;
			atom.elementSymbol = tokens[0];
			atom.x = parseFloat(tokens[2])*AU2ANGSTROM;
			atom.y = parseFloat(tokens[3])*AU2ANGSTROM;
			atom.z = parseFloat(tokens[4])*AU2ANGSTROM;
			++atomCount;
    } while ( ((line=reader.readLine()) != null) &&
              (line.indexOf("---") < 0) );  
    
    // now we are ready to put the vibrations on the structure(s)
    int modelNumber = modelCount; // the number of models before I start adding vectors
    boolean firstTime = true;     // flag for first time 1 model less to duplicate..
    int nNewModels;               // the number of models to duplicate

    // position myself to start reading the frequencies themselves
    discardLinesUntilContains(reader, "(Projected Frequencies expressed in cm-1)");
    discardLines(reader, 3); // step over the line with the numbers

    while ((line=reader.readLine())!=null && line.indexOf("P.Frequency") >= 0) {
      
      tokens = getTokens(line);      // FIXME deal with frequency line here

      // determine the number of frequencies to interpret in this set of lines
      int nFreq = tokens.length - 1; // P.Frequency comes in as a token
      
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

      // firstModelAtom is the index in model.atoms that has the first atom
      // of the first model where the first to be read vibration needs to go
      int firstModelAtom = model.atomCount - nFreq*atomCount;
      
      discardLines(reader, 1);      // skip over empty line
      
      // rows are frequency displacement for all frequencies
      // row index (i) 3n = x, 3n+1 = y, 3n+2=z
      for (int i = 0; i < atomCount*3; ++i) {
        line = reader.readLine();
        tokens = getTokens(line);
        for (int j = 0; j < nFreq; ++j) {
					Atom atom = model.atoms[firstModelAtom+j*atomCount + i/3];
					float val = parseFloat(tokens[j+1]);
					switch (i%3) {
						case 0:
							atom.vectorX = val;
							break;
						 case 1:
							 atom.vectorY = val;
							 break;
						 case 2:
							 atom.vectorZ = val;
					 }
        }
      }
      modelNumber += nFreq;
      discardLines(reader, 3);
    }
  }

  private String fixTag(String tag) {
    // make sure that Bq's are not interpreted as boron
    if (tag.toLowerCase().startsWith("bq"))
      return tag.substring(2)+"-Bq";
    return tag;
  }

// duplicate the last model 
  private void duplicateLastModel() {
    Atom[] atoms = model.atoms;
    int offset = model.atomCount - atomCount;
    modelCount++;  // new count of models is increased
    for (int i = 0; i < atomCount; ++i) {
      Atom atomNew = model.newCloneAtom(atoms[offset+i]);
      atomNew.modelNumber = modelCount;  // associate the new model number with the atoms
    }
  }

}
