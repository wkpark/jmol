/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002  The Jmol Development Team
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
package org.openscience.jmol;

import org.openscience.jmol.render.AtomShape;
import java.awt.Color;
import java.util.Vector;
import java.util.Enumeration;
import javax.vecmath.Point3d;
import javax.vecmath.Matrix4d;
import org.openscience.cdk.tools.IsotopeFactory;

/**
 * Stores and manipulations information and properties of
 * atoms.
 */
public class Atom extends org.openscience.cdk.Atom {

  /**
   * Creates an atom with the given type.
   *
   * @param the type of this atom.
   */
  public Atom(BaseAtomType atomType, int atomNumber,
              double x, double y, double z) {
    super(atomType.getSymbol(), new Point3d(x, y, z));
    super.setID(atomType.getID());
    try {
        IsotopeFactory.getInstance().configure(this);
    } catch (Exception e) {
        // failed to configure atom
        System.err.println("Error configuration of atom: " +
                           atomType.getSymbol());
    }
    this.atomType = new AtomType(atomType);
    this.atomNumber = atomNumber;
    this.atomShape = new AtomShape(this);
  }

  /**
   * Returns the atom's number.
   */
  public int getAtomNumber() {
    return atomNumber;
  }

  /**
   * Returns this atom's base type.
   *
   * @return the base type of this atom.
   */
  public BaseAtomType getType() {
    return atomType.getBaseAtomType();
  }

  public double getVdwRadius() {
    double radius = atomType.getBaseAtomType().getVdwRadius();
    if (radius == 0) {
      System.out.println("Radius not defined -- defaulting to 1");
      radius = 1;
    }
    return radius;
  }

  /**
   * Returns whether this atom is a hydrogen atom.
   *
   * @return true if this atom is a hydrogen atom.
   */
  public boolean isHydrogen() {
    return getAtomicNumber() == 1;
  }
  
