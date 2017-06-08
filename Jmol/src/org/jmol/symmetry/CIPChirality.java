package org.jmol.symmetry;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.V3;

import org.jmol.java.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Logger;
import org.jmol.util.SimpleEdge;
import org.jmol.util.SimpleNode;
import org.jmol.viewer.JC;

/**
 * A fully validated relatively efficient implementation of Cahn-Ingold-Prelog
 * rules for assigning R/S, M/P, and E/Z stereochemical descriptors. Based on
 * IUPAC Blue Book rules of 2013.
 * https://iupac.org/projects/project-details/?project_nr=2001-043-1-800
 * 
 * Features include:
 * 
 *  - deeply validated 
 *  
 *  - implemented in Java (Jmol) and JavaScript (JSmol)
 *  
 *  - only two Java classes; roughly 1000 lines
 *  
 *  - efficient, one-pass process for each center using a single finite digraph for all auxiliary descriptors
 *  
 *  - exhaustive processing of all 8 sequence rules (1a, 1b, 2, 3, 4a, 4b, 4c, 5)
 *  
 *  - includes R/S, r/s, M/P (axial, not planar), E/Z
 *  
 *  - covers any-length odd and even cumulenes
 *  
 *  - uses Jmol conformational SMARTS to detect atropisomers and helicenes 
 *  
 *  - covers chiral phosphorus and sulfur, including trigonal pyramidal and tetrahedral
 *  
 *  - properly treats complex combinations of R/S, M/P, and seqCis/seqTrans centers (Rule 4b)
 * 
 *  - properly treats neutral-species resonance structures using fractional atomic mass and a modified Rule 1b
 *  
 *  - implements CIP spiro rule (BB P-93.5.3.1)
 *  
 *  - detects small rings (fewer than 8 members) and removes E/Z specifications for such
 * 
 *  - detects chiral bridgehead nitrogens and E/Z imines and diazines 
 *  
 *  - reports atom descriptor along with the rule that ultimately decided it
 * 
 * Primary 236-compound Chapter-9 validation set (AY-236) provided by Andrey
 * Yerin, ACD/Labs (Moscow).
 * 
 * Mikko Vainio also supplied a 64-compound testing suite (MV-64), which is
 * available on SourceForge in the Jmol-datafiles directory.
 * (https://sourceforge.net/p/jmol/code/HEAD/tree/trunk/Jmol-datafiles/cip).
 * 
 * Additional test structures provided by John Mayfield.
 * 
 * Additional thanks to the IUPAC Blue Book Revision project, specifically
 * Karl-Heinz Hellwich for alerting me to the errata page for the 2013 IUPAC
 * specs (http://www.chem.qmul.ac.uk/iupac/bibliog/BBerrors.html), Gerry Moss
 * for discussions, Andrey Yerin for discussion and digraph checking.
 * 
 * Many thanks to the members of the BlueObelisk-Discuss group, particularly
 * Mikko Vainio, John Mayfield (aka John May), Wolf Ihlenfeldt, and Egon Willighagen, for
 * encouragement, examples, serious skepticism, and extremely helpful advice.
 * 
 * References:
 * 
 * CIP(1966) R.S. Cahn, C. Ingold, V. Prelog, Specification of Molecular
 * Chirality, Angew.Chem. Internat. Edit. 5, 385ff
 * 
 * Custer(1986) Roland H. Custer, Mathematical Statements About the Revised
 * CIP-System, MATCH, 21, 1986, 3-31
 * http://match.pmf.kg.ac.rs/electronic_versions/Match21/match21_3-31.pdf
 * 
 * Mata(1993a) Paulina Mata, Ana M. Lobo, Chris Marshall, A.Peter Johnson The
 * CIP sequence rules: Analysis and proposal for a revision, Tetrahedron:
 * Asymmetry, Volume 4, Issue 4, April 1993, Pages 657-668
 * 
 * Mata(1994) Paulina Mata, Ana M. Lobo, Chris Marshall, and A. Peter Johnson,
 * Implementation of the Cahn-Ingold-Prelog System for Stereochemical Perception
 * in the LHASA Program, J. Chem. Inf. Comput. Sci. 1994, 34, 491-504 491
 * http://pubs.acs.org/doi/abs/10.1021/ci00019a004
 * 
 * Mata(2005) Paulina Mata, Ana M. Lobo, The Cahn, Ingold and Prelog System:
 * eliminating ambiguity in the comparison of diastereomorphic and
 * enantiomorphic ligands, Tetrahedron: Asymmetry, Volume 16, Issue 13, 4 July
 * 2005, Pages 2215-2223
 * 
 * Favre(2013) Henri A Favre, Warren H Powell, Nomenclature of Organic Chemistry
 * : IUPAC Recommendations and Preferred Names 2013 DOI:10.1039/9781849733069
 * http://pubs.rsc.org/en/content/ebook/9780854041824#!divbookcontent
 * 
 * IUPAC Project: Corrections, Revisions and Extension for the Nomenclature of
 * Organic Chemistry - IUPAC Recommendations and Preferred Names 2013 (the IUPAC
 * Blue Book)
 * https://iupac.org/projects/project-details/?project_nr=2015-052-1-800
 * 
 * code history:
 * 
 * 6/8/2017 Jmol 14.18.2 removed unnecessary presort for Rule 1b
 * 
 * 5/27/17 Jmol 14.17.2 fully interfaced using SimpleNode and SimpleEdge
 * 
 * 5/27/17 Jmol 14.17.1 fully validated; simplified code; 978 lines
 * 
 * 5/17/17 Jmol 14.16.1. adds helicene M/P chirality; 959 lines validated using
 * CCDC structures HEXHEL02 HEXHEL03 HEXHEL04 ODAGOS ODAHAF
 * http://pubs.rsc.org/en/content/articlehtml/2017/CP/C6CP07552E
 * 
 * 5/14/17 Jmol 14.15.5. trimmed up and documented; no need for lone pairs; 948
 * lines
 * 
 * 5/13/17 Jmol 14.15.4. algorithm simplified; validated for mixed Rule 4b
 * systems involving auxiliary R/S, M/P, and seqCis/seqTrans; 959 lines
 * 
 * 5/06/17 validated for 236 compound set AY-236.
 * 
 * 5/02/17 validated for 161 compounds, including M/P, m/p (axial chirality for
 * biaryls and odd-number cumulenes)
 * 
 * 4/29/17 validated for 160 compounds, including M/P, m/p (axial chirality for
 * biaryls and odd-number cumulenes)
 * 
 * 4/28/17 Validated for 146 compounds, including imines and diazines, sulfur,
 * phosphorus
 * 
 * 4/27/17 Rules 3-5 preliminary version 14.15.1
 * 
 * 4/6/17 Introduced in Jmol 14.12.0; validated for Rules 1 and 2 in Jmol
 * 14.13.2; 100 lines
 * 
 * 
 * NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE! NOTE!
 * 
 * Added logic to Rule 1b: The root distance for a Kekule-ambiguous duplicate
 * atom is its own sphere, not the sphere of its duplicated atom.
 * 
 * Stated more precisely:
 * 
 * Proposed amended Rule 1:
 * 
 * (1a) higher atomic number precedes lower;
 * 
 * (1b) in comparing two duplicate nodes, lower root distance precedes higher
 * root distance, where "root distance" is defined:
 * 
 * (i) in the case of a duplicate atom for which the atomic number is averaged
 * over two or more atoms in applying Rule 1a, the distance from the duplicate
 * node itself to the root node; and
 * 
 * (ii) in all other cases, the distance of its corresponding nonduplicated atom
 * node to the root node.
 * 
 * 
 * Rationale: Using only the distance of the duplicated atom (ii) introduces a Kekule
 * bias, which is not acceptable.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class CIPChirality {

  //The rules:
  //  
  //  P-92.1.3.1 Sequence Rule 1 has two parts:
  //
  //    (a) Higher atomic number precedes lower;
  //    (b) A duplicate atom node whose corresponding nonduplicated atom
  //    node is the root or is closer to the root ranks higher than a
  //    duplicate atom node whose corresponding nonduplicated node is 
  //    farther from the root.
  //
  //  P-92.1.3.2 Sequence Rule 2
  //
  //    Higher atomic mass number precedes lower;
  //
  //  P-92.1.3.3 Sequence Rule 3
  //
  //    When considering double bonds and planar tetraligand atoms, 'seqcis' = 'Z'
  //    precedes 'seqtrans' = 'E' and this precedes nonstereogenic double bonds.
  //
  //  P-92.1.3.4 Sequence Rule 4 is best considered in three parts:
  //
  //    (a) Chiral stereogenic units precede pseudoasymmetric stereogenic units and
  //        these precede nonstereogenic units.
  //    (b) When two ligands have different descriptor pairs, then the one with the
  //        first chosen like descriptor pairs has priority over the one with a
  //        corresponding unlike descriptor pair.
  //      (i) Like descriptor pairs are: 'RR', 'SS', 'MM', 'PP', 'RM', 'SP',
  //          'seqCis/seqCis', 'seqTran/sseqTrans', 'RseqCis',
  //          'SseqTrans', 'MseqCis', 'PseqTrans' ...;
  //      (ii) Unlike discriptor pairs are 'RS', 'MP', 'RP', 'SM',
  //           'seqCis/seqTrans', 'RseqTrans', 'SseqCis', 'PseqCis',
  //           'MseqTrans'....
  //    (c) 'r' precedes 's' and 'm' precedes 'p'
  //
  //  P-92.1.3.5 Sequence Rule 5
  //
  //    An atom or group with descriptor 'R', 'M' and 'seqCis' has priority over its
  //    enantiomorph 'S', 'P' or 'seqTrans', 'seqCis' or 'seqTrans'.

  // "Scoring" a vs. b involves returning 0 (TIE) or +/-n, where n>0 indicates b won, n < 0
  // indicates a won, and the |a| indicates in which shell the decision was made. 
  // The basic strategy is to loop through the Sequential Rules 1-5, including all parts 
  // exhaustively prior to applying another rule. This includes subparts:
  //
  // Rule 1a (atomic number -- note that this requires an aromaticity check first)
  // Rule 1b (duplicated atom progenitor root-distance check; revised as described above
  //         for aromatic systems.
  // Rule 2  (nominal isotopic mass)
  // Rule 3  (E/Z not including enantiomorphic seqCis/seqTrans designations)
  // Rule 4a (chiral precedes pseudochiral precedes nonstereochemical; more of a guideline
  //         than a sequential rule)
  // Rule 4b like precedes unlike
  // Rule 4c r precedes s
  // Rule 5  R precedes S; M precedes P (final determination of pseudoasymmetry descriptors)
  //
  // Some nuances I've learned along the way here, some of which are still being checked:
  //
  // 1. Rule 1a requires a definition of aromaticity -- harder than you might think! 
  // 2. Rule 1b had to be revised to account for Kekule bias (AY-236.215). Note that this 
  //            rule may only be applied AFTER Rule 1a has been applied exhaustively. In  
  //            my mind it deserves its own number for this reason. See AY-236.53, 
  //            (1S,5R)-bicyclo[3.1.0]hex-2-ene, for example.
  // 3. Rule 2  This rule is simple to implement; must be executed only for ties from 1a and 1b.  
  // 4. Rule 3  requires the concept of "auxiliary" (temporary, digraph-specific) descriptors.
  //            This concept of auxiliary descriptors is the key to not having an analysis
  //            blow up or somehow require complex, impossible iteration.
  // 5. Rule 4a needs to be addressed exhaustively prior to Rules 4b and 4c. This involves the
  //            the initial generation of all auxiliary descriptors, including r/s and R/S at
  //            branching points. In the course of doing this, all rules, 1-5, must be employed
  //            at these auxiliary centers using the already-created digraph. This rule serves to
  //            avoid the need for Rule 4b for all except the most unusual cases, where, for example,
  //            there are two otherwise identical branches, but one can be identified as S and the
  //            other only r or no-stereo, but not R. Thus, only branches that end up as R/R, R/S, S/S,
  //            r/r, r/s, s/s, or nst/nst need be investigated by Rule 4b.  
  // 6. Rule 4b Somehow missed in the discussion is that the reference descriptor is determined
  //            once and only once for each branch from the center under scrutiny. All Rules 
  //            preceding Rule 4 can be applied to iterated subsections of a digraph. Not this one,
  //            nor Rule 5, though. The key is to determine one single "Mata sequence" of R and S descriptors
  //            for each pair of branches being considered. This same reference carries through all  
  //            future iterations of the algorithm for that branch.
  // 7. Rule 4c Again, this subrule must be invoked only after Rule 4b is completed, and again
  //            it is only for the root branches, not anywhere else.
  // 8. Rule 5  Final setting pseudoasymmetry (r/s, m/p) can be done along the same line as Rule 4b,
  //            but with slightly different sorting criteria.
  //

  // The algorithm:
  // 
  //
  //  getChirality(molecule) {
  //    prefilterAtoms()
  //    checkForAlkenes()
  //    checkForSmallRings()
  //    checkForHelicenes()
  //    checkForBridgeheadNitrogens()
  //    checkForKekuleIssues()
  //    checkForAtropisomerism()
  //    for(all filtered atoms) getAtomChirality(atom)
  //    if (haveAlkenes) {
  //      for(all double bonds) getBondChirality(a1, a2)
  //      removeUnnecessaryEZDesignations()
  //      indicateHeliceneChirality()
  //    }
  //  }
  //
  // getAtomChirality(atom) {
  //   for (each Rule){  
  //     sortSubstituents() 
  //     if (done) return checkHandedness();
  //   }
  //   return NO_CHIRALITY
  // }
  // 
  //  getBondChirality(a1, a2) {
  //    atop = getAlkeneEndTopPriority(a1)
  //    btop = getAlkeneEndTopPriority(a2)
  //    return (atop >= 0 && btop >= 0 ? getEneChirality(atop, a1, a2, btop) : NO_CHIRALITY)
  //  }
  //
  // sortSubstituents() {
  //   for (all pairs of substituents a1 and a2) {
  //     score = a1.compareTo(a2, currentRule)
  //     if (score == TIED) 
  //       score = breakTie(a1,a2)
  // }
  // 
  // breakTie(a,b) { 
  //    score = compareShallowly(a, b)
  //    if (score != TIED) return score
  //    a.sortSubstituents(), b.sortSubstituents()
  //    return compareDeeply(a, b)
  // }
  // 
  // compareShallowly(a, b) {
  //    for (each substituent pairing i in a and b) {
  //      score = applyCurrentRule(a_i, b_i)
  //      if (score != TIED) return score
  //    }
  //    return TIED
  // }
  //
  // compareDeeply(a, b) {
  //    bestScore = Integer.MAX_VALUE
  //    for (each substituent pairing i in a and b) {
  //      bestScore = min(bestScore, breakTie(a_i, b_i))
  //    }
  //    return bestScore
  // }
  //
  // Of course, the actual code is a bit more complex than that.

  static final int NO_CHIRALITY = 0;
  static final int TIED = NO_CHIRALITY;
  static final int B_WINS = 1;
  static final int A_WINS = -1;
  static final int DIASTEREOMERIC = -3;
  static final int DIASTEREOMERIC_A_WINS = -2;
  static final int DIASTEREOMERIC_B_WINS = 2;
  static final int IGNORE = Integer.MIN_VALUE;
  static final int NOT_RELEVANT = Integer.MIN_VALUE;

  static final int STEREO_UNDETERMINED = -1;

  static final int STEREO_R = JC.CIP_CHIRALITY_R_FLAG;
  static final int STEREO_S = JC.CIP_CHIRALITY_S_FLAG;

  static final int STEREO_M = JC.CIP_CHIRALITY_M_FLAG;
  static final int STEREO_P = JC.CIP_CHIRALITY_P_FLAG;

  static final int STEREO_Z = JC.CIP_CHIRALITY_Z_FLAG;
  static final int STEREO_E = JC.CIP_CHIRALITY_E_FLAG;

  static final int STEREO_BOTH_RS = STEREO_R | STEREO_S; // must be the number 3
  static final int STEREO_BOTH_EZ = STEREO_E | STEREO_Z;

  static final int RULE_1a = 1;
  static final int RULE_1b = 2;
  static final int RULE_2 = 3;
  static final int RULE_3 = 4;
  static final int RULE_4a = 5;
  static final int RULE_4b = 6;
  static final int RULE_4c = 7;
  static final int RULE_5 = 8;

  public String getRuleName() {
    return JC.getCIPRuleName(currentRule);
  }

  /**
   * measure of planarity in a trigonal system, in Angstroms
   * 
   */
  static final float TRIGONALITY_MIN = 0.2f;

  /**
   * maximum path to display for debugging only using SET DEBUG in Jmol
   */
  static final int MAX_PATH = 50;

  /**
   * maximum ring size that can have a double bond with no E/Z designation; also
   * used for identifying aromatic rings and bridgehead nitrogens
   */
  static final int SMALL_RING_MAX = 7;

  /**
   * incremental pointer providing a unique ID to every CIPAtom for debugging
   * 
   */
  int ptID;

  /**
   * The atom for which we are determining the stereochemistry
   * 
   */
  CIPAtom root;

  /**
   * The current rule being applied exhaustively.
   * 
   */
  int currentRule = RULE_1a;

  /**
   * track small rings for removing E/Z indicators as per IUPAC
   * 2013.P-93.5.1.4.1
   */
  Lst<BS> lstSmallRings = new Lst<BS>();

  /**
   * the aromatic atoms connected by a nonaromatic single bond smarts("a-a")
   * 
   */
  BS bsAtropisomeric;

  /**
   * needed for Jmol's Rule 1b addition
   * 
   */
  BS bsKekuleAmbiguous;

  /**
   * used to determine whether N is potentially chiral - could do this here, of
   * course.... see AY-236.203
   */
  BS bsAzacyclic;

  // temporary fields

  V3 vNorm = new V3(), vNorm2 = new V3(), vTemp = new V3();

  boolean allowRule1bAlkenes = true;

  public CIPChirality() {
    // for reflection
    System.out.println("TESTING ALLOWRULE1B");
    allowRule1bAlkenes = false;
  }

  /**
   * Initialize for a new molecular determination.
   * 
   */
  private void init() {
    ptID = 0;
    lstSmallRings.clear();
    bsKekuleAmbiguous = null;
    bsAtropisomeric = new BS();
  }

  /**
   * A more general determination of chirality that involves ultimately all
   * Rules 1-5.
   * 
   * @param atoms
   *        atoms to process
   * @param bsAtoms
   *        bit set of all atoms to process
   * @param bsAtropisomeric
   *        bit set of all biphenyl-like connections
   * @param bsHelixM
   *        aromatic atoms at the end of a negative helical turn;
   *        smarts("A{a}(.t:-10,-40)a(.t:-10,-40)aaa")
   * @param bsHelixP
   *        aromatic atoms at the end of a positive helical turn;
   *        smarts("A{a}(.t:10,40)a(.t:10,40)aaa")
   */
  public void getChiralityForAtoms(SimpleNode[] atoms, BS bsAtoms,
                                   BS bsAtropisomeric, BS bsHelixM, BS bsHelixP) {
    if (bsAtoms.isEmpty())
      return;
    init();
    this.bsAtropisomeric = (bsAtropisomeric == null ? new BS()
        : bsAtropisomeric);

    // using BSAtoms here because we need the entire graph,
    // including multiple molecular units (AY-236.93

    BS bs = BSUtil.copy(bsAtoms);
    lstSmallRings = new Lst<BS>();
    while (!bs.isEmpty())
      getSmallRings(atoms[bs.nextSetBit(0)], bs);
    bsKekuleAmbiguous = getKekule(atoms);
    bsAzacyclic = getAzacyclic(atoms, bsAtoms);

    BS bsToDo = BSUtil.copy(bsAtoms);
    boolean haveAlkenes = preFilterAtomList(atoms, bsToDo);

    // set atom chiralities

    for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1)) {
      SimpleNode a = atoms[i];
      a.setCIPChirality(0);
      ptID = 0;
      int c = getAtomChiralityLimited(a, null, null, RULE_5);
      a.setCIPChirality(c == 0 ? JC.CIP_CHIRALITY_NONE : c
          | ((currentRule - 1) << JC.CIP_CHIRALITY_NAME_OFFSET));
    }
    if (haveAlkenes) {

      // set bond chiralities E/Z and M/P

      Lst<int[]> lstEZ = new Lst<int[]>();
      for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1))
        getAtomBondChirality(atoms[i], lstEZ, bsToDo);
      if (lstSmallRings.size() > 0 && lstEZ.size() > 0)
        clearSmallRingEZ(atoms, lstEZ);

      // Add helicene chiralities -- predetermined using a Jmol SMARTS conformational search.
      //
      // M: A{a}(.t:-10,-40)a(.t:-10,-40)aaa
      // P: A{a}(.t:10,40)a(.t:10,40)aaa
      //
      // Note that indicators are on the first and last aromatic atoms {a}. 

      if (bsHelixM != null)
        for (int i = bsHelixM.nextSetBit(0); i >= 0; i = bsHelixM
            .nextSetBit(i + 1))
          atoms[i].setCIPChirality(STEREO_M);

      if (bsHelixP != null)
        for (int i = bsHelixP.nextSetBit(0); i >= 0; i = bsHelixP
            .nextSetBit(i + 1))
          atoms[i].setCIPChirality(STEREO_P);
    }

    if (Logger.debugging) {
      Logger.info("sp2-aromatic = " + bsKekuleAmbiguous);
      Logger.info("smallRings = " + PT.toJSON(null, lstSmallRings));
    }

  }

  /**
   * Identify bridgehead nitrogens, as these may need to be given chirality
   * designations. See AY-236.203 P-93.5.4.1
   * 
   * @param atoms
   * 
   * @param bsAtoms
   * @return a bit set of bridgehead nitrogens. I just liked the name
   *         "azacyclic".
   */
  private BS getAzacyclic(SimpleNode[] atoms, BS bsAtoms) {
    BS bsAza = null;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      SimpleNode atom = atoms[i];
      if (atom.getElementNumber() != 7 || atom.getCovalentBondCount() != 3
          || bsKekuleAmbiguous.get(i))
        continue;
      // bridgehead N must be in two rings that have at least three atoms in common.
      Lst<BS> nRings = new Lst<BS>();
      for (int j = lstSmallRings.size(); --j >= 0;) {
        BS bsRing = lstSmallRings.get(j);
        if (bsRing.get(i))
          nRings.addLast(bsRing);
      }
      int nr = nRings.size();
      if (nr < 2)
        continue;
      BS bsSubs = new BS();
      SimpleEdge[] bonds = atom.getEdges();
      for (int b = bonds.length; --b >= 0;)
        if (bonds[b].isCovalent())
          bsSubs.set(bonds[b].getOtherNode(atom).getIndex());
      BS bsBoth = new BS();
      BS bsAll = new BS();
      for (int j = 0; j < nr - 1 && bsAll != null; j++) {
        BS bs1 = nRings.get(j);
        for (int k = j + 1; k < nr && bsAll != null; k++) {
          BS bs2 = nRings.get(k);
          BSUtil.copy2(bs1, bsBoth);
          bsBoth.and(bs2);
          if (bsBoth.cardinality() > 2) {
            BSUtil.copy2(bs1, bsAll);
            bsAll.or(bs2);
            bsAll.and(bsSubs);
            if (bsAll.cardinality() == 3) {
              if (bsAza == null)
                bsAza = new BS();
              bsAza.set(i);
              bsAll = null;
            }
          }
        }
      }
    }
    return bsAza;
  }

  /**
   * Remove unnecessary atoms from the list and let us know if we have alkenes
   * to consider.
   * 
   * @param atoms
   * @param bsToDo
   * @return whether we have any alkenes that could be EZ
   */
  private boolean preFilterAtomList(SimpleNode[] atoms, BS bsToDo) {
    boolean haveAlkenes = false;
    for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1)) {
      if (!couldBeChiralAtom(atoms[i])) {
        bsToDo.clear(i);
        continue;
      }
      if (!haveAlkenes
          && couldBeChiralAlkene(atoms[i], null) != STEREO_UNDETERMINED)
        // do Rule 3, and check for rings that in the end should force removal of E/Z designations
        haveAlkenes = true;
    }
    return haveAlkenes;
  }

  /**
   * Determine whether an atom is one we need to consider.
   * 
   * @param a
   * @return true for selected atoms and hybridizations
   * 
   */
  private boolean couldBeChiralAtom(SimpleNode a) {
    boolean mustBePlanar = false;
    switch (a.getCovalentBondCount()) {
    default:
      System.out.println("???? too many bonds! " + a);
      return false;
    case 0:
      return false;
    case 1:
      return false;
    case 2:
      return a.getElementNumber() == 7; // could be diazine or imine
    case 3:
      switch (a.getElementNumber()) {
      case 7: // N
        if (bsAzacyclic != null && bsAzacyclic.get(a.getIndex()))
          break;
        return false;
      case 6: // C
        mustBePlanar = true;
        break;
      case 15: // P
      case 16: // S
      case 33: // As
      case 34: // Se
      case 51: // Sb
      case 52: // Te
      case 83: // Bi
      case 84: // Po
        break;
      case 4:
        break;
      default:
        return false;
      }
      break;
    case 4:
      break;
    }
    // check that the atom has at most one 1H atom and whether it must be planar and has a double bond
    SimpleEdge[] edges = a.getEdges();
    int nH = 0;
    boolean haveDouble = false;
    for (int j = edges.length; --j >= 0;) {
      if (mustBePlanar && edges[j].getCovalentOrder() == 2)
        haveDouble = true;
      if (edges[j].getOtherNode(a).getIsotopeNumber() == 1)
        nH++;
    }
    return (nH < 2 && (haveDouble || mustBePlanar == Math.abs(getTrigonality(a,
        vNorm)) < TRIGONALITY_MIN));
  }

  /**
   * Allow double bonds only if trivalent and first-row atom. (IUPAC
   * 2013.P-93.2.4) Currently: a) first row b) doubly bonded c) doubly bonded
   * atom is also first row
   * 
   * @param a
   * @param b
   *        optional other atom
   * @return if the atom could be an EZ node
   */
  private int couldBeChiralAlkene(SimpleNode a, SimpleNode b) {
    switch (a.getCovalentBondCount()) {
    default:
      return STEREO_UNDETERMINED;
    case 2:
      // imines and diazines
      if (a.getElementNumber() != 7) // nitrogen
        return STEREO_UNDETERMINED;
      break;
    case 3:
      // first-row only (IUPAC 2013.P-93.2.4)
      if (!isFirstRow(a))
        return STEREO_UNDETERMINED;
      break;
    }
    SimpleEdge[] bonds = a.getEdges();
    int n = 0;
    for (int i = bonds.length; --i >= 0;)
      if (bonds[i].getCovalentOrder() == 2) {
        if (++n > 1)
          return STEREO_M; //central allenes
        SimpleNode other = bonds[i].getOtherNode(a);
        if (!isFirstRow(other))
          return STEREO_UNDETERMINED;
        if (b != null && (other != b || b.getCovalentBondCount() == 1)) {
          // could be allene central, but I think this is not necessary
          return STEREO_UNDETERMINED;
        }
      }
    return STEREO_Z;
  }

  /**
   * Check if an atom is 1st row.
   * 
   * @param a
   * @return elemno > 2 && elemno <= 10
   */
  boolean isFirstRow(SimpleNode a) {
    int n = a.getElementNumber();
    return (n > 2 && n <= 10);
  }

  /**
   * Just six-membered rings with three internal pi bonds or fused rings such as
   * naphthalene or anthracene. Obviously, this is not a full-fledged Kekule
   * check, but it will have to do.
   * 
   * @param atoms
   * @return bsKekuleAmbiguous
   */
  private BS getKekule(SimpleNode[] atoms) {
    int nRings = lstSmallRings.size();
    BS bs = new BS(), bsDone = new BS();
    for (int i = nRings; --i >= 0;) {
      if (bsDone.get(i))
        continue;
      BS bsRing = lstSmallRings.get(i);
      if (bsRing.cardinality() != 6) {
        bsDone.set(i);
        continue;
      }
      int nPI = 0;
      for (int j = bsRing.nextSetBit(0); j >= 0; j = bsRing.nextSetBit(j + 1)) {
        SimpleNode a = atoms[j];
        if (bs.get(a.getIndex())) {
          nPI++;
          continue;
        }
        int nb = a.getCovalentBondCount();
        if (nb == 3 || nb == 2) {
          SimpleEdge[] bonds = a.getEdges();
          for (int k = bonds.length; --k >= 0;) {
            SimpleEdge b = bonds[k];
            if (b.getCovalentOrder() != 2)
              continue;
            if (bsRing.get(b.getOtherNode(a).getIndex())) {
              nPI++;
              break;
            }
          }
        }
      }
      if (nPI == 6) {
        bs.or(bsRing);
        bsDone.set(i);
        i = nRings;
      }
    }
    return bs;
  }

  /**
   * Run through a minimal graph to find and catalog all rings.
   * 
   * @param atom
   * @param bs
   *        tracks all atoms in this molecular unit
   */
  private void getSmallRings(SimpleNode atom, BS bs) {
    root = new CIPAtom().create(atom, null, false, false);
    addSmallRings(root, bs);
  }

  /**
   * initiate a new CIPAtom for each substituent of atom, and as part of this
   * process, check to see if a new ring is being formed.
   * 
   * @param a
   * @param bs
   */
  private void addSmallRings(CIPAtom a, BS bs) {
    if (a == null || a.atom == null || a.sphere > SMALL_RING_MAX)
      return;
    if (bs != null)
      bs.clear(a.atom.getIndex());
    if (a.isTerminal || a.isDuplicate || a.atom.getCovalentBondCount() > 4)
      return;
    SimpleNode atom2;
    int pt = 0;
    SimpleEdge[] bonds = a.atom.getEdges();
    for (int i = bonds.length; --i >= 0;) {
      SimpleEdge bond = bonds[i];
      if (!bond.isCovalent()
          || (atom2 = bond.getOtherNode(a.atom)).getCovalentBondCount() == 1
          || a.parent != null && atom2 == a.parent.atom)
        continue;
      CIPAtom r = a.addAtom(pt++, atom2, false, false);
      if (r.isDuplicate)
        r.updateRingList();
    }
    for (int i = 0; i < pt; i++) {
      addSmallRings(a.atoms[i], bs);
    }
  }

  /**
   * Remove E/Z designations for small-rings double bonds (IUPAC
   * 2013.P-93.5.1.4.1).
   * 
   * @param atoms
   * @param lstEZ
   */
  private void clearSmallRingEZ(SimpleNode[] atoms, Lst<int[]> lstEZ) {
    for (int j = lstSmallRings.size(); --j >= 0;)
      lstSmallRings.get(j).andNot(bsAtropisomeric);
    for (int i = lstEZ.size(); --i >= 0;) {
      int[] ab = lstEZ.get(i);
      for (int j = lstSmallRings.size(); --j >= 0;) {
        BS ring = lstSmallRings.get(j);
        if (ring.get(ab[0]) && ring.get(ab[1])) {
          atoms[ab[0]].setCIPChirality(JC.CIP_CHIRALITY_NONE);
          atoms[ab[1]].setCIPChirality(JC.CIP_CHIRALITY_NONE);
        }
      }
    }
  }

  /**
   * Determine the trigonality of an atom in order to determine whether it might
   * have a lone pair. The global vector vNorm is returned as well, pointing
   * from the atom to the base plane of its first three substituents.
   * 
   * @param a
   * @param vNorm
   *        a vector returned with the normal from the atom to the base plane
   * @return distance from plane of first three covalently bonded nodes to this
   *         node
   */
  float getTrigonality(SimpleNode a, V3 vNorm) {
    P3[] pts = new P3[4];
    SimpleEdge[] bonds = a.getEdges();
    for (int n = bonds.length, i = n, pt = 0; --i >= 0 && pt < 4;)
      if (bonds[i].isCovalent())
        pts[pt++] = bonds[i].getOtherNode(a).getXYZ();
    P4 plane = Measure.getPlaneThroughPoints(pts[0], pts[1], pts[2], vNorm,
        vTemp, new P4());
    return Measure.distanceToPlane(plane, (pts[3] == null ? a.getXYZ() : pts[3]));
  }

  /**
   * Get E/Z characteristics for specific atoms. Also check here for
   * atropisomeric M/P designations
   * 
   * @param atom
   * @param lstEZ
   * @param bsToDo
   */

  private void getAtomBondChirality(SimpleNode atom, Lst<int[]> lstEZ, BS bsToDo) {
    int index = atom.getIndex();
    SimpleEdge[] bonds = atom.getEdges();
    int c = NO_CHIRALITY;
    boolean isAtropic = bsAtropisomeric.get(index);
    for (int j = bonds.length; --j >= 0;) {
      SimpleEdge bond = bonds[j];
      SimpleNode atom1;
      int index1;
      if (isAtropic) {
        atom1 = bonds[j].getOtherNode(atom);
        index1 = atom1.getIndex();
        if (!bsAtropisomeric.get(index1))
          continue;
        c = setBondChirality(atom, atom1, atom, atom1, true, RULE_5);
      } else if (bond.getCovalentOrder() == 2) {
        atom1 = getLastCumuleneAtom(bond, atom, null, null);
        index1 = atom1.getIndex();
        if (index1 < index)
          continue;
        c = getBondChiralityLimited(bond, atom, RULE_5);
      } else {
        continue;
      }
      if (c != NO_CHIRALITY) {
        if (!isAtropic)
          lstEZ.addLast(new int[] { index, index1 });
        bsToDo.clear(index);
        bsToDo.clear(index1);
      }
      if (isAtropic)
        break;
    }
  }

  /**
   * 
   * @param bond
   * @param atom
   * @param nSP2
   *        returns the number of sp2 carbons in this alkene or cumulene
   * @param parents
   * @return the terminal atom of this alkene or cumulene
   */
  private SimpleNode getLastCumuleneAtom(SimpleEdge bond, SimpleNode atom, int[] nSP2,
                                   SimpleNode[] parents) {
    // we know this is a double bond
    SimpleNode atom2 = bond.getOtherNode(atom);
    if (parents != null) {
      parents[0] = atom2;
      parents[1] = atom;
    }
    // connected atom must have only two covalent bonds
    if (nSP2 != null)
      nSP2[0] = 2;
    int ppt = 0;
    while (true) {
      if (atom2.getCovalentBondCount() != 2)
        return atom2;
      SimpleEdge[] edges = atom2.getEdges();
      for (int i = edges.length; --i >= 0;) {
        SimpleNode atom3 = (bond = edges[i]).getOtherNode(atom2);
        if (atom3 == atom)
          continue;
        // connected atom must only have one other bond, and it must be double to continue
        if (bond.getCovalentOrder() != 2)
          return atom2; // was atom3
        if (parents != null) {
          if (ppt == 0) {
            parents[0] = atom2;
            ppt = 1;
          }
          parents[1] = atom2;
        }
        // a=2=3
        if (nSP2 != null)
          nSP2[0]++;
        atom = atom2;
        atom2 = atom3;
        // we know we only have two covalent bonds
        break;
      }
    }
  }

  /**
   * Determine R/S or one half of E/Z determination
   * 
   * @param atom
   *        ignored if a is not null
   * @param cipAtom
   *        ignored if atom is not null
   * @param parent
   *        null for tetrahedral, other alkene carbon for E/Z
   * @param ruleMax
   *        allows limiting this to Rule 3
   * 
   * @return if parent != null: [0:none, 1:R, 2:S] otherwise [0:none, 1:
   *         atoms[0] is higher, 2: atoms[1] is higher]
   */
  private int getAtomChiralityLimited(SimpleNode atom, CIPAtom cipAtom,
                                      CIPAtom parent, int ruleMax) {
    int rs = NO_CHIRALITY;
    boolean isChiral = false, isAlkene = false;
    try {
      if (cipAtom == null) {
        cipAtom = new CIPAtom().create(atom, null, false, isAlkene);
        int nSubs = atom.getCovalentBondCount(), elemNo = atom.getElementNumber();
        isAlkene = (nSubs == 3 && elemNo <= 10 && !cipAtom.isTrigonalPyramidal); // (IUPAC 2013.P-93.2.4)
        if (nSubs != (parent == null ? 4 : 3)
            - (nSubs == 3 && !isAlkene ? 1 : 0))
          return rs;
      } else {
        atom = cipAtom.atom;
        isAlkene = cipAtom.isAlkene;
      }
      root = cipAtom;
      cipAtom.parent = parent;
      if (parent != null)
        cipAtom.htPathPoints = parent.htPathPoints;
      currentRule = RULE_1a;
      if (cipAtom.setNode()) {
        for (currentRule = RULE_1a; currentRule <= ruleMax; currentRule++) {
          if (Logger.debugging)
            Logger.info("-Rule " + getRuleName() + " CIPChirality for "
                + cipAtom + "-----"); // Logger
          if (currentRule == RULE_4a)
            cipAtom.createAuxiliaryRule4Data(null, null);
          isChiral = cipAtom.sortSubstituents(0);
          if (Logger.debugging) {
            Logger.info(currentRule + ">>>>" + cipAtom);
            for (int i = 0; i < cipAtom.bondCount; i++) { // Logger
              if (cipAtom.atoms[i] != null) // Logger
                Logger.info(cipAtom.atoms[i] + " "
                    + Integer.toHexString(cipAtom.priorities[i])); // Logger
            }
          }
          if (cipAtom.achiral) {
            isChiral = false;
            break;
          }
          if (currentRule == RULE_5)
            cipAtom.isPseudo = cipAtom.canBePseudo;
          if (isChiral) {
            rs = (!isAlkene ? cipAtom.checkHandedness()
                : cipAtom.atoms[0].isDuplicate ? STEREO_S : STEREO_R);
            if (!isAlkene && cipAtom.isPseudo && cipAtom.canBePseudo) {
              currentRule = RULE_5;
              rs = rs | JC.CIP_CHIRALITY_PSEUDO_FLAG;
            }
            if (Logger.debugging)
              Logger.info(atom + " " + JC.getCIPChiralityName(rs) + " by Rule "
                  + getRuleName() + "\n----------------------------------");
            break;
          }
        }
      }
    } catch (Throwable e) {
      System.out.println(e + " in CIPChirality");
      /**
       * @j2sNative alert(e);
       */
      {
        e.printStackTrace();
      }
      return STEREO_BOTH_RS;
    }
    return rs;
  }

  /**
   * Determine the axial or E/Z chirality for this bond, with the given starting
   * atom a
   * 
   * @param bond
   * @param a
   *        first atom to consider, or null
   * @param ruleMax
   * @return one of: {NO_CHIRALITY | STEREO_Z | STEREO_E | STEREO_Ra | STEREO_Sa
   *         | STEREO_ra | STEREO_sa}
   */
  private int getBondChiralityLimited(SimpleEdge bond, SimpleNode a, int ruleMax) {
    if (Logger.debugging)
      Logger.info("get Bond Chirality " + bond);
    if (a == null)
      a = bond.getOtherNode(null);
    if (couldBeChiralAlkene(a, bond.getOtherNode(a)) == STEREO_UNDETERMINED)
      return NO_CHIRALITY;
    int[] nSP2 = new int[1];
    SimpleNode[] parents = new SimpleNode[2];
    SimpleNode b = getLastCumuleneAtom(bond, a, nSP2, parents);
    boolean isAxial = nSP2[0] % 2 == 1;
    return setBondChirality(a, parents[0], parents[1], b, isAxial, ruleMax);
  }

  /**
   * Determine the axial or E/Z chirality for the a-b bond.
   * 
   * @param a
   * @param pa
   * @param pb
   * @param b
   * @param isAxial
   * @param ruleMax
   * @return one of: {NO_CHIRALITY | STEREO_Z | STEREO_E | STEREO_M | STEREO_P |
   *         STEREO_m | STEREO_p}
   */
  private int setBondChirality(SimpleNode a, SimpleNode pa, SimpleNode pb, SimpleNode b,
                               boolean isAxial, int ruleMax) {
    CIPAtom a1 = new CIPAtom().create(a, null, false, true);
    int atop = getAlkeneEndTopPriority(a1, pa, isAxial, ruleMax);
    int ruleA = currentRule;
    CIPAtom b2 = new CIPAtom().create(b, null, false, true);
    int btop = getAlkeneEndTopPriority(b2, pb, isAxial, ruleMax);
    int ruleB = currentRule;
    int c = (atop >= 0 && btop >= 0 ? getEneChirality(b2.atoms[btop], b2, a1,
        a1.atoms[atop], isAxial, true) : NO_CHIRALITY);
    if (c != NO_CHIRALITY
        && (isAxial || !bsAtropisomeric.get(a.getIndex())
            && !bsAtropisomeric.get(b.getIndex()))) {
      a.setCIPChirality(c | ((ruleA - 1) << JC.CIP_CHIRALITY_NAME_OFFSET));
      b.setCIPChirality(c | ((ruleB - 1) << JC.CIP_CHIRALITY_NAME_OFFSET));
      if (Logger.debugging)
        Logger.info(a + "-" + b + " " + JC.getCIPChiralityName(c));
    }
    return c;
  }

  /**
   * Determine the stereochemistry of a bond
   * 
   * @param top1
   * @param end1
   * @param end2
   * @param top2
   * @param isAxial
   *        if an odd-cumulene
   * @param allowPseudo
   *        if we are working from a high-level bond stereochemistry method
   * @return STEREO_M, STEREO_P, STEREO_Z, STEREO_E, STEREO_m, STEREO_p or
   *         NO_CHIRALITY
   */
  int getEneChirality(CIPAtom top1, CIPAtom end1, CIPAtom end2, CIPAtom top2,
                      boolean isAxial, boolean allowPseudo) {
    return (top1 == null || top2 == null || top1.atom == null
        || top2.atom == null ? NO_CHIRALITY
        : isAxial ? (isPos(top1, end1, end2, top2) ? STEREO_P : STEREO_M)
            | (allowPseudo && (end2.ties == null) != (end1.ties == null) ? JC.CIP_CHIRALITY_PSEUDO_FLAG
                : 0)
            : (isCis(top1, end1, end2, top2) ? STEREO_Z : STEREO_E));
  }

  /**
   * Alkene end check for final bond chirality, not auxiliary.
   * 
   * @param a
   * @param pa
   * @param isAxial
   * @param ruleMax
   * @return -1, 0, or 1
   */
  private int getAlkeneEndTopPriority(CIPAtom a, SimpleNode pa, boolean isAxial,
                                      int ruleMax) {
    a.canBePseudo = a.isAxialRoot = isAxial;
    return getAtomChiralityLimited(a.atom, a,
        new CIPAtom().create(pa, null, false, true), ruleMax) - 1;
  }

  /**
   * Check cis vs. trans nature of a--b==c--d.
   * 
   * @param a
   * @param b
   * @param c
   * @param d
   * @return true if this is a cis relationship
   */
  boolean isCis(CIPAtom a, CIPAtom b, CIPAtom c, CIPAtom d) {
    Measure.getNormalThroughPoints(a.atom.getXYZ(), b.atom.getXYZ(),
        c.atom.getXYZ(), vNorm, vTemp);
    V3 vNorm2 = new V3();
    Measure.getNormalThroughPoints(b.atom.getXYZ(), c.atom.getXYZ(),
        d.atom.getXYZ(), vNorm2, vTemp);
    return (vNorm.dot(vNorm2) > 0);
  }

  /**
   * Checks the torsion angle and returns true if it is positive
   * 
   * @param a
   * @param b
   * @param c
   * @param d
   * @return true if torsion angle is
   */
  boolean isPos(CIPAtom a, CIPAtom b, CIPAtom c, CIPAtom d) {
    float angle = Measure.computeTorsion(a.atom.getXYZ(), b.atom.getXYZ(),
        c.atom.getXYZ(), d.atom.getXYZ(), true);
    return (angle > 0);
  }

  private class CIPAtom implements Comparable<CIPAtom>, Cloneable {

    /**
     * the associated Jmol (or otherwise). Use of Node allows us to implement
     * this in SMILES or Jmol
     * 
     */
    SimpleNode atom;

    /**
     * unique ID for this CIPAtom for debugging only
     * 
     */
    private int id;

    /**
     * direct ancestor of this atom
     * 
     */
    CIPAtom parent;

    /**
     * first atom in this atom's root branch
     */
    private CIPAtom rootSubstituent;

    /**
     * Rule 1 nominal element number; may be fractional for Kekule issues
     */
    float elemNo;

    /**
     * Rule 2 nominal atomic mass; may be a rounded value so that 12C is the
     * same as C
     */
    int massNo;

    /**
     * bond distance from the root atom to this atom
     */
    int sphere;

    /**
     * the array of indices of the associated atoms in the path to this atom
     * node from the root; used to keep track of the path to this atom in order
     * to prevent infinite cycling; the last atom in the path when cyclic is a
     * duplicate atom.
     * 
     */
    BS bsPath;

    /**
     * String path, for debugging only
     * 
     */
    private String myPath = "";

    /**
     * Rule 1b measure: Distance from root of the corresponding nonduplicated
     * atom (duplicate nodes only).
     * 
     * AMENDED HERE for duplicate nodes associated with a double-bond in a
     * 6-membered-ring benzenoid (benzene, naphthalene, pyridine, pyrazoline,
     * etc.) "Kekule-ambiguous" system to be its sphere.
     * 
     */

    private int rootDistance;

    /**
     * number of substituent atoms (non-null atoms[] entries)
     */
    private int nAtoms;

    /**
     * the number of distinct priorities determined for this atom for the
     * current rule
     */
    private int nPriorities;

    /**
     * a count of how many 1H atoms we have found on this atom; used to halt
     * further processing of this atom
     */
    private int h1Count;

    /**
     * auxiliary chirality as determined in createAuxiliaryRule4Data
     */
    private String auxChirality = "~";

    /**
     * a flag to prevent finalization of an atom node more than once
     * 
     */
    private boolean isSet;

    /**
     * a flag to indicate atom that is a duplicate of another, either due to
     * ring closure or multiple bonding -- element number and mass, but no
     * substituents; slightly lower in priority than standard atoms.
     * 
     */
    boolean isDuplicate = true;

    /**
     * a flag to indicate an atom that has no substituents; a branch end point;
     * typically H or a halogen (F, Cl, Br, I)
     * 
     */
    boolean isTerminal;

    /**
     * is one atom of a double bond
     */

    boolean isAlkene;

    /**
     * first atom of an alkene or cumulene atom
     */

    CIPAtom alkeneParent;

    /**
     * last atom of an alkene or cumulene atom
     */

    CIPAtom alkeneChild;

    /**
     * last atom of a root-spiro
     */

    CIPAtom spiroEnd;

    /**
     * a flag used in Rule 3 to indicate the second carbon of a double bond
     */

    private boolean isAlkeneAtom2;

    /**
     * temporary check for pseudochirality
     */
    boolean doCheckPseudo;

    /**
     * permanent check for pseudochirality
     */
    boolean isPseudo;

    /**
     * Force achiral condition due to two identical ligands after Rule 4 check.
     */
    boolean achiral;

    /**
     * true atom covalent bond count
     */
    int bondCount;

    /**
     * the substituents -- up to four supported here at this time
     * 
     */
    CIPAtom[] atoms = new CIPAtom[4];

    /**
     * priorities associated with each subsituent from high (0) to low (3); due
     * to equivaliencies at a given rule level, these numbers may duplicate and
     * have gaps - for example, [0 2 0 3]
     */
    int[] priorities = new int[4];

    /**
     * a list that tracks stereochemical paths for Mata analysis
     * 
     */
    private String[] rule4List;

    /**
     * the application-assigned unique atom index for this atom; used in
     * updating lstSmallRings
     * 
     */
    private int atomIndex;

    /**
     * already-determined auxiliary chirality (E/Z, R/S, etc) for this atom
     * node; this value must be cleared after Rule 3 if continuing
     */
    private int auxEZ = STEREO_UNDETERMINED;

    /**
     * a flag set false in Mata analysis  
     */
    boolean canBePseudo = true;

    /**
     * used to determine pseudochirality
     */
    Lst<int[]> ties;

    /**
     * used to defer pseudochirality check
     */
    boolean isAxialRoot;

    /**
     * first =X= atom in a string of =X=X=X=...
     */
    private CIPAtom nextSP2;

    /**
     * points to next branching point that has two or more chiral branches 
     */
    private CIPAtom nextChiralBranch;

    /**
     * [sphere, nR, nS] -- tracks the number of R and S centers for the lowest sphere
     */
    private int[] rule4Count;

    /**
     * 
     */
    private int priority;

    /**
     * Rule 1b hash table that maintains distance of the associated
     * nonduplicated atom node
     * 
     */
    Map<Integer, Integer> htPathPoints;

    boolean isTrigonalPyramidal;

    /**
     * is an atom that is involved in more than one Kekule form
     */
    boolean isKekuleAmbiguous;

    /**
     * potentially useful information that this duplicate is from an double- or
     * triple-bond, not a ring closure
     */
    boolean sp2Duplicate;

    /**
     * alkene or even cumulene, so chirality will be EZ, not MP
     */
    private boolean isEvenEne = true;

    CIPAtom() {
      // had a problem in JavaScript that the constructor of an inner function cannot
      // access this.b$ yet. That assignment is made after construction.
    }

    /**
     * 
     * @param atom
     *        or null to indicate a null placeholder
     * @param parent
     * @param isDuplicate
     * @param isAlkene
     * @return this
     */
    CIPAtom create(SimpleNode atom, CIPAtom parent, boolean isDuplicate,
                   boolean isAlkene) {
      this.id = ++ptID;
      this.parent = parent;
      if (atom == null)
        return this;
      this.isAlkene = isAlkene;
      this.atom = atom;
      atomIndex = atom.getIndex();
      isKekuleAmbiguous = (bsKekuleAmbiguous != null && bsKekuleAmbiguous
          .get(atomIndex));
      elemNo = (isDuplicate && isKekuleAmbiguous ? parent
          .getKekuleElementNumber() : atom.getElementNumber());
      massNo = atom.getNominalMass();
      bondCount = atom.getCovalentBondCount();
      isTrigonalPyramidal = (bondCount == 3 && !isAlkene && (elemNo > 10 || bsAzacyclic != null
          && bsAzacyclic.get(atomIndex)));
      canBePseudo = (bondCount == 4 || isTrigonalPyramidal);
      if (parent != null)
        sphere = parent.sphere + 1;
      if (sphere == 1) {
        rootSubstituent = this;
        htPathPoints = new Hashtable<Integer, Integer>();
      } else if (parent != null) {
        rootSubstituent = parent.rootSubstituent;
        htPathPoints = rootSubstituent.htPathPoints;
      }
      this.bsPath = (parent == null ? new BS() : BSUtil.copy(parent.bsPath));

      sp2Duplicate = isDuplicate;

      rootDistance = sphere;
      // The rootDistance for a nonDuplicate atom is just its sphere.
      // The rootDistance for a duplicate atom is (by IUPAC) the sphere of its duplicated atom.
      // I argue that for aromatic compounds, this introduces a Kekule problem and that for
      // those cases, the rootDistance should be its own sphere, not the sphere of its duplicated atom.
      // This shows up in AV-360#215. 

      if (parent == null) {
        // original atom
        bsPath.set(atomIndex);
      } else if (sp2Duplicate && (!allowRule1bAlkenes || isKekuleAmbiguous)) {
        // *** Rule 1b Jmol amendment ***
      } else if (atom == root.atom) {
        // pointing to original atom
        isDuplicate = true;
        rootDistance = 0;
        rootSubstituent.spiroEnd = parent;
      } else if (bsPath.get(atomIndex)) {
        isDuplicate = true;
        rootDistance = rootSubstituent.htPathPoints.get(
            Integer.valueOf(atomIndex)).intValue();
      } else {
        bsPath.set(atomIndex);
        rootSubstituent.htPathPoints.put(Integer.valueOf(atomIndex),
            Integer.valueOf(rootDistance));
      }
      this.isDuplicate = isDuplicate;
      if (Logger.debugging) {
        if (sphere < MAX_PATH) // Logger
          myPath = (parent != null ? parent.myPath + "-" : "") + this; // Logger
        if (Logger.debuggingHigh)
          Logger.info("new CIPAtom " + myPath);
      }
      return this;
    }

    /**
     * Calculate the average element numbers of associated double-bond atoms
     * weighted by their most significant Kekule resonance contributor(s). We
     * only consider simple benzenoid systems -- 6-membered rings and their
     * 6-memebered rings fused to them. Calculated for the parent of an
     * sp2-duplicate atom.
     * 
     * @return an averaged element number
     */
    private float getKekuleElementNumber() {
      SimpleEdge[] edges = atom.getEdges();
      SimpleEdge bond;
      float ave = 0;
      int n = 0;
      for (int i = edges.length; --i >= 0;)
        if ((bond = edges[i]).isCovalent()) {
          SimpleNode other = bond.getOtherNode(atom);
          if (bsKekuleAmbiguous.get(other.getIndex())) {
            n++;
            ave += other.getElementNumber();
          }
        }
      return ave / n;
    }

    /**
     * Create a bit set that gives all the atoms in this ring if it is smaller
     * than 8.
     * 
     */
    void updateRingList() {
      BS bsRing = BSUtil.newAndSetBit(atomIndex);
      CIPAtom p = this;
      int index = -1;
      while ((p = p.parent) != null && index != atomIndex)
        bsRing.set(index = p.atomIndex);
      if (bsRing.cardinality() <= SMALL_RING_MAX) {
        for (int i = lstSmallRings.size(); --i >= 0;)
          if (lstSmallRings.get(i).equals(bsRing))
            return;
        lstSmallRings.addLast(bsRing);
      }
    }

    /**
     * Set the atom to have substituents.
     * 
     * @return true if a valid atom for consideration
     * 
     */
    boolean setNode() {
      if (isSet)
        return true;
      isSet = true;
      if (isDuplicate)
        return true;

      SimpleEdge[] bonds = atom.getEdges();
      int nBonds = bonds.length;
      if (Logger.debuggingHigh)
        Logger.info("set " + this);
      int pt = 0;
      for (int i = 0; i < nBonds; i++) {
        SimpleEdge bond = bonds[i];
        if (!bond.isCovalent())
          continue;
        SimpleNode other = bond.getOtherNode(atom);
        boolean isParentBond = (parent != null && parent.atom == other);
        int order = bond.getCovalentOrder();
        if (order == 2) {
          if (elemNo > 10 || !isFirstRow(other))
            order = 1;
          else {
            isAlkene = true;
            if (isParentBond)
              setEne();
          }
        }
        if (nBonds == 1 && order == 1 && isParentBond) {
          isTerminal = true;
          return true;
        }
        // from here on, isTerminal indicates an error condition
        switch (order) {
        case 3:
          if (addAtom(pt++, other, isParentBond, false) == null) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 2:
          if (addAtom(pt++, other, order != 2 || isParentBond, order == 2) == null) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 1:
          if (!isParentBond
              && addAtom(pt++, other, order != 1 && elemNo <= 10, false) == null) {
            isTerminal = true;
            return false;
          }
          break;
        default:
          isTerminal = true;
          return false;
        }
      }
      // don't think this can happen
      isTerminal = (pt == 0);
      nAtoms = pt;
      for (; pt < atoms.length; pt++)
        atoms[pt] = new CIPAtom().create(null, this, true, false);

      // Do an initial very shallow atom-only Rule 1 sort using a.compareTo(b)

      Arrays.sort(atoms);
      return true;
    }

    /**
     * set all ene-related fields upon finding the second atom
     */
    private void setEne() {
      parent.alkeneChild = null;
      alkeneParent = (parent.alkeneParent == null ? parent
          : parent.alkeneParent);
      alkeneParent.alkeneChild = this;
      nextSP2 = parent;
      if (parent.alkeneParent == null)
        parent.nextSP2 = this;
      if (atom.getCovalentBondCount() == 2 && atom.getValence() == 4) {
        parent.isAlkeneAtom2 = false;
        alkeneParent.isEvenEne = !alkeneParent.isEvenEne;
      } else {
        isAlkeneAtom2 = true;
      }
    }

    /**
     * Add a new atom or return null
     * 
     * @param i
     * @param other
     * @param isDuplicate
     * @param isAlkene
     * @return new atom or null
     */
    CIPAtom addAtom(int i, SimpleNode other, boolean isDuplicate, boolean isAlkene) {
      if (i >= atoms.length) {
        if (Logger.debugging)
          Logger.info(" too many bonds on " + atom);
        return null;
      }
      if (parent == null) {
        // For top level, we do not allow two 1H atoms.
        int atomIsotope = other.getIsotopeNumber();
        if (atomIsotope == 1) {
          if (++h1Count > 1) {
            if (Logger.debuggingHigh)
              Logger.info(" second H atom found on " + atom);
            return null;
          }
        }
      }
      return atoms[i] = new CIPAtom()
          .create(other, this, isDuplicate, isAlkene);
    }

    /**
     * Deep-Sort the substituents of an atom, setting the node's atoms[] and
     * priorities[] arrays. Checking for "ties" that will lead to
     * pseudochirality is also done here.
     * 
     * @param sphere
     *        current working sphere
     * @return all priorities assigned
     * 
     */
    boolean sortSubstituents(int sphere) {

      int[] indices = new int[4];
      int[] prevPrior = new int[4];
      ties = null;
      int nTies = 0;
      for (int i = 0; i < 4; i++) {
        prevPrior[i] = priorities[i];
        priorities[i] = 0;
      }

      if (Logger.debuggingHigh) {
        Logger.info(root + "---sortSubstituents---" + this);
        for (int i = 0; i < 4; i++) { // Logger
          Logger.info(getRuleName() + ": " + this + "[" + i + "]="
              + atoms[i].myPath + " " + Integer.toHexString(prevPrior[i])); // Logger
        }
        Logger.info("---");
      }

      // if this is Rule 4 or 5, then we do a check of the forward-based stereochemical path
      boolean checkRule4List = (rule4List != null && currentRule > RULE_4a);
      for (int i = 0; i < 4; i++) {
        CIPAtom a = atoms[i];
        for (int j = i + 1; j < 4; j++) {
          CIPAtom b = atoms[j];
          boolean logger_debugHigh = Logger.debuggingHigh && b.isHeavy()
              && a.isHeavy();
          int score = (a.atom == null ? B_WINS : b.atom == null ? A_WINS
              : prevPrior[i] == prevPrior[j] ? TIED
                  : prevPrior[j] < prevPrior[i] ? B_WINS : A_WINS);
          // note that a.compareTo(b) also down-scores duplicated atoms relative to actual atoms.
          if (score == TIED)
            score = (checkRule4List ? checkRule4And5(i, j) : a.checkPriority(b));
          if (logger_debugHigh)
            Logger.info(dots() + "ordering " + this.id + "." + i + "." + j
                + " " + this + "-" + a + " vs " + b + " = " + score);  // Logger
          switch (score) {
          case IGNORE:
            // just increment the index and go on to the next rule -- no breaking of the tie
            if (checkRule4List && sphere == 0)
              achiral = true; // two ligands for the root atom found to be equivalent in Rule 4 must be achiral
            indices[i]++;
            if (logger_debugHigh)
              Logger.info(dots() + atom + "." + b + " ends up with tie with " + a);
            break;
          case B_WINS:
            indices[i]++;
            priorities[i]++;
            if (logger_debugHigh)
              Logger.info(dots() + this + "." + b + " B-beats " + a);
            break;
          case A_WINS:
            indices[j]++;
            priorities[j]++;
            if (logger_debugHigh)
              Logger.info(dots() + this + "." + a + " A-beats " + b);
            break;
          case TIED:
            switch (score = sign(a.breakTie(b, sphere + 1))) {
            case TIED:
              if (parent == null && currentRule >= RULE_4c && ++nTies == 2
                  && a.spiroEnd != null && atoms[0].spiroEnd != null
                  && a.spiroEnd.atom != b.atom
                  && atoms[0].spiroEnd != a.spiroEnd) {
                priorities[1]++;
                int pt = (a.spiroEnd.atom == atoms[0].atom ? j : i);
                priorities[pt]++;
                indices[pt]++;
                if (logger_debugHigh)
                  Logger.info(dots() + this + " spiro Xaa'bb' tie resolved");
              } else {
                indices[j]++;
                if (logger_debugHigh)
                  Logger.info(dots() + this + "." + b + " ends up with tie with " + a);
              }
              break;
            case B_WINS:
              indices[i]++;
              priorities[i]++;
              if (logger_debugHigh)
                Logger.info(dots() + this + "." + b + " wins in tie with " + a);
              break;
            case A_WINS:
              indices[j]++;
              priorities[j]++;
              if (logger_debugHigh)
                Logger.info(dots() + this + "." + a + " wins in tie with " + b);
              break;
            }
            break;
          }
          if (doCheckPseudo) {
            // Rule 4 has found enantiomeric ligands. We need to make sure 
            // there are not two such sets. 
            doCheckPseudo = false;
            if (ties == null)
              ties = new Lst<int[]>();
            ties.addLast(new int[] { i, j, score });
          }
        }
      }

      // update the atoms[] and priorities[] arrays

      CIPAtom[] newAtoms = new CIPAtom[4];
      int[] newPriorities = new int[4];

      BS bs = new BS();
      for (int i = 0; i < 4; i++) {
        int pt = indices[i];
        CIPAtom a = newAtoms[pt] = atoms[i];
        newPriorities[pt] = priorities[i];
        if (a.atom != null)
          bs.set(priorities[i]);
      }
      atoms = newAtoms;
      priorities = newPriorities;
      nPriorities = bs.cardinality();

      // Check for pseudochirality

      if (ties != null) {
        switch (ties.size()) {
        case 1:
          // in the case of an alkene, we are just checking for a winner on one atom
          // otherwise we need to check the chirality of this tie
          switch (isAlkene ? STEREO_R : checkPseudoHandedness(ties.get(0), indices)) {
          case STEREO_R:
          case STEREO_S:
            isPseudo = canBePseudo;
            break;
          }
          break;
        case 2:
          break;
        case 4:
          if (nPriorities == 2) {
            priorities[1]++;
            priorities[atoms[0].spiroEnd == atoms[2].spiroEnd ? 3 : 2]++;
            nPriorities = 4;
            achiral = false;
          }
          break;
        }
      }
      if ((Logger.debuggingHigh) && atoms[2].atom != null && atoms[2].elemNo != 1) {
        Logger.info(dots() + atom + " nPriorities = " + nPriorities);
        for (int i = 0; i < 4; i++) { // Logger
          Logger.info(dots() + myPath + "[" + i + "]=" + atoms[i] + " "
              + priorities[i] + " " + Integer.toHexString(priorities[i])
              + " new");  // Logger
        }
        Logger.info(dots() + "-------");
      }
      return (nPriorities >= bondCount);
    }

    /**
     * Provide an indent for clarity in debugging messages
     * 
     * @return a string of dots based on the value of atom.sphere.
     */
    private String dots() {
      return ".....................".substring(0, Math.min(20, sphere));
    }

    /**
     * Break a tie at any level in the iteration between to atoms that otherwise
     * are the same by sorting their substituents.
     * 
     * @param b
     * @param sphere
     *        current working sphere
     * @return [0 (TIED), -1 (A_WINS), or 1 (B_WINS)] * sphere where broken
     */
    private int breakTie(CIPAtom b, int sphere) {
      if (atomIndex == 1 && b.atomIndex == 3)
        if (Logger.debugging && isHeavy() && b.isHeavy())
          Logger.info(dots() + "tie for " + this + " and " + b + " at sphere " + sphere);

      // Two duplicates of the same atom are always tied.
      if (isDuplicate && b.isDuplicate && atom == b.atom
          && rootDistance == b.rootDistance)
        return TIED;

      // Do a duplicate check -- if that is not a TIE we do not have to go any further.
      // NOTE THAT THIS IS NOT EXPLICIT IN THE RULES
      // BECAUSE DUPLICATES LOSE IN THE NEXT SPHERE, NOT THIS.

      // THE NEED FOR (sphere+1) HERE SHOWS THAT 
      // SUBRULE 1a MUST BE COMPLETED EXHAUSTIVELY PRIOR TO SUBRULE 1b.

      int score = checkIsDuplicate(b);
      if (score != TIED)
        return score * (sphere + 1);

      // return NO_CHIRALITY/TIED if:
      //  a) one or the other can't be set (because it has too many connections)
      //  b) or both are terminal or both are duplicates

      if (!setNode() || !b.setNode() || isTerminal && b.isTerminal
          || isDuplicate && b.isDuplicate)
        return TIED;

      // We are done -- again, for the next sphere, actually -- if one of these
      // is terminal.
      if (isTerminal != b.isTerminal)
        return (isTerminal ? B_WINS : A_WINS) * (sphere + 1);

// not necessary after all     // Rule 1b requires a special presort to ensure that all duplicate atoms 
//      // are in the right order -- first by element number and then by root distance.
//
//      if (currentRule == RULE_1b && false) {
//        preSortRule1b();
//        b.preSortRule1b();
//      }

      // Phase I -- shallow check only
      //
      // Check to see if any of the three connections to a and b are different.
      // Note that we do not consider duplicate atoms different from regular atoms here
      // because we are just looking for atom differences, not substituent differences.
      // This nuance is not clear from the "simplified" digraphs found in Chapter 9. 
      //
      // Say we have {O (O) C} and {O O H}
      //
      // The rules require that we first only look at just the atoms, so OOC beats OOH.

      if ((score = compareShallowly(b, sphere)) != TIED)
        return score;

      // Phase II -- deep check using breakTie
      //
      // OK, so all three are nominally the same.
      // Now iteratively deep-sort each list based on substituents
      // and then check them one by one to see if the tie can be broken.

      sortSubstituents(sphere);
      b.sortSubstituents(sphere);
      return compareDeeply(b, sphere);
    }

//    /**
//     * Sort duplicate nodes of the same element by root distance, from closest
//     * to root to furthest.
//     * 
//     */
//    private void preSortRule1b() {
//      CIPAtom a1, a2;
//      for (int i = 0; i < 3; i++) {
//        if (!(a1 = atoms[i]).isDuplicate)
//          continue;
//        for (int j = i + 1; j < 4; j++) {
//          if (!(a2 = atoms[j]).isDuplicate || a1.elemNo != a2.elemNo
//              || a1.rootDistance <= a2.rootDistance)
//            continue;
//          atoms[i] = a2;
//          atoms[j] = a1;
//          j = 4;
//          i = -1;
//        }
//      }
//    }

    /**
     * Just checking for hydrogen.
     * 
     * @return true if not hydrogen.
     */
    private boolean isHeavy() {
      return massNo > 1;
    }

    /**
     * The first part of breaking a tie at the current level. Compare only in
     * the current substitutent sphere; a preliminary check using the current
     * rule.
     * 
     * @param b
     * @param sphere
     *        current working sphere
     * @return 0 (TIED) or [-1 (A_WINS), or 1 (B_WINS)]*sphereOfSubstituent
     */
    private int compareShallowly(CIPAtom b, int sphere) {
      for (int i = 0; i < nAtoms; i++) {
        CIPAtom ai = atoms[i];
        CIPAtom bi = b.atoms[i];
        int score = ai.checkCurrentRule(bi);
        // checkCurrentRule can return IGNORE, but we ignore that here.
        if (score == IGNORE)
          score = TIED;
        if (score != TIED) {
          if (Logger.debugging && ai.isHeavy() && bi.isHeavy())
            Logger.info(ai.dots() + "compareShallow " + i + " " + this + "."
                + ai + "/" + b + "." + bi + ": " + score * (sphere + 1));  // Logger
          return score * (sphere + 1);
        }
      }
      return TIED;
    }

    /**
     * Continue to break ties in each substituent in a and b, as we now know
     * that all of them are tied. This take us to the next sphere.
     * 
     * @param b
     * @param sphere
     *        current working sphere
     * @return 0 (TIED) or [-1 (A_WINS), or 1 (B_WINS)]*n, where n is the lowest
     *         sphere of a win
     */
    private int compareDeeply(CIPAtom b, int sphere) {
      int finalScore = (nAtoms == 0 ? B_WINS : TIED), absScore = Integer.MAX_VALUE;
      for (int i = 0; i < nAtoms; i++) {
        CIPAtom ai = atoms[i], bi = b.atoms[i];
        if (Logger.debuggingHigh && ai.isHeavy() && bi.isHeavy())
          Logger.info(ai.dots() + "compareDeep sub " + this + "." + ai + " " + b + "." + bi);
        int score = ai.breakTie(bi, sphere + 1);
        if (score == TIED)
          continue;
        int abs = Math.abs(score);
        if (abs < absScore) {
          absScore = abs;
          finalScore = score;
          if (Logger.debugging && ai.isHeavy() && bi.isHeavy())
            Logger.info(ai.dots() + "compareDeep sub " + ai + " " + bi + ": " + score);
        }
      }
      if (Logger.debuggingHigh)
        Logger.info(dots() + "compareDeep " + this + " " + b + ": " + finalScore);
      return finalScore;
    }

    /**
     * Used in Array.sort when an atom is set; includes a preliminary check for
     * duplicates, since we know that that atom will ultimately be lower
     * priority if all other rules are tied. This is just a convenience.
     * 
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    @Override
    public int compareTo(CIPAtom b) {
      int score;
      return (b == null ? A_WINS
          : (atom == null) != (b.atom == null) ? (atom == null ? B_WINS
              : A_WINS) : (score = checkRule1a(b)) != TIED ? score
              : checkIsDuplicate(b));
    }

    /**
     * Used in sortSubstituents
     * 
     * @param b
     * 
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkPriority(CIPAtom b) {
      int score;
      return (b == null ? A_WINS
          : (atom == null) != (b.atom == null) ? (atom == null ? B_WINS
              : A_WINS) : (score = checkCurrentRule(b)) == IGNORE ? TIED
              : score);
    }

    /**
     * This check is not technically one of those listed in the rules, but it is
     * useful when preparing to check substituents because if one of the atoms
     * has substituents and the other doesn't, we are done -- there is no reason
     * to check substituents.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkIsDuplicate(CIPAtom b) {
      return b.isDuplicate == isDuplicate ? TIED : b.isDuplicate ? A_WINS
          : B_WINS;
    }

    /**
     * Check this atom "A" vs a challenger "B" against the current rule.
     * 
     * Note that example BB 2013 page 1258, P93.5.2.3 requires that RULE_1b be
     * applied only after RULE_1a has been applied exhaustively for a sphere;
     * otherwise C1 is R, not S (AY-236.53).
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS), or Intege.MIN_VALUE
     *         (IGNORE)
     */
    public int checkCurrentRule(CIPAtom b) {
      switch (currentRule) {
      default:
      case RULE_1a:
        //        // Checking what happens if you do 1a and 1b together
        //        // note a fix is also required 
        //        int score = checkRule1a(b);
        //        if (score == TIED) {
        //          score = checkRule1b(b);
        //        }
        //        return score;
        return checkRule1a(b);
      case RULE_1b:
        return checkRule1b(b);
      case RULE_2:
        return checkRule2(b);
      case RULE_3:
        return checkRule3(b); // can be IGNORE
      case RULE_4a:
        return checkRules4a(b, " sr SR PM");
      case RULE_4b:
      case RULE_4c:
      case RULE_5:
        return TIED; // not carried out here because these need access to a full list of ligands 
        //      case RULE_4c:  // taken care of by RULE_4bc
        //        return checkRules4a4c(b, " s r");
      }
    }

    /**
     * Looking for same atom or ghost atom or element number
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRule1a(CIPAtom b) {
      return b.atom == null ? A_WINS : atom == null ? B_WINS
          : b.elemNo < elemNo ? A_WINS : b.elemNo > elemNo ? B_WINS : TIED;
    }

    /**
     * Looking for root distance -- duplicate atoms only.
     * 
     * @param b
     * 
     * 
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */

    private int checkRule1b(CIPAtom b) {
      return b.isDuplicate != isDuplicate ? TIED
          : !allowRule1bAlkenes && (b.isAlkene || b.isAlkene) ? TIED :   
            b.rootDistance != rootDistance ? (b.rootDistance > rootDistance ? A_WINS
              : B_WINS)
              : TIED;
    }

    /**
     * Chapter 9 Rule 2. atomic mass
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRule2(CIPAtom b) {
      return b.massNo < massNo ? A_WINS : b.massNo > massNo ? B_WINS : TIED;
    }

    /**
     * Chapter 9 Rule 3. E/Z.
     * 
     * We carry out this step only as a tie in the sphere AFTER the final atom
     * of the alkene or even-cumulene.
     * 
     * If the this atom and the comparison atom b are on the same parent, then
     * this is simply a request to break their tie based on Rule 3; otherwise
     * two paths are being checked for one being seqCis and one being seqTrans.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRule3(CIPAtom b) {
      int za, zb;
      return isDuplicate || b.isDuplicate 
          || !parent.isAlkeneAtom2 || !b.parent.isAlkeneAtom2
          || !parent.alkeneParent.isEvenEne || !b.parent.alkeneParent.isEvenEne ? IGNORE
          : parent == b.parent ? sign(breakTie(b, 0))
              : (za = parent.getEZaux()) < (zb = b.parent.getEZaux()) ? A_WINS
                  : za > zb ? B_WINS : TIED;
    }

    /**
     * Check auxiliary Z by temporarily setting return path.
     * 
     * This method uses CIPAtom.clone() effectively to create a second
     * independent path that is checked without messing up the currently
     * expanding node graph.
     * 
     * Note that one path from the current atom is in reverse - the path back to
     * the root atom. This must be reconstructed, because until this point we
     * have not carried out many of the necessary comparisons.
     * 
     * @return one of [STEREO_Z, STEREO_E, STEREO_BOTH_EZ]
     */
    private int getEZaux() {
      // this is the second atom of the alkene, checked as the parent of the next atom
      // (because there is no need to do this test until we are on the branch that includes
      //  the atom after the alkene).
      if (auxEZ == STEREO_UNDETERMINED
          && (auxEZ = alkeneParent.auxEZ) == STEREO_UNDETERMINED) {
        auxEZ = getEneWinnerChirality(alkeneParent, this, RULE_3, false);
        if (auxEZ == NO_CHIRALITY)
          auxEZ = STEREO_BOTH_EZ;
      }
      alkeneParent.auxEZ = auxEZ;
      if (Logger.debuggingHigh)
        Logger.info("getZaux " + alkeneParent + " " + auxEZ);
      return auxEZ;
    }

    /**
     * Determine the winner on one end of an alkene or cumulene.
     * 
     * @param end1
     * @param end2
     * @param maxRule
     * @param isAxial
     * @return one of: {NO_CHIRALITY | STEREO_Z | STEREO_E | STEREO_M |
     *         STEREO_P}
     */
    private int getEneWinnerChirality(CIPAtom end1, CIPAtom end2, int maxRule,
                                      boolean isAxial) {
      CIPAtom winner1 = getEneEndWinner(end1, end1.nextSP2, maxRule), 
          winner2 = (winner1 == null || winner1.atom == null ? null
          : getEneEndWinner(end2, end2.nextSP2, maxRule));
      return getEneChirality(winner1, end1, end2, winner2, isAxial, false);
    }

    /**
     * Get the atom that is the highest priority of two atoms on the end of a
     * double bond after sorting from Rule 1a through a given rule (Rule 3 or
     * Rule 5)
     * 
     * @param end
     * @param prevSP2
     * @param ruleMax
     * @return higher-priority atom, or null if they are equivalent
     */
    private CIPAtom getEneEndWinner(CIPAtom end, CIPAtom prevSP2, int ruleMax) {
      CIPAtom atom1 = (CIPAtom) end.clone();
      if (atom1.parent != prevSP2)
        atom1.addReturnPath(prevSP2, end);
      CIPAtom a = null;
      for (int i = RULE_1a; i <= ruleMax; i++)
        if ((a = atom1.getTopSorted(i)) != null)
          break;
      return (a == null || a.atom == null ? null : a);
    }

    /**
     * 
     * @param newParent
     * @param fromAtom
     */
    private void addReturnPath(CIPAtom newParent, CIPAtom fromAtom) {
      Lst<CIPAtom> path = new Lst<CIPAtom>();
      CIPAtom thisAtom = this, newSub, oldParent = fromAtom, oldSub = newParent;
      // create path back to root
      while (oldParent.parent != null && oldParent.parent.atoms[0] != null) {
        if (Logger.debuggingHigh)
          Logger.info("path:" + oldParent.parent + "->" + oldParent);
        path.addLast(oldParent = oldParent.parent);
      }
      path.addLast(null);
      for (int i = 0, n = path.size(); i < n; i++) {
        oldParent = path.get(i);
        newSub = (oldParent == null ? new CIPAtom().create(null, this, true,
            isAlkene) : (CIPAtom) oldParent.clone());
        newSub.sphere = thisAtom.sphere + 1;
        thisAtom.replaceParentSubstituent(oldSub, newParent, newSub);
        if (i > 0 && thisAtom.isAlkene && !thisAtom.isAlkeneAtom2) {
          // reverse senses of alkenes
          if (newParent.isAlkeneAtom2) {
            newParent.isAlkeneAtom2 = false;
            thisAtom.alkeneParent = newParent;
          }
          thisAtom.setEne();
        }
        newParent = thisAtom;
        thisAtom = newSub;
        oldSub = fromAtom;
        fromAtom = oldParent;
      }
    }

    /**
     * Swap a substituent and the parent in preparation for reverse traversal of
     * this path back to the root atom.
     * 
     * @param oldSub
     * @param newParent
     * @param newSub
     */
    private void replaceParentSubstituent(CIPAtom oldSub, CIPAtom newParent,
                                          CIPAtom newSub) {
      for (int i = 0; i < 4; i++)
        if (atoms[i] == oldSub || newParent == null && atoms[i].atom == null) {
          if (Logger.debuggingHigh)
            Logger.info("reversed: " + newParent + "->" + this + "->" + newSub);
          parent = newParent;
          atoms[i] = newSub;
          Arrays.sort(atoms);
          break;
        }
    }

    /**
     * The result of checking a Mata series of parallel paths may be one of
     * several values. TIED here probably means something went wrong; IGNORE
     * means we have two of the same chirality, for example RSRR RSRR. In that
     * case, we defer to Rule 5. The idea is to handle all such business in
     * Sphere 1.
     * 
     * 
     * @param i
     * @param j
     * @return 0 (TIED), -1 (A_WINS), 1 (B_WINS), Integer.MIN_VALUE (IGNORE)
     */
    private int checkRule4And5(int i, int j) {
      return (rule4List[i] == null && rule4List[j] == null ? TIED
          : rule4List[j] == null ? A_WINS : rule4List[i] == null ? B_WINS
              : compareMataPair(i, j));
    }

    /**
     * Chapter 9 Rules 4 and 5: like vs. unlike
     * 
     * Compare two strings such as RSSSR and SRSRR for like and unlike and find
     * a winner. Return IGNORE if they are identical; return A_WINS or B_WINS if
     * there is a winner, and set this.doCheckPseudo if they are opposites with
     * reference atom R or S (but not r or s).
     * 
     * @param ia
     * @param ib
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS), or Integer.MIN_VALUE
     *         (IGNORE)
     */
    private int compareMataPair(int ia, int ib) {
      // note that opposites will need to generate "R" or "S" keys, which will be 
      // resolved as "r" or "s" 
      // but generally we will want to process this as "R" and "S"
      // note that this analysis cannot be done ahead of time
      String aStr = rule4List[ia].substring(1), bStr = rule4List[ib].substring(1);
      if (currentRule == RULE_4c) {
        aStr = PT.rep(aStr, "~", "");
        bStr = PT.rep(bStr, "~", "");
      } else {
        boolean haveRS = false, isRule5 = (currentRule == RULE_5);
        if (atoms[ia].nextChiralBranch != null) {
          String s = atoms[ia].getMataList(getFirstRef(aStr), isRule5);
          haveRS = (s.indexOf("|") >= 0);
          aStr = (haveRS ? s : aStr + s);
        }
        if (atoms[ib].nextChiralBranch != null) {
          String s = atoms[ib].getMataList(getFirstRef(bStr), isRule5);
          haveRS |= (s.indexOf("|") >= 0);
          bStr = (s.indexOf("|") < 0 ? bStr + s : s);
        }
        if (Logger.debugging)
          Logger.info(dots() + this + " comparing " + atoms[ia] + " " + aStr
              + " to " + atoms[ib] + " " + bStr); // Logger
        if (isRule5 || !haveRS && aStr.length() != bStr.length()) {
          // note that these two strings can be different lengths
          // if we have sXX and ~
          return sign(aStr.compareTo(bStr));
        }
        aStr = cleanRule4Str(aStr);
        bStr = cleanRule4Str(bStr);
        if (haveRS) {
          // Mata(2005) Figure 9
          // We are trying to ascertain that
          // R lull                  R luuu
          // S luuu   is the same as S lull
          // 
          // Solution is to SUM all winners. If that is 0, then they are the same
          String[] aList = PT.split(aStr, "|"), bList = PT.split(bStr, "|");
          int minScore = Integer.MAX_VALUE, sumScore = 0;
          aStr = aList[0];
          bStr = bList[0];
          for (int i = aList.length; --i >= 0;) {
            for (int j = bList.length; --j >= 0;) {
              int score = compareRule4PairStr(aList[i], bList[j], true);
              sumScore += score;
              if (score != TIED && Math.abs(score) <= minScore) {
                minScore = Math.abs(score);
                aStr = aList[i];
                bStr = bList[j];
              }
            }
          }
          if (sumScore == TIED)
            return TIED;
        }
      }
      if (aStr.length() == 1 && "RS".indexOf(aStr) < 0) {
        int score = checkEnantiomer(aStr, bStr, 0, aStr.length(), " rs");
        switch (score) {
        case A_WINS:
        case B_WINS:
          canBePseudo = false;
          doCheckPseudo = !isAxialRoot;
          return score;
        }
      }
      return compareRule4PairStr(aStr, bStr, false);
    }

    private String cleanRule4Str(String aStr) {
      return (aStr.length() > 1 ? PT.replaceAllCharacters(aStr, "sr~", "")
          : aStr);
    }

    /**
     * Just get the first R- or S-equivalent in "~~~~xxxxx"
     * 
     * @param aStr
     * @return "R", "S", or null
     */
    private String getFirstRef(String aStr) {
      int r = aStr.indexOf("R"), s = aStr.indexOf("S");
      return (r < 0 && s < 0 ? null : r < 0 ? "S" : "R");
    }

    /**
     * Retrieve the Mata Rule 4b list for a given atom.
     * 
     * @param aref
     * @param isRule5
     * @return a String representation of the path through the atoms
     * 
     */
    private String getMataList(String aref, boolean isRule5) {
      int n = 0;
      for (int i = rule4List.length; --i >= 0;)
        if (rule4List[i] != null)
          n++;
      String[] listA = new String[n];
      for (int j = n, i = rule4List.length; --i >= 0;)
        if (rule4List[i] != null)
          listA[--j] = rule4List[i];
      if (aref == null) {
        aref = getMataRef(isRule5);
      } else {
        // we need to add the priority business only if this is the first case
        for (int i = 0; i < n; i++)
          listA[i] = "." + listA[i].substring(1);
      }
      return (aref.length() == 1 ? getMataSequence(listA, aref, isRule5)
          : getMataSequence(listA, "R", false) + "|"
              + getMataSequence(listA, "S", false));
    }

    /**
     * The reference designation is the most popular of R and S of the highest-
     * priority node, or both if there are the same number at highest-priority
     * node level
     * 
     * @param isRule5
     * @return "R", "S", or "RS"
     */
    private String getMataRef(boolean isRule5) {
      return (isRule5 ? "R" : rule4Count[STEREO_R] > rule4Count[STEREO_S] ? "R"
          : rule4Count[STEREO_R] < rule4Count[STEREO_S] ? "S" : "RS");
    }

    /**
     * This is the key Mata method -- getting the correct sequence of R and S
     * from a set of diasteromorphic paths. Given a specific reference
     * designation, the task is to sort the paths based on priority (we can't
     * change the base priority already determined using Rules 1-3) and
     * reference.
     * 
     * We do the sort lexicographically, simply using Array.sort(String[]) with
     * our reference atom temporarily given the lowest ASCII characater "A"
     * (65).
     * 
     * @param lst
     * @param chRef
     * @param isRule5
     * @return one string, possibly separated by | indicating that the result
     *         has both an R and S side to it
     */
    private String getMataSequence(String[] lst, String chRef, boolean isRule5) {
      int n = lst.length, len = 0;
      String[] lst1 = new String[n];
      for (int j = n, i = rule4List.length; --i >= 0;) {
        if (rule4List[i] != null) {
          --j;
          lst1[j] = lst[j];
          if (atoms[i].nextChiralBranch != null)
            lst1[j] += atoms[i].nextChiralBranch.getMataList(chRef, isRule5);
        }
      }
      String[] sorted = (isRule5 ? lst1 : getMataSortedList(lst1, chRef));
      for (int i = 0; i < n; i++) {
        String rs = sorted[i];
        if (rs.length() > len)
          len = rs.length();
      }

      // Strip out all non-R/S designations
      String mlist = "";
      char ch;
      for (int i = 1; i < len; i++) {
        for (int j = 0; j < n; j++) {
          String rs = sorted[j];
          if (i < rs.length() && (ch = rs.charAt(i)) != '~' && ch != ';')
            mlist += ch;
        }
        if (isRule5) {
          // clear out this sphere and resort
          for (int j = 0; j < n; j++) {
            String rs = sorted[j];
            if (i < rs.length())
              sorted[j] = rs.substring(0, i) + "~" + rs.substring(i + 1);
          }
          Arrays.sort(sorted);
        }
      }
      return mlist;
    }

    /**
     * Comparison of two strings such as RSSR and SRSS for Rule 4b.
     * 
     * @param aStr
     * @param bStr
     * @param isRSTest
     *        This is just a test for optional pathways, for example: RS|SR
     * 
     * 
     * @return 0 (TIED), -1 (A_WINS), 1 (B_WINS), Integer.MIN_VALUE (IGNORE)
     */
    private int compareRule4PairStr(String aStr, String bStr, boolean isRSTest) {
      if (Logger.debugging)
        Logger.info(dots() + this.myPath + " Rule 4b comparing " + aStr + " " + bStr);
      doCheckPseudo = false;
      int n = aStr.length();
      if (n == 0 || n != bStr.length())
        return TIED;
      char aref = aStr.charAt(0), bref = bStr.charAt(0);
      for (int c = 1; c < n; c++) {
        boolean alike = (aref == aStr.charAt(c));
        if (alike != (bref == bStr.charAt(c)))
          return (isRSTest ? c : 1) * (alike ? A_WINS : B_WINS);
      }
      if (isRSTest)
        return TIED;
      if (aref == bref)
        return IGNORE;
      // are opposites
      if (!canBePseudo)
        root.canBePseudo = false;
      doCheckPseudo = canBePseudo && (aref == 'R' || aref == 'S');
      return aref < bref ? A_WINS : B_WINS;
    }

    /**
     * Sort Mata list of ["RS...", "SR..."] by temporarily assigning the
     * reference atom chirality the letter "A" and then sorting
     * lexicographically.
     * 
     * @param lst
     * @param aref
     * @return sorted list
     */
    private String[] getMataSortedList(String[] lst, String aref) {
      int n = lst.length;
      String[] sorted = new String[n];
      for (int i = 0; i < n; i++)
        sorted[i] = PT.rep(lst[i], aref, "A");
      Arrays.sort(sorted);
      for (int i = 0; i < n; i++)
        sorted[i] = PT.rep(sorted[i], "A", aref);
      if (Logger.debuggingHigh)
        for (int i = 0; i < n; i++)
          // Logger
          Logger.info("Sorted Mata list " + i + " " + aref + ": " + sorted[i]);
      return sorted;
    }

    /**
     * This critical method creates a list of downstream (higher-sphere)
     * auxiliary chirality designators (R or S) in the correct order specified
     * by Mata that are passed upstream ultimately to the Sphere-1 root
     * substituent.
     * 
     * @param node1
     *        first node; sphere 1
     * @param ret
     *        CIPAtom of next stereochemical branching point
     * 
     * @return collective string, with setting of rule4List
     */
    String createAuxiliaryRule4Data(CIPAtom node1, CIPAtom[] ret) {
      int rs = -1;
      String subRS = "", s = (node1 == null ? "" : "~");
      boolean isBranch = false, noPseudo = false;
      if (atom != null) {
        rule4List = new String[4]; // full list based on atoms[]
        int[] mataList = new int[4]; //sequential pointers into rule4List
        int nRS = 0;
        CIPAtom[] ret1 = new CIPAtom[1];
        for (int i = 0; i < 4; i++) {
          CIPAtom a = atoms[i];
          if (a != null)
            a.setNode();
          if (a != null && !a.isDuplicate && !a.isTerminal) {
            a.priority = priorities[i];
            ret1[0] = null;
            String ssub = a.createAuxiliaryRule4Data(node1 == null ? a : node1,
                ret1);
            if (ret1[0] != null) {
              a.nextChiralBranch = ret1[0];
              if (ret != null)
                ret[0] = ret1[0];
            }
            rule4List[i] = a.priority + ssub;
            if (a.nextChiralBranch != null || isChiralSequence(ssub)) {
              mataList[nRS] = i;
              nRS++;
              subRS += ssub;
            } else {
              rule4List[i] = null;
            }
          }
        }
        int adj = TIED;
        switch (nRS) {
        case 0:
          subRS = "";
          break;
        case 1:
          break;
        case 2:
          if (node1 != null) {
            // we want to now if these two are enantiomorphic, identical, or diastereomorphic.
            adj = (compareRule4aIsomers(mataList[0], mataList[1]));
            switch (adj) {
            case TIED:
              // identical
              isBranch = true;
              s = "~";
              subRS = "";
              break;
            case NOT_RELEVANT:
              // a and b are different priorities - we need to check to see if we have a chiral branch node
              isBranch = true;
              break;
            case DIASTEREOMERIC_A_WINS:
            case DIASTEREOMERIC_B_WINS:
              adj -= sign(adj);
              subRS = "r";
              //$FALL-THROUGH$
            case A_WINS:
            case B_WINS:
              isBranch = true;
              noPseudo = subRS.indexOf("r") >= 0;
              // enantiomers -- we have an r/s situation
              // process to determine chirality, but then set ret[0] to be null
              subRS = "";
              break;
            }
          }
          break;
        case 3:
          // 
          // check for exactly one enantiomeric pair among the three groups
          //
          //    {RRS}    e      d     
          //           {SSR}    d
          //                  {SRS}  two enantiomers and a diastereomer (r/s)
          //
          //
          int irs = 0,
          jrs = 0,
          adj0 = TIED;
          for (int i = 0; i < 2; i++) {
            for (int j = i + 1; j < 3; j++) {
              adj0 = (compareRule4aIsomers(mataList[i], mataList[j]));
              switch (adj0) {
              case A_WINS:
              case B_WINS:
                if (adj == TIED) {
                  adj = adj0;
                  irs = i;
                  jrs = j;
                  continue;
                }
                i = j = 3;
                adj = TIED;
                break;
              default:
                break;
              }
            }
          }
          if (adj != TIED) {
            mataList[0] = mataList[irs];
            mataList[1] = mataList[jrs];
          }
          //$FALL-THROUGH$
        case 4:
          s = "";
          isBranch = true;
          break;
        }
        if (isBranch) {
          subRS = "";
          if (ret != null)
            ret[0] = this;
        }
        if (!isBranch || adj == A_WINS || adj == B_WINS || adj == NOT_RELEVANT) {
          if (isAlkene) {
            if (!isBranch && alkeneChild != null) {
              // must be alkeneParent 
              boolean isSeqCT = (ret != null && ret[0] == alkeneChild);
              // All odd cumulenes need to be checked.
              // If it is an alkene or even cumulene, we must do an auxiliary check on this only 
              // if it is not already a defined stereochemistry, because in that
              // case we have a simple E or Z, and there is no need to check AND
              // it does not contribute to the Mata sequence (so would mess it up).
              if (!isEvenEne || auxEZ == STEREO_BOTH_EZ
                  && alkeneChild.bondCount >= 2 && !isKekuleAmbiguous) {
                rs = getEneWinnerChirality(this, alkeneChild, RULE_5, !isEvenEne);
                switch (rs) {
                case STEREO_M:
                  rs = STEREO_R;
                  s = "R";
                  break;
                case STEREO_P:
                  rs = STEREO_S;
                  s = "S";
                  break;
                case STEREO_Z:
                  rs = STEREO_R;
                  s = "R";
                  break;
                case STEREO_E:
                  rs = STEREO_S;
                  s = "S";
                  break;
                }
                if (rs != NO_CHIRALITY) {
                  auxChirality = s;
                  addMataRef(sphere, priority, rs);
                  subRS = "";
                  if (isSeqCT) {
                    nextChiralBranch = alkeneChild;
                    ret[0] = this;
                  }

                }
              }
            }
          } else if (node1 != null
              && (bondCount == 4 && nPriorities >= 3 - Math.abs(adj) || isTrigonalPyramidal
                  && nPriorities >= 2 - Math.abs(adj))) {
            // if adj is 1 or -1, then we check for one fewer priorities because
            // it means we had two groups that were either enantiomers or diasteriomers

            if (isBranch && adj != NOT_RELEVANT) {
              // if here, adj is A_WINS (-1), or B_WINS (1) 
              // we check based on A winning, but then reverse it if B actually won
              switch (checkPseudoHandedness(mataList, null)) {
              case STEREO_R:
                s = (adj == A_WINS ? "r" : "s");
                break;
              case STEREO_S:
                s = (adj == A_WINS ? "s" : "r");
                break;
              }
              if (noPseudo)
                s = s.toUpperCase(); // Rule 4c or diasteriomers // AY-236.148
              auxChirality = s;
              subRS = "";
              //if (ret != null)
              //ret[0] = null;
            } else {
              // if here, adj is TIED (0) or
              CIPAtom atom1 = (CIPAtom) clone();
              if (atom1.setNode()) {
                atom1.addReturnPath(null, this);
                atom1.sortToRule(RULE_3);
                rs = atom1.checkHandedness();
                s = (rs == STEREO_R ? "R" : rs == STEREO_S ? "S" : "~");
                if (Logger.debugging)
                  Logger.info("AUX " + s + " for " + atom1.myPath);
                parent.addMataRef(sphere, priority, rs);
              }
            }
          }
        }
      }
      s += subRS;
      if (Logger.debugging && !s.equals("~"))
        Logger.info("creating aux " + myPath + s);
      return s;
    }

    /**
     * Sort by a given rule, preserving root.canBePseudo and currentRule.
     * 
     * @param rule
     * @return true if a decision has bee made
     */
    private boolean sortByRule(int rule) {
      boolean rootPseudo = root.canBePseudo;
      int current = currentRule;
      currentRule = rule;
      boolean isChiral = sortSubstituents(0);
      currentRule = current;
      root.canBePseudo = rootPseudo;
      return isChiral;
    }

    private void sortToRule(int maxRule) {
      for (int i = RULE_1a; i <= maxRule; i++)
        if (sortByRule(i))
          return;
    }

    private boolean isChiralSequence(String ssub) {
      return ssub.indexOf("R") >= 0 || ssub.indexOf("S") >= 0
          || ssub.indexOf("r") >= 0 || ssub.indexOf("s") >= 0;
    }

    /**
     * Accumlate the number of R and S centers at a given sphere+priority level
     * 
     * @param sphere
     *        1,2,3...
     * @param priority
     *        1-4
     * @paramPriority
     * @param rs
     */
    private void addMataRef(int sphere, int priority, int rs) {
      if (rule4Count == null) {
        rule4Count = new int[] { Integer.MAX_VALUE, 0, 0 };
      }
      int n = sphere * 10 + priority;
      if (n <= rule4Count[0]) {
        if (n < rule4Count[0]) {
          rule4Count[0] = n;
          rule4Count[STEREO_R] = rule4Count[STEREO_S] = 0;
        }
        rule4Count[rs]++;
      }
      if (Logger.debugging)
        Logger.info(this + " addMata " + sphere + " " + priority + " " + rs
            + " " + PT.toJSON("rule4Count", rule4Count));  // Logger
      if (parent != null)
        parent.addMataRef(sphere, priority, rs);
    }

    /**
     * Check for enantiomeric strings such as S;R; or SR
     * 
     * @param i1
     * @param i2
     * @return NOT_RELEVANT if there is no stereochemistry, TIED if they are
     *         equal, A_WINS for enantiomer Rxxx, B_WINS for Sxxxx, or
     *         DIASTERIOMERIC_A_WINS or DIASTERIOMERIC_B_WINS
     */
    private int compareRule4aIsomers(int i1, int i2) {
      String rs1 = rule4List[i1], rs2 = rule4List[i2];
      if (rs1.charAt(0) != rs2.charAt(0))
        return NOT_RELEVANT;
      int n = rs1.length();
      if (n != rs2.length())
        return NOT_RELEVANT; // TODO: ?? this may not be true -- paths with and without O, N, C for example, that still have stereochemistry
      if (rs1.equals(rs2))
        return TIED;
      boolean haveRS = (rs1.indexOf("R") >= 0 || rs1.indexOf("S") >= 0);
      String rs = (haveRS ? "~RS" : "~rs");
      if (haveRS) {
        rs1 = PT.replaceAllCharacters(rs1, "rs", "~");
        rs2 = PT.replaceAllCharacters(rs2, "rs", "~");
      }
      int score = checkEnantiomer(rs1, rs2, 1, n, rs);
      if (score == DIASTEREOMERIC) {
        switch (compareMataPair(i1, i2)) {
        case A_WINS:
          return DIASTEREOMERIC_A_WINS;
        case B_WINS:
          return DIASTEREOMERIC_B_WINS;
        }
      }
      return score;
    }

    /**
     * Check to see if two RS-strings are enantiomeric or not. If enantiomeric,
     * returns which has higher priority by Rule 4b. Can be used to compare "r"
     * and "s" as well as "R" and "S"
     * 
     * @param rs1
     * @param rs2
     * @param m
     * @param n
     * @param rs
     * @return DIASTEROMERIC, A_WINS, or B_WINS
     */
    private int checkEnantiomer(String rs1, String rs2, int m, int n, String rs) {
      int finalScore = TIED; // not a possible return
      // "0~~R 0~~S"
      for (int i = m; i < n; i++) {
        // a score of 0 means ~ was present for both
        // a score of 3 means one was R and one was S
        // any other score indicates diasteriomeric
        int i1 = rs.indexOf(rs1.charAt(i));
        int score = i1 + rs.indexOf(rs2.charAt(i));
        if (score == 0)
          continue;
        if (score != STEREO_BOTH_RS) {
          return DIASTEREOMERIC;
        }
        if (finalScore == TIED)
          finalScore = (i1 == STEREO_R ? A_WINS : B_WINS);
      }
      return finalScore;
    }

    /**
     * Reverse the path to the parent and check r/s chirality
     * 
     * @param iab
     * @param indices
     * @return STEREO_R or STEREO_S
     * 
     */
    private int checkPseudoHandedness(int[] iab, int[] indices) {
      int ia = (indices == null ? iab[0] : indices[iab[0]]);
      int ib = (indices == null ? iab[1] : indices[iab[1]]);
      CIPAtom atom1;
      // critical here that we do NOT include the tied branches
      atom1 = (CIPAtom) clone();
      atom1.atoms[ia] = new CIPAtom().create(null, atom1, false, false);
      atom1.atoms[ib] = new CIPAtom().create(null, atom1, false, false);
      atom1.addReturnPath(null, this);
      // We are guaranteed that only RULE_1a is necessary, because one of our
      // paths goes all the way back to the root, without a duplicate atom, and any
      // other path reaching that will terminate with a duplicate atom instead.
      atom1.sortByRule(RULE_1a);
      // Now add the tied branches at the end; it doesn't matter where they 
      // go as long as they are together and in order.
      atom1.atoms[bondCount - 2] = atoms[Math.min(ia, ib)];
      atom1.atoms[bondCount - 1] = atoms[Math.max(ia, ib)];
      int rs = atom1.checkHandedness();
      if (Logger.debugging) {
        for (int i = 0; i < 4; i++)
          Logger.info("pseudo " + rs + " " + priorities[i] + " " + atoms[i].myPath);
      }
      return rs;
    }

    /**
     * Return top-priority non-sp2-duplicated atom.
     * 
     * @param rule
     * 
     * @return highest-priority non-duplicated atom
     */
    private CIPAtom getTopSorted(int rule) {
      sortByRule(rule);
      for (int i = 0; i < 4; i++) {
        CIPAtom a = atoms[i];
        if (!a.sp2Duplicate)
          return priorities[i] == priorities[i + 1] ? null : atoms[i];
      }
      return null;
    }

    /**
     * Chapter 9 Rules 4a and 4c. This method allows for "RS" to be checked as
     * "either R or S". See AY236.66, AY236.67,
     * AY236.147,148,156,170,171,201,202, etc. (4a)
     * 
     * @param b
     * @param test
     *        String to test against; depends upon subrule being checked
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRules4a(CIPAtom b, String test) {
      if (isTerminal || isDuplicate)
        return TIED;
      int isRa = test.indexOf(auxChirality), isRb = test.indexOf(b.auxChirality);
      return (isRa > isRb + 1 ? A_WINS : isRb > isRa + 1 ? B_WINS : TIED);
    }

    /**
     * Determine the ordered CIP winding of this atom. For this, we just take
     * the directed normal through the plane containing the top three
     * substituent atoms and dot that with the vector from any one of them to
     * the fourth ligand (or the root atom if trigonal pyramidal). If this is positive, we have R.
     * 
     * @return 1 for "R", 2 for "S"
     */
    int checkHandedness() {
      P3 p1 = atoms[0].atom.getXYZ(), p2 = atoms[1].atom.getXYZ(), p3 = atoms[2].atom.getXYZ();
      Measure.getNormalThroughPoints(p1, p2, p3, vNorm, vTemp);
      vTemp.setT((atoms[3].atom == null ? atom : atoms[3].atom).getXYZ());
      vTemp.sub(p1);
      return (vTemp.dot(vNorm) > 0 ? STEREO_R : STEREO_S);
    }

    /**
     * Just a simple signum for integers
     * 
     * @param score
     * @return 0, -1, or 1
     */
    public int sign(int score) {
      return (score < 0 ? -1 : score > 0 ? 1 : 0);
    }

    @Override
    public Object clone() {
      CIPAtom a = null;
      try {
        a = (CIPAtom) super.clone();
      } catch (CloneNotSupportedException e) {
      }
      a.id = ptID++;
      a.atoms = new CIPAtom[4];
      a.priorities = new int[4];
      a.htPathPoints = htPathPoints;
      a.doCheckPseudo = false;
      a.alkeneParent = null;
      //a.rule4Count = null;
      //a.rule4List = null;
      for (int i = 0; i < 4; i++) {
        //        a.priorities[i] = priorities[i];
        if (atoms[i] != null) {
          a.atoms[i] = atoms[i];
        }
      }
      a.ties = null;
//      if (Logger.debugging)
//        Logger.info("cloning " + this + " as " + a);
      return a;

    }

    @Override
    public String toString() {
      return (atom == null ? "<null>" : "[" + currentRule + "." + sphere + ","
          + rootDistance + "." + id + "." + atom.getAtomName()
          + (isDuplicate ? "*" : "") + "]");
    }

  }

}
