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
package org.jmol.viewer.datamodel;

import org.jmol.viewer.*;

public class NucleicPolymer extends Polymer {

  NucleicPolymer(Monomer[] monomers) {
    super(monomers);
  }

  public Atom getNucleicPhosphorusAtom(int monomerIndex) {
    return monomers[monomerIndex].getLeadAtom();
  }

  boolean hasWingPoints() { return true; }

  public void calcHydrogenBonds() {
    for (int i = model.getPolymerCount(); --i >= 0; ) {
      Polymer otherPolymer = model.getPolymer(i);
      if (otherPolymer == this) // don't look at self
        continue;
      if (otherPolymer == null || !(otherPolymer instanceof NucleicPolymer))
        continue;
      lookForHbonds((NucleicPolymer)otherPolymer);
    }
  }

  void lookForHbonds(NucleicPolymer other) {
    System.out.println("NucleicPolymer.lookForHbonds()");
    for (int i = count; --i >= 0; ) {
      NucleicMonomer myNucleotide = (NucleicMonomer)monomers[i];
      if (! myNucleotide.isPurine())
        continue;
      Atom myN1 = myNucleotide.getN1();
      Atom bestN3 = null;
      float minDist2 = 5*5;
      NucleicMonomer bestNucleotide = null;
      for (int j = other.count; --j >= 0; ) {
        NucleicMonomer otherNucleotide = (NucleicMonomer)other.monomers[j];
        if (! otherNucleotide.isPyrimidine())
          continue;
        Atom otherN3 = otherNucleotide.getN3();
        float dist2 = myN1.point3f.distanceSquared(otherN3.point3f);
        if (dist2 < minDist2) {
          bestNucleotide = otherNucleotide;
          bestN3 = otherN3;
          minDist2 = dist2;
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

  void createHydrogenBond(Atom atom1, Atom atom2) {
    //    System.out.println("createHydrogenBond:" +
    // atom1.getAtomNumber() + "<->" + atom2.getAtomNumber());
    if (atom1 != null && atom2 != null) {
      Frame frame = model.mmset.frame;
      frame.bondAtoms(atom1, atom2, JmolConstants.BOND_H_NUCLEOTIDE);
    }
  }
}
