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
package org.openscience.jmol.render;
import org.openscience.jmol.*;
import org.openscience.jmol.g25d.Graphics25D;

import javax.vecmath.Point3d;

import freeware.PrintfFormat;
import java.awt.Font;
import java.awt.FontMetrics;
//import java.awt.Graphics;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class Distance implements MeasurementInterface {

  int[] iatoms = new int[2];
  Atom atom1;
  Atom atom2;
  private double distance;
  private String strDistance;

  public Distance(int iatom1, Atom atom1, int iatom2, Atom atom2) {
    super();
    iatoms[0] = iatom1;
    iatoms[1] = iatom2;
    this.atom1 = atom1;
    this.atom2 = atom2;
    distance = atom1.getPoint3D().distance(atom2.getPoint3D());
    strDistance = formatDistance(distance);
  }

  public void paint(Graphics25D g25d, DisplayControl control,
                    boolean showLabel) {
    paintDistLine(g25d, control);
    if (showLabel)
      paintDistString(g25d, control);
  }

  private void paintDistLine(Graphics25D g25d, DisplayControl control) {
    control.maybeDottedStroke(g25d);
    g25d.setColor(control.getColorDistance());
    g25d.drawLine(atom1.getScreenX(), atom1.getScreenY(), atom1.getScreenZ(),
                  atom2.getScreenX(), atom2.getScreenY(), atom2.getScreenZ());
    control.defaultStroke(g25d);
  }

  /**
   * The format used for displaying distance values.
   */
  private static PrintfFormat distanceFormat = new PrintfFormat("%0.3f \u00c5");
  
  private String formatDistance(double dist) {
    return distanceFormat.sprintf(dist);
  }

  private void paintDistString(Graphics25D g25d, DisplayControl control) {
    int x1 = atom1.getScreenX(), y1 = atom1.getScreenY(),
      z1 = atom1.getScreenZ(), d1 = atom1.getScreenDiameter();
    int x2 = atom2.getScreenX(), y2 = atom2.getScreenY(),
      z2 = atom2.getScreenZ(), d2 = atom2.getScreenDiameter();
    
    int avgRadius = (d1 + d2) / 4;

    Font font = control.getMeasureFont(avgRadius);
    g25d.setFont(font);
    FontMetrics fontMetrics = g25d.getFontMetrics(font);
    g25d.setColor(control.getColorDistance());
    int j = fontMetrics.stringWidth(strDistance);
    int z = (z1 + z2) / 2;
    if (x2 == x1) {
      g25d.drawString(strDistance,
                      x1 + 1, ((y1 + y2) / 2) + 1, z);
    } else {
      g25d.drawString(strDistance,
                      (x1 + x2) / 2 - j - 1, (y1 + y2) / 2 - 1, z);
    }
  }

  public int[] getAtomList() {
    return iatoms;
  }

  public boolean sameAs(int a1, int a2) {

    if ((iatoms[0] == a1) && (iatoms[1] == a2)) {
      return true;
    } else {
      if ((iatoms[0] == a2) && (iatoms[1] == a1)) {
        return true;
      } else {
        return false;
      }
    }
  }

  public String toString() {
    return ("[" + iatoms[0] + "," + iatoms[1] + " = " + distance + "]");
  }
}


