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
import javax.vecmath.Point3f;

public class NucleicMonomer extends Monomer {

  // negative values are optional
  final static byte[] interestingNucleicAtomIDs = {
    -JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS,    // 0 P  the lead, phosphorus
    JmolConstants.ATOMID_NUCLEIC_WING,           // 1 the wing man, c6

    -JmolConstants.ATOMID_RNA_O2PRIME, // 2  O2' for RNA

    JmolConstants.ATOMID_N1,   //  3 N1
    JmolConstants.ATOMID_C2,   //  4 C2
    JmolConstants.ATOMID_N3,   //  5 N3
    JmolConstants.ATOMID_C4,   //  6 C4
    JmolConstants.ATOMID_C5,   //  7 C5
    JmolConstants.ATOMID_C6,   //  8 C6

    -JmolConstants.ATOMID_O2,  //  9 O2

    -JmolConstants.ATOMID_N7,  // 10 N7
    -JmolConstants.ATOMID_C8,  // 11 C8
    -JmolConstants.ATOMID_N9,  // 12 C9

    -JmolConstants.ATOMID_O4,  // 13 O4   U (& ! C5M)
    -JmolConstants.ATOMID_O6,  // 14 O6   I (& ! N2)
    -JmolConstants.ATOMID_N4,  // 15 N4   C
    -JmolConstants.ATOMID_C5M, // 16 C5M  T
    -JmolConstants.ATOMID_N6,  // 17 N6   A
    -JmolConstants.ATOMID_N2,  // 18 N2   G
    -JmolConstants.ATOMID_S4,  // 19 S4   tU

    -JmolConstants.ATOMID_H5T_TERMINUS, // 20 H5T terminus
  };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstAtomIndex, int lastAtomIndex,
                        int[] specialAtomIndexes) {

    byte[] offsets = scanForOffsets(firstAtomIndex,
                                    specialAtomIndexes,
                                    interestingNucleicAtomIDs);

    if (offsets == null || (offsets[0] == -1 && offsets[20] == -1))
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
    if (offsets[0] == -1)
      offsets[0] = offsets[20];
    this.hasRnaO2Prime = offsets[2] != -1;
    this.isPyrimidine = offsets[9] != -1;
    this.isPurine =
      offsets[10] != -1 && offsets[11] != -1 && offsets[12] != -1;
    /*
    System.out.println("NucleicMonomer(" + this + ")" +
                       " hasRnaO2Prime=" + hasRnaO2Prime +
                       " isPyrimidine=" + isPyrimidine +
                       " isPurine=" + isPurine +
                       " offsets[3]=" + offsets[3]);
    */
  }

  boolean hasRnaO2Prime;
  boolean isPurine;
  boolean isPyrimidine;

  boolean isNucleicMonomer() { return true; }

  public boolean isDna() { return ! hasRnaO2Prime; }

  public boolean isRna() { return hasRnaO2Prime; }

  public boolean isPurine() { return isPurine; }

  public boolean isPyrimidine() { return isPyrimidine; }

  boolean isGuanine() { return offsets[18] != -1; }

  byte getProteinStructureType() {
    return (hasRnaO2Prime
            ? JmolConstants.PROTEIN_STRUCTURE_RNA
            : JmolConstants.PROTEIN_STRUCTURE_DNA);
  }

  ////////////////////////////////////////////////////////////////

  Atom getN1() {
    return getAtomFromOffsetIndex(3);
  }

  Atom getN3() {
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

  Atom getAtom(byte specialAtomID) {
    return getSpecialAtom(interestingNucleicAtomIDs, specialAtomID);
  }

  Point3f getAtomPoint(byte specialAtomID) {
    return getSpecialAtomPoint(interestingNucleicAtomIDs, specialAtomID);
  }

  void getBaseRing6Points(Point3f[] ring6Points) {
    for (int i = 6; --i >= 0; )
      ring6Points[i] = getAtomPointFromOffsetIndex(i + 3);
  }

  final static byte[] ring5OffsetIndexes = {6, 7, 10, 11, 12};

  boolean maybeGetBaseRing5Points(Point3f[] ring5Points) {
    if (isPurine)
      for (int i = 5; --i >= 0; )
        ring5Points[i] = getAtomPointFromOffsetIndex(ring5OffsetIndexes[i]);
    return isPurine;
  }
}
