/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002  The Jmol Development Team
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
package org.openscience.jmol;

import java.util.Hashtable;

public class ProteinProp {

  // FIXME mth -- a very quick/dirty/ugly implementation
  // just to get some complex queries running
  public String recordPdb;
  byte resid;
  byte atomID;

  public ProteinProp(String recordPdb) {
    this.recordPdb = recordPdb;

    Integer resInt = (Integer)htResidue.get(recordPdb.substring(17, 20));
    resid = (resInt != null) ? (byte)resInt.intValue() : -1;
    Integer atomInt = (Integer)htAtom.get(getName());
    atomID = (atomInt != null) ? (byte)atomInt.intValue() : -1;
  }

  public boolean isHetero() {
    return recordPdb.startsWith("HETATM");
  }

  public boolean isResidue(String residue) {
    return recordPdb.regionMatches(true, 17, residue, 0, 3);
  }

  public String getName () {
    return recordPdb.substring(12, 16).trim();
  }

  public String getResidue() {
    return resid < 0 ? "???" : residues[resid];
  }

  public byte getResID() {
    return resid;
  }

  public byte getAtomID() {
    return atomID;
  }

  public String getAtomName() {
    if (atomID > -1)
      return atomNames[atomID];
    return getName();
  }

  public boolean isResidueNameMatch(String strWildcard) {
    if (strWildcard.length() != 3) {
      System.err.println("residue wildcard length != 3");
      return false;
    }
    String strResidue = getResidue();
    for (int i = 0; i < 3; ++i) {
      char charWild = strWildcard.charAt(i);
      if (charWild == '?')
        continue;
      if (Character.toUpperCase(charWild) != strResidue.charAt(i))
        return false;
    }
    return true;
  }

  public int getResno() {
    int chain = 0;
    try {
      chain = Integer.parseInt(recordPdb.substring(22, 26).trim());
    } catch (NumberFormatException e) {
      System.out.println("Resno is not an integer:" + recordPdb);
    }
    return chain;
  }

  public int getTemperature() {
    double temp = 0;
    try {
      temp = Double.parseDouble(recordPdb.substring(60, 66).trim());
    } catch (NumberFormatException e) {
      System.out.println("temp is not a decimal:" + recordPdb);
    }
    return (int)(temp * 100);
  }

  public char getChain() {
    return recordPdb.charAt(21);
  }

  static String[] residues = {
    // tabel taken from rasmol source molecule.h
          "ALA", /* 8.4% */     "GLY", /* 8.3% */
          "LEU", /* 8.0% */     "SER", /* 7.5% */
          "VAL", /* 7.1% */     "THR", /* 6.4% */
          "LYS", /* 5.8% */     "ASP", /* 5.5% */
          "ILE", /* 5.2% */     "ASN", /* 4.9% */
          "GLU", /* 4.9% */     "PRO", /* 4.4% */
          "ARG", /* 3.8% */     "PHE", /* 3.7% */
          "GLN", /* 3.5% */     "TYR", /* 3.5% */
          "HIS", /* 2.3% */     "CYS", /* 2.0% */
          "MET", /* 1.8% */     "TRP", /* 1.4% */

          "ASX", "GLX", "PCA", "HYP",

    /*===================*/
    /*  DNA Nucleotides  */
    /*===================*/
          "  A", "  C", "  G", "  T",

    /*===================*/
    /*  RNA Nucleotides  */
    /*===================*/
          "  U", " +U", "  I", "1MA", 
          "5MC", "OMC", "1MG", "2MG", 
          "M2G", "7MG", "OMG", " YG", 
          "H2U", "5MU", "PSU",

    /*=================*/
    /*  Miscellaneous  */ 
    /*=================*/
          "UNK", "ACE", "FOR", "HOH",
          "DOD", "SO4", "PO4", "NAD",
          "COA", "NAP", "NDP"  };

  private static Hashtable htResidue = new Hashtable();
  static {
    for (int i = 0; i < residues.length; ++i) {
      htResidue.put(residues[i], new Integer(i));
    }
  }

  public final static int ATOM_BACKBONE_MIN =  0;
  public final static int ATOM_BACKBONE_MAX =  3;
  public final static int ATOM_SHAPELY_MAX  =  7;
  public final static int ATOM_NUCLEIC_BACKBONE_MIN =  7;
  public final static int ATOM_NICLEIC_BACKBONE_MAX = 18;

  static String[] atomNames = {
    "N",   // 0
    "CA",
    "C",
    "O",   // 3
    "C'",  // 4
    "OT",
    "S",
    "P",   // 7
    "O1P",
    "O2P",
    "O5*",
    "C5*",
    "C4*",
    "O4*",
    "C3*",
    "O3*",
    "C2*",
    "O2*",
    "C1*",
    "CA2",
    "SG",
    "N1",
    "N2",
    "N3",
    "N4",
    "N6",
    "O2",
    "O4",
    "O6"
  };
  
  private static Hashtable htAtom = new Hashtable();
  static {
    for (int i = 0; i < atomNames.length; ++i) {
      htAtom.put(atomNames[i], new Integer(i));
    }
  }
}
