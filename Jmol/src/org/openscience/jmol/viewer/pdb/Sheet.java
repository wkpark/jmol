/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.viewer.pdb;
import org.openscience.jmol.viewer.JmolConstants;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class Sheet extends PdbStructure {

  Sheet(PdbChain chain, short startResidueID, int residueCount) {
    super(chain, JmolConstants.SECONDARY_STRUCTURE_SHEET,
          startResidueID, residueCount);
  }

  void calcAxis() {
    if (axisA != null)
      return;
    axisA = new Point3f();
    chain.getAlphaCarbonMidPoint(startResidueIndex + 1, axisA);
    axisB = new Point3f();
    chain.getAlphaCarbonMidPoint(endResidueIndex, axisB);

    axisUnitVector = new Vector3f();
    axisUnitVector.sub(axisB, axisA);
    axisUnitVector.normalize();

    Point3f tempA = new Point3f();
    chain.getAlphaCarbonMidPoint(startResidueIndex, tempA);
    projectOntoAxis(tempA);
    Point3f tempB = new Point3f();
    chain.getAlphaCarbonMidPoint(startResidueIndex + residueCount, tempB);
    projectOntoAxis(tempB);
    axisA = tempA;
    axisB = tempB;
  }

  Vector3f widthUnitVector;
  Vector3f heightUnitVector;
  
  void calcSheetUnitVectors() {
    if (widthUnitVector == null) {
      Vector3f vectorCO = new Vector3f();
      Vector3f vectorCOSum = new Vector3f();
      vectorCOSum.sub(chain.getResiduePoint(startResidueIndex, 3),
                      chain.getResiduePoint(startResidueIndex, 2));
      for (int i = residueCount; --i > 0; ) {
        vectorCO.sub(chain.getResiduePoint(startResidueIndex + i, 3),
                     chain.getResiduePoint(startResidueIndex + i, 2));
        if (vectorCOSum.angle(vectorCO) < (float)Math.PI/2)
          vectorCOSum.add(vectorCO);
        else
          vectorCOSum.sub(vectorCO);
      }
      heightUnitVector = vectorCO; // just reuse the same temp vector;
      heightUnitVector.cross(axisUnitVector, vectorCOSum);
      heightUnitVector.normalize();
      widthUnitVector = vectorCOSum;
      widthUnitVector.cross(axisUnitVector, heightUnitVector);
    }
  }

  public Vector3f getWidthUnitVector() {
    if (widthUnitVector == null)
      calcSheetUnitVectors();
    return widthUnitVector;
  }

  public Vector3f getHeightUnitVector() {
    if (heightUnitVector == null)
      calcSheetUnitVectors();
    return heightUnitVector;
  }

  /*

    Vector3f vectorT = new Vector3f();
    float projectedLength;

    axisB = new Point3f();
    chain.getAlphaCarbonMidPoint(residueEnd + 1, axisB);
    vectorT.sub(axisB, tempA);
    projectedLength = vectorT.dot(axisUnitVector);
    axisB.set(axisUnitVector);
    axisB.scaleAdd(projectedLength, tempA);

    axisA = new Point3f();
    chain.getAlphaCarbonMidPoint(residueStart, axisA);
    vectorT.sub(axisA, tempB);
    projectedLength = vectorT.dot(axisUnitVector);
    axisA.set(axisUnitVector);
    axisA.scaleAdd(projectedLength, tempB);

  */
}
