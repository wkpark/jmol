/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.modelset;



import javajs.util.ArrayUtil;

import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.JmolEdge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;

import org.jmol.viewer.JC;
import org.jmol.java.BS;
import org.jmol.modelset.Bond.BondSet;
import org.jmol.script.T;

abstract public class BondCollection extends AtomCollection {

  @Override
  protected void releaseModelSet() {
    releaseModelSetBC();
  }

  protected void releaseModelSetBC() {
    bonds = null;
    freeBonds = null;
    releaseModelSetAC();
  }

  //note: Molecules is set up to only be calculated WHEN NEEDED
  protected JmolMolecule[] molecules;
  protected int moleculeCount;

  protected void resetMolecules() {
    molecules = null;
    moleculeCount = 0;
  }

  public Bond[] bonds;
  public int bondCount;
  
  public Bond getBondAt(int bondIndex) {
    return bonds[bondIndex];
  }

  public BondIterator getBondIteratorForType(int bondType, BS bsAtoms) {
    //Dipoles, Sticks
    return new BondIteratorSelected(bonds, bondCount, bondType, bsAtoms, 
        viewer.getBoolean(T.bondmodeor));
  }

  public BondIterator getBondIterator(BS bsBonds) {
    //Sticks
    return new BondIteratorSelected(bonds, bondCount, JmolEdge.BOND_ORDER_NULL, bsBonds, false);
  }
  
  public Atom getBondAtom1(int i) {
    return bonds[i].atom1;
  }

  public Atom getBondAtom2(int i) {
    return bonds[i].atom2;
  }

  public float getBondRadius(int i) {
    return bonds[i].getRadius();
  }

  public int getBondOrder(int i) {
    return bonds[i].order;
  }

  public short getBondColix1(int i) {
    return bonds[i].getColix1();
  }

  public short getBondColix2(int i) {
    return bonds[i].getColix2();
  }
  
  public int getBondModelIndex(int i) {
    return bonds[i].atom1.modelIndex;
  }

  /**
   * for general use
   * 
   * @param modelIndex the model of interest or -1 for all
   * @return the actual number of connections
   */
  protected int getBondCountInModel(int modelIndex) {
    int n = 0;
    for (int i = bondCount; --i >= 0;)
      if (bonds[i].atom1.modelIndex == modelIndex)
        n++;
    return n;
  }

  public BS getBondsForSelectedAtoms(BS bsAtoms, boolean bondSelectionModeOr) {
    BS bs = new BS();
    for (int iBond = 0; iBond < bondCount; ++iBond) {
      Bond bond = bonds[iBond];
      boolean isSelected1 = bsAtoms.get(bond.atom1.index);
      boolean isSelected2 = bsAtoms.get(bond.atom2.index);
      if ((!bondSelectionModeOr & isSelected1 & isSelected2)
          || (bondSelectionModeOr & (isSelected1 | isSelected2)))
        bs.set(iBond);
    }
    return bs;
  }

  public Bond bondAtoms(Atom atom1, Atom atom2, int order, short mad, BS bsBonds, float energy, boolean addGroup, boolean isNew) {
    // this method used when a bond must be flagged as new
    Bond bond = getOrAddBond(atom1, atom2, order, mad, bsBonds, energy, true);
    if (isNew) {
      bond.order |= JmolEdge.BOND_NEW;
      if (addGroup) {
        // for adding hydrogens
        atom1.group = atom2.group;
        atom1.group.addAtoms(atom1.index);
      }
    }
    return bond;
  }

  protected final static int BOND_GROWTH_INCREMENT = 250;

  private Bond getOrAddBond(Atom atom, Atom atomOther, int order, short mad,
                            BS bsBonds, float energy, boolean overrideBonding) {
    int i;
    if (order == JmolEdge.BOND_ORDER_NULL || order == JmolEdge.BOND_ORDER_ANY)
      order = 1;
    if (atom.isBonded(atomOther)) {
      i = atom.getBond(atomOther).index;
      if (overrideBonding) {
        bonds[i].setOrder(order);
        bonds[i].setMad(mad);
        if (bonds[i] instanceof HBond)
          ((HBond) bonds[i]).energy = energy;
      }
    } else {
      if (bondCount == bonds.length)
        bonds = (Bond[]) ArrayUtil.arrayCopyObject(bonds, bondCount
            + BOND_GROWTH_INCREMENT);
      i = setBond(bondCount++,
          bondMutually(atom, atomOther, order, mad, energy)).index;
    }
    if (bsBonds != null)
      bsBonds.set(i);
    return bonds[i];
  }

