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

import java.awt.Rectangle;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

class FrankRenderer extends ShapeRenderer {

  private final static String frankString = "Jmol";
  private final static String frankFontName = "Serif";
  private final static int frankFontStyle = Font.BOLD;
  private final static int frankFontSize = 14;
  private final static int frankMargin = 4;
  Font frankFont;
  int frankWidth;
  int frankDescent;

  void render() {
    Frank frank = (Frank)shape;
    if (! frank.show)
      return;

    if (frankFont == null) {
      frankFont = new Font(frankFontName, frankFontStyle, frankFontSize);
      FontMetrics fm = g3d.getFontMetrics(frankFont);
      frankWidth = fm.stringWidth(frankString);
      frankDescent = fm.getDescent();
    }
    g3d.setFont(frankFont);
    g3d.drawString(frankString, frank.colix,
                   g3d.width - frankWidth - frankMargin,
                   g3d.height - frankDescent - frankMargin,
                   0);
  }
}
