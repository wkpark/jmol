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
package org.openscience.jmol;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;

public class Java12 {

  DisplayControl control;

  public Java12(DisplayControl control) {
    this.control = control;
  }

  public void enableAntialiasing(Graphics g, boolean enableAntialiasing) {
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                         (enableAntialiasing
                          ? RenderingHints.VALUE_ANTIALIAS_ON
                          : RenderingHints.VALUE_ANTIALIAS_OFF));
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                         (enableAntialiasing
                          ? RenderingHints.VALUE_RENDER_QUALITY
                          : RenderingHints.VALUE_RENDER_SPEED));
  }

  private BasicStroke dottedStroke = new BasicStroke(1, BasicStroke.CAP_ROUND,
                                                     BasicStroke.JOIN_ROUND, 0,
                                                     new float[] {3, 3}, 0);
  public void dottedStroke(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    g2.setStroke(dottedStroke);
  }

  private BasicStroke defaultStroke = new BasicStroke();

  public void defaultStroke(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    g2.setStroke(defaultStroke);
  }
}
