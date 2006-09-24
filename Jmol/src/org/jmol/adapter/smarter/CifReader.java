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

/**
 * A true line-free CIF file reader for CIF and mmCIF files.
 *
 *<p>
 * <a href='http://www.iucr.org/iucr-top/cif/'>
 * http://www.iucr.org/iucr-top/cif/
 * </a>
 * 
 * <a href='http://www.iucr.org/iucr-top/cif/standard/cifstd5.html'>
 * http://www.iucr.org/iucr-top/cif/standard/cifstd5.html
 * </a>
 *
 * @author Miguel, Egon, and Bob (hansonr@stolaf.edu)
 * 
 * symmetry added by Bob Hanson:
 * 
 *  setSpaceGroupName()
 *  setSymmetryOperator()
 *  setUnitCellItem()
 *  setFractionalCoordinates()
 *  setAtomCoord()
 *  applySymmetry()
 *  
 */
class CifReader extends AtomSetCollectionReader {

  RidiculousFileFormatTokenizer tokenizer = new RidiculousFileFormatTokenizer();

  String thisDataSetName = "";
  String chemicalName = "";
  String thisStructuralFormula = "";
  String thisFormula = "";
  
  Hashtable htHetero;

  AtomSetCollection readAtomSetCollection(BufferedReader reader)
      throws Exception {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("cif");

    /*
     * Modified for 10.9.64 9/23/06 by Bob Hanson to remove as much as possible of line dependence.
     * a loop could now go:
     * 
     * blah blah blah  loop_ _a _b _c 0 1 2 0 3 4 0 5 6 loop_...... 
     * 
     */
    line = "";
    boolean skipping = false;
    while ((key = tokenizer.peekToken()) != null) {
      if (key.indexOf("#jmolscript:") >= 0)
        checkLineForScript(line);
      if (key.startsWith("data_")) {
        if (iHaveDesiredModel)
          break;
        skipping = (++modelNumber != desiredModelNumber && desiredModelNumber > 0);
        if (skipping) {
          tokenizer.getTokenPeeked();
        } else {
          chemicalName = "";
          thisStructuralFormula = "";
          thisFormula = "";
          applySymmetry();
          processDataParameter();
          iHaveDesiredModel = (desiredModelNumber > 0);
        }
        continue;
      }
      if (key.startsWith("loop_")) {
        if (skipping) {
          tokenizer.getTokenPeeked();
        } else {
          processLoopBlock();
        }
        continue;
      }
      if (key.indexOf("_") != 0) {
        logger.log("CIF ERROR ? should be an underscore: " + key);
        tokenizer.getTokenPeeked();
      } else if (!getData()) {
        continue;
      }
      if (!skipping) {
        if (key.startsWith("_chemical_name")) {
          processChemicalInfo("name");
        } else if (key.startsWith("_chemical_formula_structural")) {
          processChemicalInfo("structuralFormula");
        } else if (key.startsWith("_chemical_formula_sum")) {
          processChemicalInfo("formula");
        } else if (key.startsWith("_cell_") || key.startsWith("_cell.")) {
          processCellParameter();
        } else if (key.startsWith("_symmetry_space_group_name_H-M")
            || key.startsWith("_symmetry.space_group_name_H-M")
            || key.startsWith("_symmetry_space_group_name_Hall")
            || key.startsWith("_symmetry.space_group_name_Hall")) {
          processSymmetrySpaceGroupName();
        } else if (key.startsWith("_atom_sites.fract_tran")
            || key.startsWith("_atom_sites.fract_tran")) {
          processUnitCellTransformMatrix();
        }
      }
    }
    applySymmetry();
    return atomSetCollection;
  }

  ////////////////////////////////////////////////////////////////
  // processing methods
  ////////////////////////////////////////////////////////////////

  void processDataParameter() {
    tokenizer.getTokenPeeked();
    thisDataSetName = (key.length() < 6 ? "" : key.substring(5));
    if (thisDataSetName.length() > 0) {
      if (atomSetCollection.currentAtomSetIndex >= 0) {
        // note that there can be problems with multi-data mmCIF sets each with
        // multiple models; and we could be loading multiple files!
        atomSetCollection.newAtomSet();
        atomSetCollection.setCollectionName("<collection of "
            + (atomSetCollection.currentAtomSetIndex + 1) + " models>");
      } else {
        atomSetCollection.setCollectionName(thisDataSetName);
      }
    }
    logger.log(key);
  }

