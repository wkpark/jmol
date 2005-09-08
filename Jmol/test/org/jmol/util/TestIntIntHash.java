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

public class TestIntIntHash extends junit.framework.TestCase {

  public TestIntIntHash() {
  }

  public void setUp() {
  }

  public void tearDown() {
  }

  public void testOne() {
    IntIntHash h = new IntIntHash(10);
    for (int i = 0; i < 10; ++i)
      h.put(i, i, new Integer(i));
    for (int i = 0; i < 10; ++i)
      assertEquals(((Integer)h.get(i, i)).intValue(), i);
  }

  public void test256() {
    IntIntHash h = new IntIntHash(256);
    for (int i = 0; i < 256; ++i)
      h.put(i, i, new Integer(i));
    for (int i = 0; i < 256; ++i)
      assertEquals(((Integer)h.get(i, i)).intValue(), i);
  }

  public void test257() {
    IntIntHash h = new IntIntHash(256);
    for (int i = 0; i < 257; ++i)
      h.put(i, i, new Integer(i));
    for (int i = 0; i < 257; ++i)
      assertEquals(((Integer)h.get(i, i)).intValue(), i);
  }

  public void testUpTo1000() {
    for (int i = 1; i < 1000; i += 100)
      tryOne(i);
  }

  void tryOne(int count) {
    IntIntHash h = new IntIntHash(4);
    for (int i = 0; i < count; ++i)
      h.put(i, i, new Integer(i));
    //    dumpHash(h);
    for (int i = 0; i < count; ++i)
      assertEquals(((Integer)h.get(i, i)).intValue(), i);
  }

  void dumpHash(IntIntHash h) {
    System.out.println("dumping hash:" + h);
    System.out.println("h.entryCount=" + h.entryCount);
    IntIntHash.Entry[] entries = h.entries;
    for (int i = 0; i < entries.length; ++i) {
      System.out.print("" + i + ": ");
      for (IntIntHash.Entry e = entries[i]; e != null; e = e.next) {
        System.out.print("" + e.key1 + "," + e.key2 + " ");
      }
      System.out.println("");
    }
  }

  public void test1000() {
    IntIntHash h = new IntIntHash();
    for (int i = 0; i < 1000; ++i)
      h.put(i, -i, new Integer(i));
    for (int i = 0; i < 1000; ++i)
      assertEquals(((Integer)h.get(i, -i)).intValue(), i);
  }

}
