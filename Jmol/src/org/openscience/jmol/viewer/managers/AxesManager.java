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
package org.openscience.jmol.viewer.managers;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.Colix;

public class AxesManager {

  JmolViewer viewer;

  public AxesManager(JmolViewer viewer) {
    this.viewer = viewer;

    setColixAxes(Colix.LIGHTGRAY);
    setColixAxesText(Colix.LIGHTGRAY);
  }

  public byte modeAxes = JmolConstants.AXES_NONE;
  public void setModeAxes(byte modeAxes) {
    this.modeAxes = modeAxes;
    // for now, break axes so that it either shows or doesn't
    viewer.setGraphicShow(JmolConstants.GRAPHIC_AXES,
                          modeAxes != JmolConstants.AXES_NONE);
  }

  public boolean showBoundingBox = false;
  public void setShowBoundingBox(boolean showBoundingBox) {
    this.showBoundingBox = showBoundingBox;
    viewer.setGraphicShow(JmolConstants.GRAPHIC_BBOX, showBoundingBox);
  }

  public short colixAxes;
  public void setColixAxes(short colixAxes) {
    this.colixAxes = colixAxes;
  }

  public short colixAxesText;
  public void setColixAxesText(short colixAxesText) {
    this.colixAxesText = colixAxesText;
  }
}
