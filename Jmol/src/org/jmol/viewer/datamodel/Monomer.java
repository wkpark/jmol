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
package org.jmol.viewer.datamodel;

import org.jmol.viewer.datamodel.Atom;
import javax.vecmath.Point3f;

abstract class Monomer extends Group {

  Polymer polymer;

  final byte[] offsets;

  Monomer(Chain chain, String group3, int seqcode,
          int firstAtomIndex, int lastAtomIndex,
          byte[] interestingAtomOffsets) {
    super(chain, group3, seqcode, firstAtomIndex, lastAtomIndex);
    offsets = interestingAtomOffsets;
  }

  void setPolymer(Polymer polymer) {
    this.polymer = polymer;
  }

  public int getPolymerLength() {
    return polymer == null ? 0 : polymer.count;
  }

  ////////////////////////////////////////////////////////////////

  static byte[] scanForOffsets(int firstAtomIndex,
                               int[] specialAtomIndexes,
                               byte[] interestingAtomIDs) {
    int interestingCount = interestingAtomIDs.length;
    byte[] offsets = new byte[interestingCount];
    for (int i = interestingCount; --i >= 0; ) {
      int atomIndex;
      int atomID = interestingAtomIDs[i];
      // mth 2004 06 09
      // use ~ instead of - as the optional indicator
      // because I got hosed by a missing comma
      // in an interestingAtomIDs table
      if (atomID < 0) {
        atomIndex = specialAtomIndexes[~atomID]; // optional
      } else {
        atomIndex = specialAtomIndexes[atomID];  // required
        if (atomIndex < 0)
          return null;
      }
      int offset;
      if (atomIndex < 0)
        offset = 255;
      else {
        offset = atomIndex - firstAtomIndex;
        if (offset < 0 || offset > 254)
          throw new NullPointerException();
      }
      offsets[i] = (byte)offset;
    }
    return offsets;
  }

  ////////////////////////////////////////////////////////////////

  public boolean isDna() { return false; }
  public boolean isRna() { return false; }
  final public boolean isProtein() {return this instanceof AlphaMonomer;}
  final public boolean isNucleic() {return this instanceof PhosphorusMonomer;}

  ////////////////////////////////////////////////////////////////

  void setStructure(ProteinStructure proteinstructure) { }
  ProteinStructure getProteinStructure() { return null; }
  byte getProteinStructureType() { return 0; }
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

  final Atom getSpecialAtom(byte[] interestingIDs, byte specialAtomID) {
    for (int i = interestingIDs.length; --i >= 0; ) {
      int interestingID = interestingIDs[i];
      if (interestingID < 0)
        interestingID = -interestingID;
      if (specialAtomID == interestingID) {
        int offset = offsets[i] & 0xFF;
        if (offset == 255)
          return null;
        return chain.frame.atoms[firstAtomIndex + offset];
      }
    }
    return null;
  }

  final Point3f getSpecialAtomPoint(byte[] interestingIDs,
                                    byte specialAtomID) {
    for (int i = interestingIDs.length; --i >= 0; ) {
      int interestingID = interestingIDs[i];
      if (interestingID < 0)
        interestingID = -interestingID;
      if (specialAtomID == interestingID) {
        int offset = offsets[i] & 0xFF;
        if (offset == 255)
          return null;
        return chain.frame.atoms[firstAtomIndex + offset].point3f;
      }
    }
    return null;
  }

  Atom getAtom(byte specialAtomID) { return null; }

  Point3f getAtomPoint(byte specialAtomID) { return null; }

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

  Atom getInitiatorAtom() {
    return getLeadAtom();
  }
  
  Atom getTerminatorAtom() {
    return getLeadAtom();
  }

  abstract boolean isConnectedAfter(Monomer possiblyPreviousMonomer);

  /**
   * Selects LeadAtom when this Monomer is clicked iff it is
   * closer to the user.
   * 
   * @param x
   * @param y
   * @param closest
   * @param madBegin
   * @param madEnd
   */
  void findNearestAtomIndex(int x, int y, Closest closest,
                            short madBegin, short madEnd) {
  }
}