  /**
   * Adds a <code>PhysicalProperty</code> to this atom if not already defined.
   * If a <code>PhysicalProperty</code> with the same description already
   * exists, the property is not added.
   *
   * @param property the <code>PhysicalProperty</code> to be added.
   */
  public void addProperty(PhysicalProperty property) {
    if (!hasProperty(property.getDescriptor())) {
      properties.addElement(property);
    }
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
   * Returns the <code>PhysicalProperty</code> matching the description given.
   *
   * @return the <code>PhysicalProperty</code>, or null if not found.
   */
  public PhysicalProperty getProperty(String description) {

    Enumeration e = properties.elements();
    while (e.hasMoreElements()) {
      PhysicalProperty fp = (PhysicalProperty) e.nextElement();
      if (description.equals(fp.getDescriptor())) {
        return fp;
      }
    }
    return null;
  }

  /**
   * Returns the <code>PhysicalProperty</code>'s associated with this atom.
   *
   * @return a Vector of <code>PhysicalProperty</code>
   */
  public Vector getProperties() {
    return properties;
  }

  /**
   * Returns whether this atom has a property with the given description.
   *
   * @param description the property to be found.
   * @return true if a property with the description was found, else false.
   */
  public boolean hasProperty(String description) {
    if (getProperty(description) != null) {
      return true;
    }
    return false;
  }

  /**
   * Returns the atom's position.
   */
  public Point3d getPosition() {
    return point3D;
  }

  /**
   * Returns the atom's vector, or null if not set.
   */
  public boolean hasVector() {
    return vector != null;
  }

  public Point3d getVector() {
      return vector;
  }

  private static final Point3d zeroPoint = new Point3d();
  public double getVectorMagnitude() {
    // FIXME mth dec 2003 -- get these out of here and into AtomVectorShape
    // mth 2002 nov
    // I don't know why they were scaling by two,
    // but that is the way it was working
    if (vector == null)
      return 0f;
    return vector.distance(zeroPoint) * 2.0f;
  }

  public Point3d getScaledVector() {
    if (vector == null) {
      System.out.println("(vector == null) in getScaledVector");
      return null;
    }
    Point3d vectorScaled = new Point3d();
    vectorScaled.scaleAdd(2.0, vector, getPosition());
    return vectorScaled;
  }

  /**
   * Sets the atom's vector.
   */
  public void setVector(Point3d vector) {

    if (vector == null) {
      this.vector = null;
    } else {
      if (this.vector == null) {
        this.vector = new Point3d();
      }
      this.vector.set(vector);
    }
  }


  /**
     * Adds an atom to this atom's bonded list.
     */
  public void addBondedAtom(Atom toAtom, int bondOrder) {
    int i = 0;
    if (bondedAtoms == null) {
      bondedAtoms = new Atom[1];
    } else {
      i = bondedAtoms.length;
      Atom[] bondedAtomsNew = new Atom[i + 1];
      System.arraycopy(bondedAtoms, 0, bondedAtomsNew, 0, i);
      bondedAtoms = bondedAtomsNew;
    }
    bondedAtoms[i] = toAtom;
    if (bondOrder > 1) {
      int[] bondOrdersNew = new int[i + 1];
      if (bondOrders != null)
        System.arraycopy(bondOrders, 0, bondOrdersNew, 0, bondOrders.length);
      bondOrders = bondOrdersNew;
      // bond orders are stored one less so that when we allocate a new
      // array the default values will be 0 -- a bond order of 1
      bondOrders[i] = bondOrder-1;
    }
  }

  /**
   * Returns the list of atoms to which this atom is bonded.
   */
  public Atom[] getBondedAtoms() {
    return bondedAtoms;
  }

  /**
   * Clears the bonded atoms list.
   */
  public void clearBondedAtoms() {
    bondedAtoms = null;
    bondOrders = null;
  }

  public int getBondOrder(Atom atom2) {
    if (bondedAtoms != null) {
      for (int i = 0; i < bondedAtoms.length; ++i) {
        if (bondedAtoms[i] == atom2)
          return
            (bondOrders==null || bondOrders.length<=i) ? 1 : bondOrders[i] + 1;
      }
    }
    return -1;
  }

  public int getBondedCount() {
    return (bondedAtoms == null) ? 0 : bondedAtoms.length;
  }

  /**
   * Returns true if the two atoms are within the distance fudge
   * factor of each other.
   */
  public static boolean closeEnoughToBond(Atom atom1, Atom atom2,
      double distanceFudgeFactor) {

    if (atom1 != atom2) {
      double squaredDistanceBetweenAtoms =
        atom1.getPosition().distanceSquared(atom2.getPosition());
      double bondingDistance =
        distanceFudgeFactor
          * (atom1.atomType.getBaseAtomType().getCovalentRadius()
             + atom2.atomType.getBaseAtomType().getCovalentRadius());

      if (squaredDistanceBetweenAtoms <= bondingDistance * bondingDistance) {
        return true;
      }
    }
    return false;
  }

  public AtomShape atomShape;
  public AtomShape getAtomShape() {
    return atomShape;
  }

  private AtomType atomType;

  /**
   * Position in screen space.
   */

  public int getScreenX() {
    return atomShape.x;
  }

  public int getScreenY() {
    return atomShape.y;
  }

  public int getScreenZ() {
    return atomShape.z;
  }

  public int getScreenDiameter() {
    return atomShape.diameter;
  }

  public int getScreenRadius() {
    return atomShape.diameter / 2;
  }

  /**
   * An array of atoms to which this atom is bonded;
   */
  private Atom[] bondedAtoms = null;

  /**
   * A list of bond orders. Lazily initialized.
   * Don't get flustered here, it is pretty simple. Most bond orders are 1.
   * So I don't store bondOrders unless their value is >1.
   * Bond orders stored here are one less -- so bond order 1 is stored as 0
   * Look at the implementation of getBondOrder first and I think it is
   * pretty clear. Then look at addBondedAtom.
   */
  private int[] bondOrders = null;

  /**
   * Vibrational vector in world space.
   */
  private Point3d vector = null;
  
  /**
   * Atom number in set of all atoms. Not the atomic number!
   */
  private int atomNumber;

  /**
   * A list of properties
   */
  private Vector properties = new Vector();

  public String toString() {
    String type = getSymbol();
    return "Atom{" + type + " #" + getAtomNumber() + " @" +
      getPosition() + " @" + getScreenX() + ","
	+ getScreenY() + "," + getScreenZ() + "}";
  }
}


