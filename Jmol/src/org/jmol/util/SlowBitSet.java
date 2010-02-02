/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.util;

import java.util.BitSet;
/*
 * for comparison purposes -- the standard BitSet implementation (dots and geosurfaces only)
 * implemented by EnvelopeCalculation using set testflag2 TRUE
 */

public class SlowBitSet extends FastBitSet {

  public SlowBitSet() {
    super();
  }
  
  public static FastBitSet allocateBitmap(int count) {
    return new SlowBitSet(count, true);
  }

  private BitSet bs;

  protected SlowBitSet(int count, boolean asBits) {
    bs = new BitSet(asBits ? count : count * 64 ); 
  }
  
  public FastBitSet copyFast() {
    return (SlowBitSet) this.clone();
  }

  public boolean getBit(int i) {
    return bs.get(i);
  }

  public int getPointCount(int dotCount) {
    return bs.cardinality();
  }

  public int getSize() {
    return bs.size();
  }
  
  public void setBit(int i) {
   bs.set(i);
  }

  public void clearBit(int i) {
    bs.clear(i);
  }

  public void setAllBits(int count) {
    bs.set(0, count);
  }
  
  public void clearBitmap() {
    bs.clear();
  }

  public int getMapStorageCount() {
    return bs.size() * 64;
  }

  public int getCardinality() {
    return bs.cardinality();
  }

  public BitSet toBitSet() {
    return BitSetUtil.copy(bs);
  }

  public Object clone() {
    SlowBitSet result = new SlowBitSet();
    result.bs = (BitSet) bs.clone();
    return result;
  }

  public String toString() {
    return Escape.escape(bs);
  }
      
  public int hashCode() {
    return bs.hashCode();
  }
}