  void processChemicalInfo(String type) throws Exception {
    // someone should generalize this with an array of desired name info
    if (type.equals("name"))
      chemicalName = data;
    else if (type.equals("structuralFormula"))
      thisStructuralFormula = data;
    else if (type.equals("formula"))
      thisFormula = data;
    logger.log(type + " = " + data);
  }

  void processSymmetrySpaceGroupName() throws Exception {
    setSpaceGroupName(data);
  }

  final static String[] cellParamNames = { "_cell_length_a", "_cell_length_b",
      "_cell_length_c", "_cell_angle_alpha", "_cell_angle_beta",
      "_cell_angle_gamma", "_cell.length_a", "_cell.length_b",
      "_cell.length_c", "_cell.angle_alpha", "_cell.angle_beta",
      "_cell.angle_gamma" };

  void processCellParameter() throws Exception {
    for (int i = cellParamNames.length; --i >= 0;)
      if (isMatch(key, cellParamNames[i])) {
        setUnitCellItem(i % 6, parseFloat(data));
        return;
      }
  }

  final static String[] TransformFields = {
      "x[1][1]", "x[1][2]", "x[1][3]", "r[1]",
      "x[2][1]", "x[2][2]", "x[2][3]", "r[2]",
      "x[3][1]", "x[3][2]", "x[3][3]", "r[3]",
  };

  void processUnitCellTransformMatrix() throws Exception {
    /*
     * PDB:
     
     SCALE1       .024414  0.000000  -.000328        0.00000
     SCALE2      0.000000   .053619  0.000000        0.00000
     SCALE3      0.000000  0.000000   .044409        0.00000

     * CIF:

     _atom_sites.fract_transf_matrix[1][1]   .024414 
     _atom_sites.fract_transf_matrix[1][2]   0.000000 
     _atom_sites.fract_transf_matrix[1][3]   -.000328 
     _atom_sites.fract_transf_matrix[2][1]   0.000000 
     _atom_sites.fract_transf_matrix[2][2]   .053619 
     _atom_sites.fract_transf_matrix[2][3]   0.000000 
     _atom_sites.fract_transf_matrix[3][1]   0.000000 
     _atom_sites.fract_transf_matrix[3][2]   0.000000 
     _atom_sites.fract_transf_matrix[3][3]   .044409 
     _atom_sites.fract_transf_vector[1]      0.00000 
     _atom_sites.fract_transf_vector[2]      0.00000 
     _atom_sites.fract_transf_vector[3]      0.00000 

     */
    float v = parseFloat(data);
    if (Float.isNaN(v))
      return;
    for (int i = 0; i < TransformFields.length; i++) {
      if (key.indexOf(TransformFields[i]) >= 0) {
        setUnitCellItem(6 + i, v);
        return;
      }
    }
  }
  
  ////////////////////////////////////////////////////////////////
  // loop_ processing
  ////////////////////////////////////////////////////////////////

  String key;
  String data;
  boolean getData() throws Exception {
    key = tokenizer.getTokenPeeked();
    data = tokenizer.getNextToken();
    return (data.length() == 0 || data.charAt(0) != '\0');
  }
  
  private void processLoopBlock() throws Exception {
    tokenizer.getTokenPeeked(); //loop_
    String str = tokenizer.peekToken();
    if (str == null)
      return;
    if (str.startsWith("_atom_site_") || str.startsWith("_atom_site.")) {
      if (!processAtomSiteLoopBlock())
        return;
      atomSetCollection.setAtomSetName(thisDataSetName);
      atomSetCollection.setAtomSetAuxiliaryInfo("chemicalName", chemicalName);
      atomSetCollection.setAtomSetAuxiliaryInfo("structuralFormula",
          thisStructuralFormula);
      atomSetCollection.setAtomSetAuxiliaryInfo("formula", thisFormula);
      return;
    }
    if (str.startsWith("_geom_bond")) {
      if (doApplySymmetry) //not reading bonds when symmetry is enabled yet
        skipLoop();
      else
        processGeomBondLoopBlock();
      return;
    }
    if (str.startsWith("_pdbx_entity_nonpoly")) {
      processNonpolyLoopBlock();
      return;
    }
    if (str.startsWith("_struct_conf")
        && !str.startsWith("_struct_conf_type")) {
      processStructConfLoopBlock();
      return;
    }
    if (str.startsWith("_struct_sheet_range")) {
      processStructSheetRangeLoopBlock();
      return;
    }
    if (str.startsWith("_struct_sheet_range")) {
      processStructSheetRangeLoopBlock();
      return;
    }
    if (str.startsWith("_symmetry_equiv_pos")
        || str.startsWith("space_group_symop")) {
      if (ignoreFileSymmetryOperators) {
        logger.log("ignoring file-based symmetry operators");
        skipLoop();
      } else {
        processSymmetryOperationsLoopBlock();
      }
      return;
    }    
    skipLoop();
  }

