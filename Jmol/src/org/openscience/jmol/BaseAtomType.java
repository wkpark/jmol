
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
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * The basic properties of an atom are represented by the atom
 * type. Each atom type is identified with a unique string label.  To
 * ensure that only one atom type exists for each label, atom types
 * can not be created, but only retrieved from a static pool of
 * BaseAtomTypes. If the pool does contain an BaseAtomType for a given
 * label, one will be created upon the first retreival.
 */
public class BaseAtomType extends org.openscience.cdk.AtomType {

  /**
   * Gets the BaseAtomType corresponding to the given name and sets
   * its values to the parameters.
   *
   * @param name the name of this atom type (e.g. CA for alpha carbon)
   * @param root the root of this atom type (e.g. C for alpha carbon)
   * @param atomicNumber the atomic number (usually the number of
   *        protons of the root)
   * @param mass the atomic mass
   * @param vdwRadius the van der Waals radius (helps determine drawing size)
   * @param covalentRadius the covalent radius (helps determine bonding)
   * @param color the color for drawing
   * @return the atom type corresponding to the name.
   */
  public static BaseAtomType get(String name, String root, int atomicNumber,
      double mass, double vdwRadius, double covalentRadius, Color color) {
    BaseAtomType at = get(name, root);
    at.set(root, atomicNumber, mass, vdwRadius, covalentRadius, color);
    return at;
  }

  /**
   * Returns the first occurence of an AtomType with the given
   * atomic number.
   *
   * @param atomicNumber  atomic number of the atom type to find.
   * @return the first atom type with atomic number matching
   *         parameter, or null if not found.
   */
  public static BaseAtomType get(int atomicNumber) {

    Enumeration iter = typePool.elements();
    while (iter.hasMoreElements()) {
      BaseAtomType at = (BaseAtomType) iter.nextElement();
      if (atomicNumber == at.getAtomicNumber()) {
        BaseAtomType atr = at;
        return atr;
      }
    }
    return null;
  }

  /**
   * Returns the BaseAtomType corresponding to the name given.
   * If a corresponding atom type does not exist, one will be created
   * with default values.
   *
   * @param name the name of this atom type (e.g. CA for alpha carbon)
   * @return the atom type corresponding to the name.
   */
  public static BaseAtomType get(String name, String root) {

    if (exists(name)) {
      return (BaseAtomType) typePool.get(name);
    }
    BaseAtomType at = new BaseAtomType(name, root);
    typePool.put(name, at);
    return at;
  }

  /**
   * Returns true if the BaseAtomType with the name given exists.
   *
   * @param name the name of this atom type (e.g. CA for alpha carbon).
   * @return true if the atom type exists.
   */
  public static boolean exists(String name) {
    return typePool.containsKey(name);
  }

  /**
   * Creates an atom type with the given name.
   *
   * @param name the name of this atom type (e.g. CA for alpha carbon)
   */
  private BaseAtomType(String name, String root) {
    super(name, root);
  }

  /**
   * Sets the values of this atom type.
   *
   * @param root the root of this atom type (e.g. C for alpha carbon)
   * @param atomicNumber the atomic number (usually the number of protons of the root)
   * @param mass the atomic mass
   * @param vdwRadius the van der Waals radius (helps determine drawing size)
   * @param covalentRadius the covalent radius (helps determine bonding)
   * @param color the color for drawing
   */
  public void set(String root, int atomicNumber, double mass,
      double vdwRadius, double covalentRadius, Color color) {

    super.setSymbol(root);
    super.setAtomicNumber(atomicNumber);
    super.setExactMass(mass);
    this.vdwRadius = vdwRadius;
    this.covalentRadius = covalentRadius;
    this.color = color;
  }

  /**
   * Converts the string into the BaseAtomType.
   *
   * @param s1 the string to be converted to an BaseAtomType
   */
  public static BaseAtomType parse(String s1) {

    StringTokenizer st1 = new StringTokenizer(s1, "\t ,;");

    String localName = st1.nextToken();
    String root      = st1.nextToken();
    BaseAtomType at = get(localName, root);
    at.setAtomicNumber(Integer.parseInt(st1.nextToken()));
    at.setExactMass(Double.valueOf(st1.nextToken()).doubleValue());
    at.vdwRadius = Double.valueOf(st1.nextToken()).doubleValue();
    at.covalentRadius = Double.valueOf(st1.nextToken()).doubleValue();
    at.color = new Color(Integer.parseInt(st1.nextToken()),
        Integer.parseInt(st1.nextToken()), Integer.parseInt(st1.nextToken()));
    return at;
  }

