/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.adapter.smarter;

import java.io.BufferedReader;

/**
 * Reads Mopac 93, 97 or 2002 output files, but was tested only
 * for Mopac 93 files yet. (Miguel tweaked it to handle 2002 files,
 * but did not test extensively.)
 *
 * @author Egon Willighagen <egonw@jmol.org>
 */
class MopacReader extends AtomSetCollectionReader {
    
  String frameInfo;
  int baseAtomIndex;
  
  private boolean chargesFound = false;

  AtomSetCollection readAtomSetCollection(BufferedReader input) throws Exception {
    atomSetCollection = new AtomSetCollection("mopac");
        
    frameInfo = null;

    String line;
    while ((line = input.readLine()) != null && ! line.startsWith(" ---")) {
      if (line.indexOf("MOLECULAR POINT GROUP") >= 0) {
          // hasSymmetry = true;
      } else if (line.trim().equals("CARTESIAN COORDINATES")) {
          System.out.println("Found input structure...");
          processCoordinates(input);
          atomSetCollection.setAtomSetName("Input Structure");
      }
    }

    while ((line = input.readLine()) != null) {
      if (line.indexOf("TOTAL ENERGY") >= 0)
        processTotalEnergy(line);
      else if (line.indexOf("ATOMIC CHARGES") >= 0)
        processAtomicCharges(input);
      else if (line.trim().equals("CARTESIAN COORDINATES"))
        processCoordinates(input);
      else if (line.indexOf("ORIENTATION OF MOLECULE IN FORCE") >= 0) {
        processCoordinates(input);
        atomSetCollection.setAtomSetName("Orientation in Force Field");
      } else if (line.indexOf("NORMAL COORDINATE ANALYSIS") >= 0)
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
   * 
   * @param input
   * @throws Exception
   */
void processAtomicCharges(BufferedReader input) throws Exception {
    discardLines(input, 2);
    System.out.println("Reading atomic charges");
    atomSetCollection.newAtomSet(); // charges before coords, see JavaDoc
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
    System.out.println("#atoms" + atomSetCollection.atomCount);
    System.out.println("#models" + atomSetCollection.atomSetCount);
    chargesFound = true;
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
   * In a MOPAC2002 file the columns are different:
   * <pre>
   *          CARTESIAN COORDINATES
   *
   * NO.       ATOM           X             Y             Z
   *
   *  1         H        0.00000000    0.00000000    0.00000000
   *  2         O        0.95094500    0.00000000    0.00000000
   *  3         H        1.23995160    0.90598439    0.00000000
   * </pre>
   * 
   * @param input
   * @throws Exception
   */
  void processCoordinates(BufferedReader input) throws Exception {
    System.out.println("processCoordinates()");
    discardLines(input, 3);
    int expectedAtomNumber = 0;

    System.out.println("chargesFound: " + chargesFound);
    
    if (!chargesFound) {
      System.out.println("No model created yet, so doing so now...");
      atomSetCollection.newAtomSet();
      baseAtomIndex = atomSetCollection.atomCount;
    } else {
      chargesFound = false;
    }

    String line;
    while ((line = input.readLine()) != null) {
      System.out.println("Processing line: " + line);
      int atomNumber = parseInt(line);
      if (atomNumber == Integer.MIN_VALUE) // blank line
        break;
      ++expectedAtomNumber;
      if (atomNumber != expectedAtomNumber)
        throw new Exception("unexpected atom number in coordinates");
      String elementSymbol = parseToken(line, ichNextParse);

      Atom atom = atomSetCollection.atoms[baseAtomIndex + atomNumber - 1];
      if (atom == null) {
          System.out.println("No atom defined yet, creating one now...");
          atom = atomSetCollection.addNewAtom(); // if no charges were found first
      }
      atom.atomSerial = atomNumber;
      atom.elementSymbol = elementSymbol;
      atom.x = parseFloat(line, ichNextParse);
      atom.y = parseFloat(line, ichNextParse);
      atom.z = parseFloat(line, ichNextParse);
      System.out.println(atom.elementSymbol + " " + atom.x + " " + atom.y + " " + atom.z);
    }
    System.out.println("#atoms" + atomSetCollection.atomCount);
    System.out.println("#models" + atomSetCollection.atomSetCount);
    System.out.println("chargesFound: " + chargesFound);
    System.out.println("processCoordinates(END)");
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
  //private boolean hasSymmetry = false;
  
}
