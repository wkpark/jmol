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

import java.awt.Color;
import java.util.Vector;
import java.util.Enumeration;
import javax.vecmath.Point3f;
import javax.vecmath.Point3d;
import javax.vecmath.Matrix4f;
import org.openscience.cdk.tools.IsotopeFactory;

/**
 * Stores and manipulations information and properties of
 * atoms.
 */
public class Atom extends org.openscience.cdk.Atom {

   public Atom(BaseAtomType at) {
    super(at.getName());
    try {
        IsotopeFactory.getInstance().configure(this);
    } catch (Exception e) {
        // failed to configure atom
        System.err.println("Error configuration of atom: " + at.getName());
    }
    this.atomType = new AtomType(at);
  }

  /**
   * Creates an atom with the given type.
   *
   * @param the type of this atom.
   */
  public Atom(BaseAtomType atomType, int atomNumber,
              float x, float y, float z) {
    super(atomType.getName(), new Point3d(x, y, z));
    try {
        IsotopeFactory.getInstance().configure(this);
    } catch (Exception e) {
        // failed to configure atom
        System.err.println("Error configuration of atom: " + atomType.getName());
    }
    this.atomType = new AtomType(atomType);
    this.atomNumber = atomNumber;
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
   * Returns this atom's color.
   *
   * @return the color of this atom.
   */
  public Color getColor() {
    return atomType.getColor();
  }

  /**
   * Sets this atom's color.
   *
   * @param the color to set the atom.
   */
  public void setColor(Color color) {
    atomType.setColor(color);
  }

  /**
   * Returns the atom's position.
   */
  public Point3f getPosition() {
    return new Point3f((float)getX3D(), (float)getY3D(), (float)getZ3D());
  }

  /**
   * Returns the atom's on-screen position. Note: the atom must
   * first be transformed. Otherwise, a point at the origin is returned.
   */
  public Point3f getScreenPosition() {
    return screenPosition;
  }

  /**
   * Returns the atom's vector, or null if not set.
   */
  public boolean hasVector() {
    return vector != null;
  }

  public Point3f getVector() {
    if (vector == null)
      return null;
    return new Point3f(vector);
  }

  private static final Point3f zeroPoint = new Point3f();
  public float getVectorMagnitude() {
    // mth 2002 nov
    // I don't know why they were scaling by two,
    // but that is the way it was working
    if (vector == null)
      return 0f;
    return vector.distance(zeroPoint) * 2.0f;
  }

  public Point3f getScaledVector() {
    Point3f vectorScaled = new Point3f();
    vectorScaled.scaleAdd(2.0f, vector, getPosition());
    return vectorScaled;
  }

  /**
   * Sets the atom's vector.
   */
  public void setVector(Point3f vector) {

    if (vector == null) {
      this.vector = null;
    } else {
      if (this.vector == null) {
        this.vector = new Point3f();
      }
      this.vector.set(vector);
    }
  }

  /**
   * Returns the atom's on-screen vibrational vector. Note: the atom must
   * first be transformed. Otherwise, a point at the origin is returned.
   */
  public Point3f getScreenVector() {
    return screenVector;
  }

  /**
   * Sets the atom's screen position by transforming the atom's position by
   * the given matrix.
   */
  public void transform(Matrix4f transformationMatrix,
                        DisplaySettings settings ) {

    transformationMatrix.transform(getPosition(), screenPosition);
    screenX = (int) screenPosition.x;
    screenY = (int) screenPosition.y;
    screenZ = (int) screenPosition.z;
    screenDiameter =
      (int) (2.0f
        * settings.getCircleRadius(screenZ,
                                   atomType.getBaseAtomType().getVdwRadius()));
    if (vector != null) {
      screenVector.set(getScaledVector());
      transformationMatrix.transform(screenVector);
    }
  }

  /**
     * Adds an atom to this atom's bonded list.
     */
  public void addBondedAtom(Atom toAtom, int bondOrder) {

    if (bondedAtoms == null) {
      bondedAtoms = new Vector();
    }
    if (bondOrders == null) {
      bondOrders = new Vector();
    }
    bondedAtoms.addElement(toAtom);
    bondOrders.addElement(new Integer(bondOrder));
  }

  /**
   * Returns the list of atoms to which this atom is bonded.
   */
  public Enumeration getBondedAtoms() {

    if (bondedAtoms == null) {
      return new NoBondsEnumeration();
    } else {
      return bondedAtoms.elements();
    }
  }

  /**
   * Returns the list of bond orders.
   */
  public Enumeration getBondOrders() {

    if (bondOrders == null) {
      return new NoBondsEnumeration();
    } else {
      return bondOrders.elements();
    }
  }

  /**
   * Clears the bonded atoms list.
   */
  public void clearBondedAtoms() {
    if (bondedAtoms != null) {
      bondedAtoms.removeAllElements();
      bondOrders.removeAllElements();
    }
  }

  /**
   * Returns true if the two atoms are within the distance fudge
   * factor of each other.
   */
  public static boolean closeEnoughToBond(Atom atom1, Atom atom2,
      float distanceFudgeFactor) {

    if (atom1 != atom2) {
      float squaredDistanceBetweenAtoms =
        atom1.getPosition().distanceSquared(atom2.getPosition());
      float bondingDistance =
        distanceFudgeFactor
          * ((float) atom1.atomType.getBaseAtomType().getCovalentRadius()
             + (float) atom2.atomType.getBaseAtomType().getCovalentRadius());

      if (squaredDistanceBetweenAtoms <= bondingDistance * bondingDistance) {
        return true;
      }
    }
    return false;
  }

  private AtomType atomType;

  /**
   * Position in screen space.
   */
  private Point3f screenPosition = new Point3f();

  public int screenX;
  public int screenY;
  public int screenZ;
  public int screenDiameter;

  /**
   * A list of atoms to which this atom is bonded. Lazily initialized.
   */
  private Vector bondedAtoms = null;

  /**
   * A list of bond orders. Lazily initialized.
   */
  private Vector bondOrders = null;

  /**
   * Vibrational vector in world space.
   */
  private Point3f vector = null;
  
  /**
   * Atom number in set of all atoms. Not the atomic number!
   */
  private int atomNumber;

  /**
   * Vibrational vector in screen space.
   */
  private Point3f screenVector = new Point3f();

  /**
   * A list of properties
   */
  private Vector properties = new Vector();

  public String toString() {
    String type = getType().getRoot();
    return "Atom{" + type + " #" + getAtomNumber() + " @" +
      getPosition() + " @" + screenX + "," + screenY + "," + screenZ + "}";
  }

  static class NoBondsEnumeration implements Enumeration {

    public boolean hasMoreElements() {
      return false;
    }

    public Object nextElement() throws java.util.NoSuchElementException {
      throw new java.util.NoSuchElementException();
    }
  }
}


