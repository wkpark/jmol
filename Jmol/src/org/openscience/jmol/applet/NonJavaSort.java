/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.applet;
import org.openscience.jmol.render.Shape;

public class NonJavaSort {

  public interface Comparator {
    int compare(Object o1, Object o2);
  }
  public static void sortShapes(Object[] shapes) {
    sort(shapes, new Comparator() {
        public int compare(Object shape1, Object shape2) {
          int z1 = ((Shape) shape1).z;
          int z2 = ((Shape) shape2).z;
          if (z1 < z2)
            return -1;
          if (z1 == z2)
            return 0;
          return 1;
        }
      }
         );
  }

  // This code is cloned from the gnu ClassPath project
  // classpath/java/util/Arrays.java 1.18

  /**
   * Sort an array of Objects according to a Comparator. The sort is
   * guaranteed to be stable, that is, equal elements will not be reordered.
   * The sort algorithm is a mergesort with the merge omitted if the last
   * element of one half comes before the first element of the other half. This
   * algorithm gives guaranteed O(n*log(n)) time, at the expense of making a
   * copy of the array.
   *
   * @param a the array to be sorted
   * @param c a Comparator to use in sorting the array; or null to indicate
   *        the elements' natural order
   * @throws ClassCastException if any two elements are not mutually
   *         comparable by the Comparator provided
   * @throws NullPointerException if a null element is compared with natural
   *         ordering (only possible when c is null)
   */
  public static void sort(Object[] a, Comparator c)
  {
    sort(a, 0, a.length, c);
  }

  /**
   * Sort an array of Objects according to a Comparator. The sort is
   * guaranteed to be stable, that is, equal elements will not be reordered.
   * The sort algorithm is a mergesort with the merge omitted if the last
   * element of one half comes before the first element of the other half. This
   * algorithm gives guaranteed O(n*log(n)) time, at the expense of making a
   * copy of the array.
   *
   * @param a the array to be sorted
   * @param fromIndex the index of the first element to be sorted
   * @param toIndex the index of the last element to be sorted plus one
   * @param c a Comparator to use in sorting the array; or null to indicate
   *        the elements' natural order
   * @throws ClassCastException if any two elements are not mutually
   *         comparable by the Comparator provided
   * @throws ArrayIndexOutOfBoundsException if fromIndex and toIndex
   *         are not in range.
   * @throws IllegalArgumentException if fromIndex &gt; toIndex
   * @throws NullPointerException if a null element is compared with natural
   *         ordering (only possible when c is null)
   */
  public static void sort(Object[] a, int fromIndex, int toIndex, Comparator c)
  {
    if (fromIndex > toIndex)
      throw new IllegalArgumentException("fromIndex " + fromIndex
                                         + " > toIndex " + toIndex);

    // In general, the code attempts to be simple rather than fast, the
    // idea being that a good optimising JIT will be able to optimise it
    // better than I can, and if I try it will make it more confusing for
    // the JIT. First presort the array in chunks of length 6 with insertion
    // sort. A mergesort would give too much overhead for this length.
    for (int chunk = fromIndex; chunk < toIndex; chunk += 6)
      {
        int end = Math.min(chunk + 6, toIndex);
        for (int i = chunk + 1; i < end; i++)
          {
            if (c.compare(a[i - 1], a[i]) > 0)
              {
                // not already sorted
                int j = i;
                Object elem = a[j];
                do
                  {
                    a[j] = a[j - 1];
                    j--;
                  }
                while (j > chunk
                       && c.compare(a[j - 1], elem) > 0);
                a[j] = elem;
              }
          }
      }

    int len = toIndex - fromIndex;
    // If length is smaller or equal 6 we are done.
    if (len <= 6)
      return;

    Object[] src = a;
    Object[] dest = new Object[len];
    Object[] t = null; // t is used for swapping src and dest

    // The difference of the fromIndex of the src and dest array.
    int srcDestDiff = -fromIndex;

    // The merges are done in this loop
    for (int size = 6; size < len; size <<= 1)
      {
        for (int start = fromIndex; start < toIndex; start += size << 1)
          {
            // mid is the start of the second sublist;
            // end the start of the next sublist (or end of array).
            int mid = start + size;
            int end = Math.min(toIndex, mid + size);

            // The second list is empty or the elements are already in
            // order - no need to merge
            if (mid >= end
                || c.compare(src[mid - 1], src[mid]) <= 0)
              {
                System.arraycopy(src, start,
                                 dest, start + srcDestDiff, end - start);

                // The two halves just need swapping - no need to merge
              }
            else if (c.compare(src[start], src[end - 1]) > 0)
              {
                System.arraycopy(src, start,
                                 dest, end - size + srcDestDiff, size);
                System.arraycopy(src, mid,
                                 dest, start + srcDestDiff, end - mid);

              }
            else
              {
                // Declare a lot of variables to save repeating
                // calculations.  Hopefully a decent JIT will put these
                // in registers and make this fast
                int p1 = start;
                int p2 = mid;
                int i = start + srcDestDiff;

                // The main merge loop; terminates as soon as either
                // half is ended
                while (p1 < mid && p2 < end)
                  {
                    dest[i++] =
                      src[(c.compare(src[p1], src[p2]) <= 0
                           ? p1++ : p2++)];
                  }

                // Finish up by copying the remainder of whichever half
                // wasn't finished.
                if (p1 < mid)
                  System.arraycopy(src, p1, dest, i, mid - p1);
                else
                  System.arraycopy(src, p2, dest, i, end - p2);
              }
          }
        // swap src and dest ready for the next merge
        t = src;
        src = dest;
        dest = t;
        fromIndex += srcDestDiff;
        toIndex += srcDestDiff;
        srcDestDiff = -srcDestDiff;
      }

    // make sure the result ends up back in the right place.  Note
    // that src and dest may have been swapped above, so src
    // contains the sorted array.
    if (src != a)
      {
        // Note that fromIndex == 0.
        System.arraycopy(src, 0, a, srcDestDiff, toIndex);
      }
  }



  /*
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
  */
}

    

