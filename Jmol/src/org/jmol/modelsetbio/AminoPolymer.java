/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:44:28 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7224 $
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

import org.jmol.api.JmolEdge;
import org.jmol.modelset.Atom;
import org.jmol.modelset.HBond;
import org.jmol.modelset.Polymer;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.viewer.JmolConstants;

import java.util.BitSet;
import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class AminoPolymer extends AlphaPolymer {

  // the primary offset within the same mainchain;
  /*
  private short[] mainchainHbondOffsets;
  private short[] min1Indexes;
  private short[] min1Energies;
  private short[] min2Indexes;
  private short[] min2Energies;
  */

  AminoPolymer(Monomer[] monomers) {
    super(monomers);
    type = TYPE_AMINO;
  }

  private boolean hasOAtoms;
  boolean hasWingPoints() { return hasOAtoms; }

  //boolean debugHbonds;

  public void calcRasmolHydrogenBonds(Polymer polymer, BitSet bsA, BitSet bsB, Vector vHBonds, int nMaxPerResidue) {
    Point3f pt = new Point3f();
    Vector3f vNH = new Vector3f();
    boolean intraChain = (polymer == null);
    if (intraChain)
      polymer = this;
    AminoMonomer source;
    AminoPolymer p = (AminoPolymer)polymer;
    for (int i = 1; i < p.monomerCount; ++i) { //not first N
      if ((source = ((AminoMonomer)p.monomers[i])).getNHPoint(pt, vNH)) {
        boolean isInA = (bsA.get(source.getNitrogenAtom().index));
        if (!isInA)
          continue;
        checkRasmolHydrogenBond(source, (intraChain ? i : -100), pt, (isInA ? bsB : bsA), 
            vHBonds, nMaxPerResidue);
      }
    }
  }

  private final static float maxHbondAlphaDistance = 9;
  private final static float maxHbondAlphaDistance2 =
    maxHbondAlphaDistance * maxHbondAlphaDistance;
  private final static float minimumHbondDistance2 = 0.5f; // note: RasMol is 1/2 this. RMH

  private void checkRasmolHydrogenBond(AminoMonomer source, int indexDonor, Point3f hydrogenPoint,
                         BitSet bsB, Vector vHBonds, int nMaxPerResidue) {
    Point3f sourceAlphaPoint = source.getLeadAtom();
    Point3f sourceNitrogenPoint = source.getNitrogenAtom();
    Atom nitrogen = source.getNitrogenAtom();
    int energyMin1 = 0;
    int energyMin2 = 0;
    int indexMin1 = -1;
    int indexMin2 = -1;
    for (int i = monomerCount; --i >= 0; ) {
      if ((i == indexDonor || (i+1) == indexDonor) || (i-1) == indexDonor)
        continue;
      AminoMonomer target = (AminoMonomer)monomers[i];
      Atom oxygen = target.getCarbonylOxygenAtom();
      if (!bsB.get(oxygen.index))
        continue;
      Point3f targetAlphaPoint = target.getLeadAtom();
      float dist2 = sourceAlphaPoint.distanceSquared(targetAlphaPoint);
      if (dist2 > maxHbondAlphaDistance2)
        continue;
      int energy = calcHbondEnergy(source.getNitrogenAtom(), sourceNitrogenPoint, hydrogenPoint, target);
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
      addResidueHydrogenBond(nitrogen, ((AminoMonomer)monomers[indexMin1]).getCarbonylOxygenAtom(), indexDonor, indexMin1, energyMin1/1000f, vHBonds);
      if (indexMin2 >= 0 && nMaxPerResidue > 1) {
        addResidueHydrogenBond(nitrogen,((AminoMonomer)monomers[indexMin2]).getCarbonylOxygenAtom(), indexDonor, indexMin2, energyMin2/1000f, vHBonds);
      }
    }
  }

  //private int hPtr = 0;
  private int calcHbondEnergy(Atom nitrogen, Point3f nitrogenPoint,
                      Point3f hydrogenPoint, AminoMonomer target) {
    Point3f targetOxygenPoint = target.getCarbonylOxygenAtom();

    /*
     * the following were changed from "return -9900" to "return 0"
     * Bob Hanson 8/30/06
     */
    if (targetOxygenPoint == null)
      return 0;
    float distON2 = targetOxygenPoint.distanceSquared(nitrogenPoint);
    if (distON2 < minimumHbondDistance2)
      return 0;

    float distOH2 = targetOxygenPoint.distanceSquared(hydrogenPoint);
    if (distOH2 < minimumHbondDistance2)
      return 0;

    Point3f targetCarbonPoint = target.getCarbonylCarbonAtom();
    float distCH2 = targetCarbonPoint.distanceSquared(hydrogenPoint);
    if (distCH2 < minimumHbondDistance2)
      return 0;

    float distCN2 = targetCarbonPoint.distanceSquared(nitrogenPoint);
    if (distCN2 < minimumHbondDistance2)
      return 0;
    
    /*
     * I'm adding these two because they just makes sense -- Bob Hanson
     */
    
    double distOH = Math.sqrt(distOH2);
    double distCH = Math.sqrt(distCH2);
    double distCN = Math.sqrt(distCN2);
    double distON = Math.sqrt(distON2);

    int energy = HBond.getEnergy(distOH, distCH, distCN, distON);

    boolean isHbond = (distCN2 > distCH2 && distOH <= 3.0f && energy <= -500);
    /*
    if (isHbond)
      Logger.info("draw calcHydrogen"+ " ("+nitrogen.getInfo()+") {" + hydrogenPoint.x + " "
          + hydrogenPoint.y + " " + hydrogenPoint.z + "} #" + isHbond + " "
          + nitrogen.getInfo() + " " + target.getLeadAtom().getInfo()
          + " distOH=" + distOH + " distCH=" + distCH + " distCN=" + distCN
          + " distON=" + distON + " energy=" + energy);
    */
    return (!isHbond ? 0 : energy < -9900 ? -9900 : energy);
  }

  private void addResidueHydrogenBond(Atom nitrogen, Atom oxygen, int indexAminoGroup,
                                      int indexCarbonylGroup, float energy,
                                      Vector vHBonds) {
    int order;
    switch (indexAminoGroup - indexCarbonylGroup) {
    case 2:
      order = JmolEdge.BOND_H_PLUS_2;
      break;
    case 3:
      order = JmolEdge.BOND_H_PLUS_3;
      break;
    case 4:
      order = JmolEdge.BOND_H_PLUS_4;
      break;
    case 5:
      order = JmolEdge.BOND_H_PLUS_5;
      break;
    case -3:
      order = JmolEdge.BOND_H_MINUS_3;
      break;
    case -4:
      order = JmolEdge.BOND_H_MINUS_4;
      break;
    default:
      order = JmolEdge.BOND_H_CALC;
    }
    vHBonds.add(new HBond(nitrogen, oxygen, order, energy));
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

  public void calculateStructures() {
    //deprecated: calcHydrogenBonds();
    //System.out.println("calculateStructures for model " + this.model.getModelIndex());
    char[] structureTags = new char[monomerCount];
    for (int i = 0; i < monomerCount - 1; ++i) {
      AminoMonomer leadingResidue = (AminoMonomer) monomers[i];
      AminoMonomer trailingResidue = (AminoMonomer) monomers[i + 1];
      float phi = trailingResidue.getPhi();
      float psi = leadingResidue.getPsi();
      if (isHelix(psi, phi)) {
        //this next is just Bob's attempt to separate different helices
        //it is CONSERVATIVE -- it displays fewer helices than before
        //thus allowing more turns and (presumably) better rockets.

        structureTags[i] = (phi < 0 && psi < 25 ? '4' : '3');
      } else if (isSheet(psi, phi)) {
        structureTags[i] = 's';
      } else if (isTurn(psi, phi)) {
        structureTags[i] = 't';
      } else {
        structureTags[i] = 'n';
      }

      if (Logger.debugging)
        Logger.debug((0+this.monomers[0].getChainID()) + " aminopolymer:" + i
            + " " + trailingResidue.getPhi() + "," + leadingResidue.getPsi() + " " + structureTags[i]);
    }

    // build alpha helix stretches
    for (int start = 0; start < monomerCount; ++start) {
      if (structureTags[start] == '4') {
        int end;
        for (end = start + 1; end < monomerCount && structureTags[end] == '4'; ++end) {
        }
        end--;
        if (end >= start + 3) {
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_HELIX, null, 0, 0, start,
              end);
        }
        start = end;
      }
    }

    for (int start = 0; start < monomerCount; ++start) {
      if (structureTags[start] == '3') {
        int end;
        for (end = start + 1; end < monomerCount && structureTags[end] == '3'; ++end) {
        }
        end--;
        if (end >= start + 3) {
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_HELIX, null, 0, 0, start,
              end);
        }
        start = end;
      }
    }

    // build beta sheet stretches
    for (int start = 0; start < monomerCount; ++start) {
      if (structureTags[start] == 's') {
        int end;
        for (end = start + 1; end < monomerCount && structureTags[end] == 's'; ++end) {
        }
        end--;
        if (end >= start + 2) {
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_SHEET, null, 0, 0, start,
              end);
        }
        start = end;
      }
    }

    // build turns
    for (int start = 0; start < monomerCount; ++start) {
      if (structureTags[start] == 't') {
        int end;
        for (end = start + 1; end < monomerCount && structureTags[end] == 't'; ++end) {
        }
        end--;
        if (end >= start + 2) {
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_TURN, null, 0, 0, start,
              end);
        }
        start = end;
      }
    }
  }

  protected void resetHydrogenPoints() {
    ProteinStructure ps;
    ProteinStructure psLast = null;
    for (int i = 0; i < monomerCount; i++) {
      if ((ps = getProteinStructure(i)) != null && ps != psLast)
        (psLast = ps).resetAxes();
      ((AminoMonomer) monomers[i]).resetHydrogenPoint();
    }
  }

  private boolean checkWingAtoms() {
    for (int i = 0; i < monomerCount; ++i)
      if (!((AminoMonomer) monomers[i]).hasOAtom())
        return false;
    return true;
  }
  
  public void freeze() {
    hasOAtoms = checkWingAtoms();
    calcPhiPsiAngles();
  }
  
  protected boolean calcPhiPsiAngles() {
    for (int i = 0; i < monomerCount - 1; ++i)
      calcPhiPsiAngles((AminoMonomer) monomers[i], (AminoMonomer) monomers[i + 1]);
    return true;
  }
  
  private void calcPhiPsiAngles(AminoMonomer residue1,
                        AminoMonomer residue2) {
    
    /*
     * G N Ramachandran and V. Sasisekharan,
     * "Conformation of Polypeptides and Proteins" 
     * in Advances in Protein Chemistry, D.C. Rees, Ed.,
     * Volume 23, Elsevier, 1969, p 284
     * 
     *   N1-Ca1-C1-N2-Ca2-C2
     *    residue1  residue2
     *   low -----------> high   atomIndex
     * 
     * UNfortunately, omega is defined for residue 1 (page 294)
     * such that the residue having unusual omega is not the
     * proline itself but the one prior to it.
     * 
     */
    Point3f nitrogen1 = residue1.getNitrogenAtom();
    Point3f alphacarbon1 = residue1.getLeadAtom();
    Point3f carbon1 = residue1.getCarbonylCarbonAtom();
    Point3f nitrogen2 = residue2.getNitrogenAtom();
    Point3f alphacarbon2 = residue2.getLeadAtom();
    Point3f carbon2 = residue2.getCarbonylCarbonAtom();

    residue2.setPhi(Measure.computeTorsion(carbon1, nitrogen2,
                                            alphacarbon2, carbon2, true));
    residue1.setPsi(Measure.computeTorsion(nitrogen1, alphacarbon1,
      carbon1, nitrogen2, true));
    // to offset omega so cis-prolines show up off the plane, 
    // we would have to use residue2 here:
    residue1.setOmega(Measure.computeTorsion(alphacarbon1,
	        carbon1, nitrogen2, alphacarbon2, true));
  }
  
  protected float calculateRamachandranHelixAngle(int m, char qtype) {
    float psiLast = (m == 0 ? Float.NaN : monomers[m - 1].getPsi());
    float psi = monomers[m].getPsi();
    float phi = monomers[m].getPhi();
    float phiNext = (m == monomerCount - 1 ? Float.NaN
        : monomers[m + 1].getPhi());
    float psiNext = (m == monomerCount - 1 ? Float.NaN
        : monomers[m + 1].getPsi());
    switch (qtype) {
    default:
    case 'p':
    case 'r':
    case 'P':
      /* 
       * an approximation by Bob Hanson and Steven Braun 7/7/2009
       * 
       * P-straightness utilizes phi[i], psi[i] and phi[i+1], psi[i+1]
       * and is approximated as:
       * 
       *   1 - 2 acos(|cos(theta/2)|) / PI
       * 
       * where 
       * 
       *   cos(theta/2) = q[i]\q[i-1] = cos(dPsi/2)cos(dPhi/2) - sin(alpha)sin(dPsi/2)sin(dPhi/2)
       * 
       * and 
       * 
       *   dPhi = phi[i+1] - phi[i]
       *   dPsi = psi[i+1] - psi[i]
       * 
       */ 
      float dPhi = (float) ((phiNext - phi) / 2 * Math.PI / 180);
      float dPsi = (float) ((psiNext - psi) / 2 * Math.PI / 180);
      return (float) (180 / Math.PI * 2 * Math.acos(Math.cos(dPsi) * Math.cos(dPhi) - Math.cos(70*Math.PI/180)* Math.sin(dPsi) * Math.sin(dPhi)));
    case 'c':
    case 'C':
      /* an approximation by Bob Hanson and Dan Kohler, 7/2008
       * 
       * The near colinearity of the C_alpha-C and N'-C_alpha'
       * allows for the remarkably simple relationship
       * 
       *  psi[i] - psi[i-1] + phi[i+1] - phi[i]
       *
       */
      return  (psi - psiLast + phiNext - phi);
    }
  }
  
  /**
   * 
   * @param psi N-C-CA-N torsion for NEXT group
   * @param phi C-CA-N-C torsion for THIS group
   * @return whether this corresponds to a helix
   */
  private static boolean isHelix(float psi, float phi) {
    return (phi >= -160) && (phi <= 0) && (psi >= -100) && (psi <= 45);
  }

  private static boolean isSheet(float psi, float phi) {
    return
      ( (phi >= -180) && (phi <= -10) && (psi >= 70) && (psi <= 180) ) || 
      ( (phi >= -180) && (phi <= -45) && (psi >= -180) && (psi <= -130) ) ||
      ( (phi >= 140) && (phi <= 180) && (psi >= 90) && (psi <= 180) );
  }

  private static boolean isTurn(float psi, float phi) {
    return (phi >= 30) && (phi <= 90) && (psi >= -15) && (psi <= 95);
  }


  /* 
   * old code for assigning SS
   *

   void calculateStructures() {
   calcHydrogenBonds();
   char[] structureTags = new char[monomerCount];

   findHelixes(structureTags);
   for (int iStart = 0; iStart < monomerCount; ++iStart) {
   if (structureTags[iStart] != 'n') {
   int iMax;
   for (iMax = iStart + 1;
   iMax < monomerCount && structureTags[iMax] != 'n';
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
   structureTags[i] = 'n';

   findSheets(structureTags);
   
   if (debugHbonds)
   for (int i = 0; i < monomerCount; ++i)
   Logger.debug("" + i + ":" + structureTags[i] +
   " " + min1Indexes[i] + " " + min2Indexes[i]);
   for (int iStart = 0; iStart < monomerCount; ++iStart) {
   if (structureTags[iStart] != 'n') {
   int iMax;
   for (iMax = iStart + 1;
   iMax < monomerCount && structureTags[iMax] != 'n';
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
   Logger.debug("findSheets(...)");
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
   Logger.debug("parallel found a=" + a + " b=" + b);
   structureTags[a] = structureTags[b] = 
   structureTags[a+1] = structureTags[b+1] = 
   structureTags[a+2] = structureTags[b+2] = 'p';
   } else if (isHbonded(a, b) && isHbonded(b, a)) {
   if (debugHbonds)
   Logger.debug("antiparallel found a=" + a + " b=" + b);
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
   Logger.debug("antiparallel found a=" + a + " b=" + b);
   structureTags[a] = structureTags[a+1] =
   structureTags[b] = structureTags[b+1] = 'A';
   }
   }
   }
   }
   
   
   boolean isHbonded(int indexDonor, int indexAcceptor) {
   if (indexDonor < 0 || indexDonor >= monomerCount ||
   indexAcceptor < 0 || indexAcceptor >= monomerCount)
   return false;
   return ((min1Indexes[indexDonor] == indexAcceptor &&
   min1Energies[indexDonor] <= -500) ||
   (min2Indexes[indexDonor] == indexAcceptor &&
   min2Energies[indexDonor] <= -500));
   }

   *
   * end old code for assigning SS.
   */

}
