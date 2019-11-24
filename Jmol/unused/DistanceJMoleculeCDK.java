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
import java.util.Vector;

import javajs.util.V3;

import org.jmol.quantum.NMRCalculation;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.AtomContainer;

/**
 * 
 * @author u6x8497
 */
public class DistanceJMoleculeCDK extends DistanceJMolecule {
  AtomContainer mol;

  public DistanceJMoleculeCDK() {
  }

  public DistanceJMoleculeCDK set(AtomContainer mol, String[] labelArray) {

    this.mol = mol;
    this.labelArray = labelArray;
    return this;
  }

  @Override
  protected int[] addAtomstoMatrix() {
    // Converts the CDK molecule to input for Gary Sharman's implementation of the NoeMatrix class
    Vector<Object> hAtoms = new Vector<Object>();
    Hashtable<Atom, String> labels = new Hashtable<Atom, String>();

    // Make vector of all hAtoms in molecule
    Hashtable<Atom, Integer> indexAtominMol = new Hashtable<Atom, Integer>();
    for (int i = 0; i < mol.getAtomCount(); i++) {
      Atom a = mol.getAtomAt(i);

      if (labelArray != null) {
        if (labelArray[i] != null) {
          labels.put(a, labelArray[i]);
        } else {
          labels.put(a, "");
        }
      }
      indexAtominMol.put(a, new Integer(i));

      if (a.getSymbol().equals("H")) {
        hAtoms.add(a);
      }
    }
    // Check every H atom to see if it is part of a methyl group
    // If is, the other two H atoms are removed from the hAtoms vector,
    // and the methyl group is added
    // If not, just the H atom is added
    int nHAtoms = hAtoms.size();
    int i = nHAtoms - 1;
    while (i >= 0) {

      Atom atom = (Atom) hAtoms.get(i);

      Atom c[] = mol.getConnectedAtoms(atom);

      Vector<?> others = mol.getConnectedAtomsVector(c[0]);
      boolean methyl = false;
      if (others.size() == 4) {

        for (int j = others.size() - 1; j >= 0; j--) {
          Atom atomj = (Atom) others.get(j);
          if ((!atomj.getSymbol().equals("H")) || atomj.compare(atom)) {
            others.remove(atomj);
          }
        }
        if (others.size() == 2) {
          // it is a methyl group
          methyl = true;
          //System.out.println(atom.toString());
          Atom atom1 = (Atom) others.get(0);
          Atom atom2 = (Atom) others.get(1);
          hAtoms.remove(atom1);
          hAtoms.remove(atom2);

          Atom[] tmpa = new Atom[3];
          tmpa[0] = atom;
          tmpa[1] = atom1;
          tmpa[2] = atom2;
          i = i - 2;
          hAtoms.set(i, tmpa);
        }
      }
      if (methyl) {
        i--;
        continue;
      }
      // Check for equivalent labels
      String label = labels.get(atom);
      Vector<Atom> equiv = new Vector<Atom>();
      equiv.add(atom);
      if (!(label == null || label.trim().length() == 0)) {

        int j = i - 1;
        while (j >= 0) {
          Atom jatom = (Atom) hAtoms.get(j);
          String jlabel = labels.get(jatom);
          if (jlabel.equals(label) && (!jatom.compare(atom))) {
            equiv.add(jatom);
            hAtoms.remove(jatom);
            i--;
            //j--;
          }
          j--;
        }
        if (equiv.size() > 1) {
          hAtoms.set(i, equiv);
        }
      }
      i--;
    }

    nHAtoms = hAtoms.size();
    noeMatrix.makeAtomList(nHAtoms);

    int[] indexAtomInNoeMatrix = new int[mol.getAtomCount()];

    for (i = 0; i < nHAtoms; i++) {
      Object aobj = hAtoms.get(i);
      if (aobj.getClass() == (new Atom[3]).getClass()) {
        Atom[] a = (Atom[]) aobj;

        indexAtomInNoeMatrix[(indexAtominMol.get(a[0])).intValue()] = i;
        indexAtomInNoeMatrix[(indexAtominMol.get(a[1])).intValue()] = i;
        indexAtomInNoeMatrix[(indexAtominMol.get(a[2])).intValue()] = i;

        noeMatrix.addMethyl(a[0].getX3d(), a[0].getY3d(), a[0].getZ3d(),
            a[1].getX3d(), a[1].getY3d(), a[1].getZ3d(), a[2].getX3d(),
            a[2].getY3d(), a[2].getZ3d());
      } else if (aobj.getClass() == (new Atom()).getClass()) {
        Atom a = (Atom) hAtoms.get(i);
        indexAtomInNoeMatrix[(indexAtominMol.get(a)).intValue()] = i;
        noeMatrix.addAtom(a.getX3d(), a.getY3d(), a.getZ3d());
      }
      //else if (aobj.getClass() == (new Vector()).getClass()) {
      else {
        Vector<?> a = (Vector<?>) aobj;
        for (int j = 0; j < a.size(); j++) {
          indexAtomInNoeMatrix[(indexAtominMol.get(a.get(j)))
              .intValue()] = i;
        }
        double[] xa = new double[a.size()];
        double[] ya = new double[a.size()];
        double[] za = new double[a.size()];
        for (int j = 0; j < a.size(); j++) {
          xa[j] = ((Atom) a.get(j)).getX3d();
          ya[j] = ((Atom) a.get(j)).getY3d();
          za[j] = ((Atom) a.get(j)).getZ3d();
        }
        noeMatrix.addEquiv(xa, ya, za);
      }
    }
    return indexAtomInNoeMatrix;

  }

