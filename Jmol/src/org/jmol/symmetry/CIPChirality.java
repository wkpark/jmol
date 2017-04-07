package org.jmol.symmetry;

import java.util.Arrays;

import org.jmol.java.BS;

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.viewer.Viewer;

/**
 * A relatively simple implementation of Cohen-Ingold-Prelog rules for assigning R/S chirality
 * 
 *     [1] getChirality(Node)
 *              new CIPAtom(Node)
 *              set()
 *              return getRorS();
 *               
 *     [2] CIPAtom.getRorS()
 *              sortSubstituents()
 *              return f(SmilesMatcher().getChirality())
 *              
 *              
 *     [3] sortSubstituents()
 *             if (necessary)
   *              breakTie(a,b)
 *              
 *     [4] breakTie(a,b)
 *              a.set(), b.set()
 *              for each substituent...
 *                compareAB(a.atoms[i], b.atoms[i])
 *              if (all are tied) ...
 *                sortSubstituents(a.atoms)
 *                sortSubstituents(b.atoms)
 *                for each substituent...
 *                   breakTie(a.atoms[i], b.atoms[i])
 *               
 * 
 * Introduced in Jmol 14.12.0
 * 
 * @author Bob Hanson hansonr@stolaf.edu 
 */
public class CIPChirality {

  // Checked using:
  
  //  function checkchiral(m) {
  //    if (m)  load @m
  //    background label yellow
  //    color labels black
  //    select _C
  //    label %[atomname]
  //    refresh
  //    var b = {_C}
  //    for (var a in b) {
  //      var c = a.chirality;
  //      print _smilesString + " " + a + c
  //      if (c) {
  //         select a
  //         c = a.atomname + " " + c
  //         label @c
  //      }
  //    }
  //    select *
  //  }
  //
  //  checkchiral("$(R)-glycidol")
  //  delay 1
  //  checkchiral("$glucose")
  //  delay 1
  //  checkchiral("$(2S,3R)-2,3-oxiranediol")
  //  delay 1
  //  checkchiral("$(S)-2-butanol")
  //  delay 1
  //  checkchiral("$(R)-2-butanol")
  //  delay 1
  //  checkchiral("$(2S,3R)-2,3-butanediol")
  //  delay 1
  //  checkchiral("$(2S,3S)-2,3-butanediol")
  //  delay 1
  //  checkchiral("$(2R,3R)-2,3-butanediol")
  //  delay 1
  //  checkchiral("$(2R,3S)-2,3-butanediol")
  //  delay 1
  //  checkchiral("$1,4-dimethylcyclohexane") // no chirality
  //  delay 1
  //  checkchiral("$cholesterol") // (3S,8S,9S,10R,13R,14S,17R) and sidechain R
  
  Viewer vwr;

  public CIPChirality() {
    // for reflection 
  }

  public CIPChirality setViewer(Viewer vwr) {
    this.vwr = vwr;
    return this;
  }

  public String getChirality(Node atom) {
    if (atom.getCovalentBondCount() != 4)
      return "";
    CIPAtom a = new CIPAtom(atom, null, false);
    if (!a.set())
      return "";
    String rs = getRorS(a);
    if (Logger.debugging)
      Logger.info(atom + " " + rs);
    return rs;
  }

  private String getRorS(CIPAtom a) {
    try {
      CIPAtom[] atoms = sortSubstituents(a.atoms, false);
      if (atoms == null)
        return "";
      SmilesMatcherInterface sm = vwr.getSmilesMatcher();
      switch (sm.getChirality(atoms[3].atom, atoms[2].atom,
          atoms[1].atom, atoms[0].atom)) {
      case 1:
        return "R";
      case 2:
        return "S";
      default:
        return "";
      }
    } catch (Throwable e) {
      e.printStackTrace();
      return "";
    }
  }

