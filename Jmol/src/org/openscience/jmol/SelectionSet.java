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
package org.openscience.jmol;

public class SelectionSet {

  int[] set = new int[16];
  int count = 0;

  public int[] getSelection() {
    int[] result = new int[count];
    System.arraycopy(set, 0, result, 0, count);
    return result;
  }
  
  public void addSelection(int num) {
     if (! isSelected(num)) {
       if (count == set.length) {
         int[] setNew = new int[count * 2];
         System.arraycopy(set, 0, setNew, 0, count);
         set = setNew;
       }
       set[count++] = num;
     }
  }
  
  public void removeSelection(int num) {
    for (int i = 0; i < count; ++i)
      if (set[i] == num) {
        System.arraycopy(set, i+1, set, i, count - i);
        --count;
        return;
      }
  }
  
  public void clearSelection() {
    count = 0;
  }

  public int countSelection() {
    return count;
  }

  public boolean isSelected(int num) {
    for (int i = 0; i < count; ++i)
      if (set[i] == num)
        return true;
    return false;
  }
}
