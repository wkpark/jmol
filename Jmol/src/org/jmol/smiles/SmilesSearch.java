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

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.java.BS;
import org.jmol.util.BNode;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.viewer.JC;

/**
 *  -- was SmilesMolecule, 
 * but this now includes more data than that and the search itself
 * so as to keep this thread safe
 * 
 */
public class SmilesSearch extends JmolMolecule {

  public SmilesSearch() {
  }
  
  @Override
  public String toString() {
    SB sb = new SB().append(pattern);
    sb.append("\nmolecular formula: " + getMolecularFormula(true, null, false)); 
    return sb.toString();    
  }


  // note that JC.SMILES_... exclude 0x0FF0
  final static int NO_AROMATIC                = 0x010; //SmilesParser -> SmilesSearch
  final static int IGNORE_STEREOCHEMISTRY     = 0x020; //SmilesParser -> SmilesSearch
  final static int INVERT_STEREOCHEMISTRY     = 0x040; //SmilesParser -> SmilesSearch
  
  /**
   * AROMATIC_DEFINED draws all aromatic bonds from connection definitions
   * It is deprecated, because a=a will set it by itself. 
   */
  final static int AROMATIC_DEFINED           = 0x080; //SmilesParser -> SmilesSearch
  
  /**
   * AROMATIC_STRICT enforces Hueckel 4+2 rule, not allowing acyclic double bonds
   * 
   */
  final static int AROMATIC_STRICT            = 0x100; //SmilesParser -> SmilesSearch
  
  /**
   * AROMATIC_DOUBLE allows a distinction between single and double, as for
   * example is necessary to distinguish between n=cNH2 and ncNH2 (necessary for
   * MMFF94 atom typing)
   */
  final static int AROMATIC_DOUBLE            = 0x200; //SmilesParser -> SmilesSearch
  

  /**
   * AROMATIC_MMFF94 also raises the strictness level to force all 6- and
   * 7-membered rings to have exactly three double bonds.
   */
  static final int AROMATIC_MMFF94            = 0x300; // includes AROMATIC_STRICT and AROMATIC_DOUBLE;
  
  /**
   * AROMATIC_PLANAR only invokes planarity
   * 
   */
  final static int AROMATIC_PLANAR            = 0x400; //SmilesParser -> SmilesSearch
  
//  /**
//   * AROMATIC_JSME_NONCANONICAL matches the JSME noncanonical option.
//  * 
//   */
//  final static int AROMATIC_JSME_NONCANONICAL = 0x800; //SmilesParser -> SmilesSearch
  final static int SET_ATROPICITY             = 0x800; // internal from SmilesGenerator


 
  private final static int INITIAL_ATOMS = 16;
  SmilesAtom[] patternAtoms = new SmilesAtom[INITIAL_ATOMS];

  /* ============================================================= */
  /*                             Setup                             */
  /* ============================================================= */

  String pattern;
  Node[] jmolAtoms;
  VTemp v = new VTemp();
  
  BNode[] bioAtoms;
  int jmolAtomCount;
  private BS bsSelected;
  private boolean strictAromatic;
  private boolean aromaticPlanar;
  
  static final int addFlags(int flags, String strFlags) {
    if (strFlags.indexOf("OPEN") >= 0)
      flags |= JC.SMILES_TYPE_OPENSMILES;
    if (strFlags.indexOf("BIO") >= 0)
      flags |= JC.SMILES_GEN_BIO;
    if (strFlags.indexOf("HYDROGEN") >= 0)
      flags |= JC.SMILES_GEN_EXPLICIT_H;

//    if (strFlags.indexOf("NONCANONICAL") >= 0) // no longer used
//      flags |= AROMATIC_JSME_NONCANONICAL; 
    

    if (strFlags.indexOf("FIRSTMATCHONLY") >= 0)
      flags |= JC.SMILES_MATCH_ONCE_ONLY;

    if (strFlags.indexOf("STRICT") >= 0) // MMFF94
      flags |= AROMATIC_STRICT;    
    if (strFlags.indexOf("PLANAR") >= 0) // MMFF94
      flags |= AROMATIC_PLANAR;    
    if (strFlags.indexOf("NOAROMATIC") >= 0 || strFlags.indexOf("NONAROMATIC") >= 0)
      flags |= NO_AROMATIC;
    if (strFlags.indexOf("AROMATICDOUBLE") >= 0) // MMFF94; deprecated
      flags |= AROMATIC_DOUBLE;
    if (strFlags.indexOf("AROMATICDEFINED") >= 0)
      flags |= AROMATIC_DEFINED;
    if (strFlags.indexOf("MMFF94") >= 0)
      flags |= AROMATIC_MMFF94;

    if (strFlags.indexOf("NOSTEREO") >= 0) {
      flags |= IGNORE_STEREOCHEMISTRY;
    } else if (strFlags.indexOf("INVERTSTEREO") >= 0) {
      if ((flags & INVERT_STEREOCHEMISTRY) != 0)
        flags &= ~INVERT_STEREOCHEMISTRY;
      else
        flags |= INVERT_STEREOCHEMISTRY;
    }
    if (strFlags.indexOf("ATOMCOMMENT") >= 0)
      flags |= JC.SMILES_GEN_ATOM_COMMENT;

    if (strFlags.indexOf("GROUPBYMODEL") >= 0)
      flags |= JC.SMILES_GROUP_BY_MODEL;
    
    if ((flags & JC.SMILES_GEN_BIO) == JC.SMILES_GEN_BIO) {
      if (strFlags.indexOf("NOCOMMENT") >= 0)
        flags |= JC.SMILES_GEN_BIO_NOCOMMENTS;
      if (strFlags.indexOf("UNMATCHED") >= 0)
        flags |= JC.SMILES_GEN_BIO_ALLOW_UNMATCHED_RINGS;
      if (strFlags.indexOf("COVALENT") >= 0)
        flags |= JC.SMILES_GEN_BIO_COV_CROSSLINK;
      if (strFlags.indexOf("HBOND") >= 0)
        flags |= JC.SMILES_GEN_BIO_HH_CROSSLINK;
    }

    if (strFlags.indexOf("_SETATROP_") >= 0)
      flags |= SET_ATROPICITY;
    return flags;
  }

  void setFlags(int flags) {
    this.flags = flags;
    noAromatic = ((flags & NO_AROMATIC) == NO_AROMATIC);
    
    // starting with Jmol 12.3.24, we allow the flag AROMATICDOUBLE to allow a
    // distinction between single and double, as for example is necessary to distinguish
    // between n=cNH2 and ncNH2 (necessary for MMFF94 atom typing
    //
    // starting with Jmol 14.4.5, presence of a=a will set this automatically.
    // but still of possible use in terms of comparing two structures
    // and used by the SMILES generator

    aromaticDouble = ((flags & AROMATIC_DOUBLE) == AROMATIC_DOUBLE); // {1.1}.find("SMILES/aromaticDouble",{2.1})

    ignoreStereochemistry = ((flags & IGNORE_STEREOCHEMISTRY) == IGNORE_STEREOCHEMISTRY);

    openSMILES = ((flags & JC.SMILES_TYPE_OPENSMILES) == JC.SMILES_TYPE_OPENSMILES);
    
    strictAromatic = ((flags & AROMATIC_STRICT) == AROMATIC_STRICT);

    aromaticPlanar = ((flags & AROMATIC_PLANAR) == AROMATIC_PLANAR);
    
    setAtropicity = ((flags & SET_ATROPICITY) == SET_ATROPICITY);    

    byModel = ((flags & JC.SMILES_GROUP_BY_MODEL) == JC.SMILES_GROUP_BY_MODEL);
    
    exitFirstMatch |= ((flags & JC.SMILES_MATCH_ONCE_ONLY) == JC.SMILES_MATCH_ONCE_ONLY);
  }

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
  boolean exitFirstMatch;
  boolean isSmarts;
  boolean haveTopo;
  boolean isTopology;
  boolean patternBioSequence;
  /**
   * Set false by SmilesParser to indicate to SmilesMatcher that 
   * the string already has aromatic atoms indicated and so
   * no aromaticity model should be applied.
   */
  boolean patternAromatic = true; 
  
