/*
 * Copyright 2002 The Jmol Development Team
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
import org.openscience.jmol.AtomicSymbol;
import org.openscience.jmol.Vibration;
import java.util.Vector;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 *  Reads Jaguar output files.
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
class JaguarReader extends DefaultChemFileReader {

  public JaguarReader(Reader input) {
    super(input);
  }

  public ChemFile read() throws IOException {

    ChemFile file = new ChemFile(bondsEnabled);
    ChemFrame frame = null;

    // Find energy
    String line;
    while (input.ready()) {
      line = input.readLine();
      if (line.indexOf("SCF energy:") >= 0) {
        frame.setInfo(line.trim());
      } else if (line.indexOf("Input geometry:") >= 0) {
        line = input.readLine();
        line = input.readLine();
        if (frame != null) {
          file.addFrame(frame);
        }
        frame = new ChemFrame();
        readCoordinates(frame);
      } else if (line.indexOf("new geometry:") >= 0) {
        line = input.readLine();
        line = input.readLine();
        if (frame != null) {
          file.addFrame(frame);
        }
        frame = new ChemFrame();
        readCoordinates(frame);
      } else if (line.indexOf("harmonic frequencies in cm") >= 0) {
        line = input.readLine();
        line = input.readLine();
        readFrequencies(frame);
        break;
      }
    }

    // Add current frame to file
    file.addFrame(frame);

    return file;
  }

  // read cartesian coordinates
  void readCoordinates(ChemFrame mol) throws IOException {

    String line;
    while (input.ready()) {
      line = input.readLine();
      if (line.trim().length() == 0) {
        break;
      }
      int atomicNumber;
      double x;
      double y;
      double z;
      StringReader sr = new StringReader(line);
      StreamTokenizer token = new StreamTokenizer(sr);
      if (token.nextToken() == StreamTokenizer.TT_WORD) {
        atomicNumber = atomLabelToAtomicNumber(token.sval);
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

  static int atomLabelToAtomicNumber(String label) {

    StringBuffer elementLabel = new StringBuffer();
    for (int i = 0; i < label.length(); ++i) {
      if (Character.isLetter(label.charAt(i))) {
        elementLabel.append(label.charAt(i));
      } else {
        break;
      }
    }
    return AtomicSymbol.elementToAtomicNumber(elementLabel.toString());
  }


  void readFrequencies(ChemFrame mol) throws IOException {

    String line;
    line = input.readLine();
    while (line.indexOf("frequencies") >= 0) {
      StringReader freqValRead = new StringReader(line.substring(13));
      StreamTokenizer token = new StreamTokenizer(freqValRead);

      Vector freqs = new Vector();
      while (token.nextToken() != StreamTokenizer.TT_EOF) {
        Vibration f = new Vibration(Double.toString(token.nval));
        freqs.addElement(f);
      }
      Vibration[] currentFreqs = new Vibration[freqs.size()];
      freqs.copyInto(currentFreqs);
      Object[] currentVectors = new Object[currentFreqs.length];

      line = input.readLine();
      line = input.readLine();
      for (int i = 0; i < mol.getNumberOfAtoms(); ++i) {
        line = input.readLine();
        StringReader vectorRead = new StringReader(line);
        token = new StreamTokenizer(vectorRead);
        token.nextToken();    // ignore first token
        token.nextToken();    // ignore second token
        for (int j = 0; j < currentFreqs.length; ++j) {
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
        token.nextToken();    // ignore first token
        token.nextToken();    // ignore second token
        for (int j = 0; j < currentFreqs.length; ++j) {
          if (token.nextToken() == StreamTokenizer.TT_NUMBER) {
            ((double[]) currentVectors[j])[1] = token.nval;
          } else {
            throw new IOException("Error reading frequencies");
          }
        }

        line = input.readLine();
        vectorRead = new StringReader(line);
        token = new StreamTokenizer(vectorRead);
        token.nextToken();    // ignore first token
        token.nextToken();    // ignore second token
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
        line = input.readLine();
        if (line.indexOf("frequencies") >= 0) {
          break;
        }
      }
    }
  }
}

