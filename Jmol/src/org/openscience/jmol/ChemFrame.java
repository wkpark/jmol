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
public class ChemFrame implements Transformable {

  private static float bondFudge = 1.12f;
  private static boolean AutoBond = true;

  // This stuff can vary for each frame in the dynamics:

  private String info;     // The title or info string for this frame.
  private Atom[] atoms;    // array of atom types
  private Vector properties = new Vector();
  private int numberAtoms = 0;

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

  private Point3f centerPoint;
  private float radius;

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

    atoms[i] = new Atom(type, numberAtoms, x, y, z);

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
  public void deleteAtom(Atom a) {
    clearBounds();
    // deteremine atomID
    int atomID = 0;
    for (int i = 0; i < numberAtoms; i++) {
      if (atoms[i].hashCode() == a.hashCode()) {
        atomID = i;
        i = numberAtoms;
      }
    }
    for (int i = atomID; i < numberAtoms -1; i++) {
      atoms[i] = atoms[i + 1];
    }
    atoms[numberAtoms - 1] = null;
    numberAtoms--;
    rebond();
  }

  /**
   * returns the number of atoms in the ChemFrame
   *
   *  @return the number of atoms in this frame.
   */
  public int getNumberOfAtoms() {
    return numberAtoms;
  }

  public float getRadius() {
    findBounds();
    return radius;
  }

  public Point3f getCenter() {
    findBounds();
    return centerPoint;
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

  public Atom[] getAtoms() {
    Atom[] result = new Atom[numberAtoms];
    System.arraycopy(atoms, 0, result, 0, result.length);
    return result;
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
  public void transform(Matrix4d matrix) {

    if (numberAtoms <= 0) {
      return;
    }
    for (int i = 0; i < numberAtoms; ++i) {
      Point3d pt = new Point3d(atoms[i].getPosition());
      matrix.transform(pt);
      atoms[i].transform(matrix);
    }
  }

  /**
   * Find all atoms within designated region.
   *
   * @param x1 the x coordinate of point 1 of the region's bounding rectangle
   * @param y1 the y coordinate of point 1 of the region's bounding rectangle
   * @param x2 the x coordinate of point 2 of the region's bounding rectangle
   * @param y2 the y coordinate of point 2 of the region's bounding rectangle
   * @return the atoms in the region
   */
  public Atom[] findAtomsInRegion(int x1, int y1, int x2, int y2, Matrix4d matrix) {

    if (numberAtoms <= 0) {
      return new Atom[0];
    }
    transform(matrix);
    Vector atomsInRegion = new Vector();
    for (int i = 0; i < numberAtoms; i++) {
      if (isAtomInRegion(i, x1, y1, x2, y2)) {
        atomsInRegion.addElement(atoms[i]);
      }
    }
    
    Atom[] result = new Atom[atomsInRegion.size()];
    for (int i = 0; i < result.length; ++i) {
      result[i] = (Atom) atomsInRegion.elementAt(i);
    }
    return result;
  }

  private boolean isAtomInRegion(int n, int x1, int y1, int x2, int y2) {

    int x = atoms[n].screenX;
    if ((x > x1) && (x < x2)) {
      int y = atoms[n].screenY;
      if ((y > y1) && (y < y2)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Finds the atom nearest the given screen coordinates.
   *
   * @param x the x screen coordinate
   * @param y the y screen coordinate
   * @return the atom drawn closest to the coordinates.
   */
  public Atom getNearestAtom(int x, int y, Matrix4d matrix) {

    if (numberAtoms <= 0) {
      return null;
    }
    transform(matrix);
    int dx, dy, dr2;
    Atom smallest = null;
    int smallr2 = Integer.MAX_VALUE;
    for (int i = 0; i < numberAtoms; i++) {
      Atom atom = atoms[i];
      dx = atom.screenX - x;
      dy = atom.screenY - y;
      dr2 = dx * dx + dy * dy;
      if (dr2 < smallr2) {
        smallest = atom;
        smallr2 = dr2;
      }
    }
    return smallest;
  }

  /**
   * Clears the bounds cache for this model.
   */
  private void clearBounds() {
    centerPoint = null;
    radius = 0.0f;
  }

  /**
   * Find the bounds of this model.
   */
  private void findBounds() {
    if ((centerPoint != null) || (atoms == null) || (numberAtoms <= 0))
      return;
    centerPoint = calculateCenterPoint();
    radius = calculateRadius(centerPoint);
  }

  /**
   * Note that this method is overridden by CrystalFrame
   */
  Point3f calculateCenterPoint() {
    // First, find the center of the molecule. Current definition is the center
    // of the cartesian coordinates as stored in the file. Note that this is
    // not really the center because an atom could be stuck way up in one of
    // the corners of the box
    Point3f position = atoms[0].getPosition();
    float minX = position.x, maxX = minX;
    float minY = position.y, maxY = minY;
    float minZ = position.z, maxZ = minZ;

    for (int i = 1; i < numberAtoms; ++i) {
      position = atoms[i].getPosition();
      float x = position.x;
      if (x < minX) { minX = x; }
      if (x > maxX) { maxX = x; }
      float y = position.y;
      if (y < minY) { minY = y; }
      if (y > maxY) { maxY = y; }
      float z = position.z;
      if (z < minZ) { minZ = z; }
      if (z > maxZ) { maxZ = z; }
    }
    return new Point3f((minX + maxX) / 2,
                       (minY + maxY) / 2,
                       (minZ + maxZ) / 2);
  }

  float calculateRadius(Point3f center) {
    // Now that we have defined the center, find the radius to the outermost
    // atom, including the radius of the atom itself. Note that this is
    // currently the vdw radius as scaled by the vdw display radius as set
    // in preferences. This is *not* recalculated if the user changes the
    // display scale ... perhaps it should be.
    // Atom Vectors should be included in this calculation so they don't get
    // clipped off the screen during rotations ... but I don't understand
    // them yet ... so they are not included. samples/cs2.xyz has atom vectors
    //
    // examples of crystal vectors samples/estron.cml samples/bulk_Si.in
    float radius = 0.0f;
    float atomSphereFactor = (float) Jmol.settings.getAtomSphereFactor();
    for (int i = 0; i < numberAtoms; ++i) {
      Atom atom = atoms[i];
      Point3f posAtom = atom.getPosition();
      float distAtom = center.distance(posAtom);
      distAtom += (atom.getType().getVdwRadius() * atomSphereFactor);
      if (distAtom > radius)
        radius = distAtom;
    }
    return radius;
  }

  /**
   * Walk through this frame and find all bonds again.
   */
  public void rebond() {

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
  public boolean getBondsEnabled() {
    return bondsEnabled;
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

}

