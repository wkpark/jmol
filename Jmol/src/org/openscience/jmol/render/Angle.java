
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
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class Angle implements MeasurementInterface {

  private int[] Atoms = new int[3];
  Atom atom1, atom2, atom3;
  private double angle;
  String strAngle;

  public Angle(int a1, Atom atom1, int a2, Atom atom2, int a3, Atom atom3) {
    super();
    Atoms[0] = a1;
    Atoms[1] = a2;
    Atoms[2] = a3;
    this.atom1 = atom1;
    this.atom2 = atom2;
    this.atom3 = atom3;
    Vector3d vector12 = new Vector3d(atom1.getPoint3D());
    vector12.sub(atom2.getPoint3D());
    Vector3d vector32 = new Vector3d(atom3.getPoint3D());
    vector32.sub(atom2.getPoint3D());
    angle = vector12.angle(vector32) * 180 / Math.PI;
    strAngle = angleFormat.sprintf(angle);
  }

  public void paint(Graphics g, DisplayControl control, boolean showLabel) {
    paintAngleLine(g, control);
    if (showLabel)
      paintAngleString(g, control);
  }

  private void paintAngleLine(Graphics g, DisplayControl control) {
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
  
  private void paintAngleString(Graphics g, DisplayControl control) {
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
    g.setColor(control.getColorAngle());
    int j = fontMetrics.stringWidth(strAngle);

    int xloc = (2 * x2 + x1 + x3) / 4;
    int yloc = (2 * y2 + y1 + y3) / 4;

    g.drawString(strAngle, xloc, yloc);
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
        + angle + "]");
  }

  public double getAngle() {
    return angle;
  }
}
