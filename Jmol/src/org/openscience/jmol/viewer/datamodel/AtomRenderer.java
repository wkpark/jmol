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
import java.awt.Rectangle;

import java.awt.Font;
import java.awt.FontMetrics;

class AtomRenderer extends Renderer {

  AtomRenderer(JmolViewer viewer, FrameRenderer frameRenderer) {
    setViewerFrameRenderer(viewer, frameRenderer);
  }

  int minX, maxX, minY, maxY;
  boolean wireframeRotating;
  boolean showHydrogens;
  short colixSelection;
  short colixLabel;
  boolean isLabelAtomColor;


  void render() {
    minX = rectClip.x;
    maxX = minX + rectClip.width;
    minY = rectClip.y;
    maxY = minY + rectClip.height;

    wireframeRotating = viewer.getWireframeRotating();
    colixSelection = viewer.getColixSelection();
    showHydrogens = viewer.getShowHydrogens();
    colixLabel = viewer.getColixLabel();
    isLabelAtomColor = colixLabel == 0;

    Atom[] atoms = frame.atoms;
    for (int i = frame.atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      atom.transform(viewer);
      render(atom);
      if (atom.strLabel != null)
        renderLabel(atom);
    }
  }

  void render(Atom atom) {
    if (!showHydrogens && atom.atomicNumber == 1)
      return;
    int diameter = atom.diameter;
    if (diameter == 0)
      return;
    int radius = (diameter + 1) / 2;
    int x = atom.x;
    int y = atom.y;
    int z = atom.z;
    if (x + radius < minX ||
        x - radius >= maxX ||
        y + radius < minY ||
        y - radius >= maxY)
      return;

    if (!wireframeRotating)
      g3d.fillSphereCentered(atom.colixAtom, diameter, x, y, z);
    else
      g3d.drawCircleCentered(atom.colixAtom, diameter, x, y, z);

    if (viewer.hasSelectionHalo(atom)) {
      int halowidth = diameter / 4;
      if (halowidth < 4) halowidth = 4;
      if (halowidth > 10) halowidth = 10;
      int halodiameter = diameter + 2 * halowidth;
      g3d.fillScreenedCircleCentered(colixSelection, halodiameter, x, y, z);
    }
  }

  void renderLabel(Atom atom) {
    String strLabel = atom.strLabel;
    Font font = viewer.getLabelFont(atom.diameter);
    if (font == null)
      return;
    g3d.setFont(font);

    int zLabel = atom.z - atom.diameter/2 - 2;
    if (zLabel < 0) zLabel = 0;
    g3d.drawString(strLabel,
                   isLabelAtomColor ? atom.colixAtom : colixLabel,
                   atom.x + 4,
                   atom.y - 4,
                   zLabel
                   );
  }

}
