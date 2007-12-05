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


import java.io.BufferedReader;

/**
 * A reader for NWChem 4.6
 * NWChem is a quantum chemistry program developed at
 * Pacific Northwest National Laboratory.
 *
 * <p>AtomSets will be generated for
 * output coordinates in angstroms,
 * energy gradients with vector information of the gradients,
 * and frequencies with an AtomSet for every separate frequency containing
 * vector information of the vibrational mode.
 * <p>Note that the different modules give quite different formatted output
 * so it is not certain that all modules will be properly interpreted.
 * Most testing has been done with the SCF and DFT tasks.
**/

public class NWChemReader extends AtomSetCollectionReader {

  /**
   * The number of the task begin interpreted.
   * <p>Used for the construction of the 'path' for the atom set.
   */
  private int taskNumber = 1;
  
  /**
   * The number of equivalent atom sets.
   * <p>Needed to associate identical properties to multiple atomsets
   */
  private int equivalentAtomSets = 0;
  
  /**
   * The type of energy last calculated.
   */
  private String energyKey = "";
  /**
   * The last calculated energy value.
   */
  private String energyValue = "";
  
  // need to remember a bit of the state of what was read before...
  private boolean converged;
  private boolean haveEnergy;
  private boolean haveAt;
  private boolean inInput;
 
