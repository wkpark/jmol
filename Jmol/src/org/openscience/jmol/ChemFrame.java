
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
  private static float[] axes = {
    0.0f, 0.0f, 0.0f,                     // axis vectors in
    1.0f, 0.0f, 0.0f,                     // real space
    0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f
  };
  private static float[] cellaxes = {
    0.0f, 0.0f, 0.0f,                     // axis vectors in
    5.0f, 0.0f, 0.0f,                     // real space
    0.0f, 5.0f, 0.0f, 0.0f, 0.0f, 5.0f
  };

  private static String[] AxesLabels = {
    "x", "y", "z"
  };
  private static String[] CellAxesLabels = {
    "a", "b", "c"
  };

  /*
     pickedAtoms and napicked are static because
     they deal with deformations or measurements that will persist
     across frames.
  */
  private static boolean[] pickedAtoms;
  private static int napicked;

  // Added by T.GREY for quick drawing on atom movement
  private static boolean doingMoveDraw = false;

  // This stuff can vary for each frame in the dynamics:

  private String info;                    // The title or info string for this frame.
  private float[] vert;                   // atom vertices in real space
  private float[] vect;                   // vert + dx  for vectors in real space

  private int[] tvert;                    // atom positions transformed to screen space
  private int[] tvect;                    // vector ends transformed to screen space
  private int[] taxes;                    // axes transformed to screen space
  private int[] tcellaxes;                // axes transformed to screen space
  private Atom[] atoms;               // array of atom types
  private Bond[] bonds;                   // array of bonds
  private Vector[] aProps;                // array of Vector of atom properties
  private Vector atomProps;    // Vector of all the atom properties present in this frame
  private Vector frameProps;    // Vector of all the frame properties present in this frame
  private boolean hasFrameProperties = false;
  private boolean hasAtomProperties = false;
  private boolean hasVectors = false;
  private int[] ZsortMap;
  private int nvert = 0;
  private int nbonds, maxbonds;
  private int maxbpa = 20;                // maximum number of bonds per atom
  private int[] nBpA;                     // number of bonds per atom
  private int[][] inBonds;                // atom i's membership in it's jth bond points 

  // to which bond?
  private boolean[] bondDrawn;
  private int[] bondEnd1;
  private int[] bondEnd2;
  private Vector dlist, alist, dhlist;    // distance, angle, dihedral lists

  private float xmin, xmax, ymin, ymax, zmin, zmax;

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
  public static int getNpicked() {
    return napicked;
  }

  // Added by T.GREY for quick drawing on atom movement

  /**
   * Sets whether we are in ultra-hasty on the move drawing mode
   * @PARAM movingOn If true then turns on quick mode, if false then turns it off (if its on)
   */
  public void setMovingDrawMode(boolean movingOn) {
    doingMoveDraw = movingOn;
  }

  // Added by T.GREY for quick drawing on atom movement

  /**
   * Gets whether we are in ultra-hasty on the move drawing mode
   */
  public boolean getMovingDrawMode() {
    return doingMoveDraw;
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

    frameProps = new Vector();
    atomProps = new Vector();
    vert = new float[na * 3];
    vect = new float[na * 3];
    atoms = new Atom[na];
    aProps = new Vector[na];
    pickedAtoms = new boolean[na];
    nBpA = new int[na];
    inBonds = new int[na][maxbpa];
    for (int i = 0; i < na; i++) {
      vert[3 * i] = 0.0f;
      vert[3 * i + 1] = 0.0f;
      vert[3 * i + 2] = 0.0f;
      vect[3 * i] = 0.0f;
      vect[3 * i + 1] = 0.0f;
      vect[3 * i + 2] = 0.0f;
      pickedAtoms[i] = false;
    }
  }

  /**
   * Constructor for a ChemFrame with an unknown number of atoms.
   *
   */
  public ChemFrame() {
    this(100);
  }

  /**
   * returns a Vector containing the list of Physical Properties
   * associated with this frame
   */
  public Vector getFrameProps() {
    return frameProps;
  }

  /**
   * Adds a PhysicalProperty to this ChemFrame
   *
   * @param property the PhysicalProperty to be added.
   */
  public void addFrameProperty(PhysicalProperty property) {

    String desc = property.getDescriptor();

    // Make sure we don't have an identical property already defined:

    boolean found = false;
    for (Enumeration e = frameProps.elements(); e.hasMoreElements(); ) {
      PhysicalProperty fp = (PhysicalProperty) (e.nextElement());
      String fpd = fp.getDescriptor();
      if (desc.equals(fpd)) {
        found = true;
      }
    }

    if (!found) {
      frameProps.addElement(property);
    }
  }

  public void setCellAxis(String axis, String fract, float x) {

    int index = -1;
    if (fract.equals("x")) {
      index = 0;
    }
    if (fract.equals("y")) {
      index = 1;
    }
    if (fract.equals("z")) {
      index = 2;
    }
    if (index != -1) {
      if (axis.equals("a-axis")) {
        cellaxes[3 + index] = x;
      } else if (axis.equals("b-axis")) {
        cellaxes[6 + index] = x;
      } else if (axis.equals("c-axis")) {
        cellaxes[9 + index] = x;
      }
    }
  }

  /**
   * returns a Vector containing the list of PhysicalProperty descriptors
   * present for the atoms in this frame
   */
  public Vector getAtomProps() {
    return atomProps;
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
  public int addAtom(String name, float x, float y, float z)
          throws Exception {
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
  public int addAtom(BaseAtomType type, float x, float y, float z)
          throws Exception {

    int i = nvert;
    if (i >= vert.length) {
      increaseArraySizes(2 * vert.length);
    }

    atoms[i] = new Atom(type);
    nBpA[i] = 0;
    aProps[i] = new Vector();
    if (AutoBond) {
      for (int j = 0; j < i; j++) {
        float d2 = 0.0f;
        float dx = vert[3 * j] - x;
        float dy = vert[3 * j + 1] - y;
        float dz = vert[3 * j + 2] - z;
        d2 += dx * dx + dy * dy + dz * dz;
        Atom b = atoms[j];
        float dr = bondFudge
                     * ((float) atoms[i].getType().getCovalentRadius()
                        + (float) b.getType().getCovalentRadius());
        float dr2 = dr * dr;

        if (d2 <= dr2) {

          // We found a bond
          int k = nbonds;
          if (k >= maxbonds) {
            if (bonds == null) {
              maxbonds = 100;
              bonds = new Bond[maxbonds];
              bondDrawn = new boolean[maxbonds];
              bondEnd1 = new int[maxbonds];
              bondEnd2 = new int[maxbonds];
            } else {
              maxbonds *= 2;
              Bond nb[] = new Bond[maxbonds];
              System.arraycopy(bonds, 0, nb, 0, bonds.length);
              bonds = nb;
              boolean bd[] = new boolean[maxbonds];
              System.arraycopy(bondDrawn, 0, bd, 0, bondDrawn.length);
              bondDrawn = bd;
              int be1[] = new int[maxbonds];
              System.arraycopy(bondEnd1, 0, be1, 0, bondEnd1.length);
              bondEnd1 = be1;
              int be2[] = new int[maxbonds];
              System.arraycopy(bondEnd2, 0, be2, 0, bondEnd2.length);
              bondEnd2 = be2;
            }
          }
          Bond bond = new Bond(atoms[i], b);
          bonds[k] = bond;
          bondEnd1[k] = i;
          bondEnd2[k] = j;

          int na = nBpA[i] + 1;
          int nb = nBpA[j] + 1;

          if (na >= maxbpa) {
            throw new JmolException("ChemFrame.rebond",
                    "max bonds per atom exceeded");
          }
          if (nb >= maxbpa) {
            throw new JmolException("ChemFrame.rebond",
                    "max bonds per atom exceeded");
          }

          inBonds[i][na - 1] = k;
          inBonds[j][nb - 1] = k;
          nBpA[j] = nb;
          nBpA[i] = na;

          nbonds++;
        }
      }
    }

    i *= 3;
    vert[i] = x;
    vert[i + 1] = y;
    vert[i + 2] = z;

    return nvert++;
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
  public int addAtom(int atomicNumber, float x, float y, float z)
          throws Exception {

    BaseAtomType baseType = BaseAtomType.get(atomicNumber);
    if (baseType == null) {
      return -1;
    }
    return addAtom(baseType, x, y, z);
  }

  /**
   * Adds an atom to the frame and finds all bonds between the
   * new atom and pre-existing atoms in the frame
   *
   * @param name the name of the extended atom type for the new atom
   * @param x the x coordinate of the new atom
   * @param y the y coordinate of the new atom
   * @param z the z coordinate of the new atom
   * @param props a Vector containing the properties of this atom
   */
  public int addPropertiedAtom(
          String name, float x, float y, float z, Vector props)
            throws Exception {

    hasAtomProperties = true;
    int i = addAtom(name, x, y, z);
    aProps[i] = props;

    for (int j = 0; j < props.size(); j++) {
      PhysicalProperty p = (PhysicalProperty) props.elementAt(j);
      String desc = p.getDescriptor();
      if (desc.equals("Vector")) {
        hasVectors = true;
        VProperty vp = (VProperty) p;
        double[] vtmp = new double[3];
        vtmp = vp.getVector();
        int k = i * 3;
        vect[k] = (float) (x + vtmp[0]);
        vect[k + 1] = (float) (y + vtmp[1]);
        vect[k + 2] = (float) (z + vtmp[2]);
      }

      // Update the atomProps if we found a new property
      if (atomProps.indexOf(desc) < 0) {
        atomProps.addElement(desc);
      }

    }
    return i;
  }

  /**
   * Adds a PhysicalProperty to the atom at the specified index.
   *
   * @param vertexIndex index of the vertex to which property is added.
   * @param property the PhysicalProperty to be added.
   */
  public void addProperty(int vertexIndex, PhysicalProperty property) {

    aProps[vertexIndex].addElement(property);
    String desc = property.getDescriptor();
    if (desc.equals("Vector") && (property instanceof VProperty)) {
      hasVectors = true;
      VProperty vp = (VProperty) property;
      double[] vtmp = new double[3];
      vtmp = vp.getVector();
      int k = vertexIndex * 3;
      vect[k] = (float) (vert[k] + vtmp[0]);
      vect[k + 1] = (float) (vert[k + 1] + vtmp[1]);
      vect[k + 2] = (float) (vert[k + 2] + vtmp[2]);
    }

    // Update the atomProps if we found a new property
    if (atomProps.indexOf(desc) < 0) {
      atomProps.addElement(desc);
    }
  }

  /**
   * returns the number of atoms in the ChemFrame
   */
  public int getNumberOfAtoms() {
    return nvert;
  }

  /**
   *  Returns the number of atoms in this frame.
   *
   *  @return the number of atoms in this frame.
   */
  public int getAtomCount() {
    return nvert;
  }

  /**
   *  Returns the number of bonds in this frame.
   *
   *  @return the number of bonds in this frame.
   */
  public int getBondCount() {
    return nbonds;
  }

  public float getXMin() {
    return xmin;
  }

  public float getXMax() {
    return xmax;
  }

  public float getYMin() {
    return ymin;
  }

  public float getYMax() {
    return ymax;
  }

  public float getZMin() {
    return zmin;
  }

  public float getZMax() {
    return zmax;
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
   *  Returns the bond at the given index.
   *
   *  @param index the index of the bond.
   *  @return the bond at the given index.
   */
  public Bond getBondAt(int index) {
    return bonds[index];
  }

  /**
   * Returns the atom index of the first endpoint of the given bond.
   *
   * @param index the index of the bond.
   * @return the atom at the first endpoint of the bond.
   */
  public int getBondEnd1(int index) {
    return bondEnd1[index];
  }

  /**
   * Returns the atom index of the first endpoint of the given bond.
   *
   * @param index the index of the bond.
   * @return the atom at the first endpoint of the bond.
   */
  public int getBondEnd2(int index) {
    return bondEnd2[index];
  }

  /**
   * Returns the Number of Bonds that connect to an atom.
   *
   * @param index the index of the atom.
   * @return the number of bonds that this atom connects with.
   */
  public int getNumberOfBondsForAtom(int index) {
    return nBpA[index];
  }

  /**
   * Returns the index of the atom at the other end of a bond.
   *
   * @param theAtom the index of the first atom in the bond
   * @param theBond which of the bonds that that theAtom has (NB: this is <b>not</b> the index of the bond in the overall bond list)
   * @return the index of the other atom on the end of the bond
   */
  public int getOtherBondedAtom(int theAtom, int theBond) {
    return inBonds[theAtom][theBond];
  }


  /**
   * returns the coordinates of the i'th atom
   *
   * @param i the index of the atom
   */
  public double[] getAtomCoords(int i) {

    int k = i * 3;
    double[] coords = {
      vert[k], vert[k + 1], vert[k + 2]
    };
    return coords;
  }

  /**
   * returns the properties of the i'th atom
   *
   * @param i the index of the atom
   */
  public Vector getAtomProps(int i) {
    Vector prps = aProps[i];
    return prps;
  }

  /**
   * Transform all the points in this model
   */
  public void transform() {

    if (nvert <= 0) {
      return;
    }
    if ((tvert == null) || (tvert.length < nvert * 3)) {
      tvert = new int[nvert * 3];
    }
    for (int i = 0; i < nvert * 3; i += 3) {
      Point3d pt = new Point3d(vert[i], vert[i + 1], vert[i + 2]);
      mat.transform(pt);
      tvert[i] = (int) pt.x;
      tvert[i + 1] = (int) pt.y;
      tvert[i + 2] = (int) pt.z;
    }
    if ((taxes == null) || (taxes.length < 12)) {
      taxes = new int[12];
    }
    for (int i = 0; i < 4 * 3; i += 3) {
      Point3d pt = new Point3d(axes[i], axes[i + 1], axes[i + 2]);
      mat.transform(pt);
      taxes[i] = (int) pt.x;
      taxes[i + 1] = (int) pt.y;
      taxes[i + 2] = (int) pt.z;
    }
    if ((tcellaxes == null) || (tcellaxes.length < 12)) {
      tcellaxes = new int[12];
    }
    for (int i = 0; i < 4 * 3; i += 3) {
      Point3d pt = new Point3d(cellaxes[i], cellaxes[i + 1], cellaxes[i + 2]);
      mat.transform(pt);
      tcellaxes[i] = (int) pt.x;
      tcellaxes[i + 1] = (int) pt.y;
      tcellaxes[i + 2] = (int) pt.z;
    }
    if (hasVectors) {
      if ((tvect == null) || (tvect.length < nvert * 3)) {
        tvect = new int[nvert * 3];
      }
      for (int i = 0; i < nvert * 3; i += 3) {
        Point3d pt = new Point3d(vect[i], vect[i + 1], vect[i + 2]);
        mat.transform(pt);
        tvect[i] = (int) pt.x;
        tvect[i + 1] = (int) pt.y;
        tvect[i + 2] = (int) pt.z;
      }
    }
  }

  /**
   * Paint this model to a graphics context.  It uses the matrix
   * associated with this model to map from model space to screen
   * space.
   *
   * @param g the Graphics context to paint to
   */
  public synchronized void paint(Graphics g, DisplaySettings settings) {

    if ((vert == null) || (nvert <= 0)) {
      return;
    }
    boolean drawHydrogen = settings.getShowHydrogens();
    transform();


    if (settings.getShowAxes()) {

      for (int i = 1; i < 4; i++) {

        int x0 = taxes[0];
        int y0 = taxes[1];
        int x1 = taxes[i * 3];
        int y1 = taxes[i * 3 + 1];
        int sz = (int) settings.getScreenSize(taxes[i * 3] + 2);

        ArrowLine al = new ArrowLine(g, x0, y0, x1, y1, false, true, 0,
                         3 + sz);

        Font font = new Font("Helvetica", Font.PLAIN, sz);
        FontMetrics fontMetrics = g.getFontMetrics(font);
        String s = AxesLabels[i - 1];
        int j = fontMetrics.stringWidth(s);
        int k = fontMetrics.getAscent();
        g.drawString(s, x1, y1);
      }
    }

    if (settings.getShowCellAxes()) {

      for (int i = 1; i < 4; i++) {

        int x0 = tcellaxes[0];
        int y0 = tcellaxes[1];
        int x1 = tcellaxes[i * 3];
        int y1 = tcellaxes[i * 3 + 1];
        int sz = (int) settings.getScreenSize(tcellaxes[i * 3] + 2);

        ArrowLine al = new ArrowLine(g, x0, y0, x1, y1, false, true, 0,
                         3 + sz);

        Font font = new Font("Helvetica", Font.PLAIN, sz);
        FontMetrics fontMetrics = g.getFontMetrics(font);
        String s = CellAxesLabels[i - 1];
        int j = fontMetrics.stringWidth(s);
        int k = fontMetrics.getAscent();
        g.drawString(s, x1, y1);
      }
    }

    int v[] = tvert;
    int zs[] = ZsortMap;
    if (zs == null) {
      ZsortMap = zs = new int[nvert];
      for (int i = nvert; --i >= 0; ) {
        zs[i] = i * 3;
      }
    }

    //Added by T.GREY for quick-draw on move support
    if (!doingMoveDraw) {

      /*
       * I use a bubble sort since from one iteration to the next, the sort
       * order is pretty stable, so I just use what I had last time as a
       * "guess" of the sorted order.  With luck, this reduces O(N log N)
       * to O(N)
       */

      for (int i = nvert - 1; --i >= 0; ) {
        boolean flipped = false;
        for (int j = 0; j <= i; j++) {
          int a = zs[j];
          int b = zs[j + 1];
          if (v[a + 2] > v[b + 2]) {
            zs[j + 1] = a;
            zs[j] = b;
            flipped = true;
          }
        }
        if (!flipped) {
          break;
        }
      }
    }
    int lg = 0;
    int lim = nvert;
    if ((lim <= 0) || (nvert <= 0)) {
      return;
    }

    for (int k = 0; k < nbonds; k++) {
      bondDrawn[k] = false;
    }

    for (int i = 0; i < lim; i++) {
      int j = zs[i];
      if (settings.getShowBonds()) {
        int na = nBpA[j / 3];
        for (int k = 0; k < na; k++) {
          int which = inBonds[j / 3][k];
          if (!drawHydrogen && bonds[which].bindsHydrogen()) {

            // tricky... just pretend it has already been drawn
            bondDrawn[which] = true;
          }
          if (!bondDrawn[which]) {
            int l;
            if (bondEnd1[which] == j / 3) {
              l = 3 * bondEnd2[which];
              bonds[which].paint(g, settings, v[j], v[j + 1], v[j + 2], v[l],
                      v[l + 1], v[l + 2], doingMoveDraw);
            } else {
              l = 3 * bondEnd1[which];
              bonds[which].paint(g, settings, v[l], v[l + 1], v[l + 2], v[j],
                      v[j + 1], v[j + 2], doingMoveDraw);
            }

          }
        }
      }

      //Added by T.GREY for quick-draw on move support
      if (settings.getShowAtoms() && !doingMoveDraw) {

        // don't paint if atom is a hydrogen and !showhydrogens
        if (!drawHydrogen
                && (atoms[j / 3].getType().getAtomicNumber() == 1)) {

          // atom is an hydrogen and should not be painted
        } else {
          atoms[j / 3].paint(g, settings, v[j], v[j + 1], v[j + 2],
                  j / 3 + 1, aProps[j / 3], pickedAtoms[j / 3]);
        }
      }

      if (settings.getShowVectors() && hasVectors) {
        ArrowLine al = new ArrowLine(g, v[j], v[j + 1], tvect[j],
                         tvect[j + 1], false, true, 0,
                         (int) settings.getScreenSize(tvect[j + 2]));
      }

    }

    if (dlist != null) {
      for (Enumeration e = dlist.elements(); e.hasMoreElements(); ) {
        Distance d = (Distance) e.nextElement();
        int[] al = d.getAtomList();
        int l = 3 * al[0];
        int j = 3 * al[1];
        try {
          d.paint(g, settings, v[l], v[l + 1], v[l + 2], v[j], v[j + 1],
                  v[j + 2]);
        } catch (Exception ex) {
        }
      }
    }
    if (alist != null) {
      for (Enumeration e = alist.elements(); e.hasMoreElements(); ) {
        Angle an = (Angle) e.nextElement();
        int[] al = an.getAtomList();
        int l = 3 * al[0];
        int j = 3 * al[1];
        int k = 3 * al[2];
        try {
          an.paint(g, settings, v[l], v[l + 1], v[l + 2], v[j], v[j + 1],
                  v[j + 2], v[k], v[k + 1], v[k + 2]);
        } catch (Exception ex) {
        }
      }
    }
    if (dhlist != null) {
      for (Enumeration e = dhlist.elements(); e.hasMoreElements(); ) {
        Dihedral dh = (Dihedral) e.nextElement();
        int[] dhl = dh.getAtomList();
        int l = 3 * dhl[0];
        int j = 3 * dhl[1];
        int k = 3 * dhl[2];
        int m = 3 * dhl[3];
        try {
          dh.paint(g, settings, v[l], v[l + 1], v[l + 2], v[j], v[j + 1],
                  v[j + 2], v[k], v[k + 1], v[k + 2], v[m], v[m + 1],
                    v[m + 2]);
        } catch (Exception ex) {
        }
      }
    }
  }

  /**
   * return a Vector with selected atoms
   */
  public Vector getSelectedAtoms() {

    Vector result = new Vector();
    for (int i = 0; i < nvert; i++) {
      if (pickedAtoms[i]) {
        result.add(new Integer(i + 1));
      }
      ;
    }
    return result;
  }

  /**
   * Add atom by its number
   */
  public void selectAtomByNumber(int atom) {

    if ((nvert <= 0) || (atom > nvert)) {
      return;
    }
    pickedAtoms[atom - 1] = true;
    napicked++;
  }

  /**
   * Add all atoms in this frame to the list of picked atoms
   */
  public void selectAll() {

    if (nvert <= 0) {
      return;
    }
    napicked = 0;
    for (int i = 0; i < nvert; i++) {
      pickedAtoms[i] = true;
      napicked++;
    }
  }

  /**
   * Remove all atoms in this frame from the list of picked atoms
   */
  public void deselectAll() {

    if (nvert <= 0) {
      return;
    }
    for (int i = 0; i < nvert; i++) {
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
    if (pickedAtoms[smallest]) {
      pickedAtoms[smallest] = false;
      napicked = 0;
    } else {
      pickedAtoms[smallest] = true;
      napicked = 1;
    }
    for (int i = 0; i < nvert; i++) {
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

    if (nvert <= 0) {
      return;
    }
    transform();
    int v[] = tvert;
    napicked = 0;
    for (int i = 0; i < nvert; i++) {
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

    if (nvert <= 0) {
      return;
    }
    transform();
    int v[] = tvert;
    for (int i = 0; i < nvert; i++) {
      if (isAtomInRegion(i, x1, y1, x2, y2)) {
        if (!pickedAtoms[i]) {
          pickedAtoms[i] = true;
          napicked++;
        }
      }
    }
  }

  private boolean isAtomInRegion(int n, int x1, int y1, int x2, int y2) {

    int x = tvert[3 * n];
    int y = tvert[3 * n + 1];
    if ((x > x1) && (x < x2)) {
      if ((y > y1) && (y < y2)) {
        return true;
      }
    }
    return false;
  }

  private int getNearestAtom(int x, int y) {

    if (nvert <= 0) {
      return -1;
    }
    transform();
    int v[] = tvert;
    int dx, dy, dr2;
    int smallest = -1;
    int smallr2 = Integer.MAX_VALUE;
    for (int i = 0; i < nvert; i++) {
      dx = v[3 * i] - x;
      dy = v[3 * i + 1] - y;
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
   * Find the bounding box of this model
   */
  public void findBB() {

    if (nvert <= 0) {
      return;
    }
    float v[] = vert;
    float xmin = v[0], xmax = xmin;
    float ymin = v[1], ymax = ymin;
    float zmin = v[2], zmax = zmin;
    for (int i = nvert * 3; (i -= 3) > 0; ) {
      float x = v[i];
      if (x < xmin) {
        xmin = x;
      }
      if (x > xmax) {
        xmax = x;
      }
      float y = v[i + 1];
      if (y < ymin) {
        ymin = y;
      }
      if (y > ymax) {
        ymax = y;
      }
      float z = v[i + 2];
      if (z < zmin) {
        zmin = z;
      }
      if (z > zmax) {
        zmax = z;
      }
    }
    this.xmax = xmax;
    this.xmin = xmin;
    this.ymax = ymax;
    this.ymin = ymin;
    this.zmax = zmax;
    this.zmin = zmin;
  }

  /**
   * Walk through this frame and find all bonds again.
   */
  public void rebond() throws Exception {

    // zero out the currently existing bonds:
    nbonds = 0;
    for (int i = 0; i < nvert; i++) {
      nBpA[i] = 0;
    }

    // do a n*(n-1) scan to get new bonds:
    if (AutoBond) {
      for (int i = 0; i < nvert - 1; i++) {
        Atom a = atoms[i];
        float ax = vert[3 * i];
        float ay = vert[3 * i + 1];
        float az = vert[3 * i + 2];
        for (int j = i; j < nvert; j++) {
          float d2 = 0.0f;
          float dx = vert[3 * j] - ax;
          float dy = vert[3 * j + 1] - ay;
          float dz = vert[3 * j + 2] - az;
          d2 += dx * dx + dy * dy + dz * dz;
          Atom b = atoms[j];
          float dr = bondFudge
                       * ((float) a.getType().getCovalentRadius()
                          + (float) b.getType().getCovalentRadius());
          float dr2 = dr * dr;

          if (d2 <= dr2) {

            // We found a bond
            int k = nbonds;
            if (k >= maxbonds) {
              if (bonds == null) {
                maxbonds = 100;
                bonds = new Bond[maxbonds];
                bondDrawn = new boolean[maxbonds];
                bondEnd1 = new int[maxbonds];
                bondEnd2 = new int[maxbonds];
              } else {
                maxbonds *= 2;
                Bond nb[] = new Bond[maxbonds];
                System.arraycopy(bonds, 0, nb, 0, bonds.length);
                bonds = nb;
                boolean bd[] = new boolean[maxbonds];
                System.arraycopy(bondDrawn, 0, bd, 0, bondDrawn.length);
                bondDrawn = bd;
                int be1[] = new int[maxbonds];
                System.arraycopy(bondEnd1, 0, be1, 0, bondEnd1.length);
                bondEnd1 = be1;
                int be2[] = new int[maxbonds];
                System.arraycopy(bondEnd2, 0, be2, 0, bondEnd2.length);
                bondEnd2 = be2;
              }
            }
            Bond bt = new Bond(a, b);
            bonds[k] = bt;
            bondEnd1[k] = i;
            bondEnd2[k] = j;

            int na = nBpA[i] + 1;
            int nb = nBpA[j] + 1;

            if (na >= maxbonds) {
              throw new JmolException("ChemFrame.rebond",
                      "max bonds per atom exceeded");
            }
            if (nb >= maxbonds) {
              throw new JmolException("ChemFrame.rebond",
                      "max bonds per atom exceeded");
            }

            inBonds[i][na - 1] = k;
            inBonds[j][nb - 1] = k;
            nBpA[j] = nb;
            nBpA[i] = na;

            nbonds++;
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

    float nv[] = new float[newArraySize * 3];
    System.arraycopy(vert, 0, nv, 0, vert.length);
    vert = nv;

    Atom nat[] = new Atom[newArraySize];
    System.arraycopy(atoms, 0, nat, 0, atoms.length);
    atoms = nat;

    Vector nap[] = new Vector[newArraySize];
    System.arraycopy(aProps, 0, nap, 0, aProps.length);
    aProps = nap;

    float nve[] = new float[newArraySize * 3];
    System.arraycopy(vect, 0, nve, 0, vect.length);
    vect = nve;

    boolean np[] = new boolean[newArraySize];
    System.arraycopy(pickedAtoms, 0, np, 0, pickedAtoms.length);
    pickedAtoms = np;

    int nbpa[] = new int[newArraySize];
    System.arraycopy(nBpA, 0, nbpa, 0, nBpA.length);
    nBpA = nbpa;

    int inb2[][] = new int[newArraySize][maxbpa];
    System.arraycopy(inBonds, 0, inb2, 0, inBonds.length);
    inBonds = inb2;
  }
}

