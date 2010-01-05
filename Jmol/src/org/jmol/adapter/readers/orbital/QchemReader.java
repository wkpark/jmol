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

package org.jmol.adapter.readers.orbital;

import org.jmol.adapter.smarter.*;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A reader for Q-Chem 2.1 and 3.2
 * Q-Chem  is a quantum chemistry program developed
 * by Q-Chem, Inc. (http://www.q-chem.com/)
 *
 * <p> Molecular coordinates, normal coordinates of
 * vibrations and MOs are read.
 * 
 *  <p> In order to get the output required for MO reading
 *  make sure that the $rem block has<p>
 * <code>print_general_basis TRUE<br>  print_orbitals TRUE</code>
 *
 * <p> This reader was developed from only a few
 * output files, and therefore, is not guaranteed to
 * properly read all Q-chem output. If you have problems,
 * please contact the author of this code, not the developers
 * of Q-chem.
 *
 * <p> This is a hacked version of Miguel's GaussianReader
 *
 * @author Rene P.F Kanters (rkanters@richmond.edu)
 * @version 1.1
 * 
 * @author Steven E. Wheeler (swheele2@ccqc.uga.edu)
 * @version 1.0
 * 
*/

public class QchemReader extends MOReader {
 
/** The number of the calculation being interpreted. */
  private int calculationNumber = 1;

  MOInfo[] alphas = null;
  MOInfo[] betas = null;
  int nShell = 0;          // # of shells according to qchem
  int nBasis = 0;          // # of basis according to qchem

  
  public void readAtomSetCollection(BufferedReader reader) {
    readAtomSetCollection(reader, "qchem");
  }

  /**
   * @return true if need to read new line
   * @throws Exception
   * 
   */
  protected boolean checkLine() throws Exception {
    if (line.indexOf("Standard Nuclear Orientation") >= 0) {
      readAtoms();
      moData = null; // no MO data for this structure
      return true;
    }
    if (line.indexOf("Requested basis set is") >= 0) {
      readCalculationType();
      return true;
    }
    if (line.indexOf("VIBRATIONAL FREQUENCIES") >= 0) {
      readFrequencies();
      return true;
    }
    if (line.indexOf("Mulliken Net Atomic Charges") >= 0) {
      readPartialCharges();
      return true;
    }
    if (line.startsWith("Job ")) {
      calculationNumber++;
      moData = null; // start 'fresh'
      return true;
    }
    if (line.indexOf("Basis set in general basis input format") >= 0) {
      if (moData == null) {
        // only read the first basis (not basis2)
        readBasis();
      }
      return true;
    }
    if (moData == null)
      return true;
    if (line.indexOf("Orbital Energies (a.u.) and Symmetries") >= 0) {
      if (filterMO())
        readESym(true);
      return true;
    }
    if (line.indexOf("Orbital Energies (a.u.)") >= 0) {
      if (filterMO())
        readESym(false);
      return true;
    }
    if (line.indexOf("MOLECULAR ORBITAL COEFFICIENTS") >= 0) {
      if (filterMO())
        readQchemMolecularOrbitals();
      return true;
    }
    return checkNboLine();
  }

  private void readCalculationType() {
    calculationType = line.substring(line.indexOf("set is") + 6).trim();
  }


/* Q-chem 2.1 format:
       Standard Nuclear Orientation (Angstroms)
    I     Atom         X            Y            Z
 ----------------------------------------------------
    1      H       0.000000     0.000000     4.756791
*/

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
   * <p>
   * The vectors are added to a clone of the last read AtomSet. Only the
   * Frequencies, reduced masses, force constants and IR intensities are set as
   * properties for each of the frequency type AtomSet generated.
   * 
   * @throws Exception
   *           If no frequences were encountered
   * @throws IOException
   *           If an I/O error occurs
   **/
  private void readFrequencies() throws Exception, IOException {
    while (readLine() != null && line.indexOf("STANDARD") < 0) {
      if (!line.startsWith(" Frequency:"))
        discardLinesUntilStartsWith(" Frequency:");
      String[] frequencies = getTokens();
      int frequencyCount = frequencies.length - 1;
      boolean[] ignore = new boolean[frequencyCount];
      int atomCount = atomSetCollection.getLastAtomSetAtomCount();
      int iAtom0 = atomSetCollection.getAtomCount();
      for (int i = 0; i < frequencyCount; ++i) {
        ignore[i] = !doGetVibration(++vibrationNumber);
        if (ignore[i])
          continue;
        atomSetCollection.cloneLastAtomSet();
        atomSetCollection.setAtomSetName(frequencies[i + 1] + " cm^-1");
        // set the properties
        atomSetCollection.setAtomSetProperty("Frequency", frequencies[i + 1]
            + " cm^-1");
        atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
            "Calculation " + calculationNumber
                + SmarterJmolAdapter.PATH_SEPARATOR + "Frequencies");
      }

      // position to start reading the displacement vectors
      discardLinesUntilStartsWith("               X");
      fillFrequencyData(iAtom0, atomCount, ignore, true, 0, 0);
      discardLinesUntilBlank();
    }
  }

  void readPartialCharges() throws Exception {
    discardLines(3);
    Atom[] atoms = atomSetCollection.getAtoms();
    int atomCount = atomSetCollection.getLastAtomSetAtomCount();
    for (int i = 0; i < atomCount && readLine() != null; ++i)
      atoms[i].partialCharge = parseFloat(getTokens()[2]);
  }


/* SAMPLE BASIS OUTPUT for a cartesian basis set
 * if using pure the same shells are there, but nbasis is 18 (one less)
 * (because of only 5 d orbitals on O).

Basis set in general basis input format:
-----------------------------------------------------------------------
$basis
O    0
S    6    1.000000
   5.48467170E+03    1.83110000E-03 
   8.25234950E+02    1.39501000E-02 
   1.88046960E+02    6.84451000E-02 
   5.29645000E+01    2.32714300E-01 
   1.68975700E+01    4.70193000E-01 
   5.79963530E+00    3.58520900E-01 
SP   3    1.000000
   1.55396160E+01   -1.10777500E-01   7.08743000E-02 
   3.59993360E+00   -1.48026300E-01   3.39752800E-01 
   1.01376180E+00    1.13076700E+00   7.27158600E-01 
SP   1    1.000000
   2.70005800E-01    1.00000000E+00   1.00000000E+00 
D    1    1.000000
   8.00000000E-01    1.00000000E+00 
****
H    0
S    3    1.000000
   1.87311370E+01    3.34946000E-02 
   2.82539370E+00    2.34726950E-01 
   6.40121700E-01    8.13757330E-01 
S    1    1.000000
   1.61277800E-01    1.00000000E+00 
****
H    0
S    3    1.000000
   1.87311370E+01    3.34946000E-02 
   2.82539370E+00    2.34726950E-01 
   6.40121700E-01    8.13757330E-01 
S    1    1.000000
   1.61277800E-01    1.00000000E+00 
****
$end
-----------------------------------------------------------------------
 There are 8 shells and 19 basis functions

 * Since I don't know beforehand whether or not we use spherical or cartesians
 * I need to keep track of which shell and orbitals is where in the sdata
 * That way when I read the MOs I can see which shell goes where. 
 */

  private void readBasis() throws Exception {
    // initialize the 'global' variables
    moData = new Hashtable();
    int atomCount = 0;
    int shellCount = 0;
    int gaussianCount = 0;
    // local variables
    Vector sdata = new Vector();
    Vector gdata = new Vector();
    String[] tokens;

    discardLinesUntilStartsWith("$basis");
    readLine(); // read the atom line
    while (readLine() != null) {  // read shell line
      if (line.startsWith("****")) {
        atomCount++;           // end of basis for an atom
        if (readLine() != null && line.startsWith("$end")) break;
        continue; // atom line has been read
      }
      shellCount++;
      int[] slater = new int[4];
      tokens = getTokens(line);
      slater[0] = atomCount;
      slater[1] = JmolAdapter.getQuantumShellTagID(tokens[0]); // default cartesian
      slater[2] = gaussianCount;
      int nGaussians = parseInt(tokens[1]);
      slater[3] = nGaussians;
      sdata.addElement(slater);
      gaussianCount += nGaussians;
      for (int i = 0; i < nGaussians; i++)
        gdata.addElement(getTokens(readLine()));     
    }
    // now rearrange the gaussians (direct copy from GaussianReader)
    float[][] garray = new float[gaussianCount][];
    for (int i = 0; i < gaussianCount; i++) {
      tokens = (String[]) gdata.get(i);
      garray[i] = new float[tokens.length];
      for (int j = 0; j < tokens.length; j++)
        garray[i][j] = parseFloat(tokens[j]);
    }
    moData.put("shells", sdata);
    moData.put("gaussians", garray);
    if (Logger.debugging) {
      Logger.debug(shellCount + " slater shells read");
      Logger.debug(gaussianCount + " gaussian primitives read");
    }
    discardLinesUntilStartsWith(" There are");
    tokens = getTokens(line);
    nShell = parseInt(tokens[2]);
    nBasis = parseInt(tokens[5]);
    moData.put("calculationType", calculationType);
  }

// since the orbital coefficients don't show the symmetry, I will read them here
  /* 
   * sample output for an unrestricted calculation
   * 
 --------------------------------------------------------------
             Orbital Energies (a.u.) and Symmetries
 --------------------------------------------------------------
 Warning : Irrep of orbital(   1) could not be determined
....
 Warning : Irrep of orbital(  86) could not be determined
 
 Alpha MOs, Unrestricted
 -- Occupied --                  
-10.446 -10.446 -10.446 -10.446 -10.412 -10.412  -1.100  -0.998
  0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   1 A1g   1 E1u                
....
 -0.611  -0.571  -0.569  -0.512  -0.479
  1 A2u   1 E2g   1 E2g   1 E1g   1 E1g                                        
 -- Virtual --                   
 -0.252  -0.226  -0.102  -0.076  -0.049  -0.039  -0.015  -0.006
  1 E2u   1 E2u   2 A1g   1 B2g   0 xxx   0 xxx   2 E2g   2 E2g                
....
  4.427
  5 B1u                                                                        
 Warning : Irrep of orbital(   1) could not be determined
....
 Warning : Irrep of orbital(  57) could not be determined
 
 Beta MOs, Unrestricted
 -- Occupied --                  
-10.442 -10.442 -10.441 -10.441 -10.413 -10.413  -1.088  -0.978
....
 -0.577  -0.569  -0.566  -0.473
  1 A2u   2 E2g   2 E2g   1 E1g                                                
 -- Virtual --                   
 -0.416  -0.214  -0.211  -0.100  -0.053  -0.047  -0.039  -0.008
  1 E1g   1 E2u   1 E2u   3 A1g   1 B2g   0 xxx   0 xxx   3 E2g                
 ....
  4.100   4.433
 10 E2g   6 B1u                                                                
 --------------------------------------------------------------
 

   * 
   * For a restricted open shell
   * 
 --------------------------------------------------------------
             Orbital Energies (a.u.) and Symmetries
 --------------------------------------------------------------
 Warning : Irrep of orbital(   1) could not be determined
....
 Warning : Irrep of orbital(  86) could not be determined
 
 Alpha MOs, Restricted
 -- Doubly Occupied --           
-10.446 -10.446 -10.445 -10.445 -10.413 -10.413  -1.099  -0.996
  0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   1 A1g   1 E1u                
....
 -0.609  -0.572  -0.570  -0.481
  1 A2u   2 E2g   2 E2g   1 E1g                                                
 -- Singly Occupied (Occupied) --
 -0.507
  1 E1g                                                                        
 -- Virtual --                   
 -0.248  -0.230  -0.102  -0.076  -0.049  -0.039  -0.015  -0.007
  1 E2u   1 E2u   2 A1g   1 B2g   0 xxx   0 xxx   3 E2g   3 E2g                
....
  4.427
  6 B1u                                                                        
 
 Beta MOs, Restricted
 -- Doubly Occupied --           
-10.443 -10.443 -10.442 -10.442 -10.413 -10.413  -1.088  -0.980
  0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   0 xxx   1 A1g   1 E1u                
....
 -0.578  -0.569  -0.566  -0.470
  1 A2u   2 E2g   2 E2g   1 E1g                                                
 -- Singly Occupied (Vacant) --  
 -0.421
  1 E1g                                                                        
 -- Virtual --                   
 -0.215  -0.212  -0.100  -0.055  -0.047  -0.038  -0.008  -0.004
  1 E2u   1 E2u   2 A1g   1 B2g   0 xxx   0 xxx   3 E2g   3 E2g                
....
  4.433
  6 B1u                                                                        
 --------------------------------------------------------------

   * 
   * For a restricted one : only need to read the alpha ones....
   * 
 --------------------------------------------------------------
             Orbital Energies (a.u.) and Symmetries
 --------------------------------------------------------------
 
 Alpha MOs, Restricted
 -- Occupied --                  
-10.187 -10.187 -10.187 -10.186 -10.186 -10.186  -0.847  -0.740
  1 A1g   1 E1u   1 E1u   1 E2g   1 E2g   1 B1u   2 A1g   2 E1u                
....
 -0.360  -0.340  -0.340  -0.247  -0.246
  1 A2u   3 E2g   3 E2g   1 E1g   1 E1g                                        
 -- Virtual --                   
  0.004   0.004   0.091   0.145   0.145   0.165   0.182   0.182
  1 E2u   1 E2u   4 A1g   4 E1u   4 E1u   1 B2g   4 E2g   4 E2g                
....
  4.668
 10 B1u                                                                        
 
 Beta MOs, Restricted
 -- Occupied --                  
-10.187 -10.187 -10.187 -10.186 -10.186 -10.186  -0.847  -0.740
  1 A1g   1 E1u   1 E1u   1 E2g   1 E2g   1 B1u   2 A1g   2 E1u                
....
 -0.360  -0.340  -0.340  -0.247  -0.246
  1 A2u   3 E2g   3 E2g   1 E1g   1 E1g                                        
 -- Virtual --                   
  0.004   0.004   0.091   0.145   0.145   0.165   0.182   0.182
  1 E2u   1 E2u   4 A1g   4 E1u   4 E1u   1 B2g   4 E2g   4 E2g                
....
  4.668
 10 B1u                                                                        
 --------------------------------------------------------------

   * 
   */
  protected void readESym(boolean haveSym) throws Exception {
    String[] tokens, spin = {"A","B"};
    alphas = new MOInfo[nBasis];
    betas = new MOInfo[nBasis];
    MOInfo[] moInfos;
    int ne=0;  // number of electrons for a particular series of orbitals
    boolean readBetas = false;

    discardLinesUntilStartsWith(" Alpha");
    tokens = getTokens(line); // initialize tokens for later as well
    moInfos = alphas;
    for (int e = 0; e < 2; e++) { // do for A and B electrons
      int nMO = 0;
      while (readLine() != null) { // will break out of loop
        if (line.startsWith(" -- ")) {
          ne = 0;
          if (line.indexOf("Vacant") < 0) {
            if (line.indexOf("Occupied") > 0) ne = 1;
          }
          readLine();
        }
        if (line.startsWith(" -------")) {
          e = 2; // pretend I did read beta whether it happened or not
          break; // done....
        }
        int nOrbs = getTokens(line).length;
        if (nOrbs == 0 || line.startsWith(" Warning")) { 
          discardLinesUntilStartsWith(" Beta"); // now the beta ones.
          readBetas = true;
          moInfos = betas;
          break;
        }
        if (haveSym) tokens = getTokens(readLine());
        for (int i=0, j=0; i < nOrbs; i++, j+=2) {
          MOInfo info = new MOInfo();
          info.ne = ne;
          info.label = spin[e];
          if (haveSym) info.symmetry = tokens[j]+tokens[j+1];
          moInfos[nMO] = info;
          nMO++;
        }
      }
    }
    if (!readBetas) betas=alphas; // no beta symmetry info: Restricted no sym
  }

/* Restricted orbitals cartesian see H2O-B3LYP-631Gd.out:
 * 
                        RESTRICTED (RHF) MOLECULAR ORBITAL COEFFICIENTS
                         1         2         3         4         5         6
 eigenvalues:        -19.138    -0.998    -0.517    -0.372    -0.291     0.063
   1  O     s        0.99286  -0.20950   0.00000  -0.08810   0.00000   0.10064
   2  O     s        0.02622   0.46921   0.00000   0.17726   0.00000  -0.11929
   3  O     px       0.00000   0.00000   0.51744   0.00000   0.00000   0.00001
   4  O     py       0.00000   0.00000   0.00000   0.00000   0.64458   0.00000
   5  O     pz      -0.00110  -0.12769   0.00000   0.55181   0.00000   0.28067
   6  O     s        0.01011   0.43952   0.00000   0.41043   0.00000  -1.25784
   7  O     px       0.00000   0.00000   0.26976   0.00000   0.00000   0.00001
   8  O     py       0.00000   0.00000   0.00000   0.00000   0.50605   0.00000
   9  O     pz       0.00000  -0.06065   0.00000   0.37214   0.00000   0.47747
  10  O     dxx     -0.00777   0.01878   0.00000   0.00088   0.00000   0.04509
  11  O     dxy      0.00000   0.00000   0.00000   0.00000   0.00000   0.00000
  12  O     dyy     -0.00772  -0.01094   0.00000  -0.00026   0.00000   0.05804
  13  O     dxz      0.00000   0.00000  -0.04127   0.00000   0.00000   0.00000
  14  O     dyz      0.00000   0.00000   0.00000   0.00000  -0.03544   0.00000
  15  O     dzz     -0.00775   0.01607   0.00000  -0.05242   0.00000   0.02731
  16  H 1   s        0.00037   0.13914  -0.23744  -0.14373   0.00000   0.09628
  17  H 1   s       -0.00103   0.00645  -0.14196  -0.11428   0.00000   0.96908
  18  H 2   s        0.00037   0.13914   0.23744  -0.14373   0.00000   0.09627
  19  H 2   s       -0.00103   0.00645   0.14195  -0.11428   0.00000   0.96905
                         7         8         9        10
 eigenvalues:          0.148     0.772     0.861     0.891
   1  O     s        0.00000   0.00000   0.03777   0.00000
....

 * and for pure d, H2O-B3LYP-631Gd_pure.out:
                        RESTRICTED (RHF) MOLECULAR ORBITAL COEFFICIENTS
                         1         2         3         4         5         6
 eigenvalues:        -19.130    -0.997    -0.516    -0.371    -0.290     0.065
   1  O     s        0.99505  -0.21173   0.00000  -0.08338   0.00000   0.08960
   2  O     s        0.02790   0.46512   0.00000   0.18751   0.00000  -0.15229
   3  O     px       0.00000   0.00000   0.51708   0.00000   0.00000   0.00001
   4  O     py       0.00000   0.00000   0.00000   0.00000   0.64424   0.00000
   5  O     pz      -0.00169  -0.12726   0.00000   0.55128   0.00000   0.28063
   6  O     s       -0.01316   0.46728   0.00000   0.34668   0.00000  -1.09467
   7  O     px       0.00000   0.00000   0.26985   0.00000   0.00000   0.00001
   8  O     py       0.00000   0.00000   0.00000   0.00000   0.50641   0.00000
   9  O     pz       0.00261  -0.06385   0.00000   0.37987   0.00000   0.46045
  10  O     d 1      0.00000   0.00000   0.00000   0.00000   0.00000   0.00000
  11  O     d 2      0.00000   0.00000   0.00000   0.00000  -0.03550   0.00000
  12  O     d 3     -0.00004   0.00813   0.00000  -0.03535   0.00000  -0.01590
  13  O     d 4      0.00000   0.00000  -0.04131   0.00000   0.00000   0.00000
  14  O     d 5     -0.00027   0.01732   0.00000   0.00046   0.00000  -0.00725
  15  H 1   s        0.00029   0.13911  -0.23753  -0.14352   0.00000   0.09663
  16  H 1   s        0.00298   0.00119  -0.14211  -0.10113   0.00000   0.93864
  17  H 2   s        0.00029   0.13911   0.23753  -0.14352   0.00000   0.09663
  18  H 2   s        0.00298   0.00119   0.14210  -0.10113   0.00000   0.93860

 * section finishes with an empty line containing only a space.
 * 
 * Since I could not determine from the basis information whether a shell
 * was cartesian or pure, I need to check this from the first time I go
 * through the AO's used
 */

  private void readQchemMolecularOrbitals() throws Exception {
    // since I can't get length of getShellOrder, I need to hardcode here
    // how many orbitals each shell has.
    int nOrbitalsPerShell[] = {1,3,4,6,5,10,7};
    /* reorder: value is offset that the ith AO should have in the shell
     * because of g03 order of orbitals expected.
     * g03:   XX, YY, ZZ, XY, XZ, YZ 
     * qchem: xx, xy, yy, xz, yz, zz : VERIFIED
     * g03:   d0, d1+, d1-, d2+, d2-
     * qchem: d 1=d2-, d 2=d1-, d 3=d0, d 4=d1+, d 5=d2+
     * g03:   XXX, YYY, ZZZ, XYY, XXY, XXZ, XZZ, YZZ, YYZ, XYZ
     * qchem: xxx, xxy, xyy, yyy, xxz, xyz, yyz, xzz, yzz, zzz
     * g03:   f0, f1+, f1-, f2+, f2-, f3+, f3-
     * qchem: f 1=f3-, f 2=f2-, f 3=f1-, f 4=f0, f 5=f1+, f 6=f2+, f 7=f3+
     * 
     * NB d 5 = d2+ show nothing...
     */
   int[][] reorder = {
        {0},
        {0, 1, 2},
        {0, 1, 2, 3},
        {0, 3, 1, 4, 5, 2},
        {4, 2, 0, 1, 3},
        {0, 4, 3, 1, 5, 9, 8, 6, 7, 2},
        {6, 4, 2, 0, 1, 3, 5}
    };
    float[] reordered = new float[10];
    int nMOs;  // total number of MOs that were read
    
    Vector orbitals = new Vector();
    String[] aoLabels = new String[nBasis];
    String orbitalType = getTokens(line)[0]; // is RESTRICTED or ALPHA
    nMOs = readMOs(orbitalType.equals("RESTRICTED"), aoLabels, orbitals, alphas);
    if (orbitalType.equals("ALPHA")) { // we also have BETA orbitals....
      discardLinesUntilContains("BETA");
      nMOs += readMOs(false, aoLabels, orbitals, betas);
    }
    // based on labels adjust the cartesian vs pure for the proper shells
    int iAO = 0; // index of first AO for a particular shell
    Vector sdata = (Vector) moData.get("shells");
    // also need the coefficients for easy access to reorder if needed
    float[][] mocoef = new float[nMOs][];
    for (int i = 0; i < nMOs; i++) { // get a reference to the mo coefficients
      Hashtable orb = (Hashtable) orbitals.get(i);
      mocoef[i] = (float[]) orb.get("coefficients");
    }    
    for (int i = 0; i < nShell; i++) {
      int[] slater = (int[]) sdata.get(i);
      if (getTokens(aoLabels[iAO]).length > 1 )  // is a spherical orbital
        slater[1] += slater[1] % 2; // only increment 1 if odd to make spherical
      int nOrbs = nOrbitalsPerShell[slater[1]];
      // only check reorder for slater >= SHELL_D_CARTESIAN
      if (slater[1] >= JmolAdapter.SHELL_D_CARTESIAN) {
        for (int j=0; j< nMOs; j++) {
          int[] order = reorder[slater[1]];
          for (int k=0, l=iAO; k < nOrbs; k++, l++)
            reordered[order[k]] = mocoef[j][l]; // read in proper order
          for (int k=0, l=iAO; k < nOrbs; k++, l++)
            mocoef[j][l] = reordered[k];        // now just set them
        }
      }
      iAO += nOrbs;
    }   
    moData.put("mos", orbitals);
    moData.put("energyUnits", "au");
    setMOData(moData);
  }

  private int readMOs(boolean restricted, String[] aoLabels,
                      Vector orbitals, MOInfo[] moInfos) throws Exception {
    Hashtable[] mos = new Hashtable[6];  // max 6 MO's per line
    float[][] mocoef = new float[6][];   // coefficients for each MO
    int[] moid = new int[6];             // mo numbers
    String[] tokens, energy;
    int nMOs = 0;
    
    while (readLine().length() > 2) {
      tokens = getTokens(line);
      int nMO = tokens.length;    // number of MO columns
      energy = getTokens(readLine().substring(13));
      for (int i = 0; i < nMO; i++) {
        moid[i] = parseInt(tokens[i])-1;
        mocoef[i] = new float[nBasis];
        mos[i] = new Hashtable();
      }
      for (int i = 0; i < nBasis; i++) {
        tokens = getTokens(readLine());
        aoLabels[i] = line.substring(12, 17); // collect the shell labels
        for (int j = tokens.length-nMO, k=0; k < nMO; j++, k++)
          mocoef[k][i] = parseFloat(tokens[j]);
      }
      // we have all the info we need 
      for (int i = 0; i < nMO; i++ ) {
        MOInfo moInfo = moInfos[moid[i]];
        mos[i].put("energy", new Float(energy[i]));
        mos[i].put("coefficients",mocoef[i]);
        String label = moInfo.label;
        int ne = moInfo.ne;
        if (restricted) ne = alphas[moid[i]].ne + betas[moid[i]].ne;
        mos[i].put("occupancy", new Float(ne));
        if (ne == 2) label = "AB";
        if (ne == 0) {
          if (restricted) label = "V";
          else label = "V"+label; // keep spin information for the orbital
        }
        mos[i].put("symmetry", moInfo.symmetry+" "+label +"("+(moid[i]+1)+")");
        orbitals.addElement(mos[i]);
      }
      nMOs += nMO;
    }
    return nMOs;
  }
  
  // inner class moInfo for storing occupancy and symmetry info from the
  // orbital energies and symmetrys block
  protected class MOInfo {
    int ne = 0;      // 0 or 1
    String label = "???";
    String symmetry = "???";
  }
}
