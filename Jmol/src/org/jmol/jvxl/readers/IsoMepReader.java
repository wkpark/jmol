/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.quantum.MepCalculation;
import org.jmol.util.Logger;
import org.jmol.viewer.Atom;

class IsoMepReader extends VolumeDataReader {

  IsoMepReader(SurfaceGenerator sg) {
    super(sg);
    precalculateVoxelData = true;
  }
    
  /////// molecular electrostatic potential ///////

  Atom[] mep_atoms;

  void setup() {
    Atom[] atoms = viewer.getFrame().atoms;
    Point3f xyzMin = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE,
        Float.MAX_VALUE);
    Point3f xyzMax = new Point3f(-Float.MAX_VALUE, -Float.MAX_VALUE,
        -Float.MAX_VALUE);
    int iAtom = 0;
    int nSelected = 0;
    params.iUseBitSets = true;
    int nAtoms = viewer.getAtomCount();
    int modelIndex = params.modelIndex;
    for (int i = 0; i < nAtoms; i++)
      if (atoms[i].getModelIndex() == modelIndex) {
        ++iAtom;
        if (params.bsSelected == null || params.bsSelected.get(i))
          ++nSelected;
      }
    int mep_nAtoms = iAtom;
    if (nSelected > 0)
      Logger.info(nSelected + " of " + mep_nAtoms
          + " atoms will be used in the mep calculation");
    if (mep_nAtoms > 0)
      mep_atoms = new Atom[mep_nAtoms];
    iAtom = 0;
    for (int i = 0; i < nAtoms; i++) {
      Atom atom = atoms[i];
      if (atom.getModelIndex() != modelIndex)
        continue;
      Point3f pt = new Point3f(atom);
      if (params.bsSelected == null || params.bsSelected.get(i)) {
        float rA = atom.getVanderwaalsRadiusFloat() + params.mep_marginAngstroms;
        if (pt.x - rA < xyzMin.x)
          xyzMin.x = pt.x - rA;
        if (pt.x + rA > xyzMax.x)
          xyzMax.x = pt.x + rA;
        if (pt.y - rA < xyzMin.y)
          xyzMin.y = pt.y - rA;
        if (pt.y + rA > xyzMax.y)
          xyzMax.y = pt.y + rA;
        if (pt.z - rA < xyzMin.z)
          xyzMin.z = pt.z - rA;
        if (pt.z + rA > xyzMax.z)
          xyzMax.z = pt.z + rA;
        mep_atoms[iAtom++] = atom;
      } else {
        ++iAtom;
      }
    }
    if (!Float.isNaN(params.scale)) {
      Vector3f v = new Vector3f(xyzMax);
      v.sub(xyzMin);
      v.scale(0.5f);
      xyzMin.add(v);
      v.scale(params.scale);
      xyzMax.set(xyzMin);
      xyzMax.add(v);
      xyzMin.sub(v);
    }

    Logger.info("MEP range bohr " + xyzMin + " to " + xyzMax);
    jvxlFileHeaderBuffer = new StringBuffer();
    jvxlFileHeaderBuffer.append("MEP\n range bohr ").append(xyzMin).append(" to ").append(xyzMax).append("\n");
    setRangesAndAddAtoms(xyzMin, xyzMax, params.mep_ptsPerAngstrom, params.mep_gridMax, 
        atoms, iAtom, nAtoms, modelIndex);
  }

  protected void generateCube() {
    float[] origin = { volumetricOrigin.x, volumetricOrigin.y,
        volumetricOrigin.z };
    MepCalculation m = new MepCalculation(mep_atoms, params.theProperty);
    m.createMepCube(voxelData, voxelCounts, origin, volumeData.volumetricVectorLengths);
  }
}
