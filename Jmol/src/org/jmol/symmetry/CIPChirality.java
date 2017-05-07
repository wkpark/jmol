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
import org.jmol.util.Edge;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.viewer.JC;

/**
 * A relatively efficient implementation of Cahn-Ingold-Prelog rules for assigning
 * R/S and E/S stereochemical labels. Based on IUPAC rules of 2013. 
 * 
 * Many thanks to the members of the BlueObelisk-Discuss group, particularly
 * Mikko Vainio, JOHN MAYFIELD, Wolf Ihlenfeldt, and Egon Willighagen, for the excellent
 * testing suite (https://sourceforge.net/p/jmol/code/HEAD/tree/trunk/Jmol-datafiles/cip),
 * encouragement, and extremely helpful advice. 
 * 
 * Additional thanks to the IUPAC Blue Book Revision project, specifically 
 * Karl-Heinz Hellwich for alerting me to the errata page for the 2013 IUPAC specs
 * (http://www.chem.qmul.ac.uk/iupac/bibliog/BBerrors.html)     
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
 * 
 * 4/6/17 Introduced in Jmol 14.12.0; 
 * validated for Rules 1 and 2 in Jmol 14.13.2; 
 * E/Z added 14.14.1; 
 * 4/27/17 Ruled 3-5 completed 14.15.1
 * 4/28/17 Validated for 146 compounds, including imines and diazines, sulfur, phosphorus
 * 4/29/17 validated for 160 compounds, including M/P, m/p (axial chirality for biaryls and odd-number cumulenes)  
 * 5/02/17 validated for 161 compounds, including M/P, m/p (axial chirality for biaryls and odd-number cumulenes)  
 * 
 * TODO: mix seqCis and seqTrans with R/S
 * TODO: mix allene M/P with R/S
 * 
 * 
 * Added logic to Rule 1b: The root distance for an aromatic duplicate atom is that of its parent, not its duplicated atom.
 * 
 *  Rationale: Giving the distance of the duplicated atom introduces a Kekule bias, which is not acceptable.
 * 
 * validation suite: see https://sourceforge.net/p/jmol/code/HEAD/tree/trunk/Jmol-datafiles/cip/
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class CIPChirality {

  // Very rough Pseudocode:
  // 
  // individual "standard" carbon-based R/S and E/Z stereochemistry (Rules 1, 2, and 3)
  //
  //  getChirality(molecule) {
  //    checkForAlkenes()
  //    for(all atoms) getAtomChirality(applyRules1-3)
  //    if (haveAlkenes) {
  //      checkForSmallRings()
  //      for(all double bonds) getBondChirality(applyRules1-3)
  //    }
  //    for(all atoms still without designations) getAtomChirality(applyRules1-5)
  //    if (haveAlkenes) {
  //      for(all double bonds still without designations) getBondChirality(applyRules1-5)
  //      removeUnnecessaryEZDesignations()
  //    }
  //  }
  //
  // getChirality(atom) {
  //  for (each Rule){  
  //    sortSubstituents() 
  //    if (done) exit checkHandedness();
  //
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
  //    bestScore = Integer.MAX_VALUE
  //    for (each substituent pairing i in a and b) {
  //      bestScore = min(bestScore, breakTie(a_i, b_i)
  //    }
  //    return bestScore
  // }
  //
  // 

 

  static final int NO_CHIRALITY = 0;
  static final int TIED = NO_CHIRALITY;
  static final int B_WINS = 1;
  static final int A_WINS = -1;
  static final int DIASTEREOMERIC = Integer.MAX_VALUE;
  static final int IGNORE = Integer.MIN_VALUE;
  static final int NOT_RELEVANT = Integer.MIN_VALUE;
  static final int STEREO_SAME = Integer.MAX_VALUE;

  static final int STEREO_UNDETERMINED = -1;

  static final int STEREO_RS = -1;
  static final int STEREO_EZ = -2;
  static final int STEREO_ALLENE = -3;

  static final int STEREO_R = JC.CIP_CHIRALITY_R_FLAG;
  static final int STEREO_S = JC.CIP_CHIRALITY_S_FLAG;

  static final int STEREO_M = JC.CIP_CHIRALITY_M_FLAG;
  static final int STEREO_P = JC.CIP_CHIRALITY_P_FLAG;
  
  static final int STEREO_Z = JC.CIP_CHIRALITY_Z_FLAG;
  static final int STEREO_E = JC.CIP_CHIRALITY_E_FLAG;

  static final int STEREO_BOTH_RS = STEREO_R | STEREO_S;
  static final int STEREO_BOTH_EZ = STEREO_Z | STEREO_E;

  static final int RULE_1a = 1;
  static final int RULE_1b = 2;
  static final int RULE_2 = 3;
  static final int RULE_3 = 4;
  static final int RULE_4 = 5;
  static final int RULE_5 = 6;
  
  public String getRuleName() {
    switch (currentRule) {
    case RULE_1a:
      return "1a";
    case RULE_1b:
      return "1b";
    default:
      return "" + (currentRule - 1);
    }
  }

  final static int PRIORITY_12_MASK = 0x7FFFF000;
  final static int PRIORITY_1b_MASK = 0x00F00000;
  //     3         2         1
  //   210987654321098765432109876543210 
  //     -1a----                             0x7F << 24
  //            -1b-                          0xF << 20
  //                -2------                 0xFF << 12 
  //                        -3-               0x7 << 9 
  //                           -4-            0x7 << 6 
  //                              -5-         0x7 << 3 
  
  final static int[] PRIORITY_SHIFT = new int[] { -1, 24, 20, 12, 9, 6, 3, 0 };

  static final float TRIGONALITY_MIN = 0.2f;
  
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
   * track small rings for removing E/Z indicators as per IUPAC 2013.P-93.5.1.4.1
   */
  Lst<BS> lstSmallRings = new Lst<BS>();
  
  /**
   * max priorities across an entire molecule, for cyclic loops
   */
  int nPriorityMax;
  
  /**
   * don't do this atom again (atropisomer)
   * 
   */
  BS bsAtropisomeric;
  
  /**
   * Needed for Bob's Rule 1b addition
   * 
   */
  BS bsAromatic;
  
  V3 vNorm = new V3();
  V3 vNorm2 = new V3();
  V3 vTemp = new V3();

  public CIPChirality() {
    // for reflection 
  }

  /**
   * Initialize for a new molecular determination.
   * 
   */
  private void init() {
    ptID = 0;
    nPriorityMax = 0;
    lstSmallRings.clear();
  }

  /**
   * A more general determination of chirality that involves ultimately all
   * Rules 1-5.
   * 
   * @param atoms atoms to process 
   * @param bsAtoms bit set of all atoms to process
   * @param bsAromatic set of all aromatic atoms
   * @param bsAtropisomeric bit set of all biphenyl-like connections
   */
  public void getChiralityForAtoms(Node[] atoms, BS bsAtoms, BS bsAromatic, BS bsAtropisomeric) {
    if (bsAtoms.isEmpty())
      return;
    this.bsAromatic = (bsAromatic.cardinality() == 0 ? null : bsAromatic);
    this.bsAtropisomeric = bsAtropisomeric;
    init();
    BS bsToDo = BSUtil.copy(bsAtoms);
    boolean haveAlkenes = preFilterAtomList(atoms, bsToDo);

    if (haveAlkenes) {
      // using BSAtoms here because we need the entire graph, even starting with an H atom. 
      getSmallRings(atoms[bsAtoms.nextSetBit(0)]);
    }

    // Initially only Rules 1-3

    for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1)) {
      Node atom = atoms[i];
      // This call checks to be sure we do not already have it.
      // Jmol will clear this anyway prior to CALCULATE CHIRALITY, so this is mute.
      // But it is here in case we want to implement full molecular chirality generation
      // prior to any request for atom.chirality. Right now we don't do that.
      String c = atom.getCIPChirality(false);
      if (c.length() > 0) {
        bsToDo.clear(i);
      } else {
        ptID = 0;
        atom.setCIPChirality(getAtomChiralityLimited(atom, null, null, RULE_3));
      }
    }

    // bond chiralities E/Z and M/P only through Rule 3 

    Lst<int[]> lstEZ = new Lst<int[]>();
    if (haveAlkenes) {
      for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1))
        getAtomBondChirality(atoms[i], false, RULE_3, lstEZ, bsToDo);
    }

    // On to Rules 4 and 5 for atoms that still could be chiral but don't have a designation.
    
    // Interesting question as to whether we need to do this in two stages, but 
    // for now that is what we are doing. The rules require that we can do this, 
    // because no later rule can reverse the result of a previous rule, and so
    // if we do Rules 1-3 for all atoms and then Rules 1-5 for all atoms, that is 
    // at worst inefficient. 
    
    for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1)) {
      Node a = atoms[i];
      a.setCIPChirality(0);
      a.setCIPChirality(getAtomChiralityLimited(a, null, null, RULE_5));
    }

    // 
    if (haveAlkenes) {
      for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1))
        getAtomBondChirality(atoms[i], false, RULE_5, lstEZ, bsToDo);
      // Finally, remove any E/Z indications in small rings
      if (lstSmallRings.size() > 0 && lstEZ.size() > 0)
        clearSmallRingEZ(atoms, lstEZ);
    }
   
  }

  /**
   * remove unnecessary atoms from the list and let us know if we have alkenes to consider
   * @param atoms
   * @param bsToDo
   * @return whether we have any alkenes that could be EZ
   */
  private boolean preFilterAtomList(Node[] atoms, BS bsToDo) {
    boolean haveAlkenes = false;
    for (int i = bsToDo.nextSetBit(0); i >= 0; i = bsToDo.nextSetBit(i + 1)) {
      if (!couldBeChiralAtom(atoms[i])) {
        bsToDo.clear(i);
        continue;
      }
      if (!haveAlkenes && couldBeChiralAlkene(atoms[i], null) != STEREO_UNDETERMINED)
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
  private boolean couldBeChiralAtom(Node a) {
    boolean mustBePlanar = false;
    switch (a.getCovalentBondCount()) {
    default:
      System.out.println("???? too many bonds! " + a);
      return  false;
    case 0:
      return false;
    case 1:
      return false;
    case 2:
      return a.getElementNumber() == 7; // could be diazine or imine
    case 3:
      switch (a.getElementNumber()) {
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
    // check that the atom has at most one 1H atom
    Edge[] edges = a.getEdges();
    int nH = 0;
    for (int j = a.getBondCount(); --j >= 0;) {
      if (edges[j].getOtherAtomNode(a).getAtomicAndIsotopeNumber() == 1
          && ++nH == 2) {
        return false;
      }
    }
    float d = getTrigonality(a, vNorm);
    boolean planar = (Math.abs(d) < TRIGONALITY_MIN);
    if (planar == mustBePlanar)
      return  true;
    System.out.println("??????? planar=" + planar + "??" + a);
    return false; 
  }

  /**
   * Allow double bonds only if trivalent and first-row atom. (IUPAC
   * 2013.P-93.2.4) Currently: a) first row b) doubly bonded c) doubly bonded
   * atom is also first row
   * 
   * @param a
   * @param b optional other atom
   * @return if the atom could be an EZ node
   */
  private int couldBeChiralAlkene(Node a, Node b) {
    switch (a.getCovalentBondCount()) {
    default:
      return STEREO_UNDETERMINED;
    case 2:
      // imines and diazines
      if (a.getElementNumber  () != 7) // nitrogen
        return STEREO_UNDETERMINED;
      break;
    case 3:
      // first-row only (IUPAC 2013.P-93.2.4)
      if (!isFirstRow(a))
        return STEREO_UNDETERMINED;
      break;
    }
    Edge[] bonds = a.getEdges();
    int n = 0;
    for (int i = bonds.length; --i >= 0;)
      if (bonds[i].getCovalentOrder() == 2) {
        if (++n > 1)
          return STEREO_ALLENE; //central allenes
        Node other = bonds[i].getOtherAtomNode(a);
        if (!isFirstRow(other))
          return STEREO_UNDETERMINED;
        if (b != null && (other != b || b.getCovalentBondCount() == 1)) {
          // could be allene central, but I think this is not necessary
          return STEREO_UNDETERMINED;
        }
      }
    return STEREO_EZ;
  }

  /**
   * Check if an atom is 1st row
   * @param a
   * @return elemno > 2 && elemno <= 10
   */
  boolean isFirstRow(Node a) {
    int n = a.getElementNumber();
    return (n > 2 && n <= 10);
  }

  /**
   * Run through a minimal graph to find and catalog all rings.
   *  
   * @param atom
   */
  private void getSmallRings(Node atom) {
    lstSmallRings = new Lst<BS>(); 
    root = new CIPAtom().create(atom, null, false, false);
    addSmallRings(root);
  }

  private void addSmallRings(CIPAtom a) {
    if (a == null || a.isTerminal || a.isDuplicate || a.atom == null || a.atom.getCovalentBondCount() > 4)
      return;
    Edge[] bonds = a.atom.getEdges();
    Node atom2;
    int pt = 0;
    for (int i = bonds.length; --i >= 0;) {
      Edge bond = bonds[i]; 
      if (bond == null || !bond.isCovalent()
          || (atom2 = bond.getOtherAtomNode(a.atom)).getElementNumber() == 1
          || a.parent != null && atom2 == a.parent.atom
          )
        continue;
      a.addAtom(pt++, atom2, false, false);
    }
    for (int i = 0; i < pt; i++)
      addSmallRings(a.atoms[i]);
  }

  /**
   * Remove E/Z designations for small-rings double bonds (IUPAC 2013.P-93.5.1.4.1).
   * 
   * @param atoms
   * @param lstEZ
   */
  private void clearSmallRingEZ(Node[] atoms, Lst<int[]> lstEZ) {
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
   * Determine the trigonality of an atom in order to determine whether it might have a lone pair.
   * The global vector vNorm is returned as well, pointing from the atom to the base plane of its first three substituents.
   * 
   * @param a
   * @param vNorm  a vector returned with the normal from the atom to the base plane 
   * @return height from plane of first three covalently bonded nodes to this node
   */
  float getTrigonality(Node a, V3 vNorm) {
    P3[] pts = new P3[3];
    Edge[] bonds = a.getEdges();
    for (int i = bonds.length, pt = 0; --i >= 0 && pt < 3;)
      if (bonds[i].isCovalent())
        pts[pt++] = bonds[i].getOtherAtomNode(a).getXYZ();
    P4 plane = Measure.getPlaneThroughPoints(pts[0], pts[1], pts[2], vNorm, vTemp, new P4());
    return Measure.distanceToPlane(plane, a.getXYZ());
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
    return getAtomChiralityLimited(atom, null, null, RULE_3);
  }

  /**
   * Determine the Cahn-Ingold-Prelog E/Z chirality of a bond based only on
   * Rules 1-3 -- that is, only the chirality that is self-determined,
   * structural, and not relative-chirality dependent.
   * @param bond 
   * 
   * @return [0:none, 1:Z, 2:E]
   */
  public int getBondChirality(Edge bond) {
    init();
    getSmallRings(bond.getOtherAtomNode(null));
    return (bond.getCovalentOrder() == 2 ? getBondChiralityLimited(bond, null, RULE_3) : NO_CHIRALITY);
  }

  /**
   * Get E/Z characteristics for specific atoms. Also check here for atropisomeric M/P designations
   * 
   * @param atom
   * @param allBonds
   *        true to do all bonds to these atoms from other atoms, not just among
   *        them
   * @param ruleMax
   * @param lstEZ
   * @param bsToDo
   */

  private void getAtomBondChirality(Node atom, boolean allBonds, int ruleMax,
                                    Lst<int[]> lstEZ, BS bsToDo) {
    int index = atom.getIndex();
    Edge[] bonds = atom.getEdges();
    int c = NO_CHIRALITY;
    boolean isAtropic = bsAtropisomeric.get(index); 
    for (int j = bonds.length; --j >= 0;) {
      Edge bond = bonds[j];
      Node atom1;
      int index1;
      if (isAtropic) {
        atom1 = bonds[j].getOtherAtomNode(atom);
        index1 = atom1.getIndex();
        if (!bsAtropisomeric.get(index1))
          continue;
        c = getAxialOrEZChirality(atom, atom1, atom, atom1, true, ruleMax);
      } else if (bond.getCovalentOrder() == 2) {
        atom1 = getLastCumuleneAtom(bond, atom, null, null);
        index1 = atom1.getIndex();
        if (!allBonds && index1 < index)
          continue;
        c = getBondChiralityLimited(bond, atom, ruleMax);
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
   *        return of number of sp2 carbons in this alkene or cumulene
   * @param parents
   * @return the terminal atom of this alkene or cumulene
   */
  private Node getLastCumuleneAtom(Edge bond, Node atom, int[] nSP2,
                                   Node[] parents) {
    // we know this is a double bond
    Node atom2 = bond.getOtherAtomNode(atom);
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
      Edge[] edges = atom2.getEdges();
      for (int i = edges.length; --i >= 0;) {
        Node atom3 = (bond = edges[i]).getOtherAtomNode(atom2);
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
   * @return if parent != null: [0:none, 1:R, 2:S] otherwise [0:none, 1:
   *         atoms[0] is higher, 2: atoms[1] is higher]
   */
  private int getAtomChiralityLimited(Node atom, CIPAtom cipAtom,
                                      CIPAtom parent, int ruleMax) {
    int rs = NO_CHIRALITY;
    boolean isChiral = false;
    boolean isAlkene = false;
    try {
      if (cipAtom == null) {
        cipAtom = new CIPAtom().create(atom, null, false, isAlkene);
        int nSubs = atom.getCovalentBondCount();
        int elemNo = atom.getElementNumber();
        isAlkene = (nSubs == 3 && elemNo <= 10); // (IUPAC 2013.P-93.2.4)
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
      if (cipAtom.set()) {
        for (currentRule = RULE_1a; currentRule <= ruleMax; currentRule++) {
          if (Logger.debugging)
            Logger.info("-Rule " + getRuleName() + " CIPChirality for " + cipAtom + "-----");
          if (currentRule == RULE_4) {
            //cipAtom.resetAuxiliaryChirality();// was resetting E/Z
            cipAtom.createAuxiliaryRSCenters(null, null);
          }
          
          isChiral = false;
          cipAtom.sortSubstituents();
          isChiral = true;
          if (Logger.debugging) {
            Logger.info(currentRule + ">>>>" + cipAtom);
            for (int i = 0; i < cipAtom.bondCount; i++) { // Logger
              if (cipAtom.atoms[i] != null) // Logger
                Logger.info(cipAtom.atoms[i] + " " + Integer.toHexString(cipAtom.prevPriorities[i]));
            }
          }
          if (cipAtom.achiral) {
            isChiral = false;
            break;
          }
          for (int i = 0; i < cipAtom.bondCount - 1; i++) {
            if (cipAtom.prevPriorities[i] == cipAtom.prevPriorities[i + 1]) {
              isChiral = false;
              break;
            }
          }
          if (currentRule == RULE_5)
            cipAtom.isPseudo = cipAtom.canBePseudo;
          if (isChiral) {
            rs = (!isAlkene ? cipAtom.checkHandedness()
                : cipAtom.atoms[0].isDuplicate ? STEREO_S : STEREO_R);
            if (!isAlkene && cipAtom.isPseudo && cipAtom.canBePseudo)
              rs = rs | JC.CIP_CHIRALITY_PSEUDO_FLAG;
            break;
          }
        }
        if (Logger.debugging)
          Logger.info(atom + " " + rs + "\n----------------------------------");
      }
    } catch (Throwable e) {
      System.out.println(e + " in CIPChirality");
      /**
       * @j2sNative
       *  alert(e);
       */
      {
         e.printStackTrace();
       }
      return STEREO_BOTH_RS;
    }
    return rs;
  }

  /**
   * Determine the axial or E/Z chirality for this bond, with the given starting atom a 
   * 
   * @param bond
   * @param a first atom to consider, or null
   * @param ruleMax
   * @return one of: {NO_CHIRALITY | STEREO_Z | STEREO_E | STEREO_Ra | STEREO_Sa | STEREO_ra | STEREO_sa}
   */
  private int getBondChiralityLimited(Edge bond, Node a, int ruleMax) {
    if (Logger.debugging)
      Logger.info("get Bond Chirality " + bond);
    if (a == null)
      a = bond.getOtherAtomNode(null);
    Node b = bond.getOtherAtomNode(a);
    if (couldBeChiralAlkene(a, b) == STEREO_UNDETERMINED)
      return NO_CHIRALITY;
    int[] nSP2 = new int[1];
    Node[] parents = new Node[2];
    b = getLastCumuleneAtom(bond, a, nSP2, parents);
    boolean isCumulene = (nSP2[0] > 2);
    boolean isAxial = isCumulene && (nSP2[0] % 2 == 1);
    return getAxialOrEZChirality(a, parents[0], parents[1], b, isAxial, ruleMax);
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
   * @return one of: {NO_CHIRALITY | STEREO_Z | STEREO_E | STEREO_Ra | STEREO_Sa
   *         | STEREO_ra | STEREO_sa}
   */
  private int getAxialOrEZChirality(Node a, Node pa, Node pb, Node b, boolean isAxial, int ruleMax) {
    CIPAtom a1 = new CIPAtom().create(a, null, false, true);
    CIPAtom b1 = new CIPAtom().create(pa, null, false, true);
    a1.canBePseudo = a1.isOddCumulene = isAxial;
    int atop = getAtomChiralityLimited(a, a1, b1, ruleMax) - 1;
    CIPAtom a2 = new CIPAtom().create(pb, null, false, true);
    CIPAtom b2 = new CIPAtom().create(b, null, false, true);
    b2.canBePseudo = b2.isOddCumulene = isAxial;
    int btop = getAtomChiralityLimited(b, b2, a2, ruleMax) - 1;
    int c = NO_CHIRALITY;
    if (atop >= 0 && btop >= 0) {
      if (isAxial) {
        c = (isPos(b2.atoms[btop], b2, a1, a1.atoms[atop]) ? STEREO_P : STEREO_M);
        if ((a1.ties == null) != (b2.ties == null))
          c |= JC.CIP_CHIRALITY_PSEUDO_FLAG;
      } else {
        c = (isCis(b2.atoms[btop], b2, a1, a1.atoms[atop]) ? STEREO_Z : STEREO_E);
      }
    }
    if (c != NO_CHIRALITY && (isAxial || !isAtropisomeric(a) && !isAtropisomeric(b))) {
      a.setCIPChirality(c);
      b.setCIPChirality(c);
      if (Logger.debugging)
        Logger.info(a + "-" + b + " " + JC.getCIPChiralityName(c));
    }
    return c;
  }

  private boolean isAtropisomeric(Node a) {
    return bsAtropisomeric != null && bsAtropisomeric.get(a.getIndex());
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
    float angle = Measure.computeTorsion(a.atom.getXYZ(), b.atom.getXYZ(), c.atom.getXYZ(),
        d.atom.getXYZ(), true);
    return (angle > 0);
  }

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
    int elemNo;

    /**
     * Rule 2 nominal atomic mass; may be a rounded value so that 12C is the
     * same as C
     */
    int massNo;

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
     * pre-determined absolute root chirality of the associated atom from
     * previous determinations
     */
    private String knownAtomChirality = "~";

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
     * Force achiral condition due to two identical ligands after Rule 4 check.
     */
    boolean achiral;

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
     * priorities associated with each subsituent from high (0) to low (3); due
     * to equivaliencies at a given rule level, these numbers may duplicate and
     * have gaps - for example, [0 2 0 3]
     */
    private int[] priorities = new int[4];

    /**
     * a number that encodes a substituent's priority prior to any comparisons
     * by previous rules; see PRIORITY_SHIFT.
     * 
     */
    int[] prevPriorities = new int[] {-1, -1, -1, -1};
    /**
     * a list that tracks stereochemical paths for Mata analysis
     * 
     */
    private String[] rule4List;

    /**
     * position of the lone pair for winding check
     * 
     */
    private P3 lonePair;

    /**
     * the application-assigned unique atom index for this atom; used in updating lstSmallRings
     * 
     */
    private int atomIndex;

    /**
     * already-determined auxiliary chirality (E/Z, R/S, etc) for this atom node;
     * this value must be cleared after Rule 3 if continuing
     */
    private int auxEZ = STEREO_UNDETERMINED;

    /**
     * the cloned reversed path back to Sphere 1 for alkenes and stereogenic atoms
     * 
     */
    private CIPAtom auxParentReversed;

    /**
     * the cloned non-tied set of atoms for determining if an enantiomorphic tie is rs or not
     * 
     */
    private CIPAtom auxPseudo;

    boolean canBePseudo = true;

    /**
     * used to determine pseudochirality
     */
    Lst<int[]> ties;

    /**
     * used to defer pseudochirality check
     */
    boolean isOddCumulene;

    /**
     * first =X= atom in a string of =X=X=X=...
     */
    private CIPAtom nextSP2;

    private CIPAtom nextChiralBranch;

    private int[] rule4Count;

    private int priority;

    /**
     * Rule 1b hash table that maintains distance of the associated nonduplicated
     * atom node
     * 
     */
    Map<String, Integer> htPathPoints;
    


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
    CIPAtom create(Node atom, CIPAtom parent, boolean isDuplicate,
                   boolean isAlkene) {
      this.id = ++ptID;
      this.parent = parent;
      if (atom == null)
        return this;
      this.isAlkene = isAlkene;
      this.atom = atom;
      this.elemNo = atom.getElementNumber();
      this.massNo = atom.getNominalMass();
      atomIndex = atom.getIndex();
      bondCount = atom.getCovalentBondCount();
      if (bondCount == 3 && !isAlkene && elemNo > 10)
        getLonePair();
      canBePseudo = (bondCount == 4 || bondCount == 3 && lonePair != null);
      String c = atom.getCIPChirality(false);
      // What we are doing here is creating a lexigraphically sortable string
      // R < S < r < s < ~ and C < T < ~ 
      // we ignore r and s here. 
      if (c.equals("") || c.equals("r") || c.equals("s"))
        c = "~"; // none
      knownAtomChirality = c;
      if (parent != null)
        sphere = parent.sphere + 1;
      if (sphere == 1) {
        rootSubstituent = this;
        htPathPoints = new Hashtable<String, Integer>();
      } else if (parent != null) {
        rootSubstituent = parent.rootSubstituent;
        htPathPoints = rootSubstituent.htPathPoints;
      }
      this.bsPath = (parent == null ? new BS() : BSUtil.copy(parent.bsPath));

      boolean wasDuplicate = isDuplicate;
      // The rootDistance for a nonDuplicate atom is just its sphere.
      // The rootDistance for a duplicate atom is (by IUPAC) the sphere of its duplicated atom.
      // I argue that for aromatic compounds, this introduces a Kekule problem and that for
      // those cases, the rootDistance should be the sphere of the parent, not the duplicated atom.
      // This shows up in AV-360#215. 
      
      if (parent == null) {
        // original atom
        bsPath.set(atomIndex);
        rootDistance = 0;
      } else if (atom == root.atom) {
        // pointing to original atom
        rootDistance = 0;
        isDuplicate = true;
      } else if (wasDuplicate && bsAromatic != null && bsAromatic.get(atomIndex)) {
          rootDistance = parent.rootDistance;
      } else if (bsPath.get(atomIndex)) {
        isDuplicate = true;
        rootDistance = rootSubstituent.htPathPoints.get(atom.toString())
            .intValue();
      } else {
        bsPath.set(atomIndex);
        rootDistance = parent.rootDistance + 1;
        rootSubstituent.htPathPoints.put(atom.toString(), new Integer(
            rootDistance));
      }
      this.isDuplicate = isDuplicate;
      myPath = (parent != null ? parent.myPath + "-" : "") + this;

            if (Logger.debugging)
              Logger.info("new CIPAtom " + parent + "->" + myPath);
      if (isDuplicate && !wasDuplicate)
        updateRingList();
            
      return this;
    }

    private void getLonePair() {      
      float d = getTrigonality(atom, vNorm);
      if (Math.abs(d) > TRIGONALITY_MIN) {
        lonePair = new P3();
        vNorm.scale(d);
        lonePair.add2(atom.getXYZ(), vNorm); 
      }
    }

    /**
     * Create a bit set that gives all the atoms in this ring if it is smaller
     * than 8.
     * 
     */
    private void updateRingList() {
      BS bsRing = BSUtil.newAndSetBit(atomIndex);
      CIPAtom p = this;
      int index = -1;
      while ((p = p.parent) != null && index != atomIndex) {
        bsRing.set(index = p.atomIndex);
      }
      if (bsRing.cardinality() < 8) {
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
    boolean set() {
      if (isSet)
        return true;
      isSet = true;
      if (isDuplicate)
        return true;
      
      int nBonds = atom.getBondCount();
      Edge[] bonds = atom.getEdges();
      if (Logger.debuggingHigh)
        Logger.info("set " + this);
      int pt = 0;
      for (int i = 0; i < nBonds; i++) {
        Edge bond = bonds[i];
        if (!bond.isCovalent())
          continue;
        Node other = bond.getOtherAtomNode(atom);
        boolean isParentBond = (parent != null && parent.atom == other);
        int order = bond.getCovalentOrder();
        if (order == 2) {
          if (elemNo > 10 || !isFirstRow(other))
            order = 1;
          else {
            isAlkene = true;
            if (isParentBond) {
              knownAtomChirality = bond.getCIPChirality(false);
              if (atom.getCovalentBondCount() == 2 && atom.getValence() == 4) {
                parent.isAlkeneAtom2 = false;
              } else {
                isAlkeneAtom2 = true;
              }
              parent.alkeneChild = null;
              alkeneParent = (parent.alkeneParent == null ? parent : parent.alkeneParent);
              alkeneParent.alkeneChild = this;
              if (parent.alkeneParent == null)
                parent.nextSP2 = this;
            }
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

      // Do an initial atom-only shallow sort using a.compareTo(b)

      int ruleNow = currentRule;
      currentRule = RULE_1a;
      Arrays.sort(atoms);
      currentRule = ruleNow;
      // pretty sure this next test cannot be true
      if (isTerminal)
        System.out.println("????");
      return !isTerminal;
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
    CIPAtom addAtom(int i, Node other, boolean isDuplicate, boolean isAlkene) {
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
            if (Logger.debuggingHigh)
              Logger.info(" second H atom found on " + atom);
            return null;
          }
        }
      }
      atoms[i] = new CIPAtom().create(other, this, isDuplicate, isAlkene);
      if (currentRule > RULE_2) {
        prevPriorities[i] = atoms[i].getBasePriority(true);
      }
      return atoms[i];
    }

    /**
     * Deep-Sort the substituents of an atom. Don't allow ties if this is the
     * first atom (parent == null).
     * 
     */
    void sortSubstituents() {

      int[] indices = new int[4];

      ties = null;

      for (int i = 0; i < 4; i++) {
        priorities[i] = 0;
        if (prevPriorities[i] == -1 && currentRule > RULE_2) {
          prevPriorities[i] = atoms[i].getBasePriority(true);
        }

      }
      
      if (Logger.debugging) {
        Logger.info(root + "---sortSubstituents---" + this);
        for (int i = 0; i < 4; i++) { // Logger
          Logger.info(getRuleName() + ": " + this + "[" + i + "]=" + atoms[i].myPath + " " + Integer.toHexString(prevPriorities[i]));
        }
        Logger.info("---");
      }


      // if this is Rule 4 or 5, then we do a check of the forward-based stereochemical path
      boolean checkRule4List = (currentRule > RULE_3 && rule4List != null);
      for (int i = 0; i < 4; i++) {
        CIPAtom a = atoms[i];
        for (int j = i + 1; j < 4; j++) {
          CIPAtom b = atoms[j];
          boolean Logger_debugHigh = Logger.debuggingHigh && b.isHeavy() && a.isHeavy();
          int score = (a.atom == null ? B_WINS : b.atom == null ? A_WINS
                : prevPriorities[i] == prevPriorities[j] ? TIED
                    : prevPriorities[j] < prevPriorities[i] ? B_WINS : A_WINS);
          if (score == TIED)
            score = (checkRule4List ? checkRule4And5(i, j) : a.compareTo(b));
          if (Logger_debugHigh)
            Logger.info(dots() + "ordering " + this.id + "." + i + "." + j + " " + this + "-" + a + " vs " + b + " = " + score);
          switch (score) {
          case IGNORE:
            // just increment the index and go on to the next rule -- no breaking of the tie
            if (checkRule4List && sphere == 0)
              achiral = true; // two ligands for the root atom found to be equivalent in Rule 4 must be achiral
            indices[i]++;
            if (Logger_debugHigh)
              Logger.info(dots() + atom + "." + b + " ends up with tie with " + a);
            break;
          case B_WINS:
            indices[i]++;
            priorities[i]++;
            if (Logger_debugHigh)
              Logger.info(dots() + this + "." + b + " B-beats " + a);
            
            break;
          case A_WINS:
            indices[j]++;
            priorities[j]++;
            if (Logger_debugHigh)
              Logger.info(dots() + this + "." + a + " A-beats " + b);
            break;
          case TIED:
            score = a.breakTie(b);
            switch (sign(score)) {
            case TIED:
              indices[i]++;
              if (Logger_debugHigh)
                Logger.info(dots() + this + "." + b + " ends up with tie with " + a);
              break;
            case B_WINS:
              indices[i]++;
              priorities[i]++;
              if (Logger_debugHigh)
                Logger.info(dots() + this + "." + b + " wins in tie with " + a);
              break;
            case A_WINS:
              indices[j]++;
              priorities[j]++;
              if (Logger_debugHigh)
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
            ties.addLast(new int[]{ i, j });
          }
        }
      }
      // update the substituent arrays

      CIPAtom[] newAtoms = new CIPAtom[4];
      int[] newPriorities = new int[4];
      int[] newPrevPriorities = new int[4];

      BS bs = new BS();
      int shift = PRIORITY_SHIFT[currentRule];
      for (int i = 0; i < 4; i++) {
        int pt = indices[i];
        CIPAtom a = newAtoms[pt] = atoms[i];
        int p = priorities[i];
        newPriorities[pt] = p;
        int pp = prevPriorities[i];
        if (pp < 0)
          pp = 0;
//        if (currentRule == RULE_1b)
//          pp = atoms[i].getBasePriority(false) & ~PRIORITY_1b_MASK;
//        if (currentRule == RULE_2)
//          pp = atoms[i].getBasePriority(true) | (pp & PRIORITY_1b_MASK);
//        else
          pp |= (p << shift);
        newPrevPriorities[pt] = pp;
        if (a.atom != null)
          bs.set(priorities[i]);
      }
      atoms = newAtoms;
      priorities = newPriorities;
      prevPriorities = newPrevPriorities;
      nPriorities = bs.cardinality();
      if (nPriorities > nPriorityMax)
        nPriorityMax = nPriorities;

      if (ties != null && !isOddCumulene) {
        switch (ties.size()) {
        case 1:
          switch (checkPseudoHandedness(ties.get(0), indices)) {
          case STEREO_R:
          case STEREO_S:
            isPseudo = canBePseudo;
            break;
          }
          break;
        case 2:
          canBePseudo = false;
          break;
        }
      }
      if (Logger.debugging) {
        Logger.info(dots() + atom + " nPriorities = " + nPriorities);
        for (int i = 0; i < 4; i++) { // Logger
          Logger.info(dots() + myPath + "[" + i + "]=" + atoms[i] + " " + priorities[i] + " " + Integer.toHexString(prevPriorities[i]) + " new");
        }
        Logger.info(dots() + "-------");
      }
    }

    private String dots() {
      return ".....................".substring(0, Math.min(20, sphere));
    }

    /**
     * Break a tie at any level in the iteration between to atoms that otherwise
     * are the same by sorting their substituents.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int breakTie(CIPAtom b) {

 
      if (Logger.debugging && isHeavy() && b.isHeavy())
        Logger.info(dots() + "tie for " + this + " and " + b + " at sphere " + sphere);

      // Two duplicates of the same atom are always tied.
      if (isDuplicate && b.isDuplicate && atom == b.atom && rootDistance == b.rootDistance)
        return TIED;

      // Do a duplicate check -- if that is not a TIE we do not have to go any further.
      // NOTE THAT THIS IS NOT EXPLICIT IN THE RULES
      // BECAUSE DUPLICATES LOSE IN THE NEXT SPHERE NOT THIS
      // THE NEED FOR THIS TEST SHOWS THAT 
      // SUBRULE 1a MUST BE COMPLETED EXHAUSTIVELY PRIOR TO SUBRULE 1b.      
      int score = checkNoSubs(b);
      if (score != TIED)
        return score*(sphere+1); // TESTING sphere instead of sphere+1 here

      // return NO_CHIRALITY/TIED if:
      //  a) both are null
      //  b) one or the other can't be set (because it has too many connections)
      //  c) or one or the other is terminal (has no substituents) 
//      if ((atom == null) != (b.atom == null))
//        return (atom == null ? B_WINS : A_WINS)*(sphere);

      if (!set() || !b.set() || isTerminal && b.isTerminal || isDuplicate
          && b.isDuplicate)
        return TIED;
      if (isTerminal != b.isTerminal)
        return (isTerminal ? B_WINS : A_WINS)*(sphere + 1);

      if (currentRule == RULE_1b) {
        preSortRule1b();
        b.preSortRule1b();
      }


      // Phase I -- shallow check only
      //
      // Check to see if any of the three connections to a and b are different.
      // Note that we do not consider duplicate atoms different from regular atoms here
      // because we are just looking for atom differences, not substituent differences.
      //
      // Say we have {O (O) C} and {O O H}
      //
      // The rules require that we first only look at just the atoms, so OOC beats OOH.

      if ((score = compareShallow(b)) != TIED)
        return score;

      // Phase II -- deep check using breakTie
      //
      // OK, so all three are nominally the same.
      // Now seriously deep-sort each list based on substituents
      // and then check them one by one to see if the tie can be broken.

      sortSubstituents();
      b.sortSubstituents();
      return compareDeep(b);
    }

    private void preSortRule1b() {
      CIPAtom a1, a2;
      for (int i = 0; i < 3; i++) {
        if (!(a1 = atoms[i]).isDuplicate)
          continue;
        for (int j = i + 1; j < 4; j++) {
        if (!(a2 =  atoms[j]).isDuplicate
          || a1.elemNo != a2.elemNo
          || a1.rootDistance <= a2.rootDistance)
          continue;
        atoms[i] = a2;
        atoms[j] = a1;
        }
      }
    }

    private boolean isHeavy() {
      return massNo > 1;
    }

    /**
     * The first part of breaking a tie at the current level. Compare only in the current substitutent sphere; a preliminary check
     * using the current rule. 
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int compareShallow(CIPAtom b) {
      for (int i = 0; i < nAtoms; i++) {
        CIPAtom ai = atoms[i];
        CIPAtom bi = b.atoms[i];
        int score = ai.checkCurrentRule(bi);
        // checkCurrentRule can return IGNORE, but we ignore that here.
        if (score == IGNORE)
          score = TIED;
        if (score != TIED) {
          if (Logger.debugging && ai.isHeavy() && bi.isHeavy())
            Logger.info(ai.dots() + "compareShallow " + ai + " " + bi + ": " + score*ai.sphere);
          return score*ai.sphere;
        }
      }
      return TIED;
    }

    /**
     * Continue to break ties in each substituent in a and b, 
     * as we now know that all of them are tied. This take us to the
     * next sphere.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int compareDeep(CIPAtom b) {
      int finalScore = (nAtoms == 0 ? B_WINS : TIED);
      int absScore = Integer.MAX_VALUE;
      for (int i = 0; i < nAtoms; i++) {
        CIPAtom ai = atoms[i];
        CIPAtom bi = b.atoms[i];
        if (Logger.debugging && ai.isHeavy() && bi.isHeavy())
          Logger.info(ai.dots() + "compareDeep sub " + ai + " " + bi);
        int score = ai.breakTie(bi);
        if (score == TIED)
          continue;
        int abs = Math.abs(score);
        if (Logger.debugging && ai.isHeavy() && bi.isHeavy())
          Logger.info(ai.dots() + "compareDeep sub " + ai + " " + bi + ": " + score);      
        if (abs < absScore) {
          absScore = abs;
          finalScore = score;
        }
      }
      if (Logger.debugging)
        Logger.info(dots() + "compareDeep " + this + " " + b + ": " + finalScore);      
      return finalScore;
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
      return (b == null ? A_WINS
          : (atom == null) != (b.atom == null) ? (atom == null ? B_WINS
              : A_WINS) : (score = checkCurrentRule(b)) == IGNORE ? TIED
              : score != TIED ? score : checkNoSubs(b));
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
    private int checkNoSubs(CIPAtom b) {
      return b.isDuplicate == isDuplicate ? TIED : b.isDuplicate ? A_WINS
          : B_WINS;
    }

    /**
     * Check this atom "A" vs a challenger "B" against the current rule.
     * 
     * Note that example BB 2013 page 1258, P93.5.2.3 requires that RULE_1b be applied
     * only after RULE_1a has been applied exhaustively for a sphere; otherwise C1 is R, not S.  
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS), or Intege.MIN_VALUE (IGNORE) 
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
      case RULE_4: // Q: Must we also separate out 4a,b,c?
        return TIED; // not carried out here because it needs access to full  list of ligands 
      case RULE_5:
        return checkRule5(b);// only used when Rule 4 is not involved, since in that case it is more complicated
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
     * 
     * 
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */

    private int checkRule1b(CIPAtom b) {
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
    private int checkRule2(CIPAtom b) {
      return b.massNo < massNo ? A_WINS : b.massNo > massNo ? B_WINS : TIED;
    }

    /**
     * Chapter 9 Rule 3. E/Z.
     * 
     * We carry out this step only as a tie in the sphere AFTER the second atom.
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
      return parent == null || !parent.isAlkeneAtom2 || !b.parent.isAlkeneAtom2
          || isDuplicate || b.isDuplicate
          || !isCumulativeType(STEREO_EZ) || !b.isCumulativeType(STEREO_EZ)
          ? IGNORE
          : parent == b.parent 
          ? sign(breakTie(b))
              : (za = parent.getEZaux()) < (zb = b.parent.getEZaux()) ? A_WINS
                  : za > zb ? B_WINS : TIED;
    }

    private boolean isCumulativeType(int type) {
      return (parent != null && parent.isAlkeneAtom2 && 
          ((parent.alkeneParent.sphere + parent.sphere) % 2) == (type == STEREO_EZ ? 1 : 2));
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
     * @return one of [STEREO_Z, STEREO_E, NO_CHIRALITY]
     */
    private int getEZaux() {
      // this is the second atom of the alkene, checked as the parent of the next atom
      // (because there is no need to do this test until we are on the branch that includes
      //  the atom after the alkene).
      if (auxEZ == STEREO_UNDETERMINED
          && (auxEZ = alkeneParent.auxEZ) == STEREO_UNDETERMINED) {
        CIPAtom winner1 = null;
        CIPAtom winner2 = null;
        CIPAtom atom1 = null;
        auxEZ = STEREO_BOTH_EZ;
        sortSubstituents();
        //System.out.println("checking EZ for " + this);
        winner2 = getTopAtom();
        if (winner2 != null) {
          if (auxParentReversed == null) {
            if (Logger.debugging)
              Logger.info("reversing path for " + alkeneParent);
            atom1 = (CIPAtom) alkeneParent.clone();
            atom1.addReturnPath(alkeneParent.nextSP2, alkeneParent);
          } else {
            atom1 = auxParentReversed;
          }
          atom1.sortSubstituents();
          winner1 = atom1.getTopAtom();
          if (winner1 != null) {
            auxEZ = (isCis(winner2, this, atom1, winner1) ? STEREO_Z
                : STEREO_E);
          }
        }
      }
      alkeneParent.auxEZ = auxEZ;
      if (Logger.debugging)
        Logger.info("getZaux " + alkeneParent + " " + auxEZ);
      return auxEZ;
    }

    /**
     * Get a list of all the parents back to Sphere 0.
     * 
     * @param a
     * @return a list of parents
     */
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
     * @param fromAtom
     */
    private void addReturnPath(CIPAtom last, CIPAtom fromAtom) {
      Lst<CIPAtom> path = getReturnPath(fromAtom);
      CIPAtom thisAtom = this;
      for (int i = 0, n = path.size(); i < n; i++) {
        CIPAtom p = path.get(i);
        if (p == null) {
          p = new CIPAtom().create(null, this, true, isAlkene);
        } else {
          int s = p.sphere;
          p = (CIPAtom) p.clone();
          p.sphere = s + 1;
        }
        thisAtom.replaceParentSubstituent(last, p);
        if (last == null)
          break;
        last = last.parent;
        thisAtom = p;
      }
    }

    /**
     * 
     * Create a priority key that matches elemNo and massNo.
     * 
     * We can skip the duplicate flag, because all these have substituents.
     * 
     * @return a shifted key based on elemNo and massNo
     */
    int getBasePriority(boolean addMass) {
      return (atom == null ? PRIORITY_12_MASK
          : ((127 - elemNo) << PRIORITY_SHIFT[RULE_1a])
            | (addMass ? (255 - massNo) << PRIORITY_SHIFT[RULE_2] : 0));
    }
    
    /**
     * The result of checking a Mata series of parallel paths may be one of
     * several values. TIED here probably means something went wrong; 
     * IGNORE means we have two of the same chirality, for example RSRR RSRR. 
     * In that case, we defer to Rule 5. The idea is to handle all such 
     * business in Sphere 1.
     * 
     * 
     * @param i
     * @param j
     * @return 0 (TIED), -1 (A_WINS), 1 (B_WINS), Integer.MIN_VALUE (IGNORE)
     */
    private int checkRule4And5(int i, int j) {
      if (rule4List[i] == null && rule4List[j] == null)
        return TIED;
      if (rule4List[i] == null || rule4List[j] == null)
        return  rule4List[j] == null ? A_WINS : B_WINS;
      return compareRootMataPair(i, j);
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
    private int compareRootMataPair(int ia, int ib) {
      boolean isRule5 = (currentRule == RULE_5);
      // note that opposites will need to generate "R" or "S" keys, which will be 
      // resolved as "r" or "s" 
      // but generally we will want to process this as "R" and "S"
      // note that this analysis cannot be done ahead of time
      String aStr = rule4List[ia].substring(1);
      String bStr = rule4List[ib].substring(1);
      if (atoms[ia].nextChiralBranch != null) {
        String s = atoms[ia].getMataList(getFirstRef(aStr), isRule5);
        aStr = (s.indexOf("|") < 0 ? aStr + s : s);
      }
      if (atoms[ib].nextChiralBranch != null) {
        String s = atoms[ib].getMataList(getFirstRef(bStr), isRule5);
        bStr = (s.indexOf("|") < 0 ? bStr + s : s);
      }
      if (Logger.debugging)
        Logger.info(dots() + this + " comparing " + atoms[ia] + " " + aStr + " to "
            + atoms[ib] + " " + bStr);
      if (aStr.length() != bStr.length())
        return TIED;
      if (isRule5)
        return aStr.compareTo(bStr);
      if (aStr.indexOf("|") >= 0 || bStr.indexOf("|") >= 0) {
        // Mata(2005) Figure 9
        // We are trying to ascertain that
        // R lull                  R luuu
        // S luuu   is the same as S lull
        // 
        // Solution is to SUM all winners. If that is 0, then they are the same
        String[] aList = PT.split(aStr, "|");
        String[] bList = PT.split(bStr, "|");
        int minScore = Integer.MAX_VALUE;
        int sumScore = 0;
        aStr = aList[0];
        bStr = bList[0];
        for (int i = 0; i < 2; i++) {
          for (int j = 0; j < 2; j++) {
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
      aStr = PT.rep(aStr, "~", "");
      bStr = PT.rep(bStr, "~", "");
      if (aStr.length() == 1 && "RS".indexOf(aStr) < 0) {
        int score = checkEnantiomer(aStr, bStr, 0, aStr.length(), " rs");
        switch (score) {
        case A_WINS:
        case B_WINS:
          canBePseudo = false;
          doCheckPseudo = true;
          return score;
        }
      }
      return compareRule4PairStr(aStr, bStr, false);
    }

    /**
     * Just get the first R- or S-equivalent in "~~~~xxxxx"
     * @param aStr
     * @return "R", "S", or null
     */
    private String getFirstRef(String aStr) {
      for (int i = 0, n = aStr.length(); i < n; i++) {
        char c = fixMataRef(aStr.charAt(i));
        switch (c) {
        case 'R':
        case 'S':
          return "" + c;
        }
      }
      return null;
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
        if (rule4List[i] != null) {
          listA[--j] = rule4List[i];
        }
      if (aref == null) {
        aref = getMataRef(isRule5);
      } else {
        // we need to add the priority business only if this is the first case
        for (int i = 0; i < n; i++)
          listA[i] = "." + listA[i].substring(1);
      }
      switch (aref.length()) {
      default:
      case 1:
        return getMataSequence(listA, aref, isRule5);
      case 2:
        return getMataSequence(listA, "R", false)  + "|" + getMataSequence(listA, "S", false);
      }
    }

    /**
     * The reference designation is the most popular of R and S of the highest-
     * priority node, or both if there are the same number at highest-priority node level
     * @param isRule5
     * @return "R", "S", or "RS"
     */
    private String getMataRef(boolean isRule5) {
      String rs = isRule5 ? "R" 
              : rule4Count[STEREO_R] > rule4Count[STEREO_S] ? "R"
                  : rule4Count[STEREO_R] < rule4Count[STEREO_S] ? "S" : "RS";
      if (Logger.debugging)
        Logger.info(this + "mata ref: " + rs + " Rule5?" + isRule5 + " " + PT.toJSON("rule4Count", rule4Count));
      return rs;
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
      int n = lst.length;
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
      int len = 0;
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
     *        return a score qualified by measure of how far into the comparison
     *        when we find a like/unlike distance
     * 
     * @return 0 (TIED), -1 (A_WINS), 1 (B_WINS), Integer.MIN_VALUE (IGNORE)
     */
    private int compareRule4PairStr(String aStr, String bStr, boolean isRSTest) {
      if (Logger.debugging)
        Logger.info(dots() + this + " Rule 4b comparing " + aStr + " " + bStr);
      doCheckPseudo = false;
      int n = aStr.length();
      if (n == 0 ||  n != bStr.length())
        return TIED;
      char aref = fixMataRef(aStr.charAt(0));
      char bref = fixMataRef(bStr.charAt(0));
      for (int c = 1; c < n; c++) {
        boolean alike = (aref == fixMataRef(aStr.charAt(c)));
        boolean blike = (bref == fixMataRef(bStr.charAt(c)));
        if (alike != blike)
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

    private char fixMataRef(char c) {
      switch (c) {
      case 'R':
      case 'M':
      case 'Z':
        return 'R';
      case 'S':
      case 'P':
      case 'E':
        return 'S';
      default:
        return c;
      }
    }
    
    /**
     * Sort Mata list of ["RS...", "SR..."] by temporarily assigning the reference atom
     * chirality the letter "A" and then sorting lexicographically.
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
          Logger.info("Sorted Mata list " + i + " " + aref + ": " + sorted[i]);
      return sorted;
    }

    /**
     * This method creates a list of downstream (higher-sphere) auxiliary
     * chirality designators R, S, r, s, u, M, P, m, p, C, T, c, t (seqCis,
     * seqTrans) that are passed upstream ultimately to the Sphere-1 root
     * substituent.
     * 
     * work in progress
     * 
     * @param node1
     *        first node; sphere 1
     * @param ret
     *        CIPAtom of next stereochemical branching point
     * 
     * @return collective string, with setting of rule4List
     */
    String createAuxiliaryRSCenters(CIPAtom node1, CIPAtom[] ret) {

      // still deciding when/if this next two bits are necessary. Only for root?

      if (auxParentReversed != null)
        auxParentReversed.createAuxiliaryRSCenters(null, null);
      if (auxPseudo != null)
        auxPseudo.createAuxiliaryRSCenters(null, null);

      //

      int rs = -1;
      String subRS = "";
      String s = (node1 == null ? "" : "~");
      boolean isBranch = false;
      if (atom != null) {
        rule4List = new String[4]; // full list based on atoms[]
        int[] mataList = new int[4]; //sequential pointers into rule4List
        int nRS = 0;
        CIPAtom[] ret1 = new CIPAtom[1];
        for (int i = 0; i < 4; i++) {
          CIPAtom a = atoms[i];
          if (a != null)
            a.set();
          if (a != null && !a.isDuplicate && !a.isTerminal) {
            a.priority = priorities[i];
            ret1[0] = null;
            String ssub = a.createAuxiliaryRSCenters(node1 == null ? a : node1,
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
            switch (adj = (compareRule4aEnantiomers(rule4List[mataList[0]],
                rule4List[mataList[1]]))) {
            case DIASTEREOMERIC:
              isBranch = true;
              s = "";
              // create a ?<sphere>[....] object ?
              break;
            case NOT_RELEVANT:
              // create a ?<sphere>[....] object ?
              s = "";
              isBranch = true;
              adj = TIED;
              break;
            case TIED:
              // identical
              isBranch = true;
              s = "u";
              subRS = "";
// TODO:  Why is this setting of ret to null?            
              if (ret != null)
                ret[0] = null;
              break;
            case A_WINS:
            case B_WINS:
              isBranch = true;
              // enantiomers -- we have an r/s situation
              // process to determine chirality, but then set ret[0] to be null
              subRS = "";
              break;
            }
          }
          break;
        case 3:
        case 4:
          s = "";
          isBranch = true;
        }
        if (isBranch) {
          subRS = "";
          if (ret != null && s != "u")
            ret[0] = this;
        }

        if (!isBranch || adj == A_WINS || adj == B_WINS) {
          if (isAlkene && alkeneChild != null) {
            // we must check for seqCis or M/P
          } else if (node1 != null
              && (bondCount == 4 && nPriorities >= 3 - Math.abs(adj) || bondCount == 3
                  && elemNo > 10 && nPriorities >= 2 - Math.abs(adj))) {
            if (isBranch) {
              // if here, adj is A_WINS (-1), or B_WINS (1) 
              switch (checkPseudoHandedness(mataList, null)) {
              case STEREO_R:
                s = "r";
                break;
              case STEREO_S:
                s = "s";
                break;
              }
              subRS = "";
              if (ret != null)
                ret[0] = null;
            } else {
              // if here, adj is TIED (0) 
              CIPAtom atom1 = (CIPAtom) clone();
              if (atom1.set()) {
                atom1.addReturnPath(null, this);
                int thisRule = currentRule;
                currentRule = RULE_1a;
                atom1.sortSubstituents();
                currentRule = thisRule;
                rs = atom1.checkHandedness();
                s = (rs == STEREO_R ? "R" : rs == STEREO_S ? "S" : "~");
                node1.addMataRef(sphere, priority, rs);
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


    private boolean isChiralSequence(String ssub) {
      return ssub.indexOf("R") >= 0 || ssub.indexOf("S") >= 0
          || ssub.indexOf("r") >= 0 || ssub.indexOf("s") >= 0
          || ssub.indexOf("u") >= 0;
    }
    /**
     * Accumlate the number of R and S centers at a given sphere+priority level
     * 
     * @param sphere 1,2,3...
     * @param priority 1-4
     * @paramPriority
     * @param rs
     */
    private void addMataRef(int sphere, int priority, int rs) {
      if (rule4Count == null) {
        rule4Count = new int[] {Integer.MAX_VALUE, 0, 0};
      }
      int n = sphere * 10 + priority;
      if (n <= rule4Count[0]) {
        if (n < rule4Count[0]) {
          rule4Count[0] = n;
          rule4Count[STEREO_R] = rule4Count[STEREO_S] = 0;
        }
        rule4Count[rs]++;
      }
//      System.out.println(this + " " + sphere + " " + priority + " " + rs + " "
//          + PT.toJSON("rule4Count", rule4Count));
    }
    /**
     * Check for enantiomeric strings such as S;R; or SR
     * 
     * @param rs1
     * @param rs2
     * @return NOT_RELEVANT if there is no stereochemistry, TIED if they are equal,
     *         A_WINS for enantiomer Rxxx, B_WINS for Sxxxx, or DIASTEREOMERIC
     *         if diastereomeric
     */
    private int compareRule4aEnantiomers(String rs1, String rs2) {
      if (rs1.indexOf("R") < 0 && rs1.indexOf("S") < 0
          || rs1.charAt(0) != rs2.charAt(0))
        return NOT_RELEVANT;
      int n = rs1.length(); 
      if (n != rs2.length())
        return NOT_RELEVANT; // TODO: ?? this may not be true -- paths with and without O, N, C for example, that still have stereochemistry
      if (rs1.equals(rs2))
        return TIED;
      
      System.out.println("testing ~RS here with " + rs1 + " and " + rs2);
      return checkEnantiomer(rs1, rs2, 1, n, "~RS");
    }

    private int checkEnantiomer(String rs1, String rs2, int m, int n, String rs) {
      int finalScore = TIED;
      // "0~~R 0~~S"
      for (int i = m; i < n; i++) {
        int i1 = rs.indexOf(rs1.charAt(i));
        int score = i1 + rs.indexOf(rs2.charAt(i));
        if (score != 0 && score != STEREO_BOTH_RS)
          return DIASTEREOMERIC;
        if (finalScore == TIED)
          finalScore =  (i1 == STEREO_R ? A_WINS : B_WINS);
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
      if (auxPseudo == null) {
        // critical here that we do NOT include the tied branches
        atom1 = (CIPAtom) clone();
        atom1.atoms[ia] = new CIPAtom().create(null, atom1, false, isAlkene);
        atom1.atoms[ib] = new CIPAtom().create(null, atom1, false, isAlkene);
        atom1.addReturnPath(null, this);
      } else {
        atom1 = auxPseudo;
      }
      int thisRule = currentRule;
      currentRule = RULE_1a;
      atom1.sortSubstituents();
      currentRule = thisRule;
      // Now add the tied branches at the end; it doesn't matter where they 
      // go as long as they are together and in order.
      atom1.atoms[bondCount - 2] = atoms[Math.min(ia, ib)];
      atom1.atoms[bondCount - 1] = atoms[Math.max(ia, ib)];
      int rs = atom1.checkHandedness();
      if (Logger.debugging) {
        for (int i = 0; i < 4; i++) // Logger
          Logger.info("pseudo " + rs + " " + priorities[i] + " " + atoms[i].myPath);
      }
      return rs;
    }


//    /**
//     * reset auxEZ chirality to "undetermined"
//     */
//    void resetAuxiliaryChirality() {
//      auxEZ = STEREO_UNDETERMINED;
//      for (int i = 0; i < 4; i++)
//        if (atoms[i] != null && atoms[i].atom != null)
//          atoms[i].resetAuxiliaryChirality();
//      if (auxParentReversed != null)
//        auxParentReversed.resetAuxiliaryChirality();
//      if (auxPseudo != null)
//        auxPseudo.resetAuxiliaryChirality();
//    }

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
          prevPriorities[i] = atoms[i].getBasePriority(true);
          parent = newParent;
          return;
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

//
//    private CIPAtom getCommonAncestor(CIPAtom b) {
//      CIPAtom a = this;
//      while ((a = a.parent) != (b = b.parent)) {}
//      return a;
//    }

    /**
     * Chapter 9 Rule 5. "T" and "C" are seqTrans and seqCis, as determined while
     * applying Rule 4.
     * 
     * For 3D models is it not necessary to consider "unspecified" chirality, as
     * that is only a 2D concept.
     * 
     * @param b
     * @return 0 (TIED), -1 (A_WINS), or 1 (B_WINS)
     */
    private int checkRule5(CIPAtom b) {
      if (isTerminal || isDuplicate)
        return TIED;
      int isRa = ";SRPMTC;".indexOf(knownAtomChirality);
      int isRb = ";SRPMTC;".indexOf(b.knownAtomChirality);
      return (isRa == isRb ? TIED : isRa > isRb ? A_WINS : B_WINS);
    }

    /**
     * Determine the ordered CIP winding of this atom.
     * 
     * @return 1 for "R", 2 for "S"
     */
    int checkHandedness() {
      P3 p1 = atoms[0].atom.getXYZ(); // highest priority
      P3 p2 = atoms[1].atom.getXYZ();
      P3 p3 = atoms[2].atom.getXYZ();
      P3 p4 = (lonePair == null ? atoms[3].atom.getXYZ() : lonePair); // lowest priority
      float d = Measure.getNormalThroughPoints(p1, p2, p3, vNorm, vTemp);
      return (Measure.distanceToPlaneV(vNorm, d, p4) > 0 ? STEREO_R : STEREO_S);
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
      a.prevPriorities = new int[] {-1, -1, -1, -1};
      a.htPathPoints = htPathPoints;
      for (int i = 0; i < 4; i++) {
        a.priorities[i] = priorities[i];
        if (atoms[i] != null) {
          a.atoms[i] = atoms[i];
          a.prevPriorities[i] = atoms[i].getBasePriority(true);
        }
      }
      if (Logger.debugging)
        Logger.info("cloning " + this + " as " + a);
      return a;

    }


    @Override
    public String toString() {
      return (atom == null ? "<null>" : "[" + currentRule + "." + sphere + "," + rootDistance + "." + id + "." + atom.getAtomName() + (isDuplicate ? "*" : "") + "]"
      //"[" + sphere + "." + id + " "
      //    + atom.toString() + (isDuplicate ? "*" : "") + knownAtomChirality + auxAtomChirality
        //  + "]"
      //+ (root ==  null ? "" : "/"+root.atom)
      );
    }

  }

  public int sign(int score) {
    return  (score < 0 ? -1 : score > 0 ? 1 : 0);
  }

}

