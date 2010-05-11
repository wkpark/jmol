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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.api.JmolEdge;
import org.jmol.api.JmolNode;
//import org.jmol.util.Logger;

/**
 *  -- was SmilesMolecule, 
 * but this now includes more data than that and the search itself
 * so as to keep this thread safe
 * 
 * definition of "aromatic" is 
 * 
 */
public class SmilesSearch {

  boolean isSilent;
  boolean isSmilesFind;
  Hashtable htNested;
  private int nNested;
  public int addNested(String pattern) {
    if (htNested == null)
      htNested = new Hashtable();
    htNested.put("_" + (++nNested), pattern);
    return nNested;
  }  

  JmolNode[] jmolAtoms;
  int jmolAtomCount;
  BitSet bsSelected;
  BitSet bsRequired;
  BitSet bsNot;      
  boolean isAll;
  boolean isSearch;
  
  String pattern;
  int patternAtomCount;
  boolean asVector;
  boolean haveSelected;
  boolean haveBondStereochemistry;
  boolean haveAtomStereochemistry;
  boolean needRingData;
  int ringDataMax = 8;
  BitSet[] ringData;
  int[] ringCounts;
  int[] ringConnections;

  private final static int INITIAL_ATOMS = 16;
  SmilesAtom[] atoms = new SmilesAtom[INITIAL_ATOMS];
  private Vector vReturn;
  private BitSet bsReturn = new BitSet();
  private BitSet bsAromatic = new BitSet();
  
  private StringBuffer ringSets;
  
  /* ============================================================= */
  /*                             Atoms                             */
  /* ============================================================= */

