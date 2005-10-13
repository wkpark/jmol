/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

class AminoPolymer extends AlphaPolymer {

  // the primary offset within the same mainchain;
  short[] mainchainHbondOffsets;
  short[] min1Indexes;
  short[] min1Energies;
  short[] min2Indexes;
  short[] min2Energies;

  AminoPolymer(Monomer[] monomers) {
    super(monomers);
  }

  boolean hasWingPoints() { return true; }

  boolean hbondsAlreadyCalculated;

  final static boolean debugHbonds = false;

  void calcHydrogenBonds() {
    if (! hbondsAlreadyCalculated) {
      allocateHbondDataStructures();
      calcProteinMainchainHydrogenBonds();
      hbondsAlreadyCalculated = true;

      if (debugHbonds) {
        System.out.println("calcHydrogenBonds");
        for (int i = 0; i < monomerCount; ++i) {
          System.out.println("  min1Indexes=" + min1Indexes[i] +
                             "\nmin1Energies=" + min1Energies[i] +
                             "\nmin2Indexes=" + min2Indexes[i] +
                           "\nmin2Energies=" + min2Energies[i]);
        }
      }
    }
  }

  

  void allocateHbondDataStructures() {
    mainchainHbondOffsets = new short[monomerCount];
    min1Indexes = new short[monomerCount];
    min1Energies = new short[monomerCount];
    min2Indexes = new short[monomerCount];
    min2Energies = new short[monomerCount];
    for (int i = monomerCount; --i >= 0; )
      min1Indexes[i] = min2Indexes[i] = -1;
  }

  void freeHbondDataStructures() {
    mainchainHbondOffsets =
      min1Indexes = min1Energies = min2Indexes = min2Energies = null;
  }

  final Vector3f vectorPreviousOC = new Vector3f();
  final Point3f aminoHydrogenPoint = new Point3f();

  void calcProteinMainchainHydrogenBonds() {
    Point3f carbonPoint;
    Point3f oxygenPoint;
    
    for (int i = 0; i < monomerCount; ++i) {
      AminoMonomer residue = (AminoMonomer)monomers[i];
      mainchainHbondOffsets[i] = 0;
      /****************************************************************
       * This does not acount for the first nitrogen in the chain
       * is there some way to predict where it's hydrogen is?
       * mth 20031219
       ****************************************************************/
      if (i > 0 && residue.getGroupID() != JmolConstants.GROUPID_PROLINE) {
        Point3f nitrogenPoint = residue.getNitrogenAtomPoint();
        aminoHydrogenPoint.add(nitrogenPoint, vectorPreviousOC);
        bondAminoHydrogen(i, aminoHydrogenPoint);
      }
      carbonPoint = residue.getCarbonylCarbonAtomPoint();
      oxygenPoint = residue.getCarbonylOxygenAtomPoint();
      vectorPreviousOC.sub(carbonPoint, oxygenPoint);
    }
  }

  private final static float maxHbondAlphaDistance = 9;
  private final static float maxHbondAlphaDistance2 =
    maxHbondAlphaDistance * maxHbondAlphaDistance;
  private final static float minimumHbondDistance2 = 0.5f;
  private final static double QConst = -332 * 0.42 * 0.2 * 1000;

