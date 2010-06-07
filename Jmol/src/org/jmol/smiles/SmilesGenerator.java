/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-06-05 21:50:17 -0500 (Sat, 05 Jun 2010) $
 * $Revision: 13295 $
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

import java.util.BitSet;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import org.jmol.api.JmolMolecule;
import org.jmol.api.JmolNode;
import org.jmol.api.JmolEdge;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.smiles.SmilesSearch.VTemp;

/**
 * public method getBioSmiles returns a Jmol BIOSMILES string
 * or, if it is not biological, a SMILES string with comment header.
 * Square planar and tetrahedral stereochemistry only, not double-bond stereochemistry.
 * 
 */
public class SmilesGenerator {

  /*
   * see package.html for details
   *
   * Bob Hanson, Jmol 12.0.RC17 2010.06.5
   * 
   */

  // generation of SMILES strings
  static String getBioSmiles(JmolNode[] atoms, int atomCount,
                             BitSet bsSelected, String comment)
      throws InvalidSmilesException {
    StringBuffer sb = new StringBuffer();
    BitSet bs = (BitSet) bsSelected.clone();
    sb.append("//* Jmol BIOSMILES ").append(comment.replace('*', '_')).append(
        " *//");
    Hashtable ht = new Hashtable();
    int[] nPairs = new int[1];
    String end = "\n";
    BitSet bsIgnore = new BitSet();
    String lastComponent = null;
    String s;
    VTemp v = new VTemp();
    Vector vLinks = new Vector();
    try {
      int len = 0;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        JmolNode a = atoms[i];
        String ch = a.getGroup1('?');
        String groupType = a.getGroupType();
        boolean unknown = (ch.equals("?"));
        if (end != null) {
          sb.append(end);
          end = null;
          len = 0;
          if (groupType.length() > 0) {
            char id = a.getChainID();
            if (id != '\0') {
              s = "//* chain " + id + " " + groupType + " *// ";
              len = s.length();
              sb.append(s);
            }
            sb.append("~");
            len++;
          } else {
            s = getSmilesComponent(atoms, atomCount, a, bs, nPairs, v);
            if (s.equals(lastComponent)) {
              end = "";
            } else {
              lastComponent = s;
              sb.append(s);
              end = ".\n";
            }
            continue;
          }
        }
        if (len >= 75) {
          sb.append("\n  ");
          len = 2;
        }
        if (unknown) {
          sb.append("[");
          if (groupType.length() > 0)
            sb.append(a.getGroup3(false)).append(".0");
          else
            sb.append(Elements.elementNameFromNumber(a.getElementNumber()));
          sb.append("]");
        } else {
          sb.append(ch);
        }
        len++;
        int i0 = a.getOffsetResidueAtom("0", 0);
        a.getCrossLinkLeadAtomIndexes(vLinks);
        for (int j = 0; j < vLinks.size(); j++) {
          s = getRingCache(ht, i0, ((Integer)vLinks.get(j)).intValue(), nPairs);
          sb.append(":").append(s);
          len += 1 + s.length();
        }
        vLinks.clear();
        int i2 = a.getOffsetResidueAtom("0", 1);
        a.setGroupBits(bsIgnore);
        bs.andNot(bsIgnore);
        if (i2 < 0 || !bs.get(i2)) {
          if (i2 < 0 && (i2 = bs.nextSetBit(i + 1)) < 0)
            break;
          if (len > 0)
            end = ".\n";
        }
        i = i2 - 1;
      }
    } catch (Exception e) {
      return "";
    }
    if (!ht.isEmpty()) {
      dumpRingKeys(ht, sb);
      throw new InvalidSmilesException("//* ?ring error? *//");
    }
    s = sb.toString();
    if (s.endsWith(".\n"))
      s = s.substring(0, s.length() - 2);
    return s;
  }

  /**
   * 
   * creates a valid SMILES string from a model.
   * TODO: stereochemistry other than square planar and tetrahedral
   * 
   * @param atoms
   * @param atomCount
   * @param atom
   * @param bs
   * @param nPairs
   * @param vTemp
   * @return        SMILES
   * @throws InvalidSmilesException
   */
  private static String getSmilesComponent(JmolNode[] atoms, int atomCount,
                                   JmolNode atom, BitSet bs, int[] nPairs,
                                   VTemp vTemp)
      throws InvalidSmilesException {

    if (atom.getElementNumber() == 1 && atom.getEdges().length > 0)
      atom = atoms[atom.getBondedAtomIndex(0)]; // don't start with H
    BitSet bs2 = (BitSet) bs.clone();
    BitSet bsAromatic = new BitSet();
    int atomIndex = atom.getIndex();
    bs2 = JmolMolecule.getBranchBitSet(atoms, bs2, atomIndex, -1, true);
    bs.andNot(bs2);
    for (int j = 0; j < atomCount; j++)
      if (bs2.get(j)) {
        JmolNode a = atoms[j];
        if (a.getElementNumber() == 1 && a.getIsotopeNumber() == 0)
          bs2.clear(j);
      }
    StringBuffer ringSets = null;
    if (bs2.cardinality() > 2) {
      SmilesSearch search = null;
      search = SmilesParser.getMolecule("A[=&@]A", true);
      search.jmolAtoms = atoms;
      search.bsSelected = bs2;
      search.jmolAtomCount = atomCount;
      search.setRingData(null);
      bsAromatic = search.getBsAromatic();
      ringSets = search.getRingSets();
    }
    Hashtable ht = new Hashtable();
    StringBuffer sb1 = new StringBuffer();
    BitSet bs0 = (BitSet) bs2.clone();
    getSmiles(sb1, null, null, atom, bs0, bs2, ht, nPairs, bsAromatic, ringSets, vTemp);
    while (bs2.cardinality() > 0 && !ht.isEmpty()) {
      Enumeration e = ht.keys();
      e.hasMoreElements();
      atomIndex = ((Integer) ((Object[]) ht.get(e.nextElement()))[1])
          .intValue();
      sb1.append(".");
      getSmiles(sb1, null, null, atoms[atomIndex], bs0, bs2, ht, nPairs,
          bsAromatic, ringSets, vTemp);
    }
    if (!ht.isEmpty()) {
      dumpRingKeys(ht, sb1);
      throw new InvalidSmilesException("//* ?ring error? *//");
    }
    return sb1.toString();
  }

  private static void getSmiles(StringBuffer sb, JmolNode prevAtom,
                                JmolEdge prevBond, JmolNode atom, BitSet bs0,
                                BitSet bs, Hashtable ht, int[] nPairs,
                                BitSet bsAromatic, StringBuffer ringSets, VTemp vTemp) {
    int atomIndex = atom.getIndex();
    if (!bs.get(atomIndex))
      return;
    bs.clear(atomIndex);
    int atomicNumber = atom.getElementNumber();
    int nH = 0;
    Vector v = new Vector();
    JmolEdge bond0 = null;
    JmolEdge bondPrev = null;
    int prevIndex = (prevAtom == null ? -1 : prevAtom.getIndex());
    JmolEdge[] bonds = atom.getEdges();
    JmolNode aH = null;
    boolean isAromatic = bsAromatic.get(atomIndex);
    int stereoFlag = (isAromatic ? 10 : 0);
    JmolNode[] stereo = new JmolNode[7];
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;) {
        JmolEdge bond = bonds[i];
        if (!bond.isCovalent())
          continue;
        if (bond.getCovalentOrder() != 1)
          stereoFlag = 10;
        JmolNode atom1 = bonds[i].getOtherAtom(atom);
        int index1 = atom1.getIndex();
        if (atom1.getElementNumber() == 1 && atom1.getIsotopeNumber() == 0) {
          aH = atom1;
          nH++;
          if (nH > 1)
            stereoFlag = 10;
        } else if (index1 == prevIndex)
          bondPrev = bonds[i];
        else if (bs0.get(index1))
          v.add(bonds[i]);
      }
    // TODO : more stereochemistry
    int valence = atom.getValence();
    int charge = atom.getFormalCharge();
    int isotope = atom.getIsotopeNumber();
    
    if (bondPrev != null) {
      sb.append(SmilesBond.getBondOrderString(bondPrev.getCovalentOrder()));
      if (stereoFlag < 7)
        stereo[stereoFlag++] = prevAtom;
    }
    
    if (stereoFlag < 7 && nH == 1)
      stereo[stereoFlag++] = aH;
    int nMax = 0;
    StringBuffer sMore = new StringBuffer();
    for (int i = 0; i < v.size(); i++) {
      JmolEdge bond = (JmolEdge) v.get(i);
      JmolNode a = bond.getOtherAtom(atom);
      int n = a.getCovalentBondCount() - a.getCovalentHydrogenCount();
      int order = bond.getCovalentOrder();
      if (order == 1 && n == 1 && i < v.size() - (bond0 == null ? 1 : 0)) {
        StringBuffer s2 = new StringBuffer();
        s2.append("(");
        getSmiles(s2, atom, prevBond, a, bs0, bs, ht, nPairs, bsAromatic, ringSets, vTemp);
        s2.append(")");
        if (sMore.indexOf(s2.toString()) >= 0)
          stereoFlag = 10;
        sMore.append(s2);
        v.removeElementAt(i--);
        if (stereoFlag < 7)
          stereo[stereoFlag++] = a;
      } else if ((order > 1 || n > nMax)
          && !ht.containsKey(getRingKey(a.getIndex(), atomIndex))) {
        nMax = (order > 1 ? Integer.MAX_VALUE : n);
        bond0 = bond;
      }
    }
    for (int i = v.size(); --i >= 0;) {
      JmolEdge bond = (JmolEdge) v.get(i);
      if (bond == bond0)
        continue;
      JmolNode a = bond.getOtherAtom(atom);
      String s = getRingCache(ht, atomIndex, a.getIndex(), nPairs);
      sMore.append(SmilesBond.getBondOrderString(bond.getOrder()));
      sMore.append(s);
      if (stereoFlag < 7)
        stereo[stereoFlag++] = a;
    }
    JmolNode a = (bond0 == null ? null : bond0.getOtherAtom(atom));
    if (a != null && stereoFlag < 7)
      stereo[stereoFlag++] = a;
    String s = (stereoFlag > 6 ? "" : SmilesSearch.getStereoFlag(atom, stereo,
        stereoFlag, vTemp));
    sb.append(SmilesAtom.getAtomLabel(atomicNumber, isotope, valence, charge,
        nH, isAromatic, s));
    sb.append(sMore);
    if (bond0 != null)
      getSmiles(sb, atom, prevBond, a, bs0, bs, ht, nPairs, bsAromatic, ringSets, vTemp);
  }

  private static String getRingKey(int i0, int i1) {
    return Math.min(i0, i1) + "_" + Math.max(i0, i1);
  }

  private static String getRingCache(Hashtable ht, int i0, int i1, int[] nPairs) {
    String key = getRingKey(i0, i1);
    Object[] o = (Object[]) ht.get(key);
    String s = (o == null ? null : (String) o[0]);
    if (s == null) {
      ht.put(key, new Object[] { s = SmilesParser.getRingPointer(++nPairs[0]),
          new Integer(i1) });
      if (Logger.debugging)
        Logger.debug("adding for " + i0 + " ring key " + nPairs[0] + ": " + key);
    } else {
      ht.remove(key);
      if (Logger.debugging)
        Logger.debug("using ring key " + key);
    }
    return s;//  + " _" + key + "_ \n";
  }
  
  private static void dumpRingKeys(Hashtable ht, StringBuffer sb) {
    if (Logger.debugging) {
      Logger.debug(sb.toString() + "\n\n");
      Enumeration e = ht.keys();
      while (e.hasMoreElements()) {
        Logger.debug("unmatched ring key: " + e.nextElement());
      }
    }

  }
}
