/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 16:04:16 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5304 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

class CarbohydrateMonomer extends Monomer {

  final static byte[] alphaOffsets = { 0 };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstIndex, int lastIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {
    return new CarbohydrateMonomer(chain, group3, seqcode,
                            firstIndex, lastIndex, alphaOffsets);
  }
  
  ////////////////////////////////////////////////////////////////

  CarbohydrateMonomer(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
  }

  boolean isCarbohydrate() { return true; }

  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    return false;
  }

  final static String allCarbohydrates = 
    "[AFL],[AGC],[AHR],[ARA],[ARB],[BDF],[BDR],[BGC],[BMA]," +
    "[FCA],[FCB],[FRU],[FUC],[FUL],[GAL],[GLA],[GLB],[GLC]," +
    "[GUP],[LXC],[MAN],[RAA],[RAM],[RIB],[RIP],[XYP],[XYS]," +
    "[CBI],[CT3],[CTR],[CTT],[LAT],[MAB],[MAL],[MLR],[MTT]," +
    "[SUC],[TRE],[ASF],[GCU],[MTL],[NAG],[NAM],[RHA],[SOR]," +
    "[XYL]";// from Eric Martz
  final static boolean checkCarbohydrate(String group3) {
    if (group3 == null)
      return false;
    String str = "[" + group3.toUpperCase() + "]";
    return (allCarbohydrates.indexOf(str) >= 0);
  }
}
