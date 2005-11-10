/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.adapter.smarter;

import java.io.BufferedReader;

/**
 * A reader for SHELX output (RES) files. It does not read all information.
 * The list of fields that is read: TITL, REM, END, CELL, SPGR, SFAC
 * In addition atoms are read.
 *
 * <p>A reader for SHELX files. It currently supports SHELXL.
 *
 * <p>The SHELXL format is described on the net:
 * <a href="http://www.msg.ucsf.edu/local/programs/shelxl/ch_07.html"
 * http://www.msg.ucsf.edu/local/programs/shelxl/ch_07.html</a>.
 *
 */

class ShelxReader extends AtomSetCollectionReader {

  boolean endReached;
  String[] sfacElementSymbols;

  AtomSetCollection readAtomSetCollection(BufferedReader reader)
    throws Exception {
    atomSetCollection = new AtomSetCollection("shelx");
    atomSetCollection.coordinatesAreFractional = true;

    String line;
    int lineLength;
    readLine_loop:
    while ((line = reader.readLine()) != null) {
      lineLength = line.length();
      // '=' as last char of line means continue on next line
      while (lineLength > 0 && line.charAt(lineLength - 1) == '=')
        line = line.substring(0, lineLength - 1) + reader.readLine();
      if (lineLength < 4) {
        if (lineLength == 3 && "END".equalsIgnoreCase(line))
          break;
        continue;
      }
      // FIXME -- should we call toUpperCase(Locale.US) ?
      // although I really don't think it is necessary
      String command = line.substring(0, 4).toUpperCase();
      for (int i = unsupportedRecordTypes.length; --i >= 0; )
        if (command.equals(unsupportedRecordTypes[i]))
          continue readLine_loop;
      for (int i = supportedRecordTypes.length; --i >= 0; )
        if (command.equals(supportedRecordTypes[i])) {
          processSupportedRecord(i, line);
          if (endReached)
            break readLine_loop;
          continue readLine_loop;
        }
      assumeAtomRecord(line);
    }
    return atomSetCollection;
  }

  final static String[] supportedRecordTypes =
  {"TITL", "CELL", "SPGR", "END ", "SFAC"};

  void processSupportedRecord(int recordIndex, String line)
    throws Exception {
    switch(recordIndex) {
    case 0: // TITL
      atomSetCollection.collectionName = parseTrimmed(line, 4);
      break;
    case 1: // CELL
      cell(line);
      break;
    case 2: // SPGR
      atomSetCollection.spaceGroup = parseTrimmed(line, 4);
      break;
    case 3: // END
      endReached = true;
      break;
    case 4: // SFAC
      parseSfacRecord(line);
      break;
    }
  }
  
  void cell(String line) throws Exception {
    /* example:
     * CELL  1.54184   23.56421  7.13203 18.68928  90.0000 109.3799  90.0000
     * CELL   1.54184   7.11174  21.71704  30.95857  90.000  90.000  90.000
     */
    float wavelength = parseFloat(line, 4);
    float[] notionalUnitcell = new float[6];
    for (int i = 0; i < 6; ++i)
      notionalUnitcell[i] = parseFloat(line, ichNextParse);
    atomSetCollection.wavelength = wavelength;
    atomSetCollection.notionalUnitcell = notionalUnitcell;
  }

  void parseSfacRecord(String line) {
    // an SFAC record is one of two cases
    // a simple SFAC record contains element names
    // a general SFAC record contains coefficients for a single element
    String[] sfacTokens = getTokens(line, 4);
    boolean allElementSymbols = true;
    for (int i = sfacTokens.length; allElementSymbols && --i >= 0; ) {
      String token = sfacTokens[i];
      allElementSymbols = Atom.isValidElementSymbolNoCaseSecondChar(token);
      }
    if (allElementSymbols)
      parseSfacElementSymbols(sfacTokens);
    else
      parseSfacCoefficients(sfacTokens);
  }

