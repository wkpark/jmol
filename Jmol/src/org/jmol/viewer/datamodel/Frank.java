/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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
import org.jmol.g3d.*;
import java.awt.FontMetrics;

public class Frank extends SelectionIndependentShape {

  final static String frankString = "Jmol";
  final static String defaultFontName = "SansSerif";
  final static String defaultFontStyle = "Bold";
  final static short defaultFontColix = Graphics3D.GRAY;
  final static int defaultFontSize = 16;
  final static int frankMargin = 4;

  Font3D currentMetricsFont3d;
  int frankWidth;
  int frankAscent;
  int frankDescent;


  void initShape() {
    colix = defaultFontColix;
    font3d = g3d.getFont3D(defaultFontName, defaultFontStyle, defaultFontSize);
  }

  boolean wasClicked(int x, int y) {
    return (g3d.width > 0 &&
            g3d.height > 0 &&
            x > g3d.width - frankWidth - frankMargin &&
            y > g3d.height - frankAscent - frankMargin);
  }

  void calcMetrics() {
    if (font3d != currentMetricsFont3d) {
      currentMetricsFont3d = font3d;
      FontMetrics fm = font3d.fontMetrics;
      frankWidth = fm.stringWidth(frankString);
      frankDescent = fm.getDescent();
      frankAscent = fm.getAscent();
    }
  }
}
