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

import org.openscience.jmol.DisplayControl;
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

    private String jaguarVersion = "";
    
  public JaguarReader(DisplayControl control, Reader input) {
    super(control, input);
  }

  public ChemFile read() throws IOException {

    ChemFile file = new ChemFile(control, bondsEnabled);
    ChemFrame frame = null;

    // Find energy
    String line;
    while (input.ready()) {
      line = input.readLine();
      logger.debug(line);
      if (line.indexOf("SCF energy:") >= 0) {
        frame.setInfo(line.trim());
      } else if (line.indexOf("Jaguar version") >= 0) {
          int index = line.indexOf("Jaguar version") + 15;
          jaguarVersion = line.substring(index,index+16);
          logger.info("Reading output of Jaguar version: " + jaguarVersion);
      } else if (line.indexOf("Input geometry:") >= 0) {
          logger.info("Found input geometry");
        line = input.readLine();
        line = input.readLine();
        if (frame != null) {
            frame.rebond();
            file.addFrame(frame);
        }
        frame = new ChemFrame(control);
        readCoordinates(frame);
      } else if (line.indexOf("new geometry:") >= 0) {
          logger.info("Found new geometry");
        line = input.readLine();
        line = input.readLine();
        if (frame != null) {
            frame.rebond();
            file.addFrame(frame);
        }
        frame = new ChemFrame(control);
        readCoordinates(frame);
      } else if (line.indexOf("harmonic frequencies in cm") >= 0) {
          logger.info("Found frequency data");
        line = input.readLine();
        line = input.readLine();
        readFrequencies(frame);
        break;
      }
    }

    // Add current frame to file
    frame.rebond();
    file.addFrame(frame);

    return file;
  }

  // read cartesian coordinates
  void readCoordinates(ChemFrame mol) throws IOException {

      logger.info("Reading Coordinates...");
      
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
      logger.debug("Adding atom: "+ atomicNumber+ x+ y+ z);
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

      logger.info("Reading Frequencies");
      
      String line = input.readLine();
      logger.debug(line);
      // scroll upto start of frequency list
      while (line.indexOf("frequencies") < 0) {
          line = input.readLine();
          logger.debug(line);
      }
      logger.debug("Found freq block");
    while (line.indexOf("frequencies") >= 0) {
        logger.debug(line);
        // parse the numbers in this line: 
        //   frequencies  1645.13  3635.81  3786.80
      StringReader freqValRead = new StringReader(line.substring(13));
      StreamTokenizer token = new StreamTokenizer(freqValRead);

      Vector freqs = new Vector();
      while (token.nextToken() != StreamTokenizer.TT_EOF) {
        Vibration f = new Vibration(Double.toString(token.nval));
        logger.debug("Adding freq: " + Double.toString(token.nval));
        freqs.addElement(f);
      }
      Vibration[] currentFreqs = new Vibration[freqs.size()];
      freqs.copyInto(currentFreqs);
      Object[] currentVectors = new Object[currentFreqs.length];

      /* A frequency block looks like: 
      frequencies  1645.13  3635.81  3786.80
      reduc. mass     0.52     0.49     0.52
      O1   X     0.00000  0.00000  0.00000
      O1   Y     0.00000  0.00000  0.06913
      O1   Z    -0.06922  0.04696  0.00000
      H2   X     0.00000  0.00000  0.00000
      H2   Y    -0.39546 -0.58287 -0.54854
      H2   Z     0.54928 -0.37267 -0.39660
      H3   X     0.00000  0.00000  0.00000
      H3   Y     0.39546  0.58286 -0.54855
      H3   Z     0.54928 -0.37267  0.39660
      */
      if (jaguarVersion.startsWith("4.2")) {
          line = input.readLine();
      } else {
          line = input.readLine();
          line = input.readLine();
      }
      for (int i = 0; i < mol.getAtomCount(); ++i) {
          // read line with X coordinate
          line = input.readLine();
          logger.debug(line);
          logger.debug("Reading X components");
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
          
          // read line with Y coordinate
          line = input.readLine();
          logger.debug(line);
          logger.debug("Reading Y components");
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
          
          // read line with Z coordinate
          line = input.readLine();
          logger.debug(line);
          logger.debug("Reading Z components");
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
      logger.info("Found # frequencies: " + currentFreqs.length);
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

