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

  static Monomer
    validateAndAllocate(Chain chain, String group3,
                        int sequenceNumber, char insertionCode,
                        Atom[] atoms,
                        int firstAtomIndex, int lastAtomIndex) {
    AminoMonomer aminoMonomer =
      new AminoMonomer(chain, group3, sequenceNumber, insertionCode,
                       firstAtomIndex, lastAtomIndex);
    for (int i = firstAtomIndex; i <= lastAtomIndex; ++i)
      aminoMonomer.registerAtom(atoms[i]);
    return aminoMonomer;
  }
  
  
  AminoMonomer(Chain chain, String group3,
               int sequenceNumber, char insertionCode,
               int firstAtomIndex, int lastAtomIndex) {
    super(chain, group3, sequenceNumber, insertionCode,
          firstAtomIndex, lastAtomIndex);
  }
}