  void parseSfacElementSymbols(String[] sfacTokens) {
    if (sfacElementSymbols == null) {
      sfacElementSymbols = sfacTokens;
    } else {
      int oldCount = sfacElementSymbols.length;
      int tokenCount = sfacTokens.length;
      sfacElementSymbols = setLength(sfacElementSymbols,
                                     oldCount + tokenCount);
      for (int i = tokenCount; --i >= 0; )
        sfacElementSymbols[oldCount + tokenCount] = sfacTokens[i];
    }
  }

  void parseSfacCoefficients(String[] sfacTokens) {
    float a1 = parseFloat(sfacTokens[1]);
    float a2 = parseFloat(sfacTokens[3]);
    float a3 = parseFloat(sfacTokens[5]);
    float a4 = parseFloat(sfacTokens[7]);
    float c  = parseFloat(sfacTokens[9]);
    // element # is these floats rounded to nearest int
    int z = (int)(a1 + a2 + a3 + a4 + c + 0.5f);
    String elementSymbol = getElementSymbol(z);
    int oldCount = 0;
    if (sfacElementSymbols == null) {
      sfacElementSymbols = new String[1];
    } else {
      oldCount = sfacElementSymbols.length;
      sfacElementSymbols = setLength(sfacElementSymbols, oldCount + 1);
      sfacElementSymbols[oldCount] = elementSymbol;
    }
    sfacElementSymbols[oldCount] = elementSymbol;
  }

  void assumeAtomRecord(String line) {
    try {
      //    System.out.println("Assumed to contain an atom: " + line);
      // this line gives an atom, because all lines not starting with
      // a SHELX command is an atom
      String atomName = parseToken(line);
      int scatterFactor = parseInt(line, ichNextParse);
      float a = parseFloat(line, ichNextParse);
      float b = parseFloat(line, ichNextParse);
      float c = parseFloat(line, ichNextParse);
      // skip the rest
      
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = atomName;
      if (sfacElementSymbols != null) {
        int elementIndex = scatterFactor - 1;
        if (elementIndex >= 0 &&
            elementIndex < sfacElementSymbols.length)
          atom.elementSymbol = sfacElementSymbols[elementIndex];
      }
      atom.x = a;
      atom.y = b;
      atom.z = c;
    } catch (Exception ex) {
      logger.log("Exception", ex, line);
    }
  }

  final static String[] unsupportedRecordTypes = {
    /* 7.1 Crystal data and general instructions */
    "ZERR",
    "LATT",
    "SYMM",
    "DISP",
    "UNIT",
    "LAUE",
    "REM ",
    "MORE",
    "TIME",
    /* 7.2 Reflection data input */
    "HKLF",
    "OMIT",
    "SHEL",
    "BASF",
    "TWIN",
    "EXTI",
    "SWAT",
    "HOPE",
    "MERG",
    /* 7.3 Atom list and least-squares constraints */
    "SPEC",
    "RESI",
    "MOVE",
    "ANIS",
    "AFIX",
    "HFIX",
    "FRAG",
    "FEND",
    "EXYZ",
    "EXTI",
    "EADP",
    "EQIV",
    /* 7.4 The connectivity list */
    "CONN",
    "PART",
    "BIND",
    "FREE",
    /* 7.5 Least-squares restraints */
    "DFIX",
    "DANG",
    "BUMP",
    "SAME",
    "SADI",
    "CHIV",
    "FLAT",
    "DELU",
    "SIMU",
    "DEFS",
    "ISOR",
    "NCSY",
    "SUMP",
    /* 7.6 Least-squares organization */
    "L.S.",
    "CGLS",
    "BLOC",
    "DAMP",
    "STIR",
    "WGHT",
    "FVAR",
    /* 7.7 Lists and tables */
    "BOND",
    "CONF",
    "MPLA",
    "RTAB",
    "HTAB",
    "LIST",
    "ACTA",
    "SIZE",
    "TEMP",
    "WPDB",
    /* 7.8 Fouriers, peak search and lineprinter plots */
    "FMAP",
    "GRID",
    "PLAN",
    "MOLE",

    // "Disrgarding line assumed to be added by PLATON: " + line);
    "    "
  };
}
