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

import org.openscience.jmol.viewer.*;
import org.jmol.g3d.*;
import javax.vecmath.*;

class VectorsRenderer extends ShapeRenderer {

  void render() {
    Atom[] atoms = frame.atoms;
    Vectors vectors = (Vectors)shape;
    short[] mads = vectors.mads;
    if (mads == null)
      return;
    short[] colixes = vectors.colixes;
    int displayModel = this.displayModel;
    for (int i = frame.atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      if (mads[i] == 0 ||
          atom.vibrationVector == null ||
          (displayModel != 0 && atom.modelNumber != displayModel))
        continue;
      if (transform(mads[i], atom))
        renderVector(colixes[i], atom);
    }
  }

  final Point3f pointVectorEnd = new Point3f();
  final Point3f pointArrowHead = new Point3f();
  final Point3i screenVectorEnd = new Point3i();
  final Point3i screenArrowHead = new Point3i();
  final Vector3f vibrationVectorScaled = new Vector3f();
  int diameter;
  float headWidthAngstroms;
  int headWidthPixels;

  final static float arrowHeadBase = 0.8f;

  boolean transform(short mad, Atom atom) {
    if (atom.madAtom == JmolConstants.MAR_DELETED)
      return false;

    Vector3f vibrationVector = atom.vibrationVector;
    pointVectorEnd.add(atom.point3f, vibrationVector);
    viewer.transformPoint(pointVectorEnd, vibrationVector, screenVectorEnd);
    diameter = (mad <= 20)
      ? mad
      : viewer.scaleToScreen(screenVectorEnd.z, mad);
    pointArrowHead.scaleAdd(arrowHeadBase, vibrationVector, atom.point3f);
    viewer.transformPoint(pointArrowHead, vibrationVector, screenArrowHead);
    headWidthPixels = diameter * 3 / 2;
    if (headWidthPixels < 3)
      headWidthPixels = 3;
    return true;
  }
  
  void renderVector(short colix, Atom atom) {
    if (colix == 0)
      colix = atom.colixAtom;
    g3d.fillCylinder(colix, Graphics3D.ENDCAPS_OPEN, diameter,
                 atom.getScreenX(), atom.getScreenY(), atom.getScreenZ(),
                 screenArrowHead.x, screenArrowHead.y, screenArrowHead.z);
    g3d.fillCone(colix, Graphics3D.ENDCAPS_NONE, headWidthPixels,
                 screenArrowHead, screenVectorEnd);
  }
}
