/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.adapter.smarter;
import org.jmol.api.ModelAdapter;
import java.util.Hashtable;

class Model {
  String modelTypeName;
  String modelName;

  int atomCount;
  Atom[] atoms = new Atom[256];
  int bondCount;
  Bond[] bonds = new Bond[256];
  int structureCount;
  Structure[] structures = new Structure[16];

  String errorMessage;
  String fileHeader;

  String spaceGroup;
  float wavelength = Float.NaN;
  boolean coordinatesAreFractional;
  float[] notionalUnitcell;
  float[] pdbScaleMatrix;
  float[] pdbScaleTranslate;

  int pdbStructureRecordCount;
  String[] pdbStructureRecords;

  Model(String modelTypeName) {
    this.modelTypeName = modelTypeName;
  }

  protected void finalize() {
    System.out.println("Model.finalize() called");
  }

  void finish() {
    atoms = null;
    bonds = null;
    notionalUnitcell = pdbScaleMatrix = pdbScaleTranslate = null;
    pdbStructureRecords = null;
  }

  void discardPreviousAtoms() {
    for (int i = atomCount; --i >= 0; )
      atoms[i] = null;
    atomCount = 0;
    atomNameMap.clear();
  }

  Atom newAtom() {
    Atom atom = new Atom();
    addAtom(atom);
    return atom;
  }

  void addAtom(Atom atom) {
    if (atomCount == atoms.length)
      atoms = (Atom[])ModelReader.setLength(atoms, atomCount + 512);
    atoms[atomCount++] = atom;
  }

  Bond newBond(int atomIndex1, int atomIndex2) {
    Bond bond = new Bond(atomIndex1, atomIndex2, 1);
    addBond(bond);
    return bond;
  }

  Bond newBond(String atomName1, String atomName2) {
    int atomIndex1 = getAtomNameIndex(atomName1);
    if (atomIndex1 < 0)
      return null;
    int atomIndex2 = getAtomNameIndex(atomName2);
    if (atomIndex2 < 0)
      return null;
    return newBond(atomIndex1, atomIndex2);
  }

  void addBond(Bond bond) {
    if (bondCount == bonds.length)
      bonds = (Bond[])ModelReader.setLength(bonds, bondCount + 1024);
    bonds[bondCount++] = bond;
  }

  void addStructure(Structure structure) {
    if (structureCount == structures.length)
      structures = (Structure[])ModelReader.setLength(structures,
                                                      structureCount + 32);
    structures[structureCount++] = structure;
  }

  void setModelName(String modelName) {
    if (modelName != null) {
      modelName.trim();
      if (modelName.length() > 0)
        this.modelName = modelName;
    }
  }

  Hashtable atomNameMap = new Hashtable();

  void mapAtomName(Atom atom) {
    if (atom.atomName != null && atoms[atomCount - 1] != atom)
      atomNameMap.put(atom.atomName, new Integer(atomCount - 1));
  }

  void mapAtomName(String atomName, int atomIndex) {
    atomNameMap.put(atomName, new Integer(atomIndex));
  }

  int getAtomNameIndex(String atomName) {
    Object value = atomNameMap.get(atomName);
    if (value == null)
      return -1;
    return ((Integer)value).intValue();
  }
}
