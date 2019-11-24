/*  
 *  The Janocchio program is (C) 2007 Eli Lilly and Co.
 *  Authors: David Evans and Gary Sharman
 *  Contact : janocchio-users@lists.sourceforge.net.
 * 
 *  It is derived in part from Jmol 
 *  (C) 2002-2006 The Jmol Development Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
 *
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * distanceJMolecule.java
 *
 * Created on 03 April 2006, 17:05
 *
 */

package org.openscience.jmol.app.janocchio;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.BS;
import javajs.util.Lst;

import org.jmol.modelset.Atom;
import org.jmol.quantum.NMRCalculation;
import org.jmol.viewer.JC;

/**
 * Connection to NMRCalculation allowing for a CDK version as well (which is no
 * longer supported because of the 25 MB hit from the CDK library and because
 * Jmol already provides smart ways to group the hydrogen atoms).
 * 
 * @author Bob Hanson
 */
public class DistanceJMoleculeNoCDK extends DistanceJMolecule {

  NMR_JmolPanel nmrPanel;
  private BS bsMol;
  private String[] labelArray;

  /**
   * 
   * @param nmrPanel
   * @param bsMol
   * @param labelArray
   *        optional array of atom labels, one per atom in the current frame
   */
  public DistanceJMoleculeNoCDK(NMR_JmolPanel nmrPanel, BS bsMol,
      String[] labelArray) {
    this.nmrPanel = nmrPanel;
    viewer = (NMR_Viewer) nmrPanel.vwr;
    this.bsMol = bsMol;
    this.labelArray = labelArray;
    addAtomstoMatrix();

  }

  @Override
  protected DihedralCouple getDihedralCouple(int numAtom1, int numAtom2,
                                             int numAtom3, int numAtom4) {
    return new DihedralCoupleNoCDK(numAtom1, numAtom4);
  }

  /**
   * Generate noeMatrix and indexAtomInNoeMatrix fields for DistanceJMolecule.
   * 
   */
  protected void addAtomstoMatrix() {

    int atomCount = bsMol.cardinality();
    BS bsH = null;
    try {
      bsH = (atomCount == 0 ? new BS() : viewer.getSmartsMatch("[H]", bsMol));
    } catch (Exception e1) {
      // not possible - the SMARTS expression is valid.
    }

    // we will fill these two
    Map<Atom, String> labels = new Hashtable<Atom, String>();
    Map<Atom, Integer> indexAtomInMol = new Hashtable<Atom, Integer>();

    Map<String, Lst<Atom>> labelMap = createLabelMapAndIndex(bsH, labels,
        indexAtomInMol);
    Lst<Object> hAtoms = createHAtomList(bsH, labels, labelMap);
    createNOEMatrix(hAtoms, indexAtomInMol, new int[atomCount]);
  }

  private Map<String, Lst<Atom>> createLabelMapAndIndex(BS bsH,
                                                        Map<Atom, String> labels,
                                                        Map<Atom, Integer> indexAtomInMol) {

    Map<String, Lst<Atom>> labelMap = new Hashtable<String, Lst<Atom>>();
    if (viewer == null)
      System.out.println("????");
    for (int pt = 0, i = bsMol.nextSetBit(0); i >= 0; i = bsMol
        .nextSetBit(i + 1), pt++) {
      Atom a = viewer.getAtomAt(i);
      indexAtomInMol.put(a, Integer.valueOf(pt));
      if (labelArray != null) {
        String label = labelArray[pt];
        if (labelArray[pt] == null) {
          labels.put(a, "");
          // but no labelMap is necessary;
        } else {
          Lst<Atom> lst = labelMap.get(label);
          if (lst == null) {
            labelMap.put(label, lst = new Lst<Atom>());
          } else {
            bsH.clear(i);
          }
          lst.addLast(a);
          labels.put(a, label);
        }
      }
    }
    return labelMap;
  }

