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

  static Monomer
    validateAndAllocate(Chain chain, String group3,
                        int sequenceNumber, char insertionCode,
                        int distinguishingBits, Atom[] atoms,
                        int firstAtomIndex, int lastAtomIndex) {

    int phosphorusIndex = -1;
    int wingIndex = -1;
    int o2PrimeIndex = -1;

    int n1Index = -1;

    for (int i = lastAtomIndex; i >= firstAtomIndex; --i) {
      Atom atom = atoms[i];
      switch (atoms[i].specialAtomID) {
      case JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS:
        phosphorusIndex = i;
        break;
      case JmolConstants.ATOMID_NUCLEIC_WING:
        wingIndex = i;
        break;
      case JmolConstants.ATOMID_RNA_O2PRIME:
        o2PrimeIndex = i;
        break;
      case JmolConstants.ATOMID_N1:
        n1Index = i;
        break;
      }
    }

    // this is just testing if anybody is less than 0
    if ((phosphorusIndex | wingIndex) < 0)
      throw new NullPointerException();

    NucleicMonomer nucleicMonomer =
      new NucleicMonomer(chain, group3, sequenceNumber, insertionCode,
                         firstAtomIndex, lastAtomIndex,
                         phosphorusIndex, wingIndex, o2PrimeIndex);
    return nucleicMonomer;
  }

  NucleicMonomer(Chain chain, String group3,
                 int sequenceNumber, char insertionCode,
                 int firstAtomIndex, int lastAtomIndex,
                 int phosphorusIndex, int wingIndex,
                 int o2PrimeIndex) {
    super(chain, group3, sequenceNumber, insertionCode,
          firstAtomIndex, lastAtomIndex, 2);
    this.hasRnaO2Prime = o2PrimeIndex >= 0;

    offsets[0] = (byte)(phosphorusIndex - firstAtomIndex);
    offsets[1] = (byte)(wingIndex - firstAtomIndex);
  }

  boolean hasRnaO2Prime;

  boolean isNucleicMonomer() { return true; }

  public boolean isDna() { return ! hasRnaO2Prime; }

  public boolean isRna() { return hasRnaO2Prime; }

  byte getProteinStructureType() {
    return (hasRnaO2Prime
            ? JmolConstants.PROTEIN_STRUCTURE_RNA
            : JmolConstants.PROTEIN_STRUCTURE_DNA);
  }
}
