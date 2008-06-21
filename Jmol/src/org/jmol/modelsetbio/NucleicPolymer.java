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

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.modelset.Atom;
import org.jmol.util.Quaternion;
import org.jmol.viewer.JmolConstants;

public class NucleicPolymer extends BioPolymer {

  NucleicPolymer(Monomer[] monomers) {
    super(monomers);
  }

  Atom getNucleicPhosphorusAtom(int monomerIndex) {
    return monomers[monomerIndex].getLeadAtom();
  }

  boolean hasWingPoints() { return true; }

  public void calcHydrogenBonds(BitSet bsA, BitSet bsB) {
    for (int i = model.getBioPolymerCount(); --i >= 0; ) {
      BioPolymer otherPolymer = (BioPolymer) model.getBioPolymer(i);
      if (otherPolymer == this) // don't look at self
        continue;
      if (otherPolymer == null || !(otherPolymer instanceof NucleicPolymer))
        continue;
      lookForHbonds((NucleicPolymer)otherPolymer, bsA, bsB);
    }
  }

  private final static short HBOND_MASK = JmolConstants.BOND_H_NUCLEOTIDE;
  
  void lookForHbonds(NucleicPolymer other, BitSet bsA, BitSet bsB) {
    //Logger.debug("NucleicPolymer.lookForHbonds()");
    for (int i = monomerCount; --i >= 0; ) {
      NucleicMonomer myNucleotide = (NucleicMonomer)monomers[i];
      if (! myNucleotide.isPurine())
        continue;
      Atom myN1 = myNucleotide.getN1();
      Atom bestN3 = null;
      float minDist2 = 25;
      NucleicMonomer bestNucleotide = null;
      for (int j = other.monomerCount; --j >= 0; ) {
        NucleicMonomer otherNucleotide = (NucleicMonomer)other.monomers[j];
        if (! otherNucleotide.isPyrimidine())
          continue;
        Atom otherN3 = otherNucleotide.getN3();
        float dist2 = myN1.distanceSquared(otherN3);
        if (dist2 < minDist2) {
          bestNucleotide = otherNucleotide;
          bestN3 = otherN3;
          minDist2 = dist2;
        }
      }
      if (bestN3 != null) {
        model.addHydrogenBond(myN1, bestN3,  HBOND_MASK, bsA, bsB);
        if (myNucleotide.isGuanine()) {
          model.addHydrogenBond(myNucleotide.getN2(),
                             bestNucleotide.getO2(), HBOND_MASK, bsA, bsB);
          model.addHydrogenBond(myNucleotide.getO6(),
                             bestNucleotide.getN4(), HBOND_MASK, bsA, bsB);
        } else {
          model.addHydrogenBond(myNucleotide.getN6(),
                             bestNucleotide.getO4(), HBOND_MASK, bsA, bsB);
        }
      }
    }
  }

  public void getPdbData(char ctype, char qtype, int derivType,
                         BitSet bsAtoms, StringBuffer pdbATOM, 
                         StringBuffer pdbCONECT, BitSet bsSelected) {
    getPdbData(this, ctype, qtype, derivType, bsAtoms, pdbATOM, pdbCONECT, 
        bsSelected);
  }

  static Point3f getQuaternionFrameCenter(NucleicMonomer m, char qType) {
    return (m.isPurine() ? m.getAtomFromOffsetIndex(NucleicMonomer.N9)
        : m.getN1());
  }
  
  Quaternion getQuaternion(int i, char qType) {
    /*
     * also AminoMonomer
     *   
     */
     
    /*
    Point3f ptP = getP(); 
    Point3f ptO1P = getO1P();
    Point3f ptO2P = getO2P();
    if(ptP == null || ptO1P == null || ptO2P == null)
      return null;
    //vA = ptO1P - ptP
    Vector3f vA = new Vector3f(ptO1P);
    vA.sub(ptP);
    
    //vB = ptO2P - ptP
    Vector3f vB = new Vector3f(ptO2P);
    vB.sub(ptP);
    return Quaternion.getQuaternionFrame(vA, vB);   
    
    */
    
    NucleicMonomer m = (NucleicMonomer) monomers[i];
    //if (m.getLeadAtom().getElementSymbol() != "P")
      //return null;
    Point3f ptA, ptB;
    Point3f ptN = getQuaternionFrameCenter(m, qType);
    if (m.isPurine) {
      ptA = m.getAtomFromOffsetIndex(NucleicMonomer.C4);
      ptB = m.getAtomFromOffsetIndex(NucleicMonomer.C8);
    } else {
      ptA = m.getAtomFromOffsetIndex(NucleicMonomer.C2);
      ptB = m.getAtomFromOffsetIndex(NucleicMonomer.C6);
    }
    if(ptN == null || ptA == null || ptB == null)
      return null;

    Vector3f vA = new Vector3f(ptA);
    vA.sub(ptN);
    
    Vector3f vB = new Vector3f(ptB);
    vB.sub(ptN);
    //vA.set(1f, 0.2f, 0f);
    //vB.set(-0.2f, 1f, 0f);
    return Quaternion.getQuaternionFrame(vA, vB, null);
  }
   
}
