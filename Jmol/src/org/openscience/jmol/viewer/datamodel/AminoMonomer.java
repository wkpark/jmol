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

public class AminoMonomer extends AlphaMonomer {

  // negative values are optional
  final static byte[] interestingAminoAtomIDs = {
    JmolConstants.ATOMID_ALPHA_CARBON,      // 0 CA alpha carbon
    JmolConstants.ATOMID_CARBONYL_OXYGEN,   // 1 O wing man
    JmolConstants.ATOMID_AMINO_NITROGEN,    // 2 N
    JmolConstants.ATOMID_CARBONYL_CARBON,   // 3 C
    -JmolConstants.ATOMID_TERMINATING_OXT,  // 4 OXT
  };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstAtomIndex, int lastAtomIndex,
                        int[] specialAtomIndexes) {
    byte[] offsets = scanForOffsets(firstAtomIndex, specialAtomIndexes,
                                    interestingAminoAtomIDs);
    if (offsets == null)
      return null;
    AminoMonomer aminoMonomer =
      new AminoMonomer(chain, group3, seqcode,
                       firstAtomIndex, lastAtomIndex, offsets);
    return aminoMonomer;
  }
  
  ////////////////////////////////////////////////////////////////

  AminoMonomer(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
  }

  boolean isAminoMonomer() { return true; }

  Atom getNitrogenAtom() {
    return getAtomFromOffsetIndex(2);
  }

  Point3f getNitrogenAtomPoint() {
    return getAtomPointFromOffsetIndex(2);
  }

  Atom getCarbonylCarbonAtom() {
    return getAtomFromOffsetIndex(3);
  }

  Point3f getCarbonylCarbonAtomPoint() {
    return getAtomPointFromOffsetIndex(3);
  }

  Atom getCarbonylOxygenAtom() {
    return getWingAtom();
  }

  Point3f getCarbonylOxygenAtomPoint() {
    return getWingAtomPoint();
  }

  ////////////////////////////////////////////////////////////////

  Atom getAtom(byte specialAtomID) {
    return getSpecialAtom(interestingAminoAtomIDs, specialAtomID);
  }

  Point3f getAtomPoint(byte specialAtomID) {
    return getSpecialAtomPoint(interestingAminoAtomIDs, specialAtomID);
  }

  ////////////////////////////////////////////////////////////////

  void findNearestAtomIndex(int x, int y, Closest closest) {
    Atom competitor = closest.atom;
    Atom alpha = getLeadAtom();
    if (alpha.isCursorOnTop(x, y, 8, competitor) ||
        getNitrogenAtom().isCursorOnTop(x, y, 8, competitor) ||
        getCarbonylCarbonAtom().isCursorOnTop(x, y, 8, competitor))
      closest.atom = alpha;
  }
}
