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
package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;

public class NucleicMonomer extends Monomer {

  final static byte[] interestingNucleicAtomIDs = {
    JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS,
    JmolConstants.ATOMID_NUCLEIC_WING,

    JmolConstants.ATOMID_RNA_O2PRIME,

    JmolConstants.ATOMID_N1,
    JmolConstants.ATOMID_C2,
    JmolConstants.ATOMID_N3,
    JmolConstants.ATOMID_C4,
    JmolConstants.ATOMID_C5,
    JmolConstants.ATOMID_C6,

    JmolConstants.ATOMID_O2,

    JmolConstants.ATOMID_N7,
    JmolConstants.ATOMID_C8,
    JmolConstants.ATOMID_N9,

    JmolConstants.ATOMID_O4,  // U & ! C5M
    JmolConstants.ATOMID_O6,  // I & ! N2
    JmolConstants.ATOMID_N4,  // C
    JmolConstants.ATOMID_C5M, // T
    JmolConstants.ATOMID_N6,  // A
    JmolConstants.ATOMID_N2,  // G
    JmolConstants.ATOMID_S4,  // tU
  };

  final static boolean[] requiredNucleicAtomIDs = {
    true,  //  0 - P
    true,  //  1 - whatever the Wing is ... currently C6

    false, //  2 - O2' - for RNA

    true,  //  3 - N1
    true,  //  4 - C2,
    true,  //  5 - N3,
    true,  //  6 - C4,
    true,  //  7 - C5,
    true,  //  8 - C6,

    false, //  9 - O2 for pyrimidines

    false, // 10 - N7 for purines
    false, // 11 - C8 for purines
    false, // 12 - N9 for purines

    false, // 13 JmolConstants.ATOMID_O4,  // U & ! C5M
    false, // 14 JmolConstants.ATOMID_O6,  // I & ! N2
    false, // 15 JmolConstants.ATOMID_N4,  // C
    false, // 16 JmolConstants.ATOMID_C5M, // T
    false, // 17 JmolConstants.ATOMID_N6,  // A
    false, // 18 JmolConstants.ATOMID_N2,  // G
    false, // 19 JmolConstants.ATOMID_S4,  // tU
  };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstAtomIndex, int lastAtomIndex,
                        int[] specialAtomIndexes) {

    byte[] offsets = scanForOffsets(firstAtomIndex,
                                    specialAtomIndexes,
                                    interestingNucleicAtomIDs,
                                    requiredNucleicAtomIDs);
    if (offsets == null)
      return null;
    NucleicMonomer nucleicMonomer =
      new NucleicMonomer(chain, group3, seqcode,
                         firstAtomIndex, lastAtomIndex, offsets);
    return nucleicMonomer;
  }

  ////////////////////////////////////////////////////////////////

  NucleicMonomer(Chain chain, String group3, int seqcode,
                 int firstAtomIndex, int lastAtomIndex,
                 byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
    this.hasRnaO2Prime = offsets[2] != -1;
    this.isPyrimidine = offsets[9] != -1;
    this.isPurine =
      offsets[10] != -1 && offsets[11] != -1 && offsets[12] != -1;
  }

  boolean hasRnaO2Prime;
  boolean isPurine;
  boolean isPyrimidine;

  boolean isNucleicMonomer() { return true; }

  public boolean isDna() { return ! hasRnaO2Prime; }

  public boolean isRna() { return hasRnaO2Prime; }

  boolean isPurine() { return isPurine; }

  boolean isPyrimidine() { return isPyrimidine; }

  boolean isGuanine() { return offsets[18] != -1; }

  byte getProteinStructureType() {
    return (hasRnaO2Prime
            ? JmolConstants.PROTEIN_STRUCTURE_RNA
            : JmolConstants.PROTEIN_STRUCTURE_DNA);
  }

  ////////////////////////////////////////////////////////////////

  Atom getPurineN1() {
    if (! isPurine())
      return null;
    return getAtomFromOffsetIndex(9);
  }

  Atom getPyrimidineN3() {
    if (! isPyrimidine())
      return null;
    return getAtomFromOffsetIndex(5);
  }

  Atom getN2() {
    return getAtomFromOffsetIndex(18);
  }

  Atom getO2() {
    return getAtomFromOffsetIndex(9);
  }

  Atom getO6() {
    return getAtomFromOffsetIndex(14);
  }

  Atom getN4() {
    return getAtomFromOffsetIndex(15);
  }

  Atom getN6() {
    return getAtomFromOffsetIndex(17);
  }

  Atom getO4() {
    return getAtomFromOffsetIndex(13);
  }
}
