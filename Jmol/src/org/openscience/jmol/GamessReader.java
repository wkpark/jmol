/*
 * GamessReader.java
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
 * A reader for GAMESS output.
 * GAMESS is a quantum chemistry program
 * by Gordon research group at Iowa State University
 * (http://www.msg.ameslab.gov/GAMESS/GAMESS.html).
 *
 * <p> Molecular coordinates, energies, and normal coordinates of
 * vibrations are read. Each set of coordinates is added to the
 * ChemFile in the order they are found. Energies and vibrations
 * are associated with the previously read set of coordinates.
 *
 * <p> This reader was developed from a small set of
 * example output files, and therefore, is not guaranteed to
 * properly read all GAMESS output. If you have problems,
 * please contact the author of this code, not the developers
 * of GAMESS.
 *
 * @author Bradley A. Smith (yeldar@home.com)
 * @version 1.0
 */
public class GamessReader implements ChemFileReader {
    /**
     * Scaling factor for converting atomic coordinates from
     * units of Bohr to Angstroms.
     */
    public static final double angstromPerBohr = 0.529177249;
    
    /**
     * Create an GAMESS output reader.
     *
     * @param input source of GAMESS data
     */
    public GamessReader(Reader input) {
        this.input = new BufferedReader(input);
    }
    
    /**
     * Read the GAMESS output.
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
            if (line.indexOf("COORDINATES (BOHR)") >= 0) {
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
                if (line.indexOf("COORDINATES (BOHR)") >= 0) {
                    // Found a set of coordinates.
                    
                    // Add current frame to file and create a new one.
                    file.frames.addElement(frame);
                    frame = new ChemFrame();
                    readCoordinates(frame);
                } else  if (line.indexOf("TOTAL ENERGY =") >= 0) {
                    // Found an energy
                    frame.setInfo(line.trim());
                } else  if (line.indexOf("FREQUENCIES IN CM") >= 0) {
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
        while (input.ready()) {
            line = input.readLine();
            if (line == null || line.trim().length() == 0) {
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
            } else  throw new IOException("Error reading coordinates");
            double x;
            double y;
            double z;
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                x = token.nval * angstromPerBohr;
            } else  throw new IOException("Error reading coordinates");
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                y = token.nval * angstromPerBohr;
            } else  throw new IOException("Error reading coordinates");
            if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                z = token.nval * angstromPerBohr;
            } else  throw new IOException("Error reading coordinates");
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
        if (line.indexOf("*****") >= 0) {
            line = input.readLine();
            line = input.readLine();
            line = input.readLine();
            line = input.readLine();
            line = input.readLine();
        }
        line = input.readLine();
        while (line.indexOf("FREQUENCY:") >= 0) {
            StringReader freqValRead = new StringReader(line.substring(16));
            StreamTokenizer token = new StreamTokenizer(freqValRead);
            Vector vibs = new Vector();
            while (token.nextToken() != StreamTokenizer.TT_EOF) {
                if (token.ttype == StreamTokenizer.TT_WORD) {
                    // Previous Vibration was imaginary.
                    // Add this token to its label.
                    Vibration vib = new Vibration(((Vibration)vibs.lastElement(
						)).getLabel() + ' ' + token.sval);
                    vibs.removeElementAt(vibs.size() - 1);
                    vibs.addElement(vib);
                    // Read next token for current Vibration value.
                    token.nextToken();
                }
                Vibration f = new Vibration(Double.toString(token.nval));
                vibs.addElement(f);
            }
            Vibration[] currentVibs = new Vibration[vibs.size()];
            vibs.copyInto(currentVibs);
            Object[] currentVectors = new Object[currentVibs.length];
            line = input.readLine();
            line = input.readLine();
            for (int i = 0; i < frame.getNvert(); ++i) {
                line = input.readLine();
                StringReader vectorRead = new StringReader(line);
                token = new StreamTokenizer(vectorRead);
                token.nextToken();
				// ignore first token
                token.nextToken();
				// ignore second token
                token.nextToken();
				// ignore third token
                for (int j = 0; j < currentVibs.length; ++j) {
                    currentVectors[j] = new double[3];
                    if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                        ((double[])currentVectors[j])[0] = token.nval;
                    } else  throw new IOException("Error reading frequencies");
                }
                line = input.readLine();
                vectorRead = new StringReader(line);
                token = new StreamTokenizer(vectorRead);
                token.nextToken();
				// ignore first token
                for (int j = 0; j < currentVibs.length; ++j) {
                    if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                        ((double[])currentVectors[j])[1] = token.nval;
                    } else  throw new IOException("Error reading frequencies");
                }
                line = input.readLine();
                vectorRead = new StringReader(line);
                token = new StreamTokenizer(vectorRead);
                token.nextToken();
				// ignore first token
                for (int j = 0; j < currentVibs.length; ++j) {
                    if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
                        ((double[])currentVectors[j])[2] = token.nval;
                    } else  throw new IOException("Error reading frequencies");
                    currentVibs[j].addAtomVector((double[])currentVectors[j]);
                }
            }
            for (int i = 0; i < currentVibs.length; ++i) {
                frame.addVibration(currentVibs[i]);
            }
            for (int i = 0; i < 15; ++i) {
                line = input.readLine();
            }
        }
    }

    /**
     * The source for GAMESS data.
     */
    private BufferedReader input;
}
