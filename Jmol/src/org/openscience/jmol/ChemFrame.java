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
import javax.vecmath.Point3d;
import java.io.PrintStream;

/**
 *  Data representation for a molecule in a particular set of coordinates.
 *
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class ChemFrame {

  // This stuff can vary for each frame in the dynamics:

  private String info;     // The title or info string for this frame.
  private Atom[] atoms;    // array of atom types
  private Vector properties = new Vector();
  private int numberAtoms = 0;

  /**
   * Returns the list of distance measurements.
   */
  public Vector getDistanceMeasurements() {
    return dlist;
  }

  /**
   * List of distance measurements.
   */
  private Vector dlist;

  /**
   * Returns the list of angle measurements.
   */
  public Vector getAngleMeasurements() {
    return alist;
  }

  /**
   * List of angle measurements.
   */
  private Vector alist;

  /**
   * Returns the list of dihedral measurements.
   */
  public Vector getDihedralMeasurements() {
    return dhlist;
  }

  /**
   * List of dihedral measurements.
   */
  private Vector dhlist;

  private Point3d centerGeometric;
  private Point3d centerRotation;
  private double radiusGeometric;
  private double radiusRotation;
  private double minAtomVectorMagnitude;
  private double maxAtomVectorMagnitude;
  private double atomVectorRange;

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
   * Set the <code>PhysicalProperty</code>'s associated with this frame.
   *
   * @param a Vector of <code>PhysicalProperty</code>
   */
  public void setFrameProperties(Vector properties) {
    this.properties = properties;
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
  public int addAtom(String name, double x, double y, double z) {
    return addAtom(name, name, x, y, z);
  }

  public int addAtom(String name, String root, double x, double y, double z) {
    return addAtom(BaseAtomType.get(name, root), x, y, z);
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
  public int addAtom(BaseAtomType type, double x, double y, double z) {

    clearBounds();
    int i = numberAtoms;
    if (i >= atoms.length) {
      increaseArraySizes(2 * atoms.length);
    }

    atoms[i] = new Atom(type, numberAtoms, x, y, z);

    if (DisplayControl.control.getAutoBond()) {
      for (int j = 0; j < i; j++) {
        if (Atom.closeEnoughToBond(atoms[i], atoms[j],
                                   DisplayControl.control.getBondFudge())) {
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
  public int addAtom(int atomicNumber, double x, double y, double z) {

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

  public double getGeometricRadius() {
    findBounds();
    return radiusGeometric;
  }

  public Point3d getGeometricCenter() {
    findBounds();
    return centerGeometric;
  }

  public Point3d getRotationCenter() {
    findBounds();
    return centerRotation;
  }

  public double getRotationRadius() {
    findBounds();
    return radiusRotation;
  }

  public void setRotationCenter(Point3d newCenterOfRotation) {
    if (newCenterOfRotation != null) {
      centerRotation = newCenterOfRotation;
      radiusRotation = calculateRadius(centerRotation);
    } else {
      centerRotation = centerGeometric;
      radiusRotation = radiusGeometric;
    }
  }

  public double getMinAtomVectorMagnitude() {
    findBounds();
    return minAtomVectorMagnitude;
  }

  public double getMaxAtomVectorMagnitude() {
    findBounds();
    return maxAtomVectorMagnitude;
  }

  public double getAtomVectorRange() {
    findBounds();
    return maxAtomVectorMagnitude - minAtomVectorMagnitude;
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

    Point3d position = atoms[i].getPosition();
    double[] coords = {
      position.x, position.y, position.z
    };
    return coords;
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
  public Atom[] findAtomsInRegion(int x1, int y1, int x2, int y2) {

    if (numberAtoms <= 0) {
      return new Atom[0];
    }
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
	int x = atoms[n].getScreenX();
	if ((x > x1) && (x < x2)) {
	    int y = atoms[n].getScreenY();
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
   * note that this will return null if the atom is more than 5 pixels
   * outside the radius of the atom
   * mth - this code has problems
   * 1. doesn't take radius of atom into account until too late
   * 2. doesn't take Z dimension into account, so it could select an atom
   *    which is behind the one the user wanted
   * 3. doesn't take into account the fact that hydrogens could be hidden
   *    you can select a region and get extra hydrogens
   */
  public Atom getNearestAtom(int x, int y) {
    if (numberAtoms <= 0) {
      return null;
    }
    int dx, dy, dr2;
    Atom atomClosest = null;
    int r2Closest = Integer.MAX_VALUE;
    for (int i = 0; i < numberAtoms; i++) {
      Atom atom = atoms[i];
      dx = atom.getScreenX() - x;
      dy = atom.getScreenY() - y;
      dr2 = dx * dx + dy * dy;
      if (dr2 < r2Closest) {
        atomClosest = atom;
        r2Closest = dr2;
      }
    }
    int rClosest = (int)Math.sqrt(r2Closest);
    return (rClosest > atomClosest.getScreenRadius() + 5)
      ? null
      : atomClosest;
  }

  /**
   * Clears the bounds cache for this model.
   */
  private void clearBounds() {
    centerGeometric = centerRotation = null;
    radiusGeometric = radiusRotation =
      minAtomVectorMagnitude = maxAtomVectorMagnitude = atomVectorRange = 0f;
  }

  /**
   * Find the bounds of this model.
   */
  private void findBounds() {
    if ((centerGeometric != null) || (atoms == null) || (numberAtoms <= 0))
      return;
    centerGeometric = centerRotation = calculateGeometricCenter();
    calculateAtomVectorMagnitudeRange();
    radiusGeometric = radiusRotation = calculateRadius(centerGeometric);
  }

  void calculateAtomVectorMagnitudeRange() {
    minAtomVectorMagnitude = maxAtomVectorMagnitude = -1;
    for (int i = 0; i < numberAtoms; ++i) {
      if (!atoms[i].hasVector())
        continue;
      double magnitude=atoms[i].getVectorMagnitude();
      if (magnitude > maxAtomVectorMagnitude) {
        maxAtomVectorMagnitude = magnitude;
      }
      if ((magnitude < minAtomVectorMagnitude) ||
          (minAtomVectorMagnitude == -1)) {
        minAtomVectorMagnitude = magnitude;
      }
    }
    atomVectorRange = maxAtomVectorMagnitude - minAtomVectorMagnitude;
  }

  Point3d calculateGeometricCenter() {
    /**
     * Note that this method is overridden by CrystalFrame
     */
    // First, find the center of the molecule. Current definition is the center
    // of the cartesian coordinates as stored in the file. Note that this is
    // not really the center because an atom could be stuck way up in one of
    // the corners of the box
    Point3d position = atoms[0].getPosition();
    double minX = position.x, maxX = minX;
    double minY = position.y, maxY = minY;
    double minZ = position.z, maxZ = minZ;

    for (int i = 1; i < numberAtoms; ++i) {
      position = atoms[i].getPosition();
      double x = position.x;
      if (x < minX) { minX = x; }
      if (x > maxX) { maxX = x; }
      double y = position.y;
      if (y < minY) { minY = y; }
      if (y > maxY) { maxY = y; }
      double z = position.z;
      if (z < minZ) { minZ = z; }
      if (z > maxZ) { maxZ = z; }
    }
    return new Point3d((minX + maxX) / 2,
                       (minY + maxY) / 2,
                       (minZ + maxZ) / 2);
  }

  double calculateRadius(Point3d center) {
    /**
     * Note that this method is overridden by CrystalFrame
     */
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
    double radius = 0.0f;
    double atomSphereFactor = DisplayControl.control.percentVdwAtom / 100.0;
    for (int i = 0; i < numberAtoms; ++i) {
      Atom atom = atoms[i];
      Point3d posAtom = atom.getPosition();
      double distAtom = center.distance(posAtom);
      double distVdw =
        distAtom + (atom.getType().getVdwRadius() * atomSphereFactor);
      if (distVdw > radius)
        radius = distVdw;
      if (atom.hasVector()) {
        // mth 2002 nov
        // this calculation isn't right, but I can't get it to work with
        // samples/cs2.syz when I try to use
        // double distVector = center.distance(atom.getScaledVector());
        // So I am over-estimating and giving up for the day. 
        double distVector = distAtom + atom.getVectorMagnitude();
        if (distVector > radius)
          radius = distVector;
      }
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
    if (DisplayControl.control.getAutoBond()) {
      for (int i = 0; i < numberAtoms - 1; i++) {
        for (int j = i; j < numberAtoms; j++) {
          if (Atom.closeEnoughToBond(atoms[i], atoms[j],
                                     DisplayControl.control.getBondFudge())) {
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

  public void dumpAtoms(PrintStream out) {
    for (int i = 0; i < numberAtoms; ++i) {
      out.println(atoms[i].toString());
    }
  }

}

