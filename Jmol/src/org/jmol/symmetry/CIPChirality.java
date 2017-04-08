package org.jmol.symmetry;

import java.util.Arrays;

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
 * R/S chirality. Based on private knowledge of rules.
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
  //          compareIgnoreDummy(a.sub_i, b.sub_i) 
  //          if (a winner) return A_WINS or B_WINS 
  //       a.sortSubstituents(firstAtom = false)
  //       b.sortSubstituents(firstAtom = false) 
  //       for each substituent...
  //          breakTie(a.sub_i, b.sub_i) or return TIED 
  //       return TIED
  // 
  // [5] return checkWinding()
  //        return 

  
  // Validation:
  
  //  function checkRS(m, key) {
  //    if (m)  load @m
  //    background label yellow
  //    color labels black
  ////    label %[atomname]
  //    set labelfor {_C && chirality != ""} "%[atomname] %[chirality]"
  //    var rs = {*}.chirality.join("")
  //    if (_argCount == 2) {
  //      if (key == rs) {
  //        print m + "\t" + rs + "\tOK"
  //      } else {
  //        var s = m + "\t" + rs + "\t" + key + "?????"
  //        prompt s.replace("\t"," ") 
  //        print s
  //      }
  //    } else {
  //        print m + "\t" + rs
  //    }
  //    refresh
  //  }
  //
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
  static final int A_WINS = 1;
  static final int B_WINS = -1;
  
  Viewer vwr;

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

    // If one of them is a dummy atom (from an alkene, alkyne, or ring) and the other isn't, 
    // then the dummy is the loser.
    if (a.isDummy != b.isDummy)
      return (a.isDummy ? -1 : 1);

    // If both are dummies, it's a tie.
    if (a.isDummy && b.isDummy)
      return TIED;

    // return NO_CHIRALITY/TIED if:
    //  a) one or the other can't be set (because it has too many connections)
    //  b) or one or the other is terminal (has no substituents) 
    //  c) or they are the same atom
    
    if (!a.set() || !b.set() 
        || a.isTerminal || b.isTerminal 
        || a.atom == b.atom)
      return NO_CHIRALITY;
    if (Logger.debugging)
      Logger.info("tie for " + a + " and " + b);

    // Phase I -- shallow check using compareIgnoreDummy
    //
    // Check to see if any of the three connections to a and b are different.
    // Note that we do not consider dummy atom different from a regular atom here
    // because we are just looking for atom differences, not substituent differences.
    //
    // Say we have {O (O) C} and {O O H}
    //
    // The rules require that we first only look at just the atoms, so OOC beats OOH.

    int score = compareAB(a, b, true);
    if (score != TIED)
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

  private static int compareAB(CIPAtom a, CIPAtom b, boolean ignoreDummy) {
    for (int i = 0; i < a.nAtoms; i++) {
      CIPAtom ai = a.atoms[i];
      CIPAtom bi = b.atoms[i];
      int score = (ignoreDummy ? compareIgnoreDummy(ai, bi) : breakTie(ai, bi));
      if (score != TIED)
        return score;
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
     * Dummy atoms have massNo and elemNo but no substituents. They are slightly
     * lower in priority than standard atoms.
     * 
     */
    boolean isDummy = true;

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
     * For the main four atoms, isAbove will increment each time they "win" in a
     * priority contest, thus leading to our desired ordering.
     * 
     */
    int isAbove;

    /**
     * It is important to keep track of the path to this atom in order to
     * prevent infinite cycling. This is taken care of by bsPath. The last atom
     * in the path when cyclic is a dummy atom.
     * 
     */
    BS bsPath;

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
          + (isDummy ? " *" : "" + " " + (isAbove + 1));
    }

    /**
     * 
     * @param atom
     *        or null to indicate a null placeholder
     * @param parent
     * @param isDummy
     */
    CIPAtom(Node atom, CIPAtom parent, boolean isDummy) {
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
        isDummy = true;
      } else {
        bsPath.set(iatom);
      }
      this.isDummy = isDummy;
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
        if (parent != null && parent.atom == other)
          continue;
        int order = bond.getCovalentOrder();
        switch (order) {
        case 3:
          if (!addAtom(pt++, other, false)) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 2:
          if (!addAtom(pt++, other, order != 2)) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 1:
          if (!addAtom(pt++, other, order != 1)) {
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
     * @param isDummy
     * @return true if successful
     */
    private boolean addAtom(int i, Node other, boolean isDummy) {
      if (i >= atoms.length)
        return false;
      atoms[i] = new CIPAtom(other, this, isDummy);
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
        atoms[i].isAbove = 0;
      for (int i = 0; i < n; i++) {
        CIPAtom a = atoms[i];
        for (int j = i + 1; j < n; j++) {
          CIPAtom b = atoms[j];
          int score = a.compareTo(b);
          if (Logger.debugging)
            Logger.info("comparing " + n + " " + i + " " + j + " " + a + " and " + b + " = " + score);
          switch (score) {
          case A_WINS:
            a.isAbove++;
            break;
          case B_WINS:
            b.isAbove++;
            break;
          case TIED:
            switch (breakTie(a, b)) {
            case TIED:
              if (!allowTie)
                return false;
              //$FALL-THROUGH$
            case A_WINS:
              a.isAbove++;
              break;
            case B_WINS:
              b.isAbove++;
              break;
            }
            break;
          }
        }
      }
      CIPAtom[] ret = new CIPAtom[n];
      for (int i = 0; i < n; i++)
        ret[atoms[i].isAbove] = atoms[i];
      if (Logger.debugging)
        for (int i = 0; i < n; i++)
          Logger.info("" + ret[i]);
      atoms = ret;
      return true;
    }

    /**
     * Used in Array.sort and sortSubstituents; includes check for dummy.
     */
    @Override
    public int compareTo(CIPAtom a) {
      return a.atom == atom ? 0 : a.atom == null ? -1 : atom == null ? 1
          : a.elemNo != elemNo ? (a.elemNo < elemNo ? -1 : 1)
              : a.massNo != massNo ? (a.massNo < massNo ? -1 : 1)
                  : a.isDummy != isDummy ? (a.isDummy ? -1 : 1) : 0;
    }

  }

  /**
   * Used only in breakTie; do not check dummy, because this is only a shallow
   * sort.
   * 
   * @param a
   * @param b
   * @return 1 if b is higher; -1 if a is higher; otherwise 0
   */
  static int compareIgnoreDummy(CIPAtom a, CIPAtom b) {
    return b.atom == a.atom ? 0 : b.atom == null ? -1 : a.atom == null ? 1
        : b.elemNo != a.elemNo ? (b.elemNo < a.elemNo ? -1 : 1)
            : b.massNo != a.massNo ? (b.massNo < a.massNo ? -1 : 1) : 0;
  }

}
