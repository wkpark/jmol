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
import org.jmol.viewer.Viewer;

/**
 * A relatively simple implementation of Cahn-Ingold-Prelog rules for assigning
 * R/S and E/S stereochemical labels. Based on IUPAC rules. (See text at the end of this document.)
 * 
 * References:
 * 
 * CIP(1966) R.S. Cahn, C. Ingold, V. Prelog, Specification of Molecular
 * Chirality, Angew.Chem. Internat. Edit. 5, 385ff
 * 
 * Mata(1986) Paulina Mata, Ana M. Lobo, Chris Marshall, and A. Peter Johnson,
 * Implementation of the Cahn-Ingold-Prelog System for Stereochemical Perception
 * in the LHASA Program, J. Chem. Inf. Comput. Sci. 1994, 34, 491-504 491
 * http://pubs.acs.org/doi/abs/10.1021/ci00019a004
 * 
 * Custer(1986) Roland H. Custer, Mathematical Statements About the Revised
 * CIP-System, MATCH, 21, 1986, 3-31
 * http://match.pmf.kg.ac.rs/electronic_versions/Match21/match21_3-31.pdf
 * 
 * Favre(2013) Henri A Favre, Warren H Powell, Nomenclature of Organic Chemistry : IUPAC
 * Recommendations and Preferred Names 2013 DOI:10.1039/9781849733069
 * http://pubs.rsc.org/en/content/ebook/9780854041824#!divbookcontent
 * 
 * IUPAC Project: Corrections, Revisions and Extension for the Nomenclature of
 * Organic Chemistry - IUPAC Recommendations and Preferred Names 2013 (the IUPAC
 * Blue Book)
 * https://iupac.org/projects/project-details/?project_nr=2015-052-1-800
 * 
 * 
 * Introduced in Jmol 14.12.0 Validated for Rules 1 and 2 in Jmol 14.13.2
 * E/Z added 14.14.1
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class CIPChirality {

  // Very rough Pseudocode:
  // 
  // individual "standard" carbon-based R/S stereochemistry (Rules 1-2)
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

  /**
   * Jmol viewer that created this CIPChirality object
   * 
   */
  Viewer vwr;
  
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
  int currentRule = 0;

  /**
   * Rule 1b hash table that maintains distance of the associated nonduplicated atom node
   *  
   */
  Map<String, Integer> htPathPoints;

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
    BS bsToDo = BSUtil.copy(bsAtoms);
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      Node atom = atoms[i];
      String c = atom.getCIPChirality(true);
      if (c.length() > 0) {
        bsToDo.clear(i);
      }
    }

    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      Node atom = atoms[i];
      Edge[] bonds = atom.getEdges();
      int index = atom.getIndex();
      for (int j = bonds.length; --j >= 0;) {
        Edge bond = bonds[j];
        if (bond.getCovalentOrder() == 2
            && bond.getOtherAtomNode(atom).getIndex() > index) {
          getBondChirality(bond);
        }
      }
    }

    for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1)) {
      Node a = atoms[i];
      if (a.getCovalentBondCount() != 4)
        continue;
      a.setCIPChirality(0);
      a.setCIPChirality(getAtomChiralityLimited(a, null, null, 5, -1));
    }
  }
  
  
 
  /**
   * Determine the Cahn-Ingold-Prelog R/S chirality of an atom
   * based only on Rules 1 and 2 -- that is, only the chirality 
   * that is self-determined, structural, and not relative-chirality dependent.
   * 
   * @param atom
   * @return [0:none, 1:R, 2:S]
   */
  public int getAtomChirality(Node atom) {
    return getAtomChiralityLimited(atom, null, null, 3, -1);
  }    
  
  /**
   * Determine the Cahn-Ingold-Prelog E/Z chirality of a bond
   * based only on Rules 1 and 2 -- that is, only the chirality 
   * that is self-determined, structural, and not relative-chirality dependent.
   * 
   * @param bond
   * @return [0:none, 1:Z, 2:E]
   */
  public int getBondChirality(Edge bond) {
    return getBondChiralityLimited(bond, 3);
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
      if (cipAtom.set()) {
        try {
          if (iref >= 0)
            cipAtom.bsPath.set(iref);
          for (currentRule = 0; currentRule <= ruleMax && !isChiral; currentRule++) {
            isChiral = false;
            Logger.info("-Rule " + currentRule + " CIPChirality for " + cipAtom
                + "-----");
            cipAtom.sortSubstituents();
            isChiral = true;
            if (Logger.debugging) {
              Logger.info(currentRule + ">>>>" + cipAtom);
              for (int i = 0; i < nSubs; i++)
                Logger.info(cipAtom.atoms[i] + " "
                    + Integer.toHexString(cipAtom.prevPriorities[i]));
            }
            for (int i = 0; i < nSubs - 1; i++) {
              if (cipAtom.prevPriorities[i] == cipAtom.prevPriorities[i + 1]) {
                isChiral = false;
                break;
              }
            }
          }
          if (isChiral) {
            rs = (!isAlkene ? checkHandedness(cipAtom)
                : cipAtom.atoms[0].isDuplicate ? 2 : 1);
          }
        } catch (Throwable e) {
          System.out.println(e + " in CIPChirality");
          if (!vwr.isJS)
            e.printStackTrace();
        }
        if (Logger.debugging)
          Logger.info(atom
              + " "
              + (isAlkene ? "" + rs : rs == STEREO_R ? "R"
                  : rs == STEREO_S ? "S" : ""));
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
    return (Measure.distanceToPlaneV(vNorm, d, p4) > 0 ? 1 : 2);
  }

  static boolean isCIS(CIPAtom me, CIPAtom parent, CIPAtom grandParent,
                             CIPAtom greatGrandParent) {
    V3 vNorm1 = new V3();
    V3 vTemp = new V3();
    Measure.getNormalThroughPoints((P3) me.atom, (P3) parent.atom, (P3) grandParent.atom, vNorm1, vTemp);
    V3 vNorm2 = new V3();
    Measure.getNormalThroughPoints((P3) parent.atom, (P3) grandParent.atom, (P3) greatGrandParent.atom, vNorm2, vTemp);
    return (vNorm1.dot(vNorm2) > 0); 
  }


  static CIPAtom getCommonAncestor(CIPAtom a, CIPAtom b) {
    if ((a.parent == null) != (b.parent == null))
      System.out.println("OHOH3");
    while ((a = a.parent) != (b = b.parent)){}
    return a;    
  }

  final static int[] PRIORITY_SHIFT = new int[] {24, 20, 12, 8, 4, 0};
  
  private class CIPAtom implements Comparable<CIPAtom>, Cloneable {


    /**
     * the associated Jmol (or otherwise). Use of Node allows us to implement this in SMILES or Jmol
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
    CIPAtom rootSubstituent;
    
    /**
     * Rule 1 element number
     */
    int elemNo;

    /**
     * Rule 2 nominal atomic mass; may be a rounded value so that 12C is the same as C 
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
     * Rule 1b measure: Distance from root of the corresponding nonduplicated atom.
     * 
     */
    
    int rootDistance;

    /**
     * number of substituent atoms (non-null atoms[] entries)
     */
    int nAtoms;

    /**
     * the number of distinct priorities determined for this atom for the current rule
     */
    private int nPriorities;

    /**
     * a count of how many 1H atoms we have found on this atom;
     * used to halt further processing of this atom 
     */
    private int h1Count;

    /**
     * already-determined auxiliary chirality (E/Z, R/S, etc) for this atom node
     */
    private int auxiliaryEZ = -1;
    
    /**
     * pre-determined absolute root chirality of the associated atom from previous determinations
     */
    private String knownAtomChirality = "";
    
    /**
     * pre-determined chirality along the path to this atom from the root atom node
     */
    private String knownChiralityPath = "";


    /**
     * a flag to prevent finalization of an atom node more than once
     * 
     */
    boolean isSet;

    /**
     * a flag to indicate atom that is a duplicate of another, either due to ring closure or multiple bonding
     * -- element number and mass, but no substituents; slightly
     * lower in priority than standard atoms.
     * 
     */
    boolean isDuplicate = true;
    
    /**
     * a flag to indicate an atom that has no substituents; a branch end point; typically H or a halogen (F, Cl, Br, I)
     * 
     */
    boolean isTerminal;

    /**
     * a flag used in Rule 3 to indicate the second carbon of a double bond
     */
    
    boolean isAlkeneAtom2;

    /**
     * the substituents -- up to four supported here at this time
     * 
     */
    CIPAtom[] atoms = new CIPAtom[4];
    
    /**
     * priorities associated with each subsituent from high (1) to low (4);
     * due to equivaliencies at a given rule level, these numbers may duplicate
     * and have gaps - for example, [1 1 3 4] 
     */
    int[] priorities = new int[4];

    /**
     * a number that encodes a substituent's priority prior to any comparisons by previous rules;
     * in the form abcde where a-e are digits 1-4, and a is for Rule 1, b is for Rule 2, etc.  
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
      knownAtomChirality = atom.getCIPChirality(false);
      if (parent != null) {
        this.knownChiralityPath = parent.knownAtomChirality + this.knownAtomChirality;
        sphere = parent.sphere + 1;
      }
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
      } else if (atom == root.atom) {
        // pointing to original atom (0 distance)
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
       
      Arrays.sort(atoms);
      return !isTerminal;
    }

    void fillNull(int pt) {
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
    CIPAtom addAtom(int i, Node other, boolean isDuplicate) {
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
      if (currentRule > 2)
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
          Logger.info(currentRule + ": " + this + "[" + i + "]=" + atoms[i] + " " + Integer.toHexString(prevPriorities[i]));
      }

      int[] indices = new int[4];

      for (int i = 0; i < n; i++)
        priorities[i] = 1;
      for (int i = 0; i < n; i++) {
        CIPAtom a = atoms[i];
        for (int j = i + 1; j < n; j++) {
          CIPAtom b = atoms[j];
          //if (Logger.debugging)
            //Logger.info("ordering " + this.id + "." + i + "."+ j + " " +  this + "-" + a + " vs " + b + " " + prevPriorities[i] + " " +  prevPriorities[j]);
          int score = (a.atom == null || b.atom == null || prevPriorities[i] == prevPriorities[j] ? a.compareTo(b) 
              :  prevPriorities[j] < prevPriorities[i] ? B_WINS : A_WINS);
          //if (Logger.debugging)
            //Logger.info("ordering " + this.id + "." + i + "."+ j + " "  + this + "-" + a + " vs " + b
              //  + " = " + score);
          switch (score) {
          case NA:
            System.out.println("OHNO");
            break;
          case B_WINS:
            indices[i]++;
            priorities[i]++;
    //        if (Logger.debugging)
      //        Logger.info(atom + "." + b + " B-beats " + a + " ind=" + indices[i]);
            break;
          case A_WINS:
            indices[j]++;
            priorities[j]++;
     //       if (Logger.debugging)
       //       Logger.info(atom + "." + a + " A-beats " + b + " ind=" + indices[j]);
            break;
          case TIED:
            switch (a.breakTie(b)) {
            case NA:
              System.out.println("OHNO2");
              break;
            case TIED:
              indices[i]++;
        //      if (Logger.debugging)
          //      Logger.info(atom + "." + b + " ends up with tie with " + a + " ind=" + indices[i]);
              break;
            case B_WINS:
              indices[i]++;
              priorities[i]++;
    //          if (Logger.debugging)
      //          Logger.info(atom + "." + b + " wins in tie with " + a  + " ind=" + indices[i]+ "\n");
              break;
            case A_WINS:
              indices[j]++;
              priorities[j]++;
//              if (Logger.debugging)
  //              Logger.info(atom + "." + a + " wins in tie with " + b + " ind=" + indices[j] + "\n");
              break;
            }
            break;
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
        newAtoms[pt] = atoms[i];
        Node atom = atoms[i].atom;
        int p = priorities[i];
        newPriorities[pt] = p;
        // we put atomic number and mass in specially so that we can recreate them for Rule 3
        switch (currentRule) {
        case 10:
          p = (atom == null ? 0 : 128 - atom.getElementNumber());
          break;
        case 12:
          p = (atom == null ? 0 : 128 - atom.getNominalMass());
          break;
        }
        newPrevPriorities[pt] = prevPriorities[i] | (p<<shift);
        if (atoms[i].atom != null)
          bs.set(priorities[i]);
      }
      atoms = newAtoms;
      priorities = newPriorities;
      prevPriorities = newPrevPriorities;
      nPriorities = bs.cardinality();
      
      if (Logger.debugging) {
        Logger.info(atom + " nPriorities = " + nPriorities);
        for (int i = 0; i < n; i++)
          Logger.info(atom + "[" + i + "]=" + atoms[i] + " " + priorities[i]);
      }
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
     * Break a tie at any level in the iteration between to atoms that otherwise
     * are the same by sorting their substituents.
     * 
     * @param b
     * @return 1 to indicate this is the winner, -1 to indicate b is the winner; 0
     *         for a tie
     */
    int breakTie(CIPAtom b) {

      // Do a duplicate check -- if that is not a TIE we do not have to go any further.
      
      int score = checkDuplicate(b);
      if (score != TIED)
        return score;
      
      // return NO_CHIRALITY/TIED if:
      //  a) both refer to the same atom
      //  b) one or the other can't be set (because it has too many connections)
      //  c) or one or the other is terminal (has no substituents) 
      //  d) or they are the same atom
      if (atom == b.atom && (atom == null || currentRule < 3))
        return TIED;
      if ((atom == null) != (b.atom == null))
        return (atom == null ? B_WINS : A_WINS);
      if (!set() || !b.set() 
          || isTerminal || b.isTerminal
          || isDuplicate && b.isDuplicate)
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
          Logger.info("compareAB " + ai.parent + "-" + ai + " with " + bi.parent
              + "-" + bi + " goDeep=" + goDeep);
        int score = (goDeep ? ai.breakTie(bi) : ai.checkCurrentRule(bi));
        if (score == NA)
          score = TIED;
        if (score != TIED) {
          if (Logger.debugging)
            Logger.info("compareAB " + (score == B_WINS ? bi + " beats " + ai : ai
                + " beats " + bi)
                + " " + goDeep);
          return score;
        }
      }
      if (Logger.debugging)
        Logger.info("compareAB ends in tie for " + this + " vs. " + b);
      return TIED;
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
      // null should never win
      if ((atom == null) != (b.atom == null))
        return (atom == null ? B_WINS : A_WINS);
      // fullPriority is cumulative over previous Rule applications
      // 
      //if (b.fullPriority != fullPriority)
      //  return (b.fullPriority < fullPriority ? B_WINS : A_WINS);
      return  (score = checkCurrentRule(b)) == NA ? 
          TIED : 
            score != TIED ? score : checkDuplicate(b); 
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
      case 0:
        return checkRule1a(b); 
      case 1:
        return checkRule1b(b); 
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
     * Looking for root distance -- duplicate atoms only.
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
          || isDuplicate || b.isDuplicate ? NA: 
            parent == b.parent ? breakTie(b) :  
                (za = parent.getZaux()) < (zb = b.parent.getZaux()) 
            ? A_WINS  : za > zb ? B_WINS : TIED;
    }
 
    /**
     * Check auxiliary Z by temporarily setting return path.
     * 
     * This method uses CIPAtom.clone() effectively to create a second independent
     * path that is checked without messing up the currently expanding polygraph. 
     * 
     * Note that one path from the current atom is in reverse - the path back to the 
     * root atom. This must be reconstructed, because until this point we have not
     * carried out many of the necessary comparisons. 
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
    
    Lst<CIPAtom> getReturnPath(CIPAtom a) {
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
        last = last.parent;
        thisAtom = p;
      }
    }

    /**
     * Swap a substituent and the parent in preparation for reverse traversal of this path
     * 
     * @param newParent
     * @param newSub
     */
    private void replaceParentSubstituent(CIPAtom newParent, CIPAtom newSub) {
      for (int i = 0; i < 4; i++)
        if (atoms[i] == newParent) {
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
     * Chapter 9 Rule 4: like vs. unlike
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRule4(CIPAtom b) {
      if (Logger.debugging)
        Logger.info("Checking Rule 4 for " + this + " and " + b);
      int l = getCommonAncestor(this, b).knownChiralityPath.length();
      if (l >= knownChiralityPath.length())
        return TIED; // some H atoms
      String ac = knownChiralityPath.substring(l);
      String bc = b.knownChiralityPath.substring(l);
      if (ac != null && ac.length() == bc.length() && !ac.equals(bc)) {
        int n = ac.length();
        for (int i = 0; i < n - 1; i++)
          for (int j = i + 1; j < n; j++) {
            boolean alike = (ac.charAt(i) == ac.charAt(j));
            boolean blike = (bc.charAt(i) == bc.charAt(j));
            if (alike != blike)
              return (alike ? A_WINS : B_WINS);
          }
      }
      return TIED;
    }

    /**
     * Chapter 9 Rule 5. Implemented only for R and Z.
     * 
     * For 3D models is it not necessary to consider "unspecified" chirality, as that is only a 2D concept.
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRule5(CIPAtom b) {
      boolean isRZ = PT.isOneOf(knownAtomChirality, ";RZ;");
      return (isRZ == PT.isOneOf(b.knownAtomChirality, ";RZ;")? TIED : isRZ ? A_WINS
          : B_WINS);
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
        if (atoms[i] != null) {
          a.atoms[i] = atoms[i];
          a.prevPriorities[i] = getBasePriority(atoms[i]);
        }
      }
      if (Logger.debugging)
        Logger.info("cloning " + this + " as " + a);
      return a;

    }

    private int getBasePriority(CIPAtom atom) {
      Node a = atom.atom;
      int code = 0;
      if (a == null) {
        code = 0x7FFFF << PRIORITY_SHIFT[2];
      } else {
        code = ((128 - a.getElementNumber()) << PRIORITY_SHIFT[0])
            | ((128 - a.getNominalMass()) << PRIORITY_SHIFT[2]);
      }      
      return code;
    }

    @Override
    public String toString() {
      return (atom == null ? "<null>" : "[" + sphere + "." + id + " " + atom.toString()
          + (isDuplicate ? "*" : "")
          //+ (root ==  null ? "" : "/"+root.atom)
          ) + "]";
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

//status: Rule 3 implemented




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
