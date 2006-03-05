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
 */

package org.jmol.viewer;

import org.jmol.g3d.*;
import java.awt.FontMetrics;

class LabelsRenderer extends ShapeRenderer {

  // offsets are from the font baseline
  byte fidPrevious;
  Font3D font3d;
  FontMetrics fontMetrics;
  int ascent;
  int descent;
  int msgHeight;
  int msgWidth;

  void render() {
    fidPrevious = 0;

    Labels labels = (Labels)shape;
    String[] labelStrings = labels.strings;
    short[] colixes = labels.colixes;
    short[] bgcolixes = labels.bgcolixes;
    byte[] fids = labels.fids;
    short[] offsets = labels.offsets;
    if (labelStrings == null)
      return;
    Atom[] atoms = frame.atoms;
    int myVisibilityFlag = labels.myVisibilityFlag;
    for (int i = labelStrings.length; --i >= 0; ) {
      Atom atom = atoms[i];
      if (!atom.isShapeVisible(myVisibilityFlag))
        continue;
      String label = labelStrings[i];
      short colix = (colixes == null || i >= colixes.length) ? 0 : colixes[i];
      short bgcolix =
        (bgcolixes == null || i >= bgcolixes.length) ? 0 : bgcolixes[i];
      byte fid =
        ((fids == null || i >= fids.length || fids[i] == 0)
         ? labels.defaultFont3D.fid
         : fids[i]);
      if (fid != fidPrevious) {
        g3d.setFont(fid);
        fidPrevious = fid;
        font3d = g3d.getFont3DCurrent();
        fontMetrics = font3d.fontMetrics;
        ascent = fontMetrics.getAscent();
        descent = fontMetrics.getDescent();
        msgHeight = ascent + descent;
      }
      short offset = offsets == null || i >= offsets.length ? 0 : offsets[i];
      int xOffset, yOffset;
      if (offset == 0) {
        xOffset = JmolConstants.LABEL_DEFAULT_X_OFFSET;
        yOffset = JmolConstants.LABEL_DEFAULT_Y_OFFSET;
      } else if (offset == Short.MIN_VALUE) {
        xOffset = yOffset = 0;
      } else {
        xOffset = offset >> 8;
        yOffset = (byte)(offset & 0xFF);
      }
      renderLabel(atom, label, colix, bgcolix, xOffset, yOffset);
    }
  }
  
  void renderLabel(Atom atom, String strLabel, short colix, short bgcolix,
                   int labelOffsetX, int labelOffsetY) {
    int msgWidth = fontMetrics.stringWidth(strLabel);
    int boxWidth = msgWidth + 8;
    int boxHeight = msgHeight + 8;

    int xBoxOffset, yBoxOffset, zBox;
    zBox = atom.getScreenZ() - atom.getScreenD() / 2 - 2;
    if (zBox < 1) zBox = 1;

    if (labelOffsetX > 0) {
      xBoxOffset = labelOffsetX;
    } else {
      xBoxOffset = -boxWidth;
      if (labelOffsetX == 0)
        xBoxOffset /= 2;
      else
        xBoxOffset += labelOffsetX;
    }

    if (labelOffsetY < 0) {
      yBoxOffset = labelOffsetY;
    } else {
      if (labelOffsetY == 0)
        yBoxOffset = boxHeight / 2 + 2;
      else
        yBoxOffset = boxHeight + labelOffsetY;
    }
    int xBox = atom.getScreenX() + xBoxOffset;
    int yBox = atom.getScreenY() - yBoxOffset;
    colix = Graphics3D.inheritColix(colix, atom.colixAtom);
    if (bgcolix != 0) {
      g3d.fillRect(bgcolix, xBox, yBox, zBox, boxWidth, boxHeight);
      g3d.drawRect(colix, xBox+1, yBox+1, zBox-1, boxWidth - 2, boxHeight - 2);
    }
    int msgX = xBox + 4;
    int msgYBaseline = yBox + 4 + ascent;
    g3d.drawString(strLabel, font3d, colix, msgX, msgYBaseline, zBox-1);
  }

}
