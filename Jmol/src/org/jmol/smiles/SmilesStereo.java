/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-08-30 01:18:16 -0500 (Sun, 30 Aug 2015) $
 * $Revision: 20742 $
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

import java.util.Arrays;

import javajs.util.AU;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Node;

//import org.jmol.util.Logger;

/**
 * This class relates to stereochemical issues.
 */
public class SmilesStereo {

  int chiralClass = Integer.MIN_VALUE;
  int chiralOrder = Integer.MIN_VALUE;
  int atomCount;
  private String details;
  private SmilesSearch search;
  private Node[] jmolAtoms;
  private String directives;
  final public static int DEFAULT = 0;
  final public static int POLYHEDRAL = 1; // Jmol polySMILES
  final public static int ALLENE = 2;
  final public static int TRIGONAL_PYRAMIDAL = 3;
  final public static int TETRAHEDRAL = 4;
  final public static int TRIGONAL_BIPYRAMIDAL = 5;
  final public static int OCTAHEDRAL = 6;
  final public static int SQUARE_PLANAR = 7;
  final public static int T_SHAPED = 8;  // Jmol SMILES
  final public static int SEESAW = 9;    // Jmol SMILES

  private static int getChiralityClass(String xx) {
    return ("0;PH;AL;TP;TH;TB;OH;SP;TS;SS;".indexOf(xx) + 1) / 3;
  }

  public static SmilesStereo newStereo(SmilesStereo stereo)
      throws InvalidSmilesException {
    return (stereo == null ? new SmilesStereo(0, 0, 0, null, null)
        : new SmilesStereo(stereo.chiralClass, stereo.chiralOrder,
            stereo.atomCount, stereo.details, stereo.directives));
  }

  SmilesStereo(int chiralClass, int chiralOrder, int atomCount, String details,
      String directives) throws InvalidSmilesException {
    this.chiralClass = chiralClass;
    this.chiralOrder = chiralOrder;
    this.atomCount = atomCount;
    this.details = details;
    this.directives = directives;
    if (chiralClass == POLYHEDRAL)
      getPolyhedralOrders();
  }

  /**
   * Returns the chiral class of the atom. (see <code>CHIRALITY_...</code>
   * constants)
   * 
   * @return Chiral class.
   */
  public int getChiralClass() {
    return chiralClass;
  }

  /**
   * Sets the chiral class of the atom. (see <code>CHIRALITY_...</code>
   * constants)
   * 
   * @param chiralClass
   *        Chiral class.
   */
  public void setChiralClass(int chiralClass) {
    this.chiralClass = chiralClass;
  }

  /**
   * Returns the chiral order of the atom.
   * 
   * @return Chiral order.
   */
  public int getChiralOrder() {
    return chiralOrder;
  }

  /**
   * Sets the chiral order of the atom.
   * 
   * @param chiralOrder
   *        Chiral order.
   */
  public void setChiralOrder(int chiralOrder) {
    this.chiralOrder = chiralOrder;
  }

  /**
   * Check number of connections and permute them to match a canonical version
   * 
   * @param sAtom
   * @throws InvalidSmilesException
   */
  public void fixStereo(SmilesAtom sAtom) throws InvalidSmilesException {
    int nBonds = Math.max(sAtom.explicitHydrogenCount, 0)
        + sAtom.getBondCount();
    int nH = Math.max(sAtom.explicitHydrogenCount, 0);
    if (chiralClass == DEFAULT) {
      switch (nBonds) {
      case 2:
        chiralClass = ALLENE;
        break;
      case 3:
        chiralClass = TRIGONAL_PYRAMIDAL;
        break;
      case 4:
      case 5:
      case 6:
        chiralClass = nBonds;
        break;
      }
    }
    switch (chiralClass) {
    case SQUARE_PLANAR:
      if (nBonds != 4 || nH > 0)
        sAtom.stereo = null;
      break;
    case POLYHEDRAL:
      // we allow no bonds here, indicating that the next N atoms are associated (but not connected)
      // with this atom
      if (nBonds != 0 && nBonds != atomCount)
        sAtom.stereo = null;
      break;
    case ALLENE:
      if (nBonds != 2 || nH > 0)
        sAtom.stereo = null;
      break;
    case TETRAHEDRAL:
      if (nBonds != 4 || nH > 1)
        sAtom.stereo = null;
      break;
    case OCTAHEDRAL:
    case TRIGONAL_BIPYRAMIDAL:
      if (nBonds != chiralClass || nH > 0 || !normalizeClass(sAtom.bonds))
        sAtom.stereo = null;
      break;
    case T_SHAPED:
      if (nBonds != 3 || nH > 0)
        sAtom.stereo = null;
      break;
    case SEESAW:
      if (nBonds != 4 || nH > 0 || !normalizeClass(sAtom.bonds))
        sAtom.stereo = null;
      break;
    default:
      sAtom.stereo = null;
    }
    if (sAtom.stereo == null)
      throw new InvalidSmilesException(
          "Incorrect number of bonds for stereochemistry descriptor");
  }

