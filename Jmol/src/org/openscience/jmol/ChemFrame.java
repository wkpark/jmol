
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

import java.awt.Graphics;
import java.awt.*;
import java.util.*;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

/**
 *  Data representation for a molecule in a particular set of coordinates.
 *
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class ChemFrame {

  private static float bondFudge = 1.12f;
  private static boolean AutoBond = true;
  private static Matrix4d mat;

  /*
     pickedAtoms and napicked are static because
     they deal with deformations or measurements that will persist
     across frames.
  */
  private boolean[] pickedAtoms;
  private int napicked;

  // This stuff can vary for each frame in the dynamics:

  private String info;                    // The title or info string for this frame.
  private Atom[] atoms;               // array of atom types
  private Vector properties = new Vector();
  private int numberAtoms = 0;

  /**
   * Returns whether the atom at the given index is picked.
   */
  boolean isAtomPicked(int index) {
    if (index >= pickedAtoms.length) {
      throw new IllegalArgumentException("isAtomPicked(): atom index to large");
    }
    return pickedAtoms[index];
  }
  
  /**
   * Returns the list of distance measurements.
   */
  Vector getDistanceMeasurements() {
    return dlist;
  }
  
  /**
   * List of distance measurements.
   */
  private Vector dlist;
  
  /**
   * Returns the list of angle measurements.
   */
  Vector getAngleMeasurements() {
    return alist;
  }
  
  /**
   * List of angle measurements.
   */
  private Vector alist;
  
  /**
   * Returns the list of dihedral measurements.
   */
  Vector getDihedralMeasurements() {
    return dhlist;
  }
  
  /**
   * List of dihedral measurements.
   */
  private Vector dhlist;

  private Point3f min;
  private Point3f max;

  /**@shapeType AggregationLink
  @associates <b>Vibration</b>*/
  static {
    mat = new Matrix4d();
    mat.setIdentity();
  }

  /**
   * returns the number of atoms that are currently in the "selected"
   * list for future operations
   */
  public int getNpicked() {
    return napicked;
  }

  static void setBondFudge(float bf) {
    bondFudge = bf;
  }

  static float getBondFudge() {
    return bondFudge;
  }

  static void setAutoBond(boolean ab) {
    AutoBond = ab;
  }

  static boolean getAutoBond() {
    return AutoBond;
  }

  static void matmult(Matrix4d matrix) {
    mat.mul(matrix, mat);
  }

  static void matscale(float xs, float ys, float zs) {

    Matrix4d matrix = new Matrix4d();
    matrix.setElement(0, 0, xs);
    matrix.setElement(1, 1, ys);
    matrix.setElement(2, 2, zs);
    matrix.setElement(3, 3, 1.0);
    mat.mul(matrix, mat);
  }

  static void mattranslate(float xt, float yt, float zt) {
    Matrix4d matrix = new Matrix4d();
    matrix.setTranslation(new Vector3d(xt, yt, zt));
    mat.add(matrix);
  }

  static void matunit() {
    mat.setIdentity();
  }

  /**
   * Constructor for a ChemFrame with a known number of atoms.
   *
   * @param na the number of atoms in the frame
   */
  public ChemFrame(int na) {
    this(na, true);
  }
  
  public ChemFrame(int na, boolean bondsEnabled) {

    atoms = new Atom[na];
    pickedAtoms = new boolean[na];
    for (int i = 0; i < na; i++) {
      pickedAtoms[i] = false;
    }
    this.bondsEnabled = bondsEnabled;
  }

  public ChemFrame(boolean bondsEnabled) {
    this(100, bondsEnabled);
  }

  /**
   * Constructor for a ChemFrame with an unknown number of atoms.
   *
   */
  public ChemFrame() {
    this(true);
  }

  /**
   * Returns the <code>PhysicalProperty</code>'s associated with this frame.
   *
   * @return a Vector of <code>PhysicalProperty</code>
   */
  public Vector getFrameProperties() {
    return properties;
  }

  /**
   * Adds a <code>PhysicalProperty</code> to this frame if not already defined.
   * If a <code>PhysicalProperty</code> with the same description already
   * exists, the property is not added.
   *
   * @param property the <code>PhysicalProperty</code> to be added.
   */
  public void addProperty(PhysicalProperty property) {

    String newDescription = property.getDescriptor();

    boolean found = false;
    Enumeration e = properties.elements();
    while (e.hasMoreElements()) {
      PhysicalProperty fp = (PhysicalProperty) e.nextElement();
      String fpd = fp.getDescriptor();
      if (newDescription.equals(fp.getDescriptor())) {
        found = true;
      }
    }

    if (!found) {
      properties.addElement(property);
    }
  }

  public boolean hasAtomProperty(String description) {
    boolean result = false;
    for (int i=0; i < numberAtoms; ++i) {
      if (atoms[i].hasProperty(description)) {
        result = true;
      }
    }
    return result;
  }

  /**
   * Sets the information label for the frame
   *
   * @param info the information label for this frame
   */
  public void setInfo(String info) {
    this.info = info;
  }

  public void updateMlists(Vector dlist, Vector alist, Vector dhlist) {
    this.dlist = dlist;
    this.alist = alist;
    this.dhlist = dhlist;
  }

  /**
   * Returns this frame's information label
   */
  public String getInfo() {
    return info;
  }

  /**
   * Adds an atom to the frame and finds all bonds between the
   * new atom and pre-existing atoms in the frame
   *
   * @param name the name of the extended atom type for the new atom
   * @param x the x coordinate of the new atom
   * @param y the y coordinate of the new atom
   * @param z the z coordinate of the new atom
   */
  public int addAtom(String name, float x, float y, float z) {
    return addAtom(BaseAtomType.get(name), x, y, z);
  }

  /**
   * Adds an atom to the frame and finds all bonds between the
   * new atom and pre-existing atoms in the frame
   *
   * @param type atom type for the new atom
   * @param x the x coordinate of the new atom
   * @param y the y coordinate of the new atom
   * @param z the z coordinate of the new atom
   */
  public int addAtom(BaseAtomType type, float x, float y, float z) {

    int i = numberAtoms;
    if (i >= atoms.length) {
      increaseArraySizes(2 * atoms.length);
    }

    atoms[i] = new Atom(type, numberAtoms);
    atoms[i].setPosition(new Point3f(x, y, z));

    if (AutoBond) {
      for (int j = 0; j < i; j++) {
        if (Atom.closeEnoughToBond(atoms[i], atoms[j], bondFudge)) {
          atoms[i].addBondedAtom(atoms[j]);
          atoms[j].addBondedAtom(atoms[i]);
        }
      }
    }
    return numberAtoms++;
  }

  /**
   * Adds an atom to the frame and finds all bonds between the
   * new atom and pre-existing atoms in the frame
   *
   * @param atomicNumber the atomicNumber of the extended atom type for the new atom
   * @param x the x coordinate of the new atom
   * @param y the y coordinate of the new atom
   * @param z the z coordinate of the new atom
   */
  public int addAtom(int atomicNumber, float x, float y, float z) {

    BaseAtomType baseType = BaseAtomType.get(atomicNumber);
    if (baseType == null) {
      return -1;
    }
    return addAtom(baseType, x, y, z);
  }

  /**
   * returns the number of atoms in the ChemFrame
   */
  public int getNumberOfAtoms() {
    return numberAtoms;
  }

  /**
   * Returns the atoms in this frame.
   */
  public Atom[] getAtoms() {
    return atoms;
  }

  /**
   *  Returns the number of atoms in this frame.
   *
   *  @return the number of atoms in this frame.
   */
  public int getAtomCount() {
    return numberAtoms;
  }

  public float getXMin() {
    return min.x;
  }

  public float getXMax() {
    return max.x;
  }

  public float getYMin() {
    return min.y;
  }

  public float getYMax() {
    return max.y;
  }

  public float getZMin() {
    return min.z;
  }

  public float getZMax() {
    return max.z;
  }

  /**
   *  Returns the atom at the given index.
   *
   *  @param index the index of the atom.
   *  @return the atom at the given index.
   */
  public Atom getAtomAt(int index) {
    return atoms[index];
  }

  /**
   * returns the coordinates of the i'th atom
   *
   * @param i the index of the atom
   */
  public double[] getAtomCoords(int i) {

    Point3f position = atoms[i].getPosition();
    double[] coords = {
      position.x, position.y, position.z
    };
    return coords;
  }

  /**
   * Transform all the points in this model
   */
  public void transform() {

    if (numberAtoms <= 0) {
      return;
    }
    for (int i = 0; i < numberAtoms; ++i) {
      Point3d pt = new Point3d(atoms[i].getPosition());
      mat.transform(pt);
      atoms[i].transform(mat);
    }
  }

  /**
   * return a Vector with selected atoms
   */
  public Vector getSelectedAtoms() {

    Vector result = new Vector();
    for (int i = 0; i < numberAtoms; i++) {
      if (pickedAtoms[i]) {
        result.addElement(new Integer(i + 1));
      }
      ;
    }
    return result;
  }

  /**
   * Add atom by its number
   */
  public void selectAtomByNumber(int atom) {

    if ((numberAtoms <= 0) || (atom > numberAtoms)) {
      return;
    }
    pickedAtoms[atom - 1] = true;
    napicked++;
  }

  /**
   * Add all atoms in this frame to the list of picked atoms
   */
  public void selectAll() {

    if (numberAtoms <= 0) {
      return;
    }
    napicked = 0;
    for (int i = 0; i < numberAtoms; i++) {
      pickedAtoms[i] = true;
      napicked++;
    }
  }

  /**
   * Remove all atoms in this frame from the list of picked atoms
   */
  public void deselectAll() {

    if (numberAtoms <= 0) {
      return;
    }
    for (int i = 0; i < numberAtoms; i++) {
      pickedAtoms[i] = false;
    }
    napicked = 0;
  }

  public int pickMeasuredAtom(int x, int y) {
    return getNearestAtom(x, y);
  }

  /**
   * Clear out the list of picked atoms, find the nearest atom to a
   * set of screen coordinates and add this new atom to the picked
   * list.
   *
   * @param x the screen x coordinate of the selection point
   * @param y the screen y coordinate of the selection point
   */
  public void selectAtom(int x, int y) {

    int smallest = getNearestAtom(x, y);
    if (smallest < 0) {
      return;
    }
    if (pickedAtoms[smallest]) {
      pickedAtoms[smallest] = false;
      napicked = 0;
    } else {
      pickedAtoms[smallest] = true;
      napicked = 1;
    }
    for (int i = 0; i < numberAtoms; i++) {
      if (i != smallest) {
        pickedAtoms[i] = false;
      }
    }
  }

  /**
   * Find the nearest atom to a set of screen coordinates and add
   * this new atom to the picked list.
   *
   * @param x the screen x coordinate of the selection point
   * @param y the screen y coordinate of the selection point
   */
  public void shiftSelectAtom(int x, int y) {

    int smallest = getNearestAtom(x, y);
    if (pickedAtoms[smallest]) {
      pickedAtoms[smallest] = false;
      napicked--;
    } else {
      pickedAtoms[smallest] = true;
      napicked++;
    }
  }

  /**
   * Clear out the list of picked atoms, find all atoms within
   * designated region and add these atoms to the picked list.
   *
   * @param x1 the x coordinate of point 1 of the region's bounding rectangle
   * @param y1 the y coordinate of point 1 of the region's bounding rectangle
   * @param x2 the x coordinate of point 2 of the region's bounding rectangle
   * @param y2 the y coordinate of point 2 of the region's bounding rectangle
   */
  public void selectRegion(int x1, int y1, int x2, int y2) {

    if (numberAtoms <= 0) {
      return;
    }
    transform();
    napicked = 0;
    for (int i = 0; i < numberAtoms; i++) {
      if (isAtomInRegion(i, x1, y1, x2, y2)) {
        pickedAtoms[i] = true;
        napicked++;
      } else {
        pickedAtoms[i] = false;
      }
    }
  }

  /**
   * Find all atoms within designated region and add these atoms to
   * the picked list.
   *
   * @param x1 the x coordinate of point 1 of the region's bounding rectangle
   * @param y1 the y coordinate of point 1 of the region's bounding rectangle
   * @param x2 the x coordinate of point 2 of the region's bounding rectangle
   * @param y2 the y coordinate of point 2 of the region's bounding rectangle
   */
  public void shiftSelectRegion(int x1, int y1, int x2, int y2) {

    if (numberAtoms <= 0) {
      return;
    }
    transform();
    for (int i = 0; i < numberAtoms; i++) {
      if (isAtomInRegion(i, x1, y1, x2, y2)) {
        if (!pickedAtoms[i]) {
          pickedAtoms[i] = true;
          napicked++;
        }
      }
    }
  }

  private boolean isAtomInRegion(int n, int x1, int y1, int x2, int y2) {

    int x = (int) atoms[n].getScreenPosition().x;
    int y = (int) atoms[n].getScreenPosition().y;
    if ((x > x1) && (x < x2)) {
      if ((y > y1) && (y < y2)) {
        return true;
      }
    }
    return false;
  }

  private int getNearestAtom(int x, int y) {

    if (numberAtoms <= 0) {
      return -1;
    }
    transform();
    int dx, dy, dr2;
    int smallest = -1;
    int smallr2 = Integer.MAX_VALUE;
    for (int i = 0; i < numberAtoms; i++) {
      dx = (int) atoms[i].getScreenPosition().x - x;
      dy = (int) atoms[i].getScreenPosition().y - y;
      dr2 = dx * dx + dy * dy;
      if (dr2 < smallr2) {
        smallest = i;
        smallr2 = dr2;
      }
    }
    if (smallest >= 0) {
      return smallest;
    }
    return -1;
  }

  /**
   * Find the bounds of this model.
   */
  public void findBounds() {
    findBB();
  }
  
  public Point3f getMinimumBounds() {
    return min;
  }

  public Point3f getMaximumBounds() {
    return max;
  }

  /**
   * Find the bounds of this model.
   */
  public void findBB() {

    if ((atoms == null) || (numberAtoms <= 0)) {
      return;
    }
    
    min = new Point3f(atoms[0].getPosition());
    max = new Point3f(min);
    for (int i = 1; i < numberAtoms; ++i) {
      float x = atoms[i].getPosition().x;
      if (x < min.x) {
        min.x = x;
      }
      if (x > max.x) {
        max.x = x;
      }
      float y = atoms[i].getPosition().y;
      if (y < min.y) {
        min.y = y;
      }
      if (y > max.y) {
        max.y = y;
      }
      float z = atoms[i].getPosition().z;
      if (z < min.z) {
        min.z = z;
      }
      if (z > max.z) {
        max.z = z;
      }
    }
  }

  /**
   * Walk through this frame and find all bonds again.
   */
  public void rebond() throws Exception {

    // Clear the currently existing bonds.
    for (int i = 0; i < numberAtoms; i++) {
      atoms[i].clearBondedAtoms();
    }

    // Do a n*(n-1) scan to get new bonds.
    if (AutoBond) {
      for (int i = 0; i < numberAtoms - 1; i++) {
        for (int j = i; j < numberAtoms; j++) {
          if (Atom.closeEnoughToBond(atoms[i], atoms[j], bondFudge)) {
            atoms[i].addBondedAtom(atoms[j]);
            atoms[j].addBondedAtom(atoms[i]);
          }
        }
      }
    }
  }

  private Vector vibrations = new Vector();
  public void addVibration(Vibration v) {
    vibrations.addElement(v);
  }

  public Vibration getVibration(int index) {
    return (Vibration) vibrations.elementAt(index);
  }

  public int getNumberVibrations() {
    return vibrations.size();
  }

  public Enumeration getVibrations() {
    return vibrations.elements();
  }

  private void increaseArraySizes(int newArraySize) {

    Atom[] nat = new Atom[newArraySize];
    System.arraycopy(atoms, 0, nat, 0, atoms.length);
    atoms = nat;

    boolean[] newPickedAtoms = new boolean[newArraySize];
    System.arraycopy(pickedAtoms, 0, newPickedAtoms, 0, pickedAtoms.length);
    pickedAtoms = newPickedAtoms;
  }

  private boolean bondsEnabled;

  public void setMat(Matrix4d newmat) {
    mat = newmat;
  }

  public void setPickedAtoms(boolean[] newPickedAtoms) {
    int copyLength = newPickedAtoms.length;
    if (copyLength > pickedAtoms.length) {
      copyLength = pickedAtoms.length;
    }
    System.arraycopy(newPickedAtoms, 0, pickedAtoms, 0, copyLength);
  }

  /**
   * Returns whether each atom in this frame is picked.
   */
  public boolean[] getPickedAtoms() {
    return pickedAtoms;
  }
}

