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

import freeware.PrintfFormat;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.Font;
import java.awt.FontMetrics;
//import java.awt.Graphics;

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

  public void paint(Graphics25D g25d,
                    DisplayControl control, boolean showLabel) {
    paintAngleLine(g25d, control);
    if (showLabel)
      paintAngleString(g25d, control);
  }

  private void paintAngleLine(Graphics25D g25d, DisplayControl control) {
    int x1 = atom1.getScreenX(), y1 = atom1.getScreenY(),
      z1 = atom1.getScreenZ();
    int x2 = atom2.getScreenX(), y2 = atom2.getScreenY(),
      z2 = atom2.getScreenZ();
    int x3 = atom3.getScreenX(), y3 = atom3.getScreenY(),
      z3 = atom3.getScreenZ();
    int xA = (x1 + x2) / 2;
    int yA = (y1 + y2) / 2;
    int zA = (z1 + z2) / 2;
    int xB = (x3 + x2) / 2;
    int yB = (y3 + y2) / 2;
    int zB = (z3 + z2) / 2;

    control.maybeDottedStroke(g25d);
    g25d.setColor(control.getColorAngle());
    g25d.drawLine(xA, yA, zA, xB, yB, zB);
    control.defaultStroke(g25d);
  }

  /**
   * The format used for displaying angle values.
   */
  private static PrintfFormat angleFormat = new PrintfFormat("%0.1f\u00b0");
  
  private void paintAngleString(Graphics25D g25d, DisplayControl control) {
    int x1 = atom1.getScreenX(), y1 = atom1.getScreenY(),
      z1 = atom1.getScreenZ(), d1 = atom1.getScreenDiameter();
    int x2 = atom2.getScreenX(), y2 = atom2.getScreenY(),
      z2 = atom2.getScreenZ(), d2 = atom2.getScreenDiameter();
    int x3 = atom3.getScreenX(), y3 = atom3.getScreenY(),
      z3 = atom3.getScreenZ(), d3 = atom3.getScreenDiameter();
    int avgRadius = (d1 + d2 + d3) / 6;
    Font font = control.getMeasureFont(avgRadius);
    g25d.setFont(font);
    FontMetrics fontMetrics = g25d.getFontMetrics(font);
    g25d.setColor(control.getColorAngle());
    int j = fontMetrics.stringWidth(strAngle);

    int xloc = (2 * x2 + x1 + x3) / 4;
    int yloc = (2 * y2 + y1 + y3) / 4;
    int zloc = (2 * z2 + z1 + z3) / 4;

    g25d.drawString(strAngle, xloc, yloc, zloc);
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