  // assignments from http://www.opensmiles.org/opensmiles.html
  private static final int[] PERM_TB = new int[] {
    // a,p,z, where:
    // a = first axial -- shift to first position
    // p = 1 @, -1 for @@ -- once shifted, this flag sets chirality
    // z = last axial  -- shift to last position
    0, 1,4, //TB1 @
    0,-1,4, //TB2 @@
    0, 1,3, //TB3 @
    0,-1,3, //TB4 @@
    0, 1,2, //TB5 @
    0,-1,2, //TB6 @@
    0, 1,1, //TB7 @
    0,-1,1, //TB8 @@
    1, 1,4, //TB9 @
    1, 1,3, //TB10 @
    1,-1,4, //TB11 @@
    1,-1,3, //TB12 @@
    1, 1,2, //TB13 @
    1,-1,2, //TB14 @@
    2, 1,4, //TB15 @
    2, 1,3, //TB16 @
    3, 1,4, //TB17 @
    3,-1,4, //TB18 @@
    2,-1,3, //TB19 @@
    2,-1,4, //TB20 @@
  };
  
  private static final int[] PERM_OCT = new int[] {
    // a,p,z, where:
    // a = first axial -- shift to first position (0)
    // p = 1 @, -1 @@, or position to permute with p+1 after setting a and z (< 0 for @@)
    // z = last axial  -- shift to last position (5)
    // so:
    // for "U" we have 1/-1 -- standard chirality check
    // for "Z" we permute groups 3 and 4
    // for "4" we permute groups 4 and 1
      0, 1,5, //OH1 a U f @ 
      0,-1,5, //OH2 a U f @@  
      0, 1,4, //OH3 a U e @ 
      0, 3,5, //OH4 a Z f @ 
      0, 3,4, //OH5 a Z e @ 
      0, 1,3, //OH6 a U d @ 
      0, 3,3, //OH7 a Z d @ 
      0, 2,5, //OH8 a 4 f @@  
      0, 2,4, //OH9 a 4 e @@  
      0,-2,5, //OH10 a 4 f @  
      0,-2,4, //OH11 a 4 e @  
      0, 2,3, //OH12 a 4 d @@ 
      0,-2,3, //OH13 a 4 d @  
      0,-3,5, //OH14 a Z f @@ 
      0,-3,4, //OH15 a Z e @@ 
      0,-1,4, //OH16 a U e @@ 
      0,-3,3, //OH17 a Z d @@ 
      0,-1,3, //OH18 a U d @@ 
      0, 1,2, //OH19 a U c @  
      0, 3,2, //OH20 a Z c @  
      0, 2,2, //OH21 a 4 c @@ 
      0,-2,2, //OH22 a 4 c @  
      0,-3,2, //OH23 a Z c @@ 
      0,-1,2, //OH24 a U c @@ 
      0, 1,1, //OH25 a U b @  
      0, 3,1, //OH26 a Z b @  
      0, 2,1, //OH27 a 4 b @@ 
      0,-2,1, //OH28 a 4 b @  
      0,-3,1, //OH29 a Z b @@ 
      0,-1,1, //OH30 a U b @@ 
  };

  // TS - like square planar, we are just looking for axial groups
  //      no @/@@ here. 


  // SS - See-Saw - what we are reading is the rotation of the first three groups after permutation
  //      no @/@@ here because there are four atoms, like tetrahedral
  private static final int[] PERM_SS = new int[] {
   0,  1, 3,  //SS1
   0, -1, 3,  //SS2
   0,  1, 2,  //SS3
   0, -1, 2,  //SS4
   0,  1, 1,  //SS5
   0, -1, 1,  //SS6
   1,  1, 3,  //SS7
   1, -1, 3,  //SS8
   1,  1, 2,  //SS9
   1, -1, 2,  //SS10
   2,  1, 3,  //SS11
   2, -1, 3,  //SS12
  };

  /**
   * re-order bonds to match standard @ and @@ types
   * 
   * @param bonds
   * @return true if OK
   */
  private boolean normalizeClass(SmilesBond[] bonds) {
    if (chiralOrder < 3)  
      return true;
    int pt = (chiralOrder - 1) * 3;
    int[] perm;
    int ilast;
    switch (chiralClass) {
    case SEESAW:
      perm = PERM_SS;
      ilast = 3;
      break;
    case TRIGONAL_BIPYRAMIDAL:
      perm = PERM_TB;
      ilast = 4;
      break;
    case OCTAHEDRAL:
      perm = PERM_OCT;
      ilast = 5;
      break;
    default:
      return true;
    }
    if (chiralOrder > perm.length)
      return false;
    int a = perm[pt]; // shifted to first position
    int z = perm[pt + 2]; // shifted to last position
    int p = Math.abs(perm[pt + 1]); // to be permuted with its next position
    boolean isAtAt = (perm[pt + 1] < 0); // negative indicates NOT
    SmilesBond b;
    if (a != 0) {
      b = bonds[a];
      for (int i = a; i > 0; --i)
        bonds[i] = bonds[i - 1];
      bonds[0] = b;
    }
    if (z != ilast) {
      b = bonds[z];
      for (int i = z; i < ilast; i++)
        bonds[i] = bonds[i + 1];
      bonds[ilast] = b;
    }
    switch (p) {
    case 1:
      break;
    default:
      b = bonds[p + 1];
      bonds[p + 1] = bonds[p];
      bonds[p] = b;
    }
    chiralOrder = (isAtAt ? 2 : 1);
    return true;
  }

