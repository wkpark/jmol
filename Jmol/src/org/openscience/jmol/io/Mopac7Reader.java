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
 *  Reads Mopac 7 output files.
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
class Mopac7Reader extends DefaultChemFileReader {

  /**
   * Create an MOPAC output reader.
   *
   * @param input source of MOPAC data
   */
  public Mopac7Reader(Reader input) {
    super(input);
  }

  /**
   * Read the MOPAC output.
   *
   * @return a ChemFile with the coordinates, energies, and vibrations.
   * @exception IOException if an I/O error occurs
   */
  public ChemFile read() throws IOException {

    ChemFile file = new ChemFile(bondsEnabled);
    ChemFrame frame = null;
    String line;
    String frameInfo = null;
    while (input.ready()) {
      line = input.readLine();
      if (line.indexOf("TOTAL ENERGY") >= 0) {
        frameInfo = line.trim();
      } else if (line.indexOf("ORIENTATION OF MOLECULE IN FORCE CALCULATION")
          >= 0) {
        for (int i = 0; i < 3; ++i) {
          line = input.readLine();
        }
        if (frame != null) {
          if (frameInfo != null) {
            frame.setInfo(line.trim());
            frameInfo = null;
          }
          file.addFrame(frame);
        }
        frame = new ChemFrame();
        readCoordinates(frame);
      } else if (line.indexOf("NORMAL COORDINATE ANALYSIS") >= 0) {
        for (int i = 0; i < 4; ++i) {
          line = input.readLine();
        }
        readFrequencies(frame);
        break;
      }
    }

    // Add current frame to file
    file.addFrame(frame);

    return file;
  }

  /**
   * Reads a set of coordinates into ChemFrame.
   *
   * @param frame  the destination ChemFrame
   * @exception IOException  if an I/O error occurs
   */
  void readCoordinates(ChemFrame mol) throws IOException {

    String line;
    while (input.ready()) {
      line = readLine();
      if (line.trim().length() == 0) {
        break;
      }
      StringReader sr = new StringReader(line);
      StreamTokenizer token = new StreamTokenizer(sr);

      // Ignore first token
      token.nextToken();
      int atomicNumber;
      double x;
      double y;
      double z;
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        atomicNumber = (int) token.nval;
      } else {
        throw new IOException("Error reading coordinates");
      }
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
      mol.addAtom(atomicNumber, x, y, z);
    }
  }

  /**
   * Reads a set of vibrations into ChemFrame.
   *
   * @param frame  the destination ChemFrame
   * @exception IOException  if an I/O error occurs
   */
  void readFrequencies(ChemFrame mol) throws IOException {

    String line;
    line = readLine();
    while (line.indexOf("ROOT NO") >= 0) {
      line = readLine();
      line = readLine();
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

      line = readLine();
      for (int i = 0; i < mol.getAtomCount(); ++i) {
        line = readLine();
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

        line = readLine();
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

        line = readLine();
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
        line = readLine();
        if ((line.trim().length() > 0) || (line.indexOf("ROOT NO") >= 0)) {
          break;
        }
      }
    }
  }

  private String readLine() throws IOException {

    String line = input.readLine();
    while ((line != null) && (line.length() > 0)
        && Character.isDigit(line.charAt(0))) {
      line = input.readLine();
    }
    return line;
  }
}

