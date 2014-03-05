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

import javajs.util.AU;
import javajs.util.List;
import javajs.util.P3;
import javajs.util.SB;
import javajs.util.V3;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.java.BS;

import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Node;
import org.jmol.util.BNode;
import org.jmol.util.Logger;

/**
 *  -- was SmilesMolecule, 
 * but this now includes more data than that and the search itself
 * so as to keep this thread safe
 * 
 */
public class SmilesSearch extends JmolMolecule {

  @Override
  public String toString() {
    SB sb = new SB().append(pattern);
    sb.append("\nmolecular formula: " + getMolecularFormula(true));
    return sb.toString();    
  }
  
  private final static int INITIAL_ATOMS = 16;
  SmilesAtom[] patternAtoms = new SmilesAtom[INITIAL_ATOMS];

  /* ============================================================= */
  /*                             Setup                             */
  /* ============================================================= */

  String pattern;
  Node[] jmolAtoms;
  Node[] smartsAtoms;
  
  BNode[] bioAtoms;
  int jmolAtomCount;
  private BS bsSelected;
  void setSelected(BS bs) {
    if (bs == null) {
      // null because this is an atom set
      // constructed by SmilesParser.getMolecule
      //  "CCCCC".find("SMARTS","C")
      bs = BS.newN(jmolAtomCount);
      bs.setBits(0, jmolAtomCount);
    }
    bsSelected = bs;
  }
  BS bsRequired;
  boolean firstMatchOnly;
  boolean matchAllAtoms;
  boolean isSmarts;
  boolean isSmilesFind;
  SmilesSearch[] subSearches;
  boolean haveSelected;
  boolean haveBondStereochemistry;
  boolean haveAtomStereochemistry;
  boolean needRingData;
  boolean needAromatic = true; // we just have to always consider aromatic, except in the case of bioSequences.
  boolean needRingMemberships;
  int ringDataMax = Integer.MIN_VALUE;
  List<SmilesMeasure> measures = new  List<SmilesMeasure>();
  
  int flags;
  SB ringSets;
  BS bsAromatic = new BS();
  BS bsAromatic5 = new BS();
  BS bsAromatic6 = new BS();
  
  SmilesAtom lastChainAtom;

  boolean asVector;
  boolean getMaps;
  SmilesSearch top = this;
  
  //  private data 
  
  private boolean isSilent;
  private boolean isRingCheck;
  private int selectedAtomCount;
  private BS[] ringData;
  private int[] ringCounts;
  private int[] ringConnections;
  BS bsFound = new BS(); 
  private Map<String, Object> htNested;
  private int nNested;
  private SmilesBond nestedBond;

  private List<Object> vReturn;
  BS bsReturn = new BS();
  private boolean ignoreStereochemistry;
  private boolean noAromatic;
  private boolean aromaticDouble;
    

  void setAtomArray() {
    if (patternAtoms.length > atomCount)
      patternAtoms = (SmilesAtom[]) AU.arrayCopyObject(patternAtoms, atomCount);
    nodes = patternAtoms;
  }

  SmilesAtom addAtom() {
    if (atomCount >= patternAtoms.length)
      patternAtoms = (SmilesAtom[]) AU.doubleLength(patternAtoms);
    SmilesAtom sAtom = new SmilesAtom().setIndex(atomCount);
    patternAtoms[atomCount] = sAtom;
    atomCount++;
    return sAtom;
  }

  int addNested(String pattern) {
    if (top.htNested == null)
      top.htNested = new Hashtable<String, Object>();
    setNested(++top.nNested, pattern);
    return top.nNested;
  }
  
  void clear() {
    bsReturn.clearAll();
    nNested = 0;
    htNested = null;
    nestedBond = null;//new SmilesBond(0, false);
    clearBsFound(-1);
  }
  
  void setNested(int iNested, Object o) {
    top.htNested.put("_" + iNested, o);
  }

  Object getNested(int iNested) {
    return top.htNested.get("_" + iNested);
  }
  
  int getMissingHydrogenCount() {
    int n = 0;
    int nH;
    for (int i = 0; i < atomCount; i++)
      if ((nH = patternAtoms[i].missingHydrogenCount) >= 0)
          n += nH;
    return n;
  }

  void setRingData(BS bsA) throws InvalidSmilesException {
    if (needAromatic)
      needRingData = true;
    boolean noAromatic = ((flags & Edge.FLAG_NO_AROMATIC) != 0);
    needAromatic &= (bsA == null) & !noAromatic;
    // when using "xxx".find("search","....")
    // or $(...), the aromatic set has already been determined
    if (!needAromatic) {
      bsAromatic.clearAll();
      if (bsA != null)
        bsAromatic.or(bsA);
      if (!needRingMemberships && !needRingData)
        return;
    }
    getRingData(needRingData, flags, null);
  }

