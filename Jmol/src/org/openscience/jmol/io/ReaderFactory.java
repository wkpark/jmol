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

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.ChemFile;
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
   * @throws IOException  if an I/O error occurs
   * @throws IllegalArgumentException if the input is null
   */
  public static ChemFileReader
    createReader(JmolViewer viewer, Reader input) throws IOException {

    if (input == null) {
      throw new IllegalArgumentException("input cannot be null");
    }
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
        System.out.println("ReaderFactory: CMLReader");
        return new CMLReader(viewer, buffer);
      }

      // VASP file if first line contains the 'NCLASS' keyword
      if ((line != null) && line.indexOf("NCLASS") >= 0 ) {
	  System.out.println("ReaderFactory: VASPReader");
	  return new VASPReader(viewer, buffer);
      }
      


      // Abinit file if the essential keyword 'natom'
      // is found in the first 100 lines.
      buffer.mark(1 << 16);
      int lineCount = 0;
      while ((lineCount < 100) && (line != null)) {
        line = buffer.readLine();
        ++lineCount;
        if (line != null && line.indexOf("natom") >= 0) {
          buffer.reset();
          System.out.println("ReaderFactory: ABINITReader");
          return new ABINITReader(viewer, buffer);
        }
      }
      buffer.reset();

      buffer.mark(1024);
      line = buffer.readLine();

      // If the first line contains mm1gp, then the file is identified
      // as a Ghemical Molecular Dynamics file
      if ((line != null) && (line.indexOf("mm1gp") >= 0)) {
        System.out.println("ReaderFactory: ChemicalMMReader");
        return new GhemicalMMReader(viewer, buffer);
      }

      buffer.readLine();
      buffer.readLine();
      String line4 = buffer.readLine();
      buffer.reset();

    } else {
      line = buffer.readLine();
    }

    /* Search file for a line containing an identifying keyword */
    while (buffer.ready() && (line != null)) {
      if (line.indexOf("Gaussian 03:") >= 0) {
        System.out.println(": Gaussian03Reader");
        return new Gaussian03Reader(viewer, buffer);
      } else if (line.indexOf("Gaussian 98:") >= 0) {
        System.out.println("ReaderFactory: Gaussian98Reader");
        return new Gaussian98Reader(viewer, buffer);
      } else if (line.indexOf("Gaussian 95:") >= 0) {
        System.out.println("ReaderFactory: Gaussian95Reader");
        return new Gaussian94Reader(viewer, buffer);
      } else if (line.indexOf("Gaussian 94:") >= 0) {
        System.out.println("ReaderFactory: Gaussian94Reader");
        return new Gaussian94Reader(viewer, buffer);
      } else if (line.indexOf("Gaussian 92:") >= 0) {
        System.out.println("ReaderFactory: Gaussian92Reader");
        return new Gaussian92Reader(viewer, buffer);
      } else if (line.indexOf("Gaussian G90") >= 0) {
        System.out.println("ReaderFactory: Gaussian90Reader");
        return new Gaussian90Reader(viewer, buffer);
      } else if (line.indexOf("GAMESS") >= 0) {
        System.out.println("ReaderFactory: GamessReader");
        return new GamessReader(viewer, buffer);
      } else if (line.indexOf("ACES2") >= 0) {
        System.out.println("ReaderFactory: Aces2Reader");
        return new Aces2Reader(viewer, buffer);
      } else if (line.indexOf("Amsterdam Density Functional") >= 0) {
        System.out.println("ReaderFactory: ADFReader");
        return new ADFReader(viewer, buffer);
      } else if (line.indexOf("DALTON") >= 0) {
        System.out.println("ReaderFactory: DaltonReader");
        return new DaltonReader(viewer, buffer);
      } else if (line.indexOf("Jaguar") >= 0) {
        System.out.println("ReaderFactory: JaguarReader");
        buffer.reset();
        return new JaguarReader(viewer, buffer);
      } else if (line.indexOf("MOPAC:  VERSION  7.00") >= 0) {
        System.out.println("ReaderFactory: Mopac7Reader");
        return new Mopac7Reader(viewer, buffer);
      } else if ((line.indexOf("MOPAC  97.00") >= 0)
          || (line.indexOf("MOPAC2002") >= 0)) {
        System.out.println("ReaderFactory: Mopac97Reader");
        return new Mopac97Reader(viewer, buffer);
      } else if (line.startsWith("HEADER") || line.startsWith("ATOM  ")) {
        System.out.println("ReaderFactory: PDBReader");
        return new PDBReader(viewer, buffer);
      } else if (line.startsWith("molstruct")) {
        System.out.println("ReaderFactory: CACheReader");
        return new CACheReader(viewer, buffer);
      }
      line = buffer.readLine();
    }
    System.out.println("ReaderFactory: Format undetermined");
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

