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

import org.jmol.api.ModelAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StreamTokenizer;
import java.util.StringTokenizer;

/**
 * Reads Mopac 93, 97 or 2002 output files, but was tested only
 * for Mopac 93 files yet.
 *
 * @author Bradley A. Smith <bradley@baysmith.com>
 * @author Egon Willighagen <egonw@jmol.org>
 */
class MopacReader extends ModelReader {
    
    Model readModel(BufferedReader input) throws Exception {
        model = new Model("mopac");
        
        String line;
        String frameInfo = null;
        int modelNumber = 1;
        while (input.ready()) {
            line = input.readLine();
            // System.out.println("Read line: " + line);
            if (line.startsWith(" --------------")) {
                // ok, start of results is found (at least for Mopac 93)
                break;
            } else if (line.indexOf("MOLECULAR POINT GROUP") >= 0) {
                hasSymmetry = true;
            }
        }
        float[] atomicCharges = new float[0];
        
        while (input.ready()) {
            line = input.readLine();
            if (line.indexOf("TOTAL ENERGY") >= 0) {
                frameInfo = line.trim();
            } else if (line.indexOf("ATOMIC CHARGES") >= 0) {
                for (int i = 0; i < 3; ++i) {
                    line = input.readLine();
                }
                atomicCharges = readAtomicCharges(input);
            } else if (line.trim().equals("CARTESIAN COORDINATES") ||
                       (line.indexOf("ORIENTATION OF MOLECULE IN FORCE CALCULATION") >= 0)) {
                for (int i = 0; i < 3; ++i) {
                    line = input.readLine();
                }
                readCoordinates(input, modelNumber, atomicCharges);
                atomicCharges = new float[0]; // consume charges
                modelNumber++;
            } else if (line.indexOf("NORMAL COORDINATE ANALYSIS") >= 0) {
                for (int i = 0; i < 2; ++i) {
                    line = input.readLine();
                }
                // readFrequencies(); Ignore for now
                break;
            }
        }
        return model;
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
     * The are expected to be found in the file <i>before</i> the 
     * cartesian coordinate section.
     */
    private float[] readAtomicCharges(BufferedReader input) throws Exception {
        System.out.println("Reading atomic charges");
        float[] charges = new float[50];
        String line;
        int chargeCounter = 0;
        while (input.ready()) {
            line = readLine(input);
            if (line.trim().length() == 0) {
                break;
            }
            StringReader sr = new StringReader(line);
            StreamTokenizer token = new StreamTokenizer(sr);
            
            // Ignore first token; must be a number.
            if (token.nextToken() != StreamTokenizer.TT_NUMBER) {
                break;
            }
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                // ok, an element number
            } else if (token.ttype == StreamTokenizer.TT_WORD) {
                // ok, an element symbol
            } else {
                throw new IOException("Error reading atomic charges: expected an element in the second token");
            }
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                if (chargeCounter == charges.length) {
                    charges = enlargeFloatArray(charges);
                }
                charges[chargeCounter] = (float)token.nval;
                chargeCounter++;
            } else {
                throw new IOException("Error reading atomic charges: expected a charge in the third token");
            }
        }
        return charges;
    }
    
    private float[] enlargeFloatArray(float[] charges) {
		float[] newarray = new float[charges.length + 50];
		System.arraycopy(charges, 0, newarray, 0, charges.length);
		return newarray;
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
    void readCoordinates(BufferedReader input, int modelNumber, float[] charges) throws IOException {
        System.out.println("Reading coordinates");
        
        String line;
        int atomCounter = 0;
        while (input.ready()) {
            line = readLine(input);
            if (line.trim().length() == 0) {
                break;
            }
            StringReader sr = new StringReader(line);
            StreamTokenizer token = new StreamTokenizer(sr);
            
            // Ignore first token; must be a number.
            if (token.nextToken() != StreamTokenizer.TT_NUMBER) {
                break;
            }
            Atom atom = model.addNewAtom();
            // atom.modelNumber = modelNumber;
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                atom.elementNumber = (byte) token.nval;
            } else if (token.ttype == StreamTokenizer.TT_WORD) {
                atom.elementSymbol = token.sval;
            } else {
                throw new IOException("Error reading coordinates");
            }
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                atom.x = (float)token.nval;
            } else {
                throw new IOException("Error reading coordinates");
            }
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                atom.y = (float)token.nval;
            } else {
                throw new IOException("Error reading coordinates");
            }
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                atom.z = (float)token.nval;
            } else {
                throw new IOException("Error reading coordinates");
            }
            // look up charge
            if (atomCounter < charges.length) {
                atom.partialCharge = charges[atomCounter];
            }
            atomCounter++;
        }
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
    
    private String readLine(BufferedReader input) throws IOException {
        
        String line = input.readLine();
        while ((line != null) && (line.length() > 0)
        && Character.isDigit(line.charAt(0))) {
            line = input.readLine();
        }
        System.out.println("Read line: " + line);
        return line;
    }
    
    /**
    * Whether the input file has symmetry elements reported.
    */
    private boolean hasSymmetry = false;
    
}
