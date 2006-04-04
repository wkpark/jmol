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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * CIF file reader for CIF and mmCIF files.
 *
 *<p>
 * <a href='http://www.iucr.org/iucr-top/cif/'>
 * http://www.iucr.org/iucr-top/cif/
 * </a>
 *
 * @author Miguel <miguel@jmol.org>
 */
class CifReader extends AtomSetCollectionReader {

  float[] notionalUnitcell;

  BufferedReader reader;
  String line;
  RidiculousFileFormatTokenizer tokenizer =
    new RidiculousFileFormatTokenizer();
  Map strandsMap;

  void initialize() {
    notionalUnitcell = new float[6];
    for (int i = 6; --i >= 0; )
      notionalUnitcell[i] = Float.NaN;
  }

  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("cif");
    strandsMap = new Hashtable();
    
    // this loop is a little tricky
    // the CIF format seems to generate lots of problems for parsers
    // the top of this loop should be ready to process the current line
    // pay careful attention to the 'break' and 'continue' sequence
    // or you will get stuck in an infinite loop
    line = reader.readLine();
    while (line != null) {
      if (line.startsWith("loop_")) {
        processLoopBlock();
        // there is already an unprocessed line in the firing chamber
        continue;
      } else if (line.startsWith("data_")) {
        processDataParameter();
      } else if (line.startsWith("_cell_") || line.startsWith("_cell.")) {
        processCellParameter();
      } else if (line.startsWith("_symmetry_space_group_name_H-M")) {
        processSymmetrySpaceGroupNameHM();
      } else if (line.startsWith("_struct_asym")) {
        processStructAsymBlock();
      }
      line = reader.readLine();
    }
    checkUnitcell();
    logger.log("Done reading... Found these strands:");
    Iterator strands = strandsMap.values().iterator();
    while (strands.hasNext()) {
      Strand strand = (Strand)strands.next();
      logger.log(strand.toString());
    }
    return atomSetCollection;
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
    String collectionName = line.substring(5).trim();
    if (collectionName.length() > 0)
      atomSetCollection.collectionName = collectionName;
  }
  
  void processSymmetrySpaceGroupNameHM() {
    atomSetCollection.spaceGroup = line.substring(29).trim();
  }

  final static String[] cellParamNames =
  {"_cell_length_a", "_cell_length_b", "_cell_length_c",
   "_cell_angle_alpha", "_cell_angle_beta", "_cell_angle_gamma"};
  
  void processCellParameter() {
    //    logger.log("processCellParameter() line:" + line);
    String cellParameter = parseToken(line);
    for (int i = cellParamNames.length; --i >= 0; )
      if (isMatch(cellParameter, cellParamNames[i])) {
        notionalUnitcell[i] = parseFloat(line, ichNextParse);
        //        logger.log("value=" + notionalUnitcell[i]);
        return;
      }
    //    logger.log("NOT");
  }
  
  void checkUnitcell() {
    for (int i = 6; --i >= 0; ) {
      if (Float.isNaN(notionalUnitcell[i]))
        return;
    }
    atomSetCollection.notionalUnitcell = notionalUnitcell;
  }
  
  private void processLoopBlock() throws Exception {
    //    logger.log("processLoopBlock()-------------------------");
    line = reader.readLine().trim();
    //    logger.log("trimmed line:" + line);
    if (line.startsWith("_atom_site")) {
      processAtomSiteLoopBlock();
      return;
    }
    if (line.startsWith("_struct_asym")) {
      processStructAsymBlock();
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
    if (line.startsWith("_pdbx_poly_seq_scheme")) {
      processPolySeqSchemeLoopBlock();
      return;
    }
    if (line.startsWith("_pdbx_nonpoly_scheme")) {
      processNonPolySchemeLoopBlock();
      return;
    }
    if (line.startsWith("_struct_sheet_range")) {
      processStructSheetRangeLoopBlock();
      return;
    }
    //    logger.log("Skipping loop block:" + line);
    skipLoopHeaders();
    skipLoopData();
  }
  
  private void skipLoopHeaders() throws Exception {
    // skip everything that begins with _
    while (line != null &&
           (line = line.trim()).length() > 0 &&
           line.charAt(0) == '_') {
      line = reader.readLine();
    }
  }

  private void skipLoopData() throws Exception {
    // skip everything until empty line, or comment line
    // or start of a new loop_ or data_
    char ch;
    while (line != null &&
           (line = line.trim()).length() > 0 &&
           (ch = line.charAt(0)) != '_' &&
           ch != '#' &&
           ! line.startsWith("loop_") &&
           ! line.startsWith("data_")) {
      //      logger.log("skipLoopData just discarded:" + line);
      line = reader.readLine();
    }
  }
  
  ////////////////////////////////////////////////////////////////
  // atom data
  ////////////////////////////////////////////////////////////////

  final static byte NONE        = 0;
  final static byte TYPE_SYMBOL = 1;
  final static byte LABEL       = 2;
  final static byte FRACT_X     = 3;
  final static byte FRACT_Y     = 4;
  final static byte FRACT_Z     = 5;
  final static byte CARTN_X     = 6;
  final static byte CARTN_Y     = 7;
  final static byte CARTN_Z     = 8;
  final static byte OCCUPANCY   = 9;
  final static byte B_ISO       = 10;
  final static byte COMP_ID     = 11;
  final static byte ASYM_ID     = 12;
  final static byte SEQ_ID      = 13;
  final static byte INS_CODE    = 14;
  final static byte ALT_ID      = 15;
  final static byte GROUP_PDB   = 16;
  final static byte MODEL_NO    = 17;
  final static byte ATOM_PROPERTY_MAX = 18;
  

  final static String[] atomFields = {
    "_atom_site_type_symbol",
    "_atom_site_label", "_atom_site_label_atom_id",
    "_atom_site_fract_x", "_atom_site_fract_y", "_atom_site_fract_z",
    "_atom_site.Cartn_x", "_atom_site.Cartn_y", "_atom_site.Cartn_z",
    "_atom_site_occupancy",
    "_atom_site.b_iso_or_equiv",
    "_atom_site.label_comp_id", "_atom_site.label_asym_id",
    "_atom_site.label_seq_id", "_atom_site.pdbx_PDB_ins_code",
    "_atom_site.label_alt_id",
    "_atom_site.group_PDB",
    "_atom_site.pdbx_PDB_model_num",
  };

  final static byte[] atomFieldMap = {
    TYPE_SYMBOL,
    LABEL, LABEL,
    FRACT_X, FRACT_Y, FRACT_Z,
    CARTN_X, CARTN_Y, CARTN_Z,
    OCCUPANCY, B_ISO,
    COMP_ID, ASYM_ID, SEQ_ID, INS_CODE,
    ALT_ID, GROUP_PDB, MODEL_NO,
  };

  static {
    if (atomFieldMap.length != atomFields.length)
      atomFields[100] = "explode";
  }

  void processAtomSiteLoopBlock() throws Exception {
    //    logger.log("processAtomSiteLoopBlock()-------------------------");
    int currentModelNO = -1;
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
      atomSetCollection.coordinatesAreFractional = true;
      for (int i = CARTN_X; i < CARTN_Z; ++i)
        disableField(fieldCount, fieldTypes, i);
    } else {
      // it is a different kind of _atom_site loop block
      //      logger.log("?que? no atom coordinates found");
      skipLoopData();
      return;
    }

    for (; line != null; line = reader.readLine()) {
      int lineLength = line.length();
      if (lineLength == 0)
        break;
      char chFirst = line.charAt(0);
      if (chFirst == '#' ||
          chFirst == '_' ||
          line.startsWith("loop_") ||
          line.startsWith("data_"))
        break;
      if (chFirst == ' ') {
        int i;
        for (i = lineLength; --i >= 0 && line.charAt(i) == ' '; ) {
        }
        if (i < 0)
          break;
      }
      //      logger.log("line:" + line);
      //      logger.log("of length = " + line.length());
      if (line.length() == 1)
        System.out.println("char value is " + (chFirst + 0));
      tokenizer.setString(line);
      //      logger.log("reading an atom");
      Atom atom = new Atom();
      for (int i = 0; i < fieldCount; ++i) {
        if (! tokenizer.hasMoreTokens())
          tokenizer.setString(reader.readLine());
        String field = tokenizer.nextToken();
        if (field == null)
          System.out.println("field == null!");
        switch (fieldTypes[i]) {
        case NONE:
          break;
        case TYPE_SYMBOL:
          String elementSymbol;
          if (field.length() < 2) {
            elementSymbol = field;
          } else {
            char ch0 = field.charAt(0);
            char ch1 = Character.toLowerCase(field.charAt(1));
            if (Atom.isValidElementSymbol(ch0, ch1))
              elementSymbol = "" + ch0 + ch1;
            else
              elementSymbol = "" + ch0;
          }
          atom.elementSymbol = elementSymbol;
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
          if (firstChar != '?' && firstChar != '.') {
            String chainID = "" + firstChar;
            if (strandsMap.containsKey(chainID)) {
              Strand strand = (Strand)strandsMap.get(chainID);
              // OK, here is the if/else construct that Wayne send me
              if (strand.isBlank.booleanValue()) {
                atom.chainID = '0'; // no author provided ID
              } else if (strand.authorID != null) {
                // not pretty, but let's just assume the string only has one char
                atom.chainID = strand.authorID.charAt(0);
              } else {
                atom.chainID = '0';
              }
            } else {
              atom.chainID = firstChar;
            }
          }
          break;
        case SEQ_ID:
          atom.sequenceNumber = parseInt(field);
          break;
        case INS_CODE:
          char insCode = field.charAt(0);
          if (insCode != '?' && insCode != '.')
            atom.chainID = insCode;
          break;
        case ALT_ID:
          char alternateLocationID = field.charAt(0);
          if (alternateLocationID != '?' && alternateLocationID != '.')
            atom.alternateLocationID = alternateLocationID;
          break;
        case GROUP_PDB:
          if ("HETATM".equals(field))
            atom.isHetero = true;
          break;
        case MODEL_NO:
          int modelNO = parseInt(field);
          if (modelNO != currentModelNO) {
            atomSetCollection.newAtomSet();
            currentModelNO = modelNO;
          }
          break;
        }
      }
      if (Float.isNaN(atom.x) || Float.isNaN(atom.y) || Float.isNaN(atom.z))
        logger.log("atom " + atom.atomName +
                   " has invalid/unknown coordinates");
      else
        atomSetCollection.addAtomWithMappedName(atom);
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
    //    logger.log("parseLoopParameters sees fieldCount="+ fieldCount);
    return fieldCount;
  }

  Object[] parseAndProcessStructParameters(
        String[] fields,
        byte[] fieldMap,
        int[] fieldTypes,
        boolean[] propertyReferenced) throws Exception {
    int fieldCount = 0;
    Strand struct = new Strand();
    for (; line != null &&
           (line = line.trim()).length() > 0 &&
           line.charAt(0) == '_';
         ++fieldCount, line = reader.readLine()) {
      logger.log("Processing line: ", line);
      for (int i = fields.length; --i >= 0; ) {
        tokenizer.setString(line);
        String key = tokenizer.nextToken();
        // take this valid mmCIF into account:
        // _struct_asym.id A 
        String value = null;
        if (isMatch(key, fields[i])) {
          if (tokenizer.hasMoreTokens()) {
            value = tokenizer.nextToken();
            if (fields[i].equals(structAsymNames[STRUCT_ASYM_ID-1])) 
              struct.chainID = value;
            if (fields[i].equals(structAsymNames[STRUCT_BLANK_CHAIN_ID-1]))
              struct.isBlank = "Y".equals(value) ? Boolean.TRUE : Boolean.FALSE; 
            logger.log("struct", struct);
          } else {
            struct = null;
          }
          int iproperty = fieldMap[i];
          propertyReferenced[iproperty] = true;
          fieldTypes[fieldCount] = iproperty;
          i = -1;
        }
      }
    }
    logger.log("parseLoopParameters sees fieldCount=" + fieldCount);
    logger.log("parsed struct -> " + struct);
    Object[] results = new Object[2];
    results[0] = new Integer(fieldCount);
    results[1] = struct;
    return results;
  }

  ////////////////////////////////////////////////////////////////
  // bond data
  ////////////////////////////////////////////////////////////////

  final static byte GEOM_BOND_ATOM_SITE_LABEL_1 = 1;
  final static byte GEOM_BOND_ATOM_SITE_LABEL_2 = 2;
  final static byte GEOM_BOND_SITE_SYMMETRY_2   = 3;
  //  final static byte GEOM_BOND_DISTANCE          = 4;
  
  final static byte GEOM_BOND_PROPERTY_MAX      = 4;

  final static String[] geomBondFields = {
    "_geom_bond_atom_site_label_1",
    "_geom_bond_atom_site_label_2",
    "_geom_bond_site_symmetry_2",
    //    "_geom_bond_distance",
  };

  final static byte[] geomBondFieldMap = {
    GEOM_BOND_ATOM_SITE_LABEL_1,
    GEOM_BOND_ATOM_SITE_LABEL_2,
    GEOM_BOND_SITE_SYMMETRY_2,
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
        skipLoopData();
        return;
      }

    for (; line != null &&
           (line = line.trim()).length() > 0 &&
           line.charAt(0) != '#' &&
           line.charAt(0) != '_' &&
           !line.startsWith("loop_") &&
           !line.startsWith("data_");
         line = reader.readLine()) {
      tokenizer.setString(line);
      int atomIndex1 = -1;
      int atomIndex2 = -1;
      String symmetry = null;
      for (int i = 0; i < fieldCount; ++i) {
        if (! tokenizer.hasMoreTokens())
          tokenizer.setString(reader.readLine());
        String field = tokenizer.nextToken();
        switch (fieldTypes[i]) {
        case NONE:
          break;
        case GEOM_BOND_ATOM_SITE_LABEL_1:
          atomIndex1 = atomSetCollection.getAtomNameIndex(field);
          break;
        case GEOM_BOND_ATOM_SITE_LABEL_2:
          atomIndex2 = atomSetCollection.getAtomNameIndex(field);
          break;
        case GEOM_BOND_SITE_SYMMETRY_2:
          if (field.charAt(0) != '.')
            symmetry = field;
        }
      }
      if (atomIndex1 >= 0 && atomIndex2 >= 0) {
        // miguel 2004 11 19
        // for now, do not deal with symmetry
        if (symmetry == null) {
          Bond bond = new Bond();
          bond.atomIndex1 = atomIndex1;
          bond.atomIndex2 = atomIndex2;
          atomSetCollection.addBond(bond);
        }
      }
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

    "_struct_conf.beg_auth_asym_id",
    "_struct_conf.beg_auth_seq_id",
    "_struct_conf.pdbx_beg_PDB_ins_code",
    
    "_struct_conf.end_auth_asym_id",
    "_struct_conf.end_auth_seq_id",
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
        skipLoopData();
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
          structure.startChainID =
            (firstChar == '.' || firstChar == '?') ? ' ' : firstChar;
          break;
        case BEG_SEQ_ID:
          structure.startSequenceNumber = parseInt(field);
          break;
        case BEG_INS_CODE:
          structure.startInsertionCode =
            (firstChar == '.' || firstChar == '?') ? ' ' : firstChar;
          break;
        case END_ASYM_ID:
          structure.endChainID =
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
      atomSetCollection.addStructure(structure);
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
        skipLoopData();
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
          structure.startChainID =
            (firstChar == '.' || firstChar == '?') ? ' ' : firstChar;
          break;
        case BEG_SEQ_ID:
          structure.startSequenceNumber = parseInt(field);
          break;
        case BEG_INS_CODE:
          structure.startInsertionCode =
            (firstChar == '.' || firstChar == '?') ? ' ' : firstChar;
          break;
        case END_ASYM_ID:
          structure.endChainID =
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
      atomSetCollection.addStructure(structure);
    }
  }

  ////////////////////////////////////////////////////////////////
  // strand data
  ////////////////////////////////////////////////////////////////

  final static byte STRUCT_ASYM_ID = 1;
  final static byte STRUCT_BLANK_CHAIN_ID = 2;
  final static byte STRUCT_PROPERTY_MAX = 3;

  final static String[] structAsymNames =
  {"_struct_asym.id", "_struct_asym.pdbx_blank_PDB_chainid_flag"};

  final static byte[] structAsymMap = {
    STRUCT_ASYM_ID, STRUCT_BLANK_CHAIN_ID
  };

  static {
    if (structAsymMap.length != structAsymNames.length)
      structAsymNames[100] = "explode";
  }
  
  private void addOrUpdateStrandInfo(Strand strand) {
    logger.log("Found only one chain", strand.chainID);
    if (strandsMap.containsKey(strand.chainID)) {
      Strand prevStrand = (Strand)strandsMap.get(strand.chainID);
      if (strand.authorID != null) prevStrand.authorID = strand.authorID;
      if (prevStrand.isBlank != null) prevStrand.isBlank = strand.isBlank;
    } else {
      strandsMap.put(strand.chainID, strand);
    }
  }

  private void processStructAsymBlock() throws Exception {
    //    logger.log("processAtomSiteLoopBlock()-------------------------");
    int[] fieldTypes = new int[10]; // should be enough
    boolean[] structPropertyReferenced = new boolean[STRUCT_PROPERTY_MAX];
    Object[] results = parseAndProcessStructParameters(
        structAsymNames, structAsymMap,
        fieldTypes, structPropertyReferenced);
    int fieldCount = ((Integer)results[0]).intValue(); 
    Strand struct = ((Strand)results[1]);
    // now that headers are parsed, check to see if we want
    // cartesian or fractional coordinates;
    if (struct != null) {
      /* Then this syntax was found:
      * _struct_asym.id                            A 
      * _struct_asym.pdbx_blank_PDB_chainid_flag   Y 
      * _struct_asym.pdbx_modified                 N 
      * _struct_asym.entity_id                     1 
      * _struct_asym.details                       ? 
      * #
      */
      addOrUpdateStrandInfo(struct);
    } else {
      /* Then the 'normal' _loop format was found. */
      logger.log("Found multiple chains... parsing them now");
      for (; line != null; line = reader.readLine()) {
        int lineLength = line.length();
        if (lineLength == 0)
          break;
        char chFirst = line.charAt(0);
        if (chFirst == '#' ||
            chFirst == '_' ||
            line.startsWith("loop_") ||
            line.startsWith("data_"))
          break;
        if (chFirst == ' ') {
          int i;
          for (i = lineLength; --i >= 0 && line.charAt(i) == ' '; ) {
          }
          if (i < 0)
            break;
        }
        //      logger.log("line:" + line);
        //      logger.log("of length = " + line.length());
        if (line.length() == 1)
          System.out.println("char value is " + (chFirst + 0));
        tokenizer.setString(line);
        //      logger.log("reading an atom");
        struct = new Strand();
        for (int i = 0; i < fieldCount; ++i) {
          if (! tokenizer.hasMoreTokens())
            tokenizer.setString(reader.readLine());
          String field = tokenizer.nextToken();
          if (field == null)
            System.out.println("field == null!");
          switch (fieldTypes[i]) {
          case STRUCT_ASYM_ID:
            struct.chainID = field;
            break;
          case STRUCT_BLANK_CHAIN_ID:
            struct.isBlank = "Y".equals(field) ? Boolean.TRUE : Boolean.FALSE; 
            break;
          }
        }
        addOrUpdateStrandInfo(struct);
      }
    }
  }  

  ////////////////////////////////////////////////////////////////
  // poly sequence scheme data
  ////////////////////////////////////////////////////////////////
  
  final static byte POLYSEQ_ASYM_ID = 1;
  final static byte POLYSEQ_PDB_STRAND_ID = 2;
  final static byte POLYSEQ_PROPERTY_MAX = 3;

  final static String[] polySeqFields =
  {"_pdbx_poly_seq_scheme.asym_id", 
   "_pdbx_poly_seq_scheme.pdb_strand_id"};

  final static byte[] polySeqMap = {
    POLYSEQ_ASYM_ID, POLYSEQ_PDB_STRAND_ID
  };

  static {
    if (polySeqMap.length != polySeqFields.length)
      polySeqFields[100] = "explode";
  }

  private void processPolySeqSchemeLoopBlock() throws Exception {
    int[] fieldTypes = new int[100]; // should be enough
    boolean[] propertyReferenced =
      new boolean[POLYSEQ_PROPERTY_MAX];
    int fieldCount = parseLoopParameters(polySeqFields,
        polySeqMap, fieldTypes, propertyReferenced);
    for (int i = POLYSEQ_PROPERTY_MAX; --i > 1; )
      if (! propertyReferenced[i]) {
        logger.log("?que? missing _pdbx_poly_seq_scheme property:" + i);
        skipLoopData();
        return;
      }

    String lastChainID = "XXXXX";
    for (; line != null &&
           (line = line.trim()).length() > 0 &&
           line.charAt(0) != '#';
         line = reader.readLine()) {
      tokenizer.setString(line);
      Strand strand = new Strand();
      for (int i = 0; i < fieldCount; ++i) {
        if (! tokenizer.hasMoreTokens())
          tokenizer.setString(reader.readLine());
        String field = tokenizer.nextToken();
        switch (fieldTypes[i]) {
        case POLYSEQ_ASYM_ID:
          strand.chainID = field;
          break;
        case POLYSEQ_PDB_STRAND_ID:
          if (field.charAt(0) == '?') {
            strand.authorID = null; 
          } else {
            strand.authorID = field;
          }
          break;
        }
      }
      // OK, we're only interested in the chain IDs
      // hence we only parse the first AA in a chain
      if (lastChainID.equals(strand.chainID)) {
        // skip
      } else {
        addOrUpdateStrandInfo(strand);
        lastChainID = strand.chainID;
      }
    }
    logger.log("Done reading _loop Poly Seq Scheme");
    Iterator strands = strandsMap.values().iterator();
    while (strands.hasNext()) {
      Strand strand = (Strand)strands.next();
      logger.log(strand.toString());
    }
  }

  ////////////////////////////////////////////////////////////////
  // non poly scheme data
  ////////////////////////////////////////////////////////////////
  
  final static byte NONPOLY_ASYM_ID = 1;
  final static byte NONPOLY_PDB_STRAND_ID = 2;
  final static byte NONPOLY_PROPERTY_MAX = 3;

  final static String[] nonPolyFields =
  {"_pdbx_nonpoly_scheme.asym_id", 
   "_pdbx_nonpoly_scheme.pdb_strand_id"};

  final static byte[] nonPolyMap = {
    POLYSEQ_ASYM_ID, POLYSEQ_PDB_STRAND_ID
  };

  static {
    if (nonPolyMap.length != nonPolyFields.length)
      nonPolyFields[100] = "explode";
  }

  private void processNonPolySchemeLoopBlock() throws Exception {
    int[] fieldTypes = new int[100]; // should be enough
    boolean[] propertyReferenced =
      new boolean[NONPOLY_PROPERTY_MAX];
    int fieldCount = parseLoopParameters(nonPolyFields,
        polySeqMap, fieldTypes, propertyReferenced);
    for (int i = NONPOLY_PROPERTY_MAX; --i > 1; )
      if (! propertyReferenced[i]) {
        logger.log("?que? missing _pdbx_nonpoly_scheme property:" + i);
        skipLoopData();
        return;
      }

    String lastChainID = "XXXXX";
    for (; line != null &&
           (line = line.trim()).length() > 0 &&
           line.charAt(0) != '#';
         line = reader.readLine()) {
      tokenizer.setString(line);
      Strand strand = new Strand();
      for (int i = 0; i < fieldCount; ++i) {
        if (! tokenizer.hasMoreTokens())
          tokenizer.setString(reader.readLine());
        String field = tokenizer.nextToken();
        switch (fieldTypes[i]) {
        case NONPOLY_ASYM_ID:
          strand.chainID = field;
          break;
        case NONPOLY_PDB_STRAND_ID:
          if (field.charAt(0) == '?') {
            strand.authorID = null; 
          } else {
            strand.authorID = field;
          }
          break;
        }
      }
      // OK, we're only interested in the chain IDs
      // hence we only parse the first AA in a chain
      if (lastChainID.equals(strand.chainID)) {
        // skip
      } else {
        addOrUpdateStrandInfo(strand);
        lastChainID = strand.chainID;
      }
    }
    logger.log("Done reading _loop NonPoly Scheme");
    Iterator strands = strandsMap.values().iterator();
    while (strands.hasNext()) {
      Strand strand = (Strand)strands.next();
      logger.log(strand.toString());
    }
  }
  
  
  ////////////////////////////////////////////////////////////////
  // special tokenizer class
  ////////////////////////////////////////////////////////////////

  /**
   * A special tokenizer class for dealing with quoted strings in CIF files.
   *<p>
   * regarding the treatment of single quotes vs. primes in
   * cif file, PMR wrote:
   *</p>
   *<p>
   *   * There is a formal grammar for CIF
   * (see http://www.iucr.org/iucr-top/cif/index.html)
   * which confirms this. The textual explanation is
   *<p />
   *<p>
   * 14. Matching single or double quote characters (' or ") may
   * be used to bound a string representing a non-simple data value
   * provided the string does not extend over more than one line.
   *<p />
   *<p>
   * 15. Because data values are invariably separated from other
   * tokens in the file by white space, such a quote-delimited
   * character string may contain instances of the character used
   * to delimit the string provided they are not followed by white
   * space. For example, the data item
   *<code>
   *  _example  'a dog's life'
   *</code>
   * is legal; the data value is a dog's life.
   *</p>
   *<p>
   * [PMR - the terminating character(s) are quote+whitespace.
   * That would mean that:
   *<code>
   *  _example 'Jones' life'
   *</code>
   * would be an error
   *</p>
   *<p>
   * The CIF format was developed in that late 1980's under the aegis of the
   * International Union of Crystallography (I am a consultant to the COMCIFs 
   * committee). It was ratified by the Union and there have been several 
   * workshops. mmCIF is an extension of CIF which includes a relational 
   * structure. The formal publications are:
   *</p>
   *<p>
   * Hall, S. R. (1991). "The STAR File: A New Format for Electronic Data 
   * Transfer and Archiving", J. Chem. Inform. Comp. Sci., 31, 326-333.
   * Hall, S. R., Allen, F. H. and Brown, I. D. (1991). "The Crystallographic
   * Information File (CIF): A New Standard Archive File for Crystallography",
   * Acta Cryst., A47, 655-685.
   * Hall, S.R. & Spadaccini, N. (1994). "The STAR File: Detailed 
   * Specifications," J. Chem. Info. Comp. Sci., 34, 505-508.
   *</p>
   */
    
  static class RidiculousFileFormatTokenizer {
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
      char ch;
      while (ich < cch && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
        ++ich;
      return ich < cch;
    }

    /* assume that hasMoreTokens() has been called and that
     * ich is pointing at a non-white character
     */
    String nextToken() {
      if (ich == cch)
        return null;
      int ichStart = ich;
      char ch = str.charAt(ichStart);
      if (ch != '\'' && ch != '"') {
        while (ich < cch && (ch = str.charAt(ich)) != ' ' && ch != '\t')
          ++ich;
        return str.substring(ichStart, ich);
      }
      char chOpeningQuote = ch;
      boolean previousCharacterWasQuote = false;
      while (++ich < cch) {
        ch = str.charAt(ich);
        if (previousCharacterWasQuote && (ch == ' ' || ch == '\t'))
          break;
        previousCharacterWasQuote = (ch == chOpeningQuote);
      }
      if (ich == cch) {
        if (previousCharacterWasQuote) // close quote was last char of string
          return str.substring(ichStart + 1, ich - 1);
        // reached the end of the string without finding closing '
        return str.substring(ichStart, ich);
      }
      ++ich; // throw away the last white character
      return str.substring(ichStart + 1, ich - 2);
    }
  }
}



