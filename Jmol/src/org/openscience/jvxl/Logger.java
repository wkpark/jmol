/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
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

package org.openscience.jvxl;

class Logger {

  /**
   * Writes a log at DEBUG level.
   * 
   * @param txt String to write.
   */
  public static void debug(String txt) {
    System.out.println(txt);
  }

  /**
   * Writes a log at INFO level.
   * 
   * @param txt String to write.
   */
  public static void info(String txt) {
    System.out.println(txt);
  }

  /**
   * Writes a log at WARN level.
   * 
   * @param txt String to write.
   */
  public static void warn(String txt) {
    System.out.println(txt);
  }

  /**
   * Writes a log at ERROR level.
   * 
   * @param txt String to write.
   */
  public static void error(String txt) {
    System.out.println(txt);
  }
}

