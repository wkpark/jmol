/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
package org.jmol.adapter.smarter;

import java.io.BufferedReader;

/**
 * Reads Mopac 93, 97 or 2002 output files, but was tested only
 * for Mopac 93 files yet.
 *
 * @author Egon Willighagen <egonw@jmol.org>
 */
class MopacReader extends AtomSetCollectionReader {
    
  String frameInfo;
  int baseAtomIndex;

  AtomSetCollection readAtomSetCollection(BufferedReader input) throws Exception {
    atomSetCollection = new AtomSetCollection("mopac");
        
    frameInfo = null;

    String line;
    while ((line = input.readLine()) != null && ! line.startsWith(" ---")) {
      if (line.indexOf("MOLECULAR POINT GROUP") >= 0)
        hasSymmetry = true;
    }

    while ((line = input.readLine()) != null) {
      if (line.indexOf("TOTAL ENERGY") >= 0)
        processTotalEnergy(line);
      else if (line.indexOf("ATOMIC CHARGES") >= 0)
        processAtomicCharges(input);
      else if (line.indexOf("CARTESIAN COORDINATES") >= 0 ||
               line.indexOf("ORIENTATION OF MOLECULE IN FORCE") >= 0)
        processCoordinates(input);
      else if (line.indexOf("NORMAL COORDINATE ANALYSIS") >= 0)
        processFrequencies(input);
    }
    return atomSetCollection;
  }
    
  void processTotalEnergy(String line) {
    frameInfo = line.trim();
  }

  /**
   * Reads the section in MOPAC files with atomic charges.
   * These sections look like:
   * <pre>
   *               NET ATOMIC CHARGES AND DIPOLE CONTRIBUTIONS
   * 
   *          ATOM NO.   TYPE          CHARGE        ATOM  ELECTRON DENSITY
   *            1          C          -0.077432        4.0774
   *            2          C          -0.111917        4.1119
   *            3          C           0.092081        3.9079
   * </pre>
   * They are expected to be found in the file <i>before</i> the 
   * cartesian coordinate section.
   */
  void processAtomicCharges(BufferedReader input) throws Exception {
    discardLines(input, 2);
    //    System.out.println("Reading atomic charges");
    baseAtomIndex = atomSetCollection.atomCount;
    int expectedAtomNumber = 0;
    String line;
    while ((line = input.readLine()) != null) {
      int atomNumber = parseInt(line);
      if (atomNumber == Integer.MIN_VALUE) // a blank line
        break;
      ++expectedAtomNumber;
      if (atomNumber != expectedAtomNumber)
        throw new Exception("unexpected atom number in atomic charges");
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = parseToken(line, ichNextParse);
      atom.partialCharge = parseFloat(line, ichNextParse);
    }
  }
    
  /**
   * Reads the section in MOPAC files with cartesian coordinates.
   * These sections look like:
   * <pre>
   *           CARTESIAN COORDINATES
   * 
   *     NO.       ATOM         X         Y         Z
   * 
   *      1         C        0.0000    0.0000    0.0000
   *      2         C        1.3952    0.0000    0.0000
   *      3         C        2.0927    1.2078    0.0000
   * </pre>
   */
  void processCoordinates(BufferedReader input) throws Exception {
    //    System.out.println("Reading coordinates");
    discardLines(input, 3);
    int expectedAtomNumber = 0;
    String line;
    while ((line = input.readLine()) != null) {
      int atomNumber = parseInt(line);
      if (atomNumber == Integer.MIN_VALUE) // blank line
        break;
      ++expectedAtomNumber;
      if (atomNumber != expectedAtomNumber)
        throw new Exception("unexpected atom number in coordinates");
      
      Atom atom = atomSetCollection.atoms[baseAtomIndex + atomNumber - 1];
      atom.x = parseFloat(line, 30);
      atom.y = parseFloat(line, 40);
      atom.z = parseFloat(line, 50);
    }
  }
  

  void processFrequencies(BufferedReader input) throws Exception {
    discardLines(input, 2);
  }


  /* void readFrequencies() throws IOException {
        
        String line;
        line = readLine(input);
        while (line.indexOf("Root No.") >= 0) {
            if (hasSymmetry) {
                readLine(input);
                readLine(input);
            }
            readLine(input);
            line = readLine(input);
            StringReader freqValRead = new StringReader(line.trim());
            StreamTokenizer token = new StreamTokenizer(freqValRead);
            
            Vector freqs = new Vector();
            while (token.nextToken() != StreamTokenizer.TT_EOF) {
                Vibration f = new Vibration(Double.toString(token.nval));
                freqs.addElement(f);
            }
            Vibration[] currentFreqs = new Vibration[freqs.size()];
            freqs.copyInto(currentFreqs);
            Object[] currentVectors = new Object[currentFreqs.length];
            
            line = readLine(input);
            for (int i = 0; i < mol.getAtomCount(); ++i) {
                line = readLine(input);
                StringReader vectorRead = new StringReader(line);
                token = new StreamTokenizer(vectorRead);
                
                // Ignore first token
                token.nextToken();
                for (int j = 0; j < currentFreqs.length; ++j) {
                    currentVectors[j] = new double[3];
                    if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                        ((double[]) currentVectors[j])[0] = token.nval;
                    } else {
                        throw new IOException("Error reading frequencies");
                    }
                }
                
                line = readLine(input);
                vectorRead = new StringReader(line);
                token = new StreamTokenizer(vectorRead);
                
                // Ignore first token
                token.nextToken();
                for (int j = 0; j < currentFreqs.length; ++j) {
                    if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                        ((double[]) currentVectors[j])[1] = token.nval;
                    } else {
                        throw new IOException("Error reading frequencies");
                    }
                }
                
                line = readLine(input);
                vectorRead = new StringReader(line);
                token = new StreamTokenizer(vectorRead);
                
                // Ignore first token
                token.nextToken();
                for (int j = 0; j < currentFreqs.length; ++j) {
                    if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                        ((double[]) currentVectors[j])[2] = token.nval;
                    } else {
                        throw new IOException("Error reading frequencies");
                    }
                    currentFreqs[j].addAtomVector((double[]) currentVectors[j]);
                }
            }
            for (int i = 0; i < currentFreqs.length; ++i) {
                mol.addVibration(currentFreqs[i]);
            }
            for (int i = 0; i < 15; ++i) {
                line = readLine(input);
                if ((line.trim().length() > 0) || (line.indexOf("Root No.") >= 0)) {
                    break;
                }
            }
        }
    } */
    
  // mth is getting rid of this
  // skip the line if the first character is a digit?
  // looks very strange to me
  /*           
  private String readLine(BufferedReader input) throws IOException {
    
    String line = input.readLine();
    while ((line != null) && (line.length() > 0)
           && Character.isDigit(line.charAt(0))) {
      line = input.readLine();
    }
    System.out.println("Read line: " + line);
    return line;
  }
  */
  
  
  /**
   * Whether the input file has symmetry elements reported.
   */
  private boolean hasSymmetry = false;
  
}
