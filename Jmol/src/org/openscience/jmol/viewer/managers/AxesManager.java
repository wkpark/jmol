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

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.datamodel.Axes;
import org.openscience.jmol.viewer.datamodel.BoundingBox;
import org.openscience.jmol.viewer.g3d.Colix;

public class AxesManager {

  JmolViewer viewer;

  public Axes axes;
  public BoundingBox bbox;

  public AxesManager(JmolViewer viewer) {
    this.viewer = viewer;

    axes = new Axes(viewer);
    bbox = new BoundingBox(viewer);
    setColixAxes(Colix.LIGHTGRAY);
    setColixAxesText(Colix.LIGHTGRAY);
  }

  public byte modeAxes = JmolViewer.AXES_NONE;
  public void setModeAxes(byte modeAxes) {
    this.modeAxes = modeAxes;
    recalc();
  }

  public boolean showBoundingBox = false;
  public void setShowBoundingBox(boolean showBoundingBox) {
    this.showBoundingBox = showBoundingBox;
    recalc();
  }

  public void recalc() {
    if (viewer.haveFile()) {
      axes.recalc(modeAxes);
      if (showBoundingBox)
        bbox.recalc();
    }
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
