
/*
 * Copyright 2002 The Jmol Development Team
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
package org.openscience.jmol.applet;
import org.openscience.jmol.render.Shape;

public class NonJavaSort {

  public interface Comparator {
    int compare(Object o1, Object o2);
  }
  public static void sortShapes(Object[] shapes) {
    sort(shapes, new Comparator() {
        public int compare(Object shape1, Object shape2) {
          int z1 = ((Shape) shape1).getZ();
          int z2 = ((Shape) shape2).getZ();
          if (z1 < z2)
            return -1;
          if (z1 == z2)
            return 0;
          return 1;
        }
      }
         );
  }

  public static void sort(Object[] array, Comparator comparator) {
    int N = array.length;
    for (int k = N / 2; k > 0; --k) {
      downheap(array, comparator, k, N);
    }
    do {
      Object temp = array[0];
      array[0] = array[N - 1];
      array[N - 1] = temp;
      --N;
      downheap(array, comparator, 1, N);
    } while (N > 1);
  }

  private static void downheap(Object[] array, Comparator comparator,
                               int k, int N) {

    Object temp = array[k - 1];
    while (k <= N / 2) {
      int j = k + k;
      if ((j < N) && (comparator.compare(array[j - 1], array[j]) < 0)) {
        ++j;
      }
      if (comparator.compare(temp, array[j - 1]) >= 0) {
        break;
      } else {
        array[k - 1] = array[j - 1];
        k = j;
      }
    }
    array[k - 1] = temp;
  }

}

    