  protected Bond setBond(int index, Bond bond) {
    return bonds[bond.index = index] = bond;
  }

  protected Bond bondMutually(Atom atom, Atom atomOther, int order, short mad, float energy) {
    Bond bond;
    if (Bond.isOrderH(order)) {
      bond = new HBond(atom, atomOther, order, mad, C.INHERIT_ALL, energy);
    } else {
      bond = new Bond(atom, atomOther, order, mad, C.INHERIT_ALL);
    }
    addBondToAtom(atom, bond);
    addBondToAtom(atomOther, bond);
    return bond;
  }

  private void addBondToAtom(Atom atom, Bond bond) {
    if (atom.bonds == null) {
      atom.bonds = new Bond[1];
      atom.bonds[0] = bond;
    } else {
      atom.bonds = addToBonds(bond, atom.bonds);
    }
  }

  protected final static int MAX_BONDS_LENGTH_TO_CACHE = 5;
  protected final static int MAX_NUM_TO_CACHE = 200;
  protected int[] numCached = new int[MAX_BONDS_LENGTH_TO_CACHE];
  protected Bond[][][] freeBonds = new Bond[MAX_BONDS_LENGTH_TO_CACHE][][];
  {
    for (int i = MAX_BONDS_LENGTH_TO_CACHE; --i > 0;)
      // .GT. 0
      freeBonds[i] = new Bond[MAX_NUM_TO_CACHE][];
  }

  private Bond[] addToBonds(Bond newBond, Bond[] oldBonds) {
    Bond[] newBonds;
    if (oldBonds == null) {
      if (numCached[1] > 0)
        newBonds = freeBonds[1][--numCached[1]];
      else
        newBonds = new Bond[1];
      newBonds[0] = newBond;
    } else {
      int oldLength = oldBonds.length;
      int newLength = oldLength + 1;
      if (newLength < MAX_BONDS_LENGTH_TO_CACHE && numCached[newLength] > 0)
        newBonds = freeBonds[newLength][--numCached[newLength]];
      else
        newBonds = new Bond[newLength];
      newBonds[oldLength] = newBond;
      for (int i = oldLength; --i >= 0;)
        newBonds[i] = oldBonds[i];
      if (oldLength < MAX_BONDS_LENGTH_TO_CACHE
          && numCached[oldLength] < MAX_NUM_TO_CACHE)
        freeBonds[oldLength][numCached[oldLength]++] = oldBonds;
    }
    return newBonds;
  }

  ////// bonding methods //////

  public int addHBond(Atom atom1, Atom atom2, int order, float energy) {
    // from autoHbond and BioModel.getRasmolHydrogenBonds
    if (bondCount == bonds.length)
      bonds = (Bond[]) ArrayUtil.arrayCopyObject(bonds, bondCount
          + BOND_GROWTH_INCREMENT);
    return setBond(bondCount++, bondMutually(atom1, atom2, order, (short) 1,
        energy)).index;
  }

  protected static short getBondOrderFull(float bondingRadiusA,
                             float bondingRadiusB, float distance2,
                             float minBondDistance2, float bondTolerance) {
    if (bondingRadiusA == 0 || bondingRadiusB == 0 || distance2 < minBondDistance2)
      return 0;
    float maxAcceptable = bondingRadiusA + bondingRadiusB + bondTolerance;
    float maxAcceptable2 = maxAcceptable * maxAcceptable;
    return (distance2 > maxAcceptable2 ? (short) 0 : (short) 1);
  }

  private boolean haveWarned = false;

  protected boolean checkValencesAndBond(Atom atomA, Atom atomB, int order, short mad,
                            BS bsBonds) {
    if (atomA.getCurrentBondCount() > JC.MAXIMUM_AUTO_BOND_COUNT
        || atomB.getCurrentBondCount() > JC.MAXIMUM_AUTO_BOND_COUNT) {
      if (!haveWarned)
        Logger.warn("maximum auto bond count reached");
      haveWarned = true;
      return false;
    }
    int formalChargeA = atomA.getFormalCharge();
    if (formalChargeA != 0) {
      int formalChargeB = atomB.getFormalCharge();
      if ((formalChargeA < 0 && formalChargeB < 0)
          || (formalChargeA > 0 && formalChargeB > 0))
        return false;
    }
    if (atomA.altloc != atomB.altloc
        && atomA.altloc != '\0' && atomB.altloc != '\0')
      return false;
    getOrAddBond(atomA, atomB, order, mad, bsBonds, 0, false);
    return true;
  }

