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

package org.jmol.applet;

import netscape.javascript.JSObject;

import java.applet.*;
import java.awt.*;

public class Wrapper extends Applet {

  Jmol jmol;
  int percentage;
  long startTime;

  static String appletInfo = "Jmol Applet -- www.jmol.org";
  public String getAppletInfo() {
    return appletInfo;
  }

  public void init() {
    
    startTime = System.currentTimeMillis();
    new Thread(new LoadJmolTask(this)).start();

  }
  
  public void update(Graphics g) {
    if (jmol != null) {
      jmol.update(g);
      return;
    }

    g.setColor(Color.yellow);
    g.fillRect(0, 0, 1000, 1000);
    
    g.setColor(Color.black);
    
    g.drawString("applet wrapper test", 15, 20);
    
    long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;

    g.drawString("" + elapsedTime + " seconds", 15, 40);

  }

  public void paint(Graphics g) {
    //    System.out.println("paint called");
    update(g);
  }

  public boolean handleEvent(Event e) {
    if (jmol != null)
      return jmol.handleEvent(e);
    return false;
  }
  
  public void scriptButton(JSObject buttonWindow, String buttonName,
                           String script, String buttonCallback) {
    if (jmol != null)
      jmol.scriptButton(buttonWindow, buttonName,
                              script, buttonCallback);
  }
  
  public void script(String script) {
    if (jmol != null)
      jmol.script(script);
  }
  
  public void loadInline(String strModel) {
    if (jmol != null)
      jmol.loadInline(strModel);
  }
}

class LoadJmolTask implements Runnable {
    
  Wrapper wrapper;

  LoadJmolTask(Wrapper wrapper) {
    this.wrapper = wrapper;
  }
    
  public void run() {
    long startTime = System.currentTimeMillis();
    System.out.println("LoadJmolTask.run()");
    Thread tickerThread = new Thread(new TickerTask(wrapper));
    tickerThread.start();
    Jmol jmol = new Jmol(wrapper);
    tickerThread.interrupt();
    wrapper.jmol = jmol;
    wrapper.repaint();
    long loadTimeSeconds =
      (System.currentTimeMillis() - startTime + 500) / 1000;
    System.out.println("appletloadTime=" + loadTimeSeconds);
  }
}

class TickerTask implements Runnable {
  Wrapper wrapper;

  TickerTask(Wrapper wrapper) {
    this.wrapper = wrapper;
  }

  public void run() {
    do {
      try {
        Thread.sleep(999);
      } catch (InterruptedException ie) {
        break;
      }
      wrapper.repaint();
    } while (! Thread.interrupted());
  }
}

