/* $RCSfile$
 * $Author: nicove $
 * $Date: 2006-08-30 13:20:20 -0500 (Wed, 30 Aug 2006) $
 * $Revision: 5447 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
import org.jmol.util.Parser;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Jaguar reader tested for the two samples files in CVS. Both
 * these files were created with Jaguar version 4.0, release 20.
 */
public class JaguarReader extends AtomSetCollectionReader {

  int atomCount = 0;
  int moCount = 0;
  int gaussianCount = 0;
  float lumoEnergy = Float.MAX_VALUE;
  Hashtable moData = new Hashtable();
  Vector orbitals = new Vector();

  public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("jaguar");
    try {
      while (readLine() != null) {
        if (line.startsWith(" Input geometry:")) {
          readAtoms();
        } else if (line.startsWith(" Symmetrized geometry:")) {
          readAtoms();
        } else if (line.startsWith("  final geometry:")) {
          readAtoms();
        } else if (line.startsWith("  Atomic charges from electrostatic potential:")) {
          readCharges();
        } else if (line.startsWith("  number of basis functions....")) {
          moCount = parseInt(line.substring(32).trim());
        } else if (line.startsWith("  basis set:")) {
          moData.put("energyUnits", "");
          moData.put("calculationType", line.substring(13).trim());
        } else if (line.indexOf("Shell information") >= 0) {
          readBasis();
        } else if (line.indexOf("Normalized coefficients") >= 0) {
          readBasisNormalized();
        } else if (line.startsWith(" LUMO energy:")) {
          lumoEnergy = parseFloat(line.substring(13));
        } else if (line.indexOf("final wvfn") >= 0) {
          readMolecularOrbitals();
          if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
            Logger.debug(orbitals.size() + " molecular orbitals read");
          }
        } else if (line.startsWith("  harmonic frequencies in")) {
          readFrequencies();
          break;
        }
      }
      if (moCount > 0 && gaussianCount > 0) {
        atomSetCollection.setAtomSetAuxiliaryInfo("moData", moData);
      }
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }

  private void readAtoms() throws Exception {
    // we only take the last set of atoms before the frequencies
    atomSetCollection.discardPreviousAtoms();
    // start parsing the atoms
    discardLines(2);
    atomCount = 0;
    while (readLine() != null && line.length() >= 60 && line.charAt(2) != ' ') {
      String[] tokens = getTokens();
      String atomName = tokens[0];
      float x = parseFloat(tokens[1]);
      float y = parseFloat(tokens[2]);
      float z = parseFloat(tokens[3]);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)
          || atomName.length() < 2)
        return;
      String elementSymbol;
      char ch2 = atomName.charAt(1);
      if (ch2 >= 'a' && ch2 <= 'z')
        elementSymbol = atomName.substring(0, 2);
      else
        elementSymbol = atomName.substring(0, 1);
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = elementSymbol;
      atom.atomName = atomName;
      atom.x = x;
      atom.y = y;
      atom.z = z;
      atomCount++;
    }
  }

  /*

 Atom       C1           H2           H3           H4           O5      
 Charge    0.24969      0.04332     -0.02466     -0.02466     -0.65455
   
 Atom       H6      
 Charge    0.41085
  
   */
  private void readCharges() throws Exception {
   int iAtom = 0;
    while (readLine() != null && line.indexOf("sum") < 0) {
      if (line.indexOf("Charge") < 0)
        continue;
      String[] tokens = getTokens();
      for (int i = 1; i < tokens.length; i++)
        atomSetCollection.getAtom(iAtom++).partialCharge = parseFloat(tokens[i]);
    }
  }

  /*

  Gaussian Functions - Shell information
  
             s    j
             h    c  i       n
             e    o  s       f
             l    n  h       s
  atom       l    t  l  l    h          z              coef            rcoef
--------    ---  --- -- --  ---     ----------      ----------       ---------
 C1          1    6  0  1    0    3047.5248800       0.0018347       0.5363452
 C1          2   -1  0  1    0     457.3695180       0.0140373       0.9894521
 C1          3   -1  0  1    0     103.9486850       0.0688426       1.5972825

*/
  //private final static float ROOT3 = 1.73205080756887729f;

  void readBasis() throws Exception {
    String lastAtom = "";
    int iAtom = -1;
    int iShell = -1;
    int jCont = 0;
    discardLinesUntilContains("--------");
    Vector sdata = new Vector();
    Vector gdata = new Vector();
    gaussianCount = 0;
    String[] tokens;
    float factor = 1;
    while (readLine() != null && (tokens = getTokens()).length == 9) {
      jCont = parseInt(tokens[2]);
      if (jCont > 0) {
        iShell++;
        if (!tokens[0].equals(lastAtom))
          iAtom++;
        lastAtom = tokens[0];
        int iType = parseInt(tokens[4]);
        if (iType <= 2)
          iType--;  // s,p --> 0,1 because SP is 2
        int[] slater = new int[4];
        int nGaussians = jCont;
        slater[0] = iAtom;
        slater[1] = iType;
        slater[2] = gaussianCount;
        slater[3] = nGaussians;
        factor = 1;//(iType == 3 ? ROOT3 : 1);
        //System.out.println("slater: " + iAtom + " " + iType + " " + gaussianCount + " " + nGaussians);
        sdata.addElement(slater);
        gaussianCount += nGaussians;
        gdata.addElement(new float[] {parseFloat(tokens[6]), parseFloat(tokens[8]) * factor});
        for (int i = nGaussians -1; --i >= 0;) {
          tokens = getTokens(readLine());
          gdata.addElement(new float[] {parseFloat(tokens[6]), parseFloat(tokens[8]) * factor});
        }
      }
    }
    float[][] garray = new float[gaussianCount][];
    for (int i = 0; i < gaussianCount; i++)
      garray[i] = (float[]) gdata.get(i);
    moData.put("shells", sdata);
    moData.put("gaussians", garray);
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug(sdata.size() + " slater shells read");
      Logger.debug(gaussianCount + " gaussian primitives read");
    }
  }
  
  