  @SuppressWarnings("unchecked")
  void getRingData(boolean needRingData, int flags, List<BS>[] vRings)
      throws InvalidSmilesException {
    boolean aromaticStrict = ((flags & Edge.FLAG_AROMATIC_STRICT) != 0);
    boolean aromaticDefined = ((flags & Edge.FLAG_AROMATIC_DEFINED) != 0);
    if (aromaticStrict && vRings == null)
      vRings = AU.createArrayOfArrayList(4); 
    if (aromaticDefined && needAromatic) {
      // predefined aromatic bonds
      bsAromatic = SmilesAromatic.checkAromaticDefined(jmolAtoms, bsSelected);
      aromaticStrict = false;
    }
    if (ringDataMax < 0)
      ringDataMax = 8;
    if (aromaticStrict && ringDataMax < 6)
      ringDataMax = 6;
    if (needRingData) {
      ringCounts = new int[jmolAtomCount];
      ringConnections = new int[jmolAtomCount];
      ringData = new BS[ringDataMax + 1];
    }

    ringSets = new SB();
    String s = "****";
    while (s.length() < ringDataMax)
      s += s;

    List<Object> v5 = null;
    for (int i = 3; i <= ringDataMax; i++) {
      if (i > jmolAtomCount)
        continue;
      String smarts = "*1" + s.substring(0, i - 2) + "*1";
      SmilesSearch search = SmilesParser.getMolecule(smarts, true);
      List<Object> vR = (List<Object>) subsearch(search, false, true);
      if (vRings != null && i <= 5) {
        List<BS> v = new  List<BS>();
        for (int j = vR.size(); --j >= 0; )
          v.addLast((BS) vR.get(j));
        vRings[i-3] = v;
      }
      if (needAromatic) {
        if (!aromaticDefined && (!aromaticStrict || i == 5 || i == 6))
          for (int r = vR.size(); --r >= 0;) {
            BS bs = (BS) vR.get(r);
            if (aromaticDefined
                || SmilesAromatic.isFlatSp2Ring(jmolAtoms, bsSelected, bs,
                    (aromaticStrict ? 0.1f : 0.01f)))
              bsAromatic.or(bs);
          }
        if (aromaticStrict) {
          switch (i) {
          case 5:
            v5 = vR;
            break;
          case 6:
            if (aromaticDefined)
              bsAromatic = SmilesAromatic.checkAromaticDefined(jmolAtoms,
                  bsAromatic);
            else
              SmilesAromatic.checkAromaticStrict(jmolAtoms, bsAromatic, v5, vR);
            vRings[3] = new  List<BS>();
            setAromatic56(v5, bsAromatic5, 5, vRings[3]);
            setAromatic56(vR, bsAromatic6, 6, vRings[3]);
            break;
          }
        }
      }
      if (needRingData) {
        ringData[i] = new BS();
        for (int k = 0; k < vR.size(); k++) {
          BS r = (BS) vR.get(k);
          ringData[i].or(r);
          for (int j = r.nextSetBit(0); j >= 0; j = r.nextSetBit(j + 1))
            ringCounts[j]++;
        }
      }
    }
    if (needRingData) {
      for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
          .nextSetBit(i + 1)) {
        Node atom = jmolAtoms[i];
        Edge[] bonds = atom.getEdges();
        if (bonds != null)
          for (int k = bonds.length; --k >= 0;)
            if (ringCounts[atom.getBondedAtomIndex(k)] > 0)
              ringConnections[i]++;
      }
    }
  }

  private void setAromatic56(List<Object> vRings, BS bs56, int n56, List<BS> vAromatic56) {
    for (int k = 0; k < vRings.size(); k++) {
      BS r = (BS) vRings.get(k);
      v.bsTemp.clearAll();
      v.bsTemp.or(r);
      v.bsTemp.and(bsAromatic);
      if (v.bsTemp.cardinality() == n56) {
        bs56.or(r);
        if (vAromatic56 != null)
          vAromatic56.addLast(r);
      }
    }
  }

  Object subsearch(SmilesSearch search, 
                            boolean firstAtomOnly, 
                            boolean isRingCheck) throws InvalidSmilesException {
    search.ringSets = ringSets;
    search.jmolAtoms = jmolAtoms;
    search.jmolAtomCount = jmolAtomCount;
    search.bsSelected = bsSelected;
    search.htNested = htNested;
    search.isSmilesFind = isSmilesFind;
    search.bsCheck = bsCheck;
    search.isSmarts = true;
    //search.measures = measures;
    search.bsAromatic = bsAromatic;
    search.bsAromatic5 = bsAromatic5;
    search.bsAromatic6 = bsAromatic6;
    search.ringData = ringData;
    search.ringCounts = ringCounts;
    search.ringConnections = ringConnections;
    if (firstAtomOnly) {
      search.bsRequired = null;
      search.firstMatchOnly = false;
      search.matchAllAtoms = false;
      //search.bsFound = bsFound;
      /*
       * this statement commented out 5/19/12 revision 17146; it was introduced in 
       * http://jmol.svn.sourceforge.net/viewvc/jmol/trunk/Jmol/src/org/jmol/smiles/SmilesSearch.java?r1=13475&r2=13476&pathrev=13476&
       * when the  nested business was set up and apparently this was needed for something with the 3D business. 
       * It is when nestedBond was set up as well, which makes sense, but this definitely is not a good idea. 
       * Could be that nesting 3D is not compatible with commenting this out. 
       * 
       */
    } else if (isRingCheck) {
      search.bsRequired = null;
      search.isSilent = true;
      search.isRingCheck = true;
      search.asVector = true;
      search.matchAllAtoms = false;
    } else {
      // processing || 
      search.haveSelected = haveSelected;
      search.bsRequired = bsRequired;
      search.firstMatchOnly = firstMatchOnly;
      search.matchAllAtoms = matchAllAtoms;
      search.getMaps = getMaps;
      search.asVector = asVector;
      search.vReturn = vReturn;
      search.bsReturn = bsReturn;
    }
    return search.search(firstAtomOnly);
  }
  
  /* ============================================================= */
  /*                             Search                            */
  /* ============================================================= */

  /** 
   * the start of the search. ret will be either a Vector or a BitSet
   * @param firstAtomOnly TODO
   * @return BitSet or Vector
   * @throws InvalidSmilesException 
   * 
   */
  Object search(boolean firstAtomOnly) throws InvalidSmilesException {

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

    ignoreStereochemistry = ((flags & Edge.FLAG_IGNORE_STEREOCHEMISTRY) != 0);
    noAromatic = ((flags & Edge.FLAG_NO_AROMATIC) != 0);
    aromaticDouble = ((flags & Edge.FLAG_AROMATIC_DOUBLE) != 0);
    
    if (Logger.debugging && !isSilent)
      Logger.debug("SmilesSearch processing " + pattern);

    if (vReturn == null && (asVector || getMaps))
      vReturn = new  List<Object>();
    if (bsSelected == null) {
      bsSelected = BS.newN(jmolAtomCount);
      bsSelected.setBits(0, jmolAtomCount);
    }
    selectedAtomCount = bsSelected.cardinality();
    if (subSearches != null) {
      for (int i = 0; i < subSearches.length; i++) {
        if (subSearches[i] == null)
          continue;
        subsearch(subSearches[i], false, false);
        if (firstMatchOnly) {
          if (vReturn == null ? bsReturn.nextSetBit(0) >= 0 : vReturn.size() > 0)
            break;
        }
      }
    } else if (atomCount > 0) {
      checkMatch(null, -1, -1, firstAtomOnly);
    }
    return (asVector || getMaps ? (Object) vReturn : bsReturn);
  }

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
   * @throws InvalidSmilesException 
   */
  private final boolean checkMatch(SmilesAtom patternAtom, int atomNum,
                                   int iAtom, boolean firstAtomOnly)
      throws InvalidSmilesException {

    //System.out.println("checkMatch " + patternAtom + " atomnum=" + atomNum + " iatom=" + iAtom);
    Node jmolAtom;
    Edge[] jmolBonds;
    if (patternAtom == null) {
      // first atom in pattern
      if (nestedBond == null) {
        // specifically for non-bioSmarts or not $(....) 
        clearBsFound(-1);
      } else {
        // clear out the return when there's a nested bio atom when $(...) is in a biomolecule?
        bsReturn.clearAll();
      }
    } else {
      // check for requested selection or not-selection

      if (bsFound.get(iAtom) || !bsSelected.get(iAtom))
        return true;

      jmolAtom = jmolAtoms[iAtom];

      if (!isRingCheck) {
        // check atoms 
        if (patternAtom.atomsOr != null) {
          for (int ii = 0; ii < patternAtom.nAtomsOr; ii++)
            if (!checkMatch(patternAtom.atomsOr[ii], atomNum, iAtom,
                firstAtomOnly))
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
      }
      
      // Check bonds

      jmolBonds = jmolAtom.getEdges();
      for (int i = patternAtom.getBondCount(); --i >= 0;) {
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

        switch (patternBond.order) {
        case SmilesBond.TYPE_BIO_SEQUENCE:
        case SmilesBond.TYPE_BIO_PAIR:
          if (!checkMatchBond(patternAtom, atom1, patternBond, iAtom,
              matchingAtom, null))
            return true;
          break;
        default:

          // regular SMILES/SMARTS check 
          // is to find the bond and test it against the pattern

          int k = 0;
          for (; k < jmolBonds.length; k++)
            if ((jmolBonds[k].getAtomIndex1() == matchingAtom || jmolBonds[k]
                .getAtomIndex2() == matchingAtom)
                && jmolBonds[k].isCovalent())
              break;
          if (k == jmolBonds.length)
            return true; // probably wasn't a covalent bond

          if (!checkMatchBond(patternAtom, atom1, patternBond, iAtom,
              matchingAtom, jmolBonds[k]))
            return true;
        }
      }

      // Note that we explicitly do a reference using
      // index because this could be a SEARCH [x,x] "sub" atom.

      patternAtoms[patternAtom.index].setMatchingAtom(iAtom);

      // The atom has passed both the atom and the bond test.
      // Add this atom to the growing list.

      if (Logger.debugging && !isSilent)
        Logger.debug("pattern atom " + atomNum + " " + patternAtom);
      bsFound.set(iAtom);

    }
    if (!continueMatch(atomNum, iAtom, firstAtomOnly))
      return false;
    if (iAtom >= 0)
      clearBsFound(iAtom);
    return true;
  }

  private BS bsCheck;

  private boolean continueMatch(int atomNum, int iAtom, boolean firstAtomOnly)
      throws InvalidSmilesException {

    Node jmolAtom;
    Edge[] jmolBonds;

    if (++atomNum < atomCount) {

      //System.out.println("atomno=" + atomNum + " bsFound = " + bsFound + " " + this);
      // so far, so good... not done yet... on to the next position...

      /*
      if (patternAtoms[atomNum].toString().indexOf("*") < 0) {
        for (int i = 0; i < atomNum; i++)System.out.println(i + " " + patternAtoms[i]);
      //System.out.println("");
      }
      */
      
      SmilesAtom newPatternAtom = patternAtoms[atomNum];
      
      //System.out.println("continueMatch atomNum=" + atomNum + ", iAtom=" + iAtom + " checking " + newPatternAtom);


      // For all the pattern bonds for this atom...
      // find the bond to atoms already assigned.
      // If it is not there, then it means this is a
      // new component.

      // the nestedBond may be set to previous search
      SmilesBond newPatternBond = (iAtom >= 0 ? newPatternAtom.getBondTo(null)
          : atomNum == 0 ? nestedBond : null);
      if (newPatternBond == null) {

        // Option 1: we are processing "."
        // run through all unmatched and unbonded-to-match
        // selected Jmol atoms to see if there is a match. 

        BS bs = BSUtil.copy(bsFound);
        if (newPatternAtom.notBondedIndex >= 0) {
          SmilesAtom pa = patternAtoms[newPatternAtom.notBondedIndex];
          Node a = jmolAtoms[pa.getMatchingAtom()];
          if (pa.isBioAtom) {
            // clear out adjacent residues
            int ii = ((BNode) a).getOffsetResidueAtom("0", 1);
            if (ii >= 0)
              bs.set(ii);
            ii = ((BNode) a).getOffsetResidueAtom("0", -1);
            if (ii >= 0)
              bs.set(ii);
          } else {
            // clear out all atoms connected to the last atom only
            jmolBonds = a.getEdges();
            for (int k = 0; k < jmolBonds.length; k++)
              bs.set(jmolBonds[k].getOtherAtomNode(a).getIndex());
          }
        }
        boolean skipGroup = (iAtom >= 0 && newPatternAtom.isBioAtom 
            && (newPatternAtom.atomName == null || newPatternAtom.residueChar != null));
        // TODO fix the *.*.*.*.* problem
        for (int j = bsSelected.nextSetBit(0); j >= 0; j = bsSelected
            .nextSetBit(j + 1)) {
          if (!bs.get(j)
              && !checkMatch(newPatternAtom, atomNum, j, firstAtomOnly))
            return false;
          if (skipGroup) {
            int j1 = ((BNode)jmolAtoms[j]).getOffsetResidueAtom(newPatternAtom.atomName,
                1);
            if (j1 >= 0)
              j = j1 - 1;
          }
        }
        bsFound = bs;
        return true;
      }

      // The new atom is connected to the old one in the pattern.
      // It doesn't so much matter WHICH connection we found -- 
      // there may be several -- but whatever we have, we must
      // have a connection in the real molecule between these two
      // particular atoms. So we just follow that connection. 

      jmolAtom = jmolAtoms[newPatternBond.getAtom1().getMatchingAtom()];

      // Option 2: The connecting bond is a bio sequence or
      // from ~GGC(T)C:ATTC...
      // For sequences, we go to the next GROUP, either via
      // the standard sequence or via basepair/cysteine pairing. 

      switch (newPatternBond.order) {
      case SmilesBond.TYPE_BIO_SEQUENCE:
        int nextGroupAtom = ((BNode)jmolAtom).getOffsetResidueAtom(
            newPatternAtom.atomName, 1);
        if (nextGroupAtom >= 0) {
          BS bs = BSUtil.copy(bsFound);
          ((BNode)jmolAtom).getGroupBits(bsFound);

          // working here

          if (!checkMatch(newPatternAtom, atomNum, nextGroupAtom, firstAtomOnly))
            return false;
          bsFound = bs;
        }
        return true;
      case SmilesBond.TYPE_BIO_PAIR:
        List<Integer> vLinks = new  List<Integer>();
        ((BNode)jmolAtom).getCrossLinkLeadAtomIndexes(vLinks);
        BS bs = BSUtil.copy(bsFound);
        ((BNode)jmolAtom).getGroupBits(bsFound);
        for (int j = 0; j < vLinks.size(); j++)
          if (!checkMatch(newPatternAtom, atomNum, vLinks.get(j).intValue(), firstAtomOnly))
            return false;
        bsFound = bs;
        return true;
      }

      // Option 3: Standard practice

      // We looked at the next pattern atom position and 
      // found at least one bond to it from a previous 
      // pattern atom. The only valid possibilities for this
      // pattern atom position, then, is a Jmol atom that is
      // bonded to that previous connection. So we only have
      // to check a handful of atoms. We do this so
      // that we don't have to check EVERY atom in the model.

      // Run through the bonds of that assigned atom
      // to see if any match this new connection.

      jmolBonds = jmolAtom.getEdges();
      if (jmolBonds != null)
        for (int j = 0; j < jmolBonds.length; j++)
          if (!checkMatch(newPatternAtom, atomNum, jmolAtom
              .getBondedAtomIndex(j), firstAtomOnly))
            return false;

      // Done checking this atom from any one of the places
      // higher in this stack. Clear the atom and keep going...

      clearBsFound(iAtom);
      return true;
    }

    // the pattern is complete

    // check stereochemistry

    if (!ignoreStereochemistry && !checkStereochemistry())
      return true;

    // set up the return BitSet and Vector, if requested

    // bioSequences only return the "lead" atom 
    // If the search is SMILES, we add the missing hydrogens
    
    
    BS bs = new BS();
    int nMatch = 0;
    for (int j = 0; j < atomCount; j++) {
      int i = patternAtoms[j].getMatchingAtom();
      if (!firstAtomOnly && top.haveSelected && !patternAtoms[j].selected)
        continue;
      nMatch++;
      bs.set(i);
      if (patternAtoms[j].isBioAtom && patternAtoms[j].atomName == null)
        ((BNode)jmolAtoms[i]).getGroupBits(bs);
      if (firstAtomOnly)
        break;
      if (!isSmarts && patternAtoms[j].missingHydrogenCount > 0)
        getHydrogens(jmolAtoms[i], bs);
    }
    if (bsRequired != null && !bsRequired.intersects(bs))
      return true;
    if (matchAllAtoms && bs.cardinality() != selectedAtomCount)
      return true;
    if (bsCheck != null) {
      if (firstAtomOnly) {
        bsCheck.clearAll();
        for (int j = 0; j < atomCount; j++) {
          //System.out.println("checking return for " + patternAtoms[j]);
          bsCheck.set(patternAtoms[j].getMatchingAtom());
        }
        if (bsCheck.cardinality() != atomCount)
          return true;
      } else {
        if (bs.cardinality() != atomCount)
          return true;
      }
    }
    bsReturn.or(bs);

    if (getMaps) {
      // every map is important always -- why??
      int[] map = new int[nMatch];
      for (int j = 0, nn = 0; j < atomCount; j++) {
        if (!firstAtomOnly && top.haveSelected && !patternAtoms[j].selected)
          continue;
        map[nn++] = patternAtoms[j].getMatchingAtom();
      }
      vReturn.addLast(map);
      return !firstMatchOnly;
    }

    if (asVector) {
      boolean isOK = true;
      for (int j = vReturn.size(); --j >= 0 && isOK;)
        isOK = !(((BS) vReturn.get(j)).equals(bs));
      if (!isOK)
        return true;
      vReturn.addLast(bs);
    }

    if (isRingCheck) {
      ringSets.append(" ");
      for (int k = atomNum * 3 + 2; --k > atomNum;)
        ringSets.append("-").appendI(
            patternAtoms[(k <= atomNum * 2 ? atomNum * 2 - k + 1 : k - 1)
                % atomNum].getMatchingAtom());
      ringSets.append("- ");
      return true;
    }

    // requested return is a BitSet or vector of BitSets

    // TRUE means "continue searching"

    if (firstMatchOnly)
      return false;

    // only continue if we have not found all the atoms already

    return (bs.cardinality() != selectedAtomCount);

  }

  private void clearBsFound(int iAtom) {
    //System.out.println("smiless " + iAtom + " " + bsFound + " " + bsFound.hashCode());
    
    if (iAtom < 0) {
      if (bsCheck == null) {bsFound.clearAll();}
      }
    else
      bsFound.clear(iAtom);    
  }

  private Node getHydrogens(Node atom, BS bsHydrogens) {
    Edge[] b = atom.getEdges();
    int k = -1;
    for (int i = 0; i < b.length; i++)
      if (jmolAtoms[atom.getBondedAtomIndex(i)].getElementNumber() == 1) {
        k = atom.getBondedAtomIndex(i);
        if (bsHydrogens == null)
          break;
        bsHydrogens.set(k);
      }
    return (k >= 0 ? jmolAtoms[k] : null);
  }

  private boolean checkPrimitiveAtom(SmilesAtom patternAtom, int iAtom)
      throws InvalidSmilesException {
    Node atom = jmolAtoms[iAtom];
    boolean foundAtom = patternAtom.not;

    //if (pattern.indexOf("*") < 0)
    //System.out.println("testing smilessearch");

    while (true) {

      int n;

      // _ <n> apply "recursive" SEARCH -- for example, [C&$(C[$(aaaO);$(aaC)])]"
      if (patternAtom.iNested > 0) {
        Object o = getNested(patternAtom.iNested);
        if (o instanceof SmilesSearch) {
          SmilesSearch search = (SmilesSearch) o;
          if (patternAtom.isBioAtom)
            search.nestedBond = patternAtom.getBondTo(null);
          o = subsearch(search, true, false);
          if (o == null)
            o = new BS();
          if (!patternAtom.isBioAtom)
            setNested(patternAtom.iNested, o);
        }
        foundAtom = (patternAtom.not != (((BS) o).get(iAtom)));
        break;
      }
      if (patternAtom.isBioAtom) {
        BNode a = (BNode) atom;

        // BIOSMARTS
        if (patternAtom.atomName != null
            && (patternAtom.isLeadAtom() ? !a.isLeadAtom()
                : !patternAtom.atomName.equals(a.getAtomName().toUpperCase())))
          break;
        if (patternAtom.notCrossLinked && a.getCrossLinkLeadAtomIndexes(null))
          break;
        if (patternAtom.residueName != null
            && !patternAtom.residueName
                .equals(a.getGroup3(false).toUpperCase()))
          break;
        if (patternAtom.residueChar != null) {
          if (patternAtom.isDna() && !a.isDna() || patternAtom.isRna()
              && !a.isRna() || patternAtom.isProtein() && !a.isProtein()
              || patternAtom.isNucleic() && !a.isNucleic())
            break;
          String s = a.getGroup1('\0').toUpperCase();
          boolean isOK = patternAtom.residueChar.equals(s);
          switch (patternAtom.residueChar.charAt(0)) {
          case 'N':
            isOK = patternAtom.isNucleic() ? a.isNucleic() : isOK;
            break;
          case 'R': // arginine purine
            isOK = patternAtom.isNucleic() ? a.isPurine() : isOK;
            break;
          case 'Y': // tyrosine or pyrimidine
            isOK = patternAtom.isNucleic() ? a.isPyrimidine() : isOK;
            break;
          }
          if (!isOK)
            break;
        }
        // # <n> or Symbol Check atomic number
        if (patternAtom.elementNumber >= 0
            && patternAtom.elementNumber != atom.getElementNumber())
          break;

      } else {
        if (patternAtom.atomName != null
            && (!patternAtom.atomName.equals(atom.getAtomName().toUpperCase())))
          break;
        // "=" <n>  Jmol index

        if (patternAtom.jmolIndex >= 0
            && atom.getIndex() != patternAtom.jmolIndex)
          break;

        if (patternAtom.atomType != null
            && !patternAtom.atomType.equals(atom.getAtomType()))
          break;

        // # <n> or Symbol Check atomic number
        if (patternAtom.elementNumber >= 0
            && patternAtom.elementNumber != atom.getElementNumber())
          break;

        // Check aromatic
        boolean isAromatic = patternAtom.isAromatic();
        if (!noAromatic && !patternAtom.aromaticAmbiguous
            && isAromatic != bsAromatic.get(iAtom))
          break;

        // <n> Check isotope
        if ((n = patternAtom.getAtomicMass()) != Integer.MIN_VALUE) {
          int isotope = atom.getIsotopeNumber();
          if (n >= 0 && n != isotope || n < 0 && isotope != 0 && -n != isotope) {
            // smiles indicates [13C] or [12C]
            // must match perfectly -- [12C] matches only explicit C-12, not
            // "unlabeled" C
            break;
          }
        }

        // +/- Check charge
        if ((n = patternAtom.getCharge()) != Integer.MIN_VALUE
            && n != atom.getFormalCharge())
          break;

        // H explicit H count
        //problem here is that you can have C[H]
        n = patternAtom.getCovalentHydrogenCount()
            + patternAtom.missingHydrogenCount;
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

        // r <n> ring of a given size
        if (ringData != null && patternAtom.ringSize >= -1) {
          if (patternAtom.ringSize <= 0) {
            if ((ringCounts[iAtom] == 0) != (patternAtom.ringSize == 0))
              break;
          } else {
            BS rd = ringData[patternAtom.ringSize == 500 ? 5
                : patternAtom.ringSize == 600 ? 6 : patternAtom.ringSize];
            if (rd == null || !rd.get(iAtom))
              break;
            if (!noAromatic)
              if (patternAtom.ringSize == 500) {
                if (!bsAromatic5.get(iAtom))
                  break;
              } else if (patternAtom.ringSize == 600) {
                if (!bsAromatic6.get(iAtom))
                  break;
              }
          }
        }
        // R <n> a certain number of rings
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
                                 int matchingAtom, Edge bond) {

    // apply SEARCH [ , , & ; ] logic

    if (patternBond.bondsOr != null) {
      for (int ii = 0; ii < patternBond.nBondsOr; ii++)
        if (checkMatchBond(patternAtom, atom1, patternBond.bondsOr[ii], iAtom,
            matchingAtom, bond))
          return true;
      return false;
    }

    if (patternBond.primitives == null) {
      if (!checkPrimitiveBond(patternBond, iAtom, matchingAtom, bond))
        return false;
    } else {
      for (int i = 0; i < patternBond.nPrimitives; i++)
        if (!checkPrimitiveBond(patternBond.primitives[i], iAtom, matchingAtom, bond))
          return false;
    }
    patternBond.matchingBond = bond;
    return true;
  }

  private boolean checkPrimitiveBond(SmilesBond patternBond, int iAtom1,
                                     int iAtom2, Edge bond) {
    boolean bondFound = false;
    
    switch (patternBond.order) {
    case SmilesBond.TYPE_BIO_SEQUENCE:
      return (patternBond.isNot != (bioAtoms[iAtom2].getOffsetResidueAtom("0", 1)
          == bioAtoms[iAtom1].getOffsetResidueAtom("0", 0)));
    case SmilesBond.TYPE_BIO_PAIR:
      return (patternBond.isNot != bioAtoms[iAtom1].isCrossLinked(bioAtoms[iAtom2]));
    }
    
    boolean isAromatic1 = (!noAromatic && bsAromatic.get(iAtom1));
    boolean isAromatic2 = (!noAromatic && bsAromatic.get(iAtom2));
    //if (pattern.indexOf("*") < 0)
      //System.out.println("testing smilessearch");
    int order = bond.getCovalentOrder();
    if (isAromatic1 && isAromatic2) {
      switch (patternBond.order) {
      case SmilesBond.TYPE_AROMATIC: // :
      case SmilesBond.TYPE_RING:
        bondFound = isRingBond(ringSets, iAtom1, iAtom2);
        break;
      case SmilesBond.TYPE_SINGLE:
        // for SMARTS, single bond in aromatic means TO ANOTHER RING;
        // for SMILES, we don't care
        bondFound = !isSmarts || !isRingBond(ringSets, iAtom1, iAtom2);
        break;
      case SmilesBond.TYPE_DOUBLE:
        // note: Freiburg considers TYPE_DOUBLE to be NOT aromatic
        // changed for Jmol 12.2.RC8
        // but this is ambiguous at http://www.daylight.com/dayhtml/doc/theory/theory.smarts.html
        // see, for example: http://opentox.informatik.uni-freiburg.de/depict?data=[H]C%3D1C%28[H]%29%3DC%28[H]%29C%28%3DC%28C%3D1%28[H]%29%29C%28F%29%28F%29F%29S[H]&smarts=[%236]=[%236]
        // however, if it is not SMARTS, then we consider this fine -- it does
        // not matter what the order is for double/single bonds around the ring
        // 
        // starting with Jmol 12.3.24, we allow the flag AROMATICDOUBLE to allow a
        // distinction between single and double, as for example is necessary to distinguish
        // between n=c-NH2 and n-c-NH2 (necessary for MMFF94 atom typing
        //
        bondFound = !isSmarts || aromaticDouble &&
          (order == Edge.BOND_COVALENT_DOUBLE || order == Edge.BOND_AROMATIC_DOUBLE);
        break;
      case SmilesBond.TYPE_ATROPISOMER_1:
      case SmilesBond.TYPE_ATROPISOMER_2:
      case SmilesBond.TYPE_ANY:
      case SmilesBond.TYPE_UNKNOWN:
        bondFound = true;
        break;
      }
    } else {
      switch (patternBond.order) {
      case SmilesBond.TYPE_ANY:
      case SmilesBond.TYPE_UNKNOWN:
        bondFound = true;
        break;
      case SmilesBond.TYPE_SINGLE:
      case SmilesBond.TYPE_DIRECTIONAL_1:
      case SmilesBond.TYPE_DIRECTIONAL_2:
        // STEREO_NEAR and _FAR are stand-ins for find()
        bondFound = (order == Edge.BOND_COVALENT_SINGLE
            || order == Edge.BOND_STEREO_FAR
            || order == Edge.BOND_STEREO_NEAR);
        break;
      case SmilesBond.TYPE_ATROPISOMER_1:
        bondFound = (order == (isSmilesFind ? Edge.BOND_PARTIAL01 : Edge.BOND_COVALENT_SINGLE));
        break;
      case SmilesBond.TYPE_ATROPISOMER_2:
        bondFound = (order == (isSmilesFind ? Edge.BOND_PARTIAL23 : Edge.BOND_COVALENT_SINGLE));
        break;
      case SmilesBond.TYPE_DOUBLE:
        bondFound = (order == Edge.BOND_COVALENT_DOUBLE);
        break;
      case SmilesBond.TYPE_TRIPLE:
        bondFound = (order == Edge.BOND_COVALENT_TRIPLE);
        break;
      case SmilesBond.TYPE_RING:
        bondFound = isRingBond(ringSets, iAtom1, iAtom2);
        break;
      }
    }
    return bondFound != patternBond.isNot;
  }

  static boolean isRingBond(SB ringSets, int i, int j) {
    return (ringSets != null && ringSets.indexOf("-" + i + "-" + j + "-") >= 0);
  }
  
  /* ============================================================= */
  /*                          Stereochemistry                      */
  /* ============================================================= */

  private boolean checkStereochemistry() {

    // first, @ stereochemistry

    for (int i = 0; i < measures.size(); i++)
      if (!measures.get(i).check())
        return false;
    if (haveAtomStereochemistry) {

      if (Logger.debugging)
        Logger.debug("checking stereochemistry...");

      Node atom1 = null, atom2 = null, atom3 = null, atom4 = null, atom5 = null, atom6 = null;
      SmilesAtom sAtom1 = null, sAtom2 = null;
      Node[] jn;
      //for debugging, first try SET DEBUG
      //for (int i = 0; i < atomCount; i++) {
      //  SmilesAtom sAtom = patternAtoms[i];
      //  System.out.print(sAtom + "=");
      //}
      //System.out.println("");
      //for (int i = 0; i < atomCount; i++) {
      //  SmilesAtom sAtom = patternAtoms[i];
      //  JmolSmilesNode atom0 = jmolAtoms[sAtom.getMatchingAtom()];
      //  System.out.print(atom0.getIndex() + "-");
      //}
      //System.out.println("");
      for (int i = 0; i < atomCount; i++) {
        SmilesAtom sAtom = patternAtoms[i];
        Node atom0 = jmolAtoms[sAtom.getMatchingAtom()];
        int nH = sAtom.missingHydrogenCount;
        if (nH < 0)
          nH = 0;
        int chiralClass = sAtom.getChiralClass();
        if (chiralClass == Integer.MIN_VALUE)
          continue;
        int order = sAtom.getChiralOrder();
        if (isSmilesFind && (atom0.getAtomSite() >> 8) != chiralClass)
          return false;
        atom4 = null;
        if (Logger.debugging)
          Logger.debug("...type " + chiralClass + " for pattern atom " + sAtom + " " + atom0);
        switch (chiralClass) {
        //        case SmilesAtom.STEREOCHEMISTRY_DOUBLE_BOND:
        case SmilesAtom.STEREOCHEMISTRY_ALLENE:
          boolean isAllene = true;//(chiralClass == SmilesAtom.STEREOCHEMISTRY_ALLENE);
          if (isAllene) {
            sAtom1 = sAtom.getBond(0).getOtherAtom(sAtom);
            sAtom2 = sAtom.getBond(1).getOtherAtom(sAtom);
            if (sAtom1 == null || sAtom2 == null)
              continue; // "OK - stereochemistry is desgnated for something like C=C=O
            // cumulenes
            SmilesAtom sAtom1a = sAtom;
            SmilesAtom sAtom2a = sAtom;
            while (sAtom1.getBondCount() == 2
                && sAtom2.getBondCount() == 2
                && sAtom1.getValence() == 4 && sAtom2.getValence() == 4) {
              SmilesBond b = sAtom1.getBondNotTo(sAtom1a, true);
              sAtom1a = sAtom1;
              sAtom1 = b.getOtherAtom(sAtom1);
              b = sAtom2.getBondNotTo(sAtom2a, true);
              sAtom2a = sAtom2;
              sAtom2 = b.getOtherAtom(sAtom2);
            }
            sAtom = sAtom1;
          }
          jn = new Node[6];
          jn[4] = new SmilesAtom().setIndex(604);
          int nBonds = sAtom.getBondCount();
          for (int k = 0; k < nBonds; k++) {
            sAtom1 = sAtom.bonds[k].getOtherAtom(sAtom);
            if (sAtom.bonds[k].matchingBond.getCovalentOrder() == 2) {
              if (sAtom2 == null)
                sAtom2 = sAtom1;
            } else if (jn[0] == null) {
              jn[0] = getJmolAtom(sAtom1.getMatchingAtom());
            } else {
              jn[1] = getJmolAtom(sAtom1.getMatchingAtom());
            }
          }
          if (sAtom2 == null)
            continue;
          nBonds = sAtom2.getBondCount();
          if (nBonds < 2 || nBonds > 3)
            continue; // [C@]=O always matches
          for (int k = 0; k < nBonds; k++) {
            sAtom1 = sAtom2.bonds[k].getOtherAtom(sAtom2);
            if (sAtom2.bonds[k].matchingBond.getCovalentOrder() == 2) {
            } else if (jn[2] == null) {
              jn[2] = getJmolAtom(sAtom1.getMatchingAtom());
            } else {
              jn[3] = getJmolAtom(sAtom1.getMatchingAtom());
            }
          }

          if (isSmilesFind) {
            if (jn[1] == null)
              getX(sAtom, jn, 1, false, isAllene);
            if (jn[3] == null)
              getX(sAtom2, jn, 3, false, false);
            if (!setSmilesCoordinates(atom0, sAtom, sAtom2, jn))
              return false;
          }
          if (jn[1] == null)
            getX(sAtom, jn, 1, true, false);
          if (jn[3] == null)
            getX(sAtom2, jn, 3, true, false);
          if (!checkStereochemistryAll(sAtom.not, atom0, chiralClass, order,
              jn[0], jn[1], jn[2], jn[3], null, null, v))
            return false;
          continue;
        case SmilesAtom.STEREOCHEMISTRY_TETRAHEDRAL:
        case SmilesAtom.STEREOCHEMISTRY_SQUARE_PLANAR:
        case SmilesAtom.STEREOCHEMISTRY_TRIGONAL_BIPYRAMIDAL:
        case SmilesAtom.STEREOCHEMISTRY_OCTAHEDRAL:
          atom1 = getJmolAtom(sAtom.getMatchingBondedAtom(0));
          switch (nH) {
          case 0:
            atom2 = getJmolAtom(sAtom.getMatchingBondedAtom(1));
            break;
          case 1:
            atom2 = getHydrogens(getJmolAtom(sAtom.getMatchingAtom()), null);
            if (sAtom.isFirst) {
              Node a = atom2;
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

          if (isSmilesFind
              && !setSmilesCoordinates(atom0, sAtom, sAtom2, new Node[] {
                  atom1, atom2, atom3, atom4, atom5, atom6 }))
            return false;
            //System.out.println(order + ":" + sAtom + "/" + atom0 + ":" + atom1 + "," + atom2 + "," + atom3 + "," + atom4);
          if (!checkStereochemistryAll(sAtom.not, atom0, chiralClass, order,
              atom1, atom2, atom3, atom4, atom5, atom6, v))
            return false;
            //System.out.println("OK");
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
        int bondType = 0;
        SmilesBond b;
        int nBonds = sAtom1.getBondCount();
        boolean isAtropisomer = false;
        for (int j = 0; j < nBonds; j++) {
          b = sAtom1.getBond(j);
          boolean isAtom2 = (b.getAtom2() == sAtom1);
          int type = b.order;
          switch (type) {
          case SmilesBond.TYPE_ATROPISOMER_1:
          case SmilesBond.TYPE_ATROPISOMER_2:
          case SmilesBond.TYPE_DOUBLE:
            if (isAtom2)
              continue;
            sAtom2 = b.getAtom2();
            bondType = type;
            isAtropisomer = (type != SmilesBond.TYPE_DOUBLE);
            if (isAtropisomer)
              dir1 = (b.isNot ? -1 : 1);
            break;
          case SmilesBond.TYPE_DIRECTIONAL_1:
          case SmilesBond.TYPE_DIRECTIONAL_2:
            sAtomDirected1 = (isAtom2 ? b.getAtom1() : b.getAtom2());
            dir1 = (isAtom2 != (type == SmilesBond.TYPE_DIRECTIONAL_1) ? 1 : -1);
            break;
          }
        }
        if (isAtropisomer) {
          //System.out.println(sAtom1 + " " + sAtom2);
          b = sAtom1.getBondNotTo(sAtom2, false);
          if (b == null)
            return false;
          sAtomDirected1 = b.getOtherAtom(sAtom1);
          b = sAtom2.getBondNotTo(sAtom1, false);
          if (b == null)
            return false;
          sAtomDirected2 = b.getOtherAtom(sAtom2);
        } else {
          if (sAtom2 == null || dir1 == 0)
            continue;
          nBonds = sAtom2.getBondCount();
          for (int j = 0; j < nBonds && dir2 == 0; j++) {
            b = sAtom2.getBond(j);
            boolean isAtom2 = (b.getAtom2() == sAtom2);
            int type = b.order;
            switch (type) {
            case SmilesBond.TYPE_DIRECTIONAL_1:
            case SmilesBond.TYPE_DIRECTIONAL_2:
              sAtomDirected2 = (isAtom2 ? b.getAtom1() : b.getAtom2());
              dir2 = (isAtom2 != (type == SmilesBond.TYPE_DIRECTIONAL_1) ? 1
                  : -1);
              break;
            }
          }
          if (dir2 == 0)
            continue;
        }
        if (isSmilesFind)
          setSmilesBondCoordinates(sAtom1, sAtom2, bondType);
        Node dbAtom1 = getJmolAtom(sAtom1.getMatchingAtom());
        Node dbAtom2 = getJmolAtom(sAtom2.getMatchingAtom());
        Node dbAtom1a = getJmolAtom(sAtomDirected1.getMatchingAtom());
        Node dbAtom2a = getJmolAtom(sAtomDirected2.getMatchingAtom());
        if (dbAtom1a == null || dbAtom2a == null)
          return false;
        SmilesMeasure.setTorsionData((P3) dbAtom1a, (P3) dbAtom1,
            (P3) dbAtom2, (P3) dbAtom2a, v, isAtropisomer);
        if (isAtropisomer) {
          // Ranges here involve
          // acos(0.05) and acos(0.95) to exclude 
          // conformations very close to 90o and 0o
          dir2 = (bondType == SmilesBond.TYPE_ATROPISOMER_1 ? 1 : -1);
          float f = v.vTemp1.dot(v.vTemp2);
          if (f < 0.05f || f > 0.95f
              || v.vNorm1.dot(v.vNorm2) * dir1 * dir2 > 0) // sign of dihedral < or > here
            return false;
        } else {
          // for \C=C\, (dir1*dir2 == -1), dot product should be negative
          // because the bonds are oppositely directed
          // for \C=C/, (dir1*dir2 == 1), dot product should be positive
          // because the bonds are only about 60 degrees apart
          if (v.vTemp1.dot(v.vTemp2) * dir1 * dir2 < 0)
            return false;
        }
      }
    }
    return true;

  }

  private void getX(SmilesAtom sAtom, Node[] jn, int pt,
                    boolean haveCoordinates, boolean needHSwitch) {
    Node atom = getJmolAtom(sAtom.getMatchingAtom());
    boolean doSwitch = sAtom.isFirst || pt == 3;
    if (haveCoordinates) {
      if (isSmarts) {
        Edge[] b = atom.getEdges();
        for (int i = 0; i < b.length; i++) {
          if (b[i].getCovalentOrder() == 2)
            continue;
          Node a = jmolAtoms[atom.getBondedAtomIndex(i)];
          if (a == jn[pt - 1])
            continue;
          jn[pt] = a;
          break;
        }
      }
      if (jn[pt] == null) {
        // add a dummy point for stereochemical reference
        // imines and diazines only
        V3 v = new V3();
        int n = 0;
        for (int i = 0; i < 4; i++) {
          if (jn[i] == null)
            continue;
          n++;
          v.sub((P3) jn[i]);
        }
        if (v.length() == 0) {
          v.setT(((P3)jn[4]));
          doSwitch = false;
        } else {
          v.scaleAdd2(n + 1,(P3) getJmolAtom(sAtom.getMatchingAtom()), v);
          doSwitch = isSmilesFind || doSwitch ;
        }
        jn[pt] = new SmilesAtom().setIndex(-1);
        ((P3) jn[pt]).setT(v);
      }
    }
    if (jn[pt] == null) {
      jn[pt] = getHydrogens(atom, null);
      if (needHSwitch)
        doSwitch = true;
    }
    if (jn[pt] != null && doSwitch) {
      // a missing substituent on the SECOND atom 
      // should be placed in position 2, not 3
      // also check for the VERY first atom in a set
      // attached H is first in that case
      // so we have to switch it, since we have
      // assigned already the first atom to be
      // the first pattern atom
      Node a = jn[pt];
      jn[pt] = jn[pt - 1];
      jn[pt - 1] = a;
    }
  }
  
  static boolean checkStereochemistryAll(boolean isNot, Node atom0, int chiralClass, int order, 
                                Node atom1, Node atom2, Node atom3, Node atom4, Node atom5, Node atom6, VTemp v) {
    
    switch (chiralClass) {
    default:
    case SmilesAtom.STEREOCHEMISTRY_ALLENE:
    case SmilesAtom.STEREOCHEMISTRY_TETRAHEDRAL:
      return (isNot == (getHandedness(atom2, atom3, atom4, atom1, v) != order));
    case SmilesAtom.STEREOCHEMISTRY_TRIGONAL_BIPYRAMIDAL:
      // check for axial-axial'
      return (isNot == (!isDiaxial(atom0, atom0, atom5, atom1, v, -0.95f)
          || getHandedness(atom2, atom3, atom4, atom1, v) != order));
    case SmilesAtom.STEREOCHEMISTRY_OCTAHEDRAL:
      if (isNot != (!isDiaxial(atom0, atom0, atom6, atom1, v, -0.95f)))
        return false;
      // check for CW or CCW set
      getPlaneNormals(atom2, atom3, atom4, atom5, v);
      if (isNot != (v.vNorm1.dot(v.vNorm2) < 0 
          || v.vNorm2.dot(v.vNorm3) < 0))
        return false;
      // now check rotation in relation to the first atom
      v.vNorm2.sub2((P3) atom0, (P3) atom1);
      return (isNot == ((v.vNorm1.dot(v.vNorm2) < 0 ? 2 : 1) == order));
    //case SmilesAtom.STEREOCHEMISTRY_DOUBLE_BOND:
      //System.out.println("draw p1 @{point" + Point3f.new3((Point3f)atom1)+"} color red");
      //System.out.println("draw p2 @{point" + Point3f.new3((Point3f)atom2)+"} color yellow");
      //System.out.println("draw p3 @{point" + Point3f.new3((Point3f)atom3)+"} color green");
      //System.out.println("draw p4 @{point" + Point3f.new3((Point3f)atom4)+"} color blue");

      //getPlaneNormals(atom1, atom2, atom3, atom4, v);
      //System.out.println(order + " "+ atom1.getAtomName() + "-" + atom2.getAtomName() + "-"  + atom3.getAtomName() + "-" + atom4.getAtomName());
      //return (isNot == ((v.vNorm1.dot(v.vNorm2) < 0 ? 2 : 1) == order));
    case SmilesAtom.STEREOCHEMISTRY_SQUARE_PLANAR:
      getPlaneNormals(atom1, atom2, atom3, atom4, v);
      // vNorm1 vNorm2 vNorm3 are right-hand normals for the given
      // triangles
      // 1-2-3, 2-3-4, 3-4-1
      // sp1 up up up U-shaped
      // sp2 up up DOWN 4-shaped
      // sp3 up DOWN DOWN Z-shaped
      return (v.vNorm1.dot(v.vNorm2) < 0 ? isNot == (order != 3)
          : v.vNorm2.dot(v.vNorm3) < 0 ? isNot == (order != 2)
          : isNot == (order != 1));
    }
  }
  
  private Node getJmolAtom(int i) {
    return (i < 0 || i >= jmolAtoms.length ? null : jmolAtoms[i]);
  }

  private void setSmilesBondCoordinates(SmilesAtom sAtom1, SmilesAtom sAtom2,
                                        int bondType) {
    Node dbAtom1 = jmolAtoms[sAtom1.getMatchingAtom()];
    Node dbAtom2 = jmolAtoms[sAtom2.getMatchingAtom()];
    // Note that the directionality of the bond depends upon whether
    // the alkene C is the first or the second atom in the bond. 
    // if it is the first -- C(/X)= or C/1= -- then the X is UP
    // but if it is the second: -- X/C= or X/1... C1= -- then the X is DOWN
    //
    //                         C C       C     C
    //                        / /         \   /
    //      C(/C)=C/C  ==    C=C     ==    C=C     ==   C\C=C/C   
    //
    // because what we are doing is translating the / or \ vertically
    // to match the atoms it is connected to. Same with rings:
    //
    //                       CCC C     CCC     C
    //                        / /         \   /
    //  C1CC.C/1=C/C  ==     C=C    ==     C=C     ==   CCC\C=C/C   
    //
    // If the branch ALSO has a double bond,
    // then for THAT double bond we will have it the normal way:
    //
    //                              Br
    //                             /    BR
    //                          C=C      \
    //                         / C        C=C     C
    //                        / /            \   /
    //  C(/C=C/Br)=C/C  ==   C=C     ==       C=C     ==  Br\C=C\C=C/C   
    // 
    // interesting case for ring connections:
    //
    // Br/C=C\1OCCC.C/1=C/C=C/CCS/C=C\2CCCC.NN/2
    //
    // Note that that directionality of the matching ring bonds must be OPPOSITE.
    // Better is to not show it both places:
    //
    // Br/C=C\1OCCC.C/1=C/C=C/CCS/C=C\2CCCC.NN/2
    //
    dbAtom1.set(-1, 0, 0);
    dbAtom2.set(1, 0, 0);
    if (bondType == SmilesBond.TYPE_DOUBLE) {
      int nBonds = 0;
      int dir1 = 0;
      Edge[] bonds = dbAtom1.getEdges();
      for (int k = bonds.length; --k >= 0;) {
        Edge bond = bonds[k];
        Node atom = bond.getOtherAtomNode(dbAtom1);
        if (atom == dbAtom2)
          continue;
        atom.set(-1, (nBonds++ == 0) ? -1 : 1, 0);
        int mode = (bond.getAtomIndex2() == dbAtom1.getIndex() ? nBonds
            : -nBonds);
        switch (bond.order) {
        case Edge.BOND_STEREO_NEAR:
          dir1 = mode;
          break;
        case Edge.BOND_STEREO_FAR:
          dir1 = -mode;
        }
      }
      int dir2 = 0;
      nBonds = 0;
      Node[] atoms = new Node[2];
      bonds = dbAtom2.getEdges();
      for (int k = bonds.length; --k >= 0;) {
        Edge bond = bonds[k];
        Node atom = bond.getOtherAtomNode(dbAtom2);
        if (atom == dbAtom1)
          continue;
        atoms[nBonds] = atom;
        atom.set(1, (nBonds++ == 0) ? 1 : -1, 0);
        int mode = (bond.getAtomIndex2() == dbAtom2.getIndex() ? nBonds
            : -nBonds);
        switch (bond.order) {
        case Edge.BOND_STEREO_NEAR:
          dir2 = mode;
          break;
        case Edge.BOND_STEREO_FAR:
          dir2 = -mode;
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
      if ((dir1 * dir2 > 0) == (Math.abs(dir1) % 2 == Math.abs(dir2) % 2)) {
        float y = ((P3) atoms[0]).y;
        ((P3) atoms[0]).y = ((P3) atoms[1]).y;
        ((P3) atoms[1]).y = y;
      }
    } else {
      // just set ALL the attached bonds to the given dihedral setting
      Edge[] bonds = dbAtom1.getEdges();
      int dir = 0;
      for (int k = bonds.length; --k >= 0;) {
        Edge bond = bonds[k];
        if (bond.getOtherAtomNode(dbAtom1) == dbAtom2) {
          dir = (bond.order == Edge.BOND_PARTIAL01 ? 1 : -1);
          break;
        }
      }
      for (int k = bonds.length; --k >= 0;) {
        Edge bond = bonds[k];
        Node atom = bond.getOtherAtomNode(dbAtom1);
        if (atom != dbAtom2)
          atom.set(-1, 1, 0);
      }    
      bonds = dbAtom2.getEdges();
      for (int k = bonds.length; --k >= 0;) {
        Edge bond = bonds[k];
        Node atom = bond.getOtherAtomNode(dbAtom2);
        if (atom != dbAtom1)
          atom.set(1, 1, -dir/2.0f);
      }
    }
  }

  private boolean setSmilesCoordinates(Node atom, SmilesAtom sAtom, SmilesAtom sAtom2, Node[] cAtoms) {

    // When testing equality of two SMILES strings in terms of stereochemistry,
    // we need to set the atom positions based on the ORIGINAL SMILES order,
    // which, except for the H atom, will be the same as the "matchedAtom"
    // index.
    // all the necessary information is passed via the atomSite field of Atom

    // atomSite is used by smilesMatch.find to encode chiralClass and chiralOrder 
    int atomSite = atom.getAtomSite();
    if (atomSite == Integer.MIN_VALUE)
      return false;
    int chiralClass = atomSite >> 8;
    int chiralOrder = atomSite & 0xFF;
    Node a2 = (chiralClass == 2 || chiralClass == 3 ?
      a2 = jmolAtoms[sAtom2.getMatchingAtom()] : null);
        
    // set the chirality center at the origin
    atom.set(0, 0, 0);
    atom = jmolAtoms[sAtom.getMatchingAtom()];
    atom.set(0, 0, 0);

    int[] map = getMappedAtoms(atom, a2, cAtoms);
    switch (chiralClass) {
    case SmilesAtom.STEREOCHEMISTRY_ALLENE:
    case SmilesAtom.STEREOCHEMISTRY_TETRAHEDRAL:
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
/*      
    case SmilesAtom.STEREOCHEMISTRY_DOUBLE_BOND:
      switch (chiralOrder) {
      case 1: // U-shaped 0 3 2 1
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
      }
      break;
*/      
    case SmilesAtom.STEREOCHEMISTRY_SQUARE_PLANAR:
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
    case SmilesAtom.STEREOCHEMISTRY_TRIGONAL_BIPYRAMIDAL:
    case SmilesAtom.STEREOCHEMISTRY_OCTAHEDRAL:
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

  int[] getMappedAtoms(Node atom, Node a2, Node[] cAtoms) {
   
    // Here is the secret:
    // Sort the atoms by the origintal order of bonds
    // in the SMILES string that generated the
    // atom set.
    int[] map = new int[cAtoms[4] == null ? 4 : cAtoms[5] == null ? 5 : 6];
    for (int i = 0; i < map.length; i++)
      map[i] = (cAtoms[i] == null ? 104 + i * 100: cAtoms[i].getIndex());
    int k;
    Edge[] bonds = atom.getEdges();
    Edge[] b2 = (a2 == null ? null : a2.getEdges());
    for (int i = 0; i < map.length; i++) {
      for (k = 0; k < bonds.length; k++)
        if (bonds[k].getOtherAtomNode(atom) == cAtoms[i])
          break;
      if (k < bonds.length) {
        map[i] = (k * 10 + 100) + i;
      } else if (a2 != null) {
        for (k = 0; k < b2.length; k++)
          if (b2[k].getOtherAtomNode(a2) == cAtoms[i])
            break;
        if (k < b2.length)
          map[i] = (k * 10 + 300) + i;
      }
    }
    Arrays.sort(map);
    for (int i = 0; i < map.length; i++) {
      map[i] = map[i] % 10;
      //System.out.println("i=" + i + "; map[i]=" + map[i] + " a=" +
      // cAtoms[map[i]].getIndex());
    }
    return map;
  }
  VTemp v = new VTemp();
  
  static boolean isDiaxial(Node atomA, Node atomB, Node atom1, Node atom2, VTemp v, float f) {
    v.vA.sub2((P3) atomA, (P3) atom1);
    v.vB.sub2((P3) atomB, (P3) atom2);
    v.vA.normalize();
    v.vB.normalize();
    // -0.95f about 172 degrees
    return (v.vA.dot(v.vB) < f);
  }

  /**
   * compares the 
   * @param a
   * @param b
   * @param c
   * @param pt
   * @param v
   * @return   1 for "@", 2 for "@@"  
   */
  private static int getHandedness(Node a, Node b, Node c, Node pt, VTemp v) {
    float d = SmilesAromatic.getNormalThroughPoints(a, b, c, v.vTemp, v.vA, v.vB);
    //int atat = (distanceToPlane(v.vTemp, d, (Point3f) pt) > 0 ? 1 : 2);
    //System.out.println("draw p1 @{point" + Point3f.new3((Point3f)a) +"} color red");
    //System.out.println("draw p2 @{point" + Point3f.new3((Point3f)b)+"} color green");
    //System.out.println("draw p3 @{point" + Point3f.new3((Point3f)c)+"} color blue");
    //System.out.println("draw p @{point" + Point3f.new3((Point3f)a) +"} @{point" + Point3f.new3((Point3f)b)+"} @{point" + Point3f.new3((Point3f)c)+"}");
    //System.out.println("draw v vector @{point" + Point3f.new3((Point3f)pt) + "} @{point" + v.vTemp+"} \""+ (atat==2 ? "@@" : "@")+"\" color " + (atat == 2 ? "white" : "yellow"));
    return (distanceToPlane(v.vTemp, d, (P3) pt) > 0 ? 1 : 2);
  }

  private static void getPlaneNormals(Node atom1, Node atom2, Node atom3, Node atom4, VTemp v) {
    SmilesAromatic.getNormalThroughPoints(atom1, atom2, atom3, v.vNorm1, v.vTemp1,
        v.vTemp2);
    SmilesAromatic.getNormalThroughPoints(atom2, atom3, atom4, v.vNorm2, v.vTemp1,
        v.vTemp2);
    SmilesAromatic.getNormalThroughPoints(atom3, atom4, atom1, v.vNorm3, v.vTemp1,
        v.vTemp2);
  }

  static float distanceToPlane(V3 norm, float w, P3 pt) {
    return (norm == null ? Float.NaN 
        : (norm.x * pt.x + norm.y * pt.y + norm.z * pt.z + w)
        / (float) Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z
            * norm.z));
  }

/*
  static String getDoubleBondStereoFlag(JmolSmilesNode[] atoms, VTemp v) {
    JmolSmilesNode atom1 = atoms[0];
    JmolSmilesNode atom2 = atoms[1];
    JmolSmilesNode atom3 = atoms[2];
    JmolSmilesNode atom4 = atoms[3];
    getPlaneNormals(atom1, atom2, atom3, atom4, v);
    return (v.vNorm1.dot(v.vNorm2) < 0 ? "@" : "@@");
  }
*/
  
  void createTopoMap(BS bsAromatic) {
    if (bsAromatic == null)
      bsAromatic = new BS();
    int nAtomsMissing = getMissingHydrogenCount();
    SmilesAtom[] atoms = new SmilesAtom[atomCount + nAtomsMissing];
    jmolAtoms = atoms;
    int ptAtom = 0;
    BS bsFixH = new BS();
    for (int i = 0; i < atomCount; i++) {
      SmilesAtom sAtom = patternAtoms[i];
      int cclass = sAtom.getChiralClass();
      int n = sAtom.missingHydrogenCount;
      if (n < 0)
        n = 0;
      // create a Jmol atom for this pattern atom
      // we co-opt atom.matchingAtom here
      // because this search will never actually be run
      SmilesAtom atom = atoms[ptAtom] = new SmilesAtom().setAll(0, ptAtom,
          cclass == Integer.MIN_VALUE ? cclass : (cclass << 8)
              + sAtom.getChiralOrder(), sAtom.elementNumber, sAtom.getCharge());
      atom.atomName = sAtom.atomName;
      atom.residueName = sAtom.residueName;
      atom.residueChar = sAtom.residueChar;
      atom.isBioAtom = sAtom.isBioAtom;
      atom.isLeadAtom = sAtom.isLeadAtom;
      atom.setAtomicMass(sAtom.getAtomicMass());
      //System.out.println(atom);
      // we pass on the aromatic flag because
      // we don't want SmilesSearch to calculate
      // that for us
      if (sAtom.isAromatic())
        bsAromatic.set(ptAtom);
      // set up the bonds array and fill with H atoms
      // when there is only 1 H and the atom is NOT FIRST, then it will
      // be important to designate the bonds in order -- with the
      // H SECOND not first
      // this is still not satisfactory for allenes or the second atom of 
      // imines and possibly double bonds. We handle that later.

      if (!sAtom.isFirst && n == 1 && cclass > 0)
        bsFixH.set(ptAtom);

      sAtom.setMatchingAtom(ptAtom++);
      SmilesBond[] bonds = new SmilesBond[sAtom.getBondCount() + n];
      atom.setBonds(bonds);
      while (--n >= 0) {
        SmilesAtom atomH = atoms[ptAtom] = new SmilesAtom().setAll(0, ptAtom, 0,
            1, 0);
        //System.out.println(atomH);
        ptAtom++;
        atomH.setBonds(new SmilesBond[1]);
        SmilesBond b = new SmilesBond(atom, atomH,
            Edge.BOND_COVALENT_SINGLE, false);
        Logger.info("" + b);
      }
    }

    // set up bonds
    for (int i = 0; i < atomCount; i++) {
      SmilesAtom sAtom = patternAtoms[i];
      int i1 = sAtom.getMatchingAtom();
      SmilesAtom atom1 = atoms[i1];
      int n = sAtom.getBondCount();
      for (int j = 0; j < n; j++) {
        SmilesBond sBond = sAtom.getBond(j);
        boolean firstAtom = (sBond.getAtom1() == sAtom);
        //SmilesBond b;
        if (firstAtom) {
          int order = 1;
          switch (sBond.order) {
          // these first two are for cis/trans alkene
          // stereochemistry; we co-opt stereo near/far here
          case SmilesBond.TYPE_ATROPISOMER_1:
            order = Edge.BOND_PARTIAL01;
            break;
          case SmilesBond.TYPE_ATROPISOMER_2:
            order = Edge.BOND_PARTIAL23;
            break;
          case SmilesBond.TYPE_DIRECTIONAL_1:
            order = Edge.BOND_STEREO_NEAR;
            break;
          case SmilesBond.TYPE_DIRECTIONAL_2:
            order = Edge.BOND_STEREO_FAR;
            break;
          case SmilesBond.TYPE_BIO_PAIR:
          case SmilesBond.TYPE_BIO_SEQUENCE:
            order = sBond.order;
            break;
          case SmilesBond.TYPE_SINGLE:
            order = Edge.BOND_COVALENT_SINGLE;
            break;
          case SmilesBond.TYPE_AROMATIC:
            order = Edge.BOND_AROMATIC_DOUBLE;
            break;
          case SmilesBond.TYPE_DOUBLE:
            order = Edge.BOND_COVALENT_DOUBLE;
            break;
          case SmilesBond.TYPE_TRIPLE:
            order = Edge.BOND_COVALENT_TRIPLE;
            break;
          }
          SmilesAtom atom2 = atoms[sBond.getAtom2().getMatchingAtom()];
          SmilesBond b = new SmilesBond(atom1, atom2, order, false);
          // do NOT add this bond to the second atom -- we will do that later;
          atom2.bondCount--;
          Logger.info("" + b);
        } else {
          SmilesAtom atom2 = atoms[sBond.getAtom1().getMatchingAtom()];
          SmilesBond b = atom2.getBondTo(atom1);
          // NOW we can add this bond
          atom1.addBond(b);
          
        }
      }
    }
    // fix H atoms
    for (int i = bsFixH.nextSetBit(0); i >= 0; i = bsFixH.nextSetBit(i + 1)) {
      Edge[] bonds = atoms[i].getEdges();
      Edge b = bonds[0];
      bonds[0] = bonds[1];
      bonds[1] = b;
    }
  }

  public void setTop(SmilesSearch parent) {
    if (parent == null)
      this.top = this;
    else 
      this.top = parent.getTop();
  }

  SmilesSearch getTop() {
    return (top == this ? this : top.getTop());
  }

  /**
   * htNested may contain $(select xxxx) primitives. 
   * We want to clear those up before we start any search.
   * 
   */
  void getSelections() {
    Map<String, Object> ht = top.htNested;
    if (ht == null || jmolAtoms.length == 0)
      return;
    Map<String, Object> htNew = new Hashtable<String, Object>();
    for (Map.Entry<String, Object> entry : ht.entrySet()) {
      String key = entry.getValue().toString();
      if (key.startsWith("select")) {
        BS bs = (htNew.containsKey(key) ? (BS) htNew.get(key) 
            : smartsAtoms[0].findAtomsLike(key.substring(6)));
        if (bs == null)
          bs = new BS();
        htNew.put(key, bs);
        entry.setValue(bs);
      }
    }
  }
  
}
