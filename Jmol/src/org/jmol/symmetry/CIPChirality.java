package org.jmol.symmetry;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.V3;

import org.jmol.java.BS;

import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.viewer.Viewer;

/**
 * A relatively simple implementation of Cahn-Ingold-Prelog rules for assigning
 * R/S chirality. Based on IUPAC rules. (See text at the end of this document.)
 * 
 * 
 * Introduced in Jmol 14.12.0
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class CIPChirality {

  // Pseudocode:
  //
  // [1]   if !(sortSubstituents()) return NO_CHIRALITY 
  //       return getChirality(firstAtom = true)
  // 
  // 
  // [3] sortSubstituents()
  //       for all pairs of substituents i,j...
  //          compare(i,j)
  //          if (TIED and firstAtom) 
  //             breakTie(i,j) or return NO_CHIRALITY
  // 
  // [4] breakTie(a,b) 
  //       for each substituent i of a and b...
  //          compareIgnoreDuplicate(a.sub_i, b.sub_i) 
  //          if (a winner) return A_WINS or B_WINS 
  //       a.sortSubstituents(firstAtom = false)
  //       b.sortSubstituents(firstAtom = false) 
  //       for each substituent...
  //          breakTie(a.sub_i, b.sub_i) or return TIED 
  //       return TIED
  // 
  // [5] return checkWinding()
  //        return 

  
  //  function checkRS(m, key) {
  //    print "loading " + @m
  //    refresh
  //    set useMinimizationThread false
  //    if (m)  load @m filter "2D"
  //    if (!{_H})  calculate hydrogens
  //    background label yellow
  //    color labels black
  ////    label %[atomname]
  //    set labelfor {_C && chirality != ""} "%[atomname] %[chirality]"
  //    var rs = {*}.chirality.join("")
  //    if (_argCount == 2) {
  //      var ref = _M.molData["chiral_atoms"].replace("\n","").replace(" ","");
  //      if (ref) {
  //        key = ref;
  //        rs = {chirality != ""}.label("%i%[chirality]").join("")
  //      }
  //      if (key == rs) {
  //        print "OK\t" + m + "\t" + rs
  //      } else {
  //        var s = "??\t" + m + "\t" + rs + "\t" + key
  //        refresh 
  //        print s
  ////        prompt s.replace("\t"," ") 
  //      }
  //    } else {
  //        print m + "\t" + rs
  //    }
  //    refresh
  //  }
  
  //   /**
  //
  //  checkrs("$(R)-3-hydroxy-1,4-heptadiyne", "R")
  //  checkRS("$(R)-glycidol", "R")
  //  checkRS("$glucose", "RSRR")
  //  checkRS("$(2S,3R)-2,3-oxiranediol", "SR")
  //  checkRS("$(S)-2-butanol", "S")
  //  checkRS("$(R)-2-butanol", "R")
  //  checkRS("$(2S,3R)-2,3-butanediol", "SR")
  //  checkRS("$(2S,3S)-2,3-butanediol", "SS")
  //  checkRS("$(2R,3R)-2,3-butanediol", "RR")
  //  checkRS("$(2R,3S)-2,3-butanediol", "RS")
  //  checkRS("$1,4-dimethylcyclohexane", "")
  //  checkRS("$cholesterol", "RRSSSRSR") // (3S,8S,9S,10R,13R,14S,17R) and sidechain (R)
  //  checkRS("==ta1", "SSRSRSSRSRS") // taxol (1S,2S,3R,4S,7R,9S,10S,12R,15S) and sidechain (2R,3S)

  
  static final int NO_CHIRALITY = 0;
  static final int TIED = NO_CHIRALITY;
  static final int B_WINS = 1;
  static final int A_WINS = -1;
  
  Viewer vwr;
  
  Map<Integer, Integer> htPathPoints = new Hashtable<Integer, Integer>();

  public CIPChirality() {
    // for reflection 
  }

  public CIPChirality setViewer(Viewer vwr) {
    this.vwr = vwr;
    return this;
  }

  /**
   * Determine the Cahn-Ingold-Prelog R/S chirality of an atom
   * 
   * @param atom
   * @return [0:none, 1:R, 2:S]
   */
  public int getChirality(Node atom) {
    int rs = NO_CHIRALITY;
    CIPAtom a;
    if (atom.getCovalentBondCount() != 4
        || !(a = new CIPAtom(atom, null, false)).set())
      return rs;

    try {
      if (a.sortSubstituents())
        rs = getHandedness(a);
    } catch (Throwable e) {
      System.out.println(e + " in CIPChirality");
      if (!vwr.isJS)
        e.printStackTrace();
    }
    if (Logger.debugging)
      Logger.info(atom + " " + (rs == 1 ? "R" : rs == 2 ? "S" : ""));
    return rs;
  }

  /**
   * determine the winding of the circuit p1--p2--p3 relative to point p4
   * 
   * @param a 
   * @return 1 for "R", 2 for "S"
   */
  static int getHandedness(CIPAtom a) {
    P3 p1 = (P3) a.atoms[0].atom; // highest priority
    P3 p2 = (P3) a.atoms[1].atom;
    P3 p3 = (P3) a.atoms[2].atom;
    P3 p4 = (P3) a.atoms[3].atom; // lowest priority
    V3 vNorm = new V3();
    float d = Measure.getNormalThroughPoints(p1, p2, p3, vNorm, new V3());
    return (Measure.distanceToPlaneV(vNorm, d, p4) > 0 ? 1 : 2);
  }

  /**
   * Break a tie at any level in the iteration between to atoms that otherwise
   * are the same by sorting their substituents.
   * 
   * @param a
   * @param b
   * @return 1 to indicate a is the winner, -1 to indicate b is the winner; 0
   *         for a tie
   */
  static int breakTie(CIPAtom a, CIPAtom b) {

    // Do a duplicate check -- if that is not a TIE we do not have to go any further.
    
    int score = a.checkDuplicate(b);
    if (score != TIED)
      return score;
    
    score = a.checkRule1b(b);
    if (score != TIED)
      return score;
    
    // return NO_CHIRALITY/TIED if:
    //  a) one or the other can't be set (because it has too many connections)
    //  b) or one or the other is terminal (has no substituents) 
    //  c) or they are the same atom
    
    if (a.atom == b.atom || !a.set() || !b.set() 
        || a.isTerminal || b.isTerminal
        || a.isDuplicate && b.isDuplicate)
      return NO_CHIRALITY;
    if (Logger.debugging)
      Logger.info("tie for " + a + " and " + b);

    // Phase I -- shallow check using compareIgnoreDuplicate
    //
    // Check to see if any of the three connections to a and b are different.
    // Note that we do not consider Duplicate atom different from a regular atom here
    // because we are just looking for atom differences, not substituent differences.
    //
    // Say we have {O (O) C} and {O O H}
    //
    // The rules require that we first only look at just the atoms, so OOC beats OOH.

    if ((score = compareAB(a, b, true)) != TIED)
      return score;

    // Phase I -- deep check using breakTie
    //
    // OK, so all three are nominally the same.
    // Now seriously deep-sort each list based on substituents
    // and then check them one by one to see if the tie can be broken.

    a.sortSubstituents();
    b.sortSubstituents();
    return compareAB(a, b, false);
  }

  private static int compareAB(CIPAtom a, CIPAtom b, boolean ignoreDuplicate) {
    for (int i = 0; i < a.nAtoms; i++) {
      CIPAtom ai = a.atoms[i];
      CIPAtom bi = b.atoms[i];
      System.out.println("compareAB " + ai + " " + bi + " ignoreDup=" + ignoreDuplicate);
      int score = (ignoreDuplicate ? ai.checkRules12(bi) : breakTie(ai, bi));
      if (score != TIED) {
        System.out.println((score == B_WINS ? bi + " beatsi " + ai :  ai + " beatsi " + bi ) + " " + ignoreDuplicate);
        return  score;
      }
    }
    return TIED;
  }

  private class CIPAtom implements Comparable<CIPAtom> {

    /**
     * Use of Node allows us to implement this in SMILES or Jmol.
     * 
     */
    Node atom;

    /**
     * One of the two key characteristics for assigning R and S
     */
    int massNo;

    /**
     * One of the two key characteristics for assigning R and S
     */
    int elemNo;

    CIPAtom parent;

    /**
     * Duplicate atoms have massNo and elemNo but no substituents. They are slightly
     * lower in priority than standard atoms.
     * 
     */
    boolean isDuplicate = true;
    
    /**
     * Terminal (single-valence) atoms need not be followed further.
     * 
     */
    boolean isTerminal;

    /**
     * We only set an atom once.
     * 
     */
    boolean isSet;

    /**
     * For the main four atoms, isBelow will increment each time they "lose" in a
     * priority contest, thus leading to our desired ordering.
     * 
     */
    int isBelow;

    /**
     * It is important to keep track of the path to this atom in order to
     * prevent infinite cycling. This is taken care of by bsPath. The last atom
     * in the path when cyclic is a Duplicate atom.
     * 
     */
    BS bsPath;
    
    /**
     * Distance from root for a duplicated atom.
     * 
     */
    
    int rootDistance;

    /**
     * The substituents -- 4 for the base carbon; 3 or fewer for other atoms.
     * 
     */
    CIPAtom[] atoms;

    /**
     * Number of substituent atoms.
     */
    int nAtoms;

    @Override
    public String toString() {
      return (atom == null ? "<null>" : atom.toString())
          + (isDuplicate ? " *" : "" + " " + (isBelow + 1));
    }

    /**
     * 
     * @param atom
     *        or null to indicate a null placeholder
     * @param parent
     * @param isDuplicate
     */
    CIPAtom(Node atom, CIPAtom parent, boolean isDuplicate) {
      if (atom == null)
        return;
      this.atom = atom;
      this.parent = parent;
      this.isTerminal = atom.getCovalentBondCount() == 1;
      this.elemNo = atom.getElementNumber();
      this.massNo = atom.getNominalMass();
      this.bsPath = (parent == null ? new BS() : BSUtil.copy(parent.bsPath));

      int iatom = atom.getIndex();
      if (bsPath.get(iatom)) {
        isDuplicate = true;
        this.rootDistance = htPathPoints.get(new Integer(iatom)).intValue();
      } else {
        bsPath.set(iatom);
        this.rootDistance = (parent == null ? 1 : parent.rootDistance + 1);
        htPathPoints.put(new Integer(iatom), new Integer(this.rootDistance));
      }
      this.isDuplicate = isDuplicate;
    }

    /**
     * Set the atom to have substituents.
     * 
     * @return true if a valid atom for consideration
     * 
     */
    boolean set() {
      if (isTerminal || isSet)
        return true;
      isSet = true;
      atoms = new CIPAtom[parent == null ? 4 : 3];
      if (atom == null)
        System.out.println("HOHO");
      int nBonds = atom.getBondCount();
      Edge[] bonds = atom.getEdges();
      int pt = 0;
      for (int i = 0; i < nBonds; i++) {
        Edge bond = bonds[i];
        if (!bond.isCovalent())
          continue;
        Node other = bond.getOtherAtomNode(atom);
        boolean isParent = (parent != null && parent.atom == other);
        int order = bond.getCovalentOrder();
        switch (order) {
        case 3:
          if (!addAtom(pt++, other, isParent)) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 2:
          if (!addAtom(pt++, other, order != 2 || isParent)) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 1:
          if (!isParent && !addAtom(pt++, other, order != 1)) {
            isTerminal = true;
            return false;
          }
          break;
        default:
          isTerminal = true;
          return false;
        }
      }
      isTerminal = (pt == 0);
      nAtoms = pt;
      for (; pt < atoms.length; pt++)
        atoms[pt] = new CIPAtom(null, null, true);

      // Do an initial atom-only shallow sort using a.compareTo(b)

      Arrays.sort(atoms);
      return !isTerminal;
    }

    /**
     * Add a new atom or return false
     * 
     * @param i
     * @param other
     * @param isDuplicate
     * @return true if successful
     */
    private boolean addAtom(int i, Node other, boolean isDuplicate) {
      if (i >= atoms.length)
        return false;
      atoms[i] = new CIPAtom(other, this, isDuplicate);
      System.out.println(this + " adding " + i + " "+ atoms[i]);
      return true;
    }

    /**
     * Deep-Sort the substituents of an atom. 
     * Don't allow ties if this is the first atom (parent == null).
     * 
     * @return true if this is not the first atom or if all four substituents are unique
     */
    boolean sortSubstituents() {
      boolean allowTie = (parent != null);
      int n = atoms.length;
      for (int i = 0; i < n; i++)
        atoms[i].isBelow = 0;
      for (int i = 0; i < n; i++) {
        CIPAtom a = atoms[i];
        for (int j = i + 1; j < n; j++) {
          CIPAtom b = atoms[j];
          int score = a.compareTo(b);
          if (Logger.debugging)
            Logger.info("comparing parent=" + parent + " this=" + this + ": " + n + " " + i + " " + j + " " + a + " and " + b + " = " + score);
          switch (score) {
          case B_WINS:
            a.isBelow++;
            System.out.println(b + " beatsc " + a);
            break;
          case A_WINS:
            b.isBelow++;
            System.out.println(a + " beatsc " + b);
            break;
          case TIED:
            switch (breakTie(a, b)) {
            case TIED:
              if (!allowTie)
                return false;
              //$FALL-THROUGH$
            case B_WINS:
              a.isBelow++;
              System.out.println(b + " beatst " + a);
              break;
            case A_WINS:
              b.isBelow++;
              System.out.println(a + " beatst " + b);
              break;
            }
            break;
          }
        }
      }
      CIPAtom[] ret = new CIPAtom[n];
      for (int i = 0; i < n; i++)
        ret[atoms[i].isBelow] = atoms[i];
      if (Logger.debugging)
        for (int i = 0; i < n; i++)
          Logger.info(atom + "["+i+"]=" + ret[i]);
      atoms = ret;
      return true;
    }

    /**
     * Used in Array.sort and sortSubstituents; includes check for Duplicate.
     */
    @Override
    public int compareTo(CIPAtom b) {
      int score = TIED;
      return  (score = checkRules12(b)) != TIED ? score : checkDuplicate(b); 
    }

    public int checkRules12(CIPAtom b) {
      int score = TIED;
      return  (score = checkRule1a(b)) != TIED ? score
          : (score = checkRule1b(b)) != TIED ? score 
              : checkRule2(b); 
    }

    /**
     * Looking for same atom or phantom atom or element number
     * 
     * @param b
     * @return 1 if b is higher; -1 if a is higher; otherwise 0
     */
    int checkRule1a(CIPAtom b) {
      return b.atom == atom ? TIED : b.atom == null ? A_WINS : atom == null ? B_WINS
          : b.elemNo != elemNo ? (b.elemNo < elemNo ? A_WINS : B_WINS) : TIED;
    }


    int checkRule1b(CIPAtom b) {
      return !b.isDuplicate || !isDuplicate ? TIED 
          : b.rootDistance < rootDistance ? B_WINS 
          : b.rootDistance > rootDistance ? A_WINS     
          : TIED;
    }

    /**
     * Chapter 9 Rule 2.
     * 
     * @param b
     * @return 1 if b is higher; -1 if a is higher; otherwise 0
     */
    int checkRule2(CIPAtom b) {
      return b.massNo < massNo ? A_WINS :  b.massNo > massNo ? B_WINS : TIED;
    }

    int checkDuplicate(CIPAtom b) {
      return b.isDuplicate == isDuplicate ? TIED : b.isDuplicate ? A_WINS : B_WINS;
    }

  }


}

