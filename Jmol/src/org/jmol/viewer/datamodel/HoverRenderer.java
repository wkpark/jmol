/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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
package org.jmol.viewer.datamodel;

import org.jmol.viewer.*;
import org.jmol.g3d.*;
import java.awt.FontMetrics;

class HoverRenderer extends ShapeRenderer {

  void render() {
    Hover hover = (Hover)shape;
    if (hover.atomIndex == -1 || hover.labelFormat == null)
      return;
    Atom atom = frame.getAtomAt(hover.atomIndex);
    /*
    System.out.println("hover on atom:" + hover.atomIndex + " @ " +
                       atom.getScreenX() + "," + atom.getScreenY());
    */
    String msg = atom.formatLabel(hover.labelFormat);
    Font3D font3d = hover.font3d;
    FontMetrics fontMetrics = font3d.fontMetrics;
    int ascent = fontMetrics.getAscent();
    int descent = fontMetrics.getDescent();
    int msgHeight = ascent + descent;
    int msgWidth = fontMetrics.stringWidth(msg);
    short colixBackground = hover.colixBackground;
    short colixForeground = hover.colixForeground;
    int width = msgWidth + 8;
    int height = msgHeight + 8;
    int x = atom.getScreenX() + 4;
    int y = atom.getScreenY() - height - 4;

    int msgX = x + 4;
    int msgYBaseline = y + 4 + ascent;
    if (colixBackground != 0) {
      g3d.fillRect(colixBackground, x, y, 1, width, height);
      g3d.drawRect(colixForeground, x+1, y+1, 1, width - 2, height - 2);
    }
    g3d.drawString(msg, font3d, colixForeground, (short)0,
                   msgX, msgYBaseline, 0);
  }
}
