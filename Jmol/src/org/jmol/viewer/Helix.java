/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;

class Helix extends ProteinStructure {

  Helix(AlphaPolymer apolymer, int monomerIndex, int monomerCount) {
    super(apolymer, JmolConstants.PROTEIN_STRUCTURE_HELIX,
          monomerIndex, monomerCount);
  }

  void calcAxis() {
    if (axisA != null)
      return;
    Point3f[] points = new Point3f[monomerCount + 1];
    for (int i = 0; i <= monomerCount; i++) {
      points[i] = new Point3f();
      apolymer.getLeadMidPoint(monomerIndex + i, points[i]);
    }
    axisA = new Point3f();
    axisUnitVector = new Vector3f();
    Graphics3D.calcBestAxisThroughPoints(points, axisA, axisUnitVector, vectorProjection, 4);
    axisB = new Point3f(points[monomerCount]);
    Graphics3D.projectOntoAxis(axisB, axisA, axisUnitVector, vectorProjection);
  }

  /****************************************************************
   * see also: 
   * (not implemented -- I never got around to reading this -- BH)
   * Defining the Axis of a Helix
   * Peter C Kahn
   * Computers Chem. Vol 13, No 3, pp 185-189, 1989
   *
   * Simple Methods for Computing the Least Squares Line
   * in Three Dimensions
   * Peter C Kahn
   * Computers Chem. Vol 13, No 3, pp 191-195, 1989
   ****************************************************************/
}
