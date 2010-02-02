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


public class FastBitSet implements Cloneable {

  /*
   * Miguel Howard's raw bitset implementation -- faster by a factor of two over standard BitSet class
   * 
   */
  
  public final static FastBitSet mapNull = new FastBitSet(0, true);
  
  public static FastBitSet getNullMap() {
    return mapNull;
  }

  private int[] bitmap;
  
  public static FastBitSet allocateBitmap(int count) {
    return new FastBitSet(count, true);
  }

  public FastBitSet copyFast() {
    int count = getMapStorageCount();
    FastBitSet fbs = new FastBitSet(count, false);
    System.arraycopy(bitmap, 0, fbs.bitmap, 0, count);
    return fbs;
  }

  public boolean getBit(int i) {
    return getBit(bitmap, i);
  }

  public int getPointCount(int dotCount) {
    return getPointCount(bitmap, dotCount);
  }

  public int getSize() {
    return getSize(bitmap);    
  }
  
  public void setBit(int i) {
    setBit(bitmap, i);
  }

  public void clearBit(int i) {
    clearBit(bitmap, i);
  }

  public void setAllBits(int count) {
    setAllBits(bitmap, count);
  }
  
  public void clearBitmap() {
    clearBitmap(bitmap);
  }

  public int getMapStorageCount() {
    return getMapStorageCount(bitmap);
  }

  public int getCardinality() {
    return getPointCount(bitmap, Integer.MAX_VALUE);
  }

  public BitSet toBitSet() {
    BitSet bs = new BitSet();
    int iDot = bitmap.length << F_ADDRESS_BITS_PER_WORD;
    while (--iDot >= 0)
      if (getBit(bitmap, iDot)) {
        bs.set(iDot);
      }
    return bs;
  }


  private FastBitSet(int count, boolean asBits) {
    if (asBits)
      bitmap = new int[(count + F_BIT_INDEX_MASK) >> F_ADDRESS_BITS_PER_WORD];
    else
      bitmap = new int[count];
   
  }
  public FastBitSet() {
    // TODO
  }

  private final static boolean getBit(int[] bitmap, int i) {
    return (bitmap[(i >> F_ADDRESS_BITS_PER_WORD)] << (i & F_BIT_INDEX_MASK)) < 0;
  }

  private final static void setBit(int[] bitmap, int i) {
    bitmap[(i >> F_ADDRESS_BITS_PER_WORD)] |= 1/*l*/ << (~i & F_BIT_INDEX_MASK);
  }

  private final static void clearBit(int[] bitmap, int i) {
    bitmap[(i >> F_ADDRESS_BITS_PER_WORD)] &= ~(1/*l*/ << (~i & F_BIT_INDEX_MASK));
  }

  private static final int F_INT_SHIFT_MASK =   0x80000000;
  //private static final long F_LONG_SHIFT_MASK = 0x8000000000000000l;

  private final static void setAllBits(int[] bitmap, int count) {
    int i = count >> F_ADDRESS_BITS_PER_WORD;
    if ((count & F_BIT_INDEX_MASK) != 0)
      bitmap[i] = F_INT_SHIFT_MASK >> (count - 1);
    while (--i >= 0)
      bitmap[i] = -1;
  }
  
  private final static void clearBitmap(int[] bitmap) {
    for (int i = bitmap.length; --i >= 0; )
      bitmap[i] = 0;
  }

  private final static int getMapStorageCount(int[] bitmap) {
    int indexLast;
    for (indexLast = bitmap.length; --indexLast >= 0
        && bitmap[indexLast] == 0;) {
    }
    return indexLast + 1;
  }

  private static int getPointCount(int[] bitmap, int dotCount) {
    if (bitmap == null)
      return 0;
    int iDot = bitmap.length << F_ADDRESS_BITS_PER_WORD;
    if (iDot > dotCount)
      iDot = dotCount;
    int n = 0;
    while (--iDot >= 0)
      if (getBit(bitmap, iDot))
        n++;
    return n;
  }

  private static int getSize(int[] bitmap) {
    return bitmap.length << F_ADDRESS_BITS_PER_WORD;
  }
  
  private final static int F_ADDRESS_BITS_PER_WORD = 5;
  private final static int F_BITS_PER_WORD = 1 << F_ADDRESS_BITS_PER_WORD;
  private final static int F_BIT_INDEX_MASK = F_BITS_PER_WORD - 1;

  public Object clone() {
    FastBitSet result = new FastBitSet(0, false);
    result.bitmap = (int[]) bitmap.clone();
    return result;
  }

  public String toString() {
    return Escape.escape(toBitSet());
  }
      
  public int hashCode() {
    long h = 1234;
    for (int i = bitmap.length; --i >= 0;)
      h ^= bitmap[i] * (i + 1);
    return (int) ((h >> 32) ^ h);
  }

}
