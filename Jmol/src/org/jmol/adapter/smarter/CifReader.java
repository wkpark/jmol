/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004  The Jmol Development Team
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

class CifReader extends ModelReader {

  float[] notionalUnitcell;

  BufferedReader reader;
  String line;
  SpaceAndSingleQuoteTokenizer tokenizer = new SpaceAndSingleQuoteTokenizer();

  void initialize() {
    notionalUnitcell = new float[6];
    for (int i = 6; --i >= 0; )
      notionalUnitcell[i] = Float.NaN;
  }

  Model readModel(BufferedReader reader) throws Exception {
    this.reader = reader;
    model = new Model("cif");
    
    while ((line = reader.readLine()) != null) {
      if (line.length() == 0)
        continue;
      char firstChar = line.charAt(0);
      if (firstChar == '#')
        continue;
      if (firstChar != '_') {
        if (line.startsWith("data_")) {
          processDataParameter();
          continue;
        }
        if (line.startsWith("loop_")) {
          processLoopBlock();
          continue;
        }
        continue;
      }
        
      int spaceIndex = line.indexOf(' ');
      if (spaceIndex == -1)
        spaceIndex = line.length();
      String command = line.substring(0, spaceIndex);
      if (command.startsWith("_cell")) {
        processCellParameter(command, spaceIndex);
        continue;
      }
      if ("_symmetry_space_group_name_H-M".equals(command)) {
        model.spaceGroup = line.substring(29).trim();
        continue;
      }
      // skip command
    }
    checkUnitcell();
    return model;
  }
  

  final static boolean isMatch(String str1, String str2) {
    int cch = str1.length();
    if (str2.length() != cch)
      return false;
    for (int i = cch; --i >= 0; ) {
      char ch1 = str1.charAt(i);
      char ch2 = str2.charAt(i);
      if (ch1 == ch2)
        continue;
      if ((ch1 == '_' || ch1 == '.') &&
          (ch2 == '_' || ch2 == '.'))
        continue;
      if (ch1 <= 'Z' && ch1 >= 'A')
        ch1 += 'a' - 'A';
      else if (ch2 <= 'Z' && ch2 >= 'A')
        ch2 += 'a' - 'A';
      if (ch1 != ch2)
        return false;
    }
    return true;
  }
  

  void processDataParameter() {
    String modelName = line.substring(5).trim();
    if (modelName.length() > 0)
      model.modelName = modelName;
  }
  
  final static String[] cellParamNames =
  {"_cell_length_a", "_cell_length_b", "_cell_length_c",
   "_cell_angle_alpha", "_cell_angle_beta", "_cell_angle_gamma"};
  
  void processCellParameter(String command, int spaceIndex) {
    for (int i = cellParamNames.length; --i >= 0; )
      if (isMatch(command, cellParamNames[i])) {
        notionalUnitcell[i] = parseFloat(line, spaceIndex);
        return;
      }
  }
  
  void checkUnitcell() {
    for (int i = 6; --i >= 0; ) {
      if (Float.isNaN(notionalUnitcell[i]))
        return;
    }
    model.notionalUnitcell = notionalUnitcell;
  }
  
  private void processLoopBlock() throws Exception {
    line = reader.readLine().trim();
    if (line.startsWith("_atom_site")) {
      processAtomSiteLoopBlock();
      return;
    }
    if (line.startsWith("_geom_bond")) {
      processGeomBondLoopBlock();
      return;
    }
    if (line.startsWith("_struct_conf") &&
        !line.startsWith("_struct_conf_type")) {
      processStructConfLoopBlock();
      return;
    }
    if (line.startsWith("_struct_sheet_range")) {
      processStructSheetRangeLoopBlock();
      return;
    }
    //    logger.log("Skipping loop block:" + line);
    skipUntilEmptyOrCommentLine();
  }
  
  private void skipUntilEmptyOrCommentLine() throws Exception {
    // skip everything until empty line, or comment line
    while (line != null &&
           (line = line.trim()).length() > 0 &&
           line.charAt(0) != '#') {
      line = reader.readLine();
    }
  }
  
