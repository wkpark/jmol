/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004  The Jmol Development Team
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

package org.jmol.appletwrapper;

class ClassPreloader extends Thread {
    
  AppletWrapper appletWrapper;

  ClassPreloader(AppletWrapper appletWrapper) {
    this.appletWrapper = appletWrapper;
  }
    
  public void run() {
    String className;
    setPriority(getPriority() - 1);
    while ((className = appletWrapper.getNextPreloadClassName()) != null) {
      //      System.out.println("preloading " + className);
      try {
        int lastCharIndex = className.length() - 1;
        boolean constructOne = className.charAt(lastCharIndex) == '+';
        if (constructOne)
          className = className.substring(0, lastCharIndex);
        Class preloadClass = Class.forName(className);
        if (constructOne)
          preloadClass.newInstance();
        //        System.out.println("finished preloading " + className);
      } catch (Exception e) {
        System.out.println("error preloading " + className);
        e.printStackTrace();
      }
    }
  }
}
