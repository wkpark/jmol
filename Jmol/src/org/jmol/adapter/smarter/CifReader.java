/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Chemistry Development Kit (CDK) project
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.jmol.adapter.smarter;

import org.jmol.api.ModelAdapter;

import java.io.BufferedReader;
import java.util.StringTokenizer;


/**
 * This is not a reader for the CIF and mmCIF crystallographic formats.
 * It is able, however, to extract some content from it.
 * It's very ad hoc, not written
 * using any dictionary. So please complain if something is not working.
 * In addition, the things it does read are considered experimental.
 */
public class CifReader extends ModelReader {

  float[] notionalUnitcell;

  BufferedReader reader;

  void initialize() {
    notionalUnitcell = new float[6];
    for (int i = 6; --i >= 0; )
      notionalUnitcell[i] = Float.NaN;
  }

  Model readModel(BufferedReader reader) throws Exception {
    this.reader = reader;
    model = new Model(ModelAdapter.MODEL_TYPE_OTHER);
    
    String line;
    while ((line = reader.readLine()) != null) {
      if (line.length() == 0)
        continue;
      char firstChar = line.charAt(0);
      if (firstChar == '#')
        continue;
      if ((firstChar != '_') &&
          ! line.startsWith("loop")) {
        logger.log("skipping unrecognized line", line);
        continue;
      }
      /* determine CIF command */
      int spaceIndex = line.indexOf(' ');
      if (spaceIndex == -1)
        spaceIndex = line.length();
      String command = line.substring(0, spaceIndex);
      if (command.startsWith("_cell")) {
        processCellParameter(command, line, spaceIndex);
        continue;
      }
      if ("loop_".equals(command)) {
        processLoopBlock();
        continue;
      }
      if ("_symmetry_space_group_name_H-M".equals(command)) {
        model.spaceGroup = line.substring(29).trim();
        continue;
      }
      // skip command
    }
    return model;
  }
  
  final static String[] cellParamNames =
  {"_cell.length_a", "_cell.length_b", "_cell.length_c",
   "_cell.angle_alpha", "_cell.angle_beta", "_cell.angle.gamma"};

  void processCellParameter(String command, String line, int spaceIndex) {
    for (int i = cellParamNames.length; --i >= 0; )
      if (command.equals(cellParamNames[i])) {
        notionalUnitcell[i] = parseFloat(line, spaceIndex);
        return;
      }
  }
  
  private void processLoopBlock() throws Exception {
    String line = reader.readLine().trim();
    if (line.startsWith("_atom")) {
      processAtomLoopBlock(line);
      return;
    }
    logger.log("Skipping loop block");
    skipUntilEmptyOrCommentLine(line);
  }

  private void skipUntilEmptyOrCommentLine(String line) throws Exception {
    // skip everything until empty line, or comment line
    while (line != null && line.length() > 0 && line.charAt(0) != '#') {
      line = reader.readLine().trim();
    }
  }

  final static String[] atomFields = {
    "NADA", // this will not match 
    "_atom_site.type_symbol",
    "_atom_site_label",
    "_atom_site_label_atom_id",
    "_atom_site_fract_x",
    "_atom_site_fract_y",
    "_atom_site_fract_z",
    "_atom_site.Cartn_x",
    "_atom_site.Cartn_y",
    "_atom_site.Cartn_z",
  };

  final static int NONE = 0;
  final static int SYMBOL = 1;
  final static int LABEL = 2;
  final static int FRACT_X = 3;
  final static int FRACT_Y = 4;
  final static int FRACT_Z = 5;
  final static int CARTN_X = 6;
  final static int CARTN_Y = 7;
  final static int CARTN_Z = 8;

  void processAtomLoopBlock(String firstLine) throws Exception {
    String line = firstLine;
    int fieldCount = 0;
    int[] fieldTypes = new int[100]; // should be enough
    boolean hasParsableInformation = false;
    for (;
         line != null && line.length() > 0 && line.charAt(0) == '_';
         ++fieldCount, line = reader.readLine().trim()) {
      for (int i = atomFields.length; --i >= 0; )
        if (line.equals(atomFields[i])) {
          hasParsableInformation = true;
          fieldTypes[fieldCount] = i;
          break;
        }
      logger.log("unrecognized atom field", line);
    }

    if (hasParsableInformation == false ) {
      logger.log("No parsable info found :-(");
      skipUntilEmptyOrCommentLine(line);
      return;
    }
    // now that headers are parsed, read the data
    for (;
         line != null && line.length() > 0 && line.charAt(0) != '#';
         line = reader.readLine().trim()) {
      StringTokenizer tokenizer = new StringTokenizer(line);
      if (tokenizer.countTokens() < fieldCount) {
        logger.log("Column count mismatch; assuming continued on next line");
        tokenizer = new StringTokenizer(line + reader.readLine());
      }
      Atom atom = model.newAtom();
      for (int i = 0; i < fieldCount; ++i) {
        String field = tokenizer.nextToken();
        logger.log("Parsing col,token: " + i + "=" + field);
        switch (i) {
        case NONE:
          break;
        case SYMBOL:
          atom.elementSymbol = field;
          break;
        case LABEL:
          atom.atomName = field;
          break;
        case CARTN_X:
        case FRACT_X:
          atom.x = parseFloat(field);
          break;
        case CARTN_Y:
        case FRACT_Y:
          atom.y = parseFloat(field);
          break;
        case CARTN_Z:
        case FRACT_Z:
          atom.z = parseFloat(field);
        }
      }
    }
  }
}
