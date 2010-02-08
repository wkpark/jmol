/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2010  The Jmol Development Team
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
  
  private int[] bitmap;
  
  private final static int[] emptyBitmap = new int[0];
  public FastBitSet() {
    bitmap = emptyBitmap;
  }

  private FastBitSet(int bitCount) {
    bitmap = new int[getWordCountFromBitCount(bitCount)];
  }

  public FastBitSet(FastBitSet bitsetToCopy) {
    int wordCount = bitsetToCopy.bitmap.length;
    bitmap = new int[wordCount];
    System.arraycopy(bitsetToCopy.bitmap, 0, bitmap, 0, wordCount);
  }

  public static FastBitSet allocateBitmap(int bitCount) {
    return new FastBitSet(bitCount);
  }

  public void and(FastBitSet setAnd) {
    bitmapAnd(bitmap, setAnd.bitmap);
  }

  public void andNot(FastBitSet setAndNot) {
    bitmapAndNot(bitmap, setAndNot.bitmap);
  }
  
  public int cardinality() {
    return bitmapGetCardinality(bitmap);
  }

  public void clear() {
    bitmapClear(bitmap);
  }

  public void clear(int bitIndex) {
    int wordIndex = bitIndex >> F_ADDRESS_BITS_PER_WORD;
    if (wordIndex < bitmap.length)
      bitmapClearBit(bitmap, bitIndex);
  }

  public void clear(int fromIndex, int toIndex) {
    int bitmapCount = bitmapGetSizeInBits(bitmap);
    if (fromIndex >= bitmapCount)
      return;
    if (toIndex > bitmapCount)
      toIndex = bitmapCount;
    bitmapClearRange(bitmap, fromIndex, toIndex - fromIndex);
  }

  public Object clone() {
    int bitCount = bitmapGetSizeInBits(bitmap);
    FastBitSet result = new FastBitSet(bitCount);
    System.arraycopy(bitmap, 0, result.bitmap, 0, bitmap.length);
    return result;
  }

  public boolean equals(Object obj) {
    if (obj instanceof FastBitSet) {
      FastBitSet bitset2 = (FastBitSet)obj;
      return bitmapIsEqual(bitmap, bitset2.bitmap);
    }
    return false;
  }

  public void flip(int bitIndex) {
    if (get(bitIndex))
      clear(bitIndex);
    else
      set(bitIndex);
  }

  public void flip(int fromIndex, int toIndex) {
    for (int i = fromIndex ; i < toIndex; ++i)
      flip(i);
  }

  public boolean get(int bitIndex) {
    int bitCount = bitmapGetSizeInBits(bitmap);
    if (bitIndex >= bitCount)
      return false;
    return bitmapGetBit(bitmap, bitIndex);
  }

  public boolean isEmpty() {
    return bitmapIsEmpty(bitmap);
  }

  public int nextSetBit(int fromIndex) {
    return bitmapNextSetBit(bitmap, fromIndex);
  }

  public void or(FastBitSet setOr) {
    ensureSufficientWords(setOr.bitmap.length);
    bitmapOr(bitmap, setOr.bitmap);
  }

  public void set(int bitIndex) {
    ensureSufficientBits(bitIndex + 1);
    bitmapSetBit(bitmap, bitIndex);
  }

  public void set(int bitIndex, boolean value) {
    if (value)
      set(bitIndex);
    else
      clear(bitIndex);
  }

  public void set(int fromIndex, int toIndex) {
    ensureSufficientBits(toIndex);
    bitmapSetRange(bitmap, fromIndex, toIndex - fromIndex);
  }

  public void set(int fromIndex, int toIndex, boolean value) {
    if (value)
      set(fromIndex, toIndex);
    else
      clear(fromIndex, toIndex);
  }

  public int size() {
    return bitmapGetSizeInBits(bitmap);
  }

  public void xor(FastBitSet setXor) {
    ensureSufficientWords(setXor.bitmap.length);
    bitmapXor(bitmap, setXor.bitmap);
  }

  public FastBitSet copyFast() {
    int wordCount = bitmapGetMinimumWordCount(bitmap);
    FastBitSet fbs = new FastBitSet(wordCount << F_ADDRESS_BITS_PER_WORD);
    System.arraycopy(bitmap, 0, fbs.bitmap, 0, wordCount);
    return fbs;
  }

  public BitSet toBitSet() {
    BitSet bs = new BitSet();
    int i = bitmapGetSizeInBits(bitmap);
    while (--i >= 0)
      if (get(i))
	bs.set(i);
    return bs;
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

  ////////////////////////////////////////////////////////////////

  private void ensureSufficientBits(int minimumBitCount) {
    int wordCount =
      (minimumBitCount + F_BIT_INDEX_MASK) >> F_ADDRESS_BITS_PER_WORD;
    if (wordCount > bitmap.length) {
      int[] newBitmap = new int[wordCount];
      System.arraycopy(bitmap, 0, newBitmap, 0, bitmap.length);
      bitmap = newBitmap;
    }
  }

  private void ensureSufficientWords(int minimumWordCount) {
    if (minimumWordCount > bitmap.length) {
      int[] newBitmap = new int[minimumWordCount];
      System.arraycopy(bitmap, 0, newBitmap, 0, bitmap.length);
      bitmap = newBitmap;
    }
  }

  ////////////////////////////////////////////////////////////////


  private final static int F_ADDRESS_BITS_PER_WORD = 5;
  private final static int F_BITS_PER_WORD = 1 << F_ADDRESS_BITS_PER_WORD;
  private final static int F_BIT_INDEX_MASK = F_BITS_PER_WORD - 1;

  private static final int[] bitmapAllocateBitCount(int bitCount) {
    return new int[getWordCountFromBitCount(bitCount)];
  }

  private final static boolean bitmapGetBit(int[] bitmap, int i) {
    return ((bitmap[(i >> F_ADDRESS_BITS_PER_WORD)]
	     >> (i & F_BIT_INDEX_MASK)) & 1) != 0;
  }

  private final static void bitmapSetBit(int[] bitmap, int i) {
    bitmap[(i >> F_ADDRESS_BITS_PER_WORD)] |= 1 << (i & F_BIT_INDEX_MASK);
  }

  private final static void bitmapClearBit(int[] bitmap, int i) {
    bitmap[(i >> F_ADDRESS_BITS_PER_WORD)] &= ~(1 << (i & F_BIT_INDEX_MASK));
  }

  private static final int F_INT_SHIFT_MASK =   0x80000000;
  private static final int F_INT_ALL_BITS_SET = 0xFFFFFFFF;

  private final static void bitmapSetAllBits(int[] bitmap, int bitCount) {
    int wholeWordCount = bitCount >> F_ADDRESS_BITS_PER_WORD;
    int fractionalWordBitCount = bitCount & F_BIT_INDEX_MASK;
    if (fractionalWordBitCount > 0)
      bitmap[wholeWordCount] =
	~(F_INT_SHIFT_MASK >> F_BITS_PER_WORD - 1 - fractionalWordBitCount);
    while (--wholeWordCount >= 0)
      bitmap[wholeWordCount] = F_INT_ALL_BITS_SET;
  }

  private final static void bitmapSetRange(int[] bitmap,
					   int iStart,
					   int bitCount) {
    /* increment iStart up to a word boundary the slow way */
    while ((iStart & F_BIT_INDEX_MASK) != 0) {
      bitmapSetBit(bitmap, iStart++);
      if (--bitCount == 0)
	return;
    }
    /* decrement bitCount down to a whole word boundary the slow way */
    while ((bitCount & F_BIT_INDEX_MASK) != 0) {
	bitmapSetBit(bitmap, iStart + --bitCount);
      }
    /* fill in the whole words */
    int wordIndex = iStart >> F_ADDRESS_BITS_PER_WORD;
    int wordCount = bitCount >> F_ADDRESS_BITS_PER_WORD;
    while (--wordCount >= 0)
      bitmap[wordIndex++] = F_INT_ALL_BITS_SET;
  }

  private final static void bitmapClearRange(int[] bitmap,
					     int iStart,
					     int bitCount) {
    /* increment iStart up to a word boundary the slow way */
    while ((iStart & F_BIT_INDEX_MASK) != 0) {
      bitmapClearBit(bitmap, iStart++);
      if (--bitCount == 0)
	return;
    }
    /* decrement bitCount down to a whole word boundary the slow way */
    while ((bitCount & F_BIT_INDEX_MASK) != 0)
      bitmapClearBit(bitmap, iStart + --bitCount);
    /* fill in the whole words */
    int wordIndex = iStart >> F_ADDRESS_BITS_PER_WORD;
    int wordCount = bitCount >> F_ADDRESS_BITS_PER_WORD;
    while (--wordCount >= 0)
      bitmap[wordIndex++] = 0;
  }

  private final static void bitmapClear(int[] bitmap) {
    for (int i = bitmap.length; --i >= 0; )
      bitmap[i] = 0;
  }

  private final static int bitmapGetMinimumWordCount(int[] bitmap) {
    int indexLast;
    for (indexLast = bitmap.length;
	 --indexLast >= 0 && bitmap[indexLast] == 0;
	 ) {
      // nada
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
      if (bitmapGetBit(bitmap, iDot))
        n++;
    return n;
  }

  private static int bitmapGetSizeInBits(int[] bitmap) {
    return bitmap.length << F_ADDRESS_BITS_PER_WORD;
  }

  private static int getWordCountFromBitCount(int bitCount) {
    return (bitCount + F_BITS_PER_WORD - 1) >> F_ADDRESS_BITS_PER_WORD;
  }

  private static int[] bitmapResizeBitCount(int[] oldBitmap, int bitCount) {
    int newWordCount = getWordCountFromBitCount(bitCount);
    int[] newBitmap = new int[newWordCount];
    int oldWordCount = oldBitmap.length;
    int wordsToCopy =
      (newWordCount < oldWordCount) ? newWordCount : oldWordCount;
    System.arraycopy(oldBitmap, 0, newBitmap, 0, wordsToCopy);
    return newBitmap;
  }

  private static void bitmapAnd(int[] bitmap, int[] bitmapAnd) {
    int wordCount =
      bitmap.length < bitmapAnd.length ? bitmap.length : bitmapAnd.length;
    while (--wordCount >= 0)
      bitmap[wordCount] &= bitmapAnd[wordCount];
  }

  private static void bitmapAndNot(int[] bitmap, int[] bitmapAndNot) {
    int wordCount = (bitmap.length < bitmapAndNot.length)
      ? bitmap.length : bitmapAndNot.length;
    while (--wordCount >= 0)
      bitmap[wordCount] &= ~bitmapAndNot[wordCount];
  }

  // bitmap.length should be >= bitmapOr.length
  // to try to enforce this, I am just going to assume that it is the case
  // that way, an OOB exception will be raised
  private static void bitmapOr(int[] bitmap, int[] bitmapOr) {
    int wordCount = bitmapOr.length;
    while (--wordCount >= 0)
      bitmap[wordCount] |= bitmapOr[wordCount];
  }

  private static void bitmapXor(int[] bitmap, int[] bitmapXor) {
    int wordCount = bitmapXor.length;
    while (--wordCount >= 0)
      bitmap[wordCount] ^= bitmapXor[wordCount];
  }

  private static int bitmapNextSetBit(int[] bitmap, int fromIndex) {
    int maxIndex = bitmap.length << F_ADDRESS_BITS_PER_WORD;
    if (fromIndex >= maxIndex)
      return -1;
    // get up to a word boundary
    while ((fromIndex & F_BIT_INDEX_MASK) != 0) {
      if (bitmapGetBit(bitmap, fromIndex))
	return fromIndex;
      ++fromIndex;
    }
    // skip zero words
    while (fromIndex < maxIndex) {
      if (bitmap[fromIndex >> F_ADDRESS_BITS_PER_WORD] != 0)
	break;
      fromIndex += F_BITS_PER_WORD;
    }
    while (fromIndex < maxIndex) {
      if (bitmapGetBit(bitmap, fromIndex))
	return fromIndex;
      ++fromIndex;
    }
    return -1;
  }

  // shrink a bitmap array by removing any zero words at the end of the array
  // note that this may return the bitmap itself without allocating
  // a new bitmap

  private static int[] bitmapMinimize(int[] bitmap) {
    int minimumWordCount = bitmapGetMinimumWordCount(bitmap);
    if (minimumWordCount == bitmap.length)
      return bitmap;
    int[] newBitmap = new int[minimumWordCount];
    System.arraycopy(bitmap, 0, newBitmap, 0, minimumWordCount);
    return newBitmap;
  }
  
  private static int bitmapGetCardinality(int[] bitmap) {
    int dotCount = 0;
    for (int i = bitmap.length; --i >= 0; ) {
      if (bitmap[i] != 0)
	dotCount += countBitsInWord(bitmap[i]);
    }
    return dotCount;
  }

  private static int countBitsInWord(int word) {
    word = (word & 0x55555555) + ((word >> 1) & 0x55555555);
    word = (word & 0x33333333) + ((word >> 2) & 0x33333333);
    word = (word & 0x0F0F0F0F) + ((word >> 4) & 0x0F0F0F0F);
    word = (word & 0x00FF00FF) + ((word >> 8) & 0x00FF00FF);
    word = (word & 0x0000FFFF) + ((word >> 16) & 0x0000FFFF);
    return word;
  }

  private static boolean bitmapIsEqual(int[] bitmap1, int[] bitmap2) {
    if (bitmap1 == bitmap2)
      return true;
    int count1 = bitmapGetMinimumWordCount(bitmap1);
    int count2 = bitmapGetMinimumWordCount(bitmap2);
    if (count1 != count2)
      return false;
    while (--count1 >= 0)
      if (bitmap1[count1] != bitmap2[count1])
	return false;
    return true;
  }

  private static boolean bitmapIsEmpty(int[] bitmap) {
    int i = bitmap.length;
    while (--i >= 0)
      if (bitmap[i] != 0)
	return false;
    return true;
  }


  ////////////////////////////////////////////////////////////////
    
  // deprecated
  public final static FastBitSet mapNull = new FastBitSet();
  public static FastBitSet getNullMap() {
    return mapNull;
  }

  // deprecated
  public boolean getBit(int i) {
    return get(i);
  }

  public int getPointCount(int dotCount) {
    // not sure why dotCount is here
    // probably a remnant of raw bitmap array operaions
    // probably not even used
    // should probably be deprecated in favor of cardinality
    return getPointCount(bitmap, dotCount);
  }

  // deprecated
  public int getCardinality() {
    return cardinality();
  }

  // deprecated
  public int getSize() {
    return size();
  }

  // deprecated
  public void setBit(int i) {
    set(i);
  }

  // deprecated
  public void clearBit(int i) {
    clear(i);
  }

  // probably should be deprecated
  public void setAllBits(int count) {
    set(0, count);
  }
  
  // deprecated
  public void clearBitmap() {
    clear();
  }

  // deprecated
  public int getMapStorageCount() {
    return bitmapGetMinimumWordCount(bitmap);
  }

}
