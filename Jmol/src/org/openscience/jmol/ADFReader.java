
/*
 * Copyright 2001 The Jmol Development Team
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
package org.openscience.jmol;

import java.util.Vector;
import java.util.Hashtable;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * A reader for ADF output.
 * Amsterdam Density Functional (ADF) is a quantum chemistry program
 * by Scientific Computing & Modelling NV (SCM)
 * (http://www.scm.com/).
 *
 * <p> Molecular coordinates, energies, and normal coordinates of
 * vibrations are read. Each set of coordinates is added to the
 * ChemFile in the order they are found. Energies and vibrations
 * are associated with the previously read set of coordinates.
 *
 * <p> This reader was developed from a small set of
 * example output files, and therefore, is not guaranteed to
 * properly read all ADF output. If you have problems,
 * please contact the author of this code, not the developers
 * of ADF.
 *
 * @author Bradley A. Smith (yeldar@home.com)
 * @version 1.0
 */
public class ADFReader extends DefaultChemFileReader {

  /**
   * Create an ADF output reader.
   *
   * @param input source of ADF data
   */
  public ADFReader(Reader input) {
    super(input);
  }
  
  /**
   * Read the ADF output.
   *
   * @return a ChemFile with the coordinates, energies, and vibrations.
   * @exception IOException if an I/O error occurs
   */
  public ChemFile read() throws IOException {

    ChemFile file = new ChemFile(bondsEnabled);
    ChemFrame frame = null;
    String line = input.readLine();

    // Find first set of coordinates
    while (input.ready() && (line != null)) {
      if (line.indexOf("Coordinates (Cartesian)") >= 0) {
        frame = new ChemFrame();
        readCoordinates(frame);
        break;
      }
      line = input.readLine();
    }
    if (frame != null) {
      line = input.readLine();
      while (input.ready() && (line != null)) {
        if (line.indexOf("Coordinates (Cartesian)") >= 0) {

          // Found set of coordinates
          // Add current frame to file and create a new one.
          file.addFrame(frame);
          frame = new ChemFrame();
          readCoordinates(frame);
        } else if (line.indexOf("Energy:") >= 0) {

          // Found an energy
          frame.setInfo(line.trim());
        } else if (line.indexOf("Vibrations") >= 0) {

          // Found a set of vibrations
          readFrequencies(frame);
        }
        line = input.readLine();
      }

      // Add current frame to file
      file.addFrame(frame);
    }
    return file;
  }

  /**
   * Reads a set of coordinates into ChemFrame.
   *
   * @param frame  the destination ChemFrame
   * @exception IOException  if an I/O error occurs
   */
  private void readCoordinates(ChemFrame frame)
          throws IOException {

    String line;
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    while (input.ready()) {
      line = input.readLine();
      if ((line == null) || (line.indexOf("-----") > 0)) {
        break;
      }
      int atomicNumber;
      StringReader sr = new StringReader(line);
      StreamTokenizer token = new StreamTokenizer(sr);
      token.nextToken();

      // ignore first token
      if (token.nextToken() == StreamTokenizer.TT_WORD) {
        atomicNumber = AtomicSymbol.elementToAtomicNumber(token.sval);
        if (atomicNumber == 0) {

          // skip dummy atoms
          continue;
        }
      } else {
        throw new IOException("Error reading coordinates");
      }

      // Ignore coordinates in bohr
      token.nextToken();
      token.nextToken();
      token.nextToken();

      // Read coordinates in angstroms
      double x;
      double y;
      double z;
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        x = token.nval;
      } else {
        throw new IOException("Error reading coordinates");
      }
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        y = token.nval;
      } else {
        throw new IOException("Error reading coordinates");
      }
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        z = token.nval;
      } else {
        throw new IOException("Error reading coordinates");
      }
      frame.addAtom(atomicNumber, (float) x, (float) y, (float) z);
    }
  }

  /**
   * Reads a set of vibrations into ChemFrame.
   *
   * @param frame  the destination ChemFrame
   * @exception IOException  if an I/O error occurs
   */
  private void readFrequencies(ChemFrame frame)
          throws IOException {

    String line;
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    line = input.readLine();
    while (line.indexOf('.') > 0) {
      Vector currentVibs = new Vector();
      StringReader freqValRead = new StringReader(line);
      StreamTokenizer token = new StreamTokenizer(freqValRead);
      while (token.nextToken() != StreamTokenizer.TT_EOF) {
        Vibration freq = new Vibration(Double.toString(token.nval));
        currentVibs.addElement(freq);
      }
      line = input.readLine();
      for (int i = 0; i < frame.getNumberOfAtoms(); ++i) {
        line = input.readLine();
        StringReader vectorRead = new StringReader(line);
        token = new StreamTokenizer(vectorRead);
        token.nextToken();

        // ignore first token
        if (token.nextToken() == StreamTokenizer.TT_WORD) {
          int atomicNumber = AtomicSymbol.elementToAtomicNumber(token.sval);
          if (atomicNumber == 0) {

            // skip dummy atoms
            --i;
            continue;
          }
        } else {
          throw new IOException("Error reading frequencies");
        }
        for (int j = 0; j < currentVibs.size(); ++j) {
          double[] v = new double[3];
          if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
            v[0] = token.nval;
          } else {
            throw new IOException("Error reading frequencies");
          }
          if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
            v[1] = token.nval;
          } else {
            throw new IOException("Error reading frequencies");
          }
          if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
            v[2] = token.nval;
          } else {
            throw new IOException("Error reading frequencies");
          }
          ((Vibration) currentVibs.elementAt(j)).addAtomVector(v);
        }
      }
      for (int i = 0; i < currentVibs.size(); ++i) {
        frame.addVibration((Vibration) currentVibs.elementAt(i));
      }
      line = input.readLine();
      line = input.readLine();
      line = input.readLine();
    }
  }

}
