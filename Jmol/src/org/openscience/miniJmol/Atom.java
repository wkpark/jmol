/*
 * Atom.java
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

import javax.vecmath.Point3f;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This class stores and manipulations information and properties of
 * atoms.
 */
class Atom {

	/**
	 * Creates an atom at the given position with the given atom type.
	 *
	 * @param at the atom type.
	 * @param position the Cartesian coordinate of the atom.
	 */
	public Atom(BaseAtomType at, Point3f position) {
		type = at;
		this.position = position;
	}

	/**
	 * Creates an atom at the given position with the given atom type and atom number.
	 *
	 * @param at the atom type.
	 * @param position the Cartesian coordinate of the atom.
	 * @param atomNumber the number assigned to the atom.
	 */
	public Atom(BaseAtomType at, Point3f position, int atomNumber) {
		this(at, position);
		this.atomNumber = atomNumber;
	}

	/**
	 * Returns the atom's type.
	 */
	public BaseAtomType getType() {
		return type;
	}

	/**
	 * Returns the atom's position.
	 */
	public Point3f getPosition() {
		return position;
	}

	/**
	 * Returns the atom's on-screen position. Note: the atom must
	 * first be transformed. Otherwise, a point at the origin is returned.
	 */
	public Point3f getScreenPosition() {
		return screenPosition;
	}

	/**
	 * Returns the atom's number.
	 */
	public int getAtomNumber() {
		return atomNumber;
	}

	/**
	 * Sets the list of properties for this atom.
	 *
	 * @param propList a list of properties
	 */
	public void setProperties(Vector propList) {
		properties = propList;
	}

	/**
	 * Gets the list of properties for this atom.
	 *
	 * @returns a list of properties
	 */
	public Vector getProperties() {
		return properties;
	}

	/**
	 * Transforms the atom's position.
	 *
	 * @returns the z coordinate of transformed position.
	 */
	public float transform(Matrix3D transformationMatrix) {
		transformationMatrix.transform(position, screenPosition);
		return screenPosition.z;
	}

	/**
	 * Adds an atom to this atom's bonded list.
	 */
	public void addBondedAtom(Atom toAtom) {
		bondedAtoms.addElement(toAtom);
	}

	/**
	 * Returns the list of atoms to which this atom is bonded.
	 */
	public Enumeration getBondedAtoms() {
		return bondedAtoms.elements();
	}

	/**
	 * Clears the bonded atoms list.
	 */
	public void clearBondedAtoms() {
		bondedAtoms.clear();
	}

	/**
	 * Returns true if the two atoms are within the distance fudge
	 * factor of each other.
	 */
	public static boolean closeEnoughToBond(Atom atom1, Atom atom2, float distanceFudgeFactor) {
		if (atom1 != atom2) {
			float squaredDistanceBetweenAtoms = atom1.position.distanceSquared(atom2.position);
			float bondingDistance = distanceFudgeFactor*((float) atom1.type.getCovalentRadius() + 
														 (float) atom2.type.getCovalentRadius());
			
			if (squaredDistanceBetweenAtoms <= bondingDistance*bondingDistance) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The atom's type.
	 */
	private BaseAtomType type;
	/**
	 * The number assigned to this atom.
	 */
	private int atomNumber;
	/**
	 * This atom's Cartesian coordinate.
	 */
	private Point3f position;
	/**
	 * This atom's screen coordinate.
	 */
	private Point3f screenPosition = new Point3f();
	/**
	 * A list of atoms to which this atom is bonded.
	 */
	private Vector bondedAtoms = new Vector();
	/**
	 * A list of properties
	 */
	private Vector properties = new Vector();

}
