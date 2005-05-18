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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.adapter.smarter;

import org.jmol.api.JmolAdapter;

import java.io.BufferedReader;
import java.util.StringTokenizer;

class Resolver {

  static Object resolve(String name, BufferedReader bufferedReader,
                        JmolAdapter.Logger logger) throws Exception {
    AtomSetCollectionReader atomSetCollectionReader;
    String atomSetCollectionReaderName =
      determineAtomSetCollectionReader(bufferedReader, logger);
    logger.log("The Resolver thinks", atomSetCollectionReaderName);
    String className =
      "org.jmol.adapter.smarter." + atomSetCollectionReaderName + "Reader";

    if (atomSetCollectionReaderName == null)
      return "unrecognized file format";

    try {
      Class atomSetCollectionReaderClass = Class.forName(className);
      atomSetCollectionReader =
        (AtomSetCollectionReader)atomSetCollectionReaderClass.newInstance();
    } catch (Exception e) {
      String err = "Could not instantiate:" + className;
      logger.log(err);
      return err;
    }

    atomSetCollectionReader.setLogger(logger);
    atomSetCollectionReader.initialize();

    AtomSetCollection atomSetCollection =
      atomSetCollectionReader.readAtomSetCollection(bufferedReader);
    if (atomSetCollection.errorMessage != null)
      return atomSetCollection.errorMessage;
    if (atomSetCollection.atomCount == 0)
      return "No atoms in file";
    return atomSetCollection;
  }

  static String determineAtomSetCollectionReader(BufferedReader bufferedReader,
                                                 JmolAdapter.Logger logger)
    throws Exception {
    String[] lines = new String[4];
    LimitedLineReader llr = new LimitedLineReader(bufferedReader, 16384);
    for (int i = 0; i < lines.length; ++i)
      lines[i] = llr.readLineWithNewline();
    if (lines[3].length() >= 6) {
      String line4trimmed = lines[3].trim();
      if (line4trimmed.endsWith("V2000") ||
          line4trimmed.endsWith("v2000") ||
          line4trimmed.endsWith("V3000"))
        return "Mol";
      try {
        Integer.parseInt(lines[3].substring(0, 3).trim());
        Integer.parseInt(lines[3].substring(3, 6).trim());
        return "Mol";
      } catch (NumberFormatException nfe) {
      }
    }
    try {
      /*int atomCount = */Integer.parseInt(lines[0].trim());
      return "Xyz";
    } catch (NumberFormatException e) {
    }
    try {
      StringTokenizer tokens = new StringTokenizer(lines[0].trim(), " \t");
      if ((tokens != null) && (tokens.countTokens() >= 2)) {
        Integer.parseInt(tokens.nextToken().trim());
        return "FoldingXyz";
      }
    } catch (NumberFormatException e) {
      //
    }
    // run these loops forward ... easier for people to understand
    for (int i = 0; i < startsWithRecords.length; ++i) {
      String[] recordTags = startsWithRecords[i];
      for (int j = 0; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        for (int k = 0; k < lines.length; ++k) {
          if (lines[k].startsWith(recordTag))
            return startsWithFormats[i];
        }
      }
    }
    for (int i = 0; i < containsRecords.length; ++i) {
      String[] recordTags = containsRecords[i];
      for (int j = 0; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        for (int k = 0; k < lines.length; ++k) {
          if (lines[k].indexOf(recordTag) != -1)
            return containsFormats[i];
        }
      }
    }

    if (lines[1] == null || lines[1].trim().length() == 0)
      return "Jme"; // this is really quite broken :-)
    return null;
  }

  ////////////////////////////////////////////////////////////////
  // these test lines that startWith one of these strings
  ////////////////////////////////////////////////////////////////

  final static String[] pdbRecords = {
    "HEADER", "OBSLTE", "TITLE ", "CAVEAT", "COMPND", "SOURCE", "KEYWDS",
    "EXPDTA", "AUTHOR", "REVDAT", "SPRSDE", "JRNL  ", "REMARK",

    "DBREF ", "SEQADV", "SEQRES", "MODRES", 

    "HELIX ", "SHEET ", "TURN  ",

    "CRYST1", "ORIGX1", "ORIGX2", "ORIGX3", "SCALE1", "SCALE2", "SCALE3",

    "ATOM  ", "HETATM", "MODEL ",
  };

