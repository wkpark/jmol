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

  NucleotidePolymer(Chain chain, Group[] groups) {
    super(chain, groups);
  }

  public Atom getNucleotidePhosphorusAtom(int groupIndex) {
    return groups[groupIndex].getNucleotidePhosphorusAtom();
  }

  public Atom getLeadAtom(int groupIndex) {
    return getNucleotidePhosphorusAtom(groupIndex);
  }

  public Point3f getLeadPoint(int groupIndex) {
    return getLeadAtom(groupIndex).point3f;
  }

  boolean hasWingPoints() { return true; }

  Point3f getWingPoint(int polymerIndex) {
    return groups[polymerIndex].getWingAtom().point3f;
  }
  
  public void getStructureMidPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex < count &&
        groups[groupIndex].isHelixOrSheet()) {
      midPoint.set(groups[groupIndex].aminostructure.
                   getStructureMidPoint(groupIndex));
      /*
        System.out.println("" + groupIndex} + "isHelixOrSheet" +
        midPoint.x + "," + midPoint.y + "," + midPoint.z);
      */
    } else if (groupIndex > 0 &&
               groups[groupIndex - 1].isHelixOrSheet()) {
      midPoint.set(groups[groupIndex - 1].aminostructure.
                   getStructureMidPoint(groupIndex));
      /*
        System.out.println("" + groupIndex + "previous isHelixOrSheet" +
        midPoint.x + "," + midPoint.y + "," + midPoint.z);
      */
    } else {
      getLeadMidPoint(groupIndex, midPoint);
      /*
        System.out.println("" + groupIndex + "the alpha carbon midpoint" +
        midPoint.x + "," + midPoint.y + "," + midPoint.z);
      */
    }
  }

  void addSecondaryStructure(byte type,
                             int startSeqcode, int endSeqcode) {
  }

  public boolean isProtein() {
    return false;
  }

  public void calcHydrogenBonds() {
    PdbModel model = chain.pdbmodel;
    for (int i = model.getChainCount(); --i >= 0; ) {
      Chain otherChain = model.getChain(i);
      if (otherChain == chain) // don't look at self
        continue;
      Polymer otherPolymer = otherChain.getPolymer();
      if (otherPolymer == null || !(otherPolymer instanceof NucleotidePolymer))
        continue;
      lookForHbonds((NucleotidePolymer)otherPolymer);
    }
  }

  void lookForHbonds(NucleotidePolymer other) {
    for (int i = count; --i >= 0; ) {
      Group myNucleotide = groups[i];
      Atom myN1 = myNucleotide.getPurineN1();
      if (myN1 != null) {
        Atom bestN3 = null;
        float minDist2 = 5*5;
        Group bestNucleotide = null;
        for (int j = other.count; --j >= 0; ) {
          Group otherNucleotide = other.groups[j];
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
            Atom myN2 =
              myNucleotide.getNucleotideAtomID(JmolConstants.ATOMID_N2);
            Atom otherO2 =
              bestNucleotide.getNucleotideAtomID(JmolConstants.ATOMID_O2);
            createHydrogenBond(myN2, otherO2);
            Atom myO6 =
              myNucleotide.getNucleotideAtomID(JmolConstants.ATOMID_O6);
            Atom otherN4 =
              bestNucleotide.getNucleotideAtomID(JmolConstants.ATOMID_N4);
            createHydrogenBond(myO6, otherN4);
          } else {
            Atom myN6 =
              myNucleotide.getNucleotideAtomID(JmolConstants.ATOMID_N6);
            Atom otherO4 =
              bestNucleotide.getNucleotideAtomID(JmolConstants.ATOMID_O4);
            createHydrogenBond(myN6, otherO4);
          }
        }
      }
    }
  }

  void createHydrogenBond(Atom atom1, Atom atom2) {
    //    System.out.println("createHydrogenBond:" +
    // atom1.getAtomNumber() + "<->" + atom2.getAtomNumber());
    if (atom1 != null && atom2 != null) {
      Frame frame = chain.pdbmodel.pdbfile.frame;
      frame.addHydrogenBond(atom1, atom2);
    }
  }

}