  private boolean setSmilesCoordinates(SmilesAtom atom, SmilesAtom sAtom,
                                       SmilesAtom sAtom2, Node[] cAtoms) {

    // When testing equality of two SMILES strings in terms of stereochemistry,
    // we need to set the atom positions based on the ORIGINAL SMILES order,
    // which, except for the H atom, will be the same as the "matchedAtom"
    // index.
    // all the necessary information is passed via the atomSite field of Atom

    // atomSite is used by smilesMatch.find to encode chiralClass and chiralOrder 
    if (atom.stereo == null)
      return false;
    int chiralClass = atom.stereo.chiralClass;
    int chiralOrder = atom.stereo.chiralOrder;
    Node a2 = (chiralClass == ALLENE || chiralClass == 3 ? a2 = jmolAtoms[sAtom2
        .getMatchingAtomIndex()] : null);

    // set the chirality center at the origin
    atom.set(0, 0, 0);
    atom = (SmilesAtom) jmolAtoms[sAtom.getMatchingAtomIndex()];
    atom.set(0, 0, 0);

    int[] map = search.getMappedAtoms(atom, a2, cAtoms);
    int pt;
    switch (chiralClass) {
    case POLYHEDRAL:
      // todo
      break;
    case ALLENE:
    case TETRAHEDRAL:
      if (chiralOrder == 2) {
        pt = map[0];
        map[0] = map[1];
        map[1] = pt;
      }
      cAtoms[map[0]].set(0, 0, 1);
      cAtoms[map[1]].set(1, 0, -1);
      cAtoms[map[2]].set(0, 1, -1);
      cAtoms[map[3]].set(-1, -1, -1);
      break;
    /*      
        case STEREOCHEMISTRY_DOUBLE_BOND:
          switch (chiralOrder) {
          case 1: // U-shaped 0 3 2 1
            cAtoms[map[0]].set(1, 0, 0);
            cAtoms[map[1]].set(0, 1, 0);
            cAtoms[map[2]].set(-1, 0, 0);
            break;
          case 2: // 4-shaped
            cAtoms[map[0]].set(1, 0, 0);
            cAtoms[map[1]].set(-1, 0, 0);
            cAtoms[map[2]].set(0, 1, 0);
            cAtoms[map[3]].set(0, -1, 0);
            break;
          }
          break;
    */
    case SQUARE_PLANAR:
      switch (chiralOrder) {
      case 1: // U-shaped
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(0, 1, 0);
        cAtoms[map[2]].set(-1, 0, 0);
        cAtoms[map[3]].set(0, -1, 0);
        break;
      case 2: // 4-shaped
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(-1, 0, 0);
        cAtoms[map[2]].set(0, 1, 0);
        cAtoms[map[3]].set(0, -1, 0);
        break;
      case 3: // Z-shaped
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(0, 1, 0);
        cAtoms[map[2]].set(0, -1, 0);
        cAtoms[map[3]].set(-1, 0, 0);
        break;
      }
      break;
    case T_SHAPED:
      switch (chiralOrder) {
      case 1:        
        break;
      case 2:
        pt = map[2];
        map[2] = map[1];
        map[1] = pt;
        break;
      case 3:
        pt = map[0];
        map[0] = map[1];
        map[1] = pt;
        break;
      }
      cAtoms[map[0]].set(0, 0, -1);
      cAtoms[map[1]].set(0, 1, 0);
      cAtoms[map[2]].set(0, 0, 1);
      break;
    case SEESAW:
      if (chiralOrder == 2) {
        pt = map[0];
        map[0] = map[3];
        map[3] = pt;
      }
      cAtoms[map[0]].set(0, 0, 1);
      cAtoms[map[1]].set(0, 1, 0);
      cAtoms[map[1]].set(1, 1, 0);
      cAtoms[map[2]].set(0, 0, -1);
      break;
    case TRIGONAL_BIPYRAMIDAL:
    case OCTAHEDRAL:
      int n = map.length;
      if (chiralOrder == 2) {
        pt = map[0];
        map[0] = map[n - 1];
        map[n - 1] = pt;
      }
      cAtoms[map[0]].set(0, 0, 1);
      cAtoms[map[n - 1]].set(0, 0, -1);
      cAtoms[map[1]].set(1, 0, 0);
      cAtoms[map[2]].set(0, 1, 0);
      cAtoms[map[3]].set(-1, 0, 0);
      if (n == 6)
        cAtoms[map[4]].set(0, -1, 0);
      break;
      default:
       return false;
    }
    return true;
  }

