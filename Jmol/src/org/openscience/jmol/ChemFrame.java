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
import org.openscience.jmol.Atom;
import org.openscience.jmol.render.AtomShape;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.geometry.BondTools;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import java.util.Enumeration;
import java.util.BitSet;
import java.awt.Rectangle;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import java.io.PrintStream;

/**
 *  Data representation for a molecule in a particular set of coordinates.
 *
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class ChemFrame extends AtomContainer {

  // This stuff can vary for each frame in the dynamics:

  DisplayControl control;

  private String info;     // The title or info string for this frame.
  private Vector properties = new Vector();

  Point3d centerBoundingBox;
  Point3d cornerBoundingBox;
  private Point3d centerRotation;
  private double radiusBoundingBox;
  private double radiusRotation;
  private double minAtomVectorMagnitude;
  private double maxAtomVectorMagnitude;
  private double atomVectorRange;

  /**
   * Constructor for a ChemFrame with a known number of atoms.
   *
   * @param na the number of atoms in the frame
   */
  public ChemFrame(DisplayControl control, int na) {
      this(control, na, true);
  }

  public ChemFrame(DisplayControl control, int na, boolean bondsEnabled) {
      super(na, na);
      this.bondsEnabled = bondsEnabled;
      this.control = control;
  }

  public ChemFrame(DisplayControl control, boolean bondsEnabled) {
      this(control, 100, bondsEnabled);
  }

  /**
   * Constructor for a ChemFrame with an unknown number of atoms.
   */
  public ChemFrame(DisplayControl control) {
      this(control, true);
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
    Atom[] atoms = getJmolAtoms();
    for (int i = 0; i < atoms.length; ++i) {
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

  /**
   * Returns this frame's information label
   */
  public String getInfo() {
    return info;
  }

    /**
     * Method that makes sure that *all* atoms get added as Jmol atom.
     */
    public void addAtom(org.openscience.cdk.Atom atom) {
        Atom jmolAtom = null;
        if (!(atom instanceof org.openscience.jmol.Atom)) {
            jmolAtom = new org.openscience.jmol.Atom(control, atom);
        } else {
            jmolAtom = (Atom)atom;
        }
        AtomTypeList.getInstance().configure(jmolAtom);
        jmolAtom.setAtomNumber(this.getAtomCount());
        super.addAtom(jmolAtom);
    }
    
  
  public int addAtom(Atom type, double x, double y, double z) {
      return addAtom(type, x, y, z, null);
  }

  public int addAtom(Atom type, double x, double y, double z,
                     ProteinProp pprop) {
      clearBounds();
      int i = getAtomCount();
      
      Atom atom = new Atom(control, type, i, x, y, z, pprop);
      this.addAtom(atom);
      /*
        mth 2003 05 23
      if (control.getAutoBond()) {
          for (int j = 0; j < i; j++) {
              if (BondTools.closeEnoughToBond(atom, getAtomAt(j),
                                              control.getBondFudge())) {
                  addBond(i, j);
              }
          }
      }
      */
      return getAtomCount() -1;
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

      org.openscience.cdk.Atom atom = new org.openscience.cdk.Atom("");
      atom.setAtomicNumber(atomicNumber);
      atom.setX3D(x);
      atom.setY3D(y);
      atom.setZ3D(z);
      this.addAtom(atom);
      return getAtomCount()-1;
  }

  /**
   * Deletes an Atom from the frame
   */
  public void deleteAtom(int atomIndex) {
      Atom atomDeleted = (org.openscience.jmol.Atom)getAtomAt(atomIndex);
      removeAtom(atomDeleted);
      clearBounds();
      atomDeleted.delete();
  }

  public double getGeometricRadius() {
    findBounds();
    return radiusBoundingBox;
  }

  public Point3d getBoundingBoxCenter() {
    findBounds();
    return centerBoundingBox;
  }

  public Point3d getBoundingBoxCorner() {
    findBounds();
    return cornerBoundingBox;
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
      radiusRotation = calcRadius(centerRotation);
    } else {
      centerRotation = centerBoundingBox;
      radiusRotation = radiusBoundingBox;
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
   * Dummy method now, because this.addAtom(cdk.Atom) ensures that
   * the stored Atom's are jmol.Atom's.
   */
  public Atom getJmolAtomAt(int index) {
      // return new Atom(getAtomAt(index));
      return (Atom)getAtomAt(index);
  }

  /**
   * Dummy method now, because this.addAtom(cdk.Atom) ensures that
   * the stored Atom's are jmol.Atom's.
   */
  public Atom[] getJmolAtoms() {
      Atom[] atoms = new Atom[getAtomCount()];
      for (int i=0; i<getAtomCount(); i++) {
          atoms[i] = getJmolAtomAt(i);
      }
      return atoms;
  }

  /**
   * returns the coordinates of the i'th atom
   *
   * @param i the index of the atom
   */
  public double[] getAtomCoords(int i) {

    Point3d position = atoms[i].getPoint3D();
    double[] coords = {
      position.x, position.y, position.z
    };
    return coords;
  }

  final static int selectionPixelLeeway = 5;

  public int findNearestAtomIndex(int x, int y) {
    /*
     * FIXME
     * mth - this code has problems
     * 1. doesn't take radius of atom into account until too late
     * 2. doesn't take Z dimension into account, so it could select an atom
     *    which is behind the one the user wanted
     * 3. doesn't take into account the fact that hydrogens could be hidden
     *    you can select a region and get extra hydrogens
     */
    if (getAtomCount() <= 0)
      return -1;
    Atom atomNearest = null;
    int indexNearest = -1;
    int r2Nearest = Integer.MAX_VALUE;
    Atom[] atoms = getJmolAtoms();
    for (int i = 0; i < atoms.length; ++i) {
      Atom atom = atoms[i];
      int dx = atom.getScreenX() - x;
      int dx2 = dx * dx;
      if (dx2 > r2Nearest)
        continue;
      int dy = atom.getScreenY() - y;
      int dy2 = dy * dy;
      if (dy2 + dx2 > r2Nearest)
        continue;
      atomNearest = atom; // this will definitely happen the first time through
      r2Nearest = dx2 + dy2;
      indexNearest = i;
    }
    int rNearest = (int)Math.sqrt(r2Nearest);
    return (rNearest > atomNearest.getScreenRadius() + selectionPixelLeeway)
      ? -1
      : indexNearest;
  }
    
  // jvm < 1.4 does not have a BitSet.clear();
  // so in order to clear you "and" with an empty bitset.
  private final BitSet bsEmpty = new BitSet();
  private final BitSet bsFoundRectangle = new BitSet();
  public BitSet findAtomsInRectangle(Rectangle rect) {
    bsFoundRectangle.and(bsEmpty);
    Atom[] atoms = getJmolAtoms();
    for (int i = 0; i < atoms.length; ++i) {
      AtomShape atomShape = atoms[i].getAtomShape();
      if (rect.contains(atomShape.x, atomShape.y))
        bsFoundRectangle.set(i);
    }
    return bsFoundRectangle;
  }

  /**
   * Clears the bounds cache for this model.
   */
  private void clearBounds() {
    centerBoundingBox = centerRotation = null;
    radiusBoundingBox = radiusRotation =
      minAtomVectorMagnitude = maxAtomVectorMagnitude = atomVectorRange = 0f;
  }

  /**
   * Find the bounds of this model.
   */
  private void findBounds() {
    if ((centerBoundingBox != null) || (atoms == null) || (getAtomCount() <= 0))
      return;
    calcBoundingBox();
    centerRotation = centerBoundingBox;
    calculateAtomVectorMagnitudeRange();
    radiusBoundingBox = radiusRotation = calcRadius(centerBoundingBox);
  }

  void calculateAtomVectorMagnitudeRange() {
    minAtomVectorMagnitude = maxAtomVectorMagnitude = -1;
    for (int i = 0; i < getAtomCount(); ++i) {
        Atom atom = getJmolAtomAt(i);
      if (!atom.hasVector()) continue;
      double magnitude=atom.getVectorMagnitude();
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

  void calcBoundingBox() {
    /**
     * Note that this method is overridden by CrystalFrame
     */
    // bounding box is defined as the center of the cartesian coordinates
    // as stored in the file
    // Note that this is not really the geometric center of the molecule
    // ... for this we would need to do a Minimal Enclosing Sphere calculation
    Point3d position = atoms[0].getPoint3D();
    double minX = position.x, maxX = minX;
    double minY = position.y, maxY = minY;
    double minZ = position.z, maxZ = minZ;

    for (int i = 1; i < getAtomCount(); ++i) {
      position = atoms[i].getPoint3D();
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
    centerBoundingBox = new Point3d((minX + maxX) / 2,
                                    (minY + maxY) / 2,
                                    (minZ + maxZ) / 2);
    cornerBoundingBox = new Point3d(maxX, maxY, maxZ);
    cornerBoundingBox.sub(centerBoundingBox);
  }

  double calcRadius(Point3d center) {
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
    double atomSphereFactor = control.getPercentVdwAtom() / 100.0;
    Atom[] atoms = getJmolAtoms();
    for (int i = 0; i < atoms.length; ++i) {
      Atom atom = atoms[i];
      Point3d posAtom = atom.getPoint3D();
      double distAtom = center.distance(posAtom);
      double radiusVdw = atom.getVanderwaalsRadius();
      double distVdw = distAtom + (radiusVdw * atomSphereFactor);
      
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
    if (true) {
      bsptBond();
      return;
    }
    clearBonds();
    long timeBegin = System.currentTimeMillis();
    System.out.println("Rebonding atoms");
    
    int limit = 10000; // 1000;
    if (getAtomCount() < limit) {
      
      // Clear the currently existing bonds.

      // Do a n*(n-1) scan to get new bonds.
      Atom[] atoms = getJmolAtoms();
      for (int i = 0; i < atoms.length - 1; ++i) {
        for (int j = i; j < atoms.length; j++) {
          if (BondTools.closeEnoughToBond(atoms[i], atoms[j],
                                          control.getBondFudge())) {
            addBond(i, j);
          }
        }
      }
    } else {
      System.err.println("Skipped rebonding, because more than " + limit + " found");
    }
    long timeEnd = System.currentTimeMillis();
    System.out.println("Time to rebond=" + (timeEnd - timeBegin));
  }

  private final static boolean showRebondTimes = false;

  // Binary Space Partition Tree Bond
  // mth 2003 05 23
  public void bsptBond() {
    clearBonds();
    double maxCovalentRadius = 0.0;
    long timeBegin, timeEnd;
    if (showRebondTimes) {
      timeBegin = System.currentTimeMillis();
      System.out.println("BSP Tree autobonding");
    }
    Bspt bspt = new Bspt(3);
    Atom[] atoms = getJmolAtoms();
    for (int i = atoms.length; --i >= 0; ) {
      Atom atom = atoms[i];
      double myCovalentRadius = atom.getCovalentRadius();
      if (myCovalentRadius > maxCovalentRadius)
        maxCovalentRadius = myCovalentRadius;
      bspt.addTuple(atom);
    }
    this.maxCovalentRadius = maxCovalentRadius;
    this.bondTolerance = control.getBondTolerance();
    this.minBondDistance = control.getMinBondDistance();
    this.minBondDistance2 = this.minBondDistance*this.minBondDistance;
    this.bspt = bspt;
    for (int i = atoms.length; --i >= 0; )
      bondAtom(atoms[i]);
    this.bspt = null;
    if (showRebondTimes) {
      timeEnd = System.currentTimeMillis();
      System.out.println("maxCovalentRadius=" + maxCovalentRadius);
      System.out.println("Time to autoBond=" + (timeEnd - timeBegin));
    }
  }

  private Bspt bspt;
  private double maxCovalentRadius;
  private double bondTolerance;
  private double minBondDistance;
  private double minBondDistance2;

  private void bondAtom(Atom atom) {
    double myCovalentRadius = atom.getCovalentRadius();
    double searchRadius = myCovalentRadius + maxCovalentRadius + bondTolerance;
    for (Bspt.EnumerateSphere e = bspt.enumHemiSphere(atom, searchRadius);
         e.hasMoreElements(); ) {
      Atom atomNear = (Atom)e.nextElement();
      if (atomNear != atom && !atom.isBondedAtom(atomNear)) {
        int order = getBondOrder(atom, myCovalentRadius,
                                 atomNear, atomNear.getCovalentRadius(),
                                 e.foundDistance2());
        if (order > 0)
          atom.bondMutually(atomNear, order);
      }
    }
  }

  private int getBondOrder(Atom atomA, double covalentRadiusA,
                           Atom atomB, double covalentRadiusB,
                           double distance2) {
    //        System.out.println(" radiusA=" + covalentRadiusA +
    //                           " radiusB=" + covalentRadiusB +
    //                           " distance2=" + distance2 +
    //                           " tolerance=" + bondTolerance);
    double maxAcceptable = covalentRadiusA + covalentRadiusB + bondTolerance;
    double maxAcceptable2 = maxAcceptable * maxAcceptable;
    if (distance2 < minBondDistance2)
      return 0;
    if (distance2 <= maxAcceptable2)
      return 1;
    return 0;
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

  private void setAtomArraySize(int newArraySize) {
    Atom[] nat = new Atom[newArraySize];
    int countToMove = atoms.length;
    if (newArraySize < countToMove)
      countToMove = newArraySize;
    System.arraycopy(atoms, 0, nat, 0, countToMove);
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

    Atom atomI = (Atom)getAtomAt(i);
    Atom atomJ = (Atom)getAtomAt(j);
    ((Atom)getAtomAt(i)).addBondedAtom((Atom)getAtomAt(j), bondOrder);
    ((Atom)getAtomAt(j)).addBondedAtom((Atom)getAtomAt(i), bondOrder);
    atomI.getAtomShape().bondMutually(atomJ.getAtomShape(), bondOrder, control);
  }

  /**
   * Clears all bonds from all atoms.
   */
  public void clearBonds() {
      Atom[] atoms = getJmolAtoms();
      for (int i = 0; i < atoms.length; i++) {
          atoms[i].clearBondedAtoms();
      }
  }

  public void dumpAtoms(PrintStream out) {
      Atom[] atoms = getJmolAtoms();
      for (int i = 0; i < atoms.length; i++) {
          out.println(atoms[i].toString());
      }
  }

  public JmolAtomIterator getAtomIterator() {
    return new ChemFrameIterator();
  }

  class ChemFrameIterator extends JmolAtomIterator {
    int iAtom = 0;

    public boolean hasNext() {
      return (iAtom < getAtomCount());
    }

    public Atom nextAtom() {
      return (org.openscience.jmol.Atom)getAtomAt(iAtom++);
    }
  }

  public JmolAtomIterator getJmolAtomIterator(BitSet set) {
    return new ChemFrameSetIterator(set);
  }

  class ChemFrameSetIterator extends JmolAtomIterator {
    BitSet set;
    int iatom = 0;

    public ChemFrameSetIterator(BitSet set) {
      this.set = set;
    }

    public boolean hasNext() {
      for ( ; iatom < getAtomCount(); ++iatom)
        if (set.get(iatom))
          return true;
      return false;
    }

    public Atom nextAtom() {
      return (org.openscience.jmol.Atom)getAtomAt(iatom++);
    }
  }

  public JmolAtomIterator getJmolBondIterator(BitSet set, boolean bondmodeOr) {
    return new ChemFrameBondSetIterator(set, bondmodeOr);
  }

  class ChemFrameBondSetIterator extends JmolAtomIterator {
    BitSet set;
    boolean bondmodeOr;
    int iatom;
    int ibond;
    boolean bigHit;
    Atom atom;
    Atom[] bondedAtoms;

    public ChemFrameBondSetIterator(BitSet set, boolean bondmodeOr) {
      this.set = set;
      this.bondmodeOr = bondmodeOr;
      bigHit = true;
      iatom = -1;
    }

    public boolean hasNext() {
      while (true) {
        if (! bigHit && bondedAtoms != null) {
          while (++ibond < bondedAtoms.length) {
            int indexOtherAtom = bondedAtoms[ibond].getAtomNumber();
            if (set.get(indexOtherAtom)) {
              bigHit = false;
              return true;
            }
          }
        }
        boolean isSelected;
        do {
          if (++iatom >= getAtomCount())
            return false;
          atom = (org.openscience.jmol.Atom)getAtomAt(iatom);
          isSelected = set.get(iatom);
          if (isSelected && bondmodeOr)
            return bigHit = true;
        } while (!isSelected && !bondmodeOr);
        bondedAtoms = atom.getBondedAtoms();
        bigHit = false;
        ibond = -1;
      }
    }

    public Atom nextAtom() {
      return atom;
    }

    public boolean allBonds() {
      return bigHit;
    }

    public int indexBond() {
      return ibond;
    }
  }
}

