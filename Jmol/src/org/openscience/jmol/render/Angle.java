
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

public class Angle extends Measurement implements MeasurementInterface {

  private int[] Atoms = new int[3];
  private double angle;
  private boolean computed = false;
  private ChemFrame fcf;

  public Angle(int a1, int a2, int a3) {
    super();
    Atoms[0] = a1;
    Atoms[1] = a2;
    Atoms[2] = a3;
    compute();
  }

  public void paint(Graphics g, DisplayControl control,
                    Atom atom1, Atom atom2, Atom atom3) throws Exception {
    paintAngleLine(g, control, atom1, atom2, atom3);
    paintAngleString(g, control, atom1, atom2, atom3);
  }

  private void paintAngleLine(Graphics g, DisplayControl control,
                              Atom atom1, Atom atom2, Atom atom3) {
    int x1 = atom1.getScreenX(), y1 = atom1.getScreenY();
    int x2 = atom2.getScreenX(), y2 = atom2.getScreenY();
    int x3 = atom3.getScreenX(), y3 = atom3.getScreenY();
    int xa = (x1 + x2) / 2;
    int ya = (y1 + y2) / 2;
    int xb = (x3 + x2) / 2;
    int yb = (y3 + y2) / 2;

    control.maybeDottedStroke(g);
    g.setColor(control.getColorAngle());
    g.drawLine(xa, ya, xb, yb);
  }

  /**
   * The format used for displaying angle values.
   */
  private static PrintfFormat angleFormat = new PrintfFormat("%0.1f\u00b0");
  
  private void paintAngleString(Graphics g, DisplayControl control,
                                Atom atom1, Atom atom2, Atom atom3) {
    int x1 = atom1.getScreenX(), y1 = atom1.getScreenY(),
	d1 = atom1.getScreenDiameter();
    int x2 = atom2.getScreenX(), y2 = atom2.getScreenY(),
	d2 = atom2.getScreenDiameter();
    int x3 = atom3.getScreenX(), y3 = atom3.getScreenY(),
	d3 = atom3.getScreenDiameter();
    int avgRadius = (d1 + d2 + d3) / 6;
    Font font = control.getMeasureFont(avgRadius);
    g.setFont(font);
    FontMetrics fontMetrics = g.getFontMetrics(font);
    g.setColor(control.getColorText());
    String s = angleFormat.sprintf(getAngle());
    int j = fontMetrics.stringWidth(s);

    int xloc = (2 * x2 + x1 + x3) / 4;
    int yloc = (2 * y2 + y1 + y3) / 4;

    g.drawString(s, xloc, yloc);
  }

  public int[] getAtomList() {
    return Atoms;
  }

  public boolean sameAs(int a1, int a2, int a3) {

    /* ordering is important in an angle measurement.  We can rule out a
       match based on a2, and then permute a1 and a3 */
    if (Atoms[1] == a2) {
      if ((Atoms[0] == a1) && (Atoms[2] == a3)) {
        return true;
      } else {
        if ((Atoms[0] == a3) && (Atoms[2] == a1)) {
          return true;
        } else {
          return false;
        }
      }
    } else {
      return false;
    }
  }

  public String toString() {
    return ("[" + Atoms[0] + "," + Atoms[1] + "," + Atoms[2] + " = "
        + getAngle() + "]");
  }

  public double getAngle() {
    if (!computed || (cf != fcf)) {
      compute();
    }
    return angle;
  }

  public void compute() {

    if (cf == null) {
      return;
    }

    double[] c0 = cf.getAtomCoords(Atoms[0]);
    double[] c1 = cf.getAtomCoords(Atoms[1]);
    double[] c2 = cf.getAtomCoords(Atoms[2]);


    double ax = c0[0] - c1[0];
    double ay = c0[1] - c1[1];
    double az = c0[2] - c1[2];

    double bx = c2[0] - c1[0];
    double by = c2[1] - c1[1];
    double bz = c2[2] - c1[2];

    double ax2 = ax * ax;
    double ay2 = ay * ay;
    double az2 = az * az;

    double bx2 = bx * bx;
    double by2 = by * by;
    double bz2 = bz * bz;

    double rij2 = ax2 + ay2 + az2;
    double rkj2 = bx2 + by2 + bz2;

    double riji2 = 1.0 / rij2;
    double rkji2 = 1.0 / rkj2;

    double dot = ax * bx + ay * by + az * bz;
    double denom = Math.sqrt(riji2 * rkji2);
    double cosang = dot * denom;
    if (cosang > 1.0) {
      cosang = 1.0;
    }
    if (cosang < -1.0) {
      cosang = -1.0;
    }

    angle = toDegrees(Math.acos(cosang));
    fcf = cf;
    computed = true;
  }

  public static double toDegrees(double angrad) {
    return angrad * 180.0 / Math.PI;
  }
}
