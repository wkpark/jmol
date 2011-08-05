/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.smarter;

import org.jmol.constant.EnumProteinStructure;

public class Structure {
  public byte structureType;
  public byte substructureType;
  public String structureID;
  public int serialID;
  public int strandCount;
  public char startChainID = ' ';
  public int startSequenceNumber;
  public char startInsertionCode = ' ';
  public char endChainID = ' ';
  public int endSequenceNumber;
  public char endInsertionCode = ' ';
  public int modelIndex;

  public final static byte PROTEIN_STRUCTURE_NONE = EnumProteinStructure.NONE.id;
  public final static byte PROTEIN_STRUCTURE_TURN = EnumProteinStructure.TURN.id;
  public final static byte PROTEIN_STRUCTURE_SHEET = EnumProteinStructure.SHEET.id;
  public final static byte PROTEIN_STRUCTURE_HELIX = EnumProteinStructure.HELIX.id;
  public final static byte PROTEIN_STRUCTURE_HELIX_310 = EnumProteinStructure.HELIX_310.id;
  public final static byte PROTEIN_STRUCTURE_HELIX_ALPHA = EnumProteinStructure.HELIX_ALPHA.id;
  public final static byte PROTEIN_STRUCTURE_HELIX_PI = EnumProteinStructure.HELIX_PI.id;

  public static byte getHelixType(int type) {
    switch (type) {
    case 1:
      return PROTEIN_STRUCTURE_HELIX_ALPHA;
    case 3:
      return PROTEIN_STRUCTURE_HELIX_PI;
    case 5:
      return PROTEIN_STRUCTURE_HELIX_310;
    }
    return PROTEIN_STRUCTURE_HELIX;
  }
  

  public Structure(byte type) {
    structureType = substructureType = type;
  }

  public Structure(int modelIndex, byte structureType, byte substructureType,
            String structureID, int serialID, int strandCount,
            char startChainID, int startSequenceNumber, char startInsertionCode,
            char endChainID, int endSequenceNumber, char endInsertionCode) {
    this.modelIndex = modelIndex;
    this.structureType = structureType;
    this.substructureType = substructureType;
    this.structureID = structureID;
    this.strandCount = strandCount; // 1 for sheet initially; 0 for helix or turn
    this.serialID = serialID;
    this.startChainID = startChainID;
    this.startSequenceNumber = startSequenceNumber;
    this.startInsertionCode = startInsertionCode;
    this.endChainID = endChainID;
    this.endSequenceNumber = endSequenceNumber;
    this.endInsertionCode = endInsertionCode;
  }

}
