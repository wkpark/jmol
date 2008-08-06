/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-20 07:48:25 -0500 (Fri, 20 Oct 2006) $
 * $Revision: 5991 $
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
package org.jmol.adapter.readers.cifpdb;

import org.jmol.adapter.smarter.*;
import org.jmol.api.JmolAdapter;


import java.io.BufferedReader;
import java.util.Hashtable;

import org.jmol.util.Logger;

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
public class CifReader extends AtomSetCollectionReader {

  private RidiculousFileFormatTokenizer tokenizer = new RidiculousFileFormatTokenizer();

  private String thisDataSetName = "";
  private String chemicalName = "";
  private String thisStructuralFormula = "";
  private String thisFormula = "";
  
  private Hashtable htHetero;

  public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    int nAtoms = 0;
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("cif");

    /*
     * Modified for 10.9.64 9/23/06 by Bob Hanson to remove as much as possible of line dependence.
     * a loop could now go:
     * 
     * blah blah blah  loop_ _a _b _c 0 1 2 0 3 4 0 5 6 loop_...... 
     * 
     * we don't actually check that any skpped loop has the proper number of 
     * data points --- some multiple of the number of data keys -- but other
     * than that, we are checking here for proper CIF syntax, and Jmol will
     * report if it finds data where a key is supposed to be.
     * 
     *
     */
    line = "";
    boolean skipping = false;
    try {
      while ((key = tokenizer.peekToken()) != null) {
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
            if (nAtoms == atomSetCollection.getAtomCount())
              // we found no atoms -- must revert
              atomSetCollection.removeAtomSet();
            else
              applySymmetry();
            processDataParameter();
            iHaveDesiredModel = (desiredModelNumber > 0);
            nAtoms = atomSetCollection.getAtomCount();
          }
          continue;
        }
        if (key.startsWith("loop_")) {
          if (skipping) {
            tokenizer.getTokenPeeked();
            skipLoop();
          } else {
            processLoopBlock();
          }
          continue;
        }
        // global_ and stop_ are reserved STAR keywords
        // see http://www.iucr.org/iucr-top/lists/comcifs-l/msg00252.html
        // http://www.iucr.org/iucr-top/cif/spec/version1.1/cifsyntax.html#syntax

