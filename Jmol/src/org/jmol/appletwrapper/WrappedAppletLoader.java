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

class WrappedAppletLoader implements Runnable {
    
  AppletWrapper appletWrapper;
  String wrappedAppletClassName;

  WrappedAppletLoader(AppletWrapper appletWrapper,
                      String wrappedAppletClassName) {
    this.appletWrapper = appletWrapper;
    this.wrappedAppletClassName = wrappedAppletClassName;
  }
    
  public void run() {
    long startTime = System.currentTimeMillis();
    System.out.println("WrappedAppletLoader.run(" +
                       wrappedAppletClassName + ")");
    TickerThread tickerThread = new TickerThread(appletWrapper);
    tickerThread.start();
    WrappedApplet wrappedApplet = null;
    try {
      Class wrappedAppletClass = Class.forName(wrappedAppletClassName);
      wrappedApplet = (WrappedApplet)wrappedAppletClass.newInstance();
      wrappedApplet.setAppletWrapper(appletWrapper);
      wrappedApplet.init();
    } catch (Exception e) {
      System.out.println("Could not instantiate wrappedApplet class" +
                         wrappedAppletClassName);
      e.printStackTrace();
    }
    tickerThread.keepRunning = false;
    tickerThread.interrupt();
    appletWrapper.wrappedApplet = wrappedApplet;
    appletWrapper.repaint();
    long loadTimeSeconds =
      (System.currentTimeMillis() - startTime + 500) / 1000;
    System.out.println(wrappedAppletClassName + " load time = " +
                       loadTimeSeconds + " seconds");
  }
}

class TickerThread extends Thread {
  AppletWrapper appletWrapper;
  boolean keepRunning = true;

  TickerThread(AppletWrapper appletWrapper) {
    this.appletWrapper = appletWrapper;
  }

  public void run() {
    do {
      try {
        Thread.sleep(999);
      } catch (InterruptedException ie) {
        break;
      }
      appletWrapper.repaint();
    } while (keepRunning);
  }
}

