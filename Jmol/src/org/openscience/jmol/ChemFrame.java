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
import org.openscience.jmol.render.BondShape;
import org.openscience.jmol.render.ArrowLineShape;
import org.openscience.jmol.render.LineShape;
import org.openscience.jmol.render.JmolFrame;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.geometry.BondTools;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import java.util.Enumeration;
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
  JmolFrame jmframe;

  private String info;     // The title or info string for this frame.
  private Vector properties = new Vector();

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
        //        jmolAtom.setAtomNumber(this.getAtomCount());
        super.addAtom(jmolAtom);
    }

  public int addAtom(Atom type, double x, double y, double z) {
      return addAtom(type, x, y, z, null);
  }

  public int addAtom(Atom type, double x, double y, double z,
                     ProteinProp pprop) {
    //      clearBounds();
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
      //      clearBounds();
      atomDeleted.delete();
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
  private Atom[] getJmolAtoms() {
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

  /**
   * Walk through this frame and find all bonds again.
   */
  public void rebond() {
    if (true)
      return;
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

  public void addAtomVector(int atomIndex, double[] vector) {
    Atom atom = getJmolAtomAt(atomIndex);
    Point3d vector3d = new Point3d(vector);
    atom.setVector(vector3d);
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

  private static final Point3d zeroPoint = new Point3d();
  private void buildJmolFrame() {
    int atomCount = getAtomCount();
    jmframe = new JmolFrame(control, atomCount);
    char chainLast = '?';
    int indexLastCA = -1;
    AtomShape atomShapeLastCA = null;
    ProteinProp pprop = null;
    for (int i = 0; i < atomCount; ++i) {
      Atom jmolAtom = getJmolAtomAt(i);
      AtomShape atomShape = jmframe.addAtom(jmolAtom);
      Atom[] bondedAtoms = jmolAtom.getBondedAtoms();
      if (bondedAtoms != null)
        for (int j = bondedAtoms.length; --j >= 0; ) {
          jmframe.bondAtomShapes(atomShape, bondedAtoms[j],
                                 jmolAtom.getBondOrder(j));
        }
      if (jmolAtom.hasVector()) {
        Point3d atomPoint = jmolAtom.getPoint3D();
        Point3d vectorPoint = new Point3d(jmolAtom.getVector());
        vectorPoint.scaleAdd(2, atomPoint);
        jmframe.addLineShape(new ArrowLineShape(atomPoint, vectorPoint));
      }
      pprop = jmolAtom.getProteinProp();
      if (pprop != null) {
        char chainThis = pprop.getChain();
        if (chainThis == chainLast) {
          if (pprop.getName().equals("CA")) {
            if (atomShapeLastCA != null) {
              jmframe.bondAtomShapes(atomShapeLastCA, atomShape,
                                     BondShape.BACKBONE);
            }
            atomShapeLastCA = atomShape;
          }
        } else {
          chainLast = chainThis;
          atomShapeLastCA = null;
        }
      } else {
        chainLast = '?';
      }
    }

    if (this instanceof CrystalFrame) {
      CrystalFrame crystalFrame = (CrystalFrame) this;
      double[][] rprimd = crystalFrame.getRprimd();
      
      // The three primitives vectors with arrows
      for (int i = 0; i < 3; i++)
        jmframe.addLineShape(new ArrowLineShape(zeroPoint,
                                                new Point3d(rprimd[i])));
      
      // The full primitive cell
      if (true) {
        // Depends on the settings...TODO
        Vector boxEdges = crystalFrame.getBoxEdges();
        for (int i = 0; i < boxEdges.size(); i = i + 2) {
          LineShape ls = new LineShape((Point3d) boxEdges.elementAt(i),
                                       (Point3d) boxEdges.elementAt(i + 1));
          jmframe.addCrystalCellLine(ls);
        }
      }
    }
    jmframe.finalize();
  }

  public JmolFrame getJmolFrame() {
    if (jmframe == null)
      buildJmolFrame();
    return jmframe;
  }
}
