/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.io;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.ChemFile;
import org.openscience.jmol.ChemFrame;
import org.openscience.jmol.Vibration;
import java.util.Vector;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.IOException;

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
public class GamessReader extends DefaultChemFileReader {

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
  public GamessReader(JmolViewer viewer, Reader input) {
    super(viewer, input);
  }

  /**
   * Read the GAMESS output.
   *
   * @return a ChemFile with the coordinates, energies, and vibrations.
   * @exception IOException if an I/O error occurs
   */
  public ChemFile read() throws IOException {

    ChemFile file = new ChemFile(viewer, bondsEnabled);
    ChemFrame frame = null;
    String line = input.readLine();

    // Find first set of coordinates
    while (input.ready() && (line != null)) {
      if (line.indexOf("COORDINATES (BOHR)") >= 0) {

        // Found a set of coordinates.
        frame = new ChemFrame(viewer);
        input.readLine();
        readCoordinates(frame, true);
        break;
      }
      line = input.readLine();
    }
    boolean initialFrame = true;
    if (frame != null) {

      // Read all other data
      line = input.readLine();
      while (input.ready() && (line != null)) {
        if (line.indexOf("COORDINATES OF ALL ATOMS ARE (ANGS)") >= 0) {

          // Found a set of coordinates.

          // Add current frame to file. Unless it is the first one.
          // In which case, it is a duplicate of the initial
          // coordinates.
          if (initialFrame) {
            initialFrame = false;
          } else {
            file.addFrame(frame);
          }
          frame = new ChemFrame(viewer);
          input.readLine();
          input.readLine();
          readCoordinates(frame, false);
        } else if (line.indexOf("TOTAL ENERGY =") >= 0) {

          // Found an energy
          frame.setInfo(line.trim());
        } else if (line.indexOf("FREQUENCIES IN CM") >= 0) {

          // Found a set of vibrations
          readFrequencies(frame);
        }
        line = input.readLine();
      }

      // Add current frame to file
      frame.rebond();
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
  private void readCoordinates(ChemFrame frame, boolean unitsAreBohr)
      throws IOException {

    double unitsScaling = 1.0;
    if (unitsAreBohr) {
      unitsScaling = angstromPerBohr;
    }
    String line;
    while (input.ready()) {
      line = input.readLine();
      if ((line == null) || (line.trim().length() == 0)) {
        break;
      }
      int atomicNumber;
      StringReader sr = new StringReader(line);
      StreamTokenizer token = new StreamTokenizer(sr);
      token.nextToken();

      // ignore first token
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        atomicNumber = (int) token.nval;
        if (atomicNumber == 0) {

          // skip dummy atoms
          continue;
        }
      } else {
        throw new IOException("Error reading coordinates");
      }
      double x;
      double y;
      double z;
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        x = token.nval * unitsScaling;
      } else {
        throw new IOException("Error reading coordinates");
      }
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        y = token.nval * unitsScaling;
      } else {
        throw new IOException("Error reading coordinates");
      }
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        z = token.nval * unitsScaling;
      } else {
        throw new IOException("Error reading coordinates");
      }
      frame.addAtom(atomicNumber, x, y, z);
    }
  }

  /**
   * Reads a set of vibrations into ChemFrame.
   *
   * @param frame  the destination ChemFrame
   * @exception IOException  if an I/O error occurs
   */
  private void readFrequencies(ChemFrame frame) throws IOException {

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
    while ((line != null) && (line.indexOf("FREQUENCY:") >= 0)) {
      int colonIndex = line.indexOf(':');
      StringReader freqValRead = new StringReader(line.substring(colonIndex
                                   + 1));
      StreamTokenizer token = new StreamTokenizer(freqValRead);
      Vector vibs = new Vector();
      while (token.nextToken() != StreamTokenizer.TT_EOF) {
        if (token.ttype == StreamTokenizer.TT_WORD) {

          // Previous Vibration was imaginary.
          // Add this token to its label.
          Vibration vib =
            new Vibration(((Vibration) vibs.lastElement()).getLabel() + ' '
              + token.sval);
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
      for (int i = 0; i < frame.getAtomCount(); ++i) {
        line = input.readLine();
        StringReader vectorRead = new StringReader(line);
        token = new StreamTokenizer(vectorRead);

        // ignore first token
        token.nextToken();

        // ignore second token
        token.nextToken();

        // ignore third token
        token.nextToken();

        for (int j = 0; j < currentVibs.length; ++j) {
          currentVectors[j] = new double[3];
          if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
            ((double[]) currentVectors[j])[0] = token.nval;
          } else {
            throw new IOException("Error reading frequencies");
          }
        }
        line = input.readLine();
        vectorRead = new StringReader(line);
        token = new StreamTokenizer(vectorRead);

        // ignore first token
        token.nextToken();

        for (int j = 0; j < currentVibs.length; ++j) {
          if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
            ((double[]) currentVectors[j])[1] = token.nval;
          } else {
            throw new IOException("Error reading frequencies");
          }
        }
        line = input.readLine();
        vectorRead = new StringReader(line);
        token = new StreamTokenizer(vectorRead);

        // ignore first token
        token.nextToken();

        for (int j = 0; j < currentVibs.length; ++j) {
          if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
            ((double[]) currentVectors[j])[2] = token.nval;
          } else {
            throw new IOException("Error reading frequencies");
          }
          currentVibs[j].addAtomVector((double[]) currentVectors[j]);
        }
      }
      for (int i = 0; i < currentVibs.length; ++i) {
        frame.addVibration(currentVibs[i]);
      }
      for (int i = 0; i < 15; ++i) {
        line = input.readLine();
        if ((line != null) && (line.indexOf("FREQUENCY:") >= 0)) {
          break;
        }
      }
    }
  }
}
