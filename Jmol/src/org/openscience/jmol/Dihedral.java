
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

import freeware.PrintfFormat;
import java.awt.Font;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
class Dihedral extends Measurement implements MeasurementInterface {

  private int[] Atoms = new int[4];
  private double dihedral;
  private boolean computed = false;
  private ChemFrame fcf;

  public Dihedral(int a1, int a2, int a3, int a4) {

    super();
    Atoms[0] = a1;
    Atoms[1] = a2;
    Atoms[2] = a3;
    Atoms[3] = a4;
    compute();
  }

  public void paint(
      Graphics g, DisplaySettings settings, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3, int x4, int y4, int z4)
        throws Exception {
    paintDihedralLine(g, settings, x1, y1, x2, y2, x3, y3, x4, y4);
    paintDihedralString(g, settings, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4,
        y4, z4);
  }

  private void paintDihedralLine(Graphics g, DisplaySettings settings,
      int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {

    int xa = (x1 + x2) / 2;
    int ya = (y1 + y2) / 2;
    int xb = (x3 + x4) / 2;
    int yb = (y3 + y4) / 2;

    g.setColor(settings.getDihedralColor());
    String vers = System.getProperty("java.version");
    if (vers.compareTo("1.2") >= 0) {
      Graphics2D g2 = (Graphics2D) g;
      BasicStroke dotted = new BasicStroke(1, BasicStroke.CAP_ROUND,
                             BasicStroke.JOIN_ROUND, 0, new float[] {
        3, 3
      }, 0);
      g2.setStroke(dotted);
      g2.drawLine(xa, ya, xb, yb);
    } else {
      g.drawLine(xa, ya, xb, yb);
    }
  }

  /**
   * The format used for displaying dihedral values.
   */
  private static PrintfFormat dihedralFormat = new PrintfFormat("%0.1f\u00b0");
  
  private void paintDihedralString(Graphics g, DisplaySettings settings,
      int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3,
        int x4, int y4, int z4) {

    Font font = new Font("Helvetica", Font.PLAIN,
                  (int) (getAvgRadius(settings, z1, z2, z3, z4)));
    g.setFont(font);
    FontMetrics fontMetrics = g.getFontMetrics(font);
    g.setColor(settings.getTextColor());
    String s = dihedralFormat.sprintf(getDihedral());
    int j = fontMetrics.stringWidth(s);

    int xloc = (x1 + x2 + x3 + x4) / 4;
    int yloc = (y1 + y2 + y3 + y4) / 4;

    g.drawString(s, xloc, yloc);
  }

  private float getAvgRadius(DisplaySettings settings, int z1, int z2,
      int z3, int z4) {

    if (cf == null) {
      return 0.0f;
    }

    BaseAtomType a = cf.getAtomAt(Atoms[0]).getType();
    BaseAtomType b = cf.getAtomAt(Atoms[1]).getType();
    BaseAtomType c = cf.getAtomAt(Atoms[2]).getType();
    BaseAtomType d = cf.getAtomAt(Atoms[3]).getType();

    return (settings.getCircleRadius(z1, a.getVdwRadius())
        + settings.getCircleRadius(z2, b.getVdwRadius())
          + settings.getCircleRadius(z3, c.getVdwRadius())
            + settings.getCircleRadius(z4, d.getVdwRadius())) / 4.0f;
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
        + " = " + getDihedral() + "]");
  }

  public double getDihedral() {
    if (!computed || (cf != fcf)) {
      compute();
    }
    return dihedral;
  }

  public void compute() {

    if (cf == null) {
      return;
    }

    double[] c0 = cf.getAtomCoords(Atoms[0]);
    double[] c1 = cf.getAtomCoords(Atoms[1]);
    double[] c2 = cf.getAtomCoords(Atoms[2]);
    double[] c3 = cf.getAtomCoords(Atoms[3]);


    double ijx = c0[0] - c1[0];
    double ijy = c0[1] - c1[1];
    double ijz = c0[2] - c1[2];

    double kjx = c2[0] - c1[0];
    double kjy = c2[1] - c1[1];
    double kjz = c2[2] - c1[2];

    double klx = c2[0] - c3[0];
    double kly = c2[1] - c3[1];
    double klz = c2[2] - c3[2];

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
    computed = true;
  }

  public static double toDegrees(double angrad) {
    return angrad * 180.0 / Math.PI;
  }
}

