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
package org.openscience.jmol;
import org.openscience.jmol.render.Shape;
import java.util.Arrays;
import java.util.Comparator;

public class UseJavaSort {

  // mth nov 2003
  // The goal was to use the built-in Arrays.sort where it is available.
  // When it is not available then use HeapSort class
  // The only way I could get it to work without generating exceptions
  // was by splitting the classes into separate files. 

  public static void sortShapes(Object[] shapes) {
    Arrays.sort(shapes, new Comparator() {
        public int compare(Object shape1, Object shape2) {
          int z1 = ((Shape)shape1).z;
          int z2 = ((Shape)shape2).z;
          if (z1 < z2)
            return -1;
          if (z1 == z2)
            return 0;
          return 1;
        }
      }
                );
  }
}

    