  SmilesSearch[] subSearches;
  boolean haveSelected;
  boolean haveBondStereochemistry;
  SmilesStereo stereo;
  boolean needRingData;
  boolean needAromatic = true; // we just have to always consider aromatic, except in the case of bioSequences.
  boolean needRingMemberships;

  int nDouble;
  int ringDataMax = Integer.MIN_VALUE;
  SB ringSets;
  int ringCount;


  Lst<SmilesMeasure> measures = new  Lst<SmilesMeasure>();
  
  int flags;
  BS bsAromatic = new BS();
  BS bsAromatic5 = new BS();
  BS bsAromatic6 = new BS();
  
  SmilesAtom lastChainAtom;

  boolean asVector;
  boolean getMaps;
  
  SmilesSearch top = this;
  
  void setTop(SmilesSearch parent) {
    if (parent == null)
      this.top = this;
    else 
      this.top = parent.getTop();
  }

  SmilesSearch getTop() {
    return (top == this ? this : top.getTop());
  }


  
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

  private Lst<Object> vReturn;
  BS bsReturn = new BS();
  boolean ignoreStereochemistry;
  boolean invertStereochemistry;
  private boolean noAromatic;
  boolean setAtropicity;

   
  boolean aromaticDouble;
  boolean isNormalized;
  boolean openSMILES; // will also be openSMARTS if isSmarts is TRUE
  boolean haveComponents;
  private int thisJmolComponent = -1;
  private int thisPatternComponent = -1;
  private boolean byModel;

  void set() throws InvalidSmilesException {
    if (patternAtoms.length > ac)
      patternAtoms = (SmilesAtom[]) AU.arrayCopyObject(patternAtoms, ac);
    nodes = patternAtoms;
    isTopology = true;
    patternAromatic = false;
    patternBioSequence = true;
    for (int i = ac; --i >= 0;) {
      SmilesAtom atom = patternAtoms[i];
      if (isTopology && atom.isDefined())
        isTopology = false;
      if (!atom.isBioResidue)
        patternBioSequence = false;
      if (atom.isAromatic)
        patternAromatic = true;
      atom.setBondArray();
      if (!isSmarts && atom.bioType == '\0' && !atom.setHydrogenCount())
        throw new InvalidSmilesException("unbracketed atoms must be one of: "
            + SmilesAtom.UNBRACKETED_SET);
    }
    if (haveComponents) {
      for (int i = ac; --i >= 0;) {
        SmilesAtom a = patternAtoms[i];
        SmilesBond[] bonds = a.bonds;
        int ia = a.component;
        for (int j = a.bondCount; --j >= 0;) {
            SmilesBond b = bonds[j];
          int ib;
          if (b.isConnection && b.atom2 == a && (ib = b.atom1.component) != ia) {
            for (int k = ac; --k >= 0;)
              if (patternAtoms[k].component == ia)
                patternAtoms[k].component = ib;
          }
        }
      }
    }
  }

  SmilesAtom addAtom() {
    return appendAtom(new SmilesAtom());
  }

  SmilesAtom appendAtom(SmilesAtom sAtom) {
    if (ac >= patternAtoms.length)
      patternAtoms = (SmilesAtom[]) AU.doubleLength(patternAtoms);
    return patternAtoms[ac] = sAtom.setIndex(ac++);
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
    for (int i = 0; i < ac; i++)
      if ((nH = patternAtoms[i].explicitHydrogenCount) >= 0)
          n += nH;
    return n;
  }

  /**
   * Sets up all aromatic and ring data.
   * Called from SmilesGenerator.getSmilesComponent and SmilesMatcher.matchPriv.
   * 
   * @param bsA
   * @param vRings 
   * @param doProcessAromatic 
   * @throws InvalidSmilesException
   */
  void setRingData(BS bsA, Lst<BS>[] vRings, boolean doProcessAromatic) throws InvalidSmilesException {
    if (isTopology || patternBioSequence)
      needAromatic = false;
    if (needAromatic)
      needRingData = true;
    boolean noAromatic = ((flags & NO_AROMATIC) == NO_AROMATIC);
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
    getRingData(vRings, needRingData, doProcessAromatic);
  }

