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
import org.openscience.jmol.viewer.g3d.Graphics3D;

import java.awt.Font;
import java.awt.FontMetrics;


class LabelsRenderer extends ShapeRenderer {

  short colixLabel;
  boolean isLabelAtomColor;
  // offsets are from the font baseline
  int labelOffsetX;
  int labelOffsetY;
  Font labelFont;
  FontMetrics labelFontMetrics;
  int labelFontAscent;

  void render() {
    colixLabel = viewer.getColixShape(JmolConstants.SHAPE_LABELS);
    isLabelAtomColor = colixLabel == 0;
    labelOffsetX = viewer.getLabelOffsetX();
    labelOffsetY = viewer.getLabelOffsetY();
    labelFont = viewer.getLabelFont();
    labelFontMetrics = g3d.getFontMetrics(labelFont);
    labelFontAscent = labelFontMetrics.getAscent();

    g3d.setFont(labelFont);

    Labels labels = (Labels)shape;
    String[] labelStrings = labels.labelStrings;
    if (labelStrings == null)
      return;
    Atom[] atoms = frame.atoms;
    int displayModel = this.displayModel;
    for (int i = labelStrings.length; --i >= 0; ) {
      String label = labelStrings[i];
      if (label == null)
        continue;
      Atom atom = atoms[i];
      if (displayModel != 0 && atom.modelNumber != displayModel)
        continue;
      renderLabel(atom, label);
    }
  }
  
  void renderLabel(Atom atom, String strLabel) {
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
