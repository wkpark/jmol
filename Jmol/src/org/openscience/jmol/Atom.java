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
package org.openscience.jmol;

import org.openscience.jmol.Bspt;
import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.render.AtomShape;
import org.openscience.jmol.render.JmolAtom;
import java.util.Vector;
import java.util.Enumeration;
import javax.vecmath.Point3d;
import javax.vecmath.Matrix4d;

/**
 * Stores and manipulations information and properties of
 * atoms.
 */
public class Atom extends org.openscience.cdk.Atom
  implements Bspt.Tuple, JmolAtom {

  /**
   * Creates an atom with the given type.
   *
   * @param the type of this atom.
   */
  public Atom(DisplayControl control, Atom atom, int atomNumber,
              double x, double y, double z, ProteinProp pprop) {
    super(atom.getSymbol(), new Point3d(x, y, z));
    super.setID(atom.getAtomTypeName());
    this.baseAtomType = atom.baseAtomType;
    this.atomNumber = atomNumber;
    this.pprop = pprop;
    this.control = control;
  }

  public Atom(DisplayControl control, org.openscience.cdk.Atom atom) {
    super(atom.getSymbol(), atom.getPoint3D());
    super.setAtomicNumber(atom.getAtomicNumber());
    super.setAtomTypeName(atom.getAtomTypeName());
    this.atomNumber = atomNumber;
    String atomName = atom.getAtomTypeName();
    if (atomName == null) {
        atomName = atom.getSymbol();
    }
    if (!atomName.equals("")) {
        this.baseAtomType = BaseAtomType.get(atomName, atom.getSymbol(), 
                                             atom.getAtomicNumber(),
                                             atom.getExactMass(),
                                             atom.getVanderwaalsRadius(),
                                             atom.getCovalentRadius());
    } else {
        this.baseAtomType = BaseAtomType.get(atom.getAtomicNumber());
    }
    this.control = control;
  }

  /**
   * Returns the atom's number.
   */
  public int getAtomNumber() {
    return atomNumber;
  }

  /**
   * Returns the atom's number.
   */
  public void setAtomNumber(int number) {
    this.atomNumber = number;
  }

  /**
   * Returns whether this atom is a hydrogen atom.
   *
   * @return true if this atom is a hydrogen atom.
   */
  public boolean isHydrogen() {
    return getAtomicNumber() == 1;
  }
  
  ProteinProp pprop = null;
  public ProteinProp getProteinProp() {
    return pprop;
  }

  public void setProteinProp(ProteinProp pprop) {
    this.pprop = pprop;
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
  public Vector getAtomicProperties() {
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
    vectorScaled.scaleAdd(2.0, vector, getPoint3D());
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
    if (bondOrder != 1) {
      int[] bondOrdersNew = new int[i + 1];
      if (bondOrders != null)
        System.arraycopy(bondOrders, 0, bondOrdersNew, 0, bondOrders.length);
      bondOrders = bondOrdersNew;
      // bond orders are stored one less so that when we allocate a new
      // array the default values will be 0 -- a bond order of 1
      bondOrders[i] = bondOrder-1;
    }
  }

  public void bondMutually(Atom atom2, int bondOrder) {
    if (isBondedAtom(atom2))
      return;
    addBondedAtom(atom2, bondOrder);
    atom2.addBondedAtom(this, bondOrder);
  }

  public boolean isBondedAtom(Atom toAtom) {
    if (bondedAtoms != null)
      for (int i = bondedAtoms.length; --i >= 0; )
        if (bondedAtoms[i] == toAtom)
          return true;
    return false;
  }

  /**
   * Returns the list of atoms to which this atom is bonded.
   */
  public Atom[] getBondedAtoms() {
    return bondedAtoms;
  }

  public Atom getBondedAtom(int index) {
    return bondedAtoms[index];
  }

  /**
   * Clears the bonded atoms list.
   */
  public void clearBondedAtoms() {
    bondedAtoms = null;
    bondOrders = null;
  }

  public int getBondOrder(int i) {
    return
      (bondOrders==null || bondOrders.length <= i) ? 1 : bondOrders[i] + 1;
  }

  public int getBondOrder(Atom atom2) {
    if (bondedAtoms != null) {
      for (int i = 0; i < bondedAtoms.length; ++i) {
        if (bondedAtoms[i] == atom2)
          return
            (bondOrders==null || bondOrders.length<=i) ? 1 : bondOrders[i] + 1;
      }
    }
    return 0;
  }

  public int getBondedCount() {
    return (bondedAtoms == null) ? 0 : bondedAtoms.length;
  }

  private DisplayControl control;
  private AtomShape atomShape;

  public AtomShape getAtomShape() {
    if (atomShape == null)
      throw new NullPointerException();
    return atomShape;
  }

  public boolean isSelected() {
    return control.isSelected(atomNumber);
  }

  public void delete() {
    // this atom has been deleted ...
    // so notify bonded atoms so that they can make appropriate adjustments
    if (bondedAtoms == null)
      return;
    for (int i = bondedAtoms.length; --i >= 0; )
      bondedAtoms[i].deleteBondedAtom(this);
  }

  public void deleteBondedAtom(Atom atomDeleted) {
    if (bondedAtoms.length == 1) {
      if (bondedAtoms[0] != atomDeleted)
        System.out.println("Atom.deleteBondedAtom() - inconsistent #1");
      bondedAtoms = null;
      bondOrders = null;
      return;
    }
    int numBondsNew = bondedAtoms.length - 1;
    Atom[] bondedAtomsNew = new Atom[numBondsNew];
    // in this case, just allocate the bond orders
    int[] bondOrdersNew = new int[numBondsNew];
    int iNew = 0;
    for (int i = 0; i < bondedAtoms.length; ++i) {
      if (bondedAtoms[i] == atomDeleted) {
        getAtomShape().deleteBondedAtomShape(atomDeleted.getAtomShape());
      } else {
        bondedAtomsNew[iNew] = bondedAtoms[i];
        if (bondOrders != null && bondOrders.length > i)
          bondOrdersNew[iNew] = bondOrders[i];
        ++iNew;
      }
    }
    if (iNew != bondedAtomsNew.length)
      System.out.println("Atom.deleteBondedAtom() - inconsistent #2");
    bondedAtoms = bondedAtomsNew;
    bondOrders = bondOrdersNew;
  }

  private BaseAtomType baseAtomType;

  /**
   * Position in screen space.
   */


  public int getScreenX() {
    return atomShape == null ? 0 : atomShape.x;
  }

  public int getScreenY() {
    return atomShape == null ? 0 : atomShape.y;
  }

  public int getScreenZ() {
    return atomShape == null ? 0 : atomShape.z;
  }

  public int getScreenDiameter() {
    return atomShape == null ? 0 : atomShape.diameter;
  }

  public int getScreenRadius() {
    return atomShape == null ? 0 : atomShape.diameter / 2;
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

  public double getDimValue(int dim) {
    Point3d point = getPoint3D();
    return (dim == 0) ? point.x : (dim == 1) ? point.y : point.z;
  }

}