  protected void deleteAllBonds2() {
    viewer.setShapeProperty(JC.SHAPE_STICKS, "reset", null);
    for (int i = bondCount; --i >= 0;) {
      bonds[i].deleteAtomReferences();
      bonds[i] = null;
    }
    bondCount = 0;
  }
  

  protected short defaultCovalentMad;

  /**
   * When creating a new bond, determine bond diameter from order
   * 
   * @param order
   * @return if hydrogen bond, default to 1; otherwise 0 (general default)
   */
  public short getDefaultMadFromOrder(int order) {
    return (short) (Bond.isOrderH(order) ? 1
        : (order & JmolEdge.BOND_STRUT) != 0 ? (int) Math.floor(viewer
            .getFloat(T.strutdefaultradius) * 2000) : defaultCovalentMad);
  }

  protected int[] deleteConnections(float minDistance, float maxDistance,
                                    int order, BS bsA, BS bsB,
                                    boolean isBonds, boolean matchNull,
                                    float minDistanceSquared,
                                    float maxDistanceSquared) {
    boolean minDistanceIsFractionRadius = (minDistance < 0);
    boolean maxDistanceIsFractionRadius = (maxDistance < 0);
    float dAB = 0;
    float dABcalc = 0;
    if (minDistanceIsFractionRadius)
      minDistance = -minDistance;
    if (maxDistanceIsFractionRadius)
      maxDistance = -maxDistance;
    BS bsDelete = new BS();
    int nDeleted = 0;
    int newOrder = order |= JmolEdge.BOND_NEW;
    if (!matchNull && Bond.isOrderH(order))
      order = JmolEdge.BOND_HYDROGEN_MASK;
    BS bsBonds;
    if (isBonds) {
      bsBonds = bsA;
    } else {
      bsBonds = new BS();
      for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1)) {
        Atom a = atoms[i];
        if (a.bonds != null)
          for (int j = a.bonds.length; --j >= 0; )
            if (bsB.get(a.getBondedAtomIndex(j)))
              bsBonds.set(a.bonds[j].index);
      }
    }
    for (int i = bsBonds.nextSetBit(0); i < bondCount && i >= 0; i = bsBonds.nextSetBit(i + 1)) {
      Bond bond = bonds[i];
      Atom atom1 = bond.atom1;
      Atom atom2 = bond.atom2;
        float distanceSquared = atom1.distanceSquared(atom2);
        if (minDistanceIsFractionRadius || maxDistanceIsFractionRadius) {
          dAB = atom1.distance(atom2);
          dABcalc = atom1.getBondingRadiusFloat()
              + atom2.getBondingRadiusFloat();
        }
        if ((minDistanceIsFractionRadius ? dAB < dABcalc * minDistance
            : distanceSquared < minDistanceSquared)
            || (maxDistanceIsFractionRadius ? dAB > dABcalc * maxDistance
                : distanceSquared > maxDistanceSquared))
          continue;
        if (matchNull
            || newOrder == (bond.order & ~JmolEdge.BOND_SULFUR_MASK | JmolEdge.BOND_NEW)
            || (order & bond.order & JmolEdge.BOND_HYDROGEN_MASK) != 0) {
          bsDelete.set(i);
          nDeleted++;
        }
    }
    if (nDeleted > 0)
      dBm(bsDelete, false);
    return new int[] { 0, nDeleted };
  }
  
  /**
   * send request up to ModelCollection level.
   * Done this way to avoid JavaScript super call
   * 
   * @param bsBonds
   * @param isFullModel
   */
  protected void dBm(BS bsBonds, boolean isFullModel) {
    ((ModelCollection) this).deleteBonds(bsBonds, isFullModel);
  }

  protected void dBb(BS bsBond, boolean isFullModel) {
    int iDst = bsBond.nextSetBit(0);
    if (iDst < 0)
      return;
    resetMolecules();
    int modelIndexLast = -1;
    int n = bsBond.cardinality();
    for (int iSrc = iDst; iSrc < bondCount; ++iSrc) {
      Bond bond = bonds[iSrc];
      if (n > 0 && bsBond.get(iSrc)) {
        n--;
        if (!isFullModel) {
          int modelIndex = bond.atom1.modelIndex;
          if (modelIndex != modelIndexLast)
            ((ModelCollection) this).models[modelIndexLast = modelIndex]
                .resetBoundCount();
        }
        bond.deleteAtomReferences();
      } else {
        setBond(iDst++, bond);
      }
    }
    for (int i = bondCount; --i >= iDst;)
      bonds[i] = null;
    bondCount = iDst;
    BS[] sets = (BS[]) viewer.getShapeProperty(
        JC.SHAPE_STICKS, "sets");
    if (sets != null)
      for (int i = 0; i < sets.length; i++)
        BSUtil.deleteBits(sets[i], bsBond);
    BSUtil.deleteBits(bsAromatic, bsBond);
  }


  /*
   * aromatic single/double bond assignment 
   * by Bob Hanson, hansonr@stolaf.edu, Oct. 2007
   * Jmol 11.3.29.
   * 
   * This algorithm assigns alternating single/double bonds to all 
   * sets of bonds of type AROMATIC in a system. Any bonds already
   * assigned AROMATICSINGLE or AROMATICDOUBLE by the user are preserved.
   * 
   * In this way the user can assign ONE bond, and Jmol will take it from
   * there.
   * 
   * The algorithm is highly recursive.
   * 
   * We track two bond bitsets: bsAromaticSingle and bsAromaticDouble.
   *  
   * Loop through all aromatic bonds. 
   *   If unassigned, assignAromaticDouble(Bond bond).
   *   If unsuccessful, assignAromaticSingle(Bond bond).
   * 
   * assignAromaticDouble(Bond bond):
   * 
   *   Each of the two atoms must have exactly one double bond.
   *   
   *   bsAromaticDouble.set(thisBond)
   *   
   *   For each aromatic bond connected to each atom that is not
   *   already assigned AROMATICSINGLE or AROMATICDOUBLE:
   *   
   *     assignAromaticSingle(Bond bond)
   *     
   *   If unsuccessful, bsAromaticDouble.clear(thisBond) and 
   *   return FALSE, otherwise return TRUE.
   * 
   * assignAromaticSingle(Bond bond):
   * 
   *   Each of the two atoms must have exactly one double bond.
   *   
   *   bsAromaticSingle.set(thisBond)
   *   
   *   For each aromatic bond connected to this atom that is not
   *   already assigned:
   *   
   *     for one: assignAromaticDouble(Bond bond) 
   *     the rest: assignAromaticSingle(Bond bond)
   *     
   *   If two AROMATICDOUBLE bonds to the same atom are found
   *   or unsuccessful in assigning AROMATICDOUBLE or AROMATICSINGLE, 
   *   bsAromaticSingle.clear(thisBond) and 
   *   return FALSE, otherwise return TRUE.
   *   
   * The process continues until all bonds are processed. It is quite
   * possible that the first assignment will fail either because somewhere
   * down the line the user has assigned an incompatible AROMATICDOUBLE or
   * AROMATICSINGLE bond. 
   * 
   * This is no problem though, because the assignment is self-correcting, 
   * and in the second pass the process will be opposite, and success will
   * be achieved.
   * 
   * It is possible that no correct assignment is possible because the structure
   * has no valid closed-shell Lewis structure. In that case, AROMATICSINGLE 
   * bonds will be assigned to problematic areas.  
   * 
   * Bob Hanson -- 10/2007
   * 
   */

  private BS bsAromaticSingle;
  private BS bsAromaticDouble;
  protected BS bsAromatic = new BS();

  public void resetAromatic() {
    for (int i = bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      if (bond.isAromatic())
        bond.setOrder(JmolEdge.BOND_AROMATIC);
    }
  }
  
  public void assignAromaticBonds() {
    assignAromaticBondsBs(true, null);
  }

  /**
   * algorithm discussed above.
   * 
   * @param isUserCalculation   if set, don't reset the base aromatic bitset
   *                            and do report changes to STICKS as though this
   *                            were a bondOrder command.
   * @param bsBonds  passed to us by autoBond routine
   */
  protected void assignAromaticBondsBs(boolean isUserCalculation, BS bsBonds) {
    // bsAromatic tracks what was originally in the file, but
    // individual bonds are cleared if the connect command has been used.
    // in this way, users can override the file designations.
    if (!isUserCalculation)
      bsAromatic = new BS();

    //set up the two temporary bitsets and reset bonds.

    bsAromaticSingle = new BS();
    bsAromaticDouble = new BS();
    boolean isAll = (bsBonds == null);
    int i0 = (isAll ? bondCount - 1 : bsBonds.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsBonds.nextSetBit(i + 1))) {
        Bond bond = bonds[i];
        if (bsAromatic.get(i))
          bond.setOrder(JmolEdge.BOND_AROMATIC);
        switch (bond.order & ~JmolEdge.BOND_NEW) {
        case JmolEdge.BOND_AROMATIC:
          bsAromatic.set(i);
          break;
        case JmolEdge.BOND_AROMATIC_SINGLE:
          bsAromaticSingle.set(i);
          break;
        case JmolEdge.BOND_AROMATIC_DOUBLE:
          bsAromaticDouble.set(i);
          break;
        }
      }
    // main recursive loop
    Bond bond;
    isAll = (bsBonds == null);
    i0 = (isAll ? bondCount - 1 : bsBonds.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsBonds.nextSetBit(i + 1))) {
        bond = bonds[i];
        if (!bond.is(JmolEdge.BOND_AROMATIC)
            || bsAromaticDouble.get(i) || bsAromaticSingle.get(i))
          continue;
        if (!assignAromaticDouble(bond))
          assignAromaticSingle(bond);
      }
    // all done: do the actual assignments and clear arrays.
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsBonds.nextSetBit(i + 1))) {
        bond = bonds[i];
        if (bsAromaticDouble.get(i)) {
          if (!bond.is(JmolEdge.BOND_AROMATIC_DOUBLE)) {
            bsAromatic.set(i);
            bond.setOrder(JmolEdge.BOND_AROMATIC_DOUBLE);
          }
        } else if (bsAromaticSingle.get(i) || bond.isAromatic()) {
          if (!bond.is(JmolEdge.BOND_AROMATIC_SINGLE)) {
            bsAromatic.set(i);
            bond.setOrder(JmolEdge.BOND_AROMATIC_SINGLE);
          }
        }
      }

    assignAromaticNandO(bsBonds);

    bsAromaticSingle = null;
    bsAromaticDouble = null;
    
    ///////
    // This was in the former method in ModelSet, which was not accessible:
    //
    // send a message to STICKS indicating that these bonds
    // should be part of the state of the model. They will 
    // appear in the state as bondOrder commands.    
    //if (isUserCalculation)
      //shapeManager.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MIN_VALUE, null, bsAromatic);
    ///////

    
  }

  /**
   * try to assign AROMATICDOUBLE to this bond. Each atom needs to be
   * have all single bonds except for this one.  
   * 
   * @param bond
   * @return      true if successful; false otherwise
   */
  private boolean assignAromaticDouble(Bond bond) {
    int bondIndex = bond.index;
    if (bsAromaticSingle.get(bondIndex))
      return false;
    if (bsAromaticDouble.get(bondIndex))
      return true;
    bsAromaticDouble.set(bondIndex);
    if (!assignAromaticSingleForAtom(bond.atom1, bondIndex)
        || !assignAromaticSingleForAtom(bond.atom2, bondIndex)) {
      bsAromaticDouble.clear(bondIndex);
      return false;
    }
    return true;
  }
  
  /**
   * try to assign AROMATICSINGLE to this bond. Each atom needs to be
   * able to have one aromatic double bond attached.  
   * 
   * @param bond
   * @return      true if successful; false otherwise
   */
  private boolean assignAromaticSingle(Bond bond) {
    int bondIndex = bond.index;
    if (bsAromaticDouble.get(bondIndex))
      return false;
    if (bsAromaticSingle.get(bondIndex))
      return true;
    bsAromaticSingle.set(bondIndex);
    if (!assignAromaticDoubleForAtom(bond.atom1) || !assignAromaticDoubleForAtom(bond.atom2)) {
      bsAromaticSingle.clear(bondIndex);
      return false;
    }
    return true;
  }

  /**
   * N atoms with 3 bonds cannot also have a double bond; 
   * other atoms needs all single bonds, 
   * because the bond leading up to it is double.
   * 
   * @param atom
   * @param notBondIndex  that index of the bond leading to this atom --- to be ignored
   * @return      true if successful, false if not
   */
  private boolean assignAromaticSingleForAtom(Atom atom, int notBondIndex) {
    Bond[] bonds = atom.bonds;
    if (bonds == null || assignAromaticSingleHetero(atom))
      return false;
    for (int i = bonds.length; --i >= 0;) {
      Bond bond = bonds[i];
      int bondIndex = bond.index;
      if (bondIndex == notBondIndex || !bond.isAromatic()
          || bsAromaticSingle.get(bondIndex))
        continue;
      if (bsAromaticDouble.get(bondIndex) || !assignAromaticSingle(bond)) {
        return false;
      }
    }
    return true;
  }
 
  /**
   * N atoms with 3 bonds cannot also have a double bond; 
   * other atoms need one and only one double bond;
   * the rest must be single bonds.
   * 
   * @param atom
   * @return      true if successful, false if not
   */
  private boolean assignAromaticDoubleForAtom(Atom atom) {
    Bond[] bonds = atom.bonds;
    if (bonds == null)
      return false;
    boolean haveDouble = assignAromaticSingleHetero(atom);
    int lastBond = -1;
    for (int i = bonds.length; --i >= 0;) {
      if (bsAromaticDouble.get(bonds[i].index))
        haveDouble = true;
      if (bonds[i].isAromatic())
        lastBond = i;
    }
    for (int i = bonds.length; --i >= 0;) {
      Bond bond = bonds[i];
      int bondIndex = bond.index;
      if (!bond.isAromatic() || bsAromaticDouble.get(bondIndex)
          || bsAromaticSingle.get(bondIndex))
        continue;
      if (!haveDouble && assignAromaticDouble(bond))
        haveDouble = true;
      else if ((haveDouble || i < lastBond) && !assignAromaticSingle(bond)) {
        return false;
      }
    }
    return haveDouble;
  } 
  
  private boolean assignAromaticSingleHetero(Atom atom) {
    // only C N O S may be a problematic:
    int n = atom.getElementNumber();
    switch (n) {
    case 6: // C
    case 7: // N
    case 8: // O
    case 16: // S
      break;
    default:
      return true;
    }
    int nAtoms = atom.getValence();
    switch (n) {
    case 6: // C
      return (nAtoms == 4);
    case 7: // N
    case 8: // O
      return (nAtoms == 10 - n && atom.getFormalCharge() < 1);
    case 16: // S
      return (nAtoms == 18 - n && atom.getFormalCharge() < 1);
    }
    return false;
  }
  
  private void assignAromaticNandO(BS bsSelected) {
    Bond bond;
    boolean isAll = (bsSelected == null);
    int i0 = (isAll ? bondCount - 1 : bsSelected.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsSelected.nextSetBit(i + 1))) {
        bond = bonds[i];
        if (!bond.is(JmolEdge.BOND_AROMATIC_SINGLE))
          continue;
        Atom atom1;
        Atom atom2 = bond.atom2;
        int n1;
        int n2 = atom2.getElementNumber();
        if (n2 == 7 || n2 == 8) {
          n1 = n2;
          atom1 = atom2;
          atom2 = bond.atom1;
          n2 = atom2.getElementNumber();
        } else {
          atom1 = bond.atom1;
          n1 = atom1.getElementNumber();
        }
        if (n1 != 7 && n1 != 8)
          continue;
        int valence = atom1.getValence();
        if (valence < 0)
          continue; // deleted
        int bondorder = atom1.getCovalentBondCount();
        int charge = atom1.getFormalCharge();
        switch (n1) {
        case 7:
          //trivalent nonpositive N with lone pair in p orbital
          //next to trivalent C --> N=C
          if (valence == 3 && bondorder == 3 && charge < 1 && n2 == 6
              && atom2.getValence() == 3)
            bond.setOrder(JmolEdge.BOND_AROMATIC_DOUBLE);
          break;
        case 8:
          //monovalent nonnegative O next to P or S
          if (valence == 1 && charge == 0 && (n2 == 14 || n2 == 16))
            bond.setOrder(JmolEdge.BOND_AROMATIC_DOUBLE);
          break;
        }
      }
  }

  protected BS getAtomBitsMDb(int tokType, Object specInfo) {
    BS bs;
    switch (tokType) {
    default:
      return getAtomBitsMDa(tokType, specInfo);
    case T.bonds:
      bs = new BS();
      BS bsBonds = (BS) specInfo;
      for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1)) {
        bs.set(bonds[i].atom1.index);
        bs.set(bonds[i].atom2.index);
      }
      return bs;
    case T.isaromatic:
      bs = new BS();
      for (int i = bondCount; --i >= 0;)
        if (bonds[i].isAromatic()) {
          bs.set(bonds[i].atom1.index);
          bs.set(bonds[i].atom2.index);
        }
      return bs;
    }
  }
 
  public BS setBondOrder(int bondIndex, char type) {
    int bondOrder = type - '0';
    Bond bond = bonds[bondIndex];
    switch (type) {
    case '0':
    case '1':
    case '2':
    case '3':
      break;
    case 'p':
    case 'm':
      bondOrder = JmolEdge.getBondOrderNumberFromOrder(
          bond.getCovalentOrder()).charAt(0)
          - '0' + (type == 'p' ? 1 : -1);
      if (bondOrder > 3)
        bondOrder = 1;
      else if (bondOrder < 0)
        bondOrder = 3;
      break;
    default:
      return null;
    }
    BS bsAtoms = new BS();
    try {
      if (bondOrder == 0) {
        BS bs = new BS();
        bs.set(bond.index);
        bsAtoms.set(bond.getAtomIndex1());
        bsAtoms.set(bond.getAtomIndex2());
        dBm(bs, false);
        return bsAtoms;
      }
      bond.setOrder(bondOrder | JmolEdge.BOND_NEW);
      removeUnnecessaryBonds(bond.atom1, false);
      removeUnnecessaryBonds(bond.atom2, false);
      bsAtoms.set(bond.getAtomIndex1());
      bsAtoms.set(bond.getAtomIndex2());
    } catch (Exception e) {
      Logger.error("Exception in seBondOrder: " + e.toString());
    }
    return bsAtoms;
  }

  protected void removeUnnecessaryBonds(Atom atom, boolean deleteAtom) {
    BS bs = new BS();
    BS bsBonds = new BS();
    Bond[] bonds = atom.bonds;
    if (bonds == null)
      return;
    for (int i = 0; i < bonds.length; i++)
      if (bonds[i].isCovalent()) {
        Atom atom2 = bonds[i].getOtherAtom(atom);
        if (atom2.getElementNumber() == 1)
          bs.set(bonds[i].getOtherAtom(atom).index);
      } else {
        bsBonds.set(bonds[i].index);
      }
    if (bsBonds.nextSetBit(0) >= 0)
      dBm(bsBonds, false);
    if (deleteAtom)
      bs.set(atom.index);
    if (bs.nextSetBit(0) >= 0)
      viewer.deleteAtoms(bs, false);
  }
  
  public boolean haveHiddenBonds;
  
  public void displayBonds(BondSet bs, boolean isDisplay) {
    if (!isDisplay)
      haveHiddenBonds = true;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      if (i < bondCount && bonds[i].mad != 0)
        bonds[i].setShapeVisibility(isDisplay);
  }

  /**
   * used in PyMOL reader to set unique bond settings and for valence
   * 
   * @param modelIndex
   * @param iBond
   * @param bsBonds
   * @param rad
   * @param pymolValence  1 for "show multiple bonds
   * @param argb
   * @param trans
   */
  public void setBondParametersBS(int modelIndex, int iBond, BS bsBonds,
                                  float rad, float pymolValence, int argb,
                                  float trans) {
    if (bsBonds == null)
      setBondParameters(modelIndex, iBond, rad, pymolValence, argb, trans);
    else
      for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1))
        setBondParameters(modelIndex, i, rad, pymolValence, argb, trans);
  }

  public void setBondParameters(int modelIndex, int i, float rad, float pymolValence,
                             int argb, float trans) {
    if (i < 0 || i >= bondCount)
      return;
    Bond b = bonds[i];
    if (modelIndex >= 0 && b.atom1.modelIndex != modelIndex)
      return; 
    if (!Float.isNaN(rad))
      b.mad = (short) (rad * 2000);
    short colix = b.colix;
    if (argb != Integer.MAX_VALUE)
      colix = C.getColix(argb);
    if (!Float.isNaN(trans))
      b.colix = C.getColixTranslucent3(colix, trans != 0, trans);
    else if (b.colix != colix)
      b.colix = C.copyColixTranslucency(b.colix, colix);
    if (pymolValence == 1)
      b.order &= ~JmolEdge.BOND_AS_SINGLE;
    else if (pymolValence == 0)
      b.order |= JmolEdge.BOND_AS_SINGLE;
  }


}

