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

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import java.awt.Rectangle;

class AtomRenderer extends Renderer {

  AtomRenderer(JmolViewer viewer) {
    this.viewer = viewer;
  }

  int minX, maxX, minY, maxY;
  boolean wireframeRotating;
  boolean showHydrogens;
  short colixSelection;


  void setGraphicsContext(Graphics3D g3d, Rectangle rectClip,
                                 JmolFrame frame) {
    this.g3d = g3d;
    this.rectClip = rectClip;
    this.frame = frame;

    minX = rectClip.x;
    maxX = minX + rectClip.width;
    minY = rectClip.y;
    maxY = minY + rectClip.height;

    wireframeRotating = viewer.getWireframeRotating();
    colixSelection = viewer.getColixSelection();
    showHydrogens = viewer.getShowHydrogens();
  }

  void transform(Object objAtomShapes) {
    AtomShape[] atomShapes = (AtomShape[])objAtomShapes;
    for (int i = frame.atomShapeCount; --i >= 0; )
      atomShapes[i].transform(viewer);
  }

  void render(Object objAtomShapes) {
    AtomShape[] atomShapes = (AtomShape[])objAtomShapes;
    for (int i = frame.atomShapeCount; --i >= 0; )
      render(atomShapes[i]);
  }

  void render(AtomShape atomShape) {
    if (!showHydrogens && atomShape.atomicNumber == 1)
      return;
    byte styleAtom = atomShape.styleAtom;
    if (styleAtom <= JmolViewer.NONE)
      return;
    int x = atomShape.x;
    int y = atomShape.y;
    int z = atomShape.z;
    int diameter = atomShape.diameter;
    int radius = (diameter + 1) / 2;
    if (x + radius < minX ||
        x - radius >= maxX ||
        y + radius < minY ||
        y - radius >= maxY)
      return;

    if (styleAtom == JmolViewer.SHADED && !wireframeRotating)
      g3d.fillSphereCentered(atomShape.colixAtom, diameter, x, y, z);
    else
      g3d.drawCircleCentered(atomShape.colixAtom, diameter, x, y, z);

    if (viewer.hasSelectionHalo(atomShape)) {
      int halowidth = diameter / 4;
      if (halowidth < 4) halowidth = 4;
      if (halowidth > 10) halowidth = 10;
      int halodiameter = diameter + 2 * halowidth;
      g3d.fillScreenedCircleCentered(colixSelection, halodiameter, x, y, z);
    }
  }
}
