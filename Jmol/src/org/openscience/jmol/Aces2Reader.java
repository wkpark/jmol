/*
 * Aces2Reader.java
 *
 * Copyright (C) 1999  Bradley A. Smith
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openscience.jmol;

import java.io.*;
import java.util.Vector;

/**
 * A reader for ACES II output.
 * ACES II is a quantum chemistry program by Rod Bartlett's
 * research group (http://www.qtp.ufl.edu/Aces2/).
 *
 * <p> Molecular coordinates, energies, and normal coordinates of
 * vibrations are read. Each set of coordinates is added to the
 * ChemFile in the order they are found. Energies and vibrations
 * are associated with the previously read set of coordinates.
 *
 * <p> This reader was developed from a small set of
 * example output files, and therefore, is not guaranteed to
 * properly read all Aces2 output. If you have problems,
 * please contact the author of this code, not the developers
 * of Aces2.
 *
 * @author Bradley A. Smith (yeldar@home.com)
 * @version 1.0
 */
public class Aces2Reader implements ChemFileReader {
    /**
     * Scaling factor for converting atomic coordinates from
     * units of Bohr to Angstroms.
     */
    public static final double angstromPerBohr = 0.529177249;
    
    /**
     * Create an Aces2 output reader.
     *
     * @param input source of Aces2 data
     */
    public Aces2Reader(Reader input) {
        this.input = new BufferedReader(input);
    }
    
    /**
     * Read the Aces2 output.
     *
     * @return a ChemFile with the coordinates, energies, and vibrations.
     * @exception IOException if an I/O error occurs
     */
    public ChemFile read() throws IOException, Exception {
        ChemFile file = new ChemFile();
        ChemFrame frame = null;
        String line = input.readLine();
        // Find first set of coordinates
        while (input.ready() && line != null) {
            if (line.indexOf("Z-matrix   Atomic") >= 0) {
				// Found a set of coordinates.
                frame = new ChemFrame();
                readCoordinates(frame);
                break;
            }
            line = input.readLine();
        }
        if (frame != null) {
            // Read all other data
            line = input.readLine();
            while (input.ready() && line != null) {
                if (line.indexOf("Z-matrix   Atomic") >= 0) {
                    // Found a set of coordinates.
                    // Add current frame to file and create a new one.
                    file.frames.addElement(frame);
                    frame = new ChemFrame();
                    readCoordinates(frame);
                } else  if (line.indexOf("E(SCF)") >= 0) {
                    // Found an energy
                    frame.setInfo(line.trim());
                } else  if (line.indexOf("Normal Coordinates") >= 0) {
                    // Found a set of vibrations
                    readFrequencies(frame);
                }
                line = input.readLine();
            }
            // Add current frame to file
            file.frames.addElement(frame);
        }
        return file;
    }
    
    /**
     * Reads a set of coordinates into ChemFrame.
     *
     * @param frame  the destination ChemFrame
     * @exception IOException  if an I/O error occurs
     */
    private void readCoordinates(ChemFrame frame) throws IOException, Exception {
        String line;
        line = input.readLine();
        line = input.readLine();
        while (input.ready()) {
            line = input.readLine();
            if (line == null || line.indexOf("-----") >= 0) {
                break;
            }
            int atomicNumber;
            StringReader sr = new StringReader(line);
            StreamTokenizer token = new StreamTokenizer(sr);
            token.nextToken();
            // ignore first token
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                atomicNumber = (int)token.nval;
                if (atomicNumber == 0) {
                    // skip dummy atoms
                    continue;
                }
            } else  throw new IOException(
                                          "Error reading coordinates at atomic number");
            double x;
            double y;
            double z;
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                x = token.nval * angstromPerBohr;
            } else  throw new IOException(
                                          "Error reading coordinates at x coordinate");
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                y = token.nval * angstromPerBohr;
			} else  throw new IOException(
                                                      "Error reading coordinates at y coordinate");
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                z = token.nval * angstromPerBohr;
            } else  throw new IOException(
                                          "Error reading coordinates at z coordinate");
            frame.addVert(atomicNumber, (float)x, (float)y, (float)z);
        }
    }
    
    /**
     * Reads a set of vibrations into ChemFrame.
     *
     * @param frame  the destination ChemFrame
     * @exception IOException  if an I/O error occurs
     */
    private void readFrequencies(ChemFrame frame) throws IOException, Exception {
        String line;
        line = input.readLine();
        line = input.readLine();
        line = input.readLine();
        while (input.ready()) {
            line = input.readLine();
            Vector currentVibs = new Vector();
            StringReader freqValRead = new StringReader(line.trim());
            StreamTokenizer token = new StreamTokenizer(freqValRead);
            while (token.nextToken() != StreamTokenizer.TT_EOF) {
                if (token.ttype == StreamTokenizer.TT_WORD) {
                    // Previous Vibration was imaginary.
                    // Add this token to its label.
                    Vibration vib = new Vibration(((Vibration)currentVibs.
                                                   lastElement()).getLabel() + ' ' + token.sval);
                    currentVibs.removeElementAt(currentVibs.size() - 1);
                    currentVibs.addElement(vib);
                    // Read next token for current Vibration value.
                    token.nextToken();
                }
                Vibration vib = new Vibration(Double.toString(token.nval));
                currentVibs.addElement(vib);
            }
            line = input.readLine();
            line = input.readLine();
            for (int i = 0; i < frame.getNvert(); ++i) {
                line = input.readLine();
                StringReader vectorRead = new StringReader(line);
                token = new StreamTokenizer(vectorRead);
                token.nextToken();
				// ignore first token
                for (int j = 0; j < currentVibs.size(); ++j) {
                    double[] v = new double[3];
                    if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                        v[0] = token.nval;
                    } else  throw new IOException(
                                                  "Error reading frequencies: first coordinate");
                    if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                        v[1] = token.nval;
                    } else  throw new IOException(
                                                  "Error reading frequencies: second coordinate");
                    if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                        v[2] = token.nval;
                    } else  throw new IOException(
                                                  "Error reading frequencies: third coordinate");
                    ((Vibration)currentVibs.elementAt(j)).addAtomVector(v);
                }
            }
            for (int i = 0; i < currentVibs.size(); ++i) {
                frame.addVibration((Vibration)currentVibs.elementAt(i));
            }
            line = input.readLine();
            if (line.indexOf("Gradient vector") >= 0)  break;
            line = input.readLine();
            if (line.indexOf("Normal modes") >= 0)  break;
        }
    }
    
    /**
     * The source for Aces2 data.
     */
    private BufferedReader input;
}
