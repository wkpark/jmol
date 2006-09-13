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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import org.jmol.g3d.*;
import javax.vecmath.*;

class VectorsRenderer extends ShapeRenderer {

  void render() {
    if (! frame.hasVibrationVectors)
      return;
    Atom[] atoms = frame.atoms;
    Vectors vectors = (Vectors)shape;
    short[] mads = vectors.mads;
    if (mads == null)
      return;
    short[] colixes = vectors.colixes;
    for (int i = frame.atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      //Logger.debug("vector render"+atom.shapeVisibilityFlags + " " + vectors.myVisibilityFlag);
      if ((atom.shapeVisibilityFlags & JmolConstants.ATOM_IN_MODEL) ==0
          || (atom.shapeVisibilityFlags & vectors.myVisibilityFlag) ==0)
        continue;
      Vector3f vibrationVector = atom.getVibrationVector();
      if (vibrationVector == null)
        continue;
      if (transform(mads[i], atom, vibrationVector))
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

  boolean transform(short mad, Atom atom, Vector3f vibrationVector) {
    if (atom.madAtom == JmolConstants.MAR_DELETED)
      return false;

    // to have the vectors stay in the the same spot
    /*
    float vectorScale = viewer.getVectorScale();
    pointVectorEnd.scaleAdd(vectorScale, atom.vibrationVector, atom.point3f);
    viewer.transformPoint(pointVectorEnd, screenVectorEnd);
    diameter = (mad <= 20)
      ? mad
      : viewer.scaleToScreen(screenVectorEnd.z, mad);
    pointArrowHead.scaleAdd(vectorScale * arrowHeadBase,
                            atom.vibrationVector, atom.point3f);
    viewer.transformPoint(pointArrowHead, screenArrowHead);
    headWidthPixels = diameter * 3 / 2;
    if (headWidthPixels < diameter + 2)
      headWidthPixels = diameter + 2;
    return true;
    */

    // to have the vectors move when vibration is turned on
    float vectorScale = viewer.getVectorScale();
    pointVectorEnd.scaleAdd(vectorScale, vibrationVector, atom);
    viewer.transformPoint(pointVectorEnd, vibrationVector,
                          screenVectorEnd);
    diameter = (mad <= 20)
      ? mad
      : viewer.scaleToScreen(screenVectorEnd.z, mad);
    pointArrowHead.scaleAdd(vectorScale * arrowHeadBase,
                            vibrationVector, atom);
    viewer.transformPoint(pointArrowHead, vibrationVector,
                          screenArrowHead);
    headWidthPixels = diameter * 3 / 2;
    if (headWidthPixels < diameter + 2)
      headWidthPixels = diameter + 2;
    return true;
  }
  
  void renderVector(short colix, Atom atom) {
    colix = Graphics3D.inheritColix(colix, atom.colixAtom);
    g3d.fillCylinder(colix, Graphics3D.ENDCAPS_OPEN, diameter,
                 atom.getScreenX(), atom.getScreenY(), atom.getScreenZ(),
                 screenArrowHead.x, screenArrowHead.y, screenArrowHead.z);
    g3d.fillCone(colix, Graphics3D.ENDCAPS_FLAT, headWidthPixels,
                 screenArrowHead, screenVectorEnd);
  }
}
