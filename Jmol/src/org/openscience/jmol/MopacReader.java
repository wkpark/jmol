
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

import java.io.*;
import java.util.Vector;

/**
 *  Reads Mopac output files.
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
class MopacReader implements ChemFileReader {

  /**
   * The source for data.
   */
  BufferedReader input;

  /**
   * Create an MOPAC output reader.
   *
   * @param input source of MOPAC data
   */
  public MopacReader(Reader input) {
    this.input = new BufferedReader(input);
  }

  /**
   * Read the MOPAC output.
   *
   * @return a ChemFile with the coordinates, energies, and vibrations.
   * @exception IOException if an I/O error occurs
   */
  public ChemFile read() throws Exception {

    ChemFile file = new ChemFile();
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
          file.frames.addElement(frame);
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
    file.frames.addElement(frame);

    return file;
  }

  /**
   * Reads a set of coordinates into ChemFrame.
   *
   * @param frame  the destination ChemFrame
   * @exception IOException  if an I/O error occurs
   */
  void readCoordinates(ChemFrame mol) throws IOException, Exception {

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
        throw new Exception("Error reading coordinates");
      }
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        x = token.nval;
      } else {
        throw new Exception("Error reading coordinates");
      }
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        y = token.nval;
      } else {
        throw new Exception("Error reading coordinates");
      }
      if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
        z = token.nval;
      } else {
        throw new Exception("Error reading coordinates");
      }
      mol.addVert(atomicNumber, (float) x, (float) y, (float) z);
    }
  }

  /**
   * Reads a set of vibrations into ChemFrame.
   *
   * @param frame  the destination ChemFrame
   * @exception IOException  if an I/O error occurs
   */
  void readFrequencies(ChemFrame mol) throws IOException, Exception {

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
      for (int i = 0; i < mol.getNvert(); ++i) {
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
            throw new Exception("Error reading frequencies");
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
            throw new Exception("Error reading frequencies");
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
            throw new Exception("Error reading frequencies");
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