  @Override
  protected DihedralCouple getDihedralCouple(int numAtom1,
                                             int numAtom2,
                                             int numAtom3,
                                             int numAtom4) {

    return new DihedralCoupleCDK(numAtom1, numAtom2, numAtom3, numAtom4);
  }

  class DihedralCoupleCDK extends DihedralCouple {

    private Atom atom1;
    private Atom atom2;
    private Atom atom3;
    private Atom atom4;

    private String[] elements;
    private String[][] subElements = new String[2][3];
    private V3[][] subVectors = new V3[2][3];
    


    // These vectors are named for a standard HCCH dihedral, as they
    // are applied to the Altona equation. But they are also used
    // for all other dihedrals
    private V3 v23;
    private V3 v21;
    private V3 v34;


    DihedralCoupleCDK(int numAtom1, int numAtom2,
        int numAtom3, int numAtom4) {

      atom1 = mol.getAtomAt(numAtom1);
      atom2 = mol.getAtomAt(numAtom2);
      atom3 = mol.getAtomAt(numAtom3);
      atom4 = mol.getAtomAt(numAtom4);
      
      elements = new String[] {
          atom1.getSymbol(), atom2.getSymbol(), atom3.getSymbol(), atom4.getSymbol() };      

      v21 = getVector3d(atom1, atom2);
      v23 = getVector3d(atom3, atom2);
      v34 = getVector3d(atom4, atom3);

      Vector<?> subs = mol.getConnectedAtomsVector(atom2);
      for (int pt = 0, i = Math.min(subs.size(), 4); --i >= 0;) {
        Atom sub = (Atom) subs.get(i);
        if (sub == atom3)
          continue;
        subElements[0][pt] = sub.getSymbol();
        subVectors[0][pt] = getVector3d(sub, atom2);
        pt++;
      }
      for (int pt = 0, i = Math.min(subs.size(), 4); --i >= 0;) {
        Atom sub = (Atom) subs.get(i);
        if (sub == atom2)
          continue;
        subElements[1][pt] = sub.getSymbol();
        subVectors[1][pt] = getVector3d(sub, atom3);
        pt++;
      }      
      theta = NMRCalculation.calcTheta(v21, v34, v23);
      jvalue = NMRCalculation.calcJ3(elements, subElements, subVectors, v21, v34, v23, theta, CHequation);
}

    /**
     * Get the vector from atom1 to atom2 (second param to first parameter)
     * @param atom2
     * @param atom1
     * @return vector atom1 to atom2
     */
    private V3 getVector3d(Atom atom2, Atom atom1) {
      V3 v = V3.new3(
          (float)(atom2.getX3d() - atom1.getX3d()), 
          (float)(atom2.getY3d() - atom1.getY3d()),
          (float)(atom2.getZ3d() - atom1.getZ3d()));
      return v;
    }

  }

}