  ////////////////////////////////////////////////////////////////
  // atom data
  ////////////////////////////////////////////////////////////////

  final static byte NONE      = 0;
  final static byte SYMBOL    = 1;
  final static byte LABEL     = 2;
  final static byte FRACT_X   = 3;
  final static byte FRACT_Y   = 4;
  final static byte FRACT_Z   = 5;
  final static byte CARTN_X   = 6;
  final static byte CARTN_Y   = 7;
  final static byte CARTN_Z   = 8;
  final static byte OCCUPANCY = 9;
  final static byte B_ISO     = 10;
  final static byte COMP_ID   = 11;
  final static byte ASYM_ID   = 12;
  final static byte SEQ_ID    = 13;
  final static byte INS_CODE  = 14;
  final static byte ATOM_PROPERTY_MAX = 15;
  

  final static String[] atomFields = {
    "_atom_site_type_symbol",
    "_atom_site_label", "_atom_site_label_atom_id",
    "_atom_site_fract_x", "_atom_site_fract_y", "_atom_site_fract_z",
    "_atom_site.Cartn_x", "_atom_site.Cartn_y", "_atom_site.Cartn_z",
    "_atom_site_occupancy",
    "_atom_site.b_iso_or_equiv",
    "_atom_site.label_comp_id", "_atom_site.label_asym_id",
    "_atom_site.label_seq_id", "_atom_site.pdbx_PDB_ins_code",
  };

  // don't forget to deal with alternate conformations!
  // or, even better, move that code out of the ModelReader

  final static byte[] atomFieldMap = {
    SYMBOL,
    LABEL, LABEL,
    FRACT_X, FRACT_Y, FRACT_Z,
    CARTN_X, CARTN_Y, CARTN_Z,
    OCCUPANCY, B_ISO,
    COMP_ID, ASYM_ID, SEQ_ID, INS_CODE,
  };

  static {
    if (atomFieldMap.length != atomFields.length)
      atomFields[100] = "explode";
  }

  void processAtomSiteLoopBlock() throws Exception {
    int[] fieldTypes = new int[100]; // should be enough
    boolean[] atomPropertyReferenced = new boolean[ATOM_PROPERTY_MAX];
    int fieldCount = parseLoopParameters(atomFields,
                                         atomFieldMap,
                                         fieldTypes,
                                         atomPropertyReferenced);
    // now that headers are parsed, check to see if we want
    // cartesian or fractional coordinates;
    if (atomPropertyReferenced[CARTN_X]) {
      for (int i = FRACT_X; i < FRACT_Z; ++i)
        disableField(fieldCount, fieldTypes, i);
    } else if (atomPropertyReferenced[FRACT_X]) {
      model.coordinatesAreFractional = true;
      for (int i = CARTN_X; i < CARTN_Z; ++i)
        disableField(fieldCount, fieldTypes, i);
    } else {
      // it is a different kind of _atom_site loop block
      //      logger.log("?que? no atom coordinates found");
      skipUntilEmptyOrCommentLine();
      return;
    }

    for (; line != null &&
           (line = line.trim()).length() > 0 &&
           line.charAt(0) != '#';
         line = reader.readLine()) {
      tokenizer.setString(line);
      Atom atom = model.newAtom();
      for (int i = 0; i < fieldCount; ++i) {
        if (! tokenizer.hasMoreTokens())
          tokenizer.setString(reader.readLine());
        String field = tokenizer.nextToken();
        switch (fieldTypes[i]) {
        case NONE:
          break;
        case SYMBOL:
          atom.elementSymbol = field;
          break;
        case LABEL:
          atom.atomName = field;
          model.mapMostRecentAtomName();
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
          break;
        case OCCUPANCY:
          float floatOccupancy = parseFloat(field);
          if (! Float.isNaN(floatOccupancy))
            atom.occupancy = (int)(floatOccupancy * 100);
          break;
        case B_ISO:
          atom.bfactor = parseFloat(field);
          break;
        case COMP_ID:
          atom.group3 = field;
          break;
        case ASYM_ID:
          if (field.length() > 1)
            logger.log("Don't know how to deal with chains more than 1 char",
                       field);
          char firstChar = field.charAt(0);
          if (firstChar != '?' && firstChar != '.')
            atom.chainID = firstChar;
          break;
        case SEQ_ID:
          atom.sequenceNumber = parseInt(field);
          break;
        case INS_CODE:
          char insCode = field.charAt(0);
          if (insCode != '?' && insCode != '.')
            atom.chainID = insCode;
          break;
        }
      }
    }
  }

