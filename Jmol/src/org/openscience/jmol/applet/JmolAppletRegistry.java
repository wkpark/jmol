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

import java.applet.Applet;
import java.util.Hashtable;
import java.util.Enumeration;

public class JmolAppletRegistry {

  // java.util.Hashtable is synchronized
  // therefore everything here should be thread-safe
  private static Hashtable htRegistry = new Hashtable();

  public static void checkIn(String name, Applet applet) {
    System.out.println("JmolAppletRegistry.checkIn(" + name + ")");
    if (name != null)
      htRegistry.put(name, applet);
  }

  public static void checkOut(String name) {
    System.out.println("JmolAppletRegistry.checkOut(" + name + ")");
    if (name != null)
      htRegistry.remove(name);
  }

  public static Applet lookup(String name) {
    System.out.println("JmolAppletRegistry.lookup(" + name + ")");
    if (name == null)
      return null;
    Applet applet = (Applet)htRegistry.get(name);
    System.out.println("  applet != null" + (applet != null));
    return applet;
  }

  public static Enumeration applets() {
    return htRegistry.elements();
  }
}
