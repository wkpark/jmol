
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

      /* The mark and reset on the buffer, is so that we can read
       * the first line line to test for XYZ files without screwing
       * up the other tests below
       */
      buffer.mark(255);
      line = getLine(buffer);
      buffer.reset();

      // An integer on the first line is a special test for XYZ files
      try {
        new Integer(line.trim());
        return new XYZReader(buffer);
      } catch (NumberFormatException nfe) {

        // Integer not found on first line; therefore not a XYZ file
      }

      /* This line wasn't an integer, so move on to the rest of
         our filters */

      // If XML, assume CML.
      if (line.startsWith("<?xml")) {
        return new CMLReader(buffer);
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
      } else if (line.indexOf("MOPAC") >= 0) {
        return new MopacReader(buffer);
      } else if (line.startsWith("HEADER")) {
        return new PDBReader(buffer);
      } else if (line.startsWith("molstruct")) {
        return new CACheReader(buffer);
      }
      line = buffer.readLine();
    }
    return null;
  }

  static String getLine(BufferedReader buffer) throws IOException {

    StringBuffer sb1 = new StringBuffer();
    int c1 = buffer.read();
    while ((c1 > 0)
            && ((c1 == '\n') || (c1 == '\r') || (c1 == ' ')
              || (c1 == '\t'))) {
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

