
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

import freeware.PrintfFormat;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class Distance extends Measurement implements MeasurementInterface {

  private int[] Atoms = new int[2];
  private double distance;
  private boolean computed = false;
  private ChemFrame fcf;

  public Distance(int a1, int a2) {
    super();
    Atoms[0] = a1;
    Atoms[1] = a2;
    compute();
  }

  public void paint(Graphics g, DisplayControl control, boolean showLabel,
                    Atom atom1, Atom atom2) throws Exception {
    paintDistLine(g, control, atom1, atom2);
    if (showLabel)
      paintDistString(g, control, atom1, atom2);
  }

  private void paintDistLine(Graphics g, DisplayControl control,
                             Atom atom1, Atom atom2) {
    control.maybeDottedStroke(g);
    g.setColor(control.getColorDistance());
    g.drawLine(atom1.getScreenX(), atom1.getScreenY(),
	       atom2.getScreenX(), atom2.getScreenY());
  }

  /**
   * The format used for displaying distance values.
   */
  private static PrintfFormat distanceFormat = new PrintfFormat("%0.3f \u00c5");
  
  private void paintDistString(Graphics g, DisplayControl control,
                               Atom atom1, Atom atom2) {
    int x1 = atom1.getScreenX(), y1 = atom1.getScreenY(),
      d1 = atom1.getScreenDiameter();
    int x2 = atom2.getScreenX(), y2 = atom2.getScreenY(),
      d2 = atom2.getScreenDiameter();
    
    int avgRadius = (d1 + d2) / 4;

    Font font = control.getMeasureFont(avgRadius);
    g.setFont(font);
    FontMetrics fontMetrics = g.getFontMetrics(font);
    g.setColor(control.getColorDistance());
    String s = distanceFormat.sprintf(getDistance());
    int j = fontMetrics.stringWidth(s);

    if (x2 == x1) {
      g.drawString(s, x1 + 1, ((y1 + y2) / 2) + 1);
    } else {
      g.drawString(s, (x1 + x2) / 2 - j - 1, (y1 + y2) / 2 - 1);
    }
  }

  public int[] getAtomList() {
    return Atoms;
  }

  public boolean sameAs(int a1, int a2) {

    if ((Atoms[0] == a1) && (Atoms[1] == a2)) {
      return true;
    } else {
      if ((Atoms[0] == a2) && (Atoms[1] == a1)) {
        return true;
      } else {
        return false;
      }
    }
  }

  public String toString() {
    return ("[" + Atoms[0] + "," + Atoms[1] + " = " + getDistance() + "]");
  }

  public double getDistance() {
    if (!computed || (cf != fcf)) {
      compute();
    }
    return distance;
  }

  public void compute() {

    if (cf == null) {
      return;
    }

    double[] c0 = cf.getAtomCoords(Atoms[0]);
    double[] c1 = cf.getAtomCoords(Atoms[1]);

    double ax = c0[0] - c1[0];
    double ay = c0[1] - c1[1];
    double az = c0[2] - c1[2];

    double ax2 = ax * ax;
    double ay2 = ay * ay;
    double az2 = az * az;

    double rij2 = ax2 + ay2 + az2;

    distance = Math.sqrt(rij2);
    fcf = cf;
    computed = true;
  }
}


