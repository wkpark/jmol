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
import java.util.Hashtable;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;

public class AminoPolymer extends AlphaCarbonPolymer {

  AminoPolymer(Model model, Group[] groups) {
    super(model, groups);
  }

  boolean hasWingPoints() { return true; }

  Point3f getWingPoint(int polymerIndex) {
    return groups[polymerIndex].getWingAtom().point3f;
  }

  // to get something other than the alpha carbon atom
  public Point3f getResiduePoint(int groupIndex, int mainchainIndex) {
    return groups[groupIndex].getMainchainAtom(mainchainIndex).point3f;
  }

  public void calcHydrogenBonds() {
    calcProteinMainchainHydrogenBonds();
  }


  Vector3f vectorPreviousOC = new Vector3f();
  Point3f aminoHydrogenPoint = new Point3f();

  void calcProteinMainchainHydrogenBonds() {
    Point3f carbonPoint;
    Point3f oxygenPoint;
    
    for (int i = 0; i < count; ++i) {
      Group residue = groups[i];
      /****************************************************************
       * This does not acount for the first nitrogen in the chain
       * is there some way to predict where it's hydrogen is?
       * mth 20031219
       ****************************************************************/
      if (i > 0 && !residue.isProline()) {
        Point3f nitrogenPoint = residue.getNitrogenAtom().point3f;
        aminoHydrogenPoint.add(nitrogenPoint, vectorPreviousOC);
        bondAminoHydrogen(i, aminoHydrogenPoint);
      }
      carbonPoint = residue.getCarbonylCarbonAtom().point3f;
      oxygenPoint = residue.getCarbonylOxygenAtom().point3f;
      vectorPreviousOC.sub(carbonPoint, oxygenPoint);
    }
  }

  private final static float maxHbondAlphaDistance = 9;
  private final static float maxHbondAlphaDistance2 =
    maxHbondAlphaDistance * maxHbondAlphaDistance;
  private final static float minimumHbondDistance2 = 0.5f;
  private final static double QConst = -332 * 0.42 * 0.2 * 1000;

  void bondAminoHydrogen(int indexDonor, Point3f hydrogenPoint) {
    Group source = groups[indexDonor];
    Point3f sourceAlphaPoint = source.getAlphaCarbonPoint();
    Point3f sourceNitrogenPoint = source.getNitrogenAtom().point3f;
    int energyMin1 = 0;
    int energyMin2 = 0;
    int indexMin1 = -1;
    int indexMin2 = -1;
    for (int i = count; --i >= 0; ) {
      if ((i == indexDonor || (i+1) == indexDonor) || (i-1) == indexDonor) {
        //        System.out.println(" i=" +i + " indexDonor=" + indexDonor);
        continue;
      }
      Group target = groups[i];
      Point3f targetAlphaPoint = target.getAlphaCarbonPoint();
      float dist2 = sourceAlphaPoint.distanceSquared(targetAlphaPoint);
      if (dist2 > maxHbondAlphaDistance2)
        continue;
      int energy = calcHbondEnergy(sourceNitrogenPoint, hydrogenPoint, target);
      //      System.out.println("HbondEnergy=" + energy);
      if (energy < energyMin1) {
        energyMin1 = energy;
        indexMin1 = i;
      } else if (energy < energyMin2) {
        energyMin2 = energy;
        indexMin2 = i;
      }
    }
    if (indexMin1 >= 0) {
      createResidueHydrogenBond(indexDonor, indexMin1);
      if (indexMin2 >= 0)
        createResidueHydrogenBond(indexDonor, indexMin2);
    }
  }

  int calcHbondEnergy(Point3f nitrogenPoint, Point3f hydrogenPoint,
                      Group target) {
    Point3f targetOxygenPoint = target.getCarbonylOxygenAtom().point3f;
    float distOH2 = targetOxygenPoint.distanceSquared(hydrogenPoint);
    if (distOH2 < minimumHbondDistance2)
      return -9900;

    Point3f targetCarbonPoint = target.getCarbonylCarbonAtom().point3f;
    float distCH2 = targetCarbonPoint.distanceSquared(hydrogenPoint);
    if (distCH2 < minimumHbondDistance2)
      return -9900;

    float distCN2 = targetCarbonPoint.distanceSquared(nitrogenPoint);
    if (distCN2 < minimumHbondDistance2)
      return -9900;

    float distON2 = targetOxygenPoint.distanceSquared(nitrogenPoint);
    if (distON2 < minimumHbondDistance2)
      return -9900;

    double distOH = Math.sqrt(distOH2);
    double distCH = Math.sqrt(distCH2);
    double distCN = Math.sqrt(distCN2);
    double distON = Math.sqrt(distON2);

    int energy =
      (int)((QConst/distOH - QConst/distCH + QConst/distCN - QConst/distON));

    /*
    System.out.println(" distOH=" + distOH +
                       " distCH=" + distCH +
                       " distCN=" + distCN +
                       " distON=" + distON +
                       " energy=" + energy);
    */
    if (energy < -9900)
      return -9900;
    if (energy > -500)
      return 0;
    return energy;
  }

  void createResidueHydrogenBond(int indexAminoGroup,
                                 int indexCarbonylGroup) {
    int order;
    int aminoBackboneHbondOffset = indexAminoGroup - indexCarbonylGroup;
    /*
    System.out.println("aminoBackboneHbondOffset=" +
                       aminoBackboneHbondOffset +
                       " amino:" +
                       groups[indexAminoGroup].getSeqcodeString() +
                       " carbonyl:" +
                       groups[indexCarbonylGroup].getSeqcodeString());
    */
    switch (aminoBackboneHbondOffset) {
    case 2:
      order = JmolConstants.BOND_H_PLUS_2;
      break;
    case 3:
      order = JmolConstants.BOND_H_PLUS_3;
      break;
    case 4:
      order = JmolConstants.BOND_H_PLUS_4;
      break;
    case 5:
      order = JmolConstants.BOND_H_PLUS_5;
      break;
    case -3:
      order = JmolConstants.BOND_H_MINUS_3;
      break;
    case -4:
      order = JmolConstants.BOND_H_MINUS_4;
      break;
    default:
      order = JmolConstants.BOND_H_REGULAR;
    }
    //    System.out.println("createResidueHydrogenBond(" + indexAminoGroup +
    //                       "," + indexCarbonylGroup);
    Group groupDonor = groups[indexAminoGroup];
    groupDonor.setAminoBackboneHbondOffset(aminoBackboneHbondOffset);
    Atom nitrogen = groupDonor.getNitrogenAtom();
    Atom oxygen = groups[indexCarbonylGroup].getCarbonylOxygenAtom();
    Frame frame = model.mmset.frame;
    frame.bondAtoms(nitrogen, oxygen, order);
  }
}