  /**
   * Returns the name.
   */
  public String getName() {
    return super.getID();
  }

  /**
   * Sets the name.
   *
   * @param name the name
   */
  public void setName(String name) {
    super.setID(name);
  }

  /**
   * Returns the root.
   */
  public String getRoot() {
    return super.getSymbol();
  }

  /**
   * Sets the root.
   *
   * @param root the root
   */
  public void setRoot(String root) {
    super.setSymbol(root);
  }

  /**
   * Returns the mass.
   */
  public double getMass() {
    return getExactMass();
  }

  /**
   * Sets the mass.
   *
   * @param mass the mass
   */
  public void setMass(double mass) {
    super.setExactMass(mass);
  }

  /**
   * Returns the covalent radius.
   */
  public double getCovalentRadius() {
    return covalentRadius;
  }

  /**
   * Sets the covalent radius.
   *
   * @param cr the covalent radius
   */
  public void setCovalentRadius(double cr) {
    this.covalentRadius = cr;
  }

  /**
   * Returns the Van derWaals radius.
   */
  public double getVdwRadius() {
    return vdwRadius;
  }

  /**
   * Sets the Van derWaals radius.
   *
   * @param vr the Van derWaals Radius
   */
  public void setVdwRadius(double vr) {
    this.vdwRadius = vr;
  }

  /**
   * Returns the color.
   */
  public Color getColor() {
    return color;
  }

  /**
   * Sets the color.
   *
   * @param c the Color
   */
  public void setColor(Color c) {
    this.color = c;
  }

  /**
   * Returns true if this and the Object are equal.
   *
   * @param obj object for comparison.
   * @return true if the objects are equal.
   */
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }
    if (!(obj instanceof BaseAtomType)) {
      return false;
    }
    BaseAtomType at = (BaseAtomType) obj;
    boolean nameEqual = getName().equals(at.getName());
    boolean rootEqual = getRoot().equals(at.getRoot());
    boolean atomicNumberEqual = (atomicNumber == at.getAtomicNumber());
    boolean massEqual = (Double.doubleToLongBits(at.getMass())
                          == Double.doubleToLongBits(at.getMass()));
    boolean vdwRadiiEqual = (Double.doubleToLongBits(vdwRadius)
                              == Double.doubleToLongBits(at.vdwRadius));
    boolean covalentRadiiEqual =
      (Double.doubleToLongBits(covalentRadius)
        == Double.doubleToLongBits(at.covalentRadius));
    boolean colorEqual = color.equals(at.color);
    return (nameEqual && rootEqual && atomicNumberEqual && massEqual
        && vdwRadiiEqual && covalentRadiiEqual && colorEqual);
  }

  /**
   * Returns a hash code for this object.
   *
   * @return the hash code.
   */
  public int hashCode() {

    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getName().hashCode();
      result = 37 * result + getRoot().hashCode();
      result = 37 * result + getAtomicNumber();
      long longHashValue = Double.doubleToLongBits(getMass());
      result = 37 * result + (int) (longHashValue ^ (longHashValue >> 32));
      longHashValue = Double.doubleToLongBits(vdwRadius);
      result = 37 * result + (int) (longHashValue ^ (longHashValue >> 32));
      longHashValue = Double.doubleToLongBits(covalentRadius);
      result = 37 * result + (int) (longHashValue ^ (longHashValue >> 32));
      result = 37 * result + color.hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  /**
   * The hash code for this object. It is lazily initialized.
   */
  private volatile int hashCode = 0;

  /**
   * Returns a String representation of this atom type.
   */
  public String toString() {

    StringBuffer sb1 = new StringBuffer();
    sb1.append(getName());
    sb1.append('\t');
    sb1.append(getRoot());
    sb1.append('\t');
    sb1.append(Integer.toString(getAtomicNumber()));
    sb1.append('\t');
    sb1.append(Double.toString(getMass()));
    sb1.append('\t');
    sb1.append(Double.toString(vdwRadius));
    sb1.append('\t');
    sb1.append(Double.toString(covalentRadius));
    sb1.append('\t');
    sb1.append(Integer.toString(color.getRed()));
    sb1.append('\t');
    sb1.append(Integer.toString(color.getGreen()));
    sb1.append('\t');
    sb1.append(Integer.toString(color.getBlue()));
    return sb1.toString();
  }

  /**
   * Van der Waals radius.
   */
  protected double vdwRadius;

  /**
   * Covalent radius.
   */
  protected double covalentRadius;

  /**
   * Draw color.
   */
  protected Color color = Color.white;

  /**
   * Static pool of atom types.
   */
  private static Hashtable typePool = new Hashtable();
}
