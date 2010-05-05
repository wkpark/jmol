/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.smiles;

import java.util.BitSet;
import java.util.Vector;

import javax.vecmath.Vector3f;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.viewer.JmolConstants;

/**
 *  -- was SmilesMolecule, 
 * but this now includes more data than that and the search itself
 * so as to keep this thread safe
 * 
 */
public class SmilesSearch {

  Atom[] jmolAtoms;
  int jmolAtomCount;
  BitSet bsSelected;
  BitSet bsRequired;
  BitSet bsNot;      
  boolean isAll;
  boolean isSmarts;
  
  int patternAtomCount;
  boolean asVector;
  boolean haveBondStereochemistry;
  boolean haveAtomStereochemistry;

  private final static int INITIAL_ATOMS = 16;
  private SmilesAtom[] atoms = new SmilesAtom[INITIAL_ATOMS];
  private Vector vReturn;
  private BitSet bsReturn = new BitSet();
    
  /* ============================================================= */
  /*                             Atoms                             */
  /* ============================================================= */

  public SmilesAtom createAtom() {
    if (patternAtomCount >= atoms.length) {
      SmilesAtom[] tmp = new SmilesAtom[atoms.length * 2];
      System.arraycopy(atoms, 0, tmp, 0, atoms.length);
      atoms = tmp;
    }
    SmilesAtom atom = new SmilesAtom(patternAtomCount);
    atoms[patternAtomCount] = atom;
    patternAtomCount++;
    return atom;
  }

  public int getPatternAtomCount() {
    return patternAtomCount;
  }

  public SmilesAtom getAtom(int number) {
    return atoms[number];
  }
  
  /* ============================================================= */
  /* Bonds */
  /* ============================================================= */

  public SmilesBond createBond(SmilesAtom atom1, SmilesAtom atom2, int bondType) {
    SmilesBond bond = new SmilesBond(atom1, atom2, bondType);
    if (atom1 != null)
      atom1.addBond(bond);
    if (atom2 != null)
      atom2.addBond(bond);
    return bond;
  }
  
  /** 
   * the start of the search. ret will be either a Vector or a BitSet
   * @return BitSet or Vector
   * 
   */
  Object search() {

    /*
     * The essence of the search process is as follows:
     * 
     * 1) From the pattern, create an ordered set of atoms connected by bonds.
     *    
     * 2) Try all model set atoms for position 0.
     * 
     * 3) For each atom that matches position N
     *    we move to position N+1 and run through all 
     *    of the pattern bonds TO this atom (atom in position 2).
     *    Those bonds will be to atoms that have already
     *    been assigned. There may be more than one of these
     *    if the atom is associated with a ring junction.
     *    
     *    We check that previously assigned model atom,
     *    looking at all of its bonded atoms to check for 
     *    a match for our N+1 atom. This works because if 
     *    this atom is going to work in this position, then 
     *    it must be bound to the atom assigned to position N
     *    
     *    There is no need to check more than one route to this
     *    atom in this position - if it is found to be good once,
     *    that is all we need, and if it is found to be bad once,
     *    that is all we need as well.
     *    
     */
    if (asVector)
      vReturn = new Vector();    
    for (int i = 0; i < jmolAtomCount; i++)
      if (!checkMatch(atoms[0], 0, i))
        break;
    return (asVector ? (Object) vReturn : bsReturn);
  }

