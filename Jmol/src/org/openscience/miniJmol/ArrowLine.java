
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
package org.openscience.miniJmol;

import java.awt.Color;
import java.awt.Graphics;

class ArrowLine {

	private static Color vectorColor = Color.black;

	private static float radiusScale = 1.0f;
	private static float lengthScale = 1.0f;

	private boolean aStart = false;
	private boolean aEnd = true;

	private double length = -1;
	private double ctheta = 0;
	private double stheta = 0;

	private int x1;
	private int y1;
	private int x2;
	private int y2;
	private int r;
	private int s;

	static int[] xpoints = new int[4];
	static int[] ypoints = new int[4];

	static void setVectorColor(Color c) {
		vectorColor = c;
	}

	static void setLengthScale(float ls) {
		lengthScale = ls;
	}

	static float getLengthScale() {
		return lengthScale;
	}

	static void setRadiusScale(float rs) {
		radiusScale = rs;
	}

	static float getRadiusScale() {
		return radiusScale;
	}

	public ArrowLine(Graphics gc, int x1, int y1, int x2, int y2,
			boolean arrowStart, boolean arrowEnd, int radius, int size) {

		this.r = (int) (radius * radiusScale);
		this.s = (int) (size * lengthScale);
		this.aStart = arrowStart;
		this.aEnd = arrowEnd;
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;

		precalc();
		gc.setColor(vectorColor);
		paintLine(gc);
		paintArrows(gc);

	}


	private void precalc() {

		if (length < 0) {
			double dy = y2 - y1;
			double dx = x2 - x1;

			length = Math.sqrt(Math.pow(dx, 2.0) + Math.pow(dy, 2.0));

			if (Math.abs(length - 0.0) < Double.MIN_VALUE) {
				return;
			}

			ctheta = dx / length;
			stheta = dy / length;
		}
	}

	public void paintLine(Graphics gc) {
		gc.drawLine(x1, y1, x2, y2);
	}

	public void paintStartArrowHead(Graphics gc) {

		double ax = x1 + 0.5;
		double ay = y1 + 0.5;
		double px = r + 1.0;
		double py = 0.0;
		double ly = s;
		double lx = r + s + s;
		double rx = lx;
		double ry = -ly;
		double mx = r + s + s / 2.0;
		double my = 0.0;
		double tx = px * ctheta - py * stheta;
		double ty = px * stheta + py * ctheta;
		px = tx;
		py = ty;
		tx = lx * ctheta - ly * stheta;
		ty = lx * stheta + ly * ctheta;
		lx = tx;
		ly = ty;
		tx = rx * ctheta - ry * stheta;
		ty = rx * stheta + ry * ctheta;
		rx = tx;
		ry = ty;
		tx = mx * ctheta - my * stheta;
		ty = mx * stheta + my * ctheta;
		mx = tx;
		my = ty;
		px += ax;
		py += ay;
		mx += ax;
		my += ay;
		lx += ax;
		ly += ay;
		rx += ax;
		ry += ay;
		xpoints[0] = (int) px;
		xpoints[1] = (int) lx;
		xpoints[2] = (int) mx;
		xpoints[3] = (int) rx;
		ypoints[0] = (int) py;
		ypoints[1] = (int) ly;
		ypoints[2] = (int) my;
		ypoints[3] = (int) ry;
		gc.fillPolygon(xpoints, ypoints, 4);
	}

	public void paintEndArrowHead(Graphics gc) {

		double ax = x1 + 0.5;
		double ay = y1 + 0.5;
		double px = length - (r + 1);
		double py = 0;
		double ly = s;
		double lx = length - (r + s + s);
		double rx = lx;
		double ry = -ly;
		double mx = length - (r + s + s / 2);
		double my = 0;
		double tx = px * ctheta - py * stheta;
		double ty = px * stheta + py * ctheta;
		px = tx;
		py = ty;
		tx = lx * ctheta - ly * stheta;
		ty = lx * stheta + ly * ctheta;
		lx = tx;
		ly = ty;
		tx = rx * ctheta - ry * stheta;
		ty = rx * stheta + ry * ctheta;
		rx = tx;
		ry = ty;
		tx = mx * ctheta - my * stheta;
		ty = mx * stheta + my * ctheta;
		mx = tx;
		my = ty;
		px += ax;
		py += ay;
		mx += ax;
		my += ay;
		lx += ax;
		ly += ay;
		rx += ax;
		ry += ay;
		xpoints[0] = (int) px;
		xpoints[1] = (int) lx;
		xpoints[2] = (int) mx;
		xpoints[3] = (int) rx;
		ypoints[0] = (int) py;
		ypoints[1] = (int) ly;
		ypoints[2] = (int) my;
		ypoints[3] = (int) ry;
		gc.fillPolygon(xpoints, ypoints, 4);
	}

	public void paintArrows(Graphics gc) {

		if (Math.abs(length - 0.0) < Double.MIN_VALUE) {
			return;
		}
		if (aStart) {
			paintStartArrowHead(gc);
		}
		if (aEnd) {
			paintEndArrowHead(gc);
		}
	}
}