  public SmilesAtom addAtom() {
    if (patternAtomCount >= atoms.length) {
      SmilesAtom[] tmp = new SmilesAtom[atoms.length * 2];
      System.arraycopy(atoms, 0, tmp, 0, atoms.length);
      atoms = tmp;
    }
    SmilesAtom sAtom = new SmilesAtom(patternAtomCount);
    atoms[patternAtomCount] = sAtom;
    patternAtomCount++;
    return sAtom;
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
   * @param firstAtomOnly TODO
   * @return BitSet or Vector
   * 
   */
  Object search(boolean firstAtomOnly) {

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
      if (!checkMatch(atoms[0], 0, i, firstAtomOnly))
        break;
    return (asVector ? (Object) vReturn : bsReturn);
  }

  /**
   * Check for a specific match of a model set atom with a pattern position
   * 
   * @param patternAtom
   *          Atom of the pattern that is currently tested.
   * @param atomNum
   *          Current atom of the pattern.
   * @param iAtom
   *          Atom number of the atom that is currently tested to match
   *          <code>patternAtom</code>.
   * @param firstAtomOnly
   *          TODO
   * @param ret
   *          Resulting BitSet or Vector(BitSet).
   * @param search
   *          SMILES pattern.
   * @param atoms
   * @param atomCount
   * @return true to continue or false if oneOnly
   */
  private final boolean checkMatch(SmilesAtom patternAtom, int atomNum,
                                   int iAtom, boolean firstAtomOnly) {

    // check for requested selection or not-selection

    if (bsNot != null && bsNot.get(iAtom) || bsSelected != null
        && !bsSelected.get(iAtom))
      return true;

    JmolNode atom = getJmolAtom(iAtom);

// check for atom already found or atom not in this model

    for (int i = 0; i < atomNum; i++) {
      int iPrev = atoms[i].getMatchingAtom();
      if (iPrev == iAtom || atom != null
          && jmolAtoms[0].getModelIndex() != atom.getModelIndex()) {
        return true;
      }
    }

    // apply SEARCH [ , , & ; ] logic

    if (patternAtom.atomsOr != null) {
      for (int ii = 0; ii < patternAtom.nAtomsOr; ii++)
        if (!checkMatch(patternAtom.atomsOr[ii], atomNum, iAtom, firstAtomOnly))
          return false;
      return true;
    }

    if (patternAtom.primitives == null) {
      if (!checkPrimitiveAtom(patternAtom, iAtom))
        return true;
    } else {
      for (int i = 0; i < patternAtom.nPrimitives; i++)
        if (!checkPrimitiveAtom(patternAtom.primitives[i], iAtom))
          return true;
    }
    // Check bonds

    boolean bondFound = false;
    boolean isAromatic = patternAtom.isAromatic();

    JmolEdge[] bonds = atom.getEdges();

    for (int i = 0; i < patternAtom.getBondsCount(); i++) {
      SmilesBond patternBond = patternAtom.getBond(i);
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
      // this does not actually work for SEARCH
      boolean bothAromatic = (isAromatic && atom1.isAromatic());
      bondFound = false;
      for (int k = 0; k < bonds.length; k++) {
        if (bonds[k].getAtomIndex1() != matchingAtom
            && bonds[k].getAtomIndex2() != matchingAtom
            || !bonds[k].isCovalent())
          continue;
        bondFound = false;
        int order = bonds[k].getCovalentOrder();
        if (bothAromatic) {
          switch (patternBond.getBondType()) {
          case SmilesBond.TYPE_AROMATIC: // : 
          case SmilesBond.TYPE_DOUBLE:
            bondFound = (ringSets.indexOf("-" + iAtom + "-" + matchingAtom + "-") >= 0);
            break;
          case SmilesBond.TYPE_SINGLE:
            bondFound = (ringSets.indexOf("-" + iAtom + "-" + matchingAtom + "-") < 0);
            break;
          case SmilesBond.TYPE_RING_BOND:
          case SmilesBond.TYPE_ANY:
          case SmilesBond.TYPE_UNKNOWN:
            bondFound = true;
            break;
          }
        } else {
          switch (patternBond.getBondType()) {
          case SmilesBond.TYPE_ANY:
          case SmilesBond.TYPE_UNKNOWN:
            bondFound = true;
            break;
          case SmilesBond.TYPE_SINGLE:
          case SmilesBond.TYPE_DIRECTIONAL_1:
          case SmilesBond.TYPE_DIRECTIONAL_2:
            // STEREO_NEAR and _FAR are stand-ins for find()
            bondFound = (order == JmolEdge.BOND_COVALENT_SINGLE
                || order == JmolEdge.BOND_STEREO_NEAR 
                || order == JmolEdge.BOND_STEREO_FAR);
            break;
          case SmilesBond.TYPE_DOUBLE:
            bondFound = (order == JmolEdge.BOND_COVALENT_DOUBLE);
            break;
          case SmilesBond.TYPE_TRIPLE:
            bondFound = (order == JmolEdge.BOND_COVALENT_TRIPLE);
            break;
          case SmilesBond.TYPE_RING_BOND:
            bondFound = (ringSets.indexOf("-" + iAtom + "-" + matchingAtom + "-") >= 0);
            break;
          }
        }
        if (!bondFound)
          return true;
      }
      if (!bondFound)
        return true;
    }

    // add this atom to the growing list
    // note that we explicitly do a reference using
    // index because this could be a SEARCH [x,x] "sub" atom
    atoms[patternAtom.index].setMatchingAtom(iAtom);
    atomNum++;
    if (atomNum < patternAtomCount) {
      // next position...
      patternAtom = atoms[atomNum];
      // for all the pattern bonds for this atom...
      for (int i = 0; i < patternAtom.getBondsCount(); i++) {
        SmilesBond patternBond = patternAtom.getBond(i);
        // find the bond to atoms already assigned
        // note that it must be there, because SMILES strings
        // are parsed in order, from left to right. You can't
        // have two fragments going at the same time.
        if (patternBond.getAtom2() == patternAtom) {
          // run through the bonds of that assigned atom
          atom = getJmolAtom(patternBond.getAtom1().getMatchingAtom());
          bonds = atom.getEdges();
          // now run through all the bonds looking for atoms that might match
          // this is the iterative step
          if (bonds != null)
            for (int j = 0; j < bonds.length; j++)
              if (!checkMatch(patternAtom, atomNum, atom.getBondedAtomIndex(j),
                  firstAtomOnly))
                return false;
          break; // once through
        }
      }
    } else {
      if (!checkStereochemistry())
        return true;
      BitSet bs = new BitSet();
      for (int j = 0; j < patternAtomCount; j++) {
        int i = atoms[j].getMatchingAtom();
        if (!firstAtomOnly && haveSelected && !atoms[j].selected)
          continue;
        bs.set(i);
        if (firstAtomOnly)
          break;
        if (!isSearch && atoms[j].explicitHydrogenCount > 0)
          getHydrogens(getJmolAtom(i), bs);
      }
      if (bsRequired != null && !bsRequired.intersects(bs))
        return true;
      bsReturn.or(bs);
      if (asVector) {
        boolean isOK = true;
        for (int j = vReturn.size(); --j >= 0 && isOK;)
          isOK = !(((BitSet) vReturn.get(j)).equals(bs));
        if (!isOK)
          return true;
        vReturn.add(bs);
      }
/*      
      if (!isSilent && Logger.debugging) {
        StringBuffer s = new StringBuffer();
        for (int k = 0; k < atomNum; k++)
          s.append("-").append(atoms[k].getMatchingAtom());
        s.append(" ").append(atomNum).append("/").append(getPatternAtomCount());
        Logger.debug(s.toString());
        Logger.debug("match: " + bs);
      }
*/      
      if (ringSets != null) {
        ringSets.append(" ");
        for (int k = atomNum * 3 + 2; --k > atomNum;)
          ringSets.append("-").append(atoms[(k <= atomNum * 2 ? atomNum * 2 - k + 1: k - 1) % atomNum].getMatchingAtom());
        ringSets.append("- ");
      }
      if (!isAll || bsReturn.cardinality() == jmolAtomCount)
        return false;
    }
    patternAtom.setMatchingAtom(-1);
    return true;
  }

  private boolean checkPrimitiveAtom(SmilesAtom patternAtom, int iAtom) {
    JmolNode atom = jmolAtoms[iAtom];
    boolean foundAtom = patternAtom.not;
    while (true) {

      int n;

      // _ <n> apply "recursive" SEARCH -- for examle, [C&$(C[$(aaaO);$(aaC)])]"
      if (patternAtom.iNested > 0) {
        Object o = htNested.get("_" + patternAtom.iNested);
        if (!(o instanceof BitSet))
          htNested.put("_" + patternAtom.iNested, o = getBitSets((String) o,
              true, ringSets));
        foundAtom = (patternAtom.not != (((BitSet) o).get(iAtom)));
        break;
      }

      // # <n> or Symbol Check atomic number
      short targetAtomicNumber = patternAtom.elementNumber;
      n = atom.getElementNumber();
      if (targetAtomicNumber >= 0
          && targetAtomicNumber != n)
        break;

      // Check aromatic
      boolean isAromatic = patternAtom.isAromatic();
      if (targetAtomicNumber != -2 && isAromatic != bsAromatic.get(iAtom))
        break;

      // <n> Check isotope
      if (patternAtom.getAtomicMass() == 0 && n != 0)
        break;
      if ((n = patternAtom.getAtomicMass()) != Short.MIN_VALUE) {
        int isotope = atom.getIsotopeNumber();
        if (n >= 0 && n != isotope || n < 0 && isotope != 0 && -n != isotope) {
          // smiles indicates [13C] or [12C]
          // must match perfectly -- [12C] matches only explicit C-12, not
          // "unlabeled" C
          break;
        }
      }

      // +/- Check charge
      if (patternAtom.getCharge() != atom.getFormalCharge())
        break;

      // H explicit H count
      n = patternAtom.explicitHydrogenCount;
      if (n >= 0 && n != atom.getCovalentHydrogenCount())
        break;

      // h implicit H count
      n = patternAtom.implicitHydrogenCount;
      if (n != Integer.MIN_VALUE) {
        int nH = atom.getCovalentHydrogenCount();
        if (n == -1 && nH == 0 || n != -1 && n != nH)
          break;
      }

      // D <n> degree
      if (patternAtom.degree > 0
          && patternAtom.degree != atom.getCovalentHydrogenCount())
        break;

      // v <n> valence
      if (patternAtom.valence > 0 && patternAtom.valence != atom.getValence())
        break;

      // X <n> connectivity ?
      if (patternAtom.connectivity > 0
          && patternAtom.connectivity != atom.getCovalentHydrogenCount()
              + atom.getCovalentHydrogenCount())
        break;

      // r <n>
      if (ringData != null && patternAtom.ringSize >= 0) {
        if (patternAtom.ringSize == 0) {
          if (ringCounts[iAtom] != 0)
            break;
        } else if (ringData[patternAtom.ringSize] == null
            || !ringData[patternAtom.ringSize].get(iAtom)) {
          break;
        }
      }
      // R <n>
      if (ringData != null && patternAtom.ringMembership >= 0) {
        if (ringCounts[iAtom] != patternAtom.ringMembership)
          break;
      }
      // x <n>
      if (patternAtom.ringConnectivity >= 0) {
        // default > 0
        n = ringConnections[iAtom];
        if (patternAtom.ringConnectivity == -1 && n == 0
            || patternAtom.ringConnectivity != -1
            && n != patternAtom.ringConnectivity)
          break;
      }

      foundAtom = !foundAtom;
      break;
    }

    return foundAtom;
  }

  private Vector3f vTemp1;
  private Vector3f vTemp2;
  Vector3f vNorm1;
  Vector3f vNorm2;
  Vector3f vNorm3;

  private boolean checkStereochemistry() {

    // first, @ stereochemistry

    if (haveAtomStereochemistry) {
      JmolNode atom1 = null, atom2 = null, atom3 = null, 
      atom4 = null, atom5 = null, atom6 = null;
      for (int i = 0; i < patternAtomCount; i++) {
        SmilesAtom sAtom = atoms[i];
        int nH = sAtom.explicitHydrogenCount;
        if (nH < 0)
          nH = 0;
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
          // cumulenes
          while (sAtom1.getBondsCount() == 2 && sAtom2.getBondsCount() == 2) {
            sAtom1 = sAtom1.getBond(0).getOtherAtom(sAtom1);
            sAtom2 = sAtom2.getBond(1).getOtherAtom(sAtom2);
          }
          // derive atoms 1-4 from different carbons:
          atom1 = getJmolAtom(sAtom1.getMatchingBondedAtom(0));
          atom2 = getJmolAtom(sAtom1.getMatchingBondedAtom(1));
          if (atom2 == null && nH != 1)
            continue;
          if (atom2 == null) {
            atom2 = getHydrogens(getJmolAtom(sAtom1.getMatchingAtom()), null);
            if (sAtom1.isFirst) {
              // check for the VERY first atom in a set
              // attached H is first in that case
              // so we have to switch it, since we have
              // assigned already the first atom to be
              // the first pattern atom
              JmolNode a = atom2;
              atom2 = atom1;
              atom1 = a;
            }
          }
          nH = sAtom2.explicitHydrogenCount;
          atom3 = getJmolAtom(sAtom2.getMatchingBondedAtom(1));
          atom4 = getJmolAtom(sAtom2.getMatchingBondedAtom(2));
          if (atom3 == null || atom4 == null && nH != 1)
            continue;
          if (atom4 == null)
            atom4 = getHydrogens(getJmolAtom(sAtom2.getMatchingAtom()), null);
          if (isSmilesFind)
            setSmilesCoordinates(sAtom, new JmolNode[] { atom1, atom2, atom3, atom4, null, null });
          if (sAtom.not != (getChirality(atom2, atom3, atom4, atom1) != order))
            return false;
          continue;
        case SmilesAtom.CHIRALITY_TETRAHEDRAL:
        case SmilesAtom.CHIRALITY_SQUARE_PLANAR:
        case SmilesAtom.CHIRALITY_TRIGONAL_BIPYRAMIDAL:
        case SmilesAtom.CHIRALITY_OCTAHEDRAL:
          atom1 = getJmolAtom(sAtom.getMatchingBondedAtom(0));
          switch (nH) {
          case 0:
            atom2 = getJmolAtom(sAtom.getMatchingBondedAtom(1));
            break;
          case 1:
            atom2 = getHydrogens(getJmolAtom(sAtom.getMatchingAtom()), null);
            if (sAtom.isFirst) {
              JmolNode a = atom2;
              atom2 = atom1;
              atom1 = a;
            }
            break;
          default:
            continue;
          }
          atom3 = getJmolAtom(sAtom.getMatchingBondedAtom(2 - nH));
          atom4 = getJmolAtom(sAtom.getMatchingBondedAtom(3 - nH));
          atom5 = getJmolAtom(sAtom.getMatchingBondedAtom(4 - nH));
          atom6 = getJmolAtom(sAtom.getMatchingBondedAtom(5 - nH));
          
          // in all the checks below, we use Measure utilities to 
          // three given atoms -- the normal, in particular. We 
          // then use dot products to check the directions of normals
          // to see if the rotation is in the direction required. 
          
          // we only use TP1, TP2, OH1, OH2 here.
          // so we must also check that the two bookend atoms are axial

          if (isSmilesFind)
            setSmilesCoordinates(sAtom, new JmolNode[] { atom1, atom2, atom3, atom4, atom5, atom6 });
          switch (chiralClass) {
          case SmilesAtom.CHIRALITY_TETRAHEDRAL:
            if (sAtom.not != (getChirality(atom2, atom3, atom4, atom1) != order))
              return false;
            continue;
          case SmilesAtom.CHIRALITY_TRIGONAL_BIPYRAMIDAL:
            // check for axial-axial
            if (sAtom.not != (!isDiaxial(sAtom, atom5, atom1)
                || getChirality(atom2, atom3, atom4, atom1) != order))
              return false;
            continue;
          case SmilesAtom.CHIRALITY_OCTAHEDRAL:
            if (sAtom.not != (!isDiaxial(sAtom, atom6, atom1)))
              return false;
            // check for CW or CCW set
            getPlaneNormals(atom2, atom3, atom4, atom5);
            if (sAtom.not != (vNorm1.dot(vNorm2) < 0 
                || vNorm2.dot(vNorm3) < 0))
              return false;
            // now check rotation in relation to the first atom
            vNorm2.set((Point3f) getJmolAtom(sAtom.getMatchingAtom()));
            vNorm2.sub((Point3f) atom1);
            if (sAtom.not != ((vNorm2.dot(vNorm1) < 0 ? 1 : 2) != order))
              return false;
            continue;
          case SmilesAtom.CHIRALITY_SQUARE_PLANAR:
            getPlaneNormals(atom1, atom2, atom3, atom4);
            // vNorm1 vNorm2 vNorm3 are right-hand normals for the given
            // triangles
            // 1-2-3, 2-3-4, 3-4-1
            // sp1 up up up U-shaped
            // sp2 up up DOWN 4-shaped
            // sp3 up DOWN DOWN Z-shaped

            if (vNorm1.dot(vNorm2) < 0) {
              if (sAtom.not != (order != 3))
                return false;
            } else if (sAtom.not != (vNorm2.dot(vNorm3) < 0)) {
              if (order != 2)
                return false;
            } else if (sAtom.not != (order != 1)) {
              return false;
            }
            continue;
          }
          continue;
        }
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

        if (isSmilesFind)
          setSmilesBondCoordinates(sAtom1, sAtom2);
        JmolNode dbAtom1 = getJmolAtom(sAtom1.getMatchingAtom());
        JmolNode dbAtom2 = getJmolAtom(sAtom2.getMatchingAtom());
        JmolNode dbAtom1a = getJmolAtom(sAtomDirected1.getMatchingAtom());
        JmolNode dbAtom2a = getJmolAtom(sAtomDirected2.getMatchingAtom());
        Vector3f v1 = new Vector3f((Point3f) dbAtom1a);
        Vector3f v2 = new Vector3f((Point3f) dbAtom2a);
        v1.sub((Point3f) dbAtom1);
        v2.sub((Point3f) dbAtom2);
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

  private void setSmilesBondCoordinates(SmilesAtom sAtom1, SmilesAtom sAtom2) {
    JmolNode dbAtom1 = getJmolAtom(sAtom1.getMatchingAtom());
    JmolNode dbAtom2 = getJmolAtom(sAtom2.getMatchingAtom());
    dbAtom1.set(-1, 0, 0);
    dbAtom2.set(1, 0, 0);
    int nBonds = 0;
    int dir1 = 0;
    JmolEdge[] bonds = dbAtom1.getEdges();
    for (int k = bonds.length; --k >= 0;) {
      JmolEdge bond = bonds[k];
      JmolNode atom = bond.getOtherAtom(dbAtom1);
      if (atom == dbAtom2)
        continue;
      atom.set(-2, (nBonds++ == 0) ? -1 : 1, 0);
      switch (bond.getOrder()) {
      case JmolEdge.BOND_STEREO_NEAR:
        dir1 = nBonds;
        break;
      case JmolEdge.BOND_STEREO_FAR:
        dir1 = -nBonds;
      }
    }
    int dir2 = 0;
    nBonds = 0;
    JmolNode[] atoms = new JmolNode[2];
    bonds = dbAtom2.getEdges();
    for (int k = bonds.length; --k >= 0;) {
      JmolEdge bond = bonds[k];
      JmolNode atom = bond.getOtherAtom(dbAtom2);
      if (atom == dbAtom1)
        continue;
      atoms[nBonds] = atom;
      atom.set(2, (nBonds++ == 0) ? 1 : -1, 0);
      switch (bond.getOrder()) {
      case JmolEdge.BOND_STEREO_NEAR:
        dir2 = nBonds;
        break;
      case JmolEdge.BOND_STEREO_FAR:
        dir2 = -nBonds;
      }
    }
    //     2     3
    //      \   /
    //       C=C
    //      /   \
    //     1     4
    //
    // check for overall directionality matching even/oddness of bond order
    // and switch Y positions of 3 and 4 if necessary
    //  
    if ((dir1 * dir2 > 0) == (Math.abs(dir1) % 2 == Math.abs(dir2) % 2))
      return;
    float y = ((Point3f) atoms[0]).y;
    ((Point3f) atoms[0]).y = ((Point3f) atoms[1]).y;
    ((Point3f) atoms[1]).y = y;
  }

  private void setSmilesCoordinates(SmilesAtom sAtom, JmolNode[] cAtoms) {
    
    // When testing equality of two SMILES strings in terms of stereochemistry,
    // we need to set the atom positions based on the ORIGINAL SMILES order,
    // which, except for the H atom, will be the same as the "matchedAtom" index.
    // all the necessary information is passed via the atomSite field of Atom

    int iAtom = sAtom.getMatchingAtom();
    JmolNode atom = jmolAtoms[iAtom];
    int atomSite = atom.getAtomSite();
    if (atomSite == Integer.MIN_VALUE)
      return;
    int chiralClass = atomSite >> 8;
    int chiralOrder = atomSite & 0xFF;
    if (chiralClass != sAtom.getChiralClass())
      return;
    
    // set the chirality center at the origin
    atom.set(0, 0, 0);
    int[] map = new int[cAtoms[4] == null  ? 4 : cAtoms[5] == null ? 5 : 6];
    for (int i = 0; i < map.length; i++)
      map[i] = (cAtoms[i].getIndex() << 3) + i;
    Arrays.sort(map);
    switch (chiralClass) {
    case SmilesAtom.CHIRALITY_ALLENE:
    case SmilesAtom.CHIRALITY_TETRAHEDRAL:
      if (chiralOrder == 2) {
        int i = map[0];
        map[0] = map[1];
        map[1] = i;
      }
      cAtoms[map[0] & 7].set(0, 0, 1);
      cAtoms[map[1] & 7].set(1, 0, -1);
      cAtoms[map[2] & 7].set(0, 1, -1);
      cAtoms[map[3] & 7].set(-1, -1, -1);
      break;
    case SmilesAtom.CHIRALITY_SQUARE_PLANAR:
      switch (chiralOrder) {
      case 1: // U-shaped
        cAtoms[map[0] & 7].set(1, 0, 0);
        cAtoms[map[1] & 7].set(0, 1, 0);
        cAtoms[map[2] & 7].set(-1, 0, 0);
        cAtoms[map[3] & 7].set(0, -1, 0);
        break;
      case 2: // 4-shaped
        cAtoms[map[0] & 7].set(1, 0, 0);
        cAtoms[map[1] & 7].set(-1, 0, 0);
        cAtoms[map[2] & 7].set(0, 1, 0);
        cAtoms[map[3] & 7].set(0, -1, 0);
        break;
      case 3: // Z-shaped
        cAtoms[map[0] & 7].set(1, 0, 0);
        cAtoms[map[1] & 7].set(-1, 0, 0);
        cAtoms[map[2] & 7].set(0, -1, 0);
        cAtoms[map[3] & 7].set(0, 1, 0);
        break;
      }
      break;
    case SmilesAtom.CHIRALITY_TRIGONAL_BIPYRAMIDAL:
    case SmilesAtom.CHIRALITY_OCTAHEDRAL:
      int n = map.length;
      if (chiralOrder == 2) {
        int i = map[0];
        map[0] = map[n - 1];
        map[n - 1] = i;
      }
      cAtoms[map[0] & 7].set(0, 0, 1);
      cAtoms[map[n - 1] & 7].set(0, 0, -1);
      cAtoms[map[1] & 7].set(1, 0, 0);
      cAtoms[map[2] & 7].set(0, 1, 0);
      cAtoms[map[3] & 7].set(-1, 0, 0);
      if (n != 5)
        cAtoms[map[3] & 7].set(0, -1, 0);        
      break;
    }
  }

  private void getPlaneNormals(JmolNode atom1, JmolNode atom2, JmolNode atom3, JmolNode atom4) {
    if (vTemp1 == null) {
      vTemp1 = new Vector3f();
      vTemp2 = new Vector3f();
      vNorm1 = new Vector3f();
      vNorm2 = new Vector3f();
      vNorm3 = new Vector3f();
    }
    getNormalThroughPoints((Point3f) atom1, (Point3f) atom2, (Point3f) atom3, vNorm1, vTemp1,
        vTemp2);
    getNormalThroughPoints((Point3f) atom2, (Point3f) atom3, (Point3f) atom4, vNorm2, vTemp1,
        vTemp2);
    getNormalThroughPoints((Point3f) atom3, (Point3f) atom4, (Point3f) atom1, vNorm3, vTemp1,
        vTemp2);
  }

  public static float getNormalThroughPoints(Point3f pointA, Point3f pointB,
                                             Point3f pointC, Vector3f vNorm, Vector3f vAB, Vector3f vAC) {
              // for Polyhedra
              calcNormalizedNormal(pointA, pointB, pointC, vNorm, vAB, vAC);
              // ax + by + cz + d = 0
              // so if a point is in the plane, then N dot X = -d
              vAB.set(pointA);
              float d = -vAB.dot(vNorm);
              return d;
            }

  public static void calcNormalizedNormal(Point3f pointA, Point3f pointB,
                                          Point3f pointC, Vector3f vNormNorm, Vector3f vAB, Vector3f vAC) {
                                     vAB.sub(pointB, pointA);
                                     vAC.sub(pointC, pointA);
                                     vNormNorm.cross(vAB, vAC);
                                     vNormNorm.normalize();
                                   }

  public static float distanceToPlane(Vector3f norm, float w, Point3f pt) {
    return (norm == null ? Float.NaN 
        : (norm.x * pt.x + norm.y * pt.y + norm.z * pt.z + w)
        / (float) Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z
            * norm.z));
  }


  private boolean isDiaxial(SmilesAtom sAtom, JmolNode atom2, JmolNode atom1) {
    JmolNode atom0 = getJmolAtom(sAtom.getMatchingAtom());
    vA.set((Point3f) atom0);
    vB.set((Point3f) atom0);
    vA.sub((Point3f) atom1);
    vB.sub((Point3f) atom2);
    // about 172 degrees
    return (vA.dot(vB) < -0.95f);
  }

  private JmolNode getJmolAtom(int i) {
    return (i < 0 ? null : jmolAtoms[i]);
  }

  private JmolNode getHydrogens(JmolNode atom, BitSet bsHydrogens) {
    JmolEdge[] b = atom.getEdges();
    JmolNode atomH = null;
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
  private int getChirality(JmolNode a, JmolNode b, JmolNode c, JmolNode pt) {
    float d = getNormalThroughPoints((Point3f) a, (Point3f) b, (Point3f) c, vTemp, vA, vB);
    return (distanceToPlane(vTemp, d, (Point3f) pt) > 0 ? 1 : 2);
  }

  public void setRingData(BitSet bsA) {
    boolean needAromatic = (bsA == null);
    // when using "xxx".find("search","....")
    // or $(...), the aromatic set has already been determined
    if (!needAromatic)  {
      bsAromatic.clear();
      bsAromatic.or(bsA);
    }
    if (needRingData) {
      ringCounts = new int[jmolAtomCount];
      ringConnections = new int[jmolAtomCount];
      ringData = new BitSet[ringDataMax + 1];
    }
    ringSets = new StringBuffer();
    String s = "****";
    while (s.length() < ringDataMax)
      s += s;
    for (int i = 3; i < ringDataMax + 1; i++) {
      Vector v = (Vector) getBitSets("*1" + s.substring(0, i - 2) + "*1",
          false, ringSets);
      for (int r = v.size(); --r >= 0;) {
        BitSet bs = (BitSet) v.get(r);
        if (needAromatic && SmilesAromatic.isFlatSp2Ring(jmolAtoms, bsSelected, bs, 0.01f))
          for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
            bsAromatic.set(j);
      }
      if (needRingData) {
        ringData[i] = new BitSet();
        for (int k = 0; k < v.size(); k++) {
          BitSet r = (BitSet) v.get(k);
          ringData[i].or(r);
          for (int j = r.nextSetBit(0); j >= 0; j = r.nextSetBit(j + 1))
            ringCounts[j]++;
        }
      }
    }
    if (needRingData) {
      for (int i = 0; i < jmolAtomCount; i++) {
        JmolNode atom = jmolAtoms[i];
        JmolEdge[] bonds = atom.getEdges();
        if (bonds != null)
          for (int k = bonds.length; --k >= 0;)
            if (ringCounts[atom.getBondedAtomIndex(k)] > 0)
              ringConnections[i]++;
      }
    }
  }

  private Object getBitSets(String smarts, boolean firstAtomOnly, StringBuffer ringSets) {
    SmilesSearch search;
    try {
      search = SmilesParser.getMolecule(true, smarts);
    } catch (InvalidSmilesException e) {
      return null;
    }
    search.isSilent = true;
    search.bsSelected = bsSelected;
    search.bsRequired = bsRequired;
    search.bsNot = bsNot;
    search.jmolAtoms = jmolAtoms;
    search.jmolAtomCount = jmolAtomCount;
    search.htNested = htNested;
    search.isSmilesFind = isSmilesFind;
    search.isAll = true;
    // note - we do NOT pass on bsSelectOut
    if (firstAtomOnly) {
      search.setRingData(bsAromatic);
      search.ringDataMax = ringDataMax;
      search.ringData = ringData;
      search.ringCounts = ringCounts;
      search.ringConnections = ringConnections;
    }
    search.ringSets = ringSets;
    search.asVector = !firstAtomOnly;
    return search.search(firstAtomOnly);
  }

}
