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
import org.jmol.g3d.*;
import java.awt.FontMetrics;

public class Frank extends SelectionIndependentShape {

  final static String frankString = "Jmol";
  final static String frankFontName = "Serif";
  final static String frankFontStyle = "Bold";
  final static int frankFontSize = 14;
  final static int frankMargin = 4;

  Font3D font3d;
  int frankWidth;
  int frankAscent;
  int frankDescent;


  void initShape() {
    colix = Graphics3D.GRAY;

    
    font3d = g3d.getFont3D(frankFontName, frankFontStyle, frankFontSize);
    FontMetrics fm = font3d.fontMetrics;
    frankWidth = fm.stringWidth(frankString);
    frankDescent = fm.getDescent();
    frankAscent = fm.getAscent();
  }

  boolean wasClicked(int x, int y) {
    return (g3d.width > 0 &&
            g3d.height > 0 &&
            x > g3d.width - frankWidth - frankMargin &&
            y > g3d.height - frankAscent - frankMargin);
  }
}
