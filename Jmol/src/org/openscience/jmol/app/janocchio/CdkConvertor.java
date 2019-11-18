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
 *  This program is distributed in the hope that it will be useful, on an 'as is' basis, on an 'as is' basis,
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.openscience.jmol.app.janocchio;

import java.util.Hashtable;

import javajs.util.Lst;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Bond;
import org.openscience.cdk.exception.CDKException;

public class CdkConvertor {

  Lst<Object> va;
  Lst<Object> vb;

  NMR_Viewer viewer;

  public CdkConvertor() {
  }

  /**
   * Converts the currently visible atoms to a CDK AtomContainer.
   * 
   * @param viewer
   *        the NMRViewer object Relies on getProperty for conversion
   * @return CDK AtomContainer[]
   * @throws CDKException
   */
  @SuppressWarnings("unchecked")
  public AtomContainer convert(NMR_Viewer viewer) throws CDKException {
    this.viewer = viewer;

    va = (Lst<Object>) viewer.getProperty("Object", "atomInfo", "all");
    vb = (Lst<Object>) viewer.getProperty("Object", "bondInfo", "all");

    int mn = viewer.getDisplayModelIndex();
    // If all frames are displayed mn is set to -1
    // For now, let the first frame be converted and used for calculations 
    if (mn < 0) {
      mn = 0;
    }

    AtomContainer atomContainer = getAtomContainer(mn);
    return atomContainer;

  }

  /**
   * Converts the atoms in all models to an array of CDK AtomContainers.
   * 
   * @param viewer
   * @return CDK AtomContainer[]
   * @throws CDKException
   * 
   */
  @SuppressWarnings("unchecked")
  public AtomContainer[] convertAll(NMR_Viewer viewer) throws CDKException {
    this.viewer = viewer;
    AtomContainer[] atomContainers;

    va = (Lst<Object>) viewer.getProperty("Object", "atomInfo", "all");
    vb = (Lst<Object>) viewer.getProperty("Object", "bondInfo", "all");

    int nmodel = viewer.getModelCount();

    atomContainers = new AtomContainer[nmodel];

    for (int j = 0; j < nmodel; j++) {

      AtomContainer ac = getAtomContainer(j);
      atomContainers[j] = ac;
    }
    return atomContainers;
  }

  @SuppressWarnings("unchecked")
  private AtomContainer getAtomContainer(int mn) {

    Hashtable<Integer, Atom> htMapIndexToAtoms = new Hashtable<Integer, Atom>();
    int nmodel = viewer.getModelCount();
    int natot = viewer.getAtomCount();

    int na;
    if (nmodel > 0) {
      na = natot / nmodel;
    } else {
      na = 0;
    }

    int nbtot = viewer.getBondCount();
    int nb;
    if (nmodel > 0) {
      nb = nbtot / nmodel;
    } else {
      nb = 0;
    }

    AtomContainer atomContainer = new AtomContainer();
    int minindex = 1;
    for (int i = 0; i < na; i++) {
      int index = mn * na + i;
      Hashtable<String, Object> atomi = (Hashtable<String, Object>) va
          .get(index);
      int k = ((Integer) atomi.get("atomno")).intValue();
      if (k < minindex) {
        minindex = k;
      }
      Atom atom = new Atom((String) atomi.get("sym"));
      atom.setX3d(((Float) atomi.get("x")).doubleValue());
      atom.setY3d(((Float) atomi.get("y")).doubleValue());
      atom.setZ3d(((Float) atomi.get("z")).doubleValue());
      htMapIndexToAtoms.put(new Integer(i), atom);
      atomContainer.addAtom(atom);
    }

    for (int i = 0; i < nb; i++) {
      int index = mn * nb + i;
      Hashtable<String, Object> b = (Hashtable<String, Object>) vb.get(index);
      Hashtable<String, Object> a1 = (Hashtable<String, Object>) b.get("atom1");
      Hashtable<String, Object> a2 = (Hashtable<String, Object>) b.get("atom2");
      int numa1 = ((Integer) a1.get("atomno")).intValue() - minindex;
      int numa2 = ((Integer) a2.get("atomno")).intValue() - minindex;

      float order = ((Float) b.get("order")).floatValue();
      // now, look up the uids in our atom map.
      Atom atom1 = htMapIndexToAtoms.get(new Integer(numa1));
      Atom atom2 = htMapIndexToAtoms.get(new Integer(numa2));
      Bond bond = new Bond(atom1, atom2, order);
      atomContainer.addBond(bond);
    }
    return atomContainer;
  }
}