  @SuppressWarnings("unchecked")
  void getRingData(Lst<BS>[] vRings, boolean needRingData,
                   boolean doTestAromatic) throws InvalidSmilesException {
    boolean isStrict = strictAromatic || !openSMILES && !aromaticPlanar;
    boolean isOpenNotStrict = openSMILES && !strictAromatic;
    int strictness = (!isStrict ? 0
        : (flags & AROMATIC_MMFF94) == AROMATIC_MMFF94 ? 2 : 1);
    boolean isDefined = ((flags & AROMATIC_DEFINED) == AROMATIC_DEFINED);
    boolean doFinalize = (needAromatic && doTestAromatic && (isStrict || isOpenNotStrict));
    int aromaticMax = 7;
    Lst<BS> lstAromatic = (vRings == null ? new Lst<BS>()
        : (vRings[3] = new Lst<BS>()));
    Lst<SmilesRing> lstSP2 = (doFinalize ? new Lst<SmilesRing>() : null);
    int[] eCounts = (doFinalize ? new int[jmolAtomCount] : null);

    if (isDefined && needAromatic) {
      // predefined aromatic bonds
      SmilesAromatic.checkAromaticDefined(jmolAtoms, bsSelected, bsAromatic);
      strictness = 0;
    }
    int nAtoms = jmolAtomCount;
    boolean checkFlatness = (nAtoms > 0 && !(jmolAtoms[0] instanceof SmilesAtom));

    if (ringDataMax < 0)
      ringDataMax = 8;
    if (strictness > 0 && ringDataMax < 6)
      ringDataMax = 6;
    if (needRingData) {
      ringCounts = new int[nAtoms];
      ringConnections = new int[jmolAtomCount];
      ringData = new BS[ringDataMax + 1];
    }

    ringSets = new SB();
    String s = "****";
    int max = ringDataMax;
    while (s.length() < max)
      s += s;
    for (int i = 3; i <= max; i++) {
      if (i > nAtoms)
        continue;
      String smarts = "*1" + s.substring(0, i - 2) + "*1";
      SmilesSearch search = SmilesParser.getMolecule(smarts, true, true);
      Lst<Object> vR = (Lst<Object>) subsearch(search, false, true);
      if (vRings != null && i <= 5) {
        Lst<BS> v = new Lst<BS>();
        for (int j = vR.size(); --j >= 0;)
          v.addLast((BS) vR.get(j));
        vRings[i - 3] = v;
      }
      if (vR.size() == 0)
        continue;
      if (needAromatic && !isDefined && i >= 4 && i <= aromaticMax)
        SmilesAromatic.setAromatic(i, jmolAtoms, bsSelected, vR, bsAromatic,
            strictness, isOpenNotStrict, checkFlatness, v, lstAromatic, lstSP2,
            eCounts, doTestAromatic);
      if (needRingData) {
        ringData[i] = new BS();
        for (int k = vR.size(); --k >= 0;) {
          BS r = (BS) vR.get(k);
          ringData[i].or(r);
          for (int j = r.nextSetBit(0); j >= 0; j = r.nextSetBit(j + 1))
            ringCounts[j]++;
        }
      }
    }
    if (needAromatic) {
      if (doFinalize)
        SmilesAromatic.finalizeAromatic(jmolAtoms, bsAromatic, lstAromatic,
            lstSP2, eCounts, isOpenNotStrict, isStrict);
      // clean out all nonaromatic atoms from the ring list
      // and recreate 5- and 6-membered ring bitsets
      bsAromatic5.clearAll();
      bsAromatic6.clearAll();
      for (int i = lstAromatic.size(); --i >= 0;) {
        BS bs = lstAromatic.get(i);
        bs.and(bsAromatic);
        switch (bs.cardinality()) {
        case 5:
          bsAromatic5.or(bs);
          break;
        case 6:
          bsAromatic6.or(bs);
          break;
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

  Object subsearch(SmilesSearch search, 
                            boolean firstAtomOnly, 
                            boolean isRingCheck) throws InvalidSmilesException {
    search.ringSets = ringSets;
    search.jmolAtoms = jmolAtoms;
    search.bioAtoms = bioAtoms;
    search.jmolAtomCount = jmolAtomCount;
    search.bsSelected = bsSelected;
    search.htNested = htNested;
    search.haveTopo = haveTopo;
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
      search.exitFirstMatch = false;
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
    } else {
      // processing || 
      search.aromaticDouble = aromaticDouble;
      search.haveSelected = haveSelected;
      search.bsRequired = bsRequired;
      search.exitFirstMatch = exitFirstMatch;
      search.getMaps = getMaps;
      search.asVector = asVector;
      search.vReturn = vReturn;
      search.bsReturn = bsReturn;
      search.haveBondStereochemistry = haveBondStereochemistry;
    }
    return search.search2(firstAtomOnly);
  }
  
  /* ============================================================= */
  /*                             Search                            */
  /* ============================================================= */

  /** 
   * the start of the search. ret will be either a Vector or a BitSet
   * @return BitSet or Vector
   * @throws InvalidSmilesException 
   * 
   */
  Object search() throws InvalidSmilesException {
    return search2(false); 
  }
  
  private Object search2(boolean firstAtomOnly) throws InvalidSmilesException {

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

    setFlags(flags);
    // flags are passed on from SmilesParser /xxxxx/

    if (!isRingCheck && Logger.debugging && !isSilent)
      Logger.debug("SmilesSearch processing " + pattern);

    if (vReturn == null && (asVector || getMaps))
      vReturn = new  Lst<Object>();
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
        if (exitFirstMatch) {
          if (vReturn == null ? bsReturn.nextSetBit(0) >= 0 : vReturn.size() > 0)
            break;
        }
      }
    } else if (ac > 0) {
      nextTargetAtom(null, -1, -1, firstAtomOnly, true);
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
   * @param newComponent 
   * @return true to continue or false if oneOnly
   * @throws InvalidSmilesException 
   */
  private final boolean nextTargetAtom(SmilesAtom patternAtom, int atomNum,
                                   int iAtom, boolean firstAtomOnly, boolean newComponent)
      throws InvalidSmilesException {

    //if (!isRingCheck)
      //System.out.println("checkMatch " + patternAtom + " atomnum=" + atomNum + " iAtom=" + iAtom + " " + bsFound + " " + pattern);
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

      if (!isRingCheck && !isTopology) {
        // check atoms 
        if (patternAtom.atomsOr != null) {
          for (int ii = 0; ii < patternAtom.nAtomsOr; ii++)
            if (!nextTargetAtom(patternAtom.atomsOr[ii], atomNum, iAtom,
                firstAtomOnly, newComponent))
              return false;
          return true;
        }

        if (haveComponents && patternAtom.component > 0) {
          int c = (byModel ? jmolAtom.getModelIndex() : jmolAtom.getMoleculeNumber(false));
          if (newComponent) {
            if (thisJmolComponent == -1) {
              thisJmolComponent = c;
            }
          }
          System.out.println("c=" + c + " " + jmolAtom + " " + patternAtom + " " +  thisPatternComponent + " " + thisJmolComponent + " " + oldJmolComponent + (c != thisJmolComponent || c == oldJmolComponent));
          if (c != thisJmolComponent || c == oldJmolComponent)
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

        SmilesAtom atom1 = patternBond.atom1;
        int matchingAtom = atom1.getMatchingAtomIndex();

        // BIOSMILES/BIOSMARTS check is by group

        switch (patternBond.order) {
        case SmilesBond.TYPE_BIO_SEQUENCE:
        case SmilesBond.TYPE_BIO_CROSSLINK:
          if (!checkMatchBond(patternAtom, atom1, patternBond, iAtom,
              matchingAtom, null))
            return true;
          break;
        default:

          // regular SMILES/SMARTS check 
          // is to find the bond and test it against the pattern

          int k = 0;
          Edge jmolBond = null;
          for (; k < jmolBonds.length; k++)
            if ((jmolBond=jmolBonds[k]).isCovalent() && (jmolBond.getAtomIndex1() == matchingAtom || jmolBond.getAtomIndex2() == matchingAtom)
                )
              break;
          if (k == jmolBonds.length)
            return true; // probably wasn't a covalent bond or was an attached implicit H

          if (!checkMatchBond(patternAtom, atom1, patternBond, iAtom,
              matchingAtom, jmolBond))
            return true;
        }
      }

      // Note that we explicitly do a reference using
      // index because this could be a SEARCH [x,x] "sub" atom.

      patternAtoms[patternAtom.index].setMatchingAtom(jmolAtoms[iAtom], iAtom);

      // The atom has passed both the atom and the bond test.
      // Add this atom to the growing list.

      if (Logger.debugging && !isRingCheck){//&& !isSilent) {
        for (int i = 0; i <= atomNum; i++)
          Logger.debug("pattern atoms " + patternAtoms[i]);
        Logger. debug("--ss--");
      }
      bsFound.set(iAtom);
    }
    if (!nextPatternAtom(atomNum, iAtom, firstAtomOnly))
      return false;
    if (iAtom >= 0)
      clearBsFound(iAtom);
    return true;
  }

  private BS bsCheck;
  String atropKeys;
  private int oldJmolComponent;

  private boolean nextPatternAtom(int atomNum, int iAtom, boolean firstAtomOnly)
      throws InvalidSmilesException {

    Node jmolAtom;
    Edge[] jmolBonds;

    if (++atomNum < ac) {

      SmilesAtom newPatternAtom = patternAtoms[atomNum];

      // For all the pattern bonds for this atom...
      // find the bond to atoms already assigned.
      // If it is not there, then it means this is a
      // new component.

      // the nestedBond may be set to previous search
      SmilesBond newPatternBond = (iAtom >= 0 ? newPatternAtom.getBondTo(null)
          : atomNum == 0 ? nestedBond : null);
      if (newPatternBond == null) {

        // Option 1: we are processing "."

        //
        // run through all unmatched and unbonded-to-match
        // selected Jmol atoms to see if there is a match. 

        BS bs = BSUtil.copy(bsFound);
        BS bs0 = BSUtil.copy(bsFound);
        if (newPatternAtom.notBondedIndex >= 0) {
          SmilesAtom pa = patternAtoms[newPatternAtom.notBondedIndex];
          Node a = pa.getMatchingAtom();
          if (pa.isBioAtom) {
            // clear out adjacent residues
            int ii = ((BNode) a).getOffsetResidueAtom("\0", 1);
            if (ii >= 0)
              bs.set(ii);
            ii = ((BNode) a).getOffsetResidueAtom("\0", -1);
            if (ii >= 0)
              bs.set(ii);
          } else {
            // clear out all atoms connected to the last atom only
            jmolBonds = a.getEdges();
            for (int k = 0; k < jmolBonds.length; k++)
              bs.set(jmolBonds[k].getOtherAtomNode(a).getIndex());
          }
        }
        boolean skipGroup = ((newPatternAtom.isBioAtomWild));
        // TODO fix the *.*.*.*.* problem
        int j1 = bsSelected.nextSetBit(0);
        j1 = (skipGroup && j1 >= 0 ? ((BNode) jmolAtoms[j1])
            .getOffsetResidueAtom("\0", j1) : j1);
        boolean isSameComponent = (thisPatternComponent == newPatternAtom.component);
        int myPatternComponent = thisPatternComponent;
        if (!isSameComponent) {
          thisPatternComponent = newPatternAtom.component;
          oldJmolComponent = thisJmolComponent;
          thisJmolComponent = -1;
        }
        int ojc = oldJmolComponent;

        for (int j = j1; j >= 0; j = bsSelected.nextSetBit(j + 1)) {
          if (!bs.get(j)
              && !nextTargetAtom(newPatternAtom, atomNum, j, firstAtomOnly,
                  true))
            return false;
          if (haveComponents) {
            thisPatternComponent = myPatternComponent;
            if (!isSameComponent) {
              oldJmolComponent = ojc;
              thisJmolComponent = -1;
            }
          }
          if (skipGroup) {
            j1 = ((BNode) jmolAtoms[j]).getOffsetResidueAtom(
                newPatternAtom.bioAtomName, 1);
            if (j1 >= 0)
              j = j1 - 1;
          }
        }
        bsFound = bs0;
        return true;
      }

      // The new atom is connected to the old one in the pattern.
      // It doesn't so much matter WHICH connection we found -- 
      // there may be several -- but whatever we have, we must
      // have a connection in the real molecule between these two
      // particular atoms. So we just follow that connection. 

      jmolAtom = newPatternBond.atom1.getMatchingAtom();

      // Option 2: The connecting bond is a bio sequence or
      // from ~GGC(T)C:ATTC...
      // For sequences, we go to the next GROUP, either via
      // the standard sequence or via basepair/cysteine pairing. 

      switch (newPatternBond.order) {
      case SmilesBond.TYPE_BIO_SEQUENCE:
        int nextGroupAtom = ((BNode) jmolAtom).getOffsetResidueAtom(
            newPatternAtom.bioAtomName, 1);
        if (nextGroupAtom >= 0) {
          BS bs = BSUtil.copy(bsFound);
          ((BNode) jmolAtom).getGroupBits(bsFound);

          // working here

          if (!nextTargetAtom(newPatternAtom, atomNum, nextGroupAtom,
              firstAtomOnly, false))
            return false;
          bsFound = bs;
        }
        return true;
      case SmilesBond.TYPE_BIO_CROSSLINK:
        Lst<Integer> vLinks = new Lst<Integer>();
        ((BNode) jmolAtom).getCrossLinkVector(vLinks, true, true);
        BS bs = BSUtil.copy(bsFound);
        ((BNode) jmolAtom).getGroupBits(bsFound);
        // here we only use the third entry -- lead atoms
        for (int j = 2; j < vLinks.size(); j += 3)
          if (!nextTargetAtom(newPatternAtom, atomNum,
              vLinks.get(j).intValue(), firstAtomOnly, false))
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
          if (!nextTargetAtom(newPatternAtom, atomNum,
              jmolAtom.getBondedAtomIndex(j), firstAtomOnly, false))
            return false;

      // Done checking this atom from any one of the places
      // higher in this stack. Clear the atom and keep going...

      clearBsFound(iAtom);
      return true;
    }

    // the pattern is complete

    // check stereochemistry

    if (!ignoreStereochemistry && !isRingCheck && !checkStereochemistry())
      return true;

    // set up the return BitSet and Vector, if requested

    // bioSequences only return the "lead" atom 
    // If the search is SMILES, we add the missing hydrogens

    BS bs = new BS();
    int nMatch = 0;
    for (int j = 0; j < ac; j++) {
      int i = patternAtoms[j].getMatchingAtomIndex();
      if (!firstAtomOnly && top.haveSelected && !patternAtoms[j].selected)
        continue;
      nMatch++;
      bs.set(i);
      if (patternAtoms[j].isBioAtomWild)
        ((BNode) jmolAtoms[i]).getGroupBits(bs);
      if (firstAtomOnly)
        break; // TODO -- ohoh, I need this?
      if (!isSmarts)
        if (!setAtropicity && patternAtoms[j].explicitHydrogenCount > 0) {
          Node atom = jmolAtoms[i];
          for (int k = 0, n = atom.getEdges().length; k < n; k++) {
            int ia = atom.getBondedAtomIndex(k);
            if (jmolAtoms[ia].getElementNumber() == 1)
              bs.set(ia);
          }
        }
    }
    if (bsRequired != null && !bsRequired.intersects(bs))
      return true;
    if (!isSmarts && bs.cardinality() != selectedAtomCount)
      return true;
    if (bsCheck != null) {
      if (firstAtomOnly) {
        bsCheck.clearAll();
        for (int j = 0; j < ac; j++) {
          bsCheck.set(patternAtoms[j].getMatchingAtomIndex());
        }
        if (bsCheck.cardinality() != ac)
          return true;
      } else {
        if (bs.cardinality() != ac)
          return true;
      }
    }
    bsReturn.or(bs);

    if (getMaps) {
      // every map is important always -- why??
      int[] map = new int[nMatch];
      for (int j = 0, nn = 0; j < ac; j++) {
        if (!firstAtomOnly && top.haveSelected && !patternAtoms[j].selected)
          continue;
        map[nn++] = patternAtoms[j].getMatchingAtomIndex();
      }
      vReturn.addLast(map);
      return !exitFirstMatch;
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
                % atomNum].getMatchingAtomIndex());
      ringSets.append("- ");
      return true;
    }

    // requested return is a BitSet or vector of BitSets

    // TRUE means "continue searching"

    if (exitFirstMatch)
      return false;

    // only continue if we have not found all the atoms already

    return (bs.cardinality() != selectedAtomCount);

  }

  private void clearBsFound(int iAtom) {
    
    if (iAtom < 0) {
      if (bsCheck == null) {bsFound.clearAll();}
      }
    else
      bsFound.clear(iAtom);    
  }

  private boolean checkPrimitiveAtom(SmilesAtom patternAtom, int iAtom)
      throws InvalidSmilesException {
    Node atom = jmolAtoms[iAtom];
    boolean foundAtom = patternAtom.not;

    while (true) {

      // _<n> apply "recursive" SEARCH -- for example, [C&$(C[$(aaaO);$(aaC)])]"
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

      // all types
      int na = atom.getElementNumber();
      // #<n> or Symbol Check atomic number
      // look out for atomElemNo == -2 --> "*" in target SMILES
      int n = patternAtom.elementNumber;
      if (na >= 0 && n >= 0 && n != na)
        break;

      if (patternAtom.isBioResidue) {
        BNode a = (BNode) atom;
        // <*.name>
        if (patternAtom.bioAtomName != null
            && (patternAtom.isLeadAtom() ? !a.isLeadAtom()
                : !patternAtom.bioAtomName
                    .equals(a.getAtomName().toUpperCase())))
          break;
        // <res.*>
        if (patternAtom.residueName != null
            && !patternAtom.residueName
                .equals(a.getGroup3(false).toUpperCase()))
          break;
        // <res#n
        if (patternAtom.residueNumber != Integer.MIN_VALUE
            && patternAtom.residueNumber != (a.getResno()))
          break;
        if (patternAtom.residueChar != null || patternAtom.elementNumber == -2) {
          char atype = a.getBioSmilesType();
          char ptype = patternAtom.getBioSmilesType();
          boolean ok = true;
          boolean isNucleic = false;
          switch (ptype) {
          case '\0':
          case '*':
            ok = true;
            break;
          case 'n':
            ok = (atype == 'r' || atype == 'c');
            isNucleic = true;
            break;
          case 'r':
          case 'c':
            isNucleic = true;
            //$FALL-THROUGH$
          default:
            ok = (atype == ptype);
            break;
          }
          if (!ok)
            break;
          String s = a.getGroup1('\0').toUpperCase();
          char resChar = (patternAtom.residueChar == null ? '*'
              : patternAtom.residueChar.charAt(0));
          boolean isOK = (resChar == s.charAt(0));
          switch (resChar) {
          case '*':
            isOK = true;
            break;
          case 'N':
            isOK = isNucleic ? (atype == 'r' || atype == 'c') : isOK;
            break;
          case 'R': // arginine purine
            isOK = isNucleic ? a.isPurine() : isOK;
            break;
          case 'Y': // tyrosine or pyrimidine
            isOK = isNucleic ? a.isPyrimidine() : isOK;
            break;
          }
          if (!isOK)
            break;
        }
        if (patternAtom.isBioAtom) {
          // BIOSMARTS
          // cross linking, residueChar, 
          if (patternAtom.notCrossLinked
              && a.getCrossLinkVector(null, true, true))
            break;
        }
        
      } else {

        // not a bioResidue

        // [<mass>symbol<stereo><hcount><charge><:class>]

        // application-specific 
        // #-<n> application-specific atom number
        if (patternAtom.atomNumber != Integer.MIN_VALUE
            && patternAtom.atomNumber != (((BNode) atom).getAtomNumber()))
          break;
        // =<n>  Jmol index
        if (patternAtom.jmolIndex >= 0
            && atom.getIndex() != patternAtom.jmolIndex)
          break;
        // <"xxx">
        if (patternAtom.atomType != null
            && !patternAtom.atomType.equals(atom.getAtomType()))
          break;

        // could  be *
        // <n> Check isotope
        // smiles indicates [13C] or [12C]
        // must match perfectly -- [12C] matches only explicit C-12, not
        // "unlabeled" C
        if ((n = patternAtom.getAtomicMass()) != Integer.MIN_VALUE
            && (n >= 0 && n != (na = atom.getIsotopeNumber()) || n < 0
                && na != 0 && -n != na))
          break;

        // Check aromatic
        boolean isAromatic = patternAtom.isAromatic;
        // aromaticAmbiguous could be [#6] or [D3]
        if (!noAromatic && !patternAtom.aromaticAmbiguous
            && isAromatic != bsAromatic.get(iAtom))
          break;

        // <+/-> Check charge
        if ((n = patternAtom.getCharge()) != Integer.MIN_VALUE
            && n != atom.getFormalCharge())
          break;

        // H<n> TOTAL H count
        //problem here is that you can have [CH][H]
        n = patternAtom.getCovalentHydrogenCount()
            + patternAtom.explicitHydrogenCount;
        if (n >= 0 && n != atom.getTotalHydrogenCount())
          break;

        // h<n> implicit H count -- will be 0 for standard Jmol model; 
        // may be > 0 for SMILES string or PDB model
        // may be -1, from [h] alone to indicate "at least 1"
        if ((n = patternAtom.implicitHydrogenCount) != Integer.MIN_VALUE) {
          na = atom.getImplicitHydrogenCount();
          if (n == -1 ? na == 0 : n != na)
            break;
        }

        // D<n> explicit degree -- does not count missing hydrogens
        // so is NOT appropriate for PDB file MMFF94 calc
        if (patternAtom.degree > 0
            && patternAtom.degree != atom.getCovalentBondCount()
                - atom.getImplicitHydrogenCount())
          break;

        // d<n> degree
        if (patternAtom.nonhydrogenDegree > 0
            && patternAtom.nonhydrogenDegree != atom.getCovalentBondCount()
                - atom.getCovalentHydrogenCount())
          break;

        // v<n> valence
        if (isSmarts && patternAtom.valence > 0
            && patternAtom.valence != atom.getTotalValence())
          break;

        // X<n> connectivity  -- includes all missing H atoms
        if (patternAtom.connectivity > 0
            && patternAtom.connectivity != atom
                .getCovalentBondCountPlusMissingH())
          break;

        // #-<n> application-specific atom number
        if (patternAtom.atomNumber != Integer.MIN_VALUE
            && patternAtom.atomNumber != (((BNode) atom).getAtomNumber()))
          break;
        // =<n>  Jmol index
        if (patternAtom.jmolIndex >= 0
            && atom.getIndex() != patternAtom.jmolIndex)
          break;
        // <"xxx">
        if (patternAtom.atomType != null
            && !patternAtom.atomType.equals(atom.getAtomType()))
          break;

        if (openSMILES || isSmarts) {
          // :<n> atom class  -- will be Float.NaN, and Float.NaN is not equal to any number
          if (!Float.isNaN(patternAtom.atomClass)
              && patternAtom.atomClass != atom
                  .getFloatProperty("property_atomclass"))
            break;
        }
        
        if (ringData != null) {
          // r<n> ring of a given size or [R]
          if (patternAtom.ringSize >= -1) {
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
          // R<n> a certain number of rings
          if (patternAtom.ringMembership >= -1) {
            //  R --> -1 implies "!R0"
            if (patternAtom.ringMembership == -1 ? ringCounts[iAtom] == 0
                : ringCounts[iAtom] != patternAtom.ringMembership)
              break;
          }
          // x<n>
          if (patternAtom.ringConnectivity >= 0) {
            // default > 0
            n = ringConnections[iAtom];
            if (patternAtom.ringConnectivity == -1 && n == 0
                || patternAtom.ringConnectivity != -1
                && n != patternAtom.ringConnectivity)
              break;
          }
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

    if (!isRingCheck && !isTopology)
      if (patternBond.nPrimitives == 0) {
        if (!checkPrimitiveBond(patternBond, iAtom, matchingAtom, bond))
          return false;
      } else {
        for (int i = 0; i < patternBond.nPrimitives; i++) {
          SmilesBond prim = patternBond.setPrimitive(i);
          if (!checkPrimitiveBond(prim, iAtom,
              matchingAtom, bond))
            return false;
        }
      }
    patternBond.matchingBond = bond;
    return true;
  }

  private boolean checkPrimitiveBond(SmilesBond patternBond, int iAtom1,
                                     int iAtom2, Edge bond) {
    boolean bondFound = false;

    switch (patternBond.order) {
    case SmilesBond.TYPE_BIO_SEQUENCE: // +
      return (patternBond.isNot != (bioAtoms[iAtom2].getOffsetResidueAtom("\0",
          1) == bioAtoms[iAtom1].getOffsetResidueAtom("\0", 0)));
    case SmilesBond.TYPE_BIO_CROSSLINK: // :
      return (patternBond.isNot != bioAtoms[iAtom1]
          .isCrossLinked(bioAtoms[iAtom2]));
    }

    boolean isAromatic1 = (!noAromatic && bsAromatic.get(iAtom1));
    boolean isAromatic2 = (!noAromatic && bsAromatic.get(iAtom2));
    int order = bond.getCovalentOrder();
    int patternOrder = patternBond.order;
    if (isAromatic1 && isAromatic2) {
      switch (patternOrder) {
      case SmilesBond.TYPE_AROMATIC:
      case SmilesBond.TYPE_RING:
        bondFound = isRingBond(ringSets, iAtom1, iAtom2);
        break;
      case Edge.BOND_COVALENT_SINGLE:
        // for SMARTS, single bond in aromatic means TO ANOTHER RING;
        // for SMILES, we don't care
        bondFound = !isSmarts || !isRingBond(ringSets, iAtom1, iAtom2);
        break;
      case Edge.BOND_COVALENT_DOUBLE:
        // note: Freiburg considers TYPE_DOUBLE to be NOT aromatic
        // changed for Jmol 12.2.RC8
        // but this is ambiguous at http://www.daylight.com/dayhtml/doc/theory/theory.smarts.html
        // see, for example: http://opentox.informatik.uni-freiburg.de/depict?data=[H]C%3D1C%28[H]%29%3DC%28[H]%29C%28%3DC%28C%3D1%28[H]%29%29C%28F%29%28F%29F%29S[H]&smarts=[%236]=[%236]
        // however, if it is not SMARTS, then we consider this fine -- it does
        // not matter what the order is for double/single bonds around the ring
        // 
        // starting with JmpatternBond.isNotol 12.3.24, we allow the directive aromaticDouble to allow a
        // distinction between single and double, as for example is necessary to distinguish
        // between n=cNH2 and ncNH2 (necessary for MMFF94 atom typing
        //
        // starting  with Jmol 14.4.5 we allow any presence of a=a to set the aromaticDouble flag automatically
        // and deprecate the  aromaticDouble directive.

        //
        bondFound = isNormalized
            || aromaticDouble
            && (order == Edge.BOND_COVALENT_DOUBLE || order == Edge.BOND_AROMATIC_DOUBLE);
        break;
      case Edge.TYPE_ATROPISOMER:
      case Edge.TYPE_ATROPISOMER_REV:
        bondFound = !patternBond.isNot; // negates this; ensures isNot is used only in stereochem 
        break;
      case SmilesBond.TYPE_ANY:
      case SmilesBond.TYPE_UNKNOWN:
        bondFound = true;
        break;
      }
    } else {
      switch (patternOrder) {
      case SmilesBond.TYPE_AROMATIC: // :
        if (!noAromatic)
          break;
        //$FALL-THROUGH$
      case SmilesBond.TYPE_ANY:
      case SmilesBond.TYPE_UNKNOWN:
        bondFound = true;
        break;
      case Edge.BOND_COVALENT_SINGLE:
      case Edge.BOND_STEREO_NEAR:
      case Edge.BOND_STEREO_FAR:
        switch (order) {
        case Edge.BOND_COVALENT_SINGLE:
        case Edge.BOND_STEREO_NEAR:
        case Edge.BOND_STEREO_FAR:
          bondFound = true;
          break;
        }
        break;
      case Edge.TYPE_ATROPISOMER:
      case Edge.TYPE_ATROPISOMER_REV:
        switch (order) {
        case Edge.BOND_COVALENT_SINGLE:
        case Edge.TYPE_ATROPISOMER:
        case Edge.TYPE_ATROPISOMER_REV:
          bondFound = !patternBond.isNot; // negates this; ensures isNot is used only in stereochem 
          break;
        }
        break;
      case Edge.BOND_COVALENT_DOUBLE:
      case Edge.BOND_COVALENT_TRIPLE:
      case Edge.BOND_COVALENT_QUADRUPLE:
        bondFound = (order == patternOrder);
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
    if (stereo != null && !stereo.checkStereoChemistry(this, v))
      return false;

    if (!haveBondStereochemistry)
      return true;

    //  /C=C/ and /C=N/ double bond stereochemistry
    //  a^nn-a stereochemistry

    Lst<SmilesBond> lstAtrop = null;
    SmilesBond b = null;
    for (int k = 0; k < ac; k++) {
      SmilesAtom sAtom1 = patternAtoms[k];
      SmilesAtom sAtom2 = null;
      SmilesAtom sAtomDirected1 = null;
      SmilesAtom sAtomDirected2 = null;
      int dir1 = 0;
      int dir2 = 0;
      int bondType = 0;
      int nBonds = sAtom1.getBondCount();
      boolean isAtropisomer = false;
      boolean indexOrder = true;
      for (int j = 0; j < nBonds; j++) {
        b = sAtom1.getBond(j);
        boolean isAtom2 = (b.atom2 == sAtom1);
        indexOrder = (b.atom1.index < b.atom2.index);
        int type = b.order;
        switch (type) {
        case Edge.TYPE_ATROPISOMER:
        case Edge.TYPE_ATROPISOMER_REV:
          if (!indexOrder)
            continue;
          //$FALL-THROUGH$
        case Edge.BOND_COVALENT_DOUBLE:
          if (isAtom2)
            continue;
          sAtom2 = b.atom2;
          bondType = type;
          isAtropisomer = (type != Edge.BOND_COVALENT_DOUBLE);
          if (isAtropisomer)
            dir1 = (b.isNot ? -1 : 1);
          break;
        case Edge.BOND_STEREO_NEAR:
        case Edge.BOND_STEREO_FAR:
          sAtomDirected1 = (isAtom2 ? b.atom1 : b.atom2);
          dir1 = (isAtom2 != (type == Edge.BOND_STEREO_NEAR) ? 1 : -1);
          break;
        }
      }
      if (isAtropisomer) {

        if (setAtropicity) {
          if (lstAtrop == null)
            lstAtrop = new Lst<SmilesBond>();
          lstAtrop.addLast(b);
          continue;
        }

        SmilesBond b1 = sAtom1.getBond(b.atropType[0]);
        if (b1 == null)
          return false;
        sAtomDirected1 = b1.getOtherAtom(sAtom1);
        b1 = sAtom2.getBond(b.atropType[1]);
        if (b1 == null)
          return false;
        sAtomDirected2 = b1.getOtherAtom(sAtom2);
        if (Logger.debugging)
          Logger.info("atropisomer check for atoms " + sAtomDirected1 + sAtom1
              + " " + sAtom2 + sAtomDirected2);
      } else {
        if (sAtom2 == null || dir1 == 0)
          continue;
        // cumulene stuff here
        // --> new sAtom2
        Node a10 = sAtom1;
        int nCumulene = 0;
        while (sAtom2.getBondCount() == 2 && sAtom2.getValence() == 4) {
          nCumulene++;
          Edge[] e2 = sAtom2.getEdges();
          Edge e = e2[e2[0].getOtherAtomNode(sAtom2) == a10 ? 1 : 0];
          a10 = sAtom2;
          sAtom2 = (SmilesAtom) e.getOtherAtomNode(sAtom2);
        }
        if (nCumulene % 2 == 1)
          continue;
        nBonds = sAtom2.getBondCount();
        for (int j = 0; j < nBonds && dir2 == 0; j++) {
          b = sAtom2.getBond(j);
          int type = b.order;
          switch (type) {
          case Edge.BOND_STEREO_NEAR:
          case Edge.BOND_STEREO_FAR:
            boolean isAtom2 = (b.atom2 == sAtom2);
            sAtomDirected2 = (isAtom2 ? b.atom1 : b.atom2);
            dir2 = (isAtom2 != (type == Edge.BOND_STEREO_NEAR) ? 1 : -1);
            break;
          }
        }
        if (dir2 == 0)
          continue;
      }
      Node dbAtom1 = sAtom1.getMatchingAtom();
      Node dbAtom2 = sAtom2.getMatchingAtom();
      Node dbAtom1a = sAtomDirected1.getMatchingAtom();
      Node dbAtom2a = sAtomDirected2.getMatchingAtom();
      if (dbAtom1a == null || dbAtom2a == null)
        return false;
      if (haveTopo)
        setTopoCoordinates((SmilesAtom) dbAtom1, (SmilesAtom) dbAtom2,
            (SmilesAtom) dbAtom1a, (SmilesAtom) dbAtom2a, bondType);
      float d = SmilesMeasure.setTorsionData((T3) dbAtom1a, (T3) dbAtom1,
          (T3) dbAtom2, (T3) dbAtom2a, v, isAtropisomer);
      if (isAtropisomer) {
        // just looking for d value that is positive (0 to 180)
        // the dihedral, from front to back, will be positive:  0 to 180 range 
        // dir1 is 1 or -1(NOT)
        d *= dir1 * (bondType == Edge.TYPE_ATROPISOMER ? 1 : -1) * (indexOrder ? 1 : -1);
        System.out.println("atrop dihedral " + d + " " + sAtom1 + " " + sAtom2 + " " +  b);
        if (d < 1.0f) // don't count a fraction of a degree as sufficient
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
    if (setAtropicity) {
      // the goal here is to match up the Jmol atropisomerism dihedral
      // atoms with the SMILES version
      atropKeys = "";
      for (int i = 0; i < lstAtrop.size(); i++) {
        b = lstAtrop.get(i);
        int i1 = getAtropIndex(b, true);
        int i2 = getAtropIndex(b, false);
        //System.out.println("atrop keys i1 + " " + i2);
        atropKeys += "," + i1 + i2;
      }      
    }
    return true;

  }

  private int getAtropIndex(SmilesBond b, boolean isFirst) {
    SmilesAtom s1 = (isFirst ? b.atom1 : b.atom2);
    BNode a1 = (BNode) s1.getMatchingAtom();
    Node a11 = Edge.getAtropismNode(b.matchingBond.order, a1, isFirst);
    SmilesBond[] b1 = s1.bonds;
    for (int i = s1.getBondCount(); --i >= 0;)
      if (((SmilesAtom) b1[i].getOtherAtomNode(s1)).getMatchingAtom() == a11)
        return i + 1;
    return 0;
  }

  private void setTopoCoordinates(SmilesAtom dbAtom1, SmilesAtom dbAtom2,
                                      SmilesAtom dbAtom1a, SmilesAtom dbAtom2a,
                                      int bondType) {
    dbAtom1.set(-1, 0, 0);
    dbAtom2.set(1, 0, 0);
    if (bondType != Edge.BOND_COVALENT_DOUBLE) {

      // atropisomerism
      //
      // we will be looking for a + or - dihedral angle
      // so just set that

      SmilesBond bond = dbAtom1.getBondTo(dbAtom2);
      int dir = (bond.order == Edge.TYPE_ATROPISOMER ? 1 : -1);
      dbAtom1a.set(-1, 1, 0);
      dbAtom2a.set(1, 1, dir / 2.0f);
      return;
    }

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

    int nBonds = 0;
    int dir1 = 0;
    Edge[] bonds = dbAtom1.getEdges();
    for (int k = bonds.length; --k >= 0;) {
      Edge bond = bonds[k];
      if (bond.order == Edge.BOND_COVALENT_DOUBLE)
        continue;
      Node atom = bond.getOtherAtomNode(dbAtom1);
      atom.set(-1, (nBonds++ == 0) ? -1 : 1, 0);
      int mode = (bond.getAtomIndex2() == dbAtom1.getIndex() ? nBonds : -nBonds);
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
      if (bond.order == Edge.BOND_COVALENT_DOUBLE)
        continue;
      Node atom = bond.getOtherAtomNode(dbAtom2);
      atoms[nBonds] = atom;
      atom.set(1, (nBonds++ == 0) ? 1 : -1, 0);
      int mode = (bond.getAtomIndex2() == dbAtom2.getIndex() ? nBonds : -nBonds);
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
  }


  /**
   * 
   * @param bsAro
   *        null for molecular formula calculation only
   * @throws InvalidSmilesException
   * 
   */
  void createTopoMap(BS bsAro) throws InvalidSmilesException {
    boolean isForMF = (bsAro == null);
    int nAtomsMissing = getMissingHydrogenCount();
    SmilesAtom[] atoms = new SmilesAtom[ac + nAtomsMissing];
    jmolAtoms = atoms;
    for (int i = 0, ptAtom = 0; i < ac; i++, ptAtom++) {
      SmilesAtom sAtom = patternAtoms[i];
      // this number will include the number of Hs in [CH2] as well as the number needed by C by itself
      int n = sAtom.explicitHydrogenCount;
      if (n < 0)
        n = 0;
      // create a Jmol atom for this pattern atom
      // we co-opt atom.matchingAtom here
      // because this search will never actually be run
      SmilesAtom atom = atoms[ptAtom] = new SmilesAtom().setTopoAtom(sAtom.component, ptAtom,
          sAtom.symbol, sAtom.getCharge());
      atom.implicitHydrogenCount = n;
      if (isForMF)
        continue;
      atom.mapIndex = i;
      atom.stereo = sAtom.stereo;
      atom.setAtomicMass(sAtom.getAtomicMass());
      atom.bioAtomName = sAtom.bioAtomName;
      atom.residueName = sAtom.residueName;
      atom.residueChar = sAtom.residueChar;
      atom.residueNumber = sAtom.residueNumber;
      atom.atomNumber = sAtom.residueNumber;
      atom.atomClass = sAtom.atomClass;
      atom.explicitHydrogenCount = 0;
      atom.isBioAtom = sAtom.isBioAtom;
      atom.bioType = sAtom.bioType;
      atom.isLeadAtom = sAtom.isLeadAtom;

      // we pass on the aromatic flag because
      // we don't want SmilesSearch to calculate
      // that for us
      if (!isForMF && sAtom.isAromatic)
        bsAro.set(ptAtom);

      // set up the bonds array and fill with H atoms
      // when there is only 1 H and the atom is NOT FIRST, then it will
      // be important to designate the bonds in order -- with the
      // H SECOND not first
      // this is still not satisfactory for allenes or the second atom of 
      // imines and possibly double bonds. We handle that later.

      sAtom.setMatchingAtom(null, ptAtom);
      SmilesBond[] bonds = new SmilesBond[sAtom.getBondCount() + n];
      atom.setBonds(bonds);
      while (--n >= 0) {
        SmilesAtom atomH = atoms[++ptAtom] = new SmilesAtom().setTopoAtom(atom.component,
            ptAtom, "H", 0);
        atomH.mapIndex = -i - 1;
        atomH.setBonds(new SmilesBond[1]);
        SmilesBond b = new SmilesBond(atom, atomH, Edge.BOND_COVALENT_SINGLE,
            false);
        if (Logger.debugging)
          Logger.info("" + b);
      }
    }
    if (isForMF)
      return;
    // set up bonds
    for (int i = 0; i < ac; i++) {
      SmilesAtom sAtom = patternAtoms[i];
      int i1 = sAtom.getMatchingAtomIndex();
      SmilesAtom atom1 = atoms[i1];
      int n = sAtom.getBondCount();
      for (int j = 0; j < n; j++) {
        SmilesBond sBond = sAtom.getBond(j);
        boolean firstAtom = (sBond.atom1 == sAtom);
        //SmilesBond b;
        if (firstAtom) {
          int order = 1;
          switch (sBond.order) {
          case Edge.BOND_COVALENT_SINGLE:
          case Edge.BOND_COVALENT_DOUBLE:
          case Edge.BOND_COVALENT_TRIPLE:
          case Edge.BOND_COVALENT_QUADRUPLE:
          case Edge.BOND_STEREO_NEAR:
          case Edge.BOND_STEREO_FAR:
          case Edge.TYPE_ATROPISOMER:
          case Edge.TYPE_ATROPISOMER_REV:
          case SmilesBond.TYPE_BIO_CROSSLINK:
          case SmilesBond.TYPE_BIO_SEQUENCE:
            order = sBond.order;
            break;
          case SmilesBond.TYPE_AROMATIC:
            order = Edge.BOND_AROMATIC_DOUBLE;
            break;
          }
          SmilesAtom atom2 = atoms[sBond.atom2.getMatchingAtomIndex()];
          SmilesBond b = new SmilesBond(atom1, atom2, order, false);
          // do NOT add this bond to the second atom -- we will do that later;
          atom2.bondCount--;
          if (Logger.debugging)
            Logger.info("" + b);
        } else {
          SmilesAtom atom2 = atoms[sBond.atom1.getMatchingAtomIndex()];
          SmilesBond b = atom2.getBondTo(atom1);
          // NOW we can add this bond
          atom1.addBond(b);
        }
      }
    }
    // fix H atoms
    for (int i = 0; i < ac; i++) {
      SmilesAtom a = atoms[i];
      SmilesBond[] bonds = a.bonds;
      if (bonds.length < 2 || bonds[0].isFromPreviousTo(a))
        continue;
      for (int k = bonds.length; --k >= 1;)
        if (bonds[k].isFromPreviousTo(a)) {
          SmilesBond b = bonds[k];
          bonds[k] = bonds[0];
          bonds[0] = b;
        }
    }
    if (!ignoreStereochemistry)
      // should also be checking for subsearches and htNested?
      for (int i = ac; --i >= 0;) {
        SmilesAtom sAtom = patternAtoms[i];
        if (sAtom.stereo != null)
          sAtom.stereo.fixStereo(sAtom);
      }

  }

  /**
   * create a temporary object to generate the aromaticity in a SMILES pattern
   * for which there is no explicit aromaticity (Kekule)
   * 
   * Not applicable to SMARTS
   * 
   * @param atoms
   * @param bsAromatic
   * @param flags
   * @throws InvalidSmilesException
   */
  static void normalizeAromaticity(SmilesAtom[] atoms, BS bsAromatic, int flags)
      throws InvalidSmilesException {
    SmilesSearch ss = new SmilesSearch();
    ss.setFlags(flags);
    ss.jmolAtoms = atoms;
    ss.jmolAtomCount = atoms.length;
    ss.bsSelected = BSUtil.newBitSet2(0, atoms.length);
    Lst<BS>[] vRings = AU.createArrayOfArrayList(4);
    ss.setRingData(null, vRings, true);
    bsAromatic.or(ss.bsAromatic);
    if (!bsAromatic.isEmpty()) {
      Lst<BS> lst = vRings[3]; // aromatic rings
      for (int i = lst.size(); --i >= 0;) {
        BS bs = lst.get(i);
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          SmilesAtom a = atoms[j];
          if (a.isAromatic || a.elementNumber == -2 || a.elementNumber == 0)
            continue;
          a.setSymbol(a.symbol.toLowerCase());
        }
      }
    }
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
            : jmolAtoms[0].findAtomsLike(key.substring(6)));
        if (bs == null)
          bs = new BS();
        htNew.put(key, bs);
        entry.setValue(bs);
      }
    }
  }

  /**
   * calculates a normal to a plane for three points and returns a signed
   * distance
   * 
   * @param pointA
   * @param pointB
   * @param pointC
   * @param vNorm
   * @param vAB
   * @param vAC
   * @return a signed distance
   */
  static float getNormalThroughPoints(Node pointA, Node pointB, Node pointC,
                                      V3 vNorm, V3 vAB, V3 vAC) {
    vAB.sub2((P3) pointB, (P3) pointA);
    vAC.sub2((P3) pointC, (P3) pointA);
    vNorm.cross(vAB, vAC);
    vNorm.normalize();
    // ax + by + cz + d = 0
    // so if a point is in the plane, then N dot X = -d
    vAB.setT((P3) pointA);
    return -vAB.dot(vNorm);
  }

  Node findImplicitHydrogen(Node atom) {
//    if (haveTopo) {
//      SmilesAtom sAtom = (SmilesAtom) atom;
//      SmilesBond[] b = sAtom.bonds;
//      for (int i = 0; i < b.length; i++) {
//        SmilesAtom a = b[i].getOtherAtom(sAtom);
//        if (a.mapIndex < 0)
//          return a;
//      }
//    }
    Edge[] edges = atom.getEdges();
    for (int i = edges.length; --i >= 0;) {
      int k = atom.getBondedAtomIndex(i);
      if (jmolAtoms[k].getElementNumber() == 1 && !bsFound.get(k))
        return jmolAtoms[k];
    }
    return null;
  }


}

