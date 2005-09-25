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

public class TestBmp extends junit.framework.TestCase {

  public TestBmp() {
  }

  public void setUp() {
  }

  public void tearDown() {
  }

  public void testNextSetBitCleared() {
    nextCleared(1);
    nextCleared(31);
    nextCleared(32);
    nextCleared(33);
    nextCleared(34);
    nextCleared(63);
    nextCleared(64);
    nextCleared(65);
    nextCleared(66);
    nextCleared(1023);
    nextCleared(1024);
    nextCleared(1025);
    nextCleared(1026);
  }

  void nextCleared(int bitCount) {
    int[] bmp = Bmp.allocateBitmap(bitCount);

    Bmp.clearBitmap(bmp);
    for (int i = bitCount; --i >= 0; )
      assertEquals(Bmp.nextSetBit(bmp, i), -1);
    for (int i = 0; i < bitCount; ++i)
      assertEquals(Bmp.nextSetBit(bmp, i), -1);
  }

  public void testNextSetBitAllOn() {
    nextAllOn(1);
    nextAllOn(31);
    nextAllOn(32);
    nextAllOn(33);
    nextAllOn(34);
    nextAllOn(63);
    nextAllOn(64);
    nextAllOn(65);
    nextAllOn(66);
    nextAllOn(1023);
    nextAllOn(1024);
    nextAllOn(1025);
    nextAllOn(1026);
  }

  void nextAllOn(int bitCount) {
    int[] bmp = Bmp.allocateBitmap(bitCount);

    Bmp.setAllBits(bmp, bitCount);
    for (int i = bitCount; --i >= 0; )
      assertEquals(Bmp.nextSetBit(bmp, i), i);
    for (int i = 0; i < bitCount; ++i)
      assertEquals(Bmp.nextSetBit(bmp, i), i);
  }

  public void testNextSetBitSkip() {
    nextSkip(1);
    nextSkip(8);
    nextSkip(31);
    nextSkip(32);
    nextSkip(33);
    nextSkip(34);
    nextSkip(63);
    nextSkip(64);
    nextSkip(65);
    nextSkip(66);
    nextSkip(1023);
    nextSkip(1024);
    nextSkip(1025);
    nextSkip(1026);
  }

  void nextSkip(int bitCount) {
    int[] bmp = Bmp.allocateBitmap(bitCount);

    for (int i = 2; i < 10; ++i)
      nextSkip(bmp, bitCount, i);
  }

  void nextSkip(int[] bmp, int bitCount, int skip) {
    Bmp.clearBitmap(bmp);
    for (int i = 0; i < bitCount; i += skip)
      Bmp.setBit(bmp, i);
    for (int i = bitCount; --i >= 0; ) {
      int nextOn;
      nextOn = ((i + (skip - 1)) / skip) * skip;
      if (nextOn >= bitCount)
        nextOn = -1;
      assertEquals(Bmp.nextSetBit(bmp, i), nextOn);
    }
  }

  public void testSetAllWithLargerBmp() {
    int[] bmp = Bmp.allocateBitmap(65);
    Bmp.setAllBits(bmp, 0);
    for (int i = 0; i < 96; ++i)
      assertFalse(Bmp.getBit(bmp, i));
    Bmp.setAllBits(bmp, 10);
    for (int i = 0; i < 10; ++i)
      assertTrue(Bmp.getBit(bmp, i));
    for (int i = 10; i < 96; ++i)
      assertFalse(Bmp.getBit(bmp, i));

    Bmp.setAllBits(bmp, 65);
    Bmp.setAllBits(bmp, 0);
    for (int i = 0; i < 96; ++i)
      assertFalse(Bmp.getBit(bmp, i));

    Bmp.setAllBits(bmp, 65);
    Bmp.setAllBits(bmp, 10);
    for (int i = 0; i < 10; ++i)
      assertTrue(Bmp.getBit(bmp, i));
    for (int i = 10; i < 96; ++i)
      assertFalse(Bmp.getBit(bmp, i));
  }
}