  final static String[] shelxRecords =
  { "TITL ", "ZERR ", "LATT ", "SYMM ", "CELL " };

  final static String[] cifRecords =
  { "data_", "_publ" };

  final static String[] ghemicalMMRecords =
  { "!Header mm1gp", "!Header gpr" };

  final static String[] jaguarRecords =
  { "  |  Jaguar version", };

  final static String[] hinRecords = 
  {"mol "};

  final static String[] mdlRecords = 
  {"$MDL "};

  final static String[] nwchemRecords =
  {" argument  1"};

  final static String[] spartanSmolRecords =
  {"INPUT="};

  final static String[][] startsWithRecords =
  { pdbRecords, shelxRecords, cifRecords, ghemicalMMRecords,
    jaguarRecords, hinRecords , mdlRecords, nwchemRecords,
    spartanSmolRecords};

  final static String[] startsWithFormats =
  { "Pdb", "Shelx", "Cif", "GhemicalMM",
    "Jaguar", "Hin", "Mol", "NWChem", "SpartanSmol" };

  ////////////////////////////////////////////////////////////////
  // contains formats
  ////////////////////////////////////////////////////////////////
  
  final static String[] molproRecords =
  {"http://www.molpro.net/schema/molpro"};

  final static String[] cmlRecords =
  { "<?xml", "<atom", "<molecule", "<reaction", "<cml", "<bond", ".dtd\"",
    "<list>", "<entry", "<identifier", "http://www.xml-cml.org/schema/cml2/core" };

  final static String[] gaussianRecords =
  { "Entering Gaussian System", "1998 Gaussian, Inc." };

  final static String[] mopacRecords =
  { "MOPAC 93 (c) Fujitsu", "MOPAC2002 (c) Fujitsu",
    "MOPAC FOR LINUX (PUBLIC DOMAIN VERSION)"};

  final static String[] qchemRecords = 
  { "Welcome to Q-Chem", "A Quantum Leap Into The Future Of Chemistry" };

  final static String[] gamessRecords =
  { "GAMESS" };

  final static String[] spartanRecords =
  { "Spartan" };

    // It's important that molpro gets looked for before cml otherwise molpro files will probably parse as cml
  final static String[][] containsRecords =
  { molproRecords, cmlRecords, gaussianRecords, mopacRecords, qchemRecords, gamessRecords,
    spartanRecords
  };

  final static String[] containsFormats =
  { "Molpro", "Cml", "Gaussian", "Mopac", "Qchem", "Gamess", "Spartan" };
}

class LimitedLineReader {
  int readLimit;
  char[] buf;
  int cchBuf;
  int ichCurrent;

  LimitedLineReader(BufferedReader bufferedReader, int readLimit)
    throws Exception {
    this.readLimit = readLimit;
    bufferedReader.mark(readLimit);
    buf = new char[readLimit];
    cchBuf = bufferedReader.read(buf);
    ichCurrent = 0;
    bufferedReader.reset();
  }

  String readLineWithNewline() {
    // mth 2004 10 17
    // for now, I am going to put in a hack here
    // we have some CIF files with many lines of '#' comments
    // I believe that for all formats we can flush if the first
    // char of the line is a #
    // if this becomes a problem then we will need to adjust
    while (ichCurrent < cchBuf) {
      int ichBeginningOfLine = ichCurrent;
      char ch = 0;
      while (ichCurrent < cchBuf &&
             (ch = buf[ichCurrent++]) != '\r' && ch != '\n') {
      }
      if (ch == '\r' && ichCurrent < cchBuf && buf[ichCurrent] == '\n')
        ++ichCurrent;
      int cchLine = ichCurrent - ichBeginningOfLine;
      if (buf[ichBeginningOfLine] == '#') // flush comment lines;
        continue;
      StringBuffer sb = new StringBuffer(cchLine);
      sb.append(buf, ichBeginningOfLine, cchLine);
      return "" + sb;
    }
    System.out.println("org.jmol.adapter.smarter.Resolver short input buffer");
    // miguel 2005 01 26
    // for now, just return the empty string.
    // it will only affect the Resolver code
    // it will be easier to handle because then everyone does not
    // need to check for the null pointer
    //
    // If it becomes a problem, then change this to null and modify
    // all the code above to make sure that it tests for null before
    // attempting to invoke methods on the strings. 
    return "";
  }
}
