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
package org.openscience.jmol.viewer.datamodel;
import org.openscience.jmol.viewer.JmolConstants;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

class Sheet extends ProteinStructure {

  AminoPolymer aminopolymer;
  Sheet(AminoPolymer aminopolymer, int polymerIndex, int polymerCount) {
    super(aminopolymer, JmolConstants.PROTEIN_STRUCTURE_SHEET,
          polymerIndex, polymerCount);
    this.aminopolymer = aminopolymer;
  }

  void calcAxis() {
    if (axisA != null)
      return;
    if (polymerCount == 2) {
      axisA = aminopolymer.getResidueAlphaCarbonPoint(polymerIndex);
      axisB = aminopolymer.getResidueAlphaCarbonPoint(polymerIndex + 1);
    } else {
      axisA = new Point3f();
      aminopolymer.getLeadMidPoint(polymerIndex + 1, axisA);
      axisB = new Point3f();
      aminopolymer.getLeadMidPoint(polymerIndex + polymerCount - 1, axisB);
    }

    axisUnitVector = new Vector3f();
    axisUnitVector.sub(axisB, axisA);
    axisUnitVector.normalize();

    Point3f tempA = new Point3f();
    aminopolymer.getLeadMidPoint(polymerIndex, tempA);
    if (! lowerNeighborIsHelixOrSheet())
      projectOntoAxis(tempA);
    Point3f tempB = new Point3f();
    aminopolymer.getLeadMidPoint(polymerIndex + polymerCount, tempB);
    if (! upperNeighborIsHelixOrSheet())
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
      vectorCOSum.sub(aminopolymer.getResiduePoint(polymerIndex, 3),
                      aminopolymer.getResiduePoint(polymerIndex, 2));
      for (int i = polymerCount; --i > 0; ) {
        vectorCO.sub(aminopolymer.getResiduePoint(polymerIndex + i, 3),
                     aminopolymer.getResiduePoint(polymerIndex + i, 2));
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

  Vector3f getWidthUnitVector() {
    if (widthUnitVector == null)
      calcSheetUnitVectors();
    return widthUnitVector;
  }

  Vector3f getHeightUnitVector() {
    if (heightUnitVector == null)
      calcSheetUnitVectors();
    return heightUnitVector;
  }
}