  /**
   * Check for a specific match of a model set atom with a pattern position
   * 
   * @param ret
   *          Resulting BitSet or Vector(BitSet).
   * @param search
   *          SMILES pattern.
   * @param atoms
   * @param atomCount
   * @param patternAtom
   *          Atom of the pattern that is currently tested.
   * @param atomNum
   *          Current atom of the pattern.
   * @param i
   *          Atom number of the atom that is currently tested to match
   *          <code>patternAtom</code>.
   * @return true to continue or false if oneOnly
   */
  private final boolean checkMatch(SmilesAtom patternAtom, int atomNum, int i) {

    if (bsNot != null && bsNot.get(i) || bsSelected != null && !bsSelected.get(i))
      return true;

    for (int j = 0; j < atomNum; j++) {
      SmilesAtom previousAtom = atoms[j];
      if (previousAtom.getMatchingAtom() == i) {
        return true;
      }
    }
    Atom atom = getJmolAtom(i);

    // Check atomic number
    int targetAtomicNumber = patternAtom.getAtomicNumber();
    if (targetAtomicNumber != 0
        && targetAtomicNumber != (atom.getElementNumber()))
      return true;
    // Check isotope
    int targetMass = patternAtom.getAtomicMass();
    if (targetMass > 0 && targetMass != atom.getIsotopeNumber()) {
      // smiles indicates [13C] or [12C]
      // must match perfectly -- [12C] matches only explicit C-12, not
      // "unlabeled" C
      return true;
    }
    // Check charge
    if (patternAtom.getCharge() != atom.getFormalCharge())
      return true;
    // Check hcount
    int npH = patternAtom.getHydrogenCount();
    if (npH != Integer.MIN_VALUE && npH != Integer.MAX_VALUE) {
      int nH = atom.getCovalentHydrogenCount();
      if (npH < 0 && nH < -npH || npH >= 0 && nH != npH)
        return true;
    }

    // Check bonds

    Bond[] bonds = atom.getBonds();

    /*
     * the JME test for aromatic carbon is simply that the atom is carbon and is
     * in a ring and is double bonded or aromatic-bonded nothing more than that
     */
    boolean bondFound = false;
    boolean isAromatic = patternAtom.isAromatic();
    if (isAromatic && targetAtomicNumber == 6) {
      // for aromatic carbon specifically, match any atom
      // that is either doubly bonded or aromatic bonded
      for (int k = 0; k < bonds.length; k++) {
        int order = bonds[k].getCovalentOrder();
        if (order == JmolConstants.BOND_COVALENT_DOUBLE
            || (order & JmolConstants.BOND_AROMATIC_MASK) != 0) {
          bondFound = true;
          break;
        }
      }
      if (!bondFound)
        return true;
    }

    for (int j = 0; j < patternAtom.getBondsCount(); j++) {
      SmilesBond patternBond = patternAtom.getBond(j);
      // Check only if the current atom is the second atom of the bond
      if (patternBond.getAtom2() != patternAtom)
        continue;
      SmilesAtom atom1 = patternBond.getAtom1();
      int matchingAtom = atom1.getMatchingAtom();
      // at least for SMILES...
      // we don't care what the bond is designated as for an aromatic atom.
      // That may seem strange, but it's true for aromatic carbon, as we
      // already know it is double- or aromatic-bonded.
      // for N, we assume it is attached to at least one aromatic atom,
      // and that is enough for us.
      // this does not actually work for SMARTS
      boolean matchAnyBond = (isAromatic && atom1.isAromatic());
      for (int k = 0; k < bonds.length; k++) {
        if (bonds[k].getAtomIndex1() != matchingAtom
            && bonds[k].getAtomIndex2() != matchingAtom)
          continue;
        if (matchAnyBond) {
          // disregard bond type when aromatic -- could be single, double, or
          // aromatic
          bondFound = true;
          break;
        }
        bondFound = false;
        if (!bonds[k].isCovalent())
          continue;
        int order = bonds[k].getCovalentOrder();
        switch (patternBond.getBondType()) {
        case SmilesBond.TYPE_SINGLE:
        case SmilesBond.TYPE_DIRECTIONAL_1:
        case SmilesBond.TYPE_DIRECTIONAL_2:
          bondFound = (order == JmolConstants.BOND_COVALENT_SINGLE);
          break;
        case SmilesBond.TYPE_DOUBLE:
          bondFound = (order == JmolConstants.BOND_COVALENT_DOUBLE);
          break;
        case SmilesBond.TYPE_TRIPLE:
          bondFound = (order == JmolConstants.BOND_COVALENT_TRIPLE);
          break;
        case SmilesBond.TYPE_AROMATIC: // not implemented
          bondFound = ((order & JmolConstants.BOND_AROMATIC_MASK) != 0);
          break;
        case SmilesBond.TYPE_UNKNOWN:
          bondFound = true;
          break;
        }
        if (!bondFound)
          return true;
        break;
      }
    }

    // add this atom to the growing list
    patternAtom.setMatchingAtom(i);

    atomNum++;
    if (Logger.debugging) {
      StringBuffer s = new StringBuffer();
      for (int k = 0; k < atomNum; k++) {
        s.append("-").append(atoms[k].getMatchingAtom());
      }
      s.append(" ").append(atomNum).append("/").append(getPatternAtomCount());
      Logger.debug(s.toString());
    }

    if (atomNum < patternAtomCount) {
      // next position...
      patternAtom = atoms[atomNum];
      // for all the pattern bonds for this atom...
      for (int iAtom = 0; iAtom < patternAtom.getBondsCount(); iAtom++) {
        SmilesBond patternBond = patternAtom.getBond(iAtom);
        // find the bond to atoms already assigned
        // note that it must be there, because SMILES strings
        // are parsed in order, from left to right. You can't
        // have two fragments going at the same time.
        if (patternBond.getAtom2() == patternAtom) {
          // run through the bonds of that assigned atom
          atom = getJmolAtom(patternBond.getAtom1().getMatchingAtom());
          bonds = atom.getBonds();
          // now run through all the bonds looking for atoms that might match
          // this is the iterative step
          if (bonds != null)
            for (int j = 0; j < bonds.length; j++)
              if (!checkMatch(patternAtom, atomNum, atom.getBondedAtomIndex(j)))
                return false;
          break; // once through
        }
      }
    } else {
      if (!checkStereochemistry())
        return true;
      BitSet bs = new BitSet();
      for (int k = 0; k < patternAtomCount; k++) {
        int iAtom = atoms[k].getMatchingAtom();
        bs.set(iAtom);        
        if (!isSmarts) {
          npH = atoms[k].getHydrogenCount();
          if (npH != Integer.MIN_VALUE && npH != Integer.MAX_VALUE) 
              getHydrogens(getJmolAtom(iAtom), bs);
        }
      }
      if (bsRequired != null && !bsRequired.intersects(bs))
        return true;
      if (asVector) {
        boolean isOK = true;
        for (int j = vReturn.size(); --j >= 0 && isOK;)
          isOK = !(((BitSet) vReturn.get(j)).equals(bs));
        if (isOK)
          vReturn.add(bs);
        else
          return true;
        bsReturn = bs;
      } else {
        bsReturn.or(bs);
      }
      if (Logger.debugging) {
        StringBuffer s = new StringBuffer();
        for (int k = 0; k < atomNum; k++)
          s.append("-").append(atoms[k].getMatchingAtom());
        s.append(" ").append(atomNum).append("/").append(getPatternAtomCount());
        Logger.debug(s.toString());
        Logger.debug("match: " + Escape.escape(bsReturn));
      }
      if (!isAll || bsReturn.cardinality() == jmolAtomCount)
        return false;
    }
    patternAtom.setMatchingAtom(-1);
    return true;
  }