  ////////////////////////////////////////////////////////////////
  // atom data
  ////////////////////////////////////////////////////////////////

  final static byte NONE = -1;
  final static byte TYPE_SYMBOL = 0;
  final static byte LABEL = 1;
  final static byte AUTH_ATOM = 2;
  final static byte FRACT_X = 3;
  final static byte FRACT_Y = 4;
  final static byte FRACT_Z = 5;
  final static byte CARTN_X = 6;
  final static byte CARTN_Y = 7;
  final static byte CARTN_Z = 8;
  final static byte OCCUPANCY = 9;
  final static byte B_ISO = 10;
  final static byte COMP_ID = 11;
  final static byte ASYM_ID = 12;
  final static byte SEQ_ID = 13;
  final static byte INS_CODE = 14;
  final static byte ALT_ID = 15;
  final static byte GROUP_PDB = 16;
  final static byte MODEL_NO = 17;
  final static byte DUMMY_ATOM = 18;

  final static String[] atomFields = { 
      "_atom_site_type_symbol",
      "_atom_site_label", 
      "_atom_site_auth_atom_id", 
      "_atom_site_fract_x",
      "_atom_site_fract_y", 
      "_atom_site_fract_z", 
      "_atom_site.Cartn_x",
      "_atom_site.Cartn_y", 
      "_atom_site.Cartn_z", 
      "_atom_site_occupancy",
      "_atom_site.b_iso_or_equiv", 
      "_atom_site.auth_comp_id",
      "_atom_site.auth_asym_id", 
      "_atom_site.auth_seq_id",
      "_atom_site.pdbx_PDB_ins_code", 
      "_atom_site.label_alt_id",
      "_atom_site.group_PDB", 
      "_atom_site.pdbx_PDB_model_num",
      "_atom_site_calc_flag", 
  };

  /* to: hansonr@stolaf.edu
   * from: Zukang Feng zfeng@rcsb.rutgers.edu
   * re: Two mmCIF issues
   * date: 4/18/2006 10:30 PM
   * "You should always use _atom_site.auth_asym_id for PDB chain IDs."
   * 
   * 
   */