  void disableField(int fieldCount, int[] fieldTypes, int fieldIndex) {
    for (int i = fieldCount; --i >= 0; )
      if (fieldTypes[i] == fieldIndex)
        fieldTypes[i] = 0;
  }

  int parseLoopParameters(String[] fields,
                          byte[] fieldMap,
                          int[] fieldTypes,
                          boolean[] propertyReferenced) throws Exception {
    int fieldCount = 0;
    outer_loop:
    for (; line != null &&
           (line = line.trim()).length() > 0 &&
           line.charAt(0) == '_';
         ++fieldCount, line = reader.readLine()) {
      for (int i = fields.length; --i >= 0; )
        if (isMatch(line, fields[i])) {
          int iproperty = fieldMap[i];
          propertyReferenced[iproperty] = true;
          fieldTypes[fieldCount] = iproperty;
          continue outer_loop;
        }
    }
    return fieldCount;
  }

  ////////////////////////////////////////////////////////////////
  // bond data
  ////////////////////////////////////////////////////////////////

  final static byte GEOM_BOND_ATOM_SITE_LABEL_1 = 1;
  final static byte GEOM_BOND_ATOM_SITE_LABEL_2 = 2;
  //  final static byte GEOM_BOND_DISTANCE          = 3;
  final static byte GEOM_BOND_PROPERTY_MAX      = 3;

  final static String[] geomBondFields = {
    "_geom_bond_atom_site_label_1",
    "_geom_bond_atom_site_label_2",
    //    "_geom_bond_distance",
  };

  final static byte[] geomBondFieldMap = {
    GEOM_BOND_ATOM_SITE_LABEL_1,
    GEOM_BOND_ATOM_SITE_LABEL_2,
    //    GEOM_BOND_DISTANCE,
  };

  void processGeomBondLoopBlock() throws Exception {
    int[] fieldTypes = new int[100]; // should be enough
    boolean[] propertyReferenced = new boolean[GEOM_BOND_PROPERTY_MAX];
    int fieldCount = parseLoopParameters(geomBondFields,
                                         geomBondFieldMap,
                                         fieldTypes,
                                         propertyReferenced);
    for (int i = GEOM_BOND_PROPERTY_MAX; --i > 0; ) // only > 0, not >= 0
      if (! propertyReferenced[i]) {
        logger.log("?que? missing _geom_bond property:" + i);
        skipUntilEmptyOrCommentLine();
        return;
      }

    for (; line != null &&
           (line = line.trim()).length() > 0 &&
           line.charAt(0) != '#';
         line = reader.readLine()) {
      tokenizer.setString(line);
      Bond bond = new Bond();
      for (int i = 0; i < fieldCount; ++i) {
        if (! tokenizer.hasMoreTokens())
          tokenizer.setString(reader.readLine());
        String field = tokenizer.nextToken();
        switch (fieldTypes[i]) {
        case NONE:
          break;
        case GEOM_BOND_ATOM_SITE_LABEL_1:
          bond.atomIndex1 = model.getAtomNameIndex(field);
          break;
        case GEOM_BOND_ATOM_SITE_LABEL_2:
          bond.atomIndex2 = model.getAtomNameIndex(field);
          break;
        }
      }
      if (bond.atomIndex1 >= 0 && bond.atomIndex2 >= 0)
        model.addBond(bond);
    }
  }

