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
import org.jmol.api.JmolMolecularGraph;
import org.jmol.api.JmolMolecule;
import org.jmol.api.JmolNode;

/**
 *  -- was SmilesMolecule, 
 * but this now includes more data than that and the search itself
 * so as to keep this thread safe
 * 
 */
public class SmilesSearch extends JmolMolecule implements JmolMolecularGraph {

  public String toString() {
    StringBuffer sb = new StringBuffer(pattern);
    sb.append("\nmolecular formula: " + getMolecularFormula());
    return sb.toString();    
  }
  
  private final static int INITIAL_ATOMS = 16;
  SmilesAtom[] patternAtoms = new SmilesAtom[INITIAL_ATOMS];

  // JmolMolecularGraph interface -- when matching a SMILES string
  
  public JmolNode[] getAtoms() {
    return patternAtoms;
  }

  /* ============================================================= */
  /*                             Setup                             */
  /* ============================================================= */

  String pattern;
  JmolNode[] jmolAtoms;
  int jmolAtomCount;
  BitSet bsSelected;
  BitSet bsRequired;
  BitSet bsNot;      
  boolean isAll;
  boolean isSmarts;
  boolean isSmilesFind;
  
  boolean hasRings;
  boolean haveSelected;
  boolean haveBondStereochemistry;
  boolean haveAtomStereochemistry;
  boolean needRingData;
  boolean needAromatic;
  boolean needRingMemberships;
  int ringDataMax = 8;

  boolean asVector;
  boolean getMaps;

  void setAtomArray() {
    nodes = patternAtoms;
    if (patternAtoms.length > atomCount) {
      SmilesAtom[] tmp = new SmilesAtom[atomCount];
      System.arraycopy(patternAtoms, 0, tmp, 0, atomCount);
      nodes = patternAtoms = tmp;
    }
  }

  SmilesAtom addAtom() {
    if (atomCount >= patternAtoms.length) {
      SmilesAtom[] tmp = new SmilesAtom[patternAtoms.length * 2];
      System.arraycopy(patternAtoms, 0, tmp, 0, patternAtoms.length);
      patternAtoms = tmp;
    }
    SmilesAtom sAtom = new SmilesAtom(atomCount);
    patternAtoms[atomCount] = sAtom;
    atomCount++;
    return sAtom;
  }

  int addNested(String pattern) {
    if (htNested == null)
      htNested = new Hashtable();
    htNested.put("_" + (++nNested), pattern);
    return nNested;
  }
  
  int getMissingHydrogenCount() {
    int n = 0;
    int nH;
    for (int i = 0; i < atomCount; i++)
      if ((nH = patternAtoms[i].missingHydrogenCount) >= 0)
          n += nH;
    return n;
  }