  boolean processAtomSiteLoopBlock() throws Exception {
    int currentModelNO = -1;
    boolean isPDB = false;
    parseLoopParameters(atomFields);
    if (propertyReferenced[CARTN_X]) {
      setFractionalCoordinates(false);
      for (int i = FRACT_X; i < FRACT_Z; ++i)
        disableField(fieldCount, fieldTypes, i);
    } else if (propertyReferenced[FRACT_X]) {
      setFractionalCoordinates(true);
      for (int i = CARTN_X; i < CARTN_Z; ++i)
        disableField(fieldCount, fieldTypes, i);
    } else {
      // it is a different kind of _atom_site loop block
      skipLoop();
      return false;
    }
    while (tokenizer.getData(loopData, fieldCount)) {
      Atom atom = new Atom();
      for (int i = 0; i < fieldCount; ++i) {
        String field = loopData[i];
        char firstChar = field.charAt(0);
        if (firstChar == '\0')
          continue;
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
        case AUTH_ATOM:
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
          if (!Float.isNaN(floatOccupancy))
            atom.occupancy = (int) (floatOccupancy * 100);
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
          atom.chainID = firstChar;
          break;
        case SEQ_ID:
          atom.sequenceNumber = parseInt(field);
          break;
        case INS_CODE:
          atom.chainID = firstChar;
          break;
        case ALT_ID:
          atom.alternateLocationID = field.charAt(0);
          break;
        case GROUP_PDB:
          isPDB = true;
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
        case DUMMY_ATOM:
          //see http://www.iucr.org/iucr-top/cif/cifdic_html/
          //            1/cif_core.dic/Iatom_site_calc_flag.html
          if ("dum".equals(field)) {
            atom.x = Float.NaN;
            continue; //skip 
          }
          break;
        }
      }
      if (Float.isNaN(atom.x) || Float.isNaN(atom.y) || Float.isNaN(atom.z)) {
        logger
            .log("atom " + atom.atomName + " has invalid/unknown coordinates");
      } else {
        setAtomCoord(atom);
        atomSetCollection.addAtomWithMappedName(atom);
        if (atom.isHetero && htHetero != null) {
          atomSetCollection.setAtomSetAuxiliaryInfo("hetNames", htHetero);
          htHetero = null;
        }
      }
    }
    if (isPDB)
      atomSetCollection.setAtomSetAuxiliaryInfo("isPDB", new Boolean(isPDB));
    return true;
  }

  ////////////////////////////////////////////////////////////////
  // bond data
  ////////////////////////////////////////////////////////////////

  final static byte GEOM_BOND_ATOM_SITE_LABEL_1 = 0;
  final static byte GEOM_BOND_ATOM_SITE_LABEL_2 = 1;
  final static byte GEOM_BOND_SITE_SYMMETRY_2 = 2;

  final static String[] geomBondFields = { 
      "_geom_bond_atom_site_label_1",
      "_geom_bond_atom_site_label_2", 
      "_geom_bond_site_symmetry_2",
  };

  void processGeomBondLoopBlock() throws Exception {
    parseLoopParameters(geomBondFields);
    for (int i = propertyCount; --i >= 0;)
      if (!propertyReferenced[i]) {
        logger.log("?que? missing _geom_bond property:" + i);
        skipLoop();
        return;
      }

    while (tokenizer.getData(loopData, fieldCount)) {
      int atomIndex1 = -1;
      int atomIndex2 = -1;
      String symmetry = null;
      for (int i = 0; i < fieldCount; ++i) {
        String field = loopData[i];
        char firstChar = field.charAt(0);
        if (firstChar == '\0')
          continue;
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
          symmetry = field;
          break;
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

  final static byte NONPOLY_ENTITY_ID = 0;
  final static byte NONPOLY_NAME = 1;
  final static byte NONPOLY_COMP_ID = 2;

  final static String[] nonpolyFields = { 
      "_pdbx_entity_nonpoly.entity_id",
      "_pdbx_entity_nonpoly.name", 
      "_pdbx_entity_nonpoly.comp_id", 
  };

  void processNonpolyLoopBlock() throws Exception {
    parseLoopParameters(nonpolyFields);
    while (tokenizer.getData(loopData, fieldCount)) {
      String groupName = null;
      String hetName = null;
      for (int i = 0; i < fieldCount; ++i) {
        String field = loopData[i];
        char firstChar = field.charAt(0);
        if (firstChar == '\0')
          continue;
        switch (fieldTypes[i]) {
        case NONE:
        case NONPOLY_ENTITY_ID:
          break;
        case NONPOLY_COMP_ID:
          groupName = field;
          break;
        case NONPOLY_NAME:
          hetName = field;
          break;
        }
      }
      if (groupName == null || hetName == null)
        return;
      if (htHetero == null)
        htHetero = new Hashtable();
      htHetero.put(groupName, hetName);
      logger.log("hetero: "+groupName+" "+hetName);
    }
  }

  ////////////////////////////////////////////////////////////////
  // helix and turn structure data
  ////////////////////////////////////////////////////////////////

  final static byte CONF_TYPE_ID = 0;
  final static byte BEG_ASYM_ID = 1;
  final static byte BEG_SEQ_ID = 2;
  final static byte BEG_INS_CODE = 3;
  final static byte END_ASYM_ID = 4;
  final static byte END_SEQ_ID = 5;
  final static byte END_INS_CODE = 6;

  final static String[] structConfFields = { 
      "_struct_conf.conf_type_id",
      "_struct_conf.beg_auth_asym_id", 
      "_struct_conf.beg_auth_seq_id",
      "_struct_conf.pdbx_beg_PDB_ins_code",
      "_struct_conf.end_auth_asym_id", 
      "_struct_conf.end_auth_seq_id",
      "_struct_conf.pdbx_end_PDB_ins_code", 
  };

  void processStructConfLoopBlock() throws Exception {
    parseLoopParameters(structConfFields);
    for (int i = propertyCount; --i >= 0;)
      if (!propertyReferenced[i]) {
        logger.log("?que? missing _struct_conf property:" + i);
        skipLoop();
        return;
      }
    while (tokenizer.getData(loopData, fieldCount)) {
      Structure structure = new Structure();
      for (int i = 0; i < fieldCount; ++i) {
        String field = loopData[i];
        char firstChar = field.charAt(0);
        if (firstChar == '\0')
          continue;
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
          structure.startChainID = firstChar;
          break;
        case BEG_SEQ_ID:
          structure.startSequenceNumber = parseInt(field);
          break;
        case BEG_INS_CODE:
          structure.startInsertionCode = firstChar;
          break;
        case END_ASYM_ID:
          structure.endChainID = firstChar;
          break;
        case END_SEQ_ID:
          structure.endSequenceNumber = parseInt(field);
          break;
        case END_INS_CODE:
          structure.endInsertionCode = firstChar;
          break;
        }
      }
      atomSetCollection.addStructure(structure);
    }
  }

  ////////////////////////////////////////////////////////////////
  // sheet structure data
  ////////////////////////////////////////////////////////////////

  final static String[] structSheetRangeFields = {
      "_struct_sheet_range.sheet_id",  //unused placeholder
      "_struct_sheet_range.beg_auth_asym_id",
      "_struct_sheet_range.beg_auth_seq_id",
      "_struct_sheet_range.pdbx_beg_PDB_ins_code",
      "_struct_sheet_range.end_auth_asym_id",
      "_struct_sheet_range.end_auth_seq_id",
      "_struct_sheet_range.pdbx_end_PDB_ins_code", 
  };

  void processStructSheetRangeLoopBlock() throws Exception {
    parseLoopParameters(structSheetRangeFields);
    for (int i = propertyCount; --i >= 0;)
      if (!propertyReferenced[i]) {
        logger.log("?que? missing _struct_conf property:" + i);
        skipLoop();
        return;
      }
    while (tokenizer.getData(loopData, fieldCount)) {
      Structure structure = new Structure();
      structure.structureType = "sheet";
      for (int i = 0; i < fieldCount; ++i) {
        String field = loopData[i];
        char firstChar = field.charAt(0);
        if (firstChar == '\0')
          continue;
        switch (fieldTypes[i]) {
        case BEG_ASYM_ID:
          structure.startChainID = firstChar;
          break;
        case BEG_SEQ_ID:
          structure.startSequenceNumber = parseInt(field);
          break;
        case BEG_INS_CODE:
          structure.startInsertionCode = firstChar;
          break;
        case END_ASYM_ID:
          structure.endChainID = firstChar;
          break;
        case END_SEQ_ID:
          structure.endSequenceNumber = parseInt(field);
          break;
        case END_INS_CODE:
          structure.endInsertionCode = firstChar;
          break;
        }
      }
      atomSetCollection.addStructure(structure);
    }
  }

  ////////////////////////////////////////////////////////////////
  // symmetry operations
  ////////////////////////////////////////////////////////////////

  final static byte SYMOP_XYZ = 0;
  final static byte SYM_EQUIV_XYZ = 1;

  final static String[] symmetryOperationsFields = {
      "_space_group_symop_operation_xyz", 
      "_symmetry_equiv_pos_as_xyz", 
  };

  void processSymmetryOperationsLoopBlock() throws Exception {
    parseLoopParameters(symmetryOperationsFields);
    int nRefs = 0;
    for (int i = propertyCount; --i >= 0;)
      if (propertyReferenced[i])
        nRefs++;
    if (nRefs != 1) {
      logger
          .log("?que? _symmetry_equiv or _space_group_symop property not found");
      skipLoop();
      return;
    }
    while (tokenizer.getData(loopData, fieldCount)) {
      for (int i = 0; i < fieldCount; ++i) {
        String field = loopData[i];
        char firstChar = field.charAt(0);
        if (firstChar == '\0')
          continue;
        switch (fieldTypes[i]) {
        case SYMOP_XYZ:
        case SYM_EQUIV_XYZ:
          setSymmetryOperator(field);
          break;
        }
      }
    }
  }
  
  /////////////////////////////////////////////////////0///////////
  // token-based CIF loop-data reader
  ////////////////////////////////////////////////////////////////

  private String[] loopData;
  private int[] fieldTypes = new int[100]; // should be enough
  private boolean[] propertyReferenced = new boolean[50];
  private int propertyCount;
  private int fieldCount;
  
  void parseLoopParameters(String[] fields) throws Exception {
    fieldCount = 0;
    for (int i = 0; i < fields.length; i++)
      propertyReferenced[i] = false;

    propertyCount = fields.length;
    while (true) {
      String str = tokenizer.peekToken();
      if (str == null) {
        fieldCount = 0;
        break;
      }
      if (str.charAt(0) != '_')
        break;
      tokenizer.getTokenPeeked();
      fieldTypes[fieldCount] = NONE;
      for (int i = fields.length; --i >= 0;)
        if (isMatch(str, fields[i])) {
          fieldTypes[fieldCount] = i;
          propertyReferenced[i] = true;
          break;
        }
      fieldCount++;
    }
    if (fieldCount > 0)
      loopData = new String[fieldCount];
  }

  void disableField(int fieldCount, int[] fieldTypes, int fieldIndex) {
    for (int i = fieldCount; --i >= 0;)
      if (fieldTypes[i] == fieldIndex)
        fieldTypes[i] = NONE;
  }

  private void skipLoop() throws Exception {
    String str;
    while ((str = tokenizer.peekToken()) != null && str.charAt(0) == '_')
      str  = tokenizer.getTokenPeeked();
    while (tokenizer.getNextDataToken() != null) {
    }
  }

  final static boolean isMatch(String str1, String str2) {
    int cch = str1.length();
    if (str2.length() != cch)
      return false;
    for (int i = cch; --i >= 0;) {
      char ch1 = str1.charAt(i);
      char ch2 = str2.charAt(i);
      if (ch1 == ch2)
        continue;
      if ((ch1 == '_' || ch1 == '.') && (ch2 == '_' || ch2 == '.'))
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

  // not static! we are saving global variables here.
  
  class RidiculousFileFormatTokenizer {
    String str;
    int ich;
    int cch;
    boolean wasUnQuoted;

    void setString(String str) {
      this.str = str;
      cch = (str == null ? 0 : str.length());
      ich = 0;
    }

    String setStringNextLine(BufferedReader reader) throws Exception {
      setString(reader.readLine());
      if (str == null || str.length() == 0 || str.charAt(0) != ';')
        return str;
      String newline;
      ich = 1;
      str = '\1' + (hasMoreTokens() ? str.substring(1) + '\n' : "");
      while ((newline = reader.readLine()) != null) {
        if (newline.startsWith(";")) {
          if (str.length() > 1)
            str = str.substring(0, str.length() - 1);
          str += '\1' + newline.substring(1);
          break;
        }
        str += newline + '\n';
      }
      setString(str);
      return str;
    }

    boolean hasMoreTokens() {
      if (str == null)
        return false;
      char ch = '#';
      while (ich < cch && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
        ++ich;
      return (ich < cch && ch != '#');
    }

    /**
     * assume that hasMoreTokens() has been called and that
     * ich is pointing at a non-white character
     *
     * @return next token or "\0" if '.' or '?' 
     */
    String nextToken() {
      if (ich == cch)
        return null;
      int ichStart = ich;
      char ch = str.charAt(ichStart);
      if (ch != '\'' && ch != '"' && ch != '\1') {
        wasUnQuoted = true;
        while (ich < cch && (ch = str.charAt(ich)) != ' ' && ch != '\t')
          ++ich;
        if (ich == ichStart + 1)
          if (str.charAt(ichStart) == '.' || str.charAt(ichStart) == '?')
            return "\0";
        return str.substring(ichStart, ich);
      }
      wasUnQuoted = false;
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

    boolean wasUnQuoted() {
      return wasUnQuoted;
    }

    /**
     * general reader for loop data
     * fills loopData with fieldCount fields
     * 
     * @param loopData
     * @param fieldCount   if < 0, then ignore '_' in first field
     * @return successful
     * @throws Exception
     */
    boolean getData(String[] loopData, int fieldCount) throws Exception {
      // line is already present, and we leave with the next line to parse
      for (int i = 0; i < fieldCount; ++i) {
        String str = getNextDataToken();
        if (str == null)
          return false;
        loopData[i] = str;
      }
      return true;
    }

    String getNextDataToken() throws Exception { 
      String str = peekToken();
      if (str == null)
        return null;
      if (wasUnQuoted())
        if (str.charAt(0) == '_' || str.startsWith("loop_")
            || str.startsWith("data_"))
          return null;
      return tokenizer.getTokenPeeked();
    }
    
    String getNextToken() throws Exception {
      while (!hasMoreTokens())
        if ((line = setStringNextLine(reader)) == null)
          return null;
      return nextToken();
    }

    String strPeeked;
    int ichPeeked;
    
    String peekToken() throws Exception {
      while (!hasMoreTokens())
        if ((line = setStringNextLine(reader)) == null)
          return null;
      int ich = this.ich;
      strPeeked = nextToken();
      ichPeeked= this.ich;
      this.ich = ich;
      return strPeeked;
    }
    
    String getTokenPeeked() {
      this.ich = ichPeeked;
      return strPeeked;
    }
  }

}