//https://www.iupac.org/fileadmin/user_upload/publications/recommendations/CompleteDraft.pdf
//
//P-91.1.1.2 The ‘Sequence Rules’
//The following ‘Sequence Rules’ are used (ref. 30, 31, 32) to establish the order of precedence
//of atoms and groups. A more encompassing set of rules, proposed by Mata, Lobo, Marshall, and
//Johnson (ref. 34), including amendments by Custer (ref. 35), Hirschmann and Hanson (ref. 36), is
//used in this Chapter. The rules are hierarchical, i.e., each rule must be exhaustively applied in the
//order given until a decision is reached:
//
//Rule 1 (a) Higher atomic number precedes lower;
//(b) A duplicated atom, with its predecessor node having the same label closer
//to the root, ranks higher than a duplicated atom, with its predecessor node
//having the same label farther from the root, which ranks higher than any
//nonduplicated-atom-node (proposed by Custer, ref. 36)

// status: Rule 1 implemented


//Rule 2 Higher atomic mass number precedes lower;

//status: Rule 1 implemented (but before 1b??)


//Rule 3 seqcis Stereogenic units precede seqtrans stereogenic units and these
//precede nonstereogenic units (seqcis > seqtrans > nonstereogeni).
//(Proposed by Mata, Lobo, Marshall, and Johnson, ref. 34);
//The domain of application of this rule is restricted to
//geometrically diastereomorphic planar tetravalent atoms and
//double bonds. All cases involving geometrically
//diastereomorphic two-dimensional stereogenic units are
//considered in Rules 4 and 5. (Proposed by Hirschmann and
//Hanson, ref. 36).