  /**
   * Sort the substituents of an atom in preparation for comparing the set with other atoms
   * 
   * @param atoms may be 3 or 4 atoms here
   * @param allowTie 
   * 
   * @return true if we have four distinct atoms in the end
   */
  private static CIPAtom[] sortSubstituents(CIPAtom[] atoms, boolean allowTie) {
    int n = atoms.length;
    for (int i = 0; i < n; i++)
      atoms[i].isAbove = 0;
    for (int i = 0; i < n; i++) {
      CIPAtom a = atoms[i];
      for (int j = i + 1; j < n; j++) {
        CIPAtom b = atoms[j];
        int score = (int) Math.signum(a.compareTo(b));
        if (Logger.debugging)
          Logger.info("comparing " + a + " and " + b + " = " + score);
        switch (score) {
        case 1:
          a.isAbove++;
          break;
        case -1:
          b.isAbove++;
          break;
        case 0:
          switch (breakTie(a, b)) {
          case 1:
            a.isAbove++;
            break;
          case -1:
            b.isAbove++;
            break;
          case 0:
            if (allowTie)
              a.isAbove++;
            else
              return null;
          }
        }
      }
    }
    CIPAtom[] ret = new CIPAtom[n];
    for (int i = 0; i < n; i++)
      ret[atoms[i].isAbove] = atoms[i];
    if (Logger.debugging)
      for (int i = 0; i < n; i++)
        Logger.info("" + ret[i]);
    return ret;
  }

  /**
   * Break a tie at any level in the iteration between to atoms that
   * otherwise are the same by sorting heir
   * 
   * @param a
   * @param b
   * @return -1
   */
  private static int breakTie(CIPAtom a, CIPAtom b) {
    if (!a.set() || !b.set() || a.isTerminal || a.atom == b.atom)
      return 0;
    if (Logger.debugging)
      Logger.info("tie for " + a + " and " + b);
    // check to see if any of the three connections to a and b are different.
    for (int i = 0; i < a.nAtoms; i++) {
      CIPAtom ai = a.atoms[i];
      CIPAtom bi = b.atoms[i];
      int score = (int) Math.signum(compareAB(ai, bi));
      switch (score) {
      case -1:
      case 1:
        return score;
      case 0:
        break;
      }
    }
    // all are the same -- check to break tie next level
    // now isDummy counts
    
    a.atoms = sortSubstituents(a.atoms, true);
    b.atoms = sortSubstituents(b.atoms, true);
    
    for (int i = 0; i < a.nAtoms; i++) {
      CIPAtom ai = a.atoms[i];
      CIPAtom bi = b.atoms[i];
      int score = (ai.isDummy == bi.isDummy ? breakTie(ai, bi) : ai.isDummy ? -1 : 1);
      switch (score) {
      case -1:
      case 1:
        return score;
      case 0:
      }
    }
    // all are the same and no tie breakers
    return 0;
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
     * Dummy atoms have massNo and elemNo but no substituents.
     * They are slightly lower in priority than standard atoms.
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
     * It is important to keep track of the path to this atom in order
     * to prevent infinite cycling. This is taken care of by bsPath.
     * The last atom in the path when cyclic is a dummy atom. 
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
      return atom.toString() + (isDummy ? " *" : "" + " " + (isAbove + 1));
    }

    /**
     * 
     * @param atom or null to indicate a null placeholder
     * @param parent
     * @param isDummy
     */
    public CIPAtom(Node atom, CIPAtom parent, boolean isDummy) {
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
      if (isTerminal)
        return true;
      if (isSet)
        return true;
      isSet = true;
      if (isDummy)
        return false;
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
          if (!addAtom(pt++, other, true)) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 2:
          if (!addAtom(pt++, other, true)) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 1:
          if (!addAtom(pt++, other, false)) {
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
     * used in Array.sort and sortFourAtoms; includes isDummy check
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
   * Used only in breakTie; do not check dummy
   * 
   * @param a
   * @param b
   * @return 1 if b is higher; -1 if a is higher; otherwise 0
   */
  static public int compareAB(CIPAtom a, CIPAtom b) {
    return b.atom == a.atom ? 0 : b.atom == null ? -1 : a.atom == null ? 1
        : b.elemNo != a.elemNo ? (b.elemNo < a.elemNo ? -1 : 1)
        : b.massNo != a.massNo ? (b.massNo < a.massNo ? -1 : 1) : 0;
  }

}
