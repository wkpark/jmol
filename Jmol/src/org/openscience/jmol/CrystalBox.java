
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


/**
 * Describe class <code>CrystalBox</code> here.
 *
 * @author <a href="mailto:Fabian.Dortu@wanadoo.be">Fabian Dortu</a>
 * @version 1.0
 */
public class CrystalBox {

  private float[][] atomBox;
  private float[][] bondBox;
  private float[][] unitBox;

  /**
   * Two different parallepipedic clipping boxes can be combined.
   * The *atom box* determines how many atoms are included in the frame.
   *
   * Bonds will be rendered if the atoms forming
   * the bond are inside the *bond box*.
   *
   * The *atom box* parallelepiped must be equal or larger than the
   * *bond box* one.
   *
   * atomBox and bondBox are crystallographic coordinates
   *
   * Default: atomBox={0,0,0,
   *                   1,1,1}
   * (idem for bondBox).
   *
   * The *unitBox* defines how many unit cell box will be drawn.
   *
   * @param atomBox a <code>float[2][3]</code> value
   * @param bondBox a <code>float[2][3]</code> value
   * @param unitBox a <code>float[2][3]</code> value
   */
  public CrystalBox(float[][] atomBox, float[][] bondBox, float unitBox[][]) {
    this.atomBox = atomBox;
    this.bondBox = bondBox;
    this.unitBox = unitBox;
  }

  /**
   * Creates a new <code>CrystalBox</code> instance.
   *
   */
  public CrystalBox() {

    atomBox = new float[2][3];
    atomBox[0][0] = 0;
    atomBox[0][1] = 0;
    atomBox[0][2] = 0;
    atomBox[1][0] = 1;
    atomBox[1][1] = 1;
    atomBox[1][2] = 1;

    bondBox = new float[2][3];
    bondBox[0][0] = 0;
    bondBox[0][1] = 0;
    bondBox[0][2] = 0;
    bondBox[1][0] = 1;
    bondBox[1][1] = 1;
    bondBox[1][2] = 1;

    unitBox = new float[2][3];
    unitBox[0][0] = 0;
    unitBox[0][1] = 0;
    unitBox[0][2] = 0;
    unitBox[1][0] = 1;
    unitBox[1][1] = 1;
    unitBox[1][2] = 1;

  }

  /**
   * Describe <code>setAtomBox</code> method here.
   *
   * @param atomBox a <code>float[][]</code> value
   */
  public void setAtomBox(float[][] atomBox) {
    this.atomBox = atomBox;
  }

  /**
   * Describe <code>setBondBox</code> method here.
   *
   * @param bondBox a <code>float[][]</code> value
   */
  public void setBondBox(float[][] bondBox) {
    this.bondBox = bondBox;
  }

  /**
   * Describe <code>setUnitBox</code> method here.
   *
   * @param bondBox a <code>float[][]</code> value
   */
  public void setUnitBox(float[][] unitBox) {
    this.unitBox = unitBox;
  }


  /**
   * Describe <code>getAtomBox</code> method here.
   *
   * @return a <code>float[][]</code> value
   */
  public float[][] getAtomBox() {
    return atomBox;
  }

  /**
   * Describe <code>getBondBox</code> method here.
   *
   * @return a <code>float[][]</code> value
   */
  public float[][] getBondBox() {
    return bondBox;
  }

  /**
   * Describe <code>getUnitBox</code> method here.
   *
   * @return a <code>float[][]</code> value
   */
  public float[][] getUnitBox() {
    return unitBox;
  }

}
