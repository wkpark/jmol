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

  // offsets are from the font baseline
  int labelOffsetX;
  int labelOffsetY;
  int fontSizePrevious = -1;
  Font labelFont;
  FontMetrics labelFontMetrics;
  int defaultFontSize;

  void render() {
    labelOffsetX = viewer.getLabelOffsetX();
    labelOffsetY = viewer.getLabelOffsetY();
    defaultFontSize = JmolConstants.LABEL_DEFAULT_FONTSIZE;

    fontSizePrevious = -1;

    Labels labels = (Labels)shape;
    String[] labelStrings = labels.strings;
    short[] colixes = labels.colixes;
    byte[] fontSizes = labels.fontSizes;
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
      short colix = colixes == null || i >= colixes.length ? 0 : colixes[i];
      int fontSize =
        ((fontSizes == null || i >= fontSizes.length || fontSizes[i] == 0)
         ? defaultFontSize
         : fontSizes[i]);
      if (fontSize != fontSizePrevious) {
        System.out.println("setting font to be of size " + fontSize);
        Font font = labelFont = viewer.getFontOfSize(fontSize);
        g3d.setFont(font);
        labelFontMetrics = g3d.getFontMetrics();
        fontSizePrevious = fontSize;
      }
      renderLabel(atom, label, colix);
    }
  }
  
  void renderLabel(Atom atom, String strLabel, short colix) {
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
      yOffset = -labelFontMetrics.getAscent();
      if (labelOffsetY == 0)
        yOffset /= 2;
      else
        yOffset += labelOffsetY;
      ++yOffset;
    }
    g3d.drawString(strLabel,
                   colix == 0 ? atom.colixAtom : colix,
                   atom.x + xOffset, atom.y - yOffset, zLabel);
  }

}
