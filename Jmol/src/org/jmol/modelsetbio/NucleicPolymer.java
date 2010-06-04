/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-30 14:45:19 -0500 (Sat, 30 Sep 2006) $
 * $Revision: 5781 $
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

import java.util.BitSet;
import java.util.Vector;

import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.api.JmolEdge;
import org.jmol.modelset.Atom;
import org.jmol.modelset.HBond;
import org.jmol.modelset.Polymer;
import org.jmol.util.Measure;
import org.jmol.util.OutputStringBuffer;
import org.jmol.viewer.Viewer;

public class NucleicPolymer extends BioPolymer {

  NucleicPolymer(Monomer[] monomers) {
    super(monomers);
    type = TYPE_NUCLEIC;
  }

  Atom getNucleicPhosphorusAtom(int monomerIndex) {
    return monomers[monomerIndex].getLeadAtom();
  }

  boolean hasWingPoints() { return true; }

  public void calcRasmolHydrogenBonds(Polymer polymer, BitSet bsA, BitSet bsB, Vector vAtoms, int nMaxPerResidue) {
    NucleicPolymer other = (NucleicPolymer)polymer;
    Vector3f vNorm = new Vector3f();
    Vector3f vAB = new Vector3f();
    Vector3f vAC = new Vector3f();
    for (int i = monomerCount; --i >= 0; ) {
      NucleicMonomer myNucleotide = (NucleicMonomer)monomers[i];
      if (! myNucleotide.isPurine())
        continue;
      Atom myN3 = myNucleotide.getN3();
      boolean isInA = bsA.get(myN3.index);
      if (!isInA && !bsB.get(myN3.index))
        continue;
      Atom myN1 = myNucleotide.getN1();
      Atom myN9 = myNucleotide.getN0();
      Point4f plane = Measure.getPlaneThroughPoints(myN3, myN1, myN9, vNorm, vAB, vAC);
      Atom bestN3 = null;
      float minDist2 = 25;
      NucleicMonomer bestNucleotide = null;
      for (int j = other.monomerCount; --j >= 0; ) {
        NucleicMonomer otherNucleotide = (NucleicMonomer)other.monomers[j];
        if (! otherNucleotide.isPyrimidine())
          continue;
        Atom otherN3 = otherNucleotide.getN3();
        if (isInA ? !bsB.get(otherN3.index) : !bsA.get(otherN3.index))
          continue;
        Atom otherN1 = otherNucleotide.getN0();
        float dist2 = myN1.distanceSquared(otherN3);
        if (dist2 < minDist2 
            && myN9.distanceSquared(otherN1) > 50 // not stacked
            && Math.abs(Measure.distanceToPlane(plane, otherN3)) < 1 // in plane
        ) {
          bestNucleotide = otherNucleotide;
          bestN3 = otherN3;
          minDist2 = dist2;
        }
      }
      int n = 0;
      if (bestN3 != null) {
        n += addHydrogenBond(vAtoms, myN1, bestN3);
        if (n >= nMaxPerResidue)
          continue;
        if (myNucleotide.isGuanine()) {
          n += addHydrogenBond(vAtoms, myNucleotide.getN2(),
              bestNucleotide.getO2());
          if (n >= nMaxPerResidue)
            continue;
          n += addHydrogenBond(vAtoms, myNucleotide.getO6(),
              bestNucleotide.getN4());
          if (n >= nMaxPerResidue)
            continue;
        } else {
          n += addHydrogenBond(vAtoms, myNucleotide.getN6(),
              bestNucleotide.getO4());
        }
      }
    }
  }

  static protected int addHydrogenBond(Vector vAtoms, Atom atom1, Atom atom2) {
    if (atom1 == null || atom2 == null)
      return 0;
    vAtoms.add(new HBond(atom1, atom2, JmolEdge.BOND_H_NUCLEOTIDE, 0));
    return 1;
  }


  public void getPdbData(Viewer viewer, char ctype, char qtype, int mStep, int derivType,
                         boolean isDraw, BitSet bsAtoms, 
                         OutputStringBuffer pdbATOM, StringBuffer pdbCONECT, 
                         BitSet bsSelected, boolean addHeader, BitSet bsWritten) {
    getPdbData(viewer, this, ctype, qtype, mStep, derivType, isDraw, bsAtoms, pdbATOM, 
        pdbCONECT, bsSelected, addHeader, bsWritten);
  }   
}
