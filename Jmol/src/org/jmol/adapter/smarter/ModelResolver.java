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

    if (modelReaderName == "Cml")
      return "CML not yet supported";

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
    for (int i = pdbRecords.length; --i >= 0; ) {
      String recordTag = pdbRecords[i];
      for (int j = lines.length; --j >= 0; )
        if (lines[j].startsWith(recordTag))
          return "Pdb";
    }
    for (int i = cmlRecords.length; --i >= 0; ) {
      String cmlTag = cmlRecords[i];
      for (int j = lines.length; --j >= 0; )
        if (lines[j].indexOf(cmlTag) != -1)
          return "Cml";
    }
    for (int i = shelxRecords.length; --i >= 0; ) {
      String shelxTag = shelxRecords[i];
      for (int j = lines.length; --j >= 0; )
        if (lines[j].startsWith(shelxTag))
          return "Shelx";
    }
    if (lines[1] == null || lines[1].trim().length() == 0)
      return "Jme"; // this is really quite broken :-)
    return null;
  }

  final static String[] pdbRecords = {
    "HEADER", "OBSLTE", "TITLE ", "CAVEAT", "COMPND", "SOURCE", "KEYWDS",
    "EXPDTA", "AUTHOR", "REVDAT", "SPRSDE", "JRNL  ", "REMARK",

    "DBREF ", "SEQADV", "SEQRES", "MODRES", 

    "HELIX ", "SHEET ", "TURN  ",

    "CRYST1", "ORIGX1", "ORIGX2", "ORIGX3", "SCALE1", "SCALE2", "SCALE3",

    "ATOM  ", "HETATM", "MODEL ",
  };

  final static String[] cmlRecords =
  { "<atom", "<molecule", "<reaction", "<cml", "<bond", "\"cml.dtd\""};

  final static String[] shelxRecords =
  {"TITL ", "ZERR ", "LATT ", "SYMM ", "CELL "};
}