  ////////////////////////////////////////////////////////////////
  // helix and turn structure data
  ////////////////////////////////////////////////////////////////

  final static byte CONF_TYPE_ID     = 1;
  final static byte BEG_ASYM_ID      = 2;
  final static byte BEG_SEQ_ID       = 3;
  final static byte BEG_INS_CODE     = 4;
  final static byte END_ASYM_ID      = 5;
  final static byte END_SEQ_ID       = 6;
  final static byte END_INS_CODE     = 7;
  final static byte STRUCT_CONF_PROPERTY_MAX = 8;

  final static String[] structConfFields = {
    "_struct_conf.conf_type_id",

    "_struct_conf.beg_label_asym_id",
    "_struct_conf.beg_label_seq_id",
    "_struct_conf.pdbx_beg_PDB_ins_code",
    
    "_struct_conf.end_label_asym_id",
    "_struct_conf.end_label_seq_id",
    "_struct_conf.pdbx_end_PDB_ins_code",
  };

  final static byte[] structConfFieldMap = {
    CONF_TYPE_ID,
    BEG_ASYM_ID, BEG_SEQ_ID, BEG_INS_CODE,
    END_ASYM_ID, END_SEQ_ID, END_INS_CODE,
  };

  void processStructConfLoopBlock() throws Exception {
    int[] fieldTypes = new int[100]; // should be enough
    boolean[] propertyReferenced = new boolean[STRUCT_CONF_PROPERTY_MAX];
    int fieldCount = parseLoopParameters(structConfFields,
                                         structConfFieldMap,
                                         fieldTypes,
                                         propertyReferenced);
    for (int i = STRUCT_CONF_PROPERTY_MAX; --i > 0; ) // only > 0, not >= 0
      if (! propertyReferenced[i]) {
        logger.log("?que? missing _struct_conf property:" + i);
        skipUntilEmptyOrCommentLine();
        return;
      }

    for (; line != null &&
           (line = line.trim()).length() > 0 &&
           line.charAt(0) != '#';
         line = reader.readLine()) {
      tokenizer.setString(line);
      Structure structure = new Structure();
      
      for (int i = 0; i < fieldCount; ++i) {
        if (! tokenizer.hasMoreTokens())
          tokenizer.setString(reader.readLine());
        String field = tokenizer.nextToken();
        char firstChar = field.charAt(0);
        switch (fieldTypes[i]) {
        case NONE:
          break;
        case CONF_TYPE_ID:
          if (field.startsWith("HELX"))
            structure.structureType = "helix";
          else if (field.startsWith("TURN"))
            structure.structureType = "turn";
          else
            structure.structureType = "none";
          break;
        case BEG_ASYM_ID:
          structure.chainID =
            (firstChar == '.' || firstChar == '?') ? ' ' : firstChar;
          break;
        case BEG_SEQ_ID:
          structure.startSequenceNumber = parseInt(field);
          break;
        case BEG_INS_CODE:
          structure.startInsertionCode =
            (firstChar == '.' || firstChar == '?') ? ' ' : firstChar;
          break;
        case END_SEQ_ID:
          structure.endSequenceNumber = parseInt(field);
          break;
        case END_INS_CODE:
          structure.endInsertionCode =
            (firstChar == '.' || firstChar == '?') ? ' ' : firstChar;
          break;
        }
      }
      model.addStructure(structure);
    }
  }

  ////////////////////////////////////////////////////////////////
  // sheet structure data
  ////////////////////////////////////////////////////////////////

  // note that the conf_id is not used
  final static byte STRUCT_SHEET_RANGE_PROPERTY_MAX = 8;

  final static String[] structSheetRangeFields = {
    "_struct_sheet_range.beg_label_asym_id",
    "_struct_sheet_range.beg_label_seq_id",
    "_struct_sheet_range.pdbx_beg_PDB_ins_code",
    
    "_struct_sheet_range.end_label_asym_id",
    "_struct_sheet_range.end_label_seq_id",
    "_struct_sheet_range.pdbx_end_PDB_ins_code",
  };

