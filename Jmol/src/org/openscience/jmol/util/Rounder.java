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
package org.openscience.jmol.util;

import java.text.DecimalFormat;

public class Rounder {
  //Rounds according to...
  public final static int FIX = 0; // ...12334.345  -> 12334.345.
  public final static int EXP = 1; // ...12334.345  -> 1.2334E4
  
  public static String rounds(double number, int ndec, int scheme) {
    String pattern="0";
    switch(scheme) {
    case FIX:
      if (ndec !=0) {
	pattern = pattern +".";
      }
      for (int i=0; i<ndec; i++) {
	pattern = pattern + "0";
      } 
      break;
    case EXP:
      pattern = pattern + ".";
      for (int i=0; i<ndec; i++) {
	pattern = pattern + "0";
      } 
      pattern = pattern + "E0";
      break;
    }

    DecimalFormat myFormatter = new DecimalFormat(pattern);
    return myFormatter.format(number);
  }

} //end class Rounder
