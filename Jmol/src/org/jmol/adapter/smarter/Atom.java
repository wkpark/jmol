/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

package org.jmol.adapter.smarter;

class Atom {
  int modelNumber = 1;
  String elementSymbol;
  String atomName;
  int charge;
  int scatterFactor = Integer.MIN_VALUE;
  float x, y, z;
  float vectorX = Float.NaN, vectorY = Float.NaN, vectorZ = Float.NaN;
  float bfactor = Float.NaN;
  float occupancy = Float.NaN;
  String pdbAtomRecord;

  Atom() {
  }

  Atom(int modelNumber, String symbol, int charge, float occupancy,
       float bfactor,
       float x, float y, float z, String pdb) {
    this.elementSymbol = symbol;
    this.charge = charge;
    this.occupancy = occupancy;
    this.bfactor = bfactor;
    this.x = x;
    this.y = y;
    this.z = z;
    this.modelNumber = modelNumber;
    this.pdbAtomRecord = pdb;
  }
}
