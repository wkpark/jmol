/*
 * Copyright 2002 The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol;

public class SelectionSet {

  private IntSet pickedAtoms = new IntSet();

  public IntSet getPickedAtoms() {
    return pickedAtoms;
  }
  
  public void addPickedAtom(Atom atom) {
    pickedAtoms.add(atom.getAtomNumber());
  }
  
  public void addPickedAtoms(Atom[] atoms) {
    for (int i = 0; i < atoms.length; ++i) {
      pickedAtoms.add(atoms[i].getAtomNumber());
    }
  }
  
  public void removePickedAtom(Atom atom) {
    pickedAtoms.remove(atom.getAtomNumber());
  }
  
  public void removePickedAtoms(Atom[] atoms) {
    for (int i = 0; i < atoms.length; ++i) {
      pickedAtoms.remove(atoms[i].getAtomNumber());
    }
  }
  
  public void clearPickedAtoms() {
    pickedAtoms.clear();
  }

  public int countPickedAtoms() {
    return pickedAtoms.size();
  }

  public boolean isAtomPicked(Atom atom) {
    return pickedAtoms.contains(atom.getAtomNumber());
  }
}

