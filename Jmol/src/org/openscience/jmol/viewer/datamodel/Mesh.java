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
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;

class Mesh extends Mcps {

  Mcps.Mcpschain allocateMcpschain(PdbPolymer polymer) {
    return new Schain(polymer);
  }

  /* note that this code is exactly the same as
     strands and ribbons
     consolidate these things
  */

  class Schain extends Mcps.Mcpschain {
    Point3f[] centers;
    Vector3f[] vectors;

    Schain(PdbPolymer polymer) {
      super(polymer, -2, 1500, 500);
      if (polymerCount > 0) {
        centers = new Point3f[polymerCount + 1];
        vectors = new Vector3f[polymerCount + 1];
        calcCentersAndVectors(polymerCount, polymerGroups, centers, vectors);
      }
    }
  }

  Vector3f vectorA = new Vector3f();
  Vector3f vectorB = new Vector3f();
  Vector3f vectorC = new Vector3f();
  Vector3f vectorD = new Vector3f();
  
  void calcCentersAndVectors(int count, PdbGroup[] groups,
                             Point3f[] centers, Vector3f[] vectors) {
    Point3f alphaPointPrev, alphaPoint;
    centers[0] = alphaPointPrev = alphaPoint =
      groups[0].getAlphaCarbonAtom().point3f;
    int lastIndex = count - 1;
    Vector3f previousVectorD = null;
    for (int i = 1; i <= lastIndex; ++i) {
      alphaPointPrev = alphaPoint;
      alphaPoint = groups[i].getAlphaCarbonAtom().point3f;
      Point3f center = new Point3f(alphaPoint);
      center.add(alphaPointPrev);
      center.scale(0.5f);
      centers[i] = center;
      vectorA.sub(alphaPoint, alphaPointPrev);
      vectorB.sub(groups[i-1].getCarbonylOxygenAtom().point3f,
                  alphaPointPrev);
      vectorC.cross(vectorA, vectorB);
      vectorD.cross(vectorC, vectorA);
      vectorD.normalize();
      if (previousVectorD != null &&
          previousVectorD.angle(vectorD) > Math.PI/2)
        vectorD.scale(-1);
      previousVectorD = vectors[i] = new Vector3f(vectorD);
    }
    centers[count] = alphaPointPrev;
    vectors[0] = vectors[1];
    vectors[count] = vectors[lastIndex];
  }
}
