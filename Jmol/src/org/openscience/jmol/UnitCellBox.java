
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

import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;
import java.lang.reflect.Array;


/**
 * A class to store the the primitive and base vectors.
 *
 * @author Fabian Dortu (Fabian.Dortu@wanadoo.be)
 * @version 1.1
 */
public class UnitCellBox {

  // Is the representation of the base vectors
  // cartesian or crystallographic?
  private boolean isPrimVectorsCart;

  // We store either Cartesian or Crystallographic representation
  // because of numerical errors when transforming from one representation
  // to the other one.

  //Cartesian representation
  private float[][] rprim;    // adimentionnal
  private float[] acell;      // in angstrom


  //Crystallographic representation
  private float[] edges;      //in angstrom
  private float[] angles;     //in degrees


  //Space Group
  private String spgroup = "1";

  //Info
  private String info = "";

  //Atomic positions are stored in reduced coordinates
  // in angstrom
  private int[] atomType;
  private float[][] atomPos;


  /**
   * Set the primitive vectors of the crystal.
   *
   * @param rprim a <code>float[3][3]</code> value.<br>
   * The three dimensionless primitive translations in real space.
   * first vector : rprim[0][0], rprim[0][1], rprim[0][1] <br>
   * second vector : rprim[1][0], rprim[1][1], rprim[1][1] <br>
   * third vector : rprim[2][0], rprim[2][1], rprim[2][1] <br>
   * Each vector will be multiplied
   * by the corresponding <code>acell</code> value to give the dimensional
   * primitive vectors in angstrom.
   * @param acell a <code>float[3]</code> value. See above.
   * @param isAtomicPositionsCart a <code>boolean</code> value.<br>
   * Determines if the atomic positions (see <code>atomPos</code>)
   * are given in reduced or
   * cartesian coordinates.
   * @param atomType an <code>int[]</code> value. <br>
   * The atom types represented by their atomic number.
   * @param atomPos a <code>float[][]</code> value.<br>
   * The atomic positions.
   */
  public UnitCellBox(float[][] rprim, float[] acell,
      boolean isAtomicPositionsCart, int[] atomType, float[][] atomPos) {

    this.isPrimVectorsCart = true;
    this.edges = null;
    this.angles = null;
    this.rprim = new float[3][3];
    this.rprim = rprim;
    this.acell = new float[3];
    this.acell = acell;
    this.atomType = new int[Array.getLength(atomType)];
    this.atomType = atomType;
    this.atomPos = new float[Array.getLength(atomPos)][3];
    if (isAtomicPositionsCart) {
      this.atomPos = cartToRed(atomPos, getRprimd());
    } else {
      this.atomPos = atomPos;
    }
  }


  /**
   * Set the primitive vectors of the crystal.
   *
   * @param edges a <code>float[3]</code> value.<br>. In angstrom.
   * @param angles a <code>float[3]</code> value. <br> In degree.
   * @param isAtomicPositionCart a <code>boolean</code> value
   * @param atomType an <code>int[]</code> value
   * @param atomPos a <code>float[][]</code> value
   */
  public UnitCellBox(float[] edges, float[] angles,
      boolean isAtomicPositionCart, int[] atomType, float[][] atomPos) {

    this.isPrimVectorsCart = false;
    this.rprim = null;
    this.acell = null;
    this.edges = new float[3];
    this.edges = edges;
    this.angles = new float[3];
    this.angles = angles;
    this.atomType = new int[Array.getLength(atomType)];
    this.atomPos = new float[Array.getLength(atomPos)][3];
    this.atomType = atomType;
    if (isAtomicPositionCart) {
      this.atomPos = cartToRed(atomPos, getRprimd());
    } else {
      this.atomPos = atomPos;
    }
  }

  /**
   * Creates a new <code>UnitCellBox</code> instance.
   * Use default values:<br>
   * Cubic unit cell.
   */
  public UnitCellBox() {

    isPrimVectorsCart = true;
    rprim = new float[3][3];
    rprim[0][0] = 1;
    rprim[0][1] = 0;
    rprim[0][2] = 0;

    rprim[1][0] = 0;
    rprim[1][1] = 1;
    rprim[1][2] = 0;


    rprim[2][0] = 0;
    rprim[2][1] = 0;
    rprim[2][2] = 1;

    acell = new float[3];
    acell[0] = 10;
    acell[1] = 10;
    acell[2] = 10;
  }

  /**
   * Determines if the primitive vectors are stored using the cartesian
   * representation (rprim,acell) or the crystallographic representation
   * (a,b,c,alpha,beta,gamma).
   *
   * @return a <code>boolean</code> value
   */
  public boolean isPrimVectorsCartesian() {
    return isPrimVectorsCart;
  }

