/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Chemistry Development Kit (CDK) project
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may
 * distribute with programs based on this work.
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

/**
 * A reader for SHELX output (RES) files. It does not read all information.
 * The list of fields that is read: TITL, REM, END, CELL, SPGR.
 * In addition atoms are read.
 *
 * <p>A reader for SHELX files. It currently supports SHELXL.
 *
 * <p>The SHELXL format is described on the net:
 * <a href="http://www.msg.ucsf.edu/local/programs/shelxl/ch_07.html"
 * http://www.msg.ucsf.edu/local/programs/shelxl/ch_07.html</a>.
 *
 */

class ShelxReader extends ModelReader {

  boolean endReached;

  Model readModel(BufferedReader reader) throws Exception {
    this.logger = logger;
    model = new Model("shelx");
    model.coordinatesAreFractional = true;

    String line;
    int lineLength;
    readLine_loop:
    while ((line = reader.readLine()) != null) {
      lineLength = line.length();
      if (lineLength > 0 && line.charAt(lineLength - 1) == '=') {
        // this cannot be correct ... the '=' is still in the line
        // but this is what the cdk reader had in place
        line += reader.readLine();
      }
      if (lineLength < 4) {
        if (lineLength == 3 && "END".equalsIgnoreCase(line))
          break;
        continue;
      }
      // FIXME -- should we call toUpperCase(Locale.US) ?
      // although I really don't think it is necessary
      String command = line.substring(0, 4).toUpperCase();
      for (int i = unsupportedRecordTypes.length; --i >= 0; )
        if (command.equals(unsupportedRecordTypes[i]))
          continue readLine_loop;
      for (int i = supportedRecordTypes.length; --i >= 0; )
        if (command.equals(supportedRecordTypes[i])) {
          processSupportedRecord(i, line);
          if (endReached)
            break readLine_loop;
          continue readLine_loop;
        }
      assumeAtomRecord(line);
    }
    return model;
  }

  final static String[] supportedRecordTypes =
  {"TITL", "CELL", "SPGR", "END "};

  void processSupportedRecord(int recordIndex, String line)
    throws Exception {
    switch(recordIndex) {
    case 0: // TITL
      model.modelName = parseTrimmed(line, 4);
      break;
    case 1: // CELL
      cell(line);
      break;
    case 2: // SPGR
      model.spaceGroup = parseTrimmed(line, 4);
      break;
    case 3: // END
      endReached = true;
      break;
    }
  }
  
  void cell(String line) throws Exception {
    /* example:
     * CELL  1.54184   23.56421  7.13203 18.68928  90.0000 109.3799  90.0000
     * CELL   1.54184   7.11174  21.71704  30.95857  90.000  90.000  90.000
     */
    float wavelength = parseFloat(line, 4);
    float[] notionalUnitcell = new float[6];
    for (int i = 0; i < 6; ++i)
      notionalUnitcell[i] = parseFloat(line, ichNextParse);
    model.wavelength = wavelength;
    model.notionalUnitcell = notionalUnitcell;
  }

  void assumeAtomRecord(String line) {
    try {
      //    System.out.println("Assumed to contain an atom: " + line);
      // this line gives an atom, because all lines not starting with
      // a SHELX command is an atom
      String atomName = parseToken(line);
      int scatterFactor = parseInt(line, ichNextParse);
      float a = parseFloat(line, ichNextParse);
      float b = parseFloat(line, ichNextParse);
      float c = parseFloat(line, ichNextParse);
      // skip the rest
      
      Atom atom = model.addNewAtom();
      atom.atomName = atomName;
      atom.scatterFactor = scatterFactor;
      atom.x = a;
      atom.y = b;
      atom.z = c;
    } catch (Exception ex) {
      logger.log("Exception", ex, line);
    }
  }

  final static String[] unsupportedRecordTypes = {
    /* 7.1 Crystal data and general instructions */
    "ZERR",
    "LATT",
    "SYMM",
    "SFAC",
    "DISP",
    "UNIT",
    "LAUE",
    "REM ",
    "MORE",
    "TIME",
    /* 7.2 Reflection data input */
    "HKLF",
    "OMIT",
    "SHEL",
    "BASF",
    "TWIN",
    "EXTI",
    "SWAT",
    "HOPE",
    "MERG",
    /* 7.3 Atom list and least-squares constraints */
    "SPEC",
    "RESI",
    "MOVE",
    "ANIS",
    "AFIX",
    "HFIX",
    "FRAG",
    "FEND",
    "EXYZ",
    "EXTI",
    "EADP",
    "EQIV",
    /* 7.4 The connectivity list */
    "CONN",
    "PART",
    "BIND",
    "FREE",
    /* 7.5 Least-squares restraints */
    "DFIX",
    "DANG",
    "BUMP",
    "SAME",
    "SADI",
    "CHIV",
    "FLAT",
    "DELU",
    "SIMU",
    "DEFS",
    "ISOR",
    "NCSY",
    "SUMP",
    /* 7.6 Least-squares organization */
    "L.S.",
    "CGLS",
    "BLOC",
    "DAMP",
    "STIR",
    "WGHT",
    "FVAR",
    /* 7.7 Lists and tables */
    "BOND",
    "CONF",
    "MPLA",
    "RTAB",
    "HTAB",
    "LIST",
    "ACTA",
    "SIZE",
    "TEMP",
    "WPDB",
    /* 7.8 Fouriers, peak search and lineprinter plots */
    "FMAP",
    "GRID",
    "PLAN",
    "MOLE",

    // "Disrgarding line assumed to be added by PLATON: " + line);
    "    "
  };
}
