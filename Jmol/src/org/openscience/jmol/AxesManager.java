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

import org.openscience.jmol.render.Axes;
import java.awt.Color;

public class AxesManager {

  DisplayControl control;

  public Axes axes;

  public AxesManager(DisplayControl control) {
    this.control = control;

    axes = new Axes(control);
    Color co = control.getColorTransparent(Color.gray);
    setColorAxes(co);
    setColorAxesText(co);
  }

  public byte modeAxes = DisplayControl.AXES_NONE;
  public void setModeAxes(byte modeAxes) {
    this.modeAxes = modeAxes;
    recalc();
  }

  public void recalc() {
    axes.recalc(modeAxes);
  }

  public Color colorAxes;
  public void setColorAxes(Color colorAxes) {
    this.colorAxes = colorAxes;
  }

  public Color colorAxesText;
  public void setColorAxesText(Color colorAxesText) {
    this.colorAxesText = colorAxesText;
  }
}
