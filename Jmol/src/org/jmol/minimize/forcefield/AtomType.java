/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.minimize.forcefield;

public class AtomType {
  int elemNo;
  int mmType;
  int hType;
  float formalCharge;
  float fcadj;
  boolean sbmb;
  boolean arom;
  String descr;
  String smartsCode;
  AtomType(int elemNo, int mmType, int hType, float formalCharge, String descr, String smartsCode) {
    this.mmType = mmType;
    this.hType = hType;
    this.elemNo = elemNo;
    this.smartsCode = smartsCode;
    this.descr = descr;
    this.formalCharge = formalCharge;
    setFlags();
  }
  private void setFlags() {
    switch (mmType) {
    
    // Note that these are NOT fractional charges based on
    // number of connected atoms. These are relatively arbitrary
    // fractions of the formal charge to be shared with other atoms.
    // That is, it is not significant that 0.5 is 1/2, and 0.25 is 1/4; 
    // they are just numbers.
    
    case 32:
    case 35:
    case 72:
      // 32  OXYGEN IN CARBOXYLATE ANION
      // 32  NITRATE ANION OXYGEN
      // 32  SINGLE TERMINAL OXYGEN ON TETRACOORD SULFUR
      // 32  TERMINAL O-S IN SULFONES AND SULFONAMIDES
      // 32  TERMINAL O IN SULFONATES
      // 35  OXIDE OXYGEN ON SP2 CARBON, NEGATIVELY CHARGED
      // 72  TERMINAL SULFUR BONDED TO PHOSPHORUS
      fcadj = 0.5f;
      break;
    case 62:
    case 76:
      // 62  DEPROTONATED SULFONAMIDE N-; FORMAL CHARGE=-1
      // 76  NEGATIVELY CHARGED N IN, E.G, TRI- OR TETRAZOLE ANION
      fcadj = 0.25f;
      break;
    }
    switch (mmType) {
    case 37:
    case 38:
    case 39:
    case 44:
    case 58:
    case 59:
    case 63:
    case 64:
    case 65:
    case 66:
    case 69:
    case 78:
    case 79:
    case 81:
    case 82:
      arom = true;
    }
    switch (mmType) {
    case 2:
    case 3:
    case 4:
    case 9:
    case 30:
    case 37:
    case 39:
    case 54:
    case 57:
    case 58:
    case 63:
    case 64:
    case 67:
    case 75:
    case 78:
    case 80:
    case 81:
      sbmb = true;
    }
  }
}