  void bondAminoHydrogen(int indexDonor, Point3f hydrogenPoint) {
    AminoMonomer source = (AminoMonomer)monomers[indexDonor];
    Point3f sourceAlphaPoint = source.getLeadAtomPoint();
    Point3f sourceNitrogenPoint = source.getNitrogenAtomPoint();
    int energyMin1 = 0;
    int energyMin2 = 0;
    int indexMin1 = -1;
    int indexMin2 = -1;
    for (int i = monomerCount; --i >= 0; ) {
      if ((i == indexDonor || (i+1) == indexDonor) || (i-1) == indexDonor)
        continue;
      AminoMonomer target = (AminoMonomer)monomers[i];
      Point3f targetAlphaPoint = target.getLeadAtomPoint();
      float dist2 = sourceAlphaPoint.distanceSquared(targetAlphaPoint);
      if (dist2 > maxHbondAlphaDistance2)
        continue;
      int energy = calcHbondEnergy(sourceNitrogenPoint, hydrogenPoint, target);
      if (debugHbonds)
        System.out.println("HbondEnergy=" + energy);
      if (energy < energyMin1) {
        energyMin2 = energyMin1;
        indexMin2 = indexMin1;
        energyMin1 = energy;
        indexMin1 = i;
      } else if (energy < energyMin2) {
        energyMin2 = energy;
        indexMin2 = i;
      }
    }
    if (indexMin1 >= 0) {
      mainchainHbondOffsets[indexDonor] = (short)(indexDonor - indexMin1);
      min1Indexes[indexDonor] = (short)indexMin1;
      min1Energies[indexDonor] = (short)energyMin1;
      createResidueHydrogenBond(indexDonor, indexMin1);
      if (indexMin2 >= 0) {
        createResidueHydrogenBond(indexDonor, indexMin2);
        min2Indexes[indexDonor] = (short)indexMin2;
        min2Energies[indexDonor] = (short)energyMin2;
      }
    }
  }

  int calcHbondEnergy(Point3f nitrogenPoint, Point3f hydrogenPoint,
                      AminoMonomer target) {
    Point3f targetOxygenPoint = target.getCarbonylOxygenAtomPoint();
    float distOH2 = targetOxygenPoint.distanceSquared(hydrogenPoint);
    if (distOH2 < minimumHbondDistance2)
      return -9900;

    Point3f targetCarbonPoint = target.getCarbonylCarbonAtomPoint();
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

    if (debugHbonds)
      System.out.println(" distOH=" + distOH +
                         " distCH=" + distCH +
                         " distCN=" + distCN +
                         " distON=" + distON +
                         " energy=" + energy);
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
    if (debugHbonds) 
      System.out.println("aminoBackboneHbondOffset=" +
                         aminoBackboneHbondOffset +
                         " amino:" +
                         monomers[indexAminoGroup].getSeqcodeString() +
                         " carbonyl:" +
                         monomers[indexCarbonylGroup].getSeqcodeString());
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
    if (debugHbonds)
      System.out.println("createResidueHydrogenBond(" + indexAminoGroup +
                         "," + indexCarbonylGroup);
    AminoMonomer donor = (AminoMonomer)monomers[indexAminoGroup];
    Atom nitrogen = donor.getNitrogenAtom();
    AminoMonomer recipient = (AminoMonomer)monomers[indexCarbonylGroup];
    Atom oxygen = recipient.getCarbonylOxygenAtom();
    Frame frame = model.mmset.frame;
    frame.bondAtoms(nitrogen, oxygen, order);
  }

  /*
   * If someone wants to work on this code for secondary structure
   * recognition that would be great
   *
   * miguel 2004 06 16
   */
  
  /*
   * New code for assigning secondary structure based on 
   * phi-psi angles instead of hydrogen bond patterns.
   *
   * old code is commented below the new.
   *
   * molvisions 2005 10 12
   *
   */

  void calculateStructures() {
    calcHydrogenBonds();
    char[] structureTags = new char[monomerCount];

    for (int i = 0; i < monomerCount-1; ++i) {
      float[] phi_psi = new float[2];
      AminoMonomer leadingResidue = (AminoMonomer)monomers[i];
      AminoMonomer trailingResidue = (AminoMonomer)monomers[i+1];
      phi_psi = calcPhiPsiAngles(leadingResidue, trailingResidue);
      if ( isHelix(phi_psi, 4) ) {
        structureTags[i] = '4';
        //structureTags[i+1] = '4';
      } else if (isSheet(phi_psi)) {
        structureTags[i] = 's';
        //structureTags[i+1] = 's';
      } else if (isTurn(phi_psi)) {
        structureTags[i] = 't';
        //structureTags[i+1] = 't';
      }
      else {
        structureTags[i] = '\0';
        //structureTags[i+1] = '\0';      
      }
    }
    
    // build alpha helix stretches
    for (int start = 0; start < monomerCount; ++start) {
     if (structureTags[start] == '4') {
       int end;
       for (end = start + 1;
            end < monomerCount && structureTags[end] == '4';
            ++end)
         {}
       end--;
       if (end >= start+3) {
         addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_HELIX,
                               start, end);
       }
       start = end;
     }
   }
   
