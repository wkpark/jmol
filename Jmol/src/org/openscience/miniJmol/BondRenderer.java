
/*
 * BondRenderer.java
 *
 * Copyright (C) 1999  Bradley A. Smith
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openscience.miniJmol;

import org.openscience.jmol.DisplaySettings;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Color;

/**
 * Drawing methods for bonds.
 */
public class BondRenderer {

	/**
	 * Creates an BondRenderer with default parameters.
	 */
	public BondRenderer() {
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

		int x1 = (int) atom1.getScreenPosition().x;
		int y1 = (int) atom1.getScreenPosition().y;
		int z1 = (int) atom1.getScreenPosition().z;
		int x2 = (int) atom2.getScreenPosition().x;
		int y2 = (int) atom2.getScreenPosition().y;
		int z2 = (int) atom2.getScreenPosition().z;

		int xmp = (x1 + x2) / 2;
		int ymp = (y1 + y2) / 2;

		double r1 = 0.0;
		double r2 = 0.0;
		int deltaX = 0;
		int deltaY = 0;
		double tmp = 0.0;

		double run = (double) (x2 - x1);
		double run2 = run * run;
		double rise = (double) (y2 - y1);
		double rise2 = rise * rise;
		double zdiff = (double) (z2 - z1);
		double costheta = Math.sqrt(run2 + rise2)
							  / Math.sqrt(run2 + rise2 + zdiff * zdiff);

		if (settings.getDrawBondsToAtomCenters()
				|| settings.getFastRendering()) {
			r1 = 0.0;
			r2 = 0.0;
		} else {
			r1 = costheta
					* (double) settings.getCircleRadius(z1,
						atom1.getType().getVdwRadius());
			r2 = costheta
					* (double) settings.getCircleRadius(z2,
						atom2.getType().getVdwRadius());
		}

		double bl2 = run * run + rise * rise;

		double m = rise / run;

		if (bl2 <= (r1 + r2) * (r1 + r2)) {
			return;

		}
		double ddx1 = 0.0;
		double ddx2 = 0.0;

		int dx1 = 0;
		int dy1 = 0;
		int dx2 = 0;
		int dy2 = 0;

		if (x1 == x2) {
			if (y1 > y2) {
				dy1 = -(int) Math.round(r1);
				dy2 = (int) Math.round(r2);
			} else {
				dy1 = (int) Math.round(r1);
				dy2 = -(int) Math.round(r2);
			}
		} else if (x1 >= x2) {
			ddx1 = -Math.sqrt(r1 * r1 / (1 + m * m));
			ddx2 = -Math.sqrt(r2 * r2 / (1 + m * m));
			dx1 = (int) Math.round(ddx1);
			dx2 = -(int) Math.round(ddx2);
			dy1 = (int) Math.round(ddx1 * m);
			dy2 = -(int) Math.round(ddx2 * m);
		} else {
			ddx1 = Math.sqrt(r1 * r1 / (1 + m * m));
			ddx2 = Math.sqrt(r2 * r2 / (1 + m * m));
			dx1 = (int) Math.round(ddx1);
			dx2 = -(int) Math.round(ddx2);
			dy1 = (int) Math.round(ddx1 * m);
			dy2 = -(int) Math.round(ddx2 * m);
		}

		if ((settings.getBondDrawMode() == DisplaySettings.LINE)
				|| settings.getFastRendering()) {
			gc.setColor(atom1.getType().getColor());
			gc.drawLine(x1 + dx1, y1 + dy1, xmp, ymp);
			gc.setColor(atom2.getType().getColor());
			gc.drawLine(x2 + dx2, y2 + dy2, xmp, ymp);
			return;
		}

		double halfbw = 0.5 * settings.getBondWidth()
							* settings.getBondScreenScale();

		if (halfbw >= 0.5) {

			int npoints = 4;
			int xpoints[] = new int[npoints];
			int ypoints[] = new int[npoints];

			// slope of lines that make up the bond ends and midpoint line
			double slope = -run / rise;

			tmp = Math.sqrt(halfbw * halfbw / (1 + slope * slope));

			deltaX = (int) Math.round(tmp);
			if (deltaX == 0) {
				deltaY = (int) Math.round(halfbw);
			} else {
				deltaY = (int) Math.round(tmp * slope);
			}

			xpoints[0] = (x1 + dx1) + deltaX;
			ypoints[0] = (y1 + dy1) + deltaY;

			xpoints[1] = (x1 + dx1) - deltaX;
			ypoints[1] = (y1 + dy1) - deltaY;

			xpoints[2] = xmp - deltaX;
			ypoints[2] = ymp - deltaY;

			xpoints[3] = xmp + deltaX;
			ypoints[3] = ymp + deltaY;

			Polygon poly1 = new Polygon(xpoints, ypoints, 4);

			xpoints[0] = (x2 + dx2) + deltaX;
			ypoints[0] = (y2 + dy2) + deltaY;

			xpoints[1] = (x2 + dx2) - deltaX;
			ypoints[1] = (y2 + dy2) - deltaY;

			Polygon poly2 = new Polygon(xpoints, ypoints, 4);

			switch (settings.getBondDrawMode()) {
			case DisplaySettings.WIREFRAME :
				gc.setColor(atom1.getType().getColor());
				gc.drawPolygon(poly1);
				gc.setColor(atom2.getType().getColor());
				gc.drawPolygon(poly2);
				break;

			case DisplaySettings.SHADING :
				for (int i = (int) (2.0 * halfbw); i > -1; i--) {
					double len = i / (2.0 * halfbw);
					int red1 =
						(int) ((float) atom1.getType().getColor().getRed()
							* (1.0f - len));
					int green1 =
						(int) ((float) atom1.getType().getColor().getGreen()
							* (1.0f - len));
					int blue1 =
						(int) ((float) atom1.getType().getColor().getBlue()
							* (1.0f - len));

					// Bitwise masking to make color model:
					int model1 = -16777216 | red1 << 16 | green1 << 8 | blue1;

					int red2 =
						(int) ((float) atom2.getType().getColor().getRed()
							* (1.0f - len));
					int green2 =
						(int) ((float) atom2.getType().getColor().getGreen()
							* (1.0f - len));
					int blue2 =
						(int) ((float) atom2.getType().getColor().getBlue()
							* (1.0f - len));

					// Bitwise masking to make color model:
					int model2 = -16777216 | red2 << 16 | green2 << 8 | blue2;

					double dX = Math.round(tmp);
					double dY = 0.0;
					if ((int) dX == 0) {
						dY = Math.round(halfbw);
					} else {
						dY = Math.round(tmp * slope);
					}
					int dXi = (int) (2.0 * dX * len);
					int dYi = (int) (2.0 * dY * len);

					gc.setColor(new Color(model1));

					xpoints[0] = x1 + dx1 + dXi;
					ypoints[0] = y1 + dy1 + dYi;

					xpoints[1] = x1 + dx1 - dXi;
					ypoints[1] = y1 + dy1 - dYi;

					xpoints[2] = xmp - dXi;
					ypoints[2] = ymp - dYi;

					xpoints[3] = xmp + dXi;
					ypoints[3] = ymp + dYi;

					Polygon polya = new Polygon(xpoints, ypoints, 4);

					gc.fillPolygon(polya);

					gc.setColor(new Color(model2));

					xpoints[0] = (x2 + dx2) + dXi;
					ypoints[0] = (y2 + dy2) + dYi;

					xpoints[1] = (x2 + dx2) - dXi;
					ypoints[1] = (y2 + dy2) - dYi;

					Polygon polyb = new Polygon(xpoints, ypoints, 4);

					gc.fillPolygon(polyb);

				}
				break;

			/*
			 * we already did the LINE mode above to save calculation time
			 * so we don't need to do it here.
			 */
			default :
				gc.setColor(atom1.getType().getColor());
				gc.fillPolygon(poly1);
				gc.setColor(settings.getOutlineColor());
				gc.drawPolygon(poly1);
				gc.setColor(atom2.getType().getColor());
				gc.fillPolygon(poly2);
				gc.setColor(settings.getOutlineColor());
				gc.drawPolygon(poly2);
				break;
			}

		} else {

			gc.setColor(atom1.getType().getColor());
			gc.drawLine(x1 + dx1, y1 + dy1, xmp, ymp);
			gc.setColor(atom2.getType().getColor());
			gc.drawLine(x2 + dx2, y2 + dy2, xmp, ymp);
		}
	}

}
