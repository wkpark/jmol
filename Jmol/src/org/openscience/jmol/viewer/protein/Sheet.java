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
package org.openscience.jmol.viewer.protein;
import org.openscience.jmol.viewer.JmolConstants;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class Sheet extends PdbStructure {

  Sheet(PdbChain chain, int residueStart, int residueEnd) {
    super(chain, JmolConstants.SECONDARY_STRUCTURE_SHEET,
          residueStart, residueEnd);
  }

  void calcAxis() {
    if (axisA != null)
      return;
    chain.getResidueMidPoint(residueStart + 1, axisA = new Point3f());
    chain.getResidueMidPoint(residueEnd, axisB = new Point3f());

    axisUnitVector = new Vector3f();
    axisUnitVector.sub(axisB, axisA);
    axisUnitVector.normalize();

    Point3f tempA, tempB;
    chain.getResidueMidPoint(residueStart, tempA = new Point3f());
    projectOntoAxis(tempA);
    chain.getResidueMidPoint(residueEnd + 1, tempB = new Point3f());
    projectOntoAxis(tempB);
    axisA = tempA;
    axisB = tempB;
  }
  /*

    Vector3f vectorT = new Vector3f();
    float projectedLength;

    axisB = new Point3f();
    chain.getResidueMidPoint(residueEnd + 1, axisB);
    vectorT.sub(axisB, tempA);
    projectedLength = vectorT.dot(axisUnitVector);
    axisB.set(axisUnitVector);
    axisB.scaleAdd(projectedLength, tempA);

    axisA = new Point3f();
    chain.getResidueMidPoint(residueStart, axisA);
    vectorT.sub(axisA, tempB);
    projectedLength = vectorT.dot(axisUnitVector);
    axisA.set(axisUnitVector);
    axisA.scaleAdd(projectedLength, tempB);

  */
}