  /**
   * Set the primitive vectors using the cartesian representation.
   *
   * @param rprim a <code>float[][]</code> value
   * @param acell a <code>float[]</code> value
   */
  public void setPrimVectorsCartesian(float[][] rprim, float[] acell) {
    isPrimVectorsCart = true;
    this.rprim = rprim;
    this.acell = acell;
  }

  /**
   * Set the primitive vectors using the crystallographic representation.
   *
   * @param edges a <code>float[]</code> value
   * @param angles a <code>float[]</code> value
   */
  public void setPrimVectorsCrystallo(float[] edges, float[] angles) {
    isPrimVectorsCart = false;
    this.edges = edges;
    this.angles = angles;
  }


  /**
   * Set the atomic positions in the unit cell in cartesian coordinates.
   *
   * @param atomPos a <code>float[][]</code> value
   */
  public void setCartesianPos(float[][] atomPos) {
    this.atomPos = cartToRed(atomPos, getRprimd());
  }

  /**
   * Set the atomic positions in the unit cell in reduced coordinates.
   *
   * @param atomPos a <code>float[][]</code> value
   */
  public void setReducedPos(float[][] atomPos) {
    this.atomPos = atomPos;
  }

  /**
   * Set the type of the atoms.
   *
   * @param atomType an <code>int[]</code> value
   */
  public void setAtomType(int[] atomType) {
    this.atomType = atomType;
  }

  /**
   * Set an information string.
   *
   * @param info a <code>String</code> value
   */
  public void setInfo(String info) {
    this.info = info;
  }

  /**
   * Get the infoormation string.
   *
   * @return a <code>String</code> value
   */
  public String getInfo() {
    return this.info;
  }

  /**
   * Get the atomic positions in cartesian coordinates.
   * @return a <code>float[][]</code> value
   */
  public float[][] getCartesianPos() {
    return redToCart(atomPos, getRprimd());
  }

  /**
   * Get the atomic positions in reduced coordinates.
   *
   * @return a <code>float[][]</code> value
   */
  public float[][] getReducedPos() {
    return atomPos;
  }

  /**
   * Get the number of atoms.
   *
   * @return an <code>int</code> value
   */
  public int getNumberOfAtoms() {
    return Array.getLength(atomPos);
  }

  /**
   * Get the atom type of atom <code>atindex</code>.
   *
   * @param atindex an <code>int</code> value
   * @return a <code>BaseAtomType</code> value
   */
  public BaseAtomType getAtomType(int atindex) {
    return (BaseAtomType.get(atomType[atindex]));
  }


  /**
   * Get the dimensionless primitive vectors.
   * first vector : rprim[0][0], rprim[0][1], rprim[0][1] <br>
   * second vector : rprim[1][0], rprim[1][1], rprim[1][1] <br>
   * third vector : rprim[2][0], rprim.[2][1], rprim[2][1] <br>
   * @return a <code>float[3][3]</code> value.<br>
   * Dimensionless primitive vectors.
   */
  public float[][] getRprim() {

    if (isPrimVectorsCart == true) {
      return this.rprim;
    } else {
      float[][] rprim = new float[3][3];
      float[] module = new float[3];
      rprim[0][0] = edges[0];
      rprim[0][1] = 0f;
      rprim[0][2] = 0f;


      rprim[1][0] = edges[1]
          * (float) Math.cos(Math.toRadians((double) angles[0]));
      rprim[1][1] = edges[1]
          * (float) Math.sqrt(1
            - Math.cos(Math.toRadians((double) angles[0]))
              * Math.cos(Math.toRadians((double) angles[0])));
      rprim[1][2] = 0f;

      rprim[2][0] = edges[2]
          * (float) Math.cos(Math.toRadians((double) angles[2]));
      rprim[2][1] =
          (edges[1] * edges[2] * (float) Math.cos(Math.toRadians((double) angles[1])) - rprim[1][0] * rprim[2][0])
            / (rprim[1][1]);
      rprim[2][2] = (float) Math.sqrt((double) (edges[2] * edges[2]
          - rprim[2][0] * rprim[2][0] - rprim[2][1] * rprim[2][1]));

      // make rprim unitary
      for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
          rprim[i][j] = rprim[i][j] / edges[i];
        }
      }