        // stop_ is not allowed, because nested loop_ is not allowed
        // global_ is a reserved STAR word; not allowed in CIF
        // ah, heck, let's just flag them as CIF ERRORS
        /*      
         if (key.startsWith("global_") || key.startsWith("stop_")) {
         tokenizer.getTokenPeeked();
         continue;
         }
         */
        if (key.indexOf("_") != 0) {
          Logger.warn("CIF ERROR ? should be an underscore: " + key);
          tokenizer.getTokenPeeked();
        } else if (!getData()) {
          continue;
        }
        if (!skipping) {
          key = key.replace('.', '_');
          if (key.startsWith("_chemical_name")) {
            processChemicalInfo("name");
          } else if (key.startsWith("_chemical_formula_structural")) {
            processChemicalInfo("structuralFormula");
          } else if (key.startsWith("_chemical_formula_sum")) {
            processChemicalInfo("formula");
          } else if (key.startsWith("_cell_")) {
            processCellParameter();
          } else if (key.startsWith("_symmetry_space_group_name_H-M")
              || key.startsWith("_symmetry_space_group_name_Hall")) {
            processSymmetrySpaceGroupName();
          } else if (key.startsWith("_atom_sites_fract_tran")) {
            processUnitCellTransformMatrix();
          } else if (key.startsWith("_pdbx_entity_nonpoly")) {
            processNonpolyData();
          }
        }
      }

      if (atomSetCollection.getAtomCount() == nAtoms)
        atomSetCollection.removeAtomSet();
      else
        applySymmetry();
      if (htSites != null)
        addSites(htSites);
      atomSetCollection.setCollectionName("<collection of "
          + atomSetCollection.getAtomSetCount() + " models>");
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }

  ////////////////////////////////////////////////////////////////
  // processing methods
  ////////////////////////////////////////////////////////////////

  /**
   *  initialize a new atom set
   *  
   */
  private void processDataParameter() {
    tokenizer.getTokenPeeked();
    thisDataSetName = (key.length() < 6 ? "" : key.substring(5));
    if (thisDataSetName.length() > 0) {
      if (atomSetCollection.getCurrentAtomSetIndex() >= 0) {
        // note that there can be problems with multi-data mmCIF sets each with
        // multiple models; and we could be loading multiple files!
        atomSetCollection.newAtomSet();
      } else {
        atomSetCollection.setCollectionName(thisDataSetName);
      }
    }
    Logger.debug(key);
  }
  
  /**
   * reads some of the more interesting info 
   * into specific atomSetAuxiliaryInfo elements
   * 
   * @param type    "name" "formula" etc.
   * @throws Exception
   */
  private void processChemicalInfo(String type) throws Exception {
    if (type.equals("name"))
      chemicalName = data = tokenizer.fullTrim(data);
    else if (type.equals("structuralFormula"))
      thisStructuralFormula = data = tokenizer.fullTrim(data);
    else if (type.equals("formula"))
      thisFormula = data = tokenizer.fullTrim(data);
    if (Logger.debugging) {
      Logger.debug(type + " = " + data);
    }
  }

  /**
   * done by AtomSetCollectionReader
   * 
   * @throws Exception
   */
  private void processSymmetrySpaceGroupName() throws Exception {
    setSpaceGroupName(data);
  }

  final public static String[] cellParamNames = { 
    "_cell_length_a", 
    "_cell_length_b",
    "_cell_length_c", 
    "_cell_angle_alpha", 
    "_cell_angle_beta",
    "_cell_angle_gamma" 
  };

  /**
   * unit cell parameters -- two options, so we use MOD 6
   * 
   * @throws Exception
   */
  private void processCellParameter() throws Exception {
    for (int i = cellParamNames.length; --i >= 0;)
      if (isMatch(key, cellParamNames[i])) {
        setUnitCellItem(i, parseFloat(data));
        return;
      }
  }

  final private static String[] TransformFields = {
      "x[1][1]", "x[1][2]", "x[1][3]", "r[1]",
      "x[2][1]", "x[2][2]", "x[2][3]", "r[2]",
      "x[3][1]", "x[3][2]", "x[3][3]", "r[3]",
  };

  /**
   * 
   * the PDB transformation matrix cartesian --> fractional
   * 
   * @throws Exception
   */
  private void processUnitCellTransformMatrix() throws Exception {
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

  private String key;
  private String data;
  
  /**
   * 
   * @return TRUE if data, even if ''; FALSE if '.' or  '?' or eof.
   * 
   * @throws Exception
   */
  private boolean getData() throws Exception {
    key = tokenizer.getTokenPeeked();
    data = tokenizer.getNextToken();
    if (data == null) {
      Logger.warn("CIF ERROR ? end of file; data missing: " + key);
      return false;
    }
    return (data.length() == 0 || data.charAt(0) != '\0');
  }
  
  /**
   * processes loop_ blocks of interest or skips the data
   * 
   * @throws Exception
   */
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
    if (str.startsWith("_atom_type")) {
      processAtomTypeLoopBlock();
      return;
    }
    if (str.startsWith("_geom_bond")) {
      if (doApplySymmetry && !applySymmetryToBonds)
        skipLoop();
      else
        processGeomBondLoopBlock();
      return;
    }
    if (str.startsWith("_pdbx_entity_nonpoly")) {
      processNonpolyLoopBlock();
      return;
    }
    if (str.startsWith("_chem_comp")) {
      processChemCompLoopBlock();
      return;
    }
    if (str.startsWith("_struct_conf") && !str.startsWith("_struct_conf_type")) {
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
        Logger.warn("ignoring file-based symmetry operators");
        skipLoop();
      } else {
        processSymmetryOperationsLoopBlock();
      }
      return;
    }
    if (str.startsWith("_struct_site")) {
      processStructSiteBlock();
      return;
    }
    skipLoop();
  }

  ////////////////////////////////////////////////////////////////
  // atom type data
  ////////////////////////////////////////////////////////////////


  private Hashtable atomTypes;
  
  final private static byte ATOM_TYPE_SYMBOL = 0;
  final private static byte ATOM_TYPE_OXIDATION_NUMBER = 1;

  final private static String[] atomTypeFields = { 
      "_atom_type_symbol",
      "_atom_type_oxidation_number", 
  };

  /**
   * 
   * reads the oxidation number and associates it with an atom name, which can
   * then later be associated with the right atom indirectly.
   * 
   * @throws Exception
   */
  private void processAtomTypeLoopBlock() throws Exception {
    parseLoopParameters(atomTypeFields);
    for (int i = propertyCount; --i >= 0;)
      if (fieldOf[i] == NONE) {
        skipLoop();
        return;
      }

    while (tokenizer.getData()) {
      String atomTypeSymbol = null;
      float oxidationNumber = Float.NaN;
      for (int i = 0; i < fieldCount; ++i) {
        switch (fieldProperty(i)) {
        case NONE:
          break;
        case ATOM_TYPE_SYMBOL:
          atomTypeSymbol = field;
          break;
        case ATOM_TYPE_OXIDATION_NUMBER:
          oxidationNumber = parseFloat(field);
          break;
        }
      }
      if (atomTypeSymbol == null || Float.isNaN(oxidationNumber))
        continue;
      if (atomTypes == null)
        atomTypes = new Hashtable();
      atomTypes.put(atomTypeSymbol, new Float(oxidationNumber));
    }
  }

  ////////////////////////////////////////////////////////////////
  // atom site data
  ////////////////////////////////////////////////////////////////

  final private static byte NONE = -1;
  final private static byte TYPE_SYMBOL = 0;
  final private static byte LABEL = 1;
  final private static byte AUTH_ATOM = 2;
  final private static byte FRACT_X = 3;
  final private static byte FRACT_Y = 4;
  final private static byte FRACT_Z = 5;
  final private static byte CARTN_X = 6;
  final private static byte CARTN_Y = 7;
  final private static byte CARTN_Z = 8;
  final private static byte OCCUPANCY = 9;
  final private static byte B_ISO = 10;
  final private static byte COMP_ID = 11;
  final private static byte ASYM_ID = 12;
  final private static byte SEQ_ID = 13;
  final private static byte INS_CODE = 14;
  final private static byte ALT_ID = 15;
  final private static byte GROUP_PDB = 16;
  final private static byte MODEL_NO = 17;
  final private static byte DUMMY_ATOM = 18;
  final private static byte DISORDER_GROUP = 19;
  final private static byte ANISO_LABEL = 20;
  final private static byte ANISO_U11 = 21;
  final private static byte ANISO_U22 = 22;
  final private static byte ANISO_U33 = 23;
  final private static byte ANISO_U12 = 24;
  final private static byte ANISO_U13 = 25;
  final private static byte ANISO_U23 = 26;
  final private static byte ANISO_MMCIF_U11 = 27;
  final private static byte ANISO_MMCIF_U22 = 28;
  final private static byte ANISO_MMCIF_U33 = 29;
  final private static byte ANISO_MMCIF_U12 = 30;
  final private static byte ANISO_MMCIF_U13 = 31;
  final private static byte ANISO_MMCIF_U23 = 32;
  final private static byte U_ISO_OR_EQUIV = 33;
  final private static byte ANISO_B11 = 34;
  final private static byte ANISO_B22 = 35;
  final private static byte ANISO_B33 = 36;
  final private static byte ANISO_B12 = 37;
  final private static byte ANISO_B13 = 38;
  final private static byte ANISO_B23 = 39;
  final private static byte ANISO_Beta_11 = 40;
  final private static byte ANISO_Beta_22 = 41;
  final private static byte ANISO_Beta_33 = 42;
  final private static byte ANISO_Beta_12 = 43;
  final private static byte ANISO_Beta_13 = 44;
  final private static byte ANISO_Beta_23 = 45;
  final private static byte ADP_TYPE = 46;

  final private static String[] atomFields = { 
      "_atom_site_type_symbol",
      "_atom_site_label", 
      "_atom_site_auth_atom_id", 
      "_atom_site_fract_x",
      "_atom_site_fract_y", 
      "_atom_site_fract_z", 
      "_atom_site_Cartn_x",
      "_atom_site_Cartn_y", 
      "_atom_site_Cartn_z", 
      "_atom_site_occupancy",
      "_atom_site_b_iso_or_equiv", 
      "_atom_site_auth_comp_id",
      "_atom_site_auth_asym_id", 
      "_atom_site_auth_seq_id",
      "_atom_site_pdbx_PDB_ins_code", 
      "_atom_site_label_alt_id",
      "_atom_site_group_PDB", 
      "_atom_site_pdbx_PDB_model_num",
      "_atom_site_calc_flag", 
      "_atom_site_disorder_group",
      "_atom_site_aniso_label", 
      "_atom_site_aniso_U_11",
      "_atom_site_aniso_U_22",
      "_atom_site_aniso_U_33",
      "_atom_site_aniso_U_12",
      "_atom_site_aniso_U_13",
      "_atom_site_aniso_U_23",
      "_atom_site_anisotrop_U[1][1]",
      "_atom_site_anisotrop_U[2][2]",
      "_atom_site_anisotrop_U[3][3]",
      "_atom_site_anisotrop_U[1][2]",
      "_atom_site_anisotrop_U[1][3]",
      "_atom_site_anisotrop_U[2][3]",
      "_atom_site_U_iso_or_equiv",
      "_atom_site_aniso_B_11",
      "_atom_site_aniso_B_22",
      "_atom_site_aniso_B_33",
      "_atom_site_aniso_B_12",
      "_atom_site_aniso_B_13",
      "_atom_site_aniso_B_23",
      "_atom_site_aniso_Beta_11",
      "_atom_site_aniso_Beta_22",
      "_atom_site_aniso_Beta_33",
      "_atom_site_aniso_Beta_12",
      "_atom_site_aniso_Beta_13",
      "_atom_site_aniso_Beta_23",
      "_atom_site_adp_type",
  };


  /* to: hansonr@stolaf.edu
   * from: Zukang Feng zfeng@rcsb.rutgers.edu
   * re: Two mmCIF issues
   * date: 4/18/2006 10:30 PM
   * "You should always use _atom_site.auth_asym_id for PDB chain IDs."
   * 
   * 
   */

  /**
   * reads atom data in any order
   * 
   * @return TRUE if successful; FALS if EOF encountered
   * @throws Exception
   */
  boolean processAtomSiteLoopBlock() throws Exception {
    int currentModelNO = -1;
    boolean isPDB = false;
    parseLoopParameters(atomFields);
    if (fieldOf[CARTN_X] != NONE) {
      setFractionalCoordinates(false);
      disableField(FRACT_X);
      disableField(FRACT_Y);
      disableField(FRACT_Z);
    } else if (fieldOf[FRACT_X] != NONE) {
      setFractionalCoordinates(true);
      disableField(CARTN_X);
      disableField(CARTN_Y);
      disableField(CARTN_Z);
    } else if (fieldOf[ANISO_LABEL] != NONE) {
    } else {
      // it is a different kind of _atom_site loop block
      skipLoop();
      return false;
    }
    while (tokenizer.getData()) {
      Atom atom = new Atom();
      for (int i = 0; i < fieldCount; ++i) {
        switch (fieldProperty(i)) {
        case NONE:
          break;
        case TYPE_SYMBOL:
          String elementSymbol;
          if (field.length() < 2) {
            elementSymbol = field;
          } else {
            char ch1 = Character.toLowerCase(field.charAt(1));
            if (Atom.isValidElementSymbol(firstChar, ch1))
              elementSymbol = "" + firstChar + ch1;
            else
              elementSymbol = "" + firstChar;
          }
          atom.elementSymbol = elementSymbol;
          if (atomTypes != null && atomTypes.containsKey(field)) {
            float charge = ((Float) atomTypes.get(field)).floatValue();
            atom.formalCharge = (int) (charge + (charge < 0 ? -0.5 : 0.5));
            //because otherwise -1.6 is rounded UP to -1, and  1.6 is rounded DOWN to 1
            if (Math.abs(atom.formalCharge - charge) > 0.1)
              if (Logger.debugging) {
                Logger.debug("CIF charge on " + field + " was " + charge
                    + "; rounded to " + atom.formalCharge);
              }
          }
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
          atom.bfactor = parseFloat(field) * 100f;
          break;
        case COMP_ID:
          atom.group3 = field;
          break;
        case ASYM_ID:
          if (field.length() > 1)
            Logger.warn("Don't know how to deal with chains more than 1 char: "
                + field);
          atom.chainID = firstChar;
          break;
        case SEQ_ID:
          atom.sequenceNumber = parseInt(field);
          break;
        case INS_CODE:
          atom.chainID = firstChar;
          break;
        case ALT_ID:
        case DISORDER_GROUP: //not QUITE correct
          atom.alternateLocationID = firstChar;
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
        case ADP_TYPE:
          if (field.equalsIgnoreCase("Uiso")) {
            int j = fieldOf[U_ISO_OR_EQUIV];
            if (j != NONE) {
              if (atom.anisoBorU == null)
                atom.anisoBorU = new float[8];
              atom.anisoBorU[7] = parseFloat(loopData[j]);
              atom.anisoBorU[6] = 8; // Ortep Type 8: D = 2pi^2, C = 2, a*b*
            }
          }
          break;
        case ANISO_LABEL:
          int iAtom = atomSetCollection.getAtomNameIndex(field);
          if (iAtom < 0)
            return false;
          atom = atomSetCollection.getAtom(iAtom);
          break;
        case ANISO_U11:
        case ANISO_U22:
        case ANISO_U33:
        case ANISO_U12:
        case ANISO_U13:
        case ANISO_U23:
        case ANISO_MMCIF_U11:
        case ANISO_MMCIF_U22:
        case ANISO_MMCIF_U33:
        case ANISO_MMCIF_U12:
        case ANISO_MMCIF_U13:
        case ANISO_MMCIF_U23:
          if (atom.anisoBorU == null)
            atom.anisoBorU = new float[8];
          int iType = (propertyOf[i] - ANISO_U11) % 6;
          atom.anisoBorU[iType] = parseFloat(field);
          atom.anisoBorU[6] = 8; // Ortep type 8: D = 2pi^2, C = 2, a*b*
          break;
        case ANISO_B11:
        case ANISO_B22:
        case ANISO_B33:
        case ANISO_B12:
        case ANISO_B13:
        case ANISO_B23:
           if (atom.anisoBorU == null)
             atom.anisoBorU = new float[8];
           int iTypeB = (propertyOf[i] - ANISO_B11) % 6;
           atom.anisoBorU[iTypeB] = parseFloat(field);
           atom.anisoBorU[6] = 4; // Ortep Type 4: D = 1/4, C = 2, a*b*
          break;
        case ANISO_Beta_11:
        case ANISO_Beta_22:
        case ANISO_Beta_33:
        case ANISO_Beta_12:
        case ANISO_Beta_13:
        case ANISO_Beta_23:
           if (atom.anisoBorU == null)
             atom.anisoBorU = new float[8];
           int iTypeBeta = (propertyOf[i] - ANISO_Beta_11) % 6;
           atom.anisoBorU[iTypeBeta] = parseFloat(field);
           atom.anisoBorU[6] = 0; // Ortep Type 0: D = 1, c = 2 -- see org.jmol.symmetry/UnitCell.java
          break;
        }
      }
      if (Float.isNaN(atom.x) || Float.isNaN(atom.y) || Float.isNaN(atom.z)) {
        Logger.warn("atom " + atom.atomName
            + " has invalid/unknown coordinates");
      } else {
        if (fieldOf[ANISO_LABEL] != NONE)
          continue;
        if (filter != null)
          if (!filterAtom(atom))
            continue;
        setAtomCoord(atom);
        atomSetCollection.addAtomWithMappedName(atom);
        if (atom.isHetero && htHetero != null) {
          atomSetCollection.setAtomSetAuxiliaryInfo("hetNames", htHetero);
          atomSetCollection.setAtomSetCollectionAuxiliaryInfo("hetNames",
              htHetero);
          htHetero = null;
        }
      }
    }
    if (isPDB) {
      atomSetCollection
          .setAtomSetCollectionAuxiliaryInfo("isPDB", Boolean.TRUE);
      atomSetCollection.setAtomSetAuxiliaryInfo("isPDB", Boolean.TRUE);
    }
    return true;
  }
     
  ////////////////////////////////////////////////////////////////
  // bond data
  ////////////////////////////////////////////////////////////////

  final private static byte GEOM_BOND_ATOM_SITE_LABEL_1 = 0;
  final private static byte GEOM_BOND_ATOM_SITE_LABEL_2 = 1;
  final private static byte GEOM_BOND_SITE_SYMMETRY_2 = 2;

  final private static String[] geomBondFields = { 
      "_geom_bond_atom_site_label_1",
      "_geom_bond_atom_site_label_2", 
//      "_geom_bond_site_symmetry_2",
  };

  /**
   * 
   * reads bond data -- N_ijk symmetry business is ignored,
   * so we only indicate bonds within the unit cell to just the
   * original set of atoms. "connect" script or "set forceAutoBond"
   * will override these values.
   * 
   * @throws Exception
   */
  private void processGeomBondLoopBlock() throws Exception {
    parseLoopParameters(geomBondFields);
    for (int i = propertyCount; --i >= 0;)
      if (fieldOf[i] == NONE) {
        Logger.warn("?que? missing _geom_bond property:" + i);
        skipLoop();
        return;
      }

    while (tokenizer.getData()) {
      int atomIndex1 = -1;
      int atomIndex2 = -1;
      for (int i = 0; i < fieldCount; ++i) {
        switch (fieldProperty(i)) {
        case NONE:
          break;
        case GEOM_BOND_ATOM_SITE_LABEL_1:
          atomIndex1 = atomSetCollection.getAtomNameIndex(field);
          break;
        case GEOM_BOND_ATOM_SITE_LABEL_2:
          atomIndex2 = atomSetCollection.getAtomNameIndex(field);
          break;
        case GEOM_BOND_SITE_SYMMETRY_2:
          //symmetry = field;
          break;
        }
      }
      if (/*symmetry != null || */ atomIndex1 < 0 || atomIndex2 < 0)
        continue;
      Bond bond = new Bond();
      bond.atomIndex1 = atomIndex1;
      bond.atomIndex2 = atomIndex2;
      atomSetCollection.addBond(bond);
    }
  }
  
  ////////////////////////////////////////////////////////////////
  // HETATM identity
  ////////////////////////////////////////////////////////////////

  final private static byte NONPOLY_ENTITY_ID = 0;
  final private static byte NONPOLY_NAME = 1;
  final private static byte NONPOLY_COMP_ID = 2;

  final private static String[] nonpolyFields = { 
      "_pdbx_entity_nonpoly_entity_id",
      "_pdbx_entity_nonpoly_name", 
      "_pdbx_entity_nonpoly_comp_id", 
  };
  
  /**
   * 
   * optional nonloop format -- see 1jsa.cif
   * 
   */
  private String[] hetatmData;
  private void processNonpolyData() {
    if (hetatmData == null)
      hetatmData = new String[3];
    for (int i = nonpolyFields.length; --i >= 0;)
      if (isMatch(key, nonpolyFields[i])) {
        hetatmData[i] = data;
        break;
      }
    if (hetatmData[NONPOLY_NAME] == null || hetatmData[NONPOLY_COMP_ID] == null)
      return;
    addHetero(hetatmData[NONPOLY_COMP_ID], hetatmData[NONPOLY_NAME]);
    hetatmData = null;
  }


  final private static byte CHEM_COMP_ID = 0;
  final private static byte CHEM_COMP_NAME = 1;

  final private static String[] chemCompFields = { 
      "_chem_comp_id",
      "_chem_comp_name",  
  };
  

  /**
   * 
   * a general name definition field. Not all hetero
   * 
   * @throws Exception
   */
  private void processChemCompLoopBlock() throws Exception {
    parseLoopParameters(chemCompFields);
    while (tokenizer.getData()) {
      String groupName = null;
      String hetName = null;
      for (int i = 0; i < fieldCount; ++i) {
        switch (fieldProperty(i)) {
        case NONE:
          break;
        case CHEM_COMP_ID:
          groupName = field;
          break;
        case CHEM_COMP_NAME:
          hetName = field;
          break;
        }
      }
      if (groupName == null || hetName == null)
        return;
      addHetero(groupName, hetName);
    }
  }

  /**
   * 
   * a HETERO name definition field. Maybe not all hetero? nonpoly?
   * 
   * @throws Exception
   */
  private void processNonpolyLoopBlock() throws Exception {
    parseLoopParameters(nonpolyFields);
    while (tokenizer.getData()) {
      String groupName = null;
      String hetName = null;
      for (int i = 0; i < fieldCount; ++i) {
        switch (fieldProperty(i)) {
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
      addHetero(groupName, hetName);
    }
  }

  private void addHetero(String groupName, String hetName) {
    if (!JmolAdapter.isHetero(groupName))
      return;
    if (htHetero == null)
      htHetero = new Hashtable();
    htHetero.put(groupName, hetName);
    if (Logger.debugging) {
      Logger.debug("hetero: " + groupName + " = " + hetName);
    }
  }
  
  ////////////////////////////////////////////////////////////////
  // helix and turn structure data
  ////////////////////////////////////////////////////////////////

  final private static byte CONF_TYPE_ID = 0;
  final private static byte BEG_ASYM_ID = 1;
  final private static byte BEG_SEQ_ID = 2;
  final private static byte BEG_INS_CODE = 3;
  final private static byte END_ASYM_ID = 4;
  final private static byte END_SEQ_ID = 5;
  final private static byte END_INS_CODE = 6;

  final private static String[] structConfFields = { 
      "_struct_conf_conf_type_id",
      "_struct_conf_beg_auth_asym_id", 
      "_struct_conf_beg_auth_seq_id",
      "_struct_conf_pdbx_beg_PDB_ins_code",
      "_struct_conf_end_auth_asym_id", 
      "_struct_conf_end_auth_seq_id",
      "_struct_conf_pdbx_end_PDB_ins_code", 
  };

  /**
   * identifies ranges for HELIX and TURN
   * 
   * @throws Exception
   */
  private void processStructConfLoopBlock() throws Exception {
    parseLoopParameters(structConfFields);
    for (int i = propertyCount; --i >= 0;)
      if (fieldOf[i] == NONE) {
        Logger.warn("?que? missing _struct_conf property:" + i);
        skipLoop();
        return;
      }
    while (tokenizer.getData()) {
      Structure structure = new Structure();
      for (int i = 0; i < fieldCount; ++i) {
        switch (fieldProperty(i)) {
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

  final private static String[] structSheetRangeFields = {
    "_struct_sheet_range_sheet_id",  //unused placeholder
    "_struct_sheet_range_beg_auth_asym_id",
    "_struct_sheet_range_beg_auth_seq_id",
    "_struct_sheet_range_pdbx_beg_PDB_ins_code",
    "_struct_sheet_range_end_auth_asym_id",
    "_struct_sheet_range_end_auth_seq_id",
    "_struct_sheet_range_pdbx_end_PDB_ins_code", 
  };

  /**
   * 
   * identifies sheet ranges
   * 
   * @throws Exception
   */
  private void processStructSheetRangeLoopBlock() throws Exception {
    parseLoopParameters(structSheetRangeFields);
    for (int i = propertyCount; --i >= 0;)
      if (fieldOf[i] == NONE) {
        Logger.warn("?que? missing _struct_conf property:" + i);
        skipLoop();
        return;
      }
    while (tokenizer.getData()) {
      Structure structure = new Structure();
      structure.structureType = "sheet";
      for (int i = 0; i < fieldCount; ++i) {
        switch (fieldProperty(i)) {
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

  final private static byte SITE_ID = 0;
  final private static byte SITE_COMP_ID = 1;
  final private static byte SITE_ASYM_ID = 2;
  final private static byte SITE_SEQ_ID = 3;
  final private static byte SITE_INS_CODE = 4; //???

  final private static String[] structSiteRangeFields = {
    "_struct_site_gen_site_id",  
    "_struct_site_gen_auth_comp_id", 
    "_struct_site_gen_auth_asym_id", 
    "_struct_site_gen_auth_seq_id",  
    "_struct_site_gen_label_alt_id",  //should be an insertion code, not an alt ID? 
  };

  
  /*

loop_
_struct_site_gen.id 
_struct_site_gen.site_id 
_struct_site_gen.pdbx_num_res 
_struct_site_gen.label_comp_id 
_struct_site_gen.label_asym_id 
_struct_site_gen.label_seq_id 
_struct_site_gen.auth_comp_id 
_struct_site_gen.auth_asym_id 
_struct_site_gen.auth_seq_id 
_struct_site_gen.label_atom_id 
_struct_site_gen.label_alt_id 
_struct_site_gen.symmetry 
_struct_site_gen.details 
1 CAT 5 GLN A 92  GLN A 92  . . ? ? 
2 CAT 5 GLU A 58  GLU A 58  . . ? ? 
3 CAT 5 HIS A 40  HIS A 40  . . ? ? 
4 CAT 5 TYR A 38  TYR A 38  . . ? ? 
5 CAT 5 PHE A 100 PHE A 100 . . ? ? 
# 

*/
  
  private int siteNum;
  private Hashtable htSites;
  
  /**
   * 
   * identifies structure sites
   * 
   * @throws Exception
   */
  private void processStructSiteBlock() throws Exception {
    parseLoopParameters(structSiteRangeFields);
    for (int i = 3; --i >= 0;)
      if (fieldOf[i] == NONE) {
        Logger.warn("?que? missing _struct_site property:" + i);
        skipLoop();
        return;
      }
    String siteID = "";
    String seqNum = "";
    String insCode = "";
    String chainID = "";
    String resID = "";
    String group = "";
    Hashtable htSite = null;
    htSites = new Hashtable();
    while (tokenizer.getData()) {
      for (int i = 0; i < fieldCount; ++i) {
        switch (fieldProperty(i)) {
        case SITE_ID:
          if (group != "") {
            String groups = (String) htSite.get("groups");
            groups += (groups.length() == 0 ? "" : ",") + group;
            group = "";
            htSite.put("groups", groups);
          }
          siteID = field;
          htSite = (Hashtable)htSites.get(siteID);
          if (htSite == null) {
            htSite = new Hashtable();
            htSite.put("seqNum", "site_" + (++siteNum));
            htSite.put("groups", "");
            htSites.put(siteID, htSite);
          }
          seqNum = "";
          insCode = "";
          chainID = "";
          resID = "";
          break;
        case SITE_COMP_ID:
          resID = field;
          break;
        case SITE_ASYM_ID:
          chainID = field;
          break;
        case SITE_SEQ_ID:
          seqNum = field;
          break;
        case SITE_INS_CODE: //optional
          insCode = field;
          break;
        }
        if (seqNum != "" && resID != "")
          group = "[" + resID + "]" + seqNum
            + (insCode.length() > 0 ?  "^" + insCode : "")
            + (chainID.length() > 0 ? ":" + chainID : "");
      }      
    }
    if (group != "") {
      String groups = (String) htSite.get("groups");
      groups += (groups.length() == 0 ? "" : ",") + group;
      group = "";
      htSite.put("groups", groups);
    }
  }

  ////////////////////////////////////////////////////////////////
  // symmetry operations
  ////////////////////////////////////////////////////////////////

  final private static byte SYMOP_XYZ = 0;
  final private static byte SYM_EQUIV_XYZ = 1;

  final private static String[] symmetryOperationsFields = {
      "_space_group_symop_operation_xyz", 
      "_symmetry_equiv_pos_as_xyz", 
  };

  /**
   * retrieves symmetry operations
   * 
   * @throws Exception
   */
  private void processSymmetryOperationsLoopBlock() throws Exception {
    parseLoopParameters(symmetryOperationsFields);
    int nRefs = 0;
    for (int i = propertyCount; --i >= 0;)
      if (fieldOf[i] != NONE)
        nRefs++;
    if (nRefs != 1) {
      Logger.warn("?que? _symmetry_equiv or _space_group_symop property not found");
      skipLoop();
      return;
    }
    while (tokenizer.getData()) {
      for (int i = 0; i < fieldCount; ++i) {
        switch (fieldProperty(i)) {
        case SYMOP_XYZ:
        case SYM_EQUIV_XYZ:
          setSymmetryOperator(field);
          break;
        }
      }
    }
  }
  
  private int fieldProperty(int i) {
    return ((field = loopData[i]).length() > 0 
        && (firstChar = field.charAt(0)) != '\0' ? 
            propertyOf[i] : NONE);
  }

  int fieldCount;
  String[] loopData;
  String field;
  
  private char firstChar;
  private int[] propertyOf = new int[100]; // should be enough
  private byte[] fieldOf = new byte[atomFields.length];
  private int propertyCount;
  
  
  /**
   * sets up arrays and variables for tokenizer.getData()
   * 
   * @param fields
   * @throws Exception
   */
  private void parseLoopParameters(String[] fields) throws Exception {
    fieldCount = 0;
    for (int i = fields.length; --i >= 0; )
      fieldOf[i] = NONE;

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
      propertyOf[fieldCount] = NONE;
      for (int i = fields.length; --i >= 0;)
        if (isMatch(str, fields[i])) {
          propertyOf[fieldCount] = i;
          fieldOf[i] = (byte) fieldCount;
          break;
        }
      fieldCount++;
    }
    if (fieldCount > 0)
      loopData = new String[fieldCount];
  }

  /**
   * 
   * used for turning off fractional or nonfractional coord.
   * 
   * @param fieldIndex
   */
  private void disableField(int fieldIndex) {
    int i = fieldOf[fieldIndex];
    if (i != NONE)
        propertyOf[i] = NONE;
  }

  /**
   * 
   * skips all associated loop data
   * 
   * @throws Exception
   */
  private void skipLoop() throws Exception {
    String str;
    while ((str = tokenizer.peekToken()) != null && str.charAt(0) == '_')
      str  = tokenizer.getTokenPeeked();
    while (tokenizer.getNextDataToken() != null) {
    }
  }

  /**
   * 
   * @param str1
   * @param str2
   * @return TRUE if a match
   */
  private static boolean isMatch(String str1, String str2) {
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

  class RidiculousFileFormatTokenizer {
    String str;
    int ich;
    int cch;
    boolean wasUnQuoted;

    /**
     * sets a string to be parsed from the beginning
     * 
     * @param str
     */
    private void setString(String str) {
      this.str = str;
      cch = (str == null ? 0 : str.length());
      ich = 0;
    }

    /*
     * http://www.iucr.org/iucr-top/cif/spec/version1.1/cifsyntax.html#syntax
     * 
     * 17. The special sequence of end-of-line followed 
     * immediately by a semicolon in column one (denoted "<eol>;") 
     * may also be used as a delimiter at the beginning and end 
     * of a character string comprising a data value. The complete 
     * bounded string is called a text field, and may be used to 
     * convey multi-line values. The end-of-line associated with 
     * the closing semicolon does not form part of the data value. 
     * Within a multi-line text field, leading white space within 
     * text lines must be retained as part of the data value; trailing 
     * white space on a line may however be elided.
     * 
     * 18. A text field delimited by the <eol>; digraph may not 
     * include a semicolon at the start of a line of text as 
     * part of its value.
     * 
     * 20. For example, the data value foo may be expressed 
     * equivalently as an unquoted string foo, as a quoted 
     * string 'foo' or as a text field
     *
     *;foo
     *;
     *
     * By contrast the value of the text field
     *
     *; foo
     *  bar
     *;
     *
     * is  foo<eol>  bar (where <eol> represents an end-of-line); 
     * the embedded space characters are significant.
     * 
     * 
     * I (BH) note, however, that we sometimes have:
     * 
     * _some_name
     * ;
     * the name here
     * ;
     * 
     * so this should actually be
     * 
     * ;the name here
     * ;
     * 
     * for this, we use fullTrim();
     * 
     */
    
    /**
     * 
     * sets the string for parsing to be from the next line 
     * when the token buffer is empty, and if ';' is at the 
     * beginning of that line, extends the string to include
     * that full multiline string. Uses \1 to indicate that 
     * this is a special quotation. 
     * 
     * @return  the next line or null if EOF
     * @throws Exception
     */
    String setStringNextLine() throws Exception {
      setString(readLine());
      if (line == null || line.length() == 0 || line.charAt(0) != ';')
        return line;
      ich = 1;
      String str = '\1' + line.substring(1) + '\n';
      while (readLine() != null) {
        if (line.startsWith(";")) {
          // remove trailing <eol> only, and attach rest of next line
          str = str.substring(0, str.length() - 1)
            + '\1' + line.substring(1);
          break;
        }
        str += line + '\n';
      }
      setString(str);
      return line = str;
    }

    /**
     * @return TRUE if there are more tokens in the line buffer
     * 
     */
    boolean hasMoreTokens() {
      if (str == null)
        return false;
      char ch = '#';
      while (ich < cch && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
        ++ich;
      if (str.indexOf("#jmolscript:") >= 0)
        checkLineForScript();
      return (ich < cch && ch != '#');
    }

    /**
     * assume that hasMoreTokens() has been called and that
     * ich is pointing at a non-white character. Also sets
     * boolean wasUnQuoted, because we need to know if we should 
     * be checking for a control keyword. 'loop_' is different from just 
     * loop_ without the quotes.
     *
     * @return null if no more tokens, "\0" if '.' or '?', or next token 
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

    /**
     * general reader for loop data
     * fills loopData with fieldCount fields
     * 
     * @return false if EOF
     * @throws Exception
     */
    boolean getData() throws Exception {
      // line is already present, and we leave with the next line to parse
      for (int i = 0; i < fieldCount; ++i)
        if ((loopData[i] = getNextDataToken()) == null)
          return false;
      return true;
    }

    /**
     * 
     * first checks to see if the next token is an unquoted
     * control code, and if so, returns null 
     * 
     * @return next data token or null
     * @throws Exception
     */
    String getNextDataToken() throws Exception { 
      String str = peekToken();
      if (str == null)
        return null;
      if (wasUnQuoted)
        if (str.charAt(0) == '_' || str.startsWith("loop_")
            || str.startsWith("data_")
            || str.startsWith("stop_")
            || str.startsWith("global_"))
          return null;
      return getTokenPeeked();
    }
    
    /**
     * 
     * @return the next token of any kind, or null
     * @throws Exception
     */
    String getNextToken() throws Exception {
      while (!hasMoreTokens())
        if (setStringNextLine() == null)
          return null;
      return nextToken();
    }

    String strPeeked;
    int ichPeeked;
    
    /**
     * just look at the next token. Saves it for retrieval 
     * using getTokenPeeked()
     * 
     * @return next token or null if EOF
     * @throws Exception
     */
    String peekToken() throws Exception {
      while (!hasMoreTokens())
        if (setStringNextLine() == null)
          return null;
      int ich = this.ich;
      strPeeked = nextToken();
      ichPeeked= this.ich;
      this.ich = ich;
      return strPeeked;
    }
    
    /**
     * 
     * @return the token last acquired; may be null
     */
    String getTokenPeeked() {
      this.ich = ichPeeked;
      return strPeeked;
    }
    
    /**
     * specially for names that might be multiline
     * 
     * @param str
     * @return str without any leading/trailing white space, and no '\n'
     */
    String fullTrim(String str) {
      int pt0 = 0;
      int pt1 = str.length();
      for (;pt0 < pt1; pt0++)
        if ("\n\t ".indexOf(str.charAt(pt0)) < 0)
          break;
      for (;pt0 < pt1; pt1--)
        if ("\n\t ".indexOf(str.charAt(pt1 - 1)) < 0)
          break;
      return str.substring(pt0, pt1);
    }
  }
}
