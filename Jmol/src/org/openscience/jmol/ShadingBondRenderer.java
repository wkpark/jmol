
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
import java.awt.Polygon;
import java.awt.Color;

/**
 * Draws bonds as shaded rectangles colored on each half with the color of the
 * adjacent atom. The shading gives a pseudo-3D look to the bond.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class ShadingBondRenderer implements BondRenderer {

  /**
   * Creates an ShadingBondRenderer with default parameters.
   */
  public ShadingBondRenderer() {
  }

  /**
   * Draws a bond on a particular graphics context.
   *
   * @param gc the Graphics context
   * @param atom1 the atom from which the bond is to be drawn
   * @param atom2 the atom to which the bond is to be drawn
   * @param settings the display settings
   */
  public void paint(Graphics gc, Atom atom1, Atom atom2,
      DisplaySettings settings) {

    ColorProfile colorProfile;
    if (settings.getAtomColorProfile() == DisplaySettings.ATOMCHARGE) {
        colorProfile = new ChargeColorProfile();
    } else {
        colorProfile = new DefaultColorProfile();
    }
    
    int x1 = (int) atom1.getScreenPosition().x;
    int y1 = (int) atom1.getScreenPosition().y;
    int z1 = (int) atom1.getScreenPosition().z;
    int x2 = (int) atom2.getScreenPosition().x;
    int y2 = (int) atom2.getScreenPosition().y;
    int z2 = (int) atom2.getScreenPosition().z;

    double dx = x2 - x1;
    double dy = y2 - y1;
    double dx2 = dx * dx;
    double dy2 = dy * dy;
    double magnitude = Math.sqrt(dx2 + dy2);

    double ctheta = dx / magnitude;
    double stheta = dy / magnitude;

    double radius1 = 0.0;

    if (!settings.getDrawBondsToAtomCenters()) {

      // Adjust radius by perspective based upon z difference.
      double dz = z2 - z1;
      double costheta = Math.sqrt(dx2 + dy2) / Math.sqrt(dx2 + dy2 + dz * dz);
      radius1 = costheta
          * (double) settings.getCircleRadius(z1,
            atom1.getType().getVdwRadius());
      double radius2 =
        costheta
          * (double) settings.getCircleRadius(z2,
                                              atom2.getType().getVdwRadius());

      double bondLengthSquared = dx2 + dy2;

      if (bondLengthSquared <= (radius1 + radius2) * (radius1 + radius2)) {
        return;
      }

    }

    magnitude *= 0.5;

    double halfBondWidth = 0.5 * settings.getBondWidth()
                             * settings.getBondScreenScale();

    int bondOrder = Bond.getBondOrder(atom1, atom2);

    double bondSeparation = 2.0;
    if (bondOrder == 3) {
      bondSeparation *= 2.0;
    }

    Color atom1Color = colorProfile.getColor(atom1);
    for (int i = (int) halfBondWidth; i > -1; --i) {
      double len = ((double) i) / (int) halfBondWidth;
      int red1 = (int) ((float) atom1Color.getRed() * (1.0f - len));
      int green1 = (int) ((float) atom1Color.getGreen() * (1.0f - len));
      int blue1 = (int) ((float) atom1Color.getBlue() * (1.0f - len));

      // Bitwise masking to make color model:
      int model1 = -16777216 | red1 << 16 | green1 << 8 | blue1;
      gc.setColor(new Color(model1));

      if ((bondOrder == 2) || (bondOrder == 3)) {
        Polygon poly1 = RendererUtilities.getBondPolygon(x1, y1, ctheta,
                          stheta, radius1, bondSeparation * halfBondWidth,
                          magnitude, len * halfBondWidth);

        gc.fillPolygon(poly1);

        poly1 = RendererUtilities.getBondPolygon(x1, y1, ctheta, stheta,
            radius1, -bondSeparation * halfBondWidth, magnitude,
              len * halfBondWidth);

        gc.fillPolygon(poly1);
      }
      if ((bondOrder == 1) || (bondOrder == 3)) {
        Polygon poly1 = RendererUtilities.getBondPolygon(x1, y1, ctheta,
                          stheta, radius1, 0.0, magnitude,
                          len * halfBondWidth);

        gc.fillPolygon(poly1);
      }

    }
  }

}
