/*
 * Copyright 2001 The Jmol Development Team
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
package org.openscience.miniJmol;

/**
 *  Sorts arrays using the heap sort algorithm
 */
public class HeapSorter {

	public interface Comparator {
		/**
		 *  Compares two objects.
		 *
		 *  @return a negative integer, zero, or a positive integer as the
		 *  first argument is less than, equal to, or greater than the second.
		 */
		int compare(Object o1, Object o2);
	}
	
	/**
	 *  A comparison function for the objects to be sorted.
	 */
	private Comparator order;
	
	/**
	 *  Creates a sorter using the given comparision function.
	 *
	 *  @param order the comparision function which will determing
	 *  the sorted order.
	 */
	public HeapSorter(Comparator order) {
		this.order = order;
	}
	
	/**
	 *  Sorts an array of objects.
	 *
	 *  @param array the objects to sort.
	 */
	public void sort(Object[] array) {

		int N = array.length;
		for (int k = N / 2; k > 0; --k) {
			downheap(array, k, N);
		}
		do {
			Object temp = array[0];
			array[0] = array[N - 1];
			array[N - 1] = temp;
			N = N - 1;
			downheap(array, 1, N);
		} while (N > 1);
	}

	/**
	 *  Sorts an array of integers.
	 *
	 *  @param array the integers to sort.
	 */
	public static void sort(int[] array, boolean ascending) {

		int N = array.length;
		for (int k = N / 2; k > 0; --k) {
			downheap(array, k, N, ascending);
		}
		do {
			int temp = array[0];
			array[0] = array[N - 1];
			array[N - 1] = temp;
			N = N - 1;
			downheap(array, 1, N, ascending);
		} while (N > 1);
	}

	/**
	 *  Sorts an array of doubles.
	 *
	 *  @param array the doubles to sort.
	 */
	public static void sort(double[] array) {

		int N = array.length;
		for (int k = N / 2; k > 0; --k) {
			downheap(array, k, N);
		}
		do {
			double temp = array[0];
			array[0] = array[N - 1];
			array[N - 1] = temp;
			N = N - 1;
			downheap(array, 1, N);
		} while (N > 1);
	}

	void downheap(Object[] array, int k, int N) {

		Object temp = array[k - 1];
		while (k <= N / 2) {
			int j = k + k;
			if ((j < N) && (order.compare(array[j - 1], array[j]) < 0)) {
				++j;
			}
			if (order.compare(temp, array[j - 1]) >= 0) {
				break;
			} else {
				array[k - 1] = array[j - 1];
				k = j;
			}
		}
		array[k - 1] = temp;
	}

	static void downheap(int[] array, int k, int N, boolean ascending) {

		int temp = array[k - 1];
		while (k <= N / 2) {
			int j = k + k;
			if ((j < N) && ((ascending && array[j - 1] < array[j])
					|| (!ascending && array[j - 1] > array[j]))) {
				++j;
			}
			if (temp == array[j - 1]
					|| (ascending && temp > array[j -1])
					|| (!ascending && temp < array[j - 1])) {
				break;
			} else {
				array[k - 1] = array[j - 1];
				k = j;
			}
		}
		array[k - 1] = temp;
	}

	static void downheap(double[] array, int k, int N) {

		double temp = array[k - 1];
		while (k <= N / 2) {
			int j = k + k;
			if ((j < N) && (array[j - 1] < array[j])) {
				++j;
			}
			if (temp >= array[j - 1]) {
				break;
			} else {
				array[k - 1] = array[j - 1];
				k = j;
			}
		}
		array[k - 1] = temp;
	}
}

