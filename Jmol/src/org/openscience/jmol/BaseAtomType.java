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
      double mass, double vdwRadius, double covalentRadius) {
    BaseAtomType at = get(name, root);
    at.set(root, atomicNumber, mass, vdwRadius, covalentRadius);
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
      double vdwRadius, double covalentRadius) {

    super.setSymbol(root);
    super.setAtomicNumber(atomicNumber);
    super.setExactMass(mass);
    super.setVanderwaalsRadius(vdwRadius);
    super.setCovalentRadius(covalentRadius);
  }

  public static BaseAtomType get(org.openscience.cdk.AtomType at) {
      BaseAtomType bat = new BaseAtomType(at.getID(), at.getSymbol());
      bat.setAtomicNumber(at.getAtomicNumber());
      bat.setExactMass(at.getExactMass());
      bat.setVanderwaalsRadius(at.getVanderwaalsRadius());
      bat.setCovalentRadius(at.getCovalentRadius());
      Object o = at.getProperty("org.openscience.jmol.color");
      if (o != null && o instanceof Color) {
          bat.color = (Color)o;
      };
      return bat;
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
    at.setVanderwaalsRadius(Double.valueOf(st1.nextToken()).doubleValue());
    at.setCovalentRadius(Double.valueOf(st1.nextToken()).doubleValue());
    at.color = new Color(Integer.parseInt(st1.nextToken()),
        Integer.parseInt(st1.nextToken()), Integer.parseInt(st1.nextToken()));
    return at;
  }

  /**
   * Returns the Van derWaals radius.
   */
  public double getVdwRadius() {
    return super.getVanderwaalsRadius();
  }

  /**
   * Sets the Van derWaals radius.
   *
   * @param vr the Van derWaals Radius
   */
  public void setVdwRadius(double vr) {
    super.setVanderwaalsRadius(vr);
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
    boolean nameEqual = getID().equals(at.getID());
    boolean rootEqual = getSymbol().equals(at.getSymbol());
    boolean atomicNumberEqual = (atomicNumber == at.getAtomicNumber());
    boolean massEqual = (Double.doubleToLongBits(at.getExactMass())
                      == Double.doubleToLongBits(at.getExactMass()));
    boolean vdwRadiiEqual = (Double.doubleToLongBits(getVanderwaalsRadius())
                          == Double.doubleToLongBits(at.getVanderwaalsRadius()));
    boolean covalentRadiiEqual =
          (Double.doubleToLongBits(getCovalentRadius())
        == Double.doubleToLongBits(at.getCovalentRadius()));
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
      result = 37 * result + getID().hashCode();
      result = 37 * result + getSymbol().hashCode();
      result = 37 * result + getAtomicNumber();
      long longHashValue = Double.doubleToLongBits(getExactMass());
      result = 37 * result + (int) (longHashValue ^ (longHashValue >> 32));
      longHashValue = Double.doubleToLongBits(getVanderwaalsRadius());
      result = 37 * result + (int) (longHashValue ^ (longHashValue >> 32));
      longHashValue = Double.doubleToLongBits(getCovalentRadius());
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
    sb1.append(getID());
    sb1.append('\t');
    sb1.append(getSymbol());
    sb1.append('\t');
    sb1.append(Integer.toString(getAtomicNumber()));
    sb1.append('\t');
    sb1.append(Double.toString(getExactMass()));
    sb1.append('\t');
    sb1.append(Double.toString(getVanderwaalsRadius()));
    sb1.append('\t');
    sb1.append(Double.toString(getCovalentRadius()));
    sb1.append('\t');
    sb1.append(Integer.toString(color.getRed()));
    sb1.append('\t');
    sb1.append(Integer.toString(color.getGreen()));
    sb1.append('\t');
    sb1.append(Integer.toString(color.getBlue()));
    return sb1.toString();
  }

  /**
   * Draw color.
   */
  protected Color color = Color.white;
  public Color getColor() {
    return color;
  }

  /**
   * Static pool of atom types.
   */
  private static Hashtable typePool = new Hashtable();
}