  private Lst<Object> createHAtomList(BS bsH, Map<Atom, String> labels,
                                      Map<String, Lst<Atom>> labelMap) {
    /**
     * 
     * Create hAtoms list, which can have three possible element types:
     * 
     * Atom a hydrogen
     * 
     * Atom[3] a methyl group
     * 
     * Lst<Atom> otherwise noted as identical by their label
     * 
     */

    Lst<Object> hAtoms = new Lst<Object>();
    try {
      // find and group all methyl groups -- simple unique SMARTS map here:
      if (!bsMol.isEmpty()) {
        int[][] methyls = viewer.getSmartsMap("C({[H]})({[H]}){[H]}", bsMol,
            JC.SMILES_TYPE_SMARTS | JC.SMILES_MAP_UNIQUE);
        for (int i = methyls.length; --i >= 0;) {
          Atom[] methyl = new Atom[3];
          for (int j = 0; j < 3; j++) {
            int pt = methyls[i][j];
            methyl[j] = viewer.getAtomAt(pt);
            bsH.clear(pt);
          }
          hAtoms.addLast(methyl);
        }
      }
    } catch (Exception e) {
      // not possible
    }

    // 
    for (int i = bsH.nextSetBit(0); i >= 0; i = bsH.nextSetBit(i + 1)) {
      Atom a = viewer.getAtomAt(i);
      String label = labels.get(a);
      Lst<Atom> atoms = (label == null ? null : labelMap.get(labels.get(a)));
      if (atoms != null && atoms.size() > 1) {
        hAtoms.addLast(atoms);
      } else {
        hAtoms.addLast(a);
      }
    }
    return hAtoms;
  }

  /**
   * Create noeMatix and indexAtomInNoeMatrix from hAtoms and indexAtomInMol.
   * 
   * @param hAtoms
   * @param indexAtomInMol
   * @param map
   */
  private void createNOEMatrix(Lst<Object> hAtoms,
                               Map<Atom, Integer> indexAtomInMol,
                               int[] map) {
    int nHAtoms = hAtoms.size();
    indexAtomInNoeMatrix = map;
    noeMatrix = new NoeMatrix();
    noeMatrix.makeAtomList(nHAtoms);
    for (int i = 0; i < nHAtoms; i++) {
      Object aobj = hAtoms.get(i);
      if (aobj instanceof Atom) {
        Atom a = (Atom) hAtoms.get(i);
        indexAtomInNoeMatrix[(indexAtomInMol.get(a)).intValue()] = i;
        noeMatrix.addAtom(a.x, a.y, a.z);
      } else if (aobj instanceof Lst) {
        @SuppressWarnings("unchecked")
        Lst<Atom> lst = (Lst<Atom>) aobj;
        int nEquiv = lst.size();
        for (int j = 0; j < nEquiv; j++) {
          indexAtomInNoeMatrix[(indexAtomInMol.get(lst.get(j))).intValue()] = i;
        }
        double[] xa = new double[nEquiv];
        double[] ya = new double[nEquiv];
        double[] za = new double[nEquiv];
        for (int j = 0; j < nEquiv; j++) {
          Atom a = lst.get(j);
          xa[j] = a.x;
          ya[j] = a.y;
          za[j] = a.z;
        }
        noeMatrix.addEquiv(xa, ya, za);
      } else {
        Atom[] a = (Atom[]) aobj;
        indexAtomInNoeMatrix[(indexAtomInMol.get(a[0])).intValue()] = i;
        indexAtomInNoeMatrix[(indexAtomInMol.get(a[1])).intValue()] = i;
        indexAtomInNoeMatrix[(indexAtomInMol.get(a[2])).intValue()] = i;
        noeMatrix.addMethyl(a[0].x, a[0].y, a[0].z, a[1].x, a[1].y, a[1].z,
            a[2].x, a[2].y, a[2].z);
      }
    }
  }

  class DihedralCoupleNoCDK extends DihedralCouple {

    DihedralCoupleNoCDK(int numAtom1, int numAtom4) {
      String CHEquation = nmrPanel.coupleTable.CHequation;
      double[] theta_j = NMRCalculation.calc3J(viewer.getAtomAt(numAtom1),
          viewer.getAtomAt(numAtom4), CHEquation);
      if (theta_j == null) {
        jvalue = Double.NaN;
        return;
      }
      theta = theta_j[0];
      jvalue = theta_j[1];
    }

  }
}
