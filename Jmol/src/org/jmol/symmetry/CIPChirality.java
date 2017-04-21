package org.jmol.symmetry;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.V3;

import org.jmol.java.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

/**
 * A relatively simple implementation of Cahn-Ingold-Prelog rules for assigning
 * R/S and E/S stereochemical labels. Based on IUPAC rules. (See text at the end
 * of this document.)
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
 * 
 * Introduced in Jmol 14.12.0 Validated for Rules 1 and 2 in Jmol 14.13.2 E/Z
 * added 14.14.1
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class CIPChirality {

  // Very rough Pseudocode:
  // 
  // individual "standard" carbon-based R/S and E/Z stereochemistry (Rules 1, 2, and 3)
  //
  // getChirality(atom) {
  //  if (atom.getCovalentBondCount() != 4) exit NO_CHIRALITY 
  //  for (each Rule){  
  //    sortSubstituents() 
  //    if (done) exit checkHandedness();
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

  // Jmol test suite:
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
  static final int NA = -2; // not applicable

  static final int STEREO_R = 1;
  static final int STEREO_S = 2;

  static final int STEREO_Z = 1;
  static final int STEREO_E = 2;

  static final int RULE_1A = 0;
  static final int RULE_1B = 1;
  static final int RULE_2 = 2;
  static final int RULE_3 = 3;
  static final int RULE_4 = 4;
  static final int RULE_5 = 5;
  
  public String getRuleName() {
    switch (currentRule) {
    case RULE_1A:
      return "1a";
    case RULE_1B:
      return "1b";
    default:
      return "" + currentRule;
    }
  }
  /**
   * Jmol viewer that created this CIPChirality object
   * 
   */
  private Viewer vwr;

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
  int currentRule = RULE_1A;

  /**
   * Rule 1b hash table that maintains distance of the associated nonduplicated
   * atom node
   * 
   */
  Map<String, Integer> htPathPoints;
  
  /**
   * Max priorities across an entire molecule, for cyclic loops
   */
  int nPriorityMax;
  
  /**
   * Max ring size from closures, across a molecule
   * 
   */
  int maxRingSize;
  
  boolean useAuxiliaries;

  public CIPChirality() {
    // for reflection 
  }

  public CIPChirality setViewer(Viewer vwr) {
    this.vwr = vwr;
    return this;
  }

  /**
   * A more general determination of chirality that involves ultimately all
   * Rules 1-5
   * 
   * @param atoms
   * 
   * @param bsAtoms
   */
  public void getChiralityForAtoms(Node[] atoms, BS bsAtoms) {
    init();

    // Initial Rules 1-3 only

    BS bsToDo = BSUtil.copy(bsAtoms);
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      Node atom = atoms[i];

      // This call checks to be sure we do not already have it.

      String c = atom.getCIPChirality(false);
      if (c.length() > 0) {
        bsToDo.clear(i);
      } else {
        atom.setCIPChirality(getAtomChiralityLimited(atom, null, null, 3, -1));
      }
    }

    // E/Z -- Rule 3

    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      getAtomBondChirality(atoms[i], true);
    }

    // Necessary? Perhaps for pseudo-chirality 

    useAuxiliaries = (bsToDo.equals(bsAtoms) && nPriorityMax == 3 && maxRingSize > 0);
      
    for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1)) {
      Node a = atoms[i];
      if (a.getCovalentBondCount() != 4)
        continue;
      a.setCIPChirality(0);
      a.setCIPChirality(getAtomChiralityLimited(a, null, null, 5, -1));
    }
  }

  private void init() {
    useAuxiliaries = false;
    nPriorityMax = maxRingSize = 0;
  }

  /**
   * Determine the Cahn-Ingold-Prelog R/S chirality of an atom based only on
   * Rules 1 and 2 -- that is, only the chirality that is self-determined,
   * structural, and not relative-chirality dependent.
   * 
   * @param atom
   * @return [0:none, 1:R, 2:S]
   */
  public int getAtomChirality(Node atom) {
    init();
    return getAtomChiralityLimited(atom, null, null, 3, -1);
  }

  /**
   * Determine the Cahn-Ingold-Prelog E/Z chirality of a bond based only on
   * Rules 1 and 2 -- that is, only the chirality that is self-determined,
   * structural, and not relative-chirality dependent.
   * 
   * @param bond
   * @return [0:none, 1:Z, 2:E]
   */
  public int getBondChirality(Edge bond) {
    init();
    return getBondChiralityLimited(bond, 3);
  }

  /**
   * Get E/Z characteristics for specific atoms
   * 
   * @param atom
   * @param allBonds
   */

  private void getAtomBondChirality(Node atom, boolean allBonds) {
    Edge[] bonds = atom.getEdges();
    int index = atom.getIndex();
    for (int j = bonds.length; --j >= 0;) {
      Edge bond = bonds[j];
      if (bond.getCovalentOrder() == 2
          && (allBonds || bond.getOtherAtomNode(atom).getIndex() > index)) {
        getBondChirality(bond);
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
   * @param iref
   * @return if parent != null: [0:none, 1:R, 2:S] otherwise [0:none, 1:
   *         atoms[0] is higher, 2: atoms[1] is higher]
   */
  private int getAtomChiralityLimited(Node atom, CIPAtom cipAtom,
                                      CIPAtom parent, int ruleMax, int iref) {
    int rs = NO_CHIRALITY;
    boolean isChiral = false;
    if (cipAtom != null)
      atom = cipAtom.atom;
    int nSubs = atom.getCovalentBondCount();
    boolean isAlkene = (nSubs == 3);
    if (nSubs == (parent == null ? 4 : 3)) {
      if (cipAtom == null) {
        htPathPoints = new Hashtable<String, Integer>();
        cipAtom = new CIPAtom(atom, null, false);
      }
      root = cipAtom;
      cipAtom.parent = parent;
      currentRule = RULE_1A;
      if (cipAtom.set()) {
        try {
          if (iref >= 0)
            cipAtom.bsPath.set(iref);
          boolean doResetAux = false;
          for (currentRule = RULE_1A; currentRule <= ruleMax && !isChiral; currentRule++) {
            if (Logger.debugging)
              Logger.info("-Rule " + getRuleName() + " CIPChirality for "
                  + cipAtom + "-----");
            
            if (currentRule == RULE_4 && useAuxiliaries) {
              cipAtom.createAuxiliaryRSCenters(true);
              doResetAux = true;
            }
            isChiral = false;
            cipAtom.sortSubstituents();
            isChiral = true;
            if (Logger.debugging) {
              Logger.info(currentRule + ">>>>" + cipAtom);
              for (int i = 0; i < cipAtom.bondCount; i++)
                Logger.info(cipAtom.atoms[i] + " "
                    + Integer.toHexString(cipAtom.prevPriorities[i]));
            }
            if (cipAtom.aChiral)
              isChiral = false;
            else
              for (int i = 0; i < cipAtom.bondCount - 1; i++) {
                if (cipAtom.prevPriorities[i] == cipAtom.prevPriorities[i + 1]) {
                  isChiral = false;
                  break;
                }
              }
          }
          if (doResetAux) {
            cipAtom.resetAuxiliaryChirality();
          }
          if (isChiral) {
            rs = (!isAlkene ? checkHandedness(cipAtom)
                : cipAtom.atoms[0].isDuplicate ? STEREO_S : STEREO_R);
          }
          if (cipAtom.isPseudo && !isAlkene)
            rs = rs | JC.CIP_CHIRALITY_PSEUDO_FLAG;
        } catch (Throwable e) {
          System.out.println(e + " in CIPChirality");
          if (!vwr.isJS)
            e.printStackTrace();
        }
        if (Logger.debugging)
          Logger.info(atom + " " + rs);
      }
    }
    if (Logger.debugging)
      Logger.info("----------------------------------");
    return rs;
  }

  /**
   * 
   * @param bond
   * @param ruleMax
   * @return [0:none, 1:Z, 2:E]
   */
  private int getBondChiralityLimited(Edge bond, int ruleMax) {
    if (Logger.debugging)
      Logger.info("get Bond Chirality " + bond);
    int ez = NO_CHIRALITY;
    Node[] atoms = vwr.ms.at;
    if (bond.getCovalentOrder() == 2) {
      Node a = atoms[bond.getAtomIndex1()];
      Node b = atoms[bond.getAtomIndex2()];
      htPathPoints = new Hashtable<String, Integer>();
      CIPAtom a1 = new CIPAtom(a, null, false);
      CIPAtom b1 = new CIPAtom(b, null, false);
      //b1.fillNull(0);
      int atop = getAtomChiralityLimited(a, a1, b1, ruleMax, -1) - 1;
      htPathPoints = new Hashtable<String, Integer>();
      CIPAtom a2 = new CIPAtom(a, null, false);
      CIPAtom b2 = new CIPAtom(b, null, false);
      //a2.fillNull(0);
      int btop = getAtomChiralityLimited(b, b2, a2, ruleMax, -1) - 1;
      if (atop >= 0 && btop >= 0) {
        ez = (isCIS(b2.atoms[btop], b2, a1, a1.atoms[atop]) ? STEREO_Z
            : STEREO_E);
      }
      if (ez != 0) {
        a.setCIPChirality(ez << 3);
        b.setCIPChirality(ez << 3);
      }
      if (Logger.debugging)
        Logger.info(bond + " "
            + (ez == STEREO_Z ? "Z" : ez == STEREO_E ? "E" : "_"));
    }
    return ez;
  }

  /**
   * determine the winding of the circuit p1--p2--p3 relative to point p4
   * 
   * @param a
   * @return 1 for "R", 2 for "S"
   */
  static int checkHandedness(CIPAtom a) {
    P3 p1 = (P3) a.atoms[0].atom; // highest priority
    P3 p2 = (P3) a.atoms[1].atom;
    P3 p3 = (P3) a.atoms[2].atom;
    P3 p4 = (P3) a.atoms[3].atom; // lowest priority
    V3 vNorm = new V3();
    float d = Measure.getNormalThroughPoints(p1, p2, p3, vNorm, new V3());
    return (Measure.distanceToPlaneV(vNorm, d, p4) > 0 ? STEREO_R : STEREO_S);
  }

  static boolean isCIS(CIPAtom me, CIPAtom parent, CIPAtom grandParent,
                       CIPAtom greatGrandParent) {
    V3 vNorm1 = new V3();
    V3 vTemp = new V3();
    Measure.getNormalThroughPoints((P3) me.atom, (P3) parent.atom,
        (P3) grandParent.atom, vNorm1, vTemp);
    V3 vNorm2 = new V3();
    Measure.getNormalThroughPoints((P3) parent.atom, (P3) grandParent.atom,
        (P3) greatGrandParent.atom, vNorm2, vTemp);
    return (vNorm1.dot(vNorm2) > 0);
  }

  final static int[] PRIORITY_SHIFT = new int[] { 24, 20, 12, 8, 4, 0 };

  private class CIPAtom implements Comparable<CIPAtom>, Cloneable {

    /**
     * the associated Jmol (or otherwise). Use of Node allows us to implement
     * this in SMILES or Jmol
     * 
     */
    Node atom;

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
     * Rule 1 element number
     */
    private int elemNo;

    /**
     * Rule 2 nominal atomic mass; may be a rounded value so that 12C is the
     * same as C
     */
    private int massNo;

    /**
     * bond distance from the root atom to this atom
     */
    private int sphere;

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
     * atom.
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
     * already-determined auxiliary chirality (E/Z, R/S, etc) for this atom node
     */
    private int auxiliaryEZ = -1;

    /**
     * pre-determined absolute root chirality of the associated atom from
     * previous determinations
     */
    private String knownAtomChirality = "~";

    /**
     * Temporary single-use auxiliary atom chirality for inositol-breaking
     */
    private String auxAtomChirality = null;

    /**
     * pre-determined chirality along the path to this atom from the root atom
     * node
     */
    private String knownChiralityPathFull = "~";

    /**
     * pre-determined chirality along the path to this atom from the root atom
     * node not including ~
     */
    private String knownChiralityPathAbbr = "";


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
    public boolean isPseudo;

    /**
     * Force achiral condition due to double ties.
     */
    boolean aChiral;

    /**
     * true atom covalent bond count
     */
    public int bondCount;

    /**
     * the substituents -- up to four supported here at this time
     * 
     */
    CIPAtom[] atoms = new CIPAtom[4];

    /**
     * priorities associated with each subsituent from high (1) to low (4); due
     * to equivaliencies at a given rule level, these numbers may duplicate and
     * have gaps - for example, [1 1 3 4]
     */
    private int[] priorities = new int[4];

    /**
     * a number that encodes a substituent's priority prior to any comparisons
     * by previous rules; in the form abcde where a-e are digits 1-4, and a is
     * for Rule 1, b is for Rule 2, etc.
     * 
     */
    int[] prevPriorities = new int[4];

    /**
     * 
     * @param atom
     *        or null to indicate a null placeholder
     * @param parent
     * @param isDuplicate
     */
    CIPAtom(Node atom, CIPAtom parent, boolean isDuplicate) {
      this.id = ++ptID;
      this.parent = parent;
      if (atom == null)
        return;
      this.atom = atom;
      bondCount = atom.getCovalentBondCount();
      String c = atom.getCIPChirality(false);
      // What we are doing here is creating a lexigraphically sortable string
      // R < S < r < s < ~ and C < T < ~ 
      if (c.equals(""))
        c = "~";
      else if (c.equals("E"))
        c = "c";
      else if (c.equals("Z"))
        c = "t";
      if (parent != null) {
        sphere = parent.sphere + 1;
        knownChiralityPathFull = parent.knownChiralityPathFull + c;
        knownChiralityPathAbbr = parent.knownChiralityPathAbbr
            + (c == "~" ? "" : c);
      }
      knownAtomChirality = c;
      if (sphere == 1)
        rootSubstituent = this;
      else if (parent != null)
        rootSubstituent = parent.rootSubstituent;
      this.isTerminal = atom.getCovalentBondCount() == 1;
      this.elemNo = atom.getElementNumber();
      this.massNo = atom.getNominalMass();
      this.bsPath = (parent == null ? new BS() : BSUtil.copy(parent.bsPath));

      int iatom = atom.getIndex();
      if (parent == null) {
        // original atom
        bsPath.set(iatom);
        rootDistance = 0;
      } else if (atom == root.atom) {
        // pointing to original atom
        rootDistance = 0;
        if (sphere > maxRingSize)
          maxRingSize = sphere;
        isDuplicate = true;
      } else if (bsPath.get(iatom)) {
        isDuplicate = true;
        rootDistance = htPathPoints.get(rootSubstituent.atom.toString() + atom)
            .intValue();
      } else {
        bsPath.set(iatom);
        rootDistance = parent.rootDistance + 1;
        htPathPoints.put(rootSubstituent.atom.toString() + atom, new Integer(
            rootDistance));
      }
      this.isDuplicate = isDuplicate;
      
      myPath = (parent != null ? parent.myPath + "-" : "") + this;

      if (Logger.debugging)
        Logger.info("new CIPAtom " + parent + "->" + this);
    }

    /**
     * Set the atom to have substituents.
     * 
     * @return true if a valid atom for consideration
     * 
     */
    boolean set() {
      if (isSet)
        return true;
      isSet = true;
      if (isTerminal || isDuplicate)
        return true;
      atoms = new CIPAtom[4];
      int nBonds = atom.getBondCount();
      Edge[] bonds = atom.getEdges();
      if (Logger.debugging)
        Logger.info("set " + this);
      int pt = 0;
      for (int i = 0; i < nBonds; i++) {
        Edge bond = bonds[i];
        if (!bond.isCovalent())
          continue;
        Node other = bond.getOtherAtomNode(atom);
        boolean isParent = (parent != null && parent.atom == other);
        int order = bond.getCovalentOrder();
        if (isParent && order == 2) {
          isAlkeneAtom2 = true;
          knownAtomChirality = bond.getCIPChirality(false);
        }
        switch (order) {
        case 3:
          if (addAtom(pt++, other, isParent) == null) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 2:
          if (addAtom(pt++, other, order != 2 || isParent) == null) {
            isTerminal = true;
            return false;
          }
          //$FALL-THROUGH$
        case 1:
          if (!isParent && addAtom(pt++, other, order != 1) == null) {
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
      fillNull(pt);

      // Do an initial atom-only shallow sort using a.compareTo(b)

      int ruleNow = currentRule;
      currentRule = RULE_1A;
      Arrays.sort(atoms);
      currentRule = ruleNow;

      return !isTerminal;
    }

    private void fillNull(int pt) {
      for (; pt < atoms.length; pt++)
        atoms[pt] = new CIPAtom(null, this, true);
    }

    /**
     * Add a new atom or return null
     * 
     * @param i
     * @param other
     * @param isDuplicate
     * @return new atom or null
     */
    private CIPAtom addAtom(int i, Node other, boolean isDuplicate) {
      if (i >= atoms.length) {
        if (Logger.debugging)
          Logger.info(" too many bonds on " + atom);
        return null;
      }
      if (parent == null) {
        // For top level, we do not allow two 1H atoms.
        int atomIsotope = other.getAtomicAndIsotopeNumber();
        if (atomIsotope == 1) {
          if (++h1Count > 1) {
            if (Logger.debugging)
              Logger.info(" second H atom found on " + atom);
            return null;
          }
        }
      }
      atoms[i] = new CIPAtom(other, this, isDuplicate);
      if (currentRule > RULE_2)
        prevPriorities[i] = getBasePriority(atoms[i]);
      //      if (Logger.debugging)
      //        Logger.info(this + " adding " + i + " " + atoms[i]);
      return atoms[i];
    }

    /**
     * Deep-Sort the substituents of an atom. Don't allow ties if this is the
     * first atom (parent == null).
     * 
     */
    void sortSubstituents() {
      //if (isSortedRule[currentRule])
      // return;
      //isSortedRule[currentRule] = true;

      int n = 4;

      if (Logger.debugging) {
        Logger.info("---sortSubstituents---" + atom);
        for (int i = 0; i < n; i++)
          Logger.info(getRuleName() + ": " + this + "[" + i + "]=" + atoms[i].myPath
              + " " + Integer.toHexString(prevPriorities[i]));
      }

      int[] indices = new int[4];

      BS ties = null;

      for (int i = 0; i < n; i++) {
        priorities[i] = 1;
        //        if (prevPriorities[i] != 0 && 
        //            (prevPriorities[i]&0xFF000000) != (getBasePriority(atoms[i]) & 0xFF000000))
        //          System.out.println("???????");
        if (prevPriorities[i] == 0 && currentRule > RULE_1B)
          prevPriorities[i] = getBasePriority(atoms[i]);
      }
      for (int i = 0; i < n; i++) {
        CIPAtom a = atoms[i];
        for (int j = i + 1; j < n; j++) {
          CIPAtom b = atoms[j];
          if (Logger.debuggingHigh)
            Logger.info("ordering " + this.id + "." + i + "." + j + " " + this
                + "-" + a + " vs " + b + " " + " Rule " + getRuleName()
                + Integer.toHexString(prevPriorities[i]) + " "
                + Integer.toHexString(prevPriorities[j]));
          int score = compareSubs(a, b, i, j);
          if (Logger.debuggingHigh)
            Logger.info("ordering " + this.id + "." + i + "." + j + " " + this
                + "-" + a + " vs " + b + " = " + score + " Rule " + getRuleName());
          switch (score) {
          case NA:
            System.out.println("OHNO");
            break;
          case B_WINS:
            indices[i]++;
            priorities[i]++;
            if (Logger.debuggingHigh)
              Logger.info(atom + "." + b + " B-beats " + a  + " Rule " + getRuleName()+ " ind="
                  + indices[i]);
            break;
          case A_WINS:
            indices[j]++;
            priorities[j]++;
            if (Logger.debuggingHigh)
              Logger.info(atom + "." + a + " A-beats " + b  + " Rule " + getRuleName()+ " ind="
                  + indices[j]);
            break;
          case TIED:
            switch (a.breakTie(b)) {
            case NA:
              System.out.println("OHNO2");
              break;
            case TIED:
              indices[i]++;
              if (Logger.debuggingHigh)
                Logger.info(atom + "." + b + " ends up with tie with " + a + " Rule " + getRuleName()
                    + " ind=" + indices[i]);
              break;
            case B_WINS:
              indices[i]++;
              priorities[i]++;
              if (Logger.debuggingHigh)
                Logger.info(atom + "." + b + " wins in tie with " + a + " Rule " + getRuleName() + " ind="
                    + indices[i] + "\n");
              break;
            case A_WINS:
              indices[j]++;
              priorities[j]++;
              if (Logger.debuggingHigh)
                Logger.info(atom + "." + a + " wins in tie with " + " Rule " + getRuleName() + b + " ind="
                    + indices[j] + "\n");
              break;
            }
            break;
          }
          if (doCheckPseudo) {
            doCheckPseudo = false;
            if (ties == null)
              ties = new BS();
            ties.set(i);
            ties.set(j);
          }
        }
      }

      // update the substituent arrays

      CIPAtom[] newAtoms = new CIPAtom[n];
      int[] newPriorities = new int[n];
      int[] newPrevPriorities = new int[n];

      BS bs = new BS();
      int shift = PRIORITY_SHIFT[currentRule];
      for (int i = 0; i < n; i++) {
        int pt = indices[i];
        CIPAtom a = newAtoms[pt] = atoms[i];
        int p = priorities[i];
        newPriorities[pt] = p;
        // can't do this, because of tie breaking
        //        switch (currentRule) {
        //        case 0:
        //          p = 127 - (a.atom == null ? 0 : a.elemNo);
        //          break;
        //        case 2:
        //          p = 255 - (a.atom == null ? 0 : a.massNo);
        //          break;
        //        }
        newPrevPriorities[pt] = prevPriorities[i] | (p << shift);
        if (a.atom != null)
          bs.set(priorities[i]);
      }
      atoms = newAtoms;
      priorities = newPriorities;
      prevPriorities = newPrevPriorities;
      nPriorities = bs.cardinality();
      if (nPriorities > nPriorityMax)
        nPriorityMax = nPriorities;

      if (ties != null) {
        if (ties.cardinality() == 2) {
          if (sphere != 0 || useAuxiliaries) {
            checkPseudoHandedness(ties, indices);
          }
        } else if (sphere == 0) {
          aChiral = true;
        }
      }
      if (Logger.debugging) {
        Logger.info(atom + " nPriorities = " + nPriorities);
        for (int i = 0; i < n; i++)
          Logger.info(this.myPath + "[" + i + "]=" + atoms[i] + " " + priorities[i] + " new");
        Logger.info("-------");
      }
    }

    private int compareSubs(CIPAtom a, CIPAtom b, int i, int j) {
      return (a.atom == null ? B_WINS : b.atom == null ? A_WINS
          : prevPriorities[i] == prevPriorities[j] ? a.compareTo(b)
              : prevPriorities[j] < prevPriorities[i] ? B_WINS : A_WINS);
    }

    /**
     * This check is not technically one of those listed in the rules, but it us
     * useful when preparing to check substituents because if one of the atoms
     * has substituents and the other doesn't, we are done -- there is no reason
     * to check substituents.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkDuplicate(CIPAtom b) {
      return b.isDuplicate == isDuplicate ? TIED : b.isDuplicate ? A_WINS
          : B_WINS;
    }

    /**
     * Break a tie at any level in the iteration between to atoms that otherwise
     * are the same by sorting their substituents.
     * 
     * @param b
     * @return 1 to indicate this is the winner, -1 to indicate b is the winner;
     *         0 for a tie
     */
    private int breakTie(CIPAtom b) {

      // Do a duplicate check -- if that is not a TIE we do not have to go any further.

      int score = checkDuplicate(b);
      if (score != TIED)
        return score;

      // return NO_CHIRALITY/TIED if:
      //  a) both refer to the same atom
      //  b) one or the other can't be set (because it has too many connections)
      //  c) or one or the other is terminal (has no substituents) 
      //  d) or they are the same atom
      if (atom == null)// || currentRule < 3))
        //if (atom == b.atom && (atom == null || currentRule < 3))  
        return TIED;
      if ((atom == null) != (b.atom == null))
        return (atom == null ? B_WINS : A_WINS);
      if (!set() || !b.set() || isTerminal || b.isTerminal || isDuplicate
          && b.isDuplicate)
        return TIED;
      if (Logger.debugging)
        Logger.info("tie for " + this + " and " + b);

      // Phase I -- shallow check only
      //
      // Check to see if any of the three connections to a and b are different.
      // Note that we do not consider duplicate atoms different from regular atoms here
      // because we are just looking for atom differences, not substituent differences.
      //
      // Say we have {O (O) C} and {O O H}
      //
      // The rules require that we first only look at just the atoms, so OOC beats OOH.

      if ((score = compareWith(b, false)) != TIED)
        return score;

      // Phase II -- deep check using breakTie
      //
      // OK, so all three are nominally the same.
      // Now seriously deep-sort each list based on substituents
      // and then check them one by one to see if the tie can be broken.

      sortSubstituents();
      b.sortSubstituents();
      return compareWith(b, true);
    }

    private int compareWith(CIPAtom b, boolean goDeep) {
      for (int i = 0; i < nAtoms; i++) {
        CIPAtom ai = atoms[i];
        CIPAtom bi = b.atoms[i];
        if (Logger.debugging)
          Logger.info("compareAB " + ai.parent + "-" + ai + " with "
              + bi.parent + "-" + bi + " goDeep=" + goDeep);
        int score = (goDeep ? ai.breakTie(bi) : ai.checkCurrentRule(bi));
        if (score == NA)
          score = TIED;
        if (score != TIED) {
          if (Logger.debugging)
            Logger.info("compareAB "
                + (score == B_WINS ? bi + " beats " + ai : ai + " beats " + bi)
                + " by Rule " + getRuleName());
          return score;
        }
      }
      if (Logger.debugging)
        Logger.info("compareAB ends in tie for " + this + " vs. " + b + " Rule " + getRuleName());
      return TIED;
    }

    /**
     * Used in Array.sort and sortSubstituents; includes a preliminary check for
     * duplicate, since we know that that atom will ultimately be lower priority
     * if all other rules are tied. This is just a convenience.
     * 
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    @Override
    public int compareTo(CIPAtom b) {
      int score;
      // null should never win
      if ((atom == null) != (b.atom == null))
        return (atom == null ? B_WINS : A_WINS);
      // fullPriority is cumulative over previous Rule applications
      // 
      //if (b.fullPriority != fullPriority)
      //  return (b.fullPriority < fullPriority ? B_WINS : A_WINS);
      return (score = checkCurrentRule(b)) == NA ? TIED : score != TIED ? score
          : checkDuplicate(b);
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
      case RULE_1A:
        return checkRule1a(b);
      case RULE_1B:
        return checkRule1b(b);
      case RULE_2:
        return checkRule2(b);
      case RULE_3:
        return checkRule3(b);
      case RULE_4:
        return checkRule4(b);
      case RULE_5:
        return checkRule5(b);// handled at the end of Rule 4 checkRule5(b);
      }
    }

    /**
     * Looking for same atom or ghost atom or element number
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRule1a(CIPAtom b) {
      return b.atom == atom ? TIED : b.atom == null ? A_WINS
          : atom == null ? B_WINS : b.elemNo < elemNo ? A_WINS
              : b.elemNo > elemNo ? B_WINS : TIED;
    }

    /**
     * Looking for root distance -- duplicate atoms only.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */

    private int checkRule1b(CIPAtom b) {
      return !b.isDuplicate || !isDuplicate ? TIED
          : b.rootDistance < rootDistance ? B_WINS
              : b.rootDistance > rootDistance ? A_WINS : TIED;
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
     * Chapter 9 Rule 3. E/Z. If the this atom and the comparison atom b are on
     * the same parent, then this is simply a request to break their tie based
     * on Rule 3; otherwise two paths are being checked for one being seqCis and
     * one being seqTrans.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRule3(CIPAtom b) {
      int za, zb;
      return parent == null || !parent.isAlkeneAtom2 || !b.parent.isAlkeneAtom2
          || isDuplicate || b.isDuplicate ? NA
          : parent == b.parent ? breakTie(b)
              : (za = parent.getZaux()) < (zb = b.parent.getZaux()) ? A_WINS
                  : za > zb ? B_WINS : TIED;
    }

    /**
     * Check auxiliary Z by temporarily setting return path.
     * 
     * This method uses CIPAtom.clone() effectively to create a second
     * independent path that is checked without messing up the currently
     * expanding polygraph.
     * 
     * Note that one path from the current atom is in reverse - the path back to
     * the root atom. This must be reconstructed, because until this point we
     * have not carried out many of the necessary comparisons.
     * 
     * @return [1: "Zaux", 2: "Eaux", 3: no chirality]
     */
    private int getZaux() {
      // this is the second atom of the alkene, checked as the parent of the next atom
      // (because there is no need to do this test until we are on the branch that includes
      //  the atom after the alkene).
      if (auxiliaryEZ < 0)
        auxiliaryEZ = parent.auxiliaryEZ;
      if (auxiliaryEZ < 0) {
        CIPAtom winner1 = null;
        CIPAtom winner2 = null;
        CIPAtom atom1 = null;
        auxiliaryEZ = 3;
        sortSubstituents();
        winner2 = getTopAtom();
        if (winner2 != null) {
          if (Logger.debugging)
            Logger.info("reversing path for " + parent);
          Lst<CIPAtom> path = getReturnPath(parent);
          atom1 = (CIPAtom) parent.clone();
          atom1.addReturnPath(this, path);
          atom1.sortSubstituents();
          winner1 = atom1.getTopAtom();
          if (winner1 != null) {
            auxiliaryEZ = (isCIS(winner2, this, atom1, winner1) ? STEREO_Z
                : STEREO_E);
            if (Logger.debugging)
              Logger.info("getZaux " + (auxiliaryEZ == STEREO_Z ? "Z" : "E")
                  + " for " + this.atom + "=" + this.parent.atom + " : "
                  + winner1 + " " + winner2);
          }
        }
      }
      parent.auxiliaryEZ = auxiliaryEZ;
      return auxiliaryEZ;
    }

    private Lst<CIPAtom> getReturnPath(CIPAtom a) {
      Lst<CIPAtom> path = new Lst<CIPAtom>();
      while (a.parent != null && a.parent.atoms[0] != null) {
        if (Logger.debugging)
          Logger.info("path:" + a.parent.atom + "->" + a.atom);
        path.addLast(a = a.parent);
      }
      path.addLast(null);
      return path;
    }

    /**
     * 
     * @param last
     * @param path
     */
    private void addReturnPath(CIPAtom last, Lst<CIPAtom> path) {
      CIPAtom thisAtom = this;
      for (int i = 0, n = path.size(); i < n; i++) {
        CIPAtom p = path.get(i);
        if (p == null)
          p = new CIPAtom(null, this, true);
        else
          p = (CIPAtom) p.clone();
        thisAtom.replaceParentSubstituent(last, p);
        if (last == null)
          break;
        last = last.parent;
        thisAtom = p;
      }
    }

    /** 
     * Reverse the path to the parent and check r/s chirality
     * @param ties 
     * @param indices 
     * 
     */
    private void checkPseudoHandedness(BS ties, int[] indices) {
      Lst<CIPAtom> path = getReturnPath(this);
      CIPAtom atom1 = (CIPAtom) clone();
      int ia = ties.nextSetBit(0);
      int ib = ties.nextSetBit(ia + 1);
      // critical here that we do NOT include the tied branches
      CIPAtom tie1 = atoms[Math.min(indices[ia], indices[ib])];
      CIPAtom tie2 = atoms[Math.max(indices[ia], indices[ib])];
      atom1.atoms[indices[ia]] = new CIPAtom(null, atom1, false);
      atom1.atoms[indices[ib]] = new CIPAtom(null, atom1, false);
      atom1.addReturnPath(null, path);
      int thisRule = currentRule;
      currentRule = RULE_1A;
      atom1.sortSubstituents();
      // Now add the tied branches at the end; it doesn't matter where they 
      // go as long as they are together and in order. 
      atom1.atoms[2] = tie1;
      atom1.atoms[3] = tie2;
      currentRule = thisRule;
      int rs = checkHandedness(atom1);
      if (Logger.debugging) {
        for (int i = 0; i < 4; i++)
          Logger.info("pseudo " +  rs + " " + priorities[i] + " " + atoms[i].myPath);
      }
      if (rs == STEREO_R || rs == STEREO_S) {
        isPseudo = true;
        setChirality(rs == STEREO_R ? "r" : "s");
      }
    }


    private int getBasePriority(CIPAtom a) {
      int code = 0;
      if (a.atom == null) {
        code = 0x7FFFF << PRIORITY_SHIFT[2];
      } else {
        code = ((127 - a.elemNo) << PRIORITY_SHIFT[0])
            | ((255 - a.massNo) << PRIORITY_SHIFT[2]);
      }
      return code;
    }
    
    /**
     * @param isRoot unused 
     */
    void createAuxiliaryRSCenters(boolean isRoot) {
      if (atom == null)
        return;
      for (int i = 0; i < 4; i++) {
        CIPAtom a = atoms[i];
        if (a != null && !a.isDuplicate && !a.isTerminal)
          a.createAuxiliaryRSCenters(false);
      }
      System.out.println("createAux " + this + " " + nPriorities + " " + isRoot);
      if (isRoot || bondCount != 4 || nPriorities > 0 && nPriorities < 3)
        return;
      if (nPriorities == 0)
        return;
      CIPAtom atom1 = (CIPAtom) clone();
      if (!atom1.set())
        return;
      Lst<CIPAtom> path = getReturnPath(this);
      atom1.addReturnPath(null, path);
      int thisRule = currentRule;
      currentRule = RULE_1A;
      atom1.sortSubstituents(); //this sort is not going right
      currentRule = thisRule;
      int rs = checkHandedness(atom1);
      System.out.println(rs + " " + myPath + JC.getCIPChiralityName(rs));
      if (rs == STEREO_R || rs == STEREO_S) {
        setAuxiliaryChirality(rs == STEREO_R ? "R" : "S");
      }

      currentRule = thisRule;
    }


    /**
     * Swap a substituent and the parent in preparation for reverse traversal of
     * this path
     * 
     * @param newParent
     * @param newSub
     */
    private void replaceParentSubstituent(CIPAtom newParent, CIPAtom newSub) {
      for (int i = 0; i < 4; i++)
        if (atoms[i] == newParent || newParent == null && atoms[i].atom == null) {
          atoms[i] = newSub;
          if (Logger.debugging)
            Logger.info("replace " + this + "[" + i + "]=" + newSub);
          prevPriorities[i] = getBasePriority(atoms[i]);
          parent = newParent;
          break;
        }
    }

    /**
     * Return top non-duplicated atom.
     * 
     * @return highest-priority non-duplicated atom
     */
    private CIPAtom getTopAtom() {
      int i = (atoms[0].isDuplicate ? 1 : 0);
      return priorities[i] == priorities[i + 1] ? null
          : priorities[i] < priorities[i + 1] ? atoms[i] : atoms[i + 1];
    }

    /**
     * Chapter 9 Rules 4 and 5: like vs. unlike
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRule4(CIPAtom b) {
      if (Logger.debugging)
        Logger.info("Checking Rule 4 for " + this + " and " + b);
      CIPAtom anc = getCommonAncestor(b);
      int l = anc.knownChiralityPathAbbr.length();
      if (l >= knownChiralityPathAbbr.length())
        return TIED; // some H atoms
      if (getWorkingChirality().equals("~") && b.getWorkingChirality().equals("~")) 
        return TIED;
      String acAbbr = knownChiralityPathAbbr.substring(l);
      int n = acAbbr.length();
      String bcAbbr = b.knownChiralityPathAbbr.substring(l);
      if (n == 0 || n != bcAbbr.length())
        return TIED;
      // these next will be needed for full Mata analysis;
      //l = anc.knownChiralityPathFull.length();
      //String acFull = knownChiralityPathFull.substring(l);
      //String bcFull = b.knownChiralityPathFull.substring(l);      
      anc.doCheckPseudo = false;
      char aref = acAbbr.charAt(0);
      char bref = bcAbbr.charAt(0);
      for (int j = 1; j < n; j++) {
        boolean alike = (aref == acAbbr.charAt(j));
        boolean blike = (bref == bcAbbr.charAt(j));
        if (alike != blike)
          return (alike ? A_WINS : B_WINS);
      }
      if (aref != bref) {
        anc.doCheckPseudo = true;
        return aref < bref ? A_WINS : B_WINS;
      }
      return TIED;
    }

    private String getWorkingChirality() {
      return (auxAtomChirality == null ? knownAtomChirality : auxAtomChirality);
    }

    /**
     * Update the chirality path for this atom. Needs to also run through all decendents.
     * @param rs
     */
    private void setChirality(String rs) {
      System.out.println("set chirality " + this + " " + rs + " " + knownChiralityPathFull);
      knownAtomChirality = rs;
      atom.setCIPChirality(JC.getCIPChiralityCode(rs));
      knownChiralityPathFull = (parent == null ? "" : parent.knownChiralityPathFull) + rs;
      knownChiralityPathAbbr = PT.rep(knownChiralityPathFull, "~", "");
      System.out.println("set chirality " + this + " " + rs + " " + knownChiralityPathFull);
      for (int i = 0; i < 4; i++)
        if (atoms[i] != null && atoms[i].atom != null)
          atoms[i].setChirality(atoms[i].knownAtomChirality);
    }

    /**
     * Update the chirality path for this atom. Needs to also run through all decendents.
     * @param rs
     */
    private void setAuxiliaryChirality(String rs) {
      System.out.println("set auxchirality " + this + " " + rs + " " + knownChiralityPathFull);
      auxAtomChirality = rs;
      knownChiralityPathFull = (parent == null ? "" : parent.knownChiralityPathFull) + rs;
      knownChiralityPathAbbr = PT.rep(knownChiralityPathFull, "~", "");
      System.out.println("set chirality " + this + " " + rs + " " + knownChiralityPathFull);
      for (int i = 0; i < 4; i++)
        if (atoms[i] != null && atoms[i].atom != null)
          atoms[i].setAuxiliaryChirality(atoms[i].knownAtomChirality);
    }
    
    void resetAuxiliaryChirality() {
      auxAtomChirality = null;
      for (int i = 0; i < 4; i++)
        if (atoms[i] != null && atoms[i].atom != null)
          atoms[i].resetAuxiliaryChirality();
    }

    private CIPAtom getCommonAncestor(CIPAtom b) {
      CIPAtom a = this;
      if ((a.parent == null) != (b.parent == null))
        System.out.println("OHOH3");
      while ((a = a.parent) != (b = b.parent)) {
      }
      return a;
    }

    /**
     * Chapter 9 Rule 5. Implemented only for a single R/S.
     * 
     * For 3D models is it not necessary to consider "unspecified" chirality, as
     * that is only a 2D concept.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRule5(CIPAtom b) {
      // TODO
      int isRa = ";srSR;".indexOf(knownAtomChirality);
      int isRb = ";srSR;".indexOf(b.knownAtomChirality);
      return (isRa == isRb ? TIED : isRa > isRb ? A_WINS : B_WINS);
    }

    @Override
    public Object clone() {
      CIPAtom a = null;
      try {
        a = (CIPAtom) super.clone();
        a.id = ptID++;
      } catch (CloneNotSupportedException e) {
      }
      a.atoms = new CIPAtom[4];
      a.priorities = new int[4];
      a.prevPriorities = new int[4];
      for (int i = 0; i < 4; i++) {
        a.priorities[i] = priorities[i];
        if (atoms[i] != null) {
          a.atoms[i] = atoms[i];
          a.prevPriorities[i] = getBasePriority(atoms[i]);
        }
      }
      if (Logger.debugging)
        Logger.info("cloning " + this + " as " + a);
      return a;

    }


    @Override
    public String toString() {
      return (atom == null ? "<null>" : "[" + sphere + "." + id + " "
          + atom.toString() + (isDuplicate ? "*" : "")
          + "]"
      //+ (root ==  null ? "" : "/"+root.atom)
      );
    }

  }

}

