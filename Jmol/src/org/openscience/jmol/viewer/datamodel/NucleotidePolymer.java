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

public class NucleotidePolymer extends Polymer {

  NucleotidePolymer(Model model, Monomer[] monomers) {
    super(model, monomers);
  }

  public Atom getNucleotidePhosphorusAtom(int monomerIndex) {
    return monomers[monomerIndex].getLeadAtom();
  }

  boolean hasWingPoints() { return true; }

  /*
  public void getStructureMidPoint(int monomerIndex, Point3f midPoint) {
    if (monomerIndex < count &&
        monomers[monomerIndex].isHelixOrSheet()) {
      midPoint.set(monomers[monomerIndex].proteinstructure.
                   getStructureMidPoint(monomerIndex));
      //    System.out.println("" + monomerIndex} + "isHelixOrSheet" +
      //    midPoint.x + "," + midPoint.y + "," + midPoint.z);
    } else if (monomerIndex > 0 &&
               monomers[monomerIndex - 1].isHelixOrSheet()) {
      midPoint.set(monomers[monomerIndex - 1].proteinstructure.
                   getStructureMidPoint(monomerIndex));
      //    System.out.println("" + monomerIndex + "previous isHelixOrSheet" +
      //    midPoint.x + "," + midPoint.y + "," + midPoint.z);
    } else {
      getLeadMidPoint(monomerIndex, midPoint);
      //   System.out.println("" + monomerIndex + "the alpha carbon midpoint" +
      //    midPoint.x + "," + midPoint.y + "," + midPoint.z);
    }
  }
  */

  void addSecondaryStructure(byte type,
                             int startSeqcode, int endSeqcode) {
  }

  public boolean isProtein() {
    return false;
  }

  public void calcHydrogenBonds() {
    for (int i = model.getPolymerCount(); --i >= 0; ) {
      Polymer otherPolymer = model.getPolymer(i);
      if (otherPolymer == this) // don't look at self
        continue;
      if (otherPolymer == null || !(otherPolymer instanceof NucleotidePolymer))
        continue;
      lookForHbonds((NucleotidePolymer)otherPolymer);
    }
  }

  void lookForHbonds(NucleotidePolymer other) {
    for (int i = count; --i >= 0; ) {
      NucleicMonomer myNucleotide = (NucleicMonomer)monomers[i];
      Atom myN1 = myNucleotide.getPurineN1();
      if (myN1 != null) {
        Atom bestN3 = null;
        float minDist2 = 5*5;
        NucleicMonomer bestNucleotide = null;
        for (int j = other.count; --j >= 0; ) {
          NucleicMonomer otherNucleotide = (NucleicMonomer)other.monomers[j];
          Atom otherN3 = otherNucleotide.getPyrimidineN3();
          if (otherN3 != null) {
            float dist2 = myN1.point3f.distanceSquared(otherN3.point3f);
            if (dist2 < minDist2) {
              bestNucleotide = otherNucleotide;
              bestN3 = otherN3;
              minDist2 = dist2;
            }
          }
        }
        if (bestN3 != null) {
          createHydrogenBond(myN1, bestN3);
          if (myNucleotide.isGuanine()) {
            createHydrogenBond(myNucleotide.getN2(),
                               bestNucleotide.getO2());
            createHydrogenBond(myNucleotide.getO6(),
                               bestNucleotide.getN4());
          } else {
            createHydrogenBond(myNucleotide.getN6(),
                               bestNucleotide.getO4());
          }
        }
      }
    }
  }

  void createHydrogenBond(Atom atom1, Atom atom2) {
    //    System.out.println("createHydrogenBond:" +
    // atom1.getAtomNumber() + "<->" + atom2.getAtomNumber());
    if (atom1 != null && atom2 != null) {
      Frame frame = model.mmset.frame;
      frame.bondAtoms(atom1, atom2, JmolConstants.BOND_H_NUCLEOTIDE);
    }
  }

}
