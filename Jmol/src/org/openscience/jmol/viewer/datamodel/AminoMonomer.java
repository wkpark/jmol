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

  static Monomer
    validateAndAllocate(Chain chain, String group3,
                        int sequenceNumber, char insertionCode,
                        int distinguishingBits, Atom[] atoms,
                        int firstAtomIndex, int lastAtomIndex) {
    int aminoNitrogenIndex = -1;
    int alphaCarbonIndex = -1;
    int carbonylCarbonIndex = -1;
    int carbonylOxygenIndex = -1;
    int terminatingOxtIndex = -1;

    for (int i = firstAtomIndex; i <= lastAtomIndex; ++i) {
      Atom atom = atoms[i];
      switch (atoms[i].specialAtomID) {
      case JmolConstants.ATOMID_AMINO_NITROGEN:
        if (aminoNitrogenIndex < 0) {
          aminoNitrogenIndex = i;
          continue;
        }
        break;
      case JmolConstants.ATOMID_ALPHA_CARBON:
        if (alphaCarbonIndex < 0) {
          alphaCarbonIndex = i;
          continue;
        }
        break;
      case JmolConstants.ATOMID_CARBONYL_CARBON:
        if (carbonylCarbonIndex < 0) {
          carbonylCarbonIndex = i;
          continue;
        }
        break;
      case JmolConstants.ATOMID_CARBONYL_OXYGEN:
        if (carbonylOxygenIndex < 0) {
          carbonylOxygenIndex = i;
          continue;
        }
        break;
      case JmolConstants.ATOMID_TERMINATING_OXT:
        if (terminatingOxtIndex < 0) {
          terminatingOxtIndex = i;
          continue;
        }
        break;
      default:
        continue;
      }
      atoms[i].specialAtomID = 0; // reset all imposters
    }

    // don't worry about the binary | operator
    // this is just testing if anybody is less than 0
    if ((aminoNitrogenIndex | alphaCarbonIndex |
         carbonylCarbonIndex | carbonylOxygenIndex) < 0)
      throw new NullPointerException();

    AminoMonomer aminoMonomer =
      new AminoMonomer(chain, group3, sequenceNumber, insertionCode,
                       firstAtomIndex, lastAtomIndex,
                       aminoNitrogenIndex, alphaCarbonIndex,
                       carbonylCarbonIndex, carbonylOxygenIndex,
                       terminatingOxtIndex);
    return aminoMonomer;
  }
  
  
  AminoMonomer(Chain chain, String group3,
               int sequenceNumber, char insertionCode,
               int firstAtomIndex, int lastAtomIndex,
               int aminoNitrogenIndex, int alphaCarbonIndex,
               int carbonylCarbonIndex, int carbonylOxygenIndex,
               int terminatingOxtIndex) {
    super(chain, group3, sequenceNumber, insertionCode,
          firstAtomIndex, lastAtomIndex);
    this.leadAtomOffset = (byte)(alphaCarbonIndex - firstAtomIndex);
    this.wingAtomOffset = (byte)(carbonylOxygenIndex - firstAtomIndex);

    this.aminoNitrogenOffset = (byte)(aminoNitrogenIndex - firstAtomIndex);
    this.carbonylCarbonOffset = (byte)(carbonylCarbonIndex - firstAtomIndex);
    this.terminatingOxtOffset =
      (byte)(terminatingOxtIndex < 0
             ? -1 : terminatingOxtIndex - firstAtomIndex);
  }

  byte aminoNitrogenOffset;
  byte carbonylCarbonOffset;
  byte terminatingOxtOffset;
  
  boolean isAminoMonomer() { return true; }

  Atom getNitrogenAtom() {
    return getAtomFromOffset(aminoNitrogenOffset);
  }

  Point3f getNitrogenAtomPoint() {
    return getAtomPointFromOffset(aminoNitrogenOffset);
  }

  Point3f getCarbonylCarbonAtomPoint() {
    return getAtomPointFromOffset(carbonylCarbonOffset);
  }

  Atom getCarbonylOxygenAtom() {
    return getWingAtom();
  }

  Point3f getCarbonylOxygenAtomPoint() {
    return getWingAtomPoint();
  }
}
