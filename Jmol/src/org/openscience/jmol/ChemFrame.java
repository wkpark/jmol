
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

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import java.util.Enumeration;
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

  /**
   * The set of selected atoms.
   */
  private AtomSet pickedAtoms = new AtomSet();

  // This stuff can vary for each frame in the dynamics:

  private String info;     // The title or info string for this frame.
  private Atom[] atoms;    // array of atom types
  private Vector properties = new Vector();
  private int numberAtoms = 0;

  /**
   * Returns whether the atom at the given index is picked.
   */
  boolean isAtomPicked(int index) {

    if (index >= atoms.length) {
      throw new IllegalArgumentException(
          "isAtomPicked(): atom index to large");
    }
    return pickedAtoms.contains(atoms[index]);
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
    for (int i = 0; i < numberAtoms; ++i) {
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

    clearBounds();
    int i = numberAtoms;
    if (i >= atoms.length) {
      increaseArraySizes(2 * atoms.length);
    }

    atoms[i] = new Atom(type, numberAtoms);
    atoms[i].setPosition(new Point3f(x, y, z));

    if (AutoBond) {
      for (int j = 0; j < i; j++) {
        if (Atom.closeEnoughToBond(atoms[i], atoms[j], bondFudge)) {
          addBond(i, j);
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
   * Deletes an Atom from the frame
   */
  public void deleteAtom(int atomID) {

    clearBounds();
    for (int i = atomID; i < numberAtoms; i++) {
      atoms[i] = atoms[i + 1];
    }
    atoms[numberAtoms - 1] = null;
    numberAtoms--;
    try {
      rebond();
    } catch (Exception e) {

      // could not rebond atoms
    }
  }

  /**
   * returns the number of atoms in the ChemFrame
   *
   *  @return the number of atoms in this frame.
   */
  public int getNumberOfAtoms() {
    return numberAtoms;
  }

  public float getXMin() {
    if (min == null) {
      findBounds();
    }
    return min.x;
  }

  public float getXMax() {
    if (max == null) {
      findBounds();
    }
    return max.x;
  }

  public float getYMin() {
    if (min == null) {
      findBounds();
    }
    return min.y;
  }

  public float getYMax() {
    if (max == null) {
      findBounds();
    }
    return max.y;
  }

  public float getZMin() {
    if (min == null) {
      findBounds();
    }
    return min.z;
  }

  public float getZMax() {
    if (max == null) {
      findBounds();
    }
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
      if (pickedAtoms.contains(atoms[i])) {
        result.addElement(new Integer(i + 1));
      }
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
    addPickedAtom(atoms[atom-1]);
  }

  /**
   * Add all atoms in this frame to the list of picked atoms
   */
  public void selectAll() {

    if (numberAtoms <= 0) {
      return;
    }
    for (int i = 0; i < numberAtoms; i++) {
      addPickedAtom(atoms[i]);
    }
  }

  /**
   * Remove all atoms in this frame from the list of picked atoms
   */
  public void deselectAll() {

    if (numberAtoms <= 0) {
      return;
    }
    clearPickedAtoms();
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
    clearPickedAtoms();
    if (!pickedAtoms.contains(atoms[smallest])) {
      addPickedAtom(atoms[smallest]);
    }
  }

  /**
   * Clear out the list of picked atoms, find the nearest atom to a
   * set of screen coordinates and add this new atom to the picked
   * list.
   *
   * @param x the screen x coordinate of the selection point
   * @param y the screen y coordinate of the selection point
   */
  public void deleteSelectedAtom(int x, int y) {

    int smallest = getNearestAtom(x, y);
    if (smallest < 0) {
      return;
    }
    deleteAtom(smallest);
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
    if (pickedAtoms.contains(atoms[smallest])) {
      removePickedAtom(atoms[smallest]);
    } else {
      addPickedAtom(atoms[smallest]);
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
    clearPickedAtoms();
    for (int i = 0; i < numberAtoms; i++) {
      if (isAtomInRegion(i, x1, y1, x2, y2)) {
        addPickedAtom(atoms[i]);
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
        if (!pickedAtoms.contains(atoms[i])) {
          addPickedAtom(atoms[i]);
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

  public Point3f getMinimumBounds() {
    if (min == null) {
      findBounds();
    }
    return new Point3f(min);
  }

  public Point3f getMaximumBounds() {
    if (max == null) {
      findBounds();
    }
    return new Point3f(max);
  }

  /**
   * Clears the bounds cache for this model.
   */
  private void clearBounds() {
    min = null;
    max = null;
  }

  /**
   * Find the bounds of this model.
   */
  private void findBounds() {

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
    clearBonds();

    // Do a n*(n-1) scan to get new bonds.
    if (AutoBond) {
      for (int i = 0; i < numberAtoms - 1; i++) {
        for (int j = i; j < numberAtoms; j++) {
          if (Atom.closeEnoughToBond(atoms[i], atoms[j], bondFudge)) {
            addBond(i, j);
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

  }

  private boolean bondsEnabled;

  public void setMat(Matrix4d newmat) {
    mat = newmat;
  }

  public void setPickedAtoms(boolean[] newPickedAtoms) {

    clearPickedAtoms();
    for (int i = 0; i < newPickedAtoms.length; ++i) {
      if (newPickedAtoms[i]) {
        addPickedAtom(atoms[i]);
      }
    }
  }

  /**
   * Returns the set of picked atoms.
   *
   * @return the AtomSet of picked atoms.
   */
  public AtomSet getPickedAtomSet() {
    return pickedAtoms;
  }
  
  /**
   * Returns whether each atom in this frame is picked.
   */
  public boolean[] getPickedAtoms() {
    boolean[] pickedAtomsArray = new boolean[atoms.length];
    for (int i = 0; i < atoms.length; ++i) {
      if (pickedAtoms.contains(atoms[i])) {
        pickedAtomsArray[i] = true;
      } else {
        pickedAtomsArray[i] = false;
      }
    }
    return pickedAtomsArray;
  }

  /**
   * Adds a single bond between the two atoms given.
   *
   * @param i index to the first atom in the bond.
   * @param j index to the second atom in the bond.
   */
  public void addBond(int i, int j) {
    addBond(i, j, 1);
  }

  /**
   * Adds a bond between the two atoms given.
   *
   * @param i index to the first atom in the bond.
   * @param j index to the second atom in the bond.
   * @param bondOrder the order (single, double, triple) of the bond.
   */
  public void addBond(int i, int j, int bondOrder) {

    atoms[i].addBondedAtom(atoms[j], bondOrder);
    atoms[j].addBondedAtom(atoms[i], bondOrder);
  }

  /**
   * Clears all bonds from all atoms.
   */
  public void clearBonds() {

    for (int i = 0; i < numberAtoms; i++) {
      atoms[i].clearBondedAtoms();
    }
  }

  private void addPickedAtom(Atom atom) {
    Integer oldNumberOfPicked = new Integer(pickedAtoms.size());
    pickedAtoms.add(atom);
    Integer newNumberOfPicked = new Integer(pickedAtoms.size());
    changeSupport.firePropertyChange(atomPickedProperty, oldNumberOfPicked, newNumberOfPicked);
  }
  
  private void removePickedAtom(Atom atom) {
    Integer oldNumberOfPicked = new Integer(pickedAtoms.size());
    pickedAtoms.remove(atom);
    Integer newNumberOfPicked = new Integer(pickedAtoms.size());
    changeSupport.firePropertyChange(atomPickedProperty, oldNumberOfPicked, newNumberOfPicked);
  }
  
  private void clearPickedAtoms() {
    Integer oldNumberOfPicked = new Integer(pickedAtoms.size());
    pickedAtoms.clear();
    Integer newNumberOfPicked = new Integer(pickedAtoms.size());
    changeSupport.firePropertyChange(atomPickedProperty, oldNumberOfPicked, newNumberOfPicked);
  }

  public static final String atomPickedProperty = "atomPicked";

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    changeSupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changeSupport.removePropertyChangeListener(listener);
  }

  private PropertyChangeSupport changeSupport =
    new PropertyChangeSupport(this);
}

