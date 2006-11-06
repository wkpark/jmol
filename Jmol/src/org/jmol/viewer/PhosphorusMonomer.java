/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import javax.vecmath.Point3f;

class PhosphorusMonomer extends Monomer {

  final static byte[] phosphorusOffsets = { 0 };

  static float MAX_ADJACENT_PHOSPHORUS_DISTANCE = 7.5f;
 
  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstIndex, int lastIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {
    //Logger.debug("PhosphorusMonomer.validateAndAllocate");
    if (firstIndex != lastIndex ||
        specialAtomIndexes[JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS]
        != firstIndex)
      return null;
    return new PhosphorusMonomer(chain, group3, seqcode,
                            firstIndex, lastIndex, phosphorusOffsets);
  }
  
  ////////////////////////////////////////////////////////////////

  PhosphorusMonomer(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
    if (group3.indexOf('T') >= 0)
      chain.isDna = true;
    if (group3.indexOf('U') >= 0)
      chain.isRna = true;
  }

  boolean isPhosphorusMonomer() { return true; }

  boolean isDna() { return chain.isDna(); }

  boolean isRna() { return chain.isRna(); }


  byte getProteinStructureType() {
    return 0;
  }

  Atom getAtom(byte specialAtomID) {
    return (specialAtomID == JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS
            ? getLeadAtom()
            : null);
  }

  Point3f getAtomPoint(byte specialAtomID) {
    return (specialAtomID == JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS
            ? getLeadAtomPoint()
            : null);
  }

  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    if (! (possiblyPreviousMonomer instanceof PhosphorusMonomer))
      return false;
    // 1PN8 73:d and 74:d are 7.001 angstroms apart
    // but some P atoms are up to 7.4 angstroms apart
    float distance =
      getLeadAtomPoint().distance(possiblyPreviousMonomer.getLeadAtomPoint());
    return distance <= MAX_ADJACENT_PHOSPHORUS_DISTANCE;
  }
}