  final static byte[] structSheetRangeFieldMap = {
    BEG_ASYM_ID, BEG_SEQ_ID, BEG_INS_CODE,
    END_ASYM_ID, END_SEQ_ID, END_INS_CODE,
  };

  void processStructSheetRangeLoopBlock() throws Exception {
    int[] fieldTypes = new int[100]; // should be enough
    boolean[] propertyReferenced =
      new boolean[STRUCT_SHEET_RANGE_PROPERTY_MAX];
    int fieldCount = parseLoopParameters(structSheetRangeFields,
                                         structSheetRangeFieldMap,
                                         fieldTypes,
                                         propertyReferenced);
    for (int i = STRUCT_SHEET_RANGE_PROPERTY_MAX; --i > 1; )
      if (! propertyReferenced[i]) {
        logger.log("?que? missing _struct_conf property:" + i);
        skipUntilEmptyOrCommentLine();
        return;
      }

    for (; line != null &&
           (line = line.trim()).length() > 0 &&
           line.charAt(0) != '#';
         line = reader.readLine()) {
      tokenizer.setString(line);
      Structure structure = new Structure();
      structure.structureType = "sheet";
      
      for (int i = 0; i < fieldCount; ++i) {
        if (! tokenizer.hasMoreTokens())
          tokenizer.setString(reader.readLine());
        String field = tokenizer.nextToken();
        char firstChar = field.charAt(0);
        switch (fieldTypes[i]) {
        case BEG_ASYM_ID:
          structure.chainID =
            (firstChar == '.' || firstChar == '?') ? ' ' : firstChar;
          break;
        case BEG_SEQ_ID:
          structure.startSequenceNumber = parseInt(field);
          break;
        case BEG_INS_CODE:
          structure.startInsertionCode =
            (firstChar == '.' || firstChar == '?') ? ' ' : firstChar;
          break;
        case END_SEQ_ID:
          structure.endSequenceNumber = parseInt(field);
          break;
        case END_INS_CODE:
          structure.endInsertionCode =
            (firstChar == '.' || firstChar == '?') ? ' ' : firstChar;
          break;
        }
      }
      model.addStructure(structure);
    }
  }

  ////////////////////////////////////////////////////////////////
  // special tokenizer class
  ////////////////////////////////////////////////////////////////

  class SpaceAndSingleQuoteTokenizer {
    String str;
    int ich;
    int cch;

    void setString(String str) {
      if (str == null)
        str = "";
      this.str = str;
      cch = str.length();
      ich = 0;
    }

    boolean hasMoreTokens() {
      while (ich < cch && (str.charAt(ich)) == ' ')
        ++ich;
      return ich < cch;
    }

    String nextToken() {
      if (ich == cch)
        return null;
      int ichStart = ich;
      if (str.charAt(ichStart) != '\'') {
        while (ich < cch && str.charAt(ich) != ' ')
          ++ich;
        return str.substring(ichStart, ich);
      }
      boolean embeddedQuote = false;
      boolean containsEmbeddedQuote = false;
      while (++ich < cch &&
             (str.charAt(ich) != '\'' ||
              (embeddedQuote = (ich+1 < cch && str.charAt(ich+1) == '\''))))
        if (embeddedQuote) {
          ++ich;
          embeddedQuote = false;
          containsEmbeddedQuote = true;
        }
      if (ich == cch) {
        // reached the end of the string without finding closing '
        return str.substring(ichStart, ich);
      }
      String token = str.substring(ichStart + 1, ich++);
      if (! containsEmbeddedQuote)
        return token;
      StringBuffer sb = new StringBuffer(token);
      for (int i = sb.length(); --i >= 0; )
        if (sb.charAt(i) == '\'') // found a quote
          sb.deleteCharAt(i--); // skip over the previous one
      return new String(sb);
    }
  }
}