  void setRingData(BitSet bsA) {
    needAromatic &= (bsA == null);
    // when using "xxx".find("search","....")
    // or $(...), the aromatic set has already been determined
    if (!needAromatic) {
      bsAromatic.clear();
      if (bsA != null)
        bsAromatic.or(bsA);
      if (!needRingMemberships && !needRingData)
        return;
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

    for (int i = 3; i <= ringDataMax; i++) {
      if (i > jmolAtomCount)
        continue;
      Vector v = (Vector) getBitSets("*1" + s.substring(0, i - 2) + "*1",
          false, ringSets);
      if (needAromatic)
        for (int r = v.size(); --r >= 0;) {
          BitSet bs = (BitSet) v.get(r);
          if (SmilesAromatic.isFlatSp2Ring(
              jmolAtoms, bsSelected, bs, 0.01f))
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

  //private boolean isSilent;

  private Object getBitSets(String smarts, boolean firstAtomOnly, StringBuffer ringSets) {
    SmilesSearch search;
    try {
      search = SmilesParser.getMolecule(smarts, true);
    } catch (InvalidSmilesException e) {
      return null;
    }
    //search.isSilent = true;
    search.bsSelected = bsSelected;
    search.bsRequired = bsRequired;
    search.bsNot = bsNot;
    search.jmolAtoms = jmolAtoms;
    search.jmolAtomCount = jmolAtomCount;
    search.htNested = htNested;
    search.isSmilesFind = isSmilesFind;
    search.isAll = true;
    // note - we do NOT pass on bsSelectOut
    if (firstAtomOnly)
      search.setRingData(bsAromatic);
    else
      search.isRingCheck = true;
    search.ringSets = ringSets;
    search.asVector = !firstAtomOnly;
    return search.search(firstAtomOnly);
  }
  
  //  private data 
  
  private BitSet[] ringData;
  private int[] ringCounts;
  private int[] ringConnections;
  private StringBuffer ringSets;
  private boolean isRingCheck;
  
  private BitSet bsAromatic = new BitSet();
  BitSet getBsAromatic() {
    return bsAromatic;
  }
  
  private Hashtable htNested;
  private int nNested;

  private Vector vReturn;
  private BitSet bsReturn = new BitSet();
    


  /* ============================================================= */
  /*                             Search                            */
  /* ============================================================= */

  
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
    //System.out.println("smilesseearch search" + pattern);
    if (atomCount > 0) {
      SmilesAtom atom0 = patternAtoms[0];
      boolean skipGroup = (atom0.isBioAtom && atom0.atomName == null);
      for (int i = 0; i < jmolAtomCount; i++) {
        bsFound.clear();
        if (!checkMatch(atom0, 0, i, firstAtomOnly))
          break;
        if (skipGroup) {
          int j = jmolAtoms[i].getOffsetResidueAtom(null, 1);
          if (j > i)
            i = j - 1;
        }
      }
    }
    return (asVector ? (Object) vReturn : bsReturn);
  }

  private BitSet bsFound = new BitSet();
  
  /**
   * Check for a specific match of a model set atom with a pattern position
   * 
   * @param patternAtom
   *          Atom of the pattern that is currently tested.
   * @param atomNum
   *          Current atom of the pattern.
   * @param iAtom
   *          Atom number of the Jmol atom that is currently tested to match
   *          <code>patternAtom</code>.
   * @param firstAtomOnly
   *          TODO
   * @return true to continue or false if oneOnly
   */
  private final boolean checkMatch(SmilesAtom patternAtom, int atomNum,
                                   int iAtom, boolean firstAtomOnly) {

    // check for requested selection or not-selection

    if (bsFound.get(iAtom) || bsNot != null && bsNot.get(iAtom)
        || bsSelected != null && !bsSelected.get(iAtom))
      return true;

    JmolNode atom = jmolAtoms[iAtom];

    // check atoms 

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

    JmolEdge[] bonds = atom.getEdges();

    for (int i = 0; i < patternAtom.getCovalentBondCount(); i++) {
      SmilesBond patternBond = patternAtom.getBond(i);
      // Check only if the current atom is the second atom of the bond
      if (patternBond.getAtomIndex2() != patternAtom.index)
        continue;
      // note that there might be more than one of these.
      // in EACH case we need to ensure that the actual
      // bonds to the previously assigned atoms matches
      
      SmilesAtom atom1 = patternBond.getAtom1();
      int matchingAtom = atom1.getMatchingAtom();
      
      // BIOSMILES/BIOSMARTS check is by group
      
      switch (patternBond.bondType) {
      case SmilesBond.TYPE_BIO_SEQUENCE:
        continue; // no bond check here
      case SmilesBond.TYPE_BIO_PAIR:
        if (jmolAtoms[iAtom].isCrossLinked(jmolAtoms[matchingAtom]))
          continue;
        break;
      default:

        // regular SMILES/SMARTS check 
        // is to find the bond and test it against the pattern
        
        int k = 0;
        for (; k < bonds.length; k++)
          if ((bonds[k].getAtomIndex1() == matchingAtom || bonds[k]
              .getAtomIndex2() == matchingAtom)
              && bonds[k].isCovalent())
            break;
        if (k == bonds.length)
          return true; // probably wasn't a covalent bond

        if (!checkMatchBond(patternAtom, atom1, patternBond, iAtom,
            matchingAtom, bonds[k]))
          return true;
      }
    }

    // The atom has passed both the atom and the bond test.
    // Add this atom to the growing list.

    patternAtoms[patternAtom.index].setMatchingAtom(iAtom);

    // Note that we explicitly do a reference using
    // index because this could be a SEARCH [x,x] "sub" atom.

    if (++atomNum == atomCount) {
      if (!checkStereochemistry())
        return true;
      BitSet bs = new BitSet();
      for (int j = 0; j < atomCount; j++) {
        int i = patternAtoms[j].getMatchingAtom();
        if (!firstAtomOnly && haveSelected && !patternAtoms[j].selected)
          continue;
        bs.set(i);
        // note: bioSequences only return the "lead" atom. 
        if (patternAtoms[j].isBioAtom && patternAtoms[j].atomName == null)
          jmolAtoms[i].setGroupBits(bs);
        if (firstAtomOnly)
          break;
        if (!isSmarts && patternAtoms[j].missingHydrogenCount > 0)
          getHydrogens(jmolAtoms[i], bs);
      }
      if (bsRequired != null && !bsRequired.intersects(bs))
        return true;
      bsReturn.or(bs);
      if (asVector) {
        if (getMaps) {
          // every map is important always
          int[] map = new int[atomCount];
          for (int j = 0; j < atomCount; j++)
            map[j] = patternAtoms[j].getMatchingAtom();
          vReturn.add(map);
        } else {
          boolean isOK = true;
          for (int j = vReturn.size(); --j >= 0 && isOK;)
            isOK = !(((BitSet) vReturn.get(j)).equals(bs));
          if (!isOK)
            return true;
          vReturn.add(bs);
        }
      }
      /*
       * if (!isSilent && Logger.debugging) { StringBuffer s = new
       * StringBuffer(); for (int k = 0; k < atomNum; k++)
       * s.append("-").append(atoms[k].getMatchingAtom());
       * s.append(" ").append(atomNum
       * ).append("/").append(getPatternAtomCount());
       * Logger.debug(s.toString()); Logger.debug("match: " + bs); }
       */
      if (isRingCheck) {
        ringSets.append(" ");
        for (int k = atomNum * 3 + 2; --k > atomNum;)
          ringSets.append("-").append(
              patternAtoms[(k <= atomNum * 2 ? atomNum * 2 - k + 1 : k - 1)
                  % atomNum].getMatchingAtom());
        ringSets.append("- ");
      }
      return (isAll && bsReturn.cardinality() != jmolAtomCount);
    }

    // so far, so good... on to the next position...

    bsFound.set(iAtom);
    patternAtom = patternAtoms[atomNum];
    // for all the pattern bonds for this atom...
    // find the bond to atoms already assigned
    // note that it must be there, because SMILES strings
    // are parsed in order, from left to right. You can't
    // have two fragments going at the same time. 

    SmilesBond patternBond = null;
    for (int i = 0; i < patternAtom.getCovalentBondCount(); i++) {
      if ((patternBond = patternAtom.getBond(i)).getAtom2() == patternAtom)
        break;
      patternBond = null;
    }
    if (patternBond == null) {

      // Option 1: we are processing ".":

      // Question: What does "." mean?
      // Answer: For SMILES strings it means "start a new entity"
      //   UNLESS, of course, a ring connector is connecting them.
      // For SMARTS it simply means "not connected to the previous atom."

      BitSet bs = new BitSet();
      bs.or(bsFound);
      SmilesAtom pa = patternAtoms[patternAtom.notBondedIndex];
      JmolNode a = jmolAtoms[pa.getMatchingAtom()];
      if (pa.isBioSequence) {
        // clear out adjacent residues
        int ii = a.getOffsetResidueAtom("0", 1);
        if (ii >= 0)
          bs.set(ii);
        ii = a.getOffsetResidueAtom("0", -1);
        if (ii >= 0)
          bs.set(ii);
      } else {
        // clear out all atoms connected to the last atom only
        bonds = a.getEdges();
        for (int k = 0; k < bonds.length; k++)
          bs.set(bonds[k].getOtherAtom(a).getIndex());
      }
      for (int j = 0; j < jmolAtomCount; j++)
        if (!bs.get(j) && !checkMatch(patternAtom, atomNum, j, firstAtomOnly))
          return false;

      bsFound.clear(iAtom);
      return true;

    }

    // The new atom is connected to the old one in the pattern.
    // It doesn't so much matter WHICH connection we found -- 
    // there may be several -- but whatever we have, we must
    // have a connection in the real molecule between these two
    // particular atoms. So we just follow that connection. 

    SmilesAtom atom1 = patternBond.getAtom1();
    atom = jmolAtoms[atom1.getMatchingAtom()];

    // Option 2: The connecting bond is a bio sequence or
    // from ~GGC(T)C:ATTC...
    // For sequences, we go to the next GROUP, either via
    // the standard sequence or via basepair/cysteine pairing. 

    switch (patternBond.bondType) {
    case SmilesBond.TYPE_BIO_SEQUENCE:
      int nextGroupAtom = atom.getOffsetResidueAtom(patternAtom.atomName, 1);
      if (nextGroupAtom >= 0) {
        BitSet bs = new BitSet();
        atom1.setGroupBits(bs);
        bsFound.or(bs);
        if (!checkMatch(patternAtom, atomNum, nextGroupAtom, firstAtomOnly))
          return false;
        bsFound.andNot(bs);
        return true;
      }
    case SmilesBond.TYPE_BIO_PAIR:
      Vector vLinks = new Vector();
      atom.getCrossLinkLeadAtomIndexes(vLinks);
      BitSet bs = new BitSet();
      atom1.setGroupBits(bs);
      bsFound.or(bs);
      for (int j = 0; j < vLinks.size(); j++)
        if (!checkMatch(patternAtom, atomNum, ((Integer) vLinks.get(j)).intValue(),
            firstAtomOnly))
          return false;
      bsFound.andNot(bs);
      return true;
    }

    // Option 3: Standard practice
    // Run through the bonds of that assigned atom
    // to see if any match this new connection.

    bonds = atom.getEdges();
    if (bonds != null)
      for (int j = 0; j < bonds.length; j++)
        if (!checkMatch(patternAtom, atomNum, atom.getBondedAtomIndex(j),
            firstAtomOnly))
          return false;

    // Done checking this atom from any one of the places
    // higher in this stack. Clear the atom and keep going...

    bsFound.clear(iAtom);
    return true;
  }

  private JmolNode getHydrogens(JmolNode atom, BitSet bsHydrogens) {
    JmolEdge[] b = atom.getEdges();
    JmolNode atomH = null;
    for (int k = 0, i = 0; i < b.length; i++)
      if ((atomH = jmolAtoms[k = atom.getBondedAtomIndex(i)]).getElementNumber() == 1) {
        if (bsHydrogens == null)
          break;
        bsHydrogens.set(k);
      }
    return atomH;
  }

  private boolean checkPrimitiveAtom(SmilesAtom patternAtom, int iAtom) {
    JmolNode atom = jmolAtoms[iAtom];
    boolean foundAtom = patternAtom.not;
    while (true) {

      int n;

      if (patternAtom.isBioAtom) {

        // BIOSMARTS
        if (patternAtom.atomName != null
            && (patternAtom.isLeadAtom() ? !atom.isLeadAtom()
                : !patternAtom.atomName.equals(atom.getAtomName().toUpperCase())))
          break;

        if (patternAtom.residueName != null
            && !patternAtom.residueName.equals(atom.getGroup3(false)
                .toUpperCase()))
          break;

        if (patternAtom.residueChar != null
            && !patternAtom.residueChar
                .equals(atom.getGroup1('\0').toUpperCase()))
          break;


      } else {
        // _ <n> apply "recursive" SEARCH -- for example, [C&$(C[$(aaaO);$(aaC)])]"
        if (patternAtom.iNested > 0) {
          Object o = htNested.get("_" + patternAtom.iNested);
          if (!(o instanceof BitSet))
            htNested.put("_" + patternAtom.iNested, o = getBitSets((String) o,
                true, ringSets));
          foundAtom = (patternAtom.not != (((BitSet) o).get(iAtom)));
          break;
        }

        // "=" <n>  Jmol index

        if (patternAtom.jmolIndex > 0
            && atom.getIndex() != patternAtom.jmolIndex)
          break;

        // # <n> or Symbol Check atomic number
        short targetAtomicNumber = patternAtom.elementNumber;
        n = atom.getElementNumber();
        if (targetAtomicNumber >= 0 && targetAtomicNumber != n)
          break;

        // Check aromatic
        boolean isAromatic = patternAtom.isAromatic();
        if (targetAtomicNumber != -2 && isAromatic != bsAromatic.get(iAtom))
          break;

        // <n> Check isotope
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
        n = patternAtom.missingHydrogenCount;
        if (n >= 0 && n != atom.getCovalentHydrogenCount())
          break;

        // h implicit H count
        n = patternAtom.implicitHydrogenCount;
        if (n != Integer.MIN_VALUE) {
          int nH = atom.getImplicitHydrogenCount();
          if (n == -1 ? nH == 0 : n != nH)
            break;
        }

        // D <n> degree
        if (patternAtom.degree > 0
            && patternAtom.degree != atom.getCovalentBondCount())
          break;

        // d <n> degree
        if (patternAtom.nonhydrogenDegree > 0
            && patternAtom.nonhydrogenDegree != atom.getCovalentBondCount()
                - atom.getCovalentHydrogenCount())
          break;

        // v <n> valence
        if (patternAtom.valence > 0 && patternAtom.valence != atom.getValence())
          break;

        // X <n> connectivity ?
        if (patternAtom.connectivity > 0
            && patternAtom.connectivity != atom.getCovalentBondCount()
                + atom.getImplicitHydrogenCount())
          break;

        // r <n>
        if (ringData != null && patternAtom.ringSize >= -1) {
          if (patternAtom.ringSize <= 0) {
            if ((ringCounts[iAtom] == 0) != (patternAtom.ringSize == 0))
              break;
          } else if (ringData[patternAtom.ringSize] == null
              || !ringData[patternAtom.ringSize].get(iAtom)) {
            break;
          }
        }
        // R <n>
        if (ringData != null && patternAtom.ringMembership >= -1) {
          //  R --> -1 implies "!R0"
          if (patternAtom.ringMembership == -1 ? ringCounts[iAtom] == 0
              : ringCounts[iAtom] != patternAtom.ringMembership)
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

      }
      foundAtom = !foundAtom;
      break;
    }

    return foundAtom;
  }

  private boolean checkMatchBond(SmilesAtom patternAtom, SmilesAtom atom1,
                                 SmilesBond patternBond, int iAtom,
                                 int matchingAtom, JmolEdge bond) {

    // apply SEARCH [ , , & ; ] logic

    if (patternBond.bondsOr != null) {
      for (int ii = 0; ii < patternBond.nBondsOr; ii++)
        if (checkMatchBond(patternAtom, atom1, patternBond.bondsOr[ii], iAtom,
            matchingAtom, bond))
          return true;
      return false;
    }

    if (patternBond.primitives == null) {
      if (!checkPrimitiveBond(patternAtom, atom1, patternBond, iAtom, matchingAtom, bond))
        return false;
    } else {
      for (int i = 0; i < patternBond.nPrimitives; i++)
        if (!checkPrimitiveBond(patternAtom, atom1, patternBond.primitives[i], iAtom, matchingAtom, bond))
          return false;
    }
    return true;
  }

  private boolean checkPrimitiveBond(SmilesAtom patternAtom, SmilesAtom atom1,
                                     SmilesBond patternBond, int iAtom,
                                     int matchingAtom, JmolEdge bond) {
    boolean bondFound = false;
    boolean isAromatic = patternAtom.isAromatic();

    int order = bond.getCovalentOrder();
    if (isAromatic && atom1.isAromatic()) {
      switch (patternBond.bondType) {
      case SmilesBond.TYPE_AROMATIC: // :
      case SmilesBond.TYPE_DOUBLE:
      case SmilesBond.TYPE_RING:
        bondFound = isRingBond(iAtom, matchingAtom);
        break;
      case SmilesBond.TYPE_SINGLE:
        bondFound = !isRingBond(iAtom, matchingAtom);
        break;
      case SmilesBond.TYPE_ANY:
      case SmilesBond.TYPE_UNKNOWN:
        bondFound = true;
        break;
      }
    } else {
      switch (patternBond.bondType) {
      case SmilesBond.TYPE_ANY:
      case SmilesBond.TYPE_UNKNOWN:
        bondFound = true;
        break;
      case SmilesBond.TYPE_SINGLE:
      case SmilesBond.TYPE_DIRECTIONAL_1:
      case SmilesBond.TYPE_DIRECTIONAL_2:
        // STEREO_NEAR and _FAR are stand-ins for find()
        bondFound = (order == JmolEdge.BOND_COVALENT_SINGLE);
        break;
      case SmilesBond.TYPE_DOUBLE:
        bondFound = (order == JmolEdge.BOND_COVALENT_DOUBLE);
        break;
      case SmilesBond.TYPE_TRIPLE:
        bondFound = (order == JmolEdge.BOND_COVALENT_TRIPLE);
        break;
      case SmilesBond.TYPE_RING:
        bondFound = isRingBond(iAtom, matchingAtom);
        break;
      }
    }
    return bondFound != patternBond.isNot;
  }

  private boolean isRingBond(int i, int j) {
    return (ringSets.indexOf("-" + i + "-" + j + "-") >= 0);
  }
  
  /* ============================================================= */
  /*                          Stereochemistry                      */
  /* ============================================================= */

  private boolean checkStereochemistry() {

    // first, @ stereochemistry

    if (haveAtomStereochemistry) {
      JmolNode atom1 = null, atom2 = null, atom3 = null, 
      atom4 = null, atom5 = null, atom6 = null;
      for (int i = 0; i < atomCount; i++) {
        SmilesAtom sAtom = patternAtoms[i];
        JmolNode atom0 = jmolAtoms[sAtom.getMatchingAtom()];
        int nH = sAtom.missingHydrogenCount;
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
          while (sAtom1.getCovalentBondCount() == 2 && sAtom2.getCovalentBondCount() == 2) {
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
          nH = sAtom2.missingHydrogenCount;
          atom3 = getJmolAtom(sAtom2.getMatchingBondedAtom(1));
          atom4 = getJmolAtom(sAtom2.getMatchingBondedAtom(2));
          if (atom3 == null || atom4 == null && nH != 1)
            continue;
          if (atom4 == null)
            atom4 = getHydrogens(getJmolAtom(sAtom2.getMatchingAtom()), null);
          if (isSmilesFind && !setSmilesCoordinates(sAtom, new JmolNode[] { atom1, atom2, atom3, atom4, null, null }))
              return false;
          if (sAtom.not != (getChirality(atom2, atom3, atom4, atom1, v) != order))
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

          if (isSmilesFind && !setSmilesCoordinates(sAtom, new JmolNode[] { atom1, atom2, atom3, atom4, atom5, atom6 }))
              return false;
          
          if (!checkChirality(sAtom.not, atom0, chiralClass, order, atom1, atom2, atom3, atom4, atom5, atom6, v))
            return false;
          continue;
        }
      }
    }
    // next, /C=C/ double bond stereochemistry

    if (haveBondStereochemistry) {
      for (int k = 0; k < atomCount; k++) {
        SmilesAtom sAtom1 = patternAtoms[k];
        SmilesAtom sAtom2 = null;
        SmilesAtom sAtomDirected1 = null;
        SmilesAtom sAtomDirected2 = null;
        int dir1 = 0;
        int dir2 = 0;

        int nBonds = sAtom1.getCovalentBondCount();
        for (int j = 0; j < nBonds; j++) {
          SmilesBond b = sAtom1.getBond(j);
          boolean isAtom2 = (b.getAtom2() == sAtom1);
          int type = b.bondType;
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
        nBonds = sAtom2.getCovalentBondCount();
        for (int j = 0; j < nBonds && dir2 == 0; j++) {
          SmilesBond b = sAtom2.getBond(j);
          boolean isAtom2 = (b.getAtom2() == sAtom2);
          int type = b.bondType;
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

  static boolean checkChirality(boolean isNot, JmolNode atom0, int chiralClass, int order, 
                                JmolNode atom1, JmolNode atom2, JmolNode atom3, JmolNode atom4, JmolNode atom5, JmolNode atom6, VTemp v) {
    
    switch (chiralClass) {
    case SmilesAtom.CHIRALITY_TETRAHEDRAL:
      return (isNot == (getChirality(atom2, atom3, atom4, atom1, v) != order));
    case SmilesAtom.CHIRALITY_TRIGONAL_BIPYRAMIDAL:
      // check for axial-axial'
      return (isNot == (!isDiaxial(atom0, atom5, atom1, v)
          || getChirality(atom2, atom3, atom4, atom1, v) != order));
    case SmilesAtom.CHIRALITY_OCTAHEDRAL:
      if (isNot != (!isDiaxial(atom0, atom6, atom1, v)))
        return false;
      // check for CW or CCW set
      getPlaneNormals(atom2, atom3, atom4, atom5, v);
      if (isNot != (v.vNorm1.dot(v.vNorm2) < 0 
          || v.vNorm2.dot(v.vNorm3) < 0))
        return false;
      // now check rotation in relation to the first atom
      v.vNorm2.set((Point3f) atom0);
      v.vNorm2.sub((Point3f) atom1);
      return (isNot == ((v.vNorm2.dot(v.vNorm1) < 0 ? 1 : 2) != order));
    case SmilesAtom.CHIRALITY_SQUARE_PLANAR:
      getPlaneNormals(atom1, atom2, atom3, atom4, v);
      // vNorm1 vNorm2 vNorm3 are right-hand normals for the given
      // triangles
      // 1-2-3, 2-3-4, 3-4-1
      // sp1 up up up U-shaped
      // sp2 up up DOWN 4-shaped
      // sp3 up DOWN DOWN Z-shaped

      if (v.vNorm1.dot(v.vNorm2) < 0) {
        if (isNot != (order != 3))
          return false;
      } else if (isNot != (v.vNorm2.dot(v.vNorm3) < 0)) {
        if (order != 2)
          return false;
      } else if (isNot != (order != 1)) {
        return false;
      }
      return true;
    }
    return true;
  }
  
  private JmolNode getJmolAtom(int i) {
    return (i < 0 || i >= jmolAtoms.length ? null : jmolAtoms[i]);
  }

  private void setSmilesBondCoordinates(SmilesAtom sAtom1, SmilesAtom sAtom2) {
    JmolNode dbAtom1 = jmolAtoms[sAtom1.getMatchingAtom()];
    JmolNode dbAtom2 = jmolAtoms[sAtom2.getMatchingAtom()];
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
      atom.set(-1, (nBonds++ == 0) ? -1 : 1, 0);
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
      atom.set(1, (nBonds++ == 0) ? 1 : -1, 0);
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

  private boolean setSmilesCoordinates(SmilesAtom sAtom, JmolNode[] cAtoms) {

    // When testing equality of two SMILES strings in terms of stereochemistry,
    // we need to set the atom positions based on the ORIGINAL SMILES order,
    // which, except for the H atom, will be the same as the "matchedAtom"
    // index.
    // all the necessary information is passed via the atomSite field of Atom

    int iAtom = sAtom.getMatchingAtom();
    JmolNode atom = jmolAtoms[iAtom];
    // atomSite is used by smilesMatch.find to encode chiralClass and chiralOrder 
    int atomSite = atom.getAtomSite();
    if (atomSite == Integer.MIN_VALUE)
      return false;
    int chiralClass = atomSite >> 8;
    int chiralOrder = atomSite & 0xFF;
    if (chiralClass != sAtom.getChiralClass())
      return false;

    // set the chirality center at the origin
    atom.set(0, 0, 0);

    // Here is the secret:
    // Sort the atoms by the origintal order of bonds
    // in the SMILES string that generated the
    // atom set.
    int[] map = new int[cAtoms[4] == null ? 4 : cAtoms[5] == null ? 5 : 6];
    JmolEdge[] bonds = atom.getEdges();
    for (int i = 0; i < map.length; i++) {
      map[i] = cAtoms[i].getIndex();
      // System.out.println("i=" + i + "; cAtoms[i]=" + map[i]);
    }
    int k;
    JmolNode a2 = null;
    JmolEdge[] b2 = null;
    if (chiralClass == 2) {
      a2 = bonds[1].getOtherAtom(atom);
      b2 = a2.getEdges();
      atom = bonds[0].getOtherAtom(atom);
      bonds = atom.getEdges();
    }
    for (int i = 0; i < map.length; i++) {
      for (k = 0; k < bonds.length; k++)
        if (bonds[k].getOtherAtom(atom) == cAtoms[i])
          break;
      if (k < bonds.length) {
        map[i] = (k * 10 + 100) + i;
      } else if (a2 != null) {
        for (k = 0; k < b2.length; k++)
          if (b2[k].getOtherAtom(a2) == cAtoms[i])
            break;
        map[i] = (k * 10 + 200) + i;
      }
    }
    Arrays.sort(map);
    for (int i = 0; i < map.length; i++) {
      map[i] = map[i] % 10;
      // System.out.println("i=" + i + "; map[i]=" + map[i] + " a=" +
      // cAtoms[map[i]].getIndex());
    }
    switch (chiralClass) {
    case SmilesAtom.CHIRALITY_ALLENE:
    case SmilesAtom.CHIRALITY_TETRAHEDRAL:
      if (chiralOrder == 2) {
        int i = map[0];
        map[0] = map[1];
        map[1] = i;
      }
      cAtoms[map[0]].set(0, 0, 1);
      cAtoms[map[1]].set(1, 0, -1);
      cAtoms[map[2]].set(0, 1, -1);
      cAtoms[map[3]].set(-1, -1, -1);
      break;
    case SmilesAtom.CHIRALITY_SQUARE_PLANAR:
      switch (chiralOrder) {
      case 1: // U-shaped
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(0, 1, 0);
        cAtoms[map[2]].set(-1, 0, 0);
        cAtoms[map[3]].set(0, -1, 0);
        break;
      case 2: // 4-shaped
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(-1, 0, 0);
        cAtoms[map[2]].set(0, 1, 0);
        cAtoms[map[3]].set(0, -1, 0);
        break;
      case 3: // Z-shaped
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(0, 1, 0);
        cAtoms[map[2]].set(0, -1, 0);
        cAtoms[map[3]].set(-1, 0, 0);
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
      cAtoms[map[0]].set(0, 0, 1);
      cAtoms[map[n - 1]].set(0, 0, -1);
      cAtoms[map[1]].set(1, 0, 0);
      cAtoms[map[2]].set(0, 1, 0);
      cAtoms[map[3]].set(-1, 0, 0);
      if (n == 6)
        cAtoms[map[4]].set(0, -1, 0);
      break;
    }
    return true;
  }

  static class VTemp {
    final Vector3f vTemp = new Vector3f();
    final Vector3f vA = new Vector3f();
    final Vector3f vB = new Vector3f();
    Vector3f vTemp1;
    Vector3f vTemp2;
    Vector3f vNorm1;
    Vector3f vNorm2;
    Vector3f vNorm3;
  }
  
  VTemp v = new VTemp();

  
  private static boolean isDiaxial(JmolNode atom0, JmolNode atom2, JmolNode atom1, VTemp v) {
    v.vA.set((Point3f) atom0);
    v.vB.set((Point3f) atom0);
    v.vA.sub((Point3f) atom1);
    v.vB.sub((Point3f) atom2);
    // about 172 degrees
    return (v.vA.dot(v.vB) < -0.95f);
  }

  private static int getChirality(JmolNode a, JmolNode b, JmolNode c, JmolNode pt, VTemp v) {
    float d = SmilesAromatic.getNormalThroughPoints(a, b, c, v.vTemp, v.vA, v.vB);
    return (distanceToPlane(v.vTemp, d, (Point3f) pt) > 0 ? 1 : 2);
  }

  private static void getPlaneNormals(JmolNode atom1, JmolNode atom2, JmolNode atom3, JmolNode atom4, VTemp v) {
    if (v.vTemp1 == null) {
      v.vTemp1 = new Vector3f();
      v.vTemp2 = new Vector3f();
      v.vNorm1 = new Vector3f();
      v.vNorm2 = new Vector3f();
      v.vNorm3 = new Vector3f();
    }
    SmilesAromatic.getNormalThroughPoints(atom1, atom2, atom3, v.vNorm1, v.vTemp1,
        v.vTemp2);
    SmilesAromatic.getNormalThroughPoints(atom2, atom3, atom4, v.vNorm2, v.vTemp1,
        v.vTemp2);
    SmilesAromatic.getNormalThroughPoints(atom3, atom4, atom1, v.vNorm3, v.vTemp1,
        v.vTemp2);
  }

  private static float distanceToPlane(Vector3f norm, float w, Point3f pt) {
    return (norm == null ? Float.NaN 
        : (norm.x * pt.x + norm.y * pt.y + norm.z * pt.z + w)
        / (float) Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z
            * norm.z));
  }

  /**
   * 
   * @param atom0
   * @param atoms
   * @param nAtoms
   * @param v
   * @return        String
   */
  static String getStereoFlag(JmolNode atom0, JmolNode[] atoms, int nAtoms, VTemp v) {
    JmolNode atom1 = atoms[0];
    JmolNode atom2 = atoms[1];
    JmolNode atom3 = atoms[2];
    JmolNode atom4 = atoms[3];
    JmolNode atom5 = atoms[4];
    JmolNode atom6 = atoms[5];
    int chiralClass = 0;
    switch (nAtoms) {
    default:
      break;
    case 2:
    case 5:
    case 6:
      // not doing these for now
      break;
    case 4:
      float d = SmilesAromatic.getNormalThroughPoints(atom1, atom2, atom3, v.vTemp, v.vA, v.vB);
      if (Math.abs(distanceToPlane(v.vTemp, d, (Point3f) atom4)) < 0.2f) {
        chiralClass = SmilesAtom.CHIRALITY_SQUARE_PLANAR;
        if (checkChirality(false, atom0, chiralClass, 1, atom1, atom2, atom3, atom4, atom5, atom6, v))
          return "@SP1";
        if (checkChirality(false, atom0, chiralClass, 2, atom1, atom2, atom3, atom4, atom5, atom6, v))
          return "@SP2";
        if (checkChirality(false, atom0, chiralClass, 3, atom1, atom2, atom3, atom4, atom5, atom6, v))
          return "@SP3";       
      } else {
        chiralClass = SmilesAtom.CHIRALITY_TETRAHEDRAL;
        return (checkChirality(false, atom0, chiralClass, 1, atom1, atom2, atom3, atom4, atom5, atom6, v)? "@" : "@@");
      }       
    }
    return "";
  }

}
