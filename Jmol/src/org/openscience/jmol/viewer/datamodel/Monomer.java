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
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.datamodel.Frame;
import java.util.Hashtable;
import java.util.BitSet;
import javax.vecmath.Point3f;

abstract class Monomer extends Group {

  Polymer polymer;

  final byte[] offsets;

  Monomer(Chain chain, String group3,
          int sequenceNumber, char insertionCode,
          int firstAtomIndex, int lastAtomIndex,
          int interestingOffsetCount) {
    super(chain, group3, sequenceNumber, insertionCode,
          firstAtomIndex, lastAtomIndex);
    offsets = new byte[interestingOffsetCount];
    for (int i = interestingOffsetCount; --i >= 0; )
      offsets[i] = -1;
  }

  void setPolymer(Polymer polymer) {
    this.polymer = polymer;
  }

  ////////////////////////////////////////////////////////////////

  boolean isAminoMonomer() { return false; }
  boolean isAlphaMonomer() { return false; }
  boolean isNucleicMonomer() { return false; }
  public boolean isDna() { return false; }
  public boolean isRna() { return false; }
  final public boolean isProtein() { return isAlphaMonomer(); }
  final public boolean isNucleic() { return isNucleicMonomer(); }

  ////////////////////////////////////////////////////////////////

  void setStructure(ProteinStructure proteinstructure) { };
  ProteinStructure getProteinStructure() { return null; };
  byte getProteinStructureType() { return 0; };
  boolean isHelix() { return false; }
  boolean isHelixOrSheet() { return false; }

  ////////////////////////////////////////////////////////////////

  final Atom getAtomFromOffset(byte offset) {
    if (offset == -1)
      return null;
    return chain.frame.atoms[firstAtomIndex + (offset & 0xFF)];
  }

  final Point3f getAtomPointFromOffset(byte offset) {
    if (offset == -1)
      return null;
    return chain.frame.atoms[firstAtomIndex + (offset & 0xFF)].point3f;
  }

  ////////////////////////////////////////////////////////////////

  final Atom getAtomFromOffsetIndex(int offsetIndex) {
    if (offsetIndex > offsets.length)
      return null;
    int offset = offsets[offsetIndex] & 0xFF;
    if (offset == 255)
      return null;
    return chain.frame.atoms[firstAtomIndex + offset];
  }

  final Point3f getAtomPointFromOffsetIndex(int offsetIndex) {
    if (offsetIndex > offsets.length)
      return null;
    int offset = offsets[offsetIndex] & 0xFF;
    if (offset == 255)
      return null;
    return chain.frame.atoms[firstAtomIndex + offset].point3f;
  }

  final int getLeadAtomIndex() {
    return firstAtomIndex + (offsets[0] & 0xFF);
  }

  final Atom getLeadAtom() {
    return getAtomFromOffsetIndex(0);
  }

  final Point3f getLeadAtomPoint() {
    return getAtomPointFromOffsetIndex(0);
  }

  final Atom getWingAtom() {
    return getAtomFromOffsetIndex(1);
  }

  final Point3f getWingAtomPoint() {
    return getAtomPointFromOffsetIndex(1);
  }

  ////////////////////////////////////////////////////////////////
  // try to get rid of these 

  Atom getNucleotideAtomID(int atomid) {
    return null;
    /*
    if (atomid < JmolConstants.ATOMID_NUCLEOTIDE_MIN ||
        atomid >= JmolConstants.ATOMID_NUCLEOTIDE_MAX) {
      System.out.println("getNucleotideAtomID out of bounds");
      return null;
    }
    int index = atomid - JmolConstants.ATOMID_NUCLEOTIDE_MIN;
    Atom atom = getAtomIndex(nucleotideIndices[index]);
    if (atom == null)
      System.out.println("getNucleotideAtomID(" + atomid + ") -> null ?" +
                         " nucleotideIndices[" + index + "]=" +
                         nucleotideIndices[index]);
    return atom;
    */
  }

  Atom getPurineN1() {
    Atom n1 = ((groupID >= JmolConstants.GROUPID_PURINE_MIN &&
                groupID <= JmolConstants.GROUPID_PURINE_LAST)
               ? getNucleotideAtomID(JmolConstants.ATOMID_N1)
               : null);
    return n1;
  }

  Atom getPyrimidineN3() {
    return ((groupID >= JmolConstants.GROUPID_PYRIMIDINE_MIN &&
             groupID <= JmolConstants.GROUPID_PYRIMIDINE_LAST)
            ? getNucleotideAtomID(JmolConstants.ATOMID_N3)
            : null);
  }
            
  boolean isGuanine() {
    //    "@g _g=25,_g=26,_g>=39 & _g<=45,_g>=54 & _g<=56",
    return (groupID == JmolConstants.GROUPID_GUANINE ||
            groupID == JmolConstants.GROUPID_PLUS_GUANINE ||
            (groupID >= JmolConstants.GROUPID_GUANINE_1_MIN &&
             groupID <= JmolConstants.GROUPID_GUANINE_1_LAST) ||
            (groupID >= JmolConstants.GROUPID_GUANINE_2_MIN &&
             groupID <= JmolConstants.GROUPID_GUANINE_2_LAST));
  }
}
