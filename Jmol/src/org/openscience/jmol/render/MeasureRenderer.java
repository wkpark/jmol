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
package org.openscience.jmol.render;
import org.openscience.jmol.*;
import org.openscience.jmol.g25d.Graphics25D;

//import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Enumeration;

/**
 *  Drawing methods for ChemFrame measurements.
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
public class MeasureRenderer {

  /**
   * Paint this model to a graphics context.  It uses the matrix
   * associated with this model to map from model space to screen
   * space.
   *
   * @param g the Graphics context to paint to
   */
  public synchronized void paint(Graphics25D g25d, Rectangle rectClip,
                                 DisplayControl control) {
    ChemFrame frame = control.getFrame();
    boolean showMeasurementLabels = control.getShowMeasurementLabels();
    Enumeration e;

    e = control.getDistanceMeasurements().elements();
    while (e.hasMoreElements()) {
      Distance d = (Distance) e.nextElement();
      d.paint(g25d, control, showMeasurementLabels);
    }

    e = control.getAngleMeasurements().elements();
    while (e.hasMoreElements()) {
      Angle an = (Angle) e.nextElement();
      an.paint(g25d, control, showMeasurementLabels);
    }

    e = control.getDihedralMeasurements().elements();
    while (e.hasMoreElements()) {
      Dihedral dh = (Dihedral) e.nextElement();
      dh.paint(g25d, control, showMeasurementLabels);
    }
  }
}

