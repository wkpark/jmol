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

  static String appletInfo = "Jmol Applet -- www.jmol.org";
  public String getAppletInfo() {
    return appletInfo;
  }

  public void init() {
    
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
    
    g.drawString("loading Jmol Applet:" + System.currentTimeMillis(),
                 15, 20);
    g.drawString("percentage:" + percentage, 15, 40);
    
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
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    System.out.println("LoadJmolTask.run()");
    long totalTime = 5 * 1000;
    long startTime = System.currentTimeMillis();
    int stepCount = 10;
    int i = 0;
    do {
      long currentTime = System.currentTimeMillis();
      long targetTime = startTime + totalTime * i / stepCount;
      long sleepTime = targetTime - currentTime;
      if (sleepTime > 0) {
        try {
          Thread.sleep((int)sleepTime);
        } catch (InterruptedException ie) {
          System.out.println("who woke me up?");
        }
      }
      wrapper.percentage = i * 100 / stepCount;
      wrapper.repaint();
    } while (++i <= stepCount); // this is indeed <=
    wrapper.jmol = new Jmol(wrapper);
    wrapper.repaint();
  }
}

