/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-03 13:53:24 -0500 (Thu, 03 May 2007) $
 * $Revision: 7555 $
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.popup;

/*
 * Bob Hanson 5/2007
 * 
 * A class that allows xgettext to find the messages, but for them
 * to not be processed until later, so that languages can be switched
 * at will. 
 * 
 * Underscore indicates that the REAL GT will need to be run;
 * Vertical bar means we either have a replacement or we have some trailer text.
 * The presence of left brace will later indicate we need a replacement.
 * 
 */
class GT {

  static String _(String s) {
    return "_" + s;
  }

  static String _(String s1, String s2) {
    return s1+"|"+s2;
  }
}
