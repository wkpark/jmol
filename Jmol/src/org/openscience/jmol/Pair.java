
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

public class Pair {

	private AtomType ata, atb;

	/**
	 * class to do pairwise-interaction utilities for two atoms
	 * that are not necessarily (but possibly are) bonded.
	 */
	public Pair(AtomType ata, AtomType atb) {
		this.ata = ata;
		this.atb = atb;
	}

	/**
	 * Returns the distance in molecule-space between two atoms
	 *
	 * @param qax the x coordinate of atom A
	 * @param qay the y coordinate of atom A
	 * @param qaz the z coordinate of atom A
	 * @param qbx the x coordinate of atom B
	 * @param qby the y coordinate of atom B
	 * @param qbz the z coordinate of atom B
	 */
	public float getDistance(float qax, float qay, float qaz, float qbx,
			float qby, float qbz) {

		float dx = qax - qbx;
		float dy = qay - qby;
		float dz = qaz - qbz;
		float dx2 = dx * dx;
		float dy2 = dy * dy;
		float dz2 = dz * dz;
		float rab2 = dx2 + dy2 + dz2;
		float dist = (float) Math.sqrt(rab2);
		return dist;
	}

	/**
	 * Returns the average of the two screen radii in the pair
	 *
	 * @param z1 the screen-transformed z coordinate of atom 1
	 * @param z2 the screen-transformed z coordinate of atom 2
	 */
	public float getAvgRadius(DisplaySettings settings, int z1, int z2) {
		return (settings.getCircleRadius(z1, ata.getBaseAtomType().getVdwRadius()) + settings.getCircleRadius(z2, atb.getBaseAtomType().getVdwRadius()))
				/ 2.0f;
	}
}
