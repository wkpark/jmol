/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.datamodel.Frame;
import java.util.Hashtable;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;

public class AlphaPolymer extends Polymer {

  AlphaPolymer(Model model, Monomer[] monomers) {
    super(model, monomers);
  }

  /*
  void getStructureMidPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex < count &&
        monomers[groupIndex].isHelixOrSheet()) {
      midPoint.set(monomers[groupIndex].proteinstructure.
                   getStructureMidPoint(groupIndex));
      //    System.out.println("" + groupIndex} + "isHelixOrSheet" +
      //    midPoint.x + "," + midPoint.y + "," + midPoint.z);
    } else if (groupIndex > 0 &&
               monomers[groupIndex - 1].isHelixOrSheet()) {
      midPoint.set(monomers[groupIndex - 1].proteinstructure.
                   getStructureMidPoint(groupIndex));
      //    System.out.println("" + groupIndex + "previous isHelixOrSheet" +
      //    midPoint.x + "," + midPoint.y + "," + midPoint.z);
    } else {
      getLeadMidPoint(groupIndex, midPoint);
      //    System.out.println("" + groupIndex + "the alpha carbon midpoint" +
      //    midPoint.x + "," + midPoint.y + "," + midPoint.z);
    }
  }
  */

  void addSecondaryStructure(byte type,
                             int startSeqcode, int endSeqcode) {
    int polymerIndexStart, polymerIndexEnd;
    if ((polymerIndexStart = getIndex(startSeqcode)) == -1 ||
        (polymerIndexEnd = getIndex(endSeqcode)) == -1)
      return;
    int structureCount = polymerIndexEnd - polymerIndexStart + 1;
    if (structureCount < 1) {
      System.out.println("structure definition error");
      return;
    }
    ProteinStructure proteinstructure = null;
    switch(type) {
    case JmolConstants.PROTEIN_STRUCTURE_HELIX:
      proteinstructure = new Helix(this, polymerIndexStart, structureCount);
      break;
    case JmolConstants.PROTEIN_STRUCTURE_SHEET:
      if (this instanceof AminoPolymer)
        proteinstructure = new Sheet((AminoPolymer)this,
                                   polymerIndexStart, structureCount);
      break;
    case JmolConstants.PROTEIN_STRUCTURE_TURN:
      proteinstructure = new Turn(this, polymerIndexStart, structureCount);
      break;
    default:
      System.out.println("unrecognized secondary structure type");
      return;
    }
    for (int i = polymerIndexStart; i <= polymerIndexEnd; ++i)
      monomers[i].setStructure(proteinstructure);
  }

  boolean isProtein() {
    return true;
  }

  void calcHydrogenBonds() {
  }
}
