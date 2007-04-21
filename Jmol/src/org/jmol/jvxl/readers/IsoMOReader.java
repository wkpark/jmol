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

import java.util.Hashtable;
import java.util.Vector;
import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.quantum.QuantumCalculation;
import org.jmol.util.Logger;
import org.jmol.viewer.Atom;

class IsoMOReader extends VolumeDataReader {

  IsoMOReader(SurfaceGenerator sg) {
    super(sg);
    precalculateVoxelData = true;
  }
  
  /////// ab initio/semiempirical quantum mechanical orbitals ///////


  Atom[] qm_atoms;

  void setup() {
    for (int i = params.title.length; --i >= 0;)
      addMOTitleInfo(i, params.mo);
    int modelIndex = params.modelIndex;
    Atom[] atoms = viewer.getFrame().atoms;
    Point3f xyzMin = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE,
        Float.MAX_VALUE);
    Point3f xyzMax = new Point3f(-Float.MAX_VALUE, -Float.MAX_VALUE,
        -Float.MAX_VALUE);
    int iAtom = 0;
    int nSelected = 0;
    int nAtoms = viewer.getAtomCount();
    BitSet bsSelected = params.bsSelected;
    for (int i = 0; i < nAtoms; i++)
      if (atoms[i].getModelIndex() == modelIndex) {
        ++iAtom;
        if (bsSelected == null || bsSelected.get(i))
          ++nSelected;
      }
    params.qm_nAtoms = iAtom;
    if (nSelected > 0)
      Logger.info(nSelected + " of " + params.qm_nAtoms
          + " atoms will be used in the orbital calculation");
    if (params.qm_nAtoms > 0)
      qm_atoms = new Atom[params.qm_nAtoms];
    iAtom = 0;
    for (int i = 0; i < nAtoms; i++) {
      Atom atom = atoms[i];
      if (atom.getModelIndex() != modelIndex)
        continue;
      Point3f pt = new Point3f(atom);
      if (nSelected == 0 || bsSelected == null || bsSelected.get(i)) {
        float rA = atom.getVanderwaalsRadiusFloat() + params.qm_marginAngstroms;
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
        qm_atoms[iAtom++] = atom;
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

    Logger.info("MO range bohr " + xyzMin + " to " + xyzMax);
    jvxlFileHeaderBuffer = new StringBuffer();
    jvxlFileHeaderBuffer.append("MO range bohr ").append(xyzMin).append(" to ").append(xyzMax).
                   append("\ncalculation type: ").append(params.moData.get("calculationType")).append("\n");
    setRangesAndAddAtoms(xyzMin, xyzMax, params.qm_ptsPerAngstrom, params.qm_gridMax, 
        atoms, iAtom, nAtoms, modelIndex);
  }
  
  void addMOTitleInfo(int iLine, Hashtable mo) {
    String line = params.title[iLine];
    int pt = line.indexOf("%");
    if (line.length() == 0 || pt < 0)
      return;
    boolean replaced = false;
    for (int i = pt; i < line.length() - 1; i++) {
      if (line.charAt(i) == '%') {
        String info = "";
        switch (line.charAt(i + 1)) {
        case 'F':
          info = viewer.getFileName();
          break;
        case 'I':
          info += params.qm_moNumber;
          break;
        case 'N':
          info += params.qmOrbitalCount;
          break;
        case 'M':
          info += viewer.getModelNumberDotted(params.modelIndex);
          break;
        case 'E':
          info += mo.get("energy");
          break;
        case 'U':
          if (params.moData.containsKey("energyUnits"))
            info += params.moData.get("energyUnits");
          break;
        case 'S':
          if (mo.containsKey("symmetry"))
            info += mo.get("symmetry");
          break;
        case 'O':
          if (mo.containsKey("occupancy"))
            info += mo.get("occupancy");
          break;
        }
        replaced |= (info.length() > 0);
        line = line.substring(0, i) + info + line.substring(i + 2);
        i += info.length();
      }
    }
    line = (replaced || line.charAt(0) != '?' ? line : "");
    params.title[iLine] = (line.length() > 1 && line.charAt(0) == '?' ? line
        .substring(1) : line);
  }
  
  protected void generateCube() {
    QuantumCalculation q;
    Hashtable moData = params.moData;
    float[] origin = { volumetricOrigin.x, volumetricOrigin.y,
        volumetricOrigin.z };
    switch (params.qmOrbitalType) {
    case Parameters.QM_TYPE_GAUSSIAN:
      q = new QuantumCalculation((String) moData.get("calculationType"),
          qm_atoms, (Vector) moData.get("shells"), (float[][]) moData
              .get("gaussians"), (Hashtable) moData.get("atomicOrbitalOrder"),
          null, null, params.moCoefficients);
      q.createGaussianCube(voxelData, voxelCounts, origin,
          volumeData.volumetricVectorLengths);
      break;
    case Parameters.QM_TYPE_SLATER:
      q = new QuantumCalculation((String) moData.get("calculationType"),
          qm_atoms, (Vector) moData.get("shells"), null, null, (int[][]) moData
              .get("slaterInfo"), (float[][]) moData.get("slaterData"),
          params.moCoefficients);
      q.createSlaterCube(voxelData, voxelCounts, origin,
          volumeData.volumetricVectorLengths);
      break;
    default:
    }
  }
}
