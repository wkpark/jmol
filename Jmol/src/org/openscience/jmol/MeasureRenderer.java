
/*
 * Copyright 2002 The Jmol Development Team
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
  public synchronized void paint(Graphics g, ChemFrame frame,
      DisplaySettings settings) {

    if (frame.getDistanceMeasurements() != null) {
      Enumeration e = frame.getDistanceMeasurements().elements();
      while (e.hasMoreElements()) {
        Distance d = (Distance) e.nextElement();
        int[] al = d.getAtomList();
        int l = al[0];
        int j = al[1];
        try {
          d.paint(g, settings,
              (int) frame.getAtomAt(l).getScreenPosition().x,
                (int) frame.getAtomAt(l).getScreenPosition().y,
                  (int) frame.getAtomAt(l).getScreenPosition().z,
                    (int) frame.getAtomAt(j).getScreenPosition().x,
                      (int) frame.getAtomAt(j).getScreenPosition().y,
                        (int) frame.getAtomAt(j).getScreenPosition().z);
        } catch (Exception ex) {
        }
      }
    }
    if (frame.getAngleMeasurements() != null) {
      Enumeration e = frame.getAngleMeasurements().elements();
      while (e.hasMoreElements()) {
        Angle an = (Angle) e.nextElement();
        int[] al = an.getAtomList();
        int l = al[0];
        int j = al[1];
        int k = al[2];
        try {
          an.paint(g, settings,
              (int) frame.getAtomAt(l).getScreenPosition().x,
                (int) frame.getAtomAt(l).getScreenPosition().y,
                  (int) frame.getAtomAt(l).getScreenPosition().z,
                    (int) frame.getAtomAt(j).getScreenPosition().x,
                      (int) frame.getAtomAt(j).getScreenPosition().y,
                        (int) frame.getAtomAt(j).getScreenPosition().z,
                          (int) frame.getAtomAt(k).getScreenPosition().x,
                            (int) frame.getAtomAt(k).getScreenPosition().y,
                              (int) frame.getAtomAt(k).getScreenPosition().z);
        } catch (Exception ex) {
        }
      }
    }
    if (frame.getDihedralMeasurements() != null) {
      Enumeration e = frame.getDihedralMeasurements().elements();
      while (e.hasMoreElements()) {
        Dihedral dh = (Dihedral) e.nextElement();
        int[] dhl = dh.getAtomList();
        int l = dhl[0];
        int j = dhl[1];
        int k = dhl[2];
        int m = dhl[3];
        try {
          dh.paint(g, settings, (int) frame.getAtomAt(l).getScreenPosition()
              .x, (int) frame.getAtomAt(l).getScreenPosition()
                .y, (int) frame.getAtomAt(l).getScreenPosition()
                  .z, (int) frame.getAtomAt(j).getScreenPosition()
                    .x, (int) frame.getAtomAt(j).getScreenPosition()
                      .y, (int) frame.getAtomAt(j).getScreenPosition()
                        .z, (int) frame.getAtomAt(k).getScreenPosition()
                          .x, (int) frame.getAtomAt(k).getScreenPosition()
                            .y, (int) frame.getAtomAt(k).getScreenPosition()
                              .z, (int) frame.getAtomAt(m).getScreenPosition()
                                .x, (int) frame.getAtomAt(m)
                                  .getScreenPosition()
                                    .y, (int) frame.getAtomAt(m)
                                      .getScreenPosition().z);
        } catch (Exception ex) {
        }
      }
    }
  }

}

