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
 * Validated for Rules 1 and 2 in Jmol 14.13.2
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class CIPChirality {

  // Very rough Pseudocode:
  // 
  // getChirality(atom) {
  //  if (atom.getCovalentBondCount() != 4) exit NO_CHIRALITY 
  //  for (each Rule){  
  //    sortSubstituents() 
  //    if (done) exit getHandedness();
  //  }
  //  exit NO_CHIRALITY
  // }
  // 
  // sortSubstituents() {
  //   for (all pairs of substituents a and b) {
  //     score = a.compareTo(b, currentRule)
  //     if (score == TIED) 
  //       score = breakTie(a,b)
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
  //    for (each substituent pairing i in a and b) {
  //      score = breakTie(a_i, b_i)
  //      if (score != TIED) return score
  //    }
  //    return TIED
  // }
  //
  // 
  
  
  //  function checkRS(m, key) {
  //    if (m.find("//")) {
  //      key = m.split("//")[2]
  //      m = m.split("//")[1]
  //    }
  //    print "loading " + @m
  //    refresh
  //    set useMinimizationThread false
  //    if (m)  load @m filter "2D"
  //    if (!{_H})  calculate hydrogens
  //    background label yellow
  //    color labels black
  ////    label %[atomname]
  //    refresh
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
  //        prompt s.replace("\t"," ") 
  //      }
  //    } else {
  //        print m + "\t" + rs
  //    }
  //    refresh
  //  }
  //
  //  //set debug
  //
  //  x = load("cip/R.txt").lines
  //  for (f in x) {
  //    if (f.find("#") == 1) continue
  //    checkRS("cip/R/" + f, "R")
  //  }
  //
  //  x = load("cip/S.txt").lines
  //  for (f in x) {
  //    if (f.find("#") == 1) continue
  //    checkRS("cip/S/" + f, "S")
  //  }
  //
  //  x = load("cip/rs.txt").lines
  //  for (f in x) {
  //    if (f.find("#") == 1) continue
  //    checkRS("cip/RS/" + f, "?")
  //  }
  //
  //
  //  x = load("cip/more.txt").lines
  //  for (f in x) {
  //    if (f.find("#") == 1) continue
  //    checkRS("cip/" + f, "?")
  //  }
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

  static int ptID;


  Viewer vwr;
  
  Map<Integer, Integer> htPathPoints = new Hashtable<Integer, Integer>();

  CIPAtom a;
  
  /**
   * The current rule being applied exhaustively.
   * 
   */
  int currentRule = 0;

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
    boolean isChiral = false;
    if (atom.getCovalentBondCount() == 4
        && (a = new CIPAtom(atom, null, false)).set()) {
      try {
        for (currentRule = 1; currentRule <= 5 && !isChiral; currentRule++) {
          isChiral = false;
          if (Logger.debugging)
            Logger.info("-Rule " + currentRule + " CIPChirality for " + atom + "-----");
          a.sortSubstituents();
          isChiral = true;
          System.out.println(">>>>");
          for (int i = 0; i < 4; i++) {
            System.out.println(a.atoms[i] + " " + a.atoms[i].fullPriority);
          }
          for (int i = 0; i < 3; i++) {
            if (a.atoms[i].fullPriority == a.atoms[i + 1].fullPriority) {
              isChiral = false;
              break;
            }
          }
        }
      if (isChiral)
        rs = getHandedness(a);
      } catch (Throwable e) {
        System.out.println(e + " in CIPChirality");
        if (!vwr.isJS)
          e.printStackTrace();
      }
      if (Logger.debugging)
        Logger.info(atom + " " + (rs == 1 ? "R" : rs == 2 ? "S" : ""));
    }
    if (Logger.debugging)
      Logger.debug("----------------------------------");
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

    // Phase I -- shallow check only
    //
    // Check to see if any of the three connections to a and b are different.
    // Note that we do not consider duplicated atoms different from regular atoms here
    // because we are just looking for atom differences, not substituent differences.
    //
    // Say we have {O (O) C} and {O O H}
    //
    // The rules require that we first only look at just the atoms, so OOC beats OOH.

    if ((score = compareAB(a, b, false)) != TIED)
      return score;

    // Phase II -- deep check using breakTie
    //
    // OK, so all three are nominally the same.
    // Now seriously deep-sort each list based on substituents
    // and then check them one by one to see if the tie can be broken.

    a.sortSubstituents();
    b.sortSubstituents();
    return compareAB(a, b, true);
  }

  private static int compareAB(CIPAtom a, CIPAtom b, boolean goDeep) {
    for (int i = 0; i < a.nAtoms; i++) {
      CIPAtom ai = a.atoms[i];
      CIPAtom bi = b.atoms[i];
      if (Logger.debugging)
        Logger.debug("compareAB " + ai.parent + "-" + ai + " with " + bi.parent
            + "-" + bi + " goDeep=" + goDeep);
      int score = ai.checkPriorGroupPriority(bi);
      if (score != TIED)
        return score;
      score = (goDeep ? breakTie(ai, bi) : ai.checkCurrentRule(bi));
      if (score != TIED) {
        if (Logger.debugging)
          Logger.debug("compareAB " + (score == B_WINS ? bi + " beats " + ai : ai
              + " beats " + bi)
              + " " + goDeep);
        return score;
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
     * priorityNumber 1(highest) to 4(lowest).
     * 
     */
    int priorityNumber;

    /**
     * index 0 to 3.
     * 
     */
    int priorityIndex;

    /**
     * It is important to keep track of the path to this atom in order to
     * prevent infinite cycling. This is taken care of by bsPath. The last atom
     * in the path when cyclic is a Duplicate atom.
     * 
     */
    BS bsPath;
    
    /**
     * A number that reflects a group's full priority after applying Rules 1 - 5 sequentially.
     * 
     * abcde where a is priority 1-4 for Rule 1, b is priority 0-4 for Rule 2, etc.
     * where "0" indicates not evaluated because it was not necessary. (Not a tie at previous rule).   
     * 
     * 
     */
    int fullPriority;
    
    /**
     * A number that reflects a group's priority prior to any comparisons within a given rule.
     * 
     * abcde where a is priority 1-4 for Rule 1, b is priority 0-4 for Rule 2, etc.
     * where "0" indicates not evaluated because it was not necessary. (Not a tie at previous rule).   
     * 
     * 
     */
    int prevPriority;
    
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

    private boolean hasH1;

    private int id;

    @Override
    public String toString() {
      return (atom == null ? "<null>" : "[" + id + " " + atom.toString())
          + (isDuplicate ? "*" : "" + " i=" + priorityIndex + " p=" + priorityNumber) + "]";
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
      this.id = ++ptID;
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
      if (i >= atoms.length) {
        if (Logger.debugging)
          Logger.debug(" too many bonds on " + atom);
        return false;
      }
      if (parent == null) {
        // For top level, we do not allow two 1H atoms.
        int atomIsotope = other.getAtomicAndIsotopeNumber();
        if (atomIsotope == 1) {
          if (hasH1) {
            if (Logger.debugging)
              Logger.debug(" second H atom found on " + atom);
            return false;
          }
          hasH1 = true;
        }
      }
      atoms[i] = new CIPAtom(other, this, isDuplicate);
      if (Logger.debugging)
        Logger.debug(this + " adding " + i + " " + atoms[i]);
      return true;
    }

    /**
     * Deep-Sort the substituents of an atom. Don't allow ties if this is the
     * first atom (parent == null).
     * 
     */
    void sortSubstituents() {
      int n = atoms.length;
      for (int i = 0; i < n; i++) { 
        atoms[i].priorityNumber = 1;
        atoms[i].priorityIndex = 0;
        atoms[i].prevPriority = 0;
      }
      for (int i = 0; i < n; i++) {
        CIPAtom a = atoms[i];
        for (int j = i + 1; j < n; j++) {
          CIPAtom b = atoms[j];
          int score = a.compareTo(b);
          if (Logger.debugging)
            Logger.info("comparing " + parent + "-" + this + "-" + a + "/" + b
                + " = " + score);
          switch (score) {
          case B_WINS:
            a.priorityIndex++;
            a.priorityNumber++;
            if (Logger.debugging)
              Logger.debug(b + " beats " + a);
            break;
          case A_WINS:
            b.priorityIndex++;
            b.priorityNumber++;
            if (Logger.debugging)
              Logger.debug(a + " beats " + b);
            break;
          case TIED:
            switch (breakTie(a, b)) {
            case TIED:
              a.priorityIndex++;
              if (Logger.debugging)
                Logger.debug(b + " ties " + a);
              break;
            case B_WINS:
              a.priorityIndex++;
              a.priorityNumber++;
              if (Logger.debugging)
                Logger.debug(b + " wins in tie with " + a + "\n");
              break;
            case A_WINS:
              b.priorityIndex++;
              b.priorityNumber++;
              if (Logger.debugging)
                Logger.debug(a + " wins in tie with " + b + "\n");
              break;
            }
            break;
          }
        }
      }
      CIPAtom[] ret = new CIPAtom[n];
      for (int i = 0; i < n; i++) {
        System.out.println("Setting full priority for " + atoms[i]);
        atoms[i].fullPriority = atoms[i].fullPriority * 10 + atoms[i].priorityNumber; 
        ret[atoms[i].priorityIndex] = atoms[i];
      }
      if (Logger.debugging)
        for (int i = 0; i < n; i++)
          Logger.info(atom + "[" + i + "]=" + ret[i]);
      atoms = ret;
    }

    /**
     * This check is not technically one of those listed in the rules, but it 
     * us useful when preparing to check substituents because if one of the 
     * atoms has substituents and the other doesn't, we are done -- 
     * there is no reason to check substituents. 
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    int checkDuplicate(CIPAtom b) {
      return b.isDuplicate == isDuplicate ? TIED : b.isDuplicate ? A_WINS : B_WINS;
    }

    /**
     * Used in Array.sort and sortSubstituents; includes a preliminary check for duplicate, 
     * since we know that that atom will ultimately be lower priority if all other rules are tied.
     * This is just a convenience.
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    @Override
    public int compareTo(CIPAtom b) {
      int score;
      // fullPriority is cumulative over previous Rule applications
      if (b.fullPriority != fullPriority)
        return (b.fullPriority < fullPriority ? B_WINS : A_WINS);
      return  (score = checkCurrentRule(b)) != TIED ? score : checkDuplicate(b); 
    }

    /**
     * Check this atom "A" vs a challenger "B" against the current rule.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    public int checkCurrentRule(CIPAtom b) {
      switch (currentRule) {
      default:
      case 1:
        int score;
        return (score = checkRule1a(b)) != TIED ? score
            : checkRule1b(b); 
      case 2:
        return checkRule2(b);
      case 3:
        return checkRule3(b);
      case 4:
        return checkRule4(b);
      case 5:
        return checkRule5(b);
      }
    }

    /**
     * Check priority prior to this Rule's initiation.
     *  
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    public int checkPriorGroupPriority(CIPAtom b) {
      return (b.prevPriority == prevPriority ? TIED : b.prevPriority < prevPriority ? B_WINS : A_WINS);
    }

    /**
     * Looking for same atom or ghost atom or element number
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    int checkRule1a(CIPAtom b) {
      return b.atom == atom ? TIED : b.atom == null ? A_WINS : atom == null ? B_WINS
          : b.elemNo < elemNo ? A_WINS : b.elemNo > elemNo ? B_WINS : TIED;
    }

    /**
     * Looking for root distance -- duplicated atoms only.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */

    int checkRule1b(CIPAtom b) {
      return !b.isDuplicate || !isDuplicate ? TIED 
          : b.rootDistance < rootDistance ? 
              B_WINS 
          : b.rootDistance > rootDistance ? 
              A_WINS     
          : TIED;
    }

    /**
     * Chapter 9 Rule 2. atomic mass
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    int checkRule2(CIPAtom b) {
      return b.massNo < massNo ? A_WINS : b.massNo > massNo ? B_WINS : TIED;
    }

    private int checkRule3(CIPAtom b) {
      // TODO
      return TIED;
    }

    private int checkRule4(CIPAtom b) {
      // TODO
      return TIED;
    }

    private int checkRule5(CIPAtom b) {
      // TODO
      return TIED;
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

// status: Rule 2 implemented


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