 public AtomSetCollection readAtomSetCollection(BufferedReader reader)  {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("nwchem");
    init();
    try {
      while (readLine() != null) {
         if (line.startsWith("          Step")) {
           init();
        } else if (line.startsWith("      Symmetry information")) {
          readSymmetry();
        } else if (line.indexOf("Total") >= 0) {
          readTotal();          
        } else if (line.indexOf("@") >= 0) {
          readAtSign();
        } else if (line.startsWith("      Optimization converged")) {
          converged = true;
        } else if (line.indexOf("Output coordinates in angstroms") >= 0) {
          equivalentAtomSets++;
          readAtoms();
        } else if (line.indexOf("ENERGY GRADIENTS") >=0 ) {
          equivalentAtomSets++;
          readGradients();
        } else if (line.indexOf(
            "NWChem Nuclear Hessian and Frequency Analysis") >=0 ) {
          readFrequencies();
        } else if (line.startsWith(" Task  times")) {
          init();
          taskNumber++; // starting a new task
        } else if (line.trim().startsWith("NWChem")) {
          readNWChemLine();
        } else if (line.startsWith("  Mulliken analysis of the total density")) {
          // only do this if I have read an atom set in this task/step
          if (equivalentAtomSets > 0)
            readPartialCharges();
        }
      }
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }
  
  private void init() {
    haveEnergy = false;
    haveAt = false;
    converged = false;
    inInput = false;
    equivalentAtomSets = 0;
  }
  
  private void setEnergies(String key, String value, int nAtomSets) {
    energyKey = key;
    energyValue = value;
    atomSetCollection.setAtomSetProperties(
        energyKey, energyValue, equivalentAtomSets);
    atomSetCollection.setAtomSetNames(
        energyKey+" = "+energyValue, equivalentAtomSets);
    haveEnergy = true;
  }

  private void setEnergy(String key, String value) {
    energyKey = key;
    energyValue = value;
    atomSetCollection.setAtomSetProperty(energyKey, energyValue);
    atomSetCollection.setAtomSetName(energyKey+" = "+energyValue);
    haveEnergy = true;
  }

  /**
   * Read the symmetry information and set the property.
   * @throws Exception If an error occurs.
   */
  private void readSymmetry() throws Exception {
    discardLines(2);
    if (readLine() == null)
      return;
    String tokens[] = getTokens();
    atomSetCollection.setAtomSetProperties("Symmetry group name",
        tokens[tokens.length-1], equivalentAtomSets);
  }
  
  private void readNWChemLine() {
    // currently only keep track of whether I am in the input module or not.
    inInput = (line.indexOf("NWChem Input Module") >= 0);
  }
  
  /**
   * Interpret a line starting with a line with "Total" in it.
   * <p>Determine whether it reports the energy, if so set the property and name(s)
   */
  private void readTotal() {
    String tokens[] = getTokens();
    try {
      if (tokens[2].startsWith("energy")) {
        // in an optimization an energy is reported in a follow up step
        // that energy I don't want so only set the energy once
        if (!haveAt)
          setEnergies("E("+tokens[1]+")", tokens[tokens.length-1], equivalentAtomSets);
      }
    } catch (Exception e) {
      // ignore any problems in dealing with the line
    }
  }
  
  private void readAtSign() throws Exception {
    if (line.charAt(2)=='S') {
      discardLines(1); // skip over the line with the --- in it
      if (readLine() == null)
        return;
    }
    String tokens[] = getTokens();
    if (!haveEnergy) { // if didn't already have the energies, set them now
      setEnergies("E", tokens[2], equivalentAtomSets);
    } else {
      // @ comes after gradients, so 'reset' the energies for additional
      // atom sets that may be been parsed.
      setEnergies(energyKey, energyValue, equivalentAtomSets);
    }
    atomSetCollection.setAtomSetProperties("Step", tokens[1], equivalentAtomSets);
    haveAt = true;
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

  /**
   * Reads the output coordinates section into a new AtomSet.
   * @throws Exception If an error occurs.
   **/
  private void readAtoms() throws Exception {
    discardLines(3); // skip blank line, titles and dashes
    String tokens[];
    haveEnergy = false;
    atomSetCollection.newAtomSet();
    atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
        "Task "+taskNumber+
        (inInput?SmarterJmolAdapter.PATH_SEPARATOR+"Input":
         SmarterJmolAdapter.PATH_SEPARATOR+"Geometry"));
    while (readLine() != null && line.length() > 0) {
      tokens = getTokens(); // get the tokens in the line
      if (tokens.length < 6) break; // if don't have enough of them: done
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = fixTag(tokens[1]);
      atom.set(parseFloat(tokens[3]), parseFloat(tokens[4]), parseFloat(tokens[5]));
    }
    // only if was converged, use the last energy for the name and properties
    if (converged) {
      setEnergy(energyKey, energyValue);
      atomSetCollection.setAtomSetProperty("Step", "converged");
    } else if (inInput) {
      atomSetCollection.setAtomSetName("Input");
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
  /**
   * Reads the energy gradients section into a new AtomSet.
   *
   * <p>One could consider not adding a new AtomSet for this, but just
   * adding the gradient vectors to the last AtomSet read (if that was
   * indeed the same nuclear arrangement).
   * @throws Exception If an error occurs.
   **/
  private void readGradients() throws Exception {
    discardLines(3); // skip blank line, titles and dashes
    String tokens[];
    atomSetCollection.newAtomSet();
    if (equivalentAtomSets > 1)
      atomSetCollection.cloneLastAtomSetProperties();
    atomSetCollection.setAtomSetProperty("vector","gradient");
    atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
        "Task "+taskNumber+
        SmarterJmolAdapter.PATH_SEPARATOR+"Gradients");
   while (readLine() != null && line.length() > 0) {
      tokens = getTokens(); // get the tokens in the line
      if (tokens.length < 8) break; // make sure I have enough tokens
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = fixTag(tokens[1]);
      atom.set(parseFloat(tokens[2]), parseFloat(tokens[3]), parseFloat(tokens[4]));
      atom.scale(ANGSTROMS_PER_BOHR);
      // Keep gradients in a.u. (larger value that way)
      // need to multiply with -1 so the direction is in the direction the
      // atom needs to move to lower the energy
      atom.vectorX = -parseFloat(tokens[5]);
      atom.vectorY = -parseFloat(tokens[6]);
      atom.vectorZ = -parseFloat(tokens[7]);
    }
 }

  // SAMPLE FREQUENCY OUTPUT
  // First the structure. The atom column has real element names (not the tags)
  // units of X Y and Z in a.u.
  /*
   ---------------------------- Atom information ----------------------------
   atom    #        X              Y              Z            mass
   --------------------------------------------------------------------------
   O        1  9.5835700E-02  3.1863970E-07  0.0000000E+00  1.5994910E+01
   H        2 -9.8328438E-01  1.5498085E+00  0.0000000E+00  1.0078250E+00
   H        3 -9.8328460E-01 -1.5498088E+00  0.0000000E+00  1.0078250E+00
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
   
   1     0.03302     0.00000     0.00000     0.00000    -0.02102     0.23236
   2     0.08894     0.00000     0.00000     0.00000     0.22285     0.00752
   3     0.00000     0.00000     0.25004     0.00000     0.00000     0.00000
   4     0.52206     0.00000     0.00000     0.00000    -0.33418     0.13454
   5     0.42946     0.00000     0.00000     0.00000     0.00480    -0.06059
   6     0.00000     0.99611     0.00000     0.00000     0.00000     0.00000
   7    -0.45603     0.00000     0.00000     0.00000     0.29214     0.33018
   8     0.42946     0.00000     0.00000     0.00000     0.00480    -0.06059
   9     0.00000     0.00000     0.00000     0.99611     0.00000     0.00000

   7           8           9
   
   P.Frequency     1484.76     3460.15     3551.50
   
   1    -0.06910    -0.04713     0.00000
   2     0.00000     0.00000    -0.06994
   3     0.00000     0.00000     0.00000
   4     0.54837     0.37401    -0.38643
   5     0.39688    -0.58189     0.55498
   6     0.00000     0.00000     0.00000
   7     0.54837     0.37402     0.38641
   8    -0.39688     0.58191     0.55496
   9     0.00000     0.00000     0.00000



   ----------------------------------------------------------------------------
   Normal Eigenvalue ||    Projected Derivative Dipole Moments (debye/angs)
   Mode   [cm**-1]  ||      [d/dqX]             [d/dqY]           [d/dqZ]
   ------ ---------- || ------------------ ------------------ -----------------
   1        0.000 ||       0.159               2.123             0.000
   2        0.000 ||       0.000               0.000             2.480
   3        0.000 ||       0.000               0.000            -0.044
   4        0.000 ||       0.000               0.000             2.480
   5        0.000 ||      -0.101              -0.015             0.000
   6        0.000 ||       1.116              -0.303             0.000
   7     1484.764 ||       2.112               0.000             0.000
   8     3460.151 ||       1.877               0.000             0.000
   9     3551.497 ||       0.000               3.435             0.000
   ----------------------------------------------------------------------------



   
   
   ----------------------------------------------------------------------------
   Normal Eigenvalue ||           Projected Infra Red Intensities
   Mode   [cm**-1]  || [atomic units] [(debye/angs)**2] [(KM/mol)] [arbitrary]
   ------ ---------- || -------------- ----------------- ---------- -----------
   1        0.000 ||    0.196398           4.531       191.459      10.742
   2        0.000 ||    0.266537           6.149       259.833      14.578
   3        0.000 ||    0.000084           0.002         0.081       0.005
   4        0.000 ||    0.266537           6.149       259.833      14.578
   5        0.000 ||    0.000452           0.010         0.441       0.025
   6        0.000 ||    0.057967           1.337        56.509       3.170
   7     1484.764 ||    0.193384           4.462       188.520      10.577
   8     3460.151 ||    0.152668           3.522       148.828       8.350
   9     3551.497 ||    0.511498          11.801       498.633      27.976
   ----------------------------------------------------------------------------
   */

  /**
   * Reads the AtomSet and projected frequencies in the frequency section.
   *
   * <p>Attaches the vibration vectors of the projected frequencies to
   * duplicates of the atom information in the frequency section.
   * @throws Exception If an error occurs.
   **/
  private void readFrequencies() throws Exception {
    String tokens[];

    String path = "Task " + taskNumber + SmarterJmolAdapter.PATH_SEPARATOR
        + "Frequencies";

    // position myself to read the atom information, i.e., structure
    discardLinesUntilContains("Atom information");
    discardLines(2);
    atomSetCollection.newAtomSet();
    atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY, path);
    while (readLine() != null && line.indexOf("---") < 0) {
      tokens = getTokens();
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = fixTag(tokens[0]);
      atom.set(parseFloat(tokens[2]), parseFloat(tokens[3]), parseFloat(tokens[4]));
      atom.scale(ANGSTROMS_PER_BOHR);
    }

    // the first atomsetindex for the frequencies needed to add properties later
    int firstFrequencyAtomSetIndex = atomSetCollection.getCurrentAtomSetIndex();
    // the number of frequencies read
    int totalFrequencies = 0;
    // the number of atoms in each atomset that the vectors are added to
    int atomCount = atomSetCollection.getLastAtomSetAtomCount();
    // flag for first time: 1 model less to duplicate..
    boolean firstTime = true;

    // position myself to start reading the frequencies themselves
    discardLinesUntilContains("(Projected Frequencies expressed in cm-1)");
    discardLines(3); // step over the line with the numbers

    while (readLine() != null && line.indexOf("P.Frequency") >= 0) {

      tokens = getTokens(line, 12);

      // the number of frequencies to interpret in this set of lines
      int nFreq = tokens.length;
      // clone the last atom set nFreq-1 times the first time, later nFreq times.
      for (int fIndex = (firstTime ? 1 : 0); fIndex < nFreq; fIndex++) {
        atomSetCollection.cloneLastAtomSet();
        atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY, path);
      }
      firstTime = false;

      // assign the frequency values to each atomset's name and property
      for (int i = 0; i < nFreq; ++i) {
        int idx = firstFrequencyAtomSetIndex + totalFrequencies + i;
        String frequencyString = tokens[i] + " cm**-1";
        atomSetCollection.setAtomSetName(frequencyString, idx);
        atomSetCollection.setAtomSetProperty("Frequency", frequencyString, idx);
      }

      // firstModelAtom is the index in atomSetCollection.atoms that has the
      // first atom of the first model where the first to be read vibration
      // needs to go
      int firstModelAtom = atomSetCollection.getAtomCount() - nFreq * atomCount;

      discardLines(1); // skip over empty line

      // rows are frequency displacement for all frequencies
      // row index (i) 3n = x, 3n+1 = y, 3n+2=z
      Atom[] atoms = atomSetCollection.getAtoms();
      for (int i = 0; i < atomCount * 3; ++i) {
        if (readLine() == null)
          return;
        tokens = getTokens();
        for (int j = 0; j < nFreq; ++j) {
          Atom atom = atoms[firstModelAtom + j * atomCount
              + i / 3];
          float val = parseFloat(tokens[j + 1]);
          switch (i % 3) {
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
      totalFrequencies += nFreq;
      discardLines(3);
    }

    // now set the names and properties of the atomsets associated with
    // the frequencies
    // NB this is not always there: try/catch and possibly set freq value again  
    try {
      discardLinesUntilContains("Projected Infra Red Intensities");
      discardLines(2);
      for (int i = totalFrequencies, idx = firstFrequencyAtomSetIndex; --i >= 0; idx++) {
        if (readLine() == null)
          return;
        tokens = getTokens();
        String frequencyString = tokens[1] + " cm**-1";
        atomSetCollection.setAtomSetName(frequencyString, idx);
        atomSetCollection.setAtomSetProperty("Frequency", frequencyString, idx);
        atomSetCollection.setAtomSetProperty("IR Intensity", tokens[5]
            + " KM/mol", idx);
        //      atomSetCollection.setAtomSetProperty("vector","frequency");
      }
    } catch (Exception e) {
      // If exception was thrown, don't do anything here...
    }
  }
  
  /**
   * Reads partial charges and assigns them only to the last atom set. 
   * @throws Exception When an I/O error or discardlines error occurs
   */
  void readPartialCharges() throws Exception {
    String tokens[];
    discardLines(4);
    int atomCount = atomSetCollection.getAtomCount();
    int i0 = atomSetCollection.getLastAtomSetAtomIndex();
    Atom[] atoms = atomSetCollection.getAtoms();
    for (int i = i0; i < atomCount; ++i) {
      // first skip over the dummy atoms (not sure whether that really is needed..)
      while (atoms[i].elementNumber == 0)
        ++i;
      // assign the partial charge
      if (readLine() == null)
        return;
      tokens = getTokens();
      atoms[i].partialCharge = parseInt(tokens[2]) - parseFloat(tokens[3]);
    }
  }

  /**
   * Returns a modified identifier for a tag, so that the element can be determined
   * from it in the {@link Atom}.
   *<p> The result is that a tag that started with Bq (case insensitive) will
   * be renamed to have the Bq removed and '-Bq' appended to it.
   * <br>A tag consisting only of Bq (case insensitive) will return X. This
   * can happen in a frequency analysis.
   * 
   * @param tag the tag to be modified
   * @return a possibly modified tag
   **/
  private String fixTag(String tag) {
    String lctag = tag.toLowerCase();
    // make sure that Bq's are not interpreted as boron
    if (lctag.equals("bq"))
      return "X";
    if (lctag.startsWith("bq"))
      return tag.substring(2)+"-Bq";
    return tag;
  }
}