      return rprim;
    }
  }


  /**
   * Get the dimensionnal primitive vectors.
   * first vector : rprim[0][0], rprim[0][1], rprim[0][1] <br>
   * second vector : rprim[1][0], rprim[1][1], rprim[1][1] <br>
   * third vector : rprim[2][0], rprim.[2][1], rprim[2][1] <br>
   * @return a <code>float[3][3]</code> value.<br>
   * Dimensionless primitive vectors.
   */
  public float[][] getRprimd() {

    if (isPrimVectorsCart == true) {
      float[][] rprimd = new float[3][3];
      rprimd[0][0] = rprim[0][0] * acell[0];
      rprimd[0][1] = rprim[0][1] * acell[0];
      rprimd[0][2] = rprim[0][2] * acell[0];

      rprimd[1][0] = rprim[1][0] * acell[1];
      rprimd[1][1] = rprim[1][1] * acell[1];
      rprimd[1][2] = rprim[1][2] * acell[1];

      rprimd[2][0] = rprim[2][0] * acell[2];
      rprimd[2][1] = rprim[2][1] * acell[2];
      rprimd[2][2] = rprim[2][2] * acell[2];

      return rprimd;

    } else {
      float[][] rprimd = new float[3][3];
      float[][] rprim = getRprim();
      float[] acell = getAcell();
      rprimd[0][0] = rprim[0][0] * acell[0];
      rprimd[0][1] = rprim[0][1] * acell[0];
      rprimd[0][2] = rprim[0][2] * acell[0];

      rprimd[1][0] = rprim[1][0] * acell[1];
      rprimd[1][1] = rprim[1][1] * acell[1];
      rprimd[1][2] = rprim[1][2] * acell[1];

      rprimd[2][0] = rprim[2][0] * acell[2];
      rprimd[2][1] = rprim[2][1] * acell[2];
      rprimd[2][2] = rprim[2][2] * acell[2];

      return rprimd;
    }
  }


  /**
   * Describe <code>getAcell</code> method here.
   *
   * @return a <code>float[]</code> value
   */
  public float[] getAcell() {

    if (isPrimVectorsCart == true) {
      return this.acell;
    } else {
      return edges;
    }

  }

  /**
   * Describe <code>getEdges</code> method here.
   *
   * @return a <code>float[]</code> value
   */
  public float[] getEdges() {

    if (isPrimVectorsCart == false) {
      return this.edges;
    } else {
      float[] edges = new float[3];
      edges[0] = acell[0]
          * (float) Math.sqrt((double) (rprim[0][0] * rprim[0][0]
            + rprim[0][1] * rprim[0][1] + rprim[0][2] * rprim[0][2]));
      edges[1] = acell[1]
          * (float) Math.sqrt((double) (rprim[1][0] * rprim[1][0]
            + rprim[1][1] * rprim[1][1] + rprim[1][2] * rprim[1][2]));
      edges[2] = acell[2]
          * (float) Math.sqrt((double) (rprim[2][0] * rprim[2][0]
            + rprim[2][1] * rprim[2][1] + rprim[2][2] * rprim[2][2]));
      return edges;
    }
  }

  /**
   * Describe <code>getAngles</code> method here.
   *
   * @return a <code>float[]</code> value
   */
  public float[] getAngles() {

    if (isPrimVectorsCart == false) {
      return this.angles;
    } else {
      float[] angles = new float[3];
      float[] edges = new float[3];
      edges = getEdges();
      angles[0] = (float) Math.toDegrees(
          Math.acos(
            (double) (getRprimd()[0][0] * getRprimd()[1][0] + getRprimd()[0][1] * getRprimd()[1][1] + getRprimd()[0][2] * getRprimd()[1][2])
              / (edges[0] * edges[1])));

      angles[1] = (float) Math.toDegrees(
          Math.acos(
            (double) (getRprimd()[1][0] * getRprimd()[2][0] + getRprimd()[1][1] * getRprimd()[2][1] + getRprimd()[1][2] * getRprimd()[2][2])
              / (edges[1] * edges[2])));

      angles[2] = (float) Math.toDegrees(
          Math.acos(
            (double) (getRprimd()[2][0] * getRprimd()[0][0] + getRprimd()[2][1] * getRprimd()[0][1] + getRprimd()[2][2] * getRprimd()[0][2])
              / (edges[2] * edges[0])));
      return angles;
    }
  }


  /**
   * Transform an array of cartesian coordinates into reduced coordinates.
   *
   * @param cartPos a <code>float[][]</code> value. <br>
   * (<code>cartPos[i][0]</code> is the cartesian position
   * of atom i in direction x.)
   * @param rprimd a <code>float[][]</code> value
   * @return a <code>float[][]</code> value
   */
  public static float[][] cartToRed(float[][] cartPos, float[][] rprimd) {

    int natom = Array.getLength(cartPos);
    float[][] redPos = new float[natom][3];

    Matrix3f rprimdTInv = new Matrix3f();

    //the transpose of rprimd
    rprimdTInv.transpose(arrayToMatrix3f(rprimd));

    //the inverse of rprimd transposed
    try {
      rprimdTInv.invert();
    } catch (javax.vecmath.SingularMatrixException e) {
      System.out.println(
          "Ooups! The base vectors you entered seem not to be valid.");
    }

    //cartesian, angstrom

    float[] xcart = new float[3];
    float[] xred = new float[3];

    for (int i = 0; i < natom; i++) {
      for (int j = 0; j < 3; j++) {
        xcart[j] = cartPos[i][j];
      }
      xred = mulVec(matrix3fToArray(rprimdTInv), xcart);
      redPos[i][0] = xred[0];
      redPos[i][1] = xred[1];
      redPos[i][2] = xred[2];
    }
    return redPos;

  }

  /**
   *  Transform an array of reduced coordinates into cartesian coordinates.
   *
   * @param redPos a <code>float[][]</code> value
   * (<code>redPos[i][0]</code> is the reduced position
   * of atom i in direction 0.)
   * @param rprimd a <code>float[][]</code> value
   * @return a <code>float[][]</code> value
   */
  public static float[][] redToCart(float[][] redPos, float[][] rprimd) {

    int natom = Array.getLength(redPos);
    float[][] cartPos = new float[natom][3];

    Matrix3f rprimdT = new Matrix3f();
    rprimdT.transpose(arrayToMatrix3f(rprimd));

    //cartesian, angstrom
    float[] xcart = new float[3];
    float[] xred = new float[3];

    for (int i = 0; i < natom; i++) {
      for (int j = 0; j < 3; j++) {
        xred[j] = redPos[i][j];
      }
      xcart = mulVec(matrix3fToArray(rprimdT), xred);
      cartPos[i][0] = xcart[0];
      cartPos[i][1] = xcart[1];
      cartPos[i][2] = xcart[2];
    }
    return cartPos;
  }

  //Pure mathematical method

  /**
   * Multiply the matrix "mat" by the vector "vec".
   * The result is vector.
   * @param mat a <code>Matrix3f</code> value
   * @param vec a <code>Point3f</code> value
   * @return a <code>Point3f</code> value
   */
  public static Point3f mulVec(Matrix3f mat, Point3f vec) {

    Point3f result = new Point3f();
    result.x = mat.m00 * vec.x + mat.m01 * vec.y + mat.m02 * vec.z;
    result.y = mat.m10 * vec.x + mat.m11 * vec.y + mat.m12 * vec.z;
    result.z = mat.m20 * vec.x + mat.m21 * vec.y + mat.m22 * vec.z;
    return result;
  }

  /**
   * Describe <code>mulVec</code> method here.
   *
   * @param mat a <code>float[][]</code> value
   * @param vec a <code>float[]</code> value
   * @return a <code>float[]</code> value
   */
  public static float[] mulVec(float[][] mat, float[] vec) {

    float[] result = new float[3];
    result[0] = mat[0][0] * vec[0] + mat[0][1] * vec[1] + mat[0][2] * vec[2];
    result[1] = mat[1][0] * vec[0] + mat[1][1] * vec[1] + mat[1][2] * vec[2];
    result[2] = mat[2][0] * vec[0] + mat[2][1] * vec[1] + mat[2][2] * vec[2];
    return result;


  }


  /**
   * Describe <code>matrix3fToArray</code> method here.
   *
   * @param matrix3f a <code>Matrix3f</code> value
   * @return a <code>float[][]</code> value
   */
  public static float[][] matrix3fToArray(Matrix3f matrix3f) {

    float[][] array = new float[3][3];

    array[0][0] = matrix3f.m00;
    array[0][1] = matrix3f.m01;
    array[0][2] = matrix3f.m02;

    array[1][0] = matrix3f.m10;
    array[1][1] = matrix3f.m11;
    array[1][2] = matrix3f.m12;

    array[2][0] = matrix3f.m20;
    array[2][1] = matrix3f.m21;
    array[2][2] = matrix3f.m22;

    return array;
  }

  /**
   * Describe <code>arrayToMatrix3f</code> method here.
   *
   * @param array a <code>float[][]</code> value
   * @return a <code>Matrix3f</code> value
   */
  public static Matrix3f arrayToMatrix3f(float[][] array) {

    Matrix3f matrix3f = new Matrix3f();

    matrix3f.m00 = array[0][0];
    matrix3f.m01 = array[0][1];
    matrix3f.m02 = array[0][2];

    matrix3f.m10 = array[1][0];
    matrix3f.m11 = array[1][1];
    matrix3f.m12 = array[1][2];

    matrix3f.m20 = array[2][0];
    matrix3f.m21 = array[2][1];
    matrix3f.m22 = array[2][2];

    return matrix3f;
  }



}
