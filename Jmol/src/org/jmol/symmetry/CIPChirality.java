/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2017  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.symmetry;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

import org.jmol.java.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Elements;
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
 * - deeply validated
 * 
 * - included revised Rules 1b, and 2. 
 * 
 * - implemented in Java (Jmol) and JavaScript (JSmol)
 * 
 * - only two Java classes; roughly 1000 lines
 * 
 * - efficient, one-pass process for each center using a single finite digraph
 * for all auxiliary descriptors
 * 
 * - exhaustive processing of all 8 sequence rules (1a, 1b, 2, 3, 4a, 4b, 4c, 5)
 * 
 * - includes R/S, r/s, M/P (axial, not planar), E/Z
 * 
 * - covers any-length odd and even cumulenes
 * 
 * - uses Jmol conformational SMARTS to detect atropisomers and helicenes
 * 
 * - covers chiral phosphorus and sulfur, including trigonal pyramidal and
 * tetrahedral
 * 
 * - properly treats complex combinations of R/S, M/P, and seqCis/seqTrans
 * centers (Rule 4b)
 * 
 * - properly treats neutral-species resonance structures using fractional
 * atomic mass and a modified Rule 1b
 * 
 * - implements CIP spiro rule (BB P-93.5.3.1)
 * 
 * - detects small rings (fewer than 8 members) and removes E/Z specifications
 * for such
 * 
 * - detects chiral bridgehead nitrogens and E/Z imines and diazines
 * 
 * - reports atom descriptor along with the rule that ultimately decided it
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
 * Mikko Vainio, John Mayfield (aka John May), Wolf Ihlenfeldt, and Egon
 * Willighagen, for encouragement, examples, serious skepticism, and extremely
 * helpful advice.
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
 * Mata(1993) Paulina Mata, Ana M. Lobo, Chris Marshall, A.Peter Johnson The CIP
 * sequence rules: Analysis and proposal for a revision, Tetrahedron: Asymmetry,
 * Volume 4, Issue 4, April 1993, Pages 657-668
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
 * 7/19/17 Jmol 14.20.3 fixing Rule 2 (880 lines)
 * 7/13/17 Jmol 14.20.3 more thorough spiro testing (858 lines) 
 * 7/10/17 Jmol 14.20.2 adding check for C3 and double
 * spiran (CIP 1966 #32 and #33) 7/8/17 Jmol 14.20.2 adding presort for Rules 4a
 * and 4c (test12.mol; 828 lines)
 * 
 * 7/7/17 Jmol 14.20.1 minor coding efficiencies (833 lines)
 * 
 * 7/6/17 Jmol 14.20.1 major rewrite to correct and simplify logic; full
 * validation for 433 structures (many duplicates) in AY236, BH64, MV64, MV116,
 * JM, and L (836 lines)
 * 
 * 6/30/17 Jmol 14.20.1 major rewrite of Rule 4b (999 lines)
 * 
 * 6/25/17 Jmol 14.19.1 minor fixes for Rule 4b and 5 for BH64_012-015; better
 * atropisomer check
 * 
 * 6/12/2017 Jmol 14.18.2 tested for Rule 1b sphere (AY236.53, 163, 173, 192);
 * 957 lines
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
 * Added logic to Rule 1b:
 * 
 * Rule 1b: In comparing duplicate atoms, the one with lower root distance has
 * precedence, where root distance is defined as: (a) in the case of
 * ring-closure duplicates, the sphere of the duplicated atom; and (b) in the
 * case of multiple-bond duplicates, the sphere of the atom to which the
 * duplicate atom is attached.
 * 
 * Rationale: Using only the distance of the duplicated atom (current
 * definition) introduces a Kekule bias, which can be illustrated with various
 * simple models. By moving that distance to be the sphere of the parent atom of
 * the duplicate, the problem is resolved.
 * 
 * Added clarification to Rule 2:
 * 
 * Rule 2: Higher mass precedes lower mass, where mass is defined in the 
 * case of nonduplicate atoms with identified isotopes for elements as 
 * their exact isotopic mass and, in all other cases, as their 
 * element's atomic weight.
 * 
 * Rationale: BB is not self-consistent, including both "mass number" (in the rule)
 * and "atomic mass" in the description, where "79Br < Br < 81Br". And again we have the
 * same Kekule-ambiguous issue as in Rule 1b. The added clarification fixes the Kekule
 * issue (not using isotope mass number for duplicate atoms), solves the problem 
 * that F < 19F (though 100% nat. abundance), and is easily programmable. 
 * 
 * In Jmol the logic is very simple, actually using the isotope mass number, actually,
 * but doing two checks:
 * 
 * a) if one of five specific isotopes, reverse the test, and
 * b) if on the list of 100% natural isotopes or one of the non-natural elements, 
 *    use the average atomic mass. 
 *    
 * See CIPAtom.getMass();
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

  //
  // Rule 1b proposal
  //

  // Rule 1b: In comparing duplicate atoms, the one with lower root distance has 
  // precedence, where root distance is defined as: (a) in the case of ring-closure 
  // duplicates, the sphere of the duplicated atom; and (b) in the case of 
  // multiple-bond duplicates, the sphere of the atom to which the duplicate atom 
  // is attached.

  // [0]---1---2---3==4 
  //   
  // 
  // current:
  // 
  //                (4)(3)
  //                /  /
  // [0]---1---2---3--4 
  //
  // 
  // proposed:
  // 
  //                (3)(4)
  //                /  /  
  // [0]---1---2---3--4   
  //
  // 
  //               7--6                        7==6
  //              //   \                      /    \
  //              8     5                    8      5  
  //           (ri-ng) /                  (ri=ng) //
  // [0]---1---2---3==4        [0]---1---2---3---4
  //   
  // 
  // current:
  // 
  //                (4)(3)                    (8)(5)
  //                /  /                      /  /
  // [0]---1---2---3--4        [0]---1---2---3--4
  //
  // 
  // proposed:
  // 
  //                (3)(4)                    (3)(4)
  //                /  /                      /  /
  // [0]---1---2---3--4        [0]---1---2---3--4
  // 

  //
  // Implementation Notes
  //
  //

  // "Scoring" a vs. b involves returning 0 (TIE) or +/-n, where n>0 indicates b won, n < 0
  // indicates a won, and the |n| indicates in which sphere the decision was made. 
  // The basic strategy is to loop through all eight sequence rules (1a, 1b, 2, 3, 4a, 4b, 4c, and 5) 
  // in order and exhaustively prior to applying the next rule:
  //
  // Rule 1a (atomic number -- note that this requires an aromaticity check first)
  // Rule 1b (duplicated atom progenitor root-distance check; revised as described above
  //         for aromatic systems.
  // Rule 2  (nominal isotopic mass)
  // Rule 3  (E/Z, not including enantiomorphic seqCis/seqTrans designations)
  // Rule 4a (chiral precedes pseudochiral precedes nonstereochemical)
  // Rule 4b (like precedes unlike)
  // Rule 4c (r precedes s)
  // Rule 5  (R precedes S; M precedes P; C precedes T)
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
  // 5. Rule 4a needs to be addressed exhaustively prior to Rules 4b and 4c. This involves
  //            the initial generation of all auxiliary descriptors, including r/s and R/S at
  //            branching points. In the course of doing this, all rules, 1-5, must be employed
  //            at these auxiliary centers using the already-created digraph. This rule serves to
  //            avoid the need for Rule 4b for all except the most unusual cases, where, for example,
  //            there are two otherwise identical branches, but one can be identified as S and the
  //            other only r or no-stereo, but not R. Thus, only branches that end up as R/R, R/S, S/S,
  //            r/r, r/s, s/s, or nst/nst comparisons need be investigated by Rule 4b.  
  // 6. Rule 4b This rule filters out all diastereomorphic differences that do not involve r/s issues.
  //            Somehow missed in the discussion is that the reference descriptor is determined
  //            once and only once for each branch from the center under scrutiny. The key is to 
  //            determine two "Mata sequences" of R and S descriptors, one for each pair of branches being 
  //            considered. This same reference carries through all future iterations of the algorithm 
  //            for that branch.
  // 7. Rule 4c Again, this subrule must be invoked only after Rule 4b is completed, and again
  //            it is only for the root branches, not anywhere else. It filters out any remaining
  //            diastereomorphic differences based on r/s/ns branch assignments.
  // 8. Rule 5  Final setting of pseudoasymmetry (r/s, m/p) is done along the same line as Rule 4b,
  //            but in this case by setting the reference descriptor to "R" for both sequences.
  //

  /**
   * The basic idea is to switch from a tree metaphor to a "twisted strand" or
   * "thread" metaphor. For example:
   * 
   * (a) In Rule 1b, all ring-duplicates terminate on one of the nodes in the
   * sequence of parent nodes going back to the root. This has nothing to do
   * with branching.
   * 
   * (b) Generation of auxiliary descriptors prior to implementation of Rule 4
   * must start from the highest sphere, proceeding toward the root. In this
   * process the path leading back to the root will have no stereodescriptors,
   * but that does not matter, as its priority is guaranteed to be set by Rule
   * 1a.
   * 
   * (c) All auxiliary nodes can be normalized by labeling them one of
   * {R,S,r,s}; there is no need to consider them to be C/T (seqCis or
   * seqTrans), M/P, or m/p, other than to immediately equate that to R/S or
   * r/s. Note that C/T and M/P must be assigned to the sp2 node closest to the
   * root.
   * 
   * (d) Rule 4b can be analyzed using a "thread" metaphor in a five-step
   * process:
   * 
   * (d1) Generate a set of n threads leading from the root and terminating on
   * highest-ranking stereogenic centers. All nodes must be included, not just
   * stereogenic centers.
   * 
   * (d2) Determine the reference descriptors.
   * 
   * (d3) Sort these threads by priority (including that determined by Rule 4a)
   * and reference descriptors.
   * 
   * (d4) Note that the data can be seen as two n x m matrices, where the rows
   * are the threads. Now "flatten" the data to produce two 1D sequences of
   * descriptors by simply reading out the data in column order.
   * 
   * (d5) Prioritize these two sequences, looking for the first diastereotopic
   * difference.
   * 
   * (e) Rule 5 processing is just a repeat of Rule 4b processing, where the
   * reference descriptor is now set to "R".
   * 
   * (f) Tests for root-only spiro cases must be done along with each rule's
   * processing prior to continuing to the next rule. This is done by looking
   * for situations where there are two sets of matched priorities. These will
   * be broken by the axial nature of spiro connections.
   * 
   * (g) A test for root-only double enantiotopic cases (RSR'S') must be done
   * after Rule 5, allowing for the possibility for this test to return R/S or
   * M/P, not just r/s and m/p.
   * 
   * Jmol's threads in Step d1 are just strings. Jmol carries out Steps d1 and
   * d2 simultaneously with auxiliary descriptor generation, tracking the sphere
   * and priority of all auxiliary descriptors as it generates them. Sorting in
   * Step d3 is done in Jmol using a simple java Array.sort(); no actual
   * matrices are involved.
   * 
   * Finally, the "like/unlike" business can be thought of as a simple
   * diastereotopic test. Thus, the strings are tested for the first point at
   * which they become neither identical nor opposites, and only that point need
   * be checked for likeness or unlikeness to the reference. I like thinking
   * about it this way because it focuses on what Rule 4b's role is -- the
   * identification of diastereomorphism. Rule 4c takes care of
   * diasteriomorphism related to enantiomorphic (r/s, m/p) sub-paths; Rule 5
   * finally takes care of any remaining enantiomorphic issues, including the
   * possibilty that two enantiomorphic pairs are present.
   */

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
  //
  // approximate line count calculated using MSDOS batch script:
  //
  //  type CIPChirality.java | find /V "}"|find /V "//" |find /V "import" | find /V "*" |find ";"|find /V "Logger"|find /V "System.out"  > t
  //  type CIPChirality.java | find /V "}"|find /V "//" |find /V "import" | find /V "*" |find "if"|find /V "Logger"|find /V "System.out"  >> t
  //  type CIPChirality.java | find "COUNT_LINE"  >> t
  //  type t |find " " /C

  static final String RULE_2_nXX_EQ_XX = ";9Be;19F;23Na;27Al;31P;45Sc;55Mn;59Co;75As;89Y;93Nb;98Tc;103Rh;127I;133Cs;141Pr;145Pm;159Tb;165Ho;169Tm;197Au;209Bi;209Po;210At;222Rn;223Fr;226Ra;227Ac;231Pa;232Th;and all > U (atomno > 92)";
  static final String RULE_2_nXX_REV_XX = ";4He;16O;52Cr;96Mo;175Lu;";

  static final int NO_CHIRALITY = 0, TIED = 0;
  static final int A_WINS = -1, B_WINS = 1;
  static final int DIASTEREOMERIC = -3;
  static final int DIASTEREOMERIC_A_WINS = -2, DIASTEREOMERIC_B_WINS = 2;
  static final int ENANTIOMERIC_A_WINS = -3, ENANTIOMERIC_B_WINS = 3;

  static final int IGNORE = Integer.MIN_VALUE;

  static final int UNDETERMINED = -1;

  static final int STEREO_R = JC.CIP_CHIRALITY_R_FLAG,
      STEREO_S = JC.CIP_CHIRALITY_S_FLAG;
  static final int STEREO_M = JC.CIP_CHIRALITY_M_FLAG,
      STEREO_P = JC.CIP_CHIRALITY_P_FLAG;
  static final int STEREO_Z = JC.CIP_CHIRALITY_Z_FLAG,
      STEREO_E = JC.CIP_CHIRALITY_E_FLAG;

  static final int STEREO_BOTH_RS = STEREO_R | STEREO_S; // must be the number 3
  static final int STEREO_BOTH_EZ = STEREO_E | STEREO_Z;

  static final int RULE_1a = 1, RULE_1b = 2, RULE_2 = 3, RULE_3 = 4,
      RULE_4a = 5, RULE_4bTEST = 6, RULE_4b = 7, RULE_4c = 8, 
      RULE_5 = 9,
      RULE_6 = 10;

  boolean isRule4TEST = false;

  static String prefixString = ".........."; // Logger only

  static Integer zero = Integer.valueOf(0);

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

  /**
   * Rule 1b Option 0: IUPAC 2013
   */
  final static int RULE_1b_TEST_OPTION_0_UNCHANGED = 0;

  /**
   * Rule 1b Option A: assign multiple-bond duplicates to parent sphere
   */
  final static int RULE_1b_TEST_OPTION_A_PARENT = 1;

  /**
   * Rule 1b Option B: assign multiple-bond duplicates to own sphere
   */
  final static int RULE_1b_TEST_OPTION_B_SELF = 2;

  /**
   * Rule 1b Option C: do not consider multiple-bond duplicates in Rule 1b
   */
  final static int RULE_1b_TEST_OPTION_C_NONE = 3;

  /**
   * Rule 1b Option D: assign multiple-bond duplicates to own sphere only if
   * Kekule-ambiguous
   */
  final static int RULE_1b_TEST_OPTION_D_SELF_KEKULE = 4;

  /**
   * a test for different Rule 1b options.
   * 
   */
  int rule1bOption = RULE_1b_TEST_OPTION_A_PARENT;
  //  int rule1bOption = RULE_1b_TEST_OPTION_0_UNCHANGED; AY236.215, BH64.1,2,...

  /**
   * return auxiliary chirality settings for all atoms when only one atom is
   * selected and TESTFLAG1 has been set TRUE in Jmol
   * 
   */
  boolean setAuxiliary;
  public boolean haveIsotopes;

  public CIPChirality() {
    // for reflection
    System.out.println("TESTING Rule 1b option " + rule1bOption);
    isRule4TEST = false;
    System.out.println("TESTING Rule 4b option " + isRule4TEST);
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
   * @param setAuxiliary
   *        also set auxiliary (single-atom only)
   */
  public void getChiralityForAtoms(SimpleNode[] atoms, BS bsAtoms,
                                   BS bsAtropisomeric, BS bsHelixM,
                                   BS bsHelixP, boolean setAuxiliary) {
    if (bsAtoms.isEmpty())
      return;
    init();
    this.setAuxiliary = (setAuxiliary && bsAtoms.cardinality() == 1);
    this.bsAtropisomeric = (bsAtropisomeric == null ? new BS()
        : bsAtropisomeric);

    // using BSAtoms here because we need the entire graph,
    // including multiple molecular units (AY-236.93

    BS bs = BSUtil.copy(bsAtoms);
    lstSmallRings = new Lst<BS>();
    while (!bs.isEmpty())
      // COUNT_LINE
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
      int c = getAtomChiralityLimited(a, null, null);
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
          && couldBeChiralAlkene(atoms[i], null) != UNDETERMINED)
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
      System.out.println("?? too many bonds! " + a);
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
      return UNDETERMINED;
    case 2:
      // imines and diazines
      if (a.getElementNumber() != 7) // nitrogen
        return UNDETERMINED;
      break;
    case 3:
      // first-row only (IUPAC 2013.P-93.2.4)
      if (!isFirstRow(a))
        return UNDETERMINED;
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
          return UNDETERMINED;
        if (b != null && (other != b || b.getCovalentBondCount() == 1)) {
          // could be allene central, but I think this is not necessary
          return UNDETERMINED;
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
    (root = new CIPAtom().create(atom, null, false, false, false))
        .addSmallRings(bs);
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
    return Measure.distanceToPlane(plane,
        (pts[3] == null ? a.getXYZ() : pts[3]));
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
        c = setBondChirality(atom, atom1, atom, atom1, true);
      } else if (bond.getCovalentOrder() == 2) {
        atom1 = getLastCumuleneAtom(bond, atom, null, null);
        index1 = atom1.getIndex();
        if (index1 < index)
          continue;
        c = getBondChiralityLimited(bond, atom);
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
  private SimpleNode getLastCumuleneAtom(SimpleEdge bond, SimpleNode atom,
                                         int[] nSP2, SimpleNode[] parents) {
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
    while (true) { // COUNT_LINE
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
   *        ignored if a is not null (just checking ene end top priority)
   * @param cipAtom
   *        ignored if atom is not null
   * @param parentAtom
   *        null for tetrahedral, other alkene carbon for E/Z
   * 
   * @return if and E/Z test, [0:none, 1: atoms[0] is higher, 2: atoms[1] is
   *         higher] otherwise [0:none, 1:R, 2:S]
   */
  private int getAtomChiralityLimited(SimpleNode atom, CIPAtom cipAtom,
                                      SimpleNode parentAtom) {
    int rs = NO_CHIRALITY;
    try {
      boolean isAlkeneEndCheck = (atom == null);
      if (isAlkeneEndCheck) {
        // This is an alkene end determination.
        atom = (root = cipAtom).atom;
        cipAtom.htPathPoints = (cipAtom.parent = new CIPAtom().create(
            parentAtom, null, true, false, false)).htPathPoints;
      } else {
        if (!(root = cipAtom = new CIPAtom().create(atom, null, false, false,
            false)).canBePseudo) {
          // This is a root-atom call. 
          // Just checking here that center has 4 covalent bonds or is trigonal pyramidal.
          return NO_CHIRALITY;
        }
        haveIsotopes = false;
      }
      if (cipAtom.setNode()) {
        for (currentRule = RULE_1a; currentRule <= RULE_5; currentRule++) {

          if (Logger.debugging)
            Logger.info("-Rule " + getRuleName() + " CIPChirality for "
                + cipAtom + "-----"); // Logger
          switch (currentRule) {
          case RULE_4bTEST:
          case RULE_4b:
            if (isRule4TEST != (currentRule == RULE_4bTEST))
              continue;
            break;
          case RULE_4a:
          case RULE_4c:
            if (currentRule == RULE_4a) {
              cipAtom.createRule4AuxiliaryData(null, null);
              if (cipAtom.rule4Type == 0) {
                // we can skip Rules 4a - 5 if there are no chirality centers
                break;
              }
            }
            cipAtom.sortSubstituents(Integer.MIN_VALUE);
          }

          // initial call to sortSubstituents does all, recursively

          if (cipAtom.sortSubstituents(0)) {
            if (Logger.debugging) {
              Logger.info(currentRule + ">>>>" + cipAtom);
              for (int i = 0; i < cipAtom.bondCount; i++) { // Logger
                if (cipAtom.atoms[i] != null) // Logger
                  Logger.info(cipAtom.atoms[i] + " " + cipAtom.priorities[i]); // Logger
              }
            }

            // If this is an alkene end check, we just use STERE_S and STEREO_R as markers

            if (isAlkeneEndCheck)
              return (cipAtom.atoms[0].isDuplicate ? 2 : 1);

            rs = cipAtom.checkHandedness()
                | (currentRule == RULE_5 && cipAtom.canBePseudo ? JC.CIP_CHIRALITY_PSEUDO_FLAG
                    : 0);
            if (Logger.debugging)
              Logger.info(atom + " " + JC.getCIPChiralityName(rs) + " by Rule "
                  + getRuleName() + "\n----------------------------------"); // Logger
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
   * @return one of: {NO_CHIRALITY | STEREO_Z | STEREO_E | STEREO_Ra | STEREO_Sa
   *         | STEREO_ra | STEREO_sa}
   */
  private int getBondChiralityLimited(SimpleEdge bond, SimpleNode a) {
    if (Logger.debugging)
      Logger.info("get Bond Chirality " + bond);
    if (a == null)
      a = bond.getOtherNode(null);
    if (couldBeChiralAlkene(a, bond.getOtherNode(a)) == UNDETERMINED)
      return NO_CHIRALITY;
    int[] nSP2 = new int[1];
    SimpleNode[] parents = new SimpleNode[2];
    SimpleNode b = getLastCumuleneAtom(bond, a, nSP2, parents);
    boolean isAxial = nSP2[0] % 2 == 1;
    return setBondChirality(a, parents[0], parents[1], b, isAxial);
  }

  /**
   * Determine the axial or E/Z chirality for the a-b bond.
   * 
   * @param a
   * @param pa
   * @param pb
   * @param b
   * @param isAxial
   * @return one of: {NO_CHIRALITY | STEREO_Z | STEREO_E | STEREO_M | STEREO_P |
   *         STEREO_m | STEREO_p}
   */
  private int setBondChirality(SimpleNode a, SimpleNode pa, SimpleNode pb,
                               SimpleNode b, boolean isAxial) {
    CIPAtom a1 = new CIPAtom().create(a, null, true, false, false);
    int atop = getAlkeneEndTopPriority(a1, pa, isAxial);
    int ruleA = currentRule;
    CIPAtom b2 = new CIPAtom().create(b, null, true, false, false);
    int btop = getAlkeneEndTopPriority(b2, pb, isAxial);
    int ruleB = currentRule;
    int c = (atop >= 0 && btop >= 0 ? getEneChirality(b2.atoms[btop], b2, a1,
        a1.atoms[atop], isAxial, true) : NO_CHIRALITY);
    if (c != NO_CHIRALITY
        && (isAxial || !bsAtropisomeric.get(a.getIndex())
            && !bsAtropisomeric.get(b.getIndex()))) {
      if (isAxial && ((ruleA == RULE_5) != (ruleB == RULE_5))) {
        // only one of the ends may be enantiomeric to make this r or s 
        // see AY236.70 and AY236.170
        //
        // Now we must check maxRules. If [5,5], then we have
        // 
        //    R      R'
        //     \    /
        //       ==
        //     /    \
        //    S      S'
        //
        // planar flip is unchanged, and this is c/t (ignored here)
        // 
        // 
        //    R      R
        //     \    /
        //       ==
        //     /    \
        //    S      S
        //
        // planar flip is unchanged; also c/t (ignored here)
        // 

        c |= JC.CIP_CHIRALITY_PSEUDO_FLAG;
      }
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
        || top2.atom == null ? NO_CHIRALITY : isAxial ? (isPos(top1, end1,
        end2, top2) ? STEREO_P : STEREO_M)
        : (isCis(top1, end1, end2, top2) ? STEREO_Z : STEREO_E));
  }

  /**
   * Alkene end check for final bond chirality, not auxiliary.
   * 
   * @param a
   * @param pa
   * @param isAxial
   * @return -1, 0, or 1
   */
  private int getAlkeneEndTopPriority(CIPAtom a, SimpleNode pa, boolean isAxial) {
    a.canBePseudo = isAxial;
    return getAtomChiralityLimited(null, a, pa) - 1;
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
     * unique ID for this CIPAtom for debugging only
     * 
     */
    private int id;

    /**
     * bond distance from the root atom to this atom
     */
    private int sphere;

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
     * current priority of this atom, 0-3
     */

    private int priority;

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
    private boolean isTerminal;

    /**
     * is one atom of a double bond
     */

    private boolean isAlkene;

    /**
     * the associated Jmol (or otherwise) atom; use of the SimpleNode interface
     * allows us to implement this in SMILES or Jmol as well as providing an
     * interface other programs could use if implementing this code
     * 
     */
    SimpleNode atom;

    /**
     * the application-assigned unique atom index for this atom; used in
     * updating lstSmallRings
     * 
     */
    private int atomIndex;

    /**
     * true atom covalent bond count; cached for better performance
     */
    int bondCount;

    /**
     * Rule 1a nominal element number; may be fractional for Kekule issues
     */
    private float elemNo;

    /**
     * Rule 2 isotope mass number if identified or average atomic mass if not
     * 
     * C (12.011) > 12C, O (15.999) < 16O, and F (18.998) < 19F
     * 
     * Source:
     * 
     */
    private float mass = UNDETERMINED;

    ///// SUBSTITUENTS ////

    /**
     * direct ancestor of this atom
     * 
     */
    CIPAtom parent;

    /**
     * sphere-1 node in this atom's root branch
     */
    private CIPAtom rootSubstituent;

    /**
     * a count of how many 1H atoms we have found on this atom; used to halt
     * further processing of this atom
     */
    private int h1Count;

    /**
     * the substituents -- up to four supported here at this time
     * 
     */
    CIPAtom[] atoms = new CIPAtom[4];

    /**
     * number of substituent atoms (non-null atoms[] entries)
     */
    private int nAtoms;

    /**
     * bitset of indices of the associated atoms in the path to this atom node
     * from the root; used to keep track of the path to this atom in order to
     * prevent infinite cycling; the last atom in the path when cyclic is a
     * duplicate atom.
     * 
     */
    private BS bsPath;

    /**
     * String path, for debugging
     * 
     */
    private String myPath = "";

    /**
     * priorities associated with each subsituent from high (0) to low (3); due
     * to equivaliencies at a given rule level, these numbers may duplicate and
     * have gaps - for example, [0 2 0 3]
     */
    int[] priorities = new int[4];

    /**
     * the number of distinct priorities determined for this atom for the
     * current rule; 0-4 for the root atom; 0-3 for all others
     */
    private int nPriorities;

    /**
     * pointer to this branch's spiro end atom if it is found to be spiro
     */

    private CIPAtom spiroEnd;

    private int nSpiro;

    /**
     * Rule 1b hash table that maintains distance of the associated
     * nonduplicated atom node
     * 
     */
    Map<Integer, Integer> htPathPoints;

    /////// double and triple bonds ///////

    /**
     * first atom of an alkene or cumulene atom
     */

    private CIPAtom alkeneParent;

    /**
     * last atom of an alkene or cumulene atom
     */

    private CIPAtom alkeneChild;

    /**
     * a flag used in Rule 3 to indicate the second carbon of a double bond
     */

    private boolean isAlkeneAtom2;

    /**
     * is an atom that is involved in more than one Kekule form
     */
    private boolean isKekuleAmbiguous;

    /**
     * first =X= atom in a string of =X=X=X=...
     */
    private CIPAtom nextSP2;

    /**
     * potentially useful information that this duplicate is from an double- or
     * triple-bond, not a ring closure
     */
    private boolean multipleBondDuplicate;

    /**
     * alkene or even cumulene, so chirality will be EZ, not MP
     */
    private boolean isEvenEne = true;

    //// AUXILIARY CHIRALITY for Rules 4 and 5 /////

    /**
     * already-determined auxiliary chirality E/Z for this atom node; this value
     * must be cleared after Rule 3 if continuing
     */
    private int auxEZ = UNDETERMINED;

    /**
     * a flag set false in evaluation of Rule 5 to indicate that there was more
     * than one R/S decision made, so this center cannot be r/s; initially just
     * indicates that the atom has 4 covalent bonds or is trigonal pyriamidal
     */
    boolean canBePseudo = true;

    /**
     * auxiliary chirality as determined in createAuxiliaryRule4Data;
     * possibilities include R/S, r/s, M/P, m/p, C/T (but not c/t), or ~ (ASCII
     * 126, no stereochemistry); for sorting purposes C=M=R < p=r=s < ~
     */
    private char auxChirality = '~';

    /**
     * points to next branching point that has two or more chiral branches
     */
    private CIPAtom nextChiralBranch;

    /**
     * [sphere, nR, nS] -- tracks the number of R and S centers for the lowest
     * sphere
     */
    private Object[] rule4Count;

    /**
     * a list that tracks stereochemical paths for this branch section for Mata
     * analysis in the form of pAAAAA where p is 0-3, the priority up through
     * Rule 4a, and A is one of R, S, M, P, C, T, r, s, m, p, where C = seqCis;
     * T = seqTrans; seqcis and seqtrans are irrelevant)
     * 
     */
    private String[] rule4List;

    /**
     * a list of string buffers that tracks full-length stereochemical paths for
     * the branch's root atom only for Mata analysis in the form of
     * p1p2p3XXXXXYYYYZZZZZ where pn is 1-4, the priority up through Rule 4a;
     * used for the final flattening of the ligand path for like/unlike analysis
     * 
     */

    private Lst<String[]> rootRule4Paths;

    /**
     * a list of priorities from the root leading this branch; used to build
     * rootRule4Paths
     * 
     */
    private String priorityPath;

    /**
     * for the root atom, the number of auxiiary centers; for other atoms, the
     * auxiiary chirality type: 0: ~, 1: R, 2: S; normalized to R/S even if M/P
     * or C/T
     */
    int rule4Type;

    private boolean rule4checkIdentical = true;

    private char rule4Ref = 'R';

    private BS bsTemp = new BS();

    // flag to reverse the Rule 2 result for a few isotope types

    private boolean reverseRule2;

    // flag for C3-symmetry checking reference atom index
    
    private int c3RefIndex = -1;
    private int c4RefIndex = -1;

    CIPAtom() {
      // had a problem in JavaScript that the constructor of an inner function cannot
      // access this.b$ yet. That assignment is made after construction.
    }

    /**
     * 
     * @param atom
     *        or null to indicate a null placeholder
     * @param parent
     * @param isAlkene
     * @param isDuplicate
     * @param isParentBond
     * @return this
     */
    @SuppressWarnings("unchecked")
    CIPAtom create(SimpleNode atom, CIPAtom parent, boolean isAlkene,
                   boolean isDuplicate, boolean isParentBond) {
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
      bondCount = atom.getCovalentBondCount();
      canBePseudo = (bondCount == 4 || bondCount == 3 && !isAlkene
          && (elemNo > 10 || bsAzacyclic != null && bsAzacyclic.get(atomIndex)));
      if (parent != null)
        sphere = parent.sphere + 1;
      if (sphere == 1) {
        rootSubstituent = this;
        htPathPoints = new Hashtable<Integer, Integer>();
      } else if (parent != null) {
        rootSubstituent = parent.rootSubstituent;
        htPathPoints = (Map<Integer, Integer>) ((Hashtable<Integer, Integer>) parent.htPathPoints)
            .clone();
      }
      bsPath = (parent == null ? new BS() : BSUtil.copy(parent.bsPath));

      multipleBondDuplicate = isDuplicate;

      rootDistance = sphere;
      // The rootDistance for a nonDuplicate atom is just its sphere.
      // The rootDistance for a duplicate atom is (by IUPAC) the sphere of its duplicated atom.
      // I argue that for aromatic compounds, this introduces a Kekule problem and that for
      // those cases, the rootDistance should be its own sphere, not the sphere of its duplicated atom.
      // This shows up in AV-360#215. 

      if (parent == null) {
        // original atom
        bsPath.set(atomIndex);
      } else if (multipleBondDuplicate
          && (rule1bOption == RULE_1b_TEST_OPTION_D_SELF_KEKULE
              && isKekuleAmbiguous || rule1bOption == RULE_1b_TEST_OPTION_B_SELF)) {
        // just leaving the rootDistance to be as for other atoms
      } else if (multipleBondDuplicate
          && rule1bOption == RULE_1b_TEST_OPTION_A_PARENT) {
        rootDistance--;
      } else if (atom == root.atom) {
        // pointing to original atom
        isDuplicate = true;
        rootDistance = 0;
        root.nSpiro++;
        if (rootSubstituent.spiroEnd == null)
          rootSubstituent.spiroEnd = parent;
      } else if (bsPath.get(atomIndex)) {
        isDuplicate = true;
        rootDistance = (isParentBond ? parent.sphere : htPathPoints.get(
            Integer.valueOf(atomIndex)).intValue());
      } else {
        bsPath.set(atomIndex);
        htPathPoints.put(Integer.valueOf(atomIndex),
            Integer.valueOf(rootDistance));
      }
      this.isDuplicate = isDuplicate;
      if (Logger.debuggingHigh) {
        if (sphere < MAX_PATH) // Logger
          myPath = (parent != null ? parent.myPath + "-" : "") + this; // Logger
        Logger.info("new CIPAtom " + myPath);
      }
      return this;
    }

    /**
     * get the atomic mass only if needed by Rule 2, testing for two special conditions
     * in the case of isotopes:
     * 
     * 1) since we will use the mass number for the isotope, do we need to reverse the finding?
     * 
     * 2) if the is a 100% nat. abundance isotope it is the same as the unlabeled element, so
     * use the average atomic mass instead, just so those two are equal.  
     * 
     * @return mass or mass surragate
     */
    private float getMass() {
      if (mass == UNDETERMINED && (mass = atom.getMass()) == (int) mass) {
        // this is an isotope
        haveIsotopes = true;
        if (isType(RULE_2_nXX_REV_XX))
          reverseRule2 = true;
        else if (elemNo > 92 || isType(RULE_2_nXX_EQ_XX))
          mass = Elements.getAtomicMass((int) elemNo); // just switch to average 
      }
      return mass;
    }

    private boolean isType(String rule2Type) {
      return PT.isOneOf(
          (int) mass + Elements.elementSymbolFromNumber((int) elemNo),
          rule2Type);
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

    //    /**
    //     * Calculate the average element numbers of associated double-bond atoms
    //     * weighted by their most significant Kekule resonance contributor(s). We
    //     * only consider simple benzenoid systems -- 6-membered rings and their
    //     * 6-memebered rings fused to them. Calculated for the parent of an
    //     * sp2-duplicate atom.
    //     * 
    //     * @return an averaged element number
    //     */
    //    private float getKekuleMass() {
    //      SimpleEdge[] edges = atom.getEdges();
    //      SimpleEdge bond;
    //      float ave = 0;
    //      int n = 0;
    //      for (int i = edges.length; --i >= 0;)
    //        if ((bond = edges[i]).isCovalent()) {
    //          SimpleNode other = bond.getOtherNode(atom);
    //          if (bsKekuleAmbiguous.get(other.getIndex())) {
    //            n++;
    //            ave += other.getMass();
    //          }
    //        }
    //      return ave / n;
    //    }

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
        // COUNT_LINE
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
      // notice we are setting isSet TRUE here, not just testing it.
      if (isSet || (isSet = true) && isDuplicate)
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
        if (nBonds == 1 && order == 1 && isParentBond)
          return isTerminal = true;
        // from here on, isTerminal indicates an error condition
        switch (order) {
        case 3:
          if (addAtom(pt++, other, isParentBond, false, isParentBond) == null)
            return !(isTerminal = true);
          //$FALL-THROUGH$
        case 2:
          if (addAtom(pt++, other, order != 2 || isParentBond, order == 2,
              isParentBond) == null)
            return !(isTerminal = true);
          //$FALL-THROUGH$
        case 1:
          if (isParentBond
              || addAtom(pt++, other, order != 1 && elemNo <= 10, false, false) != null)
            break;
          //$FALL-THROUGH$
        default:
          return !(isTerminal = true);
        }
      }
      nAtoms = pt;
      for (; pt < atoms.length; pt++)
        atoms[pt] = new CIPAtom().create(null, this, false, true, false);

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
     * @param isParentBond
     * @return new atom or null
     */
    CIPAtom addAtom(int i, SimpleNode other, boolean isDuplicate,
                    boolean isAlkene, boolean isParentBond) {
      if (i >= atoms.length) {
        if (Logger.debugging)
          Logger.info(" too many bonds on " + atom);
        return null;
      }
      if (parent == null) {
        // For top level, we do not allow two 1H atoms.
        if (other.getIsotopeNumber() == 1) {
          if (++h1Count > 1) {
            if (Logger.debuggingHigh)
              Logger.info(" second H atom found on " + atom);
            return null;
          }
        }
      }
      return atoms[i] = new CIPAtom().create(other, this, isAlkene,
          isDuplicate, isParentBond);
    }

    /**
     * Deep-Sort the substituents of an atom, setting the node's atoms[] and
     * priorities[] arrays. Checking for "ties" that will lead to
     * pseudochirality is also done here.
     * 
     * @param sphere
     *        current working sphere; Integer.MIN_VALUE to not break ties
     * @return all priorities assigned
     * 
     */
    boolean sortSubstituents(int sphere) {

      // runs about 20% faster with this check
      if (nPriorities == (sphere == 0 ? 4 : 3))
        return true;

      // Note that this method calls breakTie and is called recursively from breakTie.

      boolean ignoreTies = (sphere == Integer.MIN_VALUE);
      if (ignoreTies) {
        // If this is Rule 4a or 4c, we must presort the full tree
        // Just before Rule4a we must sort all substituents without breaking ties
        if (isTerminal)
          return false;
        if (currentRule == RULE_6) {
          for (int i = 0; i < 4; i++)
            if (atoms[i] != null && !atoms[i].isDuplicate && atoms[i].atom != null
                && atoms[i].setNode())
              atoms[i].sortSubstituents(Integer.MIN_VALUE);          
        } else {
          for (int i = 0; i < 4; i++)
            if (rule4List[i] != null && atoms[i].atom != null
                && !atoms[i].isTerminal)
              atoms[i].sortSubstituents(Integer.MIN_VALUE);
          if (!canBePseudo)
            return false;
        }
      }
      
      ignoreTies |= (currentRule == RULE_4b || currentRule == RULE_5);

      int[] indices = new int[4], prevPrior = new int[4];
      int nPrioritiesPrev = nPriorities;
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
        Logger.info("---" + nPriorities);
      }

      int loser, score;
      for (int i = 0; i < 4; i++) {
        CIPAtom a = atoms[i];
        for (int j = i + 1; j < 4; j++) {
          CIPAtom b = atoms[loser = j];

          // Check:

          // (a) if one of the atoms is a phantom atom (P-92.1.4.1); if not, then
          // (b) if the prioritiy has already been set; if not, then
          // (c) if the current rule decides; if not, then
          // (d) if the tie can be broken in the next sphere

          switch (a.atom == null ? B_WINS : b.atom == null ? A_WINS
              : prevPrior[i] < prevPrior[j] ? A_WINS
                  : prevPrior[j] < prevPrior[i] ? B_WINS
                      : (score = checkPriority(a, b, i, j)) != TIED ? score
                          : ignoreTies ? IGNORE : sign(a
                              .breakTie(b, sphere + 1))) {
          case B_WINS:  
            loser = i;
            //$FALL-THROUGH$
          case A_WINS:
            priorities[loser]++;
            break;
          case TIED:
          case IGNORE:
            break;
          }
          indices[loser]++;
        }
      }

      // update nPriorities and all arrays

      CIPAtom[] newAtoms = new CIPAtom[4];
      int[] newPriorities = new int[4];
      String[] newRule4List = (rule4List == null ? null : new String[4]);
      bsTemp.clearAll(); // track number of priorities
      for (int i = 0; i < 4; i++) {
        int pt = indices[i];
        CIPAtom a = newAtoms[pt] = atoms[i];
        newPriorities[pt] = priorities[i];
        if (rule4List != null)
          newRule4List[pt] = rule4List[i];
        if (a.atom != null)
          bsTemp.set(priorities[i]);
      }
      atoms = newAtoms;
      priorities = newPriorities;
      rule4List = newRule4List;
      nPriorities = bsTemp.cardinality();

      if (parent == null) {

        // Check for special root-only cases:

        //  P-92.2.1.1(c) pseudoasymmetric centers must have 
        //                two and only two enantiomorphic ligands
        //
        //  P-33.5.3.1 spiro compounds have increased constraints

        if (currentRule == RULE_5) {
          if (nSpiro > 0)
            checkSpiro();          
          if (nPriorities == 4 && nPrioritiesPrev == 2) {

          // Rule 5 has decided the issue, but how many decisions did we make?
          // If priorities [0 0 2 2] went to [0 1 2 3] then
          // we have two Rule-5 decisions -- R,S,R',S'.
          // In that case, Rule 5 results in R/S, not r/s.
          //
          //     S
          //     -
          //     -
          // R---C---R'      despite this being Rule 5, the results is R, not r. 
          //     -
          //     -
          //     S'
          //
          // --------- mirror plane
          //
          //     R'
          //     -
          //     -
          // S---C---S'     not superimposible
          //     -
          //     -
          //     R
          // 
          canBePseudo = false;
        }
        }
      }
      if ((Logger.debuggingHigh) && atoms[2].atom != null
          && atoms[2].elemNo != 1) { // Logger
        Logger.info(dots() + atom + " nPriorities = " + nPriorities);
        for (int i = 0; i < 4; i++) { // Logger
          Logger.info(dots() + myPath + "[" + i + "]=" + atoms[i] + " "
              + priorities[i] + " " + Integer.toHexString(priorities[i])); // Logger
        }
        Logger.info(dots() + "-------" + nPriorities);
      }

      // We are done if the number of priorities equals the bond count.

      return (nPriorities == bondCount);
    }

    /**
     * CheckSpiro uses the concept that Andrey Yerin suggested that
     * covers all spiro, double spiro, C3-, C3-symmetric cases from CIP(1966).
     * Incredibly simple!
     * 
     */
    private void checkSpiro() {
      boolean swap23 = false;
      int a = 0, b = -1;
      switch (nPriorities * 10 + priorities[3]) {
      case 10:
        //      // CIP Helv Chim. Acta 1966 #33 -- double spiran
        //      // 0-1,2-3 or 0-1,3-2
        //      // 0-2,1-3 or 
        //      // 0-3,1-2        
        b = 1;
        swap23 = true; // just the way it has to be
        break;
      case 21:
      case 23:
        // CIP Helv. Chim. Acta 1966 #32 
        // [0 1 1 1] or [0 0 0 3]
        a = priorities[1];
        break;
      case 22:
        //
        // We have priorities [0 0 2 2], possibly being spiro
        //
        b = getSpiroEnd(0);
        break;
      default:
        return;
      }
      if (atoms[a].spiroEnd == null)
        return;
      c3RefIndex = atoms[a].atomIndex;
      if (b >= 0)
        c4RefIndex = atoms[b].atomIndex;
      if (sortByRule(RULE_6)) {
        currentRule = RULE_6;
        if (swap23) {
          CIPAtom atom = atoms[2];
          atoms[2] = atoms[3];
          atoms[3] = atom;
        }
      }
    }

    /**
     * Find the other end of a loop to root.
     * 
     * @param i0
     *        index of the spiro starting atom
     * @return pointer to spiro end of atoms[0] -- either -1 (not spiro), 0, 1, 2,
     *         or 3
     */
    private int getSpiroEnd(int i0) {
      CIPAtom a = atoms[i0].spiroEnd;
      if (a != null)
        for (int i = 0; i < 4; i++)
          if (i != i0 && a.atom == atoms[i].atom)
            return i;
      return -1;
    }

    /**
     * Provide an indent for clarity in debugging messages
     * 
     * @return a string of dots based on the value of atom.sphere.
     */
    private String dots() {
      return ".....................".substring(0, Math.min(20, sphere)); // Logger
    }

    /**
     * Break a tie at any level in the iteration between to atoms that otherwise
     * are the same by sorting their substituents.
     * 
     * @param b
     * @param sphere
     *        current working sphere
     * @return [0 (TIED), -1 (A_WINS), or 1 (B_WINS)] * (sphere where broken)
     */
    private int breakTie(CIPAtom b, int sphere) {

      // Two duplicates of the same atom are always tied
      // if they have the same root distance.

      if (isDuplicate && b.isDuplicate && atom == b.atom
          && rootDistance == b.rootDistance)
        return TIED;

      // Do a duplicate check -- if that is not a TIE we do not have to go any further.

      // NOTE THAT THIS CHECK IS NOT EXPLICIT IN THE RULES
      // BECAUSE DUPLICATES LOSE IN THE NEXT SPHERE, NOT THIS ONE.
      // THE NEED FOR (sphere+1) in AY236.53, 163, 173, 192 SHOWS THAT 
      // SUBRULE 1a MUST BE COMPLETED EXHAUSTIVELY PRIOR TO SUBRULE 1b.

      int score = checkIsDuplicate(b);
      if (score != TIED)
        return score * (sphere + 1); // COUNT_LINE

      // return TIED if:

      //  a) one or the other can't be set (because it has too many connections), or
      //  b) both are terminal or both are duplicates

      if (!setNode() || !b.setNode() || isTerminal && b.isTerminal
          || isDuplicate && b.isDuplicate)
        return TIED;

      // We are done if one of these is terminal (for the next sphere, actually).

      if (isTerminal != b.isTerminal)
        return (isTerminal ? B_WINS : A_WINS) * (sphere + 1); // COUNT_LINE

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

      if ((score = compareShallowly(b, sphere)) != TIED) {
        return score;
      }

      // Phase II -- check deeply using breakTie
      //
      // OK, so all three are nominally the same.
      // Now iteratively deep-sort each list based on substituents
      // and then check them one by one to see if the tie can be broken.

      sortSubstituents(sphere);
      b.sortSubstituents(sphere);
      int finalScore = (nAtoms == 0 ? B_WINS : TIED), absScore = Integer.MAX_VALUE;
      for (int i = 0; i < nAtoms; i++) {
        CIPAtom ai = atoms[i], bi = b.atoms[i];
        if ((score = ai.breakTie(bi, sphere + 1)) != TIED) {
          int abs = Math.abs(score);
          if (abs < absScore) {
            absScore = abs;
            finalScore = score;
          }
        }
      }
      return finalScore;
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
        if (score != IGNORE && score != TIED)
          return score * (sphere + 1); // COUNT_LINE
      }
      return TIED;
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
              : (score = checkIsDuplicate(b)) != TIED ? score : isDuplicate
                  && (score = checkRule1b(b)) != TIED ? score : checkRule2(b));
    }

    /**
     * Used in sortSubstituents
     * @param a  
     * @param b
     * @param i 
     * @param j 
     * 
     * @return 0 (TIED), -1 (A_WINS), 1 (B_WINS), or Integer.MIN_VALUE (IGNORE)
     */
    private int checkPriority(CIPAtom a, CIPAtom b, int i, int j) {
      switch (currentRule) {
      case RULE_4b:
      case RULE_5:
        return (rule4List[i] == null && rule4List[j] == null ? IGNORE
            : rule4List[j] == null ? A_WINS : rule4List[i] == null ? B_WINS
                : compareMataPair(a, b));
      }
      int score;
      return ((a.atom == null) != (b.atom == null) ? (a.atom == null ? B_WINS
          : A_WINS) : (score = a.checkCurrentRule(b)) == IGNORE ? TIED : score);
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
     * Sort for auxiliary chirality determination
     * 
     * @param maxRule
     * @return TIED or deciding rule RULE_1a - RULE_5
     */
    private int sortToRule(int maxRule) {
      for (int i = RULE_1a; i <= maxRule; i++)
        if (sortByRule(i))
          return i;
      return TIED;
    }

    /**
     * Sort by a given rule, preserving currentRule.
     * 
     * @param rule
     * @return true if a decision has been made
     */
    private boolean sortByRule(int rule) {
      if ((rule == RULE_4b || rule == RULE_4bTEST)
          && (rule == RULE_4bTEST) != isRule4TEST)
        return false;
      int current = currentRule;
      currentRule = rule;
      if (rule == RULE_6)
        sortSubstituents(Integer.MIN_VALUE);
      boolean isChiral = sortSubstituents(0);
      currentRule = current;
      return isChiral;
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
        return checkRule1a(b);
      case RULE_1b:
        return checkRule1b(b);
      case RULE_2:
        return checkRule2(b);
      case RULE_3:
        return checkRule3(b); // can be IGNORE
      case RULE_4a:
        return checkRules4ac(b, " sr SR PM");
      case RULE_4c:
        return checkRules4ac(b, " s r p m");
      case RULE_4bTEST:
        return checkRule4Test(b);
      case RULE_4b:
      case RULE_5:
        return TIED; // not carried out here because these need access to a full list of ligands
      case RULE_6:
        int score;
        return ((score = checkRule6(b, root.c3RefIndex)) != TIED ? score : root.c4RefIndex >= 0 ? checkRule6(b, root.c4RefIndex) : score);
      }
    }
    
    private int checkRule6(CIPAtom b, int refIndex) {
      return ((atomIndex == refIndex) == (b.atomIndex == refIndex) ? TIED : 
        atomIndex == refIndex ? A_WINS : B_WINS);
    }
    
    private int checkRule4Test(CIPAtom b) {
      // TODO
      boolean isNS = (auxChirality == '~');
      if (rootSubstituent.atomIndex == b.rootSubstituent.atomIndex) {
        // sortSubstituent checking priority within a ligand
        if (isNS || b.auxChirality == '~')
          return (!isNS ? A_WINS : b.auxChirality != '~' ? B_WINS : TIED);
        return (auxChirality == b.auxChirality ? TIED
            : auxChirality == rootSubstituent.rule4Ref ? A_WINS : B_WINS);
      }
      // 
      return (isNS ? TIED
          : (auxChirality == b.auxChirality) == rootSubstituent.rule4checkIdentical ? TIED
              : auxChirality == rootSubstituent.rule4Ref ? A_WINS : B_WINS);
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
          : rule1bOption == RULE_1b_TEST_OPTION_C_NONE
              && (parent.isAlkene || b.parent.isAlkene) ? TIED
              : b.rootDistance != rootDistance ? (b.rootDistance > rootDistance ? A_WINS
                  : B_WINS)
                  : TIED;
    }

    /**
     * Chapter 9 Rule 2. atomic mass, with possible reversal due to use of mass numbers
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRule2(CIPAtom b) {
      return (b.getMass() == getMass() ? TIED : 
        (reverseRule2 || b.reverseRule2) == (b.mass > mass) ? A_WINS : B_WINS);
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
      return isDuplicate || b.isDuplicate || !parent.isAlkeneAtom2
          || !b.parent.isAlkeneAtom2 || !parent.alkeneParent.isEvenEne
          || !b.parent.alkeneParent.isEvenEne ? IGNORE
          : parent == b.parent ? sign(breakTie(b, 0)) : (za = parent
              .getRule3auxEZ()) < (zb = b.parent.getRule3auxEZ()) ? A_WINS
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
    private int getRule3auxEZ() {

      // "this" is the second atom of the alkene, checked as the parent of one of its ligands, 
      // as there is no need to do this test until we are on the branch that includes
      // the atom after the alkene.

      return alkeneParent.auxEZ = (auxEZ != UNDETERMINED ? auxEZ
          : (auxEZ = getAuxEneWinnerChirality(alkeneParent, this, false, RULE_3)) == NO_CHIRALITY ? (auxEZ = STEREO_BOTH_EZ)
              : auxEZ);
    }

    /**
     * Determine the winner on one end of an alkene or cumulene, accepting a max
     * rule of RULE_3 or RULE_5, depending upon the application
     * 
     * @param end1
     * @param end2
     * @param isAxial
     * @param maxRule
     *        RULE_3 or RULE_5
     * @return one of: {NO_CHIRALITY | STEREO_Z | STEREO_E | STEREO_M |
     *         STEREO_P}
     */
    private int getAuxEneWinnerChirality(CIPAtom end1, CIPAtom end2,
                                         boolean isAxial, int maxRule) {
      CIPAtom winner1 = getAuxEneEndWinner(end1, end1.nextSP2, maxRule);
      CIPAtom winner2 = (winner1 == null || winner1.atom == null ? null
          : getAuxEneEndWinner(end2, end2.nextSP2, maxRule));
      return getEneChirality(winner1, end1, end2, winner2, isAxial, false);
    }

    /**
     * Get the atom that is the highest priority of two atoms on the end of a
     * double bond after sorting from Rule 1a through a given rule (Rule 3 or
     * Rule 5)
     * 
     * @param end
     * @param prevSP2
     * @param maxRule
     * @return higher-priority atom, or null if they are equivalent
     */
    private CIPAtom getAuxEneEndWinner(CIPAtom end, CIPAtom prevSP2, int maxRule) {
      CIPAtom atom1 = (CIPAtom) end.clone();
      if (atom1.parent != prevSP2)
        atom1.addReturnPath(prevSP2, end);
      else if (maxRule == RULE_5)
        atom1.rule4List = end.rule4List;
      CIPAtom a;
      for (int i = RULE_1a; i <= maxRule; i++)
        if ((a = atom1.getTopSorted(i)) != null)
          return (a.atom == null ? null : a);
      return null;
    }

    /**
     * Return top-priority non-sp2-duplicated atom.
     * 
     * @param rule
     * 
     * @return highest-priority non-duplicated atom
     */
    private CIPAtom getTopSorted(int rule) {
      if (sortByRule(rule))
        for (int i = 0; i < 4; i++) {
          CIPAtom a = atoms[i];
          if (!a.multipleBondDuplicate)
            return priorities[i] == priorities[i + 1] ? null : atoms[i];
        }
      return null;
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
      while (oldParent.parent != null && oldParent.parent.atoms[0] != null) { // COUNT_LINE
        if (Logger.debuggingHigh)
          Logger.info("path:" + oldParent.parent + "->" + oldParent);
        path.addLast(oldParent = oldParent.parent);
      }
      path.addLast(null);
      for (int i = 0, n = path.size(); i < n; i++) {
        oldParent = path.get(i);
        newSub = (oldParent == null ? new CIPAtom().create(null, this,
            isAlkene, true, false) : (CIPAtom) oldParent.clone());
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
     * Chapter 9 Rules 4b and 5: like vs. unlike; for root substituents only.
     * 
     * (1) Generate full set of branching stereochemical paths (rule4Paths) for
     * each ligand.
     * 
     * (2) Determine the reference descriptor (R, S, or both) for each ligand.
     * 
     * (3) Flatten each path to one string by traversing the sorted list sphere
     * by sphere.
     * 
     * (4) Compare paths using like/unlike criteria.
     * 
     * @param a
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS), or Integer.MIN_VALUE
     *         (IGNORE)
     */
    private int compareMataPair(CIPAtom a, CIPAtom b) {
      // Step 1: Generate paths by building a list of all paths from the root substituent out.
      // For example, say we have the following, where (n) is a priority, if necessary:
      // 
      //          S(1)
      //         /
      // []--S--r
      //         \
      //          R(1)
      //
      // then we build ["SrS" and "SrR"]
      // 

      a.generateRule4Paths(this);
      b.generateRule4Paths(this);

      boolean isRule5 = (currentRule == RULE_5);
      char aref = (isRule5 ? 'R' : a.getRule4ReferenceDescriptor());
      char bref = (isRule5 ? 'R' : b.getRule4ReferenceDescriptor());
      boolean haveRSOptions = (aref == '?');
      if (Logger.debugging)
        Logger.info("reference descriptors are " + aref + " and " + bref);

      int score = TIED;
      String reason = "Rule 4b"; // Logger

      while (true) {
        
        if (aref == '-' && bref == '-')
          return IGNORE;

        // check for RS on only one side
        if ((aref == '?') != (bref == '?')) {
          score = (haveRSOptions ? B_WINS : A_WINS);
          reason += " RS on only one side"; // Logger
          break;
        }

        String aStr = (haveRSOptions ? a.flattenRule4Paths('R') : a
            .flattenRule4Paths(aref));
        String aStr2 = (haveRSOptions ? a.flattenRule4Paths('S') : "");
        String bStr = (haveRSOptions ? b.flattenRule4Paths('R') : b
            .flattenRule4Paths(bref));
        String bStr2 = (haveRSOptions ? b.flattenRule4Paths('S') : "");

        // just return string comparison if Rule 5
        if (isRule5) {
          // note that these two strings cannot be different lengths
          score = sign(haveRSOptions ? (aStr + aStr2).compareTo(bStr + bStr2)
              : aStr.compareTo(bStr));
          reason = "Rule 5"; // Logger
          break;
        }

        aStr = cleanRule4Str(aStr);
        bStr = cleanRule4Str(bStr);

        if (haveRSOptions) {

          // first find the better of A(R)/A(S) and the better of B(R)/B(S)

          if (compareRule4PairStr(aStr, aStr2 = cleanRule4Str(aStr2), true) > 0)
            aStr = aStr2;
          if (compareRule4PairStr(bStr, bStr2 = cleanRule4Str(bStr2), true) > 0)
            bStr = bStr2;

          // then continue as before
        }
        score = compareRule4PairStr(aStr, bStr, false);
        break;

      }

      if (Logger.debugging && (score == A_WINS || score == B_WINS))
        Logger.info((score == A_WINS ? a : b) + " > "
            + (score == A_WINS ? b : a) + " by " + reason + "\n"); // Logger

      // no tie-breaking for Rules 4b or 5

      return (score == TIED ? IGNORE : score);
    }

    /**
     * Combine all subpaths
     * 
     * @param ignore
     *        atom to ignore (parent)
     */
    void generateRule4Paths(CIPAtom ignore) {

      getRule4PriorityPaths("", ignore.atom);
      rootRule4Paths = new Lst<String[]>();
      appendRule4Paths(this, new String[3]);
      getRule4Counts(rule4Count = new Object[] { null, zero, zero,
          Integer.valueOf(10000) });

      if (Logger.debugging) {
        Logger.info("Rule 4b paths for " + this + "=\n");
        for (int i = 0; i < rootRule4Paths.size(); i++) { // Logger
          String s = rootRule4Paths.get(i)[0].toString(); // Logger
          int prefixLen = rootRule4Paths.get(i)[1].length(); // Logger
          while (prefixString.length() < prefixLen)
            // Logger
            prefixString += prefixString; // Logger
          Logger.info(prefixString.substring(0, prefixLen)
              + s.substring(prefixLen) + " " + priorityPath);
        }
        Logger.info("");
      }
    }

    /**
     * Recursively build priority paths to stereocenters as a string.
     * 
     * @param path
     * @param ignore
     *        atom to ignore (parent)
     */
    private void getRule4PriorityPaths(String path, SimpleNode ignore) {
      priorityPath = path + (priority + 1);
      for (int i = 0; i < 4; i++)
        if (rule4List[i] != null && atoms[i].atom != ignore)
          atoms[i].getRule4PriorityPaths(priorityPath, null);
    }

    /**
     * Track the number of R and S in th.e highest-ranking ligand position.
     * 
     * @param counts
     *        ["011020...", nR, nS, sphere]
     */
    private void getRule4Counts(Object[] counts) {
      if (sphere > ((Integer) counts[3]).intValue())
        return;
      if (rule4Type > 0) {
        // Accumlate the number of R and S centers of a given cumlative priority
        int val = sign(priorityPath.length()
            - (counts[0] == null ? 10000 : ((String) counts[0]).length()));
        if (val == 0)
          val = sign(priorityPath.compareTo(counts[0].toString()));
        switch (val) {
        case -1:
          counts[0] = priorityPath;
          counts[STEREO_R] = counts[STEREO_S] = zero;
          counts[3] = Integer.valueOf(sphere);
          //$FALL-THROUGH$
        case 0:
          counts[rule4Type] = Integer.valueOf(((Integer) counts[rule4Type])
              .intValue() + 1);
          break;
        }
        if (Logger.debugging)
          Logger.info(this + " addRule4Ref " + sphere + " " + priority + " "
              + rule4Type + " " + PT.toJSON("rule4Count", counts)); // Logger
      }
      for (int i = 0; i < 4; i++)
        if (rule4List[i] != null)
          atoms[i].getRule4Counts(counts);
    }

    /**
     * Generate the list of all possible paths to stereocenters that Mata
     * analysis needs.
     * 
     * @param rootsub
     * @param pathInfo
     */
    private void appendRule4Paths(CIPAtom rootsub, String[] pathInfo) {
      String s0 = (pathInfo[0] == null ? "" + auxChirality : pathInfo[0]);
      if (pathInfo[2] == null)
        rootsub.rootRule4Paths.addLast(pathInfo = new String[] { s0, "",
            priorityPath });
      boolean isFirst = true;
      for (int i = 0; i < 4; i++)
        if (rule4List[i] != null) {
          if (isFirst)
            pathInfo[2] = priorityPath;
          else
            rootsub.rootRule4Paths.addLast(pathInfo = new String[] { s0, s0,
                priorityPath });
          isFirst = false;
          pathInfo[0] += rule4List[i];
          if (atoms[i].nextChiralBranch != null)
            atoms[i].nextChiralBranch.appendRule4Paths(rootsub, pathInfo);
        }
    }

    /**
     * rule4Count holds in [1] and [2] the number of R and S descriptors,
     * respectively, in the highest ranking sphere with stereochemistry.
     * 
     * @return "R", "S", or "RS"
     */
    private char getRule4ReferenceDescriptor() {
      if (rule4Count == null)
        return auxChirality;
      int nR = ((Integer) rule4Count[1]).intValue(), nS = ((Integer) rule4Count[2])
          .intValue();
      return (nR > nS ? 'R' : nR < nS ? 'S' : nR == 0 ? '-' : '?');
    }

    /**
     * This is the key Mata method -- getting the correct sequence of R and S
     * from a set of diastereomorphic paths.
     * 
     * Given a specific reference descriptor, the task is to sort the paths
     * based on priority and reference. We do the sort lexicographically, simply
     * using Array.sort(String[]) with our reference atom temporarily given the
     * lowest ASCII characater "A" (65).
     * 
     * @param ref
     * @return a string that can be compared with another using like/unlike
     */
    private String flattenRule4Paths(char ref) {
      int nPaths = rootRule4Paths.size();
      String[] paths = new String[nPaths];
      int nMax = 0;
      for (int i = 0; i < nPaths; i++) {
        // remove all enantiomorphic descriptors
        String path = rootRule4Paths.get(i)[0];
        String priorityPath = rootRule4Paths.get(i)[2];
        String s = PT.replaceAllCharacters(path, "srctmp", "~");
        // remove all 
        paths[i] = s = priorityPath + s.replace(ref, 'A');
        if (s.length() > nMax)
          nMax = s.length();
      }
      Arrays.sort(paths);
      // now remove the priorities
      for (int i = 0; i < nPaths; i++) {
        paths[i] = PT.replaceAllCharacters(paths[i], "1234", "").replace('A',
            ref);
        if (Logger.debugging)
          Logger.info("Flattened[" + i + "]=" + paths[i]);
      }
      SB sb = new SB();
      String s;
      for (int i = 0; i < nMax; i++) {
        for (int k = 0; k < nPaths; k++) {
          s = paths[k];
          sb.append(i < s.length() ? s.substring(i, i + 1) : "~");
        }
      }
      return sb.toString();
    }

    /**
     * Remove all unnecessary characters prior to R/S comparison. Note that at
     * this time all C/T and M/P have been changed to R/S already.
     * 
     * @param aStr
     * @return clean RS-only string
     */
    private String cleanRule4Str(String aStr) {
      return (aStr.length() > 1 ? PT.replaceAllCharacters(aStr, "rsmpct~", "")
          : aStr);
    }

    /**
     * Rule 4b comparison of two strings such as RSSR and SRSS, looking for
     * diasteriomeric differences only. A return of IGNORE indicates that they
     * are either the same or opposites and tells sortSubstituents to not
     * explore more deeply.
     * 
     * @param aStr
     * @param bStr
     * @param isRSTest
     *        This is just a test for optional pathways, for example: RS|SR
     * 
     * 
     * 
     * @return 0 (TIED), -1 (A_WINS), 1 (B_WINS), Integer.MIN_VALUE (IGNORE)
     */
    private int compareRule4PairStr(String aStr, String bStr, boolean isRSTest) {
      int n = aStr.length();
      if (n == 0 || n != bStr.length())
        return TIED;
      if (Logger.debugging)
        Logger.info(dots() + this.myPath + " Rule 4b comparing " + aStr + " "
            + bStr);
      char aref = aStr.charAt(0), bref = bStr.charAt(0);
      for (int c = 1; c < n; c++) {
        boolean alike = (aref == aStr.charAt(c));
        if (alike != (bref == bStr.charAt(c)))
          return (isRSTest ? c : 1) * (alike ? A_WINS : B_WINS); // COUNT_LINE
      }
      return (isRSTest ? TIED : IGNORE);
    }

    /**
     * By far the most complex of the methods, this method creates a list of
     * downstream (higher-sphere) auxiliary chirality designators, starting with
     * those furthest from the root and moving in, toward the root.
     * 
     * @param node1
     *        first node; sphere 1
     * @param ret
     *        CIPAtom of next stereochemical branching point
     * 
     * @return collective string, with setting of rule4List
     */
    String createRule4AuxiliaryData(CIPAtom node1, CIPAtom[] ret) {
      String retThread = "";
      char c = '~';
      if (atom == null)
        return "" + c;
      rule4List = new String[4]; // full list based on atoms[]
      if (nPriorities == 0 && !isSet) {
        setNode();
        if (!isAlkene && !isDuplicate && !isTerminal)
          sortToRule(RULE_3);
      }
      int rs = -1, nRS = 0;
      CIPAtom[] ret1 = new CIPAtom[1];
      int ruleMax = RULE_5;
      boolean prevIsChiral = true;
      for (int i = 0; i < 4; i++) {
        CIPAtom a = atoms[i];
        if (a != null && !a.isDuplicate && !a.isTerminal) {
          a.priority = priorities[i];
          ret1[0] = null;
          String rsPath = a.createRule4AuxiliaryData(node1 == null ? a : node1,
              ret1);
          if (ret1[0] != null) {
            a.nextChiralBranch = ret1[0];
            if (ret != null)
              ret[0] = ret1[0];
          }
          rule4List[i] = rsPath;
          if (a.nextChiralBranch != null || rsPath.indexOf("R") >= 0
              || rsPath.indexOf("S") >= 0 || rsPath.indexOf("r") >= 0
              || rsPath.indexOf("s") >= 0) {
            nRS++;
            retThread = rsPath;
            prevIsChiral = true;
          } else {
            if (!prevIsChiral && priorities[i] == priorities[i - 1]) {
              if (node1 == null)
                for (; i >= 0; --i)
                  rule4List[i] = null;
              // two groups have the same priority, and neither has a stereocenter
              return "~";
            }
            prevIsChiral = false;
          }
        }
      }
      boolean isBranch = (nRS >= 2);
      switch (nRS) {
      case 0:
        retThread = "";
        //$FALL-THROUGH$
      case 1:
        ruleMax = RULE_3;
        break;
      case 2:
      case 3:
      case 4:
        c = '~';
        retThread = "";
        if (ret != null)
          ret[0] = this;
        break;
      }
      if (isAlkene) {
        if (!isBranch && alkeneChild != null) {
          // must be alkeneParent -- first C of an alkene -- this is where C/T is recorded 
          boolean isSeqCT = (ret != null && ret[0] == alkeneChild);
          // All odd cumulenes need to be checked.
          // If it is an alkene or even cumulene, we must do an auxiliary check 
          // only if it is not already a defined stereochemistry, because in that
          // case we have a simple E/Z (c/t), and there is no need to check AND
          // it does not contribute to the Mata sequence (similar to r/s or m/p).
          //
          if (!isEvenEne || (auxEZ == STEREO_BOTH_EZ || auxEZ == UNDETERMINED)
              && alkeneChild.bondCount >= 2 && !isKekuleAmbiguous) {
            rs = getAuxEneWinnerChirality(this, alkeneChild, !isEvenEne, RULE_5);
            //
            // Note that we can have C/T (rule4Type = R/S):
            // 
            //    R      x
            //     \    /
            //       ==
            //     /    \
            //    S      root
            //
            // flips sense upon planar inversion; determination was Rule 5.
            //
            // Normalize M/P and E/Z to R/S
            switch (rs) {
            case STEREO_M:
            case STEREO_Z:
              rs = STEREO_R;
              c = 'R';
              break;
            case STEREO_P:
            case STEREO_E:
              rs = STEREO_S;
              c = 'S';
              break;
            }
            if (rs != NO_CHIRALITY) {
              auxChirality = c;
              rule4Type = rs;
              retThread = "";
              if (isSeqCT) {
                nextChiralBranch = alkeneChild;
                ret[0] = this;
              }

            }
          }
        }
      } else if (canBePseudo) {
        // if here, adj is TIED (0) or NOT_RELEVANT
        CIPAtom atom1 = (CIPAtom) clone();
        if (atom1.setNode()) {
          atom1.addReturnPath(null, this);
          atom1.rule4List = new String[4];
          for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
              if (atom1.atoms[i] == atoms[j]) {
                atom1.rule4List[i] = rule4List[j];
                break;
              }
            }
          }
          int rule = atom1.sortToRule(ruleMax);
          if (rule == TIED) {
            c = '~';
          } else {
            rs = atom1.checkHandedness();
            c = (rs == STEREO_R ? 'R' : rs == STEREO_S ? 'S' : '~');
            if (rule == RULE_5) {
              c = (c == 'R' ? 'r' : c == 'S' ? 's' : '~');
            } else {
              rule4Type = rs;
            }
          }
        }
        auxChirality = c;
      }
      if (setAuxiliary && auxChirality != '~')
        atom.setCIPChirality(JC.getCIPChiralityCode(auxChirality));

      if (node1 == null)
        rule4Type = nRS;
      retThread = c + retThread;
      if (Logger.debugging && !retThread.equals("~"))
        Logger.info("creating aux " + c + " for " + this + " = " + myPath);
      return retThread;
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
    private int checkRules4ac(CIPAtom b, String test) {
      if (isTerminal || isDuplicate)
        return TIED;
      int isRa = test.indexOf(auxChirality), isRb = test
          .indexOf(b.auxChirality);
      return (isRa > isRb + 1 ? 
          A_WINS : 
            isRb > isRa + 1 ? 
                B_WINS 
                : TIED);
    }

    /**
     * Determine the ordered CIP winding of this atom. For this, we just take
     * the directed normal through the plane containing the top three
     * substituent atoms and dot that with the vector from any one of them to
     * the fourth ligand (or the root atom if trigonal pyramidal). If this is
     * positive, we have R.
     * 
     * @return 1 for "R", 2 for "S"
     */
    int checkHandedness() {
      P3 p1 = atoms[0].atom.getXYZ(), p2 = atoms[1].atom.getXYZ(), p3 = atoms[2].atom
          .getXYZ();
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

    /**
     * initiate a new CIPAtom for each substituent of atom, and as part of this
     * process, check to see if a new ring is being formed.
     * 
     * @param bs
     */
    void addSmallRings(BS bs) {
      if (atom == null || sphere > SMALL_RING_MAX)
        return;
      if (bs != null)
        bs.clear(atom.getIndex());
      if (isTerminal || isDuplicate || atom.getCovalentBondCount() > 4)
        return;
      SimpleNode atom2;
      int pt = 0;
      SimpleEdge[] bonds = atom.getEdges();
      for (int i = bonds.length; --i >= 0;) {
        SimpleEdge bond = bonds[i];
        if (!bond.isCovalent()
            || (atom2 = bond.getOtherNode(atom)).getCovalentBondCount() == 1
            || parent != null && atom2 == parent.atom)
          continue;
        CIPAtom r = addAtom(pt++, atom2, false, false, false);
        if (r.isDuplicate)
          r.updateRingList();
      }
      for (int i = 0; i < pt; i++)
        if (atoms[i] != null)
          atoms[i].addSmallRings(bs);
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
      a.alkeneParent = null;
      a.rule4Count = null;
      a.rule4List = null;
      a.rootRule4Paths = null;
      a.priority = 0;
      for (int i = 0; i < 4; i++)
        if (atoms[i] != null)
          a.atoms[i] = atoms[i];
      return a;
    }

    @Override
    public String toString() {
      return (atom == null ? "<null>" : "[" + currentRule + "." + sphere + "."
          + priority + "," + id + "." + atom.getAtomName()
          + (isDuplicate ? "*(" + rootDistance + ")" : "")
          + (auxChirality == '~' ? "" : "" + auxChirality) + "]");
    }
  }

}