    // build beta sheet stretches
    for (int start = 0; start < monomerCount; ++start) {
     if (structureTags[start] == 's') {
       int end;
       for (end = start + 1;
            end < monomerCount && structureTags[end] == 's';
            ++end)
         {}
       end--;
       if (end >= start+2) {
         addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_SHEET,
                               start, end);
       }
       start = end;
     }
   }
    
    // build turns
    for (int start = 0; start < monomerCount; ++start) {
     if (structureTags[start] == 't') {
       int end;
       for (end = start + 1;
            end < monomerCount && structureTags[end] == 't';
            ++end)
         {}
       end--;
       if (end >= start+2) {
         addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_TURN,
                               start, end);
       }
       start = end;
     }
   }

  }
  
  
  float[] calcPhiPsiAngles(AminoMonomer leadingResidue, AminoMonomer trailingResidue) {
    float[] phi_psi = new float[2];

    Point3f nitrogen1 = leadingResidue.getNitrogenAtomPoint();
    Point3f alphacarbon1 = leadingResidue.getLeadAtomPoint();
    Point3f carbon1 = leadingResidue.getCarbonylCarbonAtomPoint();
    Point3f nitrogen2 = trailingResidue.getNitrogenAtomPoint();
    Point3f alphacarbon2 = trailingResidue.getLeadAtomPoint();
    Point3f carbon2 = trailingResidue.getCarbonylCarbonAtomPoint();

    phi_psi[0] = Measurement.computeTorsion(carbon1, nitrogen2, alphacarbon2, carbon2);
    phi_psi[1] = Measurement.computeTorsion(nitrogen1, alphacarbon1, carbon1, nitrogen2);

    return phi_psi;
  }
  
  
  boolean isHelix(float[] phi_psi, int pitch) {
    float phi = phi_psi[0];
    float psi = phi_psi[1];
    if ( (phi >= -160) && (phi <= 0) && (psi >= -100) && (psi <= 45) ) 
      return true;
    else 
      return false;
  }

  boolean isSheet(float[] phi_psi) {
    float phi = phi_psi[0];
    float psi = phi_psi[1];
    if ( ( (phi >= -180) && (phi <= -10) && (psi >= 70) && (psi <= 180) ) || 
         ( (phi >= -180) && (phi <= -45) && (psi >= -180) && (psi <= -130) ) ||
         ( (phi >= 140) && (phi <= 180) && (psi >= 90) && (psi <= 180) ) ) 
      return true;
    else 
      return false;
  }

  boolean isTurn(float[] phi_psi) {
    float phi = phi_psi[0];
    float psi = phi_psi[1];
    if ( (phi >= 30) && (phi <= 90) && (psi >= -15) && (psi <= 95) ) 
      return true;
    else 
      return false;
  }


  /* 
   * old code for assigning SS
   *

  void calculateStructures() {
    calcHydrogenBonds();
    char[] structureTags = new char[monomerCount];

    findHelixes(structureTags);
    for (int iStart = 0; iStart < monomerCount; ++iStart) {
      if (structureTags[iStart] != '\0') {
        int iMax;
        for (iMax = iStart + 1;
             iMax < monomerCount && structureTags[iMax] != '\0';
             ++iMax)
          {}
        int iLast = iMax - 1;
        addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_HELIX,
                              iStart, iLast);
        iStart = iLast;
      }
    }

    // reset structureTags
    // for some reason if these are not reset, all helices are classified
    // as sheets. - tim 2205 10 12
        for (int i = monomerCount; --i >= 0; )
          structureTags[i] = '\0';

    findSheets(structureTags);
    
    if (debugHbonds)
      for (int i = 0; i < monomerCount; ++i)
        System.out.println("" + i + ":" + structureTags[i] +
                           " " + min1Indexes[i] + " " + min2Indexes[i]);
    for (int iStart = 0; iStart < monomerCount; ++iStart) {
      if (structureTags[iStart] != '\0') {
        int iMax;
        for (iMax = iStart + 1;
             iMax < monomerCount && structureTags[iMax] != '\0';
             ++iMax)
          {}
        int iLast = iMax - 1;
        addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_SHEET,
                              iStart, iLast);
        iStart = iLast;
      }
    }
  }

  void findHelixes(char[] structureTags) {
    findPitch(3, 4, '4', structureTags);
  }

  void findPitch(int minRunLength, int pitch, char tag, char[] tags) {
    int runLength = 0;
    for (int i = 0; i < monomerCount; ++i) {
      if (mainchainHbondOffsets[i] == pitch) {
        ++runLength;
        if (runLength == minRunLength)
          for (int j = minRunLength; --j >= 0; )
            tags[i - j] = tag;
        else if (runLength > minRunLength)
          tags[i] = tag;
      } else {
        runLength = 0;
      }
    }
  }

  void findSheets(char[] structureTags) {
    if (debugHbonds)
      System.out.println("findSheets(...)");
    for (int a = 0; a < monomerCount; ++a) {
      //if (structureTags[a] == '4')
       //continue;
      for (int b = 0; b < monomerCount; ++b) {
        //if (structureTags[b] == '4')
          //continue;
        // tim 2005 10 11
        // changed tests to reflect actual hbonding patterns in 
        // beta sheets.
        if ( ( isHbonded(a, b) && isHbonded(b+2, a) ) || 
           ( isHbonded(b, a) && isHbonded(a, b+2) ) )  {
          if (debugHbonds)
            System.out.println("parallel found a=" + a + " b=" + b);
          structureTags[a] = structureTags[b] = 
          structureTags[a+1] = structureTags[b+1] = 
          structureTags[a+2] = structureTags[b+2] = 'p';
         } else if (isHbonded(a, b) && isHbonded(b, a)) {
          if (debugHbonds)
            System.out.println("antiparallel found a=" + a + " b=" + b);
          structureTags[a] = structureTags[b] = 'a';
          // tim 2005 10 11
          // gap-filling feature: if n is sheet, and n+2 or n-2 are sheet, 
          // make n-1 and n+1 sheet as well.
          if ( (a+2 < monomerCount) && (b-2 > 0) && 
             (structureTags[a+2] == 'a') && (structureTags[b-2] == 'a') ) 
            structureTags[a+1] = structureTags[b-1] = 'a';
          if ( (b+2 < monomerCount) && (a-2 > 0) && 
             (structureTags[a-2] == 'a') && (structureTags[b+2] == 'a') ) 
            structureTags[a-1] = structureTags[b+1] = 'a';
        } 
        else if ( (isHbonded(a, b+1) && isHbonded(b, a+1) ) || 
                ( isHbonded(b+1, a) && isHbonded(a+1, b) ) ) {
          if (debugHbonds)
            System.out.println("antiparallel found a=" + a + " b=" + b);
          structureTags[a] = structureTags[a+1] =
          structureTags[b] = structureTags[b+1] = 'A';
        }
      }
    }
  }
  
  *
  * end old code for assigning SS.
  */
  
  boolean isHbonded(int indexDonor, int indexAcceptor) {
    if (indexDonor < 0 || indexDonor >= monomerCount ||
        indexAcceptor < 0 || indexAcceptor >= monomerCount)
      return false;
    return ((min1Indexes[indexDonor] == indexAcceptor &&
             min1Energies[indexDonor] <= -500) ||
            (min2Indexes[indexDonor] == indexAcceptor &&
             min2Energies[indexDonor] <= -500));
  }

}