  private void getX(SmilesAtom sAtom, Node[] jn, int pt,
                    boolean haveCoordinates, boolean needHSwitch) {
    Node atom = getJmolAtom(sAtom.getMatchingAtomIndex());
    boolean doSwitch = sAtom.isFirst || pt == 3;
    if (haveCoordinates) {
      if (search.isSmarts) {
        Edge[] b = atom.getEdges();
        for (int i = 0; i < b.length; i++) {
          if (b[i].getCovalentOrder() == 2)
            continue;
          Node a = jmolAtoms[atom.getBondedAtomIndex(i)];
          if (a == jn[pt - 1])
            continue;
          jn[pt] = a;
          break;
        }
      }
      if (jn[pt] == null) {
        // add a dummy point for stereochemical reference
        // imines and diazines only
        V3 v = new V3();
        int n = 0;
        for (int i = 0; i < 4; i++) {
          if (jn[i] == null)
            continue;
          n++;
          v.sub((P3) jn[i]);
        }
        if (v.length() == 0) {
          v.setT(((P3) jn[4]));
          doSwitch = false;
        } else {
          v.scaleAdd2(n + 1, (P3) getJmolAtom(sAtom.getMatchingAtomIndex()), v);
          doSwitch = search.haveTopo || doSwitch;
        }
        jn[pt] = new SmilesAtom().setIndex(-1);
        ((P3) jn[pt]).setT(v);
      }
    }
    if (jn[pt] == null) {
      jn[pt] = search.getHydrogens(atom, null);
      if (needHSwitch)
        doSwitch = true;
    }
    if (jn[pt] != null && doSwitch) {
      // a missing substituent on the SECOND atom 
      // should be placed in position 2, not 3
      // also check for the VERY first atom in a set
      // attached H is first in that case
      // so we have to switch it, since we have
      // assigned already the first atom to be
      // the first pattern atom
      Node a = jn[pt];
      jn[pt] = jn[pt - 1];
      jn[pt - 1] = a;
    }
  }

  private Node getJmolAtom(int i) {
    return (i < 0 || i >= jmolAtoms.length ? null : jmolAtoms[i]);
  }

  /**
   * Sort bond array as ccw rotation around the axis connecting the atom and the
   * reference point (polyhedron center) as seen from outside the polyhedron
   * looking in.
   * 
   * Since we are allowing no branching, all atoms will appear as separate
   * components with only numbers after them. These numbers will be processed
   * and listed in this order.
   * 
   * @param atom
   * @param atomPrev
   * @param ref
   * @param bonds
   * @param vTemp 
   */
  void sortBondsByStereo(Node atom, Node atomPrev, T3 ref, Edge[] bonds,
                          V3 vTemp) {
    if (bonds.length < 2 || !(atom instanceof T3))
      return;
    if (atomPrev == null)
      atomPrev = bonds[0].getOtherAtomNode(atom);
    Object[][] aTemp = new Object[bonds.length][0];
    if (sorter == null)
      sorter = new PolyhedronStereoSorter();
    vTemp.sub2((T3)atomPrev, ref);
    sorter.setRef(vTemp);
    for (int i = bonds.length; --i >= 0;) {
      Node a = bonds[i].getOtherAtomNode(atom);
      float f = (a == atomPrev ? 0 : 
        sorter.isAligned((T3) a, ref, (T3) atomPrev) ? -999 : 
          Measure.computeTorsion((T3) atom,
          (T3) atomPrev, ref, (T3) a, true));
      if (bonds.length > 2)
        f += 360;
      aTemp[i] = new Object[] { bonds[i], Float.valueOf(f), a };
    }
    Arrays.sort(aTemp, sorter);
    if (Logger.debugging)
      Logger.info(Escape.e(aTemp));
    for (int i = bonds.length; --i >= 0;)
      bonds[i] = (Edge) aTemp[i][0];
  }

