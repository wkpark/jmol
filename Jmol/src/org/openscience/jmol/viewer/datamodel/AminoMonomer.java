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

public class AminoMonomer extends Monomer {
  // I don't think that this should extend AlphaMonomer ...
  // ... too much risk of conflict

  static Monomer
    validateAndAllocate(Chain chain, String group3,
                        int sequenceNumber, char insertionCode,
                        Atom[] atoms,
                        int firstAtomIndex, int lastAtomIndex) {
    int aminoNitrogenIndex = Integer.MIN_VALUE;
    int alphaCarbonIndex = Integer.MIN_VALUE;
    int carbonylCarbonIndex = Integer.MIN_VALUE;
    int carbonylOxygenIndex = Integer.MIN_VALUE;
    int terminatingOxtIndex = Integer.MIN_VALUE;

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
    for (int i = firstAtomIndex; i <= lastAtomIndex; ++i)
      aminoMonomer.registerAtom(atoms[i]);
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
    this.aminoNitrogenOffset = (byte)(aminoNitrogenIndex - firstAtomIndex);
    this.alphaCarbonOffset = (byte)(alphaCarbonIndex - firstAtomIndex);
    this.carbonylCarbonOffset = (byte)(carbonylCarbonIndex - firstAtomIndex);
    this.carbonylOxygenOffset = (byte)(carbonylOxygenIndex - firstAtomIndex);
    this.terminatingOxtOffset =
      (byte)(terminatingOxtIndex < 0
             ? 255 : terminatingOxtIndex - firstAtomIndex);
  }

  byte aminoNitrogenOffset;
  byte alphaCarbonOffset;
  byte carbonylCarbonOffset;
  byte carbonylOxygenOffset;
  byte terminatingOxtOffset;
  
  boolean isAminoMonomer() { return true; }
  
  int getLeadAtomIndex()  {
    //    System.out.println("AminoMonomer.getLeadAtomIndex()");
    return firstAtomIndex + (alphaCarbonOffset & 0xFF);
  }
  
  int getWingAtomIndex() {
    //    System.out.println("AminoMonomer.getWingAtomIndex()");
    return firstAtomIndex + (carbonylOxygenOffset & 0xFF);
  }
}
