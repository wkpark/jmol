/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

package org.jmol.adapter.smarter;

import org.jmol.api.ModelAdapter;

import java.io.BufferedReader;

class ModelResolver {

  static Object resolveModel(String name, BufferedReader bufferedReader,
                             ModelAdapter.Logger logger) throws Exception {
    ModelReader modelReader;
    String modelReaderName = determineModelReader(bufferedReader, logger);
    logger.log("The model resolver thinks", modelReaderName);
    String className =
      "org.jmol.adapter.smarter." + modelReaderName + "Reader";

    if (modelReaderName == null)
      return "unrecognized file format";

    try {
      Class modelReaderClass = Class.forName(className);
      modelReader = (ModelReader)modelReaderClass.newInstance();
    } catch (Exception e) {
      String err = "Could not instantiate:" + className;
      logger.log(err);
      return err;
    }

    modelReader.setLogger(logger);
    modelReader.initialize();

    Model model = modelReader.readModel(bufferedReader);
    if (model.errorMessage != null)
      return model.errorMessage;
    return model;
  }

  static String determineModelReader(BufferedReader bufferedReader,
                                     ModelAdapter.Logger logger)
    throws Exception {
    bufferedReader.mark(512);
    String[] lines = new String[4];
    for (int i = 0; i < lines.length; ++i) {
      // this is not really correct ... should probably stay null
      String line = bufferedReader.readLine();
      lines[i] = line != null ? line : "";
    }
    bufferedReader.reset();
    try {
      int atomCount = Integer.parseInt(lines[0].trim());
      return "Xyz";
    } catch (NumberFormatException e) {
    }
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
  { "data_" };

  final static String[][] startsWithRecords =
  { pdbRecords, shelxRecords, cifRecords };

  final static String[] startsWithFormats =
  { "Pdb", "Shelx", "Cif" };

  ////////////////////////////////////////////////////////////////
  // contains formats
  ////////////////////////////////////////////////////////////////
  
  final static String[] cmlRecords =
  { "<?xml", "<atom", "<molecule", "<reaction", "<cml", "<bond", ".dtd\"",
    "<list>", "<entry", "<identifier" };

  final static String[] gaussianRecords =
  { "Entering Gaussian System", "1998 Gaussian, Inc." };


  final static String[][] containsRecords =
  {cmlRecords, gaussianRecords};

  final static String[] containsFormats =
  { "Cml", "Gaussian" };
}