  public boolean checkStereoChemistry(SmilesSearch smilesSearch, VTemp v) {
    search = smilesSearch;
    jmolAtoms = search.jmolAtoms;
    boolean isSmilesFind = smilesSearch.haveTopo;
    boolean invertStereochemistry = smilesSearch.invertStereochemistry;

    if (Logger.debugging)
      Logger.debug("checking stereochemistry...");

    //for debugging, first try SET DEBUG
    //for (int i = 0; i < ac; i++) {
    //  SmilesAtom sAtom = patternAtoms[i];
    //  System.out.print(sAtom + "=");
    //}
    //System.out.println("");
    //for (int i = 0; i < ac; i++) {
    //  SmilesAtom sAtom = patternAtoms[i];
    //  JmolSmilesNode atom0 = jmolAtoms[sAtom.getMatchingAtom()];
    //  System.out.print(atom0.getIndex() + "-");
    //}
    //System.out.println("");

    for (int i = 0; i < smilesSearch.ac; i++) {
      SmilesAtom sAtom = smilesSearch.patternAtoms[i];
      if (sAtom.stereo == null)
        continue;
      boolean isNot = (sAtom.not != invertStereochemistry);
      Node atom1 = null, atom2 = null, atom3 = null, atom4 = null, atom5 = null, atom6 = null;
      SmilesAtom sAtom1 = null, sAtom2 = null, sAtom0 = null;
      Node[] jn;

      Node atom0 = sAtom.getMatchingAtom();
      if (isSmilesFind)
        sAtom0 = (SmilesAtom) atom0;
      int nH = Math.max(sAtom.explicitHydrogenCount, 0);
      int order = sAtom.stereo.chiralOrder;
      int chiralClass = sAtom.stereo.chiralClass;
      // SMILES string must match pattern for chiral class.
      // but we could do something about changing those if desired.
      if (isSmilesFind && sAtom0.getChiralClass() != chiralClass)
        return false;
      if (Logger.debugging)
        Logger.debug("...type " + chiralClass + " for pattern atom " + sAtom
            + " " + atom0);
      switch (chiralClass) {
      case POLYHEDRAL:
        if (sAtom.stereo.isNot)
          isNot = !isNot;
        if (nH > 1 || sAtom.bondCount == 0)
          continue; // no chirality for [CH2@]; skip if just an indicator
        if (isSmilesFind) {
          // TODO
          continue;
        }
        SmilesBond[] bonds = sAtom.bonds;
        int jHpt = -1;
        if (nH == 1) {
          jHpt = (sAtom.isFirst ? 0 : 1);
          // can't process this unless it is tetrahedral or perhaps square planar
          if (sAtom.getBondCount() != 3)
            return false;
          v.vA.set(0, 0, 0);
          for (int j = 0; j < 3; j++)
            v.vA.add((T3) bonds[j].getOtherAtom(sAtom0).getMatchingAtom());
          v.vA.scale(0.3333f);
          v.vA.sub2((T3) atom0, v.vA);
          v.vA.add((T3) atom0);
        }
        int[][] po = sAtom.stereo.polyhedralOrders;
        int pt;
        for (int j = po.length; --j >= 0;) {
          int[] orders = po[j];
          if (orders == null || orders.length < 2)
            continue;
          // the atom we are looking down
          pt = (j > jHpt ? j - nH : j);
          T3 ta1 = (j == jHpt ? v.vA : (T3) bonds[pt].getOtherAtom(sAtom)
              .getMatchingAtom());
          float flast = (isNot ? Float.MAX_VALUE : 0);
          T3 ta2 = null;
          for (int k = 0; k < orders.length; k++) {
            pt = orders[k];
            T3 ta3;
            if (pt == jHpt) { // attached H
              ta3 = v.vA;
            } else {
              if (pt > jHpt)
                pt--;
              ta3 = (T3) bonds[pt].getOtherAtom(sAtom).getMatchingAtom();
            }
            if (k == 0) {
              ta2 = ta3;
              continue;
            }
            float f = Measure.computeTorsion(ta3, ta1, (T3) atom0, ta2, true);
            if (Float.isNaN(f))
              f = 180; // directly across the center from the previous atom
            if (orders.length == 2)
              return ((f < 0) != isNot);
            if (f < 0)
              f += 360;
            if ((f < flast) != isNot)
              return false;
            flast = f;
          }
        }
        continue;
      case ALLENE:
        boolean isAllene = true;//(chiralClass == STEREOCHEMISTRY_ALLENE);
        if (isAllene) {
          sAtom1 = sAtom.getBond(0).getOtherAtom(sAtom);
          sAtom2 = sAtom.getBond(1).getOtherAtom(sAtom);
          if (sAtom1 == null || sAtom2 == null)
            continue; // "OK - stereochemistry is desgnated for something like C=C=O
          // cumulenes
          SmilesAtom sAtom1a = sAtom;
          SmilesAtom sAtom2a = sAtom;
          while (sAtom1.getBondCount() == 2 && sAtom2.getBondCount() == 2
              && sAtom1.getValence() == 4 && sAtom2.getValence() == 4) {
            SmilesBond b = sAtom1.getBondNotTo(sAtom1a, true);
            sAtom1a = sAtom1;
            sAtom1 = b.getOtherAtom(sAtom1);
            b = sAtom2.getBondNotTo(sAtom2a, true);
            sAtom2a = sAtom2;
            sAtom2 = b.getOtherAtom(sAtom2);
          }
          sAtom = sAtom1;
        }
        jn = new Node[6];
        jn[4] = new SmilesAtom().setIndex(604);
        int nBonds = sAtom.getBondCount();
        for (int k = 0; k < nBonds; k++) {
          sAtom1 = sAtom.bonds[k].getOtherAtom(sAtom);
          if (sAtom.bonds[k].matchingBond.getCovalentOrder() == 2) {
            if (sAtom2 == null)
              sAtom2 = sAtom1;
          } else if (jn[0] == null) {
            jn[0] = sAtom1.getMatchingAtom();
          } else {
            jn[1] = sAtom1.getMatchingAtom();
          }
        }
        if (sAtom2 == null)
          continue;
        nBonds = sAtom2.getBondCount();
        if (nBonds < 2 || nBonds > 3)
          continue; // [C@]=O always matches
        for (int k = 0; k < nBonds; k++) {
          sAtom1 = sAtom2.bonds[k].getOtherAtom(sAtom2);
          if (sAtom2.bonds[k].matchingBond.getCovalentOrder() == 2) {
          } else if (jn[2] == null) {
            jn[2] = sAtom1.getMatchingAtom();
          } else {
            jn[3] = sAtom1.getMatchingAtom();
          }
        }

        if (isSmilesFind) {
          if (jn[1] == null)
            getX(sAtom, jn, 1, false, isAllene);
          if (jn[3] == null)
            getX(sAtom2, jn, 3, false, false);
          if (!setSmilesCoordinates(sAtom0, sAtom, sAtom2, jn))
            return false;
        }
        if (jn[1] == null)
          getX(sAtom, jn, 1, true, false);
        if (jn[3] == null)
          getX(sAtom2, jn, 3, true, false);
        if (!checkStereochemistryAll(sAtom.not != invertStereochemistry, atom0,
            chiralClass, order, jn[0], jn[1], jn[2], jn[3], null, null, v))
          return false;
        continue;
      case T_SHAPED:
      case SEESAW:
      case TRIGONAL_PYRAMIDAL:
      case TETRAHEDRAL:
      case SQUARE_PLANAR:
      case TRIGONAL_BIPYRAMIDAL:
      case OCTAHEDRAL:
        atom1 = getJmolAtom(sAtom.getMatchingBondedAtom(0));
        switch (nH) {
        case 0:
          atom2 = getJmolAtom(sAtom.getMatchingBondedAtom(1));
          break;
        case 1:
          // have to correct for implicit H atom in  [@XXnH]
          atom2 = smilesSearch.getHydrogens(sAtom.getMatchingAtom(), null);
          if (sAtom.isFirst) {
            Node a = atom2;
            atom2 = atom1;
            atom1 = a;
          }
          break;
        default:
          continue;
        }
        atom3 = getJmolAtom(sAtom.getMatchingBondedAtom(2 - nH));
        atom4 = getJmolAtom(sAtom.getMatchingBondedAtom(3 - nH));
        atom5 = getJmolAtom(sAtom.getMatchingBondedAtom(4 - nH));
        atom6 = getJmolAtom(sAtom.getMatchingBondedAtom(5 - nH));

        // in all the checks below, we use Measure utilities to 
        // three given atoms -- the normal, in particular. We 
        // then use dot products to check the directions of normals
        // to see if the rotation is in the direction required. 

        // we only use TP1, TP2, OH1, OH2 here.
        // so we must also check that the two bookend atoms are axial

        if (isSmilesFind
            && !setSmilesCoordinates(sAtom0, sAtom, sAtom2, new Node[] { atom1,
                atom2, atom3, atom4, atom5, atom6 }))
          return false;
        if (!checkStereochemistryAll(isNot, atom0, chiralClass, order, atom1,
            atom2, atom3, atom4, atom5, atom6, v))
          return false;
        continue;
      }
    }
    return true;
  }

