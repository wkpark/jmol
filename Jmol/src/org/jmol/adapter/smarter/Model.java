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

  Atom newAtom() {
    Atom atom = new Atom();
    addAtom(atom);
    return atom;
  }
  
  void addAtom(Atom atom) {
    if (atomCount == atoms.length) {
      Atom[] t = new Atom[atomCount + 512];
      System.arraycopy(atoms, 0, t, 0, atomCount);
      atoms = t;
    }
    atoms[atomCount++] = atom;
  }

  void addBond(Bond bond) {
    if (bondCount == bonds.length) {
      Bond[] t = new Bond[bondCount + 1024];
      System.arraycopy(bonds, 0, t, 0, bondCount);
      bonds = t;
    }
    bonds[bondCount++] = bond;
  }

  void addStructure(Structure structure) {
    if (structureCount == structures.length) {
      Structure[] t = new Structure[structureCount + 32];
      System.arraycopy(structures, 0, t, 0, structureCount);
      structures = t;
    }
    structures[structureCount++] = structure;
  }

  void setModelName(String modelName) {
    if (modelName != null) {
      modelName.trim();
      if (modelName.length() > 0)
        this.modelName = modelName;
    }
  }
}
