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

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.g3d.Colix;
import org.openscience.jmol.viewer.protein.*;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;

public class Strands {

  JmolViewer viewer;
  Frame frame;
  boolean hasPdbRecords;
  PdbMolecule pdbMolecule;

  boolean initialized;
  int chainCount;
  short[][] madsChains;
  short[][] colixesChains;
  Point3f[][] centersChains;
  Vector3f[][] vectorsChains;

  /****************************************************************
   * M. Carson and C.E. Bugg (1986)
   * Algorithm for Ribbon Models of Proteins. J.Mol.Graphics 4:121-122.
   * http://sgce.cbse.uab.edu/carson/papers/ribbons86/ribbons86.html
   ****************************************************************/
  Vector3f vectorA = new Vector3f();
  Vector3f vectorB = new Vector3f();
  Vector3f vectorC = new Vector3f();
  Vector3f vectorD = new Vector3f();
  

  Strands(JmolViewer viewer, Frame frame) {
    this.viewer = viewer;
    this.frame = frame;
    hasPdbRecords = frame.hasPdbRecords;
    pdbMolecule = frame.pdbMolecule;
  }

  public void setMad(short mad, BitSet bsSelected) {
    if (! hasPdbRecords)
      return;
    initialize();
    for (int i = pdbMolecule.getChainCount(); --i >= 0; ) {
      short[] mads = madsChains[i];
      if (mads == null)
        continue;
      PdbResidue[] mainchain = pdbMolecule.getMainchain(i);
      for (int j = mainchain.length; --j >= 0; ) {
        if (bsSelected.get(mainchain[j].getAlphaCarbonIndex()))
          if (mad < 0) {
            mads[j] = (short)(mainchain[j].isHelixOrSheet() ? 1500 : 500);
          } else {
            mads[j] = mad;
          }
      }
      // the last one in the chain needs to set the size for the following point
      mads[mainchain.length] = mads[mainchain.length-1];
    }
  }

  public void setColix(byte palette, short colix, BitSet bsSelected) {
    if (! hasPdbRecords)
      return;
    initialize();
    for (int i = pdbMolecule.getChainCount(); --i >= 0; ) {
      short[] colixes = colixesChains[i];
      if (colixes == null)
        continue;
      PdbResidue[] mainchain = pdbMolecule.getMainchain(i);
      for (int j = mainchain.length; --j >= 0; ) {
        int atomIndex = mainchain[j].getAlphaCarbonIndex();
        if (bsSelected.get(atomIndex))
          colixes[j] =
            (colix == 0 ? viewer.getColixAtomPalette(frame.getAtomAt(atomIndex), palette) : colix);
      }
    }
  }

  void initialize() {
    if (! initialized) {
      chainCount = pdbMolecule.getChainCount();
      madsChains = new short[chainCount][];
      colixesChains = new short[chainCount][];
      centersChains = new Point3f[chainCount][];
      vectorsChains = new Vector3f[chainCount][];
      for (int i = chainCount; --i >= 0; ) {
        int chainLength = pdbMolecule.getMainchain(i).length;
        System.out.println("chainLength=" + chainLength);
        if (chainLength > 1) {
          colixesChains[i] = new short[chainLength];
          madsChains[i] = new short[chainLength + 1];
          centersChains[i] = new Point3f[chainLength + 1];
          vectorsChains[i] = new Vector3f[chainLength + 1];
          calcCentersAndVectors(pdbMolecule.getMainchain(i),
                                centersChains[i], vectorsChains[i]);
        }
      }
      initialized = true;
    }
  }

  void calcCentersAndVectors(PdbResidue[] mainchain,
                             Point3f[] centers, Vector3f[] vectors) {
    Point3f alphaPointPrev, alphaPoint;
    centers[0] = alphaPointPrev = alphaPoint =
      mainchain[0].getAlphaCarbonAtom().point3f;
    int lastIndex = mainchain.length - 1;
    Vector3f previousVectorD = null;
    for (int i = 1; i <= lastIndex; ++i) {
      alphaPointPrev = alphaPoint;
      alphaPoint = mainchain[i].getAlphaCarbonAtom().point3f;
      Point3f center = new Point3f(alphaPoint);
      center.add(alphaPointPrev);
      center.scale(0.5f);
      centers[i] = center;
      vectorA.sub(alphaPoint, alphaPointPrev);
      vectorB.sub(mainchain[i-1].getCarbonylOxygenAtom().point3f, alphaPointPrev);
      vectorC.cross(vectorA, vectorB);
      vectorD.cross(vectorC, vectorA);
      vectorD.normalize();
      if (previousVectorD != null &&
          previousVectorD.angle(vectorD) > Math.PI/2)
        vectorD.scale(-1);
      previousVectorD = vectors[i] = new Vector3f(vectorD);
    }
    centers[mainchain.length] = alphaPointPrev;
    vectors[0] = vectors[1];
    vectors[mainchain.length] = vectors[lastIndex];
  }
}