  /**
   * 
   * @param atom0
   * @param atoms
   * @param nAtoms
   * @param v
   * @return String
   */
  static String getStereoFlag(Node atom0, Node[] atoms, int nAtoms, VTemp v) {
    Node atom1 = atoms[0];
    Node atom2 = atoms[1];
    Node atom3 = atoms[2];
    Node atom4 = atoms[3];
    Node atom5 = atoms[4];
    Node atom6 = atoms[5];
    int chiralClass = TETRAHEDRAL;
    // what about POLYHEDRAL?
    switch (nAtoms) {
    default:
    case 5:
    case 6:
      // like tetrahedral
      return (checkStereochemistryAll(false, atom0, chiralClass, 1, atom1,
          atom2, atom3, atom4, atom5, atom6, v) ? "@" : "@@");
    case 2: // allene
    case 4: // tetrahedral, square planar
      if (atom3 == null || atom4 == null)
        return "";
      float d = SmilesSearch.getNormalThroughPoints(atom1, atom2, atom3,
          v.vTemp, v.vA, v.vB);
      if (Math.abs(distanceToPlane(v.vTemp, d, (P3) atom4)) < 0.2f) {
        chiralClass = SQUARE_PLANAR;
        if (checkStereochemistryAll(false, atom0, chiralClass, 1, atom1, atom2,
            atom3, atom4, atom5, atom6, v))
          return "@SP1";
        if (checkStereochemistryAll(false, atom0, chiralClass, 2, atom1, atom2,
            atom3, atom4, atom5, atom6, v))
          return "@SP2";
        if (checkStereochemistryAll(false, atom0, chiralClass, 3, atom1, atom2,
            atom3, atom4, atom5, atom6, v))
          return "@SP3";
      } else {
        return (checkStereochemistryAll(false, atom0, chiralClass, 1, atom1,
            atom2, atom3, atom4, atom5, atom6, v) ? "@" : "@@");
      }
    }
    return "";
  }

