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

class BallsRenderer extends ShapeRenderer {

  int minX, maxX, minY, maxY;
  boolean wireframeRotating;
  boolean showHydrogens;
  short colixSelection;
  short colixLabel;
  boolean isLabelAtomColor;
  // offsets are from the font baseline
  int labelOffsetX;
  int labelOffsetY;
  Font labelFont;
  FontMetrics labelFontMetrics;
  int labelFontAscent;

  void render() {
    minX = rectClip.x;
    maxX = minX + rectClip.width;
    minY = rectClip.y;
    maxY = minY + rectClip.height;

    wireframeRotating = viewer.getWireframeRotating();
    colixSelection = viewer.getColixSelection();
    showHydrogens = viewer.getShowHydrogens();
    colixLabel = viewer.getColixShape(JmolConstants.SHAPE_LABELS);
    isLabelAtomColor = colixLabel == 0;
    labelOffsetX = viewer.getLabelOffsetX();
    labelOffsetY = viewer.getLabelOffsetY();
    labelFont = viewer.getLabelFont();
    labelFontMetrics = g3d.getFontMetrics(labelFont);
    labelFontAscent = labelFontMetrics.getAscent();

    g3d.setFont(labelFont);

    Atom[] atoms = frame.atoms;
    int displayModel = this.displayModel;
    if (displayModel == 0) {
      for (int i = frame.atomCount; --i >= 0; ) {
        Atom atom = atoms[i];
        atom.transform(viewer);
        render(atom);
        if (atom.strLabel != null)
          renderLabel(atom);
      }
    } else {
      for (int i = frame.atomCount; --i >= 0; ) {
        Atom atom = atoms[i];
        if (atom.modelNumber != displayModel)
          continue;
        atom.transform(viewer);
        render(atom);
        if (atom.strLabel != null)
          renderLabel(atom);
      }
    }
  }

  void render(Atom atom) {
    if (!showHydrogens && atom.elementNumber == 1)
      return;
    boolean hasHalo = viewer.hasSelectionHalo(atom);
    int diameter = atom.diameter;
    if (diameter == 0 && !hasHalo) {
      atom.chargeAndFlags &= ~Atom.VISIBLE_FLAG;
      return;
    }
    atom.chargeAndFlags |= Atom.VISIBLE_FLAG;
    int effectiveDiameter = diameter;
    if (hasHalo) {
      int halowidth = diameter / 4;
      if (halowidth < 4) halowidth = 4;
      if (halowidth > 10) halowidth = 10;
      effectiveDiameter = diameter + 2 * halowidth;
    }
    int effectiveRadius = (effectiveDiameter + 1) / 2;
    int x = atom.x;
    int y = atom.y;
    int z = atom.z;
    if (x + effectiveRadius < minX ||
        x - effectiveRadius >= maxX ||
        y + effectiveRadius < minY ||
        y - effectiveRadius >= maxY)
      return;

    if (!wireframeRotating)
      g3d.fillSphereCentered(atom.colixAtom, diameter, x, y, z);
    else
      g3d.drawCircleCentered(atom.colixAtom, diameter, x, y, z);

    if (viewer.hasSelectionHalo(atom))
      g3d.fillScreenedCircleCentered(colixSelection,
                                     effectiveDiameter, x, y, z);
  }

  void renderLabel(Atom atom) {
    String strLabel = atom.strLabel;
    /*
      left over from when font sizes changed;
    Font font = viewer.getLabelFont(atom.diameter);
    if (font == null)
      return;
    g3d.setFont(font);
    */

    int xOffset, yOffset, zLabel;
    zLabel = atom.z - atom.diameter / 2 - 2;
    if (zLabel < 0) zLabel = 0;

    if (labelOffsetX > 0) {
      xOffset = labelOffsetX;
    } else {
      xOffset = -labelFontMetrics.stringWidth(strLabel);
      if (labelOffsetX == 0)
        xOffset /= 2;
      else
        xOffset += labelOffsetX;
    }

    if (labelOffsetY > 0) {
      yOffset = labelOffsetY;
    } else {
      yOffset = -labelFontAscent;
      if (labelOffsetY == 0)
        yOffset /= 2;
      else
        yOffset += labelOffsetY;
      ++yOffset;
    }
    g3d.drawString(strLabel,
                   isLabelAtomColor ? atom.colixAtom : colixLabel,
                   atom.x + xOffset, atom.y - yOffset, zLabel);
  }

}
