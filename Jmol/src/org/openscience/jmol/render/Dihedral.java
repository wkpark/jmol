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
import java.awt.Font;
import java.awt.FontMetrics;
//import java.awt.Graphics;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class Dihedral implements MeasurementInterface {

  private int[] Atoms = new int[4];
  Atom atom1, atom2, atom3, atom4;
  private double dihedral;
  String strDihedral;

  public Dihedral(int a1, Atom atom1, int a2, Atom atom2,
                  int a3, Atom atom3, int a4, Atom atom4) {

    super();
    Atoms[0] = a1;
    Atoms[1] = a2;
    Atoms[2] = a3;
    Atoms[3] = a4;
    this.atom1 = atom1; this.atom2 = atom2;
    this.atom3 = atom3; this.atom4 = atom4;
    compute();
    strDihedral = dihedralFormat.sprintf(dihedral);
  }

  public void paint(Graphics25D g25d,
                    DisplayControl control, boolean showLabel) {
    paintDihedralLine(g25d, control);
    if (showLabel)
      paintDihedralString(g25d, control);
  }

  private void paintDihedralLine(Graphics25D g25d, DisplayControl control) {
    int x1=atom1.getScreenX(), y1=atom1.getScreenY(), z1=atom1.getScreenZ();
    int x2=atom2.getScreenX(), y2=atom2.getScreenY(), z2=atom2.getScreenZ();
    int x3=atom3.getScreenX(), y3=atom3.getScreenY(), z3=atom3.getScreenZ();
    int x4=atom4.getScreenX(), y4=atom4.getScreenY(), z4=atom4.getScreenZ();
    int xa = (x1 + x2) / 2;
    int ya = (y1 + y2) / 2;
    int za = (z1 + z2) / 2;
    int xb = (x3 + x4) / 2;
    int yb = (y3 + y4) / 2;
    int zb = (z3 + z4) / 2;

    control.maybeDottedStroke(g25d);
    g25d.setColor(control.getColorDihedral());
    g25d.drawLine(xa, ya, za, xb, yb, zb);
    control.defaultStroke(g25d);
  }

  /**
   * The format used for displaying dihedral values.
   */
  private static PrintfFormat dihedralFormat = new PrintfFormat("%0.1f\u00b0");
  
  private void paintDihedralString(Graphics25D g25d, DisplayControl control) {
    int x1 = atom1.getScreenX(), y1 = atom1.getScreenY(),
	d1 = atom1.getScreenDiameter();
    int x2 = atom2.getScreenX(), y2 = atom2.getScreenY(),
	d2 = atom2.getScreenDiameter();
    int x3 = atom3.getScreenX(), y3 = atom3.getScreenY(),
	d3 = atom3.getScreenDiameter();
    int x4 = atom4.getScreenX(), y4 = atom4.getScreenY(),
	d4 = atom4.getScreenDiameter();
    int avgRadius = (d1 + d2 + d3 + d4) / 8;
    Font font = control.getMeasureFont(avgRadius);
    g25d.setFont(font);
    FontMetrics fontMetrics = g25d.getFontMetrics(font);
    g25d.setColor(control.getColorDihedral());
    int j = fontMetrics.stringWidth(strDihedral);

    int xloc = (x1 + x2 + x3 + x4) / 4;
    int yloc = (y1 + y2 + y3 + y4) / 4;

    g25d.drawString(strDihedral, xloc, yloc);
  }

  public int[] getAtomList() {
    return Atoms;
  }

  public boolean sameAs(int a1, int a2, int a3, int a4) {

    /* Lazy way out. Just make sure that we're one of the 2
       ordered permutations. */
    if ((Atoms[0] == a1) && (Atoms[1] == a2) && (Atoms[2] == a3)
        && (Atoms[3] == a4)) {
      return true;
    } else {
      if ((Atoms[0] == a4) && (Atoms[1] == a3) && (Atoms[2] == a2)
          && (Atoms[3] == a1)) {
        return true;
      }
    }
    return false;
  }

  public String toString() {
    return ("[" + Atoms[0] + "," + Atoms[1] + "," + Atoms[2] + "," + Atoms[3]
        + " = " + dihedral + "]");
  }

  public double getDihedral() {
    return dihedral;
  }

  public void compute() {

    Point3d p1 = atom1.getPoint3D(), p2 = atom2.getPoint3D();
    Point3d p3 = atom3.getPoint3D(), p4 = atom4.getPoint3D();

    double ijx = p1.x - p2.x;
    double ijy = p1.y - p2.y;
    double ijz = p1.z - p2.z;

    double kjx = p3.x - p2.x;
    double kjy = p3.y - p2.y;
    double kjz = p3.z - p2.z;

    double klx = p3.x - p4.x;
    double kly = p3.y - p4.y;
    double klz = p3.z - p4.z;

    double ax = ijy * kjz - ijz * kjy;
    double ay = ijz * kjx - ijx * kjz;
    double az = ijx * kjy - ijy * kjx;
    double cx = kjy * klz - kjz * kly;
    double cy = kjz * klx - kjx * klz;
    double cz = kjx * kly - kjy * klx;

    double ai2 = 1.0 / (ax * ax + ay * ay + az * az);
    double ci2 = 1.0 / (cx * cx + cy * cy + cz * cz);

    double ai = Math.sqrt(ai2);
    double ci = Math.sqrt(ci2);
    double denom = ai * ci;
    double cross = ax * cx + ay * cy + az * cz;
    double cosang = cross * denom;
    if (cosang > 1.0) {
      cosang = 1.0;
    }
    if (cosang < -1.0) {
      cosang = -1.0;
    }

    dihedral = toDegrees(Math.acos(cosang));
    double dot  =  ijx*cx + ijy*cy + ijz*cz;
    double absDot =  Math.abs(dot);
    dihedral = (dot/absDot > 0) ? dihedral : -dihedral;
  }

  public static double toDegrees(double angrad) {
    return angrad * 180.0 / Math.PI;
  }
}