  private static boolean checkStereochemistryAll(boolean isNot, Node atom0,
                                         int chiralClass, int order,
                                         Node atom1, Node atom2, Node atom3,
                                         Node atom4, Node atom5, Node atom6,
                                         VTemp v) {

    switch (chiralClass) {
    default:
      return true;
    case POLYHEDRAL:
      return true;
    case TRIGONAL_PYRAMIDAL:
      return (isNot == (getHandedness(atom2, atom3, atom0, atom1, v) != order));
    case ALLENE:
    case TETRAHEDRAL:
      return (isNot == (getHandedness(atom2, atom3, atom4, atom1, v) != order));
    case TRIGONAL_BIPYRAMIDAL:
      // check for axial-axial'
      if (!isDiaxial(atom0, atom0, atom5, atom1, v, -0.95f))
        return false;
      return (isNot == (getHandedness(atom2, atom3, atom4, atom1, v) != order));
    case T_SHAPED:
      // just checking linearity here; could do better.
      switch (order) {
      case 1:
        // @TS1
        // 1----0----3
        //      |
        //      2
        break;
      case 2:
        // @TS2
        // 1----0----2
        //      |
        //      3
        atom3 = atom2;
        break;
      case 3:
        // @TS3
        // 2----0----3
        //      |
        //      1
        atom1 = atom2;
        break;
      }
      return (isNot == !isDiaxial(atom0, atom0, atom1, atom3, v, -0.95f));
    case SEESAW:
      if (!isDiaxial(atom0, atom0, atom4, atom1, v, -0.95f))
        return false;
      return (isNot == (getHandedness(atom2, atom3, atom4, atom1, v) != order));
    case OCTAHEDRAL:
      if (!isDiaxial(atom0, atom0, atom6, atom1, v, -0.95f)
          || !isDiaxial(atom0, atom0, atom2, atom4, v, -0.95f)
          || !isDiaxial(atom0, atom0, atom3, atom5, v, -0.95f))
        return false;
      getPlaneNormals(atom2, atom3, atom4, atom5, v);
      // check for proper order 2-3-4-5
      //                          n1n2n3
      if (v.vNorm2.dot(v.vNorm3) < 0 || v.vNorm3.dot(v.vNorm4) < 0)
        return false;
      // check for CW or CCW set in relation to the first atom
      v.vNorm3.sub2((P3) atom0, (P3) atom1);
      return (isNot == ((v.vNorm2.dot(v.vNorm3) < 0 ? 2 : 1) == order));
      //case STEREOCHEMISTRY_DOUBLE_BOND:
      //System.out.println("draw p1 " + Point3f.new3((Point3f)atom1)+" color red");
      //System.out.println("draw p2 " + Point3f.new3((Point3f)atom2)+" color yellow");
      //System.out.println("draw p3 " + Point3f.new3((Point3f)atom3)+" color green");
      //System.out.println("draw p4 " + Point3f.new3((Point3f)atom4)+" color blue");

      //getPlaneNormals(atom1, atom2, atom3, atom4, v);
      //System.out.println(order + " "+ atom1.getAtomName() + "-" + atom2.getAtomName() + "-"  + atom3.getAtomName() + "-" + atom4.getAtomName());
      //return (isNot == ((v.vNorm1.dot(v.vNorm2) < 0 ? 2 : 1) == order));
    case SQUARE_PLANAR:
      getPlaneNormals(atom1, atom2, atom3, atom4, v);
      // vNorm1 vNorm2 vNorm3 are right-hand normals for the given
      // triangles
      // 1-2-3, 2-3-4, 3-4-1
      // sp1 up up up U-shaped   1 2 3 4
      // sp2 up up DOWN 4-shaped 1 2 4 3
      // sp3 up DOWN DOWN Z-shaped  1 3 2 4
      // 
      return (v.vNorm2.dot(v.vNorm3) < 0 ? isNot == (order != 3) : v.vNorm3
          .dot(v.vNorm4) < 0 ? isNot == (order != 2) : isNot == (order != 1));
    }
  }

  static boolean isDiaxial(Node atomA, Node atomB, Node atom1, Node atom2,
                           VTemp v, float f) {
    v.vA.sub2((P3) atomA, (P3) atom1);
    v.vB.sub2((P3) atomB, (P3) atom2);
    v.vA.normalize();
    v.vB.normalize();
    // -0.95f about 172 degrees
    return (v.vA.dot(v.vB) < f);
  }

  /**
   * determine the winding of the circuit a--b--c relative to point pt
   * 
   * @param a
   * @param b
   * @param c
   * @param pt
   * @param v
   * @return 1 for "@", 2 for "@@"
   */
  static int getHandedness(Node a, Node b, Node c, Node pt, VTemp v) {
    float d = SmilesSearch.getNormalThroughPoints(a, b, c, v.vTemp, v.vA,
        v.vB);
    //int atat = (distanceToPlane(v.vTemp, d, (Point3f) pt) > 0 ? 1 : 2);
    //System.out.println("draw p1 " + Point3f.new3((Point3f)a) +" color red");
    //System.out.println("draw p2 " + Point3f.new3((Point3f)b)+" color green");
    //System.out.println("draw p3 " + Point3f.new3((Point3f)c)+" color blue");
    //System.out.println("draw p " + Point3f.new3((Point3f)a) +" " + Point3f.new3((Point3f)b)+" " + Point3f.new3((Point3f)c));
    //System.out.println("draw v vector " + Point3f.new3((Point3f)pt) + " " + v.vTemp+" \""+ (atat==2 ? "@@" : "@")+"\" color " + (atat == 2 ? "white" : "yellow"));
    return (distanceToPlane(v.vTemp, d, (P3) pt) > 0 ? 1 : 2);
  }