  private boolean checkStereochemistry() {

    // first, @ stereochemistry

    if (haveAtomStereochemistry) {
      Atom atom1 = null, atom2 = null, atom3 = null, atom4 = null, atom5 = null;
      for (int k = 0; k < patternAtomCount; k++) {
        SmilesAtom sAtom = atoms[k];
        int nH = sAtom.getHydrogenCount();
        int chiralClass = sAtom.getChiralClass();
        int order = sAtom.getChiralOrder();
        atom4 = null;
        switch (chiralClass) {
        case Integer.MIN_VALUE:
          break;
        case SmilesAtom.CHIRALITY_ALLENE:
          SmilesAtom sAtom1 = sAtom.getBond(0).getOtherAtom(sAtom);
          SmilesAtom sAtom2 = sAtom.getBond(1).getOtherAtom(sAtom);
          if (sAtom1 == null || sAtom2 == null)
            continue;
          
          // ignoring cumulenes for now
          
          atom1 = getJmolAtom(sAtom1.getMatchingBondedAtom(0));
          atom2 = getJmolAtom(sAtom1.getMatchingBondedAtom(1));
          if (atom2 == null && nH != 1)
            continue;
          if (atom2 == null) {
            atom2 = getHydrogens(getJmolAtom(sAtom1.getMatchingAtom()), null);
            if (sAtom1.getIndex() == 0) {
              // attached H is first in that case
              Atom a = atom2;
              atom2 = atom1;
              atom1 = a;
            }
          }
          nH = sAtom2.getHydrogenCount();
          atom3 = getJmolAtom(sAtom2.getMatchingBondedAtom(1));
          atom4 = getJmolAtom(sAtom2.getMatchingBondedAtom(2));
          if (atom3 == null || atom4 == null && nH != 1)
            continue;
          if (atom4 == null)
            atom4 = getHydrogens(getJmolAtom(sAtom2.getMatchingAtom()), null);
          atom5 = null;
          break;          
        case SmilesAtom.CHIRALITY_TETRAHEDRAL:
          atom1 = getJmolAtom(sAtom.getMatchingBondedAtom(0));
          switch (nH) {
          case 0:
            atom2 = getJmolAtom(sAtom.getMatchingBondedAtom(1));
            break;
          case 1:
            atom2 = getHydrogens(getJmolAtom(sAtom.getMatchingAtom()), null);
            if (sAtom.getIndex() == 0) {
              // attached H is first in that case
              Atom a = atom2;
              atom2 = atom1;
              atom1 = a;
            }
            atom5 = null;
            break;
          default:
            continue;
          }
          atom3 = getJmolAtom(sAtom.getMatchingBondedAtom(2 - nH));
          atom4 = getJmolAtom(sAtom.getMatchingBondedAtom(3 - nH));
          break;
        default:
          Logger.error("chirality class " + chiralClass + " not supported");
          continue;
        }
        if (atom4 != null && atom5 == null)
          if (getChirality(atom2, atom3, atom4, atom1) != order)
            return false;
      }
    }
    // next, /C=C/ double bond stereochemistry

    if (haveBondStereochemistry) {
      for (int k = 0; k < patternAtomCount; k++) {
        SmilesAtom sAtom1 = atoms[k];
        SmilesAtom sAtom2 = null;
        SmilesAtom sAtomDirected1 = null;
        SmilesAtom sAtomDirected2 = null;
        int dir1 = 0;
        int dir2 = 0;
        
        int nBonds = sAtom1.getBondsCount();
        for (int j = 0; j < nBonds; j++) {
          SmilesBond b = sAtom1.getBond(j);
          boolean isAtom2 = (b.getAtom2() == sAtom1);
          int type = b.getBondType();
          switch (type) {
          case SmilesBond.TYPE_DOUBLE:
            if (isAtom2)
              continue;
            sAtom2 = b.getAtom2();
            break;
          case SmilesBond.TYPE_DIRECTIONAL_1:
          case SmilesBond.TYPE_DIRECTIONAL_2:
            sAtomDirected1 = (isAtom2 ? b.getAtom1() : b.getAtom2());
            dir1 = (type == SmilesBond.TYPE_DIRECTIONAL_1 ? 1 : -1);
          }
        }
        if (sAtom2 == null || dir1 == 0)
          continue;
        nBonds = sAtom2.getBondsCount();
        for (int j = 0; j < nBonds && dir2 == 0; j++) {
          SmilesBond b = sAtom2.getBond(j);
          boolean isAtom2 = (b.getAtom2() == sAtom2);
          int type = b.getBondType();
          switch (type) {
          case SmilesBond.TYPE_DIRECTIONAL_1:
          case SmilesBond.TYPE_DIRECTIONAL_2:
            sAtomDirected2 = (isAtom2 ? b.getAtom1() : b.getAtom2());
            dir2 = (type == SmilesBond.TYPE_DIRECTIONAL_1 ? 1 : -1);
            break;
          }
        }
        
        if (dir2 == 0)
          continue;

        Atom dbAtom1 = getJmolAtom(sAtom1.getMatchingAtom());        
        Atom dbAtom2 = getJmolAtom(sAtom2.getMatchingAtom());        
        Atom dbAtom1a = getJmolAtom(sAtomDirected1.getMatchingAtom());        
        Atom dbAtom2a = getJmolAtom(sAtomDirected2.getMatchingAtom());        
        Vector3f v1 = new Vector3f(dbAtom1a);
        Vector3f v2 = new Vector3f(dbAtom2a);
        v1.sub(dbAtom1);
        v2.sub(dbAtom2);
        // for \C=C\, (dir1*dir2 == 1), dot product should be negative 
        // because the bonds are oppositely directed
        // for \C=C/, (dir1*dir2 == -1), dot product should be positive 
        // because the bonds are only about 60 degrees apart
        if (v1.dot(v2) * dir1 * dir2 > 0)
          return false;
      }
    }
    return true;
  }

  private Atom getJmolAtom(int i) {
    return (i < 0 ? null : jmolAtoms[i]);
  }

  private Atom getHydrogens(Atom atom, BitSet bsHydrogens) {
    Bond[] b = atom.getBonds();
    Atom atomH = null;
    for (int k = 0, i = 0; i < b.length; i++)
      if ((atomH = getJmolAtom(k = atom.getBondedAtomIndex(i))).getElementNumber() == 1) {
        if (bsHydrogens == null)
          break;
        bsHydrogens.set(k);
      }
    return atomH;
  }



  Vector3f vTemp = new Vector3f();
  Vector3f vA = new Vector3f();
  Vector3f vB = new Vector3f();
  private int getChirality(Atom a, Atom b, Atom c, Atom pt) {
    return (Measure.getDirectedNormalThroughPoints(a, b, c, pt, vTemp, vA, vB) < 0 ? 1 : 2);
  }  

}
