/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.util;

/****************************************************************
 * The Bmp class provides support for BitMap Pool objects
 * and static support for BitMaP operations on int[]
 ****************************************************************/

public class Bmp {

  private final static boolean debugDoubleCheck = true;

  public final static int[] allocateBitmap(int count) {
    return new int[(count + 31) >> 5];
  }

  public final static int[] growBitmap(int[] bitmap, int count) {
    if (count < 0)
      throw new IndexOutOfBoundsException();
    if (bitmap == null)
      return (count == 0) ? null : allocateBitmap(count);
    int minLength = (count + 31) >> 5;
    if (bitmap.length >= minLength)
      return bitmap;
    int[] newBitmap = new int[minLength];
    for (int i = bitmap.length; --i >= 0; )
      newBitmap[i] = bitmap[i];
    return newBitmap;
  }

  public final static void setBit(int[] bitmap, int i) {
    int index = i >> 5;
    if (index >= bitmap.length)
      throw new IndexOutOfBoundsException();
    bitmap[index] |= 1 << (i & 31);
  }

  public final static int[] setBitGrow(int[] bitmap, int i) {
    bitmap = growBitmap(bitmap, i + 1);
    if (bitmap != null)
      bitmap[i >> 5] |= 1 << (i & 31);
    return bitmap;
  }

  public final static void clearBit(int[] bitmap, int i) {
    int index = i >> 5;
    if (index < bitmap.length)
      bitmap[(i >> 5)] &= ~(1 << (i & 31));
  }

  public final static boolean getBit(int[] bitmap, int i) {
    int index = i >> 5;
    if (index >= bitmap.length)
      return false;
    return (bitmap[index] & (1 << (i & 31))) != 0;
  }

  public final static void setAllBits(int[] bitmap, int count) {
    int i = (count + 31) >> 5;
    if (bitmap.length != i) {
      if (bitmap.length < i)
        throw new IndexOutOfBoundsException();
      for (int j = bitmap.length; --j > i; )
        bitmap[j] = 0;
    }
    int fractionalBitCount = count & 31;
    if (fractionalBitCount != 0) {
      bitmap[--i] =
        (0x80000000 >> (fractionalBitCount - 1)) >>> (32 - fractionalBitCount);
    }
    while (--i >= 0)
      bitmap[i] = 0xFFFFFFFF;
  }

  public final static int[] copyMinimalBitmap(int[] bitmap) {
    int indexLast;
    for (indexLast = bitmap.length;
         --indexLast >= 0 && bitmap[indexLast] == 0; )
      {}
    int[] map = null;
    if (indexLast >= 0) {
      int count = indexLast + 1;
      map = new int[count];
      for (int j = count; --j >= 0; )
        map[j] = bitmap[j];
    }
    return map;
  }

  public final static int countBits(int map) {
    map -= (map & 0xAAAAAAAA) >>> 1;
    map = (map & 0x33333333) + ((map >>> 2) & 0x33333333);
    map = (map + (map >>> 4)) & 0x0F0F0F0F;
    map += map >>> 8;
    map += map >>> 16;
    return map & 0xFF;
  }

  public final static int countBits(int[] bitmap) {
    int count = 0;
    for (int i = bitmap.length; --i >= 0; ) {
      int bits = bitmap[i];
      if (bits != 0)
        count += countBits(bits);
    }
    return count;
  }

  public final static void clearBitmap(int[] bitmap) {
    for (int i = bitmap.length; --i >= 0; )
      bitmap[i] = 0;
  }

  public final static int getMaxMappedBit(int[] bitmap) {
    if (bitmap == null)
      return 0;
    int answer1 = 0;
    if (debugDoubleCheck) {
      for (answer1 = bitmap.length * 32;
           --answer1 >= 0 && ! getBit(bitmap, answer1) ; )
        {}
      ++answer1;
    }
    
    int maxMapped = bitmap.length << 5;
    int map = 0;
    int i;
    for (i = bitmap.length; --i >= 0 && (map = bitmap[i]) == 0; )
      maxMapped -= 32;
    if (i >= 0) {
      if ((map & 0xFFFF0000) == 0) {
        map <<= 16;
        maxMapped -= 16;
      }
      if ((map & 0xFF000000) == 0) {
        map <<= 8;
        maxMapped -= 8;
      }
      if ((map & 0xF0000000) == 0) {
        map <<= 4;
        maxMapped -= 4;
      }
      if ((map & 0xC0000000) == 0) {
        map <<= 2;
        maxMapped -= 2;
      }
      if (map >= 0)
        maxMapped -= 1;
    }
    if (debugDoubleCheck) {
      if (answer1 != maxMapped) {
        System.out.println("answer1=" + answer1 + " maxMapped=" + maxMapped);
        System.out.println("bitmap.length=" + bitmap.length);
        for (int j = 0; j < bitmap.length; ++j)
          System.out.println("bitmap[" + j + "]=" +
                             Integer.toBinaryString(bitmap[j]));
        throw new NullPointerException();
      }
    }
    return maxMapped;
  }

  public final static int getMinMappedBit(int[] bitmap) {
    if (bitmap == null)
      return -1;
    int mapLength = bitmap.length;
    int maxMapped = mapLength << 5;
    int answer1 = 0;
    if (debugDoubleCheck) {
      for ( ; answer1 < maxMapped && !getBit(bitmap, answer1) ; ++answer1)
        {}
      if (answer1 == maxMapped)
        answer1 = 0;
    }
    int map = 0;
    int minMapped = 0;
    int i;
    for (i = 0; i < mapLength && (map = bitmap[i]) == 0; ++i)
      minMapped += 32;
    if (i == mapLength) {
      minMapped = 0;
    } else {
      if ((map & 0x0000FFFF) == 0) {
        map >>= 16;
        minMapped += 16;
      }
      if ((map & 0x000000FF) == 0) {
        map >>= 8;
        minMapped += 8;
      }
      if ((map & 0x0000000F) == 0) {
        map >>= 4;
        minMapped += 4;
      }
      if ((map & 0x00000003) == 0) {
        map >>= 2;
        minMapped += 2;
      }
      if ((map & 0x00000001) == 0)
        minMapped += 1;
    }
    if (debugDoubleCheck) {
      if (answer1 != minMapped) {
        System.out.println("answer1=" + answer1 + " minMapped=" + minMapped);
        System.out.println("bitmap.length=" + bitmap.length);
        for (int j = 0; j < bitmap.length; ++j)
          System.out.println("bitmap[" + j + "]=" +
                             Integer.toBinaryString(bitmap[j]));
        throw new NullPointerException();
      }
    }
    return minMapped;
  }
}

