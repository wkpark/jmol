
/*
 * Copyright 2001 The Jmol Development Team
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

import java.awt.Font;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
class Distance extends Measurement implements MeasurementInterface {

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

  public void paint(
      Graphics g, DisplaySettings settings, int x1, int y1, int z1, int x2, int y2, int z2)
        throws Exception {
    paintDistLine(g, settings, x1, y1, x2, y2);
    paintDistString(g, settings, x1, y1, z1, x2, y2, z2);
  }

  private void paintDistLine(Graphics g, DisplaySettings settings, int x1,
      int y1, int x2, int y2) {

    g.setColor(settings.getDistanceColor());
    String vers = System.getProperty("java.version");
    if (vers.compareTo("1.2") >= 0) {
      Graphics2D g2 = (Graphics2D) g;
      BasicStroke dotted = new BasicStroke(1, BasicStroke.CAP_ROUND,
                             BasicStroke.JOIN_ROUND, 0, new float[] {
        3, 3
      }, 0);
      g2.setStroke(dotted);
      g2.drawLine(x1, y1, x2, y2);
    } else {
      g.drawLine(x1, y1, x2, y2);
    }
  }

  private void paintDistString(Graphics g, DisplaySettings settings, int x1,
      int y1, int z1, int x2, int y2, int z2) {

    double run = (double) (x2 - x1);
    double rise = (double) (y2 - y1);
    double m = rise / run;
    Font font = new Font("Helvetica", Font.PLAIN,
                  (int) (getAvgRadius(settings, z1, z2)));
    g.setFont(font);
    FontMetrics fontMetrics = g.getFontMetrics(font);
    g.setColor(settings.getTextColor());
    String s = (new Double(getDistance())).toString();
    if (s.length() > 5) {
      s = s.substring(0, 5);
    }
    s += " \u00c5";
    int j = fontMetrics.stringWidth(s);

    if (x2 == x1) {
      g.drawString(s, x1 + 1, ((y1 + y2) / 2) + 1);
    } else {
      g.drawString(s, (x1 + x2) / 2 - j - 1, (y1 + y2) / 2 - 1);
    }
  }

  public float getAvgRadius(DisplaySettings settings, int z1, int z2) {

    if (cf == null) {
      return 0.0f;
    }

    BaseAtomType a = cf.getAtomAt(Atoms[0]).getType();
    BaseAtomType b = cf.getAtomAt(Atoms[1]).getType();

    return (settings.getCircleRadius(z1, a.getVdwRadius()) + settings.getCircleRadius(z2, b.getVdwRadius()))
        / 2.0f;
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


