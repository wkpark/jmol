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
import java.awt.Font;
import java.awt.FontMetrics;

public class LabelRenderer {

  JmolViewer viewer;
  public LabelRenderer(JmolViewer viewer) {
    this.viewer = viewer;
  }

  Graphics3D g3d;
  Rectangle clip;

  public void setGraphicsContext(Graphics3D g3d, Rectangle clip) {
    this.g3d = g3d;
    this.clip = clip;
    colixLabel = viewer.getColixLabel();
    isLabelAtomColor = colixLabel == 0;
  }

  short colixLabel;
  boolean isLabelAtomColor;

  public void render(AtomShape atomShape) {
    String strLabel = atomShape.strLabel;
    if (strLabel == null)
      return;
    Font font = viewer.getLabelFont(atomShape.diameter);
    if (font == null)
      return;
    g3d.setFont(font);

    int zLabel = atomShape.z - atomShape.diameter/2 - 2;
    if (zLabel < 0) zLabel = 0;
    g3d.drawString(strLabel,
                   isLabelAtomColor ? atomShape.colixAtom : colixLabel,
                   atomShape.x + 4,
                   atomShape.y - 4,
                   zLabel
                   );
  }

  public void renderStringOffset(String label, short colix, int points,
                                 int x, int y, int z,
                                 int xOffset, int yOffset) {
    Font font = viewer.getFontOfSize(points);
    g3d.setFont(font);
    FontMetrics fontMetrics = g3d.getFontMetrics(font);
    int labelHeight = fontMetrics.getAscent();
    labelHeight -= 2; // this should not be necessary, but looks like it is;
    if (yOffset > 0)
      y += yOffset + labelHeight;
    else if (yOffset == 0)
      y += labelHeight / 2;
    else
      y += yOffset;
    if (xOffset > 0)
      x += xOffset;
    else if (xOffset == 0)
      x -= fontMetrics.stringWidth(label) / 2;
    else
      x += xOffset - fontMetrics.stringWidth(label);
    g3d.drawString(label, colix, x, y, z);
  }

  public void renderStringOutside(String label, short colix, int points,
                                  int x, int y, int z) {
    g3d.setColix(colix);
    Font font = viewer.getFontOfSize(points);
    g3d.setFont(font);
    FontMetrics fontMetrics = g3d.getFontMetrics(font);
    int labelAscent = fontMetrics.getAscent();
    int labelWidth = fontMetrics.stringWidth(label);
    int xLabelCenter, yLabelCenter;
    int xCenter = viewer.getBoundingBoxCenterX();
    int yCenter = viewer.getBoundingBoxCenterY();
    int dx = x - xCenter;
    int dy = y - yCenter;
    if (dx == 0 && dy == 0) {
      xLabelCenter = x;
      yLabelCenter = y;
    } else {
      int dist = (int) Math.sqrt(dx*dx + dy*dy);
      xLabelCenter = xCenter + ((dist + 2 + (labelWidth + 1) / 2) * dx / dist);
      yLabelCenter = yCenter + ((dist + 3 + (labelAscent + 1)/ 2) * dy / dist);
    }
    int xLabelBaseline = xLabelCenter - labelWidth / 2;
    int yLabelBaseline = yLabelCenter + labelAscent / 2;
    g3d.drawString(label, colix, xLabelBaseline, yLabelBaseline, z);
  }
}