//status: Rule 3 NOT implemented




//Rule 4 (a) Chiral stereogenic units precede pseudoasymmetric stereogenic units and
//these precede nonstereogenic units. (Sub-rule originally proposed by
//Prelog and Helmchen (ref. 32), but their inclusion as first sub-rule of
//Rule 4 was proposed by Custer, ref. 35). Geometrically enantiomorphic
//twodimensional stereogenic units precede two-dimensional
//nonstereogenic units (Proposed by Mata, Lobo, Marshall and Johnson,
//ref. 34).
//(b) When two ligands have different descriptor pairs, then the one with the
//first chosen like descriptor pairs has priority over the one with a
//corresponding unlike descriptor pair.
//(i) Like descriptor pairs are: ‘RR’, ‘SS’, ‘MM’, ‘PP’,
//‘seqCis/seqCis’, ‘seqTran/sseqTrans’, ‘RseqCis’,
//‘SseqTrans’, ‘MseqCis’, ‘PseqTrans’.
//(ii) Unlike discriptor pairs are ‘RS’, ‘MP’, ‘RP’, ‘SM’,
//‘seqCis/seqTrans’, ‘RseqTrans’, ‘SseqCis’, ‘PseqCis’ and
//‘MseqTrans’. (the descriptor pairs ‘RRe’, ‘SSi’, ‘ReRe’,
//‘SiSi’,’ReM’, ‘SiP’, ’ReSi’, ‘Rsi’, ’ReP’ and ‘MSi’ are not
//included in this rule (proposed by Mata, Lobo, Marshall and
//Johnson, ref. 34).
//Methodology for pairing descriptors:
//The descriptor assigned to geometrically enantiomorphic double
//bonds should be associated in the digraph with the first node
//corresponding to the atoms involved in the double bond (proposed by
//Mata, Lobo, Marshall and Johnson, ref. 34).
//For each atom or group the descriptor chosen at first (highest ranked
//descriptor) is paired with all the remaining descriptors. The following
//characteristics determine the hierarchical rank of the pairs of descriptors:
//(i) higher rank of the second descriptor in the pair;
//(ii) lower rank of the least common ancestor in the graph
//(proposed by Custer, ref. 35).
//(c) ‘r’ Precedes ‘s’ and ‘p’ precedes ‘m’ (proposed by Mata, Lobo,
//Marshall and Johnson, ref. 34).
//Re-inclusion of this subrule in Rule 4 was proposed by Custer (ref. 35).

//status: Rule 4 NOT implemented


//Rule 5 An atom or group with descriptor ‘R’, ‘M’ and ‘seqCis’ has priority over its
//enantiomorph ‘S’, ‘P’ or ‘seqTrans’, ‘seqCis’ and ‘seqTrans’ (proposed by
//Hirschmann and Hanson, ref. 36)

//status: Rule 5 NOT implemented


//These rules are based on the hierarchical order of atoms or groups properties, material and
//topological properties for rules 1 and 2, geometrical properties for rules 3 and 4, and
//topographical properties for rule 5. The first four properties are reflection-invariant, the fifth is
//reflection-variant.
//Atoms and groups of atoms are monovalent or divalent as exemplified by ‘diyl’ groups; they
//can be acyclic or cyclic.
//The five ‘Sequence Rules’ are applied as follows:
//(a) each rule is applied exhaustively to all atoms or groups being compared;
//(b) each rule is applied in accordance with a hierachical digraph (see P-91.2)
//(c) the atom or group that is found to have precedence (priority) at the first
//occurrence of a difference in a digraph retains this precedence (priority)
//regardless of differences that occur later in the exploration of the digraph;
//(d) precedence (priority) of an atom in a group established by a rule does not
//change on application of a subsequent rule.