/*
   Gaussian Functions - Normalized coefficients
   
   s
   h    t
   e    y
   l    p    f
   atom       l    e    n          z          rcoef        rmfac     rcoef*rmfac
   --------    ---  ---  ---     ---------    ----------   ----------  -----------
   C1          1    S    1    3047.524880     0.536345     1.000000     0.536345
   C1          2    S    1     457.369518     0.989452     1.000000     0.989452
   C1          3    S    1     103.948685     1.597283     1.000000     1.597283
   C1          4    S    1      29.210155     2.079187     1.000000     2.079187
   C1          5    S    1       9.286663     1.774174     1.000000     1.774174
   C1          6    S    1       3.163927     0.612580     1.000000     0.612580
   C1          7    S    2       7.868272    -0.399556     1.000000    -0.399556
   C1          8    S    2       1.881289    -0.184155     1.000000    -0.184155
   C1          9    S    2       0.544249     0.516390     1.000000     0.516390
   C1         10    X    3       7.868272     1.296082     1.000000     1.296082
                    Y    4                                 1.000000     1.296082
                    Z    5                                 1.000000     1.296082
   C1         11    X    3       1.881289     0.993754     1.000000     0.993754

   */
  void readBasisNormalized() throws Exception {
    
    //don't know what this is about yet -- asking Jimmy Stewart 
    
    if (true)
      return;
    String lastAtom = "";
    int iAtom = -1;
    discardLinesUntilContains("--------");
    float z = 0;
    float rCoef = 0;
    String id;

    while (readLine() != null && line.length() > 3) {
      String[] tokens = getTokens();
      if (tokens.length == 4) { //continuation
        id = tokens[0];
      } else {
        if (!tokens[0].equals(lastAtom))
          iAtom++;
        lastAtom = tokens[0];
        id = tokens[2];
        z = parseFloat(tokens[4]);
        rCoef = parseFloat(tokens[5]);
      }
    }
  }

  /*

   Occupied + virtual Orbitals- final wvfn
   
   ***************************************** 
   
   
   1         2         3         4         5
   eigenvalues-            -20.56138 -11.27642  -1.35330  -0.91170  -0.68016
   1 C1               S    0.00002   0.99583  -0.07294   0.17630   0.01918
   2 C1               S   -0.00028   0.02695   0.13608  -0.34726  -0.03173
   3 C1               X   -0.00014   0.00018   0.02808   0.00925   0.23168
   4 C1               Y    0.00033  -0.00073  -0.09792  -0.06147  -0.12659
   5 C1               Z    0.00003  -0.00013  -0.01570  -0.01416   0.08005

   
   
   */

  private void readMolecularOrbitals() throws Exception {
    String[][] dataBlock = new String[moCount][];
    readLine();
    readLine();
    readLine();
    int nMo = 0;
    while (line != null) {
      readLine();
      readLine();
      readLine();
      if (line == null || line.indexOf("eigenvalues-") < 0)
        break;
      String[] eigenValues = getTokens();
      int n = eigenValues.length - 1;
      fillDataBlock(dataBlock);
      for (int iOrb = 0; iOrb < n; iOrb++) {
        float[] coefs = new float[moCount];
        Hashtable mo = new Hashtable();
        float energy = parseFloat(eigenValues[iOrb + 1]);
        mo.put("energy", new Float(energy));
        if (Math.abs(energy - lumoEnergy) < 0.0001) {
          moData.put("HOMO", new Integer(nMo));
          lumoEnergy = Float.MAX_VALUE;
        }
        nMo++;
        for (int i = 0; i < moCount; i++)
          coefs[i] = parseFloat((String) dataBlock[i][iOrb + 3]);
        mo.put("coefficients", coefs);
        orbitals.addElement(mo);
      }
    }
    moData.put("mos", orbitals);
  }

  /* A block without symmetry, looks like:

   harmonic frequencies in cm**-1, IR intensities in km/mol, and normal modes:
   
   frequencies  1350.52  1354.79  1354.91  1574.28  1577.58  3047.10  3165.57
   intensities    14.07    13.95    13.92     0.00     0.00     0.00    25.19
   C1   X     0.00280 -0.11431  0.01076 -0.00008 -0.00001 -0.00028 -0.00406
   C1   Y    -0.00528  0.01062  0.11423 -0.00015 -0.00001 -0.00038  0.00850
   C1   Z     0.11479  0.00330  0.00502 -0.00006  0.00000  0.00007 -0.08748
   
   With symmetry:
   
   harmonic frequencies in cm**-1, IR intensities in km/mol, and normal modes:
   
   frequencies  1352.05  1352.11  1352.16  1574.91  1574.92  3046.33  3164.52
   symmetries   B3       B1       B3       A        A        A        B1      
   intensities    14.01    14.00    14.00     0.00     0.00     0.00    25.06
   C1   X     0.08399 -0.00233 -0.07841  0.00000  0.00000  0.00000 -0.01133
   C1   Y     0.06983 -0.05009  0.07631 -0.00001  0.00000  0.00000 -0.00283
   C1   Z     0.03571  0.10341  0.03519  0.00001  0.00000  0.00001 -0.08724
   */

  private void readFrequencies() throws Exception {
    int modelNumber = 1;
    while (readLine() != null && !line.startsWith("  frequencies ")) {
    }
    if (line == null)
      return;
    // determine number of freqs on this line (starting with "frequencies")
    do {
      int freqCount = Parser.countTokens(line, 0) - 1;
      while (readLine() != null && !line.startsWith("  intensities ")) {
      }
      for (int atomCenterNumber = 0; atomCenterNumber < atomCount; atomCenterNumber++) {
        // this assumes that the atoms are given in the same order as their
        // atomic coordinates, and disregards the label which is should use
        String[] tokensX = getTokens(readLine());
        String[] tokensY = getTokens(readLine());
        String[] tokensZ = getTokens(readLine());
        for (int j = 0; j < freqCount; j++)
          recordAtomVector(modelNumber + j, atomCenterNumber,
              parseFloat(tokensX[j + 2]), parseFloat(tokensY[j + 2]),
              parseFloat(tokensZ[j + 2]));
      }
      discardLines(1);
      modelNumber += freqCount;
    } while (readLine() != null && (line.startsWith("  frequencies ")));
  }

  private void recordAtomVector(int modelNumber, int atomCenterNumber, float x,
                                float y, float z) throws Exception {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)
        || atomCenterNumber <= 0 || atomCenterNumber > atomCount)
      return;
    if (atomCenterNumber == 1 && modelNumber > 1)
      atomSetCollection.cloneFirstAtomSet();
    Atom atom = atomSetCollection.getAtom((modelNumber - 1) * atomCount
        + atomCenterNumber - 1);
    atom.vectorX = x;
    atom.vectorY = y;
    atom.vectorZ = z;
  }
}
