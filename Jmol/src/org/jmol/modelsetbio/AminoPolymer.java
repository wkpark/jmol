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
import org.jmol.modelset.Bond;
import org.jmol.modelset.HBond;
import org.jmol.modelset.Polymer;
import org.jmol.script.Token;
//import org.jmol.util.Escape;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JmolConstants;

//import java.util.ArrayList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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

  @Override
  boolean hasWingPoints() {
    return hasOAtoms;
  }

  //boolean Logger.debugging;

  @Override
  public void calcRasmolHydrogenBonds(Polymer polymer, BitSet bsA, BitSet bsB,
                                      List<Bond> vHBonds, int nMaxPerResidue,
                                      int[][][] min, boolean checkDistances) {
    if (polymer == null)
      polymer = this;
    if (!(polymer instanceof AminoPolymer))
      return;
    Point3f pt = new Point3f();
    Vector3f vNH = new Vector3f();
    AminoMonomer source;
    int[][] min1 = (min == null ? new int[2][3] : null);
    for (int i = 1; i < monomerCount; ++i) { //not first N
      if (min == null) {
        min1[0][0] = min1[1][0] = bioPolymerIndexInModel;
        min1[0][1] = min1[1][1] = Integer.MIN_VALUE;
        min1[0][2] = min1[1][2] = 0;
      } else {
        min1 = min[i];
      }
      if ((source = ((AminoMonomer) monomers[i])).getNHPoint(pt, vNH,
          checkDistances)) {
        boolean isInA = (bsA == null || bsA.get(source.getNitrogenAtom().index));
        if (!isInA)
          continue;
        checkRasmolHydrogenBond(source, polymer, i, pt,
            (isInA ? bsB : bsA), vHBonds, min1, checkDistances);
      }
    }
  }

  // max distance from RasMol 2.7.2.1.1  #define MaxHDist ((Long)2250*2250) 
  private final static float maxHbondAlphaDistance = 9;
  private final static float maxHbondAlphaDistance2 = maxHbondAlphaDistance
      * maxHbondAlphaDistance;
  // this next was fixed in Jmol 12.1.14; was just 0.5f (0.71*0.71) since Jmol 10.0.00
  private final static float minimumHbondDistance2 = 0.5f * 0.5f; 

  private void checkRasmolHydrogenBond(AminoMonomer source, Polymer polymer,
                                       int indexDonor, Point3f hydrogenPoint,
                                       BitSet bsB, List<Bond> vHBonds,
                                       int[][] min, boolean checkDistances) {
    Point3f sourceAlphaPoint = source.getLeadAtom();
    Point3f sourceNitrogenPoint = source.getNitrogenAtom();
    Atom nitrogen = source.getNitrogenAtom();
    int[] m;
    for (int i = polymer.monomerCount; --i >= 0;) {
      if (polymer == this
          && (i == indexDonor || i + 1 == indexDonor || i - 1 == indexDonor))
        continue;
      AminoMonomer target = (AminoMonomer) ((BioPolymer) polymer).monomers[i];
      Atom oxygen = target.getCarbonylOxygenAtom();
      if (bsB != null && !bsB.get(oxygen.index))
        continue;
      Point3f targetAlphaPoint = target.getLeadAtom();
      float dist2 = sourceAlphaPoint.distanceSquared(targetAlphaPoint);
      if (dist2 >= maxHbondAlphaDistance2)
        continue;
      int energy = calcHbondEnergy(sourceNitrogenPoint, hydrogenPoint, target,
          checkDistances);
      if (energy < min[0][2]) {
        m = min[1];
        min[1] = min[0];
        min[0] = m;
      } else if (energy < min[1][2]) {
        m = min[1];
      } else {
        continue;
      }
      m[0] = polymer.bioPolymerIndexInModel;
      m[1] = (energy < -500 ? i : -1 - i); // so that it will not be found, but we can check it
      m[2] = energy;
    }
    if (vHBonds != null)
      for (int i = 0; i < 2; i++)
        if (min[i][1] >= 0)
          addResidueHydrogenBond(nitrogen,
              ((AminoMonomer) ((AminoPolymer) polymer).monomers[min[i][1]])
                  .getCarbonylOxygenAtom(), (polymer == this ? indexDonor : -99), min[i][1],
              min[i][2] / 1000f, vHBonds);
  }

  /**
   * based on RasMol 2.7.2.1.1 model
   * 
   * checkDistances: 
   * 
   * When we are seriously looking for H bonds, we want to 
   * also check that distCN > distCH and that the OH distance
   * is less than 3 Angstroms. Otherwise that's just too strange 
   * a hydrogen bond. (We get hydrogen bonds from i to i+2, for example)
   * 
   * This check is skipped for an actual DSSP calc., where we want the 
   * original definition and are not actually creating hydrogen bonds
   * 
   *     H .......... O
   *     |            |
   *     |            |
   *     N            C
   * 
   * @param nitrogenPoint
   * @param hydrogenPoint
   * @param target
   * @param checkDistances
   * @return               energy in cal/mol or 0 (none)
   */
  private int calcHbondEnergy(Point3f nitrogenPoint, Point3f hydrogenPoint,
                              AminoMonomer target, boolean checkDistances) {
    Point3f targetOxygenPoint = target.getCarbonylOxygenAtom();

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

    double distOH = Math.sqrt(distOH2);
    double distCH = Math.sqrt(distCH2);
    double distCN = Math.sqrt(distCN2);
    double distON = Math.sqrt(distON2);

    int energy = HBond.getEnergy(distOH, distCH, distCN, distON);

    boolean isHbond = (energy < -500 
        && (!checkDistances || distCN > distCH && distOH <= 3.0f));
    /*
    if (isHbond)
      Logger.info("draw calcHydrogen"+ " ("+nitrogen.getInfo()+") {" + hydrogenPoint.x + " "
          + hydrogenPoint.y + " " + hydrogenPoint.z + "} #" + isHbond + " "
          + nitrogen.getInfo() + " " + target.getLeadAtom().getInfo()
          + " distOH=" + distOH + " distCH=" + distCH + " distCN=" + distCN
          + " distON=" + distON + " energy=" + energy);
    */
    return (!isHbond && checkDistances || energy < -9900 ? 0 : energy);
  }

  private void addResidueHydrogenBond(Atom nitrogen, Atom oxygen,
                                      int indexAminoGroup,
                                      int indexCarbonylGroup, float energy,
                                      List<Bond> vHBonds) {
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
   * New code for assigning secondary structure based on 
   * phi-psi angles instead of hydrogen bond patterns.
   *
   * molvisions 2005 10 12
   *
   */

  @Override
  public void calculateStructures() {
    if (structureList == null)
      structureList = model.getModelSet().getStructureList();
    char[] structureTags = new char[monomerCount];
    for (int i = 0; i < monomerCount - 1; ++i) {
      AminoMonomer leadingResidue = (AminoMonomer) monomers[i];
      AminoMonomer trailingResidue = (AminoMonomer) monomers[i + 1];
      float phi = trailingResidue.getGroupParameter(Token.phi);
      float psi = leadingResidue.getGroupParameter(Token.psi);
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
        Logger.debug((0 + this.monomers[0].getChainID()) + " aminopolymer:" + i
            + " " + trailingResidue.getGroupParameter(Token.phi) + ","
            + leadingResidue.getGroupParameter(Token.psi) + " "
            + structureTags[i]);
    }

    // build alpha helix stretches
    for (int start = 0; start < monomerCount; ++start) {
      if (structureTags[start] == '4') {
        int end;
        for (end = start + 1; end < monomerCount && structureTags[end] == '4'; ++end) {
        }
        end--;
        if (end >= start + 3) {
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_HELIX, null, 0,
              0, start, end);
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
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_HELIX, null, 0,
              0, start, end);
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
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_SHEET, null, 0,
              0, start, end);
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
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_TURN, null, 0,
              0, start, end);
        }
        start = end;
      }
    }
  }

  @Override
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

  @Override
  public void freeze() {
    hasOAtoms = checkWingAtoms();
  }

  @Override
  protected boolean calcPhiPsiAngles() {
    for (int i = 0; i < monomerCount - 1; ++i)
      calcPhiPsiAngles((AminoMonomer) monomers[i],
          (AminoMonomer) monomers[i + 1]);
    return true;
  }

  private void calcPhiPsiAngles(AminoMonomer residue1, AminoMonomer residue2) {

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

    residue2.setGroupParameter(Token.phi, Measure.computeTorsion(carbon1,
        nitrogen2, alphacarbon2, carbon2, true));
    residue1.setGroupParameter(Token.psi, Measure.computeTorsion(nitrogen1,
        alphacarbon1, carbon1, nitrogen2, true));
    // to offset omega so cis-prolines show up off the plane, 
    // we would have to use residue2 here:
    residue1.setGroupParameter(Token.omega, Measure.computeTorsion(
        alphacarbon1, carbon1, nitrogen2, alphacarbon2, true));
  }

  @Override
  protected float calculateRamachandranHelixAngle(int m, char qtype) {
    float psiLast = (m == 0 ? Float.NaN : monomers[m - 1]
        .getGroupParameter(Token.psi));
    float psi = monomers[m].getGroupParameter(Token.psi);
    float phi = monomers[m].getGroupParameter(Token.phi);
    float phiNext = (m == monomerCount - 1 ? Float.NaN : monomers[m + 1]
        .getGroupParameter(Token.phi));
    float psiNext = (m == monomerCount - 1 ? Float.NaN : monomers[m + 1]
        .getGroupParameter(Token.psi));
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
       *   cos(theta/2) = dq[i]\dq[i-1] = cos(dPsi/2)cos(dPhi/2) - sin(alpha)sin(dPsi/2)sin(dPhi/2)
       * 
       * and 
       * 
       *   dPhi = phi[i+1] - phi[i]
       *   dPsi = psi[i+1] - psi[i]
       * 
       */
      float dPhi = (float) ((phiNext - phi) / 2 * Math.PI / 180);
      float dPsi = (float) ((psiNext - psi) / 2 * Math.PI / 180);
      return (float) (180 / Math.PI * 2 * Math.acos(Math.cos(dPsi)
          * Math.cos(dPhi) - Math.cos(70 * Math.PI / 180) * Math.sin(dPsi)
          * Math.sin(dPhi)));
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
      return (psi - psiLast + phiNext - phi);
    }
  }

  private float[][] structureList; // kept in StateManager.globalSettings

  @Override
  public void setStructureList(float[][] structureList) {
    this.structureList = structureList;
  }

  /**
   * 
   * @param psi N-C-CA-N torsion for NEXT group
   * @param phi C-CA-N-C torsion for THIS group
   * @return whether this corresponds to a helix
   */
  private boolean isTurn(float psi, float phi) {
    return checkPhiPsi(structureList[JmolConstants.PROTEIN_STRUCTURE_TURN],
        psi, phi);
  }

  private boolean isSheet(float psi, float phi) {
    return checkPhiPsi(structureList[JmolConstants.PROTEIN_STRUCTURE_SHEET],
        psi, phi);
  }

  private boolean isHelix(float psi, float phi) {
    return checkPhiPsi(structureList[JmolConstants.PROTEIN_STRUCTURE_HELIX],
        psi, phi);
  }

  private static boolean checkPhiPsi(float[] list, float psi, float phi) {
    for (int i = 0; i < list.length; i += 4)
      if (phi >= list[i] && phi <= list[i + 1] && psi >= list[i + 2]
          && psi <= list[i + 3])
        return true;
    return false;
  }

  void setStructure(BitSet bs, byte type) {
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      int i2 = bs.nextClearBit(i);
      if (i2 < 0)
        i2 = monomerCount;
      addSecondaryStructure(type, null, 0, 0, i, i2 - 1);
      i = i2;
    }
  }

  ////////////////////// DSSP /////////////////////
  //
  //    W. Kabsch and C. Sander, Biopolymers, vol 22, 1983, pp 2577-2637
  //
  ////////////////////// DSSP /////////////////////



  protected static String calculateStructuresDssp(Polymer[] bioPolymers,
                                            int bioPolymerCount, boolean reportOnly, List<Bond> vHBonds) {

    return "not implemented";

  }
  
  
}
