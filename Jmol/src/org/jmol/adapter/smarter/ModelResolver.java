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

  final static int UNKNOWN = -1;
  final static int XYZ = 0;
  final static int MOL = 1;
  final static int JME = 2;
  final static int PDB = 3;

  static Object resolveModel(String name, BufferedReader bufferedReader)
    throws Exception {
    Model model;
    switch (determineModel(bufferedReader)) {
    case XYZ:
      model = new XyzModel(bufferedReader);
      break;
    case MOL:
      model = new MolModel(bufferedReader);
      break;
    case JME:
      model = new JmeModel(bufferedReader);
      break;
    case PDB:
      model = new PdbModel(bufferedReader);
      break;
    default:
      return "unrecognized file format";
    }
    if (model.errorMessage != null)
      return model.errorMessage;
    return model;
  }

  static int determineModel(BufferedReader bufferedReader)
    throws Exception {
    bufferedReader.mark(512);
    String line1 = bufferedReader.readLine();
    String line2 = bufferedReader.readLine();
    String line3 = bufferedReader.readLine();
    String line4 = bufferedReader.readLine();
    bufferedReader.reset();
    if (line1 == null)
      line1 = "";
    if (line2 == null)
      line2 = "";
    if (line3 == null)
      line3 = "";
    if (line4 == null)
      line4 = "";
    try {
      int atomCount = Integer.parseInt(line1.trim());
      return XYZ;
    } catch (NumberFormatException e) {
    }
    if (line4.length() >= 6) {
      String line4trimmed = line4.trim();
      if (line4trimmed.endsWith("V2000") ||
          line4trimmed.endsWith("v2000") ||
          line4trimmed.endsWith("V3000"))
        return MOL;
      try {
        Integer.parseInt(line4.substring(0, 3).trim());
        Integer.parseInt(line4.substring(3, 6).trim());
        return MOL;
      } catch (NumberFormatException nfe) {
      }
    }
    for (int i = pdbRecords.length; --i >= 0; ) {
      String recordTag = pdbRecords[i];
      if (line1.startsWith(recordTag) || line2.startsWith(recordTag) ||
          line3.startsWith(recordTag) || line4.startsWith(recordTag))
        return PDB;
    }
    if (line2 == null || line2.trim().length() == 0)
      return JME;
    return UNKNOWN;
  }

  private final static String[] pdbRecords = {
    "HEADER", "OBSLTE", "TITLE ", "CAVEAT", "COMPND", "SOURCE", "KEYWDS",
    "EXPDTA", "AUTHOR", "REVDAT", "SPRSDE", "JRNL  ", "REMARK",

    "DBREF ", "SEQADV", "SEQRES", "MODRES", 

    "HELIX ", "SHEET ", "TURN  ",

    "CRYST1", "ORIGX1", "ORIGX2", "ORIGX3", "SCALE1", "SCALE2", "SCALE3",

    "ATOM  ", "HETATM", "MODEL ",
  };
}