  private static void getPlaneNormals(Node atom1, Node atom2, Node atom3,
                                      Node atom4, VTemp v) {
    SmilesSearch.getNormalThroughPoints(atom1, atom2, atom3, v.vNorm2,
        v.vTemp1, v.vTemp2);
    SmilesSearch.getNormalThroughPoints(atom2, atom3, atom4, v.vNorm3,
        v.vTemp1, v.vTemp2);
    SmilesSearch.getNormalThroughPoints(atom3, atom4, atom1, v.vNorm4,
        v.vTemp1, v.vTemp2);
  }

  static float distanceToPlane(V3 norm, float w, P3 pt) {
    return (norm == null ? Float.NaN : (norm.x * pt.x + norm.y * pt.y + norm.z
        * pt.z + w)
        / (float) Math
            .sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z));
  }

  static int checkChirality(String pattern, int index, SmilesAtom newAtom)
      throws InvalidSmilesException {
    int stereoClass = 0;
    int order = Integer.MIN_VALUE;
    int len = pattern.length();
    String details = null;
    String directives = null;
    int atomCount = 0;
    char ch;
    stereoClass = DEFAULT;
    order = 1;
    boolean isPoly = false;
    if (++index < len) {
      switch (ch = pattern.charAt(index)) {
      case '@':
        order = 2;
        index++;
        break;
      case 'H': // @H, @@H
        break;
      case 'P': //PH // Jmol
        isPoly = true;
        //$FALL-THROUGH$
      case 'A': //AL
      case 'O': //OH
      case 'S': //SP
      case 'T': //TH, TP
        stereoClass = (index + 1 < len ? getChiralityClass(pattern.substring(
            index, index + 2)) : -1);
        index += 2;
        break;
      default:
        order = (PT.isDigit(ch) ? 1 : -1);
      }
      int pt = index;
      if (order == 1 || isPoly) {
        while (pt < len && PT.isDigit(pattern.charAt(pt)))
          pt++;
        if (pt > index) {
          try {
            int n = Integer.parseInt(pattern.substring(index, pt));
            if (isPoly) {
              atomCount = n;
              if (pt < len && pattern.charAt(pt) == '(') {
                details = SmilesParser.getSubPattern(pattern, pt, '(');
                pt += details.length() + 2;
              }
              if (pt < len && pattern.charAt(pt) == '/') {
                directives = SmilesParser.getSubPattern(pattern, pt, '/');
                pt += directives.length() + 2;
              }
            } else {
              order = n;
            }
          } catch (NumberFormatException e) {
            order = -1;
          }
          index = pt;
        }
      }
      if (order < 1 || stereoClass < 0)
        throw new InvalidSmilesException("Invalid stereochemistry descriptor");
    }
    newAtom.stereo = new SmilesStereo(stereoClass, order, atomCount, details,
        directives);
    if (SmilesParser.getChar(pattern, index) == '?') {
      Logger.info("Ignoring '?' in stereochemistry");
      index++;
    }
    return index;
  }
  
  private int[][] polyhedralOrders;
  private boolean isNot;
  private PolyhedronStereoSorter sorter;

  /**
   * experimental Jmol polySMILES 
   *  
   * @throws InvalidSmilesException
   */
  private void getPolyhedralOrders() throws InvalidSmilesException {
    int[][] po = polyhedralOrders = AU.newInt2(atomCount);
    if (details == null)
      return;
    int[] temp = new int[details.length()];
    int[] ret = new int[1];
    String msg = null;
    int pt = 0;
    String s = details + "/";
    int n = 0;
    int len = s.length();
    int index = 0;
    int atomPt = 0;
    do {
      char ch = s.charAt(index);
      switch (ch) {
      case '!':
        isNot = true;
        index++;
        break;
      case '/':
      case '.':
        if ((pt = atomPt) >= atomCount) {
          msg = "Too many descriptors";
          break;
        }
        int[] a = po[atomPt] = new int[n];
        for (; --n >= 0;)
          a[n] = temp[n];
        n = 0;
        if (Logger.debugging)
          Logger.info(PT.toJSON("@PH" + atomCount + "[" + atomPt + "]", a));
        if (ch == '/')
          index = Integer.MAX_VALUE;
        else
          index++;
        atomPt++;
        break;
      default:
        index = SmilesParser.getRingNumber(s, index, ch, ret);
        pt = temp[n++] = ret[0] - 1;
        if (pt == atomPt)
          msg = "Atom cannot connect to itself";
        else if (pt < 0 || pt >= atomCount)
          msg = "Connection number outside of range (1-" + atomCount + ")";
        else if (n >= atomCount)
          msg = "Too many connections indicated";
      }
      if (msg != null) {
        msg += ": " + s.substring(0, index) + "<<";
        throw new InvalidSmilesException(msg);
      }
    } while (index < len);
  }



}
 