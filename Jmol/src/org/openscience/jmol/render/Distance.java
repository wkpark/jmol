
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
package org.openscience.jmol.render;
import org.openscience.jmol.*;

import javax.vecmath.Point3d;

import freeware.PrintfFormat;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

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

  public void paint(Graphics g, DisplayControl control, boolean showLabel) {
    paintDistLine(g, control);
    if (showLabel)
      paintDistString(g, control);
  }

  private void paintDistLine(Graphics g, DisplayControl control) {
    control.maybeDottedStroke(g);
    g.setColor(control.getColorDistance());
    g.drawLine(atom1.getScreenX(), atom1.getScreenY(),
	       atom2.getScreenX(), atom2.getScreenY());
  }

  /**
   * The format used for displaying distance values.
   */
  private static PrintfFormat distanceFormat = new PrintfFormat("%0.3f \u00c5");
  
  private String formatDistance(double dist) {
    return distanceFormat.sprintf(dist);
  }

  private void paintDistString(Graphics g, DisplayControl control) {
    int x1 = atom1.getScreenX(), y1 = atom1.getScreenY(),
      d1 = atom1.getScreenDiameter();
    int x2 = atom2.getScreenX(), y2 = atom2.getScreenY(),
      d2 = atom2.getScreenDiameter();
    
    int avgRadius = (d1 + d2) / 4;

    Font font = control.getMeasureFont(avgRadius);
    g.setFont(font);
    FontMetrics fontMetrics = g.getFontMetrics(font);
    g.setColor(control.getColorDistance());
    int j = fontMetrics.stringWidth(strDistance);
    if (x2 == x1) {
      g.drawString(strDistance, x1 + 1, ((y1 + y2) / 2) + 1);
    } else {
      g.drawString(strDistance, (x1 + x2) / 2 - j - 1, (y1 + y2) / 2 - 1);
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


