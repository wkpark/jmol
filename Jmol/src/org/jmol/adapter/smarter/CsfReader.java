/* $RCSfile$
 * $Author: egonw $
 * $Date: 2006-03-18 15:59:33 -0600 (Sat, 18 Mar 2006) $
 * $Revision: 4652 $
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
 * CSF file reader based on CIF idea -- fluid property fields.
 *
 * note that, like CIF, the order of fields is totally unpredictable
 * in addition, ID numbers are not sequential, requiring atomNames
 * 
 * first crack at this 2006/04/13
 * 
 * @author hansonr <hansonr@stolaf.edu>
 */
class CsfReader extends AtomSetCollectionReader {

  int nAtoms = 0;
  
  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("csf");
    
    line = reader.readLine();
    while (line != null) {
      if (line.startsWith("object_class")) {
        processObjectClass();
        // there is already an unprocessed line in the firing chamber
        continue;
      }
      line = reader.readLine();
    }
    return atomSetCollection;
  }
  private void processObjectClass() throws Exception {
    if (line.equals("object_class connector")) {
      processConnectorObject();
      return;
    }
    if (line.equals("object_class atom")) {
      processAtomObject();
      return;
    }
    if (line.equals("object_class bond")) {
      processBondObject();
      return;
    }
    if (line.equals("object_class vibrational_level")) {
      processVibrationObject();
      return;
    }
    line = reader.readLine();
  }
  
  void skipTo(String startsWith) throws Exception {
    while ((line = reader.readLine()) != null && line.indexOf(startsWith) != 0)
        {}
  }
  
  int parseLineParameters(String[] fields,
                          byte[] fieldMap,
                          int[] fieldTypes,
                          boolean[] propertyReferenced) throws Exception {
    String tokens[] = getTokens(line);
    String field;
    int fieldCount = -1;
    for (int ipt = tokens.length; --ipt >= 0; ) {
      field = tokens[ipt];
      for (int i = fields.length; --i >= 0; )
        if (field.equals(fields[i])) {
          int iproperty = fieldMap[i];
          propertyReferenced[iproperty] = true;
          fieldTypes[ipt] = iproperty;
          if (fieldCount == -1)
            fieldCount = ipt + 1;
          break;
        }
    }
    return fieldCount;
  }

  ////////////////////////////////////////////////////////////////
  // connector data
  ////////////////////////////////////////////////////////////////

  final static byte conID   = 1;
  final static byte objCls1 = 2;
  final static byte objID1  = 3;
  final static byte objCls2 = 4;
  final static byte objID2  = 5;
  
  final static byte CONNECTOR_PROPERTY_MAX      = 6;

  final static String[] connectorFields = {
    "ID", "objCls1", "objID1", "objCls2", "objID2"
  };

  final static byte[] connectorFieldMap = {
    conID, objCls1, objID1, objCls2, objID2
  };
  
  Hashtable connectors = new Hashtable();
  
  void processConnectorObject() throws Exception {
    int[] fieldTypes = new int[100]; // should be enough
    boolean[] propertyReferenced = new boolean[CONNECTOR_PROPERTY_MAX];
    skipTo("ID");
    int fieldCount = parseLineParameters(connectorFields, connectorFieldMap,
        fieldTypes, propertyReferenced);
    out: for (; (line = reader.readLine()) != null;) {
      if (line.startsWith("property_flags:"))
        break;
      String thisAtomID = null;
      String thisBondID = null;
      String tokens[] = getTokens(line);
      for (int i = 0; i < fieldCount; ++i) {
        String field = tokens[i];
        switch (fieldTypes[i]) {
        case NONE:
        case conID:
          break;
        case objCls1:
          if (!field.equals("atom"))
            continue out;
          break;
        case objCls2:
          if (!field.equals("bond")) 
            continue out;
          break;
        case objID1:
          thisAtomID = "Atom"+field;
          break;
        case objID2:
          thisBondID = "Bond"+field;
          break;
        default:
        }
      }
      if (thisAtomID != null && thisBondID != null) {
        if (connectors.containsKey(thisBondID)) {
          String[] connect = (String[])connectors.get(thisBondID);
          connect[1] = thisAtomID;  
          connectors.put(thisBondID, connect);
        } else {
          String[] connect = new String[2];
          connect[0] = thisAtomID;
          connectors.put(thisBondID, connect);
        }
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // atom data
  ////////////////////////////////////////////////////////////////

  final static byte NONE           = 0;
  final static byte atomID         = 1;
  final static byte sym            = 2;
  final static byte anum           = 3;
  final static byte chrg           = 4;
  final static byte xyz_coordinates = 5;
  final static byte ATOM_PROPERTY_MAX = 6;
  

  final static String[] atomFields = {
    "ID",
    "sym", "anum",
    "chrg", "xyz_coordinates"
  };

  final static byte[] atomFieldMap = {
    atomID, sym, anum, chrg, xyz_coordinates
  };

  static {
    if (atomFieldMap.length != atomFields.length)
      atomFields[100] = "explode";
  }

  void processAtomObject() throws Exception {
    nAtoms = 0;
    int[] fieldTypes = new int[100]; // should be enough
    boolean[] atomPropertyReferenced = new boolean[ATOM_PROPERTY_MAX];
    skipTo("ID");
    int fieldCount = parseLineParameters(atomFields, atomFieldMap, fieldTypes,
        atomPropertyReferenced);
    for (; (line = reader.readLine()) != null; ) {
      if (line.startsWith("property_flags:"))
        break;
      String tokens[] = getTokens(line);
      Atom atom = new Atom();
      for (int i = 0; i < fieldCount; i++) {
        String field = tokens[i];
        if (field == null)
          logger.log("field == null in " + line);
        switch (fieldTypes[i]) {
        case NONE:
          break;
        case atomID:
          atom.atomName = "Atom"+field;
          break;
        case sym:
          atom.elementSymbol = field;
          break;
        case anum:
          break;
        case xyz_coordinates:
          atom.x = parseFloat(field);
          field = tokens[i + 1];
          atom.y = parseFloat(field);
          field = tokens[i + 2];
          atom.z = parseFloat(field);
          break;
        }
      }
      if (Float.isNaN(atom.x) || Float.isNaN(atom.y) || Float.isNaN(atom.z))
        logger
            .log("atom " + atom.atomName + " has invalid/unknown coordinates");
      else {
        nAtoms++;
        atomSetCollection.addAtomWithMappedName(atom);
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // bond order data
  ////////////////////////////////////////////////////////////////

  final static byte bondID = 1;
  final static byte bondType = 2;
  final static byte BOND_PROPERTY_MAX      = 3;

  final static String[] bondFields  = {
    "ID", "type"
  };

  final static byte[] bondFieldMap = {
    bondID, bondType
  };

  int nBonds = 0;
  
  void processBondObject() throws Exception {
    int[] fieldTypes = new int[100]; // should be enough
    boolean[] propertyReferenced = new boolean[BOND_PROPERTY_MAX];
    skipTo("ID");
    int fieldCount = parseLineParameters(bondFields,
                                         bondFieldMap,
                                         fieldTypes,
                                         propertyReferenced);
    for (; (line = reader.readLine()) != null; ) {
      if (line.startsWith("property_flags:"))
        break;
      String thisBondID = null;
      String tokens[] = getTokens(line);
      for (int i = 0; i < fieldCount; ++i) {
        String field = tokens[i];
        switch (fieldTypes[i]) {
        case NONE:
          break;
        case bondID:
          thisBondID = "Bond"+field;
          break;
        case bondType:
          int order = 1;
          if (field.equals("single"))
            order = 1;
          else if (field.equals("double"))
            order = 2;
          else if (field.equals("triple"))
            order = 3;
          else
            logger.log("unknown CSF bond order: " + field);
          String[] connect = (String[]) connectors.get(thisBondID);
          Bond bond = new Bond();
          bond.atomIndex1 = atomSetCollection.getAtomNameIndex(connect[0]);
          bond.atomIndex2 = atomSetCollection.getAtomNameIndex(connect[1]);
          bond.order=order;
          atomSetCollection.addBond(bond);
          nBonds++;
          break;
        }
      }
    }
  }
  final static byte vibID            = 1;
  final static byte normalMode       = 2;
  final static byte vibEnergy        = 3;
  final static byte transitionDipole = 4;
  final static byte lineWidth        = 5;
  final static byte VIB_PROPERTY_MAX = 6;

  final static String[] vibFields  = {
    "ID", "normalMode", "Energy", "transitionDipole"
  };

  final static byte[] vibFieldMap = {
    vibID, normalMode, vibEnergy, transitionDipole
  };

  void processVibrationObject() throws Exception {
    int[] fieldTypes = new int[100]; // should be enough
    boolean[] propertyReferenced = new boolean[VIB_PROPERTY_MAX];
    skipTo("ID normalMode"); //a bit risky -- could miss it
    int thisvibID = -1;
    float[] vibXYZ = new float[3];
    int iatom = atomSetCollection.getFirstAtomSetAtomCount();
    int xyzpt = 0;
    Atom[] atoms = atomSetCollection.atoms;
    out: for (; (line = reader.readLine()) != null;) {
      if (line.startsWith("property_flags:"))
        break;
      String tokens[] = getTokens(line);
      if (parseInt(tokens[0]) != thisvibID) {
        thisvibID = parseInt(tokens[0]);
        atomSetCollection.cloneFirstAtomSetWithBonds(nBonds);
      }
      for (int i = 1; i < tokens.length; ++i) {
        vibXYZ[xyzpt++] = parseFloat(tokens[i]);
        if(xyzpt == 3) { 
          atoms[iatom].addVibrationVector(vibXYZ[0],vibXYZ[1],vibXYZ[2]);
          iatom++;
          xyzpt = 0;
        }
      }
    }
    
    skipTo("ID"); //second part
    int fieldCount = parseLineParameters(vibFields,
                                         vibFieldMap,
                                         fieldTypes,
                                         propertyReferenced);
    for (; (line = reader.readLine()) != null; ) {
      if (line.startsWith("property_flags:"))
        break;
      String tokens[] = getTokens(line);
      int thisvib = -1;
      for (int i = 0; i < fieldCount; ++i) {
        String field = tokens[i];
        switch (fieldTypes[i]) {
        case NONE:
          break;
        case vibID:
          thisvib = parseInt(field);
          break;
        case vibEnergy:
          atomSetCollection.setAtomSetName(field + " cm^-1", thisvib);
          atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,"Frequencies");
          break;
        }
      }
    }
  }
  
}

