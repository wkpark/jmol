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

public class AlphaMonomer extends Monomer {

  static Monomer
    validateAndAllocate(Chain chain, String group3,
                        int sequenceNumber, char insertionCode,
                        int distinguishingBits, Atom[] atoms,
                        int firstAtomIndex, int lastAtomIndex) {
    if (firstAtomIndex != lastAtomIndex)
      return null;
    if (atoms[firstAtomIndex].specialAtomID !=
        JmolConstants.ATOMID_ALPHA_CARBON) // better not happen
      throw new NullPointerException();
    return new AlphaMonomer(chain, group3, sequenceNumber, insertionCode,
                            firstAtomIndex, lastAtomIndex);
  }
  
  
  AlphaMonomer(Chain chain, String group3,
               int sequenceNumber, char insertionCode,
               int firstAtomIndex, int lastAtomIndex) {
    super(chain, group3, sequenceNumber, insertionCode,
          firstAtomIndex, lastAtomIndex);
    offsets[0] = 0;
  }

  boolean isAlphaMonomer() { return true; }

  ProteinStructure proteinStructure;
  void setStructure(ProteinStructure proteinStructure) {
    this.proteinStructure = proteinStructure;
  }

  ProteinStructure getProteinStructure() { return proteinStructure; }

  byte getProteinStructureType() {
    return proteinStructure == null ? 0 : proteinStructure.type;
  }

  boolean isHelix() {
    return proteinStructure != null &&
      proteinStructure.type == JmolConstants.PROTEIN_STRUCTURE_HELIX;
  }

  boolean isHelixOrSheet() {
    return proteinStructure != null &&
      proteinStructure.type >= JmolConstants.PROTEIN_STRUCTURE_SHEET;
  }

}
