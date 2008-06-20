/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-12-11 13:29:38 -0600 (Mon, 11 Dec 2006) $
 * $Revision: 6442 $
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
package org.jmol.modelsetbio;

import javax.vecmath.Point3f;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Chain;
import org.jmol.viewer.JmolConstants;

public class AminoMonomer extends AlphaMonomer {

  private final static byte CA = 0;
  private final static byte O = 1;
  private final static byte N = 2;
  private final static byte C = 3;
  private final static byte OT = 4;
  //private final static byte O1 = 5;
  
  // negative values are optional
  final static byte[] interestingAminoAtomIDs = {
    JmolConstants.ATOMID_ALPHA_CARBON,      // 0 CA alpha carbon
    ~JmolConstants.ATOMID_CARBONYL_OXYGEN,   // 1 O wing man
    JmolConstants.ATOMID_AMINO_NITROGEN,    // 2 N
    JmolConstants.ATOMID_CARBONYL_CARBON,   // 3 C  point man
    ~JmolConstants.ATOMID_TERMINATING_OXT,  // 4 OXT
    ~JmolConstants.ATOMID_O1,               // 5 O1
  };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstAtomIndex, int lastAtomIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {
    byte[] offsets = scanForOffsets(firstAtomIndex, specialAtomIndexes,
                                    interestingAminoAtomIDs);
    if (offsets == null)
      return null;
    checkOptional(offsets, O, firstAtomIndex, specialAtomIndexes[JmolConstants.ATOMID_O1]);
    if (atoms[firstAtomIndex].isHetero() && !isBondedCorrectly(firstAtomIndex, offsets, atoms)) 
      return null;
    AminoMonomer aminoMonomer =
      new AminoMonomer(chain, group3, seqcode,
                       firstAtomIndex, lastAtomIndex, offsets);
    return aminoMonomer;
  }

  private static boolean isBondedCorrectly(int offset1, int offset2,
                                   int firstAtomIndex,
                                   byte[] offsets, Atom[] atoms) {
    int atomIndex1 = firstAtomIndex + (offsets[offset1] & 0xFF);
    int atomIndex2 = firstAtomIndex + (offsets[offset2] & 0xFF);
    if (atomIndex1 >= atomIndex2)
      return false;
    return atoms[atomIndex1].isBonded(atoms[atomIndex2]);
  }

  private static boolean isBondedCorrectly(int firstAtomIndex, byte[] offsets,
                                 Atom[] atoms) {
    return (isBondedCorrectly(N, CA, firstAtomIndex, offsets, atoms)
            && isBondedCorrectly(CA, C, firstAtomIndex, offsets, atoms)
            && (offsets[O] == -1 
                || isBondedCorrectly(C, O, firstAtomIndex, offsets, atoms))
            );
  }
  
  ////////////////////////////////////////////////////////////////

  private AminoMonomer(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
  }

  boolean isAminoMonomer() { return true; }

  Atom getNitrogenAtom() {
    return getAtomFromOffsetIndex(N);
  }

  Point3f getNitrogenAtomPoint() {
    return getAtomFromOffsetIndex(N);
  }

  Atom getCarbonylCarbonAtom() {
    return getAtomFromOffsetIndex(C);
  }

  Point3f getCarbonylCarbonAtomPoint() {
    return getAtomFromOffsetIndex(C);
  }

  Atom getCarbonylOxygenAtom() {
    return getWingAtom();
  }

  Point3f getCarbonylOxygenAtomPoint() {
    return getWingAtomPoint();
  }

  Atom getInitiatorAtom() {
    return getNitrogenAtom();
  }

  Atom getTerminatorAtom() {
    return getAtomFromOffsetIndex(offsets[OT] != -1 ? OT : C);
  }

  boolean hasOAtom() {
    return offsets[O] != -1;
  }
  
  ////////////////////////////////////////////////////////////////

  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    if (! (possiblyPreviousMonomer instanceof AminoMonomer))
      return false;
    AminoMonomer other = (AminoMonomer)possiblyPreviousMonomer;
    return other.getCarbonylCarbonAtom().isBonded(getNitrogenAtom());
  }

  ////////////////////////////////////////////////////////////////

  void findNearestAtomIndex(int x, int y, Atom[] closest,
                            short madBegin, short madEnd) {
    
    Atom competitor = closest[0];
    Atom nitrogen = getNitrogenAtom();
    short marBegin = (short) (madBegin / 2);
    if (marBegin < 1200)
      marBegin = 1200;
    if (nitrogen.screenZ == 0)
      return;
    int radiusBegin = scaleToScreen(nitrogen.screenZ, marBegin);
    if (radiusBegin < 4)
      radiusBegin = 4;
    Atom ccarbon = getCarbonylCarbonAtom();
    short marEnd = (short) (madEnd / 2);
    if (marEnd < 1200)
      marEnd = 1200;
    int radiusEnd = scaleToScreen(nitrogen.screenZ, marEnd);
    if (radiusEnd < 4)
      radiusEnd = 4;
    Atom alpha = getLeadAtom();
    if (isCursorOnTopOf(alpha, x, y, (radiusBegin + radiusEnd) / 2,
        competitor)
        || isCursorOnTopOf(nitrogen, x, y, radiusBegin, competitor)
        || isCursorOnTopOf(ccarbon, x, y, radiusEnd, competitor))
      closest[0] = alpha;
  }

  void setNitrogenHydrogenPoint(Point3f aminoHydrogenPoint) {
    nitrogenHydrogenPoint = new Point3f(aminoHydrogenPoint);
  }
  
  Point3f getNitrogenHydrogenPoint() {
    return nitrogenHydrogenPoint;
  }
}
