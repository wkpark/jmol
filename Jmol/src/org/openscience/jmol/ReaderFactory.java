
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

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * A factory for creating ChemFileReaders.
 * The type of reader created is determined from the input.
 *
 * @author  Bradley A. Smith (bradley@baysmith.com)
 */
public abstract class ReaderFactory {

  /**
   * Creates a ChemFileReader of the type determined by
   * reading the input. The input is read line-by-line
   * until a line containing an identifying string is
   * found.
   *
   * @return  If the input type is determined, a
   *   ChemFileReader subclass is returned; otherwise,
   *   null is returned.
   * @exception IOException  if an I/O error occurs
   */
  public static ChemFileReader createReader(Reader input) throws IOException {

    BufferedReader buffer = new BufferedReader(input);
    String line = null;

    if (buffer.markSupported()) {

      // The mark and reset on the buffer, is so that we can read
      // without screwing up the other tests below
      buffer.mark(255);
      line = getLine(buffer);
      buffer.reset();

      // If XML, assume CML.
      if ((line != null) && line.startsWith("<?xml")) {
        return new CMLReader(buffer);
      }

      // Abinit
      // We test the presence of an essential keywords,
      // for instance 'natom'.
      buffer.mark(1024 * 1024);
      while ((buffer.ready()) && (line != null)) {
        line = buffer.readLine();
        if (line.indexOf("natom") >= 0) {
          buffer.reset();
          return new ABINITReader(buffer);
        }
      }
      buffer.reset();

      buffer.mark(1024);
      line = buffer.readLine();

      // If the first line contains mm1gp, then the file is identified
      // as a Ghemical Molecular Dynamics file
      if ((line != null) && (line.indexOf("mm1gp") >= 0)) {
        return new GhemicalMMReader(buffer);
      }

      buffer.readLine();
      buffer.readLine();
      String line4 = buffer.readLine();
      buffer.reset();

      // If the fourth line contains the MDL Ctab version tag or
      // contains two integers in the first 6 characters and the
      // rest of the line only contains whitespace and digits,
      // the file is identified as an MDL file
      if (line4 != null) {
        boolean mdlFile = false;
        if (line4.trim().endsWith("V2000")) {
          mdlFile = true;
        } else if (line4.length() >= 6) {
          try {
            String atomCountString = line4.substring(0, 3).trim();
            String bondCountString = line4.substring(3, 6).trim();
            new Integer(atomCountString);
            new Integer(bondCountString);
            mdlFile = true;
            if (line4.length() > 6) {
              String remainder = line4.substring(6).trim();
              for (int i = 0; i < remainder.length(); ++i) {
                char c = remainder.charAt(i);
                if (!(Character.isDigit(c) || Character.isWhitespace(c))) {
                  mdlFile = false;
                }
              }
            }
          } catch (NumberFormatException nfe) {
            // Integer not found on first line; therefore not a MDL file
          }
        }
        if (mdlFile) {
          return new MdlReader(buffer);
        }
      }

      // An integer on the first line is a special test for XYZ files
      boolean xyzFile = false;
      if (line != null) {
        StringTokenizer tokenizer = new StringTokenizer(line.trim());
        try {
          int tokenCount = tokenizer.countTokens();
          if (tokenCount == 1) {
            new Integer(tokenizer.nextToken());
            xyzFile = true;
          } else if (tokenCount == 2) {
            new Integer(tokenizer.nextToken());
            if ("Bohr".equalsIgnoreCase(tokenizer.nextToken())) {
              xyzFile = true;
            }
          }
        } catch (NumberFormatException nfe) {
          // Integer not found on first line; therefore not a XYZ file
        }
      }
      if (xyzFile) {
        return new XYZReader(buffer);
      }

    } else {
      line = buffer.readLine();
    }

    /* Search file for a line containing an identifying keyword */
    while (buffer.ready() && (line != null)) {
      if (line.indexOf("Gaussian 98:") >= 0) {
        return new Gaussian98Reader(buffer);
      } else if (line.indexOf("Gaussian 95:") >= 0) {
        return new Gaussian94Reader(buffer);
      } else if (line.indexOf("Gaussian 94:") >= 0) {
        return new Gaussian94Reader(buffer);
      } else if (line.indexOf("Gaussian 92:") >= 0) {
        return new Gaussian92Reader(buffer);
      } else if (line.indexOf("Gaussian G90") >= 0) {
        return new Gaussian90Reader(buffer);
      } else if (line.indexOf("GAMESS") >= 0) {
        return new GamessReader(buffer);
      } else if (line.indexOf("ACES2") >= 0) {
        return new Aces2Reader(buffer);
      } else if (line.indexOf("Amsterdam Density Functional") >= 0) {
        return new ADFReader(buffer);
      } else if (line.indexOf("DALTON") >= 0) {
        return new DaltonReader(buffer);
      } else if (line.indexOf("Jaguar") >= 0) {
        return new JaguarReader(buffer);
      } else if (line.indexOf("MOPAC:  VERSION  7.00") >= 0) {
        return new Mopac7Reader(buffer);
      } else if ((line.indexOf("MOPAC  97.00") >= 0)
          || (line.indexOf("MOPAC2002") >= 0)) {
        return new Mopac97Reader(buffer);
      } else if (line.startsWith("HEADER")) {
        return new PDBReader(buffer);
      } else if (line.startsWith("molstruct")) {
        return new CACheReader(buffer);
      }
      line = buffer.readLine();
    }
    return null;
  }

  /**
   * Reads a line with special XML character handling. Any whitespace characters at
   * the beginning of the line are ignored. The end-tag character '>' is
   * considered an end-of-line character.
   *
   * @param buffer the input from which to read.
   * @return the line read.
   */
  static String getLine(BufferedReader buffer) throws IOException {

    StringBuffer sb1 = new StringBuffer();
    int c1 = buffer.read();
    while ((c1 > 0)
        && ((c1 == '\n') || (c1 == '\r') || (c1 == ' ') || (c1 == '\t'))) {
      c1 = buffer.read();
    }
    while ((c1 > 0) && (c1 != '\n') && (c1 != '\r') && (c1 != '>')) {
      sb1.append((char) c1);
      c1 = buffer.read();
    }
    if (c1 == '>') {
      sb1.append((char) c1);
    }
    return sb1.toString();
  }
}

